/**
 * @AIT 2013
 * 10/10/2013 IG - Version 6
 *                 Modified for report to list both (W)ritten and (E)xception records now.
 * 16/07/2013 RL - changed in Ellipse 8
 *                 the report is now compared mnemonic to Gov Id No (Tax_File_No)
 *                 rather than Tax Reg No
 *               - changed report title to "Supplier ABN exception report"
 *               - changed program version to 2
 * Conversion from Trb203.cbl for Ellipse 8 upgrade project
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
import com.mincom.ellipse.edoi.ejb.msf200.*;
import com.mincom.ellipse.edoi.ejb.msf203.*;
import com.mincom.ellipse.edoi.ejb.msf120.*;
import com.mincom.ellipse.edoi.ejb.msf000.*;
import com.mincom.ellipse.edoi.ejb.msf080.*;
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

import javax.sql.CommonDataSource;
import javax.sql.DataSource;


//-------------------------------------------------------------------
import groovy.lang.Binding;
import com.mincom.ellipse.edoi.common.logger.EDOILogger;
import com.mincom.ria.xml.bind.BigDecimalBinder;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;

public class AITBatch203 implements GroovyInterceptable {

	private static final long REFRESH_TIME = 60 * 1000 * 5

	public EDOIWrapper edoi
	public EROIWrapper eroi
	public ServiceWrapper service
	public BatchWrapper batch;
	public CommAreaScriptWrapper commarea;
	public BatchEnvironment env
	public UnixTools tools
	public Reports report;
	public Sort sort;
	public Params params;
	public RequestInterface request;
	public Restart restart;
	

	private String uuid;
	private String taskUuid;

	private Date lastDate;

	private boolean disableInvokeMethod

	public static final int SuperBatch_VERSION = 6;
	public static final String SuperBatch_CUST = "TRAN1";

	/**
	 * Print a string into the logger.
	 * @param value a string to be printed.
	 */
	public void info(String value){
		def logObject = LoggerFactory.getLogger(getClass());
		logObject.info("------------- " + value)
	}
	
	public void debug(String value){
		def logObject = LoggerFactory.getLogger(getClass());
		logObject.debug("------------- " + value)
	}

	/**
	 * Initialize the variables based on binding object.
	 * @param b binding object
	 */
	public void init(Binding b) {
		edoi = b.getVariable("edoi");
		eroi = b.getVariable("eroi");
		service = b.getVariable("service");
		batch = b.getVariable("batch");
		commarea = b.getVariable("commarea");
		env = b.getVariable("env");
		tools = b.getVariable("tools");
		report = b.getVariable("report");
		sort = b.getVariable("sort");
		request = b.getVariable("request");
		restart = b.getVariable("restart");
		params = b.getVariable("params");

		// gets the uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
		uuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.getUUID();

		// gets the task uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
		taskUuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.request.getTaskUuid();
		
	}

	/**
	 *  Returns the uuid
	 * @return String UUID
	 */
	public String getUUID() {
		return uuid
	}

	/**
	 *  Returns the task uuid from the parent
	 * @return String UUID
	 */
	public String getTaskUUID() {
		return taskUuid
	}

	/**
	 * Print the version.
	 */
	public void printSuperBatchVersion(){
		info ("SuperBatch Version:" + SuperBatch_VERSION);
		info ("SuperBatch Customer:" + SuperBatch_CUST);
	}

	def invokeMethod(String name, args) {
		if (!disableInvokeMethod) {
			disableInvokeMethod = true;
			try {
				keepAliveConnection();
			} finally {
				disableInvokeMethod = false;
			}
		}
		def result
		def metaMethod = metaClass.getMetaMethod(name, args)
		result = metaMethod.invoke(this, metaMethod.coerceArgumentsToClasses(args))
		return result
	}

	protected void keepAliveConnection() {
		if (lastDate == null) {
			lastDate = new Date();
		} else {
			Date currentDate = new Date();
			debug("Time elapsed  = " + (currentDate.getTime() - lastDate.getTime()))
			debug("Time refresh  = " + REFRESH_TIME)
			if ((currentDate.getTime() - lastDate.getTime()) > REFRESH_TIME ) {
				lastDate = currentDate;
				restartTransaction();
			}
		}
	}

	protected void restartTransaction() {
		debug("restartTransaction")
		(0..0).each restart.each(1, { debug("Restart Transaction") })
		debug("end restart transaction")
	}
}
//-------------------------------------------------------------------

