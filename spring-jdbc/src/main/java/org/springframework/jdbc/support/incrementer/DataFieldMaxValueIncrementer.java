package org.springframework.jdbc.support.incrementer;

import org.springframework.dao.DataAccessException;

/**
 * 定义任何数据存储字段递增的最大值的约定的接口.
 * 与序列号生成器非常相似.
 *
 * <p>典型的实现可以使用标准SQL, 本机RDBMS序列或存储过程来完成此工作.
 */
public interface DataFieldMaxValueIncrementer {

	/**
	 * 递增数据存储字段的最大值.
	 * 
	 * @return 下一个数据存储值, 例如<b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException 发生错误
	 */
	int nextIntValue() throws DataAccessException;

	/**
	 * 递增数据存储字段的最大值.
	 * 
	 * @return 下一个数据存储值, 例如<b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException 发生错误
	 */
	long nextLongValue() throws DataAccessException;

	/**
	 * 递增数据存储字段的最大值.
	 * 
	 * @return 下一个数据存储值, 例如<b>max + 1</b>
	 * @throws org.springframework.dao.DataAccessException 发生错误
	 */
	String nextStringValue() throws DataAccessException;

}
