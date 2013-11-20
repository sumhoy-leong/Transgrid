/**
* @Ventyx 2012
*/
package com.mincom.ellipse.script.custom

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.slf4j.LoggerFactory

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.mincom.batch.environment.BatchEnvironment
import com.mincom.batch.script.FtpClientWrapper
import com.mincom.batch.script.RequestInterface
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Key
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.efs.EFSFile
import com.mincom.ellipse.efs.EFSHelper
import com.mincom.ellipse.efs.EllipseFileSystem
import com.mincom.ellipse.script.util.EDOIWrapper
import com.mincom.eql.Constraint
import com.mincom.eql.impl.QueryImpl

/**
 *
 * This class execute a list of commands
 * The following tokens can be used in the commands (all tokens need to be Uppercase and inside curly brackets):
 *  ${UUID} - uuid
 *  ${WORK} - work directory
 *  ${1}, ${10} - the parameters from 1 to 10
 *  ${OWNER} - request user
 *  ${DATETIME} - current date time
 * Dynamic parameters can be loaded from a file, the command config_filename define the properties file name located at the folder
 * defined by the system property ellipse.fs.root, the keys will be replaced by their values (all tokens in the commands need to be uppercase)
 * Remind that the command config_filename needs to be at the top of the list of commands
 * To connect to the sftp, it uses the private key stored at the ellipse.fs.root/.ssh.
 * For the host server to accept this private key, its correspondent public key needs to be registered
 * in the host server as authorized_keys
 */
public class BatchCommands {

    private static final version = 8

    public BatchEnvironment env
    public RequestInterface request
    public EDOIWrapper edoi

    private String batchUUID
    private Boolean remoteSFTPConnected
    private Session remoteSFTPSession
    private ChannelSftp remoteChannelSFTP
    private FtpClientWrapper remoteFTPSession
    private Boolean remoteFTPConnected
    private String onSuccessEmail
    private String onFailureEmail
    private StringBuilder erroMessage
    private String fileTransferStat
    private long timeDelay = 500
    private List <String> logMessage = new ArrayList <String>()
    private String listOfAdditionalCommands
    private List <String> additionalCommands = new ArrayList <String>()
    private List <String> additionalEFSFile = new ArrayList <String>()
    private boolean onSuccessE
    private boolean onFailedE
    private Properties configFileProperties
    private Properties specialToken
    private List <String> temporaryReportFile = new ArrayList <String> ()
    private boolean hasAsterisk
    private LinkedHashMap EFSFileTempFile =  [:]

    private String listCommands
    private List<String> listParameters

    // Time maximum to wait for the channelexec to close
    private static final Long TIMEOUT_WAITING_EXEC_SFTP_CLOSE = 1000 * 60 * 3

    //Error Message
    private static final String ERROR_PREFIX = "*** ERROR *** "
    private static final String TRANSFERID_REQUIRED = "Can not Find Transfer ID, Script aborted"
    private static final String TRANSFERID_NOTFOUND = "Could not Found Property ID"
    private static final String INVALID_COMMAND_TYPE = "Invalid Command Type"
    private static final String COMMAND_TYPE_NOT_COMPLETE = "Command Type not complete"
    private static final String INCOMPLETE_ARGUMENTS = "Incomplete arguments"
    private static final String INVALID_ARGUMENTS = "Invalid arguments"
    private static final String COPY_FAILED = "Copy Failed"
    private static final String DELETE_FAILED = "Delete Failed"
    private static final String CAT_FAILED = "Concatenate Failed"
    private static final String MUST_START_WITH_CONNECT = "must start with connect."
    private static final String UNRECOGNIZE_KEYWORD ="Unrecognize keyword"
    private static final String FOLDER_FSROOT = "custom.root"

    // Info Message
    private static final String FILE_CONFIG_NOTFOUND = "File config was not found"
    private static final String SILENT_ERROR = "The error was ignored"

    //Constant
    private static final String FILE_TRANSFER_SUCCESS = "Success"
    private static final String FILE_TRANSFER_FAILED = "Failed"
    private static final String COMMAND_ONSUCCESS_EMAIL = "on_success_email"
    private static final String COMMAND_ONFAILURE_EMAIL = "on_failure_email"
    private static final String COMMAND_TIMEOUT = "timedelay"
    private static final String COMMAND_LOCAL = "local"
    private static final String COMMAND_FTP = "ftp"
    private static final String COMMAND_SFTP = "sftp"
    private static final String COMMAND_ALL_PROTOCOL = "all"
    private static final String COMMAND_CONFIG_FILENAME = "config_filename"

