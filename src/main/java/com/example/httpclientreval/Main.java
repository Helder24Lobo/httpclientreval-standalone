package com.example.httpclientreval;

import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.ui.AppWindow;
import com.example.httpclientreval.ui.ArranqueDePerfiles;
import com.example.httpclientreval.ui.TemaPreferencias;
import com.example.httpclientreval.util.RutasApp;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Punto de entrada: corre este archivo con el botón ▶️ de IntelliJ. Toda la
 * interacción (elegir perfil, elegir modo, llenar campos) pasa por la
 * ventana Swing (AppWindow) — ya no hay menús de consola.
 *
 * La llave AES y los defaults por transacción viven en profiles.json, dentro de la carpeta de datos
 * del usuario (ver RutasApp: en Windows, ~\.httpclientreval), fuera del proyecto para que
 * ningún secreto real quede junto al código. Si el archivo falta se migra solo desde ubicaciones
 * anteriores; solo si está dañado el arranque pregunta qué hacer (ver ArranqueDePerfiles).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        TemaPreferencias.instalar();

        Path archivoPerfiles = RutasApp.archivoPerfiles();
        AtomicReference<List<Profile>> cargados = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> cargados.set(ArranqueDePerfiles.cargarOCrear(archivoPerfiles)));
        List<Profile> perfiles = cargados.get();
        if (perfiles == null) {
            System.exit(0); // el usuario eligió salir en el diálogo de arranque
        }

        SwingUtilities.invokeLater(() -> {
            AppWindow ventana = new AppWindow(perfiles, archivoPerfiles);
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
