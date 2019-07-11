package org.springframework.transaction.interceptor;

import java.util.Properties;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 用于简化声明式事务处理的代理工厂bean.
 * 这是标准AOP {@link org.springframework.aop.framework.ProxyFactoryBean}的一个方便的替代方案,
 * 具有单独的{@link TransactionInterceptor}定义.
 *
 * <p><strong>HISTORICAL NOTE:</strong> 该类最初设计用于涵盖声明性事务划分的典型情况:
 * 即, 使用事务代理包装单个目标对象, 代理目标实现的所有接口.
 * 但是, 在Spring 2.0及更高版本中, 此处提供的功能被更方便的{@code tx:} XML命名空间所取代.
 * 请参阅Spring参考文档的<a href="http://bit.ly/qUwvwz">声明式事务管理</a>部分,
 * 以了解管理Spring应用程序中的事务的现代选项.
 * 出于这些原因, <strong>用户应该支持{@code tx:} XML命名空间以及
 * @{@link org.springframework.transaction.annotation.Transactional Transactional}
 * 和@{@link org.springframework.transaction.annotation.EnableTransactionManagement EnableTransactionManagement}注解.</strong>
 *
 * <p>需要指定三个主要属性:
 * <ul>
 * <li>"transactionManager": 要使用的{@link PlatformTransactionManager}实现
 * (例如, {@link org.springframework.transaction.jta.JtaTransactionManager}实例)
 * <li>"target": 应为其创建事务代理的目标对象
 * <li>"transactionAttributes": 每个目标方法名称(或方法名称模式)的事务属性(例如, 传播行为和"readOnly"标志)
 * </ul>
 *
 * <p>如果没有显式设置"transactionManager"属性, 并且{@link FactoryBean}在{@link ListableBeanFactory}中运行,
 * 则将从{@link BeanFactory}获取类型为{@link PlatformTransactionManager}的匹配bean.
 *
 * <p>与{@link TransactionInterceptor}相反, 事务属性被指定为属性, 方法名称为键, 事务属性描述符为值.
 * 方法名称始终应用于目标类.
 *
 * <p>在内部, 使用{@link TransactionInterceptor}实例, 但此类的用户不必关心.
 * 可选, 可以指定方法切点以对底层{@link TransactionInterceptor}进行条件调用.
 *
 * <p>可以设置"preInterceptors"和"postInterceptors"属性, 以向混合添加额外的拦截器, 如
 * {@link org.springframework.aop.interceptor.PerformanceMonitorInterceptor}.
 *
 * <p><b>HINT:</b> 此类通常与父/子 bean定义一起使用.
 * 通常, 将在抽象父bean定义中定义事务管理器和默认事务属性 (用于方法名称模式),
 * 从而为特定目标对象派生具体的子bean定义.
 * 这将每个bean的定义工作量降至最低.
 *
 * <pre code="class">
 * {@code
 * <bean id="baseTransactionProxy" class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean"
 *     abstract="true">
 *   <property name="transactionManager" ref="transactionManager"/>
 *   <property name="transactionAttributes">
 *     <props>
 *       <prop key="insert*">PROPAGATION_REQUIRED</prop>
 *       <prop key="update*">PROPAGATION_REQUIRED</prop>
 *       <prop key="*">PROPAGATION_REQUIRED,readOnly</prop>
 *     </props>
 *   </property>
 * </bean>
 *
 * <bean id="myProxy" parent="baseTransactionProxy">
 *   <property name="target" ref="myTarget"/>
 * </bean>
 *
 * <bean id="yourProxy" parent="baseTransactionProxy">
 *   <property name="target" ref="yourTarget"/>
 * </bean>}</pre>
 */
@SuppressWarnings("serial")
public class TransactionProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware {

	private final TransactionInterceptor transactionInterceptor = new TransactionInterceptor();

	private Pointcut pointcut;


	/**
	 * 设置默认事务管理器.
	 * 这将执行实际的事务管理: 这个类只是一种调用它的方法.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionInterceptor.setTransactionManager(transactionManager);
	}

	/**
	 * 设置属性, 使用方法名称作为键, 事务属性描述符作为值 (通过TransactionAttributeEditor解析):
	 * e.g. key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
	 * <p>Note: 方法名称始终应用于目标类, 无论是在接口中定义还是在类本身中定义.
	 * <p>在内部, 将从给定属性创建NameMatchTransactionAttributeSource.
	 */
	public void setTransactionAttributes(Properties transactionAttributes) {
		this.transactionInterceptor.setTransactionAttributes(transactionAttributes);
	}

	/**
	 * 设置用于查找事务属性的事务属性源.
	 * 如果指定String属性值, PropertyEditor将从该值创建MethodMapTransactionAttributeSource.
	 */
	public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
		this.transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
	}

	/**
	 * 设置切点, i.e 可以根据传递的方法和属性导致TransactionInterceptor的条件调用的bean.
	 * Note: 始终调用其他拦截器.
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	/**
	 * 此回调是可选的: 如果在BeanFactory中运行, 并且没有显式设置事务管理器,
	 * 则将从BeanFactory中获取类型为{@link PlatformTransactionManager}的单个匹配bean.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.transactionInterceptor.setBeanFactory(beanFactory);
	}


	/**
	 * 为此FactoryBean的TransactionInterceptor创建一个顾问.
	 */
	@Override
	protected Object createMainInterceptor() {
		this.transactionInterceptor.afterPropertiesSet();
		if (this.pointcut != null) {
			return new DefaultPointcutAdvisor(this.pointcut, this.transactionInterceptor);
		}
		else {
			// 依赖默认切点.
			return new TransactionAttributeSourceAdvisor(this.transactionInterceptor);
		}
	}

	/**
	 * 从4.2开始, 此方法将{@link TransactionalProxy}添加到代理接口集, 以避免重新处理事务元数据.
	 */
	@Override
	protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
		proxyFactory.addInterface(TransactionalProxy.class);
	}

}
