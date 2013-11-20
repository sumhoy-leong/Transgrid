/**
 *  @Ventyx 2012
 *  This program uploads the result "TRT62C.csv" into, <br>
 *  Ellipse by closing work order in ellipse and storing comments <br>
 *  added to the work order by metering application <br>
 *  
 *  Developed based on <b>FDD.Interfacing.Ellipse-Metering Management.D02.docx</b>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.rdl.parser.util.ParseException;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import java.util.Calendar;

import groovy.sql.Sql;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.eroi.linkage.mssemp.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf621.*;
import com.mincom.ellipse.edoi.ejb.msf600.*;
import com.mincom.ellipse.edoi.ejb.msf617.*;
import com.mincom.ellipse.edoi.ejb.msf629.*;
import com.mincom.ellipse.edoi.ejb.msf622.*;
import com.mincom.ellipse.edoi.ejb.msf623.*;
import com.mincom.ellipse.edoi.ejb.msf625.*;
import com.mincom.ellipse.edoi.ejb.msf690.*;
import com.mincom.ellipse.edoi.ejb.msf627.*;
import com.mincom.ellipse.edoi.ejb.msf6a1.*;
import com.mincom.ellipse.edoi.ejb.msf62w.*;
import com.mincom.ellipse.edoi.ejb.msf624.*;
import com.mincom.ellipse.edoi.ejb.msf645.*;
import com.mincom.ellipse.edoi.ejb.msf660.*;
import com.mincom.ellipse.edoi.ejb.msf693.*;
import com.mincom.ellipse.edoi.ejb.msf655.*;
import com.mincom.ellipse.edoi.ejb.msf656.*;
import com.mincom.ellipse.edoi.ejb.msf62h.*;
import com.mincom.ellipse.edoi.ejb.msf700.*;
import com.mincom.ellipse.edoi.ejb.msf720.*;
import com.mincom.ellipse.edoi.ejb.msf731.*;
import com.mincom.ellipse.edoi.ejb.msf733.*;
import com.mincom.ellipse.edoi.ejb.msf734.*;
import com.mincom.ellipse.edoi.ejb.msf735.*;
import com.mincom.ellipse.edoi.ejb.msf04d.*;
import com.mincom.ellipse.edoi.ejb.msf04a.*;
import com.mincom.ellipse.edoi.ejb.msf04t.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf011.*;
import com.mincom.ellipse.edoi.ejb.msfx64.*;
import com.mincom.ellipse.edoi.ejb.msfx55.*;
import com.mincom.ellipse.edoi.ejb.msfx60.*;
import com.mincom.ellipse.edoi.ejb.msfx6f.*;
import com.mincom.ellipse.edoi.ejb.msfx6o.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf875.*;
import com.mincom.ellipse.edoi.ejb.msf710.*;
import com.mincom.ellipse.edoi.ejb.msf076.*;
import com.mincom.ellipse.edoi.ejb.msf062.*;
import com.mincom.ellipse.edoi.ejb.msfx6s.*;
import com.mincom.ellipse.edoi.ejb.msf877.*;
import com.mincom.ellipse.edoi.ejb.msfprt.*;
import com.mincom.ellipse.edoi.ejb.msf542.*;
import com.mincom.ellipse.edoi.ejb.msf966.*;
import com.mincom.ellipse.edoi.ejb.msf920.*;
import com.mincom.ellipse.edoi.ejb.msf930.*;
import com.mincom.ellipse.edoi.ejb.msf93f.*;
import com.mincom.ellipse.edoi.ejb.msf940.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;

import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCompleteReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCompleteRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceSetTextReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceSetTextReplyDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceSetTextRequestDTO;


/**
 * Main Process of Trb62c
 */

