/**
 * Ventyx 2012
 *
 * This class is intended to provide common methods to access the MSO screen 
 * by utilising any functionalities provided in ScreenLibrary class
 *
 */
package com.mincom.ellipse.script.custom

import java.text.SimpleDateFormat

import com.mincom.ellipse.eroi.linkage.mss080.MSS080LINK
import com.mincom.ellipse.script.util.EROIWrapper


public class ScreenAppLibrary{
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private def version = 6

    private ScreenLibrary sl = new ScreenLibrary()

    /**
     * Read Invoice Header (MSO561 - Option 2)
     * 
     * @param InvoiceDTO
     * @return InvoiceResultDTO
     */
    public InvoiceResultDTO readInvoiceHeader(InvoiceDTO dto) {
        InvoiceResultDTO replyDto = new InvoiceResultDTO()

        try {
            sl.executeProgram("MSO561")
            sl.setField("OPTION1I", "2")
            sl.setField("AR_INV_NO1I", dto.arInvNo)

            sl.okUntilNextScreen("MSM565A")

            replyDto.arInvNo = sl.getField("AR_INV_NO1I").getValue()
            replyDto.invDesc = sl.getField("INV_DESC1I").getValue()
            replyDto.custNo = sl.getField("CUST_NO1I").getValue()
            replyDto.custName = sl.getField("CUST_NAME1I").getValue()
        } catch (ScreenErrorException e) {
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }

        return replyDto
    }

    /**
     * Update Invoice Header (MSO560 - Option 4)
     * 
     * @param InvoiceDTO
     * @return InvoiceResultDTO
     */
    public InvoiceResultDTO updateInvoiceHeader(InvoiceDTO dto) {
        InvoiceResultDTO replyDto = new InvoiceResultDTO()

        try {
            sl.executeProgram("MSO560")
            sl.setField("OPTION1I", "4")
            sl.setField("AR_INV_NO1I", dto.arInvNo)

            sl.okUntilNextScreen("MSM565A")

            replyDto.arInvNo = sl.getField("AR_INV_NO1I").getValue()
            replyDto.invDesc = sl.getField("INV_DESC1I").getValue()
            replyDto.custNo = sl.getField("CUST_NO1I").getValue()
            replyDto.custName = sl.getField("CUST_NAME1I").getValue()
        } catch (ScreenErrorException e) {
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }

        return replyDto
    }

    /**
     * Create Employee Payroll Transaction (MSO832)
     * 
     * @param dto
     * @return EmployeePayrollTransactionResultDTO
     */
    public EmployeePayrollTransactionResultDTO createEmployeePayrollTransaction(EmployeePayrollTransactionDTO dto){
        EmployeePayrollTransactionResultDTO replyDto = new EmployeePayrollTransactionResultDTO()

        try {
            sl.executeProgram("MSO832")

            sl.setField("EMP_ID1I1", dto.employeeId)
            sl.setField("TRAN_DATE1I1", dto.tranDate)
            sl.setField("TRAN_TYPE1I1", dto.tranType)
            sl.setField("TRAN_CODE1I1", dto.tranCode)
            sl.setField("TRAN_UNITS1I1", dto.tranUnits)
            sl.setField("RATE_REFNO1I1", dto.rateRefno)
            sl.setField("RATE_AMOUNT1I1", dto.rateAmt)
            sl.setField("TRAN_VALUE1I1", dto.tranValue)
            sl.setField("LVE_REQ_CODE1I1", dto.lveReqCode)
            sl.setField("LVE_REASON1I1", dto.lveReason)
            sl.setField("LVE_ST_DATE1I1", dto.lveStrDate)
            sl.setField("LVE_END_DATE1I1", dto.lveEndDate)
            sl.ok()

            if (sl.isError()) {
                replyDto.error = new ScreenError(sl.getScreenCodeMessage(), sl.getCurrentCursorMap())
            }
        } catch (ScreenErrorException e) {
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }

        return replyDto
    }


