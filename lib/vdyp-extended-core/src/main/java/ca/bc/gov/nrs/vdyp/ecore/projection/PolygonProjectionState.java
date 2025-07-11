package ca.bc.gov.nrs.vdyp.ecore.projection;

import static ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode.Back;
import static ca.bc.gov.nrs.vdyp.ecore.projection.ProjectionStageCode.Forward;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.ecore.projection.model.Layer;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.ecore.projection.model.enumerations.ProjectionTypeCode;

public class PolygonProjectionState {

	public record ModelReturnCodeKey(ProjectionStageCode stage, ProjectionTypeCode type) {
	}

	private Map<ProjectionTypeCode, Double> startAgeByProjectionType = null;
	private Map<ProjectionTypeCode, Double> endAgeByProjectionType = null;

	// Per-projection type information

	/**
	 * Entries are Optional.empty() if not run; when run, contains Optional.empty() if successful, otherwise contains
	 * the resulting exception.
	 */
	private final Map<ModelReturnCodeKey, Optional<Optional<Throwable>>> processingResultByStageAndProjectionType;

	private final Map<ModelReturnCodeKey, Integer> firstYearValidYieldByProjectionType;

	private Map<ProjectionTypeCode, GrowthModelCode> growthModelByProjectionType;
	private Map<ProjectionTypeCode, ProcessingModeCode> processingModeByProjectionType;
	private Map<ProjectionTypeCode, Double> percentForestedLandUsedByProjectionType;
	private Map<ProjectionTypeCode, Double> yieldFactorByProjectionType;

	private final Map<String /* layer id */, Integer> firstYearYieldsDisplayedByLayer;

	private Path executionFolder = null;

	public PolygonProjectionState() {

		startAgeByProjectionType = new HashMap<>();
		endAgeByProjectionType = new HashMap<>();

		growthModelByProjectionType = new HashMap<>();
		processingModeByProjectionType = new HashMap<>();
		percentForestedLandUsedByProjectionType = new HashMap<>();
		yieldFactorByProjectionType = new HashMap<>();
		processingResultByStageAndProjectionType = new HashMap<>();
		firstYearValidYieldByProjectionType = new HashMap<>();

		for (ProjectionStageCode s : ProjectionStageCode.values()) {
			for (ProjectionTypeCode t : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
				growthModelByProjectionType.put(t, GrowthModelCode.UNKNOWN);
				processingModeByProjectionType.put(t, ProcessingModeCode.getDefault());
				percentForestedLandUsedByProjectionType.put(t, null);
				yieldFactorByProjectionType.put(t, null);
				processingResultByStageAndProjectionType.put(new ModelReturnCodeKey(s, t), Optional.empty());

				firstYearValidYieldByProjectionType.put(new ModelReturnCodeKey(s, t), -9999);
			}
		}

		firstYearYieldsDisplayedByLayer = new HashMap<>();
	}

	public Double getPercentForestedLandUsed(ProjectionTypeCode projectionType) {
		return percentForestedLandUsedByProjectionType.get(projectionType);
	}

	public Double getYieldFactorUsed(ProjectionTypeCode projectionType) {
		return yieldFactorByProjectionType.get(projectionType);
	}

	public Integer getFirstYearValidYields(ProjectionStageCode stage, ProjectionTypeCode type) {
		return firstYearValidYieldByProjectionType.get(new ModelReturnCodeKey(stage, type));
	}

	public void setFirstYearYieldsDisplayed(Layer layer, int year) {
		if (firstYearYieldsDisplayedByLayer.containsKey(layer.getLayerId())) {
			throw new IllegalStateException(
					MessageFormat.format(
							"setFirstYearYieldsDisplayed: firstYearYieldsDisplayed has already been set for layer {0}",
							layer.getLayerId()
					)
			);
		}
		firstYearYieldsDisplayedByLayer.put(layer.getLayerId(), year);
	}

	public Integer getFirstYearYieldsDisplayed(Layer layer) {
		return firstYearYieldsDisplayedByLayer.get(layer.getLayerId());
	}

	public static class Builder {

		private final PolygonProjectionState polygon;

