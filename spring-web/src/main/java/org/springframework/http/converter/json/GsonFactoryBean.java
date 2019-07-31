package org.springframework.http.converter.json;

import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 用于创建Google Gson 2.x {@link Gson}实例的{@link FactoryBean}.
 */
public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

	private boolean base64EncodeByteArrays = false;

	private boolean serializeNulls = false;

	private boolean prettyPrinting = false;

	private boolean disableHtmlEscaping = false;

	private String dateFormatPattern;

	private Gson gson;


	/**
	 * 在读取和写入JSON时是否Base64编码{@code byte[]}属性.
	 * <p>设置为{@code true}后, 将通过{@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}
	 * 注册自定义{@link com.google.gson.TypeAdapter}, 将{@code byte[]}属性序列化为Base64编码的String,
	 * 并将Base64编码的String反序列化为{@code byte[]}.
	 * <p><strong>NOTE:</strong> 使用此选项在Java 6或7上运行时在类路径上需要Apache Commons Codec库.
	 * 在Java 8上, 使用标准的{@link java.util.Base64}工具.
	 */
	public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
		this.base64EncodeByteArrays = base64EncodeByteArrays;
	}

	/**
	 * 在写入JSON时是否使用{@link GsonBuilder#serializeNulls()}选项.
	 * 这是设置{@code Gson}的快捷方式, 如下所示:
	 * <pre class="code">
	 * new GsonBuilder().serializeNulls().create();
	 * </pre>
	 */
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
	 * 在写入JSON时是否使用{@link GsonBuilder#setPrettyPrinting()}.
	 * 这是设置{@code Gson}的快捷方式, 如下所示:
	 * <pre class="code">
	 * new GsonBuilder().setPrettyPrinting().create();
	 * </pre>
	 */
	public void setPrettyPrinting(boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	/**
	 * 在写入JSON时是否使用{@link GsonBuilder#disableHtmlEscaping()}.
	 * 设置为{@code true}以禁用JSON中的HTML转义.
	 * 这是设置{@code Gson}的快捷方式, 如下所示:
	 * <pre class="code">
	 * new GsonBuilder().disableHtmlEscaping().create();
	 * </pre>
	 */
	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	/**
	 * 使用{@link SimpleDateFormat}样式模式定义日期/时间格式.
	 * 这是设置{@code Gson}的快捷方式, 如下所示:
	 * <pre class="code">
	 * new GsonBuilder().setDateFormat(dateFormatPattern).create();
	 * </pre>
	 */
	public void setDateFormatPattern(String dateFormatPattern) {
		this.dateFormatPattern = dateFormatPattern;
	}


	@Override
	public void afterPropertiesSet() {
		GsonBuilder builder = (this.base64EncodeByteArrays ?
				GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays() : new GsonBuilder());
		if (this.serializeNulls) {
			builder.serializeNulls();
		}
		if (this.prettyPrinting) {
			builder.setPrettyPrinting();
		}
		if (this.disableHtmlEscaping) {
			builder.disableHtmlEscaping();
		}
		if (this.dateFormatPattern != null) {
			builder.setDateFormat(this.dateFormatPattern);
		}
		this.gson = builder.create();
	}


	/**
	 * 返回创建的Gson实例.
	 */
	@Override
	public Gson getObject() {
		return this.gson;
	}

	@Override
	public Class<?> getObjectType() {
		return Gson.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
