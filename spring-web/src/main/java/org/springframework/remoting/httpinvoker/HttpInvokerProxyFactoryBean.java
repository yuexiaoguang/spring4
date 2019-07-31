package org.springframework.remoting.httpinvoker;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * 用于HTTP调用器代理的{@link FactoryBean}. 使用指定的服务接口公开代理服务以用作bean引用.
 *
 * <p>服务URL必须是公开HTTP调用器服务的HTTP URL.
 * 可选地, 可以指定代码库URL以从远程位置按需动态下载代码. 有关详细信息, 请参阅HttpInvokerClientInterceptor docs.
 *
 * <p>序列化远程调用对象并反序列化远程调用结果对象.
 * 像RMI一样使用Java序列化, 但提供与Caucho基于HTTP的Hessian和Burlap协议相同的易用性设置.
 *
 * <p><b>HTTP调用器是Java-to-Java远程处理的推荐协议.</b>
 * 它比Hessian和Burlap更强大, 更具扩展性, 但却牺牲了与Java的联系.
 * 然而, 它与Hessian和Burlap一样容易设置, 这是它与RMI相比的主要优势.
 *
 * <p><b>WARNING: 请注意由于不安全的Java反序列化导致的漏洞:
 * 在反序列化步骤中, 操作的输入流可能导致服务器上不需要的代码执行.
 * 因此, 不要将HTTP调用器端点暴露给不受信任的客户端, 而只是在自己的服务之间.</b>
 * 通常, 强烈建议使用任何其他消息格式 (e.g. JSON).
 */
public class HttpInvokerProxyFactoryBean extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (getServiceInterface() == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
	}


	@Override
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
