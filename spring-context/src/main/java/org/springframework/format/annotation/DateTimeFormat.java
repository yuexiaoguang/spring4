package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段或方法参数应格式化为日期或时间.
 *
 * <p>支持按样式模式, ISO日期时间模式或自定义格式模式字符串格式化.
 * 可以应用于 {@code java.util.Date}, {@code java.util.Calendar}, {@code java.lang.Long}, Joda-Time 值类型;
 * 从Spring 4和JDK 8开始, 到JSR-310 <code>java.time</code>类型也是如此.
 *
 * <p>对于基于样式的格式设置, 请将{@link #style}属性设置为样式模式代码.
 * 代码的第一个字符是日期样式, 第二个字符是时间样式.
 * 短格式为'S'字符, 中等为'M', 长为'L', 完整为'F'.
 * 通过指定样式字符 '-'可以省略日期或时间.
 *
 * <p>对于基于ISO的格式设置, 请将{@link #iso}属性设置为所需的{@link ISO}格式, 例如{@link ISO#DATE}.
 * 对于自定义格式, 请将{@link #pattern}属性设置为DateTime模式, 例如{@code yyyy/MM/dd hh:mm:ss a}.
 *
 * <p>每个属性都是互斥的, 因此每个注解实例只能设置一个属性 (一个最方便的格式需求).
 * 指定pattern属性时, 它优先于style和ISO属性.
 * 指定{@link #iso}属性时, 它优先于style属性.
 * 如果未指定注释属性, 则应用的默认格式为基于style, 样式代码为 'SS' (短日期, 短时间).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DateTimeFormat {

	/**
	 * 用于格式化字段的style模式.
	 * <p>短日期时间默认为 'SS'.
	 * 如果希望根据默认style以外的常用样式格式化字段, 请设置此属性.
	 */
	String style() default "SS";

	/**
	 * 用于格式化字段的ISO模式.
	 * <p>可能的ISO模式在{@link ISO}枚举中定义.
	 * <p>默认{@link ISO#NONE}, 表示应忽略此属性.
	 * 如果希望根据ISO样式格式化字段, 请设置此属性.
	 */
	ISO iso() default ISO.NONE;

	/**
	 * 用于格式化字段的自定义模式.
	 * <p>默认为空字符串, 表示未指定自定义模式String.
	 * 如果希望根据style或ISO格式以外的自定义日期时间模式格式化字段, 请设置此属性.
	 * <p>Note: 此模式遵循原始的{@link java.text.SimpleDateFormat}样式, Joda-Time也支持该样式, 对溢出的严格解析语义
	 * (e.g. 拒绝2月29日的非闰年值).
	 * 因此, 'yy' 字符表示传统风格的一年, 而不是{@link java.time.format.DateTimeFormatter}规范中的"年代"
	 * (i.e. 当使用严格的解析模式执行{@code DateTimeFormatter}时, 'yy' 变成 'uu').
	 */
	String pattern() default "";


	/**
	 * 常见的ISO日期时间格式模式.
	 */
	enum ISO {

		/**
		 * 最常见的ISO日期格式 {@code yyyy-MM-dd}, e.g. "2000-10-31".
		 */
		DATE,

		/**
		 * 最常见的ISO时间格式 {@code HH:mm:ss.SSSZ}, e.g. "01:30:00.000-05:00".
		 */
		TIME,

		/**
		 * 最常见的ISO DateTime格式 {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ}, e.g. "2000-10-31T01:30:00.000-05:00".
		 * <p>如果未指定注解值, 则这是默认值.
		 */
		DATE_TIME,

		/**
		 * 表示不应应用基于ISO的格式模式.
		 */
		NONE
	}
}
