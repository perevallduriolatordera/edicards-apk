package net.ifeu.library.Controls;

import android.content.Context;
import android.widget.Button;
import net.ifeu.edicards.R;

public class ButtonColor extends Button {

	public ButtonColor(Context context, int color) {
		super(context);
		this.setBackgroundColor(color);
		this.setFreezesText(true);
		this.setBackgroundResource(R.drawable.background_button);
		// TODO Auto-generated constructor stub
	}

}
