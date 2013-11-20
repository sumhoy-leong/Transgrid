/**
 * @Ventyx 2012
 * Conversion from trb627.cbl
 *
 * Maintenance Managers Assessment Report.
 */
package com.mincom.ellipse.script.custom;


import java.text.DecimalFormat;

import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf000.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf621.*;
import com.mincom.ellipse.edoi.ejb.msf660.*;
import com.mincom.ellipse.edoi.ejb.msf723.*;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec;
import com.mincom.ellipse.edoi.ejb.msf900.*;
import com.mincom.ellipse.edoi.ejb.msfx97.*;
import com.mincom.ellipse.edoi.ejb.msfx99.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.*;
import com.mincom.eql.impl.*;

/**
 * Request Parameters for Trb627.
 */
public class ParamsTrb627{
    //List of Input Parameters
    private String paramWorkGroup;
    private String paramEmployeeId1;
    private String paramEmployeeId2;
    private String paramEmployeeId3;
    private String paramEmployeeId4;
    private String paramEmployeeId5;
    private String paramEmployeeId6;
    private String paramEmployeeId7;
    private String paramEmployeeId8;
    private String paramEmployeeId9;
    private String paramEmployeeId10;
    private String paramEmployeeId11;
    private String paramEmployeeId12;
    private String paramEmployeeId13;
    private String paramEmployeeId14;
    private String paramEmployeeId15;
    private String paramEmployeeId16;
    private String paramEmployeeId17;
    private String paramEmployeeId18;
    private String paramEmployeeId19;
    private String paramEmployeeId20;
    private String paramPeriodFrom;
    private String paramPeriodTo;
    private String paramProjFrom;
    private String paramProjTo;
}

/**
 * Main Process of Trb627.
 */
public class ProcessTrb627 extends SuperBatch {
    private static final int EMPLOYEE_PARAM_SIZE = 20
    private static final int MAX_ROW_READ = 1000

    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 9;
    private ParamsTrb627 batchParams;

    /**
     * Record used for sorting process in Trb627.
     */
    public class Trs627ALine implements Comparable<Trs627ALine>{
        private String location;
        private String compCode;
        private String empId;
        private String projNo;
        private String period;
        private String tranType;
        private String workOrder;
        private String dstrctCode;
        private String workGroup;
        private String aCode;
        private BigDecimal noOfHrs;
        private BigDecimal tranAmount;

        public Trs627ALine(String location, String compCode, String empId,
        String projNo, String period, String tranType,
        String workOrder, String dstrctCode, String workGroup,
        String aCode, BigDecimal noOfHrs, BigDecimal tranAmount) {
            this.location = location;
            this.compCode = compCode;
            this.empId = empId;
            this.projNo = projNo;
            this.period = period;
            this.tranType = tranType;
            this.workOrder = workOrder;
            this.dstrctCode = dstrctCode;
            this.workGroup = workGroup;
            this.aCode = aCode;
            this.noOfHrs = noOfHrs;
            this.tranAmount = tranAmount;
        }

