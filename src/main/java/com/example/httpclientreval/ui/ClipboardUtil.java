package com.example.httpclientreval.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtil {

    private ClipboardUtil() {
    }

    public static void copiar(String texto) {
        StringSelection seleccion = new StringSelection(texto == null ? "" : texto);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(seleccion, null);
    }
}
