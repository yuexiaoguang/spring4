package org.springframework.mock.staticmock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate a test class for whose {@code @Test} methods
 * static methods on JPA-annotated {@code @Entity} classes should be mocked.
 *
 * <p>See {@link AnnotationDrivenStaticEntityMockingControl} for details.
 *
 * @deprecated as of Spring 4.3, in favor of a custom aspect for such purposes
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MockStaticEntityMethods {

}
