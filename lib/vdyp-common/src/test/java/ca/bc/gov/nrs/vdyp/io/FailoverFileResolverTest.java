package ca.bc.gov.nrs.vdyp.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.test.MockFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class FailoverFileResolverTest {

	@Test
	void testOneDelegate() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1);

		assertThat(unit.toString("file"), equalTo(delegate1.toString("file")));
		assertThat(unit.toPath("file"), equalTo(delegate1.toPath("file")));
		assertThat(unit.resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.resolveForOutput("outputFile"), sameInstance(outputStream));

	}

	@Test
	void testOneDelegateFileMissing() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");

		delegate1.addError("inputFile", () -> new NoSuchFileException("The file is missing and no failover"));
		delegate1.addError("outputFile", () -> new NoSuchFileException("The file is missing and no failover"));

		var unit = new FailoverFileResolver(delegate1);

		assertThrows(FileNotFoundException.class, () -> unit.resolveForInput("inputFile"));
		assertThrows(FileNotFoundException.class, () -> unit.resolveForOutput("outputFile"));
	}

	@Test
	void testTwoDelegatesTakeFirst() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThat(unit.toString("file"), equalTo(delegate1.toString("file")));
		assertThat(unit.toPath("file"), equalTo(delegate1.toPath("file")));
		assertThat(unit.resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.resolveForOutput("outputFile"), sameInstance(outputStream));
	}

	@Test
	void testTwoDelegatesTakeSecond() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1.addError(
				"inputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1.addError(
				"outputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThat(unit.toString("file"), equalTo(delegate1.toString("file")));
		assertThat(unit.toPath("file"), equalTo(delegate1.toPath("file")));
		assertThat(unit.resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.resolveForOutput("outputFile"), sameInstance(outputStream));
	}

	@Test
	void testTwoDelegatesBothFail() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");

		delegate1.addError(
				"inputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2.addError("inputFile", () -> new NoSuchFileException("The file is missing and no failover"));

		delegate1.addError(
				"outputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2.addError("outputFile", () -> new NoSuchFileException("The file is missing and no failover"));

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThrows(FileNotFoundException.class, () -> unit.resolveForInput("inputFile"));
		assertThrows(FileNotFoundException.class, () -> unit.resolveForOutput("outputFile"));
	}

	@Test
	void testTwoDelegatesRelativeTakeFirst() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");
		var delegate1rel = new MockFileResolver("delegate1rel");
		var delegate2rel = new MockFileResolver("delegate2rel");

		delegate1.addChild("relative", delegate1rel);
		delegate2.addChild("relative", delegate2rel);

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1rel.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1rel.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThat(unit.relative("relative").resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.relative("relative").resolveForOutput("outputFile"), sameInstance(outputStream));
	}

	@Test
	void testTwoDelegatesRelativeTakeSecond() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");
		var delegate1rel = new MockFileResolver("delegate1rel");
		var delegate2rel = new MockFileResolver("delegate2rel");

		delegate1.addChild("relative", delegate1rel);
		delegate2.addChild("relative", delegate2rel);

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1rel.addError(
				"inputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2rel.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1rel.addError(
				"outputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2rel.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThat(unit.relative("relative").resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.relative("relative").resolveForOutput("outputFile"), sameInstance(outputStream));
	}

	@Test
	void testTwoDelegatesRelativeToParentTakeFirst() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");
		var delegate1rel = new MockFileResolver("delegate1rel");
		var delegate2rel = new MockFileResolver("delegate2rel");

		delegate1.addChild("relative", delegate1rel);
		delegate2.addChild("relative", delegate2rel);

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1rel.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1rel.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThat(unit.relative("relative").resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.relative("relative").resolveForOutput("outputFile"), sameInstance(outputStream));
	}

	@Test
	void testTwoDelegatesRelativeToParentTakeSecond() throws IOException {
		var delegate1 = new MockFileResolver("delegate1");
		var delegate2 = new MockFileResolver("delegate2");
		var delegate1rel = new MockFileResolver("delegate1rel");
		var delegate2rel = new MockFileResolver("delegate2rel");

		delegate1.addChild("relative", delegate1rel);
		delegate2.addChild("relative", delegate2rel);

		final InputStream inputStream = TestUtils.makeInputStream("data");
		delegate1rel.addError(
				"inputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2rel.addStream("inputFile", inputStream);

		final OutputStream outputStream = new ByteArrayOutputStream();
		delegate1rel.addError(
				"outputFile",
				() -> new NoSuchFileException("The file is missing, should silently fail over to next resolver")
		);
		delegate2rel.addStream("outputFile", outputStream);

		var unit = new FailoverFileResolver(delegate1, delegate2);

		assertThat(unit.relativeToParent("relative/file").resolveForInput("inputFile"), sameInstance(inputStream));
		assertThat(unit.relativeToParent("relative/file").resolveForOutput("outputFile"), sameInstance(outputStream));
	}

}
