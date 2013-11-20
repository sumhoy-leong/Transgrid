/**
 * @Ventyx 2012
 */
package com.mincom.ellipse.script.custom

import java.text.SimpleDateFormat

import org.slf4j.LoggerFactory

import com.mincom.ellipse.edoi.ejb.msf000.MSF000_DC0001Key
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_DC0001Rec
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_DC0002Key
import com.mincom.ellipse.edoi.ejb.msf000.MSF000_DC0002Rec
import com.mincom.ellipse.edoi.ejb.msf080.MSF080Rec
import com.mincom.ellipse.edoi.ejb.msf081.MSF081Key
import com.mincom.ellipse.edoi.ejb.msf081.MSF081Rec
import com.mincom.ellipse.script.util.CommAreaScriptWrapper
import com.mincom.ellipse.script.util.EDOIWrapper
import com.mincom.eql.*
import com.mincom.eql.impl.*
import com.mincom.reporting.text.TextReport

class BatchTextReports {

	public Integer reportWidth
	public Integer maxLine
	public MSF080Rec batch080Details
	public String reportName
	public List <String> pageHeadings
	public List <String> columns
	public boolean prntHeadAtSetColumn

	private Integer pageNo
	private Integer lineNo
	private EDOIWrapper edoi
	private TextReport report
	private CommAreaScriptWrapper commarea
	private String ellipseDateFormat
	private def date = new Date()
	private String ellipseVersion
	private String districtName
	private String reportTitle
	private Boolean isReverseOption

	private static final String REQUEST_BY = "Req.By:"
	private static final String PAGE_NO = "Page:"
	private static final String REPORT_NAME = "Report:"
	private static final String VERSION_NO = "Version:"
	private static final String RUN_ON = "Run on:"
	private static final String RUN_AT = "at:"
	private static final String DASHLINES = "-"
	private static final String END_OF_LINES = "\n"
	private static final String PAGE_BREAK = "\f"
	private static final String SPACES = " "
	private static final String REQ_PARAMS = "Request Parameters:"
	private static final String REQ_USERID = "Request Userid:"
	private static final String REQ_ON = "Requested on:"
	private static final String SELCT_CRITERIA = "Selection Criteria:"
	private static final String AUSTRALIAN = "A"  //dd/mm/yy
	private static final String ISO = "I" //yy/mm/dd
	private static final String MILITARY = "M"  //dd/MM(words)/yy
	private static final String US = "U"  // mm/dd/yy

	private version = 10

	public BatchTextReports(TextReport tx, MSF080Rec batch080Details, Binding b){
		report = tx
		this.batch080Details = batch080Details
		edoi = b.getVariable("edoi");
		commarea = b.getVariable("commarea");
		lineNo = 1
		info ("Version:" + version.toString())
		setDateFormat(this.batch080Details.getRequestDstrct())
		ellipseVersion = commarea.systemVersion + "." + commarea.releaseVersion
		districtName = getDistrictName(batch080Details.getRequestDstrct())
		getReportTitle(batch080Details.getPrimaryKey().getProgName())
		prntHeadAtSetColumn = true
	}

	/**
	 * <br>Write the text to a file report.
	 * <br>when the line is greater then maximum Line it will automatically generate the page header
	 * 
	 *  <br>@param String text
	 * */
	public void write (String text){
		if (lineNo > maxLine){
			newPage()
		}
		writingText(text)
	}

	/**
	 * <br> Write the text to a file report.
	 * <br>when the line is greater then maximum Line it will automatically generate the page header
	 *
	 *  <br>when column is populated the texts will be match with the columns
	 *  <br>e.g.
	 *  <br>columns = ["heading 1    ","heading 2     ","heading3"," heading4"]
	 *  <br>texts = ["abc","def","3","4"] or object.write("abc","def","3","4")
	 *  
	 *  <br>will become:
	 *  
	 *  <br>heading 1   heading 2     heading3 heading4
	 *  <br>abc         def                  3        4
	 *  
	 *  <br>default is PadRight (justify left)
	 *  
	 *  <br>@param List texts
	 * */

