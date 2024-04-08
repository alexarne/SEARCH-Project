if not exist classes mkdir classes
javac -cp . -d classes BookSearchUi/BookSearchUi.java BookSearchUi/BookObject.java BookSearchUi/BookDisplayLabel.java
java -cp classes -Xmx1g BookSearchUi\BookSearchUi.java -d E:\Joel\Skola\KTH\Java\SEARCH-Project\BookSearchUi\BookSearchUi.java -l dd2477.png
