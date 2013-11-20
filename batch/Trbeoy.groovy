/**
 * @AIT 2013
 * Conversion from trbeoy.cbl
 * 03/04/2013 RL - VERSION 1
 * 17/04/2013 RL - VERSION 2
 * 01/05/2013 RL - VERSION 3
 * 01/05/2013 RL - VERSION 4 - Fixed EDOI calls for restart
 * 01/05/2013 RL - VERSION 5 - Data fixes, make it pdf
 * 13/05/2013 RL - VERSION 6 - UAT
 * 16/06/2013 RL - VERSION 7 - Fixed incorrect amount. Need to accumulate amount for same earning/dedn codes.
 *               - also fixed up missing records for terminated employees within the financial year
 * 16/06/2013 RL - VERSION 8 - Fixed incorrect amount. 
 * 
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase

import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.rdl.report.layout.Line;
import com.mincom.ellipse.rdl.runner.ReportLine;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.ellipse.types.m0000.instances.EmployeeId;
import com.mincom.ellipse.unittest.framework.query.DataSource;

import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.common.unix.UnixTools;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf801.*
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf823.*;
import com.mincom.ellipse.edoi.ejb.msf837.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.Comparable;
import java.text.DecimalFormat
import java.awt.Font;



////-------------------------------------------------------------------
//import groovy.lang.Binding;
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//
//public class AITBatchEOY implements GroovyInterceptable {
//
//	private static final long REFRESH_TIME = 60 * 1000 * 5
//
//	public EDOIWrapper edoi
//	public EROIWrapper eroi
//	public ServiceWrapper service
//	public BatchWrapper batch;
//	public CommAreaScriptWrapper commarea;
//	public BatchEnvironment env
//    public UnixTools tools
//	public Reports report;
//	public Sort sort;
//	public Params params;
//	public RequestInterface request;
//	public Restart restart;
//
//	private String uuid;
//	private String taskUuid;
//
//	private Date lastDate;
//
//	private boolean disableInvokeMethod
//
//	public static final int SuperBatch_VERSION = 1;
//	public static final String SuperBatch_CUST = "TRAN1";
//
//	/**
//	 * Print a string into the logger.
//	 * @param value a string to be printed.
//	 */
//	public void info(String value){
//		def logObject = LoggerFactory.getLogger(getClass());
//		logObject.info("------------- " + value)
//	}
//	
//	public void debug(String value){
//		def logObject = LoggerFactory.getLogger(getClass());
//		logObject.debug("------------- " + value)
//	}
//
//	/**
//	 * Initialize the variables based on binding object.
//	 * @param b binding object
//	 */
//	public void init(Binding b) {
//		edoi = b.getVariable("edoi");
//		eroi = b.getVariable("eroi");
//		service = b.getVariable("service");
//		batch = b.getVariable("batch");
//		commarea = b.getVariable("commarea");
//		env = b.getVariable("env");
//		tools = b.getVariable("tools");
//		report = b.getVariable("report");
//		sort = b.getVariable("sort");
//		request = b.getVariable("request");
//		restart = b.getVariable("restart");
//		params = b.getVariable("params");
//
//		// gets the uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
//		uuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.getUUID();
//
//		// gets the task uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
//		taskUuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.request.getTaskUuid();
//		
//	}
//
//	/**
//	 *  Returns the uuid
//	 * @return String UUID
//	 */
//	public String getUUID() {
//		return uuid
//	}
//
//	/**
//	 *  Returns the task uuid from the parent
//	 * @return String UUID
//	 */
//	public String getTaskUUID() {
//		return taskUuid
//	}
//
//	/**
//	 * Print the version.
//	 */
//	public void printSuperBatchVersion(){
//		info ("SuperBatch Version:" + SuperBatch_VERSION);
//		info ("SuperBatch Customer:" + SuperBatch_CUST);
//	}
//
//	def invokeMethod(String name, args) {
//		if (!disableInvokeMethod) {
//			disableInvokeMethod = true;
//			try {
//				keepAliveConnection();
//			} finally {
//				disableInvokeMethod = false;
//			}
//		}
//		def result
//		def metaMethod = metaClass.getMetaMethod(name, args)
//		result = metaMethod.invoke(this, metaMethod.coerceArgumentsToClasses(args))
//		return result
//	}
//
//	protected void keepAliveConnection() {
//		if (lastDate == null) {
//			lastDate = new Date();
//		} else {
//			Date currentDate = new Date();
//			debug("Time elapsed  = " + (currentDate.getTime() - lastDate.getTime()))
//			debug("Time refresh  = " + REFRESH_TIME)
//			if ((currentDate.getTime() - lastDate.getTime()) > REFRESH_TIME ) {
//				lastDate = currentDate;
//				restartTransaction();
//			}
//		}
//	}
//
//	protected void restartTransaction() {
//		debug("restartTransaction")
//		(0..0).each restart.each(1, { debug("Restart Transaction") })
//		debug("end restart transaction")
//	}
//}
////-------------------------------------------------------------------
 
public class ParamsTrbeoy{
	//List of Input Parameters
	String paramPayLocation;
	String paramSortBy;
}

