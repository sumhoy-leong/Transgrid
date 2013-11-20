/**
 * Script           : Trbsca
 * Title            : Support Cost Allocation Support Costs Allocation
 * Objective        :  
 * The SCA batch program will run immediately after the MSB857 batch program.
 * It will process MSF857 Labour Costing standard rate transactions that have been 
 * successfully processed by MSB857 indicated by the MSF857-POSTING-STATUS field.
 * It will need to differentiate between records that it may have already successfully processed in a previous run, 
 * so that any MSB857/SCA program re-runs do not create unwanted additional transactions. To achieve this, 
 * the current MSF857-BATCH-TYPE indicator will be set to .S. for all records successfully processed by this 
 * new SCA batch program.
 * Finally it will generate Journals Interface (MSF903) records as .BPJ. (Batch Journal Voucher Primary) transactions to 
 * allow MSB919 to then create the required Ellipse Journal Holding File records (MSF900).
 * The SCA batch program will produce reports listing both errors and successfully processed transactions.
 
 * Job Dependency                   
 * Predecessor      : MSB857
 * Successor        : MSB904, MSB919
 *
 * Data used
 * MSF856 - Labour Costing Oncost Rates 
 * MSF857 - Labour Costing Transactions 
 * MSF903 - Journals Interface
 * MSF620 - Labour Costing Oncost Rates
 * MSF010 - Table File
 * 
 *
 * Request
 * entry/ MSF081    : Required 
 *
 * Custom_batch.xml : N/A
 *
 * Usage : N/A
  *
 */

package com.mincom.ellipse.script.custom;

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Formatter
import java.math.BigDecimal
import java.io.BufferedWriter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import groovy.lang.Binding;

import com.mincom.ellipse.edoi.ejb.msf080.MSF080Key
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.eroi.linkage.mss080.MSS080LINK

import com.mincom.enterpriseservice.ellipse.batchrequest.BatchRequestServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.batchrequest.BatchRequestServiceCreateReplyDTO
import com.mincom.enterpriseservice.ellipse.batchrequest.BatchRequestServiceCreateRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.batchrequest.BatchRequestServiceCreateReplyCollectionDTO

import com.mincom.enterpriseservice.ellipse.accountupdate.AccountUpdateServiceCreateRequestDTO
import com.mincom.enterpriseservice.ellipse.accountupdate.AccountUpdateServiceCreateReplyDTO
import com.mincom.enterpriseservice.ellipse.accountupdate.AccountUpdateServiceCreateRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.accountupdate.AccountUpdateServiceCreateReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.dependant.dto.TransDetailsDTO
import com.mincom.enterpriseservice.ellipse.labourcostweek.LabourCostWeekServiceRetrieveRequestDTO
import com.mincom.enterpriseservice.ellipse.labourcostweek.LabourCostWeekServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.labourcostweek.LabourCostWeekServiceRetrieveRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.labourcostweek.LabourCostWeekServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveRequestDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveRequestDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequestDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequiredAttributesDTO
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyCollectionDTO
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException

import com.mincom.ellipse.edoi.ejb.msf620.MSF620Key
import com.mincom.ellipse.edoi.ejb.msf620.MSF620Rec
import com.mincom.ellipse.edoi.ejb.msf856.MSF856Key
import com.mincom.ellipse.edoi.ejb.msf856.MSF856Rec
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Key
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Rec
import com.mincom.ellipse.edoi.ejb.msf903.MSF903Key
import com.mincom.ellipse.edoi.ejb.msf903.MSF903Rec
import com.mincom.ellipse.edoi.ejb.msf903.MSF903_M_903Key
import com.mincom.ellipse.edoi.ejb.msf903.MSF903_M_903Rec
import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException
import com.mincom.ellipse.edoi.common.exception.EDOIDuplicateKeyException


public class ParamsTrbsca {
	//List of Input Parameters
	String paramSca;
}
	

public class ProcessTrbsca extends SuperBatch {

    private final version = 1
    private ParamsTrbsca batchParams
    def employees = [] 
    def tablecodes = []
    File mst903 
    public def dac1, dac2, dac3
    public def ocr = 1
    BigDecimal oncostval = 0
    def tmp
    def Trbscaa, Trbscab, Trbscac

