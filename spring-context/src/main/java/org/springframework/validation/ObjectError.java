package org.springframework.validation;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;

/**
 * 封装对象错误, 即拒绝对象的全局原因.
 *
 * <p>有关如何为{@code ObjectError}构建消息代码列表的详细信息, 请参阅{@link DefaultMessageCodesResolver} javadoc.
 */
@SuppressWarnings("serial")
public class ObjectError extends DefaultMessageSourceResolvable {

	private final String objectName;


	/**
	 * @param objectName 受影响对象的名称
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public ObjectError(String objectName, String defaultMessage) {
		this(objectName, null, null, defaultMessage);
	}

	/**
	 * @param objectName 受影响对象的名称
	 * @param codes 用于解析此消息的代码
	 * @param arguments	用于解析此消息的参数数组
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public ObjectError(String objectName, String[] codes, Object[] arguments, String defaultMessage) {
		super(codes, arguments, defaultMessage);
		Assert.notNull(objectName, "Object name must not be null");
		this.objectName = objectName;
	}


	/**
	 * 返回受影响对象的名称.
	 */
	public String getObjectName() {
		return this.objectName;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass() || !super.equals(other)) {
			return false;
		}
		ObjectError otherError = (ObjectError) other;
		return getObjectName().equals(otherError.getObjectName());
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 29 + getObjectName().hashCode();
	}

	@Override
	public String toString() {
		return "Error in object '" + this.objectName + "': " + resolvableToString();
	}

}
