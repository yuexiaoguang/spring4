package org.springframework.web.servlet.view.jasperreports;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver}实现,
 * 通过将提供的视图名称转换为报告文件的URL来解析{@link AbstractJasperReportsView}的实例.
 */
public class JasperReportsViewResolver extends UrlBasedViewResolver {

	private String reportDataKey;

	private Properties subReportUrls;

	private String[] subReportDataKeys;

	private Properties headers;

	private Map<String, Object> exporterParameters = new HashMap<String, Object>();

	private DataSource jdbcDataSource;


	/**
	 * 要求视图类是{@link AbstractJasperReportsView}的子类.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return AbstractJasperReportsView.class;
	}

	/**
	 * 设置视图类应使用的{@code reportDataKey}.
	 */
	public void setReportDataKey(String reportDataKey) {
		this.reportDataKey = reportDataKey;
	}

	/**
	 * 设置视图类应该使用的{@code subReportUrls}.
	 */
	public void setSubReportUrls(Properties subReportUrls) {
		this.subReportUrls = subReportUrls;
	}

	/**
	 * 设置视图类应该使用的{@code subReportDataKeys}.
	 */
	public void setSubReportDataKeys(String... subReportDataKeys) {
		this.subReportDataKeys = subReportDataKeys;
	}

	/**
	 * 设置视图类应使用的{@code headers}.
	 */
	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * 设置视图类应使用的{@code exporterParameters}.
	 */
	public void setExporterParameters(Map<String, Object> exporterParameters) {
		this.exporterParameters = exporterParameters;
	}

	/**
	 * 设置视图类应该使用的{@link DataSource}.
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
