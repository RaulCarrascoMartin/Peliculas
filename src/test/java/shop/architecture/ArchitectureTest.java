package shop.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static final String BASE = "shop";
    private static final String[] MODULES = {"catalog", "rating", "authentication", "cart", "purchase", "orders"};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void domainDoesNotDependOnApplicationNorWeb() {
        noClasses().that().resideInAPackage(BASE + "..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + "..internal.usecase..", BASE + "..internal.web..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnWeb() {
        noClasses().that().resideInAPackage(BASE + "..internal.usecase..")
                .should().dependOnClassesThat().resideInAPackage(BASE + "..internal.web..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void useCaseDtosAreRecords() {
        classes().that().resideInAPackage(BASE + "..internal.usecase..")
                .and().haveSimpleNameEndingWith("View").or().haveSimpleName("MovieData")
                .should().beAssignableTo(Record.class)
                .allowEmptyShould(true)
                .check(classes);
        classes().that().resideInAPackage(BASE + ".*.api")
                .and().areNotInterfaces()
                .should().beAssignableTo(Record.class).orShould().beAssignableTo(RuntimeException.class)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void webNeverSeesEntities() {
        noClasses().that().resideInAPackage(BASE + "..internal.web..")
                .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void modulesAreAccessedOnlyThroughTheirApi() {
        for (String module : MODULES) {
            String own = BASE + "." + module;
            noClasses().that().resideOutsideOfPackage(own + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            own + ".internal..")
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @Test
    void publicApiDoesNotDependOnItsInternals() {
        for (String module : MODULES) {
            String own = BASE + "." + module;
            noClasses().that().resideInAPackage(own + ".api..")
                    .should().dependOnClassesThat().resideInAPackage(own + ".internal..")
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @Test
    void noCyclesBetweenModules() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void sharedDoesNotDependOnModules() {
        noClasses().that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".catalog..", BASE + ".rating..", BASE + ".authentication..",
                        BASE + ".cart..", BASE + ".purchase..", BASE + ".orders..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
