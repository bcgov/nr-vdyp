package ca.bc.gov.nrs.vdyp.backend.projection;

import static ca.bc.gov.nrs.vdyp.backend.projection.ProjectionStageCode.Initial;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.backend.projection.model.PolygonMessage;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.GrowthModelCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProcessingModeCode;
import ca.bc.gov.nrs.vdyp.backend.projection.model.enumerations.ProjectionTypeCode;

public class PolygonProjectionState {

	public record ModelReturnCodeKey(ProjectionStageCode stage, ProjectionTypeCode type) {
	}

	/** The messages generated during the projection of the polygon. */
	private List<PolygonMessage> projectionMessages;

	private Double startAge = null;
	private Double endAge = null;

	// Per-projection type information

	private Map<ModelReturnCodeKey, Integer> initialModelReturnCodeByProjectionType;
	private Map<ModelReturnCodeKey, ProcessingResultsCode> initialProcessingResultByProjectionType;
	private Map<ModelReturnCodeKey, Integer> adjustmentProcessingResultByProjectionType;
	private Map<ModelReturnCodeKey, Integer> projectionProcessingResultByProjectionType;
	private Map<ModelReturnCodeKey, Integer> firstYearValidYieldByProjectionType;

	private GrowthModelCode growthModelToBeUsed;
	private Map<ProjectionTypeCode, GrowthModelCode> growthModelByProjectionType;
	private Map<ProjectionTypeCode, ProcessingModeCode> processingModeByProjectionType;
	private Map<ProjectionTypeCode, Double> percentForestedLandUsedByProjectionType;
	private Map<ProjectionTypeCode, Double> yieldFactorByProjectionType;

	private Path executionFolder = null;

	public PolygonProjectionState() {

		growthModelToBeUsed = GrowthModelCode.getDefault();

		growthModelByProjectionType = new HashMap<>();
		processingModeByProjectionType = new HashMap<>();
		percentForestedLandUsedByProjectionType = new HashMap<>();
		yieldFactorByProjectionType = new HashMap<>();
		initialModelReturnCodeByProjectionType = new HashMap<>();
		initialProcessingResultByProjectionType = new HashMap<>();
		adjustmentProcessingResultByProjectionType = new HashMap<>();
		projectionProcessingResultByProjectionType = new HashMap<>();
		firstYearValidYieldByProjectionType = new HashMap<>();

		for (ProjectionStageCode s : ProjectionStageCode.values()) {
			for (ProjectionTypeCode t : ProjectionTypeCode.ACTUAL_PROJECTION_TYPES_LIST) {
				growthModelByProjectionType.put(t, GrowthModelCode.UNKNOWN);
				processingModeByProjectionType.put(t, ProcessingModeCode.getDefault());
				percentForestedLandUsedByProjectionType.put(t, null);
				yieldFactorByProjectionType.put(t, null);
				initialModelReturnCodeByProjectionType.put(new ModelReturnCodeKey(s, t), 0);
				initialProcessingResultByProjectionType
						.put(new ModelReturnCodeKey(s, t), ProcessingResultsCode.NULL_PROCESSING_RESULT_CODE);
				adjustmentProcessingResultByProjectionType.put(new ModelReturnCodeKey(s, t), -9999);
				projectionProcessingResultByProjectionType.put(new ModelReturnCodeKey(s, t), -9999);
				firstYearValidYieldByProjectionType.put(new ModelReturnCodeKey(s, t), -9999);
			}
		}

		projectionMessages = new ArrayList<>();
	}

	public List<PolygonMessage> getProjectionMessages() {
		return projectionMessages;
	}

	public GrowthModelCode getGrowthModelToBeUsed() {
		return growthModelToBeUsed;
	}

	public Map<ProjectionTypeCode, GrowthModelCode> getGrowthModelUsedByProjectionType() {
		return Collections.unmodifiableMap(growthModelByProjectionType);
	}

	public Map<ProjectionTypeCode, ProcessingModeCode> getProcessingModeUsedByProjectionType() {
		return Collections.unmodifiableMap(processingModeByProjectionType);
	}

	public Map<ProjectionTypeCode, Double> getPercentForestedLandUsed() {
		return Collections.unmodifiableMap(percentForestedLandUsedByProjectionType);
	}

	public Map<ProjectionTypeCode, Double> getYieldFactorUsed() {
		return Collections.unmodifiableMap(yieldFactorByProjectionType);
	}

	public Map<ModelReturnCodeKey, Integer> getInitialModelReturnCode() {
		return Collections.unmodifiableMap(initialModelReturnCodeByProjectionType);
	}

	public Map<ModelReturnCodeKey, ProcessingResultsCode> getInitialProcessingResults() {
		return Collections.unmodifiableMap(initialProcessingResultByProjectionType);
	}

