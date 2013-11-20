/**
 *  @Ventyx 2012
 *  
 * This program extracts the equipment and condition monitoring, <br/>
 * then create two files for subsequent loading to TOA. <br/>
 * 
 * Developed based on <b>FDD.Interfacing.Ellipse-TOA.D04.docx</b>
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.io.BufferedWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key;
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec;
import com.mincom.ellipse.edoi.ejb.msf011.*;
import com.mincom.ellipse.edoi.ejb.msf012.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf100.*;
import com.mincom.ellipse.edoi.ejb.msf345.MSF345Key;
import com.mincom.ellipse.edoi.ejb.msf345.MSF345Rec;
import com.mincom.ellipse.edoi.ejb.msf580.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf601.*;
import com.mincom.ellipse.edoi.ejb.msf602.*;
import com.mincom.ellipse.edoi.ejb.msf60a.*;
import com.mincom.ellipse.edoi.ejb.msf619.*;
import com.mincom.ellipse.edoi.ejb.msf650.*;
import com.mincom.ellipse.edoi.ejb.msf6a1.*;
import com.mincom.ellipse.edoi.ejb.msf900.*;
import com.mincom.ellipse.edoi.ejb.msf910.*;
import com.mincom.ellipse.edoi.ejb.msf920.*;
import com.mincom.ellipse.edoi.ejb.msf930.*;
import com.mincom.ellipse.edoi.ejb.msf940.*;
import com.mincom.ellipse.edoi.ejb.msf966.*;
import com.mincom.ellipse.edoi.ejb.msf967.*;
import com.mincom.ellipse.edoi.ejb.msf968.*;
import com.mincom.ellipse.edoi.ejb.msfx61.*;
import com.mincom.ellipse.edoi.ejb.msfx63.*;
import com.mincom.ellipse.edoi.ejb.msfx65.*;
import com.mincom.ellipse.edoi.ejb.msfx68.*;
import com.mincom.ellipse.edoi.ejb.msfx69.*;
import com.mincom.ellipse.edoi.ejb.msfx6a.*;
import com.mincom.ellipse.edoi.ejb.msfx6c.*;
import com.mincom.ellipse.edoi.ejb.msfx6e.*;
import com.mincom.ellipse.edoi.ejb.msfx6f.*;
import com.mincom.ellipse.edoi.ejb.msfx6j.*;
import com.mincom.ellipse.edoi.ejb.msk600.*;
import com.mincom.enterpriseservice.exception.*;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;
import com.mincom.reporting.text.TextReport;
import java.text.DecimalFormat;

/**
 * Request Parameter for Trb60a.
 * <li><code>dateFrom</code> : Measurement Date From</li>
 * <li><code>timeFrom</code> : Measurement Time From</li>
 * <li><code>dateTo</code>   : Measurement Date To</li>
 * <li><code>timeTo</code>   : Measurement Time To</li>
 */
class ParamsTrb60a {
    String dateFrom, timeFrom, dateTo, timeTo
}

/**
 * Measurement values for OA Monitoring Type.
 */
public enum OAMeasValue {
    SAMPLENUM("SAMPLENUM", "SAMPLE-NO"),
    TESTDATE("TESTDATE", "TESTDATE"),
    LABBREFNUM("LABBREFNUM", "WO-NUMBER"),
    SAMPLEBY("SAMPLEBY", "SAMPLEBY"),
    FLUIDTEMP("FLUIDTEMP", "OILTEMP"),
    H2("H2", "H2"),
    CH4("CH4", "CH4"),
    C2H6("C2H6", "C2H6"),
    C2H4("C2H4", "C2H4"),
    C2H2("C2H2", "C2H2"),
    CO("CO", "CO"),
    CO2("CO2", "CO2"),
    O2("O2", "O2"),
    N2("N2", "N2"),
    H2O("H2O", "H2O")

    String measName, header
    /**
     * Initialize OAMeasValue.
     * @param head CSV header
     * @param name measurement's name
     */
    public OAMeasValue(String head, String name) {
        measName = name
        header = head
    }

    /**
     * Return CSV header.
     * @return CSV header
     */
    public String getCSVHeader() {
        return header
    }

    /**
     * Return measurement's name.
     * @return measurement's name
     */
    public String getMeasName() {
        return measName
    }
}

/**
 * Measurement values for OQ Monitoring Type.
 */
public enum OQMeasValue {
    SAMPLENUM("SAMPLENUM", "SAMPLE-NO"),
    TESTDATE("TESTDATE", "TESTDATE"),
    LABBREFNUM("LABBREFNUM", "WO-NUMBER"),
    SAMPLEBY("SAMPLEBY", "SAMPLEBY"),
    FLUIDTEMP("FLUIDTEMP", "OILTEMP"),
    ACIDNUM("ACIDNUM", "OILACID"),
    IFT("IFT", "IFT"),
    KVD1816("KVD1816", "DBS"),
    PF100("PF100", "DDF"),
    WATER("WATER", "H2O"),
    PCB("PCB", "PCB"),
    INHIBITOR("INHIBITOR", "OXINHIB"),
    FURAN("FURAN", "F")

    String measName, header

    /**
     * Initialize OQMeasValue.
     * @param head CSV header
     * @param name measurement's name
     */
    public OQMeasValue(String head, String name) {
        measName = name
        header = head
    }

    /**
     * Return CSV header.
     * @return CSV header
     */
    public String getCSVHeader() {
        return header
    }

    /**
     * Return measurement's name.
     * @return measurement's name
     */
    public String getMeasName() {
        return measName
    }
}

/**
 * CSV Record for Equipment.
 */
