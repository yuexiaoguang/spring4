package org.springframework.expression.spel.ast;

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ClassUtils;

/**
 * 在解析和评估期间使用的实用方法 (格式化器 etc).
 */
public class FormatHelper {

	/**
	 * 使用指定的参数为给定的方法名生成可读的表示.
	 * 
	 * @param name 方法名
	 * @param argumentTypes 方法的参数类型
	 * 
	 * @return 一个格式很好的表示, e.g. {@code foo(String,int)}
	 */
	public static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
		StringBuilder sb = new StringBuilder(name);
		sb.append("(");
		for (int i = 0; i < argumentTypes.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			TypeDescriptor typeDescriptor = argumentTypes.get(i);
			if (typeDescriptor != null) {
				sb.append(formatClassNameForMessage(typeDescriptor.getType()));
			}
			else {
				sb.append(formatClassNameForMessage(null));
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * 确定给定Class对象的可读名称.
	 * <p>String数组的格式名称为 "java.lang.String[]".
	 * 
	 * @param clazz 要格式化其名称的Class
	 * 
	 * @return 格式化的字符串, 适合消息
	 */
	public static String formatClassNameForMessage(Class<?> clazz) {
		return (clazz != null ? ClassUtils.getQualifiedName(clazz) : "null");
	}

}
