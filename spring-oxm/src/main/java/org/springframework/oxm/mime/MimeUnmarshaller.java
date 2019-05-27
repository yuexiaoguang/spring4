package org.springframework.oxm.mime;

import java.io.IOException;
import javax.xml.transform.Source;

import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * Subinterface of {@link org.springframework.oxm.Unmarshaller} that can use MIME attachments
 * to optimize storage of binary data. Attachments can be added as MTOM, XOP, or SwA.
 */
public interface MimeUnmarshaller extends Unmarshaller {

	/**
	 * Unmarshals the given provided {@link Source} into an object graph,
	 * reading binary attachments from a {@link MimeContainer}.
	 * @param source the source to marshal from
	 * @param mimeContainer the MIME container to read extracted binary content from
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IOException if an I/O Exception occurs
	 */
	Object unmarshal(Source source, MimeContainer mimeContainer) throws XmlMappingException, IOException;

}
