package io.github.jsbxyyx.iepub;

import com.intellij.openapi.Disposable;
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

    public void executeJavaScript(String script) {
        cefBrowser.executeJavaScript(script, null, 0);
    }

    public void initJava(Function<Integer, Void> keydownFun, Function<Integer, Void> pageYFun) {
        JBCefJSQuery keydown = JBCefJSQuery.create(browser);
        keydown.addHandler((args) -> {
            int keyCode = Integer.parseInt(args);
            if (keydownFun != null) keydownFun.apply(keyCode);
            return new JBCefJSQuery.Response("1");
        });

        JBCefJSQuery pageY = JBCefJSQuery.create(browser);
        pageY.addHandler((args) -> {
            int pagey = Integer.parseInt(args);
            if (pageYFun != null) pageYFun.apply(pagey);
            return new JBCefJSQuery.Response("1");
        });

        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
                browser.executeJavaScript(
                        "window.java = {" +
                                "keydown: function(arg) {" +
                                keydown.inject("arg",
                                        "(response) => {console.log(response);}",
                                        "(error_code, error_message) => {console.log(error_code, error_message);}") +
                                "}," +
                                "" +
                                "pageY: function() {" +
                                "var arg = window.pageYOffset;" +
                                pageY.inject("arg",
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
                                "document.querySelectorAll('a').forEach(a => {a.style.color = '#4e7cd0';a.href='javascript:void(0);';});" +
                                "};" +
                                "" +
                                "dark();" +
                                "" +
                                "", null, 0);
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {

            }
        }, browser.getCefBrowser());
    }

    @Override
    public void dispose() {
        PropertiesUtil.log("browser dispose");
//        cefClient.removeLoadHandler();
//        cefBrowser.stopLoad();
//        cefBrowser.close(false);
//        Disposer.dispose(browser);
    }

}