        public Trs627ALine() {
            this.location = " "
            this.compCode = " "
            this.empId = " "
            this.projNo = " "
            this.period = " "
            this.tranType = " "
            this.workOrder = " "
            this.dstrctCode = " "
            this.workGroup = " "
            this.aCode = " "
            this.noOfHrs = 0
            this.tranAmount = 0
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getCompCode() {
            return compCode;
        }

        public void setCompCode(String compCode) {
            this.compCode = compCode;
        }

        public String getEmpId() {
            return empId;
        }

        public void setEmpId(String empId) {
            this.empId = empId;
        }

        public String getProjNo() {
            return projNo;
        }

        public void setProjNo(String projNo) {
            this.projNo = projNo;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public String getTranType() {
            return tranType;
        }

        public void setTranType(String tranType) {
            this.tranType = tranType;
        }

        public String getWorkOrder() {
            return workOrder;
        }

        public void setWorkOrder(String workOrder) {
            this.workOrder = workOrder;
        }

        public String getDstrctCode() {
            return dstrctCode;
        }

        public void setDstrctCode(String dstrctCode) {
            this.dstrctCode = dstrctCode;
        }

        public String getWorkGroup() {
            return workGroup;
        }

        public void setWorkGroup(String workGroup) {
            this.workGroup = workGroup;
        }

        public String getaCode() {
            return aCode;
        }

        public void setaCode(String aCode) {
            this.aCode = aCode;
        }

        public BigDecimal getNoOfHrs() {
            return noOfHrs;
        }

        public void setNoOfHrs(BigDecimal noOfHrs) {
            this.noOfHrs = noOfHrs;
        }

        public BigDecimal getTranAmount() {
            return tranAmount;
        }

        public void setTranAmount(BigDecimal tranAmount) {
            this.tranAmount = tranAmount;
        }

        int compareTo(Trs627ALine otherReportLine) {
            if (!period.equals(otherReportLine.getPeriod())){
                return period.compareTo(otherReportLine.getPeriod())
            }
            if (!empId.equals(otherReportLine.getEmpId())){
                return empId.compareTo(otherReportLine.getEmpId())
            }
            if (!projNo.equals(otherReportLine.getProjNo())){
                return projNo.compareTo(otherReportLine.getProjNo())
            }
            if (!workOrder.equals(otherReportLine.getWorkOrder())){
                return workOrder.compareTo(otherReportLine.getWorkOrder())
            }
            return 0
        }
    }
    private File oFile
    private BufferedWriter reportB
    private def reportA
    private String [] paramEmployees
    private boolean errorSw
    private boolean firstRec = true
    private ArrayList arrayOfTrs627ALine = new ArrayList()
    private Trs627ALine currLine
    private int rowCount = 0
    private String errBuffer = " "
    private String saveDstrctCode
    private String fullPeriodFrom
    private String fullPeriodTo
    private String sWorkGroup = " "
    private String sCompCode = " "
    private String sWorkOrder = " "
    private String sProjectNo = " "
    private String sRequiredCode = " "
    private BigDecimal sMNRAct
    private BigDecimal sEstLabHrs
    private BigDecimal sMOTAct
    private BigDecimal sMOTPAct
    private BigDecimal sSNRAct
    private BigDecimal sSOTAct
    private BigDecimal sSOTPAct
    private BigDecimal sTNRAct
    private BigDecimal sTOTAct
    private BigDecimal sSUNRAct
    private BigDecimal sALAct
    private BigDecimal sOCAct
    private BigDecimal sWCAct
    private BigDecimal sMNRActHrs
    private BigDecimal sTNRActHrs
    private BigDecimal sSNRhrs
    private BigDecimal sMOTActHrs
    private BigDecimal sTOTActHrs
    private BigDecimal sSOTActHrs

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb627())
        //PrintRequest Parameters
        info("paramWorkGroup    : " + batchParams.paramWorkGroup)
        info("paramEmployeeId1  : " + batchParams.paramEmployeeId1)
        info("paramEmployeeId2  : " + batchParams.paramEmployeeId2)
        info("paramEmployeeId3  : " + batchParams.paramEmployeeId3)
        info("paramEmployeeId4  : " + batchParams.paramEmployeeId4)
        info("paramEmployeeId5  : " + batchParams.paramEmployeeId5)
        info("paramEmployeeId6  : " + batchParams.paramEmployeeId6)
        info("paramEmployeeId7  : " + batchParams.paramEmployeeId7)
        info("paramEmployeeId8  : " + batchParams.paramEmployeeId8)
        info("paramEmployeeId9  : " + batchParams.paramEmployeeId9)
        info("paramEmployeeId10 : " + batchParams.paramEmployeeId10)
        info("paramEmployeeId11 : " + batchParams.paramEmployeeId11)
        info("paramEmployeeId12 : " + batchParams.paramEmployeeId12)
        info("paramEmployeeId13 : " + batchParams.paramEmployeeId13)
        info("paramEmployeeId14 : " + batchParams.paramEmployeeId14)
        info("paramEmployeeId15 : " + batchParams.paramEmployeeId15)
        info("paramEmployeeId16 : " + batchParams.paramEmployeeId16)
        info("paramEmployeeId17 : " + batchParams.paramEmployeeId17)
        info("paramEmployeeId18 : " + batchParams.paramEmployeeId18)
        info("paramEmployeeId19 : " + batchParams.paramEmployeeId19)
        info("paramEmployeeId20 : " + batchParams.paramEmployeeId20)
        info("paramPeriodFrom   : " + batchParams.paramPeriodFrom)
        info("paramPeriodTo     : " + batchParams.paramPeriodTo)
        info("paramProjFrom     : " + batchParams.paramProjFrom)
        info("paramProjTo       : " + batchParams.paramProjTo)
        try {
            processBatch();
        } finally {
            printBatchReport();
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch");
        //write process
        initialise()
        if (errorSw){
            reportA = report.open("TRB627A")
            writeErrorLine()
        }
        else {
            processRecords()
            Collections.sort(arrayOfTrs627ALine)
            generateReport()
        }
    }

    //additional method - start from here.
    /**
     * Initialize the report writer and other variables.
     */
    private void initialise(){
        info ("initialise")
        fullPeriodFrom = "000000"
        fullPeriodTo = "000000"
        paramEmployees = new String[EMPLOYEE_PARAM_SIZE]
        //Shift Awards
        Class iClazz = batchParams.getClass()
        (1..EMPLOYEE_PARAM_SIZE).each {
            String fieldName = "paramEmployeeId${it}"
            java.lang.reflect.Field field = iClazz.getDeclaredField(fieldName)
            field.setAccessible(true)
            def idx = it-1
            paramEmployees[idx] = field.get(batchParams)
            info("paramEmployees ${it} : ${paramEmployees[idx]}")
        }

        // Intialize csv
        String workingDir   = env.workDir
        String taskUUID     = this.getTaskUUID()
        String csvFilePath  = "${workingDir}/TRO627"
        if(taskUUID?.trim()) {
            csvFilePath  = csvFilePath  + "." + taskUUID
        }
        csvFilePath  = csvFilePath  + ".csv"
        oFile = new File(csvFilePath)

        FileWriter fstream = new FileWriter(oFile)
        reportB = new BufferedWriter(fstream)

        validateParameters()
    }

