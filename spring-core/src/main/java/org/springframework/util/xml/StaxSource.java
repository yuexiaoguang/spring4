package org.springframework.util.xml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * StAX读取器的{@code Source}标记接口的实现.
 * 可以使用{@code XMLEventReader}或{@code XMLStreamReader}构建.
 *
 * <p>这个类是必要的, 因为在JAXP 1.3中没有为StAX读取器实现{@code Source}.
 * 在JAXP 1.4 (JDK 1.6)中有一个{@code StAXSource}, 但出于向后兼容的原因, 这个类被保留了.
 *
 * <p>即使{@code StaxSource}从{@code SAXSource}扩展, 调用{@code SAXSource}的方法<strong>也不支持</strong>.
 * 通常, 此类唯一支持的操作是使用通过{@link #getXMLReader()}获得的{@code XMLReader}来解析通过{@link #getInputSource()}获得的输入源.
 * 调用{@link #setXMLReader(XMLReader)}或{@link #setInputSource(InputSource)}将导致{@code UnsupportedOperationException}.
 */
class StaxSource extends SAXSource {

	private XMLEventReader eventReader;

	private XMLStreamReader streamReader;


	/**
	 * 提供的事件读取器必须处于{@code XMLStreamConstants.START_DOCUMENT}或{@code XMLStreamConstants.START_ELEMENT}状态.
	 * 
	 * @param eventReader 要读取的{@code XMLEventReader}
	 * 
	 * @throws IllegalStateException 如果读取器不在文档或元素的开头
	 */
	StaxSource(XMLEventReader eventReader) {
		super(new StaxEventXMLReader(eventReader), new InputSource());
		this.eventReader = eventReader;
	}

	/**
	 * 提供的流读取器必须处于{@code XMLStreamConstants.START_DOCUMENT}或{@code XMLStreamConstants.START_ELEMENT}状态.
	 * 
	 * @param streamReader 要读取的{@code XMLStreamReader}
	 * 
	 * @throws IllegalStateException 如果读取器不在文档或元素的开头
	 */
	StaxSource(XMLStreamReader streamReader) {
		super(new StaxStreamXMLReader(streamReader), new InputSource());
		this.streamReader = streamReader;
	}


	/**
	 * 返回此{@code StaxSource}使用的{@code XMLEventReader}.
	 * <p>如果{@code StaxSource}是使用{@code XMLStreamReader}创建的, 则结果为{@code null}.
	 * 
	 * @return 此源使用的StAX事件读取器
	 */
	XMLEventReader getXMLEventReader() {
		return this.eventReader;
	}

	/**
	 * 返回此{@code StaxSource}使用的{@code XMLStreamReader}.
	 * <p>如果{@code StaxSource}是使用{@code XMLEventReader}创建的, 则结果为{@code null}.
	 * 
	 * @return 此源使用的StAX事件读取器
	 */
	XMLStreamReader getXMLStreamReader() {
		return this.streamReader;
	}


	/**
	 * 抛出{@code UnsupportedOperationException}.
	 * 
	 * @throws 总是UnsupportedOperationException
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * 抛出{@code UnsupportedOperationException}.
	 * 
	 * @throws 总是UnsupportedOperationException
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}

}
