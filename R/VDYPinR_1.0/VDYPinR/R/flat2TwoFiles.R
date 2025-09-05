#' The function is to organize flat input (one input file) and return two input files
#'
#' @description This function is to organize flat input (one input file)
#'              and return two input files
#'
#' @param flatFile data.frame or data.table, The flat file.
#' @return a list two files layFile and polygonFile, which are two input files
#'         for VDYP7 projection
#' @importFrom data.table data.table setorder
#' @note the flatFile only contains primary layer and
#'       veteran layer
#'
#' @export flat2TwoFiles
#' @docType methods
#' @rdname flat2TwoFiles
#' @author Yingbing Chen, Yong Luo
flat2TwoFiles <- function(flatFile){
  flatFile <- data.table(flatFile)
  names(flatFile) <- toupper(names(flatFile))
  layer_p <- flatFile[,.(FEATURE_ID = FEATURE_ID,
                         TREE_COVER_LAYER_ESTIMATED_ID = NA,
                         MAP_ID = MAP_ID,
                         POLYGON_NUMBER = POLYGON_ID,
                         LAYER_LEVEL_CODE = R1_LAYER_LEVEL_CD,
                         VDYP7_LAYER_CD = "P",
                         LAYER_STOCKABILITY = NA,
                         FOREST_COVER_RANK_CODE = NA,
                         NON_FOREST_DESCRIPTOR_CODE  =  R1_NON_FOREST_DESCRIPTOR,
                         EST_SITE_INDEX_SPECIES_CD  =  R1_EST_SITE_INDEX_SPECIES_CD,
                         ESTIMATED_SITE_INDEX  =  R1_EST_SITE_INDEX,
                         CROWN_CLOSURE = R1_CROWN_CLOSURE,
                         BASAL_AREA_75 = R1_BASAL_AREA_75,
                         STEMS_PER_HA_75 = R1_VRI_LIVE_STEMS_PER_HA_75,
                         SPECIES_CD_1 = R1_SPECIES_CD_1,
                         SPECIES_PCT_1 = R1_SPECIES_PCT_1,
                         SPECIES_CD_2 = R1_SPECIES_CD_2,
                         SPECIES_PCT_2 = R1_SPECIES_PCT_2,
                         SPECIES_CD_3 = R1_SPECIES_CD_3,
                         SPECIES_PCT_3 = R1_SPECIES_PCT_3,
                         SPECIES_CD_4 = R1_SPECIES_CD_4,
                         SPECIES_PCT_4 = R1_SPECIES_PCT_4,
                         SPECIES_CD_5 = R1_SPECIES_CD_5,
                         SPECIES_PCT_5 = R1_SPECIES_PCT_5,
                         SPECIES_CD_6 = R1_SPECIES_CD_6,
                         SPECIES_PCT_6 = R1_SPECIES_PCT_6,
                         EST_AGE_SPP1 = R1_EST_AGE_SPP1,
                         EST_HEIGHT_SPP1 = R1_EST_HEIGHT_SPP1,
                         EST_AGE_SPP2 = R1_EST_AGE_SPP2,
                         EST_HEIGHT_SPP2 = R1_EST_HEIGHT_SPP2,
                         ADJ_IND = R1_ADJ_INPUT_ID,
                         LOREY_HEIGHT_75 = R1_LOREY_HEIGHT,
                         BASAL_AREA_125 = R1_BASAL_AREA_125,
                         WS_VOL_PER_HA_75 = R1_VOL_PER_HA_75,
                         WS_VOL_PER_HA_125 = R1_VOL_PER_HA_125,
                         CU_VOL_PER_HA_125 = R1_CLOSE_UTIL_VOL_125,
                         D_VOL_PER_HA_125 = R1_CLOSE_UTIL_DECAY_VOL_125,
                         DW_VOL_PER_HA_125 = R1_CLOSE_UTIL_WASTE_VOL_125)]
  layer_rs <- flatFile[!is.na(RS_LAYER_LEVEL_CD),
                       .(FEATURE_ID = FEATURE_ID,
                         TREE_COVER_LAYER_ESTIMATED_ID = NA,
                         MAP_ID = MAP_ID,
                         POLYGON_NUMBER = POLYGON_ID,
                         LAYER_LEVEL_CODE = RS_LAYER_LEVEL_CD,
                         VDYP7_LAYER_CD = "V",
                         LAYER_STOCKABILITY = NA,
                         FOREST_COVER_RANK_CODE = NA,
                         NON_FOREST_DESCRIPTOR_CODE  =  RS_NON_FOREST_DESCRIPTOR,
                         EST_SITE_INDEX_SPECIES_CD  =  RS_EST_SITE_INDEX_SPECIES_CD,
                         ESTIMATED_SITE_INDEX  =  RS_EST_SITE_INDEX,
                         CROWN_CLOSURE = RS_CROWN_CLOSURE,
                         BASAL_AREA_75 = RS_BASAL_AREA_75,
                         STEMS_PER_HA_75 = RS_VRI_LIVE_STEMS_PER_HA_75,
                         SPECIES_CD_1 = RS_SPECIES_CD_1,
                         SPECIES_PCT_1 = RS_SPECIES_PCT_1,
                         SPECIES_CD_2 = RS_SPECIES_CD_2,
                         SPECIES_PCT_2 = RS_SPECIES_PCT_2,
                         SPECIES_CD_3 = RS_SPECIES_CD_3,
                         SPECIES_PCT_3 = RS_SPECIES_PCT_3,
                         SPECIES_CD_4 = RS_SPECIES_CD_4,
                         SPECIES_PCT_4 = RS_SPECIES_PCT_4,
                         SPECIES_CD_5 = RS_SPECIES_CD_5,
                         SPECIES_PCT_5 = RS_SPECIES_PCT_5,
                         SPECIES_CD_6 = RS_SPECIES_CD_6,
                         SPECIES_PCT_6 = RS_SPECIES_PCT_6,
                         EST_AGE_SPP1 = RS_EST_AGE_SPP1,
                         EST_HEIGHT_SPP1 = RS_EST_HEIGHT_SPP1,
                         EST_AGE_SPP2 = RS_EST_AGE_SPP2,
                         EST_HEIGHT_SPP2 = RS_EST_HEIGHT_SPP2,
                         ADJ_IND = RS_ADJ_INPUT_ID,
                         LOREY_HEIGHT_75 = RS_LOREY_HEIGHT,
                         BASAL_AREA_125 = RS_BASAL_AREA_125,
                         WS_VOL_PER_HA_75 = RS_VOL_PER_HA_75,
                         WS_VOL_PER_HA_125 = RS_VOL_PER_HA_125,
                         CU_VOL_PER_HA_125 = RS_CLOSE_UTIL_VOL_125,
                         D_VOL_PER_HA_125 = RS_CLOSE_UTIL_DECAY_VOL_125,
                         DW_VOL_PER_HA_125 = RS_CLOSE_UTIL_WASTE_VOL_125)]

  layer <- rbind(layer_p, layer_rs)
  layer <- unique(layer,
                  by = c("FEATURE_ID", "VDYP7_LAYER_CD"))
  layer <- layer[order(FEATURE_ID, VDYP7_LAYER_CD),]

  poly <- flatFile[,.(FEATURE_ID = FEATURE_ID,
                      MAP_ID = MAP_ID,
                      POLYGON_NUMBER = POLYGON_ID,
                      ORG_UNIT = ORG_UNIT_CODE,
                      TSA_NAME = "UNK",
                      TFL_NAME = "UNK",
                      INVENTORY_STANDARD_CODE = INVENTORY_STANDARD_CD,
                      TSA_NUMBER = "UNK",
                      SHRUB_HEIGHT = SHRUB_HEIGHT,
                      SHRUB_CROWN_CLOSURE = SHRUB_CROWN_CLOSURE,
                      SHRUB_COVER_PATTERN = SHRUB_COVER_PATTERN,
                      HERB_COVER_TYPE_CODE = HERB_COVER_TYPE,
                      HERB_COVER_PCT = HERB_COVER_PCT,
                      HERB_COVER_PATTERN_CODE = HERB_COVER_PATTERN,
                      BRYOID_COVER_PCT = BRYOID_COVER_PCT,
                      BEC_ZONE_CODE = BEC_ZONE_CD,
                      CFS_ECOZONE = NA,
                      PRE_DISTURBANCE_STOCKABILITY = STOCKABILITY,
                      YIELD_FACTOR = YIELD_FACTOR,
                      NON_PRODUCTIVE_DESCRIPTOR_CD = NON_PRODUCTIVE_DESCRIPTOR_CD,
                      BCLCS_LEVEL1_CODE = BCLCS_LEVEL_1,
                      BCLCS_LEVEL2_CODE = BCLCS_LEVEL_2,
                      BCLCS_LEVEL3_CODE = BCLCS_LEVEL_3,
                      BCLCS_LEVEL4_CODE = BCLCS_LEVEL_4,
                      BCLCS_LEVEL5_CODE = BCLCS_LEVEL_5,
                      PHOTO_ESTIMATION_BASE_YEAR = REFERENCE_YEAR,
                      REFERENCE_YEAR = REFERENCE_YEAR,
                      PCT_DEAD = NA,
                      NON_VEG_COVER_TYPE_1 = NON_VEG_COVER_TYPE_1,
                      NON_VEG_COVER_PCT_1 = NON_VEG_COVER_PCT_1,
                      NON_VEG_COVER_PATTERN_1 = NON_VEG_COVER_PATTERN_1,
                      NON_VEG_COVER_TYPE_2 = NON_VEG_COVER_TYPE_2,
                      NON_VEG_COVER_PCT_2 = NON_VEG_COVER_PCT_2,
                      NON_VEG_COVER_PATTERN_2 = NON_VEG_COVER_PATTERN_2,
                      NON_VEG_COVER_TYPE_3 = NON_VEG_COVER_TYPE_3,
                      NON_VEG_COVER_PCT_3 = NON_VEG_COVER_PCT_3,
                      NON_VEG_COVER_PATTERN_3 = NON_VEG_COVER_PATTERN_3,
                      LAND_COVER_CLASS_CD_1 = LAND_COVER_CLASS_CD_1,
                      LAND_COVER_PCT_1 = LAND_COVER_PCT_1,
                      LAND_COVER_CLASS_CD_2 = LAND_COVER_CLASS_CD_2,
                      LAND_COVER_PCT_2 = LAND_COVER_PCT_2,
                      LAND_COVER_CLASS_CD_3 = LAND_COVER_PCT_3,
                      LAND_COVER_PCT_3 = LAND_COVER_PCT_3)]
  poly <- unique(poly, by = "FEATURE_ID")
  setorder(poly, FEATURE_ID)
  return(list(layerFile = layer,
              polygonFile = poly)) ##some feature_ids in the flat file have duplicated records
}

