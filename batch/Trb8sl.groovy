/**
 * @AIT 2012
 * Conversion from trb8sl.cbl
 * 03/04/2013 RL - VERSION 1
 * 09/04/2013 RL - VERSION 2
 * 17/04/2013 RL - VERSION 3
 * 
 */
package com.mincom.ellipse.script.custom

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import com.mincom.eilib.EllipseEnvironment;
//import com.mincom.batch.RequestDefinition;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.ellipse.types.m3875.instances.TransactionSearchParam;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.ellipse.common.unix.*;

import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf829.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf888.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf870.*;
import com.mincom.ellipse.edoi.ejb.msf808.*;

import org.apache.commons.lang.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.Comparable;
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
//public class AITBatch8SL implements GroovyInterceptable {
//
//    private static final long REFRESH_TIME = 60 * 1000 * 5
//
//    public EDOIWrapper edoi
//    public EROIWrapper eroi
//    public ServiceWrapper service
//    public BatchWrapper batch;
//    public CommAreaScriptWrapper commarea;
//    public BatchEnvironment env
//    public UnixTools tools
//    public Reports report;
//    public Sort sort;
//    public Params params;
//    public RequestInterface request;
//    public Restart restart;
//
//    private String uuid;
//    private String taskUuid;
//
//    private Date lastDate;
//
//    private boolean disableInvokeMethod
//
//    public static final int SuperBatch_VERSION = 5;
//    public static final String SuperBatch_CUST = "TRAN1";
//
//    /**
//     * Print a string into the logger. 
//     * @param value a string to be printed.
//     */
//    public void info(String value){
//        def logObject = LoggerFactory.getLogger(getClass());
//        logObject.info("------------- " + value)
//    }
//    
//    public void debug(String value){
//        def logObject = LoggerFactory.getLogger(getClass());
//        logObject.debug("------------- " + value)
//    }
//
//    /**
//     * Initialize the variables based on binding object.
//     * @param b binding object
//     */
//    public void init(Binding b) {
//        edoi = b.getVariable("edoi");
//        eroi = b.getVariable("eroi");
//        service = b.getVariable("service");
//        batch = b.getVariable("batch");
//        commarea = b.getVariable("commarea");
//        env = b.getVariable("env");
//        tools = b.getVariable("tools");
//        report = b.getVariable("report");
//        sort = b.getVariable("sort");
//        request = b.getVariable("request");
//        restart = b.getVariable("restart");
//        params = b.getVariable("params");
//
//        // gets the uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
//        uuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.getUUID();
//
//        // gets the task uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
//        taskUuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.request.getTaskUuid();
//		
//    }
//
//    /**
//     *  Returns the uuid
//     * @return String UUID
//     */
//    public String getUUID() {
//        return uuid
//    }
//
//    /**
//     *  Returns the task uuid from the parent
//     * @return String UUID
//     */
//    public String getTaskUUID() {
//        return taskUuid
//    }
//
//    /**
//     * Print the version.
//     */
//    public void printSuperBatchVersion(){
//        info ("SuperBatch Version:" + SuperBatch_VERSION);
//        info ("SuperBatch Customer:" + SuperBatch_CUST);
//    }
//
//    def invokeMethod(String name, args) {
//        if (!disableInvokeMethod) {
//            disableInvokeMethod = true;
//            try {
//                keepAliveConnection();
//            } finally {
//                disableInvokeMethod = false;
//            }
//        }
//        def result
//        def metaMethod = metaClass.getMetaMethod(name, args)
//        result = metaMethod.invoke(this, metaMethod.coerceArgumentsToClasses(args))
//        return result
//    }
//
//    protected void keepAliveConnection() {
//        if (lastDate == null) {
//            lastDate = new Date();
//        } else {
//            Date currentDate = new Date();
//            debug("Time elapsed  = " + (currentDate.getTime() - lastDate.getTime()))
//            debug("Time refresh  = " + REFRESH_TIME)
//            if ((currentDate.getTime() - lastDate.getTime()) > REFRESH_TIME ) {
//                lastDate = currentDate;
//                restartTransaction();
//            }
//        }
//    }
//
//    protected void restartTransaction() {
//        debug("restartTransaction")
//        (0..0).each restart.each(1, { debug("Restart Transaction") })
//        debug("end restart transaction")
//    }
//}
////-------------------------------------------------------------------




/**
 * Request Parameters for ParamsTrb8sl.
 */
public class ParamsTrb8sl
{
	//List of Input Parameters
	String paramDateFrom;
	String paramDateTo;
	String paramPRC;
	String paramEmployeeId;
}

 
/**
 * Sick Leave report <br>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
 * <li> input parameters: Date range - mandatory </li>
 * <li> 				  PRC - leave blank to retrieve all </li>
 * <li> 				  Employee ID - leave blank to retrieve all </li>
 * <li> output: a batch and 2 CSV files. </li>
 * <li> 1. a batch report will contain employee's hrs and days for each leave type </li>
 * <li> and a sub total group by branch and a grand total at the end of report </li>
 * <li> 2. a CSV details file will contain leave hrs and days for each transaction date </li>
 * <li> and a sub total group by branch and a grand total at the end of report </li>
 * <li> 3. a CSV summary file will contain leave hrs and days for each employee </li>
 * <li> and a sub total group by branch and a grand total at the end of report </li>
 **/
//public class ProcessTrb8sl extends AITBatch8SL {
public class ProcessTrb8sl extends SuperBatch {
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */

	private version = 3;
	private ParamsTrb8sl batchParams;

	private String userId = " ";
	private String empId888;
	private String empFirstName;
	private String empSurname;
	private String physicalLocn;
	private String empType;
	private String workCode;
	private String workCodeDesc;
	private BigDecimal stdWrkHrs;
	private String lveReason;
	private String lveCertFlag;
	private String trnDate;
	private String lveStrDate;
	private String lveEndDate;
	private BigDecimal trnUnits;
	private String PRC02;
	private String PRC03;
	private String PRC04;
	
	private ArrayList arrayOfTrb8slReportLine = new ArrayList();
	
	//Report A - summary batch report
	private def ReportA;
	
	int ReportALineCounter = 0;
	
	private String workDir;

	
	//CVS file B - details report
	File ReportBFile;
	FileWriter ReportBStream;
	String ReportBPath;
	BufferedWriter ReportB;		// = new BufferedWriter(fstream1)
	
	//CSV file C - summary report
	File ReportCFile;
	FileWriter ReportCStream;
	String ReportCPath;
	BufferedWriter ReportC;		// = new BufferedWriter(fstream2)

