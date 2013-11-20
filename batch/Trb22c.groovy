/**
* @Ventyx 2012
* Conversion from trb22bc.cbl
*
* This program sends an email advising the requesting officer of <br>
* an impending expiry of their period order as today date.
*
*Revision History 
*************************
* Date        Name     Desc											        Ver
* 26/08/2013  LokeWS   SC4338099_WO25885 Enhancement  to TRB22BA & TRB22CA Version 5
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
import com.mincom.enterpriseservice.ellipse.employee.EmployeeServiceReadReplyDTO;
import com.mincom.enterpriseservice.ellipse.stdtext.StdTextServiceGetTextReplyCollectionDTO;
import com.mincom.eql.Constraint;
import com.mincom.eql.impl.QueryImpl;

//LOKEWS - Ver 5 - ADD
import org.apache.commons.lang.StringUtils;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Rec;
import com.mincom.ellipse.edoi.ejb.msf878.MSF878Key;

/**
* Request Parameters for Trb22c:
* <li><code>districtCode</code>: District Code (Blank for all)</li>
* <li><code>date1</code>: Order Date      - From (Blank for all)</li>
* <li><code>date2</code>: Order Date      - To   (Blank for all)</li>
* <li><code>sign</code>: Positive or Negative (+,-)</li>
* <li><code>period</code>: Advance notification period in days</li>
*/
public class ParamsTrb22c {
 String districtCode
 String date1
 String date2
 String sign
 String period
}

/**
* Report Content for Trb22c
*/
public class ReportTrb22c {
 private String reqEmailAddress = " ", reqOff= " ", reqSNam= " ", reqFNam= " ", authOff= " ", authSNam= " ", authFNam= " ", poWh= " ", wpItem= " ", date= " ", description= " "
 
 //LOKEWS - Ver 5 - cater for more than 1 email address
 private ArrayList emailRecipient = new ArrayList()
 //LOKEWS - Ver 5 - End

 /**
  * Return report title literal.
  * @return report title "Control Report to indicate the E-mail sent for Order"
  */
 public String getReportTitle() {
   return "Control Report to indicate the E-mail sent for Order".center(132)
 }

 /**
  * Return report header literal.
  * @return report header literal
  */
 public String getReportHeader() {
   return String.format("%-34s%-34s%-14s  %-17s  %-34s", "Requesting Officer", "Authorising Officer", "Order No Item", "Current Due Date", "Description")
 }

 /**
  * Return report detail at first line.
  * @return report detail at first line
  */
 public String getReportDetailLine1() {
   return String.format("%-8s %-12s %-11s %-8s %-12s %-11s %-9s %-3s   %-16s   %-40s", reqOff, reqSNam, reqFNam, authOff, authSNam, authFNam, poWh, wpItem, date, description)
 }

 /**
  * Return report detail at second line.
  * @return report detail at second line
  */
 public String getReportDetailLine2() {
   return String.format("%-102s %-40s", " ", description)
 }

 /**
  * Return report detail at third line.
  * @return report detail at third line
  */
 public String getReportDetailLine3() {
   //LOKEWS - Ver 5 - Prepare email recipient line (if more than 1)
   //return String.format("To:           %-12s %-12s <<%s>>", reqFNam, reqSNam, reqEmailAddress)
   return String.format("To:           " + constructEmailRecLine())
   //LOKEWS - Ver 5 - End
 }
 
 //LOKEWS - Ver 5 - Prepare email recipient line (if more than 1)
 private String constructEmailRecLine() {
	 String emailRecLine = "";
	 
	 if(emailRecipient != null && emailRecipient.size() > 0) {
		 for(int i = 0; i < emailRecipient.size(); i++) {
			 emailRecLine = emailRecLine + emailRecipient.get(i) + "; ";
		 }
		 
		 emailRecLine = emailRecLine.substring(0, emailRecLine.length() - 2)
	 }
	 
	 return emailRecLine;
 }
 //LOKEWS - Ver 5 - End
}

