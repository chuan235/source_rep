/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// spring的两个扩展点
			// 实现了BeanFactoryPostProcessor的接口可以操作bean的定义(scope,lazy...)，这时bean还没有实例化
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// BeanDefinitionRegistryPostProcessor是BeanFactoryPostProcessor接口的子类，所以它是增强了BeanFactoryPostProcessor接口
			// 主要扩展了操作bean的定义
			// spring中在AnnotationConfigUtils.registerAnnotationConfigProcessors(BeanDefinitionRegistry, Object)方法中注入了4个或者5个或者6个bean的定义
			// 其中只有一个bean定义中的bean对象实现了BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			/**
			 * beanFactoryPostProcessors是通过方法传递过来的实现了BeanFactoryPostProcessor接口的类集合
			 * 是通过调用getBeanFactoryPostProcessors方法获得的List集合，只有一种办法可以向这个集合添加类
			 * 就是通过AnnotationConfigApplicationContext的实例调用下面这个方法，就可以向这个集合中添加我们自定义BeanFactoryPostProcessor
			 * 	context.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
			 * 同时在context对象的构造方法中不能传入非这个两个接口的class对象或者String的包名
			 * 因为在这个两个构造函数中都会自动刷新容器，导致我们注册的这个写处理器无法在bean实例化的时候执行
			 * 如果在构造方法中传入这个接口的实例class对象，那么不会在这里调用，在下面会被spring从bdmap中拿出来和spring自己的ConfigurationClassPostProcessor一起执行
			 * 使用这种方法注入也可以，context的构造函数接受的是class类型的不参长参数
			 */
			// 执行通过addBeanFactoryPostProcessor方法添加的BeanFactoryPostProcessor处理器
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 执行实现BeanDefinitionRegistryPostProcessor接口的postProcessBeanDefinitionRegistry方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 将这个类加入到bdrpp集合中
					registryProcessors.add(registryProcessor);
				}else {
					// 这个类实现了BeanFactoryPostProcessor接口，直接添加到bfpp集合中
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest. 排序
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/**
			 * 调用了DefaultListableBeanFactory.doGetBeanNamesForType(ResolvableType, boolean, boolean)方法
			 * 从 beanDefinitionNames 这个List中循环拿到beanName
			 * 	=> mergedBeanDefinitions.get(beanName)方法获得与这个beanName关联的RootBeanDefinition
			 * 	=> rbd.getDecoratedDefinition() 获取到beanName对应的BeanDefinitionHolder
			 * 	=> 核心判断BeanDefinition中的class类型与传入的type是否匹配
			 *  => 匹配成功放入数组中，否则继续循环beanDefinitionNames
			 *
			 * 	type = BeanDefinitionRegistryPostProcessor.class
			 *  从 beanDefinitionNames 去找
			 *  默认如果不注册BeanDefinitionRegistryPostProcessor类型的类的话
			 *  	spring容器只会找到一个ConfigurationClassPostProcessor
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				// 首先筛选存在优先级的BeanDefinitionRegistryPostProcessor,实现了PriorityOrdered
				// 它们总是先于实现了Ordered的BeanDefinitionRegistryPostProcessor执行
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			// 调用了BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry回调方法
			// 默认调用 ConfigurationClassPostProcessor 类的 postProcessBeanDefinitionRegistry 方法 => 这一个方法非常关键，可以说是spring容器初始化的最关键的一个方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清除集合的元素
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			/**
			 * 这里是获取容器中的所有 BeanDefinitionRegistryPostProcessor 类型, 打了注解的 BeanDefinitionRegistryPostProcessor 的实现都会在这里被找到
			 * 	因为在上一个 ConfigurationClassPostProcessor 这一个处理器中进行了包扫描
			 * 	通过下面的for可以看出执行的还是实现了 Ordered 接口的处理器
			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				// 不存在与优先级集合中并且可以匹配Ordered接口, 回调所有实现 Ordered 接口的BeanDefinitionRegistryPostProcessor
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			// 执行方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();
			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后调用所有的 BeanDefinitionRegistryPostProcessor 实现类的 postProcessBeanDefinitionRegistry() 回调方法
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}
			// 最后才调用BeanFactoryPostProcessor接口的方法
			// registryProcessors调用所有的实现了BeanDefinitionRegistryPostProcessor接口的类，有顺序 系统 -> PriorityOrdered -> Ordered
			/**
			 * 向 registryProcessors 集合中添加处理器的顺序
			 * 	1、首先添加通过 addBeanFactoryPostProcessor 方法注入的 bdrpp 类型，会立即执行 BeanDefinitionRegistryPostProcessor 的postProcessBeanDefinitionRegistry方法
			 * 	2、添加spring容器的configurationClassPostProcessor和register中注入的实现了PriorityOrdered接口的bdrpp类型，会执行回调方法
			 * 	3、添加spring容器中所有的实现了Ordered接口的bdrpp类型，会执行回调方法，这时bd Map已经存在了所有的bd，扫描已经在configurationClassPostProcessor 的回调方法中完成了
			 *  4、添加spring容器中没有实现Ordered接口的bdrpp类型 ，会执行回调方法
			 *  	（判断是否已经存在于这个 processedBeans 这个set集合中,存在就不添加了）
			 *  	processedBeans 这个set会在判断 PriorityOrdered 这个接口时开始向里面添加beanName
			 *  		也就是说这个集合里bean都已经执行过了
			 * 如果通过addBeanFactoryPostProcessor方法放进来的bdrpp，如果对应的类没有加入compent等注解，那么spring将不会在次执行
			 */
			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			/**
			 * regularPostProcessors是实现了 BeanFactoryPostProcessor 接口的类，这一个集合只在第一个循环中添加
			 * 只通过context.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
			 * 通过上面的方法添加的 bdpp 都会统一在这里回调，并且如果添加的类加了component这种注解的话
			 * 在下面的程序还会在回调一次 invokeBeanFactoryPostProcessors
 			 */
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}
		/**
		 * 获取spring自己的 和 我们程序中添加注解的和 register/构造函数 注入的 BeanFactoryPostProcessor
		 * org.springframework.context.annotation.internalConfigurationAnnotationProcessor
		 * org.springframework.context.event.internalEventListenerProcessor
		 * myBeanDefinitionRegistryPostProcessor
		 */
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest. 分类执行 BeanFactoryPostProcessor 接口的方法
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			/**
			 * 进行判断，看是否已经在invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);执行了
			 * 比如 ConfigurationClassPostProcessor 他已经存在于processedBeans中了
			 */
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			// 分类三次，按顺序执行
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have 清除缓存
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		// 拿出容器中所有 BeanPostProcessor 类
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when a bean is created during BeanPostProcessor instantiation
		// 注册 BeanPostProcessorChecker ,检查bean是否有资格执行这些 beanPostProcessor
		// i.e. when a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest. 分成三类 PriorityOrdered Ordered nonOrdered
		// 特殊的一类  internal 存储实现了 MergedBeanDefinitionPostProcessor 接口的类(不论优先级，后面会进行排序)
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 注册 PriorityOrdered
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);
		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 注册 Ordered 接口的 BeanPostProcessor
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 注册其他 BeanPostProcessor
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors. 最后注册 internal BeanPostProcessors
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc). 添加了一个监听器
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {
		// org.springframework.beans.factory.support.AbstractBeanFactory.addBeanPostProcessor
		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
