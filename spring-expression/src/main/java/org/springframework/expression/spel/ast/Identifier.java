package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;

public class Identifier extends SpelNodeImpl {

	private final TypedValue id;


	public Identifier(String payload, int pos) {
		super(pos);
		this.id = new TypedValue(payload);
	}


	@Override
	public String toStringAST() {
		return (String) this.id.getValue();
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) {
		return this.id;
	}

}
