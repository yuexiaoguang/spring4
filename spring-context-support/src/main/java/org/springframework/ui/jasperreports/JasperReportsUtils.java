package org.springframework.ui.jasperreports;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;

/**
 * 使用JasperReports的实用方法. 提供一组便捷方法, 用于生成CSV, HTML, PDF和XLS格式的报告.
 *
 * <p><b>这个类与经典的JasperReports版本兼容, 直到2.x.</b>
 * 因此, 它继续使用2014年初已弃用的{@link net.sf.jasperreports.engine.JRExporter} API.
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public abstract class JasperReportsUtils {

	/**
	 * 将给定的报告数据值转换为{@code JRDataSource}.
	 * <p>在默认实现中, 检测{@code JRDataSource}, {@ code java.util.Collection}或对象数组.
	 * 后者分别转换为{@code JRBeanCollectionDataSource}或{@code JRBeanArrayDataSource}.
	 * 
	 * @param value 要转换的报告数据值
	 * 
	 * @return the JRDataSource (never {@code null})
	 * @throws IllegalArgumentException 如果该值无法转换
	 */
	public static JRDataSource convertReportData(Object value) throws IllegalArgumentException {
		if (value instanceof JRDataSource) {
			return (JRDataSource) value;
		}
		else if (value instanceof Collection) {
			return new JRBeanCollectionDataSource((Collection<?>) value);
		}
		else if (value instanceof Object[]) {
			return new JRBeanArrayDataSource((Object[]) value);
		}
		else {
			throw new IllegalArgumentException("Value [" + value + "] cannot be converted to a JRDataSource");
		}
	}

	/**
	 * 使用提供的{@code JRAbstractExporter}实例渲染提供的{@code JasperPrint}实例, 并将结果写入提供的{@code Writer}.
	 * <p>确保提供的{@code JRAbstractExporter}实现能够写入{@code Writer}.
	 * 
	 * @param exporter 用于渲染报告的{@code JRAbstractExporter}
	 * @param print 要渲染的{@code JasperPrint}实例
	 * @param writer 要将结果写入的{@code Writer}
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void render(net.sf.jasperreports.engine.JRExporter exporter, JasperPrint print, Writer writer)
			throws JRException {

		exporter.setParameter(net.sf.jasperreports.engine.JRExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(net.sf.jasperreports.engine.JRExporterParameter.OUTPUT_WRITER, writer);
		exporter.exportReport();
	}

	/**
	 * 使用提供的{@code JRAbstractExporter}实例渲染提供的{@code JasperPrint}实例, 并将结果写入提供的{@code OutputStream}.
	 * <p>确保提供的{@code JRAbstractExporter}实现能够写入{@code OutputStream}.
	 * 
	 * @param exporter 用于渲染报告的{@code JRAbstractExporter}
	 * @param print 要渲染的{@code JasperPrint}实例
	 * @param outputStream 要将结果写入的{@code OutputStream}
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void render(net.sf.jasperreports.engine.JRExporter exporter, JasperPrint print,
			OutputStream outputStream) throws JRException {

		exporter.setParameter(net.sf.jasperreports.engine.JRExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(net.sf.jasperreports.engine.JRExporterParameter.OUTPUT_STREAM, outputStream);
		exporter.exportReport();
	}

	/**
	 * 使用提供的报告数据以CSV格式渲染报告.
	 * 将结果写入提供的{@code Writer}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param writer 要将渲染的报告写入的{@code Writer}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsCsv(JasperReport report, Map<String, Object> parameters, Object reportData,
			Writer writer) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRCsvExporter(), print, writer);
	}

	/**
	 * 使用提供的报告数据以CSV格式渲染报告.
	 * 将结果写入提供的{@code Writer}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param writer 要将渲染的报告写入的{@code Writer}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * @param exporterParameters {@code JRExporterParameter导出器参数}的{@link Map}
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsCsv(JasperReport report, Map<String, Object> parameters, Object reportData,
			Writer writer, Map<net.sf.jasperreports.engine.JRExporterParameter, Object> exporterParameters)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRCsvExporter exporter = new JRCsvExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, writer);
	}

	/**
	 * 使用提供的报告数据以HTML格式渲染报告.
	 * 将结果写入提供的{@code Writer}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param writer 要将渲染的报告写入的{@code Writer}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsHtml(JasperReport report, Map<String, Object> parameters, Object reportData,
			Writer writer) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new net.sf.jasperreports.engine.export.JRHtmlExporter(), print, writer);
	}

	/**
	 * 使用提供的报告数据以HTML格式渲染报告.
	 * 将结果写入提供的{@code Writer}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param writer 要将渲染的报告写入的{@code Writer}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * @param exporterParameters {@code JRExporterParameter导出器参数}的{@link Map}
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsHtml(JasperReport report, Map<String, Object> parameters, Object reportData,
			Writer writer, Map<net.sf.jasperreports.engine.JRExporterParameter, Object> exporterParameters)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		net.sf.jasperreports.engine.export.JRHtmlExporter exporter = new net.sf.jasperreports.engine.export.JRHtmlExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, writer);
	}

	/**
	 * 使用提供的报告数据以PDF格式渲染报告.
	 * 将结果写入提供的{@code OutputStream}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param stream 要将渲染的报告写入的{@code OutputStream}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsPdf(JasperReport report, Map<String, Object> parameters, Object reportData,
			OutputStream stream) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRPdfExporter(), print, stream);
	}

	/**
	 * 使用提供的报告数据以PDF格式渲染报告.
	 * 将结果写入提供的{@code OutputStream}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param stream 要将渲染的报告写入的{@code OutputStream}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * @param exporterParameters {@code JRExporterParameter导出器参数}的{@link Map}
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsPdf(JasperReport report, Map<String, Object> parameters, Object reportData,
			OutputStream stream, Map<net.sf.jasperreports.engine.JRExporterParameter, Object> exporterParameters)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRPdfExporter exporter = new JRPdfExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, stream);
	}

	/**
	 * 使用提供的报告数据以XLS格式渲染报告.
	 * 将结果写入提供的{@code OutputStream}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param stream 要将渲染的报告写入的{@code OutputStream}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsXls(JasperReport report, Map<String, Object> parameters, Object reportData,
			OutputStream stream) throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		render(new JRXlsExporter(), print, stream);
	}

	/**
	 * 使用提供的报告数据以XLS格式渲染报告.
	 * 将结果写入提供的{@code OutputStream}.
	 * 
	 * @param report 要渲染的{@code JasperReport}实例
	 * @param parameters 用于渲染的参数
	 * @param stream 要将渲染的报告写入的{@code OutputStream}
	 * @param reportData {@code JRDataSource}, {@code java.util.Collection}或对象数组(相应地转换), 表示要从中读取字段的报告数据
	 * @param exporterParameters {@code JRExporterParameter导出器参数}的{@link Map}
	 * 
	 * @throws JRException 如果渲染失败
	 */
	public static void renderAsXls(JasperReport report, Map<String, Object> parameters, Object reportData,
			OutputStream stream, Map<net.sf.jasperreports.engine.JRExporterParameter, Object> exporterParameters)
			throws JRException {

		JasperPrint print = JasperFillManager.fillReport(report, parameters, convertReportData(reportData));
		JRXlsExporter exporter = new JRXlsExporter();
		exporter.setParameters(exporterParameters);
		render(exporter, print, stream);
	}
}
