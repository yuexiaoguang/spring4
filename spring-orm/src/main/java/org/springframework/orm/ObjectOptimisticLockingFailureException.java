package org.springframework.orm;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * 对映射对象的乐观锁定违规抛出异常.
 * 提供有关持久类和标识符的信息.
 */
@SuppressWarnings("serial")
public class ObjectOptimisticLockingFailureException extends OptimisticLockingFailureException {

	private Object persistentClass;

	private Object identifier;


	/**
	 * @param msg 详细信息
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param persistentClass 持久化的类
	 * @param identifier 锁定失败的对象的ID
	 */
	public ObjectOptimisticLockingFailureException(Class<?> persistentClass, Object identifier) {
		this(persistentClass, identifier, null);
	}

	/**
	 * @param persistentClass 持久化的类
	 * @param identifier 锁定失败的对象的ID
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			Class<?> persistentClass, Object identifier, Throwable cause) {

		this(persistentClass, identifier,
				"Object of class [" + persistentClass.getName() + "] with identifier [" + identifier +
				"]: optimistic locking failed", cause);
	}

	/**
	 * @param persistentClass 持久化的类
	 * @param identifier 锁定失败的对象的ID
	 * @param msg 详细信息
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			Class<?> persistentClass, Object identifier, String msg, Throwable cause) {

		super(msg, cause);
		this.persistentClass = persistentClass;
		this.identifier = identifier;
	}

	/**
	 * @param persistentClassName 持久化类的名称
	 * @param identifier 锁定失败的对象的ID
	 */
	public ObjectOptimisticLockingFailureException(String persistentClassName, Object identifier) {
		this(persistentClassName, identifier, null);
	}

	/**
	 * @param persistentClassName 持久化类的名称
	 * @param identifier 锁定失败的对象的ID
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			String persistentClassName, Object identifier, Throwable cause) {

		this(persistentClassName, identifier,
				"Object of class [" + persistentClassName + "] with identifier [" + identifier +
				"]: optimistic locking failed", cause);
	}

	/**
	 * @param persistentClassName 持久化类的名称
	 * @param identifier 锁定失败的对象的ID
	 * @param msg 详细信息
	 * @param cause 源异常
	 */
	public ObjectOptimisticLockingFailureException(
			String persistentClassName, Object identifier, String msg, Throwable cause) {

		super(msg, cause);
		this.persistentClass = persistentClassName;
		this.identifier = identifier;
	}


	/**
	 * 返回锁定失败的对象的持久类.
	 * 如果未指定Class, 则此方法返回null.
	 */
	public Class<?> getPersistentClass() {
		return (this.persistentClass instanceof Class ? (Class<?>) this.persistentClass : null);
	}

	/**
	 * 返回锁定失败的对象的持久类的名称.
	 * 将适用于Class对象和String名称.
	 */
	public String getPersistentClassName() {
		if (this.persistentClass instanceof Class) {
			return ((Class<?>) this.persistentClass).getName();
		}
		return (this.persistentClass != null ? this.persistentClass.toString() : null);
	}

	/**
	 * 返回锁定失败的对象的标识符.
	 */
	public Object getIdentifier() {
		return this.identifier;
	}

}
