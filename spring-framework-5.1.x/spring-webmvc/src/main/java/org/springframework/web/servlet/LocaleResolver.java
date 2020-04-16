/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * Interface for web-based locale resolution strategies that allows for
 * both locale resolution via the request and locale modification via
 * request and response.
 *
 * <p>This interface allows for implementations based on request, session,
 * cookies, etc. The default implementation is
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver},
 * simply using the request's locale provided by the respective HTTP header.
 *
 * <p>Use {@link org.springframework.web.servlet.support.RequestContext#getLocale()}
 * to retrieve the current locale in controllers or views, independent
 * of the actual resolution strategy.
 *
 * <p>Note: As of Spring 4.0, there is an extended strategy interface
 * called {@link LocaleContextResolver}, allowing for resolution of
 * a {@link org.springframework.context.i18n.LocaleContext} object,
 * potentially including associated time zone information. Spring's
 * provided resolver implementations implement the extended
 * {@link LocaleContextResolver} interface wherever appropriate.
 *
 * @author Juergen Hoeller
 * @since 27.02.2003
 * @see LocaleContextResolver
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.servlet.support.RequestContext#getLocale
 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
 */

/**
 * 对于LocaleResolver，其主要作用在于根据不同的用户区域展示不同的视图
 * 而用户的区域也称为Locale，该信息是可以由前端直接获取的。
 * 通过这种方式，可以实现一种国际化的目的，比如针对美国用户可以提供一个视图，而针对中国用户则可以提供另一个视图。
 */
/**
FixedLocaleResolver：在声明该resolver时，需要指定一个默认的Locale，在进行Locale获取时，始终返回该Locale，并且调用其setLocale()方法也无法改变其Locale；
CookieLocaleResolver：其读取Locale的方式是在session中通过Cookie来获取其指定的Locale的，如果修改了Cookie的值，页面视图也会同步切换；
SessionLocaleResolver：其会将Locale信息存储在session中，如果用户想要修改Locale信息，可以通过修改session中对应属性的值即可；
AcceptHeaderLocaleResolver：其会通过用户请求中名称为Accept-Language的header来获取Locale信息，如果想要修改展示的视图，只需要修改该header信息即可。

 需要说明的是，Spring虽然提供的几个不同的获取Locale的方式，但这些方式处理FixedLocaleResolver以外，
 其他几个也都支持在浏览器地址栏中添加locale参数来切换Locale。
 对于Locale的切换，Spring是通过拦截器来实现的，其提供了一个LocaleChangeInterceptor，在该拦截器中的preHandle()方法中，
 Spring会读取浏览器参数中的locale参数，然后调用LocaleResolver.setLocale()方法来实现对Locale的切换。

 参考：https://my.oschina.net/zhangxufeng/blog/2222918
 */
public interface LocaleResolver {

	/**
	 * Resolve the current locale via the given request.
	 * Can return a default locale as fallback in any case.
	 * @param request the request to resolve the locale for
	 * @return the current locale (never {@code null})
	 */
	// 根据request对象根据指定的方式获取一个Locale，如果没有获取到，则使用用户指定的默认的Locale
	Locale resolveLocale(HttpServletRequest request);

	/**
	 * Set the current locale to the given one.
	 * @param request the request to be used for locale modification
	 * @param response the response to be used for locale modification
	 * @param locale the new locale, or {@code null} to clear the locale
	 * @throws UnsupportedOperationException if the LocaleResolver
	 * implementation does not support dynamic changing of the locale
	 */
	// 用于实现Locale的切换。比如SessionLocaleResolver获取Locale的方式是从session中读取，但如果
	// 用户想要切换其展示的样式(由英文切换为中文)，那么这里的setLocale()方法就提供了这样一种可能
	void setLocale(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Locale locale);

}
