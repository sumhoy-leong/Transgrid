/**
 * Script           : Trbcpy
 * Title            : Copy file utility
 * Objective        :
 * This batch utility is used to copy a source file to a target file.
 * The source and destination filenames must be delimited by a comma. By default, this batch utility uses the Ellipse
 * instance work directory for the source and target files, if no path is defined.
 * Action Parameter is not mandatory, it can be filled with MOVE, when this parameter filled with MOVE, source filename
 * will be deleted after the proccess. 
 *
 * Request
 * entry/ MSF081    : N/A
 *
 * Custom_batch.xml : Required.
 * Usage :
 * <Batch Name="transgrid.TRBCPY">
 *   <TaskList ExecuteMode="Sequential" Success="All">
 *     <RunBatchJob Name="TRBCPY" ParameterLocation="Property">
 *       <Property Name="Parameters" Value="{input file}.$(Custom.Date)},{output file.$(Custom.DateTime)},{action}"/>
 *     </RunBatchJob>
 *   </TaskList>
 * </Batch>
 *
 * e.g.
 *  <Batch Name="transgrid.TRR66R">
 *    <TaskList ExecuteMode="Sequential" Success="All">
 *      <RunBatchJob Name="TRR66R" ParameterLocation="MSF080">
 *        <Property Name="ReportOutput.0" Value="TRR66RA"/>
 *      </RunBatchJob>
 *      <RunBatchJob Name="TRBCPY" ParameterLocation="Property"> 
 *        <Property Name="Parameters" Value="TRT66R.$(Parent.UUID),/winshare/Finance/new/TRT66R.$(Custom.DateTime),MOVE"/> 
 *      </RunBatchJob> 
 *    </TaskList>
 *  </Batch>
 *  
 *
 * input file     : Input file name.
 *                  This parameter can be combined with token, for example .?WFT345.$(Custom.Date)
 * output file    : output file name.
 *                  This parameter can be combined with token, for example WFT345.$(Custom.Date)
 * action         : Currently the valid value is only MOVE. This optional parameter will delete input file.
 *
 */

package com.mincom.ellipse.script.custom;

import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.util.*;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import groovy.lang.Binding;


public class ParamsTrbcpy{
    //List of Input Parameters
    String paramSource;
    String paramDestination;
    String paramAction;
}

public class ProcessTrbcpy extends SuperBatch {

    private final version = 4;
    private ParamsTrbcpy batchParams
    def p = []
    def source
    def destination
    def action

    public void runBatch(Binding b){
        info("runBatch Version : " + version);

        init(b);

        /* 
         *  Populates input from TestCase.groovy script
         */
        batchParams = params.fill(new ParamsTrbcpy())

        if (request.getProperty("Parameters") == null) {
            info("TestCase.groovy or runCustom")
            source = batchParams.paramSource
            destination = batchParams.paramDestination
            action = batchParams.paramAction
        } else {
            /*
             * Populates parameters from Custom_Batch.xml    
             *  Prioritize to pick up parameters from Custom_batch.xml
             *  then TestCase.groovy script 
             */
            p = request.getProperty("Parameters").tokenize(",")
            if (p.size() > 0) {
                if (p.size() == 3) {
                    info("Custom_batch.xml, " + p.size() + " parameters.")
                    source = p[0]
                    destination = p[1]
                    action = p[2]
                } else {
                    info("Custom_batch.xml, " + p.size() + " parameters.")
                    source = p[0]
                    destination = p[1]
                }
            } else {
                info("TestCase.groovy or runCustom")
                source = batchParams.paramSource
                destination = batchParams.paramDestination
            }
        }
        processBatch(source, destination, action)
    }


