package ca.bc.gov.nrs.vdyp.backend.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUtils {

	public static final Logger logger = LoggerFactory.getLogger(ProjectionUtils.class);
	
	public static void prepareProjectionTypeFolder(Path rootFolder, Path executionFolder, String stepName, String... templateFilesToCopy)
			throws IOException	 {
		
		String rootFolderName = rootFolder.toString();
		String executionFolderName = executionFolder.toString();
		
		Path stepFolderPath = Files.createDirectory(Path.of(executionFolderName, stepName));
		
		for (String templateFileName: templateFilesToCopy) {
			Files.copy(Path.of(rootFolderName, templateFileName), stepFolderPath.resolve(templateFileName));
		}
	}
}
