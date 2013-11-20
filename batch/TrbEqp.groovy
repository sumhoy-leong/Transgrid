/**
 *  @Ventyx 2012
 *
 * This program test open Database connection when calling a service multiple times.
 */
package com.mincom.ellipse.script.custom

import groovy.lang.Binding;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.mincom.batch.environment.*;
import com.mincom.batch.script.*;
import com.mincom.ellipse.attribute.Attribute;
import com.mincom.ellipse.client.connection.*;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_ADKey;
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_ADRec;
import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf0p5.MSF0P5Key;
import com.mincom.ellipse.edoi.ejb.msf0p5.MSF0P5Rec;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf763.*;
import com.mincom.ellipse.edoi.ejb.msf766.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf823.*;
import com.mincom.ellipse.edoi.ejb.msf824.*;
import com.mincom.ellipse.edoi.ejb.msf827.*;
import com.mincom.ellipse.edoi.ejb.msf829.*;
import com.mincom.ellipse.edoi.ejb.msf830.*;
import com.mincom.ellipse.edoi.ejb.msf837.*;
import com.mincom.ellipse.edoi.ejb.msf840.*;
import com.mincom.ellipse.edoi.ejb.msf878.*;
import com.mincom.ellipse.edoi.ejb.msf880.*;
import com.mincom.ellipse.edoi.ejb.msf888.*;
import com.mincom.ellipse.edoi.ejb.msf89w.MSF89WKey;
import com.mincom.ellipse.edoi.ejb.msf89w.MSF89WRec;
import com.mincom.ellipse.edoi.ejb.msf900.MSF900Key;
import com.mincom.ellipse.edoi.ejb.msfx99.MSFX99Key;
import com.mincom.ellipse.ejra.mso.*;
import com.mincom.ellipse.eroi.linkage.mssbnk.MSSBNKLINK;
import com.mincom.ellipse.script.util.*;
import com.mincom.enterpriseservice.ellipse.*;
import com.mincom.enterpriseservice.ellipse.equipment.*;
import com.mincom.enterpriseservice.ellipse.equiptrace.*;
import com.mincom.enterpriseservice.ellipse.higherduties.*;
import com.mincom.enterpriseservice.ellipse.payrollemp.*;
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.timesheetallows.TimesheetAllowsServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.workorder.*;
import com.mincom.enterpriseservice.exception.*;
import com.mincom.eql.*;
import com.mincom.eql.impl.*;

import com.mincom.reporting.text.TextReport;

class ParamsRestart {
    String x;
}

public class ProcessTrbeqp extends SuperBatch {
    private static final String ALLOWANCE_WO_ATTR       = "ALLOWANCEWO"
    private static final String CREDIT_DATE_ATTR        = "CREDITDATE"
    private static final String CREDIT_TIME_ATTR        = "CREDITTIME"
    private static final String CREDIT_JNL_ATTR         = "CREDITJNL"
    private static final String DEBIT_DATE_ATTR         = "DEBITDATE"
    private static final String DEBIT_TIME_ATTR         = "DEBITTIME"
    private static final String DEBIT_JNL_ATTR          = "DEBITJNL"
    private static final String TEST_ATTR               = "testJnlNo"
    private static final String TIMESHEET_ALLOWS_ENTITY_TYPE = "TimesheetAllowsService.TimesheetAllows."

    private static final SimpleDateFormat TRN_DATE_FRMT = new SimpleDateFormat("yyyyMMdd")

    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private String version = "001";
    private def ParamsRestart p
    private TextReport reportA
    
    public void runBatch(Binding b){
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        p = params.fill(new ParamsRestart())
        // PrintRequest Parameters
        info("params : " + p.toString())

        try {
            processBatch();
        } finally {
            printBatchReport();
        }
    }

    private void processBatch(){
        info("processBatch");
        reportA = report.open("TRBEQPA")
        reportA.write("TEST WRITE REPORT")
        reportA.close()
//        validateBankAccount(" ", "003-251", "062-001")
//        validateBankAccount("GRID", "327967", "032-006")
    }

    private void printBatchReport(){
        info("printBatchReport");
        //print batch report
        info ("Report Finish")
    }

    private boolean validateBankAccount(String district, String accountNumber,
    String branchCode) {

        info("validateBankAccount ${district} - ${accountNumber} - ${branchCode}")

        MSF000_ADRec msf000_ADRec =  readDistrict(district)
        if(msf000_ADRec) {
            info("${district} is active [${msf000_ADRec.getDstrctStatus()}]")
        }

        MSSBNKLINK mssBnklink = eroi.execute("MSSBNK", {MSSBNKLINK mssBnk->
            mssBnk.setOption("V")
            mssBnk.setInpDstrctCode(district)
            mssBnk.setInpBankAcctNo(accountNumber)
            mssBnk.setInpBranchCode(branchCode)
        })

        boolean validBankAcct = false
        info("MSSBNKLINK return status is [${mssBnklink.getReturnStatus()}]")

        switch(mssBnklink.getReturnStatus()) {
            case "0":
                validBankAcct = true
                break
            case "1":
                validBankAcct = false
                info("Bank Account Not Avaliable for Use ${accountNumber}.")
                break
            case "2":
                validBankAcct = false
                info("Invalid Bank Account Owner ${accountNumber}.")
                break
            case "3":
                validBankAcct = false
                info("Invalid Bank District. ${accountNumber}")
                break
            case "4":
                validBankAcct = false
                info("Invalid Bank Account ${accountNumber}.")
                break
            default:
                validBankAcct = false
                info("Invalid Input ACCOUNT NUMBER/BRANCH CODE.")
                break
        }
        return validBankAcct
    }

