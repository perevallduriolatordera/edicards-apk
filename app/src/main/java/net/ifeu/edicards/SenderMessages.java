package net.ifeu.edicards;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Utils.MessageBoxType;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.parse.ParsePush;

public class SenderMessages extends Fragment {

	private AppConfig _appConfig;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.i("SenderMessages", "Create");

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.activity_sender_messages, container,
				false);
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		_appConfig = (AppConfig) this.getActivity()
				.getApplicationContext();

		final Button sendUpgradeButton = (Button) this.getActivity()
				.findViewById(R.id.btnSendUpgrade);

		sendUpgradeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {

				try {

					boolean send = _appConfig
							.getMessageBox()
							.ShowWithResult(
									"Envió de notificación",
									"Está seguro que quiere enviar una notificación de actualización al resto de comerciales ?",
									v.getContext(),
									MessageBoxType.Information);

					if (send) {
						ParsePush push = new ParsePush();
						push.setChannel(Constants.PARSE_VERSION_CHANNEL);
						push.setMessage(Constants.PARSE_VERSION_TEXT);
						push.sendInBackground();
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}
		});
		
		final Button sendButton = (Button) this.getActivity()
				.findViewById(R.id.btnSend);
		
		final EditText recordatorio = (EditText) this.getActivity().findViewById(R.id.txtRecordatorio);

		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {

				try {

					boolean send = _appConfig
							.getMessageBox()
							.ShowWithResult(
									"Envió de recordatorio",
									"Está seguro que quiere enviar el recordatorio al resto de comerciales ?",
									v.getContext(),
									MessageBoxType.Information);

					/*if (send) {
						
						String text = recordatorio.getText().toString();
						
						//_appConfig.getRecordatorio().save(text);
					}*/

				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}
		});

	}

}
