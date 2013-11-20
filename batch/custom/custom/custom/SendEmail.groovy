/**
 * @Ventyx 2012
 * This program provides functionality to send email.
 */
package com.mincom.ellipse.script.custom;

import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import com.mincom.batch.script.*;
import com.mincom.ellipse.script.util.*;
import com.mincom.eql.*;
import com.mincom.eql.impl.*;

public class SendEmail{
    /*
     * IMPORTANT!
     * Update this Version number EVERY push to GIT
     */
    private version = 5

    /*
     * Example on how to use the e-mailing function:
     * 
     * 
     ***** example how to Send a plain text e-mail *****
     String subject = "Test Mail"
     String mailTo = "tommy.limantono@mitrais.com"
     ArrayList message = new ArrayList()
     message.add("Line1")
     message.add("Line2")
     message.add("Line3")
     message.add("Line4")
     message.add("Line5")
     String pathName = " " //SPACE = no file attachment
     String mailFrom = "tommy.limantono@mitrais.com"
     String host = "exchange2007.mitrais.com"
     SendEmail myEmail = new SendEmail(subject,mailTo,message,pathName,mailFrom,host,false)
     myEmail.sendMail()
     ***** example how to Send a plain text e-mail to multiple e-mail account*****
     String subject = "Test Mail"
     String mailTo = "tommy.limantono@mitrais.com;hery.susanto@ventyx.abb.com"
     ArrayList message = new ArrayList()
     message.add("Line1")
     message.add("Line2")
     message.add("Line3")
     message.add("Line4")
     message.add("Line5")
     String pathName = " " //SPACE = no file attachment
     String mailFrom = "tommy.limantono@mitrais.com"
     String host = "exchange2007.mitrais.com"
     SendEmail myEmail = new SendEmail(subject,mailTo,message,pathName,mailFrom,host,false)
     myEmail.sendMail()
     ***** Example how to send an e-mail in html format *****
     String subject = "Test Mail"
     String mailTo = "tommy.limantono@mitrais.com"
     ArrayList message = new ArrayList()
     for (int i=0;i<20;i++){
     text = writeLine[i]
     text = text.replaceAll("\n", "<br>");
     text = text.replaceAll(" ", "&nbsp;");
     message.add(text)
     }
     message.add("</body>")
     message.add("</html>")
     String pathName = " " //SPACE = no file attachment
     String mailFrom = "tommy.limantono@mitrais.com"
     String host = "exchange2007.mitrais.com"
     SendEmail myEmail = new SendEmail(subject,mailTo,message,pathName,mailFrom,host,true)
     myEmail.sendMail()
     * 
     * Explanation for the parameter:
     * parameter1 (Email Subject): Mandatory parameter
     * parameter2 (mailTo): Email destination. Mandatory parameter.
     *                      Sending email to multiple email account can be performed by separate email account
     *                      using ";" character. Example: String mailTo = "tommy.limantono@mitrais.com;hery.susanto@ventyx.abb.com"
     * parameter3 (message): Email message. Mandatory parameter.
     * parameter4 (pathName): full path of the file location for the email attachment. Optional parameter.
     * parameter5 (mailFrom): Email from. Optional parameter.
     * parameter6 (host): email host. Optional parameter.   
     * parameter7 (isHtml): if parameter 3 (Email message) in html format. set this parameter to true.
     *                      if parameter 3 (Email message) is plain text. set this parameter to false.
     *                      Optional parameter. The default value is false.
     *                      
     * MULTIPLE ATTACHMENTS
     * If multiple attachments are required to be sent, place the attachments into an ArrayList
     * and pass the ArrayList into the constructor.                     
     *   
     *            ***** Example how to send an e-mail with multiple attachments *****
     *  String subject = "Test Mail"
     *  String mailTo = "tommy.limantono@mitrais.com;hery.susanto@ventyx.abb.com"
     *  ArrayList message = new ArrayList()
     *  message.add("Line1")
     *  message.add("Line2")
     *  message.add("Line3")
     *  message.add("Line4")
     *  message.add("Line5")
     *  ArrayList attachList = new ArrayList()
     *  attachList.add("file 1")
     *  attachList.add("file 2")
     *  SendEmail myEmail = new SendEmail(subject, mailTo, message, attachList)
     *  myEmail.sendMail()              
     *                      
     * ATTACHMENTS WITH ALIAS
     * We could set the alias for the pathname using ‘|’ followed by the alias.
     * Example:
     * 
     * new SendEmail(subject, mailTo, message, [
     *      "D:\\temp\\myFile.txt|reportA",
     *     "D:\\temp\\myFile - Copy.txt|reportB"
     *  ])
     *  
     *  The attachment's name will be reportA and reportB instead of myFile.txt 
     *  and myFile - Copy.txt. 
     */

    private String subject
    private String emailTo
    private ArrayList mailMessage
    private String pathName
    private String emailFrom
    private String host
    private boolean isHtml
    private String errorMessage
    private ArrayList pathNameList = new ArrayList();

