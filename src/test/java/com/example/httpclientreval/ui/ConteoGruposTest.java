package com.example.httpclientreval.ui;

import com.example.httpclientreval.model.Profile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConteoGruposTest {

    private static Profile perfil(String nombre) {
        Profile p = new Profile();
        p.nombre = nombre;
        return p;
    }

    private static final List<Profile> PERFILES = List.of(
            perfil("Recaudos - Agua"),
            perfil("Recaudos - Luz"),
            perfil("Recargas - Comcel"),
            perfil("Sin grupo"));

    @Test
    void cuentaLosPerfilesDeCadaGrupo() {
        assertEquals(2, ConteoGrupos.contar(PERFILES, "Recaudos"));
        assertEquals(1, ConteoGrupos.contar(PERFILES, "Recargas"));
        assertEquals(1, ConteoGrupos.contar(PERFILES, Profile.GRUPO_SIN_NOMBRE));
        assertEquals(0, ConteoGrupos.contar(PERFILES, "Nuevo"));
    }

    @Test
    void agregaElContadorSoloSiHayPerfiles() {
        assertEquals("Recaudos (5)", ConteoGrupos.conConteo("Recaudos", 5));
        assertEquals("Nuevo", ConteoGrupos.conConteo("Nuevo", 0));
    }

    @Test
    void nombreBaseQuitaElContadorDeUnGrupoExistente() {
        assertEquals("Recaudos", ConteoGrupos.nombreBase("Recaudos (2)", PERFILES));
        assertEquals("Recaudos", ConteoGrupos.nombreBase("  Recaudos(7) ", PERFILES));
    }

    @Test
    void nombreBaseRespetaLosNombresSinContadorONuevos() {
        assertEquals("Recaudos", ConteoGrupos.nombreBase(" Recaudos ", PERFILES));
        // "Pagos" no existe: el "(2024)" es parte del nombre del grupo nuevo, no un contador.
        assertEquals("Pagos (2024)", ConteoGrupos.nombreBase("Pagos (2024)", PERFILES));
        assertEquals("", ConteoGrupos.nombreBase(null, PERFILES));
    }
}
