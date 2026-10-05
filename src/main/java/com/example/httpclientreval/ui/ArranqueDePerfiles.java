package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.PerfilesInicial;
import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.util.Registro;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga los perfiles al arrancar. Si el archivo falta (primer arranque, o se movió a la carpeta de
 * usuario) o no se puede leer, en vez de cerrar la app con un error muestra un diálogo para
 * arreglarlo: copiar el profiles.json de la carpeta de trabajo (migración desde la ubicación
 * anterior), crear uno con perfiles de ejemplo, importar otro archivo, o salir. Un archivo dañado
 * nunca se borra: se aparta con otro nombre antes de crear el nuevo.
 */
public final class ArranqueDePerfiles {

    private ArranqueDePerfiles() {
    }

    /**
     * Los diálogos cuelgan de una ventana propietaria invisible y siempre al frente: con padre {@code null}
     * quedaban detrás del IDE/terminal que lanzó la app, sin icono en la barra de tareas, y el arranque
     * parecía colgado esperando una respuesta que nadie veía.
     */
    private static JFrame crearPropietario() {
        JFrame propietario = new JFrame("httpclientreval");
        propietario.setUndecorated(true);
        propietario.setSize(1, 1);
        propietario.setLocationRelativeTo(null);
        propietario.setAlwaysOnTop(true);
        propietario.setVisible(true);
        return propietario;
    }

    /** @return los perfiles cargados, o {@code null} si el usuario decidió salir. */
    public static List<Profile> cargarOCrear(Path archivo) {
        JFrame propietario = crearPropietario();
        try {
            return cargarOCrear(propietario, archivo);
        } finally {
            propietario.dispose();
        }
    }

    private static List<Profile> cargarOCrear(JFrame propietario, Path archivo) {
        while (true) {
            boolean existe = Files.exists(archivo);
            String problema;
            if (existe) {
                try {
                    return Profile.loadAll(archivo);
                } catch (Exception ex) {
                    Registro.error("No se pudo leer " + archivo, ex);
                    problema = "El archivo de perfiles existe pero no se pudo leer:\n" + ex.getMessage();
                }
            } else {
                problema = "No se encontró el archivo de perfiles.";
            }

            // profiles.json de versiones anteriores, que vivía en la carpeta desde la que se lanzaba la app.
            Path legado = Paths.get("profiles.json").toAbsolutePath();
            boolean hayLegado = Files.isRegularFile(legado) && !legado.equals(archivo.toAbsolutePath());

            List<String> opciones = new ArrayList<>();
            String copiarLegado = "Copiar el de la carpeta actual";
            String crearEjemplo = "Crear con perfiles de ejemplo";
            String importar = "Importar de otro archivo...";
            String salir = "Salir";
            if (hayLegado) {
                opciones.add(copiarLegado);
            }
            opciones.add(crearEjemplo);
            opciones.add(importar);
            opciones.add(salir);

            String mensaje = problema + "\n\nUbicación esperada:\n" + archivo.toAbsolutePath()
                    + (hayLegado ? "\n\nEncontré un profiles.json de una versión anterior en:\n" + legado : "")
                    + (existe ? "\n\nSi eliges crear o importar, el archivo actual se guarda aparte (no se borra)." : "");
            int eleccion = JOptionPane.showOptionDialog(propietario, mensaje, "Perfiles",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    opciones.toArray(), opciones.get(0));
            if (eleccion < 0 || opciones.get(eleccion).equals(salir)) {
                return null;
            }

            try {
                String elegida = opciones.get(eleccion);
                if (elegida.equals(copiarLegado)) {
                    apartarSiExiste(archivo);
                    PerfilesInicial.copiar(legado, archivo);
                } else if (elegida.equals(crearEjemplo)) {
                    apartarSiExiste(archivo);
                    PerfilesInicial.crearDesdeEjemplo(archivo);
                    JOptionPane.showMessageDialog(propietario,
                            "Se creó " + archivo.toAbsolutePath() + " con perfiles de ejemplo.\n\n"
                                    + "Edita la llave AES y las credenciales de cada perfil antes de enviar.",
                            "Perfiles", JOptionPane.INFORMATION_MESSAGE);
                } else if (elegida.equals(importar)) {
                    Path origen = elegirArchivo(propietario);
                    if (origen == null) {
                        continue;
                    }
                    apartarSiExiste(archivo);
                    PerfilesInicial.copiar(origen, archivo);
                }
            } catch (Exception ex) {
                Registro.error("No se pudo preparar " + archivo, ex);
                JOptionPane.showMessageDialog(propietario, "No se pudo preparar el archivo de perfiles:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            // Vuelve al inicio del ciclo: intenta cargar de nuevo, o vuelve a preguntar si algo falló.
        }
    }

    private static void apartarSiExiste(Path archivo) throws java.io.IOException {
        if (Files.exists(archivo)) {
            Path aparte = PerfilesInicial.apartarCorrupto(archivo);
            Registro.advertencia("Se apartó el archivo de perfiles anterior como " + aparte, null);
        }
    }

    private static Path elegirArchivo(JFrame propietario) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Importar perfiles");
        selector.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        return selector.showOpenDialog(propietario) == JFileChooser.APPROVE_OPTION
                ? selector.getSelectedFile().toPath() : null;
    }
}
