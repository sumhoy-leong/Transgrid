/*@Ventyx 2013
 *
 * This program will do nothing.
 * Used to suppress output from core programs that are not to be run.
 */
package com.mincom.ellipse.script.custom;

import groovy.lang.Binding;
import groovy.sql.Sql
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import com.mincom.batch.script.*;
import com.mincom.ellipse.lsi.buffer.condmeasurement.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.impl.*;
import com.mincom.eql.*;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import com.mincom.ellipse.client.connection.ConnectionHolder;
import com.mincom.enterpriseservice.ellipse.ConnectionId;
import nacaLib.varEx.Var;
import com.mincom.ellipse.ejp.EllipseSessionDataContainer;
import com.mincom.ellipse.ejp.area.CommAreaWrapper;

public class ParamsTRBXXX{
    //List of Input Parameters 
    /*
     * None
     *
     */
}

public class ProcessTRBXXX extends SuperBatch {

    /*
     * Constants
     */
   /*
    * variables
    */

   /* 
    * IMPORTANT!
    * Update this Version number EVERY push to GIT 
    */
    private version = "1";
   /**
    * Run the main batch.
    * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
    */
    public void runBatch(Binding b){
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        info("Completed");
    }
}

/*run script*/  
ProcessTRBXXX process = new ProcessTRBXXX()
process.runBatch(binding);