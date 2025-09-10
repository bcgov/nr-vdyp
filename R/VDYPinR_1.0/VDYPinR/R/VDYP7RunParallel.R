#' The function is to run VDYP7 in R environment using parallel
#'
#' @description This function is to prepare two input files (\code{polyFile} and \code{layerFile});
#'              to prepare VDYP configuration and cmd file; to run VDYP7; and load VDYP7 output into
#'              R environment.
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
#' #'
#' @return a list that contains
#'         1) simulated stand yield table (equivalent to \code{-o} in VDYP7 console);
#'         2) processing message (equivalent to \code{-e} in VDYP7 console);
#'         3) log message (equivalent to \code{-l} in VDYP7 console);
#'         4) may contain log message from core module (equivalent to \code{-v7log} in VDYP7 console);
#'         5) metadata to run the simulation (cmd file and console specifications)
#'
#' @importFrom data.table data.table ':=' setnames
#' @importFrom fs dir_copy
#' @importFrom parallel detectCores makeCluster clusterExport parLapply stopCluster
#'
#' @export
#' @docType methods
#' @rdname VDYP7RunParallel
#' @seealso see \code{\link{VDYP7Save}} to save results
#' @author Yong Luo
VDYP7RunParallel <- function(polyFile,
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

  polyFile <- data.table(polyFile)
  layerFile <- data.table(layerFile)
  allfeatureids <- unique(polyFile$FEATURE_ID)

  numCore <- detectCores()-1
  if(length(allfeatureids) < numCore){
    output <- VDYP7Run(polyFile = polyFile,
                       layerFile = layerFile,
                       utilTable = utilTable,
                       VDYP7consolePath = VDYP7consolePath,
                       timeSeries = timeSeries,
                       startTime = startTime,
                       endTime = endTime,
                       timeIncrement = timeIncrement,
                       backward = backward,
                       forward = forward,
                       allowBatphsub = allowBatphsub,
                       yTableIncProjMode = yTableIncProjMode,
                       yTableIncAge = yTableIncAge,
                       yTableIncYear = yTableIncYear,
                       yTableIncPolyID = yTableIncPolyID,
                       forceYear = forceYear,
                       forceRefYear = forceRefYear,
                       forceCrtYear = forceCrtYear,
                       secondarySpcHt = secondarySpcHt,
                       projByLayer = projByLayer,
                       projBySpecies = projBySpecies,
                       projVolume = projVolume,
                       projBiomass = projBiomass,
                       logFile = logFile)

  } else {
    numberofrow <- as.integer(length(allfeatureids)/numCore)
    alldata_list <- list()
    for(i in 1:numCore){
      if(i != numCore){
        indifeatureid <- allfeatureids[((i-1)*numberofrow+1):(i*numberofrow)]
      } else {
        indifeatureid <- allfeatureids[((i-1)*numberofrow+1):length(allfeatureids)]
      }
      alldata_list[[i]] <- list("polyFile" = polyFile[FEATURE_ID %in% indifeatureid,],
                                "layerFile" = layerFile[FEATURE_ID %in% indifeatureid,],
                                "utilTable" = utilTable,
                                "VDYP7consolePath" = VDYP7consolePath,
                                "timeSeries" = timeSeries,
                                "startTime" = startTime,
                                "endTime" = endTime,
                                "timeIncrement" = timeIncrement,
                                "backward" = backward,
                                "forward" = forward,
                                "allowBatphsub" = allowBatphsub,
                                "yTableIncProjMode" = yTableIncProjMode,
                                "yTableIncAge" = yTableIncAge,
                                "yTableIncYear" = yTableIncYear,
                                "yTableIncPolyID" = yTableIncPolyID,
                                "forceYear" = forceYear,
                                "forceRefYear" = forceRefYear,
                                "forceCrtYear" = forceCrtYear,
                                "secondarySpcHt" = secondarySpcHt,
                                "projByLayer" = projByLayer,
                                "projBySpecies" = projBySpecies,
                                "projVolume" = projVolume,
                                "projBiomass" = projBiomass,
                                "logFile" = logFile)
    }
    rm(i, indifeatureid)

    clusterInFunction <- makeCluster(numCore)

    clusterExport(clusterInFunction,
                  varlist = c("VDYP7Run", "data.table", "setnames", "dir_copy"),
                  envir = environment())
    allresults <- parLapply(cl = clusterInFunction,
                            alldata_list,
                            function(x){
                              VDYP7Run(polyFile = x$polyFile,
                                       layerFile = x$layerFile,
                                       utilTable = x$utilTable,
                                       VDYP7consolePath = x$VDYP7consolePath,
                                       timeSeries = x$timeSeries,
                                       startTime = x$startTime,
                                       endTime = x$endTime,
                                       timeIncrement = x$timeIncrement,
                                       backward = x$backward,
                                       forward = x$forward,
                                       allowBatphsub = x$allowBatphsub,
                                       yTableIncProjMode = x$yTableIncProjMode,
                                       yTableIncAge = x$yTableIncAge,
                                       yTableIncYear = x$yTableIncYear,
                                       yTableIncPolyID = x$yTableIncPolyID,
                                       forceYear = x$forceYear,
                                       forceRefYear = x$forceRefYear,
                                       forceCrtYear = x$forceCrtYear,
                                       secondarySpcHt = x$secondarySpcHt,
                                       projByLayer = x$projByLayer,
                                       projBySpecies = x$projBySpecies,
                                       projVolume = x$projVolume,
                                       projBiomass = x$projBiomass,
                                       logFile = x$logFile)})

    stopCluster(clusterInFunction)
    for(i in 1:length(allresults)){
      if(i == 1){
        allyieldtable <- allresults[[i]]$yieldTable
        allwarningtable <- allresults[[i]]$warningTable
        allerrortable <- allresults[[i]]$errorTable
        allsummary <- c(paste0("***********************node ", i, ": ***********************"),
                        trimws(allresults[[i]]$summary))
        allerrorrep <- c(paste0("***********************node ", i, ": ***********************"),
                         allresults[[i]]$errorReport)
        alllogrep <- c(paste0("***********************node ", i, ": ***********************"),
                       allresults[[i]]$logReport)
        alltemppath <- allresults[[i]]$metadata$tempDir
        allconfig <- allresults[[i]]$metadata$configFile
        allcmd <- allresults[[i]]$metadata$runSimulation
        allvdyp7log <- c(paste0("***********************node ", i, ": ***********************"),
                         allresults[[i]]$vdyp7Log)

      } else {
        allyieldtable <- rbind(allyieldtable, allresults[[i]]$yieldTable)
        allwarningtable <- rbind(allwarningtable, allresults[[i]]$warningTable)
        allerrortable <- rbind(allerrortable, allresults[[i]]$errorTable)
        allsummary <- c(allsummary,
                        paste0("***********************node ", i, ": ***********************"),
                        trimws(allresults[[i]]$summary))
        allerrorrep <- c(allerrorrep,
                         paste0("***********************node ", i, ": ***********************"),
                         allresults[[i]]$errorReport)
        alllogrep <- c(alllogrep,
                       paste0("***********************node ", i, ": ***********************"),
                       allresults[[i]]$logReport)
        allvdyp7log <- c(allvdyp7log,
                         paste0("***********************node ", i, ": ***********************"),
                         allresults[[i]]$vdyp7Log)
      }
    }
    rm(allresults)
    processNumberOfFeature <- alllogrep[grep("Polygons Processed:", alllogrep)]

    processNumberOfFeature <- sum(as.numeric(lapply(processNumberOfFeature,
                                                    function(s){unlist(strsplit(s, ":"))[2]})))
    processNumberOfFeature_line <- paste0(processNumberOfFeature,
                                          " (", round(100*processNumberOfFeature/length(allfeatureids), 1), "%)")
    success_line <- paste0(length(unique(allyieldtable$FEATURE_ID)),
                           " (", round(100*length(unique(allyieldtable$FEATURE_ID))/length(allfeatureids), 1), "%)")

    message("    Number of total polygons:                 ", length(allfeatureids), "\n",
            "    Number of processed polygons:             ", processNumberOfFeature_line, "\n",
            "    Number of successful simulated polygons:  ", success_line, "\n")
    allsummary <- c("***********************overall summary**********************",
                    paste0("Number of total polygons:                 ", length(allfeatureids)),
                    paste0("Number of processed polygons:             ", processNumberOfFeature_line),
                    paste0("Number of successful simulated polygons:  ", success_line),
                    allsummary)
    output <- list()
    output[["summary"]] <- allsummary
    output[["yieldTable"]] <- allyieldtable
    output[["warningTable"]] <- allwarningtable
    output[["errorTable"]] <- allerrortable
    output[["errorReport"]] <- allerrorrep
    output[["logReport"]] <- alllogrep
    output[["vdyp7Log"]] <- allvdyp7log
    output[["metadata"]] <- list("method" = "Parallel",
                                 "tempDir" = alltemppath,
                                 "runSimulation" = allcmd,
                                 "configFile" = allconfig,
                                 "polyFile" = polyFile,
                                 "layerFile" = layerFile)
    rm(allyieldtable, allerrorrep, alllogrep, allconfig, allcmd)
  }
  return(output)
}
