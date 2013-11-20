/**
 * Script           : TRB879
 * Objective        : Monitors start/end dates for employees and non-employees
 *
 * Gets a nominated date range by either
 * from the last time this program ran (from MSF083) until today; or
 * from user-entered run-time request parameters (MSF080)
 *
 * Reads through all non-employees and monitors contract start date (PersMiscFldx1) and contract end date (PersMiscFldx2).
 * If either of these dates has occurred within the nominated date range, then rewrite the MSF811 record
 * so that an event message is generated, and an interface is initiated to publish the non-employee.
 *
 * Reads through all active employees, finds current (or most recent) employee primary position and 
 * monitors position start date (InvStrDate) and position end date (PosStopDate).
 * If either of these dates has occurred within the nominated date range, then rewrite the MSF878 record
 * so that an event message is generated, and an interface is initiated to publish the employee.
 *
 * No reports or output produced by this program (not required). The rewrite on the database is sufficient
 * to trigger the appropriate interface when an Ellipse Event message is generated.
 */

package com.mincom.ellipse.script.custom;

import java.text.SimpleDateFormat
import java.text.ParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.mincom.batch.request.Request
import com.mincom.batch.environment.*
import com.mincom.batch.script.*
import com.mincom.ellipse.edoi.common.logger.EDOILogger 
import com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException 
import com.mincom.ellipse.reporting.source.ReportSource
import com.mincom.ellipse.script.util.*
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import org.apache.commons.lang.StringUtils

import com.mincom.ellipse.edoi.ejb.msf083.MSF083Key
import com.mincom.ellipse.edoi.ejb.msf083.MSF083Rec
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Key
import com.mincom.ellipse.edoi.ejb.msf760.MSF760Rec
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Key
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Rec
import com.mincom.ellipse.edoi.ejb.msf811.MSF811Key
import com.mincom.ellipse.edoi.ejb.msf811.MSF811Rec

import com.mincom.enterpriseservice.ellipse.*
import com.mincom.ellipse.client.connection.*
import com.mincom.ellipse.ejra.mso.*
import com.mincom.enterpriseservice.exception.*


public class ParamsTRB879{
    //List of Input Parameters
    String paramFrom
    String paramTo
}

public class ProcessTRB879 extends SuperBatch{
    /* 
     * IMPORTANT!
     * Update this Version number EVERY push to GIT 
     */
    private version = 1;
    private ParamsTRB879 batchParams;
    Date sDate, eDate, rDate
    def dateFormats = ['dd/MM/yyyy','dd/MM/yy','d/MM/yy','d/M/yy','yyyyMMdd','yyMMdd']
    SimpleDateFormat dateFormat

    /*
     * do not touch these variables as they are standard and will be moved 
     * into a super class when ellipse Groovy supports.
     * 
     */

    public void runBatch(Binding b){
	
        init(b);
        printSuperBatchVersion();
        info("runBatch Version : " + version);

        batchParams = params.fill(new ParamsTRB879())
        // PrintRequest Parameters
        info("paramFrom : " + batchParams.paramFrom)
        info("paramTo   : " + batchParams.paramTo)

		if (request.getProperty("Parameters") == null) { 
			info("TestCase.groovy or runCustom")
		} else {
		/*
        *  Prioritize to pick up parameters from Custom_batch.xml
        *  then TestCase.groovy script
		*/
			p = request.getProperty("Parameters").tokenize(",")
        		if (p.size() > 0) {
					info("Custom_batch.xml, " + p.size() )
				} 
		}

        processBatch();
    }


    private void processBatch(){
        info("processBatch") 

		boolean updMsf083 = false

		if (isValidDate(batchParams.paramFrom) &&
			isValidDate(batchParams.paramTo) ) {
			sDate = new SimpleDateFormat('yyyyMMdd').parse(batchParams.paramFrom)
			eDate = new SimpleDateFormat('yyyyMMdd').parse(batchParams.paramTo)
		} else {
			rDate = getLastRunDate()
			sDate = rDate
			eDate = new Date()
			updMsf083 = true
		}

		info("Start Date : " + sDate.format('yyyyMMdd')) 
		info("End Date   : " + eDate.format('yyyyMMdd')) 

		runNonEmployee()
		runEmployee()
		if (updMsf083) { setLastRunDate() } 

    }


