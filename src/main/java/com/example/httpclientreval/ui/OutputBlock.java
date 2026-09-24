package com.example.httpclientreval.ui;

import com.formdev.flatlaf.FlatLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.ExpandedFoldRenderStrategy;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;

/**
 * Bloque de salida de solo lectura con botón "Copiar" y, opcionalmente, un
 * badge de estado (ej. "200 - 250ms") para respuestas HTTP. Si el texto es
 * JSON o XML se muestra con formato, colores y nodos plegables; "Copiar"
 * entrega el texto original, sin reformatear.
 */
public class OutputBlock extends JPanel {

    private static final String TEMA_OSCURO = "/org/fife/ui/rsyntaxtextarea/themes/dark.xml";
    private static final String TEMA_CLARO = "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
    private static final Font FUENTE = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final RSyntaxTextArea area = new RSyntaxTextArea();
    private final JLabel badge = new JLabel(" ");
    private String textoOriginal = "";

    public OutputBlock(String titulo) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setHighlightCurrentLine(false);
        area.setCodeFoldingEnabled(true);
        aplicarTema();

        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 11f));
        badge.setVisible(false);

        JButton copiar = new JButton("Copiar", Icons.copiar());
        copiar.addActionListener(e -> ClipboardUtil.copiar(textoOriginal));

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        izquierda.add(new JLabel(titulo));
        izquierda.add(badge);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(izquierda, BorderLayout.WEST);
        norte.add(copiar, BorderLayout.EAST);

        RTextScrollPane scroll = new RTextScrollPane(area);
        scroll.setFoldIndicatorEnabled(true);
        // Por defecto los iconos de nodos expandidos solo se ven al pasar el mouse: no se descubre el plegado.
        scroll.getGutter().setExpandedFoldRenderStrategy(ExpandedFoldRenderStrategy.ALWAYS);
        scroll.setLineNumbersEnabled(false);

        add(norte, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /** Se invoca al cambiar el look and feel: los colores del editor vienen de su propio tema, hay que reaplicarlo. */
    @Override
    public void updateUI() {
        super.updateUI();
        if (area != null) {
            aplicarTema();
        }
    }

    private void aplicarTema() {
        String recurso = FlatLaf.isLafDark() ? TEMA_OSCURO : TEMA_CLARO;
        try (InputStream in = OutputBlock.class.getResourceAsStream(recurso)) {
            Theme.load(in).apply(area);
        } catch (IOException | RuntimeException ex) {
            // Sin tema se usan los colores por defecto del editor; no es motivo para fallar.
        }
        area.setFont(FUENTE);
    }

    public void setTexto(String texto) {
        textoOriginal = texto == null ? "" : texto;
        FormatoSalida.Resultado formato = FormatoSalida.formatear(textoOriginal);
        area.setSyntaxEditingStyle(switch (formato.tipo) {
            case JSON -> SyntaxConstants.SYNTAX_STYLE_JSON;
            case XML -> SyntaxConstants.SYNTAX_STYLE_XML;
            case TEXTO -> SyntaxConstants.SYNTAX_STYLE_NONE;
        });
        area.setText(formato.texto);
        // Los nodos plegables no se calculan solos tras setText; sin esto no aparecen los indicadores.
        area.getFoldManager().reparse();
        area.setCaretPosition(0);
    }

    /** Muestra un badge de estado (ej. "200 - 250ms"), verde si éxito o rojo si no. */
    public void setBadge(String texto, boolean exito) {
        badge.setText(texto);
        badge.setForeground(Color.WHITE);
        badge.setBackground(exito ? new Color(46, 125, 50) : new Color(198, 40, 40));
        badge.setVisible(true);
    }

    public void ocultarBadge() {
        badge.setVisible(false);
    }
}
