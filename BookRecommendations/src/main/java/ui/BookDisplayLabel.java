package ui;

import javax.swing.*;

public class BookDisplayLabel extends JLabel {
    public components.Book origin;
    public BookDisplayLabel(components.Book book){
        super();
        origin = book;
    }

    public BookDisplayLabel(components.Book book, String text){
        super(text);
        origin = book;
    }
}