    private static final String HOSTID_KEYWORD = "hostid"
    private static final String HOST_KEYWORD = "host"
    private static final String USER_KEYWORD = "user"
    private static final String PASS_KEYWORD = "pwd"
    private static final String PORT_KEYWORD = "port"
    private static final String COMMAND_CAT = "cat"
    private static final String COMMAND_TOUCH = "touch"
    private static final String COMMAND_CONNECT = "connect"
    private static final String COMMAND_GET = "get"
    private static final String COMMAND_MGET = "mget"
    private static final String COMMAND_PUT = "put"
    private static final String COMMAND_CD = "cd"
    private static final String COMMAND_LCD = "lcd"
    private static final String COMMAND_DELETE = "rm"
    private static final String COMMAND_CLOSE = "close"
    private static final String COMMAND_COPY = "cp"
    private static final String COMMAND_MOVE = "mv"
    private static final String COMMAND_EXEC = "exec"
    private static final String COMMAND_COPY_TOHASHDIR = "cp_tohashdir"
    private static final String CHARACTER_COMMENT = "#"

    private static final String PARAMETER_SILENT = "--silent"

    /*
     * Initialize Variables
     */
    public BatchCommands(Binding b) {
        super()
        env = b.getVariable("env")
        request = b.getVariable("request")
        edoi = b.getVariable("edoi")
    }

    private void initVariables (){
        // setup UUID
        info ("Initialize variables...")

        String uuid = request.request.getTaskUuid()?:request.getUUID()

        if (uuid.contains("AABTRF")){
            batchUUID = UUID.randomUUID().toString()
        }else{
            batchUUID = uuid
        }

        remoteFTPConnected = false
        remoteSFTPConnected = false
        onSuccessE = false
        onFailedE = false
        hasAsterisk = false
        fileTransferStat = FILE_TRANSFER_SUCCESS
        temporaryReportFile = new ArrayList <String> ()
        listOfAdditionalCommands = ""
    }

    private void deleteTemporaryFiles(){

        info ("deleteTemporaryFiles:" + temporaryReportFile.size().toString() + "File(s)" )
        temporaryReportFile.each{
            info("deleting:" + it)
            File tempFile = new File (it)
            tempFile.delete()
        }
    }

    /**
     * Execute Command
     * */
    private void executeCommand(String [] commandList){

        String commandArguments = ""
        commandList.each{String command ->
            info ("Executing: ${command}")

            List <String> commandTexts = new ArrayList <String>()
            commandTexts = command.split(":")

            validateArguments(commandTexts, 2, COMMAND_TYPE_NOT_COMPLETE)

            commandArguments = convertToken(commandTexts[1].trim())

            info ("Arguments: ${commandArguments}")

            switch(commandTexts[0].trim().toLowerCase()){
                case COMMAND_ONSUCCESS_EMAIL: onSuccessEmail = commandArguments
                    onSuccessE = true
                    break
                case COMMAND_ONFAILURE_EMAIL: onFailureEmail = commandArguments
                    onFailedE = true
                    break
                case COMMAND_TIMEOUT:         timeDelay = Long.parseLong(commandArguments)
                    break
                case COMMAND_LOCAL:           doCommandLocal(commandArguments)
                    break
                case COMMAND_SFTP:            doCommandSFTP(commandArguments)
                    break
                case COMMAND_FTP:             doCommandFTP(commandArguments)
                    break
                case COMMAND_CONFIG_FILENAME: loadConfigFileProperties(commandArguments)
                    break
                default: errorMessage(commandTexts[0].trim()+" "+INVALID_COMMAND_TYPE)
                    break

            }

            Thread.sleep(timeDelay)
        }
    }

    /**
     * Executes a closure silencing the errors if the parameter is true
     * @param silentMode defines if it needs to silence the errors
     * @param c closure to be executed
     */
    private void executeSafely(boolean silentMode, Closure c) {
        try {
            c.call()
        } catch (all) {
            if (silentMode) {
                info(SILENT_ERROR)
                info(all.message)
            } else {
                throw all
            }
        }
    }

    /**
     * Load properties to replace as tokens
     * @param filename File name of the properties
     */
    private void loadConfigFileProperties(String filename) {
        File file = new File(System.getProperty(FOLDER_FSROOT) + "/.ssh/" + filename)
        if (file.exists()) {
            DataInputStream dis
            try {
                dis = file.newDataInputStream()
                configFileProperties = new Properties()
                configFileProperties.load(dis)
            } finally {
                dis.close()
            }
        } else {
            info(FILE_CONFIG_NOTFOUND)
        }
    }

    /**
     * Convert all tokens in the parameters
     *
     * @param parameters
     *
     * @return parameters
     */
    private String convertToken (String parameters){
        String S_PARAM_NUMBER = "\\\$\\{NUMBER\\}"
        String S_UUID = "\\\$\\{UUID\\}"
        String S_OWNER = "\\\$\\{OWNER\\}"
        String S_DATETIME = "\\\$\\{DATETIME\\}"
        String S_WORK = "\\\$\\{WORK\\}"
        String S_INSTANCE = "\\\$\\{INSTANCE\\}"

        String result = parameters

        if (listParameters) {
            for (int nParam = 0; nParam < listParameters.size(); nParam++) {
                result = result.replaceAll(S_PARAM_NUMBER.replace("NUMBER", nParam.toString()), listParameters[nParam])
            }
        }

        result = result.replaceAll(S_UUID, batchUUID)
        result = result.replaceAll(S_OWNER, request.getUser())
        result = result.replaceAll(S_DATETIME, new Date().format("yyyyMMdd_kkmmss") )
        result = result.replaceAll(S_WORK, env.getWorkDir().toString())
        //result = result.replaceAll(S_INSTANCE, getInstanceName())

        // replace the properties from the config file
        if (configFileProperties != null) {
            configFileProperties.keys().each { String key ->
                result = result.replaceAll("\\\$\\{" + key.toUpperCase() + "\\}",
                        configFileProperties.getProperty(key)) }
        }

        result = result.trim().replaceAll("\\s+", " ")
        result = extractSpecialTokens (result)
        return result
    }

