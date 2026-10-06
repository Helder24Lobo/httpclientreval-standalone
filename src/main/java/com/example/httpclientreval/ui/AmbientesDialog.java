package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Entorno;
import com.example.httpclientreval.model.Entornos;
import com.example.httpclientreval.model.SoapHttpClient;
import com.example.httpclientreval.util.Registro;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Administrador de ambientes: la lista (Pruebas, Producción y los que se creen) a la izquierda y, a la
 * derecha, el formulario del seleccionado con su URL, SOAPAction, timeout y credenciales (llave AES y
 * usuario/contraseña WSSE). Desde aquí se crean, duplican y eliminan ambientes, y se elige cuál está
 * activo. Un ambiente puede guardarse incompleto (p. ej. Producción mientras no hay credenciales), pero
 * solo se puede activar cuando está completo. Los cambios se guardan en entornos.json.
 */
final class AmbientesDialog extends JDialog {

    private static final String NOMBRE = "Nombre";
    private static final String URL = "URL del servicio";
    private static final String SOAP_ACTION = "SOAPAction";
    private static final String LLAVE = "Llave AES";
    private static final String USUARIO = "Usuario WSSE";
    private static final String PASSWORD = "Contraseña WSSE";

    private final Entornos entornos = Entornos.instancia();
    private final DefaultListModel<Entorno> modeloLista = new DefaultListModel<>();
    private final JList<Entorno> lista = new JList<>(modeloLista);
    private final Map<String, JTextField> campos = new LinkedHashMap<>();
    private final JSpinner timeout = new JSpinner(new SpinnerNumberModel(SoapHttpClient.TIMEOUT_POR_DEFECTO_SEGUNDOS,
            SoapHttpClient.TIMEOUT_MINIMO_SEGUNDOS, SoapHttpClient.TIMEOUT_MAXIMO_SEGUNDOS, 5));
    private final JCheckBox produccion = new JCheckBox("Es un ambiente de PRODUCCIÓN (pide confirmación antes de enviar)");
    private final StatusBanner banner = new StatusBanner();
    private final JButton guardar = new JButton("Guardar cambios", Icons.guardar());
    private final JButton activar = new JButton("Activar este ambiente", Icons.check());
    private final JButton duplicar = new JButton("Duplicar", Icons.duplicar());
    private final JButton eliminar = new JButton("Eliminar", Icons.limpiar());

    /** Nombre del ambiente cuyo formulario se está mostrando. */
    private String seleccionado;
    private boolean cargando;
    private boolean sucio;

    static void mostrar(Window propietario) {
        new AmbientesDialog(propietario).setVisible(true);
    }

    private AmbientesDialog(Window propietario) {
        super(propietario, "Ambientes", ModalityType.APPLICATION_MODAL);

        add(crearPanelLista(), BorderLayout.WEST);
        add(crearPanelFormulario(), BorderLayout.CENTER);
        add(crearPanelInferior(), BorderLayout.SOUTH);

        recargarLista(entornos.activo().nombre);

        setPreferredSize(new Dimension(960, 600));
        pack();
        setLocationRelativeTo(propietario);
    }

    // --- construcción de la ventana ---------------------------------------------------------------

