package ca.bc.gov.nrs.vdyp.model;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * This class is able to dump its state in a form amenable to line by line comparison
 */
public interface Dumpable {
	void dumpState(Appendable output, int indent) throws IOException;

	default void dumpState(Appendable output) throws IOException {
		this.dumpState(output, 0);
	}

	static String indentString(int i) {
		return "  ".repeat(i);
	}

	static void writeProperty(Appendable output, int indent, String name, Object value) throws IOException {
		output.append(indentString(indent)).append(name).append(" = ").append(value.toString()).append("\n");
	}

	static void writeHeader(Appendable output, int indent, Class<? extends Dumpable> klazz, String id)
			throws IOException {
		output.append(indentString(indent)).append(klazz.getSimpleName()).append(" (").append(id).append(")\n");
	}

	static void writeChildren(Appendable output, int indent, String name, Collection<? extends Dumpable> children)
			throws IOException {
		output.append(indentString(indent)).append(name).append(":\n");
		if (children.isEmpty()) {
			output.append(indentString(indent + 1)).append("N/A\n");
		} else {
			for (var child : children) {
				child.dumpState(output, indent + 1);
			}
		}
	}

	static void writeChild(Appendable output, int indent, String name, Optional<? extends Dumpable> child)
			throws IOException {
		output.append(indentString(indent)).append(name).append(":\n");
		if (child.isEmpty()) {
			output.append(indentString(indent + 1)).append("N/A\n");
		} else {
			child.get().dumpState(output, indent + 1);
		}
	}

	/**
	 * Dumps an objects state to the console and exits. This is only intended for debugging.
	 *
	 * @param toDump
	 */
	@Deprecated
	static void dumpAndExit(Dumpable toDump) {
		try {
			toDump.dumpState(System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
