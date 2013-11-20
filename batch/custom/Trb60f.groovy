/**
 *  @Ventyx 2012
 *  
 * This program imports data from the CondMeasurement input files, <br>
 * then updating data in Ellipse. <br>
 * 
 * Developed based on <b>FDD.Interfacing.Ellipse-TOA.D02.docx</b>
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding

import java.text.SimpleDateFormat

import com.mincom.ellipse.edoi.ejb.msf011.*
import com.mincom.ellipse.edoi.ejb.msf012.*
import com.mincom.ellipse.edoi.ejb.msf096.*
import com.mincom.ellipse.edoi.ejb.msf100.*
import com.mincom.ellipse.edoi.ejb.msf330.*
import com.mincom.ellipse.edoi.ejb.msf340.*
import com.mincom.ellipse.edoi.ejb.msf341.*
import com.mincom.ellipse.edoi.ejb.msf345.*
import com.mincom.ellipse.edoi.ejb.msf346.*
import com.mincom.ellipse.edoi.ejb.msf580.*
import com.mincom.ellipse.edoi.ejb.msf600.*
import com.mincom.ellipse.edoi.ejb.msf601.*
import com.mincom.ellipse.edoi.ejb.msf602.*
import com.mincom.ellipse.edoi.ejb.msf60a.*
import com.mincom.ellipse.edoi.ejb.msf619.*
import com.mincom.ellipse.edoi.ejb.msf650.*
import com.mincom.ellipse.edoi.ejb.msf6a1.*
import com.mincom.ellipse.edoi.ejb.msf900.*
import com.mincom.ellipse.edoi.ejb.msf910.*
import com.mincom.ellipse.edoi.ejb.msf920.*
import com.mincom.ellipse.edoi.ejb.msf930.*
import com.mincom.ellipse.edoi.ejb.msf940.*
import com.mincom.ellipse.edoi.ejb.msf966.*
import com.mincom.ellipse.edoi.ejb.msf967.*
import com.mincom.ellipse.edoi.ejb.msf968.*
import com.mincom.ellipse.edoi.ejb.msfx61.*
import com.mincom.ellipse.edoi.ejb.msfx63.*
import com.mincom.ellipse.edoi.ejb.msfx65.*
import com.mincom.ellipse.edoi.ejb.msfx68.*
import com.mincom.ellipse.edoi.ejb.msfx69.*
import com.mincom.ellipse.edoi.ejb.msfx6a.*
import com.mincom.ellipse.edoi.ejb.msfx6c.*
import com.mincom.ellipse.edoi.ejb.msfx6e.*
import com.mincom.ellipse.edoi.ejb.msfx6f.*
import com.mincom.ellipse.edoi.ejb.msfx6j.*
import com.mincom.ellipse.edoi.ejb.msk600.*
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceRetrieveMntsReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceRetrieveMntsReplyDTO
import com.mincom.eql.Constraint
import com.mincom.eql.impl.QueryImpl

/**
 * Main Process of Trb60f.
 */
