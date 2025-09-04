test_that("VDYP8Run.R: check the volume projection.", {
  library(data.table)
  library(testthat)
  # create 5 polygons
  polyfile <- data.table(FEATURE_ID = c(1816082L, 1816114L, 1816123L, 15807655L, 16160455L),
                         MAP_ID = c("082L025", "082L025", "082L025", "092O090", "083D049"),
                         POLYGON_NUMBER = c(360L, 334L, 317L, 95439819L, 15151111L),
                         ORG_UNIT = c("DOS", "DOS", "DOS", "DCC", "DPG"),
                         TSA_NAME = "UNK",
                         TFL_NAME = "UNK",
                         INVENTORY_STANDARD_CODE = c("F", "F", "F", "I", "F"),
                         TSA_NUMBER = "UNK",
                         SHRUB_HEIGHT = NA,
                         SHRUB_CROWN_CLOSURE = NA,
                         SHRUB_COVER_PATTERN = NA,
                         HERB_COVER_TYPE_CODE = NA,
                         HERB_COVER_PCT = NA,
                         HERB_COVER_PATTERN_CODE = NA,
                         BRYOID_COVER_PCT = NA,
                         BEC_ZONE_CODE = c("IDF", "IDF", "IDF", "IDF", "ESSF"),
                         CFS_ECOZONE = 14,
                         PRE_DISTURBANCE_STOCKABILITY = c(51, 80, 82, 76, 10),
                         YIELD_FACTOR = c(1, 1, 1, 1, 0.09),
                         NON_PRODUCTIVE_DESCRIPTOR_CD = c("", "", "", "", "NP"),
                         BCLCS_LEVEL1_CODE = "V",
                         BCLCS_LEVEL2_CODE = TRUE,
                         BCLCS_LEVEL3_CODE = "U",
                         BCLCS_LEVEL4_CODE = "TC",
                         BCLCS_LEVEL5_CODE = c("OP", "SP", "OP", "SP", "SP"),
                         PHOTO_ESTIMATION_BASE_YEAR = c(1964L, 1975L, 1964L, 1996L, 2018L),
                         REFERENCE_YEAR = c(1964L, 1975L, 1964L, 1996L, 2018L),
                         PCT_DEAD = c(NA, NA, NA, NA, 11L),
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
  
  # for each polygon, there are multiple layers
  layerfile <- data.table(FEATURE_ID = c(1816082L, 1816114L, 1816114L, 1816123L,
                                         1816123L, 15807655L, 15807655L, 15807655L,
                                         15807655L, 16160455L, 16160455L),
                          TREE_COVER_LAYER_ESTIMATED_ID = c(1269276L, 1269309L, 1269310L,
                                                            1269318L, 1269319L, 16569995L, 16569996L, 16569997L,
                                                            16569998L, 16988761L, 16988762L),
                          MAP_ID = c("082L025", "082L025",
                                     "082L025", "082L025", "082L025", "092O090", "092O090", "092O090",
                                     "092O090", "083D049", "083D049"),
                          POLYGON_NUMBER = c(360L, 334L,
                                             334L, 317L, 317L, 95439819L, 95439819L, 95439819L, 95439819L,
                                             15151111L, 15151111L),
                          LAYER_LEVEL_CODE = c("1", "1", "2", "1",
                                               "2", "1", "2", "3", "4", "1", "D"),
                          VDYP7_LAYER_CD = c("P", "V",
                                             "P", "V", "P", "R", "R", "P", "", "P", "D"),
                          LAYER_STOCKABILITY = NA,
                          FOREST_COVER_RANK_CODE = c(1L, NA, 1L, NA, 1L, NA, NA, 1L, NA, 1L, NA),
                          NON_FOREST_DESCRIPTOR_CODE = "",
                          EST_SITE_INDEX_SPECIES_CD = c("", "", "FD", "", "", "PLI", "PLI", "PLI", "FDI", "", ""),
                          ESTIMATED_SITE_INDEX = c(NA, NA, 5, NA, NA, 18, 18, 18, 18, NA, NA),
                          CROWN_CLOSURE = c(30L, 3L, 20L, 4L, 40L, 3L, 4L, 10L, 2L, 9L, NA),
                          BASAL_AREA_75 = c(16.84712, 0.8656, NA, 7.02827, 2.78266, 0.43631, NA, NA, NA, 1.759, 0.2125),
                          STEMS_PER_HA_75 = c(243L, 12L, NA, 101L, 380L, 43L, 564L,
                                              3636L, 2964L, 64L, 8L),
                          SPECIES_CD_1 = c("PY", "FD", "FD", "FD",
                                           "FD", "PLI", "PLI", "PLI", "FDI", "BL", "PL"),
                          SPECIES_PCT_1 = c(60, 50, 90, 60, 60, 60, 80, 70, 60, 68, 100),
                          SPECIES_CD_2 = c("FD", "LW", "PY", "LW", "LW", "FDI", "FDI", "FDI", "PLI", "S", ""),
                          SPECIES_PCT_2 = c(40, 30, 10, 40, 40, 40, 20, 30, 30, 22, NA),
                          SPECIES_CD_3 = c("", "PY", "", "", "", "", "", "", "AT", "PL", ""),
                          SPECIES_PCT_3 = c(NA, 20, NA, NA, NA, NA, NA, NA, 10, 10, NA),
                          SPECIES_CD_4 = "",
                          SPECIES_PCT_4 = NA,
                          SPECIES_CD_5 = "",
                          SPECIES_PCT_5 = NA,
                          SPECIES_CD_6 = "",
                          SPECIES_PCT_6 = NA,
                          EST_AGE_SPP1 = c(130L, 200L, 10L, 200L, 30L, 50L, 27L,
                                           16L, 7L, 137L, 127L),
                          EST_HEIGHT_SPP1 = c(24, 27, 1, 27,
                                              9.02, 11, 9.45, 4.77, 0.71, 17.1, 13.91),
                          EST_AGE_SPP2 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, 137L, NA),
                          EST_HEIGHT_SPP2 = c(NA, NA, NA, NA, NA, NA, NA, NA, NA, 16.5, NA),
                          ADJ_IND = NA,
                          LOREY_HEIGHT_75 = NA,
                          BASAL_AREA_125 = NA,
                          WS_VOL_PER_HA_75 = NA,
                          WS_VOL_PER_HA_125 = NA,
                          CU_VOL_PER_HA_125 = NA,
                          D_VOL_PER_HA_125 = NA,
                          DW_VOL_PER_HA_125 = NA)
  
  
  ## test only volume simulation with age with by species
  testrun1_vol_byspecies <- VDYP8Run(polyFile = polyfile,
                                     layerFile = layerfile,
                                     BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                     utilTable = utilTableGenerator(all = 17.5,
                                                                    askForConfirm = FALSE),
                                     timeSeries = "age",
                                     startTime = 0,
                                     endTime = 250,
                                     timeIncrement = 5,
                                     projVolume = TRUE,
                                     forceRefYear = TRUE)
  
  expect_equal(names(testrun1_vol_byspecies),
               c("summary", "yieldTable", "warningTable",
                 "errorTable", "errorReport",
                 "logReport", "vdyp7Log", "metadata"))
  expect_equal(dim(testrun1_vol_byspecies$yieldTable), c(411, 64))
  expect_equal(names(testrun1_vol_byspecies$yieldTable),
               c("TABLE_NUM", "FEATURE_ID", "DISTRICT", "MAP_ID", "POLYGON_ID",
                 "LAYER_ID", "PROJECTION_YEAR", "PRJ_TOTAL_AGE", "SPECIES_1_CODE",
                 "SPECIES_1_PCNT", "SPECIES_2_CODE", "SPECIES_2_PCNT", "SPECIES_3_CODE",
                 "SPECIES_3_PCNT", "SPECIES_4_CODE", "SPECIES_4_PCNT", "SPECIES_5_CODE",
                 "SPECIES_5_PCNT", "SPECIES_6_CODE", "SPECIES_6_PCNT", "PRJ_PCNT_STOCK",
                 "PRJ_SITE_INDEX", "PRJ_DOM_HT", "PRJ_SCND_HT", "PRJ_LOREY_HT",
                 "PRJ_DIAMETER", "PRJ_TPH", "PRJ_BA", "PRJ_VOL_WS", "PRJ_VOL_CU",
                 "PRJ_VOL_D", "PRJ_VOL_DW", "PRJ_VOL_DWB", "PRJ_SP1_VOL_WS", "PRJ_SP1_VOL_CU",
                 "PRJ_SP1_VOL_D", "PRJ_SP1_VOL_DW", "PRJ_SP1_VOL_DWB", "PRJ_SP2_VOL_WS",
                 "PRJ_SP2_VOL_CU", "PRJ_SP2_VOL_D", "PRJ_SP2_VOL_DW", "PRJ_SP2_VOL_DWB",
                 "PRJ_SP3_VOL_WS", "PRJ_SP3_VOL_CU", "PRJ_SP3_VOL_D", "PRJ_SP3_VOL_DW",
                 "PRJ_SP3_VOL_DWB", "PRJ_SP4_VOL_WS", "PRJ_SP4_VOL_CU", "PRJ_SP4_VOL_D",
                 "PRJ_SP4_VOL_DW", "PRJ_SP4_VOL_DWB", "PRJ_SP5_VOL_WS", "PRJ_SP5_VOL_CU",
                 "PRJ_SP5_VOL_D", "PRJ_SP5_VOL_DW", "PRJ_SP5_VOL_DWB", "PRJ_SP6_VOL_WS",
                 "PRJ_SP6_VOL_CU", "PRJ_SP6_VOL_D", "PRJ_SP6_VOL_DW", "PRJ_SP6_VOL_DWB",
                 "PRJ_MODE"))
  
  yieldtable_by_layer_test1 <- testrun1_vol_byspecies$yieldTable[,.(FEATURE_ID, LAYER_ID,
                                                                    PROJECTION_YEAR, PRJ_TOTAL_AGE,
                                                                    PRJ_PCNT_STOCK,
                                                                    PRJ_SITE_INDEX,
                                                                    PRJ_DOM_HT,
                                                                    PRJ_SCND_HT,
                                                                    PRJ_LOREY_HT,
                                                                    PRJ_DIAMETER, PRJ_TPH, PRJ_BA,
                                                                    PRJ_VOL_WS, PRJ_VOL_CU,
                                                                    PRJ_VOL_D, PRJ_VOL_DW, PRJ_VOL_DWB,
                                                                    PRJ_MODE)]
  
  ## test only volume simulation with age without by species
  testrun2_vol_byspecies <- VDYP8Run(polyFile = polyfile,
                                     layerFile = layerfile,
                                     BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                     utilTable = utilTableGenerator(all = 17.5,
                                                                    askForConfirm = FALSE),
                                     timeSeries = "age",
                                     startTime = 0,
                                     endTime = 250,
                                     timeIncrement = 5,
                                     projVolume = TRUE,
                                     projBySpecies = FALSE)
  expect_equal(nrow(testrun2_vol_byspecies$yieldTable), 411)
  expect_equal(names(testrun2_vol_byspecies$yieldTable),
               c("TABLE_NUM", "FEATURE_ID", "DISTRICT", "MAP_ID", "POLYGON_ID",
                 "LAYER_ID", "PROJECTION_YEAR", "PRJ_TOTAL_AGE", "SPECIES_1_CODE",
                 "SPECIES_1_PCNT", "SPECIES_2_CODE", "SPECIES_2_PCNT", "SPECIES_3_CODE",
                 "SPECIES_3_PCNT", "SPECIES_4_CODE", "SPECIES_4_PCNT", "SPECIES_5_CODE",
                 "SPECIES_5_PCNT", "SPECIES_6_CODE", "SPECIES_6_PCNT", "PRJ_PCNT_STOCK",
                 "PRJ_SITE_INDEX", "PRJ_DOM_HT", "PRJ_SCND_HT", "PRJ_LOREY_HT",
                 "PRJ_DIAMETER", "PRJ_TPH", "PRJ_BA", "PRJ_VOL_WS", "PRJ_VOL_CU",
                 "PRJ_VOL_D", "PRJ_VOL_DW", "PRJ_VOL_DWB", "PRJ_MODE"))
  
  expect_equivalent(yieldtable_by_layer_test1,
                    testrun2_vol_byspecies$yieldTable[,.(FEATURE_ID, LAYER_ID,
                                                         PROJECTION_YEAR, PRJ_TOTAL_AGE,
                                                         PRJ_PCNT_STOCK,
                                                         PRJ_SITE_INDEX,
                                                         PRJ_DOM_HT,
                                                         PRJ_SCND_HT,
                                                         PRJ_LOREY_HT,
                                                         PRJ_DIAMETER, PRJ_TPH, PRJ_BA,
                                                         PRJ_VOL_WS, PRJ_VOL_CU,
                                                         PRJ_VOL_D, PRJ_VOL_DW, PRJ_VOL_DWB,
                                                         PRJ_MODE)])
  ## only volume when set logfile = T, I should expect no projection by species
  testrun3_vol_byspecies <- VDYP8Run(polyFile = polyfile,
                                     layerFile = layerfile,
                                     BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                     utilTable = utilTableGenerator(all = 17.5,
                                                                    askForConfirm = FALSE),
                                     timeSeries = "age",
                                     startTime = 0,
                                     endTime = 250,
                                     timeIncrement = 5,
                                     projVolume = TRUE,
                                     logFile = TRUE)
  
  expect_equal(names(testrun3_vol_byspecies$yieldTable),
               c("FEATURE_ID", "LAYER_ID", "PROJECTION_YEAR", "PRJ_TOTAL_AGE",
                 "STANDCOMPOSITION", "PRJ_PCNT_STOCK", "PRJ_SITE_INDEX", "PRJ_DOM_HT",
                 "PRJ_LOREY_HT", "PRJ_DIAMETER", "PRJ_TPH", "PRJ_BA", "PRJ_VOL_WS",
                 "PRJ_VOL_CU", "PRJ_VOL_D", "PRJ_VOL_DW", "PRJ_VOL_DWB", "PRJ_MODE_VOL"))
  expect_equal(yieldtable_by_layer_test1,
               testrun3_vol_byspecies$yieldTable[,.(FEATURE_ID, LAYER_ID,
                                                    PROJECTION_YEAR, PRJ_TOTAL_AGE,
                                                    PRJ_PCNT_STOCK,
                                                    PRJ_SITE_INDEX,
                                                    PRJ_DOM_HT,
                                                    PRJ_SCND_HT = NA,
                                                    PRJ_LOREY_HT,
                                                    PRJ_DIAMETER, PRJ_TPH, PRJ_BA,
                                                    PRJ_VOL_WS, PRJ_VOL_CU,
                                                    PRJ_VOL_D, PRJ_VOL_DW, PRJ_VOL_DWB,
                                                    PRJ_MODE = PRJ_MODE_VOL)],
               tolerance = 0.01)
  ## test only volume simulation with year with by species
  testrun4_vol_byspecies <- VDYP8Run(polyFile = polyfile,
                                     layerFile = layerfile,
                                     BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                     utilTable = utilTableGenerator(all = 17.5,
                                                                    askForConfirm = FALSE),
                                     timeSeries = "year",
                                     startTime = 1970,
                                     endTime = 2020,
                                     timeIncrement = 5,
                                     projVolume = TRUE)
  
  expect_equal(names(testrun4_vol_byspecies),
               c("summary", "yieldTable", "warningTable",
                 "errorTable", "errorReport",
                 "logReport", "vdyp7Log", "metadata"))
  expect_equal(dim(testrun4_vol_byspecies$yieldTable), c(93, 64))
  expect_equal(names(testrun4_vol_byspecies$yieldTable),
               c("TABLE_NUM", "FEATURE_ID", "DISTRICT", "MAP_ID", "POLYGON_ID",
                 "LAYER_ID", "PROJECTION_YEAR", "PRJ_TOTAL_AGE", "SPECIES_1_CODE",
                 "SPECIES_1_PCNT", "SPECIES_2_CODE", "SPECIES_2_PCNT", "SPECIES_3_CODE",
                 "SPECIES_3_PCNT", "SPECIES_4_CODE", "SPECIES_4_PCNT", "SPECIES_5_CODE",
                 "SPECIES_5_PCNT", "SPECIES_6_CODE", "SPECIES_6_PCNT", "PRJ_PCNT_STOCK",
                 "PRJ_SITE_INDEX", "PRJ_DOM_HT", "PRJ_SCND_HT", "PRJ_LOREY_HT",
                 "PRJ_DIAMETER", "PRJ_TPH", "PRJ_BA", "PRJ_VOL_WS", "PRJ_VOL_CU",
                 "PRJ_VOL_D", "PRJ_VOL_DW", "PRJ_VOL_DWB", "PRJ_SP1_VOL_WS", "PRJ_SP1_VOL_CU",
                 "PRJ_SP1_VOL_D", "PRJ_SP1_VOL_DW", "PRJ_SP1_VOL_DWB", "PRJ_SP2_VOL_WS",
                 "PRJ_SP2_VOL_CU", "PRJ_SP2_VOL_D", "PRJ_SP2_VOL_DW", "PRJ_SP2_VOL_DWB",
                 "PRJ_SP3_VOL_WS", "PRJ_SP3_VOL_CU", "PRJ_SP3_VOL_D", "PRJ_SP3_VOL_DW",
                 "PRJ_SP3_VOL_DWB", "PRJ_SP4_VOL_WS", "PRJ_SP4_VOL_CU", "PRJ_SP4_VOL_D",
                 "PRJ_SP4_VOL_DW", "PRJ_SP4_VOL_DWB", "PRJ_SP5_VOL_WS", "PRJ_SP5_VOL_CU",
                 "PRJ_SP5_VOL_D", "PRJ_SP5_VOL_DW", "PRJ_SP5_VOL_DWB", "PRJ_SP6_VOL_WS",
                 "PRJ_SP6_VOL_CU", "PRJ_SP6_VOL_D", "PRJ_SP6_VOL_DW", "PRJ_SP6_VOL_DWB",
                 "PRJ_MODE"))
  ## test only volume simulation with age with by species
  ## forceRefYear = false
  testrun1_vol_byspecies2 <- VDYP8Run(polyFile = polyfile,
                                      layerFile = layerfile,
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = "age",
                                      startTime = 0,
                                      endTime = 250,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      forceRefYear = FALSE)
  expect_equal(names(testrun1_vol_byspecies2),
               c("summary", "yieldTable", "warningTable",
                 "errorTable", "errorReport",
                 "logReport", "vdyp7Log", "metadata"))
  expect_equal(dim(testrun1_vol_byspecies2$yieldTable), c(409, 64))
  # force current year
  testrun1_vol_byspecies3 <- VDYP8Run(polyFile = polyfile,
                                      layerFile = layerfile,
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = "age",
                                      startTime = 0,
                                      endTime = 250,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      forceCrtYear = TRUE)
  expect_equal(dim(testrun1_vol_byspecies3$yieldTable[PRJ_MODE == "Crnt",]), c(8, 64))
  
  # check force year
  testrun1_vol_byspecies4 <- VDYP8Run(polyFile = polyfile,
                                      layerFile = layerfile,
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = "age",
                                      startTime = 0,
                                      endTime = 250,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      forceCrtYear = TRUE,
                                      forceYear = 2006)
  expect_equal(dim(testrun1_vol_byspecies4$yieldTable[PRJ_MODE == "Crnt",]), c(8, 64))
  expect_equal(dim(testrun1_vol_byspecies4$yieldTable[PRJ_MODE == "Spcl",]), c(8, 64))
  expect_equal(unique(testrun1_vol_byspecies4$yieldTable[PRJ_MODE == "Spcl",]$PROJECTION_YEAR),
               2006)
  
  ## a special case project to a force year
  testrun1_vol_byspecies5 <- VDYP8Run(polyFile = polyfile,
                                      layerFile = layerfile,
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = NULL,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      forceCrtYear = TRUE,
                                      forceRefYear = TRUE,
                                      forceYear = 2006)
  
  expect_equal(dim(testrun1_vol_byspecies5$yieldTable), c(25, 64))
  expect_equal(dim(testrun1_vol_byspecies5$yieldTable[PRJ_MODE == "Crnt",]), c(8, 64))
  expect_equal(unique(testrun1_vol_byspecies5$yieldTable[PRJ_MODE == "Crnt",]$PROJECTION_YEAR),
               as.numeric(substr(Sys.time(), 1, 4)))
  expect_equal(dim(testrun1_vol_byspecies5$yieldTable[PRJ_MODE == "Ref",]), c(9, 64))
  refyear_prjed <- unique(testrun1_vol_byspecies5$yieldTable[PRJ_MODE == "Ref",
                                                             .(FEATURE_ID, REFERENCE_YEAR = PROJECTION_YEAR)])
  refyear_input <- unique(polyfile[,.(FEATURE_ID, REFERENCE_YEAR)])
  expect_equal(refyear_prjed, refyear_input)
  
  expect_equal(dim(testrun1_vol_byspecies5$yieldTable[PRJ_MODE == "Spcl",]), c(8, 64))
  expect_equal(unique(testrun1_vol_byspecies5$yieldTable[PRJ_MODE == "Spcl",]$PROJECTION_YEAR),
               2006)
  
  ## a special case only project to a force year
  testrun1_vol_byspecies6 <- VDYP8Run(polyFile = polyfile,
                                      layerFile = layerfile,
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = NULL,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      forceCrtYear = FALSE,
                                      forceRefYear = FALSE,
                                      forceYear = 2006)
  
  expect_equal(dim(testrun1_vol_byspecies6$yieldTable), c(9, 64)) # there is a ref year for dead layer
  
  ## turn the logfile on, so that the intermediate output format is txt
  testrun1_vol_byspecies7 <- VDYP8Run(polyFile = polyfile,
                                      layerFile = layerfile,
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = NULL,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      forceCrtYear = TRUE,
                                      forceRefYear = TRUE,
                                      forceYear = 2006,
                                      logFile = TRUE)
  
  expect_equal(dim(testrun1_vol_byspecies7$yieldTable), c(25, 18))
  expect_equal(dim(testrun1_vol_byspecies7$yieldTable[PRJ_MODE_VOL == "Crnt",]), c(8, 18))
  expect_equal(unique(testrun1_vol_byspecies7$yieldTable[PRJ_MODE_VOL == "Crnt",]$PROJECTION_YEAR),
               as.numeric(substr(Sys.time(), 1, 4)))
  expect_equal(dim(testrun1_vol_byspecies7$yieldTable[PRJ_MODE_VOL == "Ref",]), c(9, 18))
  refyear_prjed <- unique(testrun1_vol_byspecies7$yieldTable[PRJ_MODE_VOL == "Ref",
                                                             .(FEATURE_ID, REFERENCE_YEAR = PROJECTION_YEAR)])
  refyear_input <- unique(polyfile[,.(FEATURE_ID, REFERENCE_YEAR)])
  expect_equal(refyear_prjed, refyear_input)
  
  expect_equal(dim(testrun1_vol_byspecies7$yieldTable[PRJ_MODE_VOL == "Spcl",]), c(8, 18))
  expect_equal(unique(testrun1_vol_byspecies7$yieldTable[PRJ_MODE_VOL == "Spcl",]$PROJECTION_YEAR),
               2006)
  expect_equal(testrun1_vol_byspecies5$yieldTable[,
                                                  .(FEATURE_ID, LAYER_ID,
                                                    PROJECTION_YEAR, PRJ_TOTAL_AGE,
                                                    PRJ_PCNT_STOCK,
                                                    PRJ_SITE_INDEX,
                                                    PRJ_DOM_HT,
                                                    PRJ_SCND_HT,
                                                    PRJ_LOREY_HT,
                                                    PRJ_DIAMETER, PRJ_TPH, PRJ_BA,
                                                    PRJ_VOL_WS, PRJ_VOL_CU,
                                                    PRJ_VOL_D, PRJ_VOL_DW, PRJ_VOL_DWB,
                                                    PRJ_MODE)],
               testrun1_vol_byspecies7$yieldTable[,
                                                  .(FEATURE_ID, LAYER_ID,
                                                    PROJECTION_YEAR, PRJ_TOTAL_AGE,
                                                    PRJ_PCNT_STOCK,
                                                    PRJ_SITE_INDEX,
                                                    PRJ_DOM_HT,
                                                    PRJ_SCND_HT = NA,
                                                    PRJ_LOREY_HT,
                                                    PRJ_DIAMETER, PRJ_TPH, PRJ_BA,
                                                    PRJ_VOL_WS, PRJ_VOL_CU,
                                                    PRJ_VOL_D, PRJ_VOL_DW, PRJ_VOL_DWB,
                                                    PRJ_MODE = PRJ_MODE_VOL)],
               tolerance = 0.01)
  ## test secondary ht projection
  testrun1_vol_byspecies8 <- VDYP8Run(polyFile = polyfile[FEATURE_ID == 16160455,],
                                      layerFile = layerfile[FEATURE_ID == 16160455,],
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = "age",
                                      startTime = 0,
                                      endTime = 250,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      secondarySpcHt = TRUE)

  expect_equal(testrun1_vol_byspecies8$yieldTable$PRJ_SCND_HT,
               c(0, 0.10125, 0.21443, 0.42626, 0.82564, 1.34217, 1.65916, 2.11965,
                 2.67243, 3.29161, 3.95982, 4.66414, 5.39441, 6.14246, 6.90157,
                 7.66621, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 16.5,
                 16.81792, 17.41735, 18.00016, 18.5665, 19.1166, 19.65072, 20.16916,
                 20.67227, 21.1604, 21.63392, 22.09324, 22.53873, 22.97081, 23.38987,
                 23.79631, 24.19053, 24.57292, 24.94387, 25.30375, 25.65294, 25.99178,
                 26.32064, 26.63986, NA))
  
  testrun1_vol_byspecies9 <- VDYP8Run(polyFile = polyfile[FEATURE_ID == 16160455,],
                                      layerFile = layerfile[FEATURE_ID == 16160455,],
                                      BaseRESTAPIURL = "http://localhost:8080/api/v8/",
                                      utilTable = utilTableGenerator(all = 17.5,
                                                                     askForConfirm = FALSE),
                                      timeSeries = "age",
                                      startTime = 0,
                                      endTime = 250,
                                      timeIncrement = 5,
                                      projVolume = TRUE,
                                      secondarySpcHt = TRUE,
                                      logFile = TRUE)
  expect_equal(testrun1_vol_byspecies8$yieldTable$PRJ_SCND_HT,
               testrun1_vol_byspecies9$yieldTable$PRJ_SCND_HT,
               tolerance = 0.01)
})
