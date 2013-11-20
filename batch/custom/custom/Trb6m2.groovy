/**
 *@Ventyx 2012
 *
 * This program will create or update existing work orders in
 * ellipse through input file from trr6m2.
 * This program will be rollback per line input file when
 * action create or update fail.
 *
 * Revision History 
 *************************
 * Date        Name     Desc										Ver
 * 18/11/2013  LokeWS   SC4349620 TRB6M2 Create / Update Work Order         				6
 *                      Check duration provided if less than 24 then 
 *                      set the start and stop times. If number provided
 *                      more than 24, create multiple Work Order Duration,
 *                      each having maximum 24 hours. 
 * 21/08/2013  LokeWS   SC4349620 TRB6M2 Create / Update Work Order   				5 
 *                      Work Order Duration (if any) is not created 
 *						due to the warning return from routine.
 *						"WARNING: ELAPSED DIFFERENT THAN STOP/START TIMES" 
 *						Pass "false" to createWorkOrderDuration
 *                      service to ignore warning message   
 *
 */
 
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.lsi.buffer.planagecredit.PlanAgeCreditBufferImpl;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import com.mincom.ellipse.types.m0000.instances.OrigPriority;
import com.mincom.ellipse.eroi.linkage.mssdat.*;
import groovy.lang.Binding;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.mincom.ellipse.edoi.ejb.msf010.*;
import com.mincom.ellipse.edoi.ejb.msf008.*;
import com.mincom.ellipse.edoi.ejb.msf04d.*;
import com.mincom.ellipse.edoi.ejb.msf076.*;
import com.mincom.ellipse.edoi.ejb.msf080.*;
import com.mincom.ellipse.edoi.ejb.msf096.*;
import com.mincom.ellipse.edoi.ejb.msf542.*;
import com.mincom.ellipse.edoi.ejb.msf620.*;
import com.mincom.ellipse.edoi.ejb.msf621.*;
import com.mincom.ellipse.edoi.ejb.msf629.*;
import com.mincom.ellipse.edoi.ejb.msf62w.*;
import com.mincom.ellipse.edoi.ejb.msf660.*;
import com.mincom.ellipse.edoi.ejb.msf760.*;
import com.mincom.ellipse.edoi.ejb.msf766.*;
import com.mincom.ellipse.edoi.ejb.msf801.*;
import com.mincom.ellipse.edoi.ejb.msf808.*;
import com.mincom.ellipse.edoi.ejb.msf810.*;
import com.mincom.ellipse.edoi.ejb.msf820.*;
import com.mincom.ellipse.edoi.ejb.msf824.*;
import com.mincom.ellipse.edoi.ejb.msf829.*;
import com.mincom.ellipse.edoi.ejb.msf8p1.*;
import com.mincom.ellipse.edoi.ejb.msf8p4.*;
import com.mincom.ellipse.edoi.ejb.msf920.*;
import com.mincom.ellipse.edoi.ejb.msf930.*;
import com.mincom.ellipse.edoi.ejb.msf93f.*;
import com.mincom.ellipse.edoi.ejb.msf966.*;
import com.mincom.ellipse.edoi.ejb.msfprt.*;
import com.mincom.ellipse.edoi.ejb.msfx55.*;
import com.mincom.ellipse.edoi.ejb.msfx6o.*;
import com.mincom.ellipse.edoi.ejb.msfx6x.*;
import com.mincom.ellipse.edoi.ejb.msf581.*;

import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceAppendReplyDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceAppendRequestDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceDeleteRequestDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextRequestDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceSetTextRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCompleteReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCompleteRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateWorkOrderDurationReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceCreateWorkOrderDurationRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceDeleteReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceDeleteRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceDeleteWorkOrderDurationRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceModifyReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceModifyRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceModifyRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadRequestDTO;
import com.mincom.enterpriseservice.ellipse.workorder.WorkOrderServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.dependant.dto.DurationsDTO;
import com.mincom.enterpriseservice.ellipse.dependant.dto.WorkOrderDTO;


public class ParamsTrb6m2{
	//List of Input Parameters

}

public class ProcessTrb6m2 extends SuperBatch {
	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private version = 6

	private ParamsTrb6m2   batchParams;
	private BufferedReader inputFile;
	private def            reportA;
	private boolean        bInputFileOpen = false;
	private boolean        bReportAOpen   = false;
	private boolean        bUpdateMode    = false;
	private boolean        firstErr = true
	private WorkOrderDTO   workOrderNo;
	private Long           lLineNo        = 0;
	private String         sDistrictCode  = " ";
	private WorkOrderServiceReadReplyDTO workOrderImg;
	private StdTextServiceGetTextReplyCollectionDTO stdTextImg;
	private StdTextServiceGetTextReplyCollectionDTO completionImg;
	private String fieldRef
	//	private String errValue
	private String errMsg


	private static final String  INPUT_FILE_NAME    = "TRI6M2.txt";
	private static final String  PROGRAM_NAME       = "TRB6M2"
	private static final String  REPORT_A_NAME      = "TRB6M2A";
	private static final String  MODULE             = "3620";
	private static final Integer VALID_LENGTH       =  494;
	private static final String  WORK_ORDER_SERVICE = "WORKORDER";
	private static final String  STD_TEXT_SERVICE   = "STDTEXT";
	private static final String  MIN_DATE           = "01011900"

	// error message
	private static final String  INVALID_DATE       = "Invalid Date"

