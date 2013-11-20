package com.mincom.ellipse.script.custom

import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec
import java.text.SimpleDateFormat

import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_R1_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_R1_801Rec
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Key
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Rec
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec
import com.mincom.ellipse.edoi.ejb.msf891.MSF891AIX1
import com.mincom.ellipse.edoi.ejb.msf891.MSF891Key
import com.mincom.ellipse.edoi.ejb.msf891.MSF891Rec
import com.mincom.ellipse.edoi.ejb.msf8c2.MSF8C2Key
import com.mincom.ellipse.edoi.ejb.msf8c2.MSF8C2Rec
import com.mincom.ellipse.eroi.linkage.mssprd.MSSPRDLINK
import com.mincom.eql.Query
import com.mincom.eql.StringConstraintUnsafe
import com.mincom.eql.impl.QueryImpl



/**
 * List of Input Parameters
 */
public class ParamsTrb89q {
    String payLocation
    String payPeriodsToProcess
    String excludedWorkCode1
    String excludedWorkCode2
    String excludedWorkCode3
    String excludedWorkCode4
    String excludedWorkCode5
    String excludedWorkCode6
    String excludedWorkCode7
    String excludedWorkCode8
    String excludedWorkCode9
    String excludedWorkCode10
}

public class OvertimeReportDTO {
    /**
     * Update this Version number EVERY push to GIT
     */
    private String version = "0001"

    String payLocation
    String employeeId
    String employeeName
    String dayOfTheWeek
    String tranDate
    String workCode
    String fromTime
    String endTime
    String minimumPeriodEndTime
    String tranApprStatus
    String tranReversalStatus
}

public class ProcessTrb89q extends SuperBatch {

    /**
     * Update this Version number every time we push to GIT
     */
    private version = 5
    private ParamsTrb89q batchParams

    private String errField
    private String errValue
    private String errMessages

    private static final String ERR_INVALID_INPUT = "Input must be numeric"
    private static final String ERR_INVALID_PAY_LOCATION = "Invalid pay location"
    private static final String ERR_INPUT_MANDATORY = "Input is mandatory"
    private static final String MINIMUM_INPUT_REQUIRED = "Minimum of one work code should be entered"
    private static final String WORK_CODE_DOES_NOT_EXIST = "The entered work code does not exist in MSF801_R1"

    private static final String PAY_LOCATION = "Pay Location "
    private static final String PAY_PERIODS_TO_PROCESS = "Pay Periods to Process "
    private static final String EXCLUDED_WORK_CODES = "Exclude Travel Time Work Codes "

    private static final String PAY_GROUP = "TG1"

    private ArrayList<String> excludedWorkCodeList = new ArrayList<String>()

    private MSF801_PG_801Rec msf801PGRec = null
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy")

    /**
     * We use LinkedHashSet for two reasons.
     * 
     * 1. To ensure that the records are retrieved in the same order they are inserted.
     * 2. Duplicates are not allowed. If we add duplicates, it will be ignored.
     */
    private LinkedHashMap<String, LinkedHashSet<OvertimeReportDTO>> reportMap = new LinkedHashMap<String, LinkedHashSet<OvertimeReportDTO>>()

    private boolean errorFlag

    private BatchTextReports Trb89qa

    public void runBatch(Binding b) {

        init(b)

        printSuperBatchVersion()
        info("runBatch version : " + version)

        batchParams = params.fill(new ParamsTrb89q())

        /**
         * PrintRequest Parameters
         */
        info("payLocation     		  : " + batchParams.payLocation)
        info("payPeriodsToProcess     : " + batchParams.payPeriodsToProcess)
        info("excludedWorkCode1       : " + batchParams.excludedWorkCode1)
        info("excludedWorkCode2       : " + batchParams.excludedWorkCode2)
        info("excludedWorkCode3       : " + batchParams.excludedWorkCode3)
        info("excludedWorkCode4       : " + batchParams.excludedWorkCode4)
        info("excludedWorkCode5       : " + batchParams.excludedWorkCode5)
        info("excludedWorkCode6       : " + batchParams.excludedWorkCode6)
        info("excludedWorkCode7       : " + batchParams.excludedWorkCode7)
        info("excludedWorkCode8       : " + batchParams.excludedWorkCode8)
        info("excludedWorkCode9       : " + batchParams.excludedWorkCode9)
        info("excludedWorkCode10      : " + batchParams.excludedWorkCode10)

        Trb89qa = report.open('TRB89QA')
        processBatch()
        Trb89qa.close()
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        initialiseExcludedWorkCodeList()
        validateReqParam()
        if (!errorFlag) {
            initialiseMSF801PGRec()
            processOvertimeClaimedWithinMinimumPeriod()
        }
    }

