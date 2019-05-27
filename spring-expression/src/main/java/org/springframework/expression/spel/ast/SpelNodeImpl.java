package org.springframework.expression.spel.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 解析的Spring Expression Language格式表达式中所有AST节点的公共超类型.
 */
public abstract class SpelNodeImpl implements SpelNode, Opcodes {

	private static final SpelNodeImpl[] NO_CHILDREN = new SpelNodeImpl[0];


	protected int pos; // start = top 16bits, end = bottom 16bits

	protected SpelNodeImpl[] children = SpelNodeImpl.NO_CHILDREN;

	private SpelNodeImpl parent;

	/**
	 * 指示此表达式节点的结果的类型描述符.
	 * 一旦知道就立即设置. 对于文字节点, 它是立即知道的.
	 * 对于属性访问或方法调用, 在对该节点进行一次评估之后就知道了.
	 * <p>描述符类似于字节码形式, 但更容易使用.
	 * 它不包括尾随分号 (对于非数组引用类型).
	 * 例如: Ljava/lang/String, I, [I
     */
	protected volatile String exitTypeDescriptor;


	public SpelNodeImpl(int pos, SpelNodeImpl... operands) {
		this.pos = pos;
		// pos结合了开始和结束, 因此永远不能为零, 因为令牌不能为零长度
		Assert.isTrue(pos != 0, "Pos must not be 0");
		if (!ObjectUtils.isEmpty(operands)) {
			this.children = operands;
			for (SpelNodeImpl childNode : operands) {
				childNode.parent = this;
			}
		}
	}


	@Deprecated
	protected SpelNodeImpl getPreviousChild() {
		SpelNodeImpl result = null;
		if (this.parent != null) {
			for (SpelNodeImpl child : this.parent.children) {
				if (this == child) {
					break;
				}
				result = child;
			}
		}
		return result;
	}

