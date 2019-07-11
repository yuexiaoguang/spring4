package org.springframework.transaction.annotation;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.transaction.config.TransactionManagementConfigUtils;

/**
 * 根据导入{@code @Configuration}类中{@link EnableTransactionManagement#mode}的值,
 * 选择应使用的{@link AbstractTransactionManagementConfiguration}实现.
 */
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {

	/**
	 * 分别为{@link EnableTransactionManagement#mode()}的{@code PROXY}和{@code ASPECTJ}值,
	 * 返回{@link ProxyTransactionManagementConfiguration}或{@code AspectJTransactionManagementConfiguration}.
	 */
	@Override
	protected String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return new String[] {AutoProxyRegistrar.class.getName(),
						ProxyTransactionManagementConfiguration.class.getName()};
			case ASPECTJ:
				return new String[] {
						TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME};
			default:
				return null;
		}
	}

}
