/*
 @Ventyx 2012
 *  Conversion from Trb878.groovy
 *
 * This program can list users who can access a program. <br>
 */
package com.mincom.ellipse.script.custom;

import java.util.regex.Matcher;

import com.mincom.ellipse.edoi.ejb.msf020.MSF020Key;
import com.mincom.ellipse.edoi.ejb.msf020.MSF020Rec;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Key;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Rec;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Key;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Rec;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;

public class ParamsTrb878{
    //List of Input Parameters
    String paramProg;
    String paramPpos;
    String paramPrc;
    String paramPsd;
}

public class ProcessTrb878 extends SuperBatch {

    private version = 5;
    private ParamsTrb878 batchParams;

    private String charValue020P;
    private Integer charPos020P;
    private String district;

    private boolean isAbort=false;

    private def reportA;
    private def csvReport;
    private File csvFile
    private String workDir = ""
    private String oFilePath = ""

    private static final String reportTitle = "                                       List of Users with access to Specific Program ";
    private static final String csvHeader="Position-Id,Emp-Id,Glob-Prof,First-Name,Surname,Pos-Title,Prim-Pos,Pos-Start-Date,Pos-Stop-Date,Program";
    private static final String reportHeader= "Pos-Id".padRight(11)+"Emp-Id".padRight(11)+"Global-Profile".padRight(15)+"First-Name".padRight(15)+"Surname".padRight(16)+"Position-Title".padRight(41)+"Pri".padRight(4)+"Pos-Start".padRight(10)+"Stop-Date".padRight(9);

    private ArrayList<Trb878Record> arrayOfTrb878Record = new ArrayList<Trb878Record>();
    private ArrayList<String> checkedProfile = new ArrayList<String>();

    private class Trb878Record implements Comparable<Trb878Record>{
        private String positionId;
        private String employeeId;
        private String glPro;
        private String poTi;
        private String psd;
        private String invd;
        private String first;
        private String surnm;
        private String priPos;
        private String prog;
        private String srtPosId;

        public Trb878Record(){
            this.positionId=" ";
            this.employeeId=" ";
            this.glPro=" ";
            this.poTi=" ";
            this.psd=" ";
            this.invd=" ";
            this.first=" ";
            this.surnm=" ";
            this.priPos=" ";
            this.prog=" ";
            this.srtPosId=" ";
        }

        public void setPositionId(String positionId){
            this.positionId = positionId.padRight(10);
            checkPositionId();
        }

        public String getPositionId(){
            return positionId;
        }

        public void setEmployeeId(String employeeId){
            this.employeeId = employeeId;
        }

        public String getEmployeeId(){
            String returnString;
            if(employeeId.isNumber()){
                returnString = employeeId.padLeft(10,"0").substring(5,10);
            }
            else{
                returnString = employeeId;
            }
            if (returnString.length()>5){
                returnString = returnString.substring(0, 5);
            }
            return returnString;
        }

        public void setGlPro(String glPro){
            this.glPro = glPro;
        }

        public String getGlPro(){
            return glPro;
        }

        public void setPoTi(String poTi){
            if(poTi.length()>40){
                this.poTi = poTi.substring(0,40);
            }
            else{
                this.poTi = poTi;
            }
        }

        public String getPoTi(){
            return poTi;
        }

        public void setPsd(String psd){
            if(!psd.trim().equals("00000000")){
                this.psd = getFormattedDate(psd);
            }
        }

        public String getPsd(){
            return psd;
        }

        public void setInvd(String invd){
            this.invd = getFormattedDate(getNormalDate(invd));
        }

        public String getInvd(){
            return invd;
        }

        public void setFirst(String first){
            if(first.length()>14){
                this.first = first.substring(0,14);
            }
            else{
                this.first = first;
            }
        }

        public String getFirst(){
            return first;
        }

        public void setSurnm(String surnm){
            if(surnm.length()>15){
                this.surnm = surnm.substring(0,15);
            }
            else{
                this.surnm = surnm;
            }
        }

        public String getSurnm(){
            return surnm;
        }

        public void setPriPos(String priPos){
            this.priPos = priPos;
        }

