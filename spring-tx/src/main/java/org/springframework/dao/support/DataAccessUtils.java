package org.springframework.dao.support;

import java.util.Collection;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;

/**
 * DAO实现的其他工具方法.
 * 适用于任何数据访问技术.
 */
public abstract class DataAccessUtils {

	/**
	 * 从给定的Collection返回单个结果对象.
	 * <p>如果找到0个结果对象, 则返回{@code null}; 如果找到多于1个元素, 则抛出异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 单个结果对象, 或{@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个元素
	 */
	public static <T> T singleResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			return null;
		}
		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的Collection返回单个结果对象.
	 * <p>如果找到0或多于1个元素, 则抛出异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 单个结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个元素
	 * @throws EmptyResultDataAccessException 如果在给定的Collection中找不到任何元素
	 */
	public static <T> T requiredSingleResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			throw new EmptyResultDataAccessException(1);
		}
		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的Collection返回唯一的结果对象.
	 * <p>如果找到0个结果对象, 则返回{@code null}; 如果找到多个实例, 则抛出异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 唯一的结果对象, 或{@code null}
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个结果对象
	 */
	public static <T> T uniqueResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			return null;
		}
		if (!CollectionUtils.hasUniqueObject(results)) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的Collection返回唯一的结果对象.
	 * <p>如果找到0或多于1个实例, 则引发异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 唯一的结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的Collection中找不到任何结果对象
	 */
	public static <T> T requiredUniqueResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
		if (CollectionUtils.isEmpty(results)) {
			throw new EmptyResultDataAccessException(1);
		}
		if (!CollectionUtils.hasUniqueObject(results)) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}

	/**
	 * 从给定的Collection返回唯一的结果对象.
	 * 如果找到0或多于1个结果对象, 或者如果唯一结果对象不能转换为指定的所需类型, 则抛出异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 唯一的结果对象
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的Collection中找不到任何结果对象
	 * @throws TypeMismatchDataAccessException 如果唯一对象与指定的类型不匹配
	 */
	@SuppressWarnings("unchecked")
	public static <T> T objectResult(Collection<?> results, Class<T> requiredType)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		Object result = requiredUniqueResult(results);
		if (requiredType != null && !requiredType.isInstance(result)) {
			if (String.class == requiredType) {
				result = result.toString();
			}
			else if (Number.class.isAssignableFrom(requiredType) && Number.class.isInstance(result)) {
				try {
					result = NumberUtils.convertNumberToTargetClass(((Number) result), (Class<? extends Number>) requiredType);
				}
				catch (IllegalArgumentException ex) {
					throw new TypeMismatchDataAccessException(ex.getMessage());
				}
			}
			else {
				throw new TypeMismatchDataAccessException(
						"Result object is of type [" + result.getClass().getName() +
						"] and could not be converted to required type [" + requiredType.getName() + "]");
			}
		}
		return (T) result;
	}

	/**
	 * 返回给定Collection中的唯一int结果.
	 * 如果找到0或多于1个结果对象, 或者如果唯一结果对象不能转换为int, 则抛出异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 唯一的int结果
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的Collection中找不到任何结果对象
	 * @throws TypeMismatchDataAccessException 如果集合中的唯一对象不可转换为int
	 */
	public static int intResult(Collection<?> results)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		return objectResult(results, Number.class).intValue();
	}

	/**
	 * 返回给定Collection中的唯一long结果.
	 * 如果找到0或多于1个结果对象, 或者如果唯一结果对象不可转换为long, 则抛出异常.
	 * 
	 * @param results 结果Collection (can be {@code null})
	 * 
	 * @return 唯一long结果
	 * @throws IncorrectResultSizeDataAccessException 如果在给定的Collection中找到了多个结果对象
	 * @throws EmptyResultDataAccessException 如果在给定的Collection中找不到任何结果对象
	 * @throws TypeMismatchDataAccessException 如果集合中的唯一对象不可转换为 long
	 */
	public static long longResult(Collection<?> results)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		return objectResult(results, Number.class).longValue();
	}


	/**
	 * 如果这是合适的, 则返回已转换的异常, 否则按原样返回给定的异常.
	 * 
	 * @param rawException 要转换的异常
	 * @param pet 用于执行转换的PersistenceExceptionTranslator
	 * 
	 * @return 如果可以进行转换, 则为已转换的持久化异常; 如果不能, 则为原始异常
	 */
	public static RuntimeException translateIfNecessary(
			RuntimeException rawException, PersistenceExceptionTranslator pet) {

		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		DataAccessException dex = pet.translateExceptionIfPossible(rawException);
		return (dex != null ? dex : rawException);
	}

}
