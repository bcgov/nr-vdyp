package ca.bc.gov.nrs.vdyp.test_oracle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import ca.bc.gov.nrs.vdyp.common.Utils;

public class OracleRunner {

	static final String INPUT_DIR_OPT = "input-dir";
	static final String OUTPUT_DIR_OPT = "output-dir";
	static final String INSTALL_DIR_OPT = "install-dir";
	static final String TEMP_DIR_OPT = "temp-dir";

	static final String INPUT_DIR_ENV = "InputFileDir";
	static final String OUTPUT_DIR_ENV = "OutputFileDir";
	static final String INSTALL_DIR_ENV = "InstallDir";
	static final String PARAM_DIR_ENV = "ParmsFileDir";

	public static void main(String[] args) throws InterruptedException {
		try {
			var app = new OracleRunner();
			app.run(args);
		} catch (ParseException | IOException | ExecutionException e) {
			e.printStackTrace(System.err);
		}
	}

	public static Path getSourceFile(IntermediateStage stage, ModelObject obj, Layer layer) {
		final String extension = obj == ModelObject.CONTROL ? "ctl" : "dat";
		final String savePrefix = obj == ModelObject.CONTROL ? "" : "SAVE_";
		return Path.of(String.format("%s-%sVDYP7_%s.%s", layer.code, savePrefix, obj.getStageCode(stage), extension));
	}

	public static Path getDestFile(IntermediateStage stage, ModelObject obj, Layer layer) {
		final String extension = obj == ModelObject.CONTROL ? "ctl" : "dat";
		return Path.of(stage.filename, layer.filename, String.format("%s.%s", obj.filename, extension));
	}

	public static enum Layer {
		PRIMARY("primary", "P"), //
		VETERAN("veteran", "V"), //
		YOUNG("young", "Y"), //
		DEAD("dead", "D"), //
		REGENERATION("regen", "R");

		public final String filename;
		public final String code;

		Layer(String name, String code) {
			this.filename = name;
			this.code = code;
		}

		static Layer byCode(String code) {
			for (var e : Layer.values()) {
				if (e.code.equals(code)) {
					return e;
				}
			}
			throw new IllegalArgumentException(String.format("Unknown layer code", code));
		}
	}

	public static enum ModelObject {
		POLYGON("polygon", "P"), //
		LAYER("layer", "L"), //
		SPECIES("species", "S"), //
		SITE("site", "I"), //
		UTILIZATION("util", "U"), //
		ADJUSTMENTS("adjust", "A"), //
		GROW_TO("grow", "GROW") {
			@Override
			String getStageCode(IntermediateStage stage) {
				return this.code;
			};
		},
		COMPATIBILITY("compat", "C"), //
		CONTROL("control", "CTL") {
			@Override
			String getStageCode(IntermediateStage stage) {
				switch (stage) {
				case BACK_INPUT:
					return "BACK";
				case FORWARD_INPUT:
					return "VDYP";
				default:
					throw new IllegalArgumentException("No stage code for control file for stage " + stage);
				}
			};
		};

		public final String filename;
		public final String code;

		ModelObject(String name, String code) {
			this.filename = name;
			this.code = code;
		}

		String getStageCode(IntermediateStage stage) {
			return String.format("%s%s", stage.code, this.code);
		};

		static ModelObject byCode(String code) {
			for (var e : ModelObject.values()) {
				if (e.code.equals(code)) {
					return e;
				}
			}
			throw new IllegalArgumentException(String.format("Unknown model object code", code));
		}
	}

