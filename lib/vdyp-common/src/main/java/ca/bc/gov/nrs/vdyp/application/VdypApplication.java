package ca.bc.gov.nrs.vdyp.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of all VDYP applications.
 *
 * <p>
 * Expects <tt>application.properties</tt> to be on the class path.
 *
 * @author Michael Junkin, Vivid Solutions
 */
public abstract class VdypApplication extends VdypComponent {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(VdypApplication.class);

	public abstract VdypApplicationIdentifier getId();

	/**
	 * @returns the ordinal of the application's identifier. It will agree with the JPROGRAM values from the FORTRAN
	 *          implementation.
	 */
	public int getJProgramNumber() {
		return getId().getJProgramNumber();
	}

	public static List<String> getControlMapFileNames(
			final String[] args, final String defaultName, final VdypApplicationIdentifier appId,
			PrintStream writeToIfNoArgs,
			InputStream readFromIfNoArgs
	) throws IOException {
		List<String> controlFileNames;
		if (args.length == 0) {
			writeToIfNoArgs.printf(
					"Enter name of %s control file (or RETURN for %s) or *name for both): ", appId.toString(),
					defaultName
			);

			controlFileNames = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(readFromIfNoArgs))) {
				String userResponse = br.readLine().strip();
				if (userResponse.length() == 0 || userResponse.equals("*")) {
					controlFileNames.add(defaultName);
				} else if (userResponse.startsWith("*")) {
					controlFileNames.add(defaultName);

					userResponse = userResponse.substring(1);
					controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
				} else {
					controlFileNames.addAll(Arrays.asList(userResponse.split("\s+")));
				}
			}
		} else {
			controlFileNames = Arrays.stream(args)
					.flatMap(arg -> arg.startsWith("*") ? Stream.of(defaultName, arg.substring(1)) : Stream.of(arg))
					.filter(s -> !s.isEmpty())
					.toList();
		}
		return controlFileNames;
	}

}
