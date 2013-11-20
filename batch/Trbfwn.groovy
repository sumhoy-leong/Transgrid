/**
 * @AIT 2012
 * Conversion from trbfwn.cbl
 * 03/04/2013 RL - VERSION 1
 * 17/04/2013 RL - VERSION 2
 * 01/05/2013 RL - VERSION 3
 * 24/05/2013 RL - VERSION 4 Removed repeated columns in csv 
 * 07/06/2013 RL - VERSION 5 Allow entry of P and C into employee id params
 *                           Add P and C in front of emp id on output
 * 20/06/2013 RL - VERSION 6 fixed employee's status issue in checkStaffStatus                        
 * 21/06/2013 RL - VERSION 7 Added PDF                        
 *
 * 
 */
package com.mincom.ellipse.script.custom

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import com.lowagie.text.Image;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase


//import com.mincom.eilib.EllipseEnvironment;
//import com.mincom.batch.RequestDefinition;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.rules.java.JavaRuleSet;
import com.mincom.ellipse.script.util.*;
import com.mincom.ellipse.types.base.instances.Bool;
import com.mincom.ellipse.types.m0000.instances.InpChar;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.ellipse.common.unix.*;

import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf200.*;
import com.mincom.ellipse.edoi.ejb.msf500.*;
import com.mincom.ellipse.edoi.ejb.msf766.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf808.MSF808Key;
import com.mincom.ellipse.edoi.ejb.msf808.MSF808Rec;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf811.*;
import com.mincom.ellipse.edoi.ejb.msf835.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf770.MSF770Key;
import com.mincom.ellipse.edoi.ejb.msf770.MSF770Rec;
import com.mincom.ellipse.edoi.ejb.msf771.MSF771Key;
import com.mincom.ellipse.edoi.ejb.msf771.MSF771Rec;
import com.mincom.ellipse.edoi.ejb.msf772.*;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870AIX3;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Key;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Rec;
import com.mincom.ellipse.edoi.ejb.msf875.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf891.*;

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
//import com.mincom.enterpriseservice.ellipse.lsi.BaseCommandObjectTranslator;
////import com.mincom.ellipse.common.unix.*;
////import com.mincom.ellipse.script.util.*;
//import com.mincom.ria.xml.bind.BigDecimalBinder;
//
//public class AITBatchFWN implements GroovyInterceptable
//{
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
 * Request Parameters for ParamsTrbfwn.
 */
public class ParamsTrbfwn
{
	//List of Input Parameters
	String paramMyEmployees;
	String paramPRC;
	String paramOrganisation;
	String paramPhyLocation;
	String paramCourseId;
	String paramStaffIdEmp;
	String paramStaffIdNonEmp;
	String paramEmpType;
	String paramForecastPeriod;
}

 
/**
 **/
//public class ProcessTrbfwn extends AITBatchFWN {
public class ProcessTrbfwn extends SuperBatch {
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 7;
	private ParamsTrbfwn batchParams;
	
	private String empId;
	private String empIdType;
	private String surname;
	private String prefix;
	private String firstName;
	private String attendedType;
	private String courseId;
	private String courseName;
	private String sessionNo;
	private String delMethodforOldSess;
	private String delMethodD;
	private String delMethod;
	private String attStatus;
	private String requalDate;
	private String attResult;
	private String attResultD;
	private String schedStatusD;
	private String schedStatus;
	private Date forecastDate;
	private String PRC02;
	private String PRC03;
	private String PRC04;
	private String physicalLocnCode;
	private String physicalLocn;
	private String organisation;
	private String organisationCode;
	private String trainersurname;
	private String trainerfirstname;
	private String trainerType;
	private String PRC;
	
	private Date todayDate;
	private String stodayDate;

	//sort list
	private ArrayList listOfAttendees = new ArrayList();
	
	//report list
	private ArrayList arrayOfTrbfwnReportLine = new ArrayList();
	
	//Report A - batch report
	private def ReportA;
	
	int ReportALineCounter = 0;
	int ReportCLineCounter = 0;
	
	private String workDir;
	
	//Report B - CSV file
	File ReportBFile;
	FileWriter ReportBStream;
	String ReportBPath;
	BufferedWriter ReportB;
	
	//Report C - PDF file
	String ReportCPath;
	Document ReportC;


	private DecimalFormat decFormatter = new DecimalFormat("################0.00");
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat disDateFormat = new SimpleDateFormat("dd/MM/yyyy");
	
	com.lowagie.text.Font titleFont = FontFactory.getFont("Arial", 18, Font.BOLD);
	com.lowagie.text.Font boldFont8 = FontFactory.getFont("Arial", 8,Font.BOLD);
	com.lowagie.text.Font boldFont8W = FontFactory.getFont("Arial", 8,Font.BOLD);
	com.lowagie.text.Font boldFont9 = FontFactory.getFont("Arial", 9,Font.BOLD);
	com.lowagie.text.Font normalFont = FontFactory.getFont("Arial", 8);
	PdfPTable dataTable = null;
	
	/**
	 * Run the main batch.
	 * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b) 
	{
		init(b);
		info("uuid         :" + getUUID());
		
		todayDate = new Date();
		stodayDate = todayDate.format("yyyyMMdd");
		
		//java.awt.Color tgblue = new java.awt.Color(30,65,160);
		boldFont8W.setColor(255, 255, 255);

		workDir = env.getWorkDir().toString() + "/";
		ReportBPath = workDir +"TRTFWN" + "." + taskUUID + ".csv";
		ReportBFile = new File(ReportBPath);
		ReportBStream = new FileWriter(ReportBFile)
		ReportB = new BufferedWriter(ReportBStream)
		info("ReportBPath   :" + ReportBPath);

		ReportCPath = workDir +"TRTFWN" + "." + taskUUID + ".pdf";
		ReportC = new Document(PageSize.A4.rotate(),30, 30, 60, 30);  // left,right,top,bottom
		PdfWriter writer = PdfWriter.getInstance(ReportC, new FileOutputStream(ReportCPath));
		AITHeaderAndFooter event = new AITHeaderAndFooter();
		writer.setPageEvent(event);
		info("ReportCPath   :" + ReportCPath);

		printSuperBatchVersion();
		info("runBatch Version : " + version);
		batchParams = params.fill(new ParamsTrbfwn());

		//PrintRequest Parameters
		info("paramMyEmployees              : " + batchParams.paramMyEmployees);
		info("paramPRC                      : " + batchParams.paramPRC);
		info("paramOrganisation (supplier)  : " + batchParams.paramOrganisation);
		info("paramPhyLocation              : " + batchParams.paramPhyLocation);
		info("paramCourseId                 : " + batchParams.paramCourseId);
		info("paramStaffIdEmp               : " + batchParams.paramStaffIdEmp);
		info("paramStaffIdNonEmp            : " + batchParams.paramStaffIdNonEmp);
		info("paramEmpType                  : " + batchParams.paramEmpType);
		info("paramForecastPeriod           : " + batchParams.paramForecastPeriod);
		

		try
		{
			processBatch();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			info("runBatch failed - ${e.getMessage()}");
			info("runBatch failed - ${e.printStackTrace()}");
		}
		finally
		{
			printBatchReport();
		}
	}

		private void processBatch()
		{
			info("processBatch");
			if (initialise_B000())
			{
				processRequest();
				generateTrbfwnReport();
			}
			
			ReportA.close();
		}
		
		public void ThrowNewLine()
		{
			ReportALineCounter++;
			
			if (ReportALineCounter == 70) 
			{
				ReportA.writeLine(132,"=");
				ReportA.write("\f");
				ReportALineCounter = 0;
				writeReportHeader();
				ReportA.writeLine(132,"=");
				
			}
		}
		public void ThrowNewLinePDF()
		{
//			ReportCLineCounter++;
//			if (ReportCLineCounter == 40)
//			{
//				ReportCLineCounter = 0;
//				info("REPORT:ADD TABLE");
//				ReportC.add(dataTable);
//				ReportC.newPage();
//				writeReportHeaderPDF();
//			}
		}

		private void WriteError(String msg)
		{
			ReportA.write(msg);
			info (msg);
			ThrowNewLine();

		}
		private boolean initialise_B000()
		{
			//add forecast days to today's date and convert to yyyyMMdd format
			String s = batchParams.paramForecastPeriod;
			s = s.replaceFirst ("^0*", "");
			Integer i = s.toInteger();
			forecastDate = new Date();
			forecastDate = forecastDate.plus(i);
			
			ReportA = report.open('TRBFWNA')

			String msg = "";
			info("initialise_B000");
			if (!batchParams.paramMyEmployees.equals("Y") && !batchParams.paramMyEmployees.equals("N"))
			{
				WriteError("Must be Y or N");
				return false;
			}
			if (batchParams.paramMyEmployees.equals("Y"))
			{
				if (!batchParams.paramOrganisation.trim().equals("") ||
					!batchParams.paramPhyLocation.trim().equals("") ||
					!batchParams.paramPRC.trim().equals(""))
				{
					WriteError ("Only one primary search allowed");
					return false;
				}
			}
			else{
	
				if (!batchParams.paramPRC.trim().equals("") && !batchParams.paramOrganisation.trim().equals(""))
				{
					WriteError ("Both PRC and Organisation cannot be entered at the same time");
					return false;
				}
	
				if (!batchParams.paramEmpType.trim().equals("") && !batchParams.paramEmpType.trim().equals("N") && !batchParams.paramEmpType.trim().equals("E"))
				{
					WriteError ("Must be E or N");
					return false;
				}
				if (batchParams.paramEmpType.trim().equals("N") && !batchParams.paramPhyLocation.trim().equals(""))
				{
					WriteError ("Physical Location is only for employees");
					return false;
				}
				if (batchParams.paramEmpType.trim().equals("N") && !batchParams.paramPRC.trim().equals(""))
				{
					WriteError ("PRC is only for employees");
					return false;
				}
				if (batchParams.paramEmpType.trim().equals("E") && !batchParams.paramOrganisation.trim().equals(""))
				{
					WriteError ("Origanisation(supplier) is only for non employees");
					return false;
				}
			}
	
			//default to 42 days if blank
			if (batchParams.paramForecastPeriod.trim().equals(""))
			{
				batchParams.paramForecastPeriod = "42";
			}
			
			if (!batchParams.paramPRC.trim().equals(""))
			{
				batchParams.paramEmpType = "E";
			}
			if (!batchParams.paramPhyLocation.trim().equals(""))
			{
				batchParams.paramEmpType = "E";
			}
			if (!batchParams.paramOrganisation.trim().equals(""))
			{
				batchParams.paramEmpType = "N";
			}

			return true;
		}
	
		/**
		 * sort list of attendees.
		 */
		private class TRSFWN implements Comparable<TRSFWN>
		{
			private String sAttnType;
			private String sEmpId;
			private String sCourseId;
			private String sCompltDate;
			private String sSessionNo;
			private String sAttResult;
			private String sRequalDate;
			private String sAttStatus;
			private String sDelMethod;
	
