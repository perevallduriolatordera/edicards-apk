package net.ifeu.library.Trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.ifeu.edicards.Constants.Constants;

import org.xmlpull.v1.XmlSerializer;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

public class Trace {

	private List<String> _list;
	public String Name;
	
	public Trace()
	{ 
		_list =  new ArrayList<String>();
	}
	
	public List<String> List()
	{
		return _list;
	}
	
	public void Add(String line)
	{
		_list.add(line);
	}
	
	@SuppressWarnings("unused")
	public void Save() throws IllegalArgumentException, IllegalStateException, IOException
	{
		
		if (true) return;
		
		XmlSerializer serializer = Xml.newSerializer();
		 
		SimpleDateFormat formatterName;
		formatterName = new SimpleDateFormat("yyyyMMddhhmmss");
			
		 String fileName = this.Name + "_" + formatterName.format(new Date()) + ".xml";
		 
		 File newxmlfile = new File(Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_TRACE + "/" + fileName);
	        try {
	            newxmlfile.createNewFile();
	        }catch(IOException e)
	        {
	            Log.e("Trace::Save", "Exception in create new File(");
	        }
	        
	        FileOutputStream fileos = null;
	        try{
	            fileos = new FileOutputStream(newxmlfile);

	        }catch(FileNotFoundException e)
	        {
	            Log.e("Trace::Save",e.toString());
	        }
	        
		 
		 serializer.setOutput(fileos, "UTF-8");
	     serializer.startDocument(null, Boolean.valueOf(true));
	     serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	     serializer.startTag(null, "Trace");
	        
		 for (String line : this.List())
		 {
		 	serializer.startTag(null, "Line");
		 	serializer.text(line);
	        serializer.endTag(null,"Line");
		 }
			
		 serializer.endTag(null,"Trace");
	     serializer.endDocument();
	     serializer.flush();
	     fileos.close();
	}
	
}
