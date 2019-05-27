package org.springframework.remoting.caucho;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.burlap.io.BurlapInput;
import com.caucho.burlap.io.BurlapOutput;
import com.caucho.burlap.server.BurlapSkeleton;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteExporter;
import org.springframework.util.Assert;

/**
 * General stream-based protocol exporter for a Burlap endpoint.
 *
 * <p>Burlap is a slim, XML-based RPC protocol.
 * For information on Burlap, see the
 * <a href="http://www.caucho.com/burlap">Burlap website</a>.
 * This exporter requires Burlap 3.x.
 *
 * @deprecated as of Spring 4.0, since Burlap hasn't evolved in years
 * and is effectively retired (in contrast to its sibling Hessian)
 */
@Deprecated
public class BurlapExporter extends RemoteExporter implements InitializingBean {

	private BurlapSkeleton skeleton;


	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * Initialize this service exporter.
	 */
	public void prepare() {
		checkService();
		checkServiceInterface();
		this.skeleton = new BurlapSkeleton(getProxyForService(), getServiceInterface());
	}


	/**
	 * Perform an invocation on the exported object.
	 * @param inputStream the request stream
	 * @param outputStream the response stream
	 * @throws Throwable if invocation failed
	 */
	public void invoke(InputStream inputStream, OutputStream outputStream) throws Throwable {
		Assert.notNull(this.skeleton, "Burlap exporter has not been initialized");
		ClassLoader originalClassLoader = overrideThreadContextClassLoader();
		try {
			this.skeleton.invoke(new BurlapInput(inputStream), new BurlapOutput(outputStream));
		}
		finally {
			try {
				inputStream.close();
			}
			catch (IOException ex) {
				// ignore
			}
			try {
				outputStream.close();
			}
			catch (IOException ex) {
				// ignore
			}
			resetThreadContextClassLoader(originalClassLoader);
		}
	}

}
