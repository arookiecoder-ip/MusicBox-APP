package in.synthora.musicbox;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.ResultReceiver;

/** Standalone test-APK process: framework APIs only, with no target-APK libraries. */
public final class UntrustedControllerProbeService extends Service {
    private MediaBrowser browser;

    @Override
    @SuppressWarnings("deprecation")
    public IBinder onBind(Intent intent) {
        final ResultReceiver receiver = intent.getParcelableExtra("result");
        if (receiver == null) return new Binder();
        browser = new MediaBrowser(this,
            new ComponentName("in.synthora.musicbox", "in.synthora.musicbox.services.PlaybackService"),
            new MediaBrowser.ConnectionCallback() {
                @Override public void onConnected() { report(false); }
                @Override public void onConnectionFailed() { report(true); }
                @Override public void onConnectionSuspended() { report(false); }
                private void report(boolean rejected) {
                    Bundle result = new Bundle();
                    result.putInt("uid", Process.myUid());
                    result.putBoolean("rejected", rejected);
                    receiver.send(0, result);
                    browser.disconnect();
                }
            }, Bundle.EMPTY);
        browser.connect();
        return new Binder();
    }
    @Override public void onDestroy() {
        if (browser != null) browser.disconnect();
        super.onDestroy();
    }
}
