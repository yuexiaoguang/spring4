package org.springframework.http.converter.json;

import java.lang.reflect.Type;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.springframework.util.Base64Utils;

/**
 * 一个简单的实用工具类, 用于在读取和编写JSON时获取Google Gson 2.x {@link GsonBuilder},
 * 其中Base64编码{@code byte[]}属性.
 */
public abstract class GsonBuilderUtils {

	/**
	 * 在读取和编写JSON时获取{@link GsonBuilder}, 它将Base64编码{@code byte[]}属性.
	 * <p>自定义{@link com.google.gson.TypeAdapter}将通过{@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)}注册,
	 * 将{@code byte[]}属性序列化为Base64编码的字符串, 或将Base64编码的字符串反序列化为{@code byte[]}.
	 * <p><strong>NOTE:</strong> 使用此选项在Java 6或7上运行时需要Apache Commons Codec库.
	 * 在Java 8上, 使用标准的{@link java.util.Base64}工具.
	 */
	public static GsonBuilder gsonBuilderWithBase64EncodedByteArrays() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());
		return builder;
	}


	private static class Base64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

		@Override
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(Base64Utils.encodeToString(src));
		}

		@Override
		public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) {
			return Base64Utils.decodeFromString(json.getAsString());
		}
	}

}