	private class InputRecord{
		private WorkOrderDTO workOrderNo;
		private String workOrderDesc;
		private String standardJobNo;
		private String workOrderType;
		private String maintenanceType;
		private String componentCode;
		private String origPriority;
		private String workGroup;
		private String equipNumber;
		private String equipPlantNo;
		private String raisedDate;
		private String originatorId;
		private String unitsRequired;
		private String unitsComplete;
		private String extText;
		private String completedBy;
		private String completionCode;
		private String closedDate;
		private String completionText
		private String relatedWo;
		private String planStrDate;
		private String jobCode1;
		private String jobCode2;
		private String jobCode3;
		private String jobCode4;
		private String jobCode5;
		private String jobCode6;
		private String jobCode7;
		private String maintSchedTaskNo;
		private String sessionComplInspect;
		private String origDocNo;
		private String estOtherCosts;

		public InputRecord(WorkOrderDTO workOrderNo, String restOfLine){
			this.workOrderNo         = workOrderNo;
			this.workOrderDesc       = restOfLine.substring(0,100).trim();
			this.standardJobNo       = restOfLine.substring(100,106).trim()
			this.workOrderType       = restOfLine.substring(106,108).trim();
			this.maintenanceType     = restOfLine.substring(116,118).trim();
			this.componentCode       = restOfLine.substring(118,122).trim();
			this.origPriority        = restOfLine.substring(122,124).trim();
			this.workGroup           = restOfLine.substring(128,135).trim();
			this.equipNumber         = restOfLine.substring(135,146).trim();
			this.equipPlantNo        = restOfLine.substring(146,158).trim();
			this.raisedDate          = restOfLine.substring(158,166).trim();
			this.originatorId        = restOfLine.substring(166,177).trim();
			this.unitsRequired       = restOfLine.substring(177,189).trim();
			this.unitsComplete       = restOfLine.substring(189,201).trim();
			this.extText             = restOfLine.substring(201,251).trim();
			this.completedBy         = restOfLine.substring(251,262).trim();
			this.completionCode      = restOfLine.substring(262,264).trim();
			this.closedDate          = restOfLine.substring(264,272).trim();
			this.completionText      = restOfLine.substring(272,372).trim();
			this.relatedWo           = restOfLine.substring(372,384).trim();
			this.planStrDate         = restOfLine.substring(384,392).trim();
			this.jobCode1            = restOfLine.substring(392,398).trim();
			this.jobCode2            = restOfLine.substring(398,404).trim();
			this.jobCode3            = restOfLine.substring(404,410).trim();
			this.jobCode4            = restOfLine.substring(410,416).trim();
			this.jobCode5            = restOfLine.substring(416,422).trim();
			this.jobCode6            = restOfLine.substring(422,428).trim();
			this.jobCode7            = restOfLine.substring(428,434).trim();
			this.maintSchedTaskNo    = restOfLine.substring(434,438).trim();
			this.sessionComplInspect = restOfLine.substring(438,451).trim();
			this.origDocNo           = restOfLine.substring(451,471).trim();
			this.estOtherCosts       = restOfLine.substring(471).trim();
		}
	}

	public void runBatch(Binding b){

		init(b)
		printSuperBatchVersion()
		info("runBatch Version : " + version)

		//PrintRequest Parameters
		info("No input parameters")
		try {
			processBatch();
		}
		catch(Exception e){
			info("error ${e.getMessage()}")
		}
		finally {

			if(bInputFileOpen){
				inputFile.close();
			}
			if(bReportAOpen){
				reportA.close();
			}

		}

	}

	//additional method - start from here.

	private void processBatch(){
		info("processBatch");
		initialise();
		processRequest();
	}

	private void initialise(){
		info("initialise");
		sDistrictCode = commarea.district;

		String path = env.getWorkDir().toString()+"/"+INPUT_FILE_NAME;
        String uuid  = getTaskUUID();
        if(uuid?.trim()) {
            path = path + "." + uuid
        }
		inputFile = openInputFile(path);
		bInputFileOpen = true;
	}

	private void processRequest(){
		info("processRequest");
		String lineRead = null;
		if(inputFile != null){
			while ((lineRead = inputFile.readLine()) != null){
                               lLineNo++
			       if(isValidLine(lineRead)){
					info("bupdate mode :${bUpdateMode}")
					InputRecord inputRecord = new InputRecord(workOrderNo,lineRead.substring(11));
					info("work order  : ${inputRecord.workOrderNo}")
					info("work desc   : ${inputRecord.workOrderDesc}")
					info("stdjob      : ${inputRecord.standardJobNo}")
					info("work type   : ${inputRecord.workOrderType}")
					info("Main type   : ${inputRecord.maintenanceType}")
					info("Component cd: ${inputRecord.componentCode}")
					info("orig prior  : ${inputRecord.origPriority}")
					info("work grp    : ${inputRecord.workGroup}")
					info("equip no    : ${inputRecord.equipNumber}")
					info("equip plan  : ${inputRecord.equipPlantNo}")
					info("raise date  : ${inputRecord.raisedDate}")
					info("originator  : ${inputRecord.originatorId}")
					info("unit requir : ${inputRecord.unitsRequired}")
					info("unit comp   : ${inputRecord.unitsComplete}")
					info("ext text    : ${inputRecord.extText}")
					info("completed id: ${inputRecord.completedBy}")
					info("completion  : ${inputRecord.completionCode}")
					info("closed dt   : ${inputRecord.closedDate}")
					info("completiontx: ${inputRecord.completionText}")
					info("relatedWO   : ${inputRecord.relatedWo}")
					info("Plan str dt : ${inputRecord.planStrDate}")
					info("wo jb code 1: ${inputRecord.jobCode1}")
					info("wo jb code 2: ${inputRecord.jobCode2}")
					info("wo jb code 3: ${inputRecord.jobCode3}")
					info("wo jb code 4: ${inputRecord.jobCode4}")
					info("wo jb code 5: ${inputRecord.jobCode5}")
					info("wo jb code 6: ${inputRecord.jobCode6}")
					info("wo jb code 7: ${inputRecord.jobCode7}")
					info("mn schd tsk : ${inputRecord.maintSchedTaskNo}")
					info("session cmp : ${inputRecord.sessionComplInspect}")
					info("orig dc no  : ${inputRecord.origDocNo}")
					info("est oth cst : ${inputRecord.estOtherCosts}")
					// validate input type date
					boolean validDate = true
					if(inputRecord.raisedDate?.trim()){
						if (!isValidDate(inputRecord.raisedDate)){
							validDate = false
							fieldRef = "raised Date: ${inputRecord.raisedDate}"
							errMsg = INVALID_DATE
							printErrorMsg()
						}
					}
					if(inputRecord.planStrDate?.trim()){
						if(!isValidDate(inputRecord.planStrDate)){
							validDate = false
							fieldRef = "plan start Date: ${inputRecord.planStrDate}"
							errMsg = INVALID_DATE
							printErrorMsg()
						}
					}
					if(inputRecord.closedDate?.trim()){
						if(!isValidDate(inputRecord.closedDate)){
							validDate = false
							fieldRef = "closed Date: ${inputRecord.closedDate}"
							errMsg = INVALID_DATE
							printErrorMsg()
						}
					}
					if(validDate){
						if(bUpdateMode){
							updateWorkOrder(inputRecord);
						}else{
							createWorkOrder(inputRecord);
						}
					}
				}
			}
			if(lLineNo == 0){
				fieldRef = INPUT_FILE_NAME
				errMsg = "Input File is Empty"
				printErrorMsg()
			}
		}else{
			info("error file not found")
			bInputFileOpen = false
			fieldRef = INPUT_FILE_NAME
			errMsg = "Unable to open input File"
			printErrorMsg()
		}
	}

