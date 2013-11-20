s Groovy script will only ever be run as an Ellipse custom BATCH JOB within an Ellipse custom BATCH
       * and will ALWAYS have preceding BATCH JOBS within this custom BATCH,
       * with these preceding batch jobs being RDLS (within the same batch) that are producing the 
       * files that will be picked up and used within this TRBCOG Groovy script
       * 
       * All of the separate BATCH JOBS within a single BATCH will all run with the same taskuuid
       *  
       * The preceding RDL BATCH JOBS will have been requested with a reporting medium of 'P' (print), and a publish type of 'T' (text)
       *  
       * This will have resulted with Ellipse 8 putting these files into the EFS (Ellipse File System) directories by the time that this 
       * TRBCOG script runs, with the same taskuuid as this TRBCOG script is currently running with.
       *
       * Therefore this script needs to use various "EFS" type commands to pick up its input files for processing;
       * 
	 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;
      /*
       * Import the "EFS" Ellipse File System Package
	 */
import com.mincom.ellipse.efs.*;

public class ParamsTRBCOG{
	//List of Input Parameters used in testcases
      /*
       * The only input parameter will have a value of 
       * "POE" (Purchase Order email) OR
       * "POF" (Purchase Order fax)  OR
       * "QQE" (Request For Quote email) OR
       * "QQF" (Request For Quote fax)  OR
       * "REM" (Remittance Advice email and fax) 
       *
       * and this next input parameter will NOT be coming from an MSF080 report request, but relates to when running this script via
       * TestCase.groovy or runCustom - NOT when passing the parameter from the custom_batch table.
	 */
	String paramInputFileType;

}

public class ProcessTRBCOG extends SuperBatch {
	/* 
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT 
	 */
	private version = 1;
	/* 
	 * This next line defines the variable batchParams within the class ParamsTRBCOG
	 *   
	 */
	private ParamsTRBCOG batchParams;
	/* 
	 * This next line defines an "array" called "p" that is then used in picking up the params from the Custom_Batch.xml
	 */
      def p = [] 
	/* 
	 * This next line defines a variable called custombatchInputFileType which will hold the parameter supplied from the Custom_Batch.xml
	 */      
      def custombatchInputFileType

	private static final String INPUT_FILE_PART_1 = "TRR"
	private static final String INPUT_FILE_PART_2 = "1A"
	private static final String OUTPUT_FILE = "OUTPUT"
	private static final String WINSHARE = "//vsvwin2008e054/ELLIPSE/"

	public void runBatch(Binding b){

		init(b);

		printSuperBatchVersion();
		info("runBatch Version : " + version);
	/* 
	 * This next line populates the batch parameters from TestCase.groovy script
	 */      
		batchParams = params.fill(new ParamsTRBCOG())
	/* 
	 * This next line populates the batch parameters from Custom_Batch.xml
	 */      
		p = request.getProperty("Parameters").tokenize(",")
	/* 
	 * These next few lines will firstly
       * Pick up the parameters from the custom_batch.xml if supplied from there (we can tell if the "p" array has a size of 1), 
       * and otherwise will pick up the parameter from the TestCase.groovy script or the runCustom command
	 */   
           if (p.size() == 1) {
           info("paramInputFileType: Custom_batch.xml,1")  
           custombatchInputFileType = p[0];} 
           else {
                info("TestCase.groovy or runCustom")  
           custombatchInputFileType = batchParams.paramInputFileType}

		try {
			processBatch();
		} finally {
			printBatchReport();
		}
	}

