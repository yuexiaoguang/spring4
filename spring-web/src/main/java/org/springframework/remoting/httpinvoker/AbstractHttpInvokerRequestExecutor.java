package org.springframework.remoting.httpinvoker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * HttpInvokerRequestExecutor接口的抽象基础实现.
 *
 * <p>预实现RemoteInvocation对象的序列化和RemoteInvocationResults对象的反序列化.
 */
public abstract class AbstractHttpInvokerRequestExecutor implements HttpInvokerRequestExecutor, BeanClassLoaderAware {

	/**
	 * 默认内容类型: "application/x-java-serialized-object"
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";

	private static final int SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE = 1024;


	protected static final String HTTP_METHOD_POST = "POST";

	protected static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";

	protected static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";

	protected static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";

	protected static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	protected static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";

	protected static final String ENCODING_GZIP = "gzip";


	protected final Log logger = LogFactory.getLog(getClass());

	private String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private boolean acceptGzipEncoding = true;

	private ClassLoader beanClassLoader;


	/**
	 * 指定用于发送HTTP调用器请求的内容类型.
	 * <p>默认"application/x-java-serialized-object".
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * 返回用于发送HTTP调用器请求的内容类型.
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否接受GZIP编码, 即是否以"gzip"作为值发送HTTP "Accept-Encoding" header.
	 * <p>默认"true". 关闭此标志, 如果不想使用GZIP响应压缩, 即使在HTTP服务器上启用了.
	 */
	public void setAcceptGzipEncoding(boolean acceptGzipEncoding) {
		this.acceptGzipEncoding = acceptGzipEncoding;
	}

	/**
	 * 返回是否接受GZIP编码, 即是否以"gzip"作为值发送HTTP "Accept-Encoding" header.
	 */
	public boolean isAcceptGzipEncoding() {
		return this.acceptGzipEncoding;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 返回此执行器应该使用的bean ClassLoader.
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	@Override
	public final RemoteInvocationResult executeRequest(
			HttpInvokerClientConfiguration config, RemoteInvocation invocation) throws Exception {

		ByteArrayOutputStream baos = getByteArrayOutputStream(invocation);
		if (logger.isDebugEnabled()) {
			logger.debug("Sending HTTP invoker request for service at [" + config.getServiceUrl() +
					"], with size " + baos.size());
		}
		return doExecuteRequest(config, baos);
	}

	/**
	 * 将给定的RemoteInvocation序列化为ByteArrayOutputStream.
	 * 
	 * @param invocation RemoteInvocation对象
	 * 
	 * @return 具有序列化RemoteInvocation的ByteArrayOutputStream
	 * @throws IOException
	 */
	protected ByteArrayOutputStream getByteArrayOutputStream(RemoteInvocation invocation) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(SERIALIZED_INVOCATION_BYTE_ARRAY_INITIAL_SIZE);
		writeRemoteInvocation(invocation, baos);
		return baos;
	}

