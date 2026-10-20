package net.ifeu.library.Controls;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.ifeu.library.Utils.Screen.ScreenManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComboBox extends LinearLayout {

    private AutoCompleteTextView _text;
   private ImageButton _button;
   private LabelColor _label;
   private TextView _topLabel;
   final private Map<String, IComboBoxChangeEvent> _observers = new HashMap<>();

   public ComboBox(Context context) {
       super(context);
       this.createChildControls(context);
   }

   public ComboBox(Context context, AttributeSet attrs) {
       super(context, attrs);
       this.createChildControls(context);
   }

   public void addObserver(String id, IComboBoxChangeEvent observer) {
	   this._observers.put(id, observer);
   }

   private void createChildControls(Context context) {

	   final ComboBox that = this;
       // Cambiar a VERTICAL para tener label arriba
       this.setOrientation(VERTICAL);
       LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
               LayoutParams.WRAP_CONTENT);

       this.setLayoutParams(params);

       // Label superior (inicialmente vacío, se puede setear desde fuera)
       _topLabel = new TextView(context);
       _topLabel.setTextSize(8);
       _topLabel.setTextColor(Color.parseColor("#757575")); // Gris como los otros labels
       _topLabel.setGravity(Gravity.CENTER);
       _topLabel.setVisibility(GONE); // Oculto por defecto
       LayoutParams topLabelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       topLabelParams.setMargins(0, 0, 0, 2);
       _topLabel.setLayoutParams(topLabelParams);
       this.addView(_topLabel);

       // Contenedor horizontal para el campo y el botón (más compacto)
       LinearLayout horizontalContainer = new LinearLayout(context);
       horizontalContainer.setOrientation(HORIZONTAL);
       horizontalContainer.setGravity(Gravity.CENTER_VERTICAL);
       LayoutParams horizontalParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       horizontalContainer.setLayoutParams(horizontalParams);

       RelativeLayout relativeLayout = new RelativeLayout(context);

       _text = new AutoCompleteTextView(context);
       WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

       _text.setWidth(ScreenManager.getScreenSizeByPercentage(windowManager, 0.2f).getWidth());
       _text.setFocusable(false);
       _text.setSingleLine();
       int TEXT_SIZE = 16;
       _text.setTextSize(TEXT_SIZE);
       _text.setOnItemClickListener((listView, view, position, id) -> {
           _label.setText(listView.getItemAtPosition(position).toString());
            _text.setText("");

           for (Map.Entry<String, IComboBoxChangeEvent> item : that._observers.entrySet()) {
             item.getValue().onChange(item.getKey(), _label.getText().toString());
        }
       });

       relativeLayout.addView(_text);

       _label = new LabelColor(context, Color.parseColor("#212121"));
       _label.setTextSize(TEXT_SIZE);

       RelativeLayout.LayoutParams labelLayoutParams =
    		    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

       labelLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
       _label.setLayoutParams(labelLayoutParams);
       relativeLayout.addView(_label,labelLayoutParams);

       horizontalContainer.addView(relativeLayout, new LayoutParams(LayoutParams.WRAP_CONTENT,
    	                       LayoutParams.WRAP_CONTENT, 1));

       _button = new ImageButton(context);
       _button.setImageResource(android.R.drawable.arrow_down_float);
       _button.setOnClickListener(v -> _text.showDropDown());
       // Reducir padding del botón para que esté más pegado
       _button.setPadding(2, 2, 2, 2);
       LayoutParams buttonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       buttonParams.setMargins(2, 0, 0, 0);
       horizontalContainer.addView(_button, buttonParams);

       this.addView(horizontalContainer);
   }

   /**
    * Sets the source for DDLB suggestions.
    * Cursor MUST be managed by supplier!!
    * @param source Source of suggestions.
    * @param column Which column from source to show.
    */

   public void setSuggestionArray(List<String> list)
   {
	 
       ArrayAdapter<String> adapter = new ArrayAdapter<>(this.getContext(),android.R.layout.simple_dropdown_item_1line, list);
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

   /**
    * Sets the top label text.
    */
   public void setTopLabelText(String text) {
       _topLabel.setText(text);
       _topLabel.setVisibility(text != null && !text.isEmpty() ? VISIBLE : GONE);
   }

   /**
    * Gets the top label text.
    */
   public String getTopLabelText() {
       return _topLabel.getText().toString();
   }

}

