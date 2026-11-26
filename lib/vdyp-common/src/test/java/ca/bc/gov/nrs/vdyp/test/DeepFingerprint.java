package ca.bc.gov.nrs.vdyp.test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

/**
 * Computes a checksum of the state reachable from an object, for tests that check an object graph is not modified. VDYP
 * model classes are walked field by field; maps, collections, arrays and optionals are walked by content; other JDK
 * value types contribute their string form. Classes outside the VDYP packages that are not handled above (loggers,
 * lambdas and so on) contribute only their class name. Used by SharedControlMapTest and TemplateControlMapsTest to
 * check that building a run's control map, and running each type of application, leaves the shared control maps
 * unchanged.
 */
public final class DeepFingerprint {

	private static final String VDYP_PACKAGE = "ca.bc.gov.nrs.vdyp";

	private final Checksum checksum = new CRC32C();
	private final Map<Object, Integer> visited = new IdentityHashMap<>();

	private DeepFingerprint() {
	}

	/**
	 * @return a hex checksum of the state reachable from {@code root}
	 */
	public static String of(Object root) {
		var fingerprint = new DeepFingerprint();
		fingerprint.walk(root);
		return Long.toHexString(fingerprint.checksum.getValue());
	}

	private void append(String s) {
		checksum.update(s.getBytes(StandardCharsets.UTF_8));
		checksum.update(0);
	}

	private void walk(Object o) {
		if (o == null) {
			append("null");
			return;
		}
		Class<?> c = o.getClass();
		if (o instanceof CharSequence || o instanceof Number || o instanceof Boolean || o instanceof Character
				|| o instanceof Enum<?> || o instanceof Class<?>) {
			append(c.getName() + ":" + o);
			return;
		}

		var previous = visited.putIfAbsent(o, visited.size());
		if (previous != null) {
			append("ref:" + previous);
			return;
		}

		append(c.getName());
		if (c.isArray()) {
			int length = Array.getLength(o);
			append("length:" + length);
			for (int i = 0; i < length; i++) {
				walk(Array.get(o, i));
			}
		} else if (o instanceof Map<?, ?> map) {
			append("size:" + map.size());
			for (var entry : map.entrySet()) {
				walk(entry.getKey());
				walk(entry.getValue());
			}
		} else if (o instanceof Collection<?> collection) {
			append("size:" + collection.size());
			for (var element : collection) {
				walk(element);
			}
		} else if (o instanceof Optional<?> optional) {
			walk(optional.orElse(null));
		} else if (c.getName().startsWith(VDYP_PACKAGE) && !c.isSynthetic() && !c.getName().contains("$$Lambda")) {
			walkFields(o, c);
		} else if (c.getName().startsWith("java.")) {
			append(o.toString());
		}
	}

	private void walkFields(Object o, Class<?> c) {
		for (Class<?> k = c; k != null && k.getName().startsWith(VDYP_PACKAGE); k = k.getSuperclass()) {
			for (Field field : k.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
					continue;
				}
				append(k.getName() + "." + field.getName());
				try {
					field.setAccessible(true);
					walk(field.get(o));
				} catch (ReflectiveOperationException | RuntimeException e) {
					append("inaccessible");
				}
			}
		}
	}
}
