/**
 * @Ventyx 2012
 * Conversion from trb265.cbl
 *
 * This program will extract Journal transactions for TRR267
 * Finally a request for TRR267 is written to process the output file.
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import com.mincom.ellipse.edoi.ejb.msf000.MSF000_ADKey;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_ADRec;
import com.mincom.ellipse.edoi.ejb.msf900.MSF900Key;
import com.mincom.ellipse.edoi.ejb.msf900.MSF900Rec;
import com.mincom.ellipse.edoi.ejb.msfx90.MSFX90Key;
import com.mincom.ellipse.edoi.ejb.msfx90.MSFX90Rec;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;
import groovy.sql.Sql
import javax.sql.DataSource;

/**
 * Request Parameters for ParamsTrb265.
 * reqPerFrom: Accounting Period From YYMM format
 * reqPerTo  : Accounting Period To YYMM format
 * reqDateFrom: Date Period From
 * reqDateTo  : Date Period To
 * reqDist1...reqDist11: Select District(s) (Blank for All)
 * reqAtax1...reqAtax9: Process Atax Code(s) (Blank for all)
 * reqSupp1...reqSupp5: Process Supplier(s) (Blank for all)
 * reqLevel: Detail Level (T/S/D)
 */
public class ParamsTrb265 {
    private String reqPerFrom, reqPerTo;
    private String reqDateFrom, reqDateTo;
    private String reqDist1, reqDist2, reqDist3, reqDist4, reqDist5, reqDist6,
    reqDist7, reqDist8, reqDist9, reqDist10, reqDist11;
    private String reqAtax1, reqAtax2, reqAtax3, reqAtax4, reqAtax5, reqAtax6,
    reqAtax7, reqAtax8, reqAtax9;
    private String reqSupp1, reqSupp2, reqSupp3, reqSupp4, reqSupp5;
    private String reqLevel;
}

/**
 * Output file from Trb265.
 */
public class TRO265 {
    String districtCode, key, acct, gstAcct, controlRecNo

    /**
     * Write the record information as a string.
     * @return record information as a string
     */
    public String writeDetail() {
        return String.format("%-4s%-24s%-13s%-13s%-4s",
        districtCode?.length() > 4 ? districtCode?.substring(0,4) : districtCode,
        key?.length() > 24 ? key?.substring(0,24) : key,
        acct?.length() > 13 ? acct?.substring(0,13) : acct,
        gstAcct?.length() > 13 ? gstAcct?.substring(0,13) : gstAcct,
        controlRecNo?.length() > 4 ? controlRecNo?.substring(0,4) : controlRecNo
        )
    }
}

/**
 * Record used for sorting process in Trb265.
 */
public class TRS265A {
    String tranGroupKey, journalNo, dstrctCode, processDate, transactionNo, userNo, rec900Type, accountCode, trnDateRvsd, controlRecNo
    double tranAmmount

    /**
     * Create new instance from this record.
     * @return new instance from this record
     */
    public TRS265A createNewInstance(){
        TRS265A.metaClass.getProperties().findAll(){it.getSetter()!=null}.inject(new TRS265A()){obj,metaProp->
            metaProp.setProperty(obj,metaProp.getProperty(this))
            obj
        }
    }
	public String toString(){
		return (tranGroupKey+","+journalNo+","+dstrctCode+","+processDate+","+transactionNo+","+userNo+","+rec900Type+","+accountCode+","+trnDateRvsd+","+controlRecNo
			+","+ tranAmmount.toString())
	}
}

/**
 * Comparator for TRS265A used in sorting process. <br>
 * Sort TRS265A based on <code>TRAN-GROUP-KEY, JOURNAL-NO, DSTRCT-CODE, PROCESS-DATE, 
 * TRANSACTION-NO, USERNO, CREATION-DATE, CREATION-TIME</code>.
 */
