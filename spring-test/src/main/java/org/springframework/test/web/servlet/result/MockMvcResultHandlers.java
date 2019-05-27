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
 * Static factory methods for {@link ResultHandler}-based result actions.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 */
public abstract class MockMvcResultHandlers {

	private static final Log logger = LogFactory.getLog("org.springframework.test.web.servlet.result");


	/**
	 * Log {@link MvcResult} details as a {@code DEBUG} log message via
	 * Apache Commons Logging using the log category
	 * {@code org.springframework.test.web.servlet.result}.
	 */
	public static ResultHandler log() {
		return new LoggingResultHandler();
	}

	/**
	 * Print {@link MvcResult} details to the "standard" output stream.
	 */
	public static ResultHandler print() {
		return print(System.out);
	}

	/**
	 * Print {@link MvcResult} details to the supplied {@link OutputStream}.
	 */
	public static ResultHandler print(OutputStream stream) {
		return new PrintWriterPrintingResultHandler(new PrintWriter(stream, true));
	}

	/**
	 * Print {@link MvcResult} details to the supplied {@link Writer}.
	 */
	public static ResultHandler print(Writer writer) {
		return new PrintWriterPrintingResultHandler(new PrintWriter(writer, true));
	}


	/**
	 * A {@link PrintingResultHandler} that writes to a {@link PrintWriter}.
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
	 * A {@link ResultHandler} that logs {@link MvcResult} details at
	 * {@code DEBUG} level via Apache Commons Logging.
	 *
	 * <p>Delegates to a {@link PrintWriterPrintingResultHandler} for
	 * building the log message.
	 *
	 * @since 4.2
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
