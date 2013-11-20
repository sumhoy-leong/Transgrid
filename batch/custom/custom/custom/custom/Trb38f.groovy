/**
 * @Ventyx 2012
 *  
 * The program will generate the list of for any new Contract Claims (Valuations) </br>
 * loaded in MSE389 that have a Variable Element Code of EX or VR (from the EN Table File) </br>
 * on a daily basis.
 */
package com.mincom.ellipse.script.custom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.mincom.ellipse.edoi.ejb.msf38b.*;
import com.mincom.ellipse.edoi.ejb.msf384.*;
import com.mincom.ellipse.edoi.ejb.msf385.*;
import com.mincom.ellipse.edoi.ejb.msf386.*;
import com.mincom.ellipse.edoi.ejb.msf387.*;
import com.mincom.ellipse.edoi.ejb.msf388.*;
import com.mincom.ellipse.edoi.ejb.msf581.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.enterpriseservice.ellipse.contract.*;
import com.mincom.enterpriseservice.ellipse.contractitem.*;
import com.mincom.enterpriseservice.ellipse.stdtext.*;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.enterpriseservice.ellipse.valuations.*;
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException;

public class ParamsTrb38f{
    //List of Input Parameters
    String dayDiff, elementCode1, elementCode2, elementCode3
}

public class ProcessTrb38f extends SuperBatch {

    /**
     * Record line.
     */
    class Trb38fLine {
        String claimReceivedDate, contractNo, contractDesc, contractor,
        contractAdmin, invoiceNumber, projectManager, comments
        BigDecimal claimAmmount

