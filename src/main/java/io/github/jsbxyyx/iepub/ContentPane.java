package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.browsersupport.NavigationEvent;
import nl.siegmann.epublib.browsersupport.NavigationEventListener;
import nl.siegmann.epublib.browsersupport.Navigator;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ContentPane extends JPanel implements NavigationEventListener, HyperlinkListener {

    private static final Logger log = LoggerFactory.getLogger(ContentPane.class);
    private Navigator navigator;
    private Resource currentResource;
    private JEditorPane editorPane;
    private JScrollPane scrollPane;
    private HTMLDocumentFactory htmlDocumentFactory;
    private List<MyEventCallback> myEventCallbackList = new ArrayList<>();

    public ContentPane(Navigator navigator) {
        super(new GridLayout(1, 0));
        this.scrollPane = (JScrollPane) add(new JScrollPane());
        this.scrollPane.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
//                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
//                    Point viewPosition = scrollPane.getViewport().getViewPosition();
//                    int newY = (int) (viewPosition.getY() + 100);
//                    scrollPane.getViewport().setViewPosition(new Point((int) viewPosition.getX(), newY));
//                }
            }
        });
        this.scrollPane.addMouseWheelListener(new MouseWheelListener() {

            private boolean gotoNextPage = false;
            private boolean gotoPreviousPage = false;

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
//                int notches = e.getWheelRotation();
//                int increment = scrollPane.getVerticalScrollBar().getUnitIncrement(1);
//                if (notches < 0) {
//                    Point viewPosition = scrollPane.getViewport().getViewPosition();
//                    if (viewPosition.getY() - increment < 0) {
//                        if (gotoPreviousPage) {
//                            gotoPreviousPage = false;
//                            ContentPane.this.navigator.gotoPreviousSpineSection(-1, ContentPane.this);
//                        } else {
//                            gotoPreviousPage = true;
//                            scrollPane.getViewport().setViewPosition(new Point((int) viewPosition.getX(), 0));
//                        }
//                    }
//                } else {
//                    // only move to the next page if we are exactly at the bottom of the current page
//                    Point viewPosition = scrollPane.getViewport().getViewPosition();
//                    int viewportHeight = scrollPane.getViewport().getHeight();
//                    int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();
//                    if (viewPosition.getY() + viewportHeight + increment > scrollMax) {
//                        if (gotoNextPage) {
//                            gotoNextPage = false;
//                            ContentPane.this.navigator.gotoNextSpineSection(ContentPane.this);
//                        } else {
//                            gotoNextPage = true;
//                            int newY = scrollMax - viewportHeight;
//                            scrollPane.getViewport().setViewPosition(new Point((int) viewPosition.getX(), newY));
//                        }
//                    }
//                }
            }
        });
        this.navigator = navigator;
        navigator.addNavigationEventListener(this);
        this.editorPane = createJEditorPane();
        scrollPane.getViewport().add(editorPane);
        this.htmlDocumentFactory = new HTMLDocumentFactory(navigator, editorPane.getEditorKit());
        initBook(navigator.getBook());
    }

    private void initBook(Book book) {
        if (book == null) {
            return;
        }
        htmlDocumentFactory.init(book);
        displayPage(book.getCoverPage());
    }


    private static boolean matchesAny(String searchString, String... possibleValues) {
        for (int i = 0; i < possibleValues.length; i++) {
            String attributeValue = possibleValues[i];
            if (StringUtils.isNotBlank(attributeValue) && (attributeValue.equals(searchString))) {
                return true;
            }
        }
        return false;
    }


    private static void scrollToElement(JEditorPane editorPane, HTMLDocument.Iterator elementIterator) {
        try {
            Rectangle rectangle = editorPane.modelToView(elementIterator.getStartOffset());
            if (rectangle == null) {
                return;
            }
            // the view is visible, scroll it to the
            // center of the current visible area.
            Rectangle visibleRectangle = editorPane.getVisibleRect();
            // r.y -= (vis.height / 2);
            rectangle.height = visibleRectangle.height;
            editorPane.scrollRectToVisible(rectangle);
        } catch (BadLocationException e) {
            log.error(e.getMessage());
        }
    }


    private void scrollToNamedAnchor(String fragmentId) {
        HTMLDocument doc = (HTMLDocument) editorPane.getDocument();
        for (HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A); iter.isValid(); iter.next()) {
            AttributeSet attributes = iter.getAttributes();
            if (matchesAny(fragmentId, (String) attributes.getAttribute(HTML.Attribute.NAME),
                    (String) attributes.getAttribute(HTML.Attribute.ID))) {
                scrollToElement(editorPane, iter);
                break;
            }
        }
    }

    private JEditorPane createJEditorPane() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setBackground(Color.decode("#17181a"));
        editorPane.setEditable(false);
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        StyleSheet myStyleSheet = new StyleSheet();
        String normalTextStyle = "font-size: 11px; font-family: 'Mono'; color: #f6f7f9;";
        myStyleSheet.addRule("body {" + normalTextStyle + "}");
        myStyleSheet.addRule("p {" + normalTextStyle + "}");
        myStyleSheet.addRule("div {" + normalTextStyle + "}");
        htmlKit.setStyleSheet(myStyleSheet);
        editorPane.setEditorKit(htmlKit);
        editorPane.addHyperlinkListener(this);
        editorPane.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent keyEvent) {
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
                    navigator.gotoNextSpineSection(ContentPane.this);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_PAGE_UP) {
                    navigator.gotoPreviousSpineSection(ContentPane.this);
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_LEFT) {
                    ContentPane.this.gotoPreviousPage();
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_RIGHT) {
                    ContentPane.this.gotoNextPage();
                } else if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
                    Point viewPosition = scrollPane.getViewport().getViewPosition();
                    int newY = (int) (viewPosition.getY() + 150);
                    scrollPane.getViewport().setViewPosition(new Point((int) viewPosition.getX(), newY));
                }
            }
        });
        return editorPane;
    }

    public void displayPage(Resource resource) {
        displayPage(resource, 0);
    }

    public void displayPage(Resource resource, int sectionPos) {
        if (resource == null) {
            return;
        }
        try {
            HTMLDocument document = htmlDocumentFactory.getDocument(resource);
            if (document == null) {
                return;
            }
            currentResource = resource;
            editorPane.setDocument(document);
            log.info("\ndocument :: " + editorPane.getText());
            scrollToCurrentPosition(sectionPos);
        } catch (Exception e) {
            log.error("When reading resource " + resource.getId() + "(" + resource.getHref() + ") :" + e.getMessage(), e);
        }
    }

    private void scrollToCurrentPosition(int sectionPos) {
        if (sectionPos < 0) {
            editorPane.setCaretPosition(editorPane.getDocument().getLength());
        } else {
            editorPane.setCaretPosition(sectionPos);
        }
        if (sectionPos == 0) {
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
        } else if (sectionPos < 0) {
            int viewportHeight = scrollPane.getViewport().getHeight();
            int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();
            scrollPane.getViewport().setViewPosition(new Point(0, scrollMax - viewportHeight));
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        final URL url = event.getURL();
        if (url.getProtocol().toLowerCase().startsWith("http") && !"".equals(url.getHost())) {
            try {
                DesktopUtil.launchBrowser(event.getURL());
                return;
            } catch (DesktopUtil.BrowserLaunchException ex) {
                log.warn("Couldn't launch system web browser.", ex);
            }
        }
        String resourceHref = calculateTargetHref(event.getURL());
        if (resourceHref.startsWith("#")) {
            scrollToNamedAnchor(resourceHref.substring(1));
            return;
        }

        Resource resource = navigator.getBook().getResources().getByHref(resourceHref);
        if (resource == null) {
            log.error("Resource with url " + resourceHref + " not found");
        } else {
            navigator.gotoResource(resource, this);
        }
    }

    public void gotoXPage(int spinePos, int x, int y) {
        navigator.gotoSpineSection(spinePos, this);
        scrollPane.getViewport().setViewPosition(new Point(x, y));
    }

    public void gotoFirstPage() {
        gotoXPage(0, 0, 0);
    }

    public void gotoLastPage() {
        gotoXPage(navigator.getBook().getSpine().size() - 1, 0, 0);
    }

    public void gotoPreviousPage() {
        Point viewPosition = scrollPane.getViewport().getViewPosition();
        if (viewPosition.getY() <= 0) {
            navigator.gotoPreviousSpineSection(this);
            return;
        }
        int viewportHeight = scrollPane.getViewport().getHeight();
        int newY = (int) viewPosition.getY();
        newY -= viewportHeight;
        newY = Math.max(0, newY - viewportHeight);
        scrollPane.getViewport().setViewPosition(new Point((int) viewPosition.getX(), newY));
    }

    public void gotoNextPage() {
        Point viewPosition = scrollPane.getViewport().getViewPosition();
        int viewportHeight = scrollPane.getViewport().getHeight();
        int scrollMax = scrollPane.getVerticalScrollBar().getMaximum();
        if (viewPosition.getY() + viewportHeight >= scrollMax) {
            navigator.gotoNextSpineSection(this);
            return;
        }
        int newY = ((int) viewPosition.getY()) + viewportHeight;
        scrollPane.getViewport().setViewPosition(new Point((int) viewPosition.getX(), newY));
    }


    private String calculateTargetHref(URL clickUrl) {
        String resourceHref = clickUrl.toString();
        try {
            resourceHref = URLDecoder.decode(resourceHref, Constants.CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        resourceHref = resourceHref.substring(ImageLoaderCache.IMAGE_URL_PREFIX.length());

        if (resourceHref.startsWith("#")) {
            return resourceHref;
        }
        if (currentResource != null && StringUtils.isNotBlank(currentResource.getHref())) {
            int lastSlashPos = currentResource.getHref().lastIndexOf('/');
            if (lastSlashPos >= 0) {
                resourceHref = currentResource.getHref().substring(0, lastSlashPos + 1) + resourceHref;
            }
        }
        return resourceHref;
    }


    public void navigationPerformed(NavigationEvent navigationEvent) {
        if (navigationEvent.isBookChanged()) {
            initBook(navigationEvent.getCurrentBook());
        } else {
            if (navigationEvent.isResourceChanged()) {
                displayPage(navigationEvent.getCurrentResource(), navigationEvent.getCurrentSectionPos());
            } else if (navigationEvent.isSectionPosChanged()) {
                editorPane.setCaretPosition(navigationEvent.getCurrentSectionPos());
            }
            if (StringUtils.isNotBlank(navigationEvent.getCurrentFragmentId())) {
                scrollToNamedAnchor(navigationEvent.getCurrentFragmentId());
            }
        }
        Point viewPosition = scrollPane.getViewport().getViewPosition();
        for (MyEventCallback myEventCallback : myEventCallbackList) {
            myEventCallback.call(navigator, (int) viewPosition.getX(), (int) viewPosition.getY());
        }
    }

    public Point getViewPosition() {
        return scrollPane.getViewport().getViewPosition();
    }

    public void registerEventCallback(MyEventCallback eventCallback) {
        if (eventCallback != null) {
            myEventCallbackList.add(eventCallback);
        }
    }

}