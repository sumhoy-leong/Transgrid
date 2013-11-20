/**
 * @author Ventyx 2012
 *
 */

import java.text.SimpleDateFormat
import java.util.ArrayList

import com.mincom.ellipse.attribute.Attribute
import com.mincom.ellipse.edoi.ejb.msf0p5.*
import com.mincom.ellipse.edoi.ejb.msf891.*
import com.mincom.ellipse.hook.hooks.ServiceHook
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceDeleteReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceDeleteReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceDeleteRequestDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.timesheettrans.TimesheetTransServiceReadReplyDTO
import com.mincom.enterpriseservice.exception.*
import com.mincom.eql.impl.QueryImpl

class TimesheetAllowsService_multipleDelete extends ServiceHook{

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */

    private def version = 2
    private static final String ALLOWANCEWO_CUSTOM_ATT = "ALLOWANCEWO"
    private static final String ALLOWANCEWO_ENTITY_TYPE = "TimesheetAllowsService.TimesheetAllows."
    private static final String PAID_APPROVAL = "PAID"
    private ArrayList oriTimesheets = new ArrayList()

    @Override
    public Object onPreExecute(Object dto) {
        log.info("TimesheetAllowsService_multipleDelete onPreExecute - version: ${version}")
        Calendar prevTranDate = Calendar.getInstance()
        String approvalStatus = ""

        dto.each {
            TimesheetAllowsServiceDeleteRequestDTO request ->
            if(!prevTranDate.equals(request.getTranDate())){
                try{
                    TimesheetTransServiceReadReplyDTO tableReply =  tools.service.get("TimesheetTrans").read({
                        it.employee = request.getEmployee()
                        it.payGroup = request.getPayGroup()
                        it.sequenceNo = request.getSequenceNo()
                        it.tranDate = request.getTranDate()
                    })

                    approvalStatus = tableReply.getTranApprovalStatus()
                }catch(EnterpriseServiceOperationException e){
                    listErrors(e)
                }

                if(approvalStatus.equals(PAID_APPROVAL)) {
                    originalTimesheetAllows(request.getEmployee(), request.getPayGroup(),
                    request.getSequenceNo(), request.getTranDate())
                }
                prevTranDate = request.getTranDate()
            }
        }

        return null
    }

    @Override
    public Object onPostExecute(Object input, Object result) {
        log.info("TimesheetAllowsService_multipleDelete onPostExecute - version: ${version}")
        //store previous TimseheetAllows records
        prevTimesheetAllows()
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

        HashMap<TimesheetAllowsServiceRetrieveReplyDTO, String> oriTimesheet = new HashMap<TimesheetAllowsServiceRetrieveReplyDTO, String>()
        String AllowanceWo = ""
        try{
            def restart = ""
            List<TimesheetAllowsServiceRetrieveReplyDTO> replies = new ArrayList<TimesheetAllowsServiceRetrieveReplyDTO>()
            TimesheetAllowsServiceRetrieveReplyCollectionDTO timesheetReply = tools.service.get("TimesheetAllows").retrieve({
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
                String entityKey = ""
                Map.Entry timesheet = iterate.next()
                TimesheetAllowsServiceRetrieveReplyDTO oriTimesheetAllows = timesheet.getKey()
                if(!prevTranDate.equals(oriTimesheetAllows.getTranDate())){
                    //get maximum sequence
                    QueryImpl qMSF891 = new QueryImpl(MSF891Rec.class)
                            .and(MSF891Key.employeeId.equalTo(oriTimesheetAllows.getEmployee()))
                            .and(MSF891Key.payGroup.equalTo(oriTimesheetAllows.getPayGroup()))
                            .and(MSF891Key.trnDate.equalTo(new SimpleDateFormat("yyyyMMdd").format(oriTimesheetAllows.getTranDate().getTime()))).max(MSF891Key.seqNo)
                    maxSeqNo = tools.edoi.firstRow(qMSF891)
                    prevTranDate = oriTimesheetAllows.getTranDate()
                }
                entityKey = oriTimesheetAllows.getEmployee()
                entityKey = entityKey + oriTimesheetAllows.getAllowanceCode()
                entityKey = entityKey + new SimpleDateFormat("HHmm").format(oriTimesheetAllows.getStartTime().getTime())
                entityKey = entityKey + oriTimesheetAllows.getPayGroup()
                entityKey = entityKey + maxSeqNo
                entityKey = entityKey + new SimpleDateFormat("yyyyMMdd").format(oriTimesheetAllows.getTranDate().getTime())
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