        public String writeDetail() {
            String tmpClaimAmmt = currencyFormatter.format(claimAmmount)
            String tmpDate      = claimReceivedDate.padLeft(8).replace(" ", "0")

            StringBuilder tmpClaimDate = new StringBuilder()
            tmpClaimDate.append(tmpDate.substring(6))
            tmpClaimDate.append("/")
            tmpClaimDate.append(tmpDate.substring(4,6))
            tmpClaimDate.append("/")
            tmpClaimDate.append(tmpDate.substring(0, 4))

            return String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\","+
            "\"%s\",\"%s\",\"%s\",\"%s\"",
            tmpClaimDate.toString(),
            contractNo.padRight(8).substring(0,8),
            contractDesc.padRight(60),
            contractor.padRight(60).substring(0,60),
            contractAdmin.padRight(60).substring(0,60),
            "\$".concat(tmpClaimAmmt).padLeft(16).substring(0,16),
            invoiceNumber.padRight(20).substring(0,20),
            projectManager.padRight(60).substring(0,60),
            comments)
        }
    }

    /**
     * Record line comparator based on the date.
     */
    class Trb38fLineComparator implements Comparator<Trb38fLine> {
        @Override
        public int compare(Trb38fLine o1, Trb38fLine o2) {
            return o1.claimReceivedDate.compareTo(o2.claimReceivedDate)
        }
    }

    private static final DecimalFormat currencyFormatter
    static {
        currencyFormatter = new DecimalFormat("#,###,###,###.##")
        currencyFormatter.setMinimumFractionDigits(2)
    }
    private static final String REPORT_NAME           = "TRB38FA"
    private static final String STD_TEXT_SERVICE      = "STDTEXT"
    private static final String CONTRACT_SERVICE      = "CONTRACT"
    private static final String VALUATIONS_SERVICE    = "VALUATIONS"
    private static final String CONTRACT_ITEM_SERVICE = "CONTRACTITEM"
    private static final String SERVICE_NAME_TABLE    = "TABLE"
    private static final String EMAIL_TABLE_CODE      = "TRE"
    private static final String EMAIL_TABLE_TYPE      = "+EML"
    private static final int MAX_INSTANCE             = 20
    private static final int MAX_INT_CMMT_LINES       = 20

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 9
    private ParamsTrb38f batchParams
    private BufferedWriter reportWriter
    private File outputFile
    private ArrayList<Trb38fLine> records
    private ArrayList<String> elementCodes
    private Calendar todayDate

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version)

        batchParams = params.fill(new ParamsTrb38f())
        //PrintRequest Parameters
        info("Day Difference : " + batchParams.dayDiff)
        info("ElementCode 1  : " + batchParams.elementCode1)
        info("ElementCode 2  : " + batchParams.elementCode2)
        info("ElementCode 3  : " + batchParams.elementCode3)

        try {
            processBatch()
        } catch(Exception e) {
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB38F ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")
        }
        finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch")
        initialize()
        browseValuations()
    }

    /**
     * Initialize the variables.
     */
    private void initialize() {
        info("initialize")
        todayDate = Calendar.getInstance()
        records   = new ArrayList<Trb38fLine>()

        elementCodes = new ArrayList<String>()
        if(batchParams.elementCode1?.trim()) {
            elementCodes.add(batchParams.elementCode1.trim())
        }
        if(batchParams.elementCode2?.trim()) {
            elementCodes.add(batchParams.elementCode2.trim())
        }
        if(batchParams.elementCode3?.trim()) {
            elementCodes.add(batchParams.elementCode3.trim())
        }
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        writeReportOutput()

        if(reportWriter != null) {
            reportWriter.close()
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(outputFile,
                        "text/comma-separated-values", REPORT_NAME);
            }
            sendEmail()
        }
    }

    /**
     * Browse Valuations records.
     */
    private void browseValuations() {
        info("browseValuations")

        ValuationsServiceRetrieveRequiredAttributesDTO reqAtt =
                new ValuationsServiceRetrieveRequiredAttributesDTO()
        reqAtt.setReturnContractNo(true)
        reqAtt.setReturnContractNoDescription(true)
        reqAtt.setReturnCntrctrRefDate(true)
        reqAtt.setReturnCntrctrRefRcptDate(true)
        reqAtt.setReturnCntrctrRefAmt(true)
        reqAtt.setReturnCntrctrRefNo(true)
        reqAtt.setReturnContractor(true)
        reqAtt.setReturnContractorName(true)
        reqAtt.setReturnValuedBy(true)
        reqAtt.setReturnValuedByName(true)
        reqAtt.setReturnValuationNo(true)
        reqAtt.setReturnExtComment(true)
        reqAtt.setReturnIntComment(true)

        ValuationsServiceRetrieveRequestDTO retrieveReq =
                new ValuationsServiceRetrieveRequestDTO()
        retrieveReq.setNormalValn(false)
        retrieveReq.setWIPValn(false)
        retrieveReq.setLastValuation(false)
        retrieveReq.setFinalValn(false)
        retrieveReq.setSearchAllType(" ")

        def restart = ""
        boolean firstLoop = true
        while (firstLoop || restart?.trim()){
            ValuationsServiceRetrieveReplyCollectionDTO valuationReply =
                    retrieveValuation(retrieveReq, reqAtt, restart)
            firstLoop = false
            restart = valuationReply?.getCollectionRestartPoint()

            valuationReply?.getReplyElements().each { ValuationsServiceRetrieveReplyDTO valuation->
                processValuation(valuation)
            }
        }
    }

    /**
     * Process Valuation record.
     * @param valuation ValuationsServiceRetrieveReplyDTO
     */
    private void processValuation(ValuationsServiceRetrieveReplyDTO valuation) {
        info("processValuation")
        Date loadedDate = retrieveLoadedDate(valuation.getContractNo(),
                valuation.getValuationNo())

        long timeDiff = 86400000 * (batchParams.dayDiff as int)
        long dateDiff = todayDate.getTime().getTime() - loadedDate.getTime()

        if(dateDiff >= 0 && dateDiff <= timeDiff) {
            ContractServiceReadReplyDTO contract = readContract(valuation.getContractNo())
            if(contract && validateElementCode(contract?.getContractNo())) {
                Trb38fLine rec = new Trb38fLine()
                rec.contractNo        = contract.getContractNo()
                rec.contractDesc      = contract.getContractDesc()
                rec.claimReceivedDate = new java.text.SimpleDateFormat("yyyyMMdd").
                        format(valuation.getCntrctrRefRcptDate().getTime())
                rec.contractor        = valuation.getContractorName()
                rec.contractAdmin     = valuation.getValuedByName()
                rec.claimAmmount      = valuation.getCntrctrRefAmt()
                rec.invoiceNumber     = valuation.getCntrctrRefNo()
                rec.projectManager    = contract.getAuthorisedByDesc()
                rec.comments          = readInternalComments(valuation.getIntComment())
                records.add(rec)
            }
        }
    }

    /**
     * Retrieve Valuations records.
     * @param retrieveReq ValuationsServiceRetrieveRequestDTO
     * @param reqAtt ValuationsServiceRetrieveRequiredAttributesDTO
     * @param restart restart information
     * @return ValuationsServiceRetrieveReplyCollectionDTO
     */
    private ValuationsServiceRetrieveReplyCollectionDTO retrieveValuation(ValuationsServiceRetrieveRequestDTO retrieveReq,
    ValuationsServiceRetrieveRequiredAttributesDTO reqAtt, def restart){
        info("retriveValuation")
        try {
            return service.get(VALUATIONS_SERVICE).retrieve(retrieveReq, reqAtt, MAX_INSTANCE, restart)
        } catch(EnterpriseServiceOperationException ex) {
            logExceptionService("${VALUATIONS_SERVICE}.retrieve", ex)
            return null
        }
        return null
    }

    /**
     * Read contract based on contract number.
     * @param contractNo contract number
     * @return ContractServiceReadReplyDTO
     */
    private ContractServiceReadReplyDTO readContract(String contractNo) {
        info("readContract")
        try {
            ContractServiceReadRequiredAttributesDTO reqAtt =
                    new ContractServiceReadRequiredAttributesDTO()
            reqAtt.setReturnContractNo(true)
            reqAtt.setReturnContractDesc(true)
            reqAtt.setReturnAuthorisedBy(true)
            reqAtt.setReturnAuthorisedByDesc(true)
            reqAtt.setReturnExtendedText(true)

            ContractServiceReadRequestDTO readReq =
                    new ContractServiceReadRequestDTO()
            readReq.setContractNo(contractNo)
            readReq.setRequiredAttributes(reqAtt)

            return service.get(CONTRACT_SERVICE).read(readReq)
        } catch(EnterpriseServiceOperationException ex) {
            logExceptionService("${CONTRACT_SERVICE}.read", ex)
            return null
        }
        return null
    }

    /**
     * Retrieve ContractItem records.
     * @param retrieveReq ContractItemServiceRetrieveRequestDTO
     * @param reqAtt ContractItemServiceRetrieveRequiredAttributesDTO
     * @param restart restart information
     * @return ContractItemServiceRetrieveReplyCollectionDTO
     */
    private ContractItemServiceRetrieveReplyCollectionDTO retrieveContractItems(ContractItemServiceRetrieveRequestDTO retrieveReq,
    ContractItemServiceRetrieveRequiredAttributesDTO reqAtt, def restart){
        info("retrieveContractItems")
        try {
            return service.get(CONTRACT_ITEM_SERVICE).retrieve(retrieveReq, reqAtt, MAX_INSTANCE, restart)
        } catch(EnterpriseServiceOperationException ex) {
            logExceptionService("${CONTRACT_ITEM_SERVICE}.retrieve", ex)
            return null
        }
        return null
    }

    /**
     * Retrieve ContractItem Elements records.
     * @param retrieveReq ContractItemServiceRetrieveElementRequestDTO
     * @param reqAtt ContractItemServiceRetrieveElementRequiredAttributesDTO
     * @param restart restart information
     * @return ContractItemServiceRetrieveElementReplyCollectionDTO
     */
    private ContractItemServiceRetrieveElementReplyCollectionDTO retrieveContractItemElements(ContractItemServiceRetrieveElementRequestDTO retrieveReq,
    ContractItemServiceRetrieveElementRequiredAttributesDTO reqAtt, def restart){
        info("retrieveContractItemElements")
        try {
            return service.get(CONTRACT_ITEM_SERVICE).retrieveElement(retrieveReq, reqAtt, MAX_INSTANCE, restart)
        } catch(EnterpriseServiceOperationException ex) {
            logExceptionService("${CONTRACT_ITEM_SERVICE}.retrieveElement", ex)
            return null
        }
        return null
    }

    /**
     * Check and validate Element Code from a Contract.
     * @param contractNo Contract Number
     * @return true if the Element Code matches the request parameters.
     */
    private boolean validateElementCode(String contractNo) {
        info("validateElementCode")
        boolean validElementCode = false
        ContractItemServiceRetrieveRequiredAttributesDTO reqAtt =
                new ContractItemServiceRetrieveRequiredAttributesDTO()
        reqAtt.setReturnContractNo(true)
        reqAtt.setReturnContractVer(true)
        reqAtt.setReturnElementCode(true)
        reqAtt.setReturnPortion(true)
        reqAtt.setReturnCategoryCode(true)
        reqAtt.setReturnCategoryNo(true)

        ContractItemServiceRetrieveRequestDTO retrieveReq =
                new ContractItemServiceRetrieveRequestDTO()
        retrieveReq.setContractNo(contractNo)
        retrieveReq.setCopyToContract(contractNo)

        def restart = ""
        boolean firstLoop = true
        while (firstLoop || restart?.trim()){
            ContractItemServiceRetrieveReplyCollectionDTO contractItemReply =
                    retrieveContractItems(retrieveReq, reqAtt, restart)
            firstLoop = false
            restart = contractItemReply?.getCollectionRestartPoint()

            contractItemReply?.getReplyElements().each { ContractItemServiceRetrieveReplyDTO contractItem->
                if(checkElementCode(contractItem)) {
                    validElementCode = true
                    restart = ""
                }
            }
        }

        return validElementCode
    }

    /**
     * Check the element code from the contract item.
     * @param contractItem ContractItemServiceRetrieveReplyDTO
     * @return true if the Element Code matches the request parameters.
     */
    private boolean checkElementCode(ContractItemServiceRetrieveReplyDTO contractItem) {
        boolean validElementCode = false
        info("checkElementCode")
        ContractItemServiceRetrieveElementRequiredAttributesDTO reqAtt =
                new ContractItemServiceRetrieveElementRequiredAttributesDTO()
        reqAtt.setReturnContractNo(true)
        reqAtt.setReturnContractVer(true)
        reqAtt.setReturnElementCode(true)
        reqAtt.setReturnElementDescription(true)
        reqAtt.setReturnElementVal(true)
        reqAtt.setReturnItemType(true)
        reqAtt.setReturnPortion(true)

        ContractItemServiceRetrieveElementRequestDTO retrieveReq =
                new ContractItemServiceRetrieveElementRequestDTO()
        retrieveReq.setContractNo(contractItem.getContractNo())
        retrieveReq.setPortion(contractItem.getPortion())

        def restart = ""
        boolean firstLoop = true
        while (firstLoop || restart?.trim()){
            ContractItemServiceRetrieveElementReplyCollectionDTO contractItemElementReply =
                    retrieveContractItemElements(retrieveReq, reqAtt, restart)
            firstLoop = false
            restart = contractItemElementReply?.getCollectionRestartPoint()

            contractItemElementReply?.getReplyElements().each { ContractItemServiceRetrieveElementReplyDTO contractItemElement->
                if(elementCodes.contains(contractItemElement.getElementCode())) {
                    validElementCode = true
                    restart = ""
                }
            }
        }
        return validElementCode
    }

    /**
     * Read internal comments based on the standard text id.
     * @param stdTextId standard text id
     * @return internal comments as a String
     */
    private String readInternalComments(String stdTextId) {
        info("retrieveInternalComments")
        StringBuilder sb = new StringBuilder()

        StdTextServiceGetTextRequiredAttributesDTO reqAttr =
                new StdTextServiceGetTextRequiredAttributesDTO()
        reqAttr.setReturnLineCount(true)
        reqAttr.setReturnStartLineNo(true)
        reqAttr.setReturnTextLine(true)

        StdTextServiceGetTextRequestDTO getTextReq =
                new StdTextServiceGetTextRequestDTO()
        getTextReq.setStdTextId(stdTextId)

        def restart = ""
        boolean firstLoop = true
        int lineCount = 0
        while ((firstLoop || restart?.trim()) && lineCount < MAX_INT_CMMT_LINES){
            StdTextServiceGetTextReplyCollectionDTO stdTextReply =
                    retrieveStdText(getTextReq, reqAttr, restart)
            firstLoop = false
            restart = stdTextReply?.getCollectionRestartPoint()

            stdTextReply?.getReplyElements().each {StdTextServiceGetTextReplyDTO stdText->
                stdText.getTextLine().each {
                    sb.append(it)
                    sb.append(",")
                    lineCount++
                }
            }
        }
        return sb.toString()
    }

    /**
     * Retrieve Standard Text.
     * @param getTextReq StdTextServiceGetTextRequestDTO
     * @param reqAttr StdTextServiceGetTextRequiredAttributesDTO
     * @param restart restart information
     * @return StdTextServiceGetTextReplyCollectionDTO
     */
    private StdTextServiceGetTextReplyCollectionDTO retrieveStdText(StdTextServiceGetTextRequestDTO getTextReq,
    StdTextServiceGetTextRequiredAttributesDTO reqAttr, def restart) {
        info("retrieveStdText")
        try {
            return service.get(STD_TEXT_SERVICE).getText(getTextReq, reqAttr, MAX_INSTANCE, restart)
        } catch(EnterpriseServiceOperationException ex) {
            logExceptionService("${STD_TEXT_SERVICE}.getText", ex)
            return null
        }
        return null
    }

    /**
     * Retrieve the email addres for Treasury PIC.</br>
     * By default the email addres is Corporate.Finance@transgrid.com.au
     * @return email addres
     */
    private String retrieveEmailAddress() {
        info("retrieveEmailAddress")
        TableServiceReadReplyDTO dto = readTable(EMAIL_TABLE_TYPE, EMAIL_TABLE_CODE)
        if(dto?.getDescription()?.trim()) {
            return dto?.getDescription()?.trim()
        }
        return "Corporate.Finance@transgrid.com.au"
    }

    /**
     * Read Table by using service call.
     * @param tableType Table Type
     * @param tableCode Table Code
     * @return TableServiceReadReplyDTO
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable")
        try {
            TableServiceReadRequestDTO tableDTO = new TableServiceReadRequestDTO()
            tableDTO.setTableType(tableType)
            tableDTO.setTableCode(tableCode)

            return service.get(SERVICE_NAME_TABLE).read(tableDTO)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            logExceptionService("${SERVICE_NAME_TABLE}.read", serviceExc)
            return null
        }
    }

    /**
     * Send the email to Treasury PIC with the CSV attched.
     */
    private void sendEmail(){
        info ("sendEmail")

        String subject = "<< Ellipse - Contract Claim Loaded >>  "
        String mailFrom = " "
        String host = " "
        String mailTo = retrieveEmailAddress()
        ArrayList<String> message = new ArrayList()
        message.add("Please find TRB38FA report in attached file.")
        ArrayList<String> attachments = new ArrayList()
        attachments.add(outputFile.getPath())

        SendEmail myEmail = new SendEmail(subject, mailTo, message,
                attachments, mailFrom, host, false)
        myEmail.sendMail()

        if(myEmail.isError()){
            info("Error sending email : " + myEmail.getErrorMessage())
        }
    }

    /**
     * Retrieve loaded date from the Valuation Master.
     * @param contractNo contract number
     * @param valuationNo valuation number
     * @return MSF38B-LOADED-DATE
     */
    private Date retrieveLoadedDate(String contractNo, String valuationNo) {
        info("retrieveLoadedDate")
        MSF38BRec valRec = readValuationMaster(contractNo, valuationNo)
        if(valRec) {
            String date = valRec.getLoadedDate()?.trim()
            if(date?.trim()) {
                return new java.text.SimpleDateFormat("yyyyMMdd").parse(date)
            }
        }
        return new Date(Long.MIN_VALUE)
    }

    /**
     * Read Valuation Master record based on contract number and valuation number.
     * @param contractNo contract number
     * @param valuationNo valuation number
     * @return MSF38BRec
     */
    private MSF38BRec readValuationMaster(String contractNo, String valuationNo) {
        info("readValuationMaster")
        try {
            MSF38BKey valMasterKey = new MSF38BKey()
            valMasterKey.setContractNo(contractNo)
            valMasterKey.setValnNo(valuationNo)
            return edoi.findByPrimaryKey(valMasterKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF38BKey ${contractNo} ${valuationNo} not found!")
            return null
        }
    }

    /**
     * Write the processed records into report output.
     */
    private void writeReportOutput() {
        info("writeReportOutput")
        if(!records.isEmpty()) {
            //open the writer and file
            def workingDir  = env.workDir
            String csvPath  = "${workingDir}/${REPORT_NAME}"
            if(taskUUID?.trim()) {
                csvPath = csvPath + "." + taskUUID
            }
            csvPath   = csvPath + ".csv"
            outputFile = new File(csvPath)
            reportWriter = new BufferedWriter(new FileWriter(outputFile))
            info("${REPORT_NAME} created in ${csvPath}")
            //write header
            reportWriter.write("Claim Received Date,Contract No.,Contract Description," +
                    "Contractor,Contract Administrator,Exchange Adjustment due to Contractor,"+
                    "Invoice Number,Project Manager,Comments")
            //sort the records
            Collections.sort(records, new Trb38fLineComparator())
            //write the records
            records.each {
                reportWriter.write("\r\n")
                reportWriter.write(it.writeDetail())
            }
        }
    }

    /**
     * Log the EnterpriseServiceOperationException.
     * @param serviceName name of the executed service
     * @param serviceExc EnterpriseServiceOperationException
     */
    private void logExceptionService(String serviceName, EnterpriseServiceOperationException serviceExc) {
        info("logExceptionService")
        String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
        String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
        String errorField = serviceExc.getErrorMessages()[0].getFieldName()
        info("Error during execute ${serviceName} caused by ${errorField}: ${errorCode} - ${errorMsg}.")
    }
}

ProcessTrb38f process = new ProcessTrb38f();
process.runBatch(binding);