			public TRSFWN (sNewEmpId,sNewCourseId,sNewCompltDate,sNewAttnType,sNewSessionNo,sNewAttStatus, sNewAttResult, sNewRequalDate, sNewDelMethod)
			{
//				info("ADDING TRSFWN: sNewEmpId="+sNewEmpId + " - " + "sNewAttnType="+sNewAttnType + " - " + "sNewCourseId="+sNewCourseId + " - " + "sNewSessionNo="+sNewSessionNo + " - " + "sNewAttResult="+sNewAttResult + " - " + "sNewRequalDate="+sNewRequalDate + " - " + "sNewCompltDate="+sNewCompltDate + " - " + "sNewAttStatus="+sNewAttStatus);
	
				setSortEmpId(sNewEmpId);
				setSortAttnType(sNewAttnType);
				setSortCourseId(sNewCourseId);
				setSortSessionNo(sNewSessionNo);
				setSortCompltDate(sNewCompltDate);
				setSortAttStatus(sNewAttStatus);
				setSortAttResult(sNewAttResult);
				setSortRequalDate(sNewRequalDate);
				setSortDelMethod(sNewDelMethod);
			}
	
			public void setSortEmpId(String sNewEmpId){
				sEmpId = sNewEmpId;
			}
			public String getSortEmpId(){
				return sEmpId;
			}
	
			public void setSortAttnType(String sNewAttnType){
				sAttnType = sNewAttnType;
			}
			public String getSortAttnType(){
				return sAttnType;
			}
	
			public void setSortCourseId(String sNewCourseId){
				sCourseId = sNewCourseId;
			}
			public String getSortCourseId(){
				return sCourseId;
			}
	
			public void setSortSessionNo(String sNewSessionNo){
				sSessionNo = sNewSessionNo;
			}
			public String getSortSessionNo(){
				return sSessionNo;
			}
	
			public void setSortCompltDate(String sNewCompltDate){
				sCompltDate = sNewCompltDate;
			}
			public String getSortCompltDate(){
				return sCompltDate;
			}
	
			public void setSortAttResult(String sNewAttResult){
				sAttResult = sNewAttResult;
			}
			public String getSortAttResult(){
				return sAttResult;
			}
	
			public void setSortRequalDate(String sNewRequalDate){
				sRequalDate = sNewRequalDate;
			}
			public String getSortRequalDate(){
				return sRequalDate;
			}
	
			public void setSortAttStatus(String sNewAttStatus){
				sAttStatus = sNewAttStatus;
			}
			public String getSortAttStatus(){
				return sAttStatus;
			}
			
			public void setSortDelMethod(String sNewDelMethod){
				sDelMethod = sNewDelMethod;
			}
			public String getSortDelMethod(){
				return sDelMethod;
			}
			
			public int compareTo(TRSFWN nextLine)
			{
				if (!sEmpId.equals(nextLine.getSortEmpId())){
					return sEmpId.compareTo(nextLine.getSortEmpId())
				}
				if (!sCourseId.equals(nextLine.getSortCourseId())){
					return sCourseId.compareTo(nextLine.getSortCourseId())
				}
				
				// 01/05/2013 RL sort deliver method
				if (!sDelMethod.equals(nextLine.getSortDelMethod())){
					return sDelMethod.compareTo(nextLine.getSortDelMethod())
				}
				if (!sCompltDate.equals(nextLine.getSortCompltDate())){
					return sCompltDate.compareTo(nextLine.getSortCompltDate())
				}

//				// reverse date - latest first
//				if (!sCompltDate.equals(nextLine.getSortCompltDate())){
//					return nextLine.getSortCompltDate().compareTo(sCompltDate)
//				}
//				// reverse session so we only display the latest & after date
//				if (!sSessionNo.equals(nextLine.getSortSessionNo())){
//					return nextLine.getSortSessionNo().compareTo(sSessionNo)
//				}
				return 0;
			}
		}
	
		/**
		* define setters & getters for report
		 */
		private class TrbfwnaReportLine implements Comparable<TrbfwnaReportLine>
		{
			private String empId;
			private String surname;
			private String firstName;
			private String attnType;
			private String organisation;
			private String busUnit;
			private String group;
			private String branch;
			private String physLocn;
			private String courseId;
			private String courseName;
			private String sessionNo;
			private String delMethod;
			private String attResult;
			private String trainersurname;
			private String trainerfirstname;
			private String compltDate;
			private String requalDate;
			private String schedStatus;
			private String prefix;
	
			public TrbfwnaReportLine (newEmpId,newSurname,newFirstName,newAttnType,newOrganisation,
						 	newBusUnit,newGroup,newBranch,newPhysLocn,newCourseId,newCourseName,newSessionNo,newDelMethod,
							newAttResult,newTrainerSurname,newTrainerFirstname,newCompltDate,newRequalDate,newSchedStatus,newPrefix)
			{
//				info("TrbfwnaReportLine: " + newEmpId + " - " + newSurname + " - " + newFirstName + " - " +
//					newAttnType + " - " + newOrganisation + " - " + newBusUnit + " - " + newGroup + " - " + newBranch + " - " + newPhysLocn + " - " +
//					newCourseId + " - " + newCourseName + " - " + newSessionNo + " - " + newDelMethod + " - " +
//					newAttResult + " - " + newTrainerSurname + " - " + newTrainerFirstname + " - " + newCompltDate + " - " + newRequalDate + " - " + newSchedStatus);

	
				setEmpId(newEmpId);
				setSurname(newSurname);
				setFirstName(newFirstName);
				setAttnType(newAttnType);
				setOrganisation(newOrganisation)
				setBusUnit(newBusUnit);
				setGroup(newGroup);
				setBranch(newBranch);
				setPhysLocn(newPhysLocn);
				setCourseId(newCourseId);
				setCourseName(newCourseName);
				setSessionNo(newSessionNo);
				setDelMethod(newDelMethod);
				setAttResult(newAttResult);
				setTrainerSurname(newTrainerSurname);
				setTrainerFirstname(newTrainerFirstname);
				setCompltDate(newCompltDate);
				setRequalDate(newRequalDate);
				setSchedStatus(newSchedStatus);
				setPrefix(newPrefix);
			}
	
			public void setEmpId(String newEmpId){
				empId = newEmpId;
			}
			public String getEmpId(){
				return empId;
			}
			public void setSurname(String newSurname){
				surname = newSurname;
			}
			public String getSurname(){
				return surname;
			}
			public String setFirstName(String newFirstName){
				firstName = newFirstName;
			}
			public String getFirstName(){
				return firstName;
			}
			public String getNameForReport(){
				return surname + ", " + firstName;
			}
			public String getNameForCSV(){
				return surname + " " + firstName;
			}

			public void setAttnType(String newAttnType){
				attnType = newAttnType;
			}
			public String getAttnType(){
				return attnType;
			}

			public void setOrganisation(String newOrganisation){
				organisation = newOrganisation;
			}
			public String getOrganisation(){
				return organisation;
			}
	