	public void write(String [] textsString){
		String totalText = ""
		String txt
		Integer i = 0

		if (textsString != null){

			if (textsString.size() == columns.size()){
				columns.each{
					totalText = totalText +textsString[i].padRight(it.length())
					i++
				}
			}else{
				totalText = "Data <> Column , The total Data and Column must match"
			}
		}

		write(totalText)

	}

	/**
	 * please see write(String [] texts) for more details
	 * */
	public void setColumns(List <String> textCol){
		this.columns = textCol

		if (prntHeadAtSetColumn){
			heading()
		}

	}

	public void firstPage(){
		header()
		requestParams()
		newPage()
	}

	/**
	 * <br>Write a single character with repetition
	 * <br>e.g writeLine(5,"-") will result -----
	 * 
	 * <br>@param Number numberOfOccurence
	 * <br>@param String text
	 * */
	public void writeLine(Number numberOfOccurence,String text){
		write(text.padRight(numberOfOccurence,text))
	}

	/**
	 * Close the report file it will also generate the **** End of Report ***
	 * 
	 */
	public void close(){
		if (lineNo > maxLine){
			newPage()
		}
		report.close()
	}

	public void newPage() {
		pageNo ++
		lineNo = 1
		writingText (PAGE_BREAK)
		header()
		heading()
	}

	/**
	 * Write the Standard Page Header
	 * */
	private void header (){
		String rName = ""
		String requestedByString = REQUEST_BY + SPACES +padRight(batch080Details.getRequestBy(),32," ")
		String runOnAt = RUN_ON + SPACES + getTodayDate().padRight(10," ") + SPACES + RUN_AT + SPACES + getTodayTime().padRight(8," ")

		if (reportName.length()<7){
			rName = reportName
		}else{
			rName = reportName.substring(0, 7)
		}
		writingText(DASHLINES.padRight(reportWidth,"-"))

		writingText(requestedByString +
				centre(districtName,52) +
				PAGE_NO.padLeft(30)+
				pageNo.toString().padLeft(10))

		writingText(runOnAt.padRight(32," ")+
				SPACES.padRight(11," ")+ SPACES.padRight(74," ") +
				REPORT_NAME + SPACES + rName)

		writingText (SPACES.padRight(40) +
				centre(reportTitle,52) +
				VERSION_NO.padLeft(33)+ ellipseVersion.trim().padLeft(7))

		writingText(DASHLINES.padRight(reportWidth,"-"))
	}

