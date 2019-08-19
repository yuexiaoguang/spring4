package org.springframework.web.servlet.view.document;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.web.servlet.view.AbstractView;

/**
 * PDF视图的抽象超类. 特定于应用程序的视图类将扩展此类.
 * 视图将保留在子类本身中, 而不是模板中.
 *
 * <p>此视图实现使用Bruno Lowagie的<a href="http://www.lowagie.com/iText">iText</a> API.
 * 已知可以使用原始的iText 2.1.7及其分支<a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>.
 * <b>强烈建议使用OpenPDF, 因为它是主动维护的, 并修复了不受信任的PDF内容的重要漏洞.</b>
 *
 * <p>Note: Internet Explorer需要".pdf"扩展名, 因为它并不总是尊重声明的内容类型.
 */
public abstract class AbstractPdfView extends AbstractView {

	/**
	 * 此构造函数设置适当的内容类型"application/pdf".
	 * 请注意, IE不会注意到这一点, 但可以做很多事情.
	 * 生成的文档应具有".pdf"扩展名.
	 */
	public AbstractPdfView() {
		setContentType("application/pdf");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// IE workaround: write into byte array first.
		ByteArrayOutputStream baos = createTemporaryOutputStream();

		// Apply preferences and build metadata.
		Document document = newDocument();
		PdfWriter writer = newWriter(document, baos);
		prepareWriter(model, writer, request);
		buildPdfMetadata(model, document, request);

		// Build PDF document.
		document.open();
		buildPdfDocument(model, document, writer, request, response);
		document.close();

		// Flush to HTTP response.
		writeToResponse(response, baos);
	}

	/**
	 * 创建一个新文档来保存PDF内容.
	 * <p>默认返回A4文档, 但子类可以指定任何Document, 可能通过View上定义的bean属性进行参数化.
	 * 
	 * @return 新创建的iText Document实例
	 */
	protected Document newDocument() {
		return new Document(PageSize.A4);
	}

	/**
	 * 为给定的iText文档创建一个新的PdfWriter.
	 * 
	 * @param document 为其创建写入器的iText Document
	 * @param os 要写入的OutputStream
	 * 
	 * @return 要使用的PdfWriter实例
	 * @throws DocumentException 如果在创建写入器期间抛出
	 */
	protected PdfWriter newWriter(Document document, OutputStream os) throws DocumentException {
		return PdfWriter.getInstance(document, os);
	}

	/**
	 * 准备给定的PdfWriter. 在构建PDF文档之前调用, 即在调用{@code Document.open()}之前调用.
	 * <p>例如, 用于注册页面事件监听器.
	 * 默认实现设置此类的{@code getViewerPreferences()}方法返回的查看器首选项.
	 * 
	 * @param model 模型, 以防必须从中填充元信息
	 * @param writer 要准备的PdfWriter
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 * 
	 * @throws DocumentException 如果在准备写入器期间被抛出
	 */
	protected void prepareWriter(Map<String, Object> model, PdfWriter writer, HttpServletRequest request)
			throws DocumentException {

		writer.setViewerPreferences(getViewerPreferences());
	}

	/**
	 * 返回PDF文件的查看器首选项.
	 * <p>默认返回{@code AllowPrinting}和{@code PageLayoutSinglePage}, 但可以进行子类化.
	 * 子类可以具有固定的首选项, 也可以从View上定义的bean属性中检索它们.
	 * 
	 * @return 包含针对PdfWriter定义的位信息的int
	 */
	protected int getViewerPreferences() {
		return PdfWriter.ALLOW_PRINTING | PdfWriter.PageLayoutSinglePage;
	}

	/**
	 * 填充iText文档的元字段 (author, title, etc.).
	 * <br>默认是一个空实现. 子类可以重写此方法以添加元字段, 例如 title, subject, author, creator, keywords, etc.
	 * 在将PdfWriter分配给Document之后和调用{@code document.open()}之前调用此方法.
	 * 
	 * @param model 模型, 以防必须从中填充元信息
	 * @param document 正在填充的iText文档
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 */
	protected void buildPdfMetadata(Map<String, Object> model, Document document, HttpServletRequest request) {
	}

	/**
	 * 在给定模型的情况下, 子类必须实现此方法来构建iText PDF文档.
	 * 在{@code Document.open()} 和 {@code Document.close()}调用之间调用.
	 * <p>请注意, 传入的HTTP响应应该用于设置cookie或其他HTTP headers.
	 * 在此方法返回后, 构建的PDF文档本身将自动写入响应.
	 * 
	 * @param model 模型Map
	 * @param document 要添加元素的iText文档
	 * @param writer 要使用的PdfWriter
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 * @param response 以防需要设置cookie. 不应该写入它.
	 * 
	 * @throws Exception 文档构建期间发生的任何异常
	 */
	protected abstract void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