public class TRS265AComparator implements Comparator<TRS265A> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(TRS265A o1, TRS265A o2) {
        int c = o1.getTranGroupKey().compareTo(o2.getTranGroupKey())
        if(c == 0) {
            c = o1.getJournalNo().compareTo(o2.getJournalNo())
        }
        if(c == 0) {
            c = o1.getDstrctCode().compareTo(o2.getDstrctCode())
        }
        if(c == 0) {
            c = o1.getProcessDate().compareTo(o2.getProcessDate())
        }
        if(c == 0) {
            c = o1.getTransactionNo().compareTo(o2.getTransactionNo())
        }
        if(c == 0) {
            c = o1.getUserNo().compareTo(o2.getUserNo())
        }
        return c
    }
}

/**
 * Main process of Trb265.
 */
public class ProcessTrb265 extends SuperBatch {
    /*
     * Constants
     */
    public static final String FILE_OUTPUT_NAME = "TRO265"
    public static final String MSF000_DSTRC_STATUS_A = "A"
    public static final String MSF900_REC900_TYPE_I = "I"
    public static final String MSF900_REC900_TYPE_M = "M"
    public static final String REQ_LEVEL_D = "D"
    public static final String REQ_LEVEL_S = "S"
    public static final String REQ_LEVEL_T = "T"
    public static final String WX_ACCTS_PAY_CP = "AcctsPayCp"
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    int version = 4