    /**
     * Returns the instance of the ellipse
     * @return
     */
    public String getInstanceName() {
        return java.net.InetAddress.getLocalHost().getHostName().split("\\.")[1]
    }

    /*
     * Extract Special tokens
     *
     * @param : Parameters
     *
     * Example local:cp ${REPORT_NAME=Table File Listing Listing table file - MSR010A.pdf} abcd/the_description_MSR010A.1234.pdf
     * will get the REPORT_NAME=Table File Listing Listing table file - MSR010A.pdf
     * */
    private String extractSpecialTokens(String parameters){
        info ("extractSpecialTokens : " +  parameters)
        String FILES_IN_HASH_DIR = 'FILES_IN_HASHDIR'
        String FILE_IN_HASH_DIR = 'FILE_IN_HASHDIR'
        String translatedSpecialToken = ""
        String result = parameters
        Pattern regex = Pattern.compile("\\\$\\{([^}]*)\\}")
        Matcher regexMatcher = regex.matcher(result)
        hasAsterisk = false
        String outputFile = ""
        List <String> tempRptFiles = new ArrayList <String>()

        while (regexMatcher.find()) {

            if ( regexMatcher.group(1).contains(FILES_IN_HASH_DIR)){

                translatedSpecialToken = doCopySourceReportName(regexMatcher.group(1),false,batchUUID)

                info (FILES_IN_HASH_DIR + " Translated Token:" + translatedSpecialToken)
                result = result.replace(regexMatcher.group(0),translatedSpecialToken)

                if (hasAsterisk){
                    //if has asterisk then the output become the suffix
                    outputFile = EFSFileTempFile.get(translatedSpecialToken)
                    result = result + "_"+outputFile.replaceAll(" ","_")
                }
                info ("FinalCommand:" + result)
                buildingAdditionalCommands(translatedSpecialToken, parameters,regexMatcher.group(0),"")
            }

            if ( regexMatcher.group(1).contains(FILE_IN_HASH_DIR)){
                translatedSpecialToken = doCopySourceReportName(regexMatcher.group(1),true,batchUUID)
                info (FILE_IN_HASH_DIR + "Translated Token:" + translatedSpecialToken)
                result = result.replace(regexMatcher.group(0),translatedSpecialToken)
            }
        }

        info ("list of Additional command: " + listOfAdditionalCommands)
        return result

    }

    private void buildingAdditionalCommands (String translatedSpecialToken,String parameters, String InputFile, String prefixOutput){
        info("buildingAdditionalCommands translatedSpecialToken: ${translatedSpecialToken} paramInputFile:${InputFile} outputFile:${prefixOutput}")
        List <String> tempRptFiles = new ArrayList <String>()
        String outputFile = ""
        //change any whitespace to underscore (as it will impact the filename)
        tempRptFiles.addAll(temporaryReportFile)
        tempRptFiles.remove(translatedSpecialToken)
        tempRptFiles.each{
            //most of the time the REPORT_NAME can only be used for local command.
            //thus the 'local:' is added in the additionalCommands array

            if (hasAsterisk){
                outputFile = EFSFileTempFile.get(it)
                listOfAdditionalCommands = listOfAdditionalCommands + "local: " + parameters.replace(InputFile,it) +  "_" +outputFile.replaceAll(" ","_")+prefixOutput+ "\r\n"
            }else{
                listOfAdditionalCommands = listOfAdditionalCommands + "local: " + parameters + "\r\n"
            }
        }
    }
    /**
     * Copy RFile from the hash directory to temporary File
     * temporaryFile is used so it will work with the current command.
     * @param parameters
     *
     * return temporaryReportFileName
     *
     * Example
     * parameters: {REPORT_NAME=Table File Listing Listing table file - MSR010A.pdf
     * return:    env.getWorkDir().toString()+"/" + UUID.randomUUID()
     * */
    private String doCopySourceReportName(String parameters,boolean onlyOneFile, String  stringUUID){
        info ("doCopySourceReportName ${parameters}")
        List <String> reportName = new ArrayList <String>()
        reportName = parameters.split("=")

        EllipseFileSystem efs = EFSHelper.getEllipseFileSystem()
        EFSFile f = null

        if (reportName.size() < 1){
            errorMessage(INCOMPLETE_ARGUMENTS)
        }

        if (reportName[1].contains("*")){
            hasAsterisk = true
            return processingAsterisk(efs,reportName[1],onlyOneFile)
        }else{
            // read this file into InputStream
            f = new EFSFile(stringUUID, reportName[1])
            return copyingEFSFiles(efs,f)

        }

    }

