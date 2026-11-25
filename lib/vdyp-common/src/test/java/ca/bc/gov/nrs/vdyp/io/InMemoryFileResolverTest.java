package ca.bc.gov.nrs.vdyp.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InMemoryFileResolverTest {

	@ParameterizedTest
	@CsvSource(
		{ "VDYP.CTR, VDYP.CTR", "coe/Becdef.dat, coe/Becdef.dat", "coe\\Becdef.dat, coe/Becdef.dat",
				"/coe//./Becdef.dat, coe/Becdef.dat", "coe/../VDYP.CTR, VDYP.CTR", "../../VDYP.CTR, VDYP.CTR" }
	)
	void testNormalize(String path, String expected) {
		assertThat(InMemoryFileResolver.normalize(path), is(expected));
	}

	@Test
	void testFromZipResolvesRelativeToControlFile() throws Exception {
		var bytes = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry("VDYP.CTR"));
			zip.write("control".getBytes(StandardCharsets.UTF_8));
			zip.putNextEntry(new ZipEntry("coe/"));
			zip.putNextEntry(new ZipEntry("coe/Becdef.dat"));
			zip.write("bec".getBytes(StandardCharsets.UTF_8));
		}

		var resolver = InMemoryFileResolver.fromZip(new ByteArrayInputStream(bytes.toByteArray()));

		try (var is = resolver.relativeToParent("VDYP.CTR").resolveForInput("coe\\Becdef.dat")) {
			assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8), is("bec"));
		}
		try (var is = resolver.relative("coe").resolveForInput("../VDYP.CTR")) {
			assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8), is("control"));
		}
		assertThrows(FileNotFoundException.class, () -> resolver.resolveForInput("missing.dat"));
		assertThrows(UnsupportedOperationException.class, () -> resolver.resolveForOutput("VDYP.CTR"));
	}

	private static String read(FileResolver resolver, String filename) throws Exception {
		try (var is = resolver.resolveForInput(filename)) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void testResolvesIgnoringCaseWhenNoExactMatch() throws Exception {
		var resolver = new InMemoryFileResolver(
				Map.of(
						"coe/SIEQN.PRM", "sieqn".getBytes(StandardCharsets.UTF_8), "coe/FIPSTKR.PRM",
						"stk".getBytes(StandardCharsets.UTF_8)
				)
		);

		// Names as VRISTART.CTR writes them
		assertThat(read(resolver, "coe/SIEQN.prm"), is("sieqn"));
		assertThat(read(resolver, "coe/fipstkR.prm"), is("stk"));
		assertThat(read(resolver.relative("COE"), "fipstkr.prm"), is("stk"));
	}

	@Test
	void testPrefersExactMatchAndRejectsAmbiguousCaseMatch() throws Exception {
		var resolver = new InMemoryFileResolver(
				Map.of(
						"a.dat", "lower".getBytes(StandardCharsets.UTF_8), "A.DAT",
						"upper".getBytes(StandardCharsets.UTF_8)
				)
		);

		assertThat(read(resolver, "a.dat"), is("lower"));
		assertThat(read(resolver, "A.DAT"), is("upper"));
		assertThrows(FileNotFoundException.class, () -> resolver.resolveForInput("A.dat"));
	}
}