    /*
     * Variables
     */
    String workingDir
    String cho0A00, sub0A01, cho0A03, cat0A04, cho0A05, sub0A06, cho0A08, cat0A09, cho0A10, cho0A11, cho0A12
    BigDecimal num0A02, num0A07
    ParamsTrb265 batchParams
    File fileTRO265
    Writer fileWriter
    ArrayList<TRS265A> listOfTRS265A
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd")
    String periodStart = ""
    String periodEnd = ""
    String queryDistrict = ""
    private DataSource dataSource;
    /**
     * Run the main batch.
     * @param b a Binding object passed from ScriptRunner
     */
    public void runBatch(Binding b) {
        init(b)
        dataSource = b.getVariable("dataSource");
        printSuperBatchVersion()
        info("runBatch Version : " + version)
        batchParams = params.fill(new ParamsTrb265())
        def req = request.request
        def param = req.getParameters().get("Parameters")

        printRequestParametersInformation()
        try {
            processBatch()
        } catch(Exception e) {
            e.printStackTrace()
            info("processBatch failed - ${e.getMessage()}")
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
        checkConstants()
        processRequest()
    }

    /**
     * Print the report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        fileWriter.close()
    }

    /**
     * Print the request parameters.
     */
    private void printRequestParametersInformation() {
        info("printRequestParametersInformation")
        info("reqPerFrom  : |${batchParams.reqPerFrom}|")
        info("reqPerTo    : |${batchParams.reqPerTo}|")
        info("reqDateFrom : |${batchParams.reqDateFrom}|")
        info("reqDateTo   : |${batchParams.reqDateTo}|")
        info("reqDist1    : |${batchParams.reqDist1}|")
        info("reqDist2    : |${batchParams.reqDist2}|")
        info("reqDist3    : |${batchParams.reqDist3}|")
        info("reqDist4    : |${batchParams.reqDist4}|")
        info("reqDist5    : |${batchParams.reqDist5}|")
        info("reqDist6    : |${batchParams.reqDist6}|")
        info("reqDist7    : |${batchParams.reqDist7}|")
        info("reqDist8    : |${batchParams.reqDist8}|")
        info("reqDist9    : |${batchParams.reqDist9}|")
        info("reqDist10   : |${batchParams.reqDist10}|")
        info("reqDist11   : |${batchParams.reqDist11}|")
        info("reqAtax1    : |${batchParams.reqAtax1}|")
        info("reqAtax2    : |${batchParams.reqAtax2}|")
        info("reqAtax3    : |${batchParams.reqAtax3}|")
        info("reqAtax4    : |${batchParams.reqAtax4}|")
        info("reqAtax5    : |${batchParams.reqAtax5}|")
        info("reqAtax6    : |${batchParams.reqAtax6}|")
        info("reqAtax7    : |${batchParams.reqAtax7}|")
        info("reqAtax8    : |${batchParams.reqAtax8}|")
        info("reqAtax9    : |${batchParams.reqAtax9}|")
        info("reqSupp1    : |${batchParams.reqSupp1}|")
        info("reqSupp2    : |${batchParams.reqSupp2}|")
        info("reqSupp3    : |${batchParams.reqSupp3}|")
        info("reqSupp4    : |${batchParams.reqSupp4}|")
        info("reqSupp5    : |${batchParams.reqSupp5}|")
        info("reqLevel    : |${batchParams.reqLevel}|")
    }

    /**
     * Initialize the working directory, output file, and other variables.
     */
    private void initialize() {
        info("initialize")
        workingDir = env.workDir
        //create fileTRO265 if not exists
        String fileNameTRO265 = "${workingDir}/${FILE_OUTPUT_NAME}"
        //get uuid
        String uuid = getUUID();
        if(uuid?.trim()){
            fileNameTRO265 = fileNameTRO265 + "." + uuid
        }
        fileTRO265 = new File(fileNameTRO265)
        if(!fileTRO265.exists()) {
            fileTRO265.createNewFile()
            info("${FILE_OUTPUT_NAME} created in ${fileTRO265.absolutePath}")
        }
        fileWriter = new FileWriter(fileTRO265)

        listOfTRS265A = new ArrayList<TRS265A>()
    }

    /**
     * Check the request parameter values.
     */
    private void checkConstants() {
        info("checkConstants")
        String wxAcctsPayCp = commarea.getProperty(WX_ACCTS_PAY_CP)
        //W90-REQ-PER-FROM
        if(batchParams.reqPerFrom?.trim()) {
            cho0A00 = batchParams.reqPerFrom
        } else {
            cho0A00 = wxAcctsPayCp
        }
        sub0A01 = cho0A00.substring(0, 2)
        try {
            num0A02 = new BigDecimal(sub0A01)
        } catch(NumberFormatException nex) {
            num0A02 = 0
        }
        if(num0A02 > 25) {
            cho0A03 = "19"
        } else {
            cho0A03 = "20"
        }

        //W90-REQ-PER-TO
        if(batchParams.reqPerTo?.trim()) {
            cho0A05 = batchParams.reqPerTo
        } else {
            cho0A05 = wxAcctsPayCp
        }
        sub0A06 = cho0A05.substring(0, 2)
        try {
            num0A07 = new BigDecimal(sub0A06)
        } catch(NumberFormatException nex) {
            num0A07 = 0
        }

        if(num0A07 > 25) {
            cho0A08 = "19"
        } else {
            cho0A08 = "20"
        }

        //W90-REQ-DATE-FROM
        if(batchParams.reqDateFrom?.trim()) {
            cho0A10 = batchParams.reqDateFrom
        } else {
            cho0A10 = dateFormat.format(new Date())
        }

        //W90-REQ-DATE-TO
        if(batchParams.reqDateTo?.trim()) {
            cho0A11 = batchParams.reqDateTo
        } else {
            cho0A11 = dateFormat.format(new Date())
        }

        cat0A09= " "
        cat0A09 = cho0A08 + cho0A05
        cat0A04 = " "
        cat0A04 = cho0A03 + cho0A00

        if((batchParams.reqPerFrom?.trim()  || batchParams.reqPerTo?.trim())
        && (batchParams.reqDateFrom?.trim() || batchParams.reqDateTo?.trim())) {
            cho0A12 = "1"
        } else if(cat0A09.compareTo(cat0A04) < 0) {
            cho0A12 = "1"
        } else if(cho0A11.compareTo(cho0A10) < 0) {
            cho0A12 = "1"
        } else if (!batchParams.reqLevel?.equals(ProcessTrb265.REQ_LEVEL_D)
        && !batchParams.reqLevel?.equals(ProcessTrb265.REQ_LEVEL_S)
        && !batchParams.reqLevel?.equals(ProcessTrb265.REQ_LEVEL_T)) {
            cho0A12 = "1"
        } else {
            cho0A12 = "0"
        }
    }

    /**
     * Process the request.
     * Populate the TRS265A records.
     * Sort the TRS265A records.
     * Write TRS265A records into the output file.
     */
    private void processRequest() {
        info("processRequest")
        inputProcess()
        Collections.sort(listOfTRS265A, new TRS265AComparator())
        outputProcess()
    }

    /**
     * Populate TRS265A records from Account District constrained by the request parameters.
     */
    private void inputProcess() {
        info("inputProcess")
        if((!batchParams.reqDist1?.trim()  && !batchParams.reqDist2?.trim()
        && !batchParams.reqDist3?.trim()   && !batchParams.reqDist4?.trim()
        && !batchParams.reqDist5?.trim()   && !batchParams.reqDist6?.trim()
        && !batchParams.reqDist7?.trim()   && !batchParams.reqDist8?.trim()
        && !batchParams.reqDist9?.trim()   && !batchParams.reqDist10?.trim()
        && !batchParams.reqDist11?.trim()) && cho0A12.equals("0")) {
            getRecordOption1()
        } else {
            getRecordOption2()
        }
    }

    /**
     * Populate TRS265A records from Account District where district status is A.
     */
    private void getRecordOption1() {
        info("getRecordOption1")
        Constraint cDstrctCode = MSF000_ADKey.dstrctCode.equalTo(" ")
        Constraint cDstrctStat = MSF000_ADRec.dstrctStatus.equalTo(MSF000_DSTRC_STATUS_A)
        QueryImpl qMSF000_AD = new QueryImpl(MSF000_ADRec.class).and(cDstrctCode).and(cDstrctStat)
        edoi.search(qMSF000_AD) { MSF000_ADRec msf000Rec->
            getTransactions(msf000Rec)
        }
    }

    /**
     * Populate TRS265A records from Account District where district status is A and 
     * Control Record Number matches the request parameters.
     */
    private void getRecordOption2() {
        info("getRecordOption2")
        Constraint cDstrctCode = MSF000_ADKey.dstrctCode.equalTo(" ")
        Constraint cDstrctStat = MSF000_ADRec.dstrctStatus.equalTo(MSF000_DSTRC_STATUS_A)
        QueryImpl qMSF000_AD = new QueryImpl(MSF000_ADRec.class).and(cDstrctCode).and(cDstrctStat)
        edoi.search(qMSF000_AD) {MSF000_ADRec msf000Rec->
            if(cho0A12.equals("0") && validateDistrictRecNo(msf000Rec.getPrimaryKey().controlRecNo)) {
                getTransactions(msf000Rec)
            }
        }
    }

    /**
     * Validate the Control Record Number, it should match one of the district parameters.
     * @param recNo Control Record Number
     * @return true if Control Record Number matches one of the district parameters, false otherwise.
     */
    private boolean validateDistrictRecNo(String recNo) {
        info("validateDistrictRecNo")
        if(recNo?.equals(batchParams.reqDist1) || recNo?.equals(batchParams.reqDist2)
        || recNo?.equals(batchParams.reqDist3) || recNo?.equals(batchParams.reqDist4)
        || recNo?.equals(batchParams.reqDist5) || recNo?.equals(batchParams.reqDist6)
        || recNo?.equals(batchParams.reqDist7) || recNo?.equals(batchParams.reqDist8)
        || recNo?.equals(batchParams.reqDist9) || recNo?.equals(batchParams.reqDist10)
        || recNo?.equals(batchParams.reqDist11)) {
            return true
        }
        return false
    }
// Create SQL statement
    private String qryString() {
        info("setQryString")
        StringBuffer qry = new StringBuffer()
        qry.append("select ")
        qry.append("b.tran_group_key, ")
        qry.append("a.journal_no, ")
        qry.append("a.dstrct_code, ")
        qry.append("a.process_date, ")
        qry.append("a.transaction_no, ")
        qry.append("a.userno, ")
        qry.append("a.rec900_type, ")
        qry.append("b.tran_amount, ")
        qry.append("b.account_code, ")
        qry.append("b.trndte_revsd ")
        qry.append("from msfx90 a ")
        qry.append("inner join ellipse.msf900 b ")
        qry.append("on b.dstrct_code = a.dstrct_code and ")
        qry.append("b.process_date = a.process_date and ")
        qry.append("b.transaction_no = a.transaction_no and ")
        qry.append("b.userno = a.userno and ")
        qry.append("b.rec900_type = a.rec900_type ")
        qry.append("where a.dstrct_code = '${queryDistrict}' ")
        qry.append("and a.full_period >= '${periodStart}' ")
        qry.append("and a.full_period <= '${periodEnd}' ")
        qry.append("and a.rec900_type in ('I','M') ")
        qry.append("and a.journal_no >= ' ' ")
        qry.append("order by ")
        qry.append("a.dstrct_code, a.journal_no, a.full_period, a.process_date, a.transaction_no, a.userno, a.rec900_type")
        info(qry.toString())
        return qry.toString()
    } 
    /**
     * Browse the Transaction records based on specified District.
     * @param msf000Rec an Account District / MSF000_ADRec record.
     */
    private void getTransactions(MSF000_ADRec msf000Rec) {
        info("getTransactions")
        def sql = new Sql(dataSource)
        periodStart = cat0A04.trim()
        periodEnd = cat0A09.trim()
        queryDistrict = msf000Rec.getPrimaryKey().controlRecNo
        sql.eachRow(qryString(), {
                TRS265A rec = new TRS265A()
                rec.setTranGroupKey(it.tran_group_key)
                rec.setJournalNo(it.journal_no)
                rec.setDstrctCode(it.dstrct_code)
                rec.setProcessDate(it.process_date)
                rec.setTransactionNo(it.transaction_no)
                rec.setUserNo(it.userno)
                rec.setRec900Type(it.rec900_type)
                rec.setTranAmmount(it.tran_amount)
                rec.setAccountCode(it.account_code)
                rec.setTrnDateRvsd(it.trndte_revsd)
                rec.setControlRecNo(msf000Rec.getPrimaryKey().controlRecNo)
                listOfTRS265A.add(rec)
            }
        )
    }

    /**
     * Process the sorted TRS265 list. The TRS265 list will be writen into the output file.
     */
    private void outputProcess() {
        info("outputProcess")
        boolean firstRecord = true
        long iTransactions = 0
        TRS265A tmp1, tmp2
        info("listOfTRS265A ${listOfTRS265A.size()}")
        listOfTRS265A.each {TRS265A rec->
            tmp2 = rec.createNewInstance()
            if(firstRecord) {
                tmp1 = tmp2.createNewInstance()
                firstRecord = false
            }
            debug("tmp2:" + tmp2.toString())
	    debug("tmp1:" + tmp1.toString())
            if((!tmp2.tranGroupKey.equals(tmp1.tranGroupKey)||(tmp2.tranGroupKey.equals(tmp1.tranGroupKey) && !tmp2.journalNo.equals(tmp1.journalNo))
            || (tmp2.tranGroupKey.equals(tmp1.tranGroupKey) && tmp2.journalNo.equals(tmp1.journalNo)
            && (!tmp2.dstrctCode.equals(tmp1.dstrctCode) || !tmp2.processDate.equals(tmp1.processDate) || !tmp2.transactionNo.equals(tmp1.transactionNo)
            || !tmp2.userNo.equals(tmp1.userNo) || !tmp2.rec900Type.equals(tmp1.rec900Type))))
            && tmp2.rec900Type.equals(MSF900_REC900_TYPE_I) && tmp1.rec900Type.equals(MSF900_REC900_TYPE_M)) {
                iTransactions++
                writeRecord(tmp1, tmp2)
            }
            tmp1 = tmp2.createNewInstance()
        }
        info("Wrote ${iTransactions.toString()} transactions to output file.")
    }

    /**
     * Write TRS265 records into the output file.
     * @param tmp1 first TRS265 record
     * @param tmp2 second TRS265 record
     */
    private void writeRecord(TRS265A tmp1, TRS265A tmp2) {
        debug("writeRecord")
        TRO265 line = new TRO265()
        line.key          = tmp2.processDate + tmp2.transactionNo + tmp2.userNo + tmp2.rec900Type
        line.acct         = tmp1.accountCode
        line.gstAcct      = tmp2.accountCode
        line.districtCode = tmp2.dstrctCode
        line.controlRecNo = tmp1.controlRecNo
        fileWriter.append(line.writeDetail())
    } 
}


/**
 * Run the script
 */
ProcessTrb265 process = new ProcessTrb265()
process.runBatch(binding)
