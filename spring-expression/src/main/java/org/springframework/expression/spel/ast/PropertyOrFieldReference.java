package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;

/**
 * 表示简单的属性或字段引用.
 */
public class PropertyOrFieldReference extends SpelNodeImpl {

	private final boolean nullSafe;

	private String originalPrimitiveExitTypeDescriptor = null;

	private final String name;

	private volatile PropertyAccessor cachedReadAccessor;

	private volatile PropertyAccessor cachedWriteAccessor;


	public PropertyOrFieldReference(boolean nullSafe, String propertyOrFieldName, int pos) {
		super(pos);
		this.nullSafe = nullSafe;
		this.name = propertyOrFieldName;
	}


	public boolean isNullSafe() {
		return this.nullSafe;
	}

	public String getName() {
		return this.name;
	}


	@Override
	public ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		return new AccessorLValue(this, state.getActiveContextObject(), state.getEvaluationContext(),
				state.getConfiguration().isAutoGrowNullReferences());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue tv = getValueInternal(state.getActiveContextObject(), state.getEvaluationContext(),
				state.getConfiguration().isAutoGrowNullReferences());
		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (accessorToUse instanceof CompilablePropertyAccessor) {
			CompilablePropertyAccessor accessor = (CompilablePropertyAccessor) accessorToUse;
			setExitTypeDescriptor(CodeFlow.toDescriptor(accessor.getPropertyType()));
		}
		return tv;
	}

	private TypedValue getValueInternal(TypedValue contextObject, EvaluationContext evalContext,
			boolean isAutoGrowNullReferences) throws EvaluationException {

		TypedValue result = readProperty(contextObject, evalContext, this.name);

		// 如果用户请求了可选行为, 则动态创建对象
		if (result.getValue() == null && isAutoGrowNullReferences &&
				nextChildIs(Indexer.class, PropertyOrFieldReference.class)) {
			TypeDescriptor resultDescriptor = result.getTypeDescriptor();
			// 为索引器创建新的集合或Map
			if (List.class == resultDescriptor.getType()) {
				try {
					if (isWritableProperty(this.name, contextObject, evalContext)) {
						List<?> newList = ArrayList.class.newInstance();
						writeProperty(contextObject, evalContext, this.name, newList);
						result = readProperty(contextObject, evalContext, this.name);
					}
				}
				catch (InstantiationException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_LIST_FOR_INDEXING);
				}
				catch (IllegalAccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_LIST_FOR_INDEXING);
				}
			}
			else if (Map.class == resultDescriptor.getType()) {
				try {
					if (isWritableProperty(this.name,contextObject, evalContext)) {
						Map<?,?> newMap = HashMap.class.newInstance();
						writeProperty(contextObject, evalContext, this.name, newMap);
						result = readProperty(contextObject, evalContext, this.name);
					}
				}
				catch (InstantiationException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_MAP_FOR_INDEXING);
				}
				catch (IllegalAccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_MAP_FOR_INDEXING);
				}
			}
			else {
				// 'simple' object
				try {
					if (isWritableProperty(this.name,contextObject, evalContext)) {
						Object newObject  = result.getTypeDescriptor().getType().newInstance();
						writeProperty(contextObject, evalContext, this.name, newObject);
						result = readProperty(contextObject, evalContext, this.name);
					}
				}
				catch (InstantiationException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, result.getTypeDescriptor().getType());
				}
				catch (IllegalAccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, result.getTypeDescriptor().getType());
				}
			}
		}
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object newValue) throws EvaluationException {
		writeProperty(state.getActiveContextObject(), state.getEvaluationContext(), this.name, newValue);
	}

	@Override
	public boolean isWritable(ExpressionState state) throws EvaluationException {
		return isWritableProperty(this.name, state.getActiveContextObject(), state.getEvaluationContext());
	}

	@Override
	public String toStringAST() {
		return this.name;
	}

	/**
	 * 尝试从当前上下文对象中读取命名属性.
	 * 
	 * @return 属性值
	 * @throws EvaluationException 如果访问该属性时有问题或无法找到它
	 */
	private TypedValue readProperty(TypedValue contextObject, EvaluationContext evalContext, String name)
			throws EvaluationException {

		Object targetObject = contextObject.getValue();
		if (targetObject == null && this.nullSafe) {
			return TypedValue.NULL;
		}

		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (accessorToUse != null) {
			if (evalContext.getPropertyAccessors().contains(accessorToUse)) {
				try {
					return accessorToUse.read(evalContext, contextObject.getValue(), name);
				}
				catch (Exception ex) {
					// 这没关系 - 由于类改变, 它可能已经过时了, 让我们尝试换一个新的并在放弃之前调用它...
				}
			}
			this.cachedReadAccessor = null;
		}

		List<PropertyAccessor> accessorsToTry =
				getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
		// 浏览可能能够解析它的访问器.
		// 如果它们是可缓存的访问器, 则获取访问器并使用它.
		// 如果它们不可缓存, 但报告它们可以读取该属性, 那么请他们读取它
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canRead(evalContext, contextObject.getValue(), name)) {
						if (accessor instanceof ReflectivePropertyAccessor) {
							accessor = ((ReflectivePropertyAccessor) accessor).createOptimalAccessor(
									evalContext, contextObject.getValue(), name);
						}
						this.cachedReadAccessor = accessor;
						return accessor.read(evalContext, contextObject.getValue(), name);
					}
				}
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_DURING_PROPERTY_READ, name, ex.getMessage());
			}
		}
		if (contextObject.getValue() == null) {
			throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL, name);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, name,
					FormatHelper.formatClassNameForMessage(getObjectClass(contextObject.getValue())));
		}
	}

	private void writeProperty(TypedValue contextObject, EvaluationContext evalContext, String name, Object newValue)
			throws EvaluationException {

		if (contextObject.getValue() == null && this.nullSafe) {
			return;
		}

		PropertyAccessor accessorToUse = this.cachedWriteAccessor;
		if (accessorToUse != null) {
			if (evalContext.getPropertyAccessors().contains(accessorToUse)) {
				try {
					accessorToUse.write(evalContext, contextObject.getValue(), name, newValue);
					return;
				}
				catch (Exception ex) {
					// 这没关系 - 由于类改变, 它可能已经过时了, 让我们尝试换一个新的并在放弃之前调用它...
				}
			}
			this.cachedWriteAccessor = null;
		}

		List<PropertyAccessor> accessorsToTry =
				getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canWrite(evalContext, contextObject.getValue(), name)) {
						this.cachedWriteAccessor = accessor;
						accessor.write(evalContext, contextObject.getValue(), name, newValue);
						return;
					}
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
						name, ex.getMessage());
			}
		}
		if (contextObject.getValue() == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL, name);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE, name,
					FormatHelper.formatClassNameForMessage(getObjectClass(contextObject.getValue())));
		}
	}

	public boolean isWritableProperty(String name, TypedValue contextObject, EvaluationContext evalContext)
			throws EvaluationException {

		List<PropertyAccessor> accessorsToTry =
				getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
		if (accessorsToTry != null) {
			for (PropertyAccessor accessor : accessorsToTry) {
				try {
					if (accessor.canWrite(evalContext, contextObject.getValue(), name)) {
						return true;
					}
				}
				catch (AccessException ex) {
					// let others try
				}
			}
		}
		return false;
	}

	/**
	 * 确定应该用于尝试和访问指定目标类型的属性的属性解析器集.
	 * 解析器被认为是在有序列表中, 但是在返回的列表中,
	 * 何与输入目标类型完全匹配的 (与可能适用于任何类型的'通用'解析器相对) 都放在列表的开头.
	 * 此外, 有一些特定的解析器可以精确地命名相关的类, 还有一些解析器可以命名特定的类, 但它是我们所拥有的类的父类型.
	 * 这些放在特定解析器集的末尾, 并将在完全匹配的访问器之后, 但在通用访问器之前尝试.
	 * 
	 * @param contextObject 正在尝试访问属性的对象
	 * 
	 * @return 应该尝试访问该属性的解析器列表
	 */
	private List<PropertyAccessor> getPropertyAccessorsToTry(Object contextObject, List<PropertyAccessor> propertyAccessors) {
		Class<?> targetType = (contextObject != null ? contextObject.getClass() : null);

		List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
		List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
		for (PropertyAccessor resolver : propertyAccessors) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) {
				// 通用解析器, 它可以用于任何类型
				generalAccessors.add(resolver);
			}
			else if (targetType != null) {
				for (Class<?> clazz : targets) {
					if (clazz == targetType) {
						specificAccessors.add(resolver);
						break;
					}
					else if (clazz.isAssignableFrom(targetType)) {
						generalAccessors.add(resolver);
					}
				}
			}
		}
		List<PropertyAccessor> resolvers = new ArrayList<PropertyAccessor>();
		resolvers.addAll(specificAccessors);
		generalAccessors.removeAll(specificAccessors);
		resolvers.addAll(generalAccessors);
		return resolvers;
	}
	
	@Override
	public boolean isCompilable() {
		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		return (accessorToUse instanceof CompilablePropertyAccessor &&
				((CompilablePropertyAccessor) accessorToUse).isCompilable());
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (!(accessorToUse instanceof CompilablePropertyAccessor)) {
			throw new IllegalStateException("Property accessor is not compilable: " + accessorToUse);
		}
		Label skipIfNull = null;
		if (nullSafe) {
			mv.visitInsn(DUP);
			skipIfNull = new Label();
			Label continueLabel = new Label();
			mv.visitJumpInsn(IFNONNULL,continueLabel);
			CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
			mv.visitJumpInsn(GOTO, skipIfNull);
			mv.visitLabel(continueLabel);
		}
		((CompilablePropertyAccessor) accessorToUse).generateCode(this.name, mv, cf);
		cf.pushDescriptor(this.exitTypeDescriptor);
		if (originalPrimitiveExitTypeDescriptor != null) {
			// 访问器的输出是基础类型, 但是从上面的块可能是null,
			// 因此, 要在skipIfNull目标处具有公共堆栈元素类型, 必须对基础类型进行封装
			CodeFlow.insertBoxIfNecessary(mv, originalPrimitiveExitTypeDescriptor);
		}
		if (skipIfNull != null) {
			mv.visitLabel(skipIfNull);
		}
	}

	void setExitTypeDescriptor(String descriptor) {
		// 如果此属性或字段访问将返回一个基础类型 - 但它也标记为null安全 - 那么必须将退出类型描述符提升为装箱类型以允许传递空值
		if (this.nullSafe && CodeFlow.isPrimitive(descriptor)) {
			this.originalPrimitiveExitTypeDescriptor = descriptor;
			this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
		}
		else {
			this.exitTypeDescriptor = descriptor;
		}
	}


	private static class AccessorLValue implements ValueRef {

		private final PropertyOrFieldReference ref;

		private final TypedValue contextObject;

		private final EvaluationContext evalContext;

		private final boolean autoGrowNullReferences;

		public AccessorLValue(PropertyOrFieldReference propertyOrFieldReference, TypedValue activeContextObject,
				EvaluationContext evalContext, boolean autoGrowNullReferences) {
			this.ref = propertyOrFieldReference;
			this.contextObject = activeContextObject;
			this.evalContext = evalContext;
			this.autoGrowNullReferences = autoGrowNullReferences;
		}

		@Override
		public TypedValue getValue() {
			TypedValue value =
					this.ref.getValueInternal(this.contextObject, this.evalContext, this.autoGrowNullReferences);
			PropertyAccessor accessorToUse = this.ref.cachedReadAccessor;
			if (accessorToUse instanceof CompilablePropertyAccessor) {
				this.ref.setExitTypeDescriptor(CodeFlow.toDescriptor(((CompilablePropertyAccessor) accessorToUse).getPropertyType()));
			}
			return value;
		}

		@Override
		public void setValue(Object newValue) {
			this.ref.writeProperty(this.contextObject, this.evalContext, this.ref.name, newValue);
		}

		@Override
		public boolean isWritable() {
			return this.ref.isWritableProperty(this.ref.name, this.contextObject, this.evalContext);
		}
	}

}
