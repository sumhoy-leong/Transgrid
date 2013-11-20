/**
 * @Ventyx 2012
 *
 * This program will create a report & csv file contains employee's contiguous time
 * between travel time and over time so Payroll can make a correction to the 
 * employee’s pay prior to payment.
 * 
 * Developed based on <b>URS.REPORTING.E8.EXCEPTION.ADJUST.TT.IN.MINIMUMS.D03.DOCX</b>
 *
 */
package com.mincom.ellipse.script.custom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.orchestration.CollectionOrchestrator;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf817.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf832.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.enterpriseservice.ellipse.table.*;

public class ParamsTrb897{
    //List of Input Parameters
    private String paramPayLocation;

    private String paramPayPeriods;

    private String paramTravelTimeWorkCode0, paramTravelTimeWorkCode1, paramTravelTimeWorkCode2, paramTravelTimeWorkCode3,
    paramTravelTimeWorkCode4

    private String paramNonMergedOTWorkCode0, paramNonMergedOTWorkCode1, paramNonMergedOTWorkCode2, paramNonMergedOTWorkCode3,
    paramNonMergedOTWorkCode4, paramNonMergedOTWorkCode5, paramNonMergedOTWorkCode6,
    paramNonMergedOTWorkCode7, paramNonMergedOTWorkCode8, paramNonMergedOTWorkCode9,
    paramNonMergedOTWorkCode10, paramNonMergedOTWorkCode11, paramNonMergedOTWorkCode12,
    paramNonMergedOTWorkCode13, paramNonMergedOTWorkCode14, paramNonMergedOTWorkCode15,
    paramNonMergedOTWorkCode16, paramNonMergedOTWorkCode17

    private String paramMinimumEarningsCode;

    private String paramTravelTimeEarningsCodes0, paramTravelTimeEarningsCodes1, paramTravelTimeEarningsCodes2;

    private String paramTravelTimeAdjustmentCode;

    //Restart Variables
    String restartTableCode = "    ";
}

public class TRB897A implements Comparable<TRB897A>{
    String empId, empName, payLoc, trnDate, trnCode,
    workCode, apprStatus, reverseStatus,
    lastModDate, lastModTime, payPeriodNo, payGroup, codeType;
    BigDecimal TTAdjust, startTime, stopTime;

    public int compareTo(TRB897A o) {
        int c = this.getPayLoc().compareTo(o.getPayLoc())
        if(c==0) c = this.getEmpName().compareTo(o.getEmpName())
        if(c==0) c = this.getTrnDate().compareTo(o.getTrnDate())
        if(c==0) c = this.getStartTime().compareTo(o.getStartTime())
        return c;
    }
}

public class ProcessTrb897 extends SuperBatch {
    /*
     * Constants
     */
    private static final String PAY_GROUP_TG1             = "TG1"
    private static final String TRAN_APPR_STATUS          = "APPR"
    private static final String TRAN_PAID_STATUS          = "PAID"
    private static final String REV_RPLD_STATUS           = "RPLD"
    private static final String REV_RVSD_STATUS           = "RVSD"
    private static final String MSF801_TYPE_PG            = "PG"
    private static final String TRAVEL_TIME               = "TRAVEL-TIME"
    private static final String NON_MERGED_OT             = "NON-MERGED-OT"
    private static final String ERR_MSG_INPUT_REQUIRED    = "INPUT REQUIRED"
    private static final String ERR_MSG_INPUT_NOT_EXST    = "%s - INPUT DOES NOT EXIST"
    private static final String ERR_MSG_INPUT_MAX_LENGTH  = "%s - MAX INPUT LENGTH EXCEED"
    private static final String ERR_MSG_INPUT_NUMERIC     = "%s - INPUT MUST BE NUMERIC"
    private static final String DEFAULT_TRAN_TYPE         = "E"
    private static final String TABLE_SERVICE             = "TABLE"

