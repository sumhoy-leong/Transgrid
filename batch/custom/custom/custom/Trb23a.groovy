/**
 * @AIT 2013
 * Conversion from Trb23a.cbl for Ellipse 8 upgrade project
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
//public class AITBatch23a implements GroovyInterceptable {
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

public class ParamsTrb23a
{
	//List of Input Parameters
	String paramPreqNo;
	String paramPreqItem;
	String paramStatus;
	String paramRunType;
}

/**
 * Archive complete/uncomplete purchase requisitions that can't be archived online <br>
 * <li> This program can be run in both report and update modes,</li>
 * <li> to uncomplete/complete purchase requisition items that </li>
 * <li> cannot be achieved online. </li>
 * <li> output: 1 report - TRB23AA. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrb23a extends AITBatch23a
public class ProcessTrb23a extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrb23a batchParams;

	public static final String WX_DISTRICT = "district";
	private String wxDstrct;
	
	private ArrayList arrayOfTrb23aReportLine = new ArrayList();
		
	private BigDecimal recordCount231Excpt;
	private BigDecimal recordCount231Write;
	
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
		ProcessTrb23a.printinfo("VERSION      :" + version);
		ProcessTrb23a.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb23a.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb23a());

		//PrintRequest Parameters
		ProcessTrb23a.printinfo("paramPreqNo            : " + batchParams.paramPreqNo);
		ProcessTrb23a.printinfo("paramPreqItem          : " + batchParams.paramPreqItem);
		ProcessTrb23a.printinfo("paramStatus            : " + batchParams.paramStatus);
		ProcessTrb23a.printinfo("paramRunType           : " + batchParams.paramRunType);
		ProcessTrb23a.printinfo(" ");

		try
		{
			processBatch();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb23a.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport();
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb23a);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb23a.printinfo("processBatch");

		if (initialise()){
			processRequest();
			generateTrb23aReport();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrb23a.printinfo("initialise");
		
		recordCount231Write = 0;
		recordCount231Excpt = 0;
		
		ReportA = report.open('TRB23AA')
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
		if (batchParams.paramPreqNo.trim().equals(""))
		{
			WriteError ("Purchase Requisition is Mandatory");
			return false;
		}
		if (batchParams.paramPreqItem.trim().equals(""))
		{
			WriteError ("Purchase Requisition item is Mandatory");
			return false;
		}
		if (batchParams.paramStatus.trim().equals(""))
		{
			WriteError ("Status is Mandatory");
			return false;
		}
		if (!batchParams.paramStatus.equals("0") && !batchParams.paramStatus.equals("1") &&
			!batchParams.paramStatus.equals("2") && !batchParams.paramStatus.equals("3") &&
			!batchParams.paramStatus.equals("4") && !batchParams.paramStatus.equals("9"))
		{
			WriteError ("Status must be 0, 1, 2, 3, 4 or 9");
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
		ProcessTrb23a.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb23a.printinfo("processRequest");
		Boolean bUpdated;
		
		
		try
		{	
			wxDstrct = commarea.getProperty(WX_DISTRICT);
			//info ("wxDstrct:" + wxDstrct);
			//info ("preqNo:" + batchParams.paramPreqNo);
			//info ("preqItem:" + batchParams.paramPreqItem);
			
			Constraint c1 = MSF231Key.dstrctCode.equalTo(wxDstrct);
			Constraint c2 = MSF231Key.preqNo.equalTo(batchParams.paramPreqNo);
			Constraint c3 = MSF231Key.preqItemNo.equalTo(batchParams.paramPreqItem);
			
			def query = new QueryImpl(MSF231Rec.class).and(c1).and(c2).and(c3)
			MSF231Rec msf231Rec = (MSF231Rec) edoi.firstRow(query);
			
			//process records
			if (msf231Rec)
			{			
				//info ("msf231Rec: " + msf231Rec.getPrimaryKey());
				//info ("status_231: " + msf231Rec.getStatus_231());
				//validate status
				if (msf231Rec.getStatus_231().equals("0") || msf231Rec.getStatus_231().equals("1") ||
					msf231Rec.getStatus_231().equals("2") || msf231Rec.getStatus_231().equals("3") ||
					msf231Rec.getStatus_231().equals("4") || msf231Rec.getStatus_231().equals("9")){
					if (batchParams.paramRunType.equals("U")){
						bUpdated = updateMSF231(msf231Rec.getPrimaryKey()); 
					}
				}							
				//write to array
				def line = new Trb23aaReportLine();
				line.dstrctCode = msf231Rec.getPrimaryKey().getDstrctCode();
				line.preqNo = msf231Rec.getPrimaryKey().getPreqNo();
				line.preqItem = msf231Rec.getPrimaryKey().getPreqItemNo();
				line.status = msf231Rec.getStatus_231();
				line.validPreq = "Y";
				if (bUpdated){
					line.updated = "Y";
				}
				else
				{
					line.updated = " ";
				}

				arrayOfTrb23aReportLine.add(line);				
				//info ("added line 1: " + arrayOfTrb23aReportLine.size());
			}
			else{
				//info ("write invalid record")
				def line = new Trb23aaReportLine();
				line.dstrctCode = wxDstrct;
				line.preqNo = batchParams.paramPreqNo;
				line.preqItem = batchParams.paramPreqItem;
				line.status = " ";
				line.validPreq = "N";
				line.updated = " ";

				arrayOfTrb23aReportLine.add(line);
				//info ("added line 2: " + arrayOfTrb23aReportLine.size());
			}

		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb23a.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb23a.printinfo ("processRequest Error: " + ex.message);
		}		
	}

	/**
	 * update status_231 in MSF231. <br>
	 **/
	private Boolean updateMSF231 (MSF231Key key231)
	{
		//info ("process updateMSF231");
		Boolean result = false;
		
		try
		{	MSF231Key msf231Key = new MSF231Key();
			msf231Key.setDstrctCode(key231.getDstrctCode());
			msf231Key.setPreqNo(key231.getPreqNo());
			msf231Key.setPreqItemNo(key231.getPreqItemNo());
			MSF231Rec msf231Rec = edoi.findByPrimaryKey(msf231Key)
//			info (" ")
//			info ("before update")
//			info ("msf231 rec: " + msf231Rec.getPrimaryKey() + " - " + msf231Rec.getStatus_231())
			
			msf231Rec.setStatus_231(batchParams.paramStatus);			
//			info ("ready for update")
//			info ("msf231 status: " + msf231Rec.getStatus_231())
			
			edoi.update(msf231Rec);			
			result = true;
//			info ("*** updated ***")
			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb23a.printinfo("updateMSF231 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb23a.printinfo ("updateMSF231 Error: " + ex.message);
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
	private class Trb23aaReportLine implements Comparable<Trb23aaReportLine>
	{
		public String dstrctCode;
		public String preqNo;
		public String preqItem;
		public String status;
		public String validPreq;
		public String updated;

		
		public Trb23aaReportLine()
		{
		}
		
		//report is sorted via dstrct code & preq no
		public int compareTo(Trb23aaReportLine otherReportLine)
		{
			if (!dstrctCode.equals(otherReportLine.dstrctCode))
			{
				return dstrctCode.compareTo(otherReportLine.dstrctCode)
			}
			if (!preqNo.equals(otherReportLine.preqNo))
			{
				return preqNo.compareTo(otherReportLine.preqNo)
			}
			if (!preqItem.equals(otherReportLine.preqItem))
			{
				return preqItem.compareTo(otherReportLine.preqItem)
			}
			return 0;
		}				
	}
	
	/**
	 * generate batch report <br>
	 * <li> 1. exception report Trb23aA </li>
	 **/
	private generateTrb23aReport()
	{
		ProcessTrb23a.printinfo ("Process generateTrb23aReport");
		
		try
		{
			Trb23aaReportLine currLine;
			//String APP = "\"";
			String tempString;

			String sDesc1 = " ";
			String sDesc2 = " ";
			int idx;
			
			Collections.sort(arrayOfTrb23aReportLine);
			
			//info ("report size: " + arrayOfTrb23aReportLine.size());
			
			//output to report line
			idx = 0;
			while(idx < arrayOfTrb23aReportLine.size())
			{
				currLine = arrayOfTrb23aReportLine.get(idx);
				
				tempString = " ";
				tempString = tempString + (currLine.dstrctCode).padRight(8);
				tempString = tempString + (currLine.preqNo).padRight(13);
				tempString = tempString + (currLine.preqItem).padRight(13);

				if (!currLine.status.trim().equals("")){
					sDesc1 = statusDesc(currLine.status);
					tempString = tempString + sDesc1.padRight(28);

					sDesc2 = statusDesc(batchParams.paramStatus);
					tempString = tempString + sDesc2.padRight(27);
					
					if (!currLine.status.equals("0") && !currLine.status.equals("1") &&
						!currLine.status.equals("2") && !currLine.status.equals("3") &&
						!currLine.status.equals("4") && !currLine.status.equals("9")){
						tempString = tempString + ("*** Invalid Status ***");
						recordCount231Excpt++;
					}
					else {
						if (batchParams.paramRunType.equals("U") && currLine.updated.equals("Y")){
							tempString = tempString + ("Updated");
							recordCount231Write++;
						}
					}
				}
				else
				{
					sDesc1 = "                            ";
					tempString = tempString + sDesc1;
					
					sDesc2 = statusDesc(batchParams.paramStatus);
					tempString = tempString + sDesc2.padRight(27);
					
					tempString = tempString + ("Rejected - Purch Req Item No. does not exist");
					recordCount231Excpt++;
				}
				
				//info ("tempString 1: " + tempString);				
				DoReportA(tempString);
				idx++;
			}
			//write total records
			DoReportA(" ");
			tempString = " ";
			tempString = tempString + "No of 231 records exception   : ".padLeft(54);
			tempString = tempString + recordCount231Excpt.toString();
			DoReportA(tempString);
			
			tempString = " ";
			tempString = tempString + "No of 231 records updated     : ".padLeft(54);
			tempString = tempString + recordCount231Write.toString();
			DoReportA(tempString);

			DoReportA(" ");
			//add a blank line
			DoReportA(" ");			
		}
		catch (Exception ex)
		{
			ProcessTrb23a.printinfo ("generateTrb23aReport Error: " + ex.message);
		}		
	}

	/**
	 * status description <br>
	 **/
	private String statusDesc(String sStatus)
	{
		String result = " ";
		
		switch (sStatus) {
			case '0':
				result = "Not ordered             ";
				break;
			case '1':
				result = "Pro forma ordered       ";
				break;
			case '2':
				result = "Ordered but not received";
				break;
			case '3':
				result = "Partially received      ";
				break;
			case '4':
				result = "Fully received          ";
				break;
			case '9':
				result = "Complete                ";
				break;
			default:
				result = "*Unknown*               ";
				break;
		}
		return result;
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
		DoReportA(StringUtils.center("Change status of Purchase Requisition Items On-line - " + sMode,132));

		ReportA.writeLine(132,"-");
		tempString = " Dstrct  Preq No      Preq Item    Original Status             New Status                 Remarks";
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
	private void printBatchReport()
	{

		ProcessTrb23a.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb23a process = new ProcessTrb23a();
process.runBatch(binding);
