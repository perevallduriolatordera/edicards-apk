package net.ifeu.edicards.Xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.DTOLineaDeposito;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Gasto;
import net.ifeu.edicards.DataTier.GastosInfo;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.LineaHistorico;
import net.ifeu.edicards.DataTier.LineaMovimientos;
import android.content.Context;
import android.os.Environment;

public class XmlCreator {

	private AppConfig _appConfig;
	private Context _context;

	public XmlCreator(AppConfig appConfig, Context context) {
		_appConfig = appConfig;
		_context = context;
	}
	
	public void createXmlRecuento() throws Exception {
		this.createXmlDailyStock();
	}

	
	public void createXmlArticulos() throws Exception {

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_STOCK
				+ "/"
				+ Constants.FILE_STOCK
				+ "."
				+ String.valueOf(0));
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		Articulo articulo = new Articulo();

		try {
			articulo.InitializePersistance(_appConfig,
					_context.getApplicationContext());
		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		// Obtenemos el histórico a partir de la fecha del día

		Historico historico = new Historico();
		historico.InitializePersistance(_appConfig,
				_context.getApplicationContext());

		Calendar calendar1 = this.setWeekStart(Calendar.getInstance());
		Calendar calendar2 = Calendar.getInstance();
		calendar2.add(Calendar.DATE, 7);

		ArrayList<Historico> list = historico.getHistoricosBetweenDates(
				calendar1.getTime(), calendar2.getTime());

		HashMap<String, Reports> report = this.getReports(list);

		fileos.write(("<Stock>")
				.getBytes());
		
		fileos.write("<US>".getBytes());
		fileos.write(_appConfig.getUser().User.getBytes());
		fileos.write("</US>".getBytes());
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		fileos.write("<FR>".getBytes());
		fileos.write(String.valueOf(
				formatter.format(calendar1.getTime()))
				.getBytes());
		fileos.write("</FR>".getBytes());
		
		fileos.write(("<Articulos>")
				.getBytes());


		for (Articulo articuloInCatalgo : articulo.getAllArticulos(1).values()) {
			
			try {
				fileos.write("<A>".getBytes());

				fileos.write("<CA>".getBytes());
				fileos.write(articuloInCatalgo.CodigoArticulo.getBytes());
				fileos.write("</CA>".getBytes());

				fileos.write("<SC>".getBytes());
				fileos.write(String.valueOf(articuloInCatalgo.Stock)
						.getBytes());
				fileos.write("</SC>".getBytes());

				fileos.write("<SD>".getBytes());
				fileos.write(String.valueOf(
						articuloInCatalgo.StockDefectuoso).getBytes());
				fileos.write("</SD>".getBytes());

				if (!report.containsKey(articuloInCatalgo.CodigoArticulo)) {
					

					fileos.write("<PR>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</PR>".getBytes());

					fileos.write("<RR>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</RR>".getBytes());

				} else {
					Reports item = (Reports) report
							.get(articuloInCatalgo.CodigoArticulo);

					fileos.write("<PR>".getBytes());
					fileos.write(String.valueOf(item.Potenciados)
							.getBytes());
					fileos.write("</PR>".getBytes());

					fileos.write("<RR>".getBytes());
					fileos.write(String.valueOf(item.Retirados).getBytes());
					fileos.write("</RR>".getBytes());

				}

				fileos.write("</A>".getBytes());

			} catch (Exception e) {
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
						e);
			}
			
		}

