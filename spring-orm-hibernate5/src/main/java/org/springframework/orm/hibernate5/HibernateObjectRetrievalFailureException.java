package org.springframework.orm.hibernate5;

import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;

import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * ObjectRetrievalFailureException的特定于Hibernate的子类.
 * 转换Hibernate的UnresolvableObjectException和WrongClassException.
 */
@SuppressWarnings("serial")
public class HibernateObjectRetrievalFailureException extends ObjectRetrievalFailureException {

	public HibernateObjectRetrievalFailureException(UnresolvableObjectException ex) {
		super(ex.getEntityName(), ex.getIdentifier(), ex.getMessage(), ex);
	}

	public HibernateObjectRetrievalFailureException(WrongClassException ex) {
		super(ex.getEntityName(), ex.getIdentifier(), ex.getMessage(), ex);
	}

}
