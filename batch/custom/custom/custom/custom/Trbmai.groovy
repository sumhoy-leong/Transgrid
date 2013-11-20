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
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Key
import com.mincom.ellipse.edoi.ejb.msf010.MSF010Rec
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Key
import com.mincom.ellipse.edoi.ejb.msf810.MSF810Rec

import com.mincom.enterpriseservice.ellipse.*
import com.mincom.ellipse.client.connection.*
import com.mincom.ellipse.ejra.mso.*;
import com.mincom.enterpriseservice.exception.*;

/**
 * Script           : Trbmai
 * Title            : Email results to recipient list.
 * Objective        : Email recipient list derived from Table/MSF010, table type #MAI
 * 				      and table code 'GRID345'.  Output file, from TRB345, will be attached
 * 					  and email accordingly.
 * Request
 * entry/ MSF081    : N/A
 *
 * Custom_batch.xml : Required
 *
 * Usage :
 *  <Batch Name="transgrid.TRBMAI">
 *   <TaskList ExecuteMode="Sequential" Success="All">
 *     <RunBatchJob Name="TRBMAI" ParameterLocation="Property">
 *       <Property Name="Parameters" Value={grid no.} />
 *     </RunBatchJob>
 *   </TaskList>
 * </Batch>
 *
 * e.g. :
 *  <Batch Name="transgrid.TRBMAI">
 *   <TaskList ExecuteMode="Sequential" Success="All">
 *     <RunBatchJob Name="TRBMAI" ParameterLocation="Property">
 *       <Property Name="Parameters" Value="GRID345"/>
 *     </RunBatchJob>
 *   </TaskList>
 * </Batch>
 *
 * grid no.  : GRID number.
 *
 */

public class ParamsTrbmai{
    //List of Input Parameters
    String paramGrid;

}

public class ProcessTrbmai extends SuperBatch{
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 1;
    private ParamsTrbmai batchParams

/*
  Variables meant for Service Call, commented off due to 
  replacement to edoi method.
  
    private static final int REQUEST_REPLY_NUM = 20
    private int i = 0

*/

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
        info("runBatch Version : " + version)

        batchParams = params.fill(new ParamsTrbmai())
        // PrintRequest Parameters
        info("paramGrid : " + batchParams.paramGrid)

		if (request.getProperty("Parameters") == null) { 
           info("TestCase.groovy or runCustom")
           grid = batchParams.paramGrid
		} else {
       /* 
        *  Prioritize to pick up parameters from Custom_batch.xml
        *  then TestCase.groovy script
       */
	    	p = request.getProperty("Parameters").tokenize(",")
            if (p.size() > 0) {
               info("Custom_batch.xml, " + p.size() )
               grid = p[0]
            }
        }

        processBatch(grid)

    }

    private void processBatch(def grid){
        info("processBatch");

		def names = []
		def emails = []
        def subject = getPreference("#MAI."+grid+".email.subject")
        List message = new ArrayList()
        message.add(getPreference("#MAI."+grid+".email.body"))
        def outf = getPreference("#MAI."+grid+".path.output")
        def from = getPreference("#MAI."+grid+".email.from")

        names = getTable(grid)    

		names.each() { emails.addAll(getEmail(it))}    

		emails.each() {
//			For debugging purpose, SendMail will abort when values are null passed in
//	    	info(subject + ","+ it + "," + message + "," + outf + "," + from) 
			SendEmail sendEmail = new SendEmail(subject, "${it}" , message, outf, from)
			sendEmail.sendMail()
		} 
    }


    private String getPreference(String property) {
       info ("getPreference ") 
       /*
       * Parameter Preferences table, field Property 
       * @param <code> property </code>
       * 
       * Returns values from Preferences table
       * @return <code> value </code>
	   * Tables involved :
	   * MSFPRF - Preferences
	   *
	   * Retrieve email information ie, Subject, message, attachement
	   *
	   */
	   
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

		/*
		* Parameter grid
		* @param <code> grid </code>
		* 
		* Returns User name list eligble for mailing list
		* @return <code> nameList </code>
		* 
		* Tables involved :
		* MSF010 - Table
		*
		* Table type : #MAI
		* Table Desc : GRID345
		* Table Code : Names of GRID345 to be email
		* 
		*/
	
        MSF010Key msf010key = new MSF010Key();
        Constraint c1 = MSF010Key.tableType.equalTo('#MAI');
        def query = new QueryImpl(MSF010Rec.class).and(c1);
        def sTableCode
        def nameList = []

        MSF010Key msf010keyDisp = new MSF010Key();
        edoi.search(query,{MSF010Rec msf010rec ->
            msf010keyDisp = msf010rec.getPrimaryKey()
	        if (msf010rec.getTableDesc().contains(grid)) {
            	sTableCode = "Table Code : " + msf010keyDisp.getTableCode()
          	    nameList.add(msf010keyDisp.getTableCode().trim())
            	info(sTableCode)
	        }

        })
		
        return nameList 
    }
	
	
	private List<String> getEmail(def Name) {
		info("getEmail ")

		/*
		 * Parameter User's name from mailing list
		 * @param <code> Name </code>
		 * 
		 * Return List of email address of respective users.
		 * @return <code> emailList </code>
		 * 
		* Tables involved :
		* MSF810 - Employee
		*
		* Loops through all Employee which consist of matching names, first and last name, 
		* of table MSF010.
		* For every matching record, store email into list and return.
		*
		*/
		
		def emailList = []
		def name = Arrays.asList(Name.split(" "))
	
		MSF810Key msf810key = new MSF810Key();	
		Constraint c1 = MSF810Key.employeeId.greaterThan('0');
		def query = new QueryImpl(MSF810Rec.class).and(c1);

		MSF810Key msf810keyDisp = new MSF810Key();
		edoi.search(query,{MSF810Rec msf810rec ->	
			msf810keyDisp = msf810rec.getPrimaryKey()
            if ( msf810rec.getFirstName().contains(name[1]) && 
				 msf810rec.getSurname().contains(name[0]) ) {
				 if (msf810rec.getEmailAddress() != null) {
					 emailList.add(msf810rec.getEmailAddress().trim())
				 }
			}
		})

	return emailList		
	}
	
	
	/*
	 *  Unable to use service request due to Custom_batch.xml
	 *  ParameterLocation values are taken up by Property.
	 *  As a result, Service calls methods fails to locate
	 *  request's credentials, userid and password.
	 *  ie
	 *  <RunBatchJob Name="TRBMAI" ParameterLocation="Property" />
	 *    <Property Name="Parameters" Value="Parameters" />
	 *
	 *  Service calls only works with ;
	 *  <RunBatchJob Name="TRBMAI" ParameterLocation="MSF080" />
	 *
	 */

/*
    private List<String> getTable(def grid) {

	info("getTable") 
	def nameList = []

	try {
		service.get('Table').retrieve({ it.tableType = '#MAI'},20).replyElements.each {
			if (it.description.contains('GRID345')) {
				nameList.add(it.getTableCode().trim()) 
			}
		}
	} catch (EnterpriseServiceOperationException e) { info("${e.getMessage()}")  } 

	return nameList
    }
*/

/*
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
	
*/


}
/*runscript*/  
ProcessTrbmai process = new ProcessTrbmai();
process.runBatch(binding);
