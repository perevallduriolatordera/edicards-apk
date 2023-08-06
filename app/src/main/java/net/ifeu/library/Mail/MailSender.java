package net.ifeu.library.Mail;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.ifeu.edicards.Constants.ConstantsMail;

//Class is extending AsyncTask because this class is going to perform a networking operation
public class MailSender  {

    //Information to send email
  final private String email;
  final private String subject;
  final private String message;
  final private String attachment;

  //Class Constructor
  public MailSender(String email, String subject, String message, String attachment){
      //Initializing variables
      this.email = email;
      this.subject = subject;
      this.message = message;
      this.attachment = attachment;
  }

  public void send() throws MessagingException, IOException {
	  
      //Creating properties
      Properties props = new Properties();

      //Configuring properties for gmail
      //If you are not using gmail you may need to change the values
      props.put("mail.smtp.host", "smtp.grupediciones.com");
      //props.put("mail.smtp.socketFactory.port", "587");
      //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.port", "587");

      //Creating a new session
      //Authenticating the password
      //Declaring Variables
      Session session = Session.getDefaultInstance(props,
              new javax.mail.Authenticator() {
                  //Authenticating the password
                  protected PasswordAuthentication getPasswordAuthentication() {
                      return new PasswordAuthentication(ConstantsMail.MAIL_USER, ConstantsMail.MAIL_PASSWORD);
                  }
              });

      try {
          //Creating MimeMessage object
          MimeMessage mm = new MimeMessage(session);

          //Setting sender address
          mm.setFrom(new InternetAddress(ConstantsMail.MAIL_FROM));
          //Adding receiver
          mm.addRecipient(Message.RecipientType.TO, new InternetAddress(this.email));
          //Adding subject
          mm.setSubject(this.subject, "UTF-8");
          
          // creates message part
          
          MimeBodyPart messageBodyPart = new MimeBodyPart();
          messageBodyPart.setText(this.message, "UTF-8");
          //messageBodyPart.setContent(this.message, "text/html");
   
          // creates multi-part
          Multipart multipart = new MimeMultipart();
          multipart.addBodyPart(messageBodyPart);
   
          // adds attachments

          if (this.attachment != null) {
              MimeBodyPart attachPart = new MimeBodyPart();

              try {
                  attachPart.attachFile(attachment);
              } catch (IOException ex) {
                  throw new RuntimeException(ex);
              }

              multipart.addBodyPart(attachPart);
          }
          // sets the multi-part as e-mail's content
          mm.setContent(multipart);

          //Sending email
          Transport.send(mm);

      } catch (MessagingException e) {
          throw new RuntimeException(e);
      }
  }
}
