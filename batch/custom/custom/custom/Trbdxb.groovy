package com.mincom.ellipse.script.custom;

import com.mincom.ellipse.edoi.ejb.msfx69.MSFX69Rec;
import com.mincom.ellipse.edoi.ejb.msfx69.MSFX69Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import com.mincom.batch.request.Request;
import com.mincom.batch.environment.*;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.common.logger.EDOILogger;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.ellipse.types.m0000.instances.MktName
import com.mincom.ellipse.types.m3110.instances.MarketPlaceDTO
import com.mincom.ellipse.types.m3110.instances.MarketPlaceServiceResult
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import org.apache.commons.lang.StringUtils
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.ejra.mso.GenericMsoRecord
import com.mincom.ellipse.eroi.linkage.mss080.MSS080LINK;
import com.mincom.ellipse.eroi.linkage.mss080.MSS080LINKBinding;
import com.mincom.enterpriseservice.ellipse.*
import com.mincom.ellipse.client.connection.*
import com.mincom.ellipse.ejra.mso.*;
import com.mincom.ellipse.edoi.ejb.msf011.MSF011Key;
import com.mincom.ellipse.edoi.ejb.msf011.MSF011Rec;
import com.mincom.ellipse.edoi.ejb.msf012.MSF012Rec;
import com.mincom.ellipse.edoi.ejb.msf096.MSF096Rec;
import com.mincom.ellipse.edoi.ejb.msf100.MSF100Rec;
import com.mincom.ellipse.edoi.ejb.msf345.MSF345Key;
import com.mincom.ellipse.edoi.ejb.msf345.MSF345Rec;
import com.mincom.ellipse.edoi.ejb.msf580.MSF580Rec;
import com.mincom.ellipse.edoi.ejb.msf600.MSF600Rec;
import com.mincom.ellipse.edoi.ejb.msf601.MSF601Rec;
import com.mincom.ellipse.edoi.ejb.msf602.MSF602Rec;
import com.mincom.ellipse.edoi.ejb.msf619.MSF619Rec;
import com.mincom.ellipse.edoi.ejb.msf650.MSF650Rec;
import com.mincom.ellipse.edoi.ejb.msf6a1.MSF6A1Rec;
import com.mincom.ellipse.edoi.ejb.msf900.MSF900Rec;
import com.mincom.ellipse.edoi.ejb.msf910.MSF910Rec;
import com.mincom.ellipse.edoi.ejb.msf920.MSF920Rec;
import com.mincom.ellipse.edoi.ejb.msf930.MSF930Rec;
import com.mincom.ellipse.edoi.ejb.msf940.MSF940Rec;
import com.mincom.ellipse.edoi.ejb.msf966.MSF966Rec;
import com.mincom.ellipse.edoi.ejb.msf967.MSF967Rec;
import com.mincom.ellipse.edoi.ejb.msf968.MSF968Rec;
import com.mincom.ellipse.edoi.ejb.msfx61.MSFX61Rec;
import com.mincom.ellipse.edoi.ejb.msfx63.MSFX63Rec;
import com.mincom.ellipse.edoi.ejb.msfx65.MSFX65Rec;
import com.mincom.ellipse.edoi.ejb.msfx68.MSFX68Rec;
import com.mincom.ellipse.edoi.ejb.msfx69.MSFX69Rec;
import com.mincom.ellipse.edoi.ejb.msfx6a.MSFX6ARec;
import com.mincom.ellipse.edoi.ejb.msfx6c.MSFX6CRec;
import com.mincom.ellipse.edoi.ejb.msfx6e.MSFX6ERec;
import com.mincom.ellipse.edoi.ejb.msfx6f.MSFX6FRec;
import com.mincom.ellipse.edoi.ejb.msfx6j.MSFX6JRec;
import com.mincom.ellipse.edoi.ejb.msf605.MSF605Rec;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequestDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadRequestDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.equiptrace.EquipTraceServiceRetrieveFitEquipTracingReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.equiptrace.EquipTraceServiceRetrieveFitEquipTracingReplyDTO
import com.mincom.enterpriseservice.exception.*;

public class ParamsAABTST{
    //List of Input Parameters
    String paramType;

}

