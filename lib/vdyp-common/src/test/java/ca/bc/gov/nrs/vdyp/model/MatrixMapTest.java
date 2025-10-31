package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MatrixMapTest {

	@Test
	void testContructNoDimensionsFails() {
		List<List<Object>> dims = Collections.emptyList();
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims)
		);
	}

	@Test
	@Disabled("No longer enforce this restriction as empty maps have a use")
	void testContructEmptyDimensionsFails() {
		var dim1 = Collections.emptyList();
		var dims = Arrays.asList(dim1);
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims)
		);
	}

	@Test
	void testNewMapIsEmpty() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var result = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		assertTrue(result.all(Optional::isEmpty), "All entries empty");
	}

	@Test
	void testDefaultIsEmpty() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		var result = map.getM("a", 2);

		assertThat(result, notPresent());
	}

	@Test
	void testCanRetrieveAValue() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		map.putM(Optional.of('Z'), "a", 2);
		var result = map.getM("a", 2);

		assertThat(result, present(is('Z')));
	}

	@Test
	void testGetIndex() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		assertThat(map.getIndex("a", 1), is(0));
		assertThat(map.getIndex("b", 1), is(1));
		assertThat(map.getIndex("a", 2), is(2));
		assertThat(map.getIndex("b", 2), is(3));

		assertThrows(IllegalArgumentException.class, () -> map.getIndex("c", 2));
		assertThrows(IllegalArgumentException.class, () -> map.getIndex("b", 3));
	}

	@Test
	void testFullMap() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var dims = Arrays.asList(dim1, dim2);
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		map.putM(Optional.of('W'), "a", 1);
		map.putM(Optional.of('X'), "a", 2);
		map.putM(Optional.of('Y'), "b", 1);
		map.putM(Optional.of('Z'), "b", 2);

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
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		map.putM(Optional.of('W'), "a", 1);
		map.putM(Optional.of('X'), "a", 2);
		map.putM(Optional.of('Y'), "b", 1);

		map.setAll(Optional.of('A'));

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
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

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
		MatrixMap<Optional<Character>> map = new MatrixMapImpl<>(k -> Optional.empty(), dims);

		map.putM(Optional.of('X'), "a");

		var result = MatrixMap
				.<String, Optional<Character>, Character>cast(map, v -> v.orElse(null), Optional::ofNullable);
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
		var map = new MatrixMapImpl<Optional<Character>>(k -> Optional.empty(), dims);

		assertThrows(ClassCastException.class, () -> MatrixMap.cast(map, v -> v.orElse(null), Optional::ofNullable));

	}

	@Test
	void testBuildFromMapOfMaps() {
		var dim1 = Arrays.asList("a", "b");
		var dim2 = Arrays.asList(1, 2);
		var map = new MatrixMap2Impl<String, Integer, Optional<Character>>(dim1, dim2, (k1, k2) -> Optional.empty());

		Map<String, Map<Integer, Character>> nestedMap = new HashMap<>();
		nestedMap.put("a", Collections.singletonMap(1, 'X'));
		nestedMap.put("b", Collections.singletonMap(2, 'Y'));

		map.addAll(nestedMap, Optional::of);

		assertThat(map.getM("a", 1), present(is('X')));
		assertThat(map.getM("a", 2), notPresent());
		assertThat(map.getM("b", 1), notPresent());
		assertThat(map.getM("b", 2), present(is('Y')));
	}

	@Nested
	class EqualityAndHash {

		enum Dim1 {
			D1_A(1), D1_B(2), D1_C(4);

			public final int i;

			private Dim1(int i) {
				this.i = i;
			}

		}

		enum Dim2 {
			D2_A(8), D2_B(16), D2_C(32);

			public final int i;

			private Dim2(int i) {
				this.i = i;
			}
		}

		enum Dim3 {
			D3_A(64), D3_B(128), D3_C(256);

			public final int i;

			private Dim3(int i) {
				this.i = i;
			}
		}

		private static List<Arguments> testData() {

			List<Arguments> result = new LinkedList<>();

			var obj = new MatrixMap2Impl<Dim1, Dim2, Integer>(
					EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
			);
			result.add(Arguments.of(obj, obj, true));
			result.add(
					Arguments.of(
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
							),
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
							), true
					)
			);
			result.add(
					Arguments.of(
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.of(Dim2.D2_A, Dim2.D2_C), (k1, k2) -> k1.i + k2.i
							),
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
							), false
					)
			);
			result.add(
					Arguments.of(
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class),
									(k1, k2) -> k1 == Dim1.D1_A && k2 == Dim2.D2_A ? 0 : k1.i + k2.i
							),
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
							), false
					)
			);
			result.add(
					Arguments.of(
							new MatrixMap2Impl<Dim1, Dim3, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim3.class), (k1, k2) -> k1.i + k2.i / 8
							),
							new MatrixMap2Impl<Dim1, Dim2, Integer>(
									EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
							), false
					)
			);

			return result;

		}

		@Test
		void testEqualsNull() {
			var unit = new MatrixMap2Impl<Dim1, Dim2, Integer>(
					EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
			);
			assertThat(unit.equals(null), is(false));
		}

		@Test
		void testEqualsDifferentClass() {
			var unit = new MatrixMap2Impl<Dim1, Dim2, Integer>(
					EnumSet.allOf(Dim1.class), EnumSet.allOf(Dim2.class), (k1, k2) -> k1.i + k2.i
			);
			assertThat(unit.equals("This is not a matrix map"), is(false));
		}

		@ParameterizedTest
		@MethodSource("testData")
		void testEquals(MatrixMap<?> map1, MatrixMap<?> map2, boolean equal) {
			assertThat(map1.equals(map2), is(equal));
			assertThat(map2.equals(map1), is(equal));
		}

		@ParameterizedTest
		@MethodSource("testData")
		void testHashCode(MatrixMap<?> map1, MatrixMap<?> map2, boolean equal) {
			if (equal) {
				assertThat(map1.hashCode(), equalTo(map2.hashCode()));
			} else {
				assertThat(map1.hashCode(), not(equalTo(map2.hashCode())));
			}
		}
	}
}