    private String processingAsterisk(EllipseFileSystem efs,String fName, boolean onlyOneFile){
        info ("processingAsterisk ${fName}")
        String tempFiles = ""
        String EFSFileName =""
        boolean okToExecuteNext = true
        List <String> partFile = new ArrayList<String>()
        List <EFSFile> listEFSFile = new ArrayList <EFSFile>()
        partFile = fName.split("\\*")
        listEFSFile = efs.listGroup(batchUUID)

        listEFSFile.each{
            EFSFileName = it.toString().substring(it.toString().lastIndexOf("/")+1+1,it.toString().length()-1)

            if (okToExecuteNext){
                if (partFile.size() == 0){
                    //copy all files and process the 1st the rest will be processed later
                    tempFiles = copyingEFSFiles(efs,it)
                }

                if (partFile.size() == 1){
                    //if the size is 1 that means startwith
                    //e.g reportName[1] = prefix*

                    if (EFSFileName.startsWith(partFile[0])){
                        tempFiles = copyingEFSFiles(efs,it)
                    }

                }

                if (partFile.size() == 2){
                    //if the size is 1 that means startwith
                    //e.g reportName[1] = prefix*suffix
                    // or
                    // reportName[1] = *suffix

                    if (EFSFileName.endsWith(partFile[1])){

                        if (partFile[0].trim() ==""){
                            tempFiles = copyingEFSFiles(efs,it)
                        }else{

                            if (EFSFileName.startsWith(partFile[0])){
                                tempFiles = copyingEFSFiles(efs,it)
                            }

                        }

                    }

                }// end if for partFile.size() = 2
            }


            if (partFile.size()>2){
                errorMessage(INVALID_ARGUMENTS)
            }

            if (onlyOneFile && tempFiles != ""){
                okToExecuteNext = false
            }

        }

        return tempFiles
    }

    private String copyingEFSFiles(EllipseFileSystem efs, EFSFile f){
        info ("copyingEFSFiles")
        String EFSFileName = f.toString().substring(f.toString().lastIndexOf("/")+1+1,f.toString().length()-1)
        InputStream inputStream = efs.read(f)

        // write the inputStream to a FileOutputStream
        String tempEFSFile = env.getWorkDir().toString()+"/" + UUID.randomUUID()
        OutputStream out = new FileOutputStream(new File(tempEFSFile))

        int read = 0
        byte[] bytes = new byte[1024]

        while ((read = inputStream.read(bytes)) != -1) {
            out.write(bytes, 0, read)
        }

        inputStream.close()
        out.flush()
        out.close()

        temporaryReportFile.add(tempEFSFile)
        EFSFileTempFile.put(tempEFSFile, EFSFileName)
        return tempEFSFile
    }
    /**
     * Do Local Command
     * Valid local command are:
     * cp -> copy
     * rm -> delete
     * mv -> move
     * touch -> touch file
     * */
    private void doCommandLocal(String commandArguments){

        boolean silentErrors = false
        if (commandArguments.contains(PARAMETER_SILENT)) {
            silentErrors = true
            commandArguments = commandArguments.replace(PARAMETER_SILENT, "")
        }

        List <String> commandLines = new ArrayList <String>()
        commandLines = commandArguments.split(" ")


        switch (commandLines[0]){
            case COMMAND_COPY:  commandLines.remove(COMMAND_COPY)
                localCopyFiles(commandLines)
                break

            case COMMAND_DELETE:commandLines.remove(COMMAND_DELETE)
                localDeleteFiles(commandLines,silentErrors)
                break

            case COMMAND_MOVE:  commandLines.remove(COMMAND_MOVE)
                localMoveFiles (commandLines)
                break

            case COMMAND_CAT:   commandLines.remove(COMMAND_CAT)
                localCatFiles (commandLines)
                break

            case COMMAND_TOUCH:   commandLines.remove(COMMAND_TOUCH)
                localTouchFiles (commandLines)
                break
            case COMMAND_COPY_TOHASHDIR: commandLines.remove(COMMAND_COPY_TOHASHDIR)
                localCopyToHashDir(commandLines)
                break
            default:            errorMessage(commandLines[0].trim()+" "+INVALID_COMMAND_TYPE)
                break

        }

    }

    private void localCopyFiles (List <String> parametersCollection){

        validateArguments(parametersCollection,1,INCOMPLETE_ARGUMENTS)

        File fileSource = new File(parametersCollection[0])
        File fileDestination = new File(parametersCollection[1])

        info ("Copying " + parametersCollection[0] + " to "  + parametersCollection[1])

        if (fileDestination.exists()){
            fileDestination.delete()
        }

        DataInputStream SourceInput = fileSource.newDataInputStream()
        DataOutputStream DestinationOutput = fileDestination.newDataOutputStream()

        DestinationOutput << SourceInput

        SourceInput.close()
        DestinationOutput.close()

        if (fileDestination.exists()){
            info("Copy Successfull")
        }else{
            errorMessage(COPY_FAILED)
        }

    }

