/**
 *  @Ventyx 2012
 *  
 * This program extracts the work order, 
 * that not matching costing solution. <br>
 * 
 * Developed based on <b>FDD-Online Maintenance Costing Solution V01.D01.docx</b>
 */
package com.mincom.ellipse.script.custom;

import com.mincom.batch.environment.BatchEnvironment
import com.mincom.batch.script.*
import com.mincom.ellipse.edoi.ejb.msf010.*
import com.mincom.ellipse.edoi.ejb.msf011.*
import com.mincom.ellipse.edoi.ejb.msf096.*
import com.mincom.ellipse.edoi.ejb.msf600.*
import com.mincom.ellipse.edoi.ejb.msf601.*
import com.mincom.ellipse.edoi.ejb.msf602.*
import com.mincom.ellipse.edoi.ejb.msf60a.*
import com.mincom.ellipse.edoi.ejb.msf619.*
import com.mincom.ellipse.edoi.ejb.msf620.*
import com.mincom.ellipse.edoi.ejb.msf650.*
import com.mincom.ellipse.edoi.ejb.msf655.*
import com.mincom.ellipse.edoi.ejb.msf656.*
import com.mincom.ellipse.edoi.ejb.msf660.*
import com.mincom.ellipse.edoi.ejb.msf661.*
import com.mincom.ellipse.edoi.ejb.msf662.*
import com.mincom.ellipse.edoi.ejb.msf664.*
import com.mincom.ellipse.edoi.ejb.msf665.*
import com.mincom.ellipse.edoi.ejb.msf666.*
import com.mincom.ellipse.edoi.ejb.msf667.*
import com.mincom.ellipse.edoi.ejb.msf668.*
import com.mincom.ellipse.edoi.ejb.msf680.*
import com.mincom.ellipse.edoi.ejb.msf685.*
import com.mincom.ellipse.edoi.ejb.msf6a1.*
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec
import com.mincom.ellipse.edoi.ejb.msf900.*
import com.mincom.ellipse.edoi.ejb.msf910.*
import com.mincom.ellipse.edoi.ejb.msf920.*
import com.mincom.ellipse.edoi.ejb.msf930.*
import com.mincom.ellipse.edoi.ejb.msf93f.*
import com.mincom.ellipse.edoi.ejb.msf93i.*
import com.mincom.ellipse.edoi.ejb.msf940.*
import com.mincom.ellipse.edoi.ejb.msf966.*
import com.mincom.ellipse.edoi.ejb.msf967.*
import com.mincom.ellipse.edoi.ejb.msf968.*
import com.mincom.ellipse.edoi.ejb.msf970.*
import com.mincom.ellipse.edoi.ejb.msfx51.*
import com.mincom.ellipse.edoi.ejb.msfx55.*
import com.mincom.ellipse.edoi.ejb.msfx60.*
import com.mincom.ellipse.edoi.ejb.msfx61.*
import com.mincom.ellipse.edoi.ejb.msfx62.*
import com.mincom.ellipse.edoi.ejb.msfx63.*
import com.mincom.ellipse.edoi.ejb.msfx65.*
import com.mincom.ellipse.edoi.ejb.msfx66.*
import com.mincom.ellipse.edoi.ejb.msfx68.*
import com.mincom.ellipse.edoi.ejb.msfx69.*
import com.mincom.ellipse.edoi.ejb.msfx6a.*
import com.mincom.ellipse.edoi.ejb.msfx6c.*
import com.mincom.ellipse.edoi.ejb.msfx6e.*
import com.mincom.ellipse.edoi.ejb.msfx6f.*
import com.mincom.ellipse.edoi.ejb.msfx6j.*
import com.mincom.ellipse.edoi.ejb.msfx6n.*
import com.mincom.ellipse.edoi.ejb.msfx6s.*
import com.mincom.ellipse.edoi.ejb.msfx6w.*
import com.mincom.ellipse.eroi.linkage.mssemp.*
import com.mincom.ellipse.script.util.*
import com.mincom.eql.*
import com.mincom.eql.impl.*

/**
 * Request Parameter for Trbpw3. <b>This batch process has no parameters.</b>
 */
public class ParamsTrbpw3{
    //List of Input Parameters
    String empty;
}

/**
 * Main Process of Trbpw3
 */
