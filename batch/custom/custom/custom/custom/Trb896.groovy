/**
 * @Ventyx 2012
 * 
 * This report is to produce a list of all employees who have had overtime
 * to be processed in the current period and have not had their minimum breaks.
 * 
 * This program developed using edoi instead of service call.
 * 
 * Developed based on <b>URS.Exception Report - OT Minimum Breaks.pdf</b>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf817.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf828.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.ellipse.edoi.ejb.msf898.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceCreateReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;

import java.text.SimpleDateFormat;
import groovy.time.*;

import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*

/**
 * Request parameters for Trb896.
 */
public class ParamsTrb896{
    private String payLocation
    private String payPeriod
    private String shiftWorkerAward0, shiftWorkerAward1, shiftWorkerAward2, shiftWorkerAward3,
    shiftWorkerAward4, shiftWorkerAward5, shiftWorkerAward6, shiftWorkerAward7, shiftWorkerAward8,
    shiftWorkerAward9, shiftWorkerAward10, shiftWorkerAward11, shiftWorkerAward12, shiftWorkerAward13,
    shiftWorkerAward14, shiftWorkerAward15, shiftWorkerAward16, shiftWorkerAward17, shiftWorkerAward18,
    shiftWorkerAward19;
    private String overtimeThreshold
    private String dayMinBreakThresholdGT
    private String dayMinBreakThresholdLET
    private String shiftMinBreakThresholdGT
    private String shiftMinBreakThresholdLET
}

/**
 * Entity to store Overtime Minimum Breaks record
 */
public class OvertimeFoundTrb896 implements Comparable<OvertimeFoundTrb896>{
    String employeeId   = " "
    String employeeName = " "
    String tranDate     = " "
    String startTime    = " "
    String stopTime     = " "
    String workCode     = " "
    String shiftType    = " "
    String rostStart    = " "
    String rostStop     = " "
    String status       = " "
    String revsdStatus  = " "
    String payLocation  = " "
    String lastModDate  = " "
    String lastModTime  = " "

    public int compareTo(OvertimeFoundTrb896 otherRecord){
        if (!payLocation.equals(otherRecord.payLocation)){
            return payLocation.compareTo(otherRecord.payLocation)
        }
        if (!employeeName.equals(otherRecord.employeeName)){
            return employeeName.compareTo(otherRecord.employeeName)
        }
        if (!employeeId.equals(otherRecord.employeeId)){
            return employeeId.compareTo(otherRecord.employeeId)
        }
        if (!tranDate.equals(otherRecord.tranDate)){
            return tranDate.compareTo(otherRecord.tranDate)
        }
        if (!startTime.equals(otherRecord.startTime)){
            return startTime.compareTo(otherRecord.startTime)
        }
        if (!stopTime.equals(otherRecord.stopTime)){
            return stopTime.compareTo(otherRecord.stopTime)
        }
        return 0;
    }

    public Boolean isEqual(OvertimeFoundTrb896 other) {
        return employeeId.equals(other.employeeId) && employeeName.equals(other.employeeName) && tranDate.equals(other.tranDate) && startTime.equals(other.startTime) && stopTime.equals(other.stopTime) && workCode.equals(other.workCode) && shiftType.equals(other.shiftType) && rostStart.equals(other.rostStart) && rostStop.equals(other.rostStop) && status.equals(other.status) && revsdStatus.equals(other.revsdStatus) && payLocation.equals(other.payLocation) && lastModDate.equals(other.lastModDate) && lastModTime.equals(other.lastModTime)
    }


    public String toString() {
        String writtenString = "     "

        writtenString         = writtenString + this.employeeId.padRight(14)
        writtenString         = writtenString + this.employeeName.padRight(42)

        Calendar rostStart    = TimeHelperTrb896.getTimeInCalendar(this.tranDate, this.rostStart)
        writtenString         = writtenString + rostStart.format("EEE").padRight(5)
        writtenString         = writtenString + rostStart.format("dd/MM/yy").padRight(10)
        writtenString         = writtenString + rostStart.format("HH:mm").padRight(12)

        writtenString         = writtenString + TimeHelperTrb896.getTimeInCalendar(this.tranDate, this.rostStop).format("HH:mm").padRight(10)
        writtenString         = writtenString + this.workCode.padRight(6)
        writtenString         = writtenString + TimeHelperTrb896.getTimeInCalendar(this.tranDate, this.startTime).format("HH:mm").padRight(7)
        writtenString         = writtenString + TimeHelperTrb896.getTimeInCalendar(this.tranDate, this.stopTime).format("HH:mm").padRight(7)
        writtenString         = writtenString + this.status

        if(this.revsdStatus?.trim()) {
            writtenString         = writtenString + "/" + this.revsdStatus
        }

        return writtenString
    }
}

public class TimeHelperTrb896 {
    /**
     * Returns a calendar representation of the time which is the result of adding the addTimeInMillis to the input time.
     * @param inputTime
     * @param addTimeInMillis
     * @return a calendar representation of the time which is the result of adding the addTimeInMillis to the input time.
     */
    public static Calendar addTimeInMilliseconds(Calendar inputTime, Long addTimeInMillis) {
        Calendar cal = Calendar.getInstance()
        cal.clear()
        cal.setTimeInMillis(inputTime.getTimeInMillis()+addTimeInMillis)
        return cal
    }


    /**
     * Return the Calendar representation of the date/time entered.
     * @param date
     * @param time
     * @return the Calendar representation of the date/time entered.
     */
    public static Calendar getTimeInCalendar(String date, String time) {
        Calendar cal = Calendar.getInstance()
        cal.clear()
        cal.setTime(getTimeInDate(date,time))
        return cal
    }

    /**
     * Return the Date representation of the date/time entered.
     * @param date
     * @param time
     * @return the Date representation of the date/time entered.
     */
    public static Date getTimeInDate(String date, String time) {
        String usedTime = time
        if(usedTime.equals("2400")) {
            usedTime = "0000"
        }
        SimpleDateFormat dateParser = new SimpleDateFormat("yyyyMMddHHmm")
        dateParser.setLenient(false)
        return dateParser.parse(date+usedTime)
    }

