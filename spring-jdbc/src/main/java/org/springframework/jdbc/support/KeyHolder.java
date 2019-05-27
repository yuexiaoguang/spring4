package org.springframework.jdbc.support;

import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * 用于检索键的接口, 通常用于JDBC插入语句可能返回的自动生成的键.
 *
 * <p>该接口的实现可以容纳任意数量的键.
 *
 * <p>大多数应用程序仅在每行键上使用, 并且在insert语句中一次只处理一行.
 * 在这些情况下, 只需调用{@code getKey}来检索键. 此处返回的值是Number, 这是自动生成的键的常用类型.
 */
public interface KeyHolder {

	/**
	 * 从第一个Map中检索第一个条目, 假设只有一个条目和一个Map, 并且该条目是一个数字.
	 * 这是典型的情况: 生成的单个数字的键.
	 * <p>键保存在Map列表中, 其中列表中的每个条目代表每行的键.
	 * 如果有多列, 那么Map也会有多个条目.
	 * 如果此方法遇到Map或List中的多个条目, 意味着返回了多个键, 则抛出InvalidDataAccessApiUsageException.
	 * 
	 * @return 生成的键
	 * @throws InvalidDataAccessApiUsageException 如果遇到多个键.
	 */
	Number getKey() throws InvalidDataAccessApiUsageException;

	/**
	 * 检索第一个Map的键.
	 * 如果列表中有多个条目 (意味着多行有返回的键), 则抛出InvalidDataAccessApiUsageException.
	 * 
	 * @return 生成的键的Map
	 * @throws InvalidDataAccessApiUsageException 如果遇到多行的键
	 */
	Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException;

	/**
	 * 返回对包含键的List的引用.
	 * 可用于提取多行的键 (不常见的情况), 也可用于添加新的键映射.
	 * 
	 * @return 生成的键的列表, 每个条目都是列名和键值的映射
	 */
	List<Map<String, Object>> getKeyList();

}
