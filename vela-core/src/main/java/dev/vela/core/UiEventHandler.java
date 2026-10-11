package dev.vela.core;

@FunctionalInterface
public interface UiEventHandler {
    void handle(UiEvent event);
}
