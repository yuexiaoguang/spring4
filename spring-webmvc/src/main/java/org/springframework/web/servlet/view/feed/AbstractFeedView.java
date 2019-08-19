package org.springframework.web.servlet.view.feed;

import java.io.OutputStreamWriter;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Atom和RSS Feed视图的抽象基类, 使用<a href="https://github.com/rometools/rome">ROME</a>包.
 *
 * <p>><b>NOTE: 从Spring 4.1开始, 这是基于ROME版本1.5的{@code com.rometools}变体. 请升级构建依赖项.</b>
 *
 * <p>特定于应用程序的视图类通常从{@link AbstractRssFeedView} 或 {@link AbstractAtomFeedView}扩展, 而不是从此类扩展.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 */
public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		T wireFeed = newFeed();
		buildFeedMetadata(model, wireFeed, request);
		buildFeedEntries(model, wireFeed, request, response);

		setResponseContentType(request, response);
		if (!StringUtils.hasText(wireFeed.getEncoding())) {
			wireFeed.setEncoding("UTF-8");
		}

		WireFeedOutput feedOutput = new WireFeedOutput();
		ServletOutputStream out = response.getOutputStream();
		feedOutput.output(wireFeed, new OutputStreamWriter(out, wireFeed.getEncoding()));
		out.flush();
	}

	/**
	 * 创建一个新的Feed实例来保存条目.
	 * 
	 * @return 新创建的Feed实例
	 */
	protected abstract T newFeed();

	/**
	 * 填充Feed元数据 (title, link, description, etc.).
	 * <p>默认是一个空实现. 子类可以重写此方法以添加元字段, 如标题, 链接描述等.
	 * 
	 * @param model 模型, 以防必须从中填充元信息
	 * @param feed 正在填充的Feed
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 */
	protected void buildFeedMetadata(Map<String, Object> model, T feed, HttpServletRequest request) {
	}

	/**
	 * 在给定模型的情况下, 子类必须实现此方法来构建feed条目.
	 * <p>请注意, 传入的HTTP响应应该用于设置cookie或其他 HTTP header.
	 * 在此方法返回后, 构建的Feed本身将自动写入响应.
	 * 
	 * @param model 模型 Map
	 * @param feed 要添加条目的Feed
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 * @param response 以防需要设置cookie. 不应该写入它.
	 * 
	 * @throws Exception 构建期间发生的任何异常
	 */
	protected abstract void buildFeedEntries(Map<String, Object> model, T feed,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
