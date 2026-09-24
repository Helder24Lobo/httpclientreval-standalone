package com.example.httpclientreval.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;

/**
 * Iconos SVG (16x16) con color real, cargados desde
 * {@code resources/com/example/httpclientreval/ui/icons}. Java/Swing no
 * renderiza emoji a color de forma confiable en botones/pestañas, así que se
 * usan SVG propios. {@link FlatSVGIcon} los rasteriza según la escala de la
 * pantalla, por lo que se ven nítidos en HiDPI.
 */
final class Icons {

    private static final String RUTA = "com/example/httpclientreval/ui/icons/";
    private static final int TAMANO = 16;

    private Icons() {
    }

    private static Icon icono(String nombre) {
        return new FlatSVGIcon(RUTA + nombre + ".svg", TAMANO, TAMANO);
    }

    static Icon generar() {
        return icono("generar");
    }

    static Icon enviar() {
        return icono("enviar");
    }

    static Icon limpiar() {
        return icono("limpiar");
    }

    static Icon guardar() {
        return icono("guardar");
    }

    static Icon descifrar() {
        return icono("descifrar");
    }

    static Icon nuevo() {
        return icono("nuevo");
    }

    static Icon copiar() {
        return icono("copiar");
    }

    static Icon editar() {
        return icono("editar");
    }

    static Icon configuracion() {
        return icono("configuracion");
    }

    static Icon buscar() {
        return icono("buscar");
    }

    static Icon errorNegocio() {
        return icono("error-negocio");
    }

    static Icon errorHttp() {
        return icono("error-http");
    }

    static Icon check() {
        return icono("check");
    }

    static Icon alerta() {
        return icono("alerta");
    }

    static Icon info() {
        return icono("info");
    }
}
