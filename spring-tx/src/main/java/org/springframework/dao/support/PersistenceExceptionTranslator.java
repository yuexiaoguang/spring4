package org.springframework.dao.support;

import org.springframework.dao.DataAccessException;

/**
 * Spring与生成运行时异常的数据访问技术集成实现的接口, 例如JPA, TopLink, JDO 和 Hibernate.
 *
 * <p>这允许一致使用组合异常转换功能, 而不必强制单个转换器理解每种可能的异常类型.
 */
public interface PersistenceExceptionTranslator {

	/**
	 * 将持久化框架抛出的给定运行时异常转换为Spring的通用{@link org.springframework.dao.DataAccessException}层次结构中的相应异常.
	 * <p>不要转换此转换器无法理解的异常:
	 * 例如, 如果来自另一个持久化框架, 或者来自用户代码或与持久化无关.
	 * <p>特别重要的是正确转换为DataIntegrityViolationException, 例如在约束违规时.
	 * 实现可以使用Spring JDBC的复杂异常转换, 在SQLException事件中提供更多信息作为根本原因.
	 * 
	 * @param ex 要转换的RuntimeException
	 * 
	 * @return 相应的DataAccessException (或{@code null}, 如果无法转换异常, 因为在这种情况下, 它可能来自用户代码而不是实际的持久性问题)
	 */
	DataAccessException translateExceptionIfPossible(RuntimeException ex);

}
