/**
 *  @Ventyx 2012
 *  Conversion from trb075.cbl
 *
 *  This program creates the Monthly Cash Book Summary report.
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;

import java.text.DecimalFormat;
import java.util.Comparator;

import com.mincom.ellipse.edoi.ejb.msf270.MSF270Key;
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Rec;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;

/**
 * Request Parameters for Trb075.
 * <li><code>period</code> : Period (Format YYYYMM)</li>
 * <li><code>bankAcctCode</code> : Bank Account Code</li>
 * <li><code>bankBranchCode</code> : Bank Branch Code</li>
 */
public class ParamsTrb075 {
    String period, bankAcctCode, bankBranchCode
}

/**
 * Report content for Trb075.
 */
public class ReportTrb075 {
    /*
     * Currency formatter, change a Numeric into "999,999,999.99"
     */
    static DecimalFormat currencyFormatter
    static {
        currencyFormatter = new DecimalFormat("###,###,###.##")
        currencyFormatter.setMinimumFractionDigits(2)
    }
    String chqDtl1Date
    double chqDtl1CompChqsVal, chqDtl1ManChqsVal, chqDtl1EftPaymntsVal, chqDtl1ManEftVal, chqDtl1CancChqsVal, chqDtl1TotalVal
    double chqsRepTotCompChqs, chqsRepTotManChqs, chqsRepTotEft, chqsRepTotManEft, chqsRepTotCancChqs, chqsRepMonthlyTotal

    /**
     * Write the report's title.
     * @return report's title "MMIS PAYMENTS - MONTHLY CASH BOOK SUMMARY"
     */
    public static String writeReportTitle() {
        return "MMIS PAYMENTS - MONTHLY CASH BOOK SUMMARY".center(132)
    }

    /**
     * Write report's header at first line, followed by date.
     * @param dateHdg a date
     * @return report's header at first line "Summary of all Computer and Manual Cheques, EFT and Manual EFT Payments and Cheque cancellations processed for the period <code>dateHdg</code>"
     */
    public static String writeReportHeading1(String dateHdg) {
        return String.format("Summary of all Computer and Manual Cheques, EFT and Manual EFT Payments and Cheque cancellations processed for the period %-6s", dateHdg)
    }

    /**
     * Write report's header at second line.
     * @return report's header at second line "Date     Computer Cheques   +  Manual Cheques   +   EFT Payments   +  Man. EFT Payments  -  Cancelled Cheques  =    Daily Total"
     */
    public static String writeReportHeading2() {
        return "    Date     Computer Cheques   +  Manual Cheques   +   EFT Payments   +  Man. EFT Payments  -  Cancelled Cheques  =    Daily Total "
    }

    /**
     * Write dashed line to separate report detail and the footer.
     * @return dashed line
     */
    public static String writeReportTotalLine() {
        return "             ----------------    ----------------   ----------------       ----------------      ----------------   ---------------"
    }

    /**
     * Write the report's detail.
     * @return report's detail
     */
    public String writeChqReportDetail() {
        return String.format("    %-8s   %14s      %14s     %14s         %14s        %14s    %14s",
        chqDtl1Date, currencyFormatter.format(chqDtl1CompChqsVal), currencyFormatter.format(chqDtl1ManChqsVal), currencyFormatter.format(chqDtl1EftPaymntsVal),
        currencyFormatter.format(chqDtl1ManEftVal), currencyFormatter.format(chqDtl1CancChqsVal), currencyFormatter.format(chqDtl1TotalVal))
    }

    /**
     * Write the total into the report's footer.
     * @return report's footer
     */
    public String writeReportTotal() {
        return String.format("               %14s      %14s     %14s         %14s        %14s    %14s",
        currencyFormatter.format(chqsRepTotCompChqs), currencyFormatter.format(chqsRepTotManChqs), currencyFormatter.format(chqsRepTotEft),
        currencyFormatter.format( chqsRepTotManEft), currencyFormatter.format(chqsRepTotCancChqs), currencyFormatter.format(chqsRepMonthlyTotal))
    }
}

/**
 * Record used for sorting process in Trb075.
 */
public class TRS075 {
    String date
    int categoryIndicator
    double chequeAmmount

    /**
     * Initialize TRS075 with default value. <br>
     * Set <code>date</code> to empty string. <br>
     * Set <code>categoryIndicator</code> to 0. <br>
     * Set <code>chequeAmmount</code> to 0. <br>
     */
    public TRS075() {
        date = " "
        categoryIndicator = 0
        chequeAmmount = 0.0d
    }
}

/**
 * Comparator for TRS075 used in sorting process. <br>
 * Sort TRS075 based on <code>date</code>.
 */
public class Trs075RecordComparator implements Comparator<TRS075> {

    @Override
    public int compare(TRS075 o1, TRS075 o2) {
        return o1.getDate().compareTo(o2.getDate());
    }
}

/**
 * Main Process of Trb075
 */
