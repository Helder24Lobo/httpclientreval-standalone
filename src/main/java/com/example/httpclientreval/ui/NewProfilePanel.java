package com.example.httpclientreval.ui;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.model.MensajeNegocio;
import com.example.httpclientreval.model.Profile;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Panel "+ Nueva transacción": registra un perfil nuevo (llave, credenciales
 * del WS, defaults del sobre y del mensaje de negocio) y lo agrega a
 * profiles.json, para no tener que editar el archivo ni el código cada vez
 * que llega una transacción nueva.
 */
public class NewProfilePanel extends JPanel {

    private final Map<String, JTextField> campos = new LinkedHashMap<>();
    private final StatusBanner statusBanner = new StatusBanner();
    private TablaClaveValor tablaMensaje;
    private final Path archivoPerfiles;
    private final List<Profile> perfiles;
    private final Consumer<Profile> alGuardar;
    private final String llaveAesDefault;
    private final String wsseUsernameDefault;
    private final String wssePasswordDefault;

    /** {@code alCancelar} se invoca al pulsar la X de arriba a la derecha: el llamador vuelve a la pantalla principal. */
    public NewProfilePanel(Path archivoPerfiles, List<Profile> perfiles, Consumer<Profile> alGuardar,
                           Runnable alCancelar) {
        super(new BorderLayout(8, 8));
        this.archivoPerfiles = archivoPerfiles;
        this.perfiles = perfiles;
        this.alGuardar = alGuardar;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // La llave AES y las credenciales del WS son las mismas para todos los
        // perfiles existentes; se precargan del primero para no reescribirlas
        // cada vez que se registra una transacción nueva.
        Profile referencia = perfiles.isEmpty() ? null : perfiles.get(0);
        llaveAesDefault = referencia != null && referencia.llaveAes != null ? referencia.llaveAes : "";
        wsseUsernameDefault = referencia != null && referencia.wsseUsername != null ? referencia.wsseUsername : "";
        wssePasswordDefault = referencia != null && referencia.wssePassword != null ? referencia.wssePassword : "";

        MensajeNegocio.BodyMensaje bodyDefaults = new MensajeNegocio.BodyMensaje();
        MensajeNegocio.HeaderMensaje headerDefaults = new MensajeNegocio.HeaderMensaje();

        JTabbedPane tabs = new JTabbedPane();
        Accesibilidad.nombrar(tabs, "Datos de la nueva transacción");
        tabs.addTab("Datos generales", crearPanelGeneral());
        tabs.addTab("Mensaje de negocio", crearPanelMensaje(bodyDefaults, headerDefaults));

        JButton guardar = new JButton("Guardar perfil", Icons.guardar());
        guardar.addActionListener(e -> guardar());

        JButton limpiar = new JButton("Limpiar", Icons.limpiar());
        limpiar.addActionListener(e -> limpiarCampos());

        guardar.setToolTipText(Atajos.texto(Atajos.ENVIAR));
        limpiar.setToolTipText(Atajos.texto(Atajos.LIMPIAR));
        Atajos.registrar(this, Atajos.ENVIAR, this::guardar);
        Atajos.registrar(this, Atajos.LIMPIAR, this::limpiarCampos);

        JPanel botones = new JPanel();
        botones.add(guardar);
        botones.add(limpiar);

        JPanel accion = new JPanel(new BorderLayout(8, 0));
        accion.add(statusBanner, BorderLayout.CENTER);
        accion.add(botones, BorderLayout.EAST);

        JLabel titulo = new JLabel("Nueva transacción");
        titulo.putClientProperty(FlatClientProperties.STYLE, "font: bold +2");

        JButton cancelar = new JButton(Icons.cancelar());
        cancelar.setToolTipText("Cancelar y volver a la pantalla principal");
        Accesibilidad.nombrar(cancelar, "Cancelar nueva transacción");
        cancelar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        cancelar.addActionListener(e -> alCancelar.run());

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(cancelar, BorderLayout.EAST);

        add(encabezado, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(accion, BorderLayout.SOUTH);
    }

    private JScrollPane crearPanelGeneral() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};
        FormFields.agregarCampo(panel, gbc, fila, campos, "Nombre", "");
        FormFields.agregarCampoSecreto(panel, gbc, fila, campos, "LlaveAes", llaveAesDefault);
        FormFields.agregarCampo(panel, gbc, fila, campos, "WsseUsername", wsseUsernameDefault);
        FormFields.agregarCampoSecreto(panel, gbc, fila, campos, "WssePassword", wssePasswordDefault);
        FormFields.agregarSeccion(panel, gbc, fila, "Datos del sobre (_header)");
        FormFields.agregarCampo(panel, gbc, fila, campos, "IdCliente", "");
        FormFields.agregarCampo(panel, gbc, fila, campos, "IdTransaccion (sobre)", "");
        FormFields.agregarCampo(panel, gbc, fila, campos, "IpCliente", "172.17.0.4");

