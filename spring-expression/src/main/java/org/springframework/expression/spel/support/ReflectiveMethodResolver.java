package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodFilter;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * 除非已指定显式方法解析器, 否则默认情况下在{@link StandardEvaluationContext}中使用基于反射的{@link MethodResolver}.
 */
public class ReflectiveMethodResolver implements MethodResolver {

	// 使用距离将确保发现更准确的匹配, 更严格遵循Java规则.
	private final boolean useDistance;

	private Map<Class<?>, MethodFilter> filters;


	public ReflectiveMethodResolver() {
		this.useDistance = true;
	}

	/**
	 * 这个构造函数允许配置ReflectiveMethodResolver, 以便它使用距离计算来检查两个匹配中哪个更好 (当有多个匹配时).
	 * 使用距离计算, 旨在确保匹配更能代表Java编译器在考虑装箱/取消装箱时, 将执行的操作, 以及是否声明候选方法以处理传入的(参数)类型的超类型.
	 * 
	 * @param useDistance {@code true} 如果计算匹配时应使用距离计算; 否则{@code false}
	 */
	public ReflectiveMethodResolver(boolean useDistance) {
		this.useDistance = useDistance;
	}


	/**
	 * 为给定类型的方法注册过滤器.
	 * 
	 * @param type 要过滤的类型
	 * @param filter 相应的方法过滤器, 或{@code null}清除给定类型的过滤器
	 */
	public void registerMethodFilter(Class<?> type, MethodFilter filter) {
		if (this.filters == null) {
			this.filters = new HashMap<Class<?>, MethodFilter>();
		}
		if (filter != null) {
			this.filters.put(type, filter);
		}
		else {
			this.filters.remove(type);
		}
	}

	/**
	 * 在类型上找到方法. 可能会发生三种匹配:
	 * <ol>
	 * <li>完全匹配, 其中参数的类型与构造函数的类型匹配
	 * <li>不完全匹配, 要查找的类型是构造函数中定义的类型的子类型
	 * <li>根据已注册的类型转换器, 能够将参数转换为构造函数所期望的参数
	 * </ol>
	 */
	@Override
	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException {

		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
			List<Method> methods = new ArrayList<Method>(getMethods(type, targetObject));

			// 如果为此类型注册了过滤器, 调用它
			MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
			if (filter != null) {
				List<Method> filtered = filter.filter(methods);
				methods = (filtered instanceof ArrayList ? filtered : new ArrayList<Method>(filtered));
			}

			// 将方法排序
			if (methods.size() > 1) {
				Collections.sort(methods, new Comparator<Method>() {
					@Override
					public int compare(Method m1, Method m2) {
						int m1pl = m1.getParameterTypes().length;
						int m2pl = m2.getParameterTypes().length;
						// varargs methods go last
						if (m1pl == m2pl) {
						    if (!m1.isVarArgs() && m2.isVarArgs()) {
						    	return -1;
						    }
						    else if (m1.isVarArgs() && !m2.isVarArgs()) {
						    	return 1;
						    }
						    else {
						    	return 0;
						    }
						}
						return (m1pl < m2pl ? -1 : (m1pl > m2pl ? 1 : 0));
					}
				});
			}

			// 解析桥接方法
			for (int i = 0; i < methods.size(); i++) {
				methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
			}

			// 删除重复的方法 (可能由于解析的桥接方法)
			Set<Method> methodsToIterate = new LinkedHashSet<Method>(methods);

			Method closeMatch = null;
			int closeMatchDistance = Integer.MAX_VALUE;
			Method matchRequiringConversion = null;
			boolean multipleOptions = false;

			for (Method method : methodsToIterate) {
				if (method.getName().equals(name)) {
					Class<?>[] paramTypes = method.getParameterTypes();
					List<TypeDescriptor> paramDescriptors = new ArrayList<TypeDescriptor>(paramTypes.length);
					for (int i = 0; i < paramTypes.length; i++) {
						paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
					}
					ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
					if (method.isVarArgs() && argumentTypes.size() >= (paramTypes.length - 1)) {
						// *sigh* complicated
						matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
					}
					else if (paramTypes.length == argumentTypes.size()) {
						// 名称和参数数量匹配, 检查参数
						matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
					}
					if (matchInfo != null) {
						if (matchInfo.isExactMatch()) {
							return new ReflectiveMethodExecutor(method);
						}
						else if (matchInfo.isCloseMatch()) {
							if (this.useDistance) {
								int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
								if (closeMatch == null || matchDistance < closeMatchDistance) {
									// This is a better match...
									closeMatch = method;
									closeMatchDistance = matchDistance;
								}
							}
							else {
								// 如果没有一个, 请将此作为一个紧密匹配
								if (closeMatch == null) {
									closeMatch = method;
								}
							}
						}
						else if (matchInfo.isMatchRequiringConversion()) {
							if (matchRequiringConversion != null) {
								multipleOptions = true;
							}
							matchRequiringConversion = method;
						}
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveMethodExecutor(closeMatch);
			}
			else if (matchRequiringConversion != null) {
				if (multipleOptions) {
					throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
				}
				return new ReflectiveMethodExecutor(matchRequiringConversion);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve method", ex);
		}
	}

	private Set<Method> getMethods(Class<?> type, Object targetObject) {
		if (targetObject instanceof Class) {
			Set<Method> result = new LinkedHashSet<Method>();
			// 添加这些以便在类型上可以调用静态方法: e.g. Float.valueOf(..)
			Method[] methods = getMethods(type);
			for (Method method : methods) {
				if (Modifier.isStatic(method.getModifiers())) {
					result.add(method);
				}
			}
			// 还从java.lang.Class本身公开方法
			result.addAll(Arrays.asList(getMethods(Class.class)));
			return result;
		}
		else if (Proxy.isProxyClass(type)) {
			Set<Method> result = new LinkedHashSet<Method>();
			// 公开接口方法 (不是代理声明的覆盖) 以进行正确的vararg内省
			for (Class<?> ifc : type.getInterfaces()) {
				Method[] methods = getMethods(ifc);
				for (Method method : methods) {
					if (isCandidateForInvocation(method, type)) {
						result.add(method);
					}
				}
			}
			return result;
		}
		else {
			Set<Method> result = new LinkedHashSet<Method>();
			Method[] methods = getMethods(type);
			for (Method method : methods) {
				if (isCandidateForInvocation(method, type)) {
					result.add(method);
				}
			}
			return result;
		}
	}

	/**
	 * 返回此类型的方法集.
	 * 对于给定的{@code type}, 默认实现返回的结果, 但子类可以覆盖以改变结果, e.g. 指定在别处声明的静态方法.
	 * 
	 * @param type 要返回方法的类
	 */
	protected Method[] getMethods(Class<?> type) {
		return type.getMethods();
	}

	/**
	 * 确定给定的{@code Method}是否是给定目标类的实例上方法解析的候选者.
	 * <p>默认实现将任何方法视为候选方法, 即使对于{@link Object}基类上的非用户声明方法的静态方法也是如此.
	 * 
	 * @param method 要评估的Method
	 * @param targetClass 正在被内省的具体目标类
	 */
	protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
		return true;
	}
}
