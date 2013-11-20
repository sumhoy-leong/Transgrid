/**
 * @author Ventyx 2012
 *
 * TRBWOE - Sends email to a Work Group Team Leader when a call-out Work Order
 *          assigned to his/her Work Group is created or closed.
 *          This program is initiated via the hook "WorkOrderService_create" and
 *          "WorkOrderService_complete". It is not meant to be requested manually.
 */

package com.mincom.ellipse.script.custom

import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequestDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workgroup.WorkGroupServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*

import java.text.SimpleDateFormat
import java.util.ArrayList;
import java.util.List;

import javax.mail.*
import javax.mail.internet.*
import javax.activation.*

import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class ParamsTrbwoe {
    // List of Input Parameters
    String paramDistrictCode
    String paramWorkOrder
    String paramEvent
    
    //Restart Variables
}

public class ProcessTrbwoe extends SuperBatch {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private String version = "5"
    private ParamsTrbwoe batchParams
    private String sEvent = ""
    
    public void runBatch(Binding b) {
        init(b)
        
        printSuperBatchVersion()
        info("runBatch version : ${version}")
        
        // Request Parameters
        batchParams = params.fill(new ParamsTrbwoe())
        info("paramDistrictCode: " + batchParams.paramDistrictCode)
        info("paramWorkOrder: " + batchParams.paramWorkOrder)
        info("paramEvent: " + batchParams.paramEvent)
        
        // Setup request parameters. If parameters are valid then continue
        if (processParams()) {
            processBatch()
        } else {
            info ("##### ERROR: Mandatory inputs are missing!!! #####")
        }
    }
    
    // Validate request parameters
    private Boolean processParams() {
        info("processParams")
        
        Boolean bParamsOk = true
        
        // District Code is mandatory
        if (batchParams.paramDistrictCode.equals("") || batchParams.paramDistrictCode.equals(null)){
            bParamsOk = false
            info("District Code is Mandatory!!!")
        }
        
        // Work Order is mandatory
        if (batchParams.paramWorkOrder.equals("") || batchParams.paramWorkOrder.equals(null)){
            bParamsOk = false
            info("Work Order is Mandatory!!!")
        } 
        // Determine if a creation or completion event has occured
        if (batchParams.paramEvent == "C") {
            sEvent = "COMPLETE"
        } else {
            sEvent = "CREATE"
        }
        return bParamsOk
    }
    
    private void processBatch() {
        info("processBatch")
        
        // Get some key WO information
        try {
            WorkOrderServiceReadReplyDTO woReply = service.get('WorkOrder').read ({
                it.districtCode = batchParams.paramDistrictCode
                it.workOrder = new WorkOrderDTO(batchParams.paramWorkOrder)})
            
            String sDistrictCode = woReply.getDistrictCode()
            String sWorkOrder = woReply.getWorkOrder().toString()
            String sWorkGroup = woReply.getWorkGroup()
            
            // Find the Work Group Team Leader
            String sTeamLeader = getTeamLeader(sDistrictCode, sWorkGroup)
            if (sTeamLeader != "") {
                // Find the Team Leader's Email Address
                String sEmailAddress = getEmailAddress(sTeamLeader)
                
                // Team leader has a valid email address
                if (sEmailAddress != "") {
                    // Build and send the Email message
                    composeEmail(sDistrictCode, sWorkOrder, sEmailAddress)
                }
            }
        } catch (EnterpriseServiceOperationException e){
            listErrors(e)
        }
    }
    
    /**
     * Get the Work Group Team Leader
     * @param sDistrictCode District Code
     * @param sWorkGroup Work Group
     * @return Work Group Team Leader's Employee Id
     */
    private String getTeamLeader(String sDistrictCode, String sWorkGroup) {
        info("getTeamLeader")
        
        String sTeamLeader = ""
        
        try {
            WorkGroupServiceReadReplyDTO workGroupReply = service.get('WorkGroup').read({
                it.districtCode = sDistrictCode
                it.workGroup = sWorkGroup})
            
            // Work Group does not have a team leader
            if (workGroupReply.getSupervisor().equals("") || workGroupReply.getSupervisor().equals(null)){
                info("Team Leader for " + sWorkGroup + " not found!")
            }
            // Work Group has a Team Leader
            else {
                sTeamLeader = workGroupReply.getSupervisor().trim()
            }
        } catch (EnterpriseServiceOperationException e){
            listErrors(e)
        }
        return sTeamLeader
    }
    
