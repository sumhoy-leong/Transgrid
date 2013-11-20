/*
@Ventyx 2012
 * Conversion from trb22b.cbl
 *
 * This program sends an email advising the requesting officer of
 * an impending expiry of their period order (a number of months
 * in advance). This allows enough time to set up a new one.
 *
 *Revision History 
 *************************
 * Date        Name     Desc											        Ver
 * 26/08/2013  LokeWS   SC4338099_WO25885 Enhancement  to TRB22BA & TRB22CA Version 3
 *                      Purchase order notification will be sent to new
 *						employee via position details.
 *						- If more than one person exists in a position then 
 *						  send to all in that position.
 *                      - If no one in a position then send email to 
 *                        Requested By
 *                      - Only send emails to person who have position as
 *                        primary   
 *
*/
package com.mincom.ellipse.script.custom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mincom.batch.request.Request;
import com.mincom.batch.script.*;
import com.mincom.ellipse.reporting.source.ReportSource;
import com.mincom.ellipse.script.util.*;
import groovy.sql.Sql
import com.mincom.eql.impl.*
import com.mincom.eql.*
import com.mincom.ellipse.edoi.ejb.msf035.*
import com.mincom.ellipse.edoi.ejb.msf036.*
import com.mincom.ellipse.edoi.ejb.msf040.*
import com.mincom.ellipse.edoi.ejb.msf096.*
import com.mincom.ellipse.edoi.ejb.msf220.*
import com.mincom.ellipse.edoi.ejb.msf221.*
import com.mincom.ellipse.edoi.ejb.msf230.*
import com.mincom.ellipse.edoi.ejb.msf231.*
import com.mincom.ellipse.edoi.ejb.msf810.*
import java.util.Date
import java.text.SimpleDateFormat
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyDTO
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO

//LOKEWS - Ver 3 
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Key;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Rec;
import org.apache.commons.lang.StringUtils;

