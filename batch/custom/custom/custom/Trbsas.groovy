/**
 * @Ventyx 2012
 * Conversion from trisas.cbl and trissf.cbl
 *
 * This program calculates the superable
 * salary for employees.
 */
package com.mincom.ellipse.script.custom;

import java.text.DecimalFormat

import com.mincom.batch.script.*
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec
import com.mincom.ellipse.edoi.ejb.msf785.MSF785Key
import com.mincom.ellipse.edoi.ejb.msf785.MSF785Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Rec
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec
import com.mincom.ellipse.edoi.ejb.msf821.MSF821Key
import com.mincom.ellipse.edoi.ejb.msf821.MSF821Rec
import com.mincom.ellipse.edoi.ejb.msf822.MSF822Key
import com.mincom.ellipse.edoi.ejb.msf822.MSF822Rec
import com.mincom.ellipse.edoi.ejb.msf828.MSF828Key
import com.mincom.ellipse.edoi.ejb.msf828.MSF828Rec
import com.mincom.ellipse.edoi.ejb.msf830.MSF830Key
import com.mincom.ellipse.edoi.ejb.msf830.MSF830Rec
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Key
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Rec
import com.mincom.ellipse.edoi.ejb.msf846.MSF846Key
import com.mincom.ellipse.edoi.ejb.msf846.MSF846Rec
import com.mincom.ellipse.eroi.linkage.msscnv.*
import com.mincom.ellipse.eroi.linkage.mssemp.*
import com.mincom.ellipse.eroi.linkage.mssrat.*
import com.mincom.ellipse.script.util.*
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.*
import com.mincom.eql.impl.*


/**
 * Request Parameters for Trbsas.
 * 
 * <li><code>paramSuperannuationFund</code> : Super Fund. Must select (D)efined Benefit or (R)etirement Scheme</li>
 * <li><code>paramReportDate</code> : Report Date.</li>
 * <li><code>paramStartDate</code> : Shift Start Date.</li>
 * <li><code>paramEndDate</code> : Shift End Date.</li>
 * <li><code>paramBirthDate</code> : Birth Date. </li>
 */
public class ParamsTrbsas{
	//List of Input Parameters
	String paramSuperannuationFund;
	String paramReportDate;
	String paramStartDate;
	String paramEndDate;
	String paramBirthDate;
}

public class ProcessTrbsas extends SuperBatch {
	private static final int MAX_ROW_READ = 1000

	/* 
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT 
	 */
	private version = 7;
	private ParamsTrbsas batchParams;

	private class SallEnt{
		private String sallCode
		private String sallType
		private BigDecimal sallCompFreq
		private BigDecimal sallEarnFactor

		public SallEnt(String sallCode, String sallType,BigDecimal sallCompFreq, BigDecimal sallEarnFactor) {
			this.sallCode = sallCode
			this.sallType = sallType
			this.sallCompFreq = sallCompFreq
			this.sallEarnFactor = sallEarnFactor
		}


		public String getSallCode() {
			return sallCode
		}
		public void setSallCode(String sallCode) {
			this.sallCode = sallCode
		}
		public String getSallType() {
			return sallType
		}
		public void setSallType(String sallType) {
			this.sallType = sallType
		}
		public BigDecimal getSallCompFreq() {
			return sallCompFreq
		}
		public void setSallCompFreq(BigDecimal sallCompFreq) {
			this.sallCompFreq = sallCompFreq
		}
		public BigDecimal getSallEarnFactor() {
			return sallEarnFactor
		}
		public void setSallEarnFactor(BigDecimal sallEarnFactor) {
			this.sallEarnFactor = sallEarnFactor
		}
	}

	private class ShftEnt{
		private String shftCode
		private String shftType
		public ShftEnt(String shftCode, String shftType) {
			this.shftCode = shftCode
			this.shftType = shftType
		}
		public String getShftCode() {
			return shftCode
		}
		public void setShftCode(String shftCode) {
			this.shftCode = shftCode
		}
		public String getShftType() {
			return shftType
		}
		public void setShftType(String shftType) {
			this.shftType = shftType
		}
	}

	private class TrbsasaReportLine{
		private String employeeId;
		private String memberNo;
		private String firstName;
		private String lastName;
		private String type;
		private String gender;
		private String birthDate;
		private BigDecimal superableSalary;
		private String shftCategDesc;
		private BigDecimal ttlShftWork;
		private String reason;

		public TrbsasaReportLine(String employeeId, String memberNo,
		String firstName, String lastName, String type,
		String gender, String birthDate,
		BigDecimal superableSalary, String shftCategDesc,
		BigDecimal ttlShftWork, String reason) {
			this.employeeId = employeeId;
			this.memberNo = memberNo;
			this.firstName = firstName;
			this.lastName = lastName;
			this.type = type;
			this.gender = gender;
			this.birthDate = birthDate;
			this.superableSalary = superableSalary;
			this.shftCategDesc = shftCategDesc;
			this.ttlShftWork = ttlShftWork;
			this.reason = reason;
		}

		public String getEmployeeId() {
			return employeeId;
		}

		public void setEmployeeId(String employeeId) {
			this.employeeId = employeeId;
		}

