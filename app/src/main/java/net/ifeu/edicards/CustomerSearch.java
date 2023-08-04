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
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.LineaDeposito;

import java.util.ArrayList;
import java.util.List;


public class CustomerSearch extends Fragment implements TextWatcher {

	 AutoCompleteTextView myAutoComplete;
	 AppConfig app;
	 Cliente cliente;    
	 List<String> item = new ArrayList<>();
	    		
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
    	cliente = Factory.build(Cliente.class, app);

        try {
   			item = cliente.getClientesNameByFilter("", Constants.CUSTOMER_FILTER_NAME, false);
   			
   		} catch (Exception e) {
            throw new RuntimeException(e);
   		}
   	
    	myAutoComplete = (AutoCompleteTextView) getActivity().findViewById(R.id.myautocomplete);
        
        myAutoComplete.addTextChangedListener(this);
        
        myAutoComplete.setThreshold(1);
        myAutoComplete.setCompletionHint("Pulse el cliente que desea visualizar");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_dropdown_item_1line, item);
                
        myAutoComplete.setAdapter(adapter);
        
        myAutoComplete.setOnItemClickListener((listView, view, position, id) -> {
            String customer =  listView.getItemAtPosition(position).toString();

            try {
                cliente.setClienteByName(customer);
            } catch (Exception e) {
                throw new RuntimeException(e);
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
            ((TextView) getActivity().findViewById(R.id.lblDescuento1Cliente)).setText(cliente.DescuentoProntoPago + " %");
            ((TextView) getActivity().findViewById(R.id.lblDescuento2Cliente)).setText(cliente.DescuentoFinanciero + " %");
            ((TextView) getActivity().findViewById(R.id.lblFormaPagoCliente)).setText(cliente.formaPago.Descripcion);

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
