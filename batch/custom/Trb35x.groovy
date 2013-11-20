/**
 *  @Ventyx 2012
 *  Conversion from Trb35x.groovy
 *
 * This program provides the measurement value of the various <br>
 * condition monitoring measurements based on condition <br>
 * monitoring position and condition monitoring type. <br>
 *
 * Revision based on <b>URS-Reporting E8v3-Report TRB35X.DO5.docx</b>
 */

package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.util.ArrayList;

import com.mincom.ellipse.edoi.ejb.msf340.*;
import com.mincom.ellipse.edoi.ejb.msf341.*;
import com.mincom.ellipse.edoi.ejb.msf345.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf610.*;
import com.mincom.ellipse.edoi.ejb.msf650.*;
import com.mincom.ellipse.edoi.ejb.msfh67.*;
import com.mincom.enterpriseservice.ellipse.equipment.*;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.enterpriseservice.exception.*;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;


/**
 * Request Parameter for Trb35x.
 * <li><code>measurementDateFrom</code> : Measurement Date From</li>
 * <li><code>measurementDateTo</code> : Measurement Date To</li>
 * <li><code>componentCode</code> : Component Code    (eg. TX--)</li>
 * <li><code>conditionMonitoringPos</code> : Cond Mon Position (eg. MAIN, S0155*)</li>
 * <li><code>conditionMonitoringType</code> : Cond Mon Type     (eg. CK, OA, OQ)</li>
 * <li><code>alarmOnly</code> : Alarm Only Y/N</li>
 * <li><code>fittedEquipmentNo</code> : Rotable Equip Number</li>
 * <li><code>egi</code> : EGI</li>
 * <li><code>status</code> : Equipment Status</li>
 * <li><code>requlatoryCategory</code> : Equipment Requlatory Category</li>
 * <li><code>customer</code> : Equipment Type (Owner)</li>
 */
public class ParamsTrb35x {
    String measurementDateFrom
    String measurementDateTo
    String componentCode
    String conditionMonitoringPos
    String conditionMonitoringType
    String alarmOnly
    String fittedEquipmentNo
    String egi
    String productiveUnit
    String status
    String requlatoryCategory
    String customer
}

/**
 * CSV record content used as Trb35x report.
 */
public class Trb35xRecord {
    String componentNo, equipRef, dblQuote1, equipDesc, dblQuote2, egi, originalDoc,
    compCode, serviceStat, condMonPos, condMonType, measDate, measTime, measType, visInspCode1, visInspCode2

    /**
     * Initialize the CSV fields with blank literal.
     */
    public void init() {
        componentNo = equipRef = dblQuote1 = equipDesc = dblQuote2 = egi = originalDoc = " "
        compCode = serviceStat = condMonPos = condMonType = measDate = measTime = measType = " "
    }

    /**
     * Write CSV header.
     * @return CSV header "Component-No,Equip. Ref  , Equip. Description  ,EGI ,Contract No,Code,Status,Position,Type,Date ,Time ,"
     */
    public String writeHeader() {
        init()
        componentNo = "Component-No"
        equipRef    = "Equip. Ref  "
        equipDesc   = "Equip. Description "
        egi         = "EGI "
        originalDoc = "Contract No"
        compCode    = "Code"
        serviceStat = "Status"
        condMonPos  = "Position"
        condMonType = "Type"
        measDate    = "Date "
        measTime    = "Time "
        return writeDetail()
    }

    /**
     * Write the last part of CSV header.
     * @return last part of CSV header ",Vis Insp Code 1,Vis Insp Code 2"
     */
    public String writeHeader2() {
        visInspCode1 = "Vis Insp Code 1"
        visInspCode2 = "Vis Insp Code 2"
        return writeDetail2()
    }

    /**
     * Write blank lines.
     * @return blank lines
     */
    public String writeBlankLine() {
        init()
        return writeDetail()
    }

    /**
     * Write the CSV detail.
     * @return CSV detail
     */
    public String writeDetail() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,",
        componentNo, equipRef, dblQuote1+equipDesc+dblQuote2, egi, originalDoc,
        compCode, serviceStat, condMonPos, condMonType, measDate, measTime)
    }

    /**
     * Write the last part of CSV detail.
     * @return last part of CSV detail
     */
    public String writeDetail2() {
        return String.format("%s,%s", visInspCode1, visInspCode2)
    }
}

/**
 * Main Process of Trb35x.
 */
public class ProcessTrb35x extends SuperBatch {

    /**
     * Data type for Measurement Type. <br>
     * Contains Measurement Type and Description.
     */
    private class MeasurementTypeTab implements Comparable {
        String type
        String description

        /**
         * Initialize MeasurementTypeTab with given value.
         * @param type specified type
         * @param description specified description
         */
        public MeasurementTypeTab(String type, String description) {
            this.type = type
            this.description = description
        }

        @Override
        public int compareTo(Object another) {
            if(another == null) {
                return -1
            }
            if(!(another instanceof MeasurementTypeTab)) {
                return -1
            }
            MeasurementTypeTab o2 = (MeasurementTypeTab) another
            return type.compareTo(o2.type)
        }
    }

    /**
     * Data type for Measurement Value<br>
     * Contains Measurement Data and Value.
     */
    private class MeasurementValueTab {
        String[] data = new String[MSF341_MEAS_TABLE_DATA_LENGTH]
        double[][] value = new double[MSF341_MEAS_TABLE_DATA_LENGTH][MSF341_MEAS_TABLE_LENGTH]

        /**
         * Initialize MeasurementValueTab with empty literal.
         */
        public MeasurementValueTab() {
            (0..MSF341_MEAS_TABLE_DATA_LENGTH-1).each { int i->
                setDataAt(i, " ")
                (0..MSF341_MEAS_TABLE_LENGTH-1).each { int j->
                    setValueAt(i, j, 0.0d)
                }
            }
        }

