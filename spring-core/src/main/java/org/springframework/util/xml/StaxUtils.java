package org.springframework.util.xml;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

/**
 * 使用StAX API的便捷方法.
 * 由于JAXP 1.3兼容性, 具有部分历史性; 从Spring 4.0开始, 依赖于JDK 1.6及更高版本中包含的JAXP 1.4.
 *
 * <p>特别是, 将StAX ({@code javax.xml.stream})与TrAX API ({@code javax.xml.transform})结合使用,
 * 并将StAX读取器/写入器转换为SAX读取器/处理器和副处理器.
 */
public abstract class StaxUtils {

	/**
	 * 为给定的{@link XMLStreamReader}创建JAXP 1.4 {@link StAXSource}.
	 * 
	 * @param streamReader StAX流读取器
	 * 
	 * @return 包装{@code streamReader}的源
	 */
	public static Source createStaxSource(XMLStreamReader streamReader) {
		return new StAXSource(streamReader);
	}

	/**
	 * 为给定的{@link XMLEventReader}创建JAXP 1.4 {@link StAXSource}.
	 * 
	 * @param eventReader StAX事件读取器
	 * 
	 * @return 包装{@code eventReader}的源
	 */
	public static Source createStaxSource(XMLEventReader eventReader) throws XMLStreamException {
		return new StAXSource(eventReader);
	}

	/**
	 * 为给定的{@link XMLStreamReader}创建自定义的非JAXP 1.4 StAX {@link Source}.
	 * 
	 * @param streamReader StAX流读取器
	 * 
	 * @return 包装{@code streamReader}的源
	 */
	public static Source createCustomStaxSource(XMLStreamReader streamReader) {
		return new StaxSource(streamReader);
	}

	/**
	 * 为给定的{@link XMLEventReader}创建自定义的非JAXP 1.4 StAX {@link Source}.
	 * 
	 * @param eventReader StAX事件读取器
	 * 
	 * @return 包装{@code eventReader}的源
	 */
	public static Source createCustomStaxSource(XMLEventReader eventReader) {
		return new StaxSource(eventReader);
	}

	/**
	 * 指示给定的{@link Source}是否是JAXP 1.4 StAX源或自定义StAX源.
	 * 
	 * @return {@code true} 如果{@code source}是JAXP 1.4 {@link StAXSource}或自定义StAX源; 否则{@code false}
	 */
	public static boolean isStaxSource(Source source) {
		return (source instanceof StAXSource || source instanceof StaxSource);
	}

