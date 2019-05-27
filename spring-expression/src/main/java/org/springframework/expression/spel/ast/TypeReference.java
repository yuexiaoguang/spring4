package org.springframework.expression.spel.ast;

import java.lang.reflect.Array;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;

/**
 * 表示对类型的引用, 例如 "T(String)" 或 "T(com.somewhere.Foo)"
 */
public class TypeReference extends SpelNodeImpl {

	private final int dimensions;

	private transient Class<?> type;


	public TypeReference(int pos, SpelNodeImpl qualifiedId) {
		this(pos, qualifiedId, 0);
	}

	public TypeReference(int pos, SpelNodeImpl qualifiedId, int dims) {
		super(pos, qualifiedId);
		this.dimensions = dims;
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		// TODO 如果缓存发现的类型引用, 可以在这里进行优化, 但我们可以这样做?
		String typeName = (String) this.children[0].getValueInternal(state).getValue();
		if (!typeName.contains(".") && Character.isLowerCase(typeName.charAt(0))) {
			TypeCode tc = TypeCode.valueOf(typeName.toUpperCase());
			if (tc != TypeCode.OBJECT) {
				// 基本类型
				Class<?> clazz = makeArrayIfNecessary(tc.getType());
				this.exitTypeDescriptor = "Ljava/lang/Class";
				this.type = clazz;
				return new TypedValue(clazz);
			}
		}
		Class<?> clazz = state.findType(typeName);
		clazz = makeArrayIfNecessary(clazz);
		this.exitTypeDescriptor = "Ljava/lang/Class";
		this.type = clazz;
		return new TypedValue(clazz);
	}

	private Class<?> makeArrayIfNecessary(Class<?> clazz) {
		if (this.dimensions != 0) {
			for (int i = 0; i < this.dimensions; i++) {
				Object array = Array.newInstance(clazz, 0);
				clazz = array.getClass();
			}
		}
		return clazz;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("T(");
		sb.append(getChild(0).toStringAST());
		for (int d = 0; d < this.dimensions; d++) {
			sb.append("[]");
		}
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public boolean isCompilable() {
		return (this.exitTypeDescriptor != null);
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		// TODO 未来优化 - 如果后面跟着静态方法调用, 跳过生成代码
		if (this.type.isPrimitive()) {
			if (this.type == Boolean.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Byte.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Character.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Double.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Float.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Integer.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Long.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
			}
			else if (this.type == Short.TYPE) {
				mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
			}
		}
		else {
			mv.visitLdcInsn(Type.getType(this.type));
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