        /**
         * Set the data at specified index.
         * @param idx specified index
         * @param data specified data
         */
        public void setDataAt(int idx, String data) {
            this.data[idx] = data
        }

        /**
         * Set the value at specified index.
         * @param idx1 specified index represents row
         * @param idx2 specified index represents column
         * @param value specified value
         */
        public void setValueAt(int idx1, int idx2, double value) {
            this.value[idx1][idx2] = value
        }

        /**
         * Get data from the specified index.
         * @param idx specified index
         * @return data from the specified index
         */
        public String getDataAt(int idx) {
            String data = this.data[idx]
            return data
        }

        /**
         * Get date from the specified index.
         * @param idx specified index
         * @return date from the specified index
         */
        public String getDateFromDataAt(int idx) {
            String date = "00000000"
            if(data[idx]?.trim()) {
                date = data[idx].padRight(14).substring(0, 8)
            }
            return date
        }

        /**
         * Get time from the specified index.
         * @param idx specified index
         * @return time from the specified index
         */
        public String getTimeFromDataAt(int idx) {
            String time = "000000"
            time = data[idx].padRight(14).substring(8)
            return time
        }

        /**
         * Get value with custom fromat from the specified row and column.
         * @param idx1 specified index represents row
         * @param idx2 specified index represents column
         * @return value with custom fromat
         */
        public String getFormmatedValueAt(int idx1, int idx2) {
            String s = " "
            s = String.format("%18.6f", value[idx1][idx2])
            return s
        }
    }

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 8

    /*
     * Constants
     */
    private static final int MAX_ROW_READ = 1000
    private static final int MAX_MEAS_TYPE = 250
    private static final int MAX_INSTANCE = 20
    private static final int MAX_MEAS_DATE = 1005
    private static final int MSF341_MEAS_TABLE_LENGTH = 255
    private static final int MSF341_MEAS_TABLE_DATA_LENGTH = 1005
    private static final int COND_MON_POS_TAB_LENGTH = 10
    private static final String MSF010_TABLE_TYPE_PM = "PM"
    private static final String SERVICE_CALL_TABLE = "TABLE"
    private static final String SERVICE_EQUIPMENT  = "EQUIPMENT"
    private static final String MSF010_TABLE_TYPE_MS = "MS"
    private static final String MSF650_ET_FITMENT = "B"
    private static final String REPORT_NAME_TRB35X = "TRB35X"
    private static final String MSF340_TYPE_S = "S"
    private static final String MSF340_TYPE_REF_E = "E"
    private static final String REC_345_TYPE = "E"
    private static final String MSF345_MEAS_STATUS_NML = "NML"

    /*
     * Variables
     */
    private ParamsTrb35x batchParams
    private boolean aborted = false, condMonPosAsterix, loadMeasType, stopProcessing, equipRequired, msf345Found, alarmOnly, writeHeader
    private int revMeasurementDateFrom_9, revMeasurementDateTo_9, fetchCount, subCount
    private ArrayList<MeasurementTypeTab> measTableTypes
    private MeasurementValueTab measValueTab
    private String[] condMonPosTab
    private String condMonPosStartReqParam, condMonPos, condMonDefTypeRef, condMonDefCompModData
    private String equipNo, equipItemName1, equipItemName2, equipPlantNo, equipProdUnit
    private File csvFile
    private BufferedWriter csvWriter
    private MSF600Rec equipmentRecord
    private MSF650Rec compTracingRecord
    private MSF345Rec condMonRecord
    private Trb35xRecord trt35xRecord
    private ArrayList<String> fittedEquipmentList

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : " + version)
        batchParams = params.fill(new ParamsTrb35x())
        info("measurementDateFrom     : " + batchParams.measurementDateFrom)
        info("measurementDateTo       : " + batchParams.measurementDateTo)
        info("componentCode           : " + batchParams.componentCode)
        info("conditionMonitoringPos  : " + batchParams.conditionMonitoringPos)
        info("conditionMonitoringType : " + batchParams.conditionMonitoringType)
        info("alarmOnly               : " + batchParams.alarmOnly)
        info("fittedEquipmentNo       : " + batchParams.fittedEquipmentNo)
        info("egi                     : " + batchParams.egi)
        info("productiveUnit          : " + batchParams.productiveUnit)
        info("equipmentStatus         : " + batchParams.status)
        info("requlatoryCategory      : " + batchParams.requlatoryCategory)
        info("customer                : " + batchParams.customer)

