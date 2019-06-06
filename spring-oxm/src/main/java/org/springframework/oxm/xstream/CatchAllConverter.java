package org.springframework.oxm.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * 支持所有类的XStream {@link Converter}, 但抛出编组/解组的异常.
 *
 * <p>这个类的主要目的是
 * {@linkplain com.thoughtworks.xstream.XStream#registerConverter(com.thoughtworks.xstream.converters.Converter, int) register}
 * 此转换器为具有
 * {@linkplain com.thoughtworks.xstream.XStream#PRIORITY_NORMAL 普通} 或更高优先级的全能最后转换器,
 * 以及显式处理应支持的域类的转换器.
 * 因此, 不会调用具有较低优先级和可能的安全漏洞的默认XStream转换器.
 *
 * <p>例如:
 * <pre class="code">
 * XStreamMarshaller unmarshaller = new XStreamMarshaller();
 * unmarshaller.getXStream().registerConverter(new MyDomainClassConverter(), XStream.PRIORITY_VERY_HIGH);
 * unmarshaller.getXStream().registerConverter(new CatchAllConverter(), XStream.PRIORITY_NORMAL);
 * MyDomainClass myObject = unmarshaller.unmarshal(source);
 * </pre>
 */
public class CatchAllConverter implements Converter {

	@Override
	@SuppressWarnings("rawtypes")
	public boolean canConvert(Class type) {
		return true;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		throw new UnsupportedOperationException("Marshalling not supported");
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		throw new UnsupportedOperationException("Unmarshalling not supported");
	}

}
