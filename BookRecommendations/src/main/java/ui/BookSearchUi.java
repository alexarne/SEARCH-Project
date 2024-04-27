/**
 * Main class for the search engine + GUI
 */

package ui;

import components.Book;
import components.DisplayType;
import components.UserProfile;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import java.io.FileReader;
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

    JMenu userMenu = new JMenu("Users");
    JMenu optionsMenu = new JMenu("Search options");

    JMenu displayReadMenu = new JMenu("Display my books?");
    JMenuItem quitItem = new JMenuItem("Quit");
    JMenuItem resetItem = new JMenuItem("Reset user");
    JRadioButtonMenuItem testProfile1Item = new JRadioButtonMenuItem("Test profile 1");
    JRadioButtonMenuItem testProfile2Item = new JRadioButtonMenuItem("Test profile 2");

    JRadioButtonMenuItem testProfile3Item = new JRadioButtonMenuItem("Test profile 3");

    JRadioButtonMenuItem testProfile4Item = new JRadioButtonMenuItem("Test profile 4");

    JRadioButtonMenuItem userItem = new JRadioButtonMenuItem("User query");
    JRadioButtonMenuItem neutralItem = new JRadioButtonMenuItem("Neutral query");

    JRadioButtonMenuItem showMyBooksItem = new JRadioButtonMenuItem("Yes");
    JRadioButtonMenuItem hideMyBooksItem = new JRadioButtonMenuItem("No");
    private Font font = new Font("Arial", Font.BOLD, 16);

    private String emptyStarIconFile = "./images/starempty.png";
    private String fullStarIconFile = "./images/starfull.png";
    private String logoFile = "./images/logo.png";

    private BufferedImage emptyStar;
    private BufferedImage fullStar;

    private final int FRAME_WIDTH = 600;
    private final int FRAME_HEIGHT = 650;

    private final int PANE_HEIGHT = 450;

    private final int NUMBER_WIDTH = 30;
    private final int TITLE_WIDTH = 200;
    private final int AUTHOR_WIDTH = 150;
    private final int RATING_WIDTH = 70;
    private final int STARS_WIDTH = 100;
    private final int DISPLAY_HEIGHT = 20;
    private final int PADDING = 30;

    private BookSearcher searcher;
    private List<Book> currentResultList;

    private QueryType queryType;
    private DisplayType displayType;

    private UserProfile user;

    private RatingMatrix ratingMatrix;
    private Similarity similarity;

    private String RATINGS_FILE = "./ratings.json";

    private final int MAX_DISPLAY_RESULTS = 99;

    private final int TEST_PROFILE1_ID = 32879029;
    private final int TEST_PROFILE2_ID = 151231754;
    private final int TEST_PROFILE3_ID = 6431467;
    private final int TEST_PROFILE4_ID = 4622890;

    public BookSearchUi() {
        init();
    }

    void init() {
        Dotenv dotenv = Dotenv.configure().load();
        searcher = new BookSearcher("localhost", 9200, dotenv.get("ES_FINGERPRINT"), dotenv.get("ES_PASSWORD"), dotenv.get("ES_INDEX"));
        user = new UserProfile();

        initRatingMatrix();
        initSimilarity();

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
        menuBar.add(userMenu);
        menuBar.add(optionsMenu);
        menuBar.add(displayReadMenu);
        fileMenu.add(quitItem);
        userMenu.add(resetItem);
        userMenu.add(testProfile1Item);
        userMenu.add(testProfile2Item);
        userMenu.add(testProfile3Item);
        userMenu.add(testProfile4Item);
        optionsMenu.add(userItem);
        optionsMenu.add(neutralItem);
        displayReadMenu.add(showMyBooksItem);
        displayReadMenu.add(hideMyBooksItem);

        Action reset = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                user.resetRatings();
                testProfile1Item.setSelected(false);
                testProfile2Item.setSelected(false);
                testProfile3Item.setSelected(false);
                testProfile4Item.setSelected(false);
            }
        };
        resetItem.addActionListener(reset);
        Action quit = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
        quitItem.addActionListener(quit);

        Action testProfile1 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                testProfile2Item.setSelected(false);
                testProfile3Item.setSelected(false);
                testProfile4Item.setSelected(false);
                user = new UserProfile(TEST_PROFILE1_ID);
                user.loadRatings(ratingMatrix);
            }
        };
        testProfile1Item.addActionListener(testProfile1);
        Action testProfile2 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                testProfile1Item.setSelected(false);
                testProfile3Item.setSelected(false);
                testProfile4Item.setSelected(false);
                user = new UserProfile(TEST_PROFILE2_ID);
                user.loadRatings(ratingMatrix);
            }
        };
        testProfile2Item.addActionListener(testProfile2);
        Action testProfile3 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                testProfile1Item.setSelected(false);
                testProfile2Item.setSelected(false);
                testProfile4Item.setSelected(false);
                user = new UserProfile(TEST_PROFILE3_ID);
                user.loadRatings(ratingMatrix);
            }
        };
        testProfile3Item.addActionListener(testProfile3);
        Action testProfile4 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                testProfile1Item.setSelected(false);
                testProfile2Item.setSelected(false);
                testProfile3Item.setSelected(false);
                user = new UserProfile(TEST_PROFILE4_ID);
                user.loadRatings(ratingMatrix);
            }
        };
        testProfile4Item.addActionListener(testProfile4);

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

        hideMyBooksItem.setSelected(true);
        displayType = DisplayType.HIDE_READ_BOOKS;
        Action chooseShowReadItem = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                hideMyBooksItem.setSelected(false);
                displayType = DisplayType.SHOW_READ_BOOKS;
            }
        };
        showMyBooksItem.addActionListener(chooseShowReadItem);
        Action chooseHideReadItem = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showMyBooksItem.setSelected(false);
                displayType = DisplayType.HIDE_READ_BOOKS;
            }
        };
        hideMyBooksItem.addActionListener(chooseHideReadItem);

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
                    currentResultList = searcher.searchBooks(queryWindow.getText().toLowerCase().trim(), queryType, displayType, user, ratingMatrix, similarity);
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
    }

    /**
     * Fill rating matrix between all users on goodreads.
     */
    private void initRatingMatrix() {
        ratingMatrix = new RatingMatrix();
        JSONArray ratingEntries;
        File userFile = new File( RATINGS_FILE);
        try (FileReader reader = new FileReader(userFile)) {
            JSONParser parser = new JSONParser () ;
            ratingEntries = (JSONArray) parser.parse(reader);
            for (Object entry : ratingEntries) {
                JSONObject jsonEntry = (JSONObject) entry;
                ratingMatrix.put((int) (long) jsonEntry.get("userID"), (int) (long) jsonEntry.get("bookID"), (int) (long) jsonEntry.get("rating"));
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Display search results
     */
    void displayResults(double elapsedTime) {
        resultWindow.removeAll();
        displayInfoText(String.format(" Found %d book(s) in %.3f seconds", currentResultList.size(), elapsedTime));
        int i;
        for (i = 0; i < Math.min(MAX_DISPLAY_RESULTS, currentResultList.size()); i++) {
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