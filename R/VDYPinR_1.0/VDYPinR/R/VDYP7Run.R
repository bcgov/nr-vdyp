#' The function is to run VDYP7 in R environment
#'
#' @description This function is to prepare two input files (\code{polyFile} and \code{layerFile});
#'              to prepare VDYP configuration and cmd file; to run VDYP7; and load VDYP7 output into
#'              R environment. This function does not support debug mode. To run debug mode, you may
#'              use \code{\link{VDYP7Debug}}.
#'
#' @param polyFile data.frame or data.table, The poly file for VDYP7 run.
#'
#' @param layerFile data.frame or data.table, The layer file for VDYP7 run.
#'
#' @param utilTable data.frame or data.table,
#'                  The table contains utilization level for 16 species groups. It can be generated
#'                  using \code{\link{utilTableGenerator}}.
#'
#' @param VDYP7consolePath character, Specifies where your VDYP7console is located.
#'                         The VDYP7 is recommended to be installed at \code{C:/VDYP7}, therefore,
#'                         the default value for this arguement is \code{C:/VDYP7}. Please specify if
#'                         the software bas not been installed in this directory.
#'
#' @param timeSeries character, Specifies time series for the simulation from
#'                              either \code{age}, \code{year} or \code{NULL}.
#'                              If \code{NULL}, forceYear must be specified.
#'
#' @param startTime integer, Specifies the time to start simulation.
#'
#' @param endTime integer, Specifies the time to terminate simulation.
#'
#' @param timeIncrement integer, Specifies the time interval to summary simulation results.
#'                     Default is \code{10} years.
#' @param backward logical, Whether want to do backward simulation. Default is \code{TRUE}.
#' @param forward logical, Whether want to do forward simulation. Default is \code{TRUE}.
#' @param allowBatphsub logical, Whether to allow Batphsub. (what is this), Default is \code{FALSE}.
#' @param yTableIncProjMode logical, Whether want to use proj mode to run simulation. Default is \code{TRUE}.
#' @param yTableIncAge logical, Whether want to include age column in yield table. Default is \code{TRUE}.
#' @param yTableIncYear logical, Whether want to include Year column in yield table. Default is \code{TRUE}.
#' @param yTableIncPolyID logical, Whether yield table include polygon id. Default is \code{TRUE}.
#' @param forceYear numeric, Specifies the force year, so that it can be included in yield table.
#' @param forceRefYear logical, Whether want to include reference year in yield table. Default is \code{TRUE}.
#' @param forceCrtYear logical, Whether want to include current year in yield table. Default is \code{FALSE}.
#' @param secondarySpcHt logical, Whether want to project secondary species height in yield table. Default is \code{FALSE}.
#' @param projByLayer logical, Whether allow projection summarized by layer???. Default is \code{TRUE}.
#' @param projBySpecies logical, Whether allow projection summarized by species. Default is \code{TRUE}.
#' @param projVolume logical, whether allow the output include volume. Default is \code{TRUE}.
#' @param projBiomass logical, whether allow the output include biomass. Default is \code{FALSE}.
#' @param logFile logical, whether output meta data. Default is \code{FALSE}.
#'
#' @return a list that contains
#'         1) simulated stand yield table (equivalent to \code{-o} in VDYP7 console);
#'         2) processing message (equivalent to \code{-e} in VDYP7 console);
#'         3) log message (equivalent to \code{-l} in VDYP7 console);
#'         4) may contain log message from core module (equivalent to \code{-v7log} in VDYP7 console);
#'         5) metadata to run the simulation (cmd file and console specifications)
#'
#' @importFrom data.table data.table ':='
#' @importFrom fs dir_copy
#' @note 1) when the projBiomass = T, the unitilization level will be overwritten. And the volume and biomass
#'          summaries will be derived using the new unitilization level.
#'            AC   --> 12.5
#'            AT   --> 12.5
#'            B    --> 17.5
#'            C    --> 17.5
#'            D    --> 12.5
#'            E    --> 12.5
#'            F    --> 17.5
#'            H    --> 17.5
#'            L    --> 12.5
#'            MB   --> 12.5
#'            PA   --> 12.5
#'            PL   --> 12.5
#'            PW   --> 12.5
#'            PY   --> 12.5
#'            S    --> 17.5
#'            Y    --> 17.5
#'
#'
#' @export
#' @docType methods
#' @rdname VDYP7Run
#' @seealso see \code{\link{VDYP7Save}} to save results; \code{\link{VDYP7RunParallel}} to run VDYP7 parallel.
#'          see \code{\link{VDYP7Debug}} to turn on debug mode.
#' @author Yong Luo
VDYP7Run <- function(polyFile,
                     layerFile,
                     utilTable,
                     VDYP7consolePath = "C:/VDYP7",
                     timeSeries,
                     startTime,
                     endTime,
                     timeIncrement = 10,
                     backward = TRUE,
                     forward = TRUE,
                     allowBatphsub = FALSE,
                     yTableIncProjMode = TRUE,
                     yTableIncAge = TRUE,
                     yTableIncYear = TRUE,
                     yTableIncPolyID = TRUE,
                     forceYear = NULL, #
                     forceRefYear = TRUE,
                     forceCrtYear = FALSE,
                     secondarySpcHt = FALSE,
                     projByLayer = TRUE,
                     projBySpecies = TRUE,
                     projVolume = TRUE,
                     projBiomass = FALSE,
                     logFile = FALSE){
  if(projByLayer == FALSE){
    stop("projByLayer must be TRUE, otherwise no yield table will be produced.")
  }
  if(projBySpecies == TRUE & projBiomass == TRUE){
    warning("Biomass can not be projected by species. If you want volume by species, please specify projBiomass as FALSE.")
  } # Yingbing will check whether the logFile = ture can trigger warning message.
  if(projBySpecies == TRUE & logFile == TRUE){
    warning("Projection by species can not be produced when logFile is set as TRUE.")
  } # Yingbing will check whether the logFile = ture can trigger warning message.
  polyFile <- data.table(polyFile)
  layerFile <- data.table(layerFile)
  if(!file.exists(file.path(VDYP7consolePath, "VDYP7Console.exe"))){
    stop("VDYP7Console.exe can not be found in ", VDYP7consolePath, ". Please specify a correct path.")
  }
  systemtime_start <- Sys.time()
  ## prepare a temp folder to save input data
  temppath1 <- tempdir()
  # temppath1 <- "D:/VDYP7inR/examples/tempfolder"
  makedir <- TRUE
  folderindex <- 1
  while (makedir) {
    temppath <- file.path(temppath1, folderindex)
    if(!dir.exists(temppath)){
      dir.create(temppath)
      makedir <- FALSE
    } else {
      folderindex <- folderindex+1
    }
  }
  fs::dir_copy(file.path(VDYP7consolePath, "VDYP_CFG"),
               temppath)
  polyFile <- polyFile[order(FEATURE_ID),]
  layerFile <- layerFile[order(FEATURE_ID, LAYER_LEVEL_CODE),]

  write.table(polyFile,
              file = file.path(temppath, "polyfile.csv"),
              row.names = FALSE, sep = ",", na = "")
  write.table(layerFile,
              file = file.path(temppath, "layerfile.csv"),
              row.names = FALSE, sep = ",", na = "")
  totalNumberOfFeature <- length(unique(polyFile$FEATURE_ID))

  if(backward){backwardInd <- "Yes"} else {backwardInd <- "No"}
  if(forward){forwardInd <- "Yes"} else {forwardInd <- "No"}
  if(yTableIncProjMode){yTableIncProjModeInd <- "Yes"} else {yTableIncProjModeInd <- "No"}
  if(forceRefYear){yTableIncRefYearInd <- "Yes"} else {yTableIncRefYearInd <- "No"}
  if(forceCrtYear){yTableIncCrntYearInd <- "Yes"} else {yTableIncCrntYearInd <- "No"}
  if(yTableIncAge){yTableIncAgeInd <- "Yes"} else {yTableIncAgeInd <- "No"}
  if(yTableIncYear){yTableIncYearInd <- "Yes"} else {yTableIncYearInd <- "No"}
  if(allowBatphsub){allowBatphsubInd <- "Yes"} else {allowBatphsubInd <- "No"}
  if(yTableIncPolyID){yTableIncPolyIDInd <- "Yes"} else {yTableIncPolyIDInd <- "No"}
  if(projByLayer){projByLayerInd <- "Yes"} else {projByLayerInd <- "No"}
  if(projBySpecies){projBySpeciesInd <- "Yes"} else {projBySpeciesInd <- "No"}
  if(projVolume){projVolumeInd <- "Yes"} else {projVolumeInd <- "No"}
  if(secondarySpcHt){secondarySpcHt <- "Yes"} else {secondarySpcHt <- "No"}
  if(logFile | projBiomass){
    yieldtableoption <- "YieldTable"
    yieldtableformat <- "csv"
    if(projBiomass){
      projBiomassInd <- "Yes"
    } else {
      projBiomassInd <- "No"
    }
  } else {
    projBiomassInd <- "No"
    yieldtableoption <- "csvyieldtable"
    yieldtableformat <- "txt"
  }

  utilTable <- data.table::data.table(utilTable)
  utilTable[, text := paste0("-util ", SP0, "=", util, "\n")]
  if(!is.null(timeSeries)){
    if(timeSeries == "age"){
      timelines <- paste0("-agestart ", startTime, "\n",
                          " -ageend ", endTime, "\n",
                          " -inc ", timeIncrement, "\n",
                          " -includeagerows ", yTableIncAgeInd, "\n",
                          " -includeyearrows ", yTableIncYearInd, "\n",
                          " -forcerefyear ", yTableIncRefYearInd, "\n",
                          " -forcecrntyear ", yTableIncCrntYearInd, "\n")
    } else if(timeSeries == "year"){
      timelines <- paste0("-yearstart ", startTime, "\n",
                          " -yearend ", endTime, "\n",
                          " -inc ", timeIncrement, "\n",
                          " -includeyearrows ", yTableIncYearInd, "\n",
                          " -includeagerows ", yTableIncAgeInd, "\n",
                          " -forcerefyear ", yTableIncRefYearInd, "\n",
                          " -forcecrntyear ", yTableIncCrntYearInd, "\n")
    } else {
      stop("timeSeries must be specified using one of age and year.")
    }
  } else {
    timelines <- paste0(" -inc ", timeIncrement, "\n",
                        " -includeyearrows ", yTableIncYearInd, "\n",
                        " -includeagerows ", yTableIncAgeInd, "\n",
                        " -forcerefyear ", yTableIncRefYearInd, "\n",
                        " -forcecrntyear ", yTableIncCrntYearInd, "\n")
  }

  if(!is.null(forceYear)){
    forceyearline <- paste0("-forceyear ", forceYear, "\n")
  } else {
    forceyearline <- paste0("\n")
  }

  ## prepare the run configurations
  cat("  -ini ", paste0(gsub("/", "\\\\", VDYP7consolePath), "\\VDYP.ini \n"),
      "-ifmt hcsv \n",
      "-ofmt ", yieldtableoption, "\n",
      "-ip ", paste0(gsub("/", "\\\\", temppath), "\\polyfile.csv \n"),
      "-il ", paste0(gsub("/", "\\\\", temppath), "\\layerfile.csv \n"),
      "-o ", paste0(gsub("/", "\\\\", temppath), "\\yieldTable.", yieldtableformat,"\n"),
      "-e ", paste0(gsub("/", "\\\\", temppath), "\\errorReport.txt \n"),
      "-l ", paste0(gsub("/", "\\\\", temppath), "\\logReport.txt \n"),
      "-v7log ", paste0(gsub("/", "\\\\", temppath), "\\V7LogReport.txt \n"),
      "-c ", paste0(gsub("/", "\\\\", temppath), "\\VDYP_CFG\\ \n"),
      "-d ", paste0(gsub("/", "\\\\", temppath), "\\debug \n"),
      "-dbg No \n", # this is hard coded not to allow dbg
      "-v7save No \n", # hard coded not to allow v7save
      "-back ", backwardInd, "\n",
      "-forward ", forwardInd, "\n",
      "-includeprojmode ", yTableIncProjModeInd, "\n",
      timelines,
      forceyearline,
      "-secondarySpcsHt ", secondarySpcHt, "\n",
      "-allowbatphsub ", allowBatphsubInd, "\n",
      "-yieldtableincpolyid ", yTableIncPolyIDInd, "\n",
      "-projectedByLayer ", projByLayerInd, "\n",
      "-projectedBySpecies ", projBySpeciesInd, "\n",
      "-projectedVolumes ", projVolumeInd, "\n",
      "-projectedCFSBiomass ", projBiomassInd, "\n",
      utilTable$text,
      file = file.path(temppath, "configFile.txt"))

  ## prepare cmd file
  cat(paste0("\"", gsub("/", "\\\\", VDYP7consolePath), "\\vdyp7console\" "),
      "-p ", paste0(gsub("/", "\\\\", temppath), "\\configFile.txt \n"),
      file = file.path(temppath, "runSimulation.cmd"))
  ## run cmd in R
  orgwd <- getwd()
  setwd(temppath)
  shell(file.path(temppath, "runSimulation.cmd"),
        wait = TRUE, intern = TRUE)
  setwd(orgwd)
  yieldTable <- read.table(file.path(temppath,
                                     paste0("yieldTable.", yieldtableformat)),
                           sep = ",", header = TRUE,
                           stringsAsFactors = FALSE)
  if(projBiomass | logFile){
    yieldTable_all <- extractBYTable(rawYieldTable = yieldTable,
                                     volIncluded = projVolume,
                                     biomIncluded = projBiomass)
    yieldTable <- yieldTable_all$yieldTable
  }
  errorReport <- readLines(file.path(temppath, "errorReport.txt"),
                           warn = FALSE)
  logReport <- readLines(file.path(temppath, "logReport.txt"),
                         warn = FALSE)
  cmdFile <- readLines(file.path(temppath, "runSimulation.cmd"),
                       warn = FALSE)
  configFile <- readLines(file.path(temppath, "configFile.txt"),
                          warn = FALSE)
  configFile[grep(" -c ", configFile)] <- paste0(" -c ", paste0(gsub("/", "\\\\", VDYP7consolePath), "\\VDYP_CFG\\"), "\n")
  if(logFile){
    vdyp7log <- yieldTable_all$logfile
  } else {
    vdyp7log <- NULL
  }
  processNumberOfFeature <- logReport[grep("Polygons Processed:", logReport)]
  processNumberOfFeature <- as.numeric(unlist(strsplit(processNumberOfFeature, ":"))[2])
  processNumberOfFeature_line <- paste0(processNumberOfFeature,
                                        " (", round(100*processNumberOfFeature/totalNumberOfFeature, 1), "%)")
  success_line <- paste0(length(unique(yieldTable$FEATURE_ID)),
                         " (", round(100*length(unique(yieldTable$FEATURE_ID))/totalNumberOfFeature, 1), "%)")

  message("    Number of total polygons:                 ", totalNumberOfFeature, "\n",
          "    Number of processed polygons:             ", processNumberOfFeature_line, "\n",
          "    Number of successful simulated polygons:  ", success_line, "\n")
  unlink(temppath, recursive = TRUE)
  systemtime_end <- Sys.time()

  summarytext <- c(paste0(" Simulation started at ", systemtime_start),
                   paste0("Simulation ended at ", systemtime_end),
                   paste0("Number of total polygons:                 ",
                          totalNumberOfFeature),
                   paste0("Number of processed polygons:             ",
                          processNumberOfFeature_line),
                   paste0("Number of successful simulated polygons:  ",
                          success_line))

  warningMessage <- errorReport[grep("- W", errorReport)]
  if(length(warningMessage) > 0){
    featureid <- unlist(lapply(warningMessage, function(x){unlist(strsplit(x, "\\("))[2]}))
    featureid <- unlist(lapply(featureid, function(x){unlist(strsplit(x, "\\)"))[1]}))
    featureid <- gsub("Rcrd ID: ", "", featureid)
    featureid <- trimws(featureid)
    layerid <- unlist(lapply(warningMessage, function(x){unlist(strsplit(x, "\\)"))[2]}))
    layerid <- unlist(lapply(layerid, function(x){unlist(strsplit(x, "- W"))[1]}))
    layerid <- trimws(layerid)
    warningMessage <- unlist(lapply(warningMessage, function(x){unlist(strsplit(x, "- W"))[2]}))
    warningMessage <- trimws(warningMessage, which = "left")
    warningTable <- data.table(FEATURE_ID = featureid,
                               LAYER_ID = layerid,
                               MESSAGE = warningMessage)
    rm(warningMessage, featureid, layerid)
    warningTable <- merge(warningTable,
                          polyFile[,.(FEATURE_ID = as.character(FEATURE_ID), MAP_ID, POLYGON_NUMBER)],
                          by = "FEATURE_ID",
                          all.x = TRUE)
    warningTable <- warningTable[,.(FEATURE_ID, MAP_ID, POLYGON_NUMBER, LAYER_ID, MESSAGE)]
  } else {
    warningTable <- data.table(FEATURE_ID = character(),
                               MAP_ID = character(),
                               POLYGON_NUMBER = character(),
                               LAYER_ID = character(),
                               MESSAGE = character())
  }
  errorMessage <- errorReport[grep(" - E ", errorReport) | grep(" - F ", errorReport)]
  errorMessage <- errorReport[grep(" - E ", errorReport)]
  if(length(errorMessage) > 0){
    featureid <- unlist(lapply(errorMessage, function(x){unlist(strsplit(x, "\\("))[2]}))
    featureid <- unlist(lapply(featureid, function(x){unlist(strsplit(x, "\\)"))[1]}))
    featureid <- gsub("Rcrd ID: ", "", featureid)
    featureid <- trimws(featureid)
    layerid <- unlist(lapply(errorMessage, function(x){unlist(strsplit(x, "\\)"))[2]}))
    layerid <- unlist(lapply(layerid, function(x){unlist(strsplit(x, "- E"))[1]}))
    layerid <- trimws(layerid)
    errorMessage <- unlist(lapply(errorMessage, function(x){unlist(strsplit(x, "- E"))[2]}))
    errorMessage <- trimws(errorMessage, which = "left")
    errorTable <- data.table(FEATURE_ID = featureid,
                             LAYER_ID = layerid,
                             MESSAGE = errorMessage)
    rm(errorMessage, featureid, layerid)
    errorTable <- merge(errorTable,
                        polyFile[,.(FEATURE_ID = as.character(FEATURE_ID), MAP_ID, POLYGON_NUMBER)],
                        by = "FEATURE_ID",
                        all.x = TRUE)
    errorTable <- errorTable[,.(FEATURE_ID, MAP_ID, POLYGON_NUMBER, LAYER_ID, MESSAGE)]
  } else {
    errorTable <- data.table(FEATURE_ID = character(),
                             MAP_ID = character(),
                             POLYGON_NUMBER = character(),
                             LAYER_ID = character(),
                             MESSAGE = character())
  }
  errorMessage_f <- errorReport[grep(" - F ", errorReport)]
  if(length(errorMessage_f) > 0){
    featureid <- unlist(lapply(errorMessage_f, function(x){unlist(strsplit(x, "\\("))[2]}))
    featureid <- unlist(lapply(featureid, function(x){unlist(strsplit(x, "\\)"))[1]}))
    featureid <- gsub("Rcrd ID: ", "", featureid)
    featureid <- trimws(featureid)
    layerid <- ""
    errorMessage_f <- unlist(lapply(errorMessage_f, function(x){unlist(strsplit(x, "- F"))[2]}))
    errorMessage_f <- trimws(errorMessage_f, which = "left")
    errorTable_f <- data.table(FEATURE_ID = featureid,
                             LAYER_ID = layerid,
                             MESSAGE = errorMessage_f)
    rm(errorMessage_f, featureid, layerid)
    errorTable_f <- merge(errorTable_f,
                        polyFile[,.(FEATURE_ID = as.character(FEATURE_ID), MAP_ID, POLYGON_NUMBER)],
                        by = "FEATURE_ID",
                        all.x = TRUE)
    errorTable_f <- errorTable_f[,.(FEATURE_ID, MAP_ID, POLYGON_NUMBER, LAYER_ID, MESSAGE)]
  } else {
    errorTable_f <- data.table(FEATURE_ID = character(),
                             MAP_ID = character(),
                             POLYGON_NUMBER = character(),
                             LAYER_ID = character(),
                             MESSAGE = character())
  }
  errorTable <- rbind(errorTable, errorTable_f)
  return(list("summary" = summarytext,
              "yieldTable" = data.table(yieldTable),
              "warningTable" = warningTable,
              "errorTable" = errorTable,
              "errorReport" = errorReport,
              "logReport" = logReport,
              "vdyp7Log" = vdyp7log,
              "metadata" = list("method" = "nonParallel",
                                "polyFile" = polyFile,
                                "layerFile" = layerFile,
                                "tempDir" = temppath,
                                "runSimulation" = cmdFile,
                                "configFile" = configFile)))
}
