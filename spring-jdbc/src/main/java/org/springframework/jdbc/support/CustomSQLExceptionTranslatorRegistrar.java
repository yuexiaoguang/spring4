package org.springframework.jdbc.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

/**
 * 用于为特定数据库注册自定义{@link org.springframework.jdbc.support.SQLExceptionTranslator}实例的注册表.
 */
public class CustomSQLExceptionTranslatorRegistrar implements InitializingBean {

	/**
	 * Map注册表, 保存特定数据库的自定义转换器.
	 * Key是{@link org.springframework.jdbc.support.SQLErrorCodesFactory}中定义的数据库产品名称.
	 */
	private final Map<String, SQLExceptionTranslator> translators = new HashMap<String, SQLExceptionTranslator>();


	/**
	 * 设置{@link SQLExceptionTranslator}引用的Map,
	 * key必须是{@code sql-error-codes.xml}文件中定义的数据库名称.
	 * <p>请注意, 除非数据库名称中存在匹配项, 否则任何现有的转换器都将保留, 此时新转换器将替换现有转换器.
	 */
	public void setTranslators(Map<String, SQLExceptionTranslator> translators) {
		this.translators.putAll(translators);
	}

	@Override
	public void afterPropertiesSet() {
		for (String dbName : this.translators.keySet()) {
			CustomSQLExceptionTranslatorRegistry.getInstance().registerTranslator(dbName, this.translators.get(dbName));
		}
	}

}
