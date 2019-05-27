package org.springframework.web.servlet.view.jasperreports;

import net.sf.jasperreports.engine.export.JRXlsExporter;

/**
 * Implementation of {@code AbstractJasperReportsSingleFormatView}
 * that renders report results in XLS format.
 *
 * <p><b>This class is compatible with classic JasperReports releases back until 2.x.</b>
 * As a consequence, it keeps using the {@link net.sf.jasperreports.engine.JRExporter}
 * API which got deprecated as of JasperReports 5.5.2 (early 2014).
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