    private void localDeleteFiles (List <String> fileList, boolean silentErrors){

        fileList.each{String eachFile ->
            info ("Deleting ${eachFile} ... ")
            File fileName = new File (eachFile)



            if (fileName.delete()){
                info (eachFile + " Deleted")
            }else{
                if (silentErrors){
                    info (eachFile +" "+ DELETE_FAILED)
                }else{
                    errorMessage(eachFile +" "+ DELETE_FAILED)
                }
            }
        }
    }

    private void localCatFiles (List <String> parametersCollection){
        String  OUTPUT_SEPARTOR = ">"

        validateArguments(parametersCollection,1,INCOMPLETE_ARGUMENTS)

        if (!parametersCollection.contains(OUTPUT_SEPARTOR)){
            errorMessage(INCOMPLETE_ARGUMENTS)
        }

        Integer totalSize = parametersCollection.size()
        String  outputFileName = parametersCollection[totalSize - 1]

        File outputFile = new File (outputFileName)
        File fileSource = null



        info ("Concatenate Files to ${outputFileName}" )

        if (outputFile.exists()){
            outputFile.delete()
        }

        DataOutputStream DestinationOutput = outputFile.newDataOutputStream()

        parametersCollection.remove(OUTPUT_SEPARTOR)
        parametersCollection.remove(outputFileName)

        parametersCollection.each{String fileName ->
            fileSource = new File (fileName)
            DataInputStream SourceInput = fileSource.newDataInputStream()
            DestinationOutput << SourceInput
            DestinationOutput << "\r\n"
            SourceInput.close()
        }

        DestinationOutput.close()

        if (outputFile.exists()){
            info ("Concatenate Successfull")
        }else{
            errorMessage(CAT_FAILED)
        }
    }

    private void localTouchFiles (List <String> parametersCollection){
        info ("Touch files")
        parametersCollection.each { filename ->
            File file = new File(filename)
            if (!file.exists()) {
                file.append("")
            } else {
                file.setLastModified(new Date().getTime())
            }
        }
    }

    private void localMoveFiles (List <String> parametersCollection){
        info ("Moving Files ...")
        localCopyFiles (parametersCollection)
        List <String> setParameter = [parametersCollection[0]]
        localDeleteFiles(setParameter)
        info ("Move Successfull")
    }

    private void localCopyToHashDir (List <String> parametersCollection){
        validateArguments(parametersCollection,0,INCOMPLETE_ARGUMENTS)

        EllipseFileSystem efs = EFSHelper.getEllipseFileSystem()
        String outFileName = parametersCollection[0].substring(parametersCollection[0].lastIndexOf("/")+1,parametersCollection[0].length())
        EFSFile f = new EFSFile(batchUUID,outFileName)
        OutputStream os = efs.write(f)

        File inputFile = new File (parametersCollection[0])
        InputStream  input = new FileInputStream(inputFile)

        int read = 0
        byte[] bytes = new byte[1024]

        while ((read = input.read(bytes)) != -1) {
            os.write(bytes, 0, read)
        }

        input.close()
        os.flush()
        os.close()

    }
    private void doCommandSFTP(String commandArguments){
        boolean silentErrors = false
        if (commandArguments.contains(PARAMETER_SILENT)) {
            silentErrors = true
            commandArguments = commandArguments.replace(PARAMETER_SILENT, "")
        }

        List <String> commandLines = new ArrayList <String>()
        commandLines = commandArguments.split(" ")

        if (remoteSFTPConnected){
            switch (commandLines[0].toLowerCase()){
                case COMMAND_GET:   commandLines.remove(COMMAND_GET)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteSFTPOperation(commandLines,COMMAND_GET, silentErrors)
                    break

                case COMMAND_MGET:  commandLines.remove(COMMAND_MGET)
                    validateArguments(commandLines,0,INVALID_ARGUMENTS)
                    remoteSFTPMGet(commandLines, silentErrors)
                    break

                case COMMAND_PUT:   commandLines.remove(COMMAND_PUT)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteSFTPOperation(commandLines,COMMAND_PUT, silentErrors)
                    break

                case COMMAND_CD :   commandLines.remove(COMMAND_CD)
                    validateArguments(commandLines,0,INVALID_ARGUMENTS)
                    executeSafely(silentErrors, {remoteChannelSFTP.cd(commandLines[0])})
                    info ("Now remote directory is ${commandLines[0]}")
                    break

                case COMMAND_LCD:   commandLines.remove(COMMAND_LCD)
                    validateArguments(commandLines,0,INVALID_ARGUMENTS)
                    executeSafely(silentErrors, {remoteChannelSFTP.lcd(commandLines[0])})
                    info ("Now local directory is ${commandLines[0]}")
                    break

                case COMMAND_DELETE:commandLines.remove(COMMAND_DELETE)
                    validateArguments(commandLines,0,INVALID_ARGUMENTS)
                    remoteSFTPOperation(commandLines,COMMAND_DELETE, silentErrors)
                    break

                case COMMAND_MOVE:  commandLines.remove(COMMAND_MOVE)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteSFTPOperation(commandLines,COMMAND_MOVE, silentErrors)
                    break

                case COMMAND_EXEC:  commandLines.remove(COMMAND_EXEC)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteSFTPExecCommand(commandLines, silentErrors)
                    break

                case COMMAND_CLOSE: closeServerConnection(COMMAND_SFTP)
                    break
                default:            errorMessage(commandLines[0].trim()+" "+INVALID_COMMAND_TYPE)


            }

        }else{
            if (commandLines[0].trim().toLowerCase().equals(COMMAND_CONNECT)){
                connectServer(commandArguments,COMMAND_SFTP)
            }else{
                errorMessage(COMMAND_SFTP +" "+ MUST_START_WITH_CONNECT)
            }

        }


    }

