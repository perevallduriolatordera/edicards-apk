package net.ifeu.library.Barcodes;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class BarcodeGenerator {

    public static Bitmap generateBarcode(String data, BarcodeFormat format, int width, int height) {

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, format, width, height);

            return bitmap;
        } catch (WriterException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
