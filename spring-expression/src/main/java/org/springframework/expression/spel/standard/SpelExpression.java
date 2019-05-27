package org.springframework.expression.spel.standard;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * {@code SpelExpression}表示已准备好在指定上下文中计算的已解析的 (有效的)表达式.
 * 表达式可以单独评估, 也可以在指定的上下文中评估.
 * 在表达式求值期间, 可能会要求上下文解析对类型, bean, 属性, 方法的引用.
 */
public class SpelExpression implements Expression {

	// 在编译表达式之前解释表达式的次数
	private static final int INTERPRETED_COUNT_THRESHOLD = 100;

	// 放弃之前尝试编译表达式的次数
	private static final int FAILED_ATTEMPTS_THRESHOLD = 100;


	private final String expression;

	private final SpelNodeImpl ast;

	private final SpelParserConfiguration configuration;

	// 如果用户未提供覆盖, 则使用默认上下文
	private EvaluationContext evaluationContext;

	// 保存表达式的编译形式 (如果已编译)
	private CompiledExpression compiledAst;

	// 表达式被解释后多次计数 - 当达到某个限制时可以触发编译
	private volatile int interpretedCount = 0;

	// 尝试编译和失败的次数 - 不可能编译它时, 最终放弃.
	private volatile int failedAttempts = 0;


	/**
	 * 构造一个仅由解析器使用的表达式.
	 */
	public SpelExpression(String expression, SpelNodeImpl ast, SpelParserConfiguration configuration) {
		this.expression = expression;
		this.ast = ast;
		this.configuration = configuration;
	}


	/**
	 * 如果在评估调用中未指定任何评估上下文, 则设置将使用的评估上下文.
	 * 
	 * @param evaluationContext 要使用的评估上下文
	 */
	public void setEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	/**
	 * 如果评估调用没有提供, 则返回将使用的默认评估上下文.
	 * 
	 * @return 默认评估上下文
	 */
	public EvaluationContext getEvaluationContext() {
		if (this.evaluationContext == null) {
			this.evaluationContext = new StandardEvaluationContext();
		}
		return this.evaluationContext;
	}


	// implementing Expression

	@Override
	public String getExpressionString() {
		return this.expression;
	}

