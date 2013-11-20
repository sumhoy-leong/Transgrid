/**
 *  @Ventyx 2012
 *  Conversion from trb53a.groovy
 *  
 *  This program will generate the Workers Compensation Lost Time Statistics Report. <br>
 *  
 *  Revision based on <b>URS.Batch.TRB53A.WC.LTI.Statistics.Report.V01.docx</b>
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;

import org.slf4j.LoggerFactory;

import com.mincom.ellipse.edoi.ejb.msf012.*;
import com.mincom.ellipse.edoi.ejb.msf510.*;
import com.mincom.ellipse.edoi.ejb.msf511.*;
import com.mincom.ellipse.edoi.ejb.msf514.*;
import com.mincom.ellipse.edoi.ejb.msf536.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.eql.impl.QueryImpl;


/**
 * Request Parameters for Trb53a.
 * <li><code>dateOccFrom</code> : Date Occurrence From</li> 
 * <li><code>dateOccTo</code> : Date Occurrence To</li> 
 */
public class ParamsTrb53a {
    String dateOccFrom, dateOccTo
}

/**
 * Report content for Trb53a.
 */
public class RecordTrb53a {
    String periodDate, surName, empId, incNo, claimNo, natureOfInjury, desc1,
    status, claimLiab1, fName, incDate, desc2, claimLiab2;
    int[] monthTables = new int[12]

    public RecordTrb53a() {
        periodDate = surName = empId = incNo = claimNo = natureOfInjury = desc1 = ""
        status = claimLiab1 = fName = incDate = desc2 = claimLiab2 = ""
        (0..11).each { monthTables[it] = 0 }
    }
    /**
     * Return report detail at the first line.
     * @return report detail at the first line
     */
    public String getReportDetailLine1() {
        return String.format("%-15s %-5s %-10s %-10s %-15s %-20s %-10s %-10s  %3s %3s %3s %3s %3s %3s",
        surName.padRight(15).substring(0,15),
        empId.padRight(5).substring(0,5),
        incNo.padRight(10).substring(0,10),
        claimNo.padRight(10).substring(0,10),
        natureOfInjury.padRight(15).substring(0,15),
        desc1.padRight(20).substring(0,20),
        status.padRight(10).substring(0,10),
        claimLiab1.padRight(10).substring(0,10),
        monthTables[0], monthTables[1], monthTables[2],
        monthTables[3], monthTables[4], monthTables[5])
    }

    /**
     * Return report detail at the second line.
     * @return report detail at the second line
     */
    public String getReportDetailLine2(int accTotal) {
        return String.format("%-15s %-5s %-10s %-10s %-15s %-20s %-10s %-10s  %3s %3s %3s %3s %3s %3s %4s",
        fName.padRight(15).substring(0,15),
        "",
        incDate.padRight(10).substring(0,10),
        "",
        natureOfInjury.padRight(30).substring(15,30),
        desc2.padRight(20).substring(0,20),
        status.padRight(20).substring(10,20),
        claimLiab2.padRight(10).substring(0,10),
        monthTables[6], monthTables[7], monthTables[8],
        monthTables[9], monthTables[10], monthTables[11],
        accTotal.toString())
    }
}

/**
 * Data type for Days table. <br>
 * Contains month and days.
 */
public class DaysTable {
    int month, days
}

/**
 * Main Process of Trb53a.
 */
