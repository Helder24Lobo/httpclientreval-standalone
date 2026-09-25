package com.example.httpclientreval.model;

import com.example.httpclientreval.crypto.AES256CBC;

import java.time.LocalDateTime;

/**
 * Un envío al WS ya resuelto: la petición SOAP, lo que respondió el servidor
 * (o por qué falló), cuánto tardó y la lectura de la respuesta (OBJRequestResult
 * descifrado y código de negocio). Es lo que se muestra en pantalla y lo que
 * se guarda en el historial de la sesión, y se puede volver a enviar.
 */
public class EnvioRegistrado {

    /** Cómo terminó el envío, de lo más grave a lo más fino. */
    public enum Resultado {
        /** No hubo respuesta HTTP (sin conexión, timeout, etc.). */
        SIN_RESPUESTA,
        /** El servidor respondió con un código HTTP fuera de 2xx. */
        ERROR_HTTP,
        /** HTTP 2xx pero no se pudo obtener el código de negocio (sin OBJRequestResult o no se pudo descifrar). */
        INCOMPLETO,
        /** HTTP 2xx con un código de negocio distinto de 0. */
        ERROR_NEGOCIO,
        /** HTTP 2xx con código de negocio 0. */
        EXITO
    }

    public final LocalDateTime hora;
    public final Profile perfil;
    /** Nombre del perfil al momento del envío (el perfil puede renombrarse después). */
    public final String perfilNombre;
    public final String soapEnviado;

    /** Código HTTP, o null si no hubo respuesta. */
    public final Integer statusCode;
    public final long tiempoMs;
    public final String cuerpo;
    /** Motivo del fallo cuando no hubo respuesta HTTP; null en los demás casos. */
    public final String errorEnvio;

    public final String resultadoCifrado;
    public final String resultadoPlano;
    /** Por qué no se pudo descifrar OBJRequestResult; null si se descifró o no había. */
    public final String errorDescifrado;
    public final Integer codigoNegocio;
    public final String mensajeNegocio;

    private EnvioRegistrado(LocalDateTime hora, Profile perfil, String soapEnviado, Integer statusCode, long tiempoMs,
                            String cuerpo, String errorEnvio, String resultadoCifrado, String resultadoPlano,
                            String errorDescifrado, Integer codigoNegocio, String mensajeNegocio) {
        this.hora = hora;
        this.perfil = perfil;
        this.perfilNombre = perfil.nombre;
        this.soapEnviado = soapEnviado;
        this.statusCode = statusCode;
        this.tiempoMs = tiempoMs;
        this.cuerpo = cuerpo;
        this.errorEnvio = errorEnvio;
        this.resultadoCifrado = resultadoCifrado;
        this.resultadoPlano = resultadoPlano;
        this.errorDescifrado = errorDescifrado;
        this.codigoNegocio = codigoNegocio;
        this.mensajeNegocio = mensajeNegocio;
    }

    /**
     * Envía el SOAP al WS y analiza la respuesta. Nunca lanza: si la llamada
     * falla, devuelve un registro {@link Resultado#SIN_RESPUESTA} con el motivo.
     * Bloquea hasta tener respuesta; llamarlo fuera del hilo de la UI.
     */
    public static EnvioRegistrado enviar(Profile perfil, String soapXml) {
        LocalDateTime hora = LocalDateTime.now();
        long inicio = System.currentTimeMillis();
        try {
            return desde(hora, perfil, soapXml, SoapHttpClient.enviar(soapXml));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallido(hora, perfil, soapXml, e, System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            return fallido(hora, perfil, soapXml, e, System.currentTimeMillis() - inicio);
        }
    }

    /** Analiza una respuesta ya recibida: extrae y descifra OBJRequestResult y lee el código de negocio. */
    static EnvioRegistrado desde(LocalDateTime hora, Profile perfil, String soapXml, SoapHttpClient.Respuesta respuesta) {
        String cifrado = SoapResponseParser.extraerObjRequestResult(respuesta.cuerpo);
        String plano = null;
        String errorDescifrado = null;
        Integer codigo = null;
        String mensaje = null;

        if (cifrado != null) {
            try {
                plano = AES256CBC.decryptWithPrependedIV(cifrado, perfil.llaveAes);
                RespuestaNegocioParser.Resultado negocio = RespuestaNegocioParser.parsear(plano);
                codigo = negocio.codigo;
                mensaje = negocio.mensaje;
            } catch (Exception e) {
                errorDescifrado = e.getMessage();
            }
        }
        return new EnvioRegistrado(hora, perfil, soapXml, respuesta.statusCode, respuesta.tiempoMs, respuesta.cuerpo,
                null, cifrado, plano, errorDescifrado, codigo, mensaje);
    }

    static EnvioRegistrado fallido(LocalDateTime hora, Profile perfil, String soapXml, Throwable causa, long tiempoMs) {
        return new EnvioRegistrado(hora, perfil, soapXml, null, tiempoMs, null, String.valueOf(causa.getMessage()),
                null, null, null, null, null);
    }

    public boolean exitoHttp() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    public Resultado resultado() {
        if (statusCode == null) {
            return Resultado.SIN_RESPUESTA;
        }
        if (!exitoHttp()) {
            return Resultado.ERROR_HTTP;
        }
        if (codigoNegocio == null) {
            return Resultado.INCOMPLETO;
        }
        return codigoNegocio == 0 ? Resultado.EXITO : Resultado.ERROR_NEGOCIO;
    }

    /** Texto corto para listas: "Código 0", "HTTP 500", "Sin respuesta"... */
    public String resumen() {
        switch (resultado()) {
            case SIN_RESPUESTA:
                return "Sin respuesta";
            case ERROR_HTTP:
                return "HTTP " + statusCode;
            case INCOMPLETO:
                return cuerpo != null && resultadoCifrado == null ? "Sin OBJRequestResult" : "Sin código de negocio";
            default:
                return "Código " + codigoNegocio;
        }
    }
}
