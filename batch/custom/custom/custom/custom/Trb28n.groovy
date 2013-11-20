/**
 * @Ventyx 2012
 * Conversion from ecb28n.cbl
 *
 * This program creates the Daily Presented Cheques report.
 */
package com.mincom.ellipse.script.custom;

import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.ellipse.edoi.ejb.msf200.MSF200Key;
import com.mincom.ellipse.edoi.ejb.msf200.MSF200Rec;
import com.mincom.ellipse.edoi.ejb.msf260.MSF260Key;
import com.mincom.ellipse.edoi.ejb.msf260.MSF260Rec;
import com.mincom.ellipse.edoi.ejb.msf26a.MSF26AKey;
import com.mincom.ellipse.edoi.ejb.msf26a.MSF26ARec;
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Key;
import com.mincom.ellipse.edoi.ejb.msf270.MSF270Rec;
import com.mincom.ellipse.edoi.ejb.msf280.MSF280Key;
import com.mincom.ellipse.edoi.ejb.msf280.MSF280Rec;
import com.mincom.ellipse.edoi.ejb.msf282.MSF282Key;
import com.mincom.ellipse.edoi.ejb.msf282.MSF282Rec;
import com.mincom.ellipse.edoi.ejb.msfx21.MSFX21Key;
import com.mincom.ellipse.edoi.ejb.msfx21.MSFX21Rec;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;

/**
 * Request Parameters for Trb28n. <br>
 * <li><code>presentedDate</code> : Date (Format YYYYMMDD)</li>
 * <li><code>bankDistrict</code> : Bank District Code</li>
 * <li><code>bankAccountCode</code> : Bank Account Code</li>
 * <li><code>bankBranchCode</code> : Bank Branch Code</li>
 */
public class ParamsTrb28n {
    String presentedDate
    String bankDistrict
    String bankAccountCode
    String bankBranchCode
}

/**
 * Report content for Trb28n.
 */
public class ChequeDetail {
    /**
     * Decimal formatter, parse numeric value to "9,999,999.99".
     */
    static DecimalFormat currencyFormatter
    static {
        currencyFormatter = new DecimalFormat("#,###,###,###.##")
        currencyFormatter.setMinimumFractionDigits(2)
    }

    String supplierNo= " ", supplierName= " ", orderNo= " ", invoiceNo= " ", datePaid= " ", cheqNo = " "
    double cheqAmmount = 0, invoiceAmmount = 0

    /**
     * Return the report detail at the first line.
     * @return report detail at the first line
     */
    public String writeDetail() {
        return String.format("  %-6s   %-6s   %-32s   %-6s   %-20s %15s  %-8s %15s",
        cheqNo?.length() > 6 ? cheqNo?.substring(0,6) : cheqNo,
        supplierNo?.length() > 6 ? supplierNo?.substring(0,6) : supplierNo,
        supplierName?.length() > 32 ? supplierName?.substring(0,32) : supplierName,
        orderNo?.length() > 6 ? orderNo?.substring(0,6) : orderNo,
        invoiceNo?.length() > 20 ? invoiceNo?.substring(0,20) : invoiceNo,
        invoiceAmmount != 0 ? currencyFormatter.format(invoiceAmmount) : " ",
        datePaid?.length() > 8 ? datePaid?.substring(0,8) : datePaid,
        cheqAmmount != 0 ? currencyFormatter.format(cheqAmmount) : " ")
    }
}

/**
 * Main Process of Trb28n.
 */
public class ProcessTrb28n extends SuperBatch {

