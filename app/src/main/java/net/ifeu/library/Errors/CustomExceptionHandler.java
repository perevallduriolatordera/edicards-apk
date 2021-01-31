package net.ifeu.library.Errors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.util.Log;

public class CustomExceptionHandler {
	  private UncaughtExceptionHandler defaultHandler;
	 
	    public CustomExceptionHandler(Activity activity) {
	        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
	    }
	 
	    public void uncaughtException(Thread t, Throwable e) {
	        final Writer stringWriter = new StringWriter();
	        final PrintWriter printWriter = new PrintWriter(stringWriter);
	        e.printStackTrace(printWriter);
	        String stacktrace = stringWriter.toString();
	        printWriter.close();
	 
	        Log.e("CRASH", stacktrace);
	        Log.e("CRASH", e.toString());
	 
	        // Chain to the normal uncaught exception handler
	        defaultHandler.uncaughtException(t, e);
	    }

}
