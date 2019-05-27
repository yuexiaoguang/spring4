package org.springframework.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * 此类可用于解析在public static final成员中包含常量定义的其他类.
 * 此类的{@code asXXXX}方法允许通过其字符串名称访问这些常量值.
 *
 * <p>考虑包含{@code public final static int CONSTANT1 = 66;}的类Foo
 * 这个包装{@code Foo.class}的类的实例将从它的{@code asNumber}方法返回给定参数{@code "CONSTANT1"}的常量值66.
 *
 * <p>这个类非常适合在PropertyEditors中使用, 使它们能够识别与常量本身相同的名称, 并使它们无需维护自己的映射.
 */
public class Constants {

	/** 内省的类的名称 */
	private final String className;

	/** Map from String field name to object value */
	private final Map<String, Object> fieldCache = new HashMap<String, Object>();


	/**
	 * 创建一个包装给定类的新Constants转换器类.
	 * <p>无论其类型如何, 所有<b>public</b> static final 变量都将被公开.
	 * 
	 * @param clazz 要分析的类
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code clazz}是{@code null}
	 */
	public Constants(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		this.className = clazz.getName();
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (ReflectionUtils.isPublicStaticFinal(field)) {
				String name = field.getName();
				try {
					Object value = field.get(null);
					this.fieldCache.put(name, value);
				}
				catch (IllegalAccessException ex) {
					// just leave this field and continue
				}
			}
		}
	}


	/**
	 * 返回要分析的类的名称.
	 */
	public final String getClassName() {
		return this.className;
	}

	/**
	 * 返回暴露的常量数.
	 */
	public final int getSize() {
		return this.fieldCache.size();
	}

	/**
	 * 将字段缓存公开给子类:
	 * String字段名称到对象值的Map.
	 */
	protected final Map<String, Object> getFieldCache() {
		return this.fieldCache;
	}


	/**
	 * 返回一个常量值, 强制转换为Number.
	 * 
	 * @param code 该字段的名称 (never {@code null})
	 * 
	 * @return Number值
	 * @throws ConstantException 如果找不到字段名称或类型与Number不兼容
	 */
	public Number asNumber(String code) throws ConstantException {
		Object obj = asObject(code);
		if (!(obj instanceof Number)) {
			throw new ConstantException(this.className, code, "not a Number");
		}
		return (Number) obj;
	}

	/**
	 * 返回一个常量值.
	 * 
	 * @param code 该字段的名称 (never {@code null})
	 * 
	 * @return the String value
	 * 即使它不是一个字符串也可以工作(调用{@code toString()}).
	 * @throws ConstantException 如果找不到字段名称
	 */
	public String asString(String code) throws ConstantException {
		return asObject(code).toString();
	}

	/**
	 * 解析给定的String (接受大写或小写)并返回适当的值, 如果它是正在分析的类中的常量字段的名称.
	 * 
	 * @param code 该字段的名称 (never {@code null})
	 * 
	 * @return Object值
	 * @throws ConstantException 如果没有这样的字段
	 */
	public Object asObject(String code) throws ConstantException {
		Assert.notNull(code, "Code must not be null");
		String codeToUse = code.toUpperCase(Locale.ENGLISH);
		Object val = this.fieldCache.get(codeToUse);
		if (val == null) {
			throw new ConstantException(this.className, codeToUse, "not found");
		}
		return val;
	}


	/**
	 * 返回常量的给定组的所有名称.
	 * <p>请注意, 此方法假定常量是根据常量值的标准Java约定命名的 (i.e. 所有都大写).
	 * 在此方法的主要逻辑开始之前, 提供的{@code namePrefix}将是大写的 (以区域设置不敏感的方式).
	 * 
	 * @param namePrefix 要搜索的常量名称的前缀 (may be {@code null})
	 * 
	 * @return 常量名称
	 */
	public Set<String> getNames(String namePrefix) {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<String> names = new HashSet<String>();
		for (String code : this.fieldCache.keySet()) {
			if (code.startsWith(prefixToUse)) {
				names.add(code);
			}
		}
		return names;
	}

	/**
	 * 返回给定bean属性名称的常量组的所有名称.
	 * 
	 * @param propertyName bean属性的名称
	 * 
	 * @return 值
	 */
	public Set<String> getNamesForProperty(String propertyName) {
		return getNames(propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 返回给定常量组的所有名称.
	 * <p>请注意, 此方法假定常量是根据常量值的标准Java约定命名的 (i.e. 所有都大写).
	 * 在此方法的主要逻辑开始之前, 提供的{@code namePrefix}将是大写的 (以区域设置不敏感的方式).
	 * 
	 * @param nameSuffix 要搜索的常量名称的前缀(may be {@code null})
	 * 
	 * @return 常量名称
	 */
	public Set<String> getNamesForSuffix(String nameSuffix) {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<String> names = new HashSet<String>();
		for (String code : this.fieldCache.keySet()) {
			if (code.endsWith(suffixToUse)) {
				names.add(code);
			}
		}
		return names;
	}


	/**
	 * 返回给定常量组的所有值.
	 * <p>请注意, 此方法假定常量是根据常量值的标准Java约定命名的 (i.e. 所有都大写).
	 * 在此方法的主要逻辑开始之前, 提供的{@code namePrefix}将是大写的 (以区域设置不敏感的方式).
	 * 
	 * @param namePrefix 要搜索的常量名称的前缀 (may be {@code null})
	 * 
	 * @return 所有值
	 */
	public Set<Object> getValues(String namePrefix) {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<Object> values = new HashSet<Object>();
		for (String code : this.fieldCache.keySet()) {
			if (code.startsWith(prefixToUse)) {
				values.add(this.fieldCache.get(code));
			}
		}
		return values;
	}

	/**
	 * 返回给定bean属性名称的常量组的所有值.
	 * 
	 * @param propertyName bean属性的名称
	 * 
	 * @return 所有值
	 */
	public Set<Object> getValuesForProperty(String propertyName) {
		return getValues(propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 返回给定常量组的所有值.
	 * <p>请注意, 此方法假定常量是根据常量值的标准Java约定命名的 (i.e. 所有都大写).
	 * 在此方法的主要逻辑开始之前, 提供的{@code nameSuffix}将是大写的 (以区域设置不敏感的方式).
	 * 
	 * @param nameSuffix 要搜索的常量名称的后缀 (may be {@code null})
	 * 
	 * @return 所有值
	 */
	public Set<Object> getValuesForSuffix(String nameSuffix) {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		Set<Object> values = new HashSet<Object>();
		for (String code : this.fieldCache.keySet()) {
			if (code.endsWith(suffixToUse)) {
				values.add(this.fieldCache.get(code));
			}
		}
		return values;
	}


	/**
	 * 在给定的常量组中查找给定值.
	 * <p>将返回第一个匹配的.
	 * 
	 * @param value 要查找的常量值
	 * @param namePrefix 要搜索的常量名称的前缀 (may be {@code null})
	 * 
	 * @return 常量字段的名称
	 * @throws ConstantException 如果找不到该值
	 */
	public String toCode(Object value, String namePrefix) throws ConstantException {
		String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
		for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
			if (entry.getKey().startsWith(prefixToUse) && entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		throw new ConstantException(this.className, prefixToUse, value);
	}

	/**
	 * 在常量组中查找给定bean属性名称的给定值. 将返回第一个匹配的.
	 * 
	 * @param value 要查找的常量值
	 * @param propertyName bean属性名称
	 * 
	 * @return 常量字段的名称
	 * @throws ConstantException 如果找不到该值
	 */
	public String toCodeForProperty(Object value, String propertyName) throws ConstantException {
		return toCode(value, propertyToConstantNamePrefix(propertyName));
	}

	/**
	 * 在给定的常量组中查找给定值.
	 * <p>将返回第一个匹配的.
	 * 
	 * @param value 要查找的常量值
	 * @param nameSuffix 要搜索的常量名称的后缀 (may be {@code null})
	 * 
	 * @return 常量字段的名称
	 * @throws ConstantException 如果找不到该值
	 */
	public String toCodeForSuffix(Object value, String nameSuffix) throws ConstantException {
		String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
		for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
			if (entry.getKey().endsWith(suffixToUse) && entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		throw new ConstantException(this.className, suffixToUse, value);
	}


	/**
	 * 将给定的bean属性名称转换为常量名称前缀.
	 * <p>使用常见的命名习惯用法: 将所有小写字符转换为大写字母, 并使用下划线添加大写字符.
	 * <p>Example: "imageSize" -> "IMAGE_SIZE"<br>
	 * Example: "imagesize" -> "IMAGESIZE".<br>
	 * Example: "ImageSize" -> "_IMAGE_SIZE".<br>
	 * Example: "IMAGESIZE" -> "_I_M_A_G_E_S_I_Z_E"
	 * 
	 * @param propertyName bean属性的名称
	 * 
	 * @return 相应的常量名称前缀
	 */
	public String propertyToConstantNamePrefix(String propertyName) {
		StringBuilder parsedPrefix = new StringBuilder();
		for (int i = 0; i < propertyName.length(); i++) {
			char c = propertyName.charAt(i);
			if (Character.isUpperCase(c)) {
				parsedPrefix.append("_");
				parsedPrefix.append(c);
			}
			else {
				parsedPrefix.append(Character.toUpperCase(c));
			}
		}
		return parsedPrefix.toString();
	}

}
