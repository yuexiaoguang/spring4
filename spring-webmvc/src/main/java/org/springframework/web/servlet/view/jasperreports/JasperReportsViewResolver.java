package org.springframework.web.servlet.view.jasperreports;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver} implementation that
 * resolves instances of {@link AbstractJasperReportsView} by translating
 * the supplied view name into the URL of the report file.
 */
public class JasperReportsViewResolver extends UrlBasedViewResolver {

	private String reportDataKey;

	private Properties subReportUrls;

	private String[] subReportDataKeys;

	private Properties headers;

	private Map<String, Object> exporterParameters = new HashMap<String, Object>();

	private DataSource jdbcDataSource;


	/**
	 * Requires the view class to be a subclass of {@link AbstractJasperReportsView}.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return AbstractJasperReportsView.class;
	}

	/**
	 * Set the {@code reportDataKey} the view class should use.
	 * @see AbstractJasperReportsView#setReportDataKey
	 */
	public void setReportDataKey(String reportDataKey) {
		this.reportDataKey = reportDataKey;
	}

	/**
	 * Set the {@code subReportUrls} the view class should use.
	 * @see AbstractJasperReportsView#setSubReportUrls
	 */
	public void setSubReportUrls(Properties subReportUrls) {
		this.subReportUrls = subReportUrls;
	}

	/**
	 * Set the {@code subReportDataKeys} the view class should use.
	 * @see AbstractJasperReportsView#setSubReportDataKeys
	 */
	public void setSubReportDataKeys(String... subReportDataKeys) {
		this.subReportDataKeys = subReportDataKeys;
	}

	/**
	 * Set the {@code headers} the view class should use.
	 * @see AbstractJasperReportsView#setHeaders
	 */
	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * Set the {@code exporterParameters} the view class should use.
	 * @see AbstractJasperReportsView#setExporterParameters
	 */
	public void setExporterParameters(Map<String, Object> exporterParameters) {
		this.exporterParameters = exporterParameters;
	}

	/**
	 * Set the {@link DataSource} the view class should use.
	 * @see AbstractJasperReportsView#setJdbcDataSource
	 */
	public void setJdbcDataSource(DataSource jdbcDataSource) {
		this.jdbcDataSource = jdbcDataSource;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractJasperReportsView view = (AbstractJasperReportsView) super.buildView(viewName);
		view.setReportDataKey(this.reportDataKey);
		view.setSubReportUrls(this.subReportUrls);
		view.setSubReportDataKeys(this.subReportDataKeys);
		view.setHeaders(this.headers);
		view.setExporterParameters(this.exporterParameters);
		view.setJdbcDataSource(this.jdbcDataSource);
		return view;
	}

}
