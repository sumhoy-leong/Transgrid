/**
 * @AIT 2013
 * Conversion from trb8al.cbl
 * 01/05/2013 RL - VERSION 1
 * 06/06/2013 RL - VERSION 2 - Fix to component rates (getDailyWklyWR - Use Calc rate instead of Base rate)
 * 06/06/2013 RL - VERSION 3 - Fix to index in report writing routine
 * 
 */package com.mincom.ellipse.script.custom

 import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Image;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import com.mincom.eilib.EllipseEnvironment;
//import com.mincom.batch.RequestDefinition;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.ellipse.common.unix.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf002.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf766.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf830.*;
import com.mincom.ellipse.edoi.ejb.msf835.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf803.*;
import com.mincom.ellipse.edoi.ejb.msf785.*;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Key;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Rec;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Key;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Rec;
import com.mincom.ellipse.edoi.ejb.msf880.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf828.*;
import com.mincom.ellipse.eroi.linkage.mss880.*;
import com.mincom.ellipse.eroi.linkage.mss801.*;
import com.mincom.ellipse.eroi.linkage.mssdat.*;
import com.mincom.ellipse.eroi.linkage.msslve.*;
import com.mincom.ellipse.eroi.linkage.mssrat.*;
import com.mincom.ellipse.errors.exceptions.*;

import org.apache.commons.lang.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.Comparable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Formatter;
import java.awt.Font;
import java.text.SimpleDateFormat;


////-------------------------------------------------------------------
//import groovy.lang.Binding;
//
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
////import com.mincom.ellipse.common.unix.*;
////import com.mincom.ellipse.script.util.*;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;
//
//public class AITBatch8AL implements GroovyInterceptable {
//
//	private static final long REFRESH_TIME = 60 * 1000 * 5
//
//	public EDOIWrapper edoi
//	public EROIWrapper eroi
//	public ServiceWrapper service
//	public BatchWrapper batch;
//	public CommAreaScriptWrapper commarea;
//	public BatchEnvironment env
//	public UnixTools tools
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
//	public static final int SuperBatch_VERSION = 5;
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

public class ParamsTrb8al
{
	//List of Input Parameters
	String paramLeaveType;
	String paramProjectedDate;
	String paramIncExcSw;
	String paramUnitType;
	String paramEmployeeCat;
}

