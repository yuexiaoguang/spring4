package org.springframework.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;
import org.springframework.util.ObjectUtils;

/**
 * 对一个或两个操作数进行操作的运算符的公共超类型.
 * 在乘法或除法的情况下, 将有两个操作数, 但对于一元加或减, 只有一个.
 */
public abstract class Operator extends SpelNodeImpl {

	private final String operatorName;
	
	// 如果发现的声明描述符未提供足够的信息, 则使用运行时操作数值的描述符
	// (例如一个泛型类型, 其访问器似乎只返回 'Object' - 实际的描述符可能表示 'int')

	protected String leftActualDescriptor;

	protected String rightActualDescriptor;


	public Operator(String payload, int pos, SpelNodeImpl... operands) {
		super(pos, operands);
		this.operatorName = payload;
	}


	public SpelNodeImpl getLeftOperand() {
		return this.children[0];
	}

	public SpelNodeImpl getRightOperand() {
		return this.children[1];
	}

	public final String getOperatorName() {
		return this.operatorName;
	}

	/**
	 * 所有运算符的字符串格式都是相同的 '(' [operand] [operator] [operand] ')'
	 */
	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("(");
		sb.append(getChild(0).toStringAST());
		for (int i = 1; i < getChildCount(); i++) {
			sb.append(" ").append(getOperatorName()).append(" ");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}


