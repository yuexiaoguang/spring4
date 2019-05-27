package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MethodInvoker;

/**
 * 反射解析器代码使用的实用方法, 用于发现应在表达式中使用的适当方法/构造函数和字段.
 */
public class ReflectionHelper {

	/**
	 * 比较参数数组并返回有关它们是否匹配的信息.
	 * 提供的类型转换器和conversionAllowed标志, 允许匹配考虑到转换器可以将类型转换为不同的类型.
	 * 
	 * @param expectedArgTypes 方法/构造函数期望的类型
	 * @param suppliedArgTypes 在调用点提供的类型
	 * @param typeConverter 注册的类型转换器
	 * 
	 * @return 一个MatchInfo对象, 指示它是什么类型的匹配, 或{@code null}如果它不匹配
	 */
	static ArgumentsMatchInfo compareArguments(
			List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(expectedArgTypes.size() == suppliedArgTypes.size(),
				"Expected argument types and supplied argument types should be arrays of same length");

		ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;
		for (int i = 0; i < expectedArgTypes.size() && match != null; i++) {
			TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
			TypeDescriptor expectedArg = expectedArgTypes.get(i);
			if (!expectedArg.equals(suppliedArg)) {
				// 用户可以提供null - 除非需要基本类型, 否则这将是正常的
				if (suppliedArg == null) {
					if (expectedArg.isPrimitive()) {
						match = null;
					}
				}
				else {
					if (suppliedArg.isAssignableTo(expectedArg)) {
						if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
							match = ArgumentsMatchKind.CLOSE;
						}
					}
					else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
						match = ArgumentsMatchKind.REQUIRES_CONVERSION;
					}
					else {
						match = null;
					}
				}
			}
		}
		return (match != null ? new ArgumentsMatchInfo(match) : null);
	}

	/**
	 * 基于{@link MethodInvoker#getTypeDifferenceWeight(Class[], Object[])}, 但在TypeDescriptors上运行.
	 */
	public static int getTypeDifferenceWeight(List<TypeDescriptor> paramTypes, List<TypeDescriptor> argTypes) {
		int result = 0;
		for (int i = 0; i < paramTypes.size(); i++) {
			TypeDescriptor paramType = paramTypes.get(i);
			TypeDescriptor argType = (i < argTypes.size() ? argTypes.get(i) : null);
			if (argType == null) {
				if (paramType.isPrimitive()) {
					return Integer.MAX_VALUE;
				}
			}
			else {
				Class<?> paramTypeClazz = paramType.getType();
				if (!ClassUtils.isAssignable(paramTypeClazz, argType.getType())) {
					return Integer.MAX_VALUE;
				}
				if (paramTypeClazz.isPrimitive()) {
					paramTypeClazz = Object.class;
				}
				Class<?> superClass = argType.getType().getSuperclass();
				while (superClass != null) {
					if (paramTypeClazz.equals(superClass)) {
						result = result + 2;
						superClass = null;
					}
					else if (ClassUtils.isAssignable(paramTypeClazz, superClass)) {
						result = result + 2;
						superClass = superClass.getSuperclass();
					}
					else {
						superClass = null;
					}
				}
				if (paramTypeClazz.isInterface()) {
					result = result + 1;
				}
			}
		}
		return result;
	}

	/**
	 * 比较参数数组并返回有关它们是否匹配的信息.
	 * 提供的类型转换器和conversionAllowed标志, 允许匹配考虑到转换器可以将类型转换为不同的类型.
	 * compareArguments的这个变体也允许varargs匹配.
	 * 
	 * @param expectedArgTypes 方法/构造函数期望的类型
	 * @param suppliedArgTypes 在调用点提供的类型
	 * @param typeConverter 注册的类型转换器
	 * 
	 * @return 一个MatchInfo对象, 指示它是什么类型的匹配, 或{@code null}如果它不匹配
	 */
	static ArgumentsMatchInfo compareArgumentsVarargs(
			List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(!CollectionUtils.isEmpty(expectedArgTypes),
				"Expected arguments must at least include one array (the varargs parameter)");
		Assert.isTrue(expectedArgTypes.get(expectedArgTypes.size() - 1).isArray(),
				"Final expected argument should be array type (the varargs parameter)");

		ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;

		// 检查直到varargs参数:

		// 处理'预期数量'的参数 - 1 (除了varargs参数之外的所有内容)
		int argCountUpToVarargs = expectedArgTypes.size() - 1;
		for (int i = 0; i < argCountUpToVarargs && match != null; i++) {
			TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
			TypeDescriptor expectedArg = expectedArgTypes.get(i);
			if (suppliedArg == null) {
				if (expectedArg.isPrimitive()) {
					match = null;
				}
			}
			else {
				if (!expectedArg.equals(suppliedArg)) {
					if (suppliedArg.isAssignableTo(expectedArg)) {
						if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
							match = ArgumentsMatchKind.CLOSE;
						}
					}
					else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
						match = ArgumentsMatchKind.REQUIRES_CONVERSION;
					}
					else {
						match = null;
					}
				}
			}
		}

		// 如果已经确认它不能匹配, 那么返回
		if (match == null) {
			return null;
		}

		if (suppliedArgTypes.size() == expectedArgTypes.size() &&
				expectedArgTypes.get(expectedArgTypes.size() - 1).equals(
						suppliedArgTypes.get(suppliedArgTypes.size() - 1))) {
			// 特殊情况: 剩下一个参数, 它是一个数组, 它匹配varargs预期参数 - 这是一个匹配, 调用者已经构建了数组. 继续.
		}
		else {
			// 现在... 在我们检查的方法中有最后一个参数作为匹配, 还有0个或更多其他参数传递给它.
			TypeDescriptor varargsDesc = expectedArgTypes.get(expectedArgTypes.size() - 1);
			Class<?> varargsParamType = varargsDesc.getElementTypeDescriptor().getType();

			// 所有剩余参数必须是此类型或可转换为此类型
			for (int i = expectedArgTypes.size() - 1; i < suppliedArgTypes.size(); i++) {
				TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
				if (suppliedArg == null) {
					if (varargsParamType.isPrimitive()) {
						match = null;
					}
				}
				else {
					if (varargsParamType != suppliedArg.getType()) {
						if (ClassUtils.isAssignable(varargsParamType, suppliedArg.getType())) {
							if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
								match = ArgumentsMatchKind.CLOSE;
							}
						}
						else if (typeConverter.canConvert(suppliedArg, TypeDescriptor.valueOf(varargsParamType))) {
							match = ArgumentsMatchKind.REQUIRES_CONVERSION;
						}
						else {
							match = null;
						}
					}
				}
			}
		}

		return (match != null ? new ArgumentsMatchInfo(match) : null);
	}


	// TODO 可以做更多关于参数处理和varargs的重构
	/**
	 * 将提供的参数集转换为请求的类型.
	 * 如果parameterTypes与varargs方法相关, 那么parameterTypes数组中的最后一个条目将是一个数组本身, 其组件类型应该用作无关参数的转换目标.
	 * (例如, 如果parameterTypes是{Integer, String[]}, 并且输入参数是{Integer, boolean, float}, 那么boolean和float都必须转换为字符串).
	 * 此方法*不*将参数重新打包为适合varargs调用的形式 - 后续调用setupArgumentsForVarargsInvocation处理.
	 * 
	 * @param converter 用于类型转换的转换器
	 * @param arguments 要转换为请求的参数类型的参数
	 * @param method 目标Method
	 * 
	 * @return true 如果参数发生了某种转换
	 * @throws SpelEvaluationException 如果转换时有问题
	 */
	public static boolean convertAllArguments(TypeConverter converter, Object[] arguments, Method method)
			throws SpelEvaluationException {

		Integer varargsPosition = (method.isVarArgs() ? method.getParameterTypes().length - 1 : null);
		return convertArguments(converter, arguments, method, varargsPosition);
	}

	/**
	 * 获取参数值的输入集, 并将它们转换为指定为必需参数类型的类型.
	 * 参数在输入数组中'就地'转换.
	 * 
	 * @param converter 用于尝试转换的类型转换器
	 * @param arguments 需要转换的实际参数
	 * @param methodOrCtor 目标Method 或 Constructor
	 * @param varargsPosition varargs参数的已知位置 ({@code null} 如果不是 varargs)
	 * 
	 * @return {@code true} 如果某个参数发生了某种转换
	 * @throws EvaluationException 如果在转换过程中出现问题
	 */
	static boolean convertArguments(TypeConverter converter, Object[] arguments, Object methodOrCtor,
			Integer varargsPosition) throws EvaluationException {

		boolean conversionOccurred = false;
		if (varargsPosition == null) {
			for (int i = 0; i < arguments.length; i++) {
				TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forMethodOrConstructor(methodOrCtor, i));
				Object argument = arguments[i];
				arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
				conversionOccurred |= (argument != arguments[i]);
			}
		}
		else {
			// 将所有内容转换为varargs位置
			for (int i = 0; i < varargsPosition; i++) {
				TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forMethodOrConstructor(methodOrCtor, i));
				Object argument = arguments[i];
				arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
				conversionOccurred |= (argument != arguments[i]);
			}
			MethodParameter methodParam = MethodParameter.forMethodOrConstructor(methodOrCtor, varargsPosition);
			if (varargsPosition == arguments.length - 1) {
				// 如果目标是varargs并且只有一个参数, 那么在此转换它
				TypeDescriptor targetType = new TypeDescriptor(methodParam);
				Object argument = arguments[varargsPosition];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				arguments[varargsPosition] = converter.convertValue(argument, sourceType, targetType);
				// 前一行的三个结果:
				// 1) 输入参数已经兼容 (ie. 有效类型的数组) 并且没有做任何事情
				// 2) 输入参数是正确的类型, 但不在数组中, 因此它被制作成一个数组
				// 3) 输入参数是错误的类型, 并被转换并放入数组
				if (argument != arguments[varargsPosition] &&
						!isFirstEntryInArray(argument, arguments[varargsPosition])) {
					conversionOccurred = true; // case 3
				}
			}
			else {
				// 将剩余参数转换为varargs元素类型
				TypeDescriptor targetType = new TypeDescriptor(methodParam).getElementTypeDescriptor();
				for (int i = varargsPosition; i < arguments.length; i++) {
					Object argument = arguments[i];
					arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
					conversionOccurred |= (argument != arguments[i]);
				}
			}
		}
		return conversionOccurred;
	}

	/**
	 * 检查提供的值是否是possibleArray值表示的数组中的第一个条目.
	 * 
	 * @param value 要在数组中检查的值
	 * @param possibleArray 一个数组对象, 可以将提供的值作为第一个元素
	 * 
	 * @return true 如果提供的值是数组中的第一个条目
	 */
	private static boolean isFirstEntryInArray(Object value, Object possibleArray) {
		if (possibleArray == null) {
			return false;
		}
		Class<?> type = possibleArray.getClass();
		if (!type.isArray() || Array.getLength(possibleArray) == 0 ||
				!ClassUtils.isAssignableValue(type.getComponentType(), value)) {
			return false;
		}
		Object arrayValue = Array.get(possibleArray, 0);
		return (type.getComponentType().isPrimitive() ? arrayValue.equals(value) : arrayValue == value);
	}

	/**
	 * 打包参数, 使它们与parameterTypes中的预期值正确匹配.
	 * 例如, 如果parameterTypes是{@code (int, String[])}, 因为第二个参数被声明为{@code String...},
	 * 那么如果参数是{@code [1,"a","b"]}, 然后它必须重新打包为{@code [1,new String[]{"a","b"}]}以匹配预期的类型.
	 * 
	 * @param requiredParameterTypes 调用的参数类型
	 * @param args 要为调用准备好的参数
	 * 
	 * @return 重新打包的参数数组, 其中任何varargs设置已完成
	 */
	public static Object[] setupArgumentsForVarargsInvocation(Class<?>[] requiredParameterTypes, Object... args) {
		// 检查数组是否已为最终参数构建
		int parameterCount = requiredParameterTypes.length;
		int argumentCount = args.length;

		// 检查是否需要重新包装...
		if (parameterCount != args.length ||
				requiredParameterTypes[parameterCount - 1] !=
						(args[argumentCount - 1] != null ? args[argumentCount - 1].getClass() : null)) {

			int arraySize = 0;  // 零大小数组, 如果没有任何传递作为varargs参数
			if (argumentCount >= parameterCount) {
				arraySize = argumentCount - (parameterCount - 1);
			}

			// 为varargs参数创建一个数组
			Object[] newArgs = new Object[parameterCount];
			System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);

			// 现在理清最后的参数, 即varargs. 在进入此方法之前, 参数应该已转换为所需类型的封装形式.
			Class<?> componentType = requiredParameterTypes[parameterCount - 1].getComponentType();
			Object repackagedArgs = Array.newInstance(componentType, arraySize);
			for (int i = 0; i < arraySize; i++) {
				Array.set(repackagedArgs, i, args[parameterCount - 1 + i]);
			}
			newArgs[newArgs.length - 1] = repackagedArgs;
			return newArgs;
		}
		return args;
	}


	enum ArgumentsMatchKind {

		/** 完全匹配是参数类型与方法/构造函数所期望的完全匹配 */
		EXACT,

		/** 紧密匹配是参数类型完全匹配或与赋值兼容 */
		CLOSE,

		/** 转换匹配是必须使用类型转换器来转换某些参数类型 */
		REQUIRES_CONVERSION
	}


	/**
	 * ArgumentsMatchInfo的一个实例描述了两组参数之间实现了哪种匹配 - 方法/构造函数所期望的集合, 以及在调用时提供的集合.
	 * 如果类型表明某些参数需要转换, 那么需要转换的参数列在argsRequiringConversion数组中.
	 */
	static class ArgumentsMatchInfo {

		private final ArgumentsMatchKind kind;

		ArgumentsMatchInfo(ArgumentsMatchKind kind) {
			this.kind = kind;
		}

		public boolean isExactMatch() {
			return (this.kind == ArgumentsMatchKind.EXACT);
		}

		public boolean isCloseMatch() {
			return (this.kind == ArgumentsMatchKind.CLOSE);
		}

		public boolean isMatchRequiringConversion() {
			return (this.kind == ArgumentsMatchKind.REQUIRES_CONVERSION);
		}

		@Override
		public String toString() {
			return "ArgumentMatchInfo: " + this.kind;
		}
	}
}