public class ProcessTrb075 extends SuperBatch {
    /*
     * Constants
     */
    static final String REPORT_NAME = "TRB075A"
    static final int MSF270_CHEQUE_STATUS_1 = 1
    static final int MSF270_CHEQUE_STATUS_2 = 2
    static final int MSF270_CHEQUE_STATUS_3 = 3
    static final int MSF270_CHEQUE_STATUS_4 = 4
    static final int MSF270_CHEQUE_STATUS_5 = 5
    static final String MSF270_CHEQUE_TYPE_E = "E"
    static final String MSF270_CHEQUE_TYPE_C = "C"
    static final String MSF270_CHEQUE_TYPE_M = "M"

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    int version = 1

    /*
     * Variables
     */
    ParamsTrb075 batchParams
    ArrayList<TRS075> listOfTrs075
    def reportWriter
    String savedDate
    double compChqAmt, manChqAmt, eftAmt, manEftAmt, cancChqAmt, totCompChqAmt, totManChqAmt, totEftAmt, totManEftAmt, totCancChqAmt, totalAmmount, totalMonthlyAmmount

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version  : " + version);
        //Get request parameters
        batchParams = params.fill(new ParamsTrb075())
        info("Period            : ${batchParams.period}")
        info("Bank Account Code : ${batchParams.bankAcctCode}")
        info("Bank Branch  Code : ${batchParams.bankBranchCode}")

        try {
            processBatch()
        } catch(Exception e) {
            e.printStackTrace()
            info("Process Batch failed. Exception occurs: ${e.getMessage()}")
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
        writeToSortList()
        Collections.sort(listOfTrs075, new Trs075RecordComparator())
        generateReport()
    }

    /**
     * Print the report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        ReportTrb075 reportLine = new ReportTrb075()
        reportLine.setChqsRepTotCompChqs(totCompChqAmt)
        reportLine.setChqsRepTotManChqs(totManChqAmt)
        reportLine.setChqsRepTotEft(totEftAmt)
        reportLine.setChqsRepTotManEft(totManEftAmt)
        reportLine.setChqsRepTotCancChqs(totCancChqAmt)
        reportLine.setChqsRepMonthlyTotal(totalMonthlyAmmount)
        reportWriter.write(ReportTrb075.writeReportTotalLine())
        reportWriter.write(reportLine.writeReportTotal())
        reportWriter.write(ReportTrb075.writeReportTotalLine())
        reportWriter.close()
    }

    /**
     * Initialize the report writer and other variables.
     */
    private void initialize() {
        info("initialize")
        savedDate = " "
        compChqAmt = manChqAmt = eftAmt = manEftAmt = cancChqAmt = totCompChqAmt = totManChqAmt = totEftAmt = totManEftAmt = totCancChqAmt = totalAmmount = totalMonthlyAmmount = 0
        listOfTrs075 = new ArrayList<TRS075>()
        //Open and write report header
        reportWriter = report.open(REPORT_NAME)
        reportWriter.write(ReportTrb075.writeReportTitle())
        reportWriter.writeLine(132,"-")
        reportWriter.write(ReportTrb075.writeReportHeading1(batchParams.period))
        reportWriter.writeLine(132,"-")
        reportWriter.write(ReportTrb075.writeReportHeading2())
        reportWriter.writeLine(132,"-")
    }

