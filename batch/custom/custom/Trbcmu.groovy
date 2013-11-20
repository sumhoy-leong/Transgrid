/**
 *  @Ventyx 2012
 *
 * This program reads data from TRICMU input file, <br>
 * then creating new condition monitoring records in Ellipse. <br>
 *
 * Developed based on <b>DDD.Interfacing.Ellipse-MEMS.D02</b>
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.mincom.eql.*;
import com.mincom.eql.impl.*;
import com.mincom.ellipse.edoi.common.exception.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf04d.*;
import com.mincom.ellipse.edoi.ejb.msf076.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf330.*;
import com.mincom.ellipse.edoi.ejb.msf340.*;
import com.mincom.ellipse.edoi.ejb.msf341.*;
import com.mincom.ellipse.edoi.ejb.msf345.*;
import com.mincom.ellipse.edoi.ejb.msf542.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf621.*;
import com.mincom.ellipse.edoi.ejb.msf625.*;
import com.mincom.ellipse.edoi.ejb.msf629.*;
import com.mincom.ellipse.edoi.ejb.msf62w.*;
import com.mincom.ellipse.edoi.ejb.msf645.*;
import com.mincom.ellipse.edoi.ejb.msf660.*;
import com.mincom.ellipse.edoi.ejb.msf720.*;
import com.mincom.ellipse.edoi.ejb.msf920.*;
import com.mincom.ellipse.edoi.ejb.msf930.*;
import com.mincom.ellipse.edoi.ejb.msf93f.*;
import com.mincom.ellipse.edoi.ejb.msf940.*;
import com.mincom.ellipse.edoi.ejb.msf966.*;
import com.mincom.ellipse.edoi.ejb.msfprt.*;
import com.mincom.ellipse.edoi.ejb.msfx55.*;
import com.mincom.ellipse.edoi.ejb.msfx60.*;
import com.mincom.ellipse.edoi.ejb.msfx6f.*;
import com.mincom.ellipse.edoi.ejb.msfx6o.*;
import com.mincom.enterpriseservice.ellipse.condmeasurement.*;
import com.mincom.enterpriseservice.ellipse.equipment.*;
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException;
import com.mincom.enterpriseservice.ellipse.WarningMessageDTO;

/**
 * Main process of Trbcmu
 */
public class ProcessTrbcmu extends SuperBatch {
    private class CMRecord {
        private String equipmentRef, equipmentID, compCode, condMonPos, condMonType,
        condMonMeasure, measureValue, measureDate, measureTime, workOrder

        public String toString() {
            StringBuilder sb = new StringBuilder()
            sb.append(compCode?.trim())
            sb.append("-")
            sb.append(condMonPos?.trim())
            sb.append("-")
            sb.append(condMonType?.trim())
            sb.append("-")
            sb.append(condMonMeasure?.trim())
            return sb.toString()
        }
    }

    /*
     * Constants
     */
    private static final String REPORT_NAME     = "TRBCMUA"
    private static final String INPUT_FILE_NAME = "TRICMU"
    private static final String ERROR_FILE_NAME = "TRECMU"
    private static final String DASHED_LINE     = String.format("%132s"," ").replace(' ', '-')
    private static final int MAX_INSTANCE       = 20
    private static final String SERVICE_NAME_CONDMEASUREMENT = "CONDMEASUREMENT"
    private static final String SERVICE_NAME_EQUIPMENT       = "EQUIPMENT"
    private static final String TABLE_TYPE_CO                = "CO"
    private static final String TABLE_TYPE_PM                = "PM"
    private static final String TABLE_TYPE_OI                = "OI"
    private static final String TABLE_TYPE_MS                = "MS"
    private static final String DISTRCT_CODE_GRID            = "GRID"
    private static final Integer CMU_CHARS_LENGTH          = 105
    private static final SimpleDateFormat MEAS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd")
    private static final SimpleDateFormat MEAS_TIME_FORMAT = new SimpleDateFormat("HHmmss")
    
    private ArrayList<String> tableCOCodes
    private ArrayList<String> tablePMCodes
    private ArrayList<String> tableOICodes
    private ArrayList<String> tableMSCodes
    
    private String currEquipmentNo = " "
    private String currEquipmentRef = " "
    private String currWorkOrder = " "
    private Boolean sameEquipAsPrev = false
    private Boolean currEquipmentValid = false

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 4

