/**
 * @author Ventyx 2013
 *
 * Trb38h - Send email notification of Purchase Orders Items created to the
 *          nominated Team's group email address.
 */

package com.mincom.ellipse.script.custom

import com.mincom.batch.request.Request
import com.mincom.batch.script.*

import com.mincom.ellipse.edoi.ejb.msf100.*
import com.mincom.ellipse.edoi.ejb.msf200.*
import com.mincom.ellipse.edoi.ejb.msf220.*
import com.mincom.ellipse.edoi.ejb.msf221.*
import com.mincom.ellipse.edoi.ejb.msf231.*
import com.mincom.ellipse.edoi.ejb.msf810.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*

import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequestDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*

import com.mincom.eql.impl.*
import com.mincom.eql.*

import java.io.BufferedWriter
import java.io.File;
import java.text.DecimalFormat
import java.text.SimpleDateFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class ParamsTrb38h {
    // List of Input Parameters
    String paramTeamId
    String paramPriceCode1
    String paramPriceCode2
    String paramPriceCode3
    String paramPriceCode4
    String paramPriceCode5
    String paramPriceCode6
    String paramDays
}

public class ProcessTrb38h extends SuperBatch {
    /**
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 3
    private ParamsTrb38h batchParams
    
    // Globals
    private Integer orderCount = 0
    private String emailAddress = ""
    private String orderCreateDate = ""
    private String fileLocation = ""
    
    private File oFile  // CSV file
    private BufferedWriter outputFile
    
    // Constants
    private static final String SEPARATOR = ","
    private static final String CR = "\n"
    private static final String EMAIL_SUBJECT = "FX/Commodity Variable Purchase Order Report"
    private static final String EMAIL_BODY = "FX/COMM Variable Purchase Orders - Please find attached a Report detailing New Purchase Orders Created that are subject to FX/COMM."
    
    public void runBatch(Binding b) {
        init(b)
        
        printSuperBatchVersion()
        info("runBatch version : " + version)
        
        // Request Parameters
        batchParams = params.fill(new ParamsTrb38h())
        String pTeamId = batchParams.paramTeamId
        info("Team Id: " + pTeamId)
        
        List<String> pPriceCodes = []
        if (!batchParams.paramPriceCode1.trim().equals("") && !batchParams.paramPriceCode1.equals(null)) {
            pPriceCodes.add(batchParams.paramPriceCode1)
            info("Price Code: " + batchParams.paramPriceCode1)
        }
        if (!batchParams.paramPriceCode2.trim().equals("") && !batchParams.paramPriceCode2.equals(null)) {
            pPriceCodes.add(batchParams.paramPriceCode2)
            info("Price Code: " + batchParams.paramPriceCode2)
        }
        if (!batchParams.paramPriceCode3.trim().equals("") && !batchParams.paramPriceCode3.equals(null)) {
            pPriceCodes.add(batchParams.paramPriceCode3)
            info("Price Code: " + batchParams.paramPriceCode3)
        }
        if (!batchParams.paramPriceCode4.trim().equals("") && !batchParams.paramPriceCode4.equals(null)) {
            pPriceCodes.add(batchParams.paramPriceCode4)
            info("Price Code: " + batchParams.paramPriceCode4)
        }
        if (!batchParams.paramPriceCode5.trim().equals("") && !batchParams.paramPriceCode5.equals(null)) {
            pPriceCodes.add(batchParams.paramPriceCode5)
            info("Price Code: " + batchParams.paramPriceCode5)
        }
        if (!batchParams.paramPriceCode6.trim().equals("") && !batchParams.paramPriceCode6.equals(null)) {
            pPriceCodes.add(batchParams.paramPriceCode6)
            info("Price Code: " + batchParams.paramPriceCode6)
        }
        
        String pDays = batchParams.paramDays
        info("Number of days before today: " + pDays)
        
        // output file name
        fileLocation = env.workDir.toString() + "/TRB38H." + getTaskUUID() + ".csv"
        info("CSV file: " + fileLocation)
        
        // Process request parameters
        if (!processParams(pTeamId, pPriceCodes, pDays)) {
            // Create the output CSV file
            if (!performInitialise()) {
                writeHeader()
                // Find the POs
                processBatch(pPriceCodes)
                // Email the file to team's group email address
                if (orderCount > 0) {
                    sendMessage()
                } else {
                    info("##### No purchase orders/items found #####")
                }
                // Copy output file to job folder so that it is viewable in MSE086
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
            info("##### ERROR: Unable to create output file #####")
            info("##### PROCESS TERMINATED #####")
            bErrorOccurred = true
        }
        
        return bErrorOccurred
    }
    
    /**
     * Validate request parameters
     * @param pTeamId, pDays
     * @return bErrorOccurred
     */
    private Boolean processParams(String pTeamId, List<String> pPriceCodes, String pDays) {
        info("processParams")
        
        Boolean bErrorOccurred = false
        
        // Calculate the Creation Date to use when searching through the PO Items
        Integer iDays = pDays.toInteger()
        
        if (iDays > 0) {
            orderCreateDate = (new Date() - iDays).format('yyyyMMdd')
        } else {
            orderCreateDate = new Date().format('yyyyMMdd')
        }
        info("Find purchase order items created on: " + orderCreateDate)
        
        // Validate Price Codes
        if (pPriceCodes.isEmpty()) {
            info("##### ERROR: At least once Price Code must be supplied #####")
            bErrorOccurred = true
        } else {
            pPriceCodes.each {String priceCode ->
                try {
                    TableServiceReadReplyDTO tablePcReply = service.get('Table').read({
                        it.tableType = 'PC'
                        it.tableCode = priceCode})
                } catch (EnterpriseServiceOperationException e){
                    listErrors(e)
                    info("##### ERROR: Price Code (" + priceCode + ") is invalid #####")
                    bErrorOccurred = true
                }
            }
        }
        
        // Validate the Team Id
        if (pTeamId.trim().equals("")) {
            // Team Id must be supplied
            info("##### ERROR: Team Id is Mandatory #####")
            bErrorOccurred = true
        } else {
            try {
                TableServiceReadReplyDTO tableEmlReply = service.get('Table').read({
                     it.tableType = '+EML'
                     it.tableCode = pTeamId})
                emailAddress = tableEmlReply.getDescription().trim()
            } catch (EnterpriseServiceOperationException e){
                listErrors(e)
                info("##### ERROR: Team (" + pTeamId + ") email address not found #####")
                bErrorOccurred = true
            }
        }
        
        // Error found
        if (bErrorOccurred) {
            info("##### PROCESS TERMINATED #####")
        }
        
        return bErrorOccurred
    }
    
