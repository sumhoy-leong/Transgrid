/*@Ventyx 2013
 *
 * This program will produce both a CSV output file and a control report.
 *
 * This program will look at the Ellipse Inspections and Condition 
 * Monitoring Module tables, and will:
 * 
 * a) Highlight those Inspections that relate to taking Oil Samples
 * where there HAS NOT been corresponding Condition Monitoring
 * records recorded for BOTH the;
 *         Gas in the Oil (CM Type OA) and the
 *         Fluid in the Oil (CM Type OQ)
 * 
 * This is for the purpose of warning that these Oil Samples 
 * haven't had all of their results returned from the laboratory
 * to Transgrid and recorded in Ellipse (via the LIMS interface) into 
 * Ellipse Condition Monitoring.
 *
 * b) Provide information in a CSV file of the Oil Sample results  
 * where there HAS been corresponding Condition Monitoring
 * records recorded for BOTH the;
 *         Gas in the Oil (CM Type OA) and the
 *         Fluid in the Oil (CM Type OQ)
 *
 * Note that these is no direct link between data in the 
 * Ellipse Inspections module and data in the Ellipse 
 * Condition Monitoring module - it is expected that
 * there will be one inspection per year for equipment
 * that requires Oil Samples, and 
 * one set of Gas in the Oil (CM Type OA) results and one 
 * set of Fluids in the Oil (CM Type OQ) within
 * 90 days of the inspection being completed.
 *
 * The logical functional process of this program needs to be;
 * 
 * 1. Read through the INSP_ACT_RESULTS and History tables and select 
 * those records where the repsonse field is YES for an OIL02 sample. 
 * 
 * 2. Retrieve the INSP_ACT_HISTORY table and only
 * select those records where the COMPLETED DATE is between the 
 * "Insp Date From" and the "Insp Date To" request parameters.
 *
 * 3. Read MSF600 table for the Equip Number from the INSP_ACT_HISTORY
 * to obtain the PLANT_NO for the item of equipment inspected. 
 * 
 * 5. Read MSFX69 table for the Equip Number to find out where 
 * that piece of equipment is fitted to an installation parent (if at all). 
 *
 * At this stage we have all the OIL inspections within
 * the completed time frame, and the relevant plant numbers of either the item
 * inspected or where that item is installed. 
 * Depending upon whether the "Region Prefix" parameter was entered
 * additional filtering is performed.
 * If Region Prefix parameter WAS NOT entered, then all inspections 
 * are elegible for reporting otherwise the Equipment Plant Number or
 * Installation Parent Plant Number that match the first 3 characters are
 * selected for reporting.
 *
 * Now we need to go and find the earliest Condition Monitoring records that 
 * could relate to these OIL inspections.
 * Remember that we are interested in finding BOTH of
 * "OA" (Gas in the Oil) and "OQ" (Fluid in the Oil) 
 * condition monitoring records that relate to an inspection.
 *
 * If we find BOTH of these Condition Monitoring records, 
 * then we will output a record on the CSV file for this inspection, 
 * showing both OA an OQ condition monitoring results.
 * 
 * If however we only find either;
 * "OA" condition monitoring results but no "OQ" condition monitoring results,
 * "OQ" condition monitoring results but no "OA" condition monitoring results, 
 * Neither "OA" or "OQ" condition monitoring results,
 * then that means that we have found an Oil inspection that hasn't had all of 
 * required condition results recorded in Ellipse yet, 
 * and this is the Exception Condition that this report is mainly designed to 
 * find.
 *
 * Using the show missing inspections only parameter will only output
 * inspaction records that are missing both sets of results.
 *
 * Remember also that Condition Monitoring results are only eligible for 
 * considering if the date of the result MSF345-REVSD-DATE is no more than
 * 90 days after the completed date of the corresponding inspection 
 * ie INSP_ACT_HISTORY.COMPLETED_DATE
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;
import groovy.sql.Sql
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import com.mincom.batch.script.*;
import com.mincom.ellipse.lsi.buffer.condmeasurement.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import com.mincom.ellipse.client.connection.ConnectionHolder;
import com.mincom.enterpriseservice.ellipse.ConnectionId;
import nacaLib.varEx.Var;
import com.mincom.ellipse.ejp.EllipseSessionDataContainer;
import com.mincom.ellipse.ejp.area.CommAreaWrapper;

public class ParamsTRBOSR{
    //List of Input Parameters 
    /*
     * The parameter Region Prefix is optional but if entered will be 3 characters in length  
     * The parameter Inspection Date From is mandatory and will be in DD/MMM/YY format
     * The parameter Inspection Date To is mandatory and will be in DD/MMM/YY format
     * The parameter Missing Results Only is mandatory and will be either Y or N
     *
     */
    String paramRegionPrefix;
    String paramInspDateFrom;
    String paramInspDateTo;
    String paramMissingResultsOnly;
}

