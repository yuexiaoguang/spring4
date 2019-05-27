package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.text.NumberFormat;

import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

/**
 * 任何Number子类的属性编辑器, 如 Short, Integer, Long, BigInteger, Float, Double, BigDecimal.
 * 可以使用给定的NumberFormat进行 (特定于语言环境) 解析和呈现, 或者使用默认的 {@code decode} / {@code valueOf} / {@code toString}方法.
 *
 * <p>这不是要用作系统PropertyEditor, 而是用作自定义控制器代码中的特定于语言环境的数字编辑器,
 * 将用户输入的数字字符串解析为Bean的Number属性, 并以UI形式呈现它们.
 *
 * <p>在Web MVC代码中, 此编辑器通常会在{@code binder.registerCustomEditor}调用中注册.
 */
public class CustomNumberEditor extends PropertyEditorSupport {

	private final Class<? extends Number> numberClass;

	private final NumberFormat numberFormat;

	private final boolean allowEmpty;


	/**
	 * 创建一个新的CustomNumberEditor实例, 使用默认的{@code valueOf}方法进行解析, 使用{@code toString}方法进行呈现.
	 * <p>"allowEmpty"参数指出是否应允许空字符串进行解析, i.e. 解释为 null值.
	 * 否则, 在这种情况下会抛出IllegalArgumentException.
	 * 
	 * @param numberClass 要生成的Number子类
	 * @param allowEmpty 是否允许空字符串
	 * 
	 * @throws IllegalArgumentException 如果指定了无效的numberClass
	 */
	public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty) throws IllegalArgumentException {
		this(numberClass, null, allowEmpty);
	}

	/**
	 * 创建一个新的CustomNumberEditor实例, 使用给定的NumberFormat进行解析和呈现.
	 * <p>"allowEmpty"参数指出是否应允许空字符串进行解析, i.e. 解释为 null值.
	 * 否则, 在这种情况下会抛出IllegalArgumentException.
	 * 
	 * @param numberClass 要生成的Number子类
	 * @param numberFormat 用于解析和呈现的NumberFormat
	 * @param allowEmpty 是否允许空字符串
	 * 
	 * @throws IllegalArgumentException 如果指定了无效的numberClass
	 */
	public CustomNumberEditor(Class<? extends Number> numberClass,
			NumberFormat numberFormat, boolean allowEmpty) throws IllegalArgumentException {

		if (numberClass == null || !Number.class.isAssignableFrom(numberClass)) {
			throw new IllegalArgumentException("Property class must be a subclass of Number");
		}
		this.numberClass = numberClass;
		this.numberFormat = numberFormat;
		this.allowEmpty = allowEmpty;
	}


	/**
	 * 使用指定的NumberFormat从给定文本中解析Number.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (this.numberFormat != null) {
			// 使用给定的NumberFormat来解析文本.
			setValue(NumberUtils.parseNumber(text, this.numberClass, this.numberFormat));
		}
		else {
			// 使用默认valueOf方法解析文本.
			setValue(NumberUtils.parseNumber(text, this.numberClass));
		}
	}

	/**
	 * 将Number值强制转换为所需的目标类.
	 */
	@Override
	public void setValue(Object value) {
		if (value instanceof Number) {
			super.setValue(NumberUtils.convertNumberToTargetClass((Number) value, this.numberClass));
		}
		else {
			super.setValue(value);
		}
	}

	/**
	 * 使用指定的NumberFormat将Number格式化为String.
	 */
	@Override
	public String getAsText() {
		Object value = getValue();
		if (value == null) {
			return "";
		}
		if (this.numberFormat != null) {
			// 使用NumberFormat呈现值.
			return this.numberFormat.format(value);
		}
		else {
			// 使用toString方法呈现值.
			return value.toString();
		}
	}

}