	/**
	 * This method updates the Work Order with new information from the read line.
	 * It will also create/modify standard text, create duration, and complete the WO when needed.
	 * @param inputRecord
	 */
	private void updateWorkOrder(InputRecord inputRecord){
		info("updateWorkOrder");

		/*Update WO With service*/
		info("work order ${workOrderNo.getNo()}")
		info("work description ${inputRecord.workOrderDesc}")
		info("std job:  ${inputRecord.standardJobNo}")
		WorkOrderDTO relatedWo = new WorkOrderDTO()
		if(inputRecord.relatedWo?.trim()){
			relatedWo.setPrefix(inputRecord.relatedWo.substring(0,2))
			relatedWo.setNo(inputRecord.relatedWo.substring(2))
		}
		Calendar raisedDate = stringToCalendar(inputRecord.raisedDate)
		Calendar planStrDate = stringToCalendar(inputRecord.planStrDate)
		boolean validWoUpdt = true

		//create rollback image of record
		readWorkOrder(inputRecord.workOrderNo)


		WorkOrderServiceModifyRequiredAttributesDTO woModAtt = new WorkOrderServiceModifyRequiredAttributesDTO()
		//woModAtt.returnWorkOrder = true
		//woModAtt.returnWorkOrderDesc = true
		//woModAtt.returnStdJobNo = true
		try{
			WorkOrderServiceModifyReplyDTO woReplyDTO = service.get(WORK_ORDER_SERVICE).modify({WorkOrderServiceModifyRequestDTO it->
				//it.requiredAttributes = woModAtt
				it.districtCode = sDistrictCode
				it.workOrder = workOrderNo
				it.workOrderDesc = inputRecord.workOrderDesc
				it.stdJobNo = inputRecord.standardJobNo
				it.workOrderType = inputRecord.workOrderType
				it.maintenanceType = inputRecord.maintenanceType
				it.compCode = inputRecord.componentCode
				it.workGroup = inputRecord.workGroup
				it.equipmentNo = inputRecord.equipNumber
				it.equipmentRef = inputRecord.equipPlantNo
				if(inputRecord.raisedDate?.trim()){
					it.raisedDate = raisedDate
				}
				it.originatorId = inputRecord.originatorId
				if(inputRecord.unitsRequired?.trim() && inputRecord.unitsRequired.isNumber()){
					it.unitsRequired = inputRecord.unitsRequired.toBigDecimal()
				}
				it.relatedWo = relatedWo
				if(inputRecord.planStrDate?.trim()){
					it.planStrDate = planStrDate
				}
				it.jobCode1 = inputRecord.jobCode1
				it.jobCode2 = inputRecord.jobCode2
				it.jobCode3 = inputRecord.jobCode3
				it.jobCode4 = inputRecord.jobCode4
				it.jobCode5 = inputRecord.jobCode5
				it.jobCode6 = inputRecord.jobCode6
				it.jobCode7 = inputRecord.jobCode7
				it.maintenanceSchedTask = inputRecord.maintSchedTaskNo
				if(inputRecord.origDocNo?.trim()){
					it.origDocType = "ME"
					it.origDocNo = inputRecord.origDocNo
				}
			},false)
			info("work order : ${woReplyDTO.workOrder}")
			info("work order desc : ${woReplyDTO.workOrderDesc}")
			info("std job reply : ${woReplyDTO.stdJobNo}")
			info("work type : ${woReplyDTO.workOrderType}")
			info("maint type : ${woReplyDTO.maintenanceType}")
			info("comp code : ${woReplyDTO.compCode}")
			info("work Group : ${woReplyDTO.workGroup}")
			info("equip No : ${woReplyDTO.equipmentNo}")
			inputRecord.workOrderNo = woReplyDTO.workOrder
			validWoUpdt = true
		}
		catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Error when update workOrder ${workOrderNo.toString()} ${e.getMessage()}")
			validWoUpdt = false
			for(ErrorMessageDTO error : e.errorMessages){
				fieldRef = error.getFieldName()
				errMsg = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
				printErrorMsg()
			}
		}