    private void runNonEmployee() {
		info("runNonEmployee") 

		/*
		 * Tables involved :
		 * MSF811 - Non-Employee
		 *
		 * Check start and end contract dates are within given input request data range.
		 * If so updates MSF811.
		 * 
		 */
		
		Date cf, ct 

		try { 
	        MSF811Key msf811key = new MSF811Key()
        	Constraint c1 = MSF811Key.nonEmplId.greaterThan('0')
	        def query = new QueryImpl(MSF811Rec.class).and(c1)
		
	        MSF811Key msf811keyDisp = new MSF811Key()
	        edoi.search(query,{MSF811Rec msf811rec ->
	                msf811keyDisp = msf811rec.getPrimaryKey()
					Boolean updateRequired = false
					String dispStartDate = "[blank]"
					String dispEndDate = "[blank]"
					if (!msf811rec.getPersMiscFldx1().isEmpty()) {
							if (isValidDate(msf811rec.getPersMiscFldx1())) {	
									cf = formatDate(msf811rec.getPersMiscFldx1()) 
									dispStartDate = cf.format('yyyyMMdd')
									if ( !(cf.after(eDate) || cf.before(sDate))) {
                                                             updateRequired = true
									} 	
							} else {
								dispStartDate = msf811rec.getPersMiscFldx1() 
							}
					}
					if (!msf811rec.getPersMiscFldx2().isEmpty()) {
							if (isValidDate(msf811rec.getPersMiscFldx2())) {	
									ct = formatDate(msf811rec.getPersMiscFldx2())
									dispEndDate = ct.format('yyyyMMdd') 
									if ( !(ct.after(eDate) || ct.before(sDate))) {
                                                             updateRequired = true
									} 	
							} else {
								dispEndDate = msf811rec.getPersMiscFldx2() 
							}
					}
					if (updateRequired) {
						edoi.update(msf811rec)
						info("--- Non-Employee: " + msf811keyDisp.getNonEmplId() + ", Contract Start Date: " + dispStartDate + ", Contract Stop Date: " + dispEndDate )  
					}

	        })
		} catch (EDOIObjectNotFoundException e) { info("Error : " + e)  }
		
    }

    private void runEmployee() {
		info("runEmployee ")

		/* 
		* Tables involved : 
		* MSF760 - Employee Personel Details
		* MSF878 - Employee Position
		* 
		* Picks up only active Employee, MSF760-EMP-STATUS = 'A',
		* and locate current (as as End Date) primary position on MSF878.
		* Compare position start and end dates are within input request 
		* date range.
		* Update MSF878 if employee start/end dates are within range.
		* 
		*/
		def tDate, eRevDate
		Date cf, ct 

		def sdf = new SimpleDateFormat("yyyyMMdd") 
		def endD = sdf.format(eDate) 
        eRevDate  = 99999999 - Integer.parseInt(endD.toString())  

        MSF760Key msf760key = new MSF760Key()
        Constraint c1 = MSF760Key.employeeId.greaterThan('0')
        Constraint c2 = MSF760Rec.empStatus.equalTo('A') 
        def query1 = new QueryImpl(MSF760Rec.class).and(c1).and(c2)

        MSF760Key msf760keyDisp = new MSF760Key();
        edoi.search(query1,{MSF760Rec msf760rec ->
			msf760keyDisp = msf760rec.getPrimaryKey()

 			MSF878Key msf878key = new MSF878Key()
			Constraint c3 = MSF878Key.employeeId.equalTo(msf760keyDisp.getEmployeeId())
			Constraint c4 = MSF878Key.primaryPos.equalTo('0')
			Constraint c5 = MSF878Key.invStrDate.greaterThanEqualTo(eRevDate.toString())
			def query2 = new QueryImpl(MSF878Rec.class).and(c3).and(c4).and(c5)

			MSF878Rec msf878rec = edoi.firstRow(query2)
			
			if (msf878rec != null) {
				MSF878Key msf878keyDisp = new MSF878Key()
				msf878keyDisp = msf878rec.getPrimaryKey() 
				tDate  = 99999999 - Integer.parseInt(msf878keyDisp.getInvStrDate())  

				Boolean updateRequired = false
				String dispStartDate = "[blank]"
				String dispEndDate = "[blank]"
				if (!msf878keyDisp.getInvStrDate().isEmpty()) {
						if (isValidDate(tDate.toString())) {	
								cf = formatDate(tDate.toString()) 
								dispStartDate = cf.format('yyyyMMdd')
								if ( !(cf.after(eDate) || cf.before(sDate))) {
                                                            updateRequired = true
								} 	
						} else {
							dispStartDate = tDate.toString() 
						}
				}
				if (!msf878keyDisp.getPosStopDate().isEmpty() &&
				    !(msf878keyDisp.getPosStopDate().equals('00000000')) ) {
						if (isValidDate(msf878keyDisp.getPosStopDate())) {	
								ct = formatDate(msf878keyDisp.getPosStopDate())
								dispEndDate = ct.format('yyyyMMdd') 
								if ( !(ct.after(eDate) || ct.before(sDate))) {
                                                            updateRequired = true
								} 	
						} else {
							dispEndDate = msf878keyDisp.getPosStopDate() 
						}
				}
				if (updateRequired) {
					edoi.update(msf878rec)
					info("--- Employee: " + msf878keyDisp.getEmployeeId() + ", Pos Start Date: " + dispStartDate + ", Pos Stop Date: " + dispEndDate )  
				}

			}
        })

    }