/**
 * YTD Payroll Earnings & Deductions Report <br>
 * <li> Upgrade from 2 existing reports (TRBEOY & TRBEOY) as part of the Ellipse 8 upgrade project. </li>
 * <li> This program will produce a batch report and a CSV file </li> 
 * <li> It will list employee's earnings and/or deductions details for the latest financial year </li> 
 **/

//public class ProcessTrbeoy extends AITBatchEOY {
public class ProcessTrbeoy extends SuperBatch {
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
	
	
	
    private version = 8;
    private ParamsTrbeoy batchParams;
	
	private String payer;
	private String payerABN;
	private String empId;
	private String surname;
	private String firstName;
	private String secondName;
	private String codeType;
	private String code;
	private String codeDesc;
	private BigDecimal units;
	private BigDecimal amounts;
	
	private ArrayList arrayOfTrbeoyReportLine  = new ArrayList();
	private ArrayList arrayOfTrbeoyCSVLine  = new ArrayList();
	
	//define constants
	private static final String txt1A = "Please find a list below of the total amounts paid and deducted from your salary for the financial year just ended.";
	private static final String txt1B = "This  information, where applicable, may assist you when completing your tax return.";
	 
	private static final String txt2 = "This earnings list includes non-taxable amounts which are not included " +
								  "in the gross salary appearing on your Payment Summary.";
								  
	//Report A - batch report
	private def ReportA;
	
	int ReportALineCounter = 0;
	
	private String workDir;
	
	//Report B - CSV file
	File ReportBFile;
	FileWriter ReportBStream;
	String ReportBPath;
	BufferedWriter ReportB;

	//Report C - PDF file
	String ReportCPath;
	Document ReportC;
	
	int recordCount823=0;
	int recordCount820=0;
	int recordCount837=0;
	
	//display fields format
	private DecimalFormat decFormatter = new DecimalFormat("###,###,##0.00");

	
	//define setters & getters for report
	private class TrbeoyaReportLine implements Comparable<TrbeoyaReportLine>
	{
		private String payer;
		private String payerABN;
		private String employeeId;
		private String surname;
		private String firstName;
		private String secondName;
		private String deductionEarn;
		private String codeType;
		private String code;
		private String codeDesc;
		private BigDecimal units;
		private BigDecimal amount;
		private String sortBy;
		
		public TrbeoyaReportLine(String newPayer,
								 String newPayerABN,
								 String newEmployeeId,
								 String newSurname,
								 String newFirstName,
								 String newSecondName,
								 String newDeductionEarn,
								 String newCodeType,
								 String newCode,
								 String newCodeDesc,
								 BigDecimal newUnits,
								 BigDecimal newAmount,
								 String newSortBy)
		{
//			info("TrbeoyaReportLine " + newEmployeeId + " - " + newFirstName + " " + newSurname + " - " + newCodeType + " " + newCode + " " + newCodeDesc + " - " + newAmount + " - " + newUnits + " : SORT=" + newSortBy);
//			info("newPayer        :" + newPayer);
//			info("newPayerABN     :" + newPayerABN);
//			info("newEmployeeId   :" + newEmployeeId);
//			info("newDeductionEarn:" + newDeductionEarn);
//			info("newCodeType     :" + newCodeType);
//			info("newCode         :" + newCode);
//			info("newCodeDesc     :" + newCodeDesc);
//			info("newUnits        :" + newUnits);
//			info("newAmount       :" + newAmount);
//			info("newSortBy       :" + newSortBy);
			
			setPayer(newPayer);
			setPayerABN(newPayerABN);
			setEmployeeId(newEmployeeId);
			setSurname(newSurname);
			setFirstName(newFirstName);
			setSecondName(newSecondName);
			setDeductionEarn(newDeductionEarn);
			setCodeType(newCodeType);
			setCode(newCode);
			setCodeDesc(newCodeDesc);
			setUnits(newUnits);
			setAmount(newAmount);
			setSortBy(newSortBy);
			
		}
					
		
		public void setPayer(String newPayer){
			payer = newPayer;
		}

		public String getPayer(){
			return payer;
		}
		
		public void setPayerABN(String newPayerABN){
			payerABN = newPayerABN;
		}
		
		public String getPayerABN(){
			return payerABN;
		}

		public String setSurname(String newSurname){
			surname = newSurname;
		}
		
		public String getSurname(){
			return surname;
		}

		public String setFirstName(String newFirstName){
			firstName = newFirstName;
		}
		
		public String getFirstName(){
			return firstName;
		}

		public String setSecondName(String newSecondName){
			secondName = newSecondName;
		}
		
		public String getSecondName(){
			return secondName;
		}
		
		public void setEmployeeId(String newEmployeeId){
			employeeId = newEmployeeId;
		}
		
		public String getEmployeeId(){
			return employeeId;
		}
		
		public void setDeductionEarn(String newDeductionEarn){
			deductionEarn = newDeductionEarn;
		}
		
		public String getDeductionEarn(){
			return deductionEarn;
		}
		
		public void setCodeType(String newCodeType){
			codeType = newCodeType;
		}
		
		public String getCodeType(){
			return codeType;
		}
		
		public void setCode(String newCode){
			code = newCode;
		}
		
		public String getCode(){
			return code;
		}

		public void setCodeDesc(String newCodeDesc){
			codeDesc = newCodeDesc;
		}
		
		public String getCodeDesc(){
			return codeDesc;
		}
	
