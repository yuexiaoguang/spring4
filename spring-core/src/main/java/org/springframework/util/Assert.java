package org.springframework.util;

import java.util.Collection;
import java.util.Map;

/**
 * 断言实用程序类, 它有助于验证参数.
 *
 * <p>用于在运行时尽早清楚地识别程序员错误.
 *
 * <p>例如, 如果公共方法的约定声明它不允许{@code null}参数, 则可以使用{@code Assert}来验证该约定.
 * 这样做可以清楚地指示约定违规发生时, 保护类的不变量.
 *
 * <p>通常用于验证方法参数而不是配置属性, 以检查通常是程序员错误, 而非配置错误的情况.
 * 与配置初始化代码相比, 在这种方法中回退到默认值通常没有意义.
 *
 * <p>该类与JUnit的断言库类似. 如果参数值被视为无效, 则抛出{@link IllegalArgumentException} (通常).
 * 例如:
 *
 * <pre class="code">
 * Assert.notNull(clazz, "The class must not be null");
 * Assert.isTrue(i > 0, "The value must be greater than zero");</pre>
 *
 * <p>主要供框架内部使用; 请考虑
 * <a href="http://commons.apache.org/proper/commons-lang/">Apache's Commons Lang</a>
 * 提供更全面的{@code String}工具套件.
 */
public abstract class Assert {

	/**
	 * 断言 boolean 表达式, 如果表达式求值为{@code false}, 则抛出{@code IllegalStateException}.
	 * <p>如果希望在断言失败时抛出{@code IllegalArgumentException}, 请调用{@link #isTrue}.
	 * <pre class="code">Assert.state(id == null, "The id property must not already be initialized");</pre>
	 * 
	 * @param expression boolean表达式
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalStateException 如果{@code expression}是{@code false}
	 */
	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #state(boolean, String)}
	 */
	@Deprecated
	public static void state(boolean expression) {
		state(expression, "[Assertion failed] - this state invariant must be true");
	}

	/**
	 * 断言 boolean 表达式, 如果表达式求值为{@code false}, 则抛出{@code IllegalArgumentException}.
	 * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
	 * 
	 * @param expression boolean表达式
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果{@code expression}是{@code false}
	 */
	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #isTrue(boolean, String)}
	 */
	@Deprecated
	public static void isTrue(boolean expression) {
		isTrue(expression, "[Assertion failed] - this expression must be true");
	}

