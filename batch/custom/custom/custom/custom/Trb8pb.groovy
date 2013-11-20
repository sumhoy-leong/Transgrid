/**
 *@Ventyx 2012
 *
 * This program requests TRJ8PS and create distribution reports.
 * It may also run in archive mode which will not create the 
 * distribution reports.
 */
package com.mincom.ellipse.script.custom;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;


import groovy.lang.Binding;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf080.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf766.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf808.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf824.*;
import com.mincom.ellipse.edoi.ejb.msf829.*;
import com.mincom.ellipse.edoi.ejb.msf8p1.*;
import com.mincom.ellipse.edoi.ejb.msf8p4.*;


public class ParamsTrb8pb{
    //List of Input Parameters
    String paramEmployeeId = "";
    String paramPayGroup = "";
    String paramPeriodEndDate = "";
    String paramRunType = "";
    String paramPayRunNumber = "";
    String paramEnvelopeType = "";
    String paramPayLocation = "";
    String paramDistMethod = "";
    String paramSortOrder = "";
    String paramArchiveMode = "";
}

public class ProcessTrb8pb extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 3

    private ParamsTrb8pb batchParams;

    private String  sPayAdvHdrUuid         = " ";
    private String  sPayGroup              = " ";
    private Long    lEmailCntr             = 0;
    private Long    lPrintedCntr           = 0;
    private Long    lViewCntr              = 0;
    private def     emailDistRpt;
    private def     nonEmailDistRpt;
    private Boolean bEmailDistRptOpen      = false;
    private Boolean bNonEmailDistRptOpen   = false;
    private ScreenAppLibrary s1;

    private class EmailAddrTrb8pb{
        private String workEmailAddress;
        private String persEmailAddress;
    }

    public void runBatch(Binding b){

        init(b)
        printSuperBatchVersion()
        info("runBatch Version : " + version)

        batchParams = params.fill(new ParamsTrb8pb())

        //PrintRequest Parameters
        info("paramEmployeeId: "+batchParams.paramEmployeeId);
        info("paramPayGroup: "+batchParams.paramPayGroup);
        info("paramPeriodEndDate: "+batchParams.paramPeriodEndDate);
        info("paramRunType: "+batchParams.paramRunType);
        info("paramPayRunNumber: "+batchParams.paramPayRunNumber);
        info("paramEnvelopeType: "+batchParams.paramEnvelopeType);
        info("paramPayLocation: "+batchParams.paramPayLocation);
        info("paramDistMethod: "+batchParams.paramDistMethod);
        info("paramSortOrder: "+batchParams.paramSortOrder);
        info("paramArchiveMode: "+batchParams.paramArchiveMode);

        initialise();


        if(sPayAdvHdrUuid?.trim()){
            /*Commented out because MSS080 is not working properly*/
            //requestTrj8ps();
            if(!batchParams.paramArchiveMode.trim().equals("Y")){
                printBatchReport()
            }
        }

        if(bEmailDistRptOpen){
            closeEmailDistRpt();
        }

        if(bNonEmailDistRptOpen){
            closeNonEmailDistRpt();
        }
    }


    //additional method - start from here.

    private void initialise(){
        info("initialise");
        setPayAdvHdrFields();

        s1 = new ScreenAppLibrary();
    }


    /**
     * This method set the fields taken from the pay advice header
     * to be used later in the distribution report.
     */
    private void setPayAdvHdrFields(){

        info("getPayAdvHdrUUID");

        /*
         * Using EDOI because MSF8P* series still does not have any service.
         */

        Constraint cPayGroup = MSF8P1Rec.payGroup.equalTo(batchParams.paramPayGroup.trim());
        Constraint cPerEndDt = MSF8P1Rec.perEndDt.equalTo(batchParams.paramPeriodEndDate.trim());
        Constraint cPayRunType = MSF8P1Rec.payRunType.equalTo(batchParams.paramRunType.trim());
        Constraint cPayRunNo = MSF8P1Rec.payRunNo.equalTo(batchParams.paramPayRunNumber.trim());
        Query query = new QueryImpl(MSF8P1Rec.class).and(cPayGroup).and(cPerEndDt).and(cPayRunType).and(cPayRunNo);
        MSF8P1Rec msf8p1Rec = (MSF8P1Rec) edoi.firstRow(query);

        if(msf8p1Rec){
            sPayAdvHdrUuid = msf8p1Rec.primaryKey.payAdvHdrUuid;
            sPayGroup      = msf8p1Rec.payGroup;
        }else{
            sPayAdvHdrUuid = " ";
            sPayGroup      = " ";
        }
        
        info("Pay_adv_hdr_uuid: ${sPayAdvHdrUuid}")
    }

