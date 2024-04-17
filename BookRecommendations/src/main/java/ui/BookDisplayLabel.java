package ui;

import javax.swing.*;

public class BookDisplayLabel extends JLabel {
    public book.Book origin;
    public BookDisplayLabel(book.Book book){
        super();
        origin = book;
    }

    public BookDisplayLabel(book.Book book, String text){
        super(text);
        origin = book;
    }
}
