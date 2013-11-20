/**
 * @Ventyx 2012
 *
 * This program will process MSB83M report by adding Supplier Name
 * The result of this batch is TRB83M
 *
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import com.mincom.batch.script.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import com.mincom.ellipse.eroi.linkage.mssdat.*;
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.supplier.SupplierServiceReadRequiredAttributesDTO;
import com.mincom.ellipse.edoi.ejb.msf120.*;
import com.mincom.ellipse.edoi.ejb.msf200.*;
import com.mincom.ellipse.edoi.ejb.msf20a.*;

/**
 * No Request Parameters for Trb83M.
 */
public class ParamsTrb83m{
    String payGroup
    String runType
    String bsbNumber
    String bankNumber
    String payCalcInd
}

/**
 * Main process of Trb83M.
 */
public class ProcessTrb83m extends SuperBatch {
    /**
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 2
    private ParamsTrb83m batchParams
    private BufferedWriter reportB

    /**
     * Constants
     */
    private static final int MAX_PAGE_LINE = 59
    private static final int HEADER_LINE   = 11
    private static final String OUTPUT_FILENAME = "TRB83MA"
    private static final String INPUT_FILENAME  = "MSB83MA"

    File outputFile
    int pageLine = 1
    int pageNo   = 1
    String[] headerReport = new String[HEADER_LINE]

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){
        info("runBatch")

        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);

        //PrintRequest Parameters
        info("TRB83M Has No Request Parameters ")

        def workingDir = env.workDir
        String taskUUID = this.getTaskUUID()
        String outputFilePath = "${workingDir}/${OUTPUT_FILENAME}"

        if(taskUUID?.trim()) {
            outputFilePath = outputFilePath + "." + taskUUID
        }

        outputFile = new File(outputFilePath)
        FileWriter fstream = new FileWriter(outputFile)
        reportB = new BufferedWriter(fstream)

        try {
            processBatch()
        } finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch")
        boolean containsNewLineChar

        File reportFile = browseMSB83MAInputFile()
        info("File to be processed ${reportFile}")
        if (!reportFile) {
            info("File ${INPUT_FILENAME} does not exist")
        } else {
            reportFile.eachLine { line ->
                //Check whether the line contain new line character
                if (((pageNo == 0) || (pageNo == 1)) && line ==~ /^\f.*+/) {
                    addPageNo(1)
                }

                //Save header report to array.
                if (pageNo == 2   &&
                pageLine >= 1 &&
                pageLine <= 11 ) {
                    headerReport[pageLine - 1] = line
                }

                if(!line.padRight(6).substring(0, 6).trim().equals("") &&
                !line.padRight(6).substring(0, 6).trim().equals("SUPPLY") &&
                !line.padRight(6).substring(0, 6).trim().equals("ID") &&
                line.padRight(14).substring(6, 8).trim().equals("")) {

                    //Print line 1 of TRB83MA detail which is contain Supplier Name
                    String supplierName = getSupplierName(line)
                    reportB.writeLine(line.substring(0, 8) + supplierName)
                    addPageLine(1)

                    //Print line 2 of TRB83MA detail
                    reportB.writeLine("        " + line.substring(8, line.length()))
                    addPageLine(1)

                } else {
                    containsNewLineChar = false

                    if(line ==~ /^\f.*+/){
                        containsNewLineChar = true
                    }

                    //Print Header Report in Page 1 and 2
                    //Print line 3 of TRB83MA detail
                    if (pageNo <= 2 ||
                    (!containsNewLineChar &&
                    !line.padRight(60).substring(0, 60).trim().equals("------------------------------------------------------------") &&
                    !line.padRight(9).substring(0, 9).trim().equals("Pay Group") &&
                    !line.padRight(6).substring(0, 6).trim().equals("Req.By") &&
                    !line.padRight(6).substring(0, 6).trim().equals("Run on") &&
                    !line.padRight(6).substring(0, 6).trim().equals("SUPPLY") &&
                    !line.padRight(6).substring(0, 6).trim().equals("ID") &&
                    !line.padRight(81).substring(0, 81).trim().equals("PAYROLL DEDUCTIONS INVOICE UPDATE") &&
                    line.length() != 0)) {
                        reportB.writeLine(line)
                        addPageLine(1)
                    }
                }
            }
        }
    }

    /**
     * Browse the MSB83MA produced from MSB83M.
     * @return MSB83MA
     */
    private File browseMSB83MAInputFile() {
        info("browseMSB83MAInputFile")

        String checkedFilePattern = "${INPUT_FILENAME}.*"
        if(taskUUID?.trim()) {
            checkedFilePattern = checkedFilePattern + "." + taskUUID
        }
        info("Pattern to be checked ${checkedFilePattern}")
        File foundFile = null
        env.workDir.eachFileMatch(~/${checkedFilePattern}/) { f ->
            if (f.exists()){
                foundFile = f
            }
        }

        return foundFile
    }

    /**
     * Get Supplier Name from parameter using Supplier service
     */
    private String getSupplierName(String newLine){
        info("getSupplierName")

        String supplierNo = newLine.substring(0, 8).trim()
        String supplierName

        SupplierServiceReadRequiredAttributesDTO suppReadReq = new SupplierServiceReadRequiredAttributesDTO()
        suppReadReq.returnSupplierName = true

        try {
            SupplierServiceReadReplyDTO supplierReadReply = service.get('Supplier').read({ SupplierServiceReadRequestDTO it ->
                it.requiredAttributes = suppReadReq
                it.setSupplierNo(supplierNo)
            }, false)
            if(supplierReadReply != null) {
                supplierName   = supplierReadReply.getSupplierName()
            } else {
                supplierName    = 'No Supplier Name Found'
            }
        } catch(Exception e) {
            supplierName    = 'No Supplier Name Found'
            info("Error at read Supplier . ${e.getMessage()}")
        }
        return supplierName
    }

    /**
     * Calculate report page, if report more than 2 pages,
     * this function will manage report header print
     */
    private void addPageLine(int newLine){
        info("addPageLine")
        if ((pageLine + newLine) <= MAX_PAGE_LINE) {
            pageLine = pageLine + newLine
        } else {
            pageLine = 1
            addPageNo(1)
            if (pageNo >= 3){
                printPageHeader()
            }
        }
    }

    /**
     * Calculate report page line
     */
    private void addPageNo(int newPage){
        info("addPageNo")
        pageLine = 1
        pageNo = pageNo + newPage

    }

    /**
     * Print report header
     */
    private void printPageHeader(){
        info("printPageHeader")
        for (i in 0..10) {
            String headerLine
            if (i == 1) {
                headerLine = headerReport[i].substring(0, 115) + pageNo.toString().padLeft(17)
            } else {
                headerLine = headerReport[i]
            }
            reportB.writeLine(headerLine)
            addPageLine(1)
        }
    }

    /**
     * Close report result
     */
    private void printBatchReport(){
        info("printBatchReport")
        reportB.close()
        if (taskUUID?.trim()) {
            request.request.CURRENT.get().addOutput(outputFile,"text/plain", "TRB83MA");
        }
    }
}

/**
 * Run the script
 */
ProcessTrb83m process = new ProcessTrb83m()
process.runBatch(binding)