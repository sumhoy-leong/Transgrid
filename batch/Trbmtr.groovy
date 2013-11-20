package com.mincom.ellipse.script.custom

import groovy.lang.Binding

import java.io.Writer
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.ArrayList

import nacaLib.varEx.Var

import com.mincom.ellipse.client.connection.ConnectionHolder
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_ADKey
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_ADRec
import com.mincom.ellipse.edoi.ejb.msf010.*
import com.mincom.ellipse.edoi.ejb.msf600.*
import com.mincom.ellipse.edoi.ejb.msf620.MSF620Rec
import com.mincom.ellipse.edoi.ejb.msf900.*
import com.mincom.ellipse.edoi.ejb.msfx69.*
import com.mincom.ellipse.ejp.EllipseSessionDataContainer
import com.mincom.ellipse.ejp.area.CommAreaWrapper
import com.mincom.enterpriseservice.ellipse.ConnectionId
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveRequestDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.equiptrace.EquipTraceServiceRetrieveEquipProfilesReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.equiptrace.EquipTraceServiceRetrieveEquipProfilesReplyDTO
import com.mincom.enterpriseservice.ellipse.equiptrace.EquipTraceServiceRetrieveEquipProfilesRequestDTO
import com.mincom.enterpriseservice.ellipse.equiptrace.EquipTraceServiceRetrieveEquipProfilesRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.table.*
import com.mincom.eql.Constraint
import com.mincom.eql.common.Index
import com.mincom.eql.impl.QueryImpl

/**
 * Main Process of Trbmtr.
 */
public class ProcessTrbmtr extends SuperBatch {



    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 1
    private def reportA
    private Writer fileWriter