public class ProcessAABTST extends SuperBatch{
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 1;
    private ParamsAABTST batchParams;

    /*
     * do not touch these variables as they are standard and will be moved 
     * into a super class when ellipse Groovy supports.
     * 
     */


    public void runBatch(Binding b){

        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        batchParams = params.fill(new ParamsAABTST())
        // PrintRequest Parameters
        info("paramType: " + batchParams.paramType)

        try {
            processBatch();

        } finally {
            printBatchReport();
        }
    }

    private void processBatch(){
        info("processBatch");

//        how_to_use_edoi_search();
//        how_to_use_edoi_search_specify_column();
//        how_to_find_first_matching_record(); // a.k.a READ
        //how_to_create_record();
        //how_to_update_record();
        //how_to_delete_record();
//        how_to_create_file_in_work_dir();
          how_to_invoke_service();
//        how_to_invoke_screen_service();
//        how_to_invoke_eroi_aka_subroutine();
//        multipleServiceTest();
//        service_employee();
//        how_to_invoke_service_and_get_error();
        //write process
    }
    
    private service_employee(){
        info("service_employee")

        String empPrivInd
        List<EmployeeServiceRetrieveReplyDTO> empReplyList = new ArrayList<EmployeeServiceRetrieveReplyDTO>()
        EmployeeServiceRetrieveRequiredAttributesDTO empReqAtt = new EmployeeServiceRetrieveRequiredAttributesDTO()
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
        sdf.setLenient(false)

        empReqAtt.returnEmployee = empReqAtt.returnFirstName = true
        empReqAtt.returnPersonnelClass1 = empReqAtt.returnPersonnelClass2 = empReqAtt.returnPersonnelClass3 = true
        empReqAtt.returnPersonnelClass4 = empReqAtt.returnPersonnelClass5 = empReqAtt.returnPersonnelClass6 = true
        empReqAtt.returnPersonnelClass7 = empReqAtt.returnPersonnelClass8 = empReqAtt.returnPersonnelClass9 = true
        empReqAtt.returnServiceDate = empReqAtt.returnHireDate = empReqAtt.returnTerminationDate = true
        empReqAtt.returnPersonnelStatus = empReqAtt.returnPersEmpStatus = true
        try{
            def restart = ""
            boolean firstLoop = true
            while (firstLoop || (restart !=null && restart.trim().length() > 0)){
                EmployeeServiceRetrieveReplyCollectionDTO employeeReplyDTO = service.get("EMPLOYEE").retrieve({EmployeeServiceRetrieveRequestDTO it ->
                    it.requiredAttributes = empReqAtt
                    it.nameSearchMethod = "A"
                    it.personnelEmployeeInd = true
                    it.payrollEmployeeInd = true
                },100,  restart)
                firstLoop = false
                restart = employeeReplyDTO.getCollectionRestartPoint()
                empReplyList.addAll(employeeReplyDTO.getReplyElements())
            }

            String sHireDate
            String sServiceDate
            String sTerminateDate
            int count = 0
            for (EmployeeServiceRetrieveReplyDTO emp : empReplyList){
                count++
                sHireDate = sdf.format(emp.hireDate.getTime())
                sServiceDate = sdf.format(emp.serviceDate.getTime())
                sTerminateDate = sdf.format(emp.terminationDate.getTime())
                info("employee Id : ${emp.employee}")
                info("employee name : ${emp.getFirstName()}")
                info("employee status : ${emp.getPersEmpStatus()}")
                info("employee status : ${emp.getPersonnelStatus()}")
                info("hire date : ${sHireDate}")
                info("hire date1: ${emp.hireDate.time}")
                info("hire date1: ${emp.hireDate.getTime()}")
                info("service date : ${sServiceDate}")
                info("termination date : ${sTerminateDate}")
            }

            info("total emp: ${count}")

        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Error when retrieve Employee Service ${e.getMessage()}")
        }
    }

