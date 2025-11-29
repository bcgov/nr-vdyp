package ca.bc.gov.nrs.vdyp.application.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DebugSettingsParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.control.NonFipControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.OutputFileLocationResolver;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies.Builder;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class TestStartApplication
		extends VdypStartApplication<TestPolygon, TestLayer, TestSpecies, TestSite, TestDebugSettings> {

	boolean realInit;

	public TestStartApplication(Map<String, Object> controlMap, boolean realInit) {
		this.setControlMap(controlMap);
		this.realInit = realInit;
	}

	@Override
	public void init(
			FileSystemFileResolver resolver, PrintStream writeToIfNoArgs, InputStream readFromIfNoArgs,
			String... controlFilePaths
	) throws IOException, ResourceParseException {
		if (realInit) {
			super.init(resolver, writeToIfNoArgs, readFromIfNoArgs, controlFilePaths);
		}
	}

	@Override
	public void init(FileSystemFileResolver resolver, Map<String, Object> controlMap) throws IOException {
		if (realInit) {
			super.init(resolver, controlMap);
		}
	}

	@Override
	protected NonFipControlParser<TestDebugSettings> getControlFileParser() {
		return new NonFipControlParser<TestDebugSettings>() {

			@Override
			protected List<ControlMapValueReplacer<Object, String>> inputFileParsers() {
				return List.of();
			}

			@Override
			protected List<OutputFileLocationResolver> outputFiles() {
				return List.of();
			}

			@Override
			protected VdypApplicationIdentifier getProgramId() {
				return VdypApplicationIdentifier.VRI_START;
			}

			@Override
			protected DebugSettingsParser<TestDebugSettings> getDebugSettingsParser() {
				return new DebugSettingsParser<TestDebugSettings>() {

					@Override
					protected TestDebugSettings build(Integer[] debugSettingsValues) {
						return new TestDebugSettings(debugSettingsValues);
					}
				};
			}

		};
	}

	@Override
	public void process() throws ProcessingException {
		// Do Nothing
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VRI_START;
	}

	@Override
	protected TestSpecies copySpecies(TestSpecies toCopy, Consumer<Builder<TestSpecies, TestSite, ?>> config) {
		return null;
	}

	@Override
	protected Optional<TestSite> getPrimarySite(TestLayer layer) {
		return Utils.optSafe(layer.getSites().values().iterator().next());
	}

	@Override
	protected float getYieldFactor(TestPolygon polygon) {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	protected String getDefaultControlFileName() {
		return "test.ctr";
	}

	@Override
	public Optional<VdypPolygon> processPolygon(int polygonsRead, TestPolygon polygon) throws ProcessingException {
		return Optional.empty();
	}

}
