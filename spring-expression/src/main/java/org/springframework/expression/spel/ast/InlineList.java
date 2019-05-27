package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;

/**
 * 表示表达式中的列表, e.g. '{1,2,3}'
 */
public class InlineList extends SpelNodeImpl {

	// 如果列表纯粹是文字, 则它是一个常量值, 可以计算和缓存
	private TypedValue constant = null;  // TODO must be immutable list


	public InlineList(int pos, SpelNodeImpl... args) {
		super(pos, args);
		checkIfConstant();
	}


	/**
	 * 如果列表的所有组件都是常量, 或者列表本身包含常量, 那么可以构建一个常量列表来表示此节点.
	 * 这将加快以后的getValue调用, 并减少创建的垃圾量.
	 */
	private void checkIfConstant() {
		boolean isConstant = true;
		for (int c = 0, max = getChildCount(); c < max; c++) {
			SpelNode child = getChild(c);
			if (!(child instanceof Literal)) {
				if (child instanceof InlineList) {
					InlineList inlineList = (InlineList) child;
					if (!inlineList.isConstant()) {
						isConstant = false;
					}
				}
				else {
					isConstant = false;
				}
			}
		}
		if (isConstant) {
			List<Object> constantList = new ArrayList<Object>();
			int childcount = getChildCount();
			for (int c = 0; c < childcount; c++) {
				SpelNode child = getChild(c);
				if ((child instanceof Literal)) {
					constantList.add(((Literal) child).getLiteralValue().getValue());
				}
				else if (child instanceof InlineList) {
					constantList.add(((InlineList) child).getConstantValue());
				}
			}
			this.constant = new TypedValue(Collections.unmodifiableList(constantList));
		}
	}

	@Override
	public TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException {
		if (this.constant != null) {
			return this.constant;
		}
		else {
			List<Object> returnValue = new ArrayList<Object>();
			int childCount = getChildCount();
			for (int c = 0; c < childCount; c++) {
				returnValue.add(getChild(c).getValue(expressionState));
			}
			return new TypedValue(returnValue);
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("{");
		// 字符串ast匹配输入字符串, 而不是结果集合的'toString()', 它将使用 []
		int count = getChildCount();
		for (int c = 0; c < count; c++) {
			if (c > 0) {
				sb.append(",");
			}
			sb.append(getChild(c).toStringAST());
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 返回此列表是否为常量值.
	 */
	public boolean isConstant() {
		return (this.constant != null);
	}

	@SuppressWarnings("unchecked")
	public List<Object> getConstantValue() {
		return (List<Object>) this.constant.getValue();
	}
	
	@Override
	public boolean isCompilable() {
		return isConstant();
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		final String constantFieldName = "inlineList$" + codeflow.nextFieldId();
		final String className = codeflow.getClassName();

		codeflow.registerNewField(new CodeFlow.FieldAdder() {
			public void generateField(ClassWriter cw, CodeFlow codeflow) {
				cw.visitField(ACC_PRIVATE|ACC_STATIC|ACC_FINAL, constantFieldName, "Ljava/util/List;", null, null);
			}
		});
		
		codeflow.registerNewClinit(new CodeFlow.ClinitAdder() {
			public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
				generateClinitCode(className, constantFieldName, mv, codeflow, false);
			}
		});
		
		mv.visitFieldInsn(GETSTATIC, className, constantFieldName, "Ljava/util/List;");
		codeflow.pushDescriptor("Ljava/util/List");
	}
	
	void generateClinitCode(String clazzname, String constantFieldName, MethodVisitor mv, CodeFlow codeflow, boolean nested) {
		mv.visitTypeInsn(NEW, "java/util/ArrayList");
		mv.visitInsn(DUP);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
		if (!nested) {
			mv.visitFieldInsn(PUTSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
		}
		int childCount = getChildCount();
		for (int c = 0; c < childCount; c++) {
			if (!nested) {
				mv.visitFieldInsn(GETSTATIC, clazzname, constantFieldName, "Ljava/util/List;");
			}
			else {
				mv.visitInsn(DUP);
			}
			// 如果孩子不是常量, 孩子可能会进一步列出.
			// 在这种情况下, 不要回调到 generateCode(), 因为它会注册另一个临时加法器.
			// 相反, 直接在此处构建列表:
			if (children[c] instanceof InlineList) {
				((InlineList)children[c]).generateClinitCode(clazzname, constantFieldName, mv, codeflow, true);
			}
			else {
				children[c].generateCode(mv, codeflow);
				if (CodeFlow.isPrimitive(codeflow.lastDescriptor())) {
					CodeFlow.insertBoxIfNecessary(mv, codeflow.lastDescriptor().charAt(0));
				}
			}
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
			mv.visitInsn(POP);
		}
	}
}
