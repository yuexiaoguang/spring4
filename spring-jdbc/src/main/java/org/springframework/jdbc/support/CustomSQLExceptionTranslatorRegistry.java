package org.springframework.jdbc.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 与特定数据库关联的自定义{@link org.springframework.jdbc.support.SQLExceptionTranslator}实例的注册表,
 * 允许根据名为"sql-error-codes.xml"的配置文件中包含的值覆盖转换.
 */
public class CustomSQLExceptionTranslatorRegistry {

	private static final Log logger = LogFactory.getLog(CustomSQLExceptionTranslatorRegistry.class);

	/**
	 * 跟踪单个实例, 以便可以将其返回给请求它的类.
	 */
	private static final CustomSQLExceptionTranslatorRegistry instance = new CustomSQLExceptionTranslatorRegistry();


	/**
	 * 返回单例实例.
	 */
	public static CustomSQLExceptionTranslatorRegistry getInstance() {
		return instance;
	}


	/**
	 * 保存特定数据库的自定义转换器的注册表.
	 * Key是{@link org.springframework.jdbc.support.SQLErrorCodesFactory}中定义的数据库产品名称.
	 */
	private final Map<String, SQLExceptionTranslator> translatorMap = new HashMap<String, SQLExceptionTranslator>();


	/**
	 * <p>强制Singleton设计模式.
	 */
	private CustomSQLExceptionTranslatorRegistry() {
	}

	/**
	 * 为指定的数据库名称注册新的自定义转换器.
	 * 
	 * @param dbName 数据库名称
	 * @param translator 自定义转换器
	 */
	public void registerTranslator(String dbName, SQLExceptionTranslator translator) {
		SQLExceptionTranslator replaced = translatorMap.put(dbName, translator);
		if (replaced != null) {
			logger.warn("Replacing custom translator [" + replaced + "] for database '" + dbName +
					"' with [" + translator + "]");
		}
		else {
			logger.info("Adding custom translator of type [" + translator.getClass().getName() +
					"] for database '" + dbName + "'");
		}
	}

	/**
	 * 查找指定数据库的自定义转换器.
	 * 
	 * @param dbName 数据库名称
	 * 
	 * @return 自定义转换器, 或{@code null}
	 */
	public SQLExceptionTranslator findTranslatorForDatabase(String dbName) {
		return this.translatorMap.get(dbName);
	}
}
