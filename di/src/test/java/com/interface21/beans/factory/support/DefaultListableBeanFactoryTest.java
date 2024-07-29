package com.interface21.beans.factory.support;

import com.interface21.beans.factory.config.SingletonBeanDefinition;
import com.interface21.context.stereotype.Controller;
import com.interface21.context.stereotype.Repository;
import com.interface21.context.stereotype.Service;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import samples.JdbcSampleRepository;
import samples.SampleController;
import samples.SampleRepository;
import samples.SampleService;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultListableBeanFactoryTest {

    @Test
    void 기본생성자만_있는_클래스의_빈을_생성한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("JdbcSampleRepository", new SingletonBeanDefinition(JdbcSampleRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        assertAll(
                () -> assertThat(beanFactory.getSingletonObjects()).containsKey(JdbcSampleRepository.class),
                () -> assertThat(beanFactory.getSingletonObjects().get(JdbcSampleRepository.class)).isInstanceOf(JdbcSampleRepository.class)
        );
    }

    @Test
    void 이미_빈으로_생성된_경우_재생성하지_않는다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("JdbcSampleRepository", new SingletonBeanDefinition(JdbcSampleRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();
        Object expected = beanFactory.getSingletonObjects().get(JdbcSampleRepository.class);
        beanFactory.initialize();
        Object actual = beanFactory.getSingletonObjects().get(JdbcSampleRepository.class);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void 생성자에_파라미터가_빈이_아닌_경우_빈이_생성된_후_다시_생성한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of(
                "JdbcSampleRepository", new SingletonBeanDefinition(JdbcSampleRepository.class),
                "SampleService", new SingletonBeanDefinition(SampleService.class)
        ));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        SampleService actual = (SampleService) beanFactory.getSingletonObjects().get(SampleService.class);
        assertThat(actual.getSampleRepository()).isEqualTo(beanFactory.getSingletonObjects().get(JdbcSampleRepository.class));
    }

    @Test
    void beanClass들을_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of(
                "JdbcSampleRepository", new SingletonBeanDefinition(JdbcSampleRepository.class),
                "SampleService", new SingletonBeanDefinition(SampleService.class)
        ));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        Set<Class<?>> actual = beanFactory.getBeanClasses();
        assertThat(actual).contains(JdbcSampleRepository.class, SampleService.class);
    }

    @Test
    void 없는_bean을_가져가려하면_예외가_발생한다() {
        assertThatThrownBy(() -> new DefaultListableBeanFactory().getBean(SampleService.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 빈입니다.");
    }

    @Test
    void 생성된_빈을_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("JdbcSampleRepository", new SingletonBeanDefinition(JdbcSampleRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        JdbcSampleRepository actual = beanFactory.getBean(JdbcSampleRepository.class);
        assertThat(beanFactory.getSingletonObjects()).containsValue(actual);
    }

    @Test
    void 인터페이스로_빈을_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("JdbcSampleRepository", new SingletonBeanDefinition(JdbcSampleRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        SampleRepository actual = beanFactory.getBean(SampleRepository.class);
        assertThat(beanFactory.getSingletonObjects()).containsValue(actual);
    }

    @Test
    public void di() {
        Set<Class<?>> givenClasses = getTypesAnnotatedWith(Controller.class, Service.class, Repository.class);
        BeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry();
        for (Class<?> givenClass : givenClasses) {
            registry.registerBeanDefinition(givenClass, new SingletonBeanDefinition(givenClass));
        }

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();
        final var sampleController = beanFactory.getBean(SampleController.class);

        assertNotNull(sampleController);
        assertNotNull(sampleController.getSampleService());

        final var sampleService = sampleController.getSampleService();
        assertNotNull(sampleService.getSampleRepository());
    }

    @SuppressWarnings("unchecked")
    private Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation>... annotations) {
        Reflections reflections = new Reflections("samples");
        Set<Class<?>> beans = new HashSet<>();
        for (Class<? extends Annotation> annotation : annotations) {
            beans.addAll(reflections.getTypesAnnotatedWith(annotation));
        }
        return beans;
    }
}
