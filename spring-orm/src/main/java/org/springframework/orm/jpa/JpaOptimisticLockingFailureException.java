package org.springframework.orm.jpa;

import javax.persistence.OptimisticLockException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * ObjectOptimisticLockingFailureException的JPA特定子类.
 * 转换JPA的OptimisticLockException.
 */
@SuppressWarnings("serial")
public class JpaOptimisticLockingFailureException extends ObjectOptimisticLockingFailureException {

	public JpaOptimisticLockingFailureException(OptimisticLockException ex) {
		super(ex.getMessage(), ex);
	}

}
