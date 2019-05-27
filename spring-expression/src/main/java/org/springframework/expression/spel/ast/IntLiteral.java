package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;

/**
 * 表达式语言AST节点, 表示整数.
 */
public class IntLiteral extends Literal {

	private final TypedValue value;


	public IntLiteral(String payload, int pos, int value) {
		super(payload, pos);
		this.value = new TypedValue(value);
		this.exitTypeDescriptor = "I";
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
		int intValue = (Integer) this.value.getValue();
		if (intValue == -1) {
			// 不确定能到这里, 因为-1是OpMinus
			mv.visitInsn(ICONST_M1);
		}
		else if (intValue >= 0 && intValue < 6) {
			mv.visitInsn(ICONST_0 + intValue);
		}
		else {
			mv.visitLdcInsn(intValue);
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
