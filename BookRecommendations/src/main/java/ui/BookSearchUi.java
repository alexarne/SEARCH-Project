package ui;

import book.Book;
import io.github.cdimascio.dotenv.Dotenv;
import searcher.BookSearcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class BookSearchUi extends JFrame {
    public JPanel resultWindow = new JPanel();
    public JTextField queryWindow = new JTextField( "", 28 );
    public JTextArea docTextView = new JTextArea( "", 15, 28 );
    private JScrollPane docViewPane = new JScrollPane( docTextView );
    private JScrollPane resultPane = new JScrollPane( resultWindow );
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu( "File" );
    JMenu optionsMenu = new JMenu( "Search options" );
    JMenu normalizationMenu = new JMenu( "Normalization" );
    ButtonGroup queries = new ButtonGroup();
    JMenuItem quitItem = new JMenuItem( "Quit" );
    JRadioButtonMenuItem intersectionItem = new JRadioButtonMenuItem( "Intersection query" );
    JRadioButtonMenuItem phraseItem = new JRadioButtonMenuItem( "Phrase query" );
    JRadioButtonMenuItem rankedItem = new JRadioButtonMenuItem( "Ranked retrieval" );
    private Font font = new Font( "Arial", Font.BOLD, 16 );

    String emptyStarIconFile = "./images/starempty.png";
    String fullStarIconFile = "./images/starfull.png";
    String logoFile = "./images/logo.png";

    BufferedImage emptyStar;
    BufferedImage fullStar;
    BookSearcher searcher;

    List<Book> currentResultList;

    public BookSearchUi() {
        init();
    }

    void init() {
        Dotenv dotenv = Dotenv.configure().load();
        searcher = new BookSearcher("localhost", 9200, dotenv.get("ES_FINGERPRINT"), dotenv.get("ES_PASSWORD"), dotenv.get("ES_INDEX"));
        try {
            emptyStar = ImageIO.read(new File(emptyStarIconFile));
            fullStar = ImageIO.read(new File(fullStarIconFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setSize( 600, 650 );
        ImageIcon logoIcon = new ImageIcon(logoFile);
        setIconImage(logoIcon.getImage());
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        resultWindow.setLayout(new BoxLayout(resultWindow, BoxLayout.Y_AXIS));
        p.add(menuBar);

        resultPane.setPreferredSize( new Dimension(400, 450 ));

        menuBar.add( fileMenu );
        menuBar.add( optionsMenu );
        fileMenu.add( quitItem );
        optionsMenu.add( intersectionItem );
        optionsMenu.add( phraseItem );
        optionsMenu.add( rankedItem );

        intersectionItem.setSelected( true );

        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
        p.add( p1 );

        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        p3.add( queryWindow );
        queryWindow.setFont( font );
        p.add( p3 );
        p.add( resultPane );

        docTextView.setFont(font);
        docTextView.setText("\n\n\n  Abstracts show here.");
        docTextView.setLineWrap(true);
        docTextView.setWrapStyleWord(true);
        p.add(docViewPane);

        this.add(p);
        setVisible( true );

        Action test = new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    long startTime = System.currentTimeMillis();
                    currentResultList = searcher.searchBooks(queryWindow.getText().toLowerCase().trim());
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    displayResults(elapsedTime/1000.0);
                } catch (
                        IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        queryWindow.registerKeyboardAction( test,
                "",
                KeyStroke.getKeyStroke( "ENTER" ),
                JComponent.WHEN_FOCUSED );

        Action quit = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                System.exit( 0 );
            }
        };
        quitItem.addActionListener( quit );
    }

    // To use for errors, like when we get no results.
    void displayInfoText( String info ) {
        resultWindow.removeAll();
        JLabel label = new JLabel( info );
        label.setFont( font );
        resultWindow.add( label );
        revalidate();
        repaint();
    }

    void displayResults(double elapsedTime) {
        resultWindow.removeAll();
        int maxResultsToDisplay = 10;
        displayInfoText( String.format( "Found %d book(s) in %.3f seconds", currentResultList.size(), elapsedTime ));
        int i;
        for (i=0; i<currentResultList.size() && i<maxResultsToDisplay; i++ ) {
            JPanel bookToShow = new JPanel();
            bookToShow.setAlignmentX(Component.LEFT_ALIGNMENT);
            //bookToShow.setLayout(new BoxLayout(bookToShow, BoxLayout.X_AXIS));
            bookToShow.setLayout(new GridBagLayout());
            //bookToShow.setPreferredSize(new Dimension(430, 20));

            DecimalFormat f = new DecimalFormat("##.00");

            BookDisplayLabel titleLabel = new BookDisplayLabel(currentResultList.get(i), currentResultList.get(i).getTitle());
            BookDisplayLabel authorLabel = new BookDisplayLabel(currentResultList.get(i), currentResultList.get(i).getAuthor());
            BookDisplayLabel ratingLabel = new BookDisplayLabel(currentResultList.get(i), f.format(currentResultList.get(i).getRating()) + "/5");
            JCheckBox readBox = new JCheckBox();
            readBox.setSelected(currentResultList.get(i).getMyScore() != -1);

            JCheckBox[] starBoxes = new JCheckBox[5];

            for (int j = 0; j < starBoxes.length; j++) {
                starBoxes[j] = new JCheckBox();
                starBoxes[j].setSelectedIcon(new ImageIcon(fullStar));
                starBoxes[j].setIcon(new ImageIcon(emptyStar));
                starBoxes[j].setPreferredSize(new Dimension(20,20));
                starBoxes[j].setSelected(currentResultList.get(i).getMyScore() >= j);
            }

            titleLabel.setPreferredSize(new Dimension(200, 20));
            authorLabel.setPreferredSize(new Dimension(150, 20));
            ratingLabel.setPreferredSize(new Dimension(50, 20));

            titleLabel.setFont( font );
            authorLabel.setFont( font );
            ratingLabel.setFont( font );

            MouseAdapter showDocument = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    String abstr = ((BookDisplayLabel)e.getSource()).origin.getAbstr();
                    docTextView.setText(abstr);
                    docTextView.setCaretPosition(0);
                }
            };
            titleLabel.addMouseListener(showDocument);
            authorLabel.addMouseListener(showDocument);
            ratingLabel.addMouseListener(showDocument);
            bookToShow.add(titleLabel);
            bookToShow.add(authorLabel);
            bookToShow.add(ratingLabel);
            bookToShow.add(readBox);

            for (JCheckBox starBox : starBoxes) {
                bookToShow.add(starBox);
            }
            resultWindow.add(bookToShow);
        }

        revalidate();
        repaint();
    };

    public static void main( String[] args ) throws IOException {
        BookSearchUi bookSearchUi = new BookSearchUi();
    }
}