    //additional method - start from here.
    private void how_to_use_edoi_search(){

        info ("how_to_use_edoi_search")
        MSF010Key msf010key = new MSF010Key();

        // run the groovy with parameters table_type
        Constraint c1 = MSF010Key.tableType.equalTo(batchParams.paramType);
        def query = new QueryImpl(MSF010Rec.class).and(c1);
        def sTableCode
        def sTableDesc

        MSF010Key msf010keyDisp = new MSF010Key();
        edoi.search(query,{MSF010Rec msf010rec ->

            msf010keyDisp = msf010rec.getPrimaryKey()

            sTableCode = "TableCode:" + msf010keyDisp.getTableCode();
            sTableDesc = "TableDesc:" + msf010rec.getTableDesc();

            info(sTableCode);
            info(sTableDesc);

            info ("-----------------------------")

        })
    }

    private void how_to_use_edoi_search_specify_column(){

        info ("how_to_use_edoi_search_specify_column")

        MSF010Key msf010key = new MSF010Key();

        Constraint c1 = MSF010Key.tableType.equalTo(batchParams.paramType);
        def query = new QueryImpl(MSF010Rec.class).columns([MSF010Key.tableCode]).and(c1);
        def sTableCode
        def sTableDesc

        edoi.search(query,{rec ->
            info("This is the result: " +rec)})
        info ("-----------------------------")
    }

    private void how_to_find_first_matching_record(){

        info ("how_to_find_first_matching_record")

        MSF010Key msf010key = new MSF010Key();

        Constraint c1 = MSF010Key.tableType.equalTo(batchParams.paramType);
        def query = new QueryImpl(MSF010Rec.class).columns([MSF010Key.tableCode]).and(c1);
        def sTableCode;
        def sTableDesc;

        String sResultTableCode;
        sResultTableCode = edoi.firstRow(query);
        info("This is the result of read: " + sResultTableCode);
        info ("-----------------------------")
    }

    private void how_to_create_record(){
        info ("how_to_create_record");

        /*
         * for development purposes the transaction boundary is set to rollback.
         * if you need to set the commit flag if you want to see the result in the database.
         * this only applies in development mode, it does not apply when running via Ellipse.
         * */
        MSF010Rec msf010recb = new MSF010Rec()
        MSF010Key msf010keyb = new MSF010Key()
        msf010keyb.setTableType("MT")
        msf010keyb.setTableCode("DC")
        msf010recb.setPrimaryKey(msf010keyb)
        msf010recb.setTableDesc("DC Test")
        msf010recb.setAssocRec(" ")
        msf010recb.setActiveFlag(" ")

        edoi.create(msf010recb);
        info ("record created")

        //Below code are to check the result as the default run is to automatically roll back any database update
        Constraint c1 = MSF010Key.tableType.equalTo("MT");
        Constraint c2 = MSF010Key.tableCode.equalTo("DC");
        def query = new QueryImpl(MSF010Rec.class).columns([MSF010Rec.tableDesc]).and(c1).and(c2);

        String sResultTableDesc
        sResultTableDesc = edoi.firstRow(query);
        info("This is the result of creating record : " + sResultTableDesc);
        info ("-----------------------------")


    }

    private void how_to_update_record(){
        info ("how_to_update_record");
        MSF010Rec msf010recb = new MSF010Rec()
        MSF010Key msf010keyb = new MSF010Key()
        msf010keyb.setTableType("MT")
        msf010keyb.setTableCode("DC")
        msf010recb.setPrimaryKey(msf010keyb)
        msf010recb.setTableDesc("DC Testttttttttttttttt")
        msf010recb.setAssocRec(" ")
        msf010recb.setActiveFlag(" ")

        edoi.update(msf010recb);
        info ("record updated")

        //Below code are to check the result as the default run is to automatically roll back any database update
        Constraint c1 = MSF010Key.tableType.equalTo("MT");
        Constraint c2 = MSF010Key.tableCode.equalTo("DC");
        def query = new QueryImpl(MSF010Rec.class).columns([MSF010Rec.tableDesc]).and(c1).and(c2);

        String sResultTableDesc
        sResultTableDesc = edoi.firstRow(query);
        info("This is the result of updating record : " + sResultTableDesc);
        info ("-----------------------------")


    }

