# Android Accessibility Blocker Example

This Android project demonstrates a security technique to selectively block accessibility services from interacting with an application. The goal is to allow legitimate, trusted services like Google TalkBack to function normally while preventing potentially malicious accessibility services (malware) from monitoring user actions or exfiltrating data from the app.

---

## ⚠️ Important Warning

This project is an **educational example** and should be used with extreme caution. The logic relies on a hardcoded package name and signature hash for Google TalkBack, which can change over time. Before using this in a production application, you must:

1.  **Thoroughly test** its impact on legitimate accessibility users.
2.  **Verify and update** the signature hash to match the versions of TalkBack you intend to support.
3.  Understand that this approach might inadvertently block other legitimate assistive technologies that your users may rely on.

Implementing this without careful consideration can negatively affect your app's accessibility for users with disabilities.

---

## What it Does

The code implements a security policy with the following rules:

-   **ALLOW** events if **only one** accessibility service is enabled and that service is verified to be the official Google TalkBack.
-   **BLOCK** events if multiple accessibility services are enabled.
-   **BLOCK** events if a single service is enabled, but it is not the official Google TalkBack.

Verification is done by checking both the package name (`com.google.android.marvin.talkback`) and the SHA-256 signature hash of the installed TalkBack application.

---

## How it Works

The solution is implemented in two main parts:

### 1. The Logic (`AccessibilityEventBlocker.java`)

This utility class contains all the decision-making logic.

-   `shouldBlockAccessibilityEvents()`: Checks the list of currently enabled accessibility services.
-   `getPackageSignatureHash()`: Computes the signature hash of an installed package to verify its authenticity.

### 2. The Implementation (`SecureMainActivity.java`)

To apply the blocking logic to an entire activity, the `applySecurityToActivity()` method attaches a `View.AccessibilityDelegate` to the activity's root view. This delegate acts as a gatekeeper for all accessibility events within the activity, intercepting them and consulting `AccessibilityEventBlocker` before allowing them to proceed.

---

## Usage

To protect an activity, simply call the static method from your `onCreate` method after `setContentView()`:

```java
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SecureMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply the accessibility security policy to this activity.
        AccessibilityEventBlocker.applySecurityToActivity(this);
    }
}
