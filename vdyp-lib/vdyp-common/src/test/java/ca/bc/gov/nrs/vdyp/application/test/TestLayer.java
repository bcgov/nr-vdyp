package ca.bc.gov.nrs.vdyp.application.test;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypLayer;
import ca.bc.gov.nrs.vdyp.model.InputLayer;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;

public class TestLayer extends BaseVdypLayer<TestSpecies, TestSite> implements InputLayer {

	final float crownClosure;
	final Optional<String> primarySp0;

	protected TestLayer(
			PolygonIdentifier polygonIdentifier, LayerType layerType, Optional<Integer> inventoryTypeGroup,
			float crownClosure, Optional<String> primarySp0
	) {
		super(polygonIdentifier, layerType, inventoryTypeGroup);
		this.crownClosure = crownClosure;
		this.primarySp0 = primarySp0;
	}

	@Override
	public float getCrownClosure() {
		return crownClosure;
	}

	@Override
	public Optional<String> getPrimaryGenus() {
		return primarySp0;
	}

	public static TestLayer build(Consumer<TestLayer.Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static class Builder
			extends BaseVdypLayer.Builder<TestLayer, TestSpecies, TestSite, TestSpecies.Builder, TestSite.Builder> {

		protected Optional<Float> crownClosure = Optional.empty();

		public TestLayer.Builder crownClosure(float crownClosure) {
			this.crownClosure = Optional.of(crownClosure);
			return this;
		}

		protected Optional<String> primarySp0 = Optional.empty();

		public TestLayer.Builder primaryGenus(String primarySp0) {
			this.primarySp0 = Optional.of(primarySp0);
			return this;
		}

		@Override
		protected void check(Collection<String> errors) {
			super.check(errors);
			requirePresent(crownClosure, "crownClosure", errors);
		}

		@Override
		protected TestLayer doBuild() {

			return (new TestLayer(
					polygonIdentifier.get(), //
					layerType.get(), //
					inventoryTypeGroup, //
					crownClosure.get(), //
					primarySp0
			));
		}

		@Override
		protected TestSpecies buildSpecies(Consumer<TestSpecies.Builder> config) {
			return TestSpecies.build(sb -> {
				sb.polygonIdentifier(polygonIdentifier.get());
				sb.layerType(layerType.get());
				config.accept(sb);
			});
		}

	}
}