    /**
     * This method initialises the excludedWorkCodeList
     */
    private void initialiseExcludedWorkCodeList() {
        if(containsValue(batchParams.excludedWorkCode1)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode1)
        }
        if(containsValue(batchParams.excludedWorkCode2)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode2)
        }
        if(containsValue(batchParams.excludedWorkCode3)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode3)
        }
        if(containsValue(batchParams.excludedWorkCode4)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode4)
        }
        if(containsValue(batchParams.excludedWorkCode5)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode5)
        }
        if(containsValue(batchParams.excludedWorkCode6)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode6)
        }
        if(containsValue(batchParams.excludedWorkCode7)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode7)
        }
        if(containsValue(batchParams.excludedWorkCode8)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode8)
        }
        if(containsValue(batchParams.excludedWorkCode9)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode9)
        }
        if(containsValue(batchParams.excludedWorkCode10)) {
            excludedWorkCodeList.add(batchParams.excludedWorkCode10)
        }
    }

    /**
     * Validate all request parameters
     */
    private void validateReqParam() {
        info("Validate Pay Location")
        if(containsValue(batchParams.payLocation)) {
            MSF010Key msf010Key = new MSF010Key()
            msf010Key.tableType = "PAYL"
            msf010Key.tableCode = batchParams.payLocation
            MSF010Rec msf010Rec = edoi.findByPrimaryKey(msf010Key)
            if(msf010Rec == null) {
                errField = PAY_LOCATION
                errValue = batchParams.payLocation
                errMessages = ERR_INVALID_PAY_LOCATION
                printErrorMsg()
                errorFlag = true
            }
        }

        info("Validate Pay Periods to Process")
        if(containsValue(batchParams.payPeriodsToProcess)) {
            try {
                Integer.parseInt(batchParams.payPeriodsToProcess)
            } catch(Exception e) {
                errField = PAY_PERIODS_TO_PROCESS
                errValue = batchParams.payPeriodsToProcess
                errMessages = ERR_INVALID_INPUT
                printErrorMsg()
                errorFlag = true
            }
        } else {
            errField = PAY_PERIODS_TO_PROCESS
            errValue = batchParams.payPeriodsToProcess
            errMessages = ERR_INPUT_MANDATORY
            printErrorMsg()
            errorFlag = true
        }

        info("Validate Excluded Travel Time Work Code")
        if(excludedWorkCodeList.size() == 0) {
            errField = EXCLUDED_WORK_CODES
            errValue = ""
            errMessages = MINIMUM_INPUT_REQUIRED
            printErrorMsg()
            errorFlag = true
        } else {
            for(String excludedWorkCode : excludedWorkCodeList) {
                String cntlKeyRest = "***" + excludedWorkCode
                Query query = new QueryImpl(MSF891Rec.class)
                query.and(MSF801_R1_801Key.cntlKeyRest.equalTo(cntlKeyRest))
                List queryResults = edoi.search(query).getResults()
                if(queryResults.size() == 0) {
                    errField = EXCLUDED_WORK_CODES
                    errValue = excludedWorkCode
                    errMessages = WORK_CODE_DOES_NOT_EXIST
                    printErrorMsg()
                    errorFlag = true
                    break
                }
            }
        }
    }

    /**
     * This method prints the error message to the report.
     */
    private void printErrorMsg() {
        info("printErrorMsg")
        Trb89qa.write((errField + (errValue?.trim() ? "/":"") + errValue).padRight(40) + errMessages )
    }

    /**
     * This method generates the exception report for all the 
     */
    private void processOvertimeClaimedWithinMinimumPeriod() {
        List queryResults = edoi.search(getMSF891SearchQuery()).getResults()
        LinkedHashSet<OvertimeReportDTO> filteredList = getFilteredList(queryResults)
        addRecordsToBeReported(filteredList)
        generateExceptionReport()
    }

    /**
     * This method returns the search query for MSF891 based on the user entered search criteria. 
     * 
     * @return query
     */
    private Query getMSF891SearchQuery() {
        String periodEndDate = msf801PGRec.getCurEndDtPg()
        String periodStartDate = getMSSPRDLink(periodEndDate).getStartDate()

        Query subQuery = new QueryImpl()
        subQuery.and(MSF801_R1_801Key.cntlRecType.equalTo("R1"))
        subQuery.and(MSF801_R1_801Rec.shiftTypeR1.equalTo("V").or(MSF801_R1_801Rec.shiftTypeR1.equalTo("C")))

        Query query = new QueryImpl(MSF891Rec.class)
        query.and(MSF891Key.trnDate.greaterThanEqualTo(periodStartDate))
        query.and(MSF891Key.trnDate.lessThanEqualTo(periodEndDate))
        query.and(MSF891Rec.workCode.in(subQuery, ((StringConstraintUnsafe) MSF801_R1_801Key.cntlKeyRest).substring(4, 2)))

        for(String excludedWorkCode : excludedWorkCodeList) {
            query.and(MSF891Rec.workCode.notEqualTo(excludedWorkCode))
        }

        query.and(MSF891Rec.tranApprStatus.equalTo("APPR")
                .or(MSF891Rec.tranApprStatus.equalTo("PAID").and(MSF891Rec.reverseStatus.equalTo("RPLD")))
                .or(MSF891Rec.tranApprStatus.equalTo("PAID").and(MSF891Rec.reverseStatus.equalTo("RVSD"))))

        if(containsValue(batchParams.payLocation)) {
            query.and(MSF820Rec.payLocation.equalTo(batchParams.payLocation))
        }

        query.join(MSF891Key.employeeId, MSF820Key.employeeId)
        query.orderBy(new MSF891AIX1())
        return query
    }

    /**
     * This method filters the MSF891 records retrieved by the search query and returns only the valid records.
     * 
     * @return LinkedHashSet<OvertimeReportDTO>
     */
    private LinkedHashSet<OvertimeReportDTO> getFilteredList(List queryResults) {
        LinkedHashSet<OvertimeReportDTO> filteredList = new LinkedHashSet<OvertimeReportDTO>()
        Iterator iterator = queryResults.iterator()
        while (iterator.hasNext()) {
            Object[] objects = (Object[]) iterator.next()
            MSF891Rec msf891Rec = (MSF891Rec) objects[0]
            MSF820Rec msf820Rec = (MSF820Rec) objects[1]

            OvertimeReportDTO dto = new OvertimeReportDTO()
            dto.setPayLocation(msf820Rec.getPayLocation())
            dto.setEmployeeId(msf891Rec.getPrimaryKey().getEmployeeId())
            dto.setEmployeeName(getEmployeeName(msf891Rec.getPrimaryKey().getEmployeeId()))
            dto.setDayOfTheWeek(getDayOfTheWeek(msf891Rec.getPrimaryKey().getTrnDate()))
            dto.setTranDate(msf891Rec.getPrimaryKey().getTrnDate())
            dto.setWorkCode(msf891Rec.getWorkCode())
            dto.setFromTime(msf891Rec.getFromTime().toString())
            dto.setEndTime(msf891Rec.getEndTime().toString())
            dto.setTranApprStatus(msf891Rec.getTranApprStatus())
            dto.setTranReversalStatus(msf891Rec.getReverseStatus())

            if(dto.getTranApprStatus().equals("APPR")) {
                filteredList.add(dto)
            } else if(dto.getTranApprStatus().equals("PAID") && dto.getTranReversalStatus().equals("RPLD")) {
                /**
                 * Check if an corresponding APPR transaction for the same work code exists on the same date.
                 */
                Query dateQuery = new QueryImpl(MSF891Rec.class)
                dateQuery.and(MSF891Rec.workCode.equalTo(dto.getWorkCode()))
                dateQuery.and(MSF891Key.trnDate.equalTo(dto.getTranDate()))
                dateQuery.and(MSF891Rec.tranApprStatus.equalTo("APPR"))
                List dateQueryResults = edoi.search(dateQuery).getResults()
                if(dateQueryResults.size() > 0) {
                    filteredList.add(dto)
                }
            } else if(dto.getTranApprStatus().equals("PAID") && dto.getTranReversalStatus().equals("RVSD")) {
                /**
                 * To calculate inverse run number, select SYS_PRD_NO_PG from msf801_pg_801.
                 * Subtract that value from 999 and add 1 to calculate inverse run number.
                 */
                int sysPrdNo = Integer.valueOf(msf801PGRec.getSysPrdNoPg())
                int inverseRunNo = 999 - sysPrdNo + 1

                Query query1 = new QueryImpl(MSF817Rec.class)
                query1.and(MSF817Key.payGroup.equalTo(PAY_GROUP))
                query1.and(MSF817Key.payRunType.equalTo("U"))
                query1.and(MSF817Key.invRunNo.equalTo(String.valueOf(inverseRunNo)))
                query1.and(MSF817Rec.runDate.lessThan(msf891Rec.getLastModDate())
                        .or(MSF817Rec.runDate.equalTo(msf891Rec.getLastModDate()).and(MSF817Rec.runTime.lessThan(msf891Rec.getLastModTime()))))
                List queryResults1 = edoi.search(query1).getResults()
                if(queryResults1.size() > 0) {
                    filteredList.add(dto)
                }
            }
        }
        return filteredList
    }

    /**
     * This method adds the records that needs to be added to the report.
     * 
     * @param filteredRecMap
     */
    private void addRecordsToBeReported(LinkedHashSet<OvertimeReportDTO> filteredList) {
        OvertimeReportDTO previousDTO = null
        for(OvertimeReportDTO dto : filteredList) {
            if(previousDTO == null) {
                previousDTO = dto
                continue
            }
            if(!previousDTO.getEmployeeId().equals(dto.getEmployeeId())) {
                previousDTO = dto
                continue
            }
            String minimumHours = getMinimumHours(previousDTO.getWorkCode())
            String previousTranDate = previousDTO.getTranDate()
            String previousFromTime = previousDTO.getFromTime()
            Date shiftStartDate = getShiftStartDate(previousTranDate, previousFromTime)
            Date shiftEndDate = addMinimumHours(shiftStartDate, minimumHours)

            String nextTranDate = dto.getTranDate()
            String nextFromTime = dto.getFromTime()
            Date nextShiftStartDate = getShiftStartDate(nextTranDate, nextFromTime)

            if(nextShiftStartDate.compareTo(shiftEndDate) < 0) {
                previousDTO.setMinimumPeriodEndTime(shiftEndDate.toString().subSequence(11, 16))

                Date nextShiftEndDate = addMinimumHours(nextShiftStartDate, minimumHours)
                dto.setMinimumPeriodEndTime(nextShiftEndDate.toString().subSequence(11, 16))

                addDTOToReportMap(previousDTO)
                addDTOToReportMap(dto)
            }
            previousDTO = dto
        }
    }

    /**
     * This method adds the DTO to the reportMap based on the pay location.
     * 
     * @param dto
     */
    private void addDTOToReportMap(OvertimeReportDTO dto) {
        LinkedHashSet<OvertimeReportDTO> reportList = reportMap.get(dto.getPayLocation())
        if(reportList == null) {
            reportList = new LinkedHashSet<OvertimeReportDTO>()
            reportMap.put(dto.getPayLocation(), reportList)
        }
        reportList.add(dto)
    }

    /**
     * This method generated the exception report based on the information retrieved.
     */
    private void generateExceptionReport() {
        int count = 0
        for (String payLocation : reportMap.keySet()) {
            count++
            LinkedHashSet<OvertimeReportDTO> reportList = reportMap.get(payLocation)
            Trb89qa.write("Pay Location : " + payLocation + "                Pay Period : " + getFormattedDate(msf801PGRec.getCurEndDtPg()))
            Trb89qa.columns = [
                "EmployeeID ",
                "Employee Name       ",
                "Day ",
                "Date       ",
                "Work Code ",
                "Start Time",
                "Stop Time ",
                "Minimum Period End",
                "Status"
            ]
            for(OvertimeReportDTO dto : reportList) {
                String fromTime = getHoursAndMinutes(dto.getFromTime())
                String endTime = getHoursAndMinutes(dto.getEndTime())
                String status = dto.getTranApprStatus()
                if(containsValue(dto.getTranReversalStatus())) {
                    status += "/" + dto.getTranReversalStatus()
                }

                Trb89qa.write(
                        padSpaceString(dto.getEmployeeId(), 10),
                        padSpaceString(dto.getEmployeeName(), 20),
                        dto.getDayOfTheWeek() + " ",
                        getFormattedDate(dto.getTranDate()) + "  ",
                        dto.getWorkCode() + "        ",
                        fromTime + "      ",
                        endTime + "     " ,
                        dto.getMinimumPeriodEndTime() + "              ",
                        status)
            }
            if(count < reportMap.size()) {
                Trb89qa.write("\n\n")
            }
        }
    }

    /**
     * This method initialises MSF801_PG record for pay group TG.
     */
    private void initialiseMSF801PGRec() {
        MSF801_PG_801Key msf801Key = new MSF801_PG_801Key()
        msf801Key.setCntlKeyRest(PAY_GROUP)
        msf801Key.setCntlRecType("PG")
        msf801PGRec = edoi.findByPrimaryKey(msf801Key)
    }

    /**
     * This method searches MSF8C2 and returns minimum hours for the work code.  
     * 
     * @param workCode
     * @return minimumHours
     */
    private String getMinimumHours(String workCode) {
        String minimumHours = ""
        Query query = new QueryImpl(MSF8C2Rec.class)
        query.and(MSF8C2Key.workCodeC2.equalTo(workCode))
        List queryResults = edoi.search(query).getResults()
        if(queryResults.size() > 0) {
            MSF8C2Rec msf8C2Rec = queryResults.get(0)
            minimumHours = msf8C2Rec.getMinHrsPdC2()
        }
        return minimumHours
    }

    /**
     * This method invokes the MSSPRD subroutine with option 4 to return 
     * pay period details for a selected date +/- a Number of Periods
     * 
     * @param periodEndDate
     * @return MSSPRDLINK
     */
    private MSSPRDLINK getMSSPRDLink(String periodEndDate) {
        MSSPRDLINK mssprdLink = eroi.execute("MSSPRD", { MSSPRDLINK mssprdLink ->
            mssprdLink.optionPrd = "4"
            mssprdLink.payGroup = PAY_GROUP
            mssprdLink.reqDate = periodEndDate
            mssprdLink.reqPeriods = Integer.valueOf(batchParams.payPeriodsToProcess) * -1
        })
    }

    /**
     * This method returns date object based on the transaction time and shift start from time.
     * 
     * @param tranDate
     * @param fromTime
     * @return Date
     */
    private Date getShiftStartDate(String tranDate, String fromTime) {
        int year = Integer.valueOf(tranDate.substring(0, 4))
        int month = Integer.valueOf(tranDate.substring(4, 6)) - 1
        int day = Integer.valueOf(tranDate.substring(6, 8))

        int hours = getHoursFromTime(fromTime)
        int minutes = getMinutesFromTime(fromTime)

        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.YEAR, year)

        return calendar.getTime()
    }

    /**
     * This method adds the minimum hours to the shift start date and returns the date.
     * 
     * @param shiftStartDate
     * @param minimumHours
     * @return Date
     */
    private Date addMinimumHours(Date shiftStartDate, String minimumHours) {
        int minHours = getHoursFromTime(minimumHours)
        int minMinutes = getMinutesFromTime(minimumHours)

        Calendar calendar = Calendar.getInstance()
        calendar.setTime(shiftStartDate)
        calendar.add(Calendar.HOUR_OF_DAY, minHours)
        calendar.add(Calendar.MINUTE, minMinutes)
        return calendar.getTime()
    }

    /**
     * This method retrieves the employee name from MSF810 based on the Employee ID.
     * 
     * @param employeeId
     * @return employeeName
     */
    private String getEmployeeName(String employeeId) {
        MSF810Key msf810Key = new MSF810Key()
        msf810Key.employeeId = employeeId
        MSF810Rec msf810Rec = edoi.findByPrimaryKey(msf810Key)
        return msf810Rec.getSurname() + ", " + msf810Rec.getFirstName()
    }

    /**
     * This method returns the date in the DD/MM/YY format.
     * 
     * @param date
     * @return formattedDate
     */
    private String getFormattedDate(String date) {
        int year = Integer.valueOf(date.substring(0, 4))
        int month = Integer.valueOf(date.substring(4, 6)) - 1
        int day = Integer.valueOf(date.substring(6, 8))

        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.YEAR, year)

        return sdf.format(calendar.getTime())
    }

    /**
     * This method returns the day of the week in three characters (MON, TUE, etc).
     * 
     * @param tranDate
     * @return dayOfTheWeek
     */
    private String getDayOfTheWeek(String tranDate) {
        int year = Integer.valueOf(tranDate.substring(0, 4))
        int month = Integer.valueOf(tranDate.substring(4, 6)) - 1
        int day = Integer.valueOf(tranDate.substring(6, 8))

        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.YEAR, year)

        return calendar.getTime().toString().subSequence(0, 3)
    }

    /**
     * This method parses the provided time attribute and returns hours (HH).
     * 
     * @param time
     * @return hours
     */
    private int getHoursFromTime(String time) {
        StringTokenizer tokenizer = new StringTokenizer(time, ".")
        return Integer.valueOf(tokenizer.nextToken())
    }

    /**
     * This method parses the provided time attribute and returns minutes (MM).
     * 
     * @param time
     * @return hours
     */
    private int getMinutesFromTime(String time) {
        StringTokenizer tokenizer = new StringTokenizer(time, ".")
        tokenizer.nextToken()
        int minutes = 0
        if (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken()
            if (token.length() < 2) {
                minutes = Integer.valueOf(token) * 10
            } else {
                minutes = Integer.valueOf(token)
            }
        }
        return minutes
    }

    /**
     * This method returns hours and minutes in the HH:MM format.
     * 
     * @param time
     * @return time
     */
    private String getHoursAndMinutes(String time) {
        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, getHoursFromTime(time))
        calendar.set(Calendar.MINUTE, getMinutesFromTime(time))
        return calendar.getTime().toString().subSequence(11, 16).toString()
    }

    /**
     * This method pads space with space to form a string.
     * 
     * @param value
     * @param length
     * @return result
     */
    private String padSpaceString(String value, int length) {
        StringBuffer result = new StringBuffer(value)
        for (int i = 0; i < length - value.length(); i++) {
            result.append(' ')
        }
        return result.toString()
    }

    /**
     * This method returns true if the entered string contains value.
     * 
     * @return boolean
     */
    private boolean containsValue(String value) {
        if(value != null && value.trim().length() > 0) {
            return true
        }
        return false
    }
}

/**
 * Run Script
 */
ProcessTrb89q process = new ProcessTrb89q()
process.runBatch(binding)