    public void runBatch(Binding b){
	info("runBatch Version : " + version)
		
	init(b)

	/* 
	*  Populates input from TestCase.groovy script
	*/
	batchParams = params.fill(new ParamsTrbsca())
	if (request.getProperty("Parameters")== null) { 
		info("TestCase.groovy or runCustom")
	} else {
	    p = request.getProperty("Parameters").tokenize(",")
	}

	processBatch()
		
    }


    /**
      * Execute the concatenation
      * @param parameters
     */
    private void processBatch() {
	info("processBatch")
	
	tablecodes = tableService()
	ioReports('open')
	try {
 	    mst903 = new File(env.getWorkDir(), 'MST903.'+this.getUUID() )
	} catch (IOException e) {info("ERROR : ${e}") }
	
	printHeaders()
	loopLabourCostWeek()
	ioReports('close')  

	createNextBatch('MSB904') 		

    }

    private void createNextBatch(def job) {

        Calendar cal = Calendar.getInstance();
        String todayDate = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
        String todayTime = new SimpleDateFormat("HHmmss").format(cal.getTime());

		Constraint c1 = MSF080Key.progName.equalTo(request.request.getRequestName())
		Constraint c2 = MSF080Rec.taskUuid.equalTo(this.getUUID())
		def query = new QueryImpl(MSF080Rec.class).and(c1).and(c2)
		MSF080Rec rec = edoi.firstRow(query)

	//Call MSS080 to create request
    	MSS080LINK mss080lnk = eroi.execute("MSS080",{MSS080LINK mss080lnk->
    	    mss080lnk.requestDate = todayDate
            mss080lnk.requestTime = todayTime
            mss080lnk.deferDate = todayDate
            mss080lnk.deferTime = todayTime
            mss080lnk.progName = job  
            mss080lnk.requestRecNo = rec.getPrimaryKey().getRequestRecNo() 
            mss080lnk.requestNo = rec.getRequestNo()
            mss080lnk.userId = rec.getUserId()
            mss080lnk.requestBy = rec.getRequestBy()
            mss080lnk.dstrctCode = rec.getDstrctCode()
            mss080lnk.requestDstrct = rec.getRequestDstrct() 
            mss080lnk.progReportId = rec.getProgReportId()
            mss080lnk.medium = rec.getMedium()
            mss080lnk.jobId = rec.getJobId() 
            mss080lnk.distribCode = rec.getDistribCode()
            mss080lnk.retentionDays = rec.getRetentionDays()
            mss080lnk.traceFlg = rec.getTraceFlg()
            mss080lnk.pubType = rec.getPubType()
            mss080lnk.languageCode = rec.getLanguageCode()
            mss080lnk.taskUuid = request.request.getParent().getTaskUuid() 
            mss080lnk.uuid = this.getUUID() 
            mss080lnk.requestParams = rec.getRequestParams()
            mss080lnk.startOption = rec.getStartOption()
            mss080lnk.copyRqstSw = "N"
            mss080lnk.printerRec = (rec.getPrinter1()?:"").padRight(2) + (rec.getNoOfCopies1()?:"").padLeft(2, "0")

        })

        if (!mss080lnk.getReturnStatus().equals("Y")) {
            info("MSS080 status : " + mss080lnk.getReturnStatus() + mss080lnk.getErrorData().toString() )
        }else {
            info("MSS080 UUID : " + mss080lnk.getTaskUuid() ) 
        }

    }

    private void ioReports(def io) {
	info("ioReports")
	
	    switch (io) {
	    case 'open':
		    Trbscaa = report.open("TRBSCAA")
		    Trbscab = report.open("TRBSCAB") 
		    Trbscac = report.open("TRBSCAC") 
		    break 
	
	    case 'close':
		    Trbscaa.close()
		    Trbscab.close() 
		    Trbscac.close() 
	 	    break

	    default: 
		    info("INVALID IO OPERATION") 
		    break
	    }

    }

