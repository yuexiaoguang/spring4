package org.springframework.orm.jdo;

import javax.jdo.JDOHelper;
import javax.jdo.JDOOptimisticVerificationException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * ObjectOptimisticLockingFailureException的JDO特定子类.
 * 转换JDO的JDOOptimisticVerificationException.
 */
@SuppressWarnings("serial")
public class JdoOptimisticLockingFailureException extends ObjectOptimisticLockingFailureException {

	public JdoOptimisticLockingFailureException(JDOOptimisticVerificationException ex) {
		// 从JDOException中提取有关失败对象的信息.
		super((ex.getFailedObject() != null ? ex.getFailedObject().getClass() : null),
				(ex.getFailedObject() != null ? JDOHelper.getObjectId(ex.getFailedObject()) : null),
				ex.getMessage(), ex);
	}

}