    public SendEmail (String subject, String emailTo, ArrayList mailMessage){
        this.subject = subject
        this.emailTo = emailTo
        this.mailMessage = mailMessage
        this.pathName = ""
        this.emailFrom = ""
        this.host = ""
        this.isHtml = false
        this.errorMessage = ""
        this.pathNameList.add("")
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, String pathName){
        this(subject, emailTo, mailMessage)
        this.pathName = pathName
        this.pathNameList.add(pathName)
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, String pathName, String emailFrom){
        this(subject, emailTo, mailMessage, pathName)
        this.emailFrom = emailFrom
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, String pathName, String emailFrom, String host){
        this(subject, emailTo, mailMessage, pathName, emailFrom)
        this.host = host
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, String pathName, String emailFrom, String host, boolean isHtml){
        this(subject, emailTo, mailMessage, pathName, emailFrom, host)
        this.isHtml = isHtml
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, ArrayList pathName){
        this(subject, emailTo, mailMessage)
        this.pathNameList = pathName
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, ArrayList pathName, String emailFrom){
        this(subject, emailTo, mailMessage)
        this.emailFrom = emailFrom
        this.pathNameList = pathName
    }

    public SendEmail (String subject, String emailTo, ArrayList mailMessage, ArrayList pathName, String emailFrom, String host){
        this(subject, emailTo, mailMessage, emailFrom)
        this.host = host
        this.pathNameList = pathName
    }
    public SendEmail (String subject, String emailTo, ArrayList mailMessage, ArrayList pathName, String emailFrom, String host, boolean isHtml){
        this(subject, emailTo, mailMessage, emailFrom, host)
        this.isHtml = isHtml
        this.pathNameList = pathName
    }

    /**
     * Send the email. <br/>
     * To check the email delivery status, use {@link SendEmail#isError()} 
     * and {@link SendEmail#getErrorMessage()}
     */
    public void sendMail(){
        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server if it pass as an input parameter
        if (!host.trim().equals("")){
            properties.setProperty("mail.transport.protocol", "smtp");
            properties.setProperty("mail.host", host);
        }

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties,null);

        try{
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set Email From if it is pass as an input parameter.
            if (!emailFrom.trim().equals("")){
                message.setFrom(new InternetAddress(emailFrom));
            }

            // Set To: header field of the header.
            List<String> emailToList = Arrays.asList(emailTo.split(";"))

            for(String singleEmailTo: emailToList){
                message.addRecipient(Message.RecipientType.TO,
                        new InternetAddress(singleEmailTo))
            }

            // Set Subject: header field
            message.setSubject(subject);

            // Create the message part
            MimeBodyPart messagePart = new MimeBodyPart()
            String tempString = ""

            // Fill the message
            String newLineChar = "\n"
            if(isHtml) {
                newLineChar = "<br>"
            }
            tempString = mailMessage.join(newLineChar)
            if(!isHtml) {
                tempString = wrapTextToHTML(tempString)
            }
            messagePart.setContent(tempString, "text/html")
            // Create a multipar message
            Multipart multipart = new MimeMultipart();
            // Set text message part
            multipart.addBodyPart(messagePart);
            // Part two is attachment
            // Changed to support to set a different filename from the file, if the filename contains the character "|" it uses
            // the next token to give a nice name to the file
            if (pathNameList.size() > 0) {
                pathNameList.each {field ->
                    if (!field.toString().trim().equals("")) {
                        String inputFile = field.toString();
                        String fileName = null;
                        if (inputFile.indexOf("|") > -1) {
                            String[] filea = inputFile.split("\\|");
                            inputFile = filea[0];
                            fileName = filea[1];
                        }
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        FileDataSource fds = new FileDataSource(inputFile);
                        attachmentPart.setDataHandler(new DataHandler(fds));
                        attachmentPart.setFileName(fileName ?: fds.getName());
                        multipart.addBodyPart(attachmentPart);
                    }
                }
            }
            // Send the complete message parts
            message.setContent(multipart);

            // Send message
            Transport.send(message);
        }catch (MessagingException mex) {
            mex.printStackTrace();
            errorMessage = mex.getMessage()
        }
    }

    /**
     * Call this method after {@link SendEmail#sendMail()}.<br/>
     * This method will return true if error occured during send email.
     * @return true if error occured during send email
     */
    public boolean isError() {
        return errorMessage.trim().length() > 0
    }

    /**
     * Call this method after {@link SendEmail#sendMail()}.<br/>
     * This method will return error message occured during send email.
     * @return error message occured during send email
     */
    public String getErrorMessage() {
        return errorMessage
    }

    /**
     * Wrap text as html format.
     * @param text text
     * @return text as html format
     */
    private String wrapTextToHTML(String text) {
        StringBuffer sb = new StringBuffer();
        text = text.replaceAll("\r", "<br>");
        text = text.replaceAll("\n", "<br>");
        text = text.replaceAll(" ", "&nbsp;");
        sb.append("<html>");
        sb.append("<body style=\"font-family:Courier New; font-size:14px; \">");
        sb.append(text)
        sb.append("</body>");
        sb.append("</html>");
        return sb.toString();
    }
}