package org.springframework.oxm;

import java.io.IOException;
import javax.xml.transform.Source;

/**
 * Defines the contract for Object XML Mapping unmarshallers. Implementations of this
 * interface can deserialize a given XML Stream to an Object graph.
 */
public interface Unmarshaller {

	/**
	 * Indicate whether this unmarshaller can unmarshal instances of the supplied type.
	 * @param clazz the class that this unmarshaller is being asked if it can marshal
	 * @return {@code true} if this unmarshaller can indeed unmarshal to the supplied class;
	 * {@code false} otherwise
	 */
	boolean supports(Class<?> clazz);

	/**
	 * Unmarshal the given {@link Source} into an object graph.
	 * @param source the source to marshal from
	 * @return the object graph
	 * @throws IOException if an I/O error occurs
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 */
	Object unmarshal(Source source) throws IOException, XmlMappingException;

}
