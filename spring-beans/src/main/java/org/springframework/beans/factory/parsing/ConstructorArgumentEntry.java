package org.springframework.beans.factory.parsing;

import org.springframework.util.Assert;

/**
 * 表示(可能是索引的)构造函数参数的{@link ParseState}条目.
 */
public class ConstructorArgumentEntry implements ParseState.Entry {

	private final int index;


	/**
	 * 表示具有(当前)未知索引的构造函数参数.
	 */
	public ConstructorArgumentEntry() {
		this.index = -1;
	}

	/**
	 * 在提供的{@code index}处表示构造函数参数.
	 * 
	 * @param index 构造函数参数的索引
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code index}小于零
	 */
	public ConstructorArgumentEntry(int index) {
		Assert.isTrue(index >= 0, "Constructor argument index must be greater than or equal to zero");
		this.index = index;
	}


	@Override
	public String toString() {
		return "Constructor-arg" + (this.index >= 0 ? " #" + this.index : "");
	}

}