/**
* Email Content for Trb22c, represents the 20 lines of e-mail message text.
*/
public class EmailContentTrb22C {

 static final Integer REPORT_LINES_COUNTER = 20
 String requestedBy= " ", authorizedBy= " ", po_WHouse= " ", poItem= " ", currDueDate= " ", description1= " ", description2= " ", description3= " ", description4= " ", stdStaticArr1= " ", stdStaticArr2= " ", stdStaticArr3= " ",mailAddress= " "
 String[] lines = new String[REPORT_LINES_COUNTER]

 /**
  * Initialize <code>EmailContentTrb22C</code>. Fill each lines with blank literal.
  */
 public EmailContentTrb22C() {
   REPORT_LINES_COUNTER.times {
     lines[it] = "".padRight(80)
   }
 }

 /**
  * Generate email content from the lines as a String excluding whitespace characters.
  * @return a joined email lines as a String excluding whitespace characters.
  */
 public String generateEmailBodyExcludeWhiteSpace() {
   String body = ""
   int i =0
   lines.each {       
     if(it?.trim()) {
       body += it + "\n"
     }
     if(i==3||i==13)
       body = body + "\n"
     i++
   }
   return body
 }

 /**
  * Construct default email's body.
  */
 public void constructBodyEmail() {
   lines[0]  = String.format("This is to advise you that Period Order number %s will expire today. This", po_WHouse)
   lines[1]  = "order cannot be used after today and will be completed after all payments have"
   lines[2]  = "been made. The period order description is as follows:"
   lines[4]  = "Itm   Current Due Date   Description"
   lines[5]  = String.format("%80s", " ").replace(' ', '-')
   lines[6]  = String.format("%-3s   %-16s   %-40s", poItem, currDueDate, description1)
   lines[7]  = String.format("%24s %-40s", " ", description2)
   lines[8]  = String.format("%24s %-40s", " ", description3)
   lines[9]  = String.format("%24s %-40s", " ", description4)
   lines[10] = String.format("%-80s", stdStaticArr1)
   lines[11] = String.format("%-80s", stdStaticArr2)
   lines[12] = String.format("%-80s", stdStaticArr3)
   //LOKEWS - Ver 5 - Change message
   //lines[14] = "Please take steps to have a new period order raised by initiating a purchase"
   //lines[15] = "requisition. If you have any queries on raising a purchase requisition please"
   //lines[16] = "contact your local Supply Officer or if you are in head office the Purchasing"
   //lines[17] = "Supervisor on ext 3212."
   lines[14] = "Please take steps to have a new period order raised by initiating a purchase"
   lines[15] = "requisition at least two months prior to the expiry date of the order."
   lines[16] = "If you have any queries please contact the Commodity Manager on 900 257."
   lines[17] = " "
   //LOKEWS - Ver 5 - End
 }

 /**
  * Return the default email subject.
  * @return default email subject "<< Period Order Expiry Notification >>"
  */
 public String getSubject() {
   return "<< Period Order Expiry Notification >>"
 }
}

/**
* Main Process of Trb22c
*/
public class ProcessTrb22c extends SuperBatch {

 /*
  * Constants
  */
 private static final String MSF220_LIVE_CONF_IND = "L"
 private static final String MSF221_PRICE_CODE = "WP"
 private static final String MSF221_MSF220_STATUS = "2"
 private static final String REPORT_NAME = "TRB22CA"
 /*
  * IMPORTANT!
  * Update this Version number EVERY push to GIT
  */
 private int version = 5 

 /*
  * Variables
  */
 private def reportWriter
 private ParamsTrb22c batchParams
 private ReportTrb22c reportLine
 private EmailContentTrb22C emailContent
 private boolean aborted = false
 private int rowCount = 0
 private String todayDate, todayTime
 
