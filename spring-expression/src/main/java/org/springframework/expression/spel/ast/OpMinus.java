package org.springframework.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.NumberUtils;

/**
 * 减号运算符支持:
 * <ul>
 * <li>数字的减法
 * <li>从一个字符的字符串中减去一个int (有效地减少该字符), 因此 'd'-3='a'
 * </ul>
 *
 * <p>它可以用作数字的一元运算符.
 * 当操作数类型不同时, 将执行标准提升 (double-int=double).
 * 对于其他选项, 它遵循已注册的overloader.
 */
public class OpMinus extends Operator {

	public OpMinus(int pos, SpelNodeImpl... operands) {
		super("-", pos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		if (rightOp == null) {  // 如果只有一个操作数, 那么这是一元减去
			Object operand = leftOp.getValueInternal(state).getValue();
			if (operand instanceof Number) {
				if (operand instanceof BigDecimal) {
					return new TypedValue(((BigDecimal) operand).negate());
				}
				else if (operand instanceof Double) {
					this.exitTypeDescriptor = "D";
					return new TypedValue(0 - ((Number) operand).doubleValue());
				}
				else if (operand instanceof Float) {
					this.exitTypeDescriptor = "F";
					return new TypedValue(0 - ((Number) operand).floatValue());
				}
				else if (operand instanceof BigInteger) {
					return new TypedValue(((BigInteger) operand).negate());
				}
				else if (operand instanceof Long) {
					this.exitTypeDescriptor = "J";
					return new TypedValue(0 - ((Number) operand).longValue());
				}
				else if (operand instanceof Integer) {
					this.exitTypeDescriptor = "I";
					return new TypedValue(0 - ((Number) operand).intValue());
				}
				else if (operand instanceof Short) {
					return new TypedValue(0 - ((Number) operand).shortValue());
				}
				else if (operand instanceof Byte) {
					return new TypedValue(0 - ((Number) operand).byteValue());
				}
				else {
					// 未知Number子类型 -> 最佳猜测是double减法
					return new TypedValue(0 - ((Number) operand).doubleValue());
				}
			}
			return state.operate(Operation.SUBTRACT, operand, null);
		}

		Object left = leftOp.getValueInternal(state).getValue();
		Object right = rightOp.getValueInternal(state).getValue();

		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.subtract(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() - rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() - rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.subtract(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() - rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() - rightNumber.intValue());
			}
			else {
				// 未知Number子类型 -> 最佳猜测是double减法
				return new TypedValue(leftNumber.doubleValue() - rightNumber.doubleValue());
			}
		}

		if (left instanceof String && right instanceof Integer && ((String) left).length() == 1) {
			String theString = (String) left;
			Integer theInteger = (Integer) right;
			// 实现字符 - int (ie. b - 1 = a)
			return new TypedValue(Character.toString((char) (theString.charAt(0) - theInteger)));
		}

		return state.operate(Operation.SUBTRACT, left, right);
	}

	@Override
	public String toStringAST() {
		if (getRightOperand() == null) {  // unary minus
			return "-" + getLeftOperand().toStringAST();
		}
		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length < 2) {
			return null;
		}
		return this.children[1];
	}

	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length > 1) {
			if (!getRightOperand().isCompilable()) {
				return false;
			}
		}
		return (this.exitTypeDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		getLeftOperand().generateCode(mv, cf);
		String leftDesc = getLeftOperand().exitTypeDescriptor;
		CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, this.exitTypeDescriptor.charAt(0));
		if (this.children.length > 1) {
			cf.enterCompilationScope();
			getRightOperand().generateCode(mv, cf);
			String rightDesc = getRightOperand().exitTypeDescriptor;
			cf.exitCompilationScope();
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, this.exitTypeDescriptor.charAt(0));
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(ISUB);
					break;
				case 'J':
					mv.visitInsn(LSUB);
					break;
				case 'F':
					mv.visitInsn(FSUB);
					break;
				case 'D':
					mv.visitInsn(DSUB);
					break;
				default:
					throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
			}
		}
		else {
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(INEG);
					break;
				case 'J':
					mv.visitInsn(LNEG);
					break;
				case 'F':
					mv.visitInsn(FNEG);
					break;
				case 'D':
					mv.visitInsn(DNEG);
					break;
				default:
					throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
