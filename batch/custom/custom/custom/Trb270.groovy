/**
 * 
 * This program reformats the input sequential file
 * into a standard format readable by programs in the presented
 * cheque update/validation suite.
 * 
 * FDD Batch Interfacing E8 TransGrid Presented Cheques D02.
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.mincom.ellipse.edoi.ejb.msf000.*;
import com.mincom.ellipse.eroi.linkage.mssbnk.*;
import com.mincom.reporting.text.TextReport;

/**
 * Request parameter for Trb270.
 */
public class ParamsTrb270{
    String districtCode, branchCode, bankAcctNumber;
}

/**
 * Output Record.
 */
public class Mst271Rec {
    static DecimalFormat currencyFormatter
    static {
        currencyFormatter = new DecimalFormat("#############.##")
        currencyFormatter.setMinimumFractionDigits(2)
        currencyFormatter.setDecimalSeparatorAlwaysShown(false)
    }

    //Header
    private String batchNumber, dstrctCode, branchCode, bankAcctNo
    private long recordCounter
    //Item
    private String chequeNo, presentedDate, chequeSign, ammount,
    chequeInd, pmtSupplier;
    private BigDecimal valPresLoc

    public Mst271Rec() {
        //Header
        batchNumber = branchCode = bankAcctNo = ""
        recordCounter = 0
        //Item
        chequeNo = presentedDate = chequeSign = ammount = pmtSupplier = ""
        valPresLoc = new BigDecimal(0)
        chequeInd = "Y"

    }

    public String writeLine() {
        StringBuilder sb = new StringBuilder()
        sb.append(batchNumber.padRight(10).substring(0, 10))
        sb.append(String.valueOf(recordCounter).padLeft(10).
                substring(0, 10).replace(" ", "0"))
        sb.append(dstrctCode.padRight(4).substring(0, 4))
        sb.append(branchCode.padRight(15).substring(0, 15))
        sb.append(bankAcctNo.padRight(20).substring(0, 20))
        sb.append(chequeNo.padRight(18).substring(0, 18))
        sb.append(presentedDate.padRight(8).substring(0, 8))
        sb.append(currencyFormatter.format(valPresLoc).replace(".", "").
                replace("-", "").padLeft(15).substring(0, 15))
        sb.append(chequeSign.padRight(3).substring(0, 3))
        sb.append(chequeInd.substring(0, 1))
        sb.append(pmtSupplier.padRight(6).substring(0, 6))
        return sb.toString()
    }
}

/**
 * Main process of Trb270.
 */