		public void setAmount(BigDecimal newAmount){
			amount = newAmount;
		}
		
		public BigDecimal getAmount(){
			return amount;
		}

		public void setUnits(BigDecimal newUnits){
			units = newUnits;
		}
		
		public BigDecimal getUnits(){
			return units;
		}

		public void setSortBy(String newSortBy){
			sortBy = newSortBy;
		}

		public int compareTo(TrbeoyaReportLine otherReportLine)
		{
			//info("SORTING :" + sortBy);
			if(sortBy.equals("E"))
			{
				if (!employeeId.equals(otherReportLine.getEmployeeId())){
					return employeeId.compareTo(otherReportLine.getEmployeeId())
				}
				if (!surname.equals(otherReportLine.getSurname())){
					return surname.compareTo(otherReportLine.getSurname())
				}
				if (!deductionEarn.equals(otherReportLine.getDeductionEarn())){
					return otherReportLine.getDeductionEarn().compareTo(deductionEarn)
				}
				if (!code.equals(otherReportLine.getCode())){
					return code.compareTo(otherReportLine.getCode())
				}
				return 0;
			}
			else
			{
				if (!surname.equals(otherReportLine.getSurname())){
					return surname.compareTo(otherReportLine.getSurname())
				}
				if (!employeeId.equals(otherReportLine.getEmployeeId())){
					return employeeId.compareTo(otherReportLine.getEmployeeId())
				}
				if (!deductionEarn.equals(otherReportLine.getDeductionEarn())){
					return otherReportLine.getDeductionEarn().compareTo(deductionEarn)
				}
				if (!code.equals(otherReportLine.getCode())){
					return code.compareTo(otherReportLine.getCode())
				}
				return 0;
				 
			}
		}											
	}
	
    public void runBatch(Binding b){            
        
        init(b);
		info("uuid         :" + getUUID());
		
		workDir = env.getWorkDir().toString() + "/";
		ReportBPath = workDir +"TRTEOY" + "." + taskUUID + ".csv";
		ReportBFile = new File(ReportBPath);
		ReportBStream = new FileWriter(ReportBFile)
		ReportB = new BufferedWriter(ReportBStream) 
		info("ReportBPath   :" + ReportBPath);

		ReportCPath = workDir +"TRTEOY" + "." + taskUUID + ".pdf";
		ReportC = new Document();
		PdfWriter.getInstance(ReportC, new FileOutputStream(ReportCPath));
		info("ReportCPath   :" + ReportCPath);

		
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        
        batchParams = params.fill(new ParamsTrbeoy())
		            
        //PrintRequest Parameters
		info("paramPayLocation    : " + batchParams.paramPayLocation);
		info("paramSortBy         : " + batchParams.paramSortBy);
													
        try 
		{
			processBatch();
			info("processBatch Complete");
        } 
		finally 
		{
        	printBatchReport();
        }
    }
	
	
    private void processBatch()
	{
        info("processBatch");           
		
		if(initialise_B000())
		{
			processRequest_C000();
			generateTrbeoyaReport_E000();
		}
		ReportA.close();
    }
    
