/**
 * @Ventyx 2012
 * 
 * This report is used to calculate the OTE salary for all employees and was
 * 
 * This program developed using edoi instead of service call.
 * 
 * Developed based on TRR76S, TRR76R, and <b>URS.RDL.TRR76S.Ordinary Time Earnings Salary Report.D03.docx</b>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf785.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf822.*;
import com.mincom.ellipse.edoi.ejb.msf835.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

/**
 * Request parameters for Trb76s.
 */
public class ParamsTrb76s{
    private String dateFrom
    private String dateTo
    private String employeeId0, employeeId1, employeeId2, employeeId3,
    employeeId4, employeeId5, employeeId6, employeeId7, employeeId8,
    employeeId9;
}



/**
 * Main process of Trb76s.
 */
public class ProcessTrb76s extends SuperBatch {
    
    
    private class ReportLineTrb76s implements Comparable<ReportLineTrb76s>{
    
        String empID         = " "
        String firstName     = " "
        String lastName      = " "
        String birthDate     = " "
        String termDate      = " "
        String superFundCode = " "
        String superFundDesc = " "
        BigDecimal salary    = 0
        String message       = " "
        String memberId      = " "
        
        static final String SEPARATOR = ","
        static final String SPACE = " "
        
        private String dateToDDMMYYYY(String date) {
            String processedDate = date.padRight(8)
            return date.substring(6, 8)+"/"+date.substring(4, 6)+"/"+date.substring(0, 4)
        }
        
        private String dateToDDMMYY(String date) {
            String processedDate = date.padRight(8)
            return date.substring(6, 8)+"/"+date.substring(4, 6)+"/"+date.substring(2, 4)
        }
        
        private String toCSV() {

            String returnString =                            superFundDesc
                   returnString = returnString + SEPARATOR + memberId
                   returnString = returnString + SEPARATOR + empID
                   returnString = returnString + SEPARATOR + lastName
                   returnString = returnString + SEPARATOR + firstName
                   returnString = returnString + SEPARATOR + dateToDDMMYYYY(birthDate)
                   returnString = returnString + SEPARATOR + dateToDDMMYYYY(termDate)
                   returnString = returnString + SEPARATOR + new DecimalFormat("#####0.00").format(salary)
                   returnString = returnString + SEPARATOR + message
            return returnString
        }
        
        private String toReport() {

            String returnString =                        enforceLength(superFundDesc,true,33)
                   returnString = returnString +         enforceLength(memberId,true,10)
                   returnString = returnString + SPACE + enforceLength(empID,true,10)
                   returnString = returnString + SPACE + enforceLength(lastName,true,20)
                   returnString = returnString + SPACE + enforceLength(firstName,true,13)
                   returnString = returnString + SPACE + enforceLength(dateToDDMMYY(birthDate),true,8)
                   returnString = returnString + SPACE + enforceLength(dateToDDMMYY(termDate),true,8)
                   returnString = returnString + SPACE + enforceLength(new DecimalFormat("#####0.00").format(salary),false,13)
                   returnString = returnString + SPACE + enforceLength(message,true,10)
            return returnString
        }
        
        private String enforceLength(String value, Boolean right, Integer numOfChar) {
            String result
            if(value==null) {
                value = ""
            }
            if(value.size()>numOfChar) {
                result = value.substring(0,numOfChar)
            }
            else {
                if(right) {
                    result = value.padRight(numOfChar)
                }
                else {
                    result = value.padLeft(numOfChar)
                }
            }
            return result
        }
        
        int compareTo(ReportLineTrb76s otherReportLine){
            if (!superFundCode.equals(otherReportLine.superFundCode)){
                return superFundCode.compareTo(otherReportLine.superFundCode)
            }
            if (!empID.equals(otherReportLine.empID)){
                return empID.compareTo(otherReportLine.empID)
            }
            return 0
        }
    }

    
    
