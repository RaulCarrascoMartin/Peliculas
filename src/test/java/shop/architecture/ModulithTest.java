package shop.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import shop.ShopApplication;

/** Verifies on every build the boundaries between modules (allowed dependencies and absence of cycles). */
class ModulithTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(ShopApplication.class);

    @Test
    void moduleBoundariesAreRespected() {
        MODULES.verify();
    }

    @Test
    void generatesModuleDocumentation() {
        new Documenter(MODULES).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
