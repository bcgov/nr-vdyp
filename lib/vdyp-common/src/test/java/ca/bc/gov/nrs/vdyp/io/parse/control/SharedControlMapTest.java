package ca.bc.gov.nrs.vdyp.io.parse.control;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.controlmap.ControlMapCache;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingControlVariables;
import ca.bc.gov.nrs.vdyp.test.DeepFingerprint;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class SharedControlMapTest {

	private static final String CONTROL_FILE = "VDYP.CTR";
	private static final String STAND_CONTROL_FILE = "STAND.CTR";

	@TempDir
	Path sharedDir;

	@TempDir
	Path runDir;

	FileSystemFileResolver sharedResolver;
	FileSystemFileResolver runResolver;

	@BeforeEach
	void setUp() throws IOException {
		sharedResolver = TestUtils.initConfigDir(sharedDir, TestUtils.class, CONTROL_FILE);
		for (var dataFile : List.of("vin_p1.dat", "vin_s1.dat", "vin_u1.dat", "vin_y1.dat")) {
			IOUtils.copy(TestUtils.class.getResource(dataFile), runDir.resolve(dataFile).toFile());
		}
		runResolver = new FileSystemFileResolver(runDir);
		ControlMapCache.getInstance().clear();
	}

	@AfterEach
	void tearDown() {
		ControlMapCache.getInstance().clear();
	}

	private void writeStandControlFile(String line) throws IOException {
		Files.writeString(runDir.resolve(STAND_CONTROL_FILE), line + "\n");
	}

	@Test
	void testSharedMapIsUnmodifiableAndLeavesRunEntriesRaw() throws Exception {
		var shared = new ForwardControlParser().parseSharedByName(List.of(CONTROL_FILE), sharedResolver);

		assertThat(shared.get(ControlKey.BEC_DEF.name()), instanceOf(BecLookup.class));
		// Input and output entries are not resolved until a run
		assertThat(shared.get(ControlKey.VDYP_INPUT_VDYP_POLY.name()), is("vin_p1.dat"));
		assertThat(shared.get(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name()), is("vp_grow2.dat"));

		assertThrows(UnsupportedOperationException.class, () -> shared.put("X", "Y"));
	}

	@Test
	void testRunMapSharesParsedEntriesAndResolvesRunFilesInRunFolder() throws Exception {
		var parser = new ForwardControlParser();
		var shared = parser.parseSharedByName(List.of(CONTROL_FILE), sharedResolver);
		var sharedFingerprint = DeepFingerprint.of(shared);

		var run = parser.parseForRun(shared, List.of(), runResolver);

		assertThat(run, not(sameInstance(shared)));
		// Parsed definitions and coefficients are shared by reference
		for (var modifier : parser.sharedModifiers()) {
			var key = modifier.getControlKeyName();
			assertThat(key, run.get(key), sameInstance(shared.get(key)));
		}
		// Output files resolve in the run folder
		assertThat(run.get(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name()), is(runDir.resolve("vp_grow2.dat").toString()));
		// Input entries are replaced by parser factories
		assertThat(run.get(ControlKey.VDYP_INPUT_VDYP_POLY.name()), not(instanceOf(String.class)));

		// Building the run map leaves the shared map unchanged
		assertThat(DeepFingerprint.of(shared), equalTo(sharedFingerprint));
		assertThat(shared.get(ControlKey.VDYP_INPUT_VDYP_POLY.name()), is("vin_p1.dat"));
	}

	@Test
	void testRunMapHasSameEntriesAsFullParse() throws Exception {
		var full = new ForwardControlParser()
				.parseByName(List.of(CONTROL_FILE), TestUtils.fileResolver(TestUtils.class), new HashMap<>());

		var parser = new ForwardControlParser();
		var run = parser
				.parseForRun(parser.parseSharedByName(List.of(CONTROL_FILE), sharedResolver), List.of(), runResolver);

		assertThat(run.keySet(), equalTo(full.keySet()));
		for (var entry : full.entrySet()) {
			var runValue = run.get(entry.getKey());
			if (entry.getValue() == null) {
				assertThat(entry.getKey(), runValue, is((Object) null));
			} else {
				assertThat(entry.getKey(), runValue, instanceOf(entry.getValue().getClass()));
			}
		}
		// Parsed content is the same
		for (var modifier : parser.sharedModifiers()) {
			var key = modifier.getControlKeyName();
			assertThat(key, DeepFingerprint.of(run.get(key)), equalTo(DeepFingerprint.of(full.get(key))));
		}
	}

	@Test
	void testRunControlFileOverridesRunEntries() throws Exception {
		var parser = new ForwardControlParser();
		var shared = parser.parseSharedByName(List.of(CONTROL_FILE), sharedResolver);
		var sharedFingerprint = DeepFingerprint.of(shared);
		writeStandControlFile("101 2050   1   2   3   1   1");

		var run = parser.parseForRun(shared, List.of(STAND_CONTROL_FILE), runResolver);

		var sharedVtrol = (ProcessingControlVariables) shared.get(ControlKey.VTROL.name());
		var runVtrol = (ProcessingControlVariables) run.get(ControlKey.VTROL.name());
		assertThat(sharedVtrol.getTargetYear().getValue(), not(2050));
		assertThat(runVtrol.getTargetYear().getValue(), is(2050));
		assertThat(DeepFingerprint.of(shared), equalTo(sharedFingerprint));
		// Entries the stand file does not mention keep their shared values
		assertThat(run.get(ControlKey.BEC_DEF.name()), sameInstance(shared.get(ControlKey.BEC_DEF.name())));
		assertThat(run.get(ControlKey.MAX_NUM_POLY.name()), equalTo(shared.get(ControlKey.MAX_NUM_POLY.name())));
	}

	@Test
	void testRunControlFileMayNotOverrideSharedEntries() throws Exception {
		var parser = new ForwardControlParser();
		var shared = parser.parseSharedByName(List.of(CONTROL_FILE), sharedResolver);
		writeStandControlFile("009 coe/Becdef.dat");

		var ex = assertThrows(
				IllegalArgumentException.class,
				() -> parser.parseForRun(shared, List.of(STAND_CONTROL_FILE), runResolver)
		);
		assertThat(ex.getMessage(), containsString(ControlKey.BEC_DEF.name()));
	}

	@Test
	void testCacheBuildsEachSharedMapOnce() throws Exception {
		var cache = ControlMapCache.getInstance();
		var source = new ControlMapCache.Source("test", sharedResolver, List.of(CONTROL_FILE));

		Map<String, Object> first = cache.getShared(source, new ForwardControlParser());
		Map<String, Object> second = cache.getShared(source, new ForwardControlParser());
		Map<String, Object> otherParser = cache.getShared(source, new TestProcessingControlParser());

		assertThat(second, sameInstance(first));
		assertThat(otherParser, not(sameInstance(first)));

		var run = cache.createRunControlMap(source, new ForwardControlParser(), List.of(), runResolver);
		assertThat(run.get(ControlKey.BEC_DEF.name()), sameInstance(first.get(ControlKey.BEC_DEF.name())));
		assertThat(run.get(ControlKey.VDYP_OUTPUT_VDYP_POLYGON.name()).toString(), startsWith(runDir.toString()));
	}
}
