package org.springframework.expression.spel.ast;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.support.ReflectiveConstructorExecutor;

/**
 * 表示构造函数的调用.
 * 常规类型的构造函数或数组的构造. 构造数组时, 可以指定初始化器.
 *
 * <p>示例:<br>
 * new String('hello world')<br>
 * new int[]{1,2,3,4}<br>
 * new int[3] new int[3]{1,2,3}
 */
public class ConstructorReference extends SpelNodeImpl {

	private boolean isArrayConstructor = false;

	private SpelNodeImpl[] dimensions;

	// TODO is this caching safe - passing the expression around will mean this executor is also being passed around
	/**
	 * 可以在后续评估中重用的缓存执行器.
	 */
	private volatile ConstructorExecutor cachedExecutor;


	/**
	 * 第一个参数是类型, 其余是构造函数调用的参数
	 */
	public ConstructorReference(int pos, SpelNodeImpl... arguments) {
		super(pos, arguments);
		this.isArrayConstructor = false;
	}

	/**
	 * 第一个参数是类型, 其余是构造函数调用的参数
	 */
	public ConstructorReference(int pos, SpelNodeImpl[] dimensions, SpelNodeImpl... arguments) {
		super(pos, arguments);
		this.isArrayConstructor = true;
		this.dimensions = dimensions;
	}


	/**
	 * 实现 getValue() - 委托给构建数组或简单类型的代码.
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (this.isArrayConstructor) {
			return createArray(state);
		}
		else {
			return createNewInstance(state);
		}
	}

	/**
	 * 创建一个新的普通对象并将其返回.
	 * 
	 * @param state 正在评估此表达式的状态
	 * 
	 * @return 新对象
	 * @throws EvaluationException 如果创建对象有问题
	 */
	private TypedValue createNewInstance(ExpressionState state) throws EvaluationException {
		Object[] arguments = new Object[getChildCount() - 1];
		List<TypeDescriptor> argumentTypes = new ArrayList<TypeDescriptor>(getChildCount() - 1);
		for (int i = 0; i < arguments.length; i++) {
			TypedValue childValue = this.children[i + 1].getValueInternal(state);
			Object value = childValue.getValue();
			arguments[i] = value;
			argumentTypes.add(TypeDescriptor.forObject(value));
		}

		ConstructorExecutor executorToUse = this.cachedExecutor;
		if (executorToUse != null) {
			try {
				return executorToUse.execute(state.getEvaluationContext(), arguments);
			}
			catch (AccessException ex) {
				// 这可能发生的两个原因:
				// 1. 调用的方法实际上抛出了一个真正的异常
				// 2. 调用的方法没有传递它预期的参数并且变得 '陈旧'

				// 在第一种情况下, 我们不应该重试, 在第二种情况下, 我们应该看看是否有更合适的方法.

				// 为了确定它是什么情况, AccessException将包含一个原因.
				// 如果原因是InvocationTargetException, 则在构造函数内抛出用户异常.
				// 否则无法调用构造函数.
				if (ex.getCause() instanceof InvocationTargetException) {
					// 用户异常是根本原因 - 现在退出
					Throwable rootCause = ex.getCause().getCause();
					if (rootCause instanceof RuntimeException) {
						throw (RuntimeException) rootCause;
					}
					else {
						String typeName = (String) this.children[0].getValueInternal(state).getValue();
						throw new SpelEvaluationException(getStartPosition(), rootCause,
								SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
								FormatHelper.formatMethodForMessage("", argumentTypes));
					}
				}

				// 在这一点上, 我们知道这不是一个用户问题, 所以如果找到一个更好的候选者, 值得重试
				this.cachedExecutor = null;
			}
		}

		// 要么没有访问者, 要么它不再存在
		String typeName = (String) this.children[0].getValueInternal(state).getValue();
		executorToUse = findExecutorForConstructor(typeName, argumentTypes, state);
		try {
			this.cachedExecutor = executorToUse;
			if (this.cachedExecutor instanceof ReflectiveConstructorExecutor) {
				this.exitTypeDescriptor = CodeFlow.toDescriptor(
						((ReflectiveConstructorExecutor) this.cachedExecutor).getConstructor().getDeclaringClass());
				
			}
			return executorToUse.execute(state.getEvaluationContext(), arguments);
		}
		catch (AccessException ex) {
			throw new SpelEvaluationException(getStartPosition(), ex,
					SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
					FormatHelper.formatMethodForMessage("", argumentTypes));
		}
	}