	/**
	 * Write the Standard Request Parameters
	 * */
	private void requestParams(){
		LinkedHashMap requestParams080 =  [:]
		Integer iMaxLength = 0
		String paramValue = ""

		writingText(REQ_PARAMS + SPACES.padRight(2," ") +
				REQ_USERID + SPACES.padRight(5," ") + padRight(batch080Details.getUserId(),10," "))
		writingText(SPACES.padRight(REQ_PARAMS.length()," ") + SPACES.padRight(2," ") +
				REQ_ON + SPACES.padRight(7," ") + translateEllipseDate(padRight(batch080Details.getPrimaryKey().getRequestData(),14," ").trim()) + SPACES +
				RUN_AT + SPACES + translateEllipseTime(padRight(batch080Details.getPrimaryKey().getRequestData(),14," "))
				)
		writingText (SPACES.padRight(REQ_PARAMS.length()," ") + SPACES.padRight(2," ") +
				SELCT_CRITERIA)

		if (batch080Details != null){
			Integer maxLength = batch080Details.getRequestParams().length()
			Integer paramEndIndex = 0
			info("param080:" + batch080Details.getRequestParams())
			info("param080Len:" + maxLength.toString())

			Constraint c1 = MSF081Key.progName.equalTo(batch080Details.getPrimaryKey().getProgName())
			Constraint c2 = MSF081Key.seqNo.notEqualTo("0000")
			def query = new QueryImpl(MSF081Rec.class).and(c1).and(c2).orderBy(MSF081Rec.aix1)

			edoi.search(query,{MSF081Rec msf081Rec ->



				paramEndIndex = (Integer.parseInt(msf081Rec.getOffsetMsf080())-1)+Integer.parseInt(msf081Rec.getFieldSize())
				info("paramEndIndex:" + paramEndIndex.toString())
				info("maxLength:" + maxLength)

				if (paramEndIndex > maxLength){
					paramEndIndex = maxLength
					info("overwrite paramEndIndex:" + paramEndIndex)
				}

				Integer startIndex080 = Integer.parseInt(msf081Rec.getOffsetMsf080())-1
				Integer endIndex080 = paramEndIndex

				int repeatCnt = Integer.parseInt(msf081Rec.getRepeatCnt())
				while(repeatCnt-- > 0){

					info ("startIndex080:" + startIndex080)
					info ("endIndex080:" + endIndex080)

					if (paramEndIndex > 0 && Integer.parseInt(msf081Rec.getOffsetMsf080())-1 < paramEndIndex){
						paramValue = batch080Details.getRequestParams().substring(startIndex080,
								endIndex080)
					}else {
						paramValue = SPACES
					}

					info("paramValue:" + paramValue)
					requestParams080.put(msf081Rec.getDescLine_1().trim(),paramValue)
					startIndex080 = endIndex080
					endIndex080 = startIndex080 + Integer.parseInt(msf081Rec.getFieldSize())
				}

				if (msf081Rec.getDescLine_1().trim().length() > iMaxLength){
					iMaxLength = msf081Rec.getDescLine_1().trim().length()
				}
			})

			requestParams080.each{
				writingText(SPACES.padRight(REQ_PARAMS.length()," ") + SPACES.padRight(SELCT_CRITERIA.length()," ") +
						it.key.padRight(iMaxLength," ") +":" + SPACES +it.value
						)
			}
		}
		writingText(DASHLINES.padRight(reportWidth,"-"))
	}

	/**
	 * Print page heading
	 * */
	public void heading(){
		boolean isWritingHeading = false
		String totalTextHeadings = ""

		pageHeadings.each{
			write(it)
			isWritingHeading = true
		}

		if (!isWritingHeading){
			columns.each{ totalTextHeadings = totalTextHeadings + it }
			if (totalTextHeadings.trim().length()>0){
				write (totalTextHeadings)
				isWritingHeading = true
			}
		}

		if (isWritingHeading){
			write(DASHLINES.padRight(reportWidth,"-"))
		}
	}

	/**
	 * <br>This method is used for header,heading and requestParams
	 * <br>to prevent stack overflow avoid using write() for anywriting text from this class
	 * <br>e.g. method 1 calling method 2 however in method 2 calling method 1 again.
	 * 
	 * <br>@param String text
	 * */
	private void writingText(String text){
		report.write(text)
		lineNo++
	}

	/**
	 * Get the District Name from MSF000 DC0001
	 * @param Distrct Code
	 * */
	private String getDistrictName(String dstrctCode){
		Constraint c1 = MSF000_DC0001Key.dstrctCode.equalTo(dstrctCode)
		Constraint c2 = MSF000_DC0001Key.controlRecNo.equalTo("0001")
		Constraint c3 = MSF000_DC0001Key.controlRecType.equalTo("DC")

		def query = new QueryImpl(MSF000_DC0001Rec.class).and(c1).and(c2).and(c3)
		MSF000_DC0001Rec msf000Dc0001 = edoi.firstRow(query)

		if (msf000Dc0001 == null){
			return " "
		}else{
			return msf000Dc0001.getDstrctName().trim()
		}
	}

