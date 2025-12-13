package com.guild.core.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Szyna zdarzeń - ujednolicone zarządzanie wewnętrznymi zdarzeniami pluginu
 */
public class EventBus {

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> listeners = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(EventBus.class.getName());

    /**
     * Zarejestruj słuchacza zdarzeń
     */
    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.info("Zarejestrowano słuchacza zdarzeń: " + eventType.getSimpleName());
    }

    /**
     * Wyrejestruj słuchacza zdarzeń
     */
    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            logger.info("Wyrejestrowano słuchacza zdarzeń: " + eventType.getSimpleName());
        }
    }

    /**
     * Opublikuj zdarzenie
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<?> listener : eventListeners) {
                try {
                    ((Consumer<T>) listener).accept(event);
                } catch (Exception e) {
                    logger.severe("Błąd wykonania słuchacza zdarzeń: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Opublikuj zdarzenie asynchronicznie
     */
    public <T> void publishAsync(T event) {
        new Thread(() -> publish(event)).start();
    }

    /**
     * Wyczyść wszystkich słuchaczy
     */
    public void clear() {
        listeners.clear();
        logger.info("Wyczyszczono wszystkich słuchaczy zdarzeń");
    }

    /**
     * Pobierz liczbę słuchaczy
     */
    public int getListenerCount(Class<?> eventType) {
        CopyOnWriteArrayList<Consumer<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    /**
     * Pobierz całkowitą liczbę słuchaczy
     */
    public int getTotalListenerCount() {
        return listeners.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
    }
}
