package org.springframework.jdbc.support.xml;

import javax.xml.transform.Result;

/**
 * 定义为XML输入提供{@code Result}数据所涉及的处理的接口.
 */
public interface XmlResultProvider {

	/**
	 * 实现必须实现此方法, 以便为{@code Result}提供XML内容.
	 * 实现将根据使用的{@code Result}实现而有所不同.
	 * 
	 * @param result 用于提供XML输入的{@code Result}对象
	 */
	void provideXml(Result result);

}