    private void processBatch(List<String>pPriceCodes) {
        info("processBatch")
        
        String sPoNo = ""
        String sPoItemNo = ""
        String sPurchOfficer = ""
        String sOrderDate = ""
        String sSupplier = ""
        String sDistrictCode = ""
        String sWhouseId = ""
        String sPoItemType = ""
        String sPreqStockCode = ""
        String sItemName = ""
        String sCurrDueDate = ""
        String sCurrQty = ""
        String sCurrPrice = ""
        String sTotalPrice = ""
        
        // The PO and PO Item and PO webservices do not support searching via Creation Date.
        // Using EDOI.
        
        // Constraints
        Constraint c1 = MSF220Rec.creationDate.equalTo(orderCreateDate)
        
        // Query to PO & PO Items
        def query = new QueryImpl().
            join(MSF220Key.poNo, MSF221Key.poNo).
            columns([MSF221Key.poNo, MSF221Key.poItemNo, MSF220Rec.purchOfficer,
                MSF220Rec.orderDate, MSF220Rec.supplierNo, MSF221Rec.dstrctCode,
                MSF221Rec.whouseId, MSF221Rec.poItemType, MSF221Rec.preqStkCode,
                MSF221Rec.currDueDate, MSF221Rec.currQtyP, MSF221Rec.currNetPrP,
                MSF221Rec.priceCode]).
            and(c1).
            orderByAscending(MSF221Rec.msf221Key)
            
        try {
            // Get PO and PO Items created in the nominated date
            edoi.search(query).results.each {resultRec ->
                if (pPriceCodes.contains(resultRec[12])) {
                    if (!resultRec[0].toString().trim().equals("")) {
                        sPoNo = resultRec[0]
                    }
                    if (!resultRec[1].toString().trim().equals("")) {
                        sPoItemNo = resultRec[1]
                    }
                    if (!resultRec[2].toString().trim().equals("")) {
                        sPurchOfficer = getEmployeeName(resultRec[2])
                    }
                    if (!resultRec[3].toString().trim().equals("")) {
                        sOrderDate = new SimpleDateFormat("yyyyMMdd").parse(resultRec[3]).format("dd/MM/yyyy")
                    }
                    if (!resultRec[4].toString().trim().equals("")) {
                        sSupplier = getSupplierName(resultRec[4])
                    }
                    if (!resultRec[5].toString().trim().equals("")) {
                        sDistrictCode = resultRec[5]
                    }
                    if (!resultRec[6].toString().trim().equals("")) {
                        sWhouseId = resultRec[6]
                    }
                    if (!resultRec[7].toString().trim().equals("")) {
                        sPoItemType = resultRec[7]
                    }
                    if (!resultRec[8].toString().trim().equals("")) {
                        sPreqStockCode = resultRec[8]
                    }
                    if (!resultRec[9].toString().trim().equals("")) {
                        sCurrDueDate = new SimpleDateFormat("yyyyMMdd").parse(resultRec[9]).format("dd/MM/yyyy")
                    }
                    if (!resultRec[10].toString().trim().equals("")) {
                        sCurrQty = resultRec[10]
                    }
                    if (!resultRec[11].toString().trim().equals("")) {
                        sCurrPrice = new DecimalFormat("#0.00").format(resultRec[11])
                    }
                    if (!sCurrQty.trim().equals("") && !sCurrPrice.trim().equals("")) {
                        sTotalPrice = new DecimalFormat("#0.00").format(resultRec[10] * resultRec[11])
                    }
                    if (!sPoItemType.trim().equals("") &&
                        !sPreqStockCode.trim().equals("") &&
                        !sDistrictCode.trim().equals("")) {
                        sItemName = getItemName(sPoItemType, sPreqStockCode, sDistrictCode)
                    }
                    
                    // get standard text
                    String sPoHeadingComments = getStdText("NT2" + sPoNo)
                    String sPoNonPrintComments = getStdText("NT7" + sPoNo)
                    
                    // write to file
                    outputFile.write('"' + sPoNo + '"' + SEPARATOR +
                        '"' + sPoItemNo + '"' + SEPARATOR +
                        '"' + sWhouseId + '"' + SEPARATOR +
                        '"' + sPreqStockCode + '"' + SEPARATOR +
                        '"' + sItemName + '"' + SEPARATOR +
                        '"' + sSupplier + '"' + SEPARATOR +
                        '"' + sPurchOfficer + '"' + SEPARATOR +
                        '"' + sOrderDate + '"' + SEPARATOR +
                        '"' + sCurrDueDate + '"' + SEPARATOR +
                        '"' + sCurrQty + '"' + SEPARATOR +
                        '"' + sCurrPrice + '"' + SEPARATOR +
                        '"' + sTotalPrice + '"' + SEPARATOR +
                        '"' + sPoHeadingComments + '"' + SEPARATOR +
                        '"' + sPoNonPrintComments + '"' + CR)
                    
                    // PO Item found, increment counter
                    orderCount++
                }
            }
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            // No PO/PO Items found so just continue
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
        
        outputFile.write("Purchase Order" + SEPARATOR +
            "Purchase Order Item" + SEPARATOR +
            "Warehouse" + SEPARATOR +
            "Stockcode/PREQ" + SEPARATOR +
            "Item Name" + SEPARATOR +
            "Supplier Name" + SEPARATOR +
            "Purchasing Officer" + SEPARATOR +
            "Order Date" + SEPARATOR +
            "Due Date" + SEPARATOR +
            "Current Quantity" + SEPARATOR +
            "Current Price" + SEPARATOR +
            "Total Price" + SEPARATOR +
            "Exchange & Commodity Details" + SEPARATOR +
            "Narrative" + CR)
    }
    
    /**
     * Get Employee Formatted Name
     * @param sEmployeeId
     * @return sEmployeeName
     */
    private String getEmployeeName(String sEmployeeId) {
        info("getEmployeeName ${sEmployeeId}")
        
        String sEmployeeName = ""
        
        // Get Employee record
        try {
            // Cannot use the Employee Service since the user running the report may not have
            // sufficient access levels to use the service so using EDOI
            MSF810Key msf810key = new MSF810Key()
            msf810key.setEmployeeId(sEmployeeId)
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(msf810key)
            
            sEmployeeName = msf810Rec.getFirstName()
            if (!msf810Rec.getSecondName().trim().equals("") && !msf810Rec.getSecondName().trim().equals(null)) {
                sEmployeeName = sEmployeeName.trim() + " " + msf810Rec.getSecondName().trim()
            }
            if (!msf810Rec.getSurname().trim().equals("") && !msf810Rec.getSurname().trim().equals(null)) {
                sEmployeeName = sEmployeeName.trim() + " " + msf810Rec.getSurname().trim()
            }
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("##### Employee not found (" + sEmployeeId + ") #####")
        }
        
        return sEmployeeName
    }
    
    /**
     * Get Supplier Name
     * @param sSupplierNo
     * @return sSupplierName
     */
    private String getSupplierName(String sSupplierNo) {
        info("getSupplierName ${sSupplierNo}")
        
        String sSupplierName = ""
        
        // Get Supplier record
        try {
            // Cannot use the Supplier Service since the user running the report may not have
            // sufficient access levels to use the service so using EDOI
            MSF200Key msf200key = new MSF200Key()
            msf200key.setSupplierNo(sSupplierNo)
            MSF200Rec msf200Rec = edoi.findByPrimaryKey(msf200key)
            
            sSupplierName = msf200Rec.getSupplierName()
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("##### Supplier not found (" + sSupplierNo + ") #####")
        }
        
        return sSupplierName
    }
    
    /**
     * Get Stock Code or Purchase Requisition Item description
     * @param sPoItemType
     * @param sPreqStockCode
     * @param sDistrictCode
     * @return sItemName
     */
    private String getItemName(String sPoItemType, String sPreqStockCode, String sDistrictCode) {
        info("getItemName ${sPoItemType} ${sPreqStockCode} ${sDistrictCode}")
        
        String sItemName = ""
        
        if (sPoItemType == 'O' ||  // Owned Stock Item
            sPoItemType == 'C' ||  // Consignment Stock Item
            sPoItemType == 'D') {  // Direct Delivery
            try {
                MSF100Key msf100key = new MSF100Key()
                msf100key.setStockCode(sPreqStockCode)
                MSF100Rec msf100Rec = edoi.findByPrimaryKey(msf100key)
                
                sItemName = msf100Rec.getItemName()
            } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                info("##### Stock Code not found (" + sPreqStockCode + ") #####")
            }
        }
        
        if (sPoItemType == 'P' ||  // Purchase Requisition Item
            sPoItemType == 'S' ||  // Service Item
            sPoItemType == 'F') {  // Field Release Item
            try {
                MSF231Key msf231key = new MSF231Key()
                msf231key.setDstrctCode(sDistrictCode)
                msf231key.setPreqNo(sPreqStockCode.substring(0,6))
                msf231key.setPreqItemNo(sPreqStockCode.substring(6))
                MSF231Rec msf231Rec = edoi.findByPrimaryKey(msf231key)
                
                sItemName = msf231Rec.getItemDescx1()
            } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                info("##### Purchase Requisition Item not found (" + sPreqStockCode + ") #####")
            }
        }
        return sItemName
    }
    
    /**
     * Get Standard Text for the supplied key
     * @param sTextKey
     * @return sTempString
     */
    private String getStdText(String sTextKey) {
        info("getStdText ${sTextKey}")
        
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
     * Copy Output file to job folder that is viewable via MSE086
     */
    private void copyOutputFile() {
        info("copyOutputFile")
        
        if (oFile && getTaskUUID()?.trim()) {
            request.request.CURRENT.get().addOutput(oFile,
                "text/comma-separated-values", "TRB38H");
        }
    }
}

/*run script*/
ProcessTrb38h process = new ProcessTrb38h()
process.runBatch(binding)

