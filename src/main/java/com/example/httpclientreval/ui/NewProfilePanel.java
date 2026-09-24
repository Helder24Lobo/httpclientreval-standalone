package com.example.httpclientreval.ui;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.model.MensajeNegocio;
import com.example.httpclientreval.model.Profile;

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
    private final Path archivoPerfiles;
    private final List<Profile> perfiles;
    private final Consumer<Profile> alGuardar;
    private final String llaveAesDefault;
    private final String wsseUsernameDefault;
    private final String wssePasswordDefault;

    public NewProfilePanel(Path archivoPerfiles, List<Profile> perfiles, Consumer<Profile> alGuardar) {
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
        tabs.addTab("Datos generales", crearPanelGeneral());
        tabs.addTab("Mensaje de negocio", crearPanelMensaje(bodyDefaults, headerDefaults));
        tabs.addTab("Referencias", crearPanelReferencias(bodyDefaults));

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

        add(tabs, BorderLayout.CENTER);
        add(accion, BorderLayout.SOUTH);
    }

    private JScrollPane crearPanelGeneral() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};
        FormFields.agregarCampo(panel, gbc, fila, campos, "Nombre", "");
        FormFields.agregarCampo(panel, gbc, fila, campos, "LlaveAes", llaveAesDefault);
        FormFields.agregarCampo(panel, gbc, fila, campos, "WsseUsername", wsseUsernameDefault);
        FormFields.agregarCampo(panel, gbc, fila, campos, "WssePassword", wssePasswordDefault);
        FormFields.agregarSeccion(panel, gbc, fila, "Datos del sobre (_header)");
        FormFields.agregarCampo(panel, gbc, fila, campos, "IdCliente", "");
        FormFields.agregarCampo(panel, gbc, fila, campos, "IdTransaccion (sobre)", "");
        FormFields.agregarCampo(panel, gbc, fila, campos, "IpCliente", "172.17.0.4");

        FormFields.aplicarValidacionNumerica(campos.get("IdCliente"));
        FormFields.aplicarValidacionNumerica(campos.get("IdTransaccion (sobre)"));
        return FormFields.envolver(panel);
    }

    private JScrollPane crearPanelMensaje(MensajeNegocio.BodyMensaje b, MensajeNegocio.HeaderMensaje h) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};
        FormFields.agregarCampo(panel, gbc, fila, campos, "Autorizacion", b.autorizacion);
        FormFields.agregarCampo(panel, gbc, fila, campos, "CodBarras", b.codBarras);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Convenio", b.convenio);
        FormFields.agregarCampo(panel, gbc, fila, campos, "FechaVencimiento", b.fechaVencimiento);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Iac", b.iac);
        FormFields.agregarCampo(panel, gbc, fila, campos, "IdPersona", b.idPersona);
        FormFields.agregarCampo(panel, gbc, fila, campos, "IdTransaccion (negocio)", b.idTransaccion);
        FormFields.agregarCampo(panel, gbc, fila, campos, "NoIdentificacionUsuario", b.noIdentificacionUsuario);
        FormFields.agregarCampo(panel, gbc, fila, campos, "NombreUsuario", b.nombreUsuario);
        FormFields.agregarCampo(panel, gbc, fila, campos, "NumCelular", b.numCelular);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Observacion", b.observacion);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Otp", b.otp);
        FormFields.agregarCampo(panel, gbc, fila, campos, "TipoIdentificacion", b.tipoIdentificacion);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Valor", b.valor);
        FormFields.agregarCampo(panel, gbc, fila, campos, "NoIdentificacionCajero", h.noIdentificacionCajero);
        return FormFields.envolver(panel);
    }

    private JScrollPane crearPanelReferencias(MensajeNegocio.BodyMensaje b) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia1", b.referencia1);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia2", b.referencia2);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia3", b.referencia3);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia4", b.referencia4);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia5", b.referencia5);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia6", b.referencia6);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia7", b.referencia7);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia8", b.referencia8);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia9", b.referencia9);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia10", b.referencia10);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia11", b.referencia11);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia12", b.referencia12);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia13", b.referencia13);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia14", b.referencia14);
        FormFields.agregarCampo(panel, gbc, fila, campos, "Referencia15", b.referencia15);
        return FormFields.envolver(panel);
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
            body.autorizacion = campos.get("Autorizacion").getText();
            body.codBarras = campos.get("CodBarras").getText();
            body.convenio = campos.get("Convenio").getText();
            body.fechaVencimiento = campos.get("FechaVencimiento").getText();
            body.iac = campos.get("Iac").getText();
            body.idPersona = campos.get("IdPersona").getText();
            body.idTransaccion = campos.get("IdTransaccion (negocio)").getText();
            body.noIdentificacionUsuario = campos.get("NoIdentificacionUsuario").getText();
            body.nombreUsuario = campos.get("NombreUsuario").getText();
            body.numCelular = campos.get("NumCelular").getText();
            body.observacion = campos.get("Observacion").getText();
            body.otp = campos.get("Otp").getText();
            body.referencia1 = campos.get("Referencia1").getText();
            body.referencia2 = campos.get("Referencia2").getText();
            body.referencia3 = campos.get("Referencia3").getText();
            body.referencia4 = campos.get("Referencia4").getText();
            body.referencia5 = campos.get("Referencia5").getText();
            body.referencia6 = campos.get("Referencia6").getText();
            body.referencia7 = campos.get("Referencia7").getText();
            body.referencia8 = campos.get("Referencia8").getText();
            body.referencia9 = campos.get("Referencia9").getText();
            body.referencia10 = campos.get("Referencia10").getText();
            body.referencia11 = campos.get("Referencia11").getText();
            body.referencia12 = campos.get("Referencia12").getText();
            body.referencia13 = campos.get("Referencia13").getText();
            body.referencia14 = campos.get("Referencia14").getText();
            body.referencia15 = campos.get("Referencia15").getText();
            body.tipoIdentificacion = campos.get("TipoIdentificacion").getText();
            body.valor = campos.get("Valor").getText();
            perfil.bodyMensajeDefault = body;

            MensajeNegocio.HeaderMensaje header = new MensajeNegocio.HeaderMensaje();
            header.noIdentificacionCajero = campos.get("NoIdentificacionCajero").getText();
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
        statusBanner.ocultar();
    }
}