    /**
     * Return the duration between two time in a day.
     * When the stop time is smaller then stop time it is assumed that the stop time is in the next day
     * @param tranDate
     * @param startTime
     * @param stopTime
     * @return the duration between two time in a day.
     */
    public static TimeDuration getOvertimePeriod(String date, String startTime, String stopTime) {

        Date start = getTimeInDate(date,startTime)

        String stopDate = date
        if(stopTime.toBigInteger()<startTime.toBigInteger()) {
            stopDate  = getAddedDate(date, Calendar.DAY_OF_YEAR, 1)
        }

        Date stop = getTimeInDate(stopDate, stopTime)

        return TimeCategory.minus(stop, start)
    }

    /**
     * Get the date after added based on input. Both the input and the returned date is in yyyyMMdd format.
     * The column is field to be added, e.g. if we want to get 1 week after the date then we use Calendar.WEEK_OF_YEAR.
     * @param column
     * @param inputDate
     * @return the date one day after the input date in yyyyMMdd format.
     */
    public static String getAddedDate(String inputDate, Integer column, Integer numberOfAddition) {
        Calendar cal = TimeHelperTrb896.getTimeInCalendar(inputDate, "0000")
        //http://www.timeanddate.com/date/dateadd.html
        cal.add(column, numberOfAddition)
        return (cal.format("yyyyMMdd"))
    }
}

/**
 * Entity to store Overtime Aggregates 
 */
public class OvertimeAggregatesTrb896 {
    TimeDuration sumOfPeriod = new TimeDuration(0,0,0,0)
    String firstStartTime    = "000000"
    String firstTranDate     = "00000000"
    String lastStopTime      = "000000"
    String lastTranDate      = "00000000"

    public Boolean isBlank() {
        return sumOfPeriod.toMilliseconds()==0 && lastStopTime.equals("000000") && lastTranDate.equals("00000000")
    }
}

/**
 * Main process of Trb896.
 */
public class ProcessTrb896 extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME            = "TRB896A"
    private static final String ERR_MSG_INPUT_REQUIRED = "INPUT REQUIRED"
    private static final String ERR_MSG_INPUT_NOT_EXST = "%s INPUT DOES NOT EXIST"
    private static final String TABLE_TYPE_PAYL        = "PAYL"
    private static final String PAY_GROUP_TG1          = "TG1"
    private static final String MSF801_TYPE_C0         = "C0"
    private static final String MSF801_TYPE_PG         = "PG"
    private static final String MSF801_TYPE_R1         = "R1"
    private static final String POST_SHIFT             = "Post Shift"
    private static final String PRE_SHIFT              = "Pre Shift"
    private static final String ROSTERED_OFF           = "Rostered Off"

    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 2;

    private ParamsTrb896 batchParams;
    private String[] shiftWorkerAwards = new String[20]
    private String startPayPeriod, endPayPeriod
    private ArrayList<OvertimeFoundTrb896> listOfEmpOvertime
    private ArrayList<OvertimeFoundTrb896> listOfPrintedEmpOvertime
    private Long overtimeThresholdInMillis         = 0
    private Long dayMinBreakThresholdGTInMillis    = 0
    private Long dayMinBreakThresholdLTEInMillis   = 0
    private Long shiftMinBreakThresholdGTInMillis  = 0
    private Long shiftMinBreakThresholdLTEInMillis = 0
