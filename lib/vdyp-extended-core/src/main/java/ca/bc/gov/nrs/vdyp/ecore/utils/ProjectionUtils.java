package ca.bc.gov.nrs.vdyp.ecore.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUtils {

	public static final Logger logger = LoggerFactory.getLogger(ProjectionUtils.class);

	public static void prepareProjectionTypeFolder(InputStream is, Path executionFolder, String stepName)
			throws IOException {

		try (var zis = new ZipInputStream(is)) {
			Path stepFolderPath = Files.createDirectory(Path.of(executionFolder.toString(), stepName));

			ZipEntry zipEntry;
			while ( (zipEntry = zis.getNextEntry()) != null) {

				Path resolvedPath = stepFolderPath.resolve(zipEntry.getName()).normalize();
				if (!resolvedPath.startsWith(stepFolderPath)) {
					throw new IOException(
							"The requested zip-entry '" + zipEntry.getName()
									+ "' does not belong to the requested destination"
					);
				}
				if (zipEntry.getName().startsWith("__MACOSX")) {
					continue;
				}
				if (zipEntry.isDirectory()) {
					Files.createDirectories(resolvedPath);
				} else {
					if (!Files.isDirectory(resolvedPath.getParent())) {
						Files.createDirectories(resolvedPath.getParent());
					}
					try (FileOutputStream outStream = new FileOutputStream(resolvedPath.toFile())) {
						IOUtils.copy(zis, outStream);
					}
				}
			}
		}

		// System.out.println("done");
	}
}