    /**
     * Validate the request parameters.
     */
    private void validateParameters(){
        info ("validateParameters")
        String errToProj     = "ERROR: (To) Project is required if (from) is selected"
        String errFromProj   = "ERROR: (From) Project is required if (To) is selected"
        String errProjFromTo = "ERROR: (From) Project must not be > than (To) project"
        String errPerFromTo  = "ERROR: (From) Period must not be > than (To) period"
        String errPerFromM   = "ERROR: Period From is mandatory"
        String errPerToM     = "ERROR: Period To is mandatory"
        String errEmpWg      = "ERROR: Enter Either Employee Id Or Work Group, Not Both"

        if (batchParams.paramPeriodFrom?.trim().length() == 0){
            errBuffer = errPerFromM
            errorSw = true
        } else if (batchParams.paramPeriodTo?.trim().length() == 0){
            errBuffer = errPerToM
            errorSw = true
        } else{
            if (!errorSw){
                if (batchParams.paramPeriodFrom.trim().substring(2).toInteger() < 50){
                    fullPeriodFrom = '20' + batchParams.paramPeriodFrom.trim().substring(2) + batchParams.paramPeriodFrom.trim().substring(0,2)
                }
                else{
                    fullPeriodFrom = '19' + batchParams.paramPeriodFrom.trim().substring(2) + batchParams.paramPeriodFrom.trim().substring(0,2)
                }

                if (batchParams.paramPeriodTo.trim().substring(2).toInteger() < 50){
                    fullPeriodTo = '20' + batchParams.paramPeriodTo.trim().substring(2) + batchParams.paramPeriodTo.trim().substring(0, 2)
                }
                else {
                    fullPeriodTo = '19' + batchParams.paramPeriodTo.trim().substring(2) + batchParams.paramPeriodTo.trim().substring(0, 2)
                }
            }
        }

        if (!errorSw && fullPeriodFrom > fullPeriodTo){
            errBuffer = errPerFromTo
            errorSw = true
        }

        if (!errorSw){
            if (batchParams.paramProjFrom?.trim().length() > 0 && batchParams.paramProjTo?.trim().length() == 0){
                errBuffer = errToProj
                errorSw = true
            }else if (batchParams.paramProjTo?.trim().length() > 0 && batchParams.paramProjFrom?.trim().length() == 0){
                errBuffer = errFromProj
                errorSw = true
            }else if (batchParams.paramProjTo < batchParams.paramProjFrom){
                errBuffer = errProjFromTo
                errorSw = true
            }
        }

        if (isNotBlankEmp() && batchParams.paramWorkGroup?.trim().length() > 0){
            errBuffer = errEmpWg
            errorSw = true
        }
    }

    /**
     * Check whether employee's request parameter is not blank
     * @return true if employee's request parameter is not blank
     */
    private boolean isNotBlankEmp(){
        info ("isNotBlankEmp")
        boolean notBlankEmp = false

        int empLength = paramEmployees.length
        int i = 0
        while (i < empLength){
            if (paramEmployees[i]?.trim().length() > 0){
                notBlankEmp = true
                i = empLength
            }else {
                i++
            }
        }
        info ("isNotBlankEmp ${notBlankEmp}")
        return notBlankEmp
    }

    /**
     * Process the records from MSF000.
     */
    private void processRecords(){
        info("processRecords")
        Constraint cDistrict   = MSF000_ADKey.dstrctCode.equalTo(" ")
        Constraint cCtrRecType = MSF000_ADKey.controlRecType.equalTo("AD")
        Constraint cDstrctStat = MSF000_ADRec.dstrctStatus.equalTo("A")
        def query = new QueryImpl(MSF000_ADRec.class).and(cDistrict).and(cCtrRecType).and(cDstrctStat)
        edoi.search(query) { MSF000_ADRec msf000_ADRec->
            saveDstrctCode = msf000_ADRec.getPrimaryKey().getControlRecNo()
            browseWorkOrder()
            browseProject()
        }
    }