    /*
     * Constants
     */
    private static final String MSF270_CHQ_STATUS     = "31"
    private static final String REPORT_NAME           = "TRB28N"
    private static final String MSF26A_INV_ITM_NO_001 = "001"
    private static final String MSF280_00             = "00"

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private int version = 4
    /*
     * Variables
     */
    private def reportWriter
    private String invoiceRef, handleCde = " "
    private ParamsTrb28n batchParams
    private ChequeDetail record
    private double totalChqAmmount
    private int recCount
    private boolean aborted = false

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);

        //Get request parameters
        batchParams = params.fill(new ParamsTrb28n())
        info("presentedDate   : " + batchParams.presentedDate)
        info("bankDistrict    : " + batchParams.bankDistrict)
        info("bankAccountCode : " + batchParams.bankAccountCode)
        info("bankBranchCode  : " + batchParams.bankBranchCode)
        try {
            processBatch()
        } catch(Exception e) {
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB28N ERROR: ", e)
            info("processBatch failed - ${e.getMessage()}")
            aborted = true
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
        browseCheques()
    }

    /**
     * Initialize the report writer and other variables.
     */
    private void initialize() {
        info("initialize")
        recCount = 0
        totalChqAmmount = 0
        //Initialize report
        reportWriter = report.open(REPORT_NAME)
        reportWriter.write("DAILY PRESENTED CHEQUES/EFT REPORT".center(132))
        reportWriter.writeLine(132,"-")
        reportWriter.write("")
        reportWriter.write(String.format("Presented Cheques/EFT for %8s",
                convertDateFormat(batchParams.presentedDate)).center(132))
        reportWriter.write("")
        reportWriter.writeLine(132,"-")
        reportWriter.write("  Cheque   Supplier Supplier                           "+
                "Order    Invoice                    Invoice    Date Paid       Cheque")
        reportWriter.write("  Number   Number   Name                               "+
                "Number   Number                     Amount                      Amount")
        reportWriter.writeLine(132,"-")
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        if(!aborted) {
            if(recCount == 0) {
                reportWriter.write("")
                reportWriter.write("NO PRESENTED CHEQUES/EFT FOUND".center(132))
            }
            reportWriter.write(String.format("%105s %18s", " ", "===================="))
            reportWriter.write(String.format("%67s Total of all Presented Cheques/EFT :  \$ %18s",
                    " ", ChequeDetail.currencyFormatter.format(totalChqAmmount)))
            reportWriter.write(String.format("%105s %18s", " ", "===================="))
            reportWriter.write("")
            reportWriter.write("")
            reportWriter.write("            The total of this report has been verified "+
                    "against the debits of the TransGrid Bank Statement for the same date")
            reportWriter.write("            and with the exception of bank fees or other "+
                    "transactions as highlighted in the Bank Statement are the same.")
            reportWriter.write("")
            reportWriter.write("")
            reportWriter.write("            _________________________          "+
                    "_________________________          _______________")
            reportWriter.write("            Name of Officer                    "+
                    "Signature                          Date")
        }
        reportWriter.close()
    }

    /**
     * Browse the cheques based on bank branch code, bank account code, presented date and status.
     */
    private void browseCheques() {
        info("browseCheques")
        Constraint cBranchCode    = MSF270Key.branchCode.equalTo(batchParams.bankBranchCode)
        Constraint cAccountCode   = MSF270Key.bankAcctNo.equalTo(batchParams.bankAccountCode)
        Constraint cChequeNo      = MSF270Key.chequeNo.greaterThanEqualTo(" ")
        Constraint cChequeStatus  = MSF270Rec.chequeStatus.equalTo(MSF270_CHQ_STATUS)
        Constraint cPresentedDate = MSF270Rec.presentedDate.equalTo(batchParams.presentedDate)

        def query = new QueryImpl(MSF270Rec.class).
                and(cBranchCode).
                and(cAccountCode).
                and(cChequeStatus).
                and(cPresentedDate).
                and(cChequeNo)
        edoi.search(query) { MSF270Rec msf270Rec->
            processChequeMaster(msf270Rec)
        }
    }

    /**
     * Process the Cheque Master record.
     * @param msf270Rec a Cheque Maste record (<code>MSF270Rec</code>) to be proceed
     */
    private void processChequeMaster(MSF270Rec msf270Rec) {
        info("processChequeMaster")
        record             = new ChequeDetail()
        record.supplierNo  = msf270Rec.getPmtSupplier()
        record.cheqNo      = msf270Rec.getPrimaryKey().getChequeNo()
        record.cheqAmmount = msf270Rec.getValWrtnFor()
        totalChqAmmount   += msf270Rec.getValWrtnFor()
        //Get Supplier Information
        MSF200Rec msf200Rec = readSupplier(msf270Rec.getPmtSupplier())
        if(msf200Rec == null) {
            record.supplierName = "**SUPPLIER NO LONGER ON FILE**"
            info("SUPPLIER NOT ON FILE!")
            info("MSF270-PMT-SUPPLIER ${msf270Rec.getPmtSupplier()}")
            info("MSF270-CHEQUE-NO-9  ${msf270Rec.getChequeRunNo()}")
        }
        else {
            record.supplierName = msf200Rec.getSupplierName()
            //Convert presented date from YYYYMMDD into DD-MM-YY
            record.datePaid    = convertDateFormat(msf270Rec.getWrittenDate())
            invoiceRef         = msf270Rec.getInvoiceRef()
            handleCde          = msf270Rec.getHandleCde()

            MSF280Rec msf280Rec
            if((msf270Rec.getChequeRunNo() as int) != 0) {
                msf280Rec = readPaymentInformation(msf270Rec.getChequeRunNo())
                if(msf280Rec == null) {
                    throw new RuntimeException("ABORT! - CHEQUE NOT FOUND ON MSF280 - "+
                    "Cheque No ${msf270Rec.getPrimaryKey().getChequeNo()}")
                }
            }
            processChequeDetails(msf270Rec, msf280Rec)
        }
    }

    /**
     * Read payment information based on cheque run no.
     * @param chequeRunNo cheque run no
     * @return payment details.
     */
    private MSF280Rec readPaymentInformation(String chequeRunNo) {
        info("readPaymentInformation")
        try {
            MSF280Key pkMSF280 = new MSF280Key()
            pkMSF280.acctDstrct  = batchParams.bankDistrict
            pkMSF280.branchCode  = batchParams.bankBranchCode
            pkMSF280.bankAcctNo  = batchParams.bankAccountCode
            pkMSF280.chequeRunNo = chequeRunNo
            pkMSF280.invDstrct   = " "
            pkMSF280.ordSupplierNo = " "
            pkMSF280.invNo         = " "
            pkMSF280.mcprtRunNo    = " "
            pkMSF280.recType       = MSF280_00
            pkMSF280.seqNoMsf280   = MSF280_00
            return edoi.findByPrimaryKey(pkMSF280)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF280Rec ${chequeRunNo} not found!")
            return null
        }
    }

    /**
     * Read Supplier information based on supplier no.
     * @param suppNo supplier no
     * @return Supplier information
     */
    private MSF200Rec readSupplier(String suppNo) {
        info("readSupplier")
        try {
            MSF200Key pkMSF200 = new MSF200Key()
            pkMSF200.setSupplierNo(suppNo)
            return edoi.findByPrimaryKey(pkMSF200)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF200Rec ${suppNo} not found!")
            return null
        }
    }

    /**
     * Process cheque details based on Cheque Master record and Payment Run Cheque/EFT.
     * @param msf270Rec a Cheque Master record (<code>MSF270Rec</code>) to be proceed
     * @param msf280Rec a Payment Run Cheque/EFT record (<code>MSF280Rec</code>) to be proceed
     */
    private void processChequeDetails(MSF270Rec msf270Rec, MSF280Rec msf280Rec) {
        info("processChequeDetails")
        boolean onePrinted = false
        if((msf270Rec.chequeRunNo as int) == 0) {
            /*
             * As per W00060837 
             * Need to get the invoice no, order no. and invoice
             * amount via the cheque/invoice cross-reference file
             */
            record.orderNo = " "
            record.invoiceNo = " "
            getChequeCrossReference(msf270Rec, msf280Rec)
            reportWriter.write(record.writeDetail())
            reportWriter.write("")
            recCount++
        } else {
            Constraint cAccDstrct  = MSF282Key.acctDstrct.equalTo(msf280Rec.getPrimaryKey().getAcctDstrct())
            Constraint cBranchCode = MSF282Key.branchCode.equalTo(msf280Rec.getPrimaryKey().getBranchCode())
            Constraint cBankAccNo  = MSF282Key.bankAcctNo.equalTo(msf280Rec.getPrimaryKey().getBankAcctNo())
            Constraint cChqRunNo   = MSF282Key.chequeRunNo.equalTo(msf280Rec.getPrimaryKey().getChequeRunNo())
            Constraint cHandleCode = MSF282Key.handleCde.equalTo(handleCde)
            Constraint cSuppToPay  = MSF282Key.suppToPay.equalTo(msf270Rec.getPmtSupplier())

            def query = new QueryImpl(MSF282Rec.class).
                    and(cAccDstrct).
                    and(cBranchCode).
                    and(cBankAccNo).
                    and(cChqRunNo).
                    and(cHandleCode).
                    and(cSuppToPay).
                    and(MSF282Key.invDstrct.greaterThanEqualTo(" ")).
                    and(MSF282Key.ordSupplierNo.greaterThanEqualTo(" ")).
                    and(MSF282Key.invNo.greaterThanEqualTo(" "))

            MSF282Rec msf828LastRec
            edoi.search(query) {MSF282Rec msf282Rec->
                msf828LastRec = msf282Rec

                //Check if separate cheques(invoice-ref of msf270 is not spaces)
                if( invoiceRef?.trim() &&
                !invoiceRef.equals(msf280Rec.getPrimaryKey().getInvNo()))  {
                    //skipped
                } else {
                    record.invoiceAmmount = msf282Rec.getInvAmount() -
                            msf282Rec.getSdAmount() - 
                            msf282Rec.getPpAmount()
                    record.invoiceNo      = readInvoiceNo(
                            msf282Rec.getPrimaryKey().getInvDstrct(),
                            msf282Rec.getPrimaryKey().getSuppToPay(),
                            msf282Rec.getPrimaryKey().getInvNo())

                    //the following statements replaced those deactivated above
                    if(!msf282Rec.getPoNo()?.trim()) {
                        MSF26ARec msf26ARec = readInvoiceLine(
                                msf282Rec.getPrimaryKey().getInvDstrct(),
                                msf282Rec.getPrimaryKey().getSuppToPay(),
                                msf282Rec.getPrimaryKey().getInvNo())
                        if(msf26ARec) {
                            record.orderNo = msf26ARec.getPoNo()
                        }
                    } else {
                        record.orderNo = msf282Rec.getPoNo()
                    }
                    reportWriter.write(record.writeDetail())
                    recCount++
                    record = new ChequeDetail()
                    onePrinted = true
                }
            }

            if(msf828LastRec) {
                if(onePrinted) {
                    reportWriter.write("")
                } else {
                    /*
                     * As per W00060837 
                     * Need to get the invoice no, order no. and invoice
                     * amount via the cheque/invoice cross-reference file
                     */
                    record.orderNo = " "
                    record.invoiceNo = " "
                    getChequeCrossReference(msf270Rec, msf280Rec)
                    if(!record.invoiceNo?.trim()) {
                        if(msf828LastRec.getExtInvNo()?.trim()) {
                            record.invoiceNo = msf828LastRec.getExtInvNo()
                            record.invoiceAmmount = tempInvAmmount
                        } else {
                            record.invoiceAmmount = 0
                        }
                    }
                    reportWriter.write(record.writeDetail())
                    reportWriter.write("")
                    recCount++
                }
            }
        }
    }

    /**
     * Read invoice line detail based on the payment detail.
     * @param invDstct
     * @param suppToPay
     * @param invNo
     * @return invoice line detail
     */
    private MSF26ARec readInvoiceLine(String invDstct, String suppToPay, String invNo) {
        info("readInvoiceLine")
        try {
            MSF26AKey pkMSF26A  = new MSF26AKey()
            pkMSF26A.dstrctCode = invDstct
            pkMSF26A.supplierNo = suppToPay
            pkMSF26A.invNo      = invNo
            pkMSF26A.invItemNo  = MSF26A_INV_ITM_NO_001
            return edoi.findByPrimaryKey(pkMSF26A)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF26ARec ${invNo} not found!")
            return null
        }
    }

    /**
     * Get invoice number based on Payments AP  record.
     * @param invDstct
     * @param suppToPay
     * @param invNo
     * @return invoice number
     */
    private String readInvoiceNo(String invDstct, String suppToPay, String invNo) {
        info("readInvoiceNo")
        String invNoReturn = " "
        MSF260Rec msf260Rec
        try {
            MSF260Key pkMSF260  = new MSF260Key()
            pkMSF260.dstrctCode = invDstct
            pkMSF260.supplierNo = suppToPay
            pkMSF260.invNo      = invNo
            msf260Rec = edoi.findByPrimaryKey(pkMSF260)

            if(msf260Rec.getExtInvNo()?.trim()) {
                invNoReturn = msf260Rec.getExtInvNo()
            } else {
                invNoReturn = msf260Rec.getPrimaryKey().getInvNo()
            }

        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            invNoReturn = " "
            info("MSF260Rec ${invNo} not found!")
        }
        return invNoReturn
    }

    /**
     * Get cheque cross reference number based Cheque Master record and Payment Run Cheque/EFT.
     * @param msf270Rec a Cheque Master record (<code>MSF270Rec</code>) to be proceed
     * @param msf280Rec a Payment Run Cheque/EFT record (<code>MSF280Rec</code>) to be proceed
     */
    private void getChequeCrossReference(MSF270Rec msf270Rec, MSF280Rec msf280Rec) {
        info("getChequeCrossReference")
        //Cheque No./Invoice Cross-Ref file
        Constraint cBranchCode = MSFX21Key.branchCode.equalTo(msf280Rec != null ?
                msf280Rec.getPrimaryKey().getBranchCode() : batchParams.bankBranchCode)
        Constraint cBankAccountNo = MSFX21Key.bankAcctNo.equalTo(msf280Rec != null ?
                msf280Rec.getPrimaryKey().getBankAcctNo() : batchParams.bankAccountCode)
        Constraint cChqNo = MSFX21Key.chequeNo.equalTo(msf270Rec.getPrimaryKey().getChequeNo())

        def query = new QueryImpl(MSFX21Rec.class).
                and(cBranchCode).
                and(cBankAccountNo).
                and(cChqNo)

        MSFX21Rec msfX21Rec = edoi.firstRow(query)
        if(msfX21Rec) {
            //get the invoice master file
            MSF260Rec msf260Rec = readInvoiceMasterDetail(
                    msfX21Rec.getPrimaryKey().getDstrctCode(),
                    msf270Rec.getPmtSupplier(),
                    msfX21Rec.getPrimaryKey().getInvNo())

            if(msf260Rec) {
                record.invoiceNo      = msf260Rec.getExtInvNo()
                if(msf260Rec.getLocInvAmd() == 0) {
                    if(msf260Rec.getPpOffsetAmt() == 0) {
                        record.invoiceAmmount = msf260Rec.getLocInvOrig()
                    } else {
                        record.invoiceAmmount = msf260Rec.getLocInvAmd()
                    }
                } else {
                    record.invoiceAmmount = msf260Rec.getLocInvAmd()
                }
            } else {
                record.invoiceNo      = msfX21Rec.getPrimaryKey().getInvNo()
                record.invoiceAmmount = msf270Rec.getValWrtnLoc()
            }

            //Access invoice line item file to get PO NO
            MSF26ARec msf26ARec = readInvoiceLine(msfX21Rec.getPrimaryKey().getDstrctCode(),
                    msf270Rec.getPmtSupplier(),
                    msfX21Rec.getPrimaryKey().getInvNo())
            if(msf26ARec) {
                record.orderNo = msf26ARec.poNo
            }
        }
    }

    /**
     * Read invoice master detail.
     * @param dstrctCode district code
     * @param supplierNo supplier number
     * @param invNo invoice number
     * @return invoice master 
     */
    private MSF260Rec readInvoiceMasterDetail(String dstrctCode, String supplierNo, String invNo) {
        info("readInvoiceMasterDetail")
        try {
            MSF260Key pkMSF260  = new MSF260Key()
            pkMSF260.dstrctCode = dstrctCode
            pkMSF260.supplierNo = supplierNo
            pkMSF260.invNo      = invNo
            return edoi.findByPrimaryKey(pkMSF260)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            return null
        }
    }

    /**
     * Convert a String into a specified date format "YY-MM-DD"
     * @param dateS string of a date
     * @return specified date format "YY-MM-DD"
     */
    private String convertDateFormat(String dateS) {
        info("convertDateFormat")
        dateS = dateS.padLeft(8).replace(" ", "0")
        def formattedString = dateS.substring(6) + "-" + dateS.substring(4,6) + "-" + dateS.substring(2,4)
        return formattedString
    }
}

/**
 * Run the script
 */

ProcessTrb28n process = new ProcessTrb28n()
process.runBatch(binding)
