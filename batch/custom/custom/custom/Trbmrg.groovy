/**
 * Script           : Trbmrg
 * Title            : Merge files into designated output file.
 * Objective        : Retrieve all files from sub directories and
 *                    merge them into an output file.
 * Request
 * entry/ MSF081    : N/A
 *
 * Custom_batch.xml : Required
 *
 * Usage :
 *  <Batch Name="transgrid.TRBMRG">
 *   <TaskList ExecuteMode="Sequential" Success="All">
 *     <RunBatchJob Name="TRBMRG" ParameterLocation="Property">
 *       <Property Name="Parameters" Value="{base directory},{file pattern},{output file},{action},{output folder}"/>
 *     </RunBatchJob>
 *   </TaskList>
 * </Batch>
 *
 * e.g.
 *  <Batch Name="transgrid.TRR66R">
 *    <TaskList ExecuteMode="Sequential" Success="All">
 *      <RunBatchJob Name="TRBCPY" ParameterLocation="Property"> 
 *        <Property Name="Parameters" Value="/mincom/data/conf/git-repos/customer-software/data/ToEllipse,.?WFT345.*?\\.txt,TRT345.txt,MOVE,/mincom/data/conf/git-repos/customer-software/data/done"/>
 *      </RunBatchJob>
 *      <RunBatchJob Name="TRR66R" ParameterLocation="MSF080">
 *        <Property Name="ReportOutput.0" Value="TRR66RA"/>
 *      </RunBatchJob>
 *    </TaskList>
 *  </Batch>
 *
 * Source directory : is the source directory where all the files need to be merged resides.
 *                    If the Source directory is a relative pathname, the base directory will default to the “work” directory
 * File pattern     : Regular Expression Pattern of file name to be merged, excluding the ~/ / sign.
 *                    This parameter can be combined with token, for example .*WFT345.$(Custom.Date)
 * Output File      : Result of merged matched files. If the File does not include a pathname,
 *                    the pathname for the Target File will default to the “work” directory
 *                    This parameter can be combined with token, for example .*WFT345.$(Custom.Date)
 * Action           : Is optional. If it is used, it represents the Action
 *                    – i.e. what to do with any of the matched files
 * Output folder    : Is optional. If it is used, it represents where the Action should target
 *                    – e.g. if the Action is MOVE – then it represents where the match files will be moved to.
 *
 */

package com.mincom.ellipse.script.custom;

import java.io.File;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import groovy.lang.Binding;

import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFKey;
import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFRec;


public class ParamsTrbmrg{
    //List of Input Parameters
    String paramBaseDirectory;
    String paramFilePattern;
    String paramOutputFile;
    String paramAction;
    String paramDestination;
}
  

public class ProcessTrbmrg extends SuperBatch {

    private final version = 4;
    private ParamsTrbmrg batchParams
    def p = []
    def baseDirectory
    def filePattern
    def outputFile
    def action
    def destination

    public void runBatch(Binding b){
        info("runBatch Version : " + version)

        init(b)
        /* 
        *  Populates input from TestCase.groovy script
        */
        batchParams = params.fill(new ParamsTrbmrg())

        if (request.getProperty("Parameters")== null) { 
            info("TestCase.groovy or runCustom")
            baseDirectory = batchParams.paramBaseDirectory
            filePattern = batchParams.paramFilePattern
            outputFile = batchParams.paramOutputFile
            action = batchParams.paramAction
            destination = batchParams.paramDestination
        } else {
            p = request.getProperty("Parameters").tokenize(",")

            info("Custom_batch.xml, "+ p.size()+"  parameters.")
            baseDirectory = p[0]
            filePattern = p[1]
            outputFile = p[2]

            if (p.size() > 3) {
                action = p[3]
                destination = p[4]
            } else
            if (p.size() == 3) {
                action = ""
                destination = ""
            }
        }
        processBatch(baseDirectory, filePattern, outputFile, action, destination)
    }

