/*
 @Ventyx 2012
 */
package com.mincom.ellipse.script.custom

import java.text.DecimalFormat;
import java.util.List

import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec;
import com.mincom.ellipse.edoi.ejb.msf785.MSF785Key
import com.mincom.ellipse.edoi.ejb.msf785.MSF785Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_CD_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_CD_801Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec
import com.mincom.ellipse.edoi.ejb.msf802.MSF802Key
import com.mincom.ellipse.edoi.ejb.msf802.MSF802Rec
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec
import com.mincom.ellipse.edoi.ejb.msf822.MSF822Key
import com.mincom.ellipse.edoi.ejb.msf822.MSF822Rec
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Key
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Rec
import com.mincom.ellipse.efs.EFSFile;
import com.mincom.ellipse.efs.EFSHelper;
import com.mincom.ellipse.efs.EllipseFileSystem;
import com.mincom.ellipse.eroi.linkage.mssprd.MSSPRDLINK
import com.mincom.ellipse.rdl.report.layout.Line;
import com.mincom.ellipse.types.m30dd.instances.String1;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException
import com.mincom.eql.Constraint
import com.mincom.eql.StringConstraint
import com.mincom.eql.impl.QueryImpl


/**
 * Class to hold parameters
 * */
public class ParamsTrbeis{
    //List of Input Parameters
    String paramWeekStartDate
    //String paramWeekEndDate
}

/**
 * Class to hold Error Message
 * */
public class ErrorsTrbeis{
    String errorFields
    String errorMessage
}

/**
 * Class to hold EmployeeDetails
 * */
public class EmployeeDetails{
    String firstName = ""
    String surName = ""
    String memberNo = ""
    BigDecimal oteVal = 0
}

/**
 * Class to hold SelectedEmployee
 * */
public class SelectedEissEmployee{
    String employeeId =""
    String memberNo = ""
    String deductionCode =""
    String accountCode= ""
    BigDecimal ordinaryTimeEarnings = 0
    BigDecimal compSGEmployer = 0
    BigDecimal awardContrib = 0
    BigDecimal optionalEmployerSacrifice = 0
    BigDecimal salarySacrificeNonContEmp = 0
    BigDecimal spouseContrib = 0
    BigDecimal employeeTopUp = 0

    public String toString(){
        return "employeeId:${employeeId}\n" +
        "memberNo:${memberNo}\n"+
        "deductionCode:${deductionCode}\n"+
        "accountCode:${accountCode}\n"+
        "ordinaryTimeEarnings:${ordinaryTimeEarnings.toString()}\n"+
        "compSGEmployer:${compSGEmployer.toString()}\n" +
        "awardContrib:${awardContrib.toString()}\n" +
        "optionalEmployerSacrifice:${optionalEmployerSacrifice.toString()}\n" +
        "salarySacrificeNonContEmp:${salarySacrificeNonContEmp.toString()}\n" +
        "spouseContrib:${spouseContrib.toString()}\n"+
        "employeeTopUp:${employeeTopUp.toString()}\n"
    }
}

/***
 * Class to hold detail for Contribution Report
 **/
public class ContributionReportDetail{
    String employeeId = ""
    String memberNo = ""
    String employeeSurName = ""
    String employeeFirstname = ""
    String deductionCode = ""
    BigDecimal ordinaryTimeEarnings = 0
    BigDecimal compSGEmployer = 0
    BigDecimal awardContrib = 0
    BigDecimal optionalEmployerSacrifice = 0
    BigDecimal salarySacrificeNonContEmp = 0
    BigDecimal spouseContrib = 0
    BigDecimal employeeTopUp = 0

    public String toString(){
        return "employeeId:${employeeId}\n" +
        "memberNo:${memberNo}\n"+
        "employeeSURNAME:${employeeSurName}\n"+
        "employeeFirstname:${employeeFirstname}\n"+
        "ordinaryTimeEarnings:${ordinaryTimeEarnings.toString()}\n"+
        "compSGEmployer:${compSGEmployer.toString()}\n" +
        "awardContrib:${awardContrib.toString()}\n" +
        "optionalEmployerSacrifice:${optionalEmployerSacrifice.toString()}\n" +
        "salarySacrificeNonContEmp:${salarySacrificeNonContEmp.toString()}\n" +
        "spouseContrib:${spouseContrib.toString()}\n"+
        "employeeTopUp:${employeeTopUp.toString()}\n"
    }
}

/**
 * Class to hold detail for Exception Report
 * */
public class ExceptionReportDetail{
    String employeeId = ""
    String reasonException = ""
    String excepDeductCode = ""

    public String toString(){
        return "employeeId:${employeeId}  reasonException:${reasonException}  excepDeductCode:${excepDeductCode}"
    }
}

/**
 * Class to hold detail for CSV file
 * */
public class ContributionFileCSV{
    String employeeId
    String memberNo
    String employeeSurName
    BigDecimal compSGEmployer
    BigDecimal awardContrib
    BigDecimal optEmployerSac
    BigDecimal salSacNcEmp
    BigDecimal spouseContrib
    BigDecimal empTopUp
}

/**
 * Main class
 * */
