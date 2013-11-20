/**
 * @Ventyx 2012
 *
 * Change the Batch Type of Labour Costing transactions in MSF857 from S to O
 * if the Posting Status is CO - Confirmed.
 * Labour Costing transactions that have been already processed by TRBSCA and
 * then later reversed by a user will create new records in MSF857 that will have
 * an 'S' Batch Type and 'CO' posting status.
 *
 * Changing the Batch Type from 'S' to 'O' will allow the Support Costs Allocation
 * Batch TRBSCA to process those records.
 *
 */

package com.mincom.ellipse.script.custom;

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.mincom.batch.request.Request
import com.mincom.batch.script.*
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Key
import com.mincom.ellipse.edoi.ejb.msf857.MSF857Rec
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO
import com.mincom.enterpriseservice.exception.EnterpriseServiceOperationException

public class ParamsTrbscb {
    //List of Input Parameters
    String paramScb;
}

public class ProcessTrbscb extends SuperBatch {
    private final version = 1 // Update this number every push to GitHub
    
    private ParamsTrbscb batchParams
    
    private static final POSTING_STATUS = "CO"
    private static final BATCH_TYPE = "S"
    
    public void runBatch(Binding b) {
        info("runBatch Version : " + version)
        
        init(b)
       
        // Request parameters
        batchParams = params.fill(new ParamsTrbscb())
        processBatch()
    }
    
    /**
     * Search for qualifying Labour Costing Transactions from MSF857
     * batch-type = "S"
     * posting-status = "CO"
     */
    private void processBatch() {
        info("processBatch")
        
        Integer iRecRead = 0
        Integer iOkRec = 0
        Integer iErrRec = 0
        
        // Get Labour Costing Transaction
        Constraint c1 = MSF857Rec.postingStatus.equalTo(POSTING_STATUS)
        StringConstraint c2 = MSF857Rec.labBatchNo.substring(1,1).equalTo(BATCH_TYPE)
        Query queryMsf857 = new QueryImpl(MSF857Rec.class).and(c1).and(c2).orderBy(MSF857Rec.msf857Key)
        
        // Run query to find labour transactions matching the search criteria
        edoi.search(queryMsf857,1000,{MSF857Rec msf857rec ->
          
            iRecRead++
            try {
                msf857rec.setLabBatchNo("O" + msf857rec.getLabBatchNo().substring(1))
                edoi.update(msf857rec)
                iOkRec++
            } catch(EDOIObjectNotFoundException e) {
                info("##### ERROR: Unable to update Batch Type to O #####")
                iErrRec++
            }
        })
        info("Records updated : ${iOkRec}")
        info("Records failed  : ${iErrRec}")
        info("Records read    : ${iRecRead}")
    }
}

/*run script*/
ProcessTrbscb process = new ProcessTrbscb()
process.runBatch(binding)