		public String getMemberNo() {
			return memberNo;
		}

		public void setMemberNo(String memberNo) {
			this.memberNo = memberNo;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public String getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(String birthDate) {
			this.birthDate = birthDate;
		}

		public BigDecimal getSuperableSalary() {
			return superableSalary;
		}

		public void setSuperableSalary(BigDecimal superableSalary) {
			this.superableSalary = superableSalary;
		}

		public String getShftCategDesc() {
			return shftCategDesc;
		}

		public void setShftCategDesc(String shftCategDesc) {
			this.shftCategDesc = shftCategDesc;
		}

		public BigDecimal getTtlShftWork() {
			return ttlShftWork;
		}

		public void setTtlShftWork(BigDecimal ttlShftWork) {
			this.ttlShftWork = ttlShftWork;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}
	}

	private def ReportA
	private def ReportB
	private BufferedWriter ReportC
	private File outputFileTRTSAS
	private String[] dednCode
	private SallEnt[] sallArray
	private ShftEnt[] shftArray
	private TrbsasaReportLine currLine
	private BigDecimal annualSalary = 0
	private BigDecimal calUnitTotal = 0
	private BigDecimal superableSalTot = 0
	private String strPrd
	private String endPrd
	private BigDecimal saveHrs
	private int recordCount
	private DecimalFormat decFormatter = new DecimalFormat("################0.00");
	private DecimalFormat decFormatter2 = new DecimalFormat("#################.##");
	private String awardCode;
	private String rateRefCode;

	/**
	 * Run the main batch.
	 * @param b is a binding object passed from the <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b){

		init(b);

		printSuperBatchVersion();
		info("runBatch Version : " + version);

		batchParams = params.fill(new ParamsTrbsas())

		//PrintRequest Parameters
		info("paramSuperannuationFund: " + batchParams.paramSuperannuationFund)
		info("paramReportDate: " + batchParams.paramReportDate)
		info("paramStartDate: "+ batchParams.paramStartDate)
		info("paramEndDate: " + batchParams.paramEndDate)
		info("paramBirthDate: " +batchParams.paramBirthDate)
		try {
			processBatch();

		} finally {
			printBatchReport();
		}
	}

	/**
	 * Main process of the batch.
	 */
	private void processBatch(){
		//write process
		info("processBatch")
		initialize()
		if (validateRequestParam()){
			loadArray()
			calculateSuperableSalary()
		}
	}

	//additional method - start from here.

	/**
	 * Initialize variable and write report header.
	 */
	private void initialize(){
		info("initialize")
		recordCount = 0

		ReportA = report.open("TRBSASA")
		ReportB = report.open("TRBSASB")
		def workingDir = env.workDir
		String taskUUID = this.getTaskUUID()
		String outputFilePath = "${workingDir}/TRTSAS"
		if(taskUUID?.trim()) {
			outputFilePath = outputFilePath + "." + taskUUID
		}
		outputFilePath = outputFilePath + ".csv"
		outputFileTRTSAS = new File(outputFilePath)
		FileWriter fstream = new FileWriter(outputFileTRTSAS)
		ReportC = new BufferedWriter(fstream)
		writeReportHeader()
	}

	/**
	 * Write report header.
	 */
	private void writeReportHeader(){
		info("writeReportHeader")
		String tempString =""
		ReportA.write("EISS Superable Salary Report".center(132))
		ReportA.write("\n")
		ReportA.write("Superannuation Fund:	EISS Retirement Scheme")

		ReportB.write ("EISS Superable Salary Exception Report".center(132))
		ReportB.write("\n")
		ReportB.write("Superannuation Fund:	EISS Retirement Scheme")

		ReportC.write("Emp-Id,Member-Id,Last Name,First Name,Date of Birth,Gender,Full-time Annual Salary,Part-time Annual Salary\r\n")

		tempString = tempString + "Report Date: "+
				batchParams.paramReportDate.substring(6,8) + "/" +
				batchParams.paramReportDate.substring(4,6) + "/" +
				batchParams.paramReportDate.substring(2,4) + "  "

		if (batchParams.paramBirthDate.equals("1")){
			tempString = tempString + "Birthday: Jan-June  "
		} else if (batchParams.paramBirthDate.equals("2")){
			tempString = tempString + "Birthday: July-Dec  "
		} else{
			tempString = tempString + "Birthday: N/A       "
		}

		tempString = tempString + "Shifts work period: " + batchParams.paramStartDate.substring(6,8) + "/" +
				batchParams.paramStartDate.substring(4,6) + "/" +
				batchParams.paramStartDate.substring(2,4) + " - " +
				batchParams.paramEndDate.substring(6,8) + "/" +
				batchParams.paramEndDate.substring(4,6) + "/" +
				batchParams.paramEndDate.substring(2,4)

		ReportA.write(tempString)
		ReportA.writeLine(132,"-")

		ReportB.write(tempString)
		ReportB.writeLine(132,"-")

		ArrayList<String> reportHeaderA = new ArrayList<String>()
		reportHeaderA.add("                         Employee                                                         Superable   Shift Category   Total No.")
		reportHeaderA.add("Emp-Id       Member-No   Last Name                        Type   Gender   Birth Date         Salary   Description      Shifts Worked")

		ArrayList<String> reportHeaderB = new ArrayList<String>()
		reportHeaderB.add("                           Employee")
		reportHeaderB.add("Emp-Id        Member-No    Last Name                               Salary    Reason for Exception")

		ReportA.pageHeadings = reportHeaderA
		ReportA.heading()
		ReportB.pageHeadings = reportHeaderB
		ReportB.heading()
	}

