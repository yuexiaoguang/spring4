package org.springframework.expression.spel.ast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionInvocationTargetException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;
import org.springframework.expression.spel.support.ReflectiveMethodResolver;
import org.springframework.util.ObjectUtils;

/**
 * 表达式语言AST节点, 表示方法引用.
 */
public class MethodReference extends SpelNodeImpl {

	private final String name;

	private final boolean nullSafe;

	private String originalPrimitiveExitTypeDescriptor;

	private volatile CachedMethodExecutor cachedExecutor;


	public MethodReference(boolean nullSafe, String methodName, int pos, SpelNodeImpl... arguments) {
		super(pos, arguments);
		this.name = methodName;
		this.nullSafe = nullSafe;
	}


	public final String getName() {
		return this.name;
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		Object[] arguments = getArguments(state);
		if (state.getActiveContextObject().getValue() == null) {
			throwIfNotNullSafe(getArgumentTypes(arguments));
			return ValueRef.NullValueRef.INSTANCE;
		}
		return new MethodValueRef(state, arguments);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		EvaluationContext evaluationContext = state.getEvaluationContext();
		Object value = state.getActiveContextObject().getValue();
		TypeDescriptor targetType = state.getActiveContextObject().getTypeDescriptor();
		Object[] arguments = getArguments(state);
		TypedValue result = getValueInternal(evaluationContext, value, targetType, arguments);
		updateExitTypeDescriptor();
		return result;
	}

	private TypedValue getValueInternal(EvaluationContext evaluationContext,
			Object value, TypeDescriptor targetType, Object[] arguments) {

		List<TypeDescriptor> argumentTypes = getArgumentTypes(arguments);
		if (value == null) {
			throwIfNotNullSafe(argumentTypes);
			return TypedValue.NULL;
		}

		MethodExecutor executorToUse = getCachedExecutor(evaluationContext, value, targetType, argumentTypes);
		if (executorToUse != null) {
			try {
				return executorToUse.execute(evaluationContext, value, arguments);
			}
			catch (AccessException ex) {
				// 这可能发生的两个原因:
				// 1. 调用的方法实际上抛出了一个真正的异常
				// 2. 调用的方法没有传递它预期的参数并且变得 '陈旧'

				// 在第一种情况下, 不应该重试, 在第二种情况下, 应该看看是否有更合适的方法.

				// 要确定这种情况, AccessException将包含一个原因.
				// 如果原因是InvocationTargetException, 则在方法内抛出用户异常. 否则无法调用该方法.
				throwSimpleExceptionIfPossible(value, ex);

				// 在这一点上, 我们知道这不是一个用户问题, 所以如果找到一个更好的候选者, 值得重试.
				this.cachedExecutor = null;
			}
		}

		// 要么没有存取器, 要么它不再存在
		executorToUse = findAccessorForMethod(argumentTypes, value, evaluationContext);
		this.cachedExecutor = new CachedMethodExecutor(
				executorToUse, (value instanceof Class ? (Class<?>) value : null), targetType, argumentTypes);
		try {
			return executorToUse.execute(evaluationContext, value, arguments);
		}
		catch (AccessException ex) {
			// 上面的catch块中与上面相同的解包异常处理
			throwSimpleExceptionIfPossible(value, ex);
			throw new SpelEvaluationException(getStartPosition(), ex,
					SpelMessage.EXCEPTION_DURING_METHOD_INVOCATION, this.name,
					value.getClass().getName(), ex.getMessage());
		}
	}

