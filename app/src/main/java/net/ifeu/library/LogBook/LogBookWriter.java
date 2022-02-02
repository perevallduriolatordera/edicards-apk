package net.ifeu.library.LogBook;

import android.os.Environment;

import net.ifeu.edicards.Constants.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LogBookWriter {

    public static String getTodayFileFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(new Date());
    }

    private static String getTodayFilename() {

        String formattedDate = getTodayFileFormat();
        String filename = Constants.FILE_LOGBOOLK.replace("{0}", formattedDate);

        return Environment.getExternalStorageDirectory()
                .toString()
                + "/"
                + Constants.FOLDER_ROOT
                + "/"
                + Constants.FOLDER_LOGBOOK
                + "/"
                + filename;
    }

    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    public static void write(String content)  {

        try {
            File file = new File(getTodayFilename());
            FileWriter fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            PrintWriter pr = new PrintWriter(br);
            pr.println(getTimestamp() + "#" + content);
            pr.close();
            br.close();
            fr.close();
        } catch (IOException e) {
           //nothing
        }
    }

    public static void write(List<String> content)  {

        try {
            File file = new File(getTodayFilename());
            FileWriter fr = new FileWriter(file, true);
            BufferedWriter br = new BufferedWriter(fr);
            PrintWriter pr = new PrintWriter(br);

            for (String text : content) {
                pr.println(getTimestamp() + "#" + text);
            }

            pr.close();
            br.close();
            fr.close();
        } catch (IOException e) {
            //nothing
        }
    }
}
