package org.springframework.jdbc.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * {@link KeyHolder}接口的默认实现, 用于保存自动生成的键 (可能由JDBC插入语句返回).
 *
 * <p>为每个插入操作创建此类的实例, 并将其传递给相应的{@link org.springframework.jdbc.core.JdbcTemplate}
 * 或{org.springframework.jdbc.object.SqlUpdate}方法.
 */
public class GeneratedKeyHolder implements KeyHolder {

	private final List<Map<String, Object>> keyList;


	/**
	 * 使用默认的列表.
	 */
	public GeneratedKeyHolder() {
		this.keyList = new LinkedList<Map<String, Object>>();
	}

	/**
	 * @param keyList 用于保存键映射的列表
	 */
	public GeneratedKeyHolder(List<Map<String, Object>> keyList) {
		this.keyList = keyList;
	}


	@Override
	public Number getKey() throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
		if (this.keyList.size() == 0) {
			return null;
		}
		if (this.keyList.size() > 1 || this.keyList.get(0).size() > 1) {
			throw new InvalidDataAccessApiUsageException(
					"The getKey method should only be used when a single key is returned.  " +
					"The current key entry contains multiple keys: " + this.keyList);
		}
		Iterator<Object> keyIter = this.keyList.get(0).values().iterator();
		if (keyIter.hasNext()) {
			Object key = keyIter.next();
			if (!(key instanceof Number)) {
				throw new DataRetrievalFailureException(
						"The generated key is not of a supported numeric type. " +
						"Unable to cast [" + (key != null ? key.getClass().getName() : null) +
						"] to [" + Number.class.getName() + "]");
			}
			return (Number) key;
		}
		else {
			throw new DataRetrievalFailureException("Unable to retrieve the generated key. " +
					"Check that the table has an identity column enabled.");
		}
	}

	@Override
	public Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException {
		if (this.keyList.size() == 0) {
			return null;
		}
		if (this.keyList.size() > 1)
			throw new InvalidDataAccessApiUsageException(
					"The getKeys method should only be used when keys for a single row are returned.  " +
					"The current key list contains keys for multiple rows: " + this.keyList);
		return this.keyList.get(0);
	}

	@Override
	public List<Map<String, Object>> getKeyList() {
		return this.keyList;
	}

}