    private void remoteSFTPExecCommand(List <String> parametersCollection, boolean silentErrors){
        // Get response from the inputstream
        def getResponse = { InputStream inputStream ->
            byte[] buf = new byte[1024]
            int length
            StringBuilder output = new StringBuilder()
            while ((length=inputStream.read(buf))!=-1){
                output.append(new String(buf,0,length))
            }
            output.toString()
        }
        // Waits for the channel to close
        def waitForChannel = {ChannelExec channel, long awaitTime ->
            final long until = System.currentTimeMillis() + awaitTime
            while (!channel.isClosed() && System.currentTimeMillis() < until) {
                Thread.sleep(250)
            }
            if (!channel.isClosed()) {
                throw new RuntimeException("Channel not closed in time")
            }
        }

        ChannelExec channel = remoteSFTPSession.openChannel("exec")
        try {
            InputStream errorInputStream = channel.getErrStream()
            InputStream inputStream = channel.getInputStream()

            channel.setCommand(parametersCollection.join(" "))
            channel.connect()

            waitForChannel(channel, TIMEOUT_WAITING_EXEC_SFTP_CLOSE)

            if (channel.getExitStatus() != 0) {
                String error = getResponse(errorInputStream)
                info("Error executing command " + error)
                if (!silentErrors) {
                    throw new RuntimeException("Error executing exec shell = " + error)
                }
            }

            String result = getResponse(inputStream)
            info("Command executed - result = " + result)
        } finally {
            channel.disconnect()
        }
    }

    private void remoteSFTPOperation(List <String> parametersCollection, String operation, boolean silentErrors){

        String outputFileName = ""
        String inputFileName = parametersCollection[0]


        if ((operation.equals(COMMAND_GET)) ||
        (operation.equals(COMMAND_PUT)) ||
        (operation.equals(COMMAND_MOVE)))
        {
            switch (parametersCollection.size()){
                case 1:
                    outputFileName = inputFileName
                    break
                case 2: inputFileName = parametersCollection[0]
                    outputFileName = parametersCollection[1]
                    break
                default:
                    errorMessage(INVALID_ARGUMENTS)
            }
        }

        executeSafely(silentErrors, {
            if (operation.equals(COMMAND_GET)){
                info ("Getting files ${parametersCollection[0]} ...")
                remoteChannelSFTP.get(inputFileName,outputFileName)
                info ("Get successfull")
            }

            if (operation.equals(COMMAND_PUT)){
                info ("Putting files ${parametersCollection[0]} ...")
                remoteChannelSFTP.put(inputFileName,outputFileName)
                info ("Put successfull")
            }

            if (operation.equals(COMMAND_DELETE)){
                parametersCollection.each{String remoteFileName ->
                    info ("Deleting ${remoteFileName} ...")
                    remoteChannelSFTP.rm(remoteFileName)
                    info ("Delete Successfull")
                }
            }

            if (operation.equals(COMMAND_MOVE)){
                info ("Moving ${parametersCollection[0]} ...")
                remoteChannelSFTP.rename(inputFileName, outputFileName)
                info ("Move successfull")
            }
        })
    }

    private void remoteSFTPMGet (List <String> parametersCollection, boolean silentErrors){
        Vector <ChannelSftp.LsEntry> listAllFiles = remoteChannelSFTP.ls(parametersCollection[0])

        executeSafely(silentErrors, {
            List <String> listFiles = new ArrayList <String>()
            listAllFiles.each{
                listFiles = [it.getFilename()]
                remoteSFTPOperation(listFiles,COMMAND_GET, silentErrors)
            } }
        )

    }

    public static Map<String, String> returnHostById(String hostId) {
        Map<String, String> result = [:]

        String fileText = new File(System.getProperty("custom.root") + "/batch/src/com/mincom/ellipse/script/custom/hosts.xml").getText()

        def hosts = new XmlParser().parseText(fileText)

        def host = hosts.find {it."@id" == hostId}

        host.attributes().each {
            result[it.key] = it.value
        }
    }

