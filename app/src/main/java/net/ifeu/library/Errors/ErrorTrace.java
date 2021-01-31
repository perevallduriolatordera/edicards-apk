package net.ifeu.library.Errors;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Mail.Mail;
import net.ifeu.library.Trace.Trace;

public class ErrorTrace {
	
	public void Send(String userId, Exception e) {
	
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		
		String body = "USUARIO: " + userId + Constants.NEW_LINE + "Error: " +  errors.toString();
		Mail mail = new Mail(body);
		
		try {
			if (!mail.send())
			{
				Trace trace = new Trace();
				trace.Name = "Error";
				trace.Add(body);
				trace.Save();
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			Trace trace = new Trace();
			trace.Name = "Error";
			trace.Add(body);
			try {
				trace.Save();
			} catch (IllegalArgumentException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IllegalStateException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
	}
	
	public void SendDebug(String userId, String debugMessage) {
		
		String body = "USUARIO: " + userId + Constants.NEW_LINE + "Debug: " +  debugMessage;
		Mail mail = new Mail(body);
		
		try {
			if (!mail.send())
			{
				Trace trace = new Trace();
				trace.Name = "Debug";
				trace.Add(body);
				trace.Save();
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			Trace trace = new Trace();
			trace.Name = "Debug";
			trace.Add(body);
			try {
				trace.Save();
			} catch (IllegalArgumentException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IllegalStateException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
	}
}
