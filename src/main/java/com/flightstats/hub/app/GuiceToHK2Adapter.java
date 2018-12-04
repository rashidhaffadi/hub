package com.flightstats.hub.app;

import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
class GuiceToHK2Adapter extends AbstractBinder {

    private final Injector injector;

    GuiceToHK2Adapter(Injector injector) {
        this.injector = injector;
    }

    @Override
    protected void configure() {
        injector.getBindings().forEach((key, value) -> {
            if (isNamedBinding(key)) {
                bindNamedClass(key);
            } else {
                bindClass(key);
            }
        });
    }

    private boolean isNamedBinding(Key<?> key) {
        return key.getAnnotationType() != null && key.getAnnotationType().getSimpleName().equals("Named");
    }

    @SneakyThrows
    private void bindClass(Key<?> key) {
        try {
            String typeName = key.getTypeLiteral().getType().getTypeName();
            log.info("mapping guice to hk2: {}", typeName);
            Class boundClass = Class.forName(typeName);
            bindFactory(new ServiceFactory<>(boundClass)).to(boundClass);
        } catch (ClassNotFoundException e) {
            log.warn("unable to bind {}", key);
            throw e;
        }
    }

    @SneakyThrows
    private void bindNamedClass(Key<?> key) {
        try {
            String typeName = key.getTypeLiteral().getType().getTypeName();
            Method value = key.getAnnotationType().getDeclaredMethod("value");
            String name = (String) value.invoke(key.getAnnotation());
            log.info("mapping guice to hk2: {} (named: {})", typeName, name);
            Class boundClass = Class.forName(typeName);
            bindFactory(new ServiceFactory<>(boundClass)).to(boundClass).named(name);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.warn("unable to bind {}", key);
            throw e;
        }
    }

    private class ServiceFactory<T> implements Factory<T> {

        private final AtomicBoolean hasBeenProvided = new AtomicBoolean(false);
        private final Class<T> serviceClass;

        ServiceFactory(Class<T> serviceClass) {
            this.serviceClass = serviceClass;
        }

        public T provide() {
            if (isFirstProvide()) {
                log.debug("providing {} for the first time", serviceClass.getCanonicalName());
            }
            return injector.getInstance(serviceClass);
        }

        private boolean isFirstProvide() {
            return hasBeenProvided.compareAndSet(false, true);
        }

        public void dispose(T versionResource) {
            // do nothing
        }
    }

}