public class ProcessTrb53a extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME   = "TRB53AA"
    private static final String TABLE_SERVICE = "TABLE"
    private static final String MSF010_OHIC   = "OHIC"
    private static final String MSF010_WCCS   = "WCCS"
    private static final String MSF010_WCCL   = "WCCL"
    private static final String MSF010_SCAT   = "SCAT"
    private static final String EMP_CLASS_P   = "P"
    private static final String EMP_CLASS_F   = "F"
    private static final String MSF012_DATA_TYPE_M = "M"
    private static final String PERSON_IND_E       = "E"
    private static final String PRIMARY_ROLE_IV    = "IV"
    private static final String DASHED_LINE   = String.format("%132s"," ").replace(' ', '-')

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private int version = 5

    /*
     * Variables
     */
    private def reportWriter
    private ParamsTrb53a batchParams
    private int[] accLostDays
    private int totalRecord = 0, daysLost = 0, totalDaysLost = 0
    private boolean aborted = false, saveDate = true
    private def msf801StdHoursDayC0

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version : " + version);
        //Get request parameters
        batchParams = params.fill(new ParamsTrb53a())
        info("Date Occurance From : ${batchParams.dateOccFrom}")
        info("Date Occurance To   : ${batchParams.dateOccTo}")
        try {
            processBatch()
        } catch(Exception e) {
            aborted = true
            def logObject = LoggerFactory.getLogger(getClass());
            logObject.trace("------------- TRB53A ERROR TRACE ", e)
            info("Process terminated. ${e.getMessage()}")
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
        browseIncidents()
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        if(!aborted) {
            writeReportFooter()
        }
        reportWriter.close()
    }

    /**
     * Initialize the CSV writer, days type and other variables.
     */
    private void initialize() {
        info("initialize")
        //open report writer
        String periodDate = convertDateFormat(batchParams.dateOccFrom, "/") +
                "-" + convertDateFormat(batchParams.dateOccTo, "/")
        reportWriter = report.open(REPORT_NAME)
        reportWriter.write("Workers Compensation Lost Time Statistic Report".center(132))
        reportWriter.write(String.format("Period Date : %s", periodDate).center(132))
        reportWriter.write("")
        reportWriter.write(DASHED_LINE)
        reportWriter.write("Surname         EmpId -Incident- Claim      ---Nature of--- "+
                "- Short Description- Status     --Claim--   Jan Feb Mar Apr May Jun  Acc")
        reportWriter.write("First Name             No./Date  No            Injury       "+
                "                                Liability   Jul Aug Sep Oct Nov Des  Tot")
        reportWriter.write(DASHED_LINE)

        //initialize lostDays and accLostDays
        int mmFrom = Integer.parseInt(batchParams.dateOccFrom.substring(4,6))
        accLostDays = new int[12]
        (0..11).each { accLostDays[it] = 0 }
    }

    /**
     * Browse the incidents records, where the occurance date between date from/to <br/>
     * and the hours lost greater than 0.
     */
    private void browseIncidents() {
        info("browseIncidents")
        String revFromDate = reverseDate(batchParams.dateOccFrom as int)
        String revToDate   = reverseDate(batchParams.dateOccTo   as int)
        QueryImpl qMSF510 = new QueryImpl(MSF510Rec.class).
                and(MSF510Key.dstrctCode.greaterThanEqualTo(" ")).
                and(MSF510Key.incidentNo.greaterThanEqualTo(" ")).
                and(MSF510Rec.revOccDate.greaterThanEqualTo(revToDate)).
                and(MSF510Rec.revOccDate.lessThanEqualTo(revFromDate)).
                and(MSF510Rec.hoursLost.greaterThan(new BigDecimal(0)))

        edoi.search(qMSF510) {MSF510Rec incident->
            browseIncidentPeople(incident)
        }
    }

    /**
     * Browse incident people based on incident detail.
     * @param incident MSF510Rec
     */
    private void browseIncidentPeople(MSF510Rec incident) {
        info("browseIncidentPeople")
        QueryImpl qMSF511 = new QueryImpl(MSF511Rec.class).
                and(MSF511Key.dstrctCode.equalTo(incident.getPrimaryKey().getDstrctCode())).
                and(MSF511Key.incidentNo.equalTo(incident.getPrimaryKey().getIncidentNo())).
                and(MSF511Key.employeeId.greaterThanEqualTo(" ")).
                and(MSF511Key.personInd.equalTo(PERSON_IND_E)).
                and(MSF511Rec.primaryRole.equalTo(PRIMARY_ROLE_IV))

        edoi.search(qMSF511) {MSF511Rec incidentPeople->
            MSF820Rec empPayroll = readEmployeePayroll(
                    incidentPeople.getPrimaryKey().getEmployeeId())
            if(empPayroll) {
                browseClaims(incident, incidentPeople, empPayroll)
            }
        }
    }

    /**
     * Read Employee Payroll based on employee id
     * @param employeeId employee id
     * @return MSF820Rec
     */
    private MSF820Rec readEmployeePayroll(String empId) {
        info("readEmployeePayroll")
        try {
            return edoi.findByPrimaryKey(new MSF820Key(empId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            info("MSF820 Employee ${empId} does not exist")
            return null
        }
    }

    /**
     * Read the Incident Injury based on incident people.
     * @param incidentPeople MSF511Rec
     * @return MSF514Rec
     */
    private MSF514Rec readIncidentInjury(MSF511Rec incidentPeople) {
        info("readIncidentInjury")
        String personData = incidentPeople.getPrimaryKey().getPersonInd().trim().concat(
                incidentPeople.getPrimaryKey().getEmployeeId())
        QueryImpl qMSF514 = new QueryImpl(MSF514Rec.class).
                and(MSF514Key.incidentNo.equalTo(incidentPeople.getPrimaryKey().getIncidentNo())).
                and(MSF514Key.personData.equalTo(personData))
        return edoi.firstRow(qMSF514)
    }

    /**
     * Read Employee based on employee id
     * @param employeeId employee id
     * @return Employee MSF810Rec
     */
    private MSF810Rec readEmployee(String employeeId) {
        try {
            return edoi.findByPrimaryKey(new MSF810Key(employeeId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF810 Employee ${employeeId} does not exist")
            return null
        }
    }

    /**
     * Read Table based on table type and table code.
     * @param tableType table type
     * @param tableCode table code
     * @return TableServiceReadReplyDTO if exist, null if doesn't exist
     */
    private TableServiceReadReplyDTO readTable(String tableType, String tableCode) {
        info("readTable")
        TableServiceReadRequiredAttributesDTO tableReqAttributeDTO = new TableServiceReadRequiredAttributesDTO()
        tableReqAttributeDTO.setReturnTableCode(true)
        tableReqAttributeDTO.setReturnTableType(true)
        tableReqAttributeDTO.setReturnAssociatedRecord(true)
        tableReqAttributeDTO.setReturnDescription(true)

        try {
            TableServiceReadRequestDTO tableRequestDTO = new TableServiceReadRequestDTO()
            tableRequestDTO.setTableType(tableType)
            tableRequestDTO.setTableCode(tableCode)
            tableRequestDTO.setRequiredAttributes(tableReqAttributeDTO)

            return service.get(TABLE_SERVICE).read(tableRequestDTO, false)
        } catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
            String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = serviceExc.getErrorMessages()[0].getFieldName()
            info("Cannot read ${TABLE_SERVICE} - ${tableType}, ${tableCode} caused by ${errorField}: ${errorCode} - ${errorMsg}.")
            return null
        }
    }

    /**
     * Browse the claims based on the incident people.</br>
     * Report all claims for a lost time incident.
     * @param incident MSF510Rec
     * @param incPeople MSF511Rec
     * @param empPayroll MSF820Rec
     */
    private void browseClaims(MSF510Rec incident, MSF511Rec incPeople, MSF820Rec empPayroll) {
        info("browseClaims")
        //report all claims for a lost time incident
        String employeeId = incPeople.getPrimaryKey().getEmployeeId()
        QueryImpl qClaim = new QueryImpl(MSF536Rec.class).
                and(MSF536Key.dstrctCode.equalTo("GRID")).
                and(MSF536Key.claimNo.greaterThanEqualTo(" ")).
                and(MSF536Rec.incidentNo.equalTo(incPeople.getPrimaryKey().getIncidentNo())).
                and(MSF536Rec.claimEntity.equalTo(employeeId)).
                and(MSF536Rec.claimInd.equalTo("E"))

        edoi.search(qClaim) {MSF536Rec claim->
            writeReport(incident, incPeople, empPayroll, claim)
        }
    }

    /**
     * Write the procesed records into report.
     * @param incident MSF510Rec
     * @param incPeople MSF511Rec
     * @param empPayroll MSF820Rec
     * @param claim MSF536Rec
     */
    private writeReport(MSF510Rec incident, MSF511Rec incPeople, MSF820Rec empPayroll, MSF536Rec claim) {
        info("writeReport")
        String employeeId    = incPeople.getPrimaryKey().getEmployeeId()
        MSF514Rec incInjury  = readIncidentInjury(incPeople)
        MSF810Rec empRec     = readEmployee(employeeId)
        RecordTrb53a reportLine = new RecordTrb53a()
        daysLost = 0
        //get employee detail
        reportLine.empId   = employeeId.padRight(10).substring(5)
        reportLine.surName = empRec?.getSurname()
        reportLine.fName   = empRec?.getFirstName()
        //get injury date
        int revOccDate = reverseDate(incident.getRevOccDate() as int)
        String revDate = revOccDate.toString()
        reportLine.incDate = convertDateFormat(revDate, "/")
        reportLine.claimNo = claim.getPrimaryKey().getClaimNo()
        //get nature of injury detail
        reportLine.incNo = incident.getPrimaryKey().getIncidentNo()
        reportLine.desc1 = incident.getIncidentDesc().padRight(40).substring(0, 20)
        reportLine.desc2 = incident.getIncidentDesc().padRight(40).substring(20)
        TableServiceReadReplyDTO tableReplyDto = readTable(MSF010_OHIC,
                incInjury?.getPrimaryKey()?.getNatureInjury())
        if(tableReplyDto) {
            reportLine.natureOfInjury = tableReplyDto.getDescription()
        }
        //get claim status
        tableReplyDto = readTable(MSF010_WCCS, claim?.getClaimStatus())
        if(tableReplyDto) {
            reportLine.status = tableReplyDto.getDescription()
        }
        //get liab status
        tableReplyDto = readTable(MSF010_WCCL, readClaimLiabStatus(claim))
        if(tableReplyDto) {
            reportLine.claimLiab1 = tableReplyDto.getDescription().padRight(50).substring(0, 10)
            reportLine.claimLiab2 = tableReplyDto.getDescription().padRight(50).substring(10)
        }
        //total hours calculation
        int totalHrsLost = calculateTotalHoursLost(empPayroll, incident)
        //Put it into record
        int mm = Integer.parseInt(revDate.substring(4,6)) - 1
        reportLine.monthTables[mm] = totalHrsLost
        daysLost += totalHrsLost
        totalDaysLost += totalHrsLost
        accLostDays[mm] += totalHrsLost
        totalRecord++
        reportWriter.write("")
        reportWriter.write(reportLine.getReportDetailLine1())
        reportWriter.write(reportLine.getReportDetailLine2(daysLost))
    }

    /**
     * Calculate the total hours lost based on Employee Payroll.
     * @param empPayroll MSF820Rec
     * @param incident MSF510Rec
     * @return total hours lost
     */
    private int calculateTotalHoursLost(MSF820Rec empPayroll, MSF510Rec incident) {
        info("calculateTotalHoursLost")
        String empClass = empPayroll.getEmployeeClass()
        double empStdHours  = 0
        int totalHrsLost    = 0
        switch(empClass) {
            case EMP_CLASS_P:
                empStdHours = empPayroll.getContractHours().doubleValue() / 35 * 7
                break
            case EMP_CLASS_F:
                TableServiceReadReplyDTO tableReplyDto = readTable(MSF010_SCAT,
                empPayroll.getShiftCat().trim())
                if(tableReplyDto) {
                    String assocRec = tableReplyDto.getAssociatedRecord()
                    String stdHrs = assocRec.substring(6, 11)
                    BigDecimal bd = new BigDecimal(stdHrs).divide(100)
                    BigDecimal convValueint = (long) (bd / 1)
                    BigDecimal convUnitsDec = bd - convValueint
                    BigDecimal convValue    = (convUnitsDec * 100) / 60
                    empStdHours = convValue + convValueint
                }
                break
        }
        if(incident?.getHoursLost() > 0 && empStdHours > 0) {
            totalHrsLost = Math.floor(incident.getHoursLost() / empStdHours)
        }
        return totalHrsLost
    }

    /**
     * Read Claim Liability status.
     * @param claim MSF536Rec
     * @return
     */
    private String readClaimLiabStatus(MSF536Rec claim) {
        info("readClaimLiabStatus")
        //Key concatenation from MSF536 + District + Claim No
        StringBuilder key = new StringBuilder()
        key.append("MSF536")
        key.append(claim?.getPrimaryKey()?.getDstrctCode()?.trim())
        key.append(claim?.getPrimaryKey()?.getClaimNo()?.trim())
        try {
            MSF012Rec rec = edoi.findByPrimaryKey(new MSF012Key(dataType:MSF012_DATA_TYPE_M,
                    keyValue:key.toString()))
            return rec.getDataArea().padRight(934).substring(19, 21)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF012Rec - ${key} does not exist")
            return ""
        }
    }

    /**
     * Write the report footer.
     */
    private void writeReportFooter() {
        info("writeReportFooter")
        reportWriter.write("")
        reportWriter.write(DASHED_LINE)
        reportWriter.write(String.format("%-25s%-25s   %7s%-28s%-14s  %3s %3s %3s %3s %3s %3s",
                " ", "Total Number of Records :", totalRecord.toString(), " ", "Month Total : ",
                accLostDays[0], accLostDays[1], accLostDays[2],
                accLostDays[3], accLostDays[4], accLostDays[5]))
        reportWriter.write(String.format("%-25s%-25s   %7s%-28s%-14s  %3s %3s %3s %3s %3s %3s %4s",
                " ", " ", " ", " ", " ",
                accLostDays[6], accLostDays[7], accLostDays[8],
                accLostDays[9], accLostDays[10], accLostDays[11],
                totalDaysLost))
        reportWriter.write(DASHED_LINE)
    }

    //Utility methods - starts here
    /**
     * Convert the date format with specified separator <code>(YY/MM/DD)</code>.
     * @param dateS date as a String
     * @param separator separator for the
     * @return formatted date as a String
     */
    private String convertDateFormat(String dateS, String separator) {
        info("convertDateFormat")
        dateS = dateS.padRight(8)
        if(!separator?.trim()) {
            separator = "/"
        }
        def formattedString = dateS.substring(6) + separator + dateS.substring(4,6) + separator + dateS.substring(2,4)
        return formattedString
    }

    /**
     * Reverse date. The formula is: <code>99999999 - date</code>
     * @param date specified date
     * @return reversed date
     */
    private int reverseDate(int date) {
        info("computeDateDiff")
        return 99999999 - date
    }
    //End of - Utility methods
}

/**
 * Run the script
 */
ProcessTrb53a process = new ProcessTrb53a()
process.runBatch(binding)