public class ProcessTrb270 extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME      = "TRB270A"
    private static final String ERROR_REPORT     = "TRB270E"
    private static final String INPUT_FILE_NAME  = "MST270"
    private static final String OUTPUT_FILE_NAME = "MST271"
    private static final String CHEQUE_ID        = "WITHDRAWAL/CHEQUE"
    private static final String DSTRCT_CODE      = "GRID"
    private static final SimpleDateFormat BTCHNUM_SDF = new SimpleDateFormat("yyyyMMddHH")

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 1
    private ParamsTrb270 batchParams
    private def reportA, reportE
    private BufferedReader mst270FileReader
    private FileWriter mst271FileWriter
    private File mst270InputFile, mst271OutputFile
    private long readCounter, errorCounter, processCounter
    private Date batchDate
    private boolean headerExist

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : ${version}")
        batchParams = params.fill(new ParamsTrb270())
        info("Param District Code   : ${batchParams.districtCode}")
        info("Param Branch Code     : ${batchParams.branchCode}")
        info("Param Account Number  : ${batchParams.bankAcctNumber}")

        try {
            initialize()
            //6.1.2.1 Request Parameter Validation
            if(validateRequestParam()) {
                processBatch()
            }
        } catch(Exception e) {
            info("Process terminated. ${e.getMessage()}")
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB270 ERROR TRACE ", e)
        } finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        processInputFile()
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        if(reportA) {
            reportA.write("")
            reportA.write("Presented Cheques Formatting Control Report".center(132))
            reportA.write("")
            reportA.writeLine(132,"-")
            reportA.write("")
            reportA.write(String.format(
                    "%34s NUMBER OF RECORDS READ               :         % 6d",
                    " ", readCounter))
            reportA.write(String.format(
                    "%34s NUMBER OF RECORDS REJECTED           :         % 6d",
                    " ", errorCounter))
            reportA.write(String.format(
                    "%34s NUMBER OF RECORDS WRITTEN            :         % 6d",
                    " ", processCounter))
            reportA.write("")
            reportA.close()
        }
        if(reportE) {
            reportE.close()
        }
        if(mst270FileReader) {
            mst270FileReader.close()
        }
        if(mst271FileWriter) {
            mst271FileWriter.close()
        }
    }

    /**
     * Initialize the report writer, input file reader, and the counters.
     */
    private void initialize() {
        info("initialize")
        readCounter  = errorCounter = processCounter = 0
        batchDate    = Calendar.getInstance().getTime()
        String uuid  = getTaskUUID()
        //open report
        reportA     = report.open(REPORT_NAME)
        //open input file
        String mst270FileName = "${env.workDir}/${INPUT_FILE_NAME}"
        if(uuid?.trim()){
            mst270FileName += "." + uuid
        }
        //mst270FileName += ".csv"
        mst270InputFile = new File(mst270FileName)
        if(mst270InputFile.exists()) {
            mst270FileReader = new BufferedReader(new FileReader(mst270InputFile))
        }
        //open output file
        String mst271FileName = "${env.workDir}/${OUTPUT_FILE_NAME}"
        if(uuid?.trim()){
            mst271FileName += "." + uuid
        }
        mst271OutputFile = new File(mst271FileName)
        if(!mst271OutputFile.exists()) {
            mst271OutputFile.createNewFile()
        }
        mst271FileWriter = new FileWriter(mst271OutputFile)
    }

    /**
     * Proces the input file.
     */
    private void processInputFile() {
        info("processInputFile")
        if(mst270FileReader) {
            String line = null
            while ((line = mst270FileReader.readLine())) {
                //skip the header
                if(line.startsWith("TRAN_DATE,ACCOUNT_NO,")) {
                    continue
                }
                readCounter++
                //read the line by splitting it based on ','
                String[] fields = line.split(",")
                if(fields.length < 9) {
                    writeErrorToReport(readCounter, "COMMA",
                            "Invalid Comma Separator.")
                    errorCounter++
                    continue
                }
                String transactionDate = fields[0]?.trim()
                String accountNumber   = fields[1]?.trim()
                String accountName     = fields[2]?.trim()
                String currency        = fields[3]?.trim()
                String closingTotal    = fields[4]?.trim()
                String amount          = fields[5]?.trim()
                String transactionCode = fields[6]?.trim()
                String narrative       = fields[7]?.trim()
                String serialNumber    = fields[8]?.trim()

                //6.1.3.3 Identify cheque transactions.
                String chequeId = narrative.padRight(17).substring(0,17)
                if(CHEQUE_ID.equals(chequeId)) {
                    //A further check is that the TRAN_CODE should be zero.
                    BigDecimal tranCode
                    try {
                        tranCode = new BigDecimal(transactionCode)
                    } catch(NumberFormatException) {
                        info("Invalid Transaction Code.")
                    }
                    if(tranCode != null && tranCode == 0) {
                        Mst271Rec mst271Rec  = new Mst271Rec()
                        //if the BSB from req param starts with 0 and the account
                        //number not start with 0, append account number with 0
                        if(batchParams.branchCode.charAt(0) == '0' &&
                        accountNumber.charAt(0) != '0') {
                            accountNumber = "0".concat(accountNumber)
                        }
                        //if the 3rd char BSB from req param is dash and the account
                        //number does not contain dash, separate account number with dash
                        if(batchParams.branchCode.charAt(3) == '-' &&
                        accountNumber.charAt(3) != '-') {
                            accountNumber = accountNumber.substring(0, 3).
                                    concat("-").concat(accountNumber.substring(3))
                        }

                        int branchCodeLength = accountNumber.charAt(0) != '0' ?
                                5 : accountNumber.charAt(3) == '-' ? 7 : 6

                        mst271Rec.branchCode    = accountNumber.substring(0, branchCodeLength)
                        mst271Rec.bankAcctNo    = accountNumber.substring(branchCodeLength)
                        mst271Rec.chequeNo      = serialNumber
                        mst271Rec.ammount       = amount
                        mst271Rec.presentedDate = transactionDate

                        if(validateOutputRecord(mst271Rec)) {
                            processCounter++
                            //6.1.3.5 Reformat the input line
                            mst271Rec = reformatOutputRecord(mst271Rec)
                            //write into output file
                            writeToOutput(mst271Rec)
                        } else {
                            errorCounter++
                        }
                    } else {
                        writeErrorToReport(readCounter, transactionCode,
                                "Invalid Transaction Code.")
                        errorCounter++
                    }
                } else {
                    writeErrorToReport(readCounter, chequeId,
                            "Invalid Cheque Identifier.")
                    errorCounter++
                }
            }
        } else {
            reportE = report.open(ERROR_REPORT)
            //write report header
            reportE.write("")
            reportE.write("Presented Cheques Formatting Control Error Summary Report".center(132))
            reportE.write("")
            reportE.writeLine(132,"-")
            reportE.write("")
            reportE.write("Input file ${INPUT_FILE_NAME}.csv not found in \\AP_CHQ\\Presented directory.")
            reportE.write("")
        }
    }

    /**
     * Reformat the MST271 record so it could be read by MSB27A.
     * @param mst271Rec MST271 record
     * @return formatted MST271 record
     */
    private Mst271Rec reformatOutputRecord(Mst271Rec mst271Rec) {
        info("reformatOutputRecord")
        mst271Rec.dstrctCode    = batchParams.districtCode
        mst271Rec.batchNumber   = BTCHNUM_SDF.format(batchDate)
        mst271Rec.recordCounter = readCounter
        //Tran-Date column is reformatted from ccyymmdd to ddmmccyy.
        mst271Rec.presentedDate = convertDateFormat(mst271Rec.presentedDate)
        //The negative sign on the Amount column is removed
        mst271Rec.valPresLoc = new BigDecimal(mst271Rec.ammount)
        mst271Rec.chequeSign = mst271Rec.valPresLoc >= 0 ? "+" : "-"

        return mst271Rec
    }

    /**
     * Validate MST271 Record.
     * @param mst271Rec MST271 Record
     * @return true if valid
     */
    private boolean validateOutputRecord(Mst271Rec mst271Rec) {
        info("validateOutputRecord |${mst271Rec.branchCode}| |${mst271Rec.bankAcctNo}|")
        //6.1.3.4 Validate the Bank Details and Cheque Number
        try {
            //Validate the eighth field contains a valid cheque number
            //the cheque number range should between 300001 to 900000
            BigDecimal chequeNo = new BigDecimal(mst271Rec.chequeNo)
            if(!(chequeNo > 300001 && chequeNo < 900000)) {
                writeErrorToReport(readCounter, mst271Rec.chequeNo,
                        "Invalid Cheque Number.")
                return false
            }
        } catch(java.lang.NumberFormatException e) {
            writeErrorToReport(readCounter, mst271Rec.chequeNo,
                    "Invalid Cheque Number.")
            return false
        }
        //Validate the Bank Code and the Account Code matches the parameters given
        if(!batchParams.branchCode.trim().equals(mst271Rec.branchCode.trim())) {
            writeErrorToReport(readCounter, mst271Rec.branchCode,
                    "Invalid Branch Code.")
            return false
        }
        if(!batchParams.bankAcctNumber.trim().equals(mst271Rec.bankAcctNo.trim())) {
            writeErrorToReport(readCounter, mst271Rec.bankAcctNo,
                    "Invalid Bank Account Number.")
            return false
        }
        //Validate the cheque ammont, it should be numeric
        try {
            BigDecimal valPresLoc = new BigDecimal(mst271Rec.ammount)
        } catch(java.lang.NumberFormatException e) {
            writeErrorToReport(readCounter, mst271Rec.ammount,
                    "Invalid Cheque Amount.")
            return false
        }

        return true
    }

    /**
     * Validate the request parameter.</br>
     * The district is to be validated against the current active Ellipse districts.</br>
     * The branch code and account number is to be validated against MSSBNK.</br>
     * @return true if request paramete is valid
     */
    private boolean validateRequestParam() {
        info("validateRequestParam")

        if(!checkActiveDistrict(batchParams.districtCode)) {
            writeErrorToReport(0, batchParams.districtCode,
                    "Inactive District Code.")
            return false
        }

        if(!batchParams.bankAcctNumber?.trim()) {
            writeErrorToReport(0, "BANK ACCOUNT NUMBER",
                    "Input Required.")
            return false
        }

        if(!batchParams.branchCode?.trim()) {
            writeErrorToReport(0, "BRANCH CODE",
                    "Input Required.")
            return false
        }

        MSSBNKLINK mssBnklink = eroi.execute("MSSBNK", {MSSBNKLINK mssBnk->
            mssBnk.setOption("V")
            mssBnk.setInpDstrctCode(batchParams.districtCode)
            mssBnk.setInpBankAcctNo(batchParams.bankAcctNumber)
            mssBnk.setInpBranchCode(batchParams.branchCode)
        })

        boolean validBankAcct = false
        switch(mssBnklink.getReturnStatus()) {
            case "0":
                validBankAcct = true
                break
            case "1":
                validBankAcct = false
                writeErrorToReport(0, batchParams.bankAcctNumber,
                        "Bank Account Not Avaliable for Use.")
                break
            case "2":
                validBankAcct = false
                writeErrorToReport(0, batchParams.bankAcctNumber,
                        "Invalid Bank Account Owner.")
                break
            case "3":
                validBankAcct = false
                writeErrorToReport(0, batchParams.bankAcctNumber,
                        "Invalid Bank District.")
                break
            case "4":
                validBankAcct = false
                writeErrorToReport(0, batchParams.bankAcctNumber,
                        "Invalid Bank Account.")
                break
            default:
                validBankAcct = false
                writeErrorToReport(0, "ACCOUNT NUMBER/BRANCH CODE",
                        "Invalid Input.")
                break
        }

        return validBankAcct
    }


    /**
     * Read the district from MSF000_AD
     * @param dstrctCode district code
     * @return MSF000_ADRec
     */
    private MSF000_ADRec readDistrict(String dstrctCode) {
        info("readDistrict |${dstrctCode}|")

        try {
            MSF000_ADKey msf000ADKey = new MSF000_ADKey()
            msf000ADKey.setControlRecNo(dstrctCode)
            msf000ADKey.setControlRecType("AD")
            msf000ADKey.setDstrctCode(" ")
            return edoi.findByPrimaryKey(msf000ADKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("msf000ADKey ${dstrctCode} not found!")
            return null
        }
    }

    /**
     * Check if a District is active.
     * @param dstrctCode district code
     * @return true if active
     */
    private boolean checkActiveDistrict(String dstrctCode) {
        info("checkActiveDistrict")
        return readDistrict(dstrctCode)?.getDstrctStatus()?.trim().equals("A")
    }

    /**
     * Write MST271 into output file.
     * @param mst271Rec MST271 record
     */
    private void writeToOutput(Mst271Rec mst271Rec) {
        info("writeToOutput")
        mst271FileWriter.write(mst271Rec.writeLine())
        mst271FileWriter.write("\n")
    }

    /**
     * Write error detail into report.
     * @param line error line
     * @param message error message
     */
    private void writeErrorToReport(long line, String field, String message) {
        info("writeErrorToReport")
        if(!headerExist) {
            reportE = report.open(ERROR_REPORT)
            //write report header
            reportE.write("")
            reportE.write("Presented Cheques Formatting Control Error Summary Report".center(132))
            reportE.write("")
            reportE.writeLine(132,"-")
            reportE.write("Line/Field Ref/Value              Error/Warning Message Column Hdg")
            reportE.writeLine(132,"-")
            reportE.write("")
            headerExist = true
        }
        reportE.write(String.format("% 4d - %-25s  %-90s",
                line,
                field.padRight(25).substring(0,25),
                message.padRight(90).substring(0, 90)))
    }

    /**
     * Convert a date string from ccyymmdd to ddmmccyy
     * @param s string of a date
     * @return specified date format "ddmmccyy"
     */
    private String convertDateFormat(String s) {
        info("convertDateFormat")
        s = s.padLeft(8).replace(" ", "0")
        StringBuilder sb = new StringBuilder()
        sb.append(s.substring(6))
        sb.append(s.substring(4,6))
        sb.append(s.substring(0,4))
        return sb.toString()
    }
}

/**
 * Run the script
 */
ProcessTrb270 process = new ProcessTrb270()
process.runBatch(binding)