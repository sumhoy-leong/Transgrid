/*@Ventyx 2012
 *
 * This program will process TRT345 input file
 * and create Condition Monitoring for each Equipment listed in TRT345
 * This batch result are TRB345A Oil Analysis Control Report and
 * TRB345B Oil Analysis audit report
 *
 * Revision History 
 *************************
 * Date        Name     Desc								Ver
 * 20/08/2013  LeongSH  SC4344220 TRB345 Oil Upload Report   8 
 *                      Output TRT345Audit consist of 
 *						incorrect Bay value when Equip Fit
 *						is blank.  Bay Value should be 
 *		dkfjsdl;fkjsdf';sdkfsd;f				blank when Equip Fit is blank.   
 *
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import com.mincom.batch.script.*;
import com.mincom.ellipse.lsi.buffer.condmeasurement.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException;
import com.mincom.ellipse.edoi.ejb.msf04d.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf011.*;
import com.mincom.ellipse.edoi.ejb.msf076.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf542.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf602.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf621.*;
import com.mincom.ellipse.edoi.ejb.msf62h.*;
import com.mincom.ellipse.edoi.ejb.msf619.*;
import com.mincom.ellipse.edoi.ejb.msf625.*;
import com.mincom.ellipse.edoi.ejb.msf629.*;
import com.mincom.ellipse.edoi.ejb.msf62w.*;
import com.mincom.ellipse.edoi.ejb.msf645.*;
import com.mincom.ellipse.edoi.ejb.msf656.*;
import com.mincom.ellipse.edoi.ejb.msf660.*;
import com.mincom.ellipse.edoi.ejb.msf6a1.*;
import com.mincom.ellipse.edoi.ejb.msf720.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf920.*;
import com.mincom.ellipse.edoi.ejb.msf930.*;
import com.mincom.ellipse.edoi.ejb.msf93f.*;
import com.mincom.ellipse.edoi.ejb.msf966.*;
import com.mincom.ellipse.edoi.ejb.msfprt.*;
import com.mincom.ellipse.edoi.ejb.msfx55.*;
import com.mincom.ellipse.edoi.ejb.msfx6o.*;
import com.mincom.ellipse.edoi.ejb.msfx6j.*;
import com.mincom.ellipse.edoi.ejb.msf012.*;
import com.mincom.ellipse.edoi.ejb.msf100.*;
import com.mincom.ellipse.edoi.ejb.msf330.*;
import com.mincom.ellipse.edoi.ejb.msf340.*;
import com.mincom.ellipse.edoi.ejb.msf341.*;
import com.mincom.ellipse.edoi.ejb.msf345.*;
import com.mincom.ellipse.edoi.ejb.msf346.*;
import com.mincom.ellipse.edoi.ejb.msf580.*;
import com.mincom.ellipse.edoi.ejb.msf601.*;
import com.mincom.ellipse.edoi.ejb.msf60a.*;
import com.mincom.ellipse.edoi.ejb.msf650.*;
import com.mincom.ellipse.edoi.ejb.msf900.*;
import com.mincom.ellipse.edoi.ejb.msf910.*;
import com.mincom.ellipse.edoi.ejb.msf940.*;
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
import com.mincom.ellipse.edoi.ejb.msk600.*;
import com.mincom.ellipse.edoi.ejb.msk620.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesReplyDTO;
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceModifyReplyDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceModifyRequestDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceModifyRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceRetrieveMntTypesReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceRetrieveMntTypesReplyDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceRetrieveMntTypesRequestDTO;
import com.mincom.enterpriseservice.ellipse.condmeasurement.CondMeasurementServiceRetrieveMntTypesRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyCollectionDTO;

import java.lang.reflect.Field;
import com.mincom.ellipse.client.connection.ConnectionHolder;
import com.mincom.enterpriseservice.ellipse.ConnectionId;
import nacaLib.varEx.Var;
import com.mincom.ellipse.ejp.EllipseSessionDataContainer;
import com.mincom.ellipse.ejp.area.CommAreaWrapper;
/**
 * Class ReturnMultiValuesTrb345
 * To facilitated multiple output for a method 
 */
public class ReturnMultiValuesTrb345 {
    private Object first;
    private Object second;

    public ReturnMultiValuesTrb345(Object first, Object second) {
        this.first = first;
        this.second = second;
    }

    public Object getFirst() {
        return first;
    }

    public Object getSecond() {
        return second;
    }
}

/**
 * Main process of Trb345.
 */
public class ProcessTrb345 extends SuperBatch {
    /**
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 8

    /**
     * Constants
     */
    private static final int MAX_TRT345_COLUMN = 81
    private static final int REQUEST_REPLY_NUM = 20
    private static final String DISTRICT_CODE  = "GRID"
    private static final SimpleDateFormat MEAS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd")
    private static final SimpleDateFormat MEAS_TIME_FORMAT = new SimpleDateFormat("HHmmss")
    private DecimalFormat decFormatter = new DecimalFormat("###,###,###,##0")
    private File inputFile, outputFileCSV, emailOutputFile

