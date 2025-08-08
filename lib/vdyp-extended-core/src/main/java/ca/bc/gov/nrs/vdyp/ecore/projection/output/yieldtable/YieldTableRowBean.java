package ca.bc.gov.nrs.vdyp.ecore.projection.output.yieldtable;

interface YieldTableRowBean {

	enum MultiFieldPrefixes {
		Species("SPECIES_"), SpeciesProjection("PRJ_SP");

		public final String fieldName;

		MultiFieldPrefixes(String fieldName) {
			this.fieldName = fieldName;
		}
	};

	enum MultiFieldSuffixes {
		Code("_CODE"), //
		Percent("_PCNT"), //
		WholeStemVolume("_VOL_WS"), //
		CloseUtilizationVolume("_VOL_CU"), //
		CuVolumeLessDecay("_VOL_D"), //
		CuVolumeLessDecayWastage("_VOL_DW"), //
		CuVolumeLessDecayWastageBreakage("_VOL_DWB"), //
		MoFBiomassWholeStemVolume("_MoF_BIO_WS"), //
		MoFBiomassCloseUtilizationVolume("_MoF_BIO_CU"), //
		MoFBiomassCuVolumeLessDecay("_MoF_BIO_D"), //
		MoFBiomassCuVolumeLessDecayWastage("_MoF_BIO_DW"), //
		MoFBiomassCuVolumeLessDecayWastageBreakage("_MoF_BIO_DWB");

		public final String fieldName;

		MultiFieldSuffixes(String fieldName) {
			this.fieldName = fieldName;
		}
	}

	/**
	 * The valid (prefix, suffix) pairs are those where if the prefix ends with "_", the suffix is either _CODE or _PCNT
	 * OR both statements are false.
	 *
	 * @param p prefix
	 * @param s suffix
	 * @return as described
	 */
	default boolean isValidPrefixSuffixPair(MultiFieldPrefixes p, MultiFieldSuffixes s) {
		return p == MultiFieldPrefixes.Species && (s == MultiFieldSuffixes.Code || s == MultiFieldSuffixes.Percent)
				|| p != MultiFieldPrefixes.Species
						&& ! (s == MultiFieldSuffixes.Code || s == MultiFieldSuffixes.Percent);
	}

	default void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, Double value
	) {
		if (value != null) {
			setSpeciesFieldValue(prefix, speciesNumber, suffix, String.format("%.5f", value));
		}
	}

	default void setSpeciesFieldValue(
			MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, Integer value
	) {
		if (value != null) {
			setSpeciesFieldValue(prefix, speciesNumber, suffix, String.valueOf(value));
		}
	}

	void setSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, String value);

	String getSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix);

	void setMode(String mode);

	String getMode();

	void setCfsBiomassFoliage(Double cfsBiomassFoliage);

	String getCfsBiomassFoliage();

	void setCfsBiomassBranch(Double cfsBiomassBranch);

	String getCfsBiomassBranch();

	void setCfsBiomassBark(Double cfsBiomassBark);

	String getCfsBiomassBark();

	void setCfsBiomassStem(Double cfsBiomassStem);

	String getCfsBiomassStem();

	void setMoFBiomassCuVolumeLessDecayWastageBreakage(Double mofBiomassCuVolumeLessDecayWastageBreakage);

	String getMoFBiomassCuVolumeLessDecayWastageBreakage();

	void setMoFBiomassCuVolumeLessDecayWastage(Double mofBiomassCuVolumeLessDecayWastage);

	String getMoFBiomassCuVolumeLessDecayWastage();

	void setMoFBiomassCuVolumeLessDecay(Double mofBiomassCuVolumeLessDecay);

	String getMoFBiomassCuVolumeLessDecay();

	void setMoFBiomassCloseUtilizationVolume(Double mofBiomassCloseUtilizationVolume);

	String getMoFBiomassCloseUtilizationVolume();

	void setMoFBiomassWholeStemVolume(Double mofBiomassWholeStemVolume);

	String getMoFBiomassWholeStemVolume();

	void setCuVolumeLessDecayWastageBreakage(Double cuVolumeLessDecayWastageBreakage);

	String getCuVolumeLessDecayWastageBreakage();

	void setCuVolumeLessDecayWastage(Double cuVolumeLessDecayWastage);

	String getCuVolumeLessDecayWastage();

	void setCuVolumeLessDecay(Double cuVolumeLessDecay);

	String getCuVolumeLessDecay();

	void setCloseUtilizationVolume(Double closeUtilizationVolume);

	String getCloseUtilizationVolume();

	void setWholeStemVolume(Double wholeStemVolume);

	String getWholeStemVolume();

	void setBasalArea(Double basalArea);

	String getBasalArea();

	void setTreesPerHectare(Double treesPerHectare);

	String getTreesPerHectare();

	void setDiameter(Double diameter);

	String getDiameter();

	void setLoreyHeight(Double loreyHeight);

	String getLoreyHeight();

	void setSecondaryHeight(Double secondaryHeight);

	String getSecondaryHeight();

	void setDominantHeight(Double dominantHeight);

	String getDominantHeight();

	void setSiteIndex(Double siteIndex);

	String getSiteIndex();

	void setPercentStockable(Double percentStockable);

	String getPercentStockable();

	void setTotalAge(Integer totalAge);

	String getTotalAge();

	void setProjectionYear(Integer projectionYear);

	String getProjectionYear();

	void setLayerId(String layerId);

	String getLayerId();

	void setPolygonId(Long polygonId);

	String getPolygonId();

	void setMapId(String mapId);

	String getMapId();

	void setDistrict(String district);

	String getDistrict();

	void setFeatureId(Long featureId);

	String getFeatureId();

	void setTableNumber(int tableNumber);

	String getTableNumber();

}
