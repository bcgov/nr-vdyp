package ca.bc.gov.nrs.vdyp.controlmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.DoubleCoefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMapImpl;
import ca.bc.gov.nrs.vdyp.model.ModelCoefficients;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.projection.ProcessingDebugSettings;

class SharedControlMapValuesTest {

	private static Map<String, Object> readOnly(String key, Object value) {
		var map = new HashMap<String, Object>();
		map.put(key, value);
		SharedControlMapValues.makeReadOnly(map);
		return map;
	}

	@Test
	void testMatrixKeepsInterfaceAndValuesAndRejectsChanges() {
		MatrixMap2<String, Region, Coefficients> matrix = new MatrixMap2Impl<>(
				List.of("AC", "B"), List.of(Region.COASTAL, Region.INTERIOR),
				(k1, k2) -> new Coefficients(new float[] { 0.1f, 0.2f }, 1)
		);

		var result = readOnly("M", matrix).get("M");

		assertThat(result, instanceOf(MatrixMap2.class));
		@SuppressWarnings("unchecked")
		var readOnly = (MatrixMap2<String, Region, Coefficients>) result;
		assertThat(readOnly.get("B", Region.INTERIOR).getCoe(2), is(0.2f));
		assertThat(readOnly.hasM("B", Region.INTERIOR), is(true));

		assertThrows(UnsupportedOperationException.class, () -> readOnly.put("B", Region.INTERIOR, null));
		assertThrows(UnsupportedOperationException.class, () -> readOnly.remove("B", Region.INTERIOR));
		assertThrows(UnsupportedOperationException.class, () -> readOnly.setAll((Coefficients) null));
		assertThrows(UnsupportedOperationException.class, () -> readOnly.setAll((k1, k2) -> null));

		// The cells are read only too, including views of them with another index
		var cell = readOnly.get("AC", Region.COASTAL);
		assertThrows(UnsupportedOperationException.class, () -> cell.setCoe(1, 5f));
		assertThrows(UnsupportedOperationException.class, () -> cell.scalarInPlace(x -> x * 2));
		var reindexed = cell.reindex(0);
		assertThrows(UnsupportedOperationException.class, () -> reindexed.setCoe(0, 5f));
		assertThat(reindexed.getCoe(0), is(0.1f));

		// Operations that make a new object still give a modifiable result
		var doubled = cell.scalar(x -> x * 2);
		doubled.setCoe(1, 9f);
		assertThat(doubled.getCoe(1), is(9f));
		assertThat(cell.getCoe(1), is(0.1f));
	}

	@Test
	void testDoubleCoefficientsKeepTheirTypeAndRejectChanges() {
		// DoubleCoefficients is a List, but code casts it back to DoubleCoefficients, so it must not become a plain
		// List
		var map = new HashMap<String, DoubleCoefficients>();
		map.put("AC", new DoubleCoefficients(new double[] { 0.1, 0.2 }, 1));

		@SuppressWarnings("unchecked")
		var readOnlyMap = (Map<String, DoubleCoefficients>) SharedControlMapValues.readOnlyValue(map);

		var cell = readOnlyMap.get("AC");
		assertThat(cell, instanceOf(DoubleCoefficients.class));
		assertThat(cell.getCoe(2), is(0.2));
		assertThrows(UnsupportedOperationException.class, () -> cell.setCoe(1, 5.0));
		var reindexed = cell.reindex(0);
		assertThrows(UnsupportedOperationException.class, () -> reindexed.setCoe(0, 5.0));
		assertThat(cell.scalar(x -> x * 2).getCoe(2), is(0.4));
	}

	@Test
	void testCoefficientsSubclassWithoutReadOnlyFormIsRejected() {
		var vector = new UtilizationVector();
		assertThrows(IllegalArgumentException.class, () -> SharedControlMapValues.readOnlyValue(vector));
	}

	@Test
	void testNonprimaryHLCoefficientsKeepTheirType() {
		MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> matrix = new MatrixMap3Impl<>(
				List.of("AC"), List.of("B"), List.of(Region.COASTAL),
				(k1, k2, k3) -> Optional.of(new NonprimaryHLCoefficients(new float[] { 1.5f, 2.5f }, 3))
		);

		@SuppressWarnings("unchecked")
		var readOnly = (MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>) readOnly("M", matrix)
				.get("M");

		var cell = readOnly.get("AC", "B", Region.COASTAL).orElseThrow();
		assertThat(cell.getEquationIndex(), is(3));
		assertThat(cell.getCoe(2), is(2.5f));
		assertThrows(UnsupportedOperationException.class, () -> cell.setCoe(1, 5f));
	}

