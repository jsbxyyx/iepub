package io.github.jsbxyyx.iepub;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import com.intellij.ui.jcef.JBCefJSQuery;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.*;
import java.util.function.Function;

public class JcefBrowser implements Disposable {

    private JBCefBrowser browser;
    private CefBrowser cefBrowser;
    private CefClient cefClient;

    JcefBrowser() {
        browser = new JBCefBrowser("about:blank");
        cefBrowser = browser.getCefBrowser();
        JBCefClient jbCefClient = browser.getJBCefClient();
        cefClient = jbCefClient.getCefClient();
    }

    public JComponent getComponent() {
        return browser.getComponent();
    }

    public void loadHtml(String html) {
        browser.loadHTML(html);
    }

    public void keydown(Function<Integer, Void> f) {
        JBCefJSQuery keydown = JBCefJSQuery.create(browser);
        keydown.addHandler((args) -> {
            int keyCode = Integer.parseInt(args);
            if (f != null) f.apply(keyCode);
            return new JBCefJSQuery.Response("1");
        });

        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                browser.executeJavaScript(
                        "window.java = {" +
                                "keydown:function(arg){" +
                                keydown.inject("arg",
                                        "(response) => {console.log(response);}",
                                        "(error_code, error_message) => {console.log(error_code, error_message);}") +
                                "}" +
                                "};" +
                                "" +
                                "window.addEventListener('keydown', function(e) {" +
                                "window.java.keydown(e.keyCode);" +
                                "if (e.keyCode == 49) {" +
                                "dark();" +
                                "} else if (e.keyCode == 50) {" +
                                "light();" +
                                "}" +
                                "}, true);" +
                                "" +
                                "function dark() {" +
                                "document.querySelector('body').style.background = '#17181a';" +
                                "document.querySelector('body').style.color = '#f6f7f9';" +
                                "document.querySelectorAll('a').forEach(a => {a.style.color = '#4e7cd0';});" +
                                "};" +
                                "" +
                                "function light() {" +
                                "document.querySelector('body').style.background = '#f6f7f9';" +
                                "document.querySelector('body').style.color = '#17181a';" +
                                "document.querySelectorAll('a').forEach(a => {a.style.color = '#4e7cd0';});" +
                                "};" +
                                "" +
                                "dark();" +
                                "" +
                                "", "", 0);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {

            }
        }, browser.getCefBrowser());
    }

    @Override
    public void dispose() {
        cefClient.removeLoadHandler();
        cefBrowser.stopLoad();
        cefBrowser.close(false);
        Disposer.dispose(browser);
    }

}