	//format displayed fields
	private DecimalFormat decFormatter = new DecimalFormat("################0.00");
	private SimpleDateFormat dateFormatter = new SimpleDateFormat ("dd/MM/yyyy");
	private SimpleDateFormat ellipseDate = new SimpleDateFormat ("yyyyMMdd");
	private SimpleDateFormat dayFormatter = new SimpleDateFormat ("EEE");

	
	/**
	 * Run the main batch.
	 * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b) 
	{
		init(b);
		info("uuid         :" + getUUID());
		
		workDir = env.getWorkDir().toString() + "/";
		
		ReportBPath = workDir +"TRT8SL" + "." + taskUUID + ".csv";
		ReportBFile = new File(ReportBPath);
		ReportBStream = new FileWriter(ReportBFile)
		ReportB = new BufferedWriter(ReportBStream)
		
		ReportCPath = workDir +"TRT8SM" + "." + taskUUID + ".csv";
		ReportCFile = new File(ReportCPath);
		ReportCStream = new FileWriter(ReportCFile)
		ReportC = new BufferedWriter(ReportCStream)
		
		info("ReportBPath   :" + ReportBPath);
		info("ReportCPath   :" + ReportCPath);
		
		printSuperBatchVersion();
		info("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb8sl());;

		//PrintRequest Parameters
		info("paramDateFrom      : " + batchParams.paramDateFrom);
		info("paramDateFTo       : " + batchParams.paramDateTo);
		info("paramPRC           : " + batchParams.paramPRC);
		info("paramEmployeeId    : " + batchParams.paramEmployeeId);
		
		try
		{
			processBatch();
		}
		finally
		{
			printBatchReport();
		}		
	}

	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		info("processBatch");
		if (initialise_B000())
		{
			processRequest();
			if (!arrayOfTrb8slReportLine.isEmpty())
			{
				generateTrb8slReport();
			}
		}
	}
	
	private class lve888
	{
		public String empId;
		public String lveReason;
		public String lveType;
		public String bLveCode;
		public BigDecimal lveHrs;
		public String lveStDt;
		public String lveEndDt;
		public BigDecimal lveStTime;
		public BigDecimal lveEndTime;
	}
	
	/**
	 * Process request. <br>
	 * <li>get leave transactions within date range, </li>
	 * <li>process each record and </li>
	 * <li>Populate the TRB8slA records.</li>
	 * <li>Write TRB8SLA records into the output file.</li>
	 */
	private void processRequest()
	{
		info("processRequest");
		String sDateFrom;
		String sDateTo;
		def prevLveRec = new lve888();
		BigDecimal sumLvHrs;
		Boolean setFnd;
		
		try
		{	
			prevLveRec.empId = "";
			sumLvHrs = 0;
			prevLveRec.lveHrs = 0;
			setFnd = false;
			//retrieve MSF888 transactions for sick family care and sick leave within the date range
			sDateFrom = batchParams.paramDateFrom;
			sDateTo = batchParams.paramDateTo;
			
			//info("sDateFrom:" + sDateFrom);
			//info("sDateTo  :" + sDateTo);
			
			Constraint c1 = MSF888Key.lveStDate.greaterThanEqualTo(sDateFrom);
			Constraint c2 = MSF888Rec.lveEndDate.lessThanEqualTo(sDateTo);
			Constraint c3 = MSF888Key.lveReason.equalTo("S");
			Constraint c4 = MSF888Key.lveReason.equalTo("F");
			
			def query
			
			if (batchParams.paramEmployeeId.trim() != ""){			
				Constraint c5 = MSF888Key.employeeId.equalTo(batchParams.paramEmployeeId.trim());
				query = new QueryImpl(MSF888Rec.class).and(c1).and(c2).and((c3) .or (c4)).and(c5)
			}
			else{
				query = new QueryImpl(MSF888Rec.class).and(c1).and(c2).and((c3) .or (c4))
			}
			
			edoi.search(query,{MSF888Rec msf888Rec ->	
				if (msf888Rec){
					//ignore amended leave records
					//0000085978 S 20130116 -7.7833 20130116  
					//0000085978 S 20130117  7.7833 20130117  
					if (msf888Rec.getPrimaryKey().getEmployeeId().equals (prevLveRec.empId) &&
						msf888Rec.getPrimaryKey().getLveStDate().equals (prevLveRec.lveStDt) &&
						msf888Rec.getLveEndDate().equals (prevLveRec.lveEndDt) &&
						msf888Rec.getLveStartTime().equals (prevLveRec.lveStTime) &&
						msf888Rec.getLveStopTime().equals (prevLveRec.lveEndTime) &&
						msf888Rec.getPrimaryKey().getLveReason().equals (prevLveRec.lveReason) &&
						msf888Rec.getPrimaryKey().getLeaveType().equals (prevLveRec.lveType) &&
						msf888Rec.getBookedLvCode().equals(prevLveRec.bLveCode))
					{	
						sumLvHrs = prevLveRec.lveHrs - prevLveRec.lveHrs;
					}
					else
					{
						sumLvHrs = 1;
					}
					
					//info ("sumLvHrs: " + sumLvHrs);
					//info ("prevLveRec.empId: " + prevLveRec.empId);
					
					if (sumLvHrs != 0 && prevLveRec.empId.trim() != "" && prevLveRec.lveHrs >= 0)
					{	
						empId888 = prevLveRec.empId;
						trnDate = prevLveRec.lveStDt;				
						//lve hrs is in HHdecimal		
						trnUnits = prevLveRec.lveHrs;
						lveStrDate =  prevLveRec.lveStDt;
						lveEndDate = prevLveRec.lveEndDt;
						
						//info("MSF888:" + empId888 + "," + msf888Rec.getPrimaryKey().getLveReason() + "," + 
						//	 trnDate + "," +trnUnits);
						//info("prev rec:" + prevLveRec.empId + "," + prevLveRec.lveReason + "," + prevLveRec.lveStDt + 
						//	"," + prevLveRec.lveEndDt + "," + prevLveRec.lveHrs);
						
						if (processMSF878(trnDate))
						{		
							//write details line to an array list
							arrayOfTrb8slReportLine.add(new Trb8slaReportLine(PRC02,PRC03,PRC04,
															empId888,empSurname,empFirstName,
															empType,trnDate,workCode,workCodeDesc,
															stdWrkHrs,trnUnits,
															lveStrDate, lveEndDate, lveReason, lveCertFlag));
						}
					}
										
					prevLveRec.empId = msf888Rec.getPrimaryKey().getEmployeeId();
					prevLveRec.lveStDt = msf888Rec.getPrimaryKey().getLveStDate();
					prevLveRec.lveEndDt = msf888Rec.getLveEndDate();
					prevLveRec.lveStTime = msf888Rec.getLveStartTime();
					prevLveRec.lveEndTime = msf888Rec.getLveStopTime();
					prevLveRec.lveReason = msf888Rec.getPrimaryKey().getLveReason();
					prevLveRec.lveType = msf888Rec.getPrimaryKey().getLeaveType();
					prevLveRec.lveHrs = msf888Rec.getBlLveHours();
					prevLveRec.bLveCode = msf888Rec.getBookedLvCode();
				}
			})
			
			//process the last record
			if (sumLvHrs != 0){
				empId888 = prevLveRec.empId;									
				trnDate = prevLveRec.lveStDt;
				//lve hrs is in HHdecimal
				trnUnits = prevLveRec.lveHrs;
				lveStrDate =  prevLveRec.lveStDt;
				lveEndDate = prevLveRec.lveEndDt;
				
				//info("prev rec:" + prevLveRec.empId + "," + prevLveRec.lveReason + "," + prevLveRec.lveStDt +
				//	"," + prevLveRec.lveEndDt + "," + prevLveRec.lveHrs);
				
				if (processMSF878(trnDate))
				{
					//write details line to an array list
					arrayOfTrb8slReportLine.add(new Trb8slaReportLine(PRC02,PRC03,PRC04,
													empId888,empSurname,empFirstName,
													empType,trnDate,workCode,workCodeDesc,
													stdWrkHrs,trnUnits,
													lveStrDate, lveEndDate, lveReason, lveCertFlag));
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			info("processRequest failed - ${e.getMessage()}");
			e.printStackTrace();
		}
	}
		

	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private boolean initialise_B000()
	{
		int iCntr;

		//validate mandatory fields - date from & date to
		if (batchParams.paramDateFrom.trim().equals(""))
		{
			info ("Date From is Mandatory");
			return false;
		}
		if (batchParams.paramDateTo.trim().equals(""))
		{
			info ("Date To is Mandatory");
			return false;
		}

		//Batch report
		ReportA = report.open('TRB8SLA')
		
		writeReportHeader();
		
		//CSV detail file
		ReportB.write("Business Unit,Group,Branch,Surname,First Name,Employee Id,Type,Std Hours,Day,Date,Work Code,Description,Actual Hours,Days" + "\n")
		
		//CSV summary file
		ReportC.write("Business Unit,Group,Branch,Surname,First Name,Employee Id,FC Cert Hrs,FC Cert Days,No of Occ,FC W/O Hrs,FC W/O Days,No of Occ,SL Cert Hrs,SL Cert Days,No of Occ,SL W/O Hrs,SL W/O Days,No of Occ,Total Hrs,Total Days" + "\n")
		
		return true;
	}
	
	/**
	 * Read MSF878 to get employee's position at leave time<br>
	 **/
	private Boolean processMSF878 (String sTranDate)
	{
		String invTodayDate;
		String PRC870;
		
		try
		{
			PRC870 = "";
			
			//get the position at time of leave
			String invsTranDate =  (99999999 - sTranDate.toInteger()).toString();
//			info ("invsTranDate: " + invsTranDate)
			
			Constraint c1 = MSF878Key.employeeId.equalTo(empId888);	
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
//			info ("878 Key: " + msf878Rec.getPrimaryKey());
			
			if (msf878Rec){				
			   Constraint c11 = MSF870Key.positionId.equalTo(msf878Rec.getPrimaryKey().getPositionId());
			   def query11;

				if (batchParams.paramPRC.trim() == ""){
					query11 = new QueryImpl(MSF870Rec.class).and(c11);
				}
				else{
					Constraint c12 = MSF870Rec.primRptGrp.equalTo(batchParams.paramPRC.trim());
					query11 = new QueryImpl(MSF870Rec.class).and(c11).and(c12);
				}
			
				MSF870Rec msf870Rec = edoi.findByPrimaryKey(new MSF870Key(msf878Rec.getPrimaryKey().getPositionId()));
				
				// WHAT IF NOT 878 OR NO 870 ? Then not reported.				
				if (msf870Rec)
				{
//					info ("msf870 key: " + msf870Rec.getPrimaryKey().getPositionId() + "," + msf870Rec.getPrimRptGrp());					
					if (batchParams.paramPRC.trim() == "")
					{
						PRC870 = msf870Rec.getPrimRptGrp();
						getPRCDesc (PRC870);
						processMSF810();
						processMSF760();
						processMSF820();
						processMSF891();
						return true;
					}
					else{
						if (batchParams.paramPRC.trim().equals(msf870Rec.getPrimRptGrp().trim()))
						{
							PRC870 = msf870Rec.getPrimRptGrp();
							getPRCDesc (PRC870);
							processMSF810();
							processMSF760();
							processMSF820();
							processMSF891();
							return true;
						}
					}
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			info("processMSF878 failed - ${e.getMessage()}");
			e.printStackTrace();
		}
		return false
	}
	
	/**
	 * Read MSF801 to check for employee's leave certificate. <br>
	 **/
	//Currently NOT IN USED
/*	private String checkCert801 (String earnCode888)
	{
		String keyRest;
		String result = "";
		
		//info("payGrp888  :"+payGrp888);
		//info("earnCode888  :"+earnCode888);

		try
		{	result = "N";
			// RL use ***
			keyRest = "***" + earnCode888;
			
			Constraint c1 = MSF801_A_801Key.cntlRecType.equalTo("A");
			Constraint c2 = MSF801_A_801Key.cntlKeyRest.equalTo(keyRest);

			def query = new QueryImpl(MSF801_A_801Rec.class).and(c1.and(c2));
			MSF801_A_801Rec msf801Rec_A = (MSF801_A_801Rec) edoi.firstRow(query);
			if (msf801Rec_A)
			{
				result = msf801Rec_A.getLveCertA();
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("checkCert801 failed - ${e.getMessage()}");
		}
		return result;
	}*/
	
	/**
	 * Read MSF891 to get employee's work code, <br>
	 * <li> then read 801 to get lveReason, lveCert & work code desc. </li>
	 **/
	private void processMSF891 ()
	{	
		String keyRest;
		
		try
		{			
			Constraint c1 = MSF891AIX1.employeeId.equalTo(empId888);
			Constraint c2 = MSF891AIX1.trnDate.equalTo(trnDate);
			
			def query = new QueryImpl(MSF891Rec.class).and(c1).and(c2) ;
			
			//employee may have split transactions for that day
			edoi.search(query,{MSF891Rec msf891Rec ->
				workCodeDesc = "";			
				if (msf891Rec)
				{			 
					//info ("workCode: " + msf891Rec.getWorkCode())
	
					//keyRest = physicalLocn + msf891Rec.getWorkCode();
					keyRest = "***" + msf891Rec.getWorkCode();
					Constraint cA1 = MSF801_R1_801Key.cntlRecType.equalTo("R1");
					
					Constraint cA2 = MSF801_R1_801Key.cntlKeyRest.equalTo(keyRest);
					
					def query801 = new QueryImpl(MSF801_R1_801Rec.class).and(cA1).and(cA2);
					MSF801_R1_801Rec msf801Rec_R1 = (MSF801_R1_801Rec) edoi.firstRow(query801);
					
					if (msf801Rec_R1)
					{
						if (msf801Rec_R1.lveReasonR1.equals("S")||msf801Rec_R1.lveReasonR1.equals("F"))
						{
							workCode = msf891Rec.getWorkCode();
							workCodeDesc = msf801Rec_R1.getTnameR1();
							lveReason = msf801Rec_R1.getLveReasonR1();
							lveCertFlag = msf801Rec_R1.getLveCertR1();
						}
					}
				}
				else  //assume every transaction in 888 should have an associated 891 transaction
				{
					//info("NO MSF891");
				}
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processMSF891 failed - ${e.getMessage()}");
		}
	}

	
	/**
	 * Read MSF820 to get employee's standard working hours. <br>
	 * <li> It's used in leave calculation </li>
	 **/
	private void processMSF820()
	{
		try
		{
			stdWrkHrs = 0;
			//get standard working hours
			Constraint c1 = MSF820Key.employeeId.equalTo(empId888);
			def query820 = new QueryImpl(MSF820Rec.class).and (c1);
			MSF820Rec msf820Rec = (MSF820Rec) edoi.firstRow(query820);

//			info("820 Key: " + msf820Rec.getPrimaryKey() + "," + msf820Rec.getEmployeeClass())
			if (msf820Rec)
			{
				//std working hrs calculation for employee class P
				if (msf820Rec.getEmployeeClass().trim().equals("P"))
				{
					//info ("contract Hrs: " + msf820Rec.getContractHours())
					//convert HHMM to HHDec
					stdWrkHrs = (convHHDec(msf820Rec.getContractHours()) / 35) * 7;
					//info ("stdWrkHrs: " + stdWrkHrs)
				}
				else
				{
					//std working hrs for employee class F comes from an associate value
					// in SCAT table
					if (msf820Rec.getEmployeeClass().trim().equals("F"))
					{
						Constraint c2 = MSF010Key.tableType.equalTo("SCAT");
						Constraint c3 = MSF010Key.tableCode.equalTo(msf820Rec.getShiftCat().trim());
						def query010 = new QueryImpl(MSF010Rec.class).and(c2).and(c3);
						MSF010Rec msf010Rec = (MSF010Rec) edoi.firstRow(query010);
						
						//info("Shift Class:"+msf820Rec.getShiftCat().trim());
	
						if (msf010Rec)
						{
							String swh = msf010Rec.getAssocRec().substring(6,11);
							swh = swh.replaceFirst ("^0*", "");
	
							//info ("swh: " + swh)
							
							//convert to HHDec if std hrs has 3 integers or more (eg: 747)
							if (swh.size() >= 3)
							{
								stdWrkHrs = new BigDecimal(swh);
								stdWrkHrs = stdWrkHrs.divide(100.0);
								//convert HHMM to HHDec
								stdWrkHrs = convHHDec(stdWrkHrs);							
							}
							else{
								stdWrkHrs = new BigDecimal(swh);
							}
							//info("stdWrkHrs:"+stdWrkHrs);
						}
					}
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processMSF820 failed - ${e.getMessage()}");
		}
	}
	
	//convert hrs to HHDec
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
//		info ("HHDEC: " + HHDec);
		return HHDec;
	}
	
	/**
	 * Get PRC's description from 010 table. <br>
	 **/
	private void getPRCDesc (String PRC870)
	{
		// Set defaults because MSF766 does not have them.
		PRC02 = "";
		PRC03 = "";
		PRC04 = "";
		int idx;
		int cntr;
		int nextIdx;
		int len;
		
		if (PRC870.equals("")) return;

		try
		{
			// 'TG  CPD BAA GRPM....'
			//info("PRC870   :" + PRC870)
			
			//use PRC name in MSF808 instead
			Constraint c1 = MSF808Key.primRptCodes.equalTo(PRC870);

			def query = new QueryImpl(MSF808Rec.class).and(c1);
			MSF808Rec msf808Rec = (MSF808Rec) edoi.firstRow(query);
			
			if (msf808Rec){
				//separate PRC name into segments
				//PRC: 'TG  PSCSP&C RAR ....'
				//PRC name: 'PSCS/P&C/RECRUITMENT'
				
				//info("PRC name: " + msf808Rec.getPrcName());
				idx = 0;
				cntr = 1;
				nextIdx = 0;
				len = msf808Rec.getPrcName().size();
				//check up to (len - 1) or the If condition will fail			
				while (idx <= (len - 1))
				{	
					//length of substring = endIndex - beginIndex.
					if (msf808Rec.getPrcName().substring(idx,idx + 1) == '/')
					{
						switch (cntr)
						{	
							case 1:
								//info("cntr1: " + cntr);
								//info("PRC02: " + msf808Rec.getPrcName().substring(0,idx));
								PRC02 = msf808Rec.getPrcName().substring(0,idx);
								nextIdx = idx + 1;
								cntr++;
								break;
							case 2:
								//info("cntr2: " + cntr);
								//info("PRC03: " + msf808Rec.getPrcName().substring(nextIdx,idx));
								PRC03 = msf808Rec.getPrcName().substring(nextIdx,idx);
								nextIdx = idx + 1;
								cntr++;
								break;
							default:
								info ("More than 3 segments");
								break;
						}
					}
					idx++;
				}

				//get branch name
				if (cntr == 3){
					PRC04 = msf808Rec.getPrcName().substring(nextIdx,len);
				}
				else{
					if (cntr == 2)
					{
						PRC03 = msf808Rec.getPrcName().substring(nextIdx,len);
					}
				}
				
//				info("PRC02: " + PRC02);
//				info("PRC03: " + PRC03);
//				info("PRC04: " + PRC04);
			}

			//old codes		
			/*			if (msf808Rec)
			 {
								 
				 String desc = msf808Rec.getPrcName();
				 String[] descarray = desc.split(/\//);
				 
				 if (descarray.size() >= 1)
					 PRC02 = descarray[0];
				 if (descarray.size() >= 2 )
					 PRC02 = descarray[1];
				 if (descarray.size() >= 3)
					 PRC03 = descarray[2];
				 if (descarray.size() >= 4)
					 PRC04 = descarray[3];
			 }*/
			
/*			Constraint c1 = MSF010Key.tableType.equalTo("PC02");
			Constraint c2 = MSF010Key.tableCode.equalTo(PRC870.substring(4,8));
			Constraint c3 = MSF010Key.tableType.equalTo("PC03");
			Constraint c4 = MSF010Key.tableCode.equalTo(PRC870.substring(8,12));
			Constraint c5 = MSF010Key.tableType.equalTo("PC04");
			Constraint c6 = MSF010Key.tableCode.equalTo(PRC870.substring(12,16));

			def query1 = new QueryImpl(MSF010Rec.class).and(c1.and (c2));
			MSF010Rec msf010Rec1 = (MSF010Rec) edoi.firstRow(query1);
			if (!msf010Rec1.getTableDesc().trim().equals(""))
			{
				PRC02 = msf010Rec1.getTableDesc();
			}
			def query2 = new QueryImpl(MSF010Rec.class).and(c3 .and(c4));
			MSF010Rec msf010Rec2 = (MSF010Rec) edoi.firstRow(query2);
			if (!msf010Rec2.getTableDesc().trim().equals(""))
			{
				PRC03 = msf010Rec2.getTableDesc();
			}
			def query3 = new QueryImpl(MSF010Rec.class).and(c5 .and (c6));
			MSF010Rec msf010Rec3 = (MSF010Rec) edoi.firstRow(query3);
			if (!msf010Rec3.getTableDesc().trim().equals(""))
			{
				PRC04 = msf010Rec3.getTableDesc();
			}*/
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("getPRCDesc failed - ${e.getMessage()}");
		}
	}	
	
	/**
	 * Get employee's name. <br>
	 **/
	private void processMSF810 ()
	{
		try
		{
			empSurname = "";
			empFirstName = "";
			Constraint c1 = MSF810Key.employeeId.equalTo(empId888);

			def query = new QueryImpl(MSF810Rec.class).and(c1);
			MSF810Rec msf810Rec = (MSF810Rec) edoi.firstRow(query);
			if  (msf810Rec)
			{
				empSurname = msf810Rec.getSurname();
				empFirstName = msf810Rec.getFirstName();
			}
			//info("Name : " + empFirstName + " " + empSurname);
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processMSF810 failed - ${e.getMessage()}");
		}
	}	
	
	/**
	 * Read MSF760 to get employee's type. <br>
	 * <li> It will be reported in the CSV details file </li>
	 **/
	private void processMSF760 ()
	{
		try
		{
			//get employee type
			Constraint c1 = MSF760Key.employeeId.equalTo(empId888);
			def query = new QueryImpl(MSF760Rec.class).and (c1);
			MSF760Rec msf760Rec = (MSF760Rec) edoi.firstRow(query);
			
			if (msf760Rec)
			{
				empType = msf760Rec.getEmpType();
			}
			//info("empType : " + empType);
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			info("processMSF760 failed - ${e.getMessage()}");
		}
	}	
	
	/**
	 * Batch report and CSV files. <br>
	 * <li> define getter and setter for the batch </li>
	 **/
	private class Trb8slaReportLine implements Comparable<Trb8slaReportLine>
	{
		private String businessUnit;
		private String group;
		private String branch;
		private String surname;
		private String firstName;
		private String employeeId;
		private String empType;
		private String trnDate;
		private String workCode;
		private String workCodeDesc;
		private BigDecimal stdHrs;
		private BigDecimal actualHrs;
		private String lveStrDate;
		private String lveEndDate;
		private String lveReason;
		private String lveCertFlag;
		
		public Trb8slaReportLine(String newBusinessUnit,
								String newGroup,
								String newBranch,
								String newEmployeeId,
								String newFirstName,
								String newSurname,
								String newEmpType,
								String newTrnDate,
								String newWorkCode,
								String newWorkCodeDesc,
								BigDecimal newStdHrs,
								BigDecimal newActualHrs,
								String newLveStrDate,
								String newLveEndDate,
								String newLveReason,
								String newLveCertFlag)
		{
			setBusinessUnit(newBusinessUnit);
			setGroup (newGroup);
			setBranch(newBranch);
			setEmployeeId(newEmployeeId);
			setFirstName(newFirstName);
			setSurname(newSurname);
			setEmpType(newEmpType);
			setTrnDate(newTrnDate);
			setWorkCode(newWorkCode);
			setWorkCodeDesc(newWorkCodeDesc);
			setStdHrs(newStdHrs);
			setActualHrs(newActualHrs);
			setLveCertFlag(newLveCertFlag);
			setLveEndDate(newLveEndDate);
			setLveReason(newLveReason);
			setLveStrDate(newLveStrDate);
			
//			info("====> ADDING Trb8slaReportLine");
//			info("newBusinessUnit	:"+newBusinessUnit);
//			info("newGroup		    :"+newGroup);
//			info("newBranch		    :"+newBranch);
//			info("newEmployeeId	    :"+newEmployeeId);
//			info("newFirstName		:"+newFirstName);
//			info("newSurname		:"+newSurname);
//			info("newEmpType		:"+newEmpType);
//			info("newTrnDate		:"+newTrnDate);
//			info("newWorkCode		:"+newWorkCode);
//			info("newWorkCodeDesc	:"+newWorkCodeDesc);
//			info("newStdHrs			:"+newStdHrs);
//			info("newActualHrs		:"+newActualHrs);
//			info("newLveStrDate		:"+newLveStrDate);
//			info("newLveEndDate		:"+newLveEndDate);
//			info("newLveReason		:"+newLveReason);
//			info("newLveCertFlag	:"+newLveCertFlag);

		}

		public void setBusinessUnit(String newBusinessUnit)
		{
			businessUnit = newBusinessUnit;
		}
		public String getBusinessUnit()
		{
			if (businessUnit == null) return "";
			return businessUnit;
		}
	
		public void setGroup(String newGroup)
		{
			group = newGroup;
		}
		public String getGroup()
		{
			return group;
		}
		
		public void setBranch(String newBranch)
		{
			branch = newBranch;
		}
		public String getBranch()
		{
			return branch;
		}

		public void setSurname(String newSurname)
		{
			surname = newSurname;
		}
		public String getSurname()
		{
			return surname;
		}

		public void setFirstName(String newFirstName)
		{
			firstName = newFirstName;
		}
		public String getFirstName()
		{
			return firstName;
		}
		
		public String getSecondName()
		{
			return secondName;
		}
		public String getPackedName(){
			return (surname+", "+firstName);
		}
		
		public void setEmployeeId(String newEmployeeId)
		{
			employeeId = newEmployeeId;
		}
		public String getEmployeeId()
		{
			return employeeId;
		}

		public void setEmpType(String newEmpType)
		{
			empType = newEmpType;
		}
		public String getEmpType()
		{
			return empType;
		}

		public void setTrnDate(String newTrnDate)
		{
			trnDate = newTrnDate;
		}
		public String getTrnDate()
		{
			return trnDate;
		}
		
		public void setWorkCode(String newWorkCode)
		{
			workCode = newWorkCode;
		}
		public String getWorkCode()
		{
			return workCode;
		}
		
		public void setWorkCodeDesc(String newWorkCodeDesc)
		{
			workCodeDesc = newWorkCodeDesc;
		}
		public String getWorkCodeDesc()
		{
			return workCodeDesc;
		}
		
		public void setStdHrs(BigDecimal newStdHrs)
		{
			stdHrs = newStdHrs;
		}
		public BigDecimal getStdHrs()
		{
			return stdHrs;
		}

		public void setActualHrs(BigDecimal newActualHrs)
		{
			actualHrs = newActualHrs;
		}
		public BigDecimal getActualHrs()
		{
			return actualHrs;
		}

		public void setLveStrDate(String newLveStrDate)
		{
			lveStrDate = newLveStrDate;
		}
		public String getLveStrDate()
		{
			return lveStrDate;
		}

		public void setLveEndDate(String newLveEndDate)
		{
			lveEndDate = newLveEndDate;
		}
		public String getLveEndDate()
		{
			return lveEndDate;
		}

		public void setLveReason(String newLveReason)
		{
			lveReason = newLveReason;
		}
		public String getLveReason()
		{
			return lveReason;
		}

		public void setLveCertFlag(String newLveCertFlag)
		{
			lveCertFlag = newLveCertFlag;
		}
		public String getLveCertFlag()
		{
			return lveCertFlag;
		}
		
		//report is sorted via business unit, group and branch
		int compareTo(Trb8slaReportLine otherReportLine)
		{
//			info ("other line: " + otherReportLine);
//			info ("BU: " + businessUnit);
//			info ("other BU: " + otherReportLine.getBusinessUnit());
//			info ("group: " + group);
//			info ("other group: " + otherReportLine.getGroup());
//			info ("Branch: " + branch);
//			info ("other Branch: " + otherReportLine.getBranch());
//			info ("empId: " + employeeId);
//			info ("other empId: " + otherReportLine.getEmployeeId());
	
			if (!businessUnit.equals(otherReportLine.getBusinessUnit()))
			{
				return businessUnit.compareTo(otherReportLine.getBusinessUnit())
			}
			if (!group.equals(otherReportLine.getGroup()))
			{
				return group.compareTo(otherReportLine.getGroup())
			}
			if (!branch.equals(otherReportLine.getBranch()))
			{
				return branch.compareTo(otherReportLine.getBranch())
			}
			if (!employeeId.equals(otherReportLine.getEmployeeId()))
			{
				return employeeId.compareTo(otherReportLine.getEmployeeId())
			}
			return 0;
		}
	}	
	
	/**
	 * generate batch report & CSV files <br>
	 * <li> 1. In the batch report, lve hrs and days are calculated per employee </li>
	 * <li> then sub total by branch and grand total at the end of report. </li>
	 * <li> 2. In the CSV details file, lve transaction is reported by date, </li>
	 * <li> then sub total by branch and grand total at the end. </li>
	 * <li> 3. In the CSV summary file, lve transaction is grouped by employee </li>
	 * <li> then sub total by branch and grand total at the end. </li>
	 **/
	private generateTrb8slReport()
	{	
		info (" ");
		info (" ");
		info (" ");
		info ("Process generateTrb8slReport");

		Collections.sort(arrayOfTrb8slReportLine);
		
		info("Creating report");
		ReportHeader rpt = new ReportHeader();

		for (int i = 0; i < arrayOfTrb8slReportLine.size(); i++)
		{
			Trb8slaReportLine line = arrayOfTrb8slReportLine.get(i);
			
//			info(" Business Unit    :" + line.getBusinessUnit());
//			info(" Group            :" + line.getGroup ());
//			info(" Branch           :" + line.getBranch());
//			info(" Employee Id      :" + line.getEmployeeId());

			rpt.AddLine(line);
//			info("=========================================");
		}
//		info("adding is done - now print");
		rpt.print();
//		info("printing is done");
		ReportA.close();
		ReportB.close();
		ReportC.close();
		
	}	
	
	private String DateToString(String indate)
	{
	    String dd = indate.substring(6,8);
	    String mm = indate.substring(4,6);
	    String yy = indate.substring(2,4);
		return dd + "/" + mm + "/" + yy;
	}
	/**
	 * write batch report header <br>
	 **/
	//private void writeReportHeader(Trb8slaReportLine reportLine)
	private void writeReportHeader()
	{	
		info("writeReportHeader");
		String tempString;
		
		ReportA.writeLine(132,"-");
		ThrowNewLine();
		
		DoReportA(StringUtils.center("Sick Leave Summary Report", 132));
		
		if (batchParams.paramPRC.trim().equals(""))
		{
		    tempString = "PRC: (Blank for all)  " ;
		}
		else
		{
		    tempString = "PRC: " + batchParams.paramPRC.padRight(30) + "(Blank for all)  " ;
		}
		tempString = tempString + "Transaction Date From " + DateToString(batchParams.paramDateFrom) + " To " + DateToString(batchParams.paramDateTo);
		DoReportA(StringUtils.center(tempString,132));
		ReportA.writeLine(132,"-");
		ThrowNewLine();
		
		tempString = "                                                                                      Family Care               Sick Leave";
		//tempString = "                                                                              Family Care              Sick Leave";
		DoReportA(tempString);
		
		tempString = "                                                                                   With       Without        With       Without";
		//tempString = "                                                                           With       Without        With       Without";
		DoReportA(tempString);
		tempString = "                                                                                 Certificate  Certificate  Certificate  Certificate";
		//tempString = "                                                                         Certificate  Certificate  Certificate  Certificate";
		DoReportA(tempString);

		tempString = "Business    Group             Branch           Employee Employee Name            Hours  Days  Hours  Days  Hours  Days  Hours  Days";
		//tempString = "Business    Group       Branch      Employee  Employee Name              Hours  Days  Hours  Days  Hours  Days  Hours  Days";
		DoReportA(tempString);

		tempString = "  Unit                                           Id";
		//tempString = "  Unit                                Id";
		DoReportA(tempString);
		
		ReportA.writeLine(132,"-");
		ThrowNewLine();
	}

	//convert all HHDecimal to HHMM for report and CSV files
	private BigDecimal convHHMM (BigDecimal lveHrsDec)
	{
		try
		{
			//info(" convHHMM  IN:" + lveHrsDec.toString());		
			String sMM;
			String sHH;
			String sHHMM;
			BigDecimal MM;
			BigDecimal HHMM;
			Boolean decFnd;
			sMM = "";
			HHMM = 0;
			
			String sLveHrsDec = lveHrsDec.toString();
			decFnd = false;
			
			//find decimal place
			int idx = 0;
			int idx2 = 1;
			while (idx2 <= sLveHrsDec.size())
			{
				if (sLveHrsDec.substring(idx, idx2).equals("."))
				{
					decFnd = true;
				}
				idx++;
				idx2++;
			}
			if (decFnd)
			{			
				//get decimal and convert to MM
				sMM = sLveHrsDec.substring(sLveHrsDec.indexOf('.') + 1, sLveHrsDec.size());
				//only convert decimal to MM where applicable
				if (sMM.trim() != ""){
					MM = Math.round((sMM.toBigDecimal() * 60) /100);
					//get HH
					sHH = sLveHrsDec.substring(0, sLveHrsDec.indexOf('.'));
					sHHMM = sHH + "." + MM.toString();
					
					//convert to HHMM
					HHMM = sHHMM.toBigDecimal();
				}
			}
			else{
				HHMM = lveHrsDec;
			}
			
			//info(" convHHMM OUT:" + HHMM.toString());
			return HHMM;
		}
		catch (Exception ex)
		{
			//info(" convHHMM OUT  AHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
			return 0.0;
		}
	}
	
	public void printAandCLine(String BusinessUnit,String Group,String Branch,String FormattedEmployeeId,String Surname,String FirstName,String Name,
		BigDecimal FCHrs,BigDecimal FCDays,BigDecimal FCWOHrs,BigDecimal FCWODays,
		BigDecimal SLHrs,BigDecimal SLDays,BigDecimal SLWOHrs,BigDecimal SLWODays,
		BigDecimal noOfOcc1,BigDecimal noOfOcc2,BigDecimal noOfOcc3,BigDecimal noOfOcc4,BigDecimal TotalHrs,BigDecimal TotalDays)
	{
		String tempString = "";
		tempString = " " + CutString(BusinessUnit,4).padRight(6);
		tempString = tempString + CutString(Group,20).padRight(20);
		tempString = tempString + CutString(Branch,20).padRight(20);
		tempString = tempString + CutString(FormattedEmployeeId,6).padRight(7); 
		tempString = tempString + CutString(Name,25).padRight(25);
		tempString = tempString + decFormatter.format(convHHMM(FCHrs)).padLeft(7);
		tempString = tempString + decFormatter.format(FCDays).padLeft(6);
		tempString = tempString + decFormatter.format(convHHMM(FCWOHrs)).padLeft(7);
		tempString = tempString + decFormatter.format(FCWODays).padLeft(6);
		tempString = tempString + decFormatter.format(convHHMM(SLHrs)).padLeft(7);
		tempString = tempString + decFormatter.format(SLDays).padLeft(6);
		tempString = tempString + decFormatter.format(convHHMM(SLWOHrs)).padLeft(7);
		tempString = tempString + decFormatter.format(SLWODays).padLeft(6);

//		tempString = " " + CutString(BusinessUnit,11).padRight(11);
//		tempString = tempString + CutString(Group,11).padRight(11);
//		tempString = tempString + CutString(Branch,13).padRight(13);
//		tempString = tempString + CutString(FormattedEmployeeId,6).padRight(6);
//		tempString = tempString + CutString(Name,29).padRight(29);
//		tempString = tempString + decFormatter.format(convHHMM(FCHrs)).padLeft(7);
//		tempString = tempString + decFormatter.format(FCDays).padLeft(6);
//		tempString = tempString + decFormatter.format(convHHMM(FCWOHrs)).padLeft(7);
//		tempString = tempString + decFormatter.format(FCWODays).padLeft(6);
//		tempString = tempString + decFormatter.format(convHHMM(SLHrs)).padLeft(7);
//		tempString = tempString + decFormatter.format(SLDays).padLeft(6);
//		tempString = tempString + decFormatter.format(convHHMM(SLWOHrs)).padLeft(7);
//		tempString = tempString + decFormatter.format(SLWODays).padLeft(6);

		DoReportA(tempString);
		
		String APP = "\"";
		
		ReportC.write(APP + BusinessUnit + APP + "," +
			APP + Group + APP + "," +
			APP + Branch + APP + "," +
			APP + Surname + APP + "," +
			APP + FirstName + APP + ","+
			APP + FormattedEmployeeId + APP + "," +
			decFormatter.format(convHHMM(FCHrs)) + "," + decFormatter.format(FCDays) + "," + noOfOcc1 + "," +
			decFormatter.format(convHHMM(FCWOHrs)) + "," + decFormatter.format(FCWODays) + "," + noOfOcc2 + "," +
			decFormatter.format(convHHMM(SLHrs)) + "," + decFormatter.format(SLDays) + "," + noOfOcc3 + "," +
			decFormatter.format(convHHMM(SLWOHrs)) + "," + decFormatter.format(SLWODays) + "," + noOfOcc4 + "," +
			decFormatter.format(convHHMM(TotalHrs)) + "," +
			decFormatter.format(TotalDays)  + 
			"\n")
	}
	private String CutString(String val, Integer i)
	{
		try
		{
			return val.substring(0,i);
		}
		catch (Exception ex)
		{
			return val;
		}
	}
	
	public void printATotal(String literal,BigDecimal val)
	{
		String space = " ";
		String tempString = "";
		tempString = space.padLeft(47);	 
		//tempString = space.padLeft(43);
		tempString = tempString + literal.padRight(29);
		tempString = tempString + decFormatter.format(val).padLeft(10);

		DoReportA(tempString);
	}
	public void printABorderLine()
	{
		ReportA.writeLine(132,"-");
		ThrowNewLine();
	}
	public void printABlankLine()
	{
		ReportA.writeLine(132," ");
		ThrowNewLine();
	}
	private void DoReportA(String line)
	{
		//info("Report A: " + line);
		ReportA.write(line);
		ThrowNewLine();
	}
	public void ThrowNewLine()
	{
		ReportALineCounter++;
		info("ThrowNewLine " + ReportALineCounter);
		
		if (ReportALineCounter == 66) // stop on 73 - 1 for line, 1 for newline + 6 for header
		{
			//ReportA.writeLine(132,"=");
			ReportA.write("\f");
			ReportALineCounter = 0;
			writeReportHeader();
			ReportA.writeLine(132,"=");
		}
	}
	public void printBLine(String BusinessUnit,String Group,String Branch,String FormattedEmployeeId,String Surname,String FirstName,String EmpType,
		BigDecimal StdHrs,String Day,String TrnDate,String WorkCode,String WorkCodeDesc,
		BigDecimal TotalHrs,BigDecimal TotalDays)
	{
		String APP = "\"";
		
		//info("printBLine");
		//convert to HHMM
		String stdHoursSTR = convHHMM(StdHrs).toString();

		if (FormattedEmployeeId == "") stdHoursSTR = "";
		//info("in printBLine 3");
		ReportB.write(APP + BusinessUnit + APP + "," +
			APP + Group + APP + "," +
			APP + Branch + APP + "," +
			APP + Surname + APP + "," +
			APP + FirstName + APP + ","+
			APP + FormattedEmployeeId + APP + "," +
			APP + EmpType + APP + "," +
			stdHoursSTR + "," +
			APP + Day + APP + "," +			//dayFormatter.format(reportLine.getTrnDate()) + "," +
			APP + TrnDate + APP + "," +		//dayFormatter.format(reportLine.getTrnDate()) + "," +
			APP + WorkCode + APP + "," +
			APP + WorkCodeDesc + APP + "," +
			decFormatter.format(convHHMM(TotalHrs)) + "," +
			decFormatter.format(TotalDays)  + "," + "\n");
		//info("in printBLine 4");
	}
	
	/**
	 * print batch report <br>
	 * <li> output total record counts </li>
	 **/
	private void printBatchReport()
	{
		info("printBatchReport");
		println ("\n");
		println(StringUtils.center("End of Report ", 132));
	}	
	
	//GRAND TOTAL
	public class ReportHeader
	{
		public ArrayList Sections;
		
		public ReportHeader()
		{
			Sections = new ArrayList();
		}	
		
		public void AddLine(Trb8slaReportLine line)
		{
			
			String key = line.getBusinessUnit() + "-" + line.getGroup() + "-" + line.getBranch();
			//info("ReportHeader.AddLine :" + key);
			
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				if (s.getKey().equals(key))
				{
					//info("found and added");
					s.AddLine(line);
					return;
				}
			}
			//info("newly added");
			ReportSection s = new ReportSection(line);
			Sections.add(s);
		}
		
		public void print()
		{
			// Now print
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				s.print();
			}
			// now print footer
			printTotals();
		}
		public void printTotals()
		{
			// TODO
			// print grand total
			// report	Y
			// CSV 1	Y
			// CSV 2	Y
			
			printABorderLine();
			printAandCLine("","","","Total","","","for Report",FCHrs(),FCDays(),
						FCWOHrs(),FCWODays(),SLHrs(),SLDays(),SLWOHrs(),SLWODays(),
						noOfOcc1(),noOfOcc2(),noOfOcc3(),noOfOcc4(),TotalHrs(),TotalDays());
			printATotal("No of Employees Processed",NoEmployeesProcessed());
			printATotal("No of Leave Transactions",NoLeaveTransaction());
			printATotal("Average Days Leave per Emp",AverageDaysPerEmp());
			printABlankLine();
			printABorderLine();
			
			printBLine("","","","","","","",0,"","","","",TotalHrs(),TotalDays());

		}
		
		public BigDecimal NoEmployeesProcessed()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.NoEmployeesProcessed();
			}
			return val;
		}
		public BigDecimal NoLeaveTransaction()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.NoLeaveTransaction();
			}
			return val;
		}
		public BigDecimal AverageDaysPerEmp()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.AverageDaysPerEmp();
			}
			return val;
		}