/**
 * Main process of trbosr.
 */

public class ProcessTRBOSR extends SuperBatch {

    /*
     * Constants
     */
    private static final String CSV_TRTOSR = "TRTOSR"
    private static final String REPORT_NAME = "TRBOSRA"
   /*
    * variables
    */
    private def Trbosra                // Control and Error report
    private boolean bErrorOccured = false
    private File trtosrFile;
    private BufferedWriter csvTrtosrWriter
    private boolean firstErr = true
    private boolean bReportAOpen   = false;
    private boolean bOAset = false
    private boolean bOQset = false
    
    private boolean firstCsv = true
    private long inspRead = 0, inspSkipped = 0, inspWrite = 0, oaRead = 0, oqRead = 0, inspMissing = 0
    private DataSource dataSource;
    private String sTempMeas 
    private String sTempValue
    private String sCompMonPos
    private String sOAKey
    private String sOQKey
    // fields for CSV data
    private String sEquipNo
    private String sPlantNo
    private String sItemName
    private String sEquipGrpId
    private String sOriginalDoc
    private String sCompCode
    private String sEquipStatus
    private String sInstallStatus
    private String sCompDate
    private String sCompTime
    private String sInspDate
    private String sInspDatePlus90
    private String sInstallPlantNo
    private String sInstallName
    private String sScriptInspType
    private String sWorkOrder
    private String sInstallParent
    // variables for the CM part of the report.
    // OA
    private String sOA_Position
    private String sOA_CMType
    private String sOA_Date
    private String sOA_Time
    private String sOA_C2H2
    private String sOA_C2H4
    private String sOA_C2H6
    private String sOA_CH4
    private String sOA_CO
    private String sOA_CO2
    private String sOA_H2
    private String sOA_N2
    private String sOA_O2
    private String sOA_OILTEMP
    private String sOA_SAMPLE_NO
    private String sOA_TCG
    private String sOA_TEST_DATE
    private String sOA_TOAE
    private String sOA_WorkOrder
    // OQ
    private String sOQ_Position
    private String sOQ_CMType
    private String sOQ_Date
    private String sOQ_Time
    private String sOQ_AF
    private String sOQ_DBS
    private String sOQ_DDF
    private String sOQ_F
    private String sOQ_FFA
    private String sOQ_H2O
    private String sOQ_HMF
    private String sOQ_IFT
    private String sOQ_MEYERS
    private String sOQ_MF
    private String sOQ_OILACID
    private String sOQ_OILRES
    private String sOQ_OILTEMP
    private String sOQ_PCB
    private String sOQ_SAMPLE_NO
    private String sOQ_TEST_DATE
    private String sOQ_TF
    private String sOQ_WorkOrder
   /* 
    * IMPORTANT!
    * Update this Version number EVERY push to GIT 
    */
    private version = "3";
    private ParamsTRBOSR batchParams;
   /**
    * Run the main batch.
    * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
    */
    public void runBatch(Binding b){

        init(b);
        dataSource = b.getVariable("dataSource");
        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTRBOSR())

        //PrintRequest Parameters
        info("paramRegionPrefix: " + batchParams.paramRegionPrefix)
        info("paramInspectionDateFrom: " + batchParams.paramInspDateFrom)
        info("paramInspectionDateTo:   " + batchParams.paramInspDateTo)
        info("paramMissingResultsOnly: " + batchParams.paramMissingResultsOnly)
        batchParams.paramRegionPrefix = batchParams.paramRegionPrefix.toUpperCase()
        
