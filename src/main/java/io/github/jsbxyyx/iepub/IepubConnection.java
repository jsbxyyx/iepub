package io.github.jsbxyyx.iepub;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IepubConnection extends URLConnection {

    public static final String IEPUB = "iepub";

    public static final String IEPUB_PROTOCOL = IEPUB + ":";

    private static final Pattern re = Pattern.compile(IEPUB + "\\:.*");

    private final Matcher m;

    public IepubConnection(final URL u) throws MalformedURLException {
        super(u);
        m = re.matcher(url.toString());
        connected = m.matches();
        if (!connected) {
            throw new MalformedURLException("Wrong data protocol URL");
        }
    }

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getData());
    }

    private byte[] getData() {
        String path = url.getPath();
        byte[] data = new byte[0];
        try {
            data = BookHolder.getBook().getResources().getByIdOrHref(path).getData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

}