			public void setBusUnit(String newBusUnit)
			{
				busUnit = newBusUnit;
			}
			public String getBusUnit()
			{
				if (busUnit == null) return "";
				return busUnit;
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
	
			public void setPhysLocn(String newPhysLocn)
			{
				physLocn = newPhysLocn;
			}
			public String getPhysLocn()
			{
				return physLocn;
			}
	
			public void setCourseId(String newCourseId){
				courseId = newCourseId;
			}
			public String getCourseId(){
				return courseId;
			}
	
			public void setCourseName(String newCourseName){
				courseName = newCourseName;
			}
			public String getcourseName(){
				return courseName;
			}
	
			public void setSessionNo(String newSessionNo){
				sessionNo = newSessionNo;
			}
			public String getSessionNo(){
				return sessionNo;
			}
	
			public void setDelMethod(String newDelMethod){
				delMethod = newDelMethod;
			}
			public String getDelMethod(){
				return delMethod;
			}
	
			public void setAttResult(String newAttResult){
				attResult = newAttResult;
			}
			public String getAttResult(){
				return attResult;
			}
	
			public void setTrainerSurname(String newTrainerSurname){
				trainersurname = newTrainerSurname;
			}
			public String getTrainerSurname(){
				return trainersurname;
			}
			public void setTrainerFirstname(String newTrainerFirstname){
				trainerfirstname = newTrainerFirstname;
			}
			public String getTrainerFirstname(){
				return trainerfirstname;
			}
			public String getTrainerForReport(){
				if (trainersurname.trim().equals("") && trainerfirstname.trim().equals("")) return "";
				return trainersurname + ", " + trainerfirstname;
			}
			public String getTrainerForCSV(){
				return trainersurname + " " + trainerfirstname;
			}

			public void setCompltDate(String newCompltDate){
				compltDate = newCompltDate;
			}
			public String getCompltDate(){
				return compltDate;
			}
			public String getCompltDateD(){
				try
				{
					Date newerdate = new Date().parse("yyyyMMdd", compltDate)
					String val = newerdate.format("dd/MM/yyyy");
					return val;
				}
				catch (Exception ex)
				{
					return "";
				}
			}
			
			public void setRequalDate(String newRequalDate){
				requalDate = newRequalDate;
			}
			public String getRequalDate(){
				return requalDate;
			}
			public String getRequalDateD(){
				try
				{
					Date newerdate = new Date().parse("yyyyMMdd", requalDate)
					String val = newerdate.format("dd/MM/yyyy");
					return val;
				}
				catch (Exception ex)
				{
					return "";
				}
			}
	
			public void setSchedStatus(String newSchedStatus){
				schedStatus = newSchedStatus;
			}
			public String getSchedStatus(){
				return schedStatus;
			}
			
			//07/06/2013 RL Add P and C in front of emp id
			public void setPrefix(String newPrefix){
				prefix = newPrefix;
			}
			public String getPrefix(){
				return prefix;
			}
	
			public int compareTo(TrbfwnaReportLine otherReportLine)
			{
				if (!courseId.equals(otherReportLine.getCourseId())){
					return courseId.compareTo(otherReportLine.getCourseId())
				}
				if (!requalDate.equals(otherReportLine.getRequalDate())){
					return requalDate.compareTo(otherReportLine.getRequalDate())
				}
				if (!empId.equals(otherReportLine.getEmpId())){
					return empId.compareTo(otherReportLine.getEmpId())
				}
	
				return 0;
			}
		}
	
	
		/**
		 * Process request. <br>
		 * <li>This report will give out warning about staff whose training will be expired within a forecast day. </li>
		 * <li> It will produce a batch report & a CSV file </li>
		 **/
		private void processRequest()
		{
			info("processRequest");
			String positionId;
			Integer itodayDate = Integer.parseInt(stodayDate); 
			String stopDate;
			String invTodayDate;
			 
			try
			{
				empId =  "";
				empIdType="";
				if (batchParams.paramMyEmployees.equals("Y"))
				{
					info("My Employees");
					
					//get subordinates from login position
					positionId = request.getPosition();
					
					////positionId = "1";		// TEMP TEMP TEMP 
					info("positionId : " + positionId);
	
					Constraint c1 = MSF875AIX1.superiorId.equalTo(positionId);
					def query875 = new QueryImpl(MSF875Rec.class). and(c1).orderBy(MSF875Rec.msf875Key);
					
					edoi.search(query875,10000,{MSF875Rec msf875Rec ->
						if (msf875Rec)
						{
							
							//process positions which directly report to the requester
							Constraint c2 = MSF878Key.positionId.equalTo(msf875Rec.getPrimaryKey().getPositionId());
							def query878 = new QueryImpl(MSF878Rec.class). and(c2).orderBy(MSF878Rec.msf878Key);
							edoi.search(query878,10000,{MSF878Rec msf878Rec ->
	
								empId = msf878Rec.getPrimaryKey().getEmployeeId();
								empIdType = "E";
								invTodayDate = 99999999 - itodayDate;
								String sdte = msf878Rec.getPrimaryKey().getPosStopDate().trim();
								
								if (sdte.equals("") || sdte.equals("00000000"))
								{
									stopDate = (new Date()).format("yyyyMMdd");
								}
								else
								{
									stopDate = sdte;
								}
	
								//info("empId        :" + empId + " - getInvStrDate :" + msf878Rec.getPrimaryKey().getInvStrDate() + " - stopDate     :" + stopDate);
	
								if ((msf878Rec.getPrimaryKey().getInvStrDate().toDouble() > invTodayDate.toDouble()) &&
								    (stopDate.toDouble() >= stodayDate.toDouble()))
								{
									//process attendee history
									processMSF772();
								}
							})
						}
					})
				}
				else
				{
					empIdType = "";
					if (!batchParams.paramEmpType.trim().equals(""))
					{
						empIdType = batchParams.paramEmpType;
					}
					
					//batchParams.paramEmpType
					if (batchParams.paramStaffIdEmp.trim().equals("") && batchParams.paramStaffIdNonEmp.trim().equals(""))
					{
						info("All Employees" + " - " + empIdType);
					}
					else
					{
						
						if (!batchParams.paramStaffIdEmp.trim().equals(""))
						{
							if (empIdType.equals("")) empIdType = "E";
							empId = batchParams.paramStaffIdEmp.trim();
						}
						else if (!batchParams.paramStaffIdNonEmp.trim().equals(""))
						{
							if (empIdType.equals("")) empIdType = "N";
							empId = batchParams.paramStaffIdNonEmp.trim();
							
						}
						empId = RightJustifyZeroFill(empId,10);
						info("One Employee : " + empId + " - " + empIdType);
					}
	
					//process attendee history
					processMSF772();
				}
	
				//process array list
				processListOfAttendees();
	
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("processRequest failed - ${e.getMessage()}");
				info("processRequest failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("processRequest Error: " + ex.message);
			}
	
		}
	
		/**
		 * Read MSF772 to get attendee history details. <br>
		 * <li> and write to an array list for further processing </li>
		 **/
		private void processMSF772 ()
		{
//			info ("processMSF772");
			String sTrainer = "";
			try
			{
				//get all courses
				if (batchParams.paramCourseId.trim().equals(""))
				{
					//get specific attendee
					//get specific attendee
					if (empId.trim().equals(""))
					{
						info("All Courses - All Employee");
						getAllAttendeesAllCourses();
					}
					else
					{
						info("All Courses - One Employee");
						get1AttendeeAllCourses();
					}
				}
				else //get specific course
				{
					//get specific attendee
					if (empId.trim().equals(""))
					{
						info("One Courses - All Employee");
						getAllAttendees1Course();
					}
					else
					{
						info("One Courses - One Employee");
						get1Attendee1Course();
					}
				}
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				info("FAIL2");
				e.printStackTrace();
				info("processMSF772 failed - ${e.getMessage()}");
				info("processMSF772 failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("processMSF772 Error: " + ex.message);
			}
		}
	
		/**
		 * get individual attendee for specific course <br>
		 **/
		private void get1Attendee1Course ()
		{
			info ("get1Attendee1Course");
			String sTrainer = "";
	
			try
			{
				sTrainer = "";
				Constraint c1A = MSF772Key.employeeId.equalTo(empId);
				Constraint c1B = MSF772Key.attendeeTy.equalTo(empIdType);
				Constraint c2 = MSF772Rec.requalInd.notEqualTo("");  // equalTo("P");
				Constraint c3 = MSF772Key.compDteInv.notEqualTo("");
				Constraint c4 = MSF772Rec.requalDate.notEqualTo("");
				Constraint c5 = MSF772Rec.requalDate.notEqualTo("00000000");
				Constraint c6 = MSF772Rec.attendResult.equalTo("DA");
				Constraint c7 = MSF772Rec.attendResult.equalTo("EC");
				Constraint c8 = MSF772Rec.attendResult.equalTo("KP");
				Constraint c9 = MSF772Rec.attendResult.equalTo("PP");
				Constraint c10 = MSF772Rec.attendResult.equalTo("SC");
				Constraint c11 = MSF772Key.courseId.equalTo(batchParams.paramCourseId);
				def query1 = new QueryImpl(MSF772Rec.class).and (c11).and (c1A).and (c1B).and (c2).and (c3).and (c4).and(c5) .and((c6).or (c7).or (c8).or(c9).or(c10)).orderBy(MSF772Rec.msf772Key);
	
				edoi.search(query1,10000,{MSF772Rec msf772Rec ->
					
					courseId = msf772Rec.getPrimaryKey().getCourseId();
					sessionNo = msf772Rec.getPrimaryKey().getSessionNo();
					attResult = msf772Rec.getAttendResult();
					requalDate = msf772Rec.getRequalDate();
					String compltdate = (99999999 - msf772Rec.getPrimaryKey().getCompDteInv().toLong()).toString();
					attStatus = msf772Rec.getAttendResult();
					attendedType = msf772Rec.getPrimaryKey().getAttendeeTy();
					
					
//					info("--------");
//					info("msf772Rec  :" + msf772Rec);
//					info("courseId   :" + courseId);
//					info("sessionNo  :" + sessionNo);
//					info("compltdate :" + compltdate);
//					info("requalDate :" + requalDate);
//
//					
//					
//					info("dateFormat.parse(requalDate)		:" + dateFormat.parse(requalDate));
//					info("forecastDate                      :" + forecastDate);
//					info("todayDate                         :" + todayDate);
//					info("dateFormat.parse(requalDate).compareTo(forecastDate)		    :" + dateFormat.parse(requalDate).compareTo(forecastDate));
//					info("dateFormat.parse(requalDate).compareTo(todayDate)   		    :" + dateFormat.parse(requalDate).compareTo(todayDate));
//	
					
					//get the latest completion date for the course attended
					if (dateFormat.parse(requalDate).compareTo(forecastDate) <= 0 && dateFormat.parse(requalDate).compareTo(todayDate) >= 0)
					
					//if (dateFormat.parse(requalDate) <= forecastDate && dateFormat.parse(requalDate) >= todayDate)
					//if (dateFormat.parse(requalDate) <= forecastDate)
					{
						
						
						boolean active = checkStaffStatus(attendedType,empId);
						
						//report active staff
						if (active)
						{
							info("ADDING");
							processMSF771(courseId,sessionNo);
							listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate, delMethod))
							
							//public TRSFWN (sNewEmpId,sNewCourseId,sNewCompltDate,sNewAttnType,sNewSessionNo,sNewAttStatus, sNewAttResult, sNewRequalDate)
							
						}
					}
				})
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("get1Attendee1Course failed - ${e.getMessage()}");
				info("get1Attendee1Course failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("get1Attendee1Course Error: " + ex.message);
			}
		}
	
		/**
		 * get individual attendee for all relevant courses  <br>
		 **/
		private void get1AttendeeAllCourses ()
		{
			info ("get1AttendeeAllCourses");
			String sTrainer = "";
	
			try
			{
				//info("empId		:" + empId);
				//info("empIdType :" + empIdType);
				

				
				sTrainer = "";
				Constraint c1A = MSF772Key.employeeId.equalTo(empId);
				Constraint c1B = MSF772Key.attendeeTy.equalTo(empIdType);
				Constraint c2 = MSF772Rec.requalInd.notEqualTo("");  // equalTo("P");
				Constraint c3 = MSF772Key.compDteInv.notEqualTo("");
				Constraint c4 = MSF772Rec.requalDate.notEqualTo("");
				Constraint c5 = MSF772Rec.requalDate.notEqualTo("00000000");
				Constraint c6 = MSF772Rec.attendResult.equalTo("DA");
				Constraint c7 = MSF772Rec.attendResult.equalTo("EC");
				Constraint c8 = MSF772Rec.attendResult.equalTo("KP");
				Constraint c9 = MSF772Rec.attendResult.equalTo("PP");
				Constraint c10 = MSF772Rec.attendResult.equalTo("SC");
				def query1 = new QueryImpl(MSF772Rec.class).and (c1A).and (c1B).and (c2).and (c3).and (c4).and(c5) .and((c6).or (c7).or (c8).or(c9).or(c10)).orderBy(MSF772Rec.msf772Key);
	
				edoi.search(query1,10000,{MSF772Rec msf772Rec ->
					
					courseId = msf772Rec.getPrimaryKey().getCourseId();
					sessionNo = msf772Rec.getPrimaryKey().getSessionNo();
					attResult = msf772Rec.getAttendResult();
					requalDate = msf772Rec.getRequalDate();
					String compltdate = (99999999 - msf772Rec.getPrimaryKey().getCompDteInv().toLong()).toString();
					attStatus = msf772Rec.getAttendResult();
					attendedType = msf772Rec.getPrimaryKey().getAttendeeTy();
					
				

					//get the latest completion date for the course attended
					if (dateFormat.parse(requalDate).compareTo(forecastDate) <= 0 && dateFormat.parse(requalDate).compareTo(todayDate) >= 0)
					//if (dateFormat.parse(requalDate) <= forecastDate && dateFormat.parse(requalDate) >= todayDate)
					//if (dateFormat.parse(requalDate) <= forecastDate)
					{
						boolean active = checkStaffStatus(attendedType,empId);
						
						//info("active		    :" + active);
						
						//report active staff
						if (active)
						{
							
							processMSF771(courseId,sessionNo);
							listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate, delMethod))

							//listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate))
						}
					}
				})
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("get1AttendeeAllCourses failed - ${e.getMessage()}");
				info("get1AttendeeAllCourses failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("get1AttendeeAllCourses Error: " + ex.message);
			}
		}
	
