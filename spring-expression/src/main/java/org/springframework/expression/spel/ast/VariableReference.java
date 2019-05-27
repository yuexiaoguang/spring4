package org.springframework.expression.spel.ast;

import java.lang.reflect.Modifier;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;

/**
 * 表示变量引用, eg. #someVar.
 * 请注意, 这与$someVar之类的 *local*变量不同
 */
public class VariableReference extends SpelNodeImpl {

	// 众所周知的变量:
	private static final String THIS = "this";  // 当前活动的上下文对象

	private static final String ROOT = "root";  // 根上下文对象


	private final String name;


	public VariableReference(String variableName, int pos) {
		super(pos);
		this.name = variableName;
	}


	@Override
	public ValueRef getValueRef(ExpressionState state) throws SpelEvaluationException {
		if (this.name.equals(THIS)) {
			return new ValueRef.TypedValueHolderValueRef(state.getActiveContextObject(),this);
		}
		if (this.name.equals(ROOT)) {
			return new ValueRef.TypedValueHolderValueRef(state.getRootContextObject(),this);
		}
		TypedValue result = state.lookupVariable(this.name);
		// null值表示, 值为null或未找到变量
		return new VariableRef(this.name,result,state.getEvaluationContext());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
		if (this.name.equals(THIS)) {
			return state.getActiveContextObject();
		}
		if (this.name.equals(ROOT)) {
			TypedValue result = state.getRootContextObject();
			this.exitTypeDescriptor = CodeFlow.toDescriptorFromObject(result.getValue());
			return result;
		}
		TypedValue result = state.lookupVariable(this.name);
		Object value = result.getValue();
		if (value == null || !Modifier.isPublic(value.getClass().getModifiers())) {
			// 如果类型不是public, 那么当generateCode生成checkcast时, 将发生IllegalAccessError.
			// 如果使用Object是不够的, 则可以遍历第一个public类型的层次结构.
			this.exitTypeDescriptor = "Ljava/lang/Object";
		}
		else {
			this.exitTypeDescriptor = CodeFlow.toDescriptorFromObject(value);
		}
		// null值表示, 值为null或未找到变量
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object value) throws SpelEvaluationException {
		state.setVariable(this.name, value);
	}

	@Override
	public String toStringAST() {
		return "#" + this.name;
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return !(this.name.equals(THIS) || this.name.equals(ROOT));
	}


	class VariableRef implements ValueRef {

		private final String name;

		private final TypedValue value;

		private final EvaluationContext evaluationContext;


		public VariableRef(String name, TypedValue value,
				EvaluationContext evaluationContext) {
			this.name = name;
			this.value = value;
			this.evaluationContext = evaluationContext;
		}


		@Override
		public TypedValue getValue() {
			return this.value;
		}

		@Override
		public void setValue(Object newValue) {
			this.evaluationContext.setVariable(this.name, newValue);
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

	@Override
	public boolean isCompilable() {
		return this.exitTypeDescriptor!=null;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if (this.name.equals(ROOT)) {
			mv.visitVarInsn(ALOAD,1);
		}
		else {
			mv.visitVarInsn(ALOAD, 2);
			mv.visitLdcInsn(name);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/springframework/expression/EvaluationContext", "lookupVariable", "(Ljava/lang/String;)Ljava/lang/Object;",true);
		}
		CodeFlow.insertCheckCast(mv,this.exitTypeDescriptor);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}
}
