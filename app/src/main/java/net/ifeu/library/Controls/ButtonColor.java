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
		this.setTextColor(context.getResources().getColor(R.color.Black));
		this.setFreezesText(true);
		this.setTextSize(14);
		this.setBackgroundResource(R.drawable.background_button);
	}

	public ButtonColor(Context context, int color, Drawable drawable) {

		super(context);

		this.setBackgroundColor(color);
		this.setTextColor(context.getResources().getColor(R.color.Black));
		this.setFreezesText(true);
		this.setTextSize(14);
		this.setBackgroundResource(R.drawable.background_button);

		drawable.setBounds(0, 0, 30, 30);
		this.setCompoundDrawables(drawable, null, null, null);
	}

	public void changeAspect(Context context, int color, Drawable drawable) {
		this.setBackgroundColor(color);
		this.setTextColor(context.getResources().getColor(R.color.Black));
		this.setFreezesText(true);
		this.setTextSize(14);
		this.setBackgroundResource(R.drawable.background_button);

		drawable.setBounds(0, 0, 30, 30);
		this.setCompoundDrawables(drawable, null, null, null);
	}
}
