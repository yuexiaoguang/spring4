package org.springframework.remoting.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 远程服务导出器的抽象基类, 它显式反序列化
 * {@link org.springframework.remoting.support.RemoteInvocation}对象并序列化
 * {@link org.springframework.remoting.support.RemoteInvocationResult}对象,
 * 例如Spring的HTTP调用器.
 *
 * <p>为{@code ObjectInputStream}和{@code ObjectOutputStream}处理提供模板方法.
 */
public abstract class RemoteInvocationSerializingExporter extends RemoteInvocationBasedExporter
		implements InitializingBean {

	/**
	 * 默认内容类型: "application/x-java-serialized-object"
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";


	private String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private boolean acceptProxyClasses = true;

	private Object proxy;


	/**
	 * 指定用于发送远程调用响应的内容类型.
	 * <p>默认 "application/x-java-serialized-object".
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * 返回用于发送远程调用响应的内容类型.
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否接受代理类的反序列化.
	 * <p>默认 "true". 可以取消激活作为安全措施.
	 */
	public void setAcceptProxyClasses(boolean acceptProxyClasses) {
		this.acceptProxyClasses = acceptProxyClasses;
	}

	/**
	 * 是否接受代理类的反序列化.
	 */
	public boolean isAcceptProxyClasses() {
		return this.acceptProxyClasses;
	}


	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * 初始化此服务导出器.
	 */
	public void prepare() {
		this.proxy = getProxyForService();
	}

	protected final Object getProxy() {
		if (this.proxy == null) {
			throw new IllegalStateException(ClassUtils.getShortName(getClass()) + " has not been initialized");
		}
		return this.proxy;
	}


	/**
	 * 为给定的InputStream创建一个ObjectInputStream.
	 * <p>默认实现创建Spring {@link CodebaseAwareObjectInputStream}.
	 * 
	 * @param is 要读取的InputStream
	 * 
	 * @return 要使用的新ObjectInputStream实例
	 * @throws java.io.IOException 如果ObjectInputStream创建失败
	 */
	protected ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
		return new CodebaseAwareObjectInputStream(is, getBeanClassLoader(), isAcceptProxyClasses());
	}

	/**
	 * 从给定的ObjectInputStream执行调用结果对象的实际读取.
	 * <p>默认实现只是调用{@link java.io.ObjectInputStream#readObject()}.
	 * 可以覆盖自定义包装器对象的反序列化而不是普通调用, 例如加密感知的持有者.
	 * 
	 * @param ois 要读取的ObjectInputStream
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws java.io.IOException 如果I/O失败
	 * @throws ClassNotFoundException 如果在本地ClassLoader中找不到转移类
	 */
	protected RemoteInvocation doReadRemoteInvocation(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {

		Object obj = ois.readObject();
		if (!(obj instanceof RemoteInvocation)) {
			throw new RemoteException("Deserialized object needs to be assignable to type [" +
					RemoteInvocation.class.getName() + "]: " + ClassUtils.getDescriptiveType(obj));
		}
		return (RemoteInvocation) obj;
	}

	/**
	 * 为给定的OutputStream创建ObjectOutputStream.
	 * <p>默认实现创建一个普通的{@link java.io.ObjectOutputStream}.
	 * 
	 * @param os 要写入的OutputStream
	 * 
	 * @return 要使用的新ObjectOutputStream实例
	 * @throws java.io.IOException 如果ObjectOutputStream创建失败
	 */
	protected ObjectOutputStream createObjectOutputStream(OutputStream os) throws IOException {
		return new ObjectOutputStream(os);
	}

	/**
	 * 执行给定调用结果对象到给定ObjectOutputStream的实际写入.
	 * <p>默认实现调用{@link java.io.ObjectOutputStream#writeObject}.
	 * 可以重写自定义包装器对象的序列化, 而不是普通调用, 例如加密感知持有者.
	 * 
	 * @param result the RemoteInvocationResult object
	 * @param oos 要写入的ObjectOutputStream
	 * 
	 * @throws java.io.IOException 如果由I/O 方法抛出
	 */
	protected void doWriteRemoteInvocationResult(RemoteInvocationResult result, ObjectOutputStream oos)
			throws IOException {

		oos.writeObject(result);
	}
}