	/**
	 * 返回给定StAX源的{@link XMLStreamReader}.
	 * 
	 * @param source JAXP 1.4 {@link StAXSource}
	 * 
	 * @return the {@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXSource}或自定义StAX源
	 */
	public static XMLStreamReader getXMLStreamReader(Source source) {
		if (source instanceof StAXSource) {
			return ((StAXSource) source).getXMLStreamReader();
		}
		else if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLStreamReader();
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * 返回给定StAX源的{@link XMLEventReader}.
	 * 
	 * @param source JAXP 1.4 {@link StAXSource}
	 * 
	 * @return the {@link XMLEventReader}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXSource}或自定义StAX源
	 */
	public static XMLEventReader getXMLEventReader(Source source) {
		if (source instanceof StAXSource) {
			return ((StAXSource) source).getXMLEventReader();
		}
		else if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLEventReader();
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * 为给定的{@link XMLStreamWriter}创建JAXP 1.4 {@link StAXResult}.
	 * 
	 * @param streamWriter StAX流写入器
	 * 
	 * @return 包装{@code streamWriter}的结果
	 */
	public static Result createStaxResult(XMLStreamWriter streamWriter) {
		return new StAXResult(streamWriter);
	}

	/**
	 * 为给定的{@link XMLEventWriter}创建一个JAXP 1.4 {@link StAXResult}.
	 * 
	 * @param eventWriter StAX事件写入器
	 * 
	 * @return 包装{@code streamReader}的结果
	 */
	public static Result createStaxResult(XMLEventWriter eventWriter) {
		return new StAXResult(eventWriter);
	}

	/**
	 * 为给定的{@link XMLStreamWriter}创建自定义的非JAXP 1.4 StAX {@link Result}.
	 * 
	 * @param streamWriter StAX流写入器
	 * 
	 * @return 包装{@code streamWriter}的源
	 */
	public static Result createCustomStaxResult(XMLStreamWriter streamWriter) {
		return new StaxResult(streamWriter);
	}

	/**
	 * 为给定的{@link XMLEventWriter}创建一个自定义的非JAXP 1.4 StAX {@link Result}.
	 * 
	 * @param eventWriter StAX事件写入器
	 * 
	 * @return 包装{@code eventWriter}的源
	 */
	public static Result createCustomStaxResult(XMLEventWriter eventWriter) {
		return new StaxResult(eventWriter);
	}

	/**
	 * 指示给定的{@link Result}是JAXP 1.4 StAX结果还是自定义StAX结果.
	 * 
	 * @return {@code true} 如果{@code result}是JAXP 1.4 {@link StAXResult}或自定义StAX结果; 否则{@code false}
	 */
	public static boolean isStaxResult(Result result) {
		return (result instanceof StAXResult || result instanceof StaxResult);
	}

	/**
	 * 返回给定StAX结果的{@link XMLStreamWriter}.
	 * 
	 * @param result JAXP 1.4 {@link StAXResult}
	 * 
	 * @return the {@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXResult}或自定义StAX结果
	 */
	public static XMLStreamWriter getXMLStreamWriter(Result result) {
		if (result instanceof StAXResult) {
			return ((StAXResult) result).getXMLStreamWriter();
		}
		else if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLStreamWriter();
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * 返回给定StAX Result的{@link XMLEventWriter}.
	 * 
	 * @param result a JAXP 1.4 {@link StAXResult}
	 * 
	 * @return the {@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果{@code source}不是JAXP 1.4 {@link StAXResult}或自定义StAX结果
	 */
	public static XMLEventWriter getXMLEventWriter(Result result) {
		if (result instanceof StAXResult) {
			return ((StAXResult) result).getXMLEventWriter();
		}
		else if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLEventWriter();
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * 创建一个SAX {@link ContentHandler}, 写入给定的StAX {@link XMLStreamWriter}.
	 * 
	 * @param streamWriter StAX流写入器
	 * 
	 * @return 写入{@code streamWriter}的内容处理器
	 */
	public static ContentHandler createContentHandler(XMLStreamWriter streamWriter) {
		return new StaxStreamHandler(streamWriter);
	}

	/**
	 * 创建一个SAX {@link ContentHandler}, 将事件写入给定的StAX {@link XMLEventWriter}.
	 * 
	 * @param eventWriter StAX事件写入器
	 * 
	 * @return 写入{@code eventWriter}的内容处理器
	 */
	public static ContentHandler createContentHandler(XMLEventWriter eventWriter) {
		return new StaxEventHandler(eventWriter);
	}

	/**
	 * 创建一个SAX {@link XMLReader}, 它从给定的StAX {@link XMLStreamReader}中读取.
	 * 
	 * @param streamReader StAX流读取器
	 * 
	 * @return 读取{@code streamWriter}的XMLReader
	 */
	public static XMLReader createXMLReader(XMLStreamReader streamReader) {
		return new StaxStreamXMLReader(streamReader);
	}

	/**
	 * 创建一个SAX {@link XMLReader}, 它从给定的StAX {@link XMLEventReader}中读取.
	 * 
	 * @param eventReader StAX事件读取器
	 * 
	 * @return 读取{@code eventWriter}的XMLReader
	 */
	public static XMLReader createXMLReader(XMLEventReader eventReader) {
		return new StaxEventXMLReader(eventReader);
	}

	/**
	 * 返回从{@link XMLEventReader}读取的{@link XMLStreamReader}.
	 * 有用, 因为StAX {@code XMLInputFactory}允许用户从流读取器创建事件读取器, 但反之亦然.
	 * 
	 * @return 从事件读取器读取的流读取器
	 */
	public static XMLStreamReader createEventStreamReader(XMLEventReader eventReader) throws XMLStreamException {
		return new XMLEventStreamReader(eventReader);
	}

	/**
	 * 返回写入{@link XMLEventWriter}的{@link XMLStreamWriter}.
	 * 
	 * @return 写入事件写入器的流写入器
	 */
	public static XMLStreamWriter createEventStreamWriter(XMLEventWriter eventWriter) {
		return new XMLEventStreamWriter(eventWriter, XMLEventFactory.newFactory());
	}

	/**
	 * 返回写入{@link XMLEventWriter}的{@link XMLStreamWriter}.
	 * 
	 * @return 写入事件写入器的流写入器
	 */
	public static XMLStreamWriter createEventStreamWriter(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		return new XMLEventStreamWriter(eventWriter, eventFactory);
	}
}