	public static enum IntermediateStage {
		FIP_INPUT("fipInput", "FIP", EnumSet.of(ModelObject.POLYGON, ModelObject.LAYER, ModelObject.SPECIES)), //
		VRI_INPUT(
				"vriInput", "VRI",
				EnumSet.of(ModelObject.POLYGON, ModelObject.LAYER, ModelObject.SPECIES, ModelObject.SITE)
		),
		ADJUST_INPUT(
				"adjustInput", "AJST",
				EnumSet.of(ModelObject.POLYGON, ModelObject.SPECIES, ModelObject.UTILIZATION, ModelObject.ADJUSTMENTS)
		),
		FORWARD_INPUT(
				"forwardInput", "7INP",
				EnumSet.of(
						ModelObject.POLYGON, ModelObject.SPECIES, ModelObject.UTILIZATION, ModelObject.GROW_TO,
						ModelObject.CONTROL
				)
		),
		FORWARD_OUTPUT(
				"forwardOutput", "7OUT",
				EnumSet.of(ModelObject.POLYGON, ModelObject.SPECIES, ModelObject.UTILIZATION, ModelObject.COMPATIBILITY)
		),
		BACK_INPUT(
				"backInput", "BINP",
				EnumSet.of(
						ModelObject.POLYGON, ModelObject.SPECIES, ModelObject.UTILIZATION, ModelObject.GROW_TO,
						ModelObject.CONTROL
				)
		),
		BACK_OUTPUT(
				"backOutput", "BOUT",
				EnumSet.of(ModelObject.POLYGON, ModelObject.SPECIES, ModelObject.UTILIZATION, ModelObject.COMPATIBILITY)
		);

		public final String filename;
		public final String code;
		public final Set<ModelObject> files;

		IntermediateStage(String name, String code, Set<ModelObject> files) {
			this.filename = name;
			this.code = code;
			this.files = Collections.unmodifiableSet(files);
		}

		static IntermediateStage byCode(String code) {
			for (var e : IntermediateStage.values()) {
				if (e.code.equals(code)) {
					return e;
				}
			}
			throw new IllegalArgumentException(String.format("Unknown intermediate stage code", code));
		}
	}

	public void run(String[] args) throws InterruptedException, ParseException, IOException, ExecutionException {
		var options = new Options();

		options.addRequiredOption("i", INPUT_DIR_OPT, true, "Directory containing input sets");
		options.addRequiredOption("o", OUTPUT_DIR_OPT, true, "Directory in which to write output sets");
		options.addRequiredOption("t", TEMP_DIR_OPT, true, "Directory to use for temporary data");
		options.addRequiredOption("d", INSTALL_DIR_OPT, true, "Directory containing the installation of VDYP7");

		CommandLineParser parser = new DefaultParser();

		CommandLine cmd = parser.parse(options, args);

		var inputDir = Path.of(cmd.getOptionValue(INPUT_DIR_OPT));
		var outputDir = Path.of(cmd.getOptionValue(OUTPUT_DIR_OPT));
		var installDir = Path.of(cmd.getOptionValue(INSTALL_DIR_OPT));
		var tempDir = Path.of(cmd.getOptionValue(TEMP_DIR_OPT));

		if (Files.notExists(tempDir))
			Files.createDirectory(tempDir);
		if (Files.notExists(outputDir))
			Files.createDirectory(outputDir);

		// loop over children of inputDir
		for (var originalSubdir : Files.newDirectoryStream(inputDir, (d) -> Files.isDirectory(d))) {

			var dirname = originalSubdir.getFileName();
			var tempSubdir = tempDir.resolve(dirname);
			var inputSubdir = tempSubdir.resolve("input");
			var paramSubdir = inputSubdir;
			var outputSubdir = tempSubdir.resolve("output");
			var finalSubdir = outputDir.resolve(dirname);

			deleteDir(tempSubdir);
			Files.createDirectory(tempSubdir);
			Files.createDirectory(outputSubdir);
			copyDir(originalSubdir, inputSubdir);

			final Map<String, String> env = Utils.constMap(map -> {
				map.put(INPUT_DIR_ENV, inputSubdir.toAbsolutePath().toString());
				map.put(OUTPUT_DIR_ENV, outputSubdir.toAbsolutePath().toString());
				map.put(INSTALL_DIR_ENV, installDir.toAbsolutePath().toString());
				map.put(PARAM_DIR_ENV, paramSubdir.toAbsolutePath().toString());
			});

			// Update parameters file
			var paramFile = paramSubdir.resolve("parms.txt");
			{
				var paramsText = FileUtils.readFileToString(paramFile.toFile(), StandardCharsets.UTF_8);
				// Remove Byte order mark if present
				if (paramsText.startsWith("\uFEFF")) {
					paramsText = paramsText.substring(1, paramsText.length());
				}
				// paramsText = subPattern.matcher(paramsText).replaceAll(m ->
				// env.get(m.group()));
				var saveIntermediatesPattern = Pattern
						.compile("^-v7save\s+yes", Pattern.CASE_INSENSITIVE & Pattern.MULTILINE);
				if (!saveIntermediatesPattern.matcher(paramsText).matches()) {
					paramsText += "\r\n-v7save Yes\r\n";
				}
				FileUtils.writeStringToFile(paramFile.toFile(), paramsText, StandardCharsets.UTF_8);
			}
			var builder = new ProcessBuilder();

			builder.directory(inputSubdir.toFile());

			builder.environment().putAll(env);

			builder.environment().merge(
					"PATH", installDir.toAbsolutePath().toString(), (old, add) -> String.format("%s;%s", old, add)
			);

			// builder.command(inputSubdir.resolve("RunVDYP7.cmd").toAbsolutePath().toString());
			builder.command(
					installDir.resolve("VDYP7Console.exe").toAbsolutePath().toString(), "-p",
					paramSubdir.resolve("parms.txt").toAbsolutePath().toString(), "-env",
					String.format("%s=%s", INPUT_DIR_ENV, env.get(INPUT_DIR_ENV)), "-env",
					String.format("%s=%s", OUTPUT_DIR_ENV, env.get(OUTPUT_DIR_ENV)), "-env",
					String.format("%s=%s", INSTALL_DIR_ENV, env.get(INSTALL_DIR_ENV)), "-env",
					String.format("%s=%s", PARAM_DIR_ENV, env.get(PARAM_DIR_ENV))

			);

			Path intermediateDir = installDir.resolve("VDYP_CFG");
			deleteFiles(intermediateDir, file -> INTERMEDIATE_FILE.matcher(file.getFileName().toString()).matches());

			System.out.format("Running %s\n", builder.command());
			System.out.format("PWD=%s\n", builder.directory());
			System.out.format("ENV=%s\n", builder.environment());

			run(builder).get();

			copyOutput(originalSubdir, inputSubdir, intermediateDir, outputSubdir, finalSubdir);
		}

	}

