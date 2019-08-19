package org.springframework.web.servlet.view.jasperreports;

import net.sf.jasperreports.engine.export.JRXlsExporter;

/**
 * {@code AbstractJasperReportsSingleFormatView}的实现, 以XLS格式呈现报告结果.
 *
 * <p><b>这个类与经典的JasperReports版本兼容, 直到2.x..</b>
 * 因此, 它继续使用{@link net.sf.jasperreports.engine.JRExporter} API, 该API自JasperReports 5.5.2 (2014年初)起已弃用.
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public class JasperReportsXlsView extends AbstractJasperReportsSingleFormatView {

	public JasperReportsXlsView() {
		setContentType("application/vnd.ms-excel");
	}

	@Override
	protected net.sf.jasperreports.engine.JRExporter createExporter() {
		return new JRXlsExporter();
	}

	@Override
	protected boolean useWriter() {
		return false;
	}

}
