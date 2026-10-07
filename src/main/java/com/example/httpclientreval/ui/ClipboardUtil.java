package com.example.httpclientreval.ui;

import com.example.httpclientreval.util.Registro;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtil {

    private ClipboardUtil() {
    }

    public static void copiar(String texto) {
        try {
            StringSelection seleccion = new StringSelection(texto == null ? "" : texto);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(seleccion, null);
        } catch (Exception e) {
            Registro.advertencia("No se pudo copiar al portapapeles del sistema operativo", e);
        }
    }
}
