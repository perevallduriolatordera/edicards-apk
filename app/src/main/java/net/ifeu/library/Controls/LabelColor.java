package net.ifeu.library.Controls;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

public class LabelColor extends TextView {

	public LabelColor(Context context, int color) {
		super(context, null);
		this.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		this.setTextColor(color);

		// TODO Auto-generated constructor stub

	}
	
	public LabelColor(Context context, int color, boolean bold) {
		this(context,color);
		// TODO Auto-generated constructor stub
		if (bold) {
			this.setTypeface(Typeface.DEFAULT_BOLD);
			this.setTextColor(color);
		}

	}
	
	public LabelColor(Context context, int color, int gravity)
	{
		this(context,color);
		this.setGravity(gravity);
	}
	
	public LabelColor(Context context, int color, boolean bold, int gravity) {		
		this(context,color, bold);
		// TODO Auto-generated constructor stub
		if (bold)
			this.setGravity(gravity);
		
	}
	
	
}
