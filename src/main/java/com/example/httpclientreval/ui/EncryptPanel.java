package com.example.httpclientreval.ui;

import com.example.httpclientreval.crypto.AES256CBC;
import com.example.httpclientreval.model.Envelope;
import com.example.httpclientreval.model.MensajeNegocio;
import com.example.httpclientreval.model.Profile;
import com.example.httpclientreval.model.SoapHttpClient;
import com.example.httpclientreval.model.SoapRequestBuilder;
import com.example.httpclientreval.model.SoapResponseParser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Panel de ENCRYPT: formulario en pestañas (Sobre / Mensaje de negocio /
 * Referencias) precargado con los defaults del perfil, botón Generar, y
 * salida en pestañas con botón Copiar por bloque.
 */
public class EncryptPanel extends JPanel {

    private final Map<String, JTextField> campos = new LinkedHashMap<>();
    private final JLabel error = new JLabel(" ");
    private final OutputBlock salidaMensaje = new OutputBlock("_mensaje (Base64)");
    private final OutputBlock salidaSobre = new OutputBlock("Sobre JSON");
    private final OutputBlock salidaSoap = new OutputBlock("XML SOAP (Postman)");
    private final OutputBlock salidaHttp = new OutputBlock("Respuesta HTTP");
    private final OutputBlock salidaResultCifrado = new OutputBlock("OBJRequestResult (cifrado)");
    private final OutputBlock salidaResultPlano = new OutputBlock("Respuesta en claro");
    private final Profile perfil;
    private final MensajeNegocio.BodyMensaje bodyDefaults;
    private final MensajeNegocio.HeaderMensaje headerDefaults;
    private String ultimoSoapGenerado;

    public EncryptPanel(Profile perfil) {
        super(new BorderLayout(8, 8));
        this.perfil = perfil;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        bodyDefaults = perfil.bodyMensajeDefault != null ? perfil.bodyMensajeDefault : new MensajeNegocio.BodyMensaje();
        headerDefaults = perfil.headerMensajeDefault != null ? perfil.headerMensajeDefault : new MensajeNegocio.HeaderMensaje();

        JTabbedPane tabsEntrada = new JTabbedPane();
        tabsEntrada.addTab("Sobre (_header)", crearPanelSobre(perfil));
        tabsEntrada.addTab("Mensaje de negocio", crearPanelMensaje(bodyDefaults, headerDefaults));
        tabsEntrada.addTab("Referencias", crearPanelReferencias(bodyDefaults));

        JButton generar = new JButton("Generar");
        generar.addActionListener(e -> generar());

        JButton enviar = new JButton("Enviar al WS");
        enviar.addActionListener(e -> enviarAlWs());

        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> limpiar());

        error.setForeground(Color.RED);
        JPanel botones = new JPanel();
        botones.add(generar);
        botones.add(enviar);
        botones.add(limpiar);

        JPanel accion = new JPanel(new BorderLayout());
        accion.add(error, BorderLayout.CENTER);
        accion.add(botones, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.add(tabsEntrada, BorderLayout.CENTER);
        centro.add(accion, BorderLayout.SOUTH);

        JTabbedPane tabsSalida = new JTabbedPane();
        tabsSalida.addTab("_mensaje", salidaMensaje);
        tabsSalida.addTab("Sobre", salidaSobre);
        tabsSalida.addTab("XML SOAP", salidaSoap);
        tabsSalida.addTab("Respuesta HTTP", salidaHttp);
        tabsSalida.addTab("OBJRequestResult", salidaResultCifrado);
        tabsSalida.addTab("Respuesta en claro", salidaResultPlano);
        tabsSalida.setPreferredSize(new Dimension(100, 240));

        add(centro, BorderLayout.CENTER);
        add(tabsSalida, BorderLayout.SOUTH);
    }

