/**
 * @Ventyx 2012
 * 
 * This batch reports the usage of work code(s) in a certain period
 * 
 * This program developed using edoi instead of service call.
 * 
 * Developed based on <b>URS Exception Report - Use of Work Codes D03.pdf</b>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.ellipse.edoi.ejb.msf817.*;
import com.mincom.ellipse.edoi.ejb.msf898.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceCreateReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;

import java.io.BufferedWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import groovy.time.*;

import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*

/**
 * Request parameters for Trb892.
 */
public class ParamsTrb892{
    private String payLoc
    private String currentOnly
    private String noOfPeriods
    private String dateFrom
    private String dateTo
    private String workCode0
    private String workCode1
    private String workCode2
    private String workCode3
    private String workCode4
    private String workCode5
    private String workCode6
    private String workCode7
    private String workCode8
    private String workCode9
    private String groupByRegion
}

public class TimeHelperTrb892 {


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
     * Get the date after added based on input. Both the input and the returned date is in yyyyMMdd format.
     * The column is field to be added, e.g. if we want to get 1 week after the date then we use Calendar.WEEK_OF_YEAR.
     * @param column
     * @param inputDate
     * @return the date one day after the input date in yyyyMMdd format.
     */
    public static String getAddedDate(String inputDate, Integer column, Integer numberOfAddition) {
        Calendar cal = TimeHelperTrb892.getTimeInCalendar(inputDate, "0000")
        //http://www.timeanddate.com/date/dateadd.html
        cal.add(column, numberOfAddition)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd")
        return (formatter.format(cal.getTime()))
    }
    
    /**
     * Get the formatted inputDate based on the format entered. 
     * @param inputDate
     * @param format
     * @return String
     */
    public static String getFormattedDate(String inputDate, String format) {
        Calendar cal = TimeHelperTrb892.getTimeInCalendar(inputDate, "0000")
        SimpleDateFormat formatter = new SimpleDateFormat(format)
        return formatter.format(cal.getTime())
    }
    
    /**
     * Get the formatted of String inputTime.
     * The resulting date will be in HH(timeSeparator)MM format
     * @param inputTime
     * @param timeSeparator
     * @return String
     */
    public static String getFormattedTime(String inputTime, String timeSeparator) {
        String processed = inputTime.padRight(5)
        return processed.substring(1,3)+ timeSeparator +processed.substring(3,5)
    }
    
    /**
     * Get the formatted of BigDecimal inputTime.
     * The resulting date will be in HH(timeSeparator)MM format
     * @param inputTime
     * @param timeSeparator
     * @return String
     */
    public static String getFormattedTime(BigDecimal input, String timeSeparator) {
        String inputAsString = input.toString()
        String output = "0000"
        if(inputAsString?.trim()) {
            String[] processed = inputAsString.split("\\.")
            if(processed.size()>1) {
                output = processed[0].trim().padLeft(2, "0") + timeSeparator + processed[1].trim().padRight(2, "0")
            }
            else {
                if(processed.size()==1) {
                    output = processed[0].trim().padLeft(2, "0") + timeSeparator + "00"
                }
            }
        }
        return output
    }
}

/**
 * Main process of Trb892.
 */
