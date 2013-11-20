/**
 * @author Ventyx 2012
 *
 * Pre-Create:
 *     Apply the costing solution for qualifying Work Orders to obtain a default
 *     Project Number and Account Code.
 *     
 *     Note: This will only work if a Work Order is created from scratch. This will
 *     not work when creating a WO using the COPY feature. In MSEWOT, the COPY feature
 *     is done using the "Save As" button. This feature only accepts a District Code
 *     and existing Work Order number as input. All other inputs are ignored. Any attempt
 *     to change the input even via a hook will fail.
 *     
 * Post-Create:
 *     When a call out Work Order is created, send an email notification to the
 *     Work Group Team Leader by creating a request for TRBWOE.
 */

import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.edoi.ejb.msf080.*
import com.mincom.ellipse.edoi.ejb.msf610.*
import com.mincom.ellipse.edoi.ejb.msfx69.*
import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.batchrequest.BatchRequestServiceCreateReplyDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.standardjob.StandardJobServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import java.text.SimpleDateFormat
import java.util.Calendar

public class WorkOrderService_create extends ServiceHook {
  
/*
 * IMPORTANT!
 * Update this Version number EVERY push to GIT
 */
  
 private String version = "12"

 @Override
 public Object onPreExecute(Object dto) {
     log.info("WorkOrderService_create onPreExecute - version: ${version}")

     
     WorkOrderServiceCreateRequestDTO request = dto
     
     String sDistrictCode = request.getDistrictCode()
     String sWorkGroup = request.getWorkGroup()
     String sWOType = request.getWorkOrderType()
     String sMaintType = request.getMaintenanceType()
     String sStdJobNo = request.getStdJobNo()
     String sEquipRef = request.getEquipmentRef()
     String sEquipNo = request.getEquipmentNo()
     // Use Equipment Number if Equipment Reference is not supplied
     if (sEquipRef == "" || sEquipRef == null) {
         sEquipRef = sEquipNo
     }
     
     // Check if the costing solution applies
     if (!(checkMandatoryInputs(sEquipRef, sWorkGroup))) {
         log.info("Mandatory inputs Equipment Reference and Work Group were not supplied. Costing solution does not apply.")
     } else {
         // If the WO has a Standard Job, use the WO Type and Maintenance Type from the Standard Job
         if (!(sStdJobNo.equals("")) && !(sStdJobNo.equals(null))) {
             // Get Standard Job details
             try {
                 StandardJobServiceReadReplyDTO stdJobReply = tools.service.get('StandardJob').read({
                     it.districtCode = sDistrictCode
                     it.standardJob = sStdJobNo})
                 
                 // Use WO Type and Maint Type from Standard Job instead when applying the costing solution
                 // WO does not have a WO Type so default from the Standard Job
                 if (sWOType.equals("") || sWOType.equals(null)) {
                     request.setWorkOrderType(stdJobReply.getWorkOrderType())
                 }
                 // WO does not have a Maintenance Type so default from the Standard Job
                 if (sMaintType.equals("") || sMaintType.equals(null)) {
                     request.setMaintenanceType(stdJobReply.getMaintenanceType())
                 }
                 sWOType = stdJobReply.getWorkOrderType()
                 sMaintType = stdJobReply.getMaintenanceType()
             } catch (EnterpriseServiceOperationException e) {
                 listErrors(e)
             }
         }
         
         /*
          * At this stage we either have WO and MT from the Standard Job or have a WO and MT from the WO itself
          * or no WO and MT at all. If it is the latter then the costing solution cannot continue since it relies
          * on the WOMT combination among other things. So let the standard service handle the absence of WO and MT.
          */
         if ((sWOType.equals("")) || (sMaintType.equals("")) || (sWOType.equals(null)) || (sMaintType.equals(null))) {
             log.info("WOMT key information is not provided or cannot be derived. Costing solution does not apply.")
         } else {
             // Validate Equipment and get Equipment Classifications 0 - 2 and EGI
             Boolean bEquipFound = false
             String sEC0 = ""
             String sEC1 = ""
             String sEC2 = ""
             String sEGI = ""
             
             (bEquipFound, sEC0, sEC1, sEC2, sEGI, sEquipNo) = checkEquipment(sEquipRef)
             
             if (!bEquipFound) {
                 // Equipment not found
                 log.error("Equipment " + sEquipRef + " not found.")
                 //throw new RuntimeException("Equipment " + sEquipRef + " not found.")
                 throw new EnterpriseServiceOperationException(
                     new ErrorMessageDTO("", "Equipment ${sEquipRef} not found.", "equipmentRef", 0, 0))
             }
             
             // Check the entered Equipment's profile and see if it is set to raise the WO against
             // the installation equipment (immediate parent) instead.
             String sInstallEquip = determineEquipToUse(sEquipRef, sEquipNo, sEGI)
             if (!sInstallEquip.equals("") && !sInstallEquip.equals(null)) {
                 log.info("Equipment to use for this WO is ${sInstallEquip}")
                 sEquipRef = sInstallEquip
                 
                 // Validate installation equipment and get Equipment Classifications 0 - 2
                 bEquipFound = false
                 sEC0 = ""
                 sEC1 = ""
                 sEC2 = ""
                 sEGI = ""
                 
                 (bEquipFound, sEC0, sEC1, sEC2, sEGI, sEquipNo) = checkEquipment(sEquipRef)
                 if (!bEquipFound) {
                     // Equipment not found
                     log.error("Installation Equipment ${sEquipRef} not found.")
                     //throw new RuntimeException("Installation Equipment ${sEquipRef} not found.")
                     throw new EnterpriseServiceOperationException(
                         new ErrorMessageDTO("", "Installation Equipment ${sEquipRef} not found.", "", 0, 0))
                 }
             }
             
             // Validate WO Type and Maintenance Type combination against +CSM
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
                     log.error("WOMT Combination "  + sWOMT + " is invalid.")
                     //throw new RuntimeException("WOMT Combination "  + sWOMT + " is invalid.")
                     throw new EnterpriseServiceOperationException(
                         new ErrorMessageDTO("", "WOMT Combination ${sWOMT} is invalid.", "", 0, 0))
                 }
                 
                 // Attempt to apply the costing solution to default a Project and Account Code to the WO
                 
                 // Check against Costing Table +CST and get Project Number from the table description
                 Boolean bCstFound = true
                 String sProjNo = ""
                 String sCST = sEC0 + sEC1 + sEC2 + sWOMT + sWorkGroup
                 
                 log.info("+CST Table Code to use : " + sCST)
                 (bCstFound, sProjNo) = checkTable ('+CST', sCST)
                 
                 // Combination if fields not in +CST Table
                 if (!bCstFound) {
                     log.error("+CST entry " + sCST + " not found.")

log.info("how_to_use_edoi_search 02")
how_to_use_edoi_search()
log.info("lwh return null value here")
return null;
                     //throw new RuntimeException("+CST entry not found.")
//                     throw new EnterpriseServiceOperationException(
//                         new ErrorMessageDTO("", "+CST entry not found.", "", 0, 0))
//					 throw new Exception("lwhtesting_exception");
                 }
                 
                 // +CST entry does not have a Project Number
                 if (sProjNo.equals("")) {
                     log.error("+CST entry " + sCST + " does not have a Project Number.")
                     //throw new RuntimeException("+CST entry does not have a Project Number.")
                     throw new EnterpriseServiceOperationException(
                         new ErrorMessageDTO("", "+CST entry does not have a Project Number.", "", 0, 0))
                 }
                 
                 // Validate the Project Number and get the Project Account Code
                 Boolean bProjectFound = true
                 String sAccountCode = ""
                 
                 (bProjectFound, sAccountCode) = checkProject (sProjNo)
                 
                 if (!bProjectFound) {
                     // Project Number not found
                     log.error("Project Number " + sProjNo + " not found.")
                     //throw new RuntimeException("Project Number " + sProjNo + " not found.")
                     throw new EnterpriseServiceOperationException(
                         new ErrorMessageDTO("", "Project Number ${sProjNo} not found.", "", 0, 0))
                 }
                 
                 // default the project number and account code onto the Work Order
                 log.info("Costing Solution - defaulting Project Number to " + sProjNo)
                 log.info("Costing Solution - defaulting Account Code to " + sAccountCode)
                 request.setProjectNo(sProjNo)
                 request.setAccountCode(sAccountCode)
                 
                 // set the equipment reference & equipment number in case it was determined that
                 // WO should be against the parent
                 log.info("Costing Solution - setting the Equipment Ref to " + sEquipNo)
                 request.setEquipmentRef(sEquipNo)
                 request.setEquipmentNo(sEquipNo)
             }
         }
     }
     return null
 }
 
 @Override
 public Object onPostExecute(Object input, Object result) {
     log.info("WorkOrderService_create onPostExecute - version: ${version}")
     
     WorkOrderServiceCreateReplyDTO reply = result
     String sDistrictCode = reply.getDistrictCode()
     String sWorkOrder = reply.getWorkOrder().toString()
     String sWorkGroup = reply.getWorkGroup().toString()
     String sJobCode5 = reply.getJobCode5().toString()
     
     // Send an email only if the Work Group is used and the W4 Job Code (JobCode5) is 'COUTO'
     if (!(sWorkGroup.equals("")) && (sJobCode5.equals("COUTO"))) {
         // Find the Work Group's Team Leader
         String sTeamLeader = getTeamLeader(sDistrictCode, sWorkGroup)
         
         // Work Group has a Team Leader
         if (sTeamLeader != "") {
             // Find the Team Leader's Email Address
             String sEmailAddress = getEmailAddress(sTeamLeader)
             
             // Team Leader has a valid email address, request TRBWOE
             if (sEmailAddress != "") {
                 requestEmailBatch(sDistrictCode, sWorkOrder)
             }
         }
     }
     return result
 }

 /**
  * Get the Work Group Team Leader
  * @param sWorkGroup Work Group
  * @return Work Group Team Leader's Employee Id
  */
 private String getTeamLeader(String sDistrictCode, String sWorkGroup) {
     log.info("getTeamLeader ${sDistrictCode} ${sWorkGroup}")
     
     String sTeamLeader = ""
     
     // Get Work Group Team Leader
     try {
         WorkGroupServiceReadReplyDTO workGroupReply = tools.service.get('WorkGroup').read({
             it.districtCode = sDistrictCode
             it.workGroup = sWorkGroup})
         
         if (workGroupReply.getSupervisor().equals("") ||
             workGroupReply.getSupervisor().equals(null)){
             // Work Group does not have a team leader
             log.info("Team Leader for " + sWorkGroup + " not found!")
         } else {
             // Work Group has a Team Leader
             sTeamLeader = workGroupReply.getSupervisor().trim()
         }
     } catch (EnterpriseServiceOperationException e){
         listErrors(e)
     }
     return sTeamLeader
 }
 
 /**
  * Get Team Leader's Email Address
  * @param sTeamLeader Work Group Team Leader's Employee Id
  * @return Team Leader's Email Address
  */
 private String getEmailAddress(String sTeamLeader) {
     log.info("getEmailAddress ${sTeamLeader}")
     
     String sEmailAddress = ""
     String sTempEmailAddress = ""
     Boolean validEmail = false
     
     // Get Team Leader's Employee record
     try {
         EmployeeServiceReadReplyDTO empReply = tools.service.get('Employee').read({
             it.employee = sTeamLeader})
         
         // Team Leader does not have an Email Address
         if (empReply.getEmailAddress().equals("") ||
             empReply.getEmailAddress().equals(null)) {
             log.info("Team Leader " + sTeamLeader + " does not have an email address!")
         }
         // Team Leader has an Email Address
         else {
             sTempEmailAddress = empReply.getEmailAddress().trim()
             
             // Validate the format of the email address
             validEmail = (sTempEmailAddress ==~ /[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\.[A-Za-z0-9]+)*(\.[A-Za-z]{2,})/)
             
             if (validEmail) {
                 // Email Address is valid
                 sEmailAddress = sTempEmailAddress
             } else {
                 // Email Address is invalid
                 log.info("Team Leader " + sTeamLeader + " does not have a valid email address " +
                     sTempEmailAddress + "!")
             }
         }
     } catch (EnterpriseServiceOperationException e){
         listErrors(e)
     }
     return sEmailAddress
 }

