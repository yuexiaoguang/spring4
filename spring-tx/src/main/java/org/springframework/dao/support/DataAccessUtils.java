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
 * Miscellaneous utility methods for DAO implementations.
 * Useful with any data access technology.
 */
public abstract class DataAccessUtils {

	/**
	 * Return a single result object from the given Collection.
	 * <p>Returns {@code null} if 0 result objects found;
	 * throws an exception if more than 1 element found.
	 * @param results the result Collection (can be {@code null})
	 * @return the single result object, or {@code null} if none
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * element has been found in the given Collection
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
	 * Return a single result object from the given Collection.
	 * <p>Throws an exception if 0 or more than 1 element found.
	 * @param results the result Collection (can be {@code null})
	 * @return the single result object
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * element has been found in the given Collection
	 * @throws EmptyResultDataAccessException if no element at all
	 * has been found in the given Collection
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
	 * Return a unique result object from the given Collection.
	 * <p>Returns {@code null} if 0 result objects found;
	 * throws an exception if more than 1 instance found.
	 * @param results the result Collection (can be {@code null})
	 * @return the unique result object, or {@code null} if none
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * result object has been found in the given Collection
	 * @see org.springframework.util.CollectionUtils#hasUniqueObject
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
	 * Return a unique result object from the given Collection.
	 * <p>Throws an exception if 0 or more than 1 instance found.
	 * @param results the result Collection (can be {@code null})
	 * @return the unique result object
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * result object has been found in the given Collection
	 * @throws EmptyResultDataAccessException if no result object at all
	 * has been found in the given Collection
	 * @see org.springframework.util.CollectionUtils#hasUniqueObject
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
	 * Return a unique result object from the given Collection.
	 * Throws an exception if 0 or more than 1 result objects found,
	 * of if the unique result object is not convertible to the
	 * specified required type.
	 * @param results the result Collection (can be {@code null})
	 * @return the unique result object
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * result object has been found in the given Collection
	 * @throws EmptyResultDataAccessException if no result object
	 * at all has been found in the given Collection
	 * @throws TypeMismatchDataAccessException if the unique object does
	 * not match the specified required type
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
	 * Return a unique int result from the given Collection.
	 * Throws an exception if 0 or more than 1 result objects found,
	 * of if the unique result object is not convertible to an int.
	 * @param results the result Collection (can be {@code null})
	 * @return the unique int result
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * result object has been found in the given Collection
	 * @throws EmptyResultDataAccessException if no result object
	 * at all has been found in the given Collection
	 * @throws TypeMismatchDataAccessException if the unique object
	 * in the collection is not convertible to an int
	 */
	public static int intResult(Collection<?> results)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		return objectResult(results, Number.class).intValue();
	}

	/**
	 * Return a unique long result from the given Collection.
	 * Throws an exception if 0 or more than 1 result objects found,
	 * of if the unique result object is not convertible to a long.
	 * @param results the result Collection (can be {@code null})
	 * @return the unique long result
	 * @throws IncorrectResultSizeDataAccessException if more than one
	 * result object has been found in the given Collection
	 * @throws EmptyResultDataAccessException if no result object
	 * at all has been found in the given Collection
	 * @throws TypeMismatchDataAccessException if the unique object
	 * in the collection is not convertible to a long
	 */
	public static long longResult(Collection<?> results)
			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {

		return objectResult(results, Number.class).longValue();
	}


	/**
	 * Return a translated exception if this is appropriate,
	 * otherwise return the given exception as-is.
	 * @param rawException an exception that we may wish to translate
	 * @param pet PersistenceExceptionTranslator to use to perform the translation
	 * @return a translated persistence exception if translation is possible,
	 * or the raw exception if it is not
	 */
	public static RuntimeException translateIfNecessary(
			RuntimeException rawException, PersistenceExceptionTranslator pet) {

		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		DataAccessException dex = pet.translateExceptionIfPossible(rawException);
		return (dex != null ? dex : rawException);
	}

}