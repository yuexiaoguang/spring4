package org.springframework.test.annotation;

/**
 * <p>
 * 用于检索给定测试环境的<em>配置文件值</em>的策略接口.
 * </p>
 * <p>
 * 具体实现必须提供{@code public} 无参构造函数.
 * </p>
 * <p>
 * Spring提供了以下开箱即用的实现:
 * </p>
 * <ul>
 * <li>{@link SystemProfileValueSource}</li>
 * </ul>
 */
public interface ProfileValueSource {

	/**
	 * 获取指定键指示的<em>配置文件值</em>.
	 * 
	 * @param key <em>配置文件值</em>的名称
	 * 
	 * @return <em>配置文件值</em>的字符串值, 如果没有带有该键的<em>配置文件值</em>, 则为{@code null}
	 */
	String get(String key);

}
