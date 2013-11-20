/**
 * @Ventyx 2012
 * 
 * This is a batch to produce TransGrid reports required by the Australia 
 * Bureau of Statistics (ABS).
 * The reports are Average Weekly Earnings Survey Form and Employment and 
 * Earnings Survey Form.
 * 
 * This program developed using edoi instead of service call.
 * 
 * Developed based on <b>FDD Report ABS Survey D03.docx</b>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf835.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.ellipse.service.CollectionServiceOperation;
import com.mincom.enterpriseservice.ellipse.table.*;

import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import groovy.time.*;

import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*

/**
 * Request parameters for Trbabs.
 */
public class ParamsTrbabs{
    private String requiredReportDetail
    private String periodEndDate
    private String periodStartDate
}

public class TimeHelperTrbabs {

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
        Calendar cal = TimeHelperTrbabs.getTimeInCalendar(inputDate, "0000")
        //http://www.timeanddate.com/date/dateadd.html
        cal.add(column, numberOfAddition)
        
        return (cal.format("yyyyMMdd"))
    }
    
    public static Boolean isDay(String inputDate, Integer day) {
        Calendar cal = TimeHelperTrbabs.getTimeInCalendar(inputDate, "0000")
        return cal.get(Calendar.DAY_OF_WEEK) == day
    }
    
    public static String getNearestDay(String inputDate, Integer day, Integer searchDay ) {
        String processDate = inputDate
        while(!isDay(processDate,day)) {
            processDate = getAddedDate(processDate,Calendar.DAY_OF_YEAR, searchDay)
        }
        return processDate
    }
}


/**
 * Main process of Trbabs.
 */