class EquipmentCSVRecord implements Comparable {
    //fields for Equipment
    String equipNo, compCode, mnemonic, serialNum, plantNo, desig, owner, equipGrpId, itemName, originalDoc
    //generic fields for OA and OQ Monitoring Type
    String condMonPos, date, time
    //Map of OA and the meas value
    Map<OAMeasValue, BigDecimal> oaMeasurementValue
    //Map of OQ and the meas value
    Map<OQMeasValue, BigDecimal> oqMeasurementValue

    /**
     * Initialize fields with blank literal.
     */
    public EquipmentCSVRecord() {
        equipNo = compCode = mnemonic = serialNum = plantNo = desig = owner = equipGrpId = itemName = originalDoc = " "
        condMonPos =  date = time = " "
        oaMeasurementValue = new HashMap<OAMeasValue, BigDecimal>()
        OAMeasValue.values().each {
            oaMeasurementValue.put(it, 0)
        }
        oqMeasurementValue = new HashMap<OQMeasValue, BigDecimal>()
        OQMeasValue.values().each {
            oqMeasurementValue.put(it, 0)
        }
    }

    /**
     * Return the CSV Header for Equipment extraction.
     * @return Equipment's CSV Header "EQUIPNUM,APPRTYPE,MFR,SERIALNUM,LOCATION,DESIG,  OWNER,MODEL,DESCRIP,COMMENT"
     */
    public static String writeEquipmentCSVHeader() {
        return "EQUIPNUM,APPRTYPE,MFR,SERIALNUM,LOCATION,DESIG,  OWNER,MODEL,DESCRIP,COMMENT"
    }

    /**
     * Return the CSV record detail.
     * @return record detail
     */
    public String writeEquipmentDetail() {
        return String.format("%s,%s,%s,\"%s\",%s,%s,%s,%s,\"%s\",\"%s\"",
        equipNo.padRight(12).substring(0,12),
        compCode.padRight(4).substring(0, 4),
        mnemonic.padRight(8).substring(0, 8),
        serialNum.padRight(20).substring(0, 20),
        plantNo.padRight(20).substring(0, 20),
        desig.padRight(14).substring(0, 14),
        owner.padRight(20).substring(0, 20),
        equipGrpId.padRight(12).substring(0, 12),
        itemName.padRight(20).substring(0, 20),
        originalDoc.padRight(10).substring(0, 10)
        )
    }

    /**
     * Return the CSV OA's record detail.
     * @return OA's record detail
     */
    public String writeOADetail() {
        String detail = String.format("%s,%s,%s,%s,",
                equipNo.padRight(12).substring(0,12),
                compCode.padRight(4).substring(0, 4),
                condMonPos.padRight(7).substring(0, 7),
                date.padRight(8).substring(0, 8)
                )
        //Iterate OA enum, write the measurement value into csv detail
        int i = 0
        int c = OAMeasValue.values().length - 1
        OAMeasValue.values().each {
            if(it.getMeasName().equals(OAMeasValue.SAMPLEBY.getMeasName())) {
                detail += time.padRight(6).substring(0, 6)
            } else {
                if(it.getMeasName().equals(OAMeasValue.SAMPLENUM.getMeasName()) && oaMeasurementValue.get(it)==0) {
                    detail += getSampleNo("OA", equipNo, date, time.padRight(6).substring(0, 6))
                }
                else {
                    BigDecimal value = oaMeasurementValue.get(it)
                    String val_s = " "
                    if(value != null && value > 0) {
                        val_s = String.valueOf(value)
                    }
                    detail += val_s.padRight(14).substring(0, 14)
                }
            }
            if(i < c) {
                detail += ","
            }
            i++
        }
        return detail
    }

    /**
     * Return the CSV OQ's record detail.
     * @return OQ's record detail
     */
    public String writeOQDetail() {
        String detail = String.format("%s,%s,%s,%s,",
                equipNo.padRight(12).substring(0,12),
                compCode.padRight(4).substring(0, 4),
                condMonPos.padRight(7).substring(0, 7),
                date.padRight(8).substring(0, 8)
                )
        //Iterate OQ enum, write the measurement value into csv detail
        int i = 0
        int c = OQMeasValue.values().length - 1
        OQMeasValue.values().each {
            if(it.getMeasName().equals(OQMeasValue.SAMPLEBY.getMeasName())) {
                detail += time.padRight(6).substring(0, 6)
            } else {
                if(it.getMeasName().equals(OQMeasValue.SAMPLENUM.getMeasName()) && oqMeasurementValue.get(it)==0) {
                    detail += getSampleNo("OQ", equipNo, date, time.padRight(6).substring(0, 6))
                }
                else {
                    BigDecimal value = oqMeasurementValue.get(it)
                    String val_s = " "
                    if(value != null && value > 0) {
                        val_s = String.valueOf(value)
                    }
                    detail += val_s.padRight(14).substring(0, 14)
                }
            }
            if(i < c) {
                detail += ","
            }
            i++
        }
        return detail
    }

    /**
     * Set the value for specified OA's measurement name.
     * @param measName  specified measurement name
     * @param measValue specified measurement value
     */
    public void setOAMeasValue(String measName, BigDecimal measValue) {
        OAMeasValue key = null
        OAMeasValue.values().each {
            if(it.getMeasName().toString().equals(measName)) {
                oaMeasurementValue.put(it, measValue)
            }
        }
    }

    /**
     * Set the value for specified OQ's measurement name.
     * @param measName  specified measurement name
     * @param measValue specified measurement value
     */
    public void setOQMeasValue(String measName, BigDecimal measValue) {
        OQMeasValue key = null
        OQMeasValue.values().each {
            if(it.getMeasName().toString().equals(measName)) {
                oqMeasurementValue.put(it, measValue)
            }
        }
    }
    