		/**
		 * get all attendees for specific course <br>
		 **/
		private void getAllAttendees1Course ()
		{
			info ("getAllAttendees1Course");
			String sTrainer = "";
	
			try
			{
				sTrainer = "";

				Constraint c1A = MSF772Key.courseId.equalTo(batchParams.paramCourseId);
				Constraint c1B = MSF772Key.attendeeTy.greaterThanEqualTo("E");
				Constraint c2 = MSF772Rec.requalInd.notEqualTo("");  // equalTo("P");
				Constraint c3 = MSF772Key.compDteInv.notEqualTo("");
				Constraint c4 = MSF772Rec.requalDate.notEqualTo("");
				Constraint c5 = MSF772Rec.requalDate.notEqualTo("00000000");
				Constraint c6 = MSF772Rec.attendResult.equalTo("DA");
				Constraint c7 = MSF772Rec.attendResult.equalTo("EC");
				Constraint c8 = MSF772Rec.attendResult.equalTo("KP");
				Constraint c9 = MSF772Rec.attendResult.equalTo("PP");
				Constraint c10 = MSF772Rec.attendResult.equalTo("SC");
	
				def query1 = new QueryImpl(MSF772Rec.class).and (c1A).and (c1B).and (c2).and (c3).and (c4).and(c5) .and((c6).or (c7).or (c8).or(c9).or(c10)).orderBy(MSF772Rec.msf772Key);
	
				edoi.search(query1,10000,{MSF772Rec msf772Rec ->
					
					courseId = msf772Rec.getPrimaryKey().getCourseId();
					sessionNo = msf772Rec.getPrimaryKey().getSessionNo();
					attResult = msf772Rec.getAttendResult();
					requalDate = msf772Rec.getRequalDate();
					String compltdate = (99999999 - msf772Rec.getPrimaryKey().getCompDteInv().toLong()).toString();
					attStatus = msf772Rec.getAttendResult();
					attendedType = msf772Rec.getPrimaryKey().getAttendeeTy();
					empId = msf772Rec.getPrimaryKey().getEmployeeId();
				
					//get the latest completion date for the course attended
					if (dateFormat.parse(requalDate) <= forecastDate && dateFormat.parse(requalDate) >= todayDate)
					//if (dateFormat.parse(requalDate) <= forecastDate)
					{
						boolean active = checkStaffStatus(attendedType,empId);
						
						//report active staff
						if (active)
						//if (active &&
						//	(empIdType.equals("") || attendedType.equals(empIdType)))
						{
							processMSF771(courseId,sessionNo);
							listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate, delMethod))

								//listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate))	
						}
					}
				})
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("getAllAttendees1Course failed - ${e.getMessage()}");
				info("getAllAttendees1Course failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getAllAttendees1Course Error: " + ex.message);
			}
		}
	
		/**
		 * get all attendees for all relevant courses  <br>
		 **/
		private void getAllAttendeesAllCourses ()
		{
			info ("getAllAttendeesAllCourses");
			String sTrainer = "";
	
			try
			{
//				info("Character.MIN_VALUE :" + Character.MIN_VALUE);
//				info("Character.MIN_VALUE :" + Character.MIN_VALUE.toString());
//				
//				java.lang.Boolean restart = true;
//				MSF772Key restartKey = new MSF772Key();
//				restartKey.setAttendeeTy(Character.MIN_VALUE.toString());
//				restartKey.setCompDteInv(Character.MIN_VALUE.toString());
//				restartKey.setCourseId(Character.MIN_VALUE.toString());
//				restartKey.setEmployeeId(Character.MIN_VALUE.toString());
//				restartKey.setInvTargetDate(Character.MIN_VALUE.toString());
//				restartKey.setSessionNo(Character.MIN_VALUE.toString());
//				
//				
//				Integer counter = 0;
//				Integer max = 1000;
//				
//				while (restart)
//				{
//					info("RESTARTING...");
//					
//					restart = false;
					
					sTrainer = "";
					Constraint c1B = MSF772Key.attendeeTy.greaterThanEqualTo("E");
					Constraint c2 = MSF772Rec.requalInd.notEqualTo("");  // equalTo("P");
					Constraint c3 = MSF772Key.compDteInv.notEqualTo("");
					Constraint c4 = MSF772Rec.requalDate.notEqualTo("");
					Constraint c5 = MSF772Rec.requalDate.notEqualTo("00000000");
					Constraint c6 = MSF772Rec.attendResult.equalTo("DA");
					Constraint c7 = MSF772Rec.attendResult.equalTo("EC");
					Constraint c8 = MSF772Rec.attendResult.equalTo("KP");
					Constraint c9 = MSF772Rec.attendResult.equalTo("PP");
					Constraint c10 = MSF772Rec.attendResult.equalTo("SC");
					
//					Constraint crestart1 = MSF772Key.attendeeTy.greaterThan(restartKey.attendeeTy);
//					Constraint crestart2 = MSF772Key.compDteInv.greaterThan(restartKey.compDteInv);
//					Constraint crestart3 = MSF772Key.courseId.greaterThan(restartKey.courseId);
//					Constraint crestart4 = MSF772Key.employeeId.greaterThan(restartKey.employeeId);
//					Constraint crestart5 = MSF772Key.invTargetDate.greaterThan(restartKey.invTargetDate);
//					Constraint crestart6 = MSF772Key.sessionNo.greaterThan(restartKey.sessionNo);
					
//					def query1 = new QueryImpl(MSF772Rec.class).and (c1B).and (c2).and (c3).and (c4).and(c5) .and((c6).or (c7).or (c8).or(c9).or(c10)).and(crestart1).and(crestart2).and(crestart3).and(crestart4).and(crestart5).and(crestart6).orderBy(MSF772Rec.msf772Key);
					def query1 = new QueryImpl(MSF772Rec.class).and (c1B).and (c2).and (c3).and (c4).and(c5) .and((c6).or (c7).or (c8).or(c9).or(c10)).orderBy(MSF772Rec.msf772Key);
					
					edoi.search(query1,10000, {MSF772Rec msf772Rec ->
						
						courseId = msf772Rec.getPrimaryKey().getCourseId();
						sessionNo = msf772Rec.getPrimaryKey().getSessionNo();
						attResult = msf772Rec.getAttendResult();
						requalDate = msf772Rec.getRequalDate();
						String compltdate = (99999999 - msf772Rec.getPrimaryKey().getCompDteInv().toLong()).toString();
						attStatus = msf772Rec.getAttendResult();
						attendedType = msf772Rec.getPrimaryKey().getAttendeeTy();
						empId = msf772Rec.getPrimaryKey().getEmployeeId();
						
						//info("HERE: " + attendedType + " - " + empId + " - " + courseId + " - " + sessionNo + " - " + requalDate);
					
//						info("--------");
//						info("msf772Rec  :" + msf772Rec);
//						info("courseId   :" + courseId);
//						info("sessionNo  :" + sessionNo);
//						info("compltdate :" + compltdate);
//						info("requalDate :" + requalDate);
//	
//						
//						
//						info("dateFormat.parse(requalDate)		:" + dateFormat.parse(requalDate));
//						info("forecastDate                      :" + forecastDate);
//						info("todayDate                         :" + todayDate);
//						info("dateFormat.parse(requalDate).compareTo(forecastDate)		    :" + dateFormat.parse(requalDate).compareTo(forecastDate));
//						info("dateFormat.parse(requalDate).compareTo(todayDate)   		    :" + dateFormat.parse(requalDate).compareTo(todayDate));
		
						//get the latest completion date for the course attended
						if (dateFormat.parse(requalDate).compareTo(forecastDate) <= 0 && dateFormat.parse(requalDate).compareTo(todayDate) >= 0)
						//if (dateFormat.parse(requalDate) <= forecastDate)
						{
							boolean active = checkStaffStatus(attendedType,empId);
							
							//report active staff
							if (active)
							//if (active &&
							//	(empIdType.equals("") || attendedType.equals(empIdType)))
							{
								//info("ADDING");
								processMSF771(courseId,sessionNo);
								listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate, delMethod))
	
									//listOfAttendees.add(new TRSFWN(empId    ,courseId    ,compltdate    ,attendedType,sessionNo    ,attStatus    ,attResult      ,requalDate))	
							}
						}
//						counter++;
//						info("counter :" + counter);
//						if (counter.equals(max))
//						{
//							restart = true;
//							restartKey = msf772Rec.getPrimaryKey();
//							info("restartKey :" + restartKey);
//							counter = 0;
//						}
					})