		public Builder() {
			polygon = new PolygonProjectionState();
		}

		public Builder growthModelUsedByProjectionType(
				Map<ProjectionTypeCode, GrowthModelCode> growthModelUsedByProjectionType
		) {
			polygon.growthModelByProjectionType = growthModelUsedByProjectionType;
			return this;
		}

		public Builder processingModeUsedByProjectionType(
				Map<ProjectionTypeCode, ProcessingModeCode> processingModeUsedByProjectionType
		) {
			polygon.processingModeByProjectionType = processingModeUsedByProjectionType;
			return this;
		}

		public Builder percentForestedLandUsed(Map<ProjectionTypeCode, Double> percentForestedLandUsed) {
			polygon.percentForestedLandUsedByProjectionType = percentForestedLandUsed;
			return this;
		}

		public Builder yieldFactorUsed(Map<ProjectionTypeCode, Double> yieldFactorUsed) {
			polygon.yieldFactorByProjectionType = yieldFactorUsed;
			return this;
		}
	}

	public void setGrowthModel(
			ProjectionTypeCode projectionType, GrowthModelCode growthModel, ProcessingModeCode processingMode
	) {
		if (this.growthModelByProjectionType.get(projectionType) != GrowthModelCode.UNKNOWN) {
			throw new IllegalStateException(
					this.getClass().getName()
							+ ".ProjectionState.setGrowthModel: growthModel has already been set for projectionType "
							+ projectionType
			);
		}

		this.growthModelByProjectionType.put(projectionType, growthModel);
		this.processingModeByProjectionType.put(projectionType, processingMode);
	}

	public void modifyGrowthModel(
			ProjectionTypeCode projectionType, GrowthModelCode growthModel, ProcessingModeCode processingMode
	) {
		if (this.growthModelByProjectionType.get(projectionType) == null) {
			throw new IllegalStateException(
					this.getClass().getName()
							+ ".ProjectionState.modifyGrowthModel: growthModel has not been set for projectionType "
							+ projectionType
			);
		}
		if (this.processingModeByProjectionType.get(projectionType) == null) {
			throw new IllegalStateException(
					this.getClass().getName()
							+ ".ProjectionState.modifyGrowthModel: processingMode has not been set for projectionType "
							+ projectionType
			);
		}

		this.growthModelByProjectionType.put(projectionType, growthModel);
		this.processingModeByProjectionType.put(projectionType, processingMode);
	}

