package org.springframework.core.type.filter;

import java.io.IOException;

import org.aspectj.bridge.IMessageHandler;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.World;
import org.aspectj.weaver.bcel.BcelWorld;
import org.aspectj.weaver.patterns.Bindings;
import org.aspectj.weaver.patterns.FormalBinding;
import org.aspectj.weaver.patterns.IScope;
import org.aspectj.weaver.patterns.PatternParser;
import org.aspectj.weaver.patterns.SimpleScope;
import org.aspectj.weaver.patterns.TypePattern;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * 使用AspectJ类型模式进行匹配的类型过滤器.
 *
 * <p>此类型过滤器的一个关键实现细节是, 它不会加载要检查的类以匹配类型模式.
 */
public class AspectJTypeFilter implements TypeFilter {

	private final World world;

	private final TypePattern typePattern;


	public AspectJTypeFilter(String typePatternExpression, ClassLoader classLoader) {
		this.world = new BcelWorld(classLoader, IMessageHandler.THROW, null);
		this.world.setBehaveInJava5Way(true);
		PatternParser patternParser = new PatternParser(typePatternExpression);
		TypePattern typePattern = patternParser.parseTypePattern();
		typePattern.resolve(this.world);
		IScope scope = new SimpleScope(this.world, new FormalBinding[0]);
		this.typePattern = typePattern.resolveBindings(scope, Bindings.NONE, false, false);
	}


	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {

		String className = metadataReader.getClassMetadata().getClassName();
		ResolvedType resolvedType = this.world.resolve(className);
		return this.typePattern.matchesStatically(resolvedType);
	}
}
