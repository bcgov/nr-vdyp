package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MatrixMapTest {

	@Test
	void testContructNoDimensionsFails() {
		List<List<Object>> dims = Collections.emptyList();
		Assertions.assertThrows(IllegalArgumentException.class, () -> new MatrixMapImpl<Character>(dims));
	}

	@Test
	void testContructEmptyDimensionsFails() {
		var dim1 = Collections.emptyList();
		var dims = Arrays.asList(dim1);
		Assertions.assertThrows(IllegalArgumentException.class, () -> new MatrixMapImpl<Character>(dims));
	}

	@Test
	void testNewMapIsEmpty() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var result = new MatrixMapImpl<Character>(dims);
		assertThat(result, hasProperty("empty", is(true)));
		assertThat(result, hasProperty("full", is(false)));
	}

	@Test
	void testDefaultIsEmpty() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		var result = map.getM("a", 2);

		assertThat(result, notPresent());
	}

	@Test
	void testCanRetrieveAValue() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		map.putM('Z', "a", 2);
		var result = map.getM("a", 2);

		assertThat(result, present(is('Z')));
	}

	@Test
	void testWithOneValueIsNeitherFullNorEmpty() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		map.putM('Z', "a", 2);

		assertThat(map, hasProperty("full", is(false)));
		assertThat(map, hasProperty("empty", is(false)));
	}

	@Test
	void testGetIndex() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		assertThat(map.getIndex("a", 1), present(is(0)));
		assertThat(map.getIndex("b", 1), present(is(1)));
		assertThat(map.getIndex("a", 2), present(is(2)));
		assertThat(map.getIndex("b", 2), present(is(3)));
	}

	@Test
	void testFullMap() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		map.putM('W', "a", 1);
		map.putM('X', "a", 2);
		map.putM('Y', "b", 1);
		map.putM('Z', "b", 2);

		assertThat(map, hasProperty("full", is(true)));
		assertThat(map, hasProperty("empty", is(false)));

		assertThat(map.getM("a", 1), present(is('W')));
		assertThat(map.getM("a", 2), present(is('X')));
		assertThat(map.getM("b", 1), present(is('Y')));
		assertThat(map.getM("b", 2), present(is('Z')));
	}

	@Test
	void testSetAll() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		map.putM('W', "a", 1);
		map.putM('X', "a", 2);
		map.putM('Y', "b", 1);

		map.setAll('A');

		assertThat(map, hasProperty("full", is(true)));
		assertThat(map, hasProperty("empty", is(false)));

		assertThat(map.getM("a", 1), present(is('A')));
		assertThat(map.getM("a", 2), present(is('A')));
		assertThat(map.getM("b", 1), present(is('A')));
		assertThat(map.getM("b", 2), present(is('A')));
	}

	@Test
	void testEachKey() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		var result = new ArrayList<Object[]>();
		map.eachKey(result::add);
		assertThat(
				result,
				containsInAnyOrder(
						arrayContaining("a", 1), arrayContaining("b", 1), arrayContaining("a", 2),
						arrayContaining("b", 2)
				)
		);
	}

	@Test
	void testToMap() {
		var dim1 = Arrays.asList("a", "b");
		var dims = Arrays.asList(dim1);
		var map = new MatrixMapImpl<Character>(dims);

		map.putM('X', "a");

		var result = MatrixMap.cast(map, String.class);
		assertThat(result, isA(Map.class));
		assertThat(result.size(), is(2));
		assertThat(result.containsKey("a"), is(true));
		assertThat(result.containsKey("c"), is(false));
		assertThat(result.containsValue('X'), is(true));
		assertThat(result.containsValue('Y'), is(false));
		assertThat(result.get("a"), is('X'));
		assertThat(result.get("c"), nullValue());
		assertThat(result.isEmpty(), is(false));

		result.put("b", 'Y');
		assertThat(map.getM("b"), present(is('Y')));

		result.remove("b");
		assertThat(map.getM("b"), notPresent());

	}

	@Test
	void testToMapMultipleDimensions() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Character>(dims);

		assertThrows(ClassCastException.class, () -> MatrixMap.cast(map, String.class));

	}

	@Test
	void testBuildFromMapOfMaps() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var map = new MatrixMap2Impl<String, Integer, Character>(dim1, dim2);
		
		var nestedMap = new HashMap<String, Map<Integer, Character>>();
		nestedMap.put("a", Collections.singletonMap(1, 'X'));
		nestedMap.put("b", Collections.singletonMap(2, 'Y'));
		
		map.addAll(nestedMap);
		
		assertThat(map.getM("a", 1), present(is('X')));
		assertThat(map.getM("a", 2), notPresent());
		assertThat(map.getM("b", 1), notPresent());
		assertThat(map.getM("b", 2), present(is('Y')));
	}
}
