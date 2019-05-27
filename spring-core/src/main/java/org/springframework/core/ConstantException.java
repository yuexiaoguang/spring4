package org.springframework.core;

/**
 * 当{@link Constants}类被要求输入无效的常量名时抛出异常.
 */
@SuppressWarnings("serial")
public class ConstantException extends IllegalArgumentException {

	/**
	 * 请求无效的常量名称时抛出.
	 * 
	 * @param className 包含常量定义的类的名称
	 * @param field 无效的常量名称
	 * @param message 问题的描述
	 */
	public ConstantException(String className, String field, String message) {
		super("Field '" + field + "' " + message + " in class [" + className + "]");
	}

	/**
	 * 查找无效常量值时抛出.
	 * 
	 * @param className 包含常量定义的类的名称
	 * @param namePrefix 搜索到的常量名称的前缀
	 * @param value 查找的常量值
	 */
	public ConstantException(String className, String namePrefix, Object value) {
		super("No '" + namePrefix + "' field with value '" + value + "' found in class [" + className + "]");
	}

}
