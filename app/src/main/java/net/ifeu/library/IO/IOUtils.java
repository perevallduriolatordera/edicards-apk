package net.ifeu.library.IO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import net.ifeu.edicards.Constants.Constants;

public class IOUtils {

	public static List<String> getFilesFromDirectory(String path)
	{
		  List<String> list = new ArrayList<String>();
		  String files;
		  File folder = new File(path);
		  File[] listOfFiles = folder.listFiles(); 
		 
		  for (int i = 0; i < listOfFiles.length; i++) 
		  {
		 
		   if (listOfFiles[i].isFile()) 
		   {
		   files = listOfFiles[i].getName();
		       if (files.endsWith(".xml") 
		    		   || files.endsWith(".XML")
		    		   || files.endsWith(".pdf")
		    		   || files.endsWith(".PDF")
		    	   	   || files.endsWith(".1")
		    	       || files.endsWith(".2")
		    	       || files.endsWith(".txt")
		    	       || files.endsWith(".TXT"));
		       {
		    	   list.add(path + "/" + files);
		        }
		     }
		  }
		  
		  return list;
	}
	
	public static String getFileContent(String file) throws IOException
	{
		BufferedReader reader = new BufferedReader( new FileReader (file));
	    String         line = null;
	    String         finalString = Constants.EMPTY_STRING;
	    
	    while( ( line = reader.readLine() ) != null ) {
	    	finalString = finalString + line;
	    }

	    reader.close();
	    return finalString;
	}
	
	public static boolean deleteFile(String path)
	{
		File file = new File(path);
		 
		return file.delete();
		
	}
	
	public static boolean FileExists(String path)
	{
		File file = new File(path);
		return file.exists();
	}
	
	public static boolean writeAllText(String text, String file)
	{
		try {
		    // Create temp file.
		    File temp = new File(file);

		    // Write to temp file
		    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		    out.write(text);
		    out.close();
		    
		    return true;
		    
		} catch (IOException e) {
			return false;
		}
	}
	
	public static boolean copy(String fromFile, String toFile)
	{
		InputStream inStream = null;
		OutputStream outStream = null;
	 
	    	try{
	 
	    	    File afile =new File(fromFile);
	    	    File bfile =new File(toFile);
	 
	    	    inStream = new FileInputStream(afile);
	    	    outStream = new FileOutputStream(bfile);
	 
	    	    byte[] buffer = new byte[1024];
	 
	    	    int length;
	    	    //copy the file content in bytes 
	    	    while ((length = inStream.read(buffer)) > 0){
	 
	    	    	outStream.write(buffer, 0, length);
	 
	    	    }
	 
	    	    inStream.close();
	    	    outStream.close();
	 
	    	    return true;
	 
	    	}catch(IOException e){
	    		return false;
	    	}
	
	}
	
	@SuppressWarnings("resource")
	public static void copyFile(File src, File dst) throws IOException {
	    FileInputStream in = new FileInputStream(src);
		FileOutputStream out = new FileOutputStream(dst);
	    FileChannel fromChannel = null, toChannel = null;
	    try {
	        fromChannel = in.getChannel();
	        toChannel = out.getChannel();
	        fromChannel.transferTo(0, fromChannel.size(), toChannel); 
	    } finally {
	        if (fromChannel != null) 
	            fromChannel.close();
	        if (toChannel != null) 
	            toChannel.close();
	    }
	}
}