	protected boolean isCompilableOperatorUsingNumerics() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right = getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}

		// equals支持的操作数类型 (目前)
		String leftDesc = left.exitTypeDescriptor;
		String rightDesc = right.exitTypeDescriptor;
		DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(
				leftDesc, rightDesc, this.leftActualDescriptor, this.rightActualDescriptor);
		return (dc.areNumbers && dc.areCompatible);
	}

	/** 
	 * 数字比较运算符共享非常相似的生成代码, 只有两个比较指令不同.
	 */
	protected void generateComparisonCode(MethodVisitor mv, CodeFlow cf, int compInstruction1, int compInstruction2) {
		String leftDesc = getLeftOperand().exitTypeDescriptor;
		String rightDesc = getRightOperand().exitTypeDescriptor;
		
		boolean unboxLeft = !CodeFlow.isPrimitive(leftDesc);
		boolean unboxRight = !CodeFlow.isPrimitive(rightDesc);
		DescriptorComparison dc = DescriptorComparison.checkNumericCompatibility(
				leftDesc, rightDesc, this.leftActualDescriptor, this.rightActualDescriptor);
		char targetType = dc.compatibleType;  // CodeFlow.toPrimitiveTargetDesc(leftDesc);
		
		cf.enterCompilationScope();
		getLeftOperand().generateCode(mv, cf);
		cf.exitCompilationScope();
		if (unboxLeft) {
			CodeFlow.insertUnboxInsns(mv, targetType, leftDesc);
		}
	
		cf.enterCompilationScope();
		getRightOperand().generateCode(mv, cf);
		cf.exitCompilationScope();
		if (unboxRight) {
			CodeFlow.insertUnboxInsns(mv, targetType, rightDesc);
		}

		// assert: SpelCompiler.boxingCompatible(leftDesc, rightDesc)
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		if (targetType == 'D') {
			mv.visitInsn(DCMPG);
			mv.visitJumpInsn(compInstruction1, elseTarget);
		}
		else if (targetType == 'F') {
			mv.visitInsn(FCMPG);		
			mv.visitJumpInsn(compInstruction1, elseTarget);
		}
		else if (targetType == 'J') {
			mv.visitInsn(LCMP);		
			mv.visitJumpInsn(compInstruction1, elseTarget);
		}
		else if (targetType == 'I') {
			mv.visitJumpInsn(compInstruction2, elseTarget);
		}
		else {
			throw new IllegalStateException("Unexpected descriptor " + leftDesc);
		}

		// 其他数字尚不支持 (isCompilable 将不会返回 true)
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		cf.pushDescriptor("Z");
	}


	/**
	 * 对给定的操作数值执行相等性检查.
	 * <p>此方法不仅用于子类中的反射比较, 还用于编译的表达式代码, 这就是为什么需要将其声明为{@code public static}.
	 * 
	 * @param context 当前评估上下文
	 * @param left 左侧操作数值
	 * @param right 左侧操作数值
	 */
	public static boolean equalityCheck(EvaluationContext context, Object left, Object right) {
		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return (leftBigDecimal == null ? rightBigDecimal == null : leftBigDecimal.compareTo(rightBigDecimal) == 0);
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return (leftNumber.doubleValue() == rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return (leftNumber.floatValue() == rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return (leftBigInteger == null ? rightBigInteger == null : leftBigInteger.compareTo(rightBigInteger) == 0);
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return (leftNumber.longValue() == rightNumber.longValue());
			}
			else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
				return (leftNumber.intValue() == rightNumber.intValue());
			}
			else if (leftNumber instanceof Short || rightNumber instanceof Short) {
				return (leftNumber.shortValue() == rightNumber.shortValue());
			}
			else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
				return (leftNumber.byteValue() == rightNumber.byteValue());
			}
			else {
				// 未知 Number子类型 -> 最佳猜测是双重比较
				return (leftNumber.doubleValue() == rightNumber.doubleValue());
			}
		}

		if (left instanceof CharSequence && right instanceof CharSequence) {
			return left.toString().equals(right.toString());
		}

		if (ObjectUtils.nullSafeEquals(left, right)) {
			return true;
		}

		if (left instanceof Comparable && right instanceof Comparable) {
			Class<?> ancestor = ClassUtils.determineCommonAncestor(left.getClass(), right.getClass());
			if (ancestor != null && Comparable.class.isAssignableFrom(ancestor)) {
				return (context.getTypeComparator().compare(left, right) == 0);
			}
		}

		return false;
	}
	

	/**
	 * 描述符比较封装了比较两个操作数的描述符的结果, 并描述了它们兼容的级别.
	 */
	protected static class DescriptorComparison {

		static final DescriptorComparison NOT_NUMBERS = new DescriptorComparison(false, false, ' ');

		static final DescriptorComparison INCOMPATIBLE_NUMBERS = new DescriptorComparison(true, false, ' ');

		final boolean areNumbers;  // 两个比较描述符都是数字?

		final boolean areCompatible;  // 如果他们是数字, 他们是否兼容?

		final char compatibleType;  // 兼容时, 常见类型的描述符是什么

		private DescriptorComparison(boolean areNumbers, boolean areCompatible, char compatibleType) {
			this.areNumbers = areNumbers;
			this.areCompatible = areCompatible;
			this.compatibleType = compatibleType;
		}
		
		/**
		 * 返回一个对象, 指示输入描述符是否兼容.
		 * <p>声明的描述符是可以静态确定的 (e.g. 从查看属性访问器方法的返回值),
		 * 而实际描述符是返回的实际对象的类型, 可能不同.
		 * <p>对于具有未绑定类型变量的泛型类型, 发现的声明描述符可能是'Object',
		 * 但是从实际描述符中可以观察到对象实际上是数值 (e.g. ints).
		 * 
		 * @param leftDeclaredDescriptor 静态可确定的左描述符
		 * @param rightDeclaredDescriptor 静态可确定的右描述符
		 * @param leftActualDescriptor 动态/运行时左对象描述符
		 * @param rightActualDescriptor 动态/运行时右对象描述符
		 * 
		 * @return a DescriptorComparison object indicating the type of compatibility, if any
		 */
		public static DescriptorComparison checkNumericCompatibility(String leftDeclaredDescriptor,
				String rightDeclaredDescriptor, String leftActualDescriptor, String rightActualDescriptor) {

			String ld = leftDeclaredDescriptor;
			String rd = rightDeclaredDescriptor;

			boolean leftNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(ld);
			boolean rightNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rd);
			
			// 如果声明的描述符未提供信息, 请尝试实际描述符
			if (!leftNumeric && !ObjectUtils.nullSafeEquals(ld, leftActualDescriptor)) {
				ld = leftActualDescriptor;
				leftNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(ld);
			}
			if (!rightNumeric && !ObjectUtils.nullSafeEquals(rd, rightActualDescriptor)) {
				rd = rightActualDescriptor;
				rightNumeric = CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rd);
			}
			
			if (leftNumeric && rightNumeric) {
				if (CodeFlow.areBoxingCompatible(ld, rd)) {
					return new DescriptorComparison(true, true, CodeFlow.toPrimitiveTargetDesc(ld));
				}
				else {
					return DescriptorComparison.INCOMPATIBLE_NUMBERS;
				}
			}
			else {
				return DescriptorComparison.NOT_NUMBERS;
			}		
		}
	}
}
