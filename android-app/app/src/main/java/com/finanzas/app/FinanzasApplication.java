package com.finanzas.app;

import android.app.Application;

import com.finanzas.app.util.ThemeManager;

/**
 * Clase Application principal que inicializa el Tema Oscuro Clasico por defecto
 * al arrancar el proceso de la aplicacion.
 */
public class FinanzasApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applyTheme(this);
    }
}