public class ParamsTrb203
{
	//List of Input Parameters
	//String paramAccountingPeriod;
}

/**
 * Create name reference for supplier's tax file no <br>
 * <li> This program creates a colloquial name reference record (MSF120)for tax file no </li>
 * <li> in supplier business info table (MSF203) </li> 
 * <li> MSF120 record to be created only if the total colloquial record for </li>
 * <li> a supplier is less than 6 otherwise, the Tax file No will be printed </li>
 * <li> in an exception report. </li>
 * <li> output: 1 exception report - Trb203A. </li>
 * <li> Upgrade from an existing report as part of the Ellipse 8 upgrade project. </li>
// **/
public class ProcessTrb203 extends AITBatch203
//public class ProcessTrb203 extends SuperBatch
{
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 2;
	private ParamsTrb203 batchParams;
	
	private int cnt120 = 0;
	
	private ArrayList dstrctList = new ArrayList();
	private ArrayList arrayOfTrb203ReportLine = new ArrayList();
	
	private BigDecimal recordCount200;
	private BigDecimal recordCount203;
	private BigDecimal recordCount120;
	private BigDecimal recordCount120Excpt;
	private BigDecimal recordCount120Write;
	
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
		ProcessTrb203.printinfo("VERSION      :" + version);
		ProcessTrb203.printinfo("uuid         :" + getUUID());
		
		printSuperBatchVersion();
		
		ProcessTrb203.printinfo("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrb203());

		//PrintRequest Parameters
		//ProcessTrb203.printinfo("paramAccountingPeriod     : " + batchParams.paramAccountingPeriod);
		ProcessTrb203.printinfo(" ");

		try
		{
			processBatch();			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ProcessTrb203.printinfo("processBatch failed - ${e.getMessage()}");
		}
		finally
		{
			printBatchReport("A");
		}
	}
	
	public static void printinfo(String value)
	{
		def logObject = LoggerFactory.getLogger(ProcessTrb203);
		logObject.info("------------- " + value)
	}

	
	/**
	 * Process the main batch.
	 */
	private void processBatch()
	{
		ProcessTrb203.printinfo("processBatch");

		initialise_B000();
		processRequest();
		generateTrb203Report();

		ReportA.close();
	}
	
	/**
	 * Initialize the working directory, output file, and other variables.
	 */
	private void initialise_B000()
	{
		ProcessTrb203.printinfo("initialise_B000");
		
		recordCount200 = 0;
		recordCount203 = 0;
		recordCount120 = 0;
		recordCount120Write = 0;
		recordCount120Excpt = 0;
		
		ReportA = report.open('Trb203A')
		writeReportHeaderA();
	}

	private void WriteError(String msg)
	{
		ReportA.write(" ");
		ReportA.write(msg);
		ReportA.write(" ");
		ReportA.write(" ");
		ProcessTrb203.printinfo ("Error: " + msg);
	}
	/**
	 * Process request. <br>
	 * <li>process records from MSF200 </li>
	 */
	private void processRequest()
	{
		ProcessTrb203.printinfo("processRequest");
		int cnt;
		String dstCode = " ";
		
		try
		{	
			dstrctList = new ArrayList ();
			//get all active districts
			Constraint c1 = MSF000_ADKey.dstrctCode.greaterThanEqualTo(" ");
			Constraint c2 = MSF000_ADKey.controlRecType.greaterThanEqualTo("AD");
			Constraint c3 = MSF000_ADRec.dstrctStatus.equalTo("A");
			def query1 = new QueryImpl(MSF000_ADRec.class).and(c1).and(c2).and(c3);
			
			edoi.search(query1,100,{MSF000_ADRec msf000Rec ->
				if (msf000Rec){
					String dCode = " ";
					dCode = msf000Rec.getPrimaryKey().getControlRecNo();
					dstrctList.add(dCode);					
					//info ("dCode: " + dCode);
				}
			})
			
			//process supplier records
			Constraint c4 = MSF200Key.supplierNo.greaterThanEqualTo(" ")			
			def query2 = new QueryImpl(MSF200Rec.class).and(c4);

			edoi.search(query2,10000,{MSF200Rec msf200Rec ->
				//info ("supplier in 200: " + msf200Rec.getPrimaryKey().getSupplierNo());
				if (msf200Rec){
					cnt = 0;
					while (cnt < dstrctList.size()){
						dstCode = dstrctList.get(cnt);
						processMSF203 (msf200Rec.getPrimaryKey().getSupplierNo(), dstCode)
						cnt++;
					}
					recordCount200++;
				}
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb203.printinfo("processRequest failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb203.printinfo ("processRequest Error: " + ex.message);
		}		
	}

	/**
	 * process MSF203. <br>
	 **/
	private void processMSF203 (String supNo200, String dCode)
	{
		//info ("processMSF203");

		try
		{					
			Constraint c1 = MSF203Key.supplierNo.equalTo(supNo200);
			Constraint c2 = MSF203Key.dstrctCode.equalTo(dCode);
			
			def query = new QueryImpl(MSF203Rec.class).and(c1).and(c2);				
			MSF203Rec msf203Rec = (MSF203Rec) edoi.firstRow(query);

			//info ("tax file no in 203: " + msf203Rec.getTaxFileNo());
			
			if (msf203Rec){	
				if (!msf203Rec.getTaxFileNo().trim().equals("")){
					if (!checkMSF120(supNo200, msf203Rec.getTaxFileNo())){
						//info ("cnt120 :" + cnt120);
						
						if (cnt120 == 6){
							//info ("write exception");
							//write to array for exception report
							def line = new Trb203aReportLine();
							line.supplierNo = supNo200;
							line.taxFileNo = msf203Rec.getTaxFileNo();
							line.lineType = "E";
							arrayOfTrb203ReportLine.add(line);
							recordCount120Excpt++;
						}
						else {
							if (!msf203Rec.getTaxFileNo().trim().equals("")){
								createMSF120(supNo200, msf203Rec.getTaxFileNo());

							}
						}
					}
					recordCount203++;
				}						
			}			
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb203.printinfo("processMSF203 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb203.printinfo ("processMSF203 Error: " + ex.message);
		}
	}

	/**
	 * process MSF120. <br>
	 **/
	private boolean checkMSF120 (String supNo200, String taxFileNo)
	{
		//info ("process checkMSF120");
		String dCode = "";
		Boolean result = false;
		try
		{
			cnt120 = 0;
			Constraint c1 = MSF120AIX1.colloqType.equalTo("V");
			Constraint c2 = MSF120AIX1.colloqCode.equalTo(supNo200);
			Constraint c3 = MSF120AIX1.colloqName.greaterThan(" ");
			
			def query = new QueryImpl(MSF120Rec.class).and(c1).and(c2).and(c3);

//			info ("supNo200: " + supNo200);
//			info ("taxFileNo: " + taxFileNo);
			edoi.search(query,10,{MSF120Rec msf120Rec ->
				if (msf120Rec && cnt120 <= 6){
					//info ("colloq name in 120: " + msf120Rec.getPrimaryKey().getColloqName());
					cnt120++;
					if (msf120Rec.getPrimaryKey().getColloqName().trim().equals(taxFileNo.trim())){
						recordCount120++;
						result = true;
					}
				}	
			})
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb203.printinfo("checkMSF120 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb203.printinfo ("checkMSF120 Error: " + ex.message);
		}
		return result;
	}

	/**
	 * create colloquial name in MSF120. <br>
	 **/
	private void createMSF120 (String supNo200, String taxFileNo)
	{
		//info ("process createMSF120");
		
		try
		{	
			MSF120Rec msf120Rec = new MSF120Rec();
			MSF120Key msf120Key = new MSF120Key();
			
			msf120Key.setColloqCode(supNo200);
			msf120Key.setColloqName(taxFileNo);
			msf120Key.setColloqType("V");
			
			msf120Rec.setPrimaryKey(msf120Key);
			edoi.create(msf120Rec);
//			info ("write msf120 record")
//			info ("msf120 key: " + msf120Key);
//			info ("msf120 rec: " + msf120Rec.getPrimaryKey() + " " + msf120Rec.getLastModDate() + " " + msf120Rec.getLastModTime() + " " + msf120Rec.getLastModUser())

			//write to array for exception report
			def line = new Trb203aReportLine();
			line.supplierNo = supNo200;
			line.taxFileNo = taxFileNo;
			line.lineType = "W";
			arrayOfTrb203ReportLine.add(line);
			
			recordCount120Write++;
		}
		catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
		{
			e.printStackTrace();
			ProcessTrb203.printinfo("createMSF120 failed - ${e.getMessage()}");
		}
		catch (Exception ex)
		{
			ProcessTrb203.printinfo ("createMSF120 Error: " + ex.message);
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
	private class Trb203aReportLine implements Comparable<Trb203aReportLine>
	{
		public String supplierNo;
		public String taxFileNo;
		public String lineType;
		
		public Trb203aReportLine()
		{
		}
		
		//report is sorted via supplier no
		public int compareTo(Trb203aReportLine otherReportLine)
		{
			if (!supplierNo.equals(otherReportLine.supplierNo))
			{
				return supplierNo.compareTo(otherReportLine.supplierNo)
			}
			return 0;
		}				
	}
	
	/**
	 * generate batch report <br>
	 * <li> 1. exception report Trb203A </li>
	 **/
	private generateTrb203Report()
	{
		ProcessTrb203.printinfo ("Process generateTrb203Report");
		try
		{
			Trb203aReportLine currLine;
			//String APP = "\"";
			String tempString;
			
			int idx;
			
			Collections.sort(arrayOfTrb203ReportLine);
			
			//info ("report size: " + arrayOfTrb203ReportLine.size());
			
			//loop through the array list and output to report line
			idx = 0;
			while(idx < arrayOfTrb203ReportLine.size())
			{
				currLine = arrayOfTrb203ReportLine.get(idx);
				
				//info ("cur supplier: " + currLine.supplierNo);
				//info ("cur tax file no: " + currLine.taxFileNo);

				tempString = " ";
				tempString = tempString + (currLine.supplierNo).padRight(28);
				tempString = tempString + (currLine.taxFileNo).padRight(39);
				tempString = tempString + (currLine.lineType).padRight(9);
				DoReportA(tempString);
				
				idx++;
			}
			
			//write records count
			ReportA.writeLine(80,"-");
			DoReportA(" ");
			DoReportA(" ");
			
			tempString = " ";
			tempString = tempString + "No of 200 records read        :   ".padLeft(34);
			tempString = tempString + recordCount200.toString().padLeft(10);
			DoReportA(tempString);

			tempString = " ";
			tempString = tempString + "No of 203 records read        :   ".padLeft(34);
			tempString = tempString + recordCount203.toString().padLeft(10);
			DoReportA(tempString);

			tempString = " ";
			tempString = tempString + "No of 120 records read        :   ".padLeft(34);
			tempString = tempString + recordCount120.toString().padLeft(10);
			DoReportA(tempString);
			
			tempString = " ";
			tempString = tempString + "No of 120 records exception   :   ".padLeft(34);
			tempString = tempString + recordCount120Excpt.toString().padLeft(10);
			DoReportA(tempString);
			
			tempString = " ";
			tempString = tempString + "No of 120 records written     :   ".padLeft(34);
			tempString = tempString + recordCount120Write.toString().padLeft(10);
			DoReportA(tempString);

			DoReportA(" ");

		}
		catch (Exception ex)
		{
			ProcessTrb203.printinfo ("generateTrb203Report Error: " + ex.message);
		}		
	}

	/**
	 * write batch report header <br>
	 **/
	private void writeReportHeaderA()
	{
		//info("writeReportHeaderA");
		String tempString;
		
		ReportA.writeLine(80,"-");		
		DoReportA(StringUtils.center("SUPPLIER ABN EXCEPTION REPORT", 80));

		ReportA.writeLine(80,"-");
		tempString = " Supplier No                 Government Id No            (E)xception/(W)ritten";
		DoReportA(tempString);
		
		ReportA.writeLine(80,"-");
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

		ProcessTrb203.printinfo("printBatchReport");
		//print batch report
//			println ("\n");
//			println("No of 900 records read : "+recordCount900.toString());
//			println("No of ORD records found : "+recordCountORD.toString());
//			println("No of ORD records written : "+recordCountORDWrite.toString());
//			println(StringUtils.center("End of Report ", 132));

	}
}


/*run script*/
ProcessTrb203 process = new ProcessTrb203();
process.runBatch(binding);