  /**
   * Parameters
   * 
   * baseDirectory  : Root directory where all the files need to be merged resides.
   * filePattern    : Pattern of file name to be merged.
   * outputFile     : output file name.
   * action         : Next action to the file(s) need to be merged upon merge completion.
   * destination    : Destination folder for file(s) need to be merged.
   * @param <code> baseDirectory </code> <code> filePattern </code> <code> outputFile </code>
   *      <code> action </code> <code> destination </code>
   */
    private void Merge(String baseDirectory, String filePattern, String outputFile, String action, String destination)  {
        info("Merge ..")
        
        String checkedFilePattern = detectToken(filePattern)
        String checkedOutf        = detectToken(outputFile)
        boolean outfCanWrite = true
        boolean outfCanRead  = true
        boolean outfCanExec  = true
        boolean outfDirCanWrite = true
        boolean outfDirCanRead  = true
        boolean outfDirCanExec  = true
        boolean fileMatches = false

        Boolean bvalidationError = false

        File dir  = new File(baseDirectory)
        info("Validate Source Directory: " + dir.toString())
        if (!dir.isDirectory() ) {
            dir = new File(env.getWorkDir(), dir.toString().trim())
            info("Defaulted source directory to: " + dir)
            if (!dir.isDirectory()) {
                info("ERROR, Source Directory ${dir} is not found." )
                bvalidationError = true
            } else {
                info("Source Directory ${dir} is found.")
            }
        } else {
            info("Source Directory ${dir} is found.")
        }

        File outf = new File(checkedOutf)
        info("Validate output file: " + outf.toString())
        
        if (outf.isDirectory() ) {
            info("Error, This is a directory, output file name also required.")
            bvalidationError = true            
        } else { 
            if (outf.getParent() == null) {
                outf = new File(env.getWorkDir(), outf.toString().trim())
                info("Defaulted output file to: " + outf)
            }
            if (!outf.getParentFile().isDirectory()) {
                if(outf.getParentFile().mkdirs()) {
                    info("Output Directory ${outf.getParent()} is created.")
                } else {
                    info("Error, Output Directory ${outf.getParent()} is not found.")
                    bvalidationError = true
                }
            } else {
                info("Directory ${outf.getParent()} is found.")
                File outfDir = new File(outf.getParent())
                outfDirCanWrite = outfDir.canWrite()
                outfDirCanRead  = outfDir.canRead()
                outfDirCanExec  = outfDir.canExecute()
                info("Can WRITE Destination Dir: " + outfDirCanWrite)
                info("Can READ  Destination Dir: " + outfDirCanRead)
                info("Can EXEC  Destination Dir: " + outfDirCanExec)
                if (outf.exists()) {
                    outfCanWrite = outf.canWrite()
                    outfCanRead  = outf.canRead()
                    outfCanExec  = outf.canExecute()
                    info("Can WRITE Destination File: " + outfCanWrite)
                    info("Can READ  Destination File: " + outfCanRead)
                    info("Can EXEC  Destination File: " + outfCanExec)
                    if(outfCanWrite) {
                        info("Destination file exists, will be overwritten.")
                    } else {
                        info("Error, Destination file exists, is Read Only.")
                        bvalidationError = true
                    }
                }
            }  
        }

        File dest = new File(destination)

        if (!action.isEmpty()) {
            info("Validate Action Destination Directory:" + dest.toString())
            if (!dest.isDirectory() ) {
                if(dest.mkdirs()) {
                    info("Destination Directory ${dest} is created.")
                } else {
                    dest = new File(env.getWorkDir(), dest.toString().trim())
                    info("Defaulted destination directory to : " + dest)
                    if (!dest.isDirectory() ) {
                        info("Error, Destination Directory ${outf.getParent()} is not found.")
                        bvalidationError = true
                    }
                }
            } else {
                info("Destination Directory ${dest} is found.")
            }
        }

        info("Source Base Directory : " + dir)
        info("File Pattern : " + checkedFilePattern)
        info("Output File  : " + outf )
        info("Action       : " + action)
        info("Destination Directory : " + dest)

        Map<String, String> actionFileList = new HashMap<String, String>()
        
        // If output file exist, delete file
        if (outf.canRead()) { outf.delete() }

        // Find filename matches in source base directory
        // and merge them into output file, outf.
        if (bvalidationError) {  
            errorMessage("Error. Cannot Perform Merge.")
        } else  {
            info("Check match files in: ${dir}")
            dir.eachFileMatch(~/${checkedFilePattern}/) { f ->
                if (f.isFile() && (!f.toString().equals(outf.toString()))){
                    info("Matched file found in source dir: ${f}")
                    fileMatches = true
                    f.withInputStream { is -> outf << is }
                    if(!isEndedByNewLine(f)) {
                        outf.withWriterAppend {
                            it.newLine()
                        }
                    }

                    if (!action.isEmpty()) {
                        switch (action) {
                            case 'MOVE':
                                actionFileList.put(f.toString(), dest.toString() + File.separator + f.getName())
                                break
                        }
                    }
                }
            }
            if (!fileMatches) {info("No file matches in ${dir}")}
            fileMatches = false

            //loop in sub directories to find matches files with the filter
            info("Check match files in sub directory of ${dir}")
            dir.eachDirRecurse { d ->
                info("Checking matched files in sub directory: ${d}")
                d.eachFileMatch(~/${checkedFilePattern}/) { f ->
                    if (f.isFile() && (!f.toString().equals(outf.toString()))){
                        info("Matched file found in sub directories ${d}: ${f}")
                        fileMatches = true
                        f.withInputStream { is -> outf << is }
                        if(!isEndedByNewLine(f)) {
                            outf.withWriterAppend {
                                it.newLine()
                            }
                        }

                        if (!action.isEmpty()) {
                            switch (action) {
                                case 'MOVE':
                                     String subFolder = getRelativePath(dir, f)
                                     String pathNewFile = dest.toString() + File.separator + subFolder + f.getName()
                                     actionFileList.put(f.toString(),pathNewFile)
                                     break
                            }
                        }
                    }
                }
            }
            if (!fileMatches) {
                info("No file matches in sub directories")
            }

            if (!action.isEmpty()) {
                switch (action) {
                    case 'MOVE':
                         actionMoveFile(actionFileList)
                         break
                }
            }
        }
    }
    
