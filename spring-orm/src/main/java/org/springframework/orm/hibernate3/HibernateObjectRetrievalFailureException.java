package org.springframework.orm.hibernate3;

import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;

import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * Hibernate-specific subclass of ObjectRetrievalFailureException.
 * Converts Hibernate's UnresolvableObjectException and WrongClassException.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings("serial")
public class HibernateObjectRetrievalFailureException extends ObjectRetrievalFailureException {

	public HibernateObjectRetrievalFailureException(UnresolvableObjectException ex) {
		super(ex.getEntityName(), ex.getIdentifier(), ex.getMessage(), ex);
	}

	public HibernateObjectRetrievalFailureException(WrongClassException ex) {
		super(ex.getEntityName(), ex.getIdentifier(), ex.getMessage(), ex);
	}

}
