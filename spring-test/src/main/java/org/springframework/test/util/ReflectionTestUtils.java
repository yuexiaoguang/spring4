package org.springframework.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code ReflectionTestUtils}是一组基于反射的工具方法, 用于单元和集成测试场景.
 *
 * <p>有时候能够设置非{@code public}字段, 调用非{@code public} setter方法,
 * 或调用非{@code public} <em>配置是</em>或<em>生命周期</em>回调方法是有用的, 在测试涉及的代码时, 例如:
 *
 * <ul>
 * <li>ORA框架, 如JPA和Hibernate, 允许使用{@code private}或{@code protected}字段访问,
 * 而不是域实体中属性的{@code public} setter方法.</li>
 * <li>Spring支持
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
 * {@link javax.inject.Inject @Inject}, 和{@link javax.annotation.Resource @Resource}等注解,
 * 为{@code private}或{@code protected}字段, setter方法, 和配置方法提供依赖注入.</li>
 * <li>使用注解, 例如{@link javax.annotation.PostConstruct @PostConstruct}
 * 和{@link javax.annotation.PreDestroy @PreDestroy}进行生命周期回调方法.</li>
 * </ul>
 *
 * <p>此外, 此类中的几个方法为{@code static}字段提供支持 &mdash;
 * 例如, {@link #setField(Class, String, Object)}, {@link #getField(Class, String)}, 等.
 */
public abstract class ReflectionTestUtils {

	private static final String SETTER_PREFIX = "set";

	private static final String GETTER_PREFIX = "get";

	private static final Log logger = LogFactory.getLog(ReflectionTestUtils.class);

	private static final boolean springAopPresent = ClassUtils.isPresent(
			"org.springframework.aop.framework.Advised", ReflectionTestUtils.class.getClassLoader());


	/**
	 * 将提供的{@code targetObject}上给定{@code name}的{@linkplain Field field}设置为提供的{@code value}.
	 * <p>此方法委托给{@link #setField(Object, String, Object, Class)}, 为{@code type}参数提供{@code null}.
	 * 
	 * @param targetObject 要在其上设置字段的目标对象; never {@code null}
	 * @param name 要设置的字段的名称; never {@code null}
	 * @param value 要设置的值
	 */
	public static void setField(Object targetObject, String name, Object value) {
		setField(targetObject, name, value, null);
	}

	/**
	 * 使用提供的{@code targetObject}上的给定{@code name}/{@code type}将{@linkplain Field field}设置为提供的{@code value}.
	 * <p>此方法委托给 {@link #setField(Object, Class, String, Object, Class)}, 为{@code targetClass}参数提供{@code null}.
	 * 
	 * @param targetObject 要在其上设置字段的目标对象; never {@code null}
	 * @param name 要设置的字段的名称; 如果指定了{@code type}, 则可能是{@code null}
	 * @param value 要设置的值
	 * @param type 要设置的字段的类型; 如果指定了{@code name}, 则可能是{@code null}
	 */
	public static void setField(Object targetObject, String name, Object value, Class<?> type) {
		setField(targetObject, null, name, value, type);
	}

	/**
	 * 将提供的{@code targetClass}上给定{@code name}的静态{@linkplain Field field}设置为提供的{@code value}.
	 * <p>此方法委托给{@link #setField(Object, Class, String, Object, Class)},
	 * 为{@code targetObject}和{@code type}参数提供{@code null}.
	 * 
	 * @param targetClass 要在其上设置静态字段的目标类; never {@code null}
	 * @param name 要设置的字段的名称; never {@code null}
	 * @param value 要设置的值
	 */
	public static void setField(Class<?> targetClass, String name, Object value) {
		setField(null, targetClass, name, value, null);
	}

	/**
	 * 将提供的{@code targetClass}上给定{@code name}/{@code type}的静态{@linkplain Field field}设置为提供的{@code value}.
	 * <p>此方法委托给{@link #setField(Object, Class, String, Object, Class)}, 为{@code targetObject}参数提供{@code null}.
	 * 
	 * @param targetClass 要在其上设置静态字段的目标类; never {@code null}
	 * @param name 要设置的字段的名称; 如果指定了{@code type}, 则可能是{@code null}
	 * @param value 要设置的值
	 * @param type 要设置的字段的类型; 如果指定了{@code name}, 则可能是{@code null}
	 */
	public static void setField(Class<?> targetClass, String name, Object value, Class<?> type) {
		setField(null, targetClass, name, value, type);
	}

	/**
	 * 使用提供的{@code targetObject}/{@code targetClass}上的
	 * 给定{@code name}/{@code type}将{@linkplain Field field}设置为提供的{@code value}.
	 * <p>如果提供的{@code targetObject}是<em>代理</em>, 
	 * 则{@linkplain AopTestUtils#getUltimateTargetObject unwrapped}允许在代理的最终目标上设置字段.
	 * <p>此方法遍历类层次结构以搜索所需字段.
	 * 此外, 还会尝试让非{@code public}字段<em>可访问</em>, 从而允许设置{@code protected},
	 * {@code private}, 和<em>包级私有</em>字段.
	 * 
	 * @param targetObject 要在其上设置字段的目标对象; 如果字段是静态的, 则可以是{@code null}
	 * @param targetClass 要在其上设置字段的目标类; 如果该字段是实例字段, 则可以是{@code null}
	 * @param name 要设置的字段的名称; 如果指定了{@code type}, 则可能是{@code null}
	 * @param value 要设置的值
	 * @param type 要设置的字段的类型; 如果指定了{@code name}, 则可能是{@code null}
	 */
	public static void setField(Object targetObject, Class<?> targetClass, String name, Object value, Class<?> type) {
		Assert.isTrue(targetObject != null || targetClass != null,
			"Either targetObject or targetClass for the field must be specified");

		if (targetObject != null && springAopPresent) {
			targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
		}
		if (targetClass == null) {
			targetClass = targetObject.getClass();
		}

		Field field = ReflectionUtils.findField(targetClass, name, type);
		if (field == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find field '%s' of type [%s] on %s or target class [%s]", name, type,
					safeToString(targetObject), targetClass));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
					"Setting field '%s' of type [%s] on %s or target class [%s] to value [%s]", name, type,
					safeToString(targetObject), targetClass, value));
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, targetObject, value);
	}

	/**
	 * 从提供的{@code targetObject}获取具有给定{@code name}的{@linkplain Field field}的值.
	 * <p>此方法委托给{@link #getField(Object, Class, String)}, 为{@code targetClass}参数提供{@code null}.
	 * 
	 * @param targetObject 获取该字段的目标对象; never {@code null}
	 * @param name 要获得的字段的名称; never {@code null}
	 * 
	 * @return 该字段的当前值
	 */
	public static Object getField(Object targetObject, String name) {
		return getField(targetObject, null, name);
	}

	/**
	 * 从提供的{@code targetClass}获取带有给定{@code name}的静态{@linkplain Field field}的值.
	 * <p>此方法委托给{@link #getField(Object, Class, String)}, 为{@code targetObject}参数提供{@code null}.
	 * 
	 * @param targetClass 获取静态字段的目标类; never {@code null}
	 * @param name 要获取的字段的名称; never {@code null}
	 * 
	 * @return 该字段的当前值
	 */
	public static Object getField(Class<?> targetClass, String name) {
		return getField(null, targetClass, name);
	}

	/**
	 * 从提供的{@code targetObject}/{@code targetClass}获取具有给定{@code name}的{@linkplain Field field}的值.
	 * <p>如果提供的{@code targetObject}是<em>代理</em>,
	 * 则{@linkplain AopTestUtils#getUltimateTargetObject unwrapped}允许从代理的最终目标中检索字段.
	 * <p>此方法遍历类层次结构以搜索所需字段.
	 * 此外, 还会尝试使非{@code public}字段<em>可访问</em>, 从而允许其获得{@code protected},
	 * {@code private}, 和<em>包级私有</em>字段.
	 * 
	 * @param targetObject 获取该字段的目标对象; 如果字段是静态的, 则可以是{@code null}
	 * @param targetClass 该字段所在的目标类; 如果该字段是实例字段, 则可以是{@code null}
	 * @param name 要获得的字段的名称; never {@code null}
	 * 
	 * @return 该字段的当前值
	 */
	public static Object getField(Object targetObject, Class<?> targetClass, String name) {
		Assert.isTrue(targetObject != null || targetClass != null,
			"Either targetObject or targetClass for the field must be specified");

		if (targetObject != null && springAopPresent) {
			targetObject = AopTestUtils.getUltimateTargetObject(targetObject);
		}
		if (targetClass == null) {
			targetClass = targetObject.getClass();
		}

		Field field = ReflectionUtils.findField(targetClass, name);
		if (field == null) {
			throw new IllegalArgumentException(String.format("Could not find field '%s' on %s or target class [%s]",
					name, safeToString(targetObject), targetClass));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Getting field '%s' from %s or target class [%s]", name,
					safeToString(targetObject), targetClass));
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, targetObject);
	}

	/**
	 * 使用提供的{@code value}在提供的目标对象上使用给定的{@code name}调用setter方法.
	 * <p>此方法遍历类层次结构以搜索所需方法.
	 * 此外, 还将尝试让非{@code public}方法<em>可访问</em>, 从而允许调用{@code protected},
	 * {@code private}, 和<em>包级私有的</em> setter方法.
	 * <p>此外, 此方法支持JavaBean样式的<em>属性</em>名称.
	 * 例如, 如果在目标对象上设置{@code name}属性, 则可以传递 &quot;name&quot; 和 &quot;setName&quot; 作为方法名称.
	 * 
	 * @param target 要调用指定setter方法的目标对象
	 * @param name 要调用的setter方法的名称或相应的属性名称
	 * @param value 提供给setter方法的值
	 */
	public static void invokeSetterMethod(Object target, String name, Object value) {
		invokeSetterMethod(target, name, value, null);
	}

	/**
	 * 使用提供的{@code value}在提供的目标对象上使用给定的{@code name}调用setter方法.
	 * <p>此方法遍历类层次结构以搜索所需方法.
	 * 此外, 还将尝试让非{@code public}方法<em>可访问</em>, 从而允许调用{@code protected},
	 * {@code private}, 和<em>包级私有的</em> setter方法.
	 * <p>此外, 此方法支持JavaBean样式的<em>属性</em>名称.
	 * 例如, 如果在目标对象上设置{@code name}属性, 则可以传递 &quot;name&quot; 和 &quot;setName&quot; 作为方法名称.
	 * 
	 * @param target 要调用指定setter方法的目标对象
	 * @param name 要调用的setter方法的名称或相应的属性名称
	 * @param value 提供给setter方法的值
	 * @param type setter方法声明的形式参数类型
	 */
	public static void invokeSetterMethod(Object target, String name, Object value, Class<?> type) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");
		Class<?>[] paramTypes = (type != null ? new Class<?>[] {type} : null);

		String setterMethodName = name;
		if (!name.startsWith(SETTER_PREFIX)) {
			setterMethodName = SETTER_PREFIX + StringUtils.capitalize(name);
		}

		Method method = ReflectionUtils.findMethod(target.getClass(), setterMethodName, paramTypes);
		if (method == null && !setterMethodName.equals(name)) {
			setterMethodName = name;
			method = ReflectionUtils.findMethod(target.getClass(), setterMethodName, paramTypes);
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find setter method '%s' on %s with parameter type [%s]", setterMethodName,
					safeToString(target), type));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Invoking setter method '%s' on %s with value [%s]", setterMethodName,
					safeToString(target), value));
		}

		ReflectionUtils.makeAccessible(method);
		ReflectionUtils.invokeMethod(method, target, value);
	}

	/**
	 * 使用提供的{@code value}在提供的目标对象上使用给定的{@code name}调用getter方法.
	 * <p>此方法遍历类层次结构以搜索所需方法.
	 * 此外, 还将尝试让非{@code public}方法<em>可访问</em>, 从而允许调用{@code protected},
	 * {@code private}, 和<em>包级私有的</em> getter方法.
	 * <p>此外, 此方法支持JavaBean样式的<em>属性</em>名称.
	 * 例如, 如果在目标对象上获取{@code name}属性, 则可以传递&quot;name&quot; 和 &quot;getName&quot; 作为方法名称.
	 * 
	 * @param target 要在其上调用指定的getter方法的目标对象
	 * @param name 要调用的getter方法的名称或相应的属性名称
	 * 
	 * @return 调用返回的值
	 */
	public static Object invokeGetterMethod(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");

		String getterMethodName = name;
		if (!name.startsWith(GETTER_PREFIX)) {
			getterMethodName = GETTER_PREFIX + StringUtils.capitalize(name);
		}
		Method method = ReflectionUtils.findMethod(target.getClass(), getterMethodName);
		if (method == null && !getterMethodName.equals(name)) {
			getterMethodName = name;
			method = ReflectionUtils.findMethod(target.getClass(), getterMethodName);
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find getter method '%s' on %s", getterMethodName, safeToString(target)));
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Invoking getter method '%s' on %s", getterMethodName, safeToString(target)));
		}
		ReflectionUtils.makeAccessible(method);
		return ReflectionUtils.invokeMethod(method, target);
	}

	/**
	 * 使用提供的参数在提供的目标对象上调用具有给定{@code name}的方法.
	 * <p>此方法遍历类层次结构以搜索所需方法.
	 * 此外, 还将尝试让非{@code public}方法<em>可访问</em>, 从而允许调用{@code protected},
	 * {@code private}, 和<em>包级私有的</em>方法.
	 * 
	 * @param target 要调用指定方法的目标对象
	 * @param name 要调用的方法的名称
	 * @param args 提供给方法的参数
	 * 
	 * @return 调用结果
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeMethod(Object target, String name, Object... args) {
		Assert.notNull(target, "Target object must not be null");
		Assert.hasText(name, "Method name must not be empty");

		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(target);
			methodInvoker.setTargetMethod(name);
			methodInvoker.setArguments(args);
			methodInvoker.prepare();

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Invoking method '%s' on %s with arguments %s", name, safeToString(target),
						ObjectUtils.nullSafeToString(args)));
			}

			return (T) methodInvoker.invoke();
		}
		catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
			throw new IllegalStateException("Should never get here");
		}
	}

	private static String safeToString(Object target) {
		try {
			return String.format("target object [%s]", target);
		}
		catch (Exception ex) {
			return String.format("target of type [%s] whose toString() method threw [%s]",
				(target != null ? target.getClass().getName() : "unknown"), ex);
		}
	}
}