public class ProcessTrbpw3 extends SuperBatch {

    /*
     * Constants
     */
    private static final int MAX_ROW_READ = 1000
    static final String CSV_TRTPW3_FILENAME    = "TRTPW3A"
    //    static final String SERVICE_NAME_EQUIPMENT = "EQUIPMENT"
    static final String TABLE_TYPE_CST         = "+CST"
    //    static final String SERVICE_NAME_PROJECT   = "PROJECT"

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 5;

    /*
     * Variables
     */
    private ParamsTrbpw3 batchParams
    private String wxDistrict
    private boolean writeTrtpw3Header
    private File trtpw3AFile
    private BufferedWriter csvTrtpw3writer


    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrbpw3())

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
        initialize()
        //browse work order
        browseWorkOrder()
    }

    /**
     * Initialize report writer and other variables
     */
    private void initialize(){
        info("initialize")
        wxDistrict = request.getPropertyString("District")
        writeTrtpw3Header = false
        String workingDir = env.workDir
        String trtpw3AFilePath = "${workingDir}/${CSV_TRTPW3_FILENAME}"
        if(taskUUID?.trim()) {
            trtpw3AFilePath = trtpw3AFilePath + "." + taskUUID
        }
        trtpw3AFilePath = trtpw3AFilePath + ".csv"
        trtpw3AFile = new File(trtpw3AFilePath)
        info ("${CSV_TRTPW3_FILENAME} created in ${trtpw3AFile.getAbsolutePath()}")
    }

    /**
     * Browse all work order that closed date field is blank
     * than get the equipment detail for each work order that have equipment number
     */
    private void browseWorkOrder(){
        info("browseWorkOrder")
        //Use edoi to browse work order since the constrain blank closedDt cannot be implemented in service called.
        int countBrws = 0
        Constraint cCloseDate = MSF620Rec.closedDt.equalTo(" ")
        QueryImpl qWorkOrder = new QueryImpl(MSF620Rec.class).and(cCloseDate).orderBy(MSF620Rec.msf620Key)
        try{
            edoi.search(qWorkOrder, MAX_ROW_READ, {MSF620Rec msf620Rec->
                countBrws++
                info ("Records : ${countBrws}")
                info ("Work Order: ${msf620Rec.getPrimaryKey().getWorkOrder()}")
                String msf620EquipNo = msf620Rec.equipNo
                String msf620WorkGroup = msf620Rec.workGroup
                String assoc64 = msf620Rec.getWoType().padRight(2) + msf620Rec.getMaintType().padRight(2)
                if (msf620EquipNo !=null){
                    if (msf620EquipNo.trim().length() != 0){
                        try{
                            //using edoi to read equipment since there is still problem using service call
                            MSF600Key msf600Key = new MSF600Key()
                            msf600Key.equipNo = msf620EquipNo
                            MSF600Rec msf600Rec = getMSF600Record(msf600Key)
                            if (msf600Rec != null){
                                String msf600EquipClassif1 = msf600Rec.getEquipClassifx1()
                                String msf600EquipClassif2 = msf600Rec.getEquipClassifx2()
                                String msf600EquipClassif3 = msf600Rec.getEquipClassifx3()
                                String msf010AssocRec = msf600EquipClassif1.padRight(2) + msf600EquipClassif2.padRight(2) + msf600EquipClassif3.padRight(2) + assoc64.padRight(4) + msf620WorkGroup
                                //get MSF010 Description as project Number
                                getProjectNo(msf010AssocRec, msf620Rec, msf600Rec)
                            }
                            //This features has been changed to edoi since there is problem with ellipse web service
                            //                    ScreenAppLibrary screenAppLib = new ScreenAppLibrary()
                            //                    EquipmentDTO equipDTO = new EquipmentDTO()
                            //                    equipDTO.setEquipNo(msf620EquipNo)
                            //                    EquipmentResultDTO equipReplyDTO = screenAppLib.readEquipmentClassif(equipDTO)
                            //                  msf620EquipNo = "000000064412"
                            //                  cntBrowse++
                            //                  info ("eqipment : ${msf620EquipNo}")
                            //                  info ("counter = ${cntBrowse}")
                            //                  if (!StringOperation.isBlank(msf620EquipNo)){
                            //                      EquipmentServiceReadReplyDTO equipmentReadReply = service.get(SERVICE_NAME_EQUIPMENT).read({EquipmentServiceReadRequestDTO it->
                            //                          it.setEquipmentNo(msf620EquipNo)
                            //                      })
                            //                      if(equipmentReadReply != null){
                            //                          String msf600EquipClassif1 = equipmentReadReply.getEquipmentClassif0()
                            //                          String msf600EquipClassif2 = equipmentReadReply.getEquipmentClassif1()
                            //                          String msf600EquipClassif3 = equipmentReadReply.getEquipmentClassif2()
                            //                          info ("class1 : ${msf600EquipClassif1}")
                            //                          info ("class2 : ${msf600EquipClassif2}")
                            //                          info ("class3 : ${msf600EquipClassif3}")
                            //                          String msf010AssocRec = msf600EquipClassif1.padRight(2) + msf600EquipClassif2.padRight(2) + msf600EquipClassif3.padRight(2) + assoc64.padRight(4) + msf620WorkGroup
                            //                          info ("assocRec ${msf010AssocRec}")
                            //                          //get MSF010 Description as project Number
                            //                          getProjectNo(msf010AssocRec, msf620Rec, equipmentReadReply)
                            //                      }
                            //                  }
                        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                            info("Error at read Equipment ${e.getMessage()}")
                            e.printStackTrace()
                        }
                    }
                }
            })
        }
        catch(Exception exp){
            info("Error when browse Work Order ${exp.getMessage()} in records ${countBrws}")
        }
    }