 //LOKEWS - Ver 5 - new variable
 private String reqPosNo;
 //LOKEWS - Ver 5 - End

 /**
  * Run the main batch.
  * @param b a <code>Binding</code> object passed from <code>ScriptRunner</code>
  */
 public void runBatch(Binding b) {
   init(b);
   printSuperBatchVersion();
   info("runBatch Version : " + version);
   //Get request parameters
   batchParams = params.fill(new ParamsTrb22c())
   //Print request Parameters
   info("paramDistrict : " + batchParams.districtCode)
   info("paramDate1    : " + batchParams.date1)
   info("paramDate2    : " + batchParams.date2)
      
   try {
     processBatch()
   } catch(Exception e) {
     e.printStackTrace()
     info("processBatch failed - ${e.getMessage()}")
     aborted = true
   } finally {
     printBatchReport()
   }
 }

 /**
  * Process the main batch.
  */
 private void processBatch() {
   info("processBatch")
   initialize()
   browsePurchaseOrder()
 }

 /**
  * Print the batch report.
  */
 private void printBatchReport() {
   info("printBatchReport")
   reportWriter.close()
 }

 //additional method - start from here.
 /**
  * Initialize the report writer and other variables.
  */
 private void initialize() {
   info("initialize")
   //parse the today date and time
   Date currentDate = new Date()
   SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd")
   SimpleDateFormat tf = new SimpleDateFormat("hhmmss")
   todayDate = df.format(currentDate)
   todayTime = tf.format(currentDate)

   //initialize and open report
   reportLine = new ReportTrb22c()
   reportWriter = report.open(ProcessTrb22c.REPORT_NAME)
   reportWriter.write(reportLine.getReportTitle())
   reportWriter.write("")
 }

 /**
  * Browse Purchase Order records with specified constraints.
  */
 private void browsePurchaseOrder() {
   info("browsePurchaseOrder")
   Constraint cStatus220 = MSF220Rec.status_220.lessThanEqualTo(MSF221_MSF220_STATUS)
   Constraint cDstrctCode = batchParams.districtCode?.trim() ? MSF220Rec.dstrctCode.equalTo(batchParams.districtCode) : MSF220Rec.dstrctCode.greaterThanEqualTo(" ")
   Constraint cLiveConfId = MSF220Rec.liveConfInd.equalTo(MSF220_LIVE_CONF_IND)
   QueryImpl query = new QueryImpl(MSF220Rec.class).and(cStatus220).and(cDstrctCode).and(cLiveConfId)
   if(batchParams.date1?.trim() && batchParams.date2?.trim()) {
     Constraint cOrderDateBetween1And2 = MSF220Rec.orderDate.between(batchParams.date1, batchParams.date2)
     query = new QueryImpl(MSF220Rec.class).and(cStatus220).and(cDstrctCode).and(cLiveConfId).and(cOrderDateBetween1And2)
   }
   else {
     if(batchParams.date1?.trim()) {
       Constraint cOrderDate1 = MSF220Rec.orderDate.greaterThanEqualTo(batchParams.date1)
       query = new QueryImpl(MSF220Rec.class).and(cStatus220).and(cDstrctCode).and(cLiveConfId).and(cOrderDate1)
     }
     if(batchParams.date2?.trim()) {
       Constraint cOrderDate2 = MSF220Rec.orderDate.lessThanEqualTo(batchParams.date2)
       query = new QueryImpl(MSF220Rec.class).and(cStatus220).and(cDstrctCode).and(cLiveConfId).and(cOrderDate2)
     }
   }
   edoi.search(query, { MSF220Rec msf220Rec->
     browsePurchaseOrderItem(msf220Rec)
   })
 }

