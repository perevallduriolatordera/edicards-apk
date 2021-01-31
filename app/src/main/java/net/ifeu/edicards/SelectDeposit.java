package net.ifeu.edicards;

import java.util.ArrayList;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.library.Controls.ButtonColor;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class SelectDeposit extends Activity {

	private final int TEXT_SIZE_BUTTON = 16;

	AppConfig _appConfig;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_deposit);
		
		setCanceledOnTouchOutside(false);

		android.view.WindowManager.LayoutParams params = getWindow()
				.getAttributes();
		params.height = 400;
		params.width = android.app.ActionBar.LayoutParams.WRAP_CONTENT;
		getWindow().setAttributes(
				(android.view.WindowManager.LayoutParams) params);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		
		
		try {
			fillDepositos();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}
	}

	private void setCanceledOnTouchOutside(boolean b) {
		// TODO Auto-generated method stub
		
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
		Deposito deposito = new Deposito();

		try {
			deposito.InitializePersistance(_appConfig, this);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e1);
		}

		ArrayList<Deposito> depositosList = (ArrayList<Deposito>) deposito
				.getDepositosByCliente(String.valueOf(cliente.IdCliente));

		final LinearLayout layout2 = new LinearLayout(this);
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.VERTICAL);
		
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

			//depositoButton.setLayoutParams(params);

			depositoButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub

					try {
						_appConfig.getWorkingArea().CurrentDeposito = dep;
						setResult(0);
						finish();

					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(
								_appConfig.getUser().User, e);
					}
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
					.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							// TODO Auto-generated method stub

							Deposito dep = new Deposito();
							try {
								dep.setClienteById(String
										.valueOf(cliente.IdCliente));
							} catch (Exception e2) {
								// TODO Auto-generated catch block
								_appConfig.getErrorTrace().Send(
										_appConfig.getUser().User, e2);
							}

							dep.TipoDeposito = Constants.TIPO_DEPOSITO_CONVENCIONAL;

							try {
								dep.InitializePersistance(_appConfig,
										arg0.getContext());
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								_appConfig.getErrorTrace().Send(
										_appConfig.getUser().User, e1);
							}

							try {
								_appConfig.getWorkingArea().CurrentDeposito = dep;
								setResult(0);
								finish();

							} catch (Exception e) {
								// TODO Auto-generated catch block
								_appConfig.getErrorTrace().Send(
										_appConfig.getUser().User, e);
							}
						}
					});

			layout.addView(depositoConvencionalButton);

			ButtonColor depositoCampanaButton = new ButtonColor(this,
					Color.BLUE);
			depositoCampanaButton.setText("Nuevo depósito de campaña");
			depositoCampanaButton.setTextColor(Color.WHITE);
			depositoCampanaButton.setLayoutParams(params);
			depositoCampanaButton.setTextSize(TEXT_SIZE_BUTTON);
			depositoCampanaButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub

					Deposito dep = new Deposito();
					try {
						dep.assingFromCliente(cliente);
					} catch (Exception e2) {
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(
								_appConfig.getUser().User, e2);
					}

					dep.TipoDeposito = Constants.TIPO_DEPOSITO_CAMPANA;

					try {
						dep.InitializePersistance(_appConfig, arg0.getContext());
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(
								_appConfig.getUser().User, e1);
					}

					try {
						_appConfig.getWorkingArea().CurrentDeposito = dep;
						setResult(0);
						finish();

					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(
								_appConfig.getUser().User, e);
					}
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
						.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								// TODO Auto-generated method stub

								Deposito dep = new Deposito();
								try {
									dep.assingFromCliente(cliente);
								} catch (Exception e2) {
									// TODO Auto-generated catch block
									_appConfig.getErrorTrace().Send(
											_appConfig.getUser().User, e2);
								}

								dep.TipoDeposito = Constants.TIPO_DEPOSITO_CONVENCIONAL;

								try {
									dep.InitializePersistance(_appConfig,
											arg0.getContext());
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									_appConfig.getErrorTrace().Send(
											_appConfig.getUser().User, e1);
								}

								try {
									_appConfig.getWorkingArea().CurrentDeposito = dep;
									setResult(0);
									finish();

								} catch (Exception e) {
									// TODO Auto-generated catch block
									_appConfig.getErrorTrace().Send(
											_appConfig.getUser().User, e);
								}
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
				depositoCampanaButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub

						Deposito dep = new Deposito();
						try {
							dep.assingFromCliente(cliente);
						} catch (Exception e2) {
							// TODO Auto-generated catch block
							_appConfig.getErrorTrace().Send(
									_appConfig.getUser().User, e2);
						}

						dep.TipoDeposito = Constants.TIPO_DEPOSITO_CAMPANA;

						try {
							dep.InitializePersistance(_appConfig,
									arg0.getContext());
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							_appConfig.getErrorTrace().Send(
									_appConfig.getUser().User, e1);
						}

						try {
							_appConfig.getWorkingArea().CurrentDeposito = dep;
							setResult(0);
							finish();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getErrorTrace().Send(
									_appConfig.getUser().User, e);
						}
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
