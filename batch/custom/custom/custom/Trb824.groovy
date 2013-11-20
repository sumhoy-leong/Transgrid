/*
 @Ventyx 2012
 *  Conversion from Trb824.cbl
 *
 * This program will consolidate employees earnings and deductions. <br>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key;
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec;
import com.mincom.ellipse.edoi.ejb.msf083.MSF083Key;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Rec;
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Rec;
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Key;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec;
import com.mincom.ellipse.edoi.ejb.msf823.MSF823Key;
import com.mincom.ellipse.edoi.ejb.msf823.MSF823Rec;
import com.mincom.ellipse.edoi.ejb.msf837.MSF837Key;
import com.mincom.ellipse.edoi.ejb.msf837.MSF837Rec;
import java.text.DecimalFormat;

public class ParamsTrb824{
    //List of Input Parameters
    String paramStaffCateg;
    String paramReportPeriod;
    String paramDummy1;
    String paramDummy2;
}

public class ProcessTrb824 extends SuperBatch{
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 12;
    private ParamsTrb824 batchParams;
    private static final String PAY_GROUP_TG1 = "TG1"
    private static final String PAY_GROUP_T01 = "T01"
    private static final int MAX_ROW_READ     = 1000

    public class Trs824ALine implements Comparable<Trs824ALine>{
        private String employeeId;
        private String payGroup;
        private String payLocation;
        private String dedEarnTax;
        private String earnType;
        private String earnCode;
        private BigDecimal lastPerUnits;
        private BigDecimal lastPerAmt;
        private BigDecimal curMthUnits;
        private BigDecimal curMthAmt;
        private BigDecimal curFisUnits;
        private BigDecimal curFisAmt;

        public Trs824ALine(String employeeId, String payGroup, String payLocation, String dedEarnTax, String earnType,
        String earnCode, BigDecimal lastPerUnits,
        BigDecimal lastPerAmt, BigDecimal curMthUnits,
        BigDecimal curMthAmt, BigDecimal curFisUnits,
        BigDecimal curFisAmt) {
            this.employeeId = employeeId;
            this.payGroup = payGroup;
            this.payLocation = payLocation;
            this.dedEarnTax = dedEarnTax;
            this.earnType = earnType;
            this.earnCode = earnCode;
            this.lastPerUnits = lastPerUnits;
            this.lastPerAmt = lastPerAmt;
            this.curMthUnits = curMthUnits;
            this.curMthAmt = curMthAmt;
            this.curFisUnits = curFisUnits;
            this.curFisAmt = curFisAmt;
        }

        public String getEmployeeId(){
            return employeeId;
        }

        public void setEmployeeId(String employeeId){
            this.employeeId = employeeId
        }

        public String getEarnCode() {
            return earnCode;
        }

        public void setEarnCode(String earnCode) {
            this.earnCode = earnCode;
        }

        public String getDedEarnTax() {
            return dedEarnTax;
        }

        public void setDedEarnTax(String dedEarnTax) {
            this.dedEarnTax = dedEarnTax;
        }

        public String getEarnType() {
            return earnType;
        }

        public void setEarnType(String earnType) {
            this.earnType = earnType;
        }

        public String getPayGroup() {
            return payGroup;
        }

        public void setPayGroup(String payGroup) {
            this.payGroup = payGroup;
        }

        public String getPayLocation() {
            return payLocation;
        }

        public void setPayLocation(String payLocation) {
            this.payLocation = payLocation;
        }

        public BigDecimal getLastPerUnits() {
            return lastPerUnits;
        }

        public void setLastPerUnits(BigDecimal lastPerUnits) {
            this.lastPerUnits = lastPerUnits;
        }

        public BigDecimal getLastPerAmt() {
            return lastPerAmt;
        }

        public void setLastPerAmt(BigDecimal lastPerAmt) {
            this.lastPerAmt = lastPerAmt;
        }

        public BigDecimal getCurMthUnits() {
            return curMthUnits;
        }

        public void setCurMthUnits(BigDecimal curMthUnits) {
            this.curMthUnits = curMthUnits;
        }

        public BigDecimal getCurMthAmt() {
            return curMthAmt;
        }

        public void setCurMthAmt(BigDecimal curMthAmt) {
            this.curMthAmt = curMthAmt;
        }

        public BigDecimal getCurFisUnits() {
            return curFisUnits;
        }

        public void setCurFisUnits(BigDecimal curFisUnits) {
            this.curFisUnits = curFisUnits;
        }

        public BigDecimal getCurFisAmt() {
            return curFisAmt;
        }

        public void setCurFisAmt(BigDecimal curFisAmt) {
            this.curFisAmt = curFisAmt;
        }

        public String getType(){
            if (this.earnType.equals("1")){
                return ("E")
            } else if (this.earnType.equals("2")||this.earnType.equals("4")){
                return ("D")
            } else if (this.earnType.equals("3")){
                return ("T")
            } else{
                return (" ")
            }
        }

        int compareTo(Trs824ALine otherReportLine){
            if (!dedEarnTax.equals(otherReportLine.getDedEarnTax())){
                return dedEarnTax.compareTo(otherReportLine.getDedEarnTax())
            }
            if (!earnType.equals(otherReportLine.getEarnType())){
                return earnType.compareTo(otherReportLine.getEarnType())
            }
            if (!earnCode.equals(otherReportLine.getEarnCode())){
                return earnCode.compareTo(otherReportLine.getEarnCode())
            }
            return 0
        }
    }

    private class TableRepf{
        private String tr;
        private String trDesc;
        private BigDecimal totpLastPerAmt;
        private BigDecimal totgLastPerAmt;
        private BigDecimal totpCurMthAmt;
        private BigDecimal totgCurMthAmt;
        private BigDecimal totpCurFisAmt;
        private BigDecimal totgCurFisAmt;
        public TableRepf(String tr, String trDesc, BigDecimal totpLastPerAmt,
        BigDecimal totgLastPerAmt, BigDecimal totpCurMthAmt,
        BigDecimal totgCurMthAmt, BigDecimal totpCurFisAmt,
        BigDecimal totgCurFisAmt) {
            this.tr = tr;
            this.trDesc = trDesc;
            this.totpLastPerAmt = totpLastPerAmt;
            this.totgLastPerAmt = totgLastPerAmt;
            this.totpCurMthAmt = totpCurMthAmt;
            this.totgCurMthAmt = totgCurMthAmt;
            this.totpCurFisAmt = totpCurFisAmt;
            this.totgCurFisAmt = totgCurFisAmt;
        }
        public String getTr() {
            return tr;
        }
        public void setTr(String tr) {
            this.tr = tr;
        }
        public String getTrDesc() {
            return trDesc;
        }
        public void setTrDesc(String trDesc) {
            this.trDesc = trDesc;
        }
        public BigDecimal getTotpLastPerAmt() {
            return totpLastPerAmt;
        }
        public void setTotpLastPerAmt(BigDecimal totpLastPerAmt) {
            this.totpLastPerAmt = totpLastPerAmt;
        }
        public BigDecimal getTotgLastPerAmt() {
            return totgLastPerAmt;
        }
        public void setTotgLastPerAmt(BigDecimal totgLastPerAmt) {
            this.totgLastPerAmt = totgLastPerAmt;
        }
        public BigDecimal getTotpCurMthAmt() {
            return totpCurMthAmt;
        }
        public void setTotpCurMthAmt(BigDecimal totpCurMthAmt) {
            this.totpCurMthAmt = totpCurMthAmt;
        }
        public BigDecimal getTotgCurMthAmt() {
            return totgCurMthAmt;
        }
        public void setTotgCurMthAmt(BigDecimal totgCurMthAmt) {
            this.totgCurMthAmt = totgCurMthAmt;
        }
        public BigDecimal getTotpCurFisAmt() {
            return totpCurFisAmt;
        }
        public void setTotpCurFisAmt(BigDecimal totpCurFisAmt) {
            this.totpCurFisAmt = totpCurFisAmt;
        }
        public BigDecimal getTotgCurFisAmt() {
            return totgCurFisAmt;
        }
        public void setTotgCurFisAmt(BigDecimal totgCurFisAmt) {
            this.totgCurFisAmt = totgCurFisAmt;
        }
    }

    private class TableRepfD{
        private String trD;
        private String trDescD;
        private BigDecimal totpLastPerAmtD;
        private BigDecimal totgLastPerAmtD;
        private BigDecimal totpCurMthAmtD;
        private BigDecimal totgCurMthAmtD;
        private BigDecimal totpCurFisAmtD;
        private BigDecimal totgCurFisAmtD;
        public TableRepfD(String trD, String trDescD,
        BigDecimal totpLastPerAmtD, BigDecimal totgLastPerAmtD,
        BigDecimal totpCurMthAmtD, BigDecimal totgCurMthAmtD,
        BigDecimal totpCurFisAmtD, BigDecimal totgCurFisAmtD) {
            this.trD = trD;
            this.trDescD = trDescD;
            this.totpLastPerAmtD = totpLastPerAmtD;
            this.totgLastPerAmtD = totgLastPerAmtD;
            this.totpCurMthAmtD = totpCurMthAmtD;
            this.totgCurMthAmtD = totgCurMthAmtD;
            this.totpCurFisAmtD = totpCurFisAmtD;
            this.totgCurFisAmtD = totgCurFisAmtD;
        }
        public String getTrD() {
            return trD;
        }
        public void setTrD(String trD) {
            this.trD = trD;
        }
        public String getTrDescD() {
            return trDescD;
        }
        public void setTrDescD(String trDescD) {
            this.trDescD = trDescD;
        }
        public BigDecimal getTotpLastPerAmtD() {
            return totpLastPerAmtD;
        }
        public void setTotpLastPerAmtD(BigDecimal totpLastPerAmtD) {
            this.totpLastPerAmtD = totpLastPerAmtD;
        }
        public BigDecimal getTotgLastPerAmtD() {
            return totgLastPerAmtD;
        }
        public void setTotgLastPerAmtD(BigDecimal totgLastPerAmtD) {
            this.totgLastPerAmtD = totgLastPerAmtD;
        }
        public BigDecimal getTotpCurMthAmtD() {
            return totpCurMthAmtD;
        }
        public void setTotpCurMthAmtD(BigDecimal totpCurMthAmtD) {
            this.totpCurMthAmtD = totpCurMthAmtD;
        }
        public BigDecimal getTotgCurMthAmtD() {
            return totgCurMthAmtD;
        }
        public void setTotgCurMthAmtD(BigDecimal totgCurMthAmtD) {
            this.totgCurMthAmtD = totgCurMthAmtD;
        }
        public BigDecimal getTotpCurFisAmtD() {
            return totpCurFisAmtD;
        }
        public void setTotpCurFisAmtD(BigDecimal totpCurFisAmtD) {
            this.totpCurFisAmtD = totpCurFisAmtD;
        }
        public BigDecimal getTotgCurFisAmtD() {
            return totgCurFisAmtD;
        }
        public void setTotgCurFisAmtD(BigDecimal totgCurFisAmtD) {
            this.totgCurFisAmtD = totgCurFisAmtD;
        }
    }

    private def ReportA
    private String workDir = ""
    private String oFilePath = ""
    private File oFile
    private BufferedWriter ReportB

    private String[] earnTypeArray
    private String[] earnTypeArrayD
    private String contentToWrite
    private TableRepf[] tableRepfArray
    private TableRepfD[] tableRepfArrayD
    private ArrayList arrayOfTrs824ALine = new ArrayList()
    private Trs824ALine currLine
    private int msf823Count
    private int msf837Count
    private String h1EmployeeType
    private String h1CurrentPrevious
    private BigDecimal totLastPerUnits = 0
    private BigDecimal totLastPerAmt = 0
    private BigDecimal totCurMthUnits = 0
    private BigDecimal totCurMthAmt = 0
    private BigDecimal totCurFisUnits = 0
    private BigDecimal totCurFisAmt = 0
    private BigDecimal tLastPerAmt = 0
    private BigDecimal tCurMthAmt = 0
    private BigDecimal tCurFisAmt = 0
    private BigDecimal tegLastPerAmt = 0
    private BigDecimal tegCurMthAmt = 0
    private BigDecimal tegCurFisAmt = 0
    private BigDecimal tLastPerAmtD = 0
    private BigDecimal tCurMthAmtD = 0
    private BigDecimal tCurFisAmtD = 0
    private BigDecimal tegLastPerAmtD = 0
    private BigDecimal tegCurMthAmtD = 0
    private BigDecimal tegCurFisAmtD = 0
    private BigDecimal teTotlLastPerAmt = 0
    private BigDecimal teTotlLastPerUnits = 0
    private BigDecimal teTotlCurMthAmt = 0
    private BigDecimal teTotlCurMthUnits = 0
    private BigDecimal teTotlCurFisAmt = 0
    private BigDecimal teTotlCurFisUnits = 0
    private BigDecimal deTotlLastPerAmt = 0
    private BigDecimal deTotlLastPerUnits = 0
    private BigDecimal deTotlCurMthAmt = 0
    private BigDecimal deTotlCurMthUnits = 0
    private BigDecimal deTotlCurFisAmt = 0
    private BigDecimal deTotlCurFisUnits = 0
    private BigDecimal degTotlLastPerAmt = 0
    private BigDecimal degTotlCurMthAmt = 0
    private BigDecimal degTotlCurFisAmt = 0
    private BigDecimal txTotlLastPerAmt = 0
    private BigDecimal txTotlLastPerUnits = 0
    private BigDecimal txTotlCurMthAmt = 0
    private BigDecimal txTotlCurMthUnits = 0
    private BigDecimal txTotlCurFisAmt = 0
    private BigDecimal txTotlCurFisUnits = 0
    private BigDecimal txgTotlLastPerAmt = 0
    private BigDecimal txgTotlLastPerUnits = 0
    private BigDecimal txgTotlCurMthAmt = 0
    private BigDecimal txgTotlCurMthUnits = 0
    private BigDecimal txgTotlCurFisAmt = 0
    private BigDecimal txgTotlCurFisUnits = 0

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb824())

        //PrintRequest Parameters
        info("paramStaffCateg: " + batchParams.paramStaffCateg)
        info("paramReportPeriod: " + batchParams.paramReportPeriod)

        workDir = env.getWorkDir().toString() + "/"
        oFilePath = workDir +"TRT824"
        if(taskUUID?.trim()) {
            oFilePath = oFilePath + "." + taskUUID
        }
        oFilePath = oFilePath + ".csv"
        oFile = new File(oFilePath)
        ReportB = new BufferedWriter(new FileWriter(oFile))

        try {
            processBatch();

        } finally {
            printBatchReport();
        }
    }

    /**
     * Main process
     */
    private void processBatch(){
        info("processBatch");
        //write process
        boolean errorValidate = validateReqParams()
        if (!errorValidate){
            boolean error = initialize()
            if(!error){
                populateEarnType()
                populateEarnTypeD()
                setupRepfTable()
                setupRepfDTable()
                mainProcess()
                Collections.sort(arrayOfTrs824ALine)
                generateReport()
            }
        }
    }

    //additional method - start from here.
    /**
     * initialize variables and arrays
     * 
     */
    private boolean initialize(){
        info("initialize")

        if (!batchParams.paramStaffCateg.trim().equals("")){
            h1EmployeeType = getStaffCateg()

        } else{
            h1EmployeeType = "All Staff Category"
        }

        if (batchParams.paramReportPeriod.trim().equals("P")){
            h1CurrentPrevious = "Previous"

        } else{
            h1CurrentPrevious = "Current"
        }


        //Initialize Array
        earnTypeArray = new String[200]
        earnTypeArrayD = new String[200]

        for (int i=0; i<200; i++){
            earnTypeArray[i] = " "
            earnTypeArrayD[i] = " "
        }

        tableRepfArray = new TableRepf[100]
        tableRepfArrayD = new TableRepf[100]

        for (int i=0; i<100; i++){
            tableRepfArray[i] = new TableRepf(" "," ",0,0,0,0,0,0)
            tableRepfArrayD[i] = new TableRepfD(" "," ",0,0,0,0,0,0)
        }

        return false
    }
    /**
     * Get msf801_pg record
     * @return msf801_pg record
     */
    private MSF801_PG_801Rec getPrvPrdNoPg() {
        info("getPrvPrdNoPg()")
        try{
            MSF801_PG_801Rec msf801_pg_801rec = edoi.findByPrimaryKey(new MSF801_PG_801Key("PG", PAY_GROUP_TG1))
            return msf801_pg_801rec
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info ("MSF801 PG TG1 Rec not found")
            return null
        }
    }

    /**
     * Validate request parameter
     * @return boolean
     */
    private boolean validateReqParams(){
        info("validateReqParams")
        // Validate Staff Category

        if(!batchParams.paramStaffCateg.trim().equals("")){
            try{
                MSF010Rec msf010rec = edoi.findByPrimaryKey(new MSF010Key("STFC", batchParams.paramStaffCateg))
            } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
                info ("Staff Category not found")
                report.open("TRB824A")
                ReportA.write(batchParams.paramStaffCateg + " " + "*** Not Found ***")
                ReportA.close()
                return true
            }
        }

        // Validate Report Period

        if(!batchParams.paramReportPeriod.trim().equals("P") && !batchParams.paramReportPeriod.trim().equals("C")){
                info ("Report Period not C or P")
                report.open("TRB824A")
                ReportA.write(batchParams.paramReportPeriod + " " + "*** Must be C or P ***")
                ReportA.close()
                return true
        }
        return false
    }
    /**
     * Write report header
     * @param headerNo as String
     */
    private ArrayList<String> writeReportAHeader(String headerNo){
        info ("writeReportAHeader")
        ArrayList<String> rptPgHead = new ArrayList<String>()
        rptPgHead.add("Consolidated Earnings and Deductions Summary Report".center(132))
        String tempString = "Staff Category: " + h1EmployeeType
        rptPgHead.add(tempString.center(132))
        tempString = ""
        tempString = "Reporting Period: " + h1CurrentPrevious
        rptPgHead.add(tempString.center(132))
        rptPgHead.add(" ")
        rptPgHead.add("-".multiply(132))

        if (batchParams.paramReportPeriod.trim().equals("P")){
            if (headerNo.equals("1")){
                rptPgHead.add("                                                                                                                  PREVIOUS          ")
                rptPgHead.add("Earn                                                                                                     --- FISCAL YEAR TO DATE ---")
                rptPgHead.add("Code  Description                                                                                            Units            Value ")
            } else {
                rptPgHead.add("                                                                                                                  PREVIOUS          ")
                rptPgHead.add("                                                                                                         --- FISCAL YEAR TO DATE ---")
                rptPgHead.add("Description                                                                                                                   Value ")
            }
        } else {
            if (headerNo.equals("1")){
                rptPgHead.add("                                                    CURRENT                        CURRENT                        CURRENT           ")
                rptPgHead.add("Earn                                       -------- THIS PAY ---------    ------ MONTH TO DATE ------    --- FISCAL YEAR TO DATE ---")
                rptPgHead.add("Code  Description                              Units            Value         Units            Value         Units            Value ")
            } else {
                rptPgHead.add("                                                    CURRENT                        CURRENT                        CURRENT           ")
                rptPgHead.add("                                           -------- THIS PAY ---------    ------ MONTH TO DATE ------    --- FISCAL YEAR TO DATE ---")
                rptPgHead.add("Description                                                     Value                          Value                          Value ")
            }
        }

        return rptPgHead

    }

    /**
     * Write csv header
     */
    private void writeReportBHeader(){
        info("writeReportBHeader")
        if (batchParams.paramReportPeriod.trim().equals("P")){
            ReportB.write("P/L,Code,Description,Type, , , , ,PREV FYTD - Units,PREV FYTD - Value\n")
        } else {
            ReportB.write("P/L,Code,Description,Type,T/Pay - Units,T/Pay - Value,MTD - Units,MTD - Value,FYTD - Units,FYTD - Value\n")
        }
    }

    /**
     * Get staff category
     */
    private String getStaffCateg(){
        info("getStaffCateg")
        String tempString = " "
        try{
            MSF010Rec msf010rec = edoi.findByPrimaryKey(new MSF010Key("STFC", batchParams.paramStaffCateg))
            tempString = batchParams.paramStaffCateg + " " + msf010rec.getTableDesc()
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("MSF010 object not found with key: STFC"+batchParams.paramStaffCateg)
        }
        return tempString
    }

    /**
     * Populate earn type for earnings
     */
    private void populateEarnType(){
        info("populateEarnType")
        Constraint c1 = MSF801_A_801Key.cntlRecType.equalTo("A")
        def query = new QueryImpl(MSF801_A_801Rec.class).and(c1)
        edoi.search(query, {MSF801_A_801Rec msf801_a_801rec ->
            if (!msf801_a_801rec.getMiscRptFldAx1().trim().equals("")){
                int index = 0
                boolean endLoop = false
                while (!endLoop){
                    if (earnTypeArray[index].trim().equals("")){
                        earnTypeArray[index] = msf801_a_801rec.getMiscRptFldAx1()
                        endLoop = true
                    } else if (earnTypeArray[index].equals(msf801_a_801rec.getMiscRptFldAx1())){
                        endLoop = true
                    } else{
                        index++
                        if (index > 199){
                            endLoop = true
                        }
                    }
                }
            }
        })
    }

    /**
     * Populate earn type for deduction
     */
    private void populateEarnTypeD(){
        info("populateEarnTypeD")
        Constraint c1 = MSF801_D_801Key.cntlRecType.equalTo("D")
        def query = new QueryImpl(MSF801_D_801Rec.class).and(c1)
        edoi.search(query, {MSF801_D_801Rec msf801_d_801rec ->
            if (!msf801_d_801rec.getMiscRptFldDx1().trim().equals("")){
                int index = 0
                boolean endLoop = false
                while (!endLoop){
                    if (earnTypeArrayD[index].trim().equals("")){
                        earnTypeArrayD[index] = msf801_d_801rec.getMiscRptFldDx1()
                        endLoop = true
                    } else if (earnTypeArrayD[index].equals(msf801_d_801rec.getMiscRptFldDx1())){
                        endLoop = true
                    } else{
                        index++
                        if (index > 199){
                            endLoop = true
                        }
                    }
                }
            }
        })
    }

    /**
     * Populate earning code classification
     */
    private void setupRepfTable(){
        info("setupRepfTable")
        int counter = 0
        Constraint c1 = MSF010Key.tableType.equalTo("REPF")
        def query = new QueryImpl(MSF010Rec.class).and(c1).orderBy(MSF010Rec.msf010Key)
        edoi.search(query, MAX_ROW_READ, {MSF010Rec msf010rec ->
            int index = 0
            boolean endLoop = false
            while (!endLoop){
                if (earnTypeArray[index].trim().equals("")){
                    endLoop = true
                } else if (earnTypeArray[index].trim().equals(msf010rec.getPrimaryKey().getTableCode().trim())){
                    tableRepfArray[counter].setTr(msf010rec.getPrimaryKey().getTableCode().trim())
                    tableRepfArray[counter].setTrDesc(msf010rec.getTableDesc())
                    counter++
                    index++
                    if (index>199){
                        endLoop = true
                    }
                } else{
                    index++
                    if (index>199){
                        endLoop = true
                    }
                }
            }
        })
    }

    /**
     * Populate deduction code classification
     */
    private void setupRepfDTable(){
        info("setupRepfDTable")
        int counter = 0
        Constraint c1 = MSF010Key.tableType.equalTo("REPF")
        def query = new QueryImpl(MSF010Rec.class).and(c1).orderBy(MSF010Rec.msf010Key)
        edoi.search(query, MAX_ROW_READ, {MSF010Rec msf010rec ->
            int index = 0
            boolean endLoop = false
            while (!endLoop){
                if (earnTypeArrayD[index].trim().equals("")){
                    endLoop = true
                } else if (earnTypeArrayD[index].trim().equals(msf010rec.getPrimaryKey().getTableCode().trim())){
                    tableRepfArrayD[counter].setTrD(msf010rec.getPrimaryKey().getTableCode().trim())
                    tableRepfArrayD[counter].setTrDescD(msf010rec.getTableDesc())
                    counter++
                    index++
                    if (index>199){
                        endLoop = true
                    }
                } else{
                    index++
                    if (index>199){
                        endLoop = true
                    }
                }
            }
        })
    }
    /**
     * Process emp's earn and emp's deduction
     */
    private void mainProcess(){
        info("mainProcess")
        processEmpEarn()
        processEmpDeduct()
    }

    /**
     * Browse for employee earning
     */
    private void processEmpEarn(){
        info("processEmpEarn")
        msf823Count = 0
        Constraint c1 = MSF823Key.earnCode.notEqualTo("000")
        def query = new QueryImpl(MSF823Rec.class).and(c1).orderBy(MSF823Rec.msf823Key)
        edoi.search(query, MAX_ROW_READ, {MSF823Rec msf823rec ->
            boolean processedSw = validateStaffCateg(msf823rec.getPrimaryKey().getEmployeeId())
            if (processedSw){
                boolean validateSw = validateEarnCode(msf823rec.getPrimaryKey().getEarnCode())
                if (validateSw){
                        getPayGroup(msf823rec.getPrimaryKey().getEmployeeId())
                        currLine.setEmployeeId(msf823rec.getPrimaryKey().getEmployeeId())
                        currLine.setEarnCode(msf823rec.getPrimaryKey().getEarnCode())
                        currLine.setDedEarnTax("0")
                        if (batchParams.paramReportPeriod.trim().equals("P")){
                            currLine.setLastPerUnits(0)
                            currLine.setLastPerAmt(0)
                            currLine.setCurMthUnits(0)
                            currLine.setCurMthAmt(0)
                            currLine.setCurFisUnits(msf823rec.getPrvFisUnits())
                            currLine.setCurFisAmt(msf823rec.getPrvFisAmtL())
                        } else {
                            currLine.setLastPerUnits(msf823rec.getLastPerUnits())
                            currLine.setLastPerAmt(msf823rec.getLastPerAmtL())
                            currLine.setCurMthUnits(msf823rec.getCurMthUnits())
                            currLine.setCurMthAmt(msf823rec.getCurMthAmtL())
                            currLine.setCurFisUnits(msf823rec.getCurFisUnits())
                            currLine.setCurFisAmt(msf823rec.getCurFisAmtL())
                        }
                        arrayOfTrs824ALine.add(currLine)
                        msf823Count++
                }
            }
        })
    }

    /**
     * Validate employee personnel details
     * @param employeeId as a String
     * @return boolean
     */
    private boolean validateStaffCateg(String employeeId){
        debug("validateStaffCateg ${employeeId}")
        if (batchParams.paramStaffCateg.trim().equals("")){
            return true
        }
        try{
            MSF760Rec msf760rec = edoi.findByPrimaryKey(new MSF760Key(employeeId))
            if (msf760rec.getStaffCateg().trim().equals(batchParams.paramStaffCateg.trim())){
                return true
            } else{
                return false
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            return false
        }
    }

    /**
     * Validate earn code
     * @param earnCode as a String
     * @return boolean
     */
    private boolean validateEarnCode(String earnCode){
        debug("validateEarnCode ${earnCode}")
        String cntlKeyRest = "***" + earnCode
        try{
            MSF801_A_801Rec msf801_a_801rec = edoi.findByPrimaryKey(new MSF801_A_801Key("A",cntlKeyRest))
            if (msf801_a_801rec.getMiscRptFldAx1().trim().equals("")){
                return false
            } else{
                currLine = new Trs824ALine(" "," "," "," "," "," ",0,0,0,0,0,0)
                currLine.setEarnType(msf801_a_801rec.getMiscRptFldAx1())
                return true
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            return false
        }
    }

    /**
     * Get employee pay group
     * @param employeeId as a String
     */
    private void getPayGroup(String employeeId){
        debug("getPayGroup ${employeeId}")
        try{
            MSF820Rec msf820rec = edoi.findByPrimaryKey(new MSF820Key(employeeId))
            currLine.setPayGroup(msf820rec.getPayGroup())
            currLine.setPayLocation(msf820rec.getPayLocation())
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("getPayGroup error message : "+e.message)
        }
    }

    /**
     * Browse for employee deduction
     */
    private void processEmpDeduct(){
        info("processEmpDeduct")
        msf837Count = 0
        Constraint c1 = MSF837Key.dednCode.notEqualTo("000")
        def query = new QueryImpl(MSF837Rec.class).and(c1).orderBy(MSF837Rec.msf837Key)
        edoi.search(query, MAX_ROW_READ, {MSF837Rec msf837rec ->
            boolean processedSw = validateStaffCateg(msf837rec.getPrimaryKey().getEmployeeId())
            if (processedSw){
                boolean validateSw = validateDednCode(msf837rec.getPrimaryKey().getDednCode())
                if (validateSw){
                        getPayGroup(msf837rec.getPrimaryKey().getEmployeeId())
                        currLine.setEmployeeId(msf837rec.getPrimaryKey().getEmployeeId())
                        currLine.setEarnCode(msf837rec.getPrimaryKey().getDednCode())
                        if (batchParams.paramReportPeriod.trim().equals("P")){
                            currLine.setLastPerUnits(0)
                            currLine.setLastPerAmt(0)
                            currLine.setCurMthUnits(0)
                            currLine.setCurMthAmt(0)
                            currLine.setCurFisUnits(msf837rec.getPrvFisUnits())
                            currLine.setCurFisAmt(msf837rec.getPrvFisAmtL())
                        } else {
                            currLine.setLastPerUnits(msf837rec.getLastPerUnits())
                            currLine.setLastPerAmt(msf837rec.getLastPerAmtL())
                            currLine.setCurMthUnits(msf837rec.getCurMthUnits())
                            currLine.setCurMthAmt(msf837rec.getCurMthAmtL())
                            currLine.setCurFisUnits(msf837rec.getCurFisUnits())
                            currLine.setCurFisAmt(msf837rec.getCurFisAmtL())
                        }
                        arrayOfTrs824ALine.add(currLine)
                        msf837Count++
                }
            }
        })
    }

    /**
     * Validate inputted code
     * @param dednCode
     * @return
     */
    private boolean validateDednCode(String dednCode){
        debug("validateDednCode ${dednCode}")
        String cntlKeyRest = "***" + dednCode
        try{
            MSF801_D_801Rec msf801_d_801rec = edoi.findByPrimaryKey(new MSF801_D_801Key("D",cntlKeyRest))

            if (msf801_d_801rec.getDedTypeD().equals("R") || msf801_d_801rec.getDedTypeD().equals("S")){
                return false
            } else if (msf801_d_801rec.getDedTypeD().equals("T")){
                currLine = new Trs824ALine(" "," "," "," "," "," ",0,0,0,0,0,0)
                currLine.setEarnType(msf801_d_801rec.getMiscRptFldDx1())
                currLine.setDedEarnTax("1")
                return true
            } else{
                currLine = new Trs824ALine(" "," "," "," "," "," ",0,0,0,0,0,0)
                currLine.setDedEarnTax("2")
                currLine.setEarnType(msf801_d_801rec.getMiscRptFldDx1())
                return true
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            currLine.setDedEarnTax("3")
            return true
        }

    }

    /**
     * Generate report
     */
    private void generateReport(){
        info("generateReport")
        info("MSF823 Counter: "+ msf823Count)
        info("MSF837 Counter: "+ msf837Count)

        //Write Report Header
        ArrayList<String> reportHead = writeReportAHeader("1")
        ReportA = report.open("TRB824A", reportHead)
        writeReportBHeader()

        if (!arrayOfTrs824ALine.isEmpty()){
            readArrayReportLine("Y")
        }
        ReportA.close()
        ReportB.close()

        String medium = request.getPropertyString("Medium")
        info("Report Medium is ${medium}")
        if(!medium.equals("P")){
            info("Adding CSV into Request.")
            if (taskUUID?.trim()) {
                request.request.CURRENT.get().addOutput(oFile,
                        "text/comma-separated-values", "TRT824");
            }
        }
    }

    /**
     * Read report line collection
     * @param firstTimeSw as String
     */
    private void readArrayReportLine(String firstTimeSw){
        info("readArrayReportLine")
        Trs824ALine prevLine
        String payGroup
        String dedEarnTax
        String earnCode
        String earnType
        boolean endLoop
        int index
        BigDecimal tempValue
        for(Trs824ALine reportLine: arrayOfTrs824ALine){
            if (firstTimeSw.equals("Y")){
                firstTimeSw = "N"
                payGroup = reportLine.getPayGroup()
                dedEarnTax = reportLine.getDedEarnTax()
                earnCode = reportLine.getEarnCode()
                earnType = reportLine.getEarnType()
            }

            if (!dedEarnTax.equals(reportLine.getDedEarnTax())){
                printCodeTotal(prevLine)
                printEarnTotal(prevLine)
                printDedearnTotal(prevLine)
                dedEarnTax = reportLine.getDedEarnTax()
                earnType = reportLine.getEarnType()
                earnCode = reportLine.getEarnCode()
            }
            if (!earnType.equals(reportLine.getEarnType())){
                printCodeTotal(prevLine)
                printEarnTotal(prevLine)
                earnType = reportLine.getEarnType()
                earnCode = reportLine.getEarnCode()
            }
            if (!earnCode.equals(reportLine.getEarnCode())){
                printCodeTotal(prevLine)
                earnCode = reportLine.getEarnCode()
            }

            totLastPerUnits = totLastPerUnits + reportLine.getLastPerUnits()
            totLastPerAmt = totLastPerAmt + reportLine.getLastPerAmt()
            totCurMthUnits = totCurMthUnits + reportLine.getCurMthUnits()
            totCurMthAmt = totCurMthAmt + reportLine.getCurMthAmt()
            totCurFisUnits = totCurFisUnits + reportLine.getCurFisUnits()
            totCurFisAmt = totCurFisAmt + reportLine.getCurFisAmt()

            if (reportLine.getDedEarnTax().equals("0")){
                index = 0
                endLoop = false
                while(!endLoop){
                    if (tableRepfArray[index].getTr().trim().equals("")){
                        endLoop = true
                    } else if (tableRepfArray[index].getTr().equals(reportLine.getEarnType())){
                        tempValue = tableRepfArray[index].getTotpLastPerAmt() + reportLine.getLastPerAmt()
                        tableRepfArray[index].setTotpLastPerAmt(tempValue)
                        tempValue = tableRepfArray[index].getTotgLastPerAmt() + reportLine.getLastPerAmt()
                        tableRepfArray[index].setTotgLastPerAmt(tempValue)
                        tLastPerAmt = tLastPerAmt + reportLine.getLastPerAmt()
                        tegLastPerAmt = tegLastPerAmt + reportLine.getLastPerAmt()
                        teTotlLastPerAmt = teTotlLastPerAmt + reportLine.getLastPerAmt()

                        tempValue = tableRepfArray[index].getTotpCurMthAmt() + reportLine.getCurMthAmt()
                        tableRepfArray[index].setTotpCurMthAmt(tempValue)
                        tempValue = tableRepfArray[index].getTotgCurMthAmt() + reportLine.getCurMthAmt()
                        tableRepfArray[index].setTotgCurMthAmt(tempValue)
                        tCurMthAmt = tCurMthAmt + reportLine.getCurMthAmt()
                        tegCurMthAmt = tegCurMthAmt + reportLine.getCurMthAmt()
                        teTotlCurMthAmt = teTotlCurMthAmt + reportLine.getCurMthAmt()

                        tempValue = tableRepfArray[index].getTotpCurFisAmt() + reportLine.getCurFisAmt()
                        tableRepfArray[index].setTotpCurFisAmt(tempValue)
                        tempValue = tableRepfArray[index].getTotgCurFisAmt() + reportLine.getCurFisAmt()
                        tableRepfArray[index].setTotgCurFisAmt(tempValue)
                        tCurFisAmt = tCurFisAmt + reportLine.getCurFisAmt()
                        tegCurFisAmt = tegCurFisAmt + reportLine.getCurFisAmt()
                        teTotlCurFisAmt = teTotlCurFisAmt + reportLine.getCurFisAmt()

                        teTotlLastPerUnits = teTotlLastPerUnits + reportLine.getLastPerUnits()
                        teTotlCurMthUnits = teTotlCurMthUnits + reportLine.getCurMthUnits()
                        teTotlCurFisUnits = teTotlCurFisUnits + reportLine.getCurFisUnits()

                        endLoop = true
                    } else{
                        index++
                        if (index > 98){
                            endLoop = true
                        }
                    }
                }
            }

            if (reportLine.getDedEarnTax().equals("1")){
                txTotlLastPerUnits = txTotlLastPerUnits + reportLine.getLastPerUnits()
                txTotlLastPerAmt = txTotlLastPerAmt + reportLine.getLastPerAmt()
                txTotlCurMthUnits = txTotlCurMthUnits + reportLine.getCurMthUnits()
                txTotlCurMthAmt = txTotlCurMthAmt + reportLine.getCurMthAmt()
                txTotlCurFisUnits = txTotlCurFisUnits + reportLine.getCurFisUnits()
                txTotlCurFisAmt = txTotlCurFisAmt + reportLine.getCurFisAmt()
                txgTotlLastPerUnits = txgTotlLastPerUnits + reportLine.getLastPerUnits()
                txgTotlLastPerAmt = txgTotlLastPerAmt + reportLine.getLastPerAmt()
                txgTotlCurMthUnits = txgTotlCurMthUnits + reportLine.getCurMthUnits()
                txgTotlCurMthAmt = txgTotlCurMthAmt + reportLine.getCurMthAmt()
                txgTotlCurFisUnits = txgTotlCurFisUnits + reportLine.getCurFisUnits()
                txgTotlCurFisAmt = txgTotlCurFisAmt + reportLine.getCurFisAmt()
            }

            if (reportLine.getDedEarnTax().equals("2")){
                if (reportLine.getEarnType().trim().equals("")){
                    tempValue = tableRepfArrayD[99].getTotpLastPerAmtD() + reportLine.getLastPerAmt()
                    tableRepfArrayD[99].setTotpLastPerAmtD(tempValue)
                    tempValue = tableRepfArrayD[99].getTotgLastPerAmtD() + reportLine.getLastPerAmt()
                    tableRepfArrayD[99].setTotgLastPerAmtD(tempValue)
                    tLastPerAmtD = tLastPerAmtD + reportLine.getLastPerAmt()
                    tegLastPerAmtD = tegLastPerAmtD + reportLine.getLastPerAmt()
                    deTotlLastPerAmt = deTotlLastPerAmt + reportLine.getLastPerAmt()

                    tempValue = tableRepfArrayD[99].getTotpCurMthAmtD() + reportLine.getCurMthAmt()
                    tableRepfArrayD[99].setTotpCurMthAmtD(tempValue)
                    tempValue = tableRepfArrayD[99].getTotgCurMthAmtD() + reportLine.getCurMthAmt()
                    tableRepfArrayD[99].setTotgCurMthAmtD(tempValue)
                    tCurMthAmtD = tCurMthAmtD + reportLine.getCurMthAmt()
                    tegCurMthAmtD = tegCurMthAmtD + reportLine.getCurMthAmt()
                    deTotlCurMthAmt = deTotlCurMthAmt + reportLine.getCurMthAmt()

                    tempValue = tableRepfArrayD[99].getTotpCurFisAmtD() + reportLine.getCurFisAmt()
                    tableRepfArrayD[99].setTotpCurFisAmtD(tempValue)
                    tempValue = tableRepfArrayD[99].getTotgCurFisAmtD() + reportLine.getCurFisAmt()
                    tableRepfArrayD[99].setTotgCurFisAmtD(tempValue)
                    tCurFisAmtD = tCurFisAmtD + reportLine.getCurFisAmt()
                    tegCurFisAmtD = tegCurFisAmtD + reportLine.getCurFisAmt()
                    deTotlCurFisAmt = deTotlCurFisAmt + reportLine.getCurFisAmt()

                    deTotlLastPerUnits = deTotlLastPerUnits + reportLine.getLastPerUnits()
                    deTotlCurMthUnits = deTotlCurMthUnits + reportLine.getCurMthUnits()
                    deTotlCurFisUnits = deTotlCurFisUnits + reportLine.getCurFisUnits()
                }

                index = 0
                endLoop = false
                while(!endLoop){
                    if (tableRepfArrayD[index].getTrD().trim().equals("")){
                        endLoop = true
                    } else if (tableRepfArrayD[index].getTrD().equals(reportLine.getEarnType())){
                        tempValue = tableRepfArrayD[index].getTotpLastPerAmtD() + reportLine.getLastPerAmt()
                        tableRepfArrayD[index].setTotpLastPerAmtD(tempValue)
                        tempValue = tableRepfArrayD[index].getTotgLastPerAmtD() + reportLine.getLastPerAmt()
                        tableRepfArrayD[index].setTotgLastPerAmtD(tempValue)
                        tLastPerAmtD = tLastPerAmtD + reportLine.getLastPerAmt()
                        tegLastPerAmtD = tegLastPerAmtD + reportLine.getLastPerAmt()
                        deTotlLastPerAmt = deTotlLastPerAmt + reportLine.getLastPerAmt()

                        tempValue = tableRepfArrayD[index].getTotpCurMthAmtD() + reportLine.getCurMthAmt()
                        tableRepfArrayD[index].setTotpCurMthAmtD(tempValue)
                        tempValue = tableRepfArrayD[index].getTotgCurMthAmtD() + reportLine.getCurMthAmt()
                        tableRepfArrayD[index].setTotgCurMthAmtD(tempValue)
                        tCurMthAmtD = tCurMthAmtD + reportLine.getCurMthAmt()
                        tegCurMthAmtD = tegCurMthAmtD + reportLine.getCurMthAmt()
                        deTotlCurMthAmt = deTotlCurMthAmt + reportLine.getCurMthAmt()

                        tempValue = tableRepfArrayD[index].getTotpCurFisAmtD() + reportLine.getCurFisAmt()
                        tableRepfArrayD[index].setTotpCurFisAmtD(tempValue)
                        tempValue = tableRepfArrayD[index].getTotgCurFisAmtD() + reportLine.getCurFisAmt()
                        tableRepfArrayD[index].setTotgCurFisAmtD(tempValue)
                        tCurFisAmtD = tCurFisAmtD + reportLine.getCurFisAmt()
                        tegCurFisAmtD = tegCurFisAmtD + reportLine.getCurFisAmt()
                        deTotlCurFisAmt = deTotlCurFisAmt + reportLine.getCurFisAmt()

                        deTotlLastPerUnits = deTotlLastPerUnits + reportLine.getLastPerUnits()
                        deTotlCurMthUnits = deTotlCurMthUnits + reportLine.getCurMthUnits()
                        deTotlCurFisUnits = deTotlCurFisUnits + reportLine.getCurFisUnits()

                        endLoop = true
                    } else{
                        index++
                        if (index > 98){
                            endLoop = true
                        }
                    }
                }
            }

            prevLine = reportLine

        }
        printCodeTotal(prevLine)
        printEarnTotal(prevLine)
        printDedearnTotal(prevLine)

        printGrandTotal()
    }

    /**
     * print report detail
     */
    private String printCodeTotal(Trs824ALine reportLine){
        debug("printCodeTotal")
        String cntlKeyRest = "***" + reportLine.getEarnCode()
        String recType
        if (reportLine.getDedEarnTax().equals("0")){
            recType = "A"
        } else {
            recType = "D"
        }
        String description = getDescription(recType,cntlKeyRest)
        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = reportLine.getEarnCode().padRight(6) +
                description.padRight(35) +
                "              " +
                "                   " +
                "            " +
                "                 " +
                twoDecimalFormatter(totCurFisUnits, "U").padLeft(12) + "  " +
                twoDecimalFormatter(totCurFisAmt, "A").padLeft(15)
        } else {
            contentToWrite = reportLine.getEarnCode().padRight(6) +
                description.padRight(35) +
                twoDecimalFormatter(totLastPerUnits, "U").padLeft(12) + "  " +
                twoDecimalFormatter(totLastPerAmt, "A").padLeft(15) + "    " +
                twoDecimalFormatter(totCurMthUnits, "U").padLeft(10) + "  " +
                twoDecimalFormatter(totCurMthAmt, "A").padLeft(15) + "  " +
                twoDecimalFormatter(totCurFisUnits, "U").padLeft(12) + "  " +
                twoDecimalFormatter(totCurFisAmt, "A").padLeft(15)
        }
        ReportA.write(contentToWrite)

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = reportLine.getPayLocation()+","+
                reportLine.getEarnCode()+","+
                description+","+
                reportLine.getEarnType() + ","+
                " ,"+
                " ,"+
                " ,"+
                " ,"+
                twoDecimalFormatter(totCurFisUnits, "U")+","+
                twoDecimalFormatter(totCurFisAmt, "A")+"\n"
        } else {
            contentToWrite = reportLine.getPayLocation()+","+
                reportLine.getEarnCode()+","+
                description+","+
                reportLine.getEarnType() + ","+
                twoDecimalFormatter(totLastPerUnits, "U")+","+
                twoDecimalFormatter(totLastPerAmt, "A")+","+
                twoDecimalFormatter(totCurMthUnits, "U")+","+
                twoDecimalFormatter(totCurMthAmt, "A")+","+
                twoDecimalFormatter(totCurFisUnits, "U")+","+
                twoDecimalFormatter(totCurFisAmt, "A")+"\n"
        }
        ReportB.write(contentToWrite)

        // Initialize value
        totLastPerUnits = 0
        totLastPerAmt = 0
        totCurMthUnits = 0
        totCurMthAmt = 0
        totCurFisUnits = 0
        totCurFisAmt = 0

    }

    /**
     * Get description
     * @param recType as String
     * @param cntlKeyRest as String
     * @return String
     */
    private String getDescription(String recType, String cntlKeyRest){
        debug("getDescription")
        if (recType.equals("A")){
            try{
                MSF801_A_801Rec msf801_a_801rec = edoi.findByPrimaryKey(new MSF801_A_801Key(recType,cntlKeyRest))
                return msf801_a_801rec.getTnameA()
            }
            catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
                return " "
            }
        } else{
            try{
                MSF801_D_801Rec msf801_d_801rec = edoi.findByPrimaryKey(new MSF801_D_801Key(recType,cntlKeyRest))
                return msf801_d_801rec.getTnameD()
            }
            catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
                return " "
            }
        }
    }

    /**
     * print total earning
     */
    private void printEarnTotal(Trs824ALine reportLine){
        debug("printEarnTotal")
        int index
        boolean endLoop
        String description
        if (reportLine.getDedEarnTax().equals("0")){
            index = 0
            endLoop = false
            while(!endLoop){
                if (tableRepfArray[index].getTr().trim().equals("")){
                    endLoop = true
                } else if (tableRepfArray[index].getTr().equals(reportLine.getEarnType())){
                    ReportA.writeLine(132,"-")
                    description = "   " + tableRepfArray[index].getTrDesc()
                    if (batchParams.paramReportPeriod.trim().equals("P")){
                        contentToWrite = "      " + description.padRight(37) + "            " +
                            "                               " +
                            "                               " +
                            twoDecimalFormatter(tCurFisAmt, "A").padLeft(15)
                    } else {
                        contentToWrite = "      " + description.padRight(37) + "            " +
                            twoDecimalFormatter(tLastPerAmt, "A").padLeft(15) + "                " +
                            twoDecimalFormatter(tCurMthAmt, "A").padLeft(15) + "                " +
                            twoDecimalFormatter(tCurFisAmt, "A").padLeft(15)
                    }
                    ReportA.write(contentToWrite)
                    ReportA.write(" ")
                    ReportA.write(" ")

                    if (batchParams.paramReportPeriod.trim().equals("P")){
                        contentToWrite = " , ," +
                            description+", , ,"+
                            " , ,"+
                            " , ,"+
                            twoDecimalFormatter(tCurFisAmt, "A")+"\n"
                    } else {
                        contentToWrite = " , ," +
                            description+", , ,"+
                            twoDecimalFormatter(tLastPerAmt, "A")+", ,"+
                            twoDecimalFormatter(tCurMthAmt, "A")+", ,"+
                            twoDecimalFormatter(tCurFisAmt, "A")+"\n"
                    }
                    ReportB.write(contentToWrite)
                    ReportB.write("\n")

                    index++
                    if (index > 98){
                        endLoop = true
                    }

                } else{
                    index++
                    if (index > 98){
                        endLoop = true
                    }
                }
            }
        }

        if (reportLine.getDedEarnTax().equals("2")){
            if (reportLine.getEarnType().trim().equals("")){
                ReportA.writeLine(132,"-")
                description = "   " + "Miscellaneous Deductions"
                if (batchParams.paramReportPeriod.trim().equals("P")){
                    contentToWrite = "      " + description.padRight(37) + "            " +
                        "                               " +
                        "                               " +
                        twoDecimalFormatter(tCurFisAmtD, "A").padLeft(15)
                } else {
                    contentToWrite = "      " + description.padRight(37) + "            " +
                        twoDecimalFormatter(tLastPerAmtD, "A").padLeft(15) + "                " +
                        twoDecimalFormatter(tCurMthAmtD, "A").padLeft(15) + "                " +
                        twoDecimalFormatter(tCurFisAmtD, "A").padLeft(15)
                }
                ReportA.write(contentToWrite)
                ReportA.write(" ")
                ReportA.write(" ")

                if (batchParams.paramReportPeriod.trim().equals("P")){
                    contentToWrite = " , ," +
                        description+", , ,"+
                        " , ,"+
                        " , ,"+
                        twoDecimalFormatter(tCurFisAmtD, "A")+"\n"
                } else {
                    contentToWrite = " , ," +
                        description+", , ,"+
                        twoDecimalFormatter(tLastPerAmtD, "A")+", ,"+
                        twoDecimalFormatter(tCurMthAmtD, "A")+", ,"+
                        twoDecimalFormatter(tCurFisAmtD, "A")+"\n"
                }
                ReportB.write(contentToWrite)
                ReportB.write("\n")

            } else {
                index = 0
                endLoop = false
                while(!endLoop){
                    if (tableRepfArrayD[index].getTrD().trim().equals("")){
                        endLoop = true
                    } else if (tableRepfArrayD[index].getTrD().equals(reportLine.getEarnType())){
                        ReportA.writeLine(132,"-")
                        description = "   " + tableRepfArrayD[index].getTrDescD()
                        if (batchParams.paramReportPeriod.trim().equals("P")){
                            contentToWrite = "      " + description.padRight(37) + "            " +
                                "                               " +
                                "                               " +
                                twoDecimalFormatter(tCurFisAmtD, "A").padLeft(15)
                        } else {
                            contentToWrite = "      " + description.padRight(37) + "            " +
                                twoDecimalFormatter(tLastPerAmtD, "A").padLeft(15) + "                " +
                                twoDecimalFormatter(tCurMthAmtD, "A").padLeft(15) + "                " +
                                twoDecimalFormatter(tCurFisAmtD, "A").padLeft(15)
                        }
                        ReportA.write(contentToWrite)
                        ReportA.write(" ")
                        ReportA.write(" ")
                        if (batchParams.paramReportPeriod.trim().equals("P")){
                            contentToWrite = " , ," +
                                description+", , ,"+
                                " , ,"+
                                " , ,"+
                                twoDecimalFormatter(tCurFisAmtD, "A")+"\n"
                        } else {
                            contentToWrite = " , ," +
                                description+", , ,"+
                                twoDecimalFormatter(tLastPerAmtD, "A")+", ,"+
                                twoDecimalFormatter(tCurMthAmtD, "A")+", ,"+
                                twoDecimalFormatter(tCurFisAmtD, "A")+"\n"
                        }
                        ReportB.write(contentToWrite)
                        ReportB.write("\n")

                        index++
                        if (index > 98){
                            endLoop = true
                        }

                    } else{
                        index++
                        if (index > 98){
                            endLoop = true
                        }
                    }
                }

            }
        }
        // Initialize the value
        tLastPerAmt = 0
        tCurMthAmt = 0
        tCurFisAmt = 0
        tLastPerAmtD = 0
        tCurMthAmtD = 0
        tCurFisAmtD = 0
    }

    /**
     * print total deduction
     */
    private void printDedearnTotal(Trs824ALine reportLine){
        debug("printDedearnTotal")
        String description
        if (reportLine.getDedEarnTax().equals("0")){
            description = "** Total  Earnings "
            ReportA.writeLine(132,"-")
            if (batchParams.paramReportPeriod.trim().equals("P")){
                contentToWrite = "      " + description.padRight(37) +
                    "            " +
                    "                   " +
                    "            " +
                    "                 " +
                    twoDecimalFormatter(teTotlCurFisUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(teTotlCurFisAmt, "A").padLeft(15)
            } else {
                contentToWrite = "      " + description.padRight(37) +
                    twoDecimalFormatter(teTotlLastPerUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(teTotlLastPerAmt, "A").padLeft(15) + "    " +
                    twoDecimalFormatter(teTotlCurMthUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(teTotlCurMthAmt, "A").padLeft(15) + "  " +
                    twoDecimalFormatter(teTotlCurFisUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(teTotlCurFisAmt, "A").padLeft(15)
            }
            ReportA.write(contentToWrite)
            ReportA.writeLine(132,"-")
            ReportA.write(" ")

            ReportB.write("\n")
            if (batchParams.paramReportPeriod.trim().equals("P")){
                contentToWrite = " , ," +
                    description+", ,"+
                    " ,"+
                    " ,"+
                    " ,"+
                    " ,"+
                    twoDecimalFormatter(teTotlCurFisUnits, "U")+","+
                    twoDecimalFormatter(teTotlCurFisAmt, "A")+"\n"
            } else {
                contentToWrite = " , ," +
                    description+", ,"+
                    twoDecimalFormatter(teTotlLastPerUnits, "U")+","+
                    twoDecimalFormatter(teTotlLastPerAmt, "A")+","+
                    twoDecimalFormatter(teTotlCurMthUnits, "U")+","+
                    twoDecimalFormatter(teTotlCurMthAmt, "A")+","+
                    twoDecimalFormatter(teTotlCurFisUnits, "U")+","+
                    twoDecimalFormatter(teTotlCurFisAmt, "A")+"\n"
            }
            ReportB.write(contentToWrite)
            ReportB.write("\n")

            teTotlLastPerAmt = 0
            teTotlLastPerUnits = 0
            teTotlCurMthAmt = 0
            teTotlCurMthUnits = 0
            teTotlCurFisAmt = 0
            teTotlCurFisUnits = 0
        } else if (reportLine.getDedEarnTax().equals("1")) {
            description = "** Total  Tax "
            ReportA.writeLine(132,"-")
            if (batchParams.paramReportPeriod.trim().equals("P")){
                contentToWrite = "      " + description.padRight(37) +
                    "            " +
                    "                   " +
                    "            " +
                    "                 " +
                    twoDecimalFormatter(txTotlCurFisUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(txTotlCurFisAmt, "A").padLeft(15)
            } else {
                contentToWrite = "      " + description.padRight(37) +
                    twoDecimalFormatter(txTotlLastPerUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(txTotlLastPerAmt, "A").padLeft(15) + "    " +
                    twoDecimalFormatter(txTotlCurMthUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(txTotlCurMthAmt, "A").padLeft(15) + "  " +
                    twoDecimalFormatter(txTotlCurFisUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(txTotlCurFisAmt, "A").padLeft(15)
            }
            ReportA.write(contentToWrite)
            ReportA.writeLine(132,"-")
            ReportA.write(" ")

            ReportB.write("\n")
            if (batchParams.paramReportPeriod.trim().equals("P")){
                contentToWrite = " , ," +
                    description+", ,"+
                    " ,"+
                    " ,"+
                    " ,"+
                    " ,"+
                    twoDecimalFormatter(txTotlCurFisUnits, "U")+","+
                    twoDecimalFormatter(txTotlCurFisAmt, "A")+"\n"
            } else {
                contentToWrite = " , ," +
                    description+", ,"+
                    twoDecimalFormatter(txTotlLastPerUnits, "U")+","+
                    twoDecimalFormatter(txTotlLastPerAmt, "A")+","+
                    twoDecimalFormatter(txTotlCurMthUnits, "U")+","+
                    twoDecimalFormatter(txTotlCurMthAmt, "A")+","+
                    twoDecimalFormatter(txTotlCurFisUnits, "U")+","+
                    twoDecimalFormatter(txTotlCurFisAmt, "A")+"\n"
            }
            ReportB.write(contentToWrite)
            ReportB.write("\n")

            txTotlLastPerAmt = 0
            txTotlLastPerUnits = 0
            txTotlCurMthAmt = 0
            txTotlCurMthUnits = 0
            txTotlCurFisAmt = 0
            txTotlCurFisUnits = 0

        }  else if (reportLine.getDedEarnTax().equals("2")){
            description = "** Total  Deductions "
            ReportA.writeLine(132,"-")
            if (batchParams.paramReportPeriod.trim().equals("P")){
                contentToWrite = "      " + description.padRight(37) +
                    "            " +
                    "                   " +
                    "            " +
                    "                 " +
                    twoDecimalFormatter(deTotlCurFisUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(deTotlCurFisAmt, "A").padLeft(15)
            } else {
                contentToWrite = "      " + description.padRight(37) +
                    twoDecimalFormatter(deTotlLastPerUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(deTotlLastPerAmt, "A").padLeft(15) + "    " +
                    twoDecimalFormatter(deTotlCurMthUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(deTotlCurMthAmt, "A").padLeft(15) + "  " +
                    twoDecimalFormatter(deTotlCurFisUnits, "U").padLeft(10) + "  " +
                    twoDecimalFormatter(deTotlCurFisAmt, "A").padLeft(15)
            }
            ReportA.write(contentToWrite)
            ReportA.writeLine(132,"-")
            ReportA.write(" ")

            ReportB.write("\n")
            if (batchParams.paramReportPeriod.trim().equals("P")){
                contentToWrite = " , ," +
                    description+", ,"+
                    " ,"+
                    " ,"+
                    " ,"+
                    " ,"+
                    twoDecimalFormatter(deTotlCurFisUnits, "U")+","+
                    twoDecimalFormatter(deTotlCurFisAmt, "A")+"\n"
            } else {
                contentToWrite = " , ," +
                    description+", ,"+
                    twoDecimalFormatter(deTotlLastPerUnits, "U")+","+
                    twoDecimalFormatter(deTotlLastPerAmt, "A")+","+
                    twoDecimalFormatter(deTotlCurMthUnits, "U")+","+
                    twoDecimalFormatter(deTotlCurMthAmt, "A")+","+
                    twoDecimalFormatter(deTotlCurFisUnits, "U")+","+
                    twoDecimalFormatter(deTotlCurFisAmt, "A")+"\n"
            }
            ReportB.write(contentToWrite)
            ReportB.write("\n")

            degTotlLastPerAmt = degTotlLastPerAmt + deTotlLastPerAmt
            degTotlCurMthAmt = degTotlCurMthAmt + deTotlCurMthAmt
            degTotlCurFisAmt = degTotlCurFisAmt + deTotlCurFisAmt

            deTotlLastPerAmt = 0
            deTotlLastPerUnits = 0
            deTotlCurMthAmt = 0
            deTotlCurMthUnits = 0
            deTotlCurFisAmt = 0
            deTotlCurFisUnits = 0
        } else{
            description = "** Unknown"
            ReportA.writeLine(132,"-")
            ReportA.write("      " + description.padRight(37))
            ReportA.writeLine(132,"-")
            ReportA.write(" ")

            ReportB.write("\n")
            ReportB.write(" , ,** Unknown, , , , , , , ")
            ReportB.write("\n")

        }

    }

    /**
     * print grand total of earning and deduction
     */
    private void printGrandTotal(){
        info("printGrandTotal")
        ReportA.write("\f")
        ReportB.write("\n")
        ArrayList<String> reportHead = writeReportAHeader("2")
        ReportA.pageHeadings = reportHead
        ReportA.newPage()
        int index = 0
        boolean endLoop = false
        String description
        while (!endLoop){
            if (tableRepfArray[index].getTr().trim().equals("")){
                endLoop = true
            } else{
                description = "Grand Total " + tableRepfArray[index].getTrDesc()
                if (batchParams.paramReportPeriod.trim().equals("P")){
                    contentToWrite = description.padRight(43) + "            " +
                        "                               " +
                        "                               " +
                        twoDecimalFormatter(tableRepfArray[index].getTotgCurFisAmt(), "A").padLeft(15)
                } else {
                    contentToWrite = description.padRight(43) + "            " +
                        twoDecimalFormatter(tableRepfArray[index].getTotgLastPerAmt(), "A").padLeft(15) + "                " +
                        twoDecimalFormatter(tableRepfArray[index].getTotgCurMthAmt(), "A").padLeft(15) + "                " +
                        twoDecimalFormatter(tableRepfArray[index].getTotgCurFisAmt(), "A").padLeft(15)
                }
                ReportA.write(contentToWrite)

                if (batchParams.paramReportPeriod.trim().equals("P")){
                    contentToWrite = " , ," +
                        description+", , ,"+
                        " , ,"+
                        " , ,"+
                        twoDecimalFormatter(tableRepfArray[index].getTotgCurFisAmt(), "A")+"\n"
                } else {
                    contentToWrite = " , ," +
                        description+", , ,"+
                        twoDecimalFormatter(tableRepfArray[index].getTotgLastPerAmt(), "A")+", ,"+
                        twoDecimalFormatter(tableRepfArray[index].getTotgCurMthAmt(), "A")+", ,"+
                        twoDecimalFormatter(tableRepfArray[index].getTotgCurFisAmt(), "A")+"\n"
                }
                ReportB.write(contentToWrite)

                index++

                if (index>98) {
                    endLoop = true
                }

            }
        }

        ReportA.writeLine(132,"-")
        ReportB.write("\n")
        description = "Grand Total Earnings"
        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = description.padRight(43) + "            " +
                "                               " +
                "                               " +
                twoDecimalFormatter(tegCurFisAmt, "A").padLeft(15)
        } else {
            contentToWrite = description.padRight(43) + "            " +
                twoDecimalFormatter(tegLastPerAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(tegCurMthAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(tegCurFisAmt, "A").padLeft(15)
        }
        ReportA.write(contentToWrite)

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = " , ," +
                description+", , ,"+
                " , ,"+
                " , ,"+
                twoDecimalFormatter(tegCurFisAmt, "A")+"\n"
        } else {
            contentToWrite = " , ," +
                description+", , ,"+
                twoDecimalFormatter(tegLastPerAmt, "A")+", ,"+
                twoDecimalFormatter(tegCurMthAmt, "A")+", ,"+
                twoDecimalFormatter(tegCurFisAmt, "A")+"\n"
        }
        ReportB.write(contentToWrite)
        ReportA.writeLine(132,"-")
        ReportB.write("\n")

        description = "Grand Total Miscellaneous Deductions"
        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = description.padRight(43) + "            " +
                "                               " +
                "                               " +
                twoDecimalFormatter(tableRepfArrayD[99].getTotgCurFisAmtD(), "A").padLeft(15)
        } else {
            contentToWrite = description.padRight(43) + "            " +
                twoDecimalFormatter(tableRepfArrayD[99].getTotgLastPerAmtD(), "A").padLeft(15) + "                " +
                twoDecimalFormatter(tableRepfArrayD[99].getTotgCurMthAmtD(), "A").padLeft(15) + "                " +
                twoDecimalFormatter(tableRepfArrayD[99].getTotgCurFisAmtD(), "A").padLeft(15)
        }
        ReportA.write(contentToWrite)

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = " , ," +
                description+", , ,"+
                " , ,"+
                " , ,"+
                twoDecimalFormatter(tableRepfArrayD[99].getTotgCurFisAmtD(), "A")+"\n"
        } else {
            contentToWrite = " , ," +
                description+", , ,"+
                twoDecimalFormatter(tableRepfArrayD[99].getTotgLastPerAmtD(), "A")+", ,"+
                twoDecimalFormatter(tableRepfArrayD[99].getTotgCurMthAmtD(), "A")+", ,"+
                twoDecimalFormatter(tableRepfArrayD[99].getTotgCurFisAmtD(), "A")+"\n"
        }
        ReportB.write(contentToWrite)

        index = 0
        endLoop = false
        while (!endLoop){
            if (tableRepfArrayD[index].getTrD().trim().equals("")){
                endLoop = true
            } else{
                description = "Grand Total " + tableRepfArrayD[index].getTrDescD()
                if (batchParams.paramReportPeriod.trim().equals("P")){
                    contentToWrite = description.padRight(43) + "            " +
                        "                               " +
                        "                               " +
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgCurFisAmtD(), "A").padLeft(15)
                } else {
                    contentToWrite = description.padRight(43) + "            " +
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgLastPerAmtD(), "A").padLeft(15) + "                " +
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgCurMthAmtD(), "A").padLeft(15) + "                " +
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgCurFisAmtD(), "A").padLeft(15)
                }
                ReportA.write(contentToWrite)

                if (batchParams.paramReportPeriod.trim().equals("P")){
                    contentToWrite = " , ," +
                        description+", , ,"+
                        " , ,"+
                        " , ,"+
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgCurFisAmtD(), "A")+"\n"
                } else {
                    contentToWrite = " , ," +
                        description+", , ,"+
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgLastPerAmtD(), "A")+", ,"+
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgCurMthAmtD(), "A")+", ,"+
                        twoDecimalFormatter(tableRepfArrayD[index].getTotgCurFisAmtD(), "A")+"\n"
                }
                ReportB.write(contentToWrite)

                index++

            }
        }

        description = "Grand Total Deductions"
        ReportA.writeLine(132,"-")
        ReportB.write("\n")

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = description.padRight(43) + "            " +
                "                               " +
                "                               " +
                twoDecimalFormatter(degTotlCurFisAmt, "A").padLeft(15)
        } else {
            contentToWrite = description.padRight(43) + "            " +
                twoDecimalFormatter(degTotlLastPerAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(degTotlCurMthAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(degTotlCurFisAmt, "A").padLeft(15)
        }
        ReportA.write(contentToWrite)

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = " , ," +
                description+", , ,"+
                " , ,"+
                " , ,"+
                twoDecimalFormatter(degTotlCurFisAmt, "A")+"\n"
        } else {
            contentToWrite = " , ," +
                description+", , ,"+
                twoDecimalFormatter(degTotlLastPerAmt, "A")+", ,"+
                twoDecimalFormatter(degTotlCurMthAmt, "A")+", ,"+
                twoDecimalFormatter(degTotlCurFisAmt, "A")+"\n"
        }
        ReportB.write(contentToWrite)

        ReportA.writeLine(132,"-")
        ReportB.write("\n")
        ReportA.write(" ")


        description = "Grand Total Tax"
        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = description.padRight(43) + "            " +
                "                               " +
                "                               " +
                twoDecimalFormatter(txgTotlCurFisAmt, "A").padLeft(15)
        } else {
            contentToWrite = description.padRight(43) + "            " +
                twoDecimalFormatter(txgTotlLastPerAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(txgTotlCurMthAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(txgTotlCurFisAmt, "A").padLeft(15)
        }
        ReportA.write(contentToWrite)

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = " , ," +
                description+", , ,"+
                " , ,"+
                " , ,"+
                twoDecimalFormatter(txgTotlCurFisAmt, "A")+"\n"
        } else {
            contentToWrite = " , ," +
                description+", , ,"+
                twoDecimalFormatter(txgTotlLastPerAmt, "A")+", ,"+
                twoDecimalFormatter(txgTotlCurMthAmt, "A")+", ,"+
                twoDecimalFormatter(txgTotlCurFisAmt, "A")+"\n"
        }
        ReportB.write(contentToWrite)

        ReportA.write(" ")
        ReportB.write("\n")

        description = "Grand Total Net Pay"
        BigDecimal wpLastPerAmt = tegLastPerAmt - txgTotlLastPerAmt - degTotlLastPerAmt
        BigDecimal wpCurMthAmt = tegCurMthAmt - txgTotlCurMthAmt - degTotlCurMthAmt
        BigDecimal wpCurFisAmt = tegCurFisAmt - txgTotlCurFisAmt - degTotlCurFisAmt

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = description.padRight(43) + "            " +
                "                               " +
                "                               " +
                twoDecimalFormatter(wpCurFisAmt, "A").padLeft(15)
        } else {
            contentToWrite = description.padRight(43) + "            " +
                twoDecimalFormatter(wpLastPerAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(wpCurMthAmt, "A").padLeft(15) + "                " +
                twoDecimalFormatter(wpCurFisAmt, "A").padLeft(15)
        }
        ReportA.write(contentToWrite)

        if (batchParams.paramReportPeriod.trim().equals("P")){
            contentToWrite = " , ," +
                description+", , ,"+
                " , ,"+
                " , ,"+
                twoDecimalFormatter(wpCurFisAmt, "A")+"\n"
        } else {
            contentToWrite = " , ," +
                description+", , ,"+
                twoDecimalFormatter(wpLastPerAmt, "A")+", ,"+
                twoDecimalFormatter(wpCurMthAmt, "A")+", ,"+
                twoDecimalFormatter(wpCurFisAmt, "A")+"\n"
        }
        ReportB.write(contentToWrite)

    }

    /**
     * format unit/amount into two digit after dot and set - as suffix
     * @param amount
     * @param type: U for unit formatting; A for amount formatting
     * @return formatted amount in type String
     */

    private String twoDecimalFormatter(def amount, String type){
        debug("twoDecimalFormatter")
        String result = ""
        if(type.equals("U")){
            DecimalFormat unitFormatter = new DecimalFormat("######.##")
            unitFormatter.setMinimumFractionDigits(2)
            unitFormatter.setNegativePrefix("")
            unitFormatter.setNegativeSuffix("-")
            result = unitFormatter.format(amount)
        } else {
            DecimalFormat totalFormatter = new DecimalFormat("#######.##")
            totalFormatter.setMinimumFractionDigits(2)
            totalFormatter.setNegativePrefix("")
            totalFormatter.setNegativeSuffix("-")
            result = totalFormatter.format(amount)
        }

        if(!result.substring(result.length()-1).equals("-")){
            result += " "
        }

        return result
    }

    /** 
     * print batch report
     */

    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
    }
}

/*run script*/
ProcessTrb824 process = new ProcessTrb824();
process.runBatch(binding);
