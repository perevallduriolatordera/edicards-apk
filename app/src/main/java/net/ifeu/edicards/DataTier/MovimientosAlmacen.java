package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class MovimientosAlmacen extends Persistent implements IPersistable {

	public Articulo Articulo = new Articulo();
	public int Entradas;
	public int Salidas;
	public Date Fecha;
	public int Tipo; // 1-Entradas, 2-Salidas
	public int TipoStock; // 1-Stock, 2-Stock Defectuoso
	
	@Override
	public void InitializePersistance(AppConfig appConfig, Context context) {
		
		super.InitializePersistance(appConfig, context);
	}
	
	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
	}
	
	@Override
	public void save() throws Exception {
				
		ContentValues values = new ContentValues();
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("Entradas", this.Entradas);
		values.put("Salidas", this.Salidas);
		values.put("Tipo", this.Tipo);
		values.put("TipoStock", this.TipoStock);
		
		super.getDatabaseOperations().insert(Constants.TABLE_MOVIMIENTOS_ALMACEN, null , values);
		
	}
	
	public LinkedHashMap<String,ArrayList<MovimientosAlmacen>> getMovimientosByMonth(int year, int month) throws Exception
	{
		LinkedHashMap<String,ArrayList<MovimientosAlmacen>> list = new LinkedHashMap<>();
		
		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_MOVIMIENTOS_ALMACEN,
				Constants.EMPTY_STRING, Constants.EMPTY_STRING, true, 
				" AND  CAST(strftime('%m', Fecha) AS INTEGER) = " + month + " AND CAST(strftime('%Y', Fecha) AS INTEGER)  = " + year,"IdArticulo"); 

		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				do {
					MovimientosAlmacen movimiento = new MovimientosAlmacen();
					
					long id = cursor.getLong(cursor.getColumnIndex("IdArticulo"));
					
					Articulo articulo = new Articulo();
					articulo.InitializePersistance(super.appConfig, super.appConfig);
					
					articulo.setArticuloById(String.valueOf(id));
					
					movimiento.Articulo = articulo;
					movimiento.Entradas = cursor.getInt(cursor.getColumnIndex("Entradas"));
					movimiento.Salidas = cursor.getInt(cursor.getColumnIndex("Salidas"));
					movimiento.Tipo = cursor.getInt(cursor.getColumnIndex("Tipo"));
					movimiento.TipoStock = cursor.getInt(cursor.getColumnIndex("TipoStock"));
					
					SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy"); //please notice the capital M
					
					String fechaString = cursor.getString(cursor.getColumnIndex("Fecha"));
					String fechaToDate = fechaString.substring(8) + "/" + fechaString.substring(5,7)
							+ "/" + fechaString.substring(0,4);
					
					movimiento.Fecha = formatter.parse(fechaToDate);
					
					if (list.containsKey(articulo.CodigoArticulo))
					{
						ArrayList<MovimientosAlmacen> array = list.get(articulo.CodigoArticulo);
						array.add(movimiento);
					}
					else
					{
						ArrayList<MovimientosAlmacen>array = new ArrayList<>();
						array.add(movimiento);
						list.put(articulo.CodigoArticulo, array);
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
	
	
}
