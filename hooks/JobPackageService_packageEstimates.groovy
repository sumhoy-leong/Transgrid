/**
 * @author Ventyx 2013
 *
 * onPostExecute:
 *     Find all Work Orders associated with the Job Estimate just packaged.
 *     Update the WO Type, Maintenance Type, Account Code and Project Number
 *     of all the Work Orders based on the "Costing Solution".
 */

import com.mincom.ellipse.edoi.ejb.msf080.*
import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.eql.impl.*
import com.mincom.eql.*

import com.mincom.ellipse.edoi.ejb.msfx55.MSFX55Key
import com.mincom.ellipse.edoi.ejb.msfx55.MSFX55Rec

import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.jobpackage.JobPackageServicePackageEstimatesRequestDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.standardjob.StandardJobServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceModifyReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceModifyRequestDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*

import java.text.SimpleDateFormat
import java.util.Calendar

public class JobPackageService_packageEstimates extends ServiceHook {
   
 /*
  * IMPORTANT!
  * Update this Version number EVERY push to GIT
  */
   
 private String version = "1"
 
 @Override
  public Object onPreExecute(Object dto) {
      log.info("JobPackageService_packageEstimates onPreExecute - version: ${version}")
      
      // Nothing to do here
      
      return null
  }
  
  @Override
  public Object onPostExecute(Object input, Object result) {
      log.info("JobPackageService_packageEstimates onPostExecute - version: ${version}")
      
      JobPackageServicePackageEstimatesRequestDTO jobPkgRequest = input	  
      String sJobEstimateNo = jobPkgRequest.getJobEstimatedNo().trim()
      String sVersionNo = jobPkgRequest.getJobEstimatedVersionNo().trim()
      //String sItemNo = String.format("%06d", jobPkgRequest.getJobEstimatedItmNo().toString())
      
      // Get all WOs for the Job Estimate
      if (!sJobEstimateNo.equals("") && !sJobEstimateNo.equals(null)) {
          // Find pay locations from PAYL table
          Constraint c1 = MSFX55Key.estimateNo.equalTo(sJobEstimateNo)
          Constraint c2 = MSFX55Key.classX55Type.equalTo("WO")
          Constraint c3 = MSFX55Rec.versionNo.equalTo(sVersionNo)
          def query = new QueryImpl(MSFX55Rec.class).and(c1).and(c2).and(c3)
              
          MSFX55Key msfx55key = new MSFX55Key()
          tools.edoi.search(query,{MSFX55Rec msfx55RecRead ->
              
              // Make sure the key to reading the Work Order details is complete
              if (msfx55RecRead.getPrimaryKey().getClassX55Key().size() >= 12) {
                  String sDistrictCode = msfx55RecRead.getPrimaryKey().getClassX55Key().substring(0,4)
                  String sWorkOrder = msfx55RecRead.getPrimaryKey().getClassX55Key().substring(4,12)
                  
                  // Test the work order if the costing solution can be applied
                  checkWorkOrder (sDistrictCode, sWorkOrder)
              }
          })
      }
      return result
  }
  
