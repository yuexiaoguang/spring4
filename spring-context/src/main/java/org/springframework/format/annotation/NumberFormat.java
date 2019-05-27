package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段或方法参数应格式化为数字.
 *
 * <p>支持按style或自定义模式字符串格式化.
 * 可以应用于任何JDK {@code java.lang.Number}类型.
 *
 * <p>对于基于style的格式设置, 请将{@link #style}属性设置为所需的{@link Style}.
 * 对于自定义格式设置, 请将{@link #pattern}属性设置为数字模式, 例如{@code #, ###.##}.
 *
 * <p>每个属性是互斥的, 因此每个注解实例只设置一个属性 (最方便的一个属性用于格式化需求).
 * 指定{@link #pattern}属性后, 它优先于{@link #style}属性.
 * 如果未指定注解属性, 则应用的默认格式是基于style的任一货币数, 具体取决于带注解的字段或方法参数类型.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface NumberFormat {

	/**
	 * 用于格式化字段的style模式.
	 * <p>对于大多数带注解的类型, 默认为{@link Style#DEFAULT}, 用于通用数字格式,
	 * 除了默认为货币格式的货币类型.
	 * 如果希望根据默认样式以外的常用样式格式化字段, 请设置此属性.
	 */
	Style style() default Style.DEFAULT;

	/**
	 * 用于格式化字段的自定义模式.
	 * <p>默认为空字符串, 表示未指定自定义模式String.
	 * 如果希望根据style之外的自定义数字模式格式化字段, 请设置此属性.
	 */
	String pattern() default "";


	/**
	 * 常用数字格式样式.
	 */
	enum Style {

		/**
		 * 注解类型的默认格式: 通常为 '数字', 但可能为货币类型的 '货币' (e.g. {@code javax.money.MonetaryAmount)}.
		 */
		DEFAULT,

		/**
		 * 当前区域设置的通用数字格式.
		 */
		NUMBER,

		/**
		 * 当前区域设置的百分比格式.
		 */
		PERCENT,

		/**
		 * 当前区域设置的货币格式.
		 */
		CURRENCY
	}

}
