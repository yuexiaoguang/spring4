package org.springframework.dao.annotation;

import java.lang.annotation.Annotation;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

/**
 * Bean后处理器, 自动将持久化异常转换应用于标记有Spring的
 * @{@link org.springframework.stereotype.Repository Repository}注解的任何bean,
 * 将相应的{@link PersistenceExceptionTranslationAdvisor}添加到公开的代理 (现有的AOP代理或实现所有目标接口的新生成的代理).
 *
 * <p>将本机资源异常转换为Spring的{@link org.springframework.dao.DataAccessException DataAccessException}层次结构.
 * 自动检测实现{@link org.springframework.dao.support.PersistenceExceptionTranslator PersistenceExceptionTranslator}接口的bean,
 * 随后要求它们转换候选异常.
 *

 * <p>所有Spring的适用资源工厂 (e.g. {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean})
 * 都实现了开箱即用的{@code PersistenceExceptionTranslator}接口.
 * 因此, 通常需要启用自动异常转换的所有Bean都是使用{@code @Repository}注解标记所有受影响的bean (例如 Repositories 或 DAOs),
 * 并在应用程序上下文中将此后处理器定义为bean.
 */
@SuppressWarnings("serial")
public class PersistenceExceptionTranslationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	private Class<? extends Annotation> repositoryAnnotationType = Repository.class;


	/**
	 * 设置'repository'注解类型.
	 * 默认存储库注解类型是{@link Repository}注解.
	 * <p>存在此setter属性, 以便开发人员可以提供自己的(非Spring特定的)注解类型来指示类具有的存储库角色.
	 * 
	 * @param repositoryAnnotationType 所需的注解类型
	 */
	public void setRepositoryAnnotationType(Class<? extends Annotation> repositoryAnnotationType) {
		Assert.notNull(repositoryAnnotationType, "'repositoryAnnotationType' must not be null");
		this.repositoryAnnotationType = repositoryAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);

		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalArgumentException(
					"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
		}
		this.advisor = new PersistenceExceptionTranslationAdvisor(
				(ListableBeanFactory) beanFactory, this.repositoryAnnotationType);
	}

}
