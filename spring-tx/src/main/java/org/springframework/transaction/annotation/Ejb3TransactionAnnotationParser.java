package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import javax.ejb.ApplicationException;
import javax.ejb.TransactionAttributeType;

import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * 解析EJB3的{@link javax.ejb.TransactionAttribute}注解的策略实现.
 */
@SuppressWarnings("serial")
public class Ejb3TransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

	@Override
	public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
		javax.ejb.TransactionAttribute ann = element.getAnnotation(javax.ejb.TransactionAttribute.class);
		if (ann != null) {
			return parseTransactionAnnotation(ann);
		}
		else {
			return null;
		}
	}

	public TransactionAttribute parseTransactionAnnotation(javax.ejb.TransactionAttribute ann) {
		return new Ejb3TransactionAttribute(ann.value());
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof Ejb3TransactionAnnotationParser);
	}

	@Override
	public int hashCode() {
		return Ejb3TransactionAnnotationParser.class.hashCode();
	}


	/**
	 * EJB3特定的TransactionAttribute, 实现EJB3的回滚规则, 这些规则基于带注解的异常.
	 */
	private static class Ejb3TransactionAttribute extends DefaultTransactionAttribute {

		public Ejb3TransactionAttribute(TransactionAttributeType type) {
			setPropagationBehaviorName(PREFIX_PROPAGATION + type.name());
		}

		@Override
		public boolean rollbackOn(Throwable ex) {
			ApplicationException ann = ex.getClass().getAnnotation(ApplicationException.class);
			return (ann != null ? ann.rollback() : super.rollbackOn(ex));
		}
	}

}