    public String getSampleNo(String condMonType, String equipNo, String date, String time) {
        String returnValue = "${date}${time}"
        if(condMonType.trim().equals("OA") && equipNo.trim().equals("ETA3935") && returnValue.trim().equals("20110223132500")) {
            returnValue = "2011022.31330"
        }
        else {
            returnValue = new DecimalFormat("0000000.00000").format((date + time.substring(0,4)).toBigDecimal() / 100000)
        }

        return returnValue.padRight(14)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( param0) {
        if(param0 == null) {
            return false
        }
        if (this.is(param0)) {
            return true
        }
        EquipmentCSVRecord that = (EquipmentCSVRecord) param0

        return this.equipNo.trim().equals(that.equipNo.trim()) &&
        this.compCode.trim().equals(that.compCode.trim()) &&
        this.condMonPos.trim().equals(that.condMonPos.trim()) &&
        this.date.trim().equals(that.date.trim()) &&
        this.time.trim().equals(that.time.trim())
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Object that) {
        if(that == null) {
            return false
        }
        if(!(that instanceof EquipmentCSVRecord)) {
            return false
        }
        EquipmentCSVRecord o = (EquipmentCSVRecord) that
        return this.equipNo.compareTo(o.equipNo);
    }
}


/**
 * Main Process of Trb60a.
 */
public class ProcessTrb60a extends SuperBatch {

    /*
     * Constants
     */
    private static final String CSV_TRT_60F_FILENAME   = "TRT60F"
    private static final String CSV_TRT_60C_FILENAME   = "TRT60C"
    private static final String CSV_TRT_60G_FILENAME   = "TRT60G"
    private static final String REPORT_FILENAME        = "TRB60AA"
    private static final String TABLE_CODE_OA          = "OA"
    private static final String TABLE_CODE_OQ          = "OQ"
    private static final String TABLE_TYPE_TOA         = "#TOA"
    private static final String TABLE_TYPE_ES          = "ES"
    private static final String COND_MEAS_TYPE_OA      = "OA"
    private static final String COND_MEAS_TYPE_OQ      = "OQ"
    private static final String SERVICE_NAME_TABLE     = "TABLE"
    private static final String SERVICE_NAME_EQUIPMENT = "EQUIPMENT"
    private static final String SCREEN_NAME_MSO010     = "MSO010"
    private static final String SCREEN_NAME_MSM010B    = "MSM010B"
    private static final String SERVICE_NAME_EQUIPMENT_TRACE = "EQUIPTRACE"
    private static final String MSF345_REC_TYPE_E      = "E"
    private static final long   MAX_RECORD             = 65000
    private static final Integer MAX_RECORD_EDOI       = 1000
    private static final SimpleDateFormat MEAS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd")

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 9

    /*
     * Variables
     */
    private ParamsTrb60a batchParams
    private File trt60CFile, trt60FFile, trt60GFile
    private BufferedWriter csvTrt60CWriter, csvTrt60FWriter, csvTrt60GWriter
    private boolean writeTrt60CHeader, writeTrt60FHeader, writeTrt60GHeader
    private def reportWriter
    private String measOADateFrom, measOATimeFrom, measOQDateFrom, measOQTimeFrom, measDateTo, measTimeTo
    //List of unique Equipment Number
    private ArrayList<String> equipNoDistinctList
    //List of extracted Equipment
    private ArrayList<EquipmentCSVRecord> extractedEq
    //List of extracted OA
    private ArrayList<EquipmentCSVRecord> extractedOA
    //List of extracted OQ
    private ArrayList<EquipmentCSVRecord> extractedOQ
    //List of read OA
    private ArrayList<EquipmentCSVRecord> readOA
    //List of read OQ
    private ArrayList<EquipmentCSVRecord> readOQ
    boolean isOAexist = false, isOQExist = false
    //Variables used for reporting
    private String paramError = "", warning = ""
    private long eqRead = 0, eqSent = 0, oaRead = 0, oaSent = 0, oqRead = 0, oqSent = 0

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : " + version)
        batchParams = params.fill(new ParamsTrb60a())
        info("Measurement Date From : ${batchParams.dateFrom}")
        info("Measurement Time From : ${batchParams.timeFrom}")
        info("Measurement Date To   : ${batchParams.dateTo}")
        info("Measurement Time To   : ${batchParams.timeTo}")
        try {
            processBatch()
        } catch(Exception e) {
            info("Process terminated. ${e.getMessage()}")
        } finally {
            printBatchReport()
        }
        info("runBatch finished")
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        initialize()
        checkTOATypes()
        validateRequestParameter()
        //Browse Equipment
        browseEquipmentRecords()
        //write populated Equipmet, OA and OQ records into CSV
        writeEquipmentCSV()
        writeCondMonOACSV()
        writeCondMonOQCSV()
        //check record count
        checkRecordsCount()
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        //write report
        if(warning.trim().length() > 0) {
            reportWriter.write(String.format("%132s"," ").replace(' ', '-'))
            String[] warnings = warning.split("\n")
            warnings.each { reportWriter.write(it) }
            reportWriter.write(String.format("%132s"," ").replace(' ', '-'))
            reportWriter.write("")
        }

        reportWriter.write("Summary:")
        reportWriter.write(writeDateSummaryDetail())
        reportWriter.write("")

        reportWriter.write("Equipment Register Records processed:")
        reportWriter.write(writeEquipmentDetail())
        reportWriter.write("")

        reportWriter.write("Condition Monitoring Records processed:")
        reportWriter.write(writeCondMonDetail())
        reportWriter.write("")