	private boolean initialise_B000(){
		info("initialise_B000");

		ReportA = report.open('TRBEOYA')
			
		if (!batchParams.paramSortBy.trim().equals("E") && !batchParams.paramSortBy.trim().equals("S"))
		{
			WriteError ("Input must be E or S");
			return false;
		}
		
		ReportB.write("Employee ID,Surname,First Name,Second Name,Type,Code,Description,Units,Amount\n");	
		ReportC.open();
		return true;
		

	}
	private void WriteError(String msg)
	{
		ReportA.write(msg);
		info (msg);

	}
	private void processRequest_C000()
	{
		info("processRequest_C000");
		
		String sPayLocation;
		boolean payLocFound;

		int numberOfEmployee = 0;
		
		try 
		{	
			//retrieve employees from all pay locations
			if (batchParams.paramPayLocation.trim().equals(""))
			{
				// Find pay locations from PAYL table
				Constraint c1 = MSF010Key.tableType.equalTo("PAYL")
				def query = new QueryImpl(MSF010Rec.class).and(c1).orderBy(MSF010Rec.msf010Key);
								 						   
				MSF010Key msf010key = new MSF010Key()
				edoi.search(query,10000,{MSF010Rec msf010RecRead ->
					if (msf010RecRead)
					{
						
						sPayLocation = msf010RecRead.getPrimaryKey().getTableCode()
						info("sPayLocation  : " + sPayLocation);
						
						Constraint c2 = MSF820Rec.payLocation.equalTo(sPayLocation)
						query = new QueryImpl(MSF820Rec.class).and(c2).orderBy(MSF820Rec.msf820Key);
						
						edoi.search(query,10000,{MSF820Rec msf820Rec ->
							recordCount820++;
							processEarnDedn(msf820Rec.getPrimaryKey().getEmployeeId(),msf820Rec.getPayGroup());
						})
					}
				})
			}
			else 
			{
				// Validate the specific pay location if entered
				
				//payLocFound = true;
				MSF010Key msf010key = new MSF010Key()
				msf010key.setTableType("PAYL")
				msf010key.setTableCode(batchParams.paramPayLocation)
				MSF010Rec msf010RecRead = edoi.findByPrimaryKey(msf010key)
				
				// process this pay location only
				sPayLocation = msf010RecRead.getPrimaryKey().getTableCode()
				info("sPayLocation  : " + sPayLocation);
					
				Constraint c1 = MSF820Rec.payLocation.equalTo(sPayLocation);
				def query = new QueryImpl(MSF820Rec.class).and(c1).orderBy(MSF820Rec.msf820Key);
					
				edoi.search(query,10000,{MSF820Rec msf820Rec ->
					recordCount820++
					processEarnDedn(msf820Rec.getPrimaryKey().getEmployeeId(),msf820Rec.getPayGroup());
				})
			}
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			info ("Pay Location "+ batchParams.paramPayLocation + " Not Found")
			e.printStackTrace();
			info("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			info ("processRequest_C000 Error: " + ex.message);
		}
	}

	/**
	 * get employee's earnings/deduction details. <br>
	 **/
	private void processEarnDedn(String empId820, String payGrp820)
	{
//		info ("processEarnDedn");		
//		String empStatus;
		
		try
		{
			//16/06/13 ignore 760 status. print all with $ amount
			//check for active status
			//empStatus = processMSF760(empId820);
			
			//if (empStatus.trim().equals("A"))
			//{
				//get payer information
				processMSF801(payGrp820);
				
				//get employee's name
				getEmpName(empId820);
				
				//get earning codes and write to list
				processMSF823(empId820);
				
				//get deduction codes and write to list
				processMSF837(empId820);
			//}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processEarnDedn failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			info ("processEarnDedn Error: " + ex.message);
		}

		
	}

	/**
	 * Read MSF801 to get payer's details. <br>
	 **/
	private String processMSF801 (String payGrp820)
	{
//		info ("processMSF801");
		String result;
		
		try
		{
			//get payer's name and ABN
			Constraint c1 = MSF801_PG_801Key.cntlRecType.equalTo("PG");
			Constraint c2 = MSF801_PG_801Key.cntlKeyRest.equalTo(payGrp820);
			def query = new QueryImpl(MSF801_PG_801Rec.class). and(c1).and(c2);
			MSF801_PG_801Rec msf801Rec_PG = (MSF801_PG_801Rec) edoi.firstRow(query);
			
			if (msf801Rec_PG)
			{
				payer = msf801Rec_PG.getGrpTaxNmePg();
				payerABN = msf801Rec_PG.getGrpTaxNoPg();
			} 
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processMSF801 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			info ("processMSF801 Error: " + ex.message);
		}

	}
	
	/**
	 * Read MSF760 to get employee's personnel details. <br>
	 **/
//	private String processMSF760 (String empId820)
//	{
//		info ("processMSF760");
//		String result;
//		
//		try
//		{
//			Constraint c1 = MSF760Key.employeeId.equalTo(empId820);
//			def query = new QueryImpl(MSF760Rec.class).and (c1);
//			MSF760Rec msf760Rec = (MSF760Rec) edoi.firstRow(query);
//			
//			if (msf760Rec)
//			{
//				result = msf760Rec.getEmpStatus();
//			}
// 			
//		}
//		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
//		{
//			e.printStackTrace();
//			info("processMSF760 failed - ${e.getMessage()}");
//		}
//		catch (Exception ex)
//		{
//			info ("processMSF760 Error: " + ex.message);
//		}
//		return result;
//	}
	
	/**
	 * Read MSF810 to get employee's name. <br>
	 **/
	private void getEmpName(String empId820)
	{
//		info("Process getEmpName");
		surname = " ";
		firstName = " ";
		
		try
		{
			MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(empId820));
			surname = msf810Rec.getSurname();
			firstName = msf810Rec.getFirstName();
			secondName = msf810Rec.getSecondName();
			 
			
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			info ("Employee Name not found for "+ empId820)
			e.printStackTrace();
			info("getEmpName failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			info ("getEmpName Error: " + ex.message);
		}
	}

	/**
	 * Read MSF823 to get employee's YTD earning codes. <br>
	 **/
	private void processMSF823(String empId820)
	{
//		info("processMSF823");
		String desc = "";
		//16/06/13
		String prvEarnCode;
		BigDecimal prvFisAmt;
		BigDecimal prvFisUnits;
		Integer i;
		BigDecimal zero = 0.0;
		try
		{
			//16/06/13 
			prvEarnCode = "";
			prvFisAmt = 0;
			prvFisUnits = 0;
			i = 1;
			//leave blank for the latest YTD
			Constraint c1 = MSF823Key.consPayGrp.equalTo(" ");
			Constraint c2 = MSF823Key.employeeId.equalTo(empId820);
			Constraint c3 = MSF823Key.earnCode.greaterThanEqualTo("000");
			def query = new QueryImpl(MSF823Rec.class).and(c1).and(c2).and(c3).orderBy(MSF823Rec.msf823Key);
			
			edoi.search(query,10000,{MSF823Rec msf823Rec ->
				
				//info("Employee Id:" + empId820 + " - EarnCode:" + msf823Rec.getPrimaryKey().getEarnCode()+ " - Units:" + msf823Rec.getPrvFisUnits() + " - Amount:" + msf823Rec.getPrvFisAmtL());
				
				if(!msf823Rec.getPrimaryKey().getEarnCode().equals("000"))
				{			
					//if (msf823Rec.getPrvFisUnits() != zero || msf823Rec.getPrvFisAmtL() != zero)
					if (msf823Rec.getPrvFisAmtL() != zero)
					{
						//get code Description & type
						//getDednEarn801("E",msf823Rec.getPrimaryKey().getEarnCode());
						
						//16/06/13 RL - accumulate amount for same earning code
						if (!msf823Rec.getPrimaryKey().earnCode.equals(prvEarnCode)){
							if (i == 1){
								prvFisAmt = msf823Rec.getPrvFisAmtL();
								prvFisUnits = msf823Rec.getPrvFisUnits();
							}
							else{
								//get code Description & type
								getDednEarn801("E",prvEarnCode);
								
								AddLine(new TrbeoyaReportLine(payer,
									payerABN,
									empId820,
									surname,
									firstName,
									secondName,
									"Earnings",
									codeType,
									//msf823Rec.getPrimaryKey().getEarnCode(),
									prvEarnCode,
									codeDesc,
									//msf823Rec.getPrvFisUnits(),
									//msf823Rec.getPrvFisAmtL(),
									prvFisUnits,
									prvFisAmt,
									//msf823Rec.getCurFisUnits(),
									//msf823Rec.getCurFisAmtL(),
									batchParams.paramSortBy));							
								prvFisAmt = msf823Rec.getPrvFisAmtL();
								prvFisUnits = msf823Rec.getPrvFisUnits();
								i = 1;
							}
							prvEarnCode = msf823Rec.getPrimaryKey().earnCode;							
						}
						else
						{
							prvFisAmt = prvFisAmt + msf823Rec.getPrvFisAmtL();
							prvFisUnits = prvFisUnits + msf823Rec.getPrvFisUnits();
						}
						recordCount823++;
						i++;
					}				
				}
			})	
			//add the last line to array
			if (prvFisAmt != zero){				
				getDednEarn801("E",prvEarnCode);			
				AddLine(new TrbeoyaReportLine(payer,
					payerABN,
					empId820,
					surname,
					firstName,
					secondName,
					"Earnings",
					codeType,
					prvEarnCode,
					codeDesc,
					prvFisUnits,
					prvFisAmt,
					batchParams.paramSortBy));
			}
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			info("getEmpName failed - ${e.getMessage()}");
			e.printStackTrace();
		}
		catch (Exception ex)
		{
			info ("getEmpName Error: " + ex.message);
		}

	}
	private void AddLine(TrbeoyaReportLine line)
	{
		arrayOfTrbeoyReportLine.add(line);
	}

	/**
	 * Read MSF837 to get employee's YTD deduction codes. <br>
	 **/
	private void processMSF837(String empId820)
	{
//		info("processMSF837");
		String desc = "";
		//16/06/13
		String prvDednCode;
		BigDecimal prvFisAmt;
		BigDecimal prvFisUnits;
		Integer i;
		
		BigDecimal zero = 0.0;
		
		try
		{
			//16/06/13
			prvDednCode = "";
			prvFisAmt = 0;
			prvFisUnits = 0;
			i = 1
			//leave blank for the latest YTD
			Constraint c5 = MSF837Key.consPayGrp.equalTo(" ");
			Constraint c6 = MSF837Key.employeeId.equalTo(empId820);
			Constraint c7 = MSF837Key.dednCode.greaterThanEqualTo("000");
			def query = new QueryImpl(MSF837Rec.class).and(c5).and(c6).and(c7).orderBy(MSF837Rec.msf837Key);
			
			edoi.search(query,10000,{MSF837Rec msf837Rec ->
				if(!msf837Rec.getPrimaryKey().getDednCode().equals("000"))
				{
					//get code Description & type
					//getDednEarn801("D",msf837Rec.getPrimaryKey().getDednCode());	

					//16/06/13 RL - accumulate amount for same deduction code
					//if (msf837Rec.getPrvFisUnits() != zero || msf837Rec.getPrvFisAmtL() != zero)
					if (msf837Rec.getPrvFisAmtL() != zero)
					{
						if (!msf837Rec.getPrimaryKey().dednCode.equals(prvDednCode)){
							if (i == 1){
								prvFisAmt = msf837Rec.getPrvFisAmtL();
								prvFisUnits = msf837Rec.getPrvFisUnits();
							}
							else
							{
								//get code Description & type
								getDednEarn801("D",prvDednCode);
								
								AddLine(new TrbeoyaReportLine(payer,
									payerABN,
									empId820,
									surname,
									firstName,
									secondName,
									"Deductions",
									codeType,
									//msf837Rec.getPrimaryKey().getDednCode(),
									prvDednCode,
									codeDesc,
									//msf837Rec.getPrvFisUnits(),
									//msf837Rec.getPrvFisAmtL(),
									prvFisUnits,
									prvFisAmt,
									//msf837Rec.getCurFisUnits(),
									//msf837Rec.getCurFisAmtL(),
									batchParams.paramSortBy));								
								prvFisAmt = msf837Rec.getPrvFisAmtL();
								prvFisUnits = msf837Rec.getPrvFisUnits();
								i = 1;
							}
							prvDednCode = msf837Rec.getPrimaryKey().dednCode;
						}
						else
						{
							prvFisAmt = prvFisAmt + msf837Rec.getPrvFisAmtL();
							prvFisUnits = prvFisUnits + msf837Rec.getPrvFisUnits();
						}
						recordCount837++;
						i++;
					}
				}
			})
			//add the last line to array
			if (prvFisAmt != zero){				
				getDednEarn801("D",prvDednCode);
				AddLine(new TrbeoyaReportLine(payer,
					payerABN,
					empId820,
					surname,
					firstName,
					secondName,
					"Deductions",
					codeType,
					prvDednCode,
					codeDesc,
					prvFisUnits,
					prvFisAmt,
					batchParams.paramSortBy));	
			}
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processMSF837 failed - ${e.getMessage()}");
		}					
		catch (Exception ex)
		{
			info ("processMSF837 Error: " + ex.message);
		}
	}

	/**
	 * Read MSF801 to get earning /deduction code's description & type. <br>
	 **/
	private void getDednEarn801(String cdeType, String code823)
	{
//		info("Process getDednEarn801");
		
		try
		{ 
			
			if(cdeType.equals("E"))
			{
				MSF801_A_801Rec msf801_a_801rec = edoi.findByPrimaryKey(new MSF801_A_801Key("A", "***"+code823));
				codeDesc = msf801_a_801rec.getTnameA();
				codeType = msf801_a_801rec.getEarnTypeA();
				return;
			}
			else
			{
				if(cdeType.equals("D"))
				{
					MSF801_D_801Rec msf801_d_801rec = edoi.findByPrimaryKey(new MSF801_D_801Key("D", "***"+code823));
					codeDesc = msf801_d_801rec.getTnameD();
					codeType = msf801_d_801rec.getDedTypeD();
					return;
				}
			} 
			return;
		}
		catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			info ("Description not found for "+ code823)
			e.printStackTrace();
			info("getDednEarn801 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			info ("getDednEarn801 Error: " + ex.message);
		}
	}
	
	private void generateTrbeoyaReport_E000()
	{
		info("generateTrbeoyaReport_E000");
		 
		Collections.sort(arrayOfTrbeoyReportLine);
		 
		
		ReportHeader rpt = new ReportHeader();
		for (int i = 0; i < arrayOfTrbeoyReportLine.size(); i++)
		{
			TrbeoyaReportLine line = arrayOfTrbeoyReportLine.get(i);
			rpt.AddLine(line);
		}
 		rpt.print();
 		
		// now print the csv
		// Always sort by E in the CSV
 		for (int i = 0; i < arrayOfTrbeoyReportLine.size(); i++)
		{
			TrbeoyaReportLine line = arrayOfTrbeoyReportLine.get(i);
			line.setSortBy("E");
			arrayOfTrbeoyCSVLine.add(line);
		}
  
		Collections.sort(arrayOfTrbeoyCSVLine);

		rpt = new ReportHeader();
		for (int i = 0; i < arrayOfTrbeoyCSVLine.size(); i++)
		{
			TrbeoyaReportLine line = arrayOfTrbeoyCSVLine.get(i);
			rpt.AddLine(line);
		}
		rpt.printCSV();
		
		ReportB.close();
		ReportC.close();
		
	}
		
    private void printBatchReport()
	{
        info("printBatchReport")
		
        //print batch report
		info("No of 820 record found : "+recordCount820.toString());
		info("No of 823 record found : "+recordCount823.toString());
		info("No of 837 record found : "+recordCount837.toString());
	}
	
	
	//GRAND TOTAL
	public class ReportHeader
	{
		public ArrayList<ReportEmployee> Employees;
		
		public ReportHeader()
		{
			Employees = new ArrayList<ReportEmployee>();
		}
		
		public void AddLine(TrbeoyaReportLine line)
		{
			String key = line.getEmployeeId();
			
			for (int i = 0; i < Employees.size(); i++)
			{
				ReportEmployee s = Employees[i];
				if (s.getKey().equals(key))
				{
					s.AddLine(line);
					return;
				}
			}
			ReportEmployee s = new ReportEmployee(line);
			Employees.add(s);
		}
		
		public void print()
		{
			// Now print
			for (int i = 0; i < Employees.size(); i++)
			{
				ReportEmployee s = Employees[i];
				s.print();
			}
			
		}
		public void printCSV()
		{
			// Now print
			for (int i = 0; i < Employees.size(); i++)
			{
				ReportEmployee s = Employees[i];
				s.printCSV();
			}
		}
	}
	
	public class ReportEmployee
	{
		public ArrayList<ReportDetail> Earnings;
		public ArrayList<ReportDetail> Deductions;
		
		public String EmployeId = "";
		public String Surname = "";
		public String FirstName = "";
		public String SecondName = "";
		public String Payee = "";
		public String PayeeABN = "";
		
		public String FormattedEmployeeId()
		{
			String s = EmployeId;
			s = s.replaceFirst ("^0*", "");
			return s;
		}
		
		public ReportEmployee()
		{
			Earnings = new ArrayList<ReportDetail>();
			Deductions = new ArrayList<ReportDetail>(); 
		}
		
		public ReportEmployee(TrbeoyaReportLine line)
		{
			Earnings = new ArrayList<ReportDetail>();
			Deductions = new ArrayList<ReportDetail>();
			
			EmployeId = line.getEmployeeId();
			Surname = line.getSurname();
			FirstName = line.getFirstName();
			SecondName = line.getSecondName();
			Payee = line.getPayer();
			PayeeABN = line.getPayerABN();
			
			AddLine(line);
		}
		public String getKey()
		{
			return EmployeId;
		}
		public void AddLine(TrbeoyaReportLine line)
		{
			
			if (line.getDeductionEarn().equals("Earnings"))
				AddEarningsLine(line);
			else
				AddDeductionLine(line);
		}
		public void AddEarningsLine(TrbeoyaReportLine line)
		{
			String key = line.getCode();
			for (int i = 0; i < Earnings.size(); i++)
			{
				ReportDetail s = Earnings[i];
				if (s.getKey().equals(key))
				{
					s.AddLine(line);
					return;
				}
			}
			ReportDetail s = new ReportDetail(line);
			Earnings.add(s);
		}
		public void AddDeductionLine(TrbeoyaReportLine line)
		{
			String key = line.getCode();
			for (int i = 0; i < Deductions.size(); i++)
			{
				ReportDetail s = Deductions[i];
				if (s.getKey().equals(key))
				{
					s.AddLine(line);
					return;
				}
			}
			ReportDetail s = new ReportDetail(line);
			Deductions.add(s);
		}
		
		public void print()
		{
			
			// Employee Header
			printEmployeeHeader();
			
			// Earnings header
			printEarningsHeader();
			
			com.lowagie.text.Font boldFont = FontFactory.getFont("Arial", 8,Font.BOLD);
			
			PdfPTable earnTable = new PdfPTable(3);
			earnTable.getDefaultCell().setBorder(0);
			earnTable.setWidthPercentage(100f);
			float[] a = [10f, 20f, 10f]
			earnTable.setWidths(a);
			
			PdfPCell c1 = new PdfPCell(new Phrase("Earnings Code No",boldFont));
			c1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			earnTable.addCell(c1);
		
			c1 = new PdfPCell(new Phrase("Description",boldFont));
			c1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			earnTable.addCell(c1);
		
			c1 = new PdfPCell(new Phrase("Amount",boldFont));
			c1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
			c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
			earnTable.addCell(c1);
			earnTable.setHeaderRows(1);
			
			// Now print Earnings
			for (int i = 0; i < Earnings.size(); i++)
			{
				ReportDetail s = Earnings[i];
				s.print();
				s.printPDF(earnTable);
			}
			ReportC.add(earnTable);
			ReportC.add(new Paragraph(" "));
			
			// Deductions header
			printDeductionsHeader();
			
			
			PdfPTable dedTable = new PdfPTable(3);
			earnTable.getDefaultCell().setBorder(0);
			dedTable.setWidthPercentage(100f);
			dedTable.setWidths(a);
			
			c1 = new PdfPCell(new Phrase("Deductions Code No",boldFont));
			c1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dedTable.addCell(c1);
		
			c1 = new PdfPCell(new Phrase("Description",boldFont));
			c1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dedTable.addCell(c1);
		
			c1 = new PdfPCell(new Phrase("Amount",boldFont));
			c1.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
			c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
			dedTable.addCell(c1);
			dedTable.setHeaderRows(1);
			
			// Now print Deductions
			for (int i = 0; i < Deductions.size(); i++)
			{
				ReportDetail s = Deductions[i];
				s.print();
				s.printPDF(dedTable);
			}
			ReportC.add(dedTable);
			ReportC.add(new Paragraph(" "));
			
			printEmployeeFooter();
			
		}
		public void printCSV()
		{
			// Now print Earnings
			for (int i = 0; i < Earnings.size(); i++)
			{
				ReportDetail s = Earnings[i];
				s.printCSV();
			}
			
			// Now print Deductions
			for (int i = 0; i < Deductions.size(); i++)
			{
				ReportDetail s = Deductions[i];
				s.printCSV();
			}
		}
		public void DoReportA(String line)
		{
			ReportA.write(line);
		}

		public void printEmployeeHeader()
		{
			String tempString = "";
			
			////////////////////////////// TEXT
			ReportA.writeLine(132,"=");
			
			DoReportA("                                                YTD Payroll Earnings & Deductions Report");
			DoReportA(" ");

			
			tempString = "Payer's Name: "+ Payee.padRight(25);
			tempString = tempString+"Payer's ABN:  "+ PayeeABN.padRight(16);
			tempString = tempString+"Employee ID: "+ FormattedEmployeeId().padRight(15);
			tempString = tempString+"Name: " + Surname + "," + FirstName;
			DoReportA("  " + tempString);
			
			DoReportA(" ");
			DoReportA("  " + txt1A);
			DoReportA("  " + txt1B);
			DoReportA(" ");
			DoReportA("  " + txt2);
			DoReportA(" ");
			
			//////////////////////////// PDF
			com.lowagie.text.Font titleFont = FontFactory.getFont("Arial", 18, Font.BOLD);
			com.lowagie.text.Font boldFont = FontFactory.getFont("Arial", 8,Font.BOLD);
			com.lowagie.text.Font normalFont = FontFactory.getFont("Arial", 8);
			
			Paragraph title = new Paragraph("YTD Payroll Earnings & Deductions Report",titleFont);
			title.setAlignment(Element.ALIGN_CENTER);
			ReportC.add(title);
			
			//ReportC.add(new Paragraph("                                                YTD Payroll Earnings & Deductions Report"));
			tempString = "\nPayer's Name: "+ Payee.padRight(25);
			tempString = tempString+"Payer's ABN:  "+ PayeeABN.padRight(16);
			tempString = tempString+"Employee ID: "+ FormattedEmployeeId().padRight(15);
			tempString = tempString+"Name: " + Surname + "," + FirstName;
			
			Paragraph linepara = new Paragraph(tempString,boldFont);
			ReportC.add(linepara);
			
			linepara = new Paragraph("\n" + txt1A + " " + txt1B + "\n\n" + txt2 + "\n\n",boldFont);
			ReportC.add(linepara);

		}
		public void printEmployeeFooter()
		{
			////////////////////////////// TEXT
			ReportA.writeLine(132,"=");
			ReportA.write(" ")
			//ReportA.write("\f")
	
			//////////////////////////// PDF
			com.lowagie.text.Font normalFont = FontFactory.getFont("Arial", 8);
			ReportC.add(new LineSeparator());
			//ReportC.add(new Paragraph(" "));
			ReportC.add(new Paragraph("Payroll Services",normalFont));
			ReportC.add(new Paragraph(new Date().format("dd/MM/yyyy"),normalFont));
			//ReportC.add( Chunk.NEWLINE );
			ReportC.add(new Paragraph(new Phrase(Chunk.NEXTPAGE)));
			

		}
		public void printEarningsHeader()
		{
			////////////////////////////// TEXT
			String tempString = "";
			ReportA.writeLine(132,"=");
			tempString = ("Earnings Code No").padRight(22);
			tempString = tempString + ("Description").padRight(35);
			tempString = tempString + ("Amount").padLeft(20);
			DoReportA("  " + tempString);
			ReportA.writeLine(132,"=");
			

			
		}
		public void printDeductionsHeader()
		{
			////////////////////////////// TEXT
			String tempString = "";
			ReportA.writeLine(132,"=");
			tempString = ("Deductions Code No").padRight(22);
			tempString = tempString + "Description".padRight(35);
			tempString = tempString + "Amount".padLeft(20);
			DoReportA("  " + tempString);
			ReportA.writeLine(132, "=");
			
			
		}
	}
	
	public class ReportDetail
	{
		public String deductionEarn = "";
		public String code = "";
		public String codeType = "";
		public String codeDesc = "";
		public BigDecimal units = 0;
		public BigDecimal amount = 0;

		public String EmployeId = "";
		public String Surname = "";
		public String FirstName = "";
		public String SecondName = "";
		public String Payee = "";
		public String PayeeABN = "";
		
		public ReportDetail()
		{
			units = 0;
			amount = 0;
		}
		public ReportDetail(TrbeoyaReportLine line)
		{
			deductionEarn = line.getDeductionEarn();
			codeType = line.getCodeType();
			codeDesc = line.getCodeDesc();
			code = line.getCode();
			units = line.getUnits();
			amount = line.getAmount();
			
			EmployeId = line.getEmployeeId();
			Surname = line.getSurname();
			FirstName = line.getFirstName();
			SecondName = line.getSecondName();
			Payee = line.getPayer();
			PayeeABN = line.getPayerABN();
			 
			
			
			if (deductionEarn == null) deductionEarn = "";
			if (codeType == null) codeType = "";
			if (codeDesc == null) codeDesc = "";
			if (code == null) code = "";
			
			
		}
		public void AddLine(TrbeoyaReportLine line)
		{
			
			units = line.getUnits();
			amount = line.getAmount();
		}
		public String getKey()
		{
			return code;
		}
		public void print()
		{
			
			String tempString;
			
			tempString = "  ";
			tempString = tempString + (code).padRight(22);
			tempString = tempString + (codeDesc).padRight(35);
			tempString = tempString + decFormatter.format(amount).padLeft(20);
			
			ReportA.write(tempString);
		}
		public void printPDF(PdfPTable table)
		{
			
			com.lowagie.text.Font normalFont = FontFactory.getFont("Arial", 8);
			
			PdfPCell c1 = new PdfPCell(new Phrase(code,normalFont));
			c1.setBorder(Rectangle.NO_BORDER);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			table.addCell(c1);
			
			c1 = new PdfPCell(new Phrase(codeDesc,normalFont));
			c1.setBorder(Rectangle.NO_BORDER);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			table.addCell(c1);
			
			String amountField = decFormatter.format(amount).toString();
			c1 = new PdfPCell(new Phrase(amountField,normalFont));
			c1.setBorder(Rectangle.NO_BORDER);
			c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(c1);

		}
		public void printCSV()
		{
			String APP = "\"";
			
			ReportB.write(APP + EmployeId + APP + "," +
				APP + Surname + APP + "," +
				APP + FirstName + APP + ","+
				APP + SecondName + APP +  "," +
				APP + codeType + APP + "," +
				APP + code + APP + "," +
				APP + " " + codeDesc + APP + "," +
				units.toString() + "," +
				amount.toString() + "\n"); 
		}
	}
	
}
        
/*run script*/  
ProcessTrbeoy process = new ProcessTrbeoy();
process.runBatch(binding);
