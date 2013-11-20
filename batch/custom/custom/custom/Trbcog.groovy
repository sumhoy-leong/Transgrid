/*
 @Ventyx 2012
 */
/*
 * This Groovy script will only ever be run as an Ellipse custom BATCH JOB within an Ellipse custom BATCH
 * and will ALWAYS have preceding BATCH JOBS within this custom BATCH,
 * with these preceding batch jobs being RDLS (within the same batch) that are producing the
 * files that will be picked up and used within this TRBCOG Groovy script
 *
 * All of the separate BATCH JOBS within a single BATCH will all run with the same taskuuid
 *
 * The preceding RDL BATCH JOBS will have been requested with a reporting medium of 'P' (print), and a publish type of 'T' (text)
 *
 * This will have resulted with Ellipse 8 putting these files into the EFS (Ellipse File System) directories by the time that this
 * TRBCOG script runs, with the same taskuuid as this TRBCOG script is currently running with.
 *
 * Therefore this script needs to use various "EFS" type commands to pick up its input files for processing;
 *
 */
package com.mincom.ellipse.script.custom;

import java.util.LinkedHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import groovy.io.FileType;
/*
 * Import the "EFS" Ellipse File System Package
 */
import com.mincom.ellipse.efs.*;

public class ParamsTRBCOG{
    //List of Input Parameters used in testcases
    /*
     * The only input parameter will have a value of
     * "POE" (Purchase Order email) OR
     * "POF" (Purchase Order fax)  OR
     * "QQE" (Request For Quote email) OR
     * "QQF" (Request For Quote fax)  OR
     * "REM" (Remittance Advice email and fax)
     *
     * and this next input parameter will NOT be coming from an MSF080 report request, but relates to when running this script via
     * TestCase.groovy or runCustom - NOT when passing the parameter from the custom_batch table.
     */
    String paramInputFileType;
    String paramInputFileName;
    String paramInputOutputDir;

}

public class EntityRecord {
    String userCommand = ""
    String mediumType = ""
    StringBuilder recordContents = new StringBuilder()
}

