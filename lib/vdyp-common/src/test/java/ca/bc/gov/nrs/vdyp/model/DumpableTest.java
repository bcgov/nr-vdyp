package ca.bc.gov.nrs.vdyp.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class DumpableTest {

	class TestDumpable implements Dumpable {
		int i;

		public TestDumpable(int i) {
			this.i = i;
		}

		@Override
		public void dumpState(Appendable out, int indent) throws IOException {
			Dumpable.writeHeader(out, 0, TestDumpable.class, "This is a Test " + i);
			Dumpable.writeProperty(out, indent + 1, "property", "value" + i);
		}

	}

	class TestDumpableChild implements Dumpable {
		int i;

		public TestDumpableChild(int i) {
			this.i = i;
		}

		@Override
		public void dumpState(Appendable out, int indent) throws IOException {
			Dumpable.writeHeader(out, indent, TestDumpableChild.class, "This is a Test " + i);
			Dumpable.writeProperty(out, indent + 1, "property", "value" + i);
		}

	}

	@Test
	void testIndentString() {
		assertThat(Dumpable.indentString(0), equalTo(""));
		assertThat(Dumpable.indentString(1), equalTo("  "));
		assertThat(Dumpable.indentString(2), equalTo("    "));
		assertThat(Dumpable.indentString(3), equalTo("      "));
	}

	@Test
	void testWriteHeader() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeHeader(out, 0, TestDumpable.class, "This is a Test");

		assertThat(out.toString(), equalTo("TestDumpable (This is a Test)\n"));

		out = new StringBuilder();

		Dumpable.writeHeader(out, 1, TestDumpable.class, "This is a Test");

		assertThat(out.toString(), equalTo("  TestDumpable (This is a Test)\n"));
	}

	@Test
	void testWriteProperty() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeProperty(out, 0, "testProperty", "This is a Test");

		assertThat(out.toString(), equalTo("testProperty = This is a Test\n"));

		out = new StringBuilder();

		Dumpable.writeProperty(out, 1, "testProperty", "This is a Test");

		assertThat(out.toString(), equalTo("  testProperty = This is a Test\n"));
	}

	@Test
	void testWriteNoChildren() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeChildren(out, 0, "children", List.of());

		assertThat(out.toString(), equalTo("children:\n  N/A\n"));

		out = new StringBuilder();

		Dumpable.writeChildren(out, 1, "children", List.of());

		assertThat(out.toString(), equalTo("  children:\n    N/A\n"));

	}

	@Test
	void testWriteOneChildren() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeChildren(out, 0, "children", List.of(new TestDumpableChild(1)));

		assertThat(
				out.toString(), equalTo("children:\n  TestDumpableChild (This is a Test 1)\n    property = value1\n")
		);

		out = new StringBuilder();

		Dumpable.writeChildren(out, 1, "children", List.of(new TestDumpableChild(1)));

		assertThat(
				out.toString(),
				equalTo("  children:\n    TestDumpableChild (This is a Test 1)\n      property = value1\n")
		);

	}

	@Test
	void testWriteTwoChildren() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeChildren(out, 0, "children", List.of(new TestDumpableChild(1), new TestDumpableChild(2)));

		assertThat(
				out.toString(),
				equalTo(
						"children:\n  TestDumpableChild (This is a Test 1)\n    property = value1\n  TestDumpableChild (This is a Test 2)\n    property = value2\n"
				)
		);

		out = new StringBuilder();

		Dumpable.writeChildren(out, 1, "children", List.of(new TestDumpableChild(1), new TestDumpableChild(2)));

		assertThat(
				out.toString(),
				equalTo(
						"  children:\n    TestDumpableChild (This is a Test 1)\n      property = value1\n    TestDumpableChild (This is a Test 2)\n      property = value2\n"
				)
		);

	}

	@Test
	void testWriteNoChild() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeChild(out, 0, "child", Optional.empty());

		assertThat(out.toString(), equalTo("child:\n  N/A\n"));

		out = new StringBuilder();

		Dumpable.writeChild(out, 1, "child", Optional.empty());

		assertThat(out.toString(), equalTo("  child:\n    N/A\n"));

	}

	@Test
	void testWriteWithChild() throws IOException {
		Appendable out = new StringBuilder();

		Dumpable.writeChild(out, 0, "child", Optional.of(new TestDumpableChild(1)));

		assertThat(out.toString(), equalTo("child:\n  TestDumpableChild (This is a Test 1)\n    property = value1\n"));

		out = new StringBuilder();

		Dumpable.writeChild(out, 1, "child", Optional.of(new TestDumpableChild(1)));

		assertThat(
				out.toString(), equalTo("  child:\n    TestDumpableChild (This is a Test 1)\n      property = value1\n")
		);

	}

	@Test
	void testDumpStateNoIndent() throws IOException {
		Appendable out = new StringBuilder();

		var unit = new TestDumpable(1);
		unit.dumpState(out);

		assertThat(out.toString(), equalTo("TestDumpable (This is a Test 1)\n  property = value1\n"));
	}

}
