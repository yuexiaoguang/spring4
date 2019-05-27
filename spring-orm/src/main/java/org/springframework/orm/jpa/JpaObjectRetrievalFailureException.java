package org.springframework.orm.jpa;

import javax.persistence.EntityNotFoundException;

import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * 特定于JPA的ObjectRetrievalFailureException的子类.
 * 转换JPA的EntityNotFoundException.
 */
@SuppressWarnings("serial")
public class JpaObjectRetrievalFailureException extends ObjectRetrievalFailureException {

	public JpaObjectRetrievalFailureException(EntityNotFoundException ex) {
		super(ex.getMessage(), ex);
	}

}
