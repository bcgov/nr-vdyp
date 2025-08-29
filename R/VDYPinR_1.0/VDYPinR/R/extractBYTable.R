#' The function is to extract yield tables when biomass is turned on
#'
#' @description The function is to extract yield tables when biomass is turned on
#'
#' @param rawYieldTable data.frame or data.table, The yield table with biomass is turned on.
#'
#' @param volIncluded logical, Specifies whether the volume yield table is turned on.
#' @param biomIncluded logical, Specifies whether the biomass yield table is turned on.
#'
#' @importFrom data.table data.table
#' @return biomass yield table, and volume yield table is volume is turned on
#'
#' @export
#' @docType methods
#' @rdname extractBYTable
#'
#' @author Yingbing Chen, Yong Luo
extractBYTable <- function(rawYieldTable,
                           volIncluded,
                           biomIncluded){
  names(rawYieldTable) <- "tmpName"
  group <- cumsum(grepl("District",rawYieldTable$tmpName))
  rawYieldTable <- split(rawYieldTable, group)
  logfile <- rawYieldTable$`0`
  fid <- unlist(lapply(rawYieldTable,function(test){unlist(strsplit(test$tmpName[1],split="\\("))[2]}))
  fid <- gsub("Rcrd ID:", "", fid)
  fid <- gsub("\\)", "", fid)
  fid <- gsub(" ", "", fid)
  layers <- unlist(lapply(rawYieldTable,function(test){unlist(strsplit(test$tmpName[1],split="Layer:"))[2]}))
  layers <- unlist(lapply(layers,function(test){unlist(strsplit(test,split="\\(Rcrd"))[1]}))
  layers <- gsub(" ", "", layers)
  volyieldtable <- NULL
  biomassytable <- NULL
  lastrow_vol <- 0
  lastrow_biom <- 0
  for( i in 2:length(rawYieldTable)){
    group2 <- cumsum(grepl("vvvvvvvvvv",rawYieldTable[[c(i)]][,1]))
    tmpsplit <- split(rawYieldTable[[c(i)]][1],group2)
    if(volIncluded){
      vol_indi <- tmpsplit[[1]]
      if(length(tmpsplit) == 2){
        biom_indi <- tmpsplit[[2]]
      } else {
        biom_indi <- NULL
      }
      vol_indi_names <- vol_indi$tmpName[2]
      vol_indi_splitmark <- vol_indi$tmpName[3]
      vol_indi_table <- vol_indi$tmpName[4:(nrow(vol_indi)-1)]
      cutnumber <- c(1, unlist(gregexpr(" ", vol_indi_splitmark)), nchar(vol_indi_names))
      indivolumeT <- suppressWarnings(data.frame(FEATURE_ID = rep(fid[i], length(vol_indi_table)),
                                                 LAYER_ID = substr(layers[i], 1, 1)))
      for (indicut in 2:length(cutnumber)) {
        indiname <- gsub(" ", "",
                         substr(vol_indi_names, cutnumber[(indicut-1)], cutnumber[indicut]))
        indivalues <- gsub(" ", "",
                           substr(vol_indi_table, cutnumber[(indicut-1)], cutnumber[indicut]))
        indicolumn <- data.frame(a = indivalues)
        names(indicolumn) <- indiname
        indivolumeT <- cbind(indivolumeT, indicolumn)
        rm(indicolumn)
      }
      volyieldtable <- rbind(volyieldtable,
                             indivolumeT)
      lastrow_vol <- i
      rm(indivolumeT)
    } else if (biomIncluded){
      biom_indi <- tmpsplit[[1]]
    }
    if(biomIncluded & !is.null(biom_indi)){
      biom_indi_names <- biom_indi$tmpName[2]
      biom_indi_splitmark <- biom_indi$tmpName[3]
      biom_indi_table <- biom_indi$tmpName[4:(nrow(biom_indi)-1)]
      cutnumber <- c(1, unlist(gregexpr(" ", biom_indi_splitmark)), nchar(biom_indi_names))
      indibiomassT <- suppressWarnings(data.frame(FEATURE_ID = rep(fid[i], length(biom_indi_table)),
                                                  LAYER_ID = substr(layers[i], 1, 1)))
      for (indicut in 2:length(cutnumber)) {
        indiname <- gsub(" ", "",
                         substr(biom_indi_names, cutnumber[(indicut-1)], cutnumber[indicut]))
        indivalues <- gsub(" ", "",
                           substr(biom_indi_table, cutnumber[(indicut-1)], cutnumber[indicut]))
        indicolumn <- data.frame(a = indivalues)
        names(indicolumn) <- indiname
        indibiomassT <- cbind(indibiomassT, indicolumn)
        rm(indicolumn)
      }
      biomassytable <- rbind(biomassytable,
                             indibiomassT)
      lastrow_biom <- i
      nrow(indibiomassT)
    }
  }
  if(!is.null(biomassytable)){
    biomassytable <- data.table(biomassytable)
    setnames(biomassytable,
             c("Year", "Age", "Mode"),
             c("PROJECTION_YEAR", "PRJ_TOTAL_AGE", "PRJ_MODE_BIOM"))
    biomassytable[,':='(FEATURE_ID = as.numeric(FEATURE_ID),
                        PROJECTION_YEAR = as.numeric(PROJECTION_YEAR),
                        PRJ_TOTAL_AGE = as.numeric(PRJ_TOTAL_AGE),
                        Bstem = as.numeric(Bstem),
                        Bbark = as.numeric(Bbark),
                        Bbranch = as.numeric(Bbranch),
                        Bfol = as.numeric(Bfol))]
  }
  if(lastrow_biom >= lastrow_vol){
    biomassytable <- biomassytable[-nrow(biomassytable),]
  } else {
    volyieldtable <- volyieldtable[-nrow(volyieldtable),]
  }
  if(volIncluded){
    # rename table according to csvyieldtable
    volyieldtable <- data.table(volyieldtable)
    setnames(volyieldtable,
             c("Year", "Age", "%Stk",
               "SI", "DHgt", "LHgt", "Dia", "TPH", "BA", "Vws", "Vcu", "Vd",
               "Vdw", "Vdwb", "Mode"),
             c("PROJECTION_YEAR", "PRJ_TOTAL_AGE",
               "PRJ_PCNT_STOCK",
               "PRJ_SITE_INDEX", "PRJ_DOM_HT", "PRJ_LOREY_HT", "PRJ_DIAMETER",
               "PRJ_TPH", "PRJ_BA", "PRJ_VOL_WS", "PRJ_VOL_CU", "PRJ_VOL_D",
               "PRJ_VOL_DW", "PRJ_VOL_DWB", "PRJ_MODE_VOL"))
    volyieldtable[,':='(FEATURE_ID = as.numeric(FEATURE_ID),
                        PROJECTION_YEAR = as.numeric(PROJECTION_YEAR),
                        PRJ_TOTAL_AGE = as.numeric(PRJ_TOTAL_AGE),
                        PRJ_PCNT_STOCK = as.numeric(PRJ_PCNT_STOCK),
                        PRJ_SITE_INDEX = as.numeric(PRJ_SITE_INDEX),
                        PRJ_DOM_HT = as.numeric(PRJ_DOM_HT),
                        PRJ_LOREY_HT = as.numeric(PRJ_LOREY_HT),
                        PRJ_DIAMETER = as.numeric(PRJ_DIAMETER),
                        PRJ_TPH = as.numeric(PRJ_TPH),
                        PRJ_BA = as.numeric(PRJ_BA),
                        PRJ_VOL_WS = as.numeric(PRJ_VOL_WS),
                        PRJ_VOL_CU = as.numeric(PRJ_VOL_CU),
                        PRJ_VOL_D = as.numeric(PRJ_VOL_D),
                        PRJ_VOL_DW = as.numeric(PRJ_VOL_DW),
                        PRJ_VOL_DWB = as.numeric(PRJ_VOL_DWB))]
    if("SHgt" %in% names(volyieldtable)){
      setnames(volyieldtable, "SHgt", "PRJ_SCND_HT")
      volyieldtable[, PRJ_SCND_HT := as.numeric(PRJ_SCND_HT)]
    }
  }
  if(volIncluded & biomIncluded){
    biomassytable[,':='(StandComposition = NULL,
                        Vcu = NULL)]
    volyieldtable[, VOL_PROJ := TRUE]
    biomassytable[, BIOM_PROJ := TRUE]
    output <- merge(volyieldtable,
                    biomassytable,
                    by = c("FEATURE_ID", "LAYER_ID", "PROJECTION_YEAR", "PRJ_TOTAL_AGE"),
                    all = TRUE)
  } else if(volIncluded){
    output <- volyieldtable
  } else if(biomIncluded){
    output <- biomassytable
  }
  names(output) <- toupper(names(output))
  return(list(yieldTable = output,
              logfile = logfile))
}
