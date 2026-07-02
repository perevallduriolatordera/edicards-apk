package net.ifeu.library.Controls;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;
import net.ifeu.edicards.R;

public class ButtonColor extends Button {

	public ButtonColor(Context context, AttributeSet attrs) {
		super(context,attrs);
	}

	public ButtonColor(Context context, int color) {
		super(context);
		this.setBackgroundColor(color);
		this.setTextColor(0xFFFFFFFF);
		this.setFreezesText(true);
		this.setTextSize(16);
		this.setPadding(32, 24, 32, 24);
		this.setAllCaps(false);
	}

	public ButtonColor(Context context, int color, Drawable drawable) {

		super(context);

		this.setBackgroundColor(color);
		this.setTextColor(0xFFFFFFFF);
		this.setFreezesText(true);
		this.setTextSize(16);
		this.setPadding(32, 24, 32, 24);
		this.setAllCaps(false);

		drawable.setBounds(0, 0, 40, 40);
		this.setCompoundDrawables(drawable, null, null, null);
		this.setCompoundDrawablePadding(16);
	}

	public void changeAspect(Context context, int color, Drawable drawable) {
		this.setBackgroundColor(color);
		this.setTextColor(0xFFFFFFFF);
		this.setFreezesText(true);
		this.setTextSize(16);
		this.setPadding(32, 24, 32, 24);
		this.setAllCaps(false);

		drawable.setBounds(0, 0, 40, 40);
		this.setCompoundDrawables(drawable, null, null, null);
		this.setCompoundDrawablePadding(16);
	}
}
