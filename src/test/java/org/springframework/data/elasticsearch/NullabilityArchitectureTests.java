package org.springframework.data.elasticsearch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = { "org.springframework.data.elasticsearch" })
class NullabilityArchitectureTests {

	@ArchTest public static final ArchRule shouldNotUseSpringNullability = ArchRuleDefinition
			.noClasses()
			.that()
			.resideInAPackage("org.springframework.data.elasticsearch..")
			.should()
			.dependOnClassesThat()
			.haveFullyQualifiedName("org.springframework.lang.NonNull")
			.orShould()
			.dependOnClassesThat()
			.haveFullyQualifiedName("org.springframework.lang.Nullable");
}