public class ProcessTrbeis extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 9
    private ParamsTrbeis batchParams

    //Constant
    private static final String GLOBAL_PAYGROUP = "***"
    private static final String CNTL_REC_TYPE = 'PG'
    private static final String DEFAULT_PAY_GROUP = 'TG1'
    private static final String WEEKS_START_DATE = "WeekStartDate"
    private static final String WEEKS_END_DATE ="WeekEndDate"
    private static final String A1_COMP_SGC_EMPLOYER = "A1"
    private static final String A6_COMP_SGC_EMPLOYER = "A6"
    private static final String A2_SPOUSE_CONTRIB_POST_TAX = "A2"
    private static final String A3_EMP_TOPUP_POST_TAX = "A3"
    private static final String A4_OPTL_EMPLOYER_SACRF_CNTRCT_EMP = "A4"
    private static final String A5_SLRY_SACRF_NONCONT_EMP = "A5"
    private static final String A7_AWD_ACC_SCHEME = "A7"
    private static final String UNKNOWN = "UNKNOWN"
    private static final String REPORT_TITLE = "EIS - Accumulation Scheme - Employee and Employer Contributions"
    private static final String REPORT_PRD_COVERED = "Period Covered : "
    private static final String REPORT_COMP_SG_EMPLOYER = "Comp SG Employer Super Rate : "
    private static final String REPORT_AWD_CONTRIB = "Award Contrib Super Rate : "
    private static final Integer EMPID_LNGTH = 10
    private static final Integer MEMBERNO_LNGTH = 10
    private static final Integer SURNAME_LNGTH = 20
    private static final Integer FIRSTNAME_LNGTH = 12
    private static final Integer BIGDECIMAL_LNGTH = 11
    private static final Integer ACC_CODE_LNGTH = 12
    private static final Integer EXCEPT_REASON = 75
    private static final String ENERGY_INDUSTRIES = "Energy Industries"
    private static final String SUPPLIER_CODE = "023123"
    private static final String DIVISION = "Division A Commonwealth Bank"
    private static final String POOL_A = "EISS Pool A"
    private static final String END_OF_LINES = "\r\n"
    private static final String ZERO_OTE = "ZERO Amount - Ordinary Time Earnings"
    private static final String ZERO_DED = "ZERO Amount - Deductions"
    private static final String NO_MEMBERNO = "No Member No. Found"
    private static final String EMP_TERMINATE = "Employee Terminating This Pay"
    private static final String A1_MEMBER_DIFF_A6 = "Deduction A1 Member No. Diffrent from A6 Member No"
    private static final String FINSIH_THIS_PAY = "Deduction finishing This Pay"
    private static final String EXCPT_REPORT_TITLE = "EIS - Accumulation Scheme -Exception Report"
    private static final String TRAN_IND = "1"

    //Working storage
    private String weekstrDate = "00000000"
    private String weekendDate = "00000000"
    private String sysPrdNo = ""
    private List <ErrorsTrbeis> errorTrbeis = new ArrayList <ErrorsTrbeis>()
    private Boolean errorFound = false
    private List <String> eissCode = [
        A1_COMP_SGC_EMPLOYER,
        A6_COMP_SGC_EMPLOYER,
        A2_SPOUSE_CONTRIB_POST_TAX,
        A3_EMP_TOPUP_POST_TAX,
        A4_OPTL_EMPLOYER_SACRF_CNTRCT_EMP,
        A5_SLRY_SACRF_NONCONT_EMP,
        A7_AWD_ACC_SCHEME
    ]
    private String csvFileName = ""
    private List <String> listDeductCode = new ArrayList <String> ()
    private List <String> oteCode = new ArrayList <String>()
    private List <String> listSelectedAccCode = new ArrayList <String> ()
    private List <SelectedEissEmployee> selectedRecord = new ArrayList <SelectedEissEmployee>()
    private List <ContributionReportDetail> reportContribDetail = new ArrayList <ContributionReportDetail>()
    private List <String> contentOfCsvFile = new ArrayList <String>()
    private LinkedHashMap deductionSuperReportFields =  [:]
    private LinkedHashMap deductionFactor = [:]
    private LinkedHashMap deductionAccCode = [:]
    private LinkedHashMap totalAccCode =  [:]
    private List <String> listDCodeEmp = new ArrayList <String>()
    private BigDecimal a1DeductFactor = 0
    private BigDecimal a7DeductFactor = 0
    private List <ExceptionReportDetail> exReportDetails = new ArrayList <ExceptionReportDetail>()
    private BigDecimal empOteAmount = 0
    private LinkedHashMap  empExReportRecDetails = [:]
    private String empMemberNo = ""
    private String dashLines = "-"
    private String spacesBetween = " "
    private String whiteSpaces = " "
    private Integer totalReportWidth = 132
    private Integer totalIndentFooter = 22

    public void runBatch(Binding b){

        init(b)

        printSuperBatchVersion()
        info("runBatch Version : " + version)

        batchParams = params.fill(new ParamsTrbeis())

        //PrintRequest Parameters
        info("paramWeekStartDate: " + batchParams.paramWeekStartDate)
        //info("paramWeekEndDate: " + batchParams.paramWeekEndDate)

        try {
            processBatch()
        } finally {
            printBatchReports()
        }
    }

    private void processBatch(){
        info("processBatch")
        validateRequestParams()
        validateRecord()
        if (!errorFound){
            processRequest()
        }

        //write process
    }

    /**
     * Validate Request Parameters
     * */
    private void validateRequestParams(){
        info("validateRequestParams")

        if (batchParams.paramWeekStartDate.trim().equalsIgnoreCase("")){
            determineWeekStartEndDate()
        }else{
            determineWeekEndDate(batchParams.paramWeekStartDate)
        }

        info("weekstrDate : ${weekstrDate}")
        info("weekendDate : ${weekendDate}")
        info("sysPrdNo : ${sysPrdNo}" )

        //       if (batchParams.paramWeekStartDate.trim().equalsIgnoreCase("") ){
        //           if (batchParams.paramWeekEndDate.trim().equalsIgnoreCase("") ){
        //               determineWeekStartEndDate()
        //           }else{
        //               writeErrorMessage(WEEKS_START_DATE,"Input Required")
        //           }
        //       }else{
        //           if (batchParams.paramWeekEndDate.trim().equalsIgnoreCase("") ){
        //               determineWeekEndDate(batchParams.paramWeekStartDate)
        //           }else{
        //
        //           }
        //       }


    }


    /**
     * Find and Validate the Super Deduction and Super Earning
     *
     * listDeductCode will contains all Super Deduction
     * oteCode will contains all Super Earning Code
     * */
    private void validateRecord(){
        //collect EISS Deduction code
        getEISSDeduction()

        info ()

        if (listDeductCode.isEmpty()){
            writeErrorMessage("Super Deduction", "Empty")
        }

        info("SuperDeduction Codes are:" + listDeductCode.toString())

        //collect Super Earning Code
        getOTECode()

        info ("SuperEarning Codes are:" + oteCode.toString())
    }

    /**
     * Default the weekStrDate,weekendDate and pay period from previous pay run
     * */
    private void determineWeekStartEndDate(){
        info("determineWeekStartEndDate")
        try{
            MSF801_PG_801Rec msf801_pg_801rec = edoi.findByPrimaryKey(new MSF801_PG_801Key(CNTL_REC_TYPE,DEFAULT_PAY_GROUP))
            weekstrDate = msf801_pg_801rec.getPrvStrDtPg()
            weekendDate = msf801_pg_801rec.getPrvEndDtPg()
            sysPrdNo = (msf801_pg_801rec.getSysPrdNoPg().toBigInteger() - 1).toString().padLeft(3, "0")
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            writeErrorMessage("", "MSF801 ${CNTL_REC_TYPE} ${DEFAULT_PAY_GROUP} Rec not found")
        }
    }

    /**
     * Find the weekStrDate,weekEndDate and pay period from the requested Date
     *
     * @param weekDate  any valid date
     * */
    private void determineWeekEndDate(String weekDate){

        MSSPRDLINK mssprdLnk = eroi.execute('MSSPRD',{MSSPRDLINK mssprdLink ->
            mssprdLink.payGroup = DEFAULT_PAY_GROUP
            mssprdLink.reqDate = weekDate
        })

        if (mssprdLnk.errorNoPrd.trim().equals("")){
            weekstrDate = mssprdLnk.getStartDate()
            weekendDate = mssprdLnk.getEndDate()
            sysPrdNo = mssprdLnk.getSysPrdNo()
        }else{
            writeErrorMessage(WEEKS_START_DATE,mssprdLnk.errorNoPrd + getTableDescription("ER",mssprdLnk.errorNoPrd ))
        }
    }

    /**
     * Get Table Description using TableService
     *
     * @param tableType that exist in MSF010
     * @param tableCode that exist in MSF010
     *
     * @return tableDescriptions
     *
     * */
    private String getTableDescription(String tableType, String tablleCode){

        try{
            TableServiceReadReplyDTO tableReply = service.get('Table').read({
                it.tableType = tableType
                it.tableCode = tablleCode})
            return tableReply.getDescription()
        }catch (EnterpriseServiceOperationException e){
            writeErrorMessage("getTableDescription",e.toString())
            return ""
        }
    }

    /**
     * processing request
     * */
    private void processRequest(){

        Constraint c1 = MSF820Key.employeeId.greaterThan(" ")
        def query = new QueryImpl(MSF820Rec.class).and(c1).orderBy(MSF820Rec.msf820Key)
        String msf820_empId =""
        edoi.search(query,{MSF820Rec msf820rec ->

            msf820_empId = msf820rec.getPrimaryKey().getEmployeeId()
            empOteAmount = 0
            empMemberNo = ""
            processEmployee(msf820_empId)

            MSF760Rec msf760rec = edoi.findByPrimaryKey(new MSF760Key(msf820_empId))
            debug ("TermDate: ${msf760rec.getTermDate()}")
            if (msf760rec.getTermDate().toFloat()  > weekstrDate.toFloat() && msf760rec.getTermDate().toFloat()  < weekendDate.toFloat()){
                selectException(msf820_empId,EMP_TERMINATE)
            }

        })

        if (!selectedRecord.isEmpty()){
            createReports()
        }

    }

    /**
     * Process for each employee
     *
     * @param employee id
     * */
    private void processEmployee(String employeeId) {
        info("processEmployee:" + employeeId )

        //Browse MSF835
        Constraint c1 = MSF835Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF835Key.prdEndDate.equalTo(weekendDate)
        Constraint c3 = MSF835Key.payGroup.equalTo(DEFAULT_PAY_GROUP)
        Constraint c4 = MSF835Key.payRunNo.equalTo(sysPrdNo)

        def query = new QueryImpl(MSF835Rec.class).and(c1).and(c2).and(c3).and(c4).orderBy(MSF835Rec.msf835Key)

        MSF835Rec msf835rec = edoi.firstRow(query)

        if (msf835rec!= null){
            // Only process employee that has payroll transaction for this period

            //Reset the Deduction Code for each employee
            listDCodeEmp = new ArrayList <String>()

            //Looking for Super Deduction in MSO785
            browseMSF785(employeeId)

            //Looking for Super Deduction in MSO822
            browseMSF822(employeeId)


        }


    }

    /**
     * Processing selected record
     * do not initialise 'selectedRecord' object after this process 
     * as it is need to calculate AccoutCode in Contribution Report
     * */
    private void createReports(){

        //sort by employee
        selectedRecord.sort {it.employeeId}

        String prevEmployee = ""
        List <SelectedEissEmployee> groupEmployee = new ArrayList <SelectedEissEmployee>()

        selectedRecord.each{
            debug (it.toString())


            //This will save all selected account code
            if (listSelectedAccCode.contains(it.getAccountCode())){
                totalAccCode.put(it.getAccountCode(),totalValueAccCode(it,totalAccCode.get(it.getAccountCode())))
            }else{
                listSelectedAccCode.add(it.getAccountCode())
                totalAccCode.put(it.getAccountCode(),totalValueAccCode(it,0))
            }


            //if different employee then do the processingReports
            if (!prevEmployee.equals(it.getEmployeeId())){
                prevEmployee = it.employeeId

                if (!groupEmployee.isEmpty()){
                    // This will process each Employee to populate the Contribution Report, Exception Report and CSV
                    processingReports(groupEmployee)
                }

                //once finish reset the groupEmployee Object to save next employee
                groupEmployee = new ArrayList <SelectedEissEmployee>()

            }

            groupEmployee.add(it)

        }

        // process the last employee
        if (!groupEmployee.isEmpty()){
            processingReports(groupEmployee)
        }


        if (!reportContribDetail.isEmpty()){
            constructContributionReport()
            constructCSVFile()
            copyToHashDir()
        }

        if (!exReportDetails.isEmpty()){
            exReportDetails.each{
                info(it.toString())
            }
            constructExceptionReport()
        }
    }


    /**
     * Create detail for the following report : Contribution, Exception and CSV
     * this will process for each deduction for each employee
     *
     * @param (SelectedEissEmployee) groupByEmployee
     *
     * */
    private void processingReports(List <SelectedEissEmployee> singleEmployee){

        //Sort my memberNo
        singleEmployee.sort {it.memberNo}
        ContributionReportDetail empContribDetail = new ContributionReportDetail()
        ContributionReportDetail tempEmpContribDetail = new ContributionReportDetail()
        LinkedHashMap empEachMember = [:]
        BigDecimal oteValue = 0
        String NON_MEMBERNO = "TREBIESNOMEMBERNO"

        // below code to make sure that the OTE is in the first row
        if ( singleEmployee[0].getOrdinaryTimeEarnings() == 0){


            singleEmployee.each{
                if (it.getOrdinaryTimeEarnings() != 0){
                    oteValue = it.getOrdinaryTimeEarnings()
                    it.setOrdinaryTimeEarnings(0)
                }
            }

            singleEmployee[0].setOrdinaryTimeEarnings(oteValue)
        }else{
            oteValue = singleEmployee[0].getOrdinaryTimeEarnings()
        }


        //Ready to process
        singleEmployee.each {

            info(it.toString())

            //reset the object
            empContribDetail = new ContributionReportDetail()

            if (it.getMemberNo().trim() == ""){
                tempEmpContribDetail = processContribReport(tempEmpContribDetail, it)
            }else{
                // consolidate each empContribDetail for the same
                if (empEachMember.get(it.getMemberNo().trim()) == null){
                    empContribDetail = processContribReport(empContribDetail, it)
                }else{
                    empContribDetail = processContribReport(empEachMember.get(it.getMemberNo().trim()),it)
                }

                //save each memberNo and the empContribDetail into empEachMember properties
                empEachMember.put(it.getMemberNo().trim(),empContribDetail)
            }

        }


        //Overwrite MemberNo with a valid MemberNo
        if (tempEmpContribDetail.getEmployeeId().trim()!=""){
            // This logic is to find where a line has member no
            String empMemberNo = ""

            empEachMember.each{
                if (it.key.trim() !="" && empMemberNo == "" ){
                    empMemberNo = it.key.trim()
                }
            }

            if (empMemberNo ==""){
                empMemberNo = NON_MEMBERNO
                empEachMember.put(empMemberNo,tempEmpContribDetail)
            }else{
                tempEmpContribDetail = processContribReport(empEachMember.get(empMemberNo),tempEmpContribDetail)
                empEachMember.put(empMemberNo,tempEmpContribDetail)
            }
        }

        //Sort by memberNo
        empEachMember.sort{ a, b -> a.key <=> b.key }

        //now save the final result into the main variable
        empEachMember.each{
            ContributionReportDetail cRptEmpDetail = (ContributionReportDetail) it.value
            if (cRptEmpDetail.getOrdinaryTimeEarnings() == 0){
                cRptEmpDetail.setOrdinaryTimeEarnings(oteValue)
            }

            reportContribDetail.add(cRptEmpDetail)

            if (it.key.trim().equals(NON_MEMBERNO)){
                selectException(cRptEmpDetail.getEmployeeId(),NO_MEMBERNO)
            }
            exceptEmpDet(cRptEmpDetail)
        }
    }

    /**
     * To sum up all the Super Deduction
     * 
     * @ param SelectedEissEmployee object
     * @ return total value
     * 
     * With assumption it has the correct data please see checkDecimal
     * 
     * e.q
     * params:
     * Emp      DedCod      OTE     CompSGEmp       AwardContrib        OptionalEmp     SalarySacRifice     SpouseContrib       EmployeeTopUp
     * 123      103         100     100             0                   0               0                   0                   0
     * 123      104         0       0               100                 0               0                   0                   0
     * 123      105         0       0               0                   100             0                   0                   0
     * 123      106         0       0               0                   0               100                 0                   0
     * 123      107         0       0               0                   0               0                   100                 0
     * 
     * return 500 (does not include OTE)
     * */
    private BigDecimal totalValueAccCode(SelectedEissEmployee selectedEmp, BigDecimal returnValue){

        if (selectedEmp.getCompSGEmployer() != 0){
            returnValue = returnValue + selectedEmp.getCompSGEmployer()
        }

        if (selectedEmp.getAwardContrib() != 0){
            returnValue = returnValue + selectedEmp.getAwardContrib()
        }

        if (selectedEmp.getOptionalEmployerSacrifice()!= 0){
            returnValue = returnValue + selectedEmp.getOptionalEmployerSacrifice()
        }

        if (selectedEmp.getSalarySacrificeNonContEmp() != 0){
            returnValue = returnValue + selectedEmp.getSalarySacrificeNonContEmp()
        }

        if (selectedEmp.getSpouseContrib()!= 0){
            returnValue = returnValue + selectedEmp.getSpouseContrib()
        }

        if (selectedEmp.getEmployeeTopUp() != 0){
            returnValue = returnValue + selectedEmp.getEmployeeTopUp()
        }

        return returnValue

    }
    /**
     * Populate and add the value of ContributionReportDetail object
     * this will be display in the detail of Contribution Report
     *
     * @param the original value of ContributionReportDetail
     * @param the new value which will be added to the original value
     *
     * @return the total value of ContributionReportDetail
     * */
    private ContributionReportDetail processContribReport(ContributionReportDetail empContribDet, SelectedEissEmployee it) {

        if (!empContribDet.getEmployeeId().equals(it.getEmployeeId())){
            empContribDet.setEmployeeId(it.getEmployeeId())
            empContribDet.setEmployeeFirstname(getEmpDet(it.getEmployeeId()).getFirstName())
            empContribDet.setEmployeeSurName(getEmpDet(it.getEmployeeId()).getSurName())
        }
        empContribDet.setDeductionCode(it.getDeductionCode())
        empContribDet.setMemberNo(it.getMemberNo())
        empContribDet.setOrdinaryTimeEarnings(checkDecimal(it.getOrdinaryTimeEarnings(),empContribDet.getOrdinaryTimeEarnings()))
        empContribDet.setCompSGEmployer(checkDecimal(it.getCompSGEmployer(),empContribDet.getCompSGEmployer()))
        empContribDet.setAwardContrib(checkDecimal(it.getAwardContrib(),empContribDet.getAwardContrib()))
        empContribDet.setOptionalEmployerSacrifice(checkDecimal(it.getOptionalEmployerSacrifice(),empContribDet.getOptionalEmployerSacrifice()))
        empContribDet.setSalarySacrificeNonContEmp(checkDecimal(it.getSalarySacrificeNonContEmp(),empContribDet.getSalarySacrificeNonContEmp()))
        empContribDet.setSpouseContrib(checkDecimal(it.getSpouseContrib(),empContribDet.getSpouseContrib()))
        empContribDet.setEmployeeTopUp(checkDecimal(it.getEmployeeTopUp(),empContribDet.getEmployeeTopUp()))

        return empContribDet
    }

    /**
     * Same as above but different parameters.
     * this logic is to facilitate if the it.getMemberNo = ""
     * therefore do not override the empContribDet.getMemberNo
     * see "Overwrite MemberNo with a valid MemberNo"
     * */
    private ContributionReportDetail processContribReport(ContributionReportDetail empContribDet, ContributionReportDetail it) {

        if (!empContribDet.getEmployeeId().equals(it.getEmployeeId())){
            empContribDet.setEmployeeId(it.getEmployeeId())
            empContribDet.setEmployeeFirstname(getEmpDet(it.getEmployeeId()).getFirstName())
            empContribDet.setEmployeeSurName(getEmpDet(it.getEmployeeId()).getSurName())
        }
        empContribDet.setDeductionCode(it.getDeductionCode())
        empContribDet.setOrdinaryTimeEarnings(checkDecimal(it.getOrdinaryTimeEarnings(),empContribDet.getOrdinaryTimeEarnings()))
        empContribDet.setCompSGEmployer(checkDecimal(it.getCompSGEmployer(),empContribDet.getCompSGEmployer()))
        empContribDet.setAwardContrib(checkDecimal(it.getAwardContrib(),empContribDet.getAwardContrib()))
        empContribDet.setOptionalEmployerSacrifice(checkDecimal(it.getOptionalEmployerSacrifice(),empContribDet.getOptionalEmployerSacrifice()))
        empContribDet.setSalarySacrificeNonContEmp(checkDecimal(it.getSalarySacrificeNonContEmp(),empContribDet.getSalarySacrificeNonContEmp()))
        empContribDet.setSpouseContrib(checkDecimal(it.getSpouseContrib(),empContribDet.getSpouseContrib()))
        empContribDet.setEmployeeTopUp(checkDecimal(it.getEmployeeTopUp(),empContribDet.getEmployeeTopUp()))

        return empContribDet
    }

    /**
     * Sum a value
     * only add if one of them is zero
     * because we are expecting record to be like the following
     * Example the expected record
     * Emp      DedCod      OTE     CompSGEmp       AwardContrib        OptionalEmp     SalarySacRifice     SpouseContrib       EmployeeTopUp
     * 123      103         100     100             0                   0               0                   0                   0
     * 123      104         0       0               100                 0               0                   0                   0
     * 123      105         0       0               0                   100             0                   0                   0
     * 123      106         0       0               0                   0               100                 0                   0
     * 123      107         0       0               0                   0               0                   100                 0
     * 123      108         0       0               0                   0               0                   0                   100
     *
     * Result
     * Emp      MemberNo    OTE     CompSGEmp       AwardContrib        OptionalEmp     SalarySacRifice     SpouseContrib       EmployeeTopUp
     * 123      ABC         100     100             100                 100             100                 100                 100
     * 
     * Exceptions:
     * There is a possibility that the record to be like this
     * 
     * Emp      MemberNo    OTE     CompSGEmp       AwardContrib        OptionalEmp     SalarySacRifice     SpouseContrib       EmployeeTopUp
     * 123      ABC         100     100             100                 100             100                 0                   100      
     * 123      DEF         100     100             0                   0               0                   100                 0
     * 
     * This could be happened when there is a same member no in different deduction code
     * @param bigDecimal an input value
     * @param bigDecimal an original value
     *
     * @return the total = input + original
     * */
    private BigDecimal checkDecimal(BigDecimal bigDecimal, BigDecimal totalBigDecimal){

        if (bigDecimal > 0 && totalBigDecimal > 0 && !bigDecimal.equals(totalBigDecimal)){

            info ("**** ERROR **** Potentially Number will be doubled   bigDecimal : ${bigDecimal}  total: ${totalBigDecimal}")
            assert false
        }

        // if one of them is zero  or  bigDecimal=totalBigDecimal
        if (bigDecimal.equals(totalBigDecimal)){
            return totalBigDecimal
        }else{
            return totalBigDecimal + bigDecimal
        }

    }

    /**
     * Fetch employee details
     * 
     * @param employee id
     * @return Employee Details (e.g First name, Last Name)
     * */
    private EmployeeDetails getEmpDet(String employeeId){
        Constraint c1 = MSF810Key.employeeId.equalTo(employeeId)

        def query = new QueryImpl(MSF810Rec.class).and(c1)

        MSF810Rec msf810rec = edoi.firstRow(query)

        EmployeeDetails empDet = new EmployeeDetails()

        empDet.setFirstName(msf810rec.getFirstName())
        empDet.setSurName(msf810rec.getSurname())

        return empDet
    }

    private void exceptEmpDet (ContributionReportDetail empContribDetail){
        EmployeeDetails exceptEmpDet = new EmployeeDetails()
        exceptEmpDet.setFirstName(empContribDetail.getEmployeeFirstname())
        exceptEmpDet.setSurName(empContribDetail.getEmployeeSurName())
        exceptEmpDet.setOteVal(empContribDetail.getOrdinaryTimeEarnings())
        exceptEmpDet.setMemberNo(empContribDetail.getMemberNo())
        empExReportRecDetails.put(empContribDetail.getEmployeeId(), exceptEmpDet)
    }
    /**
     * Find and Validate all Super Deduction for each Employee from MSO822/MSF822
     *
     * @param employee id
     * */
    private void browseMSF822(String employeeId){


        String conYTDTots = ""
        String YES_STRING = "Y"
        boolean bcalcuateOTE = false
        if (commarea.ConsYtdTots.equals(YES_STRING)){
            conYTDTots = "   "
        }else{
            conYTDTots = GLOBAL_PAYGROUP
        }
        Constraint c1 = MSF822Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF822Key.consPayGrp.equalTo(conYTDTots)

        def query = new QueryImpl(MSF822Rec.class).and(c1).and(c2).orderBy(MSF822Rec.msf822Key)

        edoi.search(query,{MSF822Rec msf822rec ->

            if ((msf822rec.getEndDate().equals("00000000") || msf822rec.getEndDate().toFloat() >= weekstrDate.toFloat())
            && listDeductCode.contains(msf822rec.getPrimaryKey().getDednCode()) && !listDCodeEmp.contains(msf822rec.getPrimaryKey().getDednCode())
            ){

                // do not select the employee if the deduction is exist in listDCodeEmp where this deduction code has been processed in MSO785.

                if ( msf822rec.getEndDate().toFloat() >= weekstrDate.toFloat() && msf822rec.getEndDate().toFloat() <= weekendDate.toFloat() ){
                    selectException(employeeId,FINSIH_THIS_PAY,msf822rec.getPrimaryKey().getDednCode())
                }

                info ("processing deductCode: ${msf822rec.getPrimaryKey().getDednCode()} StartDate: ${msf822rec.getStartDate()}  EndDate: ${msf822rec.getEndDate()}")
                SelectedEissEmployee selectedEissEmployee = processDeduction(msf822rec.getPrimaryKey().getDednCode(), msf822rec.getDednRef(), employeeId,bcalcuateOTE)

                //selectedRecord list will contains all records (raw data) for Contribution Report and CSV
                selectedRecord.add(selectedEissEmployee)

                bcalcuateOTE = true
            }

        })

    }

    /**
     * Find and Validate all Super Deduction for each Employee from MSO875/MSF875
     *
     * @param employee id
     * */
    private void browseMSF785(String employeeId){
        info ("browseMSF785")
        boolean bcalcuateOTE = false

        Constraint c1 = MSF785Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF785Key.invEffDate.greaterThanEqualTo((99999999 - weekendDate.toInteger()).toString())

        def query = new QueryImpl(MSF785Rec.class).and(c1).and(c2).orderBy(MSF785Rec.msf785Key)

        MSF785Rec msf785rec = edoi.firstRow(query)

        if (msf785rec == null){
            info ("Could not found MSF785 ")
            return
        }

        if (msf785rec.getBenefitType_1().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_1(),msf785rec.getBenefitRef_1(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_2().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_2(),msf785rec.getBenefitRef_2(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_3().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_3(),msf785rec.getBenefitRef_3(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_4().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_4(),msf785rec.getBenefitRef_4(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_5().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_5(),msf785rec.getBenefitRef_5(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_6().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_6(),msf785rec.getBenefitRef_6(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_7().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_7(),msf785rec.getBenefitRef_7(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_8().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_8(),msf785rec.getBenefitRef_8(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_9().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_9(),msf785rec.getBenefitRef_9(),employeeId,bcalcuateOTE)
        }

        if (msf785rec.getBenefitType_10().trim()!=""){
            bcalcuateOTE = processBenefitType(msf785rec.getBenefitType_10(),msf785rec.getBenefitRef_10(),employeeId,bcalcuateOTE)
        }
    }


    /**
     *
     * */
    private boolean processBenefitType(String benefitType,String memberNo,String employeeId, boolean bcalcuateOTE){
        debug ("processBenefitType")
        String benefitDeductCode = benefitType.substring(1)

        if (listDeductCode.contains(benefitDeductCode)){
            SelectedEissEmployee selectedEissEmployee = processDeduction(benefitDeductCode, memberNo, employeeId,bcalcuateOTE)
            selectedRecord.add(selectedEissEmployee)
            listDCodeEmp.add(benefitDeductCode)
            bcalcuateOTE = true
        }

        return bcalcuateOTE
    }

    /**
     * Construct SelectedEissEmployee object
     *
     * @param deduction code
     * @param member no
     * @param employee id
     * @param boolean for calculating OTE or not, if it is true the it will calculate OTE
     *
     * @return SelectedEissEmployee object where it will contains list of deduction code along with the amount for all employees
     * */
    private SelectedEissEmployee processDeduction(String deductionCode, String memberNo, String employeeId,boolean bcalcuateOTE) {
        info ("SelectedEissEmployee")

        SelectedEissEmployee selectedEissEmployee = new SelectedEissEmployee()

        selectedEissEmployee.setEmployeeId(employeeId)
        selectedEissEmployee.setMemberNo(memberNo)
        selectedEissEmployee.setDeductionCode(deductionCode)
        selectedEissEmployee.setAccountCode(deductionAccCode.get(deductionCode))

        if (!bcalcuateOTE){
            empOteAmount = sumOTE(employeeId)
            selectedEissEmployee.setOrdinaryTimeEarnings(empOteAmount)

            if (empOteAmount == 0){
                selectException(employeeId,ZERO_OTE)
            }

            bcalcuateOTE = true
        }

        BigDecimal deductionAmount = browseMSF835(employeeId,deductionCode)



        if (deductionAmount == 0){
            //at this stage we know the deduction code must be a super deduction
            selectException(employeeId,deductionCode + " - " + ZERO_DED)
        }

        if (deductionSuperReportFields.get(deductionCode).equals(A1_COMP_SGC_EMPLOYER)
        || deductionSuperReportFields.get(deductionCode).equals(A6_COMP_SGC_EMPLOYER)){
            debug ("SuperReport fields ${deductionSuperReportFields.get(deductionCode)}")

            // Save the Super Rate factor
            if (deductionSuperReportFields.get(deductionCode).equals(A1_COMP_SGC_EMPLOYER)){
                debug ("A1 - ${deductionCode}")
                if(!a1DeductFactor.equals(deductionFactor.get(deductionCode))){
                    if (a1DeductFactor ==0){
                        a1DeductFactor = deductionFactor.get(deductionCode)
                    }else{
                        // This should not happened
                        // if it does contact HR Consultant what we should do about it
                        info ("Found diffrent rate for the deduction super report A1 ")
                        assert false
                    }
                }

                empMemberNo = checkMemberNo(empMemberNo,memberNo,employeeId,deductionCode)
            }else{

                empMemberNo = checkMemberNo(empMemberNo,memberNo,employeeId,deductionCode)

            }




            selectedEissEmployee.setCompSGEmployer(deductionAmount)
        }

        if(deductionSuperReportFields.get(deductionCode).equals(A7_AWD_ACC_SCHEME)){

            // Save the Super Rate factor
            debug ("A7 - ${deductionCode}")
            if(!a7DeductFactor.equals(deductionFactor.get(deductionCode))){
                if (a7DeductFactor ==0){
                    a7DeductFactor = deductionFactor.get(deductionCode)
                }else{
                    // This should not happened
                    // if it does contact HR Consultant what we should do about it
                    info ("Found diffrent rate for the deduction super report A7 ")
                    assert false
                }
            }
            selectedEissEmployee.setAwardContrib(deductionAmount)
        }

        if(deductionSuperReportFields.get(deductionCode).equals(A4_OPTL_EMPLOYER_SACRF_CNTRCT_EMP)){
            selectedEissEmployee.setOptionalEmployerSacrifice(deductionAmount)
        }

        if(deductionSuperReportFields.get(deductionCode).equals(A5_SLRY_SACRF_NONCONT_EMP)){
            selectedEissEmployee.setSalarySacrificeNonContEmp(deductionAmount)
        }

        if(deductionSuperReportFields.get(deductionCode).equals(A2_SPOUSE_CONTRIB_POST_TAX)){
            selectedEissEmployee.setSpouseContrib(deductionAmount)
        }

        if(deductionSuperReportFields.get(deductionCode).equals(A3_EMP_TOPUP_POST_TAX)){
            selectedEissEmployee.setEmployeeTopUp(deductionAmount)
        }

        info (selectedEissEmployee.toString())
        return selectedEissEmployee
    }

    /**
     * Create Exceptions Report Details
     * 
     * @param employeeId
     * @param excptReason
     * 
     * */
    private void selectException (String employeeId, String excptReason){
        ExceptionReportDetail excptDetails = new ExceptionReportDetail()
        excptDetails.setEmployeeId(employeeId)
        excptDetails.setReasonException(excptReason)
        exReportDetails.add(excptDetails)
        exReportDetails.sort{it.employeeId}
    }

    /***
     * Create Exceptions Report Details
     *
     * @param Deduction Code
     * @param MemberNo
     * @param EmployeeId
     *
     * */
    private void selectException (String employeeId, String excptReason, String deductCode){
        ExceptionReportDetail excptDetails = new ExceptionReportDetail()
        excptDetails.setEmployeeId(employeeId)
        excptDetails.setReasonException(excptReason)
        excptDetails.setExcepDeductCode(deductCode)
        exReportDetails.add(excptDetails)
    }

    /**
     * Sum up the total amount for super earning from payroll history transaction.
     * the super earning are located in oteCode (List)
     *
     * @param employee id
     * @return total amount
     * */
    private BigDecimal sumOTE(String employeeId){
        info("sumOTE")


        BigDecimal oteAmount = 0

        Constraint c1 = MSF835Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF835Key.prdEndDate.equalTo(weekendDate)
        Constraint c3 = MSF835Key.payGroup.equalTo(DEFAULT_PAY_GROUP)
        Constraint c4 = MSF835Key.payRunNo.equalTo(sysPrdNo)
        Constraint c5 = MSF835Key.tranInd.equalTo(TRAN_IND)
        def query = new QueryImpl(MSF835Rec.class).and(c1).and(c2).and(c3).and(c4).and(c5).orderBy(MSF835Rec.msf835Key)

        edoi.search(query,{MSF835Rec msf835rec ->

            if (oteCode.contains(msf835rec.getHdaEarnCode())){
                debug("Found HDA Earning Code: ${msf835rec.getHdaEarnCode()} Trandate: ${msf835rec.getPrimaryKey().getTrnDate()}")
                oteAmount = oteAmount + msf835rec.getHdAmount()
            }else{
                if (oteCode.contains(msf835rec.getPrimaryKey().getTranCode())){
                    debug("Found Earning Code: ${msf835rec.getPrimaryKey().getTranCode()} Trandate: ${msf835rec.getPrimaryKey().getTrnDate()}")

                    if (msf835rec.getHdAmount() != 0){
                        oteAmount = oteAmount + msf835rec.getHdAmount()
                    }else{
                        oteAmount = oteAmount + msf835rec.getAmount()
                    }
                }
            }

        })

        return oteAmount

    }

    /**
     * Sum up the total amount for a tran code (deduction code) from payroll history transaction.
     * the super earning are located in oteCode (List)
     *
     * @param employee id
     * @param tran code (deduction code)
     * @return total amount
     * */
    private BigDecimal browseMSF835(String employeeId, String tranCode){
        info("browseMSF835 tranCode: ${tranCode}" )
        BigDecimal amount = 0

        Constraint c1 = MSF835Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF835Key.prdEndDate.equalTo(weekendDate)
        Constraint c3 = MSF835Key.payGroup.equalTo(DEFAULT_PAY_GROUP)
        Constraint c4 = MSF835Key.payRunNo.equalTo(sysPrdNo)
        Constraint c5 = MSF835Key.tranCode.equalTo(tranCode)
        Constraint c6 = MSF835Key.tranInd.notEqualTo(TRAN_IND)

        def query = new QueryImpl(MSF835Rec.class).and(c1).and(c2).and(c3).and(c4).and(c5).and(c6).orderBy(MSF835Rec.msf835Key)

        edoi.search(query,{MSF835Rec msf835rec ->

            amount = amount + msf835rec.getAmount()

        })

        return amount
    }


    /**
     * Searching any deduction code where the MiscReportFields 1~5 contains A1~A7
     * */
    private void getEISSDeduction(){
        info("getEISSDeduction")
        Constraint c1 = MSF801_D_801Key.cntlRecType.equalTo("D")
        def query = new QueryImpl(MSF801_D_801Rec.class).and(c1)

        edoi.search(query,{MSF801_D_801Rec msf801_d_801rec ->

            boolean foundDeduction = false
            if (msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(0,3).equals(GLOBAL_PAYGROUP)){

                /**
                 * If the program found that one deduction has more than one super reporting code
                 * the program must report an an error and stop
                 * */
                if(eissCode.contains(msf801_d_801rec.getMiscRptFldDx1())){
                    foundDeduction = setupEissDeductCode(msf801_d_801rec,msf801_d_801rec.getMiscRptFldDx1(),foundDeduction)
                }

                if(eissCode.contains(msf801_d_801rec.getMiscRptFldDx2())){
                    foundDeduction = setupEissDeductCode(msf801_d_801rec,msf801_d_801rec.getMiscRptFldDx2(),foundDeduction)
                }

                if(eissCode.contains(msf801_d_801rec.getMiscRptFldDx3())){
                    foundDeduction = setupEissDeductCode(msf801_d_801rec,msf801_d_801rec.getMiscRptFldDx3(),foundDeduction)
                }

                if(eissCode.contains(msf801_d_801rec.getMiscRptFldDx4())){
                    foundDeduction = setupEissDeductCode(msf801_d_801rec,msf801_d_801rec.getMiscRptFldDx4(),foundDeduction)
                }

                if(eissCode.contains(msf801_d_801rec.getMiscRptFldDx5())){
                    foundDeduction = setupEissDeductCode(msf801_d_801rec,msf801_d_801rec.getMiscRptFldDx5(),foundDeduction)
                }

            }


        })

    }

    /**
     * populate listDeductCode (List), deductionFactor(Properties) and deductionSuperReportFields (Properties)
     * it is expected that one deduction code could only has one super code in the MiscReportingFields1~5
     * 
     * @param MSF801_D_Rec
     * @param SuperReportFields A1~A7
     * @param boolean to validate the request parameters - only one deduction for one super report
     *
     * @return boolean true to indicate the deduction code has been saved.
     * */
    private boolean setupEissDeductCode(MSF801_D_801Rec msf801_d_801rec, String superReportFields ,boolean foundDeductionCode) {
        info ("setupEissDeductCode")
        if (foundDeductionCode){
            writeErrorMessage(msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6), "has more than one super reporting codes")
            return true
        }

        listDeductCode.add(msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6))
        deductionFactor.put(msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6), msf801_d_801rec.getDedFactorD())
        deductionSuperReportFields.put(msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6), superReportFields)
        readMSF801CD (msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6))
        return true
    }


    /**
     * populate deduction Account Code to deductionAccCode (properties)
     *
     * @param deduction code
     * */
    private void readMSF801CD(String deductionCode){
        info ("readMSF801CD")
        String accountCode = ""

        MSF801_CD_801Rec msf801_cd_801rec = edoi.findByPrimaryKey(new MSF801_CD_801Key("CD", GLOBAL_PAYGROUP+deductionCode))

        if (msf801_cd_801rec != null){
            if (msf801_cd_801rec.getGlCreditNoCd().trim()!=""){
                accountCode = msf801_cd_801rec.getGlCreditNoCd().trim()
            } else {
                accountCode = UNKNOWN
            }

        }
        deductionAccCode.put(deductionCode, accountCode)

    }

    /**
     * Searching for Super Earning Code where it has 'Y' in CurrAccumAx2.
     * */
    private void getOTECode(){
        info("getOTECode")

        Integer oteCount = 0
        Constraint c1 = MSF801_A_801Key.cntlRecType.equalTo("A")
        def query = new QueryImpl(MSF801_A_801Rec.class).and(c1)

        edoi.search(query,{MSF801_A_801Rec msf801_A_801rec ->

            if (msf801_A_801rec.getPrimaryKey().getCntlKeyRest().substring(0,3).equals(GLOBAL_PAYGROUP)){

                oteCount++
                if (msf801_A_801rec.getCurrAccumAx2().equals("Y")){
                    oteCode.add(msf801_A_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6))
                }

            }

        })

        if (oteCount == 0){
            writeErrorMessage("", "No Super Earning Found")
        }

    }

    private void writeErrorMessage(String sFields, String errorMessage){
        info (sFields + " " + errorMessage)
        ErrorsTrbeis errorsObject = new ErrorsTrbeis()
        errorsObject.setErrorFields(sFields)
        errorsObject.setErrorMessage(errorMessage)
        errorTrbeis.add(errorsObject)
        errorFound = true
    }


    private void printBatchReports(){

        info("printAnyReports")
        //print batch report

        if (errorFound){
            printErrorReport()
        }

    }

    private void printErrorReport(){
        def Trbeisd = report.open("TRBEISD")
        errorTrbeis.each{
            Trbeisd.write(it.getErrorFields() + "-" + it.getErrorMessage())
        }
        Trbeisd.close()
    }

    /**
     * Construct Contribution Report
     * */
    private void constructContributionReport(){
        BigDecimal totalEmployee = 0
        BigDecimal totalOTE = 0
        BigDecimal totalCompSGEmployer = 0
        BigDecimal totalAwardContrib = 0
        BigDecimal totalOptionalEmployerSacrifice = 0
        BigDecimal totalSalarySacrificeNonContEmp = 0
        BigDecimal totalSpouseContrib = 0
        BigDecimal totalEmployeeTopUp = 0
        BigDecimal grandtotalAccCode = 0

        //Header Column
        List <String> trbeisaHeading = new ArrayList <String>()
        trbeisaHeading.add(REPORT_PRD_COVERED + new Date().parse("yyyyMMdd", weekstrDate).format("dd/MM/yy") + "-" + new Date().parse("yyyyMMdd", weekendDate).format("dd/MM/yy")+
                whiteSpaces.padLeft(10," ") +
                REPORT_COMP_SG_EMPLOYER + new DecimalFormat("###.##").format(a1DeductFactor)+"%" +
                whiteSpaces.padLeft(10," ") +
                REPORT_AWD_CONTRIB + new DecimalFormat("###.##").format(a7DeductFactor)+"%")
        trbeisaHeading.add("                                                                                                                                    ")
        trbeisaHeading.add("                                                          Ordinary                         Optional     Salary                      ")
        trbeisaHeading.add("                                                              Time    Comp SG      Award   Employer  Sacrifice     Spouse   Employee")
        trbeisaHeading.add("Emp-Id     Member No. +---- Surname ----+ +First Name+    Earnings   Employer    Contrib  Sacrifice NonContEmp    Contrib     Top-Up")

        BatchTextReports Trbeisa = report.open("TRBEISA",trbeisaHeading)
        Trbeisa.prntHeadAtSetColumn = false
        Trbeisa.columns  = [
            "Emp-Id     ",
            "Member No. ",
            "+--- Surname ---+ ",
            "+First Name+ ",
            "Earnings   ",
            "Employer   ",
            "Contrib    ",
            "Sacrifice  ",
            "NonCont Emp",
            "Contrib    ",
            "Top-Up     "
        ]

        //Detail
        reportContribDetail.each{
            totalEmployee++
            totalOTE = totalOTE + it.getOrdinaryTimeEarnings()
            totalCompSGEmployer = totalCompSGEmployer + it.getCompSGEmployer()
            totalAwardContrib = totalAwardContrib + it.getAwardContrib()
            totalOptionalEmployerSacrifice = totalOptionalEmployerSacrifice + it.getOptionalEmployerSacrifice()
            totalSalarySacrificeNonContEmp = totalSalarySacrificeNonContEmp + it.getSalarySacrificeNonContEmp()
            totalSpouseContrib = totalSpouseContrib + it.getSpouseContrib()
            totalEmployeeTopUp = totalEmployeeTopUp + it.getEmployeeTopUp()

            Trbeisa.write(it.getEmployeeId(),
                    it.getMemberNo(),
                    fixedLengthString(it.getEmployeeSurName(),SURNAME_LNGTH),
                    fixedLengthString(it.getEmployeeFirstname(),FIRSTNAME_LNGTH),
                    bigDecimalFormatForReports(it.getOrdinaryTimeEarnings()),
                    bigDecimalFormatForReports(it.getCompSGEmployer()),
                    bigDecimalFormatForReports(it.getAwardContrib()),
                    bigDecimalFormatForReports(it.getOptionalEmployerSacrifice()),
                    bigDecimalFormatForReports(it.getSalarySacrificeNonContEmp()),
                    bigDecimalFormatForReports(it.getSpouseContrib()),
                    bigDecimalFormatForReports(it.getEmployeeTopUp()))
        }
        Trbeisa.write(dashLines.padLeft(totalReportWidth,"-"))


        //Summary Report
        Trbeisa.write(whiteSpaces.padRight(22," ")+ 
                "Grand Total".padRight(20," ")+ 
                bigDecimalFormatForReports(totalEmployee).trim().padRight(12," ")+ 
                 bigDecimalFormatForReports(totalOTE).padLeft(12," ") +
                bigDecimalFormatForReports(totalCompSGEmployer)+
                bigDecimalFormatForReports(totalAwardContrib)+
                bigDecimalFormatForReports(totalOptionalEmployerSacrifice)+
                bigDecimalFormatForReports(totalSalarySacrificeNonContEmp)+
                bigDecimalFormatForReports(totalSpouseContrib)+
                bigDecimalFormatForReports(totalEmployeeTopUp))

        Trbeisa.write(whiteSpaces.padLeft(totalReportWidth," "))
        Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter," ") +
                "Supplier Name : ${ENERGY_INDUSTRIES}" + whiteSpaces.padLeft(51," ") + "Supplier Code : ${SUPPLIER_CODE}")
        Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter," ") +
                DIVISION )
        Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter," ") +
                POOL_A )

        Trbeisa.write(whiteSpaces.padLeft(totalReportWidth," ") )
        listSelectedAccCode.sort()


        listSelectedAccCode.each{
            grandtotalAccCode = grandtotalAccCode + totalAccCode.get(it)
            Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter," ") +
                    it.padRight(ACC_CODE_LNGTH,whiteSpaces) + " : " + spacesBetween + "\$" +
                    bigDecimalFormatForReports(totalAccCode.get(it)).padLeft(BIGDECIMAL_LNGTH+3,whiteSpaces)
                    )
        }

        Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter+ACC_CODE_LNGTH+3," ")+dashLines.padLeft(BIGDECIMAL_LNGTH+7,dashLines))
        Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter," ")+"Total".padRight(ACC_CODE_LNGTH,whiteSpaces) + " : " + spacesBetween + "\$" +
                bigDecimalFormatForReports(grandtotalAccCode).padLeft(BIGDECIMAL_LNGTH+3,whiteSpaces))
        Trbeisa.write(whiteSpaces.padLeft(totalIndentFooter+ACC_CODE_LNGTH+3," ")+dashLines.padLeft(BIGDECIMAL_LNGTH+7,dashLines))
        Trbeisa.close()
    }

    private String bigDecimalFormatForReports(BigDecimal decValue){
        DecimalFormat decFormat = new DecimalFormat("###,###.##")
        return decFormat.format(decValue).padLeft(BIGDECIMAL_LNGTH,whiteSpaces)
    }

    private String fixedLengthString (String inputString,Integer fixedLen){

        if (inputString.length()>fixedLen){
            return inputString.substring(0, fixedLen)
        }else{
            return inputString.padRight(fixedLen," ")
        }
    }

    private String checkMemberNo (String prevMemberNo, String newMemberNo, String empId, String dCode){

        info ("prevMemberNo: ${prevMemberNo} newMemberNo:${newMemberNo} empId:${empId} dCode:${dCode} ")
        if (prevMemberNo != "" && !prevMemberNo.equals(UNKNOWN)&& !prevMemberNo.equals(newMemberNo)){
            selectException(empId,A1_MEMBER_DIFF_A6,dCode)
        }

        if (prevMemberNo =="" || prevMemberNo.equals(UNKNOWN)){
            prevMemberNo = newMemberNo
        }

        return prevMemberNo
    }

    /**
     * Construct Exception Report
     * */
    private void constructExceptionReport(){
        List <String> trbeisbHeading = new ArrayList <String>()
        trbeisbHeading.add(REPORT_PRD_COVERED + new Date().parse("yyyyMMdd", weekstrDate).format("dd/MM/yy") + "-" + new Date().parse("yyyyMMdd", weekendDate).format("dd/MM/yy")+
                whiteSpaces.padLeft(10," ") +
                REPORT_COMP_SG_EMPLOYER + new DecimalFormat("###.##").format(a1DeductFactor)+"%" +
                whiteSpaces.padLeft(10," ") +
                REPORT_AWD_CONTRIB + new DecimalFormat("###.##").format(a7DeductFactor)+"%" )
        trbeisbHeading.add(whiteSpaces.padLeft(totalReportWidth," ") )
        trbeisbHeading.add("                                                                  Ordinary                                                          ")
        trbeisbHeading.add("                                                                      Time                                                          ")
        trbeisbHeading.add("Emp-Id       Member No.   +---- Surname ----+ +First Name+        Earnings   +-----------------------Reason------------------------+")


        BatchTextReports Trbeisb = report.open("TRBEISB",trbeisbHeading)
        Trbeisb.prntHeadAtSetColumn = false
        Trbeisb.columns = [
            "Emp-Id       ",
            "Member No.   ",
            "+---- Surname ----+ ",
            "+First Name+      ",
            "Earnings     ",
            "+-----------------------Reason------------------------+"
        ]
        //Header
        //Detail
        exReportDetails.each{
            info ("getting exception for empid:" + it.getEmployeeId())
            EmployeeDetails empDetails = empExReportRecDetails.get(it.getEmployeeId())

            if (empDetails !=null){
                Trbeisb.write(it.getEmployeeId(),
                        empDetails.getMemberNo(),
                        fixedLengthString(empDetails.getSurName(),SURNAME_LNGTH),
                        fixedLengthString(empDetails.getFirstName(),FIRSTNAME_LNGTH),
                        bigDecimalFormatForReports(empDetails.getOteVal()),
                        it.getReasonException().padRight(EXCEPT_REASON, whiteSpaces) )
            }
        }
        Trbeisb.close()
    }

    private void constructCSVFile(){

        BigDecimal totalCompSGEmployer = 0
        BigDecimal totalAwardContrib = 0
        BigDecimal totalOptionalEmployerSacrifice = 0
        BigDecimal totalSalarySacrificeNonContEmp = 0
        BigDecimal totalSpouseContrib = 0
        BigDecimal totalEmployeeTopUp = 0
        String lineString =""

        csvFileName = env.getWorkDir().toString()+"/TRTEIS." + getUUID()+".csv"
        File trtEisCsv = new File (csvFileName)
        trtEisCsv.append("Emp-Id,Member-Id,Last-Name,Comp-SG-Employer,Award-Contrib,Opt-Employer-Sac,Sal-Sac-Nc-Emp,Spouse-Contrib,Emp-Top-Up" + END_OF_LINES

                )

        reportContribDetail.each{
            totalCompSGEmployer = totalCompSGEmployer + it.getCompSGEmployer()
            totalAwardContrib = totalAwardContrib + it.getAwardContrib()
            totalOptionalEmployerSacrifice = totalOptionalEmployerSacrifice + it.getOptionalEmployerSacrifice()
            totalSalarySacrificeNonContEmp = totalSalarySacrificeNonContEmp + it.getSalarySacrificeNonContEmp()
            totalSpouseContrib = totalSpouseContrib + it.getSpouseContrib()
            totalEmployeeTopUp = totalEmployeeTopUp + it.getEmployeeTopUp()

            def row = [
                it.getEmployeeId(),
                it.getMemberNo(),
                it.getEmployeeSurName(),
                it.getCompSGEmployer().toString(),
                it.getAwardContrib().toString(),
                it.getOptionalEmployerSacrifice().toString(),
                it.getSalarySacrificeNonContEmp().toString(),
                it.getSpouseContrib().toString(),
                it.getEmployeeTopUp().toString()
            ]

            trtEisCsv.append row.join(',')
            trtEisCsv.append END_OF_LINES
        }

        trtEisCsv.append("Total, , ,${totalCompSGEmployer.toString()},${totalAwardContrib.toString()}," +
                "${totalOptionalEmployerSacrifice.toString()},${totalSalarySacrificeNonContEmp.toString()},"+
                "${totalSpouseContrib.toString()},${totalEmployeeTopUp.toString()}" + END_OF_LINES)

        BigDecimal grandTotal = 0
        grandTotal = totalCompSGEmployer+totalAwardContrib+totalOptionalEmployerSacrifice+totalSalarySacrificeNonContEmp+
                totalSpouseContrib+totalEmployeeTopUp
        trtEisCsv.append("Grand Total, , ,${grandTotal.toString()}, , , , , " + END_OF_LINES)
    }

    private void copyToHashDir(){
        // read this file into InputStream
        EllipseFileSystem efs = EFSHelper.getEllipseFileSystem()
        EFSFile f = new EFSFile(getUUID(),"TRTEIS.csv")
        OutputStream os = efs.write(f);

        File inputFile = new File (csvFileName)
        InputStream  input = new FileInputStream(inputFile)

        int read = 0
        byte[] bytes = new byte[1024]

        while ((read = input.read(bytes)) != -1) {
            os.write(bytes, 0, read)
        }

        input.close()
        os.flush()
        os.close()

    }
}

ProcessTrbeis process = new ProcessTrbeis()
process.runBatch(binding)
