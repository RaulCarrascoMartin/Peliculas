package shop.arquitectura;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import shop.ShopApplication;

/** Verifica en cada build los límites entre módulos (dependencias permitidas y ausencia de ciclos). */
class ModulithTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(ShopApplication.class);

    @Test
    void loslimitesEntreModulosSeRespetan() {
        MODULOS.verify();
    }

    @Test
    void generaLaDocumentacionDeLosModulos() {
        new Documenter(MODULOS).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
