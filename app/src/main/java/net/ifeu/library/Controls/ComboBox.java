package net.ifeu.library.Controls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ComboBox extends LinearLayout {
	
	private final int TEXT_SIZE = 16;

   private AutoCompleteTextView _text;
   private ImageButton _button;
   private LabelColor _label;
   private Map<String, IComboBoxChangeEvent> _observers = new HashMap<String, IComboBoxChangeEvent>();
   
   public ComboBox(Context context, int width) {
       super(context);
       this.createChildControls(context,width);
   }

   public ComboBox(Context context, AttributeSet attrs, int width) {
       super(context, attrs);
       this.createChildControls(context,width);
   }
   
   public void addObserver(String id, IComboBoxChangeEvent observer) {
	   this._observers.put(id, observer);
   }

   private void createChildControls(Context context, int width) {
	   
	   final ComboBox that = this;
       this.setOrientation(HORIZONTAL);
       this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                       LayoutParams.WRAP_CONTENT));

       RelativeLayout relativeLayout = new RelativeLayout(context);
       
       _text = new AutoCompleteTextView(context);
       _text.setFocusable(false);
       _text.setWidth(width);
       _text.setSingleLine();
     /*  _text.setInputType(InputType.TYPE_CLASS_TEXT
                       | InputType.TYPE_TEXT_VARIATION_NORMAL
                       | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                       | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
                       | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
       _text.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);*/
       _text.setTextSize(TEXT_SIZE);
       _text.setOnItemClickListener(new OnItemClickListener() {
           public void onItemClick(AdapterView<?> listView, View view,
                       int position, long id) {
               // 
        	   
           	_label.setText(listView.getItemAtPosition(position).toString());
           	 _text.setText("");

           	for (Map.Entry<String, IComboBoxChangeEvent> item : that._observers.entrySet()) {
            	 item.getValue().callback(item.getKey(), _label.getText().toString());
            }
           	 //for (IComboBoxChangeEvent observer : that._observers) {
           	//	 observer.callback(_label.getText().toString());
           	// }
           	
           }
       });
       
       relativeLayout.addView(_text);
       
       _label = new LabelColor(context,Color.WHITE);
       _label.setTextSize(TEXT_SIZE);
       
       RelativeLayout.LayoutParams labelLayoutParams = 
    		    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
       
       labelLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
       _label.setLayoutParams(labelLayoutParams);
       relativeLayout.addView(_label,labelLayoutParams);
       
       //this.addView(_text, new LayoutParams(LayoutParams.WRAP_CONTENT,
       //                LayoutParams.WRAP_CONTENT, 1));
       
       this.addView(relativeLayout, new LayoutParams(LayoutParams.WRAP_CONTENT,
    	                       LayoutParams.WRAP_CONTENT, 1));

       _button = new ImageButton(context);
       _button.setImageResource(android.R.drawable.arrow_down_float);
       _button.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View v) {
                   _text.showDropDown();
               }
       });
       this.addView(_button, new LayoutParams(LayoutParams.WRAP_CONTENT,
                       LayoutParams.WRAP_CONTENT));
   }

   /**
    * Sets the source for DDLB suggestions.
    * Cursor MUST be managed by supplier!!
    * @param source Source of suggestions.
    * @param column Which column from source to show.
    */
   public void setSuggestionSource(Cursor source, String column) {
       String[] from = new String[] { column };
       int[] to = new int[] { android.R.id.text1 };
       @SuppressWarnings("deprecation")
	SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this.getContext(),
                       android.R.layout.simple_dropdown_item_1line, source, from, to);
       // this is to ensure that when suggestion is selected
       // it provides the value to the textbox
       cursorAdapter.setStringConversionColumn(source.getColumnIndex(column));
       _text.setAdapter(cursorAdapter);
       
   }
   
   public void setSuggestionArray(List<String> list)
   {
	 
       ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(),android.R.layout.simple_dropdown_item_1line, list);
      // ComboAdapter adapter = new ComboAdapter(this.getContext(),android.R.layout.simple_dropdown_item_1line, (ArrayList<String>) list);
       _text.setAdapter(adapter);
   }
   

   /**
    * Gets the text in the combo box.
    *
    * @return Text.
    */
   public String getText() {
       //return _text.getText().toString();
       return _label.getText().toString();
   }

   /**
    * Sets the text in combo box.
    */
   public void setText(String text) {
       //_text.setText(text);
	   _label.setText(text);
   }

}

