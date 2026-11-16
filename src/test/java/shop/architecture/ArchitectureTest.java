package shop.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ENUMS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Rules of the structure. The application is a modular monolith and, inside each module, the three layers of the
 * backend rules: controller ({@code api.http}) → domain ({@code internal.domain}) ← persistence
 * ({@code internal.persistence}), with one subpackage per kind of class. {@code internal.infrastructure} holds the
 * technical implementations of domain interfaces (the {@code config} of a module) and
 * {@code internal.application.facade} the implementation of the module's public facade.
 */
class ArchitectureTest {

    private static final String BASE = "shop";
    private static final String[] MODULES = {"catalog", "rating", "authentication", "cart", "purchase", "orders"};

    private static final String HTTP = BASE + "..api.http..";
    private static final String DOMAIN = BASE + "..internal.domain..";
    private static final String PERSISTENCE = BASE + "..internal.persistence..";
    private static final String INFRASTRUCTURE = BASE + "..internal.infrastructure..";
    private static final String APPLICATION = BASE + "..internal.application..";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    // ---------------------------------------------------------------- dependencies between layers

    @Test
    void domainDependsOnNoOtherLayer() {
        noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(
                        HTTP, PERSISTENCE, INFRASTRUCTURE, APPLICATION, BASE + "..api.facade..")
                .check(classes);
    }

    @Test
    void domainKnowsNeitherHttpNorSqlNorFormatValidation() {
        noClasses().that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..", "org.springframework.http..", "jakarta.servlet..",
                        "org.springframework.jdbc..", "java.sql..", "javax.sql..", "jakarta.validation..")
                .check(classes);
    }

    @Test
    void controllerDependsOnlyOnTheDomainNeverSkippingLayers() {
        noClasses().that().resideInAPackage(HTTP)
                .should().dependOnClassesThat().resideInAnyPackage(PERSISTENCE, INFRASTRUCTURE, APPLICATION)
                .check(classes);
        noClasses().that().resideInAPackage(HTTP)
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + "..internal.domain.repository..", "org.springframework.jdbc..", "java.sql..")
                .check(classes);
    }

    @Test
    void persistenceDependsOnTheDomainAndNeverOnTheController() {
        noClasses().that().resideInAnyPackage(PERSISTENCE, INFRASTRUCTURE)
                .should().dependOnClassesThat().resideInAnyPackage(HTTP, APPLICATION)
                .check(classes);
    }

    @Test
    void jdbcIsUsedOnlyInPersistence() {
        noClasses().that().resideInAPackage(BASE + "..").and().resideOutsideOfPackage(PERSISTENCE)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.jdbc..", "java.sql..")
                .check(classes);
    }

    /** Layers talk with DTO: the anemic model is used only by the service and its mapper (enums travel in the DTO). */
    @Test
    void theModelNeverLeavesTheService() {
        DescribedPredicate<JavaClass> model = resideInAPackage(BASE + "..internal.domain.model..")
                .and(DescribedPredicate.not(ENUMS)).as("domain models that are not enums");
        noClasses().that().resideOutsideOfPackages(
                        BASE + "..internal.domain.model..", BASE + "..internal.domain.service..",
                        BASE + "..internal.domain.mapper..")
                .should().dependOnClassesThat(model)
                .check(classes);
    }

    @Test
    void applicationHoldsOnlyTheFacadeImplementation() {
        classes().that().resideInAPackage(APPLICATION)
                .should().resideInAPackage(BASE + "..internal.application.facade..")
                .andShould().haveSimpleNameEndingWith("FacadeImpl")
                .allowEmptyShould(true)
                .check(classes);
        noClasses().that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAnyPackage(HTTP, PERSISTENCE, INFRASTRUCTURE)
                .allowEmptyShould(true)
                .check(classes);
    }

    // ---------------------------------------------------------------- one package per kind of class

    @Test
    void servicesLiveInDomainService() {
        classes().that().haveSimpleNameEndingWith("Service").or().haveSimpleNameEndingWith("ServiceImpl")
                .should().resideInAPackage(BASE + "..internal.domain.service..")
                .check(classes);
        classes().that().haveSimpleNameEndingWith("ServiceImpl")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .check(classes);
    }

    @Test
    void repositoriesAreDeclaredInTheDomainAndImplementedInPersistence() {
        classes().that().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces().andShould().resideInAPackage(BASE + "..internal.domain.repository..")
                .check(classes);
        classes().that().haveSimpleNameEndingWith("RepositoryImpl")
                .should().resideInAPackage(BASE + "..internal.persistence.repository..")
                .check(classes);
        classes().that().resideInAPackage(BASE + "..internal.persistence.repository..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("RepositoryImpl")
                .check(classes);
    }

    @Test
    void mappersLiveInMapperPackages() {
        classes().that().haveSimpleNameEndingWith("Mapper").or().haveSimpleNameEndingWith("MapperImpl")
                .should().resideInAPackage("..mapper..")
                .check(classes);
        classes().that().haveSimpleNameEndingWith("WebMapper")
                .should().resideInAPackage(BASE + "..api.http.mapper..")
                .check(classes);
        classes().that().haveSimpleNameEndingWith("Mapper").and().haveSimpleNameNotEndingWith("WebMapper")
                .should().resideInAPackage(BASE + "..internal.domain.mapper..")
                .check(classes);
    }

    @Test
    void dtosLiveInDtoPackagesAndAreRecords() {
        classes().that().haveSimpleNameEndingWith("Dto")
                .should().resideInAPackage("..dto..").andShould().beAssignableTo(Record.class)
                .check(classes);
        classes().that().resideInAnyPackage(BASE + "..internal.domain.dto..", BASE + "..api.facade.dto..")
                .and().haveSimpleNameNotEndingWith("package-info")
                .should().beAssignableTo(Record.class).andShould().haveSimpleNameEndingWith("Dto")
                .check(classes);
        classes().that().haveSimpleNameEndingWith("Request").or().haveSimpleNameEndingWith("Response")
                .should().resideInAPackage(BASE + "..api.http.dto..").andShould().beAssignableTo(Record.class)
                .check(classes);
    }

    @Test
    void exceptionsLiveInExceptionPackagesOrInTheSharedErrors() {
        classes().that().haveSimpleNameEndingWith("Exception")
                .should().resideInAnyPackage("..exception..", BASE + ".shared.error..")
                .check(classes);
        classes().that().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(RuntimeException.class)
                .check(classes);
    }

    @Test
    void controllersLiveInHttpController() {
        classes().that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage(BASE + "..api.http.controller..")
                .check(classes);
        classes().that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(classes);
    }

    @Test
    void dependenciesAreInjectedByConstructor() {
        noFields().should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .orShould().beAnnotatedWith(jakarta.inject.Inject.class)
                .check(classes);
    }

    // ---------------------------------------------------------------- modules

    @Test
    void modulesAreAccessedOnlyThroughTheirFacade() {
        for (String module : MODULES) {
            String own = BASE + "." + module;
            noClasses().that().resideOutsideOfPackage(own + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(own + ".internal..", own + ".api.http..")
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
    void noCyclesBetweenModules() {
        slices().matching(BASE + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }

    @Test
    void sharedDependsOnNoModule() {
        noClasses().that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".catalog..", BASE + ".rating..", BASE + ".authentication..",
                        BASE + ".cart..", BASE + ".purchase..", BASE + ".orders..")
                .check(classes);
    }
}
