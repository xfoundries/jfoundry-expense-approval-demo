package io.github.xfoundries.demo.expenseapproval;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import io.github.xfoundries.demo.expenseapproval.application.payment.port.out.PaymentStatusProjectionPort;
import org.jfoundry.test.archunit.HexagonalAdapterPackageConvention;
import org.jfoundry.test.archunit.JFoundryRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "io.github.xfoundries.demo.expenseapproval")
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchTests jfoundryRules = JFoundryRules.hexagonalStrict();

    @ArchTest
    static final ArchRule adapterPackages = JFoundryRules.hexagonalAdapterPackageConvention(
            HexagonalAdapterPackageConvention.IN_OUT);

    @ArchTest
    static final ArchRule paymentStatusProjectionAdapterPackage = classes()
            .that().implement(PaymentStatusProjectionPort.class)
            .should().resideInAPackage("..adapter.out.projection..");

    @ArchTest
    static final ArchTests jmoleculesDddRules = JFoundryRules.jmoleculesDdd();

    @ArchTest
    static final ArchTests aggregateRepositoryRules = JFoundryRules.aggregateRepositoryConventions();

    @ArchTest
    static final ArchTests cqrsRules = JFoundryRules.cqrs();
}