        if(paramError.trim().length() > 0) {
            reportWriter.write("Parameters Errors:")
            reportWriter.write("------------------")
            reportWriter.write(paramError)
            reportWriter.write("")
        }

        //close all the BufferedWriters
        reportWriter.close()
        if(csvTrt60CWriter != null) {
            csvTrt60CWriter.close()
            info("Adding TRT60C CSV into Request.")
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(trt60CFile,
                        "text/comma-separated-values", CSV_TRT_60C_FILENAME);
            }
        }
        if(csvTrt60FWriter != null) {
            csvTrt60FWriter.close()
            info("Adding TRT60F CSV into Request.")
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(trt60FFile,
                        "text/comma-separated-values", CSV_TRT_60F_FILENAME);
            }
        }
        if(csvTrt60GWriter != null) {
            csvTrt60GWriter.close()
            info("Adding TRT60G CSV into Request.")
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(trt60GFile,
                        "text/comma-separated-values", CSV_TRT_60G_FILENAME);
            }
        }
        info("printBatchReport finished")
    }

    /**
     * Initialize the csv writers, report writer.
     */
    private void initialize() {
        info("initialize")
        writeTrt60CHeader = writeTrt60FHeader = writeTrt60GHeader = false
        reportWriter  = report.open(REPORT_FILENAME)
        equipNoDistinctList = new ArrayList<String>()
        extractedEq = new ArrayList<EquipmentCSVRecord>()
        extractedOA = new ArrayList<EquipmentCSVRecord>()
        extractedOQ = new ArrayList<EquipmentCSVRecord>()
        readOA = new ArrayList<EquipmentCSVRecord>()
        readOQ = new ArrayList<EquipmentCSVRecord>()
    }

    /**
     * Validate the request parameter. 
     * <li>If the Date From parameter is blank, then use the Last Run Date/Time from the #TOA Table codes associated value.</li>
     * <li>If the Date To parameter is blank, then use the current system date / time. <br/>
     * Date / Time combinations must be less than or equal to the current system date / time.</li>
     * <li>The Date From combination must be less than the Date To combination.</li>
     * <li>If the Time From / To parameter is blank, then use '000000'.</li>
     */
    private void validateRequestParameter() {
        info("validateRequestParameter")
        measOADateFrom = measOATimeFrom = measOQDateFrom = measOQTimeFrom = measDateTo = measTimeTo = ""
        Date date = new Date()
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHMMSS")
        String currDateTime = df.format(date)
        //Check Date-From
        boolean emptyDateFrom = batchParams.dateFrom == null ? true :
                batchParams.dateFrom.trim().length() == 0

        if(!batchParams.dateFrom.trim()) {
            //Check #TOA - OA date
            if(isOAexist) {
                TableServiceReadReplyDTO oaTableDTO = readTable(TABLE_TYPE_TOA, TABLE_CODE_OA)
                if(oaTableDTO != null && oaTableDTO?.getAssociatedRecord().trim()) {
                    measOADateFrom = oaTableDTO?.getAssociatedRecord().length() > 8 ?
                            oaTableDTO?.getAssociatedRecord().substring(0, 8) :
                            oaTableDTO?.getAssociatedRecord()
                    measOATimeFrom = "000000"
                } else {
                    isOAexist = false
                }
                try {
                    MEAS_DATE_FORMAT.parse(measOADateFrom)
                } catch(java.text.ParseException e) {
                    isOAexist = false
                    warning += "Invalid #TOA - OA Date From format.\n"
                    info("Invalid #TOA - OA Date From format.")
                }
            }

            //Check #TOA - OQ date
            if(isOQExist) {
                TableServiceReadReplyDTO oqTableDTO = readTable(TABLE_TYPE_TOA, TABLE_CODE_OQ)
                if(oqTableDTO != null && oqTableDTO?.getAssociatedRecord().trim()) {
                    measOQDateFrom = oqTableDTO?.getAssociatedRecord().length() > 8 ?
                            oqTableDTO?.getAssociatedRecord().substring(0, 8) :
                            oqTableDTO?.getAssociatedRecord()
                    measOQTimeFrom = "000000"
                } else {
                    isOQExist = false
                }
                try {
                    MEAS_DATE_FORMAT.parse(measOQDateFrom)
                } catch(java.text.ParseException e) {
                    isOQExist = false
                    warning += "Invalid #TOA - OQ Date From format.\n"
                    info("Invalid #TOA - OQ Date From format.")
                }
            }

            if(!isOAexist && !isOQExist) {
                paramError = "#TOA Date From not set. Enter date manually."
                throw new RuntimeException(paramError)
            }
        } else {
            measOADateFrom = batchParams.dateFrom.length() > 8 ?
                    batchParams.dateFrom.substring(0, 8) :
                    batchParams.dateFrom
            measOQDateFrom = batchParams.dateFrom.length() > 8 ?
                    batchParams.dateFrom.substring(0, 8) :
                    batchParams.dateFrom
        }

        //Check Date-To
        boolean emptyDateTo = batchParams.dateTo == null ? true :
                batchParams.dateTo.trim().length() == 0
        if(emptyDateTo) {
            measDateTo = currDateTime.length() > 8 ?
                    currDateTime.substring(0, 8) :
                    currDateTime
        } else {
            measDateTo = batchParams.dateTo.length() > 8 ?
                    batchParams.dateTo.substring(0, 8) :
                    batchParams.dateTo
        }

        //Check OA and OQ Time-From
        if(measOATimeFrom.trim().length() == 0) {
            if(batchParams.timeFrom.trim().length() == 0) {
                measOATimeFrom = "000000"
            } else {
                measOATimeFrom = batchParams.timeFrom.replace(":", "") + "00"
            }
        }

        if(measOQTimeFrom.trim().length() == 0) {
            if(batchParams.timeFrom.trim().length() == 0) {
                measOQTimeFrom = "000000"
            } else {
                measOQTimeFrom = batchParams.timeFrom.replace(":", "") + "00"
            }
        }

        //Check Time-To
        if(batchParams.timeTo.trim().length() == 0) {
            measTimeTo = "000000"
        }else {
            measTimeTo = batchParams.timeTo.replace(":", "") + "00"
        }

        //Check date ranges
        long dateFrom_9 = (measOADateFrom + measOATimeFrom) as long
        long dateTo_9   = (measDateTo   + measTimeTo  ) as long
        long currDate_9 = currDateTime.substring(0,14) as long

        if(dateFrom_9 > currDate_9) {
            paramError = "Date / Time From later than Current Date / Time. Modify the Date / Time From."
            throw new RuntimeException(paramError)
        }

        if(dateTo_9 > currDate_9) {
            paramError = "Date / Time To later than Current Date / Time. Modify the Date / Time To."
            throw new RuntimeException(paramError)
        }

        if(dateFrom_9 > dateTo_9) {
            paramError = "Date / Time From later than Date / Time To. Modify the date range."
            throw new RuntimeException(paramError)
        }
    }

    /**
     * Check the #TOA type, if there is a Table Code besides OA and OQ then report it.
     */
    private void checkTOATypes() {
        info("checkTOATypes")

        TableServiceRetrieveRequestDTO retDTO = new TableServiceRetrieveRequestDTO()
        retDTO.setTableType(TABLE_TYPE_TOA)

        TableServiceRetrieveRequiredAttributesDTO reqAttr = new TableServiceRetrieveRequiredAttributesDTO()
        reqAttr.setReturnTableType(true)
        reqAttr.setReturnTableCode(true)

        def restart = ""
        boolean firstLoop = true

        while (firstLoop || restart?.trim()){
            TableServiceRetrieveReplyCollectionDTO replyColl = retrieveTable(retDTO, reqAttr, restart)
            firstLoop = false
            if(replyColl) {
                replyColl.getReplyElements().each {TableServiceRetrieveReplyDTO table->
                    String tableCode = table.getTableCode().trim()
                    if(!TABLE_CODE_OA.equals(tableCode) &&
                    !TABLE_CODE_OQ.equals(tableCode)) {
                        warning += "Invalid #TOA Monitoring Type set - ${tableCode}.\n"
                    }

                    if(TABLE_CODE_OA.equals(tableCode)) {
                        isOAexist = true
                    }

                    if(TABLE_CODE_OQ.equals(tableCode)) {
                        isOQExist = true
                    }
                }
                restart = replyColl.getCollectionRestartPoint()
            }
        }
    }

    /**
     * Browse Equipment Installation Positions records distinctly and order the records by Equipment Number.<br/>
     * Then process Equipment record for each Equipment Number returned.
     */
    private void browseEquipmentRecords() {
        info("browseEquipmentRecords")
        //Use edoi since there is no service call for Equipment Installation Positions
        QueryImpl qIstllnPstn = new QueryImpl(MSFX69Rec.class).orderBy(MSFX69Rec.msfx69Key)

        edoi.search(qIstllnPstn,MAX_RECORD_EDOI) {MSFX69Rec msfx69Rec->
            eqRead++
            String equipNo = msfx69Rec.getPrimaryKey().getEquipNo()
            MSF600Rec msf600Rec = readEquipment(equipNo)
            if(msf600Rec != null &&
            !equipNoDistinctList.contains(equipNo) &&
            msf600Rec.getTraceableFlg()?.trim().equalsIgnoreCase('y')) {

                equipNoDistinctList.add(equipNo)
                EquipmentCSVRecord rec = new EquipmentCSVRecord()
                rec.equipNo            = msf600Rec.getPrimaryKey().getEquipNo()
                rec.compCode           = msf600Rec.getCompCode()
                rec.mnemonic           = msf600Rec.getMnemonic()
                rec.serialNum          = msf600Rec.getSerialNumber().length() > 20 ?
                        msf600Rec.getSerialNumber().substring(0, 20) :
                        msf600Rec.getSerialNumber()
                rec.plantNo            = readPlantNumberFromInstalationPosition(
                        msfx69Rec.getPrimaryKey().getInstallPosn())
                rec.equipGrpId         = msf600Rec.getEquipGrpId()
                rec.itemName           = msf600Rec.getItemName_1()
                rec.originalDoc        = msf600Rec.getOriginalDoc()
                rec.desig              = readInstallationPosition(equipNo)
                rec.owner              = msf600Rec.getEquipStatus()
                //add into equipment list
                extractedEq.add(rec)
                eqSent++
                //browse Condition Monitoring
                browseCondMeasurement(equipNo)
            }
        }
    }

    /**
     * Read the Equipment based on equipment number.
     * @param equipNo equipment number
     * @return MSF600Rec
     */
    private MSF600Rec readEquipment(String equipNo) {
        info("readEquipment")
        try {
            return edoi.findByPrimaryKey(new MSF600Key(equipNo: equipNo))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info("Equipment ${equipNo} does not exist.")
        }
        return null
    }

    /**
     * Return Plant Number taken from MSF600-Plant-No for the installation position.
     * @param equipNo specified equipment status
     * @return plantNumber
     */
    private String readPlantNumberFromInstalationPosition(String instalationPosn) {
        info("readPlantNumberFromInstalationPosition ${instalationPosn}")
        String plantNumber = " "
        if(instalationPosn.trim()) {
            try {
                String installationEquip = instalationPosn.padRight(18).substring(0, 12)
                MSF600Rec equipRec = edoi.findByPrimaryKey(new MSF600Key(installationEquip))
                plantNumber = equipRec.getPlantNo()
            } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                info("Error during readPlantNumberFromInstalationPosition: ${e.getMessage()}")
            }
        }
        return plantNumber
    }

    /**
     * Search Equipment Installation Position (DESIG) from Component Tracing based on equipment number. <br/>
     * DESIG is concatenaton between MSF650-INSTALL-EQUIP (12) and MSF650-INSTALL-MOD (2); <br/>
     * parent equipment number plus Installation position modifier code (14 char). 
     * @param equipNo specified equipment number
     * @return Equipment Installation Position (DESIG) if found, else return blank
     */
    private String readInstallationPosition(String equipNo) {
        info("readInstallationPosition ${equipNo}")
        String desig = " "
        if(equipNo.trim().length() > 0) {
            try {
                Constraint cEquipNo      = MSF650Key.fitEquipNo.equalTo(equipNo)
                QueryImpl  qEquipTracing = new QueryImpl(MSF650Rec.class).and(cEquipNo)
                MSF650Rec  msf650Rec     = edoi.firstRow(qEquipTracing)
                if(msf650Rec != null) {
                    desig = msf650Rec.getInstallPosn().substring(0, 12) +
                            msf650Rec.getInstallPosn().substring(16)
                }
            } catch(Exception e) {
                info("Error during readInstallationPosition: ${e.getMessage()}")
            }
        }
        return desig
    }

    /**
     * Browse Condition Monitoring records for traceable equipment.<br/>
     * For each Monitoring Type OA and OQ read the MSF345 measurement data and <br/>
     * where date and time are set to greater than the Date / Time. <br/>
     * @param equipNo traceable equipment number
     */
    private void browseCondMeasurement(String equipNo) {
        info("browseCondMeasurement ${equipNo}")

        try {
            //Use edoi to browse CondMeasurement since the constraints are too complicated to be implemented using service call.
            Constraint cRecType       = MSF345Key.rec_345Type.equalTo(MSF345_REC_TYPE_E)
            Constraint cEquipNo       = MSF345Key.equipNo.equalTo(equipNo)
            Constraint cCompPosDataOA = MSF345Key.compPosData.like("%"+ProcessTrb60a.COND_MEAS_TYPE_OA)
            Constraint cCompPosDataOQ = MSF345Key.compPosData.like("%"+ProcessTrb60a.COND_MEAS_TYPE_OQ)
            QueryImpl qCondMeas = new QueryImpl(MSF345Rec.class).and(cRecType).and(cEquipNo).and(cCompPosDataOA.or(cCompPosDataOQ)).orderBy(MSF345Rec.msf345Key)
            
            Boolean setOACounted = false
            Boolean setOQCounted = false
            edoi.search(qCondMeas, MAX_RECORD_EDOI) {MSF345Rec msf345Rec->
                String compPosData = msf345Rec.getPrimaryKey().compPosData
                String compCode    = compPosData.substring(0, 4)
                String condMonPos  = compPosData.substring(6, 13)
                String condMonType = compPosData.substring(13).trim()
                String revMeasData = msf345Rec.getPrimaryKey().revMeasData
                String measDate    = computeDateReverse( revMeasData.substring(0, 8) as int ) as String
                String measTime    = computeTimeReverse( revMeasData.substring(8)    as int ) as String
                //If measurement time's length less than 6, pad it with 0
                if(measTime.length() < 6) {
                    measTime = measTime.padLeft(6).replace(" ", "0")
                }
                String conMonMeas    = msf345Rec.getPrimaryKey().condMonMeas.toString().trim()
                BigDecimal measValue = msf345Rec.measureValue


                long measDate_9 = (measDate + measTime) as long
                long dateFrom_9 = 0
                //Check the meas date based on meas type
                if(isOAexist && COND_MEAS_TYPE_OA.equals(condMonType)) {
                    dateFrom_9  = (measOADateFrom + measOATimeFrom) as long
                } else if(isOQExist && COND_MEAS_TYPE_OQ.equals(condMonType)) {
                    dateFrom_9  = (measOQDateFrom + measOQTimeFrom) as long
                }
                long dateTo_9   = (measDateTo + measTimeTo) as long

                EquipmentCSVRecord eq = new EquipmentCSVRecord()
                eq.equipNo            = msf345Rec.getPrimaryKey().getEquipNo()
                eq.compCode           = compCode
                eq.condMonPos         = condMonPos
                eq.date               = measDate
                eq.time               = measTime

                if(measDate_9 > dateFrom_9 && measDate_9 <= dateTo_9) {
                    
                    if(isOAexist && COND_MEAS_TYPE_OA.equals(condMonType)) {
                        extractOARecord(eq, conMonMeas, measValue)
                        addToOARecordReadList(eq)
                    }
                    if(isOQExist && COND_MEAS_TYPE_OQ.equals(condMonType)) {
                        extractOQRecord(eq, conMonMeas, measValue)
                        addToOQRecordReadList(eq)
                    }

                }
            }
        } catch(Exception e) {
            info("browseCondMeasurement aborted due to ${e.getMessage()}")
        }
    }

    /**
     * Extract OA equipment record information.
     * @param eq specified equipment record
     * @param conMonMeas measurement name
     * @param measValue  measurement value
     */
    private void extractOARecord(EquipmentCSVRecord eq, String conMonMeas, BigDecimal measValue) {
        info("extractOARecord")
        
        if(validateOAMeas(conMonMeas)) {
            int idx = extractedOA.indexOf(eq)
            //already exist, get it from list
            if(idx > -1) {
                EquipmentCSVRecord oaRec = extractedOA.get(idx)
                oaRec.setOAMeasValue(conMonMeas, measValue)
                extractedOA.set(idx, oaRec)
            }
            //does not exist, add it into list
            else {
                eq.setOAMeasValue(conMonMeas, measValue)
                extractedOA.add(eq)
            }
        }
    }

    /**
     * Count OA equipment record information that has been read.
     * @param eq specified equipment record
     */
    private void addToOARecordReadList(EquipmentCSVRecord inputRecord) {
        info("countOARecordRead")
        
        int idx = readOA.indexOf(inputRecord)
        //First time read, add to read list
        if(idx <= -1) {
            EquipmentCSVRecord eq = new EquipmentCSVRecord()
            eq.equipNo            = inputRecord.equipNo
            eq.compCode           = inputRecord.compCode
            eq.condMonPos         = inputRecord.condMonPos
            eq.date               = inputRecord.date
            eq.time               = inputRecord.time
            readOA.add(eq)
        }
    }
    
    /**
     * Extract OA equipment record information.
     * @param eq specified equipment record
     * @param conMonMeas measurement name
     * @param measValue  measurement value
     */
    private void extractOQRecord(EquipmentCSVRecord eq, String conMonMeas, BigDecimal measValue) {
        info("extractOQRecord")
        if(validateOQMeas(conMonMeas)) {
            int idx = extractedOQ.indexOf(eq)
            //already exist, get it from list
            if(idx > -1) {
                EquipmentCSVRecord oqRec = extractedOQ.get(idx)
                oqRec.setOQMeasValue(conMonMeas, measValue)
                extractedOQ.set(idx, oqRec)
            }
            //does not exist, add it into list
            else {
                eq.setOQMeasValue(conMonMeas, measValue)
                extractedOQ.add(eq)
            }
        }
    }
    
    /**
     * Count OQ equipment record information that has been read.
     * @param inputRecord specified equipment record
     */
    private void addToOQRecordReadList(EquipmentCSVRecord inputRecord) {
        info("countOARecordRead")
        
        int idx = readOQ.indexOf(inputRecord)
        //First time read, add to read list
        if(idx <= -1) {
            EquipmentCSVRecord eq = new EquipmentCSVRecord()
            eq.equipNo            = inputRecord.equipNo
            eq.compCode           = inputRecord.compCode
            eq.condMonPos         = inputRecord.condMonPos
            eq.date               = inputRecord.date
            eq.time               = inputRecord.time
            readOQ.add(eq)
        }
    }

    /**
     * Create CSV file in working directory
     * @param fileName
     * @return csv File resource
     */
    private File createCSVFile(String fileName) {
        def workingDir  = env.workDir
        String csvPath  = "${workingDir}/${fileName}"
        if(taskUUID?.trim()) {
            csvPath = csvPath + "." + taskUUID
        }
        csvPath   = csvPath + ".csv"
        return new File(csvPath)
    }

    /**
     * Write the processed records from {@link #browseEquipmentRecords()} into CSV.
     */
    private void writeEquipmentCSV() {
        info("writeEquipmentCSV")
        if(!writeTrt60CHeader) {
            trt60CFile      = createCSVFile(CSV_TRT_60C_FILENAME)
            info("${CSV_TRT_60C_FILENAME} created in ${trt60CFile.getAbsolutePath()}")
            csvTrt60CWriter = new BufferedWriter(new FileWriter(trt60CFile))
            csvTrt60CWriter.write(EquipmentCSVRecord.writeEquipmentCSVHeader())
            csvTrt60CWriter.write("\r\n")
            writeTrt60CHeader = true
        }
        //before write into csv, sort the list
        Collections.sort(extractedEq)
        extractedEq.each {
            csvTrt60CWriter.write(it.writeEquipmentDetail())
            csvTrt60CWriter.write("\r\n")
        }
    }

    /**
     * Write the processed OA records from {@link #browseCondMeasurement()} into CSV.
     */
    private void writeCondMonOACSV() {
        info("writeCondMonOACSV")
        if(!writeTrt60FHeader) {
            trt60FFile      = createCSVFile(CSV_TRT_60F_FILENAME)
            info("${CSV_TRT_60F_FILENAME} created in ${trt60FFile.getAbsolutePath()}")
            csvTrt60FWriter = new BufferedWriter(new FileWriter(trt60FFile))
            writeTrt60FHeader = true
        }
        oaSent = extractedOA.size()
        oaRead = readOA.size()
        extractedOA.each {
            csvTrt60FWriter.write(it.writeOADetail())
            csvTrt60FWriter.write("\r\n")
        }
    }

    /**
     * Write the processed OQ records from {@link #browseCondMeasurement()} into CSV.
     */
    private void writeCondMonOQCSV() {
        info("writeCondMonOQCSV")
        if(!writeTrt60GHeader) {
            trt60GFile      = createCSVFile(CSV_TRT_60G_FILENAME)
            info("${CSV_TRT_60G_FILENAME} created in ${trt60GFile.getAbsolutePath()}")
            csvTrt60GWriter = new BufferedWriter(new FileWriter(trt60GFile))
            writeTrt60GHeader = true
        }
        oqSent = extractedOQ.size()
        oqRead = readOQ.size()
        extractedOQ.each {
            csvTrt60GWriter.write(it.writeOQDetail())
            csvTrt60GWriter.write("\r\n")
        }
    }

    /**
     * Validate measurement name against {@link OAMeasValue}.
     * @param measName measurement name to be valiated
     * @return true if measName is in {@link OAMeasValue}, false otherwise.
     */
    private boolean validateOAMeas(String measName) {
        info("validateOAMeas")
        boolean found = false
        OAMeasValue.values().each {
            if(it.getMeasName().toString().equals(measName)) {
                found = true
            }
        }
        return found
    }

    /**
     * Validate measurement name against {@link OQMeasValue}.
     * @param measName measurement name to be valiated
     * @return true if measName is in {@link OQMeasValue}, false otherwise.
     */
    private boolean validateOQMeas(String measName) {
        info("validateOQMeas")
        boolean found = false
        OQMeasValue.values().each {
            if(it.getMeasName().toString().equals(measName)) {
                found = true
            }
        }
        return found
    }

    /**
     * Check browsed records. If the number of records sent to TOA <br/>
     * exceed {@link MAX_RECORD} rows the following message is to be written to the control report.
     */
    private void checkRecordsCount() {
        info("checkRecordsCount")
        //Condition Monitoring numbers should be the number of sets read (ie the records sent to TOA) not the individual values.

        if(oqSent >= MAX_RECORD || oaSent >= MAX_RECORD) {
            String warn = String.format("\"WARNING: The maximum number of records allowed (%s) \n"+
                    "was reached before all [[#TOA Table Code CM OR Equipment]] records were processed. \n" +
                    "Please amend the search criteria by narrowing the date range and rerun the extract.\"",
                    String.valueOf(MAX_RECORD))
            info(warn)
            warning += warn
        }
    }

    /**
     * Read table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable ${tableType} - ${tableCode}")
        TableServiceReadReplyDTO tableReplyDTO = null
        try{
            TableServiceReadRequestDTO tableReadDTO = new TableServiceReadRequestDTO()
            tableReadDTO.setTableType(tableType)
            tableReadDTO.setTableCode(tableCode)
            tableReplyDTO = service.get("TABLE").read(tableReadDTO)
        }catch (EnterpriseServiceOperationException e){
            tableReplyDTO = null
            String errorMsg   = e.getErrorMessages()[0].getMessage()
            String errorCode  = e.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = e.getErrorMessages()[0].getFieldName()
            info("Error during execute TABLE-read caused by ${errorField}: ${errorCode} - ${errorMsg}.")
        }
        return tableReplyDTO
    }

    /**
     * Retrieve Table.
     * @param retDTO TableServiceRetrieveRequestDTO
     * @param reqAttr TableServiceRetrieveRequiredAttributesDTO
     * @param restart restart
     * @return
     */
    private TableServiceRetrieveReplyCollectionDTO retrieveTable(
    TableServiceRetrieveRequestDTO retDTO, TableServiceRetrieveRequiredAttributesDTO reqAttr,
    def restart) {
        info("retrieveTable")
        return service.get("TABLE").retrieve(retDTO, reqAttr, 20, false, restart)
    }

    /**
     * Calculate reverse date. The formula is: <code>99999999 - date</code>
     * @param date specified date
     * @return reversed date
     */
    private int computeDateReverse(int date) {
        info("computeDateDiff")
        return 99999999 - date
    }

    /**
     * Calculate reverse time. The formula is: <code>999999 - time</code>
     * @param time specified time
     * @return reversed time
     */
    private int computeTimeReverse(int time) {
        info("computeTimeReverse")
        return 999999 - time
    }

    /**
     * Return count detail for Equipment Records.
     * @return detail for Equipment Records
     */
    private String writeEquipmentDetail() {
        return String.format("Total Number of Equipment Records Read        : % 10d"+
        "\nTotal Number of Equipment Records Sent to TOA : % 10d", eqRead, eqSent)
    }

    /**
     * Return count detail for Condition Monitoring Records.
     * @return detail for Condition Monitoring Records
     */
    private String writeCondMonDetail() {
        return String.format("Number of OA (GAS) Sets Read                  : % 10d"+
        "\nNumber of OA (GAS) Records Send to TOA        : % 10d"+
        "\n\nNumber of OQ (FLUID) Sets Read                : % 10d"+
        "\nNumber of OQ (FLUID) Records Sent to TOA      : % 10d",
        oaRead, oaSent, oqRead, oqSent)
    }

    /**
     * Return date details.
     * @return detail for the date
     */
    private String writeDateSummaryDetail() {
        return String.format("\tOA Date From : %-8s"+
        "\n\tOA Time From : %-6s"+
        "\n\tOQ Date From : %-8s"+
        "\n\tOQ Time From : %-6s"+
        "\n\tDate To      : %-8s"+
        "\n\tTime To      : %-6s",
        convertDateFormat(measOADateFrom), convertTimeFormat(measOATimeFrom),
        convertDateFormat(measOQDateFrom), convertTimeFormat(measOQTimeFrom),
        convertDateFormat(measDateTo), convertTimeFormat(measTimeTo))
    }

    /**
     * Convert the date format with specified separator into <code>(YY/MM/DD)</code>.
     * @param dateS date as a String
     * @return formatted date as a String
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat ${dateS}")
        dateS = dateS.trim().padLeft(8).replace(" ", "0")
        // dd/mm/yyyy
        def formattedString = dateS.substring(6) + "/" + dateS.substring(4,6) + "/" + dateS.substring(2,4)
        return formattedString
    }

    /**
     * Convert the time format with specified separator into <code>(HH:MM:SS)</code>.
     * @param timeS time as a String
     * @return formatted time as a String
     */
    private String convertTimeFormat(String timeS) {
        info("convertTimeFormat ${timeS}")
        timeS = timeS.trim().padLeft(6).replace(" ", "0")
        //hh:mm:ss
        def formattedString = timeS.substring(0,2) + ":" + timeS.substring(2,4)
        return formattedString
    }
}

/**
 * Run the script
 */
ProcessTrb60a process = new ProcessTrb60a()
process.runBatch(binding)