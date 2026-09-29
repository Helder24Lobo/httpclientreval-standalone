package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.model.SoapHttpClient;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

        JButton duplicarPerfil = new JButton("Duplicar", Icons.duplicar());
        duplicarPerfil.setToolTipText("Crea una copia del perfil seleccionado, lista para editar o renombrar");
        duplicarPerfil.addActionListener(e -> duplicarPerfilSeleccionado());

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

        // Tamaño de fuente con el teclado (como el zoom del navegador): Ctrl + / Ctrl - / Ctrl 0.
        for (KeyStroke mas : new KeyStroke[]{Atajos.AUMENTAR_FUENTE, Atajos.AUMENTAR_FUENTE_MAS, Atajos.AUMENTAR_FUENTE_NUM}) {
            Atajos.registrar(getRootPane(), mas, () -> cambiarTamanoFuente(FuentePreferencias.desplazar(FuentePreferencias.obtener(), 1)));
        }
        for (KeyStroke menos : new KeyStroke[]{Atajos.REDUCIR_FUENTE, Atajos.REDUCIR_FUENTE_NUM}) {
            Atajos.registrar(getRootPane(), menos, () -> cambiarTamanoFuente(FuentePreferencias.desplazar(FuentePreferencias.obtener(), -1)));
        }
        Atajos.registrar(getRootPane(), Atajos.RESTABLECER_FUENTE, () -> cambiarTamanoFuente(FuentePreferencias.POR_DEFECTO));

        // Cada combo con su etiqueta asociada: el lector de pantalla anuncia "Grupo", "Transacción" o "Modo" al enfocarlos.
        JLabel etiquetaGrupo = new JLabel("Grupo:");
        Accesibilidad.etiquetar(etiquetaGrupo, comboGrupo, "Grupo");
        JLabel etiquetaTransaccion = new JLabel("Transacción:");
        Accesibilidad.etiquetar(etiquetaTransaccion, comboPerfil, "Transacción");
        JLabel etiquetaModo = new JLabel("Modo:");
        Accesibilidad.etiquetar(etiquetaModo, comboModo, "Modo");

        JPanel filaTransaccion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaTransaccion.add(etiquetaGrupo);
        filaTransaccion.add(comboGrupo);
        filaTransaccion.add(etiquetaTransaccion);
        filaTransaccion.add(comboPerfil);
        filaTransaccion.add(botonFavorito);
        filaTransaccion.add(duplicarPerfil);
        filaTransaccion.add(renombrarPerfil);
        filaTransaccion.add(eliminarPerfil);

        JPanel filaAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaAcciones.add(etiquetaModo);
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
        String accion = favorito ? "Quitar de favoritos" : "Marcar como favorito";
        botonFavorito.setToolTipText(accion + " (" + Atajos.texto(Atajos.FAVORITO) + ")");
        // Botón solo con icono (estrella llena o vacía): el nombre accesible dice qué hace y en qué estado está.
        Accesibilidad.nombrar(botonFavorito, actual != null ? accion + ": " + actual.nombre : accion);
    }

    private void refrescar() {
        centro.removeAll();

        if (mostrandoNuevaTransaccion) {
            centro.add(new NewProfilePanel(archivoPerfiles, perfiles, this::alGuardarPerfil, this::alCancelarNuevaTransaccion),
                    BorderLayout.CENTER);
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
        if (existeNombre(seleccionado, nuevoNombre)) {
            JOptionPane.showMessageDialog(this, "Ya existe un perfil con ese nombre.",
                    "Renombrar perfil", JOptionPane.WARNING_MESSAGE);
            return;
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

    /** true si algún OTRO perfil (no {@code excepto}) ya tiene ese nombre, sin distinguir mayúsculas. */
    private boolean existeNombre(Profile excepto, String nombre) {
        for (Profile otro : perfiles) {
            if (otro != excepto && otro.nombre.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Crea una copia exacta del perfil seleccionado (llave AES, credenciales del WS y todos los
     * defaults) con un nombre disponible ("... (copia)", "... (copia 2)"...) y queda seleccionada,
     * lista para renombrar o editar sin haber tenido que llenar el formulario de "Nueva transacción" a mano.
     */
    private void duplicarPerfilSeleccionado() {
        Profile original = (Profile) comboPerfil.getSelectedItem();
        if (original == null) {
            return;
        }

        Profile copia = original.copia();
        copia.nombre = nombreDuplicadoDisponible(original.nombre);

        int posicion = perfiles.indexOf(original);
        perfiles.add(posicion + 1, copia);
        try {
            Profile.saveAll(archivoPerfiles, perfiles);
            mostrandoNuevaTransaccion = false;
            cargarGrupos(copia.grupo(), copia);
            refrescar();
        } catch (IOException ex) {
            perfiles.remove(copia);
            JOptionPane.showMessageDialog(this,
                    "No se pudo actualizar profiles.json: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String nombreDuplicadoDisponible(String nombreOriginal) {
        String candidato = nombreOriginal + " (copia)";
        for (int n = 2; existeNombre(null, candidato); n++) {
            candidato = nombreOriginal + " (copia " + n + ")";
        }
        return candidato;
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

    /** Sale de "Nueva transacción" sin guardar y vuelve a la pantalla principal (perfil y modo que estaban elegidos). */
    private void alCancelarNuevaTransaccion() {
        mostrandoNuevaTransaccion = false;
        refrescar();
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

        JComboBox<String> comboFuente = new JComboBox<>();
        for (int porcentaje : FuentePreferencias.PORCENTAJES) {
            comboFuente.addItem(porcentaje + " %");
        }
        comboFuente.setSelectedItem(FuentePreferencias.obtener() + " %");

        JLabel etiquetaTema = new JLabel("Tema:");
        Accesibilidad.etiquetar(etiquetaTema, comboTema, "Tema");
        JLabel etiquetaFuente = new JLabel("Tamaño de fuente:");
        Accesibilidad.etiquetar(etiquetaFuente, comboFuente, "Tamaño de fuente");
        comboFuente.setToolTipText("También con " + Atajos.texto(Atajos.AUMENTAR_FUENTE) + " / "
                + Atajos.texto(Atajos.REDUCIR_FUENTE) + " y " + Atajos.texto(Atajos.RESTABLECER_FUENTE) + " para restablecer");

        // Entorno del servicio: URL, SOAPAction y timeout, para poder apuntar a producción u otro
        // ambiente (o ajustar el tiempo de espera) sin recompilar la app.
        JTextField campoUrl = new JTextField(SoapHttpClient.getUrl(), 28);
        JTextField campoSoapAction = new JTextField(SoapHttpClient.getSoapAction(), 28);
        JSpinner campoTimeout = new JSpinner(new SpinnerNumberModel(SoapHttpClient.getTimeoutSegundos(),
                SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS, SoapHttpClient.TIMEOUT_MAXIMO_SEGUNDOS, 5));

        JLabel etiquetaUrl = new JLabel("URL del servicio:");
        Accesibilidad.etiquetar(etiquetaUrl, campoUrl, "URL del servicio");
        JLabel etiquetaSoapAction = new JLabel("SOAPAction:");
        Accesibilidad.etiquetar(etiquetaSoapAction, campoSoapAction, "SOAPAction");
        JLabel etiquetaTimeout = new JLabel("Timeout (segundos):");
        Accesibilidad.etiquetar(etiquetaTimeout, campoTimeout, "Timeout en segundos");

        JButton restablecerEntorno = new JButton("Restablecer al ambiente de pruebas");
        restablecerEntorno.addActionListener(e -> {
            campoUrl.setText(SoapHttpClient.URL_POR_DEFECTO);
            campoSoapAction.setText(SoapHttpClient.SOAP_ACTION_POR_DEFECTO);
            campoTimeout.setValue(SoapHttpClient.TIMEOUT_POR_DEFECTO_SEGUNDOS);
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};
        gbc.gridy = fila[0]++;
        gbc.gridx = 0;
        panel.add(etiquetaTema, gbc);
        gbc.gridx = 1;
        panel.add(comboTema, gbc);
        gbc.gridy = fila[0]++;
        gbc.gridx = 0;
        panel.add(etiquetaFuente, gbc);
        gbc.gridx = 1;
        panel.add(comboFuente, gbc);

        FormFields.agregarSeccion(panel, gbc, fila, "Entorno del servicio");
        gbc.gridy = fila[0]++;
        gbc.gridx = 0;
        panel.add(etiquetaUrl, gbc);
        gbc.gridx = 1;
        panel.add(campoUrl, gbc);
        gbc.gridy = fila[0]++;
        gbc.gridx = 0;
        panel.add(etiquetaSoapAction, gbc);
        gbc.gridx = 1;
        panel.add(campoSoapAction, gbc);
        gbc.gridy = fila[0]++;
        gbc.gridx = 0;
        panel.add(etiquetaTimeout, gbc);
        gbc.gridx = 1;
        panel.add(campoTimeout, gbc);
        gbc.gridy = fila[0]++;
        gbc.gridx = 1;
        panel.add(restablecerEntorno, gbc);

        // En bucle: si la URL no es válida se avisa y se vuelve a mostrar el mismo panel (con lo que
        // ya se había escrito) en vez de cerrar la ventana como si se hubiera guardado.
        while (true) {
            int resultado = JOptionPane.showConfirmDialog(this, panel, "Configuración",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resultado != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                SoapHttpClient.setUrl(campoUrl.getText());
                SoapHttpClient.setSoapAction(campoSoapAction.getText());
                SoapHttpClient.setTimeoutSegundos((Integer) campoTimeout.getValue());
                break;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Entorno del servicio",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        String nuevoTema = (String) comboTema.getSelectedItem();
        TemaPreferencias.guardarTema(nuevoTema);
        int nuevaFuente = FuentePreferencias.PORCENTAJES[comboFuente.getSelectedIndex()];
        if (nuevaFuente != FuentePreferencias.obtener()) {
            // Cambia el tema y la fuente en una sola pasada (una sola reinstalación del look and feel).
            FuentePreferencias.guardar(nuevaFuente);
            reinstalarTema(TemaPreferencias.esOscuro(nuevoTema));
        } else {
            aplicarTema(TemaPreferencias.esOscuro(nuevoTema));
        }
    }

    /** Cambia el tamaño de fuente de toda la interfaz (se recuerda para la próxima vez). */
    private void cambiarTamanoFuente(int porcentaje) {
        if (porcentaje == FuentePreferencias.obtener()) {
            return;
        }
        FuentePreferencias.guardar(porcentaje);
        reinstalarTema(FlatLaf.isLafDark());
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
        reinstalarTema(oscuro);
    }

    /** Reinstala el look and feel (con el tema y el tamaño de fuente guardados) y refresca todas las ventanas abiertas. */
    private void reinstalarTema(boolean oscuro) {
        try {
            TemaPreferencias.instalar(oscuro);
            FlatLaf.updateUI();
            ajustarTamanoVentana();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo aplicar el tema: " + ex.getMessage());
        }
    }

    /**
     * Con letra más grande los controles de arriba pueden necesitar más espacio. Se agranda la ventana solo si hace
     * falta (nunca se encoge) para no deshacer el tamaño que eligió el usuario, como haría pack().
     */
    private void ajustarTamanoVentana() {
        if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
            return;
        }
        Dimension preferido = getPreferredSize();
        setSize(Math.max(getWidth(), preferido.width), Math.max(getHeight(), preferido.height));
    }
}
