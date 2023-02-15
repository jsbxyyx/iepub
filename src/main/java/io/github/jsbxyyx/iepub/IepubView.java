package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IepubView extends JPanel {

    private EpubReader reader = new EpubReader();

    private JButton btnFile;
    private JTextField tfFile;

    private JTree treeToc;
    private JcefBrowser browser;
    private JComponent browserComponent;

    private JButton btnLeft;
    private JButton btnRight;

    private Book book;
    private int current = 0;

    private static final String xml = "<?xml version='1.0' encoding='utf-8'?>";

    public IepubView() {
        IepubURLProtocolHandler.install();

        setPreferredSize(new Dimension(100, 100));
        setLayout(new BorderLayout());

        JPanel n = new JPanel();
        add(n, BorderLayout.NORTH);
        tfFile = new JTextField(20);
        n.add(tfFile);

        btnFile = new JButton("...");
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

        browser = new JcefBrowser();
        browserComponent = browser.getComponent();
        browser.keydown((code) -> {
            if (code == KeyEvent.VK_LEFT) {
                prev();
            } else if (code == KeyEvent.VK_RIGHT) {
                next();
            }
            return null;
        });
        add(browserComponent, BorderLayout.CENTER);


        JPanel s = new JPanel();
        add(s, BorderLayout.SOUTH);
        btnLeft = new JButton("left");
        btnLeft.addActionListener(e -> {
            prev();
        });
        s.add(btnLeft);

        btnRight = new JButton("right");
        btnRight.addActionListener(e -> {
            next();
        });
        s.add(btnRight);

    }

    public void openBook(File file) {
        try {
            book = reader.readEpub(new FileInputStream(file));
            BookHolder.setBook(book);
        } catch (IOException e) {
            e.printStackTrace();
        }
        current = 0;
        openContent(current);
    }

    public void openContent(int index) {
        try {
            List<Resource> contents = book.getContents();
            Resource resource = contents.get(index);
            String data = new String(resource.getData(), resource.getInputEncoding());
            data = data.replace(xml, "");
            data = convertImg(data, (src) -> {
                byte[] srcData = new byte[0];
                try {
                    srcData = book.getResources().getByIdOrHref(src).getData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "data:image/png;base64, " + Base64.getEncoder().encodeToString(srcData);
            });
            System.out.println(data);
            browser.loadHtml(data);
            goTop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goTop() {
    }

    public void prev() {
        if (current <= 0) {
            return;
        }
        current--;
        openContent(current);
    }

    public void next() {
        if (current >= book.getContents().size() - 1) {
            return;
        }
        current++;
        openContent(current);
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