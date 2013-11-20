/**
 * @Ventyx 2012
 * Conversion from ecb28l.cbl
 *
 * This program creates the Daily Cash Book report.
 * 
 * Revision History
 * *******************
 * Date         Name        Ver   Desc
 * 19/11/2013   LWH         5     SC0000004400439 TRAN1_SC0000004400439_TRB28L Daily Cash Book Report 
 *                                Ventyx WO #26077
 *                                Additional validation for record written and cancelled on the same day.
 *                                Report needs to be modified to allow cheque to be generated in both Computer Cheques and Cancelled Cheques.
 *
 */
package com.mincom.ellipse.script.custom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import com.mincom.ellipse.edoi.ejb.msf200.*;
import com.mincom.ellipse.edoi.ejb.msf260.*;
import com.mincom.ellipse.edoi.ejb.msf26a.*;
import com.mincom.ellipse.edoi.ejb.msf270.*;
import com.mincom.ellipse.edoi.ejb.msf282.*;
import com.mincom.ellipse.edoi.ejb.msfx21.*;

/**
 * Request Parameters for Trb28l. <br>
 * <li><code>monthEndDate</code> : Date (Format DDMMYY)</li>
 * <li><code>bankDistrict</code> : Bank District Code</li>
 * <li><code>bankAccountCode</code> : Bank Account Code</li>
 * <li><code>bankBranchCode</code> : Bank Branch Code</li>
 */
public class ParamsTrb28l{
    //List of Input Parameters
    String paramMonthEndDate;
    String paramBankDistrict;
    String paramBankAccntCode;
    String paramBankBranchCode;
}

/**
 * Main Process of Trb28l.
 */