    /**
     * Connect via SFTP
     *
     * */
    private connectServer(String commandArgumnents, String sProtocol){

        String oneSpaces = ""
        String remoteUserName = ""
        String remoteHostName = ""
        String remotePass = ""
        int remotePort = 0

        List <String> sConfig = new ArrayList <String>()
        List <String> keywords = new ArrayList <String>()


        keywords = commandArgumnents.trim().split("\\-\\-")

        keywords.each{String keyword->

            oneSpaces = keyword.trim().replaceAll("\\s+", " ")
            sConfig = oneSpaces.split(" ")

            switch (sConfig[0].trim()){
                case HOSTID_KEYWORD:
                    Map<String, String> server = returnHostById(sConfig[1])
                    remoteHostName = server.host
                    remoteUserName = server.user
                    remotePass = server.pwd
                    break
                case HOST_KEYWORD: remoteHostName = sConfig[1]
                    break

                case USER_KEYWORD: remoteUserName = sConfig[1]
                    break

                case PASS_KEYWORD: remotePass = sConfig[1]
                    break

                case PORT_KEYWORD: remotePort = Integer.parseInt(sConfig[1])
                    break

                case COMMAND_CONNECT: break

                default : errorMessage(sConfig[0] + " " + UNRECOGNIZE_KEYWORD)

            }
        }

        info ("Connecting to ${remoteHostName} ...")

        //SFTP connection
        if (sProtocol.equals(COMMAND_SFTP)){

            if (remotePort == 0){
                remotePort = 22
            }
            JSch remote = new JSch()

            info("Directory .ssh private key = " + System.getProperty(FOLDER_FSROOT) + "/.ssh/id_rsa")

            remote.addIdentity(System.getProperty(FOLDER_FSROOT) + "/.ssh/id_rsa")
            remoteSFTPSession  = remote.getSession(remoteUserName, remoteHostName, remotePort)
            Properties config = new Properties()
            config.setProperty("StrictHostKeyChecking", "no")
            remoteSFTPSession.setConfig(config)
            remoteSFTPSession.connect()
            remoteChannelSFTP = remoteSFTPSession.openChannel("sftp")
            remoteChannelSFTP.connect()
            remoteChannelSFTP.lcd(env.getWorkDir().toString())
            remoteSFTPConnected = true
        }

        //FTP Connection
        if (sProtocol.equals(COMMAND_FTP)){

            if (remotePort == 0){
                remotePort = 21
            }
            remoteFTPSession = new FtpClientWrapper()
            remoteFTPSession.connect(remoteHostName, remotePort, remoteUserName, remotePass)
            remoteFTPConnected = true
        }

        info ("Connected")
    }

    private void closeServerConnection(String sProtocol){

        if ((sProtocol.equals(COMMAND_SFTP) || sProtocol.equals(COMMAND_ALL_PROTOCOL))){

            if (remoteChannelSFTP) {
                remoteChannelSFTP.exit()
            }

            if (remoteSFTPSession) {
                remoteSFTPSession.disconnect()
            }
            remoteSFTPConnected = false
        }

        if (remoteFTPConnected && (sProtocol.equals(COMMAND_FTP)|| sProtocol.equals(COMMAND_ALL_PROTOCOL))){
            remoteFTPSession.disconnect()
            remoteFTPConnected = false
        }
    }

    private void doCommandFTP(String commandArguments){
        boolean silentErrors = false
        if (commandArguments.contains(PARAMETER_SILENT)) {
            silentErrors = true
            commandArguments = commandArguments.replace(PARAMETER_SILENT, "")
        }

        List <String> commandLines = new ArrayList <String>()
        commandLines = commandArguments.split(" ")

        if (remoteFTPConnected){

            switch (commandLines[0].toLowerCase()){
                case COMMAND_GET:   commandLines.remove(COMMAND_GET)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteFTPOperation(commandLines,COMMAND_GET, silentErrors)
                    break

                case COMMAND_MGET:  commandLines.remove(COMMAND_MGET)
                    validateArguments(commandLines,0,INVALID_ARGUMENTS)
                    remoteFTPMGet(commandLines, silentErrors)
                    break

                case COMMAND_PUT:   commandLines.remove(COMMAND_PUT)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteFTPOperation(commandLines,COMMAND_PUT, silentErrors)
                    break

                case COMMAND_CD :   commandLines.remove(COMMAND_CD)
                    validateArguments(commandLines,0,INVALID_ARGUMENTS)
                    executeSafely(silentErrors, {
                        remoteFTPSession.changeWorkDir(commandLines[0])
                        info ("Now remote directory is ${commandLines[0]}")
                    })
                    break

                case COMMAND_DELETE:commandLines.remove(COMMAND_DELETE)
                    validateArguments(commandLines,1,INVALID_ARGUMENTS)
                    remoteFTPOperation(commandLines,COMMAND_DELETE, silentErrors)
                    break

                case COMMAND_CLOSE: closeServerConnection(COMMAND_FTP)
                    break
                default:            errorMessage(commandLines[0].trim()+" "+INVALID_COMMAND_TYPE)
            }

        }else{

            if (commandLines[0].trim().toLowerCase().equals(COMMAND_CONNECT)){
                connectServer(commandArguments,COMMAND_FTP)
            }else{
                errorMessage(COMMAND_FTP +" "+ MUST_START_WITH_CONNECT)
            }

        }

    }