	static final Pattern INTERMEDIATE_FILE = Pattern.compile(
			"(?<layer>\\w)\\-(?:SAVE_)?VDYP7_(?<suffix>(?:GROW|VDYP|BACK)|(?<stage>\\w+?)(?<obj>\\w))\\.(?<ext>\\w+)"
	);

	/**
	 * Create the final output
	 *
	 * @param originalSubdir
	 * @param inputSubdir
	 * @param outputSubdir
	 * @param finalSubdir
	 * @throws IOException
	 */
	void copyOutput(Path originalSubdir, Path inputSubdir, Path intermediateDir, Path outputSubdir, Path finalSubdir)
			throws IOException {
		deleteDir(finalSubdir);
		Files.createDirectory(finalSubdir);

		var inputDir = finalSubdir.resolve("input");
		var outputDir = finalSubdir.resolve("output");

		Files.createDirectory(inputDir);
		Files.createDirectory(outputDir);

		copyDir(inputSubdir, inputDir);

		final var activeStages = EnumSet.noneOf(IntermediateStage.class);
		final var activeLayers = EnumSet.noneOf(Layer.class);

		findActiveLayersAndStages(intermediateDir, activeStages, activeLayers);

		for (final var stage : activeStages) {
			final var stageDir = finalSubdir.resolve(stage.filename);
			Files.createDirectory(stageDir);

			for (final var layer : activeLayers) {
				final var layerDir = stageDir.resolve(layer.filename);
				Files.createDirectory(layerDir);
				for (var fileType : stage.files) {
					final var sourceFile = intermediateDir.resolve(getSourceFile(stage, fileType, layer));
					final var destFile = finalSubdir.resolve(getDestFile(stage, fileType, layer));
					if (Files.exists(sourceFile)) {
						System.out.printf("  Copying %s to %s", sourceFile, destFile).println();
						Files.copy(sourceFile, destFile);
					}
				}
			}
		}

		copyFiles(outputSubdir, outputDir, file -> file.getFileName().toString().startsWith("Output_"));
	}

	private void findActiveLayersAndStages(
			Path intermediateDir, final EnumSet<IntermediateStage> activeStages, final EnumSet<Layer> activeLayers
	) {
		final var it = FileUtils.iterateFiles(intermediateDir.toFile(), new String[] { "dat", "ctl" }, false);
		while (it.hasNext()) {
			final var file = it.next();
			final var match = INTERMEDIATE_FILE.matcher(file.toPath().getFileName().toString());
			if (match.find()) {
				final var stage = match.group("stage");
				final var layer = match.group("layer");
				if (stage != null) {
					activeStages.add(IntermediateStage.byCode(stage));
				}
				activeLayers.add(Layer.byCode(layer));
			}
		}
	}

