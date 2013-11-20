/**
 * @author Ventyx 2012
 *
 */

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar

import com.mincom.ellipse.attribute.Attribute
import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheettrans.TimesheetTransServiceModifyStatusRequestDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*

class TimesheetTransService_multipleModifyStatus extends ServiceHook {
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 1

    private static final String MSF010_TABLE_TYPE_ALW = "#ALW"
    private static final String MSF010_TABLE_TYPE_ER  = "ER"
    private static final String TRANS_DISTRICT = "GRID"
    private static final String ALLOWANCEWO_CUSTOM_ATT = "ALLOWANCEWO"
    private static final String ERR_CODE_ALL_WO_NOT_OPEN = "+110"
    private static final String ERR_CODE_ALL_WO_NOT_PAID = "+111"
    private static final String ERR_CODE_ALL_WO_REQUIRED = "+112"
    private static final String ERR_CODE_ALL_WO_NOT_REQD = "+113"
    private static final String APPR_APPROVAL = "APPR"
    private Calendar closedCommitDate
    private boolean finalCosts = false
    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd")

    @Override
    public Object onPreExecute(Object dto) {
        log.info("TimesheetTransService_multipleModifyStatus onPreExecute - version: ${version}")

        String allowanceWO = ""
        String allowanceCode = ""
        boolean bTableFound = false
        String dtmCCYYMMDD = ""
        Calendar cal = Calendar.getInstance()
        Calendar prevTranDate = Calendar.getInstance()
        dtmCCYYMMDD = DATE_FORMAT.format(cal.getTime())
        com.mincom.ellipse.eboi.types.Date defaultDate = new com.mincom.ellipse.eboi.types.Date("")

        dto.each {TimesheetTransServiceModifyStatusRequestDTO request ->
            log.info("${request.getEmployee()} - ${request.getTranApprovalStatus()}")
            String status = request.getTranApprovalStatus().trim()
            switch(status) {
                case APPR_APPROVAL:
                    ArrayList<TimesheetAllowsServiceRetrieveReplyDTO> allowances =
                    retrieveTimesheetAllows(request.getEmployee(),request.getPayGroup(),
                    request.getSequenceNo(),request.getTranDate())

                    allowances.each {
                        allowanceWO = ""
                        //get the AllowanceWO value
                        log.info("Custom Attributes values:")
                        List<Attribute> custAttribs = it.getCustomAttributes()
                        custAttribs.each{Attribute customAttribute ->
                            log.info("${customAttribute.getName()} : ${customAttribute.getValue()}")
                            if(customAttribute.getName().equals(ALLOWANCEWO_CUSTOM_ATT))
                                allowanceWO = customAttribute.getValue()
                        }
                        allowanceCode = it.getAllowanceCode()
                        bTableFound = checkTable(allowanceCode)

                        //check Allowance WO and AllowanceWO
                        if(bTableFound && !allowanceWO?.trim()){
                            throw new EnterpriseServiceOperationException(
                            new ErrorMessageDTO(
                            ERR_CODE_ALL_WO_REQUIRED,
                            getErrorDescription(ERR_CODE_ALL_WO_REQUIRED),
                            "AllowanceWO", 0, 0))
                            return dto
                        }
                        if(!bTableFound && allowanceWO?.trim()){
                            throw new EnterpriseServiceOperationException(
                            new ErrorMessageDTO(
                            ERR_CODE_ALL_WO_NOT_REQD,
                            getErrorDescription(ERR_CODE_ALL_WO_NOT_REQD),
                            "AllowanceWO", 0, 0))
                            return dto
                        }
                        else{
                            if(allowanceWO?.trim()){
                                WorkOrderServiceReadReplyDTO workOrderReply = readWorkOrder(allowanceWO)
                                if(workOrderReply) {
                                    closedCommitDate = workOrderReply.getCloseCommitDate()
                                    finalCosts = workOrderReply.getFinalCosts()
                                    if(finalCosts || (!closedCommitDate.getTime().equals(defaultDate.getValue().getTime()) &&
                                    Integer.parseInt(DATE_FORMAT.format(closedCommitDate.getTime())) < Integer.parseInt(dtmCCYYMMDD))){
                                        throw new EnterpriseServiceOperationException(
                                        new ErrorMessageDTO(
                                        ERR_CODE_ALL_WO_NOT_OPEN,
                                        getErrorDescription(ERR_CODE_ALL_WO_NOT_OPEN),
                                        "AllowanceWO", 0, 0))
                                        return dto
                                    }
                                } else {
                                    throw new EnterpriseServiceOperationException(
                                    new ErrorMessageDTO(
                                    "0039",
                                    getErrorDescription("0039"),
                                    "AllowanceWO", 0, 0))
                                }
                            }
                        }
                    }
                    break
                default:
                    break
            }
        }

        return null
    }

