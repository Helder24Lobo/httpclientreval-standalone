package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.util.Registro;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Exportar un subconjunto de perfiles a un archivo JSON aparte, e importar perfiles de un archivo así
 * (o de un profiles.json completo de otra máquina) a la lista actual. Usa el mismo formato que
 * profiles.json ({@link Profile#saveAll}/{@link Profile#loadAll}), así que un export es, de hecho, un
 * profiles.json parcial válido — se puede usar directamente como tal si hace falta.
 */
final class ImportarExportarPerfiles {

    private ImportarExportarPerfiles() {
    }

    /** Deja elegir cuáles exportar y a qué archivo. No modifica {@code perfiles} ni lo ya guardado. */
    static void exportar(Component padre, Path archivoPerfiles, List<Profile> perfiles) {
        if (perfiles.isEmpty()) {
            JOptionPane.showMessageDialog(padre, "No hay perfiles para exportar.", "Exportar perfiles",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Profile> elegidos = elegirPerfiles(padre, "Exportar perfiles", perfiles, p -> p.nombre,
                "El archivo exportado incluye la llave AES y las credenciales del WS en texto plano.");
        if (elegidos == null || elegidos.isEmpty()) {
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Exportar perfiles");
        selector.setCurrentDirectory(directorioDe(archivoPerfiles));
        selector.setSelectedFile(new File("perfiles-exportados.json"));
        selector.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path destino = conExtensionJson(selector.getSelectedFile().toPath());
        try {
            Profile.saveAll(destino, elegidos);
            JOptionPane.showMessageDialog(padre,
                    elegidos.size() + " perfil(es) exportado(s) a " + destino.getFileName(),
                    "Exportar perfiles", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            Registro.error("No se pudo exportar perfiles a " + destino, ex);
            JOptionPane.showMessageDialog(padre, "No se pudo exportar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Lee un archivo (un export de este mismo diálogo, o un profiles.json completo de otra máquina),
     * deja elegir cuáles traer y los agrega a {@code perfiles}, guardando. Un nombre que ya existe se
     * renombra solo ("... (copia)", "... (copia 2)"...) en vez de sobreescribir el perfil existente.
     *
     * @return el primer perfil importado (para seleccionarlo en la ventana), o {@code null} si se
     *         canceló o no se importó nada.
     */
    static Profile importar(Component padre, Path archivoPerfiles, List<Profile> perfiles) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Importar perfiles");
        selector.setCurrentDirectory(directorioDe(archivoPerfiles));
        selector.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        if (selector.showOpenDialog(padre) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        Path origen = selector.getSelectedFile().toPath();

        List<Profile> encontrados;
        try {
            encontrados = Profile.loadAll(origen);
        } catch (Exception ex) {
            Registro.error("No se pudo leer el archivo a importar " + origen, ex);
            JOptionPane.showMessageDialog(padre, "No se pudo leer el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // Nombres que resultarían si se importan TODOS, en orden: así la casilla ya muestra de una vez
        // con qué nombre quedaría cada uno si hay choques (incluso entre sí, no solo contra los actuales).
        List<Profile> simulacion = new ArrayList<>(perfiles);
        java.util.Map<Profile, String> nombreResuelto = new java.util.HashMap<>();
        for (Profile candidato : encontrados) {
            String resuelto = nombreDisponible(simulacion, candidato.nombre);
            nombreResuelto.put(candidato, resuelto);
            Profile marcador = new Profile();
            marcador.nombre = resuelto;
            simulacion.add(marcador);
        }

        List<Profile> elegidos = elegirPerfiles(padre, "Importar perfiles", encontrados,
                p -> nombreResuelto.get(p).equals(p.nombre) ? p.nombre : p.nombre + "  →  " + nombreResuelto.get(p),
                "Un nombre que ya existe se renombra solo (se agrega \"(copia)\"); nada se sobreescribe.");
        if (elegidos == null || elegidos.isEmpty()) {
            return null;
        }

        List<Profile> agregados = new ArrayList<>();
        for (Profile original : elegidos) {
            Profile copia = original.copia();
            copia.nombre = nombreResuelto.get(original);
            perfiles.add(copia);
            agregados.add(copia);
        }
        try {
            Profile.saveAll(archivoPerfiles, perfiles);
        } catch (IOException ex) {
            perfiles.removeAll(agregados);
            Registro.error("No se pudo guardar profiles.json tras importar desde " + origen, ex);
            JOptionPane.showMessageDialog(padre, "No se pudo guardar profiles.json: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return agregados.get(0);
    }

    /** Lista con casillas (todas marcadas por defecto) y botones "Todos"/"Ninguno"; null si se canceló. */
    private static List<Profile> elegirPerfiles(Component padre, String titulo, List<Profile> candidatos,
                                                Function<Profile, String> etiqueta, String nota) {
        JCheckBox[] casillas = new JCheckBox[candidatos.size()];
        JPanel lista = new JPanel();
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        for (int i = 0; i < candidatos.size(); i++) {
            casillas[i] = new JCheckBox(etiqueta.apply(candidatos.get(i)), true);
            lista.add(casillas[i]);
        }

        JScrollPane scroll = new JScrollPane(lista);
        Accesibilidad.nombrar(scroll, titulo, "Lista de perfiles con casilla; Todos/Ninguno cambian la selección completa");
        scroll.setPreferredSize(new Dimension(440, Math.min(320, Math.max(60, candidatos.size() * 26 + 10))));

        JButton todos = new JButton("Todos");
        todos.addActionListener(e -> aplicar(casillas, true));
        JButton ninguno = new JButton("Ninguno");
        ninguno.addActionListener(e -> aplicar(casillas, false));
        JPanel botonesSeleccion = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        botonesSeleccion.add(todos);
        botonesSeleccion.add(ninguno);

        JLabel notaLabel = new JLabel("<html><body style='width:380px'>" + nota + "</body></html>");
        notaLabel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        notaLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(botonesSeleccion, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(notaLabel, BorderLayout.SOUTH);

        int resultado = JOptionPane.showConfirmDialog(padre, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (resultado != JOptionPane.OK_OPTION) {
            return null;
        }

        List<Profile> elegidos = new ArrayList<>();
        for (int i = 0; i < candidatos.size(); i++) {
            if (casillas[i].isSelected()) {
                elegidos.add(candidatos.get(i));
            }
        }
        return elegidos;
    }

    private static void aplicar(JCheckBox[] casillas, boolean seleccionado) {
        for (JCheckBox c : casillas) {
            c.setSelected(seleccionado);
        }
    }

    /** Si {@code nombreOriginal} ya existe en {@code existentes}, el primer "... (copia[ N])" que esté libre. */
    private static String nombreDisponible(List<Profile> existentes, String nombreOriginal) {
        if (!existeNombre(existentes, nombreOriginal)) {
            return nombreOriginal;
        }
        String candidato = nombreOriginal + " (copia)";
        for (int n = 2; existeNombre(existentes, candidato); n++) {
            candidato = nombreOriginal + " (copia " + n + ")";
        }
        return candidato;
    }

    private static boolean existeNombre(List<Profile> existentes, String nombre) {
        for (Profile p : existentes) {
            if (p.nombre.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    private static File directorioDe(Path archivoPerfiles) {
        Path directorio = archivoPerfiles.toAbsolutePath().getParent();
        return directorio == null ? null : directorio.toFile();
    }

    private static Path conExtensionJson(Path archivo) {
        String nombre = archivo.getFileName().toString();
        return nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".json")
                ? archivo : archivo.resolveSibling(nombre + ".json");
    }
}