public class ProcessTRBCOG extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private static final String ENTITY_POE = "POE"
    private static final String ENTITY_POF = "POF"
    private static final String ENTITY_QQE = "QQE"
    private static final String ENTITY_QQF = "QQF"
    private static final String ENTITY_REM = "REM"
    private static final String GONOR_CMD = "*/GO /NOR"
    private static final String USER_CMD = "*/USER="
    private static final String MEDIUM_CMD = "*/DE=M"
    private static final String EQUAL_SIGN = "="
    private static final String PREFIX_PO = "PO"
    private static final String PREFIX_RFQ = "RFQ"
    private static final String PREFIX_REM = "RA"
    private static final String MEDIUM_EMAIL = "M"
    private static final String MEDIUM_FAX = "F"
    private static final String SUFFIX_EMAIL = "email"
    private static final String SUFFIX_FAX = "fax"
    private static final String PUBLISH_TYPE = "txt"
    private static final String SEPARATOR_HYPEN = "-"
    private static final String END_OF_LINES = "\r\n"

    private version = 10;
    private ParamsTRBCOG batchParams;
    private boolean validRecord;

    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTRBCOG())

        //PrintRequest Parameters
        info("paramInputFileType: " + batchParams.paramInputFileType)
        info("paramInputFileName: " + batchParams.paramInputFileName)
        info("paramInputOutputDir: "+ batchParams.paramInputOutputDir)
        info("taskUUId: " + getTaskUUID())
        info("uuid: "+ getUUID())
        try {
            processBatch();

        } finally {
            printBatchReport();
        }
    }

    private void processBatch(){
        info("processBatch");

        // Read Input File
        String S_UUID = "\\\$\\{UUID\\}"
        String hasAsterisk = "*"
        String inputFile = batchParams.paramInputFileName.replaceAll(S_UUID, getUUID())
        validRecord = false

        if (inputFile.contains(hasAsterisk)){
            inputFile = findFiles(inputFile)
        }else{
            processFiles(inputFile)
        }

    }

    private void findFiles(String inputFile){
        Boolean fileNotFound = true
        info ("findFiles:" + inputFile)
        inputFile = inputFile.replaceAll("\\*", ".*.")
        // assuming it only has one, if it found more than one then it will only select the first one
        File workDir = new File(env.getWorkDir().toString())
        String [] partFile = inputFile.split("\\*")

        workDir.eachFileMatch(~/${inputFile}/){
            processFiles(it.name)
            fileNotFound = false

        }

        if (fileNotFound){
            info (" No File Found within the criteria.")
        }
    }

    private void processFiles(String inputFile){
        validRecord = false
        List <EntityRecord> entityRec =  readFile(inputFile)
        if (validRecord){
            constructOutputFile (entityRec)
        }
    }

    private List readFile(String inputFileName) {
        info ("Opening : " + env.getWorkDir().toString() + "/"+inputFileName)
        File inputFile = new File(env.getWorkDir().toString() + "/"+inputFileName)
        StringBuilder sb = null
        String userCommand = ""
        String mediumType = ""
        List <EntityRecord> entityRecords= new ArrayList <EntityRecord> ()
        EntityRecord entityRecord = new EntityRecord()

        inputFile.eachLine {String eachLine ->


            if (eachLine.trim().equals(GONOR_CMD)){
                validRecord = true
                //when we find another GONOR_CMD saved the entity along with the text
                if (sb != null){
                    entityRecords.add(safeEntiyRecord(userCommand, mediumType, sb))
                }
                userCommand = ""
                sb = new StringBuilder()
                entityRecord = new EntityRecord()

            }

            //find the entity
            if (eachLine.trim().contains(USER_CMD)){
                userCommand = eachLine.substring(eachLine.lastIndexOf(EQUAL_SIGN)+1, eachLine.length())
            }

            //find the medium
            if (eachLine.trim().contains(MEDIUM_CMD)){
                mediumType = eachLine.substring(eachLine.lastIndexOf(EQUAL_SIGN)+2, eachLine.length())
            }

            //Start writing if we found a valid record.
            if (validRecord){
                sb.append(eachLine.replaceAll("[\u0000-\u001f]","") + END_OF_LINES)
            }

        }

        //process the last record
        if (sb != null){
            entityRecords.add(safeEntiyRecord(userCommand, mediumType, sb))
        }

        return entityRecords
    }

    private EntityRecord safeEntiyRecord(String userCommand, String mediumType, StringBuilder sb) {
        EntityRecord entityRecord = new EntityRecord()

        if (sb != null){

            if (userCommand == ""){
                info ("Could not find the /USER Command")
                assert false  //abort
            }

            if (mediumType == ""){
                info ("Could not find the /DE=M Command")
                assert false //abort
            }

            entityRecord.userCommand = userCommand.trim()
            entityRecord.mediumType = mediumType.trim()
            entityRecord.recordContents = sb
        }
        return entityRecord
    }

    private void constructOutputFile ( List <EntityRecord> entityRecords){

        String prefixFileName = ""
        String outputFileName = ""
        String suffixFileName = ""

        if (batchParams.paramInputFileType.equals(ENTITY_POE) || batchParams.paramInputFileType.equals(ENTITY_POF)){
            prefixFileName = PREFIX_PO
        }

        if (batchParams.paramInputFileType.equals(ENTITY_QQE) || batchParams.paramInputFileType.equals(ENTITY_QQF)){
            prefixFileName = PREFIX_RFQ
        }

        if (batchParams.paramInputFileType.equals(ENTITY_REM)){
            prefixFileName = PREFIX_REM
        }

        if (prefixFileName == ""){
            info ("Invalid Input File Type: ${batchParams.paramInputFileType}")
            assert false
        }

        entityRecords.each {EntityRecord theRecord ->
            if (theRecord.mediumType.equals(MEDIUM_EMAIL)){
                suffixFileName = SEPARATOR_HYPEN + SUFFIX_EMAIL + "." + PUBLISH_TYPE
            }
            if (theRecord.mediumType.equals(MEDIUM_FAX)){
                suffixFileName = SEPARATOR_HYPEN + SUFFIX_FAX + "." + PUBLISH_TYPE
            }

            outputFileName = batchParams.paramInputOutputDir +prefixFileName+SEPARATOR_HYPEN+theRecord.userCommand+suffixFileName
            createOutputFile(outputFileName,theRecord.recordContents)
        }

    }

    private void createOutputFile(String fileName,StringBuilder contents){
        info ("Writing to : " + fileName)
        File outputFileName = new  File(fileName)
        if (outputFileName.exists()){
            outputFileName.delete()
        }
        outputFileName.write(contents.toString())
    }
    //additional method - start from here.

    private void printBatchReport(){
        info("...Finish")
        //print batch report
    }
}

/*run script*/
ProcessTRBCOG process = new ProcessTRBCOG();
process.runBatch(binding);

