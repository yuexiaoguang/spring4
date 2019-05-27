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
 * 实现{@code multiply}运算符.
 *
 * <p>转换和提升按照
 * <a href="http://java.sun.com/docs/books/jls/third_edition/html/conversions.html">Java语言规范的第5.6.2节</a>
 * 中的定义进行处理, 其中包含{@code BigDecimal}/{@code BigInteger}管理:
 *
 * <p>如果任何操作数是引用类型, 则执行拆箱转换 (Section 5.1.8). 那么:<br>
 * 如果任一操作数的类型为{@code BigDecimal}, 则另一个操作数将转换为{@code BigDecimal}.<br>
 * 如果任一操作数的类型为 double, 则另一个操作数将转换为 double.<br>
 * 否则, 如果任一操作数的类型为 float, 则另一个操作数将转换为 float.<br>
 * 如果任一操作数的类型为 {@code BigInteger}, 则另一个操作数将转换为{@code BigInteger}.<br>
 * 否则, 如果任一操作数的类型为 long, 则另一个操作数将转换为 long.<br>
 * 否则, 两个操作数都转换为int类型.
 */
public class OpMultiply extends Operator {

	public OpMultiply(int pos, SpelNodeImpl... operands) {
		super("*", pos, operands);
	}


	/**
	 * 对于某些类型的受支持操作数, 此处直接实现{@code multiply}运算符,
	 * 否则将委托给任何已注册的overloader以获取此处不支持的类型.
	 * <p>支持的操作数类型:
	 * <ul>
	 * <li>数字
	 * <li>String 和 int ('abc' * 2 == 'abcabc')
	 * </ul>
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
		Object rightOperand = getRightOperand().getValueInternal(state).getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.multiply(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() * rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.multiply(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() * rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() * rightNumber.intValue());
			}
			else {
				// 未知 Number子类型 -> 最佳猜测是double乘法
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String && rightOperand instanceof Integer) {
			int repeats = (Integer) rightOperand;
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < repeats; i++) {
				result.append(leftOperand);
			}
			return new TypedValue(result.toString());
		}

		return state.operate(Operation.MULTIPLY, leftOperand, rightOperand);
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
					mv.visitInsn(IMUL);
					break;
				case 'J':
					mv.visitInsn(LMUL);
					break;
				case 'F': 
					mv.visitInsn(FMUL);
					break;
				case 'D':
					mv.visitInsn(DMUL);
					break;				
				default:
					throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
