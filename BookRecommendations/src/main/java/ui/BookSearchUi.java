package ui;

import components.Book;
import components.UserProfile;
import io.github.cdimascio.dotenv.Dotenv;
import searcher.BookSearcher;
import similarity.CosineSimilarity;
import similarity.RatingMatrix;
import similarity.Similarity;
import components.QueryType;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class BookSearchUi extends JFrame {
    public JPanel resultWindow = new JPanel();
    public JTextField queryWindow = new JTextField("", 28);
    public JTextArea docTextView = new JTextArea("", 15, 28);
    private JScrollPane docViewPane = new JScrollPane(docTextView);
    private JScrollPane resultPane = new JScrollPane(resultWindow);
    JMenuBar menuBar = new JMenuBar();
    JMenu fileMenu = new JMenu("File");
    JMenu optionsMenu = new JMenu("Search options");
    JMenuItem quitItem = new JMenuItem("Quit");
    JMenuItem resetItem = new JMenuItem("Reset user");
    JRadioButtonMenuItem userItem = new JRadioButtonMenuItem("User query");
    JRadioButtonMenuItem neutralItem = new JRadioButtonMenuItem("Neutral query");
    private Font font = new Font("Arial", Font.BOLD, 16);

    String emptyStarIconFile = "./images/starempty.png";
    String fullStarIconFile = "./images/starfull.png";
    String logoFile = "./images/logo.png";

    BufferedImage emptyStar;
    BufferedImage fullStar;
    BookSearcher searcher;

    List<Book> currentResultList;

    QueryType queryType;

    UserProfile user;

    private RatingMatrix ratingMatrix;
    private Similarity similarity;

    final int FRAME_WIDTH = 600;
    final int FRAME_HEIGHT = 650;

    final int PANE_HEIGHT = 450;

    final int NUMBER_WIDTH = 30;
    final int TITLE_WIDTH = 200;
    final int AUTHOR_WIDTH = 150;
    final int RATING_WIDTH = 70;
    final int STARS_WIDTH = 100;
    final int DISPLAY_HEIGHT = 20;
    final int PADDING = 30;

    public BookSearchUi() {
        init();
    }

    void init() {
        Dotenv dotenv = Dotenv.configure().load();
        searcher = new BookSearcher("localhost", 9200, dotenv.get("ES_FINGERPRINT"), dotenv.get("ES_PASSWORD"), dotenv.get("ES_INDEX"));
        try {
            emptyStar = ImageIO.read(new File(emptyStarIconFile));
            fullStar = ImageIO.read(new File(fullStarIconFile));
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setResizable(false);
        ImageIcon logoIcon = new ImageIcon(logoFile);
        setIconImage(logoIcon.getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        resultWindow.setLayout(new BoxLayout(resultWindow, BoxLayout.Y_AXIS));
        p.add(menuBar);

        resultPane.setPreferredSize(new Dimension(FRAME_WIDTH, PANE_HEIGHT));

        menuBar.add(fileMenu);
        menuBar.add(optionsMenu);
        fileMenu.add(quitItem);
        fileMenu.add(resetItem);
        optionsMenu.add(userItem);
        optionsMenu.add(neutralItem);

        userItem.setSelected(true);
        queryType = QueryType.USER_QUERY;
        Action chooseUserItem = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                neutralItem.setSelected(false);
                queryType = QueryType.USER_QUERY;
            }
        };
        userItem.addActionListener(chooseUserItem);
        Action chooseNeutralItem = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                userItem.setSelected(false);
                queryType = QueryType.NEUTRAL_QUERY;
            }
        };
        neutralItem.addActionListener(chooseNeutralItem);

        JPanel p1 = new JPanel();
        p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
        p.add(p1);

        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        p3.add(queryWindow);
        queryWindow.setFont(font);
        p.add(p3);
        p.add(resultPane);

        docTextView.setFont(font);
        docTextView.setText("\n\n\n  Abstracts show here.");
        docTextView.setLineWrap(true);
        docTextView.setWrapStyleWord(true);
        p.add(docViewPane);

        this.add(p);
        setVisible(true);

        Action search = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    long startTime = System.currentTimeMillis();
                    //currentResultList = searcher.searchBooks(queryWindow.getText().toLowerCase().trim(), queryType, user);
                    currentResultList = searcher.searchBooks(queryWindow.getText().toLowerCase().trim(), queryType, user, ratingMatrix, similarity);
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    displayResults(elapsedTime / 1000.0);
                } catch (
                        IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        queryWindow.registerKeyboardAction(search,
                "",
                KeyStroke.getKeyStroke("ENTER"),
                JComponent.WHEN_FOCUSED);

        Action reset = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                user.resetRatings();
                resetItem.setSelected(false);
            }
        };

        resetItem.addActionListener(reset);
        Action quit = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        quitItem.addActionListener(quit);

        user = new UserProfile();

        initRatingMatrix();
        initSimilarity();
    }

    /**
     * Fill rating matrix between all users on goodreads.
     */
    private void initRatingMatrix() {
        ratingMatrix = new RatingMatrix();
        // Fill matrix with data from index.
        // e.g:
        // ratingMatrix.put(user_id, book_id, rating);
    }

    /**
     * Setup similarity.
     */
    private void initSimilarity() {
        Similarity cosineSimilarity = new CosineSimilarity(ratingMatrix);
        //Similarity similarityMatrix = new SimilarityMatrix(cosineSimilarity, user.getId(), ratingMatrix.getUserIds());

        similarity = cosineSimilarity;
        //similarity = similarityMatrix;
    }

    // To use for errors, like when we get no results.
    void displayInfoText(String info) {
        resultWindow.removeAll();
        JLabel label = new JLabel(info);
        label.setFont(font);
        resultWindow.add(label);
        revalidate();
        repaint();
    }

    void displayResults(double elapsedTime) {
        resultWindow.removeAll();
        displayInfoText(String.format(" Found %d book(s) in %.3f seconds", currentResultList.size(), elapsedTime));
        int i;
        for (i = 0; i < currentResultList.size(); i++) {
            final Book currBook = currentResultList.get(i);
            JPanel bookToShow = new JPanel();
            bookToShow.setAlignmentX(Component.LEFT_ALIGNMENT);
            bookToShow.setLayout(new BoxLayout(bookToShow, BoxLayout.X_AXIS));
            bookToShow.setPreferredSize(new Dimension(FRAME_WIDTH-PADDING, DISPLAY_HEIGHT));

            DecimalFormat f = new DecimalFormat("##.00");

            BookDisplayLabel numberLabel = new BookDisplayLabel(currBook, " " +(i+1)+". ");
            BookDisplayLabel titleLabel = new BookDisplayLabel(currBook, currBook.getTitle());
            BookDisplayLabel authorLabel = new BookDisplayLabel(currBook, currBook.getAuthor());
            BookDisplayLabel ratingLabel = new BookDisplayLabel(currBook, f.format(currBook.getRating()) + "/5");

            final JCheckBox[] starBoxes = new JCheckBox[5];
            for (int j = 0; j < 5; j++) {
                starBoxes[j] = new JCheckBox();
                starBoxes[j].setIcon(new ImageIcon(emptyStar));
                starBoxes[j].setPreferredSize(new Dimension(STARS_WIDTH/5, DISPLAY_HEIGHT));
                starBoxes[j].setIcon(user.getRating(currBook) >= (j+1) ? new ImageIcon(fullStar) : new ImageIcon(emptyStar));
                starBoxes[j].setSelected((j+1) == user.getRating(currBook));
                final int rating = j + 1;
                ItemListener setRating = new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            user.setRating(currBook, rating);

                            /* Update rating matrix for user. */
                            ratingMatrix.put(user.getId(), currBook.getId(), rating);
                            System.out.println(ratingMatrix.getRating(user.getId(), currBook.getId()));
                            
                            for (int k = 0; k < 5; k++) {
                                starBoxes[k].setIcon(rating >= (k+1) ? new ImageIcon(fullStar) : new ImageIcon(emptyStar));
                                if ((k+1) != rating) starBoxes[k].setSelected(false);
                            }
                        }
                        else if (rating == user.getRating(currBook)) {
                            user.removeRating(currBook);
                            for (int k = 0; k < 5; k++) {
                                starBoxes[k].setIcon(new ImageIcon(emptyStar));
                            }
                        }
                    }
                };
                starBoxes[j].addItemListener(setRating);
            }

            numberLabel.setPreferredSize(new Dimension(NUMBER_WIDTH, PANE_HEIGHT));
            titleLabel.setPreferredSize(new Dimension(TITLE_WIDTH, PANE_HEIGHT));
            authorLabel.setPreferredSize(new Dimension(AUTHOR_WIDTH, PANE_HEIGHT));
            ratingLabel.setPreferredSize(new Dimension(RATING_WIDTH, PANE_HEIGHT));

            numberLabel.setFont(font);
            titleLabel.setFont(font);
            authorLabel.setFont(font);
            ratingLabel.setFont(font);

            MouseAdapter showDocument = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    String title = ((BookDisplayLabel) e.getSource()).origin.getTitle();
                    String author = ((BookDisplayLabel) e.getSource()).origin.getAuthor();
                    String abstr = ((BookDisplayLabel) e.getSource()).origin.getAbstr();
                    docTextView.setText("Displaying abstract of " + title + " by " + author + ": \n\n" + abstr);
                    docTextView.setCaretPosition(0);
                }
            };
            numberLabel.addMouseListener(showDocument);
            titleLabel.addMouseListener(showDocument);
            authorLabel.addMouseListener(showDocument);
            ratingLabel.addMouseListener(showDocument);
            bookToShow.add(numberLabel);
            bookToShow.add(titleLabel);
            bookToShow.add(Box.createHorizontalGlue());
            bookToShow.add(authorLabel);
            bookToShow.add(Box.createHorizontalGlue());
            bookToShow.add(ratingLabel);
            bookToShow.add(Box.createHorizontalGlue());
            for (JCheckBox starBox : starBoxes) {
                bookToShow.add(starBox);
            }
            resultWindow.add(bookToShow);
        }

        revalidate();
        repaint();
    }

    public static void main( String[] args ) throws IOException {
        BookSearchUi bookSearchUi = new BookSearchUi();
    }
}