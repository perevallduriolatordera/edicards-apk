package net.ifeu.edicards;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.LinearLayout;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DepositView extends Activity {

	AppConfig _appConfig;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_deposit_view);
		this.setTitle("Depósito");

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		params.height = LayoutParams.FILL_PARENT;
		android.view.Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		params.width = (int) (size.x * 0.95); // 95% del ancho de pantalla
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		try {
			fillDeposito();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void fillDeposito()  {
		_appConfig = (AppConfig) this.getApplicationContext();

		LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
		mainLinearLayout.removeAllViews();
		mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
		mainLinearLayout.setBackgroundColor(Color.WHITE);
		mainLinearLayout.setPadding(16, 16, 16, 16);

		// Header azul
		final LinearLayout headerLayout = new LinearLayout(this);
		headerLayout.setBackgroundColor(Color.parseColor("#0277BD"));
		headerLayout.setOrientation(LinearLayout.HORIZONTAL);
		headerLayout.setPadding(16, 12, 16, 12);
		android.widget.LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		headerParams.setMargins(0, 0, 0, 16);
		headerLayout.setLayoutParams(headerParams);

		LabelColor titleLabel = new LabelColor(this, Color.WHITE, Gravity.CENTER);
		titleLabel.setText("DEPÓSITO - Artículos Repuestos");
		titleLabel.setTextSize(18);
		android.widget.LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		titleLabel.setLayoutParams(titleParams);
		headerLayout.addView(titleLabel);
		mainLinearLayout.addView(headerLayout);

		// Headers de columnas
		final LinearLayout columnHeaderLayout = new LinearLayout(this);
		columnHeaderLayout.setBackgroundColor(Color.parseColor("#01579B"));
		columnHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
		columnHeaderLayout.setPadding(12, 10, 12, 10);
		android.widget.LinearLayout.LayoutParams columnHeaderParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		columnHeaderParams.setMargins(0, 0, 0, 8);
		columnHeaderLayout.setLayoutParams(columnHeaderParams);

		android.widget.LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		LabelColor colCodigo = new LabelColor(this, Color.WHITE, Gravity.LEFT);
		colCodigo.setText("Código");
		colCodigo.setTextSize(13);
		colCodigo.setWidth(150);
		colCodigo.setLayoutParams(colParams);
		columnHeaderLayout.addView(colCodigo);

		LabelColor colDescripcion = new LabelColor(this, Color.WHITE, Gravity.LEFT);
		colDescripcion.setText("Descripción");
		colDescripcion.setTextSize(13);
		colDescripcion.setWidth(300);
		colDescripcion.setLayoutParams(colParams);
		columnHeaderLayout.addView(colDescripcion);

		LabelColor colUnidades = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
		colUnidades.setText("Unidades");
		colUnidades.setTextSize(13);
		colUnidades.setWidth(150);
		colUnidades.setLayoutParams(colParams);
		columnHeaderLayout.addView(colUnidades);

		LabelColor colPVP = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
		colPVP.setText("PVP");
		colPVP.setTextSize(13);
		colPVP.setWidth(150);
		colPVP.setLayoutParams(colParams);
		columnHeaderLayout.addView(colPVP);

		mainLinearLayout.addView(columnHeaderLayout);

		final LinearLayout layout = new LinearLayout(this);
		layout.setBackgroundColor(Color.WHITE);
		layout.removeAllViews();

		layout.setOrientation(LinearLayout.VERTICAL);
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		List<LineaDeposito> tempList = new ArrayList<>(_appConfig.getWorkingArea().CurrentDeposito.Lineas.values());

		Collections.sort(tempList, new LineaDeposito().new ArticuloComparator());

		int rowCount = 0;
		for (LineaDeposito linea : tempList) {
			if (linea.UnidadesRepuestas > 0) {
				final LinearLayout layout2 = new LinearLayout(this);
				layout2.removeAllViews();

				layout2.setOrientation(LinearLayout.HORIZONTAL);
				// Alternar colores de fila para mejor legibilidad
				if (rowCount % 2 == 0) {
					layout2.setBackgroundColor(Color.parseColor("#F5F5F5"));
				} else {
					layout2.setBackgroundColor(Color.WHITE);
				}
				layout2.setPadding(12, 8, 12, 8);

				int TEXT_SIZE = 14;

				LabelColor codigoArticuloLabel = new LabelColor(this, Color.parseColor("#212121"), Gravity.LEFT);
				codigoArticuloLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				codigoArticuloLabel.setText(linea.Articulo.CodigoArticulo);
				codigoArticuloLabel.setTextSize(TEXT_SIZE);
				codigoArticuloLabel.setWidth(150);
				codigoArticuloLabel.setLayoutParams(params);

				layout2.addView(codigoArticuloLabel);

				LabelColor descripcionLabel = new LabelColor(this, Color.parseColor("#212121"), Gravity.LEFT);
				descripcionLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				descripcionLabel.setText(linea.Articulo.Descripcion);
				descripcionLabel.setTextSize(TEXT_SIZE);
				descripcionLabel.setWidth(300);
				descripcionLabel.setLayoutParams(params);

				layout2.addView(descripcionLabel);

				LabelColor unidadesLabel = new LabelColor(this, Color.parseColor("#1976D2"), Gravity.RIGHT);
				unidadesLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				unidadesLabel.setText(linea.UnidadesRepuestas + " uds.");
				unidadesLabel.setTextSize(TEXT_SIZE);
				unidadesLabel.setWidth(150);
				unidadesLabel.setLayoutParams(params);

				layout2.addView(unidadesLabel);

				LabelColor pvpLabel = new LabelColor(this, Color.parseColor("#388E3C"), Gravity.RIGHT);
				pvpLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				pvpLabel.setText(linea.PVPAnterior + " €");
				pvpLabel.setTextSize(TEXT_SIZE);
				pvpLabel.setWidth(150);
				pvpLabel.setLayoutParams(params);

				layout2.addView(pvpLabel);

				layout.addView(layout2);
				rowCount++;
			}

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

		android.widget.LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params2.setMargins(0, 0, 0, 0);
		closeButton.setLayoutParams(params2);

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

	@Override
	public void onDestroy() {

		super.onDestroy();

	}

}
