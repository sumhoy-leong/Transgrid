/**
 * @author Ventyx 2013
 *
 * Post-Complete:
 * When a call out Work Order Task is completed, send an email notification to the
 * Work Group Team Leader by requesting TRBWOE if the Work Order is Complete.
 *
 */

import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.batchrequest.BatchRequestServiceCreateReplyDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workordertask.WorkOrderTaskServiceCompleteReplyDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.*
import java.text.SimpleDateFormat
import javax.mail.*
import javax.mail.internet.*
import javax.activation.*

public class WorkOrderTaskService_complete extends ServiceHook {
  
 /*
  * IMPORTANT!
  * Update this Version number EVERY push to GIT
  */
 private String version = "1"
 
 @Override
 public Object onPreExecute(Object dto) {
     // Nothing to do here
     return null
 }

 @Override
 public Object onPostExecute(Object input, Object result) {
     log.info("WorkOrderTaskService_complete onPostExecute - version: ${version}")
     
     WorkOrderTaskServiceCompleteReplyDTO reply = result
     
     try {
         // Get WO details 
         WorkOrderServiceReadReplyDTO woReply = tools.service.get('WorkOrder').read ({
             it.districtCode = reply.getDistrictCode()
             it.workOrder = reply.getWorkOrder()})
         
         String sDistrictCode = woReply.getDistrictCode()
         String sWorkOrder = woReply.getWorkOrder().toString()
         String sWorkGroup = woReply.getWorkGroup()
         String sJobCode5 = woReply.getJobCode5()
         String sWOStatus = woReply.getClosedStatus()
         
         // Process the sending of an email only if the Work Order is closed.
         if (sWOStatus.equals("C")) {
			 log.info("Process for COUTO")
             // Send an email only if the Work Group is used and the W4 Job Code (JobCode5) is 'COUTO'
             if (!(sWorkGroup.equals("")) && (sJobCode5.equals("COUTO"))) {
                 // Find the Work Group's Team Leader
                 String sTeamLeader = getTeamLeader(sDistrictCode, sWorkGroup)
                 
                 // Work Group has a Team Leader
                 if (sTeamLeader != "") {
                     // Find the Team Leader's Email Address
                     String sEmailAddress = getEmailAddress(sTeamLeader)
                     
                     // Team Leader's has an Email Address, request TRBWOE
                     if (sEmailAddress != "") {
                         requestEmailBatch(sDistrictCode, sWorkOrder)
                     }
                 }
             }
        }
     } catch (EnterpriseServiceOperationException e){
         listErrors (e)
     }
     return result
 }
 
 /**
  * Get the Work Group Team Leader
  * @param sDistrictCode - District Code
  * @param sWorkGroup - Work Group the Work Order is assigned to
  * @return Employee Id of Work Group Team Leader
  */
 private String getTeamLeader(String sDistrictCode, String sWorkGroup) {
     log.info("getTeamLeader")
     
     String sTeamLeader = ""
     
     try {
         WorkGroupServiceReadReplyDTO workGroupReply = tools.service.get('WorkGroup').read({
             it.districtCode = sDistrictCode
             it.workGroup = sWorkGroup})
         
         // Work Group record not found
         if (workGroupReply.equals("") || workGroupReply.equals(null)) {
             log.info("Work Group " + sWorkGroup + " record not found!")
         }
         // Work Group does not have a team leader
         else if (workGroupReply.getSupervisor().equals("")){
             log.info("Team Leader for " + sWorkGroup + " not found!")
         }
         // Work Group has a Team Leader
         else {
             sTeamLeader = workGroupReply.getSupervisor().trim()
         }
     } catch (EnterpriseServiceOperationException e){
         listErrors (e)
     }
     return sTeamLeader
 }

 /**
  * Get Team Leader's Email Address
  * @param sTeamLeader - Work Group's Team Leader
  * @return Email Address of Work Group Team Leader
  */
 private String getEmailAddress(String sTeamLeader) {
     log.info("getEmailAddress")
     
     String sEmailAddress = ""
     String sTempEmailAddress = ""
     Boolean validEmail = false
     
     try {
         // Get Team Leader's Employee record
         EmployeeServiceReadReplyDTO empReply = tools.service.get('Employee').read({it.employee = sTeamLeader})
         // Employee record not found
         if (empReply.equals("") || empReply.equals(null)) {
             log.info("Team Leader's " + sTeamLeader + " Employee record not found!")
         }
         // Team Leader does not have an Email Address
         else if (empReply.getEmailAddress().equals("")) {
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
         listErrors (e)
     }
     return sEmailAddress
 }
 
 /**
  * Request TRBWOE to send email with a 6 minute delay
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
     
     // Add 6 minutes and use it as the defer date and time for the report request
     calendar.add(Calendar.MINUTE, 6)
     
     try {
         BatchRequestServiceCreateReplyDTO batchRequestReply = tools.service.get('BatchRequest').create({
             it.setProgramName("TRBWOE")
             it.setBatchSubmission(true)
             it.setDeferDate(calendar)
             it.setDeferTime(calendar)
             it.setRequestedBy("COUTO WO Complete")
             it.setDistrictCode(sDistrictCode)
             it.setMedium("R")
             it.setRequestParameter1(sDistrictCode)
             it.setRequestParameter2(sWorkOrder)
             it.setRequestParameter3("C")})
         
         log.info("TRBWOE requested successfully.")
     } catch (EnterpriseServiceOperationException e){
         listErrors(e)
         log.info("Failed to request TRBWOE.")
     }
 }
 
 private listErrors (EnterpriseServiceOperationException e) {
     List <ErrorMessageDTO> listError = e.getErrorMessages()
     listError.each{ErrorMessageDTO errorDTO ->
         log.info ("Error Code: " + errorDTO.getCode())
         log.info ("Error Message: " + errorDTO.getMessage())
         log.info ("Error Fields: " + errorDTO.getFieldName())
     }
 }
}
