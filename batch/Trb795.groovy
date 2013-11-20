/**
 * @Ventyx 2012
 * 
 * This program loads Employee Performance Appraisal information via MSO795.
 * Reads from input file TRT795.
 * Produces report TRB795A listing results for each input file record.
 */

package com.mincom.ellipse.script.custom

import java.text.SimpleDateFormat

public class ProcessTrb795 extends SuperBatch {

	/**
	 * Update this Version number every time we push to GIT
	 */
	private version = 3

	private BatchTextReports Trb795a

	private static final String INPUT_FILE_NAME = "TRT795";
      private static final String REPORT_NAME     = "TRB795A";


	public void runBatch(Binding b) {

		init(b)

		printSuperBatchVersion()
		info("runBatch version : " + version)

            String workingDir = env.getWorkDir().toString()
            String taskUUID = this.getTaskUUID()
            String inputFileInclPath = "${workingDir}/${INPUT_FILE_NAME}"
            if(taskUUID?.trim()) {
                inputFileInclPath = inputFileInclPath + "." + taskUUID
            }
            info("Input File:" + inputFileInclPath)


		int createdRecords = 0
		int updatedRecords = 0
		int invalidRecords = 0

		Trb795a = report.open(REPORT_NAME)

		File file = new File(inputFileInclPath).withReader { reader ->
			def value = reader.readLine()

			/**
			 * If the first line read is null, then the file is empty and we cannot proceed
			 */
			if(value == null) {
				Trb795a.write("\nThe input file is empty/invalid, please provide a valid input file")
			} else {
				Trb795a.columns  = ["Employee   ", "Effective Date   ", "Appraisal Period           ", "Message   "]
				while (value != null) {
					/**
					 * If the length of the line read is less than 54, then we cannot proceed
					 */
					if(value.size() < 54) {
						invalidRecords++
						Trb795a.write("\nThe parsed line is invalid, skipping the line and moving to the next line")
					} else {
						String message = null
						String employeeId = value.substring(0, 10)
						String effectiveDate = value.substring(11, 19)
						String periodFrom = value.substring(34, 42)
						String periodTo = value.substring(43, 51)
                                    info("Input Record:" + value)
	
						EmployeeAppraisalDTO appraisalDTO = new EmployeeAppraisalDTO ()
						appraisalDTO.setEmployeeId(employeeId)
						appraisalDTO.setEffectiveDate(getMsoDate(effectiveDate))
						appraisalDTO.setAppraisalReason(value.substring(20, 22))
						appraisalDTO.setAppraiser(value.substring(23, 33))
						appraisalDTO.setPeriodFromDate(getMsoDate(periodFrom))
						appraisalDTO.setPeriodToDate(getMsoDate(periodTo))
						appraisalDTO.setSummaryRating(value.substring(52, 54))
	
						ScreenAppLibrary sl = new ScreenAppLibrary()
						info("Create Employee Appraisal:" + " employeeId:" + employeeId + " effectiveDate:" + effectiveDate)
						ScreenResultDTO resultDTO = sl.processEmployeeAppraisal(appraisalDTO, "1")
	
						/**
						 * If there are no errors, then the employee appraisal has been successfully processed
						 */
						if(resultDTO.error == null) {
							createdRecords++
							message = "INFO: SUCCESSFULLY LOADED"
						}
						/**
						 * If the error code is 3178, then the employee performance appraisal already exists. So we call MSO795 with an option 2
						 */
						else if(resultDTO.error.getErrorCode().equals("3178")) {
							info("Update Employee Appraisal:" + " employeeId:" + employeeId + " effectiveDate:" + effectiveDate)
							resultDTO = sl.processEmployeeAppraisal(appraisalDTO, "2")
							if(resultDTO.error == null) {
								updatedRecords++
								message = "INFO: ALREADY EXISTS, SUCCESSFULLY MODIFIED"
							} else {
								invalidRecords++
								message = "ERROR: " + resultDTO.error.getErrorMsg();
							}
						}
						/**
						 * If some other error is returned, then its just written in to the report
						 */
						else {
							invalidRecords++
							message = "ERROR: " + resultDTO.error.getErrorMsg();
						}
						
						Trb795a.write(employeeId, getReportDate(effectiveDate) + "      ", getReportDate(periodFrom) + " to " + getReportDate(periodTo), message)
					}
					value = reader.readLine()
				}
			}
		}

		Trb795a.write("\n")

		Trb795a.write(createdRecords + " Employee records created.");
		Trb795a.write(updatedRecords + " Employee records updated.");
		Trb795a.write(invalidRecords + " Employee records could not be processed.");

		Trb795a.write("\n")
		Trb795a.close()
	}

	/**
	 * This method takes a string input and returns date in the format required by the MSO program.
	 * 
	 * @param String
	 * @return String
	 */
	private String getMsoDate(String date) {
		return date.substring(6, 8) + "/" + date.substring(4, 6) + "/" + date.substring(2, 4)
	}

	/**
	 * This method takes a string input and returns date in the user readable format.
	 *
	 * @param String
	 * @return String
	 */
	private String getReportDate(String value) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd")
		Date date = sdf.parse(value)
		return new SimpleDateFormat ("dd/MM/yyyy").format(date)
	}
}

/**
 * Run Script
 */
ProcessTrb795 process = new ProcessTrb795()
process.runBatch(binding)


