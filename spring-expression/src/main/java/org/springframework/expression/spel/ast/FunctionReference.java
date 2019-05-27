package org.springframework.expression.spel.ast;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 函数引用的格式为 "#someFunction(a,b,c)".
 * 可以在评估表达式之前在上下文中定义函数.
 * 函数也可以是静态Java方法, 在调用表达式之前在上下文中注册.
 *
 * <p>功能非常简单. 参数不是定义的一部分 (现在), 因此名称必须是唯一的.
 */
public class FunctionReference extends SpelNodeImpl {

	private final String name;

	// 捕获最近使用的函数调用方法, *if* 该方法可以安全地用于编译 (i.e. 不进行参数转换)
	private volatile Method method;


	public FunctionReference(String functionName, int pos, SpelNodeImpl... arguments) {
		super(pos, arguments);
		this.name = functionName;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue value = state.lookupVariable(this.name);
		if (value == TypedValue.NULL) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
		}
		if (!(value.getValue() instanceof Method)) {
			// 可能是作为函数注册的静态Java方法
			throw new SpelEvaluationException(
					SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
		}

		try {
			return executeFunctionJLRMethod(state, (Method) value.getValue());
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getStartPosition());
			throw ex;
		}
	}

	/**
	 * 执行一个表示为{@code java.lang.reflect.Method}的函数.
	 * 
	 * @param state 表达式评估状态
	 * @param method 要调用的方法
	 * 
	 * @return 调用的Java方法的返回值
	 * @throws EvaluationException 如果调用该方法有任何问题
	 */
	private TypedValue executeFunctionJLRMethod(ExpressionState state, Method method) throws EvaluationException {
		Object[] functionArgs = getArguments(state);

		if (!method.isVarArgs()) {
			int declaredParamCount = method.getParameterTypes().length;
			if (declaredParamCount != functionArgs.length) {
				throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
						functionArgs.length, declaredParamCount);
			}
		}
		if (!Modifier.isStatic(method.getModifiers())) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.FUNCTION_MUST_BE_STATIC, ClassUtils.getQualifiedMethodName(method), this.name);
		}

		// 必要时转换参数, 并根据需要将其重新映射为varargs
		TypeConverter converter = state.getEvaluationContext().getTypeConverter();
		boolean argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
		if (method.isVarArgs()) {
			functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
					method.getParameterTypes(), functionArgs);
		}
		boolean compilable = false;

		try {
			ReflectionUtils.makeAccessible(method);
			Object result = method.invoke(method.getClass(), functionArgs);
			compilable = !argumentConversionOccurred;
			return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
		}
		catch (Exception ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
					this.name, ex.getMessage());
		}
		finally {
			if (compilable) {
				this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
				this.method = method;
			}
			else {
				this.exitTypeDescriptor = null;
				this.method = null;
			}
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("#").append(this.name);
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
	 * 计算函数的参数, 它们是此表达式节点的子元素.
	 * 
	 * @return 函数调用的参数值数组
	 */
	private Object[] getArguments(ExpressionState state) throws EvaluationException {
		// 计算函数的参数
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = this.children[i].getValueInternal(state).getValue();
		}
		return arguments;
	}
	
	@Override
	public boolean isCompilable() {
		Method method = this.method;
		if (method == null) {
			return false;
		}
		int methodModifiers = method.getModifiers();
		if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
			return false;
		}
		for (SpelNodeImpl child : this.children) {
			if (!child.isCompilable()) {
				return false;
			}
		}
		return true;
	}
	
	@Override 
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		Method method = this.method;
		Assert.state(method != null, "No method handle");
		String classDesc = method.getDeclaringClass().getName().replace('.', '/');
		generateCodeForArguments(mv, cf, method, this.children);
		mv.visitMethodInsn(INVOKESTATIC, classDesc, method.getName(),
				CodeFlow.createSignatureDescriptor(method), false);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
