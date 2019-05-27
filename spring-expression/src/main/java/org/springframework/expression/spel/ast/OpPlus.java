package org.springframework.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * 加号运算符将:
 * <ul>
 * <li>加数字
 * <li>连接字符串
 * </ul>
 *
 * <p>它可以用作数字的一元运算符.
 * 当操作数类型不同时, 将执行标准提升 (double+int=double).
 * 对于其他选项, 它遵循已注册的overloader.
 */
public class OpPlus extends Operator {

	public OpPlus(int pos, SpelNodeImpl... operands) {
		super("+", pos, operands);
		Assert.notEmpty(operands, "Operands must not be empty");
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		if (rightOp == null) {  // 如果只有一个操作数, 那么这是一元加
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				if (operandOne instanceof Double) {
					this.exitTypeDescriptor = "D";
				}
				else if (operandOne instanceof Float) {
					this.exitTypeDescriptor = "F";
				}
				else if (operandOne instanceof Long) {
					this.exitTypeDescriptor = "J";
				}
				else if (operandOne instanceof Integer) {
					this.exitTypeDescriptor = "I";
				}
				return new TypedValue(operandOne);
			}
			return state.operate(Operation.ADD, operandOne, null);
		}

		TypedValue operandOneValue = leftOp.getValueInternal(state);
		Object leftOperand = operandOneValue.getValue();
		TypedValue operandTwoValue = rightOp.getValueInternal(state);
		Object rightOperand = operandTwoValue.getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.add(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.add(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
			}
			else {
				// 未知 Number 子类型 -> 最佳猜测是double添加
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String && rightOperand instanceof String) {
			this.exitTypeDescriptor = "Ljava/lang/String";
			return new TypedValue((String) leftOperand + rightOperand);
		}

		if (leftOperand instanceof String) {
			return new TypedValue(
					leftOperand + (rightOperand == null ? "null" : convertTypedValueToString(operandTwoValue, state)));
		}

		if (rightOperand instanceof String) {
			return new TypedValue(
					(leftOperand == null ? "null" : convertTypedValueToString(operandOneValue, state)) + rightOperand);
		}

		return state.operate(Operation.ADD, leftOperand, rightOperand);
	}

	@Override
	public String toStringAST() {
		if (this.children.length < 2) {  // unary plus
			return "+" + getLeftOperand().toStringAST();
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

	/**
	 * 使用已注册的转换器或使用{@code toString}方法将操作数值转换为字符串.
	 * 
	 * @param value 要转换的类型值
	 * @param state 表达式状态
	 * 
	 * @return 转换为{@code String}的{@code TypedValue}实例
	 */
	private static String convertTypedValueToString(TypedValue value, ExpressionState state) {
		TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(String.class);
		if (typeConverter.canConvert(value.getTypeDescriptor(), typeDescriptor)) {
			return String.valueOf(typeConverter.convertValue(value.getValue(),
					value.getTypeDescriptor(), typeDescriptor));
		}
		return String.valueOf(value.getValue());
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

	/**
	 * 遍历可能的节点树, 这些节点组合了字符串并将它们全部附加到相同的 (在堆栈上) StringBuilder上.
	 */
	private void walk(MethodVisitor mv, CodeFlow cf, SpelNodeImpl operand) {
		if (operand instanceof OpPlus) {
			OpPlus plus = (OpPlus)operand;
			walk(mv, cf, plus.getLeftOperand());
			walk(mv, cf, plus.getRightOperand());
		}
		else {
			cf.enterCompilationScope();
			operand.generateCode(mv,cf);
			if (!"Ljava/lang/String".equals(cf.lastDescriptor())) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/String");
			}
			cf.exitCompilationScope();
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		}
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if ("Ljava/lang/String".equals(this.exitTypeDescriptor)) {
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
			walk(mv,cf,getLeftOperand());
			walk(mv,cf,getRightOperand());
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		}
		else {
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
						mv.visitInsn(IADD);
						break;
					case 'J':
						mv.visitInsn(LADD);
						break;
					case 'F':
						mv.visitInsn(FADD);
						break;
					case 'D':
						mv.visitInsn(DADD);
						break;
					default:
						throw new IllegalStateException(
								"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
				}
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