    private JPanel crearPanelLista() {
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Accesibilidad.nombrar(lista, "Ambientes", "Lista de ambientes; el marcado con un punto es el activo");
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object valor, int indice,
                                                          boolean seleccionada, boolean conFoco) {
                Entorno e = (Entorno) valor;
                boolean esActivo = e.nombre.equalsIgnoreCase(entornos.activo().nombre);
                String texto = (esActivo ? "● " : "    ") + e.nombre
                        + (e.produccion ? "  — PRODUCCIÓN" : "")
                        + (e.listoParaUsar() ? "" : "  (incompleto)");
                Component c = super.getListCellRendererComponent(l, texto, indice, seleccionada, conFoco);
                if (!seleccionada && e.produccion) {
                    c.setForeground(new Color(214, 70, 70));
                }
                return c;
            }
        });
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !cargando) {
                alCambiarSeleccion();
            }
        });

        JButton nuevo = new JButton("Nuevo...", Icons.nuevo());
        nuevo.addActionListener(e -> crearNuevo());
        duplicar.addActionListener(e -> duplicarSeleccionado());
        eliminar.addActionListener(e -> eliminarSeleccionado());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        botones.add(nuevo);
        botones.add(duplicar);
        botones.add(eliminar);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 4));
        panel.add(new JLabel("Ambientes"), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setPreferredSize(new Dimension(250, 300));
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};

        FormFields.agregarCampo(formulario, gbc, fila, campos, NOMBRE, "");
        FormFields.agregarSeccion(formulario, gbc, fila, "Servicio");
        FormFields.agregarCampo(formulario, gbc, fila, campos, URL, "");
        FormFields.agregarCampo(formulario, gbc, fila, campos, SOAP_ACTION, "");

        JLabel etiquetaTimeout = new JLabel("Timeout (segundos):");
        Accesibilidad.etiquetar(etiquetaTimeout, timeout, "Timeout en segundos");
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.weightx = 0;
        formulario.add(etiquetaTimeout, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formulario.add(timeout, gbc);

        FormFields.agregarSeccion(formulario, gbc, fila, "Credenciales de este ambiente");
        FormFields.agregarCampoSecreto(formulario, gbc, fila, campos, LLAVE, "");
        FormFields.agregarCampo(formulario, gbc, fila, campos, USUARIO, "");
        FormFields.agregarCampoSecreto(formulario, gbc, fila, campos, PASSWORD, "");

        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.gridwidth = 2;
        formulario.add(produccion, gbc);
        gbc.gridwidth = 1;

        JLabel nota = new JLabel("<html><body style='width:430px'>Cada ambiente tiene sus propias credenciales: lo que "
                + "sirve en Pruebas no sirve en Producción. Un ambiente se puede guardar incompleto, pero solo se puede "
                + "activar cuando tiene URL, llave AES, usuario y contraseña.</body></html>");
        nota.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.gridwidth = 2;
        formulario.add(nota, gbc);

        // Cualquier edición marca el formulario como "con cambios sin guardar".
        DocumentListener alEscribir = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                marcarSucio();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                marcarSucio();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                marcarSucio();
            }
        };
        for (JTextField campo : campos.values()) {
            campo.getDocument().addDocumentListener(alEscribir);
        }
        timeout.addChangeListener(e -> marcarSucio());
        produccion.addItemListener(e -> marcarSucio());

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 10));
        JScrollPane scroll = FormFields.envolver(formulario);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelInferior() {
        guardar.addActionListener(e -> guardarCambios());
        activar.addActionListener(e -> activarSeleccionado());
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> cerrar());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.add(guardar);
        botones.add(activar);
        botones.add(cerrar);

        // El banner va en su propia fila, a todo el ancho: junto a los botones el texto largo no cabía y no se veía.
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        panel.add(banner, BorderLayout.CENTER);
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    // --- comportamiento ---------------------------------------------------------------------------

    private void marcarSucio() {
        if (!cargando) {
            sucio = true;
        }
    }

    /** Vuelve a llenar la lista desde {@link Entornos} y deja seleccionado (y cargado en el formulario) a {@code nombre}. */
    private void recargarLista(String nombre) {
        cargando = true;
        modeloLista.clear();
        Entorno aSeleccionar = null;
        for (Entorno e : entornos.lista()) {
            modeloLista.addElement(e);
            if (e.nombre.equalsIgnoreCase(nombre)) {
                aSeleccionar = e;
            }
        }
        if (aSeleccionar == null) {
            aSeleccionar = modeloLista.get(0);
        }
        lista.setSelectedValue(aSeleccionar, true);
        cargando = false;
        cargarFormulario(aSeleccionar);
    }

    private void alCambiarSeleccion() {
        Entorno nuevo = lista.getSelectedValue();
        if (nuevo == null) {
            return;
        }
        if (sucio && !confirmarDescartar()) {
            // Se queda donde estaba: se vuelve a seleccionar el anterior sin recargar su formulario.
            cargando = true;
            for (int i = 0; i < modeloLista.size(); i++) {
                if (modeloLista.get(i).nombre.equalsIgnoreCase(seleccionado)) {
                    lista.setSelectedIndex(i);
                }
            }
            cargando = false;
            return;
        }
        cargarFormulario(nuevo);
    }

    private boolean confirmarDescartar() {
        return JOptionPane.showConfirmDialog(this,
                "Hay cambios sin guardar en \"" + seleccionado + "\". ¿Descartarlos?",
                "Ambientes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void cargarFormulario(Entorno e) {
        cargando = true;
        seleccionado = e.nombre;
        campos.get(NOMBRE).setText(e.nombre);
        campos.get(URL).setText(e.url);
        campos.get(SOAP_ACTION).setText(e.soapAction);
        timeout.setValue(e.timeoutSegundos);
        campos.get(LLAVE).setText(e.llaveAes);
        campos.get(USUARIO).setText(e.wsseUsername);
        campos.get(PASSWORD).setText(e.wssePassword);
        produccion.setSelected(e.produccion);
        sucio = false;
        cargando = false;

        boolean esActivo = e.nombre.equalsIgnoreCase(entornos.activo().nombre);
        activar.setEnabled(!esActivo);
        eliminar.setEnabled(!esActivo && entornos.lista().size() > 1);
        setTitle("Ambientes — activo: " + entornos.activo().nombre);

        if (esActivo && !e.listoParaUsar()) {
            banner.mostrarError("Activo pero incompleto: falta " + String.join(", ", e.faltantes())
                    + ". Complétalo y guarda.");
        } else if (esActivo) {
            banner.mostrarInfo("Ambiente activo: los envíos van a " + e.url);
        } else if (!e.listoParaUsar()) {
            banner.mostrarInfo("Incompleto: falta " + String.join(", ", e.faltantes())
                    + ". Se guarda así, pero no se activa hasta completarlo.");
        } else {
            banner.ocultar();
        }
    }

    private Entorno leerFormulario() {
        Entorno e = new Entorno(campos.get(NOMBRE).getText());
        e.url = campos.get(URL).getText();
        e.soapAction = campos.get(SOAP_ACTION).getText();
        e.timeoutSegundos = (Integer) timeout.getValue();
        e.llaveAes = campos.get(LLAVE).getText();
        e.wsseUsername = campos.get(USUARIO).getText();
        e.wssePassword = campos.get(PASSWORD).getText();
        e.produccion = produccion.isSelected();
        return e;
    }

    /** @return {@code true} si se guardó. Si no, deja el motivo en el banner. */
    private boolean guardarCambios() {
        try {
            Entorno nuevo = leerFormulario();
            entornos.guardar(seleccionado, nuevo);
            Registro.advertencia("Ambiente \"" + nuevo.nombre.trim() + "\" guardado", null);
            recargarLista(nuevo.nombre.trim());
            banner.mostrarExito("Cambios guardados en \"" + nuevo.nombre.trim() + "\".");
            return true;
        } catch (IllegalArgumentException ex) {
            banner.mostrarError(ex.getMessage());
        } catch (IOException ex) {
            Registro.error("No se pudo guardar entornos.json", ex);
            banner.mostrarError("No se pudo guardar entornos.json: " + ex.getMessage());
        }
        return false;
    }

    private void activarSeleccionado() {
        // Lo que se ve en el formulario es lo que se activa: primero se guardan los cambios pendientes.
        if (sucio && !guardarCambios()) {
            return;
        }
        Entorno e = entornos.buscar(seleccionado);
        if (e.produccion) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "Vas a activar PRODUCCIÓN.\n\nDestino: " + e.url + "\n\nDesde ahora los envíos van a ese servicio. ¿Continuar?",
                    "Activar producción", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            entornos.activar(e.nombre);
            Registro.advertencia("Ambiente activo: \"" + e.nombre + "\"", null);
            recargarLista(e.nombre);
            banner.mostrarExito("Ambiente activo: \"" + e.nombre + "\".");
        } catch (IllegalArgumentException ex) {
            banner.mostrarError(ex.getMessage());
        } catch (IOException ex) {
            Registro.error("No se pudo activar el ambiente " + e.nombre, ex);
            banner.mostrarError("No se pudo guardar entornos.json: " + ex.getMessage());
        }
    }

    private void crearNuevo() {
        if (sucio && !confirmarDescartar()) {
            return;
        }
        Object nombre = JOptionPane.showInputDialog(this, "Nombre del nuevo ambiente (ej. Desarrollo, QA):",
                "Nuevo ambiente", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (nombre == null || ((String) nombre).isBlank()) {
            return;
        }
        guardarNuevo(new Entorno(((String) nombre).trim()), "creado");
    }

    private void duplicarSeleccionado() {
        if (sucio && !confirmarDescartar()) {
            return;
        }
        Entorno original = entornos.buscar(seleccionado);
        Entorno copia = original.copia();
        copia.nombre = nombreDisponible(original.nombre);
        // Una copia nunca nace como producción: hay que marcarla a propósito, igual que cualquier otro ambiente.
        copia.produccion = false;
        guardarNuevo(copia, "duplicado");
    }

    private void guardarNuevo(Entorno nuevo, String verbo) {
        try {
            entornos.guardar(null, nuevo);
            Registro.advertencia("Ambiente \"" + nuevo.nombre + "\" " + verbo, null);
            recargarLista(nuevo.nombre);
            banner.mostrarExito("Ambiente \"" + nuevo.nombre + "\" " + verbo + ". Completa sus datos y guárdalo.");
        } catch (IllegalArgumentException ex) {
            banner.mostrarError(ex.getMessage());
        } catch (IOException ex) {
            Registro.error("No se pudo guardar entornos.json", ex);
            banner.mostrarError("No se pudo guardar entornos.json: " + ex.getMessage());
        }
    }

    private String nombreDisponible(String base) {
        String candidato = base + " (copia)";
        for (int n = 2; entornos.buscar(candidato) != null; n++) {
            candidato = base + " (copia " + n + ")";
        }
        return candidato;
    }

    private void eliminarSeleccionado() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el ambiente \"" + seleccionado + "\" con sus credenciales?\nEsta acción no se puede deshacer.",
                "Eliminar ambiente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            entornos.eliminar(seleccionado);
            Registro.advertencia("Ambiente \"" + seleccionado + "\" eliminado", null);
            recargarLista(entornos.activo().nombre);
        } catch (IllegalArgumentException ex) {
            banner.mostrarError(ex.getMessage());
        } catch (IOException ex) {
            Registro.error("No se pudo guardar entornos.json", ex);
            banner.mostrarError("No se pudo guardar entornos.json: " + ex.getMessage());
        }
    }

    private void cerrar() {
        if (sucio && !confirmarDescartar()) {
            return;
        }
        dispose();
    }
}
