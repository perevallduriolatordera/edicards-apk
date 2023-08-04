package net.ifeu.edicards.DataTier;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

public class Articulo extends Persistent implements IPersistable {

	public long IdArticulo;
	public String CodigoArticulo;
	public String Descripcion;
	public double PVP;
	public String Familia;
	public String FamiliaCorta;
	public double Descuento1;
	public double Descuento2;
	public String TipoIVA;
	public int Stock;
	public int StockDefectuoso;
	public int Tipo; // 1 - Venta, 2 - Compra;
	public int Salidas;
	public int Entradas;
	public boolean Activo;
	public int MovimientoStock;
	public int MovimientoStockDefectuosas;
	public boolean StockPropio;

	@Override
	public void save() throws Exception {
		
		ContentValues values = new ContentValues();
		values.put("Activo", this.Activo ? "1" : "2");
		values.put("CodigoArticulo", this.CodigoArticulo);
		values.put("Descripcion", this.Descripcion);
		values.put("PVP", this.PVP);
		values.put("Familia", this.Familia);
		values.put("FamiliaCorta", this.FamiliaCorta);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		values.put("TipoIVA", this.TipoIVA);
		values.put("Stock", this.Stock);
		values.put("StockDefectuoso",this.StockDefectuoso);
		values.put("Tipo", this.Tipo);
		values.put("StockPropio", this.StockPropio ? 1 : 0 );
		
		this.IdArticulo = super.getDatabaseOperations().insert(Constants.TABLE_ARTICULOS, null , values);
		
	}
	
	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();
		
		values.put("IdArticulo", this.IdArticulo);
		values.put("Activo", this.Activo ? "1" : "2");
		values.put("CodigoArticulo", this.CodigoArticulo);
		values.put("Descripcion", this.Descripcion);
		values.put("PVP", this.PVP);
		values.put("Familia", this.Familia);
		values.put("FamiliaCorta", this.FamiliaCorta);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		values.put("TipoIVA", this.TipoIVA);
		values.put("Stock", this.Stock);
		values.put("StockDefectuoso", this.StockDefectuoso);
		values.put("Tipo", this.Tipo);
		values.put("StockPropio", this.StockPropio ? 1 : 0);

		String[] whereArgs = { String.valueOf(this.IdArticulo) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_ARTICULOS, values, "IdArticulo = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_ARTICULOS);
	}
	
