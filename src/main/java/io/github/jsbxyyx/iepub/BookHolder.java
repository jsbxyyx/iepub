package io.github.jsbxyyx.iepub;

import nl.siegmann.epublib.domain.Book;

public class BookHolder {

    private volatile static Book book_;

    static void setBook(Book book) {
        book_ = book;
    }

    public static Book getBook() {
        return book_;
    }

}