    private static String[] TYPE_NEED_ADDITIONAL = ["OA","OQ"]
    private static String TESTDATE = "TESTDATE"
    private static String WO_NUMBER = "WO-NUMBER"
    private static String SAMPLE_NO = "SAMPLE-NO"
    
    def reportA
    def reportB
    List<String> listTRT345
    Boolean emailAddressExist = false
    String outputFilePath
    List<String> emailAddresses = new ArrayList<String>()
    
    private static String createErrorMessage = ""

    int cntrRead = 0, cntrOmitted = 0, cntrWritten = 0, cntrReject = 0

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){
        info("runBatch")

        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        info("No Request Param for this Batch")

        //PrintRequest Parameters
        info("TRB345 Has No Request Parameters ")

        reportA = report.open("TRB345A")
        def workingDir = env.workDir

        String taskUUID = this.getTaskUUID()
        String inputFilePath  = "${workingDir}/TRT345"
        outputFilePath = "${workingDir}/TRT345Audit"
        if(taskUUID?.trim()) {
            inputFilePath  = inputFilePath  + "." + taskUUID
            outputFilePath = outputFilePath + "." + taskUUID
        }

        outputFilePath  = outputFilePath + ".csv"
        inputFile       = new File(inputFilePath)
        outputFileCSV   = new File(outputFilePath)
        reportB         = new BufferedWriter(new FileWriter(outputFileCSV))
        try {
            processBatch()
        } finally {
            reportA.close()
            reportB.close()
            printBatchReport()

            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(outputFileCSV,"text/comma-separated-values", "TRT345Audit");
            }

            if (emailAddressExist) {
                sendEmail()
            }
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch")

        if (inputFile.exists()) {
            boolean reportBHeaderWritten = false
            inputFile.eachLine{ line ->
                String errorMessage = ""
                String woOmittedMessage = ""
                boolean parentEquipmentValid = false
                boolean forceComponentNo = false
                line = line.padRight(MAX_TRT345_COLUMN)
                
                if(line?.trim()) {
                    cntrRead++
                    String counter = cntrRead.toString()
                    def componentNo        = line.substring(0, 10).trim()
                    def monitoringPosition = line.substring(10, 17).trim()
                    def testDate           = line.substring(17, 25).trim()
                    def measurementDate    = line.substring(25, 33).trim()
                    def measurementTime    = line.substring(33, 39).trim()
                    def analysisType       = line.substring(39, 49).trim()
                    def weatherCode        = line.substring(49, 51).trim()
                    def faultCode          = line.substring(51, 53).trim()
                    def testResult         = line.substring(53, 63).trim()
                    def sampleNumber       = line.substring(63, 73).trim()
                    def workOrderNumber    = line.substring(73, 81).trim()
    
                    //validate component Number using Equipment Read Service
                    def equipmentCompCode = "", parentEquipment = "", parentCompCode = ""
                    ReturnMultiValuesTrb345 validateEquipmentNoReturn = readEquipmentNo(componentNo)
                    EquipmentServiceReadReplyDTO equipmentReadReply = validateEquipmentNoReturn.getFirst()
                    String equipmentResultMessage = validateEquipmentNoReturn.getSecond()
                    if (equipmentResultMessage.trim().length() > 0) {
                        if (equipmentResultMessage.contains("mims.e.0444")) {
                            equipmentResultMessage = "REJECTED - PIC Not In WMS"
                        }
                        errorMessage = insertErrorMessage(errorMessage, equipmentResultMessage)
                    } else {
                        if (equipmentReadReply != null){
                            equipmentCompCode = equipmentReadReply.getCompCode()
                            parentEquipment   = equipmentReadReply.getParentEquipment()
                        }
                    }
                    //validate parent Equipment Number if Equipment has Parent Equipment Number
                    if(parentEquipment.trim().length() > 0)
                    {
                        ReturnMultiValuesTrb345 readParentEquipmentReturn = readEquipmentNo(parentEquipment)
                        EquipmentServiceReadReplyDTO parentEquipmentReadReply = readParentEquipmentReturn.getFirst()
                        String parentEquipmentErrorMessage = readParentEquipmentReturn.getSecond()
                        if (parentEquipmentErrorMessage.trim().length() == 0 &&
                        parentEquipmentReadReply != null) {
                            parentEquipmentValid = true
                            parentCompCode       = parentEquipmentReadReply.getCompCode()
                        }
                    }
                    //Validate monitoring position using Table service
                    ReturnMultiValuesTrb345 validateMonitoringPositionResult = readTableValue("PM",monitoringPosition)
                    String monitoringPositionMessage = validateMonitoringPositionResult.getSecond()
                    if (monitoringPositionMessage.trim().length() > 0){
                        if (monitoringPositionMessage.contains("mims.e.0041") || monitoringPositionMessage.contains("mims.e.0011")){
                            monitoringPositionMessage = "REJECTED - Monitoring Position Not In PM Table"
                        }
                        errorMessage = insertErrorMessage(errorMessage, monitoringPositionMessage)
                    }
                    String strTestDate = " "
                    Calendar measDate
                    Calendar measTime
                    try {
                        MEAS_DATE_FORMAT.setLenient(false)
                        MEAS_TIME_FORMAT.setLenient(false)
                        
                        Date date = MEAS_DATE_FORMAT.parse(measurementDate)
                        Date time = MEAS_TIME_FORMAT.parse(measurementTime)
                        
                        
                        Date tstDate = MEAS_DATE_FORMAT.parse(testDate)
                        //Set testDate format to dd.mmyy
                        strTestDate = "${testDate.substring(6,8)}.${testDate.substring(4,6)}${testDate.substring(2,4)}"
                        
                        measDate = Calendar.getInstance()
                        measDate.setTime(date)
                        measTime = Calendar.getInstance()
                        measTime.setTime(time)
                        
                        //The input date is valid, now only need to check whether it's in the future or not
                        Calendar inputDate = Calendar.getInstance()
                        SimpleDateFormat wholeFormat = new SimpleDateFormat("yyyyMMddHHmmss")
                        wholeFormat.setLenient(false)
                        inputDate.setTime(wholeFormat.parse("${measurementDate}${measurementTime}"))
                        Calendar now = Calendar.getInstance()
                        if(inputDate.after(now)) {
                            errorMessage = insertErrorMessage(errorMessage, "REJECTED - Date/time input cannot be in future")
                        }
                    
                    }
                    catch(java.text.ParseException e) {
                        errorMessage = insertErrorMessage(errorMessage, "REJECTED - Date/Time not in correct format")
                    }
    
                    //Validate Analysis Type using Table service
                    ReturnMultiValuesTrb345 validateAnalysisTypeResult = readTableValue("#LW",analysisType)
                    TableServiceReadReplyDTO validateAnalysisTypeReturn = validateAnalysisTypeResult.getFirst()
                    String analysisTypeMessage = validateAnalysisTypeResult.getSecond()
                    String condMonType = "", condMonMeas = ""
                    if (validateAnalysisTypeReturn != null){
                        String analysisTypeAssovVal = validateAnalysisTypeReturn.getAssociatedRecord()
                        condMonType = analysisTypeAssovVal.padRight(9).substring(0, 2)
                        condMonMeas = analysisTypeAssovVal.padRight(9).substring(2, 9)
                    }
                    if (analysisTypeMessage.trim().length() > 0){
                        if (analysisTypeMessage.contains("mims.e.0041") || analysisTypeMessage.contains("mims.e.0011")){
                            analysisTypeMessage = "REJECTED - Anal.Type Not In LW"
                        }
                        errorMessage = insertErrorMessage(errorMessage, analysisTypeMessage)
                    }
                    //Sanitize Measurement Value from testResult variable
                    String tempTestResult = "", measurementValue = ""
                    if (testResult.contains(">") && testResult.contains("<")){
                        errorMessage = insertErrorMessage(errorMessage, "REJECTED - Bad Meas.Val.")
                        measurementValue = testResult
                    } else {
                        tempTestResult  = testResult.replaceAll(">", "")
                        measurementValue = tempTestResult.replaceAll("<", "")
                        if (!measurementValue.isNumber()){
                            errorMessage = insertErrorMessage (errorMessage, "REJECTED - Bad Meas.Val")
                        }
                    }
                    //Validate Weather Code using Table service
                    if(weatherCode?.trim()) {
                        ReturnMultiValuesTrb345 validateWeatherCodeResult = readTableValue("VI",weatherCode)
                        String weatherCodeMessage = validateWeatherCodeResult.getSecond()
                        if (weatherCodeMessage.trim().length() > 0){
                            if (weatherCodeMessage.contains("mims.e.0041") || weatherCodeMessage.contains("mims.e.0041")){
                                weatherCodeMessage = "REJECTED - Bad Weather Code"
                            }
                            errorMessage = insertErrorMessage(errorMessage, weatherCodeMessage)
                        }
                    }
                    //Validate Tester Code using Table service
                    if(faultCode?.trim()) {
                        ReturnMultiValuesTrb345 validateFaultCodeResult = readTableValue("VI",faultCode)
                        String faultCodeMessage = validateFaultCodeResult.getSecond()
                        if (faultCodeMessage.trim().length() > 0){
                            if (faultCodeMessage.contains("mims.e.0041") || faultCodeMessage.contains("mims.e.0011")){
                                faultCodeMessage = "REJECTED - Bad Tester Code"
                            }
                            errorMessage = insertErrorMessage(errorMessage, faultCodeMessage)
                        }
                    }
                    //SampleNo Divide by 10000
                    String strSampleNo = sampleNumber
                    try {
                        BigDecimal sampleNo = sampleNumber.toBigDecimal()
                        sampleNo = sampleNo / 10000
                        strSampleNo = sampleNo.toString()
                    }
                    catch (NumberFormatException e) {
                        errorMessage = insertErrorMessage(errorMessage, "REJECTED - Invalid Sample No")
                    }
                    
                    //validate work order Number
                    if(workOrderNumber.trim().contains(" ")){
                        woOmittedMessage = insertErrorMessage(woOmittedMessage, "WO OMITTED - Embedded Spaces")
                    } else if(!workOrderNumber.trim().isInteger()){
                        woOmittedMessage = insertErrorMessage(woOmittedMessage, "WO OMITTED - Bad Format")
                    } else {
                        ReturnMultiValuesTrb345 validateWorkOrderReturn = readWorkOrder(workOrderNumber,DISTRICT_CODE)
                        String validateWorkOrderMessage = validateWorkOrderReturn.getSecond()
                        if (validateWorkOrderMessage.trim().length() > 0){
                            if (validateWorkOrderMessage.contains("mims.e.0039")){
                                validateWorkOrderMessage = "WO OMITTED - Not Found"
                            }
                            woOmittedMessage = insertErrorMessage(woOmittedMessage, validateWorkOrderMessage)
                        }
                    }
                    //validate CM Measurement
                    List<CondMeasurementServiceRetrieveMntTypesReplyDTO> cmReplyList = retrieveMntTypes(componentNo, equipmentCompCode, monitoringPosition, condMonType)
                    boolean isEmpty = cmReplyList.isEmpty()
                    boolean equipmentNoSent = false, parentEquipmentNoSent = false, validateMntTypesSuccess = false
                    //if the reply list is not empty, process the collected records
                    if(!isEmpty){
                        if(!ifCondMeasExist(condMonMeas, cmReplyList)) {
                            errorMessage = insertErrorMessage(errorMessage, "REJECTED - Bad measurement type in CM Set")
                        } else {
                            equipmentNoSent = true
                            validateMntTypesSuccess = true
                        }
                    }
                    else {
                        if (parentEquipmentValid){
                            List<CondMeasurementServiceRetrieveMntTypesReplyDTO> cmReplyListParent = retrieveMntTypes(parentEquipment, parentCompCode, monitoringPosition, condMonType)
                            boolean isParentEquipReplyEmpty = cmReplyListParent.isEmpty()
                            //if the reply list is not empty, process the collected records
                            if(!isParentEquipReplyEmpty){
                                if(!ifCondMeasExist(condMonMeas, cmReplyListParent)) {
                                    errorMessage = insertErrorMessage(errorMessage, "REJECTED - Bad measurement type in CM Set")
                                } else {
                                    parentEquipmentNoSent = true
                                    validateMntTypesSuccess = true
                                }
                            } else {
                                errorMessage = insertErrorMessage(errorMessage, "REJECTED - CM Set not defined.")
                            }
                        } else {
                            errorMessage = insertErrorMessage(errorMessage, "REJECTED - CM Set not defined.")
                        }
                    }
                    
                    if(errorMessage.trim().length() != 0) {
                        forceComponentNo = true
                    }
                    
                    //create CondMeasurement
                    if (validateMntTypesSuccess &&
                    errorMessage.trim().length() == 0)
                    {
                        boolean newCondMonSuccess = false
                        createErrorMessage = " "
                        if (equipmentNoSent){
                            newCondMonSuccess = createNewCondMon(componentNo, equipmentCompCode, monitoringPosition, condMonType, measDate, measTime, condMonMeas, measurementValue, weatherCode, faultCode)
                            if(condMonType.trim() in TYPE_NEED_ADDITIONAL) {
                                newCondMonSuccess = newCondMonSuccess &&
                                                    createNewCondMon(componentNo, equipmentCompCode, monitoringPosition, condMonType, measDate, measTime, SAMPLE_NO, strSampleNo, weatherCode, faultCode) &&
                                                    createNewCondMon(componentNo, equipmentCompCode, monitoringPosition, condMonType, measDate, measTime, TESTDATE, strTestDate, weatherCode, faultCode)
                                if(woOmittedMessage.trim().length() == 0) {
                                    newCondMonSuccess = newCondMonSuccess &&
                                                        createNewCondMon(componentNo, equipmentCompCode, monitoringPosition, condMonType, measDate, measTime, WO_NUMBER, workOrderNumber, weatherCode, faultCode)
                                }
                            }
                        } else {
                            newCondMonSuccess = createNewCondMon(parentEquipment, parentCompCode, monitoringPosition, condMonType, measDate, measTime, condMonMeas, measurementValue, weatherCode, faultCode)
                            if(condMonType.trim() in TYPE_NEED_ADDITIONAL) {
                                newCondMonSuccess = newCondMonSuccess &&
                                                    createNewCondMon(parentEquipment, parentCompCode, monitoringPosition, condMonType, measDate, measTime, SAMPLE_NO, strSampleNo, weatherCode, faultCode) &&
                                                    createNewCondMon(parentEquipment, parentCompCode, monitoringPosition, condMonType, measDate, measTime, TESTDATE, strTestDate, weatherCode, faultCode)
                                if(woOmittedMessage.trim().length() == 0) {
                                    newCondMonSuccess = newCondMonSuccess &&
                                                        createNewCondMon(parentEquipment, parentCompCode, monitoringPosition, condMonType, measDate, measTime, WO_NUMBER, workOrderNumber, weatherCode, faultCode)
                                }
                            }
                        }
                        if (newCondMonSuccess){
                            cntrWritten++
                        } else {
                            errorMessage = insertErrorMessage(errorMessage, "Load Failed when Create New Condition Monitoring")
                        }
                    }
                    
                    //print TRB345B
                    if(!reportBHeaderWritten){
                        reportB.write("Fit Equip,Pos,Sample DT,Test Date,Time,Measure,Weather,Tester,Value,Sample,WOrder,Ccode,Bay,Message\r\n")
                        reportBHeaderWritten = true
                    }
                    if (errorMessage.trim().length() == 0){
                        errorMessage = "Succesfully Load"
                    } else {
                        cntrReject++
                    }
                    
                    //Add the wo omitted message as the omitted wo doesn't mean the whole process failed.
                    errorMessage = insertErrorMessage(errorMessage, woOmittedMessage)
                    
                    if (equipmentNoSent||forceComponentNo) {
                        reportB.write(componentNo + "," + monitoringPosition + "," + testDate + "," + measurementDate + "," + formatTime(measurementTime) + "," + analysisType  + "," + weatherCode + "," + faultCode + "," + measurementValue + "," + strSampleNo + "," + workOrderNumber + "," + equipmentCompCode + "," + getBay(componentNo) + "," + errorMessage+"\r\n")
                    } else {
                        reportB.write(parentEquipment + "," + monitoringPosition + "," + testDate + "," + measurementDate + "," + formatTime(measurementTime) + "," + analysisType  + "," + weatherCode + "," + faultCode + "," + measurementValue + "," + strSampleNo + "," + workOrderNumber + "," + parentCompCode + "," + getBay(parentEquipment) + "," + errorMessage+"\r\n")
                    }
                }


            }
        }
        //print TRB345BA
        if (cntrRead > 0){
            cntrOmitted = cntrRead - cntrWritten - cntrReject
            reportA.write(String.format(" No of records read:       %-6s", decFormatter.format(cntrRead).padLeft(6)))
            reportA.write(String.format(" No of records omitted:    %-6s", decFormatter.format(cntrOmitted).padLeft(6)))
            reportA.write(String.format(" No of records written:    %-6s", decFormatter.format(cntrWritten).padLeft(6)))
            reportA.write(String.format(" No of records rejected:   %-6s", decFormatter.format(cntrReject).padLeft(6)))
        } else
        {
            reportA.write("GRID - The overnight Oil upload process has run successfully. However, there was no input file for loading and no records were added")
            reportA.write("to Ellipse.")
            reportB.write("GRID - The overnight Oil upload process has run successfully. However, there was no input file for loading and no records were added to Ellipse.")
        }

        getRecipientMail()
    }
    
    
    /**
     * Returns the value for bay column
     * @param equipNo
     * @return String
     */
    private String getBay(String equipNo) {
        debug("getBay")
        
        String returnValue = ""

	/* Ver 8 */        
        if (equipNo.isEmpty() || equipNo == null) { return returnValue; }
        
        MSFX69Rec installPosXRef = readInstallPosXRef(equipNo)
        if(installPosXRef) {
            MSF600Rec installEquip = readEquipment(installPosXRef.primaryKey.installPosn.padRight(12).substring(0,12))
            if(installEquip) {
                returnValue = installEquip.plantNo
            }
        }
        
        return returnValue
    }
    
    /**
     * Returns the installation position x-ref for an equipment no.
     * @param equipNo
     * @return MSFX69Rec
     */
    private MSFX69Rec readInstallPosXRef(String equipNo) {
        debug("readInstallPosXRef")
               
        Constraint cEquipNo = MSFX69Key.equipNo.equalTo(equipNo)
        Constraint cInstallPos = MSFX69Key.installPosn.greaterThanEqualTo(" ")
        QueryImpl query = new QueryImpl(MSFX69Rec.class).and(cEquipNo).and(cInstallPos).orderBy(MSFX69Rec.aix1)
        
        MSFX69Rec returnRec = (MSFX69Rec) edoi.firstRow(query)
        
        return returnRec
    }
    
    /**
     * Returns the equipment record for an equipment no.
     * @param equipNo
     * @return MSF600Rec
     */
    private MSF600Rec readEquipment(String equipNo) {
        debug("readEquipment")
               
        MSF600Rec returnRec = null
        try{
            MSF600Key key = new MSF600Key()
            key.setEquipNo(equipNo)
            returnRec = edoi.findByPrimaryKey(key)
        }
        catch(EDOIObjectNotFoundException e) {
            
        }
        
        return returnRec
    }

    /**
     * Get email address list that will be sent the report.
     */
    private void getRecipientMail() {
        info("getRecipientMail")
        List<TableServiceRetrieveCodesReplyDTO> tblReplyList =  retrieveTableCode("#MAI")
        for(TableServiceRetrieveCodesReplyDTO tbl : tblReplyList) {
            if (tbl.getDescription().trim().contains("GRID345")) {
                emailAddresses.addAll(getEmailAdress(tbl.getTableCode().trim()))
            }
        }

        if(!emailAddresses.isEmpty()) {
            emailAddressExist = true
        } else {
            reportA.write("GRID - The overnight Oil upload process has run successfully. However, there was no email address found in GRID345 mail group.")
        }
    }

    /**
     * Insert Error Message that will be used in report
     * @param existingErrorMessage   Existing Error Message
     * @param newErrorMessage        New Error Message
     * @return String sum of error messages.
     */
    private String insertErrorMessage(String existingErrorMessage, String newErrorMessage) {
        debug("insertErrorMessage")

        if(existingErrorMessage.trim().length() > 0){
            existingErrorMessage += "; $newErrorMessage"
        } else {
            existingErrorMessage = newErrorMessage
        }

        return existingErrorMessage
    }

    /**
     * Read Table value based on given constraints.
     * @param tableType   Table Type
     * @param tableCode   Table Code
     * @return TableServiceReadReplyDTO and ErrorMessage.
     */
    private ReturnMultiValuesTrb345 readTableValue(String tableType, String tableCode) {
        debug("readTableValue")

        TableServiceReadReplyDTO tableReadReply
        def errorMessage = ""
        try {
            tableReadReply = service.get("TABLE").read({ TableServiceReadRequestDTO it ->
                it.setTableType(tableType)
                it.setTableCode(tableCode)
            },false)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error during execute Table Service: ${serviceExc.getMessage()}")
            errorMessage = serviceExc.getMessage()
        }
        return new ReturnMultiValuesTrb345(tableReadReply,errorMessage)
    }

    /**
     * Check whether a specified Cond Mon Meas exist in the CondMeasurementServiceRetrieveMntTypesReplyDTO list.
     * @param condMonMeas specified Cond Mon Meas (TOAE or TOAF) 
     * @param cmList list of CondMeasurementServiceRetrieveMntTypesReplyDTO record
     * @return true if a Cond Meas exists, false otherwise
     */
    private boolean ifCondMeasExist(String condMonMeas, List<CondMeasurementServiceRetrieveMntTypesReplyDTO> cmReplyList) {
        debug("ifCondMeasExist ${condMonMeas}")
        boolean exist = false
        for(CondMeasurementServiceRetrieveMntTypesReplyDTO cm : cmReplyList) {
            if(cm.getCondMonMeas().trim().equals(condMonMeas.trim())) {
                exist = true
                break
            }
        }
        return exist
    }

    /**
     * Read Equipment based on given constraints.
     * @param equipNo   Equipment Number
     * @return EquipmentServiceReadReplyDTO and ErrorMessage.
     */
    private ReturnMultiValuesTrb345 readEquipmentNo(String equipNo) {
        debug("readEquipmentNo: ${equipNo}")

        EquipmentServiceReadReplyDTO equipmentReadReply
        String errorMessage = ""
        EquipmentServiceReadRequiredAttributesDTO eqpReadReq = new EquipmentServiceReadRequiredAttributesDTO()
        eqpReadReq.returnCompCode        = true
        eqpReadReq.returnParentEquipment = true

        try {
            equipmentReadReply = service.get("EQUIPMENT").read({ EquipmentServiceReadRequestDTO it ->
                it.requiredAttributes = eqpReadReq
                it.equipmentNo        = equipNo
            },false)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error during execute Equipment Service: ${serviceExc.getMessage()}")
            errorMessage = serviceExc.getMessage()
        }
        return new ReturnMultiValuesTrb345(equipmentReadReply,errorMessage)
    }

    private List<String> getEmailAdress(String Name) {
        debug("getEmailAdress")
        List<String> emailAdresses = new ArrayList<String>()
        List<String> name = Arrays.asList(Name.split(" ")) // index 0 is lastname, index 1 is firstname

        EmployeeServiceRetrieveReplyDTO employeeReadReply
        String errorMessage = ""

        List<EmployeeServiceRetrieveReplyDTO> cmReplyList = new ArrayList<EmployeeServiceRetrieveReplyDTO>()
        try {
            //restart value
            def restart = ""
            debug(name[1].toString())
            EmployeeServiceRetrieveReplyCollectionDTO cmReplyDTO = service.get("EMPLOYEE").retrieve({ EmployeeServiceRetrieveRequestDTO it ->
                it.nameSearchMethod   = "E"
                it.firstName          = name[1]
            }, REQUEST_REPLY_NUM, false, restart)
            restart = cmReplyDTO.getCollectionRestartPoint()
            cmReplyList.addAll(cmReplyDTO.getReplyElements())
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart.trim().length() > 0) {
                cmReplyDTO = service.get("EMPLOYEE").retrieve({ EmployeeServiceRetrieveRequestDTO it ->
                    it.nameSearchMethod   = "E"
                    it.firstName          = name[1]
                }, REQUEST_REPLY_NUM, false, restart)
                restart = cmReplyDTO.getCollectionRestartPoint()
                cmReplyList.addAll(cmReplyDTO.getReplyElements())
            }
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error at retrieveMntTypes ${serviceExc.getMessage()}")
        }

        if (cmReplyList) {
            for(EmployeeServiceRetrieveReplyDTO emp : cmReplyList) {

                if (name[0].equals(emp.getLastName())
                && emp.getEmailAddress()?.trim().length() > 0
                && "A".equals(emp.getPersEmpStatus())) {
                    emailAdresses.add(emp.getEmailAddress())
                }
            }
        }

        return emailAdresses
    }

    /**
     * Read Work Order based on given constraints.
     * @param woNumber   Work Order Number
     * @param dstrctCode District Code
     * @return WorkOrderServiceReadReplyDTO and ErrorMessage.
     */
    private ReturnMultiValuesTrb345 readWorkOrder(String woNumber, String dstrctCode) {
        debug("readWorkOrder")

        WorkOrderDTO woNo = new WorkOrderDTO(woNumber)
        WorkOrderServiceReadReplyDTO woReadReply
        WorkOrderServiceReadRequiredAttributesDTO woReadReq = new WorkOrderServiceReadRequiredAttributesDTO()
        woReadReq.returnWorkOrder     = true
        woReadReq.returnWorkOrderDesc = true

        String errorMessage = ""
        try {
            woReadReply = service.get("WORKORDER").read({ WorkOrderServiceReadRequestDTO it ->
                it.requiredAttributes = woReadReq
                it.workOrder          = woNo
                it.districtCode       = dstrctCode
                it.includeTasks       = false
            }, false)
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error during execute Work Order Service: ${serviceExc.getMessage()}")
            errorMessage = serviceExc.getMessage()
        }
        return new ReturnMultiValuesTrb345(woReadReply,errorMessage)
    }

    private List retrieveTableCode(String tableType) {
        info("retrieveTableCode")
        List<TableServiceRetrieveCodesReplyDTO> cmReplyList = new ArrayList<TableServiceRetrieveCodesReplyDTO>()

        try {
            //restart value
            def restart = ""
            TableServiceRetrieveCodesReplyCollectionDTO cmReplyDTO = service.get("TABLE").retrieveCodes({ TableServiceRetrieveCodesRequestDTO it ->
                it.tableType = tableType
            }, REQUEST_REPLY_NUM, false, restart)
            restart = cmReplyDTO.getCollectionRestartPoint()
            cmReplyList.addAll(cmReplyDTO.getReplyElements())
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart.trim().length() > 0) {
                cmReplyDTO = service.get("TABLE").retrieveCodes({ TableServiceRetrieveCodesRequestDTO it ->
                    it.tableType = tableType
                }, REQUEST_REPLY_NUM, false, restart)
                restart = cmReplyDTO.getCollectionRestartPoint()
                cmReplyList.addAll(cmReplyDTO.getReplyElements())
            }
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error at retrieveMntTypes ${serviceExc.getMessage()}")
        }
        return cmReplyList
    }

    /**
     * Retrieve Cond Mon Types based on given constraints.
     * @param equipmentNo Equipment Number
     * @param compCode    component code of equipment
     * @param condMonPos  condition monitoring position
     * @param condMonType condition monitoring type
     * @return List of browsed records that match the constraints.
     */
    private List retrieveMntTypes(String equipmentNo, String compCode, String condMonPos, String condMonType) {
        debug("retrieveMntTypes")
        List<CondMeasurementServiceRetrieveMntTypesReplyDTO> cmReplyList = new ArrayList<CondMeasurementServiceRetrieveMntTypesReplyDTO>()

        CondMeasurementServiceRetrieveMntTypesRequiredAttributesDTO condMeasureReq = new CondMeasurementServiceRetrieveMntTypesRequiredAttributesDTO()
        condMeasureReq.returnCondMonMeas = true

        try {
            //restart value
            def restart = ""
            CondMeasurementServiceRetrieveMntTypesReplyCollectionDTO cmReplyDTO = service.get("CONDMEASUREMENT").retrieveMntTypes({ CondMeasurementServiceRetrieveMntTypesRequestDTO it ->
                it.requiredAttributes = condMeasureReq
                it.equipmentNo        = equipmentNo
                it.compCode           = compCode
                it.condMonPos         = condMonPos
                it.condMonType        = condMonType
            }, REQUEST_REPLY_NUM, false, restart)
            restart = cmReplyDTO.getCollectionRestartPoint()
            cmReplyList.addAll(cmReplyDTO.getReplyElements())
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart.trim().length() > 0) {
                cmReplyDTO = service.get("CONDMEASUREMENT").retrieveMntTypes({ CondMeasurementServiceRetrieveMntTypesRequestDTO it ->
                    it.requiredAttributes = condMeasureReq
                    it.equipmentNo        = equipmentNo
                    it.compCode           = compCode
                    it.condMonPos         = condMonPos
                    it.condMonType        = condMonType
                }, REQUEST_REPLY_NUM, false, restart)
                restart = cmReplyDTO.getCollectionRestartPoint()
                cmReplyList.addAll(cmReplyDTO.getReplyElements())
            }
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error at retrieveMntTypes ${serviceExc.getMessage()}")
        }
        return cmReplyList
    }

    /**
     * Create new Cond Mon record information into system.
     * @param equipmentNo equipment number from Cond Mon record
     * @param compCode component code of the equipment number
     * @param condMonPos condition monitoring position
     * @param condMonType condition monitoring type
     * @param measDate sample taken date from Cond Mon record
     * @param measTime time taken date from Cond Mon record
     * @param condMonMeas conditional measure value
     * @param measureValue measurement value
     * @param visInsCode1 weather code
     * @param visInsCode2 fault code
     * @return true if create success, false otherwise
     */    
    private boolean createNewCondMon(String equipmentNo, String compCode,String condMonPos, String condMonType, Calendar measDate, Calendar measTime, String condMonMeas, String measureValue, String visInsCode1, String visInsCode2) {
        debug("createNewCondMon")
        boolean success = false
        try {
            CondMeasurementServiceModifyRequestDTO requestDTO = new CondMeasurementServiceModifyRequestDTO()
            CondMeasurementServiceModifyRequiredAttributesDTO reqAttribute = new CondMeasurementServiceModifyRequiredAttributesDTO()
            reqAttribute.returnCompCode = true
            requestDTO.setEquipmentNo(equipmentNo)
            requestDTO.setCompCode(compCode)
            requestDTO.setCondMonPos(condMonPos)
            requestDTO.setCondMonType(condMonType)
            requestDTO.setMeasureDate(measDate)
            requestDTO.setMeasureTime(measTime)
            requestDTO.setCondMonMeas(condMonMeas)
            requestDTO.setMeasureValue(measureValue)
            requestDTO.setVisInsCode1(visInsCode1)
            requestDTO.setVisInsCode2(visInsCode2)
            requestDTO.setRequiredAttributes(reqAttribute)
            CondMeasurementServiceModifyRequestDTO[] arrayOfDTO = [requestDTO]
            service.get("CONDMEASUREMENT").multipleModify(arrayOfDTO,false)
            success = true
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            success = false
            info("Error at createNewCondMon ${serviceExc.getMessage()}")
        }
        return success
    }

    /**
     *Format time to hh:mm:dd.
     */
    private String formatTime(String time) {
        debug("formatTime")
        time = time.padRight(6)
        String frmtTime = time.substring(0, 2) + ":" + time.substring(2, 4) + ":" + time.substring(4, 6)
        return frmtTime
    }

    private void sendEmail(){
        info ("sendEmail")

        String mailTo, subject, pathName, mailFrom, host

        subject = "<< Ellipse - LIMS Oil Upload >>  "
        ArrayList<String> message = new ArrayList()
        ArrayList<String> attachments = new ArrayList();

        message.add("Please find TRB345 report in attached file")
        mailFrom = " "
        host = " "

        attachments.add(outputFilePath)

        for(emailAddress in emailAddresses){
            SendEmail myEmail = new SendEmail(subject,emailAddress,message,attachments,mailFrom,host,false)
            myEmail.sendMail();
            if(myEmail.isError()){
                info("Error sending email : " + myEmail.getErrorMessage())
            }
        }
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
    }
}

/**
 * Run the script
 */
ProcessTrb345 process = new ProcessTrb345()
process.runBatch(binding)