    private void actionMoveFile(HashMap moveFileList) {
        info("actionMoveFile")
        
        info("Start MOVE matched files to destination directory")

        for (Map.Entry<String, String> entry : moveFileList.entrySet()) {
            
            File prevFile   = new File(entry.getKey())
            File newFile    = new File(entry.getValue())
            File newFileDir = new File(newFile.getParent())
            
            if (!newFileDir.exists()) { newFileDir.mkdirs() }

            if ( prevFile.renameTo(new File(entry.getValue())) ) {
                info("Move ${prevFile} to ${entry.getValue()} success")
            } else {
                info("Move ${prevFile} to ${entry.getValue()} failed")
            }
        }
    }

    private String getRelativePath(File baseDir, File currentFile) {
        info("getRelativePath")

        File baseFileDir = new File (currentFile.getParent())
        String getRelativePath = ""

        while (!baseDir.getPath().equals(baseFileDir.getPath())) {
            getRelativePath = baseFileDir.getName() + File.separator + getRelativePath
            baseFileDir = new File(baseFileDir.getParentFile().getPath())
        }
        return getRelativePath
    }
    
    private String detectToken(def parm) {
        info("detectToken .. $parm")

        def pattern = '$('
        def closePattern = ')'
        def file = ''
        def sdate
        int startIndex = parm.indexOf(pattern)
        int endIndex   = parm.indexOf(closePattern, startIndex)
        if (startIndex> 0 && endIndex > 0) {
            endIndex += 1
            info("Token Detected: " + parm.substring(startIndex, endIndex))
            switch (parm.substring(startIndex, endIndex)) {
                case '$(Custom.Date)':
                    sdate = new SimpleDateFormat("yyyyMMdd").format(new Date())
                    file = parm.substring(0,startIndex) + sdate + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Custom.DateTime)':
                    sdate = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
                    file = parm.substring(0,startIndex) + sdate + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Custom.Env)':
                    file = parm.substring(0,startIndex) + env.getWorkDir() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Parent.UUID)':
                    file = parm.substring(0,startIndex) + request.request.getParent().getTaskUuid() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(UUID)':
                    file = parm.substring(0,startIndex) + request.request.getUUID() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Name)':
                    file = parm.substring(0,startIndex) + request.request.getName() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Name.Full)':
                    file = parm.substring(0,startIndex) + request.request.getFullName() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Name.Request)':
                    file = parm.substring(0,startIndex) + request.request.getRequestName() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Owner)':
                    file = parm.substring(0,startIndex) + request.request.getOwner() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Status)':
                    file = parm.substring(0,startIndex) + request.request.getStatus() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Title)':
                    file = parm.substring(0,startIndex) + request.request.getTitle() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Time.Create)':
                    file = parm.substring(0,startIndex) + request.request.getCreateTime() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Time.LastChange)':
                    file = parm.substring(0,startIndex) + request.request.getUpdateTime() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Output.Type)':
                    file = parm.substring(0,startIndex) + request.request.getOutputDevice() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Output.Destination)':
                    file = parm.substring(0,startIndex) + request.request.getDestination() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                case '$(Output.Copies)':
                    file = parm.substring(0,startIndex) + request.request.getCopies() + parm.substring(endIndex)
                    info("Complete Path with Token: " + file)
                    break
                /*  FOR FUTURE DEVELOPMENT
                case '$(Output.Title)':
                    file = parm.substring(0,parm.indexOf(pattern)) + request.getUUID() + parm.substring(endIndex) 
                    info("Complete Path with Token: " + file)
                    break
                case '$(Output.Description)':
                    file = parm.substring(0,parm.indexOf(pattern)) + request.getUUID() + parm.substring(endIndex) 
                    info("Complete Path with Token: " + file)
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
     * Check whether file is ended with a newline or not.
     * @param file
     * @return Boolean
     */
    private Boolean isEndedByNewLine(File file) {
        info("Checking whether file ended with blank line...")
        Boolean isNewLine = false
        RandomAccessFile fileHandler
        try {
            fileHandler = new RandomAccessFile( file, "r" )
            fileHandler.seek( file.length() - 1 )
            int readByte = fileHandler.readByte()
            if( readByte == 0xA || readByte == 0xD) {
                isNewLine=true
            }
        }
        catch( java.io.IOException e ) {
            e.printStackTrace()
        }
        finally {
            fileHandler.close() 
        }
        
        return isNewLine
        
    }
                           
  /**
   * Execute the concatenation
   * @param parameters
   */
  private void processBatch(String baseDirectory,String  filePattern, 
          String outputFile, String action, String destination) {
    info("processBatch")

    info("Parameter Source Base Directory : " + baseDirectory)
    info("Parameter File Pattern : " + filePattern)
    info("Parameter Output File  : " + outputFile )
    info("Parameter Action        : " + action)
    info("Parameter Destination Directory : " + destination)

    Merge(baseDirectory, filePattern, outputFile, action, destination)
  }

}


/*run script*/
ProcessTrbmrg process = new ProcessTrbmrg();
process.runBatch(binding);
