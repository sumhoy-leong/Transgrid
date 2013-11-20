/**
 *  @Ventyx 2012
 *
 * This program writes an output file containing workers compensation <br>
 * claims details. This output file will be downloaded to a tape and <br>
 * sent to the workcover authority of NSW <br>
 * 
 * Developed based on <b>msbwcc.cbl@@/main/el5.2.3.2_dst/26</b>
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.List;
import com.mincom.eql.impl.*
import com.mincom.eql.*;

import com.mincom.ellipse.edoi.ejb.msf000.*;
import com.mincom.ellipse.edoi.ejb.msf002.*;
import com.mincom.ellipse.edoi.ejb.msf083.*;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf070.*;
import com.mincom.ellipse.edoi.ejb.msf071.*;
import com.mincom.ellipse.edoi.ejb.msf012.*;
import com.mincom.ellipse.edoi.ejb.msf510.*;
import com.mincom.ellipse.edoi.ejb.msf513.*;
import com.mincom.ellipse.edoi.ejb.msf514.*;
import com.mincom.ellipse.edoi.ejb.msf521.*;
import com.mincom.ellipse.edoi.ejb.msf530.*;
import com.mincom.ellipse.edoi.ejb.msf53a.*;
import com.mincom.ellipse.edoi.ejb.msf531.*;
import com.mincom.ellipse.edoi.ejb.msf533.*;
import com.mincom.ellipse.edoi.ejb.msf536.*;
import com.mincom.ellipse.edoi.ejb.msf537.*;
import com.mincom.ellipse.edoi.ejb.msf538.*;
import com.mincom.ellipse.edoi.ejb.msf539.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf828.*;
import com.mincom.ellipse.edoi.ejb.msf870.*;
import com.mincom.ellipse.edoi.ejb.msf871.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.eroi.linkage.mss071.*;
import com.mincom.ellipse.eroi.linkage.mssrat.*;
import com.mincom.ellipse.eroi.linkage.mssdat.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadRequiredAttributesDTO;
import java.util.Collections;

/**
 * Request Parameters for Trbwcc
 * <li><code>paramRunType</code>   : Run Type Indicator (U-pdate, E-dit)</li>
 * <li><code>paramState</code>     : State Code (Table SY)</li>
 * <li><code>paramDistrict</code>  : District code</li>
 * <li><code>paramStartDate</code> : Submission Start Date(DDMMYYYY)</li>
 * <li><code>paramEndDate</code>   : Submission End Date(DDMMYYYY)</li>
 */
public class ParamsTrbwcc {
    //List of Input Parameters
    String paramRunType
    String paramState
    String paramDistrict
    String paramStartDate
    String paramEndDate
}

/**
 * Main process of Trbwcc
 */
