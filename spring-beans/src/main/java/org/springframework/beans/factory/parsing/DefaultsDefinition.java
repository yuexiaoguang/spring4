package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;

/**
 * 用于默认定义的标记接口, 扩展BeanMetadataElement以继承源公开性.
 *
 * <p>具体实现通常基于“文档默认值”, 例如在XML文档中的根标签级别指定.
 */
public interface DefaultsDefinition extends BeanMetadataElement {

}
