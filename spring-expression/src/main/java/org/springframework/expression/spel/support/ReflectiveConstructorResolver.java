package org.springframework.expression.spel.support;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;

/**
 * 一个构造函数解析器, 它使用反射来定位应该调用的构造函数.
 */
public class ReflectiveConstructorResolver implements ConstructorResolver {

	/**
	 * 在类型上查找构造函数. 可能会发生三种匹配:
	 * <ol>
	 * <li>精确匹配, 其中参数的类型与构造函数的类型匹配
	 * <li>不完全匹配, 正在寻找的类型是构造函数中定义的类型的子类型
	 * <li>根据注册的类型转换器, 能够将参数转换为构造函数所期望的参数的匹配.
	 * </ol>
	 */
	@Override
	public ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
			throws AccessException {

		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = context.getTypeLocator().findType(typeName);
			Constructor<?>[] ctors = type.getConstructors();

			Arrays.sort(ctors, new Comparator<Constructor<?>>() {
				@Override
				public int compare(Constructor<?> c1, Constructor<?> c2) {
					int c1pl = c1.getParameterTypes().length;
					int c2pl = c2.getParameterTypes().length;
					return (c1pl < c2pl ? -1 : (c1pl > c2pl ? 1 : 0));
				}
			});

			Constructor<?> closeMatch = null;
			Constructor<?> matchRequiringConversion = null;

			for (Constructor<?> ctor : ctors) {
				Class<?>[] paramTypes = ctor.getParameterTypes();
				List<TypeDescriptor> paramDescriptors = new ArrayList<TypeDescriptor>(paramTypes.length);
				for (int i = 0; i < paramTypes.length; i++) {
					paramDescriptors.add(new TypeDescriptor(new MethodParameter(ctor, i)));
				}
				ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
				if (ctor.isVarArgs() && argumentTypes.size() >= paramTypes.length - 1) {
					// 复杂
					// 基本上.. 必须让所有参数匹配, 直到varargs为止, 然后提供的其余内容应该是相同的类型,
					// 而方法的最后一个参数必须是一个数组 - 或者提供的最终参数确实匹配 (它已经是一个数组).
					matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
				}
				else if (paramTypes.length == argumentTypes.size()) {
					// 值得仔细看看
					matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
				}
				if (matchInfo != null) {
					if (matchInfo.isExactMatch()) {
						return new ReflectiveConstructorExecutor(ctor);
					}
					else if (matchInfo.isCloseMatch()) {
						closeMatch = ctor;
					}
					else if (matchInfo.isMatchRequiringConversion()) {
						matchRequiringConversion = ctor;
					}
				}
			}

			if (closeMatch != null) {
				return new ReflectiveConstructorExecutor(closeMatch);
			}
			else if (matchRequiringConversion != null) {
				return new ReflectiveConstructorExecutor(matchRequiringConversion);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve constructor", ex);
		}
	}

}
