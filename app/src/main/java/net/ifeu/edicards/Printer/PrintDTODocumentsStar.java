package net.ifeu.edicards.Printer;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.R;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.DTOLineaDeposito;
import net.ifeu.edicards.DataTier.Totales;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;

public class PrintDTODocumentsStar extends PrintDocumentsStar implements IPrintDTO {
	
	private void printHeaderData(StarIOPort port, Context context, DTODeposito deposito, AppConfig app, int Tipo) throws StarIOPortException
	{
		byte[] outputByteBuffer = null;
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		String pago = (Tipo == 1) ? "\n" : "Pago: " + deposito.PagoDescripcion + "\n";
		
        outputByteBuffer = ("Fecha: " + formatter.format(deposito.FechaDeposito) + "  Vendedor: " + app.getUser().Name + "\n" +
		            "Codigo Cliente: " + deposito.CodigoCliente + "  Nombre: " + deposito.Nombre + "\n" +
		            "Razón: " + deposito.Razon + "\n" + 
		            "NIF: " + deposito.NIF + "\n" +
		            "Direccion: " + deposito.Direccion1 + "\n" +
		            "Cod. Postal: " + deposito.CodigoPostal + "  Poblacion:  " + deposito.Poblacion +"\n" +
		            "Telefono 1: " + deposito.Telefono1 + "  Telefono 2: " + deposito.Telefono2 + "  Mail:" + deposito.Mail + "\n" +
		            pago).getBytes();
        
        port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
        
	}
	
	private void printHeaderFields(StarIOPort port, Context context, int tipo,AppConfig app) throws StarIOPortException
	{
		  byte[] outputByteBuffer = null;
		
		  outputByteBuffer = ("--------------------------------------------------------------------- \n").getBytes();
		  port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			
		  String total;
		  
		  if (tipo == 2)
			  total = padLeft("TOTAL",10);
		  else
			  total = Constants.EMPTY_STRING;
		  
		  outputByteBuffer = (padRight("COD.",10) + padRight("DESCRIPCION",25) + padLeft("UNID.",10) +  padLeft("PRECIO.",10) + total +"\n").getBytes();
		  port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

	      outputByteBuffer = ("--------------------------------------------------------------------- \n").getBytes();
	      port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
	}
	