    /**
     * Populate TRS075 from Cheque Master records. <br>
     * Browse Cheque Master records with specified Branch Code, Bank Account Number, and period.
     */
    private void writeToSortList() {
        info("writeToSortList")
        Constraint cBranchCode   = MSF270Key.branchCode.equalTo(batchParams.bankBranchCode)
        Constraint cBankAcctCode = MSF270Key.bankAcctNo.equalTo(batchParams.bankAcctCode)
        Constraint cChequeNo     = MSF270Key.chequeNo.greaterThanEqualTo(" ")
        Constraint cChequeStatus = MSF270Rec.chequeStatus.notEqualTo(String.valueOf(MSF270_CHEQUE_STATUS_4))
        Constraint cFullPerWrtn  = MSF270Rec.fullPerWrtn.equalTo(batchParams.period.trim())
        Constraint cFullPerCanc  = MSF270Rec.fullPerCanc.equalTo(batchParams.period.trim())
        Constraint cPeriod       = cFullPerWrtn.or(cFullPerCanc)

        QueryImpl qMSF270 = new QueryImpl(MSF270Rec.class).and(cBranchCode).and(cBankAcctCode).and(cChequeStatus).and(cChequeNo).and(cPeriod)
        edoi.search(qMSF270) { MSF270Rec msf270Rec->
            boolean skip = false
            if(msf270Rec.fullPerCanc.equals(batchParams.period.trim())) {
                TRS075 rec = new TRS075()
                rec.setCategoryIndicator(ProcessTrb075.MSF270_CHEQUE_STATUS_4)
                rec.setDate(msf270Rec.cancelledDate)
                rec.setChequeAmmount(msf270Rec.valWrtnLoc)
                listOfTrs075.add(rec)
                skip = !msf270Rec.fullPerWrtn.equals(batchParams.period.trim())
            }
            if(!skip) {
                TRS075 rec = new TRS075()
                rec.setDate(msf270Rec.writtenDate)
                //Evaluate MSF270-CHEQUE-TYPE
                String msf270RecChequeType = msf270Rec.chequeType
                String msf270RecChequeNo = msf270Rec.getPrimaryKey().chequeNo
                switch(msf270RecChequeType) {
                    case MSF270_CHEQUE_TYPE_C :
                        int msf270RecChequeNo_9 = msf270RecChequeNo as int
                        if(msf270RecChequeNo_9 > 399999 && msf270RecChequeNo_9 < 500000) {
                            rec.setCategoryIndicator(MSF270_CHEQUE_STATUS_3)
                        } else {
                            if(msf270RecChequeNo_9 > 999999) {
                                rec.setCategoryIndicator(MSF270_CHEQUE_STATUS_3)
                            } else {
                                rec.setCategoryIndicator(MSF270_CHEQUE_STATUS_1)
                            }
                        }
                        break
                    case MSF270_CHEQUE_TYPE_E :
                        rec.setCategoryIndicator(MSF270_CHEQUE_STATUS_3)
                        break
                    case MSF270_CHEQUE_TYPE_M :
                        int msf270RecChequeNo_9 = msf270RecChequeNo as int
                        if(msf270RecChequeNo_9 > 549999 && msf270RecChequeNo_9 < 560000) {
                            rec.setCategoryIndicator(MSF270_CHEQUE_STATUS_5)
                        } else {
                            rec.setCategoryIndicator(MSF270_CHEQUE_STATUS_2)
                        }
                        break
                }
                //End Evaluate MSF270-CHEQUE-TYPE
                //If categoryIndicator is not 0, add record into the list
                if(rec.categoryIndicator > 0) {
                    rec.setChequeAmmount(msf270Rec.valWrtnLoc)
                    listOfTrs075.add(rec)
                }
            }
        }
    }

    /**
     * Process the sorted TRS075 list. The record will be written into the report file.
     */
    private void generateReport() {
        info("generateReport")
        if(!listOfTrs075.isEmpty()) {
            listOfTrs075.each {TRS075 rec->
                if((savedDate?.trim()) && !savedDate.equals(rec.date)) {
                    writeReportDetail()
                    compChqAmt = manChqAmt = eftAmt = manEftAmt = cancChqAmt = totalAmmount = 0
                }
                savedDate = rec.date
                //Evaluate TRS075A-CATEGORY-INDICATOR
                switch(rec.categoryIndicator) {
                    case MSF270_CHEQUE_STATUS_1:
                        compChqAmt += rec.chequeAmmount
                        break
                    case MSF270_CHEQUE_STATUS_2:
                        manChqAmt += rec.chequeAmmount
                        break
                    case MSF270_CHEQUE_STATUS_3:
                        eftAmt += rec.chequeAmmount
                        break
                    case MSF270_CHEQUE_STATUS_4:
                        cancChqAmt += rec.chequeAmmount
                        break
                    case MSF270_CHEQUE_STATUS_5:
                        manEftAmt += rec.chequeAmmount
                        break
                }
            }
            writeReportDetail()
        }
    }

    /**
     * Write the TRS075 information into the report file.
     */
    private void writeReportDetail() {
        info("writeReportDetail")
        ReportTrb075 reportLine = new ReportTrb075()
        reportLine.setChqDtl1Date(convertDateFormat(savedDate))
        reportLine.setChqDtl1CompChqsVal(compChqAmt)
        reportLine.setChqDtl1ManChqsVal(manChqAmt)
        reportLine.setChqDtl1EftPaymntsVal(eftAmt)
        reportLine.setChqDtl1ManEftVal(manEftAmt)
        reportLine.setChqDtl1CancChqsVal(cancChqAmt)
        totalAmmount = compChqAmt + manChqAmt + eftAmt + manEftAmt - cancChqAmt
        reportLine.setChqDtl1TotalVal(totalAmmount)
        reportWriter.write(reportLine.writeChqReportDetail())
        totCompChqAmt       += compChqAmt
        totManChqAmt        += manChqAmt
        totEftAmt           += eftAmt
        totManEftAmt        += manEftAmt
        totCancChqAmt       += cancChqAmt
        totalMonthlyAmmount += totalAmmount
    }

    /**
     * Convert the date format into <code>(YY/MM/DD)</code>.
     * @param dateS date as a String
     * @return formatted date <code>(YY/MM/DD)</code>
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat")
        String procString   = dateS.padRight(8)
        def formattedString = procString.substring(6) + "-" + procString.substring(4,6) + "-" + procString.substring(2,4)
        return formattedString
    }

}

/**
 * Run the script
 */
ProcessTrb075 process = new ProcessTrb075()
process.runBatch(binding)
