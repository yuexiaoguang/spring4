package org.springframework.jmx.export.naming;

import java.util.Hashtable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@code ObjectNamingStrategy}接口的实现, 它根据给定实例的标识创建名称.
 *
 * <p>生成的{@code ObjectName}将在表单中
 * <i>package</i>:class=<i>class name</i>,hashCode=<i>identity hash (in hex)</i>
 */
public class IdentityNamingStrategy implements ObjectNamingStrategy {

	public static final String TYPE_KEY = "type";

	public static final String HASH_CODE_KEY = "hashCode";


	/**
	 * 根据托管资源的标识, 返回{@code ObjectName}的实例.
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		String domain = ClassUtils.getPackageName(managedBean.getClass());
		Hashtable<String, String> keys = new Hashtable<String, String>();
		keys.put(TYPE_KEY, ClassUtils.getShortName(managedBean.getClass()));
		keys.put(HASH_CODE_KEY, ObjectUtils.getIdentityHexString(managedBean));
		return ObjectNameManager.getInstance(domain, keys);
	}

}