    /**
     * Retrieve original timesheet allowance
     * @param employeeId as String
     * @param payGroup as String
     * @param sequenceNo as String
     * @param tranDate as String
     */
    private ArrayList retrieveTimesheetAllows(String employeeId, String payGroup, String sequenceNo, Calendar tranDate){
        log.info("retrieveTimesheetAllows")

        ArrayList<TimesheetAllowsServiceRetrieveReplyDTO> replies = new ArrayList<TimesheetAllowsServiceRetrieveReplyDTO>()
        try{
            def restart = ""
            TimesheetAllowsServiceRetrieveReplyCollectionDTO timesheetReply = tools.service.get("TimesheetAllows").retrieve({
                it.employee = employeeId
                it.payGroup = payGroup
                it.sequenceNo = sequenceNo
                it.tranDate = tranDate},100,false,restart)

            restart = timesheetReply.getCollectionRestartPoint()
            replies.addAll(timesheetReply.replyElements)

            while(restart?.trim()){
                timesheetReply = tools.service.get("TimesheetAllows").retrieve({
                    it.employee = employeeId
                    it.payGroup = payGroup
                    it.sequenceNo = sequenceNo
                    it.tranDate = tranDate},100,false,restart)

                restart = timesheetReply.getCollectionRestartPoint()
                replies.addAll(timesheetReply.replyElements)
            }
        }catch(EnterpriseServiceOperationException e){
            listErrors(e)
        }
        return replies
    }

    /**
     * Check allowance code in #ALW table type
     * @param sAllowanceCode as String
     * @return boolean
     */
    private boolean checkTable(String sAllowanceCode){
        log.info("checkTable")
        boolean tableFound = false

        try{
            TableServiceReadReplyDTO tableReply =
                    tools.service.get('Table').read({
                        it.tableType = MSF010_TABLE_TYPE_ALW
                        it.tableCode = sAllowanceCode
                    })
            tableFound = true
        }catch (EnterpriseServiceOperationException e){
            tableFound = false
            listErrors(e)
        }

        return tableFound
    }

    /**
     * Get error description from Table -ER based on error code.
     * @param errorCode Error Code
     * @return error description
     */
    private String getErrorDescription(String errorCode){
        log.info("getErrorDescription")
        try{
            TableServiceReadReplyDTO tableReply =
                    tools.service.get('Table').read({
                        it.tableType = MSF010_TABLE_TYPE_ER
                        it.tableCode = errorCode
                    })
            if(tableReply) {
                return tableReply?.getDescription().trim()
            }
        }catch (EnterpriseServiceOperationException e){
            listErrors(e)
            return ""
        }
    }

    /**
     * Check work order closed date and final cost status
     * @param sAllowanceWO as String
     */
    private WorkOrderServiceReadReplyDTO readWorkOrder(String sAllowanceWO){
        log.info("readWorkOrder")

        WorkOrderDTO woNo = new  WorkOrderDTO(sAllowanceWO)
        try{
            WorkOrderServiceReadReplyDTO workOrderReply =
                    tools.service.get('WorkOrder').read({
                        it.districtCode = TRANS_DISTRICT
                        it.workOrder = woNo
                    })
            return workOrderReply
        }catch (EnterpriseServiceOperationException e){
            listErrors(e)
            return null
        }
    }

    private void listErrors (EnterpriseServiceOperationException e) {
        List <ErrorMessageDTO> listError = e.getErrorMessages()
        listError.each{ErrorMessageDTO errorDTO ->
            log.info ("Error Code    : " + errorDTO.getCode())
            log.info ("Error Message : " + errorDTO.getMessage())
            log.info ("Error Fields  : " + errorDTO.getFieldName())
        }
    }
}
