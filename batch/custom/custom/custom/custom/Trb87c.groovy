/**
 * @Ventyx 2012
 * Conversion from trb87c.cbl
 *
 * This program will produce a report and a file which contains the details of authority.
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.io.BufferedWriter;
import java.util.Comparator;

import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Key;
import com.mincom.ellipse.edoi.ejb.msf870.MSF870Rec;
import com.mincom.ellipse.edoi.ejb.msf872.MSF872Key;
import com.mincom.ellipse.edoi.ejb.msf872.MSF872Rec;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Key;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Rec;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;

/**
 * Request Parameters for Trb87c.
 * <li><code>authority</code> : Authority Type</li>
 * <li><code>district</code> : District - Blank for All</li>
 */
public class ParamsTrb87c {
    private String authority, district
}

/**
 * Report content for Trb87c.
 */
public class ReportTrb87cLine {

    private String posId = "", prim = "", poTi = "", os = "", empId = "", fName = "", surName = "", empName = "", glPr = "", dist = "", at = "", psd = ""
    private double llt = 0, ult = 0

    /**
     * Return the report title.
     * @return browseEmployeeAuthority "List of Staff Positions with Authorities"
     */
    public static final String writeReportTitle() {
        return "List of Staff Positions with Authorities".center(132)
    }

    /**
     * Return the report header at the first line.
     * @return report header at the first line
     */
    public static final String writeReportHeader1() {
        return "           Pri                               Occ".padRight(83) + " Global            ------ A U T H O R I T Y ----"
    }

    /**
     * Return the report header at the second line.
     * @return report header at the second line
     */
    public static final String writeReportHeader2() {
        return " Pos-Id    Pos Position Title                Stat Emp-Id   Employee Name            Profile    Dist  Type      Low Limit  High Limit"
    }

    /**
     * Return the report summary, show the number of staff.
     * @param lastCounter number of staff
     * @return report summary showing the number of staff
     */
    public static final String writeReportLast(int lastCounter) {
        return String.format("Total Number of Staff     : %d", lastCounter).center(132)
    }

    /**
     * Return CSV title.
     * @return CSV title
     */
    public static final String writeCSVTitle() {
        return "Position-Id,Prim-Pos,Pos-Title,Occ-Stat,Emp-Id,Emp-Name,Glob-Prof,Dist,Auth-Type,Auth-Low-Limit," + "Auth-High-Limit,".padRight(36)
    }

    /**
     * Return the report detail.
     * @return report detail
     */
    public String getReportDetail() {
        return String.format(" %-11s%-3s%-32s%-3s%-8s %-24s %-11s%-5s %-11s%8.2f %10.2f",
        posId.length() > 11 ? posId.substring(0,11): posId, prim,
        poTi.length() > 32 ? poTi.substring(0, 32) : poTi, os,
        empId.length() > 8 ? empId.substring(0, 8) : empId,
        empName.length() > 24 ? empName.substring(0,24): empName,
        glPr, dist, at, llt, ult)
    }

    /**
     * Return the CSV detail.
     * @return CSV detail
     */
    public String getCSVDetail() {
        return String.format("%-10s,%-1s,%-32s,%-3s,%-10s,%-24s,%-10s,%-4s,%-4s,%10.2f,%10.2f,",
        posId, prim, poTi, os, empId, empName, glPr, dist, at, llt, ult)
    }
}

/**
 * Comparator for Trs87CARecord used in sorting process. 
 * Sort Trs87CARecord based on <code>PosId, EmpId, Prim, os, Psd</code>
 */
public class Trt87cRecordComparator implements Comparator<ReportTrb87cLine> {

    @Override
    public int compare(ReportTrb87cLine o1, ReportTrb87cLine o2) {
        int c = o1.posId.compareTo(o2.posId)
        if(c == 0) {
            c = o1.empId.compareTo(o2.empId)
        }
        if(c == 0) {
            c = o1.prim.compareTo(o2.prim)
        }
        if(c == 0) {
            c = o1.os.compareTo(o2.os)
        }
        if(c == 0) {
            c = o1.psd.compareTo(o2.psd)
        }
        return c
    }
}

/**
 * Main process of Trb87c.
 */
public class ProcessTrb87c extends SuperBatch {
    /*
     * Constants
     */
    private static final String REPORT_NAME = "TRB87CA"
    private static final String MSF872_REC_TYPE_P = "P"
    private static final String MSF878_POS_STOP_DATE = "00000000"
    private static final String MSF870_OCCUP_STATUS_D = "D"
    private static final String MSF760_EMP_STATUS_A = "A"
	private static final int MAX_ROW_READ = 1000

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private int version = 3

