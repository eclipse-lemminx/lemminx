package org.eclipse.lemminx.services;

import static com.tngtech.archunit.base.DescribedPredicate.anyElementThat;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.*;

@AnalyzeClasses(packagesOf = {
		org.eclipse.lemminx.services.IXMLDocumentProvider.class,
	}, importOptions = {
		DoNotIncludeTests.class,
		DoNotIncludeJars.class
	})
@DisplayName("We don't want to expose Xerces types in our API")
public class NoXercesArchUnitTest {

	private JavaClasses classes;
	private DescribedPredicate<JavaClass> xercesType;

	@BeforeEach
	public void setUp() {
		classes = new ClassFileImporter().importClasspath();
		xercesType = resideInAPackage("org.apache.xerces..");
	}

	@Test
	@DisplayName("via public fields")
	public void testFields() {
		freeze(
			noFields()
				.that().arePublic()
				.should()
					.haveRawType(xercesType)
		)
		.check(classes);
	}

	@Test
	@DisplayName("via public method signatures")
	public void testMethods() {
		freeze(
			noMethods()
				.that().arePublic()
				.should()
					.haveRawReturnType(xercesType)
				.orShould()
					.haveRawParameterTypes(anyElementThat(xercesType))
		)
		.check(classes);
	}

	@Test
	@DisplayName("via inheritance from public classes")
	public void testInheritance() {
		freeze(
			noClasses()
				.that().arePublic()
				.should()
					.implement(xercesType)
				.orShould()
					.beAssignableFrom(xercesType)
		)
		.check(classes);
	}
	
}
