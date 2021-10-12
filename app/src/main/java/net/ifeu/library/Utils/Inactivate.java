package net.ifeu.library.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Inactivate {

    public static boolean inactivateIfNecessary() {
        // Posem aquest bloc per desactivar una tablet puntualment. Això ha d'estar com a data 01/01/2050
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        try {
            Date dateInactivate = sdf.parse("2100-01-01 17:00:00");
            if (dateInactivate.compareTo(new Date()) <= 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