    private void printHeaders() {
	info("printHeaders") 

	    Date today = new Date() 

	    Trbscaa.write("SCA - Support Costs Allocation - Summary Report ")
	    Trbscaa.write(String.format("Run Date: %td/%tm/%tY            Run Time: %tH:%tH      ", today, today, today, today, today) ) 
        Trbscaa.write(String.format("%132s"," ").replace(' ', '-') ) 

    	Trbscab.write("SCA - Support Costs Allocation - Detail Work Order Transaction Report \n") 
    	Trbscab.write(String.format("Run Date: %td/%tm/%tY             Run Time: %tH:%tH    \n", today, today, today, today, today) ) 
	    Trbscab.write("Batch Number: " + this.getUUID().substring(0,8)+ "\n"  )
	    Trbscab.write("                                                         Derived                                SCA ")
	    Trbscab.write("                                                          SCA                                 Oncost")
	    Trbscab.write("Employee   Tran Date  #SCA match WO Number    Equip No   Acct Code     Lab Hours     Lab Rate Rate(%)    SCA (BJP) Value")
        Trbscab.write(String.format("%132s"," ").replace(' ', '-')+"\n" ) 
	
	    Trbscac.write("SCA - Support Costs Allocation - Transaction Error Report \n")  
	    Trbscac.write(String.format("Run Date: %td/%tm/%tY              Run Time: %tH:%tH    \n", today, today, today, today, today) ) 
	    Trbscac.write("Batch Number: " + this.getUUID().substring(0,8) + "\n"  )
	    Trbscac.write("\n") 
	    Trbscac.write("                      Derived   Derived     #SCA               ")
	    Trbscac.write("                        #SCA      SCA      Oncost  ")
	    Trbscac.write("Employee   Tran Date    Value    Acct Code   Rate WO_Number     Equip No    Lab Hours         Lab Rate                Error Details")
        Trbscac.write(String.format("%132s"," ").replace(' ', '-') ) 
    }


