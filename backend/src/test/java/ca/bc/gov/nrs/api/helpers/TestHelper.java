package ca.bc.gov.nrs.api.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters;
import ca.bc.gov.nrs.vdyp.backend.model.v1.Parameters.ExecutionOption;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessage;
import ca.bc.gov.nrs.vdyp.backend.model.v1.ValidationMessageKind;
import ca.bc.gov.nrs.vdyp.backend.projection.input.HcsvPolygonRecordBean;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.exceptions.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardDataStreamReader;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypUtilizationParser;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.test.TestUtils;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestHelper {

	private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);

	public static final String ROOT_PATH = "/api/v8";

	public Path getResourceFile(Path testResourceFolderPath, String fileName) {

		String resourceFilePath = Path.of(testResourceFolderPath.toString(), fileName).toString();

		URL testFileURL = this.getClass().getResource("/" + resourceFilePath);
		try {
			File resourceFile = new File(testFileURL.toURI());
			return Path.of(resourceFile.getAbsolutePath());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(MessageFormat.format("Unable to find test resource {0}", resourceFilePath));
		}
	}

	public byte[] readZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (zipInputStream.available() > 0) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ( (bytesRead = zipInputStream.read(buffer, 0, 1024)) > 0) {
				baos.write(buffer, 0, bytesRead);
			}
		}

		return baos.toByteArray();
	}

	public InputStream buildTestFile() {
		return new ByteArrayInputStream("Test data".getBytes());
	}

	public Parameters addSelectedOptions(Parameters params, Parameters.ExecutionOption... executionOptions) {
		params.setSelectedExecutionOptions(List.of(executionOptions));
		return params;
	}

	public Parameters addExcludedOptions(Parameters params, ExecutionOption executionOptions) {
		params.setExcludedExecutionOptions(List.of(executionOptions));
		return params;
	}

	public static void verifyMessageSetIs(List<ValidationMessage> validationMessages, ValidationMessageKind... kinds) {
		Set<ValidationMessageKind> expectedKinds = Set.of(kinds);
		Set<ValidationMessageKind> presentKinds = new HashSet<>();

		for (var message : validationMessages) {
			presentKinds.add(message.getKind());
		}

		Assert.assertEquals(expectedKinds, presentKinds);
	}

	public static Parameters buildValidParametersObject() {
		return new Parameters().ageEnd(400).ageStart(1);
	}

	public static Map<String, FieldMetadata> POLYGON_METADATA = new HashMap<>();

	public String buildStandardPolygonCsvStream() {
		return buildPolygonCsvStream();
	}

	public record ValueOverride(String field, Object value) {
	}

	public String buildPolygonCsvStream(ValueOverride... overrides) {
		StringBuffer sb = new StringBuffer();

		Map<String, Object> fieldMap = new TreeMap<String, Object>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1 == null && o2 == null) {
					return 0;
				} else if (o1 == null) {
					return -1;
				} else if (o2 == null) {
					return 1;
				} else {
					return POLYGON_METADATA.get(o1).ordering - POLYGON_METADATA.get(o2).ordering;
				}
			}
		});

		for (var e : STANDARD_POLYGON_MAP.entrySet()) {
			fieldMap.put(e.getKey(), e.getValue());
		}

		for (var o : overrides) {
			if (!fieldMap.containsKey(o.field)) {
				throw new IllegalArgumentException(
						MessageFormat.format("\"{0}\" is not a HscvPolygonRecordBean field", o.field)
				);
			}

			fieldMap.put(o.field, o.value);
		}

		// headers
		for (var e : fieldMap.entrySet()) {
			sb.append(POLYGON_METADATA.get(e.getKey()).columnName).append(',');
		}
		sb.delete(sb.length() - 1, sb.length());
		sb.append('\n');

		// one row of values
		for (var e : fieldMap.entrySet()) {
			sb.append(e.getValue() == null ? "" : e.getValue()).append(',');
		}
		sb.delete(sb.length() - 1, sb.length());

		return sb.toString();
	}

	public static Map<String, Object> STANDARD_POLYGON_MAP = new HashMap<>();

	static {
		STANDARD_POLYGON_MAP.put("polyFeatureId", "13919428");
		STANDARD_POLYGON_MAP.put("mapId", "093C090");
		STANDARD_POLYGON_MAP.put("polygonNumber", Long.valueOf(94833422L));
		STANDARD_POLYGON_MAP.put("orgUnit", "DQU");
		STANDARD_POLYGON_MAP.put("tsaName", "UNK");
		STANDARD_POLYGON_MAP.put("tflName", "UNK");
		STANDARD_POLYGON_MAP.put("inventoryStandardCode", "V");
		STANDARD_POLYGON_MAP.put("tsaNumber", "UNK");
		STANDARD_POLYGON_MAP.put("shrubHeight", Double.valueOf(0.6));
		STANDARD_POLYGON_MAP.put("shrubCrownClosure", Integer.valueOf(10));
		STANDARD_POLYGON_MAP.put("shrubCoverPattern", "3");
		STANDARD_POLYGON_MAP.put("herbCoverTypeCode", "HE");
		STANDARD_POLYGON_MAP.put("herbCoverPercent", Integer.valueOf(35));
		STANDARD_POLYGON_MAP.put("herbCoverPatternCode", "8");
		STANDARD_POLYGON_MAP.put("bryoidCoverPercent", null);
		STANDARD_POLYGON_MAP.put("becZoneCode", "MS");
		STANDARD_POLYGON_MAP.put("cfsEcoZoneCode", Integer.valueOf(14));
		STANDARD_POLYGON_MAP.put("percentStockable", Double.valueOf(50.0));
		STANDARD_POLYGON_MAP.put("yieldFactor", Double.valueOf(1.0));
		STANDARD_POLYGON_MAP.put("nonProductiveDescriptorCode", null);
		STANDARD_POLYGON_MAP.put("bclcsLevel1Code", "V");
		STANDARD_POLYGON_MAP.put("bclcsLevel2Code", "T");
		STANDARD_POLYGON_MAP.put("bclcsLevel3Code", "U");
		STANDARD_POLYGON_MAP.put("bclcsLevel4Code", "TC");
		STANDARD_POLYGON_MAP.put("bclcsLevel5Code", "SP");
		STANDARD_POLYGON_MAP.put("referenceYear", Integer.valueOf(2013));
		STANDARD_POLYGON_MAP.put("yearOfDeath", Integer.valueOf(2013));
		STANDARD_POLYGON_MAP.put("percentDead", Double.valueOf(60.0));
		STANDARD_POLYGON_MAP.put("nonVegCoverType1", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverPercent1", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverPattern1", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverType2", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverPercent2", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverPattern2", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverType3", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverPercent3", null);
		STANDARD_POLYGON_MAP.put("nonVegCoverPattern3", null);
		STANDARD_POLYGON_MAP.put("landCoverClassCode1", "TC");
		STANDARD_POLYGON_MAP.put("landCoverPercent1", Integer.valueOf(100));
		STANDARD_POLYGON_MAP.put("landCoverClassCode2", null);
		STANDARD_POLYGON_MAP.put("landCoverPercent2", null);
		STANDARD_POLYGON_MAP.put("landCoverClassCode3", null);
		STANDARD_POLYGON_MAP.put("landCoverPercent3", null);
	}

	static {
		buildFieldMetadata(POLYGON_METADATA, HcsvPolygonRecordBean.class);
	}

	private record FieldMetadata(String columnName, Integer ordering) {
	}

	private static void buildFieldMetadata(Map<String, FieldMetadata> map, Class<?> clazz) {

		logger.info("Building metadata information for class {}", clazz.getName());

		for (var f : clazz.getDeclaredFields()) {
			if (!Modifier.isStatic(f.getModifiers())) {
				var fieldName = f.getName();
				var columnName = f.getAnnotation(CsvBindByName.class).column();
				var fieldOrdering = f.getAnnotation(CsvBindByPosition.class).position();

				map.put(fieldName, new FieldMetadata(columnName, fieldOrdering));
			}
		}
	}

	public ForwardDataStreamReader buildForwardDataStreamReader(
			InputStream polygonStream, InputStream speciesStream, InputStream utilizationsStream
	) throws IOException {

		var readerControlMap = new HashMap<String, Object>();

		var polygonFile = Files.createTempFile("polygon", null);
		Files.write(polygonFile, polygonStream.readAllBytes());
		var speciesFile = Files.createTempFile("species", null);
		Files.write(speciesFile, speciesStream.readAllBytes());
		var utilizationsFile = Files.createTempFile("utilizations", null);
		Files.write(utilizationsFile, utilizationsStream.readAllBytes());

		var absolutePathFileResolver = new FileSystemFileResolver();
		readerControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
				new VdypPolygonParser().map(polygonFile.toString(), absolutePathFileResolver, readerControlMap)
		);
		readerControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
				new VdypSpeciesParser().map(speciesFile.toString(), absolutePathFileResolver, readerControlMap)
		);
		readerControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				new VdypUtilizationParser().map(utilizationsFile.toString(), absolutePathFileResolver, readerControlMap)
		);

		TestUtils.populateControlMapBecReal(readerControlMap);
		TestUtils.populateControlMapGenusReal(readerControlMap);

		ForwardDataStreamReader reader;
		try {
			reader = new ForwardDataStreamReader(readerControlMap);
		} catch (ProcessingException e) {
			throw new IOException(e.getMessage(), e);
		}

		return reader;
	}
}
