/**
* @Ventyx 2012
*/
package com.mincom.ellipse.script.custom

import com.mincom.batch.script.Reports;
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.script.util.CommAreaScriptWrapper;
import com.mincom.ellipse.script.util.EDOIWrapper;
import com.mincom.reporting.text.TextReport
import com.mincom.reporting.text.TextReportHelper;
import com.mincom.reporting.text.TextReportImpl
import com.mincom.reporting.text.TextReportService
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BatchReports {

    private EDOIWrapper edoi
    private CommAreaScriptWrapper commarea;
    private Reports coreReport;
    private TextReport report;
    private version = 1
    private MSF080Rec msf080Rec
    private Binding b

    public BatchReports (Binding b, MSF080Rec msf080Rec){
        coreReport = b.getVariable("report");
        if (msf080Rec == null){
            this.msf080Rec = new MSF080Rec()
        }else{
            this.msf080Rec = msf080Rec
        }
        this.b = b
        info("Version:" + version)
    }

    /**
     * Create a blank report with the default report width 132 and default max row 60
     * This method also create a page header and request header
     * @param reportName  e.g. AAB123A, AAB123B
     * */
    public BatchTextReports open(String reportName){
        File file = File.createTempFile(reportName+ ".","")
        TextReportImpl report = new TextReportImpl(reportName, file);
        coreReport.outputHandler.addFile(report.getFile())
        BatchTextReports bTextReport = new BatchTextReports(report,msf080Rec,b)
        bTextReport.reportWidth = 132
        bTextReport.maxLine = 60
        bTextReport.pageNo = 1
        bTextReport.reportName = reportName
        bTextReport.firstPage()
        return bTextReport
    }

    /**
    * Create a blank report with the default report width 132 and default max row 60
    * This method also create a page header and request header and pageHeading
    * @param String reportName  e.g. AAB123A, AAB123B
    * @param List pageHeading
    * */
   public BatchTextReports open(String reportName, List <String> pageHeadings){
       BatchTextReports bTextReport = open(reportName)
       bTextReport.pageHeadings = pageHeadings
       bTextReport.heading()
       return bTextReport
   }

    private void info(String value){
        def logObject = LoggerFactory.getLogger(getClass());
        logObject.info("------------- " + value)
    }
}