  /**
   * Determine if the costing solution can be applied to the Work Order
   * @param objWorkOrder Work Order Object
   */
  private void checkWorkOrder (String sDistrictCode, String sWorkOrder) {
      log.info("checkWorkOrder")
      
      try {
          // Get the WO details
          WorkOrderServiceReadReplyDTO objWorkOrder = tools.service.get('WorkOrder').read({
              it.districtCode = sDistrictCode
              it.workOrder = new WorkOrderDTO(sWorkOrder)})
          
          String sAccountCode = objWorkOrder.getAccountCode()
          String sProjectNo = objWorkOrder.getProjectNo()
          String sWorkGroup = objWorkOrder.getWorkGroup()
          String sWOType = objWorkOrder.getWorkOrderType()
          String sMaintType = objWorkOrder.getMaintenanceType()
          String sStdJobNo = objWorkOrder.getStdJobNo()
          String sEquipRef = objWorkOrder.getEquipmentRef()
          // Use Equipment Number if Reference is not provided
          if (sEquipRef.equals("") || sEquipRef.equals(null)) {
              sEquipRef = objWorkOrder.getEquipmentNo()
          }
          
          // Attempt to apply the costing solution
          if (!(checkMandatoryInputs(sEquipRef, sWorkGroup))) {
              log.info("Mandatory inputs Equipment Reference and Work Group were not supplied. " +
                  "Costing solution does not apply.")
          } else {
              // If the WO has a Standard Job, use the WO Type and Maintenance Type from the Standard Job
              if (!(sStdJobNo.equals("")) && !(sStdJobNo.equals(null))) {
                  // Get Standard Job details
                  try {
                      StandardJobServiceReadReplyDTO stdJobReply = tools.service.get('StandardJob').read({
                          it.districtCode = sDistrictCode
                          it.standardJob = sStdJobNo})
                      
                      // Use WO Type and Maint Type from Standard Job instead when applying the costing solution
                      if (!stdJobReply.getWorkOrderType().equals("") && !stdJobReply.getWorkOrderType().equals(null)) {
                          sWOType = stdJobReply.getWorkOrderType().trim()
                      }
                      if (!stdJobReply.getMaintenanceType().equals("") && !stdJobReply.getMaintenanceType().equals(null)) {
                          sMaintType = stdJobReply.getMaintenanceType().trim()
                      }
                  } catch (EnterpriseServiceOperationException e) {
                      listErrors(e)
                  }
              }
              
              /*
               * At this stage we either have WO and MT from the Standard Job or have a WO and MT from the WO itself
               * or no WO and MT at all. If it is the latter then the costing solution cannot continue since it relies
               * on the WOMT combination among other things.
               */
              if ((sWOType.equals("")) || (sMaintType.equals("")) || (sWOType.equals(null)) || (sMaintType.equals(null))) {
                  log.info("WOMT key information is not provided or cannot be derived. Costing solution does not apply.")
              } else {
                  Boolean bCsmFound = true
                  String sCsmName = ""
                  String sWOMT = sWOType + sMaintType
                  
                  (bCsmFound, sCsmName) = checkTable("+CSM", sWOMT)
                  
                  if (!bCsmFound) {
                      // WOMT combination is not in the +CSM table so costing solution does not apply
                      log.info("WOMT Combination "  + sWOMT + " is not in +CSM. Costing solution does not apply.")
                  } else {
                      // WOMT combination is in the +CSM table so now validate the combination against the WOMT table
                      Boolean bWomtFound = true
                      String sWomtName = ""
                      
                      (bWomtFound, sWomtName) = checkTable("WOMT", sWOMT)
                      
                      if (!bWomtFound) {
                          // WOMT is not a valid combination so error
                          log.info("WOMT Combination "  + sWOMT + " is invalid. Costing solution will not be applied.")
                      } else {
                          // Attempt to apply the costing solution to default a Project and Account Code to the WO
                          
                          // Validate Equipment and get Equipment Classifications 0 - 2
                          Boolean bEquipFound = true
                          String sEC0 = ""
                          String sEC1 = ""
                          String sEC2 = ""
                          
                          (bEquipFound, sEC0, sEC1, sEC2) = checkEquipment(sEquipRef)
                          
                          if (!bEquipFound) {
                              // Equipment not found so error
                              log.info("Equipment " + sEquipRef + " not found. Costing solution will not be applied.")
                          } else {
                              // Check against Costing Table +CST and get Project Number from the table description
                              Boolean bCstFound = true
                              String sProjNo = ""
                              String sCST = sEC0 + sEC1 + sEC2 + sWOMT + sWorkGroup
                              
                              (bCstFound, sProjNo) = checkTable ('+CST', sCST)
                              
                              if (!bCstFound) {
                                  // Combination if fields not in +CST Table
                                  log.info("+CST entry " + sCST + " not found. Costing solution will not be applied.")
                              } else if (sProjNo.equals("")) {
                                  // +CST entry does not have a Project Number
                                  log.info("+CST entry " + sCST + " does not have a Project Number. " +
                                      "Costing solution will not be applied.")
                              } else {
                                  // Validate the Project Number and get the Project Account Code
                                  Boolean bProjectFound = true
                                  String sAcctCode = ""
                                  
                                  (bProjectFound, sAcctCode) = checkProject (sProjNo)
                                  
                                  if (!bProjectFound) {
                                      // Project Number not found
                                      log.info("Project Number " + sProjNo + " not found. Costing solution will not be applied.")
                                  } else {
                                      if ((sAccountCode != sAcctCode) || (sProjectNo != sProjNo)) {
                                          // update the project number and account code of the Work Order
                                          updateWorkOrder (sDistrictCode, sWorkOrder, sAcctCode, sProjNo, sWOType, sMaintType)
                                      }
                                  }
                              }
                          }
                      }
                  }
              }
          }
      } catch (EnterpriseServiceOperationException e){
          listErrors(e)
      }
  }
  