 /**
  * Browse Purchase Order Item based on Purchase Order record.
  * @param msf220Rec a Purchase Order (<code>MSF220Rec</code>) record to be proceed.
  */
 private void browsePurchaseOrderItem(MSF220Rec msf220Rec) {
   info("browsePurchaseOrderItem")
   Constraint cPoNo = MSF221Key.poNo.equalTo(msf220Rec.getPrimaryKey().poNo)
   Constraint cDstrctCode = batchParams.districtCode?.trim() ? MSF221Rec.dstrctCode.equalTo(batchParams.districtCode) : MSF221Rec.dstrctCode.greaterThanEqualTo(" ")
   Constraint cPriceCode = MSF221Rec.priceCode.equalTo(MSF221_PRICE_CODE)
   Constraint cStatus221 = MSF221Rec.status_221.lessThanEqualTo(MSF221_MSF220_STATUS)
   def query = new QueryImpl(MSF221Rec.class).and(cPoNo).and(cStatus221).and(cPriceCode).and(cDstrctCode)
   edoi.search(query, { MSF221Rec msf221Rec->
     if(msf221Rec.currDueDate.toString().equals(todayDate)) {
       layoutEmailText(msf220Rec, msf221Rec)
       sendEmail(msf221Rec)
       writeReport()
     }
   })
 }

 /**
  * Layout the email content based on Purchase Order record and Purchase Order Item record.
  * @param msf220Rec a Purchase Order (<code>MSF220Rec</code>) record to be proceed
  * @param msf221Rec a Purchase Order Item (<code>MSF221Rec</code>) record to be proceed
  */
 private void layoutEmailText(MSF220Rec msf220Rec, MSF221Rec msf221Rec) {
   info("layoutEmailText")
   emailContent = new EmailContentTrb22C()
   emailContent.po_WHouse = msf220Rec.getPrimaryKey().poNo + "-" + msf221Rec.whouseId
   readPurchaseRequisitionItem(msf221Rec)
   readStandardText(msf221Rec)
   //After all email information collected, construct the email body
   emailContent.constructBodyEmail()
   //LOKEWS - Ver 5 - clear report email line
   reportLine.emailRecipient = new ArrayList()
   //LOKEWS - Ver 5 - End
 }

 /**
  * Send the email based on the information from Purchase Order Item record.
  * @param msf221Rec a Purchase Order Item (<code>MSF221Rec</code>) record to be proceed
  */
 private void sendEmail(MSF221Rec msf221Rec) {
   info("sendEmail")
   readPurchaseRequisition(msf221Rec)
   readEmployeeEmail(emailContent.requestedBy)
   /*
    * Use SendEmail to send an email insted of executing macro from MSS040LINK
    */
   ArrayList content = new ArrayList()
   content.addAll(emailContent.lines)
   SendEmail sendEmail = new SendEmail(emailContent.getSubject(), emailContent.mailAddress, content)
   sendEmail.sendMail()
   if(sendEmail.isError()) {
	   info("Error during send email ${sendEmail.getErrorMessage()}")
   }

 }

