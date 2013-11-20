/**
 * @author Ventyx 2013
 *
 * Trb38g - Send email notification of contracts created to nominated Team's
 *          group email address.
 */

package com.mincom.ellipse.script.custom

import com.mincom.batch.request.Request
import com.mincom.batch.script.*

import com.mincom.ellipse.edoi.ejb.msf384.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*

import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.contract.ContractServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequestDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*

import com.mincom.eql.impl.*
import com.mincom.eql.*

import java.text.DecimalFormat
import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

 public class ParamsTrb38g {
     // List of Input Parameters
     String paramTeamId
     String paramDays
 }
 
 public class ProcessTrb38g extends SuperBatch {
     /**
      * IMPORTANT!
      * Update this Version number EVERY push to GIT
      */
     private version = 4
     private ParamsTrb38g batchParams
     
     // Globals
     private Integer contractCount = 0
     private String emailAddress = ""
     private String contractCreateDate = ""
     private String fileLocation = ""
     
     private File oFile  // CSV file
     private BufferedWriter outputFile
     
     // Constants
     private static final String SEPARATOR = ","
     private static final String CR = "\n"
     private static final String EMAIL_SUBJECT = "FX/COMM Contracts Created"
     private static final String EMAIL_BODY = "Please find attached a report detailing Contracts Created with Fx/Comm exposure."
     
     public void runBatch(Binding b) {
         init(b)
         
         printSuperBatchVersion()
         info("runBatch version : " + version)
         
         // Request Parameters
         batchParams = params.fill(new ParamsTrb38g())
         String pTeamId = batchParams.paramTeamId
         String pDays = batchParams.paramDays
         info("Team Id: " + pTeamId)
         info("Number of days before today: " + pDays)
         fileLocation = env.workDir.toString() + "/TRB38G." + getTaskUUID() + ".csv"
         info("CSV file: " + fileLocation)
         
         // Process request parameters
         if (!processParams(pTeamId, pDays)) {
             // Create the output CSV file
             if (!performInitialise()) {
                 writeHeader()
                 // Find the contracts
                 processBatch()
                 // Email the file to team's group email address
                 if (contractCount > 0) {
                     sendMessage()
                 } else {
                     info("##### No contracts found #####")
                 }
                 // Copy output file to batch job folder so it can be
                 // seen in MSE086
                 copyOutputFile()
             }
         }
     }
     
     /**
     * Initialisation - create files
     * @param None
     * @return None
     */
     private Boolean performInitialise() {
         info("performInitialise")
         
         Boolean bErrorOccurred = false
         
         // Create output file
         try {
             oFile = new File(fileLocation)
             outputFile = new BufferedWriter(new FileWriter(oFile))
        } catch (IOException e){
             // Error encountered during file creation
             e.printStackTrace()
             info ("##### ERROR: Unable to create output file #####")
             info ("##### PROCESS TERMINATED #####")
             bErrorOccurred = true
         }
         return bErrorOccurred
     }

     /**
      * Validate request parameters
      * @param pTeamId, pDays
      * @return TRUE/FALSE - success fail of parameters validation
      */
     private Boolean processParams(String pTeamId, String pDays) {
         info("processParams")
         
         Boolean bErrorOccurred = false
         
         // Calculate the Creation Date to use when searching through the Contracts
         Integer iDays = pDays.toInteger()
         
         if ((!pDays.equals("")) && (!pDays.equals(null)) && (iDays < 1)) {
             contractCreateDate = new Date().format('yyyyMMdd')
         } else {
             contractCreateDate = (new Date() - iDays).format('yyyyMMdd')
         }
         info("Find contracts created on: " + contractCreateDate)
         
         // Validate the Team Id
         if (pTeamId.trim().equals("")) {
             // Team Id must be supplied
             info("##### ERROR: Team Id is Mandatory #####")
             info("##### PROCESS TERMINATED #####")
             bErrorOccurred = true
         } else {
             try {
                 TableServiceReadReplyDTO tableEmlReply = service.get('Table').read({
                      it.tableType = '+EML'
                      it.tableCode = pTeamId})
                 emailAddress = tableEmlReply.getDescription().trim()
             } catch (EnterpriseServiceOperationException e){
                 listErrors(e)
                 info("##### ERROR: Team email not found #####")
                 info("##### PROCESS TERMINATED #####")
                 bErrorOccurred = true
             }
         }
         
         return bErrorOccurred
     }
     
     private void processBatch() {
         info("processBatch")
         
         // The Contracts webservice does not support searching via Creation Date.
         // Using EDOI.
         
         // Constraints
         Constraint c1 = MSF384Rec.creationDate.equalTo(contractCreateDate)
         // Query to contracts
         def query = new QueryImpl(MSF384Rec.class).and(c1).
         nonIndexSortAscending(MSF384Rec.loaDate)
         
         try {
             // Get contracts created on the nominated date.
             edoi.search(query, {MSF384Rec msf384Rec ->
                 String sContractNo = msf384Rec.getPrimaryKey().getContractNo()
                 
                 try {
                     // Get contract details using the Contract Service.
                     // Was originally using the EDOI results to fill in the output file
                     // and when it came to getting employee names was using the Employee
                     // Service. That did not work all the time since the user running this
                     // report may not have access to other Employee's details via the
                     // Employee service so the Names come out as blanks in the output file.
                     // In order to obtain all the descriptions without needing higher
                     // security levels to other services, use the Contract service and it
                     // can provide all the required descriptions such as Supplier Name
                     // and Employee Name.
                     ContractServiceReadReplyDTO contractReply = service.get('Contract').read({
                         it.contractNo = sContractNo})
                     
                     String sLoaDate = ""
                     if (!msf384Rec.getLoaDate().equals("") &&
                         !msf384Rec.getLoaDate().equals(null)) {
                         String sTempDate = msf384Rec.getLoaDate()
                         sLoaDate = sTempDate.substring(6,8) + "/" +
                                    sTempDate.substring(4,6) + "/" +
                                    sTempDate.substring(2,4)
                     }
                     String sContractDesc = contractReply.getContractDesc()
                     String sContractor = contractReply.getSupplierDesc()
                     String sContractAdmin = formatEmployeeName(contractReply.getRequestedByDesc())
                     String sProjectNo = contractReply.getProject()
                     String sFxComm = contractReply.getClassifCode4()
                     String sComments = getStdText("AD" + sContractNo)
                     String sProjectManager = formatEmployeeName(contractReply.getAuthorisedByDesc())
                     
                     // write to file
                     outputFile.write('"' + sLoaDate + '"' + SEPARATOR +
                         '"' + sContractNo + '"' + SEPARATOR +
                         '"' + sContractDesc + '"' + SEPARATOR +
                         '"' + sContractor + '"' + SEPARATOR +
                         '"' + sContractAdmin + '"' + SEPARATOR +
                         '"' + sFxComm + '"' + SEPARATOR +
                         '"' + sComments + '"' + SEPARATOR +
                         '"' + sProjectManager + '"' + CR)
                     
                     // contract found increment counter
                     contractCount++
                 } catch (EnterpriseServiceOperationException e){
                    listErrors(e)
                 }
             })
         } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
             // If the record is not found then just continue
             info("##### No contracts found #####")
         } finally {
             // Contracts have been written to the file (if any were found), close the file
             // before it is e-mailed
             outputFile.close()
         }
     }
     
     /**
      * Write file header
      */
     private void writeHeader() {
         info("writeHeader")
         
         outputFile.write("LOA Date" + SEPARATOR +
             "Contract No." + SEPARATOR +
             "Contract Description" + SEPARATOR +
             "Contractor" + SEPARATOR +
             "Contract Administrator" + SEPARATOR +
             "Foreign/Commodity" + SEPARATOR +
             "Contract Comments" + SEPARATOR +
             "Project Manager" + CR)
     }
     
     /**
      * Get Standard Text for the supplied key
      * @param sTextKey Standard Text Key
      * @return Standard Text for the key provided
      */
     private String getStdText(String sTextKey) {
         info("getStdText")
         
         List<String> textLines = []
         String sTempString = ""
         Integer sub = 0
         Integer lineCount = 0
         Integer SUB_MAX = 599
         
         List<StdTextServiceGetTextReplyDTO> textReply = new ArrayList<StdTextServiceGetTextReplyDTO>()
         try {
             //restart value
             def restart = ""
             StdTextServiceGetTextReplyCollectionDTO textReplyCollection =
                 service.get("StdText").getText({StdTextServiceGetTextRequestDTO it->
                     it.setStdTextId(sTextKey)},100, true, restart)
             restart = textReplyCollection.getCollectionRestartPoint()
             
             for (StdTextServiceGetTextReplyDTO stdTextDTO : textReplyCollection.getReplyElements()) {
                 for (String text : stdTextDTO.getTextLine()) {
                     textLines.add(text)
                     sub++
                     if (sub >= SUB_MAX) {
                         break
                     }
                 }
                 if (sub >= SUB_MAX) {
                     break
                 }
             }
             /*
              * restart value has been set from above service call, then do a loop while restart value is not blank
              * this loop below will get the rest of the rows
              */
             while (restart != null && restart.trim().length() > 0 && sub < SUB_MAX) {
                 textReplyCollection = service.get("StdText").getText({StdTextServiceGetTextRequestDTO it->
                     it.setStdTextId(sTextKey)},100, true, restart)
                 restart = textReplyCollection.getCollectionRestartPoint()
                 
                 for (StdTextServiceGetTextReplyDTO stdTextDTO : textReplyCollection.getReplyElements()) {
                     for (String text : stdTextDTO.getTextLine()) {
                         textLines.add(text)
                         sub++
                         if (sub >= SUB_MAX) {
                             break
                         }
                     }
                     if (sub >= SUB_MAX) {
                         break
                     }
                 }
             }
         } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
             info("Error during execute StdText.getText: ${serviceExc.getMessage()}")
         } catch (Exception e) {
             info("Unknown error during execute StdText.getText: ${e.getClass().toString()} ${e.getMessage()}")
         }
         
         // Get the contents of the list textLines and concatenate them into sTempString
         if (sub > 0) {
             lineCount = sub
             textLines.each {
                 if (lineCount > 1) {
                     sTempString = sTempString + it + "\n"
                     lineCount--
                 } else {
                     sTempString = sTempString + it
                 }
             }
         }
         return sTempString
     }
     
     /**
      * Name comes in the format "Lastname, Firstname". Reformat to "Firstname Lastname".
      * @param sEmployeeName
      * @return sFormattedName
      */
     private String formatEmployeeName(String sEmployeeName) {
         info("formatEmployeeName ${sEmployeeName}")
         
         String sFormattedName = ""
         
         if (!sEmployeeName.equals("") && !sEmployeeName.equals(null) && sEmployeeName.contains(",")) {
             List<String> sTempName = sEmployeeName.tokenize(",")
             sFormattedName = sTempName[1].trim() + " " + sTempName[0].trim()
         } else {
             sFormattedName = sEmployeeName
         }
         return sFormattedName
     }
     
     /**
      * Send email
      */
     private void sendMessage() {
         info("sendMessage")
         
         List<String> emailBody = []
         emailBody.add(EMAIL_BODY)
         
         SendEmail sendEmail = new SendEmail(EMAIL_SUBJECT, emailAddress, emailBody, fileLocation)
         sendEmail.sendMail()
     }
     
     /**
      * List all errors encountered to the log after running a web service
      * @param e Error Object
      */
     private void listErrors(EnterpriseServiceOperationException e) {
         List <ErrorMessageDTO> listError = e.getErrorMessages()
         listError.each{ErrorMessageDTO errorDTO ->
             info("Error Code: " + errorDTO.getCode())
             info("Error Message: " + errorDTO.getMessage())
             info("Error Fields: " + errorDTO.getFieldName())
         }
     }
     
     /**
      * Copy output CSV file into batch job folder so it is visible in MSE086
      */
     private void copyOutputFile() {
         info("copyOutputFile")
         
         if(oFile != null && taskUUID?.trim()) {
             request.request.CURRENT.get().addOutput(oFile,
                 "text/comma-separated-values", "TRB38G")
         }
     }
 }
 
/*run script*/
ProcessTrb38g process = new ProcessTrb38g()
process.runBatch(binding)


