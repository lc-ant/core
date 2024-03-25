package net.lecousin.ant.core.springboot;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import net.lecousin.ant.core.springboot.http.PageRequestMethodArgumentResolver;

@Configuration
public class WebFluxConfiguration extends DelegatingWebFluxConfiguration {

	@Override
    protected LocaleContextResolver createLocaleContextResolver() {
		AcceptHeaderLocaleContextResolver resolver = new AcceptHeaderLocaleContextResolver();
		resolver.setDefaultLocale(Locale.ENGLISH);
		resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.US, Locale.FRENCH));
		return resolver;
    }
	
	@Override
	protected void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		configurer.addCustomResolver(new PageRequestMethodArgumentResolver());
		super.configureArgumentResolvers(configurer);
	}

}