	public Map<ModelReturnCodeKey, Integer> getAdjustmentProcessingResults() {
		return Collections.unmodifiableMap(adjustmentProcessingResultByProjectionType);
	}

	public Map<ModelReturnCodeKey, Integer> getProjectionProcessingResults() {
		return Collections.unmodifiableMap(projectionProcessingResultByProjectionType);
	}

	public Map<ModelReturnCodeKey, Integer> getFirstYearValidYields() {
		return Collections.unmodifiableMap(firstYearValidYieldByProjectionType);
	}

	public static class Builder {

		private final PolygonProjectionState polygon;

		public Builder() {
			polygon = new PolygonProjectionState();
		}

		public Builder growthModelToBeUsed(GrowthModelCode growthModelToBeUsed) {
			polygon.growthModelToBeUsed = growthModelToBeUsed;
			return this;
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

		public Builder initialModelReturnCode(Map<ModelReturnCodeKey, Integer> initialModelReturnCode) {
			polygon.initialModelReturnCodeByProjectionType = initialModelReturnCode;
			return this;
		}

		public Builder
				initialProcessingResults(Map<ModelReturnCodeKey, ProcessingResultsCode> initialProcessingResults) {
			polygon.initialProcessingResultByProjectionType = initialProcessingResults;
			return this;
		}

		public Builder adjustmentProcessingResults(Map<ModelReturnCodeKey, Integer> adjustmentProcessingResults) {
			polygon.adjustmentProcessingResultByProjectionType = adjustmentProcessingResults;
			return this;
		}

		public Builder projectionProcessingResults(Map<ModelReturnCodeKey, Integer> projectionProcessingResults) {
			polygon.projectionProcessingResultByProjectionType = projectionProcessingResults;
			return this;
		}

		public Builder firstYearValidYields(Map<ModelReturnCodeKey, Integer> firstYearValidYields) {
			polygon.firstYearValidYieldByProjectionType = firstYearValidYields;
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

	public void setInitialProcessingResults(
			ProjectionTypeCode projectionType, Integer modelReturnCode, ProcessingResultsCode processingResultsCode
	) {
		var key = new ModelReturnCodeKey(Initial, projectionType);
		if (this.initialProcessingResultByProjectionType
				.get(key) != ProcessingResultsCode.NULL_PROCESSING_RESULT_CODE) {
			throw new IllegalStateException(
					this.getClass().getName()
							+ ".ProjectionState.setInitialProcessingResults: initialProcessingResults has already been set for projectionType "
							+ projectionType
			);
		}

		this.initialModelReturnCodeByProjectionType.put(key, modelReturnCode);
		this.initialProcessingResultByProjectionType.put(key, processingResultsCode);
	}

	public Integer getInitialModelReturnCode(ProjectionTypeCode projectionType) {
		var key = new ModelReturnCodeKey(Initial, projectionType);
		if (initialModelReturnCodeByProjectionType.get(key) == null) {
			throw new IllegalStateException(
					this.getClass().getName()
							+ ".ProjectionState.getInitialModelReturnCode: initialModelReturnCode has not been set for projectionType "
							+ projectionType
			);
		}
		return initialModelReturnCodeByProjectionType.get(key);
	}

	public ProcessingResultsCode
			getProcessingResultsCode(ProjectionStageCode state, ProjectionTypeCode projectionType) {
		var key = new ModelReturnCodeKey(state, projectionType);
		if (initialProcessingResultByProjectionType.get(key) == null) {
			throw new IllegalStateException(
					this.getClass().getName()
							+ ".ProjectionState.getProcessingResultsCode: initialProcessingResult has not been set for projectionType "
							+ projectionType
			);
		}
		return initialProcessingResultByProjectionType.get(key);
	}

	public void setProjectionRange(Double startAge, Double endAge) {
		if (this.startAge != null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState.setProjectionRange: startAge has already been set"
			);
		}
		if (this.endAge != null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState.setProjectionRange: endAge has already been set"
			);
		}

		this.startAge = startAge;
		this.endAge = endAge;
	}

	public void updateProjectionRange(Double startAge, Double endAge) {
		if (this.startAge == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState.updateProjectionRange: startAge has not been set"
			);
		}
		if (this.endAge == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState.updateProjectionRange: endAge has not been set"
			);
		}

		this.startAge = startAge;
		this.endAge = endAge;
	}

	public Double getStartAge() {
		if (startAge == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState.getStartAge: startAge has not been set"
			);
		}
		return startAge;
	}

	public Double getEndAge() {
		if (endAge == null) {
			throw new IllegalStateException(
					this.getClass().getName() + ".ProjectionState.getEndAge: endAge has not been set"
			);
		}
		return endAge;
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
}
