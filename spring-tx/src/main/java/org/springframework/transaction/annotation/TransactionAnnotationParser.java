package org.springframework.transaction.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Strategy interface for parsing known transaction annotation types.
 * {@link AnnotationTransactionAttributeSource} delegates to such
 * parsers for supporting specific annotation types such as Spring's own
 * {@link Transactional}, JTA 1.2's {@link javax.transaction.Transactional}
 * or EJB3's {@link javax.ejb.TransactionAttribute}.
 */
public interface TransactionAnnotationParser {

	/**
	 * Parse the transaction attribute for the given method or class,
	 * based on an annotation type understood by this parser.
	 * <p>This essentially parses a known transaction annotation into Spring's metadata
	 * attribute class. Returns {@code null} if the method/class is not transactional.
	 * @param element the annotated method or class
	 * @return the configured transaction attribute, or {@code null} if none found
	 */
	TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
