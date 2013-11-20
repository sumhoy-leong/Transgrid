package com.mincom.ellipse.script.custom

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.lang.math.RandomUtils;

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.mincom.batch.script.FtpClientWrapper
import com.mincom.batch.script.Params
import com.mincom.batch.script.RequestInterface
import com.mincom.batch.environment.BatchEnvironment
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Key
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFKey
import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFRec
import com.mincom.ellipse.efs.EFSFile;
import com.mincom.ellipse.efs.EFSHelper;
import com.mincom.ellipse.efs.EllipseFileSystem;
import com.mincom.ellipse.script.util.EDOIWrapper
import com.mincom.eql.Constraint
import com.mincom.eql.impl.QueryImpl
import com.mincom.ellipse.script.util.EDOIWrapper


/**
 *
 * This class reads a list of commands from the Table MSFPRF and execute them using the class BatchCommands
 * It expects the transferId from the parameters to look at the table MSFPRF
 * The following tokens can be used in the commands (all tokens need to be Uppercase and inside curly brackets):
 *  ${UUID} - uuid
 *  ${WORK} - work directory
 *  ${1}, ${10} - the parameters from 1 to 10
 *  ${OWNER} - request user
 *  ${DATETIME} - current date time
 * Dynamic parameters can be loaded from a file, the command config_filename define the properties file name located at the folder
 * defined by the system property ellipse.fs.root, the keys will be replaced by their values (all tokens in the commands need to be uppercase)
 * Remind that the command config_filename needs to be at the top of the list of commands
 * To connect to the sftp, it uses the private key stored at the ellipse.fs.root/.ssh.
 * For the host server to accept this private key, its correspondent public key needs to be registered
 * in the host server as authorized_keys
 */
public class ProcessAABTRF extends SuperBatch{
	
	private static class ParamsAABTRF{
		//List of Input Parameters
		// as improvement the number of parameters could be dynamic, splitting the raw parameters list and in the converttoken
		// it would regexp to get any parameter
		String paramTransferId = ""
		String paramNumber1 = ""
		String paramNumber2 = ""
		String paramNumber3 = ""
		String paramNumber4 = ""
		String paramNumber5 = ""
		String paramNumber6 = ""
		String paramNumber7 = ""
		String paramNumber8 = ""
		String paramNumber9 = ""
		String paramNumber10 = ""
	}

	private static final version = 16

	private ParamsAABTRF batchParams = new ParamsAABTRF()
	
	private static final String TRANSFERID_REQUIRED = "Can not Find Transfer ID, Script aborted"
	private static final String TRANSFERID_NOTFOUND = "Could not Found Property ID"

	public void runBatch(Binding b){
		init(b)
		info("AABTRF version:"+ version)

		try {

			//Read parameters from custom_batch.xml property
			getParamsFromProperty()

			//Read Preference record
			String prefValue = readPreferenceId(batchParams.getParamTransferId())
			
			new BatchCommands(b).loadCommands(prefValue).setParameters([batchParams.paramNumber1, batchParams.paramNumber2,
				batchParams.paramNumber3, batchParams.paramNumber4, batchParams.paramNumber5, batchParams.paramNumber6,
				batchParams.paramNumber7, batchParams.paramNumber8, batchParams.paramNumber9, batchParams.paramNumber10]).execute();
		}
		catch (all){
			Writer writer = new StringWriter()
			PrintWriter printWriter = new PrintWriter(writer)
			all.printStackTrace(printWriter)
			errorMessage (writer.toString())
		}
	}

	/*
	 * Get Parameters from custom_batch.xml's property
	 */
	private void getParamsFromProperty () {

		// Hardcoded at the moment
		//batchParams.setParamTransferId("local:cp \${REPORT_NAME=Table File Listing Listing table file - MSR010A.pdf} \${WORK}/the_description_MSR010A.\${UUID}.pdf")
		info("Get Params from Property")
		batchParams = params.fill(new ParamsAABTRF())

		batchParams.each{
			info("Transfer ID: " +  it.getParamTransferId())
			info("Params 1   : " +  it.getParamNumber1())
			info("Params 2   : " +  it.getParamNumber2())
			info("Params 3   : " +  it.getParamNumber3())
			info("Params 4   : " +  it.getParamNumber4())
			info("Params 5   : " +  it.getParamNumber5())
			info("Params 6   : " +  it.getParamNumber6())
			info("Params 7   : " +  it.getParamNumber7())
			info("Params 8   : " +  it.getParamNumber8())
			info("Params 9   : " +  it.getParamNumber9())
			info("Params 10   : " +  it.getParamNumber10())
		}

		if ("".equals(batchParams.getParamTransferId().trim())){
			// try to retrieve from request parameters
			batchParams.setParamTransferId("UNDEFINED")
			errorMessage(TRANSFERID_REQUIRED)
		}

	}

	/*
	 * Read Preference record
	 */
	private String readPreferenceId (String transferId){
		String sPrefValue = ""

		/*// it seems there is no preference service at com.mincom.enterpriseservice/ellipse
		 info("read_mseprf ")
		 service.get('PreferencesService').read({
		 it.prefProperty = 'STEF2'
		 }, 20).replyElements.each {
		 println "${it.prefValue} "}
		 info ("-----------------------------");*/

		/**
		 * Browsing MSFPRF with dstrctCode, userId and prefCounter blanks.
		 */
		info ("Read Preference Record.")

		Constraint c1 = MSFPRFKey.prefProperty.equalTo(transferId)
		Constraint c2 = MSFPRFKey.dstrctCode.equalTo("    ")
		Constraint c3 = MSFPRFKey.userId.equalTo("          ")
		Constraint c4 = MSFPRFKey.prefCounter.equalTo("         ")

		def query = new QueryImpl(MSFPRFRec.class).and(c1).and(c2).and(c3).and(c4)

		edoi.search(query){MSFPRFRec preferenceRec ->
			sPrefValue = sPrefValue + preferenceRec.getPrefValue()
		}

		if (sPrefValue == ""){
			//this could be a valid command
			sPrefValue = transferId
		}

		info ("Found Preference Record.")
		return sPrefValue
	}

	/**
	 * Print Error Message
	 **/
	private void errorMessage(String sErrorMsg){
		info("------------- " + sErrorMsg)
		assert false,sErrorMsg
	}
	

}

/*run script*/
ProcessAABTRF process = new ProcessAABTRF()
process.runBatch(binding)