public class ProcessTrb62c extends SuperBatch {
    /*
     * Constants
     */
    static final String INPUT_FILE_METERING   = "TRT62C.csv"
    static final int MAX_LENGTH_INPUT         = 920
    static final String SERVICE_WORK_ORDER    = "WORKORDER"
    static final String SERVICE_NAME_STDTEXT  = "STDTEXT"
    static final String COMPLETED_CODE_AW     = "AW"
    static final String COMPLETED_CODE_OK     = "OK"
    static final String STD_TEXT_CODE         = "CW"
    static final String ERR_INVALID_LENGTH    = "SKIPPED - Invalid Length Record"
    static final String ERR_DUPLICATE         = "SKIPPED - Work Order same as PREVIOUS"
    static final String ERR_DATE_FORMAT       = "SKIPPED - INVALID DATE FORMAT SPECIFIED"
    static final String ERR_COMPLETED_BY      = "SKIPPED - INVALID COMPLETED-BY NOT IN MSF810"
    static final String ERR_COMPLETED_CODE    = "SKIPPED - INVALID COMPLETED CODE SPECIFIED"
    static final String ERR_WO_ALREADY_CLOSED = "SKIPPED - WO NUMBER ALREADY CLOSED"
    static final String ERR_WO_NOT_EXIST      = "SKIPPED - WO NUMBER DOES NOT EXIST"
    static final String ERR_DSTRCT_CODE       = "SKIPPED - INVALID ELLIPSE DISTRICT SPECIFIED"
    static final String ERR_CODE_WO           = "0039"
    static final String ERR_CODE_DSTRCT       = "0049"
    static final String ERR_NOT_ACT_DSTRCT    = "5358"
    static final String MSG_PROCESS_SUCCESS   = "PROCESSED SUCCESSFULLY"
    
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 2

    /**
     * Comparator for sorting the input file TRT62C.csv
     * Sort TRT62C based on <code>dstrctCode, workOrder, closedDate, completedBy, completedCode</code>
     */

    public class Trs62CALine implements Comparable<Trs62CALine>{
        private String dstrctCode
        private String workOrder
        private String closedDate
        private String completedBy
        private String completedCode
        private String completedComments

        public Trs62CALine(String dstrctCode, String workOrder,
        String closedDate, String completedBy, String completedCode,
        String completedComments) {
            super()
            this.dstrctCode = dstrctCode
            this.workOrder = workOrder
            this.closedDate = closedDate
            this.completedBy = completedBy
            this.completedCode = completedCode
            this.completedComments = completedComments
        }

        public Trs62CALine() {
            this.dstrctCode = " "
            this.workOrder = " "
            this.closedDate = " "
            this.completedBy = " "
            this.completedCode = " "
            this.completedComments = " "
        }

        public String getDstrctCode() {
            return dstrctCode;
        }
        public void setDstrctCode(String dstrctCode) {
            this.dstrctCode = dstrctCode;
        }
        public String getWorkOrder() {
            return workOrder;
        }
        public void setWorkOrder(String workOrder) {
            this.workOrder = workOrder;
        }
        public String getClosedDate() {
            return closedDate;
        }
        public void setClosedDate(String closedDate) {
            this.closedDate = closedDate;
        }
        public String getCompletedBy() {
            return completedBy;
        }
        public void setCompletedBy(String completedBy) {
            this.completedBy = completedBy;
        }
        public String getCompletedCode() {
            return completedCode;
        }
        public void setCompletedCode(String completedCode) {
            this.completedCode = completedCode;
        }
        public String getCompletedComments() {
            return completedComments;
        }
        public void setCompletedComments(String completedComments) {
            this.completedComments = completedComments;
        }
        @Override
        public int compareTo(Trs62CALine otherLine) {
            // TODO Auto-generated method stub
            if (!dstrctCode.equals(otherLine.dstrctCode)){
                return dstrctCode.compareTo(otherLine.dstrctCode)
            }
            if (!workOrder.equals(otherLine.workOrder)){
                return workOrder.compareTo(otherLine.workOrder)
            }
            if (!closedDate.equals(otherLine.closedDate)){
                return closedDate.compareTo(otherLine.closedDate)
            }
            if (!completedBy.equals(otherLine.completedBy)){
                return completedBy.compareTo(otherLine.completedBy)
            }
            if (!completedCode.equals(otherLine.completedCode)){
                return completedCode.compareTo(otherLine.completedCode)
            }
            return 0
        }
    }

    /*
     * variables
     */
    private def workingDir
    private def ReportA
    private ArrayList arrayOfTrb62c
    private Trs62CALine currLine
    private boolean isMissing
    private String remarksMessage
    private BigDecimal countInput
    private BigDecimal countCompDet
    private BigDecimal countCompCom
    private BigDecimal countError

