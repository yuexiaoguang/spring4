package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * 表示对值的引用.
 * 通过引用, 可以获取或设置值.
 * 传递值引用而不是值本身, 可以避免错误地重复操作数评估.
 * 例如, 在'list[index++]++' 没有'list[index++]'的值引用时,
 * 有必要两次评估 list[index++] (一次获取值, 一次确定值的位置), 这将两次增加索引.
 */
public interface ValueRef {

	/**
	 * 返回此ValueRef指向的值, 它不应要求表达式组件重新评估.
	 */
	TypedValue getValue();

	/**
	 * 设置此ValueRef指向的值, 它不应要求表达式组件重新评估.
	 * 
	 * @param newValue 新值
	 */
	void setValue(Object newValue);

	/**
	 * 指示是否支持调用 setValue(Object).
	 * 
	 * @return true 如果此值引用支持 setValue()
	 */
	boolean isWritable();


	/**
	 * null值的ValueRef.
	 */
	class NullValueRef implements ValueRef {

		static final NullValueRef INSTANCE = new NullValueRef();

		@Override
		public TypedValue getValue() {
			return TypedValue.NULL;
		}

		@Override
		public void setValue(Object newValue) {
			// 异常位置'0'不正确, 但创建每个节点的实例 (节点仅用于错误报告) 的开销将是不幸的.
			throw new SpelEvaluationException(0, SpelMessage.NOT_ASSIGNABLE, "null");
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}


	/**
	 * 单个值的ValueRef持有者, 无法设置.
	 */
	class TypedValueHolderValueRef implements ValueRef {

		private final TypedValue typedValue;

		private final SpelNodeImpl node;  // 仅用于错误报告

		public TypedValueHolderValueRef(TypedValue typedValue, SpelNodeImpl node) {
			this.typedValue = typedValue;
			this.node = node;
		}

		@Override
		public TypedValue getValue() {
			return this.typedValue;
		}

		@Override
		public void setValue(Object newValue) {
			throw new SpelEvaluationException(this.node.pos, SpelMessage.NOT_ASSIGNABLE, this.node.toStringAST());
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}
}