	/**
	 * Validate Request Parameter.
	 */
	private boolean validateRequestParam(){
		info("validateRequestParam")
		if (!batchParams.paramSuperannuationFund.equals("D") && !batchParams.paramSuperannuationFund.equals("R")){
			ReportB.write("INVALID PARAMETER - Superannuation Fund Must be (D)efined Benefit  or (R)etirement ")
			return false
		}
		if (!batchParams.paramBirthDate.equals("1") && !batchParams.paramBirthDate.equals("2") && !batchParams.paramBirthDate.equals(" ")){
			ReportB.write("INVALID PARAMETER - Birth Date Must be 1, or 2, or SPACE")
			return false
		}
		return true
	}

	/**
	 * Initiate Array variable.
	 */
	private void loadArray(){
		info("loadArray")
		dednCode = new String[100]
		sallArray = new SallEnt[100]
		shftArray = new ShftEnt[100]
		for (int i =0; i<100; i++){
			dednCode[i] = " "
			sallArray[i] = new SallEnt(" "," ",0,0)
			shftArray[i] = new ShftEnt(" "," ")
		}

		if (batchParams.paramSuperannuationFund.equals("D")){
			loadD1Rec()
		} else {
			loadR1Rec()
		}

		loadSallShftArray()
	}

	/**
	 * Load Deduction Records with MiscRptField = 'D1'.
	 */
	private void loadD1Rec(){
		info("loadD1Rec")
		int index = 0

		Constraint c1 = MSF801_D_801Key.cntlRecType.equalTo("D")
		Constraint c2 = MSF801_D_801Rec.miscRptFldDx1.equalTo("D1")
		Constraint c3 = MSF801_D_801Rec.miscRptFldDx2.equalTo("D1")
		Constraint c4 = MSF801_D_801Rec.miscRptFldDx3.equalTo("D1")
		Constraint c5 = MSF801_D_801Rec.miscRptFldDx4.equalTo("D1")
		Constraint c6 = MSF801_D_801Rec.miscRptFldDx5.equalTo("D1")
		def query = new QueryImpl(MSF801_D_801Rec.class).and(c1.and((c2).or(c3).or(c4).or(c5).or(c6))).orderBy(new MSF801_D_801Key())
		edoi.search(query,MAX_ROW_READ,{MSF801_D_801Rec msf801_d_801rec ->
			boolean isD1 = checkMiscRptField(msf801_d_801rec.getMiscRptFldDx1(),
					msf801_d_801rec.getMiscRptFldDx2(),
					msf801_d_801rec.getMiscRptFldDx3(),
					msf801_d_801rec.getMiscRptFldDx4(),
					msf801_d_801rec.getMiscRptFldDx5(), "D1")
			if (isD1){
				if (index < 99){
					dednCode[index] = msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6)
					index++
				} else {
					info("TOO MANY DEDUCTION CODES")
				}
			}
		})
	}

