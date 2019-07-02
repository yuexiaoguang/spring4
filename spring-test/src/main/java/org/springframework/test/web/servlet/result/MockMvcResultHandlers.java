package org.springframework.test.web.servlet.result;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.CollectionUtils;

/**
 * 基于{@link ResultHandler}的结果操作的静态工厂方法.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to this setting, open the Preferences and type "favorites".
 */
public abstract class MockMvcResultHandlers {

	private static final Log logger = LogFactory.getLog("org.springframework.test.web.servlet.result");


	/**
	 * 使用日志类别{@code org.springframework.test.web.servlet.result}
	 * 通过Apache Commons Logging将{@link MvcResult}详细信息记录为{@code DEBUG}日志消息.
	 */
	public static ResultHandler log() {
		return new LoggingResultHandler();
	}

	/**
	 * 将{@link MvcResult}详细信息打印到“标准”输出流.
	 */
	public static ResultHandler print() {
		return print(System.out);
	}

	/**
	 * 打印{@link MvcResult}详细信息到提供的{@link OutputStream}.
	 */
	public static ResultHandler print(OutputStream stream) {
		return new PrintWriterPrintingResultHandler(new PrintWriter(stream, true));
	}

	/**
	 * 打印{@link MvcResult}详细信息到提供的{@link Writer}.
	 */
	public static ResultHandler print(Writer writer) {
		return new PrintWriterPrintingResultHandler(new PrintWriter(writer, true));
	}


	/**
	 * 写入{@link PrintWriter}的{@link PrintingResultHandler}.
	 */
	private static class PrintWriterPrintingResultHandler extends PrintingResultHandler {

		public PrintWriterPrintingResultHandler(final PrintWriter writer) {
			super(new ResultValuePrinter() {
				@Override
				public void printHeading(String heading) {
					writer.println();
					writer.println(String.format("%s:", heading));
				}
				@Override
				public void printValue(String label, Object value) {
					if (value != null && value.getClass().isArray()) {
						value = CollectionUtils.arrayToList(value);
					}
					writer.println(String.format("%17s = %s", label, value));
				}
			});
		}
	}


	/**
	 * 一个{@link ResultHandler}, 通过Apache Commons Logging在{@code DEBUG}级别记录{@link MvcResult}详细信息.
	 *
	 * <p>委托{@link PrintWriterPrintingResultHandler}来构建日志消息.
	 */
	private static class LoggingResultHandler implements ResultHandler {

		@Override
		public void handle(MvcResult result) throws Exception {
			if (logger.isDebugEnabled()) {
				StringWriter stringWriter = new StringWriter();
				ResultHandler printingResultHandler =
						new PrintWriterPrintingResultHandler(new PrintWriter(stringWriter));
				printingResultHandler.handle(result);
				logger.debug("MvcResult details:\n" + stringWriter);
			}
		}
	}
}
