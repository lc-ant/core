package net.lecousin.ant.core.springboot.utils;

import org.springframework.context.ApplicationContext;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpringContextUtils {

	@SuppressWarnings("unchecked")
	public static <T> T initBean(ApplicationContext ctx, T bean, String beanName) {
		ctx.getAutowireCapableBeanFactory().autowireBean(bean);
		return (T) ctx.getAutowireCapableBeanFactory().initializeBean(bean, beanName);
	}
	
}