    /**
     * Parameters Source file path to be copied and destination of file path.
     * @param <code> source </code> <code> destination </code>
     * 
     */
    private void Copy(String source, String destination, String action) {
        info("Copy ..")

        def tsrc = detectToken(source)
        def tdst = detectToken(destination)

        Boolean bvalidationError = false 
        File fsrc = new File(tsrc)
        File fdst = new File(tdst)
        info("Parameter source directory/file : " + fsrc) 
        info("Parameter destination directory/file : " + fdst) 

        /* 
         * Check if SOURCE directory exist and subsequently SOURCE file exists.
         * If SOURCE directory does not exist, defaults it to $WORK directory.
         */
         
        info(" ") 
        info("Checking source directory/file: " + fsrc) 
        
        if (fsrc.isDirectory() ) {
            info("This is a directory, file name also required.")
            bvalidationError = true            
        } else { 
            if (fsrc.getParent() == null) { 
                info("Directory is null, will be set to the default value (work directory).")
                fsrc = new File(env.getWorkDir(), tsrc)
                info("Checking source directory/file: " + fsrc)
            } 
            
            if (!fsrc.getParentFile().isDirectory()) {
                info("Directory, ${fsrc.getParent()}, is not found." )
                bvalidationError = true
            } else {
                info("Directory, ${fsrc.getParent()}, is found.")
                if (!fsrc.exists()) {
                    info("Source file not found, " + fsrc)
                    bvalidationError = true
                } else {
                    if (!fsrc.canRead() ) {
                        info("Source file cannot be read, " + fsrc)
                        bvalidationError = true
                    }
                }
            }  
            
        }

        /* 
         * Check if DESTINATION directory exist and subsequently DESTINATION file exists.
         * If DESTINATION directory does not exist, defaults it to $WORK directory.
         */
        boolean fdstCanWrite = true
        boolean fdstCanRead  = true
        boolean fdstCanExec  = true
        boolean fdstDirCanWrite = true
        boolean fdstDirCanRead  = true
        boolean fdstDirCanExec  = true
         
        info(" ") 
        info("Checking destination directory/file: " + fdst) 
        
        if (fdst.isDirectory() ) {
            info("This is a directory, destination file name also required.")
            bvalidationError = true            
        } else { 
            if (fdst.getParent() == null) { 
                info("Directory is null, will be set to the default value (work directory).")
                fdst = new File(env.getWorkDir(), tdst)
                info("Checking destination directory/file: " + fdst)
            } 
            
            if (!fdst.getParentFile().isDirectory()) {
                info("Directory, ${fdst.getParent()}, is not found." )
                bvalidationError = true
            } else {
                info("Directory, ${fdst.getParent()}, is found.")
                File fdstDir = new File(fdst.getParent())
                fdstDirCanWrite = fdstDir.canWrite()
                fdstDirCanRead  = fdstDir.canRead()
                fdstDirCanExec  = fdstDir.canExecute()
                info("Can WRITE Destination Dir: " + fdstDirCanWrite)
                info("Can READ  Destination Dir: " + fdstDirCanRead)
                info("Can EXEC  Destination Dir: " + fdstDirCanExec)
                if (fdst.exists()) {
                    fdstCanWrite = fdst.canWrite()
                    fdstCanRead  = fdst.canRead()
                    fdstCanExec  = fdst.canExecute()
                    info("Can WRITE Destination File: " + fdstCanWrite)
                    info("Can READ  Destination File: " + fdstCanRead)
                    info("Can EXEC  Destination File: " + fdstCanExec)
                    if(fdstCanWrite) {
                        info("Destination file exists, will be overwritten.")
                    } else {
                        info("Destination file exists, is Read Only.")
                        bvalidationError = true
                    }
                }
            }  
            
        }

        /* 
         * Do the Copy.
         */
        if (bvalidationError) {  
            errorMessage("Cannot Perform Copy.")
        } else {		
            if(fdst.exists()) {
                fdst.delete()
                fdst.createNewFile()
                fsrc.withInputStream { is -> fdst << is }
                if (action.equals("MOVE")) {
                    fsrc.delete()
                    info("Move Successful.")
                } else {
                    info("Copy Successful.")
                }
                info("Copied from: " + fsrc)
                info("Overwritten: " + fdst)
            } else {
                fdst.createNewFile()
                fsrc.withInputStream { is -> fdst << is }
                if (action.equals("MOVE")) {
                    fsrc.delete()
                    info("Move Successful.")
                } else {
                    info("Copy Successful.")
                }
                info("Copied from: " + fsrc)
                info("Created    : " + fdst)
            }
        }
    }


    /**
     * Parameter String of file path that could hold tokens to be translated
     * @param <code> parm </parm>
     */
    private String detectToken(def parm) {
        info("detectToken ..")

        def pattern = '$'
        def closePattern = ')'
        def file = ''
        def sdate
        int startIndex = parm.indexOf(pattern)
        int endIndex   = parm.indexOf(closePattern, startIndex)
        if (startIndex> 0 && endIndex > 0) {
            endIndex += 1
            info(parm.substring(startIndex, endIndex))
            switch (parm.substring(startIndex, endIndex)) {
                case '$(Custom.Date)':
                    sdate = new SimpleDateFormat("yyyyMMdd").format(new Date())
                    file = parm.substring(0,startIndex) + sdate + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Custom.DateTime)':
                    sdate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
                    file = parm.substring(0,startIndex) + sdate + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Custom.Env)':
                    file = parm.substring(0,startIndex) + env.getWorkDir() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Parent.UUID)':
                    file = parm.substring(0,startIndex) + request.request.getParent().getTaskUuid() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(UUID)':
                    file = parm.substring(0,startIndex) + request.request.getUUID() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Name)':
                    file = parm.substring(0,startIndex) + request.request.getName() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Name.Full)':
                    file = parm.substring(0,startIndex) + request.request.getFullName() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Name.Request)':
                    file = parm.substring(0,startIndex) + request.request.getRequestName() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Owner)':
                    file = parm.substring(0,startIndex) + request.request.getOwner() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Status)':
                    file = parm.substring(0,startIndex) + request.request.getStatus() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Title)':
                    file = parm.substring(0,startIndex) + request.request.getTitle() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Time.Create)':
                    file = parm.substring(0,startIndex) + request.request.getCreateTime() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Time.LastChange)':
                    file = parm.substring(0,startIndex) + request.request.getUpdateTime() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Output.Type)':
                    file = parm.substring(0,startIndex) + request.request.getOutputDevice() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Output.Destination)':
                    file = parm.substring(0,startIndex) + request.request.getDestination() + parm.substring(endIndex)
                    info(file)
                    break
                case '$(Output.Copies)':
                    file = parm.substring(0,startIndex) + request.request.getCopies() + parm.substring(endIndex)
                    info(file)
                    break
                /*  FOR FUTURE DEVELOPMENT
                case '$(Output.Title)':
                    file = parm.substring(0,parm.indexOf(pattern)) + request.getUUID() + parm.substring(endIndex) 
                    info(file)
                    break
                case '$(Output.Description)':
                    file = parm.substring(0,parm.indexOf(pattern)) + request.getUUID() + parm.substring(endIndex) 
                    info(file)
                    break
                */
                default:
                    info("No more tokens to substitute")
            }
        }
        if (!file.isEmpty()) {
            parm = file
        }
        return parm
    }

	/**
     * Print Error Message
     **/
    private void errorMessage(String sErrorMsg){
        info("*** ERROR *** " + sErrorMsg)
        assert false,sErrorMsg
    }


    /**
     * Execute the copy
     * @param parameters
     */
    private void processBatch(String source, String destination, String action) {
        info("processBatch")

        Copy(source, destination, action)
    }
}


/*run script*/
ProcessTrbcpy process = new ProcessTrbcpy();
process.runBatch(binding);

