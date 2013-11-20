/**
 * @AIT 2013
 * Conversion from Trb22a.cbl for Ellipse 8 upgrade project
 * 18/07/2013 RL - VERSION 1
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
import com.mincom.ellipse.edoi.ejb.msf221.*;
import com.mincom.ellipse.edoi.ejb.msf220.*;
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
//public class AITBatch220a implements GroovyInterceptable {
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

public class ParamsTrb22a
{
	//List of Input Parameters
	String paramPoNo1;
	String paramItemNo1;
	String paramApprValue1;
	String paramStatus1;
	String paramPoNo2;
	String paramItemNo2;
	String paramApprValue2;
	String paramStatus2;
	String paramPoNo3;
	String paramItemNo3;
	String paramApprValue3;
	String paramStatus3;
	String paramPoNo4;
	String paramItemNo4;
	String paramApprValue4;
	String paramStatus4;
	String paramPoNo5;
	String paramItemNo5;
	String paramApprValue5;
	String paramStatus5;
	String paramPoNo6;
	String paramItemNo6;
	String paramApprValue6;
	String paramStatus6;
}

/**
 * Report to correct value of goods received  for POs <br>
 * <li> This program corrects POs which have </li>
 * <li> approved value <>  value of goods received</li>
 * <li> output: 1 report - Trb22aA. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrb22a extends AITBatch220a
public class ProcessTrb22a extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrb22a batchParams;
	
	public static final String WX_DISTRICT = "district";
	private String wxDstrct;
	
	private ArrayList arrayOfTrb22aReportLine = new ArrayList();
	
	private BigDecimal recordCount221;
	private BigDecimal recordCount221Excpt;
	private BigDecimal recordCount221Write;
	
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
		ProcessTrb22a.printinfo("VERSION      :" + version);
		ProcessTrb22a.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb22a.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb22a());

		//PrintRequest Parameters
		ProcessTrb22a.printinfo("paramPONo1       : " + batchParams.paramPoNo1);
		ProcessTrb22a.printinfo("paramItemNo1     : " + batchParams.paramItemNo1);
		ProcessTrb22a.printinfo("paramApprValue1  : " + batchParams.paramApprValue1);
		ProcessTrb22a.printinfo("paramStatus1     : " + batchParams.paramStatus1);
		ProcessTrb22a.printinfo("paramPONo2       : " + batchParams.paramPoNo2);
		ProcessTrb22a.printinfo("paramItemNo2     : " + batchParams.paramItemNo2);
		ProcessTrb22a.printinfo("paramApprValue2  : " + batchParams.paramApprValue2);
		ProcessTrb22a.printinfo("paramStatus2     : " + batchParams.paramStatus2);
		ProcessTrb22a.printinfo("paramPONo3       : " + batchParams.paramPoNo3);
		ProcessTrb22a.printinfo("paramItemNo3     : " + batchParams.paramItemNo3);
		ProcessTrb22a.printinfo("paramApprValue3  : " + batchParams.paramApprValue3);
		ProcessTrb22a.printinfo("paramStatus3     : " + batchParams.paramStatus3);
		ProcessTrb22a.printinfo("paramPONo4       : " + batchParams.paramPoNo4);
		ProcessTrb22a.printinfo("paramItemNo4     : " + batchParams.paramItemNo4);
		ProcessTrb22a.printinfo("paramApprValue4  : " + batchParams.paramApprValue4);
		ProcessTrb22a.printinfo("paramStatus4     : " + batchParams.paramStatus4);
		ProcessTrb22a.printinfo("paramPONo5       : " + batchParams.paramPoNo5);
		ProcessTrb22a.printinfo("paramItemNo5     : " + batchParams.paramItemNo5);
		ProcessTrb22a.printinfo("paramApprValue5  : " + batchParams.paramApprValue5);
		ProcessTrb22a.printinfo("paramStatus5     : " + batchParams.paramStatus5);
		ProcessTrb22a.printinfo("paramPONo6       : " + batchParams.paramPoNo6);
		ProcessTrb22a.printinfo("paramItemNo6     : " + batchParams.paramItemNo6);
		ProcessTrb22a.printinfo("paramApprValue6  : " + batchParams.paramApprValue6);
		ProcessTrb22a.printinfo("paramStatus6     : " + batchParams.paramStatus6);
		ProcessTrb22a.printinfo(" ");

		try
		{
			processBatch();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb22a.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb22a);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb22a.printinfo("processBatch");
		
		if (initialise()){
			processRequest();
			generateTrb22aReport();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrb22a.printinfo("initialise");
		
		recordCount221 = 0;
		recordCount221Write = 0;
		recordCount221Excpt = 0;
		
		ReportA = report.open('TRB22AA')
		writeReportHeaderA();
		
		//validate mandatory fields
		if (batchParams.paramPoNo1.trim().equals(""))
		{
			WriteError ("PO is mandatory");
			return false;
		}
		if (batchParams.paramItemNo1.trim().equals(""))
		{
			WriteError ("PO Item is mandatory");
			return false;
		}
		if (batchParams.paramApprValue1.equals(0))
		{
			WriteError ("Approved Value is mandatory");
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
		ProcessTrb22a.printinfo ("Error: " + msg);
	}
	
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb22a.printinfo("processRequest");

		int idx ;
		
		try
		{
			wxDstrct = commarea.getProperty(WX_DISTRICT);
			//info ("wxDstrct:" + wxDstrct);
		
			//get all params and put it in a list
			ArrayList <paramPOs> paramPOList = new ArrayList<paramPOs>();
			paramPOList = paramList();
						
			//process all PO & PO item from param list
			ArrayList <poItemRecord> POIList = new ArrayList<poItemRecord>();
			POIList = processMSF221(paramPOList);
			
			idx = 0;
			while (idx < POIList.size())
			{
				def rec = new poItemRecord();
				rec = POIList.get(idx);
				
				if (rec.errNo.trim().equals("")){
					//no error so prepare for update
					updatePO(rec);
				}
				
				//write to array
				def line = new Trb22aaReportLine();
				line.dstrctCode = rec.getDstrctCode();
				line.poNo = rec.getPoNo();
				line.poItem = rec.getPoItem();
				line.approvedValue = rec.getApprovedValue();
				line.recptValue = rec.getRecptValue();
				line.oldStatus = rec.getOldStatus();
				line.newStatus = rec.getNewStatus();
				line.wHouseId = rec.getwHouseId();
				line.poItemType = rec.getPoItemType();
				line.errorNo = rec.getErrNo();

				arrayOfTrb22aReportLine.add(line);
				idx++;
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb22a.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb22a.printinfo ("processRequest Error: " + ex.message);
		}
	}

	/**
	 * store params in an array list. <br>
	 **/
	private ArrayList <paramPOs> paramList ()
	{
		ArrayList <paramPOs> result = new ArrayList <paramPOs>();
		
		if (!batchParams.paramPoNo1.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPoNo1;
			p.poItem = batchParams.paramItemNo1;
			p.approvedValue = StringToBigDecimal(batchParams.paramApprValue1) / 100;
			p.status = batchParams.paramStatus1;
			result.add(p);
//			info ("po1: " + p.poNo);
//			info ("po item1: " + p.poItem);
//			info ("appr value1: " + p.approvedValue);
//			info ("status1: " + p.status);
		}

		if (!batchParams.paramPoNo2.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPoNo2;
			p.poItem = batchParams.paramItemNo2;
			p.approvedValue = StringToBigDecimal(batchParams.paramApprValue2) / 100;
			if (!batchParams.paramStatus2.trim().equals("")){
				p.status = batchParams.paramStatus2;
			}
			else{
				p.status = " ";
			}
			result.add(p);
//			info ("po2: " + p.poNo);
//			info ("po item2: " + p.poItem);
//			info ("appr value2: " + p.approvedValue);
//			info ("status2: " + p.status);
		}
		
		if (!batchParams.paramPoNo3.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPoNo3;
			p.poItem = batchParams.paramItemNo3;
			p.approvedValue = StringToBigDecimal(batchParams.paramApprValue3) / 100;
			if (!batchParams.paramStatus3.trim().equals("")){
				p.status = batchParams.paramStatus3;
			}
			else{
				p.status = " ";
			}
			result.add(p);
//			info ("po3: " + p.poNo);
//			info ("po item3: " + p.poItem);
//			info ("appr value3: " + p.approvedValue);
//			info ("status3: " + p.status);
		}
		
		if (!batchParams.paramPoNo4.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPoNo4;
			p.poItem = batchParams.paramItemNo4;
			p.approvedValue = StringToBigDecimal(batchParams.paramApprValue4) / 100;
			if (!batchParams.paramStatus4.trim().equals("")){
				p.status = batchParams.paramStatus4;
			}
			else{
				p.status = " ";
			}
			result.add(p);
//			info ("po4: " + p.poNo);
//			info ("po item4: " + p.poItem);
//			info ("appr value4: " + p.approvedValue);
//			info ("status4: " + p.status);
		}
		
		if (!batchParams.paramPoNo5.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPoNo5;
			p.poItem = batchParams.paramItemNo5;
			p.approvedValue = StringToBigDecimal(batchParams.paramApprValue5) / 100;
			if (!batchParams.paramStatus5.trim().equals("")){
				p.status = batchParams.paramStatus5;
			}
			else{
				p.status = " ";
			}
			result.add(p);
//			info ("po5: " + p.poNo);
//			info ("po item5: " + p.poItem);
//			info ("appr value5: " + p.approvedValue);
//			info ("status5: " + p.status);
		}
		
		if (!batchParams.paramPoNo6.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPoNo6;
			p.poItem = batchParams.paramItemNo6;
			p.approvedValue = StringToBigDecimal(batchParams.paramApprValue6) / 100;
			if (!batchParams.paramStatus6.trim().equals("")){
				p.status = batchParams.paramStatus6;
			}
			else{
				p.status = " ";
			}
			result.add(p);
//			info ("po6: " + p.poNo);
//			info ("po item6: " + p.poItem);
//			info ("appr value6: " + p.approvedValue);
//			info ("status6: " + p.status);
		}

		return result;
	}
	
	/**
	 * get PO and PO item records.
	 */
	private ArrayList<poItemRecord> processMSF221(ArrayList<paramPOs> paramList) {
		//ProcessTrb22a.printinfo("processMSF221");
		
		ArrayList<poItemRecord> result = new ArrayList<poItemRecord>();
		int i;		
		String errorSw;
				
		try
		{
			i = 0;
			while (i < paramList.size())
			{
				errorSw = " ";
				def rec = new paramPOs();
				rec = paramList.get(i);
				
				//browse through 221
				Constraint c1 = MSF221Key.poNo.equalTo(rec.getPoNo());
				Constraint c2 = MSF221Key.poItemNo.equalTo(rec.getPoItem());
				
				def query = new QueryImpl(MSF221Rec.class).and(c1).and(c2)
				MSF221Rec msf221Rec = (MSF221Rec) edoi.firstRow(query);
				
				if (msf221Rec){
					if (msf221Rec.getStatus_221().equals("0") || msf221Rec.getStatus_221().equals("1") ||
						msf221Rec.getStatus_221().equals("2") || msf221Rec.getStatus_221().equals("3") ||
						msf221Rec.getStatus_221().equals("9")){
					
						if (msf221Rec.getValAppr() != rec.getApprovedValue()){
							errorSw = "2"
						}
					}
					else{
						errorSw = "3"
					}
				}
				else{
					errorSw = "1";
				}
				
				def pi = new poItemRecord();
				pi.dstrctCode = msf221Rec.getDstrctCode();
				pi.poNo = msf221Rec.getPrimaryKey().getPoNo();
				pi.poItem = msf221Rec.getPrimaryKey().getPoItemNo();
				pi.oldStatus = msf221Rec.getStatus_221();
				if (!rec.getStatus().trim().equals("")){
					pi.newStatus = rec.getStatus();
				}
				else{
					pi.newStatus = " ";
				}
				pi.recptValue = msf221Rec.getValRcptFor();
				pi.approvedValue = rec.getApprovedValue();
				pi.wHouseId = msf221Rec.getWhouseId();
				pi.poItemType = msf221Rec.getPoItemType();
				if (errorSw.trim().equals("")){
					pi.errNo = " ";
				}
				else{
					pi.errNo = errorSw;
				}
				
				//info("pi record: " + pi.poNo + " / " + pi.poItem + " / " + pi.oldStatus + " / " + pi.wHouseId + " / " + pi.approvedValue + " / " + msf221Rec.getValAppr());
				result.add(pi);
				i++;
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb22a.printinfo("processMSF221 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb22a.printinfo ("processMSF221 Error: " + ex.message);
		}
		return result;
	}

	/**
	 * update 220 & 221. <br>
	 **/
	private void updatePO (poItemRecord p)
	{
		//info ("process updatePO");
		
		try
		{
			//update status_200 if new status was entered
			if (!p.getNewStatus().trim().equals("")) {
				MSF220Rec msf220Rec = edoi.findByPrimaryKey(new MSF220Key(p.getPoNo()))
//				info (" ")
//				info ("before update")
//				info ("msf220 rec: " + msf220Rec.getPrimaryKey() + " - " + msf220Rec.getStatus_220())
				
				msf220Rec.setStatus_220(p.getNewStatus());
//				info ("ready for update")
//				info ("msf220 rec: " + msf220Rec.getStatus_220())
				
				edoi.update(msf220Rec);
//				info ("*** updated MSF220 ***");
//				info (" ");
			}
			
			//correct value goods received in MSF221
			MSF221Rec msf221Rec = edoi.findByPrimaryKey(new MSF221Key(p.getPoNo(),p.getPoItem()))
//			info (" ")
//			info ("before update")
//			info ("msf221 rec: " + msf221Rec.getPrimaryKey() + " - " + msf221Rec.getStatus_221()+ " - " + msf221Rec.getValRcptFor())
			
			if (!p.getNewStatus().trim().equals("")) {
				msf221Rec.setStatus_221(p.getNewStatus());
			}
			msf221Rec.setValRcptFor(p.getApprovedValue());
//			info ("ready for update")
//			info ("msf221 rec: " + msf221Rec.getStatus_221() + " - " + msf221Rec.getValRcptFor())
			
			edoi.update(msf221Rec);
//			info ("*** updated MSF221 ***");
//			info (" ");

		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb22a.printinfo("updatePO failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb22a.printinfo ("updatePO Error: " + ex.message);
		}
	}

	/**
	 * Parameters class.
	 */
	public class paramPOs
	{
		String poNo;
		String poItem;
		BigDecimal approvedValue;
		String status;
	}
	
	/**
	 * PO item class.
	 */
	public class poItemRecord
	{
		String dstrctCode = " ";
		String poNo = " ";
		String poItem = " ";
		BigDecimal recptValue = 0;
		BigDecimal approvedValue = 0;
		String oldStatus = " ";
		String newStatus = " ";
		String wHouseId = " ";
		String poItemType = " ";
		String errNo = " ";
	}

	
	/**
	 * report file. <br>
	 * <li> define getter and setter for the both report A and B </li>
	 **/
	private class Trb22aaReportLine implements Comparable<Trb22aaReportLine>
	{
		public String dstrctCode;
		public String poNo;
		public String poItem;
		public BigDecimal recptValue;
		public BigDecimal approvedValue;
		public String oldStatus;
		public String newStatus;
		public String wHouseId;
		public String errorNo;
		public String poItemType = "";
		
		public Trb22aaReportLine()
		{
		}
		
		//report is sorted via po no
		public int compareTo(Trb22aaReportLine otherReportLine)
		{
			if (!dstrctCode.equals(otherReportLine.dstrctCode))
			{
				return dstrctCode.compareTo(otherReportLine.dstrctCode)
			}
			if (!poNo.equals(otherReportLine.poNo))
			{
				return poNo.compareTo(otherReportLine.poNo)
			}
			if (!poItem.equals(otherReportLine.poItem))
			{
				return poItem.compareTo(otherReportLine.poItem)
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
	 * <li> 1. exception report Trb22aA </li>
	 **/
	private generateTrb22aReport()
	{
		ProcessTrb22a.printinfo ("Process generateTrb22aReport");
		String statusD = "";
		
		try
		{
			Trb22aaReportLine currLine;
			//String APP = "\"";
			String tempString;
			int idx;
			
			Collections.sort(arrayOfTrb22aReportLine);
			
			//info ("report size: " + arrayOfTrb22aReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb22aReportLine.size())
			{
				currLine = arrayOfTrb22aReportLine.get(idx);

				tempString = " ";
				if (currLine.dstrctCode.equals(wxDstrct) &&
					(currLine.poItemType.equals("P") || currLine.poItemType.equals("S") || currLine.poItemType.equals("F") &&
					 !currLine.wHouseId.trim().equals(""))){
				 
					 tempString = tempString + (currLine.poNo + "-" + currLine.wHouseId).padRight(10);
				}
				else{
					tempString = tempString + (currLine.poNo).padRight(10);
				}
				
				tempString = tempString + (currLine.poItem).padRight(4);
				tempString = tempString + decFormatter.format(currLine.approvedValue).padLeft(18);
				tempString = tempString + decFormatter.format(currLine.recptValue).padLeft(18);
				statusD = statusDesc(currLine.oldStatus);
				tempString = tempString + ("             " + statusD).padRight(27);
				statusD = statusDesc(currLine.newStatus);
				tempString = tempString + (statusD).padRight(13);
				
				switch (currLine.errorNo){
					case "1":
						tempString = tempString + "Rejected - PO Item No. Not Existing";
						recordCount221Excpt++;
						break;
					case "2":
						tempString = tempString + "Rejected - Param Apprvd Value not = MSF221 Value";
						recordCount221Excpt++;
						break;
					case "3":
						tempString = tempString + "Rejected - Invalid Status.  Must be 0, 1, 2, 3, 9";
						recordCount221Excpt++;
						break;
					default:
						tempString = tempString + "Updated";
						recordCount221Write++;
						break;
				}
				
				DoReportA(tempString);
				
				idx++;
			}
			
			//write records count
			ReportA.writeLine(132,"-");
			DoReportA(" ");
			DoReportA(" ");

			tempString = " ";
			tempString = tempString + "No of 220 records exception   : ".padLeft(54);
			tempString = tempString + recordCount221Excpt.toString();
			DoReportA(tempString);
			
			tempString = " ";
			tempString = tempString + "No of 220 records updated     : ".padLeft(54);
			tempString = tempString + recordCount221Write.toString();
			DoReportA(tempString);

			DoReportA(" ");

		}
		catch (Exception ex)
		{
			ProcessTrb22a.printinfo ("generateTrb22aReport Error: " + ex.message);
		}
	}
	
	private String statusDesc(String s)
	{
		String result = " ";
		switch (s){
			case "0":
				result = "Unprinted";
				break;
			case "1":
				result = "Printed";
				break;
			case "2":
				result = "Modified";
				break;
			case "3":
				result =  "Cancelled";
				break;
			case "9":
				result =  "Complete";
				break;
			case " ":
				result =  " ";
				break;
			default:
				result =  "*Unknown*";
				break;
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
		DoReportA(StringUtils.center("Correct Purchase Order Approved Value Not = Value of Goods Received",132));

		ReportA.writeLine(132,"-");
		tempString = " PO No.    Item           Approved-Value    Goods-Recvd-Value   Orig-Status   New-Status   Remarks";
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

		ProcessTrb22a.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb22a process = new ProcessTrb22a();
process.runBatch(binding);
