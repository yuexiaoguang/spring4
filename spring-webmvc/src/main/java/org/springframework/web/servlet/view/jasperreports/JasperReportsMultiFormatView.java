package org.springframework.web.servlet.view.jasperreports;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

/**
 * JasperReports视图类, 允许使用模型中包含的参数在运行时指定实际的呈现格式.
 *
 * <p>此视图适用于格式键和映射键的概念.
 * 格式键用于将映射键从{@code Controller}传递到Spring, 作为模型的一部分,
 * 映射键用于将逻辑格式映射到实际的JasperReports视图类.
 *
 * <p>例如, 可以将以下代码添加到{@code Controller}:
 *
 * <pre class="code">
 * Map<String, Object> model = new HashMap<String, Object>();
 * model.put("format", "pdf");</pre>
 *
 * 这里{@code format}是格式键, {@code pdf}是映射键.
 * 渲染报告时, 此类在格式键下查找模型参数, 默认情况下为{@code format}.
 * 然后, 它使用此参数的值来查找要使用的实际{@code View}类.
 *
 * <p>格式查找的默认映射是:
 *
 * <p><ul>
 * <li>{@code csv} - {@code JasperReportsCsvView}</li>
 * <li>{@code html} - {@code JasperReportsHtmlView}</li>
 * <li>{@code pdf} - {@code JasperReportsPdfView}</li>
 * <li>{@code xls} - {@code JasperReportsXlsView}</li>
 * <li>{@code xlsx} - {@code JasperReportsXlsxView}</li> (as of Spring 4.2)
 * </ul>
 *
 * <p>可以使用{@code formatKey}属性更改格式键.
 * 可以使用{@code formatMappings}属性配置适用的key-to-view-class映射.
 */
public class JasperReportsMultiFormatView extends AbstractJasperReportsView {

	/**
	 * 用于格式键的默认值: "format"
	 */
	public static final String DEFAULT_FORMAT_KEY = "format";


	/**
	 * 保存格式键的模型参数的键.
	 */
	private String formatKey = DEFAULT_FORMAT_KEY;

	/**
	 * 存储格式映射, 格式鉴别符为键, 相应的视图类为值.
	 */
	private Map<String, Class<? extends AbstractJasperReportsView>> formatMappings;

	/**
	 * 存储映射键到Content-Disposition header值的映射.
	 */
	private Properties contentDispositionMappings;


  /**
   * 使用一组默认映射.
   */
	public JasperReportsMultiFormatView() {
		this.formatMappings = new HashMap<String, Class<? extends AbstractJasperReportsView>>(4);
		this.formatMappings.put("csv", JasperReportsCsvView.class);
		this.formatMappings.put("html", JasperReportsHtmlView.class);
		this.formatMappings.put("pdf", JasperReportsPdfView.class);
		this.formatMappings.put("xls", JasperReportsXlsView.class);
		this.formatMappings.put("xlsx", JasperReportsXlsxView.class);
	}


	/**
	 * 设置保存格式鉴别符的模型参数的键.
	 * 默认为 "format".
	 */
	public void setFormatKey(String formatKey) {
		this.formatKey = formatKey;
	}

	/**
	 * 设置格式鉴别符到视图类名的映射.
	 * 默认映射为:
	 * <p><ul>
	 * <li>{@code csv} - {@code JasperReportsCsvView}</li>
	 * <li>{@code html} - {@code JasperReportsHtmlView}</li>
	 * <li>{@code pdf} - {@code JasperReportsPdfView}</li>
	 * <li>{@code xls} - {@code JasperReportsXlsView}</li>
	 * <li>{@code xlsx} - {@code JasperReportsXlsxView}</li> (as of Spring 4.2)
	 * </ul>
	 */
	public void setFormatMappings(Map<String, Class<? extends AbstractJasperReportsView>> formatMappings) {
		if (CollectionUtils.isEmpty(formatMappings)) {
			throw new IllegalArgumentException("'formatMappings' must not be empty");
		}
		this.formatMappings = formatMappings;
	}

	/**
	 * 设置{@code Content-Disposition} header值到映射键的映射.
	 * 如果指定, Spring将查看这些映射以确定给定格式映射的{@code Content-Disposition} header的值.
	 */
	public void setContentDispositionMappings(Properties mappings) {
		this.contentDispositionMappings = mappings;
	}

	/**
	 * 返回{@code Content-Disposition} header值到映射键的映射.
	 * 主要用于通过指定单个键的属性路径进行配置.
	 */
	public Properties getContentDispositionMappings() {
		if (this.contentDispositionMappings == null) {
			this.contentDispositionMappings = new Properties();
		}
		return this.contentDispositionMappings;
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	/**
	 * 使用配置的鉴别符键在模型中找到格式键, 并使用此键从映射中查找相应的视图类.
	 * 然后, 将报告的渲染委托给该视图类的实例.
	 */
	@Override
	protected void renderReport(JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception {

		String format = (String) model.get(this.formatKey);
		if (format == null) {
			throw new IllegalArgumentException("No format found in model");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering report using format mapping key [" + format + "]");
		}

		Class<? extends AbstractJasperReportsView> viewClass = this.formatMappings.get(format);
		if (viewClass == null) {
			throw new IllegalArgumentException("Format discriminator [" + format + "] is not a configured mapping");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Rendering report using view class [" + viewClass.getName() + "]");
		}

		AbstractJasperReportsView view = BeanUtils.instantiateClass(viewClass);
		// 可以跳过大多数初始化, 因为所有相关的URL处理都已完成 - 只需要在子视图上转换参数.
		view.setExporterParameters(getExporterParameters());
		view.setConvertedExporterParameters(getConvertedExporterParameters());

		// 准备响应并渲染报告.
		populateContentDispositionIfNecessary(response, format);
		view.renderReport(populatedReport, model, response);
	}

	/**
	 * 如果已指定映射并且给定格式存在有效映射，则使用特定于格式的值添加/覆盖{@code Content-Disposition} header值.
	 * 
	 * @param response 要设置header的{@code HttpServletResponse}
	 * @param format 映射的格式键
	 */
	private void populateContentDispositionIfNecessary(HttpServletResponse response, String format) {
		if (this.contentDispositionMappings != null) {
			String header = this.contentDispositionMappings.getProperty(format);
			if (header != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting Content-Disposition header to: [" + header + "]");
				}
				response.setHeader(HEADER_CONTENT_DISPOSITION, header);
			}
		}
	}
}
