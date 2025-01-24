package net.ifeu.library.Utils.Number;

public class NumberDecimal {

    public static float roundToNearestFive(double value) {
        // Dividimos el valor entre 5, redondeamos al entero más cercano y lo multiplicamos por 5
        return Math.round(value / 5) * 5;
    }
}
