package com.pawmatch.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Enforces the hexagonal boundary rules from {@code .claude/CLAUDE.md} (Architecture → Boundary rules).
 * If a rule fails, fix the dependency direction. Don't weaken the rule without updating CLAUDE.md first.
 */
class HexagonalArchitectureTest {

    private static final String ROOT = "com.pawmatch";
    private static final List<String> FEATURES =
            List.of("catalog", "questionnaire", "recommendation", "feedback", "inference");

    private static final String[] FRAMEWORKS_FORBIDDEN_IN_DOMAIN = {
            "org.springframework..", "jakarta..", "javax.persistence..", "org.hibernate..",
            "com.fasterxml.jackson..", "tools.jackson.."};

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    // Rule 1: domain is plain Java and depends on nothing outside itself except the shared kernel.
    @Test
    void domainIsPlainJava() {
        check(noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS_FORBIDDEN_IN_DOMAIN));
    }

    @Test
    void domainDoesNotDependOnApplicationOrAdapters() {
        check(noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..", "..common.web.."));
    }

    // Rule 2: application depends only on domain, its ports and the kernel; Spring only for wiring/transactions.
    @Test
    void applicationHasNoAdapterOrTechnologyDependencies() {
        check(noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..", "..common.web..",
                        "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                        "org.springframework.web..", "org.springframework.http..", "jakarta.servlet..",
                        "com.fasterxml.jackson.databind..", "tools.jackson.."));
    }

    // Rule 3: adapters depend inward. Outbound adapters never depend on inbound adapters and vice versa.
    @Test
    void inboundAndOutboundAdaptersAreIndependent() {
        check(noClasses().that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out.."));
        check(noClasses().that().resideInAPackage("..adapter.out..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.in.."));
    }

    // Rule 4: JPA and Spring Data live only in persistence adapters.
    @Test
    void persistenceTechnologyOnlyInPersistenceAdapters() {
        check(noClasses().that().resideOutsideOfPackage("..adapter.out.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..", "org.hibernate..", "org.springframework.data.."));
    }

    // Rule 5: web technology lives only in web adapters; controllers call inbound ports, never services.
    @Test
    void webTechnologyOnlyInWebAdapters() {
        check(noClasses().that().resideOutsideOfPackages("..adapter.in.web..", "..common.web..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..", "org.springframework.http..", "jakarta.servlet.."));
    }

    @Test
    void webAdaptersUseInboundPortsOnly() {
        check(noClasses().that().resideInAPackage("..adapter.in.web..")
                .should().dependOnClassesThat().resideInAnyPackage("..application.service..", "..application.port.out.."));
    }

    // Rule 6: features talk to each other only through the other feature's port/in (and the domain types in it).
    // Rule 7: the one exception is inference implementing recommendation's ScoringEngine port (port/out).
    @TestFactory
    List<DynamicTest> featuresAccessEachOtherOnlyThroughInboundPorts() {
        return FEATURES.stream()
                .flatMap(from -> FEATURES.stream().filter(to -> !to.equals(from)).map(to -> DynamicTest.dynamicTest(
                        from + " → " + to, () -> check(crossFeatureRule(from, to)))))
                .toList();
    }

    private static ArchRule crossFeatureRule(String from, String to) {
        String target = ROOT + "." + to;
        boolean inferenceImplementsScoringPort = from.equals("inference") && to.equals("recommendation");
        String[] forbidden = inferenceImplementsScoringPort
                ? new String[] {target + ".application.service..", target + ".adapter.."}
                : new String[] {target + ".application.service..", target + ".application.port.out..", target + ".adapter.."};
        return noClasses().that().resideInAPackage(ROOT + "." + from + "..")
                .should().dependOnClassesThat().resideInAnyPackage(forbidden);
    }

    @Test
    void featuresAreFreeOfCycles() {
        check(slices().matching(ROOT + ".(*)..").should().beFreeOfCycles());
    }

    // Shared kernel: plain Java, depends on nothing else in the application.
    @Test
    void kernelIsPlainJavaAndSelfContained() {
        check(noClasses().that().resideInAPackage(ROOT + ".common.kernel..")
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORKS_FORBIDDEN_IN_DOMAIN));
        check(noClasses().that().resideInAPackage(ROOT + ".common.kernel..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        FEATURES.stream().map(f -> ROOT + "." + f + "..").toArray(String[]::new)));
    }

    // Cross-cutting web config is wired by Spring; no code may depend on it directly.
    @Test
    void nothingDependsOnCommonWeb() {
        check(noClasses().that().resideOutsideOfPackage(ROOT + ".common.web..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".common.web.."));
    }

    private static void check(ArchRule rule) {
        // Feature packages may still be empty; an empty rule is not a violation.
        rule.allowEmptyShould(true).check(classes);
    }
}
