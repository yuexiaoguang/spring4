package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;

interface ImportRegistry {

	AnnotationMetadata getImportingClassFor(String importedClass);

	void removeImportingClass(String importingClass);

}
