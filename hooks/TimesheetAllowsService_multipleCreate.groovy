/**
 * @author Ventyx 2012
 *
 */

import java.text.SimpleDateFormat

import com.mincom.ellipse.attribute.Attribute
import com.mincom.ellipse.eboi.types.Date
import com.mincom.ellipse.edoi.ejb.msf0p5.*
import com.mincom.ellipse.edoi.ejb.msf820.*
import com.mincom.ellipse.edoi.ejb.msf891.*
import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceCreateReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceCreateReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheettrans.TimesheetTransServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.impl.QueryImpl

class TimesheetAllowsService_multipleCreate extends ServiceHook {

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 5
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
    private ArrayList oriTimesheets = new ArrayList()

    @Override
    public Object onPreExecute(Object dto) {
        log.info("TimesheetAllowsService_multipleCreate onPreExecute - version: ${version}")
        String allowanceWO = ""
        String allowanceCode = ""
        String approvalStatus = ""
        boolean bTableFound = false
        String dtmCCYYMMDD = ""
        Calendar cal = Calendar.getInstance()
        Calendar prevTranDate = Calendar.getInstance()
        dtmCCYYMMDD = DATE_FORMAT.format(cal.getTime())
        Date defaultDate = new Date("")

        dto.each { TimesheetAllowsServiceCreateRequestDTO request ->
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

                if(approvalStatus.equals(PAID_APPROVAL)) {
                    originalTimesheetAllows(request.getEmployee(),request.getPayGroup(),
                            request.getSequenceNo(),request.getTranDate())
                }

                prevTranDate = request.getTranDate()
            }

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
                        return dto
                    }
                }
            }
        }

        return null
    }

    @Override
    public Object onPostExecute(Object input, Object result) {
        log.info("TimesheetAllowsService_multipleCreate onPostExecute - version: ${version}")
        //store TimesheetAllowsServiceCreateReplyCollectionDTO
        storeTimesheetAllowsPayGroup(result)
        //store previous TimseheetAllows records
        prevTimesheetAllows()
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

    /**
     * Retrieve original timesheet allowance
     * @param employeeId as String
     * @param payGroup as String
     * @param sequenceNo as String
     * @param tranDate as String
     */
    private void originalTimesheetAllows(String employeeId, String payGroup, String sequenceNo, Calendar tranDate){
        log.info("originalTimesheetAllows")

        HashMap<TimesheetAllowsServiceRetrieveReplyDTO, String> oriTimesheet =
                new HashMap<TimesheetAllowsServiceRetrieveReplyDTO, String>()
        String AllowanceWo = ""
        try{
            def restart = ""
            List<TimesheetAllowsServiceRetrieveReplyDTO> replies =
                    new ArrayList<TimesheetAllowsServiceRetrieveReplyDTO>()
            TimesheetAllowsServiceRetrieveReplyCollectionDTO timesheetReply =
                    tools.service.get("TimesheetAllows").retrieve({
                        it.employee = employeeId
                        it.payGroup = payGroup
                        it.sequenceNo = sequenceNo
                        it.tranDate = tranDate
                    },100,false,restart)

            restart = timesheetReply.getCollectionRestartPoint()
            replies.addAll(timesheetReply.replyElements)

            while(restart?.trim()){
                timesheetReply = tools.service.get("TimesheetAllows").retrieve({
                    it.employee = employeeId
                    it.payGroup = payGroup
                    it.sequenceNo = sequenceNo
                    it.tranDate = tranDate
                },100,false,restart)

                restart = timesheetReply.getCollectionRestartPoint()
                replies.addAll(timesheetReply.replyElements)
            }

            replies.each {TimesheetAllowsServiceRetrieveReplyDTO reply ->
                AllowanceWo = ""
                List<Attribute> custAttribs = reply.getCustomAttributes()
                custAttribs.each{Attribute customAttribute ->
                    if(customAttribute.getName().equals(ALLOWANCEWO_CUSTOM_ATT))
                        AllowanceWo = customAttribute.getValue()
                }
                oriTimesheet.put(reply, AllowanceWo)
            }

            oriTimesheets.add(oriTimesheet)
        }catch(EnterpriseServiceOperationException e){
            listErrors(e)
        }
    }

    /**
     * Write allowance work order value
     */
    private void prevTimesheetAllows(){
        log.info("prevTimesheetAllows")
        Calendar prevTranDate = Calendar.getInstance()
        String maxSeqNo = ""

        Iterator iterateTimesheets = oriTimesheets.iterator()
        while(iterateTimesheets.hasNext()){
            HashMap<TimesheetAllowsServiceRetrieveReplyDTO, String> oriTimesheet = iterateTimesheets.next()
            Iterator iterate = oriTimesheet.iterator()
            while(iterate.hasNext()){
                Map.Entry timesheet = iterate.next()
                TimesheetAllowsServiceRetrieveReplyDTO oriTimesheetAllows = timesheet.getKey()
                if(!prevTranDate.equals(oriTimesheetAllows.getTranDate())){
                    //get maximum sequence number
                    QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                            .and(MSF891Key.employeeId.equalTo(oriTimesheetAllows.getEmployee()))
                            .and(MSF891Key.payGroup.equalTo(oriTimesheetAllows.getPayGroup()))
                            .and(MSF891Key.trnDate.equalTo(DATE_FORMAT.format(oriTimesheetAllows.getTranDate().getTime())))
                            .max(MSF891Key.seqNo)
                    maxSeqNo = tools.edoi.firstRow(qMSF891)
                    prevTranDate = oriTimesheetAllows.getTranDate()
                }

                String entityKey = oriTimesheetAllows.getEmployee()
                entityKey = entityKey + oriTimesheetAllows.getAllowanceCode()
                entityKey = entityKey + TIME_FORMAT.format(oriTimesheetAllows.getStartTime().getTime())
                entityKey = entityKey + oriTimesheetAllows.getPayGroup()
                entityKey = entityKey + maxSeqNo
                entityKey = entityKey + DATE_FORMAT.format(oriTimesheetAllows.getTranDate().getTime())
                saveCustomAttribute(entityKey, ALLOWANCEWO_ENTITY_TYPE+ALLOWANCEWO_CUSTOM_ATT, timesheet.getValue())
            }
        }
    }

    /**
     * Save custom attribute value.
     * @param key entity key
     * @param type entity type
     * @param value property value
     */
    private void saveCustomAttribute(String key, String type, String value) {
        log.info("saveCustomAttribute")
        def query = new QueryImpl(MSF0P5Rec.class)
                .and(MSF0P5Rec.entityKey.equalTo(key))
                .and(MSF0P5Rec.entityType.equalTo(type))

        MSF0P5Rec rec = tools.edoi.firstRow(query)
        if(rec) {
            if(!rec.getPropertyValue().trim().equals(value)) {
                try {
                    MSF0P5Key msf0p5Key = new MSF0P5Key(rec.getPrimaryKey().getInstanceId())
                    MSF0P5Rec msf0p5Rec = new MSF0P5Rec()
                    msf0p5Rec.setPrimaryKey(msf0p5Key)
                    msf0p5Rec.setEntityKey(key)
                    msf0p5Rec.setEntityType(type)
                    msf0p5Rec.setPropertyValue(value)
                    tools.edoi.update(msf0p5Rec)
                    log.info("Record update in MSF0P5 entity key: ${key} , entity type: ${type},property value: ${value}")
                }catch(Exception e) {
                    log.info("Cannot update MSF0P5 entity key: ${key} , entity type: ${type},property value: ${value}")
                }
            }
        } else {
            try {
                MSF0P5Key msf0p5Key = new MSF0P5Key()
                MSF0P5Rec msf0p5Rec = new MSF0P5Rec()
                msf0p5Rec.setPrimaryKey(msf0p5Key)
                msf0p5Rec.setEntityKey(key)
                msf0p5Rec.setEntityType(type)
                msf0p5Rec.setPropertyValue(value)
                tools.edoi.create(msf0p5Rec)
                log.info("Record created in MSF0P5 entity key: ${key} , entity type: ${type},property value: ${value}")
            }catch(Exception e) {
                log.info("Cannot create MSF0P5 entity key: ${key} , entity type: ${type},property value: ${value}")
            }
        }
    }

    /**
     * Save the Allowance WO from TimesheetAllowsServiceCreateReplyCollectionDTO
     * using the correct pay group from the employee.
     * @param result TimesheetAllowsServiceCreateReplyCollectionDTO
     */
    private void storeTimesheetAllowsPayGroup(Object result) {
        log.info("storeTimesheetAllowsPayGroup")
        TimesheetAllowsServiceCreateReplyCollectionDTO coll = (TimesheetAllowsServiceCreateReplyCollectionDTO) result
        coll.getReplyElements().each {TimesheetAllowsServiceCreateReplyDTO request ->
            log.info("AWA: TimesheetAllowsServiceCreateReplyDTO: ${request.getEmployee()} ${request.getPayGroup()} ${request.getAllowanceCode()} ${request.getSequenceNo()}")
            log.info("AWA: Custom Attributes values:")
            MSF820Rec empPayroll = readEmployeePayroll(request.getEmployee().trim())
            if(empPayroll) {
                log.info("AWA: Employee PayGroup ${empPayroll.getPayGroup().trim()}")
                //If the allowance's paygroup different with the employee's paygroup
                //store the allowance wo using employee's paygroup
                if(!empPayroll.getPayGroup().trim().equals(request.getPayGroup().trim())) {
                    String allowanceWO
                    List<Attribute> custAttribs = request.getCustomAttributes()
                    custAttribs.each{Attribute customAttribute ->
                        log.info("AWA: ${customAttribute.getName()} : ${customAttribute.getValue()}")
                        if(customAttribute.getName().equals(ALLOWANCEWO_CUSTOM_ATT)) {
                            allowanceWO = customAttribute.getValue()
                        }

                        if(allowanceWO) {
                            String entityKey = request.getEmployee()
                            entityKey = entityKey + request.getAllowanceCode()
                            entityKey = entityKey + TIME_FORMAT.format(request.getStartTime().getTime())
                            entityKey = entityKey + empPayroll.getPayGroup().trim()
                            entityKey = entityKey + request.getSequenceNo()
                            entityKey = entityKey + DATE_FORMAT.format(request.getTranDate().getTime())
                            saveCustomAttribute(entityKey, ALLOWANCEWO_ENTITY_TYPE+ALLOWANCEWO_CUSTOM_ATT, allowanceWO)
                        }
                    }
                }
            }
        }
    }

    /**
     * Read employee payroll
     * @param empId employee id
     * @retun employee payroll
     */
    private MSF820Rec readEmployeePayroll(String empId) {
        log.info("readEmployeePayroll")
        try {
            return tools.edoi.findByPrimaryKey(new MSF820Key(empId))
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
            log.info("Cannot read Employee Payroll caused by ${e.getMessage()}")
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
