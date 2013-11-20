/**
 * @AIT 2013
 * Conversion from Trb231.cbl for Ellipse 8 upgrade project
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
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf230.*;
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


////-------------------------------------------------------------------
//import groovy.lang.Binding;
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;
//
//public class AITBatch231 implements GroovyInterceptable {
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

public class ParamsTrb231
{
	//List of Input Parameters
	String paramPreqNo;
	String paramNewRequestedBy;
	String paramNewAuthorisedBy;
}

/**
 * Update authorized and requested by user in MSF230<br>
 * <li> This program is a clone of TRB230.</li>
 * <li> It updates the requested officer or authorised officer for a purchase requisition </li>
 * <li> that is not at a status of complete. </li>
 * <li> There are a number of Purchase Requisitions that were converted as a </li>
 * <li> result of District Consolidation (June2005)-all requisitions were </li>
 * <li> consolidated to GRID District. When this project was undertaken there</li>
 * <li> was a question that the users were required to enter-"HAVE YOU CHANGED THE WAREHOUESE" </li>
 * <li> This question was removed during the current Ellipse upgrade (May 2006) </li>
 * <li> as it was no longer required. However, this has resulted in the requested  </li>
 * <li> **authorised by fields for some converted orders not able to be amended. </li>
 * <li> Up till now work requests have been entered to enable an SQL to be run to make changes to the </li>
 * <li> authorised *requested by officers. As this is occurring regularly it is </li>
 * <li> recommended that a batch report be created to fulfill the same functionality.</li>
 * 
 * <li> Note: That the leading zeros for the employees is required to be </li>
 * <li> entered as no leading zeros causes problems with running APQWFT (Approvals Manager) </li>
 * <li> output: 1 report - Trb231A. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrb231 extends AITBatch231
public class ProcessTrb231 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrb231 batchParams;

	public static final String WX_DISTRICT = "district";
	private String wxDstrct;
	
	private ArrayList arrayOfTrb231ReportLine = new ArrayList();
	
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
		ProcessTrb231.printinfo("VERSION      :" + version);
		ProcessTrb231.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb231.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb231());

		//PrintRequest Parameters
		ProcessTrb231.printinfo("paramPreqNo             : " + batchParams.paramPreqNo);
		ProcessTrb231.printinfo("paramNewRequestedBy     : " + batchParams.paramNewRequestedBy);
		ProcessTrb231.printinfo("paramNewAuthorisedBy    : " + batchParams.paramNewAuthorisedBy);
		ProcessTrb231.printinfo(" ");

		try
		{
			processBatch();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb231.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb231);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb231.printinfo("processBatch");

		if (initialise()){
			processRequest();
			generateTrb231Report();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrb231.printinfo("initialise");
		
		ReportA = report.open('Trb231A')
		writeReportHeaderA();
		
		//validate mandatory fields		
		if (batchParams.paramPreqNo.trim().equals(""))
		{
			WriteError ("Purchase Requisition number is Mandatory");
			return false;
		}
		if (batchParams.paramNewAuthorisedBy.trim().equals("") && batchParams.paramPreqNo.trim().equals(""))
		{
			WriteError ("Neither Requestor nor Authoriser Specified");
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
		ProcessTrb231.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb231.printinfo("processRequest");
		Boolean bUpdated;
		String requestedByStatus = " ";
		String authorisedByStatus = " ";
		Boolean updatedOK = false;
		
		
		try
		{	
			wxDstrct = commarea.getProperty(WX_DISTRICT);
			//info ("wxDstrct:" + wxDstrct);
			
			Constraint c1 = MSF230Key.dstrctCode.equalTo(wxDstrct);
			Constraint c2 = MSF230Key.preqNo.equalTo(batchParams.paramPreqNo);
			
			def query = new QueryImpl(MSF230Rec.class).and(c1).and(c2)

			edoi.search(query,10000,{MSF230Rec msf230Rec ->
				//process records
				if (msf230Rec){
//					info ("primary key: " + msf230Rec.getPrimaryKey());
//					info ("auth status: " + msf230Rec.getAuthsdStatus());
//					info ("no of items: " + msf230Rec.getNoOfItems());
//					info ("complete items: " + msf230Rec.getCompleteItems());
//					info ("complete date: " + msf230Rec.getCompletedDate());
					
					if (msf230Rec.getAuthsdStatus().equals("A") && 
						msf230Rec.getNoOfItems() != msf230Rec.getCompleteItems() &&
						msf230Rec.getCompletedDate().trim().equals("")){
						if (!batchParams.paramNewRequestedBy.trim().equals("")){
							requestedByStatus = processMSF760(batchParams.paramNewRequestedBy);
							if (!batchParams.paramNewAuthorisedBy.trim().equals("")) {
								authorisedByStatus = processMSF760(batchParams.paramNewAuthorisedBy);
								if (requestedByStatus.equals("A") && authorisedByStatus.equals("A")){
									updatedOK = updateMSF230 ("AR");
								}
							}
							else{
								if (requestedByStatus.equals("A")){
									updatedOK = updateMSF230 ("R");
								}
							}
						}
						else {
							if (!batchParams.paramNewAuthorisedBy.trim().equals("")) {
								authorisedByStatus = processMSF760(batchParams.paramNewAuthorisedBy);
								if (authorisedByStatus.equals("A")){
									updatedOK = updateMSF230 ("A");
								}
							}
						}
					}
				}
			
				//write to array
				def line = new Trb231aReportLine();
				line.preqNo = msf230Rec.getPrimaryKey().getPreqNo();
				if (!batchParams.paramNewAuthorisedBy.trim().equals("")){
					line.oldAuthsdBy = msf230Rec.getAuthsdBy();
					line.newAuthsdBy = batchParams.paramNewAuthorisedBy;
				}
				if (!batchParams.paramNewRequestedBy.trim().equals("")){
					line.oldRequestedBy = msf230Rec.getRequestedBy();
					line.newRequestedBy = batchParams.paramNewRequestedBy;
				}
				line.authStatus = msf230Rec.getAuthsdStatus();				
				if (msf230Rec.getNoOfItems() == msf230Rec.getCompleteItems() &&
					!msf230Rec.getCompletedDate().trim().equals("")){
					line.compltStatus = "C"
				}
				else{
					line.compltStatus = " "
				}
				line.authByStatus = authorisedByStatus;
				line.reqByStatus = requestedByStatus;
				if (updatedOK){
					line.updated = "Y";
				}
				arrayOfTrb231ReportLine.add(line);
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb231.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb231.printinfo ("processRequest Error: " + ex.message);
		}		
	}

	/**
	 * validate employee ID. <br>
	 **/
	private String processMSF760 (String empId)
	{
		//info ("process MSF760");
		String result;
		
		try
		{	
			result = " ";
			//info ("empId: " + empId)
			MSF760Rec msf760Rec = edoi.findByPrimaryKey(new MSF760Key(empId))
			result = msf760Rec.getEmpStatus();
			//info ("emp status: " + result);			
		}	
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb231.printinfo("processMSF760 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb231.printinfo ("processMSF760 Error: " + ex.message);
		}
		return result;
	}
	
	/**
	 * update authsd by and/or requested by in MSF230. <br>
	 **/
	private Boolean updateMSF230 (String sType)
	{
		//info ("process updateMSF230");
		Boolean result = false;
		
		try
		{	MSF230Key msf230Key = new MSF230Key();
			msf230Key.setDstrctCode(wxDstrct);
			msf230Key.setPreqNo(batchParams.paramPreqNo);
			MSF230Rec msf230Rec = edoi.findByPrimaryKey(msf230Key)
//			info (" ");
//			info ("before update");
//			info ("msf230 rec: " + msf230Rec.getPrimaryKey() + " - " + msf230Rec.getAuthsdBy() + " - " + msf230Rec.getRequestedBy());
			
			if (sType.equals("AR")){
				msf230Rec.setAuthsdBy(batchParams.paramNewAuthorisedBy);	
				msf230Rec.setRequestedBy(batchParams.paramNewRequestedBy);
			}
			if (sType.equals("R")){
				msf230Rec.setRequestedBy(batchParams.paramNewRequestedBy);
			}
			if (sType.equals("A")){
				msf230Rec.setAuthsdBy(batchParams.paramNewAuthorisedBy);
			}
					
//			info ("ready for update");
//			info ("msf230 authsd by: " + msf230Rec.getAuthsdBy());
//			info ("msf230 requested by: " + msf230Rec.getRequestedBy());
			
			edoi.update(msf230Rec);			
			result = true;			
//			info ("*** updated MSF230 record ***");
			
		}	
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb231.printinfo("updateMSF230 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb231.printinfo ("updateMSF230 Error: " + ex.message);
		}
		return result;
	}

	/**
	 * report file. <br>
	 * <li> define getter and setter for the both report A and B </li>
	 **/
	private class Trb231aReportLine implements Comparable<Trb231aReportLine>
	{
		public String preqNo;
		public String oldRequestedBy;
		public String newRequestedBy;
		public String oldAuthsdBy;
		public String newAuthsdBy;
		public String authStatus;
		public String compltStatus;
		public String validEmp;
		public String empStatus;
		public String authByStatus;
		public String reqByStatus;
		public String updated;
		
		public Trb231aReportLine()
		{
		}
		
		//report is sorted via preq no
		public int compareTo(Trb231aReportLine otherReportLine)
		{
			if (!preqNo.equals(otherReportLine.preqNo))
			{
				return preqNo.compareTo(otherReportLine.preqNo)
			}
			return 0;
		}				
	}
	
	/**
	 * generate batch report <br>
	 * <li> 1. exception report Trb231A </li>
	 **/
	private generateTrb231Report()
	{
		ProcessTrb231.printinfo ("Process generateTrb231Report");
		
		try
		{
			Trb231aReportLine currLine;
			//String APP = "\"";
			String tempString;
			int idx;
			String sReason = " ";
			
			Collections.sort(arrayOfTrb231ReportLine);
			
			//info ("report size: " + arrayOfTrb231ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb231ReportLine.size())
			{
				currLine = arrayOfTrb231ReportLine.get(idx);
				
				tempString = " ";
				tempString = tempString + ("Purchase Requisition No    : ");
				tempString = tempString + (currLine.preqNo);
				DoReportA(tempString);
				
				DoReportA(" ");
				tempString = " ";
				tempString = tempString + ("Previous Requesting Officer: ");
				tempString = tempString + (currLine.oldRequestedBy);
				DoReportA(tempString);

				tempString = " ";
				tempString = tempString + ("new Requesting Officer     : ");
				tempString = tempString + (currLine.newRequestedBy);
				DoReportA(tempString);

				DoReportA(" ");
				tempString = " ";
				tempString = tempString + ("Previous Authorised Officer: ");
				tempString = tempString + (currLine.oldAuthsdBy);
				DoReportA(tempString);

				tempString = " ";
				tempString = tempString + ("new Authorised Officer     : ");
				tempString = tempString + (currLine.newAuthsdBy);
				DoReportA(tempString);

				if (!currLine.authStatus.equals("A")){
					sReason = "PReq not Authorised"
				}
				else{
					if (currLine.compltStatus.equals("C")){
						sReason = "PReq is completed";
					}
					else
					{
						if (!batchParams.paramNewAuthorisedBy.trim().equals("")){
							if (currLine.authByStatus.trim().equals("")){
								sReason = "New Authoriser not an Employee";
							}
							else{
								if (!currLine.authByStatus.equals("A")){
									sReason = "New Authoriser not an Active Employee";
								}
							}
						}
						else{
							if (!batchParams.paramNewRequestedBy.trim().equals("")){
								if (currLine.reqByStatus.trim().equals("")){
									sReason = "New Requestor not an Employee";
								}
								else{
									if (!currLine.reqByStatus.equals("A")){
										sReason = "New Requestor not an Active Employee";
									}
								}
							}
						}
					}
				}		
				
				if (currLine.updated.equals("Y")){;
					DoReportA(" ");
					
					tempString = " ";
					tempString = tempString + ("Status                     : Updated");
					DoReportA(tempString);
				}
				else{
					if (!currLine.authStatus.equals("A") || currLine.compltStatus.equals("C")
						|| currLine.authByStatus.trim().equals("") || !currLine.authByStatus.equals("A")
						|| currLine.reqByStatus.trim().equals("") || !currLine.reqByStatus.equals("A"))
					{
						DoReportA(" ");
						
						tempString = " ";
						tempString = tempString + ("Status                     : Rejected");
						DoReportA(tempString);
						
						tempString = " ";
						tempString = tempString + ("Reason                     : " + sReason);
						DoReportA(tempString);
					}
				}
				idx++;
			}
			DoReportA(" ");
		}
		catch (Exception ex)
		{
			ProcessTrb231.printinfo ("generateTrb231Report Error: " + ex.message);
		}		
	}

	/**
	 * write batch report header <br>
	 **/
	private void writeReportHeaderA()
	{
		//info("writeReportHeaderA");
		String tempString;
		
		ReportA.writeLine(132,"-");		
		DoReportA(StringUtils.center("TRB231:Amend Requesting/Authorising Officers",132));
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

		ProcessTrb231.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb231 process = new ProcessTrb231();
process.runBatch(binding);
