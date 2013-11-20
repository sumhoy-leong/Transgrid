/**
 * @Ventyx 2012
 * This program will create a report and csv file for adjustments to employee's overtime
 * when they work before midday on a non-public holiday Saturday.
 */
package com.mincom.ellipse.script.custom;

import java.text.SimpleDateFormat
import java.util.Calendar;
import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_R1_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_R1_801Rec;
import com.mincom.ellipse.edoi.ejb.msf817.*;
import com.mincom.ellipse.edoi.ejb.msf805.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.ellipse.edoi.ejb.msf898.*;
import com.mincom.ellipse.edoi.ejb.msf8c2.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.enterpriseservice.ellipse.empaward.EmpAwardServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.empaward.EmpAwardServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.empaward.EmpAwardServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.empphysicalloc.EmpPhysicalLocServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.empphysicalloc.EmpPhysicalLocServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.empphysicalloc.EmpPhysicalLocServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;

import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

/**
 * Request Parameter for Trb983
 *
 */
public class ParamsTrb983{
    //List of Input Parameters
    String paramPayLocation;
    String paramPayPeriod;
    String paramWorkCodes0, paramWorkCodes1, paramWorkCodes2, paramWorkCodes3, paramWorkCodes4, paramWorkCodes5,
    paramWorkCodes6, paramWorkCodes7, paramWorkCodes8, paramWorkCodes9, paramWorkCodes10, paramWorkCodes11, paramWorkCodes12,
    paramWorkCodes13,paramWorkCodes14, paramWorkCodes15, paramWorkCodes16, paramWorkCodes17, paramWorkCodes18, paramWorkCodes19
    String paramTimeHalfCode;
    String paramDoubleTimeCode;
}

/**
 * Entity to hold emplyee's Saturday overtime before midday
 */

public class EmployeeSaturdayOT implements Comparable<EmployeeSaturdayOT>{
    String employeeId, employeeName
    BigDecimal rosteredStartTime, rosteredStopTime, startTime, stopTime
    String workCode, date, status, tranType, tranCode
    Integer duration

    /**
     * Construct Employee Overtime Record based on start/stop, roster, and employee information.
     * @param empStartStopRec start/stop record
     * @param empRosRec roster record
     * @param empRec employee record
     */
    public EmployeeSaturdayOT(MSF891Rec empStartStopRec, BigDecimal rostStrTime, BigDecimal rostStopTime, String empName) {
        this.employeeId        = empStartStopRec.getPrimaryKey().getEmployeeId()
        this.employeeName      = empName
        this.date              = empStartStopRec.getPrimaryKey().getTrnDate()
        this.rosteredStartTime = rostStrTime
        this.rosteredStopTime  = rostStopTime
        this.workCode          = empStartStopRec.getWorkCode()
        this.startTime         = empStartStopRec.getFromTime()
        this.stopTime          = empStartStopRec.getEndTime()
        //Set the status and Transaction Type
        this.status            = empStartStopRec.getTranApprStatus() +
                (empStartStopRec.getReverseStatus()?.trim() ? "/" + empStartStopRec.getReverseStatus() : "")
        this.tranType          = "E" //default transaction type based on URS
    }

    @Override
    public int compareTo(EmployeeSaturdayOT o) {
        int c = this.getEmployeeName().compareTo(o.getEmployeeName())
        if(c == 0) {
            c = this.getDate().compareTo(o.getDate())
        }
        return c
    }
}


