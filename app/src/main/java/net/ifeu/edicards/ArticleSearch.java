package net.ifeu.edicards;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Articulo;

import java.util.ArrayList;
import java.util.List;

public class ArticleSearch extends Fragment implements TextWatcher {

	 AutoCompleteTextView myAutoComplete;
	    
	 AppConfig app;
	 Articulo articulo;    
	 List<String> item = new ArrayList<>();
	    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
        
    	return inflater.inflate(R.layout.activity_article_search, container,false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
    	super.onActivityCreated(savedInstanceState);
    	
    	app = (AppConfig) getActivity().getApplicationContext();  
    	articulo = new Articulo();
        try {
   			articulo.InitializePersistance(app, getActivity());
   			item = articulo.getArticulosByFilter("", false);
   			
   			
   		} catch (Exception e) {
   			// TODO Auto-generated catch block
            throw new RuntimeException(e);
   		}

    	myAutoComplete = (AutoCompleteTextView) getActivity().findViewById(R.id.myautocompleteArticle);
        myAutoComplete.addTextChangedListener(this);
        
        myAutoComplete.setThreshold(1);
        myAutoComplete.setCompletionHint("Pulse el artículo que desea visualizar");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_dropdown_item_1line, item);
                
        myAutoComplete.setAdapter(adapter);
        
        myAutoComplete.setOnItemClickListener((listView, view, position, id) -> {
            String article =  listView.getItemAtPosition(position).toString();

            try {
                articulo.setArticuloByName(article);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Update the parent class's TextView
            ((TextView) getActivity().findViewById(R.id.lblDescripcionArticulo)).setText(article);
            ((TextView) getActivity().findViewById(R.id.lblIdArticulo)).setText(articulo.CodigoArticulo);
            ((TextView) getActivity().findViewById(R.id.lblPrecioArticulo)).setText(articulo.PVP + " €");
            ((TextView) getActivity().findViewById(R.id.lblFamiliaArticulo)).setText(articulo.FamiliaCorta);
            ((TextView) getActivity().findViewById(R.id.lblTipoIVAArticulo)).setText(articulo.TipoIVA);
            ((TextView) getActivity().findViewById(R.id.lblDescuento1Articulo)).setText(articulo.Descuento1 +" %");
            ((TextView) getActivity().findViewById(R.id.lblDescuento2Articulo)).setText(articulo.Descuento2 + " %");

            myAutoComplete.setText("");


        });
    }

    public void afterTextChanged(Editable arg0) {
     

    }

    public void beforeTextChanged(CharSequence s, int start, int count,
      int after) {
     

    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
     

    }
}
