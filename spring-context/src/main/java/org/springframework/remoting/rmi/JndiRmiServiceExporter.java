package org.springframework.remoting.rmi;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiTemplate;

/**
 * 将RMI服务绑定到JNDI的服务导出器.
 * 通常用于RMI-IIOP (CORBA).
 *
 * <p>通过{@link javax.rmi.PortableRemoteObject}类导出服务.
 * 您需要使用"-iiop"选项运行 "rmic", 以便为每个导出的服务生成相应的stub和骨架.
 *
 * <p>还支持通过RMI调用器公开任何非RMI服务,
 * 可通过{@link JndiRmiClientInterceptor} / {@link JndiRmiProxyFactoryBean}自动检测此类调用器.
 *
 * <p>使用RMI调用器, RMI通信在{@link RmiInvocationHandler}级别上工作, 对于任何服务只需要一个stub.
 * 服务接口不必在所有方法上扩展{@code java.rmi.Remote}或抛出{@code java.rmi.RemoteException}, 但in和out参数必须是可序列化的.
 *
 * <p>JNDI环境可以指定为"jndiEnvironment" bean属性, 或者在{@code jndi.properties}文件中配置或作为系统属性配置.
 * For example:
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props>
 *		 &lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		 &lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 */
public class JndiRmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	private JndiTemplate jndiTemplate = new JndiTemplate();

	private String jndiName;

	private Remote exportedObject;


	/**
	 * 设置用于JNDI查找的JNDI模板.
	 * 还可以通过"jndiEnvironment"指定JNDI环境设置.
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * 设置用于JNDI查找的JNDI环境.
	 * 使用给定的环境设置创建JndiTemplate.
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * 设置导出的RMI服务的JNDI名称.
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}


	@Override
	public void afterPropertiesSet() throws NamingException, RemoteException {
		prepare();
	}

	/**
	 * 初始化此服务导出器, 将指定的服务绑定到JNDI.
	 * 
	 * @throws NamingException 如果服务绑定失败
	 * @throws RemoteException 如果服务导出失败
	 */
	public void prepare() throws NamingException, RemoteException {
		if (this.jndiName == null) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}

		// 初始化并缓存导出的对象.
		this.exportedObject = getObjectToExport();
		PortableRemoteObject.exportObject(this.exportedObject);

		rebind();
	}

	/**
	 * 将指定的服务重新绑定到JNDI, 以便在目标注册表重新启动时进行恢复.
	 * 
	 * @throws NamingException 如果服务绑定失败
	 */
	public void rebind() throws NamingException {
		if (logger.isInfoEnabled()) {
			logger.info("Binding RMI service to JNDI location [" + this.jndiName + "]");
		}
		this.jndiTemplate.rebind(this.jndiName, this.exportedObject);
	}

	/**
	 * 在bean工厂关闭时从JNDI取消绑定RMI服务.
	 */
	@Override
	public void destroy() throws NamingException, NoSuchObjectException {
		if (logger.isInfoEnabled()) {
			logger.info("Unbinding RMI service from JNDI location [" + this.jndiName + "]");
		}
		this.jndiTemplate.unbind(this.jndiName);
		PortableRemoteObject.unexportObject(this.exportedObject);
	}
}
