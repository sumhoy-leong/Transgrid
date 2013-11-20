/*
@Ventyx 2012
*/
package com.mincom.ellipse.script.custom;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.eilib.EllipseEnvironment;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*


import java.lang.reflect.*;


    public class ParamsServiceCall{
		String paramDistrict;
		String paramPosition;
    }
    
    public class ProcessServiceCall extends SuperBatch {
        /* 
         * IMPORTANT!
         * Update this Version number EVERY push to GIT 
         */
        private version = 1;
        private ParamsServiceCall batchParams;
                
		private String serviceName
		private String method
		private ArrayList attributeName
		private ArrayList attributeValue
		
		public void runBatch(Binding b){            
            
            init(b);
            
            printSuperBatchVersion();
            info("runBatch Version : " + version);
            
            batchParams = params.fill(new ParamsServiceCall())
			//PrintRequest Parameters
			info("paramDistrict: " + batchParams.paramDistrict)
			info("paramPosition: " + batchParams.paramPosition)
            		
            try {
                    processBatch();
                
            } finally {
                    printBatchReport();
            }
        }

        private void processBatch(){
            info("processBatch");           
            //write process
			setAttribute()
			callService()

        }
        
        //additional method - start from here.
        
		private void setAttribute(){
			info("setAttribute")
			serviceName = "Table"
			method = "retrieve"
			
			attributeName = new ArrayList()
			attributeValue = new ArrayList()
			attributeName.add("TableType")
			attributeValue.add("MT")
				
			attributeName.add("TableCode")
			attributeValue.add("AB")
		}
		
		private void callService(){
			info("callService")
			
			// Original Code to call service

//			try{
//				(service.get('Table')).retrieve({Object it ->
//					it.tableType = 'MT' }).replyElements.each {
//					println "TableCode: ${it.tableCode} Table Desc: ${it.description}"}
//			} catch (Exception ex) {
//				println ex.toString()
//			}
			
			
			try{
			// Modified Code to call service dynamically
			// Cannot changes the retrieve into dynamic method
			// however the  the service name, attribute name, and attribute value has been set dynamically	
				service.get(serviceName).retrieve({Object it ->
					Class cls = it.class
					info ("ClassName: "+ cls.getName())
					Method[] methods = cls.getMethods()
					for (Method method : methods){
						String methodName = method.getName()
						info("MethodName: "+methodName)
						if (methodName.substring(0,3).equals("set")){
							int i = 0
							for(String attName : attributeName){
								info ("AttributeName: " + attName)
								info("attributeValue: "+ attributeValue[i])
								if (methodName.substring(3,methodName.length()).equals(attName)){
									method.invoke(it,attributeValue[i])
								}
								i++
							}
						}
					}
					}).replyElements.each {
					println "TableCode: ${it.tableCode} Table Desc: ${it.description}"}
			} catch (Exception ex) {
				println ex.toString()
			}
			
		}
		
        private void printBatchReport(){
            info("printBatchReport")
            //print batch report
        }
    }
        
/*run script*/  
ProcessServiceCall process = new ProcessServiceCall();
process.runBatch(binding);