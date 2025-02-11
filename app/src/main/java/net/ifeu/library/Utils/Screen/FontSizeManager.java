package net.ifeu.library.Utils.Screen;

import android.content.res.Configuration;
import android.util.TypedValue;
import android.widget.TextView;

public class FontSizeManager {

    public static void adjustFontSize(TextView textView, float baseSize) {
        Configuration config = textView.getContext().getResources().getConfiguration();
        float fontScale = config.fontScale; // Escala de la fuente del sistema

        // Ajustar el tamaño de la fuente proporcionalmente
        float finalSize = baseSize / fontScale;

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, finalSize);
    }
}