    /**
     * Browse the Work Order records.
     */
    private void browseWorkOrder(){
        info ("browseWorkOrder ${saveDstrctCode}")
        Constraint cDstrctCode = MSF620Key.dstrctCode.equalTo(saveDstrctCode)
        def query = new QueryImpl(MSF620Rec.class).
                and(cDstrctCode).
                and(MSF620Key.workOrder.greaterThanEqualTo(" "))
        if (batchParams.paramProjFrom?.trim().length() == 0){
            query = query.and(MSF620Rec.projectNo.greaterThanEqualTo(" ")).orderBy(MSF620Rec.msf620Key)
        } else {
            Constraint cProjectNo1 = MSF620Rec.projectNo.greaterThanEqualTo(batchParams.paramProjFrom)
            Constraint cProjectNo2 = MSF620Rec.projectNo.lessThanEqualTo(batchParams.paramProjTo)
            query = query.and(cProjectNo1).and(cProjectNo2).orderBy(MSF620Rec.msf620Key)
        }
        edoi.search(query, MAX_ROW_READ, {MSF620Rec msf620Rec->
            sWorkOrder = msf620Rec.getPrimaryKey().getWorkOrder()
            sProjectNo = msf620Rec.getProjectNo()
            sWorkGroup = msf620Rec.getWorkGroup()
            sCompCode = msf620Rec.getCompCode()
            String equipNo = msf620Rec.getEquipNo()
            // Read the work order cross reference File
            selectXrefWorkOrder(equipNo)
        })
    }

    /**
     * Browse the Project records.
     */
    private void browseProject(){
        int i = 0
        info ("browseProject ${saveDstrctCode}")
        if (batchParams.paramProjFrom?.trim().length() == 0){
            Constraint cDstrctCode = MSF660Key.dstrctCode.equalTo(saveDstrctCode)
            Constraint cProjectNo = MSF660Key.projectNo.greaterThanEqualTo(" ")
            def query = new QueryImpl(MSF660Rec.class).and(cDstrctCode).and(cProjectNo).orderBy(MSF660Rec.msf660Key)
            edoi.search(query, MAX_ROW_READ, {MSF660Rec msf660Rec->
                sProjectNo = msf660Rec.getPrimaryKey().getProjectNo()
                selectXrefProject()
            })
        }else{
            Constraint cDstrctCode = MSF660Key.dstrctCode.equalTo(saveDstrctCode)
            Constraint cProjectNo1 = MSF660Key.projectNo.greaterThanEqualTo(batchParams.paramProjFrom)
            Constraint cProjectNo2 = MSF660Key.projectNo.lessThanEqualTo(batchParams.paramProjTo)
            def query = new QueryImpl(MSF660Rec.class).and(cDstrctCode).and(cProjectNo1).and(cProjectNo2).orderBy(MSF660Rec.msf660Key)
            edoi.search(query, MAX_ROW_READ, {MSF660Rec msf660Rec->
                sProjectNo = msf660Rec.getPrimaryKey().getProjectNo()
                selectXrefProject()
            })
        }
    }

    /**
     * Search for Xref WO/Account Code/GL Trans based on the equipment number.
     * @param equipNo equipment number
     */
    private void selectXrefWorkOrder(String equipNo){
        info ("selectXrefWorkOrder ${equipNo}")
        String accCode;

        Constraint cDstrctCode  = MSFX99Key.dstrctCode.equalTo(saveDstrctCode)
        Constraint cWorkOrder   = MSFX99Key.workOrder.equalTo(sWorkOrder)
        Constraint cRecType     = MSFX99Key.rec900Type.equalTo("L")
        Constraint cFullPeriod1 = MSFX99Key.fullPeriod.greaterThanEqualTo(fullPeriodFrom)
        Constraint cFullPeriod2 = MSFX99Key.fullPeriod.lessThanEqualTo(fullPeriodTo)
        def query = new QueryImpl(MSFX99Rec.class).and(cDstrctCode).and(cWorkOrder).and(cRecType).and(cFullPeriod1).and(cFullPeriod2).orderBy(MSFX99Rec.msfx99Key)
        int i = 0
        edoi.search(query, MAX_ROW_READ, {MSFX99Rec msfx99Rec ->
            MSF900Key key900 = new MSF900Key()
            key900.dstrctCode = msfx99Rec.getPrimaryKey().getDstrctCode()
            key900.processDate = msfx99Rec.getPrimaryKey().getProcessDate()
            key900.transactionNo = msfx99Rec.getPrimaryKey().getTransactionNo()
            key900.userno = msfx99Rec.getPrimaryKey().getUserno()
            key900.rec900Type = msfx99Rec.getPrimaryKey().getRec900Type()
            MSF900Rec msf900Rec = getTransactionMaster(key900)
            if (msf900Rec != null){
                // set data to sort list
                currLine = new Trs627ALine()
                //sample account code from TG: 285100515218
                accCode = msf900Rec.getAccountCode().padRight(12)
                currLine.setaCode(accCode.substring(9,12))
                if (isRequiredCode()){
                    MSF600Rec msf600Rec = getEquipment(equipNo)
                    currLine.setPeriod(msf900Rec.getFullPeriod().substring(2))
                    currLine.setEmpId(msf900Rec.getEmployeeId())
                    currLine.setDstrctCode(saveDstrctCode)
                    currLine.setWorkOrder(sWorkOrder)
                    currLine.setWorkGroup(sWorkGroup)
                    currLine.setProjNo(sProjectNo)
                    currLine.setTranType(msf900Rec.getTranType())
		            currLine.setCompCode(sCompCode)
                    if (msf600Rec != null){
                        currLine.setLocation(msf600Rec.getEquipLocation())
                    }
                    currLine.setNoOfHrs(msf900Rec.getNoOfHours())
                    currLine.setTranAmount(msf900Rec.getTranAmount())

                    if (!isNotBlankEmp() && batchParams.paramWorkGroup?.trim().length() == 0){
                        addToArrayList()
                    }else{
                        if (checkEmployeeWorkGroup(msf900Rec.getEmployeeId())){
                            addToArrayList()
                        }
                    }
                }
            }
        })
    }

