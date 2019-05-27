package org.springframework.expression.spel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * ExpressionState用于维护每个表达式评估状态, 其他表达式看不到对它的任何更改,
 * 但它提供了一个位置来保存局部变量和复合表达式中的组件表达式以通信状态.
 * 这与在表达式评估之间共享的EvaluationContext形成对比, 并且其他表达式或任何选择提出上下文问题的代码都会看到对它的任何更改.
 *
 * <p>它还可以作为定义各种AST节点可能需要的公共实用程序例程的地方.
 */
public class ExpressionState {

	private final EvaluationContext relatedContext;

	private final TypedValue rootObject;

	private final SpelParserConfiguration configuration;

	private Stack<TypedValue> contextObjects;

	private Stack<VariableScope> variableScopes;

	// 当输入新范围时, 有一个新的基础对象应该用于 '#this'引用 (或作为非限定引用的目标).
	// 此堆栈在每个嵌套的作用域级别捕获这些对象.
	// 例如:
	// #list1.?[#list2.contains(#this)]
	// 在输入选择时, 我们输入一个新范围, 现在 #this 是list1中的元素
	private Stack<TypedValue> scopeRootObjects;


	public ExpressionState(EvaluationContext context) {
		this(context, context.getRootObject(), new SpelParserConfiguration(false, false));
	}

	public ExpressionState(EvaluationContext context, SpelParserConfiguration configuration) {
		this(context, context.getRootObject(), configuration);
	}

	public ExpressionState(EvaluationContext context, TypedValue rootObject) {
		this(context, rootObject, new SpelParserConfiguration(false, false));
	}

	public ExpressionState(EvaluationContext context, TypedValue rootObject, SpelParserConfiguration configuration) {
		Assert.notNull(context, "EvaluationContext must not be null");
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.relatedContext = context;
		this.rootObject = rootObject;
		this.configuration = configuration;
	}


	private void ensureVariableScopesInitialized() {
		if (this.variableScopes == null) {
			this.variableScopes = new Stack<VariableScope>();
			// top level empty variable scope
			this.variableScopes.add(new VariableScope());
		}
		if (this.scopeRootObjects == null) {
			this.scopeRootObjects = new Stack<TypedValue>();
		}
	}

	/**
	 * 活动上下文对象是解析属性/etc的非限定引用所针对的对象.
	 */
	public TypedValue getActiveContextObject() {
		if (CollectionUtils.isEmpty(this.contextObjects)) {
			return this.rootObject;
		}
		return this.contextObjects.peek();
	}

	public void pushActiveContextObject(TypedValue obj) {
		if (this.contextObjects == null) {
			this.contextObjects = new Stack<TypedValue>();
		}
		this.contextObjects.push(obj);
	}

	public void popActiveContextObject() {
		if (this.contextObjects == null) {
			this.contextObjects = new Stack<TypedValue>();
		}
		this.contextObjects.pop();
	}

	public TypedValue getRootContextObject() {
		return this.rootObject;
	}

	public TypedValue getScopeRootContextObject() {
		if (CollectionUtils.isEmpty(this.scopeRootObjects)) {
			return this.rootObject;
		}
		return this.scopeRootObjects.peek();
	}

	public void setVariable(String name, Object value) {
		this.relatedContext.setVariable(name, value);
	}

	public TypedValue lookupVariable(String name) {
		Object value = this.relatedContext.lookupVariable(name);
		return (value != null ? new TypedValue(value) : TypedValue.NULL);
	}

	public TypeComparator getTypeComparator() {
		return this.relatedContext.getTypeComparator();
	}

	public Class<?> findType(String type) throws EvaluationException {
		return this.relatedContext.getTypeLocator().findType(type);
	}

	public Object convertValue(Object value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
		return this.relatedContext.getTypeConverter().convertValue(value,
				TypeDescriptor.forObject(value), targetTypeDescriptor);
	}

	public TypeConverter getTypeConverter() {
		return this.relatedContext.getTypeConverter();
	}

	public Object convertValue(TypedValue value, TypeDescriptor targetTypeDescriptor) throws EvaluationException {
		Object val = value.getValue();
		return this.relatedContext.getTypeConverter().convertValue(val, TypeDescriptor.forObject(val), targetTypeDescriptor);
	}

	/*
	 * 调用函数时输入新范围.
	 */
	public void enterScope(Map<String, Object> argMap) {
		ensureVariableScopesInitialized();
		this.variableScopes.push(new VariableScope(argMap));
		this.scopeRootObjects.push(getActiveContextObject());
	}

	public void enterScope() {
		ensureVariableScopesInitialized();
		this.variableScopes.push(new VariableScope(Collections.<String,Object>emptyMap()));
		this.scopeRootObjects.push(getActiveContextObject());
	}

	public void enterScope(String name, Object value) {
		ensureVariableScopesInitialized();
		this.variableScopes.push(new VariableScope(name, value));
		this.scopeRootObjects.push(getActiveContextObject());
	}

	public void exitScope() {
		ensureVariableScopesInitialized();
		this.variableScopes.pop();
		this.scopeRootObjects.pop();
	}

	public void setLocalVariable(String name, Object value) {
		ensureVariableScopesInitialized();
		this.variableScopes.peek().setVariable(name, value);
	}

	public Object lookupLocalVariable(String name) {
		ensureVariableScopesInitialized();
		int scopeNumber = this.variableScopes.size() - 1;
		for (int i = scopeNumber; i >= 0; i--) {
			if (this.variableScopes.get(i).definesVariable(name)) {
				return this.variableScopes.get(i).lookupVariable(name);
			}
		}
		return null;
	}

	public TypedValue operate(Operation op, Object left, Object right) throws EvaluationException {
		OperatorOverloader overloader = this.relatedContext.getOperatorOverloader();
		if (overloader.overridesOperation(op, left, right)) {
			Object returnValue = overloader.operate(op, left, right);
			return new TypedValue(returnValue);
		}
		else {
			String leftType = (left == null ? "null" : left.getClass().getName());
			String rightType = (right == null? "null" : right.getClass().getName());
			throw new SpelEvaluationException(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES, op, leftType, rightType);
		}
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return this.relatedContext.getPropertyAccessors();
	}

	public EvaluationContext getEvaluationContext() {
		return this.relatedContext;
	}

	public SpelParserConfiguration getConfiguration() {
		return this.configuration;
	}


	/**
	 * 调用函数时会输入一个新范围, 它用于将参数保存到函数调用中.
	 * 如果参数的名称与更高级别范围中的参数名称冲突, 则在执行函数时将无法访问更高级别范围中的参数名称.
	 * 函数返回时, 将退出范围.
	 */
	private static class VariableScope {

		private final Map<String, Object> vars = new HashMap<String, Object>();

		public VariableScope() {
		}

		public VariableScope(Map<String, Object> arguments) {
			if (arguments != null) {
				this.vars.putAll(arguments);
			}
		}

		public VariableScope(String name, Object value) {
			this.vars.put(name,value);
		}

		public Object lookupVariable(String name) {
			return this.vars.get(name);
		}

		public void setVariable(String name, Object value) {
			this.vars.put(name,value);
		}

		public boolean definesVariable(String name) {
			return this.vars.containsKey(name);
		}
	}
}
