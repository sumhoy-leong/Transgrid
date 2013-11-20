/**
 * Ventyx @2013
 * 
 * This program will create report file contains employee's sustenance
 * claims which is being paid for more than 13 weeks (91 days).
 * 
 * Developed based on <b>TRB895 - URS.Exception Report - Excessive Suso.pdf</b>
 * 
 */
package com.mincom.ellipse.script.custom;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf89w.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.ellipse.edoi.ejb.msf898.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;

public class ParamsTrb895{
    //List of Input Parameters
    String paramPayLoc;
    String paramPayPeriods;

    //Restart Variables
    String restartTableCode = "    ";
}

public class TRB895A implements Comparable<TRB895A> {
    String payLoc, empId, empName, trnDate,
    workCode, apprStatus, reversalStatus, allowanceCode, locationTo, locationFrom;
    BigDecimal allowanceUnits, allowanceAmount;

    public int compareTo(TRB895A o) {
        int c = this.getEmpName().compareTo(o.getEmpName())
        if(c==0) c = this.getTrnDate().compareTo(o.getTrnDate())
        if(c==0) c = this.getLocationFrom().compareTo(o.getLocationFrom())
        if(c==0) c = this.getLocationTo().compareTo(o.getLocationTo())
        return c;
    }
}

public class ProcessTrb895 extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 3;
    private ParamsTrb895 batchParams;

    private static final String PAY_GROUP_TG1             = "TG1"
    private static final String TRAN_APPR_STATUS          = "APPR"
    private static final String TRN_STATUS_RGEN           = "RGEN"
    private static final String MSF801_TYPE_PG            = "PG"
    private static final String TABLE_SERVICE             = "TABLE"
    private static final String ERR_MSG_INPUT_NOT_EXST    = "%s - INPUT DOES NOT EXIST"
    private static final String SHIFT_TYPE_S              = "S"
    private static final String SHIFT_TYPE_O              = "O"

    private static final String REPORT_NAME            = "TRB895A"
    private static final String DASHED_LINE            = String.format("%132s"," ").replace(' ', '-')
    private static final String REPORT_TITLE           = "Exception Report - Excessive Sustenance Claims"
    private static final String REPORT_TITLE_2         = "Pay Location: %-2s - %-30s    Pay Period: %-8s"
    private static final String REPORT_ERROR_TITLE     = "${REPORT_NAME} - Exception Report - Excessive Sustenance Claims"
    private static final String REPORT_ERROR_HEADING   = "Field Ref/Value                 Error/Warning Message Column Hdg "
    private static final String REPORT_ERROR_DETAIL    = "%-30s  %-99s"
    private static final String REPORT_SUB_HEAD        = "PAY LOCATION: %-2s - %-30s"
    private static final String REPORT_HEADING_1       = "Employee ID  Employee Name                   Day  Date      Work Code   Status    Allowance  Allowance  Allowance Location  Location"
    private static final String REPORT_HEADING_2       = "                                                                                  Code       Units      Amount    From      To"
    private static final String REPORT_DETAIL          = "%-10s   %-30s  %-3s  %-8s  %-5s       %-9s %-5s     %5.2f       %5.2f    %-8s  %-8s"
    private static final String START_CONS_DATE        = "START_DATE"
    private static final String END_CONS_DATE          = "END_DATE"

    private LinkedHashMap<String, ArrayList<TRB895A>> finalEmpRec
    private LinkedHashMap<String, String> errorMessages
    private boolean allPayLoc
    private String startPayPeriod, endPayPeriod, empApprStatus, empRevStatus, empLocFr, empLocTo
    private TRB895A empRec
    private def reportWriter, reportErrorWriter
    private boolean errorFlag = true
    private BigDecimal earnFactor

    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb895())

        //PrintRequest Parameters
        info("paramPayLoc    : " + batchParams.paramPayLoc)
        info("paramPayPeriods: " + batchParams.paramPayPeriods)

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
        initialize()
        validateParamPayLoc()
        if(!errorFlag){
            calculatePayPeriod()
            processEmployeeMasterfileDetails()
            writeReport()
        }
    }

    //additional method - start from here.

    /**
     * Browse the employee payroll masterfile details
     */
    private void processEmployeeMasterfileDetails(){
        info("processEmployeeMasterfileDetails")
        String payLocation
        int countRec = 0
        ArrayList<TRB895A> emp = new ArrayList<TRB895A>()
        //        try{
        QueryImpl queryMSF820 = new QueryImpl(MSF820Rec.class).and(MSF820Key.employeeId.greaterThan(" "))
        if(!allPayLoc){
            queryMSF820 = queryMSF820.and(MSF820Rec.payLocation.equalTo(batchParams.paramPayLoc))
        }
        edoi.search(queryMSF820){MSF820Rec msf820rec ->
            countRec++
            payLocation = msf820rec.getPayLocation()?.trim()
            info("process emp id : "+msf820rec.getPrimaryKey().getEmployeeId())
            info("pay location   : "+payLocation)
            emp.addAll(processSustenanceAllowance(msf820rec))
            if(!emp?.isEmpty()){
                addToFinalEmpRecMap(payLocation, emp)
                emp.clear()
            }
        }
    }
    /**
     * Find the employee's sustenance date and find the consecutive date.
     * Calculate the total date from start consecutive date to end consecutive date.
     * Print to report if total date > 91 days.
     * @param msf820rec
     * @return ArrayList<TRB895A> of employee to be printed in report
     */
    private ArrayList<TRB895A> processSustenanceAllowance(MSF820Rec msf820rec){
        info("processSustenanceAllowance")
        String rosterWorkCode, shiftType, earnCode, startSustenanceDate, endSustenanceDate, oneDayBefore, oneDayAfter
        long totalConsecutiveDays = 0
        BigDecimal amount = 0
        ArrayList<TRB895A> empList = new ArrayList<TRB895A>()
        QueryImpl queryMSF89W = new QueryImpl(MSF89WRec.class).
                and(MSF89WKey.employeeId.equalTo(msf820rec.getPrimaryKey().getEmployeeId())).
                and(MSF89WKey.trnDate.greaterThanEqualTo(startPayPeriod)).
                and(MSF89WKey.trnDate.lessThanEqualTo(endPayPeriod)).
                and(MSF89WKey.payGroup.greaterThanEqualTo(" "))
        edoi.search(queryMSF89W){ MSF89WRec msf89Wrec ->
            //find the initial sustenance allowance
            empRec = new TRB895A()
            empRec.setPayLoc(msf820rec.getPayLocation())
            empRec.setEmpId(msf89Wrec.getPrimaryKey().getEmployeeId())
            empRec.setEmpName(getEmpName(msf89Wrec.getPrimaryKey().getEmployeeId()))
            empRec.setTrnDate(msf89Wrec.getPrimaryKey().getTrnDate())

            earnCode = msf89Wrec.getPrimaryKey().getEarnCode()

            if(isEarnCodeSustenace(earnCode) && isCorrespondingApprExist(msf89Wrec)){
                rosterWorkCode = getRosterWorkCode(msf89Wrec.getPrimaryKey().getEmployeeId(), msf89Wrec.getPrimaryKey().getTrnDate())
                shiftType = getShiftType(rosterWorkCode)

                empRec.setAllowanceCode(earnCode)
                empRec.setAllowanceUnits(msf89Wrec.getUnits())

                amount = msf89Wrec.getAmount()
                if(amount > 0) empRec.setAllowanceAmount(amount)
                else  empRec.setAllowanceAmount(earnFactor)

                empRec.setApprStatus(empApprStatus)
                empRec.setReversalStatus(empRevStatus)
                empRec.setLocationTo(empLocTo)
                empRec.setLocationFrom(empLocFr)
                empRec.setWorkCode(getTSWorkCode(msf89Wrec.getPrimaryKey().getPayGroup(), msf89Wrec.getPrimaryKey().getTrnDate(), msf89Wrec.getPrimaryKey().getEmployeeId(), msf89Wrec.getPrimaryKey().getSeqNo()))

                if(!shiftType.equals(SHIFT_TYPE_S) && !shiftType.equals(SHIFT_TYPE_O)){
                        oneDayBefore = calculateDate(msf89Wrec.getPrimaryKey().getTrnDate()?.trim(), -1)
                        startSustenanceDate = getConsecutiveRosteredShift(msf89Wrec.getPrimaryKey().getEmployeeId(), msf89Wrec.getPrimaryKey().getTrnDate()?.trim(), startPayPeriod, oneDayBefore, START_CONS_DATE)
                        if(!startSustenanceDate?.trim()) startSustenanceDate = msf89Wrec.getPrimaryKey().getTrnDate()?.trim()

                        oneDayAfter = calculateDate(msf89Wrec.getPrimaryKey().getTrnDate()?.trim(), 1)
                        endSustenanceDate = getConsecutiveRosteredShift(msf89Wrec.getPrimaryKey().getEmployeeId(), msf89Wrec.getPrimaryKey().getTrnDate()?.trim(), oneDayAfter, endPayPeriod, END_CONS_DATE)
                        if(!endSustenanceDate?.trim()) endSustenanceDate = msf89Wrec.getPrimaryKey().getTrnDate()?.trim()

                    if(!startSustenanceDate.equals(endSustenanceDate)){
                        totalConsecutiveDays = calculateDurationDay(startSustenanceDate, endSustenanceDate)
                        info("totalConsecutiveDays: "+totalConsecutiveDays)
                        if(totalConsecutiveDays > 91){
                            //save record to arraylist to be printed
                            empList.add(empRec)
                        }
                    }
                }
            }
        }
        return empList
    }
    /**
     * calculate the date x day(s) before/after
     * @param calDate
     * @param day
     * @return
     */
    private String calculateDate(String calDate, int day){
        info("calculateDate")
        Calendar cal = Calendar.getInstance()
        cal.set(calDate.substring(0, 4) as int, //year
                (calDate.substring(4, 6) as int) - 1, //month, starts from 0
                calDate.substring(6, 8) as int) //day
        cal.add(Calendar.DAY_OF_WEEK, day)
        return new java.text.SimpleDateFormat("yyyyMMdd").format(cal.getTime())
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
            info("Error message : "+e.getMessage())
            info(e.printStackTrace())
            return ""
        }
    }
    /**
     * Process to found the start consecutive date or end consecutive date
     * @param empId
     * @param startDate
     * @param endDate
     * @param flag
     * @return
     */
    private String getConsecutiveRosteredShift(String empId, String tranDate, String startDate, String endDate, String flag){
        info("getConsecutiveRosteredShift")
        String workCode898, transacDate, shiftType898, earnCode89w, latestDate = ""
        int durationDays = calculateDurationDay(startDate, endDate)
        boolean isStop = false
        for(int i = 1;i<=durationDays;i++){
            info("i : "+i+" of "+durationDays)
            if(flag.equals(START_CONS_DATE)){
                transacDate = calculateDate(tranDate, -i)
            } else {
                transacDate = calculateDate(tranDate, i)
            }
            
            workCode898 = getRosterWorkCode(empId, transacDate)
            shiftType898 = getShiftType(workCode898)
            
            if(!shiftType898.equals(SHIFT_TYPE_S) || !shiftType898.equals(SHIFT_TYPE_O)){
                //check wheter if the code sustenance or not
                QueryImpl queryMSF89W = new QueryImpl(MSF89WRec.class).
                        and(MSF89WKey.employeeId.equalTo(empId)).
                        and(MSF89WKey.trnDate.equalTo(transacDate)).
                        and(MSF89WKey.payGroup.greaterThanEqualTo(" "))
                edoi.search(queryMSF89W){ MSF89WRec msf89Wrec ->
                    earnCode89w = msf89Wrec.getPrimaryKey().getEarnCode()
                    //if the code is sustenance, save it
                    if(isEarnCodeSustenace(earnCode89w)){
                        latestDate = transacDate
                    } else {
                        isStop = true
                    }
                }
                if(isStop) break
            }
        }
        info("out")
        //return the latest saved date
        return latestDate
    }
    /**
     * Calculate duration between two dates
     * @param startDate
     * @param endDate
     * @return
     */
    private long calculateDurationDay(String startDate, String endDate){
        info("calculateDurationDay")
        long daysBetween = 1 //if startDate & endDate is same, it count as 1 day
        Calendar calStartDate = Calendar.getInstance()
        Calendar calEndDate = Calendar.getInstance()
        calStartDate.clear()
        calEndDate.clear()
        calStartDate.set(startDate.substring(0, 4) as int, //year
                (startDate.substring(4, 6) as int) - 1, //month, starts from 0
                startDate.substring(6, 8) as int) //day
        calEndDate.set(endDate.substring(0, 4) as int, //year
                (endDate.substring(4, 6) as int) - 1, //month, starts from 0
                endDate.substring(6, 8) as int) //day
        while(calStartDate.before(calEndDate)){
            calStartDate.add(Calendar.DAY_OF_MONTH, 1)
            daysBetween++
        }
        return daysBetween
    }
    /**
     * Get work code from MSF898
     * @param empId
     * @param tranDate
     * @return
     */
    private String getRosterWorkCode(String empId, String tranDate){
        info("getRosterWorkCode")
        MSF898Key msf898key = new MSF898Key()
        msf898key.setEmployeeId(empId)
        msf898key.setTrnDate(tranDate)
        msf898key.setTrnStatus(TRAN_APPR_STATUS)
        String returnString = " "
        
        MSF898Rec msf898rec = readRoster(msf898key)

        if(msf898rec == null){
            info("msf898 is null")
            msf898key.setTrnStatus(TRN_STATUS_RGEN)
            msf898rec = readRoster(msf898key)
        }
        
        if(msf898rec != null) {
            returnString = msf898rec.getWorkCode()
        }
        return returnString
    }
    
    /**
     * This function returns the roster record for the supplied key
     * @param msf898Key
     * @return the roster record if found, null if not
     */
    private MSF898Rec readRoster(MSF898Key msf898Key) {
        info("readRoster")
        MSF898Rec msf898rec = null
        try{
            msf898rec = edoi.findByPrimaryKey(msf898Key)
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Cannot read MSF898Rec caused by ${e.getMessage()}")
        }
        return msf898rec
    }
    
    /**
     * Get work code from timesheet
     * @param payGroup
     * @param tranDate
     * @param empId
     * @param seqNo
     * @return work code if found, space if not found
     */
    private String getTSWorkCode(String payGroup, String tranDate, String empId,  String seqNo){
        info("getTSWorkCode")
        MSF891Key msf891key = new MSF891Key()
        msf891key.setEmployeeId(empId)
        msf891key.setPayGroup(payGroup)
        msf891key.setSeqNo(seqNo)
        msf891key.setTrnDate(tranDate)
        String returnString = " "
        try{
            MSF891Rec msf891rec = edoi.findByPrimaryKey(msf891key)
            returnString = msf891rec.getWorkCode()
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Cannot read MSF891Rec caused by ${e.getMessage()}")
        }

        return returnString
    }
    /**
     * Get shift type from MSF801_R1
     * @param workCode
     * @return
     */
    private String getShiftType(String workCode){
        info("getShiftType")
        try{
            MSF801_R1_801Key msf801R1key = new MSF801_R1_801Key()
            msf801R1key.setCntlKeyRest("***${workCode}")
            msf801R1key.setCntlRecType("R1")
            MSF801_R1_801Rec msf801R1rec = edoi.findByPrimaryKey(msf801R1key)
            if(msf801R1rec.getShiftTypeR1()!=null) return msf801R1rec.getShiftTypeR1()?.trim()
            else return ""
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info(e.getMessage())
            return ""
        }
    }
    /**
     * Check wheter a code is sustenance code or not
     * @param earnCode
     * @return
     */
    private boolean isEarnCodeSustenace(String earnCode){
        info("isEarnCodeSustenace")
        info("earnCode : "+earnCode)
        earnFactor = 0

        MSF801_A_801Rec allowanceRec = getAllowanceRecord(earnCode)
        if(allowanceRec) {
            String[] miscRptFldA = [
                allowanceRec.getMiscRptFldAx1().trim(),
                allowanceRec.getMiscRptFldAx2().trim(),
                allowanceRec.getMiscRptFldAx3().trim(),
                allowanceRec.getMiscRptFldAx4().trim(),
                allowanceRec.getMiscRptFldAx5().trim()
            ]
            for(String s : miscRptFldA) {
                if(s.equals("S")) {
                    earnFactor = allowanceRec.getEarnFactorA()
                    return true
                }
            }
        }
        return false
    }
    /**
     * Get Allowance based on the work code.
     * @param allowanceCode Allowance code
     * @return MSF801_A_801Rec
     */
    private MSF801_A_801Rec getAllowanceRecord(String allowanceCode) {
        info("getAllowanceRecord")
        MSF801_A_801Key allowanceKey = new MSF801_A_801Key()
        allowanceKey.setCntlKeyRest("***${allowanceCode}")
        allowanceKey.setCntlRecType("A")
        MSF801_A_801Rec allowanceRec
        try {
            allowanceRec = edoi.findByPrimaryKey(allowanceKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("Cannot read MSF801_A_801Rec caused by ${e.getMessage()}")
            allowanceRec = null
        }
        return allowanceRec
    }
    /**
     * Check wheter a record has corresponding approval
     * @param msf89Wrec
     * @return
     */
    private boolean isCorrespondingApprExist(MSF89WRec msf89Wrec){
        info("isCorrespondingApprExist")
        empApprStatus = ""
        empRevStatus = ""
        empLocFr = ""
        empLocTo = ""
        QueryImpl queryMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.employeeId.equalTo(msf89Wrec.getPrimaryKey().getEmployeeId())).
                and(MSF891Key.payGroup.greaterThanEqualTo(" ")).
                and(MSF891Key.trnDate.equalTo(msf89Wrec.getPrimaryKey().getTrnDate())).
                and(MSF891Key.seqNo.equalTo(msf89Wrec.getPrimaryKey().getSeqNo())).
                and(MSF891Rec.tranApprStatus.equalTo(TRAN_APPR_STATUS))
        MSF891Rec msf891rec = edoi.firstRow(queryMSF891)
        if(msf891rec!=null){
            empApprStatus = msf891rec.getTranApprStatus()?.trim()
            empRevStatus = msf891rec.getReverseStatus()?.trim()
            empLocTo = msf891rec.getTranLocTo()?.trim()
            empLocFr = msf891rec.getTranLocFr()?.trim()
            return true
        } else return false
    }
    /**
     * add employee record to the final List
     * @param payLoc
     * @param emp
     */
    private void addToFinalEmpRecMap(String payLoc, ArrayList<TRB895A> emp){
        info("addToFinalEmpRecMap")
        ArrayList<TRB895A> empList = finalEmpRec.get(payLoc)
        if(empList == null) {
            empList = new ArrayList<TRB895A>()
        }
        empList.addAll(emp)
        finalEmpRec.put(payLoc, empList)
    }
    /**
     * initialize the error flag, and maps
     */
    private void initialize(){
        info("initialize")
        errorFlag = false
        finalEmpRec = new LinkedHashMap<String, ArrayList<TRB895A>>()
        errorMessages = new LinkedHashMap<String, String>()
    }

    /**
     * Validate pay location input parameter
     */
    private void validateParamPayLoc(){
        info("validateParamPayLoc")
        allPayLoc = batchParams.paramPayLoc?.trim().length() == 0
        if(batchParams.paramPayLoc?.trim()){
            if(batchParams.paramPayLoc?.trim().length()<=2){
                TableServiceReadReplyDTO tableReply = readTable("PAYL", batchParams.paramPayLoc)
                if(tableReply == null) {
                    errorFlag = true
                    errorMessages.put("PayLocation",
                            String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayLoc))
                }
            }
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
     * Print the report or error report if any
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
        if(!errorMessages.isEmpty()) {
            writeErrorReport()
            reportErrorWriter.close()
        }
        //Close the writer
        if(reportWriter != null) {
            reportWriter.close()
        }
    }
    /**
     * Write Exception After Lockout Report
     *
     */
    private void writeReport(){
        info("writeReport")
        reportWriter = report.open(REPORT_NAME)
        reportWriter.write(DASHED_LINE)
        reportWriter.write(REPORT_TITLE.center(132))
        reportWriter.write(DASHED_LINE)
        //write header
        String payLocationName = !allPayLoc ?
                getPayLocationName(batchParams.paramPayLoc) : "(Blank For All)"
        String payPeriod = convertDateFormat(endPayPeriod)
        reportWriter.write(String.format(REPORT_TITLE_2,
                batchParams.paramPayLoc, payLocationName, payPeriod).center(132))
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
        //iterate each map
        info("finalEmprec : "+finalEmpRec.size())
        finalEmpRec.each{ payLoc, employees ->
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
            employees.each{
                String status = it.reversalStatus?.trim() ? "${it.apprStatus}/${it.reversalStatus}" : "${it.apprStatus}"
                info("it.trnDate : "+it.trnDate)
                reportWriter.write(String.format(REPORT_DETAIL,
                        it.empId,
                        it.empName,
                        getDayNameFromDate(it.trnDate),
                        convertDateFormat(it.trnDate),
                        it.workCode,
                        status,
                        it.allowanceCode,
                        it.allowanceUnits,
                        it.allowanceAmount,
                        it.locationFrom,
                        it.locationTo))
            }
            reportWriter.write("")
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
                    key,
                    value))
        }
        reportErrorWriter.write(DASHED_LINE)
        reportErrorWriter.write("")
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
            if(tableDTO) {
                return tableDTO.getDescription().trim()
            }
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Error message : "+e.getMessage())
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
        info("dateS : "+dateS)
        if(dateS){
            dateS = dateS.trim().padLeft(8).replace(" ", "0")
            info("replaced dateS : "+dateS)
            String temp = dateS.substring(6) + "/" + dateS.substring(4,6) + "/" + dateS.substring(2,4)
            info("formatted      : "+temp)
            return dateS.substring(6) + "/" + dateS.substring(4,6) + "/" + dateS.substring(2,4)
        }
        return ""
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
}

/*run script*/
ProcessTrb895 process = new ProcessTrb895();
process.runBatch(binding);