    /**
     * Get Team Leader's Email Address
     * @param sTeamLeader Work Group Team Leader
     * @return Team Leader's Email address
     */
    private String getEmailAddress(String sTeamLeader) {
        info("getEmailAddress")
        
        String sEmailAddress = ""
        String sTempEmailAddress = ""
        Boolean validEmail = false
        
        try {
            // Get Team Leader's Employee record
            EmployeeServiceReadReplyDTO empReply = service.get('Employee').read({it.employee = sTeamLeader})
            
            // Team Leader does not have an Email Address
            if (empReply.getEmailAddress().equals("")) {
                info("Team Leader " + sTeamLeader + " does not have an email address!")
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
                    info("Team Leader " + sTeamLeader + " does not have a valid email address " +
                        sTempEmailAddress + "!")
                }
            }
        } catch (EnterpriseServiceOperationException e){
            listErrors(e)
        }
        return sEmailAddress
    }
    
    /**
     * Build email message and send to Team Leader
     * @param sDistrictCode District Code
     * @param sWorkOrder Work Order Number
     * @param sEmailAddress Team Leader's Email Address
     * @return None
     */
    private void composeEmail(String sDistrictCode, String sWorkOrder, String sEmailAddress) {
        info("composeEmail ${sDistrictCode}, ${sWorkOrder}, ${sEmailAddress}")
        
        // Get message details common to both WO create and WO complete
        
        // Get WO details
        WorkOrderServiceReadReplyDTO woReply = service.get('WorkOrder').read ({
            it.districtCode = sDistrictCode
            it.workOrder = new WorkOrderDTO(sWorkOrder)})
        
        // Get WorkOrder details
        String sEquipRef = woReply.getEquipmentRef().trim()
        String sWODesc = woReply.getWorkOrderDesc().trim()
        String sRaisedDate = new SimpleDateFormat("dd/MM/yyyy").format((woReply.getRaisedDate()).
            getTime()).toString()
        
        // Originator Details
        String sOriginatorId = woReply.getOriginatorId().trim()
        EmployeeServiceReadReplyDTO empReply = service.get('Employee').read({it.employee = sOriginatorId})
        String sOriginator = empReply.getFirstName().trim() + " " + empReply.getLastName().trim()
        
        // Get WO Extended Description
        String sExtWODescKey = woReply.getExtendedText().trim()
        String sWOExtDesc = ""
        if (!(sExtWODescKey.equals(""))) {
            sWOExtDesc = getStdText(sExtWODescKey)
        }
        
        // Get Equipment details
        String sEquipDesc1 = ""
        String sEquipDesc2 = ""
        String sEquipLocDesc = ""
        if (!(sEquipRef.equals(""))) {
            EquipmentServiceReadReplyDTO equipReply = service.get('Equipment').read({
                it.equipmentRef = sEquipRef})
            sEquipDesc1 = equipReply.getEquipmentNoDescription1().toString()
            sEquipDesc2 = equipReply.getEquipmentNoDescription2().toString()
            sEquipLocDesc = equipReply.getEquipmentLocationDescription().toString()
        }
        
        // Construct the email message
        String sSubject = ""
        String sMessage = ""
        
        if (sEvent == "CREATE") {
            // Event is WO creation
            // Email Subject and Message
            sSubject = "Callout WO Raised - Equip Ref " + sEquipRef + ", Raised by " + sOriginator
            sMessage = "MESSAGE:" + "\r\n" +
                "Work Order No. - " + sWorkOrder + "\r\n" +
                "Equipment Reference - " + sEquipRef + "\r\n" +
                "Equipment Description - " + sEquipDesc1 + " " + sEquipDesc2 + "\r\n" +
                "Equipment Location - " + sEquipLocDesc + "\r\n" +
                "Work Order Description - " + sWODesc + "\r\n" +
                "Raised Date - " + sRaisedDate + "\r\n" +
                "Originator - " + sOriginatorId + " " + sOriginator + "\r\n" +
                "Work Order Extended Description - " + sWOExtDesc + "\r\n"
        } else {
            // Event is WO completion/closure
            // Get extra closure information and change the message
            
            // CompletedBy Details
            String sCompById = woReply.getCompletedBy().trim()
            EmployeeServiceReadReplyDTO compByEmpReply = service.get('Employee').read({it.employee = sCompById})
            String sCompBy = compByEmpReply.getFirstName().trim() + " " + compByEmpReply.getLastName().trim()
            
            // Completion Code and Description
            String sCompDate = ""
            String sCompCodeDesc = ""
            String sCompCode = woReply.getCompletedCode().trim()
            if (!(sCompCode.equals(""))) {
                sCompDate = new SimpleDateFormat("dd/MM/yyyy").format((woReply.getClosedDate()).getTime()).toString()
                TableServiceReadReplyDTO tabReply = service.get('Table').read ({
                    it.tableType = "SC"
                    it.tableCode = sCompCode})
                
                sCompCodeDesc = tabReply.getCodeDescription().trim()
            }
            
            // Get WO Completion Comments
            String sCompCommKey = woReply.getCompletionText().trim()
            String sCompComm = ""
            if (!(sCompCommKey.equals(""))) {
                sCompComm = getStdText(sCompCommKey)
            }
            
            // Email Subject and Message
            sSubject = "Callout WO Completed - Equip Ref " + sEquipRef + ", Raised by " + sOriginator +
                ", Completed by " + sCompBy
            sMessage = "MESSAGE:" + "\r\n" +
                "Work Order No. - " + sWorkOrder + "\r\n" +
                "Equipment Reference - " + sEquipRef + "\r\n" +
                "Equipment Description - " + sEquipDesc1 + " " + sEquipDesc2 + "\r\n" +
                "Equipment Location - " + sEquipLocDesc + "\r\n" +
                "Work Order Description - " + sWODesc + "\r\n" +
                "Raised Date - " + sRaisedDate + "\r\n" +
                "Originator - " + sOriginatorId + " " + sOriginator + "\r\n" +
                "Work Order Extended Description - " + sWOExtDesc + "\r\n" +
                "Closed Date - " + sCompDate + "\r\n" +
                "Completion Code - " + sCompCode + " " + sCompCodeDesc + "\r\n" +
                "Completed By - " + sCompById + " " + sCompBy + "\r\n" +
                "Work Order Completion Description - " + sCompComm
        }
        // Send the email
        sendEmail(sSubject, sEmailAddress, sMessage)
    }
    
    /**
     * Get Standard Text for the supplied key
     * @param sTextKey Standard Text Key
     * @return Standard Text for the key provided
     */
    private String getStdText(String sTextKey) {
        info("getStdText")
        
        List<String> textLines = []
        String sTempString = ""
        int sub = 0
        int SUB_MAX = 599
        
        List<StdTextServiceGetTextReplyDTO> textReply = new ArrayList<StdTextServiceGetTextReplyDTO>()
        try {
            //restart value
            def restart = ""
            StdTextServiceGetTextReplyCollectionDTO textReplyCollection =
                    service.get("StdText").getText({StdTextServiceGetTextRequestDTO it->
                        it.setStdTextId(sTextKey)
                    },100, true, restart)
            restart = textReplyCollection.getCollectionRestartPoint()
            
            for (StdTextServiceGetTextReplyDTO stdTextDTO : textReplyCollection.getReplyElements()) {
                for (String text : stdTextDTO.getTextLine()) {
                    textLines.add(text)
                    sub++
                    if (sub >= SUB_MAX) {
                        break
                    }
                }
                if (sub >= SUB_MAX) {
                    break
                }
            }
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while (restart != null && restart.trim().length() > 0 && sub < SUB_MAX) {
                textReplyCollection = service.get("StdText").getText({StdTextServiceGetTextRequestDTO it->
                    it.setStdTextId(sTextKey)
                },100, true, restart)
                restart = textReplyCollection.getCollectionRestartPoint()
                
                for (StdTextServiceGetTextReplyDTO stdTextDTO : textReplyCollection.getReplyElements()) {
                    for (String text : stdTextDTO.getTextLine()) {
                        textLines.add(text)
                        sub++
                        if (sub >= SUB_MAX) {
                            break
                        }
                    }
                    if (sub >= SUB_MAX) {
                        break
                    }
                }
            }
            
        } catch (com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException serviceExc) {
            info("Error during execute StdText.getText: ${serviceExc.getMessage()}")
        } catch (Exception e) {
            info("Unknown error during execute StdText.getText: ${e.getClass().toString()} ${e.getMessage()}")
        }
        
        // Get the contents of the list textLines and concatenate them into sTempString
        if (sub > 0) {
            textLines.each {
                sTempString = sTempString + it + "\n"
            }
        }
        return sTempString
    }
    
    /**
     * Send the email to the Work Group Team Leader
     * @param sSubject Email Subject
     * @param sEmailAddress Email Recipient
     * @param sMessage Email message
     * @return None
     */
    private void sendEmail(String sSubject, String sEmailAddress, String sMessage) {
        info("sendEmail")
        
        // Get default email information from system properties
        Properties properties = System.getProperties()
        // Get the default Session object
        Session session = Session.getDefaultInstance(properties,null)
        
        // Construct the message
        MimeMessage msg = new MimeMessage(session)
        msg.setSubject(sSubject)
        msg.setRecipients(Message.RecipientType.TO,sEmailAddress)
        msg.setText(sMessage)
        
        // Send the message
        Transport.send(msg)
    }
    /**
     * Write errors to log
     * @param e Error Object
     * @return None
     */
    private void listErrors(EnterpriseServiceOperationException e) {
        List <ErrorMessageDTO> listError = e.getErrorMessages()
        listError.each{ErrorMessageDTO errorDTO ->
            info ("Error Code: " + errorDTO.getCode())
            info ("Error Message: " + errorDTO.getMessage())
            info ("Error Fields: " + errorDTO.getFieldName())
        }
    }
}

/*run script*/
ProcessTrbwoe process = new ProcessTrbwoe()
process.runBatch(binding)