public class ProcessTrb892 extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_A_NAME          = "TRB892A"
    private static final String OUTPUT_FILE_NAME       = "TRT892A"
    private static final Integer MAX_RECORD_RESULT     = 1000
    private static final String APPR_STATUS            = "APPR"
    private static final String PAID_STATUS            = "PAID"
    private static final String RPLD_RVS_STATUS        = "RPLD"
    private static final String RVSD_RVS_STATUS        = "RVSD"
    private static final String SEPARATOR              = ","
    private static final String TIME_SEPARATOR         = ":"
    
    private class PeriodStartEndTrb892{
        private String periodStartDate
        private String periodEndDate
    }
    private class EmployeeNamesTrb892{
        private String reportName = ""
        private String csvName = ""
    }
    
    private class ReportComparatorWithRegion implements Comparator<ReportLineTrb892> {
        @Override
        public int compare(ReportLineTrb892 line1, ReportLineTrb892 line2) {
            if(!line1.payLoc.trim().equals(line2.payLoc.trim())) {
                return line1.payLoc.trim().compareTo(line2.payLoc.trim())
            }
            if(!line1.timesheetRecord.workCode.trim().equals(line2.timesheetRecord.workCode.trim())) {
                return line1.timesheetRecord.workCode.trim().compareTo(line2.timesheetRecord.workCode.trim())
            }
            if(!line1.employeeName.reportName.trim().equals(line2.employeeName.reportName.trim())) {
                return line1.employeeName.reportName.trim().compareTo(line2.employeeName.reportName.trim())
            }
            if(!line1.timesheetRecord.primaryKey.trnDate.trim().equals(line2.timesheetRecord.primaryKey.trnDate.trim())) {
                return line1.timesheetRecord.primaryKey.trnDate.trim().compareTo(line2.timesheetRecord.primaryKey.trnDate.trim())
            }
            if(!line1.timesheetRecord.strFromTime.trim().equals(line2.timesheetRecord.strFromTime.trim())) {
                return line1.timesheetRecord.strFromTime.trim().compareTo(line2.timesheetRecord.strFromTime.trim())
            }
            return 0;
        }
    }
    private class ReportComparatorWithoutRegion implements Comparator<ReportLineTrb892> {
        @Override
        public int compare(ReportLineTrb892 line1, ReportLineTrb892 line2) {
            if(!line1.timesheetRecord.workCode.trim().equals(line2.timesheetRecord.workCode.trim())) {
                return line1.timesheetRecord.workCode.trim().compareTo(line2.timesheetRecord.workCode.trim())
            }
            if(!line1.employeeName.reportName.trim().equals(line2.employeeName.reportName.trim())) {
                return line1.employeeName.reportName.trim().compareTo(line2.employeeName.reportName.trim())
            }
            if(!line1.timesheetRecord.primaryKey.trnDate.trim().equals(line2.timesheetRecord.primaryKey.trnDate.trim())) {
                return line1.timesheetRecord.primaryKey.trnDate.trim().compareTo(line2.timesheetRecord.primaryKey.trnDate.trim())
            }
            if(!line1.timesheetRecord.strFromTime.trim().equals(line2.timesheetRecord.strFromTime.trim())) {
                return line1.timesheetRecord.strFromTime.trim().compareTo(line2.timesheetRecord.strFromTime.trim())
            }
            return 0;
        }
    }
    
    private class CsvComparator implements Comparator<ReportLineTrb892> {
        @Override
        public int compare(ReportLineTrb892 line1, ReportLineTrb892 line2) {
            if(!line1.payLoc.trim().equals(line2.payLoc.trim())) {
                return line1.payLoc.trim().compareTo(line2.payLoc.trim())
            }
            if(!line1.timesheetRecord.primaryKey.employeeId.trim().equals(line2.timesheetRecord.primaryKey.employeeId.trim())) {
                return line1.timesheetRecord.primaryKey.employeeId.trim().compareTo(line2.timesheetRecord.primaryKey.employeeId.trim())
            }
            if(!line1.timesheetRecord.primaryKey.trnDate.trim().equals(line2.timesheetRecord.primaryKey.trnDate.trim())) {
                return line1.timesheetRecord.primaryKey.trnDate.trim().compareTo(line2.timesheetRecord.primaryKey.trnDate.trim())
            }
            if(!line1.timesheetRecord.strFromTime.trim().equals(line2.timesheetRecord.strFromTime.trim())) {
                return line1.timesheetRecord.strFromTime.trim().compareTo(line2.timesheetRecord.strFromTime.trim())
            }
            return 0;
        }
    }
    
    private class ReportLineTrb892 {       
        private String payLoc = ""
        private String payLocDesc =""
        private EmployeeNamesTrb892 employeeName = new EmployeeNamesTrb892()
        private String tranDay = ""
        private String workCodeDesc = ""
        private String rostWorkCode = ""
        private String rostWorkCodeDesc = ""
        private BigDecimal rostStartTime = 0
        private BigDecimal rostEndTime = 0
        private MSF891Rec timesheetRecord = new MSF891Rec()

        private String toReport() {
            String returnString =                 timesheetRecord.primaryKey.employeeId.padRight(17)
                   returnString =  returnString + employeeName.reportName.toUpperCase().padRight(33)
                   returnString =  returnString + tranDay.padRight(4)
                   returnString =  returnString + TimeHelperTrb892.getFormattedDate(timesheetRecord.primaryKey.trnDate,"dd/MM/yy").padRight(11)
                   returnString =  returnString + rostWorkCode.padRight(11)
                   returnString =  returnString + TimeHelperTrb892.getFormattedTime(rostStartTime, TIME_SEPARATOR).padRight(10)
                   returnString =  returnString + TimeHelperTrb892.getFormattedTime(rostEndTime, TIME_SEPARATOR).padRight(11)
                   returnString =  returnString + TimeHelperTrb892.getFormattedTime(timesheetRecord.strFromTime, TIME_SEPARATOR).padRight(11)
                   returnString =  returnString + TimeHelperTrb892.getFormattedTime(timesheetRecord.endTime, TIME_SEPARATOR).padRight(11)
                   returnString =  returnString + timesheetRecord.tranApprStatus
                   if(timesheetRecord.tranApprStatus.equals(PAID_STATUS) && !timesheetRecord.reverseStatus.trim().equals("")) {
                       returnString = returnString + "/" + timesheetRecord.reverseStatus
                   }
                   return returnString
        }
        
        private String toCSV() {
            String returnString =                             payLoc
                   returnString =  returnString + SEPARATOR + payLocDesc
                   returnString =  returnString + SEPARATOR + timesheetRecord.primaryKey.employeeId
                   returnString =  returnString + SEPARATOR + employeeName.csvName.toUpperCase()
                   returnString =  returnString + SEPARATOR + timesheetRecord.primaryKey.trnDate
                   returnString =  returnString + SEPARATOR + timesheetRecord.primaryKey.seqNo
                   returnString =  returnString + SEPARATOR + rostWorkCode
                   returnString =  returnString + SEPARATOR + rostWorkCodeDesc
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(rostStartTime, TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(rostEndTime, TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + timesheetRecord.workCode
                   returnString =  returnString + SEPARATOR + workCodeDesc
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(timesheetRecord.strFromTime, TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(timesheetRecord.endTime, TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(timesheetRecord.mealBrkStart, TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(timesheetRecord.mealBrkStop,TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + timesheetRecord.units.toString()
                   returnString =  returnString + SEPARATOR + timesheetRecord.payPerNo
                   returnString =  returnString + SEPARATOR + timesheetRecord.reverseStatus
                   returnString =  returnString + SEPARATOR + timesheetRecord.tranApprStatus
                   returnString =  returnString + SEPARATOR + timesheetRecord.batchId
                   returnString =  returnString + SEPARATOR + timesheetRecord.tranLocFr
                   returnString =  returnString + SEPARATOR + timesheetRecord.tranLocTo
                   returnString =  returnString + SEPARATOR + timesheetRecord.tranReason
                   returnString =  returnString + SEPARATOR + timesheetRecord.workedThruMb
                   returnString =  returnString + SEPARATOR + timesheetRecord.workedThruPb
                   returnString =  returnString + SEPARATOR + timesheetRecord.adjoinsPrevDay
                   returnString =  returnString + SEPARATOR + timesheetRecord.absenceHours.toString()
                   returnString =  returnString + SEPARATOR + timesheetRecord.lveReason
                   returnString =  returnString + SEPARATOR + timesheetRecord.lrLveStDate
                   returnString =  returnString + SEPARATOR + timesheetRecord.lrLeaveType
                   returnString =  returnString + SEPARATOR + timesheetRecord.lveStDate
                   returnString =  returnString + SEPARATOR + timesheetRecord.lveEndDate
                   returnString =  returnString + SEPARATOR + timesheetRecord.claimNo
                   returnString =  returnString + SEPARATOR + timesheetRecord.absAuth
                   returnString =  returnString + SEPARATOR + timesheetRecord.costInd
                   returnString =  returnString + SEPARATOR + timesheetRecord.costingCode
                   returnString =  returnString + SEPARATOR + timesheetRecord.workOrder
                   returnString =  returnString + SEPARATOR + timesheetRecord.tranChanged
                   returnString =  returnString + SEPARATOR + timesheetRecord.createdDate
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(timesheetRecord.createdTime_9,TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + timesheetRecord.createdEmpId
                   returnString =  returnString + SEPARATOR + timesheetRecord.approvedDate
                   returnString =  returnString + SEPARATOR + TimeHelperTrb892.getFormattedTime(timesheetRecord.approvedTime_9, TIME_SEPARATOR)
                   returnString =  returnString + SEPARATOR + timesheetRecord.approvedEmpId
                   returnString =  returnString + SEPARATOR + timesheetRecord.lastChangeRef
                   returnString =  returnString + SEPARATOR + timesheetRecord.lastModDate
                   returnString =  returnString + SEPARATOR + timesheetRecord.lastModTime
                   returnString =  returnString + SEPARATOR + timesheetRecord.lastModUser
                   return returnString
        }
    }
    
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 3

    private ParamsTrb892 batchParams
    private Integer noOfPeriodsReported             = 0
    private Boolean processCurrPeriodOnly           = false
    private Boolean reportGroupedByRegion           = false
    private Map payGroupStartEnd                    = new HashMap<String, PeriodStartEndTrb892>()
    private Map employeeNames                       = new HashMap<String, EmployeeNamesTrb892>()
    private Map workCodeDescriptions                = new HashMap<String, String>()
    private Map payLocDescriptions                  = new HashMap<String, String>()
    private Map payGroupLastRun                     = new HashMap<String, Long>()
    private ArrayList<ReportLineTrb892> reportLines = new ArrayList<ReportLineTrb892>()
    private ArrayList<String> workCodes             = new ArrayList<String>()

    /*
     * Reporting variables 
     */
    private def reportWriterA
    private BufferedWriter outputFileWriter
    private File outputFile
    private String taskUUID

    /**
     * Run the main batch
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b)

        printSuperBatchVersion();
        info("runBatch Version : " + version)
        

        batchParams = params.fill(new ParamsTrb892())        

        try {
            processBatch()
        } catch(Exception e) {
            info("error ${e.printStackTrace()}")
            e.printStackTrace()
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB892 ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")
        } finally {
            if(reportWriterA != null) {
                reportWriterA.close()
            }
            if(outputFileWriter != null) {
                outputFileWriter.close()
            }
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(outputFile,"text/comma-separated-values", "TRT892");
            }

        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch")
        if(initialise()) {
            browseEmployees()
            printReport()
            printCSV()
        }
        info("end processBatch")
    }
    
    /**
     * Initialise and validate the batch parameters.
     * Return true if validation is successful.
     * @return Boolean
     */
    private Boolean initialise() {
        info("initialise")
        
        taskUUID = this.getTaskUUID()
        outputFile = openFile(OUTPUT_FILE_NAME)
        outputFileWriter = new BufferedWriter(new FileWriter(outputFile))
        outputFileWriter.write("Pay Location,Pay Location Description,Employee Id,EmployeeName,Tran Date,Sequence No,Rostered Work Code,Rostered Work Code Description,Rostered Start Time,Rostered Stop Time,Work Code,Work Code Description,Start Time,Stop Time,Meal Break Start,Meal Break Stop,Units,Pay Period No,Reverse Status,Transaction Status,Batch ID,Transaction Location From,Transaction Location To,Transaction Reason,Worked Thru Meal Break,Worked Thru Paid Break,Adjoins Previous Day,Absence Hours,Leave Reason,Leave Request Start Date,Leave Type,Leave Start Date PIA,Leave End Date PIA,Claim No,Absence Authority,Costing Indicator,Cost Code,Work Order,Transaction Modified,Created Date,Created Time,Created Employee,Approved Date,Approved Time,Approved Employee,Last Change Ref,Last Modified Date,Last Modified Time,Last Modified Employee\r\n")
        
        Boolean isValid = true
        ArrayList<String> errorMessage = new ArrayList<String>()
        
        processCurrPeriodOnly = batchParams.currentOnly.trim().equalsIgnoreCase("Y")
        
        if(processCurrPeriodOnly) {
            if(!isEmpty(batchParams.dateFrom)) {
                errorMessage.add("  Date From                         Input parameter not required if Current Period Only is set to Y")
            }
            if(!isEmpty(batchParams.dateTo)) {
                errorMessage.add("  Date To                           Input parameter not required if Current Period Only is set to Y")
            }
            if(isEmpty(batchParams.noOfPeriods)) {
                errorMessage.add("  Current Pay Periods Processed     Input parameter required if Current Period Only is set to Y")
            }
            else {
                noOfPeriodsReported = batchParams.noOfPeriods.trim().toInteger()
                if(noOfPeriodsReported == 0) {
                    errorMessage.add("  Current Pay Periods Processed     Input parameter required if Current Period Only is set to Y")
                }
            }
        }
        else {
            if(isEmpty(batchParams.dateFrom)) {
                errorMessage.add("  Date From                         Input parameter required if Current Period Only is not set to Y")
            }
            if(isEmpty(batchParams.dateTo)) {
                errorMessage.add("  Date To                           Input parameter required if Current Period Only is not set to Y")
            }
        }
        
        (0..9).each {
            if(!isEmpty(batchParams."workCode${it}")) {
                workCodes.add(batchParams."workCode${it}")
            }
        }
        
        reportGroupedByRegion = batchParams.groupByRegion.trim().equalsIgnoreCase("Y")
        
        ArrayList<String> rptPgHead = new ArrayList<String>()
        if(errorMessage.size>0) {
            isValid = false
            rptPgHead.add("  Input Parameter                   Error Message")
            reportWriterA = report.open(REPORT_A_NAME, rptPgHead)
            errorMessage.each {reportWriterA.write(it)}
        }
        else {
            rptPgHead.add("Employee         Employee                         Day Date       Rostered   Rostered  Rostered  Work Code  Work Code  Status")
            rptPgHead.add("ID               Name                                            Work Code  Start Tm  Stop Tm   Start Tm   Stop Tm")
            reportWriterA = report.open(REPORT_A_NAME, rptPgHead)
        }
       
        return isValid

    }
    
    /**
     * Browse the employees from the Employee Payroll file for further process.
     * When the pay location parameter is entered, it is also used as a constraint.
     */
    private void browseEmployees() {
        info("browseEmployees")
        QueryImpl query = new QueryImpl(MSF820Rec.class).and(MSF820Key.employeeId.greaterThanEqualTo(" "))
        if(!isEmpty(batchParams.payLoc)) {
            query= query.and(MSF820Rec.payLocation.equalTo(batchParams.payLoc.trim()))
        }
        query = query.orderBy(MSF820Rec.msf820Key)
        
        edoi.search(query,MAX_RECORD_RESULT){MSF820Rec msf820Rec ->
            browseTimesheet(msf820Rec.payGroup, msf820Rec.primaryKey.employeeId, msf820Rec.payLocation)
        }
    }
    
    /**
     * Browse the Timesheet file which will be processed.
     * @param payGroup
     * @param employeeId
     * @param payLoc
     */
    private void browseTimesheet(String payGroup, String employeeId, String payLoc) {
        info("browseTimesheet")
        PeriodStartEndTrb892 period = getPeriodStartEnd(payGroup)
        
        Constraint cPayGroup = MSF891Key.payGroup.equalTo(payGroup)
        Constraint cTranDateFr = MSF891Key.trnDate.greaterThanEqualTo(period.periodStartDate)
        Constraint cTranDateTo = MSF891Key.trnDate.lessThanEqualTo(period.periodEndDate)
        Constraint cEmployeeId = MSF891Key.employeeId.equalTo(employeeId)

        QueryImpl query = new QueryImpl(MSF891Rec.class).and(cPayGroup).and(cTranDateFr).and(cTranDateTo).and(cEmployeeId)
        
        
        if(processCurrPeriodOnly) {
            Constraint cStatus1    = MSF891Rec.tranApprStatus.equalTo(APPR_STATUS)
            Constraint cStatus2    = MSF891Rec.tranApprStatus.equalTo(PAID_STATUS).and(MSF891Rec.reverseStatus.equalTo(RPLD_RVS_STATUS))
            Constraint cStatus3    = MSF891Rec.tranApprStatus.equalTo(PAID_STATUS).and(MSF891Rec.reverseStatus.equalTo(RVSD_RVS_STATUS))
            Constraint cStatus     = cStatus1.or(cStatus2).or(cStatus3)
            query = query.and(cStatus)
        }

        
        if(workCodes.size()>0) {
            Constraint cWorkCode
            Boolean first = true
            workCodes.each {
                if(first) {
                    cWorkCode = MSF891Rec.workCode.equalTo(it)
                    first = false
                }
                else {
                    cWorkCode = cWorkCode.or(MSF891Rec.workCode.equalTo(it))
                }
            }
            query = query.and(cWorkCode)
        }

        query = query.orderBy(MSF891Rec.msf891Key)
        
        edoi.search(query,MAX_RECORD_RESULT){MSF891Rec msf891Rec ->
            processTimesheet(msf891Rec, payLoc)
        }
    }
    
    /**
     * Determine the period start and period end date to be used in browsing the timesheet.
     * If the Process Current Period Only is set to Y, this will be determined by calculating the
     * number of periods before the current period end date.
     * If it is set to N, the batch parameters date from and date to are used.
     * @param payGroup
     * @return PeriodStartEndTrb892
     */
    private PeriodStartEndTrb892 getPeriodStartEnd(String payGroup) {
        info("getPeriodStartEnd")
        
        PeriodStartEndTrb892 result
        if(processCurrPeriodOnly) {
            result = payGroupStartEnd.get(payGroup)
            if(result == null) {
                MSF801_PG_801Rec currPeriod = readPGCurrPeriod(payGroup)
                result = new PeriodStartEndTrb892()
                if(currPeriod !=null) {
                    result.periodEndDate = currPeriod.curEndDtPg
                    
                    Integer payPeriod_9 = noOfPeriodsReported * -1
                    result.periodStartDate = TimeHelperTrb892.getAddedDate(TimeHelperTrb892.getAddedDate(result.periodEndDate, Calendar.WEEK_OF_YEAR, payPeriod_9),Calendar.DAY_OF_YEAR, 1)
                }
                payGroupStartEnd.put(payGroup, result)
            }
        }
        else {
            result = new PeriodStartEndTrb892()
            result.periodStartDate = batchParams.dateFrom.trim()
            result.periodEndDate   = batchParams.dateTo.trim()
        }
        
        return result
    }
    
    /**
     * Return the pay group record.
     * @param payGroup
     * @return MSF801_PG_801Rec
     */
    private MSF801_PG_801Rec readPGCurrPeriod(String payGroup) {
        info("readPGCurrPeriod")
        
        MSF801_PG_801Rec result = null
        
        try {
            MSF801_PG_801Key key = new MSF801_PG_801Key()
            key.cntlRecType = "PG"
            key.cntlKeyRest = payGroup
            result = edoi.findByPrimaryKey(key)
        }
        catch(EDOIObjectNotFoundException e) {}
        
        return result
        
    }
    
    /**
     * Process the timesheet record to become the report line.
     * @param timesheetRecord
     * @param payLoc
     */
    private  void processTimesheet(MSF891Rec timesheetRecord, String payLoc) {
        info("processTimesheet")
        
        if(isPrintedRecord(timesheetRecord)) {
            ReportLineTrb892 line = new ReportLineTrb892()
            line.payLoc = payLoc
            line.payLocDesc = getPayLocDesc(payLoc)
            line.timesheetRecord = timesheetRecord
            line.workCodeDesc    = getWorkCodeDesc(timesheetRecord.workCode)
            line.employeeName    = getemployeeName(timesheetRecord.primaryKey.employeeId)
            line.tranDay         = TimeHelperTrb892.getFormattedDate(timesheetRecord.primaryKey.trnDate, "EEE").toUpperCase()
            
            MSF898Rec roster = getEmployeeRoster(timesheetRecord.primaryKey.employeeId, timesheetRecord.primaryKey.trnDate)
            if(roster!=null) {
                line.rostWorkCode     = roster.workCode
                line.rostWorkCodeDesc = getWorkCodeDesc(roster.workCode)
                line.rostStartTime    = roster.rostStrTime
                line.rostEndTime      = roster.rostStopTime 
            }
            
            reportLines.add(line)
        }
    }
    
    /**
     * Additional filters to determine whether a timesheet record needs to be printed or not.
     * For PAID/RPLD records, there must be another record with the same date, employee id, work code
     * and APPR status.
     * For PAID/RVSD records, the last mod date and last mod time must be after the latest payroll update
     * run date and run time for that pay group.
     * @param timesheetRecord
     * @return Boolean
     */
    private Boolean isPrintedRecord(MSF891Rec timesheetRecord) {
        info("isPrintedRecord")
        
        Boolean isPrinted = true
        if(processCurrPeriodOnly && timesheetRecord.tranApprStatus.equals(PAID_STATUS)) {
            if(timesheetRecord.reverseStatus.equals(RPLD_RVS_STATUS) && !isApprExistForRecord(timesheetRecord)) {
                isPrinted = false
            }
            if(timesheetRecord.reverseStatus.equals(RVSD_RVS_STATUS)) {
                Long lastRunDate = getLastRunDate(timesheetRecord.primaryKey.payGroup)
                Long timesheetModDateTime = (timesheetRecord.lastModDate + timesheetRecord.lastModTime).toLong()
                if(timesheetModDateTime <= lastRunDate) {
                    isPrinted = false
                }
            }
        }
        
        return isPrinted
    }
    
    /**
     * Determine whether another record with the same date, employee id, work code
     * and APPR status exist for input timesheet record. Return true if exist.
     * @param timesheetRecord
     * @return Boolean.
     */
    private Boolean isApprExistForRecord(MSF891Rec timesheetRecord) {
        info("isApprExistForRecord")
        Boolean isExist = false
        Constraint cPayGroup   = MSF891Key.payGroup.equalTo(timesheetRecord.primaryKey.payGroup)
        Constraint cTranDate   = MSF891Key.trnDate.equalTo(timesheetRecord.primaryKey.trnDate)
        Constraint cEmployeeId = MSF891Key.employeeId.equalTo(timesheetRecord.primaryKey.employeeId)
        Constraint cSeqNo      = MSF891Key.seqNo.notEqualTo(timesheetRecord.primaryKey.seqNo)
        Constraint cStatus     = MSF891Rec.tranApprStatus.equalTo(APPR_STATUS)
        QueryImpl query = new QueryImpl(MSF891Rec.class).and(cPayGroup).and(cEmployeeId).and(cTranDate).and(cSeqNo).and(cStatus).orderBy(MSF891Rec.msf891Key)
        MSF891Rec rec = (MSF891Rec)edoi.firstRow(query)
        if(rec) {
            isExist = true
        }
        return isExist
    }
    
    /**
     * Return the last payroll update run date and run time as a concatenated long.
     * @param payGroup
     * @return Long
     */
    private Long getLastRunDate(String payGroup) {
        info("getLastRunDate")
        
        Long lastRun = payGroupLastRun.get(payGroup)
        if(lastRun==null) {
            MSF817Rec msf817Rec = readLatestRunRecord(payGroup)
            lastRun=0
            if(msf817Rec!=null) {
                lastRun = (msf817Rec.runDate + msf817Rec.runTime).toLong()
            }
            payGroupLastRun.put(payGroup, lastRun)
        }
        
        return lastRun
    }
    
    /**
     * Return the latest payroll update run record for payGroup.
     * @param payGroup
     * @return MSF817Rec
     */
    private MSF817Rec readLatestRunRecord(String payGroup) {
        info("readLatestRunRecord")
        
        Constraint cPayGroup   = MSF817Key.payGroup.equalTo(payGroup)
        Constraint cInvEndDate = MSF817Key.invEndDate.greaterThanEqualTo(" ")
        Constraint cRunType    = MSF817Key.payRunType.equalTo("U")
        QueryImpl query = new QueryImpl(MSF817Rec.class).and(cPayGroup).and(cInvEndDate).and(cRunType).orderBy(MSF817Rec.msf817Key)
        MSF817Rec returnRec = (MSF817Rec)edoi.firstRow(query)
        return returnRec
    }
    
    /**
     * Return employee name for employeeId.
     * @param employeeId
     * @return EmployeeNamesTrb892
     */
    private EmployeeNamesTrb892 getemployeeName(String employeeId) {
        info("getemployeeName")
        
        EmployeeNamesTrb892 name = employeeNames.get(employeeId)
        if(name==null) {
            MSF810Rec msf810Rec = readEmployeeRecord(employeeId)
            name = new EmployeeNamesTrb892()
            if(msf810Rec!=null) {   
                name.reportName = "${msf810Rec.surname.trim()}, ${msf810Rec.firstName.trim()}"
                name.csvName    = "${msf810Rec.firstName.trim()} ${msf810Rec.surname.trim()}"
            }
            employeeNames.put(employeeId, name)
        }
        
        return name
    }

    /**
     * Return the employee record for employeeId.
     * @param employeeId
     * @return MSF810Rec
     */
    private MSF810Rec readEmployeeRecord(String employeeId) {
        info("readEmployeeRecord")
        
        MSF810Rec record = null
        try {
            record = edoi.findByPrimaryKey(new MSF810Key(employeeId))
        }
        catch(EDOIObjectNotFoundException e) {}
        return record
    }
    
    /**
     * Get the employee roster information based on start and stop.
     * The first priority is to find roster with APPR status.
     * If this is not found, there were no modification and we need to find the RGEN roster.
     * @param employeeId
     * @param trnDate
     * @return MSF898Rec
     */
    private MSF898Rec getEmployeeRoster(String employeeId, String trnDate) {
        info("getEmployeeRoster")
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
     * @return MSF898Rec
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
     * Get the pay location description
     * @param payLoc
     * @return String
     */
    private String getPayLocDesc(String payLoc) {
        info("getPayLocDesc")
        
        String desc = payLocDescriptions.get(payLoc)
        if(desc==null) {
            TableServiceReadReplyDTO tableRec = readTable("PAYL", payLoc)
            desc=""
            if(tableRec!=null) {
                desc = tableRec.description
            }
            payLocDescriptions.put(payLoc, desc)
        }
        
        return desc
    }

    /**
     * Read Table based on table type and table code.
     * Return null if record exist.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable")
        TableServiceReadRequiredAttributesDTO tableReqAttributeDTO = new TableServiceReadRequiredAttributesDTO()
        tableReqAttributeDTO.returnDescription = true

        TableServiceReadReplyDTO tableReplyDTO = null
        try {
            TableServiceReadRequestDTO tableRequestDTO = new TableServiceReadRequestDTO()
            tableRequestDTO.tableType = tableType
            tableRequestDTO.tableCode = tableCode
            tableRequestDTO.requiredAttributes = tableReqAttributeDTO
            tableReplyDTO = service.get("TABLE").read(tableRequestDTO, false)
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
        }
        return tableReplyDTO
    }

    /**
     * Return the work code description.
     * @param workCode
     * @return String
     */
    private String getWorkCodeDesc(String workCode) {
        info("getWorkCodeDesc")
        
        String desc = workCodeDescriptions.get(workCode)
        if(desc==null) {
            MSF801_R1_801Rec wcRecord = readWorkCodeRecord(workCode)
            desc=""
            if(wcRecord!=null) {
                desc = wcRecord.tnameR1
            }
            workCodeDescriptions.put(workCode, desc)
        }
        
        return desc
    }

    /**
     * Return the work code record
     * @param workCode
     * @return MSF801_R1_801Rec
     */
    private MSF801_R1_801Rec readWorkCodeRecord(String workCode) {
        info("readWorkCodeRecord")
        
        MSF801_R1_801Rec record = null
        try {
            record = edoi.findByPrimaryKey(new MSF801_R1_801Key("R1","***${workCode}"))
        }
        catch(EDOIObjectNotFoundException e) {}
        return record
    }
    
    /**
     * Return true if input is null or blank.
     * @param input
     * @return Boolean
     */
    private Boolean isEmpty(String input) {
        info("isEmpty")
        Boolean isEmptyValue = (input==null)||(input.trim().equals(""))
        return isEmptyValue
    }
        
    /**
     * Prints the hard copy report.
     * The order of the print is determined by the Grouped by Region batch parameter.
     * If it is grouped, the records will be grouped by pay location then work code
     * and for each work code, the records are sorted by employee name, date, and time.
     * If it is not grouped, the records will be grouped by work code only,  and for each 
     * work code, the records are sorted by employee name, date, and time.
     */
    private void printReport() {
        info("printReport")
        String currPayLoc   = ""
        String currWorkCode = ""

        if(reportGroupedByRegion) {
            Collections.sort(reportLines, new ReportComparatorWithRegion())
        }
        else {
            Collections.sort(reportLines, new ReportComparatorWithoutRegion())
        }
        
        reportLines.each {
            if(!it.payLoc.equals(currPayLoc) && reportGroupedByRegion) {
                reportWriterA.write(" ")
                reportWriterA.write(" ")
                reportWriterA.write(" ${it.payLoc} - ${it.payLocDesc}")
                currPayLoc = it.payLoc
            }
            if(!it.timesheetRecord.workCode.equals(currWorkCode)) {
                reportWriterA.write(" ")
                reportWriterA.write(" ${it.timesheetRecord.workCode} - ${it.workCodeDesc}")
                reportWriterA.write(" ")
                currWorkCode = it.timesheetRecord.workCode
            }
            reportWriterA.write("${it.toReport()}")
        }
    }
    
    /**
     * Prints the csv file.
     * The records are ordered by pay location, employee id, transaction date, then work code
     * start time.
     */
    private void printCSV() {
        info("printCSV")
        Collections.sort(reportLines, new CsvComparator())
        reportLines.each {
            outputFileWriter.write("${it.toCSV()}\r\n")
        }
    }
    
    /**
     * Create a file in the working directory with supplied name.
     * @param name
     * @return output file representation
     */
    private File openFile(String name) {
        info("openFile file name: ${name}")
        
        def workingDir = env.workDir
        String outputFilePath = "${workingDir}/${name}"
        if(taskUUID?.trim()){
            outputFilePath = outputFilePath + "." + taskUUID
        }
        outputFilePath = outputFilePath
        File outputFile = new File(outputFilePath)
        info("end openFile, ${name} created in ${outputFile.getAbsolutePath()}")
        return outputFile
    }
}

/*run script*/  
ProcessTrb892 process = new ProcessTrb892();
process.runBatch(binding);