    /**
     * Search for Xref Xref WO/Account Code/GL Trans.
     * @param equipNo equipment number
     */
    private void selectXrefProject(){
        info("selectXrefProject ${sProjectNo}")
        String accCode;
        String[] lTranType = ["LAB", "EAN"]

        Constraint cDstrctCode  = MSFX97Key.dstrctCode.equalTo(saveDstrctCode)
        Constraint cProjectNo   = MSFX97Key.projectNo.equalTo(sProjectNo)
        Constraint cRecType     = MSFX97Key.rec900Type.equalTo("L")
        Constraint cFullPeriod1 = MSFX97Key.fullPeriod.greaterThanEqualTo(fullPeriodFrom)
        Constraint cFullPeriod2 = MSFX97Key.fullPeriod.lessThanEqualTo(fullPeriodTo)
        def query = new QueryImpl(MSFX97Rec.class).and(cDstrctCode).and(cProjectNo).and(cRecType).and(cFullPeriod1).and(cFullPeriod2).orderBy(MSFX97Rec.msfx97Key)
        edoi.search(query, MAX_ROW_READ, {MSFX97Rec msfx97Rec ->
            MSF900Key key900 = new MSF900Key()
            key900.dstrctCode = msfx97Rec.getPrimaryKey().getDstrctCode()
            key900.processDate = msfx97Rec.getPrimaryKey().getProcessDate()
            key900.transactionNo = msfx97Rec.getPrimaryKey().getTransactionNo()
            key900.userno = msfx97Rec.getPrimaryKey().getUserno()
            key900.rec900Type = msfx97Rec.getPrimaryKey().getRec900Type()
            MSF900Rec msf900Rec = getTransactionMaster(key900)
            if (msf900Rec != null){
                if (msf900Rec.tranType in lTranType){
                    //set data to sort list
                    currLine = new Trs627ALine()
                    //sample account code from TG: 285100515218
                    accCode = msf900Rec.getAccountCode().padRight(12)
                    currLine.setaCode(accCode.substring(9,12))
                    currLine.setDstrctCode(saveDstrctCode)
                    currLine.setProjNo(sProjectNo)
                    currLine.setPeriod(msf900Rec.getFullPeriod().substring(2))
                    currLine.setTranType(msf900Rec.getTranType())
                    currLine.setWorkOrder(msf900Rec.getWorkOrder())
                    currLine.setWorkGroup(" ")
                    currLine.setEmpId(msf900Rec.getEmployeeId())
                    currLine.setNoOfHrs(msf900Rec.getNoOfHours())
                    currLine.setTranAmount(msf900Rec.getTranAmount())
                    if (isRequiredCode()){
                        if (!isNotBlankEmp() && batchParams.paramWorkGroup?.trim().length() == 0){
                            addToArrayList()
                        }
                        else{
                            if (checkEmployeeWorkGroup(msf900Rec.getEmployeeId())){
                                addToArrayList()
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * Search for Transaction Master based on the primary key.
     * @param key900 primary key for Transaction Master.
     * @return Transaction Master record
     */
    private MSF900Rec getTransactionMaster(MSF900Key key900){
        info ("getTransactionMaster")
        try{
            MSF900Rec msf900Rec = edoi.findByPrimaryKey(key900)
            return msf900Rec
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            return null
        }
    }

    /**
     * Search for Equipment based on the equipment number.
     * @param equipNo equipment number.
     * @return Equipment record
     */
    private MSF600Rec getEquipment(String equipNo){
        info("getEquipment ${equipNo}")
        try{
            MSF600Key pkMSF600 = new MSF600Key()
            pkMSF600.equipNo = equipNo
            MSF600Rec msf600Rec = edoi.findByPrimaryKey(pkMSF600)
            return msf600Rec
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            return null
        }
    }

    /**
     * Check the Employee's Work Group.
     * @param empId employee id
     * @return true if employee id matches the request parameter.
     */
    private boolean checkEmployeeWorkGroup(String empId){
        info ("checkEmployeeWorkGroup ${empId}")
        boolean flagWGEmp = false
        if (batchParams.paramWorkGroup?.trim().length() > 0){
            Constraint cWorkGroup = MSF723Key.workGroup.equalTo(batchParams.paramWorkGroup)
            Constraint cRecType   = MSF723Key.rec_723Type.equalTo("W")
            Constraint cEquipNo   = MSF723Key.equipNo.equalTo(" ")
            Constraint cEmpId     = MSF723Key.employeeId.equalTo(empId)
            Constraint cStopDt    = MSF723Rec.stopDtRevsd.equalTo("00000000")
            def query = new QueryImpl(MSF723Rec.class).and(cWorkGroup).and(cRecType).and(cEquipNo).and(cEmpId).and(cStopDt).orderBy(MSF723Rec.msf723Key)
            MSF723Rec msf723rec = edoi.firstRow(query)
            if(msf723rec){
                flagWGEmp = true
            }
        }
        else{
            int i = 0
            int empLength = paramEmployees.length
            while (i < empLength){
                if (paramEmployees[i].trim().equals(empId)){
                    flagWGEmp = true
                    i = empLength
                }else{
                    i++
                }
            }
        }

        return flagWGEmp;
    }

    /**
     * Check wheter current code from Trs627ALine is valid.
     * @return true if valid.
     */
    private boolean isRequiredCode(){
        info("isRequiredCode")
        List<String> reqCode
        reqCode = Arrays.asList("218","219","220","223","225","237","234","235","233","231","245","246","270","273","274","305","317","326","222","241","306","308")
        return reqCode.contains(currLine.getaCode())
    }

    /**
     * Add Trs627ALine into the array list.
     */
    private void addToArrayList(){
        info ("addToArrayList")
        arrayOfTrs627ALine.add(new Trs627ALine(
                currLine.getLocation(),
                currLine.getCompCode(),
                currLine.getEmpId(),
                currLine.getProjNo(),
                currLine.getPeriod(),
                currLine.getTranType(),
                currLine.getWorkOrder(),
                currLine.getDstrctCode(),
                currLine.getWorkGroup(),
                currLine.getaCode(),
                currLine.getNoOfHrs(),
                currLine.getTranAmount()
                ))
    }

    /**
     * Generate the report based on the array of Trs627ALine.
     */
    private void generateReport(){
        info("generateReport")
        if (!arrayOfTrs627ALine.isEmpty()){
            extractArrayReportLine()
        }
    }

    /**
     * Extract Trs627ALine line into CSV.
     */
    private void extractArrayReportLine(){
        info("extractArrayReportLine")
        Trs627ALine saveRecLine

        initValue()
        int i = 0
        for (Trs627ALine reportLine: arrayOfTrs627ALine){
            if (firstRec){
                firstRec = false
                reportB.write(
                        "period,"+
                        "employee-id,"+
                        "employee-name,"+
                        "wrkgrp,"+
                        "wrkord,"+
                        "comp-cde,"+
                        "location,"+
                        "std-job-no,"+
                        "workdesc,"+
                        "project-no,"+
                        "est-lab-hrs," +
                        "actlab-hrs,"+
                        " acttrv-hrs,"+
                        " actswi-hrs," +
                        "otlab-hrs,"+
                        " ottrv-hrs,"+
                        " otswi-hrs," +
                        "main/nr-act-cost,"+
                        "main/ot-act-cost,"+
                        "main/ot-pen-cost,"+
                        "swi/nr-act-cost," +
                        "swi/ot-act-cost,"+
                        "swi/ot-pen-cost,"+
                        "trv/nr-act-cost," +
                        "trv/ot-act-cost,"+
                        "sustenance-cost," +
                        "allownce-cost,"+
                        "on-cost-cost,"+
                        "district,"+
                        "acc-code"
                        )
                reportB.write("\n")
                saveRecLine = reportLine
                initValue()
                accumulateVal(reportLine)
                continue
            }
            if (!reportLine.period.equals(saveRecLine.period)){
                writeRecord(saveRecLine)
                saveRecLine = reportLine
                initValue()
                accumulateVal(reportLine)
                continue
            }
            if (!reportLine.empId.equals(saveRecLine.empId)){
                writeRecord(saveRecLine)
                saveRecLine = reportLine
                initValue()
                accumulateVal(reportLine)
                continue
            }
            if (!reportLine.projNo.equals(saveRecLine.projNo)){
                writeRecord(saveRecLine)
                saveRecLine = reportLine
                initValue()
                accumulateVal(reportLine)
                continue
            }
            if (!reportLine.workOrder.equals(saveRecLine.workOrder)){
                writeRecord(saveRecLine)
                saveRecLine = reportLine
                initValue()
                accumulateVal(reportLine)
                continue
            }
            accumulateVal(reportLine)
        }
        //at the end, write record one more for summary
        writeRecord(saveRecLine)
    }

    /**
     * Initialize values for report.
     */
    private void initValue(){
        info ("initValue")

        sMNRAct    = 0
        sMNRActHrs = 0
        sMOTAct    = 0
        sMOTPAct   = 0
        sSNRAct    = 0
        sSOTAct    = 0
        sSOTActHrs = 0
        sSOTPAct   = 0
        sTNRAct    = 0
        sTNRActHrs = 0
        sTOTAct    = 0
        sTOTActHrs = 0
        sSUNRAct   = 0
        sALAct     = 0
        sOCAct     = 0
        sWCAct     = 0
        sSNRhrs    = 0
        sMOTActHrs = 0
        sEstLabHrs = 0
    }

    /**
     * Accumulate the value from Trs627ALine.
     * @param reportLine Trs627ALine
     */
    private void accumulateVal(Trs627ALine reportLine){
        info("accumulateVal")
        String aCode = reportLine.aCode.trim()
        info("code to be accumulated: |${aCode}|")
        switch(aCode) {
            //Main / normal / actual
            case "220":
            case "218":
            case "219":
                sMNRAct += reportLine.tranAmount
                sMNRActHrs += reportLine.noOfHrs
                info("sMNRAct ${sMNRAct}")
                info("sMNRActHrs ${sMNRActHrs}")
                break
            //Main / overtime / actual
            case "223":
                sMOTAct += reportLine.tranAmount
                sMOTActHrs += reportLine.noOfHrs
                info("sMOTAct ${sMOTAct}")
                info("sMOTActHrs ${sMOTActHrs}")
                break
            //Main / overtime / penalty
            case "225":
                sMOTPAct += reportLine.tranAmount
                info("sMOTPAct ${sMOTPAct}")
                break
            //Switching / normal / actual
            case "237":
                sSNRAct += reportLine.tranAmount
                sSNRhrs += reportLine.noOfHrs
                info("sSNRAct ${sSNRAct}")
                info("sSNRhrs ${sSNRhrs}")
                break
            //Switching / overtime / actual
            case "234":
                sSOTAct += reportLine.tranAmount
                sSOTActHrs += reportLine.noOfHrs
                info("sSOTAct ${sSOTAct}")
                info("sSOTActHrs ${sSOTActHrs}")
                break
            //Switching / overtime / penalty
            case "235":
                sSOTPAct += reportLine.tranAmount
                info("sSOTPAct ${sSOTPAct}")
                break
            //Travelling / normal / actual
            case "233":
                sTNRAct += reportLine.tranAmount
                sTNRActHrs += reportLine.noOfHrs
                info("sTNRAct ${sTNRAct}")
                info("sTNRActHrs ${sTNRActHrs}")
                break
            //Travelling / overtime / actual
            case "231":
                sTOTAct += reportLine.tranAmount
                sTOTActHrs += reportLine.noOfHrs
                info("sTOTAct ${sTOTAct}")
                info("sTOTActHrs ${sTOTActHrs}")
                break
            //Sustenance
            case "245":
                sSUNRAct += reportLine.tranAmount
                info("sSUNRAct ${sSUNRAct}")
                break
            //Allowances
            case "246":
                sALAct += reportLine.tranAmount
                info("sALAct ${sALAct}")
                break
            //On-cost
            case "222":
                sOCAct += reportLine.tranAmount
                info("sOCAct ${sOCAct}")
                break
            //Workers Comp.
            case "241":
                sWCAct += reportLine.tranAmount
                info("sWCAct ${sWCAct}")
                break
        }
    }

    /**
     * Using EDOI for formatted name as approved by Peter Deacon
     * in regards of performance issue when using screen service.
     * 
     * @param empId employee Id
     * @return employee formatted name.
     */
    private String getEmpFormattedName(String empId){
        info("getEmpFormattedName")
        String empName = " "
        try {
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(empId))
            empName = "${msf810Rec.getSurname().trim()}, ${msf810Rec.getFirstName().trim()}"
            if(msf810Rec.getSecondName()?.trim()) {
                empName = "${empName} ${msf810Rec.getSecondName().trim()}"
            }
            if(msf810Rec.getThirdName()?.trim()) {
                empName = "${empName} ${msf810Rec.getThirdName().trim()}"
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
        }
        return empName;
    }

    /**
     * Write Trs627ALine as CSV row.
     * @param saveRecLine Trs627ALine
     */
    private void writeRecord(Trs627ALine saveRecLine){
        info("writeRecord")
        String lPeriod
        String empName
        String lAccountCode = " "
        DecimalFormat decFormatter = new DecimalFormat("####0.00")

        //Get employee name
        empName = '"' + getEmpFormattedName(saveRecLine.getEmpId()) + '"'

        lPeriod = saveRecLine.getPeriod().substring(2) + "/" + saveRecLine.getPeriod().substring(0,2)

        //Get the work order description
        MSF620Rec msf620Rec = getWoDesc(saveRecLine.dstrctCode, saveRecLine.workOrder)

        String lAccCode = getProjectControlAcctCode(saveRecLine.dstrctCode, saveRecLine.projNo)
        if (lAccCode != null){
            lAccountCode = lAccCode
        }else{
            lAccountCode = msf620Rec ? msf620Rec.getDstrctAcctCode().padRight(24).substring(4) : " "
        }

        info("sMNRAct    = ${sMNRAct}")
        info("sMNRActHrs = ${sMNRActHrs}")
        info("sMOTAct    = ${sMOTAct}")
        info("sMOTPAct   = ${sMOTPAct}")
        info("sSNRAct    = ${sSNRAct}")
        info("sSOTAct    = ${sSOTAct}")
        info("sSOTActHrs = ${sSOTActHrs}")
        info("sSOTPAct   = ${sSOTPAct}")
        info("sTNRAct    = ${sTNRAct}")
        info("sTNRActHrs = ${sTNRActHrs}")
        info("sTOTAct    = ${sTOTAct}")
        info("sTOTActHrs = ${sTOTActHrs}")
        info("sSUNRAct   = ${sSUNRAct}")
        info("sALAct     = ${sALAct}")
        info("sOCAct     = ${sOCAct}")
        info("sWCAct     = ${sWCAct}")
        info("sSNRhrs    = ${sSNRhrs}")
        info("sMOTActHrs = ${sMOTActHrs}")
        info("sEstLabHrs = ${sEstLabHrs}")
        reportB.write(
                " " + lPeriod + "," +
                saveRecLine.getEmpId() + "," +
                empName + "," +
                saveRecLine.getWorkGroup() + "," +
                saveRecLine.getWorkOrder() + "," +
                saveRecLine.getCompCode() + "," +
                saveRecLine.getLocation() + "," +
                msf620Rec.stdJobNo + "," +
                msf620Rec.woDesc + "," +
                //add aphostrophe infront of project no to avoid exponential issue
                "'"+saveRecLine.projNo + "," +
                decFormatter.format(sEstLabHrs) + "," +
                decFormatter.format(sMNRActHrs) + "," +
                decFormatter.format(sTNRActHrs) + "," +
                decFormatter.format(sSNRhrs) + "," +
                decFormatter.format(sMOTActHrs) + "," +
                decFormatter.format(sTOTActHrs) + "," +
                decFormatter.format(sSOTActHrs) + "," +
                decFormatter.format(sMNRAct) + "," +
                decFormatter.format(sMOTAct) + "," +
                decFormatter.format(sMOTPAct) + "," +
                decFormatter.format(sSNRAct) + "," +
                decFormatter.format(sSOTAct) + "," +
                decFormatter.format(sSOTPAct) + "," +
                decFormatter.format(sTNRAct) + "," +
                decFormatter.format(sTOTAct) + "," +
                decFormatter.format(sSUNRAct) + "," +
                decFormatter.format(sALAct) + "," +
                decFormatter.format(sOCAct) +"," +
                saveRecLine.getDstrctCode() + "," +
                //add aphostrophe infront of account code to avoid leading 0 being stripped
                "'"+lAccountCode
                )
        reportB.write("\n")
        sEstLabHrs = 0
    }

    /**
     * Return Work Order's Description based on the district code and work order.
     * @param dstrctCode district code
     * @param workOrder work order
     * @return Work Order Description
     */
    private MSF620Rec getWoDesc(String dstrctCode, String workOrder){
        info ("getWoDesc")
        try{
            MSF620Rec msf620Rec = edoi.findByPrimaryKey(new MSF620Key(dstrctCode, workOrder))
            try{
                MSF621Rec msf621Rec = edoi.findByPrimaryKey(new MSF621Key(dstrctCode, workOrder))
                sEstLabHrs =  msf621Rec.getEstLabHrs()
            }
            catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
                //skip
            }
            return msf620Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            sEstLabHrs = 0
            MSF620Rec msf620Reca = new MSF620Rec()
            msf620Reca.stdJobNo  = " "
            msf620Reca.woDesc    = " "
            return msf620Reca
        }
    }

    /**
     * Return Project Control's Account Code based on the district code and project number.
     * @param dstrctCode district code 
     * @param projNo project number
     * @return Project Control's Account Code
     */
    private String getProjectControlAcctCode(String dstrctCode, String projNo){
        info("getProjectControlAcctCode")
        try{
            MSF660Rec msf660Rec = edoi.findByPrimaryKey(new MSF660Key(dstrctCode, projNo))
            return msf660Rec.getAccountCode()
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            return null
        }
    }

    /**
     * Write error to the report.
     */
    private void writeErrorLine(){
        info ("writeErrorLine")
        reportA.write("Error :" + errBuffer)
        reportA.close()
    }

    /**
     * Close the report.
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
        reportB.close()
        if (oFile && getTaskUUID()?.trim()) {
            info("Adding CSV into Request.")
            request.request.CURRENT.get().addOutput(oFile,
                    "text/comma-separated-values", "TRO627");
        }

    }
}

/**
 * Run the script
 */
ProcessTrb627 process = new ProcessTrb627();
process.runBatch(binding);