	@Override
	public Object getValue() throws EvaluationException {
		if (this.compiledAst != null) {
			try {
				TypedValue contextRoot =
						(this.evaluationContext != null ? this.evaluationContext.getRootObject() : null);
				return this.compiledAst.getValue(
						(contextRoot != null ? contextRoot.getValue() : null), this.evaluationContext);
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(Class<T> expectedResultType) throws EvaluationException {
		if (this.compiledAst != null) {
			try {
				TypedValue contextRoot =
						(this.evaluationContext != null ? this.evaluationContext.getRootObject() : null);
				Object result = this.compiledAst.getValue(
						(contextRoot != null ? contextRoot.getValue() : null), this.evaluationContext);
				if (expectedResultType == null) {
					return (T) result;
				}
				else {
					return ExpressionUtils.convertTypedValue(
							getEvaluationContext(), new TypedValue(result), expectedResultType);
				}
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(
				expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	public Object getValue(Object rootObject) throws EvaluationException {
		if (this.compiledAst != null) {
			try {
				return this.compiledAst.getValue(rootObject, evaluationContext);
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(Object rootObject, Class<T> expectedResultType) throws EvaluationException {
		if (this.compiledAst != null) {
			try {
				Object result = this.compiledAst.getValue(rootObject, null);
				if (expectedResultType == null) {
					return (T)result;
				}
				else {
					return ExpressionUtils.convertTypedValue(
							getEvaluationContext(), new TypedValue(result), expectedResultType);
				}
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(
				expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
	}

	@Override
	public Object getValue(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");

		if (this.compiledAst != null) {
			try {
				TypedValue contextRoot = context.getRootObject();
				return this.compiledAst.getValue(contextRoot.getValue(), context);
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(EvaluationContext context, Class<T> expectedResultType) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");

		if (this.compiledAst != null) {
			try {
				TypedValue contextRoot = context.getRootObject();
				Object result = this.compiledAst.getValue(contextRoot.getValue(), context);
				if (expectedResultType != null) {
					return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
				}
				else {
					return (T) result;
				}
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");

		if (this.compiledAst != null) {
			try {
				return this.compiledAst.getValue(rootObject,context);
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		Object result = this.ast.getValue(expressionState);
		checkCompile(expressionState);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> expectedResultType)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext is required");

		if (this.compiledAst != null) {
			try {
				Object result = this.compiledAst.getValue(rootObject, context);
				if (expectedResultType != null) {
					return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
				}
				else {
					return (T) result;
				}
			}
			catch (Throwable ex) {
				// 如果在混合模式下运行, 则还原为已解释
				if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
					this.interpretedCount = 0;
					this.compiledAst = null;
				}
				else {
					// 在SpelCompilerMode.immediate模式下运行 - 将异常传播给调用者
					throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
				}
			}
		}

		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
		checkCompile(expressionState);
		return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
	}

	@Override
	public Class<?> getValueType() throws EvaluationException {
		return getValueType(getEvaluationContext());
	}

	@Override
	public Class<?> getValueType(Object rootObject) throws EvaluationException {
		return getValueType(getEvaluationContext(), rootObject);
	}

	@Override
	public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
		return (typeDescriptor != null ? typeDescriptor.getType() : null);
	}

	@Override
	public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
		return (typeDescriptor != null ? typeDescriptor.getType() : null);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
		return getValueTypeDescriptor(getEvaluationContext());
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
		ExpressionState expressionState =
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		ExpressionState expressionState = new ExpressionState(context, this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext is required");
		ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
		return this.ast.getValueInternal(expressionState).getTypeDescriptor();
	}

	@Override
	public boolean isWritable(Object rootObject) throws EvaluationException {
		return this.ast.isWritable(
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		return this.ast.isWritable(new ExpressionState(context, this.configuration));
	}

	@Override
	public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		return this.ast.isWritable(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
	}

	@Override
	public void setValue(Object rootObject, Object value) throws EvaluationException {
		this.ast.setValue(
				new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, Object value) throws EvaluationException {
		Assert.notNull(context, "EvaluationContext is required");
		this.ast.setValue(new ExpressionState(context, this.configuration), value);
	}

	@Override
	public void setValue(EvaluationContext context, Object rootObject, Object value)
			throws EvaluationException {

		Assert.notNull(context, "EvaluationContext is required");
		this.ast.setValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration), value);
	}


	/**
	 * 如果表达式的评估超过了触发编译的阈值次数, 则编译该表达式.
	 * 
	 * @param expressionState 用于确定编译模式的表达式状态
	 */
	private void checkCompile(ExpressionState expressionState) {
		this.interpretedCount++;
		SpelCompilerMode compilerMode = expressionState.getConfiguration().getCompilerMode();
		if (compilerMode != SpelCompilerMode.OFF) {
			if (compilerMode == SpelCompilerMode.IMMEDIATE) {
				if (this.interpretedCount > 1) {
					compileExpression();
				}
			}
			else {
				// compilerMode = SpelCompilerMode.MIXED
				if (this.interpretedCount > INTERPRETED_COUNT_THRESHOLD) {
					compileExpression();
				}
			}
		}
	}


	/**
	 * 执行表达式编译.
	 * 只有在确定了所有节点的退出描述符后, 这才会成功.
	 * 如果编译失败并且失败超过100次, 则表达式不再适合编译.
	 */
	public boolean compileExpression() {
		if (this.failedAttempts > FAILED_ATTEMPTS_THRESHOLD) {
			// Don't try again
			return false;
		}
		if (this.compiledAst == null) {
			synchronized (this.expression) {
				// 在此线程进入同步块之前, 可能由另一个线程编译
				if (this.compiledAst != null) {
					return true;
				}
				SpelCompiler compiler = SpelCompiler.getCompiler(this.configuration.getCompilerClassLoader());
				this.compiledAst = compiler.compile(this.ast);
				if (this.compiledAst == null) {
					this.failedAttempts++;
				}
			}
		}
		return (this.compiledAst != null);
	}

	/**
	 * 如果表达式已使用已编译的形式, 则导致表达式恢复为被解释.
	 * 它还会重置编译尝试失败计数 (如果在100次尝试后无法编译, 则表达式通常不再被视为可编译).
	 */
	public void revertToInterpreted() {
		this.compiledAst = null;
		this.interpretedCount = 0;
		this.failedAttempts = 0;
	}

	/**
	 * 返回表达式的抽象语法树.
	 */
	public SpelNode getAST() {
		return this.ast;
	}

	/**
	 * 为表达式生成抽象语法树的字符串表示形式.
	 * 这应该理想地看起来像输入表达式, 但格式正确, 因为在表达式的解析过程中将丢弃任何不必要的空格.
	 * 
	 * @return AST的字符串表示形式
	 */
	public String toStringAST() {
		return this.ast.toStringAST();
	}

	private TypedValue toTypedValue(Object object) {
		return (object != null ? new TypedValue(object) : TypedValue.NULL);
	}
}
