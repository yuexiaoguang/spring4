package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

import org.springframework.util.ClassUtils;

/**
 * 用于根据类型声明确定对象顺序的常规工具.
 * 处理Spring的{@link Order}注解以及{@link javax.annotation.Priority}.
 */
@SuppressWarnings("unchecked")
public abstract class OrderUtils {

	private static Class<? extends Annotation> priorityAnnotationType = null;

	static {
		try {
			priorityAnnotationType = (Class<? extends Annotation>)
					ClassUtils.forName("javax.annotation.Priority", OrderUtils.class.getClassLoader());
		}
		catch (Throwable ex) {
			// javax.annotation.Priority not available, or present but not loadable (on JDK 6)
		}
	}


	/**
	 * 返回指定{@code type}的顺序.
	 * <p>负责{@link Order @Order} 和 {@code @javax.annotation.Priority}.
	 * 
	 * @param type 要处理的类型
	 * 
	 * @return 顺序值, 或{@code null}如果找不到
	 */
	public static Integer getOrder(Class<?> type) {
		return getOrder(type, null);
	}

	/**
	 * 返回指定的{@code type}上的顺序; 如果找不到, 则返回指定的默认值.
	 * <p>负责{@link Order @Order}和{@code @javax.annotation.Priority}.
	 * 
	 * @param type 要处理的类型
	 * 
	 * @return 优先级值, 或指定的默认顺序
	 */
	public static Integer getOrder(Class<?> type, Integer defaultOrder) {
		Order order = AnnotationUtils.findAnnotation(type, Order.class);
		if (order != null) {
			return order.value();
		}
		Integer priorityOrder = getPriority(type);
		if (priorityOrder != null) {
			return priorityOrder;
		}
		return defaultOrder;
	}

	/**
	 * 返回在指定类型上声明的{@code javax.annotation.Priority}注解的值, 或{@code null}.
	 * 
	 * @param type 要处理的类型
	 * 
	 * @return 如果声明了注释, 则为优先级值; 如果没有, 则为{@code null}
	 */
	public static Integer getPriority(Class<?> type) {
		if (priorityAnnotationType != null) {
			Annotation priority = AnnotationUtils.findAnnotation(type, priorityAnnotationType);
			if (priority != null) {
				return (Integer) AnnotationUtils.getValue(priority);
			}
		}
		return null;
	}
}
