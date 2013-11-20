/**
 * @Ventyx 2012
 * Custom batch for testing MSSSOH.
 */
package com.mincom.ellipse.script.custom

import java.text.SimpleDateFormat;

import com.mincom.ellipse.edoi.ejb.msf096.MSF096_STD_VOLATKey;
import com.mincom.ellipse.edoi.ejb.msf096.MSF096_STD_VOLATRec;
import com.mincom.ellipse.edoi.ejb.msf220.MSF220Rec;
import com.mincom.ellipse.edoi.ejb.msf221.MSF221Key;
import com.mincom.ellipse.edoi.ejb.msf221.MSF221Rec;
import com.mincom.ellipse.edoi.ejb.msf230.MSF230Key;
import com.mincom.ellipse.edoi.ejb.msf230.MSF230Rec;
import com.mincom.ellipse.edoi.ejb.msf231.MSF231Key;
import com.mincom.ellipse.edoi.ejb.msf231.MSF231Rec;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key;
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec;
import com.mincom.ellipse.edoi.ejb.msf140.MSF140Key;
import com.mincom.ellipse.edoi.ejb.msf140.MSF140Rec;
import com.mincom.ellipse.edoi.ejb.msf141.MSF141Key;
import com.mincom.ellipse.edoi.ejb.msf141.MSF141Rec;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;

/**
 * Request Parameters for Trbsoh:
 * <li><code>districtCode</code>: District Code (Blank for all)</li>
 */
public class ParamsTrbsoh {
	String districtCode
}

/**
 * Main Process of Trbsoh
 */
public class ProcessTrbsoh extends SuperBatch {

	/*
	 * IMPORTANT!
	 * Update this Version number EVERY push to GIT
	 */
	private int version = 1
	private ParamsTrbsoh batchParams

	/**
	 * Run the main batch.
	 * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
	 */
	public void runBatch(Binding b) {
		init(b);
		printSuperBatchVersion();
		info("runBatch Version : " + version);
		//Get request parameters
		batchParams = params.fill(new ParamsTrbsoh())
		//Print request Parameters
		info("paramDistrict : " + batchParams.districtCode)

		try {
			processBatch()
		} catch(Exception e) {
			e.printStackTrace()
		} finally {
			printBatchReport()
		}
	}

	/**
	 * Process the main batch.
	 */
	private void processBatch() {
		info("processBatch")
		test_msssoh_option2()
	}
	
	private void test_msssoh_option2() {
		info("test_msssoh_option2")
		
		def msssohlnk = eroi.execute('MSSSOH', {msssohlnk ->
			msssohlnk.optionSoh = "2"
			msssohlnk.dstrctCode = "GRID"
			msssohlnk.stockCode = "000442640"
			msssohlnk.whouseId = "D1"})
		
		info("Debug MSSSOH: Stock Available = ${msssohlnk.stockAvailable}")
	}

	/**
	 * Print the batch report.
	 */
	private void printBatchReport() {
		info("printBatchReport")
	}

}

/**
 * Run the script.
 */
ProcessTrbsoh process = new ProcessTrbsoh()
process.runBatch(binding)