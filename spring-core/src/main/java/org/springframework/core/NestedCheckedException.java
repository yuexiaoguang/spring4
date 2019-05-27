package org.springframework.core;

/**
 * 使用根本原因包装受检的{@code Exceptions}.
 *
 * <p>这个类是{@code abstract}来强制程序员扩展类.
 * {@code getMessage}将包含嵌套的异常信息;
 * {@code printStackTrace}和其他类似方法将委托给包装的异常.
 *
 * <p>这个类和{@link NestedRuntimeException}类之间的相似性是不可避免的, 因为Java强制这两个类具有不同的超类
 * (啊, 具体继承的不灵活性!).
 */
public abstract class NestedCheckedException extends Exception {

	/** Use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = 7100714597678207546L;

	static {
		// 在调用getMessage()时, 实时地加载NestedExceptionUtils类以避免OSGi上的类加载器死锁问题. Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	public NestedCheckedException(String msg) {
		super(msg);
	}

	public NestedCheckedException(String msg, Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息, 包括嵌套异常中的消息.
	 */
	@Override
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}


	public Throwable getRootCause() {
		return NestedExceptionUtils.getRootCause(this);
	}

	/**
	 * 检索此异常的最具体原因, 即最内层原因(根本原因)或此异常本身.
	 * <p>与{@link #getRootCause()}不同之处在于, 如果没有根本原因, 它会回退到当前异常.
	 * 
	 * @return 最具体的原因 (never {@code null})
	 */
	public Throwable getMostSpecificCause() {
		Throwable rootCause = getRootCause();
		return (rootCause != null ? rootCause : this);
	}

	/**
	 * 检查此异常是否包含给定类型的异常:
	 * 要么它是给定的类本身, 要么它包含给定类型的嵌套原因.
	 * 
	 * @param exType 要查找的异常类型
	 * 
	 * @return 是否存在指定类型的嵌套异常
	 */
	public boolean contains(Class<?> exType) {
		if (exType == null) {
			return false;
		}
		if (exType.isInstance(this)) {
			return true;
		}
		Throwable cause = getCause();
		if (cause == this) {
			return false;
		}
		if (cause instanceof NestedCheckedException) {
			return ((NestedCheckedException) cause).contains(exType);
		}
		else {
			while (cause != null) {
				if (exType.isInstance(cause)) {
					return true;
				}
				if (cause.getCause() == cause) {
					break;
				}
				cause = cause.getCause();
			}
			return false;
		}
	}

}
