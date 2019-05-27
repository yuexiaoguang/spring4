package org.springframework.expression.spel.standard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * SpelCompiler将采用常规解析表达式, 并创建 (并加载) 包含与该表达式相同的字节代码的类.
 * 表达式的编译形式将比解释形式的评估速度快得多.
 *
 * <p>SpelCompiler目前不处理所有表达式类型, 但涵盖了许多常见情况.
 * 该框架可扩展, 以涵盖未来的更多案例. 对于绝对最大速度, 编译代码中*没有检查*.
 * 表达式的编译版本在生成字节代码时, 使用在表达式的解释运行期间学习的信息.
 * 例如, 如果它知道某个特定的属性取消引用似乎总是返回一个Map, 那么它将生成字节代码, 该字节代码要求属性取消引用的结果为Map.
 * 这确保了最大的性能, 但是如果取消引用导致除了Map之外的其他内容, 则编译的表达式将失败
 * - 如果在常规Java程序中传递意外类型的数据, 就会发生ClassCastException.
 *
 * <p>由于缺乏检查, 可能存在一些永远不应编译的表达式, 例如, 如果表达式不断处理不同类型的数据.
 * 由于这些情况, 编译器必须为关联的SpelExpressionParser (通过{@link SpelParserConfiguration}对象)选择性地打开, 默认情况下不启用.
 *
 * <p>可以通过调用{@code SpelCompiler.compile(expression)}来编译单个表达式.
 */
public class SpelCompiler implements Opcodes {

	private static final Log logger = LogFactory.getLog(SpelCompiler.class);

	// 为每个类加载器创建一个编译器, 它管理该类加载器的子类加载器, 子进程用于加载已编译的表达式.
	private static final Map<ClassLoader, SpelCompiler> compilers =
			new ConcurrentReferenceHashMap<ClassLoader, SpelCompiler>();


	// 子ClassLoader用于加载已编译的表达式类
	private final ChildClassLoader ccl;

	// 此SpelCompiler实例中生成的类的计数器后缀
	private final AtomicInteger suffixId = new AtomicInteger(1);


	private SpelCompiler(ClassLoader classloader) {
		this.ccl = new ChildClassLoader(classloader);
	}


