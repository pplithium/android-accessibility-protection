package no.ck.accessibilityprotection;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * A utility class to determine if accessibility events should be blocked and
 * to apply this security policy to an Activity.
 */
public final class AccessibilityEventBlocker {

    private static final String TAG = "AccessibilityBlocker";
    private static final String TALKBACK_SIGNATURE_HASH_SHA256 = "3d6c1b35787b415286595a847250b2990494541517451296a202a6b251a37c44";
    private static final String TALKBACK_PACKAGE_NAME = "com.google.android.marvin.talkback";

    private AccessibilityEventBlocker() {}

    /**
     * Applies the accessibility security policy to an entire Activity.
     * This method attaches a delegate to the root view of the activity to intercept
     * and potentially block accessibility events.
     *
     * @param activity The activity to protect.
     */
    public static void applySecurityToActivity(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot apply security to a null activity.");
            return;
        }

        View rootView = activity.getWindow().getDecorView().getRootView();

        rootView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            /**
             * This method is called when an accessibility service requests information
             * about a view. We can intercept this and provide no information if
             * events should be blocked.
             */
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfo info) {
                if (AccessibilityEventBlocker.shouldBlockAccessibilityEvents(activity.getApplicationContext())) {
                    // To block, we simply do not call the super method.
                    // This leaves the AccessibilityNodeInfo empty, effectively hiding the view.
                    return;
                }
                super.onInitializeAccessibilityNodeInfo(host, info);
            }

            /**
             * This is the primary gatekeeper for events. It's called before any
             * accessibility event is sent from a view or any of its children.
             */
            @Override
            public boolean onRequestSendAccessibilityEvent(@NonNull ViewGroup host, @NonNull View child, @NonNull AccessibilityEvent event) {
                if (AccessibilityEventBlocker.shouldBlockAccessibilityEvents(activity.getApplicationContext())) {
                    // Block the event by returning false.
                    return false;
                }
                // Allow the event by calling the default implementation.
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
        });
    }

    /**
     * Checks if accessibility events should be blocked according to the defined policy.
     * @param context The application context.
     * @return true if events should be blocked, false otherwise.
     */
    public static boolean shouldBlockAccessibilityEvents(Context context) {
        if (context == null) {
            Log.w(TAG, "Context is null. Blocking events as a precaution.");
            return true;
        }

        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) {
            Log.w(TAG, "AccessibilityManager is not available. Blocking events as a precaution.");
            return true;
        }

        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        if (enabledServices == null || enabledServices.size() != 1) {
            if (enabledServices != null && enabledServices.size() > 1) {
                Log.w(TAG, "Blocking events: More than one accessibility service is enabled.");
            } else if (enabledServices == null || enabledServices.isEmpty()) {
                return false;
            }
            return true;
        }

        AccessibilityServiceInfo serviceInfo = enabledServices.get(0);
        String servicePackageName = serviceInfo.getResolveInfo().serviceInfo.packageName;

        if (!TALKBACK_PACKAGE_NAME.equals(servicePackageName)) {
            Log.w(TAG, "Blocking events: Enabled service is not TalkBack (package name mismatch). Found: " + servicePackageName);
            return true;
        }

        String serviceSignatureHash = getPackageSignatureHash(context, servicePackageName);
        if (serviceSignatureHash == null) {
            Log.e(TAG, "Blocking events: Could not compute signature hash for package: " + servicePackageName);
            return true;
        }

        if (TALKBACK_SIGNATURE_HASH_SHA256.equalsIgnoreCase(serviceSignatureHash)) {
            Log.i(TAG, "Allowing events: Verified Google TalkBack is the only enabled service.");
            return false;
        } else {
            Log.w(TAG, "Blocking events: The signature hash of the enabled service does not match the expected TalkBack hash.");
            return true;
        }
    }

    private static String getPackageSignatureHash(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            Signature[] signatures = packageInfo.signatures;
            if (signatures == null || signatures.length == 0) return null;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(signatures[0].toByteArray());
            byte[] digest = md.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (PackageManager.NameNotFoundException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to get signature hash for " + packageName, e);
        }
        return null;
    }
}

