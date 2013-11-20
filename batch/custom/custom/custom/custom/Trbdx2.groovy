package com.mincom.ellipse.script.custom;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.environment.*;
import com.mincom.batch.script.*;
import com.mincom.ellipse.edoi.common.logger.EDOILogger;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import org.apache.commons.lang.StringUtils

import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequestDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceRetrieveReplyCollectionDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadRequiredAttributesDTO;
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadRequestDTO; 
import com.mincom.enterpriseservice.ellipse.ErrorMessageDTO

import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFKey
import com.mincom.ellipse.edoi.ejb.msfprf.MSFPRFRec

import com.mincom.enterpriseservice.ellipse.*
import com.mincom.ellipse.client.connection.*
import com.mincom.ellipse.ejra.mso.*;
import com.mincom.enterpriseservice.exception.*;

public class ParamsTrbdx2{
    //List of Input Parameters
    String paramGrid;

}

public class ProcessTrbdx2 extends SuperBatch{
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 1;
    private ParamsTrbdx2 batchParams;

    private static final int REQUEST_REPLY_NUM = 20
    private int i = 0

    /*
     * do not touch these variables as they are standard and will be moved 
     * into a super class when ellipse Groovy supports.
     * 
     */


    public void runBatch(Binding b){
	def grid = ""
	def p = []
	
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);
        batchParams = params.fill(new ParamsTrbdx2())
        // PrintRequest Parameters
        info("paramGrid : " + batchParams.paramGrid)

	p = request.getProperty("Parameters").tokenize(",")

       /*
        *  Prioritize to pick up parameters from Custom_batch.xml
        *  then TestCase.groovy script
       */
	info("p SIZE : " + p.size()) 
        if (p.size() > 0) {
           info("Custom_batch.xml, " + p.size() )
           grid = p[0];
        } else {
           info("TestCase.groovy or runCustom")
           grid = batchParams.paramGrid
        }


        processBatch(grid);

    }

    private void processBatch(def grid){
	def names = []
	def emails = []
        def subject = getPreference("#MAI."+grid+".email.subject")
        List message = new ArrayList()
        message.add(getPreference("#MAI."+grid+".email.body"))
        def outf = getPreference("#MAI."+grid+".path.output")
        def from = getPreference("#MAI."+grid+".email.from")

        info("processBatch");

	names = getTable(grid)    

	names.each() { 
        	emails.addAll(getEmailAdress(it)) 
	}  

	emails.each() {
        info("xxx email ${it} ")
	//endEmail sendEmail = new SendEmail(subject, "${it}" , message, outf, from)
//sendEmail.sendMail()
	} 
    }

