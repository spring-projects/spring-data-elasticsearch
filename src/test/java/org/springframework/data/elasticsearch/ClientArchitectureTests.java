package org.springframework.data.elasticsearch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(
		packages = { "org.springframework.data.elasticsearch", "co.elastic" },
		importOptions = ImportOption.DoNotIncludeTests.class)
class ClientArchitectureTests {

	@ArchTest public static final ArchRule elasticLibrariesShouldOnlyBeUsedInClientElc = ArchRuleDefinition
			.noClasses()
			.that()
			.resideInAPackage("org.springframework.data.elasticsearch..")
			.and()
			.resideOutsideOfPackage("org.springframework.data.elasticsearch.client.elc..")
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("co.elastic.clients..", "org.elasticsearch.client..");
}
