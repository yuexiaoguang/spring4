package org.springframework.transaction.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * {@link FactoryBean}等同于 &lt;tx:jta-transaction-manager/&gt; XML元素,
 * 自动检测WebLogic和WebSphere服务器, 并公开相应的{@link org.springframework.transaction.jta.JtaTransactionManager}子类.
 */
public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager> {

	private static final String WEBLOGIC_JTA_TRANSACTION_MANAGER_CLASS_NAME =
			"org.springframework.transaction.jta.WebLogicJtaTransactionManager";

	private static final String WEBSPHERE_TRANSACTION_MANAGER_CLASS_NAME =
			"org.springframework.transaction.jta.WebSphereUowTransactionManager";

	private static final String JTA_TRANSACTION_MANAGER_CLASS_NAME =
			"org.springframework.transaction.jta.JtaTransactionManager";


	private static final boolean weblogicPresent = ClassUtils.isPresent(
			"weblogic.transaction.UserTransaction", JtaTransactionManagerFactoryBean.class.getClassLoader());

	private static final boolean webspherePresent = ClassUtils.isPresent(
			"com.ibm.wsspi.uow.UOWManager", JtaTransactionManagerFactoryBean.class.getClassLoader());


	private final JtaTransactionManager transactionManager;


	@SuppressWarnings("unchecked")
	public JtaTransactionManagerFactoryBean() {
		String className = resolveJtaTransactionManagerClassName();
		try {
			Class<? extends JtaTransactionManager> clazz = (Class<? extends JtaTransactionManager>)
					ClassUtils.forName(className, JtaTransactionManagerFactoryBean.class.getClassLoader());
			this.transactionManager = BeanUtils.instantiate(clazz);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load JtaTransactionManager class: " + className, ex);
		}
	}


	@Override
	public JtaTransactionManager getObject() {
		return this.transactionManager;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.transactionManager != null ? this.transactionManager.getClass() : JtaTransactionManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	static String resolveJtaTransactionManagerClassName() {
		if (weblogicPresent) {
			return WEBLOGIC_JTA_TRANSACTION_MANAGER_CLASS_NAME;
		}
		else if (webspherePresent) {
			return WEBSPHERE_TRANSACTION_MANAGER_CLASS_NAME;
		}
		else {
			return JTA_TRANSACTION_MANAGER_CLASS_NAME;
		}
	}

}
