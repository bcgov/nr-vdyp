package ca.bc.gov.nrs.vdyp.backend.projection.output.yieldtable;

interface YieldTableRowValues {

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

	void setSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix, String value);

	String getSpeciesFieldValue(MultiFieldPrefixes prefix, int speciesNumber, MultiFieldSuffixes suffix);

	void setMode(String mode);

	String getMode();

	void setCfsBiomassFoliage(String cfsBiomassFoliage);

	String getCfsBiomassFoliage();

	void setCfsBiomassBranch(String cfsBiomassBranch);

	String getCfsBiomassBranch();

	void setCfsBiomassBark(String cfsBiomassBark);

	String getCfsBiomassBark();

	void setCfsBiomassStem(String cfsBiomassStem);

	String getCfsBiomassStem();

	void setMoFBiomassCuVolumeLessDecayWastageBreakage(String mofBiomassCuVolumeLessDecayWastageBreakage);

	String getMoFBiomassCuVolumeLessDecayWastageBreakage();

	void setMoFBiomassCuVolumeLessDecayWastage(String mofBiomassCuVolumeLessDecayWastage);

	String getMoFBiomassCuVolumeLessDecayWastage();

	void setMoFBiomassCuVolumeLessDecay(String mofBiomassCuVolumeLessDecay);

	String getMoFBiomassCuVolumeLessDecay();

	void setMoFBiomassCloseUtilizationVolume(String mofBiomassCloseUtilizationVolume);

	String getMoFBiomassCloseUtilizationVolume();

	void setMoFBiomassWholeStemVolume(String mofBiomassWholeStemVolume);

	String getMoFBiomassWholeStemVolume();

	void setCuVolumeLessDecayWastageBreakage(String cuVolumeLessDecayWastageBreakage);

	String getCuVolumeLessDecayWastageBreakage();

	void setCuVolumeLessDecayWastage(String cuVolumeLessDecayWastage);

	String getCuVolumeLessDecayWastage();

	void setCuVolumeLessDecay(String cuVolumeLessDecay);

	String getCuVolumeLessDecay();

	void setCloseUtilizationVolume(String closeUtilizationVolume);

	String getCloseUtilizationVolume();

	void setWholeStemVolume(String wholeStemVolume);

	String getWholeStemVolume();

	void setBasalArea(String basalArea);

	String getBasalArea();

	void setTreesPerHectare(String treesPerHectare);

	String getTreesPerHectare();

	void setDiameter(String diameter);

	String getDiameter();

	void setLoreyHeight(String loreyHeight);

	String getLoreyHeight();

	void setSecondaryHeight(String secondaryHeight);

	String getSecondaryHeight();

	void setDominantHeight(String dominantHeight);

	String getDominantHeight();

	void setSiteIndex(String siteIndex);

	String getSiteIndex();

	void setPercentStockable(String percentStockable);

	String getPercentStockable();

	void setTotalAge(String totalAge);

	String getTotalAge();

	void setProjectionYear(String projectionYear);

	String getProjectionYear();

	void setLayerId(String layerId);

	String getLayerId();

	void setPolygonId(String polygonId);

	String getPolygonId();

	void setMapId(String mapId);

	String getMapId();

	void setDistrict(String district);

	String getDistrict();

	void setFeatureId(String featureId);

	String getFeatureId();

	void setTableNumber(String tableNumber);

	String getTableNumber();
}
