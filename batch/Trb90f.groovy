/**
 *@Ventyx 2012
 *This batch extract Fleet related transactions and put them to the output file TRT90F.
 *The transactions input parameters are setup in the #FLK table.
 * 
 */
package com.mincom.ellipse.script.custom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;



import groovy.lang.Binding;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import java.text.DecimalFormat;

import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf100.*;
import com.mincom.ellipse.edoi.ejb.msf140.*;
import com.mincom.ellipse.edoi.ejb.msf141.*;
import com.mincom.ellipse.edoi.ejb.msf200.*;
import com.mincom.ellipse.edoi.ejb.msf20a.*;
import com.mincom.ellipse.edoi.ejb.msf231.*;
import com.mincom.ellipse.edoi.ejb.msf232.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf900.*;
import com.mincom.enterpriseservice.exception.*;
import com.mincom.ellipse.edoi.common.exception.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesReplyDTO;
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.purchaseorder.PurchaseOrderServiceFetchPReqItemReplyDTO
import com.mincom.enterpriseservice.ellipse.purchaseorder.PurchaseOrderServiceFetchPReqItemRequestDTO;
import com.mincom.enterpriseservice.ellipse.purchaseorder.PurchaseOrderServiceFetchPReqItemRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequestDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequiredAttributesDTO;
import com.mincom.ellipse.types.m3101.instances.CatalogueDTO;
import com.mincom.ellipse.types.m3101.instances.CatalogueServiceResult;
import com.mincom.ellipse.types.m0000.instances.StockCode;



public class ParamsTrb90f {
    //List of Input Parameters
}

/**
 * @author diazdwi
 *
 */
