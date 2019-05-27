package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;

/**
 * 表达式语言AST节点, 表示长整数.
 */
public class LongLiteral extends Literal {

	private final TypedValue value;


	public LongLiteral(String payload, int pos, long value) {
		super(payload, pos);
		this.value = new TypedValue(value);
		this.exitTypeDescriptor = "J";
	}


	@Override
	public TypedValue getLiteralValue() {
		return this.value;
	}
	
	@Override
	public boolean isCompilable() {
		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		mv.visitLdcInsn(this.value.getValue());
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
