#' The function is to prepare the util input table for runing VDYP7
#'
#' @description The function is to prepare the util input table for runing VDYP7. This function is
#'              to allow DBH setting for each of the 16 possible species group (SP0).
#'              The DBH util level can be supplied from 4, 7.5, 12.5, 17.5 and 22.5.
#'
#' @param all numeric, Assign all the 16 species group with this DBH util level.
#'
#' @param exception character, Modify the util level by species using this argument. This argument
#'                  is formed as \code{SP0,util}, for example, \code{AC,4}.
#' @param biomass logical, Generate biomass unil table, default is \code{FALSE}. When it is TRUE,
#'                         the function ignores inputs from \code{all} and \code{exception} arguments,
#'                         as VDYP7 only uses this util when simulate biomass.
#'
#' @param askForConfirm logical, Print the output to confirm the util table, default is TRUE.
#'
#' @importFrom data.table data.table ':='
#' @return util table for 16 species group
#' @examples
#' \dontrun{
#'   utiltable1 <- utilTableGenerator(all = 12.5, askForConfirm = FALSE)
#'   print(utiltable1)
#'       SP0 util
#'    1:  AC 12.5
#'    2:  AT 12.5
#'    3:   B 12.5
#'    4:   C 12.5
#'    5:   D 12.5
#'    6:   E 12.5
#'    7:   F 12.5
#'    8:   H 12.5
#'    9:   L 12.5
#'   10:  MB 12.5
#'   11:  PA 12.5
#'   12:  PL 12.5
#'   13:  PW 12.5
#'   14:  PY 12.5
#'   15:   S 12.5
#'   16:   Y 12.5
#'
#'   utiltable2 <- utilTableGenerator(all = 12.5,
#'                           exception = c("AC,4", "H, 17.5", "Y, 22.5"),
#'                           askForConfirm = FALSE)
#'   print(utiltable2)
#'       SP0 util
#'    1:  AC  4.0
#'    2:  AT 12.5
#'    3:   B 12.5
#'    4:   C 12.5
#'    5:   D 12.5
#'    6:   E 12.5
#'    7:   F 12.5
#'    8:   H 17.5
#'    9:   L 12.5
#'   10:  MB 12.5
#'   11:  PA 12.5
#'   12:  PL 12.5
#'   13:  PW 12.5
#'   14:  PY 12.5
#'   15:   S 12.5
#'   16:   Y 22.5
#'
#'   utiltable3 <- utilTableGenerator(all = 12.5,
#'                           exception = c("AC,4", "H, 17.5", "Y, 22.5"),
#'                           biomass = TRUE,
#'                           askForConfirm = FALSE)
#'    print()
#'            SP0 util
#'         1:  AC 12.5
#'         2:  AT 12.5
#'         3:   B 17.5
#'         4:   C 17.5
#'         5:   D 12.5
#'         6:   E 12.5
#'         7:   F 17.5
#'         8:   H 17.5
#'         9:   L 12.5
#'        10:  MB 12.5
#'        11:  PA 12.5
#'        12:  PL 12.5
#'        13:  PW 12.5
#'        14:  PY 12.5
#'        15:   S 17.5
#'        16:   Y 17.5
#'
#' }
#'
#' @export
#' @docType methods
#' @rdname utilTableGenerator
#'
#' @author Yong Luo
utilTableGenerator <- function(all,
                               exception = NULL,
                               biomass = FALSE,
                               askForConfirm = TRUE){
  SP0_ref <- c("AC", "AT", "B", "C", "D", "E", "F", "H", "L",
               "MB", "PA", "PL", "PW", "PY", "S", "Y")
  util_ref <- c(4, 7.5, 12.5, 17.5, 22.5)
  if(biomass){
    output <- data.table(SP0 = c("AC", "AT", "B", "C", "D",
                                 "E", "F", "H", "L", "MB",
                                 "PA", "PL", "PW",
                                 "PY", "S", "Y"),
                         util = c(12.5, 12.5, 17.5, 17.5, 12.5,
                                  12.5, 17.5, 17.5, 12.5, 12.5,
                                  12.5, 12.5, 12.5,
                                  12.5, 17.5, 17.5))
    #' when the projBiomass = T, the unitilization level will be overwritten. And the volume and biomass
    #' summaries will be derived using the new unitilization level.
    #'            AC   --> 12.5
    #'            AT   --> 12.5
    #'            B    --> 17.5
    #'            C    --> 17.5
    #'            D    --> 12.5
    #'
    #'            E    --> 12.5
    #'            F    --> 17.5
    #'            H    --> 17.5
    #'            L    --> 12.5
    #'            MB   --> 12.5
    #'
    #'            PA   --> 12.5
    #'            PL   --> 12.5
    #'            PW   --> 12.5
    #'
    #'            PY   --> 12.5
    #'            S    --> 17.5
    #'            Y    --> 17.5
  } else {
    output <- data.table(SP0 = SP0_ref,
                         util = all)
    if(!is.null(exception)){
      exceptiontable <- data.table(exception)
      exceptiontable[, ':='(SP0 = lapply(strsplit(exception, ","), function(s){unlist(s)[1]}),
                            util_mod = lapply(strsplit(exception, ","), function(s){unlist(s)[2]}))]

      exceptiontable <- exceptiontable[,.(SP0 = gsub(" ", "", SP0),
                                          util_mod = as.numeric(gsub(" ", "", util_mod)))]
      if(nrow(exceptiontable[!(SP0 %in% SP0_ref), ])){
        stop("SP0 in exception arguement is not correctly specified: ",
             paste0(exceptiontable[!(SP0 %in% SP0_ref), ]$SP0, collapse = ", "), ".\n",
             "SP0 must be in ", paste0(SP0_ref, collapse = ", "), ".")
      }
      output <- merge(output, exceptiontable,
                      by = "SP0", all.x = TRUE)
      output[!is.na(util_mod), util := util_mod]
      output[, util_mod := NULL]
    }
    if(nrow(output[!(util %in% util_ref), ])){
      stop("util is not correctly specified: ",
           paste0(output[!(util %in% util_ref), ]$util, collapse = ", "), ".\n",
           "It must be in ", paste0(util_ref, collapse = ", "), ".")
    }
  }

  if(askForConfirm){
    print(output)
    answer <- readline("Please confirm the util table (Y/N):")
    if(toupper(answer) == "Y"){
      return(output)
    } else if (toupper(answer) == "N"){
      stop("The util table is not returned based on your input.")
    } else {
      stop("Invalid input. Must be Y or N")
    }
  } else {
    return(output)
  }
}
