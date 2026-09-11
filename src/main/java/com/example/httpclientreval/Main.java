package com.example.httpclientreval;

import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.ui.AppWindow;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Punto de entrada: corre este archivo con el botón ▶️ de IntelliJ. Toda la
 * interacción (elegir perfil, elegir modo, llenar campos) pasa por la
 * ventana Swing (AppWindow) — ya no hay menús de consola.
 *
 * La llave AES y los defaults por transacción viven en profiles.json (copia
 * profiles.example.json y complétalo); profiles.json está en .gitignore
 * para que ningún secreto real quede commiteado.
 */
public class Main {

    private static final Path ARCHIVO_PERFILES = Paths.get("profiles.json");

    public static void main(String[] args) throws Exception {
        FlatLightLaf.setup();

        List<Profile> perfiles = Profile.loadAll(ARCHIVO_PERFILES);

        SwingUtilities.invokeLater(() -> {
            AppWindow ventana = new AppWindow(perfiles, ARCHIVO_PERFILES);
            ventana.setVisible(true);

            // Fuerza la ventana al frente si el proceso se lanzó desde una
            // terminal/IDE con foco (Windows no siempre se lo cede solo).
            ventana.setAlwaysOnTop(true);
            ventana.toFront();
            ventana.requestFocus();
            ventana.setAlwaysOnTop(false);
        });
    }
}
