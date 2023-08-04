package net.ifeu.edicards.DataTier.Factories;

import android.content.Context;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Persistance.Persistent;


public class Factory {

    public static <T> T build(Class<? extends Persistent> clazz, AppConfig context) {

        T instance;
        try {
            instance = (T) clazz.newInstance();
            ((Persistent) instance).InitializePersistance(context);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return instance;
    }
}