 /**
  * Write the detail of the report.
  */
 private void writeReport() {
   info("writeReport")
   String requestedBy  = emailContent.requestedBy
   requestedBy = requestedBy.padRight(10)
   String authorizedBy = emailContent.authorizedBy
   authorizedBy = authorizedBy.padRight(10)
   reportLine.reqOff   = requestedBy.substring(5)
   reportLine.authOff  = authorizedBy.substring(5)

   try {
     MSF810Rec req810Rec = edoi.findByPrimaryKey(new MSF810Key(requestedBy))
     reportLine.reqSNam  = req810Rec.surname
     reportLine.reqFNam  = req810Rec.firstName
     reportLine.reqEmailAddress = req810Rec.emailAddress
   } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
     info("MSF810 Requested Employee ${requestedBy} does not exist")
   }
   try {
     MSF810Rec auth810Rec = edoi.findByPrimaryKey(new MSF810Key(authorizedBy))
     reportLine.authSNam  = auth810Rec.surname
     reportLine.authFNam  = auth810Rec.firstName
   } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
     info("MSF810 Authorized Employee ${authorizedBy} does not exist")
   }
   reportLine.wpItem = emailContent.poItem
   reportLine.date   = emailContent.currDueDate
   reportLine.poWh   = emailContent.po_WHouse
   reportLine.description = emailContent.description1
   //rewrite the header
   reportWriter.writeLine(132,"-")
   reportWriter.write(reportLine.getReportHeader())
   reportWriter.writeLine(132,"-")
   reportWriter.write(reportLine.getReportDetailLine1())
   if(emailContent.description2?.trim()) {
     reportLine.description = emailContent.description2
     reportWriter.write(reportLine.getReportDetailLine2())
   }
   if(emailContent.description3?.trim()) {
     reportLine.description = emailContent.description3
     reportWriter.write(reportLine.getReportDetailLine2())
   }
   if(emailContent.description4?.trim()) {
     reportLine.description = emailContent.description4
     reportWriter.write(reportLine.getReportDetailLine2())
   }
   reportWriter.write("")
   reportWriter.write(reportLine.getReportDetailLine3())
   reportWriter.write(String.format("Subject:      %-40s", emailContent.getSubject()))
   reportWriter.write("MESSAGE:      ")
   reportWriter.write("")
   reportWriter.write(emailContent.generateEmailBodyExcludeWhiteSpace())
   reportWriter.write("")
 }

 /**
  * Convert the date format with specified separator <code>(YY/MM/DD)</code>.
  * @param dateS date as a String
  * @param separator separator for the
  * @return formatted date as a String
  */
 private String convertDateFormat(String dateS, String separator) {
   info("convertDateFormat")
   dateS = dateS.padRight(8)
   if(!separator?.trim()) {
     separator = "/"
   }
   def formattedString = dateS.substring(6) + separator + dateS.substring(4,6) + separator + dateS.substring(2,4)
   return formattedString
 }

 /**
  * Read Purchase Requisition Item based the information from Purchase Order Item record.
  * @param msf221Rec a Purchase Order Item (<code>MSF221Rec</code>) record to be proceed
  */
 private void readPurchaseRequisitionItem(MSF221Rec msf221Rec) {
   info("readPurchaseRequisitionItem")
   def origDstrctCode = msf221Rec.origDstCde
   def preqNo = msf221Rec.preqStkCode.toString().padRight(9).substring(0, 6)
   def preqItemNo = msf221Rec.preqStkCode.toString().padRight(9).substring(6, 9)
   try {
     MSF231Rec msf231Rec = edoi.findByPrimaryKey(new MSF231Key(origDstrctCode, preqNo, preqItemNo))
     emailContent.poItem = msf221Rec.getPrimaryKey().poItemNo
     emailContent.currDueDate = convertDateFormat(msf221Rec.currDueDate, "/")
     emailContent.description1 = msf231Rec.itemDescx1
     if(msf231Rec.itemDescx2.toString()?.trim()) {
       emailContent.description2 = msf231Rec.itemDescx2
     }
     if(msf231Rec.itemDescx3.toString()?.trim()) {
       emailContent.description3 = msf231Rec.itemDescx3
     }
     if(msf231Rec.itemDescx4.toString()?.trim()) {
       emailContent.description4 = msf231Rec.itemDescx4
     }
   } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
     info("MSF231 ${origDstrctCode} - ${preqNo} - ${preqItemNo} does not exist")
   }
 }

 /**
  * Read Standard Text based on the information from Purchase Order Item record.
  * @param msf221Rec a Purchase Order Item (<code>MSF221Rec</code>) record to be proceed
  */
 private void readStandardText(MSF221Rec msf221Rec) {
   info("readStandardText")
   def stdTextCode = "PR"
   def stdKey      = msf221Rec.dstrctCode + msf221Rec.preqStkCode
   def stdLineNo   = "0000"
   try {
     MSF096_STD_VOLATRec msf096Rec = edoi.findByPrimaryKey(new MSF096_STD_VOLATKey(stdTextCode, stdKey, stdLineNo))
     if(msf096Rec.stdVolat_1.toString()?.trim()) {
       emailContent.stdStaticArr1 = msf096Rec.stdVolat_1
     }
     if(msf096Rec.stdVolat_2.toString()?.trim()) {
       emailContent.stdStaticArr2 = msf096Rec.stdVolat_2
     }
     if(msf096Rec.stdVolat_3.toString().trim()) {
       emailContent.stdStaticArr3 = msf096Rec.stdVolat_3
     }
   } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
     info("MSF096 ${stdTextCode} - ${stdKey} - ${stdLineNo} does not exist")
   }
 }

 /**
  * Read Purchase Requisition based the information from Purchase Order Item record.
  * @param msf221Rec a Purchase Order Item (<code>MSF221Rec</code>) record to be proceed
  */
 private void readPurchaseRequisition(MSF221Rec msf221Rec) {
   info("readPurchaseRequisition")
   def origDstrctCode = msf221Rec.origDstCde
   def preqNo = msf221Rec.preqStkCode.toString().padRight(9).substring(0, 6)
   try {
     MSF230Rec msf230Rec = edoi.findByPrimaryKey(new MSF230Key(origDstrctCode, preqNo))
     emailContent.requestedBy  = msf230Rec.requestedBy
     emailContent.authorizedBy = msf230Rec.authsdBy
     //LOKEWS - Ver 5 - Get Req Pos No - start
	 reqPosNo = msf230Rec.reqByPos 
	 //LOKEWS - Ver 5 - END     
   } catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException e) {
     info("MSF230 ${origDstrctCode} - ${preqNo} does not exist")
   }
 }

