package net.ifeu.edicards;

import java.util.ArrayList;
import java.util.List;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class ArticleSearch extends Fragment implements TextWatcher {

	 AutoCompleteTextView myAutoComplete;
	    
	 AppConfig app;
	 Articulo articulo;    
	 List<String> item = new ArrayList<String>(); 
	    
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
   			item = articulo.getArticulosByFilter("", Constants.ARTICLE_FILTER_NAME, false);
   			
   			
   		} catch (Exception e) {
   			// TODO Auto-generated catch block
   			app.getErrorTrace().Send(app.getUser().User, e);
   		}
        
    	Log.i("Test","createView");
    	myAutoComplete = (AutoCompleteTextView) getActivity().findViewById(R.id.myautocompleteArticle);
    	Log.i("Test","getView");
        
        myAutoComplete.addTextChangedListener(this);
        
        myAutoComplete.setThreshold(1);
        myAutoComplete.setCompletionHint("Pulse el artículo que desea visualizar");
        Log.i("Test","listener");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_dropdown_item_1line, item);
                
        myAutoComplete.setAdapter(adapter);
        
        myAutoComplete.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View view,
                        int position, long id) {
                // Get the cursor, positioned to the corresponding row in the
                // result set
            	String article =  listView.getItemAtPosition(position).toString();
            	
            	try {
					articulo.setArticuloByName(article);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					app.getErrorTrace().Send(app.getUser().User, e);
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
            	
            	
            }
        });
 
        Log.i("Test","set adapter");
    
    }
    
    @Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

    public void afterTextChanged(Editable arg0) {
     // TODO Auto-generated method stub

    }

    public void beforeTextChanged(CharSequence s, int start, int count,
      int after) {
     // TODO Auto-generated method stub

    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
     // TODO Auto-generated method stub

    }
    
    public void onFilterComplete(int Count)
    {
    	
    }
    
}
