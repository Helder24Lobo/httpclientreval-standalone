package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;
import com.formdev.flatlaf.FlatLaf;

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
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
 * profiles.json), y "Configuración" cambia el tema (claro, oscuro o el del sistema) en
 * caliente. El tamaño y la posición de la ventana se recuerdan entre corridas.
 */
public class AppWindow extends JFrame {

    /** Grupos virtuales al inicio del combo de grupos; solo aparecen si tienen al menos un perfil. */
    private static final String GRUPO_FAVORITOS = "★ Favoritos";
    private static final String GRUPO_RECIENTES = "◷ Recientes";

    private final PerfilesPreferencias preferencias = PerfilesPreferencias.instancia();
    private final JButton botonFavorito = new JButton();
    private HistorialDialog historialDialog;
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
        // En Favoritos/Recientes se mezclan grupos, así que ahí se muestra el nombre completo.
        comboPerfil.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object texto = value;
                if (value instanceof Profile) {
                    Profile p = (Profile) value;
                    texto = esGrupoVirtual(comboGrupo.getSelectedItem()) ? p.nombre : p.detalle();
                }
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
            registrarReciente();
        });
        comboPerfil.addActionListener(e -> {
            if (actualizandoCombos) {
                return;
            }
            mostrandoNuevaTransaccion = false;
            refrescar();
            registrarReciente();
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

        botonFavorito.addActionListener(e -> alternarFavorito());
        Atajos.registrar(getRootPane(), Atajos.FAVORITO, this::alternarFavorito);

        JButton renombrarPerfil = new JButton("Renombrar", Icons.editar());
        renombrarPerfil.addActionListener(e -> renombrarPerfilSeleccionado());

        JButton eliminarPerfil = new JButton("Eliminar perfil", Icons.limpiar());
        eliminarPerfil.addActionListener(e -> eliminarPerfilSeleccionado());

        JButton buscarPerfil = new JButton("Buscar", Icons.buscar());
        buscarPerfil.setToolTipText("Buscar perfil (" + Atajos.texto(Atajos.BUSCAR) + ")");
        buscarPerfil.addActionListener(e -> buscarPerfil());
        Atajos.registrar(getRootPane(), Atajos.BUSCAR, this::buscarPerfil);

        JButton historialEnvios = new JButton("Historial", Icons.historial());
        historialEnvios.setToolTipText("Historial de envíos de la sesión (" + Atajos.texto(Atajos.HISTORIAL) + ")");
        historialEnvios.addActionListener(e -> abrirHistorial());
        Atajos.registrar(getRootPane(), Atajos.HISTORIAL, this::abrirHistorial);

        JButton configuracion = new JButton("Configuración", Icons.configuracion());
        configuracion.addActionListener(e -> abrirConfiguracion());

        JPanel filaTransaccion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaTransaccion.add(new JLabel("Grupo:"));
        filaTransaccion.add(comboGrupo);
        filaTransaccion.add(new JLabel("Transacción:"));
        filaTransaccion.add(comboPerfil);
        filaTransaccion.add(botonFavorito);
        filaTransaccion.add(renombrarPerfil);
        filaTransaccion.add(eliminarPerfil);

        JPanel filaAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaAcciones.add(new JLabel("Modo:"));
        filaAcciones.add(comboModo);
        filaAcciones.add(nuevaTransaccion);
        filaAcciones.add(buscarPerfil);
        filaAcciones.add(historialEnvios);
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
        if (!VentanaPreferencias.restaurarYRecordar(this)) {
            setLocationRelativeTo(null);
        }

        // Con "Seguir el sistema", el tema del SO puede cambiar con la app
        // abierta; se vuelve a consultar cada vez que la ventana recupera el foco.
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                sincronizarConSistema();
            }
        });
    }

    private static boolean esGrupoVirtual(Object grupo) {
        return GRUPO_FAVORITOS.equals(grupo) || GRUPO_RECIENTES.equals(grupo);
    }

    /** Perfiles de un grupo: los de Favoritos/Recientes salen de las preferencias (ignorando los que ya no existen). */
    private List<Profile> perfilesDelGrupo(String grupo) {
        List<Profile> resultado = new ArrayList<>();
        if (esGrupoVirtual(grupo)) {
            List<String> nombres = GRUPO_FAVORITOS.equals(grupo) ? preferencias.favoritos() : preferencias.recientes();
            for (String nombre : nombres) {
                for (Profile p : perfiles) {
                    if (nombre.equals(p.nombre)) {
                        resultado.add(p);
                        break;
                    }
                }
            }
        } else {
            for (Profile p : perfiles) {
                if (p.grupo().equals(grupo)) {
                    resultado.add(p);
                }
            }
        }
        return resultado;
    }

    /**
     * Rellena el combo de grupos y deja seleccionado el indicado. Primero van
     * Favoritos y Recientes (si tienen algo) y después los grupos reales, en
     * orden de aparición. Si el perfil pedido no está en el grupo elegido (p.
     * ej. se acaba de quitar de favoritos), se muestra en su grupo real.
     */
    private void cargarGrupos(String grupoASeleccionar, Profile perfilASeleccionar) {
        actualizandoCombos = true;
        try {
            Set<String> grupos = new LinkedHashSet<>();
            if (!perfilesDelGrupo(GRUPO_FAVORITOS).isEmpty()) {
                grupos.add(GRUPO_FAVORITOS);
            }
            if (!perfilesDelGrupo(GRUPO_RECIENTES).isEmpty()) {
                grupos.add(GRUPO_RECIENTES);
            }
            for (Profile p : perfiles) {
                grupos.add(p.grupo());
            }
            String elegido = grupos.contains(grupoASeleccionar) ? grupoASeleccionar : grupos.iterator().next();
            if (perfilASeleccionar != null && !perfilesDelGrupo(elegido).contains(perfilASeleccionar)) {
                elegido = perfilASeleccionar.grupo();
            }
            comboGrupo.removeAllItems();
            for (String g : grupos) {
                comboGrupo.addItem(g);
            }
            comboGrupo.setSelectedItem(elegido);
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
            List<Profile> delGrupo = perfilesDelGrupo(grupo);
            for (Profile p : delGrupo) {
                modeloPerfiles.addElement(p);
            }
            Profile elegido = perfilASeleccionar != null && delGrupo.contains(perfilASeleccionar)
                    ? perfilASeleccionar : (delGrupo.isEmpty() ? null : delGrupo.get(0));
            modeloPerfiles.setSelectedItem(elegido);
        } finally {
            actualizandoCombos = false;
        }
    }

    /** Si estamos viendo Favoritos/Recientes se queda ahí; si no, en el grupo real del perfil. */
    private String grupoParaMantener(Profile perfil) {
        Object actual = comboGrupo.getSelectedItem();
        if (esGrupoVirtual(actual)) {
            return (String) actual;
        }
        return perfil != null ? perfil.grupo() : (String) actual;
    }

    /** Recarga los combos (p. ej. tras cambiar favoritos o recientes) sin tocar el panel ni lo que haya escrito. */
    private void sincronizarCombos() {
        Profile actual = (Profile) comboPerfil.getSelectedItem();
        cargarGrupos(grupoParaMantener(actual), actual);
        actualizarBotonFavorito();
    }

    /** Anota como reciente el perfil que el usuario acaba de abrir (no al elegir dentro de Recientes, para no reordenarla). */
    private void registrarReciente() {
        Profile actual = (Profile) comboPerfil.getSelectedItem();
        if (actual == null || GRUPO_RECIENTES.equals(comboGrupo.getSelectedItem())) {
            return;
        }
        preferencias.registrarReciente(actual.nombre);
        sincronizarCombos();
    }

    private void alternarFavorito() {
        Profile actual = (Profile) comboPerfil.getSelectedItem();
        if (actual == null) {
            return;
        }
        preferencias.alternarFavorito(actual.nombre);
        sincronizarCombos();
    }

    private void actualizarBotonFavorito() {
        Profile actual = (Profile) comboPerfil.getSelectedItem();
        boolean favorito = actual != null && preferencias.esFavorito(actual.nombre);
        botonFavorito.setEnabled(actual != null);
        botonFavorito.setIcon(favorito ? Icons.favorito() : Icons.favoritoVacio());
        botonFavorito.setToolTipText((favorito ? "Quitar de favoritos" : "Marcar como favorito")
                + " (" + Atajos.texto(Atajos.FAVORITO) + ")");
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
        actualizarBotonFavorito();
    }

    /** Abre el buscador rápido y salta al perfil elegido, manteniendo el modo (Cifrar/Descifrar) actual. */
    private void buscarPerfil() {
        new BuscadorPerfiles(this, perfiles, preferencias, elegido -> {
            mostrandoNuevaTransaccion = false;
            cargarGrupos(elegido.grupo(), elegido);
            refrescar();
            preferencias.registrarReciente(elegido.nombre);
        }).setVisible(true);
        // Aunque no se elija nada, en el buscador pudieron marcarse o quitarse favoritos.
        sincronizarCombos();
    }

    /** Abre (o trae al frente) el historial de envíos; es una sola ventana que se reutiliza. */
    private void abrirHistorial() {
        if (historialDialog == null) {
            historialDialog = new HistorialDialog(this);
        }
        historialDialog.setVisible(true);
        historialDialog.toFront();
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
            preferencias.renombrar(nombreAnterior, nuevoNombre);
            // El grupo puede haber cambiado; se recargan los combos sin refrescar el panel
            // para no perder lo que haya escrito en el formulario.
            cargarGrupos(grupoParaMantener(seleccionado), seleccionado);
            actualizarBotonFavorito();
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
            preferencias.eliminar(seleccionado.nombre);
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
        JComboBox<String> comboTema = new JComboBox<>(new String[]{
                TemaPreferencias.SISTEMA, TemaPreferencias.OSCURO, TemaPreferencias.CLARO});
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
        aplicarTema(TemaPreferencias.esOscuro(nuevoTema));
    }

    /** Si el tema es "Seguir el sistema", consulta el SO (fuera del EDT) y cambia el tema si no coincide. */
    private void sincronizarConSistema() {
        if (!TemaPreferencias.SISTEMA.equals(TemaPreferencias.obtenerTema())) {
            return;
        }
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return TemaPreferencias.sistemaEnOscuro();
            }

            @Override
            protected void done() {
                try {
                    // Se revalida: el usuario pudo cambiar el tema mientras se consultaba el SO.
                    if (TemaPreferencias.SISTEMA.equals(TemaPreferencias.obtenerTema())) {
                        aplicarTema(get());
                    }
                } catch (Exception ignorada) {
                    // Si no se pudo consultar el SO se deja el tema actual.
                }
            }
        }.execute();
    }

    private void aplicarTema(boolean oscuro) {
        if (oscuro == FlatLaf.isLafDark()) {
            return;
        }
        try {
            TemaPreferencias.instalar(oscuro);
            // Refresca todas las ventanas abiertas sin pack(), para respetar el tamaño elegido por el usuario.
            FlatLaf.updateUI();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo aplicar el tema: " + ex.getMessage());
        }
    }
}
