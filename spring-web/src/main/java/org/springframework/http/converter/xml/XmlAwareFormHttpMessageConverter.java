package org.springframework.http.converter.xml;

import javax.xml.transform.Source;

import org.springframework.http.converter.FormHttpMessageConverter;

/**
 * Extension of {@link org.springframework.http.converter.FormHttpMessageConverter},
 * adding support for XML-based parts through a {@link SourceHttpMessageConverter}.
 *
 * @deprecated in favor of
 * {@link org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter}
 */
@Deprecated
public class XmlAwareFormHttpMessageConverter extends FormHttpMessageConverter {

	public XmlAwareFormHttpMessageConverter() {
		super();
		addPartConverter(new SourceHttpMessageConverter<Source>());
	}

}
