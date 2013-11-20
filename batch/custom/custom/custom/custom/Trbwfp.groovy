/**
 * @Ventyx 2012
 * The TRBWFP Work Force Profile extract/reporting program, will
 * collect and analyze the characteristics of the NSW Public
 * sector employment on a regular basis.
 * 
 * It will include demographic information such as age, gender
 * EEO group membership and work location, as well as employment
 * information such as hours worked, leave patterns remuneration
 * and mobility within the sector.
 * 
 * The user is encouraged to enter the locations of user defined
 * variables and locations, in order for the program to work
 * successfully. This is achieved via the new table code 'NSWP'.
 * (Most) Parameters will also be a mandatory requirement, prompting
 * the user to enter date ranges/restrictions and earnings codes
 * required for specific sections of the extract file.
 * These include:
 *  1.  Census Date
 *  2.  Agency Code
 *  3.  Reference Period Start Date
 *  4.  Reference Period   End Date
 *  5.  (Option of two) Leave type for Recreational Leave.
 *  6.  (Option of two) Leave type for         Sick Leave.
 *  7.  (Option of two) Leave type for     Extended Leave.
 *  8.  Type of Report (Y,Q,M,Period)
 *  9.  Higher Duty Paid as an allowance (Y/N flag)
 *  10. Salary Maintenance Method
 *      (A-Allowance, P-Position Reason code, S-Salary Code)
 *  11. Pay Group (Generally used for testing purposes 
 *  12. No. Of Weekly Pay Periods for FTE (Z1)
 *  13. No. Of Fortnightly Pay Periods for FTE (Z1)
 *  14. No. Of Monthly Pay Periods for FTE (Z1)
 *  15. Division Id   
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.lsi.buffer.empleavebalance.EmpLeaveBalanceFetchReplyInstance;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;

import groovy.sql.Sql;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf827.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf880.*;
import com.mincom.ellipse.edoi.ejb.msf888.*;
import com.mincom.ellipse.edoi.ejb.msf829.*;
import com.mincom.ellipse.edoi.ejb.msf823.*;
import com.mincom.ellipse.edoi.ejb.msf824.*;
import com.mincom.ellipse.edoi.ejb.msf837.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf766.*;
import com.mincom.ellipse.edoi.ejb.msf763.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf840.*;
import com.mincom.ellipse.edoi.ejb.msf830.*;
import com.mincom.ellipse.edoi.ejb.msf866.*;
import com.mincom.ellipse.edoi.ejb.msf870.*;
import com.mincom.ellipse.edoi.ejb.msf871.*;
import com.mincom.ellipse.edoi.ejb.msf874.*;
import com.mincom.ellipse.edoi.ejb.msf873.*;
import com.mincom.ellipse.edoi.ejb.msf808.*;
import com.mincom.ellipse.edoi.ejb.msf803.*;
import com.mincom.ellipse.edoi.ejb.msf826.*;

import com.mincom.ellipse.eroi.linkage.mssrat.*;

import com.mincom.enterpriseservice.ellipse.empaward.EmpAwardServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.empaward.EmpAwardServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.empaward.EmpAwardServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.empdeduction.EmpDeductionServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.empleavebalance.EmpLeaveBalanceService;
import com.mincom.enterpriseservice.ellipse.empleavebalance.EmpLeaveBalanceServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.empleavebalance.EmpLeaveBalanceServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.empleavebalance.EmpLeaveBalanceServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.empphysicalloc.EmpPhysicalLocServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.empphysicalloc.EmpPhysicalLocServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.empphysicalloc.EmpPhysicalLocServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.emppositions.EmpPositionsServiceRetrieveReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.emppositions.EmpPositionsServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.emppositions.EmpPositionsServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.emppositions.EmpPositionsServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.higherduties.HigherDutiesServiceRetrieveReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.higherduties.HigherDutiesServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.higherduties.HigherDutiesServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.higherduties.HigherDutiesServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.payrollemp.PayrollEmpServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.payrollemp.PayrollEmpServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.payrollemp.PayrollEmpServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.personnelemp.PersonnelEmpServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.personnelemp.PersonnelEmpServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.personnelemp.PersonnelEmpServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.position.PositionServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.position.PositionServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.position.PositionServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.refcodes.RefCodesServiceRetrieveReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.refcodes.RefCodesServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.refcodes.RefCodesServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.refcodes.RefCodesServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveRequiredAttributesDTO;

public class ParamsTrbwfp{
	//List of Input Parameters
	String paramCensusDate;
	String paramAgencyCode;
	String paramRefPerStrDate;
	String paramRefPerEndDate;
	String paramRecLveType1;
	String paramRecLveType2;
	String paramSicLveType1;
	String paramSicLveType2;
	String paramExtLveType1;
	String paramExtLveType2;
	String paramReportType;
	String paramSalMaintMethod;
	String paramPayGroup;
}

public class EarningTbl{
	//list of field earning tbl that want to build
	String earnCodeEmp, earnCodeRf5
	BigDecimal earnCodeLpu, earnCodeCmu, earnCodeCqu, earnCodeCfu, earnCodeLpa, earnCodeCma, earnCodeCqa, earnCodeCfa
}

public class DeductionTbl{
	//list of field earning tbl that want to build
	String dednCodeEmp, dednCodeLpu, dednCodeCmu, dednCodeCqu, dednCodeCfu, dednCodeLpa, dednCodeCma, dednCodeCqa, dednCodeCfa
}

public class ProcessTrbwfp extends SuperBatch {

	/*
	 * Constants
	 */
	private static final String REPORT_NAME      = "TRBWFPA"
	private static final String CENSUS_FIELD     = "Census Date"
	private static final String AGENCY_FIELD     = "Agency Code"
	private static final String PER_STR_DT_FIELD = "Reference Period Start Date"
	private static final String PER_END_DT_FIELD = "Reference Period End Date"
	private static final String REC_LVE_FIELD    = "Recreational Leave"
	private static final String SICK_LVE_FIELD   = "Sick Leave"
	private static final String EXT_LVE_FIELD    = "Extended Leave"
	private static final String REPORT_TYPE_FIELD   = "Type of Report"
	private static final String SAL_MAINT_MET_FIELD = "Salary Maintenance Method"
	private static final List<String> listTableCode = Arrays.asList(
	"1A","1B","1C","1D","1E","2A","2B","2C","2D",
	"3","3A","3B","3C","3D","3E_A","3F","3G",
	"3H","4A","4B","4C","4D","4E","4G","4I",
	"4J","4K","4L","5A","5B","5C","5D_A","5E",
	"5F","5G","5H","5I","5J","5K","5L","5M",
	"5N","5O","5Q","6A","6B","6C","6D","6E","7A",
	"7B","7C","3I","3J","3K_A","3L","3M","3N",
	"3P","3Q","ZZZZ","Z1","Z2")

	private static final String ERR_INPUT_MANDATORY = "Input Required Error"
	private static final String ERR_INVALID_DATE    = "Invalid Date Value"
	private static final String ERR_LVE_TYPE        = "Invalid LEAVE TYPE"
	private static final String ERR_RPT_TYPE1       = "No totals will be gathered for" +
	'"3ea","3f","4d","4g","4i","4k"' +
	',"4l","5b","5da","5e","5f",'
	private static final String ERR_RPT_TYPE2       = '"5h","5i","5j","5k","5l","5m","5n"' +
	'and "5o" extract fields'
	private static final String ERR_SAL_MAINT_MTHD  = "'4l' extract report error"
	private static final String ERR_TABLE_NSWP      = "NSWP Table Not Set Up"
	private static final String ERR_UDF_MSF760      = "Invalid MSF760 UDF"
	private static final String ERR_AWARD_CODE      = "No Award Code Attached (828)"
	private static final String ERR_TABLE_NSWA      = "No Award Details attached to NSWA"
	private static final String ERR_BIRTH_DATE_1B   = "Invalid Birth Date (1B)"
	private static final String ERR_GENDER_1C       = "Invalid Gender (1C)"
	private static final String ERR_PAY_LOC820      = "invalid 820 paylocation udf (1D)"
	private static final String ERR_PAY_LOC829      = "Invalid 829 Physical Loc UDF(1D)"
	private static final String ERR_EMP_MSF829      = "Employee Not on MSF829 File (1D)"
	private static final String ERR_CASUAL_EMP      = "Invalid Casual employee with no hpaid hours"
	private static final String ERR_CATEG_3G        = "Invalid 760 Category UDF (3G)"
	private static final String ERR_POS_REP_CODE    = "Invalid 870 PosRepCode 4 UDF (3N)"

	private static final String SERVICE_EMPLOYEE    = "EMPLOYEE"
	private static final String SERVICE_TABLE       = "TABLE"
	private static final String SERVICE_REF_CODES   = "REFCODES"
	private static final String SERVICE_AWARD       = "EMPAWARD"
	private static final String SERVICE_PHY_LOC     = "EMPPHYSICALLOC"
	private static final String SERVICE_POSITION    = "POSITION"
	private static final String SERVICE_PAYROLL_EMP = "PAYROLLEMP"

	private static final String TABLE_OLVD = "OLVD"
	private static final String TABLE_NSWP = "NSWP"
	private static final String TABLE_NSWA = "NSWA"
	private static final String TABLE_TFRR = "TFRR"
	private static final String TABLE_EXTY = "EXTY"
	private static final String TABLE_POOC = "POOC"
	private static final String CSV_TRTWFP = "TRTWFP"
	private static final String MIN_DATE   = "19000101"

	private static final int MAX_INSTANCE  = 20

	/* 
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT 
	 */
	private version = 7;
	private ParamsTrbwfp batchParams;

	/*
	 * variables
	 */
	private File trtWFPFile
	private BufferedWriter csvTrtWFPWriter
	private EmployeeServiceRetrieveReplyDTO empR
	private HashMap<String, String> assocValNswp
	private String errMessages
	private String errValue
	private String errField
	private String nswaEmpLegId
	private String nswaSalAwdId
	private String nswaCondEmpAwd
	private String nswaAgcyOccAwd
	private String nswaOverTime
	private String nswaHighDuties
	private String nswaRecLeave
	private String nswaSickLeave
	private String nswaSickLveCarer
	private String nswaUnpaidSickLve
	private String nswaExtLve
	private String nswaUnpaidLve
	private String nswaMaternityLve
	private String nswaFacsLve
	private String nswaLumpSums
	private String nswaSalaryMaint
	private String nswaSupContrib
	private String nswaRecruitRetAll
	private String nswaDisplacement
	private String nswaIncExcAwd
	private String awdPayFrqC0
	private BigDecimal awdHrsPerPRD
	private BigDecimal empContractHrs
	private String empClass
	private String empPayGroup
	private String exitType
	private String empPrivInd
	private List<EarningTbl> earnList
	private BigDecimal calcAnlRate
	private BigDecimal baseAnlRate
	private BigDecimal bTermAnlRate
	private BigDecimal baseAnlHdRate
	private BigDecimal temp4C
	private BigDecimal temp4AB
	private BigDecimal temp4ABT
	private String baseFreqType
	private String baseFreqHdType
	private String hdRateRef
	private String posId878
	private String terminatePos
	private String suspendPos

	private boolean invalidEmp
	private boolean firstErr
	private boolean firstCsv
	private boolean errorFlag
	private boolean earnCodeFnd
	private boolean hisPosFnd
	private boolean lveTypefnd
	private boolean dispEmpValueFnd
	private def ReportA

	//variable extract
	private String extractAgencyCode, extract1A, extract1B, extract1C, extract1D , extract1E = " "
	private String extract2A, extract2B, extract2C, extract2D = " "
	private String extract3, extract3A, extract3B, extract3C, extract3G, extract3H, extract3L  = " "
	private String extract3M, extract3N, extract3O, extract3P, extract3Q = " "
	private String extract4M, extract4N = " "
	private BigDecimal extract3D, extract3EA, extract3F, extract3I, extract3J, extract3KA = 0
	private BigDecimal extract4A, extract4B, extract4C, extract4D, extract4E, extract4G = 0
	private BigDecimal extract4H, extract4I, extract4J, extract4K, extract4L = 0
	private BigDecimal extract5A, extract5B, extract5C, extract5DA, extract5E, extract5F, extract5G = 0
	private BigDecimal extract5H, extract5I, extract5J, extract5K, extract5L, extract5M, extract5N = 0
	private BigDecimal extract5O, extract5P, extract5Q = 0
	private String extract6A, extract6B, extract6C, extract6C1, extract6C2, extract6D, extract6E = " "
	private String extract7A, extract7B, extract7C = " "
	private BigDecimal extractZ1, extractZ11, extractZ2 = 0
	private String extractZ4, extractZ5, extractZ6, extract8C, extract8D = " "


	public void runBatch(Binding b){
		init(b);

		printSuperBatchVersion();
		info("runBatch Version : " + version);

		batchParams = params.fill(new ParamsTrbwfp())

		//PrintRequest Parameters
		info("CensusDate     : " + batchParams.paramCensusDate)
		info("AgencyCode     : " + batchParams.paramAgencyCode)
		info("RefPerStrDate  : " + batchParams.paramRefPerStrDate)
		info("RefPerEndDate  : " + batchParams.paramRefPerEndDate)
		info("RecLveType1    : " + batchParams.paramRecLveType1)
		info("RecLveType2    : " + batchParams.paramRecLveType2)
		info("SicLveType1    : " + batchParams.paramSicLveType1)
		info("SicLveType2    : " + batchParams.paramSicLveType2)
		info("ExtLveType1    : " + batchParams.paramExtLveType1)
		info("ExtLveType2    : " + batchParams.paramExtLveType2)
		info("ReportType     : " + batchParams.paramReportType)
		info("SalMaintMethod : " + batchParams.paramSalMaintMethod)
		info("PayGroup       : " + batchParams.paramPayGroup)


		try {
			processBatch();
		}catch(Exception e){
			info("error ${e.printStackTrace()}")
			e.printStackTrace()
			def logObject = LoggerFactory.getLogger(getClass());
			logObject.trace("------------- TRBWFP ERROR TRACE ", e)
			info("Process terminated. ${e.getMessage()}")
		}
		finally {
			printBatchReport();
		}
	}

	/**
	 * Process the main batch.
	 */
	private void processBatch(){
		info("processBatch");
		initialize()
		validateReqParam()
		getNswpValues()
		validateTableNswp()
		if (!errorFlag){
			processBrowseEmployee()
		}
	}

	/**
	 * Initialize the variables.
	 */
	private void initialize(){
		info("initialize")
		firstErr = true
		firstCsv = true
		errorFlag = false
		invalidEmp = false
		earnCodeFnd = false
		terminatePos = commarea.terminatePos
		suspendPos = commarea.suspendPos
	}

	/**
	 * Validate all request params
	 * CensusDate
	 * Agency Code
	 * Reference period Start Date
	 * Reference Period End Date
	 * Leave Type
	 * Type of report
	 */
	private void validateReqParam(){
		info("validateReqParam")
		validateCensusDate()
		validateAgencyCode()
		validateRefPerStrDate()
		validateRefPerEndDate()
		validateRecLveType()
		validateSicLveType()
		validateExtLveType()
		validateReportType()
		validateSalMaintMethod()
	}

	/**
	 * validate table NSWP, if there is table code
	 * that not set up yet, print error
	 */
	private void validateTableNswp(){
		info("validateTableNswp")
		for (String list : listTableCode) {
			if (!assocValNswp.containsKey(list)){
				info("table ${list}/${TABLE_NSWP} Not Setup")
				errField = ""
				errValue = ""
				errMessages = "table ${list}/${TABLE_NSWP} Not Setup"
				printErrorMsg()
				errorFlag = true
			}
		}
	}

	/**
	 * browse all employee core details msf810 using employee service
	 * this service also include check whether the employee exist in personnel details
	 * and payroll detials or not
	 */
	private void processBrowseEmployee(){
		info("processBrowseEmployee")
		/**
		 * use service employee to retrieve all of employee
		 */
		ArrayList<EmployeeServiceRetrieveReplyDTO> employeeList = retriveEmployee()
		debug("afterArrayList")
		String sHireDate
		String sServiceDate
		String sTerminateDate
		String tableChosen
		String udfChosen760
		String udfChosen810
		int count = 0
		for (EmployeeServiceRetrieveReplyDTO emp : employeeList){
			count++
			invalidEmp = false
			empR = new EmployeeServiceRetrieveReplyDTO()
			empR = emp
			sHireDate = calendarToString(emp.hireDate)
			sHireDate = isValidDate(sHireDate)? sHireDate : "00000000"
			sServiceDate = calendarToString(emp.serviceDate)
			sServiceDate = isValidDate(sServiceDate)? sServiceDate : "00000000"
			sTerminateDate = calendarToString(emp.terminationDate)
			sTerminateDate = isValidDate(sTerminateDate)? sTerminateDate : "00000000"
			tableChosen = assocValNswp.get("ZZZZ").toString().substring(0,6)
			udfChosen760 = assocValNswp.get("ZZZZ").toString().substring(6,10)
			udfChosen810 = assocValNswp.get("ZZZZ").toString().substring(6,9)

			info("employee Id : ${empR.employee}")
			info("employee name : ${emp.getFirstName()}")
			info("employee status : ${emp.getPersEmpStatus()}")
			info("employee status : ${emp.getPersonnelStatus()}")
			info("staff category : ${emp.staffCategory}")
			info("employee type : ${emp.employeeType}")
			info("hire date : ${sHireDate}")
			info("hire date1: ${emp.hireDate.getTime()}")
			info("service date : ${sServiceDate}")
			info("termination date1: ${empR.terminationDate.getTime()}")
			info("termination date : ${sTerminateDate}")
			/*
			 * Check of employee hired before the period end date
			 * or fired before the period start date.
			 */
			if ((sHireDate < batchParams.paramRefPerEndDate) &&
			(sTerminateDate > batchParams.paramRefPerStrDate ||
			sTerminateDate == "00000000" ||
			sTerminateDate.trim().length() == 0)) {

				if (!batchParams.paramPayGroup?.trim() ||
				emp.paygroup.equals(batchParams.paramPayGroup.trim())){

					if (tableChosen == "MSF760"){
						debug("getDetail MSF760")
						empPrivInd = getPrivacyEmp760(udfChosen760, emp)
						info("employee privacy: ${empPrivInd}")
					}else if (tableChosen == "MSF810"){
						debug("getDetail MSF810")
						empPrivInd = getPrivacyEmp810(udfChosen810)
						info("employee privacy: ${empPrivInd}")
					}
					getLevel1WFP()
					if(!invalidEmp){
						getLevel2WFP()
						getLevel3WFP()
						if(!invalidEmp){
							getLevel4WFP()
							getLevel5WFP()
							getLevel6WFP()
							getLevel7AWFP()
							getLevel7WFP()
							getLevel3IWFP()
							getLevel3JWFP()
							getLevel3KAWFP()
							getLevel3LWFP()
							getLevel3NWFP()
							getLevel3MWFP()
							getLevel3PWFP()
							getLevel3QWFP()
							getLevel3OWFP()
							getLevel8AWFP()
							getLevel8BWFP()
							getLevel8CWFP()
							getLevel8DWFP()
							addEmpRecordToExtract()
						}
					}
				}
			}
		}
		info("total emp: ${count}")
	}

	/**
	 * use service employee to retrieve all of employee
	 * @return List<EmployeeServiceRetrieveReplyDTO>
	 */
	private ArrayList<EmployeeServiceRetrieveReplyDTO> retriveEmployee(){
		debug("retriveEmployee")
		ArrayList<EmployeeServiceRetrieveReplyDTO> empReplyList = new ArrayList<EmployeeServiceRetrieveReplyDTO>()
		def restart = ""
		boolean firstLoop = true
		int eCount = 0
		while (firstLoop || (restart !=null && restart.trim().length() > 0)){
			eCount++
			EmployeeServiceRetrieveReplyCollectionDTO employeeReplyDTO = employeeServiceRet(restart)
			debug("finServiceRet")
			firstLoop = false
			restart = employeeReplyDTO.getCollectionRestartPoint()
			debug("restartPoint")
			empReplyList.addAll(employeeReplyDTO.getReplyElements())
			debug("replyElements")
			debug("total retriveEmployee: ${eCount}")
		}

		return empReplyList
	}

	/**
	 * retrieve employee using sevice
	 * @param restart
	 * @return
	 */
	private EmployeeServiceRetrieveReplyCollectionDTO employeeServiceRet(def restart){
		info("employeeServiceRet")
		EmployeeServiceRetrieveRequiredAttributesDTO empReqAtt = new EmployeeServiceRetrieveRequiredAttributesDTO()
		EmployeeServiceRetrieveReplyCollectionDTO employeeReplyDTO = service.get(SERVICE_EMPLOYEE).retrieve({EmployeeServiceRetrieveRequestDTO it ->
			it.nameSearchMethod = "A"
		},MAX_INSTANCE,  restart)
		return employeeReplyDTO
	}

	/**
	 * retrieve employee using sevice
	 * @param empId
	 * @return
	 */
	private EmployeeServiceRetrieveReplyCollectionDTO employeeServiceRetEmpId(String empId){
		info("employeeServiceRetEmpId")
		EmployeeServiceRetrieveRequiredAttributesDTO empReqAtt = new EmployeeServiceRetrieveRequiredAttributesDTO()
		EmployeeServiceRetrieveReplyCollectionDTO employeeReplyDTO = service.get(SERVICE_EMPLOYEE).retrieve({EmployeeServiceRetrieveRequestDTO it ->
			it.nameSearchMethod = "E"
			it.employee = empId
			it.personnelEmployeeInd = false
			it.payrollEmployeeInd = false
		},MAX_INSTANCE,  restart)
		return employeeReplyDTO
	}

	/**
	 * validate census Date must be mandatory
	 * and the format date must be yyyyMMdd
	 */
	private void validateCensusDate(){
		info("validateCensusDate")

		if (!batchParams.paramCensusDate?.trim()){
			errField = CENSUS_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if(!isValidDate(batchParams.paramCensusDate)){
			errField = CENSUS_FIELD
			errValue = batchParams.paramCensusDate
			errMessages = ERR_INVALID_DATE
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate agency code, input must be mandatory
	 */
	private void validateAgencyCode(){
		info("validateAgencyCode")

		if(!batchParams.paramAgencyCode?.trim()){
			errField = AGENCY_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate reference period start Date
	 * input must be mandatory
	 */
	private void validateRefPerStrDate(){
		info("validateRefPerStrDate")
		if (!batchParams.paramRefPerStrDate?.trim()){
			errField = PER_STR_DT_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if(!isValidDate(batchParams.paramRefPerStrDate)){
			errField = PER_STR_DT_FIELD
			errValue = batchParams.paramRefPerStrDate
			errMessages = ERR_INVALID_DATE
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate reference period end date
	 * input must be mandatory
	 */
	private void validateRefPerEndDate(){
		info("validateRefPerEndDate")
		if (!batchParams.paramRefPerEndDate?.trim()){
			errField = PER_END_DT_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if(!isValidDate(batchParams.paramRefPerEndDate)){
			errField = PER_END_DT_FIELD
			errValue = batchParams.paramRefPerEndDate
			errMessages = ERR_INVALID_DATE
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate recreational leave,
	 * input mus be mandatory
	 * input must be exist in OLVD table
	 */
	private void validateRecLveType(){
		info("validateRecLveType")
		if (!batchParams.paramRecLveType1?.trim() && !batchParams.paramRecLveType2?.trim()){
			errField = REC_LVE_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if((batchParams.paramRecLveType1?.trim() && !isOlvdTable(batchParams.paramRecLveType1.substring(0,1)))
		|| (batchParams.paramRecLveType2?.trim() && !isOlvdTable(batchParams.paramRecLveType2.substring(0,1)))){
			errField = REC_LVE_FIELD
			if (batchParams.paramRecLveType1?.trim()){
				errValue = batchParams.paramRecLveType1
			}else{
				errValue = batchParams.paramRecLveType2
			}
			errMessages = ERR_LVE_TYPE
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate sick leave type,
	 * input must be mandatory
	 *input must be exist in OLVD Table
	 */
	private void validateSicLveType(){
		info("validateSicLveType")

		if (!batchParams.paramSicLveType1?.trim() && !batchParams.paramSicLveType2?.trim()){
			errField = SICK_LVE_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if ((batchParams.paramSicLveType1.trim() && !isOlvdTable(batchParams.paramSicLveType1.substring(0,1)))
		||(batchParams.paramRecLveType2?.trim() && !isOlvdTable(batchParams.paramSicLveType2.substring(0,1)))){
			errField = SICK_LVE_FIELD
			if (batchParams.paramRecLveType1?.trim()){
				errValue = batchParams.paramSicLveType1
			}else{
				errValue = batchParams.paramSicLveType2
			}
			errMessages = ERR_LVE_TYPE
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate extended leave type
	 * input must be mandatory
	 * input must be exist in OLVD table
	 */
	private void validateExtLveType(){
		info("validateExtLveType")
		if (!batchParams.paramExtLveType1?.trim() && !batchParams.paramExtLveType2?.trim()){
			errField = EXT_LVE_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if((batchParams.paramExtLveType1?.trim() && !isOlvdTable(batchParams.paramExtLveType1.substring(0,1)))
		|| (batchParams.paramExtLveType2?.trim() && !isOlvdTable(batchParams.paramExtLveType2.substring(0,1)))){
			errField = EXT_LVE_FIELD
			if (batchParams.paramExtLveType1?.trim()){
				errValue = batchParams.paramExtLveType1
			}else{
				errValue = batchParams.paramExtLveType2
			}
			errMessages = ERR_LVE_TYPE
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate report type
	 * the input is mandatory
	 * and must be have value Y, M, Q, P
	 */
	private void validateReportType(){
		info("validateReportType")
		List<String> listReportType = Arrays.asList("Y", "M", "Q", "P")
		if (!batchParams.paramReportType?.trim()){
			errField = REPORT_TYPE_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if(!listReportType.contains(batchParams.paramReportType)){
			errField = REPORT_TYPE_FIELD
			errValue = batchParams.paramReportType
			errMessages = ERR_RPT_TYPE1
			printErrorMsg()
			errField = " "
			errValue = " "
			errMessages = ERR_RPT_TYPE2
			printErrorMsg()
			errorFlag = true
		}
	}

	private void validateSalMaintMethod(){
		info("validateSalMaintMethod")
		List<String> listSalMaint = Arrays.asList("A","P","S")
		if (!batchParams.paramSalMaintMethod?.trim()){
			errField = SAL_MAINT_MET_FIELD
			errValue = ""
			errMessages = ERR_INPUT_MANDATORY
			printErrorMsg()
			errorFlag = true
		}else if (!listSalMaint.contains(batchParams.paramSalMaintMethod)){
			errField = SAL_MAINT_MET_FIELD
			errValue = batchParams.paramSalMaintMethod
			errMessages = ERR_SAL_MAINT_MTHD
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * validate date format using standard ellipse : yyyyMMdd
	 * @param date : String date from the input
	 * @return true or false
	 */
	private boolean isValidDate(String date){
		info("isValidDate: ${date}")
		/* 
		 * if date less than min date (1 jan 1900) than return false
		 */
		if(date >= MIN_DATE){
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
			try{
				format.setLenient(false)
				format.parse(date)
				return true
			}catch(java.text.ParseException e){
				return false
			}
		}else{
			return false
		}
	}

	/**
	 * check leave type against OLVD table
	 * @param tblCode
	 * @return true or false
	 */
	private boolean isOlvdTable(String tblCode){
		info("isOlvdTable: ${tblCode}")
		try{
			TableServiceReadRequiredAttributesDTO tableReadReq = new TableServiceReadRequiredAttributesDTO()
			tableReadReq.returnTableType = true
			tableReadReq.returnTableCode = true
			TableServiceReadReplyDTO tableReadReply = service.get(SERVICE_TABLE).read({ TableServiceReadRequestDTO it ->
				it.requiredAttributes = tableReadReq
				it.tableType = TABLE_OLVD
				it.tableCode = tblCode
			})
			return (tableReadReply !=null)
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot Read Table ${TABLE_OLVD} : ${e.getMessage()}")
			return false
		}
	}

	/**
	 * setup the NSWP table file codes
	 * store into assocNswp
	 */
	private void getNswpValues(){
		info("getNswpValues")
		List<TableServiceRetrieveReplyDTO> listTableReply = new ArrayList<TableServiceRetrieveReplyDTO>()
		assocValNswp = new HashMap<String, String>()

		TableServiceRetrieveRequiredAttributesDTO tableRetReq = new TableServiceRetrieveRequiredAttributesDTO()
		tableRetReq.returnTableType = true
		tableRetReq.returnTableCode = true
		tableRetReq.returnAssociatedRecord = true
		try{
			def restart = ""
			boolean firstLoop = true
			while (firstLoop || (restart !=null && restart.trim().length() > 0)){
				TableServiceRetrieveReplyCollectionDTO tableRetReplyDTO = retrieveTableNSWP(restart)
				firstLoop = false
				restart = tableRetReplyDTO.getCollectionRestartPoint()
				listTableReply.addAll(tableRetReplyDTO.getReplyElements())
			}
			int count = 0
			if (listTableReply != null){
				for (TableServiceRetrieveReplyDTO tbl : listTableReply){
					count++
					info("table type : ${tbl.tableType}")
					info("table type : ${tbl.tableCode}")

					if (listTableCode.contains(tbl.tableCode)){
						info("table code in NSWP : ${tbl.tableCode}")
						assocValNswp.put(tbl.tableCode.trim(), tbl.associatedRecord.trim())
					}
					info("Counter : ${count}")
				}
			}
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot Retrieve Table ${TABLE_NSWP} : ${e.getMessage()}")
			errField = TABLE_NSWP
			errValue = ""
			errMessages = ERR_TABLE_NSWP
			printErrorMsg()
			errorFlag = true
		}
	}

	/**
	 * retrieve Table service
	 * @param restart
	 * @return TableServiceRetrieveReplyCollectionDTO
	 */
	private TableServiceRetrieveReplyCollectionDTO retrieveTableNSWP(def restart){
		info("retrieveTableNSWP")
		TableServiceRetrieveRequiredAttributesDTO tableRetReq = new TableServiceRetrieveRequiredAttributesDTO()
		tableRetReq.returnTableType = true
		tableRetReq.returnTableCode = true
		tableRetReq.returnAssociatedRecord = true

		TableServiceRetrieveReplyCollectionDTO tableRetReplyDTO = service.get(SERVICE_TABLE).retrieve({ TableServiceRetrieveRequestDTO it ->
			it.requiredAttributes = tableRetReq
			it.searchMethod = "A"
			it.objectTypeSearch = "A"
			it.tableType = TABLE_NSWP
		},MAX_INSTANCE, restart)
		return tableRetReplyDTO
	}

	/**
	 * get perosnnel class of msf760
	 * @param udf
	 * @param emp
	 * @return String personnelClass
	 */
	private String getPrivacyEmp760(String udf, EmployeeServiceRetrieveReplyDTO emp){
		info("getPrivacyEmp")
		info ("udf : ${udf}")
		if (udf == "PU01"){
			return emp.personnelClass1
		}else if(udf == "PU02"){
			return emp.personnelClass2
		}else if(udf == "PU03"){
			return emp.personnelClass3
		}else if(udf == "PU04"){
			return emp.personnelClass4
		}else if(udf == "PU05"){
			return emp.personnelClass5
		}else if(udf == "PU06"){
			return emp.personnelClass6
		}else if(udf == "PU07"){
			return emp.personnelClass7
		}else if(udf == "PU08"){
			return emp.personnelClass8
		}else if(udf == "PU09"){
			return emp.personnelClass9
		}else if(udf == "PU10"){
			return emp.personnelClass10
		}else{
			invalidEmp = true
			errField = "UDF"
			errValue = udf
			errMessages = ERR_UDF_MSF760
			printErrorMsg()
			return ""
		}
	}

	/**
	 * get privacy employee from msf810
	 * @param udf
	 * @return ref code
	 */
	private String getPrivacyEmp810(String udf){
		info("getPrivacyEmp810")
		List<RefCodesServiceRetrieveReplyDTO> listRefCodes = new ArrayList<RefCodesServiceRetrieveReplyDTO>()
		RefCodesServiceRetrieveRequiredAttributesDTO refReqAtt = new RefCodesServiceRetrieveRequiredAttributesDTO()

		refReqAtt.returnEntityType = true
		refReqAtt.returnEntityValue = true
		refReqAtt.returnRefCode = true
		try{
			RefCodesServiceRetrieveReplyCollectionDTO refCodesReplyDTO = service.get(SERVICE_REF_CODES).retrieve({RefCodesServiceRetrieveRequestDTO it->
				it.requiredAttributes = refReqAtt
				it.entityType = "EMP"
				it.entityValue = empR.employee
				it.refNo = udf
			},1)
			listRefCodes = refCodesReplyDTO.getReplyElements()
			return listRefCodes[0].getRefCode()

		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot retrieve record for ${SERVICE_REF_CODES} : ${e.getMessage()}")
			return ""
		}
	}

	/**
	 * Read first record of Employee Position.
	 * @param q Query
	 * @return MSF878Rec
	 */
	private  MSF878Rec readFirstEmployeePosition(Query q) {
		info("readFirstEmployeePosition")
		return (MSF878Rec) edoi.firstRow(q)
	}

	/**
	 * Read first record of Employee Earnings.
	 * @param q Query
	 * @return MSF823Rec
	 */
	private  MSF823Rec readFirstEmployeeEearning(Query q) {
		info("readFirstEmployeeEearning")
		return (MSF823Rec) edoi.firstRow(q)
	}

	/**
	 * Read first record of Employee Higher Duties.
	 * @param q Query
	 * @return MSF840Rec
	 */
	private  MSF840Rec readFirstEmployeeHigherDuties(Query q) {
		info("readFirstEmployeeHigherDuties")
		return (MSF840Rec) edoi.firstRow(q)
	}

	/**
	 * get level 1 trbwfp :
	 * 1 - Agency Code
	 * 1a - Unique Identifier
	 * 1b - Date of Birth
	 * 1c - Gender
	 * 1d - Employee Location
	 */
	private void getLevel1WFP(){
		info("getLevel1WFP")

		/*
		 *Set up the default employee award details
		 *via the settings on the NSWA table. 
		 */
		getAwardCodeEmp()
		if (!invalidEmp){
			extract1A = " "
			process1AUniqueId()
			extract1B = " "
			process1BDateOfBirth()
			extract1C = " "
			process1CGender()
			extract1D = " "
			process1DLocation()
			extract1E = " "
			process1ELocation()
			
			// check employee position as the census date
			// use edoi to browse emp Position since service emp position required access 9
			String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
			Constraint cEmployeeId = MSF878Key.employeeId.equalTo(empR.employee)
			Constraint cPrimaryPos = MSF878Key.primaryPos.equalTo("0")
			Constraint cInvStrDate = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
			Constraint cPosStopDate1 = MSF878Key.posStopDate.greaterThanEqualTo(batchParams.paramCensusDate)
			Constraint cPosStopDate2 = MSF878Key.posStopDate.equalTo("00000000")

			def query = new QueryImpl(MSF878Rec.class).and(cEmployeeId).and(cPrimaryPos).and(cInvStrDate.and(cPosStopDate1.or(cPosStopDate2)))

			MSF878Rec msf878Rec = readFirstEmployeePosition(query)
			if(msf878Rec){
				info("position ID : ${msf878Rec.getPrimaryKey().positionId}")
				info("commarea terminate pos : ${terminatePos}")
				/*
				 *if the position is a terminated or suspended position
				 *check if employee was moved to that position within the
				 *reference period. if not, then reject the employee 
				 */
				if(msf878Rec.getPrimaryKey().positionId == terminatePos ||
				msf878Rec.getPrimaryKey().positionId == suspendPos){
					String tmpPosStrDt = (99999999 - msf878Rec.getPrimaryKey().getInvStrDate().toLong()).toString()
					if (tmpPosStrDt < batchParams.paramRefPerStrDate && tmpPosStrDt > batchParams.paramRefPerEndDate){
						invalidEmp = true
					}
				}
			}
		}
	}

	/**
	 * get level 2 of trbwfp such as:
	 * 2a. Aboriginal person or torres strait islander
	 * 2b. Person with a Disability
	 * 2c. Person from a Racial, Ethnic, or Ethno-Religious Minority Group
	 * 2d. Language First Spoken as a child
	 */
	private void getLevel2WFP(){
		info("getLevel2WFP")
		// 2A - Aboriginal Person or Torres Strait Islander
		extract2A = " "
		process2AAborigTsiId()
		// 2B - Person with Disability
		extract2B = " "
		process2BDisabilityId()
		// 2C - Person from Ethnic Group
		extract2C = " "
		process2CEthnicityId()
		// 2D - Language first Spoken
		extract2D = " "
		process2DFirstLanguage()
	}

	/**
	 * get level 3 of trbwfp such as :
	 * 3.  Employing Legislation Identifier
	 * 3a. Award Determining Salary Rate
	 * 3b. Award Determining Conditions of Employment
	 * 3d. Usual Hours Worked
	 * 3e_a. Total Number of Hours Paid Annual Reference Period
	 * 3f. Overtime Hours
	 * 3g. Employment Category
	 * 3h. Census Date Status
	 */
	private void getLevel3WFP(){
		info("getLevel3WFP")

		// 3 - Employee Legislation Id
		extract3 = " "
		process3EmpLegId()
		// 3a - Award Determining Salary Rate
		extract3A = " "
		process3ASalAwdId()
		// 3b - Award Determining Conditions of Employment
		extract3B = " "
		process3BCondEmpAwdId()
		// 3d - Employee Usual Hours Worked per week
		extract3D = 0
		process3DEmpUslHrsWkd()
		// 3EA - Total Number of Hours Paid Annual Reference Period
		extract3EA = 0
		earnCodeFnd = false
		process3EAEmpPaidHrsRp()
		if (extract3EA == 0){
			if(empClass == "C"){
				invalidEmp = true
				errField = " "
				errValue = empR.employee
				errMessages = ERR_CASUAL_EMP
				printErrorMsg()
			}else{
				if(!earnCodeFnd){
					extract3EA = -1
				}
			}
		}
		// 3F - Overtime Hours
		extract3F = 0
		process3FEmpOtimeHrsRp()
		if(extract3F == 0 && !earnCodeFnd){
			extract3F = -1
		}
		//3G - Employee Category
		extract3G = " "
		process3GEmployeeCateg()
		//3H - Census Date Status
		extract3H = " "
		process3HCensusStatus()
	}

	/**
	 * get level 4 of trbwfp such as:
	 * 4A - Total Annual Remuneration (Substantive Position)
	 * 4B - Total Annual Remuneration (Current Position)
	 * 4C - Recruitment Remuneration
	 * 4D - Total Gross Earnings in Reference Period
	 * 4E - Total Gross Earnings for Census Pay Period.
	 * 4G - Actual Overtime Payments (Reference Period)
	 * 4H - Actual Higher Duty Allowances (reference Period)
	 * 4I - Actual Voluntary Red. Lump Sum Pymnts (ref period)
	 * 4K - Actual Recruitment Retention Skills Shortage
	 * 4L - Actual Salary Maintenance Payments (reference Period)
	 * 4M - Leave loading, TransGrid does not have leave loading thus it is hard coded to -2
	 * 4N - Remote Area Allowance, TransGrid does not have leave loading thus it is hard coded to -2
	 */
	private void getLevel4WFP(){
		info("getLevel4WFP")
		String termDate = calendarToString(empR.terminationDate)
		String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()


		//4A - Total Annual Remuneration (Substantive Position)
		extract4A = 0
		process4ATotAnnualRemRp()
		//4B - Total Annual Remuneration (Current Position)
		extract4B = 0
		process4BTotAnnualRemCp()
		//4C - Recruitment Remuneration
		process4CRecruitRemun()
		//4D - Total Gross Earnings in Reference Period
		earnCodeFnd = false
		extract4D = 0
		process4DTotGrossEarnRp()

		if(!earnCodeFnd){
			extract4D = -1
		}

		//4E - Total Gross Earnings for Census Pay Period.
		earnCodeFnd = false
		extract4E = 0
		process4ETotGrossEarnCp()

		if(extract4E == 0){
			if(empPrivInd == "F"){
				extract4E = -3
			}else{
				extract4E = 0
			}
		}else{
			if(termDate < batchParams.paramCensusDate &&
			termDate != "00000000" &&
			termDate?.trim()){

				extract4E = -2
			}
			if(empClass =="C"){
				extract4E = 0
			}else if(empPrivInd == "F"){
				extract4E = -3
			}
		}
		if(awdPayFrqC0 == "W"){
			extract4E = extract4E * 2
		}else if(awdPayFrqC0 == "M"){
			extract4E = (extract4E*12)/26.25
		}

		//4G - Total Gross Earnings for Census Pay Period.
		earnCodeFnd = false
		extract4G = 0
		if(nswaOverTime == "Y"){
			process4GActualOtimePmnts()
		}else{
			extract4G = -2
		}
		//If the accumulated units are equal to zero
		//and if a unit was found, move zeros to the extract
		//otherwise move -1 (missing) units to theextract.
		if(extract4G == 0){
			if(empPrivInd == "F"){
				extract4G = -3
			}
		}else{
			if(empPrivInd == "F"){
				extract4G = -3
			}
		}

		// * and moved section to here so that it is done
		// * at the same time as the new 6C separation type
		// * 6C will now be the amalgamation of the former
		// * 6C movemement type and 6D separation type
		//6C - Movement Type
		extract6C = extract6C1 = extract6C2 = " "
		process6CEmpMovement()
		// 6C - Separation Type
		if(!extract6C2?.trim()){
			process6CSeparationType()
			if(!extract6C2?.trim()){
				if(extract6C1 == "1"){
					extract6C2 = "03"
				}else if(extract6C1 == "2"){
					extract6C2 = "05"
				}else if(extract6C1 == "3"){
					extract6C2 = "11"
				}
			}
		}
		extract6C = extract6C1 + extract6C2

		if((empClass == "F" || empClass == "C") &&
		(extract3H == "2") &&
		(!extract6C?.trim())){
			extract6C = "-1"
		}

		// 4H - Actual Earnings - Recreation Leave Lump Sum Payout
		earnCodeFnd = false
		extract4H = 0
		if(nswaLumpSums == "Y"){
			process4HActualLrlsPmnts()

			if(earnCodeFnd){
				if(termDate >= batchParams.paramRefPerStrDate &&
				termDate <= batchParams.paramRefPerEndDate &&
				termDate != "00000000" &&
				termDate?.trim() &&
				extract6C?.trim()){
				}else{
					extract4H = -1
				}
			}else{
				extract4H = 0
			}
		}else{
			extract4H = -2
		}

		// 4I - Actual Voluntary Red. Lump Sum Pymnts (ref period)

		earnCodeFnd = false
		extract4I = 0
		if(nswaLumpSums == "Y"){
			//Only if the 6C separation type is 14, will the program look
			//for values in 4I.
			// new regulation change the code from 14 to 09
			if(extract6C2 == "09"){
				process4IActualVrlsPmnts()
			}
		}else{
			extract4I = -2
		}

		//if employee have full privacy move 3 to extract

		if(empPrivInd == "F"){
			extract4I = -3
		}

		// 4J - Actual Earnings . Extended Leave Lump Sum Payout
		earnCodeFnd = false
		extract4J = 0
		if(nswaLumpSums == "Y"){
			process4JActualLelsPmnts()
			if(earnCodeFnd){
				if(termDate >= batchParams.paramRefPerStrDate &&
				termDate <= batchParams.paramRefPerEndDate &&
				termDate != "00000000" && termDate?.trim() &&
				extract6C?.trim()){
					extract4J = extract4J
				}else{
					extract4J = -1
				}
			}else{
				extract4J = 0
			}
		}else{
			extract4J = -2
		}

		// 4K - Actual Recruitment Retention Skills Shortage
		// Allowance (reference Period)
		earnCodeFnd = false
		extract4K = 0
		if(nswaRecruitRetAll == "Y"){
			process4KActualRcrtRsAllow()
		}else{
			extract4K = -2
		}

		//if employee have full privacy move 3 to extract

		if(empPrivInd == "F"){
			extract4K = -3
		}

		// 4L - Actual Salary Maintenance Payments (reference Period)
		earnCodeFnd = false
		extract4L = 0
		if(empClass == "C"){
			extract4L = -2
		}else{
			if(nswaSalaryMaint == "Y"){
				// * CHeck that the parameter is populated with valid entry.
				if(batchParams.paramSalMaintMethod == "A"){
					process4LActualSalMaintRsA()
					// * If the accumulated units are equal to zero
					// * and if a unit was found, move zeros to the extract
					// * otherwise move -1 (missing) units to theextract.
					if(extract4L <= 0){
						if(empPrivInd == "F"){
							extract4L = -3
						}else{
							extract4L = 2
						}
					}else{
						if(empPrivInd == "F"){
							extract4L = -3
						}else{
							extract4L = 1
						}
					}
				}else if(batchParams.paramSalMaintMethod == "P"){
					if(empPrivInd == "F"){
						extract4L = -3
					}else{
						hisPosFnd = false
						if(process4LActualSalMaintRsB()){
							extract4L = 1
						}else{
							if(hisPosFnd){
								extract4L = 2
							}else{
								extract4L = -1
							}
						}
					}
				}else if(batchParams.paramSalMaintMethod == "S"){
					if(empPrivInd == "F"){
						extract4L = -3
					}else{
						process4LActualSalMaintRsC()
					}
				}
			}else{
				if(empPrivInd == "F"){
					extract4L = -3
				}else{
					extract4L = -2
				}
			}
		}
		
		extract4M = "-2"
		extract4N = "-2"
	}

	/**
	 * get level 5 of trbwfp such as:
	 * 5A - Recreation Leave Accrued as at Census Date
	 * 5B - Recreation Leave Taken (reference Period)
	 * 5C - Sick Leave Accrued as at Census Date
	 * 5DA - Paid Sick Leave Taken during the annual reference period
	 * 5E - Unpaid Sick Leave Taken (reference Period)
	 * 5F - Paid Sick Leave Taken as carer's leave (reference Period)
	 * 5G - Long Service Leave Accrued as at Census Date
	 * 5H - Extended Leave Taken on Full Pay (reference Period)
	 * 5I - Extended Leave Taken on Half Pay (reference Period)
	 * 5J - Maternity Leave Taken on Full Pay (reference Period)
	 * 5K - Maternity Leave Taken on Half Pay (reference Period)
	 * 5L - Unpaid Maternity Leave Taken(reference Period)
	 * 5M - Family and Community Services Leave Taken(reference Period)
	 * 5N - Unpaid Leave Taken(reference Period)
	 * 5O - Extended Leave Taken at Double Pay (Reference Period)
	 * 5Q - Special Leave Taken During the Reference Period
	 */
	private void getLevel5WFP(){
		info("getLevel5WFP")

		//5A - Recreation Leave Accrued as at Census Date
		lveTypefnd = false
		earnCodeFnd = false
		extract5A = 0
		if(nswaRecLeave == "Y"){
			process5ARecLveAccrdCd()
		}else{
			extract5A = -2
		}

		if(empPrivInd == "F"){
			extract5A = -3
		}

		//5B - Recreation Leave Taken (reference Period)
		lveTypefnd = false
		earnCodeFnd = false
		extract5B = 0
		if(nswaRecLeave == "Y"){
			process5BRecLeaveTakenRp()
		}else{
			extract5B = -2
		}

		if(empPrivInd == "F"){
			extract5B = -3
		}

		//5C - Sick Leave Accrued as at Census Date
		lveTypefnd = false
		earnCodeFnd = false
		extract5C = 0
		if(nswaSickLeave == "Y"){
			process5CSickLveAccrdCd()
		}

		if(empPrivInd == "F"){
			extract5C = -3
		}

		//5DA - Sick Leave Accrued as at Census Date
		lveTypefnd = false
		earnCodeFnd = false
		extract5DA = 0
		if(nswaSickLeave == "Y"){
			process5DASickLeaveTakenRp()
		}else{
			extract5DA = -2
		}

		if(empPrivInd == "F"){
			extract5DA = -3
		}

		//5E - Unpaid Sick Leave Taken (reference Period)
		lveTypefnd = false
		earnCodeFnd = false
		extract5E = 0
		if(nswaUnpaidSickLve == "Y"){
			process5EUnpaidSickLeaveRp()
		}else{
			extract5E = -2
		}

		if(empPrivInd == "F"){
			extract5E = -3
		}

		//5F - Paid Sick Leave Taken as carer's leave (reference Period)
		extract5F = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaSickLveCarer == "Y"){
			process5FPaidSlCarerRp()
		}else{
			extract5F = -2
		}

		if(empPrivInd == "F"){
			extract5F = -3
		}

		//5G - Long Service Leave Accrued as at Census Date
		extract5G = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaExtLve == "Y"){
			process5GLongServLveAccrdCd()
		}else{
			extract5G = -2
		}

		if(empPrivInd == "F"){
			extract5G = -3
		}

		//5H - Extended Leave Taken on Full Pay (reference Period)
		extract5H = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaExtLve == "Y"){
			process5HExtLveFpRp()
		}else{
			extract5H = -2
		}

		if(empPrivInd == "F"){
			extract5H = -3
		}

		//5I - Extended Leave Taken on Half Pay (reference Period)
		extract5I = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaRecLeave == "Y"){
			process5IExtLveHpRp()
		}else{
			extract5I = -2
		}

		if(empPrivInd == "F"){
			extract5I = -3
		}

		//5J - Maternity Leave Taken on Full Pay (reference Period)
		extract5J = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaMaternityLve == "Y" && extract1C == "2"){
			process5JMatLveFpRp()
		}else{
			extract5J = -2
		}

		if(empPrivInd == "F"){
			extract5J = -3
		}

		//5K - Maternity Leave Taken on Half Pay (reference Period)
		extract5K = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaMaternityLve == "Y" && extract1C == "2"){
			process5KMatLveHpRp()
		}else{
			extract5K = -2
		}

		if(empPrivInd == "F"){
			extract5K = -3
		}

		//5L - Unpaid Maternity Leave Taken(reference Period)
		extract5L = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaMaternityLve == "Y"){
			process5LUnpaidMatLveRp()
		}else{
			extract5L = -2
		}

		if(empPrivInd == "F"){
			extract5L = -3
		}

		//5M - Family and Community Services Leave Taken(reference Period)
		extract5M = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaFacsLve == "Y"){
			process5MFamComSrvLveRp()
		}else{
			extract5M = -2
		}
		if(empPrivInd == "F"){
			extract5M = -3
		}

		//5N - Unpaid Leave Taken(reference Period)
		extract5N = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaUnpaidLve == "Y"){
			process5NUnpaidLveRp()
		}else{
			extract5N = -2
		}
		if(empPrivInd == "F"){
			extract5N = -3
		}

		//5O - Extended Leave Taken at Double Pay (Reference Period)
		extract5O = 0
		lveTypefnd = false
		earnCodeFnd = false
		if(nswaExtLve == "Y"){
			process5OExtLveDblPayRp()
		}else{
			extract5O = -2
		}
		if(empPrivInd == "F"){
			extract5O = -3
		}

		//5P - Commonwealth Paid Parental Leave
		extract5P = 0
		earnCodeFnd = false
		lveTypefnd = false
		process5PComPaidParLveRp()
		if(earnCodeFnd){
			extract5P = 1
		}else{
			extract5P = 2
		}
		
		//5Q - Special Leave Taken During the Reference Period
		extract5Q = 0
		lveTypefnd = false
		earnCodeFnd = false
		process5QSpecialLeaveTaken()
		
	}
	/**
	 * get level 6 of trbwfp such as: 
	 * 6A - Date of Most recent Public Sector Entry
	 * 6B - Date of Commencement with the agency
	 * 6D - Date of Separation
	 * 6E - Displaced Employees
	 */
	private void getLevel6WFP(){
		info("getLevel6WFP")
		String hireDate = calendarToString(empR.hireDate)

		//6A - Date of Most recent Public Sector Entry
		process6AMostRecentPs()
		//6B - Date of Commencement with the agency
		extract6B = hireDate
		//6D - Date of Separation
		process6DDateOfSeparation()
		if((empClass == "F" || empClass == "P") &&
		extract3H == "2" &&
		!extract6D?.trim()){
			extract6D = "-1"
		}else{
			if(extract3H == "-1" &&
			extract6D?.trim() &&
			extract6D != batchParams.paramCensusDate){
				extract6D = " "
			}
		}
		//6E - Displaced Employees
		process6EDisplacedEmployee()

	}

	/**
	 * get level 7A of trbwfp
	 * 7A - Sub Agency Code
	 */
	private void getLevel7AWFP(){
		info("getLevel7AWFP")
		//7A - Sub Agency Code
		extract7A = " "
		process7ASubAgyCode()
	}
	/**
	 * get level 7 of trbwfp
	 * 7B - Agency Code 2
	 * 7C - Agency Code 3
	 * 3C - Agency or Occupation Specific Award Identifier
	 */
	private void getLevel7WFP(){
		info("getLevel7WFP")
		//7B - Agency Coce 2
		extract7B = " "
		process7BAgencyCode()
		//7C - Agency Coce 3
		extract7C = " "
		process7CAgencyCode()
		//3C - Agency or Occupation Specific Award Identifier
		process3CAgyOccAwdId()
	}

	/**
	 * get level 3I of Trbwfp
	 * Total Hours Paid (Census Period)
	 */
	private void getLevel3IWFP(){
		info("getLevel3IWFP")
		String termDate = calendarToString(empR.terminationDate)
		extract3I = 0

		if(empClass == "C"){
			extract3I = 0
		}else{
			if(termDate < batchParams.paramCensusDate &&
			termDate != "00000000" &&
			termDate?.trim()){
				extract3I = -2
			}else{
				earnCodeFnd = false
				process3ITotHrsPaid()
				if(extract3I == 0 && !earnCodeFnd){
					extract3I = -1
				}

				if(awdPayFrqC0 == "W"){
					extract3I = extract3I * 2
				}else{
					if(awdPayFrqC0 == "M"){
						extract3I = (extract3I * 12)/26.25
					}
				}
			}
		}
	}

	/**
	 * get level 3I of Trbwfp
	 * Standard Weekly Full Time Award/Agreement Hours
	 */
	private void getLevel3JWFP(){
		info("getLevel3JWFP")
		extract3J = 0
		Calendar censusDate = stringToCalendar(batchParams.paramCensusDate)

		try{
			MSF820Rec msf820rec = edoi.findByPrimaryKey(new MSF820Key(empR.employee))
			//Find the employee's award at as the census date
			EmpAwardServiceReadRequiredAttributesDTO awdReqAtt = new EmpAwardServiceReadRequiredAttributesDTO()
			awdReqAtt.returnAwardCode = true
			try{
				EmpAwardServiceReadReplyDTO empAwardDTO = service.get(SERVICE_AWARD).read({EmpAwardServiceReadRequestDTO it ->
					it.requiredAttributes = awdReqAtt
					it.employee  = empR.employee
					it.startDate = censusDate
				})
				if(empAwardDTO != null){
					//Full Time employee use 3D
					if(msf820rec.getEmployeeClass() == "F"){
						extract3J = extract3D
					}else if(msf820rec.getEmployeeClass() == "P"){
						//Part Time employee use Hours per Week from Award
						// use screen service mso8fa to get value award code details
						info("Read Award Details")
						MSF801_C0_801Rec msf801_c0_rec = getAwardCodeDet(empAwardDTO.awardCode)
						if (msf801_c0_rec!=null){
							extract3J = msf801_c0_rec.getStdHrsWkC0()
						}else {
							extract3J = -1
						}
					}else{
						extract3J = -1
					}
				}
			}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
				info("Error when read award Service ${e.getMessage()}")
				extract3J = -1
			}
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			info("EmployeePayrollDetail-NotFound-3J")
			extract3J = -1
		}
	}

	/**
	 * process 3KA - Number of Days Annual Reference Period
	 */
	private void getLevel3KAWFP(){
		info("getLevel3KAWFP")
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
		sdf.setLenient(false)
		Date date1 = sdf.parse(batchParams.paramRefPerStrDate)
		Date date2 = sdf.parse(batchParams.paramRefPerEndDate)
		extract3KA = 0

		int diff = date2 - date1
		info ("different : ${diff.toString()}")
		extract3KA = diff + 1
	}

	/**
	 * Process 3L - Position Code
	 */
	private void getLevel3LWFP(){
		info("getLevel3LWFP")
		extract3L = "9999999"
		String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
		String revsdRpStrDate = (99999999 - batchParams.paramRefPerStrDate.toLong()).toString()
		String posCensus = " "
		//Find the employee's higher duty position as at the census date
		//use edoi to find employee position since employee position service
		//required access 9
		Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
		Constraint c2 = MSF878Key.primaryPos.equalTo("2")
		Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
		Constraint c4 = MSF878Key.posStopDate.greaterThanEqualTo(batchParams.paramCensusDate)
		Constraint c5 = MSF878Key.posStopDate.equalTo("00000000")
		def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3.and(c4.or(c5)))
		MSF878Rec msf878Rec = readFirstEmployeePosition(query)
		if(msf878Rec != null){
			posCensus = msf878Rec.getPrimaryKey().getPositionId()
			processWfpPosCode(posCensus)
		}else{
			// Find the employee's secondary position as at the census date
			// if no HD position found
			Constraint c6 = MSF878Key.primaryPos.equalTo("1")
			def query1 = new QueryImpl(MSF878Rec.class).and(c1).and(c6).and(c3.and(c4.or(c5)))
			MSF878Rec msf878Rec1 = readFirstEmployeePosition(query1)
			if(msf878Rec1 != null){
				// Check if secondary position is an HD position
				if(msf878Rec1.getChangeReason()?.trim()){
					String assocVal = readTableService(TABLE_TFRR, msf878Rec1.changeReason).padRight(50)
					if(assocVal.substring(3,4) == "Y"){
						posCensus = msf878Rec1.getPrimaryKey().getPositionId()
						processWfpPosCode(posCensus)
					}
				}
			}else{
				//* Find the employee's primary position as at the census date
				//* if no HD position found
				info("find primary position")
				Constraint c7 = MSF878Key.primaryPos.equalTo("0")
				def query2 = new QueryImpl(MSF878Rec.class).and(c1).and(c7).and(c3.and(c4.or(c5)))
				MSF878Rec msf878Rec2 = readFirstEmployeePosition(query2)
				if(msf878Rec2 !=null){
					//* Terminate the previous browse
					//* If the employee's position is the terminated or suspended
					//* position as at the census date but they have been moved into
					//* that position in this period then get their previous position
					if(msf878Rec2.getPrimaryKey().positionId == terminatePos
					|| msf878Rec2.getPrimaryKey().positionId == suspendPos){
						Constraint c8 = MSF878Key.invStrDate.greaterThanEqualTo(revsdRpStrDate)
						Constraint c9 = MSF878Key.posStopDate.lessThanEqualTo(batchParams.paramRefPerEndDate)
						Constraint c10 = MSF878Key.positionId.notEqualTo(terminatePos)
						Constraint c11 = MSF878Key.positionId.notEqualTo(suspendPos)
						def query3 = new QueryImpl(MSF878Rec.class).and(c1).and(c7).and(c3).and(c8).and(c9).and(c10.or(c11))
						MSF878Rec msf878Rec3 = readFirstEmployeePosition(query3)
						if(msf878Rec3 != null){
							posCensus = msf878Rec3.getPrimaryKey().getPositionId()
							processWfpPosCode(posCensus)
						}else{
							if(!posCensus?.trim()){
								extract3L = "-1"
							}
						}
					}else{
						posCensus = msf878Rec2.getPrimaryKey().getPositionId()
						processWfpPosCode(posCensus)
					}
				}else{
					if(!posCensus?.trim()){
						extract3L = "-1"
					}
				}
			}
		}
	}

	/**
	 * Process 3N - Treasury
	 */
	private void getLevel3NWFP(){
		info("getLevel3NWFP")
		String udfChosen3N = assocValNswp.get("3N").toString().padRight(50)
		udfChosen3N = udfChosen3N.substring(6,10)
		extract3N = " "
		if(posId878?.trim()){
			//use service to read position msf870
			try{
				PositionServiceReadRequiredAttributesDTO posReqAtt = new PositionServiceReadRequiredAttributesDTO()
				posReqAtt.returnPosition = true
				posReqAtt.returnPrimRepCode = true
				posReqAtt.returnPositionClass12 = true
				PositionServiceReadReplyDTO posReplyDTO = service.get(SERVICE_POSITION).read({PositionServiceReadRequestDTO it->
					it.position = posId878
					it.requiredAttributes = posReqAtt
				})
				//Primary reporting code - level 2
				if(posReplyDTO != null){
					if(posReplyDTO.primRepCode?.trim()){
						extract3N = "-2"
					}else{
						// Read the asociate record held against the table linked to the
						// UDField held on MSF870
						String assocVal = readTableService(udfChosen3N, posReplyDTO.positionClass12).padRight(50)
						if(assocVal?.trim()){
							if(assocVal.substring(10,11)?.trim()){
								extract3N = assocVal.substring(10,11)
							}else{
								extract3N = "-2"
							}
						}else{
							extract3N = "-2"
							errField = "UDF"
							errValue = udfChosen3N
							errMessages = ERR_POS_REP_CODE
							printErrorMsg()
						}
					}
				}
			}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
				info("Error when read position Service ${e.getMessage()}")
				extract3N = "-2"
			}
		}
	}

	/**
	 * Process 3M - ANZSCO
	 */
	private void getLevel3MWFP(){
		info("getLevel3MWFP")
		extract3M = " "
		if(posId878?.trim()){
			//use service to read position msf870
			try{
				PositionServiceReadRequiredAttributesDTO posReqAtt = new PositionServiceReadRequiredAttributesDTO()
				posReqAtt.returnPosition = true
				posReqAtt.returnPrimRepCode = true
				posReqAtt.returnPositionClass13 = true
				PositionServiceReadReplyDTO posReplyDTO = service.get(SERVICE_POSITION).read({PositionServiceReadRequestDTO it->
					it.position = posId878
					it.requiredAttributes = posReqAtt
				})
				//Primary reporting code - level 2
				if(posReplyDTO != null && posReplyDTO.positionClass13?.trim()){
					// Read the asociate record held against the table linked to the
					// UDField held on MSF870
					String assocVal = readTableService(TABLE_POOC, posId878).padRight(50)
					if(assocVal?.trim()){
						extract3M = assocVal.substring(0,6)
					}else{
						extract3M = "-1"
					}
				}else{
					extract3M = "-1"
				}
			}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
				info("Error when read position Service ${e.getMessage()}")
				extract3M = "-1"
			}
		}else{
			extract3M = "-1"
		}
	}

	/**
	 * Process 3P - Position ID (Substantive Position)
	 */
	private void getLevel3PWFP(){
		info("getLevel3PWFP")
		String termDate = calendarToString(empR.terminationDate)
		String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
		//Default all position fields to all nines
		extract3P = "999999999999999999"

		Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
		Constraint c2 = MSF878Key.primaryPos.equalTo("0")
		Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
		Constraint c4 = MSF878Key.posStopDate.greaterThanEqualTo(batchParams.paramCensusDate)
		Constraint c5 = MSF878Key.posStopDate.equalTo(termDate)
		Constraint c6 = MSF878Key.posStopDate.equalTo("00000000")
		Constraint c7 = MSF878Key.positionId.notEqualTo(terminatePos)
		Constraint c8 = MSF878Key.positionId.notEqualTo(suspendPos)

		if(empClass == "C"){
			extract3P = "-2"
		}else{
			if(empR.persEmpStatus == "X" || empR.persEmpStatus =="Z" &&
			(termDate >= batchParams.paramRefPerStrDate &&
			termDate <= batchParams.paramRefPerEndDate &&
			termDate < batchParams.paramCensusDate &&
			termDate != "00000000" && termDate?.trim())) {
				def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3.and(c4.or(c5)))
				MSF878Rec msf878Rec = readFirstEmployeePosition(query)
				if(msf878Rec != null){
					extract3P = msf878Rec.getPrimaryKey().positionId
				}else{
					extract3P = "-1"
				}
			}else{
				def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3.and(c4.or(c6)).and(c7.or(c8)))
				MSF878Rec msf878Rec = readFirstEmployeePosition(query)
				if(msf878Rec != null){
					extract3P = msf878Rec.getPrimaryKey().positionId
				}else{
					extract3P = "-1"
				}
			}
		}
	}
	/**
	 * Process 3Q - Position ID (Current Position)
	 */
	private void getLevel3QWFP(){
		info("getLevel3PWFQ")
		String termDate = calendarToString(empR.terminationDate)
		String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
		//Default all position fields to all nines
		extract3Q = "999999999999999999"

		Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
		Constraint c2 = MSF878Key.primaryPos.equalTo("0")
		Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
		Constraint c4 = MSF878Key.posStopDate.lessThanEqualTo(batchParams.paramCensusDate)
		Constraint c5 = MSF878Key.posStopDate.equalTo(termDate)
		Constraint c6 = MSF878Key.posStopDate.equalTo("00000000")
		Constraint c7 = MSF878Key.positionId.notEqualTo(terminatePos)
		Constraint c8 = MSF878Key.positionId.notEqualTo(suspendPos)
		Constraint c9 = MSF878Key.posStopDate.greaterThanEqualTo(batchParams.paramCensusDate)

		if(empClass == "C"){
			extract3Q = "-2"
		}else{
			if(empR.persEmpStatus == "X" || empR.persEmpStatus =="Z" &&
			(termDate >= batchParams.paramRefPerStrDate &&
			termDate <= batchParams.paramRefPerEndDate &&
			termDate < batchParams.paramCensusDate &&
			termDate != "00000000" &&
			termDate?.trim())){

				def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3.and(c4.or(c5)))
				MSF878Rec msf878Rec = readFirstEmployeePosition(query)
				if(msf878Rec != null){
					extract3Q = msf878Rec.getPrimaryKey().positionId
				}else{
					extract3Q = "-1"
				}
			}else{
				def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3.and(c9.or(c6)).and(c7.or(c8)))
				MSF878Rec msf878Rec = readFirstEmployeePosition(query)
				if(msf878Rec != null){
					extract3Q = msf878Rec.getPrimaryKey().positionId
				}else{
					extract3Q = "-1"
				}
			}
		}
	}
	/**
	 * Process 3O - Division ID
	 */
	private void getLevel3OWFP(){
		info("getLevel3OWFP")
		if(batchParams.paramAgencyCode?.trim()){
			extract3O = batchParams.paramAgencyCode
		}else{
			extract3O = "-2"
		}
	}
	/**
	 * Process 8A - Census Period FTE
	 */
	private void getLevel8AWFP(){
		info("getLevel8AWFP")
		//8A is equal to the previous Z2
		BigDecimal temp3I
		BigDecimal temp3J
		if(extract3I < 0){
			temp3I = extract3I
			temp3I = temp3I * -1
		}else{
			temp3I = extract3I
		}
		if(extract3J < 0){
			temp3J = extract3J
			temp3J = temp3J * -1
		}else{
			temp3J = extract3J
		}

		extractZ2 = temp3I/(temp3J*2)
	}
	/**
	 * Process 8B - Annual Reference Period FTE
	 */
	private void getLevel8BWFP(){
		info("getLevel8BWFP")
		//8B is equal to the previous Z1
		BigDecimal temp3EA
		BigDecimal temp3J
		BigDecimal temp3KA

		if(extract3EA < 0){
			temp3EA = extract3EA
			temp3EA = temp3EA * -1
		}else{
			temp3EA = extract3EA
		}

		if(extract3J < 0){
			temp3J = extract3J
			temp3J = temp3J * -1
		}else{
			temp3J = extract3J
		}

		temp3KA = extract3KA

		extractZ1 = (temp3EA * 7) / (temp3J * temp3KA)
	}
	/**
	 * Process 8C - Override Census Period FTE
	 */
	private void getLevel8CWFP(){
		info("getLevel8CWFP")
		BigDecimal temp8C

		temp8C = extractZ2
		if(extractZ2 < 0){
			temp8C = temp8C * -1
		}

		extract8C = temp8C < 0 ? "0" : " "
	}
	/**
	 * Process 8d_a Override Annual Reference Period FTE
	 */
	private void getLevel8DWFP(){
		info("getLevel8CWFP")
		BigDecimal temp8D

		temp8D = extractZ1
		if(extractZ1 < 0){
			temp8D = temp8D * -1
		}

		extract8D = temp8D < 0 ? "0" : " "
	}

	/**
	 * read MSF870 using service to get Position reporting code
	 * @param posCensus
	 */
	private void processWfpPosCode(String posCensus){
		info("processWfpPosCode")
		try{
			PositionServiceReadRequiredAttributesDTO posReqAtt = new PositionServiceReadRequiredAttributesDTO()
			posReqAtt.returnPosition = true
			posReqAtt.returnPositionClass14 = true
			posReqAtt.returnPositionClass15 = true
			posReqAtt.returnPositionClass16 = true
			posReqAtt.returnPositionClass17 = true
			posReqAtt.returnPositionClass18 = true
			PositionServiceReadReplyDTO posReplyDTO = service.get(SERVICE_POSITION).read({PositionServiceReadRequestDTO it->
				it.position = posCensus
				it.requiredAttributes = posReqAtt
			})
			if(posReplyDTO != null){
				String posRptCde6 = posReplyDTO.positionClass14.padRight(4)
				String posRptCde7 = posReplyDTO.positionClass15.padRight(4)
				String posRptCde8 = posReplyDTO.positionClass16.padRight(4)
				String posRptCde9 = posReplyDTO.positionClass17.padRight(4)
				String posRptCde10 = posReplyDTO.positionClass18.padRight(4)
				extract3L = posRptCde6.substring(0,1) + posRptCde7.substring(0,1) + posRptCde8.substring(0,1) + posRptCde9.substring(0,2) + posRptCde10.substring(0,2)
			}
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Error when read position Service ${e.getMessage()}")
		}
	}

	/**
	 * Set up the default employee award details
	 * via the settings on the NSWA table.
	 */
	private void getAwardCodeEmp(){
		info("getAwardCodeEmp")
		String assocVal
		Calendar startDate = stringToCalendar(batchParams.paramCensusDate)
		temp4AB  = 0
		temp4C   = 0
		temp4ABT = 0

		EmpAwardServiceReadRequiredAttributesDTO awdReqAtt = new EmpAwardServiceReadRequiredAttributesDTO()
		awdReqAtt.returnAwardCode = true
		try{
			EmpAwardServiceReadReplyDTO empAwardDTO = service.get(SERVICE_AWARD).read({EmpAwardServiceReadRequestDTO it ->
				it.requiredAttributes = awdReqAtt
				it.employee  = empR.employee
				it.startDate = startDate
			})
			info("Award Code: ${ empAwardDTO.awardCode}")
			assocVal = readTableService(TABLE_NSWA, empAwardDTO.awardCode.trim())
			if(assocVal?.trim()){
				nswaEmpLegId = assocVal.substring(0, 4)
				nswaSalAwdId = assocVal.substring(4, 12)
				nswaCondEmpAwd = assocVal.substring(12, 20)
				nswaAgcyOccAwd = assocVal.substring(20, 28)
				nswaOverTime = assocVal.substring(28, 29)
				nswaHighDuties = assocVal.substring(29, 30)
				nswaRecLeave = assocVal.substring(30, 31)
				nswaSickLeave = assocVal.substring(31, 32)
				nswaSickLveCarer = assocVal.substring(32, 33)
				nswaUnpaidSickLve = assocVal.substring(33, 34)
				nswaExtLve = assocVal.substring(34, 35)
				nswaUnpaidLve = assocVal.substring(35, 36)
				nswaMaternityLve = assocVal.substring(36, 37)
				nswaFacsLve = assocVal.substring(37, 38)
				nswaLumpSums = assocVal.substring(38, 39)
				nswaSalaryMaint = assocVal.substring(39, 40)
				nswaSupContrib = assocVal.substring(40, 41)
				nswaRecruitRetAll = assocVal.substring(41, 42)
				nswaDisplacement = assocVal.substring(42, 43)
				nswaIncExcAwd = assocVal.substring(43, 44)

				info("nswaEmpLegId : ${nswaEmpLegId}")
				info("nswaSalAwdId : ${nswaSalAwdId}")
				info("nswaCondEmpAwd : ${nswaCondEmpAwd}")
				info("nswaAgcyOccAwd : ${nswaAgcyOccAwd}")
				info("nswaOverTime : ${nswaOverTime}")
				info("nswaHighDuties : ${nswaHighDuties}")
				info("nswaRecLeave : ${nswaRecLeave}")
				info("nswaSickLeave : ${nswaSickLeave}")
				info("nswaSickLveCarer : ${nswaSickLveCarer}")
				info("nswaUnpaidSickLve : ${nswaUnpaidSickLve}")
				info("nswaExtLve : ${nswaExtLve}")
				info("nswaUnpaidLve : ${nswaUnpaidLve}")
				info("nswaMaternityLve : ${nswaMaternityLve}")
				info("nswaFacsLve : ${nswaFacsLve}")
				info("nswaLumpSums : ${nswaLumpSums}")
				info("nswaSalaryMaint : ${nswaSalaryMaint}")
				info("nswaSupContrib : ${nswaSupContrib}")
				info("nswaRecruitRetAll : ${nswaRecruitRetAll}")
				info("nswaDisplacement : ${nswaDisplacement}")
				info("nswaIncExcAwd : ${nswaIncExcAwd}")

				if (nswaIncExcAwd == "Y"){
					MSF801_C0_801Rec msf801_c0_rec = getAwardCodeDet(empAwardDTO.awardCode)
					if (msf801_c0_rec!=null){
						awdHrsPerPRD = msf801_c0_rec.getStdHrsPrdC0()
						awdPayFrqC0 = msf801_c0_rec.getPayFrqC0()
					}else {
						awdHrsPerPRD = 0
						awdPayFrqC0 = ""
					}
					debug ("award code : ${empAwardDTO.awardCode}")
					debug ("hrs per period : ${awdHrsPerPRD}")
					debug ("pay freq : ${awdPayFrqC0}")

					/*
					 * find the class emp, pay group, contract hours using screen service
					 * since payroll emp service required access 9
					 */

					info("Read Employee Payroll Details")
					try{
						MSF820Rec msf820rec = edoi.findByPrimaryKey(new MSF820Key(empR.employee))
						empClass = msf820rec.getEmployeeClass()
						empContractHrs = msf820rec.getContractHours()
						empPayGroup = msf820rec.getPayGroup()
						exitType = msf820rec.getExitType()
					}
					catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
						info("EmployeePayrollDetail-NotFound-GetAward")
						empClass = ""
						empPayGroup = ""
						empContractHrs = 0
						exitType = ""
					}
					info("emp class : ${empClass}")
					info("emp contract hrs: ${empContractHrs}")
					info("emp pay group: ${empPayGroup}")
					info("emp exit type: ${exitType}")

					/*
					 * Build a table of all the earnings and deductions codes
					 * linked to employee with their report field 5's attached
					 * if no spaces
					 */
					buildEarningsTbl()

					/*
					 * Find out if the employee's total annual remuneration is
					 * an annual amount via MSSRAT and find the rate as of:
					 * the census date
					 */

					MSSRATLINK mssRatlnk = eroi.execute("MSSRAT", {MSSRATLINK mssratlnk ->
						mssratlnk.option = "1"
						mssratlnk.employeeId = empR.employee
						mssratlnk.requiredDate = batchParams.paramCensusDate
					})
					info("error message 1: ${mssRatlnk.ratErrorCode}")
					calcAnlRate = mssRatlnk.calcAnlRate
					baseAnlRate = mssRatlnk.baseAnlRate
					baseFreqType = mssRatlnk.baseFreqType

					temp4AB = calcCompRates(mssRatlnk.compTable)

					/*
					 * and the hire date
					 */
					MSSRATLINK mssRatlnk2 = eroi.execute("MSSRAT", {MSSRATLINK mssratlnk ->
						mssratlnk.option = "1"
						mssratlnk.employeeId = empR.employee
						mssratlnk.requiredDate = calendarToString(empR.hireDate)
					})
					info("error message 2: ${mssRatlnk2.ratErrorCode}")
					baseAnlHdRate = mssRatlnk2.baseAnlRate
					baseFreqHdType = mssRatlnk2.baseFreqType

					temp4C = calcCompRates(mssRatlnk2.compTable)

					hdRateRef = mssRatlnk2.hdRateRef?.trim() ? "Y" : "N"

					/*
					 * terminate date
					 */
					String termDate = calendarToString(empR.terminationDate)
					if(termDate?.trim() && termDate != "00000000"){

						Calendar termCal = Calendar.getInstance()
						termCal.setTime(empR.terminationDate.getTime())
						termCal.add(empR.terminationDate.DATE, -1)

						String tempTermDate = calendarToString(termCal)

						MSSRATLINK mssRatlnk3 = eroi.execute("MSSRAT", {MSSRATLINK mssratlnk ->
							mssratlnk.option = "1"
							mssratlnk.employeeId = empR.employee
							mssratlnk.requiredDate = tempTermDate
						})
						info("error message 3: ${mssRatlnk3.ratErrorCode}")
						bTermAnlRate = mssRatlnk3.baseAnlRate
						temp4ABT = calcCompRates(mssRatlnk3.compTable)
					}

				}else{
					invalidEmp = true
				}
			}else{
				errField = "NSWA"
				errValue = empR.employee
				errMessages = ERR_TABLE_NSWA
				invalidEmp = true
				printErrorMsg()
			}
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot read record for ${SERVICE_AWARD} : ${e.getMessage()}")
			errField = "AWARD CODE"
			errValue = empR.employee
			errMessages = ERR_AWARD_CODE
			invalidEmp = true
			printErrorMsg()
		}
	}

	/**
	 * read table service that returned assoc value
	 * @param tblType
	 * @param tblCode
	 * @return String assocVal
	 */
	private String readTableService(String tblType, String tblCode){
		info("readTableService")
		try{
			TableServiceReadRequiredAttributesDTO tableReadReq = new TableServiceReadRequiredAttributesDTO()
			tableReadReq.returnTableType = true
			tableReadReq.returnTableCode = true
			tableReadReq.returnAssociatedRecord = true
			TableServiceReadReplyDTO tableReadReply = service.get(SERVICE_TABLE).read({ TableServiceReadRequestDTO it ->
				it.requiredAttributes = tableReadReq
				it.tableType = tblType
				it.tableCode = tblCode
			})
			info("assoc Val: ${tableReadReply.associatedRecord}")
			return tableReadReply.associatedRecord
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot Read Table ${tblType} : ${e.getMessage()}")
			return ""
		}
	}

	/**
	 * Find the standard pay period hours and the pay frequency
	 * held against the award.
	 * @param awardCode
	 */
	private MSF801_C0_801Rec getAwardCodeDet(String awardCode){
		info("getAwardCodeDet")
		try{
			MSF801_C0_801Key msf801_C0Key = new MSF801_C0_801Key()
			msf801_C0Key.cntlRecType = "C0"
			msf801_C0Key.cntlKeyRest = awardCode
			MSF801_C0_801Rec msf801Rec = edoi.findByPrimaryKey(msf801_C0Key)
			return msf801Rec
		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
			info("Cannot found award code in table HR MSF801_C0_801")
			return null
		}
	}

	/**
	 * Build a table of all the earnings codes
	 * linked to employee with their report field 5's attached
	 * if no spaces
	 */
	private void buildEarningsTbl(){
		info("buildEarningsTbl")
		int count = 0
		earnList = new ArrayList<EarningTbl>()
		Constraint c1 = MSF823Key.consPayGrp.equalTo(" ")
		Constraint c2 = MSF823Key.employeeId.equalTo(empR.employee)
		Constraint c3 = MSF823Key.earnCode.greaterThanEqualTo(" ")
		Constraint c4 = MSF823Key.primRptCd.greaterThanEqualTo(" ")
		def query = new QueryImpl(MSF823Rec.class).and(c1).and(c2).and(c3).and(c4)

		edoi.search(query) {MSF823Rec msf823Rec->
			count++
			info("counter : ${count}")

			EarningTbl earnTbl = new EarningTbl()
			earnTbl.earnCodeEmp = msf823Rec.getPrimaryKey().getEarnCode()
			earnTbl.earnCodeLpu = msf823Rec.lastPerUnits
			earnTbl.earnCodeCmu = msf823Rec.curMthUnits
			earnTbl.earnCodeCqu = msf823Rec.curQtrUnits
			earnTbl.earnCodeCfu = msf823Rec.curFisUnits
			earnTbl.earnCodeLpa = msf823Rec.lastPerAmtL
			earnTbl.earnCodeCma = msf823Rec.curMthAmtL
			earnTbl.earnCodeCqa = msf823Rec.curQtrAmtL
			earnTbl.earnCodeCfa = msf823Rec.curFisAmtL

			MSF801_A_801Rec hrRec = readHRTable(msf823Rec.getPrimaryKey().getEarnCode())
			if(hrRec) {
				earnTbl.earnCodeRf5 = hrRec.getMiscRptFldAx5()?.trim() ? hrRec.getMiscRptFldAx5() : " "
			} else {
				earnTbl.earnCodeRf5 = " "
			}
			info("earnTbl earn : ${earnTbl.earnCodeEmp}")
			info("earnTbl Rf5 : ${earnTbl.earnCodeRf5}")
			info("earnTbl Cfu : ${earnTbl.earnCodeCfu}")
			//add to array list max 999
			if(count <= 999){
				info("add array list earn code")

				earnList.add(earnTbl)
			}
		}
	}

	/**
	 * Read HR Table based on Earn Code.
	 * @param earnCode Earn Code
	 * @return MSF801_A_801Rec
	 */
	private MSF801_A_801Rec readHRTable(String earnCode) {
		info("readHRTable")
		try{
			MSF801_A_801Key msf801_AKey = new MSF801_A_801Key()
			msf801_AKey.cntlRecType = "A"
			msf801_AKey.cntlKeyRest = "***" + earnCode
			return edoi.findByPrimaryKey(msf801_AKey)

		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
			info("cannot found earning in table HR MSF801_A_801")
			return null
		}
	}

	/**
	 * find out if the employee's total annual remuneration is
	 * an annual amount via MSSRAT and find the rate as of :
	 * the census date
	 */
	private BigDecimal calcCompRates(CompTable[] compTable){
		info("calcCompRates")
		List<CompTable> compTbl = compTable
		DecimalFormat decFormatter = new DecimalFormat("#####.##")
		BigDecimal returnVal = 0

		for(CompTable cTbl : compTbl){
			info("comp code: ${cTbl.getComponentCode()}")
			info("comp paid: ${cTbl.componentPaid}")
			info("comp H Rate: ${decFormatter.format(cTbl.getCompHRate()).toString()}")
			info("comp D Rate: ${decFormatter.format(cTbl.getCompDRate()).toString()}")
			info("comp W Rate: ${decFormatter.format(cTbl.getCompWRate()).toString()}")
			if(cTbl.componentPaid == "Y"){
				returnVal = returnVal + cTbl.compARate
			}
		}
		info("total returnVal : ${returnVal}")

		return returnVal
	}

	/**
	 * move employee id to extract field
	 * move the agency code to the first column in the
	 * extract at the same time
	 */
	private void process1AUniqueId(){
		info("process1AUniqueId")
		/*
		 * Move the employee-id to the extract field.
		 * Move the agency code to the first column in the
		 * extract at the same time.
		 */
		extractAgencyCode = batchParams.paramAgencyCode
		extract1A = empR.employee
	}

	/**
	 * process date of birth 1B
	 */
	private void process1BDateOfBirth(){
		info("process1BDateOfBirth")
		if (calendarToString(empR.birthDate) != '00000000' && calendarToString(empR.birthDate)?.trim()){
			//check if the employee has full or partial privacy on his/her information
			extract1B = (empPrivInd == "F"? "-3" : calendarToString(empR.birthDate))
		}
		else{
			extract1B = (empPrivInd == "F"? "-3" : "-1")
			//write error message in report : 'invalid Birth Date (1B)'
			errField = "Employee"
			errValue = calendarToString(empR.birthDate)
			errMessages = ERR_BIRTH_DATE_1B
			printErrorMsg()
		}
	}

	/**
	 * process 1C - Employee Gender
	 */
	private void process1CGender(){
		info("process1CGender")
		/*
		 * Check if the employee has full or partial privacy on his/her
		 * information
		 */
		if (empPrivInd == "F"){
			extract1C = "-3"
		}else{
			if(empR.gender == "M"){
				extract1C = "1"
			}else if(empR.gender == "F"){
				extract1C = "2"
			}else{
				extract1C = "-1"
				// write error message in report : 'invalid Gender (1C)'
				errField = "Employee"
				errValue = empR.employee
				errMessages = ERR_GENDER_1C
				printErrorMsg()
			}
		}
	}

	/**
	 * process 1D - Employee Location
	 */
	private void process1DLocation(){
		info("process1DLocation")
		String tableChosen1D = assocValNswp.get("1D").toString().substring(0,6)
		String udfChosen1D = assocValNswp.get("1D").toString().substring(6,10)

		if (tableChosen1D == "MSF820"){
			try{
				MSF820Rec msf820rec = edoi.findByPrimaryKey(new MSF820Key(empR.employee))

				info("emp id : ${msf820rec.getPrimaryKey().getEmployeeId()}")
				info("emp Location: ${msf820rec.getPayLocation()}")
				String assocVal = readTableService(udfChosen1D, msf820rec.getPayLocation()).padRight(50)
				if(assocVal?.trim()){
					if(empPrivInd == "F"){
						extract1D = "-3"
					}else{
						extract1D = assocVal.substring(4,8)?.trim()? assocVal.substring(4,8) : "-1"
					}
				}else{
					extract1D = "-1"
					// write error message in report : 'invalid 820 paylocation udf (1D)'
					errField = "UDF"
					errValue = empR.employee
					errMessages = ERR_PAY_LOC820
					printErrorMsg()
				}
			} catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				info("EmployeePayrollDetail-NotFound-1D")
				if(tableChosen1D == "MSF829"){
					Calendar startDate = stringToCalendar(batchParams.paramCensusDate)
					//use service call employee physical location from msf829
					readEmployeeLocation(startDate, udfChosen1D)
				}
			}
		}
	}
	/**
	 * readEmployeeLocation
	 * @param startDate
	 * @param udfChosen1D
	 * @return
	 */
	private readEmployeeLocation(Calendar startDate, String udfChosen1D) {
		info ("readEmployeeLocation")
		try{
			EmpPhysicalLocServiceReadReplyDTO empPhyReplyDTO = readEmpPhysicalLoc(startDate)

			if(empPhyReplyDTO != null && empPhyReplyDTO.physicalLocation?.trim()){
				info("emp id : ${empPhyReplyDTO.employee}")
				info("emp Location: ${empPhyReplyDTO.physicalLocation}")
				String assocVal = readTableService(udfChosen1D, empPhyReplyDTO.physicalLocation).padRight(50)
				if(assocVal?.trim()){
					if (empPrivInd == "F"){
						extract1D = "-3"
					}else{
						extract1D = assocVal.substring(4,8)?.trim()? assocVal.substring(4,8) : "-1"
					}
				}else{
					extract1D = "-1"
					// write error message in report : 'invalid 820 pay location udf (1D)'
					errField = "UDF"
					errValue = empR.employee
					errMessages = ERR_PAY_LOC829
					printErrorMsg()
				}
			}
		} catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot read service ${SERVICE_PHY_LOC} : ${e.getMessage()}")
			extract1D = "-1"
			// write error message in report : '- Employee Not on MSF829 File (1D)'
			errField = "Employee"
			errValue = empR.employee
			errMessages = ERR_EMP_MSF829
			printErrorMsg()
		}
	}
	
	/***
	 * Get employee post code from msf810
	 * */
	private void process1ELocation(){
		info ("process1ELocation")
		String tableChosen1E = assocValNswp.get("1E").toString().substring(0,6)
		
		if (tableChosen1E == "MSF810"){
			try{
				MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(empR.getEmployee()))
				
				if (msf810rec.getResCntry().equals("AU")){
					extract1E = msf810rec.getResZipcode()
				}else{
					extract1E = "9999"
				}
			}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				info (empR.employee + "Not found in MSF810")
				extract1E = "-1"
			}
		}
		
	}
	/**
	 * Read Employee Physical Location
	 * @param startDate
	 * @return
	 */
	private EmpPhysicalLocServiceReadReplyDTO readEmpPhysicalLoc(Calendar startDate) {
		info("readEmpPhysicalLoc")
		EmpPhysicalLocServiceReadRequiredAttributesDTO empPhyReq = new EmpPhysicalLocServiceReadRequiredAttributesDTO()
		empPhyReq.returnEmployee = true
		empPhyReq.returnPhysicalLocation = true

		EmpPhysicalLocServiceReadReplyDTO empPhyReplyDTO = service.get(SERVICE_PHY_LOC).read({EmpPhysicalLocServiceReadRequestDTO it ->
			it.requiredAttributes = empPhyReq
			it.employee = empR.employee
			it.startDate = startDate
		})
		return empPhyReplyDTO
	}

	/**
	 * Process 2A - Aboriginal Person or Torres Strait Islander
	 */
	private void process2AAborigTsiId(){
		info("process2AAborigTsiId")
		String tableChosen2A = assocValNswp.get("2A").toString().substring(0,6)
		String udfChosen2A = assocValNswp.get("2A").toString().substring(6,10)
		String suppress2A = assocValNswp.get("2A").toString().substring(10,11)

		if (tableChosen2A == "MSF760"){
			String udfValue = getPrivacyEmp760(udfChosen2A, empR)
			/*
			 * if the has partial or full privacy indicator
			 * then this field will be suppressed.
			 */
			if (empPrivInd == "Y" ||(empPrivInd == "P" && suppress2A == "Y")){
				extract2A = "-3"
			}else if(!invalidEmp){
				if(!udfValue?.trim()){
					extract2A = "-1"
				}else{
					String assocVal = readTableService(udfChosen2A, udfValue).padRight(50)
					if(assocVal?.trim()){
						extract2A = assocVal.substring(0,2)
					}else{
						extract2A = "-1"
					}
				}
			}else{
				extract2A = "-1"
			}
		}
	}

	/**
	 * Process 2B - Person with disability
	 */
	private void process2BDisabilityId(){
		info("process2BDisabilityId")
		String tableChosen2B = assocValNswp.get("2B").toString().substring(0,6)
		String udfChosen2B = assocValNswp.get("2B").toString().substring(6,10)
		String suppress2B = assocValNswp.get("2B").toString().substring(10,11)

		if (tableChosen2B == "MSF760"){
			String udfValue = getPrivacyEmp760(udfChosen2B, empR)
			/*
			 * if the has partial or full privacy indicator
			 * then this field will be suppressed.
			 */
			if (empPrivInd == "Y" ||(empPrivInd == "P" && suppress2B == "Y")){
				extract2B = "-3"
			}else if(!invalidEmp){
				if(!udfValue?.trim()){
					extract2B = "-1"
				}else{
					String assocVal = readTableService(udfChosen2B, udfValue).padRight(50)
					if(assocVal?.trim()){
						extract2B = assocVal.substring(0,2)
					}else{
						extract2B = "-1"
					}
				}
			}else{
				extract2B = "-1"
			}
		}
	}

	/**
	 * Process 2C - Person from a racial, ethnic
	 * or ethno-religous minority group
	 */
	private void process2CEthnicityId(){
		info("process2CEthnicityId")
		String tableChosen2C = assocValNswp.get("2C").toString().substring(0,6)
		String udfChosen2C = assocValNswp.get("2C").toString().substring(6,10)
		String suppress2C = assocValNswp.get("2C").toString().substring(10,11)

		if (tableChosen2C == "MSF760"){
			String udfValue = getPrivacyEmp760(udfChosen2C, empR)
			/*
			 * if the has partial or full privacy indicator
			 * then this field will be suppressed.
			 */
			if (empPrivInd == "Y" ||(empPrivInd == "P" && suppress2C == "Y")){
				extract2C = "-3"
			}else if(!invalidEmp){
				if(!udfValue?.trim()){
					extract2C = "-1"
				}else{
					String assocVal = readTableService(udfChosen2C, udfValue).padRight(50)
					if(assocVal?.trim()){
						extract2C = assocVal.substring(0,2)
					}else{
						extract2C = "-1"
					}
				}
			}else{
				extract2C = "-1"
			}
		}
	}

	/**
	 * Process 2D - Language First Spoken as a Child
	 */
	private void process2DFirstLanguage(){
		info("process2DFirstLanguage")
		String tableChosen2D = assocValNswp.get("2D").toString().substring(0,6)
		String udfChosen2D = assocValNswp.get("2D").toString().substring(6,10)
		String suppress2D = assocValNswp.get("2D").toString().substring(10,11)

		if (tableChosen2D == "MSF760"){
			String udfValue = getPrivacyEmp760(udfChosen2D, empR)
			/*
			 * if the has partial or full privacy indicator
			 * then this field will be suppressed.
			 */
			if (empPrivInd == "Y" ||(empPrivInd == "P" && suppress2D == "Y")){
				extract2D = "-3"
			}else if(!invalidEmp){
				if(!udfValue?.trim()){
					extract2D = "-1"
				}else{
					String assocVal = readTableService(udfChosen2D, udfValue).padRight(50)
					if(assocVal?.trim()){
						extract2D = assocVal.substring(0,2)
					}else{
						extract2D = "-1"
					}
				}
			}else{
				extract2D = "-1"
			}
		}
	}

	/**
	 * Process 3 - Employing legislation identifier
	 */
	private void process3EmpLegId(){
		info("process3EmpLegId")
		if (nswaEmpLegId != null && nswaEmpLegId?.trim()){
			extract3 = nswaEmpLegId.trim()
		}else{
			extract3 = "-1"
		}
	}

	/**
	 * Process 3A - Award Determining Salary Rate
	 */
	private void process3ASalAwdId(){
		info("process3ASalAwdId")
		if (nswaSalAwdId != null && nswaSalAwdId?.trim()){
			extract3A = nswaSalAwdId
		}else{
			//based on fdd in not applicable set -2
			extract3A = "-2"
		}
	}

	/**
	 * Process 3B - Award Determining Conditions of Employment
	 */
	private void process3BCondEmpAwdId(){
		info("process3BCondEmpAwdId")
		if (nswaCondEmpAwd != null && nswaCondEmpAwd?.trim()){
			extract3B = nswaCondEmpAwd
		}else{
			extract3B = "-2"
		}
	}

	/**
	 * Process 3D - Usual Hours Worked
	 */
	private void process3DEmpUslHrsWkd(){
		info("process3DEmpUslHrsWkd")

		if(empClass == "F"){
			if(awdPayFrqC0 == "W"){
				extract3D = awdHrsPerPRD
			}else if(awdPayFrqC0 == "F"){
				extract3D = (awdHrsPerPRD/2)
			}else if(awdPayFrqC0 == "M"){
				extract3D = ((awdHrsPerPRD*12)/52.25)
			}else{
				extract3D = -1
			}
		}else if(empClass == "P"){
			if(awdPayFrqC0 == "W"){
				extract3D = empContractHrs
			}else if(awdPayFrqC0 == "F"){
				extract3D = (empContractHrs/2)
			}else if(awdPayFrqC0 == "M"){
				extract3D = (empContractHrs*12)/52.25
			}else{
				extract3D = -1
			}
		}else if(empClass == "C"){
			extract3D = -2
		}else{
			extract3D = -1
		}
	}

	/**
	 * process 3EA - Total Number of Hours Paid Annual Reference Period
	 */
	private void process3EAEmpPaidHrsRp(){
		info("process3EAEmpPaidHrsRp")
		String earnCode3EA =  assocValNswp.get("3E_A").toString().padRight(50)
		info("earnCode3EA |${earnCode3EA}|")
		int counter1 = 0
		for(EarningTbl earnTbl : earnList){
			counter1++
			info("earn RF5 : |${earnTbl.earnCodeRf5}|")
			info("earn CFU : |${earnTbl.earnCodeCfu}|")
			info("earn CQU : |${earnTbl.earnCodeCqu}|")
			info("earn CMU : |${earnTbl.earnCodeCmu}|")
			info("earn LPU : |${earnTbl.earnCodeLpu}|")
			info("counter1 : |${counter1}|")
			if(earnTbl.earnCodeRf5.trim().equals("AR")){
				extract3EA = -2
				break
			}
			int n = 0
			int counter2 = 0
			//MSF823    NPBATPSCLEFEHEDMFMHFA
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				counter2++
				String matchEarnCode = earnCode3EA.substring(n,n+2)
				info("match earn code: |${matchEarnCode}|")
				debug("counter2 : ${counter2}")
				if(matchEarnCode?.trim() &&
				matchEarnCode.trim().equals(earnTbl.earnCodeRf5.trim())) {
					earnCodeFnd = true
					String reportType = batchParams.paramReportType.trim().toUpperCase()
					switch(reportType) {
						case "Y":
							extract3EA = extract3EA + earnTbl.earnCodeCfu
							break
						case "M":
							extract3EA = extract3EA + earnTbl.earnCodeCmu
							break
						case "Q":
							extract3EA = extract3EA + earnTbl.earnCodeCqu
							break
						case "P":
							extract3EA = extract3EA + earnTbl.earnCodeLpu
							break
						default:
							extract3EA = extract3EA + 0
							break
					}
					info("extract3ea : |${extract3EA}|")
				} else {
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * process 3F - Employee Overtime Hours Paid in Reference Period
	 */
	private void process3FEmpOtimeHrsRp(){
		info("process3FEmpOtimeHrsRp")
		String earnCode3F =  assocValNswp.get("3F").toString().padRight(50)
		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode3F.substring(n,n+2)

				if(matchEarnCode?.trim() &&
				matchEarnCode.trim().equals(earnTbl.earnCodeRf5.trim())) {
					earnCodeFnd = true
					String reportType = batchParams.paramReportType.trim().toUpperCase()
					info("reportType ${reportType}")
					switch(reportType) {
						case "Y":
							extract3F = extract3F + earnTbl.earnCodeCfu
							break
						case "M":
							extract3F = extract3F + earnTbl.earnCodeCmu
							break
						case "Q":
							extract3F = extract3F + earnTbl.earnCodeCqu
							break
						case "P":
							extract3F = extract3F + earnTbl.earnCodeLpu
							break
						default:
							extract3F = extract3F + 0
							break
					}
					info("extract3F : ${extract3F}")
				} else {
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 3G - Employment Category
	 */
	private void process3GEmployeeCateg(){
		info("process3GEmployeeCateg")
		String tableChosen3G = assocValNswp.get("3G").toString().substring(0,6)
		String udfChosen3G = assocValNswp.get("3G").toString().substring(6,10)
		String staffCateg
		String empType
		String assocVal

		if(udfChosen3G == "STFC"){
			staffCateg = empR.staffCategory
			assocVal = readTableService(udfChosen3G, staffCateg)
		}else if(udfChosen3G =="EMTY"){
			empType = empR.employeeType
			assocVal = readTableService(udfChosen3G, empType)
		}
		if(assocVal?.trim() && assocVal.substring(0,2)?.trim()){
			extract3G = assocVal.substring(0,2)
		}else{
			extract3G = "-1"
			// write error message in report : 'Invalid 760 Category UDF (3G)'
			errField = "UDF"
			errValue = udfChosen3G
			errMessages = ERR_CATEG_3G
			printErrorMsg()
		}
	}

	private void process3HCensusStatus(){
		info("process3HCensusStatus")
		String termDate = calendarToString(empR.terminationDate)
		String hireDate = calendarToString(empR.hireDate)

		if(empClass == "F" || empClass == "P" || empClass =="C"){
			if((termDate >= batchParams.paramRefPerStrDate && termDate <= batchParams.paramRefPerEndDate
			&& termDate < batchParams.paramCensusDate  && termDate != "00000000" && termDate?.trim())
			|| hireDate > batchParams.paramCensusDate){
				extract3H = "2"
			}else if(empClass == "C"){
				Constraint c1 = MSF823Key.consPayGrp.equalTo(empPayGroup)
				Constraint c2 = MSF823Key.employeeId.equalTo(empR.employee)
				Constraint c3 = MSF823Key.earnCode.greaterThanEqualTo(" ")
				def query = new QueryImpl(MSF823Rec.class).and(c1).and(c2).and(c3)
				MSF823Rec msf823Rec = readFirstEmployeeEearning(query)
				if(msf823Rec != null && msf823Rec.lastPerAmtL > 0){
					extract3H = "1"
				}else{
					extract3H = "3"
				}
			}else if(empClass == "F" || empClass == "P"){
				String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
				Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
				Constraint c2 = MSF878Key.primaryPos.equalTo("0")
				Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
				def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3)
				MSF878Rec msf878Rec = readFirstEmployeePosition(query)
				if (msf878Rec != null){
					if(msf878Rec.getPrimaryKey().getPositionId()?.trim()
					&& (msf878Rec.getPrimaryKey().positionId != suspendPos
					|| msf878Rec.getPrimaryKey().positionId != terminatePos)){
						extract3H = "1"
					}else{
						extract3H = "2"
					}
				}else{
					extract3H = "-1"
				}
			}
		}else{
			extract3H = "-1"
		}
	}

	/**
	 * Process 4A - Total Annual Remuneration (Substantive Position)
	 */
	private void process4ATotAnnualRemRp(){
		info("process4ATotAnnualRemRp")
		String termDate = calendarToString(empR.terminationDate)

		if(empClass == "F" || empClass == "P" || empClass =="C"){
			if(empClass =="C"){
				extract4A = -2
			}else{
				if(hdRateRef == "N"){
					String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
					Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
					Constraint c2 = MSF878Key.primaryPos.equalTo("0")
					Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
					def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3)
					MSF878Rec msf878Rec = readFirstEmployeePosition(query)
					if (msf878Rec != null){
						if(msf878Rec.getPrimaryKey().positionId == suspendPos
						|| msf878Rec.getPrimaryKey().positionId == terminatePos){
							if(termDate >= batchParams.paramRefPerStrDate && termDate <= batchParams.paramRefPerEndDate
							&& termDate != "00000000" && termDate?.trim()){
								extract4A = bTermAnlRate
								extract4A = extract4A + temp4ABT
							}
						}else{
							extract4A = baseAnlRate
							extract4A = extract4A + temp4AB
						}
					}else{
						extract4A = -1
					}
				}else{
					extract4A = -1
				}
			}
		}else{
			extract4A = -1
		}
	}

	/**
	 * Process 4B - Total Annual Base Remuneration (Current Position)
	 */
	private void process4BTotAnnualRemCp(){
		info("process4BTotAnnualRemCp")
		String termDate = calendarToString(empR.terminationDate)
		String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()

		if(empClass == "F" || empClass == "P" || empClass =="C"){
			if(empClass =="C"){
				extract4B = -2
			}else{
				Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
				Constraint c2 = MSF878Key.primaryPos.equalTo("0")
				Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdCensusDate)
				def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3)
				MSF878Rec msf878Rec = readFirstEmployeePosition(query)
				if (msf878Rec != null){
					if(msf878Rec.getPrimaryKey().positionId == suspendPos
					|| msf878Rec.getPrimaryKey().positionId == terminatePos){
						if(termDate >= batchParams.paramRefPerStrDate && termDate <= batchParams.paramRefPerEndDate
						&& termDate != "00000000" && termDate?.trim()){
							posId878 = msf878Rec.getPrimaryKey().positionId
						}
					}
				}

				// use edoi msf840 since higher duties service required access level 9
				Constraint cEmp = MSF840Key.employeeId.equalTo(empR.employee)
				Constraint cStartDate = MSF840Key.hdStartDate.lessThanEqualTo(batchParams.paramCensusDate)
				Constraint cEndDate = MSF840Rec.invEndDate.lessThanEqualTo(revsdCensusDate)
				def query1 = new QueryImpl(MSF840Rec.class).and(cEmp).and(cStartDate).and(cEndDate)
				MSF840Rec msf840Rec = readFirstEmployeeHigherDuties(query1)
				if(msf840Rec != null){
					if(msf840Rec.getRateRefCode()?.trim()){
						MSSRATLINK mssRatlnk = eroi.execute("MSSRAT", {MSSRATLINK mssratlnk ->
							mssratlnk.option = "1"
							mssratlnk.employeeId = empR.employee
							mssratlnk.requiredDate = batchParams.paramCensusDate
							mssratlnk.hdRateRef = msf840Rec.rateRefCode
						})
						temp4AB = calcCompRates(mssRatlnk.compTable)
						extract4B = extract4B + temp4AB

					}else{
						// get the HD amount from MSSRAT
						if(msf840Rec.salaryAmount > 0){
							if(termDate >= batchParams.paramRefPerStrDate && termDate <= batchParams.paramRefPerEndDate
							&& termDate != "00000000" && termDate?.trim()
							&&(posId878 == suspendPos || posId878 == terminatePos)){
								extract4B = extract4A
							}else{
								extract4B = msf840Rec.salaryAmount
							}
						}else{
							extract4B = baseAnlRate
							extract4B = temp4AB + extract4B
						}
					}
				}else{
					extract4B = baseAnlRate
					extract4B = temp4AB + extract4B
				}
			}
		}else{
			extract4B = -1
		}
	}

	/**
	 * Process 4C - Recruitment Remuneration
	 */
	private void process4CRecruitRemun(){
		info("process4CRecruitRemun")
		String termDate = calendarToString(empR.terminationDate)
		String revsdHireDate = (99999999 - calendarToString(empR.hireDate).toLong()).toString()

		if(empClass == "F" || empClass == "P" || empClass =="C"){
			if(empClass =="C"){
				extract4C = -2
			}else{
				if(termDate >= batchParams.paramRefPerStrDate && termDate <= batchParams.paramRefPerEndDate){
					Constraint cEmp = MSF830Key.employeeId.equalTo(empR.employee)
					Constraint cinvDate = MSF830Key.invStrDate.equalTo(revsdHireDate)
					def query = new QueryImpl(MSF830Rec.class).and(cEmp).and(cinvDate)
					MSF830Rec msf830Rec = edoi.firstRow(query)
					if(msf830Rec != null){
						if (baseAnlHdRate == 0){
							extract4C = msf830Rec.salaryAmount
						}else{
							extract4C = baseAnlHdRate
							extract4C = temp4C + extract4C
						}
					}else{
						extract4C = -1
					}
				}else{
					extract4C = -2
				}
			}
		}else{
			extract4C = -1
		}
	}

	/**
	 * Process 4D - Total Gross Earnings in Reference Period
	 */
	private void process4DTotGrossEarnRp(){
		info("process4DTotGrossEarnRp")
		String earnCode4D =  assocValNswp.get("4D").toString().padRight(50)
		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4D.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4D = extract4D + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4D = extract4D + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4D = extract4D + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "P"){
						extract4D = extract4D + earnTbl.earnCodeLpa
					}else{
						extract4D = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 4E - Total Gross Earnings for Census Pay Period
	 */
	private void process4ETotGrossEarnCp(){
		info("process4ETotGrossEarnCp")
		String earnCode4E =  assocValNswp.get("4E").toString().padRight(50)
		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4E.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					extract4E = extract4E + earnTbl.earnCodeLpa
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 4G - Actual Earnings Overtime Payments
	 */
	private void process4GActualOtimePmnts(){
		info("process4GActualOtimePmnts")
		String earnCode4G =  assocValNswp.get("4G").toString().padRight(50)
		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4G.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4G = extract4G + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4G = extract4G + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4G = extract4G + earnTbl.earnCodeCqa
					}else if(batchParams.paramReportType == "P"){
						extract4G = extract4G + earnTbl.earnCodeLpa
					}else{
						extract4G = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * process 6C - Movement Type
	 */
	private void process6CEmpMovement(){
		info("process6CEmpMovement")
		String termDate = calendarToString(empR.terminationDate)
		String hireDate = calendarToString(empR.hireDate)
		String revsdRpStrDate = (99999999 - batchParams.paramRefPerStrDate.toLong()).toString()
		String revsdRpEndDate = (99999999 - batchParams.paramRefPerEndDate.toLong()).toString()

		try{
			//use edoi since there is no service for employee tax details
			MSF824Key msf824Key = new MSF824Key()
			msf824Key.employeeId = empR.employee
			msf824Key.consPayGrp = "   "
			MSF824Rec msf824Rec = edoi.findByPrimaryKey(msf824Key)
			if(msf824Rec != null){
				if(msf824Rec.reinstDate >= batchParams.paramRefPerStrDate
				&& msf824Rec.reinstDate <= batchParams.paramRefPerStrDate
				&& termDate != "00000000" && termDate?.trim()
				&& msf824Rec.reinstDate >= termDate
				&& empR.persEmpStatus != "Z"){
					extract6C1 = "2"
				}else{
					if(termDate >= batchParams.paramRefPerStrDate
					&& termDate <= batchParams.paramRefPerEndDate){
						extract6C1 = "3"
					}else{
						if(hireDate >= batchParams.paramRefPerStrDate
						&& hireDate <= batchParams.paramRefPerEndDate){
							extract6C1 = "2"
						}
					}
				}
			}
		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
			extract6C1 = " "
		}

		if(!extract6C1?.trim()){
			Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
			Constraint c2 = MSF878Key.primaryPos.equalTo("0")
			Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdRpEndDate)
			Constraint c4 = MSF878Key.invStrDate.lessThanEqualTo(revsdRpStrDate)

			def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3).and(c4)
			MSF878Rec msf878Rec = readFirstEmployeePosition(query)
			if(msf878Rec != null){
				if(msf878Rec.getChangeReason()?.trim()){
					String assocVal = readTableService(TABLE_TFRR, msf878Rec.changeReason).padRight(50)
					if(assocVal?.trim()){
						if(assocVal.substring(17,18) == "2"
						|| assocVal.substring(17,18) == "3"){
							extract6C1 = assocVal.substring(17,18)
						}else{
							extract6C1 = "1"
						}
					}else{
						extract6C1 = "1"
						extract6C2 = "03"
					}
				}else{
					extract6C1 = "1"
					extract6C2 = "00"
				}
			}else{
				extract6C1 = "1"
				extract6C2 = "00"
			}
		}
	}

	private void process6CSeparationType(){
		info("process6CSeparationType")
		String termDate = calendarToString(empR.terminationDate)
		String revsdTempDate

		if((empClass == "F" || empClass == "P")
		&& (termDate >= batchParams.paramRefPerStrDate
		&& termDate <= batchParams.paramRefPerEndDate)){

			Calendar termCal = Calendar.getInstance()
			termCal.setTime(empR.terminationDate.getTime())
			termCal.add(Calendar.DAY_OF_MONTH, 1)
			info("term date: ${termDate}")

			revsdTempDate = (99999999- calendarToString(termCal).toLong()).toString()
			info("temp date: ${revsdTempDate}")

			Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
			Constraint c2 = MSF878Key.primaryPos.equalTo("0")
			Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdTempDate)

			def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3)
			MSF878Rec msf878Rec = readFirstEmployeePosition(query)
			if(msf878Rec != null){
				if(msf878Rec.getPrimaryKey().getPositionId()?.trim()
				&& msf878Rec.getChangeReason()?.trim()){
					//* Check first for exit type on EXTY table
					if(exitType?.trim()){
						String assocVal = readTableService(TABLE_EXTY, exitType).padRight(50)
						if(assocVal?.trim() && assocVal.substring(6,8)?.trim()){
							extract6C2 = assocVal.substring(6,8)
						}
					}

					if(!extract6C2?.trim()){
						String assocVal = readTableService(TABLE_TFRR, exitType).padRight(50)
						if(assocVal?.trim() && assocVal.substring(8,10)?.trim()){
							extract6C2 = assocVal.substring(8,10)
						}
					}
				}
			}

		}
	}

	/**
	 * Process 4H - Actual Earnings - Recreation Leave Lump Sum Payout
	 */
	private void process4HActualLrlsPmnts(){
		info("process4HActualLrlsPmnts")
		String earnCode4H =  assocValNswp.get("4H").toString().padRight(50)
		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4H.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4H = extract4H + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4H = extract4H + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4H = extract4H + earnTbl.earnCodeCqa
					}else if(batchParams.paramReportType == "P"){
						extract4H = extract4H + earnTbl.earnCodeLpa
					}else{
						extract4H = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 4I - Actual Voluntary Red. Lump Sum Pymnts (ref period)
	 */
	private void process4IActualVrlsPmnts(){
		info("process4IActualVrlsPmnts")
		String earnCode4I =  assocValNswp.get("4I").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4I.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4I = extract4I + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4I = extract4I + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4I = extract4I + earnTbl.earnCodeCqa
					}else if(batchParams.paramReportType == "P"){
						extract4I = extract4I + earnTbl.earnCodeLpa
					}else{
						extract4I = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}

	}

	/**
	 * process 4J - Actual Earnings . Extended Leave Lump Sum Payout
	 */
	private void process4JActualLelsPmnts(){
		info("process4JActualLelsPmnts")

		String earnCode4J =  assocValNswp.get("4J").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4J.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4J = extract4J + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4J = extract4J + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4J = extract4J + earnTbl.earnCodeCqa
					}else if(batchParams.paramReportType == "P"){
						extract4J = extract4J + earnTbl.earnCodeLpa
					}else{
						extract4J = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * 4K - Actual Earnings - Recruitment & Retention and Skills Shortage Allowance
	 */
	private void process4KActualRcrtRsAllow(){
		info("process4KActualRcrtRsAllow")

		String earnCode4K =  assocValNswp.get("4K").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4K.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4K = extract4K + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4K = extract4K + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4K = extract4K + earnTbl.earnCodeCqa
					}else if(batchParams.paramReportType == "P"){
						extract4K = extract4K + earnTbl.earnCodeLpa
					}else{
						extract4K = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 4L A - Salary Maintenance
	 */
	private void process4LActualSalMaintRsA(){
		info("process4LActualSalMaintRs")
		String earnCode4L =  assocValNswp.get("4L").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode4L.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract4L = extract4L + earnTbl.earnCodeCfa
					}else if(batchParams.paramReportType == "M"){
						extract4L = extract4L + earnTbl.earnCodeCma
					}else if(batchParams.paramReportType == "Q"){
						extract4L = extract4L + earnTbl.earnCodeCqa
					}else if(batchParams.paramReportType == "P"){
						extract4L = extract4L + earnTbl.earnCodeLpa
					}else{
						extract4L = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 4L B - Salary Maintenance
	 * Program now reads every posiotn until end od 878 file, and if
	 * at least one 'Y' is found, then output a '1' to the extract,
	 * otherwise, output a '2'. 
	 * @return boolean salMaintYes
	 */
	private boolean process4LActualSalMaintRsB(){
		info("process4LActualSalMaintRsB")
		//        * Program now reads every posiotn until end od 878 file, and if
		//        * at least one 'Y' is found, then output a '1' to the extract,
		//        * otherwise, output a '2'.
		String revsdRpStrDate = (99999999 - batchParams.paramRefPerStrDate.toLong()).toString()
		String revsdRpEndDate = (99999999 - batchParams.paramRefPerEndDate.toLong()).toString()
		Boolean salMaintYes = false

		Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
		Constraint c2 = MSF878Key.primaryPos.equalTo("0")
		Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdRpEndDate)
		Constraint c4 = MSF878Key.invStrDate.lessThanEqualTo(revsdRpStrDate)

		def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3).and(c4)
		MSF878Rec msf878Rec = readFirstEmployeePosition(query)
		if(msf878Rec != null){
			hisPosFnd = true
			if(msf878Rec.getChangeReason()?.trim()){
				String assocVal = readTableService(TABLE_TFRR, msf878Rec.changeReason).padRight(50)
				if(assocVal?.trim() && assocVal.substring(12,13) == "Y"){
					salMaintYes = true
				}
			}
		}
		return salMaintYes
	}

	/**
	 * Process 4L - Salary maintenance reason SMM
	 */
	private void process4LActualSalMaintRsC(){
		info("process4LActualSalMaintRsC")
		String revsdRpStrDate = (99999999 - batchParams.paramRefPerStrDate.toLong()).toString()
		String revsdRpEndDate = (99999999 - batchParams.paramRefPerEndDate.toLong()).toString()

		Constraint cEmp = MSF830Key.employeeId.equalTo(empR.employee)
		Constraint cendDt = MSF830Key.invStrDate.greaterThanEqualTo(revsdRpEndDate)
		Constraint cstrDt = MSF830Key.invStrDate.lessThanEqualTo(revsdRpStrDate)
		def query = new QueryImpl(MSF830Rec.class).and(cEmp).and(cendDt).and(cstrDt)
		MSF830Rec msf830Rec = (MSF830Rec) edoi.firstRow(query)
		if(msf830Rec != null && msf830Rec.getChangeReason().trim()){
			String assocVal = readTableService(TABLE_TFRR, msf830Rec.changeReason).padRight(50)
			if(assocVal.substring(12,13) == "Y"){
				extract4L = 1
			}else{
				extract4L = 2
			}
		}else{
			if(empPrivInd == "F"){
				extract4L = -3
			}else{
				extract4L = 2
			}
		}
	}

	/**
	 * process 5A - Recreation Leave Accrued as at Census Date
	 */
	private void process5ARecLveAccrdCd(){
		info("process5ARecLveAccrdCd")
		String termDate = calendarToString(empR.terminationDate)
		String recLveType1 = batchParams.paramRecLveType1.padRight(2)
		String recLveType2 = batchParams.paramRecLveType2.padRight(2)
		info("rectype1 -1 : ${recLveType1.substring(0,1)}")
		info("rectype1 -2 : ${recLveType1.substring(1,2)}")
		info("terminate date : ${termDate}")
		info("empClass : ${empClass}")
		BigDecimal lveTypeDiv = 0
		if(empClass =="F" || empClass =="P" || empClass == "C"){
			if(empClass == "C" || (termDate < batchParams.paramCensusDate
			&& termDate != "00000000" && termDate?.trim())){
				extract5A = -2
			}else{
				// Accumulate leave from first recreation type entered.
				// use edoi since leave type doesn't have service
				MSF880Rec msf880Rec = readEmployeLeaveBalance(empR.employee,
						recLveType1.substring(0,1))
				if(msf880Rec != null){
					lveTypefnd = true
					if(recLveType1.substring(1,2) == "H"){
						lveTypeDiv = 2
					}else{
						lveTypeDiv = 1
					}
					extract5A = ((msf880Rec.lveStBalance + msf880Rec.lveManAdj +
							msf880Rec.lveAccrued) - msf880Rec.leaveTaken)/lveTypeDiv
				}
				// Accumulate leave from second recreation type entered.
				// Add on to first rec type leave balance.
				MSF880Rec msf880Rec2 = readEmployeLeaveBalance(empR.employee,
						recLveType2.substring(0,1))
				if(msf880Rec2 != null){
					lveTypefnd = true
					lveTypeDiv = 0
					if(recLveType2.substring(1,2) == "H"){
						lveTypeDiv =2
					}else{
						lveTypeDiv =1
					}
					extract5A = extract5A + ((msf880Rec2.lveStBalance + msf880Rec2.lveManAdj +
							msf880Rec2.lveAccrued - msf880Rec2.leaveTaken)/lveTypeDiv)
				}
			}
		}else{
			extract5A = -1
		}
	}

	/**
	 * Read Employee Leave Balance
	 * @param empId employee id
	 * @param leaveType leave type
	 * @return MSF880Rec
	 */
	private MSF880Rec readEmployeLeaveBalance(String empId, String leaveType) {
		info("readEmployeLeaveBalance")
		try{
			MSF880Key msf880Key  = new MSF880Key()
			msf880Key.employeeId = empId
			msf880Key.leaveType  = leaveType
			return edoi.findByPrimaryKey(msf880Key)
		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
			info("cannot read MSF880Rec leaveType ${leaveType} : ${e.message}")
			return null
		}
	}

	/**
	 * 5B - Recreation Leave Taken (reference Period)
	 */
	private void process5BRecLeaveTakenRp(){
		info("process5BRecLeaveTakenRp")
		String earnCode5B =  assocValNswp.get("5B").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5B.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5B = extract5B + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5B = extract5B + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5B = extract5B + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5B = extract5B + earnTbl.earnCodeLpu
					}else{
						extract5B = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5C - Sick Leave Accrued as at Census Date
	 */
	private void process5CSickLveAccrdCd(){
		info("process5CSickLveAccrdCd")
		String termDate = calendarToString(empR.terminationDate)
		String sickLveType1 = batchParams.paramSicLveType1.padRight(2)
		String sickLveType2 = batchParams.paramSicLveType2.padRight(2)
		BigDecimal lveTypeDiv

		if(empClass =="F" || empClass =="P" || empClass == "C"){
			if(empClass == "C" || (termDate < batchParams.paramCensusDate
			&& termDate != "00000000" && termDate?.trim())){
				extract5C = -2
			}else{
				// Accumulate leave from first sick type entered.
				MSF880Rec msf880Rec = readEmployeLeaveBalance(empR.employee,
						sickLveType1.substring(0,1))
				if(msf880Rec != null){
					lveTypefnd = true
					lveTypeDiv = 0
					if(sickLveType1.substring(1,2) == "H"){
						lveTypeDiv = 2
					}else{
						lveTypeDiv = 1
					}
					extract5C = ((msf880Rec.lveStBalance + msf880Rec.lveManAdj +
							msf880Rec.lveAccrued) - msf880Rec.leaveTaken)/lveTypeDiv
				}

				// Accumulate leave from second recreation type entered.
				// Add on to first rec type leave balance.
				MSF880Rec msf880Rec2 = readEmployeLeaveBalance(empR.employee,
						sickLveType2.substring(0,1))
				if(msf880Rec2 != null){
					lveTypefnd = true
					lveTypeDiv = 0
					if(sickLveType2.substring(1,2) == "H"){
						lveTypeDiv =2
					}else{
						lveTypeDiv =1
					}
					extract5C = extract5C + ((msf880Rec2.lveStBalance + msf880Rec2.lveManAdj +
							msf880Rec2.lveAccrued - msf880Rec2.leaveTaken)/lveTypeDiv)
				}
			}
		}else{
			extract5C = -1
		}
	}

	/**
	 * Process 5DA - Paid Sick Leave Taken during the annual reference period 
	 */
	private void process5DASickLeaveTakenRp(){
		info("process5DASickLeaveTakenRp")
		String earnCode5DA =  assocValNswp.get("5D_A").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5DA.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5DA = extract5DA + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5DA = extract5DA + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5DA = extract5DA + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5DA = extract5DA + earnTbl.earnCodeLpu
					}else{
						extract5DA = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * process 5E - Unpaid Sick Leave Taken (reference Period)
	 */
	private void process5EUnpaidSickLeaveRp(){
		info("process5EUnpaidSickLeaveRp")
		String earnCode5E =  assocValNswp.get("5E").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5E.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5E = extract5E + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5E = extract5E + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5E = extract5E + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5E = extract5E + earnTbl.earnCodeLpu
					}else{
						extract5E = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5F - Paid Sick Leave Taken as carer's leave (reference Period)
	 */
	private void process5FPaidSlCarerRp(){
		info("process5FPaidSlCarerRp")
		String earnCode5F =  assocValNswp.get("5F").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5F.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5F = extract5F + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5F = extract5F + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5F = extract5F + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5F = extract5F + earnTbl.earnCodeLpu
					}else{
						extract5F = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * process 5G - Long Service Leave Accrued as at Census Date
	 */
	private void process5GLongServLveAccrdCd(){
		info("process5GLongServLveAccrdCd")
		String termDate = calendarToString(empR.terminationDate)
		String servDate = calendarToString(empR.serviceDate)
		String extLveType1 = batchParams.paramExtLveType1.padRight(2)
		String extLveType2 = batchParams.paramExtLveType2.padRight(2)
		BigDecimal lveTypeDiv
		BigDecimal lsLveAccrued
		BigDecimal yearDifference
		BigDecimal dtDiff

		if(empClass =="F" || empClass =="P" || empClass == "C"){
			if(empClass == "C" || (termDate < batchParams.paramCensusDate
			&& termDate != "00000000" && termDate?.trim())){
				extract5G = -2
			}else{
				// Accumulate leave from first extended leave type entered.
				MSF880Rec msf880Rec = readEmployeLeaveBalance(empR.employee,
						extLveType1.substring(0,1))
				if(msf880Rec != null){
					lveTypefnd = true
					// calculate the number of years between the service date
					// and the census date. If >=5 add the leave accrued to the
					// total balance.
					lsLveAccrued = 0
					yearDifference = 0
					dtDiff = 0

					yearDifference = (batchParams.paramCensusDate.substring(0,4).toLong() -
							servDate.substring(0,4).toLong()).toBigDecimal()
					if(yearDifference >= 5){
						lsLveAccrued = msf880Rec.lveAccrued
					}else{
						lsLveAccrued = 0
					}
					lveTypeDiv = 0
					if(extLveType1.substring(1,2) == "H"){
						lveTypeDiv = 2
					}else{
						lveTypeDiv = 1
					}
					extract5G = extract5G + ((msf880Rec.lveStBalance + msf880Rec.lveManAdj +
							lsLveAccrued - msf880Rec.leaveTaken)/lveTypeDiv)
				}

				// Accumulate leave from second recreation type entered.
				// Add on to first rec type leave balance.
				MSF880Rec msf880Rec2 = readEmployeLeaveBalance(empR.employee,
						extLveType2.substring(0,1))
				if(msf880Rec2 != null){
					lveTypefnd = true
					// calculate the number of years between the service date
					// and the census date. If >=5 add the leave accrued to the
					// total balance.
					lsLveAccrued = 0
					yearDifference = 0
					dtDiff = 0
					yearDifference = (batchParams.paramCensusDate.substring(0,4).toLong() -
							servDate.substring(0,4).toLong()).toBigDecimal()
					if(yearDifference >= 5){
						lsLveAccrued = msf880Rec2.lveAccrued
					}else{
						lsLveAccrued = 0
					}
					lveTypeDiv = 0
					if(extLveType2.substring(1,2) == "H"){
						lveTypeDiv = 2
					}else{
						lveTypeDiv = 1
					}
					extract5G = extract5G + ((msf880Rec2.lveStBalance + msf880Rec2.lveManAdj +
							lsLveAccrued - msf880Rec2.leaveTaken)/lveTypeDiv)
				}
			}
		}else{
			extract5G = -1
		}
	}

	/**
	 * Process 5H - Extended Leave Taken on Full Pay (reference Period)
	 */
	private void process5HExtLveFpRp(){
		info("process5HExtLveFpRp")
		String earnCode5H =  assocValNswp.get("5H").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5H.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5H = extract5H + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5H = extract5H + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5H = extract5H + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5H = extract5H + earnTbl.earnCodeLpu
					}else{
						extract5H = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5I - Extended Leave Taken on Half Pay (reference Period)
	 */
	private void process5IExtLveHpRp(){
		info("process5IExtLveHpRp")
		String earnCode5I =  assocValNswp.get("5I").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5I.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5I = extract5I + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5I = extract5I + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5I = extract5I + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5I = extract5I + earnTbl.earnCodeLpu
					}else{
						extract5I = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5J - Maternity Leave Taken on Full Pay (reference Period)
	 */
	private void process5JMatLveFpRp(){
		info("process5JMatLveFpRp")
		String earnCode5J =  assocValNswp.get("5J").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5J.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5J = extract5J + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5J = extract5J + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5J = extract5J + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5J = extract5J + earnTbl.earnCodeLpu
					}else{
						extract5J = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5K - Maternity Leave Taken on Half Pay (reference Period)
	 */
	private void process5KMatLveHpRp(){
		info("process5KMatLveHpRp")
		String earnCode5K =  assocValNswp.get("5K").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5K.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5K = extract5K + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5K = extract5K + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5K = extract5K + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5K = extract5K + earnTbl.earnCodeLpu
					}else{
						extract5K = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5L - Unpaid Maternity Leave Taken(reference Period)
	 */
	private void process5LUnpaidMatLveRp(){
		info("process5LUnpaidMatLveRp")
		String earnCode5L =  assocValNswp.get("5L").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5L.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5L = extract5L + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5L = extract5L + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5L = extract5L + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5L = extract5L + earnTbl.earnCodeLpu
					}else{
						extract5L = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5M - Family and Community Services Leave Taken(reference Period)
	 */
	private void process5MFamComSrvLveRp(){
		info("process5MFamComSrvLveRp")
		String earnCode5M =  assocValNswp.get("5M").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5M.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5M = extract5M + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5M = extract5M + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5M = extract5M + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5M = extract5M + earnTbl.earnCodeLpu
					}else{
						extract5M = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5N - Unpaid Leave Taken(reference Period)
	 */
	private void process5NUnpaidLveRp(){
		info("process5NUnpaidLveRp")
		String earnCode5N =  assocValNswp.get("5N").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5N.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5N = extract5N + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5N = extract5N + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5N = extract5N + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5N = extract5N + earnTbl.earnCodeLpu
					}else{
						extract5N = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5O - Extended Leave Taken at Double Pay (Reference Period)
	 */
	private void process5OExtLveDblPayRp(){
		info("process5OExtLveDblPayRp")
		String earnCode5O =  assocValNswp.get("5O").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5O.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5O = extract5O + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5O = extract5O + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5O = extract5O + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5O = extract5O + earnTbl.earnCodeLpu
					}else{
						extract5O = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5P - Commonwealth Paid Parental Leave
	 */
	private void process5PComPaidParLveRp(){
		info("process5PComPaidParLveRp")
		String earnCode5P =  assocValNswp.get("5P").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5P.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * Process 5Q - Special Leave Taken During the Reference Period
	 * */
	private void process5QSpecialLeaveTaken(){
		
		info("process5QSpecialLeaveTaken")
		String earnCode5Q =  assocValNswp.get("5Q").toString().padRight(50)

		for(EarningTbl earnTbl : earnList){
			int n = 0
			for(int i = 0; i < 20; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode5Q.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					if(batchParams.paramReportType == "Y"){
						extract5Q = extract5Q + earnTbl.earnCodeCfu
					}else if(batchParams.paramReportType == "M"){
						extract5Q = extract5Q + earnTbl.earnCodeCmu
					}else if(batchParams.paramReportType == "Q"){
						extract5Q = extract5Q + earnTbl.earnCodeCqu
					}else if(batchParams.paramReportType == "P"){
						extract5Q = extract5Q + earnTbl.earnCodeLpu
					}else{
						extract5Q = 0
					}
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
		
	}
	/**
	 * Process 6A - Date of Most recent Public Sector Entry
	 */
	private void process6AMostRecentPs(){
		info("process6AMostRecentPs")
		String hireDate = calendarToString(empR.hireDate)
		String servDate = calendarToString(empR.serviceDate)
		if(servDate < hireDate){
			extract6A = servDate
		}else{
			extract6A = hireDate
		}
	}

	/**
	 * process 6D - Date of Separation
	 */
	private void process6DDateOfSeparation(){
		info("process6DDateOfSeparation")
		String termDate = calendarToString(empR.terminationDate)

		if(termDate < batchParams.paramRefPerStrDate || termDate > batchParams.paramRefPerEndDate){
			extract6D = "-2"
		}else{
			extract6D = termDate
		}
	}

	/**
	 * Process 6E - Displaced Employees
	 */
	private void process6EDisplacedEmployee(){
		info("process6EDisplacedEmployee")
		String tableChosen6E = assocValNswp.get("6E").toString().substring(0,6)
		String udfChosen6E = assocValNswp.get("6E").toString().substring(6,10)

		if(empPrivInd == "F"){
			extract6E = "-3"
		}else{
			if(empClass == "F" || empClass == "P" || empClass == "C"){
				if(empClass == "C" || nswaDisplacement != "Y"){
					extract6E = "-2"
				}else{
					if(tableChosen6E == "MSF878"){
						hisPosFnd = false
						dispEmpValueFnd = false
						process6E878Browse()
						if(hisPosFnd){
							if(!dispEmpValueFnd){
								extract6E = "3"
							}
						}else{
							if(nswaDisplacement == "Y"){
								extract6E ="3"
							}else{
								extract6E = "-1"
							}
						}
					}
					if(tableChosen6E == "MSF760"){
						String assocVal = readTableService(TABLE_EXTY, empR.employeeType).padRight(50)
						if(assocVal?.trim() && assocVal.substring(3,4)?.trim()){
							extract6E = assocVal.substring(3,4)
						}else{
							if(nswaDisplacement == "Y"){
								extract6E = "3"
							}else{
								extract6E = "-1"
							}
						}
					}
				}
			}else{
				extract6E = "-1"
			}
		}
	}

	/**
	 * Browse 878 until at least one value founds attached to the change reason.
	 */
	private void process6E878Browse(){
		info("process6E878Browse")
		String revsdRpStrDate = (99999999 - batchParams.paramRefPerStrDate.toLong()).toString()
		String revsdRpEndDate = (99999999 - batchParams.paramRefPerEndDate.toLong()).toString()
		//Browse 878 until at least one value founds attached to the change reason.
		Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
		Constraint c2 = MSF878Key.primaryPos.equalTo("0")
		Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdRpEndDate)
		Constraint c4 = MSF878Key.invStrDate.lessThanEqualTo(revsdRpStrDate)
		Constraint c5 = MSF878Rec.changeReason.greaterThan(" ")

		def query = new QueryImpl(MSF878Rec.class).
				and(MSF878Key.positionId.greaterThanEqualTo(" ")).
				and(c1).
				and(c2).
				and(c3).
				and(c4).
				and(MSF878Key.posStopDate.greaterThanEqualTo(" ")).
				and(c5)
		edoi.search(query){ MSF878Rec msf878Rec->
			if(msf878Rec.getPrimaryKey().getPositionId()?.trim()){
				hisPosFnd = true
				if(msf878Rec.getChangeReason()?.trim()){
					String assocVal = readTableService(TABLE_TFRR, msf878Rec.changeReason).padRight(50)
					if(assocVal?.trim() && assocVal.substring(12,13)?.trim() && assocVal.substring(12,13) != "0"){
						dispEmpValueFnd = true
						extract6E = assocVal.substring(12,13)
					}
				}
			}
		}
	}

	/**
	 * process 7A - Sub Agency Code
	 */
	private void process7ASubAgyCode(){
		info("process7ASubAgyCode")
		String termDate = calendarToString(empR.terminationDate)
		String revsdCensusDate = (99999999 - batchParams.paramCensusDate.toLong()).toString()
		String revsdTempDate

		if(termDate?.trim() && termDate != "00000000"){
			Calendar termCal = Calendar.getInstance()
			termCal.setTime(empR.terminationDate.getTime())
			termCal.add(Calendar.DAY_OF_MONTH, 1)

			String tempTermDate = calendarToString(termCal)
			revsdTempDate = (99999999 - tempTermDate.toLong()).toString()
		}else{
			revsdTempDate = revsdCensusDate
		}

		Constraint c1 = MSF878Key.employeeId.equalTo(empR.employee)
		Constraint c2 = MSF878Key.primaryPos.equalTo("0")
		Constraint c3 = MSF878Key.invStrDate.greaterThanEqualTo(revsdTempDate)
		def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3)
		MSF878Rec msf878Rec = readFirstEmployeePosition(query)

		if(msf878Rec != null && msf878Rec.getPrimaryKey().getPositionId()?.trim()){
			posId878 = msf878Rec.getPrimaryKey().positionId

			MSF826Rec msf826Rec = readEmployeeCosting(empR.employee)
			if(msf826Rec && msf826Rec?.getCstAccCodex1()?.trim()) {
				extract7A = msf826Rec.getCstAccCodex1()
			} else {
				MSF874Rec msf874Rec = readPositionCosting(posId878)
				if(msf874Rec && msf874Rec?.getCstAccCodex1()?.trim()) {
					extract7A = msf874Rec.getCstAccCodex1()
				} else {
					extract7A = "-1"
				}
			}
		}

		if(!extract7A?.trim()) {
			extract7A = "-1"
		}
	}

	/**
	 * Read Employee Costing based on employee id.
	 * @param empId employee id
	 * @return MSF826Rec
	 */
	private MSF826Rec readEmployeeCosting(String empId) {
		info("readEmployeeCosting")
		try {
			return edoi.findByPrimaryKey(new MSF826Key(empId))
		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
			info("cannot found employee costing")
			return null
		}
	}

	/**
	 * Read Position Costing based on position id.
	 * @param posId position id
	 * @return MSF874Rec
	 */
	private MSF874Rec readPositionCosting(String posId) {
		info("readPositionCosting")
		try {
			return edoi.findByPrimaryKey(new MSF874Key(posId))
		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
			info("cannot found position costing")
			return null
		}
	}

	/**
	 * Read Position
	 * @param positionId position number.
	 * @return
	 */
	private PositionServiceReadReplyDTO readPosition(String positionId) {
		info("readPosition")
		PositionServiceReadRequiredAttributesDTO posReqAtt = new PositionServiceReadRequiredAttributesDTO()
		posReqAtt.returnPosition = true
		posReqAtt.returnPrimRepCode = true
		PositionServiceReadReplyDTO posReplyDTO = service.get(SERVICE_POSITION).read({PositionServiceReadRequestDTO it->
			it.position = positionId
			it.requiredAttributes = posReqAtt
		})
		return posReplyDTO
	}

	/**
	 * Process 7B - Agency Code 2
	 */
	private void process7BAgencyCode(){
		info("process7BAgencyCode")
		if(posId878?.trim()){
			//use service to read position msf870
			try{
				PositionServiceReadReplyDTO posReplyDTO = readPosition(posId878)
				//Primary reporting code - level 2
				String primRepCode = posReplyDTO.primRepCode.padRight(40)
				if(posReplyDTO != null && primRepCode.substring(4,8)?.trim()){
					extract7B = primRepCode.substring(4,8)
				}else{
					extract7B = "-2"
				}
			}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
				info("Error when read position Service ${e.getMessage()}")
				extract7B = "-2"
			}
		}else{
			extract7B = "-2"
		}
	}

	/**
	 * process 7C - Agency Code 3
	 */
	private void process7CAgencyCode(){
		info("process7CAgencyCode")
		if(posId878?.trim()){
			//use service to read position msf870
			try{
				PositionServiceReadReplyDTO posReplyDTO = readPosition(posId878)
				//Primary reporting code - level 3
				String primRepCode = posReplyDTO.primRepCode.padRight(40)
				if(posReplyDTO != null && primRepCode.substring(8,12)?.trim()){
					extract7C = primRepCode.substring(8,12)
				}else{
					extract7C = "-2"
				}
			}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
				info("Error when read position Service ${e.getMessage()}")
				extract7C = "-2"
			}
		}else{
			extract7C = "-2"
		}

	}

	/**
	 * Process 3C - Agency or Occupation Specific Award Identifier
	 */
	private void process3CAgyOccAwdId(){
		info("process3CAgyOccAwdId")
		if(nswaAgcyOccAwd?.trim()){
			extract3C = nswaAgcyOccAwd
		}else{
			extract3C = "-2"
		}
	}

	/**
	 * Process 3I - Total Hours Paid (Census Period)
	 */
	private void process3ITotHrsPaid(){
		info("process3ITotHrsPaid")
		String earnCode3I =  assocValNswp.get("3I").toString().padRight(50)

		//  * IF ANY earnings have 'AR' in reporting field 5,
		//  * then output -2. This code is applicable to groups
		//  * who are paid a retainer or allowance rather than
		//  * a salary.
		for(EarningTbl earnTbl : earnList){
		//	info("in looping for")
			if(earnTbl.earnCodeRf5 == "AR"){
				extract3I = -2
				break
			}
			int n = 0
			for(int i = 0; i < 15; i++){
				n = 11 + i*2
				String matchEarnCode = earnCode3I.substring(n,n+2)
				if(matchEarnCode == earnTbl.earnCodeRf5  &&  matchEarnCode?.trim()){
					earnCodeFnd = true
					extract3I = extract3I + earnTbl.earnCodeLpu
				}else{
					if(!matchEarnCode?.trim()){
						break
					}
				}
			}
		}
	}

	/**
	 * extract all the data to CSV file
	 */
	private void addEmpRecordToExtract(){
		info("addEmpRecordToExtract")
		DecimalFormat format3Dot2 = new DecimalFormat("000.00")
		DecimalFormat format4Dot2 = new DecimalFormat("0000.00")
		DecimalFormat format2Dot2 = new DecimalFormat("00.00")
		DecimalFormat format7Dot2 = new DecimalFormat("0000000.00")
		DecimalFormat format2Dot7 = new DecimalFormat("00.0000000")
		DecimalFormat format3     = new DecimalFormat("000")
		DecimalFormat format2     = new DecimalFormat("00")

		if(firstCsv){
			firstCsv = false
			def workingDir = env.workDir
			// String taskUUID = this.getTaskUUID()
			String taskUUID = this.getTaskUUID()
			String inputFilePath = "${workingDir}/${CSV_TRTWFP}"
			String outputFilePath = "${workingDir}/${CSV_TRTWFP}"
			if(taskUUID?.trim()){
				outputFilePath = outputFilePath + "." + taskUUID
			}
			outputFilePath = outputFilePath + ".csv"
			trtWFPFile = new File(outputFilePath)
			info("${CSV_TRTWFP} created in ${trtWFPFile.getAbsolutePath()}")
			csvTrtWFPWriter = new BufferedWriter(new FileWriter(trtWFPFile))
			csvTrtWFPWriter.write("Workforce Profile Annual Extract")
			csvTrtWFPWriter.write("\n")
		}

		csvTrtWFPWriter.write(extractAgencyCode.padRight(3) + "," + extract1A.padRight(18) + "," + extract1B.padRight(8) + "," + extract1C.padRight(2) + "," + extract1D.padRight(4) + "," +extract1E.padRight(4)+
				"," + extract2A.padRight(2) + "," + extract2B.padRight(2) + "," + extract2C.padRight(2) + "," + extract2D.padRight(2) + "," + extract3.padRight(3) + "," + extract3A.padRight(8) +
				"," + extract3B + "," + extract3C + "," + format3Dot2.format(extract3D).toString().padLeft(7) + "," + format4Dot2.format(extract3EA).toString().padLeft(8) + "," + format4Dot2.format(extract3F).toString().padLeft(8) + "," + extract3G.padRight(2) +
				"," + extract3H.padRight(2) + "," + format4Dot2.format(extract3I).toString().padLeft(8) + "," + format2Dot2.format(extract3J).toString().padLeft(6) + "," + format3.format(extract3KA).padLeft(4) + "," + extract3L.padRight(10) +
				"," + extract3M.padRight(6) + "," + extract3N.padRight(5) + "," + extract3O.padRight(3) + "," + extract3P.padRight(18) + "," + extract3Q.padRight(18) + "," + format7Dot2.format(extract4A).toString().padLeft(11) +
				"," + format7Dot2.format(extract4B).toString().padLeft(11) + "," + format7Dot2.format(extract4C).toString().padLeft(11) + "," + format7Dot2.format(extract4D).toString().padLeft(11) +
				"," + format7Dot2.format(extract4E).toString().padLeft(11) + "," + format7Dot2.format(extract4G).toString().padLeft(11) +
				"," + format7Dot2.format(extract4H).toString().padLeft(11) + "," + format7Dot2.format(extract4I).toString().padLeft(11) + "," + format7Dot2.format(extract4J).toString().padLeft(11) +
				"," + format7Dot2.format(extract4K).toString().padLeft(11) + "," + extract4L.toString().padLeft(2) + "," + extract4M + "," + extract4N +
				"," + format4Dot2.format(extract5A).toString().padLeft(8) + "," + format4Dot2.format(extract5B).toString().padLeft(8) + "," +  format4Dot2.format(extract5C).toString().padLeft(8) +
				"," +  format4Dot2.format(extract5DA).toString().padLeft(8) + "," +  format4Dot2.format(extract5E).toString().padLeft(8) +
				"," +  format4Dot2.format(extract5F).toString().padLeft(8) + "," +  format4Dot2.format(extract5G).toString().padLeft(8) + "," +  format4Dot2.format(extract5H).toString().padLeft(8) +
				"," +  format4Dot2.format(extract5I).toString().padLeft(8) + "," +  format4Dot2.format(extract5J).toString().padLeft(8) +
				"," +  format4Dot2.format(extract5K).toString().padLeft(8) + "," +  format4Dot2.format(extract5L).toString().padLeft(8) + "," +  format4Dot2.format(extract5M).toString().padLeft(8) +
				"," +  format4Dot2.format(extract5N).toString().padLeft(8) + "," +  format4Dot2.format(extract5O).toString().padLeft(8) +
				"," + extract5P + "," + extract5Q+ "," + extract6A.padRight(8) + "," + extract6B.padRight(8) + "," + extract6C.padRight(3) +    "," + extract6D.padRight(8) + "," + extract6E.padRight(2) +    "," + extract7A.padRight(8) +
				"," + extract7B.padRight(8) + "," + extract7C.padRight(12) + "," + format2Dot7.format(extractZ2).toString().padLeft(11) + "," + format2Dot7.format(extractZ1).toString().padLeft(11) + "," + extract8C + "," + extract8D
				)
		csvTrtWFPWriter.write("\n")
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
		sdf.setLenient(false)
		if(sdf.format(calDate.getTime()) >= MIN_DATE){
			return sdf.format(calDate.getTime())
		}else{
			return "00000000"
		}
	}

	/**
	 * convert String to calendar with format yyyyMMdd
	 * @param String sDate
	 * @return Calendar
	 */
	private Calendar stringToCalendar(String sDate){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
		sdf.setLenient(false)
		Date date = sdf.parse(sDate)
		Calendar calDate = Calendar.getInstance()
		calDate.setTime(date)

		return calDate
	}

	private void printErrorMsg(){
		info("printErrorMsg")
		if (firstErr){
			ReportA = report.open(REPORT_NAME)
			ReportA.write("WorkForce Profile Data Collection Error Report".center(132))
			ReportA.writeLine(132,"-")
			ReportA.write("Field Ref/ Value".padRight(40) + "Error/ Warning MessageColumn Hdg")
			ReportA.writeLine(132,"-")
			firstErr = false
		}
		ReportA.write((errField + (errValue?.trim() ? "/":"") + errValue).padRight(40) + errMessages )
	}

	private void printBatchReport(){
		info("printBatchReport")
		if (ReportA !=null){
			ReportA.close()
		}
		if(csvTrtWFPWriter != null){
			csvTrtWFPWriter.close()
			info("Adding CSV into Request.")
			if (taskUUID?.trim()) {
				request.request.CURRENT.get().addOutput(trtWFPFile,
						"text/comma-separated-values", CSV_TRTWFP);
			}
		}
	}
}

/*run script*/  
ProcessTrbwfp process = new ProcessTrbwfp();
process.runBatch(binding);
