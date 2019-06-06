package org.springframework.oxm.castor;

import org.springframework.oxm.XmlMappingException;

/**
 * Exception thrown by {@link CastorMarshaller} whenever it encounters a mapping problem.
 *
 * @deprecated as of Spring Framework 4.3.13, due to the lack of activity on the Castor project
 */
@Deprecated
@SuppressWarnings("serial")
public class CastorMappingException extends XmlMappingException {

	public CastorMappingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