    /**
     * Create Report Request (MSS080)
     * 
     * @param eroi - EROIWrapper
     * @param dto - ReportRequestDTO
     * @return replyDto - ReportRequestResultDTO
     */
    public ReportRequestResultDTO createReportRequest(EROIWrapper eroi, ReportRequestDTO dto){
        ReportRequestResultDTO replyDto = new ReportRequestResultDTO()

        //Get todays date and time
        Calendar cal = Calendar.getInstance()
        String todayDate = new SimpleDateFormat("yyyyMMdd").format(cal.getTime())
        String todayTime = new SimpleDateFormat("HHmm").format(cal.getTime())

        //Call MSS080 to create request
        MSS080LINK mss080lnk = eroi.execute("MSS080",{MSS080LINK mss080lnk->
            mss080lnk.requestDate = todayDate
            mss080lnk.deferDate = todayDate
            mss080lnk.requestTime = todayTime
            if (dto.getProgName())
                mss080lnk.progName = dto.getProgName()
            if (dto.getDeferTime())
                mss080lnk.deferTime = dto.getDeferTime()
            if (dto.getRequestRecNo())
                mss080lnk.requestRecNo = dto.getRequestRecNo()
            if (dto.getRequestNo())
                mss080lnk.requestNo = dto.getRequestNo()
            if(dto.getUserId())
                mss080lnk.userId = dto.getUserId()
            if (dto.getRequestBy())
                mss080lnk.requestBy = dto.getRequestBy()
            if(dto.getDstrctCode())
                mss080lnk.dstrctCode = dto.getDstrctCode()
            if (dto.getRequestDstrct())
                mss080lnk.requestDstrct = dto.getRequestDstrct()
            if (dto.getProgReportId())
                mss080lnk.progReportId = dto.getProgReportId()
            if (dto.getMedium())
                mss080lnk.medium = dto.getMedium()
            if (dto.getJobId())
                mss080lnk.jobId = dto.getJobId()
            if (dto.getDistribCode())
                mss080lnk.distribCode = dto.getDistribCode()
            if (dto.getRetentionDays())
                mss080lnk.retentionDays = dto.getRetentionDays()
            if (dto.getTraceFlg())
                mss080lnk.traceFlg = dto.getTraceFlg()
            if (dto.getPubType())
                mss080lnk.pubType = dto.getPubType()
            if (dto.getLanguageCode())
                mss080lnk.languageCode = dto.getLanguageCode()
            if (dto.getTaskUuid())
                mss080lnk.taskUuid = dto.getTaskUuid()
            if (dto.getUuid())
                mss080lnk.uuid = dto.getUuid()
            if (dto.getRequestParams())
                mss080lnk.requestParams = dto.getRequestParams()
            mss080lnk.copyRqstSw = "Y"
            mss080lnk.printerRec = " "
        })

        if (!mss080lnk.getReturnStatus().equals("Y")) {
            replyDto.error = new ScreenError(mss080lnk.getReturnStatus(), mss080lnk.getErrorData().toString())
        }

        return replyDto
    }

    /**
     * Update Table's description (MSO010 - Option 2)
     *
     * @param TableDTO
     * @return TableResultDTO
     */
    public TableResultDTO updateTableDescription(TableDTO dto) {
        TableResultDTO replyDto = new TableResultDTO()

        try {
            sl.executeProgram("MSO010")
            sl.setField("OPTION1I", "2")
            sl.setField("TABLE_TYPE1I", dto.getTableType())
            sl.setField("TABLE_CODE1I", dto.getTableCode())
            sl.okUntilNextScreen("MSM010B")


            sl.setField("TABLE_DESC2I1", dto.getDescription())
            sl.okUntilNextScreen("MSM010A")

            if (sl.isError()) {
                replyDto.error = new ScreenError(sl.getScreenCodeMessage(), sl.getCurrentCursorMap())
            }
        } catch (ScreenErrorException e) {
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }

        sl.functionKey(1)
        return replyDto
    }

