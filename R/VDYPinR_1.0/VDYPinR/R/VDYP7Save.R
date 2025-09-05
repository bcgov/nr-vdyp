#' The function is to examine whether the data is valid for VDYP7 run
#'
#' @description The VDYP7 takes two input files poly file and layer file. This function is to
#'              examine whether those two input files are valid for VDYP7 run, in terms of
#'              1) data structure and 2) data matchness
#'
#' @param simuOutput list, The output from \code{\link{VDYP7Run}}.
#'
#' @param savePath character, Specifies the path to save outputs.
#'
#' @param saveName character, Specifies the name of saved outputs.
#'                 This is prefix for saved files. This arguement has to be specified.
#'
#' @param saveFmt character, Specifies the format you want to save the yield table. Currently, it
#'                supports \code{rds}, \code{txt}, \code{csv} and \code{xlsx}. Default is \code{txt}.
#'                Please note that the rest of the outputs are saved as \code{txt} file.
#' @param overWrite logical, Specifies whether the user wish to overwrite the existing file
#'                  in the save path. Default is \code{FALSE}.
#' @param metadata logical, Specifies whether the user wish to save metadata.
#'                 Default is \code{FALSE}.
#'
#' @importFrom openxlsx write.xlsx
#' @return no value
#'
#' @export
#' @docType methods
#' @rdname VDYP7Save
#'
#' @author Yong Luo
VDYP7Save <- function(simuOutput, savePath, saveName,
                      saveFmt = "txt", overWrite = FALSE,
                      metadata = FALSE){
  yieldtable_exists <- file.exists(file.path(savePath, paste0(saveName, "_yieldtable.", saveFmt)))
  error_exists <- file.exists(file.path(savePath, paste0(saveName, "_errorReport.txt")))
  log_exists <- file.exists(file.path(savePath, paste0(saveName, "_logReport.txt")))
  errormes <- c()
  if(yieldtable_exists & !overWrite){
    errormes <- c(errormes, paste0("File ", paste0(saveName, "_yldTable_volume.", saveFmt), " exists in the savePath."))
  }
  if(error_exists & !overWrite){
    errormes <- c(errormes, paste0("File ", paste0(saveName, "_errorReport.txt"), " exists in savePath."))
  }
  if(log_exists & !overWrite){
    errormes <- c(errormes, paste0("File ", paste0(saveName, "_logReport.txt"), " exists in savePath."))
  }

  if((yieldtable_exists | error_exists | log_exists) & !overWrite){
    stop("\n", paste0("    ", errormes, collapse = "\n"),
         "\n    Rename saveName or set overWrite as TRUE")
  }

  if(saveFmt == "rds"){
    saveRDS(simuOutput$yieldTable, file.path(savePath, paste0(saveName, "_yieldtable.rds")))
    saveRDS(simuOutput$warningTable, file.path(savePath, paste0(saveName, "_warningTable.rds")))
    saveRDS(simuOutput$errorTable, file.path(savePath, paste0(saveName, "_errorTable.rds")))
  } else if (saveFmt %in% c("txt", "csv")){
    write.table(simuOutput$yieldTable,
                file.path(savePath, paste0(saveName, "_yieldtable.", saveFmt)),
                sep = ",", row.names = FALSE)
    write.table(simuOutput$warningTable,
                file.path(savePath, paste0(saveName, "_warningTable.", saveFmt)),
                sep = ",", row.names = FALSE)
    write.table(simuOutput$errorTable,
                file.path(savePath, paste0(saveName, "_errorTable.", saveFmt)),
                sep = ",", row.names = FALSE)

  } else if (saveFmt == "xlsx"){
    openxlsx::write.xlsx(simuOutput$yieldTable,
                         file.path(savePath, paste0(saveName, "_yieldtable.xlsx")),
                         rowNames = FALSE)
    openxlsx::write.xlsx(simuOutput$warningTable,
                         file.path(savePath, paste0(saveName, "_warningTable.xlsx")),
                         rowNames = FALSE)
    openxlsx::write.xlsx(simuOutput$errorTable,
                         file.path(savePath, paste0(saveName, "_errorTable.xlsx")),
                         rowNames = FALSE)
  } else {
    stop("saveFmt is not correctly specified. It must be one of rds, txt, csv and xlsx.")
  }
  summary_text <- c(simuOutput$summary, "\n",
                    paste0("yield table was saved to ", paste0(saveName, "_yieldtable")),
                    paste0("warning messages were saved to ", paste0(saveName, "_warningTable")),
                    paste0("error messages were saved to ", paste0(saveName, "_errorTable")),
                    paste0("all raw messages were saved to ", paste0(saveName, "_errorReport.txt")),
                    paste0("all raw log was saved to ", paste0(saveName, "_logReport.txt")),
                    paste0("all raw log in core module was saved to ", paste0(saveName, "_VDYP.log")))
  if(metadata){
    summary_text <- c(summary_text, "\n",
                      paste0("cmd file was saved to ", paste0(saveName, "_runSimulation.cmd")),
                      paste0("VDYP7 console configure file was saved to ", paste0(saveName, "_configFile.txt")),
                      "input poly file was saved to polyfile.csv",
                      "input layer file was saved to layerfile.csv")
  }

  cat(paste0(summary_text, "\n"),
      file = file.path(savePath, paste0(saveName, "_summary.txt")))


  temppath <- simuOutput$metadata$tempDir
  temppath_rep <- gsub("\\\\", "\\\\\\\\", temppath)
  errorReport <- simuOutput$errorReport
  errorReport <- gsub(temppath_rep, gsub("/", "\\\\\\\\", savePath), errorReport)
  cat(paste0(errorReport, "\n"),
      file = file.path(savePath, paste0(saveName, "_errorReport.txt")))

  logReport <- simuOutput$logReport
  logReport <- gsub(temppath_rep, gsub("/", "\\\\\\\\", savePath), logReport)
  cat(paste0(logReport, "\n"),
      file = file.path(savePath, paste0(saveName, "_logReport.txt")))

  vdyp7Log <- simuOutput$vdyp7Log
  vdyp7Log <- gsub(temppath_rep, gsub("/", "\\\\\\\\", savePath),
                   vdyp7Log)
  cat(paste0(vdyp7Log, "\n"),
      file = file.path(savePath, paste0(saveName, "_VDYP.log")))
  if(metadata){
    temppath_rep <- gsub("/", "\\\\\\\\", temppath_rep)
    cmdfile <- simuOutput$metadata$runSimulation
    savePath_tmp <- gsub("\\\\", "\\\\\\\\", savePath)
    cmdfile <- gsub(temppath_rep, gsub("/", "\\\\\\\\", savePath_tmp), cmdfile)
    cmdfile <- gsub("configFile", paste0(saveName, "_configFile"), cmdfile)
    cat(paste0(cmdfile, "\n"),
        file = file.path(savePath, paste0(saveName, "_runSimulation.cmd")))
    configurationFile <- simuOutput$metadata$configFile
    configurationFile <- gsub(temppath_rep, gsub("/", "\\\\\\\\", savePath_tmp),
                              configurationFile)
    cat(paste0(configurationFile, "\n"),
        file = file.path(savePath, paste0(saveName, "_configFile.txt")))

      write.table(simuOutput$metadata$polyFile,
                  file = file.path(savePath, "polyfile.csv"),
                  row.names = FALSE, sep = ",", na = "")
      write.table(simuOutput$metadata$layerFile,
                  file = file.path(savePath, "layerfile.csv"),
                  row.names = FALSE, sep = ",", na = "")
  }
}