    private void remoteFTPMGet(List <String> parametersCollection, boolean silentErrors){
        executeSafely(silentErrors, {
            List <String> listFiles = remoteFTPSession.getFilesList(parametersCollection[0])
            listFiles.each {String eachFile ->
                remoteFTPOperation([
                    eachFile,
                    eachFile + "." + batchUUID
                ], COMMAND_GET, silentErrors)
            }
        })
    }

    private void remoteFTPOperation(List <String> parametersCollection, String operation, boolean silentErrors){
        String outputFileName = ""
        String inputFileName = parametersCollection[0]

        if((operation.equals(COMMAND_GET)) ||
        (operation.equals(COMMAND_PUT))){
            switch (parametersCollection.size()){
                case 1:
                    outputFileName = inputFileName
                    break
                case 2: inputFileName = parametersCollection[0]
                    outputFileName = parametersCollection[1]
                    break
                default:
                    errorMessage(INVALID_ARGUMENTS)
            }

        }

        executeSafely(silentErrors, {
            if (operation.equals(COMMAND_GET)){
                info ("Getting files ${parametersCollection[0]} ...")
                remoteFTPSession.getFile(inputFileName,outputFileName)
                info ("Get successfull")
            }

            if (operation.equals(COMMAND_PUT)){
                info ("Putting files ${parametersCollection[0]} ...")
                remoteFTPSession.putFile(inputFileName, outputFileName)
                info ("Put successfull")
            }

            if (operation.equals(COMMAND_DELETE)){
                parametersCollection.each {String remoteFileName ->
                    info ("Deleting files ${remoteFileName} ...")
                    remoteFTPSession.deleteFile(remoteFileName)
                    info ("Delete successfull")
                }
            }
        })
    }

    private void validateArguments(List<String> argumentsCollections, Integer minSize, String errorMsg){
        if (minSize == 0){
            if (argumentsCollections.size()!=1){
                info ("Size Arguments: " + argumentsCollections.size().toString())
                errorMessage(errorMsg)
            }
        }else{
            if (argumentsCollections.size()< minSize){
                info ("Size Arguments" + argumentsCollections.size().toString())
                errorMessage(errorMsg)
            }
        }
    }

    private void sendEmail(String transferStatus){
        info ("Sending Email ...")

        String subject = "File Transfer " + transferStatus
        String mailTo = null
        if (transferStatus.equals(FILE_TRANSFER_SUCCESS)){
            if (onSuccessE) {
                mailTo = onSuccessEmail
                info ("OnSuccess Email to:" +  mailTo)
            }
        }else{
            if (onFailedE) {
                mailTo = onFailureEmail
                info ("OnFailure Email to:" + mailTo)
            }
        }

        if (mailTo) {
            SendEmail myEmail = new SendEmail(subject,mailTo,logMessage)
            myEmail.sendMail()
            info ("Email Sent...")
        }

    }

    /**
     * Print Error Message
     **/
    private void errorMessage(String sErrorMsg){
        fileTransferStat = FILE_TRANSFER_FAILED
        info(ERROR_PREFIX+sErrorMsg)
        logMessage.add("------------- " + ERROR_PREFIX+sErrorMsg)
        assert false,sErrorMsg
    }

    /**
     * Print a string into the logger.
     * @param value a string to be printed.
     */
    private void info(String value){
        def logObject = LoggerFactory.getLogger(getClass())
        logObject.info("------------- " + value)
    }

    public BatchCommands loadCommands(String commands) {
        listCommands = commands
        this
    }

    public BatchCommands setParameters(List<String> parameters) {
        listParameters = parameters
        this
    }

    /**
     * Execute the commands passed by the method loadCommands
     */
    public void execute() {
        info("BatchCommands version: "+ version)
        try {
            //Initialize Variables
            initVariables()

            String [] commandsList  = listCommands.split("\\r?\\n").findAll { String command ->
                // remove comments and blank lines
                !command.trim().startsWith(CHARACTER_COMMENT) && command.trim()
            }
            //Execute Command
            executeCommand(commandsList)

            if (listOfAdditionalCommands.length()>0){
                info ("executing additional commands...")
                commandsList  = listOfAdditionalCommands.split("\\r?\\n").findAll { String command ->
                    // remove comments and blank lines
                    !command.trim().startsWith(CHARACTER_COMMENT) && command.trim()
                }
                executeCommand(commandsList)
            }

        } finally {
            closeServerConnection(COMMAND_ALL_PROTOCOL)
            if (onSuccessE || onFailedE){
                sendEmail(fileTransferStat)
            }
            deleteTemporaryFiles()
            info("Finish ...")

        }
    }

}
