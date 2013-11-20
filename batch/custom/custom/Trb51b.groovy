/*
 @Ventyx 2012
 * Conversion from trb51b.cbl
 *
 * This program will generate the Total Injuries Report
 * for the Health and Safety Module.
 */
package com.mincom.ellipse.script.custom;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf510.*;
import com.mincom.ellipse.edoi.ejb.msf514.*;
import com.mincom.ellipse.edoi.ejb.msf536.*;
import com.mincom.ellipse.edoi.ejb.msf53a.*;
import com.mincom.ellipse.edoi.ejb.msf723.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf828.*;
import com.mincom.ellipse.edoi.ejb.msf870.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf893.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.*;
import com.mincom.eql.impl.*;

/**
 * Request Parameters for Trb51b. <br>
 *
 * <li>First request parameter is <code>paramPayLocation</code> represents Pay Location</li>
 * <li>Second request parameter is <code>paramWorkGroup</code> represents Work Group occurs 10 times</li>
 * <li>Third request parameter is <code>paramDateFrom</code> represents Date From of injury occurrence</li>
 * <li>Fourth request parameter is <code>paramDateTo</code> represent Date to of injury occurrence</li>
 * <li>Fifth request parameter is <code>paramEmployeeId</code> represent Employee Id occurs 10 times</li>
 * <li>Sixth request parameter is <code>paramReportingCode</code> represent Primary Reporting Code</li>
 */
public class ParamsTrb51b{
    //List of Input Parameters
    String paramPayLocation;
    String paramWorkGroup1;
    String paramWorkGroup2;
    String paramWorkGroup3;
    String paramWorkGroup4;
    String paramWorkGroup5;
    String paramWorkGroup6;
    String paramWorkGroup7;
    String paramWorkGroup8;
    String paramWorkGroup9;
    String paramWorkGroup10;
    String paramDateFrom;
    String paramDateTo;
    String paramEmployeeId1;
    String paramEmployeeId2;
    String paramEmployeeId3;
    String paramEmployeeId4;
    String paramEmployeeId5;
    String paramEmployeeId6;
    String paramEmployeeId7;
    String paramEmployeeId8;
    String paramEmployeeId9;
    String paramEmployeeId10;
    String paramReportingCode;
}

public class ProcessTrb51b extends SuperBatch {
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 2;
    private ParamsTrb51b batchParams;

    private class Trb51baLine implements Comparable<Trb51baLine> {
        private String revOccDate;
        private String wcLoc;
        private String employeeId;
        private String incidentNo;
        private String natureInjury;
        private String incidentDesc;
        private BigDecimal daysLost;
        private BigDecimal hoursLost;
        public Trb51baLine(String revOccDate, String wcLoc, String employeeId,
        String incidentNo, String natureInjury, String incidentDesc,
        BigDecimal daysLost, BigDecimal hoursLost) {
            this.revOccDate = revOccDate;
            this.wcLoc = wcLoc;
            this.employeeId = employeeId;
            this.incidentNo = incidentNo;
            this.natureInjury = natureInjury;
            this.incidentDesc = incidentDesc;
            this.daysLost = daysLost;
            this.hoursLost = hoursLost;
        }
        public String getRevOccDate() {
            return revOccDate;
        }
        public void setRevOccDate(String revOccDate) {
            this.revOccDate = revOccDate;
        }
        public String getWcLoc() {
            return wcLoc;
        }
        public void setWcLoc(String wcLoc) {
            this.wcLoc = wcLoc;
        }
        public String getEmployeeId() {
            return employeeId;
        }
        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }
        public String getIncidentNo() {
            return incidentNo;
        }
        public void setIncidentNo(String incidentNo) {
            this.incidentNo = incidentNo;
        }
        public String getNatureInjury() {
            return natureInjury;
        }
        public void setNatureInjury(String natureInjury) {
            this.natureInjury = natureInjury;
        }
        public String getIncidentDesc() {
            return incidentDesc;
        }
        public void setIncidentDesc(String incidentDesc) {
            this.incidentDesc = incidentDesc;
        }
        public BigDecimal getDaysLost() {
            return daysLost;
        }
        public void setDaysLost(BigDecimal daysLost) {
            this.daysLost = daysLost;
        }
        public BigDecimal getHoursLost() {
            return hoursLost;
        }
        public void setHoursLost(BigDecimal hoursLost) {
            this.hoursLost = hoursLost;
        }