	@Test
	void testMapsAndListsAreUnmodifiableWithReadOnlyValues() {
		var map = new HashMap<Integer, ModelCoefficients>();
		map.put(1, new ModelCoefficients(7, new Coefficients(new float[] { 3f }, 0)));
		var list = new ArrayList<Coefficients>(List.of(new Coefficients(new float[] { 4f }, 0)));

		@SuppressWarnings("unchecked")
		var readOnlyMap = (Map<Integer, ModelCoefficients>) readOnly("MAP", map).get("MAP");
		@SuppressWarnings("unchecked")
		var readOnlyList = (List<Coefficients>) SharedControlMapValues.readOnlyValue(list);

		var mapCoefficients = readOnlyMap.get(1).getCoefficients();
		assertThat(readOnlyMap.get(1).getModel(), is(7));
		assertThat(mapCoefficients.getCoe(0), is(3f));
		assertThrows(UnsupportedOperationException.class, () -> readOnlyMap.put(2, null));
		assertThrows(UnsupportedOperationException.class, () -> mapCoefficients.setCoe(0, 1f));

		var listCoefficients = readOnlyList.get(0);
		assertThat(listCoefficients.getCoe(0), is(4f));
		assertThrows(UnsupportedOperationException.class, () -> readOnlyList.add(null));
		assertThrows(UnsupportedOperationException.class, () -> listCoefficients.setCoe(0, 1f));
	}

	@Test
	void testMapSubclassKeepsItsBehaviour() {
		// Like the default result of SiteCurveAgeMaximumParser, which answers for keys it does not hold
		var map = new HashMap<Integer, String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String get(Object key) {
				return containsKey(key) ? super.get(key) : "default";
			}
		};
		map.put(1, "one");

		@SuppressWarnings("unchecked")
		var readOnlyMap = (Map<Integer, String>) SharedControlMapValues.readOnlyValue(map);

		assertThat(readOnlyMap.get(1), is("one"));
		assertThat(readOnlyMap.get(99), is("default"));
		assertThrows(UnsupportedOperationException.class, () -> readOnlyMap.put(2, "two"));
	}

	@Test
	void testMatrixMapAsMapView() {
		// Like the BREAKAGE and SMALL_COMP_* entries, which are one dimension MatrixMaps seen as a Map
		var oneDimension = new MatrixMapImpl<Coefficients>(
				k -> new Coefficients(new float[] { 6f }, 0), List.of("AC", "B")
		);
		Map<String, Coefficients> view = MatrixMap.cast(oneDimension);

		@SuppressWarnings("unchecked")
		var readOnlyView = (Map<String, Coefficients>) SharedControlMapValues.readOnlyValue(view);

		assertThat(readOnlyView.keySet(), contains("AC", "B"));
		var cell = readOnlyView.get("B");
		assertThat(cell.getCoe(0), is(6f));
		assertThrows(UnsupportedOperationException.class, () -> cell.setCoe(0, 1f));
		assertThrows(UnsupportedOperationException.class, () -> readOnlyView.put("AC", null));
	}

	@Test
	void testSharedReferencesStayShared() {
		var shared = new Coefficients(new float[] { 1f }, 0);
		var map = new HashMap<String, Object>();
		map.put("A", shared);
		map.put("B", new HashMap<>(Map.of(1, shared)));

		SharedControlMapValues.makeReadOnly(map);

		@SuppressWarnings("unchecked")
		var nested = (Map<Integer, Coefficients>) map.get("B");
		assertThat(map.get("A"), not(sameInstance(shared)));
		assertThat(nested.get(1), sameInstance(map.get("A")));
	}

	@Test
	void testDebugSettingsAreMadeReadOnly() {
		var settings = new ProcessingDebugSettings(new Integer[] { 1, 2 });

		var result = readOnly("DEBUG", settings).get("DEBUG");

		assertThat(result, sameInstance(settings));
		assertThat(settings.isReadOnly(), is(true));
		assertThat(settings.getValue(2), is(2));
		assertThrows(UnsupportedOperationException.class, () -> settings.setValue(2, 3));
	}

	@Test
	void testImmutableValuesAreKept() {
		var value = "vp_grow.dat";
		assertThat(readOnly("FILE", value).get("FILE"), sameInstance(value));
		assertThat(readOnly("REGION", Region.COASTAL).get("REGION"), is(Region.COASTAL));
		assertThat(readOnly("NULL", null).get("NULL"), is((Object) null));
	}

	@Test
	void testUnknownTypeIsRejected() {
		var value = new StringBuilder("x");
		var ex = assertThrows(IllegalArgumentException.class, () -> readOnly("ODD", value));
		assertThat(ex.getMessage(), containsString("ODD"));
		assertThat(ex.getMessage(), containsString(StringBuilder.class.getName()));
	}
}
