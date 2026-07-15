package io.github.xfoundries.demo.expenseapproval;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.xfoundries.demo.expenseapproval.application.approval.ApprovedExpenseAmountReader;
import io.github.xfoundries.demo.expenseapproval.application.payment.PaymentStatusProjectionStore;
import org.jfoundry.architecture.hexagonal.PrimaryPort;
import org.jfoundry.test.archunit.JFoundryRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.xfoundries.demo.expenseapproval")
class OnionSimpleArchitectureTest {

    @ArchTest
    static final ArchTests jfoundryRules = JFoundryRules.onionSimple();

    @ArchTest
    static final ArchTests jmoleculesDddRules = JFoundryRules.jmoleculesDdd();

    @ArchTest
    static final ArchTests aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();

    @ArchTest
    static final ArchTests cqrsRules = JFoundryRules.cqrs();

    @ArchTest
    static final ArchRule paymentStatusProjectionStoresBelongToProjectionInfrastructure = classes()
            .that().implement(PaymentStatusProjectionStore.class)
            .should().resideInAPackage("..infrastructure.projection..");

    @ArchTest
    static final ArchRule approvedExpenseAmountReadersBelongToLookupInfrastructure = classes()
            .that().implement(ApprovedExpenseAmountReader.class)
            .should().resideInAPackage("..infrastructure.lookup..");

    @ArchTest
    static final ArchRule onionDoesNotUseFixedClaimCommandDispatchers = noClasses()
            .should().haveSimpleNameContaining("ClaimCommandDispatcher");

    @ArchTest
    static final ArchRule onionDoesNotUseHexagonalPrimaryPortPackages = noClasses()
            .should().resideInAnyPackage("..port.in..", "..port.out..");

    @ArchTest
    static final ArchRule onionDoesNotUseHexagonalPrimaryPorts = noClasses()
            .should().beAnnotatedWith(PrimaryPort.class);
}
