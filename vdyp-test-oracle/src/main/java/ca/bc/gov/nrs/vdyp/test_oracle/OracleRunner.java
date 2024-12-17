package ca.bc.gov.nrs.vdyp.test_oracle;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

		// loop over children of inputDir
		for (var originalSubdir : Files.newDirectoryStream(inputDir, (d) -> Files.isDirectory(d))) {

			var dirname = originalSubdir.getFileName();
			var tempSubdir = tempDir.resolve(dirname);
			var inputSubdir = tempSubdir.resolve("input");
			var paramSubdir = inputSubdir;
			var outputSubdir = tempSubdir.resolve("output");

			Files.deleteIfExists(tempSubdir);
			Files.createDirectory(tempSubdir);
			Files.createDirectory(outputSubdir);

			// this is the most concise way I could find to "copy the content of a directory" in Java
			try (var files = Files.walk(originalSubdir)) {
				files.filter(source -> {
					return !source.getFileName().startsWith(".");
				})
					.forEach(source -> {
							Path destination = inputSubdir.resolve(originalSubdir.relativize(source));
						try {
							Files.copy(source, destination);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
			}

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

			// TODO Copy files for test case to output dir
		}

	}

	protected CompletableFuture<Void> run(ProcessBuilder builder) throws IOException {
		return builder.start().onExit().thenAccept(proc -> {
		});
	}
}
