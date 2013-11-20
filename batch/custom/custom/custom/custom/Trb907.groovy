/**
 * @AIT 2013
 * Conversion from trb907.cbl for Ellipse 8 upgrade project
 * 18/06/2013 RL - VERSION 1
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
import com.mincom.ellipse.edoi.ejb.msf900.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf000.*;
import com.mincom.ellipse.edoi.ejb.msf080.*;
import com.mincom.ellipse.edoi.ejb.msf222.*;
import com.mincom.ellipse.eroi.linkage.mss001.*;
import com.mincom.ellipse.eroi.linkage.mssdat.*;
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
import javax.sql.DataSource;


////-------------------------------------------------------------------
//import groovy.lang.Binding;
//import com.mincom.ellipse.edoi.common.logger.EDOILogger;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;
//
//public class AITBatch907 implements GroovyInterceptable {
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

public class ParamsTrb907
{
	//List of Input Parameters
	String paramAccountingPeriod;
}

/**
 * ORD and SRD Variance reports <br>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
 * <li> input parameters: Accounting Period - mandatory </li>
 * <li> This program extract and report certain types of transactions from MSF900 for
 * <li> a nominated accounting period. </li>
 * <li> output: 2 reports - TRB907A (ORS Variance) and TRB907B (SRD Variance). </li>
// **/
//public class ProcessTrb907 extends AITBatch907
public class ProcessTrb907 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 1;
	private ParamsTrb907 batchParams;
	
	private BigDecimal chgQtyIssued;
	private BigDecimal accumINP;
	private BigDecimal recpQtyIssued;
	private String changeNo;
	private BigDecimal SOHAfter;
	private BigDecimal qtyRcvd;
	private BigDecimal priceVarLoc;
	private BigDecimal inventChgLoc;

	private ArrayList arrayOfTrb907ReportLine = new ArrayList();
	private ArrayList <tranRecord> listOfTrans = new ArrayList <tranRecord>();
	private DataSource dataSource;
	
	private BigDecimal recordCount900;
	private BigDecimal recordCountORD;
	private BigDecimal recordCountORDWrite;
	private BigDecimal recordCountSRD;
	private BigDecimal recordCountSRDWrite;
	
	Date sTranDateD;
	String sTranDate;
	String sFullPeriod;
	
	//Report A - batch report
	private def ReportA;

	//Report B - batch report
	private def ReportB;

	private DecimalFormat decFormatter = new DecimalFormat("################0.00");
	private DecimalFormat decFormatterNoDec = new DecimalFormat("################0");
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat disDateFormat = new SimpleDateFormat("dd/MM/yyyy");
	
	int ReportALineCounter = 0;
	int ReportBLineCounter = 0;
	
	/**
	 * Run the main batch.
	 * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b)
	{
		init(b);
		dataSource = b.getVariable("dataSource");
		ProcessTrb907.printinfo("VERSION      :" + version);
		ProcessTrb907.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb907.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb907());

		//PrintRequest Parameters
		ProcessTrb907.printinfo("paramAccountingPeriod     : " + batchParams.paramAccountingPeriod);
		
		sTranDateD = new Date();
		sTranDate = sTranDateD.format("yyyyMMdd");

		try
		{
			processBatch();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb907.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
			printBatchReport("B");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb907);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb907.printinfo("processBatch");
		if (initialise_B000())
		{
			processRequest();
			generateTrb907Report();
		}
		ReportA.close();
		ReportB.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private boolean initialise_B000()
	{
		ProcessTrb907.printinfo("initialise_B000");
		
		recordCount900 = 0;
		recordCountORD = 0;
		recordCountORDWrite = 0;
		recordCountSRD = 0;
		recordCountSRDWrite = 0;
		
		ReportA = report.open('TRB907A')
		writeReportHeaderA();
		ReportB = report.open('TRB907B')
		writeReportHeaderB();
		
		//validate mandatory fields
		if (batchParams.paramAccountingPeriod.trim().equals(""))
		{
			WriteError ("Accounting Period is Mandatory");
			return false;
		}
		else
		{
			return true;
		}
	}

	private void WriteError(String msg)
	{
		ReportA.write(" ");
		ReportA.write(msg);
		ReportA.write(" ");
		ReportA.write(" ");
		ProcessTrb907.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process transactions from MSF900 for a nominated accounting period </li>
	 * <li>Write records to TRB907A and/or TRB907B.</li>
	 */
	private void processRequest()
	{
		ProcessTrb907.printinfo("processRequest");
		String prevDstrct;
		BigDecimal srdAmt = 0;
		BigDecimal iadAmt = 0;
		String stockCodeR = "";

		try
		{					
			//retrieve ORD & SRD transaction records
			listOfTrans = getTransactions();
			//info("list of trans size   :" + listOfTrans.size());
			
			//process transaction records
			if (listOfTrans.size() > 0){
				listOfTrans.each {tranRecord rec->
					String sKey = rec.getDstrctCode() + rec.getProcessDate() + rec.getTransactionNo() + rec.getUserNo() + rec.getRec900Type();
					String sPOItem = rec.getPoNo() + rec.getPoItem();
					
//					info("tran key   :" + sKey);
//					info("POItem   :" + sPOItem);
//					info("tran type   :" + rec.tranType);
					
					if (rec.getTranType().equals("ORD")){						
						processMSF222(rec);
						//CHECK CONDITION AGAIN!
						if ((accumINP != 0) && (priceVarLoc < -100 || priceVarLoc > 100)){
							//write to array
							def line = new Trb907aReportLine();
							line.tranType = rec.getTranType();
							line.key900 = sKey;
							line.poItem = sPOItem;
							line.stockCode = rec.getStockCode();
							line.priceVar = rec.getPriceAdj900();
							line.qtyReceived = qtyRcvd;
							line.qtyIssued = recpQtyIssued;
							line.amt1 = rec.getTranAmount900();
							line.amt2 = priceVarLoc;
							line.amt3 = accumINP;
							arrayOfTrb907ReportLine.add(line);
						}
						recordCountORD++;
					}
					if (rec.getTranType().equals("SRD")){
						String x =  rec.getPoNo().substring(0,1);
						//info ("x: " + x)
						if (!x.equals("X")){
							//rounding to 2 decimal
							srdAmt = Math.round((rec.getQtyRcvUOI900() * rec.getNetPriceUOI900()) * 100.0) / 100.0;
//							info ("srdAmt: " + srdAmt)
//							info ("QtyRcvUOI900: " + rec.getQtyRcvUOI900())
//							info ("tranAmount900: " + rec.getTranAmount900())
//							info ("NetPriceUOI900: " + rec.getNetPriceUOI900())
							
							if (srdAmt != rec.getTranAmount900()){
								iadAmt = srdAmt - rec.getTranAmount900();
//								info ("iadAmt: " + iadAmt)
//								info ("Rec900Type: " + rec.getRec900Type())
								
								if (iadAmt < -100 || iadAmt > 100){
									//only display if rec900type = 'R'
									if (!rec.getRec900Type().equals("R")){
										sPOItem = " ";
										stockCodeR = " ";	
									}
									else
									{
										stockCodeR = rec.getStockCode();
									}
									
									def line = new Trb907aReportLine();
									line.tranType = rec.getTranType();
									line.key900 = sKey;
									line.poItem = sPOItem;
									line.stockCode = stockCodeR;
									line.priceVar = 0;
									line.qtyReceived = 0;
									line.qtyIssued = 0;
									line.amt1 = rec.getTranAmount900();
									line.amt2 = srdAmt;
									line.amt3 = iadAmt;
									arrayOfTrb907ReportLine.add(line);
								}								
							}
						}
						recordCountSRD++;
					}
				}
			}
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb907.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb907.printinfo ("processRequest Error: " + ex.message);
		}		
	}

    /**
     * Browse the Transaction records.
     */
    private ArrayList<tranRecord> getTransactions() {
		ProcessTrb907.printinfo("getTransactions");
		List <tranRecord> result = new ArrayList <tranRecord>()
		
		
		try
		{
				
			sFullPeriod = "20" + batchParams.paramAccountingPeriod.substring(2,4) +  batchParams.paramAccountingPeriod.substring(0,2);
			//info ("sfullperiod : " + sFullPeriod);
			
			//browse through MSF900
			def sql = new Sql(dataSource)
			sql.eachRow(qryString(), {
//				info ("it: " + it);
				
				if (it.tran_type.equals("ORD") || it.tran_type.equals("SRD")){	
									
					def rec = new tranRecord()					
					rec.setDstrctCode(it.dstrct_code)
					rec.setProcessDate(it.process_date)
					rec.setTransactionNo(it.transaction_no)
					rec.setUserNo(it.userno)
					rec.setRec900Type(it.rec900_type)
					rec.setTranAmount900(it.tran_amount)
					rec.setTranType(it.tran_type)
					rec.setPoNo(it.po_no)
					rec.setPoItem(it.po_item)
					rec.setStockCode(it.stock_code)
					rec.setPriceAdj900(it.net_pr_adj_i)
					rec.setQtyRcvUOI900(it.qty_rcv_UOI)
					rec.setNetPriceUOI900(it.net_pr_UOI)
					rec.setTranAmount900(it.tran_amount)
					rec.setCreationDate(it.creation_date)
					rec.setCreationTime(it.creation_time)
					result.add(rec)
					recordCount900++;
//					info ("rec900: " + 
//						"tran type: " + rec.getTranType() + " " + 
//						"dstrct: " + rec.getDstrctCode() + " " +
//						"po no: " +  rec.getPoNo() +  " " +
//						"po item: " + rec.getPoItem() + " " + 
//						"tran amt: " + rec.getTranAmount900() + " " + 
//						"price adj: " + rec.getPriceAdj900() + " " +
//						"qty rcvd uoi: " + rec.getQtyRcvUOI900() + " " + 
//						"net price uoi: " + rec.getNetPriceUOI900() + " "); 
//						"changed value: " + rec.getChgValue() + " " +
//						"changed price var: " + rec.getChgPriceVar() + " " + 
//						"changed qty rcvd: " + rec.getChgQtyRcvd());
				}
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb907.printinfo("getTransactions failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb907.printinfo ("getTransactions Error: " + ex.message);
		}
		return result;		
	}
		
	// Create SQL statement
	private String qryString() {
		info("setQryString")
		StringBuffer qry = new StringBuffer()
		qry.append("select ")
		qry.append("a.dstrct_code, ")
		qry.append("a.process_date, ")
		qry.append("a.transaction_no, ")
		qry.append("a.userno, ")
		qry.append("a.rec900_type, ")
		qry.append("a.tran_type, ")
		qry.append("a.tran_amount, ")
		qry.append("a.stock_code, ")
		qry.append("a.net_pr_adj_i, ")
		qry.append("a.qty_rcv_UOI, ")
		qry.append("a.net_pr_UOI, ")
		qry.append("a.po_no, ")
		qry.append("a.po_item, ")
		qry.append("a.creation_date, ")
		qry.append("a.creation_time ")
		qry.append("from msf900 a ")
		qry.append("where a.full_period = '${sFullPeriod}' ")
		qry.append("and (a.tran_type = 'ORD' or a.tran_type = 'SRD') ")
		qry.append("order by ")
		qry.append("a.tran_type, a.dstrct_code, a.process_date, a.transaction_no, a.userno, a.rec900_type")
		info(qry.toString())
		return qry.toString()
	}

	/**
	 * list of ORD and SRD transaction records.
	 */
	public class tranRecord
	{
		String dstrctCode = "";
		String processDate = "";
		String transactionNo = "";
		String userNo = "";
		String rec900Type = "";
		String poNo = "";
		String poItem = "";
		String stockCode = "";
		String tranType = "";
		String changeNo = "";
		BigDecimal priceAdj900 = 0;
		BigDecimal qtyRcvUOI900 = 0;
		BigDecimal netPriceUOI900 = 0;
		BigDecimal tranAmount900 = 0;
		BigDecimal chgSOHAfter = 0;
		BigDecimal chgValue = 0;
		BigDecimal chgPriceVar = 0;
		BigDecimal chgQtyRcvd = 0;	
		String creationDate = "";
		String creationTime = "";
	}

	/**
	 * process MSF222. <br>
	 **/
	private void processMSF222 (tranRecord tRec)
	{
		//info ("process MSF222");
		
		try
		{
			changeNo = "";
			SOHAfter = 0;
			qtyRcvd = 0;
			priceVarLoc = 0;
			inventChgLoc = 0;
			
			Constraint c1 = MSF222Key.poNo.equalTo(tRec.getPoNo());
			Constraint c2 = MSF222Key.poItem.equalTo(tRec.getPoItem());
			Constraint c3 = MSF222Rec.poHistType.equalTo("1");
			Constraint c4 = MSF222Rec.dstrctCode.equalTo(tRec.getDstrctCode());
			Constraint c5 = MSF222Rec.creationDate.equalTo(tRec.getCreationDate());
			Constraint c6 = MSF222Rec.creationTime.lessThanEqualTo(tRec.getCreationTime());
			
			def query = new QueryImpl(MSF222Rec.class).and(c1).and(c2).and(c3).and(c4).and(c5).and(c6);
			
			MSF222Rec msf222Rec = (MSF222Rec) edoi.firstRow(query);
			if (msf222Rec){

				changeNo = msf222Rec.getPrimaryKey().getChangeNo();
				SOHAfter = msf222Rec.getSohAfter();
				qtyRcvd = msf222Rec.getQtyRcvd();
				priceVarLoc = msf222Rec.getPriceVarLoc();
				inventChgLoc = msf222Rec.getInventChgLoc();
				
				calcMSF222(tRec);
			}			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb907.printinfo("processMSF222 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb907.printinfo ("processMSF222 Error: " + ex.message);
		}
	}
	
	/**
	 * calculate PO values. <br>
	 **/
	private void calcMSF222 (tranRecord tRec)
	{
		//info ("process calcMSF222");
		try
		{
			accumINP = 0;
			chgQtyIssued = 0;
			recpQtyIssued = 0;
			
			Constraint c1 = MSF222Key.poNo.equalTo(tRec.getPoNo());
			Constraint c2 = MSF222Key.poItem.equalTo(tRec.getPoItem());
			Constraint c3 = MSF222Rec.poHistType.equalTo("0");
			Constraint c4 = MSF222Key.changeNo.greaterThanEqualTo(changeNo);
			
			def query = new QueryImpl(MSF222Rec.class).and(c1).and(c2).and(c3).and(c4);
			
			MSF222Rec msf222Rec = (MSF222Rec) edoi.firstRow(query);
			if (msf222Rec){
				
				//info ("soh after: " + msf222Rec.getSohAfter());
				//info ("changed soh after: " + tRec.getChgSOHAfter());
				//info ("changed qty rcvd: " + tRec.getChgQtyRcvd());
				
				chgQtyIssued = msf222Rec.getSohAfter() - SOHAfter;
			
				if (chgQtyIssued < 0){
					recpQtyIssued = 0;
				}
				else
				{
					if (chgQtyIssued > qtyRcvd){
						recpQtyIssued = qtyRcvd;
					}
				}
				accumINP = -1 * (tRec.getTranAmount900() + priceVarLoc)	
				
				//info ("recpQtyIssued: " + recpQtyIssued);
				//info ("accumINP: " + accumINP);
			}				
			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb907.printinfo("calcMSF222 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb907.printinfo ("calcMSF222 Error: " + ex.message);
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
	private class Trb907aReportLine implements Comparable<Trb907aReportLine>
	{
		public String tranType;
		public String key900;
		public String poItem;
		public String stockCode;
		public BigDecimal priceVar;
		public BigDecimal qtyReceived;
		public BigDecimal qtyIssued;
		public BigDecimal amt1;
		public BigDecimal amt2;
		public BigDecimal amt3;
		
		public Trb907aReportLine()
		{
		}
		
		//report is sorted via tran type, key900 & po
		public int compareTo(Trb907aReportLine otherReportLine)
		{
			if (!tranType.equals(otherReportLine.tranType))
			{
				return tranType.compareTo(otherReportLine.tranType)
			}
			if (!key900.equals(otherReportLine.key900))
			{
				return key900.compareTo(otherReportLine.key900)
			}
			if (!poItem.equals(otherReportLine.poItem))
			{
				return poItem.compareTo(otherReportLine.poItem)
			}
			return 0;
		}				
	}
	
	/**
	 * generate batch reports <br>
	 * <li> 1. Output ORD transaction records to TRB907A and SRD transaction records to TRB907B </li>
	 **/
	private generateTrb907Report()
	{
		ProcessTrb907.printinfo ("Process generateTrb907Report");
		try
		{
			Trb907aReportLine currLine;
			String APP = "\"";
			String tempString;
			String prvDstrctA = " ";
			String prvDstrctB = " ";
			String curDstrct = " ";
			
			BigDecimal ordTotal = 0;
			BigDecimal ordIADTotal = 0;
			BigDecimal ordINPTotal = 0;
			BigDecimal srdAmt1Total = 0;
			BigDecimal srdAmt2Total = 0;
			BigDecimal srdAmt3Total = 0;
			BigDecimal ordDstrctTotal = 0;
			BigDecimal ordIADDstrctTotal = 0;
			BigDecimal ordINPDstrctTotal = 0;
			BigDecimal srdAmt1DstrctTotal = 0;
			BigDecimal srdAmt2DstrctTotal = 0;
			BigDecimal srdAmt3DstrctTotal = 0;
			
			int idx;
			
			Collections.sort(arrayOfTrb907ReportLine);
			
			//info ("report size: " + arrayOfTrb907ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb907ReportLine.size())
			{
				currLine = arrayOfTrb907ReportLine.get(idx);
				curDstrct = currLine.key900.substring(0,4);
				
				//info ("curDstrct: " + curDstrct);
				//info ("prvDstrctA: " + prvDstrctA);
				//info ("curline tran type: " + currLine.tranType);
				
				if (currLine.tranType.equals("ORD")){
					tempString = " ";
					tempString = tempString + (currLine.key900).padRight(30);
					tempString = tempString + (currLine.poItem).padRight(10);
					tempString = tempString + (currLine.stockCode).padRight(10);

					tempString = tempString + decFormatter.format(currLine.priceVar).padLeft(12);
					tempString = tempString + decFormatter.format(currLine.qtyReceived).padLeft(8);
					tempString = tempString + decFormatter.format(currLine.qtyIssued).padLeft(12);
					tempString = tempString + decFormatter.format(currLine.amt1).padLeft(13);
					tempString = tempString + decFormatter.format(currLine.amt2).padLeft(17);
					tempString = tempString + decFormatter.format(currLine.amt3).padLeft(15);
					
					//info ("tempString line:  " + tempString);
					
					DoReportA(tempString);
					recordCountORDWrite++;
					
					if (!prvDstrctA.equals(curDstrct) && !prvDstrctA.trim().equals("")){
						ReportA.writeLine(132,"-");						
						tempString = " ";
						tempString = tempString + "District".padLeft(79);
						tempString = tempString + decFormatter.format(ordDstrctTotal).padLeft(16);
						tempString = tempString + decFormatter.format(ordIADDstrctTotal).padLeft(17);
						tempString = tempString + decFormatter.format(ordINPDstrctTotal).padLeft(15);
						DoReportA(tempString);						
						ReportA.writeLine(132,"-");
						DoReportA(" ");
						
						prvDstrctA = curDstrct;
						ordDstrctTotal = 0;
						ordIADDstrctTotal = 0;
						ordINPDstrctTotal = 0;
					}

					ordDstrctTotal = ordDstrctTotal + currLine.amt1;
					ordIADDstrctTotal = ordIADDstrctTotal + currLine.amt2;
					ordINPDstrctTotal = ordINPDstrctTotal + currLine.amt3;
					
					ordTotal = ordTotal + currLine.amt1;
					ordIADTotal = ordIADTotal + currLine.amt2;
					ordINPTotal = ordINPTotal + currLine.amt3;	
				}

				if (currLine.tranType.equals("SRD")){
					tempString = " ";
					tempString = tempString + (currLine.key900).padRight(30);
					tempString = tempString + (currLine.poItem).padRight(11);
					tempString = tempString + (currLine.stockCode).padRight(11);
					tempString = tempString + decFormatter.format(currLine.amt1).padLeft(14);
					tempString = tempString + decFormatter.format(currLine.amt2).padLeft(25);
					tempString = tempString + decFormatter.format(currLine.amt3).padLeft(28);
					
					DoReportB(tempString);
					recordCountSRDWrite++;
					
					if (!prvDstrctB.equals(curDstrct) && !prvDstrctB.trim().equals("")){						
						ReportB.writeLine(132,"-");						
						tempString = " ";
						tempString = tempString + "District".padLeft(52);
						tempString = tempString + decFormatter.format(srdAmt1DstrctTotal).padLeft(14);
						tempString = tempString + decFormatter.format(srdAmt2DstrctTotal).padLeft(25);
						tempString = tempString + decFormatter.format(srdAmt3DstrctTotal).padLeft(28);
						DoReportB(tempString);						
						ReportB.writeLine(132,"-");
						DoReportB(" ");
						
						prvDstrctB = curDstrct;
						srdAmt1DstrctTotal = 0;
						srdAmt2DstrctTotal = 0;
						srdAmt3DstrctTotal = 0;
					}

					srdAmt1DstrctTotal = srdAmt1DstrctTotal + currLine.amt1;
					srdAmt2DstrctTotal = srdAmt2DstrctTotal + currLine.amt2;
					srdAmt3DstrctTotal = srdAmt3DstrctTotal + currLine.amt3;
					
					srdAmt1Total = srdAmt1Total + currLine.amt1;
					srdAmt2Total = srdAmt2Total + currLine.amt2;
					srdAmt3Total = srdAmt3Total + currLine.amt3;
				}
				idx++;
			}
			
			//*** TOTALS FOR REPORT A
			if (recordCountORDWrite > 0){
				//write the last district total for report A
				ReportA.writeLine(132,"-");				
				tempString = " ";
				tempString = tempString + "District".padLeft(79);
				tempString = tempString + decFormatter.format(ordDstrctTotal).padLeft(16);
				tempString = tempString + decFormatter.format(ordIADDstrctTotal).padLeft(17);
				tempString = tempString + decFormatter.format(ordINPDstrctTotal).padLeft(15);	
				DoReportA(tempString);				
				ReportA.writeLine(132,"-");
				DoReportA(" ");
				DoReportA(" ");
				
				//write totals for report A
				tempString = " ";
				tempString = tempString + "Report".padLeft(79);
				tempString = tempString + decFormatter.format(ordTotal).padLeft(16);
				tempString = tempString + decFormatter.format(ordIADTotal).padLeft(17);
				tempString = tempString + decFormatter.format(ordINPTotal).padLeft(15);				
				DoReportA(tempString);				
				ReportA.writeLine(132,"-");				

				//write records count
				DoReportA(" ");
				DoReportA(" ");
				
				tempString = " ";
				tempString = tempString + "No of 900 records read    : ".padLeft(54);
				tempString = tempString + recordCount900.toString().padLeft(10);
				DoReportA(tempString);
				
				tempString = " ";
				tempString = tempString + "No of ORD records found   : ".padLeft(54);
				tempString = tempString + recordCountORD.toString().padLeft(10);
				DoReportA(tempString);
				
				tempString = " ";
				tempString = tempString + "No of ORD records written : ".padLeft(54);
				tempString = tempString + recordCountORDWrite.toString().padLeft(10);
				DoReportA(tempString);

				DoReportA(" ");
			}
			//***
			//*** TOTALS FOR REPORT B
			if (recordCountSRDWrite++ > 0){
				//write the last district total for report B
				ReportB.writeLine(132,"-");				
				tempString = " ";
				tempString = tempString + "District".padLeft(52);
				tempString = tempString + decFormatter.format(srdAmt1DstrctTotal).padLeft(14);
				tempString = tempString + decFormatter.format(srdAmt2DstrctTotal).padLeft(25);
				tempString = tempString + decFormatter.format(srdAmt3DstrctTotal).padLeft(28);	
				DoReportB(tempString);				
				ReportB.writeLine(132,"-");
				DoReportB(" ");
				DoReportB(" ");
				
				//write totals for report B
				tempString = " ";
				tempString = tempString + "Report".padLeft(52);
				tempString = tempString + decFormatter.format(srdAmt1Total).padLeft(14);
				tempString = tempString + decFormatter.format(srdAmt2Total).padLeft(25);
				tempString = tempString + decFormatter.format(srdAmt3Total).padLeft(28);				
				DoReportB(tempString);				
				ReportB.writeLine(132,"-");

				//write records count
				DoReportB(" ");
				DoReportB(" ");		
						
				tempString = " ";
				tempString = tempString + "No of 900 records read    : ".padLeft(54);
				tempString = tempString + recordCount900.toString().padLeft(10);
				DoReportB(tempString);
				
				tempString = " ";
				tempString = tempString + "No of SRD records found   : ".padLeft(54);
				tempString = tempString + recordCountSRD.toString().padLeft(10);
				DoReportB(tempString);
				
				tempString = " ";
				tempString = tempString + "No of SRD records written : ".padLeft(54);
				tempString = tempString + recordCountSRDWrite.toString().padLeft(10);
				DoReportB(tempString);

				DoReportB(" ");			
			}
		}
		catch (Exception ex)
		{
			ProcessTrb907.printinfo ("generateTrb907Report Error: " + ex.message);
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
		DoReportA(StringUtils.center("MSF900 - ORD Variances Report", 132));
		
		tempString = "For the Accounting Period of " + batchParams.paramAccountingPeriod.substring(2,4) + batchParams.paramAccountingPeriod.substring(0,2);
		DoReportA(StringUtils.center(tempString,132));
		ReportA.writeLine(132,"-");

		tempString = " MSF900 Key                  PO No/Item  StockCode      Price     Received    Issued    ORD-Amount      IAD-Issued      INP-Issued";
		DoReportA(tempString);
		
		ReportA.writeLine(132,"-");
	}


	private void DoReportA(String line)
	{
		//info("Report A: " + line);
		ReportA.write(line);
	}
	
	private void writeReportHeaderB()
	{
		//info("writeReportHeaderB");
		String tempString;
		
		ReportB.writeLine(132,"-");		
		DoReportB(StringUtils.center("MSF900 - SRD Variances Report", 132));
		
		tempString = "For the Accounting Period of " + batchParams.paramAccountingPeriod.substring(2,4) + batchParams.paramAccountingPeriod.substring(0,2);
		DoReportB(StringUtils.center(tempString,132));
		ReportB.writeLine(132,"-");
		
		tempString = " MSF900 Key                   PO No/Item  StockCode       MSF900 Amount           Correct Amount                IAD Value";
		DoReportB(tempString);
		
		ReportB.writeLine(132,"-");
	}
		
	private void DoReportB(String line)
	{
		//info("Report B: " + line);
		ReportB.write(line);
	}
	
	/**
	 * print batch report <br>
	 * <li> output total record counts </li>
	 **/
	private void printBatchReport(String rep)
	{

		ProcessTrb907.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of 900 records read : "+recordCount900.toString());
//			println("No of ORD records found : "+recordCountORD.toString());
//			println("No of ORD records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb907 process = new ProcessTrb907();
process.runBatch(binding);
