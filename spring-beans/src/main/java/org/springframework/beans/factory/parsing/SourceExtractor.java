package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;

/**
 * 简单的策略, 允许工具控制源数据如何附加到bean定义元数据.
 *
 * <p>配置解析器可以提供在解析阶段附加源数据的能力.
 * 他们将以通用格式提供此元数据, 在附加到bean定义元数据之前, 可以通过{@link SourceExtractor}进一步修改它们.
 */
public interface SourceExtractor {

	/**
	 * 从配置解析器提供的候选对象中提取源数据.
	 * 
	 * @param sourceCandidate 原始源数据 (never {@code null})
	 * @param definingResource 定义给定源对象的资源 (may be {@code null})
	 * 
	 * @return 要存储的源数据对象 (may be {@code null})
	 */
	Object extractSource(Object sourceCandidate, Resource definingResource);

}
