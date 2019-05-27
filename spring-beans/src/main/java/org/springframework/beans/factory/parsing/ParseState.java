package org.springframework.beans.factory.parsing;

import java.util.Stack;

/**
 * 基于{@link Stack}的简单结构, 用于在解析过程中跟踪逻辑位置.
 * 以特定于阅读器的方式在解析阶段的每个点, 添加{@link Entry entries}到堆栈中.
 *
 * <p>调用{@link #toString()}将呈现解析阶段中当前逻辑位置的树型视图.
 * 此表示旨在用于错误消息.
 */
public final class ParseState {

	/**
	 * 渲染树形表示时使用的制表符.
	 */
	private static final char TAB = '\t';

	/**
	 * 内部{@link Stack}存储.
	 */
	private final Stack<Entry> state;


	public ParseState() {
		this.state = new Stack<Entry>();
	}

	@SuppressWarnings("unchecked")
	private ParseState(ParseState other) {
		this.state = (Stack<Entry>) other.state.clone();
	}


	/**
	 * 将新的{@link Entry}添加到{@link Stack}.
	 */
	public void push(Entry entry) {
		this.state.push(entry);
	}

	/**
	 * 从{@link Stack}中删除{@link Entry}.
	 */
	public void pop() {
		this.state.pop();
	}

	/**
	 * 如果{@link Stack}为空, 则返回当前位于{@link Stack}顶部的{@link Entry}或{@code null}.
	 */
	public Entry peek() {
		return this.state.empty() ? null : this.state.peek();
	}

	/**
	 * 创建{@link ParseState}的新实例, 它是此实例的独立快照.
	 */
	public ParseState snapshot() {
		return new ParseState(this);
	}


	/**
	 * 返回当前{@code ParseState}的树型表示形式.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int x = 0; x < this.state.size(); x++) {
			if (x > 0) {
				sb.append('\n');
				for (int y = 0; y < x; y++) {
					sb.append(TAB);
				}
				sb.append("-> ");
			}
			sb.append(this.state.get(x));
		}
		return sb.toString();
	}


	/**
	 * {@link ParseState}中的条目的标记接口.
	 */
	public interface Entry {

	}

}