	void copyFiles(Path source, Path destination, DirectoryStream.Filter<Path> filter) throws IOException {
		System.out.println("  Copying select files in " + source + " to " + destination);
		try (var dirStream = Files.newDirectoryStream(source, filter)) {
			for (var file : dirStream) {
				System.out.println("    Copying " + file.getFileName());
				FileUtils.copyFileToDirectory(file.toFile(), destination.toFile());
			}
		}
	}

	void deleteFiles(Path parent, DirectoryStream.Filter<Path> filter) throws IOException {
		try (var dirStream = Files.newDirectoryStream(parent, filter)) {
			for (var file : dirStream) {
				FileUtils.deleteQuietly(file.toFile());
			}
		}
	}

	void copyDir(Path source, Path destination) throws IOException {
		System.out.println("  Copying all files in " + source + " to " + destination);
		FileUtils.copyDirectory(source.toFile(), destination.toFile());
	}

	void deleteDir(Path dir) throws IOException {
		FileUtils.deleteDirectory(dir.toFile());
	}

	protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
		return builder.start().onExit().thenAccept(proc -> {
		});
	}

	static class Execution {
		public final Layer layer;
		public final String polygonId;
		public final Path dir;
		public final Map<String, Integer> lines;

		public Execution(String polygonId, Path dir, Map<String, Integer> lines) {
			super();
			this.layer = null;
			this.polygonId = polygonId;
			this.dir = dir;
			this.lines = lines;
		}

	};

	boolean removeInitialLines(Path file, int lines) throws IOException {
		Path temp = file.resolveSibling(file.getFileName().toString() + "_TEMP");
		try (
				var in = Files.newBufferedReader(file);
				var out = Files.newBufferedWriter(temp)
		) {
			for (int i = 0; i < lines; i++) {
				in.readLine();
			}

			in.transferTo(out);
		}
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		if (Files.size(file) == 0) {
			Files.delete(file);
			return true;
		}
		return false;
	}

	public void separateExecutions(Path configDir) throws IOException {
		final List<Execution> executions = new ArrayList<>();

		try (var dirStream = Files.newDirectoryStream(configDir, "execution-*")) {
			for (var executionDir : dirStream) {
				final Map<String, Integer> lineMap = new HashMap<>();
				String polygonId = null;
				try (var fileStream = Files.newDirectoryStream(executionDir, "?-SAVE_*")) {
					for (var file : fileStream) {
						int lines = 0;

						try (var lineIt = FileUtils.lineIterator(file.toFile())) {

							while (lineIt.hasNext()) {
								String line = lineIt.next();
								polygonId = line.substring(0, 21);
								lines++;
							}
						}
						if (lines > 0) {
							lineMap.put(file.getFileName().toString(), lines);
						}
					}
				}
				if (!lineMap.isEmpty()) {
					executions.add(new Execution(polygonId.trim(), executionDir, lineMap));
				}
			}
		}

		// We want the executions in order of number of lines
		Comparator<Execution> comp = Utils.compareUsing(e -> e.lines.values().stream().mapToInt(x -> x).sum());
		executions.sort(comp);

		Execution previous = null;
		for (var execution : executions) {
			Layer layer = null;
			for (var entry : execution.lines.entrySet()) {
				int previousLines = 0;
				if (null != previous) {
					previousLines = previous.lines.getOrDefault(entry.getKey(), 0);
				}
				int actualLines = entry.getValue() - previousLines;
				Path file = execution.dir.resolve(entry.getKey());
				if (!removeInitialLines(file, previousLines)) {
					Layer currentLayer = Layer.byCode(file.getFileName().toString().substring(0, 1));
					if (layer == null) {
						layer = currentLayer;
					} else if (layer != currentLayer) {
						// During an execution, only the files for a single layer should have been appended to and 
						// removeInitialLines should have removed all files which did not change
						throw new IllegalStateException("Could not separate layers in " + execution.dir);
					}
				}
			}

			Files.move(
					execution.dir, execution.dir.resolveSibling(
							"execution-" + execution.polygonId + "-" + layer.filename
					)
			);
			previous = execution;
		}
	}
}
