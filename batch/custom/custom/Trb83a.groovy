/**
 *  @Ventyx 2012
 *
 * This program will create a journal in Ellipse for each allowance entered with a work order.
 *
 * Developed based on <b>FDD.Online.Cost.Allowances.To.WO.V01.pdf</b>
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.text.SimpleDateFormat;

import org.slf4j.LoggerFactory;

import com.mincom.ellipse.attribute.Attribute;
import com.mincom.ellipse.edoi.ejb.msf081.*;
import com.mincom.ellipse.edoi.ejb.msf08a.*;
import com.mincom.ellipse.edoi.ejb.msf0p5.MSF0P5Key;
import com.mincom.ellipse.edoi.ejb.msf0p5.MSF0P5Rec;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf80e.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf817.*;
import com.mincom.ellipse.edoi.ejb.msf874.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.ellipse.edoi.ejb.msf898.*;
import com.mincom.ellipse.edoi.ejb.msf89w.*;
import com.mincom.ellipse.edoi.ejb.msf900.*;
import com.mincom.ellipse.edoi.ejb.msfprt.*;
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.enterpriseservice.ellipse.timesheetallows.*;
import com.mincom.enterpriseservice.ellipse.workorder.*;
import com.mincom.enterpriseservice.exception.*;
import com.mincom.eql.impl.QueryImpl;

/**
 * Request Parameter for Trb83a.
 * <li><code>runUpdateMode</code>: Run Update Mode Y / N.</li>
 * <li><code>accountant</code>: Employee Id from the Accountant.</li>
 */
class ParamsTrb83a {
    String runUpdateMode, accountant
}

/**
 * Report line for Trb83a.
 * <li><code>employeeId</code>: Employee Id from the allowance entry.</li>
 * <li><code>trnDate</code>: Transaction date from the allowance entry.</li>
 * <li><code>payGroup</code>: Pay Group code from the allowance entry.</li>
 * <li><code>allowanceCode</code>: Earn Code from the allowance entry.</li>
 * <li><code>seqNo</code>: Sequence Number from the allowance entry.</li>
 * <li><code>msg</code>: Message (if any)</li>
 */
class ReportLineTrb83a {
    String employeeId, trnDate, payGroup, allowanceCode, seqNo, msg
    String workOrder, drCode, crCode, amount

    public String writeErrorDetail() {
        return String.format("%-10s   %-8s   %-3s        %-3s             %-6s    %-70s",
        employeeId.padRight(10).substring(0, 10),
        trnDate.padRight(8).substring(0, 8),
        payGroup.padRight(3).substring(0, 3),
        allowanceCode.padRight(3).substring(0, 3),
        seqNo.padRight(6).substring(0, 6),
        msg.padRight(70).substring(0, 70))
    }

    public String writeTransactionDetail() {
        return String.format("%-10s   %-8s   %-3s        %-3s             %-6s    %-10s       %-12s      %-12s      %-18s",
        employeeId.padRight(10).substring(0, 10),
        trnDate.padRight(8).substring(0, 8),
        payGroup.padRight(3).substring(0, 3),
        allowanceCode.padRight(3).substring(0, 3),
        seqNo.padRight(6).substring(0, 6),
        workOrder.padRight(10).substring(0, 10),
        drCode.padRight(12).substring(0, 12),
        crCode.padRight(12).substring(0, 12),
        amount.padRight(18).substring(0, 18))
    }
}

/**
 * Main Process of Trb83a.
 */
public class ProcessTrb83a extends SuperBatch {

    /*
     * Constants
     */
    private static final String REPORT_A_FILENAME       = "TRB83AA"
    private static final String REPORT_B_FILENAME       = "TRB83AB"
    private static final String DASHED_LINE             = String.format("%132s"," ").replace(' ', '-')
    private static final String TIMESHEETALLOWS_SERVICE = "TIMESHEETALLOWS"
    private static final String TABLE_SERVICE           = "TABLE"
    private static final String WORKORDER_SERVICE       = "WORKORDER"
    private static final String ALW_TABLE_CODE          = "#ALW"
    private static final String TRN_STATUS_APPR         = "APPR"
    private static final String TRN_STATUS_PAID         = "PAID"
    private static final String REV_STATUS_RPLD         = "RPLD"
    private static final String REV_STATUS_RVSD         = "RVSD"
    private static final String ALLOWANCE_WO_ATTR       = "ALLOWANCEWO"
    private static final String CREDIT_DATE_ATTR        = "CREDITDATE"
    private static final String CREDIT_TIME_ATTR        = "CREDITTIME"
    private static final String CREDIT_JNL_ATTR         = "CREDITJNL"
    private static final String DEBIT_DATE_ATTR         = "DEBITDATE"
    private static final String DEBIT_TIME_ATTR         = "DEBITTIME"
    private static final String DEBIT_JNL_ATTR          = "DEBITJNL"
    private static final String AWD_CODE                = "#***"
    private static final String CA_801_TYPE             = "CA"
    private static final String DSTRCT_CODE_GRID        = "GRID"
    private static final SimpleDateFormat TRN_DATE_FRMT = new SimpleDateFormat("yyyyMMdd")
    private static final SimpleDateFormat TRN_TIME_FRMT = new SimpleDateFormat("HHmmss")
    private static final SimpleDateFormat FRM_TIME_FRMT = new SimpleDateFormat("HHmm")
    private static final String TIMESHEET_ALLOWS_ENTITY_TYPE = "TimesheetAllowsService.TimesheetAllows."


    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 5

