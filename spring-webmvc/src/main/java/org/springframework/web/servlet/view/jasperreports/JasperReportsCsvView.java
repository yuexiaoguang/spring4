package org.springframework.web.servlet.view.jasperreports;

import net.sf.jasperreports.engine.export.JRCsvExporter;

/**
 * Implementation of {@code AbstractJasperReportsSingleFormatView}
 * that renders report results in CSV format.
 *
 * <p><b>This class is compatible with classic JasperReports releases back until 2.x.</b>
 * As a consequence, it keeps using the {@link net.sf.jasperreports.engine.JRExporter}
 * API which got deprecated as of JasperReports 5.5.2 (early 2014).
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public class JasperReportsCsvView extends AbstractJasperReportsSingleFormatView {

	public JasperReportsCsvView() {
		setContentType("text/csv");
	}

	@Override
	protected net.sf.jasperreports.engine.JRExporter createExporter() {
		return new JRCsvExporter();
	}

	@Override
	protected boolean useWriter() {
		return true;
	}

}