public class ProcessTrb60f extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME           = "TRB60FA"
    private static final String INPUT_FILE_GAS_NAME   = "TRI60F"
    private static final String INPUT_FILE_FLUID_NAME = "TRI60D"
    private static final String COND_MON_TYPE_GAS     = "OA"
    private static final String COND_MON_MEAS_GAS     = "TOAE"
    private static final String COND_MON_TYPE_FLUID   = "OQ"
    private static final String COND_MON_MEAS_FLUID   = "TOAF"
    private static final String DASHED_LINE           = String.format("%132s"," ").replace(' ', '-')

    private static final String SERVICE_NAME_CONDMEASUREMENT = "CONDMEASUREMENT"
    private static final String SERVICE_NAME_STDTEXT         = "STDTEXT"
    private static final String NO_COMPONENT_CODE_LITERAL    = "No Component Code setup for this Equip"
    private static final String CANNOT_CREATE_EQUIPMENT      = "Cannot create %s %s"
    private static final String CANNOT_UPDATE_EQUIPMENT      = "Cannot update %s %s"

    private static final SimpleDateFormat MEAS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd")
    private static final SimpleDateFormat MEAS_TIME_FORMAT = new SimpleDateFormat("HHmmss")

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 2

    /*
     * Variables
     */
    private def workingDir
    private def reportWriter
    private boolean writeErrorHeader
    //report
    private int gasRead = 0, gasIgnored = 0, gasRejected = 0, gasWritten = 0, fluidRead = 0, fluidIgnored = 0, fluidRejected = 0, fluidWritten = 0

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : " + version)
        info("No input parameters")
        try {
            processBatch()
        } catch(Exception e) {
            e.printStackTrace()
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
        processGasInputFile()
        processFluidInputFile()
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        reportWriter.write(DASHED_LINE)
        reportWriter.write("")
        reportWriter.write("Condition Monitoring Records processed:")
        reportWriter.write(String.format("Number of OA (GAS) Sets Read              : % 10d"+
                "\nNumber of OA (GAS) Sets Ignored           : % 10d"+
                "\nNumber of OA (GAS) Sets Rejected          : % 10d"+
                "\nNumber of OA (GAS) Sets Written           : % 10d"+
                "\n\nNumber of OQ (FLUID) Sets Read            : % 10d"+
                "\nNumber of OQ (FLUID) Sets Ignored         : % 10d"+
                "\nNumber of OQ (FLUID) Sets Rejected        : % 10d"+
                "\nNumber of OQ (FLUID) Sets Written         : % 10d",
                gasRead, gasIgnored, gasRejected, gasWritten, fluidRead,
                fluidIgnored, fluidRejected, fluidWritten))
        reportWriter.close()
    }

    /**
     * Initialize the working directory, report writer, and other variables.
     */
    private void initialize() {
        info("initialize")
        workingDir    = env.workDir
        reportWriter  = report.open(REPORT_NAME)
        //write report header
        reportWriter.write("TRB60FA Load TOA CM Results Control Report".center(132))
        reportWriter.write(DASHED_LINE)
        writeErrorHeader = false
    }

    /**
     * Process OA Gas input file. If the file does not exist, write warning into the report.
     */
    private void processGasInputFile() {
        info("processGasInputFile")
        String gasInputFilePath  = "${workingDir}/${INPUT_FILE_GAS_NAME}"
        if(taskUUID?.trim()) {
            gasInputFilePath = gasInputFilePath + "." + taskUUID
        }
        File gasInputFile = new File(gasInputFilePath)
        if(gasInputFile != null && gasInputFile.exists()) {
            /* 
             * Use buffering, reading one line at a time 
             * FileReader always assumes default encoding is OK!
             */
            BufferedReader input = new BufferedReader(new FileReader(gasInputFile))
            try {
                String line = null
                while ((line = input.readLine()) != null){
                    //Extract CM Data
                    if(line.length() < 705) {
                        line = line.padRight(705)
                    }
                    String equipNum   = line.substring(0, 12).trim()
                    String apprType   = line.substring(20, 24).trim()
                    String tank       = line.substring(28, 32).trim()
                    String sampleDate = line.substring(40, 50).trim()
                    String sampleNum  = line.substring(56, 68).trim()
                    String testDate   = line.substring(68, 80).trim()
                    String labRefNum  = line.substring(96, 108).trim()
                    String sampledBy  = line.substring(108, 114).trim()
                    String fluidTempC = line.substring(140, 159).trim()
                    String equipCond  = line.substring(179, 183).trim()
                    String commentary = line.substring(451, 705).trim()

                    Date date = MEAS_DATE_FORMAT.parse(sampleDate)
                    Date time = MEAS_TIME_FORMAT.parse(sampledBy)
                    Calendar measDate = Calendar.getInstance()
                    Calendar measTime = Calendar.getInstance()
                    measDate.setTime(date)
                    measTime.setTime(time)

                    //List of CondMeasurementServiceRetrieveMntsReplyDTO
                    List<CondMeasurementServiceRetrieveMntsReplyDTO> cmReplyList = browseCondMonRecords(apprType, tank, COND_MON_TYPE_GAS, equipNum, measDate, measTime)
                    boolean rejected = cmReplyList.isEmpty()
                    //if the reply list is not empty, process the collected records
                    if(!rejected){
                        boolean success = false
                        //Do not create/update the TOAE measurement type if the EQUIPCOND and COMMENTARY are both blank. Add one to the ignore record count and process the next record.
                        if(equipCond.trim().length() == 0 && commentary.trim().length() == 0) {
                            gasIgnored++
                        } else {
                            //If no TOAE records exist, create the TOAE measurement type
                            String msg = CANNOT_UPDATE_EQUIPMENT
                            if(!ifCondMeasExist(COND_MON_MEAS_GAS, cmReplyList)) {
                                //Create an entry with the measurement value (MSF345-MEASURE-VALUE) set to that provided or a default of zero
                                msg = CANNOT_CREATE_EQUIPMENT
                                success = createNewCondMon(equipNum, COND_MON_TYPE_GAS, COND_MON_MEAS_GAS,
                                        equipCond, apprType, tank, measDate, measTime)
                            }
                            //If the measurement exists update the value.
                            else {
                                msg = CANNOT_UPDATE_EQUIPMENT
                                success = updateCondMon(equipNum, COND_MON_TYPE_GAS, COND_MON_MEAS_GAS,
                                        equipCond, apprType, tank, measDate, measTime)
                            }

                            if(success) {
                                //increase the written record
                                gasWritten++
                                //After the entry created/updated, a retrieveMnts must be performed again to retrieve the allocated text key.
                                cmReplyList = browseCondMonRecords(apprType, tank, COND_MON_TYPE_GAS, equipNum, measDate, measTime)
                                String stdTxtKey = getStdTextKeyFromCondMonRecords(COND_MON_MEAS_GAS, cmReplyList)
                                //If the stdTxtKey and COMMENTARY are set replace stdTxtKey with the COMMENTARY
                                if(stdTxtKey.trim().length() > 0 && commentary.trim().length() > 0) {
                                    success = updateStdText(stdTxtKey, commentary)
                                }
                            } else {
                                writeRejectedRecord(equipNum, apprType, tank, COND_MON_TYPE_GAS, COND_MON_MEAS_GAS, sampleDate, sampledBy,
                                        String.format(msg, equipNum, COND_MON_MEAS_GAS))
                                gasRejected++
                            }
                        }
                    } else {
                        //if the reply list is empty (rejected), write rejected record into report control
                        writeRejectedRecord(equipNum, apprType, tank, COND_MON_TYPE_GAS, COND_MON_MEAS_GAS, sampleDate, sampledBy, NO_COMPONENT_CODE_LITERAL)
                        gasRejected++
                    }
                    gasRead++
                }
            }
            finally {
                input.close()
            }
        } else {
            reportWriter.write("${INPUT_FILE_GAS_NAME} does not exist, continue checking ${INPUT_FILE_FLUID_NAME}.")
            info("${INPUT_FILE_GAS_NAME} does not exist, continue checking ${INPUT_FILE_FLUID_NAME}.")
        }
    }


    /**
     * Process OA Fluid input file. If the file does not exist, write warning into the report.
     */
    private void processFluidInputFile() {
        info("processFluidInputFile")
        String fluidInputFilePath  = "${workingDir}/${INPUT_FILE_FLUID_NAME}"
        if(taskUUID?.trim()) {
            fluidInputFilePath = fluidInputFilePath + "." + taskUUID
        }
        File fluidInputFile = new File(fluidInputFilePath)
        if(fluidInputFile != null && fluidInputFile.exists()) {
            /*
             * Use buffering, reading one line at a time
             * FileReader always assumes default encoding is OK!
             */
            BufferedReader input = new BufferedReader(new FileReader(fluidInputFile))
            try {
                String line = null
                while ((line = input.readLine()) != null){
                    //Extract CM Data
                    if(line.length() < 1020) {
                        line = line.padRight(1020)
                    }
                    String equipNum   = line.substring(0, 12).trim()
                    String apprType   = line.substring(20, 24).trim()
                    String tank       = line.substring(28, 32).trim()
                    String sampleDate = line.substring(40, 50).trim()
                    String sampleNum  = line.substring(56, 68).trim()
                    String testDate   = line.substring(68, 80).trim()
                    String labRefNum  = line.substring(96, 108).trim()
                    String sampledBy  = line.substring(108, 114).trim()
                    String fluidTempC = line.substring(140, 159).trim()
                    String equipCond  = line.substring(179, 183).trim()
                    String commentary = line.substring(766, 1020).trim()

                    Date date = MEAS_DATE_FORMAT.parse(sampleDate)
                    Date time = MEAS_TIME_FORMAT.parse(sampledBy)
                    Calendar measDate = Calendar.getInstance()
                    Calendar measTime = Calendar.getInstance()
                    measDate.setTime(date)
                    measTime.setTime(time)

                    //List of CondMeasurementServiceRetrieveMntsReplyDTO
                    List<CondMeasurementServiceRetrieveMntsReplyDTO> cmReplyList = browseCondMonRecords(apprType, tank, COND_MON_TYPE_FLUID, equipNum, measDate, measTime)
                    boolean rejected = cmReplyList.isEmpty()
                    //if the reply list is not empty, process the collected records
                    if(!rejected){
                        boolean success = false
                        //Do not create/update the TOAF measurement type if the EQUIPCOND and COMMENTARY are both blank. Add one to the ignore record count and process the next record.
                        if(equipCond.trim().length() == 0 && commentary.trim().length() == 0) {
                            fluidIgnored++
                        } else {
                            String msg = CANNOT_UPDATE_EQUIPMENT
                            //If no TOAF records exist, create the TOAF measurement type
                            if(!ifCondMeasExist(COND_MON_MEAS_FLUID, cmReplyList)) {
                                //Create an entry with the measurement value (MSF345-MEASURE-VALUE) set to that provided or a default of zero
                                msg = CANNOT_CREATE_EQUIPMENT
                                success = createNewCondMon(equipNum, COND_MON_TYPE_FLUID, COND_MON_MEAS_FLUID,
                                        equipCond, apprType, tank, measDate, measTime)
                            }
                            //If the measurement exists update the value.
                            else {
                                msg = CANNOT_UPDATE_EQUIPMENT
                                success = updateCondMon(equipNum, COND_MON_TYPE_FLUID, COND_MON_MEAS_FLUID,
                                        equipCond, apprType, tank, measDate, measTime)
                            }

                            if(success) {
                                //increase the written record
                                fluidWritten++
                                //After the entry created/updated, a retrieveMnts must be performed again to retrieve the allocated text key.
                                cmReplyList = browseCondMonRecords(apprType, tank, COND_MON_TYPE_FLUID, equipNum, measDate, measTime)
                                String stdTxtKey = getStdTextKeyFromCondMonRecords(COND_MON_MEAS_FLUID, cmReplyList)
                                //If the stdTxtKey and COMMENTARY are set replace stdTxtKey with the COMMENTARY
                                if(stdTxtKey.trim().length() > 0 && commentary.trim().length() > 0) {
                                    updateStdText(stdTxtKey, commentary)
                                }
                            } else {
                                writeRejectedRecord(equipNum, apprType, tank, COND_MON_TYPE_FLUID, COND_MON_MEAS_FLUID, sampleDate, sampledBy,
                                        String.format(msg, equipNum, COND_MON_MEAS_FLUID))
                                fluidRejected++
                            }
                        }
                    } else {
                        //if the reply list is empty (rejected), write rejected record into report control
                        writeRejectedRecord(equipNum, apprType, tank, COND_MON_TYPE_FLUID, COND_MON_MEAS_FLUID, sampleDate, sampledBy, NO_COMPONENT_CODE_LITERAL)
                        fluidRejected++
                    }
                    fluidRead++
                }
            }
            finally {
                input.close()
            }
        } else {
            reportWriter.write("${INPUT_FILE_FLUID_NAME} does not exist, process terminated.")
            info("${INPUT_FILE_FLUID_NAME} does not exist, process terminated.")
        }
    }

    /**
     * Browse Cond Mon records based on given constraints.
     * @param apprType component code from Cond Mon
     * @param tank condition monitoring position from Cond Mon
     * @param condMonType condition monitoring type, OA or OQ
     * @param equipNum equipment number
     * @param measDate measurement date
     * @param measTime measurement time
     * @return List of browsed records that match the constraints.
     */
    private List browseCondMonRecords(String apprType, String tank, String condMonType, String equipNum, Calendar measDate, Calendar measTime) {
        info("browseCondMonRecords ${equipNum} - ${tank} - ${apprType} - ${condMonType} - ${measDate.getTime()} - ${measTime.getTime()}")
        List<CondMeasurementServiceRetrieveMntsReplyDTO> cmReplyList = new ArrayList<CondMeasurementServiceRetrieveMntsReplyDTO>()
        try {
            //restart value
            def restart = ""
            CondMeasurementServiceRetrieveMntsReplyCollectionDTO cmReplyDTO = service.get(SERVICE_NAME_CONDMEASUREMENT).retrieveMnts({
                it.setCompCode(apprType)
                it.setCondMonPos(tank)
                it.setCondMonType(condMonType)
                it.setEquipmentNo(equipNum)
                it.setMeasureDate(measDate)
                it.setMeasureTime(measTime)
            }, 100, false, restart)
            restart = cmReplyDTO.getCollectionRestartPoint()
            cmReplyList.addAll(cmReplyDTO.getReplyElements())
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart?.trim()) {
                cmReplyDTO = service.get(SERVICE_NAME_CONDMEASUREMENT).retrieveMnts({
                    it.setCompCode(apprType)
                    it.setCondMonPos(tank)
                    it.setCondMonType(condMonType)
                    it.setEquipmentNo(equipNum)
                    it.setMeasureDate(measDate)
                    it.setMeasureTime(measTime)
                }, 100, false, restart)
                restart = cmReplyDTO.getCollectionRestartPoint()
                cmReplyList.addAll(cmReplyDTO.getReplyElements())
            }
        } catch(Exception e) {
            info("Error at retrieve Cond Meas ${equipNum} ${e.getMessage()}")
        }
        return cmReplyList
    }

    /**
     * Create new Cond Mon record information into report.
     * @param equipNum equipment number from Cond Mon record
     * @param condMonType condition monitoring type from Cond Mon record
     * @param condMonMeas condition monitoring measurement from Cond Mon record
     * @param equipCond measurement value from Cond Mon record
     * @param apprType component code from Cond Mon record
     * @param tank condition monitoring position from Cond Mon record
     * @param measDate sample taken date from Cond Mon record
     * @param measTime time taken date from Cond Mon record
     * @return true if update success, false otherwise
     */
    private boolean createNewCondMon(String equipNum, String conMonType, String condMonMeas, String equipCond, String apprType,
    String tank, Calendar measDate, Calendar measTime) {
        info("createNewCondMon: ${equipNum} - ${tank} - ${apprType} - ${condMonMeas} - ${conMonType} - ${equipCond}")
        boolean success = false
        String measValue = equipCond.trim().length() > 0 ? equipCond : "0"
        try {
            service.get(SERVICE_NAME_CONDMEASUREMENT).create({
                it.setEquipmentNo(equipNum)
                it.setCondMonType(conMonType)
                it.setCondMonMeas(condMonMeas)
                it.setMeasureValue(measValue)
                it.setCompCode(apprType)
                it.setCondMonPos(tank)
                it.setMeasureDate(measDate)
                it.setMeasureTime(measTime)
            })
            success = true
        }catch(Exception e) {
            success = false
            info("Cannot create new record for ${SERVICE_NAME_CONDMEASUREMENT} - ${equipNum}: ${e.getMessage()}")
        }

        return success
    }

    /**
     * Create new Cond Mon record information into report.
     * @param equipNum equipment number from Cond Mon record
     * @param condMonType condition monitoring type from Cond Mon record
     * @param condMonMeas condition monitoring measurement from Cond Mon record
     * @param equipCond measurement value from Cond Mon record
     * @param apprType component code from Cond Mon record
     * @param tank condition monitoring position from Cond Mon record
     * @param measDate sample taken date from Cond Mon record
     * @param measTime time taken date from Cond Mon record
     * @return true if update success, false otherwise
     */
    private boolean updateCondMon(String equipNum, String conMonType, String condMonMeas, String equipCond, String apprType,
    String tank, Calendar measDate, Calendar measTime) {
        info("updateCondMon: ${equipNum} - ${tank} - ${apprType} - ${condMonMeas} - ${conMonType} - ${equipCond}")
        boolean success = false
        String measValue = equipCond.trim().length() > 0 ? equipCond : "0"
        try {
            service.get(SERVICE_NAME_CONDMEASUREMENT).modify({
                it.setEquipmentNo(equipNum)
                it.setCondMonType(conMonType)
                it.setCondMonMeas(condMonMeas)
                it.setMeasureValue(measValue)
                it.setCompCode(apprType)
                it.setCondMonPos(tank)
                it.setMeasureDate(measDate)
                it.setMeasureTime(measTime)
            })
            success = true
        }catch(Exception e) {
            success = false
            info("Cannot update record for ${SERVICE_NAME_CONDMEASUREMENT} - ${equipNum}: ${e.getMessage()}")
        }

        return success
    }

    /**
     * Write rejected Cond Mon record information into report.
     * @param equipNo equipment number from Cond Mon record
     * @param compCode component code from Cond Mon record
     * @param condMonPos condition monitoring position from Cond Mon record
     * @param condMonType condition monitoring type from Cond Mon record
     * @param condMonMeas condition monitoring measurement from Cond Mon record
     * @param sampleDate sample taken date from Cond Mon record
     * @param sampledBy time taken date from Cond Mon record
     * @param message error or warning message should be written
     */
    private void writeRejectedRecord(String equipNo, String compCode, String condMonPos, String condMonType, String condMonMeas,
    String sampleDate, String sampledBy, String message) {
        info("writeRejectedRecord")
        if(!writeErrorHeader) {
            reportWriter.write("Equip No      Comp  Pos     Type Meas      Date / Time          Error/ Warning Message")
            writeErrorHeader = true
        }
        reportWriter.write(writeErrorWarningDetail(equipNo, compCode, condMonPos,
                condMonType, condMonMeas, sampleDate + " " + sampledBy,
                message))
    }

    /**
     * Write warning into the report control.
     * @param equipNo equipment number from Cond Mon record
     * @param compCode component code from Cond Mon record
     * @param pos condition monitoring position from Cond Mon record
     * @param measType condition monitoring type from Cond Mon record
     * @param measName condition monitoring measurement from Cond Mon record
     * @param dateTime sample taken date from Cond Mon record
     * @param errMsg error or warning message should be written
     * @return
     */
    private String writeErrorWarningDetail(String equipNo, String compCode, String pos, String measType, String measName, String dateTime, String errMsg) {
        return String.format("%-12s  %-4s  %-4s    %-2s   %-4s      %-19s  %-67s",
        equipNo, compCode, pos, measType, measName, dateTime, errMsg)
    }

    /**
     * Check whether a specified Cond Mon Meas exist in the CondMeasurementServiceRetrieveMntsReplyDTO list.
     * @param condMonMeas specified Cond Mon Meas (TOAE or TOAF) 
     * @param cmList list of CondMeasurementServiceRetrieveMntsReplyDTO record
     * @return true if a Cond Meas with Meas TOAE exists, false otherwise
     */
    private boolean ifCondMeasExist(String condMonMeas, List<CondMeasurementServiceRetrieveMntsReplyDTO> cmReplyList) {
        info("ifCondMeasExist ${condMonMeas}")
        boolean exist = false
        for(CondMeasurementServiceRetrieveMntsReplyDTO cm : cmReplyList) {
            if(cm.getCondMonMeas().trim().equals(condMonMeas.trim())) {
                exist = true
                break
            }
        }
        return exist
    }

    /**
     * Get standard text key from specified Cond Mon record in the CondMeasurementServiceRetrieveMntsReplyDTO list.
     * @param condMonMeas specified Cond Mon Meas (TOAE or TOAF)
     * @param cmList list of CondMeasurementServiceRetrieveMntsReplyDTO record
     * @return standard text key from Cond Mon with specified Cond Mon Meas, by default it is empty string
     */
    private String getStdTextKeyFromCondMonRecords(String condMonMeas, List<CondMeasurementServiceRetrieveMntsReplyDTO> cmReplyList) {
        info("getStdTextKeyFromCondMonRecords ${condMonMeas}")
        String stdTextKey = ""
        for(CondMeasurementServiceRetrieveMntsReplyDTO cm : cmReplyList) {
            if(cm.getCondMonMeas().trim().equals(condMonMeas.trim())) {
                stdTextKey = cm.getStdTxtKey()
                info("getStdTextKeyFromCondMonRecords; stdTextKey found ${stdTextKey}")
                break
            }
        }
        return stdTextKey
    }

    /**
     * Update standard text content for specified id. <br>
     * To update perform StdText.delete and StdText.setText. 
     * @param stdTextId specified standard text id
     * @param description new description
     * @return true if update success, false otherwise
     */
    private boolean updateStdText(String stdTextId, String description) {
        info("updateStdText ${stdTextId} - ${description}")
        boolean success = false
        try {
            //Delete
            service.get(SERVICE_NAME_STDTEXT).delete({it.setStdTextId(stdTextId)})
            //Set new text
            String[] lines = wrapText(description, 60)
            service.get(SERVICE_NAME_STDTEXT).setText({
                it.setStdTextId(stdTextId)
                it.setTextLine(lines)
                it.setLineCount(lines.length)
                it.setTotalCurrentLines(lines.length)
                it.setTotalRetrievedLines(lines.length)
                it.setStartLineNo(0)
            })
            success = true
        } catch(Exception e) {
            success = false
            info("Cannot update standard text for ${stdTextId}: ${e.getMessage()}")
        }
        return success
    }

    /**
     * This function takes a string value and a line length, and returns an array of lines. <br/>
     * Lines are cut on word boundaries, where the word boundary is a space character. <br/>
     * Spaces are included as the last character of a word, so most lines will actually end with a space. <br/>
     * This isn't too problematic, but will cause a word to wrap if that space pushes it past the max line length.<br/>
     * @see http://progcookbook.blogspot.com/2006/02/text-wrapping-function-for-java.html
     * @param text text to wrap
     * @param len length of a line
     * @return array of String from wrapped text
     */
    public static String[] wrapText(String text, int len) {
        // return empty array for null text
        if (text == null) {
            String[] x = [""]
            return x
        }
        // return text if len is zero or less
        if (len <= 0) {
            String[] x = [text]
            return x
        }
        // return text if less than length
        if (text.length() <= len) {
            String[] x = [text]
            return x
        }
        char[] chars = text.toCharArray()
        Vector<String> lines = new Vector<String>()
        StringBuffer line = new StringBuffer()
        StringBuffer word = new StringBuffer()
        for (int i = 0; i < chars.length; i++) {
            word.append(chars[i])
            if (chars[i] == ' ') {
                if ((line.length() + word.length()) > len) {
                    lines.add(line.toString())
                    line.delete(0, line.length())
                }
                line.append(word)
                word.delete(0, word.length())
            }
        }
        // handle any extra chars in current word
        if (word.length() > 0) {
            if ((line.length() + word.length()) > len) {
                lines.add(line.toString())
                line.delete(0, line.length())
            }
            line.append(word)
        }
        // handle extra line
        if (line.length() > 0) {
            lines.add(line.toString())
        }
        String[] ret = new String[lines.size()]
        int c = 0 // counter
        for (Enumeration<String> e = lines.elements(); e.hasMoreElements(); c++) {
            ret[c] = e.nextElement()
        }
        return ret
    }
}

/**
 * Run the script
 */
ProcessTrb60f process = new ProcessTrb60f()
process.runBatch(binding)