	private void throwIfNotNullSafe(List<TypeDescriptor> argumentTypes) {
		if (!this.nullSafe) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.METHOD_CALL_ON_NULL_OBJECT_NOT_ALLOWED,
					FormatHelper.formatMethodForMessage(this.name, argumentTypes));
		}
	}

	private Object[] getArguments(ExpressionState state) {
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			// 使根对象再次成为活动上下文以评估参数表达式
			try {
				state.pushActiveContextObject(state.getScopeRootContextObject());
				arguments[i] = this.children[i].getValueInternal(state).getValue();
			}
			finally {
				state.popActiveContextObject();
			}
		}
		return arguments;
	}

	private List<TypeDescriptor> getArgumentTypes(Object... arguments) {
		List<TypeDescriptor> descriptors = new ArrayList<TypeDescriptor>(arguments.length);
		for (Object argument : arguments) {
			descriptors.add(TypeDescriptor.forObject(argument));
		}
		return Collections.unmodifiableList(descriptors);
	}

	private MethodExecutor getCachedExecutor(EvaluationContext evaluationContext, Object value,
			TypeDescriptor target, List<TypeDescriptor> argumentTypes) {

		List<MethodResolver> methodResolvers = evaluationContext.getMethodResolvers();
		if (methodResolvers == null || methodResolvers.size() != 1 ||
				!(methodResolvers.get(0) instanceof ReflectiveMethodResolver)) {
			// 不是默认的ReflectiveMethodResolver - 不知道缓存是否有效
			return null;
		}

		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck != null && executorToCheck.isSuitable(value, target, argumentTypes)) {
			return executorToCheck.get();
		}
		this.cachedExecutor = null;
		return null;
	}

	private MethodExecutor findAccessorForMethod(List<TypeDescriptor> argumentTypes, Object targetObject,
			EvaluationContext evaluationContext) throws SpelEvaluationException {

		AccessException accessException = null;
		List<MethodResolver> methodResolvers = evaluationContext.getMethodResolvers();
		for (MethodResolver methodResolver : methodResolvers) {
			try {
				MethodExecutor methodExecutor = methodResolver.resolve(
						evaluationContext, targetObject, this.name, argumentTypes);
				if (methodExecutor != null) {
					return methodExecutor;
				}
			}
			catch (AccessException ex) {
				accessException = ex;
				break;
			}
		}

		String method = FormatHelper.formatMethodForMessage(this.name, argumentTypes);
		String className = FormatHelper.formatClassNameForMessage(
				targetObject instanceof Class ? ((Class<?>) targetObject) : targetObject.getClass());
		if (accessException != null) {
			throw new SpelEvaluationException(
					getStartPosition(), accessException, SpelMessage.PROBLEM_LOCATING_METHOD, method, className);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.METHOD_NOT_FOUND, method, className);
		}
	}

	/**
	 * 解码 AccessException, 抛出一个轻量级的评估异常, 或者, 如果原因是RuntimeException, 则直接抛出RuntimeException.
	 */
	private void throwSimpleExceptionIfPossible(Object value, AccessException ex) {
		if (ex.getCause() instanceof InvocationTargetException) {
			Throwable rootCause = ex.getCause().getCause();
			if (rootCause instanceof RuntimeException) {
				throw (RuntimeException) rootCause;
			}
			throw new ExpressionInvocationTargetException(getStartPosition(),
					"A problem occurred when trying to execute method '" + this.name +
					"' on object of type [" + value.getClass().getName() + "]", rootCause);
		}
	}

	private void updateExitTypeDescriptor() {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck != null && executorToCheck.get() instanceof ReflectiveMethodExecutor) {
			Method method = ((ReflectiveMethodExecutor) executorToCheck.get()).getMethod();
			String descriptor = CodeFlow.toDescriptor(method.getReturnType());
			if (this.nullSafe && CodeFlow.isPrimitive(descriptor)) {
				originalPrimitiveExitTypeDescriptor = descriptor;
				this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
			}
			else {
				this.exitTypeDescriptor = descriptor;
			}
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder(this.name);
		sb.append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * 如果方法引用已被解析为可反射访问的方法并且子节点 (方法的参数) 也可编译, 则方法引用可编译.
	 */
	@Override
	public boolean isCompilable() {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck == null || executorToCheck.hasProxyTarget() ||
				!(executorToCheck.get() instanceof ReflectiveMethodExecutor)) {
			return false;
		}

		for (SpelNodeImpl child : this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}

		ReflectiveMethodExecutor executor = (ReflectiveMethodExecutor) executorToCheck.get();
		if (executor.didArgumentConversionOccur()) {
			return false;
		}
		Class<?> clazz = executor.getMethod().getDeclaringClass();
		if (!Modifier.isPublic(clazz.getModifiers()) && executor.getPublicDeclaringClass() == null) {
			return false;
		}

		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		CachedMethodExecutor executorToCheck = this.cachedExecutor;
		if (executorToCheck == null || !(executorToCheck.get() instanceof ReflectiveMethodExecutor)) {
			throw new IllegalStateException("No applicable cached executor found: " + executorToCheck);
		}

		ReflectiveMethodExecutor methodExecutor = (ReflectiveMethodExecutor) executorToCheck.get();
		Method method = methodExecutor.getMethod();
		boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
		String descriptor = cf.lastDescriptor();

		Label skipIfNull = null;
		if (descriptor == null && !isStaticMethod) {
			// 堆栈中没有任何东西, 但需要一些东西
			cf.loadTarget(mv);
		}
		if ((descriptor != null || !isStaticMethod) && this.nullSafe) {
			mv.visitInsn(DUP);
			skipIfNull = new Label();
			Label continueLabel = new Label();
			mv.visitJumpInsn(IFNONNULL, continueLabel);
			CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
			mv.visitJumpInsn(GOTO, skipIfNull);
			mv.visitLabel(continueLabel);
		}
		if (descriptor != null && isStaticMethod) {
			// 当什么都不需要时, 堆栈上的东西
			mv.visitInsn(POP);
		}
		
		if (CodeFlow.isPrimitive(descriptor)) {
			CodeFlow.insertBoxIfNecessary(mv, descriptor.charAt(0));
		}

		String classDesc = (Modifier.isPublic(method.getDeclaringClass().getModifiers()) ?
				method.getDeclaringClass().getName().replace('.', '/') :
				methodExecutor.getPublicDeclaringClass().getName().replace('.', '/'));
		if (!isStaticMethod && (descriptor == null || !descriptor.substring(1).equals(classDesc))) {
			CodeFlow.insertCheckCast(mv, "L" + classDesc);
		}

		generateCodeForArguments(mv, cf, method, this.children);
		mv.visitMethodInsn((isStaticMethod ? INVOKESTATIC : INVOKEVIRTUAL), classDesc, method.getName(),
				CodeFlow.createSignatureDescriptor(method), method.getDeclaringClass().isInterface());
		cf.pushDescriptor(this.exitTypeDescriptor);

		if (this.originalPrimitiveExitTypeDescriptor != null) {
			// 访问器的输出将是一个原始类型, 但是从上面的块可能是null,
			// 所以要在skipIfNull目标上有一个'common stack'元素, 我们需要将原始类型打包
			CodeFlow.insertBoxIfNecessary(mv, this.originalPrimitiveExitTypeDescriptor);
		}
		if (skipIfNull != null) {
			mv.visitLabel(skipIfNull);
		}
	}


	private class MethodValueRef implements ValueRef {

		private final EvaluationContext evaluationContext;

		private final Object value;

		private final TypeDescriptor targetType;

		private final Object[] arguments;

		public MethodValueRef(ExpressionState state, Object[] arguments) {
			this.evaluationContext = state.getEvaluationContext();
			this.value = state.getActiveContextObject().getValue();
			this.targetType = state.getActiveContextObject().getTypeDescriptor();
			this.arguments = arguments;
		}

		@Override
		public TypedValue getValue() {
			TypedValue result = MethodReference.this.getValueInternal(
					this.evaluationContext, this.value, this.targetType, this.arguments);
			updateExitTypeDescriptor();
			return result;
		}

		@Override
		public void setValue(Object newValue) {
			throw new IllegalAccessError();
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}


	private static class CachedMethodExecutor {

		private final MethodExecutor methodExecutor;

		private final Class<?> staticClass;

		private final TypeDescriptor target;

		private final List<TypeDescriptor> argumentTypes;

		public CachedMethodExecutor(MethodExecutor methodExecutor, Class<?> staticClass,
				TypeDescriptor target, List<TypeDescriptor> argumentTypes) {

			this.methodExecutor = methodExecutor;
			this.staticClass = staticClass;
			this.target = target;
			this.argumentTypes = argumentTypes;
		}

		public boolean isSuitable(Object value, TypeDescriptor target, List<TypeDescriptor> argumentTypes) {
			return ((this.staticClass == null || this.staticClass == value) &&
					ObjectUtils.nullSafeEquals(this.target, target) && this.argumentTypes.equals(argumentTypes));
		}

		public boolean hasProxyTarget() {
			return (this.target != null && Proxy.isProxyClass(this.target.getType()));
		}

		public MethodExecutor get() {
			return this.methodExecutor;
		}
	}
}
