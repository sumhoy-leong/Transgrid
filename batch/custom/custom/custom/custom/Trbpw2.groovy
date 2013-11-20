/*
* @Ventyx 2012
* This program extract the Work Order with Account Code
* different to Project Number
*
* Developed based on <b>FDD-Online Maintenance Costing Solution D04.docx</b>
*
*/
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf660.*;
import com.mincom.ellipse.edoi.ejb.msfx6n.*;
import com.mincom.ellipse.edoi.ejb.msf966.*;
import com.mincom.ellipse.eroi.linkage.mssemp.MSSEMPLINK;
import com.mincom.ellipse.eroi.linkage.mss900.MSS900LINK;

public class ParamsTrbpw2{
   //List of Input Parameters
   String empty;
}

public class ProcessTrbpw2 extends SuperBatch {
   /*
    * IMPORTANT!
    * Update this Version number EVERY push to GIT
    */
   private version = 5;
   private ParamsTrbpw2 batchParams;
   private File newFileA, newFileB, newFileC
   private FileWriter fstreamA, fstreamB, fstreamC
   private BufferedWriter reportA, reportB, reportC
   private int counter = 0
   private static final int MAX_ROW_READ = 1000

   public void runBatch(Binding b){
       init(b);
       printSuperBatchVersion();
       info("runBatch Version : " + version);
       batchParams = params.fill(new ParamsTrbpw2())
       try {
           processBatch();
       } finally {
           printBatchReport();
       }
   }

   private void processBatch(){
       info("processBatch");
       initialize()
       browseWorkOrder()
   }


   //additional method - start from here.

   /**
    * Initialize report writer for report A, B, and C
    */
   private void initialize(){
       info("initialize")
       String workingDir = env.workDir
       String taskUUID = this.getTaskUUID()
       String reportAPath = "${workingDir}/TRBPW2A"
       String reportBPath = "${workingDir}/TRBPW2B"
       String reportCPath = "${workingDir}/TRBPW2C"
       if(taskUUID?.trim()) {
           reportAPath = reportAPath + "." + taskUUID
           reportBPath = reportBPath + "." + taskUUID
           reportCPath = reportCPath + "." + taskUUID
       }
       reportAPath = reportAPath + ".csv"
       reportBPath = reportBPath + ".csv"
       reportCPath = reportCPath + ".csv"

       newFileA = new File(reportAPath)
       fstreamA = new FileWriter(newFileA)
       reportA = new BufferedWriter(fstreamA)
       reportA.write("Work Order,WO Description,RC/AC,Project No,RC/AC,Originator")
       reportA.write("\n")

       newFileB = new File(reportBPath)
       fstreamB = new FileWriter(newFileB)
       reportB = new BufferedWriter(fstreamB)
       reportB.write("Work Order,WO Description,RC/AC,Originator")
       reportB.write("\n")

       newFileC = new File(reportCPath)
       fstreamC = new FileWriter(newFileC)
       reportC = new BufferedWriter(fstreamC)
       reportC.write("Work Order,WO Description,Project/task,Originator")
       reportC.write("\n")
   }

   /**
    * Browse WorkOrder where closed date is empty.
    */
   private void browseWorkOrder() {
       info("browseWorkOrder")
       Constraint cCloseDate = MSF620Rec.closedDt.equalTo(" ")
       QueryImpl qWorkOrder = new QueryImpl(MSF620Rec.class).and(cCloseDate).orderBy(MSF620Rec.msf620Key)
       edoi.search(qWorkOrder, MAX_ROW_READ, {MSF620Rec msf620Rec->
           processReportA(msf620Rec)
           processReportB(msf620Rec)
           processReportC(msf620Rec)
           counter++
       })
       info("count data : "+counter.toString())
   }

