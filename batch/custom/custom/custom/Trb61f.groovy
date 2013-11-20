/**
 *  @Ventyx 2012
 *
 * This batch program will process master listing from FigTree and 
 * either update the Ellipse Equipment Register or create a new Equipment 
 * Register record.
 *
 * Developed based on <b>FDD Interfacing Ellipse-Fleet V02</b>
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;
import java.io.File;
import com.mincom.enterpriseservice.ellipse.equipment.*;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.enterpriseservice.ellipse.alternateref.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf601.*;
import com.mincom.ellipse.edoi.ejb.msf602.*;
import com.mincom.ellipse.edoi.ejb.msk600.*;
import com.mincom.ellipse.edoi.ejb.msk601.*;
import com.mincom.ellipse.edoi.ejb.msfx67.*;
import com.mincom.ellipse.edoi.ejb.msfx68.*;
import com.mincom.ellipse.edoi.ejb.msfx69.*;
import com.mincom.ellipse.edoi.ejb.msfx6f.*;
import com.mincom.ellipse.edoi.ejb.msfh67.*;

/**
 * Fleet Vehicle record.
 */
public class FleetVehicle {
    String fleetNumber, fleetDivision, fleetRespCode, vehTypeCode, vehMakeDesc, vehModelDesc, vehTypeDesc, fleetRego, fleetStatus

    /**
     * Intialize new FleetVehicle with value from a column.
     * @param columns columns 
     * <code>
     * index-0 is Fleet Number </br>
     * index-1 is Fleet Division </br>
     * index-2 is Fleet Responsibility Code </br>
     * index-3 is Vehicle Type Code </br>
     * index-4 is Vehicle Make Description </br>
     * index-5 is Vehicle Model Description </br>
     * index-6 is Vehicle Type Description </br>
     * index-7 is Fleet Rego </br>
     * index-8 is Fleet Status </br>
     * </code>
     */
    public FleetVehicle(String []columns) {
        this.fleetNumber   = columns[0].replaceAll("\"","")
        this.fleetDivision = columns[1].replaceAll("\"","")
        this.fleetRespCode = columns[2].replaceAll("\"","")
        this.vehTypeCode   = columns[3].replaceAll("\"","")
        this.vehMakeDesc   = columns[4].replaceAll("\"","")
        this.vehModelDesc  = columns[5].replaceAll("\"","")
        this.vehTypeDesc   = columns[6].replaceAll("\"","")
        this.fleetRego     = columns[7].replaceAll("\"","")
        this.fleetStatus   = columns[8].replaceAll("\"","")
    }
}

/**
 * Main Process of Trb61f.
 */
