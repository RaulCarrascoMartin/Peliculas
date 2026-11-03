package shop.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Rules of the module structure: {@code <module>/{api.facade, api.http, internal.{application, domain,
 * infrastructure, persistence}}} plus the composition root {@code shop.app}.
 */
class ArchitectureTest {

    private static final String BASE = "shop";
    private static final String[] MODULES = {"catalog", "rating", "authentication", "cart", "purchase", "orders"};
    private static final String[] FRAMEWORKS = {
            "org.springframework..", "jakarta.persistence..", "jakarta.validation..", "org.hibernate..",
            "org.mapstruct.."};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void domainStaysFrameworkFree() {
        noClasses().that().resideInAPackage(BASE + "..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void applicationDoesNotUseSpringNorPersistenceFrameworks() {
        noClasses().that().resideInAPackage(BASE + "..internal.application..")
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void domainDependsOnNoOtherLayer() {
        noClasses().that().resideInAPackage(BASE + "..internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + "..internal.application..", BASE + "..internal.persistence..",
                        BASE + "..internal.infrastructure..", BASE + "..api..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void applicationDoesNotDependOnPersistenceInfrastructureNorHttp() {
        noClasses().that().resideInAPackage(BASE + "..internal.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + "..internal.persistence..", BASE + "..internal.infrastructure..",
                        BASE + "..api.http..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void persistenceDoesNotDependOnApplicationNorHttp() {
        // infrastructure may drive a use case (e.g. the startup runner); persistence never does.
        noClasses().that().resideInAPackage(BASE + "..internal.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + "..internal.application..", BASE + "..api.http..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses().that().resideInAPackage(BASE + "..internal.infrastructure..")
                .should().dependOnClassesThat().resideInAPackage(BASE + "..api.http..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void httpNeverSeesDomainModelNorPersistence() {
        // Only domain enums (value types that are parameters of the use cases) may be seen from the HTTP layer.
        noClasses().that().resideInAPackage(BASE + "..api.http..")
                .should().dependOnClassesThat(
                        com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage(BASE + "..internal.domain..")
                                .and(com.tngtech.archunit.base.DescribedPredicate.not(
                                        com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES.or(
                                                com.tngtech.archunit.core.domain.JavaClass.Predicates.ENUMS))))
                .allowEmptyShould(true)
                .check(classes);
        noClasses().that().resideInAPackage(BASE + "..api.http..")
                .should().dependOnClassesThat().resideInAPackage(BASE + "..internal.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void useCaseDtosAreRecords() {
        classes().that().resideInAPackage(BASE + "..internal.application..")
                .and().haveSimpleNameEndingWith("View").or().haveSimpleName("MovieData")
                .should().beAssignableTo(Record.class)
                .allowEmptyShould(true)
                .check(classes);
        classes().that().resideInAPackage(BASE + ".*.api.facade..")
                .and().areNotInterfaces()
                .and().areNotAnnotations()
                .should().beAssignableTo(Record.class).orShould().beAssignableTo(RuntimeException.class)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void entitiesStayInPersistence() {
        classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAPackage(BASE + "..internal.persistence..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void modulesAreAccessedOnlyThroughTheirFacade() {
        // Only the composition root (shop.app) sees the internals of every module, to wire them.
        for (String module : MODULES) {
            String own = BASE + "." + module;
            noClasses().that().resideOutsideOfPackages(own + "..", BASE + ".app..")
                    .should().dependOnClassesThat().resideInAnyPackage(own + ".internal..", own + ".api.http..")
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @Test
    void facadeDoesNotDependOnItsInternals() {
        for (String module : MODULES) {
            String own = BASE + "." + module;
            noClasses().that().resideInAPackage(own + ".api.facade..")
                    .should().dependOnClassesThat().resideInAnyPackage(own + ".internal..", own + ".api.http..")
                    .allowEmptyShould(true)
                    .check(classes);
        }
    }

    @Test
    void appHoldsOnlyConfiguration() {
        classes().that().resideInAPackage(BASE + ".app..")
                .and().areNotAnnotations().and().haveSimpleNameNotEndingWith("package-info")
                .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void noCyclesBetweenModules() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    void sharedAndAppDoNotBeDependedOnByModules() {
        noClasses().that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".catalog..", BASE + ".rating..", BASE + ".authentication..",
                        BASE + ".cart..", BASE + ".purchase..", BASE + ".orders..", BASE + ".app..")
                .allowEmptyShould(true)
                .check(classes);
        noClasses().that().resideOutsideOfPackage(BASE + ".app..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".app..")
                .allowEmptyShould(true)
                .check(classes);
    }
}
