package org.springframework.remoting.caucho;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.HessianDebugOutputStream;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import org.apache.commons.logging.Log;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteExporter;
import org.springframework.util.Assert;
import org.springframework.util.CommonsLogWriter;

/**
 * 用于Hessian端点的基于流的一般协议导出器.
 *
 * <p>Hessian是一种轻量级的二进制RPC协议.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>.
 * <b>Note: 从Spring 4.0开始, 这个导出器需要Hessian 4.0或更高版本.</b>
 */
public class HessianExporter extends RemoteExporter implements InitializingBean {

	public static final String CONTENT_TYPE_HESSIAN = "application/x-hessian";


	private SerializerFactory serializerFactory = new SerializerFactory();

	private HessianRemoteResolver remoteResolver;

	private Log debugLogger;

	private HessianSkeleton skeleton;


	/**
	 * 指定要使用的Hessian SerializerFactory.
	 * <p>这通常作为{@code com.caucho.hessian.io.SerializerFactory}类型的内部bean定义传入, 并应用了自定义bean属性值.
	 */
	public void setSerializerFactory(SerializerFactory serializerFactory) {
		this.serializerFactory = (serializerFactory != null ? serializerFactory : new SerializerFactory());
	}

	/**
	 * 设置是否为每个序列化集合发送Java集合类型. 默认为"true".
	 */
	public void setSendCollectionType(boolean sendCollectionType) {
		this.serializerFactory.setSendCollectionType(sendCollectionType);
	}

	/**
	 * 设置是否允许非可序列化类型作为Hessian参数和返回值. 默认"true".
	 */
	public void setAllowNonSerializable(boolean allowNonSerializable) {
		this.serializerFactory.setAllowNonSerializable(allowNonSerializable);
	}

	/**
	 * 指定用于解析远程对象引用的自定义HessianRemoteResolver.
	 */
	public void setRemoteResolver(HessianRemoteResolver remoteResolver) {
		this.remoteResolver = remoteResolver;
	}

	/**
	 * 设置是否应启用Hessian的调试模式, 记录到此导出器的Commons Logging日志. 默认"false".
	 */
	public void setDebug(boolean debug) {
		this.debugLogger = (debug ? logger : null);
	}


	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * 初始化导出器.
	 */
	public void prepare() {
		checkService();
		checkServiceInterface();
		this.skeleton = new HessianSkeleton(getProxyForService(), getServiceInterface());
	}


	/**
	 * 对导出的对象执行调用.
	 * 
	 * @param inputStream 请求流
	 * @param outputStream 响应流
	 * 
	 * @throws Throwable 如果调用失败
	 */
	public void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable {
		Assert.notNull(this.skeleton, "Hessian exporter has not been initialized");
		doInvoke(this.skeleton, inputStream, outputStream);
	}

	/**
	 * 实际上用给定的流调用骨架.
	 * 
	 * @param skeleton 要调用的骨架
	 * @param inputStream 请求流
	 * @param outputStream 响应流
	 * 
	 * @throws Throwable 如果调用失败
	 */
	protected void doInvoke(HessianSkeleton skeleton, InputStream inputStream, OutputStream outputStream)
			throws Throwable {

		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			InputStream isToUse = inputStream;
			OutputStream osToUse = outputStream;

			if (this.debugLogger != null && this.debugLogger.isDebugEnabled()) {
				PrintWriter debugWriter = new PrintWriter(new CommonsLogWriter(this.debugLogger));
				@SuppressWarnings("resource")
				HessianDebugInputStream dis = new HessianDebugInputStream(inputStream, debugWriter);
				@SuppressWarnings("resource")
				HessianDebugOutputStream dos = new HessianDebugOutputStream(outputStream, debugWriter);
				dis.startTop2();
				dos.startTop2();
				isToUse = dis;
				osToUse = dos;
			}

			if (!isToUse.markSupported()) {
				isToUse = new BufferedInputStream(isToUse);
				isToUse.mark(1);
			}

			int code = isToUse.read();
			int major;
			int minor;

			AbstractHessianInput in;
			AbstractHessianOutput out;

			if (code == 'H') {
				// Hessian 2.0 stream
				major = isToUse.read();
				minor = isToUse.read();
				if (major != 0x02) {
					throw new IOException("Version " + major + '.' + minor + " is not understood");
				}
				in = new Hessian2Input(isToUse);
				out = new Hessian2Output(osToUse);
				in.readCall();
			}
			else if (code == 'C') {
				// Hessian 2.0 call... for some reason not handled in HessianServlet!
				isToUse.reset();
				in = new Hessian2Input(isToUse);
				out = new Hessian2Output(osToUse);
				in.readCall();
			}
			else if (code == 'c') {
				// Hessian 1.0 call
				major = isToUse.read();
				minor = isToUse.read();
				in = new HessianInput(isToUse);
				if (major >= 2) {
					out = new Hessian2Output(osToUse);
				}
				else {
					out = new HessianOutput(osToUse);
				}
			}
			else {
				throw new IOException("Expected 'H'/'C' (Hessian 2.0) or 'c' (Hessian 1.0) in hessian input at " + code);
			}

			if (this.serializerFactory != null) {
				in.setSerializerFactory(this.serializerFactory);
				out.setSerializerFactory(this.serializerFactory);
			}
			if (this.remoteResolver != null) {
				in.setRemoteResolver(this.remoteResolver);
			}

			try {
				skeleton.invoke(in, out);
			}
			finally {
				try {
					in.close();
					isToUse.close();
				}
				catch (IOException ex) {
					// ignore
				}
				try {
					out.close();
					osToUse.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
		finally {
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

}
