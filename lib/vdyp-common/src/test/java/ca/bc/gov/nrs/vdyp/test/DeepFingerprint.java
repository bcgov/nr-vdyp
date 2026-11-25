package ca.bc.gov.nrs.vdyp.test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Computes a digest of the state reachable from an object, for tests that check an object graph is not modified. VDYP
 * model classes are walked field by field; maps, collections, arrays and optionals are walked by content; other JDK
 * value types contribute their string form. Classes outside the VDYP packages that are not handled above (loggers,
 * lambdas and so on) contribute only their class name.
 *
 * Used by SharedControlMapTest to ensure that after each type of application completes the map is not mutated
 */
public final class DeepFingerprint {

	private static final String VDYP_PACKAGE = "ca.bc.gov.nrs.vdyp";

	private final MessageDigest digest;
	private final Map<Object, Integer> visited = new IdentityHashMap<>();

	private DeepFingerprint() {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @return a hex digest of the state reachable from {@code root}
	 */
	public static String of(Object root) {
		var fingerprint = new DeepFingerprint();
		fingerprint.walk(root);
		return HexFormat.of().formatHex(fingerprint.digest.digest());
	}

	private void append(String s) {
		digest.update(s.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
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
