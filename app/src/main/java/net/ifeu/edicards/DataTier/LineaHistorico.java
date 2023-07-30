package net.ifeu.edicards.DataTier;

import java.util.LinkedHashMap;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class LineaHistorico extends Persistent implements IPersistable {

	public long IdLineaHistorico;
	public long IdHistorico;
	public long IdArticulo;
	public Historico Historico = new Historico();
	public Articulo Articulo = new Articulo();
	public int Unidades;
	public float PVP;
	public int Tipo; 
	public int MovimientoStock;
	public int MovimientoStockDefectuosas;
	
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
		values.put("IdHistorico", this.Historico.IdHistorico);
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("Unidades", this.Unidades);
		values.put("MovimientoStock", this.MovimientoStock);
		values.put("MovimientoStockDefectuosas", this.MovimientoStockDefectuosas);
		values.put("Tipo", this.Tipo);
		values.put("PVP", this.PVP);
		
		try {
			this.IdLineaHistorico = super.getDatabaseOperations().insert(Constants.TABLE_LINEAS_HISTORICO, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();
		
		values.put("IdHistorico", this.IdHistorico);
		values.put("IdArticulo", this.IdArticulo);
		values.put("Unidades", this.Unidades);
		values.put("MovimientoStock", this.MovimientoStock);
		values.put("MovimientoStockDefectuosas", this.MovimientoStockDefectuosas);
		values.put("Tipo", this.Tipo);
		values.put("PVP", this.PVP);

		
		String[] whereArgs = { String.valueOf(this.IdLineaHistorico) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_LINEAS_HISTORICO, values, "IdLineaHistorico = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_LINEAS_HISTORICO);
	}
	
	
	public LinkedHashMap<String, LineaHistorico> getLineasHistoricoByHistorico(Historico historico) throws Exception
	{
			LinkedHashMap<String,LineaHistorico> list = new LinkedHashMap<>();
			
			Cursor cursor = super.getDatabaseOperations().getRecordsFromFieldNumeric(Constants.TABLE_LINEAS_HISTORICO, "IdHistorico", String.valueOf(historico.IdHistorico), Constants.EMPTY_STRING, null);
			
			if (cursor != null)
			{
				cursor.moveToFirst();
				
				if (cursor.getCount() > 0)
				{
					do {
						LineaHistorico linea = new LineaHistorico();
						
						linea.Historico = historico; 
						linea.IdLineaHistorico = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdLineaHistorico")));
						linea.IdArticulo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdArticulo")));
						linea.IdHistorico = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdHistorico")));
						linea.Unidades = (int) Float.parseFloat(cursor.getString(cursor.getColumnIndex("Unidades")));
						linea.MovimientoStock = Integer.parseInt(cursor.getString(cursor.getColumnIndex("MovimientoStock")));
						linea.MovimientoStockDefectuosas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("MovimientoStockDefectuosas")));
						linea.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
						
						if (!cursor.isNull(cursor.getColumnIndex("PVP")))
							linea.PVP = Float.parseFloat(cursor.getString(cursor.getColumnIndex("PVP")));
						
						Articulo articulo = new Articulo();
						
						try {
							articulo.InitializePersistance(super.appConfig, super.context);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
						
						if (articulo.setArticuloById(cursor.getString(cursor.getColumnIndex("IdArticulo"))))
						{
							linea.Articulo = articulo;
						}
						
						list.put(String.valueOf(linea.IdLineaHistorico), linea);
						
					} while (cursor.moveToNext());
					
					cursor.close();
					return list;
					}
				else
				{
					cursor.close();
					return list;
				}
					
			}
			else
				return list;

	}
	
}
