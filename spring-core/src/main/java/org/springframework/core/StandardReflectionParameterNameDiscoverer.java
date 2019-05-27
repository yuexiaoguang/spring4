package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.lang.UsesJava8;

/**
 * {@link ParameterNameDiscoverer}实现, 它使用JDK 8的反射工具来内省参数名称 (基于"-parameters"编译器标志).
 */
@UsesJava8
public class StandardReflectionParameterNameDiscoverer implements ParameterNameDiscoverer {

	@Override
	public String[] getParameterNames(Method method) {
		Parameter[] parameters = method.getParameters();
		String[] parameterNames = new String[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			if (!param.isNamePresent()) {
				return null;
			}
			parameterNames[i] = param.getName();
		}
		return parameterNames;
	}

	@Override
	public String[] getParameterNames(Constructor<?> ctor) {
		Parameter[] parameters = ctor.getParameters();
		String[] parameterNames = new String[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			if (!param.isNamePresent()) {
				return null;
			}
			parameterNames[i] = param.getName();
		}
		return parameterNames;
	}

}