//    /**
//     * This method requests TRJ8PS to create the actual pay slips.
//     */
//    private void requestTrj8ps(){
//        info("requestTrj8ps");
//
//        String uuid = getUUID();
//        String taskUuid = getTaskUUID();
//        def query = new QueryImpl(MSF080Rec.class).and(MSF080Key.progName.equalTo("TRB8PB"))
//        if(uuid?.trim()&&taskUuid?.trim()){
//            query = query.and(MSF080Rec.taskUuid.equalTo(taskUuid)).and(MSF080Rec.uuid.equalTo(uuid))
//        }
//
//
//        ReportRequestResultDTO replyDto;
//
//        try {
//            MSF080Rec rec = edoi.firstRow(query)
//
//            if (rec != null) {
//
//                ReportRequestDTO dto = new ReportRequestDTO();
//                dto.progName      = "TRJ8PS";
//                dto.deferDate     = rec.primaryKey.deferDate;
//                dto.deferTime     = rec.primaryKey.deferTime;
//                dto.requestRecNo  = rec.primaryKey.requestRecNo;
//                dto.requestNo     = rec.requestNo;
//                dto.userId        = rec.userId;
//                dto.requestBy     = rec.requestBy;
//                dto.dstrctCode    = rec.dstrctCode;
//                dto.requestDstrct = rec.requestDstrct;
//                dto.printerRec    = " ";
//                dto.progReportId  = rec.progReportId;
//                dto.medium        = rec.medium;
//                dto.jobId         = rec.jobId;
//                dto.distribCode   = rec.distribCode;
//                dto.retentionDays = rec.retentionDays;
//                dto.traceFlg      = rec.traceFlg;
//                dto.pubType       = rec.pubType;
//                dto.languageCode  = rec.languageCode;
//                dto.taskUuid      = rec.taskUuid;
//                dto.copyRqstSw    = "Y";
//                dto.requestParams = rec.requestParams;
//
//                //Call screen service to create report request
//                replyDto = s1.createReportRequest(eroi,dto)
//
//            }
//        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
//            String errorMsg = "Failed creating request for TRJ8PS. Abort the program"
//            info(errorMsg)
//            throw new RuntimeException(errorMsg);
//        } catch (ScreenErrorException e) {
//            replyDto.error = new ScreenError(e.code, e.message)
//        }
//    }


    /**
     * Print the distribution report for each employees.
     */
    private void printBatchReport(){
        info("printBatchReport");

        processEmployees()

    }

    /**
     * This method will get employee(s) based on the parameters supplied 
     * and print an entry to each of them.
     */
    private void processEmployees(){
        info("processEmployees");

        /*
         * Using EDOI because MSF8P* series still does not have any service.
         */

        QueryImpl query = new QueryImpl(MSF8P4Rec.class).and(MSF8P4Key.payAdvEmpUuid.greaterThanEqualTo(" ")).and(MSF8P4Rec.payAdvHdrUuid.trim().equalTo(sPayAdvHdrUuid.trim()));
        if(!batchParams.paramEnvelopeType.trim().equals("")){
            query = query.and(MSF8P4Rec.envelopeType.trim().equalTo(batchParams.paramEnvelopeType.trim()))
        }

        if(!batchParams.paramEmployeeId.trim().equals("")){
            query = query.and(MSF8P4Rec.employeeId.trim().equalTo(batchParams.paramEmployeeId.trim()))
        }

        if(!batchParams.paramPayLocation.trim().equals("")){
            query = query.and(MSF8P4Rec.payLocation.trim().equalTo(batchParams.paramPayLocation.trim()))
        }
        
        edoi.search(query) {MSF8P4Rec msf8p4Rec->
            EmailAddrTrb8pb emailAddrs = getEmailAddresses(msf8p4Rec.employeeId);
            printDistributionReport(msf8p4Rec.payLocation, msf8p4Rec.employeeId, msf8p4Rec.firstName,msf8p4Rec.surname,emailAddrs.workEmailAddress, emailAddrs.persEmailAddress)
        }
    }

    /**
     * Get all of the employee email addresses
     * @param employeeId
     * @return all email addresses the employee have in the system.
     */
    private EmailAddrTrb8pb getEmailAddresses(String employeeId){
        info("getEmailAddresses");

        EmailAddrTrb8pb emailAddrs = new EmailAddrTrb8pb();
        try{
            MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(employeeId));
            emailAddrs.persEmailAddress = msf810Rec.personalEmail;
            emailAddrs.workEmailAddress = msf810Rec.emailAddress;
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){

        }

        return emailAddrs;
    }

    /**
     * This method print an entry to the distribution record for an employee. 
     * It will print to either the email distribution report
     * or the non-email distribution report based on  employee payroll reference.
     * If the payroll preference is 'PERS', 'WORK', or blank it will print to email distribution report.
     * If the payroll preference is 'PRNT' or 'VIEW' it will print to non-email distribution report.
     * @param payLocation
     * @param employeeId
     * @param firstname
     * @param surname
     * @param workEmail
     * @param personalEmail
     */
    private void printDistributionReport(String payLocation, String employeeId, String firstname, String surname, String workEmail, String personalEmail){
        info("processEmployees");

        String empPayrollPref = getEmpPayrollPref(employeeId);
        if(batchParams.paramDistMethod?.trim()){
            if(batchParams.paramDistMethod.trim().equals("E")){
                if(empPayrollPref.trim().equals("PERS") || empPayrollPref.trim().equals("WORK")||empPayrollPref.trim().equals("")){
                    printEmailDistRpt(payLocation, employeeId, firstname, surname, workEmail, personalEmail, empPayrollPref)
                }
            }
            else{
                if(batchParams.paramDistMethod.trim().equals("P")){
                    if(empPayrollPref.trim().equals("PRNT") || empPayrollPref.trim().equals("VIEW")){
                        printNonEmailDistRpt(payLocation, employeeId, firstname, surname, empPayrollPref)
                    }
                }
            }
        }
        else{
            if(empPayrollPref.trim().equals("PERS") || empPayrollPref.trim().equals("WORK")||empPayrollPref.trim().equals("")){
                printEmailDistRpt(payLocation, employeeId, firstname, surname, workEmail, personalEmail, empPayrollPref)
            }else{
                printNonEmailDistRpt(payLocation, employeeId, firstname, surname, empPayrollPref)
            }
        }

    }

    /**
     * The function returns the payroll reference for the employee.
     * @param employeeId
     * @return employee payroll reference
     */
    private String getEmpPayrollPref(String employeeId){
        info("getEmpPayrollPref");

        /* Using screen service to get employee payroll preference
         try{
         EmployeePersonnelDTO empDto = new EmployeePersonnelDTO();
         empDto.setNextEemployeeId(employeeId);
         EmployeePersonnelResultDTO replyDto =  sl.readEmployeePersonnelDetails(empDto);
         return replyDto.jobClassLevel;
         } catch (Exception  e) {
         e.printStackTrace()
         }*/

        /*Using EDOI as using service inside EDOI search is not working*/
        try{
            MSF760Rec msf760Rec = edoi.findByPrimaryKey(new MSF760Key(employeeId));
            return msf760Rec.jobClassLvl;
        }
        catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
            return "";
        }
    }

    /**
     * This method print an entry to the email distribution report
     * It will open the file if it is not open, print the header, and then print the line.
     * The email printed will be the employee personal email for 'PERS' and employee work email for 'WORK' and blank preference.
     * @param payLocation
     * @param employeeId
     * @param firstname
     * @param surname
     * @param workEmail
     * @param personalEmail
     * @param payrollPref
     */
    private void printEmailDistRpt(String payLocation, String employeeId, String firstname, String surname, String workEmail, String personalEmail, String payrollPref){
        info("printEmailDistRpt");
        if(!bEmailDistRptOpen){
            openEmailDistRpt();
        }

        if(payrollPref.trim().equals("PERS")){
            emailDistRpt.write("  "+sPayGroup.padRight(9)+payLocation.padRight(14)+employeeId.padRight(18)+firstname.padRight(21)+surname.padRight(20)+personalEmail);
        }
        else{
            emailDistRpt.write("  "+sPayGroup.padRight(9)+payLocation.padRight(14)+employeeId.padRight(18)+firstname.padRight(21)+surname.padRight(20)+workEmail);
        }

        lEmailCntr++;
    }

    /**
     * This method print an entry to the non-email distribution report
     * It will open the file if it is not open, print the header, and then print the line.
     * The print indicator will be 'Y' if for 'PRNT' and 'N' for 'VIEW'.
     * @param payLocation
     * @param employeeId
     * @param firstname
     * @param surname
     * @param payrollPref
     */
    private void printNonEmailDistRpt(String payLocation, String employeeId, String firstname, String surname, String payrollPref){
        info("printNonEmailDistRpt");
        if(!bNonEmailDistRptOpen){
            openNonEmailDistRpt();
        }

        if(payrollPref.trim().equals("PRNT")){
            nonEmailDistRpt.write("  "+sPayGroup.padRight(9)+payLocation.padRight(14)+employeeId.padRight(18)+firstname.padRight(21)+surname.padRight(20)+"Y");
            lPrintedCntr++;
        }
        else{
            nonEmailDistRpt.write("  "+sPayGroup.padRight(9)+payLocation.padRight(14)+employeeId.padRight(18)+firstname.padRight(21)+surname.padRight(20)+"N");
            lViewCntr++;
        }
    }

    /**
     * This method opens the email distribution report and create the header.
     */
    private void openEmailDistRpt(){
        info("openEmailDistRpt");
        ArrayList<String> rptPgHead = new ArrayList<String>();
        rptPgHead.add("                                   Employee Listing of Emailed Pay Advice Distribution");
        rptPgHead.add(" ");
        rptPgHead.add(String.format("%132s"," ").replace(' ', '-'));
        rptPgHead.add(" ");
        rptPgHead.add("  Pay      Pay           Employee          Employee             Employe             Email Address")
        rptPgHead.add("  Group    Location      Id                First Name           Surname");
        emailDistRpt = report.open("TRB8PBA",rptPgHead)
        bEmailDistRptOpen = true;
    }

    /**
     * This method opens the non-email distribution report and create the header.
     */
    private void openNonEmailDistRpt(){
        info("openNonEmailDistRpt");
        ArrayList<String> rptPgHead = new ArrayList<String>();
        rptPgHead.add("                                   Employee Listing of Non-Emailed Pay Advice Distribution");
        rptPgHead.add(" ");
        rptPgHead.add(String.format("%132s"," ").replace(' ', '-'));
        rptPgHead.add(" ");
        rptPgHead.add("  Pay      Pay           Employee          Employee             Employe             Pay Advice Printed");
        rptPgHead.add("  Group    Location      Id                First Name           Surname");
        nonEmailDistRpt = report.open("TRB8PBB",rptPgHead)
        bNonEmailDistRptOpen = true;
    }

    
    /**
     * This method print the summary and closes the email distribution report.
     */
    private void closeEmailDistRpt(){
        info("closeEmailDistRpt");
        emailDistRpt.write("                No. of Employees with emailed Pay Advices: "+lEmailCntr.toString().padLeft(9));
        emailDistRpt.close();
        bEmailDistRptOpen = false;
    }

    /**
     * This method print the summary and closes the non-email distribution report.
     */
    private void closeNonEmailDistRpt(){
        info("openNonEmailDistRpt");
        nonEmailDistRpt.write("                No. of Employees with printed Pay Advices              : "+lPrintedCntr.toString().padLeft(9));
        nonEmailDistRpt.write("                No. of Employees without printed OR emailed Pay Advices: "+lViewCntr.toString().padLeft(9));
        nonEmailDistRpt.close();
        bNonEmailDistRptOpen = false;
    }
}

/*run script*/
ProcessTrb8pb process = new ProcessTrb8pb();
process.runBatch(binding);