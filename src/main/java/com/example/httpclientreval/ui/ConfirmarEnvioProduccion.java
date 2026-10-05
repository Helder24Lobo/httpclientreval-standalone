package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.SoapHttpClient;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Freno extra solo para Producción: antes de mandar una petición al WS pide confirmar, mostrando el
 * perfil y la URL destino. En Pruebas y Personalizado no hace nada (se envía directo, como siempre),
 * así que no estorba el trabajo diario con datos de prueba.
 */
final class ConfirmarEnvioProduccion {

    private static final String ENVIAR = "Enviar a PRODUCCIÓN";
    private static final String CANCELAR = "Cancelar";

    private ConfirmarEnvioProduccion() {
    }

    static boolean esProduccion() {
        return SoapHttpClient.getAmbiente() == SoapHttpClient.Ambiente.PRODUCCION;
    }

    /** true si se puede seguir con el envío: no es Producción, o el usuario confirmó expresamente. */
    static boolean confirmar(Component padre, String perfilNombre) {
        if (!esProduccion()) {
            return true;
        }
        int opcion = JOptionPane.showOptionDialog(padre,
                "El ambiente activo es PRODUCCIÓN.\n\n"
                        + "Perfil: " + perfilNombre + "\n"
                        + "Destino: " + SoapHttpClient.getUrl() + "\n\n"
                        + "¿Enviar esta petición?",
                "Confirmar envío a producción", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, new Object[]{ENVIAR, CANCELAR}, CANCELAR);
        return opcion == 0;
    }
}