    /**
     * Read the district from MSF000_AD
     * @param dstrctCode district code
     * @return MSF000_ADRec
     */
    private MSF000_ADRec readDistrict(String dstrctCode) {
        info("readDistrict")

        try {
            MSF000_ADKey msf000ADKey = new MSF000_ADKey()
            msf000ADKey.setControlRecNo(dstrctCode)
            msf000ADKey.setControlRecType("AD")
            msf000ADKey.setDstrctCode(" ")
            return edoi.findByPrimaryKey(msf000ADKey)
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("msf000ADKey ${dstrctCode} not found!")
            return null
        }
    }

    private void readTimesheetAllowanceRecord() {
        info("readTimesheetAllowanceRecord")

        MSF89WKey msf89wKey = new MSF89WKey()
        msf89wKey.setEmployeeId("0000067430")
        msf89wKey.setEarnCode("456")
        msf89wKey.setPayGroup("TG1")
        msf89wKey.setSeqNo("0001")
        msf89wKey.setTrnDate("20121202")
        msf89wKey.setFrmTime("00000")
        MSF89WRec msf89wRec
        try {
            msf89wRec = edoi.findByPrimaryKey(msf89wKey)
            info("AWA: Write custom attributes.")
            writeCustomAttributes(msf89wRec, TEST_ATTR, "TEST-AWA-1")

            info("AWA: Read TimesheetAllows with required attributes.")
            TimesheetAllowsServiceReadReplyDTO tsAllowanceReplyDTO = readTimesheetAllowance(msf89wRec, true)
            if(tsAllowanceReplyDTO) {
                for(Attribute a : tsAllowanceReplyDTO.getCustomAttributes()) {
                    info("AWA: Attribute name ${a.getName()}")
                    info("AWA: Attribute val  ${a.getValue()}")
                }
            }
            info("AWA: Read TimesheetAllows without required attributes.")
            tsAllowanceReplyDTO = readTimesheetAllowance(msf89wRec, false)
            if(tsAllowanceReplyDTO) {
                for(Attribute a : tsAllowanceReplyDTO.getCustomAttributes()) {
                    info("AWA: Attribute name ${a.getName()}")
                    info("AWA: Attribute val  ${a.getValue()}")
                }
            }
        } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
            info("MSFX69 does not exist")
        }
    }

    /**
     * Read Timesheet Allowance based on Allowance record.
     * @param msf89wRec Allowance record
     * @param useReqAttr flag to indicate use required attributes or not
     * @return Timesheet Allowance DTO
     */
    private TimesheetAllowsServiceReadReplyDTO readTimesheetAllowance(MSF89WRec msf89wRec, boolean useReqAttr) {
        info("readTimesheetAllowance")
        TimesheetAllowsServiceReadReplyDTO tsAllowanceReplyDTO = null
        try {
            TimesheetAllowsServiceReadRequestDTO tsAllowanceReadDTO = new TimesheetAllowsServiceReadRequestDTO()
            tsAllowanceReadDTO.setEmployee(msf89wRec.getPrimaryKey().getEmployeeId())
            tsAllowanceReadDTO.setPayGroup(msf89wRec.getPrimaryKey().getPayGroup())
            tsAllowanceReadDTO.setAllowanceCode(msf89wRec.getPrimaryKey().getEarnCode())
            tsAllowanceReadDTO.setSequenceNo(msf89wRec.getPrimaryKey().getSeqNo())
            Calendar c = Calendar.getInstance()
            c.setTime(TRN_DATE_FRMT.parse(msf89wRec.getPrimaryKey().getTrnDate()))
            tsAllowanceReadDTO.setTranDate(c)
            if(useReqAttr) {
                TimesheetAllowsServiceReadRequiredAttributesDTO tsAllowanceReadAttr = new TimesheetAllowsServiceReadRequiredAttributesDTO()
                tsAllowanceReadAttr.setReturnEmployee(true)
                tsAllowanceReadAttr.setReturnOriginalAllowanceCode(true)
                tsAllowanceReadAttr.setReturnAllowanceCode(true)
                tsAllowanceReadAttr.setReturnPayGroup(true)
                tsAllowanceReadAttr.setReturnSequenceNo(true)
                tsAllowanceReadAttr.setReturnTranDate(true)
                tsAllowanceReadAttr.setReturnOriginalStartTime(true)
                tsAllowanceReadAttr.setReturnStartTime(true)
                tsAllowanceReadAttr.setReturnStopTime(true)
                tsAllowanceReadAttr.setReturnAllowanceAmount(true)
                tsAllowanceReadAttr.setReturnAllowanceUnits(true)
                tsAllowanceReadAttr.setReturnLastModDate(true)
                tsAllowanceReadAttr.setReturnLastModTime(true)

                tsAllowanceReadAttr.requiredAttributes.put(ALLOWANCE_WO_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put(CREDIT_DATE_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put(CREDIT_TIME_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put(CREDIT_JNL_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put(DEBIT_DATE_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put(DEBIT_TIME_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put(DEBIT_JNL_ATTR, true)
                tsAllowanceReadAttr.requiredAttributes.put("testJnlNo", true)
                tsAllowanceReadDTO.setRequiredAttributes(tsAllowanceReadAttr)
                info("AWA: Required Attributes is set.")
            }

            tsAllowanceReplyDTO = service.get("TIMESHEETALLOWS").read(tsAllowanceReadDTO)
        } catch(EnterpriseServiceOperationException serviceExc) {
            tsAllowanceReplyDTO = null
            logExceptionService("TIMESHEETALLOWS", serviceExc)
        }
        return tsAllowanceReplyDTO
    }

    /**
     * Write custom attributes value for specified allowance.
     * @param msf89wRec allowance record
     * @param entityType custom attribute type
     * @param propertyValue custom attribute value
     */
    private void writeCustomAttributes(MSF89WRec msf89wRec, String entityType, String propertyValue) {
        info("writeCustomAttributes")
        StringBuilder entityKey = new StringBuilder()
        entityKey.append(msf89wRec.getPrimaryKey().getEmployeeId())
        entityKey.append(msf89wRec.getPrimaryKey().getEarnCode())
        entityKey.append(msf89wRec.getPrimaryKey().getFrmTime()?.substring(0,4))
        entityKey.append(msf89wRec.getPrimaryKey().getPayGroup())
        entityKey.append(msf89wRec.getPrimaryKey().getSeqNo())
        entityKey.append(msf89wRec.getPrimaryKey().getTrnDate())
        entityType = TIMESHEET_ALLOWS_ENTITY_TYPE.concat(entityType)

        MSF0P5Key msf0p5Key = new MSF0P5Key()
        MSF0P5Rec msf0p5Rec = new MSF0P5Rec()
        msf0p5Rec.setPrimaryKey(msf0p5Key)
        msf0p5Rec.setEntityKey(entityKey.toString())
        msf0p5Rec.setEntityType(entityType)
        msf0p5Rec.setPropertyValue(propertyValue)

        edoi.create(msf0p5Rec)
    }

    /**
     * Log the EnterpriseServiceOperationException.
     * @param serviceName name of the executed service
     * @param serviceExc EnterpriseServiceOperationException
     */
    private void logExceptionService(String serviceName, EnterpriseServiceOperationException serviceExc) {
        info("logExceptionService")
        String errorMsg   = serviceExc.getErrorMessages()[0].getMessage()
        String errorCode  = serviceExc.getErrorMessages()[0].getCode().replace("mims.e.","")
        String errorField = serviceExc.getErrorMessages()[0].getFieldName()
        info("Error during execute ${serviceName} caused by ${errorField}: ${errorCode} - ${errorMsg}.")
    }

    private void processWorkOrder() {
        info ("processWorkOrder")
        String saveDstrctCode = "GRID"
        def query = new QueryImpl(MSF620Rec.class).
                and(MSF620Key.dstrctCode.equalTo(saveDstrctCode)).
                and(MSF620Key.workOrder.equalTo(MSFX99Key.workOrder)).
                and(MSFX99Key.dstrctCode.equalTo(MSF620Key.dstrctCode)).
                and(MSFX99Key.rec900Type.equalTo("L")).
                and(MSFX99Key.fullPeriod.greaterThanEqualTo("201201")).
                and(MSFX99Key.fullPeriod.lessThanEqualTo("201212")).
                and(MSF900Key.dstrctCode.equalTo(MSFX99Key.dstrctCode)).
                and(MSF900Key.processDate.equalTo(MSFX99Key.processDate)).
                and(MSF900Key.transactionNo.equalTo(MSFX99Key.transactionNo)).
                and(MSF900Key.userno.equalTo(MSFX99Key.userno)).
                and(MSF900Key.rec900Type.equalTo(MSFX99Key.rec900Type))

        edoi.search(query) {
            info("AWA:1 " + it[0])
            info("AWA:2 " + it[1])
            info("AWA:3 " + it[2])
        }
    }
}

/*runscript*/

ProcessTrbeqp process = new ProcessTrbeqp();
process.runBatch(binding);