        try {
            processBatch();
        }
        catch(Exception e){
            info("error ${e.printStackTrace()}")
            e.printStackTrace()
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRBOSRA ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")
        }
        finally {
// do nothing
        }
    }

    private void processBatch(){
        info("processBatch");
        performInitialise()
        
        if (!bErrorOccured) {
            // Process request parameters
            processParams()
            
            // request parameters are valid so continue
            try {
                processOilInspections()
            } finally {
                performFinalise()
                printBatchReport()
            }
        }
    }
   /**
    * Initialisation - create control report and output file
    * @param None
    * @return None
    */
    private void performInitialise() {
        info("performInitialise")
        // Create Control Report
        List <String> headingsA = new ArrayList <String>()
        headingsA = setPageHeadingsA()
        Trbosra = report.open("TRBOSRA", headingsA)
        bReportAOpen = true;
    }
     
   /**
    * Finalisation Process
    * @param None
    * @return None
    */
    private void performFinalise () {
        info("performFinalise")
        if (bReportAOpen) {
            // Print Control report summary
            printControlReportSummary() 
            // Print Report trailer
            if (bErrorOccured) {
                 Trbosra.write("\n>> Process Completed with errors. <<\n")
            } else {
                 Trbosra.write("\n>> Process Completed Successfully. No errors encountered. <<\n")
            } 
            Trbosra.close()
        }
    }
    // Set Report A header
    private List setPageHeadingsA() {
        info("setPageHeadingsA")
        List <String> headings = new ArrayList <String>()
        headings.add("OIL SAMPLE CONFIRMATION - CONTROL AND ERROR REPORT".center(132))
        return headings
    }
   /**
    * Validate and setup request parameters
    */
    private void processParams() {
        info("processParams")
    }
    private void writeCsvReport(){
        debug("writeCsvReport")
        if(firstCsv){
            firstCsv = false
            def workingDir = env.workDir
            String taskUUID = this.getTaskUUID()
            String inputFilePath = "${workingDir}/${CSV_TRTOSR}"
            String outputFilePath = "${workingDir}/${CSV_TRTOSR}"
            if(taskUUID?.trim()){
                outputFilePath = outputFilePath + "." + taskUUID
            }
            outputFilePath = outputFilePath + ".csv"
            trtosrFile = new File(outputFilePath)
            info("${CSV_TRTOSR} created in ${trtosrFile.getAbsolutePath()}")
            csvTrtosrWriter = new BufferedWriter(new FileWriter(trtosrFile))
            String header = setPageHeadingsCSV()
            csvTrtosrWriter.write(header)
            csvTrtosrWriter.write("\r\n")
        }
        String rowCSV = setRowCSV()
        debug("writeCsvReport bOAset: ${bOAset.toString()} bOQset: ${bOQset.toString()} ")
        if ((!bOQset) || (!bOAset)) {
            inspMissing++
        }

        if ((bOAset) && (bOQset) && (batchParams.paramMissingResultsOnly).equals("Y")) {
            //do nothing
        } else {
            inspWrite++
            csvTrtosrWriter.write(rowCSV.toString())
            csvTrtosrWriter.write("\r\n")
        }
    }
    private initialiseCSVfields() {
        sOAKey = ""
        sOQKey = ""
        sEquipNo = ""
        sPlantNo = ""
        sItemName = ""
        sInstallParent = ""
        sInstallPlantNo = ""
        sInstallName = ""
        sScriptInspType = ""
        sEquipGrpId = ""
        sOriginalDoc = ""
        sCompCode = ""
        sEquipStatus = ""
        sInstallStatus = ""
        sCompDate = ""
        sCompTime = ""
        sWorkOrder = ""
        sOA_Position = ""
        sOA_CMType = ""
        sOA_Date = ""
        sOA_Time = ""
        sOA_C2H2 = ""
        sOA_C2H4 = ""
        sOA_C2H6 = ""
        sOA_CH4 = ""
        sOA_CO = ""
        sOA_CO2 = ""
        sOA_H2 = ""
        sOA_N2 = ""
        sOA_O2 = ""
        sOA_OILTEMP = ""
        sOA_SAMPLE_NO = ""
        sOA_TCG = ""
        sOA_TEST_DATE = ""
        sOA_TOAE = ""
        sOA_WorkOrder = ""
        sOQ_Position = ""
        sOQ_CMType = ""
        sOQ_Date = ""
        sOQ_Time = ""
        sOQ_AF = ""
        sOQ_DBS = ""
        sOQ_DDF = ""
        sOQ_F = ""
        sOQ_FFA = ""
        sOQ_H2O = ""
        sOQ_HMF = ""
        sOQ_IFT = ""
        sOQ_MEYERS = ""
        sOQ_MF = ""
        sOQ_OILACID = ""
        sOQ_OILRES = ""
        sOQ_OILTEMP = ""
        sOQ_PCB = ""
        sOQ_SAMPLE_NO = ""
        sOQ_TEST_DATE = ""
        sOQ_TF = ""
        sOQ_WorkOrder = ""
    }
    // Set CSV header
    private String setPageHeadingsCSV() {
        info("setPageHeadingsCSV")
        StringBuffer headings = new StringBuffer()
        headings.append("Equip Number,")
        headings.append("Equip Reference,")
        headings.append("Equip Description,")
        headings.append("EGI,")
        headings.append("Contract No,")
        headings.append("Comp Code,")
        headings.append("Status,")
        headings.append("Insp Type,")
        headings.append("Date,")
        headings.append("Time,")
        headings.append("Work Order,")
        headings.append("Position,")
        headings.append("CM Type,")
        headings.append("Date,")
        headings.append("Time,")
        headings.append("C2H2,")
        headings.append("C2H4,")
        headings.append("C2H6,")
        headings.append("CH4,")
        headings.append("CO,")
        headings.append("CO2,")
        headings.append("H2,")
        headings.append("N2,")
        headings.append("O2,")
        headings.append("OILTEMP,")
        headings.append("SAMPLE-NO,")
        headings.append("TCG,")
        headings.append("TEST DATE,")
        headings.append("TOAE,")
        headings.append("WO-NUMBER,")
        headings.append("Position,")
        headings.append("CM Type,")
        headings.append("Date,")
        headings.append("Time,")
        headings.append("AF,")
        headings.append("DBS,")
        headings.append("DDF,")
        headings.append("F,")
        headings.append("FFA,")
        headings.append("H2O,")
        headings.append("HMF,")
        headings.append("IFT,")
        headings.append("MEYERS,")
        headings.append("MF,")
        headings.append("OILACID,")
        headings.append("OILRES,")
        headings.append("OILTEMP,")
        headings.append("PCB,")
        headings.append("SAMPLE-NO,")
        headings.append("TEST DATE,")
        headings.append("TF,")
        headings.append("WO-NUMBER")
        //second line heading
        headings.append("\r\n,,,,,,,,,,,,,,,")
        headings.append("Acetylene,")
        headings.append("Ethylene,")
        headings.append("Ethane,")
        headings.append("Methane,")
        headings.append("Carbon Monoxide,")
        headings.append("Carbon Dioxide,")
        headings.append("Hydrogen,")
        headings.append("Nitrogen,")
        headings.append("Oxygen,")
        headings.append("Oil Temperature,")
        headings.append("LIMS Sample Number,")
        headings.append("Total Combined Gas,")
        headings.append("Test Date,")
        headings.append("TOA Equipment Condit,")
        headings.append("Work Order Number,,,,,")
        headings.append("2-ACETYLFURAN,")
        headings.append("Dielectric Breakdown,")
        headings.append("Dielectric Dissipati,")
        headings.append("2-FURFURAL,")
        headings.append("Furfuryl Alcohol,")
        headings.append("Moisture Level,")
        headings.append("5-METHYL METHYL - 2,")
        headings.append("Interfacial Tension,")
        headings.append("Meyers Index Number,")
        headings.append("5-METHYL - FURFURAL,")
        headings.append("Oil Acidity,")
        headings.append("Oil Resistivity,")
        headings.append("Oil Temperature,")
        headings.append("Polychlorinated Biph,")
        headings.append("LIMS Sample Number,")
        headings.append("Test Date,")
        headings.append("TOTAL FURANS,")
        headings.append("Work Order Number")
        return headings.toString()
    }
    // Set CSV rows
    private String setRowCSV() {
        debug("setRowCSV EquipNo: $sEquipNo Installation Parent: $sInstallParent")
        StringBuffer row = new StringBuffer()
        row.append(sEquipNo.padRight(12))
        if (sInstallParent !=null && !sInstallParent.trim().equals("")) { 
            row.append("," + sInstallPlantNo.padRight(30))
            row.append(",\"" + sInstallName.padRight(40) + "\"")        
        } else {
            row.append("," + sPlantNo.padRight(30))
            row.append(",\"" + sItemName.padRight(40) + "\"")
        }
        row.append(",\"" + sEquipGrpId.padRight(12) + "\"")
        row.append(",\"" + sOriginalDoc.padRight(10) + "\"")
        row.append("," + sCompCode.padRight(4))
        if (sInstallParent !=null && !sInstallParent.trim().equals("")) {   
            row.append("," + sInstallStatus.padRight(2))
        } else {
            row.append("," + sEquipStatus.padRight(2))
        }
        row.append("," + sScriptInspType.padRight(12))
        row.append("," + sCompDate)
        row.append("," + sCompTime)
        row.append("," + sWorkOrder)
        //now find the CM results for the OA and OQ measurements
        retrieveCMResults()
        row.append("," + sOA_Position)
        row.append("," + sOA_CMType)
        row.append("," + sOA_Date)
        row.append("," + sOA_Time)
        row.append("," + sOA_C2H2)
        row.append("," + sOA_C2H4)
        row.append("," + sOA_C2H6)
        row.append("," + sOA_CH4)
        row.append("," + sOA_CO)
        row.append("," + sOA_CO2)
        row.append("," + sOA_H2)
        row.append("," + sOA_N2)
        row.append("," + sOA_O2)
        row.append("," + sOA_OILTEMP)
        row.append("," + sOA_SAMPLE_NO)
        row.append("," + sOA_TCG)
        row.append("," + sOA_TEST_DATE)
        row.append("," + sOA_TOAE)
        row.append("," + sOA_WorkOrder)
        row.append("," + sOQ_Position)
        row.append("," + sOQ_CMType)
        row.append("," + sOQ_Date)
        row.append("," + sOQ_Time)
        row.append("," + sOQ_AF)
        row.append("," + sOQ_DBS)
        row.append("," + sOQ_DDF)
        row.append("," + sOQ_F)
        row.append("," + sOQ_FFA)
        row.append("," + sOQ_H2O)
        row.append("," + sOQ_HMF)
        row.append("," + sOQ_IFT)
        row.append("," + sOQ_MEYERS)
        row.append("," + sOQ_MF)
        row.append("," + sOQ_OILACID)
        row.append("," + sOQ_OILRES)
        row.append("," + sOQ_OILTEMP)
        row.append("," + sOQ_PCB)
        row.append("," + sOQ_SAMPLE_NO)
        row.append("," + sOQ_TEST_DATE)
        row.append("," + sOQ_TF)
        row.append("," + sOQ_WorkOrder)
        return row.toString()
    }
    // Create SQL statement
    private String qryString() {
        info("setQryString")
        StringBuffer qry = new StringBuffer()
        qry.append("select ")
        qry.append("a.script_id, ")
        qry.append("c.work_order, ")
        qry.append("c.equip_no, ")
        qry.append("e.item_name_1, ")
        qry.append("e.equip_grp_id, ")
        qry.append("e.original_doc, ")
        qry.append("e.comp_code, ")
        qry.append("e.equip_status, ")
        qry.append("to_char( c.completed_date ,'DD/MM/YYYY') comp_date, ")
        qry.append("to_char( c.completed_date ,'HH24:MI:SS') comp_time, ")
        //take days off the completion date to get the range correct
        //for the CM results.
        qry.append("to_char( c.completed_date - 3 ,'YYYYMMDD') insp_date, ")
        qry.append("to_char( c.completed_date + 90,'YYYYMMDD') insp_dateplus90days, ")
        qry.append("b.attribute_id, ")       
        qry.append("a.response, ")
        qry.append("e.plant_no, ")
        qry.append("substr(f.install_posn, 1, 12) installparent, ")
        qry.append("g.plant_no installplantno, ")
        qry.append("g.equip_status installstatus, ")
        qry.append("g.item_name_1 installname, ")
        qry.append("h.script_insp_type ")
        qry.append("from ellipse.insp_act_results a ")
        qry.append("inner join ellipse.insp_script_item b on b.script_id = a.script_id and ")
        qry.append("b.script_version_no = a.script_version_no and ")
        qry.append("b.script_item_no = a.script_item_no ")
        qry.append("inner join ellipse.insp_act_history c on c.history_id = a.history_id ")
        qry.append("inner join ellipse.insp_script_head h on h.script_id = a.script_id and ")
        qry.append("h.script_version_no = a.script_version_no ")
        qry.append("inner join ellipse.msf600 e on e.equip_no = c.equip_no ")
        qry.append("left outer join ellipse.msfx69 f on f.equip_no = c.equip_no ")
        qry.append("left outer join ellipse.msf600 g on g.equip_no = substr(f.install_posn, 1, 12) ")
        qry.append("where b.attribute_id = 'OIL02' ")
        qry.append("and a.response = 'YES' ")
        qry.append("and (c.completed_date between to_date ('" + batchParams.paramInspDateFrom + "', 'yyyymmdd') ")
        qry.append("and to_date ('" + batchParams.paramInspDateTo + "', 'yyyymmdd')) ")
        qry.append("order by e.equip_no, c.completed_date ")
        info(qry.toString())
        return qry.toString()
    } 
    private void printControlReportSummary() {
        info("printControlReportSummary")

        Trbosra.write(String.format("\nNumber of Inspections Read           : % 10d"+
                           "\nInspections Skipped                  : % 10d"+
                           "\nInspections Written                  : % 10d"+
                           "\nInspections with OA (GAS) records    : % 10d"+
                           "\nInspections with OQ (FLUID) records  : % 10d"+
                           "\nInspections missing records          : % 10d",
                           inspRead, inspSkipped, inspWrite, oaRead, oqRead, inspMissing))        
    }
    private void printBatchReport(){
        info("printBatchReport")
        if (csvTrtosrWriter != null) {
            csvTrtosrWriter.close()
            info("Adding TRTOSR CSV into Request.")
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(trtosrFile,
                        "text/comma-separated-values", CSV_TRTOSR);
            }
        }
    }
    private class InspResults{
        String scriptId = ""
        String workOrder = ""
        String equipNo = ""
        String itemName = ""
        String equipGrpId = ""
        String originalDoc = ""
        String compCode = ""
        String equipStatus = ""
        String compDate = "" //completion date
        String compTime = "" //completion time
        String inspDate = "" // to_char( c.completed_date ,'YYYYMMDD') insp_date,
        String inspDatePlus90 = "" //    to_char( c.completed_date + 90,'YYYYMMDD') insp_dateplus90days,
        String attributeId = ""        
        String response = ""
        String plantNo = ""
        String installParent = "" //may be null
        String installPlantNo = "" //may be null
        String installStatus = "" //may be null
        String installName = "" //may be null
        String scriptInspType
    }

    private void processOilInspections() {
    // Main Processing
    // Read inspections.
        info("processOilInspections")
        //set error condition
        bErrorOccured = true 
        if (batchParams.paramRegionPrefix !=null) {
            info("Filter Prefix: " + batchParams.paramRegionPrefix)
        }
        def sql = new Sql(dataSource)
        List <InspResults> insResults = new ArrayList <InspResults>()
        sql.eachRow(qryString(), {insResults.add(getResults(it))} )
        inspRead = insResults.size()
        insResults.each { InspResults insR ->
            initialiseCSVfields()
            sEquipNo = insR.getEquipNo()
            if (insR.getPlantNo() !=null) {
                sPlantNo = insR.getPlantNo()
            }
            sItemName = insR.getItemName()
            if (insR.getEquipGrpId() !=null) {
                sEquipGrpId = insR.getEquipGrpId()
            }
            if (insR.getOriginalDoc() !=null) {
                sOriginalDoc = insR.getOriginalDoc() 
            }
            if (insR.getCompCode() !=null) {
                sCompCode = insR.getCompCode()
            }
            if (insR.getEquipStatus() !=null) {
                sEquipStatus = insR.getEquipStatus()
            }
            sCompDate = insR.getCompDate()
            sCompTime = insR.getCompTime()
            sInspDate = insR.getInspDate()
            sInspDatePlus90 = insR.getInspDatePlus90()
            if (insR.getInstallPlantNo() !=null) {
                sInstallPlantNo = insR.getInstallPlantNo()
            }
            if (insR.getInstallStatus() !=null) {
                sInstallStatus = insR.getInstallStatus()
            }
            if (insR.getInstallName() !=null) {
                sInstallName = insR.getInstallName()
            }
            sScriptInspType = insR.getScriptInspType()
            sWorkOrder = insR.getWorkOrder()
            if (insR.getInstallParent() !=null) {
                sInstallParent = insR.getInstallParent()
            }
            if (batchParams.paramRegionPrefix !=null && batchParams.paramRegionPrefix.equals("")) { 
                writeCsvReport()
            } else {
                if (sInstallPlantNo !=null && sInstallPlantNo.startsWith(batchParams.paramRegionPrefix)) {
                    writeCsvReport()
                } else if (sPlantNo !=null && sPlantNo.startsWith(batchParams.paramRegionPrefix)) {
                    writeCsvReport()
                } else {
                    debug("No match on Installation Plant: " + sInstallPlantNo + " or Plant: " + sPlantNo + " add to skipped count")
                    inspSkipped++
                }
            }
        }
        //set no error - all sections completed without exception.
        bErrorOccured = false 
    }
    private InspResults getResults(Object it){
        InspResults inspRes = new InspResults()
        inspRes.setScriptId(it.script_id)
        inspRes.setWorkOrder(it.work_order)
        inspRes.setEquipNo(it.equip_no)
        inspRes.setItemName(it.item_name_1)
        inspRes.setEquipGrpId(it.equip_grp_id)
        inspRes.setOriginalDoc(it.original_doc)
        inspRes.setCompCode(it.comp_code)
        inspRes.setEquipStatus(it.equip_status)
        inspRes.setCompDate(it.comp_date)
        inspRes.setCompTime(it.comp_time)
        inspRes.setInspDate(it.insp_date)
        inspRes.setInspDatePlus90(it.insp_dateplus90days)
        inspRes.setAttributeId(it.attribute_id)      
        inspRes.setResponse(it.response)
        inspRes.setPlantNo(it.plant_no)
        inspRes.setInstallParent(it.installparent)
        inspRes.setInstallPlantNo(it.installplantno)
        inspRes.setInstallStatus(it.installstatus)
        inspRes.setInstallName(it.installname)
        inspRes.setScriptInspType(it.script_insp_type)
        return inspRes       
    }
    private class InspCMResults{
        String compPosData = ""
        String condMonMeas = ""
        String measDate = ""
        String measTime = ""
        BigDecimal measureValue 
    }
    private InspCMResults getCMResults(Object it){
        InspCMResults inspCMRes = new InspCMResults()
        inspCMRes.setCompPosData(it.comp_pos_data)
        inspCMRes.setCondMonMeas(it.cond_mon_meas)
        inspCMRes.setMeasDate(it.meas_date)
        inspCMRes.setMeasTime(it.meas_time)
        inspCMRes.setMeasureValue(it.measure_value)
        return inspCMRes       
    }
    private retrieveCMResults() {
        debug("retrieveCMResults for Equipment: $sEquipNo ")
        def cmsql = new Sql(dataSource)
        bOAset = false
        bOQset = false
        List <InspCMResults> insCMResults = new ArrayList <InspCMResults>()
        cmsql.eachRow(qryCMString(), {insCMResults.add(getCMResults(it))} )
        debug("Found ${insCMResults.size()} CM Results")
       /*
        * Results from the sql will have a set of OA and OQ values.
        * For each OA and OQ set there are some values that will be repeated. 
        * Set the values once per set of results.
        * CM Position and Measure Date and Time represent an instance of a 
        * set of CM results. Once the instance is obtained later results can be 
        * discarded. This is to cope with the possibility that multiple results
        * have been loaded with the 90 days.
        */
        insCMResults.each { InspCMResults insCMR ->
            debug("Found full getCompPosData: ${insCMR.getCompPosData()}")
            sCompMonPos = (insCMR.getCompPosData()).padRight(15)
            sCompMonPos = sCompMonPos.substring(13,15)
            debug("Found sCompMonPos: $sCompMonPos")
            sTempMeas = insCMR.getCondMonMeas()
            debug("Found $sCompMonPos Measurement Type: $sTempMeas")
            sTempValue = (insCMR.getMeasureValue()).toString()
            debug("Found $sCompMonPos Measurement Type: $sTempMeas Value: $sTempValue")
            if ((!bOAset) && sCompMonPos.equals("OA")) {
                sOA_Position = (insCMR.getCompPosData())[6..12]
                sOA_CMType = sCompMonPos
                sOA_Date = (insCMR.getMeasDate())[6,7] + "/" + (insCMR.getMeasDate())[4,5] + "/" + (insCMR.getMeasDate())[0,1,2,3] 
                sOA_Time = (insCMR.getMeasTime())[0,1] + ":" + (insCMR.getMeasTime())[2,3] + ":" + (insCMR.getMeasTime())[4,5] 
                oaRead++
                //assemble OA key
                sOAKey = insCMR.getMeasDate() + insCMR.getMeasTime()
                bOAset = true
            }
            if ((!bOQset) && sCompMonPos.equals("OQ")) {
                sOQ_Position = (insCMR.getCompPosData())[6..12]
                sOQ_CMType = sCompMonPos
                sOQ_Date = (insCMR.getMeasDate())[6,7] + "/" + (insCMR.getMeasDate())[4,5] + "/" + (insCMR.getMeasDate())[0,1,2,3] 
                sOQ_Time = (insCMR.getMeasTime())[0,1] + ":" + (insCMR.getMeasTime())[2,3] + ":" + (insCMR.getMeasTime())[4,5]
                oqRead++
                //assemble OQ key
                sOQKey = insCMR.getMeasDate() + insCMR.getMeasTime()
                bOQset = true
            }
            //Ff the type is OA and the date/time is the same, the CM result is in the same set.
            if ((sCompMonPos.equals("OA")) && (sOAKey.equals(insCMR.getMeasDate() + insCMR.getMeasTime()))) {
                if (sTempMeas.trim().equals("C2H2")) {
                    sOA_C2H2 = sTempValue
                }
                if (sTempMeas.trim().equals("C2H4")) {
                    sOA_C2H4 = sTempValue
                }
                if (sTempMeas.trim().equals("C2H6")) {
                    sOA_C2H6 = sTempValue
                }
                if (sTempMeas.trim().equals("CH4")) {
                    sOA_CH4 = sTempValue
                }
                if (sTempMeas.trim().equals("CO")) {
                    sOA_CO = sTempValue
                }
                if (sTempMeas.trim().equals("CO2")) {
                    sOA_CO2 = sTempValue
                }
                if (sTempMeas.trim().equals("H2")) {
                    sOA_H2 = sTempValue
                }
                if (sTempMeas.trim().equals("N2")) {
                    sOA_N2 = sTempValue
                }
                if (sTempMeas.trim().equals("O2")) {
                    sOA_O2 = sTempValue
                }
                if (sTempMeas.trim().equals("OILTEMP")) {
                    sOA_OILTEMP = sTempValue
                }
                if (sTempMeas.trim().equals("SAMPLE-NO")) {
                    sOA_SAMPLE_NO = sTempValue
                }
                if (sTempMeas.trim().equals("TCG")) {
                    sOA_TCG = sTempValue
                }
                if (sTempMeas.trim().equals("TESTDATE")) {
                    sOA_TEST_DATE = sTempValue
                }
                if (sTempMeas.trim().equals("TOAE")) {
                    sOA_TOAE = sTempValue
                }
                if (sTempMeas.trim().equals("WO-NUMBER")) {
                    sOA_WorkOrder = sTempValue
                }
            }
            if ((sCompMonPos.equals("OQ")) && (sOQKey.equals(insCMR.getMeasDate() + insCMR.getMeasTime()))) {
                if (sTempMeas.trim().equals("AF")) {
                    sOQ_AF = sTempValue
                }
                if (sTempMeas.trim().equals("DBS")) {
                    sOQ_DBS = sTempValue
                }
                if (sTempMeas.trim().equals("DDF")) {
                    sOQ_DDF = sTempValue
                }
                if (sTempMeas.trim().equals("F")) {
                    sOQ_F = sTempValue
                }
                if (sTempMeas.trim().equals("FFA")) {
                    sOQ_FFA = sTempValue
                }
                if (sTempMeas.trim().equals("H2O")) {
                    sOQ_H2O = sTempValue
                }
                if (sTempMeas.trim().equals("HMF")) {
                    sOQ_HMF = sTempValue
                }
                if (sTempMeas.trim().equals("IFT")) {
                    sOQ_IFT = sTempValue
                }
                if (sTempMeas.trim().equals("MEYERS")) {
                    sOQ_MEYERS = sTempValue
                }
                if (sTempMeas.trim().equals("MF")) {
                    sOQ_MF = sTempValue
                }
                if (sTempMeas.trim().equals("OILACID")) {
                    sOQ_OILACID = sTempValue
                }
                if (sTempMeas.trim().equals("OILRES")) {
                    sOQ_OILRES = sTempValue
                }
                if (sTempMeas.trim().equals("OILTEMP")) {
                    sOQ_OILTEMP = sTempValue
                }
                if (sTempMeas.trim().equals("PCB")) {
                    sOQ_PCB = sTempValue
                }
                if (sTempMeas.trim().equals("SAMPLE-NO")) {
                    sOQ_SAMPLE_NO = sTempValue
                }
                if (sTempMeas.trim().equals("TESTDATE")) {
                    sOQ_TEST_DATE = sTempValue
                }
                if (sTempMeas.trim().equals("TF")) {
                    sOQ_TF = sTempValue
                }
                if (sTempMeas.trim().equals("WO-NUMBER")) {
                    sOQ_WorkOrder = sTempValue
                }
            }
            if ((sCompMonPos.equals("OA")) && (!sOAKey.equals(insCMR.getMeasDate() + insCMR.getMeasTime()))) {
                debug("Multiple results for $sEquipNo $sCompMonPos")
            }
            if ((sCompMonPos.equals("OQ")) && (!sOQKey.equals(insCMR.getMeasDate() + insCMR.getMeasTime()))) {
                debug("Multiple results for $sEquipNo $sCompMonPos")
            }
        } 
    }
    // Create SQL statement for retrieving CM results
    private String qryCMString() {
        debug("setQryCMString - start")
        StringBuffer qryCM = new StringBuffer()
        qryCM.append("select ")
        qryCM.append("comp_pos_data, ")
        qryCM.append("cond_mon_meas, ")
        qryCM.append("substr((99999999999999 - rev_meas_data), 1, 8) meas_date, ")
        qryCM.append("substr((99999999999999 - rev_meas_data), 9, 6) meas_time, ")
        qryCM.append("measure_value ")
        qryCM.append("from ellipse.msf345 ")
        qryCM.append("where equip_no = '" + sEquipNo +"' ")
        qryCM.append("and (substr(comp_pos_data, 14, 2) = 'OA' ")
        qryCM.append("or substr(comp_pos_data, 14, 2) = 'OQ') ")
        qryCM.append("and (rev_meas_data between (99999999999999 - ${sInspDatePlus90}999999) ")
        qryCM.append("and (99999999999999 - ${sInspDate}999999)) ")
        qryCM.append("order by rev_meas_data desc, comp_pos_data, cond_mon_meas ")
        debug(qryCM.toString())
        return qryCM.toString()
    } 
}

/*run script*/  
ProcessTRBOSR process = new ProcessTRBOSR()
process.runBatch(binding);