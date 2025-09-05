test_that("VDYP7Save.R: check save function.", {
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
  ## project biomass with volume
  testrun1 <- VDYP7Run(polyFile = polyfile,
                       layerFile = layerfile,
                       utilTable = utilTableGenerator(all = 17.5,
                                                      askForConfirm = FALSE),
                       timeSeries = "age",
                       startTime = 0,
                       endTime = 250,
                       timeIncrement = 5,
                       projVolume = TRUE,
                       projBiomass = TRUE,
                       forceCrtYear = FALSE,
                       forceRefYear = FALSE)

  yieldtable1 <- testrun1$yieldTable
  setkey(yieldtable1, NULL)
  savedir_parent <- tempdir()

  testformats <- c("rds", "txt", "csv", "xlsx")
  for(indiformat in testformats){
    savedir_indi <- file.path(savedir_parent, paste0("test_", indiformat))
    if(dir.exists(savedir_indi)){
      unlink(savedir_indi, recursive = TRUE)
    }
    dir.create(savedir_indi)
    VDYP7Save(testrun1,
              savePath = savedir_indi,
              saveName = "test1",
              saveFmt = indiformat,
              metadata = FALSE)

    expect_equal(sort(dir(savedir_indi)),
                 c("test1_VDYP.log",
                   "test1_errorReport.txt",
                   paste0("test1_errorTable.", indiformat),
                   "test1_logReport.txt",
                   "test1_summary.txt",
                   paste0("test1_warningTable.", indiformat),
                   paste0("test1_yieldtable.", indiformat)))
    if(indiformat == "rds"){
    yieldtable_from_folder <- readRDS(file.path(savedir_indi, "test1_yieldtable.rds"))
    } else if(indiformat == "txt"){
      yieldtable_from_folder <- read.table(file.path(savedir_indi, "test1_yieldtable.txt"),
                                           header = TRUE,
                                           sep = ",",
                                           na.strings = NA) %>% data.table
    } else if(indiformat == "csv"){
      yieldtable_from_folder <- read.csv(file.path(savedir_indi, "test1_yieldtable.csv"),
                                         sep = ",") %>% data.table
    } else {
      yieldtable_from_folder <- openxlsx::read.xlsx(file.path(savedir_indi,
                                                              "test1_yieldtable.xlsx")) %>%
        data.table
    }
    setkey(yieldtable_from_folder, NULL)
    expect_equal(yieldtable1, yieldtable_from_folder)
    unlink(savedir_indi, recursive = TRUE)
  }

  # test meta data to see if it can be run for both vol and biomass
  savedir_meta <- file.path(savedir_parent, "test_meta")
  if(dir.exists(savedir_meta)){
    unlink(savedir_meta, recursive = TRUE)
  }
  dir.create(savedir_meta)
  VDYP7Save(testrun1,
            savePath = savedir_meta,
            saveName = "meta",
            saveFmt = "csv",
            metadata = TRUE)
  yieldtable_org <- read.csv(file.path(savedir_meta, "meta_yieldtable.csv")) %>% data.table
  errortable_org <- read.csv(file.path(savedir_meta, "meta_errorTable.csv")) %>% data.table
  warningtable_org <- read.csv(file.path(savedir_meta, "meta_warningTable.csv")) %>% data.table
  file.remove(c(file.path(savedir_meta, "meta_errorReport.txt"),
                file.path(savedir_meta, "meta_errorTable.csv"),
                file.path(savedir_meta, "meta_logReport.txt"),
                file.path(savedir_meta, "meta_summary.txt"),
                file.path(savedir_meta, "meta_VDYP.log"),
                file.path(savedir_meta, "meta_warningTable.csv"),
                file.path(savedir_meta, "meta_yieldtable.csv")))
  shell(file.path(savedir_meta, "meta_runSimulation.cmd"),
        wait = TRUE, intern = TRUE)
  yieldtable_new_all <- read.csv(file.path(savedir_meta, "yieldTable.csv"))
  yieldtable_new_all <- extractBYTable(yieldtable_new_all,
                                   volIncluded = TRUE,
                                   biomIncluded = TRUE)
  setkey(yieldtable_new_all$yieldTable, NULL)
  expect_equal(yieldtable_org, yieldtable_new_all$yieldTable)
  unlink(savedir_meta, recursive = TRUE)


  # test volume only
  testrun2 <- VDYP7Run(polyFile = polyfile,
                       layerFile = layerfile,
                       utilTable = utilTableGenerator(all = 17.5,
                                                      askForConfirm = FALSE),
                       timeSeries = "age",
                       startTime = 0,
                       endTime = 250,
                       timeIncrement = 5,
                       projVolume = TRUE,
                       projBiomass = FALSE,
                       forceCrtYear = FALSE,
                       forceRefYear = FALSE)
  savedir_meta <- file.path(savedir_parent, "test_meta")
  if(dir.exists(savedir_meta)){
    unlink(savedir_meta, recursive = TRUE)
  }
  dir.create(savedir_meta)
  VDYP7Save(testrun2,
            savePath = savedir_meta,
            saveName = "meta",
            saveFmt = "csv",
            metadata = TRUE)
  yieldtable_org <- read.csv(file.path(savedir_meta, "meta_yieldtable.csv")) %>% data.table
  file.remove(c(file.path(savedir_meta, "meta_errorReport.txt"),
                file.path(savedir_meta, "meta_errorTable.csv"),
                file.path(savedir_meta, "meta_logReport.txt"),
                file.path(savedir_meta, "meta_summary.txt"),
                file.path(savedir_meta, "meta_VDYP.log"),
                file.path(savedir_meta, "meta_warningTable.csv"),
                file.path(savedir_meta, "meta_yieldtable.csv")))
  shell(file.path(savedir_meta, "meta_runSimulation.cmd"),
        wait = TRUE, intern = TRUE)
  yieldtable_new_all <- read.csv(file.path(savedir_meta, "yieldTable.txt")) %>%
    data.table
  setkey(yieldtable_new_all$yieldTable, NULL)
  expect_equal(testrun2$yieldTable, yieldtable_new_all)
  unlink(savedir_meta, recursive = TRUE)
})
