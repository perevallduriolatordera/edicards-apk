package net.ifeu.edicards.Cache;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import android.content.Context;
import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.FormaPago;

public class CacheData {
	
	LinkedHashMap<String, Articulo> _articulos = null;
	LinkedHashMap<String, Articulo> _gastos = null;
	HashMap<String, FormaPago> _formasPago = null;
	AppConfig _appConfig;
	Context _context;
	
	public CacheData(AppConfig appConfig, Context context) {
		this._appConfig = appConfig;
		this._context = context;
	}
	
	public LinkedHashMap<String, Articulo> getAllArticulos() throws Exception {

		if (_articulos == null) {
			
			Articulo articulo = new Articulo();

			articulo.InitializePersistance(_appConfig, this._context);

			this._articulos = articulo.getAllArticulos(1); 
			return this._articulos;
	
		} else {
			return _articulos;
		}
		
	}
	
	public LinkedHashMap<String, Articulo> getAllGastos() throws Exception {

		if (_gastos == null) {
			
			Articulo articulo = new Articulo();

			try {
				articulo.InitializePersistance(_appConfig, this._context);
			} catch (Exception e) {
				throw new RuntimeException(e);			}

			this._gastos = articulo.getAllArticulos(2);
			return this._gastos;
	
		} else {
			return _gastos;
		}
		
	}
	
	public HashMap<String, FormaPago> getAllFormasPago() {

		if (_formasPago == null) {
			
			FormaPago formaPago = new FormaPago();

			try {
				formaPago.InitializePersistance(_appConfig, this._context);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			this._formasPago = formaPago.getAllFormasPago();
			return this._formasPago;
	
		} else {
			return _formasPago;
		}
		
	}
	
	public LinkedList<String> getAllFormasPagoList() throws Exception {
		
		LinkedList<String> newList = new LinkedList<String>();
		HashMap<String, FormaPago> formasPago = this.getAllFormasPago();

		for (FormaPago fp : formasPago.values()) {
			String item = fp.Descripcion;
			newList.add(item);
		}
		
		Collections.sort(newList, new Comparator<String>() {
		     @Override
		     public int compare(String o1, String o2) {
		         return Collator.getInstance().compare(o1, o2);
		     }
		 });

		return newList;
	}

	public void invalidate() {
		_articulos = null;
		_gastos = null;
		_formasPago = null;
	}
	
}