Binding binding;
    /*
     * Reporting variables 
     */
    private def reportWriter

    /**
     * Run the main batch
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

       binding = b;
info("lwh binding 01: "+binding)
       init(b);
info("lwh binding 02: "+binding)

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb896())

        //PrintRequest Parameters
        populateAndPrintRequestParams()

        try {
            processBatch();
        } catch(Exception e) {
            info("error ${e.printStackTrace()}")
            e.printStackTrace()
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB896 ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")

			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			e.printStackTrace(printWriter);
			info (writer.toString())
        } finally {
            if(reportWriter != null) {
                reportWriter.close()
            }
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch");
        initialize()
        if (isValidRequestParameters()) {
            overtimeThresholdInMillis         = ((batchParams.overtimeThreshold.toBigDecimal())         * 3600000).toLong()
            dayMinBreakThresholdGTInMillis    = ((batchParams.dayMinBreakThresholdGT.toBigDecimal())    * 3600000).toLong()
            dayMinBreakThresholdLTEInMillis   = ((batchParams.dayMinBreakThresholdLET.toBigDecimal())   * 3600000).toLong()
            shiftMinBreakThresholdGTInMillis  = ((batchParams.shiftMinBreakThresholdGT.toBigDecimal())  * 3600000).toLong()
            shiftMinBreakThresholdLTEInMillis = ((batchParams.shiftMinBreakThresholdLET.toBigDecimal()) * 3600000).toLong()
            ArrayList<OvertimeFoundTrb896> listOfPrintedOvertimes = browseEmployeePayroll(batchParams.payLocation.trim())
            if(!listOfPrintedOvertimes.isEmpty()) {
                printOvertimes(listOfPrintedOvertimes)
            }
        }

    }

    /**
     * Write the overtime list to the report
     * @param listOfPrintedOvertimes
     */
    private void printOvertimes(ArrayList<OvertimeFoundTrb896> listOfPrintedOvertimes) {
        info("printOvertimes");

        Collections.sort(listOfPrintedOvertimes)

        String currentPayLocation = " "
        String prevDate           = " "
        for(overtime in listOfPrintedOvertimes) {
            if( !prevDate.equals(overtime.tranDate)) {
                reportWriter.write(" ")
                prevDate = overtime.tranDate
            }
            if(!overtime.payLocation.equals(currentPayLocation)) {
                currentPayLocation = overtime.payLocation
                printReportFieldHeader(currentPayLocation)
            }
            reportWriter.write(overtime.toString())
        }
    }

    /**
     * Print the field header  in the report.
     */
    private void printReportFieldHeader(String payLocation){
        info("printReportFieldHeader")

        reportWriter.write("")
        reportWriter.write("")
        TableServiceReadReplyDTO payLocationDescription = readTable("PAYL", payLocation)
        if(payLocationDescription!=null) {
            reportWriter.write("PAY LOCATION: " + payLocation + " - " + payLocationDescription.getDescription())
        }
        else {
            reportWriter.write("PAY LOCATION: " + payLocation + " - Description Not Found")
        }

        reportWriter.write("")
        reportWriter.write("     Employee ID   Employee Name                             Day  Date      Rostered    Rostered  Work  Start  Stop   Status")
        reportWriter.write("                                                                            Start Time  Stop Time Code  Time   Time")
        reportWriter.write("     --------------------------------------------------------------------------------------------------------------------------")

    }

    /**
     * Print request parameters.
     */
    private void populateAndPrintRequestParams() {
        info("populateAndPrintRequestParams")
        info("Pay Location                : ${batchParams.payLocation}")
        info("Pay Periods to Process      : ${batchParams.payPeriod}")
        //Shift Awards
        Class iClazz = batchParams.getClass()
        (0..19).each {
            String fieldName = "shiftWorkerAward${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName)
            field.setAccessible(true)
            shiftWorkerAwards[it] = field.get(batchParams).toString().trim()
            info("Shift Worker Award ${it}    : ${shiftWorkerAwards[it]}")
        }
        info("Overtime Threshold          : ${batchParams.overtimeThreshold}")
        info("Day Min Break > Threshold   : ${batchParams.dayMinBreakThresholdGT}")
        info("Day Min Break <= Threshold  : ${batchParams.dayMinBreakThresholdLET}")
        info("Shift Min Break > Threshold : ${batchParams.shiftMinBreakThresholdGT}")
        info("Shift Min Break <= Threshold: ${batchParams.shiftMinBreakThresholdLET}")
    }

    /**
     * Initialize report writer.
     */
    private void initialize() {
        info("initialize")
        //Initialize Report
        reportWriter   = report.open(REPORT_NAME)
        reportWriter.write("Exception Report - Overtime Minimum Breaks".center(132));
        reportWriter.write(" ");
        reportWriter.write("".padLeft(132,"-"));
        reportWriter.write(" ");

    }

    /**
     * Validate request parameters.
     * @return true if request parameters are valid, false if otherwise
     */
    private Boolean isValidRequestParameters() {
        info("isValidRequestParameters")
        Boolean valid = true

        //Validate Shift Worker Awards
        if(isShiftAwardCodesEmpty()) {
            valid = false
            reportWriter.write("  Shift Award Codes - " + ERR_MSG_INPUT_REQUIRED)
        } else {
            for(String awardCode : shiftWorkerAwards) {
                if(awardCode?.trim()) {
                    if(getAwardRecord(awardCode) == null) {
                        valid = false
                        reportWriter.write("  Shift Award Code - " + String.format(ERR_MSG_INPUT_NOT_EXST, awardCode))
                        break
                    }
                }
            }
        }

        //        if(!isValidThreshold(batchParams.overtimeThreshold)) {
        //            reportWriter.write("  Overtime threshold must be numeric with a maximum of 2 decimals ")
        //            valid = false
        //        }
        //
        //        if(!isValidThreshold(batchParams.dayMinBreakThresholdGT)) {
        //            reportWriter.write("  DayMinBreak > Threshold must be numeric with a maximum of 2 decimals ")
        //            valid = false
        //        }
        //
        //        if(!isValidThreshold(batchParams.dayMinBreakThresholdLET)) {
        //            reportWriter.write("  DayMinBreak <= Threshold must be numeric with a maximum of 2 decimals ")
        //            valid = false
        //        }
        //
        //        if(!isValidThreshold(batchParams.shiftMinBreakThresholdGT)) {
        //            reportWriter.write("  ShiftMinBreak > Threshold must be numeric with a maximum of 2 decimals ")
        //            valid = false
        //        }
        //
        //        if(!isValidThreshold(batchParams.shiftMinBreakThresholdLET)) {
        //            reportWriter.write("  ShiftMinBreak <= Threshold must be numeric with a maximum of 2 decimals")
        //            valid = false
        //        }

        //calculate pay periods, it should exist
        try {
            endPayPeriod = getEndPayPeriod(PAY_GROUP_TG1)
            startPayPeriod = getStartPayPeriod(endPayPeriod, batchParams.payPeriod.toInteger())
            info("startPayPeriod: "+startPayPeriod)
            info("endPayPeriod: "+endPayPeriod)
            if(valid) {
                reportWriter.write("Period start: ${TimeHelperTrb896.getTimeInCalendar(startPayPeriod, "0000").format("dd/MM/yy")}     Period end: ${TimeHelperTrb896.getTimeInCalendar(endPayPeriod, "0000").format("dd/MM/yy")}".center(132))
            }
        } catch(Exception e) {
            reportWriter.write("Start and End Pay period cannot be determined")
            valid = false
        }

        return valid
    }

    /**
     * Browse all employee in payroll and return all overtime transactions that will be printed.
     * If the pay location is not spaces, we will only take employees with that pay location.
     * @param payLocation
     * @return the list of all printed overtime transactions.
     */
    private ArrayList<OvertimeFoundTrb896> browseEmployeePayroll(String payLocation) {
        info("browseEmployeePayroll")
        QueryImpl qMSF820 = new QueryImpl(MSF820Rec.class).and(MSF820Key.employeeId.greaterThanEqualTo(" "))

        if (payLocation?.trim()) {
            qMSF820.and(MSF820Rec.payLocation.equalTo(payLocation))
        }
        qMSF820.orderBy(MSF820Rec.msf820Key)
        ArrayList<OvertimeFoundTrb896> listOfPrintedOvertimes = new ArrayList<OvertimeFoundTrb896>()
        edoi.search(qMSF820, 100, {MSF820Rec empPayrollRec->
            ArrayList<OvertimeFoundTrb896> allOvertime = getEmployeeOvertimes(empPayrollRec.primaryKey.employeeId, startPayPeriod, endPayPeriod, empPayrollRec.payLocation)
            if(!allOvertime.isEmpty()) {
                ArrayList<OvertimeFoundTrb896> processedOvertime = processEmployeeOvertimes(allOvertime)
                if(!processedOvertime.isEmpty()) {
                    listOfPrintedOvertimes.addAll(processedOvertime)
                }
            }
info("lwh binding 03: "+binding)
activateSession(binding);
        })

        return listOfPrintedOvertimes
    }