    private void how_to_delete_record(){

        info ("how_to_delete_record");
        MSF010Rec msf010recb = new MSF010Rec()
        MSF010Key msf010keyb = new MSF010Key()
        msf010keyb.setTableType("MT")
        msf010keyb.setTableCode("DC")
        msf010recb.setPrimaryKey(msf010keyb)
        edoi.delete(msf010keyb)
        info ("-----------------------------")
    }

    private void how_to_create_file_in_work_dir(){
        info("how_to_create_file_in_work_dir")
        info (env.getWorkDir().toString())
        FileWriter fstream = new FileWriter(env.getWorkDir().toString()+"/TRTEIS.csv");
        BufferedWriter ReportC = new BufferedWriter(fstream);
        ReportC.write("THIS IS TESTE ONLYYY")
        ReportC.close()
        info ("-----------------------------");

    }

    private void how_to_invoke_service(){
        info("how_to_invoke_service - This command will retrieve the first 20 recordss ")
        service.get('Table').retrieve({ it.tableType = '#MAI' }, 20).replyElements.each {
           try
           { 
              info ("${it.tableCode} ${it.description}")
           }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
               info("Error when retrieve table codes      ${e.getMessage()}")
           }
        }

        Object obj = service.get('Table')
        info ("-----------------------------");
    }

    private void how_to_invoke_screen_service(){
        info("how_to_invoke_service - screen service ")
        EllipseScreenService screenService = EllipseScreenServiceLocator.ellipseScreenService;
        ConnectionId msoCon = ConnectionHolder.connectionId;
        GenericMsoRecord screen = screenService.executeByName(msoCon, "MSO010");

        screen.setFieldValue("OPTION1I", "2");
        screen.setFieldValue("TABLE_TYPE1I", "ACTK");
        screen.setFieldValue("TABLE_CODE1I", "NOA");

        screen.nextAction = GenericMsoRecord.TRANSMIT_KEY;
        screen = screenService.execute(msoCon, screen);

        if (isErrorOrWarning(screen) ) {
            info ("Error type {}", (char)screen.errorType);
            throw new Exception("Error from msm010a submit" + screen.getErrorString());
        }

        if ( screen.mapname.trim().equals(new String("MSM010B")) ) {
            screen.setFieldValue("TABLE_DESC2I1", "No Action Allowed")
            screen.nextAction = GenericMsoRecord.TRANSMIT_KEY;
            screen = screenService.execute(msoCon, screen);
            info(screen.getMapname())
            if (isErrorOrWarning(screen) ) {
                info("Error type {}", (char)screen.errorType);
                throw new Exception("Error from mso submit msm010b " +  screen.getErrorString());
            }
        }

        info ("-----------------------------");
    }

    private boolean isErrorOrWarning(GenericMsoRecord screen) {

        return ((char)screen.errorType) == MsoErrorMessage.ERR_TYPE_ERROR || ((char)screen.errorType) == MsoErrorMessage.ERR_TYPE_WARNING;
    }

    private void how_to_invoke_eroi_aka_subroutine(){
        info("how_to_invoke_eroi_aka_subroutine")


        def mss080lnk = eroi.execute('MSS080', {mss080lnk ->
            mss080lnk.deferDate = new Date().format('yyyyMMdd')
            mss080lnk.deferTime = new Date().format('hhmmss')
            mss080lnk.requestDate = new Date().format('yyyyMMdd')
            mss080lnk.requestTime = new Date().format('hhmmss')
            mss080lnk.requestRecNo = '01'
            mss080lnk.noOfCopies1 = '01'
            mss080lnk.progReportId = 'A'
            mss080lnk.medium = 'R'
            mss080lnk.retentionDays9 = 5
            mss080lnk.startOption = 'N'
            mss080lnk.userId = commarea.UserId
            mss080lnk.requestDstrct = commarea.District
            mss080lnk.dstrctCode = commarea.District
            mss080lnk.requestBy = commarea.UserId
            mss080lnk.printer1 = ' '
            mss080lnk.progName = 'ACB9M1'
            mss080lnk.requestParams = 'NNNNACTK'})

        println "Return Status from mss080 is ${mss080lnk.returnStatus}"
    }

    private void multipleServiceTest(){
        info("multipleServiceTest")
        // Testing multiple service call
        String startEquipNo = "000000000001"
        int count = 0
        Constraint c1 = MSFX69Key.equipNo.greaterThan(startEquipNo)
        def query = new QueryImpl(MSFX69Rec.class).and(c1).orderBy(MSFX69Rec.msfx69Key)

        edoi.restartSearch(params.getRequestParameter() == null ? null : params.getRequestParameter().getLastProcessedRecord(), query,
                {info("Performing pre-processing here - Point A")},
                restart.each({MSFX69Rec rec ->
                    info("This is the result: " + rec.getPrimaryKey().getEquipNo())
                    count++
                    info("counter in: ${count}")
                    try{
                        EquipmentServiceReadRequiredAttributesDTO equipReqAtt = new EquipmentServiceReadRequiredAttributesDTO()
                        equipReqAtt.setReturnEquipmentNo(true)
                        equipReqAtt.setReturnEquipmentNoDescription1(true)
                        EquipmentServiceReadReplyDTO equipmentReadReply = service.get('Equipment').read({EquipmentServiceReadRequestDTO it ->
                            it.setRequiredAttributes(equipReqAtt)
                            it.equipmentNo = rec.getPrimaryKey().getEquipNo()
                        })

                        info ("Result from Screen Service : EquipNo: " + equipmentReadReply.getEquipmentNo() + " And Description : " + equipmentReadReply.getEquipmentNoDescription1())
//                        info ("Result next service call " + getInstallationPosition(equipmentReadReply.getEquipmentNo()))
                    }
                    catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
                        info("Cannot read record for Equipment - ${rec.getPrimaryKey().getEquipNo()}: ${e.getMessage()}")
                    }

                }))

        //        edoi.search(query,{MSFX69Rec rec ->
        //            info("This is the result: " +rec.getPrimaryKey().getEquipNo())
        //
        //            EquipmentServiceReadReplyDTO equipmentReadReply = service.get('Equipment').read({
        //                it.equipmentNo = rec.getPrimaryKey().getEquipNo()
        //            })
        //
        //            info ("Result from Screen Service : EquipNo: " + equipmentReadReply.getEquipmentNo() + "And Description : " + equipmentReadReply.getEquipmentNoDescription1())
        //            info ("Result next service call " + getInstallationPosition(equipmentReadReply.getEquipmentNo()))
        //
        //
        //        })

    }

    /*
     * This method only used by multipeServiceTest
     */
    private String getInstallationPosition(String equipNo) {
        info("getInstallationPosition ${equipNo}")
        String SERVICE_NAME = "EQUIPTRACE"
        String desig = " "
        try {

            // Limit the MAX_INSTANCE param to 1 since the service call returns the sorted list.

            EquipTraceServiceRetrieveFitEquipTracingReplyCollectionDTO fitEqDTO = service.get(SERVICE_NAME).retrieveFitEquipTracing(
                    { it.setFitEquipmentNo(equipNo)},
                    1)
            EquipTraceServiceRetrieveFitEquipTracingReplyDTO fitEq = fitEqDTO.getReplyElements()[0]
            if(fitEq != null) {
                desig = fitEq.getInstallEquipment() + fitEq.getModCode()
            }
        } catch(Exception e) {
            info("Error at read ${SERVICE_NAME}. ${e.getMessage()}")
        }
        return desig
    }


    private void how_to_invoke_service_and_get_error(){

        try{
            EquipmentServiceReadReplyDTO equipmentReadReply = service.get('Equipment').read({ it.equipmentNo = 'SSSSS' })
            info("Stef"+equipmentReadReply.getWarningsAndInformation())

        }catch(Exception e) {
            info("Error at . ${e.getMessage()}")
        }

        try{
            MarketPlaceServiceResult replyDto = service.get('MarketPlace').read({
                //MktName marketName = new MktName("XXX")
                //it.setName(marketName)
            })

        }catch (Exception f){
            info("Error at read ${f.getMessage()}")
            info(f.getStackTrace().toString())

        }

    }

    private void printBatchReport(){
        info("printBatchReport");
        //print batch report

        def Lni83za = report.open("LNI83Z");
        Lni83za.write("Abc");
        report
    }

}

/*runscript*/  
ProcessAABTST process = new ProcessAABTST();process.runBatch(binding);