public class ProcessTrbabs extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_A_NAME          = "TRBABSA"
    private static final String REPORT_B_NAME          = "TRBABSB"
    private static final String REPORT_C_NAME          = "TRBABSC"
    private static final String MALE                   = "M"
    private static final String FEMALE                 = "F"

    private static final String WORKING_PAY_GROUP      = "TG1"
    private static final String[] VALID_GENDER         = ["M","F"]
    private static final String[] VALID_REPORT_MODE    = ["A", "B"]
    private static final String[] SALARY_WAGES_CODES   = ["GW", "O",  "AL",  "LF",  "LH",  "SF",  "SH",  "SN",  "ST",  "LW",  "CO",  "PL",  "ML",  "AA",  "MI",  "MH",  "AS",  "AC",  "AW",  "DF"]
    private static final String[] SEVERENCE_CODES      = ["SP", "VR"]
    private static final String[] GROSS_WEEKLY_CODES   = ["GW"]
    private static final String[] OVERTIME_CODES       = ["O"]
    private static final String[] SAL_SAC_CODES        = ["S7"]
    private static final Integer MAX_RECORD_RESULT     = 1000
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 2

    private ParamsTrbabs batchParams
    private String startPayPeriod, endPayPeriod
    private EmployeePersonnelResultDTO currEmployee
    private String prevEmpId                      = " "
    private Boolean isEmployeeInclude             = false
    private Boolean isFullTimeEmployee            = false
    private Boolean isValueAddedForEmp            = false

    private ArrayList<String> processedEmp        = new ArrayList<String>()        
    private Long maleFTEmployee                   = 0
    private Long maleOtherEmployee                = 0
    private Long femaleFTEmployee                 = 0
    private Long femaleOtherEmployee              = 0
    private BigDecimal maleFTSacEarning           = 0
    private BigDecimal maleOtherSacEarning        = 0
    private BigDecimal femaleFTSacEarning         = 0
    private BigDecimal femaleOtherSacEarning      = 0
    private BigDecimal maleFTTaxGrossWeekly       = 0
    private BigDecimal maleOtherTaxGrossWeekly    = 0
    private BigDecimal femaleFTTaxGrossWeekly     = 0
    private BigDecimal femaleOtherTaxGrossWeekly  = 0
    private BigDecimal maleFTWeeklyOvertime       = 0
    private BigDecimal maleOtherWeeklyOvertime    = 0
    private BigDecimal femaleFTWeeklyOvertime     = 0
    private BigDecimal femaleOtherWeeklyOvertime  = 0
    private BigDecimal grossWagesAndSalary        = 0
    private BigDecimal severanceTermRedundant     = 0

    /*
     * Reporting variables 
     */
    private def reportWriterA
    private def reportWriterB
    private def reportWriterC

    /**
     * Run the main batch
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);
        

        batchParams = params.fill(new ParamsTrbabs())

        try {
            processBatch();
        } catch(Exception e) {
            info("Error ${e.getMessage()}")
        } finally {
        
            if(reportWriterA != null) {
                reportWriterA.close()
            }
            if(reportWriterB != null) {
                reportWriterB.close()
            }
            if(reportWriterC != null) {
                reportWriterC.close()
            }
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch");

        if(initializeAndValidate()) {
            browseEmployeePayTransHist()
            printReport()
        }
    }



    /**
     * Initialize derived values and validate request parameters.
     * @return true if request parameters are valid, false if otherwise
     */
    private Boolean initializeAndValidate() {
        info("isValidRequestParameters")
        Boolean valid = true

        
        reportWriterC = report.open(REPORT_C_NAME)
        reportWriterC.write("".padRight(132, "-"))
        reportWriterC.write("TRBABS Error Report".center(132))
        reportWriterC.write("".padRight(132, "-"))
        reportWriterC.write("")
        reportWriterC.write("  Field Ref/ Value                  Error/ Warning MessageColumn Hdg")
        


        if(!(batchParams.requiredReportDetail.trim() in VALID_REPORT_MODE)) {
            reportWriterC.write("  Required report Detail            Invalid input, valid input are ${VALID_REPORT_MODE.join(", ")}")
            valid = false
        }
        

        endPayPeriod  = TimeHelperTrbabs.getNearestDay(batchParams.periodEndDate.trim(), Calendar.FRIDAY, 1)
        String oneYearBefore = getStartPayPeriod(endPayPeriod, 52)
        
        if(!batchParams.periodStartDate.trim().equals("")) {
            if(batchParams.requiredReportDetail.trim().equals("A")) {
                reportWriterC.write("  Start Pay Period                  Start pay period not required")
                valid = false
            }
            else {
                startPayPeriod = TimeHelperTrbabs.getNearestDay(batchParams.periodStartDate.trim(), Calendar.SATURDAY, -1)
                if(startPayPeriod.toLong()<oneYearBefore.toLong()) {
                    reportWriterC.write("  Start Pay Period                  Start pay period must be less than 364 from end period")
                    valid = false
                }
                
            }
                
        }
        else {
            if(batchParams.requiredReportDetail.trim().equals("A")) {
                startPayPeriod = getStartPayPeriod(endPayPeriod, 1)
            }
            else {
                startPayPeriod = oneYearBefore
            }
        }
             
        MSF801_PG_801Rec payGroupRec= getPayGroupRecord(WORKING_PAY_GROUP)
        if(payGroupRec!=null) {
            String currStartPeriod = payGroupRec.curStrDtPg
            String currEndPeriod   = payGroupRec.curEndDtPg
            if(endPayPeriod.toLong()>=currStartPeriod.toLong() && endPayPeriod.toLong()<=currEndPeriod.toLong()) {
                reportWriterC.write("  End Pay Period                    End pay period cannot be in the current period")
                valid = false
            }
            
        }
        return valid
    }

    /**
     * The method browse all employee pay transaction history and process the found record accordingly.
     */
    private void browseEmployeePayTransHist() {
        info("browseEmployeePayTransHist")

        Constraint cEmpId     = MSF835Key.employeeId.greaterThanEqualTo(" ")
        Constraint cTrnDate   = (MSF835Key.trnDate.greaterThanEqualTo(startPayPeriod)).and(MSF835Key.trnDate.lessThanEqualTo(endPayPeriod))
        Constraint cPayRunTyp = MSF835Rec.payRunType.equalTo("U")
        Constraint cTranInd   = (MSF835Key.tranInd.equalTo("1")).or(MSF835Key.tranInd.equalTo("2"))

        QueryImpl query = new QueryImpl(MSF835Rec.class).and(cEmpId).and(cTrnDate).and(cPayRunTyp).and(cTranInd).orderBy(MSF835Rec.msf835Key)

        edoi.search(query,MAX_RECORD_RESULT) {MSF835Rec empHistTranRec ->
            processEmpPayTransHist(empHistTranRec)
        }

    }

    /**
     * This method print the report based on the required report detail batch parameter.
     * If the required report detail is 'A', Average Weekly Earnings Survey Form is printed.
     * If the required report detail is 'B', Employment and Earnings Survey Form is printed.
     */
    private void printReport() {
        info("printReport")

        DecimalFormat dfInteger   = new DecimalFormat("###,##0")
        DecimalFormat dfDecimal   = new DecimalFormat("##,###,##0.00")
        String dispStartPayPeriod = startPayPeriod.substring(6,8)+"/"+startPayPeriod.substring(4,6)+"/"+startPayPeriod.substring(2,4)
        String dispEndPayPeriod   = endPayPeriod.substring(6,8)+"/"+endPayPeriod.substring(4,6)+"/"+endPayPeriod.substring(2,4)

        if(batchParams.requiredReportDetail.trim().equals("A")) {
            BigDecimal totalMaleFTAmount = maleFTSacEarning + maleFTTaxGrossWeekly + maleFTWeeklyOvertime
            BigDecimal totalFemaleFTAmount = femaleFTSacEarning + femaleFTTaxGrossWeekly + femaleFTWeeklyOvertime
            BigDecimal totalMaleOtherAmount = maleOtherSacEarning + maleOtherTaxGrossWeekly + maleOtherWeeklyOvertime
            BigDecimal totalFemaleOtherAmount = femaleOtherSacEarning + femaleOtherTaxGrossWeekly + femaleOtherWeeklyOvertime
            
            reportWriterA = report.open(REPORT_A_NAME)
            reportWriterA.write("")
            reportWriterA.write("Average Weekly Earnings Survey".center(132))
            reportWriterA.write("".padRight(132, "-"))
            reportWriterA.write("For the period: ${dispStartPayPeriod} - ${dispEndPayPeriod}".center(132))
            reportWriterA.write("".padRight(132, "-"))
            reportWriterA.write("")
            reportWriterA.write("                                                           Full-time Employees                       All Other Employees")
            reportWriterA.write("")
            reportWriterA.write("                                                          Male            Female                     Male            Female")
            reportWriterA.write("  Number of Employees                                     ${dfInteger.format(maleFTEmployee).padLeft(7)}         ${dfInteger.format(femaleFTEmployee).padLeft(7)}                    ${dfInteger.format(maleOtherEmployee).padLeft(7)}         ${dfInteger.format(femaleOtherEmployee).padLeft(7)}")
            reportWriterA.write("  Weekly Salary Sacrificed Earnings                 ${dfDecimal.format(maleFTSacEarning).padLeft(13)}   ${dfDecimal.format(femaleFTSacEarning).padLeft(13)}              ${dfDecimal.format(maleOtherSacEarning).padLeft(13)}   ${dfDecimal.format(femaleOtherSacEarning).padLeft(13)}")
            reportWriterA.write("  Taxable Gross Weekly Earnings                     ${dfDecimal.format(maleFTTaxGrossWeekly).padLeft(13)}   ${dfDecimal.format(femaleFTTaxGrossWeekly).padLeft(13)}              ${dfDecimal.format(maleOtherTaxGrossWeekly).padLeft(13)}   ${dfDecimal.format(femaleOtherTaxGrossWeekly).padLeft(13)}")
            reportWriterA.write("  Weekly Overtime Earnings                          ${dfDecimal.format(maleFTWeeklyOvertime).padLeft(13)}   ${dfDecimal.format(femaleFTWeeklyOvertime).padLeft(13)}              ${dfDecimal.format(maleOtherWeeklyOvertime).padLeft(13)}   ${dfDecimal.format(femaleOtherWeeklyOvertime).padLeft(13)}")
            reportWriterA.write("  Total Earnings and Overtime                       ${dfDecimal.format(totalMaleFTAmount).padLeft(13)}   ${dfDecimal.format(totalFemaleFTAmount).padLeft(13)}              ${dfDecimal.format(totalMaleOtherAmount).padLeft(13)}   ${dfDecimal.format(totalFemaleOtherAmount).padLeft(13)}")
            reportWriterA.write("".padRight(132, "-"))
        }
        else {
            Long totalEmployee = maleFTEmployee+femaleFTEmployee+maleOtherEmployee+femaleOtherEmployee
            BigDecimal totalSacrificedEarnings = maleFTSacEarning+femaleFTSacEarning+maleOtherSacEarning+femaleOtherSacEarning
            BigDecimal totalAmount = grossWagesAndSalary+severanceTermRedundant+totalSacrificedEarnings
            
            reportWriterB = report.open(REPORT_B_NAME)
            reportWriterB.write("")
            reportWriterB.write("Employment and Earnings Survey".center(132))
            reportWriterB.write("".padRight(132, "-"))
            reportWriterB.write("For the period: ${dispStartPayPeriod} - ${dispEndPayPeriod}".center(132))
            reportWriterB.write("".padRight(132, "-"))
            reportWriterB.write("")
            reportWriterB.write("  Gross Earnings for the Year: ")
            reportWriterB.write("         Gross Wages and Salaries                                               ${dfDecimal.format(grossWagesAndSalary).padLeft(14)}")
            reportWriterB.write("         Severance, Termination and Redundancy Payments                         ${dfDecimal.format(severanceTermRedundant).padLeft(14)}")
            reportWriterB.write("         Salary Sacrificed Earnings                                             ${dfDecimal.format(totalSacrificedEarnings).padLeft(14)}")
            reportWriterB.write("         Total                                                                  ${dfDecimal.format(totalAmount).padLeft(14)}")
            reportWriterB.write(" Total Number of Employees                 ${dfInteger.format(totalEmployee).padLeft(7)}")
            reportWriterB.write("".padRight(132, "-"))
        }
    }

    /**
     * This method process all found employee history transaction record.
     * @param empHistTranRec
     */
    private void processEmpPayTransHist(MSF835Rec empHistTranRec) {
        info("processEmpPayTransHist")


        //Set the new values for the current employee.
        currEmployee       = getEmpPersonell(empHistTranRec.primaryKey.employeeId)
        isEmployeeInclude  = isEmployeeIncluded(currEmployee)
        isValueAddedForEmp = false


        if(isEmployeeInclude && (currEmployee.gender in VALID_GENDER)) {

            String[] miscRptFieldArray = getMiscRptFieldArray(empHistTranRec)

            BigDecimal amount = empHistTranRec.hdAmount
            if(amount==0) {
                amount = empHistTranRec.amount
            }
            
            isFullTimeEmployee = isFullTimeEmployee(currEmployee)

            //Salary sacrifice is used by both reports
            if(isInCodeList(miscRptFieldArray, SAL_SAC_CODES)) {
                isValueAddedForEmp = true
                if(currEmployee.gender.equals(MALE)) {
                    if(isFullTimeEmployee) {
                        maleFTSacEarning      = maleFTSacEarning + amount
                    }
                    else {
                        maleOtherSacEarning   = maleOtherSacEarning + amount
                    }
                }
                else {
                    if(isFullTimeEmployee) {
                        femaleFTSacEarning    = femaleFTSacEarning + amount
                    }
                    else {
                        femaleOtherSacEarning = femaleOtherSacEarning + amount
                    }
                }
            }
            
            //Taxable gross weekly earnings
            if(batchParams.requiredReportDetail.trim().equals("A")) {
                if(isInCodeList(miscRptFieldArray, GROSS_WEEKLY_CODES)) {
                    isValueAddedForEmp = true
                    if(currEmployee.gender.equals(MALE)) {
                        if(isFullTimeEmployee) {
                            maleFTTaxGrossWeekly      = maleFTTaxGrossWeekly + amount
                        }
                        else {
                            maleOtherTaxGrossWeekly   = maleOtherTaxGrossWeekly + amount
                        }
                    }
                    else {
                        if(isFullTimeEmployee) {
                            femaleFTTaxGrossWeekly    = femaleFTTaxGrossWeekly + amount
                        }
                        else {
                            femaleOtherTaxGrossWeekly = femaleOtherTaxGrossWeekly + amount
                        }
                    }
                }
                //Weekly overtime earnings
                if(isInCodeList(miscRptFieldArray, OVERTIME_CODES)) {
                    isValueAddedForEmp = true
                    if(currEmployee.gender.equals(MALE)) {
                        if(isFullTimeEmployee) {
                            maleFTWeeklyOvertime      = maleFTWeeklyOvertime + amount
                        }
                        else {
                            maleOtherWeeklyOvertime   = maleOtherWeeklyOvertime + amount
                        }
                    }
                    else {
                        if(isFullTimeEmployee) {
                            femaleFTWeeklyOvertime    = femaleFTWeeklyOvertime + amount
                        }
                        else {
                            femaleOtherWeeklyOvertime = femaleOtherWeeklyOvertime + amount
                        }
                    }
                }
            }
                
            if(batchParams.requiredReportDetail.trim().equals("B")) {
                if(isInCodeList(miscRptFieldArray, SALARY_WAGES_CODES)) {
                    isValueAddedForEmp = true
                    grossWagesAndSalary = grossWagesAndSalary + amount
                }
                if(isInCodeList(miscRptFieldArray, SEVERENCE_CODES)) {
                    isValueAddedForEmp = true
                    severanceTermRedundant = severanceTermRedundant + amount
                }

            }

            if(isValueAddedForEmp) {
                if(!(currEmployee.employeeId in processedEmp.toArray())) {
                    processedEmp.add(currEmployee.employeeId)
                    if(currEmployee.gender.equals(MALE)) {
                        if(isFullTimeEmployee) {
                            maleFTEmployee++
                        }
                        else {
                            maleOtherEmployee++
                        }
                    }
                    else {
                        if(isFullTimeEmployee) {
                            femaleFTEmployee++
                        }
                        else {
                            femaleOtherEmployee++
                        }
                    }
                }
            }

        }
    }


    /**
     * Return the MiscRptField fields based on the input employee history transaction.
     * If the record is an earning, the fields are taken from the Earnings file.
     * If the record is a deduction, the fields are taken from the Deduction file.
     * @param empHistTranRec
     * @return array of MiscRptField fields if any is found, array of empty strings if not found.
     */
    private String[] getMiscRptFieldArray(MSF835Rec empHistTranRec) {
        info("getMiscRptFieldArray")

        String[] retStringArray = ["", "", "", "", ""]

        if(empHistTranRec.primaryKey.tranInd.equals("1")) {
            //Earnings
            MSF801_A_801Rec earningsRecord = getEarningsRecord(empHistTranRec.primaryKey.tranCode)
            if(earningsRecord!=null) {
                retStringArray = [earningsRecord.miscRptFldAx1, earningsRecord.miscRptFldAx2, earningsRecord.miscRptFldAx3, earningsRecord.miscRptFldAx4, earningsRecord.miscRptFldAx5]
            }
        }
        else {
            if(empHistTranRec.primaryKey.tranInd.equals("2")) {
                //Deductions
                MSF801_D_801Rec dednRecord = getDeductionRecord(empHistTranRec.primaryKey.tranCode)
                if(dednRecord!=null) {
                    retStringArray = [dednRecord.miscRptFldDx1, dednRecord.miscRptFldDx2, dednRecord.miscRptFldDx3, dednRecord.miscRptFldDx4, dednRecord.miscRptFldDx5]
                }
            }
        }

        return retStringArray
    }


    /**
     * This method returns true if any string in the input array is in the compare array.
     * @param inputArray
     * @param compareArray
     * @return true if any string in the input array is in the compare array.
     */
    private Boolean isInCodeList(String[] inputArray, String[] compareArray) {
        info("isInCodeList")

        Boolean isInCodeListRet = false
        for(inputCode in inputArray) {
            if((!inputCode.trim().equals("")) && (inputCode in compareArray)) {
                isInCodeListRet = true
                break
            }
        }

        return isInCodeListRet
    }

    /**
     * This method return true if the employee is a full time employee.
     * An employee is included if the employee type in EMTY table 4th associated value is 'Y'.
     * @param employee
     * @return true if the employee is a full time employee.
     */
    private Boolean isFullTimeEmployee(EmployeePersonnelResultDTO employee) {
        info("isFullTimeEmployee")
        Boolean isFullTime = false

        info("Emp type : ${employee.empType}")
        TableServiceReadReplyDTO empTypeTable = readTable("EMTY", employee.empType)
        if(empTypeTable!=null) {
            info("Assoc Rec : ${empTypeTable.associatedRecord}")
            if(empTypeTable.associatedRecord.padRight(7).substring(6,7).equals("Y")) {
                isFullTime=true
            }
        }

        return isFullTime
    }

    /**
     * This method return true if the employee is included in the report.
     * An employee is included if the employee staff category in STFC table 4th associated value is 'Y'.
     * @param employee
     * @return true if the employee is a full time employee.
     */
    private Boolean isEmployeeIncluded(EmployeePersonnelResultDTO employee) {
        info("isEmployeeIncluded")
        Boolean isIncluded = false

        info("Emp category : ${employee.staffCategory}")
        TableServiceReadReplyDTO empStaffCategTable = readTable("STFC", employee.staffCategory)
        if(empStaffCategTable!=null) {
            info("Assoc Rec : ${empStaffCategTable.associatedRecord}")
            if(empStaffCategTable.associatedRecord.padRight(7).substring(6,7).equals("Y")) {
                isIncluded=true
            }
        }

        return isIncluded
    }

    /**
     * Returns the employee personell data.
     * @param employeeId
     * @return the employee personell data
     */
    private EmployeePersonnelResultDTO getEmpPersonell(String employeeId) {
        info("getEmpPersonell")

        //        ScreenAppLibrary sl = new ScreenAppLibrary()
        //        EmployeePersonnelDTO empPersDTO = new EmployeePersonnelDTO()
        //        empPersDTO.setNextEemployeeId(employeeId)
        //        EmployeePersonnelResultDTO empPersReplyDTO = sl.readEmployeePersonnelDetails(empPersDTO)
        //        if(!empPersReplyDTO.errorMessage?.trim()){
        //            return empPersReplyDTO
        //        }else{
        //            return null
        //        }

        /*
         * Using EDOI as the screen service had a problem with
         * performance and invalid data in TransGrid Dev
         */
        EmployeePersonnelResultDTO empPersReplyDTO = new EmployeePersonnelResultDTO()
        MSF760Rec empRec              = getEmployeePersonellRecord(employeeId)
        empPersReplyDTO.employeeId    = empRec.primaryKey.employeeId
        empPersReplyDTO.staffCategory = empRec.staffCateg
        empPersReplyDTO.gender        = empRec.gender
        empPersReplyDTO.empType       = empRec.empType

        return empPersReplyDTO
    }

    private MSF760Rec getEmployeePersonellRecord(employeeId) {
        info("getEmployeePersonellRecord")

        MSF760Rec empRec = null
        try {
            empRec = edoi.findByPrimaryKey(new MSF760Key(employeeId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return empRec
    }

    /**
     * Get earnings based on the earnCode.
     * @param earnCode
     * @return MSF801_A_801Rec
     */
    private MSF801_A_801Rec getEarningsRecord(String earnCode) {
        info("getWorkCodeRecord")
        MSF801_A_801Rec earningRec = null
        try {
            earningRec = edoi.findByPrimaryKey(new MSF801_A_801Key("A","***${earnCode}"))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return earningRec
    }

    /**
     * Get deduction based on the dednCode.
     * @param dednCode
     * @return MSF801_D_801Rec
     */
    private MSF801_D_801Rec getDeductionRecord(String dednCode) {
        info("getDeductionRecord")
        MSF801_D_801Rec deductionRec = null
        try {
            deductionRec = edoi.findByPrimaryKey(new MSF801_D_801Key("D","***${dednCode}"))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return deductionRec
    }

    /**
     * Get Pay Group based on the pay code.
     * @param payCode Pay Group code
     * @return MSF801_PG_801Rec
     */
    private MSF801_PG_801Rec getPayGroupRecord(String payGroup) {
        info("getPayGroupRecord")
        MSF801_PG_801Rec payGroupRec = null
        try {
            payGroupRec = edoi.findByPrimaryKey(new MSF801_PG_801Key('PG', payGroup))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
        }
        return payGroupRec
    }



    /**
     * Get the start pay period date. The start pay period date is determined by substracting a number of weeks from the end period.
     * @param endPeriod
     * @param weeksBeforeEnd
     * @return start pay period date, empty string if the endPeriod is blank.
     */
    private String getStartPayPeriod(String endPeriod, Integer weeksBeforeEnd) {
        info("getStartPayPeriod")
        String resultDate = ""
        if(!endPeriod.trim().equals("")) {
            Integer payPeriod_9 = weeksBeforeEnd * -1
            resultDate = TimeHelperTrbabs.getAddedDate(TimeHelperTrbabs.getAddedDate(endPeriod, Calendar.WEEK_OF_YEAR, payPeriod_9),Calendar.DAY_OF_YEAR, 1)
        }
        
        return resultDate
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
            tableReplyDTO = service.get("TABLE").read(tableRequestDTO, false)
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            tableReplyDTO = null
        }
        return tableReplyDTO
    }

}

/*run script*/  
ProcessTrbabs process = new ProcessTrbabs();
process.runBatch(binding);