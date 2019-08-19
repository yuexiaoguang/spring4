package org.springframework.web.servlet.view.jasperreports;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.core.io.Resource;
import org.springframework.ui.jasperreports.JasperReportsUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * 所有JasperReports视图的基类.
 * 根据需要应用报告设计的动态编译并协调渲染过程.
 * 主报告的资源路径需要指定为{@code url}.
 *
 * <p>此类负责从提供给视图的模型中获取报表数据.
 * 默认实现首先检查指定的{@code reportDataKey}下的模型对象, 然后回退到查找类型{@code JRDataSource},
 * {@code java.util.Collection}, 对象数组的值 (按此顺序).
 *
 * <p>如果在模型中找不到{@code JRDataSource}, 那么将使用配置的{@code javax.sql.DataSource}填写报告.
 * 如果{@code JRDataSource}或{@code javax.sql.DataSource}都不可用, 则会引发{@code IllegalArgumentException}.
 *
 * <p>通过{@code subReportUrls}和{@code subReportDataKeys}属性为子报告提供支持.
 *
 * <p>使用子报告时, 应使用{@code url}属性配置主报告, 并使用{@code subReportUrls}属性配置子报告文件.
 * {@code subReportUrls} Map中的每个条目对应一个单独的子报告.
 * 条目的键必须与{@code net.sf.jasperreports.engine.JasperReport}类型的报告文件中的子报告参数匹配,
 * 并且条目的值必须是子报告文件的URL.
 *
 * <p>对于需要{@code JRDataSource}实例的子报告, 也就是说, 它们没有用于数据检索的硬编码查询,
 * 可以在模型中包含适当的数据, 就像使用父级报告的数据源一样.
 * 但是, 必须通过{@code subReportDataKeys}属性提供需要转换为子报告的{@code JRDataSource}实例的参数名称列表.
 * 使用{@code JRDataSource}实例进行子报告时, <i>必须</i>为{@code reportDataKey}属性指定一个值, 指示用于主报告的数据.
 *
 * <p>允许使用{@code exporterParameters}属性以声明方式配置导出器参数.
 * 这是一个{@code Map}类型属性, 其中条目的键对应于{@code JRExporterParameter}的静态字段的完全限定名称,
 * 条目的值是分配给导出器参数的值.
 *
 * <p>响应header可以通过{@code headers}属性进行控制.
 * Spring将尝试为{@code Content-Diposition} header设置正确的值, 以便在Internet Explorer中正确呈现报告.
 * 但是, 可以通过{@code headers}属性覆盖此设置.
 *
 * <p><b>这个类与经典的JasperReports版本兼容, 直到2.x..</b>
 * 因此, 它继续使用{@link net.sf.jasperreports.engine.JRExporter} API, 该API自JasperReports 5.5.2 (2014年初)起已弃用.
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public abstract class AbstractJasperReportsView extends AbstractUrlBasedView {

	/**
	 * 定义"Content-Disposition" header的常量.
	 */
	protected static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * 默认的Content-Disposition header. 用于IE.
	 */
	protected static final String CONTENT_DISPOSITION_INLINE = "inline";


	/**
	 * 用于在模型中查找{@code JRDataSource}的String键.
	 */
	private String reportDataKey;

	/**
	 * 存储此顶级报告使用的任何子报告文件的路径, 以及它们在顶级报告文件中映射到的键.
	 */
	private Properties subReportUrls;

	/**
	 * 存储需要转换为{@code JRDataSource}实例并包含在要传递到子报告的报告参数中的任何数据源对象的名称.
	 */
	private String[] subReportDataKeys;

	/**
	 * 存储与每个响应一起写入的header
	 */
	private Properties headers;

	/**
	 * 存储用户传入的导出器参数.
	 * 可以使用导出器参数字段的完全限定名称作为键.
	 */
	private Map<?, ?> exporterParameters = new HashMap<Object, Object>();

	/**
	 * 存储转换后的导出器参数 - 由{@code JRExporterParameter}作为键.
	 */
	private Map<net.sf.jasperreports.engine.JRExporterParameter, Object> convertedExporterParameters;

	/**
	 * 存储用作报告数据源的{@code DataSource}.
	 */
	private DataSource jdbcDataSource;

	/**
	 * 用于呈现视图的{@code JasperReport}.
	 */
	private JasperReport report;

	/**
	 * 保存子报告键和{@code JasperReport}对象之间的映射.
	 */
	private Map<String, JasperReport> subReports;


	/**
	 * 设置表示报告数据的模型属性的名称.
	 * 如果未指定, 将搜索模型Map以查找匹配的值类型.
	 * <p>{@code JRDataSource}将按原样使用. 对于其他类型, 转换将适用:
	 * 默认情况下, {@code java.util.Collection}将转换为{@code JRBeanCollectionDataSource},
	 * 并将对象数组转换为{@code JRBeanArrayDataSource}.
	 * <p><b>Note:</b> 如果在模型Map中传入Collection或object数组以用作普通报告参数,
	 * 而不是作为报告数据从中提取字段, 则需要指定要使用的实际报告数据的键, 以避免按类型检测不到报告数据.
	 */
	public void setReportDataKey(String reportDataKey) {
		this.reportDataKey = reportDataKey;
	}

	/**
	 * 指定必须作为{@code JasperReport}实例加载的资源路径,
	 * 并传递给JasperReports引擎, 以便在与此映射中相同的键下呈现为子报告.
	 * 
	 * @param subReports 模型键和资源路径 (Spring资源位置)之间的映射
	 */
	public void setSubReportUrls(Properties subReports) {
		this.subReportUrls = subReports;
	}

	/**
	 * 设置与包含数据源对象的模型参数对应的名称列表, 以便在子报告中使用.
	 * Spring会将这些对象转换为适用的{@code JRDataSource}实例,
	 * 然后在传递给JasperReports引擎的参数中包含生成的{@code JRDataSource}.
	 * <p>列表中指定的名称应对应于模型Map中的属性, 以及报告文件中的子报告数据源参数.
	 * 如果将{@code JRDataSource}对象作为模型属性传递, 则不需要指定此键列表.
	 * <p>如果指定子报告数据键列表, 则还需要为主报告指定{@code reportDataKey}, 以避免涉及各种报告的数据源对象之间的混淆.
	 * 
	 * @param subReportDataKeys 子报告数据源对象的名称列表
	 */
	public void setSubReportDataKeys(String... subReportDataKeys) {
		this.subReportDataKeys = subReportDataKeys;
	}

	/**
	 * 指定每个响应中包含的header集.
	 * 
	 * @param headers 要写入每个响应的header
	 */
	public void setHeaders(Properties headers) {
		this.headers = headers;
	}

	/**
	 * 设置渲染视图时应使用的导出器参数.
	 * 
	 * @param parameters 使用{@code JRExporterParameter}实例的完全限定字段名作为键的{@code Map}
	 * (e.g. "net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI")
	 * 以及分配给参数的值作为值
	 */
	public void setExporterParameters(Map<?, ?> parameters) {
		this.exporterParameters = parameters;
	}

	/**
	 * 返回此视图使用的导出器参数.
	 */
	public Map<?, ?> getExporterParameters() {
		return this.exporterParameters;
	}

	/**
	 * 允许子类填充转换后的导出器参数.
	 */
	protected void setConvertedExporterParameters(Map<net.sf.jasperreports.engine.JRExporterParameter, Object> parameters) {
		this.convertedExporterParameters = parameters;
	}

	/**
	 * 允许子类检索已转换的导出器参数.
	 */
	protected Map<net.sf.jasperreports.engine.JRExporterParameter, Object> getConvertedExporterParameters() {
		return this.convertedExporterParameters;
	}

	/**
	 * 指定用于具有嵌入式SQL语句的报告的{@code javax.sql.DataSource}.
	 */
	public void setJdbcDataSource(DataSource jdbcDataSource) {
		this.jdbcDataSource = jdbcDataSource;
	}

	/**
	 * 返回此视图使用的{@code javax.sql.DataSource}.
	 */
	protected DataSource getJdbcDataSource() {
		return this.jdbcDataSource;
	}


	/**
	 * JasperReports视图并不严格要求 'url'值.
	 * 或者, 可以覆盖{@link #getReport()}模板方法.
	 */
	@Override
	protected boolean isUrlRequired() {
		return false;
	}

	/**
	 * 检查配置中是否提供了有效的报告文件URL. 编译报告文件是必要的.
	 * <p>子类可以通过覆盖{@link #onInit}方法来添加自定义初始化逻辑.
	 */
	@Override
	protected final void initApplicationContext() throws ApplicationContextException {
		this.report = loadReport();

		// 如果需要, 加载子报告, 并检查数据源参数.
		if (this.subReportUrls != null) {
			if (this.subReportDataKeys != null && this.subReportDataKeys.length > 0 && this.reportDataKey == null) {
				throw new ApplicationContextException(
						"'reportDataKey' for main report is required when specifying a value for 'subReportDataKeys'");
			}
			this.subReports = new HashMap<String, JasperReport>(this.subReportUrls.size());
			for (Enumeration<?> urls = this.subReportUrls.propertyNames(); urls.hasMoreElements();) {
				String key = (String) urls.nextElement();
				String path = this.subReportUrls.getProperty(key);
				Resource resource = getApplicationContext().getResource(path);
				this.subReports.put(key, loadReport(resource));
			}
		}

		// 转换用户提供的exporterParameters.
		convertExporterParameters();

		if (this.headers == null) {
			this.headers = new Properties();
		}
		if (!this.headers.containsKey(HEADER_CONTENT_DISPOSITION)) {
			this.headers.setProperty(HEADER_CONTENT_DISPOSITION, CONTENT_DISPOSITION_INLINE);
		}

		onInit();
	}

	/**
	 * 子类可以覆盖它以添加一些自定义初始化逻辑.
	 * 所有标准初始化逻辑完成后, 由{{@link #initApplicationContext()}调用.
	 */
	protected void onInit() {
	}

	/**
	 * 将用户传入的由{@code JRExporterParameter}的完全限定名称对应的{@code String}作为键的导出器参数转换为
	 * 由{@code JRExporterParameter}作为键的参数.
	 */
	protected final void convertExporterParameters() {
		if (!CollectionUtils.isEmpty(this.exporterParameters)) {
			this.convertedExporterParameters =
					new HashMap<net.sf.jasperreports.engine.JRExporterParameter, Object>(this.exporterParameters.size());
			for (Map.Entry<?, ?> entry : this.exporterParameters.entrySet()) {
				net.sf.jasperreports.engine.JRExporterParameter exporterParameter = getExporterParameter(entry.getKey());
				this.convertedExporterParameters.put(
						exporterParameter, convertParameterValue(exporterParameter, entry.getValue()));
			}
		}
	}

	/**
	 * 将提供的参数值转换为相应{@code JRExporterParameter}所需的实际类型.
	 * <p>默认实现只是将String值"true" 和 "false"转换为相应的{@code Boolean}对象,
	 * 并尝试将以数字开头的String值转换为{@code Integer}对象 (如果转换为数字失败, 只需将它们保存为String).
	 * 
	 * @param parameter 参数键
	 * @param value 参数值
	 * 
	 * @return 转换后的参数值
	 */
	protected Object convertParameterValue(net.sf.jasperreports.engine.JRExporterParameter parameter, Object value) {
		if (value instanceof String) {
			String str = (String) value;
			if ("true".equals(str)) {
				return Boolean.TRUE;
			}
			else if ("false".equals(str)) {
				return Boolean.FALSE;
			}
			else if (str.length() > 0 && Character.isDigit(str.charAt(0))) {
				// Looks like a number... let's try.
				try {
					return Integer.valueOf(str);
				}
				catch (NumberFormatException ex) {
					// OK, then let's keep it as a String value.
					return str;
				}
			}
		}
		return value;
	}

	/**
	 * 返回给定的参数对象的{@code JRExporterParameter}, 必要时从String转换它.
	 * 
	 * @param parameter 参数对象, String或JRExporterParameter
	 * 
	 * @return 给定参数对象的JRExporterParameter
	 */
	protected net.sf.jasperreports.engine.JRExporterParameter getExporterParameter(Object parameter) {
		if (parameter instanceof net.sf.jasperreports.engine.JRExporterParameter) {
			return (net.sf.jasperreports.engine.JRExporterParameter) parameter;
		}
		if (parameter instanceof String) {
			return convertToExporterParameter((String) parameter);
		}
		throw new IllegalArgumentException(
				"Parameter [" + parameter + "] is invalid type. Should be either String or JRExporterParameter.");
	}

	/**
	 * 将给定的完全限定字段名称转换为相应的JRExporterParameter实例.
	 * 
	 * @param fqFieldName 完全限定的字段名称, 由类名后跟一个点后跟字段名称组成
	 * (e.g. "net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI")
	 * 
	 * @return 相应的JRExporterParameter实例
	 */
	protected net.sf.jasperreports.engine.JRExporterParameter convertToExporterParameter(String fqFieldName) {
		int index = fqFieldName.lastIndexOf('.');
		if (index == -1 || index == fqFieldName.length()) {
			throw new IllegalArgumentException(
					"Parameter name [" + fqFieldName + "] is not a valid static field. " +
					"The parameter name must map to a static field such as " +
					"[net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IMAGES_URI]");
		}
		String className = fqFieldName.substring(0, index);
		String fieldName = fqFieldName.substring(index + 1);

		try {
			Class<?> cls = ClassUtils.forName(className, getApplicationContext().getClassLoader());
			Field field = cls.getField(fieldName);

			if (net.sf.jasperreports.engine.JRExporterParameter.class.isAssignableFrom(field.getType())) {
				try {
					return (net.sf.jasperreports.engine.JRExporterParameter) field.get(null);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalArgumentException(
							"Unable to access field [" + fieldName + "] of class [" + className + "]. " +
							"Check that it is static and accessible.");
				}
			}
			else {
				throw new IllegalArgumentException("Field [" + fieldName + "] on class [" + className +
						"] is not assignable from JRExporterParameter - check the type of this field.");
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException(
					"Class [" + className + "] in key [" + fqFieldName + "] could not be found.");
		}
		catch (NoSuchFieldException ex) {
			throw new IllegalArgumentException("Field [" + fieldName + "] in key [" + fqFieldName +
					"] could not be found on class [" + className + "].");
		}
	}

	/**
	 * 从指定的{@code Resource}加载主{@code JasperReport}.
	 * 如果{@code Resource}指向未编译的报告设计文件, 则会动态编译报告文件并将其加载到内存中.
	 * 
	 * @return {@code JasperReport}实例, 如果没有静态定义主报告, 则为{@code null}
	 */
	protected JasperReport loadReport() {
		String url = getUrl();
		if (url == null) {
			return null;
		}
		Resource mainReport = getApplicationContext().getResource(url);
		return loadReport(mainReport);
	}

	/**
	 * 从指定的{@code Resource}加载{@code JasperReport}.
	 * 如果{@code Resource}指向未编译的报告设计文件, 则会动态编译报告文件并将其加载到内存中.
	 * 
	 * @param resource 包含报告定义或设计的{@code Resource}
	 * 
	 * @return {@code JasperReport}实例
	 */
	protected final JasperReport loadReport(Resource resource) {
		try {
			String filename = resource.getFilename();
			if (filename != null) {
				if (filename.endsWith(".jasper")) {
					// Load pre-compiled report.
					if (logger.isInfoEnabled()) {
						logger.info("Loading pre-compiled Jasper Report from " + resource);
					}
					InputStream is = resource.getInputStream();
					try {
						return (JasperReport) JRLoader.loadObject(is);
					}
					finally {
						is.close();
					}
				}
				else if (filename.endsWith(".jrxml")) {
					// Compile report on-the-fly.
					if (logger.isInfoEnabled()) {
						logger.info("Compiling Jasper Report loaded from " + resource);
					}
					InputStream is = resource.getInputStream();
					try {
						JasperDesign design = JRXmlLoader.load(is);
						return JasperCompileManager.compileReport(design);
					}
					finally {
						is.close();
					}
				}
			}
			throw new IllegalArgumentException(
					"Report filename [" + filename + "] must end in either .jasper or .jrxml");
		}
		catch (IOException ex) {
			throw new ApplicationContextException(
					"Could not load JasperReports report from " + resource, ex);
		}
		catch (JRException ex) {
			throw new ApplicationContextException(
					"Could not parse JasperReports report from " + resource, ex);
		}
	}


	/**
	 * 查找用于呈现报告的报告数据, 然后调用应由子类实现的{@link #renderReport}方法.
	 * 
	 * @param model 传递给视图渲染的模型Map.
	 * 必须包含可根据{@link #fillReport}方法的规则转换为{@code JRDataSource}的报告数据值.
	 */
	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (this.subReports != null) {
			// Expose sub-reports as model attributes.
			model.putAll(this.subReports);

			// Transform any collections etc into JRDataSources for sub reports.
			if (this.subReportDataKeys != null) {
				for (String key : this.subReportDataKeys) {
					model.put(key, convertReportData(model.get(key)));
				}
			}
		}

		// Expose Spring-managed Locale and MessageSource.
		exposeLocalizationContext(model, request);

		// Fill the report.
		JasperPrint filledReport = fillReport(model);
		postProcessReport(filledReport, model);

		// Prepare response and render report.
		populateHeaders(response);
		renderReport(filledReport, model, response);
	}

	/**
	 * 将当前Spring管理的Locale和MessageSource公开给JasperReports i18n ($R expressions etc).
	 * 如果报告本身未定义此包, 则MessageSource应仅作为JasperReports资源包公开.
	 * <p>默认实现为Spring ApplicationContext公开Spring RequestContext Locale和MessageSourceResourceBundle适配器,
	 * 类似于{@code JstlUtils.exposeLocalizationContext}方法.
	 */
	protected void exposeLocalizationContext(Map<String, Object> model, HttpServletRequest request) {
		RequestContext rc = new RequestContext(request, getServletContext());
		Locale locale = rc.getLocale();
		if (!model.containsKey(JRParameter.REPORT_LOCALE)) {
			model.put(JRParameter.REPORT_LOCALE, locale);
		}
		TimeZone timeZone = rc.getTimeZone();
		if (timeZone != null && !model.containsKey(JRParameter.REPORT_TIME_ZONE)) {
			model.put(JRParameter.REPORT_TIME_ZONE, timeZone);
		}
		JasperReport report = getReport();
		if ((report == null || report.getResourceBundle() == null) &&
				!model.containsKey(JRParameter.REPORT_RESOURCE_BUNDLE)) {
			model.put(JRParameter.REPORT_RESOURCE_BUNDLE,
					new MessageSourceResourceBundle(rc.getMessageSource(), locale));
		}
	}

	/**
	 * 从配置的{@code JasperReport}实例创建一个填充的{@code JasperPrint}实例.
	 * <p>默认情况下, 此方法将使用通过{@link #setReportDataKey}找到的,
	 * 在模型Map中查找类型为{@code JRDataSource}的任何{@code JRDataSource}实例 (或可包装的{@code Object}),
	 * 或者通过{@link #getReportData}检索的特殊值.
	 * <p>如果找不到{@code JRDataSource}, 则此方法将使用从配置的{@code javax.sql.DataSource}(或模型中的DataSource属性)
	 * 获取的JDBC {@code Connection}.
	 * 如果也找不到JDBC DataSource, 则将使用普通模型Map调用JasperReports引擎,
	 * 假设模型包含标识报告数据源的参数 (e.g. Hibernate 或 JPA查询).
	 * 
	 * @param model 此请求的模型
	 * 
	 * @return 填充的{@code JasperPrint}实例
	 * @throws IllegalArgumentException 如果找不到{@code JRDataSource}且没有提供{@code javax.sql.DataSource}
	 * @throws SQLException 如果使用{@code javax.sql.DataSource}填充报告时出错
	 * @throws JRException 如果使用{@code JRDataSource}填充报告时出错
	 */
	protected JasperPrint fillReport(Map<String, Object> model) throws Exception {
		// Determine main report.
		JasperReport report = getReport();
		if (report == null) {
			throw new IllegalStateException("No main report defined for 'fillReport' - " +
					"specify a 'url' on this view or override 'getReport()' or 'fillReport(Map)'");
		}

		JRDataSource jrDataSource = null;
		DataSource jdbcDataSourceToUse = null;

		// Try model attribute with specified name.
		if (this.reportDataKey != null) {
			Object reportDataValue = model.get(this.reportDataKey);
			if (reportDataValue instanceof DataSource) {
				jdbcDataSourceToUse = (DataSource) reportDataValue;
			}
			else {
				jrDataSource = convertReportData(reportDataValue);
			}
		}
		else {
			Collection<?> values = model.values();
			jrDataSource = CollectionUtils.findValueOfType(values, JRDataSource.class);
			if (jrDataSource == null) {
				JRDataSourceProvider provider = CollectionUtils.findValueOfType(values, JRDataSourceProvider.class);
				if (provider != null) {
					jrDataSource = createReport(provider);
				}
				else {
					jdbcDataSourceToUse = CollectionUtils.findValueOfType(values, DataSource.class);
					if (jdbcDataSourceToUse == null) {
						jdbcDataSourceToUse = this.jdbcDataSource;
					}
				}
			}
		}

		if (jdbcDataSourceToUse != null) {
			return doFillReport(report, model, jdbcDataSourceToUse);
		}
		else {
			// Determine JRDataSource for main report.
			if (jrDataSource == null) {
				jrDataSource = getReportData(model);
			}
			if (jrDataSource != null) {
				// Use the JasperReports JRDataSource.
				if (logger.isDebugEnabled()) {
					logger.debug("Filling report with JRDataSource [" + jrDataSource + "]");
				}
				return JasperFillManager.fillReport(report, model, jrDataSource);
			}
			else {
				// 假设模型包含标识报告数据源的参数 (e.g. Hibernate 或JPA查询).
				logger.debug("Filling report with plain model");
				return JasperFillManager.fillReport(report, model);
			}
		}
	}

	/**
	 * 使用给定的JDBC DataSource和模型填充给定的报告.
	 */
	private JasperPrint doFillReport(JasperReport report, Map<String, Object> model, DataSource ds) throws Exception {
		// Use the JDBC DataSource.
		if (logger.isDebugEnabled()) {
			logger.debug("Filling report using JDBC DataSource [" + ds + "]");
		}
		Connection con = ds.getConnection();
		try {
			return JasperFillManager.fillReport(report, model, con);
		}
		finally {
			try {
				con.close();
			}
			catch (Throwable ex) {
				logger.debug("Could not close JDBC Connection", ex);
			}
		}
	}

	/**
	 * 使用用户提供的header填充{@code HttpServletResponse}中的header.
	 */
	private void populateHeaders(HttpServletResponse response) {
		// Apply the headers to the response.
		for (Enumeration<?> en = this.headers.propertyNames(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			response.addHeader(key, this.headers.getProperty(key));
		}
	}

	/**
	 * 确定要填充的{@code JasperReport}. 由{@link #fillReport}调用.
	 * <p>默认实现返回通过'url'属性静态配置的报告 (并由{@link #loadReport()}加载).
	 * 可以在子类中重写, 以便动态获取{@code JasperReport}实例.
	 * 作为替代方案, 考虑覆盖{@link #fillReport}模板方法本身.
	 * 
	 * @return {@code JasperReport}的实例
	 */
	protected JasperReport getReport() {
		return this.report;
	}

	/**
	 * 为传入的报告数据创建适当的{@code JRDataSource}.
	 * 当{@link #fillReport}自己的查找步骤不成功时, 由{@link #fillReport}调用.
	 * <p>默认实现查找类型为{@code java.util.Collection}或对象数组(按此顺序)的值.
	 * 可以在子类中重写.
	 * 
	 * @param model 传递给视图渲染的模型Map
	 * 
	 * @return {@code JRDataSource}, 如果找不到数据源, 则为{@code null}
	 */
	protected JRDataSource getReportData(Map<String, Object> model) {
		// Try to find matching attribute, of given prioritized types.
		Object value = CollectionUtils.findValueOfType(model.values(), getReportDataTypes());
		return (value != null ? convertReportData(value) : null);
	}

	/**
	 * 将给定的报告数据值转换为{@code JRDataSource}.
	 * <p>默认实现委托给{@code JasperReportUtils}, 除非报告数据值是{@code JRDataSourceProvider}的实例.
	 * 检测{@code JRDataSource}, {@code JRDataSourceProvider}, {@code java.util.Collection} 或对象数组.
	 * {@code JRDataSource}按原样返回, 而{@code JRDataSourceProvider}用于创建{@code JRDataSource}的实例, 然后返回.
	 * 后两者分别转换为{@code JRBeanCollectionDataSource} 或 {@code JRBeanArrayDataSource}.
	 * 
	 * @param value 要转换的报告数据值
	 * 
	 * @return the JRDataSource
	 * @throws IllegalArgumentException 如果该值无法转换
	 */
	protected JRDataSource convertReportData(Object value) throws IllegalArgumentException {
		if (value instanceof JRDataSourceProvider) {
			return createReport((JRDataSourceProvider) value);
		}
		else {
			return JasperReportsUtils.convertReportData(value);
		}
	}

	/**
	 * 使用给定的提供器创建报告.
	 * 
	 * @param provider 要使用的JRDataSourceProvider
	 * 
	 * @return 创建的报告
	 */
	protected JRDataSource createReport(JRDataSourceProvider provider) {
		try {
			JasperReport report = getReport();
			if (report == null) {
				throw new IllegalStateException("No main report defined for JRDataSourceProvider - " +
						"specify a 'url' on this view or override 'getReport()'");
			}
			return provider.create(report);
		}
		catch (JRException ex) {
			throw new IllegalArgumentException("Supplied JRDataSourceProvider is invalid", ex);
		}
	}

	/**
	 * 返回可按优先顺序转换为{@code JRDataSource}的值类型.
	 * 应该只返回{@link #convertReportData}方法实际上能够转换的类型.
	 * <p>默认值类型为: {@code java.util.Collection} 和 {@code Object}数组.
	 * 
	 * @return 优先顺序中的值类型
	 */
	protected Class<?>[] getReportDataTypes() {
		return new Class<?>[] {Collection.class, Object[].class};
	}


	/**
	 * 要为已填充报告的自定义后处理重写的模板方法.
	 * 在填充之后但在渲染之前调用.
	 * <p>默认实现为空.
	 * 
	 * @param populatedReport 已填充的{@code JasperPrint}
	 * @param model 包含报告参数的Map
	 * 
	 * @throws Exception 如果后处理失败
	 */
	protected void postProcessReport(JasperPrint populatedReport, Map<String, Object> model) throws Exception {
	}

	/**
	 * 子类应该实现此方法来执行实际的渲染过程.
	 * <p>请注意, 尚未设置内容类型: 实现者应构建内容类型String并通过{@code response.setContentType}设置它.
	 * 如有必要, 可以包含特定编码的charset子句.
	 * 后者仅在文本输出到Writer时, 并且仅在JasperReports导出器参数中指定编码的情况下是必需的.
	 * <p><b>WARNING:</b> 除非他们愿意依赖Servlet API 2.4或更高版本, 否则实现者不应使用{@code response.setCharacterEncoding}.
	 * 首选带有charset子句的串联内容类型String.
	 * 
	 * @param populatedReport 要渲染的已填充的{@code JasperPrint}
	 * @param model 包含报告参数的Map
	 * @param response 应该渲染报告的HTTP响应
	 * 
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderReport(
			JasperPrint populatedReport, Map<String, Object> model, HttpServletResponse response)
			throws Exception;

}
