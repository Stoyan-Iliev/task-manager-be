package com.gradproject.taskmanager.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;


class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.gradproject.taskmanager");
    }

    
    @Test
    void layeredArchitecture_shouldBeRespected() {
        Architectures.LayeredArchitecture rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Controllers").definedBy("..modules..controller..")
                .layer("Services").definedBy("..modules..service..")
                .layer("Repositories").definedBy("..modules..repository..")
                .layer("Domain").definedBy("..modules..domain..")
                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Services")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Controllers", "Services", "Repositories");

        rule.check(classes);
    }

    
    @Test
    void modules_shouldNotDependOnEachOther() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.auth..")
                .should().dependOnClassesThat().resideInAPackage("..modules.user..");

        rule.check(classes);

        
        ArchRule authToProject = noClasses()
                .that().resideInAPackage("..modules.auth..")
                .should().dependOnClassesThat().resideInAPackage("..modules.project..");
        authToProject.check(classes);
    }


    @Test
    void modules_canDependOnSharedAndInfrastructure() {
        ArchRule sharedRule = classes()
                .that().resideInAPackage("..modules..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..modules..",
                        "..shared..",
                        "..infrastructure..",
                        "java..",
                        "javax..",
                        "org.springframework..",
                        "jakarta..",
                        "lombok..",
                        "com.fasterxml..",
                        "org.slf4j..",
                        "org.hibernate..",
                        "com.sendgrid.."  // SendGrid SDK for email notifications
                );

        sharedRule.check(classes);
    }

    
    @Test
    void moduleControllers_shouldResideInControllerPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .and().resideInAPackage("..modules..")
                .should().resideInAPackage("..controller..");

        rule.check(classes);
    }

    
    @Test
    void moduleServices_shouldResideInServicePackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Service.class)
                .and().resideInAPackage("..modules..")
                .should().resideInAPackage("..service..");

        rule.check(classes);
    }

    
    @Test
    void repositories_shouldResideInRepositoryPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Repository.class)
                .or().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage("..repository..");

        rule.check(classes);
    }

    
    @Test
    void controllers_shouldBeNamedWithControllerSuffix() {
        ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .and().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller");

        rule.check(classes);
    }

    
    @Test
    void services_shouldBeNamedWithServiceSuffix() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .and().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("ServiceImpl");

        rule.check(classes);
    }

    
    @Test
    void repositories_shouldBeNamedWithRepositorySuffix() {
        ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("Repository");

        rule.check(classes);
    }

    
    @Test
    void entities_shouldResideInDomainPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..domain..");

        rule.check(classes);
    }

    
    @Test
    void dtos_shouldResideInDtoPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .or().haveSimpleNameEndingWith("DTO")
                .and().doNotHaveSimpleName("GitPullRequest")
                .should().resideInAPackage("..dto..");

        rule.check(classes);
    }

    
    @Test
    void infrastructureConfig_shouldNotDependOnModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infrastructure.config..")
                .should().dependOnClassesThat().resideInAPackage("..modules..");

        rule.check(classes);
    }

    
    @Test
    void fieldInjection_shouldNotBeUsed() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat()
                .resideInAnyPackage("..service..", "..controller..", "..repository..")
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired");

        rule.check(classes);
    }
}
