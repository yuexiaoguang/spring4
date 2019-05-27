package org.springframework.jdbc.support.xml;

import org.springframework.jdbc.support.SqlValue;

/**
 * {@link org.springframework.jdbc.support.SqlValue}的子接口,
 * 支持将XML数据传递到指定列并添加清理回调, 在设置值并执行相应的语句后调用.
 */
public interface SqlXmlValue extends SqlValue {

}
