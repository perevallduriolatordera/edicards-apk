package net.ifeu.edicards;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.edicards.DataTier.Reporting.Retirado;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Utils.Screen.ScreenManager;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.widget.LinearLayout;

public class Reporting_Retirados extends Activity {

	AppConfig _appConfig;

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting__potenciados);

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.95f).getWidth();
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        fillRetirados();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_reporting__potenciados, menu);
        return true;
    }
    
    private void fillRetirados()
    {
    	_appConfig = (AppConfig) this.getApplicationContext();
    	Reporting reporting = _appConfig.getWorkingArea().CurrentReporting;

    	LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
		mainLinearLayout.setBackgroundColor(Color.WHITE);
		mainLinearLayout.setPadding(16, 16, 16, 16);

		// Header naranja
		final LinearLayout headerLayout = new LinearLayout(this);
		headerLayout.setBackgroundColor(Color.parseColor("#FF9800"));
		headerLayout.setOrientation(LinearLayout.HORIZONTAL);
		headerLayout.setPadding(16, 12, 16, 12);
		android.widget.LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		headerParams.setMargins(0, 0, 0, 16);
		headerLayout.setLayoutParams(headerParams);

		LabelColor titleLabel = new LabelColor(this, Color.WHITE, Gravity.CENTER);
		titleLabel.setText("ARTÍCULOS RETIRADOS");
		titleLabel.setTextSize(18);
		android.widget.LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		titleLabel.setLayoutParams(titleParams);
		headerLayout.addView(titleLabel);
		mainLinearLayout.addView(headerLayout);

		// Headers de columnas
		final LinearLayout columnHeaderLayout = new LinearLayout(this);
		columnHeaderLayout.setBackgroundColor(Color.parseColor("#F57C00"));
		columnHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
		columnHeaderLayout.setPadding(12, 10, 12, 10);
		android.widget.LinearLayout.LayoutParams columnHeaderParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		columnHeaderParams.setMargins(0, 0, 0, 8);
		columnHeaderLayout.setLayoutParams(columnHeaderParams);

		android.widget.LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		LabelColor colArticulo = new LabelColor(this, Color.WHITE, Gravity.LEFT);
		colArticulo.setText("Artículo");
		colArticulo.setTextSize(13);
		colArticulo.setWidth(500);
		colArticulo.setLayoutParams(colParams);
		columnHeaderLayout.addView(colArticulo);

		LabelColor colUnidades = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
		colUnidades.setText("Unidades");
		colUnidades.setTextSize(13);
		colUnidades.setWidth(200);
		colUnidades.setLayoutParams(colParams);
		columnHeaderLayout.addView(colUnidades);

		mainLinearLayout.addView(columnHeaderLayout);

	 	final LinearLayout layout = new LinearLayout(this);
		layout.removeAllViews();
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setBackgroundColor(Color.WHITE);

    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

		int rowCount = 0;
    	for (Retirado retirado : reporting.Retirados.values())
    	{

    		final LinearLayout layout2 = new LinearLayout(this);
    		layout2.removeAllViews();
    		layout2.setOrientation(LinearLayout.HORIZONTAL);

			// Alternar colores de fila
			if (rowCount % 2 == 0) {
				layout2.setBackgroundColor(Color.parseColor("#F5F5F5"));
			} else {
				layout2.setBackgroundColor(Color.WHITE);
			}
    		layout2.setPadding(12, 8, 12, 8);

			int TEXT_SIZE = 14;

    		LabelColor label = new LabelColor(this,Color.parseColor("#212121"), Gravity.LEFT);
    		label.setRawInputType(InputType.TYPE_CLASS_NUMBER);
    		label.setText(retirado.articulo.Descripcion);
			label.setTextSize(TEXT_SIZE);
    		label.setWidth(500);
    		label.setLayoutParams(params);

        	layout2.addView(label);

        	LabelColor unidades = new LabelColor(this,Color.parseColor("#FF9800"), true, Gravity.RIGHT);
        	unidades.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        	unidades.setText(retirado.unidades + " uds.");
        	unidades.setTextSize(TEXT_SIZE);
        	unidades.setWidth(200);
        	unidades.setLayoutParams(params);

        	layout2.addView(unidades);

        	layout.addView(layout2);
			rowCount++;
    	}

		// Botón Cerrar
		final LinearLayout layout3 = new LinearLayout(this);
		layout3.removeAllViews();

		layout3.setOrientation(LinearLayout.HORIZONTAL);
		layout3.setBackgroundColor(Color.WHITE);
		layout3.setGravity(Gravity.CENTER);
		layout3.setPadding(16, 24, 16, 24);

		android.widget.LinearLayout.LayoutParams layout3Params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		layout3.setLayoutParams(layout3Params);

		ButtonColor closeButton = new ButtonColor(this, Color.parseColor("#D32F2F"));

		closeButton.setText("CERRAR");
		int TEXT_SIZE_BUTTON = 16;
		closeButton.setTextSize(TEXT_SIZE_BUTTON);
		closeButton.setTextColor(Color.WHITE);
		closeButton.setPadding(40, 16, 40, 16);
		closeButton.setGravity(Gravity.CENTER);

		android.widget.LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		buttonParams.setMargins(0, 0, 0, 0);
		closeButton.setLayoutParams(buttonParams);

		closeButton.setOnClickListener(arg0 -> {
			try {
				finish();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		layout3.addView(closeButton);

    	mainLinearLayout.addView(layout);
		mainLinearLayout.addView(layout3);

    }

    
}
