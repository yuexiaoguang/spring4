package org.springframework.orm.jdo;

import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;

import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * ObjectRetrievalFailureException的JDO特定子类.
 * 转换JDO的JDOObjectNotFoundException.
 */
@SuppressWarnings("serial")
public class JdoObjectRetrievalFailureException extends ObjectRetrievalFailureException {

	public JdoObjectRetrievalFailureException(JDOObjectNotFoundException ex) {
		// 从JDOException中提取有关失败对象的信息.
		super((ex.getFailedObject() != null ? ex.getFailedObject().getClass() : null),
				(ex.getFailedObject() != null ? JDOHelper.getObjectId(ex.getFailedObject()) : null),
				ex.getMessage(), ex);
	}

}