	/**
	 * Load Deduction Records with MiscRptField = 'R1'.
	 */
	private void loadR1Rec(){
		info("loadR1Rec")
		int index = 0

		Constraint c1 = MSF801_D_801Key.cntlRecType.equalTo("D")
		Constraint c2 = MSF801_D_801Rec.miscRptFldDx1.equalTo("R1")
		Constraint c3 = MSF801_D_801Rec.miscRptFldDx2.equalTo("R1")
		Constraint c4 = MSF801_D_801Rec.miscRptFldDx3.equalTo("R1")
		Constraint c5 = MSF801_D_801Rec.miscRptFldDx4.equalTo("R1")
		Constraint c6 = MSF801_D_801Rec.miscRptFldDx5.equalTo("R1")
		def query = new QueryImpl(MSF801_D_801Rec.class).and(c1.and((c2).or(c3).or(c4).or(c5).or(c6))).orderBy(new MSF801_D_801Key())
		edoi.search(query, MAX_ROW_READ,{MSF801_D_801Rec msf801_d_801rec ->
			if (index < 99){
				dednCode[index] = msf801_d_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6)
				index++
			} else {
				info("TOO MANY DEDUCTION CODES")
			}
		})
	}

	/**
	 * Load Allowance code and shift code Records.
	 */
	private void loadSallShftArray(){
		info("loadSallShftArray")
		int indexSall = 0
		int indexShft = 0

		Constraint c1 = MSF801_A_801Key.cntlRecType.equalTo("A")
		def query = new QueryImpl(MSF801_A_801Rec.class).and(c1).orderBy(new MSF801_A_801Key())
		edoi.search(query,MAX_ROW_READ,{MSF801_A_801Rec msf801_a_801rec ->
			boolean isSU = checkMiscRptField(msf801_a_801rec.getMiscRptFldAx1(),
					msf801_a_801rec.getMiscRptFldAx2(),
					msf801_a_801rec.getMiscRptFldAx3(),
					msf801_a_801rec.getMiscRptFldAx4(),
					msf801_a_801rec.getMiscRptFldAx5(), "SU")
			boolean isSX = checkMiscRptField(msf801_a_801rec.getMiscRptFldAx1(),
					msf801_a_801rec.getMiscRptFldAx2(),
					msf801_a_801rec.getMiscRptFldAx3(),
					msf801_a_801rec.getMiscRptFldAx4(),
					msf801_a_801rec.getMiscRptFldAx5(), "SX")
			if (isSU || isSX){
				if (indexSall < 99){
					isSU = checkMiscRptField(msf801_a_801rec.getMiscRptFldAx1(),
							msf801_a_801rec.getMiscRptFldAx2(),
							msf801_a_801rec.getMiscRptFldAx3(),
							msf801_a_801rec.getMiscRptFldAx4(),
							msf801_a_801rec.getMiscRptFldAx5(), "SU")
					if (isSU){
						sallArray[indexSall].setSallType("SU")
					} else{
						sallArray[indexSall].setSallType("SX")
					}
					sallArray[indexSall].setSallCode(msf801_a_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6))
					sallArray[indexSall].setSallCompFreq(msf801_a_801rec.getCompFreqRtA())
					sallArray[indexSall].setSallEarnFactor(msf801_a_801rec.getEarnFactorA())
					indexSall++
				} else {
					info("TOO MANY ALLOWANCE CODES")
				}
			}
			boolean isSR = checkMiscRptField(msf801_a_801rec.getMiscRptFldAx1(),
					msf801_a_801rec.getMiscRptFldAx2(),
					msf801_a_801rec.getMiscRptFldAx3(),
					msf801_a_801rec.getMiscRptFldAx4(),
					msf801_a_801rec.getMiscRptFldAx5(), "SR")
			if (isSR){
				if (indexShft < 99){
					shftArray[indexShft].setShftType("SR")
					shftArray[indexShft].setShftCode(msf801_a_801rec.getPrimaryKey().getCntlKeyRest().substring(3,6))
					indexShft++
				} else {
					info("TOO MANY SHIFT CODES")
				}
			}
		})
		for(ShftEnt e : shftArray) {
			debug("ShftEnt ${e.getShftType()} - ${e.getShftCode()}")
		}
	}

	/**
	 * Check Misc Rpt Field
	 * @return true if Misc Rpt Field is valid otherwise return false
	 */
	private boolean checkMiscRptField (String rptField1, String rptField2, String rptField3, String rptField4, String rptField5, String compareValue){
		info ("checkMiscRptField")
		if (rptField1.trim().equals(compareValue.trim())
		|| rptField2.trim().equals(compareValue.trim())
		|| rptField3.trim().equals(compareValue.trim())
		|| rptField4.trim().equals(compareValue.trim())
		|| rptField5.trim().equals(compareValue.trim())){
			return true
		}
		return false
	}

	/**
	 * Perform Calculation of Superable Salary
	 */
	private void calculateSuperableSalary(){
		info("calculateSuperableSalary")

		MSF822Key msf822key = new MSF822Key()
		Constraint c1 = MSF822Key.employeeId.greaterThan(" ")
		def query = new QueryImpl(MSF822Rec.class).and(c1).orderBy(MSF822Rec.msf822Key)
		edoi.search(query, MAX_ROW_READ, {MSF822Rec msf822rec ->
			if (msf822rec.getStartDate().equals("00000000") || msf822rec.getStartDate().compareTo(batchParams.paramReportDate) <= 0){
				if (msf822rec.getEndDate().equals("00000000") || msf822rec.getEndDate().compareTo(batchParams.paramReportDate) >= 0){
					boolean endLoop = false
					int index = 0
					while (!endLoop){
						if (msf822rec.getPrimaryKey().getDednCode().equals(dednCode[index])){
							endLoop = true
							currLine = new TrbsasaReportLine(" "," "," "," "," "," "," ",0," ",0," ")
							currLine.setMemberNo(msf822rec.getDednRef())
							boolean ProcessedSw = checkEmployee(msf822rec.getPrimaryKey().getEmployeeId(),
									msf822rec.getDednRef().trim())
						} else{
							index++
							if (msf822rec.getPrimaryKey().getDednCode().equals(" ") || index > 99 ){
								endLoop = true
							}

						}
					}
				}
			}

		})


		//Browse MSF785 to collect all the employee_id
		int iReverseParamReportDate = 99999999 - Integer.parseInt(batchParams.paramReportDate)
		List <String> employee785 = collect785EmployeeId(iReverseParamReportDate.toString())

		//Browse MSF785 for salary package

		employee785.each{
			String memberId785
			Constraint c_785_1 = MSF785Key.employeeId.equalTo(it)
			Constraint c_785_2 = MSF785Key.invEffDate.greaterThanEqualTo(iReverseParamReportDate.toString())

			def query785 = new QueryImpl(MSF785Rec.class).and(c_785_1).and(c_785_2).orderBy(MSF785Rec.msf785Key)

			MSF785Rec msf785rec = edoi.firstRow(query785)

			if (msf785rec != null){

				Integer counter = 1
				Boolean found = false

				List lSuperDeductionCode = dednCode.flatten().findAll{it != null}

				while(counter<=10 && !found) {
					String fieldName = "getBenefitType_${counter}"
					String dednCode785 = msf785rec."$fieldName"()
					//info("dednCode785 : ${dednCode785}")
					if(dednCode785?.trim() && lSuperDeductionCode.contains(dednCode785.padRight(4).substring(1,4))) {
						found=true
					}
					else {
						counter++
					}
				}

				if (found){
					String fieldName = "getBenefitRef_${counter}"
					memberId785 = msf785rec."$fieldName"()
					currLine = new TrbsasaReportLine(" "," "," "," "," "," "," ",0," ",0," ")
					currLine.setMemberNo(memberId785.trim())
					boolean ProcessedSw = checkEmployee(msf785rec.getPrimaryKey().getEmployeeId(),
							memberId785.trim())
				}else{
					memberId785 = ""
				}


			}
		}
	}

	private List <String> collect785EmployeeId(String invEffectiveDate){
		List <String> employees785 = new ArrayList <String>()
		Constraint c_785_1 = MSF785Key.employeeId.greaterThan(" ")
		Constraint c_785_2 = MSF785Key.invEffDate.greaterThanEqualTo(invEffectiveDate)

		def query785 = new QueryImpl(MSF785Rec.class).and(c_785_1).and(c_785_2).orderBy(MSF785Rec.msf785Key)

		edoi.search(query785, MAX_ROW_READ, {MSF785Rec msf785rec ->

			if (!employees785.contains(msf785rec.getPrimaryKey().getEmployeeId())){
				employees785.add(msf785rec.getPrimaryKey().getEmployeeId())
			}
		})

		return employees785
	}
	/**
	 * Check employee Details
	 * @param employeeId is Employee Id
	 * @param memberId is Member Id
	 * @return true if employee is valid otherwise return false
	 */
	private boolean checkEmployee(String employeeId, String memberId){
		info("checkEmployee ${employeeId}")
		MSF810Rec msf810rec
		MSF760Rec msf760rec
		MSF820Rec msf820rec
		//Check if the employee id exist in MSF760, MSF810, MSF820
		MSSEMPLINK mssemplnk = eroi.execute('MSSEMP', {MSSEMPLINK mssemplnk ->
			mssemplnk.link = ""
			mssemplnk.employeeId = employeeId
			mssemplnk.empRecType = "2"
		})

		if (mssemplnk.foundSwEmp.equals("Y")){
			//Get MSF810 Record
			msf810rec = getEmployeeDetails(employeeId)

			//Get MSF760 Record
			msf760rec = getEmployeePersonnelDetails(employeeId)

			//Get MSF820 Record
			msf820rec = getEmployeePayrollDetails(employeeId)

		} else{
			return false
		}

		if ((!msf760rec.getTermDate().equals("00000000")) && (msf760rec.getTermDate().compareTo(batchParams.paramReportDate) < 0)){
			return false
		}

		if ((!msf760rec.getServiceDate().equals("00000000")) && (msf760rec.getServiceDate().compareTo(batchParams.paramReportDate) > 0)){
			return false
		}

		// As per Email 29/03/06 12:43:21 pm - Drop following
		if (msf820rec.getEmployeeClass().equals("C")){
			return false
		}

		String birthDate = msf760rec.getBirthDate()
		if (birthDate.substring(4,6).toInteger() <= 6 && batchParams.paramBirthDate.equals("2")){
			return false
		}

		if (birthDate.substring(4,6).toInteger() >= 7 && batchParams.paramBirthDate.equals("1")){
			return false
		}

		currLine.setEmployeeId(employeeId)
		currLine.setLastName(msf810rec.getSurname())
		currLine.setFirstName(msf810rec.getFirstName())
		currLine.setGender(msf760rec.getGender())
		currLine.setType(msf820rec.getEmployeeClass())
		currLine.setBirthDate(birthDate)
		currLine.setShftCategDesc(msf820rec.getShiftCat())
		String tempString

		countSuperableSalary(employeeId, msf760rec.getStaffCateg(),msf820rec.getShiftCat())

		if (currLine.getSuperableSalary() == 0){
			currLine.setReason("SUPERABLE SALARY IS ZERO")
			writeReportBDetails(currLine)
		}
		if (currLine.getMemberNo().equals(" ")){
			currLine.setReason("MEMBER NO. IS BLANK")
			writeReportBDetails(currLine)
		}
		if (currLine.getTtlShftWork() <= 208.0){
			currLine.setReason("TOTAL SHIFT LESS THAN 208")
			writeReportBDetails(currLine)
		}
		if (msf760rec.getEmpStatus().equals("Z")){
			tempString = ""
			debug("tempString: "+ tempString)
			tempString = tempString + msf760rec.getTermDate().substring(6,8) + "/" + msf760rec.getTermDate().substring(4,6) + "/" + msf760rec.getTermDate().substring(2,4)
			debug("tempString: "+ tempString)
			currLine.setReason("TERMINATED - "+ tempString)
			writeReportBDetails(currLine)
		}
		if (msf760rec.getEmpStatus().equals("X")){
			tempString = ""
			debug("tempString: "+ tempString)
			msf760rec.getTermDate().substring(4,6)
			tempString = tempString + msf760rec.getTermDate().substring(6,8) + "/" + msf760rec.getTermDate().substring(4,6) + "/" + msf760rec.getTermDate().substring(2,4)
			debug("tempString: "+ tempString)
			currLine.setReason("TERMINATED - "+ tempString)
			writeReportBDetails(currLine)
		}

		writeReportCDetails(currLine)
		writeReportADetails(currLine)

		return true
	}

	/**
	 * Count Superable Salary
	 * @param employeeId is Employee Id
	 * @param staffCateg is Staff Category
	 * @param shiftCat is Shift Category
	 */
	private void countSuperableSalary(String employeeId, String staffCateg, String shiftCat){
		info("countSuperableSalary")
		BigDecimal superableSalary = 0
		boolean shiftWork = false
		boolean shiftWork2 = false
		saveHrs = 0
		calUnitTotal = 0
		BigDecimal shiftLoadCount = 0

		shiftWork = checkShiftCategory(shiftCat)

		//Get Shift Worked Units (SWUNT)
		getShiftWorkedUnits(employeeId)

		if(staffCateg.equals("SES") || staffCateg.equals("SCO")){
			//Senior Contract Staff
			shiftLoadCount = calUnitTotal
			superableSalary = getSuperSalary(employeeId)
		}
		else if(staffCateg.equals("IEA")){
			//Contract or Individual Employment Arrengement Staff
			rateRefCode = awardCode = " "
			shiftLoadCount = calUnitTotal
			getRateRefCode(employeeId)
			MSSRATLINK mssratlnk = getRates(employeeId)
			if (!mssratlnk.ratStatus?.trim().equals("")){
				info ("EXITING AT MSSRAT")
			}else{
				superableSalary = mssratlnk.getCalcAnlRate()
			}
		}
		else if(shiftWork){
			//Shift Workers
			BigDecimal percentShiftLoad = 0
			BigDecimal weeklyRate = 0
			BigDecimal newWeeklyRate = 0
			BigDecimal superableAllowance = 0

			rateRefCode = awardCode = " "
			getRateRefCode(employeeId)

			MSSRATLINK mssratlnk = getRates(employeeId)
			if (!mssratlnk.ratStatus?.trim().equals("")){
				info ("EXITING AT MSSRAT")
			}else{
				weeklyRate = mssratlnk.getCalcWklyRate()
			}

			shiftLoadCount = calUnitTotal / saveHrs

			//Determine Percentage of Shift Loading
			if (shiftLoadCount < 105){
				percentShiftLoad = 0
			} else if (shiftLoadCount > 104 && shiftLoadCount < 157){
				percentShiftLoad = 0.1
			} else if (shiftLoadCount > 156 && shiftLoadCount < 209){
				percentShiftLoad = 0.15
			} else{
				percentShiftLoad = 0.2
			}

			//newWeeklyRate = weeklyRate + percentShiftLoad
			newWeeklyRate = weeklyRate + (weeklyRate * percentShiftLoad)
			superableAllowance = getSuperableAllowance(employeeId)
			superableSalary = (newWeeklyRate + superableAllowance) * 52.2
		}
		else{
			//Award Staff
			BigDecimal weeklyRate = 0
			BigDecimal superableAllowance = 0

			MSSRATLINK mssratlnk = getRates(employeeId)
			if (!mssratlnk.ratStatus?.trim().equals("")){
				info ("EXITING AT MSSRAT")
			}else{
				weeklyRate = mssratlnk.getCalcWklyRate()
			}

			shiftLoadCount = calUnitTotal
			superableAllowance = getSuperableAllowance(employeeId)
			superableSalary = (weeklyRate + superableAllowance) * 52.2
		}

		superableSalary = superableSalary.setScale(0, BigDecimal.ROUND_HALF_DOWN)
		currLine.setTtlShftWork(shiftLoadCount)
		currLine.setSuperableSalary(superableSalary)
	}

	private boolean checkShiftCategory(String shiftCat){
		info("checkShiftCategory")

		boolean shiftWork = false
		try{
			TableServiceReadReplyDTO tableReply = service.get('Table').read({
				it.tableType = "SCAT"
				it.tableCode = shiftCat})
			if(tableReply.getAssociatedRecord().substring(5,6).equals("Y")){
				saveHrs = (tableReply.getDescription().substring(26,30).toInteger() / 100)
				shiftWork = true
			}
		}catch (EnterpriseServiceOperationException e){
			info("Shift Category not found")
		}

		return shiftWork
	}

	private BigDecimal getSuperSalary(String employeeId){
		info("getSuperSalary")

		boolean flag = false
		BigDecimal superSalary = 0
		int reportDate = Integer.parseInt(batchParams.paramReportDate)


		try{
			Constraint c1 = MSF846Key.employeeId.equalTo(employeeId.trim())
			Constraint c2 = MSF846Key.invStrDate.greaterThanEqualTo(" ")
			def query = new QueryImpl(MSF846Rec.class).and(c1.and(c2)).orderBy(MSF846Rec.msf846Key)
			edoi.search(query, MAX_ROW_READ, {MSF846Rec msf846Rec ->
				int strDate = (99999999 - (msf846Rec.getPrimaryKey().getInvStrDate() as int))
				if(strDate <= reportDate && !flag){
					flag = true

					if (msf846Rec.getSuperSalary() > 0){
						superSalary = msf846Rec.getSuperSalary()
					}else{
						superSalary = msf846Rec.getOrideSalary()
					}

				}
			})
		}catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			info("MSF846-SUPER-SALARY not found")
		}

		return superSalary
	}

	private MSSRATLINK getRates(String employeeId){
		info("getRates")

		rateRefCode = awardCode = " "
		getRateRefCode(employeeId)
		MSSRATLINK mssratlnk = eroi.execute('MSSRAT', {MSSRATLINK mssratlnk ->
			mssratlnk.option = "3"
			mssratlnk.requiredDate = batchParams.paramReportDate
			mssratlnk.awardCode = awardCode
			mssratlnk.rateRefCode = rateRefCode
		})

		return mssratlnk
	}

	private BigDecimal getSuperableAllowance(String employeeId){
		info("getSuperableAllowance")

		BigDecimal superableAllowance = 0

		Constraint c1 = MSF821Key.employeeId.equalTo(employeeId)
		Constraint c2 = MSF821Key.earningsCode.greaterThanEqualTo(" ")
		Constraint c3 = MSF821Rec.endDate.equalTo("00000000")
		def query = new QueryImpl(MSF821Rec.class).and(c1.and(c2).and(c3)).orderBy(MSF821Rec.msf821Key)
		edoi.search(query, MAX_ROW_READ, {MSF821Rec msf821rec ->
			int index = 0
			boolean endLoop = false
			while (!endLoop){

				if (msf821rec.getPrimaryKey().getEarningsCode().equals(sallArray[index].getSallCode())){
					endLoop = true
					if (sallArray[index].getSallType().equals("SX")){
						superableAllowance = sallArray[index].getSallCompFreq() * 52.2
					} else{
						superableAllowance = sallArray[index].getSallEarnFactor() * 52.2
					}
				} else{
					index++
					if (msf821rec.getPrimaryKey().getEarningsCode().equals(" ") || index > 99 ){
						endLoop = true
					}

				}
			}
		})

		return superableAllowance
	}


	/**
	 * Get Employee Details
	 * @param employeeId is Employee Id
	 */
	private MSF810Rec getEmployeeDetails(String employeeId){
		info("getMSF810Record")
		try{
			MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(employeeId))
			return msf810rec
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			info("MSF810 Not Found")
			return null
		}
	}

	/**
	 * Get Employee Personnel Details
	 * @param employeeId is Employee Id
	 */
	private MSF760Rec getEmployeePersonnelDetails(String employeeId){
		info ("getEmployeePersonnelDetails")
		try{
			MSF760Rec msf760rec = edoi.findByPrimaryKey(new MSF760Key(employeeId))
			return msf760rec
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			info("MSF760 Not Found")
			return null
		}
	}

	/**
	 * Get Employee Payroll Details
	 * @param employeeId is Employee Id
	 */
	private MSF820Rec getEmployeePayrollDetails(String employeeId){
		info("getEmployeePayrollDetails")
		try{
			MSF820Rec MSF820rec = edoi.findByPrimaryKey(new MSF820Key(employeeId))
			return MSF820rec
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			info("MSF820 Not Found")
			return null
		}
	}

	/**
	 * Set the award code for employee
	 * @param employeeId is Employee Id
	 * @param invDate is inverse date 
	 */
	private void getAwardCode(String employeeId, String invDate){
		info("getAwardCode")
		Constraint c1 = MSF828Key.employeeId.equalTo(employeeId)
		Constraint c2 = MSF828Key.invStrDate.greaterThanEqualTo(invDate)
		def query = new QueryImpl(MSF828Rec.class).and(c1.and(c2))

		MSF828Rec msf828rec = (MSF828Rec) edoi.firstRow(query)
		if (msf828rec){
			awardCode = msf828rec.awardCode
		}
	}

	/**
	 * Set the ref code
	 * @param employeeId is Employee Id
	 */
	private void getRateRefCode(String employeeId){
		info("getRateRefCode")
		String invCompDate = (99999999 - batchParams.paramReportDate.toBigInteger()).toString()
		Constraint c1 = MSF830Key.employeeId.equalTo(employeeId)
		Constraint c2 = MSF830Key.invStrDate.greaterThanEqualTo(invCompDate)
		def query = new QueryImpl(MSF830Rec.class).and(c1).and(c2)

		MSF830Rec msf830rec = (MSF830Rec) edoi.firstRow(query)
		if (msf830rec){
			rateRefCode = msf830rec.rateRefCode
			getAwardCode(employeeId, msf830rec.primaryKey.invStrDate)
		}
	}

	/**
	 * Get Shift Worked Units
	 * @param employeeId is Employee Id
	 */
	private void getShiftWorkedUnits(String employeeId){
		info("getShiftWorkedUnits")

		Constraint c1 = MSF835Key.employeeId.equalTo(employeeId)
		Constraint c2 = MSF835Key.trnDate.greaterThanEqualTo(batchParams.paramStartDate)
		Constraint c3 = MSF835Key.trnDate.lessThanEqualTo(batchParams.paramEndDate)
		Constraint c7 = MSF835Key.envelopeType.equalTo(" ")
		Constraint c8 = MSF835Key.envelopeType.equalTo("A")
		Constraint c9 = MSF835Rec.prevRetro.notEqualTo("Y")
		Constraint c10 = MSF835Rec.payRunType.equalTo("U")
		Constraint c11 = MSF835Key.tranInd.equalTo("1")
		Constraint c12 = MSF835Key.prdEndDate.greaterThanEqualTo(" ")

		def query = new QueryImpl(MSF835Rec.class).and(c1).and(c2).and(c3).and((c7).or(c8)).
				and(c9).and(c10).and(c11).and(c12).orderBy(MSF835Rec.msf835Key)

		edoi.search(query, MAX_ROW_READ, {MSF835Rec msf835rec ->
			info ("getTranCode():" +  msf835rec.getPrimaryKey().getTranCode())
			int index = 0
			boolean endLoop = false
			while (!endLoop){
				debug ("shftArray[${index.toString()}].getShftCode():" + shftArray[index].getShftCode())
				if (msf835rec.getPrimaryKey().getTranCode().trim().equals(shftArray[index].getShftCode())){
					endLoop = true
					if (shftArray[index].getShftType().trim().equals("SR")){
						calUnitTotal = calUnitTotal + msf835rec.getTrnUnits()
					}
				} else{
					index++
					if (shftArray[index].getShftCode().equals(" ") || index > 99 ){
						endLoop = true
					}
				}
			}
		})

		info ("calUnitTotal:" + calUnitTotal.toString())
	}

	/**
	 * Write TRBSASB Report Details
	 * @param writeLine is Report Line
	 */
	private void writeReportBDetails(TrbsasaReportLine writeLine){
		info("writeReportBDetails")
		String tempString = ""
		tempString = writeLine.getEmployeeId().padRight(14) +
				writeLine.getMemberNo().padRight(13) +
				writeLine.getLastName().padRight(33) +
				decFormatter2.format(writeLine.getSuperableSalary()).padLeft(13) + "    " +
				writeLine.getReason().padRight(43)
		ReportB.write(tempString)
	}

	/**
	 * Write TRTSAS Report Details
	 * @param writeLine is Report Line
	 */
	private void writeReportCDetails(TrbsasaReportLine writeLine){
		info("writeReportCDetails")
		String tempString = ""
		tempString = tempString + writeLine.getEmployeeId() + ","
		tempString = tempString + writeLine.getMemberNo() + ","
		tempString = tempString + writeLine.getLastName() + ","
		tempString = tempString + writeLine.getFirstName() + ","
		tempString = tempString + writeLine.getBirthDate() + ","
		tempString = tempString + writeLine.getGender() + ","
		if (writeLine.getType().equals("F")){
			tempString = tempString + writeLine.getSuperableSalary().toString() + "," + " "
		} else{
			tempString = tempString + " " + "," + writeLine.getSuperableSalary().toString()
		}
		ReportC.write(tempString+"\r\n")
	}

	/**
	 * Write TRBSASB Report Details
	 * @param writeLine is Report Line
	 */
	private void writeReportADetails(TrbsasaReportLine writeLine){
		info("writeReportADetails")
		String tempString = ""
		tempString = tempString + writeLine.getEmployeeId().padRight(13)
		tempString = tempString + writeLine.getMemberNo().padRight(12)
		tempString = tempString + writeLine.getLastName().padRight(33)
		tempString = tempString + writeLine.getType().padRight(7)
		tempString = tempString + writeLine.getGender().padRight(9)
		tempString = tempString + writeLine.getBirthDate().substring(6,8) + "/" + writeLine.getBirthDate().substring(4,6) + "/" + writeLine.getBirthDate().substring(2,4) + "   "
		tempString = tempString + decFormatter2.format(writeLine.getSuperableSalary()).padLeft(14) + "   "
		tempString = tempString + writeLine.getShftCategDesc().padRight(17)
		tempString = tempString + decFormatter.format(writeLine.getTtlShftWork()).padLeft(13)
		ReportA.write(tempString)
		recordCount++
	}

	/**
	 * Write Report Footer and close report
	 */
	private void printBatchReport(){
		info("printBatchReport")
		//Write Report A Footer
		ReportA.write(" ")
		ReportA.write("Total Number of Records: "+recordCount.toString())
		//Close Report
		ReportA.close()
		ReportB.close()
		ReportC.close()

		String medium = request.getPropertyString("Medium").trim()
		debug("Report Medium is ${medium}")
		if(!medium.equals("P")){
			if(taskUUID?.trim()) {
				debug("Adding CSV into Request.")
				request.request.CURRENT.get().addOutput(outputFileTRTSAS,
						"text/comma-separated-values", "TRTSAS");
			}
		}
	}

}

/*run script*/  
ProcessTrbsas process = new ProcessTrbsas();
process.runBatch(binding);
