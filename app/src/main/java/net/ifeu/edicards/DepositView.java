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
		params.width = 800;
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
		mainLinearLayout.setBackgroundColor(Color.BLACK);

		final LinearLayout layout = new LinearLayout(this);
		layout.setBackgroundColor(Color.BLACK);
		layout.removeAllViews();

		layout.setOrientation(LinearLayout.VERTICAL);
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		List<LineaDeposito> tempList = new ArrayList<>(_appConfig.getWorkingArea().CurrentDeposito.Lineas.values());

		Collections.sort(tempList, new LineaDeposito().new ArticuloComparator());

		for (LineaDeposito linea : tempList) {
			final LinearLayout layout2 = new LinearLayout(this);
			layout2.removeAllViews();

			layout2.setOrientation(LinearLayout.HORIZONTAL);
			layout2.setBackgroundColor(Color.BLACK);

			if (linea.UnidadesRepuestas > 0) {
				LabelColor codigoArticuloLabel = new LabelColor(this, Color.WHITE, Gravity.LEFT);
				codigoArticuloLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				codigoArticuloLabel.setText(linea.Articulo.CodigoArticulo);
				int TEXT_SIZE = 14;
				codigoArticuloLabel.setTextSize(TEXT_SIZE);
				codigoArticuloLabel.setWidth(150);
				codigoArticuloLabel.setLayoutParams(params);

				layout2.addView(codigoArticuloLabel);

				LabelColor descripcionLabel = new LabelColor(this, Color.WHITE, Gravity.LEFT);
				descripcionLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				descripcionLabel.setText(linea.Articulo.Descripcion);
				descripcionLabel.setTextSize(TEXT_SIZE);
				descripcionLabel.setWidth(300);
				descripcionLabel.setLayoutParams(params);

				layout2.addView(descripcionLabel);

				LabelColor unidadesLabel = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
				unidadesLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				unidadesLabel.setText(linea.UnidadesRepuestas + " unidades");
				unidadesLabel.setTextSize(TEXT_SIZE);
				unidadesLabel.setWidth(150);
				unidadesLabel.setLayoutParams(params);

				layout2.addView(unidadesLabel);

				LabelColor pvpLabel = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
				pvpLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
				pvpLabel.setText(linea.PVPAnterior + " €");
				pvpLabel.setTextSize(TEXT_SIZE);
				pvpLabel.setWidth(150);
				pvpLabel.setLayoutParams(params);

				layout2.addView(pvpLabel);

			}

			layout.addView(layout2);

		}

		// Botón Cerrar

		final LinearLayout layout3 = new LinearLayout(this);
		layout3.removeAllViews();

		layout3.setOrientation(LinearLayout.HORIZONTAL);
		layout3.setBackgroundColor(Color.BLACK);

		ButtonColor closeButton = new ButtonColor(this, Color.WHITE);

		closeButton.setText("Cerrar");
		int TEXT_SIZE_BUTTON = 14;
		closeButton.setTextSize(TEXT_SIZE_BUTTON);
		int BUTTONS_WIDTH = 130;
		closeButton.setWidth(BUTTONS_WIDTH);

		android.widget.LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params2.setMargins(0, 30, 0, 0);
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
