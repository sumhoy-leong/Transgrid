/*
 @Ventyx 2013
 *
 * This program is Phil's test program <br>
 */
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key;
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec;
import com.mincom.ellipse.edoi.ejb.msf083.MSF083Key;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key;
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_PG_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_A_801Rec;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Key;
import com.mincom.ellipse.edoi.ejb.msf801.MSF801_D_801Rec;
import com.mincom.ellipse.edoi.ejb.msf802.MSF802Key;
import com.mincom.ellipse.edoi.ejb.msf802.MSF802Rec;
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Rec;
import com.mincom.ellipse.edoi.ejb.msf817.MSF817Key;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Key;
import com.mincom.ellipse.edoi.ejb.msf820.MSF820Rec;
import com.mincom.ellipse.edoi.ejb.msf823.MSF823Key;
import com.mincom.ellipse.edoi.ejb.msf823.MSF823Rec;
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Key;
import com.mincom.ellipse.edoi.ejb.msf835.MSF835Rec;
import com.mincom.ellipse.edoi.ejb.msf837.MSF837Key;
import com.mincom.ellipse.edoi.ejb.msf837.MSF837Rec;
import java.text.DecimalFormat;

public class ParamsTRBPC0{
    //List of Input Parameters
    String paramStaffCateg;
    String paramWeekEndDate;
}

public class ProcessTRBPC0 extends SuperBatch{
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 1;
    private ParamsTRBPC0 batchParams;
    private static final String PAY_GROUP_TG1 = "TG1"
    private static final String PAY_GROUP_T01 = "T01"
    private static final int MAX_ROW_READ     = 1000
 
    private int msf823Count
    private int msf837Count

    /**
     * Run the main batch.
     * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
     */
    public void runBatch(Binding b){

        init(b);

        printSuperBatchVersion();
        info("runBatch Version : " + version);

        processBatch();

    }

    /**
     * Main process
     */
    private void processBatch(){
        info("processBatch");
        mainProcess()
    }


    /**
     * Process emp's earn and emp's deduction
     */
    private void mainProcess(){
        info("mainProcess")
        processEmpDeduct()
    }


    /**
     * Browse for employee deduction
     */
    private void processEmpDeduct(){
        info("processEmpDeduct")
        msf837Count = 0
        Constraint c1 = MSF837Key.dednCode.notEqualTo("000")
        Constraint c2 = MSF837Key.employeeId.equalTo("0000066849")
        Constraint c3 = MSF837Key.dednCode.equalTo("400")
        def query = new QueryImpl(MSF837Rec.class).columns([MSF837Key.employeeId,MSF837Key.dednCode]).and(c1.and(c2)).distinct().orderBy(MSF837Key)
        edoi.search(query, MAX_ROW_READ, {msf837result ->
            info("MSF837 Employee: "+msf837result.getAt(0) + " Deduction Code: "+msf837result.getAt(1))
			msf837Count++
        })
		info("MSF837 Counter: "+ msf837Count)
    }

}

/*run script*/
ProcessTRBPC0 process = new ProcessTRBPC0();
process.runBatch(binding);