	private void printTotals(StarIOPort port, Context context, AppConfig app, int tipo, DTODeposito deposito) throws StarIOPortException
	{
		
		DecimalFormat df = new DecimalFormat("0.00");
		byte[] outputByteBuffer = null;
		
		if (tipo == 1)
		{
			try {
				deposito.CalculateDeposito();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			outputByteBuffer = ("--------------------------------------------------------------------- \n").getBytes();
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
	    	
		}
		else
		{
			try {
				deposito.Calculate();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if ((deposito.Totales.DescuentoFinanciero != 0 || deposito.Totales.DescuentoProntoPago != 0)) {
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBaseSinDte)), 10);

				outputByteBuffer = ("\n" + total + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

				if (deposito.Totales.DescuentoProntoPago != 0) {
					String prontoPago = padLeft(" ", 38)
							+ padRight("DTE. COMERCIAL:", 10)
							+ padLeft(" ", 5)
							+ padRight(
									String.valueOf(df
											.format(deposito.Totales.TotalDescuentoProntoPago)),
									10);
					outputByteBuffer = (prontoPago + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				}

				if (deposito.Totales.DescuentoFinanciero != 0) {
					String financiero = padLeft(" ", 38)
							+ padRight("DTE. FINANCIERO:", 16)
							+ padLeft(" ", 4)
							+ padRight(
									String.valueOf(df
											.format(deposito.Totales.TotalDescuentoFinanciero)),
									10);
					outputByteBuffer = (financiero + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				}
				
				String neto = padLeft(" ", 43)
						+ padRight("NETO:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);
				outputByteBuffer = (neto + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			}

	    	
			if (deposito.Serie.equals(app.getUser().SerialInvoiceA))
			{
				String suma = padLeft(" ", 38) + padRight("SUMA:",10) + padLeft(" ",10) + padRight(String.valueOf(df.format(deposito.Totales.TotalBase)),10);
				outputByteBuffer = ("\n" + suma + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				
				String impuestos = padLeft(" ", 38) + padRight("IMPUESTOS:",10) + padLeft(" ",10) + padRight(String.valueOf(df.format(deposito.Totales.TotalIVA + deposito.Totales.TotalRecargo)),10);
		    	outputByteBuffer = (impuestos + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				
				String total = padLeft(" ", 38) + padRight("TOTAL:",10) + padLeft(" ",10) + padRight(String.valueOf(df.format(deposito.Totales.Total)),10);
		    	outputByteBuffer = (total + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				
			}
			
			if (deposito.Serie.equals(app.getUser().SerialInvoiceB))
			{
				
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);

				outputByteBuffer = (total + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				
				port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
				
				outputByteBuffer = ("\nIVA NO INCLUIDO\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				
				outputByteBuffer = ("Forma de pago: " + deposito.PagoDescripcion+ "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
				if (isMadeInSpain(deposito.CodigoPostal))
					this.PrintBitmapImage(context, PORT, SETTINGS, context.getResources(), R.drawable.madeinspain, 1500);
				
				port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);                 //Set Emphasized Printing OFF (same command as on)
			}
			else
			{
				for (Totales.Base base: deposito.Totales.Bases.values())
				{
					port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
					
					outputByteBuffer = ("Impuestos:\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					outputByteBuffer = ("\u0009" + "Base imp: " + padRight(df.format(base.Base),10) + padRight(df.format(base.IvaPerc) + "%" ,10)  + padRight(df.format(base.Iva) + " Euros" ,16)  + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					outputByteBuffer = ("\u0009" + "Rec eq: " + padRight(" ",10) + padRight(df.format(base.RecargoPerc) + "%",10)  + padRight(df.format(base.Recargo)+ " Euros " ,16)  + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);                 //Set Emphasized Printing OFF (same command as on)
					
					outputByteBuffer = ("\nForma de pago: " + deposito.PagoDescripcion+ "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
										
				}
				
				if (deposito.Pagado)
				{
					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(context, PORT, SETTINGS,
								context.getResources(), R.drawable.madeinspain,
								1500);

					
					outputByteBuffer = ("Conforme   firma cliente:\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					outputByteBuffer = ("\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					outputByteBuffer = ("HE RECIBIDO DE " + deposito.Nombre + " LA CANTIDAD DE " + df.format(deposito.CantidadPagada) + " Euros EN CONCEPTO DEL PAGO DEL ALBARAN " + app.getUser().User + "/" + String.valueOf(deposito.NumeroAlbaran) +"\n\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					if (deposito.CantidadPagada < deposito.Totales.Total)
					{
						outputByteBuffer = ("QUEDA PENDIENTE DE PAGO LA CANTIDAD DE " + df.format(deposito.Totales.Total - deposito.CantidadPagada) + " Euros EN CONCEPTO DEL PAGO DEL ALBARAN " + app.getUser().User + "/" + String.valueOf(deposito.NumeroAlbaran) +"\n").getBytes();
						port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					}
					
					outputByteBuffer = ("\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					outputByteBuffer = ("FIRMA VENDEDOR: " + app.getUser().Name +"\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					this.PrintBitmapSignatureVendor(context, PORT, SETTINGS, 150);
					
					outputByteBuffer = ("\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					outputByteBuffer = ("FIRMA CLIENTE\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					this.PrintBitmapSignature(context, PORT, SETTINGS, 150);
					
				}
				else
				{
					outputByteBuffer = ("\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(context, PORT, SETTINGS, context.getResources(), R.drawable.madeinspain, 1500);
					
					port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
					outputByteBuffer = ("Conforme - Firma Cliente:\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					this.PrintBitmapSignature(context, PORT, SETTINGS, 150);
				}

			}

			if (tipo == 2 &&  app.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards) {

				port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);

				outputByteBuffer = ("MERCANCIA PENDIENTE DE ENVIO" + "\n").getBytes();

				port.writePort(outputByteBuffer, 0,
						outputByteBuffer.length);

				port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);
			}

		}
	}
	
	private void printHeaderDetail(StarIOPort port, Context context, AppConfig app, int tipo, DTODeposito deposito) throws StarIOPortException
	{
		DecimalFormat df = new DecimalFormat("0.00");
		
		byte[] outputByteBuffer = null;
		
		List<DTOLineaDeposito> tempList = new ArrayList<DTOLineaDeposito>();

		for (DTOLineaDeposito linea : deposito.Lineas.values()) {
			tempList.add(linea);
		}

		Collections
				.sort(tempList, new DTOLineaDeposito().new ArticuloComparator());

		
		if (tipo == 2)
			for (DTOLineaDeposito linea: tempList)
			{
				if (linea.UnidadesFacturadas > 0)
				{
					String desc;
					if (linea.Descripcion.length() > 25)
						desc = linea.Descripcion.substring(0,25);
					else
						desc = linea.Descripcion;
					
					outputByteBuffer = (padRight(linea.CodigoArticulo,10) + padRight(desc,25) + padLeft(String.valueOf(linea.UnidadesFacturadas),10) + padLeft(df.format(linea.PVP),10) + padLeft(String.valueOf(df.format(linea.PVP * linea.UnidadesFacturadas)),10) +"\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					
					if (linea.TotalAbono != 0)
					{
						outputByteBuffer = (padRight(linea.CodigoArticulo,10) + padRight(desc,25) + padLeft(String.valueOf(linea.UnidadesAbono),10) + padLeft(df.format(linea.PVPAbono),10) + padLeft(String.valueOf(df.format(linea.TotalAbono)),10) +"\n").getBytes();
						port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					}
				}
				else
				{
					if (linea.TotalAbono != 0)
					{
						
						String desc;
						if (linea.Descripcion.length() > 25)
							desc = linea.Descripcion.substring(0,25);
						else
							desc = linea.Descripcion;
						
						outputByteBuffer = (padRight(linea.CodigoArticulo,10) + padRight(desc,25) + padLeft(String.valueOf(linea.UnidadesAbono),10) + padLeft(df.format(linea.PVPAbono),10) + padLeft(String.valueOf(df.format(linea.TotalAbono)),10) +"\n").getBytes();
						port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					}
				}
			}
		else
		{
			for (DTOLineaDeposito linea: tempList)
			{
				if (linea.UnidadesRepuestas > 0)
				{
					String desc;
					if (linea.Descripcion.length() > 25)
						desc = linea.Descripcion.substring(0,25);
					else
						desc = linea.Descripcion;
					
					outputByteBuffer = (padRight(linea.CodigoArticulo,10) + padRight(desc,25) + padLeft(String.valueOf(linea.UnidadesRepuestas),10) + padLeft(df.format(linea.PVPAnterior),10) + /*padLeft(String.valueOf(df.format(linea.PVP * linea.UnidadesRepuestas)),10) +*/"\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				}
			}
		}
		
		
	}
	
	public boolean printAlbaran(DTODeposito deposito, Context context, AppConfig app, String guid)
	{
		_GUID = guid;
		
		StarIOPort port = null;
		try 
    	{
			port = StarIOPort.getPort(PORT, SETTINGS, 10000, context);
			
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}
			
			byte[] outputByteBuffer = null;

			if (deposito.Serie.equals(app.getUser().SerialInvoiceA))
				printHeader(port,context);
			
			port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
			
			if (deposito.Serie.equals(app.getUser().SerialInvoiceB))
				outputByteBuffer = ("PRESUPUESTO: NUM " + app.getUser().User + "/" + String.valueOf(deposito.NumeroAlbaran) + "\n").getBytes();
			else if(!deposito.Pagado)
				outputByteBuffer = ("ALBARAN: NUM " + app.getUser().User + "/" + String.valueOf(deposito.NumeroAlbaran) + "\n").getBytes();
			else
				outputByteBuffer = ("ALBARAN ENTREGA: NUM " + app.getUser().User + "/" + String.valueOf(deposito.NumeroAlbaran) + "\n").getBytes();
			
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
	            
            port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);                 //Set Emphasized Printing OFF (same command as on)
	            
        	this.printHeaderData(port, context, deposito, app, 2);
	        
	        port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
	        
	        printHeaderFields(port,context,2, app);
	        
	        printHeaderDetail(port,context, app, 2, deposito);
	        
	        printTotals(port,context,app,2,deposito);
			
			port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);                 //Set Emphasized Printing OFF (same command as on)
			
			closePage(port);
			
    	}
		catch (StarIOPortException e)
    	{
    		/*Builder dialog = new AlertDialog.Builder(context);
    		dialog.setNegativeButton("Ok", null);
    		AlertDialog alert = dialog.create();
    		alert.setTitle("Error de impresión");
    		alert.setMessage("Error al conectar con la impresora");
    		alert.show();*/
			//app.getMessageBox().Show("Estado de impresión", "Error al conectar con la impresora", context, MessageBoxType.Error);
    		return false;
    	}
		
		finally
		{
			if(port != null)
			{
				try {
				StarIOPort.releasePort(port);
				} 
				catch (StarIOPortException e) {}
			}
		}
		
		return true;
	}
	
	public boolean printDeposito(DTODeposito deposito, Context context, AppConfig app, String guid)
	{
		
		_GUID = guid;
		
		StarIOPort port = null;
		try 
    	{
 			port = StarIOPort.getPort(PORT, SETTINGS, 10000, context);
			
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}
			
			byte[] outputByteBuffer = null;

			printHeader(port,context);
			
			port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
			
			outputByteBuffer = ("DEPOSITO: NUM " + app.getUser().User + "/" + String.valueOf(deposito.IdDeposito) + "\n").getBytes();
			
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
	            
            port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);                 //Set Emphasized Printing OFF (same command as on)
	            
        	this.printHeaderData(port, context, deposito, app, 1);
	        
	        port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
	        
	        printHeaderFields(port,context,1, app);
			
	        printHeaderDetail(port,context, app, 1, deposito);
	        
	        printTotals(port,context,app,1,deposito);
			
			outputByteBuffer = ("\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n").getBytes();
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			
			//outputByteBuffer = ("\nFABRICADO EN ESPAÑA.  Todo el proceso de diseño y fabricación de nuestros productos ha sido realizado íntegramente en España. Consumir productos españoles asegura el futuro, la economía del país y el afianzamiento del empleo.\n\n").getBytes();
			//port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			
			if (isMadeInSpain(deposito.CodigoPostal))
				this.PrintBitmapImage(context, PORT, SETTINGS, context.getResources(), R.drawable.madeinspain, 1500);
			
			this.PrintBitmapImage(context, PORT, SETTINGS, context.getResources(), R.drawable.contract, 1500);
		
			
			port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);                 //Set Emphasized Printing ON
			outputByteBuffer = ("Conforme - Firma Cliente:\n").getBytes();
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			
			this.PrintBitmapSignature(context, PORT, SETTINGS, 150);
			
			port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);                 //Set Emphasized Printing OFF (same command as on)
			
			closePage(port);
			
    	}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        		catch (StarIOPortException e)
    	{
    		/*Builder dialog = new AlertDialog.Builder(context);
    		dialog.setNegativeButton("Ok", null);
    		AlertDialog alert = dialog.create();
    		alert.setTitle("Error de impresión");
    		alert.setMessage("Error al conectar con la impresora");
    		alert.show();*/
    		
			//app.getMessageBox().Show("Estado de impresión", "Error al conectar con la impresora", context, MessageBoxType.Error);
    		return false;
    	}
		
		finally
		{
			if(port != null)
			{
				try {
				StarIOPort.releasePort(port);
				} 
				catch (StarIOPortException e) {}
			}
		}
		
		return true;
	}
	
	public void PrintBitmapSignature(Context context, String portName, String portSettings, int maxWidth)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_FIRMAS + "/" + "C_" + _GUID + ".png", options);
		
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);
		
		StarIOPort port = null;
		try 
    	{
			
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}

			byte[] command = starbitmap.getImageEscPosDataForPrinting();
			port.writePort(command, 0, command.length);
			
			try
			{
				Thread.sleep(3000);
			}
			catch(InterruptedException e) {}			
		}
    	catch (StarIOPortException e)
    	{
    		/*Builder dialog = new AlertDialog.Builder(context);
    		dialog.setNegativeButton("Ok", null);
    		AlertDialog alert = dialog.create();
    		alert.setTitle("Error");
    		alert.setMessage("Error al conectar a la impresora");
    		alert.show();*/

    	}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {}
			}
		}
	}
	
	public void PrintBitmapSignatureVendor(Context context, String portName, String portSettings, int maxWidth)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_FIRMAS + "/" + "V_" + _GUID + ".png", options);
		
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);
		
		StarIOPort port = null;
		try 
    	{
		
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}

			byte[] command = starbitmap.getImageEscPosDataForPrinting();
			port.writePort(command, 0, command.length);
			 
			try
			{
				Thread.sleep(3000);
			}
			catch(InterruptedException e) {}			
		}
    	catch (StarIOPortException e)
    	{
    		/*Builder dialog = new AlertDialog.Builder(context);
    		dialog.setNegativeButton("Ok", null);
    		AlertDialog alert = dialog.create();
    		alert.setTitle("Error");
    		alert.setMessage("Error al conectar a la impresora");
    		alert.show();*/
    		//app.getMessageBox().Show("Estado de impresión", "Error al conectar con la impresora", context, MessageBoxType.Error);
    	}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {}
			}
		}
	}
	
	public void PrintBitmapImage(Context context, String portName, String portSettings,  Resources res, int source, int maxWidth)
	{
		Bitmap bm = BitmapFactory.decodeResource(res, source);
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);
		
		StarIOPort port = null;
		try 
    	{
		
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
		
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}

			byte[] command = starbitmap.getImageEscPosDataForPrinting();
			port.writePort(command, 0, command.length);
		        
			try
			{
				Thread.sleep(3000);
			}
			catch(InterruptedException e) {}			
		}
    	catch (StarIOPortException e)
    	{
    		/*Builder dialog = new AlertDialog.Builder(context);
    		dialog.setNegativeButton("Ok", null);
    		AlertDialog alert = dialog.create();
    		alert.setTitle("Error");
    		alert.setMessage("Error al conectar a la impresora");
    		alert.show();*/
    		//app.getMessageBox().Show("Estado de impresión", "Error al conectar con la impresora", context, MessageBoxType.Error);
    	}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {}
			}
		}
	}
	
}
