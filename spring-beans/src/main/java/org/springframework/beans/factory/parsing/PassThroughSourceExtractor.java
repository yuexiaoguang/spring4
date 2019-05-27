package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;

/**
 * 简单的{@link SourceExtractor}实现, 它只传递候选源元数据对象以进行附件.
 *
 * <p>使用此实现意味着, 工具将获得对该工具提供的底层配置源数据的原始访问权限.
 *
 * <p>不应在生产应用程序中使用此实现, 因为它可能会在内存中保留过多元数据 (unnecessarily).
 */
public class PassThroughSourceExtractor implements SourceExtractor {

	/**
	 * 只需按原样返回提供的{@code sourceCandidate}即可.
	 * 
	 * @param sourceCandidate 源数据
	 * 
	 * @return 提供的{@code sourceCandidate}
	 */
	@Override
	public Object extractSource(Object sourceCandidate, Resource definingResource) {
		return sourceCandidate;
	}

}
