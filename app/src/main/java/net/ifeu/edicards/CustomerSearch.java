package net.ifeu.edicards;

import java.util.ArrayList;
import java.util.List;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Cliente;
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


public class CustomerSearch extends Fragment implements TextWatcher {

	 AutoCompleteTextView myAutoComplete;
	 
	 AppConfig app;
	 Cliente cliente;    
	 List<String> item = new ArrayList<String>(); 
	    		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
        
    	return inflater.inflate(R.layout.activity_customer_search, container,false);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
    	super.onActivityCreated(savedInstanceState);
    	
    	app = (AppConfig) getActivity().getApplicationContext();  
    	cliente = new Cliente();
        try {
   			cliente.InitializePersistance(app, getActivity());
   			item = cliente.getClientesNameByFilter("", Constants.CUSTOMER_FILTER_NAME, false);
   			
   		} catch (Exception e) {
   			// TODO Auto-generated catch block
   			app.getErrorTrace().Send(app.getUser().User, e);
   		}
   	
    	myAutoComplete = (AutoCompleteTextView) getActivity().findViewById(R.id.myautocomplete);
        
        myAutoComplete.addTextChangedListener(this);
        
        myAutoComplete.setThreshold(1);
        myAutoComplete.setCompletionHint("Pulse el cliente que desea visualizar");
        Log.i("Test","listener");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_dropdown_item_1line, item);
                
        myAutoComplete.setAdapter(adapter);
        
        myAutoComplete.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View view,
                        int position, long id) {
                // Get the cursor, positioned to the corresponding row in the
                // result set
            	String customer =  listView.getItemAtPosition(position).toString();
            	
            	try {
					cliente.setClienteByName(customer);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					app.getErrorTrace().Send(app.getUser().User, e);
				}
 
                // Update the parent class's TextView
            	((TextView) getActivity().findViewById(R.id.lblNombreCliente)).setText(customer);
	            ((TextView) getActivity().findViewById(R.id.lblNIFCliente)).setText(cliente.NIF);
                ((TextView) getActivity().findViewById(R.id.lblIdCliente)).setText(cliente.CodigoCliente);
                ((TextView) getActivity().findViewById(R.id.lblDireccionCliente)).setText(cliente.Direccion1);
                ((TextView) getActivity().findViewById(R.id.lblLocalidadCliente)).setText(cliente.Poblacion);
                ((TextView) getActivity().findViewById(R.id.lblProvinciaCliente)).setText(cliente.Provincia);
                ((TextView) getActivity().findViewById(R.id.lblCodigoPostalCliente)).setText(cliente.CodigoPostal);
                ((TextView) getActivity().findViewById(R.id.lblTelefono1Cliente)).setText(cliente.Telefono1);
                ((TextView) getActivity().findViewById(R.id.lblTelefono2Cliente)).setText(cliente.Telefono2);
                ((TextView) getActivity().findViewById(R.id.lblFaxCliente)).setText(cliente.Fax);
                ((TextView) getActivity().findViewById(R.id.lblTipoIVACliente)).setText(cliente.Filiacion);
                ((TextView) getActivity().findViewById(R.id.lblDescuento1Cliente)).setText(String.valueOf(cliente.DescuentoProntoPago) + " %");
                ((TextView) getActivity().findViewById(R.id.lblDescuento2Cliente)).setText(String.valueOf(cliente.DescuentoFinanciero) + " %");
                ((TextView) getActivity().findViewById(R.id.lblFormaPagoCliente)).setText(cliente.formaPago.Descripcion);
        	           	
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