   /**
    * Process MSFX6N to find project no with matching work order no. from input parameter.
    * compare the project's account code from MSF660 with msf620Rec.getDstrctAcctCode().substring(4)
    * print if the value not equals.
    * @param MSF620Rec Work Order record
    */
   private void processReportA(MSF620Rec msf620Rec){
       info("processReportA")
       String msfx6nProjectNo, msf620DstrctCode, projectAccCode
       msf620DstrctCode = msf620Rec.getPrimaryKey().getDstrctCode()
       try {
           Constraint cWorkOrder = MSFX6NKey.workOrder.equalTo(msf620Rec.getPrimaryKey().getWorkOrder())
           def query = new QueryImpl(MSFX6NRec.class).and(cWorkOrder)
           MSFX6NRec msfx6nrec = edoi.firstRow(query)
           if(msfx6nrec!=null){
               msfx6nProjectNo = msfx6nrec.getPrimaryKey().getProjectNo()
               projectAccCode = getProjectDetail(msf620DstrctCode, msfx6nProjectNo, "AC")
               if(!projectAccCode.equals("-")){
                   if(!msf620Rec.getDstrctAcctCode().substring(4).equals(projectAccCode))
                       writeReportA(projectAccCode, msfx6nProjectNo, msf620Rec )
               }
           } else info("project no. not found for this work order : "+ msf620Rec.getPrimaryKey().getWorkOrder())
       } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException ex){
           info("MSFX6N edoi error message: "+ex.message)
       }
   }

   /**
    * Process MSS900 to find Inactive account code
    * @param MSF620Rec Work Order record
    */
   private void processReportB(MSF620Rec msf620Rec){
       info("processReportB")
       boolean msf966StatusActive = false
       String msf620AccountCode = msf620Rec.getDstrctAcctCode().substring(4)
       if(!msf620AccountCode.trim().equals("")) {
           MSS900LINK mss900link = eroi.execute('MSS900',{mss900link ->
               mss900link.option900  = 'D'
               mss900link.costItem   = msf620AccountCode
           })
           if(!mss900link.errCode1.trim().equals("")) {
               writeReportB(msf620Rec)
           }
       }
   }

   /**
    * process MSFX6N to find project no with matching work order no. from input parameter.
    * compare today's date with ActFinDate, print report if actFinDate is empty or
    * not empty and actFinDate is equal or less than today's date.
    * @param MSF620Rec Work Order record
    */
   private void processReportC(MSF620Rec msf620Rec){
       info("processReportC")
       String msf620DstrctCode, msfx6nProjectNo, msf660ActFinDate
       msf620DstrctCode = msf620Rec.getPrimaryKey().getDstrctCode()
       Constraint cWorkOrder2 = MSFX6NKey.workOrder.equalTo(msf620Rec.getPrimaryKey().getWorkOrder())
       def query = new QueryImpl(MSFX6NRec.class).and(cWorkOrder2)
       try{
           MSFX6NRec msfx6nrec = edoi.firstRow(query)
           if(msfx6nrec!=null){
               java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd")
               String today = sdf.format(new Date())
               msfx6nProjectNo = msfx6nrec.getPrimaryKey().getProjectNo()
               msf660ActFinDate = getProjectDetail(msf620DstrctCode, msfx6nProjectNo, "AFD")
               if(!msf660ActFinDate.equals("-")){
                   if(!msf660ActFinDate.trim().equals("") && msf660ActFinDate.trim().compareTo(today)<=0)
                       writeReportC(msf620Rec, msfx6nProjectNo)
               }
           } else info("project no. not found for this work order : "+ msf620Rec.getPrimaryKey().getWorkOrder())
       } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e){
           info("ERROR MSFX6N : "+e.message)
       }
   }

   /**
    * write details to report A
    * @param MSF660 Account Code
    * @param MSF660 Project No
    * @param MSF620Rec
    */
   private void writeReportA(String msf660AccCode, String msf660ProjectNo, MSF620Rec msf620rec){
       info("writeReportA")
       String empName = "("+msf620rec.getOriginatorId()+") "+getEmployeeName(msf620rec.getOriginatorId())

       reportA.write(
               msf620rec.getPrimaryKey().getWorkOrder()?.trim()+","+
               '"'+msf620rec.getWoDesc()?.trim()+'"'+","+
               "'"+msf620rec.getDstrctAcctCode().substring(4)?.trim()+","+
               msf660ProjectNo?.trim()+","+
               "'"+msf660AccCode?.trim()+","+
               '"'+empName?.trim()+'"'
               )
       reportA.write("\n")
   }

   /**
    * write details to report B
    * @param MSF620Rec
    */
   private void writeReportB(MSF620Rec msf620rec){
       info("writeReportB")
       String empName = "("+msf620rec.getOriginatorId()+") "+getEmployeeName(msf620rec.getOriginatorId())
       reportB.write(
               msf620rec.getPrimaryKey().getWorkOrder()?.trim()+","+
               '"'+msf620rec.getWoDesc()?.trim()+'"'+","+
               "'"+msf620rec.getDstrctAcctCode().substring(4)?.trim()+","+
               '"'+empName?.trim()+'"'
               )
       reportB.write("\n")
   }

   /**
    * write details to report C
    * @param MSF620Rec
    * @param MSF660 Project No.
    */
   private void writeReportC(MSF620Rec msf620rec, String msf660ProjectNo){
       info("writeReportC")
       String empName = "("+msf620rec.getOriginatorId()+") "+getEmployeeName(msf620rec.getOriginatorId())
       reportC.write(
               msf620rec.getPrimaryKey().getWorkOrder()?.trim()+","+
               '"'+msf620rec.getWoDesc()?.trim()+'"'+","+
               msf660ProjectNo?.trim()+","+
               '"'+empName?.trim()+'"'
               )
       reportC.write("\n")
   }

   /**
    * call MSSEMPLINK to get Employee Name
    * @param MSF620 Originator ID
    */
   private String getEmployeeName(String msf620OriginatorID){
       info("getEmployeeName")
       String empName = " "
       try {
           MSF810Rec msf810Rec = edoi.findByPrimaryKey(new MSF810Key(msf620OriginatorID))
           empName = "${msf810Rec.surname.trim()}, ${msf810Rec.firstName.trim()}"
           if(!msf810Rec.secondName.trim().equals("")) {
               empName = "${empName} ${msf810Rec.secondName.trim()}"
           }
           if(!msf810Rec.thirdName.trim().equals("")) {
               empName = "${empName} ${msf810Rec.thirdName.trim()}"
           }
       }
       catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
       }
       return empName;
   }

   /**
    * Get project detail (account code or finished date) from MSF660
    * @param District Code
    * @param MSFX6N Project No.
    * @param Request ('AC' for Account Code, 'AFD' for ActFinDate)
    */
   private String getProjectDetail(String buffDstrctCode, String msfx6nProjectNo, String request) {
       info("getProjectDetail")
       String result = "-"
       try{
           MSF660Key msf660key = new MSF660Key()
           msf660key.dstrctCode = buffDstrctCode
           msf660key.projectNo = msfx6nProjectNo
           MSF660Rec msf660rec = edoi.findByPrimaryKey(msf660key)
           if(request.equals("AC")){
               result = msf660rec.getAccountCode()
               info("Account code retrieve: "+result)
           }
           if(request.equals("AFD")){
               result = msf660rec.getActFinDate()
               info("Act Fin Date retrieve : "+result)
           }
       } catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException ex){
           info("Project detail not found : "+ex.message)
       }
       return result
   }

   /**
    * print the batch report
    */
   private void printBatchReport(){
       info("printBatchReport")
       //print batch report
       reportA.close()
       if (taskUUID?.trim()) {
           info("Adding TRBPW2A into Request.")
           request.request.CURRENT.get().addOutput(newFileA,
                   "text/comma-separated-values", "TRBPW2A");
       }
       reportB.close()
       if (taskUUID?.trim()) {
           info("Adding TRBPW2B into Request.")
           request.request.CURRENT.get().addOutput(newFileB,
                   "text/comma-separated-values", "TRBPW2B");
       }
       reportC.close()
       if (taskUUID?.trim()) {
           info("Adding TRBPW2C into Request.")
           request.request.CURRENT.get().addOutput(newFileC,
                   "text/comma-separated-values", "TRBPW2C");
       }
   }
}

/*run script*/
ProcessTrbpw2 process = new ProcessTrbpw2();
process.runBatch(binding);