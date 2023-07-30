package net.ifeu.library.Controls;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

public class TextBoxColor extends EditText {

	public TextBoxColor(Context context, int color) {
		super(context);
		this.setTextColor(color);
		// TODO Auto-generated constructor stub
		
		this.setOnFocusChangeListener((v, hasFocus) -> {
			if (hasFocus)
				((EditText)v).selectAll();
		});
	}
	
	public TextBoxColor(Context context, int color,int gravity) {
		this(context,color);
		this.setGravity(gravity);
	}

}
