package org.example.arquitectura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArquitecturaTest {

    private static final String BASE = "org.example";
    private static final String[] MODULOS = {"pelicula", "valoracion", "autenticacion", "carrito", "compra"};

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void domainNoDependeDeApplicationNiWeb() {
        noClasses().that().resideInAPackage(BASE + "..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + "..application..", BASE + "..web..")
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void applicationNoDependeDeWeb() {
        noClasses().that().resideInAPackage(BASE + "..application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + "..web..")
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void losModulosSoloSeAccedenPorSuApi() {
        for (String modulo : MODULOS) {
            String propio = BASE + "." + modulo;
            noClasses().that().resideOutsideOfPackage(propio + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            propio + ".domain..", propio + ".application..", propio + ".web..")
                    .allowEmptyShould(true)
                    .check(clases);
        }
    }

    @Test
    void sinCiclosEntreModulos() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void compartidoNoDependeDeModulos() {
        noClasses().that().resideInAPackage(BASE + ".compartido..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".pelicula..", BASE + ".valoracion..", BASE + ".autenticacion..",
                        BASE + ".carrito..", BASE + ".compra..")
                .allowEmptyShould(true)
                .check(clases);
    }
}
