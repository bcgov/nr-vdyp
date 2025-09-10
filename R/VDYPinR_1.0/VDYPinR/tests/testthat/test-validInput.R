test_that("validInput.R: input validation has been correctly implemented.", {
  library(data.table)
  library(testthat)

  layerfile <- data.table(FEATURE_ID = c(2005938L, 2005940L, 2005943L, 2005945L, 2005947L, 2005953L, 2005960L, 2005961L, 2005962L, 2005964L, 2005965L),
                          TREE_COVER_LAYER_ESTIMATED_ID = c(1442330L, 1442332L, 1442333L, 1442334L, 1442336L, 1442338L, 1442343L, 1442344L, 1442345L, 1442347L, 1442348L),
                          MAP_ID = c("083D018", "083D018", "083D018", "083D018", "083D018", "083D018", "083D018", "083D018", "083D018", "083D018", "083D018"),
                          POLYGON_NUMBER = c(320L, 321L, 313L, 327L, 318L, 57L, 319L, 201L, 309L, 308L, 310L),
                          LAYER_LEVEL_CODE = c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L),
                          VDYP7_LAYER_CD = c("P", "P", "P", "P", "P", "P", "P", "P", "P", "P", "P"),
                          LAYER_STOCKABILITY = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          FOREST_COVER_RANK_CODE = c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L),
                          NON_FOREST_DESCRIPTOR_CODE = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          EST_SITE_INDEX_SPECIES_CD = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          ESTIMATED_SITE_INDEX = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          CROWN_CLOSURE = c(20L, 30L, 30L, 20L, 40L, 35L, 30L, 35L, 40L, 30L, 15L),
                          BASAL_AREA_75 = c(26.6631, 29.97538, 9.36841, 5.48273, 31.366669, 27.060322, 31.917118,
                                            39.33426, 27.7986, 30.1885, 2.09958),
                          STEMS_PER_HA_75 = c(383L, 448L, 198L, 111L, 708L, 823L, 472L, 733L, 897L, 616L, 89L),
                          SPECIES_CD_1 = c("S", "S", "B", "B", "S", "S", "S", "S", "B", "B", "BL"),
                          SPECIES_PCT_1 = c(100, 100, 60, 60, 60, 50.1, 100, 70, 60, 60, 70),
                          SPECIES_CD_2 = c("", "", "S", "S", "B", "BL", "", "HW", "S", "S", "S"),
                          SPECIES_PCT_2 = c(NA, NA, 40, 40, 40, 49.9, NA, 20, 40, 40, 30),
                          SPECIES_CD_3 = c("", "", "", "", "", "", "", "BL", "", "", ""),
                          SPECIES_PCT_3 = c(NA, NA, NA, NA, NA, NA, NA, 10L, NA, NA, NA),
                          SPECIES_CD_4 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          SPECIES_PCT_4 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          SPECIES_CD_5 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          SPECIES_PCT_5 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          SPECIES_CD_6 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          SPECIES_PCT_6 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          EST_AGE_SPP1 = c(300L, 300L, 200L, 200L, 300L, 220L, 300L, 260L, 240L, 300L, 220L),
                          EST_HEIGHT_SPP1 = c(26.9, 26.9, 23.93, 23.93, 22.4, 19, 26.9, 25, 18, 23, 14),
                          EST_AGE_SPP2 = c(NA, NA, NA, NA, NA, 220L, NA, NA, NA, NA, NA),
                          EST_HEIGHT_SPP2 = c(NA, NA, NA, NA, NA, 19.75, NA, NA, NA, NA, NA),
                          ADJ_IND = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          LOREY_HEIGHT_75 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          BASAL_AREA_125 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          WS_VOL_PER_HA_75 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          WS_VOL_PER_HA_125 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          CU_VOL_PER_HA_125 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          D_VOL_PER_HA_125 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA),
                          DW_VOL_PER_HA_125 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA))

  polyfile <- data.table(FEATURE_ID = c(2005938L, 2005940L, 2005943L, 2005945L, 2005947L, 2005953L,
                                         2005960L, 2005961L, 2005962L, 2005964L, 2005965L),
                          MAP_ID = c("083D018", "083D018", "083D018", "083D018", "083D018",
                                     "083D018", "083D018", "083D018", "083D018", "083D018", "083D018"),
                          POLYGON_NUMBER = c(320L, 321L, 313L, 327L, 318L, 57L, 319L,
                                             201L, 309L, 308L, 310L),
                          ORG_UNIT = c("DPG", "DPG", "DPG", "DPG",
                                       "DPG", "DPG", "DPG", "DPG", "DPG", "DPG", "DPG"),
                          TSA_NAME = "UNK",
                          TFL_NAME = "UNK",
                          INVENTORY_STANDARD_CODE = "V",
                          TSA_NUMBER = "UNK",
                          SHRUB_HEIGHT = NA,
                          SHRUB_CROWN_CLOSURE = NA,
                          SHRUB_COVER_PATTERN = NA,
                          HERB_COVER_TYPE_CODE = NA,
                          HERB_COVER_PCT = NA,
                          HERB_COVER_PATTERN_CODE = NA,
                          BRYOID_COVER_PCT = NA,
                          BEC_ZONE_CODE = c("ICH", "ICH", "ESSF", "ESSF", "ESSF", "ESSF",
                                            "ESSF", "ICH", "ESSF", "ESSF", "ESSF"),
                         CFS_ECOZONE = 14,
                          PRE_DISTURBANCE_STOCKABILITY = c(29L, 43L, 13L,
                                                           6L, 57L, 50L, 43L, 50L, 57L, 43L, 3L),
                          YIELD_FACTOR = c(1, 1, 0.3, 0.2, 1, 1, 1, 1, 1, 1, 0.15),
                          NON_PRODUCTIVE_DESCRIPTOR_CD = c("", "", "AF", "AF", "", "", "", "", "", "", "AF"),
                          BCLCS_LEVEL1_CODE = c("V", "V", "V", "V", "V", "V", "V", "V", "V", "V", "V"),
                          BCLCS_LEVEL2_CODE = c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
                          BCLCS_LEVEL3_CODE = c("U", "U", "U", "U", "U", "U", "U",
                                                "U", "U", "U", "U"),
                          BCLCS_LEVEL4_CODE = c("TC", "TC", "TC",
                                                "TC", "TC", "TC", "TC", "TC", "TC", "TC", "TC"),
                          BCLCS_LEVEL5_CODE = c("SP", "OP", "OP", "SP", "OP", "OP", "OP", "OP", "OP", "OP", "SP"),
                          PHOTO_ESTIMATION_BASE_YEAR = c(1974L, 1974L, 1974L, 1974L,
                                                         1974L, 1991L, 1974L, 1991L, 1974L, 1974L, 1991L),
                          REFERENCE_YEAR = c(1974L,
                                             1974L, 1974L, 1974L, 1974L, 1991L, 1974L, 1991L, 1974L, 1974L,
                                             1991L),
                          PCT_DEAD = NA,
                          NON_VEG_COVER_TYPE_1 = NA,
                          NON_VEG_COVER_PCT_1 = NA,
                          NON_VEG_COVER_PATTERN_1 = NA,
                          NON_VEG_COVER_TYPE_2 = NA,
                          NON_VEG_COVER_PCT_2 = NA,
                          NON_VEG_COVER_PATTERN_2 = NA,
                          NON_VEG_COVER_TYPE_3 = NA,
                          NON_VEG_COVER_PCT_3 = NA,
                          NON_VEG_COVER_PATTERN_3 = NA,
                          LAND_COVER_CLASS_CD_1 = NA,
                          LAND_COVER_PCT_1 = NA,
                          LAND_COVER_CLASS_CD_2 = NA,
                          LAND_COVER_PCT_2 = NA,
                          LAND_COVER_CLASS_CD_3 = NA,
                          LAND_COVER_PCT_3 = NA)
 # test perfect pass
  validInput(polyFile = polyfile,
             layerFile = layerfile)

  # test duplicate
  polyfile_dupl <- rbind(polyfile, polyfile[1:5,])
  layerfile_dupl <- rbind(layerfile, layerfile[6:10,])
  validInput(polyFile = polyfile_dupl,
             layerFile = layerfile_dupl)

  # test column name mismatch
  polyfile_namemismatch1 <- data.table::copy(polyfile)
  setnames(polyfile_namemismatch1,
           c("ORG_UNIT", "BEC_ZONE_CODE"),
           c("ORG_UNIT1", "BEC_ZONE_CODE2"))
  polyfile_namemismatch1[, onemorecol := "A"]

  layerfile_namemismatch1 <- data.table::copy(layerfile)
  setnames(layerfile_namemismatch1,
           c("WS_VOL_PER_HA_125", "BASAL_AREA_75"),
           c("WS_VOL_PER_HA_126", "BASAL_AREA_76"))
  validInput(polyFile = polyfile_namemismatch1,
             layerFile = layerfile_namemismatch1)

  # test feature id mismatch
  allfeatureids <- unique(polyfile$FEATURE_ID)
  # take 2-3 from polyfile, and 4-5 from layerfile
  polyfile_namemismatch2 <- polyfile[!(FEATURE_ID %in% allfeatureids[2:3]),]
  layerfile_namemismatch2 <- layerfile[!(FEATURE_ID %in% allfeatureids[4:5]),]
  validInput(polyFile = polyfile_namemismatch2,
             layerFile = layerfile_namemismatch2)






})