        FormFields.aplicarValidacionNumerica(campos.get("IdCliente"));
        FormFields.aplicarValidacionNumerica(campos.get("IdTransaccion (sobre)"));
        return FormFields.envolver(panel);
    }

    /**
     * Editor de tabla clave-valor para bodyMensajeDefault/headerMensajeDefault: los nombres de campo
     * son fijos (los define el WS, ver {@link MensajeNegocio}), pero en vez de un formulario de ~30
     * campos apilados se editan en una sola tabla compacta de dos columnas.
     */
    private TablaClaveValor crearPanelMensaje(MensajeNegocio.BodyMensaje b, MensajeNegocio.HeaderMensaje h) {
        LinkedHashMap<String, String> valores = new LinkedHashMap<>();
        valores.put("Autorizacion", b.autorizacion);
        valores.put("CodBarras", b.codBarras);
        valores.put("Convenio", b.convenio);
        valores.put("FechaVencimiento", b.fechaVencimiento);
        valores.put("Iac", b.iac);
        valores.put("IdPersona", b.idPersona);
        valores.put("IdTransaccion (negocio)", b.idTransaccion);
        valores.put("NoIdentificacionUsuario", b.noIdentificacionUsuario);
        valores.put("NombreUsuario", b.nombreUsuario);
        valores.put("NumCelular", b.numCelular);
        valores.put("Observacion", b.observacion);
        valores.put("Otp", b.otp);
        valores.put("TipoIdentificacion", b.tipoIdentificacion);
        valores.put("Valor", b.valor);
        valores.put("NoIdentificacionCajero", h.noIdentificacionCajero);
        valores.put("Referencia1", b.referencia1);
        valores.put("Referencia2", b.referencia2);
        valores.put("Referencia3", b.referencia3);
        valores.put("Referencia4", b.referencia4);
        valores.put("Referencia5", b.referencia5);
        valores.put("Referencia6", b.referencia6);
        valores.put("Referencia7", b.referencia7);
        valores.put("Referencia8", b.referencia8);
        valores.put("Referencia9", b.referencia9);
        valores.put("Referencia10", b.referencia10);
        valores.put("Referencia11", b.referencia11);
        valores.put("Referencia12", b.referencia12);
        valores.put("Referencia13", b.referencia13);
        valores.put("Referencia14", b.referencia14);
        valores.put("Referencia15", b.referencia15);

        tablaMensaje = new TablaClaveValor("Mensaje de negocio", valores);
        return tablaMensaje;
    }

    private void guardar() {
        try {
            String nombre = campos.get("Nombre").getText().trim();
            String llaveAes = campos.get("LlaveAes").getText().trim();

            if (nombre.isEmpty()) {
                mostrarError("El nombre no puede quedar vacío.");
                return;
            }
            for (Profile existente : perfiles) {
                if (existente.nombre.equalsIgnoreCase(nombre)) {
                    mostrarError("Ya existe un perfil con ese nombre.");
                    return;
                }
            }
            AES256CBC.validarLlave(llaveAes);

            boolean clienteOk = FormFields.esEnteroValido(campos.get("IdCliente"));
            boolean transaccionOk = FormFields.esEnteroValido(campos.get("IdTransaccion (sobre)"));
            if (!clienteOk || !transaccionOk) {
                mostrarError("IdCliente e IdTransaccion (sobre) deben ser números enteros válidos.");
                return;
            }

            int idCliente = Integer.parseInt(campos.get("IdCliente").getText().trim());
            int idTransaccionSobre = Integer.parseInt(campos.get("IdTransaccion (sobre)").getText().trim());

            Profile perfil = new Profile();
            perfil.nombre = nombre;
            perfil.llaveAes = llaveAes;
            perfil.idClienteDefault = idCliente;
            perfil.idTransaccionDefault = idTransaccionSobre;
            perfil.ipClienteDefault = campos.get("IpCliente").getText().trim();
            perfil.wsseUsername = campos.get("WsseUsername").getText();
            perfil.wssePassword = campos.get("WssePassword").getText();

            MensajeNegocio.BodyMensaje body = new MensajeNegocio.BodyMensaje();
            body.autorizacion = tablaMensaje.obtener("Autorizacion");
            body.codBarras = tablaMensaje.obtener("CodBarras");
            body.convenio = tablaMensaje.obtener("Convenio");
            body.fechaVencimiento = tablaMensaje.obtener("FechaVencimiento");
            body.iac = tablaMensaje.obtener("Iac");
            body.idPersona = tablaMensaje.obtener("IdPersona");
            body.idTransaccion = tablaMensaje.obtener("IdTransaccion (negocio)");
            body.noIdentificacionUsuario = tablaMensaje.obtener("NoIdentificacionUsuario");
            body.nombreUsuario = tablaMensaje.obtener("NombreUsuario");
            body.numCelular = tablaMensaje.obtener("NumCelular");
            body.observacion = tablaMensaje.obtener("Observacion");
            body.otp = tablaMensaje.obtener("Otp");
            body.referencia1 = tablaMensaje.obtener("Referencia1");
            body.referencia2 = tablaMensaje.obtener("Referencia2");
            body.referencia3 = tablaMensaje.obtener("Referencia3");
            body.referencia4 = tablaMensaje.obtener("Referencia4");
            body.referencia5 = tablaMensaje.obtener("Referencia5");
            body.referencia6 = tablaMensaje.obtener("Referencia6");
            body.referencia7 = tablaMensaje.obtener("Referencia7");
            body.referencia8 = tablaMensaje.obtener("Referencia8");
            body.referencia9 = tablaMensaje.obtener("Referencia9");
            body.referencia10 = tablaMensaje.obtener("Referencia10");
            body.referencia11 = tablaMensaje.obtener("Referencia11");
            body.referencia12 = tablaMensaje.obtener("Referencia12");
            body.referencia13 = tablaMensaje.obtener("Referencia13");
            body.referencia14 = tablaMensaje.obtener("Referencia14");
            body.referencia15 = tablaMensaje.obtener("Referencia15");
            body.tipoIdentificacion = tablaMensaje.obtener("TipoIdentificacion");
            body.valor = tablaMensaje.obtener("Valor");
            perfil.bodyMensajeDefault = body;

            MensajeNegocio.HeaderMensaje header = new MensajeNegocio.HeaderMensaje();
            header.noIdentificacionCajero = tablaMensaje.obtener("NoIdentificacionCajero");
            perfil.headerMensajeDefault = header;

            perfiles.add(perfil);
            Profile.saveAll(archivoPerfiles, perfiles);

            mostrarExito("Perfil \"" + nombre + "\" guardado correctamente.");
            alGuardar.accept(perfil);
        } catch (NumberFormatException ex) {
            mostrarError("IdCliente e IdTransaccion (sobre) deben ser números enteros.");
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        } catch (IOException ex) {
            mostrarError("No se pudo guardar profiles.json: " + ex.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        statusBanner.mostrarError(mensaje);
    }

    private void mostrarExito(String mensaje) {
        statusBanner.mostrarExito(mensaje);
    }

    private void limpiarCampos() {
        for (Map.Entry<String, JTextField> entrada : campos.entrySet()) {
            entrada.getValue().setText("");
        }
        campos.get("LlaveAes").setText(llaveAesDefault);
        campos.get("WsseUsername").setText(wsseUsernameDefault);
        campos.get("WssePassword").setText(wssePasswordDefault);
        campos.get("IpCliente").setText("172.17.0.4");
        tablaMensaje.restablecer();
        statusBanner.ocultar();
    }
}
