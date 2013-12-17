/**
 * @Ventyx 2012
 * Conversion from trb266.cbl
<<<<<<< HEAD
 *  df;lskdfsf;lksdf;sdlkfsd;lfdf 
=======
 *   
 *  TEST BRANCH
>>>>>>> sc123455
 * This program clones output file from TRR266.rdl and reads the last entries 
 * of standard text in the database based on specified supplier and invoice number 
 * from the input file. Then write those last entries into an output file to be
 * used by TRR265.rdl.
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;

import java.io.File;
import java.io.Writer;

import com.mincom.ellipse.edoi.ejb.msf011.*;
import com.mincom.ellipse.edoi.ejb.msf012.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf100.*;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequestDTO;

/**
 * Iffsdfdsfsdfsdfsdfdsnput and Output file from Trb266. The structure for input and output file is the same.
 */
public class TRT266 {
    /*
     * Record length
     */
    public static final int RECORD_LENGTH = 871

    private String orderNo_FW, poNO_F1, poITemNo_G, purchReq_PREQ, preqItem_PI,
    invNo_ININV, invNo_J, msb265RunNo_C, ataxCode_TX, ataxCode_X, ataxCode_X2,
    invItemNo_K, supplierNo_E, authrsdBy_A, invType_T, dstrctCode_DR,
    loadedDate_LD, dstrctCode_DC, warehouse_WH, invCutDate_CUTDTE, lowRunNo, highRunNo,
    settlmntDiscount_SETDSTCXT, settlmntValue_SETVAL, settlmtValue_ST,
    settlmtDueDate_STDUE, acctNo_ACCTNO_1, allocPc_T1, workOrder_WP1, equipNo_EQUIPNO_1,
    acctNo_ACCTNO_2, allocPc_T2, workOrder_WP2, equipNo_EQUIPNO_2, acctNo_ACCTNO_3,
    allocPc_T3, workOrder_WP3, equipNo_EQUIPNO_3, acctNo_ACCTNO_4, allocPc_T4,
    workOrder_WP4, equipNo_EQUIPNO_4, acctNo_ACCTNO_5, allocPc_T5, workOrder_WP5,
    equipNo_EQUIPNO_5, acctNo_ACCTNO_6, allocPc_T6, workOrder_WP6, equipNo_EQUIPNO_6,
    acctNo_ACCTNO_7, allocPc_T7, workOrder_WP7, equipNo_EQUIPNO_7, suppInvNo_SIN,
    dueDate_DDAT, originInvNo_OIN, frtDocket_M, receivedBy_N, lastModUser_MODUSR,
    accountCode_O, workOrder_W, projectMo_P, equipNo_Q, tro266_1_inv_comm,
    tro266_2_inv_comm, totalCnt_CNT, totalValInd_VAL

    /*
     * Based on the CPY file, below variables type should be a Number. 
     * But since they were never used in a process, change the type to String.
     */
    private String grossPrice_H, invVal_WARNING, invVal_VALINVD, invVal_D,
    gstIncAmmnt_GSTINC, amdVal_L, lastAmmendNo_U, valAppr_I, invVal_GST, invVal_VALINVD1,
    gstAmmnt_GSTSUM, gstTaxAmmnt_GSTV, gstTaxSum_GSTVSUM, ammendQty_R, qtyInvoiced_S

