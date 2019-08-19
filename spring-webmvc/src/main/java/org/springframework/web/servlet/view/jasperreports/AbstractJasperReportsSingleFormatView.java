package org.springframework.web.servlet.view.jasperreports;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.ui.jasperreports.JasperReportsUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

/**
 * 扩展{@code AbstractJasperReportsView}, 为使用固定格式的视图提供基本的渲染逻辑, e.g. 始终是PDF或始终是HTML.
 *
 * <p>子类需要实现两个模板方法: {@code createExporter} 为特定输出格式创建JasperReports导出器,
 * 和{@code useWriter}来确定是写入文本还是二进制内容.
 *
 * <p><b>这个类与经典的JasperReports版本兼容, 直到2.x..</b>
 * 因此, 它继续使用{@link net.sf.jasperreports.engine.JRExporter} API, 该API自JasperReports 5.5.2 (2014年初)起已弃用.
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public abstract class AbstractJasperReportsSingleFormatView extends AbstractJasperReportsView {

	@Override
	protected boolean generatesDownloadContent() {
		return !useWriter();
	}

	/**
	 * 对单个Jasper Report导出器执行渲染, 即对于预定义的输出格式.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void renderReport(JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception {

		net.sf.jasperreports.engine.JRExporter exporter = createExporter();

		Map<net.sf.jasperreports.engine.JRExporterParameter, Object> mergedExporterParameters = getConvertedExporterParameters();
		if (!CollectionUtils.isEmpty(mergedExporterParameters)) {
			exporter.setParameters(mergedExporterParameters);
		}

		if (useWriter()) {
			renderReportUsingWriter(exporter, populatedReport, response);
		}
		else {
			renderReportUsingOutputStream(exporter, populatedReport, response);
		}
	}

	/**
	 * 需要将文本写入响应Writer.
	 * 
	 * @param exporter 要使用的JasperReport导出器
	 * @param populatedReport 要渲染的已填充的{@code JasperPrint}
	 * @param response 应该渲染报告的HTTP响应
	 * 
	 * @throws Exception 如果渲染失败
	 */
	protected void renderReportUsingWriter(net.sf.jasperreports.engine.JRExporter exporter,
			JasperPrint populatedReport, HttpServletResponse response) throws Exception {

		// 将为报告配置的编码复制到响应中.
		String contentType = getContentType();
		String encoding = (String) exporter.getParameter(net.sf.jasperreports.engine.JRExporterParameter.CHARACTER_ENCODING);
		if (encoding != null) {
			// 仅在指定了内容类型但不包含charset子句时才应用编码.
			if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
				contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
			}
		}
		response.setContentType(contentType);

		// 渲染报告到HttpServletResponse的 Writer.
		JasperReportsUtils.render(exporter, populatedReport, response.getWriter());
	}

	/**
	 * 需要将二进制输出写入响应OutputStream.
	 * 
	 * @param exporter 要使用的JasperReport导出器
	 * @param populatedReport 要渲染的已填充的{@code JasperPrint}
	 * @param response 应该渲染报告的HTTP响应
	 * 
	 * @throws Exception 如果渲染失败
	 */
	protected void renderReportUsingOutputStream(net.sf.jasperreports.engine.JRExporter exporter,
			JasperPrint populatedReport, HttpServletResponse response) throws Exception {

		// IE workaround: 首先写入字节数组.
		ByteArrayOutputStream baos = createTemporaryOutputStream();
		JasperReportsUtils.render(exporter, populatedReport, baos);
		writeToResponse(response, baos);
	}


	/**
	 * 为特定输出格式创建JasperReport导出器, 该输出格式将用于将报告渲染给HTTP响应.
	 * <p>{@code useWriter}方法确定输出是以文本还是以二进制内容形式写入.
	 */
	protected abstract net.sf.jasperreports.engine.JRExporter createExporter();

	/**
	 * 返回是否使用{@code java.io.Writer}将文本内容写入HTTP响应.
	 * 否则, 将使用{@code java.io.OutputStream}将二进制内容写入响应.
	 */
	protected abstract boolean useWriter();

}