    private static final int MAX_ROW_READ = 1000
    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b) {
        init(b)
        printSuperBatchVersion()
        info("runBatch Version      : " + version)
        info("No request parameters.")
        try {
            processBatch()
        } finally {
            printBatchReport()
        }
    }

    /**
     * Process the main batch.
     */
    private void processBatch() {
        info("processBatch")
        //        browse000_AD()
        //        String[] equipNo = ["000000005556", "000000005557"]
        //        equipNo.each {
        //            retrieveEquipProfiles(it)
        //        }
        //        listFiles()
        //        sendEmail()
        //        initialize()
        createJournal()
        //        browseProdUnit()
        //        modifyTableDescription()
        //        browseEquipInstallPstn()
        //        browseEquipmentJoinInstPos()
        //        browseEquipmentJoinInstPosWithConstraint()
        //
        //        Index[] woIndex = [
        //            MSF620Rec.aix1,
        //            MSF620Rec.aix2,
        //            MSF620Rec.aix3,
        //            MSF620Rec.aix4,
        //            MSF620Rec.aix5
        //        ]
        //        for(Index i: woIndex) {
        //            browseWorkOrder(i)
        //        }
        //        printCommAreaInformation()
        //        browseEquipment()
        //        browseEquipment1()
        //        browseEquipment2()
    }
    private initialize() {
        info("initialize()")
    }

    private List<TableServiceRetrieveReplyDTO> browseTable(String tableType) {
        info("browseTable ${tableType}")

        TableServiceRetrieveRequestDTO retDTO = new TableServiceRetrieveRequestDTO()
        retDTO.setTableType(tableType)

        TableServiceRetrieveRequiredAttributesDTO reqAttr = new TableServiceRetrieveRequiredAttributesDTO()
        reqAttr.setReturnTableType(true)
        reqAttr.setReturnTypeDescription(true)
        reqAttr.setReturnTableCode(true)
        reqAttr.setReturnDescription(true)
        reqAttr.setReturnCodeDescription(true)
        reqAttr.setReturnAssociatedRecord(true)

        List<TableServiceRetrieveReplyDTO> tables = new ArrayList<TableServiceRetrieveReplyDTO>()

        def restart = ""
        boolean firstLoop = true

        while (firstLoop || restart?.trim()){
            TableServiceRetrieveReplyCollectionDTO replyColl = retrieveTable(retDTO, reqAttr, restart)
            firstLoop = false
            if(replyColl) {
                tables.addAll(replyColl.getReplyElements())
                info("tables  ${tables?.size()}")
                restart = replyColl.getCollectionRestartPoint()
                info("restart ${restart}")
            }
        }

        return tables
    }

    private TableServiceRetrieveReplyCollectionDTO retrieveTable(
    TableServiceRetrieveRequestDTO retDTO,
    TableServiceRetrieveRequiredAttributesDTO reqAttr,
    def restart) {
        info("retrieveTable")
        return service.get("TABLE").retrieve(retDTO, reqAttr, 20, false, restart)
    }

    private void testReportEdoi() {
        info("testReportEdoi")

        List<MSF010Rec> tables = new ArrayList<MSF010Rec>()
        tables.addAll(browseTableWithEdoi("ER"))

        reportA = report.open("TRBMTRA")
        StringBuilder headerString = new StringBuilder()
        headerString.append("ROWNUM".padRight(6))
        headerString.append("  ")
        headerString.append("TABLE CODE".padRight(18))
        headerString.append("  ")
        headerString.append("DESCRIPTION".padRight(50))
        headerString.append("  ")
        headerString.append("ASSOC REC".padRight(50))

        reportA.writeLine(132, "-")
        reportA.write(headerString.toString())
        reportA.writeLine(132, "-")
        reportA.write(" ")

        //CSV Report
        String fileName = "${env.workDir}/TRBMTRA"
        String uuid = getTaskUUID()
        if(uuid?.trim()){
            fileName = fileName + "." + uuid
        }
        fileName = fileName + ".csv"
        File outputFile = new File(fileName)
        if(!outputFile.exists()) {
            outputFile.createNewFile()
        }
        fileWriter = new FileWriter(outputFile)

        StringBuilder headerCsvString = new StringBuilder()
        headerCsvString.append("ROWNUM")
        headerCsvString.append(",")
        headerCsvString.append("TABLE CODE")
        headerCsvString.append(",")
        headerCsvString.append("DESCRIPTION")
        headerCsvString.append(",")
        headerCsvString.append("ASSOC REC")
        fileWriter.write(headerCsvString.toString())
        fileWriter.write("\n")

        info("writeReport")
        int lineCounter = 0
        for(MSF010Rec table : tables) {
            lineCounter++
            StringBuilder tableString = new StringBuilder()
            tableString.append(String.valueOf(lineCounter).padLeft(6))
            tableString.append("  ")
            tableString.append(table?.getPrimaryKey().getTableCode().padRight(18))
            tableString.append("  ")
            tableString.append(table?.getTableDesc().padRight(50))
            tableString.append("  ")
            tableString.append(table?.getAssocRec().padRight(50))
            reportA.write(tableString.toString())

            StringBuilder csvTableString = new StringBuilder()
            csvTableString.append(String.valueOf(lineCounter).padLeft(6))
            csvTableString.append(",")
            csvTableString.append(table?.getPrimaryKey().getTableCode().padRight(18))
            csvTableString.append(",")
            csvTableString.append(table?.getTableDesc().padRight(50))
            csvTableString.append(",")
            csvTableString.append(table?.getAssocRec().padRight(50))
            fileWriter.write(csvTableString.toString())
            fileWriter.write("\n")
        }

        reportA.close()
        fileWriter.close()
    }

    private browseTableWithEdoi(String tableType) {
        info("browseTableWithEdoi")
        QueryImpl qTable = new QueryImpl(MSF010Rec.class).
                and(MSF010Key.tableType.equalTo(tableType))

        return edoi.search(qTable).getResults()
    }

    private void testReport() {
        info("testReport")

        List<TableServiceRetrieveReplyDTO> tables = new ArrayList<TableServiceRetrieveReplyDTO>()
        tables.addAll(browseTable("NSWP"))
        tables.addAll(browseTable("NSWA"))
        tables.addAll(browseTable("+AST"))
        tables.addAll(browseTable("+CST"))

        reportA = report.open("TRBMTRA")
        StringBuilder headerString = new StringBuilder()
        headerString.append("ROWNUM".padRight(6))
        headerString.append("  ")
        headerString.append("TABLE CODE".padRight(18))
        headerString.append("  ")
        headerString.append("DESCRIPTION".padRight(50))
        headerString.append("  ")
        headerString.append("ASSOC REC".padRight(50))

        reportA.writeLine(132, "-")
        reportA.write(headerString.toString())
        reportA.writeLine(132, "-")
        reportA.write(" ")

        info("writeReport")
        int lineCounter = 0
        for(TableServiceRetrieveReplyDTO table : tables) {
            lineCounter++
            StringBuilder tableString = new StringBuilder()
            tableString.append(String.valueOf(lineCounter).padLeft(6))
            tableString.append("  ")
            tableString.append(table?.getTableCode().padRight(18))
            tableString.append("  ")
            tableString.append(table?.getDescription().padRight(50))
            tableString.append("  ")
            tableString.append(table?.getAssociatedRecord().padRight(50))
            reportA.write(tableString.toString())
        }
        reportA.close()
    }

    private void browseEquipment() {
        info("browseEquipment()")
        //List of unique Equipment Number
        private ArrayList<String> equipNoDistinctList = new ArrayList<String>()

        try {
            //Use edoi since there is no service call for Equipment Installation Positions
            QueryImpl qEquip = new QueryImpl(MSFX69Rec.class)
                    .and(MSFX69Key.equipNo.greaterThan(" "))
                    .and(MSFX69Key.installPosn.greaterThanEqualTo(" "))
                    .and(MSFX69Key.equipNo.equalTo(MSF600Key.equipNo))

            int counterEq = 0
            int counterIp = 0
            edoi.search(qEquip) {
                MSFX69Rec msfx69Rec = it[0]
                MSF600Rec msf600Rec = it[1]
                String equipNo = msfx69Rec.getPrimaryKey().getEquipNo()

                if(!equipNoDistinctList.contains(equipNo) && msf600Rec != null
                && msf600Rec.getTraceableFlg()?.trim().equalsIgnoreCase('y')) {
                    equipNoDistinctList.add(equipNo)
                    counterEq++
                    info("${counterEq} - Equipment No ${equipNo}")
                    if("TG000988".equals(equipNo) || "TG000989".equals(equipNo)) {
                        info("BINGGO!")
                    }
                }
                counterIp++
            }

            info("Browse MSFX69 ${counterIp}")
        } catch(Exception e) {
            info("Browse Equipment error ${e}")
        }
    }

    private void browseEquipment1() {
        info("browseEquipment1()")
        //List of unique Equipment Number
        private ArrayList<String> equipNoDistinctList = new ArrayList<String>()

        try {
            //Use edoi since there is no service call for Equipment Installation Positions
            QueryImpl qEquip = new QueryImpl(MSF600Rec.class)
                    .and(MSF600Key.equipNo.equalTo(MSFX69Key.equipNo))
                    .and(MSFX69Key.equipNo.greaterThanEqualTo(" "))
                    .and(MSFX69Key.installPosn.greaterThanEqualTo(" "))

            int counterEq = 0
            int counterIp = 0
            edoi.search(qEquip) {
                MSFX69Rec msfx69Rec = it[1]
                MSF600Rec msf600Rec = it[0]
                String equipNo = msfx69Rec.getPrimaryKey().getEquipNo()

                if(!equipNoDistinctList.contains(equipNo) && msf600Rec != null
                && msf600Rec.getTraceableFlg()?.trim().equalsIgnoreCase('y')) {
                    equipNoDistinctList.add(equipNo)
                    counterEq++
                    info("${counterEq} - Equipment No ${equipNo}")
                    if("TG000988".equals(equipNo) || "TG000989".equals(equipNo)) {
                        info("BINGGO!")
                    }
                }
                counterIp++
            }

            info("Browse MSFX69 ${counterIp}")
        } catch(Exception e) {
            info("Browse Equipment 1 error ${e}")
        }
    }

    private void browseEquipment2() {
        info("browseEquipment2()")
        //List of unique Equipment Number
        private ArrayList<String> equipNoDistinctList = new ArrayList<String>()

        try {
            //Use edoi since there is no service call for Equipment Installation Positions
            QueryImpl qEquip = new QueryImpl(MSFX69Rec.class)
                    .and(MSFX69Key.equipNo.greaterThan(" "))
                    .and(MSFX69Key.installPosn.greaterThanEqualTo(" "))

            int counterEq = 0
            int counterIp = 0
            edoi.search(qEquip) { MSFX69Rec msfx69Rec->
                String equipNo = msfx69Rec.getPrimaryKey().getEquipNo()
                MSF600Rec msf600Rec = readEquipment(equipNo)

                if(!equipNoDistinctList.contains(equipNo) && msf600Rec != null
                && msf600Rec.getTraceableFlg()?.trim().equalsIgnoreCase('y')) {
                    equipNoDistinctList.add(equipNo)
                    counterEq++
                    info("${counterEq} - Equipment No ${equipNo}")
                    if("TG000988".equals(equipNo) || "TG000989".equals(equipNo)) {
                        info("BINGGO!")
                    }
                }
                counterIp++
            }
            info("Browse MSFX69 ${counterIp}")
        } catch(Exception e) {
            info("Browse Equipment 2 error ${e}")
        }
    }

    private MSF600Rec readEquipment(String equipNo) {
        info("readEquipment")
        try {
            return edoi.findByPrimaryKey(new MSF600Key(equipNo))
        }catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
            info("MSF600Rec ${equipNo} not found!")
            return null
        }
    }

    private void printCommAreaInformation() {
        info("printCommAreaInformation")
        Field f = commarea.getClass().getDeclaredField("data"); //NoSuchFieldException
        f.setAccessible(true);
        EllipseSessionDataContainer data = (EllipseSessionDataContainer) f.get(commarea); //IllegalAccessException
        ConnectionId id = ConnectionHolder.getConnectionId();
        char[] area = data.getEllipseSessionData(id).getCOMMAREA();
        CommAreaWrapper wrapper = new CommAreaWrapper(area);

        info("${wrapper.commarea}")
        reportA = report.open("TRBMTRA")
        Class iClazz = wrapper.commarea.getClass()
        Field[] fields = iClazz.getDeclaredFields()
        for(int i = 0; i < fields.length;i++) {
            String fName = fields[i].getName()
            Field field = iClazz.getDeclaredField(fName)
            field.setAccessible(true)
            def fieldGet = field.get(wrapper.commarea)
            String val
            if(fieldGet instanceof Var) {
                val = ((Var)fieldGet).getString()
            } else {
                val = fieldGet.toString()
            }
            StringBuilder reportLine = new StringBuilder()
            reportLine.append(fName.padRight(30))
            reportLine.append(" --> ")
            reportLine.append(val)
            reportA.write(reportLine.toString())
            info("${fName} - ${val}")
        }

        reportA.close()
    }

    private void constructCSVwithLineBreak() {
        info("constructCSVwithLineBreak")
        def workingDir  = env.workDir
        String csvPath  = "${workingDir}/TRBMTR"
        if(taskUUID?.trim()) {
            csvPath = csvPath + "." + taskUUID
        }
        csvPath = csvPath + ".csv"
        File csvFile = new File(csvPath)

        BufferedWriter csvWriter = new BufferedWriter(new FileWriter(csvFile))

        String[] lines = [
            "Line 1",
            "\r\n",
            "Line 2",
            "\n\r",
            "Line 3",
            System.getProperty("line.separator")
        ]

        lines.each { csvWriter.write(it) }
        csvWriter.write("Line 4")
        csvWriter.newLine()
        csvWriter.write("Line 5")
        csvWriter.write("&#13;&#10")
        csvWriter.write("Line 6")

        csvWriter.close()
        info("Adding CSV into Request.")
        if (taskUUID?.trim()) {
            request.request.CURRENT.get().addOutput(csvFile,
                    "text/comma-separated-values", "TRBMTR");
        }
    }

    private void browseEquipInstallPstn() {
        info("browseEquipInstallPstn")
        long eqRead = 0
        QueryImpl qIstllnPstn = new QueryImpl(MSFX69Rec.class).orderBy(MSFX69Rec.msfx69Key)
        edoi.search(qIstllnPstn, MAX_ROW_READ,  {MSFX69Rec msfx69Rec-> eqRead++ })
        info("browseEquipInstallPstn, returns ${eqRead} rows.")
    }

    private void browseEquipmentJoinInstPos() {
        info("browseEquipmentJoinInstPos")
        long eqRead = 0
        QueryImpl qEquip = new QueryImpl(MSFX69Rec.class)
                .and(MSFX69Key.equipNo.greaterThan(" "))
                .and(MSFX69Key.installPosn.greaterThanEqualTo(" "))
                .and(MSFX69Key.equipNo.equalTo(MSF600Key.equipNo))
                .orderBy(MSFX69Rec.msfx69Key)
        edoi.search(qEquip, MAX_ROW_READ,  {it-> eqRead++ })
        info("browseEquipmentJoinInstPos, returns ${eqRead} rows.")
    }

    private void browseEquipmentJoinInstPosWithConstraint() {
        info("browseEquipmentJoinInstPosWithConstraint")
        long eqRead = 0
        QueryImpl qEquip = new QueryImpl(MSFX69Rec.class)
                .and(MSFX69Key.equipNo.greaterThan(" "))
                .and(MSFX69Key.installPosn.greaterThanEqualTo(" "))
                .and(MSFX69Key.equipNo.equalTo(MSF600Key.equipNo))
                .and(MSF600Rec.equipStatus.equalTo("IS"))
                .and(MSF600Rec.compCode.equalTo("CBR-"))
                .orderBy(MSFX69Rec.msfx69Key)
        edoi.search(qEquip, MAX_ROW_READ,  {it-> eqRead++ })
        info("browseEquipmentJoinInstPosWithConstraint, returns ${eqRead} rows.")
    }

    private long browseWorkOrder(Index i) {
        info("browseWorkOrder")
        QueryImpl qWO = new QueryImpl(MSF620Rec.class)
                .orderBy(i)
        long counter = 0
        edoi.search(qWO, ProcessTrbmtr.MAX_ROW_READ) { MSF620Rec rec->
            info("${rec.getPrimaryKey().getWorkOrder()}")
            counter++
        }
        return counter
    }

    private void modifyTableDescription() {
        info("modifyTableDescription")
        ScreenAppLibrary sal = new ScreenAppLibrary()
        TableDTO tabDTO = new TableDTO()
        tabDTO.tableType = "+EML"
        tabDTO.tableCode = "TRE"
        //stuart.james@transgrid.com.au
        tabDTO.description = "tommy.limantono@transgrid.com.au"
        TableResultDTO resultDTO = sal.updateTableDescription(tabDTO)
        if(resultDTO.error) {
            info("Error: ${resultDTO.error.errorCode} - ${resultDTO.error.errorMsg}")
            info("At   : ${resultDTO.error.currentCursorField} - ${resultDTO.error.currentCursorValue}")
        } else {
            info("Table updated.")
        }
    }

    /**
     * Print the batch report.
     */
    private void printBatchReport() {
        info("printBatchReport")
    }

    private void browseProdUnit() {
        info("browseProdUnit")
        String[] prodUnits = [
            //            "CMS",
            //            "CMSSE1",
            //            "CMMSYN",
            //"CMT1020",
            "CMS",
        ]

        ArrayList<String> listEqNo = new ArrayList<String>()
        prodUnits.each {
            EquipmentServiceRetrieveReplyDTO prodUnitDto = readProductiveUnit(it)
            info("Prod Unit ${it} - ${prodUnitDto?.getEquipmentNo()}")

            listEqNo.addAll(browseFittedEquipments(prodUnitDto?.getEquipmentNo()?.trim()))
        }

        listEqNo.each { info("Equip No ${it}") }
    }

    private EquipmentServiceRetrieveReplyDTO readProductiveUnit(String prodUnit) {
        info("readProductiveUnit")

        try{
            EquipmentServiceRetrieveRequestDTO equipRetDto =
                    new EquipmentServiceRetrieveRequestDTO()
            equipRetDto.setEquipmentRefSearchMethod("E")
            equipRetDto.setEquipmentRef(prodUnit)
            equipRetDto.setDistrictCode("GRID")
            equipRetDto.setProdUnitFlag(true)
            equipRetDto.setAssocEquipmentItemsExcl(false)
            equipRetDto.setExclInStore(false)
            equipRetDto.setExclScrapSold(false)

            EquipmentServiceRetrieveRequiredAttributesDTO equipReqAtt =
                    new EquipmentServiceRetrieveRequiredAttributesDTO()
            equipReqAtt.setReturnEquipmentNo(true)
            equipReqAtt.setReturnEquipmentRef(true)
            equipReqAtt.setReturnParentEquipment(true)
            equipReqAtt.setReturnParentEquipmentRef(true)
            equipReqAtt.setReturnEquipmentGrpId(true)
            equipReqAtt.setReturnEquipmentStatus(true)
            equipReqAtt.setReturnEquipmentClassif0(true)
            equipReqAtt.setReturnEquipmentClassif1(true)

            EquipmentServiceRetrieveReplyCollectionDTO equipReplyDto =
                    service.get("EQUIPMENT").retrieve(equipRetDto, equipReqAtt, 1, false)

            return equipReplyDto?.getReplyElements()[0]
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            info("Cannot Retrieve equipment : ${e.getMessage()}")
            String errorMsg   = e.getErrorMessages()[0].getMessage()
            String errorCode  = e.getErrorMessages()[0].getCode().replace("mims.e.","")
            String errorField = e.getErrorMessages()[0].getFieldName()
            info("Error during execute EQUIPMENT-retrieve caused by ${errorField}: ${errorCode} - ${errorMsg}.")
        }
        return null
    }

    private ArrayList<String> browseFittedEquipments(String parentEquipNo) {
        info("browseFittedEquipments")
        ArrayList<String> fitEquip = new ArrayList<String>()
        QueryImpl query = new QueryImpl(MSF600Rec.class)
                .and(MSF600Key.equipNo.greaterThanEqualTo(" "))
                .and(MSF600Rec.parentEquip.equalTo(parentEquipNo))
                .columns([
                    MSF600Key.equipNo
                ])
                .orderBy(MSF600Rec.msf600Key)

        List results = edoi.search(query, MAX_ROW_READ).getResults()
        if(!results.isEmpty()) {
            results.each {
                fitEquip.add(it)
                return fitEquip.addAll(browseFittedEquipments(it))
            }
        }
        return fitEquip
    }

    private void createJournal() {
        info("createJournal")
        JournalEntryDTO dto1 = new JournalEntryDTO()
        dto1.accountCode = ".245"
        dto1.ammount = "0.00"
        dto1.woProjectNo = "60685"
        dto1.woProjectIndicator = "W"
        JournalEntryDTO dto2 = new JournalEntryDTO()
        dto2.accountCode = "794100161245"
        dto2.ammount = "0.00"
        dto2.woProjectNo = " "
        dto2.woProjectIndicator = " "
        ArrayList<JournalEntryDTO> entries = new ArrayList<JournalEntryDTO>()
        entries.add(dto1)
        entries.add(dto2)

        JournalDTO journal = new JournalDTO()
        journal.description = "All WO 0000085772 471 21/04/13"
        journal.accountant = "0000016813"
        journal.approvalStatus = "Y"
        journal.tranDate = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime())
        journal.entries = entries

        ScreenAppLibrary sl = new ScreenAppLibrary()
        JournalResultDTO reply = sl.createJournal(journal)
        if(reply.error != null) {
            info("ERROR : Cannot create Journal - Field ${reply.error.currentCursorField} : ${reply.error.currentCursorValue} - ${reply.error.errorCode} ${reply.error.errorMsg}.")
        } else {
            String journalNo = reply.journalNo?.trim()
            info("created journalNo : ${journalNo}")
            if(journalNo) {
                browseManualJournalTransaction(journalNo)
            }
        }
    }

    /**
     * Browse transaction based on manual journal voucher number.
     * @param journalNo manual journal voucher number
     */
    private void browseManualJournalTransaction(String journalNo) {
        info("browseManualJournalTransaction ${journalNo}")
        QueryImpl qTransaction = new QueryImpl(MSF900Rec.class)
                .and(MSF900Key.rec900Type.equalTo("M"))
                .and(MSF900Key.dstrctCode.equalTo("GRID"))
                .and(MSF900Key.processDate.equalTo("20130429"))
                .and(MSF900Rec.tranType.equalTo("MPJ"))
                .and(MSF900Rec.manjnlVchr.equalTo(journalNo))
                .columns([
                    MSF900Key.transactionNo,
                    MSF900Rec.accountCode,
                    MSF900Rec.tranAmount,
                    MSF900Rec.workOrder
                ])
        edoi.search(qTransaction, {columns->
            String transactionNo = columns[0]
            String accountCode   = columns[1]
            BigDecimal trnAmnt   = columns[2]
            String workOrder     = columns[3]
            info("Transaction ${transactionNo} - ${accountCode} - ${trnAmnt} - ${workOrder}")
        })
    }

    void sendEmail() {
        info("sendEmail")
        String subject = "Email ."
        String mailTo  = "AntasenaWahyu.Anggarajati@mitrais.com; antasenawahyu@gmail.com"
        ArrayList message = new ArrayList()
        message.add("Test sending email .")
        message.add("-------------------------------------------------------------------------")

        SendEmail myEmail = new SendEmail(subject, mailTo, message, null, " ")
        myEmail.host = "exchange2007.mitrais.com"
        myEmail.sendMail()

        if(myEmail.isError()) {
            info ("*** Cannot Send Email to ${mailTo} because ${myEmail.getErrorMessage()}. ***")
        }

        myEmail = new SendEmail(subject, " ", message, null, " ")
        myEmail.host = "exchange2007.mitrais.com"
        myEmail.sendMail()

        if(myEmail.isError()) {
            info ("*** Cannot Send Email to ${mailTo} because ${myEmail.getErrorMessage()}. ***")
        }

        //        info("sendEmail single attachment")
        //        subject = "Email with Single Attachment."
        //        message = new ArrayList()
        //        message.add("Test sending email with single attachment.")
        //        message.add("-------------------------------------------------------------------------")
        //        myEmail = new SendEmail(subject, mailTo, message, "D:\\temp\\myFile.txt")
        //        myEmail.host = "exchange2007.mitrais.com"
        //        myEmail.sendMail()
        //
        //        if(myEmail.isError()) {
        //            info ("*** Cannot Send Email to ${mailTo} because ${myEmail.getErrorMessage()}. ***")
        //        }
        //
        //        info("sendEmail multiple attachment")
        //        subject = "Email with Multiple Attachment."
        //        message = new ArrayList()
        //        message.add("Test sending email with multiple attachment.")
        //        message.add("-------------------------------------------------------------------------")
        //        myEmail = new SendEmail(subject, mailTo, message, [
        //            "D:\\temp\\myFile.txt",
        //            "D:\\temp\\myFile - Copy.txt"
        //        ])
        //        myEmail.host = "exchange2007.mitrais.com"
        //        myEmail.sendMail()
        //
        //        if(myEmail.isError()) {
        //            info ("*** Cannot Send Email to ${mailTo} because ${myEmail.getErrorMessage()}. ***")
        //        }
        //
        //        info("sendEmail multiple attachment with alias")
        //        subject = "Email with Multiple Attachment with Alias."
        //        message = new ArrayList()
        //        message.add("Test sending email with multiple attachment with alias.")
        //        message.add("-------------------------------------------------------------------------")
        //        myEmail = new SendEmail(subject, mailTo, message, [
        //            "D:\\temp\\myFile.txt|reportA",
        //            "D:\\temp\\myFile - Copy.txt|reportB"
        //        ])
        //        myEmail.host = "exchange2007.mitrais.com"
        //        myEmail.sendMail()
        //
        //        if(myEmail.isError()) {
        //            info ("*** Cannot Send Email to ${mailTo} because ${myEmail.getErrorMessage()}. ***")
        //        }
    }

    private void listFiles() {
        info("listFiles")
        //String baseDirectory = "/var/opt/mincom/customer-software/batch/src/com/mincom/ellipse/script/custom"
        String[] dirs = [
            "/var/opt/mincom/customer-software/batch/src/com/mincom/ellipse/script/custom",
            "/var/opt/mincom/customer-software/reporting/rdl/src",
            "/var/opt/mincom/customer-software/hooks/src"
        ]

        //CSV Report
        String fileName = "${env.workDir}/TRBMTRA"
        String uuid = getTaskUUID()
        if(uuid?.trim()){
            fileName = fileName + "." + uuid
        }
        fileName = fileName + ".csv"
        File outputFile = new File(fileName)
        if(!outputFile.exists()) {
            outputFile.createNewFile()
        }
        fileWriter = new FileWriter(outputFile)

        StringBuilder headerCsvString = new StringBuilder()
        headerCsvString.append("File Name")
        headerCsvString.append(",")
        headerCsvString.append("Mod Date")
        headerCsvString.append(",")
        headerCsvString.append("Type")
        fileWriter.write(headerCsvString.toString())
        fileWriter.write("\r\n")

        dirs.each {baseDirectory->
            String type
            File dir  = new File(baseDirectory)
            switch(baseDirectory) {
                case "/var/opt/mincom/customer-software/batch/src/com/mincom/ellipse/script/custom":
                    type = "Groovy Batch"
                    break
                case "/var/opt/mincom/customer-software/reporting/rdl/src":
                    type = "RDL"
                    break
                case "/var/opt/mincom/customer-software/hooks/src":
                    type = "Hooks"
                    break
            }
            if (dir.isDirectory() ) {
                dir.eachFileRecurse {f->
                    String lastModDate = new Date(f.lastModified()).format("dd/MM/yyyy")
                    StringBuilder fileString = new StringBuilder()
                    fileString.append(f.getName())
                    fileString.append(",")
                    fileString.append(lastModDate)
                    fileString.append(",")
                    fileString.append(type)

                    fileWriter.write(fileString.toString())
                    fileWriter.write("\r\n")
                }
            }}
        fileWriter.close()
    }

    /**
     * retrieve equipment Profile using equipment trace retrieve service
     * @param equipNo
     */
    private void retrieveEquipProfiles(String equipNo){
        info("retrieveEquipProfiles")
        try{
            EquipTraceServiceRetrieveEquipProfilesRequiredAttributesDTO equipProfilesReq =
                    new EquipTraceServiceRetrieveEquipProfilesRequiredAttributesDTO()
            equipProfilesReq.setReturnEquipmentNo(true)
            equipProfilesReq.setReturnInstallEquipment(true)
            equipProfilesReq.setReturnInstallEquipmentRef(true)
            equipProfilesReq.setReturnCompCode(true)
            equipProfilesReq.setReturnModCode(true)
            equipProfilesReq.setReturnFittedStatus(true)

            List<EquipTraceServiceRetrieveEquipProfilesReplyDTO> equipProfilesList =
                    new ArrayList<EquipTraceServiceRetrieveEquipProfilesReplyDTO>()
            String sRestart = ""
            boolean firstLoop = true
            int count = 0
            while(firstLoop || (sRestart != null && sRestart?.trim())){
                EquipTraceServiceRetrieveEquipProfilesReplyCollectionDTO equipProfilesDTO =
                        service.get("EQUIPTRACE").retrieveEquipProfiles(
                        {EquipTraceServiceRetrieveEquipProfilesRequestDTO it->
                            it.setEGIEquipmentInd("E")
                            it.setEquipmentNo(equipNo)
                        }, equipProfilesReq, 20,false,sRestart)
                firstLoop = false
                sRestart = equipProfilesDTO.getCollectionRestartPoint()
                equipProfilesList.addAll(equipProfilesDTO.getReplyElements())

                equipProfilesDTO.getReplyElements().each { EquipTraceServiceRetrieveEquipProfilesReplyDTO eqProf->
                    info("${eqProf.getEquipmentNo()}")
                    info("${eqProf.getEquipmentRef()}")
                }
            }
        }catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
            for(ErrorMessageDTO error : e.errorMessages){
                def fieldRef = error.getFieldName()
                def errValue = equipNo
                def errMsg   = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
                info("Cannot Retrieve equipment trace EQUIPTRACE : ${fieldRef} - ${errValue} - ${errMsg}")

            }
        }
    }

    private void browse000_AD() {
        info("browse000_AD")
        Constraint cDistrict   = MSF000_ADKey.dstrctCode.equalTo(" ")
        Constraint cCtrRecType = MSF000_ADKey.controlRecType.equalTo("AD")
        Constraint cDstrctStat = MSF000_ADRec.dstrctStatus.equalTo("A")

        def query = new QueryImpl(MSF000_ADRec.class)
                .and(cDistrict)
                .and(cCtrRecType)
                .and(cDstrctStat)
                .orderBy(MSF000_ADRec.msf000Key)
        edoi.search(query, 1000, { MSF000_ADRec msf000_ADRec->
            info("${msf000_ADRec.getDstrctStatus()}")
        })
    }
}

/**
 * Run the script
 */
ProcessTrbmtr process = new ProcessTrbmtr()
process.runBatch(binding)
