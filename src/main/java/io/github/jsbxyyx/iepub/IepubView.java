package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IepubView extends JPanel {

    private EpubReader reader = new EpubReader();

    private JButton btnFile;
    private JButton btnRefresh;
    private JTextField tfFile;

    private JTree treeToc;
    private JcefBrowser browser;
    private JComponent browserComponent;

    private JButton btnLeft;
    private JButton btnRight;

    private JButton btnFirst;
    private JButton btnLast;

    private int current = 0;

    private static final String xml = "<?xml version='1.0' encoding='utf-8'?>";

    private static final ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(1);


    public IepubView() {
        IepubURLProtocolHandler.install();

        setPreferredSize(new Dimension(100, 100));
        setLayout(new BorderLayout());

        JPanel n = new JPanel();
        add(n, BorderLayout.NORTH);
        tfFile = new JTextField(20);
        n.add(tfFile);

        btnFile = new JButton("Open");
        btnFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("epub", "epub"));
            int result = fileChooser.showDialog(null, null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                tfFile.setText(file.getAbsolutePath());
                openBook(file);
            }
        });
        n.add(btnFile);

        btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> {
            if (BookHolder.getBook() != null) {
                int index = BookHolder.getIndex();
                openContent(index, 0);
            }
        });
        n.add(btnRefresh);

        browser = new JcefBrowser();
        browserComponent = browser.getComponent();
        browser.initJava((code) -> {
            if (code == KeyEvent.VK_LEFT) {
                prev();
            } else if (code == KeyEvent.VK_RIGHT) {
                next();
            }
            return null;
        }, (pagey) -> {
            setProgress(pagey);
            return null;
        });
        add(browserComponent, BorderLayout.CENTER);


        JPanel s = new JPanel();
        add(s, BorderLayout.SOUTH);

        btnFirst = new JButton("First");
        btnFirst.addActionListener(e -> {
            first();
        });
        s.add(btnFirst);

        btnLast = new JButton("Last");
        btnLast.addActionListener(e -> {
            last();
        });
        s.add(btnLast);

        btnLeft = new JButton("Left");
        btnLeft.addActionListener(e -> {
            prev();
        });
        s.add(btnLeft);

        btnRight = new JButton("Right");
        btnRight.addActionListener(e -> {
            next();
        });
        s.add(btnRight);
    }

    public void openBook(File file) {
        try {
            Book book = reader.readEpub(new FileInputStream(file));
            BookHolder.setBook(book);
        } catch (IOException e) {
            PropertiesUtil.log("open book failed. " + e.getMessage());
            return;
        }
        current = 0;
        int pagey = 0;
        String progress = getProgress();
        if (progress != null && !"".equals(progress.trim())) {
            String[] split = progress.split("\\,");
            current = Integer.parseInt(split[0]);
            pagey = Integer.parseInt(split[1]);
        }
        openContent(current, pagey);

        if (!BookHolder.getStart()) {
            scheduled.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        browser.executeJavaScript("window.java.pageY();");
                    } catch (Throwable e) {
                        PropertiesUtil.log("schedule error. " + e.getMessage());
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);
            BookHolder.setStart(true);
        }
    }

    public void openContent(int index, int pagey) {
        try {
            BookHolder.setIndex(index);
            Spine spine = BookHolder.getBook().getSpine();
            SpineReference spineReference = spine.getSpineReferences().get(index);
            Resource resource = spineReference.getResource();
            String data = new String(resource.getData(), resource.getInputEncoding());
            data = data.replace(xml, "");
            data = convertImg(data, (src) -> {
                byte[] srcData = new byte[0];
                try {
                    srcData = BookHolder.getBook().getResources().getByIdOrHref(src).getData();
                } catch (IOException ignore) {
                }
                return "data:image/png;base64, " + Base64.getEncoder().encodeToString(srcData);
            });
            System.out.println(data);
            browser.loadHtml(data);
            // TODO page offset go to pageY
            goTop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goTop() {
    }

    public void first() {
        current = 0;
        openContent(current, 0);
    }

    public void last() {
        current = BookHolder.getBook().getSpine().size() - 1;
        openContent(current, 0);
    }

    public void prev() {
        if (current <= 0) {
            return;
        }
        current--;
        openContent(current, 0);
    }

    public void next() {
        if (current >= BookHolder.getBook().getSpine().size() - 1) {
            return;
        }
        current++;
        openContent(current, 0);
    }

    public void setProgress(int progress) {
        String identifier = getIdentifier(BookHolder.getBook());
        PropertiesUtil.setValue("p" + identifier, BookHolder.getIndex() + "," + progress);
    }

    public String getProgress() {
        String identifier = getIdentifier(BookHolder.getBook());
        String value = PropertiesUtil.getValue("p" + identifier);
        return value;
    }

    private String getIdentifier(Book book) {
        Metadata metadata = book.getMetadata();
        String title = metadata.getTitles().get(0).trim();
        String author = metadata.getAuthors().get(0).toString().trim();
        String identifier = title + "-" + author;
        return identifier;
    }

    public static String convertImg(String content, Function<String, String> f) {
        String patternStr = "<img\\s*([^>]*)\\s*src=\\\"(.*?)\\\"\\s*([^>]*)>";
        Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        String result = content;
        while (matcher.find()) {
            String src = matcher.group(2);
            String img = matcher.group(0);
            String img2 = matcher.group(0);
            String replaceSrc = "";
            if (!src.startsWith("http:") && !src.startsWith("https:")) {
                replaceSrc = f.apply(src);
                img = img.replaceAll(src, replaceSrc);
                result = result.replaceAll(img2, img);
            }
        }
        return result;
    }

}