    private void setLastRunDate() {
		info("setLastRunDate") 

		/*
		 * Table(s) involved :
		 * MSF083 - Last Report Run Data
		 * Set LastSaveKey to today's date upon job completion.
		 * 
		 */
		def sdf = new SimpleDateFormat("yyyyMMdd") 
		def today = sdf.format(new Date()) 

		try {
			MSF083Key msf083key = new MSF083Key()
			MSF083Rec msf083rec = new MSF083Rec() 
			msf083key.setDstrctCode(request.getDistrict()) 
			msf083key.setProgName(request.request.getRequestName())
			msf083rec.setPrimaryKey(msf083key)  
			msf083rec.setLastSaveKey(today.toString()) 
			edoi.update(msf083rec)
				info("--- MSF083 updated " + today.toString()) 
		} catch (EDOIObjectNotFoundException e) { info("Error : " + e)  } 

    }


    private Date getLastRunDate() {
		info("getLastRunDate") 

		/*
		* Table(s) involved : 
		* MSF083 - Last Report Run Data
		* Picks up last date and time of batch run.
		* Return last return date, lrd
		* @return <code>lrd</code>
		*/
		def lastRD

		try {
        	MSF083Key msf083key = new MSF083Key()
                              info("--- Request District: " + request.getDistrict())
                              info("--- Request Name: " + request.request.getRequestName())
        	Constraint c1 = MSF083Key.dstrctCode.equalTo(request.getDistrict()) 
        	Constraint c2 = MSF083Key.progName.equalTo(request.request.getRequestName()) 
			def query = new QueryImpl(MSF083Rec.class).and(c1).and(c2)

	        MSF083Key msf083keyDisp = new MSF083Key()
       		edoi.search(query, {MSF083Rec msf083rec ->
                	msf083keyDisp = msf083rec.getPrimaryKey()
                              info("--- MSF083 PrimaryKey: " + msf083keyDisp)
                              info("--- MSF083 LastSaveKey: " + msf083rec.getLastSaveKey())
					if (isValidDate(msf083rec.getLastSaveKey())) {
						lastRD = msf083rec.getLastSaveKey() 
					} 
      		})
			   
		    if (lastRD == null || lastRD.isEmpty()) {
				lastRD = new SimpleDateFormat('yyyyMMdd').format(new Date().previous().previous())
			}
			
		} catch (EDOIObjectNotFoundException e) { info("Error : " + e)  } 

		info("--- Last Run Date : " + lastRD) 	
		Date lrd = new SimpleDateFormat("yyyyMMdd").parse(lastRD) 
		return lrd
		 
    }


    public Date formatDate(String date) { 

	/*
	 * Date to be converted to format YYYYMMDD
	 * Parameter date.
	 * @param <code> date </code>
	 * 
	 * Returns formated date, YYYYMMDD.
	 * @return	<code> fdate </code>
	 * 
	 */
		Date fdate, tdate
		def sdate
		String e = ''

		for (def i=0;i< dateFormats.size(); i++) {
			dateFormat = new SimpleDateFormat(dateFormats[i]) 
			if (date.trim().length() == dateFormat.toPattern().length()) {
				try { tdate = new SimpleDateFormat(dateFormats[i]).parse(date) 
				} catch (ParseException pe) { e = pe }
				if (e.size() == 0 ) {
					sdate = new SimpleDateFormat('yyyyMMdd').format(tdate)
					fdate = new SimpleDateFormat('yyyyMMdd').parse(sdate) 
					break  
			    }
			e = '' 
			}
		}

	return fdate
    }

	
    public boolean isValidDate(String vDate) { 

		/*
		 * Returns true if date format is valid
		 * @return <code> boolean </code>
		 * 
		 */
		String e = ''

		if (vDate == null) return false
		if (vDate.matches("[a-zA-Z]*")) return false

		for (def i=0;i<dateFormats.size();i++) {
			dateFormat = new SimpleDateFormat(dateFormats[i]) 
			if (vDate.trim().length() == dateFormat.toPattern().length()) {  
    			dateFormat.setLenient(false)
 		        try { dateFormat.parse(vDate.trim()) } catch (ParseException pe) { e = pe }
				if (e.size() == 0) { 
					 return true
					 break
				}
				e = ''
			}
		} 
  }



}
/*runscript*/  
ProcessTRB879 process = new ProcessTRB879();
process.runBatch(binding);
