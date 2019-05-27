package org.springframework.web.portlet.multipart;

import javax.portlet.ActionRequest;

import org.springframework.web.multipart.MultipartRequest;

/**
 * Interface which provides additional methods for dealing with multipart
 * content within a portlet request, allowing to access uploaded files.
 * Implementations also need to override the standard ActionRequest
 * methods for parameter access, making multipart parameters available.
 *
 * <p>A concrete implementation is {@link DefaultMultipartActionRequest}.
 */
public interface MultipartActionRequest extends ActionRequest, MultipartRequest {

}