    /**
     * Initialize the record based on the input line
     * @param line input line
     */
    public TRT266(String line) {
        orderNo_FW      = line.substring(0, 9)
        poNO_F1         = line.substring(9, 15)
        poITemNo_G      = line.substring(15, 18)
        grossPrice_H    = line.substring(18, 28)
        purchReq_PREQ   = line.substring(28, 34)
        preqItem_PI     = line.substring(34, 37)
        invNo_ININV     = line.substring(37, 47)
        invNo_J         = line.substring(47, 67)
        msb265RunNo_C   = line.substring(67, 73)
        invVal_WARNING  = line.substring(73, 86)
        invVal_VALINVD  = line.substring(86, 99)
        invVal_D        = line.substring(99, 112)
        ataxCode_TX     = line.substring(112, 116)
        ataxCode_X      = line.substring(116, 118)
        ataxCode_X2     = line.substring(118, 122)
        gstIncAmmnt_GSTINC = line.substring(122, 135)
        amdVal_L        = line.substring(135, 148)
        lastAmmendNo_U  = line.substring(148, 150)
        invItemNo_K     = line.substring(150, 153)
        valAppr_I       = line.substring(153, 166)
        supplierNo_E    = line.substring(166, 172)
        authrsdBy_A     = line.substring(172, 182)
        invVal_GST      = line.substring(182, 195)
        invType_T       = line.substring(195, 196)
        dstrctCode_DR   = line.substring(196, 200)
        invVal_VALINVD1 = line.substring(200, 213)
        loadedDate_LD   = line.substring(213, 221)
        gstAmmnt_GSTSUM = line.substring(221, 234)
        gstTaxAmmnt_GSTV  = line.substring(234, 247)
        gstTaxSum_GSTVSUM = line.substring(247, 260)
        dstrctCode_DC     = line.substring(260, 264)
        warehouse_WH      = line.substring(264, 268)
        invCutDate_CUTDTE = line.substring(268, 276)
        lowRunNo   = line.substring(276, 282)
        highRunNo  = line.substring(282, 288)
        settlmntDiscount_SETDSTCXT = line.substring(288, 310)
        settlmntValue_SETVAL       = line.substring(310, 318)
        settlmtValue_ST      = line.substring(318, 322)
        settlmtDueDate_STDUE = line.substring(322, 330)
        acctNo_ACCTNO_1      = line.substring(330, 340)
        allocPc_T1           = line.substring(340, 346)
        workOrder_WP1        = line.substring(346, 354)
        equipNo_EQUIPNO_1    = line.substring(354, 366)
        acctNo_ACCTNO_2      = line.substring(366, 376)
        allocPc_T2           = line.substring(376, 382)
        workOrder_WP2        = line.substring(382, 390)
        equipNo_EQUIPNO_2    = line.substring(390, 402)
        acctNo_ACCTNO_3      = line.substring(402, 412)
        allocPc_T3           = line.substring(412, 418)
        workOrder_WP3        = line.substring(418, 426)
        equipNo_EQUIPNO_3    = line.substring(426, 438)
        acctNo_ACCTNO_4      = line.substring(438, 448)
        allocPc_T4           = line.substring(448, 454)
        workOrder_WP4        = line.substring(454, 462)
        equipNo_EQUIPNO_4    = line.substring(462, 474)
        acctNo_ACCTNO_5      = line.substring(474, 484)
        allocPc_T5           = line.substring(484, 490)
        workOrder_WP5        = line.substring(490, 498)
        equipNo_EQUIPNO_5    = line.substring(498, 510)
        acctNo_ACCTNO_6      = line.substring(510, 520)
        allocPc_T6           = line.substring(520, 526)
        workOrder_WP6        = line.substring(526, 534)
        equipNo_EQUIPNO_6    = line.substring(534, 546)
        acctNo_ACCTNO_7      = line.substring(546, 556)
        allocPc_T7           = line.substring(556, 562)
        workOrder_WP7        = line.substring(562, 570)
        equipNo_EQUIPNO_7    = line.substring(570, 582)
        suppInvNo_SIN        = line.substring(582, 602)
        dueDate_DDAT         = line.substring(602, 610)
        originInvNo_OIN      = line.substring(610, 620)
        frtDocket_M          = line.substring(620, 626)
        receivedBy_N         = line.substring(626, 636)
        ammendQty_R          = line.substring(636, 649)
        qtyInvoiced_S        = line.substring(649, 662)
        lastModUser_MODUSR   = line.substring(662, 672)
        accountCode_O        = line.substring(672, 696)
        workOrder_W          = line.substring(696, 704)
        projectMo_P          = line.substring(704, 712)
        equipNo_Q            = line.substring(712, 725)
        tro266_1_inv_comm    = line.substring(725, 785)
        tro266_2_inv_comm    = line.substring(785, 845)
        totalCnt_CNT         = line.substring(845, 854)
        totalValInd_VAL      = line.substring(854)
    }