    /**
     * EDOI -  LabourCostWeek / MSF857
     *  
     * msf857rec 
     *  batch-type/labbatchno not = 'S'
     *  posting-status = 'PO'  
    */
    private void loopLabourCostWeek() {
	info("loopLabourCostWeek, EDOI") 

	def ec, wo, e0
	def code
	def oncostrate
	def accountcode = '  '
	def equipref
	def i = 0
	def j =  0
	def k = 0
	def totallabhours = 0
	def totalocv = 0
	boolean flag = false

        MSF857Key msf857key = new MSF857Key()
        Constraint c1 = MSF857Key.employeeId.greaterThan('0')
        Constraint c2 = MSF857Rec.labBatchNo.substring(1,1).equalTo('O') 
	Constraint c3 = MSF857Rec.postingStatus.equalTo('PO')
        def query = new QueryImpl(MSF857Rec.class).and(c1).and(c2).and(c3)

	// a) (MSF857) Earning Code . 3 chars (mandatory)
	// b) (MSF857/MSF620) Work Order Type . 2 chars (mandatory)
	// c) (MSF857/MSF600) Equipment Classification . 2 chars (optional)
        MSF857Key msf857keyDisp = new MSF857Key()
        edoi.search(query,{MSF857Rec msf857rec ->
	    msf857keyDisp = msf857rec.getPrimaryKey()
	    k++
	    oncostval = 0
	    info("Employee : " + msf857keyDisp.getEmployeeId() )
	    info("LabClassEarn : " + msf857keyDisp.getLabClassEarn() )
	    info("LabBatchNo : " + msf857rec.getLabBatchNo() )
	    info("Posting Status : " + msf857rec.getPostingStatus() )
	    info("Work Order : " + msf857rec.getWorkOrder() )  

	    if ( tableList(msf857keyDisp.getLabClassEarn().substring(6),null) ) {
            ec = msf857keyDisp.getLabClassEarn().substring(6)
            if ( workOrder(msf857keyDisp.getDstrctCode(), msf857rec.getWorkOrder().substring(0,2).trim()) ) {
	            wo = msf857rec.getWorkOrder().substring(0,2) 
	            oncostrate = getOnCostRate(ec+wo)
	            if (oncostrate != null) {
	                oncostval = calOnCostVal(msf857keyDisp.getDstrctCode(), msf857rec.getLabRate(), msf857rec.getLabTranHours(), oncostrate)
			        equipref = getEquipRef(msf857keyDisp.getDstrctCode(), msf857rec.getWorkOrder()) 
	            } else {
	               Trbscac.write(String.format("%10s %10s %7s %12s %6s %9s %12s %12s %16s %29s",
	               msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
  	               accountcode, msf857rec.getWorkOrder().substring(0,2), msf857rec.getWorkOrder(), msf857rec.getEquipNo(), msf857rec.getLabTranHours(), 
	               msf857rec.getLabRate(), '#SCA Oncost Rate'))
		       j++
   	            }
	        } else {
                    Trbscac.write(String.format("%10s %10s %7s %12s %6s %9s %12s %12s %16s %29s",
                    msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
  	                accountcode, msf857rec.getWorkOrder().substring(0,2), msf857rec.getWorkOrder(), msf857rec.getEquipNo(), msf857rec.getLabTranHours(), 
	                msf857rec.getLabRate(), 'Work Order Number')) 
		    	j++
	        }
	    } else {
		equipref = getEquipRef(msf857keyDisp.getDstrctCode(), msf857rec.getWorkOrder()) 
		if (!equipref.isEmpty() && equipref != null) { 
	        e0 = equipmentService(equipref) 
	        if (!e0.isEmpty() || e0 != null) { 
		        if (tableList(null, ec+wo+e0))  {
		            oncostrate = getOnCostRate(ec+wo+e0) 
	                    if (oncostrate != null) {
		               oncostval = calOnCostVal(msf857keyDisp.getDstrctCode(), msf857rec.getLabRate(), msf857rec.getLabTranHours(), oncostrate)
		            } else {
	                       Trbscac.write(String.format("%10s %10s %7s %12s %6s %9s %12s %12s %16s %29s",
		                   msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
  	                       accountcode, msf857rec.getWorkOrder().substring(0,2), msf857rec.getWorkOrder(), msf857rec.getEquipNo(), msf857rec.getLabTranHours(), 
		                   msf857rec.getLabRate(), '#SCA Oncost Rate'))
			       j++
		            }    
		        }  else {
	            	    Trbscac.write(String.format("%10s %10s %7s %12s %6s %9s %12s %12s %16s %29s",
	     	            msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
  	    	            accountcode, msf857rec.getWorkOrder().substring(0,2), msf857rec.getWorkOrder(), msf857rec.getEquipNo(), msf857rec.getLabTranHours(), 
	    	            msf857rec.getLabRate(), 'Derived #SCA Value')) 
			    j++  
			   }
	            } else {
	               Trbscac.write(String.format("%10s %10s %7s %12s %6s %9s %12s %12s %16s %29s",
	               msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
  	               accountcode, msf857rec.getWorkOrder().substring(0,2), msf857rec.getWorkOrder(), msf857rec.getEquipNo(), msf857rec.getLabTranHours(), 
	               msf857rec.getLabRate(), 'Equipment Class')) 
     		       j++ 
                       }
		} else {
           	    Trbscac.write(String.format("%10s %10s %7s %12s %6s %9s %12s %12s %16s %29s",
                    msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
                    accountcode, msf857rec.getWorkOrder().substring(0,2), msf857rec.getWorkOrder(), msf857rec.getEquipNo(), msf857rec.getLabTranHours(), 
                    msf857rec.getLabRate(), 'Equipment No'))
		    j++
		} 

	    }
 
	    if (oncostval > 0) {

       	    try {
	            i++

	    	/// write mst903 record
		        mst903 << String.format("%8s%04d%4s%3s%-24s%12s%6s%-17.2f%-17.2f%-17.2f%4s%8s%18s%-8s%8s%12s%10s%-40s%8s%1s%2s%15s%10s%8s%1s%1s%1s%24s%40s%2s%1s\n", 
		        this.getUUID().substring(0,8), i, msf857keyDisp.getDstrctCode(), 'BPJ', dac1+dac2, msf857rec.getSubledgerAcct(), 
		        new SimpleDateFormat('yyyyMM').format(new Date()), oncostval, 0.00, 0.00, 'AUD',msf857keyDisp.getLabTranDate(),'', msf857rec.getWorkOrder(),  
	            msf857rec.getProjectNo(), msf857rec.getEquipNo(),'', 'SCA'+ msf857rec.getWorkOrder()+ msf857keyDisp.getEmployeeId(),'','','',oncostval,'','',
	       	    '','','N','', msf857rec.getLabTranDesc(),'','N') 

    		// write detail report
                Trbscab.write(String.format("%10s %10s %7s %12s %6s %12s %11.2f %12.4f %7.2f %,15.2f",
                msf857keyDisp.getEmployeeId(), new SimpleDateFormat('dd/MM/yyyy').format(new Date()), msf857keyDisp.getLabClassEarn().substring(6),
                msf857rec.getWorkOrder(), msf857rec.getEquipNo(), dac1+dac2, msf857rec.getLabTranHours(), msf857rec.getLabRate(), ocr, oncostval))  

        	} catch(IOException e){
	            info("Error : " + e.getMessage() )
        	}

		    totallabhours = totallabhours + msf857rec.getLabTranHours() 
		    totalocv = totalocv + oncostval

       	    try {
	            i++ 
 	
		// write mst903 record
	 	        mst903 << String.format("%8s%04d%4s%3s%-24s%12s%6s%-17.2f%-17.2f%-17.2f%4s%8s%18s%-8s%8s%12s%10s%-40s%8s%1s%2s%15s%10s%8s%1s%1s%1s%24s%40s%2s%1s\n",  
		        this.getUUID().substring(0,8), i, msf857keyDisp.getDstrctCode(), 'BPJ', msf857rec.getEmpHomeAcct().substring(0,9)+dac3, msf857rec.getSubledgerAcct(), 
		        new SimpleDateFormat('yyyyMM').format(new Date()), oncostval*-1, 0.00, 0.00, 'AUD',msf857keyDisp.getLabTranDate(),'', msf857rec.getWorkOrder(), 
		        msf857rec.getProjectNo(), msf857rec.getEquipNo(),'', 'SCA'+ msf857rec.getWorkOrder()+ msf857keyDisp.getEmployeeId(),'','','',oncostval*-1,'','',
		        '','','N','', msf857rec.getLabTranDesc(),'','N') 

        	} catch(IOException e){
	            info("Error : " + e.getMessage() )
        	}
  
		//  Update MSF857 record.
           if (msf857rec.getLabBatchNo().size() > 2) {	
		       tmp = msf857rec.getLabBatchNo().substring(1)
		       msf857rec.setLabBatchNo('O'+tmp) 
		       edoi.update(msf857rec) 
		    } 
	
		    oncostval = 0
	    }

        })
        
 
	    Trbscab.write("\n") 
	    Trbscab.write(String.format("%132s"," ").replace(' ', '-') )
	    Trbscab.write(String.format("Batch Number: %8s   \n", this.getUUID().substring(0,8)) )
	    Trbscab.write( String.format("Total Employees: %5d                       Total Lab Hours: %11.2f          Total SCA (BPJ) Value: %,11.2f", i, totallabhours, totalocv) )
	    Trbscab.write("\n") 

	    Trbscac.write("\n") 
	    Trbscac.write(String.format("%132s"," ").replace(' ', '-') )
	    Trbscac.write(String.format("Batch Number: %8s      Total Errors: %6d  \n", this.getUUID().substring(0,8), j) )
	    Trbscac.write(String.format("%132s"," ").replace(' ', '-') )
	    Trbscac.write("\n") 

    }

