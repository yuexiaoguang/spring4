package org.springframework.transaction.interceptor;

import java.beans.PropertyEditorSupport;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.util.StringUtils;

/**
 * 将String转换为{@link TransactionAttributeSource}的属性编辑器.
 * 事务属性字符串必须由此包中的{@link TransactionAttributeEditor}解析.
 *
 * <p>字符串是属性语法, 格式为:<br>
 * {@code FQCN.methodName=&lt;transaction attribute string&gt;}
 *
 * <p>示例:<br>
 * {@code com.mycompany.mycode.MyClass.myMethod=PROPAGATION_MANDATORY,ISOLATION_DEFAULT}
 *
 * <p><b>NOTE:</b> 指定的类必须是定义方法的类; 在实现接口的情况下, 则为接口类名称.
 *
 * <p>Note: 将为给定名称注册所有重载方法. 不支持某些重载方法的显式注册.
 * 支持"xxx*"映射, e.g. "notify*"表示"notify"和"notifyAll".
 */
public class TransactionAttributeSourceEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
		if (StringUtils.hasLength(text)) {
			// 使用属性编辑器来标记保持字符串.
			PropertiesEditor propertiesEditor = new PropertiesEditor();
			propertiesEditor.setAsText(text);
			Properties props = (Properties) propertiesEditor.getValue();

			// 现在有属性, 分别处理每个属性.
			TransactionAttributeEditor tae = new TransactionAttributeEditor();
			Enumeration<?> propNames = props.propertyNames();
			while (propNames.hasMoreElements()) {
				String name = (String) propNames.nextElement();
				String value = props.getProperty(name);
				// 将值转换为事务属性.
				tae.setAsText(value);
				TransactionAttribute attr = (TransactionAttribute) tae.getValue();
				// 注册名称和属性.
				source.addTransactionalMethod(name, attr);
			}
		}
		setValue(source);
	}

}