        public String getPriPos(){
            return priPos;
        }

        public void setProg(String prog){
            this.prog = prog;
        }

        public String getProg(){
            return prog;
        }

        public String getSrtPosId(){
            return srtPosId;
        }

        int compareTo(Trb878Record otherRecord){
            if (!srtPosId.equals(otherRecord.getSrtPosId())){
                return srtPosId.compareTo(otherRecord.getSrtPosId())
            }
            if (!employeeId.equals(otherRecord.getEmployeeId())){
                return employeeId.compareTo(otherRecord.getEmployeeId())
            }
            if (!priPos.equals(otherRecord.getPriPos())){
                return priPos.compareTo(otherRecord.getPriPos())
            }
            if (!glPro.equals(otherRecord.getGlPro())){
                return glPro.compareTo(otherRecord.getGlPro())
            }
            if (!psd.equals(otherRecord.getPsd())){
                return psd.compareTo(otherRecord.getPsd())
            }
            return 0;
        }

        public String toCSV(){
            return (positionId.trim()+","+getEmployeeId().trim()+","+glPro.trim()+","+first.trim()+","+surnm.trim()+","+poTi.trim()+","+priPos.trim()+","+invd.trim()+","+psd.trim()+","+prog.trim());
        }

        public String toString(){
            return (positionId.trim().padRight(10)+" "+getEmployeeId().trim().padRight(5)+"      "+glPro.trim().padRight(10)+"     "+first.trim().padRight(14)+" "+surnm.trim().padRight(15)+" "+poTi.trim().padRight(40)+" "+priPos.trim().padRight(2)+"  "+invd.trim().padRight(8)+"  "+psd.trim().padRight(8));
        }

        private void checkPositionId(){
            String checkPosId = this.positionId;
            Matcher matcher = (checkPosId =~ / /);
            checkPosId = matcher.replaceAll("0");

            if(!checkPosId.isNumber()){
                this.srtPosId=this.positionId;
                return;
            }

            matcher = (this.positionId =~ / /);
            checkPosId = matcher.replaceAll("");
            this.srtPosId = checkPosId.padLeft(10,"0");
        }

        private String getNormalDate(String reversedDate){
            return (99999999-reversedDate.toLong()).toString();
        }

