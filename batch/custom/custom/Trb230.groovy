/**
 * @AIT 2013
 * Conversion from Trb230.cbl for Ellipse 8 upgrade project
 * 26/06/2013 RL - VERSION 1
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
import com.mincom.ellipse.edoi.ejb.msf230.*;
import com.mincom.ellipse.edoi.ejb.msf231.*;
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
import java.awt.Font;
import java.text.SimpleDateFormat;
import javax.sql.DataSource;


////-------------------------------------------------------------------
//import groovy.lang.Binding;
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;
//
//public class AITBatch230 implements GroovyInterceptable {
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

public class ParamsTrb230
{
	//List of Input Parameters
	String paramRunType;
	String paramFromDate;
	String paramToDate;
	String paramOldAuthorisedBy;
	String paramNewAuthorisedBy;
}

/**
 * Update authorized user <br>
 * <li> This program is just a one-off to update file MSF230.</li>
 * <li> It replaces authsd-by field from one authorizing officer to another. </li>
 * <li> The development of this program was brought by a situation wherein requisitions </li>
 * <li> authorized by a retired staff need to be action by another active staff.</li>
 * <li> It can be run under 2 modes: report or update mode </li>
 * <li> output: 1 report - Trb230A. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrb230 extends AITBatch230
public class ProcessTrb230 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrb230 batchParams;
	
	private ArrayList <preqRecord> listOfPreqs = new ArrayList <preqRecord>();
	private ArrayList arrayOfTrb230ReportLine = new ArrayList();
	private DataSource dataSource;
		
//	private BigDecimal recordCount230;
//	private BigDecimal recordCount231;
//	private BigDecimal recordCount230Excpt;
//	private BigDecimal recordCount230Write;
	
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
		ProcessTrb230.printinfo("VERSION      :" + version);
		ProcessTrb230.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb230.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb230());

		//PrintRequest Parameters
		ProcessTrb230.printinfo("paramRunType           : " + batchParams.paramRunType);
		ProcessTrb230.printinfo("paramFromDate          : " + batchParams.paramFromDate);
		ProcessTrb230.printinfo("paramToDate            : " + batchParams.paramToDate);
		ProcessTrb230.printinfo("paramPrevAuthorisedBy  : " + batchParams.paramOldAuthorisedBy);
		ProcessTrb230.printinfo("paramNewAuthorisedBy   : " + batchParams.paramNewAuthorisedBy);
		ProcessTrb230.printinfo(" ");

		try
		{
			processBatch();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb230.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb230);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb230.printinfo("processBatch");

		if (initialise()){
			processRequest();
			generateTrb230Report();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrb230.printinfo("initialise");
		
//		recordCount230 = 0;
//		recordCount231 = 0;
//		recordCount230Write = 0;
//		recordCount230Excpt = 0;
		
		ReportA = report.open('Trb230A')
		writeReportHeaderA();
		
		//validate mandatory fields
		if (batchParams.paramRunType.trim().equals(""))
		{
			WriteError ("Run Type is Mandatory");
			return false;
		}		
		if (!batchParams.paramRunType.trim().equals("R") && !batchParams.paramRunType.trim().equals("U"))
		{
			WriteError ("Input must be R or U");
			return false;
		}
		if (batchParams.paramOldAuthorisedBy.trim().equals(""))
		{
			WriteError ("Previous Authorised By is Mandatory");
			return false;
		}
		if (batchParams.paramNewAuthorisedBy.trim().equals(""))
		{
			WriteError ("New Authorised By is Mandatory");
			return false;
		}
		if (batchParams.paramFromDate.trim().equals(""))
		{
			WriteError ("Authorised Date From is Mandatory");
			return false;
		}
		if (batchParams.paramToDate.trim().equals(""))
		{
			WriteError ("Authorised Date To is Mandatory");
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
		ProcessTrb230.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb230.printinfo("processRequest");
		Boolean bUpdated;
		String prevDstrct = " ";
		String prevPreqNo = " ";
		
		
		try
		{	
			listOfPreqs = getPreqRecords();

			//process records
			if (listOfPreqs.size() > 0){
				listOfPreqs.each {preqRecord rec->
					//skip duplicate preq no
					if (!rec.getDstrctCode().equals(prevDstrct) || !rec.getPreqNo().equals(prevPreqNo))
					{					
						//update authsd-by for 'S' type
						if (batchParams.paramRunType.equals("U") && rec.getReqType().equals("S")){
							bUpdated = updateMSF230(rec); 
						}
						//write to array
						def line = new Trb230aReportLine();
						line.dstrctCode = rec.getDstrctCode();
						line.preqNo = rec.getPreqNo();
						line.authsdStatus = rec.getAuthsdStatus();
						line.authsdDate = rec.getAuthsdDate();
						line.oldAuthsdBy = rec.getAuthsdBy();
						if (rec.getReqType().equals("S")){
							line.newAuthsdBy = batchParams.paramNewAuthorisedBy;
							line.reqTypeG = " ";
						}
						else {
							line.newAuthsdBy = rec.getAuthsdBy();
							line.reqTypeG = "Y";
						}
						if (bUpdated){
							line.reqUpdated = "Y";
						}
						else {
							line.reqUpdated = " "
						}
						arrayOfTrb230ReportLine.add(line);
						
						prevDstrct = rec.getDstrctCode();
						prevPreqNo = rec.getPreqNo();
					}
				}	
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb230.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb230.printinfo ("processRequest Error: " + ex.message);
		}		
	}
	
	/**
	 * Browse the Preq and Preq item records.
	 */
	private ArrayList<preqRecord> getPreqRecords() {
		ProcessTrb230.printinfo("getPreqRecords");
		
		List <preqRecord> result = new ArrayList <preqRecord>()
		
		try
		{
			//browse through 230 & 231
			def sql = new Sql(dataSource)
			sql.eachRow(qryString(), {
//				info ("it: " + it);
												
				def rec = new preqRecord()
				rec.setDstrctCode(it.dstrct_code)
				rec.setPreqNo(it.preq_no)
				rec.setAuthsdBy(it.authsd_by)
				rec.setAuthsdDate(it.authsd_date)
				rec.setAuthsdStatus(it.authsd_status)
				rec.setReqType(it.req_type)
				result.add(rec)
//				info ("PREQ rec: " +
//					"dstrct: " + rec.getDstrctCode() + " " +
//					"preq no: " +  rec.getPreqNo() +  " " +
//					"authsd by: " + rec.getAuthsdBy() + " " +
//					"authsd date: " + rec.getAuthsdDate() + " " +
//					"authsd status: " + rec.getAuthsdStatus() + " " +
//					"req type: " + rec.getReqType());				
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb230.printinfo("getPreqRecords failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb230.printinfo ("getPreqRecords Error: " + ex.message);
		}
		return result;
	}
	
	// Create SQL statement
	private String qryString() {
		info("setQryString")
		StringBuffer qry = new StringBuffer()
		qry.append("select ")
		qry.append("a.dstrct_code, ")
		qry.append("a.preq_no, ")
		qry.append("a.authsd_by, ")
		qry.append("a.authsd_date, ")
		qry.append("a.authsd_status, ")
		qry.append("b.req_type ")
		qry.append("from msf230 a ")
		qry.append("inner join msf231 b ")
		qry.append("on a.preq_no = b.preq_no ")
		qry.append("and a.dstrct_code = b.dstrct_code ")
		qry.append("where a.authsd_status = 'A' ")
		qry.append("and a.authsd_by = '${batchParams.paramOldAuthorisedBy}' ")
		qry.append("and a.authsd_date >= '${batchParams.paramFromDate}' ")
		qry.append("and a.authsd_date <= '${batchParams.paramToDate}' ")
		qry.append("and a.no_of_items <> a.complete_items ")
		qry.append("and a.completed_date = ' ' ")
		qry.append("and b.status_231 in ('0', '1', '2', '3') ")
		qry.append("order by ")
		qry.append("a.dstrct_code, a.preq_no, a.authsd_status, a.authsd_date, b.req_type ")
		info(qry.toString())
		return qry.toString()
	}

	/**
	 * list of PO header and item records.
	 */
	public class preqRecord
	{
		String dstrctCode = "";
		String preqNo = "";
		String authsdBy = "";
		String authsdDate = "";
		String authsdStatus = "";
		String reqType = "";
	}

	/**
	 * update authsd-by in MSF230. <br>
	 **/
	private Boolean updateMSF230 (preqRecord preqRec)
	{
		//info ("process updateMSF230");
		Boolean result = false;
		
		try
		{	MSF230Key msf230Key = new MSF230Key();
			msf230Key.setDstrctCode(preqRec.getDstrctCode());
			msf230Key.setPreqNo(preqRec.getPreqNo());
			MSF230Rec msf230Rec = edoi.findByPrimaryKey(msf230Key)
//			info (" ")
//			info ("before update")
//			info ("msf230 rec: " + msf230Rec.getPrimaryKey() + " - " + msf230Rec.getAuthsdBy())
			
			msf230Rec.setAuthsdBy(batchParams.paramNewAuthorisedBy);			
//			info ("ready for update")
//			info ("msf230 authsd by: " + msf230Rec.getAuthsdBy())
			
			edoi.update(msf230Rec);			
			result = true;
			
			//*** FOR TESTING ONLY		
//			if (preqRec.getPreqNo().equals("329444") && preqRec.getDstrctCode().equals("GRID")){
//				edoi.update(msf230Rec);
//				result = true;
//				info ("*** update msf230 record - PREQ no: " + preqRec.getDstrctCode() + "/" + preqRec.getPreqNo())
//				info ("************************************")
//			}
//			info ("result: " + result);
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb230.printinfo("updateMSF230 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb230.printinfo ("updateMSF230 Error: " + ex.message);
		}
		return result;
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
	 * report file. <br>
	 * <li> define getter and setter for the both report A and B </li>
	 **/
	private class Trb230aReportLine implements Comparable<Trb230aReportLine>
	{
		public String dstrctCode;
		public String preqNo;
		public String authsdStatus;
		public String authsdDate;
		public String oldAuthsdBy;
		public String newAuthsdBy;
		public String reqTypeG;
		public String reqUpdated;
		
		public Trb230aReportLine()
		{
		}
		
		//report is sorted via dstrct code & preq no
		public int compareTo(Trb230aReportLine otherReportLine)
		{
			if (!dstrctCode.equals(otherReportLine.dstrctCode))
			{
				return dstrctCode.compareTo(otherReportLine.dstrctCode)
			}
			if (!preqNo.equals(otherReportLine.preqNo))
			{
				return preqNo.compareTo(otherReportLine.preqNo)
			}
			if (!authsdStatus.equals(otherReportLine.authsdStatus))
			{
				return authsdStatus.compareTo(otherReportLine.authsdStatus)
			}
			if (!authsdDate.equals(otherReportLine.authsdDate))
			{
				return authsdDate.compareTo(otherReportLine.authsdDate)
			}
			return 0;
		}				
	}
	
	/**
	 * generate batch report <br>
	 * <li> 1. exception report Trb230A </li>
	 **/
	private generateTrb230Report()
	{
		ProcessTrb230.printinfo ("Process generateTrb230Report");
		try
		{
			Trb230aReportLine currLine;
			//String APP = "\"";
			String tempString;
			int idx;
			
			Collections.sort(arrayOfTrb230ReportLine);
			
			//info ("report size: " + arrayOfTrb230ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb230ReportLine.size())
			{
				currLine = arrayOfTrb230ReportLine.get(idx);
				Date convDate =  new Date().parse("yyyyMMdd", currLine.authsdDate)
				String dispDate = convDate.format("dd/MM/yyyy");
				
				tempString = " ";
				tempString = tempString + (currLine.dstrctCode).padRight(8);
				tempString = tempString + (currLine.preqNo).padRight(12);
				tempString = tempString + (currLine.authsdStatus).padRight(17);
				tempString = tempString + dispDate.padRight(14);
				tempString = tempString + (currLine.oldAuthsdBy).padRight(19);
				tempString = tempString + (currLine.newAuthsdBy).padRight(22);
				tempString = tempString + (currLine.reqTypeG).padRight(15);
				tempString = tempString + (currLine.reqUpdated).padRight(12);

				//info ("tempString: " + tempString);
				
				DoReportA(tempString);
				
				idx++;
			}
			
			//add a blank line
			DoReportA(" ");			
		}
		catch (Exception ex)
		{
			ProcessTrb230.printinfo ("generateTrb230Report Error: " + ex.message);
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
			sMode = " UPDATE Mode"
		}
		else {
			sMode = " REPORT Mode"
		}
		
		ReportA.writeLine(132,"-");		
		DoReportA(StringUtils.center("MSF230: Authsd-by Field Change - " + sMode,132));

		ReportA.writeLine(132,"-");
		tempString = " Dstrct  Preq No     Authsd Status    Authsd Date   Prev Authsd By     New Authsd By     G Req Type    Rec Updated";
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

		ProcessTrb230.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb230 process = new ProcessTrb230();
process.runBatch(binding);