//				}
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{	
				info("FAIL!");
				e.printStackTrace();
				info("getAllAttendeesAllCourses failed - ${e.getMessage()}");
				info("getAllAttendeesAllCourses failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getAllAttendeesAllCourses Error: " + ex.message);
			}

		}
	
		private boolean CheckParameters()
		{
		
		}
		
		/**
		 * Read MSF760 & MSF811 to check for active status. <br>
		 **/
		private boolean checkStaffStatus(String sType, String sStaffId)
		{
			//info ("checkStaffStatus - sType:" + sType + " - sStaffId" + sStaffId);
			boolean result;
	
			try
			{
				result = false;
				if (sType.equals("E"))
				{
					Constraint c1 = MSF760Rec.empStatus.equalTo("A");
					Constraint c2 = MSF760Rec.staffCateg.notEqualTo("ZO");
					Constraint c3 = MSF760Rec.staffCateg.notEqualTo("WC");
					Constraint c4 = MSF760Rec.staffCateg.notEqualTo("ZA");
					Constraint c5 = MSF760Key.employeeId.equalTo(sStaffId);
					def query = new QueryImpl(MSF760Rec.class).and (c1).and(c2).and (c3).and (c4).and(c5);
					MSF760Rec msf760Rec = (MSF760Rec) edoi.firstRow(query);
					
					//20/06/13 - changed MSF760Rec to msf760Rec
					if (msf760Rec)
					{
						result = true;
					}
				}
				else //case of non employee
				{
					MSF811Rec msf811Rec = edoi.findByPrimaryKey(new MSF811Key(sStaffId));
					if (msf811Rec)
					{
						result = true;
					}
				}
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("checkStaffStatus failed - ${e.getMessage()}");
				info("checkStaffStatus failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("checkStaffStatus Error: " + ex.message);
			}
			return result;
		}
	
		/**
		 * process the attendees's sort list to get the rest of details. <br>
		 **/
		private void processListOfAttendees ()
		{
			info ("processListOfAttendees");
			String key = "";
			TRSFWN sortRec;
			TRSFWN prevSRec;
	
			Collections.sort(listOfAttendees);

			ArrayList<String> processedList = new ArrayList<String>();	
	
			for (int i = 0; i < listOfAttendees.size(); i++)
			{
				sortRec = listOfAttendees.get(i);
				
				//info("===============================================================================================================")
				//info("PROCESSING: " + sortRec.getSortEmpId() + " - " + sortRec.getSortCourseId() + " - " + sortRec.getSortSessionNo() + " - " + sortRec.getSortCompltDate());
				
				if ((prevSRec == null) ||
				    !sortRec.getSortEmpId().equals(prevSRec.getSortEmpId()) ||
					
					// RL 01/05/2013 - Also check delivery method
					!sortRec.getSortDelMethod().equals(prevSRec.getSortDelMethod()) ||
				 	!sortRec.getSortCourseId().equals(prevSRec.getSortCourseId()))
				{
					//info("PROCESSING: " + sortRec.getSortEmpId() + " - " + sortRec.getSortAttnType() + " - " + sortRec.getSortCourseId() + " - " + sortRec.getSortSessionNo() + " - " + sortRec.getSortCompltDate());
					boolean PassedParams = true;
					//info("PROCESSING next bit");
					
					
					if (sortRec.getSortAttnType().equals("E"))
					{
						//info("PROCESSING A: " + sortRec.getSortEmpId() + " - " + sortRec.getSortCourseId() + " - " + sortRec.getSortSessionNo() + " - " + sortRec.getSortCompltDate());
													
						// Validate Emp Type
						if (batchParams.paramEmpType.trim().equals("N"))
						{
							PassedParams = false;
							continue;
						}

						// validate Organisation - no emps with 
						if (!batchParams.paramOrganisation.trim().equals(""))
						{
							PassedParams = false;
							continue;
						}
						
						//get PRC & physical location
						getPRC(sortRec.getSortEmpId());

						// Validate PRC
							//info("batchParams.paramPRC  :" + batchParams.paramPRC.trim())
							//info("PRC                   :" + PRC.trim());
						if (!batchParams.paramPRC.trim().equals("") && !batchParams.paramPRC.trim().equals(PRC.trim()))
						{
							PassedParams = false;
							//info ("BAD");
							continue;
						}
						else
						{
							//info ("GOOD");
						}

						getPhysLocn(sortRec.getSortEmpId());
						
						// Validate Physical Location
						if (!batchParams.paramPhyLocation.trim().equals("") && !batchParams.paramPhyLocation.trim().equals(physicalLocnCode.trim()))
						{
							PassedParams = false;
							continue;
						}
						
						getEmpName(sortRec.getSortEmpId());						
						
					}
					else //for non employees
					{
						//07/06/2013 RL Add P and C in front of emp id
						prefix = "C";
						
						// Validate PRC - none 
						if (!batchParams.paramPRC.trim().equals(""))
						{
							PassedParams = false;
							continue;
						}
						// Validate Physical Location
						if (!batchParams.paramPhyLocation.trim().equals(""))
						{
							PassedParams = false;
							continue;
						}

						// Validate Emp Type
						if (batchParams.paramEmpType.trim().equals("E"))
						{
							PassedParams = false;
							continue;
						}
						
						//get name and organisation
						getNonEmpNameOrg(sortRec.getSortEmpId());
	
						// Validate Supplier
						if (!batchParams.paramOrganisation.trim().equals("") && !batchParams.paramOrganisation.trim().equals(organisationCode.trim()))
						{
							PassedParams = false;
							continue;
						}

					}
					
					//get delivery method and trainer for old course
					processMSF771(sortRec.getSortCourseId(),sortRec.getSortSessionNo());
					
					//get attend result & delivery method descriptions
					getDesc (sortRec.getSortAttResult(), delMethod);

					//get course name
					processMSF770(sortRec.getSortCourseId());

					//check employee schedule status for an up coming course
					checkSchedStatus(sortRec);

					if (PassedParams)
					{
						//info("PROCESSING: " + sortRec.getSortEmpId() + " - " + sortRec.getSortAttnType() + " - " + sortRec.getSortCourseId() + " - " + sortRec.getSortSessionNo() + " - " + sortRec.getSortCompltDate());
						
							arrayOfTrbfwnReportLine.add(new TrbfwnaReportLine(sortRec.getSortEmpId(),surname,firstName,
								sortRec.getSortAttnType(),organisation,PRC02,PRC03,PRC04,physicalLocn,
								sortRec.getSortCourseId(),courseName,sortRec.getSortSessionNo(),delMethodD,
								attResultD,trainersurname,trainerfirstname,sortRec.getSortCompltDate(),sortRec.getSortRequalDate(),schedStatusD,prefix))
					}

				}
				else
				{
					//info("  SKIPPING next bit");
				}
				prevSRec = sortRec;
//				index++;
			}
		}

		/**
		 * 20/06/13 - Read MSF760 to check for employee's status. <br>
		 **/
		private String processMSF760 (String empId)
		{
			info ("processMSF760");
			String result;
	
			try
			{
				Constraint c1 = MSF760Key.employeeId.equalTo(empId);
				def query = new QueryImpl(MSF760Rec.class).and (c1);
				MSF760Rec msf760Rec = (MSF760Rec) edoi.firstRow(query);
	
				if (msf760Rec)
				{
					result = msf760Rec.getEmpStatus();
				}
	
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("processMSF760 failed - ${e.getMessage()}");
			}
			catch (Exception ex)
			{
				info ("processMSF760 Error: " + ex.message);
			}
			return result;
		}
		
		/**
		 * get delivery method and trainer <br>
		 **/
		private void processMSF771 (String sCourseId, String sSessNo)
		{
//			info ("processMSF771");
			String sTrainer;
			try
			{
				sTrainer = "";
				trainersurname = "";
				trainerfirstname = "";
				trainerType = "";
				
				//get delivery method & trainer for the old session
				Constraint c21 = MSF771Key.courseId.equalTo(sCourseId);
				Constraint c22 = MSF771Key.sessionNo.equalTo(sSessNo);
				def query2 = new QueryImpl(MSF771Rec.class).and (c21).and(c22);
				MSF771Rec msf771Rec = (MSF771Rec) edoi.firstRow(query2);

				if (msf771Rec)
				{
					delMethod = msf771Rec.getDeliveryMethod();
					
					if (!msf771Rec.getTrainerTypex1().trim().equals(""))
					{
						sTrainer = msf771Rec.getTrainerx1();
	
						ReadMSF810Name(sTrainer);
						
						if (trainersurname.equals(""))
						{
							ReadMSF811Name(sTrainer);
						}
						
					}
				}
//				info ("    delMethod  :"+delMethod);
//				info ("    trainer    :"+trainer);
//				info ("    trainerType:"+trainerType);
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("processMSF771 failed - ${e.getMessage()}");
				info("processMSF771 failed - ${e.getStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("processMSF771 Error: " + ex.message);
			}
		}
		
		private void ReadMSF810Name(String empid810)
		{
			try
			{
				MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(empid810));
				if (msf810Rec)
				{
					trainersurname = msf810Rec.getSurname();
					trainerfirstname = msf810Rec.getFirstName();
					trainerType = "E";
				}
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				//info ("ReadMSF810Name failed: " + e.getMessage());
			}
			catch (Exception ex)
			{
				//info ("ReadMSF810Name Error: " + ex.message);
			}
		}
		private void ReadMSF811Name(String empid811)
		{
			try
			{
				MSF811Rec msf811Rec = edoi.findByPrimaryKey(new MSF811Key(empid811));
				if (msf811Rec)
				{
					trainersurname = msf811Rec.getSurname();
					trainerfirstname = msf811Rec.getFirstName();
					trainerType = "N";
				}
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				//info ("ReadMSF811Name failed: " + e.getMessage());
			}
			catch (Exception ex)
			{
				//info ("ReadMSF811Name Error: " + ex.message);
			}
		}
	
		/**
		 * get attend status, result and session delivery method descriptions. <br>
		 **/
		private void getDesc(String sResult, String sDelMethod)
		{
//			info("Process getDesc");
			attResultD = "";
			delMethodD = "";
	
			try
			{
				Constraint c3 = MSF010Key.tableType.equalTo("TRRS");
				Constraint c4 = MSF010Key.tableCode.equalTo(sResult);
				def query2 = new QueryImpl(MSF010Rec.class).and(c3).and (c4);
				MSF010Rec msf010Rec2 = (MSF010Rec) edoi.firstRow(query2);
				
				if (msf010Rec2)
				{
					if (!msf010Rec2.getTableDesc().trim().equals(""))
					{
						attResultD = msf010Rec2.getTableDesc();
					}
					
					Constraint c5 = MSF010Key.tableType.equalTo("DELM");
					Constraint c6 = MSF010Key.tableCode.equalTo(sDelMethod);
					def query3 = new QueryImpl(MSF010Rec.class).and(c5).and (c6);
					MSF010Rec msf010Rec3 = (MSF010Rec) edoi.firstRow(query3);
					
					if (msf010Rec3)
					{
						if (!msf010Rec3.getTableDesc().trim().equals(""))
						{
							delMethodD = msf010Rec3.getTableDesc();
						}
		//				info("    attResultD :" + delMethodD);
		//				info("    delMethodD :" + delMethodD);
					}
				}
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("getDesc failed - ${e.getMessage()}");
				info("getDesc failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getDesc Error: " + ex.message);
			}
		}
		
	
		private String RightJustifyZeroFill(String inp, int len)
		{
			info("RightJustifyZeroFill  in  : " + inp);
			try
			{
				// 07/06/2013 RL remove the P or C in the number entered
				String news1 = inp.replace("P", "");
				String news2 = news1.replace("C", "");
				
				int tmp = news2.toInteger(); // will throw if not int
				
				String news3 = news2.padLeft(10,"0");
				return news3;

			}
			catch (Exception ex)
			{
			}
			info("RightJustifyZeroFill  out2: " + inp);
			return inp;
		}
	
		/**
		 * Read MSF810 to get employee's name. <br>
		 **/
		private void getEmpName(String sEmpId)
		{
//			info("Process getEmpName");
			surname = "";
			firstName = "";
	
			try{
				MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(sEmpId));
				surname = msf810Rec.getSurname();
				firstName = msf810Rec.getFirstName();
//				info("   NAME : " + firstName + " " + surname);
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				info ("Employee Name not found for "+ sEmpId)
				e.printStackTrace();
				info("getEmpName failed - ${e.getMessage()}");
				info("getEmpName failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getEmpName Error: " + ex.message);
			}
		}
	
		/**
		 * Read MSF811 to get non employee's name. <br>
		 * <li> also, get organisation by look up supplier no, customer no or 1st line of the postal address </li>
		 **/
		private void getNonEmpNameOrg(String sEmpId)
		{
			//info("Process getNonEmpNameOrg");
			surname = "";
			firstName = "";
			organisation="";
			organisationCode="";
			PRC02 = "";
			PRC03 = "";
			PRC04 = "";
			PRC = "";
			physicalLocn="";
			physicalLocnCode="";
	
			try{
				MSF811Rec msf811Rec = edoi.findByPrimaryKey(new MSF811Key(sEmpId));
				
				if (msf811Rec)
				{
					surname = msf811Rec.getSurname();
					firstName = msf811Rec.getFirstName();
					
					if (msf811Rec.getSupplierNo().trim().equals(""))
					{
						if (msf811Rec.getCustomerNo().trim().equals(""))
						{ 
							if (!msf811Rec.getPostAddress_1().trim().equals(""))
							{
								organisation = msf811Rec.getPostAddress_1();
							}
						}
						else
						{
							MSF500Rec msf500Rec = edoi.findByPrimaryKey(new MSF500Key(msf811Rec.getCustomerNo()));
							if (msf500Rec)
							{
								organisation = msf500Rec.getCustName();
								organisationCode = msf811Rec.getCustomerNo();
							}
						}
					}
					else{
						MSF200Rec msf200Rec = edoi.findByPrimaryKey(new MSF200Key(msf811Rec.getSupplierNo()));
						if (msf200Rec)
						{
							organisation = msf200Rec.getSupplierName();
							organisationCode = msf811Rec.getSupplierNo();
						}
					}
				}
	
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				info ("Non Employee Name not found for "+ sEmpId)
				e.printStackTrace();
				info("getNonEmpNameOrg failed - ${e.getMessage()}");
				info("getNonEmpNameOrg failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getNonEmpNameOrg Error: " + ex.message);
			}
		}
	
		
		private void getPhysLocn (String sEmpId)
		{
			physicalLocn = "";
			physicalLocnCode="";
			
			//07/06/2013 RL Add P and C in front of emp id
			prefix = "C";
			
			try
			{
				//get standard working hours
				Constraint c1 = MSF820Key.employeeId.equalTo(sEmpId);
				def query820 = new QueryImpl(MSF820Rec.class).and (c1);
				MSF820Rec msf820Rec = (MSF820Rec) edoi.firstRow(query820);
				
				if (msf820Rec)
				{
					//07/06/2013 RL Add P and C in front of emp id
					prefix = "P";
					
					physicalLocnCode = msf820Rec.getRptPhyLoc();
				
					//info("physicalLocnCode:"+physicalLocnCode);
					
					
					Constraint c7 = MSF010Key.tableType.equalTo("PHYL");
					Constraint c8 = MSF010Key.tableCode.equalTo(physicalLocnCode);
					def query4 = new QueryImpl(MSF010Rec.class).and(c7.and (c8));
					MSF010Rec msf010Rec4 = (MSF010Rec) edoi.firstRow(query4);
					if (msf010Rec4)
					{
						physicalLocn = msf010Rec4.getTableDesc();
					}
//				info("physicalLocn:"+physicalLocn);
				}
				
				
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("getPhysLocn failed - ${e.getMessage()}");
				info("getPhysLocn failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getPhysLocn Error: " + ex.message);
			}
		}
		
		/**
		 * Get PRC and it's description from 010 table. <br>
		 **/
		private void getPRC (String sEmpId)
		{
//			info ("Process getPRCPhysLocn")
			String sDateFrom;
			String sDateTo;
			String PRC870;
			String empId878;
			String invTodayDate;
			
			PRC02="";
			PRC03="";
			PRC04="";
			PRC = "";
			organisation="";
			
			
			try
			{
				Date newerdate = new Date();
				String today = newerdate.format("yyyyMMdd");
				
				invTodayDate = (99999999 - today.toDouble());
				Constraint c1 = MSF878AIX1.invStrDate.greaterThanEqualTo(invTodayDate);		// TODAY
				Constraint c2 = MSF878AIX1.primaryPos.equalTo("0"); //primary position
				Constraint c3 = MSF878AIX1.posStopDate.equalTo("00000000");
				Constraint c4 = MSF878AIX1.posStopDate.equalTo("");
				Constraint c5 = MSF878AIX1.posStopDate.greaterThanEqualTo(today);
				Constraint c6 = MSF878Key.employeeId.equalTo(sEmpId);
				
				def query;
				query = new QueryImpl(MSF878Rec.class).and(c1).and(c2).and((c3).or(c4).or(c5)).and(c6).orderBy(MSF878Rec.msf878Key);

				
				//get PRC for each employee found in 878
				edoi.search(query,10000,{MSF878Rec msf878Rec ->
					if (msf878Rec)
					{
						empId878 = msf878Rec.getPrimaryKey().getEmployeeId();
						
						//read MSF870 to get PRC
						Constraint c11 = MSF870Key.positionId.equalTo(msf878Rec.getPrimaryKey().getPositionId());
						def query11;
						if (batchParams.paramPRC.trim() == ""){
							query11 = new QueryImpl(MSF870Rec.class).and(c11);
						}
						else{
							Constraint c12 = MSF870AIX3.primRptGrp.equalTo(batchParams.paramPRC);
							query11 = new QueryImpl(MSF870Rec.class).and(c11).and(c12);
						}
						MSF870Rec msf870Rec = (MSF870Rec) edoi.firstRow(query11);
						if (msf870Rec)
						{
							PRC870 = msf870Rec.getPrimRptGrp();
							//getPRCDesc (PRC870);
							
							PRC = PRC870
							//info("PRC :" + PRC);
							
							if (PRC.equals("")) return;
							
							Constraint ct1 = MSF010Key.tableType.equalTo("PC02");
							Constraint ct2 = MSF010Key.tableCode.equalTo(PRC870.substring(4,8));
							Constraint ct3 = MSF010Key.tableType.equalTo("PC03");
							Constraint ct4 = MSF010Key.tableCode.equalTo(PRC870.substring(8,12));
							Constraint ct5 = MSF010Key.tableType.equalTo("PC04");
							Constraint ct6 = MSF010Key.tableCode.equalTo(PRC870.substring(12,16));
							Constraint ct7 = MSF010Key.tableType.equalTo("PC01");
							Constraint ct8 = MSF010Key.tableCode.equalTo(PRC870.substring(0,4));

							def query1 = new QueryImpl(MSF010Rec.class).and(ct1.and (ct2));
							MSF010Rec msf010Rec1 = (MSF010Rec) edoi.firstRow(query1);
							if (msf010Rec1)
							{
								if (!msf010Rec1.getTableDesc().trim().equals(""))
								{
									PRC02 = msf010Rec1.getTableDesc();
								}
							}
							
							def query2 = new QueryImpl(MSF010Rec.class).and(ct3 .and(ct4));
							MSF010Rec msf010Rec2 = (MSF010Rec) edoi.firstRow(query2);
							if (msf010Rec2)
							{
								if (!msf010Rec2.getTableDesc().trim().equals(""))
								{
									PRC03 = msf010Rec2.getTableDesc();
								}
							}
							
							def query3 = new QueryImpl(MSF010Rec.class).and(ct5 .and (ct6));
							MSF010Rec msf010Rec3 = (MSF010Rec) edoi.firstRow(query3);
							if (msf010Rec3)
							{
								if (!msf010Rec3.getTableDesc().trim().equals(""))
								{
									PRC04 = msf010Rec3.getTableDesc();
								}
							}
							
							def query4 = new QueryImpl(MSF010Rec.class).and(ct7 .and (ct8));
							MSF010Rec msf010Rec4 = (MSF010Rec) edoi.firstRow(query4);
							if (msf010Rec4)
							{
								if (!msf010Rec4.getTableDesc().trim().equals(""))
								{
									organisation = msf010Rec4.getTableDesc();
								}
							} 
						}
					}
				})
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("Cannot read PRC - getPRC failed - ${e.getMessage()}");
				info("Cannot read PRC - getPRC failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getPRC Error: " + ex.message);
			}
//
		}
	
		private void getPRCDesc (String PRC870)
		{
			info ("Process getPRCDesc : " + PRC870);
			
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
				
				//use PRC name in MSF808 instead
				Constraint c1 = MSF808Key.primRptCodes.equalTo(PRC870);
	
				def query = new QueryImpl(MSF808Rec.class).and(c1);
				MSF808Rec msf808Rec = (MSF808Rec) edoi.firstRow(query);
				
				if (msf808Rec)
				{
				
						
					String desc = msf808Rec.getPrcName();
					String[] descarray = desc.split(/\//);
				
					
					if (descarray.size() >= 1)
						PRC02 = descarray[0];
					if (descarray.size() >= 2)
						PRC02 = descarray[1];
					if (descarray.size() >= 3)
						PRC03 = descarray[2];
					if (descarray.size() >= 4)
						PRC04 = descarray[3];
					
				}
	
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("getPRCDesc failed - ${e.getMessage()}");
				info("getPRCDesc failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("getPRCDesc Error: " + ex.message);
			}
		}
		
		/**
		 * Read MSF770 to get course name <br>
		 **/
		private void processMSF770(String sCourseId)
		{
//			info("processMSF770");
			try
			{
				//get course name
				courseName="";
				Constraint c1 = MSF770Key.courseId.equalTo(sCourseId)
				def query = new QueryImpl(MSF770Rec.class).and(c1);
				
				MSF770Rec msf770Rec = (MSF770Rec) edoi.firstRow(query);
				
				if (msf770Rec)
				{
					courseName = msf770Rec.getCourseTitle();
				}
					
//				info("   courseName   :" + courseName);
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				info ("Course Name not found for "+ sCourseId)
				e.printStackTrace();
				info("processMSF770 failed - ${e.getMessage()}");
				info("processMSF770 failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("processMSF770 Error: " + ex.message);
			}
		}
	
		/**
		 * browse MSF771 for session details. <br>
		 **/
		private void checkSchedStatus (TRSFWN sortRec)
		{
//			info ("checkSchedStatus");
			Double invRunDate;
			Date dte = new Date();
			try
			{
				schedStatus = "";
				schedStatusD = "";
				
				invRunDate = 99999999 - dateFormat.format(dte).toDouble();		// today
				
				// MSF771 - Training Course Sessions
				// MSF772 - Employee Course History
				
				//check if the employee has already been nominated to a session
				Constraint c1 = MSF771Key.courseId.equalTo(sortRec.getSortCourseId());
				Constraint c2 = MSF771Rec.deliveryMethod.equalTo(delMethod);
				
				def query = new QueryImpl(MSF771Rec.class).and (c1).and(c2).orderBy(MSF771Rec.msf771Key);
				
				edoi.search(query,10000,{MSF771Rec msf771Rec ->
					
					// Must be future or no data 
					if (msf771Rec.getInvStrDate().toDouble() < invRunDate ||
						msf771Rec.getInvStrDate().trim().equals(""))
					{
						//delMethod = msf771Rec.getDeliveryMethod();
						//get attendee's status
						Constraint c3 = MSF772Key.courseId.equalTo(sortRec.getSortCourseId());
						Constraint c4 = MSF772Key.employeeId.equalTo(sortRec.getSortEmpId());
						Constraint c5 = MSF772Key.sessionNo.equalTo(msf771Rec.getPrimaryKey().getSessionNo());
						
						def query2 = new QueryImpl(MSF772Rec.class).and (c3).and(c4).and (c5);
						
						MSF772Rec msf772Rec = (MSF772Rec) edoi.firstRow(query2);
						
						if (msf772Rec)
						{
							schedStatus = msf772Rec.getAttendStatus();
							
							//get description
							Constraint c6 = MSF010Key.tableType.equalTo("ATST");
							Constraint c7 = MSF010Key.tableCode.equalTo(schedStatus);
							
							def query3 = new QueryImpl(MSF010Rec.class).and(c6).and (c7);
							MSF010Rec msf010Rec = (MSF010Rec) edoi.firstRow(query3);
							
							if (msf010Rec)
							{
								if (!msf010Rec.getTableDesc().trim().equals(""))
								{
									schedStatusD = msf010Rec.getTableDesc();
								}
								
								if (!schedStatus.equals(""))
								{
									return;
									info ("checkSchedStatus");
									info ("    schedStatus  :" + schedStatus);
									info ("    schedStatusD :" + schedStatusD);
								}
							}
						}
					}
				})
				
			}
			catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e)
			{
				e.printStackTrace();
				info("checkSchedStatus failed - ${e.getMessage()}");
				info("checkSchedStatus failed - ${e.printStackTrace()}");
			}
			catch (Exception ex)
			{
				info ("checkSchedStatus Error: " + ex.message);
			}
		}
	
		/**
		 * print batch report <br>
		 * <li> output total record counts </li>
		 **/
		private PdfPCell BlankCell()
		{
			PdfPCell c1;
			c1 = new PdfPCell(new Phrase("",boldFont9));
			c1.setBorder(Rectangle.NO_BORDER);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			return c1;
		}
		private PdfPCell ParamCell(String message)
		{
			PdfPCell c1;
			c1 = new PdfPCell(new Phrase(message,boldFont9));
			c1.setBorder(Rectangle.NO_BORDER);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			return c1;
		}
		private void generateTrbfwnReport()
		{
			info("Start generateTrbfwnReport");
			
			//CSV detail file
			ReportB.write("Employee Id,Employee Name,Employee Type,Organisation,Business Unit,Group,Branch," +
						  "Physical Location,Course ID,Course Description,Session,Delivery Method,Result,Assessor," +
						  "Result Date,Requal Date,Scheduled Status" + "\n")
			
			info("REPORT:OPEN");
			ReportC.open();
			//Paragraph title = new Paragraph("First Warning",titleFont);
			//title.setAlignment(Element.ALIGN_LEFT);
			//ReportC.add(title);
			
			PdfPTable params = new PdfPTable(5);
			params.setHorizontalAlignment(Element.ALIGN_LEFT);
			params.setTotalWidth(20f);
			//params.getDefaultCell().setBorder(1);
			//params.setWidthPercentage(100f);
			float[] b = [10f,10f,1f,10f,10f]
			params.setWidths(b);
			PdfPCell c1;
			
			Chunk underline = new Chunk("Parameters:");
			underline.setUnderline(0.1f, -2f); //0.1 thick, -2 y-location
			underline.setFont(boldFont9);
			ReportC.add(underline);

			//------			
//			c1 = new PdfPCell(new Phrase("Parameters:",boldFont));
//			c1.setBorder(Rectangle.NO_BORDER);
//			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//			params.addCell(c1);
//			params.addCell(BlankCell());
//			params.addCell(BlankCell());
//			params.addCell(BlankCell());
//			params.addCell(BlankCell());
			//------
			params.addCell(ParamCell("Employee/Non Employee ID:"));
			if (batchParams.paramStaffIdEmp.equals(""))
				params.addCell(ParamCell(batchParams.paramStaffIdNonEmp));
			else
				params.addCell(ParamCell(batchParams.paramStaffIdEmp));
			params.addCell(BlankCell());
			params.addCell(ParamCell("Course:"));
			params.addCell(ParamCell(batchParams.paramCourseId));
			//------
			params.addCell(ParamCell("Employee Type:"));
			params.addCell(ParamCell(batchParams.paramEmpType));
			params.addCell(BlankCell());
			params.addCell(ParamCell("Forecast Period (Days):"));
			params.addCell(ParamCell(batchParams.paramForecastPeriod.toString()));
			//------
			params.addCell(ParamCell("Organisation:"));
			params.addCell(ParamCell(batchParams.paramOrganisation));
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			//------
			params.addCell(ParamCell("Primary Reporting Codes (PRC):"));
			params.addCell(ParamCell(batchParams.paramPRC));
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			//------
			params.addCell(ParamCell("My Employees (Y/N):"));
			params.addCell(ParamCell(batchParams.paramMyEmployees));
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			//------
			params.addCell(ParamCell("Employee Physical Location:"));
			params.addCell(ParamCell(batchParams.paramPhyLocation));
			params.addCell(BlankCell());
			params.addCell(BlankCell());
			params.addCell(BlankCell());

			ReportC.add(new Phrase("",boldFont9));
			ReportC.add(params);
			ReportC.add(new Phrase(" ",boldFont9));
			ReportC.add(new Phrase(" ",boldFont9));
			
			TrbfwnaReportLine prevLine, currLine;
			
			writeReportHeader();
			writeReportHeaderPDF();
			
			Collections.sort(arrayOfTrbfwnReportLine);
			
			for (int i = 0; i < arrayOfTrbfwnReportLine.size(); i++)
			{
				currLine = arrayOfTrbfwnReportLine.get(i);
				
				if (i == 0 || !currLine.getCourseId().equals(prevLine.getCourseId()))
				{
					ReportA.writeLine(132,"=");
					ThrowNewLine();
					ReportA.write("Course " + currLine.getCourseId() + " -  " + currLine.getcourseName());
					ThrowNewLine();
					ReportA.writeLine(132,"=");
					ThrowNewLine();
					
					// PDF
					c1 = new PdfPCell(new Phrase("Course " + currLine.getCourseId() + " -  " + currLine.getcourseName(),boldFont8));
					c1.setColspan(11);
					c1.setBorder(Rectangle.BOX);
					c1.setHorizontalAlignment(Element.ALIGN_LEFT);
					java.awt.Color col = new java.awt.Color(199,234,251);
					c1.setBackgroundColor(col);
					dataTable.addCell(c1);
		 
		
					ThrowNewLinePDF();
					
				}
				writeReportDetail(currLine);
				writeCSVFile(currLine);
				writeReportDetailPDF(currLine);
				prevLine = currLine;
			}
			
			
			ReportA.write("\n");
			ThrowNewLine();
			ReportA.writeLine(132,"=");
			ThrowNewLine();
			ReportA.write("\n");
			ThrowNewLine();
			
//			if (ReportCLineCounter > 1)
//			{
//				info("REPORT:ADD TABLE");
//				ReportC.add(dataTable);
//			}
			
			ReportC.add(dataTable);
			
			ReportB.close();
			info("REPORT:CLOSE");
			ReportC.close();
	
			info("End generateTrbfwnReport");
		}
		
		public String FormattedEmployeeId(String inID)
		{
			String s = inID;
			s = s.replaceFirst ("^0*", "");
			return s;
		}
	
		private void writeReportHeader()
		{
			info("writeReportHeader");
			String tempString;
	
			//ReportA.write(StringUtils.center("First Warning", 132));
			//ThrowNewLine();
			
			ReportA.writeLine(132,"=");
			ThrowNewLine();
			
			ReportA.write("Employee Employee Name              Emp Session Delivery Method          Assessor                    Result Date     Schedule");
			ThrowNewLine();
			
			ReportA.write("ID                                  Type        Result                                               Requal Date     Status");
			ThrowNewLine();
			
			
		}
		private void writeReportHeaderPDF()
		{
			
			
			
			//////////////////////////// PDF
			info("REPORTC: NEW TABLE  1");
			dataTable = new PdfPTable(11);
			
			dataTable.setHeaderRows(1);
			//dataTable.setSkipFirstHeader(true);

			
			dataTable.getDefaultCell().setBorder(1);
			dataTable.setWidthPercentage(100f);
			float[] a = [8f,25f,5f,7f,20f,20f,25f,9f,9f,9f,10f]
			dataTable.setWidths(a);
			
			java.awt.Color col = new java.awt.Color(30, 65, 160);
			

			
			PdfPCell c1 = new PdfPCell(new Phrase("Employee ID",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase("Employee Name",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Emp Type",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Session",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Delivery Method",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Result",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Assessor",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Result Date",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Requal Date",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Schedule",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			c1 = new PdfPCell(new Phrase("Status",boldFont8W));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			c1.setBackgroundColor(col);
			dataTable.addCell(c1);
			
			info("REPORTC: NEW TABLE - done");
			ThrowNewLinePDF();
			info("REPORTC: NEW TABLE - finished");
		}
		
		private void writeReportDetail(TrbfwnaReportLine reportLine)
		{
			//07/06/2013 RL Add P and C in front of emp id

			String tempString;
			tempString = "";
			tempString = tempString + (reportLine.getPrefix() + FormattedEmployeeId(reportLine.getEmpId())).padRight(9);
			tempString = tempString + (CutString(reportLine.getNameForReport(),26)).padRight(27);
			tempString = tempString + (reportLine.getAttnType().padRight(4));
			tempString = tempString + (reportLine.getSessionNo().padRight(8));
			tempString = tempString + (reportLine.getDelMethod().padRight(25));
			tempString = tempString + (CutString(reportLine.getTrainerForReport(),27).padRight(28));
			tempString = tempString + (reportLine.getCompltDateD().padRight(11));
			tempString = tempString + (reportLine.getSchedStatus());
			ReportA.write(tempString);
			ThrowNewLine();
			
			tempString = "                                                ";
			tempString = tempString + (reportLine.getAttResult().padRight(25));
			tempString = tempString + "                            ";
			tempString = tempString + (reportLine.getRequalDateD().padRight(11));
			ReportA.write(tempString);
			ThrowNewLine();

		}
		private void writeReportDetailPDF(TrbfwnaReportLine reportLine)
		{
			//07/06/2013 RL Add P and C in front of emp id


			PdfPCell c1;
			c1 = new PdfPCell(new Phrase((reportLine.getPrefix() + FormattedEmployeeId(reportLine.getEmpId())).padRight(9) ,normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase( (CutString(reportLine.getNameForReport(),26)).padRight(27),normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase( (reportLine.getAttnType().padRight(4)),normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase( (reportLine.getSessionNo().padRight(8)),normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase((reportLine.getDelMethod().padRight(25)) ,normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase((reportLine.getAttResult().padRight(25)) ,normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase( (CutString(reportLine.getTrainerForReport(),27).padRight(28)),normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase((reportLine.getCompltDateD().padRight(11)) ,normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase((reportLine.getRequalDateD().padRight(11)) ,normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase(( reportLine.getCompltDateD().padRight(11)),normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			
			c1 = new PdfPCell(new Phrase((reportLine.getSchedStatus()) ,normalFont));
			c1.setBorder(Rectangle.BOX);
			c1.setHorizontalAlignment(Element.ALIGN_LEFT);
			dataTable.addCell(c1);
			
			ThrowNewLinePDF();

		}
	
		private void writeCSVFile(TrbfwnaReportLine reportLine)
		{
			//07/06/2013 RL Add P and C in front of emp id
				
			String APP = "\"";
			ReportB.write(APP + reportLine.getPrefix() + FormattedEmployeeId(reportLine.getEmpId()) + APP + "," +
				APP + reportLine.getNameForCSV() + APP + "," +
				APP + reportLine.getAttnType() + APP + "," +
				APP + reportLine.getOrganisation() + APP + "," +
				APP + reportLine.getBusUnit() + APP + "," + 
				APP + reportLine.getGroup() + APP + "," + 
				APP + reportLine.getBranch() + APP + "," +
				APP + reportLine.getPhysLocn() + APP + "," + 
				APP + reportLine.getCourseId() + APP + "," + 
				APP + reportLine.getcourseName() + APP + "," +
				APP + reportLine.getSessionNo() + APP + "," + 
				APP + reportLine.getDelMethod () + APP + "," +
				APP + reportLine.getAttResult() + APP + "," +
				APP + reportLine.getTrainerForCSV () + APP + "," +
				APP + reportLine.getCompltDateD() + APP + "," +
				APP + reportLine.getRequalDateD() + APP + "," + 
				APP + reportLine.getSchedStatus() + APP + "\n")
		}
	
		/**
		 * print batch report <br>
		 * <li> output total record counts </li>
		 **/
	private void printBatchReport()
	{
		info("printBatchReport");
		//print batch report
		println ("\n");
		println(StringUtils.center("End of Report ", 132));
		
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
	
	public class AITHeaderAndFooter extends com.lowagie.text.pdf.PdfPageEventHelper
	{
		protected Phrase header;
		protected PdfPTable footer;
	
		public AITHeaderAndFooter()
		{
			info("AITHeaderAndFooter");
			//header = new Phrase("**** Header ****");
			//footer = new PdfPTable(1);
			//footer.setTotalWidth(150);
			//footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
			//footer.addCell(new Phrase(new Chunk("**** Footer summary Report generated by JavaGenious****").setAction(new PdfAction(PdfAction.FIRSTPAGE))));
		}
	
		public void onEndPage(PdfWriter writer, Document document)
		{
			info("onEndPage");
			try 
			{
				PdfPCell c1;
				
				com.lowagie.text.Font headerFont = FontFactory.getFont("Arial", 20,Font.BOLD);
				headerFont.setColor(30, 65, 160);
				
				PdfPTable header = new PdfPTable(1);
				header.setTotalWidth(300f);
				header.setWidthPercentage(300f);
				header.getDefaultCell().setBorder(1);
				header.setWidthPercentage(100f);
				float[] b = [150f,150f]
				header.setWidths(b);
				c1 = new PdfPCell(new Phrase("First Warning",headerFont));
				c1.setBorder(Rectangle.NO_BORDER);
				c1.setHorizontalAlignment(Element.ALIGN_LEFT);
				header.addCell(c1);
				
				header.writeSelectedRows(0, -1, 30, 580, writer.getDirectContent());

				try
				{
					String url = "/var/opt/mincom/customer-software/batch/src/com/mincom/ellipse/script/custom/TGLOGO.jpg";
					Image logo = Image.getInstance(url);
					logo.scaleAbsolute(170f,30f);				
					logo.setAbsolutePosition(610, 540);
					writer.getDirectContent().addImage(logo);
				}
				catch (Exception ex)
				{
					 
					 
				}

				
				
				//http://thewire/esc/cc/PublishingImages/Corporate%20Logo.bmp
				
				
				//  30 in
				// 580 up
				
				PdfPTable footer = new PdfPTable(1);
				footer.setTotalWidth(100f);
				footer.setWidthPercentage(100f);
				footer.getDefaultCell().setBorder(1);
				footer.setWidthPercentage(100f);
				float[] a = [100f]
				footer.setWidths(a);
				c1 = new PdfPCell(new Phrase(String.format("Page %d", writer.getPageNumber()),normalFont));
				c1.setBorder(Rectangle.NO_BORDER);
				c1.setHorizontalAlignment(Element.ALIGN_LEFT);
				footer.addCell(c1);
				footer.writeSelectedRows(0, -1, 430, 20, writer.getDirectContent());
				// 430 in
				//  20 up

	

			}
			
			catch(DocumentException de)
			{
				info("EX:" + de.message)
			}
			
	//		PdfPTable table = new PdfPTable(3);
	//		float[] a = [24f,24f,2f]
	//		table.setWidths(a);
	//		table.setTotalWidth(527);
	//		table.setLockedWidth(true);
	//		table.getDefaultCell().setFixedHeight(20);
	//		table.getDefaultCell().setBorder(Rectangle.BOTTOM);
	//		table.addCell(header);
	//		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
	//		table.addCell(String.format("Page %d of", writer.getPageNumber()));
	//		PdfPCell cell = new PdfPCell(Image.getInstance(total));
	//		cell.setBorder(Rectangle.BOTTOM);
	//		table.addCell(cell);
	//		table.writeSelectedRows(0, -1, 34, 803, writer.getDirectContent());
			
			//com.lowagie.text.Font normalFont = FontFactory.getFont("Arial", 8);
			//footer.add
			//PdfContentByte cb = writer.getDirectContent();
			//ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(String.format("%d", writer.getPageNumber()),normalFont), (document.right() - document.left()) / 2 + document.leftMargin(), document.top() + 10, 0);
			//footer.writeSelectedRows(0, -1,(document.right() - document.left() - 300) / 2 + document.leftMargin(), document.bottom() - 10, cb);
		}
	
	}
}




	
/**
 * Run the script
 */
ProcessTrbfwn process = new ProcessTrbfwn()
process.runBatch(binding)