/**
 * Get MSF600Record by primary key
 * @param msf600Key
 * @return
 */
    private MSF600Rec getMSF600Record(MSF600Key msf600Key) {
        info("getMSF600Record")
        MSF600Rec msf600Rec
        try{
            msf600Rec = edoi.findByPrimaryKey(msf600Key)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            msf600Rec = null
            info("Error message : "+e.message)
        }

        return msf600Rec
    }

    /**
     * get project number from TABLE records description based on assoc value
     * if project number not match with work order project Number than write to report
     * @param msf010AssocRec Associate record msf010
     * @param msf620Rec Work Order Record
     * @param MSF600Rec equipment record
     */

    private void   getProjectNo(String msf010AssocRec,MSF620Rec msf620Rec, MSF600Rec msf600Rec){
        info ("getProjectNo")
        //Use edoi to browse Table since the constraints cannot be implement using service call.
        String sProjectNo = " "
        String wxTodaysDate = new Date().format("yyyyMMdd")
        Constraint cTableType = MSF010Key.tableType.equalTo(TABLE_TYPE_CST)
        Constraint cAssocRec = MSF010Rec.assocRec.equalTo(msf010AssocRec)
        def query = new QueryImpl(MSF010Rec.class).and(cTableType).and(cAssocRec)

        MSF010Rec msf010Rec = (MSF010Rec)edoi.firstRow(query)
        if (msf010Rec != null){
            sProjectNo = msf010Rec.tableDesc
            //Using edoi to get project details since there is problem with ellipse web service
            try{
                MSF660Key msf660Key = new MSF660Key()
                msf660Key.setProjectNo(sProjectNo)
                msf660Key.setDstrctCode(wxDistrict)
                MSF660Rec msf660Rec = edoi.findByPrimaryKey(msf660Key)
                if (msf660Rec != null){
                    if (msf660Rec.getPrimaryKey().getProjectNo() != msf620Rec.getProjectNo()){
                        writeTrbpw3CSV(msf620Rec, msf600Rec, msf660Rec)
                    }
                }
            }
            //            This features has been changed to edoi/eroi since there is problem with ellipse web service
            //                ProjectServiceReadReplyDTO projectReadReply = service.get(SERVICE_NAME_PROJECT).read({ProjectServiceReadRequestDTO it->
            //                    it.setProjectNo(sProjectNo)
            //                })
            //                if (projectReadReply != null){
            //                    String actFinDate = projectReadReply.actualFinDate.format("yyyyMMdd")
            //                    if (msf620Rec.projectNo.notEqualTo(projectReadReply.projectNo)){
            //                        writeTrbpw3CSV(msf620Rec, msf600Rec, projectReadReply)
            //                    }
            //                }
            //            }
            catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                info("Error at read Project ${e.getMessage()}")
            }
        }
    }

    /**
     * Write process records into CSV file.
     * @param msf620Rec
     * MSF600Rec MSF600Rec
     * @param MSF660Rec
     */
    private void writeTrbpw3CSV(MSF620Rec msf620Rec,MSF600Rec msf600Rec,MSF660Rec msf660Rec){
        info ("writeTrbpw3CSV")
        if (!writeTrtpw3Header){
            csvTrtpw3writer = new BufferedWriter(new FileWriter(trtpw3AFile))
            csvTrtpw3writer.write("Work Order,WO Description,Equip No,Plant Structure No,Equip Desc,"+
                    "Reg Category(E0),Customer(E1),Agreement(E2)," +
                    "WO Type, Maint Type, Work Group, WO Project No,RC/BS/AC,\'+CST Project No,RC/BS/AC,Originator")
            csvTrtpw3writer.write("\n")
            writeTrtpw3Header = true
        }
        String sPlantNo = msf600Rec.getPlantNo()
        String sEquipDesc = msf600Rec.getItemName_1() + ";" + msf600Rec.getItemName_2()
        String accountCodeWO = msf620Rec.getDstrctAcctCode().padRight(28)
        String accountCodePR = msf660Rec.getAccountCode().padRight(24)
        String origName = getOriginatorName(msf620Rec.originatorId)
        String sSignEquipClassifx3 = ",\'"
        if (msf600Rec.getEquipClassifx3().trim().equals("")){
            sSignEquipClassifx3 = ","
        }
        String sSignAccountCodeWO = ",\'"
        if (accountCodeWO.substring(4, 13).trim().equals("")){
            sSignAccountCodeWO = ","
        }
        String sSignAccountCodePR = ",\'"
        if (accountCodePR.substring(0, 9).trim().equals("")){
            sSignAccountCodePR = ","
        }
        csvTrtpw3writer.write(msf620Rec.getPrimaryKey().getWorkOrder() + "," +  "\"" + msf620Rec.getWoDesc() + "\"" +
                "," + msf600Rec.getPrimaryKey().getEquipNo() + "," + sPlantNo +
                "," + "\"" + sEquipDesc + "\"" + "," + msf600Rec.getEquipClassifx1() +
                "," + msf600Rec.getEquipClassifx2() + sSignEquipClassifx3 + msf600Rec.getEquipClassifx3() +
                "," + msf620Rec.getWoType() + "," + msf620Rec.getMaintType() +
                "," + msf620Rec.getWorkGroup() + "," + msf620Rec.getProjectNo() +
                sSignAccountCodeWO + accountCodeWO.substring(4, 13) + "," + msf660Rec.getPrimaryKey().getProjectNo() +
                sSignAccountCodePR + accountCodePR.substring(0, 9) +
                "," + "\"" +"(" + msf620Rec.originatorId + ")" + " " + origName.trim() + "\""
                )
        csvTrtpw3writer.write("\n")
    }

    /**
     * get originator Name from work order originator id using employee service 
     * @param originatorId work order originator id
     * @return employee formatted name
     */
    private String getOriginatorName(String originatorId){
        info("getOriginatorName")

        /*
         * Using EDOI for formatted name as approved by Peter Deacon
         * in regards of performance issue when using screen service.
         *
         */
        String origName = " "
        try {
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(originatorId))
            origName = "${msf810Rec.surname.trim()}, ${msf810Rec.firstName.trim()}"
            if(!msf810Rec.secondName.trim().equals("")) {
                origName = "${origName} ${msf810Rec.secondName.trim()}"
            }
            if(!msf810Rec.thirdName.trim().equals("")) {
                origName = "${origName} ${msf810Rec.thirdName.trim()}"
            }
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info ("Error at read MSF810 ${e.getMessage()}")
        }
        return origName;

    }

    /**
     * Print the batch report.
     */
    private void printBatchReport(){
        info("printBatchReport")
        if (csvTrtpw3writer != null){
            csvTrtpw3writer.close()
            if(taskUUID?.trim()) {
                info("Adding csv into Request.")
                request.request.CURRENT.get().addOutput(trtpw3AFile,
                        "text/comma-separated-values", CSV_TRTPW3_FILENAME);
            }
        }
    }
}

/*run script*/  
ProcessTrbpw3 process = new ProcessTrbpw3();
process.runBatch(binding);