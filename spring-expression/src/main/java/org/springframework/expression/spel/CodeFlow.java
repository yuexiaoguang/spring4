package org.springframework.expression.spel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.util.Assert;

/**
 * 管理编译过程生成的类.
 *
 * <p>在生成字节码时记录中间编译状态.
 * 还包括各种字节码生成辅助函数.
 */
public class CodeFlow implements Opcodes {

	/**
	 * 要生成的类的名称.
	 * 通常在生成要访问生成的类型上新生成的字段的代码时使用.
	 */
	private final String className;

	/**
	 * 正在生成的当前类.
	 */
	private final ClassWriter classWriter;

	/**
	 * 记录字节码堆栈顶部的类型 (i.e. 前一个表达式组件的输出类型).
	 * 新范围用于评估子表达式, 例如方法调用表达式中参数值的表达式.
	 */
	private final Stack<ArrayList<String>> compilationScopes;

	/**
	 * 当调用SpEL ast节点为主要评估方法生成代码时, 他们可以注册以向该类添加字段.
	 * 在生成完主评估函数后, 将调用已注册的FieldAdders.
	 */
	private List<FieldAdder> fieldAdders;

	/**
	 * 当调用SpEL ast节点为主要评估方法生成代码时, 他们可以注册以将代码添加到类中的静态初始化程序.
	 * 在主评估函数生成完成后, 将调用任何已注册的ClinitAdders.
	 */
	private List<ClinitAdder> clinitAdders;

	/**
	 * 当代码生成需要在类级别字段中保存值时, 这用于跟踪下一个可用字段id (用作名称后缀).
	 */
	private int nextFieldId = 1;

	/**
	 * 当代码生成需要方法中的中间变量时, 此方法记录下一个可用变量 (变量0是 'this').
	 */
	private int nextFreeVariableId = 1;


	/**
	 * @param className 类名
	 * @param classWriter 对应的ASM {@code ClassWriter}
	 */
	public CodeFlow(String className, ClassWriter classWriter) {
		this.className = className;
		this.classWriter = classWriter;
		this.compilationScopes = new Stack<ArrayList<String>>();
		this.compilationScopes.add(new ArrayList<String>());
	}


