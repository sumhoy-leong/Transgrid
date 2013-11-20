/**
 * @Ventyx 2012
 * 
 * This program will create a report and csv file contains adjustments to employee's 
 * meal allowances and meal breaks when they work overtime inside normal rostered 
 * hours or when they work overtime while claiming a sustenance allowance.  
 * 
 * This program developed using edoi instead of service call.
 * 
 * Developed based on <b>URS Reporting E8 Exception OT Meal Adjustments_V2.docx</b>
 */
package com.mincom.ellipse.script.custom

import java.io.BufferedWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_C0_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_C0_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_R1_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_R1_801Rec;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec;
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Key;
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Rec;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec;
import com.mincom.ellipse.edoi.ejb.msf828.MSF828Key;
import com.mincom.ellipse.edoi.ejb.msf828.MSF828Rec;
import com.mincom.ellipse.edoi.ejb.msf891.MSF891Key;
import com.mincom.ellipse.edoi.ejb.msf891.MSF891Rec;
import com.mincom.ellipse.edoi.ejb.msf898.MSF898Key;
import com.mincom.ellipse.edoi.ejb.msf898.MSF898Rec;
import com.mincom.ellipse.edoi.ejb.msf89w.MSF89WKey;
import com.mincom.ellipse.edoi.ejb.msf89w.MSF89WRec;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.enterpriseservice.ellipse.employee.*;
import com.mincom.enterpriseservice.ellipse.empaward.*;
import com.mincom.eql.impl.QueryImpl;

/**
 * Request Parameter for TRB899.
 */
public class ParamsTrb899 {
    private String payLocation
    private String payPeriod
    private String shiftWorkerAward0, shiftWorkerAward1, shiftWorkerAward2, shiftWorkerAward3,
    shiftWorkerAward4, shiftWorkerAward5, shiftWorkerAward6, shiftWorkerAward7;
    private String overtimeWorkCode0, overtimeWorkCode1, overtimeWorkCode2, overtimeWorkCode3,
    overtimeWorkCode4, overtimeWorkCode5, overtimeWorkCode6, overtimeWorkCode7, overtimeWorkCode8,
    overtimeWorkCode9, overtimeWorkCode10, overtimeWorkCode11, overtimeWorkCode12, overtimeWorkCode13,
    overtimeWorkCode14, overtimeWorkCode15, overtimeWorkCode16, overtimeWorkCode17;
    private String mealAllowanceCode
    private String mealBreakCode
}

/**
 * Entity to hold Employee's Overtime information.
 */
public class EmployeeOvertime implements Comparable<EmployeeOvertime> {
    String employeeId, employeeName
    BigDecimal rosteredStartTime, rosteredStopTime, startTime, stopTime
    String workCode, date, status, tranType, tranMealAllowCode, tranMealBreakCode
    double mealAllow, mealBreak

    /**
     * Construct Employee Overtime Record based on start/stop, roster, and employee information.
     * @param empStartStopRec start/stop record
     * @param empRosRec roster record
     * @param empRec employee record
     */
    public EmployeeOvertime(MSF891Rec empStartStopRec, MSF898Rec empRosRec, MSF810Rec empRec) {
        this.employeeId        = empStartStopRec.getPrimaryKey().getEmployeeId()
        this.employeeName      = empRec.getSurname().trim() +
                (empRec.getFirstName()?.trim() ? ", " + empRec.getFirstName().trim() : "")
        this.date              = empStartStopRec.getPrimaryKey().getTrnDate().trim()
        this.rosteredStartTime = empRosRec.getRostStrTime()
        this.rosteredStopTime  = empRosRec.getRostStopTime()
        this.workCode          = empStartStopRec.getWorkCode()
        this.startTime         = empStartStopRec.getFromTime()
        this.stopTime          = empStartStopRec.getEndTime()
        //Set the status and Transaction Type
        this.status            = empStartStopRec.getTranApprStatus().trim() +
                (empStartStopRec.getReverseStatus()?.trim() ? "/" + empStartStopRec.getReverseStatus().trim() : "")
        this.tranType          = "E" //default transaction type based on URS
    }

    @Override
    public int compareTo(EmployeeOvertime o) {
        int c = this.getEmployeeName().compareTo(o.getEmployeeName())
        if(c == 0) {
            c = this.getDate().compareTo(o.getDate())
        }
        return c
    }
}

/**
 * Main Process of Trb899.
 */