	private void setDateFormat(String dstrctCode){
		Constraint c1 = MSF000_DC0002Key.dstrctCode.equalTo(dstrctCode)
		Constraint c2 = MSF000_DC0002Key.controlRecNo.equalTo("0002")
		Constraint c3 = MSF000_DC0002Key.controlRecType.equalTo("DC")
		def query = new QueryImpl(MSF000_DC0002Rec.class).and(c1).and(c2).and(c3)
		MSF000_DC0002Rec msf000Dc0002 = edoi.firstRow(query)

		if (msf000Dc0002 == null){
			ellipseDateFormat = "A"
		}else{
			ellipseDateFormat = msf000Dc0002.getDstrctDteFmt().trim()
		}
	}
	/**
	 * Get report title from MSF081
	 * @param Program Name
	 * */
	private void getReportTitle (String progName){
		isReverseOption = false
		Constraint c1 = MSF081Key.progName.equalTo(progName)
		Constraint c2 = MSF081Key.seqNo.equalTo("0000")
		def query = new QueryImpl(MSF081Rec.class).and(c1).and(c2)
		MSF081Rec msf081Rec= edoi.firstRow(query)
		if (msf081Rec == null){
			reportTitle = ""
		}else{
			reportTitle = msf081Rec.getDescLine_1().trim()
			if (msf081Rec.getReverseOption().equals(new String("Y"))){
				isReverseOption = true
			}
		}
	}


	private String getTodayDate(){
		SimpleDateFormat sdf
		switch (ellipseDateFormat){
			case AUSTRALIAN:
				sdf = new SimpleDateFormat("dd/MM/yyyy")
				break
			case ISO:
				sdf = new SimpleDateFormat("yyyy/MM/dd")
				break
			case MILITARY:
				sdf = new SimpleDateFormat("dd/MMM/yyyy")
				break
			case US:
				sdf = new SimpleDateFormat("MM/dd/yyyy")
				break
			default: sdf = new SimpleDateFormat("dd/MM/yyyy")
		}

		return sdf.format(date)
	}


	private String getTodayTime(){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss")
		return sdf.format(date)
	}


	private String padRight(String theString,Number numberOfChars, String paddingChar){
		if (theString == null){
			return " ".padRight(numberOfChars," ")
		}else{
			return theString.padRight(numberOfChars,paddingChar)
		}
	}

	private String centre (String theString,Number numberOfChars){
		if (theString == null){
			return " ".padRight(numberOfChars," ")
		}else{
			return theString.center(numberOfChars)
		}
	}

	private void info(String value){
		def logObject = LoggerFactory.getLogger(getClass());
		logObject.info("------------- " + value)
	}

	private String translateEllipseDate(String sDate){
		sDate = checkReverseOption(sDate)
		String convertedDate = ""
		List <String> monthInWords = [
			"Jan",
			"Feb",
			"Mar",
			"Apr",
			"May",
			"Jun",
			"Jul",
			"Aug",
			"Sep",
			"Oct",
			"Nov",
			"Dec"
		]
		if (sDate.length() == 14){
			switch (ellipseDateFormat){
				case AUSTRALIAN:
					convertedDate = sDate.substring(6,8) + "/" + sDate.substring(4,6) + "/" + sDate.substring(0,4)
					break
				case ISO:
					convertedDate =  sDate.substring(0,4) + "/" + sDate.substring(4,6) + "/" +sDate.substring(6,8)
					break
				case US:
					convertedDate = sDate.substring(4,6) + "/" + sDate.substring(6,8) + "/" +  sDate.substring(0,4)
					break
				case MILITARY:
					convertedDate = sDate.substring(6,8) +"/" + monthInWords[Integer.parseInt(sDate.substring(4,6))-1]+ "/" + sDate.substring(0,4)
					break
				default: convertedDate = sDate.substring(6,8) + "/" + sDate.substring(4,6) + "/" + sDate.substring(0,4)
			}
		}


		return convertedDate
	}
	private String translateEllipseTime(String sTime){

		sTime = checkReverseOption(sTime)

		if (sTime.length() == 14){
			return sTime.substring(8,10) + ":" + sTime.substring(10, 12) +  ":" + sTime.substring(12,14)
		}else{
			return " "
		}
	}

	private String checkReverseOption (String sDateTime){

		//  Remarked the following codes as it turns out, the date was not reverse anymore
		//        if (isReverseOption){
		//            BigDecimal iDateTime = sDateTime.toBigDecimal()
		//
		//            iDateTime = 99999999999999 - iDateTime
		//            sDateTime = iDateTime.toString()
		//        }

		return sDateTime
	}
}