    private BigDecimal calOnCostVal(def dstrctcode,def labrate, def labtranhours, def oncostrate) {
	info("calOnCostVal, EDOI") 

	    def oncostval = labrate * labtranhours
	    ocr = 1

        try {
            MSF856Key msf856key = new MSF856Key()
            Constraint c1 = MSF856Key.dstrctCode.equalTo(dstrctcode)
            Constraint c2 = MSF856Key.oncostRateTy.equalTo(oncostrate)
            def query = new QueryImpl(MSF856Rec.class).and(c1).and(c2)

            MSF856Key msf856keyDisp = new MSF856Key()
            edoi.search(query,{MSF856Rec msf856rec ->
		        msf856keyDisp = msf856rec.getPrimaryKey()
		        dac2 = msf856rec.getDrExpElement().substring(0,3) 
		        dac3 = msf856rec.getCrExpElement().substring(0,3)
		        ocr = ocr *  msf856rec.getOncostRate() 
	        })
	        
        } catch(EDOIObjectNotFoundException e){
            info("Error : " + e.getMessage() )
        }

	    if (ocr == 1) {return 0} 
	    oncostval = oncostval * ocr
  
	    return oncostval
    }

    private String equipmentService(def equipNo ) {
	info ("EquipmentService")

        List<EquipmentServiceRetrieveReplyDTO> equReplyList = new ArrayList<EquipmentServiceRetrieveReplyDTO>()
        EquipmentServiceRetrieveRequiredAttributesDTO equReqAtt = new EquipmentServiceRetrieveRequiredAttributesDTO()
        equReqAtt.returnEquipmentClass = true

        try {
            def restart = ""
            boolean firstLoop = true
            while (firstLoop || (restart !=null && restart.trim().length() > 0)){
                EquipmentServiceRetrieveReplyCollectionDTO equReplyDTO = service.get("Equipment").retrieve({EquipmentServiceRetrieveRequestDTO it ->
                    it.requiredAttributes = equReqAtt
                    it.equipmentNo = equipNo
                },100,  restart)
                firstLoop = false
                restart = equReplyDTO.getCollectionRestartPoint()
                equReplyList.addAll(equReplyDTO.getReplyElements())
            }
        } catch (EnterpriseServiceOperationException e){
            info("Error when retrieve Equipment Service ${e.getMessage()}")
        }

	    return equReplyList.get(0).equipmentClass 
    }

