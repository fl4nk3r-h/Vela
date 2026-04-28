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

import dev.vela.annotation.State;
import dev.vela.components.VelaButton;
import dev.vela.components.VelaColumn;
import dev.vela.components.VelaText;
import dev.vela.core.VelaComponent;
import dev.vela.core.ViewNode;

/**
 * /**
 * A simple counter component that can be incremented and reset.
 *
 * <p>
 * Uses the {@code @State} annotation to manage the count state
 * and marks itself as dirty whenever the count changes,
 * prompting a re-render when necessary.
 *
 * @author fl4nk3r
 * @version 1.0.0
 * @since 1.0
 */
public final class CounterScreen extends VelaComponent {
    @State
    private int count;

    /**
     * Returns the current count value.
     *
     * @return the current count
     */
    public int count() {
        return count;
    }

    /**
     * Increments the count by 1.
     */
    public void increment() {
        count++;
        markDirty();
    }

    /**
     * Resets the count to 0.
     */
    public void reset() {
        count = 0;
        markDirty();
    }

    /**
     * Renders the counter component.
     * {@inheritDoc }
     * 
     * @return the view node representing the counter
     */
    @Override
    public ViewNode render() {
        return VelaColumn.of(
                VelaText.of("Counter"),
                VelaText.of("Current value: " + count),
                VelaButton.of("Increment"));
    }
}