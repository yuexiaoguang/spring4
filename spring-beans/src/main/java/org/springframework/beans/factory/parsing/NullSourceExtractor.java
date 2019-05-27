package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;

/**
 * {@link SourceExtractor}的简单实现, 返回{@code null}作为源元数据.
 *
 * <p>这是默认实现, 可防止在正常(非工具)运行使用期间将太多元数据保存在内存中.
 */
public class NullSourceExtractor implements SourceExtractor {

	/**
	 * 对于任何输入, 此实现只返回{@code null}.
	 */
	@Override
	public Object extractSource(Object sourceCandidate, Resource definitionResource) {
		return null;
	}

}
