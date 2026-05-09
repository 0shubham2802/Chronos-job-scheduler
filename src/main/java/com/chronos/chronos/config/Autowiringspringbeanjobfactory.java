package com.chronos.chronos.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

// This class makes Quartz use Spring to create job instances
// So @Autowired works inside ChronosJobExecutor
public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory
        implements ApplicationContextAware {

    private transient AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        beanFactory = context.getAutowireCapableBeanFactory();
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        // Create the job instance the normal way
        Object job = super.createJobInstance(bundle);
        // Then autowire all @Autowired fields into it
        beanFactory.autowireBean(job);
        return job;
    }
}