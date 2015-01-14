package org.springframework.cloud.netflix.feign;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Spencer Gibb patterned after Spring Integration
 * IntegrationComponentScanRegistrar
 */
public class FeignClientScanRegistrar extends FeignConfiguration implements
		ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanClassLoaderAware {

	private ResourceLoader resourceLoader;

	private ClassLoader classLoader;

	public FeignClientScanRegistrar() {
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {

		Set<String> basePackages = getBasePackages(importingClassMetadata);

		ClassPathScanningCandidateComponentProvider scanner = getScanner();
		scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class));
		scanner.setResourceLoader(this.resourceLoader);

		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidateComponents = scanner
					.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					// verify annotated class is an interface
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Assert.isTrue(annotationMetadata.isInterface(),
							"@FeignClient can only be specified on an interface");

					BeanDefinitionHolder holder = createBeanDefinition(annotationMetadata);
					BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
				}
			}
		}
	}

	public BeanDefinitionHolder createBeanDefinition(AnnotationMetadata annotationMetadata) {
		Map<String, Object> attributes = annotationMetadata
				.getAnnotationAttributes(FeignClient.class.getCanonicalName());

		String className = annotationMetadata.getClassName();
		BeanDefinitionBuilder definition = BeanDefinitionBuilder
				.genericBeanDefinition(FeignClientFactoryBean.class);
		definition.addPropertyValue("loadbalance", attributes.get("loadbalance"));
		definition.addPropertyValue("type", className);
		definition.addPropertyValue("schemeName", attributes.get("value"));

		String beanName = StringUtils.uncapitalize(className.substring(className
				.lastIndexOf(".") + 1));
		return new BeanDefinitionHolder(definition.getBeanDefinition(), beanName);
	}

	protected ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false) {

			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				if (beanDefinition.getMetadata().isIndependent()) {
					// TODO until SPR-11711 will be resolved
					if (beanDefinition.getMetadata().isInterface()
							&& beanDefinition.getMetadata().getInterfaceNames().length == 1
							&& Annotation.class.getName().equals(
									beanDefinition.getMetadata().getInterfaceNames()[0])) {
						try {
							Class<?> target = ClassUtils.forName(beanDefinition
									.getMetadata().getClassName(),
									FeignClientScanRegistrar.this.classLoader);
							return !target.isAnnotation();
						}
						catch (Exception e) {
							this.logger.error("Could not load target class: "
									+ beanDefinition.getMetadata().getClassName(), e);

						}
					}
					return true;
				}
				return false;
			}
		};
	}

	protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> attributes = importingClassMetadata
				.getAnnotationAttributes(FeignClientScan.class.getCanonicalName());

		Set<String> basePackages = new HashSet<>();
		for (String pkg : (String[]) attributes.get("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : (String[]) attributes.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : (Class[]) attributes.get("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}

		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(importingClassMetadata
					.getClassName()));
		}
		return basePackages;
	}

}