public class ProcessTrb28l extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     * 
     */
    private version = 5;
    private ParamsTrb28l batchParams;

    /**
     * Report line content.
     */

    private class Trs28laReportLine implements Comparable<Trs28laReportLine>{
        private String categoryIndicator;
        private String chqNo;
        private String chqRunNo;
        private String supplierNo;
        private String supplierName;
        private String orderNo;
        private String invoiceNo;
        private BigDecimal invoiceAmount;
        private BigDecimal chequeAmount;
        public Trs28laReportLine(String categoryIndicator, String chqNo,
        String chqRunNo, String supplierNo, String supplierName,
        String orderNo, String invoiceNo, BigDecimal invoiceAmount,
        BigDecimal chequeAmount) {
            super();
            this.categoryIndicator = categoryIndicator;
            this.chqNo = chqNo;
            this.chqRunNo = chqRunNo;
            this.supplierNo = supplierNo;
            this.supplierName = supplierName;
            this.orderNo = orderNo;
            this.invoiceNo = invoiceNo;
            this.invoiceAmount = invoiceAmount;
            this.chequeAmount = chequeAmount;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder()
            sb.append(categoryIndicator)
            sb.append("-")
            sb.append(chqNo)
            sb.append("-")
            sb.append(chqRunNo)
            sb.append("-")
            sb.append(supplierNo)
            sb.append("-")
            sb.append(supplierName)
            sb.append("-")
            sb.append(orderNo)
            sb.append("-")
            sb.append(invoiceNo)
            sb.append("-")
            sb.append(invoiceAmount)
            sb.append("-")
            sb.append(chequeAmount)
            return sb.toString()
        }
        /**
         * Return category indicator
         * @return <code>categoryIndicator</code>
         */
        public String getCategoryIndicator() {
            return categoryIndicator;
        }
        /**
         * Set category indicator
         * @param <code>categoryIndicator</code>
         */
        public String setCategoryIndicator(String categoryIndicator) {
            this.categoryIndicator = categoryIndicator;
        }
        /**
         * Return cheque no
         * @return <code>chqNo</code>
         */
        public String getChqNo() {
            return chqNo;
        }
        /**
         * Set cheque no
         * @param <code>chqNo</code>
         */
        public void setChqNo(String chqNo) {
            this.chqNo = chqNo;
        }
        /**
         * Return cheque run no
         * @return <code>chqRunNo</code>
         */
        public String getChqRunNo() {
            return chqRunNo;
        }
        /**
         * Set cheque run no
         * @param <code>chqRunNo</code>
         */
        public void setChqRunNo(String chqRunNo) {
            this.chqRunNo = chqRunNo;
        }
        /**
         * Return supplier no
         * @return <code>supplierNo</code>
         */
        public String getSupplierNo() {
            return supplierNo;
        }
        /**
         * Set supplier no
         * @param <code>supplierNo</code>
         */
        public void setSupplierNo(String supplierNo) {
            this.supplierNo = supplierNo;
        }
        /**
         * Return supplier name
         * @return <code>supplierName</code>
         */
        public String getSupplierName() {
            return supplierName;
        }
        /**
         * Set supplier name
         * @param <code>supplierName</code>
         */
        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }
        /**
         * Return order no
         * @return <code>orderNo</code>
         */
        public String getOrderNo() {
            return orderNo;
        }
        /**
         * Set order no
         * @param <code>orderNo</code>
         */
        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }
        /**
         * Return invoice no
         * @return <code>invoiceNo</code>
         */
        public String getInvoiceNo() {
            return invoiceNo;
        }
        /**
         * Set invoice no
         * @param <code>invoiceNo</code>
         */
        public void setInvoiceNo(String invoiceNo) {
            this.invoiceNo = invoiceNo;
        }
        /**
         * Return invoice amount
         * @return <code>invoiceAmount</code>
         */
        public BigDecimal getInvoiceAmount() {
            return invoiceAmount;
        }
        /**
         * Set invoice amount
         * @param <code>invoiceAmount</code>
         */
        public void setInvoiceAmount(BigDecimal invoiceAmount) {
            this.invoiceAmount = invoiceAmount;
        }
        /**
         * Return cheque amount
         * @return <code>chequeAmount</code>
         */
        public BigDecimal getChequeAmount() {
            return chequeAmount;
        }
        /**
         * Set cheque amount
         * @param <code>chequeAmount</code>
         */
        public void setChequeAmount(BigDecimal chequeAmount) {
            this.chequeAmount = chequeAmount;
        }
        /**
         * Compare one trs28l's line with another line
         * @return the value of the <code>compareTo</code>
         */
        public int compareTo(Trs28laReportLine otherReportLine){
            if (!categoryIndicator.equals(otherReportLine.getCategoryIndicator())){
                return categoryIndicator.compareTo(otherReportLine.getCategoryIndicator())
            }
            if (!chqNo.equals(otherReportLine.getChqNo())){
                return chqNo.compareTo(otherReportLine.getChqNo())
            }
            if (!chqRunNo.equals(otherReportLine.getChqRunNo())){
                return chqRunNo.compareTo(otherReportLine.getChqRunNo())
            }
            return 0
        }
    }

    private def reportA
    private Trs28laReportLine reportLine
    private ArrayList arrayOfReportLine = new ArrayList()
    private DecimalFormat decFormatter = new DecimalFormat("###,###,###,##0.00")
    private int totalNoInvoices = 0
    private BigDecimal grandTotalAmt = 0
    private boolean error = false
    private String monthEndDate
    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTrb28l())

        //PrintRequest Parameters
        info("MonthEndDate   : " + batchParams.paramMonthEndDate)
        info("BankDistrict   : " + batchParams.paramBankDistrict)
        info("BankAccntCode  : " + batchParams.paramBankAccntCode)
        info("BankBranchCode : " + batchParams.paramBankBranchCode)

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
        //write process
        initialize()
        error = process()
    }

    //additional method - start from here.
    /**
     * Initialize the report writer and other variables
     */
    private void initialize(){
        info ("initialize")
        //convert DDMMYY to CCYYYMMDD
        batchParams.paramMonthEndDate = batchParams.paramMonthEndDate?.trim() ?
                batchParams.paramMonthEndDate.padRight(6) : "000000"

        if (batchParams.paramMonthEndDate.trim().substring(4).toInteger() < 50){
            monthEndDate = "20" + batchParams.paramMonthEndDate.trim().substring(4)
        }
        else{
            monthEndDate = "19" + batchParams.paramMonthEndDate.trim().substring(4)
        }
        monthEndDate = monthEndDate + batchParams.paramMonthEndDate.trim().substring(2,4) +
                batchParams.paramMonthEndDate.trim().substring(0,2)

        reportA = report.open("TRB28LA")
        writeReportHeader()
    }

    /**
     * Write the report header into the report
     */
    private void writeReportHeader(){
        info ("writeReportHeader")
        reportA.write("MMIS PAYMENTS - DAILY CASH BOOK".center(132))

        StringBuilder sb = new StringBuilder()
        sb.append("List of all Computer Cheques, Manual Cheques, EFT Payments, Manual EFT Payments and Cheque cancellations processed on ")
        sb.append(monthEndDate.substring(6,8))
        sb.append("-")
        sb.append(monthEndDate.substring(4,6))
        sb.append("-")
        sb.append(monthEndDate.substring(2,4))
        reportA.write(sb.toString().center(132))

        reportA.writeLine(132,"-")
        reportA.write("Cheque    Run     Supplier   Supplier                            Order     Invoice                 Invoice           Cheque")
        reportA.write("Number    No.     Number     Name                                Number    Number                  Amount            Amount")
        reportA.writeLine(132,"-")
    }

    /**
     * Process the main file MSF270
     * @return <code>abort</code>
     */
    private boolean process(){
        info("process")
        String invDstrct
        String handleCde
        String chqRunNo
        String supplierNo
        String supplierName
        String invoiceRef
        String chequeType
        String chequeNo
        BigDecimal chequeAmount
        boolean abort = false

        def query = new QueryImpl(MSF270Rec.class).
                and(MSF270Key.branchCode.equalTo(batchParams.paramBankBranchCode)).
                and(MSF270Key.bankAcctNo.equalTo(batchParams.paramBankAccntCode)).
                and(MSF270Key.chequeNo.greaterThanEqualTo(" ")).
                and(MSF270Rec.chequeStatus.notEqualTo("01")).
                and(MSF270Rec.writtenDate.equalTo(monthEndDate).
                or(MSF270Rec.cancelledDate.equalTo(monthEndDate)))

        edoi.search(query) {MSF270Rec msf270rec ->
            if (!abort){
                chqRunNo   = msf270rec.getChequeRunNo().trim()
                supplierNo = msf270rec.getPmtSupplier().trim()
                invoiceRef = msf270rec.getInvoiceRef().trim()
                handleCde  = msf270rec.getHandleCde().trim()
                chequeType = msf270rec.getChequeType().trim()
                chequeNo   = msf270rec.getPrimaryKey().getChequeNo().trim()

                if (!chequeType.equals("M") && !chequeType.equals(" ") && !chequeType.equals("S")){
                    MSF282Rec msf282Rec = readPayments(chqRunNo, supplierNo)
                    if (msf282Rec){
                        invDstrct = msf282Rec.getPrimaryKey().getInvDstrct()
                    } else{
                        if (!msf270rec.getChequeStatus().equals("31")){
                            info("Account District : " + batchParams.paramBankAccntCode)
                            info("Branch Code      : " + batchParams.paramBankBranchCode)
                            info("Bank Account     : " + batchParams.paramBankAccntCode)
                            info("Cheque Run No    : " + chqRunNo)
                            info("Handle Code      : " + handleCde)
                            info("Supplier         : " + supplierNo)
                            info("Inv Dstrct       : " + invDstrct)
                            info("Cancelled Date   : " + msf270rec.getCancelledDate())
                            info("Written Date     : " + msf270rec.getWrittenDate())
                            info("ABORT!")
                            info("CHEQUE NOT FOUND ON MSF282")
                            info("Cheque No " + chequeNo)
                            abort =  true
                        }
                    }
                }

                if (!chequeType.equals("S")){
                    MSF200Rec msf200rec = readSupplier(supplierNo)
                    if(msf200rec) {
                        supplierName = msf200rec.getSupplierName()
                    } else {
                        info("Cheque No     : " + chequeNo)
                        info("Cheque Run No : " + chqRunNo)
                        info("Supplier      : " + supplierNo)
                        info("SUPPLIER NOT ON FILE - ABORT !")
                        abort = true
                    }
                }

                reportLine = new Trs28laReportLine(" "," "," "," "," "," "," ",0,0)
                reportLine.setChequeAmount(msf270rec.getValWrtnLoc())
                reportLine.setChqNo(chequeNo)
                reportLine.setChqRunNo(chqRunNo)
                reportLine.setSupplierNo(supplierNo)
                reportLine.setSupplierName(supplierName)

                boolean continueSw = true
                if (msf270rec.getCancelledDate().equals(monthEndDate)){
                    reportLine.setCategoryIndicator("4")
                } else{
                    switch(chequeType) {
                        case "E":
                            reportLine.setCategoryIndicator("3")
                            break
                        case "C":
                            reportLine.setCategoryIndicator("1")
                            if (chequeNo.toBigDecimal() > 999999){
                                reportLine.setCategoryIndicator("3")
                            }
                            break
                        case "M":
                            if (chequeNo.toBigDecimal() > 549999 && chequeNo.toBigDecimal() < 560000){
                                reportLine.setCategoryIndicator("5")
                            } else{
                                reportLine.setCategoryIndicator("2")
                            }
                            break
                        default:
                            continueSw = false
                            break
                    }
                }

                if (reportLine.getCategoryIndicator().equals("4")){
                    chequeAmount = reportLine.getChequeAmount() * -1
                    reportLine.setChequeAmount(chequeAmount)
                }

                if (!chequeType.equals("M") && !msf270rec.getChequeStatus().equals("31")){
                    if (msf270rec.getCancelledDate().equals(monthEndDate)){
                        reportLine.setOrderNo(" ")
                        reportLine.setInvoiceNo(" ")
                        arrayOfReportLine.add(reportLine)
                    } else{
                        processChequeItems(handleCde, invoiceRef, chequeNo, invDstrct, supplierNo, msf270rec.getValWrtnLoc())
                    }
                } else{
                    reportLine.setOrderNo(" ")
                    reportLine.setInvoiceNo(" ")
                    reportLine.setInvoiceAmount(0)
                    processChequeItems(handleCde, invoiceRef, chequeNo, invDstrct, supplierNo, msf270rec.getValWrtnLoc())
                }

                if (msf270rec.getCancelledDate().equals(monthEndDate) &&
					msf270rec.getWrittenDate().equals(monthEndDate)){
                    // 20131119 LWH version 5
					// cheque cancelled date same as written date will show in both COMPUTER CHEQUES and CHEQUE CANCELLATIONS
				
					//reportLine.setCategoryIndicator("1")
                    //reportLine.setChequeAmount(msf270rec.getValWrtnLoc())
				
					Trs28laReportLine reportLine2 = new Trs28laReportLine(" "," "," "," "," "," "," ",0,0)
					reportLine2.setCategoryIndicator("1")
					reportLine2.setChequeAmount(msf270rec.getValWrtnLoc())
					reportLine2.setChqNo(chequeNo)
					reportLine2.setChqRunNo(chqRunNo)
					reportLine2.setSupplierNo(supplierNo)
					reportLine2.setSupplierName(supplierName)
					reportLine2.setOrderNo(" ")
					reportLine2.setInvoiceNo(" ")
				
                    arrayOfReportLine.add(reportLine2)
                }
            }
        }
        return abort
    }

    /**
     * Read the supplier based on supplier number.
     * @param supplierNo supplier number
     * @return MSF200Rec
     */
    private MSF200Rec readSupplier(String supplierNo) {
        info("readSupplier")
        try {
            return edoi.findByPrimaryKey(new MSF200Key(supplierNo))
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            return null
        }
    }

    /**
     * Read payments based on the cheque number and supplier number.
     * @param chqRunNo cheque number
     * @param supplierNo supplier number
     * @return MSF282Rec
     */
    private MSF282Rec readPayments(String chqRunNo, String supplierNo) {
        info("readPayments")
        def query = new QueryImpl(MSF282Rec.class).
                and(MSF282Key.acctDstrct.equalTo(batchParams.paramBankDistrict)).
                and(MSF282Key.branchCode.equalTo (batchParams.paramBankBranchCode)).
                and(MSF282Key.bankAcctNo.equalTo (batchParams.paramBankAccntCode)).
                and(MSF282Key.chequeRunNo.equalTo(chqRunNo)).
                and(MSF282Key.handleCde.greaterThanEqualTo(" ")).
                and(MSF282Key.suppToPay.equalTo(supplierNo))

        return (MSF282Rec) edoi.firstRow(query)
    }

    /**
     * Process the cheque items based on the inputted parameters
     * @param handleCde
     * @param invoiceRef
     * @param chequeNo
     * @param invDstrct
     * @param supplierNo
     * @param valWrtnLoc
     */
    private void processChequeItems(String handleCde, String invoiceRef, String chequeNo, String invDstrct, String supplierNo, BigDecimal valWrtnLoc){
        info("processChequeItems")
        int msf282Counter = 0
        String extInvNo   = " "
        String invoiceNo  = " "
        BigDecimal invoiceAmount = 0
        Trs28laReportLine prevLine

        boolean doneOneItem = false
        def query = new QueryImpl(MSF282Rec.class).
                and(MSF282Key.acctDstrct.equalTo(batchParams.paramBankDistrict)).
                and(MSF282Key.branchCode.equalTo (batchParams.paramBankBranchCode)).
                and(MSF282Key.bankAcctNo.equalTo (batchParams.paramBankAccntCode)).
                and(MSF282Key.chequeRunNo.equalTo(reportLine.getChqRunNo())).
                and(MSF282Key.handleCde.equalTo(handleCde)).
                and(MSF282Key.suppToPay.equalTo(supplierNo)).
                and(MSF282Key.invDstrct.greaterThanEqualTo(" ")).
                and(MSF282Key.ordSupplierNo.greaterThanEqualTo(" ")).
                and(MSF282Key.invNo.greaterThanEqualTo(" "))

        edoi.search(query) {MSF282Rec msf282rec ->
            extInvNo = msf282rec.getExtInvNo()
            if(invoiceRef?.trim() && !invoiceRef.equals(extInvNo)) {
                //skipped
            } else {
                invoiceAmount = msf282rec.getInvAmount() -
                        msf282rec.getSdAmount() -
                        msf282rec.getPpAmount()

                if(!msf282rec.getPoNo().trim()){
                    String poNo = getPoNo(msf282rec.getPrimaryKey().getInvDstrct(),
                            msf282rec.getPrimaryKey().getSuppToPay(),
                            msf282rec.getPrimaryKey().getInvNo())
                    reportLine.setOrderNo(poNo)
                } else{
                    reportLine.setOrderNo(msf282rec.getPoNo())
                }

                invoiceNo = getInvoiceNo(msf282rec.getPrimaryKey().getInvDstrct(),
                        msf282rec.getPrimaryKey().getSuppToPay(),
                        msf282rec.getPrimaryKey().getInvNo())

                if (reportLine.getCategoryIndicator().equals("4")){
                    invoiceAmount = invoiceAmount * -1
                } else{
                    totalNoInvoices++
                }

                reportLine.setInvoiceAmount(invoiceAmount)
                if (!invoiceNo.equals(" ")){
                    reportLine.setInvoiceNo(invoiceNo)
                } else{
                    reportLine.setInvoiceNo(msf282rec.getPrimaryKey().getInvNo())
                }
                prevLine = new Trs28laReportLine(reportLine.getCategoryIndicator(),
                        reportLine.getChqNo(),
                        reportLine.getChqRunNo(),
                        reportLine.getSupplierNo(),
                        reportLine.getSupplierName(),
                        reportLine.getOrderNo(),
                        reportLine.getInvoiceNo(),
                        reportLine.getInvoiceAmount(),
                        reportLine.getChequeAmount())

                arrayOfReportLine.add(prevLine)
                doneOneItem = true
                msf282Counter++
            }
        }

        if (msf282Counter == 0){
            if(!doneOneItem) {
                reportLine.setOrderNo(" ")
                reportLine.setInvoiceNo(" ")
                getChqInvCrossRef(chequeNo, invDstrct, supplierNo, valWrtnLoc)
                if (reportLine.getInvoiceNo().equals(" ")){
                    if (!extInvNo.equals(" ")){
                        reportLine.setInvoiceNo(extInvNo)
                        if (reportLine.getCategoryIndicator().equals("4")){
                            invoiceAmount = invoiceAmount * -1
                        }
                        reportLine.setInvoiceAmount(invoiceAmount)
                    }
                }
                arrayOfReportLine.add(reportLine)
            }
        }

    }
    /**
     * Process Cheque invoice cross reference based on the inputted parameters
     * @param chequeNo
     * @param invDstrct
     * @param supplierNo
     * @param valWrtnLoc
     */
    private void getChqInvCrossRef (String chequeNo, String invDstrct, String supplierNo, BigDecimal valWrtnLoc){
        info("getChqInvCrossRef")
        Constraint c1 = MSFX21Key.branchCode.equalTo (batchParams.paramBankBranchCode)
        Constraint c2 = MSFX21Key.bankAcctNo.equalTo (batchParams.paramBankAccntCode)
        Constraint c3 = MSFX21Key.chequeNo.equalTo(chequeNo)

        def query = new QueryImpl(MSFX21Rec.class).and(c1).and(c2).and(c3)

        MSFX21Rec msfx21rec = (MSFX21Rec) edoi.firstRow(query)
        if (msfx21rec){
            readInvoice(invDstrct, supplierNo, msfx21rec.getPrimaryKey().getInvNo(), valWrtnLoc)
        }
    }

    /**
     * Read MSF260 to get specific values
     * @param invDstrct
     * @param supplierNo
     * @param invNo
     * @param valWrtnLoc
     */
    private void readInvoice(String invDstrct, String supplierNo, String invNo, BigDecimal valWrtnLoc){
        info("readInvoice")
        BigDecimal invoiceAmount
        String poNo
        try{
            MSF260Rec msf260rec = edoi.findByPrimaryKey(new MSF260Key(invDstrct, supplierNo, invNo))
            reportLine.setInvoiceNo(msf260rec.getExtInvNo())
            if (msf260rec.getLocInvAmd() == 0){
                if (msf260rec.getPpOffsetAmt() == 0){
                    reportLine.setInvoiceAmount(msf260rec.getLocInvOrig())
                } else{
                    reportLine.setInvoiceAmount(msf260rec.getLocInvAmd())
                }
            } else{
                reportLine.setInvoiceAmount(msf260rec.getLocInvAmd())
            }
            if (reportLine.getCategoryIndicator().equals("4")){
                invoiceAmount = reportLine.getInvoiceAmount() * -1
                reportLine.setInvoiceAmount(invoiceAmount)
            }
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            reportLine.setInvoiceAmount(valWrtnLoc)
            reportLine.setInvoiceNo(invNo)
            if (reportLine.getCategoryIndicator().equals("4")){
                invoiceAmount = reportLine.getInvoiceAmount() * -1
                reportLine.setInvoiceAmount(invoiceAmount)
            }
        }

        poNo = getPoNo(invDstrct, supplierNo, invNo)
        reportLine.setOrderNo(poNo)
    }

    /**
     * Process MSF26A to get PO no
     * @param invDstrct
     * @param suppToPay
     * @param invNo
     * @return PO no if found else spaces
     */
    private String getPoNo(String invDstrct, String suppToPay, String invNo){
        info("getPoNo")
        try{
            MSF26ARec msf26arec = edoi.findByPrimaryKey(new MSF26AKey(invDstrct, suppToPay, invNo, "001"))
            return(msf26arec.getPoNo())
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            return(" ")
        }
    }

    /**
     * Process MSF260 to get invoice no
     * @param invDstrct
     * @param suppToPay
     * @param invNo
     * @return ext invoice no if found else spaces
     */
    private String getInvoiceNo(String invDstrct, String suppToPay, String invNo){
        info("getInvoiceNo")
        try{
            MSF260Rec msf260rec = edoi.findByPrimaryKey(new MSF260Key(invDstrct, suppToPay, invNo))
            if (!msf260rec.getExtInvNo().equals(" ")){
                return msf260rec.getExtInvNo()
            } else{
                return msf260rec.getPrimaryKey().getInvNo()
            }
        } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            return(" ")
        }
    }

    /**
     * Print the batch report
     */
    private void printBatchReport(){
        info("printBatchReport")
        //print batch report
        if(!error){
            Collections.sort(arrayOfReportLine)
            generateReport()
        }
        if(reportA) {
            reportA.close()
        }
    }

    /**
     * Generate report. Write the values to the specific line.
     */
    private void generateReport(){
        info("generateReport")
        int saveIndicator = 1
        int categoryIndicator
        String saveChqNo
        String saveCatInd
        String writeLine
        BigDecimal detChequeAmount
        BigDecimal catTotalAmt = 0

        reportA.write("********************")
        reportA.write("* COMPUTER CHEQUES *")
        reportA.write("********************")

        for(Trs28laReportLine currLine: arrayOfReportLine){
            writeLine = ""
            categoryIndicator = currLine.getCategoryIndicator().toInteger()
            if (categoryIndicator != saveIndicator){
                printCategoryTotal(saveIndicator, catTotalAmt)
                saveIndicator++
                catTotalAmt = 0
                if (saveIndicator < categoryIndicator){
                    printCategoryTotal(saveIndicator, catTotalAmt)
                    saveIndicator++
                    catTotalAmt = 0
                    if (saveIndicator < categoryIndicator){
                        printCategoryTotal(saveIndicator, catTotalAmt)
                        saveIndicator++
                        catTotalAmt = 0
                    }
                }
            }

            if (!currLine.getChqNo().equals(saveChqNo) || !currLine.getCategoryIndicator().equals(saveCatInd)){
                reportA.write(" ")
                writeLine = writeLine + currLine.getChqNo().padRight(10)
                writeLine = writeLine + currLine.getChqRunNo().padRight(8)
                writeLine = writeLine + currLine.getSupplierNo().padRight(11)
                writeLine = writeLine + currLine.getSupplierName().padRight(36)
                detChequeAmount = currLine.getChequeAmount()
                catTotalAmt = catTotalAmt + detChequeAmount
                saveChqNo = currLine.getChqNo()
                saveCatInd = currLine.getCategoryIndicator()
            } else{
                writeLine = writeLine + " ".padRight(65)
                detChequeAmount = 0
            }

            writeLine = writeLine + currLine.getOrderNo().padRight(10)
            writeLine = writeLine + currLine.getInvoiceNo().padRight(24)
            if (!currLine.getInvoiceNo().equals(" ")){
                writeLine = writeLine + (decFormatter.format(currLine.getInvoiceAmount())).padLeft(15) + "   "
            } else{
                writeLine = writeLine + (decFormatter.format(0)).padLeft(15) + "   "
            }
            String cheqAmmount = detChequeAmount != 0.0 ? decFormatter.format(detChequeAmount) : " "
            writeLine = writeLine + (cheqAmmount).padLeft(15)
            reportA.write(writeLine)
        }
        for (int i=saveIndicator; i<=6; i++){
            printCategoryTotal(i, catTotalAmt)
            catTotalAmt = 0
        }

    }

    /**
     * Print the total of a category
     * @param saveIndicator
     * @param catTotalAmt
     */
    private void printCategoryTotal(int saveIndicator, BigDecimal catTotalAmt){
        info("printCategoryTotal")
        switch (saveIndicator){
            case 1: grandTotalAmt = grandTotalAmt + catTotalAmt
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write(" ".padRight(89) + "Computer Cheques total :  \$" + decFormatter.format(catTotalAmt).padLeft(16))
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write("******************")
                reportA.write("* MANUAL CHEQUES *")
                reportA.write("******************")
                break
            case 2: grandTotalAmt = grandTotalAmt + catTotalAmt
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write(" ".padRight(91) + "Manual Cheques total :  \$" + decFormatter.format(catTotalAmt).padLeft(16))
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write("****************")
                reportA.write("* EFT PAYMENTS *")
                reportA.write("****************")
                break
            case 3: grandTotalAmt = grandTotalAmt + catTotalAmt
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write(" ".padRight(93) + "EFT Payments total :  \$" + decFormatter.format(catTotalAmt).padLeft(16))
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write("************************")
                reportA.write("* CHEQUE CANCELLATIONS *")
                reportA.write("************************")
                break
            case 4: grandTotalAmt = grandTotalAmt + catTotalAmt
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write(" ".padRight(88) + "Cancelled Cheques total :  \$" + decFormatter.format(catTotalAmt).padLeft(16))
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write("***********************")
                reportA.write("* MANUAL EFT PAYMENTS *")
                reportA.write("***********************")
                break
            case 5: grandTotalAmt = grandTotalAmt + catTotalAmt
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write(" ".padRight(86) + "Manual EFT Payments total :  \$" + decFormatter.format(catTotalAmt).padLeft(16))
                reportA.write(" ".padRight(115) + "-----------------")
                reportA.write(" ".padRight(114) + "==================")
                reportA.write(" ".padRight(48) + "Total Number of Invoices Paid : " +
                        totalNoInvoices.toString().padRight(4) +
                        " ".padRight(16) + "Grand Total :  \$" +
                        decFormatter.format(grandTotalAmt).padLeft(16))
                reportA.write(" ".padRight(114) + "==================")
                break
        }

    }

}

/*run script*/
ProcessTrb28l process = new ProcessTrb28l();
process.runBatch(binding);