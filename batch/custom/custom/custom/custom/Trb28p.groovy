/**
 * @Ventyx 2012
 * Conversion from ecb28p.cbl
 *
 * This program creates the Presented Cheques Daily Totals Report.
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.text.DecimalFormat;

import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.ejb.msf200.MSF200Key;
import com.mincom.ellipse.edoi.ejb.msf200.MSF200Rec;
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Key;
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Rec;
import com.mincom.ellipse.eroi.linkage.mssdat.MSSDATLINK;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.*;
import com.mincom.eql.impl.*;

/**
 * Request Parameters for Trb28p.
 * <li><code>period</code> : Period (Format YYYYMM)</li>
 * <li><code>bankDistrict</code> : Bank District</li>
 * <li><code>bankAccountCode</code> : Bank Account Code</li>
 * <li><code>bankBranchCode</code> : Bank Branch Code</li>
 */
public class ParamsTrb28p {
    String period
    String bankDistrict
    String bankAccountCode
    String bankBranchCode
}

/**
 * Report content for Trb28p.
 */
public class ReportTrb28p implements Comparable {

    private static DecimalFormat currencyFormatter
    static {
        currencyFormatter = new DecimalFormat("#,###,###,###.##")
        currencyFormatter.setMinimumFractionDigits(2)
    }

    private String presentedDate, chequeNo, supplierNo, supplierName
    private double chequeValue

    /**
     * Return report detail.
     * @return report detail
     */
    public String writeReportDetail() {
        return " ".padRight(19) + String.format("%-8s        %-6s        %-6s    %-40s %15s",
        presentedDate.padRight(8).substring(0, 8),
        chequeNo.padRight(6).substring(0, 6),
        supplierNo.padRight(6).substring(0, 6),
        supplierName.padRight(40).substring(0, 40),
        currencyFormatter.format(chequeValue))
    }

    @Override
    public int compareTo(Object another) {
        if(another == null) {
            return -1
        }
        if(!(another instanceof ReportTrb28p)) {
            return -1
        }
        ReportTrb28p o2 = (ReportTrb28p) another
        int presentedDateComp = this.presentedDate.compareTo(o2.presentedDate)
        return  ((presentedDateComp == 0) ? this.chequeNo.compareTo(o2.chequeNo) : presentedDateComp);
    }
}

/**
 * Main Process of Trb28p.
 */
public class ProcessTrb28p extends SuperBatch {

    /*
     * Constants
     */
    private static final String REPORT_NAME = "TRB28PA"
    private static final String MSF270_CHQ_STATUS_31 = "31"
    /**
     * Decimal formatter, parse numeric value to "9,999,999.99".
     */
    private static DecimalFormat currencyFormatter
    static {
        currencyFormatter = new DecimalFormat("#,###,###,###.##")
        currencyFormatter.setMinimumFractionDigits(2)
    }

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private int version = 4

    /*
     * Variables
     */
    private int rowCount = 0
    private ParamsTrb28p batchParams
    private def reportWriter
    private String savedPresentedDate, startDate, endDate
    private double totalChequeAmmount, totalDlChequeAmmount
    private boolean aborted = false
    private List<ReportTrb28p> listOfMSF270

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        //Get request parameters
        batchParams = params.fill(new ParamsTrb28p())
        info("period          : " + batchParams.period)
        info("bankDistrict    : " + batchParams.bankDistrict)
        info("bankAccountCode : " + batchParams.bankAccountCode)
        info("bankBranchCode  : " + batchParams.bankBranchCode)
        try {
            processBatch()
        }
        catch(Exception e) {
            aborted = true
            e.printStackTrace()
            info("processBatch failed - ${e.getMessage()}")
        }
        finally{
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        initialize()
        //browse MSF270 and put the browsed records into the list
        browseChequeMaster()
        //sort the MSF270 list using java.util.Collections.sort
        java.util.Collections.sort(listOfMSF270)
        //declare 'last', a temporary variable to hold the last browsed ReportTrb28p from the list
        ReportTrb28p last
        listOfMSF270.each {
            printReportDetail(it)
            last = it
            rowCount++
        }
        //if 'last' is not null, print the daily sub total summary
        if(last != null) {
            printReportDailySubTotal()
        }
    }