/**
* Read EmployeeInformation based the information from purchase requisition request by.
*/
private void readEmployeeEmail(String empId) {
 info("readEmployeeEmail")
  try{
    //LOKEWS - Ver 5 - Get employee Under request by position number first
    //MSF810Rec req810Rec = edoi.findByPrimaryKey(new MSF810Key(empId))
    //emailContent.mailAddress = req810Rec.getEmailAddress()
    //If Request Position No is not blank, get employees under the position
	  if(reqPosNo != null && reqPosNo.trim().length() > 0) {
		  String todayDate = new SimpleDateFormat("yyyyMMdd").format(new Date())
		  String invStrDate   = String.valueOf(getInvDate(getInteger(todayDate)))
		  
		  Constraint consPposId = MSF878Key.positionId.equalTo(reqPosNo);
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
				   emailContent.mailAddress = appendEmail(emailContent.mailAddress, msf810rec.getEmailAddress())
				   reportLine.emailRecipient.add(msf810rec.getFirstName().padRight(12) + " " + msf810rec.getSurname().padRight(12) + "<<" + msf810rec.getEmailAddress() + ">>")
			  }
		  }
	  }
	  
	  //If no email returned from position id, get email from Requested By
	  if(emailContent.mailAddress == null || emailContent.mailAddress.trim().length() == 0) {
		  MSF810Rec req810Rec = edoi.findByPrimaryKey(new MSF810Key(empId))
		  emailContent.mailAddress = req810Rec.getEmailAddress()
		  reportLine.emailRecipient.add(req810Rec.getFirstName().padRight(12) + " " + req810Rec.getSurname().padRight(12) + "<<" + req810Rec.getEmailAddress() + ">>")
	  }
    //LOKEWS - Ver 5 - End
  }
  catch(com.mincom.ellipse.edoi.common.exception.EDOIObjectNotFoundException  e){
    info ("E-mail Adress Not Found for employee ${empId}")
  }
}

//LOKEWS - Ver 5 - Add new method
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
//LOKEWS - Ver 5 - End
}
/**
* Run the script.
*/
ProcessTrb22c process = new ProcessTrb22c()
process.runBatch(binding)
