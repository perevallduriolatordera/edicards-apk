package net.ifeu.library.Utils.MessageBox;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.view.ContextThemeWrapper;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.R;
import net.ifeu.library.Controls.TextBoxColor;

public class MessageBox {
	
	private boolean _result;
	private String _value;

	@SuppressWarnings("deprecation")
	public void Show(String title, String text, Context context, MessageBoxType type)
	{
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme)).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(text);
		alertDialog.setButton("OK", (dialog, arg1) -> {dialog.dismiss();});
		
		switch (type)
		{
		case Error:
			{
				alertDialog.setIcon(R.drawable.ic_error);
				break;
			}
		case Information:
			{
				alertDialog.setIcon(R.drawable.ic_information);
				break;
			}
		case Ok:
			{
				alertDialog.setIcon(R.drawable.ic_ok);
				break;
			}
		}
		
		alertDialog.show();

	}
	
	public boolean ShowWithResult(String title, String text, Context context, MessageBoxType type)
	{
		 final Handler handler = new Handler() {
		        @Override
		        public void handleMessage(Message mesg) {
		            throw new RuntimeException("@Custom");
		        }  
		    };
		    
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme));
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setPositiveButton("Sí", (dialog, arg1) -> {
            setResult(true);
            // Cerrar el diálogo ANTES de enviar el mensaje para evitar errores de transición
            dialog.dismiss();
            handler.sendMessage(handler.obtainMessage());
        });

        alertDialog.setNegativeButton("No", (dialog, arg1) -> {
			setResult(false);
			// Cerrar el diálogo ANTES de enviar el mensaje para evitar errores de transición
			dialog.dismiss();
			handler.sendMessage(handler.obtainMessage());
	});
        
        alertDialog.setCancelable(false);
        
        alertDialog.create().show();
        
        // loop till a runtime exception is triggered.
        try { Looper.loop(); }
        catch(RuntimeException e2) {}

        return _result;
	}
	
	public void ShowModalWithOk(String title, String text, Context context, MessageBoxType type)
	{
		 final Handler handler = new Handler() {
		        @Override
		        public void handleMessage(Message mesg) {
		            throw new RuntimeException("@Custom");
		        }  
		    };
		    
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme));
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setPositiveButton("Aceptar", (dialog, arg1) -> {
            // Cerrar el diálogo ANTES de enviar el mensaje para evitar errores de transición
            dialog.dismiss();
            handler.sendMessage(handler.obtainMessage());
        });
        
        alertDialog.setCancelable(false);
        
        switch (type)
		{
		case Error:
			{
				alertDialog.setIcon(R.drawable.ic_error);
				break;
			}
		case Information:
			{
				alertDialog.setIcon(R.drawable.ic_information);
				break;
			}
		case Ok:
			{
				alertDialog.setIcon(R.drawable.ic_ok);
				break;
			}
		}
        
        alertDialog.create().show();
        
        // loop till a runtime exception is triggered.
        try { Looper.loop(); }
        catch(RuntimeException e2) {}
	}

	private void setResult(boolean value)
	{
		_result = value;
	}

	public String InputBox(String title, String text, Context context)
	{
		final TextBoxColor textBox = new TextBoxColor(context, Color.BLACK);
		 final Handler handler = new Handler() {
		        @Override
		        public void handleMessage(Message mesg) {
					throw new RuntimeException("@Custom");
		        } 
		    };
		    
		new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme))
	    .setTitle(title)
	    .setMessage(text)
	    .setView(textBox)
	    .setCancelable(false)
	    .setPositiveButton("Aceptar", (dialog, whichButton) -> {

			Editable value = textBox.getText();
			_value = value.toString();

			// Cerrar el diálogo ANTES de enviar el mensaje para evitar errores de transición
			dialog.dismiss();
			handler.sendMessage(handler.obtainMessage());
		}).setNegativeButton("Cancelar", (dialog, whichButton) -> {

			_value = ConstantsTypes.EMPTY_STRING;

			// Cerrar el diálogo ANTES de enviar el mensaje para evitar errores de transición
			dialog.dismiss();
			handler.sendMessage(handler.obtainMessage());
		}).create().show();
		
		 // loop till a runtime exception is triggered.
        try { Looper.loop(); }
        catch(RuntimeException e2) {}
		
		return _value;
	}
}
