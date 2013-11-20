/*
 @Ventyx 2012
 *
 * This program store Award Code, Award Title, Anniversary 
 * date, and Leave Threshold into MSF012 to be used in TRJ8PS.
 * 
 */
package com.mincom.ellipse.script.custom;

import java.io.BufferedReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf8p1.*;
import com.mincom.ellipse.edoi.ejb.msf8p4.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf012.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf8pd.*;
import com.mincom.ellipse.edoi.ejb.msf880.*;

public class ParamsTrbpay{
    //List of input parameters
    String paramPayGroup, paramPayRunType;
}

public class ProcessTrbpay extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 5;
    private ParamsTrbpay batchParams
    private def writeReport;
    private BufferedReader inputFile;
    private int countProcessedMSF8P4 = 0;
    private int countProcessedMSF8PD = 0;
    private int countCreatedMSF012Record = 0;
    private Boolean bInputFileOpen = false;

    private static final String  INPUT_FILE_NAME          = "MST82X";
    private static final int     MST82X_RUN_NO_IDX        = 0;
    private static final int     MST82X_ENVELOPE_TYPE_IDX = 4;
    private static final int     MST82X_EMP_ID_IDX        = 8;
    private static final int     MST82X_REC_TYPE_IDX      = 9;
    private static final int     MST82X_PER_END_DT_A_IDX  = 13;
    private static final int     MST82X_PAY_GROUP_A_IDX   = 59;
    private static final int  MAX_RECORD_READ = 1000

    private class EmpDataTrbpay{
        private String awardCode    = " ";
        private String threshold    = " ";
    }

    private class Mst82xInputDataTrbpay implements Comparable{
        private String payGroup;
        private String perEndDt;
        private String payRunNo;
        private String employeeId;
        private String envelopeType;
        private String recordType;

        public Mst82xInputDataTrbpay(){
            this.payRunNo     = " ";
            this.envelopeType = " ";
            this.employeeId   = " ";
            this.recordType   = " ";
            this.perEndDt     = " ";
            this.payGroup     = " ";
        }

        public Mst82xInputDataTrbpay(List<String> inputList){
            this.payRunNo     = inputList[MST82X_RUN_NO_IDX];
            this.envelopeType = inputList[MST82X_ENVELOPE_TYPE_IDX];
            this.employeeId   = inputList[MST82X_EMP_ID_IDX];
            this.recordType   = inputList[MST82X_REC_TYPE_IDX];
            this.perEndDt     = inputList[MST82X_PER_END_DT_A_IDX];
            this.payGroup     = inputList[MST82X_PAY_GROUP_A_IDX];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean equals( param0) {
            if(param0 == null) {
                return false;
            }
            if (this.is(param0)) {
                return true;
            }
            Mst82xInputDataTrbpay that = (Mst82xInputDataTrbpay) param0
            return this.payGroup.equals(that.payGroup) && this.perEndDt.equals(that.perEndDt) && this.payRunNo.equals(that.payRunNo) && this.employeeId.equals(that.employeeId) && this.envelopeType.equals(that.envelopeType);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Object that) {
            if(that == null) {
                return false;
            }
            if(!(that instanceof Mst82xInputDataTrbpay)) {
                return false;
            }
            Mst82xInputDataTrbpay o = (Mst82xInputDataTrbpay) that;

            if (!this.payGroup.equals(o.payGroup)){
                return payGroup.compareTo(o.payGroup);
            }
            if (!this.perEndDt.equals(o.perEndDt)){
                return perEndDt.compareTo(o.perEndDt);
            }
            if (!this.payRunNo.equals(o.payRunNo)){
                return this.payRunNo.compareTo(o.payRunNo);
            }
            if (!this.employeeId.equals(o.employeeId)){
                return employeeId.compareTo(o.employeeId);
            }
            if (!this.envelopeType.equals(o.envelopeType)){
                return this.envelopeType.compareTo(o.envelopeType);
            }
            return 0;
        }

        @Override
        public String toString(){
            return this.payGroup+" "+this.perEndDt+" "+this.payRunNo+" "+this.employeeId+" "+this.envelopeType;
        }
    }

    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrbpay())
        //Print Request parameters
        info("Pay Group    : "+batchParams.paramPayGroup)
        info("Pay Run Type : "+batchParams.paramPayRunType)

        try {
            processBatch();
        } finally {
            printBatchReport();
        }
    }

    private void processBatch(){
        info("processBatch");

        initialise()

        //write process
        mainProcess()
    }

    //additional method - start from here.

    /**
     * Initialise all needed global variables
     */
    private void initialise(){
        info("initialise");

        String path = env.getWorkDir().toString()+"/"+INPUT_FILE_NAME;
                String taskUuid = getTaskUUID();
                if(taskUuid?.trim()){
                    path = path + "." + taskUuid;
                }
        inputFile = openInputFile(path);
        bInputFileOpen = true;
    }

    /**
     * Main Process of the program. 
     * It will read the input file and take only the A record type.
     * For each distinct record A type, it will create the MSF012 records.
     */
    private void mainProcess(){

        /*Read input file for A record types.*/
        ArrayList<Mst82xInputDataTrbpay> arrayOfInput = new ArrayList();
        String line;
        while((line=inputFile.readLine())!=null){
            List<String> listString = line.tokenize(",");
            if(listString[MST82X_REC_TYPE_IDX].equals("A")){
                arrayOfInput.add(new Mst82xInputDataTrbpay(listString));
            }
        }

        /*Need to sort so we can check for duplicate entries in the process*/
        Collections.sort(arrayOfInput);

        Mst82xInputDataTrbpay prevData = new Mst82xInputDataTrbpay()
        for(Mst82xInputDataTrbpay currData in arrayOfInput){
            /*
             * When the current data is the same is previous data, we have already processed it and need to skip it
             * Otherwise, process the record.
             */
            if(!currData.equals(prevData)){
                processAllEmployeeInHeader(getHeader(currData), currData.envelopeType, currData.employeeId);
                prevData = currData;
            }
        }

    }

    /**
     * Get the header to be used when browsing the employee payslip data.
     * @param inputData
     * @return the payslip header id.
     */
    private String getHeader(Mst82xInputDataTrbpay inputData){
        info("getHeader");

        Constraint cPayGroup = MSF8P1Rec.payGroup.equalTo(inputData.payGroup);
        Constraint cPerEndDt = MSF8P1Rec.perEndDt.equalTo(inputData.perEndDt);
        Constraint cRunType  = MSF8P1Rec.payRunType.equalTo(batchParams.paramPayRunType);
        Constraint cRunNo    = MSF8P1Rec.payRunNo.equalTo(inputData.payRunNo);
        def query = new QueryImpl(MSF8P1Rec.class).and(cPayGroup).and(cPerEndDt).and(cRunType).and(cRunNo).orderBy(MSF8P1Rec.msf8p1Key);


        // always select the latest date and time.
        Integer lastModDate = 0
        Integer lastModTime = 0
        String payAdvHdrUuid = ""
        edoi.search(query,1000,{MSF8P1Rec msf8p1rec ->

            if (lastModDate < Integer.parseInt(msf8p1rec.getLastModDate())){
                lastModDate = Integer.parseInt(msf8p1rec.getLastModDate())
                lastModTime = Integer.parseInt(msf8p1rec.getLastModTime())
                payAdvHdrUuid = msf8p1rec.getPrimaryKey().getPayAdvHdrUuid()
    
            }else{
                if (lastModDate == Integer.parseInt(msf8p1rec.getLastModDate()) && lastModTime < Integer.parseInt(msf8p1rec.getLastModTime())){
                    lastModDate = Integer.parseInt(msf8p1rec.getLastModDate())
                    lastModTime = Integer.parseInt(msf8p1rec.getLastModTime())
                    payAdvHdrUuid = msf8p1rec.getPrimaryKey().getPayAdvHdrUuid()
                }
            }
            
        })
        debug("lastModDate:" + lastModDate)
        debug("lastModTime:" + lastModTime)
        info("payAdvHdrUuid:"+payAdvHdrUuid)
        return payAdvHdrUuid
    }


    /**
     * This method process all employee payslip record which fulfill the constraint given as parameters.
     * It will create all MSF012 record for each employee payroll record found.
     * @param msf8p1PayAdvHdrUuid
     * @param envelopeType
     * @param employeeId
     */
    private void processAllEmployeeInHeader(String msf8p1PayAdvHdrUuid, String envelopeType, String employeeId){
        info("getAllEmployeeInHeader")

        def query = new QueryImpl(MSF8P4Rec.class).and(MSF8P4Key.payAdvEmpUuid.greaterThanEqualTo(" ")).and(MSF8P4Rec.payAdvHdrUuid.equalTo(msf8p1PayAdvHdrUuid)).and(MSF8P4Rec.employeeId.equalTo(employeeId)).orderBy(MSF8P4Rec.msf8p4Key);

        if(envelopeType?.trim()){
            query = query.and(MSF8P4Rec.envelopeType.equalTo(envelopeType));
        }

        edoi.search(query, MAX_RECORD_READ, { MSF8P4Rec msf8p4rec ->
            EmpDataTrbpay empData = getEmpData(msf8p4rec.getEmployeeId());
            insertAwardCodeTitle(msf8p4rec.getPrimaryKey().getPayAdvEmpUuid(), empData.awardCode);
            processEmployeeLeave(msf8p4rec.getPrimaryKey().getPayAdvEmpUuid(), msf8p4rec.getEmployeeId(), empData.threshold);
        })
    }

    /**
     * This method process leave for all employee payslip record which fulfill the constraint given as parameters.
     * It will add the anniversary date and leave threshold to MSF8PD as FIELD1_A and FIELD2_A respectively.
     * @param msf8p4PayAdvEmpUUID
     * @param msf8p4EmpId
     * @param threshold
     */
    private void processEmployeeLeave(String msf8p4PayAdvEmpUUID, String msf8p4EmpId, String threshold){
        info("browsePayAdvLveUUID")

        /* Using EDOI because there is no service for MSF8P8*/
        def query = new QueryImpl(MSF8PDRec.class).and(MSF8PDRec.payAdvEmpUuid.equalTo(msf8p4PayAdvEmpUUID)).orderBy(MSF8PDRec.msf8pdKey);
        edoi.search(query,MAX_RECORD_READ,{MSF8PDRec msf8pdrec ->
            insertAnnivDateThreshold(msf8pdrec.getPrimaryKey().getPayAdvLveUuid(), getAnvDate(msf8p4EmpId, msf8pdrec.getLeaveType()), threshold);
            countProcessedMSF8PD++;
        })

    }

    /**
     * This function returns the employee data from MSF820 for employee empId
     * It will return empty EmpData if MSF820 is not found.
     * @param empId
     * @return employee data from MSF820 for employee empId
     */
    private EmpDataTrbpay getEmpData(String empId){
        info("getRptAwardCode EmpId:" + empId);

        /* Using EDOI because there is no service for MSF8P8*/
        EmpDataTrbpay empData = new EmpDataTrbpay();
        try{
            MSF820Rec msf820rec = edoi.findByPrimaryKey(new MSF820Key(empId));
            empData.awardCode = msf820rec.getRptAwardCode();
            empData.threshold = getEmpLeaveThreshold(msf820rec.getShiftCat());
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
        }
        return empData;
    }


    /**
     * This function returns the leave threshold for shift category shiftCat
     * It will return 0 if the leave threshold for shift category does not exist.
     * @param shiftCat
     * @return leave threshold for shift category shiftCat
     */
    private String getEmpLeaveThreshold(String shiftCat){
        info("getEmpLeaveThreshold:" + shiftCat );

        try{
            MSF010Rec msf010Rec = edoi.findByPrimaryKey(new MSF010Key("SCAT", shiftCat));
            return msf010Rec.getAssocRec().padRight(14).substring(11,14);
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info ("SCAT not found, please check the setting accordingly")
            return "0";
        }

    }

    /**
     * This function returns the award title of award code msf820RptAwardCode.
     * It will return empty string if the title does not exist. 
     * @param msf820RptAwardCode
     * @return award title of award code msf820RptAwardCode.
     */
    private String getAwardTitle(String msf820RptAwardCode){
        info("browseTnameC0")
        String tNameC0="";

        def query = new QueryImpl(MSF801_C0_801Rec.class).and(MSF801_C0_801Key.cntlKeyRest.equalTo(msf820RptAwardCode))
        MSF801_C0_801Rec msf801_c0_801rec = edoi.firstRow(query);
        if(msf801_c0_801rec){
            tNameC0 = msf801_c0_801rec.getTnameC0();
        }

        return tNameC0;
    }


    /**
     * The method will return the anniversary date of leave type msf8pdLeaveType for the employee msf8p4EmpId.
     * It not found, it will return empty string.
     * @param msf8p4EmpId
     * @param msf8pdLeaveType
     * @return the anniversary date of leave type msf8pdLeaveType for the employee msf8p4EmpId
     */
    private String getAnvDate(String msf8p4EmpId, String msf8pdLeaveType){
        info("getAnvDate");

        String anvDate = "";
        try{
            MSF880Rec msf880rec = edoi.findByPrimaryKey(new MSF880Key(msf8p4EmpId,msf8pdLeaveType))
            anvDate = msf880rec.getAnvDate()
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){}
        return anvDate
    }


    /**
     * Create the MSF012 record for award code and title
     * @param msf8p4PayAdvEmpUUID
     * @param msf820RptAwardCode
     */
    private void insertAwardCodeTitle(String msf8p4PayAdvEmpUUID, String msf820RptAwardCode){
        info("insertAwardCode")
        createRecordMSF012("C", "TRBPAY", "MSF8P4", msf8p4PayAdvEmpUUID, "001", "Award Code Title    ", msf820RptAwardCode.padRight(4) + getAwardTitle(msf820RptAwardCode).padRight(30));
        countProcessedMSF8P4++;
    }


    /**
     * Create the MSF012 record for leave anniversary date
     * @param msf8pdPayAdvLveUUID
     * @param msf880AnvDate
     */
    private void insertAnnivDateThreshold(String msf8pdPayAdvLveUUID, String msf880AnvDate, String threshold){
        info("insertAnniversaryDate")
        createRecordMSF012("C", "TRBPAY  ", "MSF8PD", msf8pdPayAdvLveUUID,"001", "Annv Date Threshold ", msf880AnvDate.padRight(8) + threshold.padRight(1))
        countProcessedMSF8PD++
    }

    /**
     * Create MSF012 record with data defined in the parameters.
     * @param dataType
     * @param interfaceId
     * @param msf8pxId
     * @param key8px
     * @param seqNo
     * @param dataAreaName
     * @param dataAreaValue
     */
    private void createRecordMSF012(String dataType, String interfaceId,String msf8pxId, String key8px, String seqNo, String dataAreaName, String dataAreaValue){
        info("createRecordMSF012");

        /*Using EDOi because MSF012 do not have service*/
        try{
            MSF012Key msf012key = new MSF012Key();
            msf012key.setDataType(dataType);
            msf012key.setKeyValue(interfaceId.padRight(8) + "  " + msf8pxId.padRight(6)  + key8px.padRight(32) + seqNo.padRight(3));
            MSF012Rec msf012rec = new MSF012Rec();
            msf012rec.setPrimaryKey(msf012key);
            msf012rec.setDataArea(dataAreaName + dataAreaValue);
            edoi.create(msf012rec);
            countCreatedMSF012Record++;
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIDuplicateKeyException e){
        }
    }


    /**
     * This function returns the logical representation of an input
     * file from the supplied path.
     * @param path
     * @return BufferedWriter to be used as an input file.
     */
    private BufferedReader openInputFile(String path){
        info("openInputFile");
        info ("Opening:"+path)
        def FileInputStream fileInputStream = new FileInputStream(path);
        def DataInputStream dataInputStream = new DataInputStream(fileInputStream);
        return new BufferedReader(new InputStreamReader(dataInputStream));
    }

    /**
     * Print the batch summary record.
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
        writeReport = report.open("TRBPAYA")
        writeReport.write("TRBPAY - Additional Payslip History Table".center(132))
        writeReport.write("Control Report".center(132))
        writeReport.writeLine(132,"-")
        writeReport.write(("Number of MSF8P4 records processed :  "+countProcessedMSF8P4.toString().padLeft(10)).center(132))
        writeReport.write(("Number of MSF8PD records processed :  "+countProcessedMSF8PD.toString().padLeft(10)).center(132))
        writeReport.write(("Number of MSF012 records created   :  "+countCreatedMSF012Record.toString().padLeft(10)).center(132))
        writeReport.close()
    }


}

/*run script*/
ProcessTrbpay process = new ProcessTrbpay();
process.runBatch(binding);