    private String getEquipRef(def dstrctcode, def wo) {
        info ("getEquipRef, EDOI")
 
        try {
           MSF620Rec msf620rec = edoi.findByPrimaryKey(new MSF620Key(dstrctcode, wo))
           if (!msf620rec.getEquipNo().isEmpty() || msf620rec.getEquipNo() != null) {
	       dac1 = msf620rec.getDstrctAcctCode().substring(4) 
        
	       return msf620rec.getEquipNo() 
	   }
       } catch (EDOIObjectNotFoundException  e) {
           info ("MSF620 Equipment Ref not found")
           return ' '
       }

    }


    private boolean workOrder(def dstrctcode, def woty) {
	info ("WorkOrder, EDOI")

	    try {
            MSF620Key msf620key = new MSF620Key()

            Constraint c1 = MSF620Key.dstrctCode.equalTo(dstrctcode)
            Constraint c2 = MSF620Rec.woType.equalTo(woty)
            def query = new QueryImpl(MSF620Rec.class).and(c1).and(c2) 

            String sResultWO
            sResultWO = edoi.firstRow(query)
	        if (sResultWO != null) 
	            { info("${woty}, workorder VALID"); return true  }

        } catch(EDOIObjectNotFoundException e){
            info("Error : " + e.getMessage() )
	        return false
	    }
    }


    private List<String> tableService() {
        info("TableService") 
        
        List<TableServiceRetrieveReplyDTO>tblReplyList = new ArrayList<TableServiceRetrieveReplyDTO>()
        TableServiceRetrieveRequiredAttributesDTO tblReqAtt = new TableServiceRetrieveRequiredAttributesDTO()
        tblReqAtt.returnTableCode = true
	    tblReqAtt.returnAssociatedRecord = true
	
        try {
            def restart = ""
            boolean firstLoop = true
            while (firstLoop || (restart !=null && restart.trim().length() > 0)){
                TableServiceRetrieveReplyCollectionDTO tblReplyDTO = service.get("Table").retrieve({TableServiceRetrieveRequestDTO it ->
                    it.requiredAttributes = tblReqAtt
                    it.tableType = '#SCA'  
                },100,  restart)
                firstLoop = false
                restart = tblReplyDTO.getCollectionRestartPoint()
                tblReplyList.addAll(tblReplyDTO.getReplyElements())
            }
        } catch (EnterpriseServiceOperationException e){
            info("Error when retrieve Table Service ${e.getMessage()}")
        }
 
	    return tblReplyList
    }


