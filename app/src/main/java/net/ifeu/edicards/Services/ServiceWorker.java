package net.ifeu.edicards.Services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsCredentials;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.Constants.ConstantsFTP;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsMail;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.Pactos;
import net.ifeu.edicards.DataTier.Tarifa;
import net.ifeu.edicards.DataTier.TipoIVA;
import net.ifeu.edicards.Excel.LogBookCreator;
import net.ifeu.edicards.Pdf.inventory.PdfInventory;
import net.ifeu.edicards.Services.RestClient.RequestMethod;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Csv.CsvCreator;
import net.ifeu.library.Debugger.Debugger;
import net.ifeu.library.Firebase.ArticuloStock;
import net.ifeu.library.Firebase.ArticuloStockResponse;
import net.ifeu.library.Firebase.FireStoreCaller;
import net.ifeu.library.Ftp.FTPUploader;
import net.ifeu.library.IO.IOUtils;
import net.ifeu.library.LogBook.LogBook;
import net.ifeu.library.Mail.Mail;
import net.ifeu.library.Mail.MailSender;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
                                                                                                                                     
public class ServiceWorker extends ServiceBase {

	public ServiceWorker() {
		super();
	}

	@SuppressLint("SimpleDateFormat")
	public void RunExport(Context context) throws Exception {

		AppConfig app;
		app = (AppConfig) context;

		// Asignamos las credenciales

		WSCredentials credentials = new WSCredentials(ConstantsCredentials.WS_AUTHENTICATION_USER, ConstantsCredentials.WS_AUTHENTICATION_PASSWORD);

		String directory;
		List<String> files;

		// * * * * * * * * * ESBORREM FITXERS TEMPORALS PER ALLIBERAR ESPAI DE DISC * * * * * * * * * * * *

		IOUtils.deleteFilesFromDirectory(Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_CUSTOMER_DOCUMENT);
		IOUtils.deleteFilesFromDirectory(Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_INGRESO_DIARIO);
		//IOUtils.deleteFilesFromDirectory(Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
		//		+ ConstantsFolders.FOLDER_FIRMAS);

		// * * * * * * * * * ENVIAMOS SEMÁFORO STOCK NTV * * * * * * * * * * * *

        String ntvFile = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
                + ConstantsFolders.FOLDER_STOCK_NTV + "/" + ConstantsFolders.FILE_STOCK_NTV;

        CsvCreator csvCreator = new CsvCreator(ntvFile, "IdArticulo", "Semaforo");

        LinkedHashMap<String, Articulo> articulos = app.getCache().getAllArticulos();

        for (Articulo articuloInCatalgo : articulos.values()) {
            csvCreator.addLine(articuloInCatalgo.CodigoArticulo, articuloInCatalgo.StockPropio);
        }

        csvCreator.flush();

        String server = ConstantsFTP.FTP_SERVER;
        int port = ConstantsFTP.FTP_PORT;
        String username = ConstantsFTP.FTP_USERNAME;
        String password = ConstantsFTP.FTP_PASSWORD;
        String remoteDirectory = ConstantsFTP.FTP_REMOTE_DIRECTORY;
        String localFilePath = ntvFile;

        // Pendent d'activació compte ftp
        FTPUploader ftpUploader = new FTPUploader();
        ftpUploader.uploadFile(server, port, username, password, remoteDirectory, localFilePath);

        // * * * * * * * * * ENVIAMOS PDF * * * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_PDF;

		files = IOUtils.getFilesFromDirectory(directory);

		for (String file : files) {

			try {
				boolean isRectificativo = false;
				File fileInfo = new File(file);
				String title = fileInfo.getName().subSequence(0, 1).equals("A") ? "Albaran " : "Deposito ";
				String albaran;

				if (fileInfo.getName().subSequence(0, 1).equals("R")) {
					title = "Albarán rectificativo ";
					isRectificativo = true;
				}

				if (!isRectificativo) {
					albaran = (fileInfo.getName().substring(2).replace(".pdf", ConstantsTypes.EMPTY_STRING));

				} else {
					albaran = (fileInfo.getName().substring(4).replace(".pdf", ConstantsTypes.EMPTY_STRING));
				}
				title = title + albaran;

				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				title = title + " generado a fecha " + sdf.format(fileInfo.lastModified());

				String[] parts = file.split("_");
				if (parts.length > 3 && parts[3].startsWith("E") && fileInfo.getName().subSequence(0, 4).equals("AALM")) {
					MailSender mailEnviosEdicards = new MailSender(ConstantsMail.MAIL_ENVIOS_EDICARDS, title, ConstantsMail.MAIL_BODY, file);
					try {
						mailEnviosEdicards.send();
						Debugger.Debug(context, app.getUser().User,"Se ha enviado el albarán " + albaran + " al almacén de envios Edicards", file);
					} catch (Exception e) {
						this.sendMailToMantenimiento(context,  e, app.getUser().User, albaran);
						continue;
					}
				}

				if (isRectificativo) {
					if (parts.length > 4 && parts[4].startsWith("E") && fileInfo.getName().subSequence(0, 5).equals("REC_A")) {
						MailSender mailEnviosEdicards = new MailSender(ConstantsMail.MAIL_ENVIOS_EDICARDS, title, ConstantsMail.MAIL_BODY, file);
						try {
							mailEnviosEdicards.send();
							Debugger.Debug(context, app.getUser().User,"Se ha enviado el albarán " + albaran  + " al almacén de envios Edicards", file);
						} catch (Exception e) {
							this.sendMailToMantenimiento(context, e, app.getUser().User, albaran);
							continue;
						}
					}
				}

				MailSender mail = new MailSender(ConstantsMail.MAIL_TO, title, ConstantsMail.MAIL_BODY, file);

				try {
					if (!fileInfo.getName().subSequence(0, 4).equals("AALM"))
						mail.send();

					IOUtils.deleteFile(file);
					Debugger.Debug(context, app.getUser().User,"Se ha enviado el albarán " + albaran + " a la cuenta de gmail de Edicards", file);
				} catch (Exception e) {
					this.sendMailToMantenimiento(context, e, app.getUser().User, albaran);
					continue;
				}
				this.Monitor().PdfSend++;

			} catch (Exception e) {

			}
		}

		// * * * * * * * * * ENVIAMOS AUTORIZACIONES * * * * * * * * * * * *                                                         

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_AUTORIZACIONES;

		List<String> filesAuth = IOUtils.getFilesFromDirectory(directory);
		for (String file : filesAuth) {

			try {
				File fileInfo = new File(file);

				String title = "Autorización ";
				title = title + (fileInfo.getName().substring(2).replace(".pdf", ConstantsTypes.EMPTY_STRING));

				Mail mail = new Mail(ConstantsMail.MAIL_HOST, ConstantsMail.MAIL_PORT, ConstantsMail.MAIL_SPORT, ConstantsMail.MAIL_USER,
						ConstantsMail.MAIL_PASSWORD, ConstantsMail.MAIL_FROM, title, ConstantsMail.MAIL_TO, ConstantsMail.MAIL_BODY);

				mail.addAttachment(file, title + ".pdf");

				try {
					mail.send();
					IOUtils.deleteFile(file);
				} catch (Exception e) {
					continue;
				}

			} catch (Exception e) {
				continue;
			}
			this.Monitor().AutorizacionesSend++;
		}

		// * * * * * * * * * ENVIAMOS GDPR * * * * * * * * * * * *                                                                   

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_GDPR;

		List<String> filesGDPR = IOUtils.getFilesFromDirectory(directory);

		for (String file : filesGDPR) {

			try {

				File fileInfo = new File(file);

				String title = "Documento GDPR del cliente ";
				title = title + (fileInfo.getName().replace(".pdf", ConstantsTypes.EMPTY_STRING));

				MailSender mail = new MailSender(ConstantsMail.MAIL_TO_GDPR, title, ConstantsMail.MAIL_BODY, file);

				try {
					mail.send();
				} catch (Exception e) {
					continue;
				}

				IOUtils.deleteFile(file);
				this.Monitor().GDPRSend++;
			} catch (Exception e) {
			}
		}

		// * * * * * * * * * ENVIAMOS LOGBOOK * * * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_LOGBOOK;

		List<String> filesLogBook = IOUtils.getFilesFromDirectory(directory);

		for (String file : filesLogBook) {

			try {

				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				String title = "Documento Trazabilidad del comercial " + app.getUser().User + " " + "con fecha " + formatter.format(new Date());
				MailSender mail = new MailSender(ConstantsMail.MAIL_TO_LOGBOOK, title, ConstantsMail.MAIL_BODY, file);

				try {
					mail.send();
					IOUtils.deleteFile(file);
				} catch (Exception e) {
				}

			} catch (Exception e) {
			}
		}

		// * * * * * * * * * ENVIAMOS INCIDENCIAS * * * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
			+ ConstantsFolders.FOLDER_INCIDENCIAS;

		List<String> incidencias = IOUtils.getFilesFromDirectory(directory);

		for (String file : incidencias) {

			try {
				File fileInfo = new File(file);
				String title = ConstantsTypes.EMPTY_STRING;

				if (fileInfo.getName().subSequence(0, 1).toString().equals("A"))
					title = "Albarán anulado enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("E"))
					title = "Albarán anulado enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("B"))
					title = "Baja de cliente enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("N"))
					title = "Alta de nuevo cliente enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("D"))
					title = "Datos fiscales modificados enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("C"))
					title = "Datos de cuenta corriente introducidos/modificados enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("F"))
					title = "Datos de filiación de cliente modificados enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("I"))
					title = "Ingreso realizado por enviado por " + app.getUser().User;
				else if (fileInfo.getName().subSequence(0, 1).toString().equals("P"))
					title = "Error producido al generar documento pdf enviado por " + app.getUser().User;

				String content = "Este mensaje se ha generado automáticamente desde el dispositivo móvil.";
				MailSender mail;

				if (fileInfo.getName().subSequence(0, 1).toString().equals("I"))
					mail = new MailSender(ConstantsMail.MAIL_ADMINISTRACION_2, title, content, file);
				else
					mail = new MailSender(ConstantsMail.MAIL_ADMINISTRACION, title,  content, file);

				if (fileInfo.getName().subSequence(0, 1).toString().equals("E"))
					mail = new MailSender(ConstantsMail.MAIL_FACTURACION, title,  content, file);

				try {
					mail.send();
					IOUtils.deleteFile(file);
				} catch (Exception e) {
					continue;
				}

				this.Monitor().IncidenciasSend++;
			} catch (Exception e) {
			}
		}

		// * * * * * * * * * ENVIAMOS ARTICULOS * * * * * * * * * * * *                                                              

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"                         
				+ ConstantsFolders.FOLDER_STOCK;                                                                                            
                                                                                                                                                                                                                                      
		files = IOUtils.getFilesFromDirectory(directory);
		String url = ConstantsEndpoints.WS_ENVIAR_ARTICULOS;
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = IOUtils.getFileContent(file);                                                                           
                                                                                                                                     
			RestClient client = new RestClient();                                                                                    
			ArrayList<NameValuePair> headers = new ArrayList<>();
			headers.add(new BasicNameValuePair("Authorization", ConstantsTypes.AUTHORIZATION_HEADER_SERVICES));
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("contingut", content));                                                                
	                                                                                                                                 
