package net.ifeu.edicards.Services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.Pactos;
import net.ifeu.edicards.DataTier.Tarifa;
import net.ifeu.edicards.DataTier.TipoIVA;
import net.ifeu.library.LogBook.LogBook;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;


@SuppressLint("ShowToast")
public class ParserResponse extends ParserBase {
	
	public ParserResponse() {
		super();
	}

	private String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}

	private Document getDocument(Document document, boolean compress)
			throws DOMException, IOException, ParserConfigurationException,
			SAXException {
		Element root = document.getDocumentElement();
		
		if (root.getChildNodes().getLength() == 0)
			return null;
		
		Log.i("ParserResponse root", root.getChildNodes().item(0)
				.getTextContent());

		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;

		if (root.getChildNodes().getLength() == 0)
			return null;

		InputStream xml;

		if (compress)
			xml = new ByteArrayInputStream(Compression.decompress(root
					.getChildNodes().item(0).getTextContent().getBytes()));
		else
			xml = new ByteArrayInputStream(root.getChildNodes().item(0)
					.getTextContent().getBytes());

		db = dbf.newDocumentBuilder();
		doc = db.parse(xml);

		return doc;
	}

	private String getValue(Document document) throws DOMException,
			IOException, ParserConfigurationException, SAXException {

		Element root = document.getDocumentElement();
		Log.i("ParserResponse root", root.getChildNodes().item(0)
				.getTextContent());

		if (root.getChildNodes().getLength() == 0)
			return Constants.EMPTY_STRING;

		return root.getChildNodes().item(0).getTextContent();

	}

	public void parseFormasPago(Document document, Context context,
			AppConfig app, FormaPago formaPago, boolean compress) // throws
																	// ParserConfigurationException,
																	// DOMException,
																	// SAXException,
																	// IOException
	{

		try {

			Document doc = getDocument(document, compress);

			if (doc == null)
				return;

			doc.getDocumentElement().normalize();

			NodeList formasPago = doc.getElementsByTagName("FPago");
			Log.i("ParserResponse", String.valueOf(formasPago.getLength()));
			int length = formasPago.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) formasPago.item(i);
				NodeList codigo = element.getElementsByTagName("Codigo");
				NodeList descripcion = element.getElementsByTagName("Descripc");

				Element codigoValue = (Element) codigo.item(0);
				Element descripcionValue = (Element) descripcion.item(0);

				if (!formaPago
						.setFormaPagoByCode(getCharacterDataFromElement(codigoValue))) {
					formaPago.CodigoFormaPago = getCharacterDataFromElement(codigoValue);
					formaPago.Descripcion = getCharacterDataFromElement(descripcionValue);

					formaPago.save();
					this.Monitor().FormaPagoSaveCounter++;
					
				} else {
					formaPago.CodigoFormaPago = getCharacterDataFromElement(codigoValue);
					formaPago.Descripcion = getCharacterDataFromElement(descripcionValue);

					formaPago.update();
					this.Monitor().FormaPagoUpdateCounter++;
				}

				Log.i("ParserResponse",
						getCharacterDataFromElement(codigoValue));
				Log.i("ParserResponse 2",
						getCharacterDataFromElement(descripcionValue));

			}
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
		}

	}

	public void parsePactos(Document document, Context context, AppConfig app,
			Pactos pacto, boolean compress) // throws
											// ParserConfigurationException,
											// DOMException, SAXException,
											// IOException
	{

		try {
			Document doc = getDocument(document, compress);

			if (doc == null)
				return;

			doc.getDocumentElement().normalize();

			NodeList pactos = doc.getElementsByTagName("Pactes");
			Log.i("ParserResponse", String.valueOf(pactos.getLength()));
			int length = pactos.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) pactos.item(i);
				NodeList codigoCliente = element
						.getElementsByTagName("CtaComer");
				NodeList codigoArticulo = element
						.getElementsByTagName("CodArt");
				NodeList pvp = element.getElementsByTagName("PreBase");
				NodeList dte = element.getElementsByTagName("Dte01");

				Element codigoClienteValue = (Element) codigoCliente.item(0);
				Element codigoArticuloValue = (Element) codigoArticulo.item(0);
				Element pvpValue = (Element) pvp.item(0);
				Element dteValue = (Element) dte.item(0);

				Articulo articulo = new Articulo();
				try {
					articulo.InitializePersistance(app, context);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					app.getErrorTrace().Send(app.getUser().User, e);
				}

				articulo.setArticuloByCodigo(getCharacterDataFromElement(codigoArticuloValue));

				Cliente cliente = new Cliente();
				try {
					cliente.InitializePersistance(app, context);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					app.getErrorTrace().Send(app.getUser().User, e);
				}

				cliente.setClienteByCodigo(getCharacterDataFromElement(codigoClienteValue));

				pacto.PVP = Double
						.parseDouble(getCharacterDataFromElement(pvpValue));
				pacto.Descuento1 = Double
						.parseDouble(getCharacterDataFromElement(dteValue));
				pacto.Cliente = cliente;
				pacto.Articulo = articulo;

				if (!pacto.setPactoByClienteArticulo(cliente, articulo)) {
					pacto.save();
					this.Monitor().PactoSaveCounter++;
				} else {
					pacto.update();
					this.Monitor().PactoUpdateCounter++;
				}
				
				articulo.ReleasePersistance();
				cliente.ReleasePersistance();

			}
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
		}

	}

	public void parseTiposIva(Document document, Context context,
			AppConfig app, TipoIVA iva, boolean compress) throws DOMException,
			IOException, ParserConfigurationException, SAXException // throws
																	// ParserConfigurationException,
																	// DOMException,
																	// SAXException,
																	// IOException
	{

		Document doc = getDocument(document, compress);

		if (doc == null)
			return;

		try {
			doc.getDocumentElement().normalize();

			NodeList tiposIva = doc.getElementsByTagName("Iva");
			Log.i("ParserResponse", String.valueOf(tiposIva.getLength()));
			int length = tiposIva.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) tiposIva.item(i);
				NodeList filiacion = element.getElementsByTagName("Filiacio");
				NodeList articulo = element.getElementsByTagName("Articulo");
				NodeList fecha = element.getElementsByTagName("FIniVig");
				NodeList impuesto = element.getElementsByTagName("Impuesto");
				NodeList recargo = element.getElementsByTagName("Recargo");
				NodeList descrip = element.getElementsByTagName("Descripc");

				Element filiacionValue = (Element) filiacion.item(0);
				Element articuloValue = (Element) articulo.item(0);
				Element impuestoValue = (Element) impuesto.item(0);
				Element recargoValue = (Element) recargo.item(0);
				Element fechaValue = (Element) fecha.item(0);
				Element descripValue = (Element) descrip.item(0);

				iva.Articulo = getCharacterDataFromElement(articuloValue);
				iva.Filiacion = getCharacterDataFromElement(filiacionValue);
				iva.Impuesto = Double
						.parseDouble(getCharacterDataFromElement(impuestoValue));
				iva.Recargo = Double
						.parseDouble(getCharacterDataFromElement(recargoValue));
				iva.Descripcion = getCharacterDataFromElement(descripValue);

				Log.i("ParserResponse", iva.Descripcion);

				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				Log.i("Fecha",
						transformWsDate(getCharacterDataFromElement(fechaValue)));
				iva.Fecha = (Date) formatter
						.parse(transformWsDate(getCharacterDataFromElement(fechaValue)));

				iva.save();
				this.Monitor().IvaSaveCounter++;
			}
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
		}
	}

	public void parseTarifas(Document document, Context context, AppConfig app,
			Tarifa tarifa, boolean compress) throws DOMException, IOException,
			ParserConfigurationException, SAXException // throws
														// ParserConfigurationException,
														// DOMException,
														// SAXException,
														// IOException
	{

		Document doc = getDocument(document, compress);

		if (doc == null)
			return;

		try {
			doc.getDocumentElement().normalize();

			NodeList tarifas = doc.getElementsByTagName("Tarifes");
			Log.i("ParserResponse", String.valueOf(tarifas.getLength()));
			int length = tarifas.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) tarifas.item(i);
				NodeList codigoTarifa = element.getElementsByTagName("CodTari");
				NodeList fechaInicio = element.getElementsByTagName("FecVigor");
				NodeList fechaFin = element.getElementsByTagName("FechaFin");
				NodeList codigoArticulo = element
						.getElementsByTagName("CodArti");
				NodeList pvp = element.getElementsByTagName("PreBase");
				NodeList dte = element.getElementsByTagName("Dte01");

				Element codigoTarifaValue = (Element) codigoTarifa.item(0);
				Element fechaInicioValue = (Element) fechaInicio.item(0);
				Element fechaFinValue = (Element) fechaFin.item(0);
				Element codigoArticuloValue = (Element) codigoArticulo.item(0);
				Element pvpValue = (Element) pvp.item(0);
				Element dteValue = (Element) dte.item(0);

				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

				tarifa.CodigoTarifa = getCharacterDataFromElement(codigoTarifaValue);
				tarifa.FechaIni = (Date) formatter
						.parse(transformWsDate(getCharacterDataFromElement(fechaInicioValue)));
				tarifa.FechaFin = (Date) formatter
						.parse(transformWsDate(getCharacterDataFromElement(fechaFinValue)));

				Articulo articulo = new Articulo();
				try {
					articulo.InitializePersistance(app, context);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					app.getErrorTrace().Send(app.getUser().User, e);
				}

				articulo.setArticuloByCodigo(getCharacterDataFromElement(codigoArticuloValue));
				tarifa.Articulo = articulo;

				tarifa.PVP = Double
						.parseDouble(getCharacterDataFromElement(pvpValue));
				tarifa.Descuento1 = Double
						.parseDouble(getCharacterDataFromElement(dteValue));

				tarifa.save();
				this.Monitor().TarifaSaveCounter++;
				
				articulo.ReleasePersistance();
			}
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
		}
	}

	public void parseArticulos(Document document, Context context,
			AppConfig app, Articulo articulo, boolean compress)
			throws Exception // throws ParserConfigurationException,
								// DOMException, SAXException, IOException
	{

		// Log.i("ParserResponse",String.valueOf(document.getDocumentElement().getTextContent().length()));

		Articulo nuevo = new Articulo();
		nuevo.InitializePersistance(app, context);
		if (!nuevo.setArticuloByName(".KILOMETRAJE 1(INICIAL)")) {
			nuevo.Activo = true;
			nuevo.CodigoArticulo = "KM-INICIAL";
			nuevo.Descripcion = ".KILOMETRAJE 1(INICIAL)";
			nuevo.Descuento1 = 0;
			nuevo.Descuento2 = 0;
			nuevo.Familia = Constants.EMPTY_STRING;
			nuevo.FamiliaCorta = Constants.EMPTY_STRING;
			nuevo.PVP = 0;
			nuevo.Tipo = 2;
			nuevo.TipoIVA = Constants.EMPTY_STRING;

			nuevo.save();
		}

		nuevo = new Articulo();
		nuevo.InitializePersistance(app, context);
		if (!nuevo.setArticuloByName(".KILOMETRAJE 2(FINAL)")) {
			nuevo.Activo = true;
			nuevo.CodigoArticulo = "KM-FINAL";
			nuevo.Descripcion = ".KILOMETRAJE 2(FINAL)";
			nuevo.Descuento1 = 0;
			nuevo.Descuento2 = 0;
			nuevo.Familia = Constants.EMPTY_STRING;
			nuevo.FamiliaCorta = Constants.EMPTY_STRING;
			nuevo.PVP = 0;
			nuevo.Tipo = 2;
			nuevo.TipoIVA = Constants.EMPTY_STRING;

			nuevo.save();
		}

		Document doc = getDocument(document, compress);

		if (doc == null)
			return;

		try {

			doc.getDocumentElement().normalize();

			NodeList articulos = doc.getElementsByTagName("Articles");
			Log.i("ParserResponse", String.valueOf(articulos.getLength()));
			int length = articulos.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) articulos.item(i);
				NodeList codigo = element.getElementsByTagName("Codigo");
				NodeList descripcion = element.getElementsByTagName("Descripc");
				NodeList activo = element.getElementsByTagName("Activo");
				NodeList venta = element.getElementsByTagName("FlagVenda");
				NodeList codigoFamilia = element
						.getElementsByTagName("Familia");
				NodeList descripcionFamilia = element
						.getElementsByTagName("FamiliaDesc");
				NodeList descripcionFamiliaCorta = element
						.getElementsByTagName("FamiliaDescCurta");
				NodeList iva = element.getElementsByTagName("Iva");
				NodeList pvp = element.getElementsByTagName("Pvp_01");
				NodeList dte = element.getElementsByTagName("Dte01");
				//NodeList stock = element.getElementsByTagName("Stock");

				Element codigoValue = (Element) codigo.item(0);
				Element descripcionValue = (Element) descripcion.item(0);
				Element activoValue = (Element) activo.item(0);
				Element ventaValue = (Element) venta.item(0);
				Element codigoFamiliaValue = (Element) codigoFamilia.item(0);
				Element descripcionFamiliaValue = (Element) descripcionFamilia
						.item(0);
				Element descripcionFamiliaCortaValue = (Element) descripcionFamiliaCorta
						.item(0);
				Element ivaValue = (Element) iva.item(0);
				Element pvpValue = (Element) pvp.item(0);
				Element dteValue = (Element) dte.item(0);

				if (articulo
						.setArticuloByCodigo(getCharacterDataFromElement(codigoValue))) {

					articulo.CodigoArticulo = getCharacterDataFromElement(codigoValue);
					articulo.Descripcion = getCharacterDataFromElement(descripcionValue);
					articulo.Activo = getCharacterDataFromElement(activoValue)
							.equals("1") ? true : false;
					articulo.Tipo = Integer
							.parseInt(getCharacterDataFromElement(ventaValue));
					articulo.Familia = getCharacterDataFromElement(codigoFamiliaValue);
					articulo.FamiliaCorta = getCharacterDataFromElement(descripcionFamiliaCortaValue);
					articulo.TipoIVA = getCharacterDataFromElement(ivaValue);
					articulo.PVP = Double
							.parseDouble(getCharacterDataFromElement(pvpValue));
					articulo.Descuento1 = Double
							.parseDouble(getCharacterDataFromElement(dteValue));
					
					//Double stockDouble = new Double(getCharacterDataFromElement(stockValue));
					//articulo.Entradas = stockDouble.intValue();
					//articulo.Stock = articulo.Stock + articulo.Entradas; 

					articulo.update();
					this.Monitor().ArticuloUpdateCounter++;

				} else {

					articulo.CodigoArticulo = getCharacterDataFromElement(codigoValue);
					articulo.Descripcion = getCharacterDataFromElement(descripcionValue);
					articulo.Activo = getCharacterDataFromElement(activoValue)
							.equals("1") ? true : false;
					articulo.Tipo = Integer
							.parseInt(getCharacterDataFromElement(ventaValue));
					articulo.Familia = getCharacterDataFromElement(codigoFamiliaValue);
					articulo.FamiliaCorta = getCharacterDataFromElement(descripcionFamiliaValue);
					articulo.TipoIVA = getCharacterDataFromElement(ivaValue);
					articulo.PVP = Double
							.parseDouble(getCharacterDataFromElement(pvpValue));
					articulo.Descuento1 = Double
							.parseDouble(getCharacterDataFromElement(dteValue));

					articulo.Entradas = 0;
					articulo.Stock = 0; 
					articulo.Entradas = 0;
					articulo.StockDefectuoso = 0;

					articulo.save();
					this.Monitor().ArticuloSaveCounter++;
				}
			}
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
		}

	}
	
	public boolean  ParserTraspasoAlmacen(Document document, Context context,
			AppConfig app, Articulo articulo, boolean compress)
			throws Exception
	{

		Document doc = getDocument(document, compress);

		if (doc == null)
			return true;

		try {

			doc.getDocumentElement().normalize();
			NodeList articulos = doc.getElementsByTagName("TrStock");

			Log.i("ParserResponse", String.valueOf(articulos.getLength()));
			int length = articulos.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) articulos.item(i);
				NodeList fecha = element.getElementsByTagName("Dia");
				NodeList codigo = element.getElementsByTagName("A");
				NodeList unidades = element.getElementsByTagName("U");
				
				Element fechaValue = (Element) fecha.item(0);
				Element codigoValue = (Element) codigo.item(0);
				Element unidadesValue = (Element) unidades.item(0);
				
				if (articulo
						.setArticuloByCodigo(getCharacterDataFromElement(codigoValue))) {

					Double stockDouble = new Double(getCharacterDataFromElement(unidadesValue));
					articulo.Entradas = stockDouble.intValue();
					int stockInicial = articulo.Stock;
					articulo.Stock = stockInicial + articulo.Entradas;

					articulo.update();

					app.getTraspasoAlmacen().put(articulo.CodigoArticulo, stockDouble);

					LogBook logBookTrace = new LogBook();
					logBookTrace.InitializePersistance(app, app);
					logBookTrace.setData("TRASPASO ALMACÉN", "","",
							articulo.CodigoArticulo, articulo.Descripcion,
							stockInicial, articulo.Stock, articulo.Entradas, 0,
							0, 0, 0,
							0,0);

					logBookTrace.save();
				}
			}

			return true;
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
			return false;
		}

	}
	
	public boolean  UndoTraspasoAlmacen(Context context,AppConfig app)
			throws Exception // throws ParserConfigurationException,
								// DOMException, SAXException, IOException
	{

		Articulo articulo = new Articulo();
		articulo.InitializePersistance(app, context);
		
		try {

			for (String key : app.getTraspasoAlmacen().keySet()) {
				String codigoValue = key;
				
				if (articulo
						.setArticuloByCodigo(codigoValue)) {

					Double stockDouble = new Double(app.getTraspasoAlmacen().get(codigoValue));
					articulo.Entradas = stockDouble.intValue();
					int stockInicial = articulo.Stock;
					articulo.Stock = stockInicial - articulo.Entradas;

					articulo.update();

					this.Monitor().ArticuloUpdateCounter++;

					LogBook logBookTrace = new LogBook();
					logBookTrace.InitializePersistance(app, app);
					logBookTrace.setData("DESHACER TRASPASO ALMACÉN", "","",
							articulo.CodigoArticulo, articulo.Descripcion,
							stockInicial, articulo.Stock, 0, 0,
							articulo.Entradas, 0, 0,
							0,0);

					logBookTrace.save();
				}
			}

			return true;
		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
			return false;
		}
	}


	public void parseClientes(Document document, Context context,
			AppConfig app, Cliente cliente, boolean compress) throws Exception // throws
																				// ParserConfigurationException,
																				// DOMException,
																				// //
																				// SAXException,
																				// IOException
	{

		// Añadimos el cliente "Nuevo Cliente"

		try{
			Cliente nuevo = new Cliente();
			nuevo.InitializePersistance(app, context);
			
			if (!nuevo.setClienteByName("Nuevo Cliente")) {
				nuevo.CodigoCliente = Constants.NEW_CUSTOMER_CODE;
				nuevo.Nombre = "Nuevo Cliente";
				nuevo.Activo = true;
				nuevo.Clave = Constants.EMPTY_STRING;
				nuevo.CodigoPostal = " ";
				nuevo.CodigoTarifa = Constants.EMPTY_STRING;
				nuevo.Descuento1 = 0;
				nuevo.Descuento2 = 0;
				nuevo.DescuentoFinanciero = 0;
				nuevo.DescuentoProntoPago = 0;
				nuevo.Direccion1 = " ";
				nuevo.Direccion2 = Constants.EMPTY_STRING;
				nuevo.Fax = Constants.EMPTY_STRING;
				nuevo.Filiacion = "002";
	
				FormaPago pago = new FormaPago();
				pago.IdFormaPago = Long.parseLong("0009");
				nuevo.formaPago = pago;
				nuevo.Mail = Constants.EMPTY_STRING;
				nuevo.NIF = " ";
				nuevo.Poblacion = " ";
				nuevo.Provincia = " ";
				nuevo.Razon = " ";
				nuevo.Telefono1 = Constants.EMPTY_STRING;
				nuevo.Telefono2 = Constants.EMPTY_STRING;
				nuevo.Web = Constants.EMPTY_STRING;
				
				nuevo.ClienteInfo.assignCCC("X", "X", "X", "X", "X");
	
				nuevo.save();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			app.getErrorTrace().Send(app.getUser().User, e);
		}

		Document doc = getDocument(document, compress);

		if (doc == null)
			return;

		try {

			doc.getDocumentElement().normalize();

			NodeList clientes = doc.getElementsByTagName("Clients");
			Log.i("ParserResponse", String.valueOf(clientes.getLength()));
			int length = clientes.getLength();
			for (int i = 0; i < length; i++) {

				Element element = (Element) clientes.item(i);
				NodeList codigo = element.getElementsByTagName("Codigo");
				NodeList nombre = element.getElementsByTagName("Nombre");
				NodeList razon = element.getElementsByTagName("Razon");
				NodeList nif = element.getElementsByTagName("Nif");
				NodeList direccion = element.getElementsByTagName("Direccio");
				NodeList direccion2 = element.getElementsByTagName("Direccio2");
				NodeList provincia = element.getElementsByTagName("Provincia");
				NodeList codigoPostal = element.getElementsByTagName("CPostal");
				NodeList localidad = element.getElementsByTagName("Localida");
				NodeList telefono1 = element.getElementsByTagName("Tlfno_1");
				NodeList telefono2 = element.getElementsByTagName("Tlfno_2");
				NodeList fax = element.getElementsByTagName("Fax");
				NodeList web = element.getElementsByTagName("DirWeb");
				NodeList mail = element.getElementsByTagName("DirEmail");
				NodeList clave = element.getElementsByTagName("Clave");
				NodeList activo = element.getElementsByTagName("Activo");
				NodeList iva = element.getElementsByTagName("IVA");
				NodeList formaPago = element.getElementsByTagName("FPago");
				NodeList dteProntoPago = element.getElementsByTagName("PPago");
				NodeList dteFinanciero = element
						.getElementsByTagName("CFinancer");
				NodeList idDeposito = element.getElementsByTagName("DP");
				NodeList codigoBanco = element.getElementsByTagName("CodBanco");
				NodeList codigoAgencia = element.getElementsByTagName("CodAgenc");
				NodeList digitoControl = element.getElementsByTagName("DigCtrol");
				NodeList numeroCuenta = element.getElementsByTagName("NumCta");
				NodeList iban = element.getElementsByTagName("DC_IBAN");

				Element codigoValue = (Element) codigo.item(0);
				Element nombreValue = (Element) nombre.item(0);
				Element razonValue = (Element) razon.item(0);
				Element nifValue = (Element) nif.item(0);
				Element direccionValue = (Element) direccion.item(0);
				Element direccion2Value = (Element) direccion2.item(0);
				Element provinciaValue = (Element) provincia.item(0);
				Element codigoPostalValue = (Element) codigoPostal.item(0);
				Element localidadValue = (Element) localidad.item(0);
				Element telefono1Value = (Element) telefono1.item(0);
				Element telefono2Value = (Element) telefono2.item(0);
				Element faxValue = (Element) fax.item(0);
				Element webValue = (Element) web.item(0);
				Element mailValue = (Element) mail.item(0);
				Element claveValue = (Element) clave.item(0);
				Element activoValue = (Element) activo.item(0);
				Element ivaValue = (Element) iva.item(0);
				Element formaPagoValue = (Element) formaPago.item(0);
				Element dteProntoPagoValue = (Element) dteProntoPago.item(0);
				Element dteFinancieroValue = (Element) dteFinanciero.item(0);
				Element idDepositoValue = (Element) idDeposito.item(0);
				Element codigoBancoValue = (Element) codigoBanco.item(0);
				Element codigoAgenciaValue = (Element) codigoAgencia.item(0);
				Element digitoControlValue = (Element) digitoControl.item(0);
				Element numeroCuentaValue = (Element) numeroCuenta.item(0);
				Element ibanValue = (Element) iban.item(0);
 
				Log.i("ParserResponse Cliente", "Passo per aqui II");
				
				//int records = cliente.removeRepeats(getCharacterDataFromElement(codigoValue));
				
				if (!cliente
						.setClienteByCodigoStatus(getCharacterDataFromElement(codigoValue))) {

					Log.i("Cliente Save", "Save()");
					cliente.CodigoCliente = getCharacterDataFromElement(codigoValue);
					cliente.Nombre = getCharacterDataFromElement(nombreValue);
					cliente.Razon = getCharacterDataFromElement(razonValue);
					cliente.NIF = getCharacterDataFromElement(nifValue);
					cliente.Direccion1 = getCharacterDataFromElement(direccionValue);
					cliente.Direccion2 = getCharacterDataFromElement(direccion2Value);
					cliente.Provincia = getCharacterDataFromElement(provinciaValue);
					cliente.CodigoPostal = getCharacterDataFromElement(codigoPostalValue);
					cliente.Poblacion = getCharacterDataFromElement(localidadValue);
					cliente.Poblacion = getCharacterDataFromElement(localidadValue);
					cliente.Telefono1 = getCharacterDataFromElement(telefono1Value);
					cliente.Telefono2 = getCharacterDataFromElement(telefono2Value);
					cliente.Fax = getCharacterDataFromElement(faxValue);
					cliente.Web = getCharacterDataFromElement(webValue);
					cliente.Mail = getCharacterDataFromElement(mailValue);
					cliente.Clave = getCharacterDataFromElement(claveValue);
					cliente.Activo = getCharacterDataFromElement(activoValue)
							.equals("1") ? true : false;
					cliente.Filiacion = getCharacterDataFromElement(ivaValue);
					cliente.DescuentoProntoPago = Double
							.parseDouble(getCharacterDataFromElement(dteProntoPagoValue));
					cliente.DescuentoFinanciero = Double
							.parseDouble(getCharacterDataFromElement(dteFinancieroValue));
					

					FormaPago pago = new FormaPago();
					try {
						pago.InitializePersistance(app, context);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						app.getErrorTrace().Send(app.getUser().User, e);
					}

					pago.setFormaPagoByCode(getCharacterDataFromElement(formaPagoValue));
					cliente.formaPago = pago;
					
					cliente.ClienteInfo.InitializePersistance(app, context);
					
					String codigoBancoString = getCharacterDataFromElement(codigoBancoValue);
					String codigoAgenciaString = getCharacterDataFromElement(codigoAgenciaValue);
					String digitoControlString = getCharacterDataFromElement(digitoControlValue);
					String numeroCuentaString = getCharacterDataFromElement(numeroCuentaValue);
					String ibanString = getCharacterDataFromElement(ibanValue);
					
					cliente.ClienteInfo.assignCCC(codigoBancoString, codigoAgenciaString, digitoControlString, numeroCuentaString, ibanString);
					cliente.ClienteInfo.Cliente = cliente;
				
					cliente.save();
					this.Monitor().ClienteSaveCounter++;

					Deposito deposito = new Deposito();
					deposito.InitializePersistance(app, context);

					if (deposito
							.setDepositoById(getCharacterDataFromElement(idDepositoValue))) {
						deposito.assingFromCliente(cliente);
						deposito.update();
						this.Monitor().DepositoUpdateCounter++;
					}
					
					//pago.ReleasePersistance();
					//deposito.ReleasePersistance();

				} else {

					cliente.CodigoCliente = getCharacterDataFromElement(codigoValue);
					cliente.Nombre = getCharacterDataFromElement(nombreValue);
					cliente.Razon = getCharacterDataFromElement(razonValue);
					cliente.NIF = getCharacterDataFromElement(nifValue);
					cliente.Direccion1 = getCharacterDataFromElement(direccionValue);
					cliente.Direccion2 = getCharacterDataFromElement(direccion2Value);
					cliente.Provincia = getCharacterDataFromElement(provinciaValue);
					cliente.CodigoPostal = getCharacterDataFromElement(codigoPostalValue);
					cliente.Poblacion = getCharacterDataFromElement(localidadValue);
					cliente.Poblacion = getCharacterDataFromElement(localidadValue);
					cliente.Telefono1 = getCharacterDataFromElement(telefono1Value);
					cliente.Telefono2 = getCharacterDataFromElement(telefono2Value);
					cliente.Fax = getCharacterDataFromElement(faxValue);
					cliente.Web = getCharacterDataFromElement(webValue);
					cliente.Mail = getCharacterDataFromElement(mailValue);
					cliente.Clave = getCharacterDataFromElement(claveValue);
					cliente.Activo = getCharacterDataFromElement(activoValue)
							.equals("1") ? true : false;
					cliente.Filiacion = getCharacterDataFromElement(ivaValue);
					cliente.DescuentoProntoPago = Double
							.parseDouble(getCharacterDataFromElement(dteProntoPagoValue));
					cliente.DescuentoFinanciero = Double
							.parseDouble(getCharacterDataFromElement(dteFinancieroValue));

					FormaPago pago = new FormaPago();
					try {
						pago.InitializePersistance(app, context);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						app.getErrorTrace().Send(app.getUser().User, e);
					}

					pago.setFormaPagoByCode(getCharacterDataFromElement(formaPagoValue));
					cliente.formaPago = pago;
					
					cliente.ClienteInfo.InitializePersistance(app, context);
					
					String codigoBancoString = getCharacterDataFromElement(codigoBancoValue);
					String codigoAgenciaString = getCharacterDataFromElement(codigoAgenciaValue);
					String digitoControlString = getCharacterDataFromElement(digitoControlValue);
					String numeroCuentaString = getCharacterDataFromElement(numeroCuentaValue);
					String ibanString = getCharacterDataFromElement(ibanValue);
					
					cliente.ClienteInfo.assignCCC(codigoBancoString, codigoAgenciaString, digitoControlString, numeroCuentaString, ibanString);
					cliente.ClienteInfo.Cliente = cliente;

					cliente.update();
					
					this.Monitor().ClienteUpdateCounter++;
					
					//pago.ReleasePersistance();
					//cliente.ReleasePersistance();
				}
			}

		} catch (Exception e) {
			app.getErrorTrace().Send(app.getUser().User, e);
		}

	}

	@SuppressLint("ShowToast")
	public void parseDepositos(Document document, Context context,
			AppConfig app, Deposito deposito, boolean compress) throws Exception
	{
		
		Document doc = getDocument(document, compress);

		if (doc == null)
			return;

		Deposito depo = new Deposito();
		depo.InitializePersistance(app, context);
		
		Articulo articulo = new Articulo();
		articulo.InitializePersistance(app, context);
		LinkedHashMap<String, Articulo> articulos = articulo.getAllArticulos(1);
		
		LinkedHashMap<String, Deposito> cacheDepositos = new LinkedHashMap<String, Deposito>();
		
		try {
			doc.getDocumentElement().normalize();

			NodeList depositos = doc.getElementsByTagName("VD");
			Log.i("ParserResponse", String.valueOf(depositos.getLength()));
			int length = depositos.getLength();

			for (int i = 0; i < length; i++) {								
					
				try {

					Element element = (Element) depositos.item(i);
					NodeList codigoCliente = element.getElementsByTagName("E1");
					NodeList codigoArticulo = element.getElementsByTagName("E2");
					NodeList unidades = element.getElementsByTagName("E3");
					NodeList numDoc = element.getElementsByTagName("E0");
					NodeList pvp = element.getElementsByTagName("E4");
					NodeList ejercicio = element.getElementsByTagName("E5");
	
					Element codigoClienteValue = (Element) codigoCliente.item(0);
					Element codigoArticuloValue = (Element) codigoArticulo.item(0);
					Element unidadesValue = (Element) unidades.item(0);
					Element numDocValue = (Element) numDoc.item(0);
					Element pvpValue = (Element) pvp.item(0);
					Element ejercicioValue = (Element) ejercicio.item(0);
	
					String strCodigoCliente = getCharacterDataFromElement(codigoClienteValue);
					float intUnidades = Float
							.parseFloat(getCharacterDataFromElement(unidadesValue));
					float dblPVP = Float
							.parseFloat(getCharacterDataFromElement(pvpValue));
					float dblNumDoc = Float
							.parseFloat(getCharacterDataFromElement(numDocValue));
					String strNumDoc = String.valueOf(Math.round(dblNumDoc));
					String strEjercicio = getCharacterDataFromElement(ejercicioValue);
					String strCodigoArticulo = getCharacterDataFromElement(codigoArticuloValue); 
	
					if (cacheDepositos.containsKey(strCodigoCliente)) {
						depo = cacheDepositos.get(strCodigoCliente);
					} else {
					
						if (!depo.setFirstDepositoByCliente(strCodigoCliente)) {
							
							Cliente cliente = new Cliente();
							cliente.InitializePersistance(app, context);
							
							cliente.setClienteByCodigo(strCodigoCliente);
			
							Log.i("ParserResponse", "Assignem client a nou dipòsit :"
									+ cliente.CodigoCliente + " , " + cliente.Nombre);
			
							depo = new Deposito();
							depo.InitializePersistance(app, context);
							
							depo.assingFromCliente(cliente);
							depo.NumDoc = strNumDoc;
							depo.FechaDeposito = new Date();
							depo.TipoDeposito = Constants.TIPO_DEPOSITO_CONVENCIONAL;
							depo.Ejercicio = strEjercicio;
		
							depo.save();
							
							this.Monitor().DepositoSaveCounter++;
							
							cacheDepositos.put(strCodigoCliente, depo);
							
						}
						
					}
					
					Articulo art = new Articulo();
					
					if (articulos.containsKey(strCodigoArticulo)) 
						art = articulos.get(strCodigoArticulo);
					
					LineaDeposito linea = new LineaDeposito();
					linea.InitializePersistance(app, context);
					
					linea.Articulo = art;
					linea.Deposito = depo;
					linea.UnidadesIniciales = Math.round(intUnidades);
					linea.PVPAnterior = dblPVP;

					linea.save();
					this.Monitor().LineaSaveCounter++;
					
				}
				catch (Exception e) {
					continue;
				}
					
			}
			
		} catch (Exception e) {
		}
	}

	public Long parseTotalDepositos(Document document, Context context,
			AppConfig app) throws IllegalArgumentException,
			IllegalStateException, IOException, DOMException,
			ParserConfigurationException, SAXException {

		String strTotal = getValue(document);
		return Long.parseLong(strTotal);

	}

	private String transformWsDate(String date) {
		return date.substring(8, 10) + "/" + date.substring(5, 7) + "/"
				+ date.substring(0, 4);
	}
	
	public ParserMonitor Monitor() {
		return this._monitor;
	}

}
