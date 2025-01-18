package net.ifeu.library.Mail;

import java.io.IOException;
import java.text.Normalizer;
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
public class EdicardsMailSender {

    //Information to send email
  final private String email;
  final private String subject;
  final private String message;
  final private String attachment;

  public enum AccountType {
      OPERACIONES_TABLET,
      CLIENTES_TABLET
  }

  public enum FormatType {
      HTML,
      TEXT
  }

  //Class Constructor
  public EdicardsMailSender(String email, String subject, String message, String attachment){
      //Initializing variables
      this.email = email;
      this.subject = subject;
      this.message = message;
      this.attachment = attachment;
  }

  public void send(AccountType accountType, FormatType format) throws MessagingException, IOException {
	  
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

      final String user = accountType == AccountType.OPERACIONES_TABLET ? ConstantsMail.MAIL_OPERACIONES_TABLET_USER : ConstantsMail.MAIL_CLIENTES_TABLET_USER;
      final String password = accountType == AccountType.OPERACIONES_TABLET ? ConstantsMail.MAIL_OPERACIONES_TABLET_PASSWORD : ConstantsMail.MAIL_CLIENTES_TABLET_PASSWORD;
      Session session = Session.getDefaultInstance(props,
              new javax.mail.Authenticator() {
                  //Authenticating the password
                  protected PasswordAuthentication getPasswordAuthentication() {
                      return new PasswordAuthentication(user, password);
                  }
              });

      try {
          //Creating MimeMessage object
          MimeMessage mm = new MimeMessage(session);

          //Setting sender address
          mm.setFrom(new InternetAddress(accountType == AccountType.OPERACIONES_TABLET ? ConstantsMail.MAIL_OPERACIONES_TABLET_USER : ConstantsMail.MAIL_CLIENTES_TABLET_USER));
          //Adding receiver
          mm.addRecipient(Message.RecipientType.TO, new InternetAddress(this.email));
          //Adding subject
          mm.setSubject(this.subject, "UTF-8");
          
          // creates message part
          
          MimeBodyPart messageBodyPart = new MimeBodyPart();
          messageBodyPart.setContent(this.message, format.equals(FormatType.TEXT) ? "UTF-8" : "text/html");
   
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
