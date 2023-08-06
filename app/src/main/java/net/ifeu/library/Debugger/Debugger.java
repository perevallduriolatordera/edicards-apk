package net.ifeu.library.Debugger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import net.ifeu.edicards.Constants.ConstantsMail;
import net.ifeu.library.Mail.MailSender;

import java.io.IOException;

import javax.mail.MessagingException;

public class Debugger {

    public static void Debug(Context context, String user, String message, String file) throws PackageManager.NameNotFoundException {

        PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        String versionName = pInfo.versionName;

        if (!versionName.toUpperCase().contains("DEBUG")) return;

        Thread thread = new Thread(() -> {
            try  {
                MailSender mailEnviosMantenimiento = new MailSender(ConstantsMail.MAIL_MANTENIMIENTO, "DEBUG edicards " + user, message, file);
                try {
                    mailEnviosMantenimiento.send();
                } catch (MessagingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }
}