	/**
     * @return true 如果下一个子节点是指定类之一
     */
	protected boolean nextChildIs(Class<?>... clazzes) {
		if (this.parent != null) {
			SpelNodeImpl[] peers = this.parent.children;
			for (int i = 0, max = peers.length; i < max; i++) {
				if (this == peers[i]) {
					if (i + 1 >= max) {
						return false;
					}
					Class<?> clazz = peers[i + 1].getClass();
					for (Class<?> desiredClazz : clazzes) {
						if (clazz.equals(desiredClazz)) {
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public final Object getValue(ExpressionState expressionState) throws EvaluationException {
		if (expressionState != null) {
			return getValueInternal(expressionState).getValue();
		}
		else {
			// 配置没有设置 - 这有关系?
			return getValue(new ExpressionState(new StandardEvaluationContext()));
		}
	}

	@Override
	public final TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException {
		if (expressionState != null) {
			return getValueInternal(expressionState);
		}
		else {
			// 配置没有设置 - 这有关系?
			return getTypedValue(new ExpressionState(new StandardEvaluationContext()));
		}
	}

	// 默认Ast节点不可写
	@Override
	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(ExpressionState expressionState, Object newValue) throws EvaluationException {
		throw new SpelEvaluationException(getStartPosition(),
				SpelMessage.SETVALUE_NOT_SUPPORTED, getClass());
	}

	@Override
	public SpelNode getChild(int index) {
		return this.children[index];
	}

	@Override
	public int getChildCount() {
		return this.children.length;
	}

	@Override
	public Class<?> getObjectClass(Object obj) {
		if (obj == null) {
			return null;
		}
		return (obj instanceof Class ? ((Class<?>) obj) : obj.getClass());
	}

	protected final <T> T getValue(ExpressionState state, Class<T> desiredReturnType) throws EvaluationException {
		return ExpressionUtils.convertTypedValue(state.getEvaluationContext(), getValueInternal(state), desiredReturnType);
	}

	@Override
	public int getStartPosition() {
		return (this.pos >> 16);
	}

	@Override
	public int getEndPosition() {
		return (this.pos & 0xffff);
	}

	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		throw new SpelEvaluationException(this.pos, SpelMessage.NOT_ASSIGNABLE, toStringAST());
	}

	/**
	 * 检查节点是否可以编译为字节码.
	 * 每个节点中的推理可能不同, 但通常涉及检查节点的退出类型描述符是否已知, 并且任何相关的子节点是否可编译.
	 * 
	 * @return {@code true} 如果此节点可以编译为字节码
	 */
	public boolean isCompilable() {
		return false;
	}

	/**
	 * 将此节点的字节码生成到提供的访问器中.
	 * 有关正在编译的当前表达式的上下文信息可在codeflow对象中获得.
	 * 例如, 它将包含有关当前堆栈中对象类型的信息.
	 * 
	 * @param mv 应该生成代码的ASM MethodVisitor
	 * @param cf 一个上下文对象, 其中包含有关堆栈内容的信息
	 */
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		throw new IllegalStateException(getClass().getName() +" has no generateCode(..) method");
	}

	public String getExitDescriptor() {
		return this.exitTypeDescriptor;
	}

	public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;

	
	/**
	 * 生成用于处理为指定方法构建参数值的代码.
	 * 此方法将考虑调用的方法是否为varargs方法, 如果是, 则参数值将被适当地打包到数组中.
	 * 
	 * @param mv 应该生成代码的方法访问器
	 * @param cf 当前codeflow
	 * @param member 正在为其设置参数的方法或构造函数
	 * @param arguments 表达式提供的参数值的表达式节点
	 */
	protected static void generateCodeForArguments(MethodVisitor mv, CodeFlow cf, Member member, SpelNodeImpl[] arguments) {
		String[] paramDescriptors = null;
		boolean isVarargs = false;
		if (member instanceof Constructor) {
			Constructor<?> ctor = (Constructor<?>) member;
			paramDescriptors = CodeFlow.toDescriptors(ctor.getParameterTypes());
			isVarargs = ctor.isVarArgs();
		}
		else { // Method
			Method method = (Method)member;
			paramDescriptors = CodeFlow.toDescriptors(method.getParameterTypes());
			isVarargs = method.isVarArgs();
		}
		if (isVarargs) {
			// final 参数可能需要也可能不需要打包成数组, 或者没有传递任何东西来满足varargs, 因此需要构建一些东西.
			int p = 0; // 正在处理当前提供的参数
			int childCount = arguments.length;
						
			// 满足除最后一个之外的所有参数要求
			for (p = 0; p < paramDescriptors.length - 1; p++) {
				generateCodeForArgument(mv, cf, arguments[p], paramDescriptors[p]);
			}
			
			SpelNodeImpl lastChild = (childCount == 0 ? null : arguments[childCount - 1]);
			String arrayType = paramDescriptors[paramDescriptors.length - 1];
			// 确定最终传递的参数是否已经以数组形式适当地打包以传递给方法
			if (lastChild != null && arrayType.equals(lastChild.getExitDescriptor())) {
				generateCodeForArgument(mv, cf, lastChild, paramDescriptors[p]);
			}
			else {
				arrayType = arrayType.substring(1); // 去掉开头的 '[', 可能还有其他 '['
				// 构建大到足以容纳剩余参数的数组
				CodeFlow.insertNewArrayCode(mv, childCount - p, arrayType);
				// 将剩余的参数打包到数组中
				int arrayindex = 0;
				while (p < childCount) {
					SpelNodeImpl child = arguments[p];
					mv.visitInsn(DUP);
					CodeFlow.insertOptimalLoad(mv, arrayindex++);
					generateCodeForArgument(mv, cf, child, arrayType);
					CodeFlow.insertArrayStore(mv, arrayType);
					p++;
				}
			}
		}
		else {
			for (int i = 0; i < paramDescriptors.length;i++) {
				generateCodeForArgument(mv, cf, arguments[i], paramDescriptors[i]);
			}
		}
	}

	/**
	 * 请求参数生成其字节码, 然后使用任何装箱/拆箱/类型检查进行跟进, 以确保它与预期的参数描述符匹配.
	 */
	protected static void generateCodeForArgument(MethodVisitor mv, CodeFlow cf, SpelNodeImpl argument, String paramDesc) {
		cf.enterCompilationScope();
		argument.generateCode(mv, cf);
		String lastDesc = cf.lastDescriptor();
		boolean primitiveOnStack = CodeFlow.isPrimitive(lastDesc);
		// 检查是否需要将其包装为方法引用?
		if (primitiveOnStack && paramDesc.charAt(0) == 'L') {
			CodeFlow.insertBoxIfNecessary(mv, lastDesc.charAt(0));
		}
		else if (paramDesc.length() == 1 && !primitiveOnStack) {
			CodeFlow.insertUnboxInsns(mv, paramDesc.charAt(0), lastDesc);
		}
		else if (!paramDesc.equals(lastDesc)) {
			// 在子类型的情况下这是不必要的 (e.g. 方法接受Number, 但传入的是Integer)
			CodeFlow.insertCheckCast(mv, paramDesc);
		}
		cf.exitCompilationScope();
	}
}
