package shop.arquitectura;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArquitecturaTest {

    private static final String BASE = "shop";
    private static final String[] MODULOS = {"catalog", "valoracion", "autenticacion", "carrito", "compra", "orders"};

    private static JavaClasses clases;

    @BeforeAll
    static void importarClases() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void domainNoDependeDeApplicationNiWeb() {
        noClasses().that().resideInAPackage(BASE + "..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + "..internal.usecase..", BASE + "..internal.web..")
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void applicationNoDependeDeWeb() {
        noClasses().that().resideInAPackage(BASE + "..internal.usecase..")
                .should().dependOnClassesThat().resideInAPackage(BASE + "..internal.web..")
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void losDtosDeLosCasosDeUsoSonRecords() {
        classes().that().resideInAPackage(BASE + "..internal.usecase..")
                .and().haveSimpleNameEndingWith("Vista").or().haveSimpleName("DatosPelicula")
                .should().beAssignableTo(Record.class)
                .allowEmptyShould(true)
                .check(clases);
        classes().that().resideInAPackage(BASE + ".*.api")
                .and().areNotInterfaces()
                .should().beAssignableTo(Record.class).orShould().beAssignableTo(RuntimeException.class)
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void laWebNuncaVeLasEntidades() {
        noClasses().that().resideInAPackage(BASE + "..internal.web..")
                .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class)
                .allowEmptyShould(true)
                .check(clases);
    }

    @Test
    void losModulosSoloSeAccedenPorSuApi() {
        for (String modulo : MODULOS) {
            String propio = BASE + "." + modulo;
            noClasses().that().resideOutsideOfPackage(propio + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            propio + ".internal..")
                    .allowEmptyShould(true)
                    .check(clases);
        }
    }

    @Test
    void laApiPublicaNoDependeDeSuInterior() {
        for (String modulo : MODULOS) {
            String propio = BASE + "." + modulo;
            noClasses().that().resideInAPackage(propio + ".api..")
                    .should().dependOnClassesThat().resideInAPackage(propio + ".internal..")
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
                        BASE + ".catalog..", BASE + ".valoracion..", BASE + ".autenticacion..",
                        BASE + ".carrito..", BASE + ".compra..", BASE + ".orders..")
                .allowEmptyShould(true)
                .check(clases);
    }
}
