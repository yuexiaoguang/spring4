package org.springframework.orm.hibernate4;

import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;

import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * ObjectRetrievalFailureException的特定于Hibernate的子类.
 * 转换Hibernate的UnresolvableObjectException 和 WrongClassException.
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
