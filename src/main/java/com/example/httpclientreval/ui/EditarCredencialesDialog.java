package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.CredencialesPerfiles;
import com.example.httpclientreval.model.Profile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana modal para cambiar la llave AES y las credenciales WS-Security de un perfil existente
 * (hasta ahora solo se podían fijar al crearlo). Si otros perfiles usan la misma llave, ofrece
 * aplicar el cambio a todos a la vez, que es lo que se necesita al rotarla.
 *
 * Solo modifica los perfiles en memoria; quien la abre decide cómo persistir (y revertir si falla).
 */
final class EditarCredencialesDialog extends JDialog {

    private static final String LLAVE = "LlaveAes";
    private static final String USUARIO = "WsseUsername";
    private static final String PASSWORD = "WssePassword";

    private final Map<String, JTextField> campos = new LinkedHashMap<>();
    private final StatusBanner statusBanner = new StatusBanner();
    private final JCheckBox aplicarATodos;
    private final List<Profile> perfiles;
    private final Profile perfil;
    private CredencialesPerfiles.Cambio cambio;

    private EditarCredencialesDialog(Window propietario, Profile perfil, List<Profile> perfiles) {
        super(propietario, "Credenciales de " + perfil.nombre, ModalityType.APPLICATION_MODAL);
        this.perfil = perfil;
        this.perfiles = perfiles;

        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = FormFields.gbc();
        int[] fila = {0};
        FormFields.agregarCampoSecreto(formulario, gbc, fila, campos, LLAVE, valor(perfil.llaveAes));
        FormFields.agregarCampo(formulario, gbc, fila, campos, USUARIO, valor(perfil.wsseUsername));
        FormFields.agregarCampoSecreto(formulario, gbc, fila, campos, PASSWORD, valor(perfil.wssePassword));

        int otros = CredencialesPerfiles.conLaMismaLlave(perfiles, perfil).size();
        aplicarATodos = new JCheckBox(otros == 1
                ? "Aplicar también al otro perfil que usa la misma llave"
                : "Aplicar también a los otros " + otros + " perfiles que usan la misma llave");
        Accesibilidad.nombrar(aplicarATodos, "Aplicar a los demás perfiles con la misma llave");
        aplicarATodos.setSelected(otros > 0);
        aplicarATodos.setEnabled(otros > 0);
        aplicarATodos.setToolTipText(otros > 0
                ? "Cambia la llave y las credenciales de todos los perfiles que hoy comparten la llave de este"
                : "Ningún otro perfil usa la misma llave");

        JButton guardar = new JButton("Guardar", Icons.guardar());
        guardar.addActionListener(e -> guardar());
        JButton cancelar = new JButton("Cancelar", Icons.cancelar());
        cancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.add(guardar);
        botones.add(cancelar);

        JPanel sur = new JPanel(new BorderLayout(8, 8));
        sur.add(statusBanner, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.add(formulario, BorderLayout.CENTER);
        centro.add(aplicarATodos, BorderLayout.SOUTH);

        JPanel contenido = new JPanel(new BorderLayout(8, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contenido.add(new JLabel("Llave AES y credenciales del WS de esta transacción"), BorderLayout.NORTH);
        contenido.add(centro, BorderLayout.CENTER);
        contenido.add(sur, BorderLayout.SOUTH);
        setContentPane(contenido);

        getRootPane().setDefaultButton(guardar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setLocationRelativeTo(propietario);
    }

    /**
     * Abre el diálogo y espera. Devuelve el cambio ya aplicado en memoria, o {@code null} si el usuario
     * canceló. Quien lo recibe debe persistir y, si falla, llamar a {@link CredencialesPerfiles.Cambio#revertir()}.
     */
    static CredencialesPerfiles.Cambio mostrar(Window propietario, Profile perfil, List<Profile> perfiles) {
        EditarCredencialesDialog dialogo = new EditarCredencialesDialog(propietario, perfil, perfiles);
        dialogo.setVisible(true);
        return dialogo.cambio;
    }

    private void guardar() {
        try {
            cambio = CredencialesPerfiles.aplicar(perfiles, perfil,
                    campos.get(LLAVE).getText(),
                    campos.get(USUARIO).getText(),
                    campos.get(PASSWORD).getText(),
                    aplicarATodos.isSelected());
            dispose();
        } catch (IllegalArgumentException ex) {
            statusBanner.mostrarError(ex.getMessage());
        }
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }
}
