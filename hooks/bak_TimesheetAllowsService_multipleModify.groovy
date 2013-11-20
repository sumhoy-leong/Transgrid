/**
 * @author Ventyx 2012
 *
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.mincom.ellipse.attribute.Attribute;
import com.mincom.ellipse.eboi.types.Date;
import com.mincom.ellipse.hook.hooks.ServiceHook;
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO;
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO;
import com.mincom.enterpriseservice.ellipse.table.*;
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceModifyRequestDTO;
import com.mincom.enterpriseservice.ellipse.timesheettrans.TimesheetTransServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.*;
import com.mincom.enterpriseservice.exception.*;

class TimesheetAllowsService_multipleModify extends ServiceHook {

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 4
    private static final String MSF010_TABLE_TYPE_ALW = "#ALW"
    private static final String MSF010_TABLE_TYPE_ER  = "ER"
    private static final String TRANS_DISTRICT = "GRID"
    private static final String ALLOWANCEWO_CUSTOM_ATT = "ALLOWANCEWO"
    private static final String ALLOWANCEWO_ENTITY_TYPE = "TimesheetAllowsService.TimesheetAllows."
    private static final String ERR_CODE_ALL_WO_NOT_OPEN = "+110"
    private static final String ERR_CODE_ALL_WO_NOT_PAID = "+111"
    private static final String ERR_CODE_ALL_WO_REQUIRED = "+112"
    private static final String ERR_CODE_ALL_WO_NOT_REQD = "+113"
    private static final String PAID_APPROVAL = "PAID"
    private Calendar closedCommitDate
    private boolean finalCosts = false
    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd")
    private SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmm")

    @Override
    public Object onPreExecute(Object dto) {
        log.info("TimesheetAllowsService_multipleModify onPreExecute - version: ${version}")
        String allowanceWO = ""
        String allowanceCode = ""
        String approvalStatus = ""
        boolean bTableFound = false
        String dtmCCYYMMDD = ""
        Calendar cal = Calendar.getInstance()
        Calendar prevTranDate = Calendar.getInstance()
        dtmCCYYMMDD = DATE_FORMAT.format(cal.getTime())
        Date defaultDate = new Date("")

        dto.each{TimesheetAllowsServiceModifyRequestDTO request ->
            allowanceWO = ""
            //get the AllowanceWO value
            log.info("Custom Attributes values:")
            List<Attribute> custAttribs = request.getCustomAttributes()
            custAttribs.each{Attribute customAttribute ->
                log.info("${customAttribute.getName()} : ${customAttribute.getValue()}")
                if(customAttribute.getName().equals(ALLOWANCEWO_CUSTOM_ATT))
                    allowanceWO = customAttribute.getValue()
            }
            allowanceCode = request.getAllowanceCode()

            if(!prevTranDate.equals(request.getTranDate())){
                try{
                    TimesheetTransServiceReadReplyDTO tableReply =
                            tools.service.get("TimesheetTrans").read({
                                it.employee = request.getEmployee()
                                it.payGroup = request.getPayGroup()
                                it.sequenceNo = request.getSequenceNo()
                                it.tranDate = request.getTranDate()
                            })
                    approvalStatus = tableReply.getTranApprovalStatus().trim()
                }catch(EnterpriseServiceOperationException e){
                    listErrors(e)
                }
                prevTranDate = request.getTranDate()
            }

            //check Allowance WO and AllowanceWO
            if(!approvalStatus.equals(PAID_APPROVAL)){
                throw new EnterpriseServiceOperationException(
                new ErrorMessageDTO(
                ERR_CODE_ALL_WO_NOT_PAID,
                getErrorDescription(ERR_CODE_ALL_WO_NOT_PAID),
                "AllowanceWO", 0, 0))
                return dto
            }
            else{
                //check Allowance WO and AllowanceWO
                bTableFound = checkTable(allowanceCode)
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
        }
        return null
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
            TableServiceReadReplyDTO tableReply = tools.service.get('Table').read({
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
        log.info("listErrors")
        List <ErrorMessageDTO> listError = e.getErrorMessages()
        listError.each{ErrorMessageDTO errorDTO ->
            log.info ("Error Code    : " + errorDTO.getCode())
            log.info ("Error Message : " + errorDTO.getMessage())
            log.info ("Error Fields  : " + errorDTO.getFieldName())
        }
    }
}
