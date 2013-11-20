/**
 * @Ventyx 2012
 * 
 * This batch provides reusable variables and methods for its child.
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mincom.batch.environment.BatchEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.common.logger.EDOILogger;
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.common.unix.*;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql;
import com.mincom.eql.*;
import com.mincom.eql.impl.*;
import com.mincom.reporting.text.TextReportHelper;
import com.mincom.reporting.text.TextReportService

/**
 * SuperBatch, this batch provides reusable variables and methods for its child. 
 */
public class SuperBatch implements GroovyInterceptable {

    private static final long REFRESH_TIME = 30 * 1000

    public EDOIWrapper edoi
    public EROIWrapper eroi
    public ServiceWrapper service
    public BatchWrapper batch;
    public CommAreaScriptWrapper commarea;
    public BatchEnvironment env
    public UnixTools tools
    public Reports ellipseReport;
    public BatchReports report
    public Sort sort;
    public Params params;
    public RequestInterface request;
    public Restart restart;
    
    private String uuid;
    private String taskUuid;

    private Date lastDate;

    private boolean disableInvokeMethod

    public static final int SuperBatch_VERSION = 18;
    public static final String SuperBatch_CUST = "TRAN1";

    /**
     * Print a string into the logger. 
     * @param value a string to be printed.
     */
    public void info(String value){
        def logObject = LoggerFactory.getLogger(getClass());
        logObject.info("------------- " + value)
    }
    
    public void debug(String value){
        def logObject = LoggerFactory.getLogger(getClass());
        logObject.debug("------------- " + value)
    }

    public void trace(String value){
        def logObject = LoggerFactory.getLogger(getClass());
        logObject.trace("------------- " + value)
    }
    
    /**
     * Initialize the variables based on binding object.
     * @param b binding object
     */
    public void init(Binding b) {
        edoi = b.getVariable("edoi");
        eroi = b.getVariable("eroi");
        service = b.getVariable("service");
        batch = b.getVariable("batch");
        commarea = b.getVariable("commarea");
        env = b.getVariable("env");
        tools = b.getVariable("tools");
        ellipseReport = b.getVariable("report");
        sort = b.getVariable("sort");
        request = b.getVariable("request");
        restart = b.getVariable("restart");
        params = b.getVariable("params");


        // gets the uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
        uuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.getUUID();

        // gets the task uuid from the request, in case the vm 'argument mincom.groovy.classes' is true the uuid will be blank
        taskUuid = Boolean.getBoolean("mincom.groovy.classes") ? "" : request.request.getTaskUuid();

        report = new BatchReports (b,getSuperBatchDetails())
    }

    /**
     *  Returns the uuid
     * @return String UUID
     */
    public String getUUID() {
        return uuid
    }

    /**
     *  Returns the task uuid from the parent
     * @return String UUID
     */
    public String getTaskUUID() {
        return taskUuid
    }

    /**
     * Print the version.
     */
    public void printSuperBatchVersion(){
        info ("SuperBatch Version:" + SuperBatch_VERSION);
        info ("SuperBatch Customer:" + SuperBatch_CUST);
    }

    def invokeMethod(String name, args) {
        if (!disableInvokeMethod) {
            disableInvokeMethod = true;
            try {
                keepAliveConnection();
            } finally {
                disableInvokeMethod = false;
            }
        }
        def result
        def metaMethod = metaClass.getMetaMethod(name, args)
        result = metaMethod.invoke(this, metaMethod.coerceArgumentsToClasses(args))
        return result
    }

    protected void keepAliveConnection() {
        if (lastDate == null) {
            lastDate = new Date();
        } else {
            Date currentDate = new Date();
            debug("Time elapsed  = " + (currentDate.getTime() - lastDate.getTime()))
            debug("Time refresh  = " + REFRESH_TIME)
            if ((currentDate.getTime() - lastDate.getTime()) > REFRESH_TIME ) {
                lastDate = currentDate;
                restartTransaction();
            }
        }
    }

    protected void restartTransaction() {
        debug("restartTransaction")
        (0..0).each restart.each(1, { debug("Restart Transaction") })
        debug("end restart transaction")
    }
    
    protected MSF080Rec getSuperBatchDetails(){
        info ("getSuperBatchDetails")
        Constraint c1 = MSF080Rec.uuid.equalTo(uuid)
        def query = new QueryImpl(MSF080Rec.class).and(c1) 
        
        MSF080Rec msf080Rec = edoi.firstRow(query) 
        if (msf080Rec == null){
            return new MSF080Rec()
        }else{
            return msf080Rec
        }
    }
}

