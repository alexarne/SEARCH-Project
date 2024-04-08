package BookSearchUi;

import javax.swing.*;

public class BookDisplayLabel extends JLabel {
    public BookObject origin;
    public BookDisplayLabel(BookObject book){
        super();
        origin = book;
    }

    public BookDisplayLabel(BookObject book, String text){
        super(text);
        origin = book;
    }
}