/*
    private String getP(String property) {

	PreferencesSearchParam searchParam = new PreferencesSearchParam();   
	int size = 3;   
	context.setMaxInstances(size);   
	ArrayOfPreferencesServiceResult result = service.search(context, searchParam, null);   
	List list = result.getPreferencesServiceResult();   
	if(list.size() == size) {   
	    Object restart = list.get(size-1);   
	    service.search(context, searchParam, restart);   
	}  


    }
*/

    private String getPreference(String property) {
       info ("getPreference ") 

        MSFPRFKey msfprfkey = new MSFPRFKey();

        Constraint c1 = MSFPRFKey.prefProperty.equalTo(property);
        def query = new QueryImpl(MSFPRFRec.class).and(c1);
        def sPrefProperty
        def sPrefValue
        def value

        MSFPRFKey msfprfkeyDisp = new MSFPRFKey();
        edoi.search(query,{MSFPRFRec msfprfrec ->

            msfprfkeyDisp = msfprfrec.getPrimaryKey()

            sPrefProperty = "Preference Property :" + msfprfkeyDisp.getPrefProperty();
            sPrefValue = "Preference Value : " + msfprfrec.getPrefValue();
	    value = msfprfrec.getPrefValue();
            info(sPrefProperty);
            info(sPrefValue);
	
        })
	return value 	
    }


    private List<String> getTable(def grid) {

	info("getTable") 
	def nameList = []

	try {
        	service.get('Table').retrieve({ it.tableType = 'ACTK' }, 20).replyElements.each { 
			println "${it.tableCode} ${it.description}"}

	        Object obj = service.get('Table')
	 } catch (EnterpriseServiceOperationException e) { info("${e.getMessage()}")  }


	try {
		service.get('Table').retrieve({ it.tableType = '#MAI'},20).replyElements.each {
			if (it.description.contains('GRID345')) {
				nameList.add(it.getTableCode().trim()) 
			}
		}
	Object obj = service.get('Table') 
	} catch (EnterpriseServiceOperationException e) { info("${e.getMessage()}")  } 

	return nameList
    }


    private List<String> getEmailAdress(String Name) {
        info("getEmailAdress")
        List<String> emailAdresses = new ArrayList<String>()
        List<String> name = Arrays.asList(Name.split(" ")) 

        EmployeeServiceRetrieveReplyDTO employeeReadReply
        String errorMessage = ""

        EmployeeServiceRetrieveRequiredAttributesDTO empReqAtrr = new EmployeeServiceRetrieveRequiredAttributesDTO()
        empReqAtrr.returnEmailAddress    = true
        empReqAtrr.returnPersonalEmail   = true
        empReqAtrr.returnEmployee        = true
        empReqAtrr.returnPersonnelStatus = true
        empReqAtrr.returnPersEmpStatus   = true
        empReqAtrr.returnFirstName       = true
        empReqAtrr.returnLastName        = true

        List<EmployeeServiceRetrieveReplyDTO> cmReplyList = new ArrayList<EmployeeServiceRetrieveReplyDTO>()
        try {
            def restart = ""
            EmployeeServiceRetrieveReplyCollectionDTO cmReplyDTO = service.get("EMPLOYEE").retrieve({ EmployeeServiceRetrieveRequestDTO it ->
                it.requiredAttributes = empReqAtrr
                it.nameSearchMethod   = "E"
                it.firstName          = name[1]
            }, REQUEST_REPLY_NUM, false, restart)
            restart = cmReplyDTO.getCollectionRestartPoint()
            cmReplyList.addAll(cmReplyDTO.getReplyElements())
            /*
             * restart value has been set from above service call, then do a loop while restart value is not blank
             * this loop below will get the rest of the rows
             */
            while(restart != null && restart.trim().length() > 0) {
                cmReplyDTO = service.get("EMPLOYEE").retrieve({ EmployeeServiceRetrieveRequestDTO it ->
                    it.requiredAttributes = empReqAtrr
                    it.nameSearchMethod   = "E"
                    it.firstName          = name[1]
                }, REQUEST_REPLY_NUM, false, restart)
                restart = cmReplyDTO.getCollectionRestartPoint()
                cmReplyList.addAll(cmReplyDTO.getReplyElements())
            }
        } catch (EnterpriseServiceOperationException serviceExc) {
            info("Error at retrieveMntTypes ${serviceExc.getMessage()}")
        }
        if (cmReplyList) {

            for(EmployeeServiceRetrieveReplyDTO emp : cmReplyList) {
                info("HRY:EMP: ${emp.getEmployee()} :Name: |${emp.getFirstName()}|${emp.getLastName()}| :Email: |${emp.getEmailAddress()}|${emp.getPersonalEmail()}| :Status ${emp.getPersEmpStatus()}")
                if (name[0].equals(emp.getLastName())
                && emp.getEmailAddress()?.trim().length() > 0
                && "A".equals(emp.getPersEmpStatus())) {
                    emailAdresses.add(emp.getEmailAddress())
                }
            }
        }
        return emailAdresses
    }


}
/*runscript*/  
ProcessTrbdx2 process = new ProcessTrbdx2();
process.runBatch(binding);
