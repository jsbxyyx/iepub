package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.domain.Book;

public class BookHolder {

    private volatile static Book book_;

    private volatile static int index_;

    private static volatile boolean start_ = false;

    static void setBook(Book book) {
        book_ = book;
    }

    public static Book getBook() {
        return book_;
    }

    static void setIndex(int current) {
        index_ = current;
    }

    public static int getIndex() {
        return index_;
    }

    synchronized static void setStart(boolean start) {
        start_ = start;
    }

    public synchronized static boolean getStart() {
        return start_;
    }

}
