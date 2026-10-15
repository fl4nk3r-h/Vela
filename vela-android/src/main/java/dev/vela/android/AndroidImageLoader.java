package dev.vela.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import android.graphics.drawable.Drawable;

/** Bounded asynchronous resource/content/file/asset/HTTPS loading. Apps opt into network permission. */
final class AndroidImageLoader implements ImageLoader, AutoCloseable {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService workers = Executors.newFixedThreadPool(2);

    AndroidImageLoader(Context context) { this.context = context.getApplicationContext(); }

    @Override public AutoCloseable load(String source, Consumer<Drawable> result, Consumer<Exception> failure) {
        AtomicBoolean cancelled = new AtomicBoolean();
        Future<?> task = workers.submit(() -> {
            try {
                Drawable drawable = read(source);
                main.post(() -> { if (!cancelled.get()) result.accept(drawable); });
            } catch (Exception error) {
                main.post(() -> { if (!cancelled.get()) failure.accept(error); });
            }
        });
        return () -> { cancelled.set(true); task.cancel(true); };
    }

    private Drawable read(String source) throws Exception {
        if (source.startsWith("@drawable/")) {
            int id = context.getResources().getIdentifier(source.substring(10), "drawable", context.getPackageName());
            if (id == 0) throw new IllegalArgumentException("Unknown drawable resource");
            return context.getDrawable(id);
        }
        HttpURLConnection connection = null;
        InputStream stream;
        Uri uri = Uri.parse(source);
        if ("https".equals(uri.getScheme())) {
            connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != 200) {
                connection.disconnect();
                throw new IllegalArgumentException("Image request failed; redirects require an injected loader");
            }
            stream = connection.getInputStream();
        } else if ("content".equals(uri.getScheme()) || "file".equals(uri.getScheme())
                || "android.resource".equals(uri.getScheme())) {
            stream = context.getContentResolver().openInputStream(uri);
        } else if (uri.getScheme() == null) {
            stream = context.getAssets().open(source);
        } else {
            throw new IllegalArgumentException("Unsupported image scheme");
        }
        try (InputStream input = stream; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalArgumentException("Image source not found");
            byte[] buffer = new byte[8192];
            int size;
            while ((size = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Image cancelled");
                if (bytes.size() + size > 10 * 1024 * 1024) throw new IllegalArgumentException("Image exceeds 10 MiB");
                bytes.write(buffer, 0, size);
            }
            byte[] data = bytes.toByteArray();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);
            options.inSampleSize = 1;
            while (options.outWidth / options.inSampleSize > 2048 || options.outHeight / options.inSampleSize > 2048) {
                options.inSampleSize *= 2;
            }
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if (bitmap == null) throw new IllegalArgumentException("Unsupported image data");
            return new BitmapDrawable(context.getResources(), bitmap);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override public void close() { workers.shutdownNow(); }
}
