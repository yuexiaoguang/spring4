package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.InternalParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;

/**
 * 表示文字的节点的公共超类 (boolean, string, number, etc).
 */
public abstract class Literal extends SpelNodeImpl {

	private final String originalValue;


	public Literal(String originalValue, int pos) {
		super(pos);
		this.originalValue = originalValue;
	}


	public final String getOriginalValue() {
		return this.originalValue;
	}

	@Override
	public final TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
		return getLiteralValue();
	}

	@Override
	public String toString() {
		return getLiteralValue().getValue().toString();
	}

	@Override
	public String toStringAST() {
		return toString();
	}


	public abstract TypedValue getLiteralValue();


	/**
	 * 处理数字的字符串形式, 使用指定的基数并返回一个适当的文字来保存它.
	 * 任何表示long的后缀都将被考虑在内 (支持 'l' 或 'L').
	 * 
	 * @param numberToken 将数字作为其有效负载的token (eg. 1234 或 0xCAFE)
	 * @param radix 数字的基础
	 * 
	 * @return 可以表示它的Literal子类型
	 */
	public static Literal getIntLiteral(String numberToken, int pos, int radix) {
		try {
			int value = Integer.parseInt(numberToken, radix);
			return new IntLiteral(numberToken, pos, value);
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(pos>>16, ex, SpelMessage.NOT_AN_INTEGER, numberToken));
		}
	}

	public static Literal getLongLiteral(String numberToken, int pos, int radix) {
		try {
			long value = Long.parseLong(numberToken, radix);
			return new LongLiteral(numberToken, pos, value);
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(pos>>16, ex, SpelMessage.NOT_A_LONG, numberToken));
		}
	}

	public static Literal getRealLiteral(String numberToken, int pos, boolean isFloat) {
		try {
			if (isFloat) {
				float value = Float.parseFloat(numberToken);
				return new FloatLiteral(numberToken, pos, value);
			}
			else {
				double value = Double.parseDouble(numberToken);
				return new RealLiteral(numberToken, pos, value);
			}
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(pos>>16, ex, SpelMessage.NOT_A_REAL, numberToken));
		}
	}

}
