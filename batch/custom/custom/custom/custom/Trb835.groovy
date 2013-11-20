/**
 *   TRB835 - Sustenance Advances Reconciliation Report
 */

package com.mincom.ellipse.script.custom

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Key
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Rec
import java.text.DecimalFormat
import java.text.Format
import java.text.SimpleDateFormat

  public class ParamsTrb835 {
      // List of Input Parameters
      String paramPayLocation
      String paramPayWeekendEndDate
      
      //Restart Variables
      //String restartTableCode = "    "
  }
  
  public class ProcessTrb835 extends SuperBatch {
      /**
       * IMPORTANT!
       * Update this Version number EVERY push to GIT
       */
      private version = 3
      private ParamsTrb835 batchParams
      
      // Non-standard global variables
      private String sStartDate
      private String sEndDate
      private String sPayLocation
      private String sPayLocDesc
      private String sPrevPayLocation
      private Boolean bFirstTime = true
      
      private def Trb835a
      
      // Constants - values of the following variables must only be set here and nowhere else!!!
      private static final String PAY_GROUP = 'TG1'        // Pay Group to use when finding the default end date
      private static final String MISC_RPT_FLD_VAL = 'S0'  // Value to look for in the Earnings Code's MiscRptFld
      
      public void runBatch(Binding b) {
          init(b)
          
          printSuperBatchVersion()
          info("runBatch version : " + version)
          
          // Request Parameters
          batchParams = params.fill(new ParamsTrb835())
          info("paramPayLocation: " + batchParams.paramPayLocation)
          info("paramPayWeekendEndDate: " + batchParams.paramPayWeekendEndDate)
          
          // Setup request parameters. If parameters are valid then continue with the report
          if (processParams()) {
              // Request parameters are valid so continue
              
              // Create Report A
              List <String> headingsA = new ArrayList <String>()
              headingsA = setPageHeadings()
              Trb835a = report.open("TRB835A", headingsA)
              
              try {
                  processBatch()
              }
              finally {
                  printBatchReport()
              }
          }
          Trb835a.close()
      }
      
      private void processBatch() {
          info("processBatch")
          
          // Process only for specified pay location
          if (sPayLocation != "" && sPayLocation != null) {
              // Find employees in the pay location
              getEmployees (sPayLocation)
          }
          // Process for all pay locations
          else {
              // Find pay locations from PAYL table
              Constraint c1 = MSF010Key.tableType.equalTo("PAYL")
              def query = new QueryImpl(MSF010Rec.class).and(c1)
              
              MSF010Key msf010key = new MSF010Key()
              edoi.search(query,{MSF010Rec msf010RecRead ->
                  
                  sPayLocation = msf010RecRead.getPrimaryKey().getTableCode()
                  sPayLocDesc = msf010RecRead.tableDesc.trim()
                  
                  // Find employees in the pay location
                  getEmployees (sPayLocation)
              })
          }
      }
      
      // Set page headings
      private List setPageHeadings() {
          info("setPageHeadings")
          
          List <String> headings = new ArrayList <String>()
          //headings.add("Sustenance Advances Reconciliation Report".center(132))
          
          // Note line on the report
          Date dTempDate = new SimpleDateFormat("yyyyMMdd").parse(sEndDate)
          String sTempDate = new SimpleDateFormat ("dd/MM/yyyy").format(dTempDate)
          headings.add("Weekend Date: " + sTempDate + "    (Note: Report will be generated 13 weeks prior to the weekend date)")
          headings.add(String.format("%132s"," ").replace(" ", "-"))
          // Report column headings
          //          1         2         3         4         5         6         7         8         9        10        11        12        13
          // 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
          // Pay Location             Employee Id      Name                          Date       Code  Description            Units         Amount
          headings.add("Pay Location".padRight(25) +
                       "Employee Id".padRight(17) +
                       "Name".padRight(30) +
                       "Date".padRight(11) +
                       "Code".padRight(6) +
                       "Description".padRight(22) +
                       "Units".padLeft(6) +
                       "Amount".padLeft(15))
          
          return headings
      }
      
      // Validate and setup request parameters
      private boolean processParams() {
          info("processParams")
          
          Date dStartDate
          Date dEndDate
                      
          info("Validate Pay Location")
          // Validate pay location
          if (batchParams.paramPayLocation.trim() != "" && batchParams.paramPayLocation != null){
              sPayLocation = batchParams.paramPayLocation.trim().toString()
            
              // Validate the pay location
              try {
                  MSF010Key msf010key = new MSF010Key()
                  msf010key.setTableType("PAYL")
                  msf010key.setTableCode(sPayLocation)
                  MSF010Rec msf010RecRead = edoi.findByPrimaryKey(msf010key)
              
                  // Pay location is valid so get the description
                  sPayLocDesc = msf010RecRead.tableDesc.trim()
              }
              // Invalid pay location
              catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                  info("Invalid Pay Location : " + sPayLocation)
                  Trb835a.write("")
                  Trb835a.write("*** ERROR: Invalid Pay Location ***")
                  
                  return false
              }
          }
          // No pay location specified as request parameter
          else {
              sPayLocation = ""
          }
          
          // Set start and end dates
          info("Setup Start and End dates")
          
          // End date has not been supplied. Determine the last pay period end date and calculate the start date.
          if (batchParams.paramPayWeekendEndDate.trim() == "" || batchParams.paramPayWeekendEndDate == null) {
              info("No Weekend End Date supplied.")
              
              // Get last period end date for the pay group PAY_GROUP.
              // The period end date is always a Friday and is the same for all pay groups.
              try {
                  MSF801_PG_801Key msf801pgkey = new MSF801_PG_801Key()
                  msf801pgkey.setCntlRecType("PG")
                  msf801pgkey.setCntlKeyRest(PAY_GROUP)
                  MSF801_PG_801Rec msf801PGRecRead = edoi.findByPrimaryKey(msf801pgkey)
                          
                  // Convert string date into date type so date calculations can be performed
                  dEndDate = new SimpleDateFormat("yyyyMMdd").parse(msf801PGRecRead.prvEndDtPg)
                  dStartDate = dEndDate - 98
                  
                  // Convert date type back to string type
                  sEndDate = new SimpleDateFormat ("yyyyMMdd").format(dEndDate)
                  sStartDate = new SimpleDateFormat ("yyyyMMdd").format(dStartDate)
                  info("sStartDate : " + sStartDate)
                  info("sEndDate   : " + sEndDate)
                  
                  return true
              }
              catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
                  info("No valid dates found!")
                  Trb835a.write("")
                  Trb835a.write("*** ERROR: No valid dates found ***")
                  
                  return false
              }
          }
          // End date has been specified
          else {
              sEndDate = batchParams.paramPayWeekendEndDate.trim().toString()
              info("Weekend End Date supplied : " + sEndDate)
              
              // Check that the entered date is a Friday
              // Convert to Date format
              Date inputDate = new SimpleDateFormat("yyyyMMdd").parse(sEndDate)
              info("inputDate: " + inputDate)
              
              // Convert to Day of Week
              String sDayOfWeek = new SimpleDateFormat("EEEE").format(inputDate)
              info("sDayOfWeek is a " + sDayOfWeek)
              
              // Date is a Friday
              if (sDayOfWeek == "Friday") {
                  // Convert string date into date type so date calculations can be performed
                  dEndDate = new SimpleDateFormat("yyyyMMdd").parse(sEndDate)
                  dStartDate = dEndDate - 98
                                      
                  // Convert date type back to string type
                  sStartDate = new SimpleDateFormat ("yyyyMMdd").format(dStartDate)
                  info("sStartDate : " + sStartDate)
                  info("sEndDate   : " + sEndDate)
                  
                  return true
              }
              // Not a Friday
              else {
                  info(sEndDate + " is not a Friday!")
                  Trb835a.write("")
                  Trb835a.write("*** ERROR " + sEndDate + " is not a Friday ***")
                  
                  return false
              }
          }
      }
      
      /**
       * Get active employees within the pay location
       * @param sPayLocation
       */
      private void getEmployees(String sPayLocation) {
          // info ("getEmployees")
          
          // Constraints
          Constraint c1 = MSF820Rec.payLocation.equalTo(sPayLocation)
          Constraint c2 = MSF760Rec.empStatus.equalTo("A")
          
          // Query to find employees
          def validEmployees = new QueryImpl().
              join(MSF820Key.employeeId, MSF760Key.employeeId).
              columns([MSF820Key.employeeId, MSF820Rec.payLocation, MSF820Rec.payGroup]).
              and(c1).and(c2).
              nonIndexSortAscending(MSF820Rec.payLocation, MSF820Key.employeeId)
              
          // Get employees
          edoi.search(validEmployees).results.each {rec1 ->
              String sEmployeeId = rec1[0]
              String sPayLoc = rec1[1]
              String sPayGroup = rec1[2]
              
              //info("sEmployee Id : " + sEmployeeId)
              //info("sPayLoc      : " + sPayLoc)
              //info("sPayGroup    : " + sPayGroup)
              
              // Get pay transactions for the employee
              getPayTrans (sEmployeeId, sPayLoc, sPayGroup)
          }
      }
      
      /**
       * Get pay transactions for each valid employee within the date range
       * @param sEmployeeId
       * @param sPayLoc
       * @param sPayGroup
       */
      private void getPayTrans(String sEmployeeId, String sPayLoc, String sPayGroup) {
          // info ("getPayTrans")
          
          String sPrevEmpId
          // Ideally, a join between MSF835 and MSF80A should be used. Test data however revealed that
          // the two files are not always in sync. Rather than risk missing some earnings code records,
          // join MSF835 with MSF801_A_801 instead.
          
          // Constraints
          Constraint c1 = MSF835Key.employeeId.equalTo(sEmployeeId)
          Constraint c2 = MSF835Key.trnDate.greaterThanEqualTo(sStartDate)
          Constraint c3 = MSF835Key.trnDate.lessThanEqualTo(sEndDate)
          Constraint c4 = MSF801_A_801Key.cntlRecType.equalTo("A")
          StringConstraint c5 = MSF801_A_801Key.cntlKeyRest.substring(1,3).equalTo("***")
          StringConstraint c6 = MSF801_A_801Key.cntlKeyRest.substring(4,3).equalTo(MSF835Key.tranCode)
          Constraint c7 = MSF801_A_801Rec.miscRptFldAx1.equalTo(MISC_RPT_FLD_VAL)
          Constraint c8 = MSF801_A_801Rec.miscRptFldAx2.equalTo(MISC_RPT_FLD_VAL)
          Constraint c9 = MSF801_A_801Rec.miscRptFldAx3.equalTo(MISC_RPT_FLD_VAL)
          Constraint c10 = MSF801_A_801Rec.miscRptFldAx4.equalTo(MISC_RPT_FLD_VAL)
          Constraint c11 = MSF801_A_801Rec.miscRptFldAx5.equalTo(MISC_RPT_FLD_VAL)
          
          // Query to find valid pay transactions
          def validPayTrans = new QueryImpl().
              columns([MSF835Key.tranCode, MSF835Key.trnDate, MSF835Rec.trnUnits, MSF835Rec.amount, MSF801_A_801Rec.shortTnameA]).
              and(c1).and(c2).and(c3).and(c4).and(c5).and(c6).
              and((c7).or(c8).or(c9).or(c10).or(c11)).
              nonIndexSortAscending(MSF835Key.employeeId, MSF835Key.trnDate)
              
          // Get pay transaction details to write to report
          edoi.search(validPayTrans).results.each {rec2 ->
              String sTranCode = rec2[0]
              Date dTranDate = new SimpleDateFormat("yyyyMMdd").parse(rec2[1])
              String sTranDate = new SimpleDateFormat ("dd/MM/yy").format(dTranDate)
              String sTranUnits = new DecimalFormat("#.0000").format(rec2[2])
              String sAmount = new DecimalFormat("#.00").format(rec2[3])
              String sDesc = rec2[4]
              String sPayLocn = (sPayLoc + " " + sPayLocDesc)
              
              // Get employee name
              MSF810Key msf810key = new MSF810Key()
              msf810key.setEmployeeId(sEmployeeId)
              MSF810Rec msf810RecRead = edoi.findByPrimaryKey(msf810key)
              
              String sEmpName = msf810RecRead.surname.trim()
              if (msf810RecRead.firstName != "") {
                  sEmpName += " " + msf810RecRead.firstName.substring(0,1)
              }
              if (msf810RecRead.secondName != "") {
                  sEmpName += " " + msf810RecRead.secondName.substring(0,1)
              }
                              
              //info("sPayLocn    : " + sPayLocn)
              //info("sEmployeeId : " + sEmployeeId)
              //info("sEmpName    : " + sEmpName)
              //info("sTranDate   : " + sTranDate)
              //info("sTranCode   : " + sTranCode)
              //info("sDesc       : " + sDesc)
              //info("sTranUnits  : " + sTranUnits)
              //info("sAmount     : " + sAmount)
              
              // Print the page headings when the Pay Location changes. We have to do this at this point
              // because here we know there is a transaction for the employee. We only want to print the
              // page heading if the pay group has employees with transactions against them.
              if (sPrevPayLocation != sPayLoc) {
                  if (bFirstTime) {
                      bFirstTime = false
                  } else {
                      Trb835a.newPage()
                  }
                  sPrevPayLocation = sPayLoc
              }
              
              // Write a blank line when the employee id changes - actually every time we come into this
              // method the employee id will be different from the previous one, we only do this because
              // we want to make sure the blank line is printed only once
              if (sPrevEmpId != "" && sPrevEmpId != sEmployeeId) {
                  sPrevEmpId = sEmployeeId
                  Trb835a.write("")
              }
              
              // Write report details
              Trb835a.write(sPayLocn.padRight(25) +
                            sEmployeeId.padRight(17) +
                            sEmpName.padRight(30) +
                            sTranDate.padRight(11) +
                            sTranCode.padRight(6) +
                            sDesc.padRight(22) +
                            sTranUnits.padLeft(7) +
                            sAmount.padLeft(14))
          }
      }
      
      // Print the report
      private void printBatchReport() {
          info("printBatchReport")
          //print batch report
      }
  }
      
/*run script*/
ProcessTrb835 process = new ProcessTrb835()
process.runBatch(binding)