/**
 * Projected Leave Liability report <br>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
 * <li> input parameters: Leave Type - mandatory </li>
 * <li> 				  Projected Date - optional </li>
 * <li> 				  Include/Exclude employees with < 5 years - mandatory </li>
 * <li> 				  Unit Type - mandatory </li>
 * <li> 				  Employee Category - optional </li>
 * <li> output: a CSV file. </li>
 * <li> This will report projected annual liability to a CSV file. </li>
// **/
//public class ProcessTrb8al extends AITBatch8AL
public class ProcessTrb8al extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 3;
	private ParamsTrb8al batchParams;
	
	private String projectedDate;
	private String empId;
	private String empSurname;
	private String empFirstInit;
	private String empStatus;
	private	String staffCateg;
	private	String serviceDate;
	private String empType;
	private	String gender;
	private	String birthDate;
	private	String hireDate;
	private	String union;
	private String empClass;
	private String empClassD;
	private BigDecimal empShiftCat;
	private String empPayGrp;
	private String PRC;
	private BigDecimal dailyWageRate;
	private BigDecimal weeklyWageRate;
	private BigDecimal empPackage;
	private BigDecimal lveAccrual;
	private BigDecimal lveEntitlement;
	private BigDecimal lveAccrualValue;
	private BigDecimal lveEntitlementValue;
	private BigDecimal hrsDaysFac;
	private String lastPayRunDate;
	private String lastPayRunStDate; 
	
	private ArrayList arrayOfTrb8alReportLine = new ArrayList();

	Date sTranDateD;
	String sTranDate;

	private String workDir;
	
	//Report A - batch report
	private def ReportA;

	//CVS file A - details report
	File ReportBFile;
	FileWriter ReportBStream;
	String ReportBPath;
	BufferedWriter ReportB;		// = new BufferedWriter(fstream1)
	
	
	//CVS file - details report
	//File newFile1 = File.createTempFile("TRT8ALA", ".csv");
	//FileWriter fstream1 = new FileWriter(newFile1)
	//BufferedWriter ReportB = new BufferedWriter(fstream1)

	private DecimalFormat decFormatter = new DecimalFormat("################0.00");
	private DecimalFormat decFormatterNoDec = new DecimalFormat("################0");
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat disDateFormat = new SimpleDateFormat("dd/MM/yyyy");
	
	/**
	 * Run the main batch.
	 * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b)
	{
		init(b);
		ProcessTrb8al.printinfo("VERSION      :" + version);
		ProcessTrb8al.printinfo("uuid         :" + getUUID());
		
		workDir = env.getWorkDir().toString() + "/";
		
		ReportBPath = workDir +"TRT8AL" + "." + taskUUID + ".csv";
		ReportBFile = new File(ReportBPath);
		ReportBStream = new FileWriter(ReportBFile)
		ReportB = new BufferedWriter(ReportBStream)
		ProcessTrb8al.printinfo("ReportBPath   :" + ReportBPath);
		
		printSuperBatchVersion();
		
		ProcessTrb8al.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb8al());

		//PrintRequest Parameters
		ProcessTrb8al.printinfo("paramLeaveType     : " + batchParams.paramLeaveType);
		ProcessTrb8al.printinfo("paramProjectedDate : " + batchParams.paramProjectedDate);
		ProcessTrb8al.printinfo("paramIncExcSw      : " + batchParams.paramIncExcSw);
		ProcessTrb8al.printinfo("paramUnitType      : " + batchParams.paramUnitType);
		ProcessTrb8al.printinfo("paramEmployeeCat   : " + batchParams.paramEmployeeCat);
		
		sTranDateD = new Date();
		sTranDate = sTranDateD.format("yyyyMMdd");

		try
		{
			processBatch();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport();
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb8al);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{	
		ProcessTrb8al.printinfo("processBatch");
		if (initialise_B000())
		{
			processRequest();
			generateTrb8alReport();
		}	
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private boolean initialise_B000()
	{
		ProcessTrb8al.printinfo("initialise_B000");
		
		ReportA = report.open('TRB8ALA')
		
		int iCntr;
		
		//validate mandatory fields
		if (batchParams.paramLeaveType.trim().equals(""))
		{
			WriteError ("Leave Type is Mandatory");
			return false;
		}
		else
		{
			if (!batchParams.paramLeaveType.trim().equals("A")&&
				!batchParams.paramLeaveType.trim().equals("L")&&
				!batchParams.paramLeaveType.trim().equals("S"))
			{
				WriteError ("Invalid Leave Type : " + batchParams.paramLeaveType);
				return false;
			}
		}
		
		if (batchParams.paramIncExcSw.trim().equals(""))
		{
			WriteError ("Include/Exclude switch is Mandatory");
			return false;
		}
		else
		{
			if (!batchParams.paramIncExcSw.trim().equals("I")&&
				!batchParams.paramIncExcSw.trim().equals("E"))
			{
				WriteError ("Invalid Include/Exclude switch : " + batchParams.paramIncExcSw);
				return false;
			}
		}
		
		if (batchParams.paramUnitType.trim().equals(""))
		{
			WriteError ("Unit Type is Mandatory");
			return false;
		}		
		else
		{
			if (!batchParams.paramUnitType.trim().equals("D")&&
				!batchParams.paramUnitType.trim().equals("H"))
			{
				WriteError ("Invalid Unit Type : " + batchParams.paramUnitType);
				return false;
			}
		}						

		
		//CSV detail file
		ReportB.write("Leave Type,Leave Type Desc,Report Date,Years of Service," +
                      "Prim Rpt Code,Employee Id,Last Name,Init,Gender,Employee Type,Staff Category,Birth Date,Age," +
					  "Hire Date,Service Date,Employer Super Fund,Daily Wage Rate,Weekly Wage Rate,Total TEC," +
					  "Leave Entitlement Balance,Leave Entitlement Value,Leave Accrual Balance,Leave Accrual Value,Total Entitlement + Accrual Balance," +
					  "Total Leave Liability,Employee Class,Unit Type,Hours/Day" + "\n")
		return true;
	}

	private void WriteError(String msg)
	{
		ReportA.write(" ");
		ReportA.write(msg);
		ReportA.write(" ");
		ReportA.write(" ");
		ProcessTrb8al.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>for all active employees (excluding the one with staff category = "WC" or "ZO" or "ZA") </li>
	 * <li> get the associate leave entitlement and accrual </li>
	 * <li>Process and populate the TRB8alA records.</li>
	 * <li>Write TRB8ALA records into the output file.</li>
	 */
	private void processRequest()
	{
		ProcessTrb8al.printinfo("processRequest");
		String lveDesc;
		String awardCode;
		BigDecimal yrsOfService = 0;
		BigDecimal empAge = 0;
		BigDecimal ratePerDay = 0;
		
		try
		{
			//browse through MSF880 
			Constraint c1 = MSF880Key.leaveType.equalTo(batchParams.paramLeaveType);			
			def query = new QueryImpl(MSF880Rec.class).and(c1);

			edoi.search(query,10000, {MSF880Rec msf880Rec ->
				if (msf880Rec)
				{
					
					dailyWageRate = 0;
					weeklyWageRate = 0;
					empPackage = 0;
					lveAccrual = 0;
					lveEntitlement = 0;
					lveAccrualValue = 0;
					lveEntitlementValue = 0;
					hrsDaysFac = 0;
					empId = "";
					empSurname = "";
					empFirstInit = "";
					empStatus = "";
					staffCateg = "";
					serviceDate = "";
					empType = "";
					gender = "";
					birthDate = "";
					hireDate = "";
					union = "";
					empClass = "";
					empClassD = "";
					empShiftCat = 0;
					empPayGrp = "";
					PRC = "";
				
					//ProcessTrb8al.printinfo("PROCESSING " + msf880Rec.getPrimaryKey().getEmployeeId());
					
					//get leave type description
					lveDesc = getLveDesc();	
					
					//get employee's name
					processMSF810(msf880Rec.getPrimaryKey().getEmployeeId());
					
					//get personnel details
					processMSF760(msf880Rec.getPrimaryKey().getEmployeeId());
					//ProcessTrb8al.printinfo("PROCESSING empStatus " + empStatus);
					
					if (empStatus.equals("A") && (!staffCateg.equals("WC") && !staffCateg.equals("ZO") && !staffCateg.equals("ZA")))	
					{	
						//get employee's payroll details
						processMSF820(msf880Rec.getPrimaryKey().getEmployeeId());
						
							//get last pay run start and end dates
							processMSF801();
							if (batchParams.paramProjectedDate.trim().equals(""))
							{
								projectedDate = lastPayRunDate;
							}
							else
							{
								projectedDate = batchParams.paramProjectedDate;
							}
							
							//get employee's PRC 
							processMSF878(msf880Rec.getPrimaryKey().getEmployeeId());
							
							//get employee's daily and weekly wage rates 
							//processMSF803(msf880Rec.getPrimaryKey().getEmployeeId());
							getDailyWklyWR(msf880Rec.getPrimaryKey().getEmployeeId());
							
							//get employee award code
							awardCode = processMSF828(msf880Rec.getPrimaryKey().getEmployeeId());
											
							//Calculate employee's leave entitlement & accrual (balance & value)
							if (batchParams.paramUnitType.equals("H")) //report in hrs
							{
								//03  LVE-ST-BALANCE          PIC S9(5)V9(4) COMP-3.      [  54] Leave Entitle Start Bal at Entitle Date  DB
								//03  LVE-MAN-ADJ             PIC S9(5)V9(4) COMP-3.      [  59] Manual Adjustments Units Since Entitle   DB
								//03  LEAVE-TAKEN             PIC S9(5)V9(4) COMP-3.      [  64] Leave Taken Since Entitlement            DB
								//03  LVE-ACCRUED             PIC S9(5)V9(4) COMP-3.      [  69] Leave Accrued Since Entitlement          DB
								lveEntitlement = (msf880Rec.getLveStBalance() + msf880Rec.getLveManAdj() - msf880Rec.getLeaveTaken());
								
								//lveEntitlementValue = (msf880Rec.getLveAmount() / (lveEntitlement + msf880Rec.getLveAccrued())) * lveEntitlement;
								lveEntitlementValue = Divide2(msf880Rec.getLveAmount(), (lveEntitlement + msf880Rec.getLveAccrued())) * lveEntitlement;
								
								if (batchParams.paramProjectedDate.trim().equals(""))
								{
									lveAccrual = msf880Rec.getLveAccrued();
								}
								else
								{
									hrsDaysFac = processMSS880(msf880Rec.getPrimaryKey().getEmployeeId(),awardCode);
									lveAccrual = calcLeaveAcc(msf880Rec.getPrimaryKey().getEmployeeId());
								}
								ratePerDay = Divide2(lveAccrual,7);
								lveAccrualValue =  ratePerDay * dailyWageRate;
							}
							else //report in days
							{
								hrsDaysFac = processMSS880(msf880Rec.getPrimaryKey().getEmployeeId(),awardCode);								
								lveEntitlement = Divide2(msf880Rec.getLveStBalance() + msf880Rec.getLveManAdj() - msf880Rec.getLeaveTaken(),hrsDaysFac)
								lveEntitlementValue = Divide2(Divide2(msf880Rec.getLveAmount(), lveEntitlement + msf880Rec.getLveAccrued()) * lveEntitlement,hrsDaysFac);
							
								if (batchParams.paramProjectedDate.trim().equals(""))
								{
									lveAccrual = Divide2( msf880Rec.getLveAccrued(), hrsDaysFac);
								}
								else
								{
									lveAccrual = Divide2(calcLeaveAcc(msf880Rec.getPrimaryKey().getEmployeeId()),hrsDaysFac);
								}
								ratePerDay = Divide2(lveAccrual,7);
								lveAccrualValue = Divide2(ratePerDay * dailyWageRate, hrsDaysFac);
							}
							
							//calculate years of service
							yrsOfService = DaysBetweenDates(sTranDate,serviceDate) / 365.25;
							
							//calculate age
							empAge = DaysBetweenDates(projectedDate, birthDate) / 365.25;
							
							//ProcessTrb8al.printinfo("paramIncExcSw    : " + batchParams.paramIncExcSw);
							//ProcessTrb8al.printinfo("yrsOfService     : " + yrsOfService);
							//ProcessTrb8al.printinfo("paramEmployeeCat : " + batchParams.paramEmployeeCat);
							//ProcessTrb8al.printinfo("staffCateg       : " + staffCateg);
							
							// other checks
							if ((batchParams.paramIncExcSw.trim().equals("I") || yrsOfService >= 5) &&
								(batchParams.paramEmployeeCat.trim().equals("") || (batchParams.paramEmployeeCat.trim().equals(staffCateg))))
							{
							
								//write to array
								arrayOfTrb8alReportLine.add (new Trb8alaReportLine
									(	batchParams.paramLeaveType,
										lveDesc,
										projectedDate,
										yrsOfService,
										PRC,
										msf880Rec.getPrimaryKey().getEmployeeId(),
										empSurname,
										empFirstInit,
										gender,
										empType,
										staffCateg,
										birthDate,
										empAge,
										hireDate,
										serviceDate,
										union,
										dailyWageRate,
										weeklyWageRate,
										empPackage,
										lveEntitlement,
										lveAccrual,
										lveEntitlementValue,
										lveAccrualValue,
										empClassD,
										batchParams.paramUnitType,
										empShiftCat));
									
									//ProcessTrb8al.printinfo("ADDED  " + msf880Rec.getPrimaryKey().getEmployeeId());
							}
							else
							{
								//ProcessTrb8al.printinfo("SKIPPED");
							}
					}							
				}				
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processRequest Error: " + ex.message);
		}
		
	}
	private BigDecimal DaysBetweenDates(String date1,String date2)
	{
		Date parsedDate1 = dateFormat.parse(date1);
		Date parsedDate2 = dateFormat.parse(date2);
		return parsedDate1 - parsedDate2;
	}
	
	
	private BigDecimal Divide2(BigDecimal v1, BigDecimal v2)
	{
		if (v1 == null) return 0;
		if (v2 == null) return 0;
		if (v1 == 0) return 0;
		if (v2 == 0) return 0;
		if (v1.equals(0.0)) return 0;
		if (v2.equals(0.0)) return 0;
		if (v1.equals(0)) return 0;
		if (v2.equals(0)) return 0;
		BigDecimal res = v1/v2;
		if (res < 0.00001) return 0; 
		return res;
	}
	
	/**
	 * Get leave type's description from 010 table. <br>
	 **/
	private String getLveDesc ()
	{
		//ProcessTrb8al.printinfo ("Process getLveDesc")
		String result = " ";
		
		try
		{
			Constraint c1 = MSF010Key.tableType.equalTo("OLVF");
			Constraint c2 = MSF010Key.tableCode.equalTo(batchParams.paramLeaveType);
			
			def query1 = new QueryImpl(MSF010Rec.class).and(c1.and (c2));
			MSF010Rec msf010Rec1 = (MSF010Rec) edoi.firstRow(query1);
			
			if (!msf010Rec1.getTableDesc().trim().equals(""))
			{
				result = msf010Rec1.getTableDesc();
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("getLveDesc failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("getLveDesc Error: " + ex.message);
		}
		return result;
	}

	/**
	 * Get employee's name. <br>
	 **/
	private void processMSF810 (String empId880)
	{
		//ProcessTrb8al.printinfo ("processMSF810");

		try
		{
			empSurname = "";
			empFirstInit = "";
			Constraint c1 = MSF810Key.employeeId.equalTo(empId880);

			def query = new QueryImpl(MSF810Rec.class).and(c1);
			MSF810Rec msf810Rec = (MSF810Rec) edoi.firstRow(query);
			if  (msf810Rec)
			{
				empSurname = msf810Rec.getSurname();
				empFirstInit = msf810Rec.getFirstName().substring(0,1);
			}
			
			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processMSF810 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSF810 Error: " + ex.message);
		}
	}
	
	/**
	 * Read MSF760 to get employee's personnel details. <br>
	 **/
	private void processMSF760 (String empId880)
	{
		//ProcessTrb8al.printinfo ("processMSF760");
		
		try
		{
			Constraint c1 = MSF760Key.employeeId.equalTo(empId880);
			def query = new QueryImpl(MSF760Rec.class).and (c1);
			MSF760Rec msf760Rec = (MSF760Rec) edoi.firstRow(query);
			
			if (msf760Rec)
			{
				empStatus = msf760Rec.getEmpStatus();
				staffCateg = msf760Rec.getStaffCateg();
				serviceDate = msf760Rec.getServiceDate();
				
				gender = msf760Rec.getGender();
				birthDate = msf760Rec.getBirthDate();
				hireDate = msf760Rec.getHireDate();
				empType = msf760Rec.getEmpType();
					
				//get description
				Constraint c2 = MSF010Key.tableType.equalTo("UN");
				Constraint c3 = MSF010Key.tableCode.equalTo(msf760Rec.getUnionCode());
				
				def query010 = new QueryImpl(MSF010Rec.class).and (c2).and (c3);
				
				MSF010Rec msf010Rec = (MSF010Rec) edoi.firstRow(query010);
				if (msf010Rec)
				{
					union = msf010Rec.getTableDesc();
				}
				
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processMSF760 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSF760 Error: " + ex.message);
		}
	}

	/**
	 * Read MSF820 to get employee's payroll details. <br>
	 **/
	private void processMSF820(String empId880)
	{
		//ProcessTrb8al.printinfo ("processMSF820 " + empId880);
		
		try
		{
			//get standard working hours
			Constraint c1 = MSF820Key.employeeId.equalTo(empId880);
			def query820 = new QueryImpl(MSF820Rec.class).and (c1);
			MSF820Rec msf820Rec = (MSF820Rec) edoi.firstRow(query820);
			

			if (msf820Rec)
			{
				empClass = msf820Rec.getEmployeeClass();
				empPayGrp = msf820Rec.getPayGroup();
				
				String shiftcat = msf820Rec.getShiftCat();
				if (shiftcat == null) shiftcat = "";
				
				//get std working hrs
				Constraint c2 = MSF010Key.tableType.equalTo("SCAT");
				Constraint c3 = MSF010Key.tableCode.equalTo(shiftcat.trim());
				def query010 = new QueryImpl(MSF010Rec.class).and(c2).and(c3);
				
				
				MSF010Rec msf010Rec = (MSF010Rec) edoi.firstRow(query010);
				if (msf010Rec)
				{
					String swh = msf010Rec.getAssocRec().substring(6,11);
					swh = swh.replaceFirst ("^0*", "");
					//empShiftCat = new BigDecimal(swh);
					empShiftCat = StringToBigDecimal(swh);
					
					if (swh.size() >= 3)
					{
						empShiftCat = empShiftCat.divide(100.0);
						//convert HHMM to HHDec
						//empShiftCat = convHHDec(empShiftCat);
					}
				}
				
				if (empClass == null) empClass = "";
				
				//get emp type desc 
				Constraint c4 = MSF010Key.tableType.equalTo("EMCL");
				Constraint c5 = MSF010Key.tableCode.equalTo(empClass.trim());
				def query010EC = new QueryImpl(MSF010Rec.class).and(c4).and(c5);
				
				
				MSF010Rec msf010RecEC = (MSF010Rec) edoi.firstRow(query010EC);
				if (msf010RecEC)
				{
					empClassD = msf010RecEC.getTableDesc();
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processMSF820 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSF820 Error: " + ex.message);
		}
	}
	private BigDecimal StringToBigDecimal(String val)
	{
		try
		{
			return new BigDecimal(val);
		}
		catch (Exception ex)
		{
			return 0;
		}
	}
	private BigDecimal convHHDec (BigDecimal lveHrs)
	{
		String sDec;
		String sHH;
		String sHHDec;
		BigDecimal dec;
		BigDecimal HHDec;
		Boolean decFnd;
		
		sDec = "";
		String sLveHrs = lveHrs.toString();
		decFnd = false;
		
		//find decimal place
		int idx = 0;
		int idx2 = 1;
		while (idx2 <= sLveHrs.size())
		{
			if (sLveHrs.substring(idx, idx2).equals("."))
			{
				decFnd = true;
			}
			idx++;
			idx2++;
		}
		
		//only convert minutes to decimal where applicable
		if (decFnd){
			//get MM and convert to decimal
			sDec = sLveHrs.substring(sLveHrs.indexOf('.') + 1, sLveHrs.size());
			dec =  (sDec.toBigDecimal()/60);
			//get HH
			sHH = sLveHrs.substring(0, sLveHrs.indexOf('.'));
			//convert to HHDec
			HHDec = sHH.toBigDecimal() + dec;
		}
		else{
			HHDec = lveHrs;
		}

		return HHDec;
	}
	/**
	 * Get last pay run date. <br>
	 **/
	private void processMSF801 ()
	{
		//ProcessTrb8al.printinfo ("processMSF801");
		
		try
		{
			lastPayRunDate = " ";
			lastPayRunStDate = " ";
			
			//result = commarea.getProperty("CurPeriodEndDate");
			//get last pay run date
			Constraint c1 = MSF801_PG_801Key.cntlRecType.equalTo("PG");
			Constraint c2 = MSF801_PG_801Key.cntlKeyRest.equalTo(empPayGrp);
			def query = new QueryImpl(MSF801_PG_801Rec.class). and(c1).and(c2);
			MSF801_PG_801Rec msf801Rec_PG = (MSF801_PG_801Rec) edoi.firstRow(query);
			
			if (msf801Rec_PG)
			{
				lastPayRunDate = msf801Rec_PG.getCurEndDtPg();
				lastPayRunStDate = msf801Rec_PG.getCurStrDtPg();
			}
			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processMSF801 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSF801 Error: " + ex.message);
		}
	}
		
	
//	/**
//	 * Get employee's PRC. <br>
//	 **/
//	private void processMSF766 (String empId880)
//	{
//		ProcessTrb8al.printinfo ("processMSF766");
//
//		try
//		{
//			//retrieve employee PRC
//			Constraint c1 = MSF766Key.employeeId.equalTo(empId880);
//			def query = new QueryImpl(MSF766Rec.class).and(c1);
//			
//			ProcessTrb8al.printinfo("processMSF766 :" + empId880);
//			
//			edoi.search(query,10000,{MSF766Rec msf766Rec ->
//				
//				ProcessTrb8al.printinfo("processMSF766 :" + msf766Rec);
//				ProcessTrb8al.printinfo("processMSF766 msf766Rec.getHistEndDate()  :" + msf766Rec.getHistEndDate());
//				ProcessTrb8al.printinfo("processMSF766 msf766Rec.getPrimRptCodes() :" + msf766Rec.getPrimRptCodes());
//				if (msf766Rec.getHistEndDate().equals("00000000") || msf766Rec.getHistEndDate().trim().equals(""))
//				{
//					PRC = msf766Rec.getPrimRptCodes();
//				}
//			})
//		}
//		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
//		{
//			e.printStackTrace();
//			ProcessTrb8al.printinfo("processMSF766 failed - ${e.getMessage()}");
//		}
//		catch (Exception ex)
//		{
//			ProcessTrb8al.printinfo ("processMSF766 Error: " + ex.message);
//		}
//	}
	/**
	 * Read MSF878 to get employee's position at leave time<br>
	 **/
	private Boolean processMSF878 (String empId880)
	{
		//ProcessTrb8al.printinfo ("processMSF878");
		
		try
		{
			
	
			//get the position at time of leave
			String invsTranDate =  (99999999 - sTranDate.toInteger()).toString();
			
			Constraint c1 = MSF878Key.employeeId.equalTo(empId880);
			Constraint c2 = MSF878Key.invStrDate.greaterThanEqualTo(invsTranDate);
			Constraint c3 = MSF878Key.primaryPos.equalTo("0"); 					//primary position
			Constraint c4 = MSF878Key.posStopDate.equalTo("00000000");
			Constraint c5 = MSF878Key.posStopDate.equalTo("");
			Constraint c6 = MSF878Key.posStopDate.greaterThanEqualTo(sTranDate);
			Constraint c7 = MSF878Key.posStopDate.equalTo(" ");
			Constraint c8 = MSF878Key.posStopDate.equalTo("        ");
			
			def query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and(c3).and((c4) .or(c5) .or(c6) .or(c7) .or(c8));
			
			//Read 870 to get PRC for each employee found in 878
			MSF878Rec msf878Rec = (MSF878Rec) edoi.firstRow(query);
			
			if (msf878Rec){
			   Constraint c11 = MSF870Key.positionId.equalTo(msf878Rec.getPrimaryKey().getPositionId());
			   def query11;

				query11 = new QueryImpl(MSF870Rec.class).and(c11);
			
				MSF870Rec msf870Rec = edoi.findByPrimaryKey(new MSF870Key(msf878Rec.getPrimaryKey().getPositionId()));
				
				// WHAT IF NOT 878 OR NO 870 ? Then not reported.
				if (msf870Rec)
				{
					PRC = msf870Rec.getPrimRptGrp();
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			ProcessTrb8al.printinfo("processMSF878 failed - ${e.getMessage()}");
			e.printStackTrace();
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSF878 Error: " + ex.message);
		}
		return false
	}

	/**
	 * Get employee's daily and weekly wage rate. <br>
	 **/
	private void getDailyWklyWR (String empId880)
	{
		try{
			dailyWageRate = 0;
			weeklyWageRate = 0;
			empPackage = 0;
			
			//info ("*** getDailyWklyWR ***")
			MSSRATLINK mssratlnk = new MSSRATLINK();
			mssratlnk = eroi.execute("MSSRAT", {MSSRATLINK mssrat ->
				mssrat.initialise()
				mssrat.option = '1'
				mssrat.employeeId = empId880.trim()
				mssrat.requiredDate = lastPayRunDate
			})
			
			if (mssratlnk.getRatStatus().trim() == "")
			{
				// 6/6/201 RL Use Calc rate instead of Base rate
				//dailyWageRate = mssratlnk.getBaseDlyRate();
				//weeklyWageRate = mssratlnk.getBaseWklyRate();
				dailyWageRate = mssratlnk.getCalcDlyRate();
				weeklyWageRate = mssratlnk.getCalcWklyRate();

				if (empType.equals("SCO") || empType.equals("IEA"))
				{
					//empPackage = mssratlnk.getBaseAnlRate();
					empPackage = mssratlnk.getCalcAnlRate();
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			ProcessTrb8al.printinfo("getDailyWklyWR failed - ${e.getMessage()}");
			e.printStackTrace();
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("getDailyWklyWR Error: " + ex.message);
		}
	}
	
	/**
	 * Get employee's daily and weekly wage rate. <br>
	 **/
//	private void processMSF803 (String empId880)
//	{
//		//ProcessTrb8al.printinfo ("processMSF803");
//
//		try
//		{
//			dailyWageRate = 0;
//			weeklyWageRate = 0;
//			//ProcessTrb8al.printinfo ("Get employee rate reference code in MSF830");
//			Constraint c1 = MSF830Key.employeeId.equalTo(empId880);
//			def query830 = new QueryImpl(MSF830Rec.class).and(c1);
//			MSF830Rec msf830Rec = (MSF830Rec) edoi.firstRow(query830);
//			
//			if (msf830Rec)
//			{
//				//ProcessTrb8al.printinfo ("Get employee daily wage rate in MSF803");
//				Constraint c2 = MSF803Key.rateRefCode.equalTo(msf830Rec.getRateRefCode());
//				Constraint c3 = MSF803Rec.freqType.equalTo("D");
//				def query803D = new QueryImpl(MSF803Rec.class).and(c2).and(c3);
//				MSF803Rec msf803Rec_Daily = (MSF803Rec) edoi.firstRow(query803D);
//				
//				if (msf803Rec_Daily)
//				{
//					dailyWageRate = msf803Rec_Daily.getFreqRate() + msf803Rec_Daily.getCompFreqRatex1() +
//					                msf803Rec_Daily.getCompFreqRatex2() + msf803Rec_Daily.getCompFreqRatex3() +
//									msf803Rec_Daily.getCompFreqRatex4() + msf803Rec_Daily.getCompFreqRatex5() +
//									msf803Rec_Daily.getCompFreqRatex6() + msf803Rec_Daily.getCompFreqRatex7() +
//									msf803Rec_Daily.getCompFreqRatex8();
//				}
//				
//				//ProcessTrb8al.printinfo ("Get employee weekly wage rate in MSF803");
//				Constraint c4 = MSF803Rec.freqType.equalTo("W");
//				def query803W = new QueryImpl(MSF803Rec.class).and(c2).and(c4);
//				MSF803Rec msf803Rec_Wkly = (MSF803Rec) edoi.firstRow(query803W);
//				
//				if (msf803Rec_Wkly)
//				{
//					weeklyWageRate = msf803Rec_Wkly.getFreqRate() + msf803Rec_Wkly.getCompFreqRatex1() +
//					                msf803Rec_Wkly.getCompFreqRatex2() + msf803Rec_Wkly.getCompFreqRatex3() +
//									msf803Rec_Wkly.getCompFreqRatex4() + msf803Rec_Wkly.getCompFreqRatex5() +
//									msf803Rec_Wkly.getCompFreqRatex6() + msf803Rec_Wkly.getCompFreqRatex7() +
//									msf803Rec_Wkly.getCompFreqRatex8();
//				}				
//			}
//		}
//		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
//		{
//			e.printStackTrace();
//			ProcessTrb8al.printinfo("processMSF803 failed - ${e.getMessage()}");
//		}
//		catch (Exception ex)
//		{
//			ProcessTrb8al.printinfo ("processMSF803 Error: " + ex.message);
//		}
//	}
	
	/**
	 * Get employee's total package. <br>
	 **/
//	private void processMSF785 (String empId880)
//	{
//		//ProcessTrb8al.printinfo ("processMSF785");
//		
//		try
//		{
//			Constraint c1 = MSF785Key.employeeId.equalTo(empId880);
//			def query = new QueryImpl(MSF785Rec.class).and(c1);
//			MSF785Rec msf785Rec = (MSF785Rec) edoi.firstRow(query);
//			
//			if (msf785Rec)
//			{
//				// This is found BUT getTotalPackage = zero!
//				//ProcessTrb8al.printinfo ("msf785Rec:"+msf785Rec);
//				empPackage = msf785Rec.getTotalPackage();
//				//ProcessTrb8al.printinfo ("empPackage:"+empPackage);
//			}	
//		} 
//		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
//		{
//			e.printStackTrace();
//			ProcessTrb8al.printinfo("processMSF785 failed - ${e.getMessage()}");
//		}
//		catch (Exception ex)
//		{
//			ProcessTrb8al.printinfo ("processMSF785 Error: " + ex.message);
//		}
//	}

	/**
	 * Get employee award code. <br>
	 **/
	private String processMSF828 (String empId880)
	{
		//ProcessTrb8al.printinfo ("processMSF828");
		String result = "";
		
		try
		{
			Constraint c1 = MSF828Key.employeeId.equalTo(empId880);
			def query = new QueryImpl(MSF828Rec.class).and(c1);
			MSF828Rec msf828Rec = (MSF828Rec) edoi.firstRow(query);
			
			//make sure we get the latest one
			if (msf828Rec.getEndDate().equals("00000000") || msf828Rec.getEndDate().trim().equals(""))
			{
				result = msf828Rec.getAwardCode();
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processMSF828 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSF828 Error: " + ex.message);
		}
		return result;
	}

	/**
	 * call MSS880 subroutine to convert hrs to days <br>
	 **/
	private BigDecimal processMSS880(String empId880, String awardCode)
	{
		//ProcessTrb8al.printinfo("processMSS880");
		BigDecimal result = 1;
		
		try
		{
			//info ("awardCode: " + awardCode);
			MSS880LINK mss880lnk = new MSS880LINK();
			mss880lnk = eroi.execute('MSS880', {MSS880LINK mss880l ->
				mss880l.option880 = "3"
				mss880l.leaveType = batchParams.paramLeaveType
				mss880l.awardCode = awardCode
				mss880l.employeeId = empId880
				});	
			
			if (mss880lnk.errorSw.trim().equals("N"))
			{
				//ProcessTrb8al.printinfo("hrsDaysFac : " + mss880lnk.hrsDaysFac);
				result = mss880lnk.hrsDaysFac;  
			}
		}
		catch (com.mincom.ellipse.errors.exceptions.ErrorException e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("processMSS880 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("processMSS880 Error: " + ex.message);
		}
		return result;
	}

	/**
	 * calculate employee's accrual leave for projected date<br>
	 **/
	private BigDecimal calcLeaveAcc(String empId880)
	{
		//ProcessTrb8al.printinfo("**** Process calcLeaveAcc");
		String forecastDate;
		BigDecimal accrualRate = 0;
		BigDecimal noOfWks = 0;
		BigDecimal result = 0;
				
		try
		{	
			//for new employees who are yet started
			if (projectedDate.toInteger() > serviceDate.toInteger())
			{
				//ProcessTrb8al.printinfo("use projectedDate");
				forecastDate = projectedDate;
			}
			else
			{
				//ProcessTrb8al.printinfo("use serviceDate");
				forecastDate = serviceDate;
			}
						
			//ProcessTrb8al.printinfo("calling msslve");
			//ProcessTrb8al.printinfo("forecastDate  :" + forecastDate);
			//ProcessTrb8al.printinfo("lastPayRunStDate  :" + lastPayRunStDate);
			//ProcessTrb8al.printinfo("empId880  :" + empId880);
			//ProcessTrb8al.printinfo("LeaveType  :" + batchParams.paramLeaveType);
			//ProcessTrb8al.printinfo("hrsDaysFac  :" + hrsDaysFac);
			
			//calculate accrual units
			MSSLVELINK msslvelnk = new MSSLVELINK();
			msslvelnk = eroi.execute("MSSLVE", {MSSLVELINK msslve ->
				msslve.initialise()
				msslve.optionLve = '3'
				msslve.leaveType = batchParams.paramLeaveType.trim()
				msslve.employeeId = empId880.trim()
				msslve.startDate = lastPayRunStDate
				msslve.endDate = forecastDate
				msslve.inputType = 'D'
				msslve.lveUnits = 0
				msslve.entDate = '00000000'
				msslve.dateFormat = 'A'
				msslve.testDate = '00000000'
				msslve.testInd = 'P'
				msslve.controlSw = 0
				msslve.lveBal = 0
				msslve.entBal = 0
				msslve.accrBal = 0
			})
			
//			ProcessTrb8al.printinfo("LeaveStatus  :" + msslvelnk.getLveStatus());
			if (msslvelnk.getLveStatus().trim() == ""){
				//LveBal returned from MSSLVE is the forecast leave balance so we need to calculate forecast
				//leave accrual
				result = msslvelnk.getLveBal() - lveEntitlement; 
				//ProcessTrb8al.printinfo("leave bal   :" + msslvelnk.getLveBal());
				//ProcessTrb8al.printinfo("leave ent   :" + lveEntitlement);
				//ProcessTrb8al.printinfo("result   :" + result);
			}
		}
		catch (com.mincom.ellipse.errors.exceptions.ErrorException e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("calcLeaveAcc failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("calcLeaveAcc Error: " + ex.message);
		}
		return result;
	}
	
	/**
	 * CSV file. <br>
	 * <li> define getter and setter for the batch </li>
	 **/
	private class Trb8alaReportLine implements Comparable<Trb8alaReportLine>
	{
		private String lveType;
		private String lveTypeDesc;
		private String reportDate;
		private BigDecimal yrsOfService;
		private String PRC;
		private String empId;
		private String empSurname;
		private String empFirstInit;
		private	String gender;
		private String empType;
		private BigDecimal empShiftCat;
		private	String hireDate;
		private BigDecimal empAge;
		private	String birthDate;
		private	String serviceDate;
		private	String union;
		private BigDecimal dailyWR;
		private BigDecimal weeklyWR;
		private BigDecimal empPackage;
		private BigDecimal lveEntBal;
		private BigDecimal lveAccBal;
		private BigDecimal lveEntValue;
		private BigDecimal lveAccValue;
		private String empClass;
		private String unitType;
		private	String staffCateg;
		       
		public Trb8alaReportLine(String newLveType,
								String newLveTypeDesc,
								String newReportDate,
								BigDecimal newYrsOfService,
								String newPRC,
								String newEmpId,
								String newEmpSurname,
								String newEmpFirstInit,
								String newGender,
								String newEmpType,
								String newStaffCateg,
								String newBirthDate,
								BigDecimal newEmpAge,
								String newHireDate,
								String newServiceDate,
								String newUnion,
								BigDecimal newDailyWR,
								BigDecimal newWklyWR,
								BigDecimal newEmpPackage,
								BigDecimal newLveEntBal,
								BigDecimal newLveAccBal,
								BigDecimal newLveEntValue,
								BigDecimal newLveAccValue,
								String newEmpClass,
								String newUnitType,
								BigDecimal newEmpShiftCat)
		{
//			ProcessTrb8al.printinfo("---------------------------------------------------------------");
//			ProcessTrb8al.printinfo("newLveType			:"+newLveType		);
//			ProcessTrb8al.printinfo("newLveTypeDesc		:"+newLveTypeDesc		);
//			ProcessTrb8al.printinfo("newReportDate		:"+newReportDate		);
//			ProcessTrb8al.printinfo("newYrsOfService		:"+newYrsOfService		);
//			ProcessTrb8al.printinfo("newPRC				:"+newPRC		);
//			ProcessTrb8al.printinfo("newEmpId				:"+newEmpId		);
//			ProcessTrb8al.printinfo("newEmpSurname		:"+newEmpSurname		);
//			ProcessTrb8al.printinfo("newEmpFirstInit		:"+newEmpFirstInit		);
//			ProcessTrb8al.printinfo("newGender			:"+newGender		);
//			ProcessTrb8al.printinfo("newEmpType			:"+newEmpType		);
//			ProcessTrb8al.printinfo("newStaffCateg		:"+newStaffCateg		);
//			ProcessTrb8al.printinfo("newHireDate			:"+newHireDate		);
//			ProcessTrb8al.printinfo("newEmpAge			:"+newEmpAge		);
//			ProcessTrb8al.printinfo("newBirthDate			:"+newBirthDate		);
//			ProcessTrb8al.printinfo("newServiceDate		:"+newServiceDate		);
//			ProcessTrb8al.printinfo("newUnion				:"+newUnion		);
//			ProcessTrb8al.printinfo("newDailyWR			:"+newDailyWR		);
//			ProcessTrb8al.printinfo("newWklyWR			:"+newWklyWR		);
//			ProcessTrb8al.printinfo("newEmpPackage		:"+newEmpPackage		);
//			ProcessTrb8al.printinfo("newLveEntBal			:"+newLveEntBal		);
//			ProcessTrb8al.printinfo("newLveAccBal			:"+newLveAccBal		);
//			ProcessTrb8al.printinfo("newLveEntValue		:"+newLveEntValue		);
//			ProcessTrb8al.printinfo("newLveAccValue		:"+newLveAccValue		);
//			ProcessTrb8al.printinfo("newEmpClass			:"+newEmpClass		);
//			ProcessTrb8al.printinfo("newUnitType			:"+newUnitType		);
//			ProcessTrb8al.printinfo("newEmpShiftCat		:"+newEmpShiftCat		);
			
			setLveType(newLveType);
			setLveTypeDesc (newLveTypeDesc);
			setReportDate (newReportDate);
			setYrsOfService (newYrsOfService);
			setPRC (newPRC);
			setEmpId (newEmpId);
			setEmpSurname (newEmpSurname);
			setEmpFirstInit (newEmpFirstInit);
			setGender (newGender);
			setEmpType (newEmpType);
			setStaffCateg (newStaffCateg);
			setHireDate (newHireDate);
			setEmpAge (newEmpAge);
			setBirthDate (newBirthDate);
			setServiceDate (newServiceDate);
			setUnion (newUnion);			
			setDailyWR (newDailyWR);
			setWklyWR (newWklyWR);
			setEmpPackage (newEmpPackage);
			setLveEntBal (newLveEntBal);
			setLveAccBal (newLveAccBal);
			setLveEntValue (newLveEntValue);
			setLveAccValue (newLveAccValue);
			setEmpClass (newEmpClass);
			setUnitType (newUnitType);
			setEmpShiftCat (newEmpShiftCat);
		}

		public void setLveType(String newLveType)
		{
			lveType = newLveType;
		}
		public String getLveType()
		{
			return lveType;
		}
		
		public void setLveTypeDesc(String newLveTypeDesc)
		{
			lveTypeDesc = newLveTypeDesc;
		}
		public String getLveTypeDesc()
		{
			return lveTypeDesc;
		}
		
		public void setReportDate(String newReportDate)
		{
			reportDate = newReportDate;
		}
		public String getReportDate()
		{
			return reportDate;
		}
		public String getReportDateD()
		{
			try
				{
					Date newerdate = new Date().parse("yyyyMMdd", reportDate)
					String val = newerdate.format("dd/MM/yyyy");
					return val;
				}
				catch (Exception ex)
				{
					return "";
				}
		}
		
		public void setYrsOfService(BigDecimal newYrsOfService)
		{
			yrsOfService = newYrsOfService;
		}
		public BigDecimal getYrsOfService()
		{
			return yrsOfService;
		}
		
		public void setPRC(String newPRC)
		{
			PRC = newPRC;
		}
		public String getPRC()
		{
			return PRC;
		}
		
		public void setEmpId(String newEmpId)
		{
			empId = newEmpId;
		}
		public String getEmpId()
		{
			return empId;
		}

		public void setEmpSurname(String newEmpSurname)
		{
			empSurname = newEmpSurname;
		}
		public String getEmpSurname()
		{
			return empSurname;
		}

		public void setEmpFirstInit(String newEmpFirstInit)
		{
			empFirstInit = newEmpFirstInit;
		}
		public String getEmpFirstInit()
		{
			return empFirstInit;
		}
		
		
		public void setGender(String newGender)
		{
			gender = newGender;
		}
		public String getGender()
		{
			return gender;
		}
	
		public void setEmpType(String newEmpType)
		{
			empType = newEmpType;
		}
		public String getEmpType()
		{
			return empType;
		}
		
		public void setEmpShiftCat(BigDecimal newEmpShiftCat)
		{
			empShiftCat = newEmpShiftCat;
		}
		public BigDecimal getEmpShiftCat()
		{
			return empShiftCat;
		}
		
		public void setHireDate(String newHireDate)
		{
			hireDate = newHireDate;
		}
		public String getHireDate()
		{
			return hireDate;
		}
		public String getHireDateD()
		{
			try
				{
					Date newerdate = new Date().parse("yyyyMMdd", hireDate)
					String val = newerdate.format("dd/MM/yyyy");
					return val;
				}
				catch (Exception ex)
				{
					return "";
				}
		}
		
		public void setEmpAge(BigDecimal newEmpAge)
		{
			empAge = newEmpAge;
		}
		public BigDecimal getEmpAge()
		{
			return empAge;
		}
		
		public void setBirthDate(String newBirthDate)
		{
			birthDate = newBirthDate;
		}
		public String getBirthDate()
		{
			return birthDate;
		}
		public String getBirthDateD()
		{
			try
				{
					Date newerdate = new Date().parse("yyyyMMdd", birthDate)
					String val = newerdate.format("dd/MM/yyyy");
					return val;
				}
				catch (Exception ex)
				{
					return "";
				}
		}

		public void setServiceDate(String newServiceDate)
		{
			serviceDate = newServiceDate;
		}
		public String getServiceDate()
		{
			return serviceDate;
		}
		public String getServiceDateD()
		{
			try
				{
					Date newerdate = new Date().parse("yyyyMMdd", serviceDate)
					String val = newerdate.format("dd/MM/yyyy");
					return val;
				}
				catch (Exception ex)
				{
					return "";
				}
		}
		
		public void setUnion(String newUnion)
		{
			union = newUnion;
		}
		public String getUnion()
		{
			return union;
		}
		
		public void setDailyWR(BigDecimal newDailyWR)
		{
			dailyWR = newDailyWR;
		}
		public BigDecimal getDailyWR()
		{
			return dailyWR;
		}
		
		public void setWklyWR(BigDecimal newWklyWR)
		{
			weeklyWR = newWklyWR;
		}
		public BigDecimal getWklyWR()
		{
			return weeklyWR;
		}
		
		public void setEmpPackage(BigDecimal newEmpPackage)
		{
			empPackage = newEmpPackage;
		}
		public BigDecimal getEmpPackage()
		{
			return empPackage;
		}
		
		public void setLveEntBal(BigDecimal newLveEntBal)
		{
			lveEntBal = newLveEntBal;
		}
		public BigDecimal getLveEntBal()
		{
			return lveEntBal;
		}
		
		public void setLveAccBal(BigDecimal newLveAccBal)
		{
			lveAccBal = newLveAccBal;
		}
		public BigDecimal getLveAccBal()
		{
			return lveAccBal;
		}
		
		public void setLveEntValue(BigDecimal newLveEntValue)
		{
			lveEntValue = newLveEntValue;
		}
		public BigDecimal getLveEntValue()
		{
			return lveEntValue;
		}
		
		public void setLveAccValue(BigDecimal newLveAccValue)
		{
			lveAccValue = newLveAccValue;
		}
		public BigDecimal getLveAccValue()
		{
			return lveAccValue;
		}
		
		public void setEmpClass(String newEmpClass)
		{
			empClass = newEmpClass;
		}
		public String getEmpClass()
		{
			return empClass;
		}
		
		public void setUnitType(String newUnitType)
		{
			unitType = newUnitType;
		}
		public String getUnitType()
		{
			return unitType;
		}
		
		public void setStaffCateg(String newStaffCateg)
		{
			staffCateg = newStaffCateg;
		}
		public String getStaffCateg()
		{
			return staffCateg;
		}
		
		//report is sorted via years of service and employee Id
		public int compareTo(Trb8alaReportLine otherReportLine)
		{
			if (!yrsOfService.equals(otherReportLine.getYrsOfService()))
			{
				return yrsOfService.compareTo(otherReportLine.getYrsOfService())
			}

			if (!empId.equals(otherReportLine.getEmpId()))
			{
				return empId.compareTo(otherReportLine.getEmpId())
			}
			return 0;
		}
				
	}
	
	/**
	 * generate batch report & CSV files <br>
	 * <li> 1. Output details of projected leave to a CSV file </li>
	 **/
	private generateTrb8alReport()
	{
		ProcessTrb8al.printinfo ("Process generateTrb8alReport");
		try
		{
			Trb8alaReportLine currLine;
			BigDecimal totLveBal;
			BigDecimal totLveLiability;
	
			String unitTypeDesc;
			String APP = "\"";
			
			
			
			Collections.sort(arrayOfTrb8alReportLine);
			
			
			//loop through the array list and output to report line
			for (int index = 0; index < arrayOfTrb8alReportLine.size(); index++)
			{
				currLine = arrayOfTrb8alReportLine.get(index);
				
				totLveBal = currLine.getLveAccBal() + currLine.getLveEntBal();
				totLveLiability = currLine.getLveAccValue() + currLine.getLveEntValue();
				
				if (currLine.getUnitType().equals("D"))
				{
					unitTypeDesc = "DAYS";
				}
				else
				{
					unitTypeDesc = "HOURS";
				}
				//write CSV file
				String line = "";
				line = line + APP + currLine.getLveType() + APP + "," ;
				line = line + APP + currLine.getLveTypeDesc() + APP + "," ;
				line = line + APP + currLine.getReportDateD() + APP + "," ;
				line = line +  decFormatterNoDec.format(currLine.getYrsOfService())   +  "," ;
				line = line + APP + currLine.getPRC() + APP + ",";
				line = line + APP + currLine.getEmpId() + APP + "," ;
				line = line + APP + currLine.getEmpSurname() + APP + "," ;
				line = line + APP + currLine.getEmpFirstInit() +APP +  "," ;
				line = line + APP + currLine.getGender() +APP +  "," ;
				line = line + APP + currLine.getEmpType() + APP + "," ;
				line = line + APP + currLine.getStaffCateg() + APP + "," ;
				line = line + APP + currLine.getBirthDateD() + APP + "," ;
				line = line + decFormatterNoDec.format(currLine.getEmpAge()) + "," ;
				line = line + APP + currLine.getHireDateD() + APP + "," ;
				line = line + APP + currLine.getServiceDateD() + APP + "," ;
				line = line + APP + currLine.getUnion() + APP + "," ;
				line = line + "\$" + decFormatter.format(currLine.getDailyWR()).toString().trim() + "," ;
				line = line + "\$" + decFormatter.format(currLine.getWklyWR()).toString().trim() + "," ;
				line = line + "\$" + decFormatter.format(currLine.getEmpPackage()).toString().trim() + ",";
				line = line + decFormatter.format(currLine.getLveEntBal()).toString().trim() + "," ;
				line = line + "\$" + decFormatter.format(currLine.getLveEntValue()).toString().trim() + "," ;
				line = line + decFormatter.format(currLine.getLveAccBal()).toString().trim() + "," ;
				line = line + "\$" + decFormatter.format(currLine.getLveAccValue()).toString().trim() + "," ;
				line = line + decFormatter.format(totLveBal).toString().trim() + "," ;
				line = line + "\$" + decFormatter.format(totLveLiability).toString().trim() + "," ;
				line = line + APP + currLine.getEmpClass() + APP + ",";
				line = line + APP + unitTypeDesc + APP + "," ;
				line = line + decFormatter.format(currLine.getEmpShiftCat()).toString().trim() + "," + "\n";
				
				ReportB.write(line);
			}
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("generateTrb8alReport Error: " + ex.message);
		}
		
		ReportB.close();
		
	}

	/**
	 * print batch report <br>
	 * <li> output total record counts </li>
	 **/
	private void printBatchReport()
	{
		ProcessTrb8al.printinfo("printBatchReport");
		//print batch report
		println ("\n");
		println(StringUtils.center("End of Report ", 132));
	}
}


/*run script*/
ProcessTrb8al process = new ProcessTrb8al();
process.runBatch(binding);
