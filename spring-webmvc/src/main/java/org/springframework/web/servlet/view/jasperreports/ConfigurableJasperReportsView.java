package org.springframework.web.servlet.view.jasperreports;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * 可配置的JasperReports View, 允许指定要通过bean属性而不是通过视图类名指定的JasperReports导出器.
 *
 * <p><b>这个类与经典的JasperReports版本兼容, 直到2.x..</b>
 * 因此, 它继续使用{@link net.sf.jasperreports.engine.JRExporter} API, 该API自JasperReports 5.5.2 (2014年初)起已弃用.
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public class ConfigurableJasperReportsView extends AbstractJasperReportsSingleFormatView {

	private Class<? extends net.sf.jasperreports.engine.JRExporter> exporterClass;

	private boolean useWriter = true;


	/**
	 * 设置要使用的{@code JRExporter}实现{@code Class}.
	 * 如果{@code Class}没有实现{@code JRExporter}, 则抛出{@link IllegalArgumentException}.
	 * 必需的设置, 因为它没有默认值.
	 */
	public void setExporterClass(Class<? extends net.sf.jasperreports.engine.JRExporter> exporterClass) {
		Assert.isAssignable(net.sf.jasperreports.engine.JRExporter.class, exporterClass);
		this.exporterClass = exporterClass;
	}

	/**
	 * 指定{@code JRExporter}是否写入与请求关联的{@link java.io.PrintWriter} ({@code true}),
	 * 或者是否直接写入请求的{@link java.io.InputStream} ({@code false}).
	 * 默认为{@code true}.
	 */
	public void setUseWriter(boolean useWriter) {
		this.useWriter = useWriter;
	}

	/**
	 * 检查是否指定了{@link #setExporterClass(Class) exporterClass}属性.
	 */
	@Override
	protected void onInit() {
		if (this.exporterClass == null) {
			throw new IllegalArgumentException("exporterClass is required");
		}
	}


	/**
	 * 返回指定的{@link net.sf.jasperreports.engine.JRExporter}类的新实例.
	 */
	@Override
	protected net.sf.jasperreports.engine.JRExporter createExporter() {
		return BeanUtils.instantiateClass(this.exporterClass);
	}

	/**
	 * 指示{@code JRExporter}应如何呈现其数据.
	 */
	@Override
	protected boolean useWriter() {
		return this.useWriter;
	}

}
