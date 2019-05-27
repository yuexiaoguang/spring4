package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 向从{@link org.springframework.stereotype.Component}或{@link Bean}派生的bean定义添加文本描述.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Description {

	/**
	 * 与bean定义关联的文本描述.
	 */
	String value();

}
