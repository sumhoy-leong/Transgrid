/**
 * Script           : Trbsca
 * Title            : Support Cost Allocation
 * Objective        :
 * The SCA batch program will run immediately after the MSB857 batch program.
 * It will process MSF857 Labour Costing standard rate transactions that have been
 * successfully processed by MSB857 indicated by the MSF857-POSTING-STATUS field.
 * It will need to differentiate between records that it may have already successfully processed in a previous
 * run, so that any MSB857/SCA program re-runs do not create unwanted additional transactions. To achieve this,
 * the current MSF857-BATCH-TYPE indicator will be set to .S. for all records successfully processed by this
 * new SCA batch program.
 *
 * Successfully validated MSF857 records will be written to MST903. A request for MSB904 will be written. When
 * MSB904 runs, it will further validate the MS903 records and attempt to generate Journals Interface (MSF903)
 * records as .BPJ. (Batch Journal Voucher Primary) transactions to allow MSB919 to then create the required
 * Ellipse Journal Holding File records (MSF900).
 * The SCA batch program will produce reports listing both errors and successfully processed transactions.
 *
 * Job Dependency
 * Predecessor      : MSB857
 * Successor        : MSB904, MSB919, MSB908
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

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Formatter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.eql.impl.*
import com.mincom.eql.*

import com.mincom.ellipse.edoi.ejb.msf080.MSF080Key
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.eroi.linkage.mss080.MSS080LINK

import com.mincom.enterpriseservice.ellipse.dependant.dto.TransDetailsDTO
import com.mincom.enterpriseservice.ellipse.equipment.EquipmentServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.table.TableServiceReadReplyDTO
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException

import com.mincom.ellipse.edoi.ejb.msf000.MSF000_DC0016Key
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_DC0016Rec
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.edoi.ejb.msf600.MSF600Key
import com.mincom.ellipse.edoi.ejb.msf600.MSF600Rec
import com.mincom.ellipse.edoi.ejb.msf620.MSF620Key
import com.mincom.ellipse.edoi.ejb.msf620.MSF620Rec
import com.mincom.ellipse.edoi.ejb.msf856.MSF856Key
import com.mincom.ellipse.edoi.ejb.msf856.MSF856Rec
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Key
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Rec
import com.mincom.ellipse.edoi.ejb.msf903.MSF903Key
import com.mincom.ellipse.edoi.ejb.msf903.MSF903Rec
import com.mincom.ellipse.edoi.ejb.msf966.MSF966Key
import com.mincom.ellipse.edoi.ejb.msf966.MSF966Rec

import com.mincom.ellipse.edoi.common.exception.EDOIInfrastructureException;
import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException
import com.mincom.ellipse.edoi.common.exception.EDOIDuplicateKeyException


public class ParamsTrbsca {
	//List of Input Parameters
	String paramSca;
}

public class LastLabourMST903BatchNo {
	String sPrefix =""
	String errorMessage = ""
	Integer iLastSeqNo = 000000
	
	public LastLabourMST903BatchNo(String lastSeqNo){
		this.sPrefix = lastSeqNo.substring(0,2)
		this.iLastSeqNo = Integer.parseInt(lastSeqNo.substring(2,8))
	}
	
	public void increaseSeqNo(){
		this.iLastSeqNo++
		if (this.iLastSeqNo >= 999999){
			throw new Exception("Maximum Batch Number is reached: 999999") 
		}
		
	}
	
	public String getLastBatchNo(){
		return sPrefix + iLastSeqNo.toString().padLeft(6,"0")
	}
}

public class ProcessTrbsca extends SuperBatch {

	private final version = 16 // Update this number every push to GitHub

	private class WoDetails {
		// Some WO details
		String WorkOrderType
		String EquipmentNo
		String AccountCode

		public void initWoDetails() {
			WorkOrderType = " "
			EquipmentNo = " "
			AccountCode = " "
		}
	}

	private class LabCostTran {
		String CrAcctCodePart1
		String CrAcctCodePart2
		String DrAcctCodePart1
		String DrAcctCodePart2
		String EquipClassif
		String OncostRateTypeCode
		String ScaKey
		String DistrictCode
		String EmployeeId
		String LabEarnCode
		String BatchType
		String BatchNo
		String Batch903No
		String PostingStatus
		String WorkOrder
		String EquipNo
		String SubledgerKey
		String LabTranDate
		String ProjectNo
		String LabTranDesc
		String ThisPeriod
		String WorkOrderType
		String WorkOrderEquip
		BigDecimal LabRate
		BigDecimal LabTranHours
		BigDecimal TotalLabHours
		BigDecimal TotalOncostValue
		BigDecimal OncostValue
		BigDecimal OncostRate

		public void initLabCostTran() {
			CrAcctCodePart1 = " "
			CrAcctCodePart2 = " "
			DrAcctCodePart1 = " "
			DrAcctCodePart2 = " "
			EquipClassif = " "
			OncostRateTypeCode = " "
			ScaKey = " "
			DistrictCode = " "
			EmployeeId = " "
			LabEarnCode = " "
			BatchType =  " "
			BatchNo = " "
			Batch903No = " "
			PostingStatus = " "
			WorkOrder = " "
			EquipNo = " "
			SubledgerKey = " "
			LabTranDate = " "
			ProjectNo = " "
			LabTranDesc = " "
			ThisPeriod = " "
			WorkOrderType =  " "
			WorkOrderEquip = " "
			LabRate = 0.0
			LabTranHours = 0.0
			TotalLabHours = 0.0
			TotalOncostValue = 0.0
			OncostValue = 0.0
			OncostRate = 0.0
		}
	}

	private ParamsTrbsca batchParams
	private LastLabourMST903BatchNo lastMST903BatchNo = null
	private File oFile
	private BufferedWriter outputFile
	private String fileLocation

	Date dToday = new Date()

	private def Trbscaa // Summary Report
	private def Trbscab // Detail Report
	private def Trbscac // Error Report

	private Integer iMst903Count = 0
	private static final MAX_BATCH_NO = 999999
	private static final MAX_SEQ_NO = 9998
	private static final ERR_WO_TYPE = "WO Type"
	private static final ERR_EQUIP_REF = "SCA table error"
	private static final ERR_EQUIP_CLASS_E0 = "Eqp E0 error"
	private static final ERR_SCA_KEY = "SCA table error"
	private static final ERR_SCA_EQUIP = "Eqp SCA rate"
	private static final ERR_SCA_WO = "WO SCA rate"
	private static final ERR_DR_ACCT_CODE = "Invalid DR Acct Code"
	private static final ERR_CR_ACCT_CODE = "Invalid CR Acct Code"
	private static final MST903_ACCOUNTANT = "0000010200"

	
	private Map <String,Integer> lastBatch903SequenceNo = new HashMap <String,Integer>()
	private Map <String,String> mapBatch903 = new HashMap <String,String>()

	public void runBatch(Binding b) {
		info("runBatch Version : " + version)

		init(b)

		// Request parameters
		batchParams = params.fill(new ParamsTrbsca())
		processBatch()
	}

	/**
	 * Execute the concatenation
	 * @param parameters
	 */
	private void processBatch() {
		info("processBatch")

		
		Boolean bIoError = false
		Boolean foundLastBatchNo = readLastLBatchSeqNo()
		lastMST903BatchNo.increaseSeqNo()
		
		ioReports("open")

		if (!foundLastBatchNo){
			Trbscac.write(lastMST903BatchNo.getErrorMessage().center(132))
			Trbscac.write("Unable to create MST903 file. TRBSCA cannot continue.".center(132))
		}
		
		try {
			// Create output file MST903
			fileLocation = env.workDir.toString() + "/MST903." + getTaskUUID()
			info("MST file: " + fileLocation)

			//LOKEWS Comment
			//oFile = new File(fileLocation)
			//outputFile = new BufferedWriter(new FileWriter(oFile))
		} catch (IOException e) {
			// Error encountered during file creation
			//writeReportCHeader("")
			Trbscac.write("Unable to create MST903 file. TRBSCA cannot continue.".center(132))

			e.printStackTrace()
			info ("##### ERROR: Unable to create output file #####")
			info ("##### PROCESS TERMINATED #####")
			bIoError = true
		}
		
		if (!bIoError && foundLastBatchNo) {
			// Get labor costing transactions from MSF857
			getLabourTransactions()
			// Create a batch request in Ellipse for MSB904
			//LOKEWS TEMP COMMENT OUT
			//if (iMst903Count > 0) {
			//	createNextBatch("MSB904")
			//	createNextBatch("MSB919")
			//	createNextBatch("MSB908")
			//}
			// Finished writing to MST903
			//LOKEWS Comment
			//outputFile.close()
			updateLastLBatchSeqNo()
		}

		ioReports("close")

                //LOKEWS TEST EXCEPTION
		  createNextBatch("MSB904")
		  createNextBatch("MSB919")
		  createNextBatch("MSB908")
                //throw new Exception("LOKEWS TEST ERROR TRBSCA>>>")
	}

	private void updateLastLBatchSeqNo(){
		try {
			MSF010Rec msf010Rec = readTableFile("+SCA","0000")
			
			if (msf010Rec == null){
				throw new Exception("Could not update TableType:+SCA and TableCode:0000 lastBatchNo:${lastMST903BatchNo.getLastBatchNo()}")
			}			
			msf010Rec.setAssocRec(lastMST903BatchNo.getLastBatchNo())
			edoi.update(msf010Rec)
		}catch (EDOIInfrastructureException e){
			info (e.printStackTrace())
			info ("Could not update TableType:+SCA and TableCode:0000 lastBatchNo:${lastMST903BatchNo.getLastBatchNo()}")
		}
		
	}
	
	/**
	 * Create a batch request record in MSF080
	 * @param sProgName
	 */
	private void createNextBatch(String sProgName) {
		info("createNextBatch: ${sProgName}")

		Calendar cal = Calendar.getInstance();
		String todayDate = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
		String todayTime = new SimpleDateFormat("HHmmss").format(cal.getTime());

		Constraint c1 = MSF080Key.progName.equalTo(request.request.getRequestName())
		Constraint c2 = MSF080Rec.uuid.equalTo(this.getUUID())

		Query query = new QueryImpl(MSF080Rec.class).and(c1).and(c2)

		MSF080Rec rec = edoi.firstRow(query)

		//Call MSS080 to create request
		if (sProgName == "MSB908") {
			MSS080LINK mss080lnk = eroi.execute("MSS080",{MSS080LINK mss080lnk->
				mss080lnk.requestDate = todayDate
				mss080lnk.requestTime = todayTime
				mss080lnk.deferDate = todayDate
				mss080lnk.deferTime = todayTime
				mss080lnk.progName = sProgName
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
				mss080lnk.requestParams = "        Y"
				mss080lnk.startOption = rec.getStartOption()
				mss080lnk.copyRqstSw = "N"
				mss080lnk.printerRec = (rec.getPrinter1()?:"").padRight(2) + (rec.getNoOfCopies1()?:"").padLeft(2, "0")
			})

			if (!mss080lnk.getReturnStatus().equals("Y")) {
				info("MSS080 status : " + mss080lnk.getReturnStatus() + mss080lnk.getErrorData().toString() )
			} else {
				info("MSS080 UUID : " + mss080lnk.getTaskUuid() )
			}
		} else {
			MSS080LINK mss080lnk = eroi.execute("MSS080",{MSS080LINK mss080lnk->
				mss080lnk.requestDate = todayDate
				mss080lnk.requestTime = todayTime
				mss080lnk.deferDate = todayDate
				mss080lnk.deferTime = todayTime
				mss080lnk.progName = sProgName
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
				mss080lnk.startOption = rec.getStartOption()
				mss080lnk.copyRqstSw = "N"
				mss080lnk.printerRec = (rec.getPrinter1()?:"").padRight(2) + (rec.getNoOfCopies1()?:"").padLeft(2, "0")
			})

			if (!mss080lnk.getReturnStatus().equals("Y")) {
				info("MSS080 status : " + mss080lnk.getReturnStatus() + mss080lnk.getErrorData().toString() )
			} else {
				info("MSS080 UUID : " + mss080lnk.getTaskUuid() )
				info("MSS080 TASK UUID : " + mss080lnk.getUuid())
			}
		}
	}

	/**
	 * Create, close reports
	 * @param io
	 */
	private void ioReports(def io) {
		info("ioReports")

		switch (io) {
			case "open":
				List <String> headingsA = new ArrayList <String>()
				headingsA = writeReportAHeader()
				Trbscaa = report.open("TRBSCAA", headingsA)

				List <String> headingsB = new ArrayList <String>()
				headingsB = writeReportBHeader()
				Trbscab = report.open("TRBSCAB", headingsB)

				List <String> headingsC = new ArrayList <String>()
				headingsC = writeReportCHeader()
				Trbscac = report.open("TRBSCAC", headingsC)
				break

			case "close":
				Trbscaa.close()
				Trbscab.close()
				Trbscac.close()
				break

			default:
				info("##### INVALID IO OPERATION #####")
				break
		}
	}

	/**
	 * Search for qualifying Labour Costing Transactions from MSF857
	 * batch-type/lab-batch-no not = "S"
	 * posting-status = "PO"
	 */
	private void getLabourTransactions() {
		info("getLabourTransactions")

		Integer iSeqNo = 0
		Boolean bErrorOccurred = false
		String sAction = ""
		String sPrevBatchNo = "FIRST TIME"
		String sErrorMess = ""
		Boolean bFirstPageReportB = true
		Boolean bFirstPageReportC = true

		// Counters and totals for report A in a batch
		Integer iRecReadInBatch = 0            // Records read in a batch
		Integer iRecProcInBatch = 0            // Records successfully processed in a batch
		BigDecimal bdTotProcValueInBatch = 0.0 // Processed value in a batch
		Integer iTotRecsErrorInBatch = 0       // Records in error in a batch

		// Counters and totals for Report A for entire TRBSCA run
		Integer iBatchCount = 0                // Total Batches
		Integer iRecRead = 0                   // Total records read
		Integer iRecProc = 0                   // Total records successfully processed
		BigDecimal bdTotProcValue = 0.0        // Total processed value
		Integer iTotRecsError = 0              // Total records in error

		// Counters and totals for report B
		Integer iTotEmpInBatch = 0             // Total Employees (total detail lines printed not
		// distinct employees) in a batch
		BigDecimal bdTotLabHoursInBatch = 0.0  // Total Labour Hours in a batch
		BigDecimal bdTotScaValueInBatch = 0.0  // Total SCA (BPJ) Value in a batch

		// Counter for report C
		Integer iTotErrorsInBatch = 0          // Total Errors in a batch

		LabCostTran objLabCostTran = new LabCostTran()

		// Get Labour Costing Transaction. Sort by Batch Number and by MSF857 Primary Key
		Constraint c1 = MSF857Rec.postingStatus.equalTo('PO')
		StringConstraint c2 = MSF857Rec.labBatchNo.substring(0,1).notEqualTo("S")
//		StringConstraint c2 = MSF857Rec.labBatchNo.substring(0,1).trim().equalTo('O')
//		StringConstraint c3 = MSF857Rec.labBatchNo.substring(0,1).trim().equalTo('B')
//	    StringConstraint c2 = MSF857Rec.labBatchNo.like("%O20%")
		Query queryMsf857 = new QueryImpl(MSF857Rec.class).and(c1).and(c2)
//				nonIndexSortAscending(MSF857Rec.labBatchNo, MSF857Key.dstrctCode, MSF857Key.employeeId,
//				MSF857Key.labTranDate, MSF857Key.labClassEarn, MSF857Key.labTranSeq)

		// Run query to find labour transactions matching the search criteria
		edoi.search(queryMsf857,9000,{MSF857Rec msf857rec ->

			info("MSF857 Key: " + msf857rec.getPrimaryKey().toString() + " " + msf857rec.getLabBatchNo().toString())
			info(" LabBatchNo : ${msf857rec.getLabBatchNo().substring(0,1)} ") 
			info(" LabBatchNo : ${msf857rec.getLabBatchNo().substring(1,1)} ")

			// Earnings Code must be in the #SCA table (Table Code characters 1 to 3)
			if (checkEarnCode(msf857rec.getPrimaryKey().getLabClassEarn().substring(6))) {

				// Initialize labour costing object for the next MSF857 record
				objLabCostTran.initLabCostTran()
				// Set values to labour costing object from MSF857 record
				objLabCostTran = setLabCost(msf857rec)

				// Batch Number has changed
				if (!sPrevBatchNo.equals(objLabCostTran.getBatchNo())) {
					// Write detail line to Report A for previous batch
					if (iRecProcInBatch > 0 || iTotRecsErrorInBatch > 0) {
						writeReportA(sPrevBatchNo, iRecReadInBatch, iRecProcInBatch,
								bdTotProcValueInBatch, iTotRecsErrorInBatch)
					} else {
						if (!sPrevBatchNo.equals("FIRST TIME")) {
							iBatchCount--                // Exclude this batch from count
							iRecRead -= iRecReadInBatch  // Exclude records in this batch from count
						}
					}
					// Reset Report A totals in a batch
					iRecReadInBatch = 0                              // Records read in a batch
					iRecProcInBatch = 0                              // Records processed in a batch
					bdTotProcValueInBatch = 0.0                      // Processed value in a batch
					iTotRecsErrorInBatch = 0                         // Records in error in a batch

					// Write summary line to Report B for previous batch
					if (iTotEmpInBatch > 0) {
						writeReportBSummary(sPrevBatchNo, iTotEmpInBatch, bdTotLabHoursInBatch,
								bdTotScaValueInBatch)
					}

					// Reset Report B totals in a batch
					iTotEmpInBatch = 0                               // Total Employees in a batch
					bdTotLabHoursInBatch = 0.0                       // Total Labour Hours in a batch
					bdTotScaValueInBatch = 0.0                       // Total SCA (BPJ) Value in a batch

					// Write summary line to Report C for previous batch
					if (iTotErrorsInBatch > 0) {
						writeReportCSummary(sPrevBatchNo, iTotErrorsInBatch)
					}

					// Reset Report C totals in a batch
					iTotErrorsInBatch = 0                            // Total Errors in a batch

					sPrevBatchNo = objLabCostTran.getBatchNo()
								
					iBatchCount++
				}

				// Counters for Report A
				iRecReadInBatch++ // Count of MSF857 recs passing selection criteria within a batch
				iRecRead++        // Count of MSF857 recs passing selection criteria within the entire TRBSCA run

				// Now do all additional validation
				(sAction, sErrorMess, objLabCostTran) = validateLabCostTran(objLabCostTran)

				switch (sAction) {
					case "ERROR":     // error. failed validation
					// Update report A counters
						iTotRecsErrorInBatch++ // MSF857 recs failed validation within a batch
						iTotRecsError++        // MSF857 recs failed validation within entire TRBSCA run

						if (iTotErrorsInBatch == 0) {
							// Start a new page and write Report C for new batch number
							if (!bFirstPageReportC) {
								Trbscac.newPage()
							} else {
								bFirstPageReportC = false
							}
							Trbscac.write("Batch Number: " + objLabCostTran.getBatchNo())
							Trbscac.write(" ")
						}
					// Update report C counter
						iTotErrorsInBatch++    // MSF857 recs failed validation within a batch

						writeReportC(objLabCostTran, sErrorMess)
						break
					case "OK":        // ok. passed validation
						iSeqNo = writeMST(objLabCostTran, iSeqNo) // Write Labour Costing details to MST903
					// Write the batch number as a sub heading if this is the first record
					// to be printed in the detail report for the batch
						if (iRecProcInBatch == 0) {
							// Start a new page and write Report B new batch number
							if (!bFirstPageReportB) {
								Trbscab.newPage()
							} else {
								bFirstPageReportB = false
							}
							Trbscab.write("Batch Number: " + objLabCostTran.getBatchNo())
							Trbscab.write(" ")
						}
						writeReportB(objLabCostTran)     // Write detail to Report B

					// Update report A counters
						iRecProcInBatch++ // Count MSF857 recs passing all validation within a batch
						iRecProc++        // Count MSF857 recs passing all validation within the entire TRBSCA run

					// Update report A totals
						bdTotProcValueInBatch += objLabCostTran.getOncostValue() // Total oncost value within a batch
						bdTotProcValue += objLabCostTran.getOncostValue()        // Total oncost value within the entire TRBSCA run

					// Update report B counters
						iTotEmpInBatch++ // Count MSF857 recs written to MST903 and the detail report B

					// Update report B totals
						bdTotLabHoursInBatch += objLabCostTran.getLabTranHours() // Total labour hours within a batch
						bdTotScaValueInBatch += objLabCostTran.getOncostValue()  // Total oncost value within a batch

					// Update MSF857 record with a batch type of "S" so it is not picked up the
					// next time TRBSCA is run
						try {
							msf857rec.setLabBatchNo("S" + objLabCostTran.getBatchNo())
							edoi.update(msf857rec)
						} catch(EDOIObjectNotFoundException e) {
							info("##### ERROR: Unable to update Batch Type to S #####")
						}
						break
					case "SKIP":      // skip. failed validation but not an error
					// Update MSF857 record with a batch type of "S" so it is not picked up the
					// next time TRBSCA is run
						try {
							msf857rec.setLabBatchNo("S" + objLabCostTran.getBatchNo())
							edoi.update(msf857rec)
						} catch(EDOIObjectNotFoundException e) {
							info("##### ERROR: Unable to update Batch Type to S #####")
						}
						break
					default:          // do nothing
						info("ERROR: ####THIS SHOULD NOT HAPPEN. It can only be OK, ERROR or SKIP!!!####")
						info("MSF857 Key: " + msf857rec.getPrimaryKey().toString() + " " + msf857rec.getLabBatchNo().toString())
						break
				}
			} else {
				//info("Skip: Earnings Code not in #SCA")
				// Update MSF857 record with a batch type of "S" so it is not picked up the
				// next time TRBSCA is run
				try {
					msf857rec.setLabBatchNo("S" +  msf857rec.getLabBatchNo().substring(1))
					edoi.update(msf857rec)
				} catch(EDOIObjectNotFoundException e) {
					info("##### ERROR: Unable to update Batch Type to S #####")
				}
			}
		})

		info("##### No more MSF857 records #####")

		if (iRecRead > 0) {
			if (iRecProcInBatch > 0 || iTotRecsErrorInBatch > 0) {
				// Write the last detail line to Report A
				writeReportA(objLabCostTran.getBatchNo(), iRecReadInBatch, iRecProcInBatch,
						bdTotProcValueInBatch, iTotRecsErrorInBatch)
			} else {
				if (!sPrevBatchNo.equals("FIRST TIME")) {
					iBatchCount--                // Exclude this batch from count
					iRecRead -= iRecReadInBatch  // Exclude records in this batch from count
				}
			}

			// Write summary line to Report A
			writeReportASummary(iBatchCount, iRecRead, iRecProc, bdTotProcValue, iTotRecsError)
			// Write summary line to Report B
			if (iTotEmpInBatch > 0) {
				writeReportBSummary(objLabCostTran.getBatchNo(), iTotEmpInBatch, bdTotLabHoursInBatch,
						bdTotScaValueInBatch)
			}
			// Write summary line to Report C
			if (iTotErrorsInBatch > 0) {
				writeReportCSummary(objLabCostTran.getBatchNo(), iTotErrorsInBatch)
			}
		} else {
			// Write summary line to Report A
			Trbscaa.write("*** No records found ***".center(132))
			writeReportASummary(iBatchCount, iRecRead, iRecProc, bdTotProcValue, iTotRecsError)
			// Write summary line to Report B
			Trbscab.write("*** No records found ***".center(132))
			writeReportBSummary("", iTotEmpInBatch, bdTotLabHoursInBatch, bdTotScaValueInBatch)
			// Write summary line to Report C
			Trbscac.write("*** No records found ***".center(132))
			writeReportCSummary("", iTotErrorsInBatch)
		}
	}

	private def checkLastSeqNo(Integer iSeqNo, String sBatchNo){
		
		boolean bMst903BatchIncrease = false
		
		if (iSeqNo >= MAX_SEQ_NO){
			lastMST903BatchNo.increaseSeqNo()
			iSeqNo = 0
			mapBatch903.put(sBatchNo,lastMST903BatchNo.getLastBatchNo())
			bMst903BatchIncrease = true
		}
		
		return [iSeqNo,bMst903BatchIncrease]
	}
	/**
	 * Set values to labour costing object from MSF857 record
	 * @param objLabCostTran
	 * @return
	 */
	private LabCostTran setLabCost(MSF857Rec msf857rec) {
		info("setLabCost")

		String sCurrLabCostPeriod = commarea.getProperty("LabourCostingCp")
		String sPeriodYR = sCurrLabCostPeriod.substring(0,2)
		String sPeriodMN = sCurrLabCostPeriod.substring(2,4)
		String sPeriodCC = "20"
		if (sPeriodYR.toInteger() > 25) {
			sPeriodCC = "19"
		}
		String sThisPeriod = sPeriodCC + sPeriodYR + sPeriodMN

		// Details from the WO
		WoDetails workOrderDetails = new WoDetails()
		workOrderDetails.initWoDetails()
		workOrderDetails = validateWorkOrder(msf857rec.getPrimaryKey().getDstrctCode(), msf857rec.getWorkOrder())

		// Details from the Labour Costing Transaction
		LabCostTran objLabCostTran = new LabCostTran()
		objLabCostTran.with {
			initLabCostTran()
			DistrictCode = msf857rec.getPrimaryKey().getDstrctCode()
			EmployeeId = msf857rec.getPrimaryKey().getEmployeeId()
			LabEarnCode = msf857rec.getPrimaryKey().getLabClassEarn().substring(6)
			BatchType =  msf857rec.getLabBatchNo().substring(0,1)
			BatchNo = msf857rec.getLabBatchNo().substring(1)
						
			Batch903No = lastMST903BatchNo.getLastBatchNo()
			
			PostingStatus = msf857rec.getPostingStatus()
			WorkOrder = msf857rec.getWorkOrder()
			EquipNo = msf857rec.getEquipNo()
			SubledgerKey = msf857rec.getSubledgerType() + msf857rec.getSubledgerAcct()
			LabTranDate = new SimpleDateFormat("yyyyMMdd").parse(msf857rec.getPrimaryKey().getLabTranDate()).
					format("dd/MM/yyyy")
			ProjectNo = msf857rec.getProjectNo()
			LabTranDesc = msf857rec.getLabTranDesc()
			if (msf857rec.getEmpHomeAcct().size() >= 9) {
				CrAcctCodePart1 = msf857rec.getEmpHomeAcct().substring(0,6) + "055"
			} else {
				CrAcctCodePart1 = "         "
			}
			DrAcctCodePart1 = workOrderDetails.getAccountCode()
			ThisPeriod = sThisPeriod
			WorkOrderType =  workOrderDetails.getWorkOrderType()
			WorkOrderEquip = workOrderDetails.getEquipmentNo()
			LabRate = msf857rec.getLabRate()
			LabTranHours = msf857rec.getLabTranHours()
			//LabClassEarn = msf857rec.getPrimaryKey().getLabClassEarn()
			//LabTranSeq = msf857rec.getPrimaryKey().getLabTranSeq()
		}
		return objLabCostTran
	}

	/**
	 * Perform all additional validation
	 * @param objLabCostTran
	 * @return objLabCostTran
	 */
	private def validateLabCostTran(LabCostTran objLabCostTran) {
		info("validateLabCostTran")

		String sAction = "OK"
		String sErrorMess = ""

		objLabCostTran.with {
			// WO Type is missing. Set error.
			if (getWorkOrderType().equals("") || getWorkOrderType().equals(null)) {
				//info("ERROR: WO Type is missing.")
				sAction = "ERROR"
				sErrorMess = ERR_WO_TYPE
			} else {
				// Attempt to get the Oncost Rate Type Code from #SCA with Earnings Code + WO Type
				ScaKey = getLabEarnCode().trim() + getWorkOrderType().trim()
				OncostRateTypeCode = getOCRTCode(getScaKey())

				// Oncost Rate Type Code was not found
				if (getOncostRateTypeCode().equals("") || getOncostRateTypeCode().equals(null)) {
					if (getWorkOrderEquip().equals("") || getWorkOrderEquip().equals(null)) {
						// There is no Equipment on the WO to obtain the third component of the #SCA key.
						//info("Skip: There is no Equipment on the WO to obtain the third component of the #SCA key.")
						sAction = "SKIP"
					} else {
						// Get the Equipment Classification E0 for the Equipment on the WO (MSF620),
						// not the Equip on Labour Transaction record (MSF857).
						EquipClassif = getEquipClassif(getWorkOrderEquip())
						if (getEquipClassif().equals("") || getEquipClassif().equals(null)) {
							// Equipment Classification E0 was empty
							//info("Skip: Equipment Classification E0 was empty")
							sAction = "SKIP"
						} else {
							// Try getting the Oncost Rate Type Code from #SCA with the Earnings Code +
							// WO Type + Equipment Classification E0
							ScaKey = getScaKey().trim() + getEquipClassif().trim()
							OncostRateTypeCode = getOCRTCode(getScaKey())

							if (getOncostRateTypeCode().equals("") || getOncostRateTypeCode().equals(null)) {
								// Cannot find Oncost Rate Type Code with Earnings Code + WO Type +
								// Equip Classif E0. Skip the Labour Transaction.
								//info("Skip: Equip Classif E0. Skip the Labour Transaction.")
								sAction = "SKIP"
							}
						}
					}
				}

				// At this point we should have the required #SCA key
				if (sAction.equals("OK")) {
					// Find Oncost Rates records and calculate Oncost Value
					(OncostValue, OncostRate, DrAcctCodePart2, CrAcctCodePart2) =
							calcOnCostVal(getDistrictCode(), getLabRate(), getLabTranHours(), getOncostRateTypeCode())
					if (getOncostValue() == 0) {
						// No Oncost Rates (MSF856) found. Set error.
						if (getScaKey().size() > 5) {
							// Used Earnings Code + WO Type + E0 as #SCA key
							sErrorMess = ERR_SCA_EQUIP
						} else {
							// Used Earnings Code + WO Type as #SCA key
							sErrorMess = ERR_SCA_WO
						}
						//info("Error: No Oncost Rates found in MSF856. Oncost value is ZERO.")
						sAction = "SKIP"
					} else {
						// Validate CR and DR Account Codes
						String sDrAccountCode = getDrAcctCodePart1() + getDrAcctCodePart2()
						String sCrAccountCode = getCrAcctCodePart1() + getCrAcctCodePart2()

						Boolean bDrIsValid = validateAccountCode(getDistrictCode(), sDrAccountCode)
						Boolean bCrIsValid = validateAccountCode(getDistrictCode(), sCrAccountCode)

						if (!bDrIsValid || !bCrIsValid) {
							if (!bDrIsValid) {
								// DR Account Code is invalid. Set error.
								//info("Error: DR Acct Code is invalid.")
								sErrorMess = ERR_DR_ACCT_CODE
							} else {
								// CR Account Code is invalid. Set error.
								//info("Error: CR Acct Code is invalid.")
								sErrorMess = ERR_CR_ACCT_CODE
							}
							sAction = "ERROR"
						}
					}
				}
			}
		}
		return [
			sAction,
			sErrorMess,
			objLabCostTran
		]
	}

	/**
	 * Validate Account Code
	 * @param sDistrictCode
	 * @param sAccountCode
	 * @return bValidAccountCode
	 */
	private Boolean validateAccountCode(String sDistrictCode, String sAccountCode) {
		info("validateAccountCode: ${sDistrictCode} ${sAccountCode}")

		Boolean bValidAccountCode = true

		// Validate Account Code
		try {
			MSF966Key msf966key = new MSF966Key()
			msf966key.setDstrctCode(sDistrictCode)
			msf966key.setAccountCode(sAccountCode)
			MSF966Rec msf966rec = edoi.findByPrimaryKey(msf966key)

		} catch(EDOIObjectNotFoundException e) {
			info("##### ERROR: Invalid Account Code : ${sAccountCode} #####")
			bValidAccountCode = false
		}
		return bValidAccountCode
	}

	/**
	 * The earnings Code must be in the #SCA table in Table Code"s characters 1 to 3
	 * @param sEarnCode
	 * @return Boolean
	 */
	private Boolean checkEarnCode(String sEarnCode) {
		info("checkEarnCode: ${sEarnCode}")

		Constraint c3 = MSF010Key.tableType.equalTo("#SCA")
		StringConstraint c4 = MSF010Key.tableCode.substring(1,3).like(sEarnCode)
		Query queryMsf010 = new QueryImpl(MSF010Rec.class).and(c3).and(c4)
		MSF010Rec msf010Rec = edoi.firstRow(queryMsf010)

		if (!msf010Rec.equals("") && !msf010Rec.equals(null)) {
			String sTableCode = msf010Rec.getPrimaryKey().getTableCode()
			return true
		} else {
			return false
		}
	}

	private Boolean readLastLBatchSeqNo(){
		String lastBatchSeqNo = ""
		MSF010Rec msf010Rec = readTableFile("+SCA","0000")
		
		if (!msf010Rec.equals("") && !msf010Rec.equals(null)) {
			
			lastBatchSeqNo = msf010Rec.getAssocRec().substring(0, 8)
			Integer iLastBatchSeqNo = Integer.parseInt(lastBatchSeqNo.substring(2,8))
			
			if (iLastBatchSeqNo >= MAX_BATCH_NO){
				//Maximum Batch No is reached please reset
				lastMST903BatchNo = new LastLabourMST903BatchNo("XX999999")
				lastMST903BatchNo.setErrorMessage("Maximum Batch Number has been reached: ${lastBatchSeqNo}")
				return false 
			}else{
				lastMST903BatchNo = new LastLabourMST903BatchNo(lastBatchSeqNo)
				return true
			}
		} else {
			   lastMST903BatchNo = new LastLabourMST903BatchNo("XX999999")
		       lastMST903BatchNo.setErrorMessage("Could not found TableType:+SCA, TableCode:0000")
			return false
		}
	}

	private MSF010Rec readTableFile(String tableType, String tableCode) {
		Constraint c1 = MSF010Key.tableType.equalTo(tableType)
		Constraint c2 = MSF010Key.tableCode.equalTo(tableCode)
		Query queryMsf010 = new QueryImpl(MSF010Rec.class).and(c1).and(c2)
		MSF010Rec msf010Rec = edoi.firstRow(queryMsf010)
		return msf010Rec
	}
	
	
	/**
	 * Calculate the Oncost Value
	 * @param dstrctcode
	 * @param labrate
	 * @param labtranhours
	 * @param sOncCostRate
	 * @return bdOncostVal, bdOncostRate
	 */
	private def calcOnCostVal(String sDistrictCode, BigDecimal bdLabRate,
			BigDecimal bdLabTranHours, String sOncCostRateType) {
		info("calcOnCostVal")

		String sDrAcctCodePart2 = ""
		String sCrAcctCodePart2 = ""
		BigDecimal bdOncostVal = 0.0
		BigDecimal bdOncostRate = 1.0
		Integer recCount = 0

		try {
			MSF856Key msf856key = new MSF856Key()
			Constraint c1 = MSF856Key.dstrctCode.equalTo(sDistrictCode)
			Constraint c2 = MSF856Key.oncostRateTy.equalTo(sOncCostRateType)
			Query query = new QueryImpl(MSF856Rec.class).and(c1).and(c2).orderBy(MSF856Rec.msf856Key)

			MSF856Key msf856keyDisp = new MSF856Key()
			edoi.search(query,1000,{MSF856Rec msf856rec ->
				sDrAcctCodePart2 = msf856rec.getDrExpElement().substring(0,3)
				sCrAcctCodePart2 = msf856rec.getCrExpElement().substring(0,3)
				bdOncostRate = bdOncostRate *  msf856rec.getOncostRate()
				recCount++
			})
		} catch(EDOIObjectNotFoundException e) {
			info("##### ERROR: Oncost Rates not found #####")
		}

		if (recCount == 0) {
			return [
				0,
				0,
				sDrAcctCodePart2,
				sCrAcctCodePart2
			]
		} else {
			bdOncostRate = bdOncostRate/100
			bdOncostVal = bdLabRate * bdLabTranHours * bdOncostRate
			bdOncostVal = Math.round(bdOncostVal * 100)/100
			return [
				bdOncostVal,
				bdOncostRate,
				sDrAcctCodePart2,
				sCrAcctCodePart2
			]
		}
	}

	/**
	 * Get the Equipment Classification E0 for the given Equipment
	 * @param sEquipNo
	 * @return sEquipClassif
	 */
	private String getEquipClassif(String sEquipNo) {
		info ("getEquipClassif: ${sEquipNo}")

		String sEquipClassif = ""

		try {
			MSF600Rec msf600rec = edoi.findByPrimaryKey(new MSF600Key(sEquipNo))

			sEquipClassif = msf600rec.getEquipClassifx1().trim()
		} catch(EDOIObjectNotFoundException e) {
			info("##### ERROR: Equipment not found : ${sEquipNo} #####")
		}
		return sEquipClassif
	}

	/**
	 * Validate the WO and return some other details
	 * @param sDistrictCode
	 * @param sWorkOrder
	 * @return woDets
	 */
	private WoDetails validateWorkOrder(String sDistrictCode, String sWorkOrder) {
		info("validateWorkOrder: ${sDistrictCode} ${sWorkOrder}")

		WoDetails woDets = new WoDetails()
		try {
			MSF620Rec msf620rec = edoi.findByPrimaryKey(new MSF620Key(sDistrictCode, sWorkOrder))

			woDets.with {
				initWoDetails()
				setEquipmentNo(msf620rec.getEquipNo())
				setWorkOrderType(msf620rec.getWoType())
				setAccountCode(msf620rec.getDstrctAcctCode().substring(4,13))
			}
		} catch(EDOIObjectNotFoundException e) {
			info("##### ERROR: Work Order not found : ${sDistrictCode}, ${sWorkOrder} #####")
		}
		return woDets
	}

	/**
	 * Get the Oncost Rate Type Code
	 * @param sLabEarnCode
	 * @param sWorkOrderType
	 * @param sEquipNo
	 * @return sOncostRateTypeCode
	 */
	private String getOCRTCode(String sScaTableCode) {
		info("getOCRTCode: ${sScaTableCode}")

		String sOncostRateTypeCode = ""

		try {
			TableServiceReadReplyDTO tableEmlReply = service.get("Table").read({
				it.tableType = "#SCA"
				it.tableCode = sScaTableCode})
			sOncostRateTypeCode = tableEmlReply.getAssociatedRecord().trim()
		} catch (EnterpriseServiceOperationException e) {
			info("##### ERROR: #SCA Entry not found using ${sScaTableCode} #####")
			//listErrors(e)
		}
		return sOncostRateTypeCode
	}

	/**
	 * Report A header
	 */
	private List writeReportAHeader() {
		info("writeReportAHeader")

		List <String> headings = new ArrayList <String>()
		headings.add("SCA - Support Costs Allocation - Summary Report".center(132))

		return headings
	}

	/**
	 * Report B header
	 */
	private List writeReportBHeader() {
		info("writeReportBHeader")

		List <String> headings = new ArrayList <String>()
		headings.add("SCA - Support Costs Allocation - Detail Work Order Transaction Report".center(132))
		headings.add(String.format("%132s"," ").replace(" ", "-"))
		headings.add("                                                           Derived                                          SCA")
		headings.add("                        #SCA                                 SCA                                           Oncost")
		headings.add("Employee    Tran Date   Match    WO Number  Equip No       Acct Code       Lab Hours            Lab Rate   Rate(%)  SCA (BPJ) Value")

		return headings
	}

	/**
	 * Report C header
	 */
	private List writeReportCHeader() {
		info("writeReportCHeader")

		List <String> headings = new ArrayList <String>()
		headings.add("SCA - Support Costs Allocation - Transaction Error Report".center(132))
		headings.add(String.format("%132s"," ").replace(" ", "-"))
		headings.add("                        Derived    Derived     SCA")
		headings.add("                          SCA        SCA      Oncost")
		headings.add("Employee    Tran Date    Value     Acc Code    Rate  WO Number  Equip No         Lab Hours          Lab Rate  Error Details")

		return headings
	}

	/**
	 * Write detail line to Report A
	 * @param sBatchNo
	 * @param iRecReadInBatch
	 * @param iRecProcInBatch
	 * @param bdTotProcValueInBatch
	 * @param iTotRecsErrorInBatch
	 */
	private void writeReportA(String sBatchNo, Integer iRecReadInBatch, Integer iRecProcInBatch,
			BigDecimal bdTotProcValueInBatch, Integer iTotRecsErrorInBatch) {
		info("writeReportA")

		DecimalFormat df = new DecimalFormat("0.00")

		//          1         2         3         4         5         6         7         8         9        10        11        12        13
		// 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
		// Batch No: xxxxxxxx  Records read: xxxxx  Records processed: xxxxx   Processed records value: zzz,zzz,zz9.99  Records in error: xxxxx
		Trbscaa.write("Batch No:" + sBatchNo.padLeft(9) +
				"  Records read:" + iRecReadInBatch.toString().padLeft(6) +
				"  Records processed:" + iRecProcInBatch.toString().padLeft(7) +
				"  Processed records value:" + df.format(bdTotProcValueInBatch).toString().padLeft(15) +
				"  Records in error:" + iTotRecsErrorInBatch.toString().padLeft(6))
	}

	/**
	 * Write detail line to Report B
	 * @param objLabCostTran
	 */
	private void writeReportB(LabCostTran objLabCostTran) {
		info("writeReportB")

		DecimalFormat df = new DecimalFormat("0.00")

		objLabCostTran.with {
			//          1         2         3         4         5         6         7         8         9        10        11        12        13
			// 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
			// xxxxxxxxxx  dd/mm/yyyy  xxxxxxx  xxxxxxxx   xxxxxxxxxxxx  999999999999  zzzzzzzz9.99  zzzzzzzzzz9.9999    z9.99   zzz,zzz,zz9.99

			// Debit detail line
			Trbscab.write(getEmployeeId().padRight(12) + getLabTranDate().padRight(12) + getScaKey().padRight(9) +
					getWorkOrder().padRight(11) + getEquipNo().padRight(14) +
					(getDrAcctCodePart1() + getDrAcctCodePart2()).padRight(14) +
					df.format(getLabTranHours()).toString().padLeft(12) +
					"  " + df.format(getLabRate()).toString().padLeft(18) +
					df.format(getOncostRate()).toString().padLeft(9) +
					df.format(getOncostValue()).toString().padLeft(18))

			// Credit detail line
			Trbscab.write(getEmployeeId().padRight(12) + getLabTranDate().padRight(12) + getScaKey().padRight(9) +
					" ".padRight(11) + " ".padRight(14) +
					(getCrAcctCodePart1() + getCrAcctCodePart2()).padRight(14) +
					df.format(getLabTranHours()).toString().padLeft(12) +
					"  " + df.format(getLabRate()).toString().padLeft(18) +
					df.format(getOncostRate()).toString().padLeft(9) +
					df.format(getOncostValue() * -1).toString().padLeft(18))
		}
	}

	/**
	 * Write detail line to Report C
	 * @param objLabCostTran
	 * @param errorMessage
	 */
	private void writeReportC(LabCostTran objLabCostTran, String errorMessage) {
		info("writeReportC")

		//Determine Account Code to use
		String sAccountCode = " "
		if (errorMessage.trim() == ERR_DR_ACCT_CODE) {
			sAccountCode = objLabCostTran.getDrAcctCodePart1() + objLabCostTran.getDrAcctCodePart2()
		}
		if (errorMessage.trim() == ERR_CR_ACCT_CODE) {
			sAccountCode = objLabCostTran.getCrAcctCodePart1() + objLabCostTran.getCrAcctCodePart2()
		}

		DecimalFormat df = new DecimalFormat("0.00")

		objLabCostTran.with {
			//          1         2         3         4         5         6         7         8         9        10        11        12        13
			// 123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012
			// xxxxxxxxxx  dd/mm/yyyy  xxxxxxx  xxxxxxxxxxxx   xx   xxxxxxxx   xxxxxxxxxxxx  zzzzzzzz9.99  zzzzzzzzzz9.9999  xxxxxxxxxxxxxxxxxxxxxx
			Trbscac.write(getEmployeeId().padRight(12) + getLabTranDate().padRight(12) + getScaKey().padRight(9) +
					sAccountCode.padRight(15) + getOncostRateTypeCode().padRight(5) + getWorkOrder().padRight(11) +
					getEquipNo().padRight(14) + df.format(getLabTranHours()).toString().padLeft(12) +
					df.format(getLabRate()).toString().padLeft(18) + "  " + errorMessage)
		}
	}

	/**
	 * Passed all validation write MSF857 details to MST903
	 * @param objLabCostTran
	 * @return None
	 */
	private Integer writeMST(LabCostTran objLabCostTran, Integer iSeqNo) {
		info("writeMST")

		DecimalFormat df = new DecimalFormat("0.00")
		boolean bMst903BatchIncrease = false
		
		//Check the seq number where it must not greater than 9998
		(iSeqNo,bMst903BatchIncrease) = checkLastSeqNo(iSeqNo,objLabCostTran.getBatchNo())
		// update Batch903No in case there is an increment.
		
		if (bMst903BatchIncrease){
			objLabCostTran.setBatch903No(mapBatch903.get(objLabCostTran.getBatchNo()))
		}
		
		
		objLabCostTran.with {
			iSeqNo++

			// Write DR MST903 record
			//LOKEWS Comment
			/*
			outputFile.write(getBatch903No().padRight(8) +                       // MST903-BATCH-NO-T
					iSeqNo.toString().padLeft(4) +                   // MST903-SEQ-NO-T
					getDistrictCode().padRight(4) +                  // MST903-DSTRCT-CODE-T
					"MJV" +                                          // MST903-TRAN-TYPE-T
					(getDrAcctCodePart1() +
					getDrAcctCodePart2()).padRight(24) +         // MST903-ACCOUNT-CODE-T
					getSubledgerKey().padRight(12) +                 // MST903-SUBLEDGER-KEY-T
					getThisPeriod().padRight(6) +                    // MST903-FULL-ACCT-PER
					df.format(getOncostValue()).toString().padLeft(17) +      // MST903-TRAN-AMOUNT-T
					"0.00".padLeft(17) +                             // MST903-MEMO-AMOUNT-T
					"0.00".padLeft(17) +                             // MST903-TRAN-AMOUNT-S-T
					"AUD".padRight(4) +                              // MST903-CURRENCY-TYPE-T
					(getLabTranDate().substring(6,10) +
					getLabTranDate().substring(3,5) +
					getLabTranDate().substring(0,2)) +           // MST903-TRAN-DATE-T
					" ".padRight(18) +                               // MST903-REL-ENT-CODE-T
					getWorkOrder().padRight(8) +                     // MST903-WORK-ORDER-M-T
					getProjectNo().padRight(8) +                     // MST903-PROJECT-NO-M-T
					getEquipNo().padRight(12) +                      // MST903-EQUIP-NO-M-T
					" ".padRight(10) +                               // MST903-MANJNL-VCHR-M-T
					(getBatch903No().padRight(8) + iSeqNo.toString().padLeft(4) + "L" +
					 getBatchNo().padRight(8) + getWorkOrder() +
					getEmployeeId()).padRight(40) +              // MST903-JOURNAL-DESC-M-T
					" ".padRight(8) +                                // MST903-DOCUMENT-REF-M-T
					"N" +                                            // MST903-AUTO-JNL-FLG-M-T
					"  " +                                           // MST903-STAT-TYPE-M-T
					" ".toString().padLeft(15) +                     // MST903-QTY-AMOUNT-M-T
					MST903_ACCOUNTANT.toString().padRight(10) +      // MST903-ACCOUNTANT-M-T
					" ".padRight(8) +                                // MST903-STND-JNL-NO-M-T
					"N" +                                            // MST903-REVERSAL-IND-M-T
					"N" +                                            // MST903-INT-DIST-IND-M-T
					"N" +                                            // MST903-FOR-CURR-IND-M-T
					" ".padRight(24) +                               // MST903-EXPENSE-ELE-M-T
					getLabTranDesc().padRight(40) +                  // MST903-JNL-DESC-M-T
					"  " +                                           // MST903-JNL-TYPE-M-T
					"N" + "\n")                                      // MST903-AUTO-SUS-M-T
			*/
			iSeqNo++

			// Write CR to MST903 record
			//LOKEWS COMMENT
			/*
			outputFile.write(getBatch903No().padRight(8) +                       // MST903-BATCH-NO-T
					iSeqNo.toString().padLeft(4) +                   // MST903-SEQ-NO-T
					getDistrictCode().padRight(4) +                  // MST903-DSTRCT-CODE-T
					"MJV" +                                          // MST903-TRAN-TYPE-T
					(getCrAcctCodePart1() +
					getCrAcctCodePart2()).padRight(24) +         // MST903-ACCOUNT-CODE-T
					getSubledgerKey().padRight(12) +                 // MST903-SUBLEDGER-KEY-T
					getThisPeriod().padRight(6) +                    // MST903-FULL-ACCT-PER
					df.format(getOncostValue() * -1).toString().padLeft(17) + // MST903-TRAN-AMOUNT-T
					"0.00".padLeft(17) +                             // MST903-MEMO-AMOUNT-T
					"0.00".padLeft(17) +                             // MST903-TRAN-AMOUNT-S-T
					"AUD".padRight(4) +                              // MST903-CURRENCY-TYPE-T
					(getLabTranDate().substring(6,10) +
					getLabTranDate().substring(3,5) +
					getLabTranDate().substring(0,2)) +           // MST903-TRAN-DATE-T
					" ".padRight(18) +                               // MST903-REL-ENT-CODE-T
					" ".padRight(8) +                                // MST903-WORK-ORDER-M-T
					//getProjectNo().padRight(8) +                     // MST903-PROJECT-NO-M-T
					//getEquipNo().padRight(12) +                      // MST903-EQUIP-NO-M-T
					" ".padRight(8) +                                // MST903-PROJECT-NO-M-T
					" ".padRight(12) +                               // MST903-EQUIP-NO-M-T
					" ".padRight(10) +                               // MST903-MANJNL-VCHR-M-T
					(getBatch903No().padRight(8) + iSeqNo.toString().padLeft(4) + "L" +
					 getBatchNo().padRight(8) + getWorkOrder() +
					getEmployeeId()).padRight(40) +              // MST903-JOURNAL-DESC-M-T
					" ".padRight(8) +                                // MST903-DOCUMENT-REF-M-T
					"N" +                                            // MST903-AUTO-JNL-FLG-M-T
					"  " +                                           // MST903-STAT-TYPE-M-T
					" ".toString().padLeft(15) +                     // MST903-QTY-AMOUNT-M-T
					MST903_ACCOUNTANT.toString().padRight(10) +      // MST903-ACCOUNTANT-M-T
					" ".padRight(8) +                                // MST903-STND-JNL-NO-M-T
					"N" +                                            // MST903-REVERSAL-IND-M-T
					"N" +                                            // MST903-INT-DIST-IND-M-T
					"N" +                                            // MST903-FOR-CURR-IND-M-T
					" ".padRight(24) +                               // MST903-EXPENSE-ELE-M-T
					getLabTranDesc().padRight(40) +                  // MST903-JNL-DESC-M-T
					"  " +                                           // MST903-JNL-TYPE-M-T
					"N" + "\n")                                      // MST903-AUTO-SUS-M-T
			*/
		}
		iMst903Count++
		
		lastBatch903SequenceNo.put(objLabCostTran.getBatch903No(),iSeqNo)
		return iSeqNo
	}

	/**
	 * Write Report A summary
	 * @param iBatchCount
	 * @param iRecRead
	 * @param iRecProc
	 * @param bdTotProcValue
	 * @param iTotRecsError
	 */
	private void writeReportASummary(Integer iBatchCount, Integer iRecRead, Integer iRecProc,
			BigDecimal bdTotProcValue, Integer iTotRecsError) {
		info("writeReportASummary")

		DecimalFormat df = new DecimalFormat("0.00")

		Trbscaa.write(" ")
		Trbscaa.write(String.format("%132s"," ").replace(" ", "-"))
		// Batches:       xxx  Records read: xxxxx  Records processed: xxxxx  Processed value:         $$$,$$$,$$9.99  Records in error: xxxxx
		Trbscaa.write("Batches:" + iBatchCount.toString().padLeft(10) +
				"  Records read:" + iRecRead.toString().padLeft(6) +
				"  Records processed:" + iRecProc.toString().padLeft(7) +
				"  Processed value:" + df.format(bdTotProcValue).toString().padLeft(23) +
				"  Records in error:" + iTotRecsError.toString().padLeft(6))
		Trbscaa.write(" ")
	}

	/**
	 * Write Report B summary
	 * @param sBatchNo
	 * @param iTotEmpInBatch
	 * @param bdTotLabHoursInBatch
	 * @param bdTotScaValueInBatch
	 */
	private void writeReportBSummary(String sBatchNo, Integer iTotEmpInBatch, BigDecimal bdTotLabHoursInBatch,
			BigDecimal bdTotScaValueInBatch) {
		info("writeReportBSummary")

		DecimalFormat df = new DecimalFormat("0.00")

		Trbscab.write(" ")
		Trbscab.write(String.format("%132s"," ").replace(" ", "-"))
		Trbscab.write("Batch Number: " + sBatchNo)
		// Total Employees: xxxxx                               Total Lab Hours:   zzzzzzzz9.99     Total SCA (BPJ) Value:   zzz,zzz,zz9.99
		Trbscab.write("Total Employees: " + iTotEmpInBatch.toString().padRight(36) +
				"Total Lab Hours:   " + df.format(bdTotLabHoursInBatch).toString().padLeft(12) + "        " +
				"Total SCA (BPJ) Value:   " + df.format(bdTotScaValueInBatch).toString().padLeft(14))
		Trbscab.write(" ")
	}

	/**
	 * Write Report C summary
	 * @param sBatchNo
	 * @param iTotEmpInBatch
	 * @param bdTotLabHoursInBatch
	 * @param bdTotScaValueInBatch
	 */
	private void writeReportCSummary(String sBatchNo, Integer iTotErrorsInBatch) {
		info("writeReportCSummary")

		Trbscac.write(" ")
		Trbscac.write(String.format("%132s"," ").replace(" ", "-"))
		// Batch Number: xxxxxxxx  Total Errors: xxxxxx
		Trbscac.write("Batch Number: " + sBatchNo.padRight(10) + "Total Errors: " + iTotErrorsInBatch.toString())
		Trbscac.write(" ")
	}

	/**
	 * List all errors encountered to the log after running a web service
	 * @param e Error Object
	 * @return None
	 */
	private void listErrors(EnterpriseServiceOperationException e) {
		info("listErrors")

		List <ErrorMessageDTO> listError = e.getErrorMessages()
		listError.each{ErrorMessageDTO errorDTO ->
			info("Error Code: " + errorDTO.getCode())
			info("Error Message: " + errorDTO.getMessage())
			info("Error Fields: " + errorDTO.getFieldName())
		}
	}
}

/*run script*/
ProcessTrbsca process = new ProcessTrbsca()
process.runBatch(binding)
