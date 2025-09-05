#' The function is to run VDYP7 in R environment with debug model turned on.
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
#' @param debugPath character, Specifies path to store debug files.
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
#' @rdname VDYP7Debug
#' @seealso see \code{\link{VDYP7Save}} to save results; \code{\link{VDYP7RunParallel}} to run VDYP7 parallel.
#'          see \code{\link{VDYP7Run}} to turn off debug mode.
#' @author Yong Luo
VDYP7Debug <- function(polyFile,
                       layerFile,
                       utilTable,
                       VDYP7consolePath = "C:/VDYP7",
                       debugPath,
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

  if(dir.exists(debugPath)){
    if(length(dir(debugPath)) > 0){
      want <- readline("The debug path contains file(s), do you want to remove (Y/N): ")
      if(toupper(want) == "Y"){
        unlink(debugPath, recursive = TRUE)
      } else {
        stop("Please specify a new debug path.")
      }
    }
  } else {
    dir.create(debugPath)
  }
  fs::dir_copy(file.path(VDYP7consolePath, "VDYP_CFG"),
               debugPath)
  polyFile <- polyFile[order(FEATURE_ID),]
  layerFile <- layerFile[order(FEATURE_ID, LAYER_LEVEL_CODE),]

  write.table(polyFile,
              file = file.path(debugPath, "polyfile.csv"),
              row.names = FALSE, sep = ",", na = "")
  write.table(layerFile,
              file = file.path(debugPath, "layerfile.csv"),
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
      "-ip ", paste0(gsub("/", "\\\\", debugPath), "\\polyfile.csv \n"),
      "-il ", paste0(gsub("/", "\\\\", debugPath), "\\layerfile.csv \n"),
      "-o ", paste0(gsub("/", "\\\\", debugPath), "\\yieldTable.", yieldtableformat,"\n"),
      "-e ", paste0(gsub("/", "\\\\", debugPath), "\\errorReport.txt \n"),
      "-l ", paste0(gsub("/", "\\\\", debugPath), "\\logReport.txt \n"),
      "-v7log ", paste0(gsub("/", "\\\\", debugPath), "\\V7LogReport.txt \n"),
      "-c ", paste0(gsub("/", "\\\\", debugPath), "\\VDYP_CFG\\ \n"),
      "-d ", paste0(gsub("/", "\\\\", debugPath), "\\debug \n"),
      "-dbg Yes \n", # this is hard coded to allow dbg
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
      file = file.path(debugPath, "configFile.txt"))

  ## prepare cmd file
  cat(paste0("\"", gsub("/", "\\\\", VDYP7consolePath), "\\vdyp7console\" "),
      "-p ", paste0(gsub("/", "\\\\", debugPath), "\\configFile.txt \n"),
      file = file.path(debugPath, "runSimulation.cmd"))
  ## run cmd in R
  orgwd <- getwd()
  setwd(debugPath)
  shell(file.path(debugPath, "runSimulation.cmd"),
        wait = TRUE, intern = TRUE)
  setwd(orgwd)
}
