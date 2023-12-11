package net.ifeu.edicards;

import java.util.ArrayList;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.library.Controls.ButtonColor;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class SelectDeposit extends Activity {

	AppConfig _appConfig;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_deposit);

		android.view.WindowManager.LayoutParams params = getWindow()
				.getAttributes();
		params.height = 400;
		params.width = android.app.ActionBar.LayoutParams.WRAP_CONTENT;
		getWindow().setAttributes(
				params);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		
		
		try {
			fillDepositos();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void fillDepositos() throws Exception {
		_appConfig = (AppConfig) this.getApplicationContext();

		LinearLayout mainLinearLayout = (LinearLayout) this
				.findViewById(R.id.mainLinearLayout);
		mainLinearLayout.removeAllViews();
		mainLinearLayout.setOrientation(LinearLayout.VERTICAL);

		final LinearLayout layout = new LinearLayout(this);
		layout.removeAllViews();
		layout.setOrientation(LinearLayout.VERTICAL);

		@SuppressWarnings("deprecation")
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		params.setMargins(0, 30, 0, 30);

		final Cliente cliente = _appConfig.getWorkingArea().CurrentCliente;
		Deposito deposito = Factory.build(Deposito.class, _appConfig);

		ArrayList<Deposito> depositosList = deposito
				.getDepositosByCliente(String.valueOf(cliente.IdCliente));

		final LinearLayout layout2 = new LinearLayout(this);
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.VERTICAL);

		int TEXT_SIZE_BUTTON = 16;
		for (final Deposito dep : depositosList) {

			ButtonColor depositoButton = new ButtonColor(this, Color.RED);
			depositoButton.setTextColor(Color.WHITE);
			depositoButton.setLayoutParams(params);

			String buttonText = dep.isDepositoConvencional() ? "Depósito Convencional"
					+ " ( " + dep.Lineas.size() + " artículos )"
					: "Depósito de Campaña" + " ( " + dep.Lineas.size()
							+ " artículos )";

			depositoButton.setText(buttonText);
			depositoButton.setTextSize(TEXT_SIZE_BUTTON);

			depositoButton.setOnClickListener(arg0 -> {

				try {
					_appConfig.getWorkingArea().CurrentDeposito = dep;
					setResult(0);
					finish();

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			layout2.addView(depositoButton);
		}
		
		switch (depositosList.size()) {
		case 0: {

			ButtonColor depositoConvencionalButton = new ButtonColor(this,
					Color.BLUE);
			depositoConvencionalButton.setText("Nuevo depósito convencional");
			depositoConvencionalButton.setTextColor(Color.WHITE);
			depositoConvencionalButton.setLayoutParams(params);
			depositoConvencionalButton.setTextSize(TEXT_SIZE_BUTTON);
			depositoConvencionalButton
					.setOnClickListener(arg0 -> {

						Deposito dep = Factory.build(Deposito.class, _appConfig);
						try {
							dep.setClienteById(String
									.valueOf(cliente.IdCliente));
						} catch (Exception e2) {
							throw new RuntimeException(e2);
						}

						dep.TipoDeposito = ConstantsTypes.TIPO_DEPOSITO_CONVENCIONAL;

						try {
							_appConfig.getWorkingArea().CurrentDeposito = dep;
							setResult(0);
							finish();

						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});

			layout.addView(depositoConvencionalButton);

			ButtonColor depositoCampanaButton = new ButtonColor(this,
					Color.BLUE);
			depositoCampanaButton.setText("Nuevo depósito de campaña");
			depositoCampanaButton.setTextColor(Color.WHITE);
			depositoCampanaButton.setLayoutParams(params);
			depositoCampanaButton.setTextSize(TEXT_SIZE_BUTTON);
			depositoCampanaButton.setOnClickListener(arg0 -> {

				Deposito dep = Factory.build(Deposito.class, _appConfig);
				try {
					dep.assingFromCliente(cliente);
				} catch (Exception e2) {
					throw new RuntimeException(e2);
				}

				dep.TipoDeposito = ConstantsTypes.TIPO_DEPOSITO_CAMPANA;

				try {
					_appConfig.getWorkingArea().CurrentDeposito = dep;
					setResult(0);
					finish();

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			layout.addView(depositoCampanaButton);

			break;
		}
		case 1: {

			if (!depositosList.get(0).isDepositoConvencional()) {
				ButtonColor depositoConvencionalButton = new ButtonColor(this,
						Color.BLUE);
				depositoConvencionalButton
						.setText("Nuevo depósito convencional");
				depositoConvencionalButton.setLayoutParams(params);
				depositoConvencionalButton.setTextColor(Color.WHITE);
				depositoConvencionalButton.setTextSize(TEXT_SIZE_BUTTON);
				depositoConvencionalButton
						.setOnClickListener(arg0 -> {

							Deposito dep = Factory.build(Deposito.class, _appConfig);
							try {
								dep.assingFromCliente(cliente);
							} catch (Exception e2) {
								throw new RuntimeException(e2);
							}

							dep.TipoDeposito = ConstantsTypes.TIPO_DEPOSITO_CONVENCIONAL;

							try {
								_appConfig.getWorkingArea().CurrentDeposito = dep;
								setResult(0);
								finish();

							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						});

				layout.addView(depositoConvencionalButton);
			} else {

				ButtonColor depositoCampanaButton = new ButtonColor(this,
						Color.BLUE);
				depositoCampanaButton.setText("Nuevo depósito de campaña");
				depositoCampanaButton.setTextColor(Color.WHITE);
				depositoCampanaButton.setTextSize(TEXT_SIZE_BUTTON);
				depositoCampanaButton.setLayoutParams(params);
				depositoCampanaButton.setOnClickListener(arg0 -> {

					Deposito dep = Factory.build(Deposito.class,_appConfig);
					try {
						dep.assingFromCliente(cliente);
					} catch (Exception e2) {
						throw new RuntimeException(e2);
					}

					dep.TipoDeposito = ConstantsTypes.TIPO_DEPOSITO_CAMPANA;

					try {
						_appConfig.getWorkingArea().CurrentDeposito = dep;
						setResult(0);
						finish();

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});

				layout.addView(depositoCampanaButton);
			}

			break;
		}
		default: {
			break;
		}
		}

		layout.addView(layout2);

		mainLinearLayout.addView(layout);

	}

}
