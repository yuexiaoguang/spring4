package org.springframework.messaging.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 基于Jackson 2的{@link MessageConverter}实现.
 *
 * <p>它通过以下方式定制Jackson的默认属性:
 * <ul>
 * <li>禁用{@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class MappingJackson2MessageConverter extends AbstractMessageConverter {

	private ObjectMapper objectMapper;

	private Boolean prettyPrint;


	/**
	 * 支持{@code application/json} MIME类型, 使用{@code UTF-8}字符集.
	 */
	public MappingJackson2MessageConverter() {
		super(new MimeType("application", "json", Charset.forName("UTF-8")));
		initObjectMapper();
	}

	/**
	 * 支持一个或多个MIME类型.
	 * 
	 * @param supportedMimeTypes 支持的MIME类型
	 */
	public MappingJackson2MessageConverter(MimeType... supportedMimeTypes) {
		super(Arrays.asList(supportedMimeTypes));
		initObjectMapper();
	}


	private void initObjectMapper() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * 设置此转换器的{@code ObjectMapper}.
	 * 如果未设置, 则使用默认的{@link ObjectMapper#ObjectMapper() ObjectMapper}.
	 * <p>设置自定义配置的{@code ObjectMapper}是进一步控制JSON序列化过程的一种方法.
	 * 例如, 可以配置扩展的{@link com.fasterxml.jackson.databind.ser.SerializerFactory}, 为特定类型提供自定义序列化器.
	 * 改进序列化过程的另一个选项是使用Jackson提供的序列化类型的注解, 在这种情况下, 不需要自定义配置的ObjectMapper.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * 返回此转换器的底层{@code ObjectMapper}.
	 */
	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * 在编写JSON时是否使用{@link DefaultPrettyPrinter}.
	 * 这是设置{@code ObjectMapper}的快捷方式, 如下所示:
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * converter.setObjectMapper(mapper);
	 * </pre>
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}

	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		if (targetClass == null || !supportsMimeType(message.getHeaders())) {
			return false;
		}
		JavaType javaType = this.objectMapper.constructType(targetClass);
		AtomicReference<Throwable> causeRef = new AtomicReference<Throwable>();
		if (this.objectMapper.canDeserialize(javaType, causeRef)) {
			return true;
		}
		logWarningIfNecessary(javaType, causeRef.get());
		return false;
	}

	@Override
	protected boolean canConvertTo(Object payload, MessageHeaders headers) {
		if (payload == null || !supportsMimeType(headers)) {
			return false;
		}
		AtomicReference<Throwable> causeRef = new AtomicReference<Throwable>();
		if (this.objectMapper.canSerialize(payload.getClass(), causeRef)) {
			return true;
		}
		logWarningIfNecessary(payload.getClass(), causeRef.get());
		return false;
	}

	/**
	 * 确定是否记录来自{@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize}检查的给定异常.
	 * 
	 * @param type Jackson (反)序列化测试的类
	 * @param cause 要评估的Jackson抛出的异常 (通常是{@link JsonMappingException})
	 */
	protected void logWarningIfNecessary(Type type, Throwable cause) {
		if (cause == null) {
			return;
		}

		// 不记录未找到序列化器的警告 (note: Jackson 2.9上的不同消息措辞)
		boolean debugLevel = (cause instanceof JsonMappingException &&
				(cause.getMessage().startsWith("Can not find") || cause.getMessage().startsWith("Cannot find")));

		if (debugLevel ? logger.isDebugEnabled() : logger.isWarnEnabled()) {
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			if (debugLevel) {
				logger.debug(msg, cause);
			}
			else if (logger.isDebugEnabled()) {
				logger.warn(msg, cause);
			}
			else {
				logger.warn(msg + ": " + cause);
			}
		}
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// 不应该调用, 因为覆盖了canConvertFrom/canConvertTo
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		JavaType javaType = getJavaType(targetClass, conversionHint);
		Object payload = message.getPayload();
		Class<?> view = getSerializationView(conversionHint);
		// Note: 在视图的情况下, 调用withType而不是forType以与Jackson <2.5兼容
		try {
			if (payload instanceof byte[]) {
				if (view != null) {
					return this.objectMapper.readerWithView(view).forType(javaType).readValue((byte[]) payload);
				}
				else {
					return this.objectMapper.readValue((byte[]) payload, javaType);
				}
			}
			else {
				if (view != null) {
					return this.objectMapper.readerWithView(view).forType(javaType).readValue(payload.toString());
				}
				else {
					return this.objectMapper.readValue(payload.toString(), javaType);
				}
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	private JavaType getJavaType(Class<?> targetClass, Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			param = param.nestedIfOptional();
			if (Message.class.isAssignableFrom(param.getParameterType())) {
				param = param.nested();
			}
			Type genericParameterType = param.getNestedGenericParameterType();
			Class<?> contextClass = param.getContainingClass();
			Type type = getJavaType(genericParameterType, contextClass);
			return this.objectMapper.getTypeFactory().constructType(type);
		}
		return this.objectMapper.constructType(targetClass);
	}

	private JavaType getJavaType(Type type, Class<?> contextClass) {
		TypeFactory typeFactory = this.objectMapper.getTypeFactory();
		if (contextClass != null) {
			ResolvableType resolvedType = ResolvableType.forType(type);
			if (type instanceof TypeVariable) {
				ResolvableType resolvedTypeVariable = resolveVariable(
						(TypeVariable<?>) type, ResolvableType.forClass(contextClass));
				if (resolvedTypeVariable != ResolvableType.NONE) {
					return typeFactory.constructType(resolvedTypeVariable.resolve());
				}
			}
			else if (type instanceof ParameterizedType && resolvedType.hasUnresolvableGenerics()) {
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				for (int i = 0; i < typeArguments.length; i++) {
					Type typeArgument = typeArguments[i];
					if (typeArgument instanceof TypeVariable) {
						ResolvableType resolvedTypeArgument = resolveVariable(
								(TypeVariable<?>) typeArgument, ResolvableType.forClass(contextClass));
						if (resolvedTypeArgument != ResolvableType.NONE) {
							generics[i] = resolvedTypeArgument.resolve();
						}
						else {
							generics[i] = ResolvableType.forType(typeArgument).resolve();
						}
					}
					else {
						generics[i] = ResolvableType.forType(typeArgument).resolve();
					}
				}
				return typeFactory.constructType(ResolvableType.
						forClassWithGenerics(resolvedType.getRawClass(), generics).getType());
			}
		}
		return typeFactory.constructType(type);
	}

	private ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
		ResolvableType resolvedType;
		if (contextType.hasGenerics()) {
			resolvedType = ResolvableType.forType(typeVariable, contextType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}

		ResolvableType superType = contextType.getSuperType();
		if (superType != ResolvableType.NONE) {
			resolvedType = resolveVariable(typeVariable, superType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		for (ResolvableType ifc : contextType.getInterfaces()) {
			resolvedType = resolveVariable(typeVariable, ifc);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		return ResolvableType.NONE;
	}

	@Override
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		try {
			Class<?> view = getSerializationView(conversionHint);
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				JsonEncoding encoding = getJsonEncoding(getMimeType(headers));
				JsonGenerator generator = this.objectMapper.getFactory().createGenerator(out, encoding);
				if (view != null) {
					this.objectMapper.writerWithView(view).writeValue(generator, payload);
				}
				else {
					this.objectMapper.writeValue(generator, payload);
				}
				payload = out.toByteArray();
			}
			else {
				Writer writer = new StringWriter();
				if (view != null) {
					this.objectMapper.writerWithView(view).writeValue(writer, payload);
				}
				else {
					this.objectMapper.writeValue(writer, payload);
				}
				payload = writer.toString();
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return payload;
	}

	/**
	 * 根据给定的转换提示确定Jackson序列化视图.
	 * 
	 * @param conversionHint 传入转换器以进行当前转换尝试的转换提示对象
	 * 
	 * @return 序列化视图类, 或{@code null}
	 */
	protected Class<?> getSerializationView(Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			JsonView annotation = (param.getParameterIndex() >= 0 ?
					param.getParameterAnnotation(JsonView.class) : param.getMethodAnnotation(JsonView.class));
			if (annotation != null) {
				return extractViewClass(annotation, conversionHint);
			}
		}
		else if (conversionHint instanceof JsonView) {
			return extractViewClass((JsonView) conversionHint, conversionHint);
		}
		else if (conversionHint instanceof Class) {
			return (Class<?>) conversionHint;
		}

		// 未指定JSON视图...
		return null;
	}

	private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
		}
		return classes[0];
	}

	/**
	 * 确定要用于给定内容类型的JSON编码.
	 * 
	 * @param contentType MessageHeader中的MIME类型
	 * 
	 * @return 要使用的JSON编码 (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(MimeType contentType) {
		if ((contentType != null) && (contentType.getCharset() != null)) {
			Charset charset = contentType.getCharset();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}
}