    private static final String REPORT_NAME            = "TRB897A"
    private static final String REPORT_ERROR_NAME      = "TRB897E"
    private static final String DASHED_LINE            = String.format("%132s"," ").replace(' ', '-')
    private static final String REPORT_TITLE           = "Exception Report After Lockout - Adjust Travel Time In Minimums"
    private static final String REPORT_TITLE_2         = "Pay Location: %-2s - %-30s    Pay Period: %-8s"
    private static final String REPORT_ERROR_TITLE     = "${REPORT_ERROR_NAME} - Exception Report - Adjust Travel Time In Minimums"
    private static final String REPORT_ERROR_HEADING   = "Field Ref/Value                 Error/Warning Message Column Hdg "
    private static final String REPORT_ERROR_DETAIL    = "%-30s  %-99s"
    private static final String REPORT_SUB_HEAD        = "PAY LOCATION: %-2s - %-30s"
    private static final String REPORT_HEADING_1       = "Employee ID  Employee Name                   Day  Date      Work Code   Start Time  Stop Time   Status     TT Adjust"
    private static final String REPORT_HEADING_2       = "                                                                                                           x 1.0"
    private static final String REPORT_DETAIL          = "%-10s   %-30s  %-3s  %-8s  %-5s       %-5s       %-5s       %-9s %5.2f    "
    private static final String REPORT_DETAIL_2        = "%-10s   %-30s  %-3s  %-8s  %-5s       %-5s       %-5s       %-9s "
    private static final String CSV_HEADER             = "Employee ID,Tran Date,Tran Type,Tran Code,Tran Units"
    private static final String CSV_DETAIL             = "%-10s,%-8s,%-1s,%-3s,% 5.2f"


    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 4;
    private ParamsTrb897 batchParams;
    private String payLocation, startPayPeriod, endPayPeriod, minimumEarningsCode, TTAdjustmentCode;
    private int payPeriodToProcess;
    private ArrayList<String> payLocationList, travelTimeWorkCode, nonMergedOTWorkCode, travelTimeEarningCode;
    private ArrayList<TRB897A> travelTimeRec, nonMergedOTRec, savedRecords, writeToCsv;
    private LinkedHashMap<String, ArrayList<TRB897A>> finalEmployeeRec
    private def ReportA
    private File newFileA
    private FileWriter fstreamA
    private BufferedWriter reportA
    private boolean errorFlag, allPayLoc
    private def reportWriter, reportErrorWriter
    private File csvFile
    private BufferedWriter csvWriter
    private LinkedHashMap<String, String> errorMessages

    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb897())

        //PrintRequest Parameters
        info("paramPayLocation        : " + batchParams.paramPayLocation)
        info("paramPayPeriodsToProcess: " + batchParams.paramPayPeriods)

        //print travel time work codes
        travelTimeWorkCode = new ArrayList<String>()
        Class iClazz = batchParams.getClass()
        (0..4).each{
            String fieldName = "paramTravelTimeWorkCode${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName)
            field.setAccessible(true)
            if(field.get(batchParams)?.toString().trim()) travelTimeWorkCode.add(field.get(batchParams))
            info("paramTravelTimeCodes${it} : ${field.get(batchParams)}")
        }

        //print non-mergedOT work codes
        nonMergedOTWorkCode = new ArrayList<String>()
        (0..17).each{
            String fieldName = "paramNonMergedOTWorkCode${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName)
            field.setAccessible(true)
            if(field.get(batchParams)?.toString().trim()) nonMergedOTWorkCode.add(field.get(batchParams))
            info("paramNonMergedOTWorkCodes${it} : ${field.get(batchParams)}")
        }

        info("paramMinimumEarningsCode: " + batchParams.paramMinimumEarningsCode)

        //print travel time earning codes
        travelTimeEarningCode = new ArrayList<String>()
        (0..2).each{
            String fieldName = "paramTravelTimeEarningsCodes${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName)
            field.setAccessible(true)
            if(field.get(batchParams)?.toString().trim()) travelTimeEarningCode.add(field.get(batchParams))
            info("paramTravelTimeEarningsCodes${it} : ${field.get(batchParams)}")
        }

        info("paramTravelTimeAdjustmentCode : " + batchParams.paramTravelTimeAdjustmentCode)

        try {
            processBatch();
        } finally {
            printBatchReport();
        }
    }

    /**
     * Process the main batch
     */
    private void processBatch(){
        info("processBatch");
        //write process
        initialize()
        validateParamPayLoc()
        validateParamPayPeriods()
        validateParamTravelTimeCodes()
        validateParamNonMergedOTWorkCodes()
        validateParamMinimumEarningsCode()
        validateParamTravelTimeEarningsCodes()
        validateParamTravelTimeAdjustmentCode()
        if(!errorFlag){
            processEmployeeMasterfileDetails()
            writeExceptionReport()
            writeCSVReport()
        }
    }

    //additional method - start from here.
    /**
     * initialize arraylist & linkedHashMap
     */
    private void initialize(){
        info("initialize")
        errorMessages = new LinkedHashMap<String, String>()
        finalEmployeeRec = new LinkedHashMap<String, ArrayList<TRB897A>>()
        travelTimeRec = new ArrayList<TRB897A>()
        nonMergedOTRec = new ArrayList<TRB897A>()
        savedRecords = new ArrayList<TRB897A>()
        writeToCsv = new ArrayList<TRB897A>()
    }

    /**
     * Validate pay location input parameter
     */
    private void validateParamPayLoc(){
        info("validateValidPayLoc")
        allPayLoc = batchParams.paramPayLocation?.trim().length() == 0
        if(batchParams.paramPayLocation?.trim()){
            if(batchParams.paramPayLocation?.trim().length()<=2){
                TableServiceReadReplyDTO tableReply = readTable("PAYL", batchParams.paramPayLocation)
                if(tableReply == null) {
                    errorFlag = true
                    errorMessages.put("PayLocation",
                            String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayLocation))
                }

            } else {
                errorFlag = true
                errorMessages.put("PayLocation",
                        String.format(ERR_MSG_INPUT_MAX_LENGTH, batchParams.paramPayLocation))
            }
        }
    }

    /**
     * Validate pay periods input parameter
     * and if it pass, calculate the pay period.
     */
    private void validateParamPayPeriods(){
        info("validateParamPayPeriods")
        boolean isChecked = false
        if(batchParams.paramPayPeriods?.trim()){
            info("pay period is not blank")
            if(batchParams.paramPayPeriods.toString().length()>3){
                errorFlag = true
                errorMessages.put("PayPeriods",
                        String.format(ERR_MSG_INPUT_MAX_LENGTH, batchParams.paramPayPeriods))
                isChecked = true
            }
            calculatePayPeriod()
            if(!startPayPeriod?.trim()){
                errorFlag = true
                errorMessages.put("StartPayPeriods",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayPeriods))
            }
            if(!endPayPeriod?.trim()){
                errorFlag = true
                errorMessages.put("EndPayPeriods",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayPeriods))
            }
        } else {
            errorFlag = true
            errorMessages.put("PayPeriods", ERR_MSG_INPUT_REQUIRED)
        }
    }

    /**
     * Calculate the pay period.
     */
    private void calculatePayPeriod() {
        info("calculatePayPeriod")
        MSF801_PG_801Rec payGroupRec = getPayGroupRecord(PAY_GROUP_TG1)
        if(payGroupRec) {
            endPayPeriod = payGroupRec.curEndDtPg
            //convert to java Calendar
            Calendar cal = Calendar.getInstance()
            cal.clear()
            cal.set(endPayPeriod.substring(0, 4) as int, //year
                    (endPayPeriod.substring(4, 6) as int) - 1, //month, starts from 0
                    endPayPeriod.substring(6, 8) as int) //day
            //calculate pay period substraction
            int payPeriod_9 = (batchParams.paramPayPeriods as int) * -1
            //substract the week, validate the result against
            //http://www.timeanddate.com/date/dateadd.html
            cal.add(Calendar.WEEK_OF_YEAR, payPeriod_9)
            cal.add(Calendar.DAY_OF_WEEK, 1)
            startPayPeriod = new java.text.SimpleDateFormat("yyyyMMdd").format(cal.getTime())
            info("endPayPeriod   : ${endPayPeriod}")
            info("startPayPeriod : ${startPayPeriod}")
        }
    }

    /**
     * Get Pay Group based on the pay code.
     * @param payCode Pay Group code
     * @return MSF801_A_801Rec
     */
    private MSF801_PG_801Rec getPayGroupRecord(String payCode) {
        info("getPayGroupRecord")
        MSF801_PG_801Key payGroupKey = new MSF801_PG_801Key()
        payGroupKey.setCntlKeyRest(payCode)
        payGroupKey.setCntlRecType(MSF801_TYPE_PG)
        MSF801_PG_801Rec payGroupRec
        try {
            payGroupRec = edoi.findByPrimaryKey(payGroupKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            payGroupRec = null
        }
        return payGroupRec
    }

    /**
     * Check if work code is validCnt HR Table Code
     * @param code
     * @return true if the code is valid
     */
    private boolean isValidHRTableCode(String code){
        info("isValidHRTableCode")
        try{
            MSF801_R1_801Key msf801R1key = new MSF801_R1_801Key()
            msf801R1key.setCntlKeyRest("***${code}")
            msf801R1key.setCntlRecType("R1")
            MSF801_R1_801Rec msf801R1rec = edoi.findByPrimaryKey(msf801R1key)
            if(msf801R1rec) return true
            else return false
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Error message : "+e.message)
            return false
        }
    }
    /**
     * Check if earning code is valid or not
     * @param code
     * @return true if earning code is found in MSF801_A_801
     */
    private boolean isValidEarningCode(String code){
        info("isValidEarningCode")
        try{
            MSF801_A_801Key msf801Akey = new MSF801_A_801Key()
            msf801Akey.setCntlKeyRest("***${code}")
            msf801Akey.setCntlRecType("A")
            MSF801_A_801Rec msf801Arec = edoi.findByPrimaryKey(msf801Akey)
            return msf801Arec!=null
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Error message : "+e.message)
            return false
        }
    }
    /**
     * Check if travel time code is empty or not
     * @return false if it's not empty
     */
    private boolean isTravelTimeCodeEmpty(){
        info("isTravelTimeCodeEmpty")
        for(String travelCode : travelTimeWorkCode) {
            if(travelCode != null && travelCode?.trim()) {
                return false
            }
        }
        return true
    }
    /**
     * Check if non-merged OT code is empty or not
     * @return false if it's not empty
     */
    private boolean isNonmergedOTCodeEmpty(){
        info("isNonmergedOTCodeEmpty")
        for(String nonmergedOTCode : nonMergedOTWorkCode) {
            if(nonmergedOTCode != null && nonmergedOTCode?.trim()) {
                return false
            }
        }
        return true
    }
    /**
     * Check if travel time earning code is empty or not
     * @return false if it's not empty
     */
    private boolean isEarningCodeEmpty(){
        info("isEarningCodeEmpty")
        for(String earningCode : travelTimeEarningCode) {
            if(earningCode != null && earningCode?.trim()) {
                return false
            }
        }
        return true
    }

    /**
     * Validate travel time codes input parameter
     */
    private void validateParamTravelTimeCodes(){
        info("validateTravelTimeCode")
        if(isTravelTimeCodeEmpty()){
            errorFlag = true
            errorMessages.put("TravelTimeCode", ERR_MSG_INPUT_REQUIRED)
        } else {
            for(String travelCode : travelTimeWorkCode){
                if(travelCode.length()>2){
                    errorMessages.put("TravelTimeCode",
                            String.format(ERR_MSG_INPUT_MAX_LENGTH, travelCode))
                    errorFlag = true
                    break
                }
                if(!isValidHRTableCode(travelCode)){
                    errorMessages.put("TravelTimeCode",
                            String.format(ERR_MSG_INPUT_NOT_EXST, travelCode))
                    errorFlag = true
                    break
                }
            }
        }
    }

    /**
     * Validate non-mergedOT work codes input parameter
     */
    private void validateParamNonMergedOTWorkCodes(){
        info("validateNonMergedOTWorkCodes")
        if(isNonmergedOTCodeEmpty()){
            errorFlag = true
            errorMessages.put("NonMergedOTCode", ERR_MSG_INPUT_REQUIRED)
        } else {
            for(String nonMergedCode : nonMergedOTWorkCode){
                if(nonMergedCode.length()>2){
                    errorMessages.put("NonMergedOTCode",
                            String.format(ERR_MSG_INPUT_MAX_LENGTH, nonMergedCode))
                    errorFlag = true
                    break
                }
                if(!isValidHRTableCode(nonMergedCode)){
                    errorMessages.put("NonMergedOTCode",
                            String.format(ERR_MSG_INPUT_NOT_EXST, nonMergedCode))
                    errorFlag = true
                    break
                }
            }
        }
    }
    /**
     * Validate minimum earning codes input parameter
     */
    private void validateParamMinimumEarningsCode(){
        info("validateMinimumEarningsCode")
        boolean isChecked = false
        if(!batchParams.paramMinimumEarningsCode?.trim()){
            errorMessages.put("MinimumEarningCode", ERR_MSG_INPUT_REQUIRED)
            errorFlag = true
            isChecked = true
        }
        if(!isChecked && batchParams.paramMinimumEarningsCode.toString().length()>3){
            errorMessages.put("MinimumEarningCode",
                    String.format(ERR_MSG_INPUT_MAX_LENGTH, batchParams.paramMinimumEarningsCode))
            errorFlag = true
            isChecked = true
        }
        if(!isChecked && !batchParams.paramMinimumEarningsCode.isNumber()){
            errorFlag = true
            errorMessages.put("MinimumEarningCode",
                    String.format(ERR_MSG_INPUT_NUMERIC, batchParams.paramMinimumEarningsCode))
        } else if(!isChecked) {
            if(isValidEarningCode(batchParams.paramMinimumEarningsCode)){
                minimumEarningsCode = batchParams.paramMinimumEarningsCode
            } else {
                info("here 2")
                errorMessages.put("MinimumEarningCode",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramMinimumEarningsCode))
            }
        }
    }
    /**
     * validate travel time adjustment code from input parameter
     */
    private void validateParamTravelTimeAdjustmentCode(){
        info("validateParamTravelTimeAdjustmentCode")
        boolean isChecked = false
        if(!batchParams.paramTravelTimeAdjustmentCode?.trim()){
            info("batchParams.paramTravelTimeAdjustmentCode 2 : "+batchParams.paramTravelTimeAdjustmentCode)
            errorMessages.put("TravelTimeAdjustmentCode", ERR_MSG_INPUT_REQUIRED)
            errorFlag = true
            isChecked = true
        }
        if(!isChecked && batchParams.paramTravelTimeAdjustmentCode.toString().length()>3){
            errorMessages.put("TravelTimeAdjustmentCode",
                    String.format(ERR_MSG_INPUT_MAX_LENGTH, batchParams.paramTravelTimeAdjustmentCode))
            errorFlag = true
            isChecked = true
        }
        if(!isChecked && !batchParams.paramTravelTimeAdjustmentCode.isNumber()){
            errorFlag = true
            errorMessages.put("TravelTimeAdjustmentCode",
                    String.format(ERR_MSG_INPUT_NUMERIC, batchParams.paramTravelTimeAdjustmentCode))
        } else if(!isChecked) {
            if(isValidEarningCode(batchParams.paramTravelTimeAdjustmentCode)){
                TTAdjustmentCode = batchParams.paramTravelTimeAdjustmentCode
            } else {
                errorFlag = true
                errorMessages.put("TravelTimeAdjustmentCode",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramTravelTimeAdjustmentCode))
            }
        }
    }
    /**
     * Validate travel time earning codes input parameter
     */
    private void validateParamTravelTimeEarningsCodes(){
        info("validateTravelTimeEarningsCodes")
        if(isEarningCodeEmpty()){
            errorFlag = true
            errorMessages.put("TravelTimeEarningCode", ERR_MSG_INPUT_REQUIRED)
        } else {
            for(String earningCode : travelTimeEarningCode){
                if(earningCode.length()>3){
                    errorMessages.put("TravelTimeEarningCode",
                            String.format(ERR_MSG_INPUT_MAX_LENGTH, earningCode))
                    errorFlag = true
                    break
                }
                if(!earningCode.isNumber()){
                    errorMessages.put("TravelTimeEarningCode",
                            String.format(ERR_MSG_INPUT_NUMERIC, earningCode))
                    errorFlag = true
                    break
                }
                if(!isValidEarningCode(earningCode)){
                    errorMessages.put("TravelTimeEarningCode",
                            String.format(ERR_MSG_INPUT_NOT_EXST, earningCode))
                    errorFlag = true
                    break
                }
            }
        }
    }

    /**
     * Get employee name from employee id
     * @param empId
     * @return employee name
     */
    private String getEmpName(String empId){
        info("getEmpName")
        try{
            MSF810Key msf810key = new MSF810Key()
            msf810key.employeeId = empId
            MSF810Rec msf810rec = edoi.findByPrimaryKey(msf810key)
            return "${msf810rec.surname}, ${msf810rec.firstName}"
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Error message : "+e.message)
            return "Employee id not found"
        }
    }

    /**
     * Browse the employee payroll masterfile details
     */
    private void processEmployeeMasterfileDetails(){
        info("processEmployeeMasterfileDetails")
        String payLocation
        ArrayList<TRB897A> empRec = new ArrayList<TRB897A>()
        ArrayList<TRB897A> empList = new ArrayList<TRB897A>()
        try{
            QueryImpl queryMSF820 = new QueryImpl(MSF820Rec.class).
                    and(MSF820Key.employeeId.greaterThan(" "))
            if(!allPayLoc){
                queryMSF820 = queryMSF820.and(MSF820Rec.payLocation.equalTo(batchParams.paramPayLocation))
            }
            edoi.search(queryMSF820){MSF820Rec msf820rec ->
                payLocation = msf820rec.getPayLocation()?.trim()
                empRec.addAll(processEmpStartStopDetails(msf820rec))
                if(!empRec?.isEmpty()){
                    addToFinalEmpRecMap(payLocation, empRec)
                    empRec.clear()
                }
            }
        } catch (Exception e){
            info("Error message : "+e.message)
        }
    }
    /**
     * add employee record to the final List
     * @param payLoc
     * @param emp
     */
    private void addToFinalEmpRecMap(String payLoc, ArrayList<TRB897A> emp){
        info("addToFinalEmpRec")
        ArrayList<TRB897A> empList = finalEmployeeRec.get(payLoc)
        if(empList == null) {
            empList = new ArrayList<TRB897A>()
        }
        empList.addAll(emp)
        finalEmployeeRec.put(payLoc, empList)
    }

    /**
     * Check if a corresponding APPR - Approved transaction for the same work code on the same date
     * @param empStartStopRec TRB897A
     * @return false if exist
     */
    private boolean isCorrespondingApprovedTransactionExist(TRB897A empStartStopRec) {
        info("isCorrespondingApprovedTransactionExist")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.employeeId.equalTo(empStartStopRec.getEmpId())).
                and(MSF891Key.trnDate.equalTo(empStartStopRec.getTrnDate())).
                and(MSF891Key.payGroup.equalTo(empStartStopRec.getPayGroup())).
                and(MSF891Rec.workCode.equalTo(empStartStopRec.getWorkCode())).
                and(MSF891Rec.tranApprStatus.equalTo(TRAN_APPR_STATUS))
        return edoi.search(qMSF891).getResults().isEmpty()
    }
    /**
     * Checking employee's last mod date / time
     * @param emp
     * @return true if employee's time/date is greather than Pay Group TG1 last mod time/date
     */
    private boolean isGreaterThanRunTimeDatePayGroup(TRB897A emp){
        info("isGreaterThanRunTimeDatePayGroup")
        try{
            QueryImpl qMSF817 = new QueryImpl(MSF817Rec.class).
                    and(MSF817Key.payGroup.equalTo(PAY_GROUP_TG1)).
                    and(MSF817Key.payRunType.equalTo("U")).
                    min(MSF817Key.invEndDate)
            String invEndDate = edoi.firstRow(qMSF817)

            qMSF817 = new QueryImpl(MSF817Rec.class).
                    and(MSF817Key.payGroup.equalTo(PAY_GROUP_TG1)).
                    and(MSF817Key.payRunType.equalTo("U")).
                    and(MSF817Key.invEndDate.equalTo(invEndDate))
            MSF817Rec msf817rec = edoi.firstRow(qMSF817)

            if(emp.getLastModDate().trim().compareTo(msf817rec.getRunDate()) > 0 ||
            emp.getLastModTime().trim().compareTo(msf817rec.getRunTime()) > 0){
                return true
            } else
                return false
        } catch (Exception e){
            info("Error message : "+e.message)
            return false
        }
    }

    /**
     * Collect employee's start and stop details
     * @param msf820rec
     */
    private ArrayList<TRB897A> processEmpStartStopDetails(MSF820Rec msf820rec){
        info("processEmpStartStopDetails")
        boolean isTravelTimeCode
        if(!savedRecords?.isEmpty()) savedRecords.clear()
        if(!travelTimeRec?.isEmpty()) travelTimeRec.clear()
        if(!nonMergedOTRec?.isEmpty()) nonMergedOTRec.clear()
        try{
            Constraint c1 = MSF891Key.payGroup.greaterThanEqualTo(" ")
            Constraint c2 = MSF891Key.employeeId.equalTo(msf820rec.getPrimaryKey().employeeId)
            Constraint c3 = MSF891Key.trnDate.lessThanEqualTo(endPayPeriod)
            Constraint c4 = MSF891Key.trnDate.greaterThanEqualTo(startPayPeriod)
            Constraint c5 = MSF891Rec.tranApprStatus.equalTo(TRAN_APPR_STATUS)
            Constraint c6 = MSF891Rec.tranApprStatus.equalTo(TRAN_PAID_STATUS)
            def queryMSF891 = new QueryImpl(MSF891Rec.class).and(c1).and(c2).and(c3).and(c4).and(c5.or(c6))
            edoi.search(queryMSF891){ MSF891Rec msf891rec ->
                //check whether the work code is timetravel code or non-mergedOT code
                isTravelTimeCode = false
                for(int i=0;i<=travelTimeWorkCode.size()-1;i++){
                    if(msf891rec.workCode.toString()?.trim().equals(travelTimeWorkCode.get(i))){
                        if(!msf891rec.getTranApprStatus().toString().trim().equals(TRAN_PAID_STATUS)
                        || !msf891rec.getReverseStatus().toString().trim().equals("")){
                            TRB897A ttRec = new TRB897A()
                            ttRec.setPayLoc(msf820rec.getPayLocation())
                            ttRec.setEmpId(msf820rec.getPrimaryKey().getEmployeeId())
                            ttRec.setEmpName(getEmpName(ttRec.getEmpId()))
                            ttRec.setTrnDate(msf891rec.getPrimaryKey().getTrnDate())
                            ttRec.setWorkCode(msf891rec.getWorkCode())
                            ttRec.setStartTime(msf891rec.getFromTime())
                            ttRec.setStopTime(msf891rec.getEndTime())
                            ttRec.setApprStatus(msf891rec.getTranApprStatus().toString().trim())
                            ttRec.setReverseStatus(msf891rec.getReverseStatus().toString().trim())
                            ttRec.setLastModDate(msf891rec.getLastModDate())
                            ttRec.setLastModTime(msf891rec.getLastModTime())
                            ttRec.setPayPeriodNo(msf891rec.getPayPerNo())
                            ttRec.setPayGroup(msf891rec.getPrimaryKey().getPayGroup())
                            ttRec.setCodeType(TRAVEL_TIME)
                            travelTimeRec.add(ttRec)
                            isTravelTimeCode = true
                            break
                        }

                    }
                }
                if(!isTravelTimeCode)
                    for(int i=0;i<=nonMergedOTWorkCode.size()-1;i++){
                        if(msf891rec.workCode.toString()?.trim().equals(nonMergedOTWorkCode.get(i))){
                            TRB897A nmOTRec = new TRB897A()
                            nmOTRec.setPayLoc(msf820rec.getPayLocation())
                            nmOTRec.setEmpId(msf820rec.getPrimaryKey().getEmployeeId())
                            nmOTRec.setEmpName(getEmpName(nmOTRec.getEmpId()))
                            nmOTRec.setTrnDate(msf891rec.getPrimaryKey().getTrnDate())
                            nmOTRec.setWorkCode(msf891rec.getWorkCode())
                            nmOTRec.setStartTime(msf891rec.getFromTime())
                            nmOTRec.setStopTime(msf891rec.getEndTime())
                            nmOTRec.setApprStatus(msf891rec.getTranApprStatus().toString().trim())
                            nmOTRec.setReverseStatus(msf891rec.getReverseStatus().toString().trim())
                            nmOTRec.setLastModDate(msf891rec.getLastModDate())
                            nmOTRec.setLastModTime(msf891rec.getLastModTime())
                            nmOTRec.setPayPeriodNo(msf891rec.getPayPerNo())
                            nmOTRec.setPayGroup(msf891rec.getPrimaryKey().getPayGroup())
                            nmOTRec.setCodeType(NON_MERGED_OT)
                            nonMergedOTRec.add(nmOTRec)
                            break
                        }
                    }
            }

            //check the contiguous traveltime code with non-mergedOT code
            if(!travelTimeRec.isEmpty() && !nonMergedOTRec.isEmpty()){
                for(int i=0;i<=travelTimeRec.size()-1;i++){
                    for(int j=0;j<=nonMergedOTRec.size()-1;j++){
                        if(travelTimeRec.get(i).getTrnDate().equals(nonMergedOTRec.get(j).getTrnDate()) &&
                        (travelTimeRec.get(i).getStopTime().equals(nonMergedOTRec.get(j).getStartTime()) ||
                        travelTimeRec.get(i).getStartTime().equals(nonMergedOTRec.get(j).getStopTime()))){
                            savedRecords.add(travelTimeRec.get(i))
                            savedRecords.add(nonMergedOTRec.get(j))
                        }
                    }
                }
            }

            //remove redundant record using hashset
            HashSet hs = new HashSet();
            hs.addAll(savedRecords)
            savedRecords.clear()
            savedRecords.addAll(hs);
            if(!savedRecords.isEmpty()){
                //process record per date, sort first so the date is sorted
                Collections.sort(savedRecords)
                LinkedHashMap<String, ArrayList<TRB897A>> sortedList = new LinkedHashMap<String, ArrayList<TRB897A>>()
                String tDate = ""
                savedRecords.each{
                    tDate = it.trnDate
                    Map tempMap = savedRecords.groupBy{it.trnDate.equals(tDate)}
                    sortedList.put(tDate, tempMap.get(true))
                }
                savedRecords.clear()
                BigDecimal minimumHours = 0, travelTimeHours = 0, TTConversion = 0, adjustmentHours = 0, TTAdjust = 0
                sortedList.each{ empDate, empRec ->
                    ArrayList<TRB897A> tempList = new ArrayList<TRB897A>();
                    tempList.addAll(sortedList.get(empDate))
                    if(checkCorrespondingTransaction(tempList)){
                        tempList.each{
                            //calculate 'minimum hours' & 'travel time hours'
                            if(it.codeType.equals(NON_MERGED_OT)){
                                minimumHours += calculateHours(it)
                            } else {
                                travelTimeHours += calculateHours(it)
                                TTConversion = travelTimeHours
                                //convert travelTimeHours to postive if it's value is negative
                                //the conversion value only used to determine which value will
                                //be use for adjustment hours.
                                //for calculation, use the original value.
                                if(TTConversion < 0) TTConversion *= -1
                            }
                        }
                        if(TTConversion<=minimumHours){
                            adjustmentHours = travelTimeHours
                            adjustmentHours *= -1
                        } else{
                            adjustmentHours = minimumHours
                            adjustmentHours *= -1
                        }
                        TTAdjust = convertToHoursMinutes(adjustmentHours)
                        //set the TTAdjust for current date in the first record only
                        tempList.get(0).setTTAdjust(TTAdjust)
                        savedRecords.addAll(empRec)

                        //save record to writeToCsvEmpDetails so employee's detail is printed 1 record per date in csv
                        TRB897A writeToCsvEmpDetails = new TRB897A()
                        writeToCsvEmpDetails.setPayLoc(tempList.get(0).payLoc)
                        writeToCsvEmpDetails.setEmpId(tempList.get(0).empId)
                        writeToCsvEmpDetails.setEmpName(tempList.get(0).empName)
                        writeToCsvEmpDetails.setStartTime(tempList.get(0).startTime)
                        writeToCsvEmpDetails.setTrnDate(empDate)
                        writeToCsvEmpDetails.setTTAdjust(TTAdjust)
                        writeToCsv.add(writeToCsvEmpDetails)
                        minimumHours = 0; travelTimeHours = 0; TTConversion = 0; adjustmentHours = 0; TTAdjust = 0
                    }
                }
            }
            return savedRecords
        } catch (Exception e){
            info("Error message : "+e.message)
        }
    }
    /**
     * Check corresponding approved transaction for PAID/RPLD and PAID/RVSD records per date
     * @param empRec
     * @return false if there is no approved transaction exist or not greather than run time date pay group
     */
    private boolean checkCorrespondingTransaction(ArrayList<TRB897A> empRec){
        info("checkCorrespondingTransaction")
        for(int i=0;i<=empRec.size()-1;i++){
            if("${empRec.get(i).getApprStatus()}/${empRec.get(i).getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/${REV_RPLD_STATUS}".toString())){
                if(isCorrespondingApprovedTransactionExist(empRec.get(i))){
                    return false
                }
            }
            if("${empRec.get(i).getApprStatus()}/${empRec.get(i).getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/${REV_RVSD_STATUS}".toString())){
                if(!isGreaterThanRunTimeDatePayGroup(empRec.get(i))){
                    return false
                }
            }
        }

        return true
    }
    /**
     * Convert from big decimal value to hours.minutes value
     * @param valToConvert
     * @return hours.minutes value
     */
    private BigDecimal convertToHoursMinutes(BigDecimal valToConvert){
        info("convertToHoursMinutes")
        //calculate from decimal to hours and minutes (without seconds)
        //validate result against:
        //http://www.springfrog.com/converter/decimal-time.htm
        info("valToConvert : "+valToConvert)
        int hours = Math.round(valToConvert * 60)/60
        int minutes = Math.round(valToConvert * 60).mod(60)
        if(minutes<0) minutes *= -1
        String result = String.valueOf(hours)+"."+String.valueOf(minutes)
        if(hours==0 && valToConvert < 0) result="-"+result
        info("hours   : "+hours)
        info("minutes : "+minutes)
        info("result  : "+result)
        return result?.toBigDecimal()
    }
    /**
     * Convert hours.minutes formatted value to decimal formatted value
     * @param valToConvert
     * @return decimal formatted value
     */
    private BigDecimal convertToDecimal(BigDecimal valToConvert){
        info("convertToDecimal")
        info("valToConvert : "+valToConvert)
        BigDecimal value = valToConvert
        if(valToConvert < 0) {
            value * -1
        }
        int[] parse = breakBigDecimal(value)
        int totalMinutes = (parse[0] * 60) + parse[1]
        BigDecimal decimalFormat = totalMinutes / 60
        DecimalFormat unitFormatter = new DecimalFormat("######.##")
        unitFormatter.setMinimumFractionDigits(2)
        String result = unitFormatter.format(decimalFormat)
        if(valToConvert < 0) {
            result += "-"
        }
        info("result : "+result)
        return result?.toBigDecimal()
    }
    /**
     * Calculate total hours
     * @param emp
     * @return minimum hours or travel time hours
     */
    private BigDecimal calculateHours(TRB897A emp){
        info("calculateHours")
        BigDecimal trnUnits = 0, factor = 0, minimumHours = 0, minimumHoursTmp = 0, travelTimeHours = 0, travelTimeHoursTmp = 0
        try{
            Constraint c1 = MSF832Key.employeeId.equalTo(emp.getEmpId())
            Constraint c2 = MSF832Key.payGroup.equalTo(emp.getPayGroup())
            Constraint c3 = MSF832Key.trnDate.equalTo(emp.getTrnDate())
            Constraint c4 = MSF832Key.tranInd.equalTo("1")
            Constraint c5 = MSF832Rec.fromTime.equalTo(emp.getStartTime())
            Constraint c6 = MSF832Rec.endTime.equalTo(emp.getStopTime())
            QueryImpl queryMSF832 = new QueryImpl(MSF832Rec.class).and(c1).and(c2).and(c3).
                    and(c4).and(c5).and(c6)
            if(emp.getApprStatus().equals(TRAN_APPR_STATUS)){
                queryMSF832.and(MSF832Key.payPerNo.equalTo(emp.getPayPeriodNo()))
            }
            edoi.search(queryMSF832){ MSF832Rec msf832rec ->
                //Get factor from MSF832, if zero then use factor from MSF801A
                factor = msf832rec.getOrideFactor()
                if(factor == 0){
                    factor = getFactor801A(msf832rec.getPrimaryKey().getTranCode())
                }
                //Minimum Hours
                if(emp.getCodeType().equals(NON_MERGED_OT)){
                    if(msf832rec.getPrimaryKey().getTranCode()?.trim().equals(minimumEarningsCode)){
                        if("${emp.getApprStatus()}/${emp.getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/${REV_RVSD_STATUS}".toString())){
                            //collect all POSITIVE units
                            if(msf832rec.getTrnUnits()>0){
                                trnUnits = convertToDecimal(msf832rec.getTrnUnits())
                                minimumHoursTmp = trnUnits * factor
                                minimumHours += minimumHoursTmp
                            }
                        } else if("${emp.getApprStatus()}/${emp.getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/${REV_RPLD_STATUS}".toString()) ||
                        emp.getApprStatus().equals(TRAN_APPR_STATUS) ||
                        //status: PAID (no reversal status)
                        "${emp.getApprStatus()}/${emp.getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/".toString())){
                            //collect all units
                            trnUnits = convertToDecimal(msf832rec.getTrnUnits())
                            minimumHoursTmp = trnUnits * factor
                            minimumHours += minimumHoursTmp
                        }
                    }
                    //Travel Time Hours
                } else {
                    for(int i=0;i<=travelTimeEarningCode.size()-1;i++){
                        if(msf832rec.getPrimaryKey().getTranCode()?.trim().equals(travelTimeEarningCode.get(i))){
                            if(emp.getApprStatus().equals(TRAN_APPR_STATUS)){
                                //collect all units
                                trnUnits = convertToDecimal(msf832rec.getTrnUnits())
                                travelTimeHoursTmp = trnUnits * factor
                                travelTimeHours += travelTimeHoursTmp
                            } else if("${emp.getApprStatus()}/${emp.getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/${REV_RPLD_STATUS}".toString()) ||
                            "${emp.getApprStatus()}/${emp.getReverseStatus()}".toString().equals("${TRAN_PAID_STATUS}/${REV_RVSD_STATUS}".toString())){
                                //collect all NEGATIVE units
                                if(msf832rec.getTrnUnits()<0){
                                    trnUnits = convertToDecimal(msf832rec.getTrnUnits())
                                    travelTimeHoursTmp = trnUnits * factor
                                    travelTimeHours += travelTimeHoursTmp
                                }
                            }
                            break
                        }
                    }
                }
            }
        } catch (Exception e){
            info("Error message : "+e.message)
        }
        if(emp.getCodeType().equals(NON_MERGED_OT)) return minimumHours
        else return travelTimeHours
    }
    /**
     * Get earn factor value from MSF801_A
     * @param code
     * @return MSF801_A_801 earn factor
     */
    private BigDecimal getFactor801A(String code){
        info("getFactor801A")
        BigDecimal earnFactor = 0.0
        MSF801_A_801Key msf801Akey = new MSF801_A_801Key()
        msf801Akey.setCntlKeyRest("***${code}")
        msf801Akey.setCntlRecType("A")
        try{
            MSF801_A_801Rec msf801Arec = edoi.findByPrimaryKey(msf801Akey)
            earnFactor = msf801Arec.getEarnFactorA()
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("Error message : "+e.message)
        }
        return earnFactor
    }

    /**
     * Read Table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO if exist, null if doesn't exist
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable")
        TableServiceReadRequiredAttributesDTO tableReqAttributeDTO = new TableServiceReadRequiredAttributesDTO()
        tableReqAttributeDTO.returnTableCode = true
        tableReqAttributeDTO.returnTableType = true
        tableReqAttributeDTO.returnAssociatedRecord = true
        tableReqAttributeDTO.returnDescription = true

        TableServiceReadReplyDTO tableReplyDTO
        try {
            TableServiceReadRequestDTO tableRequestDTO = new TableServiceReadRequestDTO()
            tableRequestDTO.setTableType(tableType)
            tableRequestDTO.setTableCode(tableCode)
            tableRequestDTO.setRequiredAttributes(tableReqAttributeDTO)
            tableReplyDTO = service.get(TABLE_SERVICE).read(tableRequestDTO, false)
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
            String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = serviceExc.getErrorMessages()[0].getFieldName()
            info("Cannot read ${TABLE_SERVICE} caused by ${errorField}: ${errorCode} - ${errorMsg}.")
            tableReplyDTO = null
        }
        return tableReplyDTO
    }
    /**
     * Write Exception After Lockout Report
     *
     */
    private void writeExceptionReport(){
        info("writeExceptionReport")
        reportWriter = report.open(REPORT_NAME)
        reportWriter.write(DASHED_LINE)
        reportWriter.write(REPORT_TITLE.center(132))
        reportWriter.write(DASHED_LINE)
        //write header
        String payLocationName = !allPayLoc ?
                getPayLocationName(batchParams.paramPayLocation) : "(Blank For All)"
        String payPeriod = convertDateFormat(endPayPeriod)
        reportWriter.write(String.format(REPORT_TITLE_2,
                batchParams.paramPayLocation, payLocationName, payPeriod).center(132))
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
        //iterate each map
        info("finalEmprec : "+finalEmployeeRec.size())
        //add to TreeMap to make sure the Pay Location is sorted
        Map<String, ArrayList<TRB897A>> treeMap = new TreeMap<String, ArrayList<TRB897A>>(finalEmployeeRec);
        finalEmployeeRec.clear()
        treeMap.each{ payLoc, employees ->
            info("payLoc : "+payLoc)
            info("employees size : "+employees?.size())
            if(allPayLoc){
                payLocationName = getPayLocationName(payLoc)
                reportWriter.write(String.format(REPORT_SUB_HEAD, payLoc, payLocationName))
                reportWriter.write("")
            }
            reportWriter.write(DASHED_LINE)
            reportWriter.write(REPORT_HEADING_1)
            reportWriter.write(REPORT_HEADING_2)
            reportWriter.write(DASHED_LINE)
            Collections.sort(employees)
            String initialEmpId = employees.get(0).getEmpId()
            String initialDate = employees.get(0).getTrnDate()
            employees.each{
                String status = it.reverseStatus?.trim() ? "${it.apprStatus}/${it.reverseStatus}" : "${it.apprStatus}"
                if(!it.empId.equals(initialEmpId) || !it.trnDate.equals(initialDate)){
                    reportWriter.write("")
                    initialEmpId = it.empId
                    initialDate = it.trnDate
                }
                if(it.TTAdjust!=null){
                    reportWriter.write(String.format(REPORT_DETAIL,
                            it.empId,
                            it.empName,
                            getDayNameFromDate(it.trnDate),
                            convertDateFormat(it.trnDate),
                            it.workCode,
                            convertTimeFormat(it.startTime),
                            convertTimeFormat(it.stopTime),
                            status,
                            it.TTAdjust))
                } else {
                    reportWriter.write(String.format(REPORT_DETAIL_2,
                            it.empId,
                            it.empName,
                            getDayNameFromDate(it.trnDate),
                            convertDateFormat(it.trnDate),
                            it.workCode,
                            convertTimeFormat(it.startTime),
                            convertTimeFormat(it.stopTime),
                            status))
                }

            }
            reportWriter.write("")
            reportWriter.write(DASHED_LINE)
        }
    }
    /**
     * Create and write error report
     */
    private void writeErrorReport(){
        info("writeErrorReport")
        reportErrorWriter = report.open(REPORT_NAME)
        reportErrorWriter.write(REPORT_ERROR_TITLE.center(132))
        reportErrorWriter.write(DASHED_LINE)
        reportErrorWriter.write("")
        reportErrorWriter.write(DASHED_LINE)
        reportErrorWriter.write(REPORT_ERROR_HEADING)
        reportErrorWriter.write(DASHED_LINE)
        errorMessages.each {key, value->
            reportErrorWriter.write(String.format(REPORT_ERROR_DETAIL,
                    key?.length() > 30 ? key?.substring(0,30) : key,
                    value?.length() > 99 ? value?.substring(0,99) : value))
        }
        reportErrorWriter.write(DASHED_LINE)
        reportErrorWriter.write("")
    }
    /**
     * Create csv report and write the values
     */
    private void writeCSVReport(){
        //Intialize CSV
        String workingDir   = env.workDir
        String taskUUID     = this.getTaskUUID()
        String csvFilePath  = "${workingDir}/${REPORT_NAME}"
        if(taskUUID?.trim()) {
            csvFilePath  = csvFilePath  + "." + taskUUID
        }
        csvFilePath  = csvFilePath  + ".csv"
        csvFile = new File(csvFilePath)
        csvWriter = new BufferedWriter(new FileWriter(csvFile))
        //write header
        csvWriter.write(CSV_HEADER)
        csvWriter.write("\n")
        Collections.sort(writeToCsv)
        writeToCsv.each {
            csvWriter.write(String.format(CSV_DETAIL,
                    it.empId,
                    it.trnDate,
                    DEFAULT_TRAN_TYPE,
                    TTAdjustmentCode,
                    it.TTAdjust
                    ))
            csvWriter.write("\n")
        }
    }

    /**
     * Get the Pay Location name
     * @param payLocation Pay Location code
     * @return Pay Location name
     */
    private String getPayLocationName(String payLocation) {
        info("getPayLocationName")
        try{
            TableServiceReadReplyDTO tableDTO = readTable("PAYL", payLocation)
            if(tableDTO.getDescription()!=null) {
                return tableDTO.getDescription()?.trim()
            } else return ""
        } catch (Exception e){
            info("Error message : "+e.message)
            return ""
        }
    }
    /**
     * Convert the date format with specified separator <code>(YY/MM/DD)</code>.
     * @param dateS date as a String
     * @return formatted date as a String
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat")
        if(dateS){
            dateS = dateS.trim().padLeft(8).replace(" ", "0")
            return dateS.substring(6) + "/" + dateS.substring(4,6) + "/" + dateS.substring(2,4)
        } else return ""
    }
    /**
     * Convert the time format with specified separator into <code>(HH:MM)</code>.
     * @param time time as a BigDecimal
     * @return formatted time as a String
     */
    private String convertTimeFormat(BigDecimal time) {
        info("convertTimeFormat")
        int[] parts = breakBigDecimal(time)
        //hh:mm:ss
        return parts[0].toString().padLeft(2).replace(" ", "0") + ":" +
        parts[1].toString().padLeft(2).replace(" ", "0")
    }
    /**
     * Separate BigDecimals into integer and fraction parts
     * @param bd Big Decimals
     * @return integer and fraction parts
     */
    private int[] breakBigDecimal(BigDecimal bd) {
        info("breakBigDecimal")
        String bd_s = String.format("%4.2f", bd.toDouble())
        int integerPart  = bd_s.substring(0, bd_s.indexOf(".")) as int
        int fractionPart = bd_s.substring(bd_s.indexOf(".") + 1) as int
        int[] parts = [integerPart, fractionPart]
        return parts
    }
    /**
     * Get day from a date
     * @param date date (yyyyMMdd)
     * @return day from a date
     */
    private String getDayNameFromDate(String date) {
        info("getDayNameFromDate")
        Calendar cal = Calendar.getInstance()
        cal.clear()
        cal.set(date.substring(0, 4) as int, //year
                (date.substring(4, 6) as int) - 1, //month, starts from 0
                date.substring(6, 8) as int) //day
        return  new java.text.SimpleDateFormat("EEE").format(cal.getTime())
    }
    /**
     * write error report, close reportWriter, and add csv into report
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
        //Create error message report - if any
        if(!errorMessages.isEmpty()) {
            writeErrorReport()
            reportErrorWriter.close()
        }
        //Close the writer
        if(reportWriter != null) {
            reportWriter.close()
        }
        if(csvWriter != null) {
            csvWriter.close()
        }
        //Adding CSV into Request.
        info("Adding CSV into Request.")
        if(csvFile && getTaskUUID()?.trim()) {
            request.request.CURRENT.get().addOutput(csvFile,
                    "text/comma-separated-values", REPORT_NAME)
        }
    }
}

/*run script*/
ProcessTrb897 process = new ProcessTrb897();
process.runBatch(binding);