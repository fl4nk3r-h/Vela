package dev.vela.android;

import android.os.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.vela.core.StateSnapshot;

public final class AndroidState {
    private AndroidState() { }

    public static Bundle encode(StateSnapshot snapshot) {
        Bundle values = new Bundle();
        snapshot.values().forEach(values::putString);
        Bundle children = new Bundle();
        snapshot.children().forEach((key, child) -> children.putBundle(key, encode(child)));
        Bundle result = new Bundle();
        result.putBundle("values", values);
        result.putBundle("children", children);
        Bundle codecs = new Bundle();
        snapshot.codecs().forEach(codecs::putString);
        result.putBundle("codecs", codecs);
        return result;
    }

    public static StateSnapshot decode(Bundle bundle) {
        Map<String, String> values = new LinkedHashMap<>();
        Bundle savedValues = bundle.getBundle("values");
        if (savedValues != null) {
            for (String key : savedValues.keySet()) {
                String value = savedValues.getString(key);
                if (value != null) values.put(key, value);
            }
        }
        Map<String, StateSnapshot> children = new LinkedHashMap<>();
        Bundle savedChildren = bundle.getBundle("children");
        if (savedChildren != null) {
            for (String key : savedChildren.keySet()) {
                Bundle child = savedChildren.getBundle(key);
                if (child != null) children.put(key, decode(child));
            }
        }
        Map<String, String> codecs = new LinkedHashMap<>();
        Bundle savedCodecs = bundle.getBundle("codecs");
        if (savedCodecs != null) for (String key : savedCodecs.keySet()) {
            String codec = savedCodecs.getString(key);
            if (codec != null) codecs.put(key, codec);
        }
        return new StateSnapshot(values, children, codecs);
    }
}