    /*
     * Variables
     */
    private int rowCount = 0
    private ParamsTrb87c batchParams
    private def reportWriter
    private BufferedWriter csvWriter
    private File csvFile
    private List<ReportTrb87cLine> trs87CARecords

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version : " + version)
        batchParams = params.fill(new ParamsTrb87c())
        info("Authority  : ${batchParams.authority}")
        info("District   : ${batchParams.district}")

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
        browseEmployeeAuthority()
        //sort the list
        Collections.sort(trs87CARecords, new Trt87cRecordComparator())
        writeReport()
    }

    /**
     * Print the report.
     */
    private void printBatchReport() {
        info("printBatchReport")
        reportWriter.writeLine(132, "-")
        reportWriter.write("")
        reportWriter.write("")
        reportWriter.write(ReportTrb87cLine.writeReportLast(trs87CARecords.size()))
        reportWriter.close()
        csvWriter.close()
        if(taskUUID?.trim()) {
            info("Adding CSV into Request.")
            request.request.CURRENT.get().addOutput(csvFile,
                    "text/comma-separated-values", REPORT_NAME);
        }
    }

    /**
     * Initialize the report writer and other variables.
     */
    private void initialize() {
        info("initialization")
        // Initialize report
        reportWriter = report.open(REPORT_NAME)
        // Intialize csv
        String workingDir   = env.workDir
        String taskUUID     = this.getTaskUUID()
        String csvFilePath  = "${workingDir}/${REPORT_NAME}"
        if(taskUUID?.trim()) {
            csvFilePath  = csvFilePath  + "." + taskUUID
        }
        csvFilePath  = csvFilePath  + ".csv"
        csvFile = new File(csvFilePath)
        info("CSV file ${csvFile.getName()} created.")
        csvWriter = new BufferedWriter(new FileWriter(csvFile))
        //initialize the records
        trs87CARecords = new ArrayList<ReportTrb87cLine>()
    }

    /**
     * Browse Employee/Position Authority constrained by the request parameters.
     */
    private void browseEmployeeAuthority() {
        info("browseEmployeeAuthority")
        /*
         * Use edoi.search since there is no service call for Authority
         */
        Constraint c872Type = MSF872Key.rec_872Type.equalTo(MSF872_REC_TYPE_P)
        Constraint cAuthority = MSF872Key.authtyType.equalTo(batchParams.authority)
        Constraint cPositionId = MSF872Key.positionId.greaterThanEqualTo(" ")
        QueryImpl query = new QueryImpl(MSF872Rec.class).and(c872Type).and(cAuthority).and(cPositionId).orderBy(MSF872Rec.msf872Key)
        if(!batchParams.district.trim().isEmpty()) {
            Constraint cDistrict = MSF872Key.dstrctCode.equalTo(batchParams.district)
            query = query.and(cDistrict)
        }
        edoi.search(query, MAX_ROW_READ) { MSF872Rec msf872Rec->
            String msf872PosId = msf872Rec.getPrimaryKey().positionId
            //Check whether the Position is already Deleted
            MSF870Rec msf870Rec = readPosition(msf872PosId)
            if(msf870Rec != null) {

                rowCount = 0
                readEmployeePosition(msf872Rec, msf870Rec)

                /* This means there are no encumbent employees
                 * on the Position.  The position still needs
                 * to be reported.
                 */
                if(rowCount == 0) {
                    buildCSVRecord(msf872Rec, msf870Rec, null, null)
                }
            }
        }
    }


    /**
     * Get the Position record based on Employee/Position Authority id.
     * @param msf872PosId id from Employee/Position Authority record
     * @return Position record based on Employee/Position Authority id
     */
    private MSF870Rec readPosition(String msf872PosId) {
        info("readPosition")
        MSF870Rec msf870Rec = null
        try {
            msf870Rec = edoi.findByPrimaryKey(new MSF870Key(positionId: msf872PosId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF870Rec with position id : ${msf872PosId} does not exist")
        }
        return msf870Rec != null && !MSF870_OCCUP_STATUS_D.equals(msf870Rec.occupStatus) ? msf870Rec : null
    }

    /**
     * Browse Employee Position records based on Employee/Position Authority and Position.
     * @param msf872Rec an Employee/Position Authority record (<code>MSF872Rec</code>) to be proceed
     * @param msf870Rec a Position record (<code>MSF870Rec</code>) to be proceed
     */
    private void readEmployeePosition(MSF872Rec msf872Rec, MSF870Rec msf870Rec) {
        info("readEmployeePosition")
        Constraint cPosId = MSF878Key.positionId.equalTo(msf872Rec.getPrimaryKey().positionId)
        Constraint cPosStopDate = MSF878Key.posStopDate.equalTo(MSF878_POS_STOP_DATE)
        Constraint cEmpId = MSF878Key.employeeId.greaterThanEqualTo(" ")
        QueryImpl query = new QueryImpl(MSF878Rec.class).and(cPosId).and(cPosStopDate).and(cEmpId).orderBy(MSF878Rec.msf878Key)
        edoi.search(query,MAX_ROW_READ) {MSF878Rec msf878Rec->
            String msf878EmpId = msf878Rec.getPrimaryKey().employeeId
            //Must be an Active employee
            MSF760Rec msf760Rec = null
            try {
                msf760Rec = edoi.findByPrimaryKey(new MSF760Key(employeeId:msf878EmpId))
            } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                info("MSF760Rec with employee id : ${msf878EmpId} does not exist")
            }
            //Must have Employee Details
            if(msf760Rec != null && MSF760_EMP_STATUS_A.equals(msf760Rec.empStatus)) {
                MSF810Rec msf810Rec = null
                try {
                    msf810Rec = edoi.findByPrimaryKey(new MSF810Key(employeeId:msf878EmpId))
                    rowCount++
                    buildCSVRecord(msf872Rec, msf870Rec, msf878Rec, msf810Rec)
                } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                    info("MSF810Rec with employee id : ${msf878EmpId} does not exist")
                }
            }
        }
    }

    /**
     * Build the CSV and report detail information based on Employee/Position Authority, Position, Employee Position, and Employee records.
     * @param msf872Rec an Employee/Position Authority record (<code>MSF872Rec</code>) to be proceed
     * @param msf870Rec a Position record (<code>MSF870Rec</code>) to be proceed
     * @param msf878Rec an Employee Position record (<code>msf878Rec</code>) to be proceed
     * @param msf810Rec an Employee record (<code>MSF810Rec</code>) to be proceed
     */
    private void buildCSVRecord(MSF872Rec msf872Rec, MSF870Rec msf870Rec, MSF878Rec msf878Rec, MSF810Rec msf810Rec) {
        info("buildCSVRecord")
        ReportTrb87cLine rec = new ReportTrb87cLine()
        rec.dist    = msf872Rec != null ? msf872Rec.getPrimaryKey().dstrctCode : " "
        rec.posId   = msf872Rec != null ? msf872Rec.getPrimaryKey().positionId : " "
        rec.at      = msf872Rec != null ? msf872Rec.getPrimaryKey().authtyType : " "
        rec.llt     = msf872Rec != null ? msf872Rec.authtyLowLim : 0
        rec.ult     = msf872Rec != null ? msf872Rec.authtyUppLim : 0
        rec.psd     = msf878Rec != null ? msf878Rec.getPrimaryKey().posStopDate : " "
        rec.empId   = msf878Rec != null ? msf878Rec.getPrimaryKey().employeeId : " "
        rec.prim    = msf878Rec != null ? msf878Rec.getPrimaryKey().primaryPos : " "
        rec.os      = msf870Rec != null ? msf870Rec.occupStatus : " "
        rec.glPr    = msf870Rec != null ? msf870Rec.globalProfile : " "
        rec.poTi    = msf870Rec != null ? msf870Rec.posTitle : " "
        rec.fName   = msf810Rec != null ? msf810Rec.firstName : " "
        rec.surName = msf810Rec != null ? msf810Rec.surname : " "
        //check Position Id
        checkPositionId(rec)
        trs87CARecords.add(rec)
    }

    /**
     * Check the position id from the CSV and report record. If it is a numeric value, replace starting spaces with '0'
     * @param rec CSV and report record
     */
    private void checkPositionId(ReportTrb87cLine rec) {
        info("checkPositionId")
        if(rec.posId?.isNumber()) {
            rec.posId = rec.posId.padLeft(10).replace(" ", "0")
        }
    }

    /**
     * Write the CSV and report record into report and CSV.
     */
    private void writeReport() {
        info("writeReport")
        csvWriter.write(ReportTrb87cLine.writeCSVTitle())
        csvWriter.write("\n")
        reportWriter.write(ReportTrb87cLine.writeReportTitle())
        reportWriter.writeLine(132, "-")
        reportWriter.write(ReportTrb87cLine.writeReportHeader1())
        reportWriter.write(ReportTrb87cLine.writeReportHeader2())
        reportWriter.writeLine(132, "-")
        reportWriter.write("")
        trs87CARecords.each {ReportTrb87cLine rec->
            if(rec.empId?.isNumber()) {
                rec.empId = rec.empId.substring(5)
            } else {
                rec.empId = rec.empId
            }
            if(!rec.fName.trim().isEmpty()) {
                rec.fName = rec.fName.padRight(1)
                rec.empName = String.format("%s. %s", rec.fName.substring(0, 1), rec.surName)
            }
            reportWriter.write(rec.getReportDetail())
            csvWriter.write(rec.getCSVDetail())
            csvWriter.write("\n")
        }
    }

}

/**
 * Run the script
 */
ProcessTrb87c process = new ProcessTrb87c()
process.runBatch(binding)
