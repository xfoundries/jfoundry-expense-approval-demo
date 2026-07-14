package io.github.xfoundries.demo.expenseapproval;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import org.jfoundry.test.archunit.JFoundryRules;

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
}
