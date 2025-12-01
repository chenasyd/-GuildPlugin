package com.guild.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Kontener usług - zarządza cyklem życia wszystkich usług i wstrzykiwaniem zależności
 */
public class ServiceContainer {

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Map<Class<?>, ServiceLifecycle> lifecycles = new HashMap<>();
    private final Logger logger = Logger.getLogger(ServiceContainer.class.getName());

    /**
     * Zarejestruj usługę
     */
    public <T> void register(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
        logger.info("Zarejestrowano usługę: " + serviceClass.getSimpleName());
    }

    /**
     * Zarejestruj usługę z cyklem życia
     */
    public <T> void register(Class<T> serviceClass, T service, ServiceLifecycle lifecycle) {
        services.put(serviceClass, service);
        lifecycles.put(serviceClass, lifecycle);
        logger.info("Zarejestrowano usługę: " + serviceClass.getSimpleName() + " (z cyklem życia)");
    }

    /**
     * Pobierz usługę
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        T service = (T) services.get(serviceClass);
        if (service == null) {
            throw new ServiceNotFoundException("Nie znaleziono usługi: " + serviceClass.getName());
        }
        return service;
    }

    /**
     * Sprawdź, czy usługa istnieje
     */
    public boolean has(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    /**
     * Uruchom wszystkie usługi
     */
    public CompletableFuture<Void> startAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Uruchamianie wszystkich usług...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().start();
                    logger.info("Usługa uruchomiona pomyślnie: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("Nie udało się uruchomić usługi: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }

    /**
     * Zatrzymaj wszystkie usługi
     */
    public CompletableFuture<Void> stopAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Zatrzymywanie wszystkich usług...");
            for (Map.Entry<Class<?>, ServiceLifecycle> entry : lifecycles.entrySet()) {
                try {
                    entry.getValue().stop();
                    logger.info("Usługa zatrzymana pomyślnie: " + entry.getKey().getSimpleName());
                } catch (Exception e) {
                    logger.severe("Nie udało się zatrzymać usługi: " + entry.getKey().getSimpleName() + " - " + e.getMessage());
                }
            }
        });
    }

    /**
     * Wyłącz kontener usług
     */
    public void shutdown() {
        try {
            stopAll().get();
            services.clear();
            lifecycles.clear();
            logger.info("Kontener usług został zamknięty");
        } catch (Exception e) {
            logger.severe("Błąd podczas zamykania kontenera usług: " + e.getMessage());
        }
    }

    /**
     * Interfejs cyklu życia usługi
     */
    public interface ServiceLifecycle {
        void start() throws Exception;
        void stop() throws Exception;
    }

    /**
     * Wyjątek nieznalezionej usługi
     */
    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }
}
