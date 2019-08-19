package org.springframework.web.servlet.view.feed;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;

import org.springframework.http.MediaType;

/**
 * RSS Feed视图的抽象超类, 使用<a href="https://github.com/rometools/rome">ROME</a>包.
 *
 * <p>><b>NOTE: 从Spring 4.1开始, 这是基于ROME版本1.5的{@code com.rometools}变体. 请升级构建依赖项.</b>
 *
 * <p>特定于应用程序的视图类将扩展此类.
 * 视图将保留在子类本身中, 而不是模板中.
 * 主要入口点是{@link #buildFeedMetadata}和{@link #buildFeedItems}.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 */
public abstract class AbstractRssFeedView extends AbstractFeedView<Channel> {

	public AbstractRssFeedView() {
		setContentType(MediaType.APPLICATION_RSS_XML_VALUE);
	}


	/**
	 * 创建一个新的Channel实例来保存条目.
	 * <p>默认返回RSS 2.0 channel, 但子类可以指定任何channel.
	 */
	@Override
	protected Channel newFeed() {
		return new Channel("rss_2.0");
	}

	/**
	 * 调用{@link #buildFeedItems(Map, HttpServletRequest, HttpServletResponse)}以获取feed条目列表.
	 */
	@Override
	protected final void buildFeedEntries(Map<String, Object> model, Channel channel,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		List<Item> items = buildFeedItems(model, request, response);
		channel.setItems(items);
	}

	/**
	 * 在给定模型的情况下, 子类必须实现此方法来构建feed条目.
	 * <p>请注意, 传入的HTTP响应应该用于设置cookie或其他 HTTP header.
	 * 在此方法返回后, 构建的Feed本身将自动写入响应.
	 * 
	 * @param model	模型 Map
	 * @param request  以防需要语言环境等. 不应该查看属性.
	 * @param response 以防需要设置cookie. 不应该写入它.
	 * 
	 * @return 要添加到Feed的Feed条目
	 * @throws Exception 文档构建期间发生的任何异常
	 */
	protected abstract List<Item> buildFeedItems(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
