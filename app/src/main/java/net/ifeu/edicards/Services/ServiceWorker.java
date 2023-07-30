package net.ifeu.edicards.Services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.Pactos;
import net.ifeu.edicards.DataTier.Tarifa;
import net.ifeu.edicards.DataTier.TipoIVA;
import net.ifeu.edicards.Excel.LogBookCreator;
import net.ifeu.edicards.Pdf.PdfInventory;
import net.ifeu.edicards.Services.RestClient.RequestMethod;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Debugger.Debugger;
import net.ifeu.library.Firebase.ArticuloStock;
import net.ifeu.library.Firebase.ArticuloStockResponse;
import net.ifeu.library.Firebase.FireStoreCaller;
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

		WindowsCredentials credentials = new WindowsCredentials();
		credentials.User = "Tablet";
		credentials.Password = "tab2012let";

		String directory;
		List<String> files;

		// * * * * * * * * * ENVIAMOS PDF * * * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_PDF;

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
					albaran = (fileInfo.getName().substring(2).replace(".pdf", Constants.EMPTY_STRING));

				} else {
					albaran = (fileInfo.getName().substring(4).replace(".pdf", Constants.EMPTY_STRING));
				}
				title = title + albaran;

				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				title = title + " generado a fecha " + sdf.format(fileInfo.lastModified());

				String[] parts = file.split("_");
				if (parts.length > 3 && parts[3].startsWith("E") && fileInfo.getName().subSequence(0, 1).equals("A")) {
					MailSender mailEnviosEdicards = new MailSender(Constants.MAIL_ENVIOS_EDICARDS, title, Constants.MAIL_BODY, file);
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
						MailSender mailEnviosEdicards = new MailSender(Constants.MAIL_ENVIOS_EDICARDS, title, Constants.MAIL_BODY, file);
						try {
							mailEnviosEdicards.send();
							Debugger.Debug(context, app.getUser().User,"Se ha enviado el albarán " + albaran  + " al almacén de envios Edicards", file);
						} catch (Exception e) {
							this.sendMailToMantenimiento(context, e, app.getUser().User, albaran);
							continue;
						}
					}
				}

				MailSender mail = new MailSender(Constants.MAIL_TO, title, Constants.MAIL_BODY, file);

				try {
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

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_AUTORIZACIONES;

		List<String> filesAuth = IOUtils.getFilesFromDirectory(directory);
		for (String file : filesAuth) {

			try {
				File fileInfo = new File(file);

				String title = "Autorización ";
				title = title + (fileInfo.getName().substring(2).replace(".pdf", Constants.EMPTY_STRING));

				Mail mail = new Mail(Constants.MAIL_HOST, Constants.MAIL_PORT, Constants.MAIL_SPORT, Constants.MAIL_USER,
						Constants.MAIL_PASSWORD, Constants.MAIL_FROM, title, Constants.MAIL_TO, Constants.MAIL_BODY);

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

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_GDPR;

		List<String> filesGDPR = IOUtils.getFilesFromDirectory(directory);

		for (String file : filesGDPR) {

			try {

				File fileInfo = new File(file);

				String title = "Documento GDPR del cliente ";
				title = title + (fileInfo.getName().replace(".pdf", Constants.EMPTY_STRING));

				MailSender mail = new MailSender(Constants.MAIL_TO_GDPR, title, Constants.MAIL_BODY, file);

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

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_LOGBOOK;

		List<String> filesLogBook = IOUtils.getFilesFromDirectory(directory);

		for (String file : filesLogBook) {

			try {

				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				String title = "Documento Trazabilidad del comercial " + app.getUser().User + " " + "con fecha " + formatter.format(new Date());
				MailSender mail = new MailSender(Constants.MAIL_TO_LOGBOOK, title, Constants.MAIL_BODY, file);

				try {
					mail.send();
					IOUtils.deleteFile(file);
				} catch (Exception e) {
				}

			} catch (Exception e) {
			}
		}

		// * * * * * * * * * ENVIAMOS INCIDENCIAS * * * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_INCIDENCIAS;

		List<String> incidencias = IOUtils.getFilesFromDirectory(directory);

		for (String file : incidencias) {

			try {
				File fileInfo = new File(file);
				String title = Constants.EMPTY_STRING;

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

				String content = "Este mensaje se ha generado automáticamente desde el dispositivo móvil.";
				MailSender mail;

				if (fileInfo.getName().subSequence(0, 1).toString().equals("I"))
					mail = new MailSender(Constants.MAIL_ADMINISTRACION_2, title, content, file);
				else
					mail = new MailSender(Constants.MAIL_ADMINISTRACION, title,  content, file);

				if (fileInfo.getName().subSequence(0, 1).toString().equals("E"))
					mail = new MailSender(Constants.MAIL_FACTURACION, title,  content, file);

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

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"                         
				+ Constants.FOLDER_STOCK;                                                                                            
                                                                                                                                                                                                                                      
		files = IOUtils.getFilesFromDirectory(directory);
		String url = Constants.WS_ENVIAR_ARTICULOS;                                                                                  
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = IOUtils.getFileContent(file);                                                                           
                                                                                                                                     
			RestClient client = new RestClient();                                                                                    
			ArrayList<NameValuePair> headers = new ArrayList<>();
			headers.add(new BasicNameValuePair("Authorization",Constants.AUTHORIZATION_HEADER_SERVICES));
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("contingut", content));                                                                
	                                                                                                                                 
			boolean result;                                                                                                          
			                                                                                                                         
			try {                                                                                                                    
				result = client.Execute(RequestMethod.POST, url, headers, params);                                                   
				                                                                                                                     
				if (result) {                                                                                                        
					IOUtils.deleteFile(file);                                                                                 
					this.Monitor().ArticulosSend++;
				}                                                                                                                    
				else {                                                                                                               
					continue;                        
				}                                                                                                                    
			} catch (Exception e) {
				continue;                                                              
			}                                                                                                                        

		}                                                                                                                            
                                                                                                                                     
		// * * * * * * * * * ENVIAMOS GASTOS * * * * * * * * * * * *                                                                 
		                                                                                                                             
		boolean anyGastos;
		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"                         
				+ Constants.FOLDER_GASTOS;                                                                                           

		files = IOUtils.getFilesFromDirectory(directory);                                                                            
		anyGastos = (files.size() > 0);                                                                                              
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = encodeURIComponent(IOUtils.getFileContent(file));
			HttpService http = new HttpService();
			url = Constants.WS_ENVIAR_GASTOS;
			                                                                                                                         
			try {                                                                                                                    
				http.CallWithoutResult(url + "?contingut=" + content, credentials);                                                  
				IOUtils.deleteFile(file);
				this.Monitor().GastosSend++;
			} catch (Exception e) {                                                                                                  
			}
		}                                                                                                                            

		// * * * * * * * * * GENERAMOS Y ENVIAMOS INVENTARIO * * * * * * * * * *

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_INVENTARIO;

		List<String> inventario = IOUtils.getFilesFromDirectory(directory);

		if (anyGastos) {
			PdfInventory inventory = new PdfInventory(context, app);
			inventory.createInventory();

			for (String file : inventario) {

				try {

					if (!file.contains("Reciclado_")) {
						File fileInfo = new File(file);
						String title = "Inventario ";
						title = title + (fileInfo.getName().replace(".pdf", Constants.EMPTY_STRING));
						Mail mail = new Mail(Constants.MAIL_HOST, Constants.MAIL_PORT, Constants.MAIL_SPORT,
								Constants.MAIL_USER, Constants.MAIL_PASSWORD, Constants.MAIL_FROM, title, Constants.MAIL_TO,
								Constants.MAIL_BODY);

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
					title = title + (fileInfo.getName().replace(".pdf", Constants.EMPTY_STRING).replace("Reciclado_", Constants.EMPTY_STRING));
					MailSender mail;
					mail = new MailSender(Constants.MAIL_FACTURACION, title, Constants.EMPTY_STRING, file);

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

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"
				+ Constants.FOLDER_RECUENTO;

		List<String> recuento = IOUtils.getFilesFromDirectory(directory);

		for (String file : recuento) {

			try {
				String content = IOUtils.getFileContent(file);
				url = Constants.WS_ENVIAR_STOCKS;
				RestClient client = new RestClient();
				ArrayList<NameValuePair> headers = new ArrayList<>();
				headers.add(new BasicNameValuePair("Authorization",Constants.AUTHORIZATION_HEADER_SERVICES));
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
                                                                                                                                     
		XmlCreator creator = new XmlCreator(app, app);                                                                               
		creator.createXmlDailyStock();                                                                                               

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"                         
				+ Constants.FOLDER_DAILYSTOCK;                                                                                       
                                                                                                                                     
		List<String> stockDiario = IOUtils.getFilesFromDirectory(directory);                                                         
                                                                                                                                     
		for (String file : stockDiario) {   
                                                                                                                                     
			String content = IOUtils.getFileContent(file);
			url = Constants.WS_ENVIAR_STOCKS;

			RestClient client = new RestClient();                                                                                    
			ArrayList<NameValuePair> headers = new ArrayList<>();
			headers.add(new BasicNameValuePair("Authorization",Constants.AUTHORIZATION_HEADER_SERVICES));
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

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"                         
				+ Constants.FOLDER_ALBARANES;                                                                                        

		files = IOUtils.getFilesFromDirectory(directory);                                                                            
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = encodeURIComponent(IOUtils.getFileContent(file));

			HttpService http = new HttpService();
			url = Constants.WS_ENVIAR_ALBARANES;

			try {                                                                                                                    
				http.CallWithoutResult(url + "?contingut=" + content, credentials);                                                  
				IOUtils.deleteFile(file);
				this.Monitor().AlbaranesSend++;
				
			} catch (Exception e) {                                                                                                  
			}
                                                                                                                                     
		}                                                                                                                            

		// * * * * * * * * * ENVIAMOS DEPOSITOS * * * * * * * * * * * *                                                              

		directory = Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/"                         
				+ Constants.FOLDER_DEPOSITOS;                                                                                        
                                                                                                                                     
		files = IOUtils.getFilesFromDirectory(directory);                                                                            
                                                                                                                                     
		for (String file : files) {                                                                                                  
			String content = encodeURIComponent(IOUtils.getFileContent(file));
			HttpService http = new HttpService();
			url = Constants.WS_ENVIAR_DEPOSITOS;

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
                                                                                                                                     
			WindowsCredentials credentials = new WindowsCredentials();                                                               
			credentials.User = "Tablet";                                                                                             
			credentials.Password = "tab2012let";                                                                                     
                                                                                                                                     
			try {
				app.getDatabaseOperations().openDB(context);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        
                                                                                                                                     
			boolean all;                                                                                                             
			Document document;                                                                                                       
			HttpService http = new HttpService();                                                                                    
                                                                                                                                     
			// Comprobamos la existencia de la tabla Contadores                                                                      
                                                                                                                                     
			Contador contador = new Contador();
			contador.InitializePersistance(app, context);
                                                                                                                                     
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
                                                                                                                                     
			contador.ReleasePersistance();                                                                                           
                                                                                                                                     
			// * * * * * * * * * * LLAMADA A FORMAS DE PAGO * * * * * * * * * *                                                      

			FormaPago formaPago = new FormaPago();
			formaPago.InitializePersistance(app, context);
			                                                                                                                         
			http = new HttpService();
                                                                                                                                     
			try {                                                                                                                    
				all = true;
				String url = compress ? Constants.WS_FPAGO_ZIP : Constants.WS_FPAGO;                                                 
                                                                                                                                     
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);

				parser.parseFormasPago(document, app, formaPago, compress);
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                     				
			}                                                                                                                        
                                                                                                                                             
			formaPago.ReleasePersistance();

			// * * * * * * * * * * LLAMADA A TIPOS DE IVA * * * * * * * * * *                                                        

			TipoIVA iva = new TipoIVA();
			iva.InitializePersistance(app, context);

			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
                                                                                                                                     
				all = (iva.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? Constants.WS_TIPO_IVA_ZIP : Constants.WS_TIPO_IVA;                                           
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseTiposIva(document, app, iva, compress);
                                                                                                                                     
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                                                                                         
			}                                                                                                                        
			                                                                                                                         
			iva.ReleasePersistance();

			// * * * * * * * * * * LLAMADA A ARTICULOS * * * * * * * * * *                                                           

			Articulo articulo = new Articulo();
			articulo.InitializePersistance(app, context);

			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
				all = (articulo.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? Constants.WS_ARTICULO_ZIP : Constants.WS_ARTICULO;                                           
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseArticulos(document, context, app, articulo, compress);                                                   
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
						Articulo articuloUpdate = new Articulo();
						articuloUpdate.InitializePersistance(app, context);
						articuloUpdate.setArticuloById(String.valueOf(art.IdArticulo));
						art.StockPropio = stock.articulos.get(art.CodigoArticulo).stock;
						articuloUpdate.StockPropio = art.StockPropio;
						articuloUpdate.update();
						articuloUpdate.ReleasePersistance();
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
				articulo.ReleasePersistance();
			} catch (Exception e) {
				result = false;
			}
			// * * * * * * * * * * LLAMADA A TRASPASO ALMACEN * * * * * * * * *                                                                                                                                                                        

			articulo.InitializePersistance(app, context);

			http = new HttpService();
			boolean resultTraspaso;
			                                                                                                                         
			try {                                                                                                                    
				all = true;
                                                                                                                                     
				String url = Constants.WS_TRASPASO_STOCK;
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
				
				this.saveDocumentToFile(this.DocumentToString(document), "TraspasoStock.xml");
				resultTraspaso = parser.ParserTraspasoAlmacen(document, context, app, articulo, compress);                           
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
				resultTraspaso = false;                                                                                              
			}                                                                                                                        
                                                                                                                                     
			articulo.ReleasePersistance();

			// * * * * * * * * * * LLAMADA A VALIDACIÓN TRASPASO * * * * * * * *                                                     
                                                                                                                                     
			if (resultTraspaso) {                                                                                                    
				http = new HttpService();                                                                                            
                                                                                                                                     
				try {                                                                                                                
					String url = Constants.WS_VALIDAR_TRASPASO;
					http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User,
							credentials);                                                                                            

				} catch (Exception e) {                                                                                              
					parser.UndoTraspasoAlmacen(context, app);                                                                        
					result = false;                                                                                                  
				}	                                                                                                                 
			} else {
				parser.UndoTraspasoAlmacen(context, app);                                                                        
				result = false;
			}
			                                                                                                                         
			app.getTraspasoAlmacen().clear();
                                                                                                                                     
			// * * * * * * * * * * LLAMADA A CLIENTES * * * * * * * * * *                                                            

			Cliente cliente = new Cliente();
			cliente.InitializePersistance(app, context);

			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
				all = (cliente.getRecordsCount() == 0 || app.getWorkingArea().UpgradeDataPost);
                                                                                                                                     
				String url = compress ? Constants.WS_CLIENTES_ZIP : Constants.WS_CLIENTES;                                           
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseClientes(document, context, app, cliente, compress);                                                     
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        

			cliente.ReleasePersistance();                                                                                            
                                                                                                                                     
			// * * * * * * * * * * LLAMADA A TARIFAS * * * * * * * * * *                                                             

			Tarifa tarifa = new Tarifa();
			tarifa.InitializePersistance(app, context);

			http = new HttpService();                                                                                                
                                                                                                                                     
			try {                                                                                                                    
				all = (tarifa.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? Constants.WS_TARIFAS_ZIP : Constants.WS_TARIFAS;                                             
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parseTarifas(document, context, app, tarifa, compress);                                                       
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        

			tarifa.ReleasePersistance();                                                                                             
                                                                                                                                     
			// * * * * * * * * * * LLAMADA A PACTOS * * * * * * * * * *                                                              

			http = new HttpService();

			Pactos pacto = new Pactos();
			pacto.InitializePersistance(app, context);

			try {                                                                                                                    
				all = (pacto.getRecordsCount() == 0);
                                                                                                                                     
				String url = compress ? Constants.WS_PACTOS_ZIP : Constants.WS_PACTOS;                                               
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + all, credentials);
                                                                                                                                     
				parser.parsePactos(document, context, app, pacto, compress);                                                         
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        

			pacto.ReleasePersistance();                                                                                              
                                                                                                                                                                                                                                                                          
			// * * * * * * * * * * LLAMADA A DEPOSITOS * * * * * * * * * *                                                           

			// Obtenemos el total de registros                                                                                       
                                                                                                                                     
			Long totalLineasDeposito = (long) 0;
			http = new HttpService();
			try {                                                                                                                    

				String url = compress ? Constants.WS_DEPOSITOS_ZIP : Constants.WS_TOTAL_DEPOSITOS;                                   
				document = http.Call(url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User                  
						+ "&Tots=" + true, credentials);
                                                                                                                                     
				totalLineasDeposito = parser.parseTotalDepositos(document, context, app);                                            
                                                                                                                                     
			} catch (Exception e) {                                                                                                  
				result = false;                                                                                                      
			}                                                                                                                        
                                                                                                                                     
			// Obtenemos los depósitos                                                                                               
			System.gc();                                                                                                             
			Deposito deposito = new Deposito();
			deposito.InitializePersistance(app, context);

			all = (deposito.getRecordsCount() == 0 || app.getWorkingArea().UpgradeDataPost);                                         
			if (all) {                                                                                                               
                                                                                                                                     
				try {
					deposito.clean();
					for (long i = 1; i < totalLineasDeposito; i = i + Constants.WS_PAGINACION) {

						boolean successful = false;                                                                                  
                                                                                                                                     
						int maxAttempts = 0;                                                                                         
						while (!successful && maxAttempts < Constants.WS_MAX_INTENTOS) {
							try {

								String url = compress ? Constants.WS_DEPOSITOS_ZIP : Constants.WS_DEPOSITOS_PAGINACION;
								http = new HttpService();
								document = http.Call(                                                                                
										url + "?empresa=" + app.getUser().Company + "&comercial=" + app.getUser().User               
												+ "&Tots=" + all + "&desde=" + i
												+ "&finsA=" + (i + Constants.WS_PAGINACION - 1),
										credentials);                                                                                
                                                                                                                                     
								parser.parseDepositos(document, context, app, deposito, compress);                                   
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
                                                                                                                                              
				deposito.ReleasePersistance();
			}
                                                                                                                                     
			// * * * * * * * * * * HACEMOS BACKUP A BASE DE DATOS * * * * * * *                                                      

			app.getDatabaseOperations().closeDB();          
			
			app.getDatabaseOperations().backupDatabase();
			app.getDatabaseOperations().openDB(context);                                                                                   
                                                                                                                                     
		} catch (Exception e) {                                                                                                      
			result = false;                                                                                                          
		}

		// Creamos excel de trazabilidad si es necesario

		try {
			LogBook logBook = new LogBook();
			logBook.InitializePersistance(app, app);

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
                                                                                                                                     
		File root = new File("/sdcard/" + Constants.FOLDER_ROOT + "/");                                                              
		root.mkdirs();                                                                                                               
		                                                                                                                             
		File traceFolder = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_TRACE + "/");                        
		traceFolder.mkdirs();                                                                                                        
                                                                                                                                     
		File firmas = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_FIRMAS + "/");                            
		firmas.mkdirs();                                                                                                             
                                                                                                                                     
		File stock = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_STOCK + "/");                              
		stock.mkdirs();                                                                                                              
                                                                                                                                     
		File depositos = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_DEPOSITOS + "/");                      
		depositos.mkdirs();                                                                                                          
                                                                                                                                     
		File albaranes = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_ALBARANES + "/");                      
		albaranes.mkdirs();                                                                                                          
                                                                                                                                     
		File gastos = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_GASTOS + "/");                            
		gastos.mkdirs();                                                                                                             
                                                                                                                                     
		File jsonPrint = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_JSON_PRINT + "/");                     
		jsonPrint.mkdirs();                                                                                                          
                                                                                                                                     
		File pdf = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_PDF + "/");                                  
		pdf.mkdirs();                                                                                                                
                                                                                                                                     
		File incidencias = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_INCIDENCIAS + "/");                  
		incidencias.mkdirs();                                                                                                        
                                                                                                                                     
		File inventario = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_INVENTARIO + "/");                    
		inventario.mkdirs();                                                                                                         
                                                                                                                                     
		File autorizaciones = new File(                                                                                              
				"/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_AUTORIZACIONES + "/");                                   
		autorizaciones.mkdirs();                                                                                                     
                                                                                                                                     
		File dbBackup = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_DB_BACKUP + "/");                       
		dbBackup.mkdirs();                                                                                                           
                                                                                                                                     
		File recuento = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_RECUENTO + "/");                        
		recuento.mkdirs();                                                                                                           
                                                                                                                                     
		File stockDiario = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_DAILYSTOCK + "/");                   
		stockDiario.mkdirs();                                                                                                        
		                                                                                                                             
		File gdpr = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_GDPR + "/");                                
		gdpr.mkdirs();                                                                                                               
		
		File services = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_SERVICES + "/");                                
		services.mkdirs();

		File logBook = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_LOGBOOK + "/");
		logBook.mkdirs();
                                                                                                                                     
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
                                                                                                                                     
		if (str.equals(Constants.EMPTY_STRING))                                                                                      
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
			File outDir = new File("/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_SERVICES + "/");
	    
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
                                                                                                                                     