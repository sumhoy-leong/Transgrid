/**
 * @author Ventyx 2012
 *
 * Trb28b - Cheque Generation Program. Produces a control report, error report
 *          and an output file containing cheque details and remittance advice.
 *          The output file is imported by Paris Spooler to continue with cheque
 *          printing on stationery.
 *          Converted from COBOL program ECB28B.
 *          
 * Date       Modifier   Description
 * 29/7/2013  Yee Mun    SC0000004290790 - v5
 *                       copy the private method writeInvCommentsToFile2 to
 *                       MSF281.INV_no is not blank block.
 *                       Initialize iCqwRunNumber to zero.
 * 19/6/2013  Yee Mun    SC0000004290790 - v4
 *                       create a new private method writeInvCommentsToFile2 to 
 *                       use msf282 object to pass the input parameter. 
 *                       Then replace the private method writeInvCommentsToFile.
 *                       Comment useless private method writeInvCommentsToFile.     
 */

package com.mincom.ellipse.script.custom

import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.edoi.ejb.msf260.MSF260Key
import com.mincom.ellipse.edoi.ejb.msf260.MSF260Rec
import com.mincom.ellipse.edoi.ejb.msf26a.MSF26AKey
import com.mincom.ellipse.edoi.ejb.msf26a.MSF26ARec
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Key
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Rec
import com.mincom.ellipse.edoi.ejb.msf280.MSF280_RUN_CTLKey
import com.mincom.ellipse.edoi.ejb.msf280.MSF280_RUN_CTLRec
import com.mincom.ellipse.edoi.ejb.msf281.MSF281Key
import com.mincom.ellipse.edoi.ejb.msf281.MSF281Rec
import com.mincom.ellipse.edoi.ejb.msf282.MSF282Key
import com.mincom.ellipse.edoi.ejb.msf282.MSF282Rec
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.ellipse.types.m0000.instances.TableType
import com.mincom.ellipse.types.m0000.instances.TableCode
import com.mincom.ellipse.types.m0000.instances.AssocRec
import com.mincom.ellipse.types.m3001.instances.TableCodeDTO
import com.mincom.ellipse.types.m3001.instances.TableCodeServiceResult
import com.mincom.ellipse.eroi.linkage.msscnv.*
import com.mincom.ellipse.eroi.linkage.mssatx.*
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import java.text.DecimalFormat
import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

 public class ParamsTrb28b {
     // List of Input Parameters
     String paramRunNumber
     String paramBank
 }
 
 public class ProcessTrb28b extends SuperBatch {
     /**
      * IMPORTANT!
      * Update this Version number EVERY push to GIT
      */
     private version = 5
     private ParamsTrb28b batchParams
     
     // Globals
     private String pRunNumber
     private String pBankCode
     //v5- initialise iCqwRunNumber to zero
     private String iCqwRunNumber = 0
     private String iCqwLastChequeNo
     private Integer iNextChequeNumber = 0
     private boolean bOFileOpen = false
     private boolean bOFileCreated = false
     private boolean bErrorOccured = false
     private boolean bChequeNoUsed = false
     private BigDecimal totalAmtOfCheques = 0.0
     private Integer firstChequeNo = 0
     private Integer noOfChequesPrinted = 0
     
     private def Trb28ba                // Error report
     private def Trb28bb                // Control report
     private BufferedWriter outputFile  // Cheque and Remittance Advice file
     
     // Print fields that go on the reports - must always initialize the print fields
     private String prtSupplierNo = " "
     private String prtBank = " "
     private String prtDate1 = " "
     private String prtChequeAmountWords1 = " "
     private String prtChequeAmountWords2 = " "
     private String prtDate2 = " "
     private String prtInvoiceTotal = " "
     private String prtSupplierName = " "
     private String prtSupplierAddress1 = " "
     private String prtSupplierAddress2 = " "
     private String prtSupplierAddress3 = " "
     private String prtSupplierPostCode = " "
     private String prtChequeNumber = " "
     private String prtChequeAndBank = " "
     private String prtInvoiceNumber = " "
     private String prtOrderNumber = " "
     private String prtSettlementDiscount = " "
     private String prtCRIndicator = " "
     private String prtPrescribedPayment = " "
     private String prtNetPayment = " "
     private String prtNetPaymentCRIndicator = " "
     private String prtRemAdvComments = " "
     private String prtChequeTotalValue = " "
     private String prtSection1 = " "
     private String prtSection2 = " "
     private String prtSection3 = " "
     private String prtTotalAmtOfCheques = " "
     private String prtTotalNoOfCheques = " "
     private String prtFirstChequeNo = " "
     private String prtLastChequeNo = " "
     
     // Constants
     private static final String BANK_DISTRICT = "GRID"
     private static final String BANK_BRANCH_CODE = "032-006"
     private static final String BANK_ACCOUNT_CODE = "327967"
     private static final String BANK_NAME = "WESTPAC"
     private static final String BLANK_CHAR = " "
     private static final String WORKING_LOC = "/winshare/Treasury/Paris Spooler/AP_CHQ/Working/TRI28B.PRN"
     private static final String SPOOLER_LOC = "/winshare/Treasury/Paris Spooler/AP_CHQ/Spooler/TRI28B.PRN"
     
     public void runBatch(Binding b) {
         init(b)
         
         printSuperBatchVersion()
         info("runBatch version : " + version)
         
         // Request Parameters
         batchParams = params.fill(new ParamsTrb28b())
         info("Run Number: " + batchParams.paramRunNumber)
         info("Bank:       " + batchParams.paramBank)
         
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
         
         boolean allOk = true
         noOfChequesPrinted = 0
         totalAmtOfCheques = 0.0
         
         // Create Reports A and B
         List <String> headingsA = new ArrayList <String>()
         headingsA = setPageHeadingsA()
         Trb28ba = report.open("TRB28BA", headingsA)
         
         List <String> headingsB = new ArrayList <String>()
         headingsB = setPageHeadingsB()
         Trb28bb = report.open("TRB28BB", headingsB)
         
         
         info("Working file location: " + WORKING_LOC)
         info("Spooler file location: " + SPOOLER_LOC)
         
         // Check if the file already exists in the spooler directory.
         // If it does, write an error and terminate program
         File spoolSrc = new File(SPOOLER_LOC)
         if (spoolSrc.exists()){
             info ("##### ERROR: Ouput file TRI28B.PRN already exists in the SPOOLER folder #####")
             info ("##### PROCESS TERMINATED #####")
             Trb28ba.write(">> ERROR: Output file TRI28B.PRN already exists in the SPOOLER folder. PROCESS TERMINATED <<\n")
             bErrorOccured = true
         } else {
             // Check if the file already exists in the working directory.
             // If it does, write an error and terminate program
             File src = new File(WORKING_LOC)
             if (src.exists()){
                 info ("##### ERROR: Ouput file TRI28B.PRN already exists in the WORKING folder #####")
                 info ("##### PROCESS TERMINATED #####")
                 Trb28ba.write(">> ERROR: Ouput file TRI28B.PRN already exists in the WORKING folder. PROCESS TERMINATED <<\n")
                 bErrorOccured = true
             } else {
                 // Create output file
                 try {
                     outputFile = new BufferedWriter(new FileWriter(WORKING_LOC))
                     bOFileOpen = true
                     bOFileCreated = true
                     info(WORKING_LOC + " sucessfully created")
                     
                     // Get Last Run Number and Last Cheque Number details
                     try {
                         TableServiceReadReplyDTO tableReply = service.get('Table').read({
                             it.tableType = "#CQW"
                             it.tableCode = "01"})
                         
                         iCqwRunNumber = tableReply.getAssociatedRecord().substring(0,6)
                         iCqwLastChequeNo = tableReply.getAssociatedRecord().substring(6,12)
                         iNextChequeNumber = iCqwLastChequeNo.toInteger()
                         firstChequeNo = iCqwLastChequeNo.toInteger() + 1
                     } catch (EnterpriseServiceOperationException e){
                         // Error encountered after service call
                         listErrors(e)
                         info ("##### ERROR: Cheque Print Run Number and Last Cheque #CQW record not found in MSF010 #####")
                         info ("##### PROCESS TERMINATED #####")
                         Trb28ba.write(">> ERROR: Cheque Print Run Number and Last Cheque #CQW record not found in MSF010. " +
                             "PROCESS TERMINATED <<\n")
                         bErrorOccured = true
                     }
                 } catch (IOException e){
                     // Error encountered during file creation
                     e.printStackTrace()
                     info ("##### ERROR: Unable to create output file #####")
                     info ("##### PROCESS TERMINATED #####")
                     Trb28ba.write(">> ERROR: Unable to create output file. PROCESS TERMINATED <<\n")
                     bErrorOccured = true
                 }
             }
         }
         info("performInitialise complete")
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
         
         // Update #CQW table with last cheque number used.
         if (!bErrorOccured) {
             updateCQW()
         }
         
         // Copy the file to the Spooler folder
         if (!bErrorOccured) {
             try {
                 //info("VENTYX - copying file from ${WORKING_LOC} to ${SPOOLER_LOC}")
                 def srcFile = new File(WORKING_LOC)
                 def destFile = new File(SPOOLER_LOC)
                 // Create an empty file
                 destFile.createNewFile()
                 // Append the contents of the source file to the new file
                 srcFile.withInputStream {is ->
                     destFile << is
                 }
             } catch (IOException e) {
                 // Error encountered during file move
                 e.printStackTrace();
                 info ("##### ERROR: Unable to copy output file to SPOOLER folder #####")
                 Trb28ba.write("\n>> ERROR: Unable to copy output file to SPOOLER folder. <<\n")
                 bErrorOccured = true
             } catch (Exception e) {
                 info("VENTYX - Unhandled exception 4")
                 info(e.toString())
                 Trb28ba.write("\n>> ERROR: Unhandled Exception Encountered. PROCESS TERMINATED <<\n")
                 bErrorOccured = true
             }
         }
         
         if (!bErrorOccured) {
             // Print Error Report trailer
             Trb28ba.write("\n>> Process Completed Sucessfully. No errors encountered. <<\n")
             
             // Print Control report summary
             printControlReportSummary()
         }
         
         // Delete output file - this would only happen if an error occurred
         if (bErrorOccured) {
             if (bOFileCreated) {
                 if (!(new File(WORKING_LOC).delete())) {
                     info("##### ERROR: Failed to delete " + WORKING_LOC + " #####")
                     Trb28ba.write(">> Failed to delete " + WORKING_LOC + " <<\n")
                 } else {
                     info("##### " + WORKING_LOC + " deleted #####")
                 }
             }
         }
         
         // Close reports
         Trb28ba.close()
         Trb28bb.close()

         info("performFinalise complete")
     }
     
     /**
      * Validate and setup request parameters
      * @param None
      * @return None
      */
     
     private void processParams() {
         info("processParams")
         
         pRunNumber = batchParams.paramRunNumber
         pBankCode = batchParams.paramBank
         
         // Validate Run Number
         if (pRunNumber.trim().equals("")) {
             // Run number must be supplied
             info ("##### ERROR: Run Number is Mandatory #####")
             info ("##### PROCESS TERMINATED #####")
             Trb28ba.write("ERROR: >> Run Number request parameter is Mandatory. " +
                 "PROCESS TERMINATED <<\n")
             bErrorOccured = true
         } else if (!(pRunNumber.toInteger() > iCqwRunNumber.toInteger())) {
             // Run number must be > Last Run Number from #CQW table
             info ("##### ERROR: Run Number (" + pRunNumber + ") must be > Last Run Number (" +
                 iCqwRunNumber + ") #####")
             info ("##### PROCESS TERMINATED #####")
             Trb28ba.write(">> ERROR: Run Number (" + pRunNumber + ") must be > Last Run Number (" +
                 iCqwRunNumber + "). PROCESS TERMINATED <<\n")
             bErrorOccured = true
         } else {
             try {
                 // Validate the Run Number. Normally a service would be used but the Cheque service
                 // does not support the search parameters that we have. A screen service is also not
                 // possible since MSM281A requires input criteria that we do not have. Using EDOI.
                 MSF280_RUN_CTLKey msf280key = new MSF280_RUN_CTLKey()
                 msf280key.acctDstrct = BANK_DISTRICT
                 msf280key.branchCode = BANK_BRANCH_CODE
                 msf280key.bankAcctNo = BANK_ACCOUNT_CODE
                 msf280key.chequeRunNo = pRunNumber
                 msf280key.invDstrct = ""
                 msf280key.ordSupplierNo = ""
                 msf280key.invNo = ""
                 msf280key.mcprtRunNo = ""
                 msf280key.recType = "00"
                 msf280key.seqNoMsf280 = "00"
                 
                  MSF280_RUN_CTLRec msf280RecRead = edoi.findByPrimaryKey(msf280key)
                 if (msf280RecRead.getRcPmtMethInd() != 'C') {
                     info("##### ERROR: This is an EFT Run No - No Cheques #####")
                     info("##### PROCESS TERMINATED #####")
                     Trb28ba.write(">> ERROR: This is an EFT Run No - No Cheques. PROCESS TERMINATED <<\n")
                     bErrorOccured = true
                 }
             } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                 info("##### ERROR: No run control for current request #####")
                 info("##### PROCESS TERMINATED #####")
                 Trb28ba.write(">> ERROR: No run control for current request. PROCESS TERMINATED <<\n")
                 bErrorOccured = true
             }
         }
         
         // Validate Bank Code
         if (pBankCode != 'W') {
             info("##### ERROR: Name of Bank Invalid #####")
             info("##### PROCESS TERMINATED #####")
             Trb28ba.write(">> ERROR: Name of Bank Invalid. Not 'W'. PROCESS TERMINATED <<\n")
             bErrorOccured = true
         }
     }
     
     private void processBatch() {
         info("processBatch")
         
         // Write header line to output file
         writeFileHeader()
         
         // Process through Payments File
         searchPayments()
     }
     
     // Print the report
     private void printBatchReport() {
         info("printBatchReport")
         //print batch report
     }
     
     /**
      * Get the Invoice Payments for the nominated Pay Run
      */
     private void searchPayments() {
         info("searchPayments")
         
         SupplierServiceReadReplyDTO supplierDetails
         
         // Get payments from MSF281
         Constraint cPayRunId = MSF281Key.payRunId.equalTo(pRunNumber)
         Constraint cAcctDstrct = MSF281Key.acctDstrct.equalTo(BANK_DISTRICT)
         Constraint cBranchCode = MSF281Key.branchCode.equalTo(BANK_BRANCH_CODE)
         Constraint cAccountCode = MSF281Key.bankAcctNo.equalTo(BANK_ACCOUNT_CODE)
         Constraint cChequeRunNo = MSF281Key.chequeRunNo.equalTo(pRunNumber)
         Constraint cChqAmount = MSF281Rec.chqAmount.greaterThan(0.0)
         
         def query = new QueryImpl(MSF281Rec.class).and(cPayRunId).and(cAcctDstrct).and(cBranchCode).and(cAccountCode).
             and(cChequeRunNo).and(cChqAmount)
             
         try {
             //info("VENTYX - Get Payment record MSF281")
             edoi.search(query, {MSF281Rec msf281Rec ->
                 String sSuppToPay = msf281Rec.getPrimaryKey().getSuppToPay()
                 
                 // Confirm that the supplier is valid
                 try {
                     //info("VENTYX - Validate Supplier")
                     SupplierServiceReadReplyDTO supplierReply = service.get('Supplier').read({
                         it.supplierNo = sSuppToPay})
                     
                     // Next Cheque Number to attempt to use
                     iNextChequeNumber++
                     
                     // Confirm that the cheque number is still available
                     try {
                         //info("VENTYX - Validate Cheque Number")
                         // See if the Cheque Number has already been used
                         MSF270Key msf270key = new MSF270Key()
                         msf270key.branchCode = BANK_BRANCH_CODE
                         msf270key.bankAcctNo = BANK_ACCOUNT_CODE
                         msf270key.chequeNo = String.format("%06d", iNextChequeNumber)
                         
                         MSF270Rec msf270RecRead = edoi.findByPrimaryKey(msf270key)
                         if (msf270RecRead.getPrimaryKey().getChequeNo() != '') {
                             info("VENTYX - Cheque Number already used")
                             bChequeNoUsed = true
                             throw new Exception("Cheque Number already used")
                         }
                     } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                         //info("VENTYX - Cheque Number available")
                         // The ideal situation is that the record does not exist i.e. the cheque number
                         // is still available.
                     
                         // Write Cheque information
                         writeChequeToFile(msf281Rec, supplierReply)
                         noOfChequesPrinted++
                         totalAmtOfCheques += msf281Rec.getChqAmount()
                         
                         // Get invoice details to print on remittance advice
                         getInvoicePayments(msf281Rec.getPrimaryKey())
                         
                         // Write Cheque Total to remittance advice
                         DecimalFormat dformat = new DecimalFormat("###,###,##0.00")
                         prtChequeTotalValue = dformat.format(msf281Rec.getChqAmount()).toString()
                         prtSection3 = "3"
                         writeRemAdvTotal()
                     } catch (Exception e) {
                         //info("VENTYX - Unhandled exception 1")
                         info(e.toString())
                         throw new Exception("Unhandled exception 1")
                     }
                 } catch (EnterpriseServiceOperationException e){
                     // Supplier not found
                     listErrors(e)
                     info("##### ERROR: Supplier (" + sSuppToPay + ") is invalid #####")
                     info("##### PROCESS TERMINATED #####")
                     Trb28ba.write(">> ERROR: Supplier (" + sSuppToPay + ") is invalid. PROCESS TERMINATED <<\n")
                     bErrorOccured = true
                 } catch (Exception e) {
                     info("Unhandled exception 2")
                     info(e.toString())
                     throw new Exception("Unhandled exception 2")
                 }
             })
         } catch (Exception e){
             bErrorOccured = true
             if (bChequeNoUsed) {
                 // The cheque number is already used
                 info("##### ERROR: Cheque Number " + String.format("%06d", iNextChequeNumber) + " is already used #####")
                 info("##### PROCESS TERMINATED #####")
                 Trb28ba.write(">> ERROR: Cheque Number " + String.format("%06d", iNextChequeNumber) + " is already used. " +
                     "PROCESS TERMINATED <<\n")
             } else {
                 // Unknown exception has occurred
                 performFinalise()
                 e.printStackTrace()
                 info("#### ERROR: Unhandled Exception Encountered ####")
                 info("#### ERROR: ${e.getMessage()} ####")
                 Trb28ba.write(">> ERROR: Unhandled Exception Encountered. PROCESS TERMINATED <<\n")
                 bErrorOccured = true
                 // Throw another exception so that the program will abort.
                 //throw new Exception(e.toString())
             }
         }
     }
     
     /**
      * Write Cheque details to TRI28B.PRN
      * @param msf281Rec - Payment Record
      * @param supplierDetails - Supplier Details
      * @return None
      */
     private void writeChequeToFile (MSF281Rec msf281Rec, SupplierServiceReadReplyDTO supplierDetails) {
         info("writeChequeToFile")
         
         // Supplier
         prtSection1 = "1"
         prtSection2 = "2" // setting this variable here so that it will print only for the first remittance line
         prtSupplierNo = supplierDetails.getSupplierNo()
         writeChequeLine1()
         
         //info("VENTYX: Cheque Date - " + msf281Rec.getPaidDate())
         // Cheque Date
         if (msf281Rec.getPaidDate().trim().equals("")) {
             prtDate1 = new SimpleDateFormat ("dd-MM-yy").format(new Date())
             prtDate2 = prtDate1
         } else {
             prtDate1 = msf281Rec.getPaidDate().substring(6,8) + "-" +
                 msf281Rec.getPaidDate().substring(4,6) + "-" +
                 msf281Rec.getPaidDate().substring(2,4)
             prtDate2 = prtDate1
         }
         writeChequeLine2()
         
         //Convert cheque amount to words
         MSSCNVLINK msscnvlink = eroi.execute('MSSCNV', {MSSCNVLINK msscnvlink ->
             msscnvlink.option = "W"
             msscnvlink.numericValue = msf281Rec.getChqAmount()
             msscnvlink.lineLth = 78})
         
         // Cheque amount in words line 1
         prtChequeAmountWords1 = msscnvlink.getOutString().substring(0,78).trim()
         writeChequeLine3()
         
         // Cheque amount in words line 2
         prtChequeAmountWords1 = msscnvlink.getOutString().substring(78).trim()
         writeChequeLine4()
         
         // Cheque Date and Formatted Cheque Amount
         // prtDate2 is populated above along with prtDate1
         DecimalFormat dformat = new DecimalFormat("###,###,##0.00")
         String s = dformat.format(msf281Rec.getChqAmount()).toString()
         
         // Prefix the cheque amount with *. Max size of the field is 14 chars
         // e.g. 123456.78 will display as ****123,456.78
         prtInvoiceTotal = ""
         for (Integer n in 1..(14 - s.size())) {
             prtInvoiceTotal += "*"
         }
         prtInvoiceTotal += s.trim()
         writeChequeLine5()
         
         // Supplier Name and Address
         if (supplierDetails.getPaymentName().trim().equals("")) {
             prtSupplierName = supplierDetails.getSupplierName()
             prtSupplierAddress1 = supplierDetails.getOrderAddr1()
             prtSupplierAddress2 = supplierDetails.getOrderAddr2()
             prtSupplierAddress3 = supplierDetails.getOrderAddr3()
             prtSupplierPostCode = supplierDetails.getOrderZip()
         } else {
             prtSupplierName = supplierDetails.getPaymentName()
             prtSupplierAddress1 = supplierDetails.getPayAddr1()
             prtSupplierAddress2 = supplierDetails.getPayAddr2()
             prtSupplierAddress3 = supplierDetails.getPayAddr3()
             prtSupplierPostCode = supplierDetails.getPayZip()
         }
         
         // Cheque Supplier Name
         writeChequeLine6()
         
         // Cheque Supplier Address
         writeChequeLine7()
         writeChequeLine8()
         if (prtSupplierAddress3.trim().equals("")) {
             writeChequeLine10()
             writeBlankLine()
         } else {
             writeChequeLine9()
             writeChequeLine10()
         }
         
         // Cheque Number
         prtChequeNumber = String.format("%06d", iNextChequeNumber)
         writeChequeLine11()
         
         // Cheque No and Bank Account
         prtChequeAndBank = "C" + String.format("%06d", iNextChequeNumber) + " C" + BANK_BRANCH_CODE.substring(0,3) + "D" +
              BANK_BRANCH_CODE.substring(4) + "B " + BANK_ACCOUNT_CODE.substring(0,2) + "D" +
              BANK_ACCOUNT_CODE.substring(2) + "C"
         writeChequeLine12()
         
         // 3 blank lines
         writeBlankLine()
         writeBlankLine()
         writeBlankLine()
     }
     
     /**
      * Print remittance advice details by Invoice Number. Also print invoice comments if
      * there are any.
      * @param MSF281Key - Primary key of MSF821
      * @return None
      */
     private void getInvoicePayments(MSF281Key msf281Key) {
         info("getInvoicePayments")
         
         // Breakdown the key into individual fields
         String sAcctDstrct = msf281Key.getAcctDstrct()
         String sBranchCode = msf281Key.getBranchCode()
         String sBankAcctNo = msf281Key.getBankAcctNo()
         String sChequeRunNo = msf281Key.getChequeRunNo()
         String sHandleCde = msf281Key.getHandleCde()
         String sSuppToPay = msf281Key.getSuppToPay()
         String sInvDstrct = msf281Key.getInvDstrct()
         String sOrdSupplierNo = msf281Key.getOrdSupplierNo()
         String sInvNo = msf281Key.getInvNo()
         
         if (sInvNo.trim().equals("")) {
             info("VENTYX - Invoice Number is unknown")
             // Invoice Number is unknown
             Constraint cAcctDstrct = MSF282Key.acctDstrct.equalTo(sAcctDstrct)
             Constraint cBranchCode = MSF282Key.branchCode.equalTo(sBranchCode)
             Constraint cAccountCode = MSF282Key.bankAcctNo.equalTo(sBankAcctNo)
             Constraint cChequeRunNo = MSF282Key.chequeRunNo.equalTo(sChequeRunNo)
             Constraint cHandleCode = MSF282Key.handleCde.equalTo(sHandleCde)
             Constraint cSuppToPay = MSF282Key.suppToPay.equalTo(sSuppToPay)
             Constraint cInvDstrct = MSF282Key.invDstrct.equalTo(sInvDstrct)
             Constraint cOrdSupplierNo = MSF282Key.ordSupplierNo.equalTo(sOrdSupplierNo)
             Constraint cInvNo = MSF282Key.invNo.equalTo(sInvNo)
             Constraint cInvDstrctGT = MSF282Key.invDstrct.greaterThan(" ")
             
             def query = new QueryImpl(MSF282Rec.class).and(cAcctDstrct).and(cBranchCode).and(cAccountCode).
                 and(cChequeRunNo).and(cHandleCode).and(cSuppToPay).and(cInvDstrctGT)
             
             //info("VENTYX[YM]-sInvDstrct:"+sInvDstrct+",sSuppToPay:"+sSuppToPay+",sInvNo:"+sInvNo)
   
             edoi.search(query, {MSF282Rec msf282Rec ->
                 // Get invoice details and write details to remittance advice
                 writeInvoiceDetailsToFile(msf282Rec)
                 // Get invoice comments and write to remittance advice
                 //V4 - Start
                 //sInvDstrct and sInvNo are blank and this cause the standard 
                 //text record has not found. So use MSF282's InvDstrct ,SuppToPay and
                 //Inv No to search the Standard Text. Create a new private method
                 // writeInvCommentsToFile2.
                 //Comment the commentsKey field and method writeInvCommentsToFile(String)
                 //String commentsKey = "IX" + sInvDstrct + sSuppToPay + sInvNo
                 //writeInvCommentsToFile(commentsKey)
                 writeInvCommentsToFile2(msf282Rec)
                 //V4 - End
             })
         } else {
             info("VENTYX - Invoice Number is known " + sInvNo)
             // Invoice Number is known
             try {
                 MSF282Key msf282key = new MSF282Key()
                 msf282key.acctDstrct = sAcctDstrct
                 msf282key.branchCode = sBranchCode
                 msf282key.bankAcctNo = sBankAcctNo
                 msf282key.chequeRunNo = sChequeRunNo
                 msf282key.handleCde = sHandleCde
                 msf282key.suppToPay = sSuppToPay
                 msf282key.invDstrct = sInvDstrct
                 msf282key.ordSupplierNo = sOrdSupplierNo
                 msf282key.invNo = sInvNo
                 
                 MSF282Rec msf282Rec = edoi.findByPrimaryKey(msf282key)
                 
                 // Get invoice details and write details to remittance advice
                 writeInvoiceDetailsToFile(msf282Rec)
                 
                 // Get invoice comments and write to remittance advice
                 //v5-put the method writeInvCommentsToFile2(msf282Rec) here

                 writeInvCommentsToFile2(msf282Rec)
                 //v4-Comment useless commentsKey variable.
                 //String commentsKey = "IX" + sInvDstrct + sSuppToPay + sInvNo
                 //writeInvCommentsToFile(commentsKey)
             } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                 // If the record is not found then just continue
                 //info("VENTYX - did not find MSF282 record")
             } catch (Exception e) {
                 info ("Unhandled exception 3")
                 throw new Exception("Unhandled exception 3")
             }
         }
     }
    
     /**
      * Get details of the selected invoice and print to remittance advice
      * @param msf282Rec - MSF282 record
      * @returns None
      */
     private void writeInvoiceDetailsToFile(MSF282Rec msf282Rec) {
         info("writeInvoiceDetailsToFile")
         
         String sAcctDstrct = msf282Rec.getPrimaryKey().getAcctDstrct()
         String sBranchCode = msf282Rec.getPrimaryKey().getBranchCode()
         String sBankAcctNo = msf282Rec.getPrimaryKey().getBankAcctNo()
         String sChequeRunNo = msf282Rec.getPrimaryKey().getChequeRunNo()
         String sHandleCde = msf282Rec.getPrimaryKey().getHandleCde()
         String sSuppToPay = msf282Rec.getPrimaryKey().getSuppToPay()
         String sInvDstrct = msf282Rec.getPrimaryKey().getInvDstrct()
         String sOrdSupplierNo = msf282Rec.getPrimaryKey().getOrdSupplierNo()
         String sInvNo = msf282Rec.getPrimaryKey().getInvNo()
         String sPoNo = msf282Rec.getPoNo()
         String sSettlementDiscount = new DecimalFormat("##,##0.00").format(msf282Rec.getSdAmount()).toString()
         
         String sExtInvNo = sInvNo   // Default the external invoice number then change it later if
                                     // a replacement is found from the Invoice file MSF260
         String sLastInvItem
         
         /**
          * Determine PO No to use. The original ECB28B had some crazy logic in working out the PO No.
          * Cleaning up all the fuzzy logic it appears that the PO Number will be:
          * 1) The default PO No (sPoNo) is from the Invoice Payments file MSF282.
          * 2) If populated, use the PO No from the Invoice file MSF260.
          * 3) If populated, use the PO No from the Invoice Item file MSF26A from the first item.
          */
         // Use PO No from Invoice File if there is one
         try {
             MSF260Key msf260key = new MSF260Key()
             msf260key.dstrctCode = sInvDstrct
             msf260key.supplierNo = sSuppToPay
             msf260key.invNo = sInvNo
             
             MSF260Rec msf260Rec = edoi.findByPrimaryKey(msf260key)
             
             // PO No in MSF260 is the first 6 characters of the Contract No
             // First make sure there are enough characters else the substring will fail with an index error
             if (msf260Rec.getContractNo().size() >= 6) {
                 // Check that the PO No filed is not just a field of 6 spaces
                 if (!(((msf260Rec.getContractNo().substring(0,6)).trim()).equals(""))) {
                     sPoNo = msf260Rec.getContractNo().substring(0,6)
                 }
             }
             // Change the External Invoice Number if the invoice record has one
             if (!(msf260Rec.getExtInvNo().trim().equals(""))) {
                 sExtInvNo = msf260Rec.getExtInvNo().toString()
             }
         } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
             // If the record is not found then just continue
         }
         
         // Constraints for the next two queries
         Constraint cDstrctCode = MSF26AKey.dstrctCode.equalTo(sInvDstrct)
         Constraint cSupplierNo = MSF26AKey.supplierNo.equalTo(sSuppToPay)
         Constraint cInvNo = MSF26AKey.invNo.equalTo(sInvNo)
         Constraint cInvItemNo = MSF26AKey.invItemNo.greaterThan("000")
         
         // Use PO No from the first Invoice Item
         def query1 = new QueryImpl(MSF26ARec.class).and(cDstrctCode).and(cSupplierNo).and(cInvNo).and(cInvItemNo)
         MSF26ARec msf26aRec1 = (MSF26ARec) edoi.firstRow(query1)
         if (!(msf26aRec1.getPoNo().trim().equals(""))) {
             sPoNo = msf26aRec1.getPoNo()
         }
         
         // Get the last Invoice Item
         def query2 = new QueryImpl(MSF26ARec.class).and(cDstrctCode).and(cSupplierNo).and(cInvNo).and(cInvItemNo)
         edoi.search(query2, {MSF26ARec msf26aRec2 ->
             sLastInvItem = msf26aRec2.getPrimaryKey().getInvItemNo()
         })
         
         // Get ADDIT Tax
         MSSATXLINK mssatxlink1 = eroi.execute('MSSATX', {MSSATXLINK mssatxlink1 ->
             mssatxlink1.optionAtx = "I"})
         
         MSSATXLINK mssatxlink2 = eroi.execute('MSSATX', {MSSATXLINK mssatxlink2 ->
             mssatxlink2.optionAtx = "X"
             mssatxlink2.dstrctCode = sInvDstrct
             mssatxlink2.supplierNo= sSuppToPay
             mssatxlink2.invNo = sInvNo
             mssatxlink2.invItemNo = sLastInvItem})
         
         // Abort if the Invoice has no ADDIT Tax
         if (!(mssatxlink2.getReturnStatus().trim().equals(""))) {
             info("#### ABORT: Invoice Header " + sInvNo + " has no ADDIT Tax ####")
             throw new Exception(">> ABORT: Invoice Header " + sInvNo + " has no ADDIT Tax <<\n")
         }
         
         // Print invoice remittance line
         prtInvoiceNumber = sExtInvNo
         prtOrderNumber = sPoNo
         prtSettlementDiscount = sSettlementDiscount
         if (mssatxlink2.getTotalTaxF() < 0) {
             prtCRIndicator = "CR"
         } else {
             prtCRIndicator = "  "
         }
         prtPrescribedPayment = new DecimalFormat("###,##0.00").format(mssatxlink2.getTotalTaxF()).toString()
         BigDecimal bdNetPayment = msf282Rec.getInvAmount() - (msf282Rec.getSdAmount() + msf282Rec.getPpAmount())
         prtNetPayment = new DecimalFormat("###,##0.00").format(bdNetPayment).toString()
         if (bdNetPayment < 0) {
             prtNetPaymentCRIndicator = "-"
         } else {
         prtNetPaymentCRIndicator = " "
         }
         // Finally write the remittance line to the output file
         writeRemAdvLine1()
     }
     
     /**
      * Get and write Invoice Comments to the output file
      * @param commentsKey - Standard Text Key
      * modified for SC0000004290790
     private void writeInvCommentsToFile(String commentsKey) {
         info("writeInvCommentsToFile")
         
         List <StdTextServiceGetTextReplyDTO> textReply = new ArrayList<StdTextServiceGetTextReplyDTO>()
         StdTextServiceGetTextReplyCollectionDTO textReplyCollection  =
             service.get("StdText").getText({it.stdTextId = commentsKey})
         textReply = textReplyCollection.getReplyElements()
         String sTempString = ""
         textReply.each {
             def tempLine = []
             tempLine = it.getTextLine()
             tempLine.each {
                 prtRemAdvComments = it
                 if (!(prtRemAdvComments.trim().equals(""))) {
                     writeRemAdvComments()
                 }
             }
         }
     }
      */
      
     /**V4 Start
      * Get and write Invoice Comments to the output file
      * @param msf282Rec - MSF282 record
      * @returns None      
      */
     private void writeInvCommentsToFile2(MSF282Rec msf282Rec) {
         info("writeInvCommentsToFile2 with input msf282rec")

          if(msf282Rec!=null){
          
          String ls_InvDstrct = msf282Rec.getPrimaryKey().getInvDstrct()
          String ls_SuppToPay = msf282Rec.getPrimaryKey().getSuppToPay()
          String ls_InvNo = msf282Rec.getPrimaryKey().getInvNo()
          String ls_CommentsKey = "IX" + ls_InvDstrct + ls_SuppToPay + ls_InvNo
         info("ls_CommentsKey:"+ls_CommentsKey)
         
         List <StdTextServiceGetTextReplyDTO> textReply = new ArrayList<StdTextServiceGetTextReplyDTO>()
         StdTextServiceGetTextReplyCollectionDTO textReplyCollection  =
             service.get("StdText").getText({it.stdTextId = ls_CommentsKey})
         textReply = textReplyCollection.getReplyElements()
         String sTempString = ""
         textReply.each {
             def tempLine = []
             tempLine = it.getTextLine()
             tempLine.each {
                 prtRemAdvComments = it
                 info(" prtRemAdvComments: "+prtRemAdvComments)
                 if (!(prtRemAdvComments.trim().equals(""))) {
                     writeRemAdvComments()
                 }
             }
         }
	     }
     }
     //V4 - End
     
     // Set Report A header
     private List setPageHeadingsA() {
         info("setPageHeadingsA")
         
         List <String> headings = new ArrayList <String>()
         //Trb28ba.writeLine(132,"-")
         headings.add("CHEQUE GENERATION - ERROR REPORT".center(132))
         //Trb28ba.writeLine(132,"-")
         
         return headings
     }
     
     //
     private void printEndOfReportA() {
         Trb28ba.write("----------------------------------------------------------" +
             " END OF REPORT " +
             "-----------------------------------------------------------\n")
     }
     
     // Set Report B header
     private List setPageHeadingsB() {
         info("setPageHeadingsB")
         
         List <String> headings = new ArrayList <String>()
         //Trb28bb.writeLine(132,"-")
         headings.add("CHEQUE GENERATION - CONTROL REPORT".center(132))
         //Trb28bb.writeLine(132,"-")
         
         return headings
     }
     
     private void printEndOfReportB() {
         Trb28bb.write("----------------------------------------------------------" +
             " END OF REPORT " +
             "-----------------------------------------------------------\n")
     }
     
     private void printControlReportSummary() {
         info("printControlReportSummary")
         
         prtTotalAmtOfCheques = "\$" + (new DecimalFormat("###,###,##0.00").format(totalAmtOfCheques)).toString()
         prtTotalNoOfCheques = (new DecimalFormat("####0").format(noOfChequesPrinted)).toString()
         prtFirstChequeNo = String.format("%06d", firstChequeNo)
         prtLastChequeNo = String.format("%06d", iNextChequeNumber)
         
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //For Cheque Run Number: XXXXXX                                                                          Branch:  XXXXXXXXXX
         Trb28bb.write("For Cheque Run Number: " + pRunNumber.padRight(6) + BLANK_CHAR.padRight(74) +
             "Branch:  " + BANK_BRANCH_CODE + "\n")
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                                                                                                       Account: XXXXXX
         Trb28bb.write(BLANK_CHAR.padRight(103) + "Account: " + BANK_ACCOUNT_CODE + "\n")
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                                         Total Cheques Printed Value:       $999,999,999.99
         Trb28bb.write(BLANK_CHAR.padRight(41) + "Total Cheques Printed Value:       " + prtTotalAmtOfCheques + "\n")
         //         1         2         3         4         5         6         7         8         9        10        11        12        13
         //123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                                         Total no. of Cheques Printed:      99999   First Cheque No: 999999, Last Cheque No: 999999
         Trb28bb.write(BLANK_CHAR.padRight(41) + "Total no. of Cheques Printed:      " + prtTotalNoOfCheques + "   First Cheque No: " +
             prtFirstChequeNo + ", Last Cheque No: " + prtLastChequeNo + "\n")
     }
     
     /**
      * Output file header
      */
     private void writeFileHeader() {
         outputFile.write("\n" + "\$XLP ENV=ECR28B2,END;" + "\n")
     }
      
     /**
      * Write on Cheque: Supplier Number and Bank Name
      */
     private void writeChequeLine1() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //1XXXXXX                                           WESTPAC
         outputFile.write(prtSection1.padRight(1) + prtSupplierNo.padRight(6) +
             BLANK_CHAR.padRight(43) + BANK_NAME + "\n")
         // Reset print fields
         prtSection1 = " "
         prtSupplierNo = " "
     }
     
     /**
      * Write on Cheque: Date
      */
     private void writeChequeLine2() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //                     DD-MM-YY
         outputFile.write(BLANK_CHAR.padRight(21) + prtDate1.padRight(8) + "\n")
         //Reset print fields
         prtDate1 = " "
     }
     
     /**
      * Write on Cheque: Amount in Words 1
      */
     private void writeChequeLine3() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(1) + prtChequeAmountWords1.padRight(79) + "\n")
         //Reset print fields
         prtChequeAmountWords1 = " "
     }
     
     /**
      * Write on Cheque: Amount in Words 2
      */
     private void writeChequeLine4() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         // XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(1) + prtChequeAmountWords2.padRight(79) + "\n")
         //Reset print fields
         prtChequeAmountWords2 = " "
     }
     
     /**
      * Write on Cheque: Cheque Date and Invoice Total
      */
     private void writeChequeLine5() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //    DD-MM-YY  999,999,999.99
         outputFile.write(BLANK_CHAR.padRight(4) + prtDate2.padRight(8) +
             BLANK_CHAR.padRight(2) + prtInvoiceTotal.padRight(14) + "\n")
         //Reset print fields
         prtDate2 = " "
         prtInvoiceTotal = " "
     }
     
     /**
      * Write on Cheque: Supplier Name
      */
     private void writeChequeLine6() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //          XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(10) + prtSupplierName.padRight(35) + "\n")
         //Reset print fields
         prtSupplierName = " "
     }
     
     /**
      * Write on Cheque: Supplier Address 1
      */
     private void writeChequeLine7() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //          XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(10) + prtSupplierAddress1.padRight(35) + "\n")
         //Reset print fields
         prtSupplierAddress1 = " "
     }
    
     /**
      * Write on Cheque: Supplier Address 2
      */
     private void writeChequeLine8() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //          XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(10) + prtSupplierAddress2.padRight(35) + "\n")
         //Reset print fields
         prtSupplierAddress2 = " "
     }
     
     /**
      * Write on Cheque: Supplier Address 3
      */
     private void writeChequeLine9() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //          XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(10) + prtSupplierAddress3.padRight(35) + "\n")
         //Reset print fields
         prtSupplierAddress3 = " "
     }
     
     /**
      * Write on Cheque: Supplier Address Post Code
      */
     private void writeChequeLine10() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //          9999
         outputFile.write(BLANK_CHAR.padRight(10) + prtSupplierPostCode.padRight(4) + "\n")
         //Reset print fields
         prtSupplierPostCode = " "
     }
     
     /**
      * Write on Cheque: Cheque Number
      */
     private void writeChequeLine11() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         // Cheque No 999999
         outputFile.write(" Cheque No".padRight(10) + BLANK_CHAR.padRight(1) +
             prtChequeNumber.padRight(6) + "\n")
         //Reset print fields
         prtChequeNumber = " "
     }
     
     /**
      * Write on Cheque: Cheque Number, BSB and Account NUmber
      */
     private void writeChequeLine12() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         // C999999 C999D999B 99D9999C
         outputFile.write(BLANK_CHAR.padRight(1) + prtChequeAndBank.padRight(26) + "\n")
         //Reset print fields
         prtChequeAndBank = " "
     }
     
     /**
      * Write on Remittance Advice: Cheque Number, BSB and Account Number
      */
     private void writeRemAdvLine1() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //2XXXXXXXXXXXXXXXXXXXX     XXXXXX          99,999.99 CR999,999.99 999,999,999.99-
         outputFile.write(prtSection2.padRight(1) + prtInvoiceNumber.padRight(20) +
             BLANK_CHAR.padRight(5) + prtOrderNumber.padRight(6) + BLANK_CHAR.padRight(10) +
             prtSettlementDiscount.padLeft(9) + BLANK_CHAR.padRight(1) +
             prtCRIndicator.padRight(2) + prtPrescribedPayment.padLeft(10) +
             BLANK_CHAR.padRight(1) + prtNetPayment.padLeft(14) + prtNetPaymentCRIndicator.padRight(1) + "\n")
         //Reset print fields
         prtSection2 = " "
         prtInvoiceNumber = " "
         prtOrderNumber = " "
         prtSettlementDiscount = " "
         prtCRIndicator = " "
         prtPrescribedPayment = " "
         prtNetPayment = " "
         prtNetPaymentCRIndicator = " "
     }
    
     /**
      * Write on Remittance Advice: Comments
      */
     private void writeRemAdvComments() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //  XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
         outputFile.write(BLANK_CHAR.padRight(2) + prtRemAdvComments.padRight(38) + "\n")
         //Reset print fields
         prtRemAdvComments = " "
     }
    
     /**
      * Write on Remittance Advice: Total Cheque value
      */
     private void writeRemAdvTotal() {
         //         1         2         3         4         5         6         7         8
         //1234567890123456789012345678901234567890123456789012345678901234567890123456789012
         //3TOTAL                                                           999,999,999.99
         outputFile.write(prtSection3.padRight(1) + "TOTAL".padRight(5) +
             BLANK_CHAR.padRight(59) + prtChequeTotalValue.padLeft(14) + "\n")
         //Reset print fields
         prtSection3 = " "
         prtChequeTotalValue = " "
     }
     
     /**
      * Write blank line
      */
     private void writeBlankLine() {
         outputFile.write("\n")
     }
     
     /**
      * Update #CQW table with the last Cheque Number used in this run
      */
     private void updateCQW() {
         info ("updateCQW")
         try {
             TableType sTableType = new TableType()
             TableCode sTableCode = new TableCode()
             AssocRec sAssocRec = new AssocRec()
             
             sTableType.setValue('#CQW')
             sTableCode.setValue('01')
             sAssocRec.setValue(pRunNumber.toString().trim() + String.format("%06d", iNextChequeNumber))
             
             TableCodeServiceResult tcsReply = service.get("TableCode").update({
                TableCodeDTO tcDto ->
                    tcDto.tableType = sTableType
                    tcDto.tableCode = sTableCode
                    tcDto.associatedRecord = sAssocRec}, false)
                 
         } catch (EnterpriseServiceOperationException e){
             listErrors(e)
             info("##### ERROR: Failed to update #CQW Table  #####")
             info("##### PROCESS TERMINATED #####")
             Trb28ba.write(">> ERROR: Failed to update #CQW Table. PROCESS TERMINATED <<\n")
             bErrorOccured = true
         }
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
ProcessTrb28b process = new ProcessTrb28b()
process.runBatch(binding)

