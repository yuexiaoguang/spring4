package org.springframework.transaction.aspectj;

import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concrete AspectJ transaction aspect using Spring's
 * {@link org.springframework.transaction.annotation.Transactional} annotation.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class
 * (and/or methods within that class), <i>not</i> the interface (if any) that
 * the class implements. AspectJ follows Java's rule that annotations on
 * interfaces are <i>not</i> inherited.
 *
 * <p>An @Transactional annotation on a class specifies the default transaction
 * semantics for the execution of any <b>public</b> operation in the class.
 *
 * <p>An @Transactional annotation on a method within the class overrides the
 * default transaction semantics given by the class annotation (if present).
 * Any method may be annotated (regardless of visibility). Annotating
 * non-public methods directly is the only way to get transaction demarcation
 * for the execution of such operations.
 */
public aspect AnnotationTransactionAspect extends AbstractTransactionAspect {

	public AnnotationTransactionAspect() {
		super(new AnnotationTransactionAttributeSource(false));
	}

	/**
	 * Matches the execution of any public method in a type with the Transactional
	 * annotation, or any subtype of a type with the Transactional annotation.
	 */
	private pointcut executionOfAnyPublicMethodInAtTransactionalType() :
		execution(public * ((@Transactional *)+).*(..)) && within(@Transactional *);

	/**
	 * Matches the execution of any method with the Transactional annotation.
	 */
	private pointcut executionOfTransactionalMethod() :
		execution(@Transactional * *(..));

	/**
	 * Definition of pointcut from super aspect - matched join points
	 * will have Spring transaction management applied.
	 */
	protected pointcut transactionalMethodExecution(Object txObject) :
		(executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod() ) && this(txObject);

}
