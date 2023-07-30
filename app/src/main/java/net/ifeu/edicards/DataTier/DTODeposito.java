package net.ifeu.edicards.DataTier;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Totales.Base;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class DTODeposito {

	@Expose
	public long IdCliente;
	@Expose
	public boolean Activo;
	@Expose
	public String CodigoCliente;
	@Expose
	public String Nombre;
	@Expose
	public String NIF;
	@Expose
	public String Razon;
	@Expose
	public String Direccion1;
	@Expose
	public String Direccion2;
	@Expose
	public String CodigoPostal;
	@Expose
	public String Poblacion;
	@Expose
	public String Provincia;
	@Expose
	public String Telefono1;
	@Expose
	public String Telefono2;
	@Expose
	public String Fax;
	@Expose
	public String Clave;
	@Expose
	public String Mail;
	@Expose
	public String Web;
	@Expose
	public double Descuento1;
	@Expose
	public double Descuento2;
	@Expose
	public double DescuentoProntoPago;
	@Expose
	public String Filiacion;
	@Expose
	public String CodigoTarifa;
	@Expose
	public Long IdDeposito;
	@Expose
	public Date FechaDeposito;
	@Expose
	public String Ejercicio;
	@Expose
	public String Serie;
	@Expose
	public String NumeroAlbaran;
	@Expose
	public float DescuentoComercial;
	@Expose
	public float DescuentoFinanciero;
	@Expose
	public String PagoDescripcion;
	@Expose
	public boolean Pagado;
	@Expose
	public double CantidadPagada;
	@Expose
	public String NumDoc;
	@Expose
	public String TipoDeposito;
	@Expose
	public LinkedHashMap<String, DTOLineaDeposito> Lineas = new LinkedHashMap<>();

	public Totales Totales = new Totales();
	public Totales TotalesDeposito = new Totales();

	private AppConfig _appConfig;
	private Context _context;

	public DTODeposito() {
	}

	public DTODeposito(AppConfig app, Context context) {
		_appConfig = app;
		_context = context;
	}

	public String serialize() {

		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.create();

		return gson.toJson(this);

	}

	public void deserialize(String serializacion) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

		DTODeposito dto = gson.fromJson(serializacion, DTODeposito.class);
		assign(dto);
	}
	
	public void cancel() {
		
		LinkedHashMap<String, DTOLineaDeposito> newLineas = new LinkedHashMap<>();
		
		for (DTOLineaDeposito linea : this.Lineas.values()) {
			
			if (linea.UnidadesFacturadas != 0 || linea.UnidadesAbono != 0) {
				DTOLineaDeposito dtoLinea = new DTOLineaDeposito();

				dtoLinea.CodigoArticulo = linea.CodigoArticulo;
				dtoLinea.Descripcion = linea.Descripcion;
				dtoLinea.Familia = linea.Familia;
				dtoLinea.Descuento1 = linea.Descuento1;
				dtoLinea.Descuento2 = linea.Descuento2;
				dtoLinea.IdArticulo = linea.IdArticulo;
				dtoLinea.IdDeposito = linea.IdDeposito;
				dtoLinea.IdLineaDeposito = linea.IdLineaDeposito;
				dtoLinea.IsNew = linea.IsNew;
				dtoLinea.IsVentaDirecta = linea.IsVentaDirecta;
				dtoLinea.PVP = linea.PVP;
				dtoLinea.PVPAbono = linea.PVPAbono;
				dtoLinea.PVPAnterior = linea.PVPAnterior;
				dtoLinea.PVPInicial = linea.PVPInicial;
				dtoLinea.StockInicial = linea.StockInicial;
				dtoLinea.TotalAbono = -linea.TotalAbono;
				dtoLinea.UnidadesAnterior = linea.UnidadesAnterior;
				dtoLinea.UnidadesDefectuosas = linea.UnidadesDefectuosas;
				dtoLinea.UnidadesDevueltas = linea.UnidadesDevueltas;
				dtoLinea.UnidadesFacturadas = -linea.UnidadesFacturadas ;
				dtoLinea.UnidadesIniciales = linea.UnidadesIniciales;
				dtoLinea.UnidadesInicialesFijas = linea.UnidadesInicialesFijas;
				dtoLinea.UnidadesRepuestas = -linea.UnidadesRepuestas;
				dtoLinea.DefectuosasAbono = linea.DefectuosasAbono;
				dtoLinea.UnidadesAbono = linea.UnidadesAbono;
	
				newLineas.put(String.valueOf(dtoLinea.CodigoArticulo), dtoLinea);
			}
		}
		
		this.Lineas.clear();
		this.Lineas = newLineas;
	}

	private void assign(DTODeposito dto) {

		this.Activo = dto.Activo;
		this.CantidadPagada = dto.CantidadPagada;
		this.Clave = dto.Clave;
		this.CodigoCliente = dto.CodigoCliente;
		this.CodigoPostal = dto.CodigoPostal;
		this.CodigoTarifa = dto.CodigoTarifa;
		this.Descuento1 = dto.Descuento1;
		this.Descuento2 = dto.Descuento2;
		this.DescuentoComercial = dto.DescuentoComercial;
		this.DescuentoFinanciero = dto.DescuentoFinanciero;
		this.DescuentoProntoPago = dto.DescuentoProntoPago;
		this.Direccion1 = dto.Direccion1;
		this.Direccion2 = dto.Direccion2;
		this.Ejercicio = dto.Ejercicio;
		this.Fax = dto.Fax;
		this.FechaDeposito = dto.FechaDeposito;
		this.Filiacion = dto.Filiacion;
		this.IdCliente = dto.IdCliente;
		this.IdDeposito = dto.IdDeposito;
		this.Mail = dto.Mail;
		this.NIF = dto.NIF;
		this.Nombre = dto.Nombre;
		this.NumDoc = dto.NumDoc;
		this.NumeroAlbaran = dto.NumeroAlbaran;
		this.Pagado = dto.Pagado;
		this.PagoDescripcion = dto.PagoDescripcion;
		this.Poblacion = dto.Poblacion;
		this.Provincia = dto.Provincia;
		this.Razon = dto.Razon;
		this.Serie = dto.Serie;
		this.Telefono1 = dto.Telefono1;
		this.Telefono2 = dto.Telefono2;
		this.TipoDeposito = dto.TipoDeposito;
		this.Web = dto.Web;

		this.Lineas.clear();
		for (DTOLineaDeposito linea : dto.Lineas.values()) {
			DTOLineaDeposito dtoLinea = new DTOLineaDeposito();

			dtoLinea.CodigoArticulo = linea.CodigoArticulo;
			dtoLinea.Descripcion = linea.Descripcion;
			dtoLinea.Familia = linea.Familia;
			dtoLinea.Descuento1 = linea.Descuento1;
			dtoLinea.Descuento2 = linea.Descuento2;
			dtoLinea.IdArticulo = linea.IdArticulo;
			dtoLinea.IdDeposito = linea.IdDeposito;
			dtoLinea.IdLineaDeposito = linea.IdLineaDeposito;
			dtoLinea.IsNew = linea.IsNew;
			dtoLinea.IsVentaDirecta = linea.IsVentaDirecta;
			dtoLinea.PVP = linea.PVP;
			dtoLinea.PVPAbono = linea.PVPAbono;
			dtoLinea.PVPAnterior = linea.PVPAnterior;
			dtoLinea.PVPInicial = linea.PVPInicial;
			dtoLinea.StockInicial = linea.StockInicial;
			dtoLinea.TotalAbono = linea.TotalAbono;
			dtoLinea.UnidadesAnterior = linea.UnidadesAnterior;
			dtoLinea.UnidadesDefectuosas = linea.UnidadesDefectuosas;
			dtoLinea.UnidadesDevueltas = linea.UnidadesDevueltas;
			dtoLinea.UnidadesFacturadas = linea.UnidadesFacturadas;
			dtoLinea.UnidadesIniciales = linea.UnidadesIniciales;
			dtoLinea.UnidadesInicialesFijas = linea.UnidadesInicialesFijas;
			dtoLinea.UnidadesRepuestas = linea.UnidadesRepuestas;
			dtoLinea.DefectuosasAbono = linea.DefectuosasAbono;
			dtoLinea.UnidadesAbono = linea.UnidadesAbono;

			this.Lineas.put(String.valueOf(dtoLinea.CodigoArticulo), dtoLinea);

		}
	}
	
	public Deposito getDeposito() throws Exception{
		
		Deposito dep = new Deposito();
		dep.InitializePersistance(_appConfig, _context);

		dep.Activo = this.Activo;
		dep.CantidadPagada = this.CantidadPagada;
		dep.Clave = this.Clave;
		dep.CodigoCliente = this.CodigoCliente;
		dep.CodigoPostal = this.CodigoPostal;
		dep.CodigoTarifa = this.CodigoTarifa;
		dep.Descuento1 = this.Descuento1;
		dep.Descuento2 = this.Descuento2;
		dep.DescuentoComercial = this.DescuentoComercial;
		dep.DescuentoFinanciero = this.DescuentoFinanciero;
		dep.DescuentoProntoPago = this.DescuentoProntoPago;
		dep.Direccion1 = this.Direccion1;
		dep.Direccion2 = this.Direccion2;
		dep.Ejercicio = this.Ejercicio;
		dep.Fax = this.Fax;
		dep.FechaDeposito = this.FechaDeposito;
		dep.Filiacion = this.Filiacion;
		dep.IdCliente = this.IdCliente;
		dep.IdDeposito = this.IdDeposito;
		dep.Mail = this.Mail;
		dep.NIF = this.NIF;
		dep.Nombre = this.Nombre;
		dep.NumDoc = this.NumDoc;
		dep.NumeroAlbaran = this.NumeroAlbaran;
		dep.Pagado = this.Pagado;
		dep.PagoDescripcion = this.PagoDescripcion;
		dep.Poblacion = this.Poblacion;
		dep.Provincia = this.Provincia;
		dep.Razon = this.Razon;
		dep.Serie = this.Serie;
		dep.Telefono1 = this.Telefono1;
		dep.Telefono2 = this.Telefono2;
		dep.TipoDeposito = this.TipoDeposito;
		dep.Web = this.Web;

		dep.Lineas.clear();
		for (DTOLineaDeposito linea : this.Lineas.values()) {
			LineaDeposito lineaDeposito = new LineaDeposito();

			lineaDeposito.Articulo.CodigoArticulo = linea.CodigoArticulo;
			lineaDeposito.Articulo.Descripcion = linea.Descripcion;
			lineaDeposito.Articulo.Familia = linea.Familia;
			lineaDeposito.Descuento1 = linea.Descuento1;
			lineaDeposito.Descuento2 = linea.Descuento2;
			lineaDeposito.IdArticulo = linea.IdArticulo;
			lineaDeposito.IdDeposito = linea.IdDeposito;
			lineaDeposito.IdLineaDeposito = linea.IdLineaDeposito;
			lineaDeposito.IsNew = linea.IsNew;
			lineaDeposito.IsVentaDirecta = linea.IsVentaDirecta;
			lineaDeposito.PVP = linea.PVP;
			lineaDeposito.PVPAbono = linea.PVPAbono;
			lineaDeposito.PVPAnterior = linea.PVPAnterior;
			lineaDeposito.PVPInicial = linea.PVPInicial;
			lineaDeposito.StockInicial = linea.StockInicial;
			lineaDeposito.TotalAbono = linea.TotalAbono;
			lineaDeposito.UnidadesAnterior = linea.UnidadesAnterior;
			lineaDeposito.UnidadesDefectuosas = linea.UnidadesDefectuosas;
			lineaDeposito.UnidadesDevueltas = linea.UnidadesDevueltas;
			lineaDeposito.UnidadesFacturadas = linea.UnidadesFacturadas;
			lineaDeposito.UnidadesIniciales = linea.UnidadesIniciales;
			lineaDeposito.UnidadesInicialesFijas = linea.UnidadesInicialesFijas;
			lineaDeposito.UnidadesRepuestas = linea.UnidadesRepuestas;
			lineaDeposito.DefectuosasAbono = linea.DefectuosasAbono;
			lineaDeposito.UnidadesAbono = linea.UnidadesAbono;

			dep.Lineas.put(String.valueOf(lineaDeposito.Articulo.CodigoArticulo), lineaDeposito);
		}
		
		return dep;
	}


	public void Calculate() throws Exception {
		Totales = new Totales();

		for (DTOLineaDeposito linea : this.Lineas.values()) {

			if (linea.UnidadesFacturadas != 0 || linea.UnidadesAbono != 0) {
				// Calculamos el precio bruto inicial
				double bruto = round(linea.UnidadesFacturadas * linea.PVP
						+ linea.TotalAbono,3);
				
				double neto;

				if (linea.Descuento1 != 0)
					neto = round(bruto - (bruto * (linea.Descuento1 / 100)),3);
				else
					neto = bruto;

				@SuppressWarnings("unused")
				double dte = round(bruto - neto,3);

				TipoIVA iva = new TipoIVA();
				iva.InitializePersistance(_appConfig, _context);

				Cliente cliente = new Cliente();
				cliente.InitializePersistance(_appConfig, _context);
				cliente.setClienteByCodigo(this.CodigoCliente);

				Articulo articulo = new Articulo();
				articulo.InitializePersistance(_appConfig, _context);
				articulo.setArticuloByCodigo(linea.CodigoArticulo);


				if (iva.setTipoIVAByClienteArticulo(cliente,
						articulo)
						&& _appConfig.getUser().SerialInvoiceB
								.equals(this.Serie)) {

				
					double baseSinDte = neto;
					double base = bruto; //round(neto - descuento1 - descuento2,3);
					base = round(base, 3);
					baseSinDte = round(baseSinDte, 3);

					Base bases;

					if (this.Totales.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.Totales.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.BaseSinDte = round(bases.BaseSinDte + baseSinDte,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
					} else {
						bases = this.Totales.new Base();
						bases.Base = base;
						bases.BaseSinDte = baseSinDte;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.Totales.Bases.put(String.valueOf(iva.Impuesto),
								bases);
					}


				} else {
					double base = bruto;
					double baseSinDte = neto;
				
					Base bases;

					if (this.Totales.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.Totales.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.BaseSinDte = round(bases.BaseSinDte + baseSinDte,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
					} else {
						bases = this.Totales.new Base();
						bases.Base = base;
						bases.BaseSinDte = baseSinDte;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.Totales.Bases.put(String.valueOf(iva.Impuesto),
								bases);
					}

				}
			}
		}
		
		this.CalculateTotals(this.Totales);

	}

	public void CalculateDeposito() throws Exception {
		TotalesDeposito = new Totales();

		for (DTOLineaDeposito linea : this.Lineas.values()) {

			if (linea.UnidadesRepuestas > 0) {
				// Calculamos el precio bruto inicial
				double bruto = round(linea.UnidadesRepuestas * linea.PVP,3);

				double neto;

				if (linea.Descuento1 != 0)
					neto = round(bruto - (bruto * (linea.Descuento1 / 100)),3);
				else
					neto = bruto;

				@SuppressWarnings("unused")
				double dte = round(bruto - neto,3);

				TipoIVA iva = new TipoIVA();
				iva.InitializePersistance(_appConfig, _context);

				Cliente cliente = new Cliente();
				cliente.InitializePersistance(_appConfig, _context);
				cliente.setClienteByCodigo(this.CodigoCliente);

				Articulo articulo = new Articulo();
				articulo.InitializePersistance(_appConfig, _context);
				articulo.setArticuloByCodigo(linea.CodigoArticulo);

				if (iva.setTipoIVAByClienteArticulo(cliente,
						articulo)
						&& _appConfig.getUser().SerialInvoiceB
								.equals(this.Serie)) {

					double base = bruto;

					Base bases;

					if (this.TotalesDeposito.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.TotalesDeposito.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
	
					} else {
						bases = this.TotalesDeposito.new Base();
						bases.Base = base;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.TotalesDeposito.Bases.put(
								String.valueOf(iva.Impuesto), bases);
					}

				} else {
					double base = bruto;

					Base bases;

					if (this.TotalesDeposito.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.TotalesDeposito.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
					} else {
						bases = this.TotalesDeposito.new Base();
						bases.Base = base;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.TotalesDeposito.Bases.put(
								String.valueOf(iva.Impuesto), bases);
					}

				}
			}
		}
		
		this.CalculateTotals(this.TotalesDeposito);

	}
	
	private void CalculateTotals(Totales totales)
	{
		for (Entry<String, Base> entry : totales.Bases.entrySet())
		{
			Base base = entry.getValue();
			
			double descuento1 = round(base.Base * (this.DescuentoComercial / 100),5);
			double descuento2 = round((base.Base - descuento1)
					* (this.DescuentoFinanciero / 100),5);
			double baseTipo = round(base.Base - descuento1 - descuento2,5);
			double totalIVA = round((baseTipo * base.IvaPerc) / 100,5);
			double totalRecargo = round((baseTipo * base.RecargoPerc) / 100,5);
			double total = round(baseTipo,2) + round(totalIVA,2) + round(totalRecargo,2);
			
			base.Descuento = round(descuento1 + descuento2 , 5);
			base.Iva = round(totalIVA,5);
			base.Recargo = round(totalRecargo,5);
			base.Total = round(total,2);
			
			totales.TotalBaseSinDte = round(totales.TotalBaseSinDte + base.BaseSinDte,5);
			totales.TotalBase = round(totales.TotalBase + baseTipo,5);
			totales.DescuentoFinanciero = this.DescuentoFinanciero;
			totales.DescuentoProntoPago = this.DescuentoComercial;
			totales.TotalDescuentoProntoPago = round(totales.TotalDescuentoProntoPago + descuento1,5);
			totales.TotalDescuentoFinanciero = round(totales.TotalDescuentoFinanciero + descuento2,5);
			
			totales.TotalIVA = round(totales.TotalIVA + totalIVA,5);
			totales.TotalRecargo = round(totales.TotalRecargo + totalRecargo,5);
			totales.Total = round(totales.Total + total,2);
			
		}
	}

	public boolean isAlbaran() {

		for (DTOLineaDeposito linea : Lineas.values()) {
			if (linea.UnidadesFacturadas > 0 || linea.UnidadesAbono > 0)
				return true;
		}

		return false;
	}

	public boolean isDeposito() {

		for (DTOLineaDeposito linea : Lineas.values()) {
			if (linea.UnidadesRepuestas > 0)
				return true;
		}

		return false;
	}

	public boolean isDepositoUpdated() {

		for (DTOLineaDeposito linea : Lineas.values()) {
			if ((linea.UnidadesInicialesFijas != linea.UnidadesRepuestas)
					|| (linea.PVPAnterior != linea.PVPInicial)) {
				return true;
			}
		}

		return false;

	}

	private double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

}