        private String getFormattedDate(String inputDate){
            return (inputDate.substring(6, 8)+"/"+inputDate.substring(4, 6)+"/"+inputDate.substring(2, 4));
        }
    }

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb878())
        //PrintRequest Parameters
        info("paramProg: " + batchParams.paramProg)
        info("paramPpos: " + batchParams.paramPpos)
        info("paramPrc: " + batchParams.paramPrc)
        info("paramPsd: " + batchParams.paramPsd)


        try {
            processBatch();
        }
        catch(Exception e){
            isAbort = true;
            info(e.message)
        }
        finally {
            if(!isAbort){
                printBatchReport();
            }

            reportA.close();
            csvReport.close();
            if(taskUUID?.trim()) {
                info("Adding CSV into Request.")
                request.request.CURRENT.get().addOutput(csvFile,
                        "text/comma-separated-values", "TRT878");
            }
        }

    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch");
        //write process
        initialise();
        mainProcess();
        info("Before sort " + arrayOfTrb878Record.size())
        Collections.sort(arrayOfTrb878Record);
        info("After sort " + arrayOfTrb878Record.size())
    }

    //additional method - start from here.

    private void initialise(){
        info("initialise");
        reportA = report.open("TRB878A");
        reportA.write(reportTitle+batchParams.paramProg);
        reportA.write(" ");
        reportA.write(" ");
        reportA.write(reportHeader)
        reportA.write("".padRight(132,"-"));

        workDir = env.getWorkDir().toString() + "/"
        oFilePath = workDir +"TRT878"
        if(taskUUID?.trim()) {
            oFilePath = oFilePath + "." + taskUUID
        }
        oFilePath = oFilePath + ".csv"
        csvFile = new File(oFilePath)
        csvReport = new BufferedWriter(new FileWriter(csvFile));

        csvReport.write(csvHeader+"\n");
    }

    private void mainProcess(){
        info("mainProcess");

        if(checkProgramProfile()){
            findGlobalProfile();
        }
        else{
            isAbort = true;
        }

    }

    /**
     * Populate Physical Location for after Payroll.
     * @param prc as String
     * @return array of prc
     */
    private String[] getParamPrcTab(String prc){
        info("getParamPrcTab")
        String tempString = prc.padRight(20);
        def tempArray = new String[5];

        int stringIdx = 0;
        int arrayIdx = 0;
        while(arrayIdx<5){
            tempArray[arrayIdx] = (tempString.substring(stringIdx,stringIdx+4));
            stringIdx=stringIdx+4;
            arrayIdx++;
        }

        return tempArray;
    }

    /**
     * Check program security profile
     * @return boolean
     */
    private boolean checkProgramProfile(){
        info("checkProgramProfile")
        try{
            MSF020Key msf020Key = new MSF020Key();
            msf020Key.setEntryType("P");
            msf020Key.setEntity(batchParams.paramProg);
            msf020Key.setDstrctCode(" ");
            MSF020Rec msf020Rec = edoi.findByPrimaryKey(msf020Key);
            String profile020P = msf020Rec.getProfile().padRight(250);

            charValue020P = " ";
            charPos020P = -1;

            String tempChar;
            for(i in 0..249){
                tempChar=profile020P.substring(i, i+1);
                if(Character.isDigit(tempChar.toCharacter())){
                    if(tempChar.toInteger()>0 && tempChar.toInteger()!=9){
                        charValue020P = tempChar;
                        charPos020P = i;
                    }
                }
            }

            return true;
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            return false;
        }
    }

    /**
     * Search for global profile
     */
    private void findGlobalProfile(){
        info("findGlobalProfile");
        Constraint cEntryType = MSF020Key.entryType.equalTo("G");
        Constraint cEntity    = MSF020Key.entity.greaterThanEqualTo(" ");
        def query = new QueryImpl(MSF020Rec.class).and(cEntryType).and(cEntity);
        edoi.search(query,{MSF020Rec msf020Rec->
            if(profilePosValue(msf020Rec.getProfile())){
                findPositionId(msf020Rec.getPrimaryKey().getEntity())
            }
        });
    }

    /**
     * Check global security profile
     * @return boolean
     */
    private boolean profilePosValue(String profile020G){
        info("profilePosValue")
        boolean returnValue = false
        if(charPos020P>=0){
            profile020G = profile020G.padRight(250)
            String tempChar = profile020G.substring(charPos020P, charPos020P+1);
            if(!tempChar.isNumber()) {
                tempChar = " "
            }
            if(profile020G.substring(0, 1).equals("9")||tempChar.compareTo(charValue020P) >= 0){
                returnValue = true;
            }
        }

        return returnValue;
    }

    /**
     * Find position id
     * @param globalProfile as String
     */
    private void findPositionId(String globalProfile){
        info("findPositionId ${globalProfile}");
        info("checkedProfile.contains(globalProfile) ${checkedProfile.contains(globalProfile)}")
        if(!checkedProfile.contains(globalProfile)) {
            checkedProfile.add(globalProfile)
            Constraint cGlobalProfile = MSF870Rec.globalProfile.equalTo(globalProfile);
            Constraint cOccupStatus = MSF870Rec.occupStatus.notEqualTo("D");
            def query = new QueryImpl(MSF870Rec.class).and(cGlobalProfile.and(cOccupStatus));
            edoi.search(query,{MSF870Rec msf870Rec->
                findEmployeeId(msf870Rec);
            })
        }
    }

    /**
     * Find employee id
     * @param msf870Rec as MSF870 record
     */
    private void findEmployeeId(MSF870Rec msf870Rec){
        info("findEmployeeId ${msf870Rec.getPrimaryKey().getPositionId()}");
        Constraint cPositionId = MSF878Key.positionId.equalTo(msf870Rec.getPrimaryKey().getPositionId());
        def query;
        if(batchParams.paramPpos?.trim()){
            Constraint cPrimaryPos = MSF878Key.primaryPos.equalTo(batchParams.paramPpos);
            query = new QueryImpl(MSF878Rec.class).and(cPositionId.and(cPrimaryPos));
        }
        else{
            query = new QueryImpl(MSF878Rec.class).and(cPositionId);
        }

        edoi.search(query, {MSF878Rec msf878Rec->
            if(!batchParams.paramPsd?.trim() || msf878Rec.getPrimaryKey().getPosStopDate().toBigInteger()<=0
            || msf878Rec.getPrimaryKey().getPosStopDate().toBigInteger()>=batchParams.paramPsd.toBigInteger()){
                validateEmployee(msf870Rec, msf878Rec);
            }
        })
    }

    /**
     * Validate active employee id
     * @param msf870Rec as MSF870 record
     * @param msf878Rec as MSF878 record
     */
    private void validateEmployee(MSF870Rec msf870Rec, MSF878Rec msf878Rec){
        info("validateEmployee ${msf878Rec.getPrimaryKey().getEmployeeId()}");
        try{
            MSF760Key msf760Key = new MSF760Key();
            msf760Key.setEmployeeId(msf878Rec.getPrimaryKey().getEmployeeId())
            MSF760Rec msf760Rec = edoi.findByPrimaryKey(msf760Key);
            if(msf760Rec.getEmpStatus().trim().equals("A")){
                if(!batchParams.paramPrc?.trim()||validatePRC(msf878Rec.getPrimaryKey().getEmployeeId())){
                    MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(msf878Rec.getPrimaryKey().getEmployeeId()));
                    writeRecord(msf810Rec, msf870Rec, msf878Rec);
                }
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){

        }

    }

    /**
     * Validate employee PRC
     * @param msf878EmployeeId as String
     * @return boolean
     */
    private boolean validatePRC(String msf878EmployeeId){
        info("validatePRC");
        boolean returnValue = true;
        try{
            MSF820Key msf820Key = new MSF820Key();
            msf820Key.setEmployeeId(msf878EmployeeId);
            MSF820Rec msf820Rec = edoi.findByPrimaryKey(msf820Key);
            String[] prcTab = getParamPrcTab(msf820Rec.getRptPrc());
            String[] paramPrcTab = getParamPrcTab(batchParams.paramPrc);

            if(batchParams.paramPrc?.trim()){
                Integer index = 0;
                boolean prcNotFound = false;
                while(index<5 && !prcNotFound && paramPrcTab[index]?.trim()){
                    if(!paramPrcTab[index].trim().equals(prcTab[index].trim())){
                        prcNotFound=true;
                    }
                    index++;
                }
                if(prcNotFound){
                    returnValue = false;
                }
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            returnValue=false;
        }

        return returnValue;
    }

    /**
     * Add record to collection
     * @param msf810Rec as MSF810 record
     * @param msf870Rec as MSF870 record
     * @param msf878Rec as MSF878 record
     */
    private void writeRecord(MSF810Rec msf810Rec, MSF870Rec msf870Rec, MSF878Rec msf878Rec){
        info("writeRecord");
        Trb878Record element = new Trb878Record();
        element.setFirst(msf810Rec.getFirstName());
        element.setSurnm(msf810Rec.getSurname());
        element.setPriPos(msf878Rec.getPrimaryKey().getPrimaryPos());
        element.setInvd(msf878Rec.getPrimaryKey().getInvStrDate());
        element.setPsd(msf878Rec.getPrimaryKey().getPosStopDate());
        element.setPositionId(msf870Rec.getPrimaryKey().getPositionId());
        element.setEmployeeId(msf878Rec.getPrimaryKey().getEmployeeId());
        element.setGlPro(msf870Rec.getGlobalProfile());
        element.setPoTi(msf870Rec.getPosTitle());
        element.setProg(batchParams.paramProg);
        arrayOfTrb878Record.add(element);
    }

    /**
     * Write collection to report
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report

        for(record in arrayOfTrb878Record){
            reportA.write(record.toString())
            csvReport.write(record.toCSV()+"\n")
        }


    }
}

/*run script*/
ProcessTrb878 process = new ProcessTrb878();
process.runBatch(binding);