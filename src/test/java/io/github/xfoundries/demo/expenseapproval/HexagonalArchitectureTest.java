package io.github.xfoundries.demo.expenseapproval;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.jfoundry.test.archunit.JFoundryRules;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .importPackages("io.github.xfoundries.demo.expenseapproval");

    @TestFactory
    Stream<DynamicTest> jfoundryRules() {
        return tests("hexagonal", JFoundryRules.hexagonalStrict());
    }

    @TestFactory
    Stream<DynamicTest> jmoleculesDddRules() {
        return tests("jmolecules-ddd", JFoundryRules.jmoleculesDdd());
    }

    @TestFactory
    Stream<DynamicTest> aggregateRepositoryRules() {
        return tests("aggregate-repository", JFoundryRules.aggregateRepositoryConventions());
    }

    private static Stream<DynamicTest> tests(String group, ArchRule[] rules) {
        return IntStream.range(0, rules.length)
                .mapToObj(index -> DynamicTest.dynamicTest(
                        group + " rule " + (index + 1),
                        () -> rules[index].check(CLASSES)));
    }
}