    /**
     * Review Equipment Classification information (MSO600 - Option 6)
     * @param dto
     * @return EquipmentResultDTO
     */
    public EquipmentResultDTO readEquipmentClassif(EquipmentDTO dto){
        EquipmentResultDTO replyDto = new EquipmentResultDTO()

        try{
            sl.executeProgram("MSO600")
            sl.setField("OPTION1I", "6")
            sl.setField("EQUIP_NO1I", dto.getEquipNo())

            sl.okUntilNextScreen("MSM60DA")

            replyDto.equipNo = sl.getField("EQUIP_NO1I").getValue()
            replyDto.itemNameCode = sl.getField("ITEM_NAME_CODE1I").getValue()
            replyDto.itemName1 = sl.getField("ITEM_NAME_11I").getValue()
            replyDto.itemName2 = sl.getField("ITEM_NAME_21I").getValue()
            replyDto.plantCodeA = sl.getField("PLANT_CODE_A1I").getValue()
            replyDto.plantCodeB = sl.getField("PLANT_CODE_B1I").getValue()
            replyDto.plantCodeC = sl.getField("PLANT_CODE_C1I").getValue()
            replyDto.plantCodeD = sl.getField("PLANT_CODE_D1I").getValue()
            replyDto.plantCodeE = sl.getField("PLANT_CODE_F1I").getValue()
            replyDto.plantCodeF = sl.getField("PLANT_CODE_F1I").getValue()
            replyDto.equipmentClassif0 = sl.getField("EQ_CLASSIFA1I1").getValue()
            replyDto.equipmentClassif1 = sl.getField("EQ_CLASSIFB1I1").getValue()
            replyDto.equipmentClassif2 = sl.getField("EQ_CLASSIFA1I2").getValue()
            replyDto.equipmentClassif3 = sl.getField("EQ_CLASSIFB1I2").getValue()
            replyDto.equipmentClassif4 = sl.getField("EQ_CLASSIFA1I3").getValue()
            replyDto.equipmentClassif5 = sl.getField("EQ_CLASSIFB1I3").getValue()
            replyDto.equipmentClassif6 = sl.getField("EQ_CLASSIFA1I4").getValue()
            replyDto.equipmentClassif7 = sl.getField("EQ_CLASSIFB1I4").getValue()
            replyDto.equipmentClassif8 = sl.getField("EQ_CLASSIFA1I5").getValue()
            replyDto.equipmentClassif9 = sl.getField("EQ_CLASSIFB1I5").getValue()
            replyDto.equipmentClassif10 = sl.getField("EQ_CLASSIFA1I6").getValue()
            replyDto.equipmentClassif11 = sl.getField("EQ_CLASSIFB1I6").getValue()
            replyDto.equipmentClassif12 = sl.getField("EQ_CLASSIFA1I7").getValue()
            replyDto.equipmentClassif13 = sl.getField("EQ_CLASSIFB1I7").getValue()
            replyDto.equipmentClassif14 = sl.getField("EQ_CLASSIFA1I8").getValue()
            replyDto.equipmentClassif15 = sl.getField("EQ_CLASSIFB1I8").getValue()
            replyDto.equipmentClassif16 = sl.getField("EQ_CLASSIFA1I9").getValue()
            replyDto.equipmentClassif17 = sl.getField("EQ_CLASSIFB1I9").getValue()
            replyDto.equipmentClassif18 = sl.getField("EQ_CLASSIFA1I10").getValue()
            replyDto.equipmentClassif19 = sl.getField("EQ_CLASSIFB1I10").getValue()
        }
        catch(ScreenErrorException e){
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }
        sl.functionKey(1)
        return replyDto
    }

