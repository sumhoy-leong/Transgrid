/**
 *  @Ventyx 2012
 *
 *  This program will extract all the prior period depreciation transactions from
 *  the Fixed Asset vehicle assets.
 */
package com.mincom.ellipse.script.custom

import java.io.File;
import java.io.Writer;

import com.mincom.ellipse.edoi.ejb.msf009.MSF009_DC0059Key;
import com.mincom.ellipse.edoi.ejb.msf009.MSF009_DC0059Rec;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_CPKey;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_CPRec;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_PCYYMMKey;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_PCYYMMRec;
import com.mincom.ellipse.edoi.ejb.msf600.MSF600Key;
import com.mincom.ellipse.edoi.ejb.msf600.MSF600Rec;
import com.mincom.ellipse.edoi.ejb.msf686.MSF686Key;
import com.mincom.ellipse.edoi.ejb.msf686.MSF686Rec;
import com.mincom.ellipse.edoi.ejb.msf687.MSF687Key;
import com.mincom.ellipse.edoi.ejb.msf687.MSF687Rec;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveReplyDTO;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;


public class ProcessTrb68f extends SuperBatch {


    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 7;

    private static final String REPORT_TITLE = "Fleet Asset Extract Control Report"
    private static final String REPORT_DISTRICT = "Fixed Asset District: "
    private static final String REPORT_PERIOD = "Accounting Period: "
    private static final String REPORT_REC_EXTRACT = "No. of records extracted: "
    private static final String REPORT_CONFIG_ERROR= "Configuration Errors"
    private static final String SERVICE_CALL_TABLE = "TABLE"
    private static final String MSF010_TABLE_TYPE_FLD = "#FLD"
    private static final String MSF009_REC_TY = "DC"
    private static final String MSF009_REC_NO = "0059"
    private static final String MSF000_REC_TY_CP = "CP"
    private static final String MSF000_REC_NO = "0005"
    private static final String MSF000_REC_TY_PC = "PC"
    private static final String MSF686_ASSET_TY = "E"
    private static final String MSF687_CAPITALISED = "2"
    private static final String MSF687_RETIRED = "6"
    private static final String MSF687_WRITTEN_DN = "7"
    private static final String MSF686_BOOK_DEPR = "BK"
    private static final String MSF600_EQUIP_CLASS = "FL"
    private static final int SERVICE_CALL_MAX_ROWS = 100

    private int recCount = 0
    private def reportA
    private def outputFile
    private def outputFileCSV
    private File csvFile
    private ArrayList districtCode
    private ArrayList errorLine
    private ArrayList assetTransactionRecs
    private String priorMonth = ""
    private String priorYear = ""
    private String priorCentury = ""

    private class AssetTransaction{
        String assetNumber = ""
        String fleetNumber = ""       
        BigDecimal ccostLCv = 0
        BigDecimal accumDepnLCv = 0
        
        public AssetTransaction (String assetNumber, String fleetNumber, BigDecimal ccostLCv, BigDecimal accumDepnLCv){
            this.assetNumber = assetNumber
            this.fleetNumber = fleetNumber
            this.ccostLCv = ccostLCv
            this.accumDepnLCv = accumDepnLCv
        }                              
    }
    
    private class AssetTransactionComparator implements Comparator<AssetTransaction>{

        @Override
        public int compare(AssetTransaction o1, AssetTransaction o2) {
            // TODO Auto-generated method stub
            return o1.fleetNumber.compareTo(o2.fleetNumber);
        }        
    }
    
    public void runBatch(Binding b){
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);

        //PrintRequest Parameters
        info("TRB68F Has No Request Parameters ")

