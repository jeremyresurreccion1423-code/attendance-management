package com.attendance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class SharedAuthDataSourceMigrationProcessor implements BeanPostProcessor {

    private static final AtomicBoolean MIGRATED = new AtomicBoolean(false);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof DataSource) || !MIGRATED.compareAndSet(false, true)) {
            return bean;
        }
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/phase3-shared-auth.sql"));
            populator.setContinueOnError(true);
            populator.execute((DataSource) bean);
            log.info("Applied Phase 3 shared auth schema migration");
        } catch (Exception ex) {
            log.warn("Phase 3 schema migration skipped or partially applied: {}", ex.getMessage());
        }
        return bean;
    }
}
