package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.browsersupport.Navigator;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IepubView2 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(IepubView2.class);

    private EpubReader reader = new EpubReader();

    private JTextField tfFile;
    private JButton btnOpenFile;
    private JButton btnReload;

    private Navigator navigator = new Navigator();
    private TableOfContentsPane tableOfContentsPane;
    private ContentPane contentPane;

    private JButton btnFirst;
    private JButton btnLast;

    private JButton btnPrev;
    private JButton btnNext;

    private JLabel labelPageNumber;

    private static final ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(1);

    public IepubView2() {
        IepubURLProtocolHandler.install();

        setLayout(new BorderLayout());

        contentPane = new ContentPane(navigator);
        tableOfContentsPane = new TableOfContentsPane(navigator);

        JPanel n = new JPanel();
        add(n, BorderLayout.NORTH);
        tfFile = new JTextField(20);
        n.add(tfFile);

        btnOpenFile = new JButton("Open");
        btnOpenFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("epub files", "epub"));
            int result = fileChooser.showDialog(null, null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                tfFile.setText(file.getAbsolutePath());
                openBook(file);
            }
        });
        n.add(btnOpenFile);

        btnReload = new JButton("Reload");
        btnReload.addActionListener(e -> {
            gotoBook(BookHolder.getBook());
        });
        n.add(btnReload);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                tableOfContentsPane, contentPane);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

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

        btnPrev = new JButton("Prev");
        btnPrev.addActionListener(e -> {
            prev();
        });
        s.add(btnPrev);

        btnNext = new JButton("Next");
        btnNext.addActionListener(e -> {
            next();
        });
        s.add(btnNext);

        labelPageNumber = new JLabel("0/0");
        s.add(labelPageNumber);

        contentPane.registerEventCallback((nav, x, y) -> {
            labelPageNumber.setText(nav.getCurrentSpinePos() + "/" + nav.getBook().getSpine().size());
        });
        contentPane.registerEventCallback((nav, x, y) -> {
            setProgress(nav, x, y);
        });
    }

    public void openBook(File file) {
        try {
            Book book = reader.readEpub(new FileInputStream(file));
            BookHolder.setBook(book);
        } catch (IOException e) {
            log.error("open book failed.", e);
            return;
        }
        gotoBook(BookHolder.getBook());

        if (!BookHolder.getStart()) {
            scheduled.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        Point viewPosition = contentPane.getViewPosition();
                        setProgress(navigator, 0, (int) viewPosition.getY());
                    } catch (Throwable e) {
                        PropertiesUtil.log("schedule error.", e);
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);
            BookHolder.setStart(true);
        }
    }

    private void gotoBook(Book book) {
        String progress = getProgress(book);
        navigator.gotoBook(book, this);
        if (progress != null && !"".equals(progress.trim())) {
            String[] split = progress.split("\\,");
            int spinePos = Integer.parseInt(split[0].trim());
            int y = Integer.parseInt(split[1].trim());
            contentPane.gotoXPage(spinePos, 0, y);
        }
    }

    public void first() {
        contentPane.gotoFirstPage();
    }

    public void last() {
        contentPane.gotoLastPage();
    }

    public void prev() {
        contentPane.gotoPreviousPage();
    }

    public void next() {
        contentPane.gotoNextPage();
    }

    public void setProgress(Navigator navigator, int x, int y) {
        if (BookHolder.getBook() != null) {
            String identifier = getIdentifier(navigator.getBook());
            PropertiesUtil.setValue("p" + identifier, navigator.getCurrentSpinePos() + "," + y);
            log.info("progress :: " + identifier + " :: " + navigator.getCurrentSpinePos() + "," + y);
        }
    }

    public String getProgress(Book book) {
        String identifier = getIdentifier(book);
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