    private boolean tableList(def earncode, code) {
	info("TableList") 
	
	    def tstr = ''
		
	    if (earncode == null || earncode.isEmpty() ) { tstr = code }
	    else { tstr = earncode }

	    switch (tstr.size()) {
	    case 3: 
	        for (def i=0; i < tablecodes.size(); i++) { 
                if (tablecodes.get(i).tableCode.substring(0,3) == earncode) {
	                info("${earncode}, earncode VALID") 
	                return true
		        }
	        }

	    case 7: 
	        for (def i=0; i < tablecodes.size(); i++) { 
	        	info("tablecodes.get(i).tableCode : ${tablecodes.get(i).tableCode}")
	        	info("code : ${code}")
                if (tablecodes.get(i).tableCode == code) {
	                info("${code}, code VALID") 
	                return true
		        }
	        }

	    default: return false 	    

	    }

	return false
    }


    private String getOnCostRate(def code) {
	info("getOnCostRate, ${code} ") 

	    for (def i=0; i < tablecodes.size(); i++) {
            if (tablecodes.get(i).tableCode.equals(code) ) {
	            info("${tablecodes.get(i).associatedRecord}, onCostRate VALID")
	            return tablecodes.get(i).associatedRecord 
	        }
	    }
	
	    return null
    }

    private List<String> loopEmployeeService() {
        info("loopEmployeeService") 
        
        List<EmployeeServiceRetrieveReplyDTO> empReplyList = new ArrayList<EmployeeServiceRetrieveReplyDTO>()
        EmployeeServiceRetrieveRequiredAttributesDTO empReqAtt = new EmployeeServiceRetrieveRequiredAttributesDTO()
        empReqAtt.returnEmployee = true

        try {
            def restart = ""
            boolean firstLoop = true
            while (firstLoop || (restart !=null && restart.trim().length() > 0)){
                EmployeeServiceRetrieveReplyCollectionDTO empReplyDTO = service.get("Employee").retrieve({EmployeeServiceRetrieveRequestDTO it ->
                    it.requiredAttributes = empReqAtt
                    it.employee > '0'  
                },100,  restart)
                firstLoop = false
                restart = empReplyDTO.getCollectionRestartPoint()
                empReplyList.addAll(empReplyDTO.getReplyElements())
            }
        } catch (EnterpriseServiceOperationException e){
            info("Error when retrieve Employee Service ${e.getMessage()}")
        }

	return empReplyList 
    }

    private void loopLabourCostWeekService(def empid) {
        info("loopLabourCostWeek") 
        
        List<LabourCostWeekServiceRetrieveReplyDTO> lcwReplyList = new ArrayList<LabourCostWeekServiceRetrieveReplyDTO>()
        LabourCostWeekServiceRetrieveRequiredAttributesDTO lcwReqAtt = new LabourCostWeekServiceRetrieveRequiredAttributesDTO()
        lcwReqAtt.returnEmployee = true
        lcwReqAtt.returnPostingStatus = true
	    lcwReqAtt.returnWorkOrder = true
	
        Calendar cal = Calendar.getInstance()
        cal.setTime( new SimpleDateFormat("yyyyMMdd").format(new Date()) ) 

        try {
            def restart = ""
            boolean firstLoop = true
            while (firstLoop || (restart !=null && restart.trim().length() > 0)){
                LabourCostWeekServiceRetrieveReplyCollectionDTO lcwReplyDTO = service.get("LabourCostWeek").retrieve({LabourCostWeekServiceRetrieveRequestDTO it ->
                    it.requiredAttributes = lcwReqAtt
                    it.employee = empid 
                    it.weekEndDate = cal 
                },100,  restart)
                firstLoop = false
                restart = lcwReplyDTO.getCollectionRestartPoint()
                lcwReplyList.addAll(lcwReplyDTO.getReplyElements())
            }
        } catch (EnterpriseServiceOperationException e){
            info("Error when retrieve LabourCostWeek Service ${e.getMessage()}")
        }

    }


}


/*run script*/
ProcessTrbsca process = new ProcessTrbsca()
process.runBatch(binding)
