package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ventana principal. Las transacciones se eligen en dos pasos: primero el
 * grupo (lo que va antes del " - " en el nombre del perfil, ej. "Recaudos") y
 * luego la transacción de ese grupo. El modo (Cifrar/Descifrar) es otro combo;
 * "Nueva transacción" registra un perfil sin editar profiles.json a mano,
 * "Renombrar" y "Eliminar perfil" lo modifican o borran (también en
 * profiles.json), y "Configuración" cambia el tema claro/oscuro en caliente.
 */
public class AppWindow extends JFrame {

    private final List<Profile> perfiles;
    private final Path archivoPerfiles;
    private final JComboBox<String> comboGrupo = new JComboBox<>();
    private final DefaultComboBoxModel<Profile> modeloPerfiles = new DefaultComboBoxModel<>();
    private final JComboBox<Profile> comboPerfil = new JComboBox<>(modeloPerfiles);
    private final JComboBox<String> comboModo;
    private final JPanel centro = new JPanel(new BorderLayout());
    private boolean mostrandoNuevaTransaccion = false;
    private boolean actualizandoCombos = false;

    public AppWindow(List<Profile> perfiles, Path archivoPerfiles) {
        super("httpclientreval");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.perfiles = new ArrayList<>(perfiles);
        this.archivoPerfiles = archivoPerfiles;

        comboModo = new JComboBox<>(new String[]{"Cifrar (ENCRYPT)", "Descifrar (DECRYPT)"});

        // En el combo de transacciones solo se muestra el detalle; el grupo ya está en el otro combo.
        comboPerfil.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object texto = value instanceof Profile ? ((Profile) value).detalle() : value;
                return super.getListCellRendererComponent(list, texto, index, isSelected, cellHasFocus);
            }
        });

        comboGrupo.addActionListener(e -> {
            if (actualizandoCombos) {
                return;
            }
            cargarPerfilesDelGrupo((String) comboGrupo.getSelectedItem(), null);
            mostrandoNuevaTransaccion = false;
            refrescar();
        });
        comboPerfil.addActionListener(e -> {
            if (actualizandoCombos) {
                return;
            }
            mostrandoNuevaTransaccion = false;
            refrescar();
        });
        comboModo.addActionListener(e -> {
            mostrandoNuevaTransaccion = false;
            refrescar();
        });

        JButton nuevaTransaccion = new JButton("Nueva transacción", Icons.nuevo());
        nuevaTransaccion.addActionListener(e -> {
            mostrandoNuevaTransaccion = true;
            refrescar();
        });

        JButton renombrarPerfil = new JButton("Renombrar", Icons.editar());
        renombrarPerfil.addActionListener(e -> renombrarPerfilSeleccionado());

        JButton eliminarPerfil = new JButton("Eliminar perfil", Icons.limpiar());
        eliminarPerfil.addActionListener(e -> eliminarPerfilSeleccionado());

        JButton configuracion = new JButton("Configuración", Icons.configuracion());
        configuracion.addActionListener(e -> abrirConfiguracion());

        JPanel filaTransaccion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaTransaccion.add(new JLabel("Grupo:"));
        filaTransaccion.add(comboGrupo);
        filaTransaccion.add(new JLabel("Transacción:"));
        filaTransaccion.add(comboPerfil);
        filaTransaccion.add(renombrarPerfil);
        filaTransaccion.add(eliminarPerfil);

        JPanel filaAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaAcciones.add(new JLabel("Modo:"));
        filaAcciones.add(comboModo);
        filaAcciones.add(nuevaTransaccion);
        filaAcciones.add(configuracion);

        JPanel norte = new JPanel();
        norte.setLayout(new BoxLayout(norte, BoxLayout.Y_AXIS));
        norte.add(filaTransaccion);
        norte.add(filaAcciones);

        setLayout(new BorderLayout());
        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        cargarGrupos(this.perfiles.get(0).grupo(), this.perfiles.get(0));
        refrescar();

        // El tamaño se fija en "centro" (no en el JFrame) para que pack() calcule
        // la altura real de "norte" incluyendo el ancho que ocupan sus botones,
        // en vez de forzar un tamaño total que los solape con las pestañas.
        centro.setPreferredSize(new Dimension(900, 650));
        pack();
        setLocationRelativeTo(null);
    }

    /** Rellena el combo de grupos (en orden de aparición) y deja seleccionado el indicado. */
    private void cargarGrupos(String grupoASeleccionar, Profile perfilASeleccionar) {
        actualizandoCombos = true;
        try {
            Set<String> grupos = new LinkedHashSet<>();
            for (Profile p : perfiles) {
                grupos.add(p.grupo());
            }
            comboGrupo.removeAllItems();
            for (String g : grupos) {
                comboGrupo.addItem(g);
            }
            comboGrupo.setSelectedItem(grupos.contains(grupoASeleccionar) ? grupoASeleccionar : grupos.iterator().next());
        } finally {
            actualizandoCombos = false;
        }
        cargarPerfilesDelGrupo((String) comboGrupo.getSelectedItem(), perfilASeleccionar);
    }

    /** Rellena el combo de transacciones con las del grupo dado, sin disparar refrescos. */
    private void cargarPerfilesDelGrupo(String grupo, Profile perfilASeleccionar) {
        actualizandoCombos = true;
        try {
            modeloPerfiles.removeAllElements();
            Profile primero = null;
            for (Profile p : perfiles) {
                if (p.grupo().equals(grupo)) {
                    modeloPerfiles.addElement(p);
                    if (primero == null) {
                        primero = p;
                    }
                }
            }
            Profile elegido = perfilASeleccionar != null && perfilASeleccionar.grupo().equals(grupo)
                    ? perfilASeleccionar : primero;
            modeloPerfiles.setSelectedItem(elegido);
        } finally {
            actualizandoCombos = false;
        }
    }

    private void refrescar() {
        centro.removeAll();

        if (mostrandoNuevaTransaccion) {
            centro.add(new NewProfilePanel(archivoPerfiles, perfiles, this::alGuardarPerfil), BorderLayout.CENTER);
        } else if (comboModo.getSelectedIndex() == 0) {
            centro.add(new EncryptPanel((Profile) comboPerfil.getSelectedItem(),
                    () -> Profile.saveAll(archivoPerfiles, perfiles)), BorderLayout.CENTER);
        } else {
            centro.add(new DecryptPanel((Profile) comboPerfil.getSelectedItem()), BorderLayout.CENTER);
        }

        centro.revalidate();
        centro.repaint();
    }

    private void renombrarPerfilSeleccionado() {
        Profile seleccionado = (Profile) comboPerfil.getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        Object entrada = JOptionPane.showInputDialog(this,
                "Nuevo nombre (formato \"Grupo - Transacción\"):",
                "Renombrar perfil", JOptionPane.PLAIN_MESSAGE, null, null, seleccionado.nombre);
        if (entrada == null) {
            return;
        }

        String nuevoNombre = ((String) entrada).trim();
        if (nuevoNombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre no puede quedar vacío.",
                    "Renombrar perfil", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nuevoNombre.equals(seleccionado.nombre)) {
            return;
        }
        for (Profile otro : perfiles) {
            if (otro != seleccionado && otro.nombre.equalsIgnoreCase(nuevoNombre)) {
                JOptionPane.showMessageDialog(this, "Ya existe un perfil con ese nombre.",
                        "Renombrar perfil", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String nombreAnterior = seleccionado.nombre;
        try {
            seleccionado.nombre = nuevoNombre;
            Profile.saveAll(archivoPerfiles, perfiles);
            // El grupo puede haber cambiado; se recargan los combos sin refrescar el panel
            // para no perder lo que haya escrito en el formulario.
            cargarGrupos(seleccionado.grupo(), seleccionado);
        } catch (IOException ex) {
            seleccionado.nombre = nombreAnterior;
            JOptionPane.showMessageDialog(this,
                    "No se pudo actualizar profiles.json: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarPerfilSeleccionado() {
        Profile seleccionado = (Profile) comboPerfil.getSelectedItem();
        if (seleccionado == null) {
            return;
        }

        if (perfiles.size() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "No puedes eliminar el único perfil que queda.",
                    "No se puede eliminar", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el perfil \"" + seleccionado.nombre + "\"?\n"
                        + "Se borra también de profiles.json. Esta acción no se puede deshacer.",
                "Eliminar perfil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        int posicion = perfiles.indexOf(seleccionado);
        try {
            perfiles.remove(seleccionado);
            Profile.saveAll(archivoPerfiles, perfiles);
            // Si el grupo se quedó sin transacciones, cargarGrupos elige otro.
            cargarGrupos(seleccionado.grupo(), null);
            mostrandoNuevaTransaccion = false;
            refrescar();
        } catch (IOException ex) {
            perfiles.add(posicion, seleccionado);
            JOptionPane.showMessageDialog(this,
                    "No se pudo actualizar profiles.json: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alGuardarPerfil(Profile nuevo) {
        mostrandoNuevaTransaccion = false;
        cargarGrupos(nuevo.grupo(), nuevo);
        comboModo.setSelectedIndex(0);
        refrescar();
    }

    private void abrirConfiguracion() {
        JComboBox<String> comboTema = new JComboBox<>(new String[]{TemaPreferencias.OSCURO, TemaPreferencias.CLARO});
        comboTema.setSelectedItem(TemaPreferencias.obtenerTema());

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Tema:"), BorderLayout.WEST);
        panel.add(comboTema, BorderLayout.CENTER);

        int resultado = JOptionPane.showConfirmDialog(this, panel, "Configuración",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resultado != JOptionPane.OK_OPTION) {
            return;
        }

        String nuevoTema = (String) comboTema.getSelectedItem();
        TemaPreferencias.guardarTema(nuevoTema);

        try {
            if (TemaPreferencias.CLARO.equals(nuevoTema)) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
            pack();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo aplicar el tema: " + ex.getMessage());
        }
    }
}