	public GrowthModelCode getGrowthModel(ProjectionTypeCode projectionType) {
		if (growthModelByProjectionType.get(projectionType) == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState: growthModel has not been set for projectionType "
							+ projectionType
			);
		}
		return growthModelByProjectionType.get(projectionType);
	}

	public ProcessingModeCode getProcessingMode(ProjectionTypeCode projectionType) {
		if (processingModeByProjectionType.get(projectionType) == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState: processingMode has not been set for projectionType "
							+ projectionType
			);
		}
		return processingModeByProjectionType.get(projectionType);
	}

	public void resetProcessingResults(ProjectionStageCode stage, ProjectionTypeCode projectionType) {
		var key = new ModelReturnCodeKey(stage, projectionType);

		this.processingResultByStageAndProjectionType.put(key, Optional.empty());
	}

	public void setProcessingResults(
			ProjectionStageCode stage, ProjectionTypeCode projectionType, Optional<Throwable> result
	) {
		var key = new ModelReturnCodeKey(stage, projectionType);
		if (this.processingResultByStageAndProjectionType.get(key).isPresent()) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}.ProjectionState.setProcessingResults: processingResult has already been set for projection type {1} of stage {2}",
							this.getClass().getName(), stage, projectionType
					)
			);
		}

		this.processingResultByStageAndProjectionType.put(key, Optional.of(result));
	}

	public Optional<Throwable> getProcessingResults(ProjectionStageCode stage, ProjectionTypeCode projectionType) {

		var key = new ModelReturnCodeKey(stage, projectionType);
		if (processingResultByStageAndProjectionType.get(key).isPresent()) {
			return processingResultByStageAndProjectionType.get(key).get();
		} else {
			throw new IllegalStateException(
					"processingResultByStageAndProjectionType does not contain a value for key " + key.toString()
			);
		}
	}

	/**
	 * Set <code>startAge</code> and <code>endAge</code> as the start and end ages for -all- projection types. Later
	 * (via calls to {@link this.updateProjectionRange}) these values may be adjusted for specific projection types.
	 *
	 * @param startAge the start age to use for all projection types
	 * @param endAge   the end age to use for all projection types
	 */
	public void setProjectionRange(Double startAge, Double endAge) {

		for (var projectionType : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {

			if (this.startAgeByProjectionType.containsKey(projectionType)) {
				throw new IllegalStateException(
						MessageFormat.format(
								"{0}.ProjectionState.setProjectionRange: startAge for projection type {1} has already been set",
								this.getClass().getName(), projectionType
						)
				);
			}
			if (this.endAgeByProjectionType.containsKey(projectionType)) {
				throw new IllegalStateException(
						MessageFormat.format(
								"{0}.ProjectionState.setProjectionRange: endAge for projection type {1} has already been set",
								this.getClass().getName(), projectionType
						)
				);
			}

			this.startAgeByProjectionType.put(projectionType, startAge);
			this.endAgeByProjectionType.put(projectionType, endAge);
		}
	}

	public void updateProjectionRange(ProjectionTypeCode projectionType, Double startAge, Double endAge) {
		if (!this.startAgeByProjectionType.containsKey(projectionType)) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}.ProjectionState.updateProjectionRange: startAge for projection type {1} has not been set",
							this.getClass().getName(), projectionType
					)
			);
		}
		if (!this.endAgeByProjectionType.containsKey(projectionType)) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}.ProjectionState.updateProjectionRange: endAge for projection type {1} has not been set",
							this.getClass().getName(), projectionType
					)
			);
		}

		this.startAgeByProjectionType.put(projectionType, startAge);
		this.endAgeByProjectionType.put(projectionType, endAge);
	}

	public Double getStartAge(ProjectionTypeCode projectionType) {
		if (!this.startAgeByProjectionType.containsKey(projectionType)) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}.ProjectionState.getStartAge: startAge for projection type {1} has not been set",
							this.getClass().getName(), projectionType
					)
			);
		}
		return startAgeByProjectionType.get(projectionType);
	}

	public Double getEndAge(ProjectionTypeCode projectionType) {
		if (!this.endAgeByProjectionType.containsKey(projectionType)) {
			throw new IllegalStateException(
					MessageFormat.format(
							"{0}.ProjectionState.getEndAge: endAge for projection type {1} has not been set",
							this.getClass().getName(), projectionType
					)
			);
		}
		return endAgeByProjectionType.get(projectionType);
	}

	public void setExecutionFolder(Path executionFolder) {
		if (this.executionFolder != null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".setExecutionFolder: executionFolder has been set"
			);
		}
		this.executionFolder = executionFolder;
	}

	public Path getExecutionFolder() {
		if (this.executionFolder == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".getExecutionFolder: executionFolder has not been set"
			);
		}
		return executionFolder;
	}

	public boolean polygonWasProjected() {
		return didRunProjectionStage(Forward) || didRunProjectionStage(Back);
	}

	public boolean layerWasProjected(Layer layer) {

		var projectionType = layer.getAssignedProjectionType();
		return didRunProjectionStage(Forward, projectionType) || didRunProjectionStage(Back, projectionType);
	}

	public boolean didRunProjectionStage(ProjectionStageCode stage, ProjectionTypeCode projectionType) {
		var key = new ModelReturnCodeKey(stage, projectionType);
		return processingResultByStageAndProjectionType.containsKey(key)
				&& processingResultByStageAndProjectionType.get(key).isPresent();
	}

	public boolean didRunProjection(ProjectionTypeCode projectionType) {
		return didRunProjectionStage(Forward, projectionType) || didRunProjectionStage(Back, projectionType);
	}

	public boolean didRunProjectionStage(ProjectionStageCode stage) {
		for (var projectionType : ProjectionTypeCode.values()) {
			if (didRunProjectionStage(stage, projectionType)) {
				return true;
			}
		}
		return false;
	}
}