        try {
            processBatch()
        } finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        initialize()
        processRequest()
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        if(csvWriter != null) {
            csvWriter.close()
            info("Adding CSV into Request.")
            if(this.getTaskUUID()?.trim()) {
                request.request.CURRENT.get().addOutput(csvFile,
                        "text/comma-separated-values", REPORT_NAME_TRB35X);
            }
        }
    }

    /**
     * Initialize the CSV writer, table type and other variables.
     */
    private void initialize() {
        info("initialize")
        /*
         * Initialize MSF341-Measurement Table
         */
        measValueTab = new MeasurementValueTab()
        measTableTypes = new ArrayList<MeasurementTypeTab>()
        (0..MSF341_MEAS_TABLE_LENGTH-1).each {
            measTableTypes.add(new MeasurementTypeTab(" ", " "))
        }
        condMonPosTab = new String[COND_MON_POS_TAB_LENGTH]

        /*
         * Initialize store variables
         */
        condMonDefCompModData = condMonDefTypeRef = equipNo = " "
        equipItemName1 = equipItemName2 = equipPlantNo = " "

        /*
         * Initialize the switch variables
         */
        loadMeasType = false
        stopProcessing = false
        writeHeader = false
        alarmOnly = batchParams.alarmOnly.equalsIgnoreCase("y")

        /*
         * Convert the Parameter Measurement Date From/To to reversed date
         */
        revMeasurementDateFrom_9 = computeDateReverse(batchParams.measurementDateFrom as int)
        revMeasurementDateTo_9 = computeDateReverse(batchParams.measurementDateTo as int)
        /*
         * Check to see if a Wildcard Pos has been entered
         */
        condMonPos = batchParams.conditionMonitoringPos
        condMonPosAsterix = batchParams.conditionMonitoringPos.padRight(7).substring(5, 6).equalsIgnoreCase("*")
        condMonPosStartReqParam = batchParams.conditionMonitoringPos.padRight(7).substring(0, 5)

        if(condMonPosAsterix) {
            (0..COND_MON_POS_TAB_LENGTH-1).each { condMonPosTab[it] = " " }
            try {
                getPMRecords()
            } catch(Exception e) {
                e.printStackTrace()
                info("Excpetion occured during initialization#getPMRecords: ${e.getMessage()}")
            }
        }

        fittedEquipmentList = new ArrayList<String>()
    }

    /**
     * Get records with table type PM from Table.
     */
    private void getPMRecords() {
        info("getPMRecords")
        try {
            TableServiceRetrieveRequiredAttributesDTO tableReqAttributeDTO = new TableServiceRetrieveRequiredAttributesDTO()
            tableReqAttributeDTO.returnTableCode = true
            tableReqAttributeDTO.returnTableType = true
            tableReqAttributeDTO.returnAssociatedRecord = true
            tableReqAttributeDTO.returnDescription = true

            TableServiceRetrieveRequestDTO tableRequestDTO = new TableServiceRetrieveRequestDTO()
            tableRequestDTO.setTableType(MSF010_TABLE_TYPE_PM)
            ArrayList<TableServiceRetrieveReplyDTO> tableList = new ArrayList<TableServiceRetrieveReplyDTO>()
            def restart = ""
            boolean firstLoop = true
            while (firstLoop || (restart !=null && restart.trim().length() > 0)){
                TableServiceRetrieveReplyCollectionDTO tableReplyDTO =
                        retrTable(tableRequestDTO, tableReqAttributeDTO, restart)
                firstLoop = false
                if(tableReplyDTO) {
                    restart = tableReplyDTO.getCollectionRestartPoint()
                    tableList.addAll(tableReplyDTO.getReplyElements())
                }
            }

            int i = 0, j = 0
            int msf010Size = tableList.size()
            while(i < COND_MON_POS_TAB_LENGTH && j < msf010Size) {
                TableServiceRetrieveReplyDTO dto = tableList.get(j)
                String condMonPosWork = dto.tableCode
                String condMonPosWorkStart = condMonPosWork.padRight(18).substring(0, 5)
                if(condMonPosWorkStart.trim().equals(condMonPosStartReqParam.trim())) {
                    condMonPosTab[i] = condMonPosWork
                    i++
                }
                j++
            }
        } catch (EnterpriseServiceOperationException e){
            String errorMsg   = e.getErrorMessages()[0].getMessage()
            String errorCode  = e.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = e.getErrorMessages()[0].getFieldName()
            info("Error during execute TABLE-retrieve caused by ${errorField}: ${errorCode} - ${errorMsg}.")
        }
    }

    /**
     * Retrieve Table
     * @param tableReqDTO TableServiceRetrieveRequestDTO
     * @param tableReqAttr TableServiceRetrieveRequiredAttributesDTO
     * @param restart restart point
     * @return TableServiceRetrieveReplyCollectionDTO
     */
    private TableServiceRetrieveReplyCollectionDTO retrTable(TableServiceRetrieveRequestDTO tableReqDTO,
    TableServiceRetrieveRequiredAttributesDTO tableReqAttr, String restart) {
        info("retrTable")
        TableServiceRetrieveReplyCollectionDTO tableReplyDTO =
                service.get(SERVICE_CALL_TABLE).retrieve(tableReqDTO, tableReqAttr,
                MAX_INSTANCE, false, restart)
        return tableReplyDTO
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
            tableReplyDTO = service.get(SERVICE_CALL_TABLE).read(tableReadDTO)
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
     * Process the request.
     */
    private void processRequest() {
        info("processRequest")
        boolean exit = false
        int i = 0

        if(batchParams.productiveUnit?.trim()) {
            EquipmentServiceRetrieveReplyDTO prodUnitDto =
                    readProductiveUnit(batchParams.productiveUnit.trim())
            if(prodUnitDto) {
                equipProdUnit = prodUnitDto?.getEquipmentNo()?.trim()
                fittedEquipmentList.add(equipProdUnit)
                fittedEquipmentList.addAll(browseProductiveUnitHierarchy(equipProdUnit))
                debug("productive unit hierarchy results: ${fittedEquipmentList}")
            }
        }

        while(!exit) {
            /*
             * Setup the correct Pos
             */
            if(condMonPosAsterix) {
                if(condMonPosTab[i]?.trim()) {
                    condMonPos = condMonPosTab[i]
                } else {
                    exit = true
                    continue
                }
            } else {
                condMonPos = batchParams.conditionMonitoringPos
            }

            /*
             * This is the 1st part of the process to retrieve
             *   all the measurement types for a given condition
             *   monitoring position and type.  This will form
             *   part of the CSV file header.
             */
            if(!loadMeasType) {
                loadMeasType = true
                getCondMonitorMeasurementType()
                /*
                 * No condition monitoring values found for the
                 * Component Code, Cond Mn Position and Cond Mon Type
                 */
                if(stopProcessing) {
                    exit = true
                    continue
                }
            }

            /*
             * This is the 2nd part ofthe process to populate
             * the measurement types with the measurement values
             * for a given measurement date range.
             */
            populateConditionMeasurementType()

            /*
             * We need to loop back sand see if there are anymore
             * POS or Standard Jobs to process.
             */
            if(condMonPosAsterix) {
                i++
            } else {
                exit = true
                continue
            }
        }
    }

    /**
     * Browse Productive Unit Hiearchy.
     * @param parentEquipNo parent equipment number
     * @return list of fitted equipment number
     */
    private ArrayList<String> browseProductiveUnitHierarchy(String parentEquipNo) {
        info("browseProductiveUnitHierarchy")
        ArrayList<String> equipNumbers = new ArrayList<String>()
        QueryImpl query = new QueryImpl(MSFH67Rec.class)
                .and(MSFH67Key.rootEquip.equalTo(parentEquipNo))
                .and(MSFH67Key.equipNo.greaterThanEqualTo(" "))
                .and(MSFH67Key.parentEquip.greaterThanEqualTo(" "))
                .orderBy(MSFH67Rec.msfh67Key)
        edoi.search(query, MAX_ROW_READ, {MSFH67Rec rec->
            equipNumbers.add(rec.getPrimaryKey().getEquipNo().trim())
        })
        return equipNumbers
    }

    /**
     * Browse Condition Monitor Measurement Defn constrained by the request parameters.
     */
    private void getCondMonitorMeasurementType() {
        info("getCondMonitorMeasurementType")
        fetchCount = 0
        subCount = 0

        Constraint cType    = MSF341Key.typeReference.like("G%")

        Constraint cMonPos  = MSF341Key.condMonPos.equalTo(condMonPos)
        if(condMonPos?.trim()) {
            cMonPos  = MSF341Key.condMonPos.equalTo(condMonPos)
        } else {
            cMonPos  = MSF341Key.condMonPos.greaterThanEqualTo(" ")
        }

        Constraint cMonType = MSF341Key.condMonType.equalTo(batchParams.conditionMonitoringType)
        if(batchParams.conditionMonitoringType?.trim()) {
            cMonType = MSF341Key.condMonType.equalTo(batchParams.conditionMonitoringType)
        } else {
            cMonPos  = MSF341Key.condMonType.greaterThanEqualTo(" ")
        }

        Constraint cMonCode
        if(batchParams.componentCode?.trim()) {
            cMonCode = MSF341Key.compModData.like(batchParams.componentCode + "%")
        } else {
            cMonCode = MSF341Key.compModData.greaterThanEqualTo(" ")
        }

        Constraint cMonMeas = MSF341Key.condMonMeas.greaterThanEqualTo(" ")

        QueryImpl query = new QueryImpl(MSF341Rec.class).and(cType).and(cMonPos).
                and(cMonType).and(cMonCode).and(cMonMeas)
        fetchCondMonitorMeasurementRecords(query)
        /*
         * The equipment is using the old monitoring set, to retrieve the
         * measurement types, one MSF600 record is read that matches the
         * parameters comp-code, cond-mon-pos & cond-mon-type.  The MSF340
         * is read to retrieve the CM-Profile-Type and CM-Profile-Ref.
         * The data will be used to retrieve the MSF341 Measurement Types.
         */
        if(fetchCount == 0) {
            stopProcessing = false
            retrieveOldCondMonitorMeasurementSet()

            /*
             * The Condition Monitoring Values entered may not be set.
             * This is to prevent further processing.
             */
            if(stopProcessing) {
                csvWriter.write("\r\n")
                csvWriter.write("NO RECORD EXTRACTED - INVALID PARAMETER VALUES ENTERED\r\n")
                info("NO RECORD EXTRACTED - INVALID PARAMETER VALUES ENTERED")
            }

            /*
             * Since there is no service call for MSF341 (Condition Monitor Measurement Defn), use edoi to browse
             */
            subCount = 0
            if(condMonDefTypeRef?.trim()) {
                cType    = MSF341Key.typeReference.equalTo(condMonDefTypeRef)
            } else {
                cType    = MSF341Key.typeReference.greaterThanEqualTo(" ")
            }

            if(condMonPos?.trim()) {
                cMonPos  = MSF341Key.condMonPos.equalTo(condMonPos)
            } else {
                cMonPos  = MSF341Key.condMonPos.greaterThanEqualTo(" ")
            }

            if(batchParams.conditionMonitoringType?.trim()) {
                cMonType = MSF341Key.condMonType.equalTo(batchParams.conditionMonitoringType)
            } else {
                cMonPos  = MSF341Key.condMonType.greaterThanEqualTo(" ")
            }

            if(condMonDefCompModData?.trim()) {
                cMonCode = MSF341Key.compModData.equalTo(condMonDefCompModData)
            } else {
                cMonCode = MSF341Key.compModData.greaterThanEqualTo(" ")
            }

            query = new QueryImpl(MSF341Rec.class).and(cType).and(cMonPos).
                    and(cMonType).and(cMonCode).and(cMonMeas)
            fetchCondMonitorMeasurementRecords(query)
        }

        /*
         * Set this on display mode for various Measurement Types
         */
        int i = 0
        String measType = measTableTypes.getAt(i)?.type
        while(measType?.trim() && i < subCount) {
            info("MEASUREMENT TYPE ${i + 1} ${measType}")
            i++
            if(measTableTypes.getAt(i) == null) {
                break
            }
            measType = measTableTypes.getAt(i).type
        }
    }

    /**
     * Fetch Condition Monitor Measurement Defn based on specified query.
     * @param query specified query
     */
    private void fetchCondMonitorMeasurementRecords(QueryImpl query) {
        info("fetchCondMonitorMeasurementRecords")

        query = query.orderBy(MSF341Rec.msf341Key)
        edoi.search(query, MAX_ROW_READ, { MSF341Rec msf341Rec->
            String msf341RecCondMonMeas = msf341Rec.getPrimaryKey().getCondMonMeas().trim()
            for(MeasurementTypeTab typeTab : measTableTypes) {
                if(typeTab != null) {
                    if(typeTab.type?.equals(msf341RecCondMonMeas)) {
                        break
                    }
                    if(!typeTab.type?.trim()) {
                        //get Meas Description
                        boolean notRequired = false
                        TableServiceReadReplyDTO tableDTO = readTable(MSF010_TABLE_TYPE_MS, msf341RecCondMonMeas)
                        notRequired = tableDTO == null ?
                                true :
                                tableDTO.getDescription().padRight(50).substring(1, 5).equals("****")
                        if(notRequired) {
                            break
                        } else {
                            typeTab.type        = msf341RecCondMonMeas
                            typeTab.description = tableDTO.getDescription()
                            subCount++
                            break
                        }
                    }
                }
            }
            if(subCount > MAX_MEAS_TYPE) {
                info("W50-341-MEAS-TYPE-TAB REACHED MAXIMUM VALUE: ${MAX_MEAS_TYPE}")
            }
            fetchCount++
        })
        //Sort the meas table types
        Collections.sort(measTableTypes)
        ArrayList<MeasurementTypeTab> tempTableTypes = new ArrayList<MeasurementTypeTab>()
        for(MeasurementTypeTab typeTab : measTableTypes) {
            if(typeTab != null) {
                if(typeTab.type == null) {
                    tempTableTypes.add(typeTab)
                }
                else if(typeTab.type.trim().isEmpty()) {
                    tempTableTypes.add(typeTab)
                }
            }
        }
        for(MeasurementTypeTab typeTab : tempTableTypes) {
            measTableTypes.remove(typeTab)
        }
    }

    /**
     * Check for old Condition Monitor Set Definition constrained by the request parameters.
     * @return true if Condition Monitor Set Definition is found, false otherwise.
     */
    private boolean retrieveOldCondMonitorMeasurementSet() {
        info("retrieveOldCondMonitorMeasurementSet")
        QueryImpl query = new QueryImpl(MSF600Rec.class).and(MSF600Key.equipNo.greaterThanEqualTo(" "))
        if(batchParams.egi?.trim()) {
            Constraint cEGI = MSF600Rec.equipGrpId.equalTo(batchParams.egi)
            query = query.and(cEGI)
        }
        if(equipProdUnit?.trim()) {
            Constraint cParentEquip = MSF600Rec.parentEquip.equalTo(equipProdUnit)
            query = query.and(cParentEquip)
        }
        if(batchParams.status?.trim()) {
            Constraint cEquipStatus = MSF600Rec.equipStatus.equalTo(batchParams.status)
            query = query.and(cEquipStatus)
        }
        if(batchParams.requlatoryCategory?.trim()) {
            Constraint cEquipClassifx1 = MSF600Rec.equipClassifx1.equalTo(batchParams.requlatoryCategory)
            query = query.and(cEquipClassifx1)
        }
        if(batchParams.customer?.trim()) {
            Constraint cEquipClassifx2 = MSF600Rec.equipClassifx2.equalTo(batchParams.customer)
            query = query.and(cEquipClassifx2)
        }

        int msf600Count = 0
        try {
            query = query.orderBy(MSF600Rec.msf600Key)
            edoi.search(query, MAX_ROW_READ, {MSF600Rec msf600Rec->
                msf600Count++
                String msf600RecEquipNo = msf600Rec.getPrimaryKey().getEquipNo().trim()
                if(!batchParams.fittedEquipmentNo?.trim()
                || batchParams.fittedEquipmentNo.trim().equals(msf600RecEquipNo)) {
                    equipNo = msf600RecEquipNo
                    equipRequired = false
                    getFitEquip()
                    if(equipRequired) {
                        MSF340_SET_DEFRec msf340Rec = getConMonSetDefinition()
                        if(msf340Rec != null) {
                            condMonDefTypeRef = msf340Rec.getCmProfileTy() + msf340Rec.getCmProfileRef()
                            condMonDefCompModData   = msf340Rec.getPrimaryKey().getCompModData()
                            //break from search closure
                            throw new RuntimeException("MSF340 found, break from the retrieveOldCMSet()#searchMSF600 closure")
                        }
                    }
                }
            })
        } catch(Exception e) {
            e.printStackTrace()
            info("retrieveOldCMSet()#searchMSF600 closure was aborted due to: ${e.getMessage()}")
        } finally {
            stopProcessing = (msf600Count == 0)
        }
    }

    /**
     * Get Condition Monitor Set Definition
     * @return MSF340_SET_DEFRec
     */
    private MSF340_SET_DEFRec getConMonSetDefinition() {
        info("getConMonSetDefinition")
        String typeRef = MSF340_TYPE_REF_E + (equipmentRecord != null ? equipmentRecord.getPrimaryKey().equipNo : equipNo)
        Constraint cRecType = MSF340_SET_DEFKey.rec_340Type.equalTo(MSF340_TYPE_S)
        Constraint cTypeRef = MSF340_SET_DEFKey.typeReference.equalTo(typeRef)
        Constraint cCondMonPos = MSF340_SET_DEFKey.condMonPos.equalTo(condMonPos)
        Constraint cCondMonType = MSF340_SET_DEFKey.condMonType.equalTo(batchParams.conditionMonitoringType)
        QueryImpl queryMSF340 = new QueryImpl(MSF340_SET_DEFRec.class).and(cRecType).and(cTypeRef).and(cCondMonPos).and(cCondMonType)
        MSF340_SET_DEFRec msf340Rec = edoi.firstRow(queryMSF340)
        return msf340Rec
    }

    /**
     * Get Equipment Register constrained by the request parameters.
     */
    private void getFitEquip() {
        info("getFitEquip ${equipNo}")
        equipItemName1 = equipItemName2 = equipPlantNo = ""
        Constraint cFitEquipNo     = MSF650Key.fitEquipNo.equalTo(equipNo)
        Constraint cTracingAccount = MSF650Rec.tracingActn.equalTo(MSF650_ET_FITMENT)
        Constraint cRevEtDate      = MSF650Key.revsdEtDate.greaterThanEqualTo(" ")
        Constraint cRevTrcSeqNo    = MSF650Key.revTrcSeqNo.greaterThanEqualTo(" ")
        QueryImpl query = new QueryImpl(MSF650Rec.class)
                .and(cFitEquipNo)
                .and(cTracingAccount)
                .and(cRevEtDate)
                .orderBy(MSF650Rec.msf650Key)
        try {
            edoi.search(query, MAX_ROW_READ, {MSF650Rec msf650Rec->
                String installPosn =  msf650Rec.getInstallPosn().trim()
                String msf650RecInstalEquip = installPosn.padRight(18).substring(0, 12)
                String msf650RecInstalComp  = installPosn.padRight(18).substring(12, 16)
                if(batchParams.componentCode?.trim()
                && !batchParams.componentCode.trim().equals(msf650RecInstalComp)) {
                    //break from search closure
                    throw new RuntimeException("W90-COMP-CODE <> SPACES AND W90-COMP-CODE <> MSF650-INSTALL-COMP, break from the getFitEquip()#searchMSF650 closure")
                }
                getEquipment(msf650RecInstalEquip)
                getLastEquipment()
                compTracingRecord = msf650Rec
                if(equipRequired) {
                    //break from search closure
                    throw new RuntimeException("equipRequired, break from the getFitEquip()#searchMSF650 closure")
                }
            })
        } catch(Exception e) {
            info("getFitEquip()#searchMSF650 closure was aborted due to: ${e.getMessage()}")
        }
    }

    /**
     * Get last processed equipment based on {@link equipNo}
     */
    private void getLastEquipment() {
        info("getLastEquipment")
        try {
            equipmentRecord = edoi.findByPrimaryKey(new MSF600Key(equipNo:equipNo))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info("lastMSF600Rec ${equipNo} does not exist")
        }
    }

    /**
     * Get Equipment
     * @param equipNo Equipment Number
     */
    private void getEquipment(String equipNo) {
        info("getEquipment")
        MSF600Rec msf600Rec = null

        try {
            msf600Rec = edoi.findByPrimaryKey(new MSF600Key(equipNo:equipNo))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info("MSF600Rec ${equipNo} does not exist")
            //break from search closure
            throw new RuntimeException("MSF600Rec ${equipNo} does not exist, break from the getFitEquip()#searchMSF650 closure")
        }

        if(batchParams.status?.trim() &&
        !batchParams.status.trim().equals(msf600Rec.getEquipStatus().trim())) {
            //break from search closure
            throw new RuntimeException("Condition checking for MSF600-EQUIP-STATUS, do not meet the criteria, break from the getFitEquip()#searchMSF650 closure")
        }

        if(equipProdUnit?.trim() &&
        !fittedEquipmentList.contains(msf600Rec.getPrimaryKey().getEquipNo().trim())) {
            //break from search closure
            throw new RuntimeException("Condition checking for MSF600-PARENT-EQUIP, do not meet the criteria, break from the getFitEquip()#searchMSF650 closure")
        }

        if(batchParams.requlatoryCategory?.trim() &&
        !batchParams.requlatoryCategory.trim().equals(msf600Rec.getEquipClassifx1().trim())) {
            //break from search closure
            throw new RuntimeException("Condition checking for MSF600-EQUIP-CLASSIF-1, do not meet the criteria, break from the getFitEquip()#searchMSF650 closure")
        }

        if(batchParams.customer?.trim() &&
        !batchParams.customer.trim().equals(msf600Rec.getEquipClassifx2().trim())) {
            //break from search closure
            throw new RuntimeException("Condition checking for MSF600-EQUIP-CLASSIF-2, do not meet the criteria, break from the getFitEquip()#searchMSF650 closure")
        }

        equipRequired = true
        equipItemName1 = msf600Rec.getItemName_1().trim()
        equipItemName2 = msf600Rec.getItemName_2().trim()
        equipPlantNo   = msf600Rec.getPlantNo().trim()
    }

    /**
     * Read the equipment based on the productive unit. 
     * @param prodUnit productive unit
     * @return
     */
    private EquipmentServiceRetrieveReplyDTO readProductiveUnit(String prodUnit) {
        info("readProductiveUnit")

        try{
            EquipmentServiceRetrieveRequestDTO equipRetDto =
                    new EquipmentServiceRetrieveRequestDTO()
            equipRetDto.setEquipmentRefSearchMethod("E")
            equipRetDto.setEquipmentRef(prodUnit)
            equipRetDto.setDistrictCode("GRID")
            equipRetDto.setProdUnitFlag(true)
            equipRetDto.setAssocEquipmentItemsExcl(false)
            equipRetDto.setExclInStore(false)
            equipRetDto.setExclScrapSold(false)

            EquipmentServiceRetrieveRequiredAttributesDTO equipReqAtt =
                    new EquipmentServiceRetrieveRequiredAttributesDTO()
            equipReqAtt.setReturnEquipmentNo(true)
            equipReqAtt.setReturnEquipmentRef(true)
            equipReqAtt.setReturnParentEquipment(true)
            equipReqAtt.setReturnParentEquipmentRef(true)
            equipReqAtt.setReturnEquipmentGrpId(true)
            equipReqAtt.setReturnEquipmentStatus(true)
            equipReqAtt.setReturnEquipmentClassif0(true)
            equipReqAtt.setReturnEquipmentClassif1(true)

            EquipmentServiceRetrieveReplyCollectionDTO equipReplyDto =
                    service.get(SERVICE_EQUIPMENT).retrieve(equipRetDto, equipReqAtt, 1, false)

            return equipReplyDto?.getReplyElements()[0]
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Cannot Retrieve equipment ${SERVICE_EQUIPMENT} : ${e.getMessage()}")
            String errorMsg   = e.getErrorMessages()[0].getMessage()
            String errorCode  = e.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = e.getErrorMessages()[0].getFieldName()
            info("Error during execute EQUIPMENT-retrieve caused by ${errorField}: ${errorCode} - ${errorMsg}.")
        }
        return null
    }

    /**
     * Populate Condition Monitor Measurement Defn based on the EGI.
     */
    private void populateConditionMeasurementType() {
        info("populateConditionMeasurementType")
        QueryImpl query = new QueryImpl(MSF600Rec.class).and(MSF600Key.equipNo.greaterThanEqualTo(" "))
        if(batchParams.egi?.trim()) {
            Constraint cEGI = MSF600Rec.equipGrpId.equalTo(batchParams.egi)
            query = query.and(cEGI)
        }
        query = query.orderBy(MSF600Rec.msf600Key)
        try {
            edoi.search(query, MAX_ROW_READ, {MSF600Rec msf600Rec->
                String msf600RecEquipNo = msf600Rec.getPrimaryKey().getEquipNo().trim()
                if(!batchParams.fittedEquipmentNo?.trim()
                || batchParams.fittedEquipmentNo.trim().equals(msf600RecEquipNo)) {
                    equipNo = msf600RecEquipNo
                    equipRequired = false
                    getFitEquip()
                    if(equipRequired) {
                        initArray()
                        msf345Found = false
                        readCondMonMeas()
                    }
                }
            })
        } catch(Exception e) {
            e.printStackTrace()
            info("populateMeasType()#searchMSF600 closure was aborted due to: ${e.getMessage()}")
        }
    }

    /**
     * Intialize the array measurement value depending on the number of measurement type set. <br>
     * In this way, the rest of the CSV header columns is space-filled.
     */
    private void initArray() {
        info("initArray")

        for(int i = 0; i < (MAX_MEAS_DATE-1); i++) {
            measValueTab.setDataAt(i, " ")
            for(int j = 0; j < (subCount-1); j++) {
                measValueTab.setValueAt(i, j, 0)
            }
        }
    }

    /**
     * Read Condition Monitor Measurement constrained by the request parameters.
     */
    private void readCondMonMeas() {
        info("readCondMonMeas")
        String equipNo          = equipmentRecord != null ? equipmentRecord.getPrimaryKey().equipNo : equipNo
        String condMonPos       = "%"+condMonPos.padRight(7)+batchParams.conditionMonitoringType
        Constraint cRecType     = MSF345Key.rec_345Type.equalTo(REC_345_TYPE)
        Constraint cEquipNo     = MSF345Key.equipNo.equalTo(equipNo)
        Constraint cCompPosData = MSF345Key.compPosData.like(condMonPos)
        Constraint cMonMeas     = MSF345Key.condMonMeas.greaterThanEqualTo(" ")
        Constraint cRevMeasData = MSF345Key.revMeasData.greaterThanEqualTo(" ")
        QueryImpl query = new QueryImpl(MSF345Rec.class)
                .and(cRecType)
                .and(cEquipNo)
                .and(cCompPosData)
                .and(cMonMeas)
                .orderBy(MSF345Rec.msf345Key)

        edoi.search(query, MAX_ROW_READ, {MSF345Rec msf345Rec->

            String revMeasData = msf345Rec.getPrimaryKey().getRevMeasData()
            String revMeasDate = revMeasData.padRight(14).substring(0, 8)
            String revMeasTime = revMeasData.padRight(14).substring(8)
            int revMeasDate_9 = revMeasDate as int

            if(revMeasDate_9 <= revMeasurementDateFrom_9 &&
            revMeasDate_9 >= revMeasurementDateTo_9) {
                if(!(alarmOnly && msf345Rec.getMeasStatus().trim().equals(MSF345_MEAS_STATUS_NML))) {

                    int measDate_9    = computeDateReverse(revMeasDate_9)
                    int measTime_9    = computeTimeReverse(revMeasTime as int)
                    String measData   = String.valueOf(measDate_9) + String.valueOf(measTime_9)
                    double measValue  = msf345Rec.getMeasureValue().doubleValue()
                    String conMonMeas = msf345Rec.getPrimaryKey().getCondMonMeas().trim()
                    msf345Found = true
                    condMonRecord = msf345Rec

                    /*
                     * For each Measurement Date & Time
                     * Store the Measurement Value on the corresponding Measurement Type
                     */
                    for(int i = 0; i < (measValueTab.data.length-1); i++) {
                        String data = measValueTab.getDataAt(i)
                        if(data.equals(measData)) {
                            for(int j = 0; j < (subCount-1); j++) {
                                MeasurementTypeTab measTypes = measTableTypes.getAt(j)
                                if(measTypes.type.equals(conMonMeas)) {
                                    measValueTab.setValueAt(i, j, measValue)
                                }
                            }
                            break
                        }
                        if(!data?.trim()) {
                            measValueTab.setDataAt(i, measData)
                            for(int j = 0; j < (subCount-1); j++) {
                                MeasurementTypeTab measTypes = measTableTypes.getAt(j)
                                if(measTypes.type.equals(conMonMeas)) {
                                    measValueTab.setValueAt(i, j, measValue)
                                }
                            }
                            break
                        }
                        if(i > MAX_MEAS_DATE) {
                            info("W50-345-MEAS-DATA-TAB REACHED MAXIMUM VALUE: ${MAX_MEAS_DATE}")
                            break
                        }
                    }
                }
            }
        })
        writeCSV()
    }

    /**
     * Get Tracing Action Description from Table
     * @param tracingAct Tracing Action
     * @return Tracing Action Description
     */
    private String getTracingActionDescription(String tracingAct) {
        info("getTracingActionDescription")
        return readTable("TA", tracingAct)?.getDescription()
    }

    /**
     * Write the Condition Monitor Measurement records into the CSV.
     */
    private void writeCSV() {
        info("writeCSV")
        trt35xRecord = new Trb35xRecord()
        //write header
        if(!writeHeader) {
            /*
             * Initialize csv writer
             */
            def workingDir  = env.workDir
            String taskUUID = this.getTaskUUID()
            String csvPath  = "${workingDir}/${REPORT_NAME_TRB35X}"
            if(taskUUID?.trim()) {
                csvPath = csvPath + "." + taskUUID
            }
            csvPath   = csvPath + ".csv"
            csvFile   = new File(csvPath)
            csvWriter = new BufferedWriter(new FileWriter(csvFile))
            writeCSVColumnHeadings()
            writeHeader = true
        }

        //write detail
        trt35xRecord.componentNo  = equipmentRecord != null ? equipmentRecord.getPrimaryKey().getEquipNo() : equipNo
        trt35xRecord.equipRef     = equipPlantNo
        trt35xRecord.dblQuote1    = "\""
        trt35xRecord.equipDesc    = equipItemName1.trim() + " " + equipItemName2.trim()
        trt35xRecord.dblQuote2    = "\""
        trt35xRecord.egi          = equipmentRecord != null ? equipmentRecord.getEquipGrpId() : batchParams.egi
        trt35xRecord.originalDoc  = equipmentRecord != null ? equipmentRecord.getOriginalDoc() : " "
        trt35xRecord.compCode     = condMonRecord != null ? condMonRecord.getPrimaryKey().getCompPosData()?.substring(0, 4) : batchParams.componentCode
        trt35xRecord.serviceStat  = condMonRecord != null ? getTracingActionDescription(compTracingRecord.getTracingActn().trim()) : " "
        trt35xRecord.condMonPos   = condMonRecord != null ? condMonRecord.getPrimaryKey().getCompPosData()?.substring(6, 13) : condMonPos
        trt35xRecord.condMonType  = condMonRecord != null ? condMonRecord.getPrimaryKey().getCompPosData()?.substring(13) : batchParams.conditionMonitoringType
        trt35xRecord.visInspCode1 = condMonRecord != null ? condMonRecord.getVisInsCodex1() : " "
        trt35xRecord.visInspCode2 = condMonRecord != null ? condMonRecord.getVisInsCodex2() : " "

        String typesDetail = ""
        int i = 0
        String date = measValueTab.getDateFromDataAt(i)
        date = date.replace("0", "")
        while(date?.trim()) {
            trt35xRecord.measDate = "\"" + convertDateFormat(measValueTab.getDateFromDataAt(i), "/") + "\""
            trt35xRecord.measTime = "\"" + convertTimeFormat(measValueTab.getTimeFromDataAt(i), ":") + "\""
            typesDetail = ""
            for(int j = 0; j < (subCount-1); j++) {
                typesDetail += String.format("%s,", measValueTab.getFormmatedValueAt(i, j))
            }
            csvWriter.write(trt35xRecord.writeDetail())
            csvWriter.write(typesDetail)
            csvWriter.write(trt35xRecord.writeDetail2())
            csvWriter.write("\r\n")
            i++
            date = measValueTab.getDateFromDataAt(i)
            date = date.replace("0", "")
        }
    }

    /**
     * Write the CSV column header.
     */
    private void writeCSVColumnHeadings() {
        info("writeCSVColumnHeadings")
        String heading = trt35xRecord.writeHeader()
        String blankLine = trt35xRecord.writeBlankLine()
        String heading2 = trt35xRecord.writeHeader2()
        String typeHeader = "", descHeader = ""
        for(int j = 0; j < (subCount-1); j++) {
            MeasurementTypeTab measTypes = measTableTypes.getAt(j)
            typeHeader += String.format("\"%s\" ,", measTypes.type)
            descHeader += String.format("\"%s\" ,", measTypes.description)
        }
        csvWriter.write(heading)
        csvWriter.write(typeHeader)
        csvWriter.write(heading2)
        csvWriter.write("\r\n")
        csvWriter.write(blankLine)
        csvWriter.write(descHeader)
        csvWriter.write("\r\n")
    }

    //Utility Method - Start
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
     * Convert the date format with specified separator into <code>(dd/mm/ccyyy)</code>.
     * @param dateS date as a String
     * @param separator separator for the
     * @return formatted date as a String
     */
    private String convertDateFormat(String dateS, String separator) {
        info("convertDateFormat ${dateS}")
        if(!dateS?.trim() || dateS.equalsIgnoreCase("0")) {
            dateS = "00000000"
        }
        dateS = dateS.trim().padLeft(8).replace(" ", "0")
        // dd/mm/ccyy
        def formattedString = dateS.substring(6) + separator + dateS.substring(4,6) + separator + dateS.substring(0,4)
        return formattedString
    }

    /**
     * Convert the time format with specified separator into <code>(HH:MM:SS)</code>.
     * @param timeS time as a String
     * @param separator separator for the
     * @return formatted time as a String
     */
    private String convertTimeFormat(String timeS, String separator) {
        info("convertTimeFormat ${timeS}")
        if(!timeS?.trim() || timeS.equalsIgnoreCase("0")) {
            timeS = "000000"
        }
        timeS = timeS.trim().padLeft(6).replace(" ", "0")
        //hh:mm:ss
        def formattedString = timeS.substring(0,2) + separator + timeS.substring(2,4) + separator + timeS.substring(4)
        return formattedString
    }
    //Utility Method - End
}

/**
 * Run the script
 */
ProcessTrb35x process = new ProcessTrb35x()
process.runBatch(binding)