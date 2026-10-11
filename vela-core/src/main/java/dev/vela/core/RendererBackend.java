package dev.vela.core;

import java.util.List;

public interface RendererBackend<H> {
    /** Creates one host object and applies initial props/events; children are mounted separately. */
    H create(ViewNode node);

    /** Refreshes props and event callbacks on the existing object, even when visual props are equal. */
    void update(H handle, ViewNode previous, ViewNode next);

    /** Applies the ordered managed children without recreating retained handles or disposing removed ones. */
    void setChildren(H parent, List<H> children);

    // A null root detaches the mounted tree from its platform container.
    void setRoot(H root);

    /** Releases resources and removes event listeners after the handle has been detached. */
    void dispose(H handle);
}
