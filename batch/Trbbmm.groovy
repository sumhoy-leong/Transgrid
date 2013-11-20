/*
 * BRANDON'S TESTING PROGRAM FOR VARIOUS THINGS
 *
 */
package com.mincom.ellipse.script.custom;

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Formatter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException

import com.mincom.ellipse.edoi.ejb.msf857.MSF857Key
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Rec

import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException
import com.mincom.ellipse.edoi.common.exception.EDOIDuplicateKeyException


public class ParamsTrbbmm {
//List of Input Parameters
    String paramBmm;
}

public class ProcessTrbbmm extends SuperBatch {

   private final version = 00 // Update this number every push to GitHub
   
   
   private ParamsTrbbmm batchParams
   
   
   Date dToday = new Date()
   
   private def Trbbmmb // Detail Report
   
   public void runBatch(Binding b) {
       info("runBatch Version : " + version)
       
       init(b)
       
       // Request parameters
       batchParams = params.fill(new ParamsTrbbmm())
       processBatch()
   }
   
   /**
    * Execute the concatenation
    * @param parameters
    */
   private void processBatch() {
       info("processBatch")
       
       Boolean bIoError = false
       
       ioReports("open")
       
       getLabourTransactionsA()
       
       ioReports("close")
   }
   
   
   /**
    * Create, close reports
    * @param io
    */
   private void ioReports(def io) {
       info("ioReports")
       
       switch (io) {
       case "open":
           List <String> headingsB = new ArrayList <String>()
           headingsB = writeReportBHeader()
           Trbbmmb = report.open("TRBBMMB", headingsB)
           break
           
       case "close":
           Trbbmmb.close()
           break
           
       default:
           info("##### INVALID IO OPERATION #####")
           break
       }
   }
   
   private void getLabourTransactionsA() {
       info("getLabourTransactionsA")

       Integer i = 0

       Constraint c1 = MSF857Rec.postingStatus.equalTo("PO")
       Query queryMsf857 = new QueryImpl(MSF857Rec.class).and(c1).
           orderBy(MSF857Rec.msf857Key)
       
       Trbbmmb.write("Sort MSF857 by Primary Key")
       edoi.search(queryMsf857,{MSF857Rec msf857rec ->
           
           i++
           Trbbmmb.write(i + "  " + msf857rec.getPrimaryKey().getDstrctCode() + "  " +
                         msf857rec.getPrimaryKey().getEmployeeId() + "  " +
                         msf857rec.getPrimaryKey().getLabClassEarn().substring(6) + "  " +
                         msf857rec.getLabBatchNo() + "  " +
                         msf857rec.getPostingStatus() + "  " +
                         msf857rec.getWorkOrder() + "  " +
                         msf857rec.getEquipNo() + "  " +
                         msf857rec.getSubledgerType() + msf857rec.getSubledgerAcct() + "  " +
                         new SimpleDateFormat("yyyyMMdd").parse(msf857rec.getPrimaryKey().getLabTranDate()).
                         format("dd/MM/yyyy") + "  " +
                         msf857rec.getProjectNo() + "  " +
                         msf857rec.getLabTranDesc() + "  " +
                         msf857rec.getLabRate() + "  " +
                         msf857rec.getLabTranHours())
       })
       Trbbmmb.newPage()

       i = 0

       Query queryMsf857A = new QueryImpl(MSF857Rec.class).and(c1).
           nonIndexSortAscending(MSF857Rec.labBatchNo, MSF857Key.dstrctCode, MSF857Key.employeeId,
               MSF857Key.labTranDate, MSF857Key.labClassEarn, MSF857Key.labTranSeq)
      
       Trbbmmb.write("Sort MSF857 by Lab Batch No and Primary Key")
       edoi.search(queryMsf857A,{MSF857Rec msf857recA ->
           
           i++
           Trbbmmb.write(i + "  " + msf857recA.getPrimaryKey().getDstrctCode() + "  " +
                         msf857recA.getPrimaryKey().getEmployeeId() + "  " +
                         msf857recA.getPrimaryKey().getLabClassEarn().substring(6) + "  " +
                         msf857recA.getLabBatchNo() + "  " +
                         msf857recA.getPostingStatus() + "  " +
                         msf857recA.getWorkOrder() + "  " +
                         msf857recA.getEquipNo() + "  " +
                         msf857recA.getSubledgerType() + msf857recA.getSubledgerAcct() + "  " +
                         new SimpleDateFormat("yyyyMMdd").parse(msf857recA.getPrimaryKey().getLabTranDate()).
                         format("dd/MM/yyyy") + "  " +
                         msf857recA.getProjectNo() + "  " +
                         msf857recA.getLabTranDesc() + "  " +
                         msf857recA.getLabRate() + "  " +
                         msf857recA.getLabTranHours())
       })
       Trbbmmb.newPage()

       i = 0

       Query queryMsf857B = new QueryImpl(MSF857Rec.class).and(c1)
      
       Trbbmmb.write("No sort order specified on MSF857")
       edoi.search(queryMsf857B,{MSF857Rec msf857recB ->
           
           i++
           Trbbmmb.write(i + "  " + msf857recB.getPrimaryKey().getDstrctCode() + "  " +
                         msf857recB.getPrimaryKey().getEmployeeId() + "  " +
                         msf857recB.getPrimaryKey().getLabClassEarn().substring(6) + "  " +
                         msf857recB.getLabBatchNo() + "  " +
                         msf857recB.getPostingStatus() + "  " +
                         msf857recB.getWorkOrder() + "  " +
                         msf857recB.getEquipNo() + "  " +
                         msf857recB.getSubledgerType() + msf857recB.getSubledgerAcct() + "  " +
                         new SimpleDateFormat("yyyyMMdd").parse(msf857recB.getPrimaryKey().getLabTranDate()).
                         format("dd/MM/yyyy") + "  " +
                         msf857recB.getProjectNo() + "  " +
                         msf857recB.getLabTranDesc() + "  " +
                         msf857recB.getLabRate() + "  " +
                         msf857recB.getLabTranHours())
       })
   }
   
   /**
    * Report B header
    */
   private List writeReportBHeader() {
       info("writeReportBHeader")
       
       List <String> headings = new ArrayList <String>()
       headings.add("BMM Test".center(132))
       
       return headings
   }
   
   /**
    * List all errors encountered to the log after running a web service
    * @param e Error Object
    * @return None
    */
   private void listErrors(EnterpriseServiceOperationException e) {
       info("listErrors")
       
       List <ErrorMessageDTO> listError = e.getErrorMessages()
       listError.each{ErrorMessageDTO errorDTO ->
           info("Error Code: " + errorDTO.getCode())
           info("Error Message: " + errorDTO.getMessage())
           info("Error Fields: " + errorDTO.getFieldName())
       }
   }
}

/*run script*/
ProcessTrbbmm process = new ProcessTrbbmm()
process.runBatch(binding)