    /*
     * Variables
     */
    private def workingDir
    private def reportWriter
    private String validationErrorMessage
    private File cmuInputFile, cmuOutputFile
    private Writer fileWriter
    private int lineCount, processedRecord
    private boolean errorExist

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
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRBCMU ERROR TRACE ", e)
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
            processInputFile()
        } else {
            //input file does not valid
            errorExist = true
        }
        info("end processBatch")
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        if(!errorExist) {
            reportWriter.write("")
            reportWriter.write("${REPORT_NAME} Create Cond. Mon. Results Summary Report".center(132))
            reportWriter.write("")
            reportWriter.write(String.format("%d RECORDS PROCESSED.", processedRecord))
        }
        if(!validationErrorMessage.isEmpty()) {
            reportWriter.write("")
            reportWriter.write("Validation Errors:")
            reportWriter.write("------------------")
            reportWriter.write(validationErrorMessage)
            reportWriter.write("")
        }
        reportWriter.write("")
        reportWriter.close()
        if(fileWriter) {
            fileWriter.close()
        }
        info("end printBatchReport")
    }

    /**
     * Initialize the working directory, report writer, and other variables.
     */
    private void initialize() {
        info("initialize")
        workingDir    = env.workDir
        reportWriter  = report.open(REPORT_NAME)
        validationErrorMessage = ""
        errorExist    = false
        //Error Report
        String fileName = "${workingDir}/${ERROR_FILE_NAME}"
        String uuid = getTaskUUID()
        if(uuid?.trim()){
            fileName = fileName + "." + uuid
        }
        cmuOutputFile = new File(fileName)
        if(!cmuOutputFile.exists()) {
            cmuOutputFile.createNewFile()
        }
        fileWriter = new FileWriter(cmuOutputFile)
        
        tableCOCodes = new ArrayList<String>()
        tablePMCodes = new ArrayList<String>()
        tableOICodes = new ArrayList<String>()
        tableMSCodes = new ArrayList<String>()
        
        info("end initialize")
    }
    

    /**
     * Validate the input file, it should be exist in the work dir and it is not empty.
     * @return true if input file is valid, false otherwise.
     */
    private boolean validateInputFile() {
        info("validateInputFile")
        boolean valid = true
        String fileName = "$workingDir/$INPUT_FILE_NAME"
        String uuid = getTaskUUID()
        if(uuid?.trim()){
            fileName = fileName + "." + uuid
        }
        cmuInputFile = new File(fileName)
        if(cmuInputFile && !cmuInputFile.exists()) {
            valid = false
            validationErrorMessage = "${INPUT_FILE_NAME} does not exist, process terminated."
            info(validationErrorMessage)
        } else {
            BufferedReader input = new BufferedReader(new FileReader(cmuInputFile))
            if(input != null && input.readLine() == null) {
                valid = false
                validationErrorMessage = "${INPUT_FILE_NAME} is empty, process terminated."
                info(validationErrorMessage)
            }
            input.close()
        }
        info("end validateInputFile")
        return valid
    }

    /**
     * Process input file:
     * <li>Extract CMU data from the input file.</li> 
     * <li>Validate extracted CMU data.</li> 
     * <li>Create new CM based on extracted CMU data.</li> 
     */
    private void processInputFile() {
        info("processInputFile")
        /*
         * Use buffering, reading one line at a time
         * FileReader always assumes default encoding is OK!
         */
        BufferedReader input = new BufferedReader(new FileReader(cmuInputFile))
        try {
            String line = null
            lineCount = 0
            processedRecord = 0
            while ((line = input.readLine()) != null){
                lineCount++
                if(line.length() < CMU_CHARS_LENGTH) {
                    writeErrorToReport(lineCount, "RECORD LENGTH",
                            "Invalid Record Length, it should be ${CMU_CHARS_LENGTH}.")
                    writeErrorLine(line)
                } else {
                    //Extract CMU Data
                    CMRecord cmRec = new CMRecord()
                    cmRec.equipmentRef   = line.substring(0, 30).trim()
                    cmRec.equipmentID    = line.substring(30, 42).trim()
                    cmRec.compCode       = line.substring(42, 46).trim()
                    cmRec.condMonPos     = line.substring(46, 53).trim()
                    cmRec.condMonType    = line.substring(53, 55).trim()
                    cmRec.condMonMeasure = line.substring(55, 64).trim()
                    cmRec.measureValue   = line.substring(64, 83).trim()
                    cmRec.measureDate    = line.substring(83, 91).trim()
                    cmRec.measureTime    = line.substring(91, 97).trim()
                    cmRec.workOrder      = line.substring(97, 105).trim()


                    if(validateCondMonRecord(cmRec)) {
                        boolean sucess = false
                        if(checkEquipRef(cmRec)) {
                            sucess = modifyCondMon(cmRec) != null
                        } else {
                            sucess = createNewCondMon(cmRec) != null
                        }

                        if(sucess){
                            if(!(sameEquipAsPrev && currWorkOrder.trim().equals(cmRec.workOrder.trim()))) {
                                cmRec.condMonMeasure = '000/WO'
                                cmRec.measureValue = cmRec.workOrder
    
                                if(checkEquipRef(cmRec)) {
                                    modifyCondMon(cmRec)
                                } else {
                                    createNewCondMon(cmRec)
                                }
                                currWorkOrder = cmRec.workOrder
                            }
                            processedRecord++
                        } else {
                            writeErrorToReport(lineCount,
                                    "${cmRec?.equipmentRef} - ${cmRec.condMonMeasure}",
                                    "CANNOT CREATE COND MON MEAS.")
                            writeErrorLine(line)
                        }
                    } else {
                        writeErrorLine(line)
                    }
                }
            }
        } catch(IOException ioEx) {
            info("IOException occured during processing input file: ${ioEx.toString()}")
            throw ioEx
        } catch(Exception e) {
            info("Unknwon exception occured during processing input file: ${e.toString()}")
            throw e
        } finally {
            info("end processInputFile")
            input.close()
        }
    }

    /**
     * Create new Cond Mon record using service call.
     * @param cmRec Cond Mon record
     * @return CondMeasurementServiceCreateReplyDTO
     */
    private CondMeasurementServiceCreateReplyDTO createNewCondMon(CMRecord cmRec) {
        info("createNewCondMon")
        
        CondMeasurementServiceCreateReplyDTO returnDTO = null
        
        try {
            CondMeasurementServiceCreateRequiredAttributesDTO reqAttr =
                    new CondMeasurementServiceCreateRequiredAttributesDTO()
            reqAttr.setReturnEquipmentRef(true)
            reqAttr.setReturnCompCode(true)
            reqAttr.setReturnCondMonPos(true)
            reqAttr.setReturnCondMonType(true)
            reqAttr.setReturnCondMonMeas(true)
            reqAttr.setReturnMeasureDate(true)
            reqAttr.setReturnMeasureTime(true)
            reqAttr.setReturnMeasureValue(true)

            Date date = MEAS_DATE_FORMAT.parse(cmRec.measureDate)
            Date time = MEAS_TIME_FORMAT.parse(cmRec.measureTime)
            Calendar measDate = Calendar.getInstance()
            Calendar measTime = Calendar.getInstance()
            measDate.setTime(date)
            measTime.setTime(time)

            CondMeasurementServiceCreateRequestDTO cmCreateDTO =
                    new CondMeasurementServiceCreateRequestDTO()
            cmCreateDTO.setEquipmentRef(cmRec.equipmentRef)
            cmCreateDTO.setCompCode(cmRec.compCode)
            cmCreateDTO.setCondMonPos(cmRec.condMonPos)
            cmCreateDTO.setCondMonType(cmRec.condMonType)
            cmCreateDTO.setCondMonMeas(cmRec.condMonMeasure)
            cmCreateDTO.setMeasureValue(cmRec.measureValue)
            cmCreateDTO.setMeasureDate(measDate)
            cmCreateDTO.setMeasureTime(measTime)
            cmCreateDTO.setRequiredAttributes(reqAttr)
            
            returnDTO = service.get(SERVICE_NAME_CONDMEASUREMENT).create(cmCreateDTO, true)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            logExceptionService("${SERVICE_NAME_CONDMEASUREMENT}.create", serviceExc)
        }
        
        info("end createNewCondMon")
        return returnDTO 
    }

    /**
     * Modify existing Cond Mon record using service call.
     * @param cmRec Cond Mon record
     * @return CondMeasurementServiceCreateReplyDTO
     */
    private CondMeasurementServiceModifyReplyDTO modifyCondMon(CMRecord cmRec) {
        info("modifyCondMon")
        
        CondMeasurementServiceModifyReplyDTO returnDTO = null
        try {
            CondMeasurementServiceModifyRequiredAttributesDTO reqAttr =
                    new CondMeasurementServiceModifyRequiredAttributesDTO()
            reqAttr.setReturnEquipmentRef(true)
            reqAttr.setReturnCompCode(true)
            reqAttr.setReturnCondMonPos(true)
            reqAttr.setReturnCondMonType(true)
            reqAttr.setReturnCondMonMeas(true)
            reqAttr.setReturnMeasureDate(true)
            reqAttr.setReturnMeasureTime(true)
            reqAttr.setReturnMeasureValue(true)

            Date date = MEAS_DATE_FORMAT.parse(cmRec.measureDate)
            Date time = MEAS_TIME_FORMAT.parse(cmRec.measureTime)
            Calendar measDate = Calendar.getInstance()
            Calendar measTime = Calendar.getInstance()
            measDate.setTime(date)
            measTime.setTime(time)

            CondMeasurementServiceModifyRequestDTO cmDTO =
                    new CondMeasurementServiceModifyRequestDTO()
            cmDTO.setEquipmentRef(cmRec.equipmentRef)
            cmDTO.setCompCode(cmRec.compCode)
            cmDTO.setCondMonPos(cmRec.condMonPos)
            cmDTO.setCondMonType(cmRec.condMonType)
            cmDTO.setCondMonMeas(cmRec.condMonMeasure)
            cmDTO.setMeasureValue(cmRec.measureValue)
            cmDTO.setMeasureDate(measDate)
            cmDTO.setMeasureTime(measTime)
            cmDTO.setRequiredAttributes(reqAttr)

            returnDTO = service.get(SERVICE_NAME_CONDMEASUREMENT).modify(cmDTO, true)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            logExceptionService("${SERVICE_NAME_CONDMEASUREMENT}.modify", serviceExc)
        }
            info("end modifyCondMon")
            return returnDTO
    }

    /**
     * Read Table by using service call.
     * @param tableType Table Type
     * @param tableCode Table Code
     * @return MSF010Rec
     */
    private MSF010Rec readTable(String tableType, String tableCode) {
        info("readTable; table type ${tableType}, table code ${tableCode}")
        MSF010Rec returnRec = null
        try {
            MSF010Key key = new MSF010Key()
            key.setTableType(tableType)
            key.setTableCode(tableCode)
            returnRec = edoi.findByPrimaryKey(key)
        } catch (EDOIObjectNotFoundException e) {
            info(e.message)
        }
        info("end readTable")
        return returnRec
    }


    /**
     * Check if a Cond Mon Record already exist.
     * @param cmRec Cond Mon record
     * @return true if Cond Mon record is exist
     */
    private boolean checkEquipRef(CMRecord cmRec) {
        info("validateEquipRef")
        boolean exist = false

        String compPosData = cmRec.compCode.padRight(4) + "  " + cmRec.condMonPos.padRight(7) + cmRec.condMonType.padRight(2)
        String revMeasData = (99999999 - cmRec.measureDate.toLong()).toString() + (999999 - cmRec.measureTime.toLong()).toString()
        
        try {
            MSF345Key key = new MSF345Key()
            key.setRec_345Type("E")
            key.setEquipNo(currEquipmentNo)
            key.setCompPosData(compPosData)
            key.setCondMonMeas(cmRec.condMonMeasure)
            key.setRevMeasData(revMeasData)
            MSF345Rec rec = edoi.findByPrimaryKey(key)
            exist = true
        }
        catch(EDOIObjectNotFoundException e) {
            
        }
        
        info("end validateEquipRef, exist: ${exist.toString()}")
        return exist
    }

    /**
     * Read the Equipment based on the equipment reference using service.
     * @param equipRef equipment reference
     * @return EquipmentServiceReadReplyDTO
     */
    private EquipmentServiceReadReplyDTO readEquipmentRef(String equipRef) {
        info("readEquipmentRef")

        EquipmentServiceReadReplyDTO returnDTO = null
        try {
            EquipmentServiceReadRequiredAttributesDTO reqAttr = new EquipmentServiceReadRequiredAttributesDTO()
            reqAttr.setReturnEquipmentNo(true)
            reqAttr.setReturnEquipmentRef(true)
            reqAttr.setReturnEquipmentNoDescription1(true)
            reqAttr.setReturnEquipmentNoDescription2(true)

            EquipmentServiceReadRequestDTO dto = new EquipmentServiceReadRequestDTO()
            dto.setEquipmentRef(equipRef)

            returnDTO =  service.get(SERVICE_NAME_EQUIPMENT).read(dto)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info(serviceExc.message)
            logExceptionService("${SERVICE_NAME_EQUIPMENT}.read", serviceExc)
        }
        
        info("end readEquipmentRef")
        return returnDTO
    }

    /**
     * Validate the component code against Table for table type 'CO'.
     * @param compCode checked component code
     * @return true if component code exist on Table 'CO'
     */
    private boolean validateCompCode(String compCode) {
        info("validateCompCode, comp code: ${compCode}")
        Boolean isValid = compCode in tableCOCodes
        if(!isValid) {
            MSF010Rec msf010Rec = readTable("CO", compCode)
            if(msf010Rec) {
                isValid = true
                tableCOCodes.add(msf010Rec.primaryKey.tableCode)
            }
        }
  
        info("end validateCompCode, isValid: ${isValid.toString()}")
        return isValid
    }

    /**
     * Validate the condition monitoring position against Table for table type 'PM'.
     * @param condMonPos checked condition monitoring position
     * @return true if condition monitoring position exist on Table 'PM'
     */
    private boolean validateCondMonPos(String condMonPos) {
        info("validateCondMonPos, cond mon pos: ${condMonPos}")
        Boolean isValid = condMonPos in tablePMCodes
        if(!isValid) {
            MSF010Rec msf010Rec = readTable("PM", condMonPos)
            if(msf010Rec) {
                isValid = true
                tablePMCodes.add(msf010Rec.primaryKey.tableCode)
            }
        }
        info("end validateCondMonPos, isValid: ${isValid.toString()}")
        return isValid
    }

    /**
     * Validate the condition monitoring type against Table for table type 'OI'.
     * @param condMonPos checked condition monitoring type
     * @return true if condition monitoring type exist on Table 'OI'
     */
    private boolean validateCondMonType(String condMonType) {
        info("validateCondMonType, cond mon type: ${condMonType}")
        Boolean isValid = condMonType in tableOICodes
        if(!isValid) {
            MSF010Rec msf010Rec = readTable("OI", condMonType)
            if(msf010Rec) {
                isValid = true
                tableOICodes.add(msf010Rec.primaryKey.tableCode)
            }
        }
        info("end validateCondMonType, isValid: ${isValid.toString()}")
        return isValid
    }

    /**
     * Validate the condition monitoring measurement against Table for table type 'MS'.
     * @param condMonPos checked condition monitoring measurement
     * @return true if condition monitoring measurement exist on Table 'MS'
     */
    private boolean validateCondMonMeas(String condMonMeas) {
        info("validateCondMonMeas, cond mon meas: ${condMonMeas}")
        Boolean isValid = condMonMeas in tableMSCodes
        if(!isValid) {
            MSF010Rec msf010Rec = readTable("MS", condMonMeas)
            if(msf010Rec) {
                isValid = true
                tableMSCodes.add(msf010Rec.primaryKey.tableCode)
            }
        }
        info("end validateCondMonMeas, isValid: ${isValid.toString()}")
        return isValid
    }

    /**
     * Validate the equipment.
     * Equipment is valid if it exist on Ellipse.
     * If the equipment validated is the same as the current equipment stored, don't recheck.
     * @param workOrder checked work order
     * @param equipRef equipment ref from Cond Mon
     * @return true if work order exist
     */
    private boolean validateEquipmentRef(String equipmentRef) {
        info("validateEquipmentRef, equip ref: ${equipmentRef}")
        info("start: currEquipNo ${currEquipmentNo}, currEquipRef ${currEquipmentRef}, currEquipValid ${currEquipmentValid.toString()}")
        Boolean isValid = false
        
        if(equipmentRef.trim().equals(currEquipmentRef.trim())) {
            isValid = currEquipmentValid
            sameEquipAsPrev = true
        }
        else {
            EquipmentServiceReadReplyDTO equipment =  readEquipmentRef(equipmentRef)
            isValid = currEquipmentValid = (equipment!=null)
            if(currEquipmentValid) {
                currEquipmentNo = equipment.getEquipmentNo()
            }
            else {
                currEquipmentNo = " "
            }
            currEquipmentRef = equipmentRef
            sameEquipAsPrev = false
        }
        info("end: currEquipNo ${currEquipmentNo}, currEquipRef ${currEquipmentRef}, currEquipValid ${currEquipmentValid.toString()}")
        info("end validateEquipmentRef, isValid: ${isValid.toString()}")
        return isValid
    }
    
    /**
     * Validate Cond Mon record fields at specfied line.
     * @param cmRec Cond Mon record
     * @return true if Cond Mon record fields are valid, false otherwise
     */
    private boolean validateCondMonRecord(CMRecord cmRec) {
        info("validateCondMonRecord")
        if(!validateEquipmentRef(cmRec.equipmentRef)) {
            writeErrorToReport(lineCount, cmRec.equipmentRef, "Invalid Equipment Reference.")
            info("end validateCondMonRecord")
            return false
        }
        if(!validateCompCode(cmRec.compCode)) {
            writeErrorToReport(lineCount, cmRec.compCode, "Invalid Component Code.")
            info("end validateCondMonRecord")
            return false
        }
        if(!validateCondMonPos(cmRec.condMonPos)) {
            writeErrorToReport(lineCount, cmRec.condMonPos, "Invalid Condition Monitoring Position.")
            info("end validateCondMonRecord")
            return false
        }
        if(!validateCondMonType(cmRec.condMonType)) {
            writeErrorToReport(lineCount, cmRec.condMonType, "Invalid Condition Monitoring Type.")
            info("end validateCondMonRecord")
            return false
        }
        if(!validateCondMonMeas(cmRec.condMonMeasure)) {
            writeErrorToReport(lineCount, cmRec.condMonMeasure, "Invalid Question ID.")
            info("end validateCondMonRecord")
            return false
        }
        if(!cmRec.measureValue.isNumber()) {
            writeErrorToReport(lineCount, cmRec.measureValue, "Invalid Answer Value.")
            info("end validateCondMonRecord")
            return false
        }
        try {
            MEAS_DATE_FORMAT.parse(cmRec.measureDate)
        } catch(java.text.ParseException e) {
            writeErrorToReport(lineCount, cmRec.measureDate, "Invalid Condition Monitoring Date.")
            info("end validateCondMonRecord")
            return false
        }
        try {
            MEAS_TIME_FORMAT.parse(cmRec.measureTime)
        } catch(java.text.ParseException e) {
            writeErrorToReport(lineCount, cmRec.measureTime, "Invalid Condition Monitoring Time.")
            info("end validateCondMonRecord")
            return false
        }
        if(!cmRec.workOrder.isNumber()) {
            writeErrorToReport(lineCount, cmRec.workOrder, "Invalid Work Order Number.")
            info("end validateCondMonRecord")
            return false
        }
        info("end validateCondMonRecord")
        return true
    }

    /**
     * Write error detail into report.
     * @param line error line
     * @param message error message
     */
    private void writeErrorToReport(int line, String field, String message) {
        info("writeErrorToReport")
        if(!errorExist) {
            //write report header
            reportWriter.write("")
            reportWriter.write("${REPORT_NAME} Create Cond. Mon. Results Summary Error Report".center(132))
            reportWriter.write("")
            reportWriter.write(DASHED_LINE)
            reportWriter.write("Line/Field Ref/Value              Error/Warning Message Column Hdg")
            reportWriter.write(DASHED_LINE)
            reportWriter.write("")
            errorExist = true
        }
        reportWriter.write(String.format("% 4d - %-25s  %-90s",
                line,
                field.length() > 25 ? field.substring(0,25) : field,
                message.length() > 90 ? message.substring(0, 90) : message))
        info("end writeErrorToReport")
    }

    /**
     * Log the EnterpriseServiceOperationException.
     * @param serviceName name of the executed service
     * @param serviceExc EnterpriseServiceOperationException
     */
    private void logExceptionService(String serviceName, EnterpriseServiceOperationException serviceExc) {
        info("logExceptionService")
        String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
        String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
        String errorField = serviceExc.getErrorMessages()[0].getFieldName()
        info("Error during execute ${serviceName} caused by ${errorField}: ${errorCode} - ${errorMsg}.")
    }

    /**
     * Write into error report.
     * @param line error line
     */
    private void writeErrorLine(String line) {
        info("writeErrorLine")
        fileWriter.write(line)
        fileWriter.write("\n")
        info("end writeErrorLine")
    }
}

/**
 * Run the script
 */
ProcessTrbcmu process = new ProcessTrbcmu()
process.runBatch(binding)