	private void processBatch(){
		info("processBatch");

		/*
		 * Step 1: read the input file
         *
		 * This next line uses the "request" class with the "getTaskUuid" method from the SuperBatch Groovy to get the UUID that the
		 * RDL (either TRRPOE, TRRPOF, TRRQQE, TRRQQF or TRRREM) ran under, so that we can work out which file to pick up as the input
		 * file.
		 */
           /*
		 */

		String uuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.getTaskUuid()
		/*
		* Validate InputFileType request parameter value.
		* Only 'PO', 'QQ' or 'RE' are the valid Input File Type.
		* For invalid value of InputFileType, abort the process.
		*
		* The ! character means reverse boolean here ie return a "false" boolean if the condition is actually true
		* The || characters mean "OR"
		*/
	   String sParamInputFileType = batchParams.paramInputFileType.padRight(2).substring(0,2).toUpperCase()
	   if (!(sParamInputFileType.equals("PO")
	   || sParamInputFileType.equals("QQ")
	   || sParamInputFileType.equals("RE"))) {
		   throw new RuntimeException("Invalid Input File Type: ${sParamInputFileType}")
	   }

	   /*
		* This next line strings together the full directory name and the input file name to be picked up for processing
		*/
		String inputFilePath = env.getWorkDir().toString() + "/"+ INPUT_FILE_PART_1 + custombatchInputFileType + INPUT_FILE_PART_2 +uuid
		/*
		* This next three lines are intended to read the input file contents and keep it in memory as class object 'BufferedReader'.
		* We will read each record in the input file through this 'BufferedReader' object by invoking its 'readLine' method.
		*/
		def FileInputStream fileInputStream = new FileInputStream(inputFilePath)
 
		def DataInputStream dataInputStream = new DataInputStream(fileInputStream)

		BufferedReader inputFileReader = new BufferedReader(new InputStreamReader(dataInputStream))

		/*
		* This next line just defines a new variable called entityGroupMap, that is a "map" type variable,
		* and initialises it with an empty member.
		*/
	   HashMap entityGroupMap = [:]
	   /*
		* This next line just defines a new variable called entityRecords, that is a "list" type variable with a data type of "Arraylist",
		*/
	   List entityRecords = new ArrayList()
	   String lineRead = ""

	   boolean isProcessing = false
	   String entityNo = ""
	   String entityDeliveryMedium = ""
	   String entityDeliveryAddress = ""
	   String entityDeliveryFaxNumber = ""
	   String entityDeliveryEmailAddress = ""

	   /*
		* Now start processing through all of the records for an "entity" until you get to the end of records for that entity
		* The first record for each entity will be a record containing  /GO/NOR
		* (If the entity is a Purchase Order - it's last record will be a /END record,
		*  but if the entity is NOT a Purchase Order (ie it is a RFQ or a RA)- it's last record will be a /STOP record)
		*/

	   while ((lineRead = inputFileReader.readLine()) != null) {
		   /*
			* Check for special characters '^L' (octal 014) or '^M' (octal 015).
			* These print control characters should be ignored (removed) for the output files.
			*/

		   String sLineRead = lineRead.replace("^L", "").replace("^M", "")
		   boolean isENTITYGroup_StartRecord = sLineRead.contains("*/GO/NOR")

		   boolean isENTITYGroup_EndRecord = false
		   if ((sParamInputFileType.equals("PO") && sLineRead.contains("*/END"))
		   || ((sParamInputFileType.equals("QQ") || sParamInputFileType.equals("RE")) && sLineRead.contains("*/STOP"))) {
			   isENTITYGroup_EndRecord = true
		   }

		   /*
			* When we find the first Rightfax command line for a new group , start a new ArrayList for that group, within the entityRecords variable
			*/
		   if (isENTITYGroup_StartRecord) {
			   entityRecords = new ArrayList()
			   isProcessing = true
		   }
		   /*
			* For each record processed on the input file, always add that record to the entityRecords group,
			* and if the record is the "/DE=F" record, then find out the 16 character fax Number and record it in the entitydeliveryFaxNumber variable
			* and if the record is the "/DE=E" record, then find out the 40 character email Add and record it in the entitydeliveryEmailAddressvariable
			* and if the record is the "/DE=M" record, then find out the Delivery Method and
			* a) record it in the entityDeliveryMedium variable and
			* b) chose to populate the entire DeliveryAddress variable with either the Fax number or the Email address (which have previously been read)
			* and if the record is the "/USER" record, then find out the entity Number and record it in the entityno variable
			* and if the record is the "/DE=M" record, then find out the Delivery Method and record it in the entityDeliveryMedium variable
			*/

		   boolean isENTITYGroup_FaxnumberRecord = sLineRead.contains("*/DE=F")
		   boolean isENTITYGroup_EmailaddressRecord = sLineRead.contains("*/DE=E")
		   boolean isENTITYGroup_MediumRecord = sLineRead.contains("*/DE=M")
		   boolean isENTITYGroup_Medium_Email = sLineRead.contains("*/DE=ME")
		   boolean isENTITYGroup_Medium_Fax = sLineRead.contains("*/DE=MF")
		   boolean isENTITYGroup_IdRecord = sLineRead.contains("*/USER")

		   if (isProcessing) {
			   entityRecords.add(sLineRead)

			   if (isENTITYGroup_FaxnumberRecord) {
				   entityDeliveryFaxNumber = sLineRead.substring(sLineRead.indexOf("=") + 1)
			   }

			   if (isENTITYGroup_EmailaddressRecord) {
				   entityDeliveryEmailAddress = sLineRead.substring(sLineRead.indexOf("=") + 1)
			   }

			   if (isENTITYGroup_MediumRecord) {
				   if (isENTITYGroup_Medium_Fax) {
					   entityDeliveryMedium = "-fax"
					   entityDeliveryAddress = entityDeliveryFaxNumber
				   }
				   if (isENTITYGroup_Medium_Email) {
					   entityDeliveryMedium = "-email"
					   entityDeliveryAddress = entityDeliveryEmailAddress
				   }
			   }

			   if (isENTITYGroup_IdRecord) {
				   entityNo = sLineRead.substring(sLineRead.indexOf("=") + 1)
			   }
		   }

		   /*
			* When you have reached the last record  for that specific entity on the input file,
			* (which we know because we are now dealing with the "/END" record (if a Purchase Order),
			* or the "/STOP" record (if a Request For Quote or a Remittance Advice)), then  put all of the records relating to that Entity
			* into the entityGroupMap map, having first also included the record that just shows the entity Number to be used in naming
			* the output file in that same entityGroupMap map
			*/
		   if (isENTITYGroup_EndRecord) {

			   /*
				* Get file name and use it as the 'key' of the map member.
				* This file name must be unique.
				*/
			   String sFilePrefixName = ""
			   switch (sParamInputFileType) {
				   case "PO":
					   sFilePrefixName = "PO"
					   break
				   case "QQ":
					   sFilePrefixName = "RFQ"
					   break
				   case "RE":
					   sFilePrefixName = "RA"
					   break
			   }
			   String entityFileName = sFilePrefixName + "-" \
						   + entityNo.trim() \
						   + entityDeliveryMedium \
						   + ".txt"

			   entityGroupMap.put(entityFileName, entityRecords)

			   /*
				* Clear all flags and variables for the next group processing
				*/
			   isProcessing = false
			   entityNo = ""
			   entityDeliveryMedium = ""
			   entityDeliveryAddress = ""
			   entityDeliveryFaxNumber = ""
			   entityDeliveryEmailAddress = ""
		   }
	   }

	   /*
		* Step 3: Create output files
		*/
	   /*
		* At this point, each separate entry in the entityGroupMap map holds all of the records pertaining to a specific entity,
		* with the first record in the map holding the entity number to be used in the naming of the output file
		*
		* The "->" character in the below lines mean "perform a loop"
		* so the 4 lines below actually perform "an inner loop within an outer loop"
		*
		* Every time the outerloop performs via looking at the entityGroupMap.each- a new output file is defined, with a new filename.
		*
		* and each time a new record is returned in the inner loop, from the each_po map key and each_group array within the entityGroupMap map ,
		* a new record is written to that output file
		*
		*/

	   String sOutputPath = Boolean.getBoolean("mincom.groovy.classes") ? (env.getWorkDir().toString() + "/") : (WINSHARE + getInstance() + "/Supply/TCOG/pending/")
	   entityGroupMap.each {String each_entityFileName, ArrayList each_entityGroupRecords ->
		   /*
			* Create output file directly into shared folder /winshare/<elldev or elltst or ellprd>/Supply/TCOG/pending
			*/
		   def outputFile = new FileWriter(new File(sOutputPath + each_entityFileName))
		   each_entityGroupRecords.each {String each_entityGroupRecord ->
			   outputFile.write(each_entityGroupRecord  + "\n")
		   }
		   outputFile.close()
	   }
   }

   /**
	* Get Instance
	* @return
	*/
   private String getInstance(){
	   info("getInstance")

	   String sReturn = ""

	   File f = new File(WINSHARE + "elldev")
	   if (f.exists()) {
		   sReturn = "elldev"
	   } else {
		   f = new File(WINSHARE + "elltst")
		   if (f.exists()) {
			   sReturn = "elltst"
		   } else {
			   f = new File(WINSHARE + "ellprd")
			   if (f.exists()) {
				   sReturn = "ellprd"
			   }
		   }
	   }

	   return sReturn
   }

   private void printBatchReport(){
	   info("printBatchReport")
	   //print batch report
   }
}

/*run script*/
ProcessTRBCOG process = new ProcessTRBCOG();
process.runBatch(binding);