	/**
	 * 浏览已注册的构造函数解析器列表, 看看是否有采用指定参数集的构造函数.
	 * 
	 * @param typeName 试图构建的类型
	 * @param argumentTypes 构造函数必须提供的参数类型
	 * @param state 表达式的当前状态
	 * 
	 * @return 可重用的ConstructorExecutor, 可以调用它来运行构造函数或null
	 * @throws SpelEvaluationException 如果查找构造函数时有问题
	 */
	private ConstructorExecutor findExecutorForConstructor(String typeName,
			List<TypeDescriptor> argumentTypes, ExpressionState state)
			throws SpelEvaluationException {

		EvaluationContext evalContext = state.getEvaluationContext();
		List<ConstructorResolver> ctorResolvers = evalContext.getConstructorResolvers();
		if (ctorResolvers != null) {
			for (ConstructorResolver ctorResolver : ctorResolvers) {
				try {
					ConstructorExecutor ce = ctorResolver.resolve(state.getEvaluationContext(), typeName, argumentTypes);
					if (ce != null) {
						return ce;
					}
				}
				catch (AccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
							FormatHelper.formatMethodForMessage("", argumentTypes));
				}
			}
		}
		throw new SpelEvaluationException(getStartPosition(), SpelMessage.CONSTRUCTOR_NOT_FOUND, typeName,
				FormatHelper.formatMethodForMessage("", argumentTypes));
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("new ");
		int index = 0;
		sb.append(getChild(index++).toStringAST());
		sb.append("(");
		for (int i = index; i < getChildCount(); i++) {
			if (i > index) {
				sb.append(",");
			}
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * 创建一个数组并返回它.
	 * 
	 * @param state 评估此表达式时的状态
	 * 
	 * @return 新数组
	 * @throws EvaluationException 如果创建数组时有问题
	 */
	private TypedValue createArray(ExpressionState state) throws EvaluationException {
		// 第一个节点为我们提供了数组类型, 它将是基本类型或引用类型
		Object intendedArrayType = getChild(0).getValue(state);
		if (!(intendedArrayType instanceof String)) {
			throw new SpelEvaluationException(getChild(0).getStartPosition(),
					SpelMessage.TYPE_NAME_EXPECTED_FOR_ARRAY_CONSTRUCTION,
					FormatHelper.formatClassNameForMessage(intendedArrayType.getClass()));
		}
		String type = (String) intendedArrayType;
		Class<?> componentType;
		TypeCode arrayTypeCode = TypeCode.forName(type);
		if (arrayTypeCode == TypeCode.OBJECT) {
			componentType = state.findType(type);
		}
		else {
			componentType = arrayTypeCode.getType();
		}
		Object newArray;
		if (!hasInitializer()) {
			// 确认指定的维度 (例如 [3][][5] 缺少第二维)
			for (SpelNodeImpl dimension : this.dimensions) {
				if (dimension == null) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.MISSING_ARRAY_DIMENSION);
				}
			}
			TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();

			// 1维的快捷方式
			if (this.dimensions.length == 1) {
				TypedValue o = this.dimensions[0].getTypedValue(state);
				int arraySize = ExpressionUtils.toInt(typeConverter, o);
				newArray = Array.newInstance(componentType, arraySize);
			}
			else {
				// 多维!
				int[] dims = new int[this.dimensions.length];
				for (int d = 0; d < this.dimensions.length; d++) {
					TypedValue o = this.dimensions[d].getTypedValue(state);
					dims[d] = ExpressionUtils.toInt(typeConverter, o);
				}
				newArray = Array.newInstance(componentType, dims);
			}
		}
		else {
			// 有一个初始化器
			if (this.dimensions.length > 1) {
				// 有一个初始化器, 但这是一个多维数组 (e.g. new int[][]{{1,2},{3,4}}) - 目前不支持
				throw new SpelEvaluationException(getStartPosition(),
						SpelMessage.MULTIDIM_ARRAY_INITIALIZER_NOT_SUPPORTED);
			}
			TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
			InlineList initializer = (InlineList) getChild(1);
			// 如果指定了维度, 请检查它是否与初始化器长度匹配
			if (this.dimensions[0] != null) {
				TypedValue dValue = this.dimensions[0].getTypedValue(state);
				int i = ExpressionUtils.toInt(typeConverter, dValue);
				if (i != initializer.getChildCount()) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.INITIALIZER_LENGTH_INCORRECT);
				}
			}
			// 构建数组并填充它
			int arraySize = initializer.getChildCount();
			newArray = Array.newInstance(componentType, arraySize);
			if (arrayTypeCode == TypeCode.OBJECT) {
				populateReferenceTypeArray(state, newArray, typeConverter, initializer, componentType);
			}
			else if (arrayTypeCode == TypeCode.BOOLEAN) {
				populateBooleanArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.BYTE) {
				populateByteArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.CHAR) {
				populateCharArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.DOUBLE) {
				populateDoubleArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.FLOAT) {
				populateFloatArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.INT) {
				populateIntArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.LONG) {
				populateLongArray(state, newArray, typeConverter, initializer);
			}
			else if (arrayTypeCode == TypeCode.SHORT) {
				populateShortArray(state, newArray, typeConverter, initializer);
			}
			else {
				throw new IllegalStateException(arrayTypeCode.name());
			}
		}
		return new TypedValue(newArray);
	}

	private void populateReferenceTypeArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer, Class<?> componentType) {

		TypeDescriptor toTypeDescriptor = TypeDescriptor.valueOf(componentType);
		Object[] newObjectArray = (Object[]) newArray;
		for (int i = 0; i < newObjectArray.length; i++) {
			SpelNode elementNode = initializer.getChild(i);
			Object arrayEntry = elementNode.getValue(state);
			newObjectArray[i] = typeConverter.convertValue(arrayEntry,
					TypeDescriptor.forObject(arrayEntry), toTypeDescriptor);
		}
	}

	private void populateByteArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		byte[] newByteArray = (byte[]) newArray;
		for (int i = 0; i < newByteArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newByteArray[i] = ExpressionUtils.toByte(typeConverter, typedValue);
		}
	}

	private void populateFloatArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		float[] newFloatArray = (float[]) newArray;
		for (int i = 0; i < newFloatArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newFloatArray[i] = ExpressionUtils.toFloat(typeConverter, typedValue);
		}
	}

	private void populateDoubleArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		double[] newDoubleArray = (double[]) newArray;
		for (int i = 0; i < newDoubleArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newDoubleArray[i] = ExpressionUtils.toDouble(typeConverter, typedValue);
		}
	}

	private void populateShortArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		short[] newShortArray = (short[]) newArray;
		for (int i = 0; i < newShortArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newShortArray[i] = ExpressionUtils.toShort(typeConverter, typedValue);
		}
	}

	private void populateLongArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		long[] newLongArray = (long[]) newArray;
		for (int i = 0; i < newLongArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newLongArray[i] = ExpressionUtils.toLong(typeConverter, typedValue);
		}
	}

	private void populateCharArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		char[] newCharArray = (char[]) newArray;
		for (int i = 0; i < newCharArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newCharArray[i] = ExpressionUtils.toChar(typeConverter, typedValue);
		}
	}

	private void populateBooleanArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		boolean[] newBooleanArray = (boolean[]) newArray;
		for (int i = 0; i < newBooleanArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newBooleanArray[i] = ExpressionUtils.toBoolean(typeConverter, typedValue);
		}
	}

	private void populateIntArray(ExpressionState state, Object newArray, TypeConverter typeConverter,
			InlineList initializer) {

		int[] newIntArray = (int[]) newArray;
		for (int i = 0; i < newIntArray.length; i++) {
			TypedValue typedValue = initializer.getChild(i).getTypedValue(state);
			newIntArray[i] = ExpressionUtils.toInt(typeConverter, typedValue);
		}
	}

	private boolean hasInitializer() {
		return (getChildCount() > 1);
	}
	
	@Override
	public boolean isCompilable() {
		if (!(this.cachedExecutor instanceof ReflectiveConstructorExecutor) || 
			this.exitTypeDescriptor == null) {
			return false;
		}

		if (getChildCount() > 1) {
			for (int c = 1, max = getChildCount();c < max; c++) {
				if (!this.children[c].isCompilable()) {
					return false;
				}
			}
		}

		ReflectiveConstructorExecutor executor = (ReflectiveConstructorExecutor) this.cachedExecutor;
		Constructor<?> constructor = executor.getConstructor();
		return (Modifier.isPublic(constructor.getModifiers()) &&
				Modifier.isPublic(constructor.getDeclaringClass().getModifiers()));
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		ReflectiveConstructorExecutor executor = ((ReflectiveConstructorExecutor) this.cachedExecutor);
		Constructor<?> constructor = executor.getConstructor();		
		String classDesc = constructor.getDeclaringClass().getName().replace('.', '/');
		mv.visitTypeInsn(NEW, classDesc);
		mv.visitInsn(DUP);
		// children[0] 是构造函数的类型, 不希望在参数处理中包含它
		SpelNodeImpl[] arguments = new SpelNodeImpl[children.length - 1];
		System.arraycopy(children, 1, arguments, 0, children.length - 1);
		generateCodeForArguments(mv, cf, constructor, arguments);	
		mv.visitMethodInsn(INVOKESPECIAL, classDesc, "<init>", CodeFlow.createSignatureDescriptor(constructor), false);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}
}