public class ProcessTrb61f extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME           = "TRB61FA"
    private static final String INPUT_FILENAME        = "TRT61F"
    private static final String EQUIPMENT_SERVICE     = "EQUIPMENT"
    private static final String ALTERNATEREF_SERVICE  = "ALTERNATEREF"
    private static final String TABLE_SERVICE         = "TABLE"
    private static final String EQUIP_REF_PREFIX      = "FL"
    private static final String PARENT_EQUIP_REF      = "TGF"
    private static final String ACCOUNT_CODE_SUFFIX   = "100756"
    private static final String EQUIP_STATUS_DEFAULT  = "IS"
    private static final String REF_CODE_TYPE_EN      = "EN"
    private static final String FLEET_STATUS_O        = "O"
    private static final String EQUIP_COSTING_FLAG_A  = "A"
    private static final String TABLE_CODE_FES        = "#FES"
    private static final String TABLE_CODE_FET        = "#FET"
    private static final String TABLE_CODE_FLD        = "#FLD"
    private static final String DASHED_LINE           = String.format("%132s"," ").replace(' ', '-')
    private static final String ERROR_LINE_FORMAT_1   = "ROW % 5d - %s"
    private static final String ERROR_LINE_FORMAT_2   = "ROW % 5d"
    private static final int COLUMN_SIZE = 9
    private static final int ROW         = 20

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private String version = "0001"

    private File inputFile
    /*
     * Reporting variables
     */
    private def reportWriter
    private boolean writeErrorHeader
    private LinkedHashMap<String, String> errMessages
    private String validationMessage
    private int recordRead, equipmentSkipped, equipmentCreated, equipmentUpdated, recordError, colloquialCreated

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : " + version)
        info("No input parameters.")
        try {
            processBatch()
        } catch(Exception e) {
            info("Process terminated. ${e.getMessage()}")
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
        if(validateInputFile()) {
            processFleetMasterFile()
        }
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        //write report header
        reportWriter.write("${REPORT_NAME} Fleet Master File Import Control Report".center(132))
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
        reportWriter.write("Processing:")
        reportWriter.write(String.format("\tNo of Input Rec Read      : % 5d", recordRead))
        reportWriter.write(String.format("\tNo of Equipment Skipped   : % 5d", equipmentSkipped))
        reportWriter.write(String.format("\tNo of Equipment Created   : % 5d", equipmentCreated))
        reportWriter.write(String.format("\tNo of Equipment Updated   : % 5d", equipmentUpdated))
        reportWriter.write(String.format("\tNo of Input Rec Errors    : % 5d", recordError))
        reportWriter.write(String.format("\tNo of Colloquials Created : % 5d", colloquialCreated))
        reportWriter.write("")
        //write error messages
        if(!errMessages.isEmpty()) {
            reportWriter.write(DASHED_LINE)
            reportWriter.write("Field Ref/Value                 Error/Warning Message Column Hdg ")
            reportWriter.write(DASHED_LINE)
            errMessages.each {key, value ->
                reportWriter.write(String.format("%-30s  %-99s",
                        key?.length() > 30 ? key?.substring(0,30) : key,
                        value?.length() > 99 ? value?.substring(0,99) : value))
            }
            reportWriter.write(DASHED_LINE)
            reportWriter.write("")
        }
        //write validation message
        if(validationMessage?.trim()) {
            reportWriter.write("Validation Errors:")
            reportWriter.write("------------------")
            reportWriter.write(validationMessage)
            reportWriter.write("")
        }
        reportWriter.close()
    }

    //additional methods starts here
    /**
     * Initialize the report content.
     */
    private void initialize() {
        info("initialize")
        reportWriter = report.open(REPORT_NAME)
        errMessages  = new LinkedHashMap<String, String>()
        validationMessage = ""
    }

    /**
     * Initialize and validate the input file, it should be exist and contains record.
     * @return true if input file is valid, false otherwise.
     */
    private boolean validateInputFile() {
        info("validateInputFile")
        //intialize input file
        String workingDir    = env.workDir
        String taskUUID      = getTaskUUID()
        String inputFilePath = "${workingDir}/${INPUT_FILENAME}"
        if(taskUUID?.trim()) {
            inputFilePath    = inputFilePath  + "." + taskUUID
        }
        inputFile = new File(inputFilePath)
        //validate the input file, it should be exist and contains record
        boolean valid = true
        if(inputFile != null && !inputFile.exists()) {
            valid = false
            validationMessage = "ERROR: ${INPUT_FILENAME} Input File does not exist. Processing terminated."
            info(validationMessage)
        } else {
            BufferedReader input = new BufferedReader(new FileReader(inputFile))
            if(input != null && input.readLine() == null) {
                valid = false
                validationMessage = "ERROR: No records found on ${INPUT_FILENAME} Input File. Processing terminated."
                info(validationMessage)
            }
            input.close()
        }
        return valid
    }

    /**
     * Process Fleet Master File and either update the Ellipse Equipment Register 
     * or create a new Equipment Register record.
     */
    private void processFleetMasterFile() {
        info("processFleetMasterFile")
        inputFile.splitEachLine(",") {
            recordRead++
            String []columns = it
            if(columns.size() == COLUMN_SIZE) {
                FleetVehicle fleet = new FleetVehicle(columns)
                //Fleet Number is mandatory field, cannot be empty.
                if(fleet.fleetNumber.trim()) {
                    try {
                        //Only a Vehicle Status other than 'O' will be processed.
                        if(!fleet.fleetStatus.equals("O")) {
                            String equipmentNo = ""
                            EquipmentServiceReadReplyDTO equipReplyDTO = readEquipment(fleet.fleetNumber)
                            //Exist, compare the Equipment Register against FleetVehicle
                            if(equipReplyDTO) {
                                equipmentNo = equipReplyDTO.equipmentNo
                                EquipmentServiceModifyReplyDTO equipModifyDTO = compareAndUpdateEquipment(equipReplyDTO, fleet)
                                if(equipModifyDTO) {
                                    equipmentUpdated++
                                }
                            }
                            //Does not exist, create new Equipment Register based on the FleetVehicle
                            else {
                                EquipmentServiceCreateReplyDTO equipCreateDTO = createEquipment(fleet)
                                if(equipCreateDTO) {
                                    equipmentNo = equipCreateDTO.equipmentNo
                                    equipmentCreated++
                                }
                            }
                            //Check Alternate References && Fleet Rego – optional. If blank ignore.
                            if(fleet.fleetRego?.trim().length() > 0 && equipmentNo.trim().length() > 0) {
                                List<AlternateRefServiceRetrieveReplyDTO> altRefList = retrieveAlternateReferences(equipmentNo)
                                //If the Fleet Rego value does not already exist in the set of Colloquials it needs to be created.
                                if(!ifColloquialsExist(altRefList, fleet.fleetRego)) {
                                    AlternateRefServiceCreateReplyDTO altRefCreateDTO = createAlternateReference(equipmentNo, fleet.fleetRego)
                                    if(altRefCreateDTO) {
                                        colloquialCreated++
                                    }
                                }
                            }
                        } else {
                            equipmentSkipped++
                        }
                    } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
                        recordError++
                        String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
                        String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
                        String errorField = serviceExc.getErrorMessages()[0].getFieldName()
                        errMessages.put(String.format(ERROR_LINE_FORMAT_1, recordRead, errorField),
                                "${errorCode} - ${errorMsg}")
                    }
                } else {
                    recordError++
                    errMessages.put(String.format(ERROR_LINE_FORMAT_1, recordRead, "FleetNumber"),
                            "FLEET NUMBER IS EMPTY")
                }
            } else {
                recordError++
                errMessages.put(String.format(ERROR_LINE_FORMAT_2, recordRead),
                        "NUMBER OF COLUMNS IN INPUT FILE IS ${columns.size()} - SHOULD BE ${COLUMN_SIZE}")
            }
        }
    }

    /**
     * Read and return EquipmentServiceReadReplyDTO based on the fleet number.
     * @param fleetNumber fleet number
     * @return EquipmentServiceReadReplyDTO if exist, null if doesn't exist.
     */
    private EquipmentServiceReadReplyDTO readEquipment(String fleetNumber) {
        info("readEquipment")
        EquipmentServiceReadRequiredAttributesDTO equipReqAttributeDTO = new EquipmentServiceReadRequiredAttributesDTO()
        equipReqAttributeDTO.returnEquipmentNo = true
        equipReqAttributeDTO.returnAccountCode = true
        equipReqAttributeDTO.returnDistrictCode = true
        equipReqAttributeDTO.returnEquipmentNoDescription1 = true
        equipReqAttributeDTO.returnEquipmentStatus = true
        equipReqAttributeDTO.returnEquipmentType = true
        equipReqAttributeDTO.returnParentEquipmentRef = true

        EquipmentServiceReadRequestDTO equipReadDTO = new EquipmentServiceReadRequestDTO()
        equipReadDTO.setEquipmentRef(EQUIP_REF_PREFIX.concat(fleetNumber))
        equipReadDTO.setRequiredAttributes(equipReqAttributeDTO)

        EquipmentServiceReadReplyDTO equipReplyDTO = null
        try {
            equipReplyDTO = service.get(EQUIPMENT_SERVICE).read(equipReadDTO, true)
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
            String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = serviceExc.getErrorMessages()[0].getFieldName()
            info("Cannot read Equipment ${EQUIP_REF_PREFIX.concat(fleetNumber)} caused by ${errorField}: ${errorCode} - ${errorMsg}.")
        }
        return equipReplyDTO
    }

    /**
     * Create new Equipment based on Fleet Vehicle information.
     * @param fleet Fleet Vehicle
     * @return EquipmentServiceCreateReplyDTO if creation sucees, null if failed.
     */
    private EquipmentServiceCreateReplyDTO createEquipment(FleetVehicle fleet) {
        info("createEquipment")
        String fleetEquipDstrct = getMappedEquipmentDistrictCode(fleet.fleetDivision)
        String fleetEquipType   = getMappedEquipmentType(fleet.vehTypeCode)
        String fleetEquipStatus = fleet.fleetStatus?.trim()? getMappedEquipmentStatus(fleet.fleetStatus) : EQUIP_STATUS_DEFAULT
        String fleetAccountCode = fleet.fleetRespCode.concat(ACCOUNT_CODE_SUFFIX)
        //The description is created from the Fleet Descriptions for Make, Model and Type with trailing
        //spaces removed and a single space between the values. Max 40 Chars
        String fleetEquipDesc   = "${fleet.vehMakeDesc.trim()} ${fleet.vehModelDesc.trim()} ${fleet.vehTypeDesc.trim()}"
        fleetEquipDesc  = fleetEquipDesc.length() > 40 ? fleetEquipDesc.substring(0, 40) : fleetEquipDesc

        EquipmentServiceCreateRequiredAttributesDTO equipReqAtttributeDTO = new EquipmentServiceCreateRequiredAttributesDTO()
        equipReqAtttributeDTO.returnEquipmentNo = true

        EquipmentServiceCreateRequestDTO equipCreateDTO = new EquipmentServiceCreateRequestDTO()
        equipCreateDTO.setRequiredAttributes(equipReqAtttributeDTO)
        equipCreateDTO.setEquipmentClass(EQUIP_REF_PREFIX)
        equipCreateDTO.setPlantCode0(EQUIP_REF_PREFIX)
        equipCreateDTO.setPlantCode1(fleet.fleetNumber)
        equipCreateDTO.setParentEquipmentRef(PARENT_EQUIP_REF)
        equipCreateDTO.setAccountCode(fleetAccountCode)
        equipCreateDTO.setDistrictCode(fleetEquipDstrct)
        equipCreateDTO.setEquipmentNoDescription1(fleetEquipDesc)
        equipCreateDTO.setEquipmentStatus(fleetEquipStatus)
        equipCreateDTO.setEquipmentType(fleetEquipType)
        equipCreateDTO.setCostingFlag(EQUIP_COSTING_FLAG_A)
        equipCreateDTO.setActiveFlag(true)
        return service.get(EQUIPMENT_SERVICE).create(equipCreateDTO, false)
    }

    /**
     * Get mapped District Code via #FLD Table Code based on the fleet's division. 
     * @param fleetDivision fleet's division 
     * @return District Code
     */
    private String getMappedEquipmentDistrictCode(String fleetDivision) {
        info("getMappedEquipmentDistrictCode")
        String districtCode = ""
        ArrayList<TableServiceRetrieveReplyDTO> tableList = retrieveFleetDistrict(fleetDivision)
        for(TableServiceRetrieveReplyDTO tableDTO : tableList) {
            String locCode = tableDTO.getTableCode().substring(0, 2) //First 2 Characters of Table Code
            String divCode = tableDTO.getTableCode().substring(2, 3) //3rd Character of Table Code
            String ellCode = tableDTO.getTableCode().substring(3, 4) //4th Character of Table Code
            String ellDstrct = tableDTO.getDescription().substring(0, 4) //First four characters of Description.
            //When mapping from Fleet to Ellipse the first 2 characters
            //match the Fleet Location and the Ellipse District to set is where the Ellipse Code = 1
            if(fleetDivision.equals(locCode) && ellCode.equals("1")) {
                districtCode = ellDstrct
                break
            }
        }
        return districtCode
    }

    /**
     * Get mapped Equipment Status via #FES Table Code based on the fleet's status.
     * @param fleetStatus fleet's status
     * @return Equipment Status
     */
    private String getMappedEquipmentStatus(String fleetStatus) {
        info("getMappedEquipmentStatus")
        String equipmentStatus = ""
        TableServiceReadReplyDTO tableReplyDTO = readTable(TABLE_CODE_FES, fleetStatus)
        if(tableReplyDTO) {
            equipmentStatus = tableReplyDTO.getAssociatedRecord().trim()
        }
        return equipmentStatus
    }

    /**
     * Get mapped Equipment Code via #FET Table Code based on the fleet's type code.
     * @param vehTypeCode fleet's type code
     * @return Equipment Code
     */
    private String getMappedEquipmentType(String vehTypeCode) {
        info("getMappedEquipmentType")
        String equipmentType = ""
        TableServiceReadReplyDTO tableReplyDTO = readTable(TABLE_CODE_FET, vehTypeCode)
        if(tableReplyDTO) {
            equipmentType = tableReplyDTO.getAssociatedRecord().trim()
        }
        return equipmentType
    }

    /**
     * Read Table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO if exist, null if doesn't exist
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable")
        TableServiceReadRequiredAttributesDTO tableReqAttributeDTO = new TableServiceReadRequiredAttributesDTO()
        tableReqAttributeDTO.returnTableCode = true
        tableReqAttributeDTO.returnTableType = true
        tableReqAttributeDTO.returnAssociatedRecord = true
        tableReqAttributeDTO.returnDescription = true

        TableServiceReadRequestDTO tableRequestDTO = new TableServiceReadRequestDTO()
        tableRequestDTO.setTableType(tableType)
        tableRequestDTO.setTableCode(tableCode)
        tableRequestDTO.setRequiredAttributes(tableReqAttributeDTO)
        return service.get(TABLE_SERVICE).read(tableRequestDTO, false)
    }

    /**
     * Compare Equipment Register against Fleet Vehicle from the input file.
     * If there are differencies, update the Equipment Register.
     * @param equipDTO EquipmentServiceReadReplyDTO to be checked
     * @param fleet FleetVehicle to be checked
     * @return EquipmentServiceModifyReplyDTO if Equipment is modified, false otherwise
     */
    private EquipmentServiceModifyReplyDTO compareAndUpdateEquipment(EquipmentServiceReadReplyDTO equipDTO, FleetVehicle fleet) {
        info("compareAndUpdateEquipment")
        String fleetEquipDstrct = getMappedEquipmentDistrictCode(fleet.fleetDivision)
        String fleetEquipType   = getMappedEquipmentType(fleet.vehTypeCode)
        String fleetEquipStatus = getMappedEquipmentStatus(fleet.fleetStatus)
        String fleetAccountCode = fleet.fleetRespCode.concat(ACCOUNT_CODE_SUFFIX)
        //The description is created from the Fleet Descriptions for Make, Model and Type with trailing
        //spaces removed and a single space between the values. Max 40 Chars
        String fleetEquipDesc   = "${fleet.vehMakeDesc.trim()} ${fleet.vehModelDesc.trim()} ${fleet.vehTypeDesc.trim()}"
        fleetEquipDesc  = fleetEquipDesc.length() > 40 ? fleetEquipDesc.substring(0, 40) : fleetEquipDesc

        boolean needModify = false
        EquipmentServiceModifyReplyDTO equipReplyDTO = null

        EquipmentServiceModifyRequiredAttributesDTO equipReqAttributeDTO = new EquipmentServiceModifyRequiredAttributesDTO()
        equipReqAttributeDTO.returnEquipmentNo = true

        EquipmentServiceModifyRequestDTO equipModifyDTO = new EquipmentServiceModifyRequestDTO()
        equipModifyDTO.setRequiredAttributes(equipReqAttributeDTO)
        //Only update the fields that require change leaving the others unset.
        //Equip No is always the same, no need to compare it.
        equipModifyDTO.setEquipmentNo(equipDTO.getEquipmentNo())
        equipModifyDTO.setCostingFlag(EQUIP_COSTING_FLAG_A)
        equipModifyDTO.setActiveFlag(true)

        //compare account code - mandatory
        String accountCode = equipDTO.getAccountCode().length() < 9 ?
                equipDTO.getAccountCode().padRight(9) : equipDTO.getAccountCode()
        if(!accountCode.substring(0, 9).equals(fleetAccountCode.trim())) {
            needModify = true
            equipModifyDTO.setAccountCode(fleetAccountCode)
        }
        //compare district code - mandatory
        if(!equipDTO.getDistrictCode().trim().equals(fleetEquipDstrct.trim())) {
            needModify = true
            equipModifyDTO.setDistrictCode(fleetEquipDstrct)
        }
        //compare type - mandatory
        if(!equipDTO.getEquipmentType().trim().equals(fleetEquipType.trim())) {
            needModify = true
            equipModifyDTO.setEquipmentType(fleetEquipType)
        }
        //parent equip ref - Set to 'TGF' if unset.
        if(equipDTO.getParentEquipmentRef()?.trim().length() == 0) {
            needModify = true
            equipModifyDTO.setParentEquipmentRef(PARENT_EQUIP_REF)
        }
        //compare description no 1 optional – assemble the description from what is available including blanks.
        if(!equipDTO.getEquipmentNoDescription1().trim().equals(fleetEquipDesc.trim())) {
            needModify = true
            equipModifyDTO.setEquipmentNoDescription1(fleetEquipDesc)
        }
        //compare status – optional. If blank ignore.
        if(fleetEquipStatus?.trim().length() > 0 && !equipDTO.getEquipmentStatus().trim().equals(fleetEquipStatus.trim())) {
            needModify = true
            equipModifyDTO.setEquipmentStatus(fleetEquipStatus)
        }

        if(needModify) {
            equipReplyDTO = service.get(EQUIPMENT_SERVICE).modify(equipModifyDTO, false)
        } else {
            equipmentSkipped++
        }

        return equipReplyDTO
    }

    /**
     * Retrieve Alternate References for specified equipment number.
     * @param equipmentNo specified equipment number
     * @return List of AlternateRefServiceRetrieveReplyDTO
     */
    private List<AlternateRefServiceRetrieveReplyDTO> retrieveAlternateReferences(String equipmentNo) {
        info("retrieveAlternateReferences")

        AlternateRefServiceRetrieveRequestDTO altRefRequestDTO = new AlternateRefServiceRetrieveRequestDTO()
        altRefRequestDTO.setRefCodeType(REF_CODE_TYPE_EN)
        altRefRequestDTO.setAltRefCode(equipmentNo)

        AlternateRefServiceRetrieveRequiredAttributesDTO altRefReqAttr = new AlternateRefServiceRetrieveRequiredAttributesDTO()
        altRefReqAttr.returnAltRefCode = true
        altRefReqAttr.returnAltRefType = true
        altRefReqAttr.returnAltReference = true

        List<AlternateRefServiceRetrieveReplyDTO> altRefReplyList = new ArrayList<AlternateRefServiceRetrieveReplyDTO>()
        def restart = ""
        boolean firstLoop = true
        while (firstLoop || (restart !=null && restart.trim().length() > 0)){
            AlternateRefServiceRetrieveReplyCollectionDTO altRefReplyDTO = retrAltRef(altRefRequestDTO, altRefReqAttr, restart)
            firstLoop = false
            restart = altRefReplyDTO.getCollectionRestartPoint()
            altRefReplyList.addAll(altRefReplyDTO.getReplyElements())
        }

        return altRefReplyList
    }

    /**
     * Retrieve Alt Ref
     * @param altRefRequestDTO AlternateRefServiceRetrieveRequestDTO
     * @param altRefReqAttr AlternateRefServiceRetrieveRequiredAttributesDTO
     * @param restart restart point
     * @return AlternateRefServiceRetrieveReplyCollectionDTO
     */
    private AlternateRefServiceRetrieveReplyCollectionDTO retrAltRef(AlternateRefServiceRetrieveRequestDTO altRefRequestDTO,
    AlternateRefServiceRetrieveRequiredAttributesDTO altRefReqAttr, String restart) {
        info("retrAltRef")
        AlternateRefServiceRetrieveReplyCollectionDTO altRefReplyDTO =
                service.get(ALTERNATEREF_SERVICE).retrieve(altRefRequestDTO, altRefReqAttr, ROW, false, restart)
        return altRefReplyDTO
    }

    /**
     * Check if Fleet's Rego exist in the the set of Colloquials.
     * @param altRefList set of Colloquials
     * @param fleetRego Fleet's Rego
     * @return true if exist, false otherwise
     */
    private boolean ifColloquialsExist(List<AlternateRefServiceRetrieveReplyDTO> altRefList, String fleetRego) {
        info("ifColloquialsExist")
        for(AlternateRefServiceRetrieveReplyDTO altRef: altRefList) {
            if(altRef.altReference.trim().equals(fleetRego.trim())) {
                return true
            }
        }
        return false
    }

    /**
     * Create new Alternate Reference (Colloquial) for specified equipment number and fleet's rego.
     * @param equipmentNo specified equipment number
     * @param fleetRego fleet's rego
     * @return AlternateRefServiceCreateReplyDTO if success, null otherwise
     */
    private AlternateRefServiceCreateReplyDTO createAlternateReference(String equipmentNo, String fleetRego) {
        info("createAlternateReference")
        AlternateRefServiceCreateRequiredAttributesDTO  altRefReqAttr = new AlternateRefServiceCreateRequiredAttributesDTO()
        altRefReqAttr.returnAltRefCode = true
        altRefReqAttr.returnAltRefType = true
        altRefReqAttr.returnAltReference = true

        AlternateRefServiceCreateRequestDTO altRefCreateDTO = new AlternateRefServiceCreateRequestDTO()
        altRefCreateDTO.setRefCodeType(REF_CODE_TYPE_EN)
        altRefCreateDTO.setAltRefCode(equipmentNo)
        altRefCreateDTO.setAltReference(fleetRego)
        altRefCreateDTO.setRequiredAttributes(altRefReqAttr)
        return service.get(ALTERNATEREF_SERVICE).create(altRefCreateDTO, false)
    }


    /**
     * Retrieve Table Fleet District (#FLD) based on table code. <br/>
     * Search Method "W", Object Search Type "A".
     * @param tableCode table code
     * @return List of TableServiceRetrieveReplyDTO
     */
    private ArrayList<TableServiceRetrieveReplyDTO> retrieveFleetDistrict(String tableCode) {
        info("retrieveFleetDistrict")
        TableServiceRetrieveRequiredAttributesDTO tableReqAttributeDTO = new TableServiceRetrieveRequiredAttributesDTO()
        tableReqAttributeDTO.returnTableCode = true
        tableReqAttributeDTO.returnTableType = true
        tableReqAttributeDTO.returnAssociatedRecord = true
        tableReqAttributeDTO.returnDescription = true

        TableServiceRetrieveRequestDTO tableRequestDTO = new TableServiceRetrieveRequestDTO()
        tableRequestDTO.setObjectTypeSearch("A")
        tableRequestDTO.setSearchMethod("W")
        tableRequestDTO.setTableType(TABLE_CODE_FLD)
        tableRequestDTO.setTableCode(tableCode)
        TableServiceRetrieveReplyCollectionDTO replyColl
        ArrayList<TableServiceRetrieveReplyDTO> tableList = new ArrayList<TableServiceRetrieveReplyDTO>()
        def restart = ""
        boolean firstLoop = true
        while (firstLoop || (restart !=null && restart.trim().length() > 0)){
            TableServiceRetrieveReplyCollectionDTO tableReplyDTO =
                    retrTable(tableRequestDTO, tableReqAttributeDTO, restart)
            firstLoop = false
            restart = tableReplyDTO.getCollectionRestartPoint()
            tableList.addAll(tableReplyDTO.getReplyElements())
        }

        return tableList
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
                service.get(TABLE_SERVICE).retrieve(tableReqDTO, tableReqAttr, ROW, false, restart)
        return tableReplyDTO
    }
}

/**
 * Run the script
 */
ProcessTrb61f process = new ProcessTrb61f()
process.runBatch(binding)
