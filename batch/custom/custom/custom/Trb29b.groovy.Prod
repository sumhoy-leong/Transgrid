/**
 * @author Ventyx 2012
 *
 * Trb29b - Cheque Re-print Program. Produces a control report, error report
 *          and an output file containing cheque details and remittance advice.
 *          The output file is imported by Paris Spooler to continue with cheque
 *          reprinting on stationery.
 *          Converted from COBOL program ECB29B.
 *
 *        - Cheque run Number parameter is mandatory.
 *        - First Cheque Number parameter is optional.
 *        - Last Cheque Number parameter is optional.
 *        - If the First Cheque Number parameter is not supplied, extract from the
 *          first cheque in the file. 
 *        - If the Last Cheque Number parameter is not supplied, extract up to the
 *          last cheque in the file. 
 */

package com.mincom.ellipse.script.custom

import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.Writer

import java.text.DecimalFormat
import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

 public class ParamsTrb29b {
     // List of Input Parameters
     String paramRunNumber
     String paramFirstCheque
     String paramLastCheque
 }
 
 public class ProcessTrb29b extends SuperBatch {
     /**
      * IMPORTANT!
      * Update this Version number EVERY push to GIT
      */
     private version = 4
     private ParamsTrb29b batchParams
     
     // Globals
     private String pRunNumber
     private String pFirstCheque
     private String pLastCheque
     private String iCqwRunNumber = " "
     private String iCqwLastChequeNo = " "
     private boolean bOFileOpen = false
     private boolean bOFileCreated = false
     private boolean bErrorOccured = false
     private BigDecimal totalAmtOfCheques = 0.0
     private Integer noOfChequesPrinted = 0
     private def Trb29ba                // Error report
     private def Trb29bb                // Control report
     private BufferedWriter outputFile  // Cheque and Remittance Advice file for reprint
     
     // Constants
     private static final String BANK_BRANCH_CODE = "032-006"
     private static final String BANK_ACCOUNT_CODE = "327967"
     private static final String BANK_NAME = "WESTPAC"
     private static final String BLANK_CHAR = " "
     private static final String SOURCE_FILE = "/winshare/Treasury/Paris Spooler/AP_CHQ/Working/TRI28B.PRN"
     private static final String WORKING_FILE = "/winshare/Treasury/Paris Spooler/AP_CHQ/Working/TRI29B.PRN"
     private static final String SPOOLER_FILE = "/winshare/Treasury/Paris Spooler/AP_CHQ/Spooler/TRI29B.PRN"
     
     public void runBatch(Binding b) {
         init(b)
         
         printSuperBatchVersion()
         info("runBatch version : " + version)
         
         // Request Parameters
         batchParams = params.fill(new ParamsTrb29b())
         info("Run Number      : " + batchParams.paramRunNumber)
         info("First Cheque No : " + batchParams.paramFirstCheque)
         info("Last Cheque No  : " + batchParams.paramLastCheque)
         
         performInitialise()
         if (!bErrorOccured) {
             // Process request parameters
             processParams()
             
             // request parameters are valid so continue
             if (!bErrorOccured) {
                 try {
                     processBatch()
                 } finally {
                     printBatchReport()
                 }
             }
         }
         performFinalise()
     }
     
     /**
      * Initialisation - create reports and files
      * @param None
      * @return None
      */
     private void performInitialise() {
         info("performInitialise")
         
         noOfChequesPrinted = 0
         totalAmtOfCheques = 0.0
         
         // Create Reports A and B
         List <String> headingsA = new ArrayList <String>()
         headingsA = setPageHeadingsA()
         Trb29ba = report.open("TRB29BA", headingsA)
         
         List <String> headingsB = new ArrayList <String>()
         headingsB = setPageHeadingsB()
         Trb29bb = report.open("TRB29BB", headingsB)
         
         info("Source file location : " + SOURCE_FILE)
         info("Working file location: " + WORKING_FILE)
         info("Spooler file location: " + SPOOLER_FILE)
         
         // Check that the input file TRI28B.PRN exists in the working directory.
         // If not, write an error and terminate the program
         File srcFile = new File(SOURCE_FILE)
         if (!srcFile.exists()){
             info ("##### ERROR: Input file TRI28B.PRN does not exist in the WORKING folder#####")
             info ("##### PROCESS TERMINATED. #####")
             Trb29ba.write(">> ERROR: Input file TRI28B.PRN does not exist in the WORKING folder. PROCESS TERMINATED. <<\n")
             bErrorOccured = true
         }
         
         // Check if the output file TRI29B.PRN already exists in the spooler directory.
         // If it does, write an error and terminate the program
         if (!bErrorOccured) {
             File spoolFile = new File(SPOOLER_FILE)
             if (spoolFile.exists()){
                 info ("##### ERROR: Ouput file TRI29B.PRN already exists in the SPOOLER folder #####")
                 info ("##### PROCESS TERMINATED. #####")
                 Trb29ba.write(">> ERROR: Output file TRI29B.PRN already exists in the SPOOLER folder. PROCESS TERMINATED. <<\n")
                 bErrorOccured = true
             }
         }
         
         // Check if the output file TRI29B.PRN already exists in the working directory.
         // If it does, write an error and terminate the program
         if (!bErrorOccured) {
             File workFile = new File(WORKING_FILE)
             if (workFile.exists()){
                 info ("##### ERROR: Ouput file TRI29B.PRN already exists in the WORKING folder #####")
                 info ("##### PROCESS TERMINATED. #####")
                 Trb29ba.write(">> ERROR: Ouput file TRI29B.PRN already exists in the WORKING folder. PROCESS TERMINATED. <<\n")
                 bErrorOccured = true
             }
         }
         
         // Create output file
         if (!bErrorOccured) {
             try {
                 outputFile = new BufferedWriter(new FileWriter(WORKING_FILE))
                 bOFileOpen = true
                 bOFileCreated = true
                 info(WORKING_FILE + " sucessfully created")
                 
                 // Get Last Run Number and Last Cheque Number details
                 try {
                     TableServiceReadReplyDTO tableReply = service.get('Table').read({
                         it.tableType = "#CQW"
                         it.tableCode = "01"})
                 
                     iCqwRunNumber = tableReply.getAssociatedRecord().substring(0,6).trim()
                     iCqwLastChequeNo = tableReply.getAssociatedRecord().substring(6,12).trim()
                 } catch (EnterpriseServiceOperationException e){
                     // Error encountered after service call
                     listErrors(e)
                     info ("##### ERROR: Cheque Print Run Number and Last Cheque #CQW record not found in MSF010 #####")
                     info ("##### PROCESS TERMINATED. #####")
                     Trb29ba.write(">> ERROR: Cheque Print Run Number and Last Cheque #CQW record not found in MSF010. " +
                         "PROCESS TERMINATED. <<\n")
                     bErrorOccured = true
                 }
             } catch (IOException e){
                 // Error encountered during file creation
                 e.printStackTrace()
                 info ("##### ERROR: Unable to create output file #####")
                 info ("##### PROCESS TERMINATED. #####")
                 Trb29ba.write(">> ERROR: Unable to create output file. PROCESS TERMINATED. <<\n")
                 bErrorOccured = true
             }
         }
     }
     
     /**
      * Finalisation Process
      * @param None
      * @return None
      */
     private void performFinalise () {
         info("performFinalise")
         
         // Close output file
         if (bOFileOpen) {
             outputFile.close()
             bOFileOpen = false
         }
         
         // Move the file to the Spooler folder
         if (!bErrorOccured) {
             try {
                 //info("VENTYX - moving file from ${WORKING_FILE} to ${SPOOLER_FILE}")
                 new File(WORKING_FILE).renameTo(new File(SPOOLER_FILE))
             } catch (IOException e) {
                 // Error encountered during file move
                 e.printStackTrace()
                 info ("##### ERROR: Unable to copy output file to SPOOLER folder #####")
                 Trb29ba.write("\n>> ERROR: Unable to copy output file to SPOOLER folder. <<\n")
                 bErrorOccured = true
             } catch (Exception e) {
                 info("Unhandled exception 1")
                 info(e.toString())
                 Trb29ba.write("\n>> ERROR: Unhandled Exception Encountered. PROCESS TERMINATED. <<\n")
                 bErrorOccured = true
             }
         }
         
         if (!bErrorOccured) {
             // Print Error Report trailer
             Trb29ba.write("\n>> Process Completed Sucessfully. No errors encountered. <<\n")
             
             // Print Control report summary
             printControlReportSummary()
         }
         
         // Delete output file - this would only happen if an error occurred
         if (bErrorOccured) {
             if (bOFileCreated) {
                 if (!(new File(WORKING_FILE).delete())) {
                     info("##### ERROR: Failed to delete " + WORKING_FILE + " #####")
                     Trb29ba.write(">> Failed to delete " + WORKING_FILE + " <<\n")
                 }
             }
         }
         
         // Close reports
         Trb29ba.close()
         Trb29bb.close()
     }
     
     /**
      * Validate and setup request parameters
      * @param None
      * @return None
      */
     private void processParams() {
         info("processParams")
         
         pRunNumber = batchParams.paramRunNumber
         pFirstCheque = batchParams.paramFirstCheque
         pLastCheque = batchParams.paramLastCheque
         
         // Validate Run Number
         if (pRunNumber.trim().equals("")) {
             // Run number must be supplied
             info ("##### ERROR: Run Number is Mandatory #####")
             info ("##### PROCESS TERMINATED. #####")
             Trb29ba.write("ERROR: >> Run Number request parameter is Mandatory. PROCESS TERMINATED. <<\n")
             bErrorOccured = true
         } else if ((pRunNumber.trim() != iCqwRunNumber)) {
             // Run number must match Last Run Number from #CQW table
             info ("##### ERROR: Run Number parameter (" + pRunNumber.trim() + ") does not match the Last Run Number (" +
                 iCqwRunNumber.trim() + ") in the #CQW table. #####")
             info ("##### PROCESS TERMINATED. #####")
             Trb29ba.write(">> ERROR: Run Number parameter (" + pRunNumber.trim() + ") does not match the Last Run Number (" +
                 iCqwRunNumber.trim() + ") in the #CQW table. PROCESS TERMINATED. <<\n")
             bErrorOccured = true
         }
         
         // Validate First and Last Cheque if they are supplied
         if (!pFirstCheque.trim().equals("") && !pLastCheque.trim().equals("")) {
             if (pFirstCheque.toInteger() > pLastCheque.toInteger()) {
                 // First Cheque Number must be < Last Cheque Number
                 info ("##### ERROR: First Cheque Number parameter (" + pFirstCheque.trim() + ") must be less than " + 
                     "the Last Cheque Number parameter (" + pLastCheque.trim() + "). #####")
                 info ("##### PROCESS TERMINATED. #####")
                 Trb29ba.write(">> ERROR: First Cheque Number parameter (" + pFirstCheque.trim() + ") must be less than " +
                     "the Last Run Number parameter (" + pLastCheque.trim() + "). <<\n")
                 bErrorOccured = true
             }
         }
         
         // First Cheque Number parameter cannot be greater than the Last Cheque Number used in #CQW
         if (!pFirstCheque.trim().equals("")) {
             if (pFirstCheque.toInteger() > iCqwLastChequeNo.toInteger()) {
                 // First Cheque Number must be <= Last Cheque Number used in #CQW table
                 info ("##### ERROR: First Cheque Number parameter (" + pFirstCheque.trim() + 
                     ") must be less than or equal to the Last Cheque Number used in #CQW (" +
                     iCqwLastChequeNo.trim() + "). #####")
                 info ("##### PROCESS TERMINATED. #####")
                 Trb29ba.write(">> ERROR: First Cheque Number parameter (" + pFirstCheque.trim() + 
                     ") must be less than or equal to the Last Cheque Number used in #CQW (" +
                     iCqwLastChequeNo.trim() + "). <<\n")
                 bErrorOccured = true
             }
         }
         
         // Last Cheque Number parameter cannot be greater than the Last Cheque Number used in #CQW
         if (!pLastCheque.trim().equals("")) {
             if (pLastCheque.toInteger() > iCqwLastChequeNo.toInteger()) {
                 // First Cheque Number must be <= Last Cheque Number used in #CQW table
                 info ("##### ERROR: Last Cheque Number parameter (" + pLastCheque.trim() + 
                     ") must be less than or equal to the Last Cheque Number used in #CQW (" +
                     iCqwLastChequeNo.trim() + "). #####")
                 info ("##### PROCESS TERMINATED. #####")
                 Trb29ba.write(">> ERROR: Last Cheque Number parameter (" + pLastCheque.trim() + 
                     ") must be less than or equal to the Last Cheque Number used in #CQW (" +
                     iCqwLastChequeNo.trim() + "). <<\n")
                 bErrorOccured = true
             }
         }
     }
     
     /**
      * Main process after initialisation and validation are successful
      * @param None
      * @return None
      */
     private void processBatch() {
         info("processBatch")
         
         // No First and Last Cheque number request parameters provided i.e. a complete extract
         if (pFirstCheque.trim().equals("") && pLastCheque.trim().equals("")) {
             extractAll()
         } else {
         // At least one of the cheque numbers have been supplied i.e. a partial extract
         
             // Write header line to output file
             writeFileHeader()
             
             // Read through the input file to locate the start and end line numbers to extract
             // then extract the cheques to the output file TRI29B.PRN
             extractCheques()
         }
     }
     
     // Print the report
     private void printBatchReport() {
         info("printBatchReport")
         //print batch report
     }
     
     /**
      * If the First and Last Cheque number request parameters are empty, then the entire
      * cheque file is to be reprinted. In this case, just copy the entire input file.
      * @param None
      * @return None
      */ 
     private void extractAll() {
         info("extractAll")
         
         new File(SOURCE_FILE).eachLine {line ->
             
             // If the current line is not a blank line, do some further testing
             if (line.size() > 0) {
                 // See if the current line is the Cheque Number line i.e. the
                 // line begins with the literals " Cheque No "
                 if (line.substring(0,11) == " Cheque No ") {
                     // Increment cheque counter
                     noOfChequesPrinted++
                     // Set First Cheque No for control report
                     if (pFirstCheque.trim().equals("")) {
                         pFirstCheque = line.substring(11,17)
                     }
                     // Set Last Cheque No for control report
                     // Keep replacing the value with the current cheque and it
                     // will eventually get to the actual Last Cheque No within
                     // the start and end line number boundary
                     pLastCheque = line.substring(11,17)
                 }
                 
                 // See if the current line is the Cheque Total line i.e. the
                 // line begins with the literals "3TOTAL"
                 if (line.substring(0,6) == "3TOTAL") {
                     // Cheque amount in this line starts from position 66 up to 79
                     // Remove commas prior to converting string to big decimal 
                     String sChequeAmount = (line.substring(65,79)).replace(",","")
                     totalAmtOfCheques += sChequeAmount.toBigDecimal()
                 }
             }
             // write the current line to the output file
             outputFile.write(line + "\n")
         }
     }
     
     /**
      * Read through the input file to locate the start and end line numbers to extract
      * then extract the cheques within those lines to the output file TRI29B.PRN
      * @param None
      * @return None
      */
     private void extractCheques() {
         info("extractCheques")
         
         Integer startLineNo = 0
         Integer endLineNo = 0
         Integer lineCount = 0
         boolean lastChequeFound = false
         
         // Start line number will be 3 if the start cheque number is not supplied
         if (pFirstCheque.trim().equals("")) {
             startLineNo = 3
         }
         
         // The first pass of the input file is to locate the start and end line numbers
         // of the cheques to extract
         new File(SOURCE_FILE).eachLine {line ->
             lineCount++
             
             // Locate the start line number
             if ((!pFirstCheque.trim().equals("")) &&(startLineNo == 0)) {
                 // If the current line is not a blank line, do some further testing
                 if (line.size() > 0) {
                     // See if the line being processed is the Cheque Number line
                     if (line.substring(0,11) == " Cheque No ") {
                         // See if this matches the First Cheque Number parameter
                         if (line.substring(11,17) == pFirstCheque) {
                             startLineNo = lineCount - 10
                         }
                     }
                 } 
             }
             
             // Locate the finish line number
             if ((!pLastCheque.trim().equals("")) && (endLineNo == 0)) {
                 // If the current line is not a blank line, do some further testing
                 if (line.size() > 0) {
                     // See if the line being processed is the Cheque Number line
                     // i.e. the line begins with the literals " Cheque No "
                     if (line.substring(0,11) == " Cheque No ") {
                         // See if this matches the Last Cheque Number parameter
                         if (line.substring(11,17) == pLastCheque) {
                             lastChequeFound = true
                         }
                     }
                     
                     // Last cheque was found now keep reading the lines until the
                     // total line for the cheque is found - i.e. the next line that
                     // begins with the literals "3TOTAL"
                     if (lastChequeFound) {
                         // See if the line being processes is the cheque TOTAL line
                         if (line.substring(0,6) == "3TOTAL") {
                             endLineNo = lineCount
                         }
                     } 
                 }
             }
         }
         
         // Set endLineNo to the last line of the input file if the Last Cheque No
         // request parameter is not specified
         if ((pLastCheque.trim().equals("")) && (endLineNo == 0)) {
             endLineNo = lineCount
         }
         
         // At this point the entire input file has been processed. If start line number is 
         // still not determined then the cheque does not exist in the input file
         if (startLineNo == 0) {
             // First Cheque Number not in the input file
             info ("##### ERROR: First Cheque Number parameter (" + pFirstCheque.trim() +
                 ") is not in the input file #####")
             info ("##### PROCESS TERMINATED. #####")
             Trb29ba.write(">> ERROR: First Cheque Number parameter (" + pFirstCheque.trim() +
                 ") is not in the input file. <<\n")
             bErrorOccured = true
         }
         
         // At this point the entire input file has been processed. If end line number is 
         // still not determined then the cheques does not exist in the input file
         if (endLineNo == 0) {
             // Last Cheque Number not in the input file
             info ("##### ERROR: Last Cheque Number parameter (" + pLastCheque.trim() +
                 ") is not in the input file #####")
             info ("##### PROCESS TERMINATED. #####")
             Trb29ba.write(">> ERROR: Last Cheque Number parameter (" + pLastCheque.trim() +
                 ") is not in the input file. <<\n")
             bErrorOccured = true
         }
         
         // The second pass of the input file is to write lines within the boundaries
         // of the start and end line numbers to the output file TRI29B.PRN
         if (!bErrorOccured) {
             lineCount = 0
             new File(SOURCE_FILE).eachLine {line ->
                 lineCount++
                 
                 // Current line being processed is within the start and end line numbers
                 // so write the line to the output file
                 if ((lineCount >= startLineNo) && (lineCount <= endLineNo)) {
                     // If the current line is not a blank line, do some further testing
                     if (line.size() > 0) {
                         // See if the current line is the Cheque Number line i.e. the
                         // line begins with the literals " Cheque No "
                         if (line.substring(0,11) == " Cheque No ") {
                             noOfChequesPrinted++
                             // Set First Cheque No for control report
                             if (pFirstCheque.trim().equals("")) {
                                 pFirstCheque = line.substring(11,17)
                             }
                             // Set Last Cheque No for control report
                             // Keep replacing the value with the current cheque and it
                             // will eventually get to the actual Last Cheque No within
                             // the start and end line number boundary
                             pLastCheque = line.substring(11,17)
                         }
                         
                         // See if the current line is the Cheque Total line i.e. the
                         // line begins with the literals "3TOTAL"
                         if (line.substring(0,6) == "3TOTAL") {
                             // Cheque amount in this line starts from position 66 up to 79
                             // Remove commas prior to converting string to big decimal
                             String sChequeAmount = (line.substring(65,79)).replace(",","")
                             totalAmtOfCheques += sChequeAmount.toBigDecimal()
                         }
                     }
                     // write the current line to the output file
                     outputFile.write(line + "\n")
                 }
             }
         }
     }
     
     // Set Report A header
     private List setPageHeadingsA() {
         info("setPageHeadingsA")
         
         List <String> headings = new ArrayList <String>()
         headings.add("CHEQUE REPRINT - ERROR REPORT".center(132))
         
         return headings
     }
     
     // Set Report B header
     private List setPageHeadingsB() {
         info("setPageHeadingsA")
         
         List <String> headings = new ArrayList <String>()
         headings.add("CHEQUE REPRINT - CONTROL REPORT".center(132))
         
         return headings
     }
     
     // Print Control Report
     private void printControlReportSummary() {
         info("printControlReportSummary")
         
         String prtTotalAmtOfCheques = "\$" + (new DecimalFormat("###,###,##0.00").format(totalAmtOfCheques)).toString()
         String prtTotalNoOfCheques = (new DecimalFormat("####0").format(noOfChequesPrinted)).toString()
         String prtFirstChequeNo = pFirstCheque
         String prtLastChequeNo = pLastCheque
         
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //For Cheque Run Number: XXXXXX                                                                          Branch:  XXXXXXXXXX
         Trb29bb.write("For Cheque Run Number: " + pRunNumber.padRight(6) + BLANK_CHAR.padRight(74) +
             "Branch:  " + BANK_BRANCH_CODE + "\n")
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                                                                                                       Account: XXXXXX
         Trb29bb.write(BLANK_CHAR.padRight(103) + "Account: " + BANK_ACCOUNT_CODE + "\n")
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                                         Total Cheques Printed Value:       $999,999,999.99
         Trb29bb.write(BLANK_CHAR.padRight(41) + "Total Cheques Printed Value:       " + prtTotalAmtOfCheques + "\n")
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                                         Total no. of Cheques Printed:      99999   First Cheque No: 999999, Last Cheque No: 999999
         Trb29bb.write(BLANK_CHAR.padRight(41) + "Total no. of Cheques Printed:      " + prtTotalNoOfCheques + "   First Cheque No: " +
             prtFirstChequeNo + ", Last Cheque No: " + prtLastChequeNo + "\n")
     }
     
     /**
      * Output file header
      */
     private void writeFileHeader() {
         outputFile.write("\n" + "\$XLP ENV=ECR28B2,END;" + "\n")
     }
      
     /**
      * Write blank line
      */
     private void writeBlankLine() {
         outputFile.write("\n")
     }
     
     /**
      * List all errors encountered to the log after running a web service
      * @param e Error Object
      * @return None
      */
     private void listErrors(EnterpriseServiceOperationException e) {
         List <ErrorMessageDTO> listError = e.getErrorMessages()
         listError.each{ErrorMessageDTO errorDTO ->
             info("Error Code: " + errorDTO.getCode())
             info("Error Message: " + errorDTO.getMessage())
             info("Error Fields: " + errorDTO.getFieldName())
         }
     }
 }
 
/*run script*/
ProcessTrb29b process = new ProcessTrb29b()
process.runBatch(binding)