	/**
	 * 将给定的RemoteInvocation序列化为给定的OutputStream.
	 * <p>默认实现首先使用{@code decorateOutputStream}装饰流 (例如, 用于自定义加密或压缩).
	 * 为最终流创建一个{@code ObjectOutputStream}并调用{@code doWriteRemoteInvocation}来实际写入对象.
	 * <p>可以重写以进行调用的自定义序列化.
	 * 
	 * @param invocation RemoteInvocation对象
	 * @param os 要写入的OutputStream
	 * 
	 * @throws IOException
	 */
	protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(decorateOutputStream(os));
		try {
			doWriteRemoteInvocation(invocation, oos);
		}
		finally {
			oos.close();
		}
	}

	/**
	 * 返回用于写入远程调用的OutputStream, 可能会修改给定的原始OutputStream.
	 * <p>默认实现按原样返回给定的流.
	 * 可以覆盖, 例如, 自定义加密或压缩.
	 * 
	 * @param os 原始OutputStream
	 * 
	 * @return 可能装饰的OutputStream
	 */
	protected OutputStream decorateOutputStream(OutputStream os) throws IOException {
		return os;
	}

	/**
	 * 实际写入给定调用对象到给定ObjectOutputStream.
	 * <p>默认实现调用{@code writeObject}.
	 * 可以重写自定义包装器对象的序列化而不是普通调用, 例如加密感知保存器.
	 * 
	 * @param invocation RemoteInvocation对象
	 * @param oos 要写入的ObjectOutputStream
	 * 
	 * @throws IOException
	 */
	protected void doWriteRemoteInvocation(RemoteInvocation invocation, ObjectOutputStream oos) throws IOException {
		oos.writeObject(invocation);
	}


	/**
	 * 执行发送给定序列化远程调用的请求.
	 * <p>实现通常会调用{@code readRemoteInvocationResult}来反序列化返回的RemoteInvocationResult对象.
	 * 
	 * @param config 指定目标服务的 HTTP调用器配置
	 * @param baos 包含序列化RemoteInvocation对象的ByteArrayOutputStream
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws IOException
	 * @throws ClassNotFoundException 如果在反序列化期间抛出
	 * @throws Exception
	 */
	protected abstract RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws Exception;

	/**
	 * 从给定的InputStream反序列化RemoteInvocationResult对象.
	 * <p>使用{@code decorateInputStream}首先装饰流 (例如, 用于自定义加密或压缩).
	 * 通过{@code createObjectInputStream}创建一个{@code ObjectInputStream}
	 * 并调用{@code doReadRemoteInvocationResult}来实际读取对象.
	 * <p>可以重写以进行调用的自定义序列化.
	 * 
	 * @param is 要读取的InputStream
	 * @param codebaseUrl 用于加载类的代码库URL, 如果未在本地找到
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws IOException
	 * @throws ClassNotFoundException 如果在反序列化期间抛出
	 */
	protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, String codebaseUrl)
			throws IOException, ClassNotFoundException {

		ObjectInputStream ois = createObjectInputStream(decorateInputStream(is), codebaseUrl);
		try {
			return doReadRemoteInvocationResult(ois);
		}
		finally {
			ois.close();
		}
	}

	/**
	 * 返回用于读取远程调用结果的InputStream, 可能会修饰给定的原始InputStream.
	 * <p>默认实现按原样返回给定的流. 可以覆盖, 例如, 自定义加密或压缩.
	 * 
	 * @param is 原始InputStream
	 * 
	 * @return 可能已装饰的InputStream
	 */
	protected InputStream decorateInputStream(InputStream is) throws IOException {
		return is;
	}

	/**
	 * 为给定的InputStream和codebase创建一个ObjectInputStream.
	 * 默认实现创建CodebaseAwareObjectInputStream.
	 * 
	 * @param is 要读取的InputStream
	 * @param codebaseUrl 用于加载类的代码库URL, 如果未在本地找到 (can be {@code null})
	 * 
	 * @return 要使用的新ObjectInputStream实例
	 * @throws IOException 如果ObjectInputStream的创建失败
	 */
	protected ObjectInputStream createObjectInputStream(InputStream is, String codebaseUrl) throws IOException {
		return new CodebaseAwareObjectInputStream(is, getBeanClassLoader(), codebaseUrl);
	}

	/**
	 * 从给定的ObjectInputStream执行实际的调用对象读取.
	 * <p>默认实现调用{@code readObject}.
	 * 可以覆盖自定义包装器对象的反序列化而不是普通调用, 例如加密感知的保存器.
	 * 
	 * @param ois 要读取的ObjectInputStream
	 * 
	 * @return RemoteInvocationResult对象
	 * @throws IOException
	 * @throws ClassNotFoundException 如果序列化对象的类名无法解析
	 */
	protected RemoteInvocationResult doReadRemoteInvocationResult(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {

		Object obj = ois.readObject();
		if (!(obj instanceof RemoteInvocationResult)) {
			throw new RemoteException("Deserialized object needs to be assignable to type [" +
					RemoteInvocationResult.class.getName() + "]: " + ClassUtils.getDescriptiveType(obj));
		}
		return (RemoteInvocationResult) obj;
	}

}