public class ProcessTrb899 extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME            = "TRB899A"
    private static final String TABLE_SERVICE          = "TABLE"
    private static final String TABLE_EMPLOYEE         = "EMPLOYEE"
    private static final String ERR_MSG_INPUT_REQUIRED = "INPUT REQUIRED"
    private static final String ERR_MSG_INPUT_NOT_EXST = "%s - INPUT DOES NOT EXIST"
    private static final String TABLE_TYPE_PAYL        = "PAYL"
    private static final String PAY_GROUP_TG1          = "TG1"
    private static final String MSF801_TYPE_C0         = "C0"
    private static final String MSF801_TYPE_A          = "A"
    private static final String MSF801_TYPE_R1         = "R1"
    private static final String TRN_STATUS_APPR        = "APPR"
    private static final String TRN_STATUS_RGEN        = "RGEN"
    private static final String TRN_STATUS_PAID        = "PAID"
    private static final String REV_STATUS_RPLD        = "RPLD"
    private static final String REV_STATUS_RVSD        = "RVSD"
    private static final String MSF801_TYPE_PG         = "PG"
    private static final String DASHED_LINE            = String.format("%132s"," ").replace(' ', '-')
    private static final String REPORT_TITLE           = "${REPORT_NAME} Exception Report After Lockout - Overtime Meal Adjustments Report"
    private static final String REPORT_TITLE_2         = "Pay Location: %-2s - %-30s    Pay Period: %-8s"
    private static final String REPORT_ERROR_TITLE     = "Validation Errors:"
    private static final String REPORT_ERROR_HEADING   = "Field Ref/Value                 Error/Warning Message Column Hdg "
    private static final String REPORT_ERROR_DETAIL    = "%-30s  %-99s"
    private static final String REPORT_SUB_HEAD_1      = "Overtime Within Normal Hours"
    private static final String REPORT_SUB_HEAD_2      = "Overtime While on Sustenance"
    private static final String REPORT_SUB_HEAD_3      = "PAY LOCATION: %-2s - %-30s"
    private static final String REPORT_HEADING_1_1     = "Employee ID  Employee Name                   Day  Date      Rostered    Rostered   Work  Start  Stop   Status     Meal     Meal "
    private static final String REPORT_HEADING_1_2     = "                                                            Start Time  Stop Time  Code  Time   Time              Allow    Break "
    private static final String REPORT_DETAIL_1        = "%-10s   %-30s  %-3s  %-8s  %-5s       %-5s      %-2s    %-5s  %-5s  %-9s  % 5.2f    % 5.2f"
    private static final String REPORT_HEADING_2_1     = "Employee ID  Employee Name                   Day  Date      Rostered    Rostered   Work  Start  Stop   Status     Meal     "
    private static final String REPORT_HEADING_2_2     = "                                                            Start Time  Stop Time  Code  Time   Time              Allow    "
    private static final String REPORT_DETAIL_2        = "%-10s   %-30s  %-3s  %-8s  %-5s       %-5s      %-2s    %-5s  %-5s  %-9s  % 5.2f"
    private static final String CSV_HEADER             = "Employee ID,Tran Date,Tran Type,Tran Code,Tran Units"
    private static final String CSV_DETAIL             = "%-10s,%-8s,%-1s,%-3s,% 5.2f"
    private static final int FOUR_HOURS_IN_MINUTE      = 240
    private static final int ONE_HALF_HOURS_IN_MINUTE  = 90
    private static final int WORK_AWARD_SIZE           = 8
    private static final int OT_WORK_CODE_SIZE         = 18
    private static final ArrayList<String> SHIFT_TYPE  = ["A", "M", "D", "N"]

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 3

    private ParamsTrb899 batchParams
    private String[] shiftWorkerAwards = new String[WORK_AWARD_SIZE]
    private String[] overtimeWorkCodes = new String[OT_WORK_CODE_SIZE]
    private boolean groupedByAllPayLoc
    private LinkedHashMap<String, List<EmployeeOvertime>> employeesOnNormalHours
    private LinkedHashMap<String, List<EmployeeOvertime>> employeesOnSustenance
    private String startPayPeriod, endPayPeriod, tmpPayLocation
    private ArrayList<String> payLocList
    /*
     * Reporting variables
     */
    private def reportWriter
    private File csvFile
    private BufferedWriter csvWriter
    private LinkedHashMap<String, String> errorMessages
    private boolean headerWritten

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){
        init(b)
        printSuperBatchVersion()
        info("runBatch Version : ${version}")
        //Fill the params
        batchParams = params.fill(new ParamsTrb899())
        //Print the params
        populateAndPrintRequestParam()
        //process the batch
        try {
            processBatch()
        } catch(Exception e) {
            e.printStackTrace()
            info("Batch Failed, error occurs : ${e.getMessage()}")
            throw e
        } finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch")
        initialize()
        if(validateRequestParameter()) {
            browseEmployeePayroll()
            constructReport()
            constructCSV()
        }
    }

    /**
     * Close the report.
     */
    private void printBatchReport(){
        info("printBatchReport")
        //Create error message report - if any
        if(!errorMessages.isEmpty()) {
            writeErrorReport()
        }
        //Close the writer
        if(reportWriter != null) {
            reportWriter.close()
        }
        if(csvWriter != null) {
            csvWriter.close()
        }
        //Adding CSV into Request.
        if(csvFile && getTaskUUID()?.trim()) {
            info("Adding CSV into Request.")
            request.request.CURRENT.get().addOutput(csvFile,
                    "text/comma-separated-values", REPORT_NAME)
        }
    }

    /**
     * Insert Shift Worker Award Code and Overtime Work Code into array and print the request param.
     */
    private void populateAndPrintRequestParam() {
        info("populateAndPrintRequestParam")
        info("Pay Location          : ${batchParams.payLocation}")
        info("Pay Period            : ${batchParams.payPeriod}")
        //Shift Awards
        Class iClazz = batchParams.getClass()
        (0..(WORK_AWARD_SIZE-1)).each {
            String fieldName = "shiftWorkerAward${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName )
            field.setAccessible(true)
            shiftWorkerAwards[it] = field.get(batchParams)
            info("Shift Worker Award ${it}  : ${shiftWorkerAwards[it]}")
        }
        //OT work codes
        (0..(OT_WORK_CODE_SIZE-1)).each {
            String fieldName = "overtimeWorkCode${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName )
            field.setAccessible(true)
            overtimeWorkCodes[it] = field.get(batchParams)
            info("Overtime Work Code ${it} : ${overtimeWorkCodes[it]}")
        }
        info("Meal Allowance Code    : ${batchParams.mealAllowanceCode}")
        info("Meal Break Code        : ${batchParams.mealBreakCode}")
    }

    /**
     * Initialize the report control and csv writer.
     */
    private void initialize() {
        info("initialize")
        //Initialize Report
        errorMessages = new LinkedHashMap<String, String>()
        headerWritten = false
        //Initialize the Map
        employeesOnNormalHours = new LinkedHashMap<String, List<EmployeeOvertime>>()
        employeesOnSustenance  = new LinkedHashMap<String, List<EmployeeOvertime>>()
    }

    /**
     * Validate the request parameter.
     * @return true if request parameter is valid, false otherwise
     */
    private boolean validateRequestParameter() {
        info("validateRequestParameter")
        boolean valid = true
        groupedByAllPayLoc = batchParams.payLocation?.trim().length() == 0
        //Validate Pay Location
        if(batchParams.payLocation?.trim()) {
            TableServiceReadReplyDTO tableReply = readTable(TABLE_TYPE_PAYL, batchParams.payLocation)
            if(tableReply == null) {
                valid = false
                errorMessages.put("PayLocation",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.payLocation))
            }
        }
        //Validate Pay Periods
        if(!batchParams.payPeriod?.trim()) {
            valid = false
            errorMessages.put("PayPeriod", ERR_MSG_INPUT_REQUIRED)
        } else {
            //calculate pay periods, it should be exist
            calculatePayPeriod()
            if(!startPayPeriod?.trim()) {
                valid = false
                errorMessages.put("StartPayPeriod",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.mealBreakCode))
            }
            if(!endPayPeriod?.trim()) {
                valid = false
                errorMessages.put("EndPayPeriod",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.mealBreakCode))
            }
        }
        //Validate Shift Worker Awards
        if(isShiftAwardCodesEmpty()) {
            valid = false
            errorMessages.put("ShiftAwardCodes", ERR_MSG_INPUT_REQUIRED)
        } else {
            for(String awardCode : shiftWorkerAwards) {
                if(awardCode?.trim()) {
                    if(getAwardRecord(awardCode) == null) {
                        valid = false
                        errorMessages.put("ShiftAwardCode",
                                String.format(ERR_MSG_INPUT_NOT_EXST, awardCode))
                        break
                    }
                }
            }
        }
        //Validate Overtime Work Orders
        if(isOvertimeWorkCodesEmpty()) {
            valid = false
            errorMessages.put("OvertimeWorkCodes", ERR_MSG_INPUT_REQUIRED)
        } else {
            for(String workCode : overtimeWorkCodes) {
                if(workCode?.trim()) {
                    if(getWorkCodeRecord(workCode) == null) {
                        valid = false
                        errorMessages.put("OvertimeWorkCode",
                                String.format(ERR_MSG_INPUT_NOT_EXST, workCode))
                        break
                    }
                }
            }
        }
        //Validate Meal Allowance Award
        if(!batchParams.mealAllowanceCode?.trim()) {
            valid = false
            errorMessages.put("MealAllowanceCode", ERR_MSG_INPUT_REQUIRED)
        } else {
            if(getAllowanceRecord(batchParams.mealAllowanceCode) == null) {
                valid = false
                errorMessages.put("MealAllowanceCode",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.mealAllowanceCode))
            }
        }
        //Validate Meal Break Code
        if(!batchParams.mealBreakCode?.trim()) {
            valid = false
            errorMessages.put("MealBreakCode", ERR_MSG_INPUT_REQUIRED)
        } else {
            if(getAllowanceRecord(batchParams.mealBreakCode) == null) {
                valid = false
                errorMessages.put("MealBreakCode",
                        String.format(ERR_MSG_INPUT_NOT_EXST, batchParams.mealBreakCode))
            }
        }
        return valid
    }

    /**
     * Check if Shift Award Codes are empty.
     * @return true if Shift Award Codes are empty
     */
    private boolean isShiftAwardCodesEmpty() {
        info("isShiftAwardCodesEmpty")
        for(String awardCode : shiftWorkerAwards) {
            if(awardCode?.trim()) {
                return false
            }
        }
        return true
    }

    /**
     * Check if Overtime Work Codes are empty.
     * @return true if Overtime Work Codes are empty
     */
    private boolean isOvertimeWorkCodesEmpty() {
        info("isOvertimeWorkCodesEmpty")
        for(String overtimeCode : overtimeWorkCodes) {
            if(overtimeCode?.trim()) {
                return false
            }
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
        tableReqAttributeDTO.setReturnTableCode(true)
        tableReqAttributeDTO.setReturnTableType(true)
        tableReqAttributeDTO.setReturnAssociatedRecord(true)
        tableReqAttributeDTO.setReturnDescription(true)

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
     * Get Award based on the award code.
     * @param awardCode award code
     * @return MSF801_C0_801Rec
     */
    private MSF801_C0_801Rec getAwardRecord(String awardCode) {
        info("getAwardRecord")
        MSF801_C0_801Key awardKey = new MSF801_C0_801Key()
        awardKey.setCntlKeyRest(awardCode)
        awardKey.setCntlRecType(MSF801_TYPE_C0)
        MSF801_C0_801Rec awardRec
        try {
            awardRec = edoi.findByPrimaryKey(awardKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("Cannot read MSF801_C0_801Rec caused by ${e.getMessage()}")
            awardRec = null
        }
        return awardRec
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
            info("Cannot read MSF801_R1_801Rec caused by ${e.getMessage()}")
            workCodeRec = null
        }
        return workCodeRec
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
        allowanceKey.setCntlRecType(MSF801_TYPE_A)
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
            info("Cannot read MSF801_PG_801Rec caused by ${e.getMessage()}")
            payGroupRec = null
        }
        return payGroupRec
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
            int payPeriod_9 = (batchParams.payPeriod as int) * -1
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
     * Browse the employee payroll.
     */
    private void browseEmployeePayroll() {
        info("browseEmployeePayroll")
        QueryImpl qMSF820 = new QueryImpl(MSF820Rec.class).
                and(MSF820Key.employeeId.greaterThanEqualTo(" "))

        if(!groupedByAllPayLoc) {
            qMSF820 = qMSF820.and(MSF820Rec.payLocation.equalTo(batchParams.payLocation))
        }
        edoi.search(qMSF820) {MSF820Rec empPayrollRec->
            browseEmployeeStartStop(empPayrollRec)
        }
    }

    /**
     * Browse employee start and stop based on employee payroll.
     * @param empPayrollRec employee payroll
     */
    private void browseEmployeeStartStop(MSF820Rec empPayrollRec) {
        info("browseEmployeeStartStop")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.employeeId.equalTo(empPayrollRec.primaryKey.employeeId)).
                and(MSF891Key.trnDate.lessThanEqualTo(endPayPeriod)).
                and(MSF891Key.trnDate.greaterThanEqualTo(startPayPeriod))

        edoi.search(qMSF891) {MSF891Rec empStartStopRec->
            String payGroup     = empPayrollRec.getPayGroup()
            MSF898Rec empRosRec = getEmployeeRoster(empStartStopRec)
            MSF810Rec empRec    = getEmployee(empPayrollRec.getPrimaryKey().getEmployeeId().trim())
            tmpPayLocation      = empPayrollRec.getPayLocation()
            //7.4.3 Overtime Within Normal Hours
            if(empRosRec && empStartStopRec && checkOvertimeWithinNormal(empStartStopRec, empRosRec)) {
                //Create employee overtime
                EmployeeOvertime empOT = new EmployeeOvertime(empStartStopRec, empRosRec, empRec)
                adjustPaymentOnNormalHours(empStartStopRec, empOT)
            }

            //7.4.4 Overtime While on Sustenance
            if(empRosRec && empStartStopRec && checkOvertimeWhileOnSustenance(empStartStopRec, empRosRec)) {
                //Create employee overtime
                EmployeeOvertime empOT = new EmployeeOvertime(empStartStopRec, empRosRec, empRec)
                adjustPaymentWithSustenance(empStartStopRec, empRosRec, empOT)
            }
        }
    }

    /**
     * Add the employee overtime record into sustenance map based on the pay location.
     * @param payLoc pay location 
     * @param emp employee overtime record
     */
    private void addToSustenanceMap(String payLoc, EmployeeOvertime emp) {
        info("addToSustenanceMap")
        List<EmployeeOvertime> empList = employeesOnSustenance.get(payLoc)
        if(empList == null) {
            empList = new ArrayList<EmployeeOvertime>()
        }
        empList.add(emp)
        employeesOnSustenance.put(payLoc, empList)
    }

    /**
     * Add the employee overtime record into normal hours map based on the pay group.
     * @param payLoc pay location
     * @param emp employee overtime record
     */
    private void addToNormalHoursMap(String payLoc, EmployeeOvertime emp) {
        info("addToNormalHoursMap")
        List<EmployeeOvertime> empList = employeesOnNormalHours.get(payLoc)
        if(empList == null) {
            empList = new ArrayList<EmployeeOvertime>()
        }
        empList.add(emp)
        employeesOnNormalHours.put(payLoc, empList)
    }

    /**
     * Get the employee roster information based on start and stop.
     * @param empStartStopRec start and stop record
     * @return employee roster
     */
    private MSF898Rec getEmployeeRoster(MSF891Rec empStartStopRec) {
        info("getEmployeeRoster")
        MSF898Rec empRosterRec
        MSF898Key rosterKey = new MSF898Key()
        rosterKey.setEmployeeId(empStartStopRec.getPrimaryKey().getEmployeeId())
        rosterKey.setTrnDate(empStartStopRec.getPrimaryKey().getTrnDate())
        rosterKey.setTrnStatus(TRN_STATUS_APPR)
        try{
            empRosterRec = edoi.findByPrimaryKey(rosterKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Cannot read MSF898Rec caused by ${e.getMessage()}")
            empRosterRec = null
        }
        if(empRosterRec == null) {
            rosterKey.setTrnStatus(TRN_STATUS_RGEN)
            try{
                empRosterRec = edoi.findByPrimaryKey(rosterKey)
            } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                info("Cannot read MSF898Rec caused by ${e.getMessage()}")
                empRosterRec = null
            }
        }
        return empRosterRec
    }

    /**
     * Check overtime within normal status.
     * @param empStartStopRec
     * @param empRoster
     * @return true if overtime within normal.
     */
    private boolean checkOvertimeWithinNormal(MSF891Rec empStartStopRec, MSF898Rec empRoster) {
        info("checkOvertimeWithinNormal")
        String workCode         = empStartStopRec.getWorkCode()
        BigDecimal endTime      = empStartStopRec.getEndTime()
        BigDecimal fromTime     = empStartStopRec.getFromTime()
        BigDecimal rostStrTime  = empRoster.getRostStrTime()
        BigDecimal rostStopTime = empRoster.getRostStopTime()

        if(fromTime > endTime) {
            endTime.add(new BigDecimal(24.00))
        }
        if(rostStrTime > rostStopTime) {
            rostStopTime.add(new BigDecimal(24.00))
        }

        //1.Where the work code (MSF891-WORK_CODE) matches any of the overtime work codes in the report request parameters
        boolean workCodeEligible = isWorkCodeExist(workCode)
        if(!workCodeEligible) {
            return false
        }
        //2.Where the work code duration (MSF891-END_TIME less MSF891-FROM_TIME) is 4 hours or more
        int endTimeMinutes  = convertPeriodToMinute(endTime)
        int fromTimeMinutes = convertPeriodToMinute(fromTime)
        boolean workTimeEligible = (endTimeMinutes - fromTimeMinutes) >= FOUR_HOURS_IN_MINUTE
        if(!workTimeEligible) {
            return false
        }

        return workCodeEligible && workTimeEligible
    }

    /**
     * Check overtime while on sustenance status.
     * @param empStartStopRec
     * @param empRoster
     * @return true if overtime while on sustenance.
     */
    private boolean checkOvertimeWhileOnSustenance(MSF891Rec empStartStopRec, MSF898Rec empRoster) {
        info("checkOvertimeWhileOnSustenance")
        String rosterWorkCode   = empRoster.getWorkCode()
        String empWorkCode      = empStartStopRec.getWorkCode()
        BigDecimal endTime      = empStartStopRec.getEndTime()
        BigDecimal fromTime     = empStartStopRec.getFromTime()
        BigDecimal rostStrTime  = empRoster.getRostStrTime()
        BigDecimal rostStopTime = empRoster.getRostStopTime()
        String rosShiftType = getWorkCodeRecord(rosterWorkCode)?.getShiftTypeR1().trim()
        String empShiftType = getWorkCodeRecord(empWorkCode)?.getShiftTypeR1().trim()
        info("rosterWorkCode ${rosterWorkCode} - ${rosShiftType}")
        info("empWorkCode    ${rosterWorkCode} - ${empShiftType}")
        if(fromTime > endTime) {
            endTime.add(new BigDecimal(24.00))
        }
        if(rostStrTime > rostStopTime) {
            rostStopTime.add(new BigDecimal(24.00))
        }

        //1.Where the work code (MSF891-WORK_CODE) matches any of the overtime work codes in the report request parameters
        boolean workCodeEligible = isWorkCodeExist(empWorkCode)

        //and where the work code duration (MSF891-END_TIME less MSF891-FROM_TIME) is 1.5 hours or more
        int endTimeMinutes  = convertPeriodToMinute(endTime)
        int fromTimeMinutes = convertPeriodToMinute(fromTime)
        boolean workTimeEligible = (endTimeMinutes - fromTimeMinutes) >= ONE_HALF_HOURS_IN_MINUTE

        //2.The employee's normal rostered shift for the day in MSF898 Employee Roster Schedule has a shift type of rostered on (MSF801_R1_801-SHIFT_TYPE_R1) of D, M, A or N (day, morning, afternoon or night)
        boolean rosShiftTypeEligible = isShiftTypeValid(rosShiftType)

        //3a.The employee's timesheet  has a shift type of rostered on (MSF801_R1_801-SHIFT_TYPE_R1) of D, M, A or N (day, morning, afternoon or night)
        boolean empShiftTypeEligible = isShiftTypeValid(empShiftType)

        //3b.Work code start time (MSF891-FROM_TIME) is greater than or equal to 1.5 hours before the rostered start time (MSF898-ROST_STR_TIME for same date),
        boolean startTimeEilgible = fromTimeMinutes >= (convertPeriodToMinute(rostStrTime) - ONE_HALF_HOURS_IN_MINUTE)
        //3c.Work code stop time (MSF891-END_TIME) is greater than or equal to 1.5 hours after the rostered stop time (MSF898-ROST_STOP_TIME for same date)
        boolean endTimeEilgible = endTimeMinutes >= (convertPeriodToMinute(rostStopTime) + ONE_HALF_HOURS_IN_MINUTE)
        //4.To determine OT while on sustenance you need to combine the validations in 7.4.4.1 (to find the work code) then 7.4.4.2 (to find if that work code has a sustenance allowance)
        boolean sustenanceStatusEligible = getSustenanceAllowance(empStartStopRec)  != null
        info ("workCodeEligible: ${workCodeEligible}")
        info ("workTimeEligible: ${workTimeEligible}")
        info ("rosShiftTypeEligible: ${rosShiftTypeEligible}")
        info ("empShiftTypeEligible: ${empShiftTypeEligible}")
        info ("startTimeEilgible: ${startTimeEilgible}")
        info ("endTimeEilgible: ${endTimeEilgible}")
        info ("sustenanceStatusEligible: ${sustenanceStatusEligible}")
        return ((workCodeEligible && workTimeEligible && rosShiftTypeEligible ) || ( empShiftTypeEligible && (startTimeEilgible || endTimeEilgible))) && sustenanceStatusEligible

    }

    /**
     * Adjust the payment on normal hours
     * @param empStartStopRec
     * @param empOT
     */
    private void adjustPaymentOnNormalHours(MSF891Rec empStartStopRec, EmployeeOvertime empOT) {
        info("adjustPaymentOnNormalHours ${empStartStopRec.getPrimaryKey().employeeId} - ${empStartStopRec.getPrimaryKey().trnDate}")
        String tranApprStatus = empStartStopRec.getTranApprStatus()?.trim()
        String reverseStatus  = empStartStopRec.getReverseStatus()?.trim()
        String empAwardCode   = getEmployeeAward(empStartStopRec.getPrimaryKey().getEmployeeId()?.trim(),
                empStartStopRec.getPrimaryKey().getTrnDate()?.trim())?.getAwardCode()
        boolean workThruPb    = empStartStopRec.getWorkedThruPb().trim().equalsIgnoreCase("y")
        //if the employee's award matches any of the shift awards entered into the report parameters
        //use the "Worked Thru Paid Break" = N
        if(isShiftAwardCodeExist(empAwardCode)) {
            workThruPb = false
        }
        //Set the mealAllow and mealBreak units based on apprStatus and reverseStatus
        switch(tranApprStatus) {
            case TRN_STATUS_APPR :
            //7.4.3.2.1 New or Changed Overtime Work Codes
                if(!workThruPb) {
                    empOT.tranMealAllowCode = batchParams.mealAllowanceCode
                    empOT.mealAllow = 1
                } else {
                    empOT.tranMealAllowCode = batchParams.mealAllowanceCode
                    empOT.tranMealBreakCode = batchParams.mealBreakCode
                    empOT.mealAllow = 1
                    empOT.mealBreak = 0.20
                }
                addToNormalHoursMap(tmpPayLocation, empOT)
                break
            case TRN_STATUS_PAID:
                switch(reverseStatus) {
                    case REV_STATUS_RPLD:
                    //7.4.3.2.1 New or Changed Overtime Work Codes - check corresponding APPR transaction
                    if(isCorrespondingApprovedTransactionExist(empStartStopRec)) {
                        if(!workThruPb) {
                            empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                            empOT.mealAllow = -1
                        } else {
                            empOT.tranMealAllowCode = batchParams.mealAllowanceCode
                            empOT.tranMealBreakCode = batchParams.mealBreakCode
                            empOT.mealAllow = -1
                            empOT.mealBreak = -0.20
                        }
                        addToNormalHoursMap(tmpPayLocation, empOT)
                    }
                    break
                    case REV_STATUS_RVSD:
                    //7.4.3.2.2 Deleted (Reversed) Overtime
                    //Determine if the transaction was modified since the last pay update was run by comparing
                    //the last modified date and time on the work code (MSF891-LAST_MOD_DATE /MSF891-LAST_MOD_TIME)
                    //with the run date and time from the TG1 pay group (MSF817- RUN_DATE / MSF817-RUN_TIME)
                    MSF817Rec payHistoryRec = getPayUpdatesHistory(empStartStopRec.getPrimaryKey().getPayGroup())
                    String histRunDateTime = (payHistoryRec?.getRunDate().trim().length() == 0 ? "00000000" : payHistoryRec?.getRunDate()) +
                            (payHistoryRec?.getRunTime().trim().length() == 0 ? "000000" : payHistoryRec?.getRunTime())
                    String lastModDateTime = (empStartStopRec?.getLastModDate().trim().length() == 0 ? "00000000" : empStartStopRec?.getLastModDate()) +
                            (empStartStopRec?.getLastModTime().trim().length() == 0 ? "000000" : empStartStopRec?.getLastModTime())
                    if(lastModDateTime.compareTo(histRunDateTime) >= 0) {
                        if(!workThruPb) {
                            empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                            empOT.mealAllow = -1
                        } else {
                            empOT.tranMealAllowCode = batchParams.mealAllowanceCode
                            empOT.tranMealBreakCode = batchParams.mealBreakCode
                            empOT.mealAllow = -1
                            empOT.mealBreak = -0.20
                        }
                        addToNormalHoursMap(tmpPayLocation, empOT)
                    }
                    break
                }
                break
        }
    }

    /**
     * Adjust the payment with Sustenance
     * @param empStartStopRec
     * @param empRoster
     * @param empOT
     */
    private void adjustPaymentWithSustenance(MSF891Rec empStartStopRec, MSF898Rec empRoster,
    EmployeeOvertime empOT) {
        info("adjustPaymentWithSustenance ${empStartStopRec.getPrimaryKey().employeeId} - ${empStartStopRec.getPrimaryKey().trnDate}")
        String tranApprStatus = empStartStopRec.getTranApprStatus()?.trim()
        String reverseStatus  = empStartStopRec.getReverseStatus()?.trim()
        debug("tranApprStatus ${tranApprStatus}")
        debug("reverseStatus  ${reverseStatus}")
        debug("tmpPayLocation  ${tmpPayLocation}")
        //Set the mealAllow and mealBreak units based on apprStatus and reverseStatus
        switch(tranApprStatus) {
            case TRN_STATUS_APPR :
            //7.4.4.3.1 New or Changed Overtime Work Codes and New Sustenance Allowance Attached to Overtime Work Code
                empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                empOT.mealAllow = -1
                addToSustenanceMap(tmpPayLocation, empOT)
                break
            case TRN_STATUS_PAID:
                switch(reverseStatus) {
                    //7.4.4.3.2 New Sustenance Allowance NOT Attached to Overtime Work Code
                    case REV_STATUS_RPLD:
                    //7.4.4.3.1 New or Changed Overtime Work Codes and New Sustenance Allowance Attached to Overtime Work Code
                    empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                    empOT.mealAllow = 1
                    addToSustenanceMap(tmpPayLocation, empOT)
                    break
                    case REV_STATUS_RVSD:
                    //7.4.4.3.3 Deleted (Reversed) Overtime
                    //Determine if the transaction was modified since the last pay update was run by comparing
                    //the last modified date and time on the work code (MSF891-LAST_MOD_DATE /MSF891-LAST_MOD_TIME)
                    //with the run date and time from the TG1 pay group (MSF817- RUN_DATE / MSF817-RUN_TIME)
                    MSF817Rec payHistoryRec = getPayUpdatesHistory(empStartStopRec.getPrimaryKey().getPayGroup())
                    String histRunDateTime = (payHistoryRec?.getRunDate().trim().length() == 0 ? "00000000" : payHistoryRec?.getRunDate()) +
                            (payHistoryRec?.getRunTime().trim().length() == 0 ? "000000" : payHistoryRec?.getRunTime())
                    String lastModDateTime = (empStartStopRec?.getLastModDate().trim().length() == 0 ? "00000000" : empStartStopRec?.getLastModDate()) +
                            (empStartStopRec?.getLastModTime().trim().length() == 0 ? "000000" : empStartStopRec?.getLastModTime())
                    if(lastModDateTime.compareTo(histRunDateTime) >= 0) {
                        empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                        empOT.mealAllow = 1
                        addToSustenanceMap(tmpPayLocation, empOT)
                    }
                    break
                    default :
                    //7.4.4.3.2 New Sustenance Allowance NOT Attached to Overtime Work Code
                    if(isCorrespondingApprovedTransactionExist(empStartStopRec)) {
                        empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                        empOT.mealAllow = -1
                        addToSustenanceMap(tmpPayLocation, empOT)
                    }
                    //7.4.4.3.4 Deleted (Reversed) Sustenance Allowance NOT Attached to Overtime Work Code
                    else if(isCorrespondingPaidReplacedTransactionExist(empStartStopRec)) {
                        //Determine if the transaction was modified since the last pay update was run by comparing
                        //the last modified date and time on the work code (MSF891-LAST_MOD_DATE /MSF891-LAST_MOD_TIME)
                        //with the run date and time from the TG1 pay group (MSF817- RUN_DATE / MSF817-RUN_TIME)
                        MSF817Rec payHistoryRec = getPayUpdatesHistory(empStartStopRec.getPrimaryKey().getPayGroup())
                        String histRunDateTime = (payHistoryRec?.getRunDate().trim().length() == 0 ? "00000000" : payHistoryRec?.getRunDate()) +
                                (payHistoryRec?.getRunTime().trim().length() == 0 ? "000000" : payHistoryRec?.getRunTime())
                        String lastModDateTime = (empStartStopRec?.getLastModDate().trim().length() == 0 ? "00000000" : empStartStopRec?.getLastModDate()) +
                                (empStartStopRec?.getLastModTime().trim().length() == 0 ? "000000" : empStartStopRec?.getLastModTime())
                        if(lastModDateTime.compareTo(histRunDateTime) >= 0) {
                            empOT.tranMealAllowCode  = batchParams.mealAllowanceCode
                            empOT.mealAllow = 1
                            addToSustenanceMap(tmpPayLocation, empOT)
                        }
                    }
                    break
                }
                break
        }
    }

    /**
     * Read the employee record based on the employee id
     * @param employeeId employee id
     * @return employee record
     */
    private MSF810Rec getEmployee(String employeeId) {
        info("getEmployee")
        MSF810Key empKey = new MSF810Key(employeeId)
        MSF810Rec empRec
        try{
            empRec = edoi.findByPrimaryKey(empKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Cannot read MSF810Rec caused by ${e.getMessage()}")
            empRec = null
        }
        return empRec
    }

    /**
     * Convert the date format with specified separator <code>(YY/MM/DD)</code>.
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
     * Check if a work code exist in the param work code.
     * @param workCode work code
     * @return true if exist
     */
    private boolean isWorkCodeExist(String workCode) {
        info("isWorkCodeExist ${workCode}")
        for(String overtimeCode : overtimeWorkCodes) {
            if(overtimeCode?.trim().equals(workCode.trim())) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a Shift Award Code exist in the param awards code.
     * @param shiftAward Shift Award Code
     * @return true if exist
     */
    private boolean isShiftAwardCodeExist(String shiftAward) {
        info("isShiftAwardCodeExist ${shiftAward}")
        boolean exist = false
        for(String shiftAwd : shiftWorkerAwards) {
            if(shiftAwd?.trim().equals(shiftAward.trim())) {
                return true
            }
        }
        return false
    }

    /**
     * Convert Period (HH:mm) into minutes. 
     * @param bd period as big decimal
     * @return minutes
     */
    private int convertPeriodToMinute(BigDecimal bd) {
        info("convertPeriodToMinute")
        int minutes = 0
        int[] parse = breakBigDecimal(bd)
        return (parse[0] * 60) + parse[1]
    }

    /**
     * Read the employee award record based on the employee id where award date within transaction date
     * @param employeeId employee id
     * @param trnDate transaction date
     * @return employee award record
     */
    private MSF828Rec getEmployeeAward(String employeeId, String trnDate) {
        info("getEmployeeAward")
        QueryImpl qMSF828 = new QueryImpl(MSF828Rec.class).
                and(MSF828Key.employeeId.equalTo(employeeId))
        MSF828Rec awardRec
        int trnDate_9 = trnDate as int
        edoi.search(qMSF828) {MSF828Rec msf828Rec->
            int endDate_9   = msf828Rec.getEndDate() as int
            int startDate_9 = (99999999 - (msf828Rec.getPrimaryKey().getInvStrDate() as int))
            if((trnDate_9 >= startDate_9 && trnDate_9 <= endDate_9) || (trnDate_9 >= startDate_9 && endDate_9==0)) {
                awardRec = msf828Rec
            }
        }
        return awardRec
    }

    /**
     * Check if a corresponding APPR - Approved transaction for the same work code on the same date
     * @param starStopEmp MSF891
     * @return true if exist
     */
    private boolean isCorrespondingApprovedTransactionExist(MSF891Rec empStartStopRec) {
        info("isCorrespondingApprovedTransactionExist ${empStartStopRec.getPrimaryKey().employeeId} - ${empStartStopRec.getPrimaryKey().trnDate}")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.employeeId.equalTo(empStartStopRec.getPrimaryKey().employeeId)).
                and(MSF891Key.trnDate.equalTo(empStartStopRec.getPrimaryKey().trnDate)).
                and(MSF891Key.payGroup.equalTo(empStartStopRec.getPrimaryKey().payGroup)).
                and(MSF891Rec.workCode.equalTo(empStartStopRec.workCode)).
                and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_APPR))
        return !edoi.search(qMSF891).getResults().isEmpty()
    }

    /**
     * Check if a corresponding PAID/RPLD transaction for the same work code on the same date
     * @param starStopEmp MSF891
     * @return true if exist
     */
    private boolean isCorrespondingPaidReplacedTransactionExist(MSF891Rec empStartStopRec) {
        info("isCorrespondingPaidReplacedTransactionExist ${empStartStopRec.getPrimaryKey().employeeId} - ${empStartStopRec.getPrimaryKey().trnDate}")
        QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class).
                and(MSF891Key.employeeId.equalTo(empStartStopRec.getPrimaryKey().employeeId)).
                and(MSF891Key.trnDate.equalTo(empStartStopRec.getPrimaryKey().trnDate)).
                and(MSF891Key.payGroup.equalTo(empStartStopRec.getPrimaryKey().payGroup)).
                and(MSF891Rec.workCode.equalTo(empStartStopRec.workCode)).
                and(MSF891Rec.tranApprStatus.equalTo(TRN_STATUS_PAID)).
                and(MSF891Rec.reverseStatus.equalTo(REV_STATUS_RPLD))
        return !edoi.search(qMSF891).getResults().isEmpty()
    }

    /**
     * Get the latest pay update history based on the pay group.
     * Latest record is the smallest INV_END_DATE where PAY_GROUP = TG1 and PAY_RUN_TYPE = U
     * @param payGroup pay group
     * @return latest pay update history
     */
    private MSF817Rec getPayUpdatesHistory(String payGroup) {
        info("getPayUpdatesHistory")
        //First get the smallest inv_end_date
        String invEndDate
        QueryImpl qMSF817 = new QueryImpl(MSF817Rec.class).
                and(MSF817Key.payGroup.equalTo(payGroup)).
                and(MSF817Key.payRunType.equalTo("U")).
                min(MSF817Key.invEndDate)
        invEndDate = edoi.firstRow(qMSF817)
        //Then find the MSF817Rec with smallest inv_end_date same as above
        MSF817Rec payUpdateHistRec
        qMSF817 = new QueryImpl(MSF817Rec.class).
                and(MSF817Key.payGroup.equalTo(payGroup)).
                and(MSF817Key.payRunType.equalTo("U")).
                and(MSF817Key.invEndDate.equalTo(invEndDate))
        return edoi.firstRow(qMSF817)
    }

    /**
     * Is Shift Type valid (A, M, D, N)
     * @param shiftType Shift Type
     * @return true if valid
     */
    private boolean isShiftTypeValid(String shiftType) {
        info("isShiftTypeValid")
        return SHIFT_TYPE.contains(shiftType)
    }

    /**
     * Get Allowance History from employe start stop
     * @param empStartStopRec employe start stop
     * @return Allowance History
     */
    private MSF89WRec getAllowanceHistory(MSF891Rec empStartStopRec) {
        info("getAllowanceHistory")
        QueryImpl qMSF89W = new QueryImpl(MSF89WRec.class).
                and(MSF89WKey.employeeId.equalTo(empStartStopRec.getPrimaryKey().employeeId)).
                and(MSF89WKey.payGroup.equalTo(empStartStopRec.getPrimaryKey().payGroup)).
                and(MSF89WKey.trnDate.equalTo(empStartStopRec.getPrimaryKey().trnDate))
        MSF89WRec allowanceHistRec
        return edoi.firstRow(qMSF89W)
    }

    /**
     * Get the Sustance Allowance History Rec from employee start stop
     * @param empStartStopRec employee start stop
     * @return Sustance Allowance History Rec
     */
    private MSF89WRec getSustenanceAllowance(MSF891Rec empStartStopRec) {
        info("getSustenanceAllowance ${empStartStopRec.getPrimaryKey().employeeId} - ${empStartStopRec.getPrimaryKey().trnDate}")
        MSF89WRec allowanceHistRec = getAllowanceHistory(empStartStopRec)
        if(allowanceHistRec) {
            String earnCode = allowanceHistRec.getPrimaryKey().getEarnCode()
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
                        return allowanceHistRec
                    }
                }
            }
        }
        return null
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
     * Construct report content.
     */
    private void constructReport() {
        info("constructReport")

        //array list for PayLoc
        payLocList = new ArrayList <String>()

        for(String key : employeesOnNormalHours.keySet()) {
            debug("employeesOnNormalHoursPayLoc: ${key}")
            if (!payLocList.contains(key)){
                payLocList.add(key)
            }
        }
        for(String key : employeesOnSustenance.keySet()) {
            debug("employeesOnSustenancePayLoc: ${key}")
            if (!payLocList.contains(key)){
                payLocList.add(key)
            }
        }

        //write header
        writeReportHeader()
        //iterate each map
        //		if(!employeesOnNormalHours.isEmpty()) {
        payLocList.each { String pLoc ->
            //			employeesOnNormalHours.each {payLoc, normalEmployees->
            if(groupedByAllPayLoc) {
                String payLocationName = getPayLocationName(pLoc)
                reportWriter.write(String.format(REPORT_SUB_HEAD_3, pLoc, payLocationName))
                reportWriter.write("")
            }

            ArrayList<EmployeeOvertime> normalEmployees = employeesOnNormalHours.get(pLoc)
            if(normalEmployees) {
                writeEmployeeWithinNormalReport(normalEmployees)
            }
            ArrayList<EmployeeOvertime> sustenanceEmployees = employeesOnSustenance.get(pLoc)
            if(sustenanceEmployees) {
                writeEmployeeWhileSustenanceReport(sustenanceEmployees)
            }
        }
    }

    /**
     * Construct the csv.
     */
    private void constructCSV() {
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
        csvWriter.write("\r\n")
        //iterate each map
        //		if(!employeesOnNormalHours.isEmpty()) {
        payLocList.each { String pLoc ->
            ArrayList<EmployeeOvertime> normalEmployees = employeesOnNormalHours.get(pLoc)
            if(normalEmployees) {
                Collections.sort(normalEmployees)
                normalEmployees.each {EmployeeOvertime eOT->
                    writeCSVRecord(eOT)
                }
            }
            ArrayList<EmployeeOvertime> sustenanceEmployees = employeesOnSustenance.get(pLoc)
            if(sustenanceEmployees) {
                Collections.sort(sustenanceEmployees)
                sustenanceEmployees.each {EmployeeOvertime eOT->
                    writeCSVRecord(eOT)
                }
            }
        }
    }

    /**
     * Write Employee Overtime record into CSV.
     * @param eOT Employee Overtime record
     */
    private void writeCSVRecord(EmployeeOvertime eOT) {
        info("writeCSVRecord")
        if(eOT.tranMealAllowCode?.trim()) {
            csvWriter.write(String.format(CSV_DETAIL,
                    eOT.employeeId,
                    eOT.date,
                    eOT.tranType,
                    eOT.tranMealAllowCode,
                    eOT.mealAllow
                    ))
            csvWriter.write("\r\n")
        }
        if(eOT.tranMealBreakCode?.trim()) {
            csvWriter.write(String.format(CSV_DETAIL,
                    eOT.employeeId,
                    eOT.date,
                    eOT.tranType,
                    eOT.tranMealBreakCode,
                    eOT.mealBreak
                    ))
            csvWriter.write("\r\n")
        }
    }

    /**
     * Write report header.
     */
    private void writeReportHeader() {
        info("writeReportHeader")
        if(!headerWritten) {
            reportWriter = report.open(REPORT_NAME)
            reportWriter.write(REPORT_TITLE.center(132))
            reportWriter.write(DASHED_LINE)
            String payLocationName = !groupedByAllPayLoc ?
                    getPayLocationName(batchParams.payLocation) : "(Blank For All)"
            String payPeriod = convertDateFormat(endPayPeriod)
            reportWriter.write(String.format(REPORT_TITLE_2,
                    batchParams.payLocation, payLocationName, payPeriod).center(132))
            reportWriter.write(DASHED_LINE)
            reportWriter.write("")
            headerWritten = true
        }
    }
    /**
     * Write employee within normal list into report.
     * @param payLoc Pay Location
     * @param employees Employee List
     */
    private void writeEmployeeWithinNormalReport(ArrayList<EmployeeOvertime> employees) {
        info("writeEmployeeWithinNormalReport")
        reportWriter.write(REPORT_SUB_HEAD_1)
        reportWriter.write(DASHED_LINE)
        reportWriter.write(REPORT_HEADING_1_1)
        reportWriter.write(REPORT_HEADING_1_2)
        reportWriter.write(DASHED_LINE)
        Collections.sort(employees)
        employees.each {
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
                    it.mealAllow,
                    it.mealBreak
                    ))
        }
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
    }

    /**
     * Write employee while on sustenance list into report.
     * @param payLoc Pay Location
     * @param employees Employee List
     */
    private void writeEmployeeWhileSustenanceReport(ArrayList<EmployeeOvertime> employees) {
        info("writeEmployeeWhileSustenanceReport")
        reportWriter.write(REPORT_SUB_HEAD_2)
        reportWriter.write(DASHED_LINE)
        reportWriter.write(REPORT_HEADING_2_1)
        reportWriter.write(REPORT_HEADING_2_2)
        reportWriter.write(DASHED_LINE)
        Collections.sort(employees)
        employees.each {
            reportWriter.write(String.format(REPORT_DETAIL_2,
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
                    it.mealAllow
                    ))
        }
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
    }

    /**
     * Write error report.
     */
    private void writeErrorReport() {
        info("writeErrorReport")
        writeReportHeader()
        reportWriter.write(REPORT_ERROR_TITLE)
        reportWriter.write(DASHED_LINE)
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
}

/**
 * Run the script
 */
ProcessTrb899 process = new ProcessTrb899()
process.runBatch(binding)
