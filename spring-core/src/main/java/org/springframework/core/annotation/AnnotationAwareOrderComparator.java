package org.springframework.core.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.DecoratingProxy;
import org.springframework.core.OrderComparator;

/**
 * {@code AnnotationAwareOrderComparator}是{@link OrderComparator}的扩展,
 * 它支持Spring的{@link org.springframework.core.Ordered}接口以及 {@link Order @Order}
 * 和{@link javax.annotation.Priority @Priority}注解, 使用{@code Ordered}实例提供的排序值覆盖静态定义的注解值.
 *
 * <p>有关非有序对象的排序语义的详细信息, 请参阅Javadoc以获取{@link OrderComparator}.
 */
public class AnnotationAwareOrderComparator extends OrderComparator {

	/**
	 * {@code AnnotationAwareOrderComparator}的共享默认实例.
	 */
	public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();


	/**
	 * 此实现检查各种元素上的{@link Order @Order}或{@link javax.annotation.Priority @Priority},
	 * 除了检查超类中的{@link org.springframework.core.Ordered}.
	 */
	@Override
	protected Integer findOrder(Object obj) {
		// 检查常规Ordered接口
		Integer order = super.findOrder(obj);
		if (order != null) {
			return order;
		}

		// 检查各种元素的@Order和@Priority
		if (obj instanceof Class) {
			return OrderUtils.getOrder((Class<?>) obj);
		}
		else if (obj instanceof Method) {
			Order ann = AnnotationUtils.findAnnotation((Method) obj, Order.class);
			if (ann != null) {
				return ann.value();
			}
		}
		else if (obj instanceof AnnotatedElement) {
			Order ann = AnnotationUtils.getAnnotation((AnnotatedElement) obj, Order.class);
			if (ann != null) {
				return ann.value();
			}
		}
		else if (obj != null) {
			order = OrderUtils.getOrder(obj.getClass());
			if (order == null && obj instanceof DecoratingProxy) {
				order = OrderUtils.getOrder(((DecoratingProxy) obj).getDecoratedClass());
			}
		}

		return order;
	}

	/**
	 * 此实现检索 @{@link javax.annotation.Priority}值, 允许在常规@{@link Order}注解上添加其他语义:
	 * 通常, 在多个匹配但仅返回一个对象的情况下, 选择一个对象而不是另一个对象.
	 */
	@Override
	public Integer getPriority(Object obj) {
		Integer priority = null;
		if (obj instanceof Class) {
			priority = OrderUtils.getPriority((Class<?>) obj);
		}
		else if (obj != null) {
			priority = OrderUtils.getPriority(obj.getClass());
			if (priority == null && obj instanceof DecoratingProxy) {
				priority = OrderUtils.getPriority(((DecoratingProxy) obj).getDecoratedClass());
			}
		}
		return priority;
	}


	/**
	 * 使用默认的AnnotationAwareOrderComparator对给定的List进行排序.
	 * <p>优化, 跳过大小为0或1的列表排序, 以避免不必要的数组提取.
	 * 
	 * @param list 要排序的List
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			Collections.sort(list, INSTANCE);
		}
	}

	/**
	 * 使用默认的AnnotationAwareOrderComparator对给定数组进行排序.
	 * <p>优化, 跳过大小为0或1的列表排序, 以避免不必要的数组提取.
	 * 
	 * @param array 要排序的数组
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * 使用默认的AnnotationAwareOrderComparator对给定数组或List进行排序.
	 * 在给定任何其他值时, 只需跳过排序.
	 * <p>优化, 跳过大小为0或1的列表排序, 以避免不必要的数组提取.
	 * 
	 * @param value 要排序的数组或List
	 */
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[]) {
			sort((Object[]) value);
		}
		else if (value instanceof List) {
			sort((List<?>) value);
		}
	}
}