		public BigDecimal FCHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.FCHrs();
			}
			return val;
		}
		public BigDecimal FCDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.FCDays();
			}
			return val;
		}
		public BigDecimal FCWOHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.FCWOHrs();
			}
			return val;
		}
		public BigDecimal FCWODays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.FCWODays();
			}
			return val;
		}
		public BigDecimal SLHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.SLHrs();
			}
			return val;
		}
		public BigDecimal SLDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.SLDays();
			}
			return val;
		}
		public BigDecimal SLWOHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.SLWOHrs();
			}
			return val;
		}
		public BigDecimal SLWODays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.SLWODays();
			}
			return val;
		}
		public BigDecimal noOfOcc1()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.noOfOcc1();
			}
			return val;
		}
		public BigDecimal noOfOcc2()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.noOfOcc2();
			}
			return val;
		}
		public BigDecimal noOfOcc3()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.noOfOcc3();
			}
			return val;
		}
		public BigDecimal noOfOcc4()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.noOfOcc4();
			}
			return val;
		}
		// Actual Hours
		public BigDecimal TotalHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.TotalHrs();
			}
			return val;
		}
		// Days
		public BigDecimal TotalDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Sections.size(); i++)
			{
				ReportSection s = Sections[i];
				val = val + s.TotalDays();
			}
			return val;
		}
	}

	// PER SECTION
	public class ReportSection
	{
		public ArrayList Employees;
		
		public String BusinessUnit = "";
		public String Group = "";
		public String Branch = "";
		
		public ReportSection()
		{
			Employees = new ArrayList();
		}
		public ReportSection(Trb8slaReportLine line)
		{
			Employees = new ArrayList();
			BusinessUnit = line.getBusinessUnit();
			Group = line.getGroup();
			Branch = line.getBranch();
			AddLine(line);
		}
		
		public String getKey()
		{
			return BusinessUnit + "-" + Group + "-" + Branch;
		}
		public void AddLine(Trb8slaReportLine line)
		{
			String key = line.getEmployeeId();
			//info("ReportSection.AddLine :" + key);
			
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				if (s.getKey().equals(key))
				{
					s.AddLine(line);
					return;
				}
			}
			EmployeeSection s = new EmployeeSection(line);
			Employees.add(s);
		}
		
		public void print()
		{
			// Now print
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				s.print();
			}
			
			// now print footer
			printTotals();
			
		}
		public void printTotals()
		{
			// TODO
			// print section total
			// report	Y
			// CSV 1	Y
			// CSV 2	Y
			
			//convert HHDec to HHMM before print report
			printAandCLine(BusinessUnit,Group,Branch,"","","","",FCHrs(),FCDays(),FCWOHrs(),
							FCWODays(),SLHrs(),SLDays(),SLWOHrs(),SLWODays(),noOfOcc1(),
							noOfOcc2(),noOfOcc3(),noOfOcc4(),TotalHrs(),TotalDays());
			DoReportA("");
			
			printBLine(BusinessUnit,Group,Branch,"","","","",0,"","","","",TotalHrs(),TotalDays());
			
		}
		
		public BigDecimal NoEmployeesProcessed()
		{
			return Employees.size();
		}
		public BigDecimal NoLeaveTransaction()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.NoLeaveTransaction();
			}
			return val;
		}
		public BigDecimal AverageDaysPerEmp()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.AverageDaysPerEmp();
			}
			return val;
		}

		public BigDecimal FCHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.FCHrs();
			}
			return val;
		}
		public BigDecimal FCDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.FCDays();
			}
			return val;
		}
		public BigDecimal FCWOHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.FCWOHrs();
			}
			return val;
		}
		public BigDecimal FCWODays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.FCWODays();
			}
			return val;
		}
		public BigDecimal SLHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.SLHrs();
			}
			return val;
		}
		public BigDecimal SLDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.SLDays();
			}
			return val;
		}
		public BigDecimal SLWOHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.SLWOHrs();
			}
			return val;
		}
		public BigDecimal SLWODays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.SLWODays();
			}
			return val;
		}
		public BigDecimal noOfOcc1()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.noOfOcc1();
			}
			return val;
		}
		public BigDecimal noOfOcc2()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.noOfOcc2();
			}
			return val;
		}
		public BigDecimal noOfOcc3()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.noOfOcc3();
			}
			return val;
		}
		public BigDecimal noOfOcc4()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.noOfOcc4();
			}
			return val;
		}
		// Actual Hours
		public BigDecimal TotalHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.TotalHrs();
			}
			return val;
		}
		// Days
		public BigDecimal TotalDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Employees.size(); i++)
			{
				EmployeeSection s = Employees[i];
				val = val + s.TotalDays();
			}
			return val;
		}
	}
	
	public class EmployeeSection
	{
		public ArrayList Transactions;
		
		public String BusinessUnit = "";
		public String Group = "";
		public String Branch = "";	
		public String EmployeeId = "";
		public String Surname = "";
		public String FirstName = "";
		
		public EmployeeSection()
		{
			Transactions = new ArrayList();
		}
		
		public EmployeeSection(Trb8slaReportLine line)
		{
			//info("EmployeeSection");
			Transactions = new ArrayList();
			
			BusinessUnit = line.getBusinessUnit();
			Group = line.getGroup();
			Branch = line.getBranch();
			EmployeeId = line.getEmployeeId();
			Surname = line.getSurname();
			FirstName = line.getFirstName();
			
			//info("EmployeeSection EmployeeId:" + EmployeeId);
			AddLine(line);
		}
		public String getKey()
		{
			return EmployeeId;
		}
		public void AddLine(Trb8slaReportLine line)
		{
			//info("EmployeeSection.AddLine enter");
			LeaveRecordSection s = new LeaveRecordSection(line);
			
			//info("EmployeeSection.AddLine new rec created");
			
			Transactions.add(s);
			//info("EmployeeSection.AddLine tran added");
		}
		
		public void print()
		{
			
			// Now print
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				s.print();
			}
			
			// now print footer
			printTotals();
			
		}
		
		public void printTotals()
		{
			// TODO
			// print employee total
			// report	Y
			// CSV 1	Y
			// CSV 2	N
			
			//convert HHDec to HHMM before printing report
			printAandCLine(BusinessUnit,Group,Branch,FormattedEmployeeId(),Surname, FirstName,Name(),
				FCHrs(),FCDays(),FCWOHrs(),FCWODays(),SLHrs(),SLDays(),
				SLWOHrs(),SLWODays(),noOfOcc1(),noOfOcc2(),noOfOcc3(),noOfOcc4(),
				TotalHrs(),TotalDays());
		}

		public String Name()
		{
			return FirstName + " " + Surname;
		}
		public String FormattedEmployeeId()
		{
			String s = EmployeeId;
			s = s.replaceFirst ("^0*", "");
			return s;
		}
		
		public BigDecimal NoLeaveTransaction()
		{
			return Transactions.size();
		}
		public BigDecimal AverageDaysPerEmp()
		{
			BigDecimal days = TotalDays();
			BigDecimal norecs = Transactions.size();
			if (norecs == 0) return 0;
			return (days / norecs);
		}
		
		public BigDecimal FCHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.FCHrs;
			}
			return val;
		}
		public BigDecimal FCDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.FCDays;
			}
			return val;
		}
		public BigDecimal FCWOHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.FCWOHrs;
			}
			return val;
		}
		public BigDecimal FCWODays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.FCWODays;
			}
			return val;
		}
		public BigDecimal SLHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.SLHrs;
			}
			return val;
		}
		public BigDecimal SLDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.SLDays;
			}
			return val;
		}
		public BigDecimal SLWOHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.SLWOHrs;
			}
			return val;
		}
		public BigDecimal SLWODays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.SLWODays;
			}
			return val;
		}
		public BigDecimal noOfOcc1()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.noOfOcc1;
			}
			return val;
		}
		public BigDecimal noOfOcc2()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.noOfOcc2;
			}
			return val;
		}
		public BigDecimal noOfOcc3()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.noOfOcc3;
			}
			return val;
		}
		public BigDecimal noOfOcc4()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.noOfOcc4;
			}
			return val;
		}
		// Actual Hours 
		public BigDecimal TotalHrs()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.TotalHrs();
			}
			return val;
		}
		// Days
		public BigDecimal TotalDays()
		{
			BigDecimal val = 0;
			for (int i = 0; i < Transactions.size(); i++)
			{
				LeaveRecordSection s = Transactions[i];
				val = val + s.TotalDays();
			}
			return val;
		}
	}

	
	public class LeaveRecordSection
	{
		public String BusinessUnit = "";
		public String Group = "";
		public String Branch = "";

		public String Surname;
		public String FirstName;
		public String EmployeeId;
		public String EmpType;
		public String TrnDate;
		public String WorkCode;
		public String WorkCodeDesc;
		public BigDecimal StdHrs;
		public BigDecimal ActualHrs;
		public String LveStrDate;
		public String LveEndDate;
		public String LveReason;
		public String LveCertFlag;
		
		private BigDecimal FCHrs;
		private BigDecimal FCDays;
		private BigDecimal FCWOHrs;
		private BigDecimal FCWODays;
		private BigDecimal SLHrs;
		private BigDecimal SLDays;
		private BigDecimal SLWOHrs;
		private BigDecimal SLWODays;
		private BigDecimal noOfOcc1;
		private BigDecimal noOfOcc2;
		private BigDecimal noOfOcc3;
		private BigDecimal noOfOcc4;
		
		public LeaveRecordSection(Trb8slaReportLine line)
		{
			
			BusinessUnit = line.getBusinessUnit();
			Group = line.getGroup();
			Branch = line.getBranch();
			Surname = line.getSurname();
			FirstName = line.getFirstName();
			EmployeeId = line.getEmployeeId();
			EmpType = line.getEmpType();
			TrnDate = line.getTrnDate();
			WorkCode = line.getWorkCode();
			WorkCodeDesc = line.getWorkCodeDesc();
			StdHrs = line.getStdHrs();
			ActualHrs = line.getActualHrs();
			LveStrDate = line.getLveStrDate();
			LveEndDate = line.getLveEndDate();
			LveReason = line.getLveReason();
			LveCertFlag = line.getLveCertFlag();	
			
			FCHrs = 0;
			FCDays = 0;
			FCWOHrs = 0;
			FCWODays = 0;
			SLHrs = 0;
			SLDays = 0;
			SLWOHrs = 0;
			SLWODays = 0;
			noOfOcc1 = 0;
			noOfOcc2 = 0;
			noOfOcc3 = 0;
			noOfOcc4 = 0;
			
//			info("LveReason   : " + line.getLveReason());
//			info("LveCertFlag : " + line.getLveCertFlag());
//			info("getLveEndDate : " + line.getLveEndDate());
//			info("getLveStrDate : " + line.getLveStrDate());
//			info("getTrnDate : " + line.getTrnDate());		
//			info("FCHrs : " + FCHrs.toString());
			
			
			//family care
			if (line.getLveReason().equals("F"))
			{
				//with certificate
				if (line.getLveCertFlag().equals("Y"))
				{					
					FCHrs = FCHrs + line.getActualHrs();
					if (line.getStdHrs() != 0) {			
						FCDays = FCDays + (FCHrs / line.getStdHrs());}
					// calculate no of occurrences
					noOfOcc1 = noOfOcc1 + 1;						
				}
				else //without certificate
				{
					FCWOHrs = FCWOHrs + line.getActualHrs();
					if (line.getStdHrs() != 0){
						FCWODays = FCWODays + (FCWOHrs / line.getStdHrs());}
					// calculate no of occurrences
					noOfOcc2 = noOfOcc2 + 1;
				}
			}
			else //sick leave
			{
				if (line.getLveReason().equals("S"))
				{
					//with certificate
					if (line.getLveCertFlag().equals("Y"))
					{						
						SLHrs = SLHrs + line.getActualHrs();
						if (line.getStdHrs() != 0){
							SLDays = SLDays + (SLHrs / line.getStdHrs());}
						// calculate no of occurrences
						noOfOcc3 = noOfOcc3 + 1;
					}
					else //without certificate
					{						
						SLWOHrs = SLWOHrs + line.getActualHrs();
						if (line.getStdHrs() != 0){
							SLWODays = SLWODays + (SLWOHrs / line.getStdHrs());}
						// calculate no of occurrences
						noOfOcc4 = noOfOcc4 + 1;
					}
				}
			}	
			
			
		}
		
		public void print()
		{
			// TODO
			// print leave transaction
			// report	N
			// CSV 1	N
			// CSV 2	Y
			
			printBLine(BusinessUnit,Group,Branch,FormattedEmployeeId(),Surname,FirstName,EmpType,StdHrs,Day(),TrnDate,WorkCode,WorkCodeDesc,TotalHrs(),TotalDays());
			
		}

		public BigDecimal TotalHrs()
		{
			return FCHrs + FCWOHrs + SLHrs + SLWOHrs;
		}
		public BigDecimal TotalDays()
		{
			return FCDays + FCWODays + SLDays + SLWODays;
		}
		
		public String Name()
		{
			return FirstName + " " + Surname;
		}
		public String FormattedEmployeeId()
		{
			String s = EmployeeId;
			s = s.replaceFirst ("^0*", "");
			return s;
		}
		
		public String Day()
		{
			Date newerdate = new Date().parse("yyyyMMdd", TrnDate)
			String val = newerdate.format("E");
			return val;
		}
		
	}
}
/**
 * Run the script
 */
ProcessTrb8sl process = new ProcessTrb8sl()
process.runBatch(binding)