int actCounter=0;
	private void activateSession(Binding b) {
		info("activateSession ~ lwh testing method");
		actCounter++;

		String ellipseVersion;
			
info("lwh binding 04: "+binding)
		info("lwh binding 05: "+b)
		CommAreaScriptWrapper commarea = b.getVariable("commarea");
		info ("lwh Version:" + version.toString())
		info ("lwh actCounter:" + actCounter)
		info ("lwh commarea:" + commarea)
		//info ("lwh commarea.getProperty():" + commarea.getProperty("data"))
		info ("lwh commarea.getProperties():" + commarea.getProperties())
		//ellipseVersion = commarea. commarea.systemVersion + "." + commarea.releaseVersion
   }



    /**
     * Browse employee start stop for entries with parameters as entered to get the employee overtimes.
     * @param employeeId
     * @param payGroup
     * @param startPeriod
     * @param endPeriod
     * @return list of employee overtimes
     */
    private ArrayList<OvertimeFoundTrb896> getEmployeeOvertimes(String employeeId, String startPeriod, String endPeriod, String payLocation) {
        debug("browseEmployeeStartStop")
        ArrayList<OvertimeFoundTrb896> listOfEmployeeOvertimes = new ArrayList<OvertimeFoundTrb896>()

        Constraint cEmployeeId = MSF891Key.employeeId.equalTo(employeeId)
        Constraint cEndDate    = MSF891Key.trnDate.lessThanEqualTo(endPeriod)
        Constraint cStartDate  = MSF891Key.trnDate.greaterThanEqualTo(startPeriod)
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).and(cEmployeeId).and(cStartDate).and(cEndDate)
		//qMSF891.orderBy(MSF891Rec.msf891Key)

        edoi.search(qMSF891) {MSF891Rec empStartStopRec->
            MSF801_R1_801Rec workCodeRec = getWorkCodeRecord(empStartStopRec.workCode)
            if(workCodeRec!=null) {
                MSF801_R1_801Rec workCodeRoster
                MSF898Rec empRosterRec = getEmployeeRoster(empStartStopRec.primaryKey.employeeId, empStartStopRec.primaryKey.trnDate)
                if(empRosterRec!=null && empRosterRec.getWorkCode()?.trim()){
                    workCodeRoster = getWorkCodeRecord(empRosterRec.workCode)
                }
                if (empRosterRec!=null && isPostOrPreShiftOvertime(empStartStopRec, empRosterRec, workCodeRec.shiftTypeR1, workCodeRoster.shiftTypeR1)) {
                    listOfEmployeeOvertimes.add(populateOvertimeRec(empStartStopRec,empRosterRec, workCodeRoster.shiftTypeR1, payLocation))
                }
            }

        }
        return listOfEmployeeOvertimes
    }

    /**
     * Create and return a new OvertimeFoundTrb896 object with values taken from the parameters
     * @param empStartStopRec
     * @param empRosterRec
     * @param shiftType
     * @param payLocation
     * @return new OvertimeFoundTrb896 object
     */
    private OvertimeFoundTrb896 populateOvertimeRec(MSF891Rec empStartStopRec, MSF898Rec empRosterRec, String shiftType, String payLocation) {
        debug("populateOvertimeRec")

        OvertimeFoundTrb896 overtime = new OvertimeFoundTrb896()
        overtime.employeeId   = empStartStopRec.primaryKey.employeeId

        MSF810Rec employeeRec = readEmployee(overtime.employeeId)
        overtime.employeeName = employeeRec.surname.trim() + ", " + employeeRec.firstName.trim()

        overtime.tranDate     = empStartStopRec.primaryKey.trnDate
        overtime.startTime    = formatTime(empStartStopRec.fromTime)
        overtime.stopTime     = formatTime(empStartStopRec.endTime)
        overtime.workCode     = empStartStopRec.workCode
        overtime.shiftType    = shiftType
        overtime.rostStart    = formatTime(empRosterRec.rostStrTime)
        overtime.rostStop     = formatTime(empRosterRec.rostStopTime)
        overtime.payLocation  = payLocation
        overtime.lastModDate  = empStartStopRec.lastModDate
        overtime.lastModTime  = empStartStopRec.lastModTime
        overtime.status       = empStartStopRec.tranApprStatus
        overtime.revsdStatus  = empStartStopRec.reverseStatus

        return overtime
    }

    private String formatTime(BigDecimal input) {
        debug("formatTime")
        String inputAsString = input.toString()
        String output = "0000"
        if(inputAsString?.trim()) {
            String[] processed = inputAsString.tokenize(".")
            if(processed.size()>1) {
                output = processed[0].trim().padLeft(2, "0") + processed[1].trim().padRight(2, "0")
            }
            else {
                if(processed.size()==1) {
                    output = processed[0].trim().padLeft(2, "0") + "00"
                }
            }
        }
        return output
    }

    /**
     * Process the list of employee overtimes and return a subset of that list that should be printed.
     * @param listOfEmpOvertimes
     * @return list of employee overtimes that will be printed.
     */
    private ArrayList<OvertimeFoundTrb896> processEmployeeOvertimes(ArrayList<OvertimeFoundTrb896> listOfEmpOvertimes) {
        info("processEmployeeOvertimes")

        Collections.sort(listOfEmpOvertimes)
        ArrayList<OvertimeFoundTrb896> listOfPrintedOvertimeForEmployee = new ArrayList<OvertimeFoundTrb896>()
        for(overtime in listOfEmpOvertimes) {
            info("Overtime: ${overtime.toString()}")
            info("Overtime shift: ${overtime.shiftType}")
            if(isOvertimeProcessed(overtime)) {
                OvertimeAggregatesTrb896 aggregate
                /*Calculate aggregate*/
                if(overtime.startTime.toBigInteger()>=overtime.rostStop.toBigInteger() && overtime.shiftType in ["D", "M", "A", "N"]) {
                    aggregate = getOvertimeAggregates(overtime, listOfEmpOvertimes, POST_SHIFT)
                }
                else if(overtime.stopTime.toBigInteger()<=overtime.rostStart.toBigInteger() && overtime.shiftType in ["D", "M", "A", "N"]){
                    aggregate = getOvertimeAggregates(overtime, listOfEmpOvertimes, PRE_SHIFT)
                }
                else {
                    aggregate = getOvertimeAggregates(overtime, listOfEmpOvertimes, ROSTERED_OFF)
                }

                /*
                 * Determine if employee is day worker or shift worker
                 */
                Boolean isShiftWorker = isShiftWorker(overtime.employeeId, overtime.tranDate)

                /*Compare to overtime threshold*/

                String nextRosterDay

                //If the overtime stop time is before the roster start and  the start time is less than the stop time then the next roster is on that day
                if(overtime.stopTime.toBigInteger()<=overtime.rostStart.toBigInteger() && overtime.startTime.toBigInteger()<=overtime.stopTime.toBigInteger()) {
                    nextRosterDay = overtime.tranDate
                }
                else {
                    //If not then the next roster day is tomorrow
                    nextRosterDay = TimeHelperTrb896.getAddedDate(overtime.tranDate, Calendar.DAY_OF_YEAR, 1)
                }

                MSF898Rec nextRosterRec = getEmployeeRoster(overtime.employeeId, nextRosterDay)
                if(nextRosterRec!=null && !aggregate.isBlank()) {
                    Calendar nextRosterStartTime  = TimeHelperTrb896.getTimeInCalendar(nextRosterRec.primaryKey.trnDate, formatTime(nextRosterRec.rostStrTime))
                    Calendar aggregateStopTime    = TimeHelperTrb896.getTimeInCalendar(aggregate.lastTranDate, aggregate.lastStopTime)
                    Calendar aggregateStartTime   = TimeHelperTrb896.getTimeInCalendar(aggregate.firstTranDate, aggregate.firstStartTime)
                    if(aggregate.sumOfPeriod.toMilliseconds() > overtimeThresholdInMillis) {
                        if(isShiftWorker) {
                            if(TimeHelperTrb896.addTimeInMilliseconds(aggregateStopTime, shiftMinBreakThresholdGTInMillis).after(nextRosterStartTime)) {
                                listOfPrintedOvertimeForEmployee.add(overtime)
                            }
                        }
                        else {
                            if(TimeHelperTrb896.addTimeInMilliseconds(aggregateStopTime, dayMinBreakThresholdGTInMillis).after(nextRosterStartTime)) {
                                listOfPrintedOvertimeForEmployee.add(overtime)
                            }
                        }
                    }
                    else {
                        if(isShiftWorker) {
                            Calendar shiftThreshold = TimeHelperTrb896.getTimeInCalendar(TimeHelperTrb896.getAddedDate(nextRosterDay, Calendar.DAY_OF_YEAR, -1), "230000")

                            if(aggregateStopTime.after(shiftThreshold) && TimeHelperTrb896.addTimeInMilliseconds(aggregateStopTime, shiftMinBreakThresholdLTEInMillis).after(nextRosterStartTime)) {
                                listOfPrintedOvertimeForEmployee.add(overtime)
                            }
                        }
                        else {
                            Calendar dayThreshold = TimeHelperTrb896.getTimeInCalendar(nextRosterDay, "000000")
                            if(aggregateStopTime.after(dayThreshold) && TimeHelperTrb896.addTimeInMilliseconds(aggregateStopTime, dayMinBreakThresholdLTEInMillis).after(nextRosterStartTime)) {
                                listOfPrintedOvertimeForEmployee.add(overtime)
                            }
                        }
                    }
                }
            }


        }

        return listOfPrintedOvertimeForEmployee

    }



    /**
     * Get the aggregates information based on the currentovertime from the listOfEmpOvertimes.
     * This aggregate includes the sum of overtime period which includes the currentOvertime, and the date/time of that period.
     * @param currentOvertime
     * @param listOfEmpOvertimes
     * @param isPost
     * @return
     */
    private OvertimeAggregatesTrb896 getOvertimeAggregates(OvertimeFoundTrb896 currentOvertime, ArrayList<OvertimeFoundTrb896> listOfEmpOvertimes, String filter) {
        info("getOvertimeAggregates")

        OvertimeAggregatesTrb896 result = new OvertimeAggregatesTrb896()
        String compareDate, stopDate
        Boolean isFirstFound = true
        if(filter == POST_SHIFT || filter == ROSTERED_OFF) {
            stopDate = compareDate = TimeHelperTrb896.getAddedDate(currentOvertime.tranDate, Calendar.DAY_OF_YEAR, 1)
        }
        else {
            compareDate = TimeHelperTrb896.getAddedDate(currentOvertime.tranDate, Calendar.DAY_OF_YEAR, -1)
            stopDate    = currentOvertime.tranDate
        }
        MSF898Rec compareRosterRec = getEmployeeRoster(currentOvertime.employeeId, compareDate)

        for(overtimeProcessed in listOfEmpOvertimes) {
            if(overtimeProcessed.tranDate.toBigInteger()>stopDate.toBigInteger()) {
                break
            }
            if(overtimeProcessed.isEqual(currentOvertime)) {
                result.sumOfPeriod = result.sumOfPeriod.plus(TimeHelperTrb896.getOvertimePeriod(overtimeProcessed.tranDate, overtimeProcessed.startTime, overtimeProcessed.stopTime))
                if(isFirstFound) {
                    isFirstFound = false
                    result.firstTranDate  = overtimeProcessed.tranDate
                    result.firstStartTime = overtimeProcessed.startTime
                }
                if(overtimeProcessed.tranDate.toBigInteger()==result.lastTranDate.toBigInteger() && overtimeProcessed.stopTime.toBigInteger()>result.lastStopTime.toBigInteger()) {
                    result.lastStopTime = overtimeProcessed.stopTime
                }
                if(overtimeProcessed.tranDate.toBigInteger()>result.lastTranDate.toBigInteger()) {
                    result.lastTranDate = overtimeProcessed.tranDate
                    result.lastStopTime = overtimeProcessed.stopTime
                }
                if(overtimeProcessed.stopTime < overtimeProcessed.startTime && result.lastTranDate.equals(overtimeProcessed.tranDate)) {
                    //This means the overtime processed finished the next day
                    result.lastTranDate = TimeHelperTrb896.getAddedDate(overtimeProcessed.tranDate, Calendar.DAY_OF_YEAR, 1)
                    result.lastStopTime = overtimeProcessed.stopTime
                }
            }
            else {
                if(filter == POST_SHIFT) {
                    if((overtimeProcessed.tranDate.equals(currentOvertime.tranDate) && overtimeProcessed.startTime.toBigInteger()>=currentOvertime.rostStop.toBigInteger()) || (overtimeProcessed.tranDate.equals(compareDate) && overtimeProcessed.stopTime.toBigInteger()<=formatTime(compareRosterRec.rostStrTime).toBigInteger())) {
                        result.sumOfPeriod = result.sumOfPeriod.plus(TimeHelperTrb896.getOvertimePeriod(overtimeProcessed.tranDate, overtimeProcessed.startTime, overtimeProcessed.stopTime))
                        if(isFirstFound) {
                            isFirstFound = false
                            result.firstTranDate  = overtimeProcessed.tranDate
                            result.firstStartTime = overtimeProcessed.startTime
                        }
                        if(overtimeProcessed.tranDate.toBigInteger()==result.lastTranDate.toBigInteger() && overtimeProcessed.stopTime.toBigInteger()>result.lastStopTime.toBigInteger()) {
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                        if(overtimeProcessed.tranDate.toBigInteger()>result.lastTranDate.toBigInteger()) {
                            result.lastTranDate = overtimeProcessed.tranDate
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                        if(overtimeProcessed.stopTime < overtimeProcessed.startTime && result.lastTranDate.equals(overtimeProcessed.tranDate)) {
                            //This means the overtime processed finished the next day
                            result.lastTranDate = TimeHelperTrb896.getAddedDate(overtimeProcessed.tranDate, Calendar.DAY_OF_YEAR, 1)
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                    }
                }
                else if(filter == PRE_SHIFT) {
                    if((overtimeProcessed.tranDate.equals(currentOvertime.tranDate) && overtimeProcessed.stopTime.toBigInteger()<=currentOvertime.rostStart.toBigInteger()) || (overtimeProcessed.tranDate.equals(compareDate) && overtimeProcessed.startTime.toBigInteger()>=formatTime(compareRosterRec.rostStopTime).toBigInteger())) {
                        result.sumOfPeriod = result.sumOfPeriod.plus(TimeHelperTrb896.getOvertimePeriod(overtimeProcessed.tranDate, overtimeProcessed.startTime, overtimeProcessed.stopTime))
                        if(isFirstFound) {
                            isFirstFound = false
                            result.firstTranDate  = overtimeProcessed.tranDate
                            result.firstStartTime = overtimeProcessed.startTime
                        }
                        if(overtimeProcessed.tranDate.toBigInteger()==result.lastTranDate.toBigInteger() && overtimeProcessed.stopTime.toBigInteger()>result.lastStopTime.toBigInteger()) {
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                        if(overtimeProcessed.tranDate.toBigInteger()>result.lastTranDate.toBigInteger()) {
                            result.lastTranDate = overtimeProcessed.tranDate
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                        if(overtimeProcessed.stopTime < overtimeProcessed.startTime && result.lastTranDate.equals(overtimeProcessed.tranDate)) {
                            //This means the overtime processed finished the next day
                            result.lastTranDate = TimeHelperTrb896.getAddedDate(overtimeProcessed.tranDate, Calendar.DAY_OF_YEAR, 1)
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                    }
                }
                else if(filter == ROSTERED_OFF){
                    if((overtimeProcessed.tranDate.equals(currentOvertime.tranDate) && overtimeProcessed.startTime.toBigInteger()>=currentOvertime.rostStop.toBigInteger()) || (overtimeProcessed.tranDate.equals(compareDate) && (overtimeProcessed.stopTime.toBigInteger()<=formatTime(compareRosterRec.rostStrTime).toBigInteger() || overtimeProcessed.startTime.toBigInteger() < formatTime(compareRosterRec.rostStrTime).toBigInteger()))){
                        result.sumOfPeriod = result.sumOfPeriod.plus(TimeHelperTrb896.getOvertimePeriod(overtimeProcessed.tranDate, overtimeProcessed.startTime, overtimeProcessed.stopTime))
                        if(isFirstFound) {
                            isFirstFound = false
                            result.firstTranDate  = overtimeProcessed.tranDate
                            result.firstStartTime = overtimeProcessed.startTime
                        }
                        if(overtimeProcessed.tranDate.toBigInteger()==result.lastTranDate.toBigInteger() && overtimeProcessed.stopTime.toBigInteger()>result.lastStopTime.toBigInteger()) {
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                        if(overtimeProcessed.tranDate.toBigInteger()>result.lastTranDate.toBigInteger()) {
                            result.lastTranDate = overtimeProcessed.tranDate
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                        if(overtimeProcessed.stopTime < overtimeProcessed.startTime && result.lastTranDate.equals(overtimeProcessed.tranDate)) {
                            //This means the overtime processed finished the next day
                            result.lastTranDate = TimeHelperTrb896.getAddedDate(overtimeProcessed.tranDate, Calendar.DAY_OF_YEAR, 1)
                            result.lastStopTime = overtimeProcessed.stopTime
                        }
                    }
                }
            }
        }

        return result
    }


    private OvertimeAggregatesTrb896 modifyAggregates(OvertimeFoundTrb896 overtimeProcessed) {

    }

    /**
     * True if the employee is a shift worker in the date entered, false is the employee is a day worker
     * @param employeeId
     * @param tranDate
     * @return True if the employee is a shift worker in the date entered, false is the employee is a day worker
     */
    private Boolean isShiftWorker(String employeeId, String tranDate) {
        info("isShiftWorker")
        Boolean isShiftWorker = false
        MSF828Rec empAwardHistory = getEmpAwardHistory(employeeId,tranDate)
        for(String awardCode : shiftWorkerAwards) {
            if(awardCode.equals(empAwardHistory.awardCode)) {
                isShiftWorker = true
                break
            }
        }
        return isShiftWorker

    }

    /**
     * Get the latest employee award for the employee which include the tranDate
     * @param employeeId
     * @param tranDate
     * @return the latest employee award for the employee which include the tranDate
     */
    private MSF828Rec getEmpAwardHistory(String employeeId, String tranDate) {
        info("getEmpAwardHistory")

        String invTranDate = (99999999 - tranDate.toBigInteger()).toString()
        Constraint cEmployeeId      = MSF828Key.employeeId.equalTo(employeeId)
        Constraint cInvStrDateSpace = MSF828Key.invStrDate.greaterThan(" ")
        Constraint cInvStrDate      = MSF828Key.invStrDate.lessThanEqualTo(invTranDate)
        Constraint cEndDate         = MSF828Rec.endDate.lessThanEqualTo(tranDate)
        Constraint cEndDateZeros    = MSF828Rec.endDate.equalTo("00000000")
        QueryImpl qMSF828 = new QueryImpl(MSF828Rec.class).and(cEmployeeId).and(cInvStrDateSpace).and((cInvStrDate.and(cEndDate)).or(cEndDateZeros))
        MSF828Rec empAwardHistory = edoi.firstRow(qMSF828)
    }

    /**
     * Checks whether the overtime record is either  post shift or pre shift overtime.
     * This is determined from the parameter entered.
     * @param empStartStopRec
     * @param empRosterRec
     * @param shiftTypeR1
     * @return true if the overtime is either post shift or pre shift, false if otherwise
     */
    private Boolean isPostOrPreShiftOvertime(MSF891Rec empStartStopRec, MSF898Rec empRosterRec, String shiftTypeR1, String rostShiftTypeR1) {
        debug("isPostOrPreShiftOvertime")

        Boolean isPostShift = false
        Boolean isPreShift = false
        if (shiftTypeR1 in ["C", "V"]) {
            isPostShift = true
        } else if (rostShiftTypeR1 in ["D", "M", "A", "N"]) {

            if (empStartStopRec.endTime > empRosterRec.rostStopTime) {
                //We must see the roster for the next day
                MSF898Rec nextRosterRec = getEmployeeRoster(empStartStopRec.primaryKey.employeeId, TimeHelperTrb896.getAddedDate(empStartStopRec.primaryKey.trnDate, Calendar.DAY_OF_YEAR,1))
                if(nextRosterRec!=null) {
                    MSF801_R1_801Rec rostWorkCodeRec = getWorkCodeRecord(nextRosterRec.workCode)
                    if (rostWorkCodeRec!=null && !(rostWorkCodeRec.shiftTypeR1 in ["O", "S"])) {
                        isPostShift = true
                    }
                }
            }
            if (empStartStopRec.fromTime < empRosterRec.rostStrTime) {
                isPreShift = true
            }
        }
        return (isPostShift || isPreShift)
    }

    /**
     * True if the overtime should be processed, false if not.
     * @param empStartStopRec
     * @return True if the overtime should be processed, false if not
     */
    private Boolean isOvertimeProcessed(OvertimeFoundTrb896 overtime) {
        info("isOvertimeProcessed")

        Boolean isProcessed = false
        if(overtime.status.equals("APPR")) {
            isProcessed = true
        }
        else {
            if(overtime.status.equals("PAID") && overtime.revsdStatus.equals("RVSD")) {
                MSF817Rec payUpdPreviewHistRec = getPayUpdatePreviewHistory(PAY_GROUP_TG1)
                if(overtime.lastModDate.toBigInteger()>payUpdPreviewHistRec.runDate.toBigInteger() || (overtime.lastModDate.equals(payUpdPreviewHistRec.runDate) && overtime.lastModTime.toBigInteger > payUpdPreviewHistRec.runTime.toBigInteger())) {
                    isProcessed = true
                }
            }
        }

        return isProcessed
    }

    /**
     * Returns the latest pay update information for the payGroup.
     * @param payGroup
     * @return the latest pay update information for the payGroup.
     */
    private MSF817Rec getPayUpdatePreviewHistory(String payGroup) {
        info("getPayUpdatePreviewHistory")
        Constraint cPayGroup   = MSF817Key.payGroup.equalTo(payGroup)
        Constraint cInvEndDate = MSF817Key.invEndDate.greaterThanEqualTo(" ")
        Constraint cRunType    = MSF817Key.payRunType.equalTo("U")
        QueryImpl qMSF817 = new QueryImpl(MSF817Rec.class).and(cPayGroup).and(cInvEndDate).and(cRunType)
        MSF817Rec payUpdPreviewHistRec
        Boolean firstRec = true

        edoi.search(qMSF817) {MSF817Rec msf817Rec ->
            if(firstRec) {
                firstRec = false
                payUpdPreviewHistRec = msf817Rec
            }
            else {
                if(msf817Rec.primaryKey.getInvEndDate().toBigInteger()<payUpdPreviewHistRec.primaryKey.getInvEndDate().toBigInteger()) {
                    payUpdPreviewHistRec = msf817Rec
                }
            }
        }

        return payUpdPreviewHistRec
    }

    /**
     * Get the employee roster information based on start and stop.
     * The first priority is to find roster with APPR status.
     * If this is not found, there were no modification and we need to find the RGEN roster.
     * @param employeeId
     * @param trnDate
     * @return employee roster
     */
    private MSF898Rec getEmployeeRoster(String employeeId, String trnDate) {
        debug("getEmployeeRoster")
        MSF898Rec empRosterRec = readEmployeeRoster(employeeId, trnDate, "APPR")
        if(empRosterRec == null) {
            empRosterRec = readEmployeeRoster(employeeId, trnDate, "RGEN")
        }

        return empRosterRec
    }

    /**
     * Read from the employee roster using key supplied
     * @param employeeId
     * @param trnDate
     * @param status
     * @return employee roster record
     */
    private MSF898Rec readEmployeeRoster(String employeeId, String trnDate, String status) {
        debug("readEmployeeRoster")
        MSF898Rec result
        try {
            result = edoi.findByPrimaryKey(new MSF898Key(employeeId, trnDate, status))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            result = null
        }
        return result
    }

    /**
     * Get WorkCode based on the work code.
     * @param workCode work code
     * @return MSF801_R1_801Rec
     */
    private MSF801_R1_801Rec getWorkCodeRecord(String workCode) {
        debug("getWorkCodeRecord")
        MSF801_R1_801Rec workCodeRec = null
        try {
            workCodeRec = edoi.findByPrimaryKey(new MSF801_R1_801Key(MSF801_TYPE_R1,"***${workCode}"))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return workCodeRec
    }

    /**
     * Get Pay Group based on the pay code.
     * @param payCode Pay Group code
     * @return MSF801_A_801Rec
     */
    private MSF801_PG_801Rec getPayGroupRecord(String payGroup) {
        info("getPayGroupRecord")
        MSF801_PG_801Rec payGroupRec = null
        try {
            payGroupRec = edoi.findByPrimaryKey(new MSF801_PG_801Key(MSF801_TYPE_PG,payGroup))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return payGroupRec
    }


    /**
     * Get the end pay period for the pay group
     * @param payGroup
     * @return end pay period date
     */
    private String getEndPayPeriod(String payGroup) {
        info("getEndPayPeriod")
        String endPeriod = " "
        MSF801_PG_801Rec payGroupRec = getPayGroupRecord(payGroup)
        if(payGroupRec) {
            endPeriod = payGroupRec.curEndDtPg
        }
        return endPeriod

    }

    /**
     * Get the start pay period date. The start pay period date is determined by substracting a number of weeks from the end period.
     * @param endPeriod
     * @param weeksBeforeEnd
     * @return start pay period date
     */
    private String getStartPayPeriod(String endPeriod, Integer weeksBeforeEnd) {
        info("getStartPayPeriod")
        Integer payPeriod_9 = weeksBeforeEnd * -1
        return TimeHelperTrb896.getAddedDate(TimeHelperTrb896.getAddedDate(endPeriod, Calendar.WEEK_OF_YEAR, payPeriod_9),Calendar.DAY_OF_YEAR, 1)
    }


    /**
     * Check if Shift Award Codes are empty.
     * @return true if Shift Award Codes are empty
     */
    private boolean isShiftAwardCodesEmpty() {
        info("isShiftAwardCodesEmpty")
        boolean isEmpty = true
        for(String awardCode : shiftWorkerAwards) {
            if(awardCode?.trim()) {
                isEmpty = false
                break
            }
        }
        return isEmpty
    }

    /**
     * Get Award based on the award code.
     * @param awardCode award code
     * @return MSF801_C0_801Rec
     */
    private MSF801_C0_801Rec getAwardRecord(String awardCode) {
        info("getAwardRecord")
        MSF801_C0_801Rec awardRec = null
        try {
            awardRec = edoi.findByPrimaryKey(new MSF801_C0_801Key(MSF801_TYPE_C0, awardCode))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return awardRec
    }

    /**
     * Read Table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO if exist, null if doesn't exist
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable : ${tableType} | ${tableCode} ")
        TableServiceReadRequiredAttributesDTO tableReqAttributeDTO = new TableServiceReadRequiredAttributesDTO()
        tableReqAttributeDTO.returnTableCode = tableReqAttributeDTO.returnTableType = tableReqAttributeDTO.returnDescription = true

        TableServiceReadReplyDTO tableReplyDTO = null
        try {
            TableServiceReadRequestDTO tableRequestDTO = new TableServiceReadRequestDTO()
            tableRequestDTO.tableType = tableType
            tableRequestDTO.tableCode = tableCode
            tableRequestDTO.requiredAttributes = tableReqAttributeDTO
            tableReplyDTO = service.get("TABLE").read(tableRequestDTO, false)
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("readTable : debug error")
            serviceExc.printStackTrace();
            info("readTable : debug error end")
        } catch(Exception ex) {
            info("readTable : debug general error")
            ex.printStackTrace();
            info("readTable : debug general error end")
        }
        return tableReplyDTO
    }

    /**
     * This method returns the employee record for employeeId
     * @param employeeId
     * @return employee record if found, null if not
     */
    private MSF810Rec readEmployee(String employeeId) {
        debug("readEmployee")

        MSF810Rec employee = null
        try {
            employee = edoi.findByPrimaryKey(new MSF810Key(employeeId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return employee
    }



    //    /**
    //     * Return true if the input is a valid threshold.
    //     * A valid threshold is in "##0.##" format, that is a number with a maximum of 3 digits of whole numbers
    //     * and 2 digits of decimals
    //     * @param input
    //     * @return Return true if the input is a valid threshold
    //     */
    //    private Boolean isValidThreshold(String input) {
    //        info("validateThreshold")
    //
    //        Boolean valid = false
    //        if(input?.trim()){
    //            //Check length
    //            String[] splitString = input.tokenize(".")
    //            if(splitString.size()==1) {
    //                splitString = [input,"00"]
    //            }
    //            Boolean isLengthValid = (splitString[0].length()<=3) && (splitString[1].length()<=2)
    //
    //            //Check value
    //            Boolean isValueValid = false
    //            if(splitString[0].isNumber() && splitString[1].isNumber()) {
    //                BigDecimal convertValue = (splitString[0]+"."+splitString[1]).toBigDecimal()
    //                isValueValid = (convertValue >0) && (convertValue <= 99.99)
    //            }
    //
    //            valid = isLengthValid && isValueValid
    //        }
    //
    //        return valid
    //    }

}

/*run script*/  
ProcessTrb896 process = new ProcessTrb896();
process.runBatch(binding);
