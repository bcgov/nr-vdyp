package ca.bc.gov.nrs.vdyp.test_oracle;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

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
			System.err.println(e.getMessage());
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
			copyDir(originalSubdir, inputSubdir);

			// Make sure the params file includes the flag to save the intermediate data
			var paramFile = paramSubdir.resolve("parms.txt");
			boolean hasSaveIntermediariesParam;
			try (Stream<String> stream = Files.lines(paramFile)) {
				hasSaveIntermediariesParam = stream.anyMatch(line -> line.matches("\\-v7save yes"));
			}
			if (!hasSaveIntermediariesParam) {
				Files.write(paramFile, "-v7save yes\r\n".getBytes(), StandardOpenOption.APPEND);
			}

			var builder = new ProcessBuilder();

			builder.directory(inputSubdir.toFile());

			builder.environment().put(INPUT_DIR_ENV, inputSubdir.toAbsolutePath().toString());
			builder.environment().put(OUTPUT_DIR_ENV, outputSubdir.toAbsolutePath().toString());
			builder.environment().put(INSTALL_DIR_ENV, installDir.toAbsolutePath().toString());
			builder.environment().put(PARAM_DIR_ENV, paramSubdir.toAbsolutePath().toString());

			builder.command("RunVDYP7.cmd");

			System.out.format("Running %s", builder.command());
			System.out.format("PWD=%s", builder.directory());
			System.out.format("ENV=%s", builder.environment());

			run(builder).get();

			copyOutput(originalSubdir, inputSubdir, outputSubdir, finalSubdir);
		}

	}

	/**
	 * Create the final output
	 * 
	 * @param originalSubdir
	 * @param inputSubdir
	 * @param outputSubdir
	 * @param finalSubdir
	 * @throws IOException
	 */
	void copyOutput(Path originalSubdir, Path inputSubdir, Path outputSubdir, Path finalSubdir) throws IOException {
		deleteDir(finalSubdir);
		Files.createDirectory(finalSubdir);

		var inputDir = finalSubdir.resolve("input");
		var fipDir = finalSubdir.resolve("fipInput");
		var vriDir = finalSubdir.resolve("vriInput");
		var adjustDir = finalSubdir.resolve("adjustInput");
		var forwardDir = finalSubdir.resolve("forwardInput");
		var backDir = finalSubdir.resolve("backInput");
		var forwardOutDir = finalSubdir.resolve("forwardOutput");
		var backOutDir = finalSubdir.resolve("backOutput");
		var outputDir = finalSubdir.resolve("output");
		var otherDir = finalSubdir.resolve("other");

		Files.createDirectory(inputDir);
		Files.createDirectory(fipDir);
		Files.createDirectory(vriDir);
		Files.createDirectory(adjustDir);
		Files.createDirectory(forwardDir);
		Files.createDirectory(backDir);
		Files.createDirectory(forwardOutDir);
		Files.createDirectory(backOutDir);
		Files.createDirectory(outputDir);
		Files.createDirectory(otherDir);

		copyDir(inputSubdir, inputDir);

		copyFiles(outputSubdir, fipDir, file -> file.getFileName().toString().contains("_FIP"));
		copyFiles(outputSubdir, vriDir, file -> file.getFileName().toString().contains("_VRI"));
		copyFiles(outputSubdir, adjustDir, file -> file.getFileName().toString().contains("_AJST"));
		copyFiles(outputSubdir, forwardDir, file -> file.getFileName().toString().contains("_7INP"));
		copyFiles(outputSubdir, forwardOutDir, file -> file.getFileName().toString().contains("_7OUT"));
		copyFiles(outputSubdir, backDir, file -> file.getFileName().toString().contains("_BINP"));
		copyFiles(outputSubdir, backOutDir, file -> file.getFileName().toString().contains("_BOUT"));
		copyFiles(outputSubdir, backOutDir, file -> file.getFileName().toString().contains("_BOUT"));
		copyFiles(outputSubdir, otherDir, file -> file.getFileName().toString().contains("_GROW"));

		copyFiles(outputSubdir, outputDir, file -> file.getFileName().toString().startsWith("Output_"));
	}

	void copyFiles(Path source, Path destination, DirectoryStream.Filter<Path> filter) throws IOException {
		try (var dirStream = Files.newDirectoryStream(source, filter)) {
			for (var file : dirStream) {
				FileUtils.copyFileToDirectory(file.toFile(), destination.toFile());
			}
		}
	}

	void copyDir(Path source, Path destination) throws IOException {
		FileUtils.copyDirectory(source.toFile(), destination.toFile());
	}

	void deleteDir(Path dir) throws IOException {
		FileUtils.deleteDirectory(dir.toFile());
	}

	protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
		return builder.start().onExit().thenAccept(proc -> {
		});
	}
}
