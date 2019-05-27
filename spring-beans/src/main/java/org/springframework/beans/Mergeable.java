package org.springframework.beans;

/**
 * 表示值集可以与父对象的值集合并的对象的接口.
 */
public interface Mergeable {

	/**
	 * 是否为此特定实例启用了合并?
	 */
	boolean isMergeEnabled();

	/**
	 * 将当前值集与提供的对象的值集合并.
	 * <p>提供的对象被视为父对象, 并且被调用者的值集中的值必须覆盖所提供对象的值.
	 * 
	 * @param parent 要与其合并的对象
	 * 
	 * @return 合并后的结果
	 * @throws IllegalArgumentException 如果提供的parent是 {@code null}
	 * @throws IllegalStateException 如果没有为此实例启用合并 (i.e. {@code mergeEnabled} 等于 {@code false}).
	 */
	Object merge(Object parent);

}