    /*
     * Constants
     */
    private static final String OUTPUT_FILE_T            = "TRT76S"
    private static final String OUTPUT_FILE_O            = "TRO76S"
    private static final String[] SCAT_NOT_INCLUDED      = ["ZA","ZO","WC","IS"]
    private static final String[] EMP_STAT_INCLUDED      = ["A","Z"]
    private static final String[] EARN_MISC_VAL_INCL     = ["AL","AC","SX","0"]
    private static final String[] SUPER_DEDN             = ["A1","A2","A3","A4","A5","A6","A7","S1","S2","S3","S4","S5","S6","SC"]
    private static final String[] ENVELOPE_TYPE_INCL     = ["A","N", ""]
    
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 1

    private ParamsTrb76s batchParams
    private String[] employeeIds = new String[10]
    private BufferedWriter outputFileWriterO
    private File fileO
    private String dispDtFrom, dispDtTo
    private String taskUUID
    private BatchTextReports reportA

    /*
     * Reporting variables 
     */
    private def reportWriter

    /**
     * Run the main batch
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){
        
        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        //PrintRequest Parameters
        populateAndPrintRequestParams()

        try {
            processBatch();
        } catch(Exception e) {
            info("error ${e.printStackTrace()}")
            e.printStackTrace()
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- Trb76s ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")
        } finally {
            if(outputFileWriterO != null) {

                outputFileWriterO.close()
            }
            if(reportA != null) {
                reportA.close()
            }
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(fileO,"text/comma-separated-values", "TRO76S");
            }

        }
    }

        /**
     * Print request parameters.
     */
    private void populateAndPrintRequestParams() {
        info("populateAndPrintRequestParams")
        batchParams = params.fill(new ParamsTrb76s())
        
        info("Process Date From   : ${batchParams.dateFrom}")
        info("Process Date To     : ${batchParams.dateTo}")
        //Shift Awards
        (0..9).each {
            String fieldName = "employeeId${it}"
            String value = batchParams."$fieldName".toString().trim()
            if(value?.trim()) {
                employeeIds[it] = value
            }
            else{
                employeeIds[it] = " "
            }
            
            info("Employee ID ${it+1}    : ${employeeIds[it]}")
        }
        info("end populateAndPrintRequestParams")
    }
    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch");
        initialize()
        printReport(browseEmployees())
        info("end processBatch");
    }

    /**
     * Initialize report writer.
     */
    private void initialize() {
        info("initialize")
        //Initialize Report
        
        taskUUID = this.getTaskUUID()
        
        fileO = openFile(OUTPUT_FILE_O)
        outputFileWriterO = new BufferedWriter(new FileWriter(fileO))
        outputFileWriterO.write("Super-Fund,Member-Number,Employee ID,Emp-Last-Name,Emp-First-Name,Birth-Date,Termination-Date,OTE-Salary,Message\n")
        

        ArrayList<String> rptPgHead = new ArrayList<String>()
        rptPgHead.add("Super                            Member     Employee   Emp-Last             Emp-First      Birth  Termination       OTE     Message")
        rptPgHead.add("Fund                               Id       Id         Name                 Name           Date      Date         Salary           ")
        reportA = report.open("TRB76SA", rptPgHead)
        
        info("end initialize")
    }
    
    /**
     * This function reads the employee personnel records for eligible records and return them as lines to be printed.
     * Only lines with salary <> zero are returned.
     * The list returned is sorted.
     * @return list of report lines
     */
    private ArrayList<ReportLineTrb76s> browseEmployees() {
        info("browseEmployee")
        
        ArrayList<ReportLineTrb76s> lines = new ArrayList<ReportLineTrb76s>()
        
        Constraint cEmployeeId = getEmployeeIdConstraint()
        Constraint cStaffCateg = MSF760Rec.staffCateg.equalTo("IS").
                                    or(MSF760Rec.staffCateg.equalTo("WC")).
                                    or(MSF760Rec.staffCateg.equalTo("ZA")).
                                    or(MSF760Rec.staffCateg.equalTo("ZO"))
        Constraint cEmpStatus  = MSF760Rec.empStatus.equalTo("A").
                                    or(MSF760Rec.empStatus.equalTo("Z").
                                        and(MSF760Rec.termDate.greaterThanEqualTo(batchParams.dateFrom.trim())).
                                        and(MSF760Rec.termDate.notEqualTo("00000000")))                       
        QueryImpl query = new QueryImpl(MSF760Rec.class).and(cEmployeeId).and(cEmpStatus).andNot(cStaffCateg)
        
        edoi.search(query) {MSF760Rec msf760Rec ->
            ReportLineTrb76s reportLine = getReportLine(msf760Rec)
            if(reportLine.salary != 0) {
                info("Adding to lines")
                lines.add(reportLine)
            }
        }
        Collections.sort(lines)
        info("end browseEmployee")
        return lines

    }
    
    
    /**
     * This function builds the information for a report line based on the personnel record supplied and returns it
     * @param msf760Rec
     * @return report line
     */
    private ReportLineTrb76s getReportLine(MSF760Rec msf760Rec) {
        info("getReportLine for ${msf760Rec.primaryKey.employeeId}")
        
        ReportLineTrb76s line = new ReportLineTrb76s()
        
        line.superFundCode = msf760Rec.unionCode
        TableServiceReadReplyDTO superFundTable = readTable("UN", line.superFundCode)
        if(superFundTable) {
           line.superFundDesc =  superFundTable.description
        }
        
        line.empID = msf760Rec.primaryKey.employeeId
        MSF810Rec empDetailRec = readEmployee(line.empID)
        if(empDetailRec) {
            line.firstName = empDetailRec.firstName
            line.lastName  = empDetailRec.surname
        }
        
        line.birthDate = msf760Rec.birthDate
        line.termDate  = msf760Rec.termDate
        
        if(msf760Rec.empStatus.trim().equals("Z")) {
            line.message = "Terminated"
        }
        
        line.memberId = getMemberId(line.empID)
        info("Staff categ: ${msf760Rec.staffCateg}  - emp status: ${msf760Rec.empStatus}")
        if(!(msf760Rec.staffCateg in SCAT_NOT_INCLUDED) && (msf760Rec.empStatus in EMP_STAT_INCLUDED) && line.memberId!=null ) {
            line.salary = getOTESalary(line.empID)
        }
        
        info("end getReportLine, line is : ${line.toReport()}")
        return line
    }
    
    /**
     * This function calculates the salary for the employee
     * from the employee payroll history records.
     * @param employeeId
     * @return total OTE salary for the period.
     */
    private BigDecimal getOTESalary(String employeeId) {
        info("getOTESalary for ${employeeId}")
        
        BigDecimal salary = 0
        
        Constraint cEmployeeId = MSF835Key.employeeId.equalTo(employeeId)
        Constraint cPrdEndDate = MSF835Key.prdEndDate.greaterThanEqualTo(" ")
        Constraint cTranInd    = MSF835Key.tranInd.equalTo("1")
        Constraint cRunType    = MSF835Rec.payRunType.equalTo("U")
        Constraint cEnvType    = MSF835Key.envelopeType.notEqualTo("T")
        Constraint cRtrIgnore  = MSF835Rec.rtrIgnore.notEqualTo("Y")
        Constraint cTrnDate    = MSF835Key.trnDate.greaterThanEqualTo(batchParams.dateFrom.trim()).and(MSF835Key.trnDate.lessThanEqualTo(batchParams.dateTo.trim()))

        QueryImpl query = new QueryImpl(MSF835Rec.class).and(cEmployeeId).and(cPrdEndDate).and(cTranInd).and(cRunType).and(cRunType).and(cRtrIgnore).and(cTrnDate)
        
        edoi.search(query) {MSF835Rec msf835Rec ->
            BigDecimal amount = msf835Rec.amount
            if(msf835Rec.hdAmount!=0 && msf835Rec.hdaEarnCode.trim().equals("")) {
                amount = msf835Rec.hdAmount
            }
            
            if(msf835Rec.primaryKey.envelopeType.trim() in ENVELOPE_TYPE_INCL && amount!=0 && isProcessedTranCode(msf835Rec.primaryKey.tranCode)) {
                salary = salary + amount
            }
        }
        info("end getOTESalary, salary for ${employeeId} is ${salary.toString()}")
        return salary
     }
    
    
    /**
     * This parameters construct the employee id constraint based
     * on whether the employee id parameters are entered or not.
     * @return
     */
    private Constraint getEmployeeIdConstraint() {
        info("getEmployeeIdConstraint")
        
        Constraint cEmp;
        if(isArrayEmpty(employeeIds)) {
            cEmp = MSF760Key.employeeId.greaterThanEqualTo(" ")
        }
        else {
            Boolean firstEmp = true
            
            for(String empId: employeeIds) {
                if(empId?.trim()) {
                    if(firstEmp) {
                        firstEmp = false
                        cEmp = MSF760Key.employeeId.equalTo(empId)
                    }
                    else {
                        cEmp = cEmp.or(MSF760Key.employeeId.equalTo(empId))
                    }
                }
            }
        }
        info("end getEmployeeIdConstraint")
        return cEmp
    }


    /**
     * This method returns true if the element of the string array
     * entered are all blank or null.
     * @param inputArray
     * @return true if the element of the string array entered are all blank or null; false otherwise
     */
    private Boolean isArrayEmpty(String[] inputArray) {
        info("isArrayEmpty for array: ${inputArray.join(", ")}") 
        
        Boolean isEmpty = true
        for(String input: inputArray) {
            if(input?.trim()) {
                isEmpty = false
                break
            }
        }
        info("end isArrayEmpty, return value: ${isEmpty.toString()}")
        return isEmpty
    }
    
    
    /**
     * Get the member Id from Employee Deductions & Banking
     * or if not found there, the Employee Salary Package History.
     * @param employeeId
     * @return
     */
    private String getMemberId(String employeeId) {
        info("getMemberId for ${employeeId}")
        
        String todaysDate = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime())
        String memberId = getMemberFromEmpDeduction(employeeId, todaysDate)
        if(!memberId?.trim()) {
            String inverseToday = (99999999 - todaysDate.toLong()).toString()
            memberId = getMemberFromEmpSalPack(employeeId, inverseToday)
        }
        
        info("end getMemberId, memberId : ${memberId}")
        
        return memberId
    }
    
    /**
     * Get member id from Employee Deductions & Banking.
     * Current deduction code is where the start date is before the report date and/or the end date is after the report date.
     * The deduction code is then a super code where any of the Miscellaneous Report Fields contain the super dedn codes.
     * The member number is the value stored in the associated reference field.
     * @param employeeId
     * @param todaysDate
     * @return member id
     */
    private String getMemberFromEmpDeduction(String employeeId, String todaysDate) {
        info("getEmpDeduction emp: ${employeeId} today: ${todaysDate}")
        
        String returnValue = null
        
        Constraint cEmpId    = MSF822Key.employeeId.equalTo(employeeId)
        Constraint cConsPG   = MSF822Key.consPayGrp.equalTo("")
        Constraint cDednCode = MSF822Key.dednCode.greaterThanEqualTo(" ")
        Constraint cStrDate  = MSF822Rec.startDate.lessThanEqualTo(todaysDate)
        Constraint cEndDate  = MSF822Rec.endDate.equalTo("00000000").or(MSF822Rec.endDate.greaterThanEqualTo(todaysDate))
        
        QueryImpl query = new QueryImpl(MSF822Rec.class).and(cEmpId).and(cConsPG).and(cDednCode).and(cStrDate).and(cEndDate)
        
        edoi.search(query) {MSF822Rec msf822Rec ->
            if(returnValue==null) {
                if(isSuperannuationDedn(msf822Rec.primaryKey.dednCode)) {
                    returnValue = msf822Rec.dednRef
                }
            }
        }
        info("end getEmpDeduction, member from Emp Deduction: ${returnValue}")
        return returnValue
    }
    
    /**
     * Return true if the deduction code entered is a super deduction
     * by examining its misc rpt fields of the deduction.
     * @param dednCode
     * @return
     */
    private Boolean isSuperannuationDedn(String dednCode) {
        info("isSuperannuationDedn for ${dednCode}")
        
        Boolean isSuperDedn = false
        
        MSF801_D_801Rec dednRec = getDednCodeRecord(dednCode)
        if(dednRec!=null) {
            String[] inputArray = [dednRec.miscRptFldDx1,dednRec.miscRptFldDx2,dednRec.miscRptFldDx3,dednRec.miscRptFldDx4,dednRec.miscRptFldDx5]
            isSuperDedn = isInCodeList(inputArray,SUPER_DEDN)
        }
        
        info("isSuperannuationDedn, return value = ${isSuperDedn.toString()}")
        return isSuperDedn
    }
    
    /**
     * Get member id from Employee Salary Package History.
     * Look for the employee’s current salary package in MSF785 (the latest effective date on or before the report date).
     * With this record, look for a benefit type where the last 3 characters match a super deduction code.
     * The member number is the value stored in the associated reference field.
     * @param employeeId
     * @param inverseToday
     * @return member id
     */
    private String getMemberFromEmpSalPack(String employeeId, String inverseToday) {
        info("getMemberFromEmpSalPack emp: ${employeeId} inverseDate: ${inverseToday}")
        
        String returnValue = null
        
        Constraint cEmpId      = MSF785Key.employeeId.equalTo(employeeId)
        Constraint cInvEffDate = MSF785Key.invEffDate.greaterThanEqualTo(inverseToday)
        
        QueryImpl query = new QueryImpl(MSF785Key.class).and(cEmpId).and(cInvEffDate)
        MSF785Rec msf785Rec = (MSF785Rec)edoi.firstRow(query)
        if(msf785Rec) {
            Integer index = getBenefitIndex(msf785Rec)
            if(index <= 10) {
                String fieldName = "getBenefitRef_${index}"
                returnValue = msf785Rec."$fieldName"()
            }
            else {
                returnValue = " "
            }
        }
        
        info("end getMemberFromEmpSalPack, member from package: ${returnValue}")
        return returnValue
    }
    
    
    /**
     * Return the index of benefit type which is a super deduction by examining the last 3 characters of benefit type.
     * If not found, it will return a value more than maximum index (10 + 1 = 11)
     * @param msf785Rec
     * @return Return the index of benefit type which is a super deduction, if not found, it will return a value more than maximum index (10 + 1 = 11)
     */
    private Integer getBenefitIndex(MSF785Rec msf785Rec) {
        info("getBenefitIndex")
        
        Integer counter = 1
        Boolean found = false
        while(counter<=10 && !found) {
            String fieldName = "getBenefitType_${counter}"
            String dednCode = msf785Rec."$fieldName"()
            info("dednCode : ${dednCode}")
            if(dednCode?.trim() && isSuperannuationDedn(dednCode.padRight(4).substring(1,4))) {
                found=true
            }
            else {
                counter++
            }
        }
        
        info("end getBenefitIndex, index is ${counter.toString()}")
        return counter
    }
    
    /**
     * Return true if the transaction code entered is one of the processed code
     * by examining the misc rpt fld for the code as an earning.
     * @param tranCode
     * @return
     */
    private Boolean isProcessedTranCode(String tranCode) {
        info("isProcessedTranCode for ${tranCode}")
        
        Boolean isProcessed = false
        
        MSF801_A_801Rec earnRec = getEarnCodeRecord(tranCode)
        if(earnRec!=null) {
            String[] inputList = [earnRec.miscRptFldAx1,earnRec.miscRptFldAx2,earnRec.miscRptFldAx3,earnRec.miscRptFldAx4,earnRec.miscRptFldAx5]
            isProcessed = isInCodeList(inputList,EARN_MISC_VAL_INCL)
        }
        
        info("end isProcessedTranCode, return value: ${isProcessed.toString()}")
        return isProcessed
    }
    
    /**
     * This method returns true if any string in the input array is in the compare array.
     * @param inputArray
     * @param compareArray
     * @return true if any string in the input array is in the compare array.
     */
    private Boolean isInCodeList(String[] inputArray, String[] compareArray) {
        info("isInCodeList for ${inputArray.join(", ")} compared to ${compareArray.join(", ")}")

        Boolean isInCodeListRet = false
        for(inputCode in inputArray) {
            if((!inputCode.trim().equals("")) && (inputCode in compareArray)) {
                isInCodeListRet = true
                break
            }
        }

        info("end isInCodeList, return value: ${isInCodeListRet.toString()}")
        return isInCodeListRet
    }
    
    /**
     * Get Earnings based on the earnings code.
     * @param earn code
     * @return MSF801_A_801Rec
     */
    private MSF801_A_801Rec getEarnCodeRecord(String earnCode) {
        info("getWorkCodeRecord")
        MSF801_A_801Rec earningRec = null
        try {
            earningRec = edoi.findByPrimaryKey(new MSF801_A_801Key("A","***${earnCode}"))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("MSF801_A_801Rec not found")
        }
        info("end getWorkCodeRecord")
        return earningRec
    }
    
    /**
     * Get deduction based on the dedn code.
     * @param dedn code
     * @return MSF801_D_801Rec
     */
    private MSF801_D_801Rec getDednCodeRecord(String dednCode) {
        info("getDednCodeRecord")
        MSF801_D_801Rec dednRec = null
        try {
            dednRec = edoi.findByPrimaryKey(new MSF801_D_801Key("D","***${dednCode}"))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("MSF801_D_801Rec not found")
        }
        info("end getDednCodeRecord")
        return dednRec
    }


    /**
     * Read Table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO if exist, null if doesn't exist
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable tableType ${tableType}, tableCode ${tableCode}")
        TableServiceReadRequiredAttributesDTO tableReqAttributeDTO = new TableServiceReadRequiredAttributesDTO()
        tableReqAttributeDTO.returnTableCode = tableReqAttributeDTO.returnTableType = tableReqAttributeDTO.returnDescription = true

        TableServiceReadReplyDTO tableReplyDTO = null
        try {
            if(tableType?.trim() && tableCode?.trim()) {
                TableServiceReadRequestDTO tableRequestDTO = new TableServiceReadRequestDTO()
                tableRequestDTO.tableType = tableType
                tableRequestDTO.tableCode = tableCode
                tableRequestDTO.requiredAttributes = tableReqAttributeDTO
                tableReplyDTO = service.get("TABLE").read(tableRequestDTO, false)
            }
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
        
            info("Error retrieveing table with message ${serviceExc.getMessage()}")
        }
        info("end readTable")
        return tableReplyDTO
    }

    /**
     * This method returns the employee record for employeeId
     * @param employeeId
     * @return employee record if found, null if not
     */
    private MSF810Rec readEmployee(String employeeId) {
        info("readEmployee")

        MSF810Rec employee = null
        try {
            employee = edoi.findByPrimaryKey(new MSF810Key(employeeId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("Employee not found")
        }
        
        info("end readEmployee")
        return employee
    }
    
    /**
     * This method print the reportLines to csv and text report.
     * @param reportLines
     */
    private void printReport(ArrayList<ReportLineTrb76s> reportLines) {
        info("printReport")
        
        Integer totalCount = 0
        Integer subCount   = 0
        String currSuperFund = " "
        String currSuperFundDesc = " "
        Boolean firstLine = true
        for(line in reportLines) {
           outputFileWriterO.write("${line.toCSV()}\r\n")
           
           if(reportA.lineNo+3>reportA.maxLine) {
               reportA.newPage()
           }

           if(!currSuperFund.equals(line.superFundCode)) {
               if(!firstLine) {
                   reportA.write(" ")
                   reportA.write("Total Number of records in the Fund ${currSuperFundDesc.padRight(33)} are: ${subCount.toString().padLeft(7)}")
                   subCount = 0
                   reportA.newPage()
               }
               else {
                   firstLine = false
                   reportA.heading()
               }

               info("current lineNo: ${reportA.lineNo.toString()}, maxLine: ${reportA.maxLine.toString()}")
               
               currSuperFund = line.superFundCode
               currSuperFundDesc = line.superFundDesc
           }
           reportA.write(line.toReport())
           subCount++
           totalCount++

        }
        
        //The last subtotal is not yet printed, we need to print it now.
        reportA.write(" ")
        reportA.write("Total number of records in the Fund ${currSuperFundDesc.padRight(33)} are:${subCount.toString().padLeft(9)}")
        reportA.write(" ")
        reportA.write(" ")
        reportA.write("                                          Total Number of Records        :${totalCount.toString().padLeft(9)}")
        reportA.write(" ")
        
        info("end printReport")
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
ProcessTrb76s process = new ProcessTrb76s();
process.runBatch(binding);