		/*Do other common process*/
		if(validWoUpdt){
			commonProcess(inputRecord);
		}

	}

	/**
	 * This method create a new Work Order based on the read line.
	 * It will also create/modify standard text, create duration, and complete the WO when needed.
	 * @param inputRecord
	 */
	private void createWorkOrder(InputRecord inputRecord){
		info("createWorkOrder");
		Calendar raisedDate = stringToCalendar(inputRecord.raisedDate)
		Calendar planStrDate = stringToCalendar(inputRecord.planStrDate)
		WorkOrderDTO relatedWo = new WorkOrderDTO()
		if(inputRecord.relatedWo?.trim()){
			relatedWo.setPrefix(inputRecord.relatedWo.substring(0,2))
			relatedWo.setNo(inputRecord.relatedWo.substring(2))
		}
		String origPriority = String.format("%02d",inputRecord.origPriority.toInteger())
		boolean validWocrt = true
		info("origPriority: ${origPriority}")
		/*Create WO With service*/
		try{
			WorkOrderServiceCreateReplyDTO crtReplyDto = service.get(WORK_ORDER_SERVICE).create({WorkOrderServiceCreateRequestDTO it->
				it.districtCode = sDistrictCode
				it.workOrder = inputRecord.workOrderNo
				it.workOrderDesc = inputRecord.workOrderDesc
				if(inputRecord.standardJobNo?.trim()){
					it.stdJobNo = inputRecord.standardJobNo
				}
				it.workOrderType = inputRecord.workOrderType
				it.maintenanceType = inputRecord.maintenanceType
				if(inputRecord.componentCode?.trim()){
					it.compCode = inputRecord.componentCode
				}
				if(inputRecord.origPriority?.trim()){
					it.origPriority = origPriority
				}
				if(inputRecord.workGroup?.trim()){
					it.workGroup = inputRecord.workGroup
				}
				if(inputRecord.equipNumber?.trim()){
					it.equipmentNo = inputRecord.equipNumber
				}
				if(inputRecord.equipPlantNo?.trim()){
					it.equipmentRef = inputRecord.equipPlantNo
				}
				it.raisedDate = raisedDate
				it.originatorId = inputRecord.originatorId
				if(inputRecord.unitsRequired?.trim() && inputRecord.unitsRequired.isNumber()){
					it.unitsRequired = inputRecord.unitsRequired.toBigDecimal()
				}
				if(inputRecord.relatedWo?.trim()){
					it.relatedWo = relatedWo
				}
				if(inputRecord.planStrDate?.trim()){
					it.planStrDate = planStrDate
				}
				if(inputRecord.jobCode1?.trim()){
					it.jobCode1 = inputRecord.jobCode1
				}
				if(inputRecord.jobCode2?.trim()){
					it.jobCode2 = inputRecord.jobCode2
				}
				if(inputRecord.jobCode3.trim()){
					it.jobCode3 = inputRecord.jobCode3
				}
				if(inputRecord.jobCode4?.trim()){
					it.jobCode4 = inputRecord.jobCode4
				}
				if(inputRecord.jobCode5?.trim()){
					it.jobCode5 = inputRecord.jobCode5
				}
				if(inputRecord.jobCode6?.trim()){
					it.jobCode6 = inputRecord.jobCode6
				}
				if(inputRecord.jobCode7?.trim()){
					it.jobCode7 = inputRecord.jobCode7
				}
				if(inputRecord.maintSchedTaskNo?.trim()){
					it.maintenanceSchedTask = inputRecord.maintSchedTaskNo
				}
				if(inputRecord.origDocNo?.trim()){
					it.origDocType = "ME"
					it.origDocNo = inputRecord.origDocNo
				}
				if(inputRecord.estOtherCosts?.trim() && inputRecord.estOtherCosts.isNumber()){
					it.estimatedOtherCost = inputRecord.estOtherCosts.toBigDecimal()
				}
			})
			info("workOrder pre: ${crtReplyDto.workOrder.getPrefix()}")
			info("workOrder no: ${crtReplyDto.workOrder.getNo()}")
			info("work des : ${crtReplyDto.workOrderDesc}")
			info("std job : ${crtReplyDto.stdJobNo}")
			info("wo type : ${crtReplyDto.workOrderType}")
			info("related wo : ${crtReplyDto.relatedWo}")
			inputRecord.workOrderNo = crtReplyDto.workOrder
			validWocrt = true
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Error when create workOrder ${workOrderNo.toString()} ${e.getMessage()}")
			validWocrt = false
			for(ErrorMessageDTO error : e.errorMessages){
				fieldRef = error.getFieldName()
				errMsg = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
				printErrorMsg()
			}
		}

		/*Do other common process*/
		if(validWocrt){
                       commonProcess(inputRecord);
		}
	}
	/**
	 * method to process create/modify :
	 * extended text, create work order duration,
	 * wo completion comment, complete WO
	 * @param inputRecord
	 */
	private void commonProcess(InputRecord inputRecord){
		info("commonProcess")
		/*Create/Modify extended text when needed*/
		boolean validExtText = true
		boolean validDur = true
		boolean validCompletion = true
		if(inputRecord.extText?.trim()){
			if(bUpdateMode){
				//store a rollback image of std text
				readExtText(inputRecord.workOrderNo)
			}
			validExtText = appendExtText(inputRecord)
		}

		/*Create duration when session complete inspection exists*/
		if(inputRecord.sessionComplInspect?.trim() && validExtText){
			validDur = createDuration(inputRecord)
		}

		/*Complete the WO when completion details exist*/
		if(inputRecord.completedBy?.trim() && inputRecord.completionCode?.trim() && inputRecord.closedDate?.trim() && validDur && validExtText){
			if(inputRecord.completionText?.trim()){
				if(bUpdateMode){
					//store a rollback image for completion comment
					readCompletionCom(inputRecord.workOrderNo)
				}
				validCompletion = completionComment(inputRecord)
			}
			if(validCompletion){
				completeWO(inputRecord)
			}
		}
	}
	/**
	 * create extended text for WO
	 * using append std text service
	 * @param inputRecord
	 * @return
	 */
	private boolean appendExtText(InputRecord inputRecord){
		info("appendExtText")
		String extTextId = "WO" + sDistrictCode + inputRecord.workOrderNo.getPrefix() + inputRecord.workOrderNo.getNo()
		String[] lines = wrapText(inputRecord.extText.trim(), 60)
		boolean returnFlag = false
		info("std Text id : ${extTextId}")
		try{
			service.get(STD_TEXT_SERVICE).append({StdTextServiceAppendRequestDTO it->
				it.lineCount = 1
				it.stdTextId = extTextId
				it.textLine = lines
			})
			returnFlag = true
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot append standard text for ${extTextId}: ${e.getMessage()}")
			for(ErrorMessageDTO error : e.errorMessages){
				fieldRef = error.getFieldName()
				errMsg = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
				printErrorMsg()
			}

			//rollback wo creation
			if(bUpdateMode){
				rollbackWO()
			}else{
				deleteWorkOrder(inputRecord.workOrderNo)
			}
			returnFlag = false
		}
		return returnFlag
	}

	/**
	 * Create duration with duration code 'STR'
	 * @param inputRecord
	 */
	private boolean createDuration(InputRecord inputRecord){
		info("createDuration")

		//LokeWS - SC4349620 - Version 6 - Create multiple duration if number provided more than 24
		BigDecimal totalHours = inputRecord.sessionComplInspect.toBigDecimal();
		List durationList = new ArrayList();
		BigDecimal remainHours = totalHours;
		
		info("Work Order [" + inputRecord.workOrderNo + "] session completed inspections [" + totalHours + "]");
		
		while(remainHours.compareTo(new BigDecimal(24)) >= 0) {
			DurationsDTO duration = new DurationsDTO()
			duration.setJobDurationsDate(Calendar.getInstance())
			duration.setJobDurationsCode("STR")
			
			duration.setJobDurationsHours(new BigDecimal(24));
			
			remainHours = remainHours.subtract(new BigDecimal(24));
			
			durationList.add(duration);
		}
		
		if(remainHours.compareTo(BigDecimal.ZERO) > 0) {
			DurationsDTO duration = new DurationsDTO()
			duration.setJobDurationsDate(Calendar.getInstance())
			duration.setJobDurationsCode("STR")
			
			Calendar startTime = Calendar.getInstance();
			startTime.setTime(new SimpleDateFormat("HHmmss").parse("000000"));
			
			duration.setJobDurationsStart(startTime)
			
			Calendar endTime = Calendar.getInstance();
			String remainTxt = "" + remainHours
			if(remainTxt.length() < 2)
				remainTxt = "0" + remainTxt
				
			endTime.setTime(new SimpleDateFormat("HHmmss").parse(remainTxt + "0000"));
			duration.setJobDurationsFinish(endTime)
			
			duration.setJobDurationsHours(remainHours);
			
			durationList.add(duration);
		}

		//LokeWS - SC4349620 - Version 6 - Comment out, not using anymore
		//DurationsDTO duration = new DurationsDTO()
		//duration.setJobDurationsDate(Calendar.getInstance())
		//duration.setJobDurationsCode("STR")
		//if(inputRecord.sessionComplInspect.isNumber()){
		//	duration.setJobDurationsHours(inputRecord.sessionComplInspect.toBigDecimal())
		//}
		//duration.setJobDurationsHours(inputRecord.sessionComplInspect.toBigDecimal())
		
		boolean returnFlag
		try{
			WorkOrderServiceCreateWorkOrderDurationReplyDTO woDurReply = service.get(WORK_ORDER_SERVICE).createWorkOrderDuration({ WorkOrderServiceCreateWorkOrderDurationRequestDTO it->
				it.districtCode = sDistrictCode
				it.workOrder = inputRecord.workOrderNo
				//LokeWS - SC4349620 - Version 6 - Use duration list to cater for multiple duration record
				//it.durations = duration
				it.durations = durationList.toArray()
			//LokeWS - SC4349620 - Version 5, add false as parameter to ignore warning
			}, false)
			returnFlag = true
		}
		catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot create work order duration for ${workOrderNo.toString()}: ${e.getMessage()}")
			for(ErrorMessageDTO error : e.errorMessages){
				fieldRef = error.getFieldName()
				errMsg = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
				printErrorMsg()
			}

			//if fail rollback wo creation and extText
			if(bUpdateMode){
				rollbackWO()
				rollbackExtText(inputRecord.workOrderNo)
			}else{
				deleteWorkOrder(inputRecord.workOrderNo)
			}
			returnFlag = false
		}
		return returnFlag
	}
	/**
	 * create completion comment using append std text services
	 * @param inputRecord
	 * @return
	 */
	private boolean completionComment(InputRecord inputRecord){
		info("completionComment")
		String compTextID = "CW" + sDistrictCode + inputRecord.workOrderNo.getPrefix() + inputRecord.workOrderNo.getNo()
		boolean returnFlag
		String[] lines = wrapText(inputRecord.completionText.trim(), 60)
		try{
			service.get(STD_TEXT_SERVICE).append({StdTextServiceAppendRequestDTO it->
				it.lineCount = 1
				it.stdTextId = compTextID
				it.textLine = lines
			})
			returnFlag = true
		}
		catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot create completion comment for ${compTextID}: ${e.getMessage()}")
			for(ErrorMessageDTO error : e.errorMessages){
				fieldRef = error.getFieldName()
				errMsg = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
				printErrorMsg()
			}

			if(bUpdateMode){
				rollbackWO()
				rollbackExtText(inputRecord.workOrderNo)
				if(inputRecord.sessionComplInspect?.trim()){
					deleteWoDuration(inputRecord.workOrderNo, inputRecord.sessionComplInspect)
				}
			}else{
				if(inputRecord.sessionComplInspect?.trim()){
					deleteWoDuration(inputRecord.workOrderNo, inputRecord.sessionComplInspect)
				}
				deleteWorkOrder(inputRecord.workOrderNo)
			}
			returnFlag = false
		}
		return returnFlag
	}

	/**
	 * complete work order using work order complete service
	 * @param inputRecord
	 * @return boolean true or false
	 */
	private boolean completeWO(InputRecord inputRecord){
		info("completeWO")
		Calendar closedDate = stringToCalendar(inputRecord.closedDate)
		boolean returnFlag
		try{
			WorkOrderServiceCompleteReplyDTO woCompReply =  service.get(WORK_ORDER_SERVICE).complete({WorkOrderServiceCompleteRequestDTO it->
				it.districtCode = sDistrictCode
				it.workOrder   = inputRecord.workOrderNo
				it.closedDate  = closedDate
				it.completedBy = inputRecord.completedBy
				it.completedCode = inputRecord.completionCode
			},false)
			info("completed work order success")
			returnFlag = true
		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e){
			info("Cannot complete work order  ${inputRecord.workOrderNo.toString()}: ${e.getMessage()}")
			//rollback work order duration & work order create
			info("message : ${e.getErrorMessages()[0].getMessage()}")
			info("fieldName : ${e.getErrorMessages()[0].getFieldName()}")
			info("errorcode : ${e.getErrorMessages()[0].getCode()}")
			for(ErrorMessageDTO error : e.errorMessages){
				fieldRef = error.getFieldName()
				errMsg = error.getCode().replace("mims.e.","") + "-" + error.getMessage()
				printErrorMsg()
			}

			if(bUpdateMode){
				rollbackWO()
				rollbackExtText(inputRecord.workOrderNo)
				if(inputRecord.sessionComplInspect?.trim()){
					deleteWoDuration(inputRecord.workOrderNo, inputRecord.sessionComplInspect)
				}
				if(inputRecord.completionText?.trim()){
					rollbackCompletion(inputRecord.workOrderNo)
				}
			}else{
				if(inputRecord.sessionComplInspect?.trim()){
					deleteWoDuration(inputRecord.workOrderNo, inputRecord.sessionComplInspect)
				}
				deleteWorkOrder(inputRecord.workOrderNo)
			}
			returnFlag = false
		}
		return returnFlag
	}
	/**
	 * delete/rollback work order if the process creation
	 * extended text error
	 * @param workOrder
	 */
	private void deleteWorkOrder(WorkOrderDTO workOrder){
		info("deleteWorkOrder")
		service.get(WORK_ORDER_SERVICE).delete({WorkOrderServiceDeleteRequestDTO it->
			it.districtCode = sDistrictCode
			it.workOrder = workOrder
		})
		info("rollback work order ${workOrder} success")
	}

	/**
	 * delete or rollback workorder duration
	 * if work order complete or completion comment service fail
	 * @param workOrder
	 * @param sessionComlInspect
	 */
	private void deleteWoDuration(WorkOrderDTO workOrder, String sessionComlInspect){
		info("deleteWoDuration")
		DurationsDTO duration = new DurationsDTO()
		duration.setJobDurationsDate(Calendar.getInstance())
		duration.setJobDurationsCode("STR")
		duration.setJobDurationsHours(sessionComlInspect.toBigDecimal())
		service.get(WORK_ORDER_SERVICE).deleteWorkOrderDuration({WorkOrderServiceDeleteWorkOrderDurationRequestDTO it->
			it.districtCode = sDistrictCode
			it.workOrder = workOrder
			it.durations = duration
		})
		info("rollback work order duration ${workOrder} success")
	}

	/**
	 * read work order to create a rollback image of the record
	 * @param workOrder
	 */
	private void readWorkOrder(WorkOrderDTO workOrder){
		info("workOrderServiceRead")
		WorkOrderServiceReadRequiredAttributesDTO woReq = new WorkOrderServiceReadRequiredAttributesDTO()
		woReq.returnDistrictCode = true
		woReq.returnWorkOrder = true
		woReq.returnWorkOrderDesc = true
		woReq.returnStdJobNo = true
		woReq.returnWorkOrderType = true
		woReq.returnMaintenanceType = true
		woReq.returnCompCode = true
		woReq.returnWorkGroup = true
		woReq.returnEquipmentNo = true
		woReq.returnEquipmentRef = true
		woReq.returnRaisedDate = true
		woReq.returnOriginatorId = true
		woReq.returnUnitsRequired = true
		woReq.returnRelatedWo = true
		woReq.returnPlanStrDate = true
		woReq.returnJobCode1 = true
		woReq.returnJobCode2 = true
		woReq.returnJobCode3 = true
		woReq.returnJobCode4 = true
		woReq.returnJobCode5 = true
		woReq.returnJobCode6 = true
		woReq.returnJobCode7 = true
		woReq.returnMaintenanceSchedTask = true
		woReq.returnOrigDocType = true
		woReq.returnOrigDocNo = true
		WorkOrderServiceReadReplyDTO woReply = service.get(WORK_ORDER_SERVICE).read({WorkOrderServiceReadRequestDTO it->
			it.requiredAttributes = woReq
			it.districtCode = sDistrictCode
			it.workOrder = workOrder
		})
		workOrderImg = new WorkOrderServiceReadReplyDTO()
		workOrderImg = woReply
	}
	/**
	 * rollback Work Order if the service call update fail
	 */
	private void rollbackWO(){
		info("rollbackWO")

		service.get(WORK_ORDER_SERVICE).modify({WorkOrderServiceModifyRequestDTO it->
			it.districtCode = workOrderImg.districtCode
			it.workOrder = workOrderImg.workOrder
			it.workOrderDesc = workOrderImg.workOrderDesc
			it.stdJobNo = workOrderImg.stdJobNo
			it.workOrderType = workOrderImg.workOrderType
			it.maintenanceType = workOrderImg.maintenanceType
			it.compCode = workOrderImg.compCode
			it.workGroup = workOrderImg.workGroup
			it.equipmentNo = workOrderImg.equipmentNo
			it.equipmentRef = workOrderImg.equipmentRef
			it.raisedDate = workOrderImg.raisedDate
			it.originatorId = workOrderImg.originatorId
			it.unitsRequired = workOrderImg.unitsRequired
			it.relatedWo = workOrderImg.relatedWo
			it.planStrDate = workOrderImg.planStrDate
			it.jobCode1 = workOrderImg.jobCode1
			it.jobCode2 = workOrderImg.jobCode2
			it.jobCode3 = workOrderImg.jobCode3
			it.jobCode4 = workOrderImg.jobCode4
			it.jobCode5 = workOrderImg.jobCode5
			it.jobCode6 = workOrderImg.jobCode6
			it.jobCode7 = workOrderImg.jobCode7
			it.maintenanceSchedTask = workOrderImg.maintenanceSchedTask
			it.origDocType = workOrderImg.origDocType
			it.origDocNo = workOrderImg.origDocNo
		})
	}

	/**
	 * read the extended text to create a rollback image
	 * for work order ext text.
	 * @param workOrder
	 */
	private void readExtText(WorkOrderDTO workOrder){
		info("readExtText")
		String extTextId = "WO" + sDistrictCode + workOrder.getPrefix() + workOrder.getNo()
		StdTextServiceGetTextReplyCollectionDTO stdTextReply = service.get(STD_TEXT_SERVICE).getText({StdTextServiceGetTextRequestDTO it->
			it.stdTextId = extTextId
		})
		stdTextImg = new StdTextServiceGetTextReplyCollectionDTO()
		stdTextImg = stdTextReply
	}
	/**
	 * rollback extText if the service call fail
	 * @param workOrder
	 */
	private void rollbackExtText(WorkOrderDTO workOrder){
		info("rollbackExtText")
		String extTextId = "WO" + sDistrictCode + workOrder.getPrefix() + workOrder.getNo()
		//delete stdText first before rollback
		service.get(STD_TEXT_SERVICE).delete({StdTextServiceDeleteRequestDTO it->
			it.stdTextId = extTextId
		})
		int i = 0
		for(StdTextServiceGetTextReplyDTO stdTextDTO : stdTextImg.getReplyElements()){
			service.get(STD_TEXT_SERVICE).append({StdTextServiceAppendRequestDTO it->
				it.lineCount = stdTextDTO.getLineCount()
				it.stdTextId = extTextId
				it.textLine = stdTextDTO.getTextLine()
			})
			info("std text: ${stdTextDTO.getLineCount()}")
			info("start line: ${stdTextDTO.getStartLineNo()}")
			info("text line indexed - ${i}: ${stdTextDTO.getTextLineIndexed(i)}")
		}

	}
	/**
	 * read std text to create rollback image of completion comment
	 */
	private void readCompletionCom(WorkOrderDTO workOrder){
		info("readCompletionCom")
		String completionTextId = "CW" + sDistrictCode + workOrder.getPrefix() + workOrder.getNo()
		StdTextServiceGetTextReplyCollectionDTO stdTextReply = service.get(STD_TEXT_SERVICE).getText({StdTextServiceGetTextRequestDTO it->
			it.stdTextId = completionTextId
		})
		completionImg = new StdTextServiceGetTextReplyCollectionDTO()
		completionImg = stdTextReply
	}

	/**
	 * rollback completion comment if the service call fail
	 * @param workOrder
	 */
	private void rollbackCompletion(WorkOrderDTO workOrder){
		info("rollbackExtText")
		String completionTextId = "CW" + sDistrictCode + workOrder.getPrefix() + workOrder.getNo()
		//delete stdText first before rollback
		service.get(STD_TEXT_SERVICE).delete({StdTextServiceDeleteRequestDTO it->
			it.stdTextId = completionTextId
		})

		for(StdTextServiceGetTextReplyDTO compTextReply : completionImg.getReplyElements()){
			service.get(STD_TEXT_SERVICE).append({StdTextServiceAppendRequestDTO it->
				it.lineCount = compTextReply.getLineCount()
				it.stdTextId = completionTextId
				it.textLine = compTextReply.getTextLine()
			})
		}
	}

	private Boolean isValidLine(String line){
		info("isValidLine");

		Boolean returnValue = false;

		if(isValidLength(line)){
			String tempWO = line.substring(0,11);
			info("tempWO : ${tempWO}")
			workOrderNo = new WorkOrderDTO();
			if(tempWO.substring(0, 1).equals("-")){
				bUpdateMode = false;
				returnValue = true;
			}else{
				bUpdateMode = true;
				workOrderNo.prefix = tempWO.substring(0,2)
				workOrderNo.no = tempWO.substring(2)
				if(isValidWorkOrder(workOrderNo, sDistrictCode)){
					info("return true for valid wo")
					returnValue = true;
				}else{
					fieldRef = "Work Order" + ": " + workOrderNo.toString()
					errMsg = "Work Order to be updated not exist in system"
					printErrorMsg()
				}
			}
		}else{
			fieldRef = "Line Length" + ": " + line.length().toString()
			errMsg = "Line length is invalid"
			printErrorMsg()
		}

		return returnValue;
	}

	/**
	 * This function return true if the line has a valid number of character, false if not.
	 * @param line
	 * @return true if the line is has a valid number of character in length, false if not
	 */
	private Boolean isValidLength(String line){
		info("isValidLength")
		Boolean returnValue = false;
		info("length line: ${line.length()}")
		if(line!=null){
			if(line.length()==this.VALID_LENGTH){
				returnValue = true;
			}
		}
		return returnValue;
	}

	/**
	 * This function checks whether the work order exists in Ellipse.
	 * @param workOrder
	 * @return true if the the work order exists, false if not
	 */
	private Boolean isValidWorkOrder(WorkOrderDTO workOrderDTO, String districtCode){
		info("isValidWorkOrder");

		Boolean returnValue = true;
		WorkOrderServiceReadRequiredAttributesDTO woReqAtt = new WorkOrderServiceReadRequiredAttributesDTO()
		woReqAtt.returnWorkOrder = true
		info("work order: ${workOrderDTO.toString()}")
		info("district code: ${districtCode}")
		try{
			WorkOrderServiceReadReplyDTO replyWO = service.get(WORK_ORDER_SERVICE).read({WorkOrderServiceReadRequestDTO it->
				it.requiredAttributes = woReqAtt
				it.districtCode = districtCode
				it.workOrder = workOrderDTO
			})
			info("work order: ${replyWO.workOrder}")

		}catch(com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException e) {
			info("error when read work order : ${e.getMessage()}")
			info("message : ${e.getErrorMessages()[0].getMessage()}")
			info("fieldName : ${e.getErrorMessages()[0].getFieldName()}")
			info("errorcode : ${e.getErrorMessages()[0].getCode()}")
			returnValue = false
		}
		return returnValue;
	}

	/**
	 * validate date format using standard ellipse : ddMMyyyy
	 * @param date : String date from the input
	 * @return true or false
	 */
	private boolean isValidDate(String date){
		info("isValidDate")
		/*
		 * if date less than min date (1 jan 1900) than return false
		 */
		if(date >= MIN_DATE && date.isNumber()){
			SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy")
			try{
				format.setLenient(false)
				format.parse(date)
				return true
			}catch(java.text.ParseException e){
				return false
			}
		}else{
			return false
		}
	}

	/**
	 * This function returns the logical representation of an input
	 * file from the supplied path.
	 * @param path
	 * @return BufferedWriter to be used as an input file.
	 */
	private BufferedReader openInputFile(String path){
		info("openInputFile");
		info ("Opening:"+path)
		try{
			def FileInputStream fileInputStream = new FileInputStream(path)
			def DataInputStream dataInputStream = new DataInputStream(fileInputStream);
			return new BufferedReader(new InputStreamReader(dataInputStream));
		}catch(FileNotFoundException e){
			info("file not found ${e.getMessage()}")
			return null
		}
	}

	/**
	 * convert String to calendar with format ddMMyyyy
	 * @param String sDate
	 * @return Calendar
	 */
	private Calendar stringToCalendar(String sDate){
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy")
		Calendar calDate = Calendar.getInstance()
		sdf.setLenient(false)
		if(sDate?.trim()){
			Date date = sdf.parse(sDate)
			calDate.setTime(date)
		}else{
			calDate = null
		}
		return calDate
	}

	/**
	 * This function takes a string value and a line length, and returns an array of lines. <br/>
	 * Lines are cut on word boundaries, where the word boundary is a space character. <br/>
	 * Spaces are included as the last character of a word, so most lines will actually end with a space. <br/>
	 * This isn't too problematic, but will cause a word to wrap if that space pushes it past the max line length.<br/>
	 * @see http://progcookbook.blogspot.com/2006/02/text-wrapping-function-for-java.html
	 * @param text text to wrap
	 * @param len length of a line
	 * @return array of String from wrapped text
	 */
	public static String[] wrapText(String text, int len) {
		// return empty array for null text
		if (text == null) {
			String[] x = [""]
			return x
		}
		// return text if len is zero or less
		if (len <= 0) {
			String[] x = [text]
			return x
		}
		// return text if less than length
		if (text.length() <= len) {
			String[] x = [text]
			return x
		}
		char[] chars = text.toCharArray()
		Vector<String> lines = new Vector<String>()
		StringBuffer line = new StringBuffer()
		StringBuffer word = new StringBuffer()
		for (int i = 0; i < chars.length; i++) {
			word.append(chars[i])
			if (chars[i] == ' ') {
				if ((line.length() + word.length()) > len) {
					lines.add(line.toString())
					line.delete(0, line.length())
				}
				line.append(word)
				word.delete(0, word.length())
			}
		}
		// handle any extra chars in current word
		if (word.length() > 0) {
			if ((line.length() + word.length()) > len) {
				lines.add(line.toString())
				line.delete(0, line.length())
			}
			line.append(word)
		}
		// handle extra line
		if (line.length() > 0) {
			lines.add(line.toString())
		}
		String[] ret = new String[lines.size()]
		int c = 0 // counter
		for (Enumeration<String> e = lines.elements(); e.hasMoreElements(); c++) {
			ret[c] = e.nextElement()
		}
		return ret
	}

	private void printErrorMsg(){
		info("printErrorMsg")
		if (firstErr){
			reportA = report.open(REPORT_A_NAME)
			reportA.write("${REPORT_A_NAME} Summary Error Report".center(132))
			reportA.writeLine(132,"-")
			reportA.write("Line/Field Ref/ Value".padRight(50) + "Error/ Warning Message")
			reportA.writeLine(132,"-")
			firstErr = false
			bReportAOpen = true;
		}

                reportA.write("Line No:${lLineNo.toString()} ${fieldRef}".padRight(50) + errMsg)
	}

}

/*run script*/
ProcessTrb6m2 process = new ProcessTrb6m2();
process.runBatch(binding);