        int compareTo(Trb51baLine otherReportLine){
            if (!wcLoc.equals(otherReportLine.getWcLoc())){
                return wcLoc.compareTo(otherReportLine.getWcLoc())
            }
            if (!incidentNo.equals(otherReportLine.getIncidentNo())){
                return incidentNo.compareTo(otherReportLine.getIncidentNo())
            }
            return 0
        }
    }

    private String[] paramWorkGroups;
    private String[] paramEmployeeIds;
    private def reportA
    String revOccDate
    String wcLoc
    String incidentDesc
    BigDecimal stdHrsDay = 0
    BigDecimal units
    BigDecimal daysLost
    BigDecimal hoursLost
    Trb51baLine reportLine
    ArrayList arrayOfTrb51baLine = new ArrayList()
    String payLoc
    DecimalFormat decFormatter = new DecimalFormat("#####0.00")

    /**
     * Run the main batch.
     * @param b is a binding object passed from the <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb51b())

        paramWorkGroups = new String[10]
        paramWorkGroups[0] = batchParams.paramWorkGroup1
        paramWorkGroups[1] = batchParams.paramWorkGroup2
        paramWorkGroups[2] = batchParams.paramWorkGroup3
        paramWorkGroups[3] = batchParams.paramWorkGroup4
        paramWorkGroups[4] = batchParams.paramWorkGroup5
        paramWorkGroups[5] = batchParams.paramWorkGroup6
        paramWorkGroups[6] = batchParams.paramWorkGroup7
        paramWorkGroups[7] = batchParams.paramWorkGroup8
        paramWorkGroups[8] = batchParams.paramWorkGroup9
        paramWorkGroups[9] = batchParams.paramWorkGroup10

        paramEmployeeIds = new String[10]
        paramEmployeeIds[0] = batchParams.paramEmployeeId1
        paramEmployeeIds[1] = batchParams.paramEmployeeId2
        paramEmployeeIds[2] = batchParams.paramEmployeeId3
        paramEmployeeIds[3] = batchParams.paramEmployeeId4
        paramEmployeeIds[4] = batchParams.paramEmployeeId5
        paramEmployeeIds[5] = batchParams.paramEmployeeId6
        paramEmployeeIds[6] = batchParams.paramEmployeeId7
        paramEmployeeIds[7] = batchParams.paramEmployeeId8
        paramEmployeeIds[8] = batchParams.paramEmployeeId9
        paramEmployeeIds[9] = batchParams.paramEmployeeId10

        //PrintRequest Parameters
        info("paramPayLocation: " + batchParams.paramPayLocation)
        for(int i = 0; i<10; i++){
            info("paramWorkGroup"+Integer.toString(i+1)+" : "+paramWorkGroups[i]);
        }

        info("paramDateFrom: " + batchParams.paramDateFrom)
        info("paramDateTo: " + batchParams.paramDateTo)

        for(int i = 0; i<10; i++){
            info("paramEmployeeId"+Integer.toString(i+1)+": "+paramEmployeeIds[i]);
        }

        info("paramReportingCode: " + batchParams.paramReportingCode)

        try {
            processBatch();

        } finally {
            printBatchReport();
        }
    }

    /**
     * Main process of the batch.
     */
    private void processBatch(){
        info("processBatch");
        //write process
        initialize()
        boolean error = validateRequestParams()
        if (!error){
            writeReportHeader()
            mainProcess()
        }
    }

    //additional method - start from here.

    /**
     * Initialize variable and write report title.
     */
    private void initialize(){
        info("initialize")
        reportA = report.open("TRB51BA")
        reportA.write(StringUtils.center("Total Injuries Report", 132))
        reportA.writeLine(132,"-")
    }

    /**
     * Write report Header.
     */
    private void writeReportHeader(){
        info("writeReportHeader")
        String stringLine
        String workGroup
        if (!batchParams.paramPayLocation.trim().equals("")){
            stringLine = ("Pay Location: " + payLoc).padRight(80)
        } else
        {
            stringLine = "Pay Location: All".padRight(80)
        }
        stringLine = stringLine + "Period Date :  "
        stringLine = stringLine + batchParams.paramDateFrom.substring(6,8) + "/" + batchParams.paramDateFrom.substring(4,6) + "/" + batchParams.paramDateFrom.substring(2,4) + " - "
        stringLine = stringLine + batchParams.paramDateTo.substring(6,8) + "/" + batchParams.paramDateTo.substring(4,6) + "/" + batchParams.paramDateTo.substring(2,4)
        reportA.write(stringLine)

        stringLine = ""
        for(int i = 0; i<10; i++){
            stringLine = stringLine + paramWorkGroups[i].padRight(8)
        }

        if (stringLine.trim().equals("")){
            stringLine = "Work Group :  All"
        } else{
            stringLine = "Work Group :  " + stringLine
        }
        reportA.write(stringLine)

        stringLine = ""
        for(int i = 0; i<10; i++){
            stringLine = stringLine + paramEmployeeIds[i].padRight(11)
        }

        if (stringLine.trim().equals("")){
            stringLine = "Employees  :  All"
        } else{
            stringLine = "Employees  :  " + stringLine
        }
        reportA.write(stringLine)
        reportA.write("PRC        :  " + batchParams.paramReportingCode)

        reportA.writeLine(132,"-")
        reportA.write("WC Loc  Empid   Employee Surname         Incident           Nat of Injury       Incident Description    LTI  Tot Full Tot Hrs")
        reportA.write("                                     Date      Number                                                   Y/N Days Lost   Lost")
        reportA.writeLine(132,"-")
    }

    /**
     * Validate request parameter.
     */
    private boolean validateRequestParams(){
        info("validateRequestParams")
        if (!batchParams.paramPayLocation.trim().equals("")){
            try{
                MSF010Rec msf010rec = edoi.findByPrimaryKey(new MSF010Key("PAYL", batchParams.paramPayLocation))
                payLoc =  msf010rec.getTableDesc()
            }
            catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
                info("Invalid Pay Location : " + batchParams.paramPayLocation)
                reportA.write("")
                reportA.write("*** ERROR: Invalid Pay Location ***")
                return true
            }
        }

        if (batchParams.paramDateFrom.trim().equals("")){
            info("Date Occ From is Mandatory parameter")
            reportA.write("")
            reportA.write("*** ERROR: Date Occ From is Mandatory parameter ***")
            return true
        }

        if (batchParams.paramDateTo.trim().equals("")){
            info("Date Occ To is Mandatory parameter")
            reportA.write("")
            reportA.write("*** ERROR: Date Occ To is Mandatory parameter ***")
            return true
        }

        return false
    }

    /**
     * Get Incident Injuries for all employee.
     */
    private void mainProcess(){
        info("mainProcess")
        int recordCount514 = 0
        Constraint c1 = MSF514Key.personData.like("E%")
        def query = new QueryImpl(MSF514Rec.class).and(c1)

        edoi.search(query,{MSF514Rec msf514rec ->
            recordCount514++
            int length = msf514rec.getPrimaryKey().getPersonData().length()
            String employeeId = msf514rec.getPrimaryKey().getPersonData().substring(1,length)
            String incidentNo = msf514rec.getPrimaryKey().getIncidentNo()
            String natureInjury = msf514rec.getPrimaryKey().getNatureInjury()
            boolean paramMatch = testParameters(employeeId)
            if (paramMatch){
                daysLost = 0
                hoursLost = 0
                boolean valid = checkIncident(incidentNo)
                if (valid){
                    boolean found = obtainStdhrs(employeeId)
                    if (found){
                        getClaim(incidentNo, employeeId)
                        writeSortRecord(employeeId, incidentNo, natureInjury)
                    }
                }
            }
        })

    }

    /**
     * This function apply filtering to the data based on
     * pay location, work group, employee Id, and PRC code
     * @param employeeId is Employee Id
     * @return true if data is valid 
     */
    private boolean testParameters(String employeeId){
        info("testParameter")

        //Validate Pay Location
        if (!batchParams.paramPayLocation.trim().equals("")){
            try{
                MSF820Rec msf820rec = edoi.findByPrimaryKey(new MSF820Key(employeeId))
                if (!msf820rec.getPayLocation().equals(batchParams.paramPayLocation)){
                    return false
                }
            }
            catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
                return false
            }
        }

        //Validate Work Group
        boolean checkWorkGroup = false
        for (int i=0;i<10;i++){
            if (!paramWorkGroups[i].trim().equals("")){
                checkWorkGroup = true
            }
        }

        if (checkWorkGroup){
            boolean found = false
            Constraint c1 = MSF723Key.rec_723Type.equalTo("W")
            Constraint c2 = MSF723Key.employeeId.equalTo(employeeId)
            Constraint c3 = MSF723Rec.stopDtRevsd.equalTo("00000000")

            def query = new QueryImpl(MSF723Rec.class).and(c1.and(c2).and(c3))
            edoi.search(query,{MSF723Rec msf723rec ->
                int i = 0
                while (!found && i<10){
                    if (!paramWorkGroups[i].trim().equals("") && (paramWorkGroups[i].equals(msf723rec.getPrimaryKey().getWorkGroup()))){
                        found = true
                    }
                    i++
                }
            })

            if(!found){
                return false
            }

        }

        //Validate Employee
        boolean checkEmployee = false
        for (int i=0;i<10;i++){
            if (!paramEmployeeIds[i].trim().equals("")){
                checkEmployee = true
            }
        }

        if (checkEmployee){
            boolean found = false
            int i = 0
            while (!found && i<10){
                if (!paramEmployeeIds[i].trim().equals("") && paramEmployeeIds[i].equals(employeeId)){
                    found = true
                }
                i++
            }

            if(!found){
                return false
            }
        }

        //Validate PRC
        if (!batchParams.paramReportingCode.trim().equals("")){
            info("validate PRC")
            Constraint c1 = MSF878Key.employeeId.equalTo(employeeId)
            Constraint c2 = MSF878Key.primaryPos.equalTo("0")
            def query = new QueryImpl(MSF878Rec.class).and(c1.and(c2))
            MSF878Rec msf878rec = (MSF878Rec) edoi.firstRow(query)
            if (msf878rec){
                try{
                    MSF870Rec msf870rec = edoi.findByPrimaryKey(new MSF870Key(msf878rec.getPrimaryKey().getPositionId()))
                    int length = batchParams.paramReportingCode.trim().length()
                    int noOfLoops = Math.ceil(length/4)
                    int endString
                    for (int i=1;i<=noOfLoops;i++){
                        if ((i*4)<length){
                            endString = (i*4)-1
                        } else{
                            endString =  length - 1
                        }
                        if (!msf870rec.getPrimRptGrp().substring((i-1)*4,endString).equals(batchParams.paramReportingCode.substring((i-1)*4,endString))){
                            if (!batchParams.paramReportingCode.substring((i-1)*4,endString).trim().equals("")){
                                return false
                            }
                        }
                    }
                }
                catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
                    return false
                }
            }
        }

        return true
    }

    /**
     * This function apply filtering to the data based on accident occurrence date 
     * @param incidentNo is Incident Number
     * @return true if data is valid
     */
    private boolean checkIncident(String incidentNo){
        info("checkIncident")
        Constraint c1 = MSF510Key.incidentNo.equalTo(incidentNo)
        def query = new QueryImpl(MSF510Rec.class).and(c1)
        MSF510Rec msf510rec = (MSF510Rec) edoi.firstRow(query)
        if (msf510rec){
            int date = 99999999 - msf510rec.getRevOccDate().toInteger()
            if (date >= batchParams.paramDateFrom.toInteger() && date <= batchParams.paramDateTo.toInteger()){
                revOccDate = msf510rec.getRevOccDate()
                wcLoc = msf510rec.getWcLocationCode()
                incidentDesc = msf510rec.getIncidentDesc().trim()
                return true
            }
        }
        return false
    }

    /**
     * This function obtain standard Hours for employee
     * @param employeeId is employeeId
     * @return true if standard hours for employee exist
     */
    private boolean obtainStdhrs(String employeeId){
        info("obtainStdhrs")
        Constraint c1 = MSF828Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF828Rec.endDate.equalTo("00000000")
        def query = new QueryImpl(MSF828Rec.class).and(c1.and(c2))
        MSF828Rec msf828rec = (MSF828Rec) edoi.firstRow(query)
        if (msf828rec){
            MSF801_C0_801Rec msf801_c0_801rec = edoi.findByPrimaryKey(new MSF801_C0_801Key("C0",msf828rec.getAwardCode()))
            BigDecimal convUnitsInt = (long) (msf801_c0_801rec.getStdHrsDayC0() / 1)
            BigDecimal convUnitsDec = msf801_c0_801rec.getStdHrsDayC0() - convUnitsInt
            stdHrsDay = (convUnitsDec * 100) / 60
            stdHrsDay = stdHrsDay + convUnitsInt
            return true
        } else{
            return false
        }
    }

    /**
     * Get Claim Record File.
     */
    private void getClaim(String incidentNo, String employeeId){
        info ("getClaim")
        Constraint c1 = MSF536Rec.incidentNo.equalTo(incidentNo)
        def query = new QueryImpl(MSF536Rec.class).and(c1)
        edoi.search(query,{MSF536Rec msf536rec ->
            getClaimAbsence(msf536rec.getPrimaryKey().getClaimNo(), employeeId)
        })

    }

    /**
     * Get Claim Absence Record.
     */
    private void getClaimAbsence(String claimNo, String employeeId){
        info("getClaimAbsence")
        int ccyymmddFr
        String ccyymmddTo
        Constraint c1 = MSF53ARec.claimNo.equalTo(claimNo)
        def query = new QueryImpl(MSF53ARec.class).and(c1)
        edoi.search(query,{MSF53ARec msf53arec ->
            ccyymmddFr = 99999999 - msf53arec.getPrimaryKey().getInvStrDate().toInteger()
            ccyymmddTo = msf53arec.getEndDate()
            getEmpHistory(ccyymmddFr.toString(), ccyymmddTo, employeeId)
            checkUnits()
        })
    }

    /**
     * Get Employee History Record.
     */
    private void getEmpHistory(String ccyymmddFr, String ccyymmddTo, String employeeId){
        info("getEmpHistory")
        boolean saveDate = true
        String trnDate
        String workDate
        units = 0
        if (batchParams.paramDateTo.toInteger()<=0){
            workDate = "99999999"
        } else{
            workDate = batchParams.paramDateTo
        }

        Constraint c1 = MSF893Key.employeeId.equalTo(employeeId)
        Constraint c2 = MSF893Key.trnDate.greaterThanEqualTo(ccyymmddFr)
        Constraint c3 = MSF893Key.trnDate.lessThanEqualTo(ccyymmddTo)
        Constraint c4 = MSF893Key.trnDate.greaterThanEqualTo(batchParams.paramDateFrom)
        Constraint c5 = MSF893Key.trnDate.lessThanEqualTo(workDate)
        def query = new QueryImpl(MSF893Rec.class).and(c1.and(c2).and(c3).and(c4).and(c5))
        edoi.search(query,{MSF893Rec msf893rec ->
            String workCode = msf893rec.getWorkCode()
            if (validateWorkCode(workCode)){
                if (saveDate){
                    saveDate = false
                    trnDate = msf893rec.getPrimaryKey().getTrnDate()
                }

                if (!trnDate.equals(msf893rec.getPrimaryKey().getTrnDate())){
                    checkUnits()
                    trnDate = msf893rec.getPrimaryKey().getTrnDate()
                    units = 0
                }
                BigDecimal convValueint = (long) (msf893rec.getUnits() / 1)
                BigDecimal convUnitsDec = msf893rec.getUnits() - convValueint
                BigDecimal convValue = (convUnitsDec * 100) / 60
                convValue = convValue + convValueint
                units = units + convValue
                hoursLost = hoursLost + convValue
				
				info("convValueint ${convValueint}")
                info("convUnitsDec ${convUnitsDec}")
                info("convValue ${convValue}")
                info("hoursLost ${hoursLost}")
            }

        })

    }

    /**
     * This function validate work code
     * @param workCode is the work code to be validate
     * @return true if work code is valid
     */
    boolean validateWorkCode(String workCode) {
        info("validateWorkCode")
        //        String[] VALID_WORK_CODE = [
        //            "FL",
        //            "FM",
        //            "W2",
        //            "W3",
        //            "W4",
        //            "W5",
        //            "W6",
        //            "W7",
        //            "W8",
        //            "W9",
        //            "Z2",
        //            "Z3",
        //            "Z4",
        //            "Z5",
        //            "Z6",
        //            "Z7",
        //            "Z8",
        //            "Z9"
        //        ]
        //        boolean found = false
        //        VALID_WORK_CODE.each {
        //            if(StringUtils.equals(it, workCode)) {
        //                found = true
        //            }
        //        }

        boolean found = false
        try {
            MSF801_R1_801Rec msf801_r1_801rec = edoi.findByPrimaryKey(
                    new MSF801_R1_801Key(cntlRecType:"R1", cntlKeyRest:"***${workCode?.trim()}"))
            found = msf801_r1_801rec.getWorkCompR1()?.trim().equalsIgnoreCase("y")
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("Invalid Work Code : " + workCode)
        }
        return found
    }

    /**
     * Check units.
     */
    private void checkUnits(){
        info ("checkUnits")
        if (units >= stdHrsDay){
            daysLost++
        }
    }

    /**
     * Write report line to array list
     * The array list will be used for data sorting
     */
    private void writeSortRecord(String employeeId, String incidentNo, String natureInjury){
        info("writeSortRecord")
        reportLine = new Trb51baLine(" "," "," "," "," "," ",0,0)
        reportLine.setRevOccDate(revOccDate)
        reportLine.setWcLoc(wcLoc)
        reportLine.setEmployeeId(employeeId)
        reportLine.setIncidentNo(incidentNo)
        reportLine.setNatureInjury(natureInjury)
        reportLine.setIncidentDesc(incidentDesc)
        reportLine.setDaysLost(daysLost)
        reportLine.setHoursLost(hoursLost)
        arrayOfTrb51baLine.add(reportLine)
    }

    /**
     * Write batch report
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
        if(!arrayOfTrb51baLine.isEmpty()){
            Collections.sort(arrayOfTrb51baLine);
            generateReport()
        }
        reportA.close()
    }

    /**
     * Write report detail line
     */
    private generateReport(){
        info("generateReport")
        DecimalFormat decFormatter = new DecimalFormat("#####0.00")
        BigDecimal accDaysLost = 0
        BigDecimal ltiCtr= 0
        int msf514Ctr = 0
        String incidentDesc
        String writeLine
        for(Trb51baLine currLine: arrayOfTrb51baLine){
            writeLine = ""
            MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(currLine.getEmployeeId()))
            String surname = msf810rec.getSurname() + " " + msf810rec.getFirstName().substring(0,1)
            MSF010Rec msf010rec = edoi.findByPrimaryKey(new MSF010Key("OHIC",currLine.getNatureInjury()))
            String natInjury = msf010rec.getTableDesc().trim()
            writeLine = writeLine + currLine.getWcLoc().padRight(8)
            writeLine = writeLine + currLine.getEmployeeId().substring(5,10).padRight(6)
            writeLine = writeLine + surname.padRight(21)
            String date = (99999999 - currLine.getRevOccDate().toInteger()).toString()
            String incDate =  date.substring(6,8) + "/" + date.substring(4,6) + "/" + date.substring(0,4)
            writeLine = writeLine + incDate.padRight(11)
            writeLine = writeLine + currLine.getIncidentNo().padRight(11)

            if(natInjury.length()>21){
                writeLine = writeLine + natInjury.substring(0,20).padRight(22)
            } else{
                writeLine = writeLine + natInjury.padRight(22)
            }

            if (currLine.getIncidentDesc().length() > 24){
                incidentDesc = currLine.getIncidentDesc().substring(0,23)
            } else{
                incidentDesc = currLine.getIncidentDesc()
            }

            writeLine = writeLine + incidentDesc.padRight(26)
            if (currLine.getDaysLost() > 0){
                ltiCtr++
                accDaysLost = accDaysLost + currLine.getDaysLost()
                writeLine = writeLine + "Y   "
            } else{
                writeLine = writeLine + "N   "
            }
            writeLine = writeLine + currLine.getDaysLost().toString().padLeft(6)
            writeLine = writeLine + (decFormatter.format(currLine.getHoursLost())).padLeft(9)
            reportA.write(writeLine)
            msf514Ctr++
        }
        generateFooting(ltiCtr,accDaysLost,msf514Ctr)
    }

    /**
     * Write report footing
     */
    private void generateFooting(BigDecimal ltiCtr, BigDecimal accDaysLost, int msf541Ctr){
        info("generateFooting")
        DecimalFormat decFormatter = new DecimalFormat("###,###,##0")
        reportA.writeLine(132,"-")
        reportA.write(" ".padRight(40) + "Total No. of LTI Records :  ".padRight(28) + (decFormatter.format(ltiCtr)).padLeft(11))
        reportA.write(" ".padRight(40) + "Total Days Lost          :  ".padRight(28) + (decFormatter.format(accDaysLost)).padLeft(11))
        reportA.write(" ".padRight(40) + "Total No. of Records     :  ".padRight(28) + (decFormatter.format(msf541Ctr)).padLeft(11))
    }

}

/*run script*/  
ProcessTrb51b process = new ProcessTrb51b();
process.runBatch(binding);