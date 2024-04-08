package BookSearchUi;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.print.Book;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;


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



    BookObject[] currentResultList = null;

    String emptyStarIconFile = "BooksearchUi/starempty.png";
    String fullStarIconFile = "BooksearchUi/starfull.png";
    String logoFile = "BooksearchUi/logo.png";

    BufferedImage emptyStar;
    BufferedImage fullStar;

    public BookSearchUi(){
        init();
    }

    void init(){
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

        Action test = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //docTextView.setText("Test performed: " + queryWindow.getText());
                displayResults();
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




    void displayResults() {
        resultWindow.removeAll();
        int maxResultsToDisplay = 10;
        int i;
        for ( i=0; i<currentResultList.length && i<maxResultsToDisplay; i++ ) {

            JPanel bookToShow = new JPanel();
            bookToShow.setAlignmentX(Component.LEFT_ALIGNMENT);
            //bookToShow.setLayout(new BoxLayout(bookToShow, BoxLayout.X_AXIS));
            bookToShow.setLayout(new GridBagLayout());
            //bookToShow.setPreferredSize(new Dimension(430, 20));

            DecimalFormat f = new DecimalFormat("##.00");

            BookDisplayLabel titleLabel = new BookDisplayLabel(currentResultList[i], currentResultList[i].getTitle());
            BookDisplayLabel authorLabel = new BookDisplayLabel(currentResultList[i], currentResultList[i].getAuthor());
            BookDisplayLabel yearLabel = new BookDisplayLabel(currentResultList[i], Integer.toString(currentResultList[i].getYear()));
            BookDisplayLabel publicScoreLabel = new BookDisplayLabel(currentResultList[i], f.format(currentResultList[i].getPublicScore()) + "/5");
            JCheckBox readBox = new JCheckBox();
            if(currentResultList[i].getMyScore() == -1){
                readBox.setSelected(false);
            }
            else{
                readBox.setSelected(true);
            }

            JCheckBox[] starBoxes = new JCheckBox[5];

            for (int j = 0; j < starBoxes.length; j++) {
                starBoxes[j] = new JCheckBox();
                starBoxes[j].setSelectedIcon(new ImageIcon(fullStar));
                starBoxes[j].setIcon(new ImageIcon(emptyStar));
                starBoxes[j].setPreferredSize(new Dimension(20,20));
                if (currentResultList[i].getMyScore() >=j){
                    starBoxes[j].setSelected(true);
                }
                else{
                    starBoxes[j].setSelected(false);
                }
            }


            titleLabel.setPreferredSize(new Dimension(200, 20));
            authorLabel.setPreferredSize(new Dimension(150, 20));
            yearLabel.setPreferredSize(new Dimension(50, 20));
            publicScoreLabel.setPreferredSize(new Dimension(50, 20));

            titleLabel.setFont( font );
            authorLabel.setFont( font );
            yearLabel.setFont( font );
            publicScoreLabel.setFont( font );

            MouseAdapter showDocument = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    String abstr = ((BookDisplayLabel)e.getSource()).origin.getAbstr();

                    docTextView.setText(abstr);
                    docTextView.setCaretPosition(0);
                }
            };
            titleLabel.addMouseListener(showDocument);
            authorLabel.addMouseListener(showDocument);
            yearLabel.addMouseListener(showDocument);
            publicScoreLabel.addMouseListener(showDocument);
            bookToShow.add(titleLabel);
            bookToShow.add(authorLabel);
            bookToShow.add(yearLabel);
            bookToShow.add(publicScoreLabel);
            bookToShow.add(readBox);

            for (int j = 0; j < starBoxes.length; j++) {
                bookToShow.add(starBoxes[j]);
            }

            resultWindow.add(bookToShow);
        }

        revalidate();
        repaint();
    };

    public static void main( String[] args ) {
        BookSearchUi bookSearchUi = new BookSearchUi();
        bookSearchUi.currentResultList = new BookObject[2];
        bookSearchUi.currentResultList[0] = new BookObject("Fellowship of the Ring", "J.R.R. Tolkien", "In ancient times the Rings of Power were crafted by the Elven-smiths, and Sauron, the Dark Lord, forged the One Ring, filling it with his own power so that he could rule all others. But the One Ring was taken from him, and though he sought it throughout Middle-earth, it remained lost to him. After many ages it fell into the hands of Bilbo Baggins, as told in The Hobbit.\n" +
                "\n" +
                "In a sleepy village in the Shire, young Frodo Baggins finds himself faced with an immense task, as his elderly cousin Bilbo entrusts the Ring to his care. Frodo must leave his home and make a perilous journey across Middle-earth to the Cracks of Doom, there to destroy the Ring and foil the Dark Lord in his evil purpose.", 1954, 4.8D);
        bookSearchUi.currentResultList[1] = new BookObject("The Hobbit", "J.R.R. Tolkien", "In a hole in the ground there lived a hobbit. Not a nasty, dirty, wet hole, filled with the ends of worms and an oozy smell, nor yet a dry, bare, sandy hole with nothing in it to sit down on or to eat: it was a hobbit-hole, and that means comfort.\n" +
                "Written for J.R.R. Tolkien’s own children, The Hobbit met with instant critical acclaim when it was first published in 1937. Now recognized as a timeless classic, this introduction to the hobbit Bilbo Baggins, the wizard Gandalf, Gollum, and the spectacular world of Middle-earth recounts of the adventures of a reluctant hero, a powerful and dangerous ring, and the cruel dragon Smaug the Magnificent. The text in this 372-page paperback edition is based on that first published in Great Britain by Collins Modern Classics (1998), and includes a note on the text by Douglas A. Anderson (2001).", 1937, 4.456453453);

    }
}