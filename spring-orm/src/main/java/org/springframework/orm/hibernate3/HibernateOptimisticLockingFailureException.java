package org.springframework.orm.hibernate3;

import org.hibernate.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Hibernate-specific subclass of ObjectOptimisticLockingFailureException.
 * Converts Hibernate's StaleObjectStateException, StaleStateException
 * and OptimisticLockException.
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings("serial")
public class HibernateOptimisticLockingFailureException extends ObjectOptimisticLockingFailureException {

	public HibernateOptimisticLockingFailureException(StaleObjectStateException ex) {
		super(ex.getEntityName(), ex.getIdentifier(), ex);
	}

	public HibernateOptimisticLockingFailureException(StaleStateException ex) {
		super(ex.getMessage(), ex);
	}

	public HibernateOptimisticLockingFailureException(OptimisticLockException ex) {
		super(ex.getMessage(), ex);
	}

}
