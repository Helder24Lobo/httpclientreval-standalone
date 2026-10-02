package com.example.httpclientreval.model;

import com.example.httpclientreval.crypto.AES256CBC;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Cambia la llave AES y las credenciales WS-Security de un perfil ya creado, y opcionalmente de todos
 * los que comparten su misma llave (un mismo entorno con varias transacciones se modela como varios
 * perfiles con la misma llave, así que rotarla es un solo paso en vez de uno por perfil).
 *
 * No toca disco: solo modifica los perfiles en memoria y devuelve un {@link Cambio} que sabe
 * deshacerse, por si quien llama no logra persistir con {@link Profile#saveAll}.
 */
public final class CredencialesPerfiles {

    private CredencialesPerfiles() {
    }

    /** Perfiles modificados con sus valores anteriores, para poder revertirlos. */
    public static final class Cambio {
        private final List<Profile> perfiles = new ArrayList<>();
        private final List<String[]> anteriores = new ArrayList<>();

        private void registrar(Profile perfil) {
            perfiles.add(perfil);
            anteriores.add(new String[]{perfil.llaveAes, perfil.wsseUsername, perfil.wssePassword});
        }

        public int cantidad() {
            return perfiles.size();
        }

        public List<Profile> perfiles() {
            return List.copyOf(perfiles);
        }

        /** Devuelve cada perfil a la llave y credenciales que tenía antes de {@link #aplicar}. */
        public void revertir() {
            for (int i = 0; i < perfiles.size(); i++) {
                Profile perfil = perfiles.get(i);
                String[] antes = anteriores.get(i);
                perfil.llaveAes = antes[0];
                perfil.wsseUsername = antes[1];
                perfil.wssePassword = antes[2];
            }
        }
    }

    /**
     * Los OTROS perfiles que usan la misma llave que {@code objetivo} (sin incluirlo). Si el objetivo no
     * tiene llave, no hay nada que "compartir": devuelve vacío en vez de agrupar a todos los que no tienen.
     */
    public static List<Profile> conLaMismaLlave(List<Profile> perfiles, Profile objetivo) {
        List<Profile> otros = new ArrayList<>();
        if (objetivo.llaveAes == null || objetivo.llaveAes.isEmpty()) {
            return otros;
        }
        for (Profile perfil : perfiles) {
            if (perfil != objetivo && Objects.equals(perfil.llaveAes, objetivo.llaveAes)) {
                otros.add(perfil);
            }
        }
        return otros;
    }

    /**
     * Asigna la llave (se recorta y se valida: exactamente 32 bytes UTF-8) y las credenciales WS al
     * {@code objetivo} y, si {@code aplicarATodos}, a los demás perfiles con su misma llave anterior.
     * Usuario y contraseña se guardan tal cual, igual que al crear un perfil.
     *
     * @throws IllegalArgumentException si la llave no es válida; en ese caso no se modifica ningún perfil
     */
    public static Cambio aplicar(List<Profile> perfiles, Profile objetivo, String llave, String usuario,
                                 String password, boolean aplicarATodos) {
        String llaveLimpia = llave == null ? "" : llave.trim();
        AES256CBC.validarLlave(llaveLimpia);

        // Se calcula antes de tocar nada: después del primer cambio la "misma llave" ya no coincide.
        List<Profile> destinatarios = new ArrayList<>();
        destinatarios.add(objetivo);
        if (aplicarATodos) {
            destinatarios.addAll(conLaMismaLlave(perfiles, objetivo));
        }

        Cambio cambio = new Cambio();
        for (Profile perfil : destinatarios) {
            cambio.registrar(perfil);
            perfil.llaveAes = llaveLimpia;
            perfil.wsseUsername = usuario == null ? "" : usuario;
            perfil.wssePassword = password == null ? "" : password;
        }
        return cambio;
    }
}