	/**
	 * 尝试编译提供的表达式.
	 * 在编译进行之前, 检查是否可以编译.
	 * 检查涉及访问表达式Ast中的所有节点, 并确保知道它们足够的状态.
	 * 
	 * @param expression 要编译的表达式
	 * 
	 * @return 实现已编译表达式的类的实例, 或{@code null} 如果无法编译
	 */
	public CompiledExpression compile(SpelNodeImpl expression) {
		if (expression.isCompilable()) {
			if (logger.isDebugEnabled()) {
				logger.debug("SpEL: compiling " + expression.toStringAST());
			}
			Class<? extends CompiledExpression> clazz = createExpressionClass(expression);
			if (clazz != null) {
				try {
					return clazz.newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to instantiate CompiledExpression", ex);
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("SpEL: unable to compile " + expression.toStringAST());
		}
		return null;
	}

	private int getNextSuffix() {
		return this.suffixId.incrementAndGet();
	}

	/**
	 * 生成封装已编译的表达式并定义它的类.
	 * 生成的类将是CompiledExpression的子类型.
	 * 
	 * @param expressionToCompile 要编译的表达式
	 * 
	 * @return 表达式调用, 或{@code null} 如果决定在代码生成期间选择退出编译
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl expressionToCompile) {
		// 创建类外层'spel/ExNNN extends org.springframework.expression.spel.CompiledExpression'
		String clazzName = "spel/Ex" + getNextSuffix();
		ClassWriter cw = new ExpressionClassWriter();
		cw.visit(V1_5, ACC_PUBLIC, clazzName, null, "org/springframework/expression/spel/CompiledExpression", null);

		// 创建默认构造函数
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression",
				"<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		// Create getValue() method
		mv = cw.visitMethod(ACC_PUBLIC, "getValue",
				"(Ljava/lang/Object;Lorg/springframework/expression/EvaluationContext;)Ljava/lang/Object;", null,
				new String[ ]{"org/springframework/expression/EvaluationException"});
		mv.visitCode();

		CodeFlow cf = new CodeFlow(clazzName, cw);

		// 请求表达式AST生成方法体
		try {
			expressionToCompile.generateCode(mv, cf);
		}
		catch (IllegalStateException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(expressionToCompile.getClass().getSimpleName() +
						".generateCode opted out of compilation: " + ex.getMessage());
			}
			return null;
		}

		CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
		if ("V".equals(cf.lastDescriptor())) {
			mv.visitInsn(ACONST_NULL);
		}
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0, 0);  // 由于COMPUTE_MAXS而未提供
		mv.visitEnd();
		cw.visitEnd();

		cf.finish();

		byte[] data = cw.toByteArray();
		// TODO 需要根据debug标志有条件地进行此操作
		// dump(expressionToCompile.toStringAST(), clazzName, data);
		return (Class<? extends CompiledExpression>) this.ccl.defineClass(clazzName.replaceAll("/", "."), data);
	}


	/**
	 * 编译器实例的工厂方法.
	 * 返回的SpelCompiler将附加一个类加载器作为给定类加载器的子级, 并且该子级将用于加载已编译的表达式.
	 * 
	 * @param classLoader 用作编译的基础的ClassLoader
	 * 
	 * @return 相应的SpelCompiler实例
	 */
	public static SpelCompiler getCompiler(ClassLoader classLoader) {
		ClassLoader clToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		synchronized (compilers) {
			SpelCompiler compiler = compilers.get(clToUse);
			if (compiler == null) {
				compiler = new SpelCompiler(clToUse);
				compilers.put(clToUse, compiler);
			}
			return compiler;
		}
	}

	/**
	 * 请求尝试编译指定的表达式.
	 * 如果表达式的组件不适合编译, 或所涉及的数据类型不适合编译, 则可能会失败. 用于测试.
	 * 
	 * @return true 如果表达式已成功编译
	 */
	public static boolean compile(Expression expression) {
		return (expression instanceof SpelExpression && ((SpelExpression) expression).compileExpression());
	}

	/**
	 * 请求恢复到解释器进行表达式评估.
	 * 任何已编译的形式都将被丢弃, 但可以通过以后重新编译重新创建.
	 * 
	 * @param expression 表达式
	 */
	public static void revertToInterpreted(Expression expression) {
		if (expression instanceof SpelExpression) {
			((SpelExpression) expression).revertToInterpreted();
		}
	}

	/**
	 * 出于调试目的, 将指定的字节代码转储到磁盘上的文件中.
	 * 尚未挂钩, 需要根据系统属性有条件地调用.
	 * 
	 * @param expressionText 编译的表达式的文本
	 * @param name 用于编译表达式的类的名称
	 * @param bytecode 生成的类的字节码
	 */
	@SuppressWarnings("unused")
	private static void dump(String expressionText, String name, byte[] bytecode) {
		String nameToUse = name.replace('.', '/');
		String dir = (nameToUse.indexOf('/') != -1 ? nameToUse.substring(0, nameToUse.lastIndexOf('/')) : "");
		String dumpLocation = null;
		try {
			File tempFile = File.createTempFile("tmp", null);
			dumpLocation = tempFile + File.separator + nameToUse + ".class";
			tempFile.delete();
			File f = new File(tempFile, dir);
			f.mkdirs();
			// System.out.println("Expression '" + expressionText + "' compiled code dumped to " + dumpLocation);
			if (logger.isDebugEnabled()) {
				logger.debug("Expression '" + expressionText + "' compiled code dumped to " + dumpLocation);
			}
			f = new File(dumpLocation);
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(bytecode);
			fos.flush();
			fos.close();
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Unexpected problem dumping class '" + nameToUse + "' into " + dumpLocation, ex);
		}
	}


	/**
	 * ChildClassLoader将加载生成的已编译的表达式类.
	 */
	private static class ChildClassLoader extends URLClassLoader {

		private static final URL[] NO_URLS = new URL[0];

		public ChildClassLoader(ClassLoader classLoader) {
			super(NO_URLS, classLoader);
		}

		public Class<?> defineClass(String name, byte[] bytes) {
			return super.defineClass(name, bytes, 0, bytes.length);
		}
	}


	private class ExpressionClassWriter extends ClassWriter {

		public ExpressionClassWriter() {
			super(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		}

		@Override
		protected ClassLoader getClassLoader() {
			return ccl;
		}
	}
}
