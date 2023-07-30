package net.ifeu.library.Utils.MessageBox;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;

import net.ifeu.edicards.R;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

public class AdvancedMessageBox {

    private boolean _result;
    private AlertDialog.Builder _dialog;

    private void setResult(boolean result) {
        this._result = result;
    }

    private AlertDialog.Builder getDialog() {
        return this._dialog;
    }

    public void Close() {
        this._dialog.create().dismiss();
    }

    public boolean Show(String title, String text, String response1, String response2, Context context, MessageBoxType type)
    {
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                throw new RuntimeException("@Custom");
            }
        };

        this._dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme));
        this._dialog.setTitle(title);
        this._dialog.setMessage(text);

        this._dialog.setPositiveButton(response1, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int arg1) {
                setResult(true);
                handler.sendMessage(handler.obtainMessage());
                dialog.dismiss();
            }});

        this._dialog.setNegativeButton(response2, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                setResult(false);
                handler.sendMessage(handler.obtainMessage());
                dialog.dismiss();
            }});

        this._dialog.setCancelable(false);

        this._dialog.create().show();
        // loop till a runtime exception is triggered.
        try { Looper.loop(); }
        catch(RuntimeException e2) {}

        return _result;
    }
}
