package org.springframework.oxm.support;

import java.io.IOException;

import org.xml.sax.InputSource;

import org.springframework.core.io.Resource;

/**
 * Convenient utility methods for dealing with SAX.
 */
public abstract class SaxResourceUtils {

	/**
	 * Create a SAX {@code InputSource} from the given resource.
	 * <p>Sets the system identifier to the resource's {@code URL}, if available.
	 * @param resource the resource
	 * @return the input source created from the resource
	 * @throws IOException if an I/O exception occurs
	 */
	public static InputSource createInputSource(Resource resource) throws IOException {
		InputSource inputSource = new InputSource(resource.getInputStream());
		inputSource.setSystemId(getSystemId(resource));
		return inputSource;
	}

	/**
	 * Retrieve the URL from the given resource as System ID.
	 * <p>Returns {@code null} if it cannot be opened.
	 */
	private static String getSystemId(Resource resource) {
		try {
			return resource.getURI().toString();
		}
		catch (IOException ex) {
			return null;
		}
	}

}
