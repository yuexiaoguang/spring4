package org.springframework.orm.hibernate5;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * ObjectOptimisticLockingFailureException的特定于Hibernate的子类.
 * 转换Hibernate的StaleObjectStateException, StaleStateException和OptimisticEntityLockException.
 */
@SuppressWarnings("serial")
public class HibernateOptimisticLockingFailureException extends ObjectOptimisticLockingFailureException {

	public HibernateOptimisticLockingFailureException(StaleObjectStateException ex) {
		super(ex.getEntityName(), ex.getIdentifier(), ex);
	}

	public HibernateOptimisticLockingFailureException(StaleStateException ex) {
		super(ex.getMessage(), ex);
	}

	public HibernateOptimisticLockingFailureException(OptimisticEntityLockException ex) {
		super(ex.getMessage(), ex);
	}

}
