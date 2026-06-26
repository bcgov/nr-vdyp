package ca.bc.gov.nrs.vdyp.application.test;

import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.InputPolygon;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;

public class TestPolygon extends BaseVdypPolygon<TestLayer, Optional<Float>, TestSpecies, TestSite>
		implements InputPolygon {
	private final float yieldFactor;

	public static TestPolygon build(Consumer<TestPolygon.Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	protected TestPolygon(
			PolygonIdentifier polygonIdentifier, Optional<Float> percentAvailable, String fiz, BecDefinition bec,
			Optional<PolygonMode> mode, float yieldFactor
	) {
		super(polygonIdentifier, percentAvailable, fiz, bec, mode, Optional.empty());
		this.yieldFactor = yieldFactor;
	}

	protected TestPolygon(
			PolygonIdentifier polygonIdentifier, Optional<Float> percentAvailable, String fiz,
			BecDefinition becIdentifier, Optional<PolygonMode> mode, Optional<Integer> inventoryTypeGroup,
			float yieldFactor
	) {
		super(polygonIdentifier, percentAvailable, fiz, becIdentifier, mode, inventoryTypeGroup);
		this.yieldFactor = yieldFactor;
	}

	public static class Builder extends
			BaseVdypPolygon.Builder<TestPolygon, TestLayer, Optional<Float>, TestSpecies, TestSite, TestLayer.Builder, TestSpecies.Builder, TestSite.Builder> {
		protected Optional<Float> yieldFactor = Optional.empty();

		@Override
		protected TestLayer.Builder getLayerBuilder() {
			var builder = new TestLayer.Builder();
			return builder;
		}

		public Builder yieldFactor(Float yieldFactor) {
			this.yieldFactor = Optional.of(yieldFactor);
			return this;
		}

		@Override
		protected TestPolygon doBuild() {
			return (new TestPolygon(
					polygonIdentifier.get(), //
					percentAvailable.flatMap(x -> x), //
					forestInventoryZone.get(), //
					biogeoclimaticZone.get(), //
					mode, //
					inventoryTypeGroup, //
					yieldFactor.orElse(1f)
			));
		}
	}

	@Override
	public float getYieldFactor() {
		return yieldFactor;
	}
}