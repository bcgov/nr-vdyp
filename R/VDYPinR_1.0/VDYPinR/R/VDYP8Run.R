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
#' @param BaseRESTAPIURL character, Specifies where your you are accessing VDYP8 REST API.
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
#' @importFrom jsonlite toJSON
#' @importFrom httr2 request
#' @importFrom curl form_data
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
#' @rdname VDYP8Run
#' @seealso see \code{\link{VDYP7Save}} to save results; \code{\link{VDYP7RunParallel}} to run VDYP7 parallel.
#'          see \code{\link{VDYP7Debug}} to turn on debug mode.
#' @author Peter Minter
VDYP8Run <- function(polyFile,
                     layerFile,
                     utilTable,
                     BaseRESTAPIURL = "http://localhost:8080/api/v8/",
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
  systemtime_start <- Sys.time()
  ## prepare a temp folder to save input data
  ## temppath1 <- tempdir()
  temppath1 <- "C:/VDYP7inR/examples/tempfolder"
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
  polyFile <- polyFile[order(FEATURE_ID),]
  layerFile <- layerFile[order(FEATURE_ID, LAYER_LEVEL_CODE),]
  
  polyFileLocation <- file.path(temppath, "polyfile.csv")
  layerFileLocation <- file.path(temppath, "layerfile.csv")
  
  write.table(polyFile,
              file = polyFileLocation,
              row.names = FALSE, sep = ",", na = "")
  write.table(layerFile,
              file = layerFileLocation,
              row.names = FALSE, sep = ",", na = "")
  totalNumberOfFeature <- length(unique(polyFile$FEATURE_ID))
  
  #Build JSON
  jsonArr <- list("outputFormat"="CSVYieldTable")
  
  #Execution options
  flags = c(backward, 
            forward, 
            forceRefYear, 
            forceCrtYear,
            allowBatphsub,
            yTableIncPolyID,
            yTableIncProjMode,
            projByLayer,
            projBySpecies,
            projVolume,
            secondarySpcHt,
            projBiomass,
            TRUE,
            TRUE,
            TRUE,
            FALSE,
            FALSE
            )
  executionOptions = c("backGrowEnabled", 
                       "forwardGrowEnabled", 
                       "doForceReferenceYearInclusionInYieldTables", 
                       "doForceCurrentYearInclusionInYieldTables",
                       "doAllowBasalAreaAndTreesPerHectareValueSubstitution",
                       "doIncludePolygonRecordIdInYieldTable",
                       "doIncludeProjectionModeInYieldTable",
                       "doSummarizeProjectionByLayer",
                       "doIncludeSpeciesProjection",
                       "doIncludeProjectedMOFVolumes",
                       "doIncludeSecondarySpeciesDominantHeightInYieldTable",
                       "doIncludeProjectedCFSBiomass",
                       "doEnableProgressLogging",
                       "doEnableErrorLogging",
                       "doEnableDebugLogging",
                       "doSaveIntermediateFiles",
                       "doIncludeProjectionFiles"
                       )
  jsonArr[["selectedExecutionOptions"]] <- executionOptions[flags]
  jsonArr[["excludedExecutionOptions"]] <- executionOptions[!flags]
  
  #These two parameters appear unsupported in VDYP8 at this time.
  #if(yTableIncAge){yTableIncAgeInd <- "Yes"} else {yTableIncAgeInd <- "No"}
  #if(yTableIncYear){yTableIncYearInd <- "Yes"} else {yTableIncYearInd <- "No"}
  # Need to add support later if necessary
  
  # TODO Some special cases about the yield table.... leaving for now, request Text Yield Table in the certain conditions
  #if(logFile | projBiomass){
  #  yieldtableoption <- "YieldTable"
  #  yieldtableformat <- "csv"
  #} else {
    yieldtableoption <- "csvyieldtable"
    yieldtableformat <- "csv"
  #}
  
  #Utilization Level logic
  utilTable[, .(
    speciesName = SP0,
    utilizationClass = paste0(as.character(util), "+")
  )] -> utilTable_mapped
  jsonArr[["utils"]] <- utilTable_mapped
  
  if(!is.null(timeSeries)){
    timeStart <- paste0(timeSeries,"Start");
    timeEnd <- paste0(timeSeries,"End");
  }
  jsonArr[[timeStart]] <- startTime
  jsonArr[[timeEnd]] <- endTime
  jsonArr[["ageIncrement"]] <- timeIncrement

  if(!is.null(forceYear)){
    jsonArr[["forceYear"]] <- forceYear
  }

  jsonArr[["selectedDebugOptions"]] = as.list("doIncludeDebugTimestamps","doIncludeDebugEntryExit","doIncludeDebugIndentBlocks","doIncludeDebugRoutineNames")
  
  json  <- jsonlite::toJSON(jsonArr, pretty = TRUE, auto_unbox = TRUE)
  #json <- "{\"ageStart\":0,\"ageEnd\":250,\"ageIncrement\":25,\"outputFormat\":\"CSVYieldTable\",\"reportTitle\":\"A Test Report to determine if a very long title will wrap and both lines will be centered\",\"selectedExecutionOptions\":[\"forwardGrowEnabled\",\"doForceReferenceYearInclusionInYieldTables\",\"doSummarizeProjectionByLayer\",\"doIncludeFileHeader\",\"doIncludeProjectionFiles\",\"doIncludeProjectionModeInYieldTable\",\"doIncludeProjectionFiles\",\"doIncludeAgeRowsInYieldTable\",\"doIncludeYearRowsInYieldTable\",\"doSummarizeProjectionByPolygon\",\"doIncludeColumnHeadersInYieldTable\",\"doAllowBasalAreaAndTreesPerHectareValueSubstitution\",\"doEnableProgressLogging\",\"doEnableErrorLogging\",\"doEnableDebugLogging\",\"doIncludeProjectedCFSBiomass\"],\"selectedDebugOptions\":[\"doIncludeDebugTimestamps\",\"doIncludeDebugEntryExit\",\"doIncludeDebugIndentBlocks\",\"doIncludeDebugRoutineNames\"],\"combineAgeYearRange\":\"intersect\",\"metadataToOutput\":\"VERSION\"}"
  #cat(json)
  
  endpoint <- paste0(BaseRESTAPIURL, "projection/hcsv")
  # Build and send request
  resp <- httr2::request(endpoint) |>
    httr2::req_url_query(trialRun = "false") |> 
    httr2::req_timeout(seconds = 120) |>
    httr2::req_body_multipart(
      # JSON part
      `Projection Parameters` = curl::form_data(
        json,
        type = "application/json"
      ),
      # Two CSV files
      `HCSV-Polygon` = curl::form_file(polyFileLocation, type = "text/csv"),
      `HCSV-Layers`  = curl::form_file(layerFileLocation,  type = "text/csv")
    ) |>
    httr2::req_perform()
  
  httr2::resp_check_status(resp)
  # 2. Write raw ZIP body to a file in tempdir()
  zip_path <- file.path(temppath, "vdyp8results.zip")
  writeBin(httr2::resp_body_raw(resp), zip_path)
  
  # 3. Unzip into a dedicated subfolder
  unzip(zip_path, exdir = temppath)
  
  # 4. Locate YieldTable.csv
  yield_path <- file.path(temppath, "YieldTable.csv")
  stopifnot(file.exists(yield_path))
  
  yieldTable <- read.table(file.path(temppath,
                                     paste0("YieldTable.", yieldtableformat)),
                           sep = ",", header = TRUE,
                           stringsAsFactors = FALSE)
  
  #TODO this is problematic... We may have to implement a different output type to support it. IF we were to implement the Biomass AND Volume instead that may be perferable
  #if(projBiomass | logFile){
  #  yieldTable_all <- extractBYTable(rawYieldTable = yieldTable,
  #                                   volIncluded = projVolume,
  #                                   biomIncluded = projBiomass)
  #  yieldTable <- yieldTable_all$yieldTable
  #}
  
  
  errorReport <- readLines(file.path(temppath, "ErrorLog.txt"),
                           warn = FALSE)
  logReport <- readLines(file.path(temppath, "ProgressLog.txt"),
                         warn = FALSE)
  #if(logFile){
  #  vdyp7log <- yieldTable_all$logfile
  #} else {
    vdyp7log <- NULL
  #}
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
                                "tempDir" = temppath)))
}
