package net.ifeu.library.IO;

import net.ifeu.edicards.Constants.ConstantsTypes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {

	public static List<String> getFilesFromDirectory(String path)
	{
		  List<String> list = new ArrayList<>();
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
	    String line;
	    String finalString = ConstantsTypes.EMPTY_STRING;
	    
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
}
