package org.thoughtcrime.securesms.gcm;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import org.signal.core.util.logging.Log;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class FcmUtil {

  private static final String TAG = Log.tag(FcmUtil.class);

  private static final long FCM_TOKEN_TIMEOUT_SECONDS = 5;

  /**
   * Retrieves the current FCM token. If one isn't available, it'll be generated.
   * Returns empty Optional if FCM is unavailable or times out.
   */
  @WorkerThread
  public static Optional<String> getToken(Context context) {
    String token = null;

    // Must be called manually if running outside of main process
    FirebaseApp.initializeApp(context);

    try {
      token = Tasks.await(FirebaseMessaging.getInstance().getToken(), FCM_TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Log.w(TAG, "Was interrupted while waiting for the token.");
    } catch (ExecutionException e) {
      Log.w(TAG, "Failed to get the token.", e.getCause());
    } catch (TimeoutException e) {
      Log.w(TAG, "FCM token request timed out after " + FCM_TOKEN_TIMEOUT_SECONDS + " seconds");
    }

    return Optional.ofNullable(TextUtils.isEmpty(token) ? null : token);
  }
}
