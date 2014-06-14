package fi.seweb.client.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class UIThread {
    private static Handler sHandler;

    public static Handler getHandler() {
        if (sHandler == null) {
            synchronized (UIThread.class) {
                if (sHandler == null) {
                    sHandler = new Handler(Looper.getMainLooper());
                }
            }
        }

        return sHandler;
    }

    public static void run(Runnable command) {
        getHandler().post(command);
    }

    public static void toast(final Context context, final String msg, final int duration) {
        run(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, duration).show();
            }
        });
    }
}