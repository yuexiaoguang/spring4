package org.springframework.oxm.mime;

import java.io.IOException;
import javax.xml.transform.Result;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * Subinterface of {@link Marshaller} that can use MIME attachments to optimize
 * storage of binary data. Attachments can be added as MTOM, XOP, or SwA.
 */
public interface MimeMarshaller extends Marshaller {

	/**
	 * Marshals the object graph with the given root into the provided {@link Result},
	 * writing binary data to a {@link MimeContainer}.
	 * @param graph the root of the object graph to marshal
	 * @param result the result to marshal to
	 * @param mimeContainer the MIME container to write extracted binary content to
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IOException if an I/O exception occurs
	 */
	void marshal(Object graph, Result result, MimeContainer mimeContainer) throws XmlMappingException, IOException;

}