		fileos.write("</Articulos>".getBytes());
		fileos.write("</Stock>".getBytes());
		fileos.close();
	}
	
	public void createXmlDailyStock() throws Exception {

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_DAILYSTOCK
				+ "/"
				+ Constants.FILE_DAILY_STOCK);
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		Articulo articulo = new Articulo();

		try {
			articulo.InitializePersistance(_appConfig,
					_context.getApplicationContext());
		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}
		
		fileos.write("<StockDiario>".getBytes());
		
		fileos.write("<US>".getBytes());
		fileos.write(_appConfig.getUser().User.getBytes());
		fileos.write("</US>".getBytes());
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		String date = formatter.format(new Date());
		
		fileos.write("<FE>".getBytes());
		fileos.write(date.getBytes());
		fileos.write("</FE>".getBytes());
		
		fileos.write("<Articulos>".getBytes());

		for (Articulo articuloInCatalgo : articulo.getAllArticulos(1).values()) {
			
			try {
				
				fileos.write("<A>".getBytes());

				fileos.write("<CA>".getBytes());
				fileos.write(articuloInCatalgo.CodigoArticulo.getBytes());
				fileos.write("</CA>".getBytes());

				fileos.write("<SC>".getBytes());
				fileos.write(String.valueOf(articuloInCatalgo.Stock)
						.getBytes());
				fileos.write("</SC>".getBytes());

				fileos.write("</A>".getBytes());

			} catch (Exception e) {
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
						e);
			}
				
		}

		fileos.write("</Articulos>".getBytes());
		fileos.write("</StockDiario>".getBytes());

		fileos.close();
	}

	public void createXmlArticulos(int block) throws Exception {

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_STOCK
				+ "/"
				+ Constants.FILE_STOCK
				+ "."
				+ String.valueOf(block));
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		Articulo articulo = new Articulo();

		try {
			articulo.InitializePersistance(_appConfig,
					_context.getApplicationContext());
		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		// Obtenemos el histórico a partir de la fecha del día

		Historico historico = new Historico();
		historico.InitializePersistance(_appConfig,
				_context.getApplicationContext());

		Calendar calendar1 = this.setWeekStart(Calendar.getInstance());
		Calendar calendar2 = Calendar.getInstance();
		calendar2.add(Calendar.DATE, 7);

		ArrayList<Historico> list = historico.getHistoricosBetweenDates(
				calendar1.getTime(), calendar2.getTime());

		HashMap<String, Reports> report = this.getReports(list);

		fileos.write(("<Articulos index='" + String.valueOf(block) + "'>")
				.getBytes());

		int total = articulo.getAllArticulos(1).size();
		int start;
		int end;

		if (block == 1) {
			start = 0;
			end = total / 2;
		} else {
			start = (total / 2) + 1;
			end = total - 1;
		}

		int i = 0;

		for (Articulo articuloInCatalgo : articulo.getAllArticulos(1).values()) {

			if (i >= start && i <= end) {
				try {
					fileos.write("<A>".getBytes());

					fileos.write("<EM>".getBytes());
					fileos.write(_appConfig.getUser().Company.getBytes());
					fileos.write("</EM>".getBytes());

					fileos.write("<US>".getBytes());
					fileos.write(_appConfig.getUser().User.getBytes());
					fileos.write("</US>".getBytes());

					fileos.write("<CA>".getBytes());
					fileos.write(articuloInCatalgo.CodigoArticulo.getBytes());
					fileos.write("</CA>".getBytes());

					fileos.write("<SC>".getBytes());
					fileos.write(String.valueOf(articuloInCatalgo.Stock)
							.getBytes());
					fileos.write("</SC>".getBytes());

					fileos.write("<SD>".getBytes());
					fileos.write(String.valueOf(
							articuloInCatalgo.StockDefectuoso).getBytes());
					fileos.write("</SD>".getBytes());

					SimpleDateFormat formatter;
					formatter = new SimpleDateFormat("dd/MM/yyyy");

					if (!report.containsKey(articuloInCatalgo.CodigoArticulo)) {
						fileos.write("<FR>".getBytes());
						fileos.write(String.valueOf(
								formatter.format(calendar1.getTime()))
								.getBytes());
						fileos.write("</FR>".getBytes());

						fileos.write("<PR>".getBytes());
						fileos.write(String.valueOf(0).getBytes());
						fileos.write("</PR>".getBytes());

						fileos.write("<RR>".getBytes());
						fileos.write(String.valueOf(0).getBytes());
						fileos.write("</RR>".getBytes());

					} else {
						Reports item = (Reports) report
								.get(articuloInCatalgo.CodigoArticulo);

						fileos.write("<FR>".getBytes());
						fileos.write(String.valueOf(
								formatter.format(calendar1.getTime()))
								.getBytes());
						fileos.write("</FR>".getBytes());

						fileos.write("<PR>".getBytes());
						fileos.write(String.valueOf(item.Potenciados)
								.getBytes());
						fileos.write("</PR>".getBytes());

						fileos.write("<RR>".getBytes());
						fileos.write(String.valueOf(item.Retirados).getBytes());
						fileos.write("</RR>".getBytes());

					}

					fileos.write("</A>".getBytes());

				} catch (Exception e) {
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}

			i++;
		}

		fileos.write("</Articulos>".getBytes());
		fileos.close();
	}

	public void createXmlGastos(Date fecha) throws Exception {

		Gasto gasto = new Gasto();
		gasto.InitializePersistance(_appConfig, _context);

		Articulo articulo = new Articulo();
		articulo.InitializePersistance(_appConfig, _context);

		SimpleDateFormat formatterDate;
		formatterDate = new SimpleDateFormat("yyyyMMdd");

		String fileName = formatterDate.format(fecha) + ".xml";

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_GASTOS + "/" + fileName);
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		GastosInfo gastosInfo = new GastosInfo();
		gastosInfo.InitializePersistance(_appConfig, _context);
		
		GastosInfo comment = gastosInfo.getGastoInfoByFecha(fecha);
		
		fileos.write(("<Gastos comment='" + comment.Comentario + "'>").getBytes());

		DecimalFormat dec = new DecimalFormat("#.##");

		int total = 0;

		for (Articulo articuloInCatalgo : articulo.getAllArticulos(2).values()) {

			if (gasto.setGastoByFechaArticulo(fecha, articuloInCatalgo)) {
				try {

					total++;

					fileos.write("<Gasto>".getBytes());

					fileos.write("<Empresa>".getBytes());
					fileos.write(_appConfig.getUser().Company.getBytes());
					fileos.write("</Empresa>".getBytes());

					fileos.write("<Usuario>".getBytes());
					fileos.write(_appConfig.getUser().User.getBytes());
					fileos.write("</Usuario>".getBytes());

					fileos.write("<CodigoArticulo>".getBytes());
					fileos.write(gasto.Articulo.CodigoArticulo.getBytes());
					fileos.write("</CodigoArticulo>".getBytes());

					SimpleDateFormat formatter;
					formatter = new SimpleDateFormat("dd/MM/yyyy");

					fileos.write("<Fecha>".getBytes());
					fileos.write(String.valueOf(formatter.format(fecha))
							.getBytes());
					fileos.write("</Fecha>".getBytes());

					fileos.write("<Importe>".getBytes());
					fileos.write(String.valueOf(dec.format(gasto.Cantidad))
							.getBytes());
					fileos.write("</Importe>".getBytes());

					fileos.write("</Gasto>".getBytes());

				} catch (Exception e) {
					fileos.write("</Gastos>".getBytes());
					fileos.close();
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				} 
			}
		}

		Historico historico = new Historico();
		historico.InitializePersistance(_appConfig, _context);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fecha);
		calendar.add(Calendar.DATE, 6); // number of days to add
		Date fecha2 = calendar.getTime();

		ArrayList<Historico> list = historico.getHistoricosBetweenDates(fecha,
				fecha2);

		int nuevos = 0;
		int bajas = 0;
		for (Historico hist : list) {
			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_NUEVO)
				nuevos += 1;

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_BAJA)
				bajas += 1;
		}

		try {

			// Clientes nuevos
			fileos.write("<Gasto>".getBytes());

			fileos.write("<Empresa>".getBytes());
			fileos.write(_appConfig.getUser().Company.getBytes());
			fileos.write("</Empresa>".getBytes());

			fileos.write("<Usuario>".getBytes());
			fileos.write(_appConfig.getUser().User.getBytes());
			fileos.write("</Usuario>".getBytes());

			fileos.write("<CodigoArticulo>".getBytes());
			fileos.write("NUEVOS".getBytes());
			fileos.write("</CodigoArticulo>".getBytes());

			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");

			fileos.write("<Fecha>".getBytes());
			fileos.write(String.valueOf(formatter.format(fecha)).getBytes());
			fileos.write("</Fecha>".getBytes());

			fileos.write("<Importe>".getBytes());
			fileos.write(String.valueOf(nuevos).getBytes());
			fileos.write("</Importe>".getBytes());

			fileos.write("</Gasto>".getBytes());

		} catch (Exception e) {
			fileos.write("</Gastos>".getBytes());
			fileos.close();
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		try {

			// Clientes bajas
			fileos.write("<Gasto>".getBytes());

			fileos.write("<Empresa>".getBytes());
			fileos.write(_appConfig.getUser().Company.getBytes());
			fileos.write("</Empresa>".getBytes());

			fileos.write("<Usuario>".getBytes());
			fileos.write(_appConfig.getUser().User.getBytes());
			fileos.write("</Usuario>".getBytes());

			fileos.write("<CodigoArticulo>".getBytes());
			fileos.write("BAJAS".getBytes());
			fileos.write("</CodigoArticulo>".getBytes());

			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");

			fileos.write("<Fecha>".getBytes());
			fileos.write(String.valueOf(formatter.format(fecha)).getBytes());
			fileos.write("</Fecha>".getBytes());

			fileos.write("<Importe>".getBytes());
			fileos.write(String.valueOf(bajas).getBytes());
			fileos.write("</Importe>".getBytes());

			fileos.write("</Gasto>".getBytes());

		} catch (Exception e) {
			fileos.write("</Gastos>".getBytes());
			fileos.close();
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		fileos.write("</Gastos>".getBytes());

		if (total > 0)
			fileos.close();

		fileos = null;
	}

	public void createXmlAlbaran(Deposito deposito) throws Exception {

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_ALBARANES
				+ "/"
				+ String.valueOf(deposito.Serie)
				+ String.valueOf(deposito.NumeroAlbaran) + ".xml");
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		fileos.write("<Albaran>".getBytes());

		fileos.write("<Empresa>".getBytes());
		fileos.write(_appConfig.getUser().Company.getBytes());
		fileos.write("</Empresa>".getBytes());

		fileos.write("<Usuario>".getBytes());
		fileos.write(_appConfig.getUser().User.getBytes());
		fileos.write("</Usuario>".getBytes());

		fileos.write("<NumeroAlbaran>".getBytes());
		fileos.write(deposito.NumeroAlbaran.getBytes());
		fileos.write("</NumeroAlbaran>".getBytes());

		fileos.write("<Serie>".getBytes());

		if (deposito.Serie.equals(_appConfig.getUser().SerialInvoiceA))
			fileos.write(String.valueOf(Constants.SERIE_A_VALUE).getBytes());
		else
			
			fileos.write(String.valueOf(Constants.SERIE_B_VALUE).getBytes());

		fileos.write("</Serie>".getBytes());

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		fileos.write("<FechaDeposito>".getBytes());
		fileos.write(formatter.format(deposito.FechaDeposito).getBytes());
		fileos.write("</FechaDeposito>".getBytes());

		fileos.write("<CodigoCliente>".getBytes());
		fileos.write(deposito.Cliente.CodigoCliente.getBytes());
		fileos.write("</CodigoCliente>".getBytes());

		fileos.write("<Nombre>".getBytes());

		if (deposito.Nombre != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Nombre).getBytes());
		fileos.write("</Nombre>".getBytes());

		fileos.write("<NIF>".getBytes());
		if (deposito.NIF != Constants.EMPTY_STRING)
			fileos.write(deposito.NIF.getBytes());
		fileos.write("</NIF>".getBytes());

		fileos.write("<Razon>".getBytes());
		if (deposito.Razon != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Razon).getBytes());
		fileos.write("</Razon>".getBytes());

		fileos.write("<Direccion1>".getBytes());
		if (deposito.Direccion1 != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Direccion1).getBytes());
		;
		fileos.write("</Direccion1>".getBytes());

		fileos.write("<Direccion2>".getBytes());
		if (deposito.Direccion2 != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Direccion2).getBytes());
		;
		fileos.write("</Direccion2>".getBytes());

		fileos.write("<CodigoPostal>".getBytes());
		if (deposito.CodigoPostal != Constants.EMPTY_STRING)
			fileos.write(deposito.CodigoPostal.getBytes());
		;
		fileos.write("</CodigoPostal>".getBytes());

		fileos.write("<Poblacion>".getBytes());
		if (deposito.Poblacion != Constants.EMPTY_STRING)
			fileos.write(deposito.Poblacion.getBytes());
		;
		fileos.write("</Poblacion>".getBytes());

		fileos.write("<Provincia>".getBytes());
		if (deposito.Provincia != Constants.EMPTY_STRING)
			fileos.write(Constants.EMPTY_STRING.getBytes());
		fileos.write("</Provincia>".getBytes());

		fileos.write("<Telefono1>".getBytes());
		if (deposito.Telefono1 != Constants.EMPTY_STRING)
			fileos.write(deposito.Telefono1.getBytes());
		;
		fileos.write("</Telefono1>".getBytes());

		fileos.write("<Telefono2>".getBytes());
		if (deposito.Telefono2 != Constants.EMPTY_STRING)
			fileos.write(deposito.Telefono2.getBytes());
		;
		fileos.write("</Telefono2>".getBytes());

		fileos.write("<Fax>".getBytes());
		if (deposito.Fax != Constants.EMPTY_STRING)
			fileos.write(deposito.Fax.getBytes());
		;
		fileos.write("</Fax>".getBytes());

		fileos.write("<Mail>".getBytes());
		if (deposito.Mail != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Mail).getBytes());
		;
		fileos.write("</Mail>".getBytes());

		fileos.write("<Web>".getBytes());
		if (deposito.Web != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Web).getBytes());
		;
		fileos.write("</Web>".getBytes());

		fileos.write("<DescuentoComercial>".getBytes());
		fileos.write(String.valueOf(deposito.Totales.DescuentoProntoPago)
				.getBytes());
		fileos.write("</DescuentoComercial>".getBytes());

		fileos.write("<DescuentoFinanciero>".getBytes());
		fileos.write(String.valueOf(deposito.Totales.DescuentoFinanciero)
				.getBytes());
		fileos.write("</DescuentoFinanciero>".getBytes());

		fileos.write("<FormaPago>".getBytes());

		if (deposito.formaPago != null)
			fileos.write(deposito.formaPago.CodigoFormaPago.getBytes());
		else
			fileos.write(Constants.EMPTY_STRING.getBytes());
		
		fileos.write("</FormaPago>".getBytes());
		
		fileos.write("<Filiacion>".getBytes());
		fileos.write(deposito.Filiacion.getBytes());
		fileos.write("</Filiacion>".getBytes());

		fileos.write("<TotalAlbaran>".getBytes());
		fileos.write(String.valueOf(deposito.Totales.Total).getBytes());
		fileos.write("</TotalAlbaran>".getBytes());

		fileos.write("<CantidadPagada>".getBytes());
		fileos.write(String.valueOf(deposito.CantidadPagada).getBytes());
		fileos.write("</CantidadPagada>".getBytes());

		fileos.write("<Lineas>".getBytes());

		for (LineaDeposito linea : deposito.Lineas.values()) {
			try {
				
				LineaMovimientos movimientos = linea.getMovimientos();
				
				if (linea.UnidadesFacturadas > 0) {

					fileos.write("<Linea>".getBytes());

					fileos.write("<CodigoArticulo>".getBytes());
					fileos.write(linea.Articulo.CodigoArticulo.getBytes());
					fileos.write("</CodigoArticulo>".getBytes());

					fileos.write("<Unidades>".getBytes());
					fileos.write(String.valueOf(linea.UnidadesFacturadas)
							.getBytes());
					fileos.write("</Unidades>".getBytes());

					fileos.write("<PVP>".getBytes());
					fileos.write(String.valueOf(linea.PVP).getBytes());
					fileos.write("</PVP>".getBytes());
					
					fileos.write("<VentaDirecta>".getBytes());
					fileos.write(String.valueOf(movimientos.TotalVentaDirecta).getBytes());
					fileos.write("</VentaDirecta>".getBytes());
					
					fileos.write("<VentaDeposito>".getBytes());
					fileos.write(String.valueOf(movimientos.TotalVentaDeposito - movimientos.TotalAbono).getBytes());
					fileos.write("</VentaDeposito>".getBytes());

					fileos.write("</Linea>".getBytes());
				}

				if (linea.UnidadesAbono > 0) {

					fileos.write("<Linea>".getBytes());

					fileos.write("<CodigoArticulo>".getBytes());
					fileos.write(linea.Articulo.CodigoArticulo.getBytes());
					fileos.write("</CodigoArticulo>".getBytes());

					fileos.write("<Unidades>".getBytes());
					fileos.write(String.valueOf(linea.UnidadesAbono * -1)
							.getBytes());
					fileos.write("</Unidades>".getBytes());

					fileos.write("<PVP>".getBytes());
					fileos.write(String.valueOf(linea.PVPAbono).getBytes());
					fileos.write("</PVP>".getBytes());
					
					fileos.write("<VentaDirecta>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</VentaDirecta>".getBytes());
					
					fileos.write("<VentaDeposito>".getBytes());
					fileos.write(String.valueOf(movimientos.TotalAbono).getBytes());
					fileos.write("</VentaDeposito>".getBytes());


					fileos.write("</Linea>".getBytes());
				}

			} catch (Exception e) {
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			}
		}

		fileos.write("</Lineas>".getBytes());
		fileos.write("</Albaran>".getBytes());
		fileos.close();
	}

	public void createXmlAlbaran(DTODeposito deposito, String oldAlbaran) throws Exception {

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_ALBARANES
				+ "/REC_"
				+ String.valueOf(deposito.Serie)
				+ String.valueOf(deposito.NumeroAlbaran) + ".xml");
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		fileos.write("<Albaran>".getBytes());

		fileos.write("<Empresa>".getBytes());
		fileos.write(_appConfig.getUser().Company.getBytes());
		fileos.write("</Empresa>".getBytes());

		fileos.write("<Usuario>".getBytes());
		fileos.write(_appConfig.getUser().User.getBytes());
		fileos.write("</Usuario>".getBytes());

		fileos.write("<NumeroAlbaran>".getBytes());
		fileos.write(deposito.NumeroAlbaran.getBytes());
		fileos.write("</NumeroAlbaran>".getBytes());

		fileos.write("<Serie>".getBytes());

		if (deposito.Serie.equals(_appConfig.getUser().SerialInvoiceA))
			fileos.write(String.valueOf(Constants.SERIE_A_VALUE).getBytes());
		else
			
			fileos.write(String.valueOf(Constants.SERIE_B_VALUE).getBytes());

		fileos.write("</Serie>".getBytes());

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		fileos.write("<FechaDeposito>".getBytes());
		fileos.write(formatter.format(deposito.FechaDeposito).getBytes());
		fileos.write("</FechaDeposito>".getBytes());

		fileos.write("<CodigoCliente>".getBytes());
		fileos.write(deposito.CodigoCliente.getBytes());
		fileos.write("</CodigoCliente>".getBytes());

		fileos.write("<Nombre>".getBytes());

		if (deposito.Nombre != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Nombre).getBytes());
		fileos.write("</Nombre>".getBytes());

		fileos.write("<NIF>".getBytes());
		if (deposito.NIF != Constants.EMPTY_STRING)
			fileos.write(deposito.NIF.getBytes());
		fileos.write("</NIF>".getBytes());

		fileos.write("<Razon>".getBytes());
		if (deposito.Razon != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Razon).getBytes());
		fileos.write("</Razon>".getBytes());

		fileos.write("<Direccion1>".getBytes());
		if (deposito.Direccion1 != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Direccion1).getBytes());
		;
		fileos.write("</Direccion1>".getBytes());

		fileos.write("<Direccion2>".getBytes());
		if (deposito.Direccion2 != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Direccion2).getBytes());
		;
		fileos.write("</Direccion2>".getBytes());

		fileos.write("<CodigoPostal>".getBytes());
		if (deposito.CodigoPostal != Constants.EMPTY_STRING)
			fileos.write(deposito.CodigoPostal.getBytes());
		;
		fileos.write("</CodigoPostal>".getBytes());

		fileos.write("<Poblacion>".getBytes());
		if (deposito.Poblacion != Constants.EMPTY_STRING)
			fileos.write(deposito.Poblacion.getBytes());
		;
		fileos.write("</Poblacion>".getBytes());

		fileos.write("<Provincia>".getBytes());
		if (deposito.Provincia != Constants.EMPTY_STRING)
			fileos.write(Constants.EMPTY_STRING.getBytes());
		fileos.write("</Provincia>".getBytes());

		fileos.write("<Telefono1>".getBytes());
		if (deposito.Telefono1 != Constants.EMPTY_STRING)
			fileos.write(deposito.Telefono1.getBytes());
		;
		fileos.write("</Telefono1>".getBytes());

		fileos.write("<Telefono2>".getBytes());
		if (deposito.Telefono2 != Constants.EMPTY_STRING)
			fileos.write(deposito.Telefono2.getBytes());
		;
		fileos.write("</Telefono2>".getBytes());

		fileos.write("<Fax>".getBytes());
		if (deposito.Fax != Constants.EMPTY_STRING)
			fileos.write(deposito.Fax.getBytes());
		;
		fileos.write("</Fax>".getBytes());

		fileos.write("<Mail>".getBytes());
		if (deposito.Mail != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Mail).getBytes());
		;
		fileos.write("</Mail>".getBytes());

		fileos.write("<Web>".getBytes());
		if (deposito.Web != Constants.EMPTY_STRING)
			fileos.write(this.getWithCDATA(deposito.Web).getBytes());
		;
		fileos.write("</Web>".getBytes());

		fileos.write("<DescuentoComercial>".getBytes());
		fileos.write(String.valueOf(deposito.Totales.DescuentoProntoPago)
				.getBytes());
		fileos.write("</DescuentoComercial>".getBytes());

		fileos.write("<DescuentoFinanciero>".getBytes());
		fileos.write(String.valueOf(deposito.Totales.DescuentoFinanciero)
				.getBytes());
		fileos.write("</DescuentoFinanciero>".getBytes());

		fileos.write("<FormaPago>".getBytes());

		fileos.write(Constants.EMPTY_STRING.getBytes());
		
		fileos.write("</FormaPago>".getBytes());
		
		fileos.write("<Filiacion>".getBytes());
		fileos.write(deposito.Filiacion.getBytes());
		fileos.write("</Filiacion>".getBytes());

		fileos.write("<TotalAlbaran>".getBytes());
		fileos.write(String.valueOf(deposito.Totales.Total).getBytes());
		fileos.write("</TotalAlbaran>".getBytes());

		fileos.write("<CantidadPagada>".getBytes());
		fileos.write(String.valueOf(deposito.CantidadPagada).getBytes());
		fileos.write("</CantidadPagada>".getBytes());
		
		fileos.write("<NumeroAlbaranRectificado>".getBytes());
		fileos.write(String.valueOf(oldAlbaran).getBytes());
		fileos.write("</NumeroAlbaranRectificado>".getBytes());

		fileos.write("<Lineas>".getBytes());

		for (DTOLineaDeposito linea : deposito.Lineas.values()) {
			try {
				
				
				if (linea.UnidadesFacturadas != 0) {

					fileos.write("<Linea>".getBytes());

					fileos.write("<CodigoArticulo>".getBytes());
					fileos.write(linea.CodigoArticulo.getBytes());
					fileos.write("</CodigoArticulo>".getBytes());

					fileos.write("<Unidades>".getBytes());
					fileos.write(String.valueOf(linea.UnidadesFacturadas)
							.getBytes());
					fileos.write("</Unidades>".getBytes());

					fileos.write("<PVP>".getBytes());
					fileos.write(String.valueOf(linea.PVP).getBytes());
					fileos.write("</PVP>".getBytes());
					
					fileos.write("<VentaDirecta>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</VentaDirecta>".getBytes());
					
					fileos.write("<VentaDeposito>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</VentaDeposito>".getBytes());

					fileos.write("</Linea>".getBytes());
				}

				if (linea.UnidadesAbono != 0) {

					fileos.write("<Linea>".getBytes());

					fileos.write("<CodigoArticulo>".getBytes());
					fileos.write(linea.CodigoArticulo.getBytes());
					fileos.write("</CodigoArticulo>".getBytes());

					fileos.write("<Unidades>".getBytes());
					fileos.write(String.valueOf(linea.UnidadesAbono)
							.getBytes());
					fileos.write("</Unidades>".getBytes());

					fileos.write("<PVP>".getBytes());
					fileos.write(String.valueOf(linea.PVPAbono).getBytes());
					fileos.write("</PVP>".getBytes());
					
					fileos.write("<VentaDirecta>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</VentaDirecta>".getBytes());
					
					fileos.write("<VentaDeposito>".getBytes());
					fileos.write(String.valueOf(0).getBytes());
					fileos.write("</VentaDeposito>".getBytes());


					fileos.write("</Linea>".getBytes());
				}

			} catch (Exception e) {
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			}
		}

		fileos.write("</Lineas>".getBytes());

		fileos.write("</Albaran>".getBytes());
		fileos.close();
		
		this.createXmlDailyStock();
	}

	public void createXmlDeposito(Deposito deposito) throws Exception {

		File newxmlfile = new File(Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ Constants.FOLDER_ROOT
				+ "/"
				+ Constants.FOLDER_DEPOSITOS
				+ "/"
				+ String.valueOf(deposito.IdDeposito) + ".xml");
		try {
			newxmlfile.createNewFile();
		} catch (IOException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(newxmlfile);

		} catch (FileNotFoundException e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		fileos.write("<Deposito>".getBytes());

		fileos.write("<Empresa>".getBytes());
		fileos.write(_appConfig.getUser().Company.getBytes());
		fileos.write("</Empresa>".getBytes());

		fileos.write("<Usuario>".getBytes());
		fileos.write(_appConfig.getUser().User.getBytes());
		fileos.write("</Usuario>".getBytes());

		fileos.write("<CodigoDeposito>".getBytes());
		fileos.write(String.valueOf(deposito.IdDeposito).getBytes());
		fileos.write("</CodigoDeposito>".getBytes());

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

		fileos.write("<FechaDeposito>".getBytes());
		fileos.write(formatter.format(deposito.FechaDeposito).getBytes());
		fileos.write("</FechaDeposito>".getBytes());

		fileos.write("<Ejercicio>".getBytes());
		fileos.write(deposito.Ejercicio.getBytes());
		fileos.write("</Ejercicio>".getBytes());

		fileos.write("<CodigoCliente>".getBytes());
		fileos.write(deposito.Cliente.CodigoCliente.getBytes());
		fileos.write("</CodigoCliente>".getBytes());

		fileos.write("<Nombre>".getBytes());
		fileos.write(this.getWithCDATA(deposito.Nombre).getBytes());
		fileos.write("</Nombre>".getBytes());

		fileos.write("<NIF>".getBytes());
		fileos.write(deposito.NIF.getBytes());
		fileos.write("</NIF>".getBytes());

		fileos.write("<Razon>".getBytes());
		fileos.write(this.getWithCDATA(deposito.Razon).getBytes());
		fileos.write("</Razon>".getBytes());

		fileos.write("<Direccion1>".getBytes());
		fileos.write(this.getWithCDATA(deposito.Direccion1).getBytes());
		fileos.write("</Direccion1>".getBytes());

		fileos.write("<Direccion2>".getBytes());
		fileos.write(this.getWithCDATA(deposito.Direccion2).getBytes());
		fileos.write("</Direccion2>".getBytes());

		fileos.write("<CodigoPostal>".getBytes());
		fileos.write(deposito.CodigoPostal.getBytes());
		fileos.write("</CodigoPostal>".getBytes());

		fileos.write("<Poblacion>".getBytes());
		fileos.write(deposito.Poblacion.getBytes());
		fileos.write("</Poblacion>".getBytes());

		fileos.write("<Provincia>".getBytes());
		fileos.write(deposito.Provincia.getBytes());
		fileos.write("</Provincia>".getBytes());

		fileos.write("<Telefono1>".getBytes());
		fileos.write(deposito.Telefono1.getBytes());
		fileos.write("</Telefono1>".getBytes());

		fileos.write("<Telefono2>".getBytes());
		fileos.write(deposito.Telefono2.getBytes());
		fileos.write("</Telefono2>".getBytes());

		fileos.write("<Fax>".getBytes());
		fileos.write(deposito.Fax.getBytes());
		fileos.write("</Fax>".getBytes());

		fileos.write("<Mail>".getBytes());
		fileos.write(this.getWithCDATA(deposito.Mail).getBytes());
		fileos.write("</Mail>".getBytes());

		fileos.write("<Web>".getBytes());
		fileos.write(this.getWithCDATA(deposito.Web).getBytes());
		fileos.write("</Web>".getBytes());

		if (deposito.NumDoc == null) {
			fileos.write("<NumDoc>".getBytes());
			fileos.write(Constants.EMPTY_STRING.getBytes());
			fileos.write("</NumDoc>".getBytes());
		} else {
			fileos.write("<NumDoc>".getBytes());
			fileos.write(deposito.NumDoc.getBytes());
			fileos.write("</NumDoc>".getBytes());
		}

		fileos.write("<FormaPago>".getBytes());

		if (deposito.formaPago != null)
			fileos.write(deposito.formaPago.CodigoFormaPago.getBytes());
		else
			fileos.write(Constants.EMPTY_STRING.getBytes());

		fileos.write("</FormaPago>".getBytes());

		fileos.write("<Filiacion>".getBytes());
		fileos.write(deposito.Filiacion.getBytes());
		fileos.write("</Filiacion>".getBytes());
		
		fileos.write("<Retirado>".getBytes());
		fileos.write(String.valueOf(deposito.isDepositoRetirado()).getBytes());
		fileos.write("</Retirado>".getBytes());
		
		fileos.write("<MotivoRetirado>".getBytes());
		fileos.write(deposito.MotivoRetirado.getBytes());
		fileos.write("</MotivoRetirado>".getBytes());

		fileos.write("<Lineas>".getBytes());

		for (LineaDeposito linea : deposito.Lineas.values()) {
			try {

				//if (linea.UnidadesRepuestas > 0 || deposito.isDepositoRetirado()) {
				if (linea.UnidadesRepuestas > 0) {	
					if (linea.Articulo != null) {
						
						LineaMovimientos movimientos = linea.getMovimientos();
						
						fileos.write("<Linea>".getBytes());
	
						fileos.write("<CodigoArticulo>".getBytes());
						fileos.write(linea.Articulo.CodigoArticulo.getBytes());
						fileos.write("</CodigoArticulo>".getBytes());
	
						fileos.write("<Unidades>".getBytes());
						fileos.write(String.valueOf(linea.UnidadesIniciales).getBytes());
						fileos.write("</Unidades>".getBytes());
	
						fileos.write("<PVP>".getBytes());
						fileos.write(String.valueOf(linea.PVPAnterior).getBytes());
						fileos.write("</PVP>".getBytes());
						
						fileos.write("<MovimientoStock>".getBytes());
						fileos.write(String.valueOf(movimientos.TotalMovimientoDeposito).getBytes());
						fileos.write("</MovimientoStock>".getBytes());
						
						//fileos.write("<MovimientoStockDefectuoso>".getBytes());
						//fileos.write(String.valueOf(movimientos.TotalDefectuoso).getBytes());
						//fileos.write("</MovimientoStockDefectuoso>".getBytes());
						
						fileos.write("<MovimientoStockAbono>".getBytes());
						fileos.write(String.valueOf(movimientos.TotalAbono).getBytes());
						fileos.write("</MovimientoStockAbono>".getBytes());
						
						//fileos.write("<MovimientoStockAbonoDefectuoso>".getBytes());
						//fileos.write(String.valueOf(movimientos.TotalAbonoDefectuoso).getBytes());
						//fileos.write("</MovimientoStockAbonoDefectuoso>".getBytes());
	
						fileos.write("</Linea>".getBytes());
					}

				}

			} catch (Exception e) {
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
				return;
			}
		}

		fileos.write("</Lineas>".getBytes());
		fileos.write("</Deposito>".getBytes());

		fileos.close();
		
		this.createXmlDailyStock();
	}
	


	private Calendar setWeekStart(Calendar calendar) {
		while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
			calendar.add(Calendar.DATE, -1);
		}
		return calendar;
	}

	private HashMap<String, Reports> getReports(ArrayList<Historico> list) {

		HashMap<String, Reports> result = new HashMap<String, Reports>();

		for (Historico hist : list) {
			for (LineaHistorico linea : hist.Lineas.values()) {
				if (linea.Tipo == Constants.TIPO_LINEA_HISTORICO_POTENCIADAS) {
					if (!result.containsKey(linea.Articulo.CodigoArticulo)) {
						Reports report = new Reports();
						report.Potenciados = linea.Unidades;

						result.put(linea.Articulo.CodigoArticulo, report);
					} else {
						Reports report = (Reports) result
								.get(linea.Articulo.CodigoArticulo);
						report.Potenciados += linea.Unidades;
					}
				}

				if (linea.Tipo == Constants.TIPO_LINEA_HISTORICO_BAJAS) {
					if (!result.containsKey(linea.Articulo.CodigoArticulo)) {
						Reports report = new Reports();
						report.Retirados = linea.Unidades;

						result.put(linea.Articulo.CodigoArticulo, report);
					} else {
						Reports report = (Reports) result
								.get(linea.Articulo.CodigoArticulo);
						report.Retirados += linea.Unidades;
					}
				}
			}
		}

		return result;
	}

	private String getWithCDATA(String text) {
		return "<![CDATA[" + text + "]]>";
	}

	private class Reports {
		public float Potenciados;
		public float Retirados;
	}

}
