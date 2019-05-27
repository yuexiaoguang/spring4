package org.springframework.expression.spel;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.PropertyAccessor;

/**
 * 可编译的属性访问器能够生成表示访问操作的字节码, 便于编译到使用访问器的表达式的字节码.
 */
public interface CompilablePropertyAccessor extends PropertyAccessor, Opcodes {

	/**
	 * 如果此属性访问器当前适合编译, 则返回{@code true}.
	 */
	boolean isCompilable();

	/**
	 * 返回被访问属性的类型 - 只有在访问发生后才能知道.
	 */
	Class<?> getPropertyType();

	/**
	 * 生成字节码, 在必要时使用代码流中的上下文信息, 对指定的MethodVisitor执行访问操作.
	 * 
	 * @param propertyName 属性名
	 * @param mv 应该生成代码的Asm方法访问器
	 * @param cf 表达式编译器的当前状态
	 */
	void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf);

}
