/*
 * Copyright (c) 2026, fl4nk3r. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.vela.app;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import dev.vela.core.TextRendererBackend;
import dev.vela.core.VelaRuntime;

/**
 * App is the entry point of the application that demonstrates the usage of the
 * CounterScreen component.
 *
 * @author fl4nk3r
 * @version 1.0.0
 * @since 1.0
 */
public final class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    /**
     * Mounts the counter, dispatches a click, and drains the deferred render queue.
     *
     * @param args
     */
    public static void main(String[] args) {
        CounterScreen counterScreen = new CounterScreen();
        TextRendererBackend backend = new TextRendererBackend();
        Deque<Runnable> frames = new ArrayDeque<>();
        try (VelaRuntime<TextRendererBackend.Node> runtime = new VelaRuntime<>(counterScreen, backend, frames::addLast)) {
            runtime.mount();
            logger.info(backend.output());
            runtime.root().children().get(2).dispatch("click");
            while (!frames.isEmpty()) {
                frames.removeFirst().run();
            }
            logger.info(backend.output());
        }
    }
}