/**
 * Request TRBWOE to send email with a 5 minute delay to allow the WO Extended Text
 * to be created and included in the email message
 * @param sDistrictCode District Code
 * @param sWorkOrder Work Order Number
 * @return None
 */
 private void requestEmailBatch(String sDistrictCode, String sWorkOrder) {
     log.info("requestEmailBatch")
     
     /* Gets a calendar using the default time zone and locale. The
      * Calendar returned is based on the current time in the default
      * time zone with the default locale.
      */
     Calendar calendar = Calendar.getInstance()
     
     // Add 5 minutes and use it as the defer date and time for the report request
     calendar.add(Calendar.MINUTE, 5)
     
     try {
         BatchRequestServiceCreateReplyDTO batchRequestReply = tools.service.get('BatchRequest').create({
             it.setProgramName("TRBWOE")
             it.setBatchSubmission(true)
             it.setDeferDate(calendar)
             it.setDeferTime(calendar)
             it.setRequestedBy("COUTO WO Create")
             it.setDistrictCode(sDistrictCode)
             it.setMedium("R")
             it.setRequestParameter1(sDistrictCode)
             it.setRequestParameter2(sWorkOrder)})
         
         log.info("TRBWOE requested successfully.")
     } catch (EnterpriseServiceOperationException e){
         listErrors(e)
         log.info("Failed to request TRBWOE.")
     }
 }
 
 /**
  * Check the supplied Equipment's profile then look for its installation equipment
  * @param sEquipRef
  * @param sEquipNo
  * @param sEGI
  * @return sInstallEquip
  */
 private String determineEquipToUse(String sEquipRef, String sEquipNo, String sEGI) {
     log.info("determineEquipToUse ${sEquipRef} ${sEquipNo} ${sEGI}")
     
     String sInstallEquip = ""
     Boolean bProfileFound = false
     Boolean bInstallWO = false
     
     // Locate the Equipment Profile
     // Try with the EGI first
     if (!sEGI.equals("") && !sEGI.equals(null)) {
         Constraint c1 = MSF610Key.equipGrpId.equalTo(sEGI)
         Constraint c2 = MSF610Key.egiRecType.equalTo("G")
         Query queryMsf610 = new QueryImpl(MSF610Rec.class).and(c1).and(c2)
         MSF610Rec msf610Rec = tools.edoi.firstRow(queryMsf610)
         
         if (!msf610Rec.equals("") && !msf610Rec.equals(null)) {
             bProfileFound = true
             if (msf610Rec.getInstallWoFlag().equals("Y")) {
                 bInstallWO = true
             }
         } else {
             log.info("Profile for EGI ${sEGI} not found")
         }
     }
     
     // Try with the Equipment if not sucessful with EGI
     if (!bProfileFound) {
         Constraint c1 = MSF610Key.equipGrpId.equalTo(sEquipNo)
         Constraint c2 = MSF610Key.egiRecType.equalTo("E")
         Query queryMsf610 = new QueryImpl(MSF610Rec.class).and(c1).and(c2)
         MSF610Rec msf610Rec = tools.edoi.firstRow(queryMsf610)
         
         if (!msf610Rec.equals("") && !msf610Rec.equals(null)) {
             if (msf610Rec.getInstallWoFlag().equals("Y")) {
                 bInstallWO = true
             }
         } else {
             log.info("Profile for Equipment ${sEquipNo} not found")
         }
     }
     
     // Profile says to create WO for the installation equipment (immediate parent).
     // Locate the installation Equipment.
     if (bInstallWO) {
         Constraint c1 = MSFX69Key.equipNo.equalTo(sEquipNo)
         Query queryMsfx69 = new QueryImpl(MSFX69Rec.class).and(c1)
         
         MSFX69Rec msfx69Rec = tools.edoi.firstRow(queryMsfx69)
         if (!msfx69Rec.equals("") && !msfx69Rec.equals(null)) {
             sInstallEquip = msfx69Rec.getPrimaryKey().getInstallPosn().substring(0,12)
         } else {
             log.info("Installation Equipment for ${sEquipRef} not found")
         }
     }
     return sInstallEquip
 }
 
 /**
  * Validate the Project Number and return the Account Code
  * @param sProjNo Project Number
  * @return Project Found TRUE/FALSE, Project Account Code
  */
 private def checkProject (String sProjNo) {
     log.info("checkProject ${sProjNo}")
     
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
     log.info("checkTable ${sTableType} ${sTableCode}")
     
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
	 //lwh testing
	 how_to_use_edoi_search();
	 //lwh testing end
     }


     return [bTableFound, sTableDesc]
 }
 
 private void how_to_use_edoi_search(){
	 
			 log.info ("------------------------------")
			 log.info ("how_to_use_edoi_search - Start")
			 log.info ("------------------------------")
	 
			 Constraint c1 = MSF010Key.tableType.equalTo("ER")
			 Constraint c2 = MSF010Key.tableCode.equalTo("1062")
			 QueryImpl query = new QueryImpl(MSF010Rec.class).and(c1).and(c2).orderBy(MSF010Rec.msf010Key)
			 Integer i = 0
	 
			 tools.edoi.search(query,{MSF010Rec msf010rec ->
				 i++
				 log.debug("----------DEBUG---------------")
				 log.info ("TableCode:" + msf010rec.getPrimaryKey().getTableCode())
				 log.info ("TableDesc:" + msf010rec.getTableDesc())
				 log.debug("----------DEBUG---------------")
	 
	 
			 })
	 
			 if (i == 0){
				 log.info ("No records was found")
			 }
	 
			 log.info  ("-------------------------------")
			 log.info  ("how_to_use_edoi_search - Finish")
			 log.info  ("-------------------------------")
			 log.info  ("\n")
	 
		 }

 /**
  * Validate Equipment Number and return the first 3 Equipment Classifications
  * @param sEquipRef Equipment Reference
  * @return Equipment Found TRUE/FALSE, Equipment Classifications 0, 1, 2
  */
 private def checkEquipment(String sEquipRef) {
     log.info("checkEquipment ${sEquipRef}")
     
     Boolean bEquipFound = false
     String sEC0 = ""
     String sEC1 = ""
     String sEC2 = ""
     String sEGI = ""
     String sEquipNo = ""
     
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
         sEGI = equipReply.getEquipmentGrpId()
         sEquipNo = equipReply.getEquipmentNo()
     } catch (EnterpriseServiceOperationException e){
         listErrors(e)
     }
     return [bEquipFound, sEC0, sEC1, sEC2, sEGI, sEquipNo]
 }
 
 /**
  * Check that the Equipment Reference and the Work Group have been supplied
  * @param sEquipRef Equipment Reference
  * @param sWorkGroup Work Group
  * @return Mandatory Inputs Supplied TRUE/FALSE
  */
 private boolean checkMandatoryInputs (String sEquipRef, String sWorkGroup) {
     log.info("checkMandatoryInputs ${sEquipRef} ${sWorkGroup}")
     
     Boolean bInputSupplied = true
     
     // Check Equipment is supplied
     if (sEquipRef.equals("") || sEquipRef.equals(null)) {
         log.info("Equipment was not supplied. Costing Solution does not apply.")
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
  * Write errors encountered to the log
  */
 private void listErrors (EnterpriseServiceOperationException e) {
     log.info("listErrors")
     
     List <ErrorMessageDTO> listError = e.getErrorMessages()
     listError.each{ErrorMessageDTO errorDTO ->
         log.info ("Error Code: " + errorDTO.getCode())
         log.info ("Error Message: " + errorDTO.getMessage())
         log.info ("Error Fields: " + errorDTO.getFieldName())
     }
 }
}

