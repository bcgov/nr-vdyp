#' The function is to examine whether the data is valid for VDYP7 run
#'
#' @description The VDYP7 takes two input files poly file and layer file. This function is to
#'              examine whether those two input files are valid for VDYP7 run, in terms of
#'              1) data structure and 2) data matchness
#'
#' @param polyFile data.frame or data.table, The poly file for VDYP7 run.
#'
#' @param layerFile data.frame or data.table, The layer file for VDYP7 run.
#'
#' @importFrom data.table data.table
#' @return a text file to present the results
#'
#' @export
#' @docType methods
#' @rdname validInput
#'
#' @author Yong Luo
validInput <- function(polyFile, layerFile){
  polyPass <- TRUE
  layerPass <- TRUE
  matchnessPass <- TRUE
  polyfile_refNames <- c("FEATURE_ID", "MAP_ID", "POLYGON_NUMBER", "ORG_UNIT", "TSA_NAME",
                         "TFL_NAME", "INVENTORY_STANDARD_CODE", "TSA_NUMBER", "SHRUB_HEIGHT",
                         "SHRUB_CROWN_CLOSURE", "SHRUB_COVER_PATTERN", "HERB_COVER_TYPE_CODE",
                         "HERB_COVER_PCT", "HERB_COVER_PATTERN_CODE", "BRYOID_COVER_PCT",
                         "BEC_ZONE_CODE", "CFS_ECOZONE", "PRE_DISTURBANCE_STOCKABILITY", "YIELD_FACTOR",
                         "NON_PRODUCTIVE_DESCRIPTOR_CD", "BCLCS_LEVEL1_CODE", "BCLCS_LEVEL2_CODE",
                         "BCLCS_LEVEL3_CODE", "BCLCS_LEVEL4_CODE", "BCLCS_LEVEL5_CODE",
                         "PHOTO_ESTIMATION_BASE_YEAR", "REFERENCE_YEAR", "PCT_DEAD", "NON_VEG_COVER_TYPE_1",
                         "NON_VEG_COVER_PCT_1", "NON_VEG_COVER_PATTERN_1", "NON_VEG_COVER_TYPE_2",
                         "NON_VEG_COVER_PCT_2", "NON_VEG_COVER_PATTERN_2", "NON_VEG_COVER_TYPE_3",
                         "NON_VEG_COVER_PCT_3", "NON_VEG_COVER_PATTERN_3", "LAND_COVER_CLASS_CD_1",
                         "LAND_COVER_PCT_1", "LAND_COVER_CLASS_CD_2", "LAND_COVER_PCT_2",
                         "LAND_COVER_CLASS_CD_3", "LAND_COVER_PCT_3")


  if(ncol(polyFile) != 43){
    polyPass <- FALSE
    polyFile_names <- names(polyFile)
    polyFile_names_missing <- polyfile_refNames[!(polyfile_refNames %in% polyFile_names)]
    polyFile_names_addition <- polyFile_names[!(polyFile_names %in% polyfile_refNames)]
    polyfail_reason <- paste0("polyFile misses essential column(s): \n",
                              paste0("    ", polyFile_names_missing, collapse = "\n"),
                              "\npolyFile has more column(s) than expected: \n",
                              paste0("    ", polyFile_names_addition, collapse = "\n"))

  } else {
    if(!identical(names(polyFile), polyfile_refNames)){
      polyPass <- FALSE
      thecomparisontable1 <- data.table::data.table(column = 1:42,
                                                    columnName_Ref = polyfile_refNames,
                                                    columnName_Now = names(polyFile))
      thecomparisontable1 <- thecomparisontable1[columnName_Ref != columnName_Now, ]
      polyfail_reason <- paste0("polyFile must be same as reference name for given columns: \n",
                             paste0(capture.output(thecomparisontable1),
                                    collapse = "\n"))


    } else if (nrow(polyFile) != length(unique(polyFile$FEATURE_ID))){ # examine the duplicate of feature id
      polyPass <- FALSE
      polyFile_featurid <- data.table::data.table(polyFile)
      polyFile_featurid[, featurelength := length(MAP_ID), by = "FEATURE_ID"]
      polyFile_featurid <- unique(polyFile_featurid[featurelength > 1,]$FEATURE_ID)
      polyfail_reason <- paste0("polyFile have duplicate FEATUR_ID: \n",
                             paste0("    ", polyFile_featurid, collapse = "\n"))
    }
  }


  layerfile_refNames <- c("FEATURE_ID", "TREE_COVER_LAYER_ESTIMATED_ID", "MAP_ID", "POLYGON_NUMBER",
                          "LAYER_LEVEL_CODE", "VDYP7_LAYER_CD", "LAYER_STOCKABILITY", "FOREST_COVER_RANK_CODE",
                          "NON_FOREST_DESCRIPTOR_CODE", "EST_SITE_INDEX_SPECIES_CD", "ESTIMATED_SITE_INDEX",
                          "CROWN_CLOSURE", "BASAL_AREA_75", "STEMS_PER_HA_75", "SPECIES_CD_1",
                          "SPECIES_PCT_1", "SPECIES_CD_2", "SPECIES_PCT_2", "SPECIES_CD_3",
                          "SPECIES_PCT_3", "SPECIES_CD_4", "SPECIES_PCT_4", "SPECIES_CD_5",
                          "SPECIES_PCT_5", "SPECIES_CD_6", "SPECIES_PCT_6", "EST_AGE_SPP1",
                          "EST_HEIGHT_SPP1", "EST_AGE_SPP2", "EST_HEIGHT_SPP2", "ADJ_IND",
                          "LOREY_HEIGHT_75", "BASAL_AREA_125", "WS_VOL_PER_HA_75", "WS_VOL_PER_HA_125",
                          "CU_VOL_PER_HA_125", "D_VOL_PER_HA_125", "DW_VOL_PER_HA_125")
  if(ncol(layerFile) != 38){
    layerPass <- FALSE
    layerFile_names <- names(layerFile)
    layerFile_names_missing <- layerfile_refNames[!(layerfile_refNames %in% layerFile_names)]
    layerFile_names_addition <- layerFile_names[!(layerFile_names %in% layerfile_refNames)]
    layerfail_reason <- paste0("layerFile misses essential column(s): \n",
                               paste0("    ", layerFile_names_missing, collapse = "\n"),
                              "\nlayerFile has more column(s) than expected: \n",
                              paste0("    ", layerFile_names_addition, collapse = "\n"))
  } else {
    if(!identical(names(layerFile),
               layerfile_refNames)){
      thecomparisontable2 <- data.table::data.table(column = 1:38,
                                                    columnName_Ref = layerfile_refNames,
                                                    columnName_Now = names(layerFile))
      thecomparisontable2 <- thecomparisontable2[columnName_Ref != columnName_Now,]
      layerfail_reason <- paste0("layerFile must be same as reference name for given columns: \n",
                                 paste0(capture.output(thecomparisontable2),
                                        collapse = "\n"))
      layerPass <- FALSE
    } else if (nrow(layerFile) != nrow(unique(layerFile, by = c("FEATURE_ID", "LAYER_LEVEL_CODE")))){
      layerPass <- FALSE
      layerFile_featurid <- data.table::data.table(layerFile)
      layerFile_featurid[, featurelength := length(MAP_ID), by = c("FEATURE_ID",
                                                                   "LAYER_LEVEL_CODE")]
      layerFile_featurid <- unique(layerFile_featurid[featurelength > 1,]$FEATURE_ID)
      layerfail_reason <- paste0("layerFile have duplicate FEATUR_ID: \n",
                              paste0("    ", layerFile_featurid, collapse = "\n"))
    }
  }
  if(polyPass & layerPass){
      inpolyfeature <- polyFile$FEATURE_ID[!(polyFile$FEATURE_ID %in% layerFile$FEATURE_ID)]
      inlayerfeature <- layerFile$FEATURE_ID[!(layerFile$FEATURE_ID %in% polyFile$FEATURE_ID)]
    if(length(inpolyfeature) > 0 | length(inlayerfeature) > 0){
      matchnessPass <- FALSE
      matchnessfail_reason <- paste0("FEATURE_ID in polyFile can not be found in layerFile: \n",
                                     paste0("    ", inpolyfeature, collapse = "\n"),
                                     "\nFEATURE_ID in layerFile can not be found in polyFile: \n",
                                     paste0("    ", inlayerfeature, collapse = "\n"))
    } else {
      message("    polyFile passed valid check; \n",
              "    layerFile passed valid check; \n",
              "    both polyFile and layerFile passed matchness check.")
    }
  }

  if(!polyPass){
    message("polyFile does not pass valid check, for the reason:\n",
            polyfail_reason)
  }
  if(!layerPass){
    message("layerFile does not pass valid check, for the reason:\n",
            layerfail_reason)
  }
  if(!matchnessPass){
    message("polyFile and layerFile do not matched well:\n",
            matchnessfail_reason)
  }
}
