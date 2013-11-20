/**
 * @AIT 2013
 * Conversion from Trh220.cbl for Ellipse 8 upgrade project
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
import com.mincom.ellipse.types.m0000.instances.ErrNo;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.ellipse.common.unix.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf220.*;
import com.mincom.ellipse.edoi.ejb.msf221.*;
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
import java.lang.String;


////-------------------------------------------------------------------
//import groovy.lang.Binding;
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;
//
//public class AITBatch220h implements GroovyInterceptable {
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

public class ParamsTrh220
{
	//List of Input Parameters
	String paramLine1;
	String paramPONo1;
	String paramPOItem1;
	String paramPONo2;
	String paramPOItem2;
	String paramPONo3;
	String paramPOItem3;
	String paramPONo4;
	String paramPOItem4;
	String paramPONo5;
	String paramPOItem5;
	String paramPONo6;
	String paramPOItem6;
}

/**
 * Report mismatched status for completed/cancelled PO items<br>
 * <li> This program reads the purchase orders items MSF221 </li>
 * <li> and determines if the item need can be complete based on </li>
 * <li> the qty ordered and qty receieved. Order item won't be</li>
 * <li> completed if there are any invoice pending items. </li>
 * <li> output: 1 report - Trh220A. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrh220 extends AITBatch220h
public class ProcessTrh220 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrh220 batchParams;
	
	private ArrayList arrayOfTrh220ReportLine = new ArrayList();
	
	private	BigDecimal curQty = 0;
	private	BigDecimal qtyRcvdDir = 0;
	private	String status221 = " ";
	
	private String errorSw;
	private Boolean purReq;
	private String dstrct231;
	private String preq231;
	private String preqItem231;
	
	private BigDecimal recordCountExcpt;
	private BigDecimal recordCountWrite;
	
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
		ProcessTrh220.printinfo("VERSION      :" + version);
		ProcessTrh220.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrh220.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrh220());
 
		//PrintRequest Parameters
		ProcessTrh220.printinfo("paramLine1       : " + batchParams.paramLine1);
		ProcessTrh220.printinfo("paramPONo1       : " + batchParams.paramPONo1);
		ProcessTrh220.printinfo("paramPOItem1     : " + batchParams.paramPOItem1);
		ProcessTrh220.printinfo("paramPONo2       : " + batchParams.paramPONo2);
		ProcessTrh220.printinfo("paramPOItem2     : " + batchParams.paramPOItem2);
		ProcessTrh220.printinfo("paramPONo3       : " + batchParams.paramPONo3);
		ProcessTrh220.printinfo("paramPOItem3     : " + batchParams.paramPOItem3);
		ProcessTrh220.printinfo("paramPONo4       : " + batchParams.paramPONo4);
		ProcessTrh220.printinfo("paramPOItem4     : " + batchParams.paramPOItem4);
		ProcessTrh220.printinfo("paramPONo5       : " + batchParams.paramPONo5);
		ProcessTrh220.printinfo("paramPOItem5     : " + batchParams.paramPOItem5);
		ProcessTrh220.printinfo("paramPONo6       : " + batchParams.paramPONo6);
		ProcessTrh220.printinfo("paramPOItem6     : " + batchParams.paramPOItem6);
		ProcessTrh220.printinfo(" ");

		try
		{
			processBatch();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrh220.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrh220);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrh220.printinfo("processBatch");

		if (initialise()){
			processRequest();
			generateTrh220Report();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrh220.printinfo("initialise");
		
		recordCountExcpt = 0;
		recordCountWrite = 0;
		
		ReportA = report.open('Trh220A')
		writeReportHeaderA();
		
		//validate mandatory fields
		if (batchParams.paramPONo1.trim().equals(""))
		{
			WriteError ("PO number is Mandatory");
			return false;
		}

		if (batchParams.paramPOItem1.trim().equals(""))
		{
			WriteError ("PO item number is Mandatory");
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
		ProcessTrh220.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process records from MSF221 </li>
	 */
	private void processRequest()
	{
		ProcessTrh220.printinfo("processRequest");
		int i = 1;
		int idx = 0;
		try
		{
			//get all params and put it in a list
			ArrayList <paramPOs> paramPOList = new ArrayList<paramPOs>();
			paramPOList = paramList();
			
			//process each PO & PO item from param list
			while (idx < paramPOList.size()){
				def poi = new paramPOs();
				poi = paramPOList.get(idx);
				processMSF221(poi.poNo, poi.poItem);
	
				if (errorSw.trim().equals("")){
					//no error so prepare for update
					if (purReq) {
						updatePurReq();
					}
					updatePOStatus(poi);
				}
				
				//write to array
				def line = new Trh220aReportLine();
				line.poNo = poi.poNo;
				line.poItem = poi.poItem;
				line.curQty = curQty;
				line.qtyRcvDir = qtyRcvdDir;
				line.status221 = status221;
				line.errNo = errorSw;

				arrayOfTrh220ReportLine.add(line);
				idx++;
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrh220.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrh220.printinfo ("processRequest Error: " + ex.message);
		}
	}

	/**
	 * store params in an array list. <br>
	 **/
	private ArrayList <paramPOs> paramList ()
	{
		ArrayList <paramPOs> result = new ArrayList <paramPOs>();
		
		if (!batchParams.paramPONo1.trim().equals("") && !batchParams.paramPOItem1.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPONo1;
			p.poItem = batchParams.paramPOItem1;
			result.add(p);
//			info ("po1: " + p.poNo);
//			info ("po item1: " + p.poItem);
		}
		if (!batchParams.paramPONo2.trim().equals("") && !batchParams.paramPOItem2.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPONo2;
			p.poItem = batchParams.paramPOItem2;
			result.add(p);
//			info ("po2: " + p.poNo);
//			info ("po item2: " + p.poItem);
		}
		if (!batchParams.paramPONo3.trim().equals("") && !batchParams.paramPOItem3.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPONo3;
			p.poItem = batchParams.paramPOItem3;
			result.add(p);
//			info ("po3: " + p.poNo);
//			info ("po item3: " + p.poItem);
		}
		if (!batchParams.paramPONo4.trim().equals("") && !batchParams.paramPOItem4.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPONo4;
			p.poItem = batchParams.paramPOItem4;
			result.add(p);
//			info ("po4: " + p.poNo);
//			info ("po item4: " + p.poItem);
		}
		if (!batchParams.paramPONo5.trim().equals("") && !batchParams.paramPOItem5.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPONo5;
			p.poItem = batchParams.paramPOItem5;
			result.add(p);
//			info ("po5: " + p.poNo);
//			info ("po item5: " + p.poItem);
		}
		if (!batchParams.paramPONo6.trim().equals("") && !batchParams.paramPOItem6.trim().equals("")){
			def p = new paramPOs();
			p.poNo = batchParams.paramPONo6;
			p.poItem = batchParams.paramPOItem6;
			result.add(p);
//			info ("po6: " + p.poNo);
//			info ("po item6: " + p.poItem);
		}
		return result;
	}
	
	/**
	 * process MSF221. <br>
	 **/
	private void processMSF221 (String po, String pi)
	{
//		info ("process processMSF221");
		try
		{
			errorSw = " ";
			curQty = 0;
			qtyRcvdDir = 0;
			status221 = " ";
			Constraint c1 = MSF221Key.poNo.equalTo(po);
			Constraint c2 = MSF221Key.poItemNo.equalTo(pi);
				
			def query = new QueryImpl(MSF221Rec.class).and(c1).and(c2);
			MSF221Rec msf221Rec = (MSF221Rec) edoi.firstRow(query);
			
			if (msf221Rec){
				curQty = msf221Rec.getCurrQtyI();
				qtyRcvdDir = msf221Rec.getQtyRcvDirI();
				status221 = msf221Rec.getStatus_221();
//				info("po: " + po);
//				info("pi: " + pi);
//				info("curQty: " + curQty);
//				info("qtyRcvdDir: " + qtyRcvdDir);
//				info("status221: " + status221);
//				info("inv pending value local: " + msf221Rec.getInvPendValL());
				
				switch (status221)
				{
					case '3':
						errorSw = "2";
						break;
					case '9':
						errorSw = "3";
						break;
					default:
						errorSw = " ";
						break;
				}
				if (curQty != qtyRcvdDir){
					errorSw = "4";
				}
				if (msf221Rec.getInvPendValL() != 0){
					errorSw = "5";
				}

				if (msf221Rec.getPoItemType().equals("P")){
					processMSF231(msf221Rec.getOrigDstCde(), msf221Rec.getPreqStkCode());
				}
			}
			else
			{
				errorSw = "1";
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrh220.printinfo("processMSF221 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrh220.printinfo ("processMSF221 Error: " + ex.message);
		}
	}
	
	public class paramPOs
	{
		String poNo;
		String poItem;
	}

	/**
	 * process MSF231. <br>
	 **/
	private void processMSF231 (String dstrct221, String po221)
	{
//		info ("process processMSF231");
		purReq = false;
		
		try
		{
			dstrct231 = "";
			preq231 = "";
			preqItem231 = "";
			Constraint c1 = MSF231Key.preqNo.equalTo(po221.substring(0, 6));
			Constraint c2 = MSF231Key.preqItemNo.equalTo(po221.substring(6, 9));
			Constraint c3 = MSF231Key.dstrctCode.equalTo(dstrct221);
			Constraint c4 = MSF231Rec.status_231.notEqualTo("9");
			
			def query = new QueryImpl(MSF231Rec.class).and(c1).and(c2).and(c3).and(c4);
			MSF231Rec msf231Rec = (MSF231Rec) edoi.firstRow(query);
			
			if (msf231Rec){
//				info ("preq qty required: " + msf231Rec.getPrQtyReqd());
//				info ("preq qty recvd: " + msf231Rec.getPrQtyRcvd());
				
				if (msf231Rec.getPrQtyReqd() != msf231Rec.getPrQtyRcvd()){
					errorSw = "6";
				}
				else
				{
					dstrct231 = msf231Rec.getPrimaryKey().getDstrctCode();
					preq231 = msf231Rec.getPrimaryKey().getPreqNo();
					preqItem231 = msf231Rec.getPrimaryKey().getPreqItemNo();
					purReq = true;
				}
			}
			else{
			//changed in Ellipse 8. extra error handling so we don't update 231
			// if no preq found
				errorSw = "7";
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrh220.printinfo("processMSF231 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrh220.printinfo ("processMSF231 Error: " + ex.message);
		}
	}

	/**
	 * complete PO & item in MSF220 & MSF221. <br>
	 **/
	private void updatePOStatus (paramPOs poRec)
	{
		//info ("process updatePOStatus");
		int complItem = 0;
		Date todayDate = new Date();
		String sTodayDate = todayDate.format("yyyyMMdd");
		Boolean update220 = false;
		
		try
		{
			//UPDATE MSF220
			MSF220Rec msf220Rec = edoi.findByPrimaryKey(new MSF220Key(poRec.getPoNo()))
			complItem = msf220Rec.getCompleteItems().toInteger() + 1;

//			info (" ")
//			info ("before update")
//			info ("msf220 rec: " + msf220Rec.getPrimaryKey() + " - " + msf220Rec.getStatus_220())
//			info ("msf220 no of items: " +  msf220Rec.getCompleteItems() + " - " + msf220Rec.getNoOfItems() + " - " + sTodayDate)

			//accumulate complete items
			msf220Rec.setCompleteItems(complItem.toString());
			if (msf220Rec.getNoOfItems().toInteger() == complItem) {
				msf220Rec.setStatus_220("9");
				msf220Rec.setCompletedDate(sTodayDate);
			}
//			info ("ready for update")
//			info ("msf220 rec: " + msf220Rec.getStatus_220() + " - " + msf220Rec.getCompleteItems() + " - " + msf220Rec.getCompletedDate())
			
			edoi.update(msf220Rec);
			update220 = true;
			
			//*** FOR TESTING ONLY
//			if (poRec.getPoNo().equals("638614") && poRec.getPoItem().equals("001")){
//				edoi.update(msf220Rec);
//				info ("*** update msf220 record - PO no : " + poRec.getPoNo() + "/" + poRec.getPoItem())
//				info ("************************************")
//			}

			//UPDATE MSF221
			MSF221Rec msf221Rec = edoi.findByPrimaryKey(new MSF221Key(poRec.getPoNo(), poRec.getPoItem()))
//			info (" ")
//			info ("before update")
//			info ("msf221 rec: " + msf221Rec.getPrimaryKey() + " - " + msf221Rec.getStatus_221() + " - " + msf221Rec.getExpediteDate())
						
			msf221Rec.setStatus_221("9");
			if (update220){
				msf221Rec.setExpediteDate("99999999");
			}
//			info ("ready for update")
//			info ("msf221 rec: " + msf221Rec.getStatus_221() + " - " + msf221Rec.getExpediteDate())
						
			edoi.update(msf221Rec);
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrh220.printinfo("updatePOStatus failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrh220.printinfo ("updatePOStatus Error: " + ex.message);
		}
	}
	
	/**
	 * for purchase requisition, complete items in MSF230 & MSF231 if not complete. <br>
	 **/
	private void updatePurReq ()
	{
		//info ("process updatePurReq");
		int complItem = 0;
		Date todayDate = new Date();
		String sTodayDate = todayDate.format("yyyyMMdd");
		
		try
		{
			//UPDATE MSF231
			MSF231Rec msf231Rec = edoi.findByPrimaryKey(new MSF231Key(dstrct231,preq231,preqItem231))
						
//			info (" ")
//			info ("Before update")
//			info ("msf231 rec: " + msf231Rec.getPrimaryKey() + " - " + msf231Rec.getStatus_231())
			
			msf231Rec.setStatus_231("9");
//			info ("ready for update")
//			info ("msf231 rec: " + msf231Rec.getStatus_231())
			
			edoi.update(msf231Rec);
			
			//*** FOR TESTING ONLY
//			if (poRec.getPoNo().equals("638614") && poRec.getPoItem().equals("001")){
//				edoi.update(msf220Rec);
//				info ("*** update msf220 record - PO no : " + poRec.getPoNo() + "/" + poRec.getPoItem())
//				info ("************************************")
//			}

			//UPDATE MSF230
			MSF230Rec msf230Rec = edoi.findByPrimaryKey(new MSF230Key(dstrct231, preq231))
			complItem = msf230Rec.getCompleteItems().toInteger() + 1;
//			info (" ")
//			info ("before update")
//			info ("msf230 rec: " + msf230Rec.getPrimaryKey() + " - " + msf230Rec.getCompleteItems() + " - " +
//				       msf230Rec.getCompletedDate() + " - " + msf230Rec.getNoOfItems())

			//accumulate complete items
			if (complItem >= msf230Rec.getNoOfItems().toInteger()){
				msf230Rec.setCompleteItems(msf230Rec.getNoOfItems());
				msf230Rec.setCompletedDate(sTodayDate);
			}

//			info ("ready for update")
//			info ("msf230 rec: " + msf230Rec.getCompleteItems() + " - " + msf230Rec.getCompletedDate())
						
			edoi.update(msf230Rec);
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrh220.printinfo("updatePurReq failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrh220.printinfo ("updatePurReq Error: " + ex.message);
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
	 * report file. <br>
	 * <li> define getter and setter for the both report A and B </li>
	 **/
	private class Trh220aReportLine implements Comparable<Trh220aReportLine>
	{
		public String poNo;
		public String poItem;
		public BigDecimal curQty;
		public BigDecimal qtyRcvDir;
		public String status221;
		public String errNo;
		public String comment;
		
		public Trh220aReportLine()
		{
		}
		
		//report is sorted via po no
		public int compareTo(Trh220aReportLine otherReportLine)
		{
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
	
	/**
	 * generate batch report <br>
	 * <li> 1. exception report Trh220A </li>
	 **/
	private generateTrh220Report()
	{
		ProcessTrh220.printinfo ("Process generateTrh220Report");
		try
		{
			Trh220aReportLine currLine;
			//String APP = "\"";
			String tempString;
			int idx;
			
			Collections.sort(arrayOfTrh220ReportLine);
			
			//info ("report size: " + arrayOfTrh220ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrh220ReportLine.size())
			{
				currLine = arrayOfTrh220ReportLine.get(idx);
				
				//info ("cur po: " + currLine.poNo);
				//info ("cur po item: " + currLine.poItem);
				
				tempString = " ";
				tempString = tempString + (currLine.poNo + "/" + currLine.poItem ).padRight(19);
				tempString = tempString + decFormatterNoDec.format(currLine.curQty).padRight(21);
				tempString = tempString + decFormatterNoDec.format(currLine.qtyRcvDir).padRight(20);
				
				switch (currLine.status221){
					case "0":
						tempString = tempString + "Unprinted".padRight(25);
						break;
					case "1":
						tempString = tempString + "Printed".padRight(25);
						break;
					case "2":
						tempString = tempString + "Modified".padRight(25);
						break;
					case "3":
						tempString = tempString + "Cancelled".padRight(25);
						break;
					case "9":
						tempString = tempString + "Complete".padRight(25);
						break;
					default:
						tempString = tempString + "*Unknown*".padRight(25);
						break;
				}
				
				switch (currLine.errNo){
					case "1":
						tempString = tempString + "Rejected - PO Item No. Not Existing";
						break;
					case "2":
						tempString = tempString + "Rejected - Item Cancelled";
						break;
					case "3":
						tempString = tempString + "Rejected - Item Complete";
						break;
					case "4":
						tempString = tempString + "Qty received not equal to qty ordered";
						break;
					case "5":
						tempString = tempString + "Rejected - Invoice Value Pending > 0";
						break;
					case "6":
						tempString = tempString + "Rejected - Purchase Req qty reqd <> qty rcvd";
						break;
					case "7":
						tempString = tempString + "Rejected - Purchase Req Item No. Not Existing";
						break;
					default:
						tempString = tempString + "Updated";
						break;
				}
				if (currLine.errNo.trim().equals("")){
					recordCountWrite++;
				}
				else{
					recordCountExcpt++;
				}
				//info("tempString: " + tempString);
				DoReportA(tempString);
				
				idx++;
			}
			
			//write records count
//			ReportA.writeLine(132,"-");
//			DoReportA(" ");
			DoReportA(" ");
			
			tempString = " ";
			tempString = tempString + "Total Number of Records Rejected : ".padLeft(54);
			tempString = tempString + recordCountExcpt.toString().padLeft(10);
			DoReportA(tempString);

			tempString = " ";
			tempString = tempString + "Total Number of Records Updated  : ".padLeft(54);
			tempString = tempString + recordCountWrite.toString().padLeft(10);
			DoReportA(tempString);

			DoReportA(" ");

		}
		catch (Exception ex)
		{
			ProcessTrh220.printinfo ("generateTrh220Report Error: " + ex.message);
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
		DoReportA(StringUtils.center("Purch Order Housekeeping - Completing Received Items",132));

		ReportA.writeLine(132,"-");
		tempString = " PO/ITEM          Current Qty          Qty Received          Orig-221-Status          Remarks"
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

		ProcessTrh220.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrh220 process = new ProcessTrh220();
process.runBatch(binding);