	/**
	 * 按字节代码加载目标 (i.e. 作为 CompiledExpression.getValue(target, context)的第一个参数传递的内容)
	 * 
	 * @param mv 应该插入加载指令的访问器
	 */
	public void loadTarget(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD, 1);
	}

	/**
	 * 按字节码加载EvaluationContext (传递给编译表达式方法的第二个参数).
	 * 
	 * @param mv 应该插入加载指令的访问器
	 */
	public void loadEvaluationContext(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD, 2);
	}

	/**
	 * 记录最近评估的表达式元素的描述符.
	 * 
	 * @param descriptor 最近评估的元素的类型描述符
	 */
	public void pushDescriptor(String descriptor) {
		Assert.notNull(descriptor, "Descriptor must not be null");
		this.compilationScopes.peek().add(descriptor);
	}

	/**
	 * 输入新的编译范围, 通常是由于嵌套表达式评估.
	 * 例如, 在计算方法调用表达式的参数时, 将在新范围中计算每个参数.
	 */
	public void enterCompilationScope() {
		this.compilationScopes.push(new ArrayList<String>());
	}

	/**
	 * 退出编译范围, 通常在评估嵌套表达式之后.
	 * 例如, 在评估了方法调用的参数之后, 此方法将我们返回到先前 (外部) 范围.
	 */
	public void exitCompilationScope() {
		this.compilationScopes.pop();
	}

	/**
	 * 返回当前位于堆栈顶部的条目的描述符 (在当前范围内).
	 */
	public String lastDescriptor() {
		ArrayList<String> scopes = this.compilationScopes.peek();
		return (!scopes.isEmpty() ? scopes.get(scopes.size() - 1) : null);
	}

	/**
	 * 如果代码流显示最后一个表达式评估到 java.lang.Boolean, 后插入必要的指令到unbox那个布尔原语.
	 * 
	 * @param mv 应该插入新指令的访问器
	 */
	public void unboxBooleanIfNecessary(MethodVisitor mv) {
		if ("Ljava/lang/Boolean".equals(lastDescriptor())) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
		}
	}

	/**
	 * 在生成主表达式求值方法之后调用, 此方法将回调已注册的FieldAdders或ClinitAdders, 以向表示已编译的表达式的类添加额外信息.
	 */
	public void finish() {
		if (this.fieldAdders != null) {
			for (FieldAdder fieldAdder : this.fieldAdders) {
				fieldAdder.generateField(this.classWriter, this);
			}
		}
		if (this.clinitAdders != null) {
			MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
			mv.visitCode();
			this.nextFreeVariableId = 0;  // to 0 because there is no 'this' in a clinit
			for (ClinitAdder clinitAdder : this.clinitAdders) {
				clinitAdder.generateCode(mv, this);
			}
			mv.visitInsn(RETURN);
			mv.visitMaxs(0,0);  // not supplied due to COMPUTE_MAXS
			mv.visitEnd();
		}
	}

	/**
	 * 注册一个FieldAdder, 它将向生成的类添加一个新字段, 以支持由ast节点主 generateCode()方法生成的代码.
	 */
	public void registerNewField(FieldAdder fieldAdder) {
		if (this.fieldAdders == null) {
			this.fieldAdders = new ArrayList<FieldAdder>();
		}
		this.fieldAdders.add(fieldAdder);
	}

	/**
	 * 注册一个ClinitAdder, 它将代码添加到生成的类中的静态初始化程序, 以支持由ast节点主 generateCode() 方法生成的代码.
	 */
	public void registerNewClinit(ClinitAdder clinitAdder) {
		if (this.clinitAdders == null) {
			this.clinitAdders = new ArrayList<ClinitAdder>();
		}
		this.clinitAdders.add(clinitAdder);
	}

	public int nextFieldId() {
		return this.nextFieldId++;
	}

	public int nextFreeVariableId() {
		return this.nextFreeVariableId++;
	}

	public String getClassName() {
		return this.className;
	}

	@Deprecated
	public String getClassname() {
		return this.className;
	}


	/**
	 * 插入任何必要的强制类型和值调用, 以从封装类型转换为原始类型值.
	 * 
	 * @param mv 应该插入指令的方法访问器
	 * @param ch 作为输出的原始类型
	 * @param stackDescriptor 堆栈顶部类型的描述符
	 */
	public static void insertUnboxInsns(MethodVisitor mv, char ch, String stackDescriptor) {
		switch (ch) {
			case 'Z':
				if (!stackDescriptor.equals("Ljava/lang/Boolean")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
				break;
			case 'B':
				if (!stackDescriptor.equals("Ljava/lang/Byte")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
				break;
			case 'C':
				if (!stackDescriptor.equals("Ljava/lang/Character")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
				break;
			case 'D':
				if (!stackDescriptor.equals("Ljava/lang/Double")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
				break;
			case 'F':
				if (!stackDescriptor.equals("Ljava/lang/Float")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
				break;
			case 'I':
				if (!stackDescriptor.equals("Ljava/lang/Integer")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
				break;
			case 'J':
				if (!stackDescriptor.equals("Ljava/lang/Long")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
				break;
			case 'S':
				if (!stackDescriptor.equals("Ljava/lang/Short")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
				break;
			default:
				throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	/**
	 * 对于数字, 请在数字上使用适当的方法将其转换为请求的基本类型.
	 * 
	 * @param mv 应该插入指令的方法访问器
	 * @param targetDescriptor 作为输出的原始类型
	 * @param stackDescriptor 堆栈顶部的类型的描述符
	 */
	public static void insertUnboxNumberInsns(MethodVisitor mv, char targetDescriptor, String stackDescriptor) {
		switch (targetDescriptor) {
			case 'D':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
				break;
			case 'F':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
				break;
			case 'J':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
				break;
			case 'I':
				if (stackDescriptor.equals("Ljava/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
				break;
			// does not handle Z, B, C, S
			default:
				throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + targetDescriptor + "'");
		}
	}

	/**
	 * 根据堆栈上的内容和所需的目标类型插入任何必要的数字转换字节码.
	 * 
	 * @param mv 应该放置指令的方法访问器
	 * @param targetDescriptor 目标类型的(原始类型)描述符
	 * @param stackDescriptor 堆栈顶部的操作数的描述符
	 */
	public static void insertAnyNecessaryTypeConversionBytecodes(MethodVisitor mv, char targetDescriptor, String stackDescriptor) {
		if (CodeFlow.isPrimitive(stackDescriptor)) {
			char stackTop = stackDescriptor.charAt(0);
			if (stackTop == 'I' || stackTop == 'B' || stackTop == 'S' || stackTop == 'C') {
				if (targetDescriptor == 'D') {
					mv.visitInsn(I2D);
				}
				else if (targetDescriptor == 'F') {
					mv.visitInsn(I2F);
				}
				else if (targetDescriptor == 'J') {
					mv.visitInsn(I2L);
				}
				else if (targetDescriptor == 'I') {
					// nop
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
				}
			}
			else if (stackTop == 'J') {
				if (targetDescriptor == 'D') {
					mv.visitInsn(L2D);
				}
				else if (targetDescriptor == 'F') {
					mv.visitInsn(L2F);
				}
				else if (targetDescriptor == 'J') {
					// nop
				}
				else if (targetDescriptor == 'I') {
					mv.visitInsn(L2I);
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
				}
			}
			else if (stackTop == 'F') {
				if (targetDescriptor == 'D') {
					mv.visitInsn(F2D);
				}
				else if (targetDescriptor == 'F') {
					// nop
				}
				else if (targetDescriptor == 'J') {
					mv.visitInsn(F2L);
				}
				else if (targetDescriptor == 'I') {
					mv.visitInsn(F2I);
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
				}
			}
			else if (stackTop == 'D') {
				if (targetDescriptor == 'D') {
					// nop
				}
				else if (targetDescriptor == 'F') {
					mv.visitInsn(D2F);
				}
				else if (targetDescriptor == 'J') {
					mv.visitInsn(D2L);
				}
				else if (targetDescriptor == 'I') {
					mv.visitInsn(D2I);
				}
				else {
					throw new IllegalStateException("Cannot get from " + stackDescriptor + " to " + targetDescriptor);
				}
			}
		}
	}


	/**
	 * 为方法创建JVM签名描述符.
	 * 这包括用括号括起来的方法参数的描述符, 后跟返回类型的描述符.
	 * 注意这里的描述符是JVM描述符, 不像编译器使用的其他描述符形式, 不包括尾随分号.
	 * 
	 * @param method 方法
	 * 
	 * @return 描述符 (e.g. "(ILjava/lang/String;)V")
	 */
	public static String createSignatureDescriptor(Method method) {
		Class<?>[] params = method.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Class<?> param : params) {
			sb.append(toJvmDescriptor(param));
		}
		sb.append(")");
		sb.append(toJvmDescriptor(method.getReturnType()));
		return sb.toString();
	}

	/**
	 * 为构造函数创建JVM签名描述符.
	 * 这包括用括号括起的构造函数参数的描述符, 后跟返回类型的描述符, 它总是 "V".
	 * 注意这里的描述符是JVM描述符, 不像编译器使用的其他描述符形式, 不包括尾随分号.
	 * 
	 * @param ctor 构造函数
	 * 
	 * @return 描述符 (e.g. "(ILjava/lang/String;)V")
	 */
	public static String createSignatureDescriptor(Constructor<?> ctor) {
		Class<?>[] params = ctor.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Class<?> param : params) {
			sb.append(toJvmDescriptor(param));
		}
		sb.append(")V");
		return sb.toString();
	}

	/**
	 * 确定指定类的JVM描述符.
	 * 与编译过程中使用的其他描述符不同, 这是JVM想要的描述符, 因此这包括任何必要的尾随分号 (e.g. Ljava/lang/String; 而不是 Ljava/lang/String)
	 * 
	 * @param clazz 类
	 * 
	 * @return 该类的JVM描述符
	 */
	public static String toJvmDescriptor(Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		if (clazz.isArray()) {
			while (clazz.isArray()) {
				sb.append("[");
				clazz = clazz.getComponentType();
			}
		}
		if (clazz.isPrimitive()) {
			if (clazz == Boolean.TYPE) {
				sb.append('Z');
			}
			else if (clazz == Byte.TYPE) {
				sb.append('B');
			}
			else if (clazz == Character.TYPE) {
				sb.append('C');
			}
			else if (clazz == Double.TYPE) {
				sb.append('D');
			}
			else if (clazz == Float.TYPE) {
				sb.append('F');
			}
			else if (clazz == Integer.TYPE) {
				sb.append('I');
			}
			else if (clazz == Long.TYPE) {
				sb.append('J');
			}
			else if (clazz == Short.TYPE) {
				sb.append('S');
			}
			else if (clazz == Void.TYPE) {
				sb.append('V');
			}
		}
		else {
			sb.append("L");
			sb.append(clazz.getName().replace('.', '/'));
			sb.append(";");
		}
		return sb.toString();
	}

	/**
	 * 确定对象实例的描述符 (or {@code null}).
	 * 
	 * @param value 对象 (possibly {@code null})
	 * 
	 * @return 对象的类型描述符 (描述符是{@code null}值的 "Ljava/lang/Object")
	 */
	public static String toDescriptorFromObject(Object value) {
		if (value == null) {
			return "Ljava/lang/Object";
		}
		else {
			return toDescriptor(value.getClass());
		}
	}

	/**
	 * 确定描述符是用于boolean基础类型还是boolean引用类型.
	 * 
	 * @param descriptor 类型描述符
	 * 
	 * @return {@code true} 如果描述符是boolean兼容的
	 */
	public static boolean isBooleanCompatible(String descriptor) {
		return (descriptor != null && (descriptor.equals("Z") || descriptor.equals("Ljava/lang/Boolean")));
	}

	/**
	 * 确定描述符是否用于基本类型.
	 * 
	 * @param descriptor 类型描述符
	 * 
	 * @return {@code true} 如果是用于基本类型
	 */
	public static boolean isPrimitive(String descriptor) {
		return (descriptor != null && descriptor.length() == 1);
	}

	/**
	 * 确定描述符是否用于基本类型数组 (e.g. "[[I").
	 * 
	 * @param descriptor 描述符
	 * 
	 * @return {@code true} 如果是用于基本类型数组
	 */
	public static boolean isPrimitiveArray(String descriptor) {
		boolean primitive = true;
		for (int i = 0, max = descriptor.length(); i < max; i++) {
			char ch = descriptor.charAt(i);
			if (ch == '[') {
				continue;
			}
			primitive = (ch != 'L');
			break;
		}
		return primitive;
	}

	/**
	 * 确定装箱/拆箱是否可以从一种类型到另一种类型.
	 * 假设至少有一种类型是封装形式 (i.e. 单个字符描述符).
	 * 
	 * @return {@code true} 如果有可能从一个描述符到另一个描述符 (通过装箱)
	 */
	public static boolean areBoxingCompatible(String desc1, String desc2) {
		if (desc1.equals(desc2)) {
			return true;
		}
		if (desc1.length() == 1) {
			if (desc1.equals("Z")) {
				return desc2.equals("Ljava/lang/Boolean");
			}
			else if (desc1.equals("D")) {
				return desc2.equals("Ljava/lang/Double");
			}
			else if (desc1.equals("F")) {
				return desc2.equals("Ljava/lang/Float");
			}
			else if (desc1.equals("I")) {
				return desc2.equals("Ljava/lang/Integer");
			}
			else if (desc1.equals("J")) {
				return desc2.equals("Ljava/lang/Long");
			}
		}
		else if (desc2.length() == 1) {
			if (desc2.equals("Z")) {
				return desc1.equals("Ljava/lang/Boolean");
			}
			else if (desc2.equals("D")) {
				return desc1.equals("Ljava/lang/Double");
			}
			else if (desc2.equals("F")) {
				return desc1.equals("Ljava/lang/Float");
			}
			else if (desc2.equals("I")) {
				return desc1.equals("Ljava/lang/Integer");
			}
			else if (desc2.equals("J")) {
				return desc1.equals("Ljava/lang/Long");
			}
		}
		return false;
	}

	/**
	 * 确定提供的描述符是否支持数字类型或boolean.
	 * 编译过程 (当前) 仅支持某些数字类型.
	 * 有 double, float, long 和 int.
	 * 
	 * @param descriptor 类型的描述符
	 * 
	 * @return {@code true} 如果描述符支持数字类型或boolean
	 */
	public static boolean isPrimitiveOrUnboxableSupportedNumberOrBoolean(String descriptor) {
		if (descriptor == null) {
			return false;
		}
		if (isPrimitiveOrUnboxableSupportedNumber(descriptor)) {
			return true;
		}
		return ("Z".equals(descriptor) || descriptor.equals("Ljava/lang/Boolean"));
	}

	/**
	 * 确定提供的描述符是否是支持的数字类型.
	 * 编译过程 (当前) 仅支持某些数字类型.
	 * 有 double, float, long 和 int.
	 * 
	 * @param descriptor 类型的描述符
	 * 
	 * @return {@code true} 如果描述符是支持的数字类型
	 */
	public static boolean isPrimitiveOrUnboxableSupportedNumber(String descriptor) {
		if (descriptor == null) {
			return false;
		}
		if (descriptor.length() == 1) {
			return "DFIJ".contains(descriptor);
		}
		if (descriptor.startsWith("Ljava/lang/")) {
			String name = descriptor.substring("Ljava/lang/".length());
			if (name.equals("Double") || name.equals("Float") || name.equals("Integer") || name.equals("Long")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定数字是否被视为整数, 以便在字节码级别进行数值运算.
	 * 
	 * @param number 要检查的数字
	 * 
	 * @return {@code true}如果是一个{@link Integer}, {@link Short} 或 {@link Byte}
	 */
	public static boolean isIntegerForNumericOp(Number number) {
		return (number instanceof Integer || number instanceof Short || number instanceof Byte);
	}

	/**
	 * 将类型描述符转换为单个字符基础类型描述符.
	 * 
	 * @param descriptor 类型描述符
	 * 
	 * @return 基础类型输入描述符的单个字符描述符
	 */
	public static char toPrimitiveTargetDesc(String descriptor) {
		if (descriptor.length() == 1) {
			return descriptor.charAt(0);
		}
		else if (descriptor.equals("Ljava/lang/Boolean")) {
			return 'Z';
		}
		else if (descriptor.equals("Ljava/lang/Byte")) {
			return 'B';
		}
		else if (descriptor.equals("Ljava/lang/Character")) {
			return 'C';
		}
		else if (descriptor.equals("Ljava/lang/Double")) {
			return 'D';
		}
		else if (descriptor.equals("Ljava/lang/Float")) {
			return 'F';
		}
		else if (descriptor.equals("Ljava/lang/Integer")) {
			return 'I';
		}
		else if (descriptor.equals("Ljava/lang/Long")) {
			return 'J';
		}
		else if (descriptor.equals("Ljava/lang/Short")) {
			return 'S';
		}
		else {
			throw new IllegalStateException("No primitive for '" + descriptor + "'");
		}
	}

	/**
	 * 为提供的描述符插入适当的CHECKCAST指令.
	 * 
	 * @param mv 应该插入指令的目标访问器
	 * @param descriptor 要转换为的类型描述符
	 */
	public static void insertCheckCast(MethodVisitor mv, String descriptor) {
		if (descriptor.length() != 1) {
			if (descriptor.charAt(0) == '[') {
				if (isPrimitiveArray(descriptor)) {
					mv.visitTypeInsn(CHECKCAST, descriptor);
				}
				else {
					mv.visitTypeInsn(CHECKCAST, descriptor + ";");
				}
			}
			else {
				if (!descriptor.equals("Ljava/lang/Object")) {
					// 砍掉 'L' 给我们留下 "java/lang/String"
					mv.visitTypeInsn(CHECKCAST, descriptor.substring(1));
				}
			}
		}
	}

	/**
	 * 确定特定类型的适当装箱指令 (如果需要装箱), 并将指令插入到提供的访问器中.
	 * 
	 * @param mv 新指令的目标访问器
	 * @param descriptor 可能需要装箱也可能不需要装箱的类型描述符
	 */
	public static void insertBoxIfNecessary(MethodVisitor mv, String descriptor) {
		if (descriptor.length() == 1) {
			insertBoxIfNecessary(mv, descriptor.charAt(0));
		}
	}

	/**
	 * 确定特定类型的适当装箱指令 (如果需要装箱), 并将指令插入到提供的访问器中.
	 * 
	 * @param mv 新指令的目标访问器
	 * @param ch 可能需要装箱的类型描述符
	 */
	public static void insertBoxIfNecessary(MethodVisitor mv, char ch) {
		switch (ch) {
			case 'Z':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
				break;
			case 'B':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				break;
			case 'C':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
				break;
			case 'D':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				break;
			case 'F':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				break;
			case 'I':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
				break;
			case 'J':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				break;
			case 'S':
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				break;
			case 'L':
			case 'V':
			case '[':
				// no box needed
				break;
			default:
				throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	/**
	 * 推断类型的描述符.
	 * 描述符类似于JVM类型名称, 但缺少尾随';', 所以对于Object, 描述符是"Ljava/lang/Object", 对于int它是 "I".
	 * 
	 * @param type 要为其确定描述符的类型 (可能是原始类型)
	 * 
	 * @return 描述符
	 */
	public static String toDescriptor(Class<?> type) {
		String name = type.getName();
		if (type.isPrimitive()) {
			switch (name.length()) {
				case 3:
					return "I";
				case 4:
					if (name.equals("byte")) {
						return "B";
					}
					else if (name.equals("char")) {
						return "C";
					}
					else if (name.equals("long")) {
						return "J";
					}
					else if (name.equals("void")) {
						return "V";
					}
					break;
				case 5:
					if (name.equals("float")) {
						return "F";
					}
					else if (name.equals("short")) {
						return "S";
					}
					break;
				case 6:
					if (name.equals("double")) {
						return "D";
					}
					break;
				case 7:
					if (name.equals("boolean")) {
						return "Z";
					}
					break;
			}
		}
		else {
			if (name.charAt(0) != '[') {
				return "L" + type.getName().replace('.', '/');
			}
			else {
				if (name.endsWith(";")) {
					return name.substring(0, name.length() - 1).replace('.', '/');
				}
				else {
					return name;  // array has primitive component type
				}
			}
		}
		return null;
	}

	/**
	 * 创建一个描述符数组, 表示所提供方法的参数类型.
	 * 如果没有参数, 则返回零大小的数组.
	 * 
	 * @param method a Method
	 * 
	 * @return 描述符的String数组, 每个方法参数一个条目
	 */
	public static String[] toParamDescriptors(Method method) {
		return toDescriptors(method.getParameterTypes());
	}

	/**
	 * 创建一个描述符数组, 表示所提供构造函数的参数类型.
	 * 如果没有参数, 则返回零大小的数组.
	 * 
	 * @param ctor a Constructor
	 * 
	 * @return 描述符的String数组, 每个构造函数参数一个条目
	 */
	public static String[] toParamDescriptors(Constructor<?> ctor) {
		return toDescriptors(ctor.getParameterTypes());
	}

	/**
	 * 从类数组中创建描述符数组.
	 * 
	 * @param types 类的输入数组
	 * 
	 * @return 描述符数组
	 */
	public static String[] toDescriptors(Class<?>[] types) {
		int typesCount = types.length;
		String[] descriptors = new String[typesCount];
		for (int p = 0; p < typesCount; p++) {
			descriptors[p] = toDescriptor(types[p]);
		}
		return descriptors;
	}

	/**
	 * 创建在堆栈上加载数字的最佳指令.
	 * 
	 * @param mv 插入字节码的位置
	 * @param value 要加载的值
	 */
	public static void insertOptimalLoad(MethodVisitor mv, int value) {
		if (value < 6) {
			mv.visitInsn(ICONST_0+value);
		}
		else if (value < Byte.MAX_VALUE) {
			mv.visitIntInsn(BIPUSH, value);
		}
		else if (value < Short.MAX_VALUE) {
			mv.visitIntInsn(SIPUSH, value);
		}
		else {
			mv.visitLdcInsn(value);
		}
	}

	/**
	 * 生成适当的字节码以将堆栈项存储在数组中.
	 * 要使用的指令取决于类型是基本类型还是引用类型.
	 * 
	 * @param mv 插入字节码的位置
	 * @param arrayElementType 数组元素的类型
	 */
	public static void insertArrayStore(MethodVisitor mv, String arrayElementType) {
		if (arrayElementType.length()==1) {
			switch (arrayElementType.charAt(0)) {
				case 'I':
					mv.visitInsn(IASTORE);
					break;
				case 'J':
					mv.visitInsn(LASTORE);
					break;
				case 'F':
					mv.visitInsn(FASTORE);
					break;
				case 'D':
					mv.visitInsn(DASTORE);
					break;
				case 'B':
					mv.visitInsn(BASTORE);
					break;
				case 'C':
					mv.visitInsn(CASTORE);
					break;
				case 'S':
					mv.visitInsn(SASTORE);
					break;
				case 'Z':
					mv.visitInsn(BASTORE);
					break;
				default:
					throw new IllegalArgumentException(
							"Unexpected arraytype " + arrayElementType.charAt(0));
			}
		}
		else {
			mv.visitInsn(AASTORE);
		}
	}

	/**
	 * 确定用于NEWARRAY字节码的相应T标记.
	 * 
	 * @param arraytype 数组基本类型
	 * 
	 * @return 用于NEWARRAY的T标签
	 */
	public static int arrayCodeFor(String arraytype) {
		switch (arraytype.charAt(0)) {
			case 'I': return T_INT;
			case 'J': return T_LONG;
			case 'F': return T_FLOAT;
			case 'D': return T_DOUBLE;
			case 'B': return T_BYTE;
			case 'C': return T_CHAR;
			case 'S': return T_SHORT;
			case 'Z': return T_BOOLEAN;
			default:
				throw new IllegalArgumentException("Unexpected arraytype " + arraytype.charAt(0));
		}
	}

	/**
	 * 返回提供的数组类型是否具有核心组件引用类型.
	 */
	public static boolean isReferenceTypeArray(String arraytype) {
		int length = arraytype.length();
		for (int i = 0; i < length; i++) {
			char ch = arraytype.charAt(i);
			if (ch == '[') {
				continue;
			}
			return (ch == 'L');
		}
		return false;
	}

	/**
	 * 生成正确的字节码以构建数组.
	 * 要使用的操作码和与操作码一起传递的签名, 可以根据数组类型的签名而变化.
	 * 
	 * @param mv 应该插入代码的methodvisitor
	 * @param size 数组的大小
	 * @param arraytype 数组的类型
	 */
	public static void insertNewArrayCode(MethodVisitor mv, int size, String arraytype) {
		insertOptimalLoad(mv, size);
		if (arraytype.length() == 1) {
			mv.visitIntInsn(NEWARRAY, CodeFlow.arrayCodeFor(arraytype));
		}
		else {
			if (arraytype.charAt(0) == '[') {
				// 处理嵌套数组.
				// If vararg is [[I then we want [I and not [I;
				if (CodeFlow.isReferenceTypeArray(arraytype)) {
					mv.visitTypeInsn(ANEWARRAY, arraytype + ";");
				}
				else {
					mv.visitTypeInsn(ANEWARRAY, arraytype);
				}
			}
			else {
				mv.visitTypeInsn(ANEWARRAY, arraytype.substring(1));
			}
		}
	}

	/**
	 * 对于在数学运算符中使用, 处理从堆栈上的 (可能是封装的) 数字转换为原始数字类型.
	 * <p>例如, 从Integer到double, 只需要调用'Number.doubleValue()', 但是从int到double, 需要使用字节码 'i2d'.
	 * 
	 * @param mv 应附加指令的方法访问器
	 * @param stackDescriptor 堆栈上操作数的描述符
	 * @param targetDescriptor 原始类型描述符
	 */
	public static void insertNumericUnboxOrPrimitiveTypeCoercion(
			MethodVisitor mv, String stackDescriptor, char targetDescriptor) {

		if (!CodeFlow.isPrimitive(stackDescriptor)) {
			CodeFlow.insertUnboxNumberInsns(mv, targetDescriptor, stackDescriptor);
		}
		else {
			CodeFlow.insertAnyNecessaryTypeConversionBytecodes(mv, targetDescriptor, stackDescriptor);
		}
	}

	public static String toBoxedDescriptor(String primitiveDescriptor) {
		switch (primitiveDescriptor.charAt(0)) {
			case 'I': return "Ljava/lang/Integer";
			case 'J': return "Ljava/lang/Long";
			case 'F': return "Ljava/lang/Float";
			case 'D': return "Ljava/lang/Double";
			case 'B': return "Ljava/lang/Byte";
			case 'C': return "Ljava/lang/Character";
			case 'S': return "Ljava/lang/Short";
			case 'Z': return "Ljava/lang/Boolean";
			default:
				throw new IllegalArgumentException("Unexpected non primitive descriptor " + primitiveDescriptor);
		}
	}


	/**
	 * 用于生成字段的接口.
	 */
	@FunctionalInterface
	public interface FieldAdder {

		void generateField(ClassWriter cw, CodeFlow codeflow);
	}


	/**
	 * 用于生成{@code clinit}静态初始化程序块的接口.
	 */
	@FunctionalInterface
	public interface ClinitAdder {

		void generateCode(MethodVisitor mv, CodeFlow codeflow);
	}
}