        try {
            processBatch();
        } finally {
            reportA.close()
            outputFileCSV.close()
            if (taskUUID?.trim()) {
                info("Adding CSV into Request.")
                request.request.CURRENT.get().addOutput(csvFile,
                        "text/comma-separated-values", "TRT68F");
            }
            outputFile.close()
            
        }
    }

    private void processBatch(){
        info("processBatch");

        String district = ""
        String farDistrict = ""
        ArrayList processedDistrict = new ArrayList()

        //Initialise
        initialise();

        retrieveDistrict()

        Iterator itr = districtCode.iterator()
        while(itr.hasNext()){
            district = itr.next()
            errorLine = new ArrayList()
            farDistrict = getFarDistrict(district)

            if(!processedDistrict.contains(farDistrict)){
                if(farDistrict?.trim()){
                    processedDistrict.add(farDistrict)
                    extractAsset(farDistrict)
                }
                writeControlReport(farDistrict)
            }
        }
    }

    /**
     * Initialize the CSV writer, report, output file and other variables.
     */
    private void initialise(){
        info("initialise");

        String workDir = env.getWorkDir().toString() + "/"
        String oFilePathCSV = workDir +"TRT68F"
        String oFilePath = workDir +"TRT68F"
        if (taskUUID?.trim()) {
            oFilePathCSV = oFilePathCSV + "." + taskUUID
            oFilePath = oFilePath + "." + taskUUID
        }
        oFilePathCSV = oFilePathCSV + ".csv"
        csvFile = new File(oFilePathCSV)
        outputFileCSV = new FileWriter(csvFile)
        outputFile = new FileWriter(new File(oFilePath))
        reportA = report.open("TRB68FA")
        reportA.write(REPORT_TITLE.center(132))
        reportA.writeLine(132,"-")

        districtCode = new ArrayList()
    }

    /**
     * Obtain the list of Districts identified in the #FLD Table Codes.
     */
    private void retrieveDistrict(){
        info("retrieveDistrict")

        List<TableServiceRetrieveReplyDTO> replies = new ArrayList<TableServiceRetrieveReplyDTO>()
        def serviceGet = service.get(SERVICE_CALL_TABLE)
        def restart = ""

        /*
         * Since groovy does not support do-while then we need to execute retrieve at the first time
         * to get the restart value
         */
        def collectionDTO = serviceGet.retrieve({ Object itrtr ->
            itrtr.setTableType(MSF010_TABLE_TYPE_FLD)
        }, SERVICE_CALL_MAX_ROWS, false, restart)

        restart = collectionDTO.getCollectionRestartPoint()
        replies.addAll(collectionDTO.replyElements)

        /*
         * restart value has been set from above service call, then do a loop while restart value is not blank
         * this loop below will get the rest of the rows
         */
        while(restart?.trim()) {
            collectionDTO = serviceGet.retrieve({ Object itrtr ->
                itrtr.setTableType(MSF010_TABLE_TYPE_FLD)
            }, SERVICE_CALL_MAX_ROWS, false, restart)

            restart = collectionDTO.getCollectionRestartPoint()
            replies.addAll(collectionDTO.replyElements)
        }

        int repliesSize = replies.size()
        for(int i=0; i<repliesSize; i++){
            TableServiceRetrieveReplyDTO dto = replies.get(i)
            if(!districtCode.contains(dto.description.padRight(50).substring(0, 4))){
                districtCode.add(dto.description.padRight(50).substring(0, 4))
            }
        }
    }

    /**
     * Determine Fixed Asset District.
     * @param district as String
     * @return farDistrict
     */
    private String getFarDistrict(String district){
        info("getFarDistrict")

        try{
            MSF009_DC0059Key msf009Key= new MSF009_DC0059Key()
            msf009Key.setDstrctCode(district)
            msf009Key.setCntlRecTy(MSF009_REC_TY)
            msf009Key.setControlRecNo(MSF009_REC_NO)
            MSF009_DC0059Rec msf009Rec = edoi.findByPrimaryKey(msf009Key)
            return msf009Rec.getFarDistrict()
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            errorLine.add("FAR district not found for district code : "+district)
        }

        return " "
    }

    /**
     * Extract fleet asset records.
     * @param farDistrict as String
     */
    private void extractAsset(String farDistrict){
        info("extractAsset")

        recCount = 0
        assetTransactionRecs = new ArrayList()
        //Constraint
        Constraint c1 = MSF686Key.dstrctCode.equalTo("GRID")
        Constraint c2 = MSF686Key.assetTy.equalTo(MSF686_ASSET_TY)
        Constraint c3 = MSF686Key.deprRecType.equalTo(MSF686_BOOK_DEPR)
        Constraint c4 = MSF600Rec.equipClass.equalTo(MSF600_EQUIP_CLASS)
        Constraint c5 = MSF686Rec.subAssetBkStat.notEqualTo("6")
        Constraint c6 = MSF686Rec.subAssetBkStat.notEqualTo("7")
        Constraint c7 = MSF686Rec.ccostLCv.greaterThan(0.0)
        //Query to extract asset transactions
        def assetTransaction = new QueryImpl().
                join(MSF600Key.equipNo,MSF686Key.assetNo).
                columns([MSF600Key.equipNo,MSF600Rec.plantNo,MSF686Rec.ccostLCv,MSF686Rec.accumDepnLCv]).                
                and(c1).and(c2).and(c3).and(c4).and(c5).and(c6).and(c7)
        

        //Extract asset transactions
        edoi.search(assetTransaction).results.each {rec1 ->
            assetTransactionRecs.add(new AssetTransaction(rec1[0],rec1[1],rec1[2],rec1[3]))
            recCount++
        }
        
        if(assetTransactionRecs.size() > 0){
            java.util.Collections.sort(assetTransactionRecs,new AssetTransactionComparator())
            Iterator itr = assetTransactionRecs.iterator()
            while(itr.hasNext()){
                assembleRecord(itr.next())
            }
        }        
    }

    /**
     * Assemble fleet asset record to write.
     * @param assetTransaction as AssetTransaction
     */
    private void assembleRecord(AssetTransaction assetTransaction){
        info("assembleRecord")

        Boolean errorRec = false
        BigInteger tempValue = 0
        String errorLine1 = "Asset number %s length exceeds 10 characters"
        String errorLine2 = "Asset number %s - net book value exceeds limit of 9999999999 or -9999999999"
        String errorLine3 = "Asset number %s - purchase amount exceeds limit of 9999999999 or -9999999999"

        String assetNumber = assetTransaction.getAssetNumber().trim()
        assetNumber = assetNumber.replaceAll("^0*", "")
        String fleetNumber = assetTransaction.getFleetNumber()
        fleetNumber = fleetNumber.padRight(30).substring(2,6)
        BigDecimal ccostLCv = assetTransaction.getCcostLCv()
        BigDecimal accumDepnLCv =  assetTransaction.getAccumDepnLCv()
        BigDecimal netBookValueBC = ccostLCv - accumDepnLCv
        BigInteger netBookValue = netBookValueBC
        String netBookValueS = ""
        BigInteger purchaseAmount = ccostLCv
        String purchaseAmountS = ""

        if(assetNumber.size() > 10){
            errorLine.add(String.format(errorLine1,assetNumber))
            errorRec = true
        }
        if(netBookValue > 9999999999 || netBookValue < -9999999999){
            errorLine.add(String.format(errorLine2,assetNumber))
            errorRec = true
        }
        if(purchaseAmount > 9999999999 || purchaseAmount < -9999999999){
            errorLine.add(String.format(errorLine3,assetNumber))
            errorRec = true
        }

        if(!errorRec){
            assetNumber = assetNumber.padRight(10)

            if(netBookValue < 0 ){
                tempValue = netBookValue * (-1)
                netBookValueS = tempValue.toString().concat("00")
                netBookValueS = netBookValueS.padLeft(10).replace(" ","0")
                if(netBookValueS.startsWith("0")){
                    netBookValueS = netBookValueS.replaceFirst("0", "-")
                }else{
                    netBookValueS = "-".concat(netBookValueS)
                }
            }else{
                tempValue = netBookValue
                netBookValueS = tempValue.toString().concat("00")
                netBookValueS = netBookValueS.padLeft(10).replace(" ","0")
            }
            if(purchaseAmount < 0 ){
                tempValue = purchaseAmount * (-1)
                purchaseAmountS = tempValue.toString().concat("00")
                purchaseAmountS = purchaseAmountS.padLeft(10).replace(" ","0")
                purchaseAmountS = purchaseAmountS.replaceFirst("0", "-")
            }else{
                tempValue = purchaseAmount
                purchaseAmountS = tempValue.toString().concat("00")
                purchaseAmountS = purchaseAmountS.padLeft(10).replace(" ","0")
            }

            writeCSV(fleetNumber,assetNumber,purchaseAmountS,netBookValueS)
            writeOutputFile(fleetNumber,assetNumber,purchaseAmountS,netBookValueS)
        }
    }

    /**
     * Write fleet asset record to output file.
     * @param fleetNumber String
     * @param assetNumber String
     * @param netBookValueS String
     * @param purchaseAmountS String
     */
    private void writeOutputFile(String fleetNumber,String assetNumber,String purchaseAmountS, String netBookValueS){
        info("writeOutputFile")

        outputFile.append(String.format("%s %s  %s  %s\n",fleetNumber, assetNumber, purchaseAmountS, netBookValueS))
    }

    /**
     * Write fleet asset record to CSV file.
     * @param fleetNumber String
     * @param assetNumber String
     * @param netBookValueS String
     * @param purchaseAmountS String
     */
    private void writeCSV(String fleetNumber,String assetNumber,String purchaseAmountS,String netBookValueS){
        info("writeCSV")

        outputFileCSV.append(String.format("%s,%s,%s,%s,\n",fleetNumber, assetNumber, purchaseAmountS, netBookValueS))
    }

    /**
     * Write fleet asset control report.
     * @param farDistrict String
     */
    private void writeControlReport(String farDistrict){
        info("writeControlReport")

        reportA.write(REPORT_DISTRICT.padLeft(41)+farDistrict)
        reportA.write(REPORT_PERIOD.padLeft(38)+priorMonth+"/"+priorYear)
        reportA.write(REPORT_REC_EXTRACT.padLeft(45)+recCount.toString())
        reportA.write("")
        reportA.write(REPORT_CONFIG_ERROR)
        Iterator iterate = errorLine.iterator()
        while(iterate.hasNext()){
            reportA.write(iterate.next())
        }
        reportA.write("")
    }
}

/*run script*/
ProcessTrb68f process = new ProcessTrb68f();
process.runBatch(binding);