/**
* Request Parameters for Trb22b. <br>
*
* <li>First request parameter is <code>paramDistrict</code> represents the District Code</li>
* <li>Second request parameter is <code>paramDate1</code> represents MSF220-ORDER-DATE From</li>
* <li>Third request parameter is <code>paramDate2</code> represents MSF220-ORDER-DATE To</li>
* <li>Fourth request parameter is <code>paramSign</code> conjunction with fifth parameter represents Positive or Negative / Advance Notification</li>
* <li>Fifth request parameter is <code>paramDaysNotice</code> conjunction with fourth parameter represents Positive or Negative / Advance Notification</li>
*/

    public class ParamsTrb22b{
        //List of Input Parameters
        String paramDistrict;
        String paramDate1;
		String paramDate2;
		String paramSign;
		String paramDaysNotice;
        
        //Restart Variables
        String restartTableCode = "    ";
    }
    
    public class ProcessTrb22b extends SuperBatch {
        /* 
         * IMPORTANT!
         * Update this Version number EVERY push to GIT 
         */
        private version = 3;
        private ParamsTrb22b batchParams;
		
		private class Trb22bReportLine{
			private String reqOff;
			private String reqSnam;
			private String reqFNam;
			private String authOff;
			private String authSnam;
			private String authFNam;
			private String poWh;
			private String item;
			private String date;
			private String desc1;
			private String desc2;
			private String desc3;
			private String desc4;
			public Trb22bReportLine(String reqOff, String reqSnam, String reqFNam,
					String authOff, String authSnam, String authFNam, String poWh,
					String item, String date, String desc1, String desc2,
					String desc3, String desc4) {
				super();
				this.reqOff = reqOff;
				this.reqSnam = reqSnam;
				this.reqFNam = reqFNam;
				this.authOff = authOff;
				this.authSnam = authSnam;
				this.authFNam = authFNam;
				this.poWh = poWh;
				this.item = item;
				this.date = date;
				this.desc1 = desc1;
				this.desc2 = desc2;
				this.desc3 = desc3;
				this.desc4 = desc4;
			}
			public String getReqOff() {
				return reqOff;
			}
			public void setReqOff(String reqOff) {
				this.reqOff = reqOff;
			}
			public String getReqSnam() {
				return reqSnam;
			}
			public void setReqSnam(String reqSnam) {
				this.reqSnam = reqSnam;
			}
			public String getReqFNam() {
				return reqFNam;
			}
			public void setReqFNam(String reqFNam) {
				this.reqFNam = reqFNam;
			}
			public String getAuthOff() {
				return authOff;
			}
			public void setAuthOff(String authOff) {
				this.authOff = authOff;
			}
			public String getAuthSnam() {
				return authSnam;
			}
			public void setAuthSnam(String authSnam) {
				this.authSnam = authSnam;
			}
			public String getAuthFNam() {
				return authFNam;
			}
			public void setAuthFNam(String authFNam) {
				this.authFNam = authFNam;
			}
			public String getPoWh() {
				return poWh;
			}
			public void setPoWh(String poWh) {
				this.poWh = poWh;
			}
			public String getItem() {
				return item;
			}
			public void setItem(String item) {
				this.item = item;
			}
			public String getDate() {
				return date;
			}
			public void setDate(String date) {
				this.date = date;
			}
			public String getDesc1() {
				return desc1;
			}
			public void setDesc1(String desc1) {
				this.desc1 = desc1;
			}
			public String getDesc2() {
				return desc2;
			}
			public void setDesc2(String desc2) {
				this.desc2 = desc2;
			}
			public String getDesc3() {
				return desc3;
			}
			public void setDesc3(String desc3) {
				this.desc3 = desc3;
			}
			public String getDesc4() {
				return desc4;
			}
			public void setDesc4(String desc4) {
				this.desc4 = desc4;
			}
			
		}
		
		private def reportA
		private String checkDate
		private String[] writeLine
		private String todaysDate
		private Trb22bReportLine reportLine
		private boolean firstLine = true
		
		//LOKEWS - Ver 3 - New parameter to keep email recipient name and email address
		private ArrayList emailRecipient = new ArrayList()
		//LOKEWS - Ver 3 - End
        
		/**
		* Run the main batch.
		* @param b is a binding object passed from the <code>ScriptRunner</code>
		*/
        public void runBatch(Binding b){            
            
            init(b);
            
            printSuperBatchVersion();
            info("runBatch Version : " + version);
            
            batchParams = params.fill(new ParamsTrb22b())
            
            //PrintRequest Parameters
			info("paramDistrict: " + batchParams.paramDistrict)
			info("paramDate1: " + batchParams.paramDate1)
			info("paramDate2: " + batchParams.paramDate2)
			info("paramSign: " + batchParams.paramSign)
			info("paramDaysNotice: " + batchParams.paramDaysNotice)
			
            try {
                    processBatch();
                
            } finally {
                    printBatchReport();
            }
        }
		
		/**
		* Main process of the batch.
		*/
        private void processBatch(){
            info("processBatch");           
            //write process
			reportA = report.open("TRB22BA")
			boolean error = initialise()
			
			if (!error){
				mainProcess()
			}
			reportA.close()
        }
        
        //additional method - start from here.
        
		/**
		* Initialise variable and write report header.
		*/
		private boolean initialise(){
			info("initialise")
			int daysNotice
			boolean error = validateParameter()
			
			if (!error){
				writeLine = new String[20]
				todaysDate = new Date().format("yyyyMMdd")
				if (batchParams.paramSign.equals("+")){
					daysNotice = batchParams.paramDaysNotice.toInteger()
				} else{
					daysNotice = -1 * batchParams.paramDaysNotice.toInteger()
				  }
				
				Calendar cal = Calendar.getInstance()
				cal.add(cal.DATE,daysNotice)
				checkDate = new SimpleDateFormat("yyyyMMdd").format(cal.getTime())
				
				info("CheckDate: "+checkDate)
				
				reportA.write("Control Report to indicate the E-mail sent for Order".center(132))
				reportA.writeLine(132,"-")
				reportA.write("                                                                                 Current")
				reportA.write("Requesting Officer              Authorising Officer             Order No   Item  Due Date  Description")
				reportA.writeLine(132,"-")
				return false
			}
			return true
		}
		
		/**
		* Validate request parameter.
		*/
		private boolean validateParameter(){
			info("validateParameter")
			if (batchParams.paramDistrict.trim().equals("")){
				info("*** ERROR: District Code is mandatory parameter ***")
				reportA.write("*** ERROR: District Code is mandatory parameter ***")
				return true
			}
			
			if (batchParams.paramSign.trim().equals("")){
				batchParams.paramSign = "-"
			}
			
			if (batchParams.paramDaysNotice.trim().equals("")){
				batchParams.paramDaysNotice = "180"
			}
			return false
		}
		
		/**
		* Get Purchase Order that match the selection criteria.
		*/
		private void mainProcess(){
			info("mainProcess")
			int msf220Count = 0
			try{
			
				Constraint c2 = MSF220Rec.status_220.lessThanEqualTo("2")
				Constraint c3 = MSF220Rec.liveConfInd.equalTo("L")
				Constraint c4
				Constraint c5
				if (batchParams.paramDistrict.trim().equals("")){
					c4 = MSF220Rec.dstrctCode.greaterThanEqualTo(" ")
				} else{
					c4 = MSF220Rec.dstrctCode.equalTo(batchParams.paramDistrict)
				  }
				
				if (batchParams.paramDate1.trim().equals("") && batchParams.paramDate2.trim().equals("")){
					c5 = MSF220Rec.orderDate.greaterThanEqualTo(" ")
				} else if(!batchParams.paramDate1.trim().equals("") && !batchParams.paramDate2.trim().equals("")){
						  c5 = MSF220Rec.orderDate.between(batchParams.paramDate1, batchParams.paramDate2)
				       } else if (!batchParams.paramDate1.trim().equals("")){
					   			  c5 = MSF220Rec.orderDate.greaterThanEqualTo(batchParams.paramDate1)
				       		  } else if (!batchParams.paramDate2.trim().equals("")){
								 		 c5 =  MSF220Rec.orderDate.lessThanEqualTo(batchParams.paramDate2)
				       		         }
				
				def query = new QueryImpl(MSF220Rec.class).and(c2.and(c3).and(c4).and(c5))
				
				edoi.search(query,{MSF220Rec msf220rec ->
						msf220Count++
						getPoItem(msf220rec.getPrimaryKey().getPoNo())
					})
				} catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
					}
				
				info ("MSF220 Count: "+ msf220Count)

		}
		
		/**
		* Get Purchase Order item Details.
		* @param poNo is Purchase Order Number
		*/
		private void getPoItem(String poNo){
			info("getPoItem")
			String preqNo
			String preqItemNo
			int msf221Count = 0
			try{
				Constraint c1 = MSF221Key.poNo.equalTo(poNo)
				Constraint c2 = MSF221Rec.status_221.lessThanEqualTo("2")
				Constraint c3 = MSF221Rec.priceCode.equalTo("WP")
				Constraint c4
				Constraint c5 = MSF221Rec.currDueDate.equalTo(checkDate)
				if (batchParams.paramDistrict.trim().equals("")){
					c4 = MSF221Rec.dstrctCode.greaterThanEqualTo(" ")
				} else{
					c4 = MSF221Rec.dstrctCode.equalTo(batchParams.paramDistrict)
				  }
				
				def query = new QueryImpl(MSF221Rec.class).and(c1.and(c2).and(c3).and(c4).and(c5))
				
				edoi.search(query,{MSF221Rec msf221rec ->
						msf221Count++
						info("PO No: " + poNo)
						info("PO Item No: " + msf221rec.getPrimaryKey().getPoItemNo())
						info("Curr Due Date: " + msf221rec.getCurrDueDate())
						if (msf221rec.getPreqStkCode().length() >= 6){
							preqNo = msf221rec.getPreqStkCode().substring(0,6)
						}
						
						if (msf221rec.getPreqStkCode().length() >= 9){
							preqItemNo = msf221rec.getPreqStkCode().substring(6,9)
						}
						

						for (int i=0; i<20; i++){
							writeLine[i] = " "
						}
						reportLine = new Trb22bReportLine(" "," "," "," "," "," "," "," "," "," "," "," "," ")
						reportLine.setItem(msf221rec.getPrimaryKey().getPoItemNo())
						reportLine.setDate(msf221rec.getCurrDueDate().substring(6,8) + "/" + msf221rec.getCurrDueDate().substring(4,6) + "/" + msf221rec.getCurrDueDate().substring(2,4))
						
						if (firstLine){
							firstLine = false
						} else{
							reportA.writeLine(132,"-")
							reportA.write("                                                                                 Current")
							reportA.write("Requesting Officer              Authorising Officer             Order No   Item  Due Date  Description")
							reportA.writeLine(132,"-")
						  }
						layoutEmailText(poNo, msf221rec.getPrimaryKey().getPoItemNo(), msf221rec.getWhouseId(), msf221rec.getDstrctCode(), msf221rec.getOrigDstCde(), preqNo, preqItemNo, msf221rec.getCurrDueDate())
						sendEmail(msf221rec.getOrigDstCde(),preqNo)
						prepareReport()

					})
				 
			} catch (com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e) {
			  }
			
			info ("MSF221Count : "+msf221Count)
		}
				
		/**
		* Write e-mail layout.
		* @param poNo is Purchase Order Number
		* @param poItemNo is Purchase Order Item Number
		* @param whouseId is PO Item warehouse Id
		* @param dstrctCode is PO Item District Code
		* @param origDstCde is PO Item Original District Code
		* @param purchReq is Purchase Requisition
		* @param preqItem is Purchase Requisition Item
		* @param currDueDate is PO Item Current Due Date
		*/
		private void layoutEmailText(String poNo, String poItemNo, String whouseId, String dstrctCode, String origDstCde, String purchReq, String preqItem, String currDueDate){
			info ("layoutEmailText")
			if (batchParams.paramSign.equals("+") || batchParams.paramDaysNotice.toInteger() == 0) {
				writeLine[0] = "This is to advise you that Period Order number " + poNo + "-" + whouseId + " will be expiring on the" 
				writeLine[1] = reportLine.getDate()
				reportLine.setPoWh(poNo + "-" + whouseId)
			} else{
					writeLine[0] = "This is to advise you that Period Order number " + poNo + "-" + whouseId + " has expired on the"
					writeLine[1] = reportLine.getDate()
					reportLine.setPoWh(poNo + "-" + whouseId)
			   }
			
			if (currDueDate.equals(todaysDate)){
				writeLine[2] = "This order cannot be used after today and will be completed after all payments"
				writeLine[3] = "have been made."
			}

			writeLine[4] = "The period order description is as follows:"
			writeLine[6] = "Itm   Current Due Date  Description"
			writeLine[7] = "--------------------------------------------------------------------------------"
			
			getItemDesc(poItemNo, origDstCde, purchReq, preqItem)
			getStdText(dstrctCode, purchReq, preqItem)
			
			//LOKEWS - Ver 3 - Change Message - Start
			//writeLine[16] = "Please take steps to have a new period order raised by initiating a purchase"
			//writeLine[17] = "requisition. If you have any queries on raising a purchase requisition please"
			//writeLine[18] = "contact your local Supply Officer or if you are in head office the Purchasing"
			//writeLine[19] = "Supervisor on ext 3212."
			writeLine[16] = "Please take steps to have a new period order raised by initiating a purchase"
			writeLine[17] = "requisition at least two months prior to the expiry date of the order."
			writeLine[18] = "If you have any queries please contact the Commodity Manager on 900 257."
			writeLine[19] = " "
			//LOKEWS - Ver 3 - End
		}
		
		/**
		* Get Purchase Requisition Item Description.
		* @param poItemNo is Purchase Order Item Number
		* @param origDstCde is PO Item Original District Code
		* @param purchReq is Purchase Requisition
		* @param preqItem is Purchase Requisition Item
		*/
		private void getItemDesc(String poItemNo, String origDstCde, String purchReq, String preqItem){
			info("getItemDesc")
			try{
				MSF231Rec msf231rec = edoi.findByPrimaryKey(new MSF231Key(origDstCde, purchReq, preqItem))
				reportLine.setDesc1(msf231rec.getItemDescx1())

				writeLine[8] = poItemNo.padRight(6) + reportLine.getDate().padRight(18) + msf231rec.getItemDescx1()
				
				if (!msf231rec.getItemDescx2().equals(" ")){
					writeLine[9] = " ".padRight(24) + msf231rec.getItemDescx2()
					reportLine.setDesc2(msf231rec.getItemDescx2())
				}
				
				if (!msf231rec.getItemDescx3().equals(" ")){
					writeLine[10] = " ".padRight(24) + msf231rec.getItemDescx3()
					reportLine.setDesc3(msf231rec.getItemDescx3())
				}
				
				if (!msf231rec.getItemDescx4().equals(" ")){
					writeLine[11] = " ".padRight(24) + msf231rec.getItemDescx4()
					reportLine.setDesc4(msf231rec.getItemDescx4())
				}
				
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			}
		}
		
		/**
		* Get Purchase Requisition Standard Text.
		* @param dstrctCode is Purchase requisition District Code
		* @param purchReq is Purchase Requisition
		* @param preqItem is Purchase Requisition Item
		*/
		private void getStdText(dstrctCode, purchReq, preqItem){
			info("getStdText")
			String stdKey,stdText,tempString = ""
			int index = 12, nextString = 0
			stdKey = "PR" + dstrctCode + purchReq + preqItem
			
			try{
				(service.get('StdText')).getText({Object it ->
					it.stdTextId = stdKey }).replyElements.each {
					   stdText = it.textLine.toString()
					   
					   /* This service call will return textLine with format as below:
					    * [Text Line1, TextLine2, TextLine3] 
					    * so we need to split the string
					    */
					   
					   for (int i=0;i<stdText.length();i++){
						   if (stdText.substring(i,i+1).equals(",")||(i==stdText.length()-1) && index <= 14){
							   writeLine[index] = " ".padRight(24) + tempString
							   index++
							   tempString = ""
							   nextString = i+1
						   } else{
								  if (i!=0 && i!=nextString){
									tempString = tempString + stdText.substring(i,i+1)
								  }
							   }
					   }
					}
			} catch (Exception ex) {
				println ex.toString()
			}
		}
		
		/**
		* Send notification e-mail to MSF230-Requested-By
		* @param dstrctCode is Purchase requisition District Code
		* @param purchReq is Purchase Requisition
		*/
		private void sendEmail(String origDstCde, String purchReq){
			info("sendEmail")
			String mailTo, subject, pathName, mailFrom, host

			try{
				MSF230Rec msf230rec = edoi.findByPrimaryKey(new MSF230Key(origDstCde, purchReq))
				

				reportLine.setReqOff(msf230rec.getRequestedBy())
				reportLine.setAuthOff(msf230rec.getAuthsdBy())
				
				try{
					//LOKEWS - Ver 3 - Get email address (position no or requested by - Start
					//MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(msf230rec.getRequestedBy()))
					//mailTo = msf810rec.getEmailAddress()
					String positionId = msf230rec.getReqByPos();
					emailRecipient = new ArrayList()				        
	
					//If position not empty, get employees & email address
					if(positionId != null && positionId.trim().length() > 0) {
						
						String todayDate = new SimpleDateFormat("yyyyMMdd").format(new Date())
						String invStrDate   = String.valueOf(getInvDate(getInteger(todayDate)))
						
						Constraint consPposId = MSF878Key.positionId.equalTo(positionId);
						Constraint consPriPos = MSF878Key.primaryPos.equalTo("0");
						Constraint consInvStrDate = MSF878Key.invStrDate.greaterThan(invStrDate);
						Constraint consPosDate1 = MSF878Key.posStopDate.equalTo("00000000");
						Constraint consPosDate2 = MSF878Key.posStopDate.greaterThanEqualTo(todayDate);
						
						def query = new QueryImpl(MSF878Rec.class).and(consPposId).and(consPriPos).and(consInvStrDate).and((consPosDate1).or(consPosDate2))
						
						edoi.search(query).results.each {MSF878Rec rec ->
							MSF878Key key = (MSF878Key)rec.getKey();
							String employeeID = key.employeeId;
							if(employeeID != null && employeeID.trim().length() > 0) {
								MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(employeeID));
								mailTo = appendEmail(mailTo, msf810rec.getEmailAddress())
								emailRecipient.add(msf810rec.getFirstName() + " " + msf810rec.getSurname() + "<<" + msf810rec.getEmailAddress() + ">>")
							}
						}
						
					}
					
					//If no email return from position detail, get email from Requested By
					if(mailTo == null || mailTo.trim().length() == 0) {
						MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(msf230rec.getRequestedBy()))
						
						mailTo = msf810rec.getEmailAddress()
						
						emailRecipient.add(msf810rec.getFirstName() + " " + msf810rec.getSurname() + "<<" + msf810rec.getEmailAddress() + ">>")
					}
					//LOKEWS - Ver 3 - End
					
					if (!mailTo.trim().equals("")){
						subject = "<< Period Order Expiry Notification >>  "
						ArrayList message = new ArrayList()
					
						for (int i=0;i<20;i++){
							message.add(writeLine[i])
						}
						
						pathName = " "
						mailFrom = " "
						host = " "
						
						SendEmail myEmail = new SendEmail(subject,mailTo,message,pathName,mailFrom,host,false)
						myEmail.sendMail()
					}					
					
				}
				catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
					info ("E-mail Adress Not Found for employee "+ msf230rec.getRequestedBy())
				}
				
			}
			catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
			}
		}
		
		/**
		* Write report layout.
		*/
		private void prepareReport(){
			info ("prepareReport")
			String emailAdress = " "
			if (!reportLine.getReqOff().equals(" ")){
				try{
					MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(reportLine.getReqOff()))
					reportLine.setReqSnam(msf810rec.getSurname())
					reportLine.setReqFNam(msf810rec.getFirstName())
					emailAdress = msf810rec.getEmailAddress()
				}
				catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				}
			}
			
			if (!reportLine.getAuthOff().equals(" ")){
				try{
					MSF810Rec msf810rec = edoi.findByPrimaryKey(new MSF810Key(reportLine.getAuthOff()))
					reportLine.setAuthSnam(msf810rec.getSurname())
					reportLine.setAuthFNam(msf810rec.getFirstName())

				}
				catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
				}
			}
			
			String reqFullName = reportLine.getReqSnam() + "," + reportLine.getReqFNam()
			String authFullName = reportLine.getAuthSnam() + "," + reportLine.getAuthFNam()
			reportA.write(reportLine.getReqOff().substring(5,10).padRight(8)+
				reqFullName.padRight(24)+
				reportLine.getAuthOff().substring(5,10).padRight(8)+
				authFullName.padRight(24)+
				reportLine.getPoWh().padRight(11)+
				reportLine.getItem().padRight(6)+
				reportLine.getDate().padRight(10)+reportLine.getDesc1())
			
			if (!reportLine.getDesc2().equals(" ")){
				reportA.write(" ".padRight(91) + reportLine.getDesc2())
			}
			if (!reportLine.getDesc3().equals(" ")){
				reportA.write(" ".padRight(91) + reportLine.getDesc3())
			}
			if (!reportLine.getDesc4().equals(" ")){
				reportA.write(" ".padRight(91) + reportLine.getDesc4())
			}
			
			reportA.write(" ")
			//LOKEWS - Ver 3 - Prepare email recipient line (if more than 1)
			//reportA.write("To:           "+reportLine.getReqFNam()+" "+reportLine.getReqSnam()+"<<"+emailAdress+">>")
			String emailRecipientLine = constructEmailRecLine()
			reportA.write("To:           "+emailRecipientLine)
			//LOKEWS - Ver 3 - End
			reportA.write("Subject:      "+"<< Period Order Expiry Notification >>  ")
			reportA.write("MESSAGE:      ")
			for (int i=0;i<=8;i++){
				reportA.write(writeLine[i])
			}
			
			if (!reportLine.getDesc2().equals(" ")){
				reportA.write(writeLine[9])
			}
			if (!reportLine.getDesc3().equals(" ")){
				reportA.write(writeLine[10])
			}
			if (!reportLine.getDesc4().equals(" ")){
				reportA.write(writeLine[11])
			}
			
			if (!writeLine[12].equals(" ")){
				reportA.write(reportLine.getItem().padRight(6)+reportLine.getDate().padRight(10)+writeLine[12])
			}
			if (!writeLine[13].equals(" ")){
				reportA.write(reportLine.getItem().padRight(6)+reportLine.getDate().padRight(10)+writeLine[13])
			}
			if (!writeLine[14].equals(" ")){
				reportA.write(reportLine.getItem().padRight(6)+reportLine.getDate().padRight(10)+writeLine[14])
			}
				
			for (int i=15;i<=19;i++){
				reportA.write(writeLine[i])
			}
			
			reportA.write(" ")

		}
			
        private void printBatchReport(){
            info("printBatchReport")
            //print batch report
        }
        
        //LOKEWS - Ver 3 - Add new method to query MSF878
		private int getInvDate(int date) {
			info("getInvDate")
			return 99999999 - date
		}
		
		private int getInteger(String s){
			info("getInteger")
	
			info("Converting string value: ${s} to integer ...")
			int retVal = 0
			try {
				retVal = Integer.parseInt((StringUtils.isNotBlank(s) ? StringUtils.trim(s) : "0"))
			} catch (NumberFormatException e) {
				info("Failed to convert value: ${s} to integer!")
			}
			return retVal
		}
		
		private String appendEmail(String returnEmail, String email) {
			info("appendEmail")
			
			if(email == null || email.trim().length() == 0) {
				return returnEmail;
			} else if(returnEmail == null || returnEmail.trim().length() == 0) {
				return email;
			} else {
				return returnEmail + ";" + email;
			}
		}
		
		private String constructEmailRecLine() {
			info("constructEmailRecLine")
			
			String emailRecLine = "";
			
			if(emailRecipient != null && emailRecipient.size() > 0) {
				for(int i = 0; i < emailRecipient.size(); i++) {
					emailRecLine = emailRecLine + emailRecipient.get(i) + "; ";
				}
				
				emailRecLine = emailRecLine.substring(0, emailRecLine.length() - 2)
			}
			
			return emailRecLine;
		}
		//LOKEWS - Ver 3 - End
    }
        
/*run script*/  
ProcessTrb22b process = new ProcessTrb22b();
process.runBatch(binding);
