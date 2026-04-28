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

import java.util.logging.Logger;

import dev.vela.core.VelaRenderer;

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
     * <p>
     * The main method initializes the VelaRenderer and CounterScreen, renders the
     * initial state, increments the counter, and renders the updated state if the
     * component is marked as dirty.
     *
     * @param args
     */
    public static void main(String[] args) {
        VelaRenderer renderer = new VelaRenderer();
        CounterScreen counterScreen = new CounterScreen();

        logger.info(renderer.render(counterScreen.render()));

        counterScreen.increment();
        if (counterScreen.isDirty()) {
            counterScreen.consumeDirty();
            logger.info(renderer.render(counterScreen.render()));
        }
    }
}