package org.springframework.oxm.mime;

import javax.activation.DataHandler;

/**
 * Represents a container for MIME attachments
 * Concrete implementations might adapt a SOAPMessage or an email message.
 */
public interface MimeContainer {

	/**
	 * Indicate whether this container is a XOP package.
	 * @return {@code true} when the constraints specified in
	 * <a href="http://www.w3.org/TR/2005/REC-xop10-20050125/#identifying_xop_documents">Identifying XOP Documents</a>
	 * are met
	 */
	boolean isXopPackage();

	/**
	 * Turn this message into a XOP package.
	 * @return {@code true} when the message actually is a XOP package
	 */
	boolean convertToXopPackage();

	/**
	 * Add the given data handler as an attachment to this container.
	 * @param contentId  the content id of the attachment
	 * @param dataHandler the data handler containing the data of the attachment
	 */
	void addAttachment(String contentId, DataHandler dataHandler);

	/**
	 * Return the attachment with the given content id, or {@code null} if not found.
	 * @param contentId the content id
	 * @return the attachment, as a data handler
	 */
	DataHandler getAttachment(String contentId);

}
