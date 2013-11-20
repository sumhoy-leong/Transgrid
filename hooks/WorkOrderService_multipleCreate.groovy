/**
 * @author Ventyx 2012
 *
 * Pre-Create:
 *     Apply the costing solution for qualifying Work Orders to obtain a default
 *     Project Number and Account Code.
 */

import com.mincom.ellipse.hook.hooks.ServiceHook

import com.mincom.ellipse.edoi.ejb.msf610.*
import com.mincom.ellipse.edoi.ejb.msfx69.*
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.project.ProjectServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.standardjob.StandardJobServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import java.text.SimpleDateFormat
import java.util.Calendar

public class WorkOrderService_multipleCreate extends ServiceHook {
  
/*
 * IMPORTANT!
 * Update this Version number EVERY push to GIT
 */
  
private String version = "5"

 @Override
 public Object onPreExecute(Object dto) {
     log.info("WorkOrderService_multipleCreate onPreExecute - version: ${version}")
     
     dto.each {WorkOrderServiceCreateRequestDTO request ->
     
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
                     log.info("Installation Equipment to use for this WO ${sInstallEquip}")
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
                         log.error("Installation Equipment " + sEquipRef + " not found.")
                         //throw new RuntimeException("Installation Equipment " + sEquipRef + " not found.")
                         throw new EnterpriseServiceOperationException(
                             new ErrorMessageDTO("", "Installation Equipment ${sEquipRef} not found.", "equipmentRef", 0, 0))
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
                         //throw new RuntimeException("+CST entry not found.")
                         throw new EnterpriseServiceOperationException(
                             new ErrorMessageDTO("", "+CST entry ${sCST} not found.", "", 0, 0))
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
     }
     return null
 }
 
 /**
  * Check the supplied Equipment's profile then look for its installation equipment
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
     }
     return [bTableFound, sTableDesc]
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


