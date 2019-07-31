package org.springframework.http.converter.xml;

import javax.xml.transform.Source;

import org.springframework.http.converter.FormHttpMessageConverter;

/**
 * {@link org.springframework.http.converter.FormHttpMessageConverter}的扩展,
 * 通过{@link SourceHttpMessageConverter}添加对基于XML的部件的支持.
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
