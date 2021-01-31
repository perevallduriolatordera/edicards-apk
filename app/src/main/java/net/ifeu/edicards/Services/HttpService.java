package net.ifeu.edicards.Services;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import android.util.Base64;
import android.util.Log;

public class HttpService {

	private InputStream OpenHttpConnection(String urlString,
			WindowsCredentials credentials) throws IOException {
		InputStream in = null;
		int response = -1;

		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();

		if (!(conn instanceof HttpURLConnection))
			throw new IOException("Not an HTTP connection");
		try {
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");

			httpConn.setRequestProperty(
					"Authorization",
					"Basic "
							+ Base64.encodeToString(
									(credentials.User + ":" + credentials.Password)
											.getBytes(), Base64.DEFAULT));
			httpConn.connect();
			response = httpConn.getResponseCode();

			if (response == HttpURLConnection.HTTP_OK)
				in = httpConn.getInputStream();
			else
				throw new Exception(
						String.valueOf(HttpURLConnection.HTTP_BAD_REQUEST));

		} catch (Exception ex) {
			Log.e("httpService", ex.getLocalizedMessage());
			throw new IOException("Error connecting");
		}

		return in;
	}

	private InputStream OpenHttpConnection(String urlString) throws IOException {
		InputStream in = null;
		int response = -1;

		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();

		if (!(conn instanceof HttpURLConnection))
			throw new IOException("Not an HTTP connection");
		try {
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");

			// httpConn.setRequestProperty("Authorization", "Basic " +
			// Base64.encodeToString((credentials.User + ":" +
			// credentials.Password).getBytes(), Base64.DEFAULT));
			httpConn.connect();
			response = httpConn.getResponseCode();

			if (response == HttpURLConnection.HTTP_OK)
				in = httpConn.getInputStream();
			else
				throw new Exception(
						String.valueOf(HttpURLConnection.HTTP_BAD_REQUEST));

		} catch (Exception ex) {
			Log.e("httpService", ex.getLocalizedMessage());
			throw new IOException("Error connecting");
		}
		return in;
	}
	
	private InputStream OpenHttpConnection(String urlString, String body,
            WindowsCredentials credentials) throws IOException {

		byte[] postData = body.getBytes( StandardCharsets.UTF_8 );
		int postDataLength = postData.length;
		
		InputStream in = null;
		int response = -1;
		
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Content-Length", Integer.toString(postDataLength ));
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		
		conn.setRequestProperty(
		"Authorization",
		"Basic "
		+ Base64.encodeToString(
		(credentials.User + ":" + credentials.Password)
		 .getBytes(), Base64.DEFAULT));
		
		if (!(conn instanceof HttpURLConnection))  new IOException("Not an HTTP connection");
		try {
		
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			
			httpConn.setRequestMethod("POST");
			java.io.OutputStream os = httpConn.getOutputStream();
			os.write(body.getBytes());
			os.flush();
			os.close();
			
			httpConn.connect();
			
			response = httpConn.getResponseCode();
			
			if (response == HttpURLConnection.HTTP_OK)
				in = httpConn.getInputStream();
			else
				throw new Exception(
				String.valueOf(HttpURLConnection.HTTP_BAD_REQUEST));
				
			} catch (Exception ex) {
				Log.e("httpService", ex.getLocalizedMessage());
				throw new IOException("Error connecting");
			}
		
		return in;
	}
	public Document Call(String url, WindowsCredentials credentials)
			throws IOException, ParserConfigurationException, SAXException {
		InputStream in = null;
		// Sample:
		// "http://80.38.149.109:81/Dades.asmx/BuscarArticles?empresa=E_ESTERJ&comercial=V09&Tots=true"

		in = OpenHttpConnection(url, credentials);
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();
		
		try {
			doc = db.parse(in);	
		} catch (Exception ex) {
			return null;
		}
		
		doc.getDocumentElement().normalize();

		Log.i("CALL 1", doc.getDocumentElement().getTextContent());

		return doc;

	}

	public Document Call(String url) throws IOException,
			ParserConfigurationException, SAXException {
		InputStream in = null;
		// Sample:
		// "http://80.38.149.109:81/Dades.asmx/BuscarArticles?empresa=E_ESTERJ&comercial=V09&Tots=true"

		in = OpenHttpConnection(url);
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();
		doc = db.parse(in);

		doc.getDocumentElement().normalize();

		Log.i("CALL 1", doc.getDocumentElement().getTextContent());

		return doc;

	}

	public void CallWithoutResult(String url, WindowsCredentials credentials)
			throws IOException, ParserConfigurationException, SAXException {
		
		InputStream in = null;
		// Sample:
		// "http://80.38.149.109:81/Dades.asmx/BuscarArticles?empresa=E_ESTERJ&comercial=V09&Tots=true"

		in = OpenHttpConnection(url, credentials);
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();
	}
	
	public void CallWithoutResult(String url,String body, WindowsCredentials credentials)
			throws IOException, ParserConfigurationException, SAXException {
		
		InputStream in = null;
		// Sample:
		// "http://80.38.149.109:81/Dades.asmx/BuscarArticles?empresa=E_ESTERJ&comercial=V09&Tots=true"

		in = OpenHttpConnection(url, body, credentials);
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();
	}


	public void CallWithoutResult(String url) throws IOException,
			ParserConfigurationException, SAXException {
		InputStream in = null;
		// Sample:
		// "http://80.38.149.109:81/Dades.asmx/BuscarArticles?empresa=E_ESTERJ&comercial=V09&Tots=true"

		in = OpenHttpConnection(url);
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;

		db = dbf.newDocumentBuilder();
	}

}