    private JScrollPane crearPanelSobre(Profile perfil) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = campoGbc();
        int[] fila = {0};
        agregarCampo(panel, gbc, fila, "IdCliente", String.valueOf(perfil.idClienteDefault));
        agregarCampo(panel, gbc, fila, "IdTransaccion (sobre)", String.valueOf(perfil.idTransaccionDefault));
        agregarCampo(panel, gbc, fila, "IpCliente", perfil.ipClienteDefault);
        return envolver(panel);
    }

    private JScrollPane crearPanelMensaje(MensajeNegocio.BodyMensaje b, MensajeNegocio.HeaderMensaje h) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = campoGbc();
        int[] fila = {0};
        agregarCampo(panel, gbc, fila, "Autorizacion", b.autorizacion);
        agregarCampo(panel, gbc, fila, "CodBarras", b.codBarras);
        agregarCampo(panel, gbc, fila, "Convenio", b.convenio);
        agregarCampo(panel, gbc, fila, "FechaVencimiento", b.fechaVencimiento);
        agregarCampo(panel, gbc, fila, "Iac", b.iac);
        agregarCampo(panel, gbc, fila, "IdPersona", b.idPersona);
        agregarCampo(panel, gbc, fila, "IdTransaccion (negocio)", b.idTransaccion);
        agregarCampo(panel, gbc, fila, "NoIdentificacionUsuario", b.noIdentificacionUsuario);
        agregarCampo(panel, gbc, fila, "NombreUsuario", b.nombreUsuario);
        agregarCampo(panel, gbc, fila, "NumCelular", b.numCelular);
        agregarCampo(panel, gbc, fila, "Observacion", b.observacion);
        agregarCampo(panel, gbc, fila, "Otp", b.otp);
        agregarCampo(panel, gbc, fila, "TipoIdentificacion", b.tipoIdentificacion);
        agregarCampo(panel, gbc, fila, "Valor", b.valor);
        agregarCampo(panel, gbc, fila, "NoIdentificacionCajero", h.noIdentificacionCajero);
        return envolver(panel);
    }

    private JScrollPane crearPanelReferencias(MensajeNegocio.BodyMensaje b) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = campoGbc();
        int[] fila = {0};
        agregarCampo(panel, gbc, fila, "Referencia1", b.referencia1);
        agregarCampo(panel, gbc, fila, "Referencia2", b.referencia2);
        agregarCampo(panel, gbc, fila, "Referencia3", b.referencia3);
        agregarCampo(panel, gbc, fila, "Referencia4", b.referencia4);
        agregarCampo(panel, gbc, fila, "Referencia5", b.referencia5);
        agregarCampo(panel, gbc, fila, "Referencia6", b.referencia6);
        agregarCampo(panel, gbc, fila, "Referencia7", b.referencia7);
        agregarCampo(panel, gbc, fila, "Referencia8", b.referencia8);
        agregarCampo(panel, gbc, fila, "Referencia9", b.referencia9);
        agregarCampo(panel, gbc, fila, "Referencia10", b.referencia10);
        agregarCampo(panel, gbc, fila, "Referencia11", b.referencia11);
        agregarCampo(panel, gbc, fila, "Referencia12", b.referencia12);
        agregarCampo(panel, gbc, fila, "Referencia13", b.referencia13);
        agregarCampo(panel, gbc, fila, "Referencia14", b.referencia14);
        agregarCampo(panel, gbc, fila, "Referencia15", b.referencia15);
        return envolver(panel);
    }

    private JScrollPane envolver(JPanel contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        return scroll;
    }

    private static GridBagConstraints campoGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int[] fila, String etiqueta, String valorPorDefecto) {
        gbc.gridx = 0;
        gbc.gridy = fila[0]++;
        gbc.weightx = 0;
        panel.add(new JLabel(etiqueta + ":"), gbc);

        JTextField campo = new JTextField(valorPorDefecto == null ? "" : valorPorDefecto, 22);
        campos.put(etiqueta, campo);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(campo, gbc);
    }

    private void generar() {
        try {
            int idCliente = Integer.parseInt(campos.get("IdCliente").getText().trim());
            int idTransaccion = Integer.parseInt(campos.get("IdTransaccion (sobre)").getText().trim());
            String ipCliente = campos.get("IpCliente").getText().trim();

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

            MensajeNegocio.HeaderMensaje header = new MensajeNegocio.HeaderMensaje();
            header.noIdentificacionCajero = campos.get("NoIdentificacionCajero").getText();

            String jsonNegocio = MensajeNegocio.build(body, header);
            String mensajeCifrado = AES256CBC.encryptWithRandomIV(jsonNegocio, perfil.llaveAes);
            String sobreCompleto = Envelope.build(mensajeCifrado, idCliente, idTransaccion, ipCliente);
            String soapCompleto = SoapRequestBuilder.build(perfil.wsseUsername, perfil.wssePassword, sobreCompleto);

            salidaMensaje.setTexto(mensajeCifrado);
            salidaSobre.setTexto(sobreCompleto);
            salidaSoap.setTexto(soapCompleto);
            ultimoSoapGenerado = soapCompleto;
            error.setText(" ");
        } catch (NumberFormatException ex) {
            error.setText("IdCliente e IdTransaccion (sobre) deben ser números enteros.");
        } catch (Exception ex) {
            error.setText("Error: " + ex.getMessage());
        }
    }

    private void enviarAlWs() {
        if (ultimoSoapGenerado == null) {
            error.setText("Primero presiona Generar.");
            return;
        }

        error.setText("Enviando al WS...");
        salidaHttp.setTexto("");
        salidaResultCifrado.setTexto("");
        salidaResultPlano.setTexto("");

        new SwingWorker<SoapHttpClient.Respuesta, Void>() {
            @Override
            protected SoapHttpClient.Respuesta doInBackground() throws Exception {
                return SoapHttpClient.enviar(ultimoSoapGenerado);
            }

            @Override
            protected void done() {
                try {
                    SoapHttpClient.Respuesta respuesta = get();
                    salidaHttp.setTexto("HTTP " + respuesta.statusCode + "\n\n" + respuesta.cuerpo);

                    String resultCifrado = SoapResponseParser.extraerObjRequestResult(respuesta.cuerpo);
                    if (resultCifrado == null) {
                        salidaResultCifrado.setTexto("(No se encontró OBJRequestResult en la respuesta)");
                    } else {
                        salidaResultCifrado.setTexto(resultCifrado);
                        try {
                            salidaResultPlano.setTexto(AES256CBC.decryptWithPrependedIV(resultCifrado, perfil.llaveAes));
                        } catch (Exception exDescifrado) {
                            salidaResultPlano.setTexto("No se pudo descifrar: " + exDescifrado.getMessage());
                        }
                    }
                    error.setText(" ");
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    error.setText("Error al enviar: " + causa.getMessage());
                }
            }
        }.execute();
    }

    /** Vuelve todos los campos a los defaults del perfil y borra las salidas, para armar la siguiente transacción. */
    private void limpiar() {
        campos.get("IdCliente").setText(String.valueOf(perfil.idClienteDefault));
        campos.get("IdTransaccion (sobre)").setText(String.valueOf(perfil.idTransaccionDefault));
        campos.get("IpCliente").setText(perfil.ipClienteDefault);

        campos.get("Autorizacion").setText(bodyDefaults.autorizacion);
        campos.get("CodBarras").setText(bodyDefaults.codBarras);
        campos.get("Convenio").setText(bodyDefaults.convenio);
        campos.get("FechaVencimiento").setText(bodyDefaults.fechaVencimiento);
        campos.get("Iac").setText(bodyDefaults.iac);
        campos.get("IdPersona").setText(bodyDefaults.idPersona);
        campos.get("IdTransaccion (negocio)").setText(bodyDefaults.idTransaccion);
        campos.get("NoIdentificacionUsuario").setText(bodyDefaults.noIdentificacionUsuario);
        campos.get("NombreUsuario").setText(bodyDefaults.nombreUsuario);
        campos.get("NumCelular").setText(bodyDefaults.numCelular);
        campos.get("Observacion").setText(bodyDefaults.observacion);
        campos.get("Otp").setText(bodyDefaults.otp);
        campos.get("TipoIdentificacion").setText(bodyDefaults.tipoIdentificacion);
        campos.get("Valor").setText(bodyDefaults.valor);
        campos.get("NoIdentificacionCajero").setText(headerDefaults.noIdentificacionCajero);

        campos.get("Referencia1").setText(bodyDefaults.referencia1);
        campos.get("Referencia2").setText(bodyDefaults.referencia2);
        campos.get("Referencia3").setText(bodyDefaults.referencia3);
        campos.get("Referencia4").setText(bodyDefaults.referencia4);
        campos.get("Referencia5").setText(bodyDefaults.referencia5);
        campos.get("Referencia6").setText(bodyDefaults.referencia6);
        campos.get("Referencia7").setText(bodyDefaults.referencia7);
        campos.get("Referencia8").setText(bodyDefaults.referencia8);
        campos.get("Referencia9").setText(bodyDefaults.referencia9);
        campos.get("Referencia10").setText(bodyDefaults.referencia10);
        campos.get("Referencia11").setText(bodyDefaults.referencia11);
        campos.get("Referencia12").setText(bodyDefaults.referencia12);
        campos.get("Referencia13").setText(bodyDefaults.referencia13);
        campos.get("Referencia14").setText(bodyDefaults.referencia14);
        campos.get("Referencia15").setText(bodyDefaults.referencia15);

        salidaMensaje.setTexto("");
        salidaSobre.setTexto("");
        salidaSoap.setTexto("");
        salidaHttp.setTexto("");
        salidaResultCifrado.setTexto("");
        salidaResultPlano.setTexto("");
        ultimoSoapGenerado = null;
        error.setText(" ");
    }
}
