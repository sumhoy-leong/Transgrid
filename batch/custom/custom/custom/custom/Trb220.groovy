/**
 * @AIT 2013
 * 16/07/2013 RL - fixed bugs
 *               - changed version to 2
 * Conversion from Trb220.cbl for Ellipse 8 upgrade project
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
import com.mincom.ellipse.edoi.ejb.msf100.*;
import com.mincom.ellipse.edoi.ejb.msf220.*;
import com.mincom.ellipse.edoi.ejb.msf221.*;
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


//-------------------------------------------------------------------
import groovy.lang.Binding;
import com.mincom.ellipse.edoi.common.logger.EDOILogger;
import com.mincom.ria.xml.bind.BigDecimalBinder;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;

//public class AITBatch220 implements GroovyInterceptable {
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

public class ParamsTrb220
{
	//List of Input Parameters
	String paramRunType;
}

/**
 * Report mismatched status for completed/cancelled PO items<br>
 * <li> This program can be run under 2 modes: report or update mode </li>
 * <li> For report mode: The program will retrieve all uncompleted/uncanceled PO headers </li>
 * <li> and check the corresponding items to see if the status is the same</li>
 * <li> if not, report those as exception. </li>
 * <li> For update mode: same logic applied but the header's status will be updated </li>
 * <li> depending on the item's status </li>
 * <li> output: 1 report - Trb220A. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
//public class ProcessTrb220 extends AITBatch220
public class ProcessTrb220 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 2;
	private ParamsTrb220 batchParams;
	
	private ArrayList <poRecord> listOfPOs = new ArrayList <poRecord>();
	private ArrayList arrayOfTrb220ReportLine = new ArrayList();
	private DataSource dataSource;
	
	public static final String WX_DISTRICT = "district";
	private String wxDstrct;
	
	private String itemDesc;
	private Boolean update220;
	
	private BigDecimal recordCount100;
	private BigDecimal recordCount220;
	private BigDecimal recordCount221;
	private BigDecimal recordCount231;
	private BigDecimal recordCount220Excpt;
	private BigDecimal recordCount220Write;
	
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
		ProcessTrb220.printinfo("VERSION      :" + version);
		ProcessTrb220.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb220.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb220());

		//PrintRequest Parameters
		ProcessTrb220.printinfo("paramRunType     : " + batchParams.paramRunType);
		ProcessTrb220.printinfo(" ");

		try
		{
			processBatch();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb220.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb220);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb220.printinfo("processBatch");

		if (initialise()){
			processRequest();
			generateTrb220Report();
		}
		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private Boolean initialise()
	{
		ProcessTrb220.printinfo("initialise");
		
		recordCount100 = 0;
		recordCount220 = 0;
		recordCount221 = 0;
		recordCount231 = 0;
		recordCount220Write = 0;
		recordCount220Excpt = 0;
		
		ReportA = report.open('Trb220A')
		writeReportHeaderA();
		
		//validate mandatory fields
		if (batchParams.paramRunType.trim().equals(""))
		{
			WriteError ("Run Type is Mandatory");
			return false;
		}
		
		if (batchParams.paramRunType.trim().equals("R") || batchParams.paramRunType.trim().equals("U"))
		{
			return true;
		}
		else
		{
			WriteError ("Input must be R or U");
			return false;
		}
	}

	private void WriteError(String msg)
	{
		ReportA.write(" ");
		ReportA.write(msg);
		ReportA.write(" ");
		ReportA.write(" ");
		ProcessTrb220.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb220.printinfo("processRequest");

		ArrayList<poRecord> p = new ArrayList<poRecord>();
		int idx ;
		
		try
		{
			wxDstrct = commarea.getProperty(WX_DISTRICT);
			//info ("wxDstrct:" + wxDstrct);
			
			listOfPOs = getPORecords();

			//process records
			if (listOfPOs.size() > 0){
				listOfPOs.each {poRecord rec->

					//for each PoNo, make sure all items has to be in status 3 or 9 before update header
					//browse through 221
					p = processMSF221(rec);
					
					if (update220){
						//info ("write to array");
						
						idx = 0;
						while (idx < p.size()){
							def r = new poItemRecord();
							r = p.get(idx);
														
							getItemDesc(r);
							//write to array
							def line = new Trb220aReportLine();
							line.poNo = r.getPoNo();
							line.poItem = r.getPoItem();
							line.orderDate = r.getOrderDate();
							line.itemDesc = itemDesc;
							line.curQty = r.getCurQty();
							line.status221 = r.getStatus_221();
			
							arrayOfTrb220ReportLine.add(line);
							recordCount220Excpt++;
							
							if (batchParams.paramRunType.equals("U")){
								updateStatus220(r.getPoNo(), r.getStatus_221());
							}
							idx++;
						}
					}
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb220.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb220.printinfo ("processRequest Error: " + ex.message);
		}
	}
	
	/**
	 * Browse the PO and PO item records.
	 */
	private ArrayList<poRecord> getPORecords() {
		ProcessTrb220.printinfo("getPORecords");
		
		List <poRecord> result = new ArrayList <poRecord>()
		
		try
		{
			//browse through 220
			def sql = new Sql(dataSource)
			sql.eachRow(qryString(), {
//				info ("it: " + it);
					
				def rec = new poRecord()
				rec.setDstrctCode(it.dstrct_code)
				rec.setPoNo(it.po_no)
				rec.setOrderDate(it.order_date)
				rec.setStatus_220(it.status_220)

				result.add(rec)
				recordCount220++;
				
	//			info ("PO rec: " +
	//				"dstrct: " + rec.getDstrctCode() + " " +
	//				"po no: " +  rec.getPoNo() +  " " +
	//				"order date: " + rec.getOrderDate() + " " +
	//				"status 220: " + rec.getStatus_220());
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb220.printinfo("getPORecords failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb220.printinfo ("getPORecords Error: " + ex.message);
		}
		return result;
	}
	
	// Create SQL statement
	private String qryString() {
		info("setQryString")
		StringBuffer qry = new StringBuffer()
		qry.append("select ")
		qry.append("a.dstrct_code, ")
		qry.append("a.po_no, ")
		qry.append("a.status_220, ")
		qry.append("a.order_date ")
		qry.append("from msf220 a ")
		qry.append("where a.status_220 in ('0', '1', '2') ")
		qry.append("and a.dstrct_code = '${wxDstrct}' ")
		qry.append("order by ")
		qry.append("a.dstrct_code, a.status_220, a.po_no")
		info(qry.toString())
		return qry.toString()
	}

	/**
	 * get PO and PO item records.
	 */
	private ArrayList<poItemRecord> processMSF221(poRecord rec) {
		//ProcessTrb220.printinfo("processMSF221");
		
		List <poItemRecord> result = new ArrayList <poItemRecord>();
		
		try
		{
			update220 = true;
			//browse through 221
			Constraint c1 = MSF221Key.poNo.equalTo(rec.getPoNo());
			Constraint c2 = MSF221Key.poItemNo.greaterThanEqualTo("001");
			
			def query = new QueryImpl(MSF221Rec.class).and(c1).and(c2)

			edoi.search(query,10000,{MSF221Rec msf221Rec ->
				if (msf221Rec){
					def pi = new poItemRecord();
					pi.dstrctCode = msf221Rec.getDstrctCode();
					pi.poNo = msf221Rec.getPrimaryKey().getPoNo();
					pi.poItem = msf221Rec.getPrimaryKey().getPoItemNo();
					pi.curQty = msf221Rec.getCurrQtyI();
					pi.status_221 = msf221Rec.getStatus_221();
					pi.stockCode = msf221Rec.getPreqStkCode();
					pi.status_220 = rec.getStatus_220();
					pi.orderDate = rec.getOrderDate();
					result.add(pi);
					
					//info("pi: " + pi.poNo + " " + pi.poItem + " " + pi.status_220 + " " + pi.status_221);
					
					if (msf221Rec.getStatus_221().equals("0") || msf221Rec.getStatus_221().equals("1") ||
						msf221Rec.getStatus_221().equals("2"))
					{
						update220 = false;
					}
					recordCount221++;
				}
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb220.printinfo("processMSF221 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb220.printinfo ("processMSF221 Error: " + ex.message);
		}
		return result;
	}
	
	/**
	 * list of PO header records.
	 */
	public class poRecord
	{
		String dstrctCode = "";
		String poNo = "";
		String orderDate = "";
		String status_220 = "";
	}
	
	/**
	 * list of PO header & items records.
	 */
	public class poItemRecord
	{
		String dstrctCode = "";
		String poNo = "";
		String poItem = "";
		String status_220 = "";
		String status_221 = "";
		String orderDate = "";
		String stockCode = "";
		BigDecimal curQty = 0;
	}
	/**
	 * process MSF100 & MSF231. <br>
	 **/
	private void getItemDesc (poItemRecord poRec)
	{
		//info ("process getItemDesc");

		try
		{
			itemDesc = "";
			
			Constraint c1 = MSF100Key.stockCode.equalTo(poRec.getStockCode());
			def query = new QueryImpl(MSF100Rec.class).and(c1);
			MSF100Rec msf100Rec = (MSF100Rec) edoi.firstRow(query);
			
			if (msf100Rec && !msf100Rec.getItemName().trim().equals("")){
				itemDesc = msf100Rec.getItemName();
				//info ("item desc 100: " + itemDesc);
			}
			else
			{
				Constraint c2 = MSF231Key.preqNo.equalTo(poRec.getStockCode().substring(0, 6));
				Constraint c3 = MSF231Key.preqItemNo.equalTo(poRec.getStockCode().substring(6, 9));
				Constraint c4 = MSF231Key.dstrctCode.equalTo(poRec.getDstrctCode());
				
				def query1 = new QueryImpl(MSF231Rec.class).and(c2).and(c3).and(c4);
				MSF231Rec msf231Rec = (MSF231Rec) edoi.firstRow(query1);
				
				if (msf231Rec){
					itemDesc = msf231Rec.getItemDescx1();
					//info ("item desc 231: " + itemDesc);
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb220.printinfo("getItemDesc failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb220.printinfo ("getItemDesc Error: " + ex.message);
		}
	}

	/**
	 * update status_220. <br>
	 **/
	private void updateStatus220 (String po, String status221)
	{
		//info ("process updateStatus220");
		
		try
		{
			MSF220Rec msf220Rec = edoi.findByPrimaryKey(new MSF220Key(po))
//			info (" ")
//			info ("before update")
//			info ("msf220 rec: " + msf220Rec.getPrimaryKey() + " - " + msf220Rec.getStatus_220())
			
			msf220Rec.setStatus_220(status221);
//			info ("ready for update")
//			info ("msf220 rec: " + msf220Rec.getStatus_220())
			
			edoi.update(msf220Rec);
			//info ("*** updated ***");
			//info (" ");
			
			//*** FOR TESTING ONLY
//			if (msf220Rec.getPrimaryKey().getPoNo().equals("638997")){
//				edoi.update(msf220Rec);
//				info ("*** update msf220 record - PO no : " + po)
//				info ("************************************")
//			}
			recordCount220Write++;
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb220.printinfo("updateStatus220 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb220.printinfo ("updateStatus220 Error: " + ex.message);
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
	private class Trb220aReportLine implements Comparable<Trb220aReportLine>
	{
		public String poNo;
		public String poItem;
		public String orderDate;
		public String itemDesc;
		public String curQty;
		public String status221;
		public String comment;
		
		public Trb220aReportLine()
		{
		}
		
		//report is sorted via po no
		public int compareTo(Trb220aReportLine otherReportLine)
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
	 * <li> 1. exception report Trb220A </li>
	 **/
	private generateTrb220Report()
	{
		ProcessTrb220.printinfo ("Process generateTrb220Report");
		try
		{
			Trb220aReportLine currLine;
			//String APP = "\"";
			String tempString;
			int idx;
			
			Collections.sort(arrayOfTrb220ReportLine);
			
			//info ("report size: " + arrayOfTrb220ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb220ReportLine.size())
			{
				currLine = arrayOfTrb220ReportLine.get(idx);
				
				Date convDate =  new Date().parse("yyyyMMdd", currLine.orderDate)
				String dispDate = convDate.format("dd/MM/yyyy");
				
				//info ("cur po: " + currLine.poNo);
				//info ("cur po item: " + currLine.poItem);

				tempString = " ";
				tempString = tempString + (currLine.poNo).padRight(8);
				tempString = tempString + (currLine.poItem).padRight(11);
				tempString = tempString + dispDate.padRight(11);
				tempString = tempString + (currLine.itemDesc).padRight(45);
				tempString = tempString + (currLine.curQty).padRight(11);
				if (currLine.status221.equals("0")){
					tempString = tempString + "Unprinted".padRight(13);
				}
				if (currLine.status221.equals("1")){
					tempString = tempString + "Printed".padRight(13);
				}
				if (currLine.status221.equals("2")){
					tempString = tempString + "Modified".padRight(13);
				}
				if (currLine.status221.equals("3")){
					tempString = tempString + "Cancelled".padRight(13);
				}
				if (currLine.status221.equals("9")){
					tempString = tempString + "Complete".padRight(13);
				}
				if (batchParams.paramRunType.equals("U")){
					tempString = tempString + "* PO header status updated".padRight(26);
				}
				DoReportA(tempString);
				
				idx++;
			}
			
			//write records count
			ReportA.writeLine(132,"-");
			DoReportA(" ");
			DoReportA(" ");
			
			tempString = " ";
			tempString = tempString + "No of 220 records read        : ".padLeft(79);
			tempString = tempString + recordCount220.toString().padLeft(10);
			DoReportA(tempString);

			tempString = " ";
			tempString = tempString + "No of 221 records read        : ".padLeft(79);
			tempString = tempString + recordCount221.toString().padLeft(10);
			DoReportA(tempString);

//			tempString = " ";
//			tempString = tempString + "No of 231 records read        : ".padLeft(79);
//			tempString = tempString + recordCount231.toString().padLeft(10);
//			DoReportA(tempString);
			
			tempString = " ";
			tempString = tempString + "No of 220 records exception   : ".padLeft(79);
			tempString = tempString + recordCount220Excpt.toString().padLeft(10);
			DoReportA(tempString);
			
			tempString = " ";
			tempString = tempString + "No of 220 records updated     : ".padLeft(79);
			tempString = tempString + recordCount220Write.toString().padLeft(10);
			DoReportA(tempString);

			DoReportA(" ");

		}
		catch (Exception ex)
		{
			ProcessTrb220.printinfo ("generateTrb220Report Error: " + ex.message);
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
		DoReportA(StringUtils.center("Completed/Cancelled MSF220/MSF221 Report    -" + sMode,132));

		ReportA.writeLine(132,"-");
		tempString = " PO No   Item No    Order Date Item Description                            Order Qty  Status Desc  Comment";
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

		ProcessTrb220.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb220 process = new ProcessTrb220();
process.runBatch(binding);
