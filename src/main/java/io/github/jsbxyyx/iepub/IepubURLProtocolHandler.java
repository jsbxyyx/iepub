package io.github.jsbxyyx.iepub;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public final class IepubURLProtocolHandler {


    private IepubURLProtocolHandler() {
    }

    public static void install() {
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return IepubConnection.IEPUB.equals(protocol) ? new URLStreamHandler() {
                    protected URLConnection openConnection(URL url) throws IOException {
                        return new IepubConnection(url);
                    }
                } : null;
            }
        });
    }

}