    /*
     * Variables
     */
    private ParamsTrb83a batchParams
    private boolean runUpdate
    private def reportAWriter, reportBWriter
    private int recRead, crProcessed, dbProcessed, recError, recSkipped, recUpdated, crCount, dbCount
    private BigDecimal dbAmmount, crAmmount
    private ArrayList<String> paramsError
    private ArrayList<ReportLineTrb83a> reportALines, reportBLines
    private Calendar todayBatchCalendar = Calendar.getInstance()

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : " + version)
        batchParams = params.fill(new ParamsTrb83a())
        info("Run Update Mode : ${batchParams.runUpdateMode}")
        info("Accountant      : ${batchParams.accountant}")
        try {
            processBatch()
        } catch(Exception e) {
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB83A ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")
        } finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        initialize()
        if(validateReqParams()) {
            runUpdate = batchParams.runUpdateMode.equalsIgnoreCase("y")
            retrieve_APPR_Allowances()
            retrieve_PAID_RPLD_Allowances()
            retrieve_PAID_RVSD_Allowances()
        }
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        //write report
        printReportA()
        printReportB()
    }

    /**
     * Print the report A content.
     */
    private void printReportA() {
        info("printReportA")
        reportAWriter = report.open(REPORT_A_FILENAME)
        reportAWriter.write("${REPORT_A_FILENAME} Create Allowance Journals Control Report".center(132))
        reportAWriter.write(DASHED_LINE)
        reportAWriter.write("")
        reportAWriter.write("Summary Counts:")
        reportAWriter.write(String.format("\tNo. of Allowances Read: % 4d", recRead))
        reportAWriter.write(String.format("\tPrev Processed Credit : % 4d", crProcessed))
        reportAWriter.write(String.format("\tPrev Processed Debit  : % 4d", dbProcessed))
        reportAWriter.write(String.format("\tNo. with Errors       : % 4d", recError))
        reportAWriter.write(String.format("\tNo. skipped           : % 4d", recSkipped))
        reportAWriter.write(String.format("\tNo. updated           : % 4d", recUpdated))
        reportAWriter.write(String.format("\tCredit Jnls Created   : % 4d", crCount))
        reportAWriter.write(String.format("\tDebit Jnls Created    : % 4d", dbCount))
        reportAWriter.write(String.format("\tCredit Amount         : %16.2f", crAmmount.toDouble()))
        reportAWriter.write(String.format("\tDebit Amount          : %16.2f", dbAmmount.toDouble()))
        reportAWriter.write("")
        reportAWriter.write(DASHED_LINE)
        if(!reportALines.isEmpty()) {
            List <String> headingsA = new ArrayList <String>()
            headingsA.add("Employee ID  Tran Date  Pay Group  Allowance Code  Seq No    Error/ Warning Message Column Hdg")
            reportAWriter.pageHeadings = headingsA
            reportAWriter.heading()
            reportALines.each {
                reportAWriter.write(it.writeErrorDetail())
            }
            reportAWriter.write(DASHED_LINE)
        }
        if(!paramsError.isEmpty()) {
            reportAWriter.write("")
            reportAWriter.write("Parameters Errors:")
            reportAWriter.write("------------------")
            paramsError.each {
                reportAWriter.write(it.toString())
            }
        }
        reportAWriter.close()
    }

    /**
     * Print the report B content.
     */
    private void printReportB() {
        info("printReportB")
        if(!reportBLines.isEmpty()) {
            reportBWriter = report.open(REPORT_B_FILENAME)
            reportBWriter.write("${REPORT_B_FILENAME} Created Transaction Control Report".center(132))
            reportBWriter.write(DASHED_LINE)
            List <String> headingsB = new ArrayList <String>()
            headingsB.add("Employee ID  Tran Date  Pay Group  Allowance Code  Seq No    Work Order       DR                CR                Amount")
            reportBWriter.pageHeadings = headingsB
            reportBWriter.heading()
            reportBLines.each {
                reportBWriter.write(it.writeTransactionDetail())
            }
            reportBWriter.write(DASHED_LINE)
            reportBWriter.close()
        }
    }

    /**
     * Initialize the report writer.
     */
    private void initialize() {
        info("initialize")
        recRead = crProcessed = dbProcessed = recError = recSkipped = crCount = dbCount = 0
        dbAmmount = crAmmount = 0.0
        reportALines = new ArrayList<ReportLineTrb83a>()
        reportBLines = new ArrayList<ReportLineTrb83a>()
        paramsError = new ArrayList<String>()
    }

    /**
     * Validate the request parameters.
     * @return true if: 
     * <li>The Run Update Mode request parameter is validated to be Y or N.</li>
     * <li>The Accountant request parameter is validated to be a valid employee.</li>
     */
    private boolean validateReqParams() {
        info("validateReqParams")
        boolean valid = true
        if(!batchParams.accountant?.trim()) {
            valid = false
            paramsError.add("ACCOUNTANT - INPUT REQUIRED")
        } else {
            if(readEmployee(batchParams.accountant.trim())) {
                MSF760Rec msf760Rec = readEmployeeDetail(batchParams.accountant.trim())
                if(!(msf760Rec && msf760Rec.getEmpStatus().trim().equalsIgnoreCase("A"))){
                    valid = false
                    paramsError.add("ACCOUNTANT \'${batchParams.accountant}\'- NOT ACTIVE EMPLOYEE")
                }
            } else {
                valid = false
                paramsError.add("ACCOUNTANT \'${batchParams.accountant}\'- INPUT DOES NOT EXIST")
            }
        }
        return valid
    }

    /**
     * Read Employee from MSF810 based on Employee Id.
     * @param empId Employee Id
     * @return MSF760Rec
     */
    private MSF760Rec readEmployeeDetail(String empId) {
        info("readEmployeeDetail ${empId}")
        MSF760Rec msf760Rec = null
        try{
            msf760Rec = edoi.findByPrimaryKey(new MSF760Key(employeeId:empId))
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            msf760Rec = null
        }
        return msf760Rec
    }

    /**
     * Read Employee from MSF810 based on Employee Id.
     * @param empId Employee Id
     * @return MSF810Rec
     */
    private MSF810Rec readEmployee(String empId) {
        info("readEmployee ${empId}")
        MSF810Rec msf810Rec = null
        try{
            msf810Rec = edoi.findByPrimaryKey(new MSF810Key(employeeId:empId))
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            msf810Rec = null
        }
        return msf810Rec
    }

    /**
     * Retrieve approved allowances.
     */
    private void retrieve_APPR_Allowances() {
        info("retrieve_APPR_Allowances")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                .and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_APPR))
                .and(MSF891Rec.reverseStatus.equalTo(" "))
                .and(MSF891Key.employeeId.equalTo(MSF89WKey.employeeId))
                .and(MSF891Key.payGroup.equalTo(MSF89WKey.payGroup))
                .and(MSF891Key.trnDate.equalTo(MSF89WKey.trnDate))
                .and(MSF891Key.seqNo.equalTo(MSF89WKey.seqNo))
                .and(MSF891Key.employeeId.greaterThanEqualTo(" "))
                .and(MSF891Key.payGroup.greaterThanEqualTo(" "))
                .and(MSF891Key.trnDate.greaterThanEqualTo(" "))
                .and(MSF891Key.seqNo.greaterThanEqualTo(" "))
        edoi.search(qMSF891) {
            MSF891Rec msf891Rec = it[0]
            MSF89WRec msf89wRec = it[1]
            processAlowance(msf891Rec, msf89wRec)
        }
    }

    /**
     * Retrieve PAID/RPLD allowances.
     */
    private void retrieve_PAID_RPLD_Allowances() {
        info("retrieve_PAID_RPLD_Allowances")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                .and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_PAID))
                .and(MSF891Rec.reverseStatus.equalTo(REV_STATUS_RPLD))
                .and(MSF891Key.employeeId.equalTo(MSF89WKey.employeeId))
                .and(MSF891Key.payGroup.equalTo(MSF89WKey.payGroup))
                .and(MSF891Key.trnDate.equalTo(MSF89WKey.trnDate))
                .and(MSF891Key.seqNo.equalTo(MSF89WKey.seqNo))
                .and(MSF891Key.employeeId.greaterThanEqualTo(" "))
                .and(MSF891Key.payGroup.greaterThanEqualTo(" "))
                .and(MSF891Key.trnDate.greaterThanEqualTo(" "))
                .and(MSF891Key.seqNo.greaterThanEqualTo(" "))
        edoi.search(qMSF891) {
            MSF891Rec msf891Rec = it[0]
            MSF89WRec msf89wRec = it[1]
            ArrayList<MSF891Rec> apprAllowances = retrieve_APPR_AllowancesForPayroll_2(msf891Rec)
            if(!apprAllowances.isEmpty()) {
                processAlowance(msf891Rec, msf89wRec)
            }
        }
    }

    /**
     * Retrieve PAID/RVSD allowances.
     */
    private void retrieve_PAID_RVSD_Allowances() {
        info("retrieve_PAID_RVSD_Allowances")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                .and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_PAID))
                .and(MSF891Rec.reverseStatus.equalTo(REV_STATUS_RVSD))
                .and(MSF891Key.employeeId.equalTo(MSF89WKey.employeeId))
                .and(MSF891Key.payGroup.equalTo(MSF89WKey.payGroup))
                .and(MSF891Key.trnDate.equalTo(MSF89WKey.trnDate))
                .and(MSF891Key.seqNo.equalTo(MSF89WKey.seqNo))
                .and(MSF891Key.employeeId.greaterThanEqualTo(" "))
                .and(MSF891Key.payGroup.greaterThanEqualTo(" "))
                .and(MSF891Key.trnDate.greaterThanEqualTo(" "))
                .and(MSF891Key.seqNo.greaterThanEqualTo(" "))
        edoi.search(qMSF891) {
            MSF891Rec msf891Rec = it[0]
            MSF89WRec msf89wRec = it[1]
            String payGroup = msf891Rec.getPrimaryKey().getPayGroup()
            String lastModDateTime = msf891Rec.getLastModDate().concat(msf891Rec.getLastModTime())
            String latestDateTime  = getLatestRunDateTimeHistory(payGroup)
            if(lastModDateTime.compareTo(latestDateTime) > 0) {
                processAlowance(msf891Rec, msf89wRec)
            }
        }
    }

    /**
     * Get the latest run date and time from the pay history based on pay group.
     * @param payGroup pay group
     * @return latest run date and time
     */
    private String getLatestRunDateTimeHistory(String payGroup) {
        info("getLatestRunDateTimeHistory ${payGroup}")
        String maxLastRunDate = ""
        QueryImpl qMSF817 = new QueryImpl(MSF817Rec.class)
                .and(MSF817Key.payGroup.equalTo(payGroup))
                .and(MSF817Key.invEndDate.greaterThanEqualTo(" "))
                .and(MSF817Key.payRunType.greaterThanEqualTo(" "))
                .and(MSF817Key.invRunNo.greaterThanEqualTo(" "))
        edoi.search(qMSF817) {MSF817Rec hist->
            String tmp = hist.getRunDate().concat(hist.getRunTime())
            if(tmp.compareTo(maxLastRunDate) > 0) {
                maxLastRunDate = tmp
            }
        }
        return maxLastRunDate
    }

    /**
     * Retrieve approved allowances based on MSF891Rec.
     * @param msf891Rec MSF891Rec
     * @return list of MSF891Rec
     */
    private ArrayList<MSF891Rec> retrieve_APPR_AllowancesForPayroll_2(MSF891Rec msf891Rec) {
        info("retrieve_APPR_AllowancesForPayroll_2")
        ArrayList<MSF891Rec> approvedAllowances = new ArrayList<MSF891Rec>()
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                .and(MSF891Key.employeeId.equalTo(MSF89WKey.employeeId))
                .and(MSF891Key.payGroup.equalTo(MSF89WKey.payGroup))
                .and(MSF891Key.trnDate.equalTo(MSF89WKey.trnDate))
                .and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_APPR))
                .and(MSF891Rec.reverseStatus.equalTo(" "))
                .and(MSF891Key.employeeId.equalTo(msf891Rec.getPrimaryKey().getEmployeeId()))
                .and(MSF891Key.payGroup.equalTo(msf891Rec.getPrimaryKey().getPayGroup()))
                .and(MSF891Key.trnDate.equalTo(msf891Rec.getPrimaryKey().getTrnDate()))

        edoi.search(qMSF891) {
            MSF891Rec o = it[0]
            approvedAllowances.add(o)
        }
        return approvedAllowances
    }


    /**
     * Retrieve approved allowances based on MSF891Rec.
     * @param msf891Rec MSF891Rec
     * @return list of MSF891Rec
     */
    private ArrayList<MSF891Rec> retrieve_APPR_AllowancesForPayroll(MSF891Rec msf891Rec) {
        info("retrieve_APPR_AllowancesForPayroll")
        ArrayList<MSF891Rec> approvedAllowances = new ArrayList<MSF891Rec>()
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                .and(MSF891Key.employeeId.equalTo(MSF89WKey.employeeId))
                .and(MSF891Key.payGroup.equalTo(MSF89WKey.payGroup))
                .and(MSF891Key.trnDate.equalTo(MSF89WKey.trnDate))
                .and(MSF891Key.seqNo.equalTo(MSF89WKey.seqNo))
                .and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_APPR))
                .and(MSF891Rec.reverseStatus.equalTo(" "))
                .and(MSF891Key.employeeId.equalTo(msf891Rec.getPrimaryKey().getEmployeeId()))
                .and(MSF891Key.payGroup.equalTo(msf891Rec.getPrimaryKey().getPayGroup()))
                .and(MSF891Key.trnDate.equalTo(msf891Rec.getPrimaryKey().getTrnDate()))

        edoi.search(qMSF891) {
            MSF891Rec o = it[0]
            approvedAllowances.add(o)
        }
        return approvedAllowances
    }

    /**
     * Check if a MSF819Rec is inside approved allowances.
     * @param apprAllowances list of approved allowances
     * @param msf891Rec MSF819Rec
     * @return true if exist in the approve allowances, false otherwise
     */
    private boolean containsApprovedAllowances(ArrayList<MSF891Rec> apprAllowances, MSF891Rec msf891Rec) {
        info("containsApprovedAllowances")
        for(MSF891Rec o : apprAllowances) {
            if(o.getPrimaryKey().getEmployeeId().equals(msf891Rec.getPrimaryKey().getEmployeeId())
            && o.getPrimaryKey().getPayGroup().equals(msf891Rec.getPrimaryKey().getPayGroup())
            && o.getPrimaryKey().getTrnDate().equals(msf891Rec.getPrimaryKey().getTrnDate())
            && o.getPrimaryKey().getSeqNo().equals(msf891Rec.getPrimaryKey().getSeqNo())) {
                return true
            }
        }
        return false
    }

    /**
     * Process the allowance.
     * @param msf891Rec MSF891Rec
     * @param msf89wRec MSF89WRec
     */
    private void processAlowance(MSF891Rec msf891Rec, MSF89WRec msf89wRec) {
        info("processAlowance")
        recRead++
        boolean creditTrn = msf891Rec.getTranApprStatus().equalsIgnoreCase(TRN_STATUS_APPR)
        TimesheetAllowsServiceReadReplyDTO tsAllowanceReplyDTO = readTimesheetAllowance(msf89wRec)
        if(tsAllowanceReplyDTO) {
            boolean processedAllowance = checkProcessedAllowance(creditTrn, tsAllowanceReplyDTO)
            if(!processedAllowance) {
                String earnCode = msf89wRec.getPrimaryKey().getEarnCode()
                boolean validAllowanceType = checkAllowanceType(earnCode)
                List<Attribute> custAttribs = tsAllowanceReplyDTO.getCustomAttributes()
                String allowanceWO = readValueFromCustomAttributes(custAttribs, ALLOWANCE_WO_ATTR)
                WorkOrderServiceReadReplyDTO woReplyDTO = readWorkOrder(allowanceWO?.trim())
                if(validAllowanceType) {
                    //If the Work Order is set and needs to be set then continue processing otherwise add one to the SKIPPED count.
                    if(woReplyDTO != null) {
                        //If a Allowance WO has been entered validate that the Work Order is open to costing.
                        if(validateWorkOrderCosting(woReplyDTO)) {
                            //Only perform the actual updates and create journal entry if the update mode Is set.
                            if(runUpdate) {
                                //calculate the ammount
                                BigDecimal ammount = 0.0
                                BigDecimal earningFactor
                                if(msf89wRec.getAmount() != null || msf89wRec.getUnits() != null) {
                                    earningFactor = readAllowanceEearningFactor(earnCode)
                                    if(earningFactor) {
                                        earningFactor = !creditTrn ? earningFactor * -1 : earningFactor
                                        if(msf89wRec.getAmount() != null && msf89wRec.getAmount() > 0) {
                                            ammount = msf89wRec.getAmount() * earningFactor
                                        } else if(msf89wRec.getUnits() != null && msf89wRec.getUnits() > 0) {
                                            ammount = msf89wRec.getUnits() * earningFactor
                                        }
                                    }
                                }
                                //Create Journal using screen service
                                JournalResultDTO reply = createJournalEntry(msf89wRec, allowanceWO, ammount, creditTrn)
                                if(reply?.error == null) {
                                    String createdJournalNo = reply?.journalNo != null ? reply?.journalNo?.trim() : " "
                                    //check the created journal, if it is exist increment the counter
                                    ReportLineTrb83a transaction = retrieveManualJournalTransaction(createdJournalNo)
                                    if(transaction) {
                                        transaction.employeeId    = msf89wRec.getPrimaryKey().getEmployeeId()
                                        transaction.trnDate       = convertDateFormat(msf89wRec.getPrimaryKey().getTrnDate())
                                        transaction.payGroup      = msf89wRec.getPrimaryKey().getPayGroup()
                                        transaction.allowanceCode = msf89wRec.getPrimaryKey().getEarnCode()
                                        transaction.seqNo         = msf89wRec.getPrimaryKey().getSeqNo()
                                        reportBLines.add(transaction)

                                        //Increment CREDIT or DEBIT count and CREDIT or DEBIT amount for the control report.
                                        if(creditTrn) {
                                            crCount++
                                            crAmmount += ammount
                                        }
                                        else {
                                            dbCount++
                                            dbAmmount += ammount
                                        }

                                        Exception e = modifyAllowanceAttribute(msf89wRec, createdJournalNo, creditTrn)
                                        if(e == null) {
                                            recUpdated++
                                        } else {
                                            //Increment the ERROR count and stop processing the record.
                                            recError++
                                            addToErrorReportLines(msf89wRec, "${e.getMessage()}.")
                                        }
                                    } else {
                                        //Increment the ERROR count and stop processing the record.
                                        recError++
                                        info("ERROR : Cannot Create Journal.")
                                        addToErrorReportLines(msf89wRec, "Cannot Create Journal.")
                                    }
                                } else {
                                    //Increment the ERROR count and stop processing the record.
                                    recError++
                                    info("ERROR : Cannot create Journal - Field ${reply.error.currentCursorField} : ${reply.error.currentCursorValue} - ${reply.error.errorCode} ${reply.error.errorMsg}.")
                                    addToErrorReportLines(msf89wRec, "Journal Error-\'${reply.error.currentCursorValue}\' : ${reply.error.errorCode} - ${reply.error.errorMsg}.")
                                }
                            }
                        } else {
                            //If the Work Order is closed to costing increment the ERROR count and stop processing the record.
                            recError++
                            addToErrorReportLines(msf89wRec, "Allowance WO \'${allowanceWO}\' is closed to costing.")
                        }
                    } else {
                        recSkipped++
                        addToErrorReportLines(msf89wRec, "Allowance WO Required.")
                    }
                } else {
                    recError++
                    addToErrorReportLines(msf89wRec, "Allowance Code \'${earnCode}\' does not exist.")
                }
            } else {
                //Add one to the previously processed CREDIT/DEBIT count. Stop processing the record.
                String msg
                if(creditTrn) {
                    crProcessed++
                    msg = "Credit Transaction has been processed."
                }
                else {
                    dbProcessed++
                    msg = "Debit Transaction has been processed."
                }
                addToErrorReportLines(msf89wRec, msg)
            }
        } else {
            //Increment the ERROR count and stop processing the record.
            recError++
            addToErrorReportLines(msf89wRec, "Timesheet Allowance does not exist.")
        }
    }

    /**
     * 6.3.3.4 Validate the Allowance has not been processed.
     * Validate the allowance against the custom attributes from TimesheetAllows service.
     * @param isCreditTrn is a credit transaction
     * @param tsAllowanceReplyDTO TimesheetAllowsServiceReadReplyDTO
     * @return true if allowance has been processed, false otherwise
     */
    private boolean checkProcessedAllowance(boolean isCreditTrn, TimesheetAllowsServiceReadReplyDTO tsAllowanceReplyDTO) {
        info("checkProcessedAllowance")
        boolean processedAllowance = true
        if(tsAllowanceReplyDTO) {
            List<Attribute> custAttribs = tsAllowanceReplyDTO.getCustomAttributes()
            def aDate = readValueFromCustomAttributes(custAttribs, isCreditTrn ? CREDIT_DATE_ATTR : DEBIT_DATE_ATTR)
            def aTime = readValueFromCustomAttributes(custAttribs, isCreditTrn ? CREDIT_TIME_ATTR : DEBIT_TIME_ATTR)
            def aJnl  = readValueFromCustomAttributes(custAttribs, isCreditTrn ? CREDIT_JNL_ATTR : DEBIT_JNL_ATTR)
            processedAllowance = aDate && aTime && aJnl
        }
        return processedAllowance
    }

    /**
     * Read Timesheet Allowance based on Allowance record.
     * @param msf89wRec Allowance record
     * @return Timesheet Allowance DTO
     */
    private TimesheetAllowsServiceReadReplyDTO readTimesheetAllowance(MSF89WRec msf89wRec) {
        info("readTimesheetAllowance")
        TimesheetAllowsServiceReadReplyDTO tsAllowanceReplyDTO = null
        try {
            TimesheetAllowsServiceReadRequestDTO tsAllowanceReadDTO =
                    new TimesheetAllowsServiceReadRequestDTO()
            tsAllowanceReadDTO.setEmployee(msf89wRec.getPrimaryKey().getEmployeeId())
            tsAllowanceReadDTO.setPayGroup(msf89wRec.getPrimaryKey().getPayGroup())
            tsAllowanceReadDTO.setAllowanceCode(msf89wRec.getPrimaryKey().getEarnCode())
            tsAllowanceReadDTO.setSequenceNo(msf89wRec.getPrimaryKey().getSeqNo())
            Calendar c = Calendar.getInstance()
            c.setTime(TRN_DATE_FRMT.parse(msf89wRec.getPrimaryKey().getTrnDate()))
            tsAllowanceReadDTO.setTranDate(c)

            TimesheetAllowsServiceReadRequiredAttributesDTO tsAllowanceReadAttr =
                    new TimesheetAllowsServiceReadRequiredAttributesDTO()
            tsAllowanceReadAttr.setReturnEmployee(true)
            tsAllowanceReadAttr.setReturnOriginalAllowanceCode(true)
            tsAllowanceReadAttr.setReturnAllowanceCode(true)
            tsAllowanceReadAttr.setReturnPayGroup(true)
            tsAllowanceReadAttr.setReturnSequenceNo(true)
            tsAllowanceReadAttr.setReturnTranDate(true)
            tsAllowanceReadAttr.setReturnOriginalStartTime(true)
            tsAllowanceReadAttr.setReturnStartTime(true)
            tsAllowanceReadAttr.setReturnStopTime(true)
            tsAllowanceReadAttr.setReturnAllowanceAmount(true)
            tsAllowanceReadAttr.setReturnAllowanceUnits(true)
            tsAllowanceReadAttr.setReturnLastModDate(true)
            tsAllowanceReadAttr.setReturnLastModTime(true)

            tsAllowanceReadAttr.requiredAttributes.put(ALLOWANCE_WO_ATTR, true)
            tsAllowanceReadAttr.requiredAttributes.put(CREDIT_DATE_ATTR, true)
            tsAllowanceReadAttr.requiredAttributes.put(CREDIT_TIME_ATTR, true)
            tsAllowanceReadAttr.requiredAttributes.put(CREDIT_JNL_ATTR, true)
            tsAllowanceReadAttr.requiredAttributes.put(DEBIT_DATE_ATTR, true)
            tsAllowanceReadAttr.requiredAttributes.put(DEBIT_TIME_ATTR, true)
            tsAllowanceReadAttr.requiredAttributes.put(DEBIT_JNL_ATTR, true)
            tsAllowanceReadDTO.setRequiredAttributes(tsAllowanceReadAttr)

            tsAllowanceReplyDTO = service.get(TIMESHEETALLOWS_SERVICE).read(tsAllowanceReadDTO)
        } catch(EnterpriseServiceOperationException serviceExc) {
            tsAllowanceReplyDTO = null
            logExceptionService(TIMESHEETALLOWS_SERVICE, serviceExc)
        }
        return tsAllowanceReplyDTO
    }


    /**
     * Modify custom attributes from an allowance.
     * @param msf89wRec allowance record
     * @param journalNo journal number
     * @param isCreditTrn allowance status
     * @return Exception if any
     */
    private Exception modifyAllowanceAttribute(MSF89WRec msf89wRec, String journalNo, boolean isCreditTrn) {
        info("modifyAllowanceAttribute")
        Exception ex = null
        try {
            writeCustomAttributes(msf89wRec, isCreditTrn ? CREDIT_DATE_ATTR : DEBIT_DATE_ATTR,
                    TRN_DATE_FRMT.format(todayBatchCalendar.getTime()))
            writeCustomAttributes(msf89wRec, isCreditTrn ? CREDIT_TIME_ATTR : DEBIT_TIME_ATTR,
                    TRN_TIME_FRMT.format(todayBatchCalendar.getTime()))
            writeCustomAttributes(msf89wRec, isCreditTrn ? CREDIT_JNL_ATTR : DEBIT_JNL_ATTR,
                    journalNo)
        } catch(Exception e) {
            ex = e
        }
        return ex
    }

    /**
     * Write custom attributes value for specified allowance.
     * @param msf89wRec allowance record
     * @param entityType custom attribute type
     * @param propertyValue custom attribute value
     */
    private void writeCustomAttributes(MSF89WRec msf89wRec, String entityType, String propertyValue) {
        info("writeCustomAttributes")
        StringBuilder entityKey = new StringBuilder()
        entityKey.append(msf89wRec.getPrimaryKey().getEmployeeId())
        entityKey.append(msf89wRec.getPrimaryKey().getEarnCode())
        entityKey.append(msf89wRec.getPrimaryKey().getFrmTime()?.substring(0,4))
        entityKey.append(msf89wRec.getPrimaryKey().getPayGroup())
        entityKey.append(msf89wRec.getPrimaryKey().getSeqNo())
        entityKey.append(msf89wRec.getPrimaryKey().getTrnDate())
        entityType = TIMESHEET_ALLOWS_ENTITY_TYPE.concat(entityType)

        MSF0P5Key msf0p5Key = new MSF0P5Key()
        MSF0P5Rec msf0p5Rec = new MSF0P5Rec()
        msf0p5Rec.setPrimaryKey(msf0p5Key)
        msf0p5Rec.setEntityKey(entityKey.toString())
        msf0p5Rec.setEntityType(entityType)
        msf0p5Rec.setPropertyValue(propertyValue)

        //Use edoi.create to store the custtom attributes since the TimesheetAllows does not allow
        //modification for PAID/RPLD and PAID/RVSD
        edoi.create(msf0p5Rec)
    }

    /**
     * Read the value from custom attributes specified by name.
     * @param custAttribs list of custom attributes
     * @param attrName attribute name
     * @return value from custom attributes
     */
    private String readValueFromCustomAttributes(List<Attribute> custAttribs, String attrName) {
        info("readValueFromTimesheetAllowanceAttribute")
        for(Attribute a : custAttribs) {
            if(a.getName().equals(attrName)) {
                return a.getValue()
            }
        }
        return null
    }

    /**
     * 6.3.3.5 Validate Allowance Type
     * Check allowance type from {@link #ALW_TABLE_CODE} table type.
     * @param allowanceCode allowance code to be checked
     * @return true if allowance code exist
     */
    private boolean checkAllowanceType(String allowanceCode) {
        info("checkAllowanceType ${allowanceCode}")
        return readTable(ALW_TABLE_CODE, allowanceCode) != null
    }

    /**
     * Read table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable ${tableType} - ${tableCode}")
        TableServiceReadReplyDTO tableReplyDTO = null
        try{
            TableServiceReadRequestDTO tableReadDTO = new TableServiceReadRequestDTO()
            tableReadDTO.setTableType(tableType)
            tableReadDTO.setTableCode(tableCode)
            tableReplyDTO = service.get(TABLE_SERVICE).read(tableReadDTO)
        }catch (EnterpriseServiceOperationException e){
            tableReplyDTO = null
            logExceptionService(TABLE_SERVICE, e)
        }
        return tableReplyDTO
    }

    /**
     * 6.3.3.6 Validate Work Order is not closed to costing
     * Validate Work Order Costing Date.
     * @param workOrder WorkOrderServiceReadReplyDTO
     * @return false:
     * <lli>If the current date is later than the date on the work order the Work Order is closed to costing.</li>
     * <lli>If set to true the Work Order is closed to costing.</li>
     */
    private boolean validateWorkOrderCosting(WorkOrderServiceReadReplyDTO woReplyDTO) {
        info("validateWorkOrderCosting")
        Date today = todayBatchCalendar.getTime()
        Date defaultMinDate = new com.mincom.ellipse.eboi.types.Date(" ").getDateValue()
        boolean finalCosts = woReplyDTO.getFinalCosts()
        Date closeCommitDt = woReplyDTO.getCloseCommitDate().getTime()
        boolean validDt = false
        if(!closeCommitDt.equals(defaultMinDate)) {
            validDt = closeCommitDt.after(today)
        } else {
            validDt = true
        }
        return validDt || !finalCosts
    }

    /**
     * Read WorkOrder based on Work Order number.
     * @param workOrder Work Order number
     * @return WorkOrderServiceReadReplyDTO
     */
    private WorkOrderServiceReadReplyDTO readWorkOrder(String workOrder) {
        info("readWorkOrder ${workOrder}")
        if(!workOrder?.trim()) {
            return null
        }
        WorkOrderServiceReadReplyDTO woReplyDTO = null
        try{
            WorkOrderServiceReadRequiredAttributesDTO woReqAttr = new WorkOrderServiceReadRequiredAttributesDTO()
            woReqAttr.setReturnFinalCosts(true)
            woReqAttr.setReturnAccountCode(true)
            woReqAttr.setReturnCloseCommitDate(true)
            woReqAttr.setReturnDistrictCode(true)
            woReqAttr.setReturnWorkOrder(true)
            woReqAttr.setReturnWorkOrderStatusM(true)

            WorkOrderDTO woDTO = new WorkOrderDTO(workOrder)
            WorkOrderServiceReadRequestDTO woReadDTO = new WorkOrderServiceReadRequestDTO()
            woReadDTO.setDistrictCode(DSTRCT_CODE_GRID)
            woReadDTO.setWorkOrder(woDTO)
            woReadDTO.setRequiredAttributes(woReqAttr)
            woReplyDTO = service.get(WORKORDER_SERVICE).read(woReadDTO)
        }catch (EnterpriseServiceOperationException e){
            woReplyDTO = null
            logExceptionService(WORKORDER_SERVICE, e)
        }
        return woReplyDTO
    }

    /**
     * Read position costing cost centre details for an employee.
     * @param empId employee id
     * @return costing cost centre 
     */
    private String readPositionHomeCostCentre(String empId) {
        info("readPositionHomeCostCentre ${empId}")
        QueryImpl qMSF874 = new QueryImpl(MSF874Rec.class)
                .and(MSF874Key.positionId.equalTo(MSF878Key.positionId))
                .and(MSF878Key.employeeId.equalTo(empId))
                .and(MSF878Key.primaryPos.equalTo("0"))
                .and(MSF878Key.posStopDate.lessThan("1899"))
        def columns = edoi.firstRow(qMSF874)
        MSF874Rec msf874Rec = columns[0]
        MSF878Rec msf878Rec = columns[1]
        return msf874Rec ? msf874Rec.getCostCentre() :  " "
    }

    /**
     * Read allowance's expense element based on the earn code.
     * @param earnCode allowance's earn code
     * @return allowance's expense element
     */
    private String readAllowanceExpenseElement(String earnCode) {
        info("readAllowanceExpenseElement ${earnCode}")
        String drExpEleCa = " "
        try{
            MSF801_CA_801Key msf801CAkey = new MSF801_CA_801Key()
            msf801CAkey.setCntlKeyRest("***${earnCode}")
            msf801CAkey.setCntlRecType(CA_801_TYPE)
            MSF801_CA_801Rec msf801R1rec = edoi.findByPrimaryKey(msf801CAkey)
            drExpEleCa = msf801R1rec.getDrExpEleCa()
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            drExpEleCa = " "
            info("MSF801_CA ${earnCode} does not exist.")
        }
        return drExpEleCa
    }

    /**
     * Read allowance's expense element factor on the earn code.
     * @param earnCode allowance's earn code
     * @return allowance's expense element factor
     */
    private BigDecimal readAllowanceEearningFactor(String earnCode) {
        info("readAllowanceEearningFactor ${earnCode}")
        BigDecimal allowanceFactor = null
        QueryImpl qMSF80E = new QueryImpl(MSF80ERec.class)
                .and(MSF80EKey.awardCode.equalTo(AWD_CODE))
                .and(MSF80EKey.earnCode.equalTo(earnCode))
                .min(MSF80EKey.inverseDate)
        String invDate = edoi.firstRow(qMSF80E)
        //Then find the MSF80ERec with smallest inv_end_date same as above
        if(invDate?.trim()) {
            try{
                MSF80EKey msf80eKey = new MSF80EKey()
                msf80eKey.setAwardCode(AWD_CODE)
                msf80eKey.setEarnCode(earnCode)
                msf80eKey.setInverseDate(invDate)
                MSF80ERec msf80eRec = edoi.findByPrimaryKey(msf80eKey)
                allowanceFactor = msf80eRec.getField1_9()
            } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                allowanceFactor = null
                info("MSF80E ${earnCode} does not exist.")
            }
        }
        return allowanceFactor
    }

    /**
     * Create new Journal Entry from the allowance.
     * @param msf89wRec allowance record
     * @param allowanceWO allowance Work Order Number
     * @param isCreditTrn is credit transaction
     * @return JournalResultDTO 
     */
    private JournalResultDTO createJournalEntry(MSF89WRec msf89wRec, String allowanceWO, BigDecimal ammount, boolean isCreditTrn) {
        info("createJournalEntry")
        String earnCode       = msf89wRec.getPrimaryKey().getEarnCode()
        
        //TODO remove before push
        if(earnCode?.trim().equals("452")) {
            return new JournalResultDTO()
        }
        
        String expenseElement = readAllowanceExpenseElement(earnCode)
        String tranDate       = convertDateFormat(msf89wRec.getPrimaryKey().getTrnDate())
        String empId          = msf89wRec.getPrimaryKey().getEmployeeId().trim()
        String costCentre     = readPositionHomeCostCentre(empId)

        JournalEntryDTO dto1 = new JournalEntryDTO()
        dto1.accountCode = ".${expenseElement}".toUpperCase()
        dto1.ammount = ammount ? ammount.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString() : "0.00"
        dto1.woProjectNo = allowanceWO.toUpperCase()
        dto1.woProjectIndicator = "W"
        JournalEntryDTO dto2 = new JournalEntryDTO()
        dto2.accountCode = costCentre.trim().concat(expenseElement).toUpperCase()
        dto2.ammount = ammount ? ammount.multiply(-1.0).setScale(2, BigDecimal.ROUND_HALF_EVEN).toString() : "0.00"
        dto2.woProjectNo = " "
        dto2.woProjectIndicator = " "
        ArrayList<JournalEntryDTO> entries = new ArrayList<JournalEntryDTO>()
        entries.add(dto1)
        entries.add(dto2)

        JournalDTO journal = new JournalDTO()
        journal.description = "All WO ${empId} ${earnCode} ${tranDate}"
        journal.accountant = batchParams.accountant
        journal.approvalStatus = "Y"
        journal.tranDate = TRN_DATE_FRMT.format(todayBatchCalendar.getTime())
        journal.entries = entries

        ScreenAppLibrary sl = new ScreenAppLibrary()
        return sl.createJournal(journal)
    }

    /**
     * Browse transaction based on manual journal voucher number.
     * @param journalNo manual journal voucher number
     */
    private ReportLineTrb83a retrieveManualJournalTransaction(String journalNo) {
        info("retrieveManualJournalTransaction ${journalNo}")
        ReportLineTrb83a transaction
        QueryImpl qTransaction = new QueryImpl(MSF900Rec.class)
                .and(MSF900Key.rec900Type.equalTo("M"))
                .and(MSF900Key.dstrctCode.equalTo("GRID"))
                .and(MSF900Key.processDate.equalTo(TRN_DATE_FRMT.format(todayBatchCalendar.getTime())))
                .and(MSF900Rec.tranType.equalTo("MPJ"))
                .and(MSF900Rec.manjnlVchr.equalTo(journalNo))
                .columns([
                    MSF900Key.transactionNo,
                    MSF900Rec.accountCode,
                    MSF900Rec.tranAmount,
                    MSF900Rec.workOrder
                ])
        edoi.search(qTransaction, {columns->
            String transactionNo = columns[0]
            String accountCode   = columns[1]
            BigDecimal trnAmnt   = columns[2]
            String workOrder     = columns[3]
            if(transaction == null) {
                transaction = new ReportLineTrb83a()
            }
            if(workOrder?.trim()) {
                transaction.workOrder = workOrder
                transaction.drCode = accountCode
                transaction.amount = trnAmnt
            } else {
                transaction.crCode = accountCode
            }
        })
        return transaction
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

    /**
     * Add new record line based on the allowance entry.
     * @param msf89wRec Allowances record
     * @param msg specified message
     */
    private void addToErrorReportLines(MSF89WRec msf89wRec, String msg) {
        info("addToErrorReportLines")
        ReportLineTrb83a line = new ReportLineTrb83a()
        line.employeeId    = msf89wRec.getPrimaryKey().getEmployeeId()
        line.trnDate       = convertDateFormat(msf89wRec.getPrimaryKey().getTrnDate())
        line.payGroup      = msf89wRec.getPrimaryKey().getPayGroup()
        line.allowanceCode = msf89wRec.getPrimaryKey().getEarnCode()
        line.seqNo         = msf89wRec.getPrimaryKey().getSeqNo()
        line.msg           = msg
        reportALines.add(line)
    }

    /**
     * Convert the date format with specified separator <code>(DD/MM/YY)</code>.
     * @param dateS date as a String
     * @return formatted date as a String
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat")
        if(dateS) {
            dateS = dateS.trim().padLeft(8).replace(" ", "0")
            return dateS.substring(6) + "/" + dateS.substring(4,6) + "/" + dateS.substring(2,4)
        }
        return ""
    }
}

/**
 * Run the script
 */
ProcessTrb83a process = new ProcessTrb83a()
process.runBatch(binding)