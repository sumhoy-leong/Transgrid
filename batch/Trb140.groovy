/**
 * @AIT 2013
 * 23/07/2013 RL - VERSION 1
 * Changed for Ellipse 8 upgrade. TRR140 will be merged with TRB140.  
 *
 */package com.mincom.ellipse.script.custom

 import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Image;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.ellipse.common.unix.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf140.*;
import com.mincom.ellipse.edoi.ejb.msf141.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.errors.exceptions.*;

import org.apache.commons.lang.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.Comparable;
import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.awt.Font;
import java.text.SimpleDateFormat;
import javax.sql.DataSource;


////-------------------------------------------------------------------
//import groovy.lang.Binding;
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;
//
//public class AITBatch140 implements GroovyInterceptable {
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

public class ParamsTrb140
{
	//List of Input Parameters
	String paramRunType;
}

/**
 * <li> changed for Ellipse 8 upgrade. This is a merge of TRR140 & TRB140. </li>
 * <li> i.e. TRR140 is no longer required. </li>
 * <li> This program will delete all issue requisition header records </li>
 * <li> with no items.</li>
 * <li> Input to this program is file TRO140 which is being created by </li>
 * <li> program TRR140. </li>
 * <li> This program is being copy requested by program TRR140. </li>
 * <li> input: TRO140 </li>
 * <li> output: 1 report - Trb140A. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrb140 extends AITBatch140
public class ProcessTrb140 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrb140 batchParams;
	
	private DataSource dataSource;
	
	private Boolean bEOF;
	
	private ArrayList <iReqRecord> listOfIReqs = new ArrayList <iReqRecord>();
	private ArrayList arrayOfTrb140ReportLine = new ArrayList();
	
	private int recordDstrctCount;
	
	//Report A - batch report
	private def ReportA;

	private DecimalFormat decFormatter = new DecimalFormat("################0.00");
	private DecimalFormat decFormatterNoDec = new DecimalFormat("################0");
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat disDateFormat = new SimpleDateFormat("dd/MM/yyyy");
	
	int ReportALineCounter = 0;
	
	/**
	 * Run the main batch.
	 * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b)
	{
		init(b);
		dataSource = b.getVariable("dataSource");
		ProcessTrb140.printinfo("VERSION      :" + version);
		ProcessTrb140.printinfo("uuid         :" + getUUID());
				
		printSuperBatchVersion();
		
		ProcessTrb140.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb140());

		//PrintRequest Parameters
		ProcessTrb140.printinfo("paramRunType       : " + batchParams.paramRunType);
		ProcessTrb140.printinfo(" ");

		try
		{
			processBatch();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb140.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb140);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb140.printinfo("processBatch");
		
		if (initialise()){
			processRequest();
			generateTrb140Report();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrb140.printinfo("initialise");
		
		recordDstrctCount = 0;
		
		ReportA = report.open('TRB140A')
		writeReportHeaderA();
		
		//validate mandatory fields
		if (batchParams.paramRunType.trim().equals(""))
		{
			WriteError ("Run Mode is mandatory");
			return false;
		}
		if (!batchParams.paramRunType.equals("U") && !batchParams.paramRunType.equals("R"))
		{
			WriteError ("Invalid Run Type: " + batchParams.paramRunType);
			return false;
		}

		return true;
	}

	private void WriteError(String msg)
	{
		ReportA.write(" ");
		ReportA.write(msg);
		ReportA.write(" ");
		ReportA.write(" ");
		ProcessTrb140.printinfo ("Error: " + msg);
	}
	
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb140.printinfo("processRequest");
		String requestByName = "";
		String authByName = "";
		try
		{
			listOfIReqs = getIReqRecords();
			
			//process records
			if (listOfIReqs.size() > 0){
				listOfIReqs.each {iReqRecord rec->
					
					//write to array
					def line = new Trb140aReportLine();
					line.dstrctCode = rec.getDstrctCode();
					line.iReqNo = rec.getiReqNo();
					line.reqById = rec.getRequestedBy();
					requestByName = getEmpName(rec.getRequestedBy());
					line.reqByName = requestByName;
					line.authById = rec.getAuthsdBy();
					authByName = getEmpName(rec.getAuthsdBy());
					line.authByName = authByName;
					line.creationDate = rec.getCreationDate();
					arrayOfTrb140ReportLine.add(line);
					
					if (batchParams.paramRunType.equals("U")){
						deleteIREQ(rec.getDstrctCode(),rec.getiReqNo());
					}
				}
			}			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb140.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb140.printinfo ("processRequest Error: " + ex.message);
		}
	}

	/**
	 * retrieve the ireq and ireq item records.
	 */
	private ArrayList<iReqRecord> getIReqRecords() {
		ProcessTrb140.printinfo("getIReqRecords");
		
		ArrayList <iReqRecord> result = new ArrayList <iReqRecord>()
		
		try
		{
			//browse through 140 & 141
			def sql = new Sql(dataSource)
			sql.eachRow(qryString(), {
//				info ("it: " + it);
												
				def rec = new iReqRecord()
				rec.setDstrctCode(it.dstrct_code)
				rec.setiReqNo(it.ireq_no)
				rec.setAuthsdBy(it.authsd_by)
				rec.setRequestedBy(it.requested_by)
				rec.setCreationDate(it.creation_date)
				result.add(rec)
//				info ("IREQ rec: " +
//					"dstrct: " + rec.getDstrctCode() + " " +
//					"preq no: " +  rec.getiReqNo() +  " " +
//					"requested by: " + rec.getRequestedBy() + " " +
//					"authsd by: " + rec.getAuthsdBy() + " " +
//					"creation date: " + rec.getCreationDate());
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb230.printinfo("getIReqRecords failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb230.printinfo ("getIReqRecords Error: " + ex.message);
		}
		return result;
	}

	// Create SQL statement
	private String qryString() {
		info("setQryString")
		StringBuffer qry = new StringBuffer()
		qry.append("select ")
		qry.append("a.dstrct_code, ")
		qry.append("a.ireq_no, ")
		qry.append("a.requested_by, ")
		qry.append("a.authsd_by, ")
		qry.append("a.creation_date ")
		qry.append("from msf140 a ")
		qry.append("where a.ireq_no not in ")
		qry.append("(select b.ireq_no from msf141 b )")
		qry.append("order by ")
		qry.append("a.dstrct_code, a.preq_no")
		info(qry.toString())
		return qry.toString()
	}

	/**
	 * list of iReq records.
	 */
	public class iReqRecord
	{
		String dstrctCode = "";
		String iReqNo = "";
		String requestedBy = "";
		String authsdBy = "";
		String creationDate = "";
	}

	/**
	 * Get employee's name. <br>
	 **/
	private String getEmpName (String empId)
	{
		//ProcessTrb8al.printinfo ("getEmpName");
		String result = " ";
		
		try
		{
			Constraint c1 = MSF810Key.employeeId.equalTo(empId);

			def query = new QueryImpl(MSF810Rec.class).and(c1);
			MSF810Rec msf810Rec = (MSF810Rec) edoi.firstRow(query);
			if  (msf810Rec)
			{
				result = msf810Rec.getFirstName().trim() + " " + msf810Rec.getSurname().trim();
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb8al.printinfo("getEmpName failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb8al.printinfo ("getEmpName Error: " + ex.message);
		}
		return result;
	}
	
	/**
	 * delete 140. <br>
	 **/
	private void deleteIREQ (String dstrct, String iReq)
	{
		//info ("process deleteIREQ");
		
		try
		{	
			//prepare for delete
			MSF140Rec msf140Rec = edoi.findByPrimaryKey(new MSF140Key(dstrct,iReq))
//			info (" ")
//			info ("before delete")
//			info ("msf140 rec: " + msf140Rec.getPrimaryKey())
			
			edoi.delete(msf140Rec.getPrimaryKey());
//			info ("*** deleted MSF140 ***");
//			info (" ");
			
			//FOR TESTING ONLY
//			if (dstrct.equals("GRID") && iReq.equals("B00780")){
//				edoi.delete(msf140Rec.getPrimaryKey());
//				info ("*** deleted MSF140 ***");
//				info (" ");
//			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb140.printinfo("deleteIREQ failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb140.printinfo ("deleteIREQ Error: " + ex.message);
		}
	}

	
	/**
	 * report file. <br>
	 * <li> define getter and setter for the both report A and B </li>
	 **/
	private class Trb140aReportLine implements Comparable<Trb140aReportLine>
	{
		public String dstrctCode;
		public String iReqNo;
		public String reqById;
		public String reqByName;
		public String authById;
		public String authByName;
		public String creationDate;
		
		public Trb140aReportLine()
		{
		}
		
		//report is sorted via po no
		public int compareTo(Trb140aReportLine otherReportLine)
		{
			if (!dstrctCode.equals(otherReportLine.dstrctCode))
			{
				return dstrctCode.compareTo(otherReportLine.dstrctCode)
			}
			if (!iReqNo.equals(otherReportLine.iReqNo))
			{
				return iReqNo.compareTo(otherReportLine.iReqNo)
			}
			return 0;
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
	
	/**
	 * generate batch report <br>
	 * <li> 1. exception report Trb140A </li>
	 **/
	private generateTrb140Report()
	{
		ProcessTrb140.printinfo ("Process generateTrb140Report");
		String statusD = "";
		
		try
		{
			Trb140aReportLine currLine;
			//String APP = "\"";
			String tempString;
			String prevDstrct;
			int idx;
			
			Collections.sort(arrayOfTrb140ReportLine);
			
			prevDstrct = " ";
			//info ("report size: " + arrayOfTrb140ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb140ReportLine.size())
			{
				currLine = arrayOfTrb140ReportLine.get(idx);
				Date convDate =  new Date().parse("yyyyMMdd", currLine.creationDate)
				String dispDate = convDate.format("dd/MM/yyyy");
				
				tempString = " ";
				if (currLine.dstrctCode.equals(prevDstrct)){
					tempString = " ";
					tempString = tempString + (currLine.dstrctCode).padRight(9);
					tempString = tempString + (currLine.iReqNo).padRight(9);
					tempString = tempString + (currLine.reqById + " " + currLine.reqByName).padRight(42);
					tempString = tempString + (currLine.authById + " " + currLine.authByName).padRight(39);
					tempString = tempString + dispDate;
					
					DoReportA(tempString);
					recordDstrctCount++;
				}
				else{
					//do district footer
					if (!prevDstrct.trim().equals("")){
						tempString = " ";
						tempString = tempString + (">> Records to be deleted from " + prevDstrct + " : ").padRight(45);
						tempString = tempString + (recordDstrctCount);
						DoReportA(tempString);
						//add a blank line
						DoReportA(" ");
					}
					
					tempString = " ";
					tempString = tempString + (currLine.dstrctCode).padRight(9);
					tempString = tempString + (currLine.iReqNo).padRight(9);
					tempString = tempString + (currLine.reqById + " " + currLine.reqByName).padRight(42);
					tempString = tempString + (currLine.authById + " " + currLine.authByName).padRight(39);
					tempString = tempString + dispDate;
					
					DoReportA(tempString);
					
					prevDstrct = currLine.dstrctCode;
					recordDstrctCount = 1;					
				}
				
				idx++;
			}

			//last line
			//do district footer
			if (!prevDstrct.trim().equals("")){
				DoReportA(" ");
				DoReportA(" ");
				tempString = " ";
				tempString = tempString + (">> Records to be deleted from " + prevDstrct + " : ").padRight(45);
				tempString = tempString + (recordDstrctCount);
				DoReportA(tempString);
				//add a blank line
				DoReportA(" ");
			}
			
			if (arrayOfTrb140ReportLine.size() <= 0){
				//write records count
				ReportA.writeLine(132,"-");
				DoReportA(" ");
				DoReportA(" ");
				
				tempString = " ";
				tempString = tempString + ">> No record qualifies for deletion << ".padLeft(54);
				DoReportA(tempString);
			}
			
			DoReportA(" ");

		}
		catch (Exception ex)
		{
			ProcessTrb140.printinfo ("generateTrb140Report Error: " + ex.message);
		}
	}

	/**
	 * write batch report header <br>
	 **/
	private void writeReportHeaderA()
	{
		//info("writeReportHeaderA");
		String tempString;
		String sMode = "";
		
		if (batchParams.paramRunType.equals("U")){
			sMode = "UPDATE Mode"
		}
		else {
			sMode = "REPORT Mode"
		}
		
		ReportA.writeLine(132,"-");
		DoReportA(StringUtils.center("Issue Requisitions With No Items - " + sMode,132));

		ReportA.writeLine(132,"-");
		tempString = " District IReq No.    Requested by                           Authorised by                          Creation Date";
		DoReportA(tempString);
		
		ReportA.writeLine(132,"-");
	}

	private void DoReportA(String line)
	{
		//info("Report A: " + line);
		ReportA.write(line);
	}
	
	/**
	 * print batch report <br>
	 * <li> output total record counts </li>
	 **/
	private void printBatchReport(String rep)
	{

		ProcessTrb140.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb140 process = new ProcessTrb140();
process.runBatch(binding);