public class ProcessTrb983 extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME            = "TRB983A"
    private static final String TABLE_SERVICE          = "TABLE"
    private static final String ERR_MSG_INPUT_REQUIRED = "INPUT REQUIRED"
    private static final String ERR_MSG_INPUT_NOT_EXST = "%s INPUT DOES NOT EXIST"
    private static final String TABLE_TYPE_PAYL        = "PAYL"
    private static final String MSF801_TYPE_R1         = "R1"
    private static final String MSF801_TYPE_A          = "A"
    private static final String PAY_GROUP_TG1          = "TG1"
    private static final String MSF801_TYPE_PG         = "PG"
    private static final String SERVICE_PHY_LOC        = "EMPPHYSICALLOC"
    private static final String SERVICE_AWARD          = "EMPAWARD"
    private static final String  MIN_DATE              = "19000101"
    private static final String STATUS_APPR            = "APPR"
    private static final String STATUS_RGEN            = "RGEN"
    private static final String STATUS_PAID            = "PAID"
    private static final String STATUS_RPLD            = "RPLD"
    private static final String STATUS_RVSD            = "RVSD"
    private static final String REPORT_TITLE           = "Exception Report After Lockout - Saturday Overtime Adjustments"
    private static final String REPORT_TITLE_2         = "Pay Location: %-2s - %-30s    Pay Period: %-8s"
    private static final String DASHED_LINE            = String.format("%132s"," ").replace(' ', '-')
    private static final String REPORT_SUB_HEAD_3      = "PAY LOCATION: %-2s - %-30s"
    private static final String REPORT_HEADING_1_1     = "Employee ID  Employee Name                   Day  Date      Rostered    Rostered   Work  Start  Stop   Status     Time 1/2    Double"
    private static final String REPORT_HEADING_1_2     = "                                                            Start Time  Stop Time  Code  Time   Time                          Time"
    private static final String REPORT_DETAIL_1        = "%-10s   %-30s  %-3s  %-8s  %-5s       %-5s      %-2s    %-5s  %-5s  %-9s  %-7s     %-7s"
    private static final String REPORT_ERROR_HEADING   = "Field Ref/Value                 Error/Warning Message Column Hdg "
    private static final String REPORT_ERROR_DETAIL    = "%-30s  %-99s"
    private static final String CSV_HEADER             = "Employee ID,Tran Date,Tran Type,Tran Code,Tran Units"
    private static final String CSV_DETAIL             = "%-10s,%-8s,%-1s,%-3s,%-7s"
    private static final String POSITIVE               = "+"
    private static final String NEGATIVE               = "-"

    private class OTDuration {
        Integer duration = 0
        String sign      = POSITIVE
        
        private void addDuration(Integer duration, String sign) {
            this.duration = getActualValue(this.duration, this.sign) + getActualValue(duration, sign)
            if(this.duration<0){
                this.sign = NEGATIVE
            }
            else {
                this.sign = POSITIVE
            }
            this.duration = this.duration.abs()
            if(this.duration > 120) {
                this.duration = 120
            }
        }
        
        private Integer getActualValue(Integer duration, String sign) {
            Integer returnValue = duration
            if(sign.trim().equals(NEGATIVE)) {
                returnValue = returnValue * -1
            }
            return returnValue
        }
        
        private String getAsString() {
            int hours = duration/60
            int minutes = duration%60
            return sign + String.format("%02d.%02d", hours,minutes)
        }
        
    }
    
    private class TimeAggregate {
        OTDuration halfTime   = new OTDuration()
        OTDuration doubleTime = new OTDuration()  
        
        private void addDuration(Integer duration, String halfTimeSign, String doubleTimeSign) {
            halfTime.addDuration(duration, halfTimeSign)
            doubleTime.addDuration(duration, doubleTimeSign)
        }
        
        private String getHalfTime() {
            return halfTime.getAsString()
        }
        
        private String getDoubleTime() {
            return doubleTime.getAsString()
        }
    }
    
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 5
    private ParamsTrb983 batchParams
    private boolean groupedByAllPayLoc
    private boolean headerWritten
    private LinkedHashMap<String, String> errorMessages
    private LinkedHashMap<String, List<EmployeeSaturdayOT>> employeeOT
    private LinkedHashMap<String, TimeAggregate> timeAggregates
    private String[] paramWorkCodes = new String[20]
    private String startPayPeriod, endPayPeriod
    private BigDecimal rostStrTime, rostStopTime

    /*
     * Reporting variables
     */ 
    private def reportWriter, reportErrorWriter
    private File csvFile
    private BufferedWriter csvWriter

    /**
     * Run the main batch
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb983())

        //Print the params
        populateAndPrintRequestParam()

        try {
            processBatch();

        }catch(Exception e){
            info("error ${e.getMessage()}")
        }
        finally {
            printBatchReport();
        }
    }
    /**
     * Insert Shift Worker Award Code and Overtime Work Code into array and print the request param.
     */
    private void populateAndPrintRequestParam(){
        info("populateAndPrintRequestParam")
        info("paramPayLocation        : ${batchParams.paramPayLocation}")
        info("paramPayPeriod          : ${batchParams.paramPayPeriod}")
        //print work codes params
        Class iClass = batchParams.getClass()
        (0..19).each {
            String fieldName = "paramWorkCodes${it}"
            java.lang.reflect.Field field = iClass.getDeclaredField(fieldName )
            field.setAccessible(true)
            paramWorkCodes[it] = field.get(batchParams).toString()
            info("param Work Code ${it} : ${paramWorkCodes[it]}")
        }
        info("paramTimeHalfCode       : ${batchParams.paramTimeHalfCode}")
        info("paramDoubleTimeCode     : ${batchParams.paramDoubleTimeCode}")
    }

    private void processBatch(){
        info("processBatch");
        //write process
        initialise();
        if(validateRequestParameter()){
            browseEmployeePayroll()
            constructReport()
            constructCSV()
        }
    }

    private void initialise(){
        info("initialise")
        //initialise Report
        errorMessages = new LinkedHashMap<String, String>()
        headerWritten = false
        employeeOT = new LinkedHashMap<String, List<EmployeeSaturdayOT>>()
        timeAggregates = new LinkedHashMap<String, TimeAggregate>()
    }

    private boolean validateRequestParameter(){
        info("validateRequestParameter")
        boolean valid = true
        groupedByAllPayLoc = batchParams.paramPayLocation?.trim().length() == 0
        //Validate Pay Location
        if(batchParams.paramPayLocation?.trim()){
            TableServiceReadReplyDTO tableReply = readTable(TABLE_TYPE_PAYL, batchParams.paramPayLocation)
            if(tableReply == null) {
                valid = false
                errorMessages.put("PayLocation",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayLocation))
            }
        }
        //Validate Pay Periods
        if(!batchParams.paramPayPeriod?.trim()) {
            valid = false
            errorMessages.put("PayPeriod", ERR_MSG_INPUT_REQUIRED)
        }else{
            //calculate pay periods, it should be exist
            calculatePayPeriod()
            if(startPayPeriod?.trim().length() == 0) {
                valid = false
                errorMessages.put("StartPayPeriod",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayPeriod))
            }
            if(endPayPeriod?.trim().length() == 0) {
                valid = false
                errorMessages.put("EndPayPeriod",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramPayPeriod))
            }
        }
        //validate Work Codes
        if(isWorkCodesEmpty()){
            valid = false
            errorMessages.put("WorkCodes", ERR_MSG_INPUT_REQUIRED)
        }else{
            for(String workCode : paramWorkCodes) {
                if(workCode?.trim()) {
                    if(getWorkCodeRecord(workCode) == null) {
                        valid = false
                        errorMessages.put("WorkCode",
                                String.format(ERR_MSG_INPUT_NOT_EXST, workCode))
                        break
                    }
                }
            }
        }
        //Validate Time 1/2 Code
        if(!batchParams.paramTimeHalfCode?.trim()) {
            valid = false
            errorMessages.put("TimeHalfCode", ERR_MSG_INPUT_REQUIRED)
        }else{
            if(getEarningCode(batchParams.paramTimeHalfCode) == null){
                valid = false
                errorMessages.put("TimeHalfCode",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramTimeHalfCode))
            }
        }

        //Validate double time Code
        if(!batchParams.paramDoubleTimeCode?.trim()) {
            valid = false
            errorMessages.put("DoubleTimeCode", ERR_MSG_INPUT_REQUIRED)
        }else{
            if(getEarningCode(batchParams.paramDoubleTimeCode) == null){
                valid = false
                errorMessages.put("DoubleTimeCode",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.paramDoubleTimeCode))
            }
        }
        return valid
    }

    /**
     * Browse employee start and stop based on employee payroll.
     * @param empPayrollRec employee payroll
     */
    private void browseEmployeePayroll(){
        info("browseEmployeePayroll")
        QueryImpl qMSF820 = new QueryImpl(MSF820Rec.class).
                and(MSF820Key.employeeId.greaterThan(" "))
        if(!groupedByAllPayLoc) {
            qMSF820 = qMSF820.and(MSF820Rec.payLocation.equalTo(batchParams.paramPayLocation))
        }
        edoi.search(qMSF820) {MSF820Rec empPayrollRec->
            browseEmployeeStartStop(empPayrollRec)
        }
    }
    /**
     * Browse employee start and stop based on employee payroll
     * @param empPayrollRec employee payroll
     */
    private void browseEmployeeStartStop(MSF820Rec empPayrollRec){
        info("browseEmployeeStartStop")
        int durSaturday
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.employeeId.equalTo(empPayrollRec.primaryKey.getEmployeeId())).
                and(MSF891Key.payGroup.greaterThanEqualTo(" ")).
                and(MSF891Key.trnDate.lessThanEqualTo(endPayPeriod)).
                and(MSF891Key.trnDate.greaterThanEqualTo(startPayPeriod)).
                and(MSF891Rec.tranApprStatus.equalTo(STATUS_APPR).
                or(MSF891Rec.reverseStatus.equalTo(STATUS_RPLD).
                or(MSF891Rec.reverseStatus.equalTo(STATUS_RVSD)))
                )

        edoi.search(qMSF891) {MSF891Rec empStartStopRec->
            String payLoc     = empPayrollRec.getPayLocation()
            info("employee: ${empStartStopRec.primaryKey.employeeId}")
            info("trn date: ${empStartStopRec.primaryKey.trnDate}")
            info("work code: ${empStartStopRec.workCode}")
            info("Pay location : ${payLoc}")
            if(isWorkCodeExist(empStartStopRec.workCode)){
                int startTime = convertPeriodToMinute(empStartStopRec.getFromTime())
                int endTime = convertPeriodToMinute(empStartStopRec.getEndTime())
                // get employee rostered
                getEmpRoster(empStartStopRec)
                //calculate end time
                endTime = calculateEndTime(empStartStopRec.getWorkCode(), startTime, endTime)
                //get employee name
                String empName = getEmpFormattedName(empStartStopRec.primaryKey.getEmployeeId())
                //create employee Saturday overtime
                EmployeeSaturdayOT empOt = new EmployeeSaturdayOT(empStartStopRec, rostStrTime, rostStopTime, empName)
                info("start time: ${startTime.toString()}")
                info("end time: ${endTime.toString()}")
                info("rostStrTime time: ${rostStrTime.toString()}")
                info("rostStopTime time: ${rostStopTime.toString()}")
                info("app status : ${empOt.status}")
                if(eligibleSaturdayWC(empStartStopRec)){
                    //calculate duration
                    int duration = calculateDuration(startTime, endTime)
                    info("duration is : ${duration}")
                    //pay adjusment check if duration less than 0 is invalid
                    if(duration > 0){
                        if(empStartStopRec.getTranApprStatus() == STATUS_APPR){
                            addEmpSaturdayOT(payLoc, empOt)
                            aggregateDurations(empOt,duration, POSITIVE, NEGATIVE)
                        }else if(empStartStopRec.getTranApprStatus() == STATUS_PAID && empStartStopRec.getReverseStatus() == STATUS_RPLD){
                            if(checkworkCodeAPPR(empStartStopRec)){
                                addEmpSaturdayOT(payLoc, empOt)
                                aggregateDurations(empOt,duration, NEGATIVE, POSITIVE)

                            }
                        }else if(empStartStopRec.getTranApprStatus() == STATUS_PAID && empStartStopRec.getReverseStatus() == STATUS_RVSD){
                            if(checkModifyTrn(empStartStopRec.getLastModDate(), empStartStopRec.getLastModTime())){
                                addEmpSaturdayOT(payLoc, empOt)
                                aggregateDurations(empOt,duration, NEGATIVE, POSITIVE)
                            }
                        }
                    }
                }else if(eligibleFridayNightWC(empStartStopRec, rostStopTime)){
                    //calculate duration
                    int rostEndTime = convertPeriodToMinute(rostStopTime)
                    int duration = calculateDuration(rostEndTime, endTime)
                    info("duration is : ${duration}")
                    //pay adjusment check if duration less than 0 is invalid
                    if(duration > 0){
                        if(empStartStopRec.getTranApprStatus() == STATUS_APPR){
                            addEmpSaturdayOT(payLoc, empOt)
                            aggregateDurations(empOt,duration, POSITIVE, NEGATIVE)
                        }else if(empStartStopRec.getTranApprStatus() == STATUS_PAID && empStartStopRec.getReverseStatus() == STATUS_RPLD){
                            if(checkworkCodeAPPR(empStartStopRec)){
                                addEmpSaturdayOT(payLoc, empOt)
                                aggregateDurations(empOt,duration, NEGATIVE, POSITIVE)
                            }
                        }else if(empStartStopRec.getTranApprStatus() == STATUS_PAID && empStartStopRec.getReverseStatus() == STATUS_RVSD){
                            if(checkModifyTrn(empStartStopRec.getLastModDate(), empStartStopRec.getLastModTime())){
                                addEmpSaturdayOT(payLoc, empOt)
                                aggregateDurations(empOt,duration, NEGATIVE, POSITIVE)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void aggregateDurations(EmployeeSaturdayOT empOT, Integer duration, String halfTimeSign, String doubleTimeSign) {
        info("aggregateDurations")
        
        String key = "${empOT.employeeId}${empOT.date}"
        TimeAggregate aggregate = timeAggregates.get(key)
        if(aggregate==null) {
            aggregate = new TimeAggregate()
            timeAggregates.put(key, aggregate)
        }
        aggregate.addDuration(duration, halfTimeSign, doubleTimeSign)
     
    }
    
    /**
     * check if work code is fridayNight transaction
     * @param empStartStopRec
     * @return boolean
     */
    private boolean eligibleFridayNightWC(MSF891Rec empStartStopRec, BigDecimal rostStopTime){
        info("eligibleFridayNightWC")
        //where the transaction date (MSF891-TRN-DATE) is a friday
        String workCode = empStartStopRec.getWorkCode()
        BigDecimal fromTime = empStartStopRec.getFromTime()
        Calendar cal = stringToCalendar(empStartStopRec.primaryKey.getTrnDate())
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        //get next day after trn-date
        cal.add(Calendar.DATE, 1)
        String nextTrnDate = calendarToString(cal)
        //get employee physical location as TranDate
        String physicalLoc = getEmpPhysicalLoc(empStartStopRec.primaryKey.getEmployeeId(), empStartStopRec.primaryKey.getTrnDate())
        //get employee award as trnDate
        String awardCode = getEmpAwardCode(empStartStopRec.primaryKey.getEmployeeId(), empStartStopRec.primaryKey.getTrnDate())
        // where transacation is work code prefix is 'N' trn date is Friday, emp rost stop time is 10 am or later, the day after trnDate is not holiday
        return (workCode.charAt(0) == "N" && dayOfWeek == Calendar.FRIDAY && rostStopTime >= 10.00 && isNotHoliday(nextTrnDate, physicalLoc, awardCode))
    }

    /**
     * check if work code is saturday transaction
     * @param empStartStopRec
     * @return boolean
     */
    private boolean eligibleSaturdayWC(MSF891Rec empStartStopRec){
        info("eligibleSaturdayWC")
        //where the transaction date (MSF891-TRN-DATE) is a saturday
        BigDecimal fromTime = empStartStopRec.getFromTime()
        Calendar cal = stringToCalendar(empStartStopRec.primaryKey.getTrnDate())
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        info("dayOfWeek : ${dayOfWeek}")
        info("Saturday : ${Calendar.SATURDAY}")
        info("from time : ${fromTime}")
        // get employee physical location as TranDate
        String physicalLoc = getEmpPhysicalLoc(empStartStopRec.primaryKey.getEmployeeId(), empStartStopRec.primaryKey.getTrnDate())
        // get employee award as trnDate
        String awardCode = getEmpAwardCode(empStartStopRec.primaryKey.getEmployeeId(), empStartStopRec.primaryKey.getTrnDate())
        // where transacation is saturday, fromTime less 12.00, and not in holiday
        return (dayOfWeek == Calendar.SATURDAY && fromTime < 12.00 && isNotHoliday(empStartStopRec.primaryKey.getTrnDate(), physicalLoc, awardCode))
    }

    /**
     * calculate duration of overtime before midday
     * if duration greather than 2 hour, set duration max 2 hours
     * @param startTime
     * @param endTime
     * @return int duration in minutes
     */
    private int calculateDuration(int startTime, int endTime){
        info("calculateDuration for ${startTime}, ${endTime}")
        int duration
        // 12.00 hour = 720 Minutes
        if(endTime >= 720){
            duration = 720 - startTime
        }else{
            duration = endTime - startTime
        }
        // capped at 2 hours = 120 Minutes
        if(duration > 120){
            return 120
        }else{
            return duration
        }
    }
    /**
     * Add the employee overtime record into list
     * @param payLoc pay location
     * @param emp employee overtime record
     */
    private void addEmpSaturdayOT(String payLoc, EmployeeSaturdayOT emp){
        info("addEmpSaturdayOT ${payLoc} , ${emp.employeeId}")
        List<EmployeeSaturdayOT> empList = employeeOT.get(payLoc)
        if(empList == null){
            empList = new ArrayList<EmployeeSaturdayOT>()
        }
        empList.add(emp)
        employeeOT.put(payLoc, empList)
    }

    /**
     * Check if a work code exist in the param work code.
     * @param workCode work code
     * @return true if exist
     */
    private boolean isWorkCodeExist(String workCode) {
        info("isWorkCodeExist")
        for(String workCode891 : paramWorkCodes) {
            if(workCode891?.trim().equals(workCode.trim())) {
                return true
            }
        }
        return false
    }
    /**
     * Set the global variable for roster start and stop time using edoi msf898
     * @param msf891Rec
     */
    private void getEmpRoster(MSF891Rec msf891Rec){
        info("getEmpRoster")
        
        rostStopTime = rostStrTime = 0

        MSF898Rec empRosterRec = readEmployeeRoster(msf891Rec.primaryKey.employeeId, msf891Rec.primaryKey.trnDate, STATUS_APPR)
        if(empRosterRec == null) {
            empRosterRec = readEmployeeRoster(msf891Rec.primaryKey.employeeId, msf891Rec.primaryKey.trnDate, STATUS_RGEN)
        }

        if(empRosterRec) {
            rostStopTime = empRosterRec.rostStopTime
            rostStrTime  = empRosterRec.rostStrTime
        }
        
        info("rostStopTime = ${rostStopTime.toString()}")
        info("rostStrTime = ${rostStrTime.toString()}")
    }
    
    /**
     * Read from the employee roster using key supplied
     * @param employeeId
     * @param trnDate
     * @param status
     * @return employee roster record
     */
    private MSF898Rec readEmployeeRoster(String employeeId, String trnDate, String status) {
        info("readEmployeeRoster")
        MSF898Rec result
        try {
            result = edoi.findByPrimaryKey(new MSF898Key(employeeId, trnDate, status))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            result = null
        }
        return result
    }
    
    /**
     * check if tranDate is holiday
     * @param tranDate
     * @return boolean valid
     */
    private boolean isNotHoliday(String tranDate, String phyLoc, String awardCode){
        info("isNotHoliday ${tranDate} , ${phyLoc} , ${awardCode}")
        MSF805Key msf805Key = new MSF805Key()
        MSF805Rec msf805Rec = new MSF805Rec()

        //find employee with tranDate, PhyLoc and award Code
        msf805Key.setHolDate(tranDate)
        msf805Key.setPhysicalLoc(phyLoc)
        msf805Key.setAwardCode(awardCode)
        try {
            msf805Rec = edoi.findByPrimaryKey(msf805Key)
            if(msf805Rec != null){
                return false
            }
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("date is not holiday in : ${tranDate}, phyloc: ${msf805Key.physicalLoc}, awdCode: ${msf805Key.awardCode}")
        }
        //find employee with tranDate, global phyLoc *** and award Code
        msf805Rec = null
        msf805Key.setHolDate(tranDate)
        msf805Key.setPhysicalLoc("***")
        msf805Key.setAwardCode(awardCode)
        try {
            msf805Rec = edoi.findByPrimaryKey(msf805Key)
            if(msf805Rec != null){
                return false
            }
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("date is not holiday in : ${tranDate}, phyloc: ${msf805Key.physicalLoc}, awdCode: ${msf805Key.awardCode}")
        }

        //find employee with tranDate, phyLoc and global award Code ****
        msf805Rec = null
        msf805Key.setHolDate(tranDate)
        msf805Key.setPhysicalLoc(phyLoc)
        msf805Key.setAwardCode("****")
        try {
            msf805Rec = edoi.findByPrimaryKey(msf805Key)
            if(msf805Rec != null){
                return false
            }
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("date is not holiday in : ${tranDate}, phyloc: ${msf805Key.physicalLoc}, awdCode: ${msf805Key.awardCode}")
        }

        //find employee with tranDate, global phyLoc *** and global award Code ****
        msf805Rec = null
        msf805Key.setHolDate(tranDate)
        msf805Key.setPhysicalLoc("***")
        msf805Key.setAwardCode("****")
        try {
            msf805Rec = edoi.findByPrimaryKey(msf805Key)
            if(msf805Rec != null){
                return false
            }
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("date is not holiday in : ${tranDate}, phyloc: ${msf805Key.physicalLoc}, awdCode: ${msf805Key.awardCode}")
        }
        return true
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
            tableReplyDTO = null
        }
        return tableReplyDTO
    }
    /**
     * get employee physical location using service call
     * @param empId
     * @param startDate
     * @return String Physical Location
     */
    private String getEmpPhysicalLoc(String empId, String tranDate){
        info("getEmpPhysicalLoc")
        //use service call employee physical location from msf829
        Calendar startDate = stringToCalendar(tranDate)
        EmpPhysicalLocServiceReadRequiredAttributesDTO empPhyReq = new EmpPhysicalLocServiceReadRequiredAttributesDTO()
        empPhyReq.returnEmployee = true
        empPhyReq.returnPhysicalLocation = true
        try{
            EmpPhysicalLocServiceReadReplyDTO empPhyReplyDTO = service.get(SERVICE_PHY_LOC).read({EmpPhysicalLocServiceReadRequestDTO it ->
                it.setRequiredAttributes(empPhyReq)
                it.setEmployee(empId)
                it.setStartDate(startDate)
            })
            if(empPhyReplyDTO != null && empPhyReplyDTO.physicalLocation?.trim()){
                return empPhyReplyDTO.physicalLocation
            }else{
                return ""
            }
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Cannot read service ${SERVICE_PHY_LOC} : ${e.getMessage()}")
            return ""
        }
    }

    /**
     * get Employee award code as transaction date
     * using employee award code service
     * @param empId
     * @param tranDate
     * @return String award code
     */
    private String getEmpAwardCode(String empId, String tranDate){
        info("getEmpAwardCode")
        Calendar startDate = stringToCalendar(tranDate)
        EmpAwardServiceReadRequiredAttributesDTO awdReqAtt = new EmpAwardServiceReadRequiredAttributesDTO()
        awdReqAtt.returnAwardCode = true
        try{
            EmpAwardServiceReadReplyDTO empAwardDTO = service.get(SERVICE_AWARD).read({EmpAwardServiceReadRequestDTO it ->
                it.setRequiredAttributes(awdReqAtt)
                it.setEmployee(empId)
                it.setStartDate(startDate)
            })
            if(empAwardDTO != null){
                return empAwardDTO.awardCode
            }else{
                return ""
            }
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Error when read award Service ${e.getMessage()}")
            return ""
        }
    }

    /**
     * Get WorkCode based on the work code.
     * @param workCode work code
     * @return MSF801_R1_801Rec
     */
    private MSF801_R1_801Rec getWorkCodeRecord(String workCode) {
        info("getWorkCodeRecord")
        MSF801_R1_801Key workCodeKey = new MSF801_R1_801Key()
        workCodeKey.setCntlKeyRest("***${workCode}")
        workCodeKey.setCntlRecType(MSF801_TYPE_R1)
        MSF801_R1_801Rec workCodeRec
        try {
            workCodeRec = edoi.findByPrimaryKey(workCodeKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            workCodeRec = null
        }
        return workCodeRec
    }

    /**
     * Get timehalfcode based on the halfcode.
     * @param workCode work code
     * @return MSF801_A_801Rec
     */
    private MSF801_A_801Rec getEarningCode(String earnCode) {
        info("getEaringCode")
        MSF801_A_801Key earnCodeKey = new MSF801_A_801Key()
        earnCodeKey.setCntlKeyRest("***${earnCode}")
        earnCodeKey.setCntlRecType(MSF801_TYPE_A)
        MSF801_A_801Rec earnCodeRec
        try {
            earnCodeRec = edoi.findByPrimaryKey(earnCodeKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            earnCodeRec = null
        }
        return earnCodeRec
    }

    /**
     * Check if Overtime Work Codes are empty.
     * @return true if Overtime Work Codes are empty
     */
    private boolean isWorkCodesEmpty() {
        info("isWorkCodesEmpty")
        for(String workCodes : paramWorkCodes) {
            if(workCodes?.trim()) {
                return false
            }
        }
        return true
    }

    /**
     * Calculate the pay period.
     */
    private void calculatePayPeriod() {
        info("calculatePayPeriod")
        MSF801_PG_801Rec payGroupRec = getPayGroupRecord(PAY_GROUP_TG1)
        if(payGroupRec) {
            endPayPeriod = payGroupRec.getCurEndDtPg()
            //convert to java Calendar
            Calendar cal = Calendar.getInstance()
            cal.clear()
            cal.set(endPayPeriod.substring(0, 4) as int, //year
                    (endPayPeriod.substring(4, 6) as int) - 1, //month, starts from 0
                    endPayPeriod.substring(6, 8) as int) //day
            //calculate pay period substraction
            int payPeriod_9 = (batchParams.paramPayPeriod as int) * -1
            //substract the week, validate the result against
            //http://www.timeanddate.com/date/dateadd.html
            cal.add(Calendar.WEEK_OF_YEAR, payPeriod_9)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            startPayPeriod = new java.text.SimpleDateFormat("yyyyMMdd").format(cal.getTime())
            info("endPayPeriod   : ${endPayPeriod}")
            info("startPayPeriod : ${startPayPeriod}")
        }
    }
    /**
     * calculate end time against msf8c2 min hrs to paid
     * @param startTime
     * @param endTime
     * @return int endTime
     */
    private int calculateEndTime(String workCode, int startTime, int endTime){
        info("calculateEndTime")
        int equivEndTime = 0
        QueryImpl qMSF8c2 = new QueryImpl(MSF8C2Rec.class).and(MSF8C2Key.awardCodeC2.greaterThanEqualTo(" ")).and(MSF8C2Key.workCodeC2.equalTo(workCode))
        MSF8C2Rec msf8c2Rec = edoi.firstRow(qMSF8c2)
        if(msf8c2Rec != null){
            if(msf8c2Rec.getMinHrsPdC2() > 0){
                if(msf8c2Rec.getMinHrsExtendC2() == "Y"){
                    //add fromTime to 2 hours = 120 minutes
                    equivEndTime = startTime + 120
                }else{
                    //add fromTime with min hrs to paid
                    equivEndTime = startTime + convertPeriodToMinute(msf8c2Rec.getMinHrsPdC2())
                }
            }
        }
        return equivEndTime > endTime ? equivEndTime : endTime
    }

    private String getEmpFormattedName(String empId){
        info("getEmpFormattedName")
//        /*
//         * find employee name using screen service
//         * since employee service call required access 9
//         */
//        ScreenAppLibrary sl = new ScreenAppLibrary()
//        EmployeePersonnelDTO empPersDTO = new EmployeePersonnelDTO()
//        empPersDTO.setNextEemployeeId(empId)
//        EmployeePersonnelResultDTO empPersReplyDTO = sl.readEmployeePersonnelDetails(empPersDTO)
//        if(!empPersReplyDTO.errorMessage?.trim()){
//            return empPersReplyDTO.name
//        }else{
//            return ""
//        }
        
        /*
         * Using EDOI for formatted name as approved by Peter Deacon 
         * in regards of performance issue when using screen service.
         * 
         */
        String empName = " "
        try {
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(empId))
            empName = "${msf810Rec.surname.trim()}, ${msf810Rec.firstName.trim()}"
            if(!msf810Rec.secondName.trim().equals("")) {
                empName = "${empName} ${msf810Rec.secondName.trim()}"
            }
            if(!msf810Rec.thirdName.trim().equals("")) {
                empName = "${empName} ${msf810Rec.thirdName.trim()}"
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {            
        }
        return empName;
    }

    /**
     * Get Pay Group records based on the pay code.
     * @param payCode Pay Group code
     * @return MSF801_PG_801Rec
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
     * Convert Period (HH:mm) into minutes.
     * @param bd period as big decimal
     * @return minutes
     */
    private int convertPeriodToMinute(BigDecimal bd){
        info("convertPeriodToMinute")
        int minutes = 0
        int[] parse = breakBigDecimal(bd)
        return (parse[0] * 60) + parse[1]
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
     * convert String to calendar with format ddMMyyyy
     * @param String sDate
     * @return Calendar
     */
    private Calendar stringToCalendar(String sDate){
		info("stringToCalendar")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
        Calendar calDate = Calendar.getInstance()
        sdf.setLenient(false)
        if(sDate?.trim() && sDate != "00000000"){
            Date date = sdf.parse(sDate)
            calDate.setTime(date)
        }else{
            calDate = null
        }
        return calDate
    }

    /**
     * convert Calendar type of date to String value
     * with format yyyyMMdd
     * @param calDate
     * @return String
     */
    private String calendarToString(Calendar calDate){
        info("calendarToString")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
        try{
            sdf.setLenient(false)
            if(sdf.format(calDate.getTime()) >= MIN_DATE){
                return sdf.format(calDate.getTime())
            }else{
                return "00000000"
            }
        }catch(java.text.ParseException e){
            return "00000000"
        }
    }

    /**
     * Construct report content
     */
    private void constructReport(){
        info("constructReport")
        //write header
        writeReportHeader()
        //iterate each map
        Boolean first = true
        employeeOT.each{payLoc, employeeOT->
            if(groupedByAllPayLoc){
                String payLocationName = getPayLocationName(payLoc)
                if(first) {
                    first=false
                }
                else {
                    reportWriter.write(DASHED_LINE)
                }
                reportWriter.write("")
                reportWriter.write(String.format(REPORT_SUB_HEAD_3, payLoc, payLocationName))
                reportWriter.write("")
            }
            writeEmployeeSatOTReport(employeeOT)
        }
        //Write closing line after last record
        reportWriter.write(DASHED_LINE)
    }
    /**
     * Construct the csv.
     */
    private void constructCSV(){
        info("constructCSV")
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
        //iterate each map
        employeeOT.each {payLoc, employeesEach->
            Collections.sort(employeesEach)
            String currentKey = ""
            employeesEach.each {
                String key = "${it.employeeId}${it.date}"
                String halfTime = ""
                String doubleTime = ""
                if(!currentKey.trim().equals(key)) {
                    currentKey = key
                    TimeAggregate aggregate = timeAggregates.get(currentKey)
                    if(aggregate) {
                        halfTime = aggregate.getHalfTime()
                        doubleTime = aggregate.getDoubleTime()
                    }


                it.tranCode = batchParams.paramDoubleTimeCode
                csvWriter.write(String.format(CSV_DETAIL,
                        it.employeeId,
                        it.date,
                        it.tranType,
                        it.tranCode,
                        doubleTime
                        ))
                csvWriter.write("\r\n")
                it.tranCode = batchParams.paramTimeHalfCode
                csvWriter.write(String.format(CSV_DETAIL,
                        it.employeeId,
                        it.date,
                        it.tranType,
                        it.tranCode,
                        halfTime
                        ))
                csvWriter.write("\r\n")
                }
            }
        }
    }

    /**
     * write report header
     */
    private void writeReportHeader(){
        info("writeReportHeader")
        if(!headerWritten){
            reportWriter = report.open(REPORT_NAME)
            reportWriter.write(REPORT_TITLE.center(132))
            reportWriter.write(DASHED_LINE)
            String payLocationName = !groupedByAllPayLoc ?
                    getPayLocationName(batchParams.paramPayLocation) : "(Blank For All)"
            String payPeriod = convertDateFormat(endPayPeriod)
            reportWriter.write(String.format(REPORT_TITLE_2,
                    batchParams.paramPayLocation, payLocationName, payPeriod).center(132))
            reportWriter.write(DASHED_LINE)
            reportWriter.write("")
            headerWritten = true
        }
    }
    /**
     * Write emplyee list into report
     * @param employees Employee List
     */
    private void writeEmployeeSatOTReport(ArrayList<EmployeeSaturdayOT> employees){
        info("writeEmployeeSatOTReport")
        reportWriter.write(DASHED_LINE)
        reportWriter.write(REPORT_HEADING_1_1)
        reportWriter.write(REPORT_HEADING_1_2)
        reportWriter.write(DASHED_LINE)
        Collections.sort(employees)
        String currentKey = ""
        Boolean first = true
        employees.each {
            String key = "${it.employeeId}${it.date}"
            String halfTime = ""
            String doubleTime = ""
            if(!currentKey.trim().equals(key)) {
                currentKey = key
                TimeAggregate aggregate = timeAggregates.get(currentKey)
                if(aggregate) {
                    halfTime = aggregate.getHalfTime()
                    doubleTime = aggregate.getDoubleTime()
                }
                if(first) {
                    first = false
                }
                else {
                    reportWriter.write("")
                }
            } 
            
             
            reportWriter.write(String.format(REPORT_DETAIL_1,
                    it.employeeId,
                    it.employeeName,
                    getDayNameFromDate(it.date),
                    convertDateFormat(it.date),
                    convertTimeFormat(it.rosteredStartTime),
                    convertTimeFormat(it.rosteredStopTime),
                    it.workCode,
                    convertTimeFormat(it.startTime),
                    convertTimeFormat(it.stopTime),
                    it.status,
                    halfTime,
                    doubleTime
                    ))
        }
    }

    /**
     * Write error report.
     */
    private void writeErrorReport() {
        info("writeErrorReport")
        writeReportHeader()
        reportWriter.write(REPORT_ERROR_HEADING)
        reportWriter.write(DASHED_LINE)
        errorMessages.each {key, value->
            reportWriter.write(String.format(REPORT_ERROR_DETAIL,
                    key?.length() > 30 ? key?.substring(0,30) : key,
                    value?.length() > 99 ? value?.substring(0,99) : value))
        }
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
    }
    /**
     * check if there is corresponding approved transaction
     * for the same work code on the same date.
     * @param empTimesheetRec
     * @return boolean true or false
     */
    private boolean checkworkCodeAPPR(MSF891Rec empTimesheetRec){
        info("checkworkCodeAPPR")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.payGroup.equalTo(empTimesheetRec.primaryKey.getPayGroup())).
                and(MSF891Key.trnDate.equalTo(empTimesheetRec.primaryKey.getTrnDate())).
                and(MSF891Key.employeeId.equalTo(empTimesheetRec.primaryKey.getEmployeeId())).
                and(MSF891Rec.workCode.equalTo(empTimesheetRec.workCode)).
                and(MSF891Rec.tranApprStatus.equalTo(STATUS_APPR))
        MSF891Rec msf891Rec = edoi.firstRow(qMSF891)
        return msf891Rec != null
    }

    private boolean checkModifyTrn(String lastModDate, String lastModTime){
        info("checkModifyTrn")
        String invEndDate
        QueryImpl qMSF817 = new QueryImpl(MSF817Rec.class).
                and(MSF817Key.payRunType.equalTo("U")).
                and(MSF817Key.payGroup.equalTo(PAY_GROUP_TG1)).
                min(MSF817Key.invEndDate)
        invEndDate = edoi.firstRow(qMSF817)
        info("invdate min is : ${invEndDate}")
        if(invEndDate?.trim()){
            MSF817Rec payUpdateHistRec
            qMSF817 = new QueryImpl(MSF817Rec.class).
                    and(MSF817Key.payGroup.equalTo(PAY_GROUP_TG1)).
                    and(MSF817Key.payRunType.equalTo("U")).
                    and(MSF817Key.invEndDate.equalTo(invEndDate))

            payUpdateHistRec = edoi.firstRow(qMSF817)
            info("invdate is : ${payUpdateHistRec.getPrimaryKey().getInvEndDate()}")
            info("Run Date is : ${payUpdateHistRec.getRunDate()}")
            info("lastmodDate: ${lastModDate}")

            return (payUpdateHistRec.getRunDate() < lastModDate || (payUpdateHistRec.getRunDate() == lastModDate && payUpdateHistRec.getRunTime() < lastModTime))
        }else{
            return false
        }
    }

    /**
     * Get the Pay Location name
     * @param payLocation Pay Location code
     * @return Pay Location name
     */
    private String getPayLocationName(String payLocation) {
        info("getPayLocationName")
        TableServiceReadReplyDTO tableDTO = readTable(TABLE_TYPE_PAYL, payLocation)
        if(tableDTO) {
            return tableDTO.getDescription().trim()
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
     * Convert the date format with specified separator <code>(YY/MM/DD)</code>.
     * @param dateS date as a String
     * @return formatted date as a String
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat")
        if(dateS?.trim()){
            dateS = dateS.padLeft(8).replace(" ", "0")
        }else{
            dateS = "00000000"
        }
        return dateS.substring(6) + "/" + dateS.substring(4,6) + "/" + dateS.substring(2,4)
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
     * Close the report
     */
    private void printBatchReport(){
        info("printBatchReport")
        //Create error message report - if any
        if(!errorMessages.isEmpty()) {
            writeErrorReport()
            //reportErrorWriter.close()
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
        if(csvFile != null && taskUUID?.trim()) {
            request.request.CURRENT.get().addOutput(csvFile,
                    "text/comma-separated-values", REPORT_NAME)
        }
    }
}

/*run script*/  
ProcessTrb983 process = new ProcessTrb983();
process.runBatch(binding);