	public LinkedList<String> getArticulosByFilter(String text,boolean onlyStartsWith)
	{		
			 return super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_ARTICULOS, "Descripcion","Descripcion",text,onlyStartsWith, "AND Activo = 1 AND Tipo = '1'","Familia");
	}
	
	public LinkedHashMap<String,Articulo> getAllArticulos(int tipo) throws Exception
	{
			LinkedHashMap<String,Articulo> list = new LinkedHashMap<>();
			
			Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_ARTICULOS, Constants.EMPTY_STRING, Constants.EMPTY_STRING, true, "AND Activo = 1 AND Tipo = '" + tipo + "'","Familia");
			
			if (cursor != null)
			{
				cursor.moveToFirst();
				
				if (cursor.getCount() > 0)
				{
					do {
						Articulo articulo = Factory.build(Articulo.class, appConfig);
						
						this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");

						if (this.Activo) {
							articulo.CodigoArticulo = cursor.getString(cursor.getColumnIndex("CodigoArticulo"));
							articulo.Descripcion=cursor.getString(cursor.getColumnIndex("Descripcion"));
							articulo.Descuento1 =  cursor.getDouble(cursor.getColumnIndex("Descuento1"));
							articulo.Descuento2 = cursor.getDouble(cursor.getColumnIndex("Descuento2"));
							articulo.Familia = cursor.getString(cursor.getColumnIndex("Familia"));
							articulo.FamiliaCorta = cursor.getString(cursor.getColumnIndex("FamiliaCorta"));
							articulo.IdArticulo=cursor.getInt(cursor.getColumnIndex("IdArticulo"));
							articulo.PVP=cursor.getDouble(cursor.getColumnIndex("PVP")); 
							articulo.Stock=cursor.getInt(cursor.getColumnIndex("Stock"));
							articulo.StockDefectuoso=cursor.getInt(cursor.getColumnIndex("StockDefectuoso")); 
							articulo.Tipo=cursor.getInt(cursor.getColumnIndex("Tipo"));
							articulo.TipoIVA = cursor.getString(cursor.getColumnIndex("TipoIVA"));
							articulo.StockPropio = cursor.getInt(cursor.getColumnIndex("StockPropio")) == 1;

							list.put(articulo.CodigoArticulo.trim(),articulo);
						}
						
					} while (cursor.moveToNext());
					
					cursor.close();
					return list;
					}
				else
					cursor.close();
			}
		return list;

	}
	
	
	public boolean setArticuloByName(String name) throws Exception
	{
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_ARTICULOS, "Descripcion", name, true);
		
		if (cursor != null)
		{
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			
			if (this.Activo) {
				this.IdArticulo = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdArticulo")));
				this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
				this.CodigoArticulo = cursor.getString(cursor.getColumnIndex("CodigoArticulo"));
				this.Descripcion= cursor.getString(cursor.getColumnIndex("Descripcion"));
				this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
				this.Familia= cursor.getString(cursor.getColumnIndex("Familia"));
				this.FamiliaCorta= cursor.getString(cursor.getColumnIndex("FamiliaCorta"));
				this.Stock= Integer.parseInt(cursor.getString(cursor.getColumnIndex("Stock")));
				this.StockDefectuoso= Integer.parseInt(cursor.getString(cursor.getColumnIndex("StockDefectuoso")));
				this.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
				this.TipoIVA = cursor.getString(cursor.getColumnIndex("TipoIVA"));
				this.StockPropio = cursor.getInt(cursor.getColumnIndex("StockPropio")) == 1;
			
			    cursor.close();
			    
				return true; 
			}
			else {
				cursor.close();
				return false;
			}
			
		}
		
		return false ; 
	}
	
	public boolean setArticuloById(String IdArticulo) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_ARTICULOS, "IdArticulo", IdArticulo,false);
		
		if (cursor != null)
		{
			
			this.IdArticulo = Long.parseLong(IdArticulo);
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			this.CodigoArticulo = cursor.getString(cursor.getColumnIndex("CodigoArticulo"));
			this.Descripcion= cursor.getString(cursor.getColumnIndex("Descripcion"));
			this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
			this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
			this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
			this.Familia= cursor.getString(cursor.getColumnIndex("Familia"));
			this.FamiliaCorta= cursor.getString(cursor.getColumnIndex("FamiliaCorta"));
			this.Stock= Integer.parseInt(cursor.getString(cursor.getColumnIndex("Stock")));
			this.StockDefectuoso= Integer.parseInt(cursor.getString(cursor.getColumnIndex("StockDefectuoso")));
			this.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
			this.TipoIVA = cursor.getString(cursor.getColumnIndex("TipoIVA"));
			this.StockPropio = cursor.getInt(cursor.getColumnIndex("StockPropio")) == 1;
			
		    cursor.close();
			return true;
		}
		
		return false ;
	}
	
	public boolean setArticuloByCodigo(String codigoArticulo) throws Exception
	{
		if (codigoArticulo == null)
			return false;
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_ARTICULOS, "CodigoArticulo", codigoArticulo,true);
		
		if (cursor != null)
		{
			
			this.IdArticulo = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdArticulo")));
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			this.CodigoArticulo = codigoArticulo;
			this.Descripcion= cursor.getString(cursor.getColumnIndex("Descripcion"));
			this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
			this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
			this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
			this.Familia= cursor.getString(cursor.getColumnIndex("Familia"));
			this.FamiliaCorta= cursor.getString(cursor.getColumnIndex("FamiliaCorta"));
			this.Stock= Integer.parseInt(cursor.getString(cursor.getColumnIndex("Stock")));
			this.StockDefectuoso= Integer.parseInt(cursor.getString(cursor.getColumnIndex("StockDefectuoso")));
			this.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
			this.TipoIVA = cursor.getString(cursor.getColumnIndex("TipoIVA"));
			this.StockPropio = cursor.getInt(cursor.getColumnIndex("StockPropio")) == 1;
			
		    cursor.close();
			return true;
		}
		
		return false ;
	}

}