    /**
     * Print the variables from this record into String.
     * @return String from this record
     */
    public String toString() {
        String value = String.format("%-9s%-6s%-3s%10s%-6s%-3s%-10s%-20s%-6s"+
                "%13s%13s%13s%-4s%-2s%-4s%13s%13s%-2s%-3s%13s%-6s%-10s"+
                "%13s%-1s%-4s%13s%-8s%13s%13s%13s%-4s%-4s%-8s%-6s%-6s%-22s%-8s"+
                "%-4s%-8s%-10s%-6s%-8s%-12s%-10s%-6s%-8s%-12s%-10s%-6s%-8s%-12s"+
                "%-10s%-6s%-8s%-12s%-10s%-6s%-8s%-12s%-10s%-6s%-8s%-12s%-10s%-6s%-8s%-12s"+
                "%-20s%-8s%-10s%-6s%-10s%13s%13s%-10s%-24s%-8s%-8s%-13s%-60s%-60s"+
                "%-9s%-17s",
                orderNo_FW, poNO_F1, poITemNo_G, grossPrice_H, purchReq_PREQ,
                preqItem_PI, invNo_ININV, invNo_J, msb265RunNo_C, invVal_WARNING,
                invVal_VALINVD, invVal_D, ataxCode_TX, ataxCode_X, ataxCode_X2,
                gstIncAmmnt_GSTINC, amdVal_L, lastAmmendNo_U, invItemNo_K, valAppr_I,
                supplierNo_E, authrsdBy_A, invVal_GST, invType_T, dstrctCode_DR,
                invVal_VALINVD1, loadedDate_LD, gstAmmnt_GSTSUM, gstTaxAmmnt_GSTV,
                gstTaxSum_GSTVSUM, dstrctCode_DC, warehouse_WH, invCutDate_CUTDTE,
                lowRunNo, highRunNo, settlmntDiscount_SETDSTCXT, settlmntValue_SETVAL,
                settlmtValue_ST, settlmtDueDate_STDUE, acctNo_ACCTNO_1, allocPc_T1,
                workOrder_WP1, equipNo_EQUIPNO_1, acctNo_ACCTNO_2, allocPc_T2,
                workOrder_WP2, equipNo_EQUIPNO_2, acctNo_ACCTNO_3, allocPc_T3,
                workOrder_WP3, equipNo_EQUIPNO_3, acctNo_ACCTNO_4, allocPc_T4,
                workOrder_WP4, equipNo_EQUIPNO_4, acctNo_ACCTNO_5, allocPc_T5,
                workOrder_WP5, equipNo_EQUIPNO_5, acctNo_ACCTNO_6, allocPc_T6,
                workOrder_WP6, equipNo_EQUIPNO_6, acctNo_ACCTNO_7, allocPc_T7,
                workOrder_WP7, equipNo_EQUIPNO_7, suppInvNo_SIN, dueDate_DDAT,
                originInvNo_OIN, frtDocket_M, receivedBy_N, ammendQty_R, qtyInvoiced_S,
                lastModUser_MODUSR, accountCode_O, workOrder_W, projectMo_P, equipNo_Q,
                tro266_1_inv_comm, tro266_2_inv_comm, totalCnt_CNT, totalValInd_VAL)
        return value
    }
}

/**
 * Main process of Trb266.
 */