    /**
     * Print the report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        if(!aborted) {
            if(rowCount > 0) {
                reportWriter.write(" ".padRight(97) + String.format("%18s", " ").replace(' ', '-'))
                reportWriter.write(" ".padRight(97) + String.format("%18s", " ").replace(' ', '='))
                reportWriter.write(" ".padRight(45) + String.format("Total value of Cheques Presented during %-6s   :  \$  %15s",
                        batchParams.period, "${currencyFormatter.format(totalChequeAmmount)}"))
                reportWriter.write(" ".padRight(97) + String.format("%18s", " ").replace(' ', '='))
            } else {
                reportWriter.write("NO PRESENTED CHEQUES FOUND".center(132))
            }
        }
        reportWriter.close()
    }

    /**
     * Initialize the report writer and other variables.
     */
    private void initialize() {
        info("initialize")
        listOfMSF270 = new ArrayList<ReportTrb28p>()
        def periodYrMn = batchParams.period.padRight(6).substring(2,6)
        //Get the start and end of Report Period from the MSSDAT
        MSSDATLINK mssdatlink = eroi.execute ('MSSDAT', {MSSDATLINK mssdatlink ->
            mssdatlink.option  = "H"
            mssdatlink.periodYrmn  = periodYrMn
            mssdatlink.periodCcYrmn = batchParams.period
        })
        savedPresentedDate = " "
        startDate = String.valueOf(mssdatlink.getStartingDate19())
        endDate   = String.valueOf(mssdatlink.getEndingDate19())
        //Open and write report header
        reportWriter = report.open(REPORT_NAME)
        reportWriter.write("DAILY CHEQUES PRESENTED SUMMARY REPORT".center(132))
        reportWriter.writeLine(132,"-")
        reportWriter.write(" ".padRight(34) +
                String.format("List of all Cheques Presented during the Period   %s",
                batchParams.period))
        reportWriter.write(" ".padRight(21) + "Date".padRight(14) +
                "Cheque".padRight(14) + "Supplier  " + "Supplier".padRight(48) + "Cheque")
        reportWriter.write(" ".padRight(18) + "Presented".padRight(17) + "Number".padRight(14) +
                "Number".padRight(10) + "Name".padRight(48) + "Amount")
        reportWriter.writeLine(132,"-")
    }

    /**
     * Convert a String into a specified date format "YY-MM-DD"
     * @param dateS string of a date
     * @return specified date format "YY-MM-DD"
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat")
        dateS = dateS.padRight(8).replace(" ", "0")
        def formattedString = dateS.substring(6) + "-" + dateS.substring(4,6) +
                "-" + dateS.substring(2,4)
        return formattedString
    }

    /**
     * Browse the Cheque Master records constraine by the request parameters.
     */
    private void browseChequeMaster() {
        info("browseChequeMaster")
        Constraint cBranchCode     = MSF270Key.branchCode.equalTo(batchParams.bankBranchCode)
        Constraint cAcctNo         = MSF270Key.bankAcctNo.equalTo(batchParams.bankAccountCode)
        Constraint cChequeStatus   = MSF270Rec.chequeStatus.equalTo(ProcessTrb28p.MSF270_CHQ_STATUS_31)
        Constraint cPresentedDate1 = MSF270Rec.presentedDate.greaterThanEqualTo(startDate)
        Constraint cPresentedDate2 = MSF270Rec.presentedDate.lessThanEqualTo(endDate)
        Constraint cChequeNo       = MSF270Key.chequeNo.greaterThan(" ")

        def query = new QueryImpl(MSF270Rec.class).and(cBranchCode.and(cAcctNo).
                and(cChequeStatus).and(cPresentedDate1).and(cPresentedDate2).and(cChequeNo))

        edoi.search(query) { MSF270Rec msf270Rec->
            //Get Supplier Information
            MSF200Rec msf200Rec = readSuppplier(msf270Rec.getPmtSupplier())
            if(msf200Rec == null) {
                def errorMsg = "SUPPLIER NOT ON FILE - ABORT !: ${msf270Rec.getPmtSupplier()}"
                info(errorMsg)
                throw new RuntimeException(errorMsg)
            }
            ReportTrb28p rec  = new ReportTrb28p()
            rec.supplierNo    = msf270Rec.getPmtSupplier()
            rec.supplierName  = msf200Rec.getSupplierName()
            rec.chequeNo      = msf270Rec.getPrimaryKey().getChequeNo()
            rec.presentedDate = msf270Rec.getPresentedDate()
            rec.chequeValue   = msf270Rec.getValWrtnFor().doubleValue()
            listOfMSF270.add(rec)
        }
    }

    /**
     * Read supplier based on Supplier Number
     * @param supplierNo Supplier Number
     * @return MSF200Rec
     */
    private MSF200Rec readSuppplier(String supplierNo) {
        info("readSuppplier ${supplierNo}")
        try {
            return edoi.findByPrimaryKey(new MSF200Key(supplierNo: supplierNo))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            return null
        }
        return null
    }

    /**
     * Print the report detail.
     * @param rec report detail to be written
     */
    private void printReportDetail(ReportTrb28p rec) {
        info("printReportDetail")
        if(!rec.presentedDate.equals(savedPresentedDate)) {
            if(!savedPresentedDate?.trim().isEmpty()) {
                printReportDailySubTotal()
            }
            savedPresentedDate = rec.presentedDate
        }
        rec.presentedDate     = convertDateFormat(rec.presentedDate)
        totalChequeAmmount   += rec.chequeValue
        totalDlChequeAmmount += rec.chequeValue
        reportWriter.write(rec.writeReportDetail())
    }

    /**
     * Print the report summary.
     */
    private void printReportDailySubTotal() {
        info("printReportDailySubTotal")
        reportWriter.write( " ".padRight(97) + String.format("%18s", " ").replace(' ', '='))
        reportWriter.write(" ".padRight(49) + String.format("Total value of Cheques Presented on %-8s :  \$  %15s",
                savedPresentedDate, "${currencyFormatter.format(totalDlChequeAmmount)}"))
        reportWriter.write(" ".padRight(97) + String.format("%18s", " ").replace(' ', '='))
        totalDlChequeAmmount = 0
    }
}

/**
 * Run the script
 */
ProcessTrb28p process = new ProcessTrb28p()
process.runBatch(binding)