public class ProcessTrb90f extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 5;

    private def     cntrlRpt;
    private Boolean bErrorReportOpen        = false;
    private Boolean bOutputFileOpen         = false;
    private BufferedWriter outputFile;
    private def ParamsTrb90f batchParams;
    private String  outputFilePath          = " ";
    private String  reportFilePath          = " ";
    private BigDecimal mpjTotal             = 0;
    private BigDecimal amountTotal          = 0;
    private Integer countTransactionRead    = 0;
    private Integer countTransactionSkipped = 0;
    private Integer countTransactionWritten = 0;
    private Integer countMPJWritten         = 0;
    private String[] ignoredTransactions;
    private String[] arrayFLA_A;
    private String[] arrayFLA_E;
    private String  sLastTranKey            = " ";
    
    private static final String SEPARATOR    = "^";
    private static final int REQUEST_REPLY_NUM = 20;
    private static final int RESTART_NUM = 100;
    private static final String BLANK = " ";
    private static final Integer MAX_RESULTS = 1000;


    private class OutputLineTrb90f{
        String fleetNumber    = " "; //4
        String fleetDivision  = " "; //2
        String respCode       = " "; //3
        String activityCentre = " "; //3
        String expElement     = " "; //3
        String reference      = " "; //28
        String tranDate       = " "; //10
        String payee          = " "; //30
        String description    = " "; //30
        BigDecimal quantity   = 0;   //10
        BigDecimal hours      = 0;   //10
        BigDecimal value      = 0;   //10
        
        public String getStringFromValue(BigDecimal inputValue) {
            DecimalFormat format = new DecimalFormat("######0.00");
            String[] tempString = format.format(inputValue).tokenize(".");
            String returnString;
            if(inputValue >= 0) {
                return ("+"+tempString[0]+tempString[1]).padLeft(10);
            }
            else {
                return (tempString[0]+tempString[1]).padLeft(10);
            }
        }
        
        public String toString() {

            String outputString =                fleetNumber.padRight(4);
                   outputString = outputString + fleetDivision.padRight(2);
                   outputString = outputString + respCode.padRight(3);
                   outputString = outputString + activityCentre.padRight(3);
                   outputString = outputString + expElement.padRight(3);
                   outputString = outputString + reference.padRight(28);
                   outputString = outputString + tranDate.padRight(10);
                   if(payee.length()>30) {
                       outputString = outputString + payee.substring(0, 30)
                   }
                   else {
                       outputString = outputString + payee.padRight(30);
                   }

                   if(description.length()>30) {
                       outputString = outputString + description.substring(0, 30)
                   }
                   else {
                       outputString = outputString + description.padRight(30);
                   }
                   
                   outputString = outputString + getStringFromValue(quantity);
                   outputString = outputString + getStringFromValue(hours);
                   outputString = outputString + getStringFromValue(value);
        }
    }
    

    public void runBatch(Binding b) {

        init(b)
        printSuperBatchVersion()
        info("runBatch Version : " + version)

        //PrintRequest Parameters
        //No parameters
        
        try {
            processBatch();
        } 
        finally {
            printBatchReport();
            closeFiles();
        }
    }


    //additional method - start from here.


    /**
     * This is the main process.
     */
    private void processBatch() {
        if(initialise()) {
            mainProcess(); 
        }
       
    }

    /**
     * This is the initialisation of the program. 
     * If all initialisation is okay, it will return true.
     * @return true if  all initialisation is okay
     */
    private Boolean initialise() {
        info("initialise");

        Boolean continueProcess = false;
        
        try {
            String fileLocation = "${env.workDir}/TRT90F";
            String taskUuid = getTaskUUID() ;
            if(taskUuid?.trim()){
                fileLocation = fileLocation + "." + taskUuid;
            }
            info("File location: ${fileLocation}");
            outputFile = this.openOutputFile(fileLocation);
        
            cntrlRpt = report.open("TRB90FA");
            cntrlRpt.write("TRB90FA Fleet Transactions Export Control Report".center(132));
            cntrlRpt.write(" ");
            cntrlRpt.write("".padLeft(132,"-"));
            cntrlRpt.write(" ");
            cntrlRpt.write("  Selection Criteria: ");
            bErrorReportOpen = true;
            
            setupIgnoredTransactions();
            setupFLAArrays()
            
            continueProcess = true;
        }
        catch(Exception e) {
            e.printStackTrace();    
        }
        finally {
            info("end initialise");
            return continueProcess;
        }
       
    }
    
    /**
     * Setup the ignoredTransactions array.
     */
    private void setupIgnoredTransactions() {
        info("setupIgnoredTransactions")
        List<TableServiceRetrieveCodesReplyDTO> tblReplyList =  retrieveTableCode("TR");
        ArrayList<String> ignoredTrans = new ArrayList<String>();
        
        info("Start for loop")
        for(retrReply in tblReplyList) {
            if((substring(retrReply.associatedRecord,2,3).equals("M") && (!substring(retrReply.tableCode,0,3).equals("MPJ")))||(substring(retrReply.associatedRecord,2,3).equals("D"))){
                ignoredTrans.add(this.substring(retrReply.tableCode,0,3));
            }
        }
        
        ignoredTransactions = ignoredTrans.toArray();
        
        info("end setupIgnoredTransactions")
    }
    
    /**
     * Setup the FLA arrays
     */
    private void setupFLAArrays() {
        info("setupFLAArrays")
        List<TableServiceRetrieveCodesReplyDTO> tblReplyList =  retrieveTableCode("#FLA");
        ArrayList<String> type_A_Arrays = new ArrayList<String>();
        ArrayList<String> type_E_Arrays = new ArrayList<String>();
        
        info("Start for loop setupIgnoredAccount")
        for(retrReply in tblReplyList) {
            if(substring(retrReply.tableCode,0,1).equals("A")) {
                type_A_Arrays.add(this.substring(retrReply.tableCode,1,4));
            }
            else {
                if(substring(retrReply.tableCode,0,1).equals("E")) {
                    type_E_Arrays.add(this.substring(retrReply.tableCode,1,4));
                }
            }

        }
        
        arrayFLA_A = type_A_Arrays.toArray();
        arrayFLA_E = type_E_Arrays.toArray();
        info("end setupFLAArrays")
    }
    
    /**
     * Main process.
     */
    private void mainProcess() {
        info("mainProcess");
        
        List<TableServiceRetrieveCodesReplyDTO> listFLKCode = retrieveTableCode("#FLK");
        
        for(flkEntry in listFLKCode){

            flkEntry.with{
                cntrlRpt.write(" ");
                cntrlRpt.write("                District Code          : " + tableCode);
                cntrlRpt.write("                Last Processed Date    : " + getFormattedDate(substring(description,0,8),false));
                cntrlRpt.write("                Last Processed Txn No  : " + substring(description,8,19));
                cntrlRpt.write("                Last Processed User No : " + substring(description,19,23));
                cntrlRpt.write("                Last Processed Type    : " + substring(description,23,24));
            }
            
            if (flkEntry!=null && flkEntry.description?.trim()) {
                sLastTranKey = " ";
                browseTransactions(flkEntry.tableCode,substring(flkEntry.description, 0, 8), substring(flkEntry.description, 8, 19), substring(flkEntry.description,24,28).toBigInteger(), getFleetDivision(flkEntry.tableCode));
                if(sLastTranKey?.trim()) {
                    updateTables(flkEntry.tableCode,"#FLK",sLastTranKey);
                }
            }
        }
        
        info("end mainProcess");
        
    }
    
    /**
     * Updates the FLK table description with the new key.
     * @param previousValues
     * @param newDescription
     */
    private void updateTables(String newTableCode, String newTableType, String newDescription) {
        info("updateFLKTables");
        
        MSF010Rec msf010Rec = readTableCode(newTableType, newTableCode, false)
        try{
            msf010Rec.setTableDesc(newDescription)
            edoi.update(msf010Rec)
        }
        catch(EDOIInfrastructureException e) {
            reportError(e, "FLK Table Description", "Update")
            info("Cannot update FLK table, error code: ${e.message}")
        }

        info("end updateFLKTables");
    }
    
    
    /**
     * Browse all transactions based on the transaction date and transaction no supplied.
     * At the end, it will return the last transaction key to be stored in the FLK table.
     * @param tranDate
     * @param tranNo
     * @param runNo
     * @return String
     */
    private void browseTransactions(String dstrctCode, String tranDate, String tranNo, BigInteger runNo, String fleetDivision) {
        info("browseTransaction");
        
        info("District: "+ dstrctCode);
        info("TranDate: "+ tranDate);
        info("TranNo  : "+ tranNo);
    
        Constraint cDistrict     = MSF900Key.dstrctCode.equalTo(dstrctCode);
        Constraint cTranDate     = MSF900Key.processDate.greaterThan(tranDate);
        Constraint cTranDateEqu  = MSF900Key.processDate.equalTo(tranDate);
        Constraint cTranNo       = MSF900Key.transactionNo.greaterThan(tranNo);

        
        QueryImpl query = new QueryImpl(MSF900Rec.class).and(cDistrict).and((cTranDate).or(cTranDateEqu.and(cTranNo))).orderBy(MSF900Rec.msf900Key);
        String nextRunNo = (runNo + 1).toString();
        edoi.search(query,MAX_RESULTS){MSF900Rec msf900Rec ->
            countTransactionRead++;
            msf900Rec.primaryKey.with{
                sLastTranKey = processDate.padRight(8)+transactionNo.padRight(11)+userno.padRight(4)+rec900Type.padRight(1)+nextRunNo.padRight(4);
            }
            if(!isIgnored(msf900Rec)) {
                processTransaction(msf900Rec, fleetDivision);
            }
            else{
                countTransactionSkipped++
            }
        }
        
        info("end browseTransaction");
        
    }
    
    /**
     * Return true if the transaction is to be ignored.
     * @param msf900Rec
     * @return Boolean
     */
    private Boolean isIgnored(MSF900Rec msf900Rec) {
        info("isIgnored");
        
        Boolean returnValue = (msf900Rec.tranType.trim() in ignoredTransactions)||substring(msf900Rec.accountCode,9,12) in arrayFLA_E;
        
        info("end isIgnored, returnValue: ${returnValue.toString()}")
        
        return returnValue
    }
    

    
    /**
     * Process the transaction found and write the required line.
     * Will also set the current transaction as last transaction if it is written.
     * @param msf900Rec
     * @param fleetDivision
     */
    private void processTransaction(MSF900Rec msf900Rec, String fleetDivision) {
        info("processTransaction");
        
        OutputLineTrb90f outputLine = new OutputLineTrb90f();
        
        if(msf900Rec.equipNo?.trim()) {
            EquipmentServiceReadReplyDTO equipment =  readEquipment(msf900Rec.equipNo);
            if(equipment!=null && equipment.equipmentClass?.trim().equals("FL")) {
                outputLine.fleetNumber = this.substring(equipment.equipmentRef,2,6)
            }
            else {
                if(!(this.substring(msf900Rec.accountCode,6,9) in arrayFLA_A)) {
                    countTransactionSkipped++;
                    info("end processTransaction A");
                    return;
                }
            }
        }
        else {
            if(!(this.substring(msf900Rec.accountCode,6,9) in arrayFLA_A)) {
                countTransactionSkipped++;
                info("end processTransaction B");
                return;
            }
        }
          
        outputLine.fleetDivision  = fleetDivision;
        outputLine.payee          = getPayee(msf900Rec);
        outputLine.description    = getDescription(msf900Rec);
        outputLine.quantity       = getQuantity(msf900Rec);

        msf900Rec.with{
            outputLine.respCode       = substring(accountCode,0,3);
            outputLine.activityCentre = substring(accountCode,6,9);
            outputLine.expElement     = substring(accountCode,9,12);
            
            primaryKey.with{
                outputLine.reference  = dstrctCode + processDate + transactionNo + userno +rec900Type;
            }
            
            outputLine.tranDate       = getFormattedDate(trndteRevsd,true);
            
            if(primaryKey.rec900Type.equals("L")) {
                outputLine.hours      = noOfHours;
            }
            
            outputLine.value          = tranAmount;
            
       
            outputFile.write(outputLine.toString()+"\r\n");
            
            countTransactionWritten++;
            amountTotal = amountTotal + tranAmount;
            if(msf900Rec.tranType.equals("MPJ")) {
                countMPJWritten++
                mpjTotal = mpjTotal + tranAmount;
            }

        }
        info("end processTransaction C");
    }
    
    
    /**
     * Return the date to the DD/MM/YYYY format.
     * If the revsdDate is true, it will format the date as a reversed date.
     * @param date
     * @param revsdDate
     * @return String
     */
    private String getFormattedDate(String date, Boolean revsdDate) {
        info("getFormattedDate");
        
        String actualDate = date;
        if(revsdDate) {
            actualDate = (99999999 - actualDate.toBigInteger()).toString();
        }
        String returnString = substring(actualDate,6,8)+"/"+substring(actualDate,4,6)+"/"+substring(actualDate,0,4);
        info("end getFormattedDate, returnValue: ${returnString}")
        return returnString;
    }
    
    
    /**
     * Return the payee based on the MSF900 supplied.
     * @param msf900Rec
     * @return
     */
    private String getPayee(MSF900Rec msf900Rec) {
        info("getPayee");
        
        String payee = " ";
        
        if(msf900Rec.primaryKey.rec900Type.equals("L")||msf900Rec.tranType.equals("LCR")) {
            EmployeeServiceReadReplyDTO employee = readEmployee(msf900Rec.employeeId);
            if(employee!=null && employee.employeeFormattedName?.trim()) {
                payee = substring(msf900Rec.employeeId,(msf900Rec.employeeId.length()-5),msf900Rec.employeeId.length()) + " " + employee.employeeFormattedName;
            }
        }
        else {      
            SupplierServiceReadReplyDTO supplierData;
            switch(msf900Rec.primaryKey.rec900Type) {
                case "M": 
                            payee = msf900Rec.manjnlVchr;
                            break;
                case "C":   
                            payee  = "NO SUPPLIER NAME";
                            if(msf900Rec.ordSupplier?.trim()||msf900Rec.pmtSupplier?.trim()) {
                                if(msf900Rec.ordSupplier?.trim()) {
                                    payee = msf900Rec.ordSupplier;
                                }
                                else {
                                    payee = msf900Rec.pmtSupplier;
                                }
                                supplierData = readSupplier(payee);
                                if(supplierData!=null && supplierData.supplierName?.trim()) {
                                    payee = supplierData.supplierName;
                                }
                            }
                            break;
                case "I": 
                            payee  = "NO SUPPLIER NAME";
                            if(msf900Rec.supplierNo?.trim()) {
                                payee = msf900Rec.supplierNo;
                                supplierData = readSupplier(payee);
                                if(supplierData!=null && supplierData.supplierName?.trim()) {
                                    payee = supplierData.supplierName;
                                }
                            }
                            break;
                              
            }

        }
        
        info("end getPayee, return value: ${payee}");
        return payee;

    }
    
    /**
     * Return the quantity based on the MSF900 supplied.
     * @param msf900Rec
     * @return
     */
    private BigDecimal getQuantity(MSF900Rec msf900Rec) {
        info("getQuantity")
        
        BigDecimal quantity = 0;

        msf900Rec.with{
            switch(primaryKey.rec900Type) {
                case "A": quantity = qtyAdjUoi;break;
                case "B": quantity = qtyAdjUoi;break;
                case "M": quantity = qtyAmount;break;
                case "P": quantity = qtyRcvUoi;break;
                case "Q": quantity = qtyAdjUoi;break;
                case "R": quantity = qtyRcvUoi;break;
                case "S": quantity = quantityIss;break;
            }
        }
        
        info("end getQuantity, return value: ${quantity.toString()}")
        return quantity;
    }
    
    /**
     * Return the description based on the MSF900 supplied.
     * @param msf900Rec
     * @return String
     */
    private String getDescription(MSF900Rec msf900Rec) {
        info("getDescription");
        
        String description = " ";

        switch(msf900Rec.primaryKey.rec900Type) {
            case "C":   
                        description = msf900Rec.descLine;
                        break;
            case "I":   
                        String subTranDesc = substring(msf900Rec.description,0,17);
                        if(subTranDesc.toUpperCase().contains("TOTAL")) {
                            description = substring(msf900Rec.invItemDesc,16,39) + " " + substring(msf900Rec.accountant,msf900Rec.accountant.length()-5,msf900Rec.accountant.length());
                        }
                        else {
                            description = msf900Rec.invItemDesc;
                        }
                        break;
            case "M":   
                        description = msf900Rec.journalDesc;
                        break;
        }
        
        if(msf900Rec.tranType in ["ISS", "ISI", "STR", "ISC", "SRD", "CRD", "ORD", "STO", "STA", "ADJ"]) {
            description = getItemName(msf900Rec.stockCode);
        }
        else {
            if(msf900Rec.tranType in ["PRD", "SVR", "PRC"]) {
                PurchaseOrderServiceFetchPReqItemReplyDTO pReq = readPReqItem(msf900Rec.primaryKey.dstrctCode, msf900Rec.preqNo, msf900Rec.preqItemNo);
                if(pReq!=null && pReq.itemDesc1?.trim()) {
                    description = pReq.itemDesc1;
                }
                else {
                    StdTextServiceGetTextReplyCollectionDTO extText = readStandardText("PR"+substring(msf900Rec.primaryKey.dstrctCode,0,4)+msf900Rec.preqNo+msf900Rec.preqItemNo);
                    if(extText!=null && extText.replyElements.size()>0) {
                        description = extText.replyElements[0].textLine.toString(); 
                    }
                }
            }
        }
        
        info("end getDescription, return value: ${description}");
        return description;
    }
    
    /**
     * Returns the fleet division which is the table code for an FLD table type entry which has processed district in the description and '1' as the third character.
     * If there is more than one, the returned table code is the first one found.
     * @return String.
     */
    private String getFleetDivision(String districtCode){
        info("getFleetDivision");
        
        List<TableServiceRetrieveCodesReplyDTO> tblReplyList =  retrieveTableCode("#FLD");
        
        String returnString = " "
        
        for(retrReply in tblReplyList) {
            if(retrReply.description.contains(districtCode) && substring(retrReply.tableCode,2,3).equals("1")) {
                returnString = substring(retrReply.tableCode,0,2);
                break
            }
        }
        
        info("end getFleetDivision, return value: ${returnString}");
        return returnString;
    }
    
    /**
     * This method reports the error occured to the error report.
     * It also writes the operation name and the key involved.
     * @param e
     * @param operationName
     * @param key
     */
    private void reportError(EnterpriseServiceOperationException e, String operationName) {
        info("reportError");
        for(retrErrors in e.errorMessages) {
            String code = (retrErrors.code.tokenize("."))[2].trim();
            cntrlRpt.write("  "+operationName.padRight(15)+" Error, Tran Key:"+sLastTranKey+"  "+retrErrors.fieldName.padRight(30)+"  "+code+" - "+retrErrors.message.padRight(80));
        }
        info("end reportError");
    }

    /**
     * This method reports the error occured to the error report.
     * It also writes the operation name and the key involved.
     * @param e
     * @param operationName
     * @param key
     */
    private void reportError(Exception e, String fieldName, String operationName) {
        info("reportError");
        cntrlRpt.write("  "+operationName.padRight(15)+" Error, Tran Key:"+sLastTranKey+"  "+fieldName.padRight(30)+"  "+e.message.padRight(80));
        info("end reportError");
    }

    
    /**
     * Close all opened output files and or reports.
     */
    private void closeFiles() {
        info("closeFiles");
        
        if(bOutputFileOpen) {
            outputFile.close();
        }

        if(bErrorReportOpen) {
            cntrlRpt.close();
        }
        info("end closeFiles");
    }
    
    /**
     * Return the required attributes for the standard text
     * @param <code>stdTextId</code>
     * @return StdTextServiceGetTextReplyDTO
     */
    private StdTextServiceGetTextReplyCollectionDTO readStandardText(String stdTextId) {
        info("readPurchaseOrderItem");
        StdTextServiceGetTextReplyCollectionDTO cmReplyDTO = null;
        try {
            StdTextServiceGetTextRequiredAttributesDTO reqAttrib = new StdTextServiceGetTextRequiredAttributesDTO();
            reqAttrib.returnTextLine = true;
            cmReplyDTO = service.get("StdText").getText({ StdTextServiceGetTextRequestDTO it ->
                it.requiredAttributes = reqAttrib;
                it.stdTextId          = stdTextId;
            }, false)

        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error at service ${serviceExc.getMessage()}")
            reportError(serviceExc, "Std Text Read")
        }
        info("end readPurchaseOrderItem");
        return cmReplyDTO;
    }
    
    /**
     * Return the required attributes for the purchase order
     * @param <code>districtCode</code>
     * @param <code>pReqNo</code>
     * @param <code>pReqItemNo</code>
     * @return PurchaseOrderServiceFetchPReqItemReplyDTO
     */
    private PurchaseOrderServiceFetchPReqItemReplyDTO readPReqItem(String districtCode, String pReqNo, String pReqItemNo) {
        info("readPurchaseOrderItem");
        PurchaseOrderServiceFetchPReqItemReplyDTO cmReplyDTO = null;
        try {
            MSF231Key key = new MSF231Key();
            key.setDstrctCode(districtCode);
            key.setPreqNo(pReqNo);
            key.setPreqItemNo(pReqItemNo);
            MSF231Rec rec = edoi.findByPrimaryKey(key);
            cmReplyDTO = new PurchaseOrderServiceFetchPReqItemReplyDTO()
            cmReplyDTO.setItemDesc1(rec.getItemDescx1());

        } catch (EDOIObjectNotFoundException  e) {
            info("Error at service ${e.getMessage()}")
            reportError(e,"Preq Item","PReq Item Read")
        }
        info("end readPurchaseOrderItem");
        return cmReplyDTO;
    }
    
    
    /**
     * Return the required attributes for the catalogue item.
     * @param <code>supplierNo</code>
     * @return CatalogueDTO
     */
    private String  getItemName(String stockCode) {
        info("getItemName");
  
        DstrctStockCodeResultDTO replyDto;
        String returnItemName = " ";
        try {
            MSF100Rec rec = edoi.findByPrimaryKey(new MSF100Key(stockCode));
            
            returnItemName = "${rec.getItemName().trim()} ;${rec.getDescLinex1().trim()} ;${rec.getDescLinex2().trim()} ;${rec.getDescLinex3().trim()} ;${rec.getDescLinex4().trim()}";
            if(returnItemName.size()>40) {
                returnItemName = returnItemName.substring(0, 40);
            }
        }
        catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info("Error at service ${e.getMessage()}")
            reportError(e,"Item Name", "Stock Code Read")
        }
        
        info("end getItemName");
        return returnItemName;

    }
    
    /**
     * Return the required attributes for the supplier
     * @param <code>supplierNo</code>
     * @return SupplierServiceReadReplyDTO
     */
    private SupplierServiceReadReplyDTO readSupplier(String supplierNo) {
        info("readSupplier");
        SupplierServiceReadReplyDTO cmReplyDTO = null;
        try {
            MSF200Rec msf200Rec = edoi.findByPrimaryKey(new MSF200Key(supplierNo));
            cmReplyDTO = new SupplierServiceReadReplyDTO()
            cmReplyDTO.setSupplierName(msf200Rec.getSupplierName());
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info("Error at service ${e.getMessage()}")
            reportError(e,"Supplier", "Supplier Read")
        }
        info("end readSupplier");
        return cmReplyDTO;
    }
    
    /**
     * Return the required attributes for the employee
     * @param <code>employeeId</code>
     * @return EmployeeServiceReadReplyDTO
     */
    private EmployeeServiceReadReplyDTO readEmployee(String employeeId) {
        info("readEmployee");
        EmployeeServiceReadReplyDTO cmReplyDTO = null;
        try {
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(employeeId));
            String formattedName = "${msf810Rec.surname.trim()}, ${msf810Rec.firstName.trim()}";
            if(!msf810Rec.secondName.trim().equals("")) {
                formattedName = "${formattedName} ${msf810Rec.secondName.trim()}";
            }
            if(!msf810Rec.thirdName.trim().equals("")) {
                formattedName = "${formattedName} ${msf810Rec.thirdName.trim()}";
            }
            cmReplyDTO = new EmployeeServiceReadReplyDTO();
            cmReplyDTO.setEmployeeFormattedName(formattedName);

        } catch (EDOIObjectNotFoundException e) {
            info("Error at service ${e.getMessage()}")
            reportError(e, "Employee", "Employee Read")
        }
        info("end readEmployee");
        return cmReplyDTO;
    }
    
    /**
     * Return the required attributes for the equipment
     * @param <code>equipmentNo</code>
     * @return EquipmentServiceReadReplyDTO
     */
    private EquipmentServiceReadReplyDTO readEquipment(String equipmentNo) {
        info("readEquipment");
        EquipmentServiceReadReplyDTO cmReplyDTO = null;
        try {
           EquipmentServiceReadRequiredAttributesDTO reqAttrib = new EquipmentServiceReadRequiredAttributesDTO();
           reqAttrib.returnEquipmentRef = reqAttrib.returnEquipmentClass = true;
           cmReplyDTO = service.get("Equipment").read({ EquipmentServiceReadRequestDTO it ->
               it.requiredAttributes = reqAttrib;
               it.equipmentNo        = equipmentNo;
           }, false)
       } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
           info("Error at service ${serviceExc.getMessage()}")
           reportError(serviceExc, "Equipment Read")
       }
        info("end readEquipment");
        return cmReplyDTO;
    }

    
    /**
     * Return the required attributes for the table with table type <code>tableType</code>
     * and table code <code>tableCode</code> 
     * @param <code>tableType</code>
     * @param <code>tableCode</code> 
     * @return MSF010Rec
     */
    private MSF010Rec readTableCode(String tableType, String tableCode, Boolean writeReport) {       
        info("readTable tableType ${tableType}, tableCode ${tableCode}")

        //Using edoi as the service causes problem
        
        MSF010Rec msf010Rec = null
        try {
            MSF010Key msf010Key = new MSF010Key()
            msf010Key.tableCode = tableCode
            msf010Key.tableType = tableType
            msf010Rec = edoi.findByPrimaryKey(msf010Key)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("Error retrieveing table with message ${e.getMessage()}")
            if(writeReport) {
                reportError(e, "Table Read")
            }
        }
        info("end readTable")
        return  msf010Rec
    }
    
    /**
     * Retrieves a list of all table entry where the type is tableType
     * @param tableType
     * @return List of TableServiceRetrieveCodesReplyDTO
     */
    private List retrieveTableCode(String tableType) {
        info("retrieveTableCode");
        List<TableServiceRetrieveCodesReplyDTO> cmReplyList = new ArrayList<TableServiceRetrieveCodesReplyDTO>();
        QueryImpl query = new QueryImpl(MSF010Rec.class).and(MSF010Key.tableType.equalTo(tableType)).orderBy(MSF010Rec.msf010Key);
        edoi.search(query,MAX_RESULTS) {MSF010Rec rec ->
            TableServiceRetrieveCodesReplyDTO replyDTO = new TableServiceRetrieveCodesReplyDTO();
            replyDTO.setTableCode(rec.primaryKey.tableCode);
            replyDTO.setAssociatedRecord(rec.assocRec);
            replyDTO.setDescription(rec.tableDesc);
            cmReplyList.add(replyDTO);
        }

        return cmReplyList
    }
    

    /**
     * Return the logical representation of the output file as the file location
     * If an output file with the same name exist, it will try to delete it first.
     * @param fileLocation
     * @return
     */
    private BufferedWriter openOutputFile(String fileLocation) {
        info("openOutputFile");

        //Delete existing output file
        bOutputFileOpen = true;
        File f = new File(fileLocation);
        if (f.exists()) {
            if (!f.delete()) {
                info("unable to delete ["+ f.getAbsolutePath() + "]");
                throw new RuntimeException("unable to delete ["+ f.getAbsolutePath() + "]");
            }
        }
        BufferedWriter returnBW = new BufferedWriter(new FileWriter(fileLocation))
        
        info("end openOutputFile");
        
        return returnBW;
    }
    
    /**
     * Perform a "safe" substring by using the pad. 
     * @param inputString
     * @param startIdx
     * @param endIdx
     * @return String
     */
    private String substring(String inputString, Integer startIdx, Integer endIdx) {
        info("substring");
        String procString = inputString!=null?inputString:" ";
        String returnValue = procString.padRight(endIdx+1).substring(startIdx, endIdx);
        
        info("substring returned: ${returnValue}")
        return returnValue
    }
    
    /**
     * Print the summary report.
     */
    private void printBatchReport() {
        info("printBatchReport");
        DecimalFormat displayFormat = new DecimalFormat("########0.00");
        cntrlRpt.write(" ");
        cntrlRpt.write("                No of Transactions Read    : " + countTransactionRead.toString().padLeft(6));
        cntrlRpt.write("                No of Transactions Skipped : " + countTransactionSkipped.toString().padLeft(6));
        cntrlRpt.write("                No of Transactions Written : " + countTransactionWritten.toString().padLeft(6));
        cntrlRpt.write("                No of MPJ Txn Written      : " + countMPJWritten.toString().padLeft(6));
        cntrlRpt.write("                Transaction MPJ Amount     : " + displayFormat.format(mpjTotal).padLeft(12));
        cntrlRpt.write("                Transaction Total Amount   : " + displayFormat.format(amountTotal).padLeft(12));
        info("end printBatchReport");
    }
    
}

/*run script*/
ProcessTrb90f process = new ProcessTrb90f();
process.runBatch(binding);