    /**
     * Review Employee Personnel details (MSO760)
     * @param dto
     * @return EmployeePersonnelResultDTO
     */
    public EmployeePersonnelResultDTO readEmployeePersonnelDetails(EmployeePersonnelDTO dto){
        EmployeePersonnelResultDTO replyDto = new EmployeePersonnelResultDTO();

        try{
            sl.executeProgram("MSO760");
            sl.setField("NEXT_EMP1I", dto.getNextEemployeeId());

            sl.ok();

            replyDto.employeeId          = sl.getField("EMP_ID1I").getValue();
            replyDto.name                = sl.getField("EMP_NAME1I").getValue();
            replyDto.position            = sl.getField("POSITION1I").getValue();
            replyDto.positionTitle       = sl.getField("POSITION_TITLE1I").getValue();
            replyDto.posReason           = sl.getField("POS_REASON1I").getValue();
            replyDto.prc                 = sl.getField("PRC_CODE1I").getValue();
            replyDto.prcDesc             = sl.getField("PRC_DESC1I").getValue();
            replyDto.birthDate           = sl.getField("BIRTH_DATE1I").getValue();
            replyDto.gender              = sl.getField("GENDER1I").getValue();
            replyDto.genderDesc          = sl.getField("GENDER_DESC1I").getValue();
            replyDto.maritalStatus       = sl.getField("MARITAL_STATUS1I").getValue();
            replyDto.maritalStatusDesc   = sl.getField("MARITAL_DESC1I").getValue();
            replyDto.noOfDependants      = sl.getField("DEPENDANTS1I").getValue();
            replyDto.prevLastName        = sl.getField("PREV_SURNAME1I").getValue();
            replyDto.deathDate           = sl.getField("DEATH_DATE1I").getValue();
            replyDto.deathReason         = sl.getField("DEATH_REASON1I").getValue();
            replyDto.deathReasonDesc     = sl.getField("DEATH_RSN_DESC1I").getValue();
            replyDto.empType             = sl.getField("EMP_TYPE1I").getValue();
            replyDto.empTypeDesc         = sl.getField("EMP_DESC1I").getValue();
            replyDto.staffCategory       = sl.getField("STAFF_CATEG1I").getValue();
            replyDto.staffCategoryDesc   = sl.getField("STAFF_DESC1I").getValue();
            replyDto.union               = sl.getField("UNION1I").getValue();
            replyDto.unionDesc           = sl.getField("UNION_DESC1I").getValue();
            replyDto.jobClassLevel       = sl.getField("JOB_CL_LVL1I").getValue();
            replyDto.jobClassLevelDesc   = sl.getField("JCL_DESC1I").getValue();
            replyDto.extTest             = sl.getField("EXT_TEXT1I").getValue();
            replyDto.hireDate            = sl.getField("HIRE_DATE1I").getValue();
            replyDto.serviceDate         = sl.getField("SERVICE_DATE1I").getValue();
            replyDto.profServiceDate     = sl.getField("PRO_SERV_DATE1I").getValue();
            replyDto.reinstatementDate   = sl.getField("REINST_DATE1I").getValue();
            replyDto.status              = sl.getField("STATUS1I").getValue();
            replyDto.statusDesc          = sl.getField("STATUS_DESC1I").getValue();
            replyDto.suspensionDate      = sl.getField("SUSPEND_DATE1I").getValue();
            replyDto.terminationDate     = sl.getField("TERM_DATE1I").getValue();
            replyDto.bonaFideTerm        = sl.getField("BONAFIDE_TERM1I").getValue()
            replyDto.rehireCode          = sl.getField("REHIRE_CODE1I").getValue();
            replyDto.rehireCodeDesc      = sl.getField("REHIRE_DESC1I").getValue();
            replyDto.retirementDate      = sl.getField("RETIRE_DATE1I").getValue();
            replyDto.personnelStatus     = sl.getField("PER_STATUS1I").getValue();
            replyDto.personnelStatusDesc = sl.getField("PER_STATUS_DESC1I").getValue();
            replyDto.previousEmployeeId  = sl.getField("PREV_EMPID1I").getValue();
            replyDto.exFromTalentMgtExt  = sl.getField("EXC_NGA_EXTRACT1I").getValue();
            replyDto.candidateId         = sl.getField("CANDIDATE_ID1I").getValue();
            replyDto.physLocation        = sl.getField("PHYSICAL_LOC1I").getValue();
            replyDto.physLocationDesc    = sl.getField("PHYSICAL_DESC1I").getValue();
            replyDto.physLocReason       = sl.getField("REASON1I").getValue();
            replyDto.workLocation        = sl.getField("WORK_LOC1I").getValue();
            replyDto.workLocationDesc    = sl.getField("WORK_DESC1I").getValue();
            replyDto.dataRef             = sl.getField("DATA_REF1I").getValue();
            replyDto.errorMessage        = sl.getField("ERRMESS1I").getValue();
        }
        catch(ScreenErrorException e){
            replyDto.error= new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto;
    }


    /**
     * Create Employee Appraisal (MSO795)
     * 
     * Option 1 - Create
     * Option 2 - Update
     *
     * @param EmployeeAppraisalDTO
     * @return ScreenResultDTO
     */
    public ScreenResultDTO processEmployeeAppraisal(EmployeeAppraisalDTO dto, String option) {
        ScreenResultDTO replyDto = new ScreenResultDTO()

        try {

            sl.executeProgram("MSO795")
            if(sl.getScreenName().equals("MSM795B")) {
                sl.functionKey(3)
            }

            sl.setField("OPTION1I", option)
            sl.setField("EMPLOYEE1I", dto.employeeId)
            sl.setField("DATE_EFF1I", dto.effectiveDate)

            sl.okUntilNextScreen("MSM795B")

            sl.setField("APP_REASON2I", dto.appraisalReason)
            sl.setField("APP_ID2I", dto.appraiser)
            sl.setField("PERIOD_FROM2I", dto.periodFromDate)
            sl.setField("PERIOD_TO2I", dto.periodToDate)
            sl.setField("SUMM_RATING2I", dto.summaryRating)

            sl.okUntilFieldBlank("EMP_ID2I")

        } catch (ScreenErrorException e) {
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }

        return replyDto
    }

    /**
     * Review an Award/Terms of Employment (General Details)
     * MSO8FA - Option 5
     * @param dto
     * @return AwardDetailsResultDTO
     */
    public AwardDetailsResultDTO readAwardDetails(AwardDetailsDTO dto){
        AwardDetailsResultDTO replyDto = new AwardDetailsResultDTO();
        try{
            sl.executeProgram("MSO8FA");
            sl.setField("OPTION1I", "5");
            sl.setField("AWARD_CODE1I", dto.getAwardCode());
            replyDto.errorMessage = sl.getField("ERRMESS1I")

            sl.okUntilNextScreen("MSM8FAB")

            replyDto.awardCode    = sl.getField("AWARD_CODE2I").getValue();
            replyDto.stdHrsPrdC0  = sl.getField("HRS_PER_PRD2I").getValue();
            replyDto.stdHrsWkC0   = sl.getField("HRS_PER_WEEK2I").getValue();
            replyDto.payFrqC0     = sl.getField("PAY_FREQ2I").getValue();
            replyDto.errorMessage = sl.getField("ERRMESS2I").getValue();
        }catch(ScreenErrorException e){
            replyDto.error= new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto;
    }

    /**
     * Review Employee Leave Payout Details - MSO8TL
     * @param dto
     * @return EmpLeavePayoutResultDTO
     */
    public EmployeeLeavePayoutResultDTO readEmployeeLeavePayout(EmployeeLeavePayoutDTO dto){
        EmployeeLeavePayoutResultDTO replyDto = new EmployeeLeavePayoutResultDTO();
        try{
            sl.executeProgram("MSO8TL");
            sl.setField("NEXT_EMP1I", dto.getEmployeeId());

            sl.ok();

            replyDto.employeeId = dto.getEmployeeId()
            replyDto.exitType = sl.getField("EXIT_TYPE1I").getValue()
            replyDto.exitDate = sl.getField("EXIT_DATE1I").getValue()
            replyDto.errorMessage = sl.getField("ERRMESS1I").getValue()

        }catch(ScreenErrorException e){
            replyDto.error= new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto
    }

    /**
     * Review Employee Payroll Details - MSO82P
     * @param dto
     * @return EmployeePayrollDetailsResultDTO
     */
    public EmployeePayrollDetailsResultDTO readEmployeePayrollDetails(EmployeePayrollDetailsDTO dto){
        EmployeePayrollDetailsResultDTO replyDto = new EmployeePayrollDetailsResultDTO();
        try{
            sl.executeProgram("MSO82P");
            sl.setField("NEXT_EMP1I", dto.getEmployeeId());

            sl.ok();

            replyDto.employeeId = sl.getField("EMP_ID1I").getValue()
            replyDto.employeeClass = sl.getField("EMP_CLASS1I").getValue()
            replyDto.payGroup = sl.getField("PAY_GROUP1I").getValue()
            replyDto.contractHours = sl.getField("CONTRACT_HOURS1I").getValue()
            replyDto.payLocation = sl.getField("PAY_LOCATION1I").getValue()
            replyDto.errorMessage = sl.getField("ERRMESS1I").getValue()

        }catch(ScreenErrorException e){
            replyDto.error= new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto;
    }
    /**
     * Review Employee Costing Details
     * @param dto
     * @return EmployeeCostingDetailsResultDTO
     */
    public EmployeeCostingDetailsResultDTO readEmployeeCostingDetails(EmployeeCostingDetailsDTO dto){
        EmployeeCostingDetailsResultDTO replyDto = new EmployeeCostingDetailsResultDTO()
        try{
            sl.executeProgram("MSO82C");
            sl.setField("NEXT_EMP1I", dto.getEmployeeId());

            sl.ok();

            replyDto.employeeId = sl.getField("EMP_NO1I").getValue()
            replyDto.currentPosition = sl.getField("POSITION_ID1I").getValue()
            replyDto.costAccountCode1 = sl.getField("CST_ACC_CODE1I1").getValue()
            replyDto.costAccountCode2 = sl.getField("CST_ACC_CODE1I2").getValue()
            replyDto.costAccountCode3 = sl.getField("CST_ACC_CODE1I3").getValue()
            replyDto.costAccountCode4 = sl.getField("CST_ACC_CODE1I4").getValue()
            replyDto.costAccountCode5 = sl.getField("CST_ACC_CODE1I5").getValue()
            replyDto.errorMessage = sl.getField("ERRMESS1I").getValue()

        }catch(ScreenErrorException e){
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto;
    }

    /**
     * Review District Inventory Control details (MSO170)
     * @param dto
     * @return DstrctStockCodeResultDTO
     */
    public DstrctStockCodeResultDTO readDistrictStockCodeDetails(DstrctStockCodeDTO dto){
        DstrctStockCodeResultDTO replyDto = new DstrctStockCodeResultDTO();

        try{
            sl.executeProgram("MSO170")
            sl.setField("OPTION1I", "4")
            sl.setField("STOCK_CODE1I", dto.stockCode)


            sl.okUntilNextScreen("MSM170B");

            replyDto.stockCode        = sl.getField("STOCK_CODE2I").getValue();
            replyDto.itemName         = sl.getField("ITEM_NAME2I").getValue();

        }
        catch(ScreenErrorException e){
            replyDto.error= new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto;
    }

    /**
     * Read Table Code (MSO010 - Option 2)
     *
     * @param TableDTO
     * @return TableResultDTO
     */
    public TableResultDTO readTableCode(TableDTO dto) {
        TableResultDTO replyDto = new TableResultDTO()
        println("KOILORD checkTableCode")
        try{
            sl.executeProgram("MSO010")
            sl.setField("OPTION1I", "2")
            sl.setField("TABLE_TYPE1I", dto.getTableType())
            sl.setField("TABLE_CODE1I", dto.getTableCode())
            sl.okUntilNextScreen("MSM010B")

            println("KOILORD TABLE_CODE2I1 : "+sl.getField("TABLE_CODE2I1").getValue().trim())

            if(!sl.getField("TABLE_CODE2I1").getValue().trim().equals(dto.getTableCode().trim()))
                new ScreenError("2104", "NO MORE TABLE CODE EXIST")
        } catch (ScreenErrorException e) {
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue)
        }

        sl.functionKey(1)
        return replyDto
    }


    /**
     * Create new Journal (MSO905)
     * @param dto JournalDTO
     * @return JournalResultDTO
     */
    public JournalResultDTO createJournal(JournalDTO dto) {
        JournalResultDTO replyDto = new JournalResultDTO()
        try{
            sl.executeProgram("MSO905")
            sl.setField("OPTION1I", "3")

            sl.okUntilNextScreen("MSM906A")

            sl.setField("JOURNAL_DESC1I", dto.description)
            sl.setField("ACCOUNTANT1I", dto.accountant)
            sl.setField("APPROVAL_STAT1I", dto.approvalStatus)
            sl.setField("ACCRUAL_IND1I", "N")
            sl.setField("TRANS_DATE1I", dto.tranDate)
            int i = 1
            for(JournalEntryDTO e : dto.entries) {
                sl.setField("ACCOUNT_CODE1I${i}", e.accountCode)
                sl.setField("TRAN_AMOUNT1I${i}", e.ammount)
                sl.setField("WORK_PROJ1I${i}", e.woProjectNo)
                sl.setField("WORK_PROJ_IND1I${i}", e.woProjectIndicator)
                i++
            }

            sl.okUntilFieldPopulated("JOURNAL_NO1I")

            replyDto.journalNo      = sl.getField("JOURNAL_NO1I").getValue()
            replyDto.description    = sl.getField("JOURNAL_DESC1I").getValue()
            replyDto.accountant     = sl.getField("ACCOUNTANT1I").getValue()
            replyDto.approvalStatus = sl.getField("APPROVAL_STAT1I").getValue()
        }
        catch(ScreenErrorException e){
            replyDto.error = new ScreenError(e.code, e.message, e.currentCursorField, e.currentCursorValue);
        }
        sl.functionKey(1);
        return replyDto;
    }
}