  /**
   * Update the Work Order with the correct values based on the costing solution formula
   * @param sWorkOrder Work Order
   * @param sAccountCode Account Code
   * @param sProjNo Project Number
   * @param sWoType Work Order Type
   * @param sMaintType Maintenance Type
   */
  private void updateWorkOrder (String sDistrictCode, String sWorkOrder, String sAccountCode, String sProjNo, String sWoType, String sMaintType) {
      log.info("updateWorkOrder")
      
      try{
          WorkOrderServiceModifyReplyDTO woReplyDTO = tools.service.get('WorkOrder').modify({
              WorkOrderServiceModifyRequestDTO it->
                  it.districtCode = sDistrictCode
                  it.workOrder = new WorkOrderDTO(sWorkOrder)
                  it.workOrderType = sWoType
                  it.maintenanceType = sMaintType
                  it.accountCode = sAccountCode
                  it.projectNo = sProjNo
              },false)
      } catch(EnterpriseServiceOperationException e){
          listErrors(e)
      }
  }
  
  /**
   * Validate the Project Number and return the Account Code
   * @param sProjNo Project Number
   * @return Project Found TRUE/FALSE, Project Account Code
   */
  private def checkProject (String sProjNo) {
      log.info("checkProject")
      
      Boolean bProjectFound = false
      String sAccountCode = ""
      
      try {
          ProjectServiceReadReplyDTO projectReply = tools.service.get('Project').read({
              it.projectNo = sProjNo})
          
          bProjectFound = true
          sAccountCode = projectReply.getAccountCode().trim()
      } catch (EnterpriseServiceOperationException e){
          listErrors(e)
      }
      return [bProjectFound, sAccountCode]
  }
  
  /**
   * Validate Table Entry and return the Table Description
   * @param sTableType Table Type
   * @param sTableCode Table Code
   * @return Table Entry Found TRUE/FALSE, Table Description
   */
  private def checkTable (String sTableType, String sTableCode) {
      log.info("checkTable")
      
      Boolean bTableFound = false
      String sTableDesc = ""
      
      try {
          TableServiceReadReplyDTO tableReply = tools.service.get('Table').read({
              it.tableType = sTableType
              it.tableCode = sTableCode})
          
          bTableFound = true
          sTableDesc = tableReply.getDescription().trim()
      } catch (EnterpriseServiceOperationException e){
          listErrors(e)
      }
      return [bTableFound, sTableDesc]
  }
  
  /**
   * Validate Equipment Number and return the first 3 Equipment Classifications
   * @param sEquipRef Equipment Reference
   * @return Equipment Found TRUE/FALSE, Equipment Classifications 0, 1, 2
   */
 private def checkEquipment(String sEquipRef) {
     log.info("checkEquipment")
     
     Boolean bEquipFound = false
     String sEC0 = ""
     String sEC1 = ""
     String sEC2 = ""
     
     try {
         // Get Equipment details that will form part of the key for +CST lookup
         EquipmentServiceReadReplyDTO equipReply = tools.service.get('Equipment').read({
             it.equipmentRef = sEquipRef})
         
         bEquipFound = true
         
         sEC0 = equipReply.getEquipmentClassif0().trim()
         if (sEC0 == "") {
             sEC0 = "  "
         }
         sEC1 = equipReply.getEquipmentClassif1().trim()
         if (sEC1 == "") {
             sEC1 = "  "
         }
         sEC2 = equipReply.getEquipmentClassif2().trim()
         if (sEC2 == "") {
             sEC2 = "  "
         }
     } catch (EnterpriseServiceOperationException e){
         listErrors(e)
     }
     return [bEquipFound, sEC0, sEC1, sEC2]
 }
  /**
   * Check that the Equipment Reference and the Work Group have been supplied
   * @param sEquipRef Equipment Reference
   * @param sWorkGroup Work Group
   * @return Mandatory Inputs Supplied TRUE/FALSE
   */
  private boolean checkMandatoryInputs (String sEquipRef, String sWorkGroup) {
      log.info("checkMandatoryInputs")
      
      Boolean bInputSupplied = true
      
      // Check Equipment is supplied
      if (sEquipRef.equals("") || sEquipRef.equals(null)) {
          log.info("Equipment Reference was not supplied. Costing Solution does not apply.")
          bInputSupplied = false
      }
      // Check Work Group is supplied
      if (sWorkGroup.equals("") || sWorkGroup.equals(null)) {
          log.info("Work Group was not supplied. Costing Solution does not apply.")
          bInputSupplied = false
      }
      return bInputSupplied
  }
  
  /**
   * Write all errors to the log
   * @param errorObject Error Object
   */
  private void listErrors (EnterpriseServiceOperationException errorObject) {
      log.info("listErrors")
      
      List <ErrorMessageDTO> listError = errorObject.getErrorMessages()
      listError.each{ErrorMessageDTO errorDTO ->
          log.info ("Error Code: " + errorDTO.getCode())
          log.info ("Error Message: " + errorDTO.getMessage())
          log.info ("Error Fields: " + errorDTO.getFieldName())
      }
  }
}