public class ProcessTrbwcc extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private int version = 12

    /*
     * Variables
     */
    private ParamsTrbwcc batchParams
    private def workingDir
    private List<String> arrayOfError
    private List<String> arrayOfMSIWCC
    private List<MSSWCCA> arrayOfMSSWCCA
    private List<RehabEntry> arrayOfRehabEntry
    private List<EstEntry> arrayOfEstEntry
    private List<PaymtRecovRec> arrayOfPaymtRecovRec
    private boolean partDay
    private boolean msf53aFound
    private String invStartDate = ""
    private String invEndDate = ""
    private String selfInsurerInd = ""
    private String insurerName = ""
    private String insurer = ""
    private String prevClaimNo = ""
    private String previousClaim = ""
    private String prevRehabFlag = ""
    private String startDate = ""
    private String invTestStartDate = ""
    private String msf530WicRateNo = ""
    private String sigInjDate = ""
    private String contactDate = ""
    private String wkrCommDate = ""
    private String injDesc = ""
    private String wkrTelNo = ""
    private String actClosedClaim = ""
    private String injuryDate = ""
    private String notifierName = ""
    private String notifierTelNo = ""
    private String incDesc = ""
	private String estimateWPI = ""
    private String msf53aEndDate = ""
    private Long claimBasRec = 0
    private Long claimActRec = 0
    private Long timeLostRec = 0
    private Long claimRehabRec = 0
    private Long payRecovRec = 0
    private Long claimEstRec = 0
    private Long claimBas2Rec = 0
    private Long claimCtlRec = 0
    private Long workCapacityRec = 0
    private Long invPayDate = 0
    private Long msf536EmpEntInpCr = 0
    private Long daysOffWork = 0
    private BigDecimal commonLawEstAmt = 0
    private BigDecimal commonLawPayAmt = 0
    private BigDecimal msf530WpSize = 0
    private BigDecimal msf530TariffRateNo = 0
    private BigDecimal gtotWcPayRec = 0
    private BigDecimal gtotWcPay = 0
    private BigDecimal gtotWcRec = 0
    private BigDecimal totWcPayments = 0
    private BigDecimal gtotEstLiab = 0
    private BigDecimal gtotEstRecov = 0
    private BigDecimal totPayAmount = 0
    private BigDecimal totRecAmount = 0
    private BigDecimal totEstLiab = 0
    private BigDecimal totEstRecov = 0
    private BigDecimal totIncapHrs = 0
    private BigDecimal totIncapMins = 0
    private BigDecimal ctlDecAdjSet = 0
    private BigDecimal ctlInpCrNset = 0

    /*
     * Constants
     */
    private static final String REPORT_A         = "TRBWCCA"
    private static final String REPORT_B         = "TRBWCCB"
    private static final String OUTPUT_FILE_NAME = "MSIWCC"
    private static final String ERR_MSG_FORMAT   = "%5s%-30s%-87s"
    private static final int SORT_ASCENDING      = 1
    private static final int MAX_ROW_READ        = 1000

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)

        printSuperBatchVersion()
        info("runBatch Version : " + version)

        batchParams = params.fill(new ParamsTrbwcc())

        //PrintRequest Parameters
        info("paramRunType  : " + batchParams.paramRunType)
        info("paramState    : " + batchParams.paramState)
        info("paramDistrict : " + batchParams.paramDistrict)
        info("paramStartDate: " + batchParams.paramStartDate)
        info("paramEndDate  : " + batchParams.paramEndDate)

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
        initialise()
        processRequest_C000()
    }

    private void initialise(){
        info("initialise")

        arrayOfError = new ArrayList<String>()
        arrayOfMSIWCC = new ArrayList<String>()
        arrayOfMSSWCCA = new ArrayList<MSSWCCA>()
        arrayOfRehabEntry = new ArrayList<RehabEntry>()
        arrayOfEstEntry = new ArrayList<EstEntry>()

        workingDir = env.getWorkDir().toString()
        invStartDate = inverseDate(batchParams.paramStartDate)
        invEndDate = inverseDate(batchParams.paramEndDate)
    }

    private void processRequest_C000(){
        info("processRequest_C000")

        writeHeaderRecord_C100()

        selfInsurerInd = getMSF002InsurerStatus_C160()

        getClaims_C200()
        List<MSSWCCA> sortedMSSWCCA = arrayOfMSSWCCA.sort(new MSSWCCAComparator(SORT_ASCENDING))
        claimDetails_F100(sortedMSSWCCA)

        writeTrailerRecord_C340()

        if (batchParams.paramRunType.trim() == 'U'){
            updateStateControls_C400()
        }

        printCoverSheet_C500()
        writeToOutputFile()
    }

    private void writeHeaderRecord_C100(){
        info("writeHeaderRecord_C100")

        try{
            MSF530Rec msf530Rec = edoi.findByPrimaryKey(new MSF530Key(batchParams.paramState))
            TableServiceReadReplyDTO wcin = readTable('WCIN', msf530Rec.getInsurer())

            if (!wcin){
                throwError(String.format(ERR_MSG_FORMAT, "", msf530Rec.getInsurer(), "Insurer Not Found In WCIN Table"))
            }

            insurer = msf530Rec.getInsurer()
            insurerName = wcin.getDescription()
            msf530WpSize = msf530Rec.getWpSize()
            msf530WicRateNo = msf530Rec.getWicRateNo()
            msf530TariffRateNo = msf530Rec.getTariffRateNo()

            HashMap<String,String> msiwccHdr = [:]
            msiwccHdr.put(MSIWCC.MSIWCC_HDR_REC_TYPE, '1')
            msiwccHdr.put(MSIWCC.MSIWCC_HDR_INSURER_NO, msf530Rec.getInsurer().padRight(3).toString().substring(0, 3))
            msiwccHdr.put(MSIWCC.MSIWCC_HDR_SUBMISSION_TYPE, 'CLAIMS')
            msiwccHdr.put(MSIWCC.MSIWCC_HDR_CLAIM_REL_NO, '05')
            msiwccHdr.put(MSIWCC.MSIWCC_HDR_SUBMIT_START, batchParams.paramStartDate)
            msiwccHdr.put(MSIWCC.MSIWCC_HDR_SUBMIT_END, batchParams.paramEndDate)

            writeMSIWCC_G100(msiwccHdr)

        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            throwError(String.format(ERR_MSG_FORMAT, "", batchParams.paramState, "State Control Record Not Found"))
        }
    }

    private String getMSF002InsurerStatus_C160(){
        info("getMSF002InsurerStatus_C160")

        try{
            MSF002_DC3510Rec msf002_dc3510 = edoi.findByPrimaryKey(new MSF002_DC3510Key((batchParams.paramDistrict?.trim() ? batchParams.paramDistrict : " "), "DC","3510"))
            return msf002_dc3510.getSelfInsurer()
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("Failed getting insurer status. The insurer status is defaulted to Y : " + e.getMessage())
        }

        return "Y"
    }

    private void getClaims_C200(){
        info("getClaims_C200")

        prevClaimNo = ""
        getClaimMSF536_C220()
        prevClaimNo = ""
        getEstimatesMSF538_C240()
        prevClaimNo = ""
        getPaymentsMSF539_C260()
        prevClaimNo = ""
        getGetRehabMSF521_C280()
        prevClaimNo = ""
    }

    private void getClaimMSF536_C220(){
        info("getClaimMSF536_C220")

        Constraint cClaimInd = MSF536Rec.claimInd.equalTo("E")
        Constraint cStatusDate = MSF536Rec.statusDate.lessThanEqualTo(batchParams.paramEndDate)
        Constraint cClaimStatus = MSF536Rec.claimStatus.notEqualTo("01")

        def query
        if (batchParams.paramDistrict ?.trim()){
            Constraint cDstrctCode = MSF536Key.dstrctCode.equalTo(batchParams.paramDistrict)
            Constraint cClaimNo = MSF536Key.claimNo.greaterThanEqualTo(" ")
            query = new QueryImpl(MSF536Rec.class).and(cClaimInd).and(cStatusDate).and(cClaimStatus).and(cDstrctCode).and(cClaimNo).orderBy(MSF536Rec.msf536Key)
        }else{
            Constraint cDstrctCode = MSF536Key.dstrctCode.greaterThanEqualTo(" ")
            query = new QueryImpl(MSF536Rec.class).and(cClaimInd).and(cStatusDate).and(cClaimStatus).and(cDstrctCode).orderBy(MSF536Rec.msf536Key)
        }

        edoi.search(query, MAX_ROW_READ, {MSF536Rec msf536Rec->

            String claimStatus = getClaimStatus_C320(msf536Rec.getClaimStatus())

            if (claimStatus.trim() == 'N'){
                return
            }

            MSF012_536Data msf012_536Data = readMSF012_Z220(msf536Rec)

            if (msf012_536Data.getBringUpInd()?.trim() != 'Y'){
                startDate = batchParams.paramStartDate
                if (msf536Rec.getStatusDate().toLong()<batchParams.paramStartDate.toLong()){
                    return
                }
            }else{
                startDate = msf012_536Data.getBringUpDate()
                if (msf536Rec.getStatusDate().toLong()<msf012_536Data.getBringUpDate().toLong()){
                    return
                }
            }

            addMSSWCCA(msf536Rec,"")
        })

    }

    private void getEstimatesMSF538_C240(){
        info("getEstimatesMSF538_C240")

        Constraint cInverseDate = MSF538Key.inverseDate.greaterThanEqualTo(invEndDate)
        Constraint cClaimNo = MSF538Key.claimNo.greaterThanEqualTo(" ")
        Constraint cEstType = MSF538Key.estType.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF538Rec.class).and(cInverseDate).and(cClaimNo).and(cEstType).orderBy(MSF538Rec.aix1)

        edoi.search(query, MAX_ROW_READ ,{ MSF538Rec msf538Rec->
            info("prevClaimNo :" + prevClaimNo)

            if (msf538Rec.getPrimaryKey().getClaimNo().trim() != prevClaimNo.trim()){
                prevClaimNo = msf538Rec.getPrimaryKey().getClaimNo()
                MSF536Rec msf536Rec = getClaim_C300(msf538Rec.getPrimaryKey().getClaimNo())

                if (msf536Rec == null){
                    return
                }

                String claimStatus = getClaimStatus_C320(msf536Rec.getClaimStatus())
                if (claimStatus.trim() == 'N'){
                    return
                }

                String normalDate = inverseDate(msf538Rec.getPrimaryKey().getInverseDate())
                info("normalDate : " + normalDate)
                MSF012_536Data msf012_536Data = readMSF012_Z220(msf536Rec)
                if (msf012_536Data.getBringUpInd().trim() != 'Y'){
                    startDate = batchParams.paramStartDate
                    if (normalDate.toLong()<batchParams.paramStartDate.toLong()){
                        return
                    }
                }else{
                    startDate = msf012_536Data.getBringUpDate()
                    if (normalDate.toLong()<msf012_536Data.getBringUpDate().toLong()){
                        return
                    }
                }

                addMSSWCCA(msf536Rec,"")

            }
        })
    }

    private void getPaymentsMSF539_C260(){
        info("getPaymentsMSF539_C260")

        Constraint cInverseDate = MSF539Key.inverseDate.greaterThanEqualTo(invEndDate)
        Constraint cClaimNo = MSF539Key.claimNo.greaterThanEqualTo(" ")
        Constraint cClaimType = MSF539Key.claimTyp.greaterThanEqualTo(" ")
        Constraint cPayRecovCode = MSF539Key.payRecovCode.greaterThanEqualTo(" ")
        Constraint cInverseSeqNo = MSF539Key.inverseSeqNo.greaterThanEqualTo(" ")
        int counter = 0

        def query = new QueryImpl(MSF539Rec.class).and(cInverseDate).and(cClaimNo).and(cClaimType).and(cPayRecovCode).and(cInverseSeqNo).orderBy(MSF539Rec.msf539Key)

        edoi.search(query, MAX_ROW_READ, { MSF539Rec msf539Rec->
            info("prevClaimNo :" + prevClaimNo)

            if (msf539Rec.getPrimaryKey().getClaimNo().trim() != prevClaimNo.trim()){


                MSF536Rec msf536Rec = getClaim_C300(msf539Rec.getPrimaryKey().getClaimNo())

                if(msf536Rec != null){
                    String claimStatus = getClaimStatus_C320(msf536Rec.getClaimStatus())

                    if (!claimStatus.trim().equals('N')){
                        String normalDate = inverseDate(msf539Rec.getPrimaryKey().getInverseDate())

                        info("normalDate :" + normalDate)
                        MSF012_536Data msf012_536Data = readMSF012_Z220(msf536Rec)
                        if (msf012_536Data.getBringUpInd().trim() != 'Y'){
                            startDate = batchParams.paramStartDate
                            if (normalDate.toLong()>=batchParams.paramStartDate.toLong()){
                                prevClaimNo=msf539Rec.getPrimaryKey().getClaimNo()
                                addMSSWCCA(msf536Rec,"")
                            }
                        }else{
                            startDate = msf012_536Data.getBringUpDate()
                            if (normalDate.toLong()>=msf012_536Data.getBringUpDate().toLong()){
                                prevClaimNo=msf539Rec.getPrimaryKey().getClaimNo()
                                addMSSWCCA(msf536Rec,"")
                            }
                        }
                    }

                }


            }
        })
    }

    private void getGetRehabMSF521_C280(){
        info("getGetRehabMSF521_C280")

        Constraint cClaimNo = MSF521Rec.claimNo.notEqualTo(" ")
        Constraint cRehabProvider = MSF521Rec.rehabProvider.notEqualTo(" ")
        Constraint cLastChanged = MSF521Rec.lastChanged.lessThanEqualTo(batchParams.paramEndDate)
        Constraint cEmployeeId = MSF521Key.employeeId.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF521Rec.class).and(cClaimNo).and(cRehabProvider).and(cLastChanged).and(cEmployeeId).orderBy(MSF521Rec.msf521Key)

        edoi.search(query, MAX_ROW_READ, { MSF521Rec msf521Rec->

            info("prevClaimNo :" + prevClaimNo)

            if (msf521Rec.getClaimNo().trim() != prevClaimNo.trim()){

                MSF536Rec msf536Rec = getClaim_C300(msf521Rec.getClaimNo())

                if(msf536Rec == null){
                    return
                }

                String claimStatus = getClaimStatus_C320(msf536Rec.getClaimStatus())

                if (claimStatus.trim() == 'N'){
                    return
                }

                MSF012_536Data msf012_536Data = readMSF012_Z220(msf536Rec)
                info("lastChanged :" + msf521Rec.getLastChanged())

                if (msf012_536Data.getBringUpInd().trim() != 'Y'){
                    startDate = batchParams.paramStartDate
                    if (msf521Rec.getLastChanged().toLong()<batchParams.paramStartDate.toLong()){
                        return
                    }
                }else{
                    startDate = msf012_536Data.getBringUpDate()
                    if (msf521Rec.getLastChanged().toLong()<msf012_536Data.getBringUpDate().toLong()){
                        return
                    }
                }
                prevClaimNo = msf521Rec.getClaimNo()
                addMSSWCCA(msf536Rec,'Y')
            }
        })
    }

    private MSF536Rec getClaim_C300(String claimNo){
        info("getClaim_C300")

        Constraint cClaimNo = MSF536Key.claimNo.equalTo(claimNo)
        Constraint cClaimInd = MSF536Rec.claimInd.equalTo("E")
        Constraint cDstrctCode = MSF536Key.dstrctCode.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF536Rec.class).and(cClaimNo).and(cClaimInd).and(cDstrctCode)

        MSF536Rec msf536Rec = edoi.firstRow(query)
        if (msf536Rec){
            return msf536Rec
        }

        return null
    }

    private String getClaimStatus_C320(String claimStatus){
        info("getClaimStatus_C320")
        TableServiceReadReplyDTO wccs = readTable('WCCS', claimStatus)
        if (wccs){
            String assocChar7 = wccs.getAssociatedRecord().padRight(7).toString().substring(6,7)
            return assocChar7
        }

        return ""
    }

    private void writeTrailerRecord_C340(){
        info("writeTrailerRecord_C340")

        DecimalFormat df9v2 = new DecimalFormat("########0.00")
        DecimalFormat df7 = new DecimalFormat("######0")

        HashMap<String,String> msiwccTlr = [:]
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_REC_TYPE, '9')
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_CLAIM_BAS_REC_COUNT, claimBasRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_CLAIM_ACT_REC_COUNT, claimActRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_TIME_LOST_REC_COUNT, timeLostRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_REHAB_REC_COUNT, claimRehabRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_PAY_REC_REC_COUNT, payRecovRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_EST_REC_COUNT, claimEstRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_DT2_REC_COUNT, claimBas2Rec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_CTL_REC_COUNT, claimCtlRec.toString().padLeft(7).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_WCD_REC_COUNT, workCapacityRec.toString().padLeft(7).replaceAll(' ','0'))
        String totPyRecAmt = df9v2.format(gtotWcPay.abs()).replace('.', '')
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_TOT_PY_REC_AMT, totPyRecAmt.padLeft(14).replaceAll(' ','0'))
        msiwccTlr.put(MSIWCC.MSIWCC_TLR_TOT_PY_REC_AMT_SIGN, (gtotWcPay < 0 ? '-' : '+'))

        writeMSIWCC_G100(msiwccTlr)
    }

    private void updateStateControls_C400(){
        info("updateStateControls_C400")
        try{
            MSF530Rec msf530Rec = edoi.findByPrimaryKey(new MSF530Key(batchParams.paramState))
            msf530Rec.setClaimRelNo(new BigDecimal('05'))
            msf530Rec.setSubStartDate(batchParams.paramStartDate)
            msf530Rec.setSubEndDate(batchParams.paramEndDate)
            edoi.update(msf530Rec)
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed updating state control : " + e.getMessage())
        }

    }

    private void printCoverSheet_C500(){
        info("printCoverSheet")

        DecimalFormat numFormat = new DecimalFormat("#,###,##0")
        DecimalFormat finFormat = new DecimalFormat("###,###,##0.00;-###,###,##0.00")

        def reportB = report.open(REPORT_B)
        reportB.write("WORKCOVER COVER SHEET".center(80))
        reportB.writeLine(80,"-")
        reportB.write(" ")
        reportB.write("                  Details of Submission by Insurer")
        reportB.write(" ")
        reportB.write("  To :          WorkCover New South Wales")
        reportB.write("                Data Management Branch   ")
        reportB.write("                Level 7 400 Kent Street  ")
        reportB.write("                Sydney NSW 2000          ")
        reportB.write("                Phone: (02)-9370 5578         Fax: (02)-9370 6114")
        reportB.write(" ")
        reportB.write("  Insurer Name: " + insurerName.padRight(30).toString().substring(0, 30) + "Number: " + insurer.padRight(4).toString().substring(0,4))
        reportB.write(" ")
        reportB.write("  Medium:      Tape / Diskette ")
        reportB.write(" ")
        reportB.write("  Number of Tapes/diskettes supplied ...........")
        reportB.write(" ")
        reportB.write("  Submission Start Date: "+ formatDate(batchParams.paramStartDate).padRight(15) + "Submission End Date: "+ formatDate(batchParams.paramEndDate).padRight(15))
        reportB.write(" ")
        reportB.write("  Total Number of Records ")
        reportB.write(" ")
        reportB.write("   Basic claim detail".padRight(30)+numFormat.format(claimBasRec))
        reportB.write("   Claim activity".padRight(30)+numFormat.format(claimActRec))
        reportB.write("   Time lost".padRight(30)+numFormat.format(timeLostRec))
        reportB.write("   Compensation".padRight(30)+numFormat.format(payRecovRec))
        reportB.write("   Estimate".padRight(30)+numFormat.format(claimEstRec))
        reportB.write("   Basic claim detail 2".padRight(30)+numFormat.format(claimBas2Rec))
        reportB.write("   Claim control".padRight(30)+numFormat.format(claimCtlRec))
        reportB.write("   Work Capacity".padRight(30)+numFormat.format(workCapacityRec))
        reportB.write(" ")
        reportB.write("  Financial totals on the submission ")
        reportB.write(" ")
        reportB.write("   Total value of claim payments".padRight(60)+finFormat.format(gtotWcPay))
        reportB.write("   Total value of claim recoveries".padRight(60)+finFormat.format(gtotWcRec))
        reportB.write("   Total value of estimates of outstanding liability".padRight(60)+finFormat.format(gtotEstLiab))
        reportB.write("   Total value of estimated recoveries".padRight(60)+finFormat.format(gtotEstRecov))
        reportB.write(" ")
        reportB.write("Signed ..............................".padRight(50)+"Date ....................")
        reportB.write(" ")
        reportB.write("       (This lower section is to be completed by WorkCover New South Wales)")
        reportB.write("".padRight(75,"-"))
        reportB.write("Received by WorkCover")
        reportB.write(" ")
        reportB.write("Signed ..............................".padRight(50)+"Date ....................")
        reportB.write(" ")
        reportB.write("Date Despatched to Insurer:         ...............")
        reportB.write(" ")
        reportB.write("Error Report Date:                  ...............")
        reportB.write(" ")
        reportB.write("Reject Reason:                      ...............")
        reportB.write(" ")
        reportB.write("Initial Contact Date:               ...............")
        reportB.write(" ")
        reportB.write("Initial Contact Date:               ...............")
        reportB.write(" ")
        reportB.write("Additional Comments:")
        reportB.write("............................................................................")
        reportB.write(" ")
        reportB.write("............................................................................")
        reportB.write(" ")
        reportB.write("............................................................................")
        reportB.close()
    }

    private void claimDetails_F100(List<MSSWCCA> arrayOfMSSWCCA){
        info("claimDetails_F100")

        Long cntr = 0
        arrayOfMSSWCCA.each {MSSWCCA msswcca ->
            processMSSWCCA_F110(msswcca)
            cntr++
        }

        //At end process
        if(cntr >= 1 && prevClaimNo?.trim()){
            writeClaim_F120()
        }
    }

    private void processMSSWCCA_F110(MSSWCCA element){
        info("processMSSWCCA_F110")

        info("prevClaimNo    : " + prevClaimNo)
        info("msswccaClaimNo : " + element.getClaimNo())

        if(prevClaimNo?.trim() && element.getInsClaimNo().trim() != prevClaimNo.trim()){
            writeClaim_F120()
            prevRehabFlag = ""
        }

        prevClaimNo = element.getInsClaimNo()
        previousClaim = element.getClaimNo()

        if(!(prevRehabFlag?.trim())){
            prevRehabFlag = element.getRehabFlag()
        }
    }

    private void writeClaim_F120(){
        info("writeClaim_F120")
        boolean isInsClaim = true
        if(prevClaimNo.trim() == previousClaim.trim()){
            isInsClaim = false
        }

        String msf536InsClaimNo = prevClaimNo

        MSF536Rec msf536Rec = getClaim_C300(previousClaim)

        if(msf536Rec==null){
            writeError(String.format(ERR_MSG_FORMAT, "", previousClaim, "MSF536 Record Not Found For Claim"))
            return
        }

        if(!batchParams.paramDistrict.trim().equals("")){
            if(msf536Rec.getPrimaryKey().getDstrctCode().trim() != batchParams.paramDistrict.trim()){
                return
            }
        }

        MSF537Rec msf537Rec = readWCDetail_Z200(msf536Rec.getPrimaryKey().getClaimNo())
        if(msf537Rec==null){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Workers Comp Record Not Found"))
            return
        }

        MSF531Rec msf531Rec = readWCompLocCodes_Z650(msf537Rec.getWcLocation())
        if(msf531Rec==null){
            writeError(String.format(ERR_MSG_FORMAT, "", msf537Rec.getWcLocation(), "Workers Comp Location Code Not Found"))
            return
        }

        boolean isUpdateMSF012

        MSF012_536Data msf012_536Data = readMSF012_Z220(msf536Rec)
        if(msf012_536Data.getBringUpInd().trim() != 'Y'){
            isUpdateMSF012 = false
            startDate = batchParams.paramStartDate
            invTestStartDate = invStartDate
        }else{
            isUpdateMSF012 = true
            startDate = msf012_536Data.getBringUpDate()
            invTestStartDate = inverseDate(msf012_536Data.getBringUpDate())
        }

        String msf536ClaimStatus = msf536Rec.getClaimStatus()

        HashMap<String,String> msiwccDtl = [:]
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_REC_TYPE, '2')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLAIM_NO, msf536InsClaimNo)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_REC_IDENT, '1')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_SHARED_CLAIM_CODE, (msf537Rec.getSharedClaim().trim().equals("") ? '0' : msf537Rec.getSharedClaim()))

        if (msf537Rec.getWcMiscx1().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536Rec.getPrimaryKey().getClaimNo(), "Claim Officer Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_ERR_RPT_TARGET, msf537Rec.getWcMiscx1().padRight(10).toString().substring(3,10))
        }

        TableServiceReadReplyDTO wcl2 = readTable('WCL2', msf531Rec.getWclocUserFldx2())
        if (wcl2){
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_INSURER_BRANCH, wcl2.getDescription())
        }else{
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Insurer Branch Not Found In WCL2 Table"))
            return
        }

        if(msf536Rec.getDateReceived().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Date Entered Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_DATE_CLAIM_ENT, msf536Rec.getDateReceived())
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_DATE_CLAIM_LOD, (msf536Rec.getDateReceived().trim().toLong() < 19980101) ? '00000000' : msf536Rec.getDateRaised())
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_POLICY_NO, (msf012_536Data.getPolicyNo().length()<19) ? msf012_536Data.getPolicyNo().padRight(19) : msf012_536Data.getPolicyNo().substring(0,19))
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_PRD_COMMENCE_DATE, '00000000')

        MSF000_DC0004Rec msf000_DC0004Rec
        if (batchParams.paramDistrict.trim().equals("")){
            MSF002_SC3801Rec msf002_SC3801Rec = readMSF002_SC3801Rec()
            if (msf002_SC3801Rec == null){
                writeError(String.format(ERR_MSG_FORMAT, "", batchParams.paramDistrict, "Default District Not Found"))
                return
            }

            msf000_DC0004Rec = readMSF000_DC0004Rec_Z750(msf002_SC3801Rec.getDefaultDstrct())
        } else {
            msf000_DC0004Rec = readMSF000_DC0004Rec_Z750(batchParams.paramDistrict)
        }

        if (msf000_DC0004Rec == null){
            writeError(String.format(ERR_MSG_FORMAT, "", batchParams.paramDistrict, "Employer ACN Not Found"))
            return
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_EMPLOYER_ACN, (msf000_DC0004Rec.getDstrctAcnNo().trim().equals("") ?
                '000000000': String.format("%9s", msf000_DC0004Rec.getDstrctAcnNo().trim()).replace(' ', '0')))

        String msf536EmployeeId = msf536Rec.getClaimEntity().padRight(10).toString().substring(0,10)
        MSF810Rec msf810Rec = readEmployeeRecord_Z400(msf536EmployeeId)

        if (msf810Rec == null){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536EmployeeId, "Employee Core Record Not Found"))
            return
        }

        String msf810ResAddress1, msf810ResAddress2, msf810ResAddress3, msf810ResPostcode

        if (!(msf810Rec.getPostAddress_1().trim().equals("")||msf810Rec.getPostAddress_1().trim().equalsIgnoreCase("AS ABOVE"))){
            msf810ResAddress1 = msf810Rec.getPostAddress_1()
            msf810ResAddress2 = msf810Rec.getPostAddress_2()
            msf810ResAddress3 = msf810Rec.getPostAddress_3()
            msf810ResPostcode = msf810Rec.getPostZipcode()
        }else{
            msf810ResAddress1 = msf810Rec.getResAddress_1()
            msf810ResAddress2 = msf810Rec.getResAddress_2()
            msf810ResAddress3 = msf810Rec.getResAddress_3()
            msf810ResPostcode = msf810Rec.getResZipcode()
        }

        if (msf810ResAddress3.trim().equals("")){
            msf810ResAddress3 = msf810ResAddress2
            msf810ResAddress2 = ""
        }

        if ((msf810ResAddress1 + msf810ResAddress2 + msf810ResAddress3).trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Employee Core Record Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_STR_ADDR, (msf810ResAddress1 + " " + msf810ResAddress2))
        }

        if(msf810ResAddress2.trim().equals("") && msf810ResAddress3.trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Home Locality Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_LOCALITY, (msf810ResAddress3.trim().equals("") ? msf810ResAddress2 : msf810ResAddress3))
        }

        if(msf810ResPostcode.trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Home Post Code Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_POSTCODE, msf810ResPostcode)
        }

        MSF760Rec msf760Rec = readPersonelRecord_Z250(msf536EmployeeId)
        if(msf760Rec == null){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536EmployeeId, "Employee Personnel Record Not Found"))
            return
        }

        if(msf760Rec.getGender().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Employee Gender Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_GENDER, msf760Rec.getGender())
        }

        if(msf760Rec.getBirthDate().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Employee Birth Date Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_BIRTHDATE, msf760Rec.getBirthDate())
        }

        String clmntLanguage = ""
        if(((msf760Rec.getPersUserFldx9().trim().equals("")) && (msf536Rec.getDateReceived().toLong()>=19980101) && (msf536Rec.getDateReceived().toLong()<20110701))
        ||((msf760Rec.getEthnicity().trim().equals("") && (msf536Rec.getDateReceived().toLong()>=20110701)))){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Employee Language Not Found"))
            return
        }else{
            if(msf760Rec.getPersUserFldx9().trim().equals("") && (msf536Rec.getDateReceived().toLong()<19980101)){
                clmntLanguage='0'
            }else{
                if((msf536Rec.getDateReceived().toLong()>=19980101) && (msf536Rec.getDateReceived().toLong()<20110701)){
                    clmntLanguage = msf760Rec.getPersUserFldx9()
                }else{
                    clmntLanguage = msf760Rec.getEthnicity()
                }
            }
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_LANGUAGE, clmntLanguage)
        info("clmntLanguage: " + clmntLanguage)

        MSF510Rec msf510Rec = readIncidentRecord_Z300(msf536Rec.getPrimaryKey().getDstrctCode(), msf536Rec.getIncidentNo())
        if (msf510Rec == null){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getIncidentNo(),',',("E" + msf536EmployeeId)),
                    "Incident Record Not Found"))
            return
        }

        String msf510DateOCC = inverseDate(msf510Rec.getRevOccDate())

        MSF878Rec msf878Rec = readPositionMSF878_Z600(msf536EmployeeId, msf510Rec.getRevOccDate(), msf510DateOCC)
        if(msf878Rec == null){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536EmployeeId, "Position Detail Record Not Found (MSF878 record)"))
            return
        }

        String posClassif = ""
        if(msf536Rec.getDateReceived().toLong()<20020701){
            try{
                MSF871Rec msf871Rec = edoi.findByPrimaryKey(new MSF871Key(msf878Rec.getPrimaryKey().getPositionId()))
                posClassif = msf871Rec.getPosClx1().padRight(4).toString().substring(0, 4)
            }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                        "Position Detail Record Not Found (MSF871 record)"))
                return
            }
        }else{
            if(msf536Rec.getDateReceived().toLong()>=20020701 && msf536Rec.getDateReceived().toLong()<20110701){
                try{
                    MSF870Rec msf870Rec = edoi.findByPrimaryKey(new MSF870Key(msf878Rec.getPrimaryKey().getPositionId()))
                    posClassif = msf870Rec.getPosReportCdex5()
                }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
                    writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                            "Position Detail Record Not Found (MSF870 Record)"))
                    return
                }
            }else{
                TableServiceReadReplyDTO pooc = readTable('POOC', msf878Rec.getPrimaryKey().getPositionId())
                if (pooc){
                    posClassif = pooc.getAssociatedRecord().padRight(4).toString().substring(0, 4)
                }else{
                    writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                            "Position Detail Record Not Found (Entry on POOC table - MSF010 Record)"))
                    return
                }
            }
        }

        if(posClassif.trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Employee Occupation Code Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_OCCUP, posClassif)
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_DEPN_CHILDREN, (msf537Rec.getDepChildren().trim().equals("") ? '0' : msf537Rec.getDepChildren()))
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_OTH_DEPN, (msf537Rec.getOtherDeps().trim().equals("") ? '0' : msf537Rec.getOtherDeps()))

        MSF820Rec msf820Rec = readPayrollRecord_Z450(msf536EmployeeId)
        if(msf820Rec == null){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf536EmployeeId),
                    "Payroll Master Record Not Found"))
            return
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_FILLER_8, '0')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_PERM_EMPLOYEE, (msf760Rec.getCitizenInd().trim() == '3' ? '3' :
                ((msf820Rec.getEmployeeClass().trim() == 'F' || msf820Rec.getEmployeeClass().trim() == 'P') ? '1' : '2')))

        if(msf536Rec.getClaimUserFldx1().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Training Status Code Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_TRNG_STATUS, msf536Rec.getClaimUserFldx1())
        }

        DecimalFormat df4 = new DecimalFormat("###0")
        if(msf537Rec.getWklyHoursAta()==0){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Hours Work Per Week is zero"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_HOURS_WORKED, df4.format(msf537Rec.getWklyHoursAta()*100))
        }

        BigDecimal clmntPayRate = 0
        String clmntPayRateSign = ""
        if(msf537Rec.getWklyNeRate() != 0 && msf536Rec.getDateReceived().toLong()>=19980101){
            clmntPayRate = msf537Rec.getWklyNeRate()
        }else{
            MSSRATLINK mssratlnk = eroi.execute('MSSRAT', {MSSRATLINK mssratlnk ->
                mssratlnk.setOption('1')
                mssratlnk.setEmployeeId(msf536EmployeeId)
                mssratlnk.setRequiredDate(msf510DateOCC)
            })

            if(mssratlnk.getRatStatus().trim().equals("")){
                if(mssratlnk.getBaseWklyRate() > 6000){
                    clmntPayRate = 6000
                }else{
                    clmntPayRate = mssratlnk.getBaseWklyRate()
                }
            }else{
                clmntPayRate = 0
            }
        }

        if(clmntPayRate == 0 && msf537Rec.getWklyNeRate() != 0){
            clmntPayRate = msf537Rec.getWklyNeRate()
        }

        DecimalFormat df5v2 = new DecimalFormat("####0.00")
        String dtlClmntPayRate = df5v2.format(clmntPayRate.abs()).replace('.','')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_PAY_RATE, dtlClmntPayRate.padLeft(7).replaceAll(' ','0'))
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_PAY_RATE_SIGN, (clmntPayRate < 0 ? '-' : '+'))

        if(clmntPayRate==0 && msf536Rec.getDateReceived().toLong()>=19980101){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Pay Rate is zero"))
            return
        }

        String personData = "E" + msf536EmployeeId
        MSF514Rec msf514Rec = readIncidentInjuryRec_Z350(msf536Rec.getIncidentNo(), personData)
        if(msf514Rec==null){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getIncidentNo(),',',personData),
                    "Incident Injury Record Not Found"))
            return
        }

        if(msf514Rec.getDutyStatus().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getIncidentNo(),',',personData),
                    "Duty Status Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_DUTY_STATUS, msf514Rec.getDutyStatus().padRight(1).toString().substring(0, 1))
        }

        String workplStrAddr = ""
        if(msf531Rec.getAddressLine_1().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf531Rec.getPrimaryKey().getWcLocation()),
                    "Work Street Address Not Found"))
            return
        }else{
            String msf531Address1 =""
            String msf531Address2 =""
            msf531Address1 = msf531Rec.getAddressLine_1()
            if(!msf531Rec.getAddressLine_3().trim().equals("")){
                msf531Address2 = msf531Rec.getAddressLine_2()
            }
            workplStrAddr = msf531Address1+" "+msf531Address2
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_WORKPL_STR_ADDR, workplStrAddr)

        String workplLocAddr = ""
        if(msf531Rec.getAddressLine_2().trim().equals("") && msf531Rec.getAddressLine_3().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf531Rec.getPrimaryKey().getWcLocation()),
                    "Work Locality Not Found"))
            return
        }else{
            if(msf531Rec.getAddressLine_3().trim().equals("")){
                workplLocAddr = msf531Rec.getAddressLine_2()
            }else{
                workplLocAddr = msf531Rec.getAddressLine_3()
            }
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_WORKPL_LOC_ADDR, workplLocAddr)

        if(msf531Rec.getZipcode().padRight(4).toString().substring(0, 4).trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf531Rec.getPrimaryKey().getWcLocation()),
                    "Work Post Code Not Found"))
            return
        }else{
            msiwccDtl.put(MSIWCC.MSIWCC_DTL_WORKPL_POSTCODE, msf531Rec.getZipcode().padRight(4).toString().substring(0, 4))
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_INDUS_ASIC, (msf536Rec.getDateReceived().toLong()>=19970701 ? '0000' : msf531Rec.getWclocUserFldx1()))

        String indusAnzsic = ""
        if(msf536Rec.getDateReceived().toLong()>=19970701 && msf536Rec.getDateReceived().toLong()<20110701){
            indusAnzsic = msf531Rec.getWpIndType()
        }else{
            if(msf536Rec.getDateReceived().toLong()>=20110701){
                indusAnzsic = msf531Rec.getWclocUserFldx3()
            }else{
                indusAnzsic = "0000"
            }
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_INDUS_ANZSIC, indusAnzsic)
        info("indusAnzsic :" + indusAnzsic)

        DecimalFormat df5 = new DecimalFormat("####0")
        BigDecimal workplSize = 0
        if(msf530WpSize==0 && msf531Rec.getWpSize()==0){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf536Rec.getPrimaryKey().getClaimNo(),',',msf531Rec.getPrimaryKey().getWcLocation()),
                    "Work Size Not Found"))
            return
        }else{
            if(msf531Rec.getWpSize()>0){
                workplSize = msf531Rec.getWpSize()
            }
            else{
                workplSize = msf530WpSize
            }
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_WORKPL_SIZE, df5.format(workplSize).padLeft(5).replaceAll(' ','0'))

        String accLocn = ""
        if(msf536Rec.getDateReceived().toLong()>=19980101 && msf510Rec.getIncUserFldx1().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", msf510Rec.getPrimaryKey().getIncidentNo(), "Accident Location Code Not Found"))
            return
        }else{
            if(msf536Rec.getDateReceived().toLong()<19980101){
                accLocn = "00"
            }else{
                accLocn = msf510Rec.getIncUserFldx1().padRight(2).toString().substring(0, 2)
            }
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_ACC_LOCN, accLocn)

        String accLocnDesc = ""
        String accLocality = ""
        String accPostcode = ""
        if(msf536Rec.getDateReceived().toLong()<19980101
        || msf510Rec.getIncUserFldx1().padRight(2).toString().substring(0, 2).trim().equals("00")
        || msf510Rec.getIncUserFldx1().padRight(2).toString().substring(0, 2).trim().equals("01")){
            accLocnDesc = "NA"
            accLocality = "NA"
            accPostcode = "0000"
        }else{

            if(!msf510Rec.getAddressLine_1().trim().equals("")){
                accLocnDesc = msf510Rec.getAddressLine_1()+" "+ msf510Rec.getAddressLine_2()
            }else{
                writeError(String.format(ERR_MSG_FORMAT, "", msf510Rec.getPrimaryKey().getIncidentNo(), "Accident Location Desc Not Found"))
                return
            }

            if(!msf510Rec.getAddressLine_3().trim().equals("")){
                accLocality = msf510Rec.getAddressLine_3()
            }else{
                writeError(String.format(ERR_MSG_FORMAT, "", msf510Rec.getPrimaryKey().getIncidentNo(), "Accident Locality Not Found"))
                return
            }

            if(!msf510Rec.getAddressZip().trim().equals("")){
                accPostcode = msf510Rec.getAddressZip()
            }else{
                writeError(String.format(ERR_MSG_FORMAT, "", msf510Rec.getPrimaryKey().getIncidentNo(), "Accident Post Code Not Found"))
                return
            }
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_ACC_LOCN_DESC, accLocnDesc)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_ACC_LOCALITY, accLocality)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_ACC_POSTCODE, accPostcode)

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_DATE_INJURY, msf510DateOCC)

        String timeInjury = ""
        TableServiceReadReplyDTO ohit = readTable('OHIT', msf510Rec.getIncidentType())
        if (ohit){
            if (ohit.getAssociatedRecord().padRight(1).toString().substring(0, 1).trim().equals("Y")){
                timeInjury = "0000"
            }else{
                timeInjury = msf510Rec.getTimeOccurred().toString().substring(0, 4)
            }
        }else{
            timeInjury = msf510Rec.getTimeOccurred().toString().substring(0, 4)
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_TIME_INJURY, timeInjury)

        String natureInjury = ""
        if(msf536Rec.getDateReceived().toLong()>19910630){
            if (msf514Rec.getPrimaryKey().getNatureInjury().trim().equals("")){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Nature of Injury Not Found"))
                return
            }

            String natureInjuryId = msf514Rec.getPrimaryKey().getNatureInjury().padRight(4).toString().substring(3, 4)
            if ((msf536Rec.getDateReceived().toLong() > 19910630 && msf536Rec.getDateReceived().toLong() < 20020701 && natureInjuryId.trim() != 'A' && !natureInjuryId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20020630 && msf536Rec.getDateReceived().toLong() < 20110701 && natureInjuryId.trim() != 'B' && !natureInjuryId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20110630 && natureInjuryId.trim() != 'C' && !natureInjuryId.trim().equals(""))){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Nature of Injury Not Found"))
                return
            }

            natureInjury = msf514Rec.getPrimaryKey().getNatureInjury().padRight(3).toString().substring(0, 3)
        }else{
            natureInjury = "000"
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_NATURE_INJURY, natureInjury)

        String bodilyLocn = ""
        if(msf536Rec.getDateReceived().toLong()>19910630){
            if (msf514Rec.getPrimaryKey().getBodilyLoc().trim().equals("")){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Bodily Location Not Found"))
                return
            }

            String bodilyLocId = msf514Rec.getPrimaryKey().getBodilyLoc().padRight(4).toString().substring(3, 4)
            if ((msf536Rec.getDateReceived().toLong() > 19910630 && msf536Rec.getDateReceived().toLong() < 20020701 && bodilyLocId.trim() != 'A' && !bodilyLocId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20020630 && msf536Rec.getDateReceived().toLong() < 20110701 && bodilyLocId.trim() != 'B' && !bodilyLocId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20110630 && bodilyLocId.trim() != 'C' && !bodilyLocId.trim().equals(""))){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Bodily Location Not Found"))
                return
            }
            bodilyLocn = msf514Rec.getPrimaryKey().getBodilyLoc().padRight(3).toString().substring(0, 3)
        }else{
            bodilyLocn = "000"
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_BODILY_LOCN, bodilyLocn)

        String mechInjury = ""
        if(msf536Rec.getDateReceived().toLong()>19910630){
            if (msf514Rec.getMechInjury().trim().equals("")){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Mech Injury Not Found"))
                return
            }

            String mechInjuryId = msf514Rec.getMechInjury().padRight(3).toString().substring(2, 3)
            if ((msf536Rec.getDateReceived().toLong() > 19910630 && msf536Rec.getDateReceived().toLong() < 20020701 && mechInjuryId.trim() != 'A' && !mechInjuryId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20020630 && msf536Rec.getDateReceived().toLong() < 20110701 && mechInjuryId.trim() != 'B' && !mechInjuryId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20110630 && mechInjuryId.trim() != 'C' && !mechInjuryId.trim().equals(""))){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Mech Injury Not Found"))
                return
            }
            mechInjury = msf514Rec.getMechInjury().padRight(2).toString().substring(0, 2)
        }else{
            mechInjury = "00"
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_MECH_INJURY, mechInjury)

        String accAgency = ""
        String toocsBreakAgency = ""
        String injAgency = ""
        String toocsAgencyInj = ""

        if(msf536Rec.getDateReceived().toLong()>20020630){
            MSF513Rec msf513Rec = getCauseFromMSF513_Z1000(msf536Rec)
            if (msf513Rec == null){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Cause of Injury Not Found"))
                return
            }else{
                if(msf536Rec.getDateReceived().toLong()>20110630){
                    accAgency = "000"
                    toocsBreakAgency = msf513Rec.getPrimaryKey().getImmCauseCode().padRight(4).toString().substring(0,4)
                }else{
                    accAgency = msf513Rec.getPrimaryKey().getImmCauseCode().padRight(3).toString().substring(0,3)
                    toocsBreakAgency = "000"
                }
            }

            String agencyInjuryId = msf514Rec.getAgencyInjury().padRight(5).toString().substring(4, 5)
            if ((msf536Rec.getDateReceived().toLong() < 20110701 && agencyInjuryId.trim() != 'B' && !agencyInjuryId.trim().equals(""))
            || (msf536Rec.getDateReceived().toLong() > 20110630 && agencyInjuryId.trim() != 'C' && !agencyInjuryId.trim().equals(""))){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                        "Agency of Accident/Injury Not Found"))
                return
            }else{
                if(msf536Rec.getDateReceived().toLong()<20110701){
                    injAgency = msf514Rec.getAgencyInjury().padRight(3).toString().substring(0, 3)
                    toocsAgencyInj = "0000"
                }else{
                    injAgency = "000"
                    toocsAgencyInj = msf514Rec.getAgencyInjury().padRight(4).toString().substring(0, 4)
                }
            }

        }else{
            if(msf536Rec.getDateReceived().toLong()>19910630){
                if(msf514Rec.getAgencyInjury().trim().equals("")){
                    writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                            "Agency of Accident/Injury Not Found"))
                    return
                }else{
                    accAgency = msf514Rec.getAgencyInjury().padRight(3).toString().substring(0,3)
                    injAgency = "000"
                    toocsAgencyInj = "0000"
                    toocsBreakAgency = "0000"
                }
            }else{
                accAgency = "000"
                injAgency = "000"
                toocsAgencyInj = "0000"
                toocsBreakAgency = "0000"
            }
        }

        msiwccDtl.put(MSIWCC.MSIWCC_DTL_ACC_AGENCY, accAgency)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_TOOCS_BREAK_AGENCY, toocsBreakAgency)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_INJ_AGENCY, injAgency)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_TOOCS_AGENCY_INJ, toocsAgencyInj)

        String resultInjury = ""
        if(msf514Rec.getResultInjury().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getPrimaryKey().getIncidentNo(),',',personData),
                    "Result of Injury Not Found"))
            return
        }else{
            resultInjury = msf514Rec.getResultInjury().padRight(1).toString().substring(0, 1)
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_RESULT_INJURY, resultInjury)

        String dateDeceased = ""
        if(msf514Rec.getResultInjury().padRight(1).toString().substring(0, 1).trim() == '1'){
            if(msf760Rec.getDeathDate().trim().equals("") || msf760Rec.getDeathDate().trim() == '00000000'){
                writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getResultInjury(),',',personData),
                        "Deceased Date Not Found"))
                return
            }else{
                if(msf760Rec.getDeathDate().toLong()<msf537Rec.getDateCeased().toLong() || msf760Rec.getDeathDate().toLong()<msf510DateOCC.toLong()){
                    writeError(String.format(ERR_MSG_FORMAT, "", String.format("%-10s%1s%-19s", msf514Rec.getResultInjury(),',',personData),
                            "Date deceased must be greater than Date Ceased Work and Date of Injury"))
                    return
                }else{
                    dateDeceased = msf760Rec.getDeathDate()
                }
            }
        }else{
            dateDeceased = '00000000'
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_DATE_DECEASED, dateDeceased)


        String tempReg = getEmployerABN_Z500(msf820Rec.getPayGroup())
        if(tempReg == null){
            return
        }

        injuryDate = msiwccDtl.get(MSIWCC.MSIWCC_DTL_DATE_INJURY)

        String employerABN = ""
        if (injuryDate.toLong() >= 20000701){
            employerABN = formatABN_Z550(tempReg)
        }else{
            employerABN = "00000000000"
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_EMPLOYER_ABN, employerABN)

        BigDecimal tariffRateNo = 0
        String wicRateNo = ""
        if(selfInsurerInd.trim() == 'Y'){
            if(injuryDate.toLong()>20010630){
                tariffRateNo = 0
                wicRateNo = msf530WicRateNo
            }else{
                if(msiwccDtl.get(MSIWCC.MSIWCC_DTL_DATE_CLAIM_ENT).toLong()>=19980101){
                    tariffRateNo = msf530TariffRateNo
                    wicRateNo = "000000"
                }else{
                    tariffRateNo = 0
                    wicRateNo = "000000"
                }
            }
        }else{
            if(batchParams.paramStartDate.toLong()>=20010630){
                tariffRateNo = 0
                wicRateNo = msf530WicRateNo
            }else{
                tariffRateNo = msf530TariffRateNo
                wicRateNo = "000000"
            }
        }

        DecimalFormat df3 = new DecimalFormat("##0")
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_TARIFF_RATE_NO, String.format("%3s",df3.format(tariffRateNo)).replaceAll(' ','0'))
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_WIC_RATE_NO, wicRateNo)

        if(tariffRateNo == 0 && wicRateNo.toLong()==0 && msiwccDtl.get(MSIWCC.MSIWCC_DTL_DATE_CLAIM_ENT).toLong()>=19980101){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536Rec.getPrimaryKey().getClaimNo(), "Tariff/WIC Rate Numbers both Zero"))
            return
        }

        formatFromRefCodes_F350(String.format("%-10s%-1s%-10s%-5s%-5s%-3s",
                msf536Rec.getIncidentNo(),'E',msf536EmployeeId, msf514Rec.getPrimaryKey().getNatureInjury(),msf514Rec.getPrimaryKey().getBodilyLoc(),''))

        if(sigInjDate.toLong() != 0){
            if(sigInjDate.toLong()>batchParams.paramEndDate.toLong() || sigInjDate.toLong()<injuryDate.toLong()){
                writeError(String.format(ERR_MSG_FORMAT, "", msf536Rec.getPrimaryKey().getClaimNo(), "Significant injury date invalid"))
                return
            }
        }
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_SIG_INJ_DATE, sigInjDate)
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_WRKR_TEL_NO, (wkrTelNo.trim().equals("") ? 'NA'.padRight(14) : wkrTelNo))

        /*
         * Additional initialisation
         */
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLAIM_FILLER_1, '')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_EMPLOYER_NAME, '')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_FILLER_2, '')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_CLMNT_FILLER_3, ' '.padRight(4))
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_FILLER_4, '')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_FILLER_5, '')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_FILLER_6, '')
        msiwccDtl.put(MSIWCC.MSIWCC_DTL_FILLER_7, '')

        writeMSIWCC_G100(msiwccDtl)

        claimBasRec++

        claimActivityData_F200(msf536Rec, msf012_536Data, msf537Rec, msf536InsClaimNo)

        totPayAmount=0
        totRecAmount=0
        totEstLiab=0
        totEstRecov=0
        totIncapHrs=0
        totIncapMins=0
        ctlDecAdjSet=0
        ctlInpCrNset=0

        if(msf536Rec.getClaimUserFldx3().trim().isNumber()){
            msf536EmpEntInpCr = msf536Rec.getClaimUserFldx3().trim().toLong()
        }else{
            msf536EmpEntInpCr = 0
        }

        partDay = false
        msf53aFound = false
        daysOffWork = 0
        msf53aEndDate = "00000000"

        if(isInsClaim){
            insClaimControl_F210(msf537Rec, msf514Rec.getResultInjury(), msf536InsClaimNo)
        }else{
            notInsClaimControl_F260(msf537Rec, msf536Rec.getPrimaryKey().getClaimNo(), msf536EmployeeId, msf514Rec.getResultInjury(), msf536InsClaimNo)
        }

        basicDetail2Data_F650(msf810Rec, msf510Rec, msf536InsClaimNo)
        workCapacityDetail_F800(msf536Rec, msf536InsClaimNo)
		claimControlData_F700(msf536InsClaimNo)
		
        if(batchParams.paramRunType.trim() == 'U' && isUpdateMSF012){
            updateMSF012_F130(msf012_536Data)
        }
    }

    private void updateMSF012_F130(MSF012_536Data msf012_536Data){
        info("updateMSF012_F130")

        try{
            MSF012Rec msf012Rec = edoi.findByPrimaryKey(msf012_536Data.getMSF012Key())
            msf012_536Data.setBringUpInd("")
            msf012Rec.setDataArea(msf012_536Data.toString())
            edoi.update(msf012Rec)
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to upadate MSO012 : " + e.getMessage())
        }
    }

    private void claimActivityData_F200(MSF536Rec msf536Rec, MSF012_536Data msf012_536Data, MSF537Rec msf537Rec, String msf536InsClaimNo){
        info("claimActivityData_F200")

        HashMap<String,String> msiwccAct = [:]
        msiwccAct.put(MSIWCC.MSIWCC_ACT_REC_TYPE, '2')
        msiwccAct.put(MSIWCC.MSIWCC_ACT_CLAIM_NO, msf536InsClaimNo)
        msiwccAct.put(MSIWCC.MSIWCC_ACT_REC_IDENT, '2')
        msiwccAct.put(MSIWCC.MSIWCC_ACT_CLOSED_CLAIM, (msf536Rec.getClaimStatus().trim() == '06'
                && msf536Rec.getDateClosed().toLong()<=batchParams.paramEndDate.toLong()) ? 'Y' : 'N')

        actClosedClaim = msiwccAct.get(MSIWCC.MSIWCC_ACT_CLOSED_CLAIM)

        if(msf536Rec.getClaimStatus().trim() == '06' && msf536Rec.getDateClosed().toLong()==0){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "No Closed Date for Closed Claim"))
            return
        }

        if(actClosedClaim.trim() == 'Y' && msf536Rec.getDateClosed().toLong()<msf536Rec.getDateReopen().toLong()){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Closed Date less than Reopen Date"))
            return
        }

        msiwccAct.put(MSIWCC.MSIWCC_ACT_CLOSED_DATE, msf536Rec.getDateClosed().toLong()<=batchParams.paramEndDate.toLong() ? msf536Rec.getDateClosed() : '00000000')

        if(msf536Rec.getDateReopen().toLong()>0 && msf536Rec.getDateClosed().toLong()==0){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Reopen Date without Closed Date"))
            return
        }

        if(msf536Rec.getDateReopen().toLong()>batchParams.paramEndDate.toLong()){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Reopen Date greater than Submission Date"))
            return
        }

        String actClosedDate = msiwccAct.get(MSIWCC.MSIWCC_ACT_CLOSED_DATE)
        msiwccAct.put(MSIWCC.MSIWCC_ACT_REOPEN_DATE, (msf536Rec.getDateReopen().toLong()>0 && actClosedDate.toLong()>0) ? msf536Rec.getDateReopen() : '00000000')

        String actReopenRsn = ""
        if(msf536Rec.getDateReopen().toLong()>0){
            if(msf536Rec.getReopenRsn().trim().equals("")){
                writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Reopen Reason Not Found"))
                return
            }else{
                actReopenRsn = msf536Rec.getReopenRsn().padRight(1).toString().substring(0, 1)
            }
        }else{
            actReopenRsn = "0"
        }

        String actReopenDate = msiwccAct.get(MSIWCC.MSIWCC_ACT_REOPEN_DATE)
        if(actReopenDate.toLong()==0){
            actReopenRsn = "0"
        }

        msiwccAct.put(MSIWCC.MSIWCC_ACT_REOPEN_RSN, actReopenRsn)

        if(msf012_536Data.getLiabStatus().trim().equals("")){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Claim Liability Not Found"))
            return
        }

        if(msf012_536Data.getLiabStatus().trim() == '6' && msf536Rec.getClaimStatus().trim() != '06'){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Null Claim (liability 6) must be closed"))
            return
        }

        if((msf012_536Data.getLiabStatus().trim() == '1' || msf012_536Data.getLiabStatus().trim() == '01')
        && actClosedClaim.trim() == 'Y'){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Liability cannot be 1 for closed claim"))
            return
        }

        String actWorkStatCode = ""
        if(msf537Rec.getHoursLost()>140 && (msf537Rec.getRetWrkCode().trim().equals("") || msf537Rec.getRetWrkCode().trim() == '00')){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Work Status Code Not Found"))
            return
        }else{
            if(msf537Rec.getRetWrkCode().trim().equals("")){
                actWorkStatCode = "00"
            }else{
                actWorkStatCode = msf537Rec.getRetWrkCode()
            }
        }
        msiwccAct.put(MSIWCC.MSIWCC_ACT_WORK_STAT_CODE, actWorkStatCode)

        if(msf012_536Data.getLiabStatus().trim() == '2' && msf537Rec.getHandleDate().toLong()==0 && msf536Rec.getDateReceived().toLong()>=19980101){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Liability Date Not Found"))
            return
        }

        msiwccAct.put(MSIWCC.MSIWCC_ACT_2ND_INJ_CLAIM, msf536Rec.getClaimUserFldx4().trim().equals("") ? 'N' : msf536Rec.getClaimUserFldx4())

        msiwccAct.put(MSIWCC.MSIWCC_ACT_NOTIFIER_CODE, msf012_536Data.getNotifCode().trim().equals("") ? '00' : String.format("%2s",msf012_536Data.getNotifCode()).replaceAll(' ', '0'))

        String statementDate = formatActivityRefs_F280(msf536Rec.getPrimaryKey().getDstrctCode()+msf536Rec.getPrimaryKey().getClaimNo())

        if(statementDate.toLong() != 0){
            if(statementDate.toLong()>batchParams.paramEndDate.toLong() || statementDate.toLong() < injuryDate.toLong()){
                writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Statement of claim date invalid"))
                return
            }
        }

        checkEstimate_F290(msf536Rec.getPrimaryKey().getClaimNo())

        if(commonLawEstAmt>0 && statementDate.toLong()==0){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Statement of claim date invalid"))
            return
        }

        addPayAmt_F295(msf536Rec.getPrimaryKey().getClaimNo())

        if(commonLawPayAmt>0 && statementDate.toLong()==0){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Statement of claim date invalid"))
            return
        }

        if(invPayDate!=0){
            if(statementDate.toLong()>invPayDate){
                writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Statement of claim date invalid"))
                return
            }
        }

        msiwccAct.put(MSIWCC.MSIWCC_ACT_STATEMENT_DATE, statementDate)

        getIncRefs_F360(msf536Rec.getPrimaryKey().getDstrctCode().padRight(4)+msf536Rec.getIncidentNo().padRight(10))

        String actNotifierCode = msiwccAct.get(MSIWCC.MSIWCC_ACT_NOTIFIER_CODE)
        if(msf536Rec.getDateReceived().toLong()>20030831){
            if((actNotifierCode.trim() == '02' || actNotifierCode.trim() == '05') && notifierName.trim().equals("")){
                writeError(String.format(ERR_MSG_FORMAT, "", msf536Rec.getIncidentNo(), "Notifier name is missing"))
                return
            }
        }

        msiwccAct.put(MSIWCC.MSIWCC_NOTIFIER_NAME, notifierName)
        msiwccAct.put(MSIWCC.MSIWCC_NOTIFIER_TEL_NO, notifierTelNo.trim().equals("") ? 'NA' : notifierTelNo)

        //        if(!validateDesc_F230(incDesc)){
        //            writeError(String.format(ERR_MSG_FORMAT, "", msf536Rec.getIncidentNo(), "Inc. description contains invalid chars"))
        //            return
        //        }
        //
        //        if(!validateDesc_F230(injDesc)){
        //            writeError(String.format(ERR_MSG_FORMAT, "", msf536Rec.getIncidentNo(), "Injury description has invalid chars"))
        //            return
        //        }

        msiwccAct.put(MSIWCC.MSIWCC_INCIDENT_DESC, incDesc)
        msiwccAct.put(MSIWCC.MSIWCC_INJURY_DESC, injDesc)
        msiwccAct.put(MSIWCC.MSIWCC_ACT_WORK_DATE, msf012_536Data.getWrkStatDate())
		msiwccAct.put(MSIWCC.MSIWCC_ACT_ESTIMATE_WPI, estimateWPI)
		
        String actWpiPercent = "000"
        if (msf536Rec.getDateRaised().toLong() < 20120619){
            actWpiPercent = msf012_536Data.getWpiPerc()
        }else{
            if(msf012_536Data.getWpiPerc().toLong() >= 10){
                actWpiPercent = msf012_536Data.getWpiPerc()
            }
        }
        msiwccAct.put(MSIWCC.MSIWCC_ACT_WPI_PERCENT, actWpiPercent)
        msiwccAct.put(MSIWCC.MSIWCC_ACT_WCC_MATTER_NO, msf012_536Data.getExtRefNo())

        String actWcaDate = ""
        String actWcaOutcome = ""

        String msf536EmployeeId = msf536Rec.getClaimEntity().padRight(10).toString().substring(0,10)
        MSF521Rec msf521Rec = workCapacityAssData_F205(msf536EmployeeId, msf536Rec.getPrimaryKey().getClaimNo())

        if (msf521Rec){
            if (msf521Rec.getPrimaryKey().getActivType().padRight(5).toString().substring(0, 3).trim() == 'WCA'){
                actWcaDate = inverseDate(msf521Rec.getPrimaryKey().getInvAcvDate())
                actWcaOutcome = msf521Rec.getPrimaryKey().getActivType().padRight(5).toString().substring(3, 5)
            }
        }else{
            actWcaDate = "00000000"
            actWcaOutcome = "00"
        }
        msiwccAct.put(MSIWCC.MSIWCC_ACT_WCA_DATE, actWcaDate)
        msiwccAct.put(MSIWCC.MSIWCC_ACT_WCA_OUTCOME, actWcaOutcome)

        /*
         * Additional initialisation
         */
        msiwccAct.put(MSIWCC.MSIWCC_ACT_FILLER_1, '')
        msiwccAct.put(MSIWCC.MSIWCC_ACT_FILLER_2, ' '.padRight(10))
        msiwccAct.put(MSIWCC.MSIWCC_ACT_FILLER_3, ' '.padRight(10))

        List<MSF012Rec> msf012Recs = getActivityRecs_F270(msf536Rec.getPrimaryKey().getClaimNo())

        String actLiabStatusDate = ""
        String actClaimLiabInd = ""
        String actExcuseCode = ""

        if (!msf012Recs.isEmpty()){

            msf012Recs.each {MSF012Rec msf012Rec ->
                MSF012_537Data msf012_537Data = new MSF012_537Data(msf012Rec)

                actLiabStatusDate = msf012_537Data.getLiabDate()
                actClaimLiabInd = msf012_537Data.getLiabStatus()

                if(msf012_537Data.getExcuseCode().trim().equals("")){
                    if(msf012_536Data.getExcuseCode().trim().equals("")){
                        actExcuseCode = "00"
                    }else{
                        actExcuseCode = msf012_536Data.getExcuseCode().trim().padLeft(2,'0')
                    }
                }else{
                    actExcuseCode = msf012_537Data.getExcuseCode().trim().padLeft(2,'0')
                }

                msiwccAct.put(MSIWCC.MSIWCC_ACT_LIAB_STATUS_DATE, actLiabStatusDate)
                msiwccAct.put(MSIWCC.MSIWCC_ACT_CLAIM_LIAB_IND, actClaimLiabInd)
                msiwccAct.put(MSIWCC.MSIWCC_ACT_EXCUSE_CODE, actExcuseCode)

                writeMSIWCC_G100(msiwccAct)

                claimActRec++
            }

        }else{
            actClaimLiabInd = msf012_536Data.getLiabStatus().trim().padLeft(2,'0')
            actLiabStatusDate = msf537Rec.getHandleDate()

            if(msf012_536Data.getExcuseCode().trim().equals("")){
                actExcuseCode = "00"
            }else{
                actExcuseCode = msf012_536Data.getExcuseCode().trim().padLeft(2,'0')
            }

            msiwccAct.put(MSIWCC.MSIWCC_ACT_LIAB_STATUS_DATE, actLiabStatusDate)
            msiwccAct.put(MSIWCC.MSIWCC_ACT_CLAIM_LIAB_IND, actClaimLiabInd)
            msiwccAct.put(MSIWCC.MSIWCC_ACT_EXCUSE_CODE, actExcuseCode)

            writeMSIWCC_G100(msiwccAct)

            claimActRec++
        }

    }

    private MSF521Rec workCapacityAssData_F205(String employeeId, String claimNo){
        info("workCapacityAssData_F205")

        Constraint cEmployeeId = MSF521Key.employeeId.equalTo(employeeId)
        Constraint cClaimNo = MSF521Rec.claimNo.equalTo(claimNo)
        Constraint cInvAcvDate = MSF521Key.invAcvDate.greaterThanEqualTo(" ")

        MSF521Rec msf521Rec = (MSF521Rec) edoi.firstRow(new QueryImpl(MSF521Rec.class).and(cEmployeeId).and(cClaimNo).and(cInvAcvDate))
        if(msf521Rec){
            return msf521Rec
        }

        return null
    }

    private void insClaimControl_F210(MSF537Rec msf537Rec, String msf514ResultInjury, String msf536InsClaimNo){
        info("insClaimControl_F210")
        arrayOfEstEntry = new ArrayList<EstEntry>()

        Constraint cInsClaimNo = MSF536Rec.insClaimNo.equalTo(msf536InsClaimNo)
        Constraint cClaimInd = MSF536Rec.claimInd.equalTo("E")
        Constraint cClaimNo = MSF536Key.claimNo.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF536Rec.class).and(cInsClaimNo).and(cClaimInd).and(cClaimNo).orderBy(MSF536Rec.aix4)

        edoi.search(query, MAX_ROW_READ, {MSF536Rec msf536Rec->
            pregetClaims_F220(msf536Rec)
        })

        writeClaimTimeLost_F320(msf537Rec, msf514ResultInjury, msf536InsClaimNo)

        if(prevRehabFlag.trim() == 'Y'){
            arrayOfRehabEntry.each {RehabEntry rehabEntry ->
                writeRehabRecs_F420(rehabEntry, msf536InsClaimNo)
            }
        }

        edoi.search(query, MAX_ROW_READ, {MSF536Rec msf536Rec->
            getPayments_F240(msf536Rec, msf536InsClaimNo)
        })

        if(actClosedClaim.trim() == 'N'){
            claimEst_F520(msf537Rec.getEstTimeOff(), msf537Rec.getTimeOffUnits(), msf536InsClaimNo)
        }
    }

    private void pregetClaims_F220(MSF536Rec msf536Rec){
        info("pregetClaims_F220")

        String claimStatus = getClaimStatus_C320(msf536Rec.getClaimStatus())
        if(claimStatus.trim() == 'N'){
            return
        }

        getDaysOff_F300(msf536Rec.getPrimaryKey().getClaimNo())

        if(prevRehabFlag.trim() == 'Y'){
            claimRehabData_F400(msf536Rec.getClaimEntity().padRight(10).toString().substring(0,10), msf536Rec.getPrimaryKey().getClaimNo())
        }

        if(actClosedClaim.trim() == 'N'){
            getInsEstimates_F500(msf536Rec.getPrimaryKey().getClaimNo())
        }

    }

    //    private boolean validateDesc_F230(String desc){
    //        info("validateDesc_F230 " + desc)
    //        String invalidChars = desc.replaceAll("/|[0-9]|[A-Z]|[a-z]|[,]|[.]|[!]|[%]|[_]|[-]|[']|[&]|[(]|[)]|[/]|[ ]|/",'')
    //        if (invalidChars?.trim()){
    //            info("Invalid chars found: " + invalidChars)
    //            return false
    //        }
    //        return true
    //    }

    private void getPayments_F240(MSF536Rec msf536Rec, String msf536InsClaimNo){
        info("getPayments_F240")

        String claimStatus = getClaimStatus_C320(msf536Rec.getClaimStatus())
        if(claimStatus.trim() == 'N'){
            return
        }

        claimPayRecovData_F600(msf536Rec.getPrimaryKey().getClaimNo(), msf536InsClaimNo)
    }

    private void notInsClaimControl_F260(MSF537Rec msf537Rec, String msf536ClaimNo, String msf536EmployeeId, String msf514ResultInjury, String msf536InsClaimNo){
        info("notInsClaimControl_F260")

        getDaysOff_F300(msf536ClaimNo)

        writeClaimTimeLost_F320(msf537Rec, msf514ResultInjury, msf536InsClaimNo)

        if(prevRehabFlag.trim() == 'Y'){
            claimRehabData_F400(msf536EmployeeId, msf536ClaimNo)
            arrayOfRehabEntry.each {RehabEntry rehabEntry ->
                writeRehabRecs_F420(rehabEntry, msf536InsClaimNo)
            }
        }

        claimPayRecovData_F600(msf536ClaimNo, msf536InsClaimNo)

        if(actClosedClaim.trim() == 'N'){
            writeClaimEst_F570(msf536ClaimNo, msf537Rec.getEstTimeOff(), msf537Rec.getTimeOffUnits(), msf536InsClaimNo)
        }
    }

    private List<MSF012Rec> getActivityRecs_F270(String msf536ClaimNo){
        info("createActivityRecs_F270")

        List<MSF012Rec> msf012Recs = new ArrayList<MSF012Rec>()

        Constraint cDataType = MSF012Key.dataType.equalTo('M')
        Constraint cKeyValueLow = MSF012Key.keyValue.greaterThanEqualTo('MSF537'+msf536ClaimNo.padRight(10)+startDate)
        Constraint cKeyValueHi = MSF012Key.keyValue.lessThanEqualTo('MSF537'+msf536ClaimNo.padRight(10)+batchParams.paramEndDate)

        def query = new QueryImpl(MSF012Rec.class).and(cDataType).and(cKeyValueLow).and(cKeyValueHi).orderBy(MSF012Rec.msf012Key)

        edoi.search(query, MAX_ROW_READ, { MSF012Rec msf012Rec->
            msf012Recs.add(msf012Rec)
        })

        return msf012Recs

    }

    private String formatActivityRefs_F280(String mss071EntityKey){
        info("formatActivityRefs_F280")

        String outputDate = "00000000"

        List<String> refCodes = runMSS071('B','CLA','1',mss071EntityKey,'110')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                outputDate = refCodes.get(0).padRight(8).toString().substring(0, 8)
                if(outputDate.toLong() > batchParams.paramEndDate.toLong()){
                    outputDate = "00000000"
                }
            }
        }

        return outputDate
    }

    private void checkEstimate_F290(String msf536ClaimNo){
        info("checkEstimate_F290")

        commonLawEstAmt = 0

        Constraint cClaimNo = MSF538Key.claimNo.equalTo(msf536ClaimNo)
        Constraint cEstType = MSF538Key.estType.equalTo("57")
        Constraint cInverseDate = MSF538Key.inverseDate.greaterThanEqualTo(invEndDate.toString())

        MSF538Rec msf538Rec = (MSF538Rec) edoi.firstRow(new QueryImpl(MSF538Rec.class).and(cClaimNo).and(cEstType).and(cInverseDate))
        if(msf538Rec){
            commonLawEstAmt = msf538Rec.getEstimateAmt()
        }
    }

    private void addPayAmt_F295(String msf536ClaimNo){
        info("addPayAmt_F295")

        commonLawPayAmt = 0
        invPayDate = 0

        Constraint cClaimNo = MSF539Key.claimNo.equalTo(msf536ClaimNo)
        Constraint cClaimTyp = MSF539Key.claimTyp.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF539Rec.class).and(cClaimNo).and(cClaimTyp).orderBy(MSF539Rec.msf539Key)

        edoi.search(query, MAX_ROW_READ, { MSF539Rec msf539Rec->
            if(msf539Rec.getPrimaryKey().getPayRecovCode().trim() == '19'
            || msf539Rec.getPrimaryKey().getPayRecovCode().trim() == '29'
            || msf539Rec.getPrimaryKey().getPayRecovCode().trim() == '30'
            || msf539Rec.getPrimaryKey().getPayRecovCode().trim() == '31'
            || msf539Rec.getPrimaryKey().getPayRecovCode().trim() == '32'){

                commonLawPayAmt = commonLawPayAmt + msf539Rec.getAmount()

                if(msf539Rec.getPrimaryKey().getInverseDate().toLong()>invPayDate){
                    invPayDate = msf539Rec.getPrimaryKey().getInverseDate().toLong()
                }
            }
        })
    }

    private void getDaysOff_F300(String msf536ClaimNo){
        info("getDaysOff_F300")

        boolean firstMSF53A = true

        Constraint cClaimNo = MSF53ARec.claimNo.equalTo(msf536ClaimNo)
        Constraint cInvStrDate = MSF53AKey.invStrDate.greaterThanEqualTo(invEndDate)
        Constraint cAbsAuth1 = MSF53ARec.absAuth.notEqualTo("PDC26")
        Constraint cAbsAuth2 = MSF53ARec.absAuth.notEqualTo("PDC87")
        Constraint cAbsAuth3 = MSF53ARec.absAuth.notEqualTo("PDO26")
        Constraint cAbsAuth4 = MSF53ARec.absAuth.notEqualTo("PDO87")
        Constraint cAbsAuth5 = MSF53ARec.absAuth.notEqualTo("SEC87")
        Constraint cAbsAuth6 = MSF53ARec.absAuth.notEqualTo("WCO26")
        Constraint cAbsAuth7 = MSF53ARec.absAuth.notEqualTo("WCO87")

        def query = new QueryImpl(MSF53ARec.class).and(cClaimNo).and(cInvStrDate).and(cAbsAuth1).and(cAbsAuth2).and(cAbsAuth3).and(cAbsAuth4).and(cAbsAuth5).and(cAbsAuth6).and(cAbsAuth7).orderBy(MSF53ARec.aix1)

        edoi.search(query, MAX_ROW_READ, {MSF53ARec msf53aRec->
            if(msf53aRec.getHoursLost()>=0){

                if(firstMSF53A){
                    msf53aEndDate = msf53aRec.getEndDate()
                    firstMSF53A = false
                }

                MSSDATLINK mssdatlnk = eroi.execute('MSSDAT', {MSSDATLINK mssdatlnk ->
                    mssdatlnk.setOption('2')
                    mssdatlnk.setDate1X(inverseDate(msf53aRec.getPrimaryKey().getInvStrDate()))
                    mssdatlnk.setDate2X(msf53aRec.getEndDate().toLong()>batchParams.paramEndDate.toLong() ? batchParams.paramEndDate : msf53aRec.getEndDate())
                })

                int mssdatlnkDays = mssdatlnk.getDays()
                if(mssdatlnkDays==0 && msf53aRec.getHoursLost()>0 && msf53aRec.getHoursLost()<7){
                    partDay = true
                }else{
                    mssdatlnkDays++
                }

                daysOffWork = daysOffWork + mssdatlnkDays
                msf53aFound = true
            }

        })

    }

    private void writeClaimTimeLost_F320(MSF537Rec msf537Rec, String msf514ResultInjury, String msf536InsClaimNo){
        info("writeClaimTimeLost_F320")

        if(msf537Rec.getDateCeased().toLong()>batchParams.paramEndDate.toLong() && daysOffWork==0){
            return
        }

        HashMap<String,String> msiwccLst = [:]
        msiwccLst.put(MSIWCC.MSIWCC_LST_REC_TYPE, '2')
        msiwccLst.put(MSIWCC.MSIWCC_LST_CLAIM_NO, msf536InsClaimNo)
        msiwccLst.put(MSIWCC.MSIWCC_LST_REC_IDENT, '3')

        if(msf537Rec.getDateCeased().toLong()==0 && msf514ResultInjury.trim() == '1'){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Date Ceased Work must be entered if result of injury is death"))
            return
        }

        if((msf537Rec.getDateCeased().toLong()==0 || msf537Rec.getDateCeased().toLong()>batchParams.paramEndDate.toLong()) && daysOffWork>0){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Date Ceased Work Not Found"))
            return
        }

        msiwccLst.put(MSIWCC.MSIWCC_LST_DATE_CEASED_WORK, msf537Rec.getDateCeased().toLong()<=batchParams.paramEndDate.toLong() ? msf537Rec.getDateCeased() : '00000000')

        msiwccLst.put(MSIWCC.MSIWCC_LST_EST_RES_DATE,
                (msf537Rec.getDateResumed().toLong()!=0 && msf537Rec.getDateResumed().toLong()<=batchParams.paramEndDate.toLong())
                ||(msf537Rec.getDeemFitDate().toLong()!=0 && msf537Rec.getDeemFitDate().toLong()<=batchParams.paramEndDate.toLong()) ? '00000000' : msf537Rec.getEstRetDate())

        msiwccLst.put(MSIWCC.MSIWCC_LST_FILLER_1, '00000000')

        if(msf537Rec.getDateResumed().toLong()>0 && msf537Rec.getDateCeased().toLong()>msf537Rec.getDateResumed().toLong()){
            writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Date Cease Work After Date Resume Work"))
            return
        }

        msiwccLst.put(MSIWCC.MSIWCC_LST_DATE_RESUMED_WORK, msf537Rec.getDateResumed().toLong()<=batchParams.paramEndDate.toLong()? msf537Rec.getDateResumed() : '00000000')

        calculateDaysOff_F321(msf537Rec)

        String lstDateCeasedWork = msiwccLst.get(MSIWCC.MSIWCC_LST_DATE_CEASED_WORK)
        if(daysOffWork==0 && lstDateCeasedWork.toLong()==0){
            return
        }

        DecimalFormat df5 = new DecimalFormat("####0")
        msiwccLst.put(MSIWCC.MSIWCC_LST_DAYS_OFF_WORK, df5.format(daysOffWork).padLeft(5).replace(' ','0'))

        writeMSIWCC_G100(msiwccLst)

        timeLostRec++
    }

    private void calculateDaysOff_F321(MSF537Rec msf537Rec){
        info("calculateDaysOff_F321")

        if(msf53aFound){
            if(msf53aEndDate.toLong()>=batchParams.paramEndDate.toLong()){
                return
            }
        }else{
            MSSDATLINK mssdatlnk = eroi.execute('MSSDAT', {MSSDATLINK mssdatlnk ->
                mssdatlnk.setOption('2')
                mssdatlnk.setDate1X(msf537Rec.getDateCeased())
                mssdatlnk.setDate2X((msf537Rec.getDateResumed().toLong()>batchParams.paramEndDate.toLong() || msf537Rec.getDateResumed().toLong() == 0) ?
                        batchParams.paramEndDate : msf537Rec.getDateResumed())
            })

            int mssdatlnkDays = mssdatlnk.getDays()
            if(msf537Rec.getDateResumed().toLong()==mssdatlnk.getDate2X().toLong() && mssdatlnkDays>1){
                mssdatlnkDays--
            }

            if(mssdatlnk.getDate1X().toLong()==mssdatlnk.getDate2X().toLong()){
                mssdatlnkDays++
            }

            daysOffWork = daysOffWork + mssdatlnkDays

            return
        }

        if (!(msf537Rec.getDateResumed().toLong()<=msf53aEndDate.toLong() && msf537Rec.getDateResumed().toLong() != 0)){

            MSSDATLINK mssdatlnk = eroi.execute('MSSDAT', {MSSDATLINK mssdatlnk ->
                mssdatlnk.setOption('2')
                mssdatlnk.setDate1X(msf53aEndDate)
                mssdatlnk.setDate2X((msf537Rec.getDateResumed().toLong()>batchParams.paramEndDate.toLong() || msf537Rec.getDateResumed().toLong() == 0) ?
                        batchParams.paramEndDate : msf537Rec.getDateResumed())
            })

            int mssdatlnkDays = mssdatlnk.getDays()
            if(msf537Rec.getDateResumed().toLong()==mssdatlnk.getDate2X().toLong()){
                mssdatlnkDays--
            }

            daysOffWork = daysOffWork + mssdatlnkDays
        }

        if(daysOffWork==0 && partDay){
            daysOffWork=1
        }
    }

    private void formatFromRefCodes_F350(String mss071EntityKey){
        info("formatFromRefCodes_F350")

        sigInjDate = "00000000"
        contactDate = "00000000"
        wkrCommDate = "00000000"
        injDesc = ""
        wkrTelNo = ""

        String injDesc1,injDesc2,injDesc3,injDesc4,injDesc5

        info("mss071EntityKey :"+mss071EntityKey)

        List<String> refCodes = runMSS071('B','INJ','1',mss071EntityKey,'100')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                sigInjDate=refCodes.get(0).padRight(8).toString().substring(0, 8)
            }
        }

        refCodes = runMSS071('B','INJ','1',mss071EntityKey,'105')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                contactDate=refCodes.get(0).padRight(8).toString().substring(0, 8)
            }
        }

        if(selfInsurerInd.trim() != 'Y'){
            refCodes = runMSS071('B','INJ','1',mss071EntityKey,'110')
            if (!refCodes.isEmpty()){
                if(refCodes.get(0)?.trim()){
                    wkrCommDate=refCodes.get(0).padRight(8).toString().substring(0, 8)
                }
            }
        }

        refCodes = runMSS071('B','INJ','1',mss071EntityKey,'115')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                wkrTelNo=refCodes.get(0)
            }
        }

        refCodes = runMSS071('B','INJ','5',mss071EntityKey,'120')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                injDesc1=refCodes.get(0)?refCodes.get(0):""
                injDesc2=refCodes.get(1)?refCodes.get(1):""
                injDesc3=refCodes.get(2)?refCodes.get(2):""
                injDesc4=refCodes.get(3)?refCodes.get(3):""
                injDesc5=refCodes.get(4)?refCodes.get(4):""
                injDesc = String.format("%-40s%-40s%-40s%-40s%-40s",injDesc1,injDesc2,injDesc3,injDesc4,injDesc5)
            }
        }

    }

    private void getIncRefs_F360(String mss071EntityKey){
        info("getIncRefs_F360")
        String incDesc1,incDesc2,incDesc3,incDesc4,incDesc5

        notifierName = ""
        notifierTelNo = ""
        incDesc = ""
		estimateWPI = ""

        List<String> refCodes = runMSS071('B','INC','1',mss071EntityKey,'100')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                notifierName = refCodes.get(0)
            }
        }

        refCodes = runMSS071('B','INC','1',mss071EntityKey,'101')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                notifierTelNo = refCodes.get(0)
            }
        }

        refCodes = runMSS071('B','INC','5',mss071EntityKey,'105')
        if (!refCodes.isEmpty()){
            if(refCodes.get(0)?.trim()){
                incDesc1=refCodes.get(0)?refCodes.get(0):""
                incDesc2=refCodes.get(1)?refCodes.get(1):""
                incDesc3=refCodes.get(2)?refCodes.get(2):""
                incDesc4=refCodes.get(3)?refCodes.get(3):""
                incDesc5=refCodes.get(4)?refCodes.get(4):""
                incDesc = String.format("%-40s%-40s%-40s%-40s%-40s",incDesc1,incDesc2,incDesc3,incDesc4,incDesc5)
            }
        }

		refCodes = runMSS071('B','INC','1',mss071EntityKey,'050')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				estimateWPI = refCodes.get(0)
			}
		}


    }

    private void claimRehabData_F400(String msf536EmployeeId, String msf536ClaimNo){
        info("claimRehabData_F400")

        Constraint cEmployeeId = MSF521Key.employeeId.equalTo(msf536EmployeeId)
        Constraint cClaimNo = MSF521Rec.claimNo.equalTo(msf536ClaimNo)
        Constraint cRehabProv = MSF521Rec.rehabProvider.notEqualTo(" ")
        Constraint cInvAcvDate = MSF521Key.invAcvDate.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF521Rec.class).and(cEmployeeId).and(cClaimNo).and(cRehabProv).and(cInvAcvDate).orderBy(MSF521Rec.msf521Key)

        edoi.search(query, MAX_ROW_READ, {MSF521Rec msf521Rec->

            RehabEntry rehabEntry = new RehabEntry()
            rehabEntry.setRehabProvider(msf521Rec.getRehabProvider())
            rehabEntry.setLastChangedDate(msf521Rec.getLastChanged())
            rehabEntry.setActivDate(inverseDate(msf521Rec.getPrimaryKey().getInvAcvDate()))
            rehabEntry.setServProvEndDate(msf521Rec.getFinalDate())

            MSF012Rec msf012Rec = readBatchInterface('M', ('MSF521' + msf521Rec.getPrimaryKey().getEmployeeId().padRight(10)
                    + msf521Rec.getPrimaryKey().getInvAcvDate().padRight(8) + msf521Rec.getPrimaryKey().getActivType()))

            if (msf012Rec){
                MSF012_521Data msf012_521Data = new MSF012_521Data(msf012Rec)
                rehabEntry.setServProvType(msf012_521Data.getServProvType())
                rehabEntry.setServProvSubType(msf012_521Data.getServProvSubt())
            }else{
                rehabEntry.setServProvType('00')
                rehabEntry.setServProvSubType('00')
            }
            rehabEntry.setServProvNullDt(msf521Rec.getRehUserFldx1().trim() == 'C' ? msf521Rec.getFinalDate() : '00000000')

            TableServiceReadReplyDTO ohr2 = readTable('OHR2', msf521Rec.getRehUserFldx2())
            if (ohr2){
                rehabEntry.setServProvABN(ohr2.getAssociatedRecord().padRight(11).toString().substring(0, 11))
            }else{
                rehabEntry.setServProvABN('00000000000')
            }

            arrayOfRehabEntry.add(rehabEntry)
        })

    }

    private void writeRehabRecs_F420(RehabEntry rehabEntry, String msf536InsClaimNo){
        info("writeRehabRecs_F420")

        if(rehabEntry.getLastChangedDate().toLong()<startDate.toLong() || rehabEntry.getLastChangedDate().toLong()>batchParams.paramEndDate.toLong()){
            return
        }

        HashMap<String,String> msiwccReh = [:]
        msiwccReh.put(MSIWCC.MSIWCC_REH_REC_TYPE, '2')
        msiwccReh.put(MSIWCC.MSIWCC_REH_CLAIM_NO, msf536InsClaimNo)
        msiwccReh.put(MSIWCC.MSIWCC_REH_REC_IDENT, '4')
        msiwccReh.put(MSIWCC.MSIWCC_REH_FILLER_1, '000')
        msiwccReh.put(MSIWCC.MSIWCC_REH_PROVIDER_CODE, rehabEntry.getRehabProvider().trim().isNumber() ?
                rehabEntry.getRehabProvider().trim().padLeft(4,'0') : rehabEntry.getRehabProvider())
        msiwccReh.put(MSIWCC.MSIWCC_REH_DATE_REF_PROVIDER, rehabEntry.getActivDate())
        msiwccReh.put(MSIWCC.MSIWCC_REH_SERV_PROV_END_DT, rehabEntry.getServProvEndDate())
        msiwccReh.put(MSIWCC.MSIWCC_REH_SERV_PROV_TYPE, rehabEntry.getServProvType())
        msiwccReh.put(MSIWCC.MSIWCC_REH_SERV_PROV_SUB_TYPE, rehabEntry.getServProvSubType())
        msiwccReh.put(MSIWCC.MSIWCC_REH_SERV_PROV_NULL_DT, rehabEntry.getServProvNullDt())
        msiwccReh.put(MSIWCC.MSIWCC_REH_SERV_PROV_ABN, rehabEntry.getServProvABN())

        writeMSIWCC_G100(msiwccReh)

        claimRehabRec++
    }

    private void getInsEstimates_F500(String msf536ClaimNo){
        info("getInsEstimates_F500")

        String msf538PrvEstType = ""

        Constraint cClaimNo = MSF538Key.claimNo.equalTo(msf536ClaimNo)
        Constraint cEstType = MSF538Key.estType.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF538Rec.class).and(cClaimNo).and(cEstType).orderBy(MSF538Rec.msf538Key)

        edoi.search(query, MAX_ROW_READ, {MSF538Rec msf538Rec->

            if (msf538Rec.getPrimaryKey().getEstType().trim() != msf538PrvEstType.trim()){
                msf538PrvEstType = msf538Rec.getPrimaryKey().getEstType()

                if(msf538Rec.getEstimateAmt()!=0){
                    getEstLiabRecov_F560(msf538Rec.getPrimaryKey().getEstType(), msf538Rec.getEstimateAmt(), msf538Rec.getPrimaryKey().getClaimNo())

                    EstEntry estEntry = null
                    arrayOfEstEntry.each{EstEntry estEnt ->
                        if (estEntry == null && estEnt.getEstType() == msf538Rec.getPrimaryKey().getEstType()){
                            estEntry = estEnt
                        }
                    }

                    if (estEntry){
                        arrayOfEstEntry.get(arrayOfEstEntry.indexOf(estEntry)).addEstAmt(msf538Rec.getEstimateAmt())
                    }else{
                        arrayOfEstEntry.add(new EstEntry(msf538Rec.getPrimaryKey().getEstType(), msf538Rec.getEstimateAmt()))
                    }
                }
            }

        })
    }

    private void claimEst_F520(BigDecimal msf537EstTimeOff, String msf537TimeOfUnits, String msf536InsClaimNo){
        info("claimEst_F520")
        arrayOfEstEntry.each {EstEntry estEntry ->
            setBuffClaimEstimates_F550(msf537EstTimeOff, msf537TimeOfUnits, msf536InsClaimNo, estEntry.getEstType(), estEntry.getEstAmt())
        }
    }

    private void setBuffClaimEstimates_F550(BigDecimal msf537EstTimeOff, String msf537TimeOfUnits, String msf536InsClaimNo, String estType, BigDecimal estAmt){
        info("setBuffClaimEstimates_F550")

        HashMap<String,String> msiwccEst = [:]
        msiwccEst.put(MSIWCC.MSIWCC_EST_REC_TYPE, '2')
        msiwccEst.put(MSIWCC.MSIWCC_EST_CLAIM_NO, msf536InsClaimNo)
        msiwccEst.put(MSIWCC.MSIWCC_EST_REC_IDENT, '6')

        BigDecimal estFutWksOff = 0
        if(estType.trim() == '50'){
            if(msf537TimeOfUnits.trim() == 'H'){
                estFutWksOff = msf537EstTimeOff / 35
            }else{
                if(msf537TimeOfUnits.trim() == 'D'){
                    estFutWksOff = msf537EstTimeOff / 7
                }else{
                    estFutWksOff = msf537EstTimeOff
                }
            }
        }

        DecimalFormat df4v1 = new DecimalFormat("###0.0")
        msiwccEst.put(MSIWCC.MSIWCC_EST_FUT_WKS_OFF_SIGN, estFutWksOff < 0 ? '-' : '+')
        String estmFutWksOff = df4v1.format(estFutWksOff.abs()).replace('.','')
        msiwccEst.put(MSIWCC.MSIWCC_EST_FUT_WKS_OFF, estmFutWksOff.padLeft(5).replaceAll(' ','0'))

        msiwccEst.put(MSIWCC.MSIWCC_EST_TYPE, estType)

        DecimalFormat df9v2 = new DecimalFormat("########0.00")
        msiwccEst.put(MSIWCC.MSIWCC_EST_AMOUNT_SIGN, estAmt < 0 ? '-' : '+')
        String estAmount = df9v2.format(estAmt.abs()).replace('.','')
        msiwccEst.put(MSIWCC.MSIWCC_EST_AMOUNT, estAmount.padLeft(11).replaceAll(' ','0'))

        writeMSIWCC_G100(msiwccEst)

        claimEstRec++
    }

    private void getEstLiabRecov_F560(String estType, BigDecimal estAmt, String claimNo){
        info("getEstLiabRecov_F560")

        TableServiceReadReplyDTO wcet = readTable('WCET', estType)
        if (wcet){
            String assocChar1 = wcet.getAssociatedRecord().padRight(1).toString().substring(0, 1)
            switch (assocChar1){
                case 'L':
                    totEstLiab = totEstLiab + estAmt
                    break
                case 'R':
                    totEstRecov = totEstRecov + estAmt
                    break
            }
        }
    }

    private void writeClaimEst_F570(String msf536ClaimNo, BigDecimal msf537EstTimeOff, String msf537TimeOfUnits, String msf536InsClaimNo){
        info("writeClaimEst_F570")

        String msf538PrvEstType = ""

        Constraint cClaimNo = MSF538Key.claimNo.equalTo(msf536ClaimNo)
        Constraint cInverseDate = MSF538Key.inverseDate.greaterThanEqualTo(invEndDate)
        Constraint cEstType = MSF538Key.estType.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF538Rec.class).and(cClaimNo).and(cInverseDate).and(cEstType).orderBy(MSF538Rec.msf538Key)

        edoi.search(query, MAX_ROW_READ, {MSF538Rec msf538Rec->
            if ((msf538Rec.getPrimaryKey().getEstType()?.trim() != msf538PrvEstType?.trim())){
                msf538PrvEstType = msf538Rec.getPrimaryKey().getEstType()
                if(msf538Rec.getEstimateAmt()!=0){
                    getEstLiabRecov_F560(msf538Rec.getPrimaryKey().getEstType(), msf538Rec.getEstimateAmt(),msf538Rec.getPrimaryKey().getClaimNo())
                    setBuffClaimEstimates_F550(msf537EstTimeOff, msf537TimeOfUnits, msf536InsClaimNo, msf538Rec.getPrimaryKey().getEstType(), msf538Rec.getEstimateAmt())
                }

            }

        })
    }

    private void claimPayRecovData_F600(String msf536ClaimNo, String msf536InsClaimNo){
        info("claimPayRecovData_F600")
        arrayOfPaymtRecovRec = new ArrayList<PaymtRecovRec>()

        Constraint cClaimNo = MSF539Key.claimNo.equalTo(msf536ClaimNo)
        Constraint cInverseDate = MSF539Key.inverseDate.greaterThanEqualTo(invEndDate)
        Constraint cClaimTyp = MSF539Key.claimTyp.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF539Rec.class).and(cClaimNo).and(cInverseDate).and(cClaimTyp).orderBy(MSF539Rec.msf539Key)

        edoi.search(query, MAX_ROW_READ, {MSF539Rec msf539Rec->
            processClaimPayRecovData(msf539Rec, msf536InsClaimNo)
        })
        
        if(!arrayOfPaymtRecovRec.isEmpty()){
            Collections.sort(arrayOfPaymtRecovRec)
            arrayOfPaymtRecovRec.each{PaymtRecovRec paymtRecov ->
                writePaymtRecov(paymtRecov)
            }
        }

    }
    
    private void writePaymtRecov(PaymtRecovRec paymtRecov){
        info ("writePaymtRecov")
        HashMap<String,String> msiwccPay = [:]
        msiwccPay.put(MSIWCC.MSIWCC_PAY_REC_TYPE, paymtRecov.recType)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_CLAIM_NO, paymtRecov.claimNo)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_REC_IDENT, paymtRecov.recIdent)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_TRANS_DATE, paymtRecov.transDate)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_ADJUST_FLAG, paymtRecov.adjustFlag)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_REC_AMOUNT_SIGN, paymtRecov.recAmountSign)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_REC_AMOUNT, paymtRecov.recAmount)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_PRD_START_DATE, paymtRecov.prdStartDate)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_PRD_END_DATE, paymtRecov.prdEndDate)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_HRS_TOT_INCAP_SIGN, paymtRecov.hrsTotIncapSign)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_HRS_TOT_INCAP, paymtRecov.hrsTotIncap)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_HRS_OTH_INCAP_SIGN, paymtRecov.hrsOthIncapSign)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_HRS_OTH_INCAP, paymtRecov.hrsOthIncap)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_SERV_DATE, paymtRecov.servDate)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_PAYEE_ID, paymtRecov.payeeId)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_SERV_PROV_ID, paymtRecov.servProvId)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_CLASS_NO, paymtRecov.classNo)
        msiwccPay.put(MSIWCC.MSIWCC_PAY_FILLER_1, '  ')
        msiwccPay.put(MSIWCC.MSIWCC_PAY_FILLER_2, ' '.padRight(12))
        writeMSIWCC_G100(msiwccPay)
    }

    private void basicDetail2Data_F650(MSF810Rec msf810Rec, MSF510Rec msf510Rec, String msf536InsClaimNo){
        info("basicDetail2Data_F650")

        HashMap<String,String> msiwccDt2 = [:]
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_REC_TYPE, '2')
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_CLAIM_NO, msf536InsClaimNo)
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_REC_IDENT, '7')
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_SURNAME, msf810Rec.getSurname())
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_GIVEN_NAMES, (msf810Rec.getFirstName().trim() + " " + msf810Rec.getSecondName().trim()))

        String dt2AccLocation = ""
        String tempIncUserField = msf510Rec.getIncUserFldx1().padRight(2).toString().substring(0, 2)
        if(tempIncUserField.trim() == '00'|| tempIncUserField.trim() == '01'){
            dt2AccLocation = "NA"
        }else{
            dt2AccLocation = msf510Rec.getAddressLine_1().trim() + " " + msf510Rec.getAddressLine_2().trim()
        }
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_ACC_LOCATION, dt2AccLocation)

        msiwccDt2.put(MSIWCC.MSIWCC_DT2_MOBILE_PHONE_NO, msf810Rec.getMobileNo().trim().equals("") ? 'NA' : msf810Rec.getMobileNo())
        msiwccDt2.put(MSIWCC.MSIWCC_DT2_WORK_PHONE_NO, msf810Rec.getWorkPhoneNo().trim().equals("") ? 'NA' : msf810Rec.getWorkPhoneNo())

        writeMSIWCC_G100(msiwccDt2)

        claimBas2Rec++
    }

    private void claimControlData_F700(String msf536InsClaimNo){
        info("claimControlData_F700")

        HashMap<String,String> msiwccCtl = [:]
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_REC_TYPE, '2')
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_CLAIM_NO, msf536InsClaimNo)
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_REC_IDENT, '9')

        DecimalFormat df9v2 = new DecimalFormat("########0.00")
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_PAY_TODATE_SIGN, totPayAmount < 0 ? '-' : '+')
        String payTodate = df9v2.format(totPayAmount.abs()).replace('.','')
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_PAY_TODATE, payTodate.padLeft(11).replaceAll(' ','0'))

        msiwccCtl.put(MSIWCC.MSIWCC_CTL_REC_TODATE_SIGN, totRecAmount < 0 ? '-' : '+')
        String recTodate = df9v2.format(totRecAmount.abs()).replace('.','')
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_REC_TODATE, recTodate. padLeft(11).replaceAll(' ','0'))

        Long hours = (Long) (totIncapMins / 60)
        BigDecimal mins = (BigDecimal) (totIncapMins - (hours*60))

        DecimalFormat df8= new DecimalFormat("#######0")
        BigDecimal ctlHrsTotalIncap = ((totIncapHrs + hours) * 100) + mins
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_HRS_TOT_INCAP_SIGN, ctlHrsTotalIncap < 0 ? '-' : '+')
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_HRS_TOT_INCAP, df8.format(ctlHrsTotalIncap.abs()).padLeft(8).replaceAll(' ','0'))

        msiwccCtl.put(MSIWCC.MSIWCC_CTL_TOT_EST_LIAB_SIGN, totEstLiab < 0 ? '-' : '+')
        String totEstLiab = df9v2.format(totEstLiab.abs()).replace('.','')
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_TOT_EST_LIAB, totEstLiab.padLeft(11).replaceAll(' ','0'))

        msiwccCtl.put(MSIWCC.MSIWCC_CTL_TOT_EST_RECOV_SIGN, totEstRecov < 0 ? '-' : '+')
        String totEstRecov = df9v2.format(totEstRecov.abs()).replace('.','')
        msiwccCtl.put(MSIWCC.MSIWCC_CTL_TOT_EST_RECOV, totEstRecov.padLeft(11).replaceAll(' ','0'))

        if(actClosedClaim.trim() == 'Y'){
            if(totEstLiab!=0 || totEstRecov!=0){
                writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Claim Estimates Must Be Removed"))
            }
        }

        writeMSIWCC_G100(msiwccCtl)

        claimCtlRec++
    }

	private void workCapacityDetail_F800(MSF536Rec msf536Rec, String msf536InsClaimNo){
		info("workCapacityDetail_F800")

		/*
		 * Work Capacity Details are held in Reference Codes against the Incident
		 * (Reference No.s 051-056 are defined for this purpose)
		 * 
		 * Example:
		 * --------
		 * 051   Original Decision Date    20120710
		 * 052   WC Decision Type          01
		 * 053   WC Review Stage           05
		 * 054   Date Type                 03
		 * 055   Transaction Date          20120718
		 * 056   WC Outcome                04
		 * 
		 */
	  
		String wcdOriginalDecisionDate = "00000000"
		String wcdDecisionType = ""
		String wcdReviewStage = ""
		String wcdDateType = ""
		String wcdTransactionDate = "00000000"
		String wcdOutcome = ""
		List<String> refCodes
		
		String mss071EntityKey = msf536Rec.getPrimaryKey().getDstrctCode().padRight(4) + msf536Rec.getIncidentNo().padRight(10)

		// get Original Decision Date
		refCodes = runMSS071('B','INC','1',mss071EntityKey,'051')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				wcdOriginalDecisionDate = refCodes.get(0).padRight(8).toString().substring(0, 8)
			}
		}
		// get Decision Type
		refCodes = runMSS071('B','INC','1',mss071EntityKey,'052')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				wcdDecisionType = refCodes.get(0)
			}
		}
		// get Review Stage
		refCodes = runMSS071('B','INC','1',mss071EntityKey,'053')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				wcdReviewStage = refCodes.get(0)
			}
		}
		// get Date Type
		refCodes = runMSS071('B','INC','1',mss071EntityKey,'054')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				wcdDateType = refCodes.get(0)
			}
		}
		// get Transaction Date
		refCodes = runMSS071('B','INC','1',mss071EntityKey,'055')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				wcdTransactionDate = refCodes.get(0).padRight(8).toString().substring(0, 8)
			}
		}
		// get Outcome
		refCodes = runMSS071('B','INC','1',mss071EntityKey,'056')
		if (!refCodes.isEmpty()){
			if(refCodes.get(0)?.trim()){
				wcdOutcome = refCodes.get(0)
			}
		}

		// Validate the wcd values retrieved from Reference Code data
		if(wcdOriginalDecisionDate.toLong() != 0){
			
			info("Original Decision Date: " + wcdOriginalDecisionDate)
			info("Transaction Date: " + wcdTransactionDate)
			info("Submission End Date: " + batchParams.paramEndDate)
			info("Claim Received Date: " + msf536Rec.getDateReceived())
			
			if(wcdOriginalDecisionDate.toLong() > batchParams.paramEndDate.toLong()){
				writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Original decision date must be less than submission end date"))
				return
			}
			if(wcdOriginalDecisionDate.toLong() < msf536Rec.getDateReceived().trim().toLong()){
				writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Original decision date must be greater or equal to date claim entered on agent/insurer system"))
				return
			}
			if(wcdTransactionDate.toLong() > batchParams.paramEndDate.toLong()){
				writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Transaction date must be less than submission end date"))
				return
			}
			if(wcdTransactionDate.toLong() < msf536Rec.getDateReceived().trim().toLong()){
				writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Transaction date must be greater or equal to date claim entered on agent/insurer system"))
				return
			}
			if(wcdReviewStage.trim() == '01'){
				if((wcdDateType.trim() != '01') && (wcdDateType.trim() != '05')){
					wcdDateType = '99'
				}
			}
			if(!wcdDecisionType.trim().equals("")){
				if((wcdDecisionType.trim() != '01') 
						&& (wcdDecisionType.trim() != '02')
						&& (wcdDecisionType.trim() != '03')
						&& (wcdDecisionType.trim() != '04')
						&& (wcdDecisionType.trim() != '05')
						&& (wcdDecisionType.trim() != '06')
						&& (wcdDecisionType.trim() != '07')){
					writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Invalid Work Capacity Decision Type"))
					return
				}
			}
			if(!wcdReviewStage.trim().equals("")){
				if((wcdReviewStage.trim() != '01')
						&& (wcdReviewStage.trim() != '02')
						&& (wcdReviewStage.trim() != '03')
						&& (wcdReviewStage.trim() != '04')){
					writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Invalid Work Capacity Review Stage"))
					return
				}
			}
			if(wcdReviewStage.trim() == '01'){
				if(wcdOutcome.trim() != '11'){
					writeError(String.format(ERR_MSG_FORMAT, "", msf536InsClaimNo, "Work Capacity Outcome Must be 11 where Review Stage is 01"))
					return
				}
			}

		    // Write the WCD record
			HashMap<String,String> msiwccWcd = [:]
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_REC_TYPE, '2')
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_CLAIM_NO, msf536InsClaimNo)
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_REC_IDENT, '8')
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_ORIG_DEC_DATE, wcdOriginalDecisionDate)
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_DECISION_TYPE, wcdDecisionType)
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_REVIEW_STAGE, wcdReviewStage)
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_DATE_TYPE, wcdDateType)
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_TRANS_DATE, wcdTransactionDate)
			msiwccWcd.put(MSIWCC.MSIWCC_WCD_OUTCOME, wcdOutcome)
			writeMSIWCC_G100(msiwccWcd)
	
			workCapacityRec++
		}
	}

    private void processClaimPayRecovData(MSF539Rec msf539Rec, String msf536InsClaimNo){
        info("processClaimPayRecovData")
        PaymtRecovRec paymtRecov = new PaymtRecovRec()
        paymtRecov.recType = '2'
        paymtRecov.claimNo = msf536InsClaimNo
        paymtRecov.recIdent = '5'
        paymtRecov.transDate = inverseDate(msf539Rec.getPrimaryKey().getInverseDate())
        paymtRecov.adjustFlag = 'N'
        
        DecimalFormat df8v2 = new DecimalFormat("#######0.00")
        paymtRecov.recAmountSign = msf539Rec.getWcPaymentAmt() < 0 ? '-' : '+'
        String payRecAmount = df8v2.format(msf539Rec.getWcPaymentAmt().abs()).padLeft(11).replaceAll(' ', '0')
        paymtRecov.recAmount = payRecAmount.replace('.','')

        String payPrdStartDate, payPrdEndDate
        String payRecovCode = msf539Rec.getPrimaryKey().getPayRecovCode()
        if(payRecovCode.trim() == '13'||payRecovCode.trim() == '14'||payRecovCode.trim() == '15'||payRecovCode.trim() == '16'){
            payPrdStartDate = msf539Rec.getServStrDate()
            payPrdEndDate = msf539Rec.getServEndDate()
        }
        paymtRecov.prdStartDate = payPrdStartDate ? payPrdStartDate : '00000000'
        paymtRecov.prdEndDate = payPrdEndDate ? payPrdEndDate : '00000000'

        BigDecimal hrMins, minsRound
        Long hours, mins

        Long payHrsTotIncap = 0
        String payHrsTotIncapSign = '+'
        if(payRecovCode.trim() == '14'||payRecovCode.trim() == '15'){
            hours = (Long) msf539Rec.getHoursMins()
            hrMins = msf539Rec.getHoursMins() - hours

            /*
             * Note:
             * Insurer 279 is Transgrid
             * Insurer 285 is Delta
             */

            if(msf539Rec.getPrimaryKey().getInverseDate().toLong()>79898898){
                mins = hrMins * 60
                minsRound = hrMins * 60
                minsRound = minsRound.setScale(0,BigDecimal.ROUND_HALF_DOWN)
                payHrsTotIncap = (hours * 100) + mins
            }else{
                mins = hrMins * 100
                minsRound = hrMins * 100
                minsRound = minsRound.setScale(0,BigDecimal.ROUND_HALF_DOWN)
                payHrsTotIncap = msf539Rec.getHoursMins() * 100
            }

            if (msf539Rec.getHoursMins() < 0){
                payHrsTotIncapSign = '-'
            }

            totIncapHrs = totIncapHrs + hours
            totIncapMins = totIncapMins + minsRound
        }

        DecimalFormat df6 = new DecimalFormat("#####0")
        paymtRecov.hrsTotIncapSign = payHrsTotIncapSign
        paymtRecov.hrsTotIncap = df6.format(payHrsTotIncap.abs()).padLeft(6).replaceAll(' ', '0')

        Long payHrsOthIncap = 0
        String payHrsOthIncapSign = '+'
        if(payRecovCode.trim() == '13'||payRecovCode.trim() == '16'){
            payHrsOthIncap = msf539Rec.getHoursMins() * 100
            if (msf539Rec.getHoursMins() < 0){
                payHrsOthIncapSign = '-'
            }
        }

        paymtRecov.hrsOthIncapSign = payHrsOthIncapSign
        paymtRecov.hrsOthIncap = df6.format(payHrsOthIncap.abs()).padLeft(6).replaceAll(' ', '0')
        paymtRecov.servDate = msf539Rec.getServStrDate()


        MSF012Rec msf012Rec = readBatchInterface('M', ('MSF539' + msf539Rec.getPrimaryKey().getClaimNo().padRight(10)
                + msf539Rec.getPrimaryKey().getClaimTyp().padRight(1) + msf539Rec.getPrimaryKey().getPayRecovCode().padRight(2)
                + msf539Rec.getPrimaryKey().getInverseDate().padRight(8) + msf539Rec.getPrimaryKey().getInverseSeqNo().padRight(3)))

        MSF012_539Data msf012_539Data = msf012Rec ? new MSF012_539Data(msf012Rec) : new MSF012_539Data()
        paymtRecov.payeeId = msf012_539Data.getPayeeId()
        paymtRecov.servProvId = msf012_539Data.getProviderId()
        paymtRecov.classNo = msf012_539Data.getClassNo()

        if(msf539Rec.getPrimaryKey().getInverseDate().toLong() >= invEndDate.toLong()
        && msf539Rec.getPrimaryKey().getInverseDate().toLong() <= invTestStartDate.toLong()){

            arrayOfPaymtRecovRec.add(paymtRecov)
            payRecovRec++
            gtotWcPayRec = gtotWcPayRec + msf539Rec.getWcPaymentAmt()
            if(msf539Rec.getPrimaryKey().getClaimTyp().trim() == 'P'){
                gtotWcPay = gtotWcPay + msf539Rec.getWcPaymentAmt()
            }else{
                gtotWcRec = gtotWcRec + msf539Rec.getWcPaymentAmt()
            }
        }

        if(msf539Rec.getPrimaryKey().getClaimTyp().trim() == 'P'){
            totPayAmount = totPayAmount + msf539Rec.getWcPaymentAmt()
        }else{
            totRecAmount = totRecAmount + msf539Rec.getWcPaymentAmt()
        }

        if(msf539Rec.getPrimaryKey().getClaimTyp() == 'R' || msf539Rec.getPrimaryKey().getClaimTyp().trim().equals("")){
            return
        }

        MSF533Rec msf533Rec = readPaymentType(msf539Rec.getPrimaryKey().getPayRecovCode())
        if (msf533Rec == null){
            return
        }

        if(msf533Rec.getPayUserFldx1().trim() == 'Y'){
            if(msf536EmpEntInpCr==0 || msf536EmpEntInpCr==100){
                ctlDecAdjSet = 0
            }else{
                ctlDecAdjSet = ctlDecAdjSet + (msf539Rec.getAtaxAmountL() * msf536EmpEntInpCr / 100)
            }
        }else{
            ctlInpCrNset = ctlInpCrNset + (msf539Rec.getAtaxAmountL() * msf536EmpEntInpCr / 100)
        }


    }

    private void writeMSIWCC_G100(HashMap<String,String> msiwcc) {
        info("writeMSIWCC_G100")
        if (msiwcc.get(MSIWCC.MSIWCC_HDR_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCHeader(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_DTL_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCDetail(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_ACT_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCActivity(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_LST_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCLostTime(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_REH_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCRehab(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_PAY_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCPayment(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_EST_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCEstimates(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_DT2_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCDetail2(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_CTL_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCControl(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_WCD_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCWorkCapacity(msiwcc))
        } else if (msiwcc.get(MSIWCC.MSIWCC_TLR_REC_TYPE)){
            arrayOfMSIWCC.add(getMSIWCCTrailer(msiwcc))
        }
    }

    private MSF537Rec readWCDetail_Z200(String claimNo){
        info("readWCDetail_Z200")

        try{
            MSF537Rec msf537Rec = edoi.findByPrimaryKey(new MSF537Key(claimNo))
            return msf537Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Workers Comp ${claimNo} from MSF537 : " + e.getMessage())
        }

        return null
    }

    private MSF012_536Data readMSF012_Z220(MSF536Rec msf536Rec){
        info("readMSF012_Z220")

        MSF012Rec msf012Rec = readBatchInterface('M', 'MSF536' + msf536Rec.getPrimaryKey().getDstrctCode().padRight(4)
                + msf536Rec.getPrimaryKey().getClaimNo().padRight(10))

        return (msf012Rec ? new MSF012_536Data(msf012Rec) :new MSF012_536Data())
    }

    private MSF760Rec readPersonelRecord_Z250(String employeeId){
        info("readPersonelRecord_Z250")

        try{
            MSF760Rec msf760Rec = edoi.findByPrimaryKey(new MSF760Key(employeeId))
            return msf760Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Personel ${employeeId} : " + e.getMessage())
        }

        return null
    }

    private MSF510Rec readIncidentRecord_Z300(String district, String incidentNo){
        info("readIncidentRecord_Z300")

        try{
            MSF510Rec msf510Rec = edoi.findByPrimaryKey(new MSF510Key(district, incidentNo))
            return msf510Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Incident record ${incidentNo} : " + e.getMessage())
        }

        return null
    }

    private MSF514Rec readIncidentInjuryRec_Z350(String incidentNo, String personData){
        info("readIncidentInjuryRec_Z350")

        Constraint cIncidentNo = MSF514Key.incidentNo.equalTo(incidentNo)
        Constraint cPersonData = MSF514Key.personData.equalTo(personData)
        MSF514Rec msf514Rec = (MSF514Rec) edoi.firstRow(new QueryImpl(MSF514Rec.class).and(cIncidentNo.and(cPersonData)))

        if(msf514Rec){
            return msf514Rec
        }

        return null
    }

    private MSF810Rec readEmployeeRecord_Z400(String employeeId){
        info("readEmployeeRecord_Z400")

        try{
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(employeeId))
            return msf810Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Employee ${employeeId} : " + e.getMessage())
        }

        return null
    }

    private MSF820Rec readPayrollRecord_Z450(String employeeId){
        info("readPayrollRecord_Z450")

        try{
            MSF820Rec msf820Rec = edoi.findByPrimaryKey(new MSF820Key(employeeId))
            return msf820Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Payroll for employee ${employeeId} : " + e.getMessage())
        }

        return null
    }

    private String getEmployerABN_Z500(String payGroup){
        info("getEmployerABN_Z500")

        String msf801GrpTaxNoPg, msf801DstrctCodePg
        MSF002_SC3801Rec msf002_SC3801Rec
        MSF000_DC0020Rec msf000_DC0020Rec

        try{
            MSF801_PG_801Rec msf801PgRec = edoi.findByPrimaryKey(new MSF801_PG_801Key("PG", payGroup))
            msf801GrpTaxNoPg = msf801PgRec.getGrpTaxNoPg()
            msf801DstrctCodePg = msf801PgRec.getDstrctCodePg()
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            if(payGroup.trim() != '****'){
                writeError(String.format(ERR_MSG_FORMAT, "", payGroup, "Employee Pay Group Invalid"))
                return null
            }else{
                msf801GrpTaxNoPg = ""
                msf801DstrctCodePg = ""
            }
        }

        if(!msf801GrpTaxNoPg.trim().equals("")){
            return msf801GrpTaxNoPg
        }

        if(msf801DstrctCodePg.trim().equals("")){
            if(batchParams.paramDistrict.trim().equals("")){
                msf002_SC3801Rec = readMSF002_SC3801Rec()
                if (msf002_SC3801Rec == null){
                    writeError(String.format(ERR_MSG_FORMAT, "", batchParams.paramDistrict, "Default District Not Found"))
                    return null
                }

                msf000_DC0020Rec = readMSF000_DC0020Rec(msf002_SC3801Rec.getDefaultDstrct())
                if (msf000_DC0020Rec == null){
                    writeError(String.format(ERR_MSG_FORMAT, "", batchParams.paramDistrict, "Employer ABN Not Found"))
                    return null
                }
            }else{
                msf000_DC0020Rec = readMSF000_DC0020Rec(batchParams.paramDistrict)
                if (msf000_DC0020Rec == null){
                    writeError(String.format(ERR_MSG_FORMAT, "", batchParams.paramDistrict, "Employer ABN Not Found"))
                    return null
                }
            }
        }else{
            msf000_DC0020Rec = readMSF000_DC0020Rec(msf801DstrctCodePg)
            if (msf000_DC0020Rec == null){
                writeError(String.format(ERR_MSG_FORMAT, "", batchParams.paramDistrict, "Employer ABN Not Found"))
                return null
            }
        }

        if (msf000_DC0020Rec){
            return msf000_DC0020Rec.getTaxRegNo()
        }

        return null

    }

    private String formatABN_Z550(String inputString){
        info("formatABN_Z550")

        String outputString = ""

        char[] c = inputString.padRight(20).toString().toCharArray()
        int indexReg = 19
        for(int indexABN=10;indexABN>= 0;indexABN--){
            if(Character.isDigit(c[indexReg])){
                outputString = c[indexReg].toString()+outputString
            }else{
                outputString = " " + outputString
            }
            indexReg--
        }

        return outputString
    }

    private MSF878Rec readPositionMSF878_Z600(String employeeId, String invStartDate, String posStopDate){
        info("readPositionMSF878_Z600")

        Constraint cEmployeeId = MSF878Key.employeeId.equalTo(employeeId)
        Constraint cPrimaryPos = MSF878Key.primaryPos.equalTo("0")
        Constraint cInvStartDate = MSF878Key.invStrDate.greaterThanEqualTo(invStartDate)
        Constraint cPosStopDate1 = MSF878Key.posStopDate.greaterThanEqualTo(posStopDate)
        Constraint cPosStopDate2 = MSF878Key.posStopDate.equalTo("00000000")
        Constraint cPositionId = MSF878Key.positionId.greaterThanEqualTo(" ")

        MSF878Rec msf878Rec = edoi.firstRow(new QueryImpl(MSF878Rec.class).and(cEmployeeId).and(cPrimaryPos).and(cInvStartDate).and(cPosStopDate1.or(cPosStopDate2)).and(cPositionId))

        if (msf878Rec){
            return msf878Rec
        }else{
            msf878Rec = getCurrentPosition_Z620(employeeId)
            if(msf878Rec){
                return msf878Rec
            }
        }

        info("Position not found")
        return null
    }

    private MSF878Rec getCurrentPosition_Z620(String employeeId){
        info("getCurrentPosition_Z620")

        Constraint cEmployeeId = MSF878Key.employeeId.equalTo(employeeId)
        Constraint cPrimaryPos = MSF878Key.primaryPos.equalTo("0")
        Constraint cNotTermEmployee = MSF878Key.positionId.notEqualTo(commarea.getProperty("TerminatePos"))
        Constraint cNotSuspendPos = MSF878Key.positionId.notEqualTo(commarea.getProperty("SuspendPos"))
        Constraint cInvStartDate = MSF878Key.invStrDate.greaterThanEqualTo(" ")

        MSF878Rec msf878Rec = edoi.firstRow(new QueryImpl(MSF878Rec.class).and(cEmployeeId).and(cPrimaryPos).and(cNotTermEmployee).and(cNotSuspendPos).and(cInvStartDate))
        if(msf878Rec){
            return msf878Rec
        }

        info("Current Position not found")
        return null
    }

    private MSF531Rec readWCompLocCodes_Z650(String wcLocation){
        info("readWCompLocCodes_Z650")

        try{
            MSF531Rec msf531Rec = edoi.findByPrimaryKey(new MSF531Key(wcLocation))
            return msf531Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Workers Comp Location Code ${wcLocation} from MSF531 : " + e.getMessage())
        }

        return null
    }

    private MSF000_DC0004Rec readMSF000_DC0004Rec_Z750(String district){
        info("readMSF000_DC0004Rec_Z750")

        try{
            MSF000_DC0004Rec msf000_DC0004Rec = edoi.findByPrimaryKey(new MSF000_DC0004Key(district, "DC", "0004"))
            return msf000_DC0004Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read district ${} from MSF000_DC0004 + " + e.getMessage())
        }

        return null
    }

    private MSF513Rec getCauseFromMSF513_Z1000(MSF536Rec msf536Rec){
        info("getCauseFromMSF513_Z1000")

        MSF513Rec outMSF513Rec = null
        Constraint cDistrict = MSF513Key.dstrctCode.equalTo(msf536Rec.getPrimaryKey().getDstrctCode())
        Constraint cIncindentNo = MSF513Key.incidentNo.equalTo(msf536Rec.getIncidentNo())
        Constraint cImmNature = MSF513Key.immNature.equalTo('1')
        Constraint cImmCauseCode = MSF513Key.immCauseCode.greaterThanEqualTo(" ")

        def query = new QueryImpl(MSF513Rec.class).and(cDistrict).and(cIncindentNo).and(cImmNature).and(cImmCauseCode).orderBy(MSF513Rec.msf513Key)

        edoi.search(query, MAX_ROW_READ, { MSF513Rec msf513Rec->

            if((msf536Rec.getDateReceived().toLong()>20110630
            && msf513Rec.getPrimaryKey().getImmCauseCode().padRight(5).toString().substring(4,5) == 'C'
            && msf513Rec.getPrimaryKey().getImmCauseCode().padRight(4).toString().substring(0,4).trim().isNumber())
            ||(msf536Rec.getDateReceived().toLong()<20110701
            && msf513Rec.getPrimaryKey().getImmCauseCode().padRight(5).toString().substring(4,5) == 'B'
            && msf513Rec.getPrimaryKey().getImmCauseCode().padRight(3).toString().substring(0,3).trim().isNumber())){
                outMSF513Rec = msf513Rec
            }
        })

        return outMSF513Rec
    }

    private MSF000_DC0020Rec readMSF000_DC0020Rec(String district){
        info("readMSF000_DC0004Rec")

        try{
            MSF000_DC0020Rec msf000_DC0020Rec = edoi.findByPrimaryKey(new MSF000_DC0020Key(district,"DC","0020"))
            return msf000_DC0020Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read from MSF000_DC0020 for district ${district} : " + e.getMessage())
        }

        return null
    }

    private MSF002_SC3801Rec readMSF002_SC3801Rec(){
        info("readMSF002_DC3801Rec")

        try{
            MSF002_SC3801Rec msf002_SC3801Rec = edoi.findByPrimaryKey(new MSF002_SC3801Key("    ", "SC", "3801"))
            return msf002_SC3801Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Default district from MSF002_SC3801 : " + e.getMessage())
        }
        return null
    }

    private String getMSIWCCHeader(HashMap<String,String> msiwcc) {
        info("getMSIWCCHeader")
        String msiwccHdr = String.format("%1s%-3s%-6s%-2s%-8s%-8s%-872s",
                msiwcc.get(MSIWCC.MSIWCC_HDR_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_HDR_INSURER_NO),
                msiwcc.get(MSIWCC.MSIWCC_HDR_SUBMISSION_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_HDR_CLAIM_REL_NO),
                msiwcc.get(MSIWCC.MSIWCC_HDR_SUBMIT_START),
                msiwcc.get(MSIWCC.MSIWCC_HDR_SUBMIT_END),
                "")
        return msiwccHdr
    }

    private String getMSIWCCDetail(HashMap<String,String> msiwcc) {
        info("getMSIWCCDetail")
        String msiwccDtl = String.format("%1s%-19s%1s%-19s%1s%-7s%-20s%8s%8s%-19s%8s%3s%-75s%9s%-40s%-120s%-30s%4s%1s%8s%4s%4s%1s%4s%2s%2s%1s%1s%1s%4s%1s%7s%1s%-120s%-30s%4s%4s%4s%5s%2s%-120s%-30s%4s%8s%4s%3s%3s%2s%3s%1s%8s%-11s%6s%-19s%3s%8s%-8s%-8s%14s%4s%4s%-26s",
                msiwcc.get(MSIWCC.MSIWCC_DTL_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_DTL_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLAIM_FILLER_1),
                msiwcc.get(MSIWCC.MSIWCC_DTL_SHARED_CLAIM_CODE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_ERR_RPT_TARGET),
                msiwcc.get(MSIWCC.MSIWCC_DTL_INSURER_BRANCH),
                msiwcc.get(MSIWCC.MSIWCC_DTL_DATE_CLAIM_ENT),
                msiwcc.get(MSIWCC.MSIWCC_DTL_DATE_CLAIM_LOD),
                msiwcc.get(MSIWCC.MSIWCC_DTL_POLICY_NO),
                msiwcc.get(MSIWCC.MSIWCC_DTL_PRD_COMMENCE_DATE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_TARIFF_RATE_NO),
                msiwcc.get(MSIWCC.MSIWCC_DTL_EMPLOYER_NAME),
                msiwcc.get(MSIWCC.MSIWCC_DTL_EMPLOYER_ACN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_FILLER_2),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_STR_ADDR),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_LOCALITY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_POSTCODE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_GENDER),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_BIRTHDATE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_FILLER_3),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_LANGUAGE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_FILLER_4),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_OCCUP),
                msiwcc.get(MSIWCC.MSIWCC_DTL_DEPN_CHILDREN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_OTH_DEPN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_FILLER_8),
                msiwcc.get(MSIWCC.MSIWCC_DTL_PERM_EMPLOYEE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_TRNG_STATUS),
                msiwcc.get(MSIWCC.MSIWCC_DTL_HOURS_WORKED),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_PAY_RATE_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_CLMNT_PAY_RATE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_DUTY_STATUS),
                msiwcc.get(MSIWCC.MSIWCC_DTL_WORKPL_STR_ADDR),
                msiwcc.get(MSIWCC.MSIWCC_DTL_WORKPL_LOC_ADDR),
                msiwcc.get(MSIWCC.MSIWCC_DTL_WORKPL_POSTCODE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_INDUS_ASIC),
                msiwcc.get(MSIWCC.MSIWCC_DTL_INDUS_ANZSIC),
                msiwcc.get(MSIWCC.MSIWCC_DTL_WORKPL_SIZE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_ACC_LOCN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_ACC_LOCN_DESC),
                msiwcc.get(MSIWCC.MSIWCC_DTL_ACC_LOCALITY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_ACC_POSTCODE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_DATE_INJURY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_TIME_INJURY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_NATURE_INJURY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_BODILY_LOCN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_MECH_INJURY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_ACC_AGENCY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_RESULT_INJURY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_DATE_DECEASED),
                msiwcc.get(MSIWCC.MSIWCC_DTL_EMPLOYER_ABN),
                msiwcc.get(MSIWCC.MSIWCC_DTL_WIC_RATE_NO),
                msiwcc.get(MSIWCC.MSIWCC_DTL_FILLER_5),
                msiwcc.get(MSIWCC.MSIWCC_DTL_INJ_AGENCY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_SIG_INJ_DATE),
                msiwcc.get(MSIWCC.MSIWCC_DTL_FILLER_6),
                msiwcc.get(MSIWCC.MSIWCC_DTL_FILLER_7),
                msiwcc.get(MSIWCC.MSIWCC_DTL_WRKR_TEL_NO),
                msiwcc.get(MSIWCC.MSIWCC_DTL_TOOCS_BREAK_AGENCY),
                msiwcc.get(MSIWCC.MSIWCC_DTL_TOOCS_AGENCY_INJ),
                "")
        return msiwccDtl
    }

    private String getMSIWCCActivity(HashMap<String,String> msiwcc) {
        info("getMSIWCCActivity")
        String msiwccAct = String.format("%1s%-19s%1s%8s%1s%8s%8s%1s%2s%-12s%2s%1s%2s%2s%10s%10s%8s%-40s%-14s%-200s%-200s%8s%-12s%3s%-31s%-8s%-6s%8s%-2s%-2s%-263s",
                msiwcc.get(MSIWCC.MSIWCC_ACT_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_ACT_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_ACT_LIAB_STATUS_DATE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_CLOSED_CLAIM),
                msiwcc.get(MSIWCC.MSIWCC_ACT_CLOSED_DATE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_REOPEN_DATE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_REOPEN_RSN),
                msiwcc.get(MSIWCC.MSIWCC_ACT_CLAIM_LIAB_IND),
                msiwcc.get(MSIWCC.MSIWCC_ACT_FILLER_1),
                msiwcc.get(MSIWCC.MSIWCC_ACT_WORK_STAT_CODE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_2ND_INJ_CLAIM),
                msiwcc.get(MSIWCC.MSIWCC_ACT_NOTIFIER_CODE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_EXCUSE_CODE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_FILLER_2),
                msiwcc.get(MSIWCC.MSIWCC_ACT_FILLER_3),
                msiwcc.get(MSIWCC.MSIWCC_ACT_STATEMENT_DATE),
                msiwcc.get(MSIWCC.MSIWCC_NOTIFIER_NAME),
                msiwcc.get(MSIWCC.MSIWCC_NOTIFIER_TEL_NO),
                msiwcc.get(MSIWCC.MSIWCC_INCIDENT_DESC),
                msiwcc.get(MSIWCC.MSIWCC_INJURY_DESC),
                msiwcc.get(MSIWCC.MSIWCC_ACT_WORK_DATE),
                "",
                msiwcc.get(MSIWCC.MSIWCC_ACT_WPI_PERCENT),
                "",
                msiwcc.get(MSIWCC.MSIWCC_ACT_WCC_MATTER_NO),
                "",
                msiwcc.get(MSIWCC.MSIWCC_ACT_WCA_DATE),
                msiwcc.get(MSIWCC.MSIWCC_ACT_WCA_OUTCOME),
				msiwcc.get(MSIWCC.MSIWCC_ACT_ESTIMATE_WPI),
                "")
        return msiwccAct
    }

    private String getMSIWCCLostTime(HashMap<String,String> msiwcc) {
        info("getMSIWCCLostTime")
        String msiwccLst = String.format("%1s%-19s%1s%8s%8s%8s%8s%5s%-842s",
                msiwcc.get(MSIWCC.MSIWCC_LST_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_LST_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_LST_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_LST_DATE_CEASED_WORK),
                msiwcc.get(MSIWCC.MSIWCC_LST_EST_RES_DATE),
                msiwcc.get(MSIWCC.MSIWCC_LST_FILLER_1),
                msiwcc.get(MSIWCC.MSIWCC_LST_DATE_RESUMED_WORK),
                msiwcc.get(MSIWCC.MSIWCC_LST_DAYS_OFF_WORK),
                "")
        return msiwccLst
    }

    private String getMSIWCCRehab(HashMap<String,String> msiwcc) {
        info("getMSIWCCRehab")
        String msiwccReh = String.format("%1s%-19s%1s%3s%4s%8s%8s%2s%2s%8s%11s%-833s",
                msiwcc.get(MSIWCC.MSIWCC_REH_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_REH_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_REH_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_REH_FILLER_1),
                msiwcc.get(MSIWCC.MSIWCC_REH_PROVIDER_CODE),
                msiwcc.get(MSIWCC.MSIWCC_REH_DATE_REF_PROVIDER),
                msiwcc.get(MSIWCC.MSIWCC_REH_SERV_PROV_END_DT),
                msiwcc.get(MSIWCC.MSIWCC_REH_SERV_PROV_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_REH_SERV_PROV_SUB_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_REH_SERV_PROV_NULL_DT),
                msiwcc.get(MSIWCC.MSIWCC_REH_SERV_PROV_ABN),
                "")
        return msiwccReh
    }

    private String getMSIWCCPayment(HashMap<String,String> msiwcc) {
        info("getMSIWCCPayment")
        String msiwccPay = String.format("%1s%-19s%1s%2s%8s%1s%1s%10s%8s%8s%1s%6s%1s%6s%12s%-20s%-20s%-15s%8s%-752s",
                msiwcc.get(MSIWCC.MSIWCC_PAY_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_PAY_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_PAY_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_PAY_FILLER_1),
                msiwcc.get(MSIWCC.MSIWCC_PAY_TRANS_DATE),
                msiwcc.get(MSIWCC.MSIWCC_PAY_ADJUST_FLAG),
                msiwcc.get(MSIWCC.MSIWCC_PAY_REC_AMOUNT_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_PAY_REC_AMOUNT),
                msiwcc.get(MSIWCC.MSIWCC_PAY_PRD_START_DATE),
                msiwcc.get(MSIWCC.MSIWCC_PAY_PRD_END_DATE),
                msiwcc.get(MSIWCC.MSIWCC_PAY_HRS_TOT_INCAP_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_PAY_HRS_TOT_INCAP),
                msiwcc.get(MSIWCC.MSIWCC_PAY_HRS_OTH_INCAP_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_PAY_HRS_OTH_INCAP),
                msiwcc.get(MSIWCC.MSIWCC_PAY_FILLER_2),
                msiwcc.get(MSIWCC.MSIWCC_PAY_PAYEE_ID),
                msiwcc.get(MSIWCC.MSIWCC_PAY_SERV_PROV_ID),
                msiwcc.get(MSIWCC.MSIWCC_PAY_CLASS_NO),
                msiwcc.get(MSIWCC.MSIWCC_PAY_SERV_DATE),
                "")
        return msiwccPay
    }

    private String getMSIWCCEstimates(HashMap<String,String> msiwcc) {
        info("getMSIWCCEstimates")
        String msiwccEst = String.format("%1s%-19s%1s%2s%1s%11s%1s%5s%-859s",
                msiwcc.get(MSIWCC.MSIWCC_EST_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_EST_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_EST_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_EST_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_EST_AMOUNT_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_EST_AMOUNT),
                msiwcc.get(MSIWCC.MSIWCC_EST_FUT_WKS_OFF_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_EST_FUT_WKS_OFF),
                "")
        return msiwccEst
    }

    private String getMSIWCCDetail2(HashMap<String,String> msiwcc) {
        info("getMSIWCCDetail2")
        String msiwccDt2 = String.format("%1s%-19s%1s%-20s%-20s%-120s%-14s%-14s%-691s",
                msiwcc.get(MSIWCC.MSIWCC_DT2_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_DT2_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_DT2_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_DT2_SURNAME),
                msiwcc.get(MSIWCC.MSIWCC_DT2_GIVEN_NAMES),
                msiwcc.get(MSIWCC.MSIWCC_DT2_ACC_LOCATION),
                msiwcc.get(MSIWCC.MSIWCC_DT2_MOBILE_PHONE_NO),
                msiwcc.get(MSIWCC.MSIWCC_DT2_WORK_PHONE_NO),
                "")
        return msiwccDt2
    }

    private String getMSIWCCWorkCapacity(HashMap<String,String> msiwcc) {
        info("getMSIWCCWorkCapacity")
        String msiwccWcd = String.format("%1s%-19s%1s%8s%8s%-2s%-2s%-2s%-2s%-855s",
                msiwcc.get(MSIWCC.MSIWCC_WCD_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_WCD_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_WCD_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_WCD_ORIG_DEC_DATE),
                msiwcc.get(MSIWCC.MSIWCC_WCD_TRANS_DATE),
                msiwcc.get(MSIWCC.MSIWCC_WCD_DATE_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_WCD_DECISION_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_WCD_REVIEW_STAGE),
                msiwcc.get(MSIWCC.MSIWCC_WCD_OUTCOME),
                "")
        return msiwccWcd
    }

    private String getMSIWCCControl(HashMap<String,String> msiwcc) {
        info("getMSIWCCControl")
        String msiwccCtl = String.format("%1s%-19s%1s%1s%11s%1s%11s%1s%11s%1s%11s%1s%8s%-822s",
                msiwcc.get(MSIWCC.MSIWCC_CTL_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_CTL_CLAIM_NO),
                msiwcc.get(MSIWCC.MSIWCC_CTL_REC_IDENT),
                msiwcc.get(MSIWCC.MSIWCC_CTL_PAY_TODATE_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_CTL_PAY_TODATE),
                msiwcc.get(MSIWCC.MSIWCC_CTL_REC_TODATE_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_CTL_REC_TODATE),
                msiwcc.get(MSIWCC.MSIWCC_CTL_TOT_EST_LIAB_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_CTL_TOT_EST_LIAB),
                msiwcc.get(MSIWCC.MSIWCC_CTL_TOT_EST_RECOV_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_CTL_TOT_EST_RECOV),
                msiwcc.get(MSIWCC.MSIWCC_CTL_HRS_TOT_INCAP_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_CTL_HRS_TOT_INCAP),
                "")
        return msiwccCtl
    }

    private String getMSIWCCTrailer(HashMap<String,String> msiwcc) {
        info("getMSIWCCTrailer")
        String msiwccTlr = String.format("%1s%7s%7s%7s%7s%7s%7s%7s%1s%14s%7s%-828s",
                msiwcc.get(MSIWCC.MSIWCC_TLR_REC_TYPE),
                msiwcc.get(MSIWCC.MSIWCC_TLR_CLAIM_BAS_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_CLAIM_ACT_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_TIME_LOST_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_REHAB_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_PAY_REC_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_EST_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_CTL_REC_COUNT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_TOT_PY_REC_AMT_SIGN),
                msiwcc.get(MSIWCC.MSIWCC_TLR_TOT_PY_REC_AMT),
                msiwcc.get(MSIWCC.MSIWCC_TLR_DT2_REC_COUNT),
                "")
        return msiwccTlr
    }

    private void addMSSWCCA(MSF536Rec msf536Rec, String rehabFlag){
        info("addMSSWCCA")
        if (msf536Rec.getInsClaimNo().trim().equals("")){
            arrayOfMSSWCCA.add(new MSSWCCA(msf536Rec.getPrimaryKey().getClaimNo(),msf536Rec.getPrimaryKey().getClaimNo(), rehabFlag))
        }else{
            arrayOfMSSWCCA.add(new MSSWCCA(msf536Rec.getInsClaimNo(),msf536Rec.getPrimaryKey().getClaimNo(), rehabFlag))
        }
    }

    private String formatDate(String inputDate){
        info("formatDate")
        String tempString = inputDate.padRight(8)
        return (tempString.substring(6, 8) + "/" + tempString.substring(4, 6) + "/" + tempString.substring(0, 4))
    }

    private String inverseDate(String inputDate){
        info("inverseDate")
        return (inputDate.trim().isNumber() ? (99999999 - inputDate.trim().toLong()).toString() : "00000000")
    }

    private void writeError(String errorMsg) {
        info("writeError")
        arrayOfError.add(errorMsg)
    }

    private void throwError(String errorMsg) {
        info("throwError")
        arrayOfError.add(errorMsg)
        throw new RuntimeException(errorMsg)
    }

    private List<String> runMSS071(String option, String entType, String noToRetrieve, String keyValue, String restartRefNo){
        info("runMSS071")

        List<String> keyRefCodes = new ArrayList<String>()

        MSS071LINK mss071lnk = eroi.execute('MSS071', {MSS071LINK mss071lnk ->
            mss071lnk.setOption071A(option)
            mss071lnk.setEntType(entType)
            mss071lnk.setNoToRetrieve(noToRetrieve.toShort())
            mss071lnk.setKeyRefCode(keyValue)
            mss071lnk.setRestartRefNo(restartRefNo)
        })

        if (!mss071lnk.getKeyRefCodes()[1].getRefCode().trim().equals("")){
            for (int idx in 1..noToRetrieve.toInteger()){
                keyRefCodes.add(mss071lnk.getKeyRefCodes()[idx].getRefCode().trim())
            }
        }else{
            Constraint cEntityType = MSF071Key.entityType.equalTo(entType)
            Constraint cEntityValue = MSF071Key.entityValue.equalTo(keyValue)
            Constraint cRefNo = MSF071Key.refNo.equalTo(restartRefNo)
            Constraint cSeqNum = MSF071Key.seqNum.greaterThanEqualTo(" ")

            def query = new QueryImpl(MSF071Rec.class).and(cEntityType).and(cEntityValue).and(cRefNo).and(cSeqNum).orderBy(MSF071Rec.msf071Key)

            int cntr = noToRetrieve.toInteger()
            edoi.search(query, MAX_ROW_READ, {MSF071Rec msf071Rec ->
                keyRefCodes.add(msf071Rec.getRefCode())
                cntr--
            })

            if (cntr > 0){
                for (int i = 0; i<cntr; i++){
                    keyRefCodes.add("")
                }
            }
        }

        return keyRefCodes
    }

    private MSF533Rec readPaymentType(String payType){
        info("readPaymentType")
        try{
            MSF533Rec msf533Rec = edoi.findByPrimaryKey(new MSF533Key(payType))
            return msf533Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read Payment type, payType: ${payType} " + e.getMessage())
        }
        return null
    }

    private MSF012Rec readBatchInterface(String dataType, String keyValue){
        info("readBatchInterface")
        try{
            MSF012Rec msf012Rec = edoi.findByPrimaryKey(new MSF012Key(dataType,keyValue))
            return msf012Rec
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            info("Failed to read MSF012 record dataType ${dataType}, keyValue ${keyValue} : " + e.getMessage())
        }

        return null
    }

    private TableServiceReadReplyDTO readTable(String tableType, String tableCode){
        info("readTable")
        try{
            TableServiceReadRequiredAttributesDTO tblReq = new TableServiceReadRequiredAttributesDTO()
            tblReq.setReturnDescription(true)
            tblReq.returnDescription = true
            tblReq.returnAssociatedRecord = true

            TableServiceReadReplyDTO tblReply = service.get('TABLE').read({TableServiceReadRequestDTO it->
                it.requiredAttributes = tblReq
                it.tableType = tableType
                it.tableCode = tableCode
            })
            return tblReply
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("error when read table code : ${e.getMessage()}")
        }

        return null
    }

    private void writeToOutputFile() {
        info("writeToOutputFile")

        File oFileMSIWCC = new  File("${workingDir}/${OUTPUT_FILE_NAME}.${taskUUID}")
        def outputFile = new FileWriter(oFileMSIWCC)
        arrayOfMSIWCC.each {String msiwcc ->
            outputFile.write(msiwcc)
        }
        outputFile.close()

        if (taskUUID?.trim()) {
            request.request.CURRENT.get().addOutput(oFileMSIWCC,"text/plain", OUTPUT_FILE_NAME)
        }
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        def reportA = report.open(REPORT_A)
        reportA.write("WORKCOVER INTERFACE ERROR REPORT".center(80))
        reportA.writeLine(80,"-")
        reportA.write(" ")
        arrayOfError.each {String errMsg ->
            reportA.write(errMsg)
        }
        reportA.close()
    }

    private class MSSWCCA {
        private String insClaimNo
        private String claimNo
        private String rehabFlag

        public MSSWCCA(){
            this.insClaimNo = ""
            this.claimNo = ""
            this.rehabFlag = ""
        }

        public MSSWCCA(String insClaimNo, String claimNo, String rehabFlag){
            this.insClaimNo = insClaimNo
            this.claimNo = claimNo
            this.rehabFlag = rehabFlag
        }

        public String getInsClaimNo(){
            return this.insClaimNo
        }

        public String getClaimNo(){
            return this.claimNo
        }

        public String getRehabFlag(){
            return this.rehabFlag
        }
    }

    private class MSF012_521Data{
        private String servProvType
        private String servProvSubt
        private MSF012Key msf012Key

        public MSF012_521Data(){
            servProvType = ""
            servProvSubt = ""
            msf012Key = null
        }

        public MSF012_521Data(MSF012Rec msf012Rec){
            this.setMSF012_521DataFields(msf012Rec)
            this.msf012Key = msf012Rec.getPrimaryKey()
        }

        public void setMSF012_521DataFields(MSF012Rec msf012Rec){
            String tempString = msf012Rec.getDataArea().padRight(934)

            servProvType = tempString.substring(0,2).trim().isNumber() ? tempString.substring(0,2) : '00'
            servProvSubt = tempString.substring(4,6).trim().isNumber() ? tempString.substring(4,6) : '00'
        }

        public String getServProvType() {
            return servProvType
        }
        public String getServProvSubt() {
            return servProvSubt
        }

    }

    private class MSF012_536Data{
        private String casId
        private String liabStatus
        private String compCourt
        private String notifCode
        private String excuseCode
        private String wpiPerc
        private String bringUpInd
        private String bringUpDate
        private String policyNo
        private String extRefNo
        private String wrkStatDate
        private MSF012Key msf012Key

        public MSF012_536Data(){
            casId=""
            liabStatus=""
            compCourt=""
            notifCode=""
            excuseCode=""
            wpiPerc="000"
            bringUpInd=""
            bringUpDate=""
            policyNo=""
            extRefNo=""
            wrkStatDate="00000000"
            msf012Key = null
        }

        public MSF012_536Data(MSF012Rec msf012Rec){
            this.setMSF012_536DataFields(msf012Rec)
            this.msf012Key = msf012Rec.getPrimaryKey()
        }

        public void setMSF012_536DataFields(MSF012Rec msf012Rec){
            String tempString = msf012Rec.getDataArea().padRight(934)

            casId = tempString.substring(0,19)
            liabStatus = tempString.substring(19,21)
            compCourt = tempString.substring(21,23)
            notifCode = tempString.substring(23,25)
            excuseCode = tempString.substring(25,27)
            wpiPerc = tempString.substring(27,30).trim().isNumber()?tempString.substring(27,30):'000'
            bringUpInd = tempString.substring(30,31)
            bringUpDate = tempString.substring(31,39)
            policyNo = tempString.substring(39,59)
            extRefNo = tempString.substring(59,74)
            wrkStatDate = tempString.substring(74,82).trim().isNumber()?tempString.substring(74,82):'00000000'
        }

        public String getBringUpInd() {
            return bringUpInd
        }
        public String getBringUpDate() {
            return bringUpDate
        }

        public String getPolicyNo() {
            return policyNo
        }

        public String getLiabStatus() {
            return liabStatus
        }

        public String getNotifCode() {
            return notifCode
        }

        public String getWrkStatDate() {
            return wrkStatDate
        }

        public String getWpiPerc() {
            return wpiPerc
        }
        public String getExtRefNo() {
            return extRefNo
        }
        public String getExcuseCode() {
            return excuseCode
        }
        public MSF012Key getMSF012Key(){
            return msf012Key
        }
        public void setBringUpInd(String newBringUpInd){
            bringUpInd = newBringUpInd
        }
        public String toString(){
            return String.format("%-19s%-2s%-2s%-2s%-2s%3s%1s%8s%-20s%-15s%8s",
            casId,liabStatus,compCourt,notifCode,excuseCode,wpiPerc,bringUpInd,bringUpDate,policyNo,extRefNo,wrkStatDate)
        }
    }

    private class EstEntry{
        private String estType
        private BigDecimal estAmt

        public EstEntry(){
            this.estType=""
            this.estAmt=0
        }

        public EstEntry(String estType, BigDecimal estAmt){
            this.estType=estType
            this.estAmt=estAmt
        }

        public void addEstAmt(BigDecimal estAmt){
            this.estAmt = this.estAmt + estAmt
        }
        public String getEstType(){
            return this.estType
        }
        public BigDecimal getEstAmt(){
            return this.estAmt
        }
    }

    private class RehabEntry{
        private String rehabProvider
        private String activDate
        private String lastChangedDate
        private String servProvEndDate
        private String servProvType
        private String servProvSubType
        private String servProvNullDt
        private String servProvABN

        public RehabEntry(){
            this.rehabProvider=""
            this.activDate=""
            this.lastChangedDate=""
            this.servProvEndDate=""
            this.servProvType=""
            this.servProvSubType=""
            this.servProvNullDt=""
            this.servProvABN=""
        }

        public String getRehabProvider(){
            return  this.rehabProvider
        }
        public String getActivDate(){
            return  this.activDate
        }
        public String getLastChangedDate(){
            return  this.lastChangedDate
        }
        public String getServProvEndDate(){
            return  this.servProvEndDate
        }
        public String getServProvType(){
            return  this.servProvType
        }
        public String getServProvSubType(){
            return  this.servProvSubType
        }
        public String getServProvNullDt(){
            return  this.servProvNullDt
        }
        public String getServProvABN(){
            return  this.servProvABN
        }
        public void setRehabProvider(String rehabProvider){
            this.rehabProvider = rehabProvider
        }
        public void setActivDate(String activDate){
            this.activDate = activDate
        }
        public void setLastChangedDate(String lastChangedDate){
            this.lastChangedDate = lastChangedDate
        }
        public void setServProvEndDate(String servProvEndDate){
            this.servProvEndDate = servProvEndDate
        }
        public void setServProvType(String servProvType){
            this.servProvType = servProvType
        }
        public void setServProvSubType(String servProvSubType){
            this.servProvSubType = servProvSubType
        }
        public void setServProvNullDt(String servProvNullDt){
            this.servProvNullDt = servProvNullDt
        }
        public void setServProvABN(String servProvABN){
            this.servProvABN = servProvABN
        }
    }

    private class MSF012_537Data{
        private String liabStatus
        private String excuseCode
        private String liabDate
        private MSF012Key msf012Key

        public MSF012_536Data(){
            liabStatus=""
            excuseCode=""
            liabDate=""
            msf012Key=null
        }

        public MSF012_537Data(MSF012Rec msf012Rec){
            this.setMSF012_537DataFields(msf012Rec)
            this.msf012Key = msf012Rec.getPrimaryKey()
        }

        public void setMSF012_537DataFields(MSF012Rec msf012Rec){
            String tempString = msf012Rec.getDataArea().padRight(934).toString()

            liabStatus = tempString.substring(0,2)
            excuseCode = tempString.substring(2,4)

            tempString = msf012Rec.getPrimaryKey().getKeyValue().padRight(63).toString()
            liabDate = tempString.substring(16,24)
        }

        public String getLiabStatus() {
            return liabStatus
        }
        public String getExcuseCode() {
            return excuseCode
        }

        public String getLiabDate(){
            return liabDate
        }

    }

    private class MSF012_539Data{
        private String payeeId
        private String providerId
        private String classNo
        private MSF012Key msf012Key

        public MSF012_539Data(){
            payeeId=""
            providerId=""
            classNo=""
            msf012Key=null
        }

        public MSF012_539Data(MSF012Rec msf012Rec){
            this.setMSF012_539DataFields(msf012Rec)
            this.msf012Key = msf012Rec.getPrimaryKey()
        }

        public void setMSF012_539DataFields(MSF012Rec msf012Rec){
            String tempString = msf012Rec.getDataArea().padRight(934)
            payeeId = tempString.substring(0,20)
            providerId = tempString.substring(20,40)
            classNo = tempString.substring(40,54)
        }

        public String getPayeeId() {
            return payeeId
        }
        public String getProviderId() {
            return providerId
        }
        public String getClassNo() {
            return classNo
        }
    }

    private class MSIWCC {
        private static final String MSIWCC_HDR_REC_TYPE                = "hdr_1"
        private static final String MSIWCC_HDR_INSURER_NO              = "hdr_2"
        private static final String MSIWCC_HDR_SUBMISSION_TYPE         = "hdr_3"
        private static final String MSIWCC_HDR_CLAIM_REL_NO            = "hdr_4"
        private static final String MSIWCC_HDR_SUBMIT_START            = "hdr_5"
        private static final String MSIWCC_HDR_SUBMIT_END              = "hdr_6"

        private static final String MSIWCC_DTL_REC_TYPE                = "dtl_1"
        private static final String MSIWCC_DTL_CLAIM_NO                = "dtl_2"
        private static final String MSIWCC_DTL_REC_IDENT               = "dtl_3"
        private static final String MSIWCC_DTL_CLAIM_FILLER_1          = "dtl_4"
        private static final String MSIWCC_DTL_SHARED_CLAIM_CODE       = "dtl_5"
        private static final String MSIWCC_DTL_ERR_RPT_TARGET          = "dtl_6"
        private static final String MSIWCC_DTL_INSURER_BRANCH          = "dtl_7"
        private static final String MSIWCC_DTL_DATE_CLAIM_ENT          = "dtl_8"
        private static final String MSIWCC_DTL_DATE_CLAIM_LOD          = "dtl_9"
        private static final String MSIWCC_DTL_POLICY_NO               = "dtl_10"
        private static final String MSIWCC_DTL_PRD_COMMENCE_DATE       = "dtl_11"
        private static final String MSIWCC_DTL_TARIFF_RATE_NO          = "dtl_12"
        private static final String MSIWCC_DTL_EMPLOYER_NAME           = "dtl_13"
        private static final String MSIWCC_DTL_EMPLOYER_ACN            = "dtl_14"
        private static final String MSIWCC_DTL_FILLER_2                = "dtl_15"
        private static final String MSIWCC_DTL_CLMNT_STR_ADDR          = "dtl_16"
        private static final String MSIWCC_DTL_CLMNT_LOCALITY          = "dtl_17"
        private static final String MSIWCC_DTL_CLMNT_POSTCODE          = "dtl_18"
        private static final String MSIWCC_DTL_CLMNT_GENDER            = "dtl_19"
        private static final String MSIWCC_DTL_CLMNT_BIRTHDATE         = "dtl_20"
        private static final String MSIWCC_DTL_CLMNT_FILLER_3          = "dtl_21"
        private static final String MSIWCC_DTL_CLMNT_LANGUAGE          = "dtl_22"
        private static final String MSIWCC_DTL_FILLER_4                = "dtl_23"
        private static final String MSIWCC_DTL_CLMNT_OCCUP             = "dtl_24"
        private static final String MSIWCC_DTL_DEPN_CHILDREN           = "dtl_25"
        private static final String MSIWCC_DTL_OTH_DEPN                = "dtl_26"
        private static final String MSIWCC_DTL_FILLER_8                = "dtl_27"
        private static final String MSIWCC_DTL_PERM_EMPLOYEE           = "dtl_28"
        private static final String MSIWCC_DTL_TRNG_STATUS             = "dtl_29"
        private static final String MSIWCC_DTL_HOURS_WORKED            = "dtl_30"
        private static final String MSIWCC_DTL_CLMNT_PAY_RATE_SIGN     = "dtl_31"
        private static final String MSIWCC_DTL_CLMNT_PAY_RATE          = "dtl_32"
        private static final String MSIWCC_DTL_DUTY_STATUS             = "dtl_33"
        private static final String MSIWCC_DTL_WORKPL_STR_ADDR         = "dtl_34"
        private static final String MSIWCC_DTL_WORKPL_LOC_ADDR         = "dtl_35"
        private static final String MSIWCC_DTL_WORKPL_POSTCODE         = "dtl_36"
        private static final String MSIWCC_DTL_INDUS_ASIC              = "dtl_37"
        private static final String MSIWCC_DTL_INDUS_ANZSIC            = "dtl_38"
        private static final String MSIWCC_DTL_WORKPL_SIZE             = "dtl_39"
        private static final String MSIWCC_DTL_ACC_LOCN                = "dtl_40"
        private static final String MSIWCC_DTL_ACC_LOCN_DESC           = "dtl_41"
        private static final String MSIWCC_DTL_ACC_LOCALITY            = "dtl_42"
        private static final String MSIWCC_DTL_ACC_POSTCODE            = "dtl_43"
        private static final String MSIWCC_DTL_DATE_INJURY             = "dtl_44"
        private static final String MSIWCC_DTL_TIME_INJURY             = "dtl_45"
        private static final String MSIWCC_DTL_NATURE_INJURY           = "dtl_46"
        private static final String MSIWCC_DTL_BODILY_LOCN             = "dtl_47"
        private static final String MSIWCC_DTL_MECH_INJURY             = "dtl_48"
        private static final String MSIWCC_DTL_ACC_AGENCY              = "dtl_49"
        private static final String MSIWCC_DTL_RESULT_INJURY           = "dtl_50"
        private static final String MSIWCC_DTL_DATE_DECEASED           = "dtl_51"
        private static final String MSIWCC_DTL_EMPLOYER_ABN            = "dtl_52"
        private static final String MSIWCC_DTL_WIC_RATE_NO             = "dtl_53"
        private static final String MSIWCC_DTL_FILLER_5                = "dtl_54"
        private static final String MSIWCC_DTL_INJ_AGENCY              = "dtl_55"
        private static final String MSIWCC_DTL_SIG_INJ_DATE            = "dtl_56"
        private static final String MSIWCC_DTL_FILLER_6                = "dtl_57"
        private static final String MSIWCC_DTL_FILLER_7                = "dtl_58"
        private static final String MSIWCC_DTL_WRKR_TEL_NO             = "dtl_59"
        private static final String MSIWCC_DTL_TOOCS_BREAK_AGENCY      = "dtl_60"
        private static final String MSIWCC_DTL_TOOCS_AGENCY_INJ        = "dtl_61"

        private static final String MSIWCC_ACT_REC_TYPE                = "act_1"
        private static final String MSIWCC_ACT_CLAIM_NO                = "act_2"
        private static final String MSIWCC_ACT_REC_IDENT               = "act_3"
        private static final String MSIWCC_ACT_LIAB_STATUS_DATE        = "act_4"
        private static final String MSIWCC_ACT_CLOSED_CLAIM            = "act_5"
        private static final String MSIWCC_ACT_CLOSED_DATE             = "act_6"
        private static final String MSIWCC_ACT_REOPEN_DATE             = "act_7"
        private static final String MSIWCC_ACT_REOPEN_RSN              = "act_8"
        private static final String MSIWCC_ACT_CLAIM_LIAB_IND          = "act_9"
        private static final String MSIWCC_ACT_FILLER_1                = "act_10"
        private static final String MSIWCC_ACT_WORK_STAT_CODE          = "act_11"
        private static final String MSIWCC_ACT_2ND_INJ_CLAIM           = "act_12"
        private static final String MSIWCC_ACT_NOTIFIER_CODE           = "act_13"
        private static final String MSIWCC_ACT_EXCUSE_CODE             = "act_14"
        private static final String MSIWCC_ACT_FILLER_2                = "act_15"
        private static final String MSIWCC_ACT_FILLER_3                = "act_16"
        private static final String MSIWCC_ACT_STATEMENT_DATE          = "act_17"
        private static final String MSIWCC_NOTIFIER_NAME               = "act_18"
        private static final String MSIWCC_NOTIFIER_TEL_NO             = "act_19"
        private static final String MSIWCC_INCIDENT_DESC               = "act_20"
        private static final String MSIWCC_INJURY_DESC                 = "act_21"
        private static final String MSIWCC_ACT_WORK_DATE               = "act_22"
        private static final String MSIWCC_ACT_WPI_PERCENT             = "act_23"
        private static final String MSIWCC_ACT_WCC_MATTER_NO           = "act_24"
        private static final String MSIWCC_ACT_WCA_DATE                = "act_25"
        private static final String MSIWCC_ACT_WCA_OUTCOME             = "act_26"
		private static final String MSIWCC_ACT_ESTIMATE_WPI            = "act_27"
		
        private static final String MSIWCC_LST_REC_TYPE                = "lst_1"
        private static final String MSIWCC_LST_CLAIM_NO                = "lst_2"
        private static final String MSIWCC_LST_REC_IDENT               = "lst_3"
        private static final String MSIWCC_LST_DATE_CEASED_WORK        = "lst_4"
        private static final String MSIWCC_LST_EST_RES_DATE            = "lst_5"
        private static final String MSIWCC_LST_FILLER_1                = "lst_6"
        private static final String MSIWCC_LST_DATE_RESUMED_WORK       = "lst_7"
        private static final String MSIWCC_LST_DAYS_OFF_WORK           = "lst_8"

        private static final String MSIWCC_REH_REC_TYPE                = "reh_1"
        private static final String MSIWCC_REH_CLAIM_NO                = "reh_2"
        private static final String MSIWCC_REH_REC_IDENT               = "reh_3"
        private static final String MSIWCC_REH_FILLER_1                = "reh_4"
        private static final String MSIWCC_REH_PROVIDER_CODE           = "reh_5"
        private static final String MSIWCC_REH_DATE_REF_PROVIDER       = "reh_6"
        private static final String MSIWCC_REH_SERV_PROV_END_DT        = "reh_7"
        private static final String MSIWCC_REH_SERV_PROV_TYPE          = "reh_8"
        private static final String MSIWCC_REH_SERV_PROV_SUB_TYPE      = "reh_9"
        private static final String MSIWCC_REH_SERV_PROV_NULL_DT       = "reh_10"
        private static final String MSIWCC_REH_SERV_PROV_ABN           = "reh_11"

        private static final String MSIWCC_PAY_REC_TYPE                = "pay_1"
        private static final String MSIWCC_PAY_CLAIM_NO                = "pay_2"
        private static final String MSIWCC_PAY_REC_IDENT               = "pay_3"
        private static final String MSIWCC_PAY_FILLER_1                = "pay_4"
        private static final String MSIWCC_PAY_TRANS_DATE              = "pay_5"
        private static final String MSIWCC_PAY_ADJUST_FLAG             = "pay_6"
        private static final String MSIWCC_PAY_REC_AMOUNT_SIGN         = "pay_7"
        private static final String MSIWCC_PAY_REC_AMOUNT              = "pay_8"
        private static final String MSIWCC_PAY_PRD_START_DATE          = "pay_9"
        private static final String MSIWCC_PAY_PRD_END_DATE            = "pay_10"
        private static final String MSIWCC_PAY_HRS_TOT_INCAP_SIGN      = "pay_11"
        private static final String MSIWCC_PAY_HRS_TOT_INCAP           = "pay_12"
        private static final String MSIWCC_PAY_HRS_OTH_INCAP_SIGN      = "pay_13"
        private static final String MSIWCC_PAY_HRS_OTH_INCAP           = "pay_14"
        private static final String MSIWCC_PAY_FILLER_2                = "pay_15"
        private static final String MSIWCC_PAY_PAYEE_ID                = "pay_16"
        private static final String MSIWCC_PAY_SERV_PROV_ID            = "pay_17"
        private static final String MSIWCC_PAY_CLASS_NO                = "pay_18"
        private static final String MSIWCC_PAY_SERV_DATE               = "pay_19"

        private static final String MSIWCC_EST_REC_TYPE                = "est_1"
        private static final String MSIWCC_EST_CLAIM_NO                = "est_2"
        private static final String MSIWCC_EST_REC_IDENT               = "est_3"
        private static final String MSIWCC_EST_TYPE                    = "est_4"
        private static final String MSIWCC_EST_AMOUNT_SIGN             = "est_5"
        private static final String MSIWCC_EST_AMOUNT                  = "est_6"
        private static final String MSIWCC_EST_FUT_WKS_OFF_SIGN        = "est_7"
        private static final String MSIWCC_EST_FUT_WKS_OFF             = "est_8"

        private static final String MSIWCC_DT2_REC_TYPE                = "dt2_1"
        private static final String MSIWCC_DT2_CLAIM_NO                = "dt2_2"
        private static final String MSIWCC_DT2_REC_IDENT               = "dt2_3"
        private static final String MSIWCC_DT2_SURNAME                 = "dt2_4"
        private static final String MSIWCC_DT2_GIVEN_NAMES             = "dt2_5"
        private static final String MSIWCC_DT2_ACC_LOCATION            = "dt2_6"
        private static final String MSIWCC_DT2_MOBILE_PHONE_NO         = "dt2_7"
        private static final String MSIWCC_DT2_WORK_PHONE_NO           = "dt2_8"

        private static final String MSIWCC_WCD_REC_TYPE                = "wcd_1"
        private static final String MSIWCC_WCD_CLAIM_NO                = "wcd_2"
        private static final String MSIWCC_WCD_REC_IDENT               = "wcd_3"
        private static final String MSIWCC_WCD_ORIG_DEC_DATE           = "wcd_4"
        private static final String MSIWCC_WCD_DECISION_TYPE           = "wcd_5"
        private static final String MSIWCC_WCD_REVIEW_STAGE            = "wcd_6"
        private static final String MSIWCC_WCD_DATE_TYPE               = "wcd_7"
        private static final String MSIWCC_WCD_TRANS_DATE              = "wcd_8"
        private static final String MSIWCC_WCD_OUTCOME                 = "wcd_9"
		
        private static final String MSIWCC_CTL_REC_TYPE                = "ctl_1"
        private static final String MSIWCC_CTL_CLAIM_NO                = "ctl_2"
        private static final String MSIWCC_CTL_REC_IDENT               = "ctl_3"
        private static final String MSIWCC_CTL_PAY_TODATE_SIGN         = "ctl_4"
        private static final String MSIWCC_CTL_PAY_TODATE              = "ctl_5"
        private static final String MSIWCC_CTL_REC_TODATE_SIGN         = "ctl_6"
        private static final String MSIWCC_CTL_REC_TODATE              = "ctl_7"
        private static final String MSIWCC_CTL_TOT_EST_LIAB_SIGN       = "ctl_8"
        private static final String MSIWCC_CTL_TOT_EST_LIAB            = "ctl_9"
        private static final String MSIWCC_CTL_TOT_EST_RECOV_SIGN      = "ctl_10"
        private static final String MSIWCC_CTL_TOT_EST_RECOV           = "ctl_11"
        private static final String MSIWCC_CTL_HRS_TOT_INCAP_SIGN      = "ctl_12"
        private static final String MSIWCC_CTL_HRS_TOT_INCAP           = "ctl_13"

        private static final String MSIWCC_TLR_REC_TYPE                = "tlr_1"
        private static final String MSIWCC_TLR_CLAIM_BAS_REC_COUNT     = "tlr_2"
        private static final String MSIWCC_TLR_CLAIM_ACT_REC_COUNT     = "tlr_3"
        private static final String MSIWCC_TLR_TIME_LOST_REC_COUNT     = "tlr_4"
        private static final String MSIWCC_TLR_REHAB_REC_COUNT         = "tlr_5"
        private static final String MSIWCC_TLR_PAY_REC_REC_COUNT       = "tlr_6"
        private static final String MSIWCC_TLR_EST_REC_COUNT           = "tlr_7"
        private static final String MSIWCC_TLR_CTL_REC_COUNT           = "tlr_8"
        private static final String MSIWCC_TLR_TOT_PY_REC_AMT_SIGN     = "tlr_9"
        private static final String MSIWCC_TLR_TOT_PY_REC_AMT          = "tlr_10"
        private static final String MSIWCC_TLR_DT2_REC_COUNT           = "tlr_11"
        private static final String MSIWCC_TLR_WCD_REC_COUNT           = "tlr_12"
    }

    private class MSSWCCAComparator implements Comparator{
        private int iCompareDirection = 0

        public MSSWCCAComparator(int iDirection){
            this.iCompareDirection = iDirection
        }

        @Override
        public int compare(Object o1, Object o2) {
            MSSWCCA obj1 = (MSSWCCA) o1
            MSSWCCA obj2 = (MSSWCCA) o2

            if (obj1 == null && obj2 == null) {
                return 0
            } else if (obj1 != null && obj2 == null) {
                return 1
            } else if (obj1 == null && obj2 != null) {
                return -1
            }

            int iSortResult = 0
            String sObj1 = getSortedCandidate(obj1)
            String sObj2 = getSortedCandidate(obj2)
            iSortResult = (compareAlphanumeric(sObj1, sObj2) * iCompareDirection)

            return iSortResult
        }

        private String getSortedCandidate(MSSWCCA obj){
            return String.format("%-20s%-10s%1s", obj.getInsClaimNo(), obj.getClaimNo(), obj.getRehabFlag())
        }

        private int compareAlphanumeric(Object obj1, Object obj2){

            String firstString = obj1.toString()
            String secondString = obj2.toString()

            if (secondString == null || firstString == null) {
                return 0
            }

            int lengthFirstStr = firstString.length()
            int lengthSecondStr = secondString.length()

            int index1 = 0
            int index2 = 0

            while (index1 < lengthFirstStr && index2 < lengthSecondStr) {
                char ch1 = firstString.charAt(index1)
                char ch2 = secondString.charAt(index2)

                char[] space1 = new char[lengthFirstStr]
                char[] space2 = new char[lengthSecondStr]

                int loc1 = 0
                int loc2 = 0

                boolean isBreak = false
                space1[loc1++] = ch1
                index1++

                if (index1 < lengthFirstStr) {
                    ch1 = firstString.charAt(index1)
                } else {
                    isBreak = true
                }

                if (!isBreak) {
                    while (Character.isDigit(ch1) == Character.isDigit(space1[0])){
                        space1[loc1++] = ch1
                        index1++

                        if (index1 < lengthFirstStr) {
                            ch1 = firstString.charAt(index1)
                        } else {
                            break
                        }
                    }
                }

                isBreak = false
                space2[loc2++] = ch2
                index2++

                if (index2 < lengthSecondStr) {
                    ch2 = secondString.charAt(index2)
                } else {
                    isBreak = true
                }

                if (!isBreak) {
                    while (Character.isDigit(ch2) == Character.isDigit(space2[0])) {
                        space2[loc2++] = ch2
                        index2++

                        if (index2 < lengthSecondStr) {
                            ch2 = secondString.charAt(index2)
                        } else {
                            break
                        }
                    }
                }

                String str1 = new String(space1)
                String str2 = new String(space2)

                int result

                if (Character.isDigit(space1[0]) && Character.isDigit(space2[0])) {
                    BigDecimal firstNumberToCompare = new BigDecimal(str1.trim())
                    BigDecimal secondNumberToCompare = new BigDecimal(str2.trim())
                    result = firstNumberToCompare.compareTo(secondNumberToCompare)
                } else {
                    result = str1.compareTo(str2)
                }

                if (result != 0) {
                    return result
                }
            }
            return lengthFirstStr - lengthSecondStr
        }

    }
    
    private class PaymtRecovRec implements Comparable<PaymtRecovRec>{
        String recType
        String claimNo
        String recIdent
        String transDate
        String adjustFlag
        String recAmountSign
        String recAmount
        String prdStartDate
        String prdEndDate
        String hrsTotIncapSign
        String hrsTotIncap
        String hrsOthIncapSign
        String hrsOthIncap
        String payeeId
        String servProvId
        String classNo
        String servDate
        
        public PaymtRecovRec(){
            recType          = ""
            claimNo          = ""
            recIdent         = ""
            transDate        = ""
            adjustFlag       = ""
            recAmountSign    = ""
            recAmount        = ""
            prdStartDate     = ""
            prdEndDate       = ""
            hrsTotIncapSign  = ""
            hrsTotIncap      = ""
            hrsOthIncapSign  = ""
            hrsOthIncap      = ""
            payeeId          = ""
            servProvId       = ""
            classNo          = ""
            servDate         = ""
        }
        
        int compareTo(PaymtRecovRec otherReportLine){
            if (otherReportLine != null){
                if (!claimNo.equals(otherReportLine.getClaimNo())){
                    return claimNo.compareTo(otherReportLine.getClaimNo())
                }
                if (!transDate.equals(otherReportLine.getTransDate())){
                    return transDate.compareTo(otherReportLine.getTransDate())
                }
            } 
            return -1
        }
        
    }
    
}

/**
 * Run the script
 */
ProcessTrbwcc process = new ProcessTrbwcc()
process.runBatch(binding)