    /**
     * Run the main batch
     * @param b a<code>Binding</code> object passed from <code>ScriptRunner</code>
     */

    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version)
        info("No input parameters")

        try {
            processBatch()

        } finally {
            generateFooting()
        }
    }


    /**
     * Process the main batch.
     */
    private void processBatch(){
        info("processBatch")
        initialize()
        processMeteringInput()
        if (!isMissing){
            Collections.sort(arrayOfTrb62c)
            processGenerateReport()
        }
    }

    /**
     * Initialize the working directory, Open Report, and other variable
     */
    private void initialize(){
        info("initialize")
        workingDir = env.workDir
        ReportA = report.open("TRB62CA")
        arrayOfTrb62c = new ArrayList()
        isMissing = false
        countInput = 0
        countError = 0
        countCompDet = 0
        countCompCom = 0

        /*
         * Create Header Report
         */
        ReportA.write("WORK ORDER  CLOSED   COMPLETED  COMPLETED  DIST  PROCESSING-REMARKS                              COMPLETED-COMMENTS")
        ReportA.write("  NUMBER     DATE        BY        CODE    CODE")
        ReportA.writeLine(132,"-")
    }

    /**
     * Process input TRT62C.csv <br>
     * <li>Populate the TRT62C.csv records.</li>
     * <li>Store into array list.</li> 
     * <li>Sort the TRT62C.</li>
     */

    private void processMeteringInput(){
        info("processMeteringInputFile")
        String invalidMissingFile = "Invalid or Missing Input File"
        String fileNameTRT62C = "${workingDir}/${INPUT_FILE_METERING}"
        //get uuid
//        String uuid = getUUID()
//        if(uuid?.trim()){
//            fileNameTRT62C = fileNameTRT62C + "." + uuid
//        }
        File meteringInput = new File(fileNameTRT62C)
        info ("Opening:" +"${fileNameTRT62C}")
        if (meteringInput != null && meteringInput.exists()){
            /* 
             * Use buffering, reading one line at a time 
             * FileReader always assumes default encoding is OK!
             */
            BufferedReader input = new BufferedReader(new FileReader(meteringInput))
            try{
                String line = null
                while((line = input.readLine()) != null){
                    countInput++

                    currLine = new Trs62CALine()
                    currLine.dstrctCode = textSubstring(line, 0, 4)
                    currLine.workOrder = textSubstring(line, 4, 12)
                    currLine.closedDate = textSubstring(line, 12, 20)
                    currLine.completedBy = textSubstring(line, 20, 30)
                    currLine.completedCode = textSubstring(line, 30, 32)
                    currLine.completedComments = textSubstring(line, 32, 920)
                    // add line to the array list
                    arrayOfTrb62c.add(currLine)
                }
            }
            finally {
                input.close()
            }
        }
        else{
            ReportA.write(invalidMissingFile.padLeft(78))
            isMissing = true
        }
    }

    /**
     * Extract the array sortlist <br>
     */
    private void processGenerateReport(){
        info("processGenerateReport")
        if (!arrayOfTrb62c.isEmpty()){
            extractArrayReportLine()
        }
    }

    /**
     * Process Extracted records<br>
     * <li>Validate input length records.</li>
     * <li>Validate duplicate records</li>
     * <li>Validate district code and work order records.</li>
     * <li>Validate formatted date of closed date.</li>
     * <li>Validate completed by.</li>
     * <li>Validate completed code, it's must be 'AW' or 'OK'.</li><br>
     * Write error into report and count the error,
     * if validate success close work order and write completion comment.
     */

    private void extractArrayReportLine(){
        info("extractArrayReportLine")
        boolean commentSuccess = true
        Trs62CALine prevRecLine = new Trs62CALine()
        for (Trs62CALine reportLine : arrayOfTrb62c){

            /*
             * input file must be equal to 920 character
             * if not equal write error report and count it
             */
            if (!isValidInputLength(reportLine)){
                remarksMessage = ERR_INVALID_LENGTH
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
                countError++
                continue
            }
            /*
             * if there is records same as previous, write error report
             */
            else if (reportLine.dstrctCode.trim().equals(prevRecLine.dstrctCode.trim()) &&
            reportLine.workOrder.trim().equals(prevRecLine.workOrder.trim()) &&
            reportLine.closedDate.trim().equals(prevRecLine.closedDate.trim()) &&
            reportLine.completedBy.trim().equals(prevRecLine.completedBy.trim()) &&
            reportLine.completedCode.trim().equals(prevRecLine.completedCode.trim())){
                remarksMessage = ERR_DUPLICATE
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
                countError++
                continue
            } else if (!isValidDstrctAndWO(reportLine)){
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
                countError++
                continue
            } else if (!isValidDate(reportLine.closedDate)){
                remarksMessage = ERR_DATE_FORMAT
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
                countError++
                continue
            } else if (isNotInMSF810(reportLine.completedBy)){
                remarksMessage = ERR_COMPLETED_BY
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
                countError++
                continue
            } 
            /*
             * write error if the completed code not 'AW' or 'OK'
             */
            else if (reportLine.completedCode != COMPLETED_CODE_AW && reportLine.completedCode != COMPLETED_CODE_OK){
                remarksMessage = ERR_COMPLETED_CODE
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
                countError++
                continue
            } else{
                String stdTextId = STD_TEXT_CODE + reportLine.dstrctCode + reportLine.workOrder
                if (!reportLine.completedComments.trim().isEmpty()){
                    // create completion comment in MSF096_STD_VOLAT
                    commentSuccess = createCompletionComments(stdTextId,reportLine.completedComments)
                    countCompCom++
                }
                
                if (commentSuccess){
                    if (closeWorkOrder(reportLine, stdTextId)){
                        countCompDet++
                        remarksMessage = MSG_PROCESS_SUCCESS
                    }
                }
                writeRecordsTrb62CA(reportLine)
                prevRecLine = reportLine
            }
        }
    }


    /**
     * validate the input line lengths is 920 characters
     * @param reportLine : records per line
     * @return true or false
     */
    private boolean isValidInputLength(Trs62CALine reportLine){
        info("isValidInputLength")

        String allString = reportLine.dstrctCode + reportLine.workOrder +
                reportLine.closedDate + reportLine.completedBy +
                reportLine.completedCode + reportLine.completedComments

        return allString.length() == MAX_LENGTH_INPUT
    }

    /**
     * validate ellipse district and work order using service call
     * @param reportLine : records input per line
     * @return true or false
     */
    private boolean isValidDstrctAndWO(Trs62CALine reportLine){
        info("isValidDstrctAndWO")
        /*
         * use work order service call read to validate district code and work order
         */
        try{
            WorkOrderDTO workOrder = new WorkOrderDTO(reportLine.workOrder)
            WorkOrderServiceReadReplyDTO workOrderReadReply = service.get(SERVICE_WORK_ORDER).read({WorkOrderServiceReadRequestDTO it ->
                it.districtCode = reportLine.dstrctCode
                it.workOrder = workOrder
            })
            if (workOrderReadReply.workOrderStatusM != "C"){
                return true
            }else{
                remarksMessage = ERR_WO_ALREADY_CLOSED
                return false
            }
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Cannot read record for ${SERVICE_WORK_ORDER} - ${reportLine.workOrder}: ${e.getMessage()}")
            if (e.getMessage().indexOf(ERR_CODE_WO) >= 0){
                remarksMessage = ERR_WO_NOT_EXIST
                return false
            }else if ((e.getMessage().indexOf(ERR_CODE_DSTRCT) >= 0) || (e.getMessage().indexOf(ERR_NOT_ACT_DSTRCT) >= 0)){
                remarksMessage = ERR_DSTRCT_CODE
                return false
            }else{
                remarksMessage = ERR_WO_NOT_EXIST
                return false
            }
        }
    }

    /**
     * validate date format using standard ellipse : yyyyMMdd
     * @param date : String date from the input
     * @return true or false
     */
    private boolean isValidDate(String date){
        info("isValidDate")
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
        format.setLenient(false)
        try{
            format.parse(date)
            return true
        }catch(java.text.ParseException e){
            return false
        }
    }

    /**
     * validate completed by whether exist in MSF810 File or not, using eroi MSSEMP
     * @param completedBy : employee completed by
     * @return true or false
     */
    private boolean isNotInMSF810(String completedBy){
        info("isNotInMSF810")
        try{
            MSSEMPLINK mssemplnk = eroi.execute('MSSEMP',{MSSEMPLINK mssemplnk ->
                mssemplnk.empRecType = "0"
                mssemplnk.employeeId = completedBy
                mssemplnk.securityLevel = "9"
                mssemplnk.commareaInd = "Y"
            })
            return mssemplnk.foundSwEmp == "N"
        }
        catch(Exception e){
            info ("Error at read MSSEMP ${e.getMessage()}")
            return true
        }
    }

    /**
     * close work order after validate the input using service call complete work order
     * @param reportLine : input records per line
     * @param stdTextId : Standard Text Id
     */
    private boolean closeWorkOrder(Trs62CALine reportLine, String stdTextId){
        info("closeWorkOrder")
        try{
            /*
             * Convert string closed date to Calender fist before passing it to service call
             */
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
            Date date = sdf.parse(reportLine.closedDate)
            Calendar closedDate = Calendar.getInstance()
            closedDate.setTime(date)

            /*
             * Use service call to complete work order based on, district, work order, closed date, completed by, completed code
             */
            WorkOrderDTO workOrder = new WorkOrderDTO(reportLine.workOrder)
            WorkOrderServiceCompleteReplyDTO workOrderCompleteReply = service.get(SERVICE_WORK_ORDER).complete({WorkOrderServiceCompleteRequestDTO it ->
                it.districtCode = reportLine.dstrctCode
                it.workOrder   = workOrder
                it.closedDate  = closedDate
                it.completedBy = reportLine.completedBy
                it.completedCode = reportLine.completedCode
            }, false)
            return (workOrderCompleteReply != null && workOrderCompleteReply.workOrderStatusM == "C")
        }
        catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Cannot complete record for ${SERVICE_WORK_ORDER} - ${reportLine.workOrder}: ${e.getMessage()}")
            remarksMessage = e.getMessage
            //rollback std Text if work order fail to closed
            service.get(SERVICE_NAME_STDTEXT).delete({it.setStdTextId(stdTextId)})
            return false
        }
    }
    
    /**
     * create completion comments using service call standart text,
     * first delete the old completion comments and create completion using method setText service call. <br>
     * carriage return indicated by ##. 
     * @param stdTextId : 'CW' + Ellipse District + Work Order Number
     * @param completionComments : completion comment with maximum length 888 char
     */
    private boolean createCompletionComments(String stdTextId, String completionComments){
        info("createCompletionComments")

        try{
            /*
             * delete standard text first before create new
             */
            service.get(SERVICE_NAME_STDTEXT).delete({it.setStdTextId(stdTextId)})
            /*
             * set new standard text for completion comments
             */
            String[] lines = wrapText(completionComments.trim(), 60)

            service.get(SERVICE_NAME_STDTEXT).setText({StdTextServiceSetTextRequestDTO it ->
                it.setStdTextId(stdTextId)
                it.setTextLine(lines)
                it.setLineCount(lines.length)
                it.setTotalCurrentLines(lines.length)
                it.setTotalRetrievedLines(lines.length)
                it.setStartLineNo(0)
            })
            return true
        }
        catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Cannot Create standard text for ${stdTextId}: ${e.getMessage()}")
            remarksMessage = e.getMessage
            return false
        }
    }

    /**
     * <p>Gets a substring from the specified String avoiding exceptions.</p>
     *
     * <p>A negative start position can be used to start/end <code>n</code>
     * characters from the end of the String.</p>
     *
     * <p>The returned substring starts with the character in the <code>start</code>
     * position and ends before the <code>end</code> position. All position counting is
     * zero-based -- i.e., to start at the beginning of the string use
     * <code>start = 0</code>. Negative start and end positions can be used to
     * specify offsets relative to the end of the String.</p>
     *
     * <p>If <code>start</code> is not strictly to the left of <code>end</code>, ""
     * is returned.</p>
     *
     * @param str  the String to get the substring from, may be null
     * @param start  the position to start from, negative means
     *  count back from the end of the String by this many characters
     * @param end  the position to end at (exclusive), negative means
     *  count back from the end of the String by this many characters
     * @return substring from start position to end positon,
     *  <code>null</code> if null String input
     */
    
    public static String textSubstring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }
        // check length next
        if (end > str.length()) {
            end = str.length();
        }
        // if start is greater than end, return ""
        if (start > end) {
            return "";
        }
        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }
        return str.substring(start, end);
    }

    /**
     * This function takes a string value and a line length, and returns an array of lines. <br/>
     * Lines are cut on word boundaries, where the word boundary is a space character. <br/>
     * Spaces are included as the last character of a word, so most lines will actually end with a space. <br/>
     * This isn't too problematic, but will cause a word to wrap if that space pushes it past the max line length.<br/>
     * @see http://progcookbook.blogspot.com/2006/02/text-wrapping-function-for-java.html
     * @param text text to wrap
     * @param len length of a line
     * @return array of String from wrapped text
     */
    private static String[] wrapText(String text, int len) {
        // return empty array for null text
        if (text == null) {
            String[] x = [""]
            return x
        }
        // return text if len is zero or less
        if (len <= 0) {
            String[] x = [text]
            return x
        }

        text = text.replaceFirst('##', '')
        // return text if less than length
        if (text.length() <= len) {
            String[] x = [text]
            return x
        }
        char[] chars = text.toCharArray()
        Vector<String> lines = new Vector<String>()
        StringBuffer line = new StringBuffer()
        StringBuffer word = new StringBuffer()
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '#' && chars[i+1] == '#'){
                line.append(word)
                word.delete(0, word.length())
                lines.add(line.toString())
                line.delete(0, line.length())
                i = i+1
            }else {
                word.append(chars[i])
                if (chars[i] == ' ') {
                    if ((line.length() + word.length()) > len) {
                        lines.add(line.toString())
                        line.delete(0, line.length())
                    }
                    line.append(word)
                    word.delete(0, word.length())
                }
            }
        }
        // handle any extra chars in current word
        if (word.length() > 0) {
            if ((line.length() + word.length()) > len) {
                lines.add(line.toString())
                line.delete(0, line.length())
            }
            line.append(word)
        }
        // handle extra line
        if (line.length() > 0) {
            lines.add(line.toString())
        }
        String[] ret = new String[lines.size()]
        int c = 0 // counter
        for (Enumeration<String> e = lines.elements(); e.hasMoreElements(); c++) {
            ret[c] = e.nextElement()
        }
        return ret
    }

    /**
     * write TRT62CA reocrds into report file/
     * @param reportLine : records perlines
     */
    private void writeRecordsTrb62CA(Trs62CALine reportLine){
        info("writeRecordsTrb62CA")
        String sCompletedComments = reportLine.completedComments.replaceAll("##","").padRight(35)
        ReportA.write(reportLine.workOrder.padRight(12) +
                reportLine.closedDate.padRight(9) +
                reportLine.completedBy.padRight(11) +
                reportLine.completedCode.padRight(11) +
                reportLine.dstrctCode.padRight(6) +
                remarksMessage.padRight(48) +
                sCompletedComments.substring(0,35)
                )
    }

    /**
     * generate summary report :
     * <li>Input Records Read.</li>
     * <li>Records Updated with closed work order.</li>
     * <li>Records Updated with completion comments.</li>
     * <li>Records Error.</li>
     */
    private void generateFooting(){
        info("generateFooting")
        DecimalFormat decFormatter = new DecimalFormat("########0")
        ReportA.writeLine(132,"-")
        ReportA.write("\n")
        ReportA.write(" ".padRight(35) + "Input records read".padRight(50) + (decFormatter.format(countInput)).padLeft(11))
        ReportA.write(" ".padRight(35) + "Records Updated with completion details".padRight(50) + (decFormatter.format(countCompDet)).padLeft(11))
        ReportA.write(" ".padRight(35) + "Records Updated with completion comments".padRight(50) + (decFormatter.format(countCompCom)).padLeft(11))
        ReportA.write(" ".padRight(35) + "Records in Error".padRight(50) + (decFormatter.format(countError)).padLeft(11))
        ReportA.close()
    }
}

/*run script*/  
ProcessTrb62c process = new ProcessTrb62c();
process.runBatch(binding);