public class ProcessTrb266 extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private String version = 2

    /*
     * Constants
     */
    private static final String INPUT_FILENAME  = "TRT266"
    private static final String OUTPUT_FILENAME = "TRO266"
    private static final String REPORT_NAME     = "TRB266A"
    private static final String TEXT_CODE_IA    = "IA"
    private static final String DASHED_LINE     = String.format("%132s"," ").replace(' ', '-')
    private static final String STDTEXT_SERVICE = "STDTEXT"
    private static final int SUB_MAX = 599

    /*
     * Variables
     */
    private String workingDir, validationErrorMessage
    private File inputFileTRT266, outputFileTRO266
    private Writer fileWriter
    private def reportWriter
    private ArrayList<String> textLines, errorLines
    private int lineCount

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version : " + version)
        info("TRB266 does not have request parameters.")

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
        if(validateInputFile(inputFileTRT266)) {
            processInputFile()
        }
    }

    /**
     * Print the report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        //write report header
        if(!validationErrorMessage.isEmpty() || !errorLines.isEmpty()) {
            reportWriter  = report.open(REPORT_NAME)
            if(!errorLines.isEmpty()) {
                reportWriter.write("")
                reportWriter.write("${REPORT_NAME} Summary Error Report".center(132))
                reportWriter.write(DASHED_LINE)
                reportWriter.write("Line/Field Ref/Value              Error/Warning Message Column Hdg")
                reportWriter.write(DASHED_LINE)
                errorLines.each { reportWriter.write(it) }
                reportWriter.write("")
            }
            if(!validationErrorMessage.isEmpty()) {
                reportWriter.write("")
                reportWriter.write("Validation Errors:")
                reportWriter.write("------------------")
                reportWriter.write(validationErrorMessage)
                reportWriter.write("")
            }
            reportWriter.close()
        }
        if(fileWriter != null) {
            fileWriter.close()
        }
    }

    /**
     * Initialize the input/output files and other variables
     */
    private void initialize() {
        info("initialize")
        workingDir = env.workDir
        String taskUUID = this.getTaskUUID()
        String inputFilePath  = "${workingDir}/${INPUT_FILENAME}"
        String outputFilePath = "${workingDir}/${OUTPUT_FILENAME}"
        if(taskUUID?.trim()) {
            inputFilePath  = inputFilePath  + "." + taskUUID
            outputFilePath = outputFilePath + "." + taskUUID
        }

        inputFileTRT266  = new File(inputFilePath)
        outputFileTRO266 = new File(outputFilePath)
        //Open the output file
        fileWriter  = new FileWriter(outputFileTRO266)
        textLines   = new ArrayList<String>()
        errorLines  = new ArrayList<String>()
        validationErrorMessage = ""
    }

    /**
     * Validate the input file, it should be exist in the work dir and it is not empty.
     * @param inputFile input file to be validated
     * @return true if input file is valid, false otherwise.
     */
    private boolean validateInputFile(File inputFile) {
        info("validateInputFile ${inputFile.getAbsolutePath()}")
        boolean valid = true
        if(inputFile != null && !inputFile.exists()) {
            valid = false
            validationErrorMessage = "*** ${INPUT_FILENAME} DOES NOT EXIST ***"
            info(validationErrorMessage)
        } else {
            BufferedReader input = new BufferedReader(new FileReader(inputFile))
            if(input != null && input.readLine() == null) {
                valid = false
                validationErrorMessage = "*** NO RECORDS FOUND ON ${INPUT_FILENAME} FILE ***"
                info(validationErrorMessage)
            }
            input.close()
        }
        return valid
    }

    /**
     * Process the input file.
     */
    private void processInputFile() {
        info("processInputFile")
        String textCode, textKey, inv
        int comSize

        lineCount = 1
        /*
         * Use buffering, reading one line at a time
         * FileReader always assumes default encoding is OK!
         */
        BufferedReader input = new BufferedReader(new FileReader(inputFileTRT266))
        try {
            String line = null
            while ((line = input.readLine()) != null){
                if(line.length() < TRT266.RECORD_LENGTH) {
                    line = line.padRight(TRT266.RECORD_LENGTH)
                }
                TRT266 trt266Rec = new TRT266(line)
                textLines.clear()
                comSize      = 0
                textCode     = TEXT_CODE_IA
                textKey      = ""
                inv          = ""

                if(trt266Rec.invNo_ININV.trim().length() > 0) {
                    inv = trt266Rec.invNo_ININV
                } else {
                    if(trt266Rec.invNo_J.trim().length() > 0) {
                        inv = trt266Rec.invNo_J.substring(0, 10)
                    }
                }
                textKey = trt266Rec.dstrctCode_DC + trt266Rec.supplierNo_E +
                        inv + trt266Rec.invItemNo_K

                populateStdTextLines(textCode, textKey)
                comSize = textLines.size()

                if(comSize > 0) {
                    checkMoveInvComm(trt266Rec, comSize)
                }

                //write to output file
                fileWriter.write(trt266Rec.toString())
                fileWriter.write("\n")
                lineCount++
            }
        } catch(IOException ioEx) {
            info("IOException occured during processing input file: ${ioEx.toString()}")
            writeErrorToReport(String.valueOf(lineCount), "IOException ${ioEx.toString()} occured. Process terminated.")
        } catch(Exception e) {
            info("Unknwon exception occured during processing input file: ${e.toString()}")
            writeErrorToReport(String.valueOf(lineCount), "Unknwon exception ${e.toString()} occured. Process terminated.")
        } finally {
            input.close()
        }
    }

    /**
     * Populate the Std Text based on the text code and text key, limit the Std Text size to 600 record.
     * @param textCode standard text's code
     * @param textKey standard text's key
     * @return standard text 
     */
    private List populateStdTextLines(String textCode, String textKey) {
        info("populateStdTextLines ${textCode} ${textKey}")
        int sub = 0
        List<StdTextServiceGetTextReplyDTO> cmStdTextReplyList = new ArrayList<StdTextServiceGetTextReplyDTO>()
        try {
            //restart value
            def restart = ""
            StdTextServiceGetTextReplyCollectionDTO stdTextReplyDTO =
                    service.get(STDTEXT_SERVICE).getText({StdTextServiceGetTextRequestDTO it->
                        it.setStdTextId(textCode + textKey)
                    },100, true, restart)
            restart = stdTextReplyDTO.getCollectionRestartPoint()

            for(StdTextServiceGetTextReplyDTO stdTextDTO : stdTextReplyDTO.getReplyElements()) {
                for(String text : stdTextDTO.getTextLine()) {
                    textLines.add(text)
                    sub++
                    if(sub >= SUB_MAX) {
                        break
                    }
                }
                if(sub >= SUB_MAX) {
                    break
                }
            }
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart.trim().length() > 0 && sub < SUB_MAX) {
                stdTextReplyDTO = service.get(STDTEXT_SERVICE).getText({StdTextServiceGetTextRequestDTO it->
                    it.setStdTextId(textCode + textKey)
                },100, true, restart)
                restart = stdTextReplyDTO.getCollectionRestartPoint()

                for(StdTextServiceGetTextReplyDTO stdTextDTO : stdTextReplyDTO.getReplyElements()) {
                    for(String text : stdTextDTO.getTextLine()) {
                        textLines.add(text)
                        sub++
                        if(sub >= SUB_MAX) {
                            break
                        }
                    }
                    if(sub >= SUB_MAX) {
                        break
                    }
                }
            }
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            writeErrorToReport(String.valueOf(lineCount), "${serviceExc.getMessage()}.")
            info("Error during execute ${STDTEXT_SERVICE}.getText: ${serviceExc.getMessage()}")
        } catch(Exception e) {
            writeErrorToReport(String.valueOf(lineCount), "${e.getMessage()}.")
            info("Unknown error during execute ${STDTEXT_SERVICE}.getText: ${e.getClass().toString()} ${e.getMessage()}")
        }
        return cmStdTextReplyList
    }

    /**
     * Check the Inv Comm value, write it to output record.
     * @param outputRec output record
     * @param comSize the com line size
     */
    private void checkMoveInvComm(TRT266 outputRec, int comSize) {
        info("checkMoveInvComm ${comSize}")
        if(comSize > 1) {
            outputRec.tro266_1_inv_comm = textLines.get(comSize - 2)
            outputRec.tro266_2_inv_comm = textLines.get(comSize - 1)
        } else {
            outputRec.tro266_1_inv_comm = textLines.get(0)
            outputRec.tro266_2_inv_comm = ""
        }
    }

    /**
     * Write error detail into report.
     * @param line error line
     * @param message error message
     */
    private void writeErrorToReport(String line, String message) {
        info("writeErrorToReport")
        errorLines.add(String.format("%-30s    %-90s", line,
                message.length() > 90 ? message.substring(0, 90) : message))
    }
}

/**
 * Run the script
 */
ProcessTrb266 process = new ProcessTrb266()
process.runBatch(binding)
