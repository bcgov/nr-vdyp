package ca.bc.gov.nrs.vdyp.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipOutputFileResolver implements ConcreteFileResolver {

	private record OutputStreamDetails(Path outputStreamLocation, OutputStream outputStream) {
	}

	private Map<String, OutputStreamDetails> entryOutputStreams = new HashMap<>();

	@Override
	public Path toPath(String filename) {
		return Path.of(filename).toAbsolutePath();
	}

	@Override
	public InputStream resolveForInput(String filename) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream resolveForOutput(String filename) throws IOException {
		Path tempFile = Files.createTempFile("vdyp", filename);
		OutputStream entryOutputStream = Files.newOutputStream(tempFile);
		entryOutputStreams.put(filename, new OutputStreamDetails(tempFile, entryOutputStream));

		return entryOutputStream;
	}

	@Override
	public String toString(String filename) throws IOException {
		return String.format("zip:%s", toPath(filename));
	}

	private static final int BUFFER_SIZE = 64 * 1024;

	public void generate(Path zipFile) throws IOException {
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {

			for (Map.Entry<String, OutputStreamDetails> e : entryOutputStreams.entrySet()) {
				close(e.getValue().outputStream);

				ZipEntry entry = new ZipEntry(e.getKey());
				try (
						InputStream entryReader = Files
								.newInputStream(e.getValue().outputStreamLocation, StandardOpenOption.READ)
				) {
					zipOutputStream.putNextEntry(entry);

					byte[] cBuffer = new byte[BUFFER_SIZE];
					int nCharsRead = entryReader.read(cBuffer, 0, BUFFER_SIZE);
					while (nCharsRead >= 0) {
						zipOutputStream.write(cBuffer, 0, nCharsRead);
						nCharsRead = entryReader.read(cBuffer, 0, BUFFER_SIZE);
					}
				} finally {
					zipOutputStream.closeEntry();
				}
			}
		}
	}

	public InputStream generateStream() throws IOException {

		Path tempFile = Files.createTempFile("vdypOutputZipFile", ".zip");

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(tempFile.toFile()))) {
			for (Map.Entry<String, OutputStreamDetails> e : entryOutputStreams.entrySet()) {
				close(e.getValue().outputStream);

				ZipEntry entry = new ZipEntry(e.getKey());
				try (
						InputStream entryReader = Files
								.newInputStream(e.getValue().outputStreamLocation, StandardOpenOption.READ)
				) {
					zipOutputStream.putNextEntry(entry);

					byte[] cBuffer = new byte[BUFFER_SIZE];
					int nCharsRead = entryReader.read(cBuffer, 0, BUFFER_SIZE);
					while (nCharsRead >= 0) {
						zipOutputStream.write(cBuffer, 0, nCharsRead);
						nCharsRead = entryReader.read(cBuffer, 0, BUFFER_SIZE);
					}
				} finally {
					zipOutputStream.closeEntry();
				}
			}
		}

		return Files.newInputStream(tempFile);
	}

	private static void close(OutputStream os) {
		try {
			os.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public FileResolver getOutputFileResolver() {
		return this;
	}

	@Override
	public FileResolver getInputFileResolver() {
		throw new UnsupportedOperationException("ZipOutputResolver.getInputFileResolver");
	}
}