	/**
	 * 断言对象是{@code null}.
	 * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
	 * 
	 * @param object 要检查的对象
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果对象不是{@code null}
	 */
	public static void isNull(Object object, String message) {
		if (object != null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #isNull(Object, String)}
	 */
	@Deprecated
	public static void isNull(Object object) {
		isNull(object, "[Assertion failed] - the object argument must be null");
	}

	/**
	 * 断言对象不是{@code null}.
	 * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
	 * 
	 * @param object 要检查的对象
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果对象是{@code null}
	 */
	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #notNull(Object, String)}
	 */
	@Deprecated
	public static void notNull(Object object) {
		notNull(object, "[Assertion failed] - this argument is required; it must not be null");
	}

	/**
	 * 断言给定的String不为空; 也就是说, 它不能是{@code null}或空字符串.
	 * <pre class="code">Assert.hasLength(name, "Name must not be empty");</pre>
	 * 
	 * @param text 要检查的字符串
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果文本为空
	 */
	public static void hasLength(String text, String message) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #hasLength(String, String)}
	 */
	@Deprecated
	public static void hasLength(String text) {
		hasLength(text,
				"[Assertion failed] - this String argument must have length; it must not be null or empty");
	}

	/**
	 * 断言给定的String包含有效的文本内容; 也就是说, 它不能是{@code null}, 并且必须至少包含一个非空格字符.
	 * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
	 * 
	 * @param text 要检查的字符串
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果文本不包含有效的文本内容
	 */
	public static void hasText(String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #hasText(String, String)}
	 */
	@Deprecated
	public static void hasText(String text) {
		hasText(text,
				"[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
	}

	/**
	 * 断言给定文本不包含给定的子字符串.
	 * <pre class="code">Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");</pre>
	 * 
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果文本包含子字符串
	 */
	public static void doesNotContain(String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #doesNotContain(String, String, String)}
	 */
	@Deprecated
	public static void doesNotContain(String textToSearch, String substring) {
		doesNotContain(textToSearch, substring,
				"[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
	}

	/**
	 * 断言数组包含元素; 也就是说, 它不能是{@code null}并且必须至少包含一个元素.
	 * <pre class="code">Assert.notEmpty(array, "The array must contain elements");</pre>
	 * 
	 * @param array 要检查的数组
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果对象数组是{@code null}或不包含任何元素
	 */
	public static void notEmpty(Object[] array, String message) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Object[], String)}
	 */
	@Deprecated
	public static void notEmpty(Object[] array) {
		notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
	}

	/**
	 * 断言数组不包含{@code null}元素.
	 * <p>Note: 如果数组为空, 不要抱怨!
	 * <pre class="code">Assert.noNullElements(array, "The array must contain non-null elements");</pre>
	 * 
	 * @param array 要检查的数组
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果对象数组包含{@code null}元素
	 */
	public static void noNullElements(Object[] array, String message) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #noNullElements(Object[], String)}
	 */
	@Deprecated
	public static void noNullElements(Object[] array) {
		noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
	}

	/**
	 * 断言集合包含元素; 也就是说, 它不能是{@code null}并且必须至少包含一个元素.
	 * <pre class="code">Assert.notEmpty(collection, "Collection must contain elements");</pre>
	 * 
	 * @param collection 要检查的集合
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果集合是{@code null}或者不包含任何元素
	 */
	public static void notEmpty(Collection<?> collection, String message) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Collection, String)}
	 */
	@Deprecated
	public static void notEmpty(Collection<?> collection) {
		notEmpty(collection,
				"[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
	}

	/**
	 * 断言Map包含条目; 也就是说, 它不能是{@code null}并且必须至少包含一个条目.
	 * <pre class="code">Assert.notEmpty(map, "Map must contain entries");</pre>
	 * 
	 * @param map 要检查的map
	 * @param message 断言失败时要使用的异常消息
	 * 
	 * @throws IllegalArgumentException 如果map是{@code null}或不包含任何条目
	 */
	public static void notEmpty(Map<?, ?> map, String message) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Map, String)}
	 */
	@Deprecated
	public static void notEmpty(Map<?, ?> map) {
		notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
	}

	/**
	 * 断言提供的对象是提供的类的实例.
	 * <pre class="code">Assert.instanceOf(Foo.class, foo, "Foo expected");</pre>
	 * 
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @param message 将提供一条消息, 以提供进一步的上下文信息.
	 * 如果它为空或以 ":" 或 ";" 或 "," 或 "."结尾, 将附加完整的异常消息.
	 * 如果它以空格结尾, 则将追加违规对象类型的名称.
	 * 在任何其他情况下, 将附加带有空格的":"和违规对象类型的名称.
	 * 
	 * @throws IllegalArgumentException 如果对象不是类型的实例
	 */
	public static void isInstanceOf(Class<?> type, Object obj, String message) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, message);
		}
	}

	/**
	 * 断言提供的对象是提供的类的实例.
	 * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
	 * 
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * 
	 * @throws IllegalArgumentException 如果对象不是类型的实例
	 */
	public static void isInstanceOf(Class<?> type, Object obj) {
		isInstanceOf(type, obj, "");
	}

	/**
	 * 断言{@code superType.isAssignableFrom(subType)} 是 {@code true}.
	 * <pre class="code">Assert.isAssignable(Number.class, myClass, "Number expected");</pre>
	 * 
	 * @param superType 要检查的超类型
	 * @param subType 要检查的子类型
	 * @param message 将提供一条消息, 以提供进一步的上下文信息.
	 * 如果它为空或以 ":" 或 ";" 或 "," 或 "."结尾, 将附加完整的异常消息.
	 * 如果它以空格结尾, 则将追加违规子类型的名称.
	 * 在任何其他情况下, 将附加带有空格的":"和违规子类型的名称.
	 * 
	 * @throws IllegalArgumentException 如果类不是派生的
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType, String message) {
		notNull(superType, "Super type to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, message);
		}
	}

	/**
	 * 断言{@code superType.isAssignableFrom(subType)}是{@code true}.
	 * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
	 * 
	 * @param superType 要检查的超类型
	 * @param subType 要检查的子类型
	 * 
	 * @throws IllegalArgumentException 如果类是不可派生的
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType) {
		isAssignable(superType, subType, "");
	}


	private static void instanceCheckFailed(Class<?> type, Object obj, String msg) {
		String className = (obj != null ? obj.getClass().getName() : "null");
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, className);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + ("Object of class [" + className + "] must be an instance of " + type);
		}
		throw new IllegalArgumentException(result);
	}

	private static void assignableCheckFailed(Class<?> superType, Class<?> subType, String msg) {
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, subType);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + (subType + " is not assignable to " + superType);
		}
		throw new IllegalArgumentException(result);
	}

	private static boolean endsWithSeparator(String msg) {
		return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
	}

	private static String messageWithTypeName(String msg, Object typeName) {
		return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
	}
}
