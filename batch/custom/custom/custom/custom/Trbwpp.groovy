/**
 *@Ventyx 2012
 *
 * This program get all authorised Work Order which have
 * 'MA' Work Order Type and blank Plan Priority.
 * It will then modify the Plan Priority to '99'.
 */
package com.mincom.ellipse.script.custom;

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
import java.text.DecimalFormat;

import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.insp_alarm_defect.*;
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO;
import com.mincom.enterpriseservice.ellipse.workorder.*;
import com.mincom.enterpriseservice.exception.*;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesRequestDTO;
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveCodesReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyCollectionDTO;


public class ParamsTrbwpp{
    //List of Input Parameters


}

public class ProcessTrbwpp extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 4;

    private def     errorRpt;
    private Boolean bErrorReportOpen       = false;
    private Boolean bOutputFileOpen        = false;
    private BufferedWriter outputFile;
    private ParamsTrbwpp batchParams;
    private String  outputFilePath         = " ";
    private String  reportFilePath         = " ";

    private static final String SEPARATOR    = "^";
    private static final String WO_USER_STATUS = "UR";
    private static final int REQUEST_REPLY_NUM = 20;
    
    private ArrayList<String> attachments = new ArrayList();

    public void runBatch(Binding b){

        init(b)
        printSuperBatchVersion()
        info("runBatch Version : " + version)

        //PrintRequest Parameters
        //No parameters
        
        try {
            processBatch();

        } finally {
            closeFiles();
            constructEmailConfigurationOutput();
        }
    }


    //additional method - start from here.


    /**
     * This is the main process.
     */
    private void processBatch(){
        if(initialise()){
            mainProcess(); 
        }
    }

    /**
     * This is the initialisation of the program. 
     * If all initialisation is okay, it will return true.
     * @return true if  all initialisation is okay
     */
    private Boolean initialise(){
        info("initialise");

        Boolean continueProcess = false;
        
        try{
            String fileLocation = "${env.workDir}/TRTWPP";
            String taskUuid = getTaskUUID() ;
            if(taskUuid?.trim()){
                fileLocation = fileLocation +"."+taskUuid;
            }
            fileLocation=fileLocation+".lis";
            info("File location: ${fileLocation}");
            outputFile = this.openOutputFile(fileLocation);
        
            ArrayList<String> errorRptPgHdr = new ArrayList<String>();
            errorRptPgHdr.add("TRBWPP Error Report".center(132));
            errorRptPgHdr.add(" ");
            errorRptPgHdr.add("".padLeft(132,"-"));
            errorRptPgHdr.add(" ");
            errorRptPgHdr.add("  WO Number      Field Ref / Value             Error / Warning Message");
            errorRpt = report.open("TRBWPPA", errorRptPgHdr);
			reportFilePath = errorRpt.report.getFile().getAbsolutePath()+"|TRBWPPA.txt";
			
            bErrorReportOpen = true;
            
            continueProcess = true;
        }
        catch(RuntimeException e){
            e.printStackTrace();    
        }
        finally{
            return continueProcess;
        }
       
    }
    
    /**
     * This method will browse all Work Order with the defined specification to be processed.
     */
    private void mainProcess(){
        info("mainProcess");
        
        Constraint cDstrctCode   = MSF620Key.dstrctCode.greaterThanEqualTo(" ");
        Constraint cWOType       = MSF620Rec.woType.equalTo("MA");
        Constraint cWOStatus     = MSF620Rec.woStatusM.equalTo("A");
        Constraint cWOUsrStat    = MSF620Rec.woStatusU.equalTo(" ");
        Constraint cMntSchedTsk  = MSF620Rec.maintSchTask.equalTo(" ");
		def query = new QueryImpl(MSF620Rec.class).
				and(cDstrctCode).
				and(cWOType).
				and(cWOStatus).
				and(cWOUsrStat).
				and(cMntSchedTsk).orderBy(MSF620Rec.msf620Key);
	
        Integer recordCount = 0;
        writeHeaderRecord()
		  edoi.search(query){MSF620Rec msf620Rec ->
			  
            if(!isFromDefect(msf620Rec.primaryKey.dstrctCode, msf620Rec.primaryKey.workOrder)) {
                WorkOrderServiceModifyReplyDTO workOrderModifyDTO = updateUserStatus(msf620Rec);
                if(workOrderModifyDTO!=null){
                    writeExtractRecord(msf620Rec, workOrderModifyDTO);
                    recordCount++;
                }
            }
        }
        
        attachments.add(reportFilePath)
        if(recordCount==0){
            errorRpt.write("  No work order were updated");
        }
        else{
            attachments.add(outputFilePath);
        }
    }
    
    private Boolean isFromDefect(String dstrctCode, String workOrder) {
        info("isFromDefect");
        
        Boolean fromDefect = false;
        
        Constraint cAlarmDstrct = INSP_ALARM_DEFECTRec.alarmDstrct.equalTo(dstrctCode);
        Constraint cAlarmWO = INSP_ALARM_DEFECTRec.alarmWorkOrder.equalTo(workOrder);

        QueryImpl query = new QueryImpl(INSP_ALARM_DEFECTRec.class).and(cAlarmDstrct).and(cAlarmWO);
        
        INSP_ALARM_DEFECTRec defect = edoi.firstRow(query);
        if(defect) {
            fromDefect = true;
        }
        
        return fromDefect;
    }
    
    /**
     * This method will try to modify the plan priority of the work order.
     * If it succeed, it will write the extract record for that work order.
     * If an error occur, it will write the error message to the error report.
     * @param msf620Rec
     */
    private WorkOrderServiceModifyReplyDTO updateUserStatus(MSF620Rec msf620Rec){
        info("updatePlanPriority");
        
        info("Updating msf620 with key District: ${msf620Rec.primaryKey.dstrctCode}, WO No: ${msf620Rec.primaryKey.workOrder}")
   
        WorkOrderServiceModifyReplyDTO workOrderModifyDTO = null;
                   
        try {
            
            WorkOrderServiceModifyRequiredAttributesDTO reqAttrib = new WorkOrderServiceModifyRequiredAttributesDTO();
            reqAttrib.returnDistrictCode = reqAttrib.returnWorkOrder = reqAttrib.returnWorkOrderStatusU = true;
            
            workOrderModifyDTO = service.get("WORKORDER").modify({
                WorkOrderServiceModifyRequestDTO it ->
                it.requiredAttributes = reqAttrib;
                it.districtCode       = msf620Rec.primaryKey.dstrctCode;
                it.workOrder          = new WorkOrderDTO(msf620Rec.primaryKey.workOrder)
				it.workOrderStatusU   = WO_USER_STATUS;
            },false)
            

        }catch(EnterpriseServiceOperationException e) {
            reportError(msf620Rec, e)
        }catch(Exception e2){
            info("Error when running Work Order Modify service: "+e2.getMessage());
        }
        
        return workOrderModifyDTO;
        
    }
    
    /**
    * This method writes all columns header to the output file.
    */
    private void writeHeaderRecord(){
        info("writeHeaderRecord")
        
        String printHeader = "DSTRCT_CODE"
               printHeader = printHeader + SEPARATOR + "WORK_ORDER"
               printHeader = printHeader + SEPARATOR + "WO_DESC"
               printHeader = printHeader + SEPARATOR + "STD_JOB_NO"
               printHeader = printHeader + SEPARATOR + "SJ_DSTRCT_CODE"
               printHeader = printHeader + SEPARATOR + "WO_TYPE"
               printHeader = printHeader + SEPARATOR + "ORIG_PRIORITY"
               printHeader = printHeader + SEPARATOR + "ORIGINATOR_ID"
               printHeader = printHeader + SEPARATOR + "WO_STATUS_M"
               printHeader = printHeader + SEPARATOR + "WO_STATUS_U"
               printHeader = printHeader + SEPARATOR + "RAISED_DATE"
               printHeader = printHeader + SEPARATOR + "DSTRCT_WO"
               printHeader = printHeader + SEPARATOR + "RAISED_TIME"
               printHeader = printHeader + SEPARATOR + "AUTHSD_BY"
               printHeader = printHeader + SEPARATOR + "AUTHSD_POSITION"
               printHeader = printHeader + SEPARATOR + "AUTHSD_DATE"
               printHeader = printHeader + SEPARATOR + "AUTHSD_TIME"
               printHeader = printHeader + SEPARATOR + "OUT_SERV_DATE"
               printHeader = printHeader + SEPARATOR + "OUT_SERV_TIME"
               printHeader = printHeader + SEPARATOR + "REQ_BY_DATE"
               printHeader = printHeader + SEPARATOR + "REQ_BY_TIME"
               printHeader = printHeader + SEPARATOR + "PLAN_FIN_DATE"
               printHeader = printHeader + SEPARATOR + "PLAN_FIN_TIME"
               printHeader = printHeader + SEPARATOR + "PLAN_STAT_TYPE"
               printHeader = printHeader + SEPARATOR + "PLAN_STAT_VAL"
               printHeader = printHeader + SEPARATOR + "COMPLETED_BY"
               printHeader = printHeader + SEPARATOR + "CLOSED_DT"
               printHeader = printHeader + SEPARATOR + "CLOSED_COMMIT_DT"
               printHeader = printHeader + SEPARATOR + "CLOSED_TIME"
               printHeader = printHeader + SEPARATOR + "COMPLETED_CODE"
               printHeader = printHeader + SEPARATOR + "CLOSED_STATUS"
               printHeader = printHeader + SEPARATOR + "SHUTDOWN_NO"
               printHeader = printHeader + SEPARATOR + "SHUTDOWN_EQUIP"
               printHeader = printHeader + SEPARATOR + "SHUTDOWN_TYPE"
               printHeader = printHeader + SEPARATOR + "PARENT_WO"
               printHeader = printHeader + SEPARATOR + "PROJECT_NO"
               printHeader = printHeader + SEPARATOR + "RELATED_WO"
               printHeader = printHeader + SEPARATOR + "WORK_GROUP"
               printHeader = printHeader + SEPARATOR + "CREW"
               printHeader = printHeader + SEPARATOR + "ASSIGN_PERSON"
               printHeader = printHeader + SEPARATOR + "MAINT_TYPE"
               printHeader = printHeader + SEPARATOR + "ORIG_DOC_TYPE"
               printHeader = printHeader + SEPARATOR + "ORIG_DOC_NO"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX1"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX2"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX3"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX4"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX5"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX6"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX7"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX8"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX9"
               printHeader = printHeader + SEPARATOR + "WO_JOB_CODEX10"
               printHeader = printHeader + SEPARATOR + "PLAN_PRIORITY"
               printHeader = printHeader + SEPARATOR + "UNIT_OF_WORK"
               printHeader = printHeader + SEPARATOR + "UOW_RATE"
               printHeader = printHeader + SEPARATOR + "UNITS_REQUIRED"
               printHeader = printHeader + SEPARATOR + "UNITS_COMPLETE"
               printHeader = printHeader + SEPARATOR + "PC_COMPLETE"
               printHeader = printHeader + SEPARATOR + "UNITS_INV_CHGE"
               printHeader = printHeader + SEPARATOR + "BILLABLE_IND"
               printHeader = printHeader + SEPARATOR + "BILLING_LVL_IND"
               printHeader = printHeader + SEPARATOR + "CUST_NO"
               printHeader = printHeader + SEPARATOR + "FINAL_COSTS"
               printHeader = printHeader + SEPARATOR + "SUPP_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "AP_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "PS_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "PAYR_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "LABC_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "JNL_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "AR_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "GL_FINAL_SW"
               printHeader = printHeader + SEPARATOR + "PAPER_HIST"
               printHeader = printHeader + SEPARATOR + "FAILURE_PART"
               printHeader = printHeader + SEPARATOR + "EQUIP_NO"
               printHeader = printHeader + SEPARATOR + "COMP_CODE"
               printHeader = printHeader + SEPARATOR + "COMP_MOD_CODE"
               printHeader = printHeader + SEPARATOR + "MAINT_SCH_TASK"
               printHeader = printHeader + SEPARATOR + "DSTRCT_ACCT_CODE"
               printHeader = printHeader + SEPARATOR + "PLAN_STR_DATE"
               printHeader = printHeader + SEPARATOR + "PLAN_STR_TIME"
               printHeader = printHeader + SEPARATOR + "LAST_MOD_DATE"
               printHeader = printHeader + SEPARATOR + "LAST_MOD_TIME"
               printHeader = printHeader + SEPARATOR + "LAST_MOD_USER"
               printHeader = printHeader + SEPARATOR + "CREATION_DATE"
               printHeader = printHeader + SEPARATOR + "CREATION_TIME"
               printHeader = printHeader + SEPARATOR + "CREATION_USER"
               printHeader = printHeader + SEPARATOR + "HIGHEST_TASK"
               printHeader = printHeader + SEPARATOR + "NO_OF_TASKS"
               printHeader = printHeader + SEPARATOR + "NO_TASKS_COMPL"
               printHeader = printHeader + SEPARATOR + "ORIG_METHOD"
               printHeader = printHeader + SEPARATOR + "RLOC_COST_ACCT"
               printHeader = printHeader + SEPARATOR + "RLOC_VAR_ACCT"
               printHeader = printHeader + SEPARATOR + "RLOC_CR_EE"
               printHeader = printHeader + SEPARATOR + "RLOC_CR_PROJECT"
               printHeader = printHeader + SEPARATOR + "RLOC_WO"
               printHeader = printHeader + SEPARATOR + "RLOC_METHOD"
               printHeader = printHeader + SEPARATOR + "RLOC_FREQ_IND"
               printHeader = printHeader + SEPARATOR + "RLOC_MARGIN_PC"
               printHeader = printHeader + SEPARATOR + "QUOTE_VALUE"
               printHeader = printHeader + SEPARATOR + "ACT_COST_INV"
               printHeader = printHeader + SEPARATOR + "INDIRECT_INV_AMT"
               printHeader = printHeader + SEPARATOR + "LAST_TRAN_RLOC"
               printHeader = printHeader + SEPARATOR + "LAST_TRAN_VAL_RLOC"
               printHeader = printHeader + SEPARATOR + "ACT_COST_RLOC"
               printHeader = printHeader + SEPARATOR + "CON_AST_SEG_FR"
               printHeader = printHeader + SEPARATOR + "CON_AST_SEG_TO"
               printHeader = printHeader + SEPARATOR + "ACT_REVENUE"
               printHeader = printHeader + SEPARATOR + "AUTO_REQ_IND"
               printHeader = printHeader + SEPARATOR + "PLAN_OFFSET_SW"
               printHeader = printHeader + SEPARATOR + "PO_NO"
               printHeader = printHeader + SEPARATOR + "PO_ITEM_NO"
               printHeader = printHeader + SEPARATOR + "REVENUE_CODE"
               printHeader = printHeader + SEPARATOR + "CAPITAL_SW"
               printHeader = printHeader + SEPARATOR + "AFUDC_DTE_SERV"
               printHeader = printHeader + SEPARATOR + "DTE_PENDING_SW"
               printHeader = printHeader + SEPARATOR + "AFUDC_SUSPEND"
               printHeader = printHeader + SEPARATOR + "REQUEST_ID"
               printHeader = printHeader + SEPARATOR + "LOCATION"
               printHeader = printHeader + SEPARATOR + "NOTICE_LOCN"
               printHeader = printHeader + SEPARATOR + "ASSOC_BPU_SW"
               printHeader = printHeader + SEPARATOR + "WO_PR_JOB_IND"
               printHeader = printHeader + SEPARATOR + "DIS_MOD_DATE"
               printHeader = printHeader + SEPARATOR + "DIS_MOD_TIME"
               printHeader = printHeader + SEPARATOR + "DIS_MOD_USER"
               printHeader = printHeader + SEPARATOR + "MUST_START_IND"
               printHeader = printHeader + SEPARATOR + "LOCATION_FR"
               printHeader = printHeader + SEPARATOR + "MSSS_STATUS_IND"
               printHeader = printHeader + SEPARATOR + "APTW_EXISTS_SW"
               printHeader = printHeader + SEPARATOR + "TASK_APTW_SW"
               printHeader = printHeader + SEPARATOR + "LINKED_IND"
               printHeader = printHeader + SEPARATOR + "RECALL_TIME_HRS"
               printHeader = printHeader + SEPARATOR + "ACTUAL_START_DATE"
               printHeader = printHeader + SEPARATOR + "ACTUAL_START_TIME"
               printHeader = printHeader + SEPARATOR + "ACTUAL_FINISH_DATE"
               printHeader = printHeader + SEPARATOR + "ACTUAL_FINISH_TIME"
               printHeader = printHeader + SEPARATOR + "RESPONDED_DATE"
               printHeader = printHeader + SEPARATOR + "RESPONDED_TIME"
               printHeader = printHeader + SEPARATOR + "SERVICE_OFF_DATE"
               printHeader = printHeader + SEPARATOR + "SERVICE_OFF_TIME"
               printHeader = printHeader + SEPARATOR + "SERVICE_ON_DATE"
               printHeader = printHeader + SEPARATOR + "SERVICE_ON_TIME"
               printHeader = printHeader + SEPARATOR + "REQ_START_DATE"
               printHeader = printHeader + SEPARATOR + "REQ_START_TIME"
               printHeader = printHeader + SEPARATOR + "MAN_EFFORT"
               printHeader = printHeader + SEPARATOR + "ASSOCIATED_EQUIP"
               printHeader = printHeader + SEPARATOR + "SCH_SEG_FR"
               printHeader = printHeader + SEPARATOR + "SCH_SEG_TO"
               printHeader = printHeader + SEPARATOR + "TRANS_DSTRCT_CDE"
               printHeader = printHeader + SEPARATOR + "TRANS_WORK_ORDER"
               printHeader = printHeader + SEPARATOR + "DIR_REVENUE"
               printHeader = printHeader + SEPARATOR + "INTEG_UPDATE_SW"
               
               
        outputFile.write(printHeader+"\n")
    }
    
    /**
     * This method writes all columns in Work Order record to the output file.
     * @param msf620Rec
     */
    private void writeExtractRecord(MSF620Rec msf620Rec, WorkOrderServiceModifyReplyDTO serviceDTO){
        info("writeExtractRecord");
        
        DecimalFormat for3V2  = new DecimalFormat("##0.##")
        DecimalFormat for7V2  = new DecimalFormat("######0.##")
        DecimalFormat for9V2  = new DecimalFormat("########0.##")
        DecimalFormat for11V4 = new DecimalFormat("##########0.####")
        DecimalFormat for13V2 = new DecimalFormat("############0.##")
        
        String printString =                            serviceDTO.districtCode.padRight(4);
        printString = printString + SEPARATOR +  serviceDTO.workOrder.toString().padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.woDesc.padRight(40);
        printString = printString + SEPARATOR +  msf620Rec.stdJobNo.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.sjDstrctCode.padRight(4);
        printString = printString + SEPARATOR +  msf620Rec.woType.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.origPriority.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.originatorId.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.woStatusM.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.woStatusU.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.raisedDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.dstrctWo.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.raisedTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.authsdBy.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.authsdPosition.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.authsdDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.authsdTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.outServDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.outServTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.reqByDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.reqByTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.planFinDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.planFinTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.planStatType.padRight(2);
        printString = printString + SEPARATOR +  for9V2.format(msf620Rec.planStatVal).padLeft(12);
        printString = printString + SEPARATOR +  msf620Rec.completedBy.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.closedDt.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.closedCommitDt.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.closedTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.completedCode.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.closedStatus.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.shutdownNo.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.shutdownEquip.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.shutdownType.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.parentWo.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.projectNo.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.relatedWo.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.workGroup.padRight(7);
        printString = printString + SEPARATOR +  msf620Rec.crew.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.assignPerson.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.maintType.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.origDocType.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.origDocNo.padRight(20);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex1.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex2.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex3.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex4.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex5.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex6.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex7.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex8.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex9.padRight(5);
        printString = printString + SEPARATOR +  msf620Rec.woJobCodex10.padRight(5);
        printString = printString + SEPARATOR +  serviceDTO.planPriority.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.unitOfWork.padRight(4);
        printString = printString + SEPARATOR +  for11V4.format(msf620Rec.uowRate).padLeft(16);
        printString = printString + SEPARATOR +  for7V2.format(msf620Rec.unitsRequired).padLeft(10);
        printString = printString + SEPARATOR +  for7V2.format(msf620Rec.unitsComplete).padLeft(10);
        printString = printString + SEPARATOR +  for3V2.format(msf620Rec.pcComplete).padLeft(6);
        printString = printString + SEPARATOR +  for7V2.format(msf620Rec.unitsInvChge).padLeft(10);
        printString = printString + SEPARATOR +  msf620Rec.billableInd.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.billingLvlInd.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.custNo.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.finalCosts.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.suppFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.apFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.psFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.payrFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.labcFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.jnlFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.arFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.glFinalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.paperHist.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.failurePart.padRight(30);
        printString = printString + SEPARATOR +  msf620Rec.equipNo.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.compCode.padRight(4);
        printString = printString + SEPARATOR +  msf620Rec.compModCode.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.maintSchTask.padRight(4);
        printString = printString + SEPARATOR +  msf620Rec.dstrctAcctCode.padRight(28);
        printString = printString + SEPARATOR +  msf620Rec.planStrDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.planStrTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.lastModDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.lastModTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.lastModUser.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.creationDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.creationTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.creationUser.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.highestTask.padRight(3);
        printString = printString + SEPARATOR +  msf620Rec.noOfTasks.padRight(3);
        printString = printString + SEPARATOR +  msf620Rec.noTasksCompl.padRight(3);
        printString = printString + SEPARATOR +  msf620Rec.origMethod.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.rlocCostAcct.padRight(24);
        printString = printString + SEPARATOR +  msf620Rec.rlocVarAcct.padRight(24);
        printString = printString + SEPARATOR +  msf620Rec.rlocCrEe.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.rlocProject.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.rlocWo.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.rlocMethod.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.rlocFreqInd.padRight(1);
        printString = printString + SEPARATOR +  for3V2.format(msf620Rec.rlocMarginPc).padLeft(6);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.quoteValue).padLeft(16);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.actCostInv).padLeft(16);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.indirectInvAmt).padLeft(16);
        printString = printString + SEPARATOR +  msf620Rec.lastTranRloc.padRight(13);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.lastTranValRloc).padLeft(16);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.actCostRloc).padLeft(16);
        printString = printString + SEPARATOR +  msf620Rec.conAstSegFr.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.conAstSegTo.padRight(6);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.actRevenue).padLeft(16);
        printString = printString + SEPARATOR +  msf620Rec.autoReqInd.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.planOffsetSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.poNo.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.poItemNo.padRight(2);
        printString = printString + SEPARATOR +  msf620Rec.revenueCode.padRight(4);
        printString = printString + SEPARATOR +  msf620Rec.capitalSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.afudcDteServ.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.dtePendingSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.afudcSuspend.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.requestId.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.location.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.noticeLocn.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.assocBpuSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.woPrJobInd.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.disModDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.disModTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.disModUser.padRight(10);
        printString = printString + SEPARATOR +  msf620Rec.mustStartInd.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.locationFr.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.msssStatusInd.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.aptwExistsSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.taskAptwSw.padRight(1);
        printString = printString + SEPARATOR +  msf620Rec.linkedInd.padRight(1);
        printString = printString + SEPARATOR +  for9V2.format(msf620Rec.recallTimeHrs).padLeft(12);
        printString = printString + SEPARATOR +  msf620Rec.actualStartDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.actualStartTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.actualFinishDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.actualFinishTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.respondedDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.respondedTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.serviceOffDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.serviceOffTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.serviceOnDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.serviceOnTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.reqStartDate.padRight(8);
        printString = printString + SEPARATOR +  msf620Rec.reqStartTime.padRight(6);
        printString = printString + SEPARATOR +  msf620Rec.manEffort.padRight(3);
        printString = printString + SEPARATOR +  msf620Rec.associatedEquip.padRight(12);
        printString = printString + SEPARATOR +  msf620Rec.schSegFr.padRight(7);
        printString = printString + SEPARATOR +  msf620Rec.schSegTo.padRight(7);
        printString = printString + SEPARATOR +  msf620Rec.transDstrctCde.padRight(4);
        printString = printString + SEPARATOR +  msf620Rec.transWorkOrder.padRight(8);
        printString = printString + SEPARATOR +  for13V2.format(msf620Rec.dirRevenue).padLeft(16);
        printString = printString + SEPARATOR +  msf620Rec.integUpdateSw.padRight(1);
               
        outputFile.write(printString+"\n");
    }
    
    /**
     * This method reports the error occured to the error report.
     * @param msf620Rec
     * @param e
     */
    private void reportError(MSF620Rec msf620Rec, EnterpriseServiceOperationException e){
        info("reportError");
        info("Error when updating msf620 with key District: ${msf620Rec.primaryKey.m_dstrctCode}, WO No: ${msf620Rec.primaryKey.m_workOrder}")
        for(retrErrors in e.errorMessages){
            String code = (retrErrors.code.tokenize("."))[2].trim();
            errorRpt.write("  ${msf620Rec.primaryKey.workOrder.padRight(15)}${retrErrors.fieldName.padRight(30)}${code} - ${retrErrors.message.padRight(80)} ");
        }
    }

    /**
     * Close all opened output files and or reports.
     */
    private void closeFiles(){
        info("closeFiles");
        
        if(bOutputFileOpen){
            outputFile.close();
        }

        if(bErrorReportOpen){
            errorRpt.close();
        }
    }

    /**
     * Construct the email configuration output file.
     */
    private void constructEmailConfigurationOutput() {
        info("constructEmailConfigurationOutput");
        List<TableServiceRetrieveCodesReplyDTO> tblReplyList =  retrieveTableCode("#MAI")
        List<String> emailAddresses = new ArrayList<String>()
        for(TableServiceRetrieveCodesReplyDTO tbl : tblReplyList) {
            if (tbl.getDescription().trim().contains("TRPWPP")) {
                info("TBLCODE:" + tbl.getTableCode().trim() + ":TBLDESC:" + tbl.getDescription().trim() + ":ASSOC:" + tbl.getAssociatedRecord().trim())
                emailAddresses.add(getEmailAdress(tbl.getTableCode().trim()))
            }
        }
        
        ArrayList<String> message = new ArrayList();
        message.add(" ");
        
        for(emailAddress in emailAddresses){
            
            SendEmail myEmail = new SendEmail("TRBWPP - Work Order Plan Priority Modification",emailAddress,message,attachments);
            myEmail.sendMail();
            if(myEmail.isError()){
                info("Error sending email : " + myEmail.getErrorMessage())
            }
        }
        
    }

    
    /**
     * Retrieves a list of all table entry with tableType.
     * @param tableType
     * @return List of TableServiceRetrieveCodesReplyDTO
     */
    private List retrieveTableCode(String tableType) {
        info("retrieveTableCode");
        List<TableServiceRetrieveCodesReplyDTO> cmReplyList = new ArrayList<TableServiceRetrieveCodesReplyDTO>();

        try {
            //restart value
            def restart = ""
            TableServiceRetrieveCodesReplyCollectionDTO cmReplyDTO = service.get("TABLE").retrieveCodes({ TableServiceRetrieveCodesRequestDTO it ->
                it.tableType = tableType
            }, REQUEST_REPLY_NUM, false, restart)
            restart = cmReplyDTO.getCollectionRestartPoint();
            cmReplyList.addAll(cmReplyDTO.getReplyElements());
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart.trim().length() > 0) {
                cmReplyDTO = service.get("TABLE").retrieveCodes({ TableServiceRetrieveCodesRequestDTO it ->
                    it.tableType = tableType;
                }, REQUEST_REPLY_NUM, false, restart)
                restart = cmReplyDTO.getCollectionRestartPoint();
                cmReplyList.addAll(cmReplyDTO.getReplyElements());
            }
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error at retrieveMntTypes ${serviceExc.getMessage()}")
        }
        return cmReplyList
    }
    
    
    /**
     * Get the email address for empName.
     * @param empName
     * @return email address for employee with name empName
     */
    private String getEmailAdress(String empName) {
        info("getEmailAdress");
        String emailAddress = " "
        List<String> name = empName.tokenize(" "); // index 0 is lastname, index 1 is firstname

        info("Name:" + name[0] + "|" + name[1] + "|")

        emailAddress = name[1].toLowerCase().capitalize()+"."+name[0].toLowerCase().capitalize()+"@transgrid.com.au"
        
        info("Email address: "+emailAddress)

        return emailAddress;
    }

    /**
     * Return the logical representation of the output file as the file location
     * If an output file with the same name exist, it will try to delete it first.
     * @param fileLocation
     * @return
     */
    private BufferedWriter openOutputFile(String fileLocation){
        info("openOutputFile");

        //Delete existing output file
        bOutputFileOpen = true;
        File f = new File(fileLocation);
        if (f.exists()) {
            if (!f.delete()) {
                info("unable to delete ["+ f.getAbsolutePath() + "]");
                throw new RuntimeException("unable to delete ["+ f.getAbsolutePath() + "]");
            }
        }
        
        outputFilePath = f.absolutePath + "|TRTWPP.lis";
        
        return new BufferedWriter(new FileWriter(fileLocation));
    }
    
}

/*run script*/
ProcessTrbwpp process = new ProcessTrbwpp();
process.runBatch(binding);