			boolean result;                                                                                                          
			                                                                                                                         
			try {                                                                                                                    
				result = client.Execute(RequestMethod.POST, url, headers, params);                                                   
				                                                                                                                     
				if (result) {                                                                                                        
					IOUtils.deleteFile(file);                                                                                 
					this.Monitor().ArticulosSend++;
				}                                                                                                                    

			} catch (Exception e) {
				continue;                                                              
			}                                                                                                                        

		}                                                                                                                            
                                                                                                                                     
		// * * * * * * * * * ENVIAMOS GASTOS * * * * * * * * * * * *                                                                 
		                                                                                                                             
		boolean anyGastos;
		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"                         
				+ ConstantsFolders.FOLDER_GASTOS;                                                                                           

		files = IOUtils.getFilesFromDirectory(directory);                                                                            
		anyGastos = (files.size() > 0);                                                                                              
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = encodeURIComponent(IOUtils.getFileContent(file));
			HttpService http = new HttpService();
			url = ConstantsEndpoints.WS_ENVIAR_GASTOS;
			                                                                                                                         
			try {                                                                                                                    
				http.CallWithoutResult(url + "?contingut=" + content, credentials);                                                  
				IOUtils.deleteFile(file);
				this.Monitor().GastosSend++;
			} catch (Exception e) {                                                                                                  
			}
		}                                                                                                                            

		// * * * * * * * * * GENERAMOS Y ENVIAMOS INVENTARIO * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_INVENTARIO;

		List<String> inventario = IOUtils.getFilesFromDirectory(directory);

		if (anyGastos) {
			PdfInventory inventory = new PdfInventory(app);
			inventory.createInventory();

			for (String file : inventario) {

				try {

					if (!file.contains("Reciclado_")) {
						File fileInfo = new File(file);
						String title = "Inventario ";
						title = title + (fileInfo.getName().replace(".pdf", ConstantsTypes.EMPTY_STRING));
						Mail mail = new Mail(ConstantsMail.MAIL_HOST, ConstantsMail.MAIL_PORT, ConstantsMail.MAIL_SPORT,
								ConstantsMail.MAIL_USER, ConstantsMail.MAIL_PASSWORD, ConstantsMail.MAIL_FROM, title, ConstantsMail.MAIL_TO,
								ConstantsMail.MAIL_BODY);

						mail.addAttachment(file, title + ".pdf");

						try {
							mail.send();
							IOUtils.deleteFile(file);
						} catch (Exception e) {
							continue;
						}
						this.Monitor().InventarioSend++;
					}
				}
				catch (Exception e) {
				}

			}
		}

		for (String file : inventario) {

			try {
				if (file.contains("Reciclado_")) {
					File fileInfo = new File(file);
					String title = "Inventario de reciclado ";
					title = title + (fileInfo.getName().replace(".pdf", ConstantsTypes.EMPTY_STRING).replace("Reciclado_", ConstantsTypes.EMPTY_STRING));
					MailSender mail;
					mail = new MailSender(ConstantsMail.MAIL_FACTURACION, title, ConstantsTypes.EMPTY_STRING, file);

					try {
						mail.send();
						IOUtils.deleteFile(file);
					} catch (Exception e) {
						continue;
					}

					this.Monitor().InventarioSend++;

				}
			}
			catch (Exception e) {
			}
		}

		// * * * * * * * * * ENVIAMOS RECUENTO * * * * * * * * * * * *                                                               

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
				+ ConstantsFolders.FOLDER_RECUENTO;

		List<String> recuento = IOUtils.getFilesFromDirectory(directory);

		for (String file : recuento) {

			try {
				String content = IOUtils.getFileContent(file);
				url = ConstantsEndpoints.WS_ENVIAR_STOCKS;
				RestClient client = new RestClient();
				ArrayList<NameValuePair> headers = new ArrayList<>();
				headers.add(new BasicNameValuePair("Authorization", ConstantsTypes.AUTHORIZATION_HEADER_SERVICES));
				ArrayList<NameValuePair> params = new ArrayList<>();
				params.add(new BasicNameValuePair("contingut", content));
				boolean result = client.Execute(RequestMethod.POST, url, headers, params);

				if (result) {
					IOUtils.deleteFile(file);
					this.Monitor().RecuentoSend++;
				}

			} catch (Exception e) {
			}

		}
		                                                                                                                             
		// * * * * * * * * * ENVIAMOS STOCK DIARIO * * * * * * * * * * * *                                                           
                                                                                                                                     
		XmlCreator creator = new XmlCreator(app);
		creator.createXmlDailyStock();                                                                                               

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"                         
				+ ConstantsFolders.FOLDER_DAILYSTOCK;                                                                                       
                                                                                                                                     
		List<String> stockDiario = IOUtils.getFilesFromDirectory(directory);                                                         
                                                                                                                                     
		for (String file : stockDiario) {   
                                                                                                                                     
			String content = IOUtils.getFileContent(file);
			url = ConstantsEndpoints.WS_ENVIAR_STOCKS;

			RestClient client = new RestClient();                                                                                    
			ArrayList<NameValuePair> headers = new ArrayList<>();
			headers.add(new BasicNameValuePair("Authorization", ConstantsTypes.AUTHORIZATION_HEADER_SERVICES));
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("contingut", content));                                                                
			                                                                                                                         
			boolean result;                                                                                                          
			                                                                                                                         
			try {                                                                                                                    
				result = client.Execute(RequestMethod.POST, url, headers, params);
				if (result) {                                                                                                        
					IOUtils.deleteFile(file);                                                                                                                             
					this.Monitor().StockDiarioSend++;
				}                                                                                                                    
			} catch (Exception e) {
			}
			                                                                                                                                                                                                                                                                                                                                                                                        
		}                                                                                                                            

		// * * * * * * * * * ENVIAMOS ALBARANES * * * * * * * * * * * *                                                              

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"                         
				+ ConstantsFolders.FOLDER_ALBARANES;                                                                                        

		files = IOUtils.getFilesFromDirectory(directory);                                                                            
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = encodeURIComponent(IOUtils.getFileContent(file));

			HttpService http = new HttpService();
			url = ConstantsEndpoints.WS_ENVIAR_ALBARANES;

			try {                                                                                                                    
				http.CallWithoutResult(url + "?contingut=" + content, credentials);                                                  
				IOUtils.deleteFile(file);
				this.Monitor().AlbaranesSend++;
				
			} catch (Exception e) {                                                                                                  
			}
                                                                                                                                     
		}                                                                                                                            

		// * * * * * * * * * ENVIAMOS DEPOSITOS * * * * * * * * * * * *                                                              

		directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"                         
				+ ConstantsFolders.FOLDER_DEPOSITOS;                                                                                        
                                                                                                                                     
		files = IOUtils.getFilesFromDirectory(directory);                                                                            
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = encodeURIComponent(IOUtils.getFileContent(file));
			HttpService http = new HttpService();
			url = ConstantsEndpoints.WS_ENVIAR_DEPOSITOS;

			try {                                                                                                                    
				http.CallWithoutResult(url + "?contingut=" + content, credentials);                                                  
				IOUtils.deleteFile(file);
				this.Monitor().DepositosSend++;
			} catch (Exception e) {                                                                                                  
			}
		}
	}                                                                                                                                
                                                                                                                                     
	public boolean RunImport(Context context, boolean compress) {

		AppConfig app;                                                                                                               
		app = (AppConfig) context;                                                                                                   
		boolean result = true;
		ParserResponse parser = new ParserResponse();              
                                                                                                                                     
		// Habilitamos la conexión Wifi y 3G                                                                                         
                                                                                                                                                                                                                                                             
		this.createFolders();
		try {                                                                                                                        
			// Asignamos las credenciales
                                                                                                                                     
			WSCredentials credentials = new WSCredentials(ConstantsCredentials.WS_AUTHENTICATION_USER, ConstantsCredentials.WS_AUTHENTICATION_PASSWORD);

			try {
				app.getDatabaseOperations().openDB(context);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        
                                                                                                                                     
			boolean all;                                                                                                             
			Document document;                                                                                                       
			HttpService http = new HttpService();                                                                                    
                                                                                                                                     
			// Comprobamos la existencia de la tabla Contadores                                                                      
                                                                                                                                     
			Contador contador = Factory.build(Contador.class, app);

			if (contador.getRecordsCount() == 0) {
				if (isNumeric(app.getUser().InitSerieA))                                                                             
					contador.ContadorSerieA = Integer.parseInt(app.getUser().InitSerieA);                                            
				else                                                                                                                 
					contador.ContadorSerieA = 0;                                                                                     
                                                                                                                                     
				if (isNumeric(app.getUser().InitSerieB))                                                                             
					contador.ContadorSerieB = Integer.parseInt(app.getUser().InitSerieB);                                            
				else                                                                                                                 
					contador.ContadorSerieB = 0;
				try {                                                                                                                
					contador.save();                                                                                                 
				} catch (Exception e) {                                                                                              
					result = false;                                                                                                  
				}
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A FORMAS DE PAGO * * * * * * * * * *                                                      

			FormaPago formaPago = Factory.build(FormaPago.class, app);
			http = new HttpService();
                                                                                                                                     
			try {                                                                                                                    
				all = true;
				String url = compress ? ConstantsEndpoints.WS_FPAGO_ZIP : ConstantsEndpoints.WS_FPAGO;
                                                                                                                                     
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);

				parser.parseFormasPago(document, formaPago, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                     				
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A TIPOS DE IVA * * * * * * * * * *                                                        

			TipoIVA iva = Factory.build(TipoIVA.class, app);
			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
                                                                                                                                     
				all = (iva.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? ConstantsEndpoints.WS_TIPO_IVA_ZIP : ConstantsEndpoints.WS_TIPO_IVA;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseTiposIva(document, iva, compress);
                                                                                                                                     
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                                                                                         
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A ARTICULOS * * * * * * * * * *                                                           

			Articulo articulo = Factory.build(Articulo.class, app);
			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
				all = (articulo.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? ConstantsEndpoints.WS_ARTICULO_ZIP : ConstantsEndpoints.WS_ARTICULO;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseArticulos(document, app, articulo, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                                                                                         
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A ARTICULOS-STOCK DE FIRECLOUD * * * * * * * * * *

			try {
				FireStoreCaller fireStoreServices = new FireStoreCaller();
				String idToken = fireStoreServices.getToken();

				ArticuloStockResponse stock = fireStoreServices.getStock(idToken);

				LinkedHashMap<String, Articulo> articulos = app.getCache().getAllArticulos()
						;
				boolean hasNew = false;
				for (Articulo art : articulos.values()) {
					if (stock.articulos.containsKey(art.CodigoArticulo)) {
						Articulo articuloUpdate = Factory.build(Articulo.class, app);
						articuloUpdate.setArticuloById(String.valueOf(art.IdArticulo));
						art.StockPropio = stock.articulos.get(art.CodigoArticulo).stock;

						if (articuloUpdate.StockPropio != art.StockPropio) {
							articuloUpdate.StockPropio = art.StockPropio;
							articuloUpdate.update();
						}

					} else {
						ArticuloStock articuloStock = new ArticuloStock();
						articuloStock.idArticulo = art.CodigoArticulo;
						articuloStock.descripcion = art.Descripcion;
						articuloStock.stock = true;
						stock.articulos.put(art.CodigoArticulo, articuloStock);
						hasNew = true;
					}
				}

				if (hasNew)
					result = fireStoreServices.createStock(idToken, stock.name, stock.articulos);

			} catch (Exception e) {
				result = false;
			}
			// * * * * * * * * * * LLAMADA A TRASPASO ALMACEN * * * * * * * * *                                                                                                                                                                        

			http = new HttpService();
			boolean resultTraspaso;
			                                                                                                                         
			try {                                                                                                                    
				all = true;
                                                                                                                                     
				String url = ConstantsEndpoints.WS_TRASPASO_STOCK;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
				
				this.saveDocumentToFile(this.DocumentToString(document), "TraspasoStock.xml");
				resultTraspaso = parser.ParserTraspasoAlmacen(document, app, articulo, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
				resultTraspaso = false;                                                                                              
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A VALIDACIÓN TRASPASO * * * * * * * *                                                     
                                                                                                                                     
			if (resultTraspaso) {                                                                                                    
				http = new HttpService();                                                                                            
                                                                                                                                     
				try {                                                                                                                
					String url = ConstantsEndpoints.WS_VALIDAR_TRASPASO;
					http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User,
							credentials);                                                                                            

				} catch (Exception e) {                                                                                              
					parser.UndoTraspasoAlmacen(app);
					result = false;                                                                                                  
				}	                                                                                                                 
			} else {
				parser.UndoTraspasoAlmacen(app);
				result = false;
			}
			                                                                                                                         
			app.getTraspasoAlmacen().clear();
                                                                                                                                     
			// * * * * * * * * * * LLAMADA A CLIENTES * * * * * * * * * *                                                            

			Cliente cliente = Factory.build(Cliente.class, app);
			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
				all = (cliente.getRecordsCount() == 0 || app.getWorkingArea().UpgradeDataPost);
                                                                                                                                     
				String url = compress ? ConstantsEndpoints.WS_CLIENTES_ZIP : ConstantsEndpoints.WS_CLIENTES;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseClientes(document, app, cliente, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A TARIFAS * * * * * * * * * *                                                             

			Tarifa tarifa = Factory.build(Tarifa.class, app);
			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
				all = (tarifa.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? ConstantsEndpoints.WS_TARIFAS_ZIP : ConstantsEndpoints.WS_TARIFAS;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseTarifas(document, app, tarifa, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A PACTOS * * * * * * * * * *                                                              

			http = new HttpService();
			Pactos pacto = Factory.build(Pactos.class, app);

			try {                                                                                                                    
				all = (pacto.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? ConstantsEndpoints.WS_PACTOS_ZIP : ConstantsEndpoints.WS_PACTOS;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parsePactos(document, app, pacto, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        

			// * * * * * * * * * * LLAMADA A DEPOSITOS * * * * * * * * * *                                                           

			// Obtenemos el total de registros                                                                                       
                                                                                                                                     
			Long totalLineasDeposito = (long) 0;
			http = new HttpService();
			try {                                                                                                                    

				String url = compress ? ConstantsEndpoints.WS_DEPOSITOS_ZIP : ConstantsEndpoints.WS_TOTAL_DEPOSITOS;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + true, credentials);
                                                                                                                                     
				totalLineasDeposito = parser.parseTotalDepositos(document);
                                                                                                                                     
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        
                                                                                                                                     
			// Obtenemos los depósitos                                                                                               
			System.gc();                                                                                                             
			Deposito deposito = Factory.build(Deposito.class, app);

			all = (deposito.getRecordsCount() == 0 || app.getWorkingArea().UpgradeDataPost);                                         
			if (all) {                                                                                                               
                                                                                                                                     
				try {
					deposito.clean();
					for (long i = 1; i < totalLineasDeposito; i = i + ConstantsEndpoints.WS_PAGINACION) {

						boolean successful = false;                                                                                  
                                                                                                                                     
						int maxAttempts = 0;                                                                                         
						while (!successful && maxAttempts < ConstantsEndpoints.WS_MAX_INTENTOS) {
							try {

								String url = compress ? ConstantsEndpoints.WS_DEPOSITOS_ZIP : ConstantsEndpoints.WS_DEPOSITOS_PAGINACION;
								http = new HttpService();
								document = http.Call(                                                                                
										url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User               
												+ "&Tots=" + all + "&desde=" + i
												+ "&finsA=" + (i + ConstantsEndpoints.WS_PAGINACION - 1),
										credentials);                                                                                
                                                                                                                                     
								parser.parseDepositos(document, app, compress);
								successful = true;                                                                                   
								maxAttempts++;                                                                                       
								                                                                                                     
							} catch (Exception ex) {                                                                                                                                         
								successful = false;                                                                                  
							}                                                                                                        
						}                                                                                                            
					}                                                                                                                
				} catch (Exception e) {                                                                                              
					result = false;                                                                                                  
				}                                                                                                                    
                                                                                                                                              
			}
                                                                                                                                     
			// * * * * * * * * * * HACEMOS BACKUP A BASE DE DATOS * * * * * * *                                                      

			app.getDatabaseOperations().closeDB();          
			
			app.getDatabaseOperations().backupDatabase(ConstantsDatabase.DATABASE_BACKUP_NAME);
			app.getDatabaseOperations().openDB(context);
                                                                                                                                     
		} catch (Exception e) {                                                                                                      
			result = false;                                                                                                          
		}

		// Creamos excel de trazabilidad si es necesario

		try {
			LogBook logBook = Factory.build(LogBook.class, app);

			if (!logBook.hasLogBookCurrentWeek()) {
				LogBookCreator logBookCreator = new LogBookCreator(app);
				logBookCreator.createExcel30Days();
			}
		} catch (Exception e) {
			result = false;
		}

		this.Monitor().ParserMonitor = parser.Monitor();
		return result;                                                                                                               
	}                                                                                                                                
                                                                                                                                     
	private void createFolders() {                                                                                                   
		// Creamos las carpetas necesarias en la tarjeta SD                                                                          
                                                                                                                                     
		File root = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/");                                                              
		root.mkdirs();                                                                                                               
		                                                                                                                             
		File traceFolder = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_TRACE + "/");                        
		traceFolder.mkdirs();                                                                                                        
                                                                                                                                     
		File firmas = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS + "/");                            
		firmas.mkdirs();                                                                                                             
                                                                                                                                     
		File stock = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_STOCK + "/");                              
		stock.mkdirs();                                                                                                              
                                                                                                                                     
		File depositos = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_DEPOSITOS + "/");                      
		depositos.mkdirs();                                                                                                          
                                                                                                                                     
		File albaranes = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_ALBARANES + "/");                      
		albaranes.mkdirs();                                                                                                          
                                                                                                                                     
		File gastos = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_GASTOS + "/");                            
		gastos.mkdirs();                                                                                                             
                                                                                                                                     
		File jsonPrint = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_JSON_PRINT + "/");                     
		jsonPrint.mkdirs();                                                                                                          
                                                                                                                                     
		File pdf = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_PDF + "/");                                  
		pdf.mkdirs();                                                                                                                
                                                                                                                                     
		File incidencias = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INCIDENCIAS + "/");                  
		incidencias.mkdirs();                                                                                                        
                                                                                                                                     
		File inventario = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INVENTARIO + "/");                    
		inventario.mkdirs();                                                                                                         
                                                                                                                                     
		File autorizaciones = new File(                                                                                              
				"/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_AUTORIZACIONES + "/");                                   
		autorizaciones.mkdirs();                                                                                                     
                                                                                                                                     
		File dbBackup = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_DB_BACKUP + "/");                       
		dbBackup.mkdirs();                                                                                                           
                                                                                                                                     
		File recuento = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_RECUENTO + "/");                        
		recuento.mkdirs();                                                                                                           
                                                                                                                                     
		File stockDiario = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_DAILYSTOCK + "/");                   
		stockDiario.mkdirs();                                                                                                        
		                                                                                                                             
		File gdpr = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_GDPR + "/");                                
		gdpr.mkdirs();                                                                                                               
		
		File services = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_SERVICES + "/");                                
		services.mkdirs();

		File logBook = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_LOGBOOK + "/");
		logBook.mkdirs();

		File ntv = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_STOCK_NTV + "/");
		ntv.mkdirs();

		File ean = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_EAN + "/");
		ean.mkdirs();

		File dni = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_CUSTOMER_DOCUMENT + "/");
		dni.mkdirs();

		File ingresoDiario = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INGRESO_DIARIO + "/");
		ingresoDiario.mkdirs();
                                                                                                                                     
	}                                                                                                                                


	private String encodeURIComponent(String s) {                                                                                    
		String result;
                                                                                                                                     
		try {                                                                                                                        
			result = URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!")                                 
					.replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")")                                      
					.replaceAll("\\%7E", "~");                                                                                       
		}                                                                                                                            
                                                                                                                                     
		// This exception should never occur.                                                                                        
		catch (UnsupportedEncodingException e) {                                                                                     
			result = s;                                                                                                              
		}                                                                                                                            
                                                                                                                                     
		return result;                                                                                                               
	}                                                                                                                                
                                                                                                                                     
	private boolean isNumeric(String str) {                                                                                          
                                                                                                                                     
		if (str.equals(ConstantsTypes.EMPTY_STRING))
			return false;                                                                                                            
                                                                                                                                     
		NumberFormat formatter = NumberFormat.getInstance();                                                                         
		ParsePosition pos = new ParsePosition(0);                                                                                    
		formatter.parse(str, pos);                                                                                                   
		return str.length() == pos.getIndex();                                                                                       
	} 
	
	private String DocumentToString(Document doc) throws TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		StringWriter sw = new StringWriter();
		trans.transform(new DOMSource(doc), new StreamResult(sw));
		return sw.toString();
	}

	private void saveDocumentToFile(String str, String file)  {
		
		try {
			File outDir = new File("/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_SERVICES + "/");
	    
			File outputFile = new File(outDir, file);
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(str);
			writer.close();
			
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    }
	}

	private boolean sendMailToMantenimiento(Context context, Exception e, String user, String albaran) {

		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			String title = "Error enviando pdf del albarán " + albaran + " del comercial " + user + ":";
			Debugger.Debug(context, user, title + "\n\n" + sw, null);

		} catch (Exception exc) {
			return false;
		}

		return true;
	}
}
                                                                                                                                     