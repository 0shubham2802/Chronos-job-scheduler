package com.chronos.chronos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    // ── JDBC-backed Quartz (production) ───────────────────────────────────────
    // Only created when spring.quartz.job-store-type=jdbc (the default in
    // application.properties). In tests we override this to "memory" so this
    // bean is *not* created, avoiding SchedulerConfigException.
    //
    // KEY INSIGHT: When you call factory.setDataSource(), Spring's
    // SchedulerFactoryBean internally registers the datasource under a
    // synthetic name and injects it into Quartz via a LocalDataSourceJobStore
    // wrapper (which extends JobStoreCMT). You must NOT set
    // org.quartz.jobStore.class or org.quartz.jobStore.dataSource in the
    // Quartz properties — Spring handles that automatically.
    @Bean
    @ConditionalOnProperty(
            name        = "spring.quartz.job-store-type",
            havingValue = "jdbc",
            matchIfMissing = true   // treat missing property as "jdbc" (production default)
    )
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // Providing a DataSource causes SchedulerFactoryBean to automatically
        // use LocalDataSourceJobStore (Spring's JDBC store wrapper).
        // Do NOT set org.quartz.jobStore.class — Spring overrides it internally.
        factory.setDataSource(dataSource);
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true);

        // Only set properties that are NOT related to datasource wiring —
        // Spring handles all the jobStore.class / jobStore.dataSource plumbing.
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName",          "ChronosScheduler");
        props.setProperty("org.quartz.scheduler.instanceId",            "AUTO");
        props.setProperty("org.quartz.jobStore.isClustered",            "true");
        props.setProperty("org.quartz.jobStore.clusterCheckinInterval", "10000");
        props.setProperty("org.quartz.jobStore.tablePrefix",            "QRTZ_");
        props.setProperty("org.quartz.jobStore.driverDelegateClass",
                "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        props.setProperty("org.quartz.threadPool.threadCount",          "10");

        factory.setQuartzProperties(props);
        return factory;
    }

    // ── In-memory Quartz (tests) ──────────────────────────────────────────────
    // Created when spring.quartz.job-store-type=memory (set in
    // application-test.properties). No DataSource needed — RAMJobStore keeps
    // everything in memory.
    @Bean
    @ConditionalOnProperty(
            name        = "spring.quartz.job-store-type",
            havingValue = "memory"
    )
    public SchedulerFactoryBean schedulerFactoryBeanMemory(
            @Value("${spring.quartz.auto-startup:true}") boolean autoStartup) {

        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(autoStartup);

        // RAMJobStore — no JDBC, no clustering.
        // Note: RAMJobStore does NOT support isClustered; omit it entirely.
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "ChronosSchedulerTest");
        props.setProperty("org.quartz.scheduler.instanceId",  "NON_CLUSTERED");
        props.setProperty("org.quartz.jobStore.class",
                "org.quartz.simpl.RAMJobStore");
        props.setProperty("org.quartz.threadPool.threadCount", "2");

        factory.setQuartzProperties(props);
        return factory;
    }
}
