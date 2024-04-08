package BookSearchUi;

public class BookObject {
    private String title = "";
    private String author = "";
    private String abstr = "";
    private int year = -1;
    private double publicScore = 1;
    private int myScore = -1;

    public BookObject(String title, String author, String abstr, int year, double publicScore, int myScore){
        this.title = title;
        this.author = author;
        this.abstr = abstr;
        this.year = year;
        this.publicScore = publicScore;
        this.myScore = myScore;
    }

    public BookObject(String title, String author, String abstr, int year, double publicScore){
        this.title = title;
        this.author = author;
        this.abstr = abstr;
        this.year = year;
        this.publicScore = publicScore;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getAbstr() {
        return abstr;
    }

    public int getYear() {
        return year;
    }

    public double getPublicScore() {
        return publicScore;
    }

    public int getMyScore() {
        return myScore;
    }

    public void setMyScore(int myScore) {
        this.myScore = myScore;
    }
}
