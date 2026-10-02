import SwiftUI
import WebKit

// ── The only knob you'll usually touch ────────────────────────────────────
private let startURL = URL(string: "https://web.snapchat.com/")!
// ───────────────────────────────────────────────────────────────────────────

struct ContentView: View {
    var body: some View {
        WebView(url: startURL)
            .ignoresSafeArea()
    }
}

/// web.snapchat.com gates on screen WIDTH, not just the UA. The fix is
/// preferredContentMode = .desktop — the programmatic "Request Desktop Website":
/// desktop UA + desktop viewport in one. Viability probe — see README.
struct WebView: UIViewRepresentable {
    let url: URL

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.websiteDataStore = .default()
        config.defaultWebpagePreferences.preferredContentMode = .desktop   // THE FIX

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.allowsBackForwardNavigationGestures = true
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        func webView(_ webView: WKWebView,
                     decidePolicyFor navigationAction: WKNavigationAction,
                     preferences: WKWebpagePreferences,
                     decisionHandler: @escaping (WKNavigationActionPolicy, WKWebpagePreferences) -> Void) {
            preferences.preferredContentMode = .desktop
            decisionHandler(.allow, preferences)
        }

        @available(iOS 15.0, *)
        func webView(_ webView: WKWebView,
                     requestMediaCapturePermissionFor origin: WKSecurityOrigin,
                     initiatedByFrame frame: WKFrameInfo,
                     type: WKMediaCaptureType,
                     decisionHandler: @escaping (WKPermissionDecision) -> Void) {
            decisionHandler(.grant)
        }
    }
}
