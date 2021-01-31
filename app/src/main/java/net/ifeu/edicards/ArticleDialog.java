package net.ifeu.edicards;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import net.ifeu.edicards.DataTier.Articulo;


public class ArticleDialog extends Activity {

	AppConfig _appConfig;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_articulo_dialog);
        
        _appConfig = (AppConfig) this.getApplicationContext();
        Articulo articulo = _appConfig.getWorkingArea().CurrentArticulo;
        
        ((TextView) this.findViewById(R.id.lblIdArticulo)).setText(articulo.CodigoArticulo);
        ((TextView) this.findViewById(R.id.lblDescripcionArticulo)).setText(articulo.Descripcion);
        ((TextView) this.findViewById(R.id.lblPrecioArticulo)).setText(String.valueOf(articulo.PVP) + " €");
        ((TextView) this.findViewById(R.id.lblFamiliaArticulo)).setText(articulo.Familia);
        ((TextView) this.findViewById(R.id.lblTipoIVAArticulo)).setText(articulo.TipoIVA);
        ((TextView) this.findViewById(R.id.lblDescuento1Articulo)).setText(String.valueOf(articulo.Descuento1) + " %");
        ((TextView) this.findViewById(R.id.lblStock)).setText(String.valueOf(articulo.Stock)+ " unidades");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_articulo_dialog, menu);
        return true;
    }
   
    
}
