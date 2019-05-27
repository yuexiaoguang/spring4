package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * 表示文字值{@code TRUE}和{@code FALSE}.
 */
public class BooleanLiteral extends Literal {

	private final BooleanTypedValue value;


	public BooleanLiteral(String payload, int pos, boolean value) {
		super(payload, pos);
		this.value = BooleanTypedValue.forValue(value);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getLiteralValue() {
		return this.value;
	}
	
	@Override
	public boolean isCompilable() {
		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if (this.value == BooleanTypedValue.TRUE) {
			mv.visitLdcInsn(1);		
		}
		else {
			mv.visitLdcInsn(0);
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
