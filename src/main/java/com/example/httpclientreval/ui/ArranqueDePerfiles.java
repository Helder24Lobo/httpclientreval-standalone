package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.PerfilesInicial;
import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.util.Registro;
import com.example.httpclientreval.util.RutasApp;

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
 * Carga los perfiles al arrancar sin preguntar nada: si el archivo falta, lo migra solo desde las
 * ubicaciones anteriores (ver cargarSinPreguntar). Solo si el archivo existe pero no se puede leer, en vez
 * de cerrar la app con un error muestra un diálogo para arreglarlo: combinar el profiles.json de la carpeta de trabajo (migración desde la ubicación
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

    /**
     * Camino normal, sin diálogos: si el archivo falta lo arma con lo que haya en ubicaciones anteriores
     * (combinado, sin pisar nada) o, si no hay nada, con perfiles de ejemplo. Devuelve {@code null}
     * solo si algo falla y hay que preguntarle al usuario (archivo dañado o ilegible).
     */
    private static List<Profile> cargarSinPreguntar(Path archivo) {
        try {
            if (!Files.exists(archivo)) {
                for (Path anterior : RutasApp.perfilesAnteriores()) {
                    if (PerfilesInicial.esLegible(anterior)) {
                        int agregados = PerfilesInicial.combinar(anterior, archivo);
                        Registro.advertencia("Se migraron perfiles de " + anterior + " a " + archivo
                                + " (" + agregados + " agregados)", null);
                    }
                }
                if (!Files.exists(archivo)) {
                    PerfilesInicial.crearDesdeEjemplo(archivo);
                    Registro.advertencia("Se creó " + archivo + " con perfiles de ejemplo", null);
                }
            }
            return Profile.loadAll(archivo);
        } catch (Exception ex) {
            Registro.error("No se pudo cargar " + archivo, ex);
            return null;
        }
    }

    /** @return los perfiles cargados, o {@code null} si el usuario decidió salir. */
    public static List<Profile> cargarOCrear(Path archivo) {
        List<Profile> automaticos = cargarSinPreguntar(archivo);
        if (automaticos != null) {
            return automaticos;
        }
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
            String copiarLegado = "Combinar con el de la carpeta actual";
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
                    + (existe ? "\n\nSi eliges una opción, el archivo actual se guarda aparte (no se borra)." : "")
                    + (hayLegado || !existe ? "\n\nCombinar e importar suman las transacciones que falten y nunca pisan las que ya tengas." : "");
            int eleccion = JOptionPane.showOptionDialog(propietario, mensaje, "Perfiles",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    opciones.toArray(), opciones.get(0));
            if (eleccion < 0 || opciones.get(eleccion).equals(salir)) {
                return null;
            }

            try {
                String elegida = opciones.get(eleccion);
                if (elegida.equals(copiarLegado)) {
                    combinarConActual(legado, archivo);
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
                    combinarConActual(origen, archivo);
                }
            } catch (Exception ex) {
                Registro.error("No se pudo preparar " + archivo, ex);
                JOptionPane.showMessageDialog(propietario, "No se pudo preparar el archivo de perfiles:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            // Vuelve al inicio del ciclo: intenta cargar de nuevo, o vuelve a preguntar si algo falló.
        }
    }

    /**
     * Trae los perfiles de {@code origen} sin pisar los que ya hay en {@code archivo}. Solo se aparta
     * el archivo actual si no se puede leer; uno legible se conserva y se le suman los que le faltan.
     */
    private static void combinarConActual(Path origen, Path archivo) throws java.io.IOException {
        if (!PerfilesInicial.esLegible(archivo)) {
            apartarSiExiste(archivo);
        }
        int agregados = PerfilesInicial.combinar(origen, archivo);
        Registro.advertencia("Se combinaron perfiles de " + origen + " con " + archivo
                + " (" + agregados + " agregados)", null);
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
