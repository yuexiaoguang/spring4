package org.springframework.util.xml;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * StAX编写器的{@code Result}标记接口的实现.
 * 可以使用{@code XMLEventConsumer}或{@code XMLStreamWriter}构建.
 *
 * <p>此类是必需的, 因为在JAXP 1.3中没有针对StaxReaders的{@code Source}实现.
 * 在JAXP 1.4 (JDK 1.6)中有一个{@code StAXResult}, 但出于向后兼容性的原因, 这个类被保留了.
 *
 * <p>即使{@code StaxResult}继承自{@code SAXResult}, 调用{@code SAXResult}的方法<strong>也不支持</strong>.
 * 通常, 此类唯一支持的操作是使用通过{@link #getHandler()}获得的{@code ContentHandler}来使用{@code XMLReader}解析输入源.
 * 调用{@link #setHandler(org.xml.sax.ContentHandler)}
 * 或{@link #setLexicalHandler(org.xml.sax.ext.LexicalHandler)}将导致{@code UnsupportedOperationException}.
 */
class StaxResult extends SAXResult {

	private XMLEventWriter eventWriter;

	private XMLStreamWriter streamWriter;


	/**
	 * 使用指定的{@code XMLEventWriter}构造{@code StaxResult}的新实例.
	 * 
	 * @param eventWriter 要写入的{@code XMLEventWriter}
	 */
	public StaxResult(XMLEventWriter eventWriter) {
		StaxEventHandler handler = new StaxEventHandler(eventWriter);
		super.setHandler(handler);
		super.setLexicalHandler(handler);
		this.eventWriter = eventWriter;
	}

	/**
	 * 使用指定的{@code XMLStreamWriter}构造{@code StaxResult}的新实例.
	 * 
	 * @param streamWriter 要写入的{@code XMLStreamWriter}
	 */
	public StaxResult(XMLStreamWriter streamWriter) {
		StaxStreamHandler handler = new StaxStreamHandler(streamWriter);
		super.setHandler(handler);
		super.setLexicalHandler(handler);
		this.streamWriter = streamWriter;
	}


	/**
	 * 返回此{@code StaxResult}使用的{@code XMLEventWriter}.
	 * <p>如果{@code StaxResult}是使用{@code XMLStreamWriter}创建的, 则结果为{@code null}.
	 * 
	 * @return 此结果使用的StAX事件写入器
	 */
	public XMLEventWriter getXMLEventWriter() {
		return this.eventWriter;
	}

	/**
	 * 返回此{@code StaxResult}使用的{@code XMLStreamWriter}.
	 * <p>如果{@code StaxResult}是使用{@code XMLEventConsumer}创建的, 则结果为{@code null}.
	 * 
	 * @return 此结果使用的StAX流写入器
	 */
	public XMLStreamWriter getXMLStreamWriter() {
		return this.streamWriter;
	}


	/**
	 * 抛出{@code UnsupportedOperationException}.
	 * 
	 * @throws 总是UnsupportedOperationException
	 */
	@Override
	public void setHandler(ContentHandler handler) {
		throw new UnsupportedOperationException("setHandler is not supported");
	}

	/**
	 * 抛出{@code UnsupportedOperationException}.
	 * 
	 * @throws 总是UnsupportedOperationException
	 */
	@Override
	public void setLexicalHandler(LexicalHandler handler) {
		throw new UnsupportedOperationException("setLexicalHandler is not supported");
	}
}
