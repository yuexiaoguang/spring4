package org.springframework.dao.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;

/**
 * 支持链接的{@link PersistenceExceptionTranslator}的实现, 允许按顺序添加PersistenceExceptionTranslator实例.
 * 在第一个匹配时返回{@code non-null}.
 */
public class ChainedPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

	/** List of PersistenceExceptionTranslators */
	private final List<PersistenceExceptionTranslator> delegates = new ArrayList<PersistenceExceptionTranslator>(4);


	/**
	 * 将PersistenceExceptionTranslator添加到已链接的委托列表中.
	 */
	public final void addDelegate(PersistenceExceptionTranslator pet) {
		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		this.delegates.add(pet);
	}

	/**
	 * 返回所有已注册的PersistenceExceptionTranslator委托 (作为数组).
	 */
	public final PersistenceExceptionTranslator[] getDelegates() {
		return this.delegates.toArray(new PersistenceExceptionTranslator[this.delegates.size()]);
	}


	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		for (PersistenceExceptionTranslator pet : this.delegates) {
			DataAccessException translatedDex = pet.translateExceptionIfPossible(ex);
			if (translatedDex != null) {
				return translatedDex;
			}
		}
		return null;
	}

}
