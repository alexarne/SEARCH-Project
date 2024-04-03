import requests
from bs4 import BeautifulSoup

GOODREADS_URL = "https://www.goodreads.com"
GOODREADS_LIST_URL = "https://www.goodreads.com/list/show/1.Best_Books_Ever?page="
ELASTIC_INSERT_URL = "https://localhost:9200/"

def main():
    URLs = getBookURLs(1)
    for URL in URLs:
        data = getBookData(URL)
        indexBook(data)
    # getBookData("https://www.goodreads.com/book/show/2767052-the-hunger-games")
    # getBookData("https://www.goodreads.com/book/show/1885.Pride_and_Prejudice")
    # getBookData("https://www.goodreads.com/book/show/12067.Good_Omens?from_search=true&from_srp=true&qid=AYPzlLhVGU&rank=1")

def getBookURLs(pageNumber):
    page = requests.get(GOODREADS_LIST_URL + str(pageNumber))
    soup = BeautifulSoup(page.content, "html.parser")
    entries = soup.find("div", id="all_votes").find_all("tr")
    URLs = [GOODREADS_URL + entry.find("a", class_="bookTitle")["href"] for entry in entries]
    return URLs

def getBookData(URL):
    page = requests.get(URL)
    soup = BeautifulSoup(page.content, "html.parser")
    result = {}

    # Isolate metadata
    mainContent = soup.find("div", class_="BookPage__mainContent")

    # Get title
    result["title"]  = mainContent.find("h1", class_="Text Text__title1").text

    # Get abstract
    abstract = mainContent.find("div", class_="DetailsLayoutRightParagraph__widthConstrained")
    for br in abstract.find_all("br"):
        br.replace_with("\n")
    result["abstract"] = abstract.text

    # Get author
    authorSection = mainContent.find("div", class_="BookPageMetadataSection__contributor")
    authors = authorSection.find_all("span", class_="ContributorLink__name")
    result["authors"] = [author.text for author in authors]
    if len(authors) > 1: 
        print("MULTIPLE AUTHORS: " + URL)

    # Get rating
    rating = float(mainContent.find("div", class_="RatingStatistics__rating").text)
    result["rating"] = rating

    # Get number of ratings and reviews
    numRatingsAndReviewsSection = mainContent.find("div", class_="RatingStatistics__meta")
    numRatingsAndReviews = numRatingsAndReviewsSection.find_all("span")
    numRatings, numReviews = numRatingsAndReviews[0].text, numRatingsAndReviews[1].text
    numRatings = int(numRatings.replace("ratings", "").strip().replace(",", ""))
    numReviews = int(numReviews.replace("reviews", "").strip().replace(",", ""))
    result["numRatings"] = numRatings
    result["numReviews"] = numReviews

    return result

def indexBook(data):
    print("Indexed book " + data["title"])

if __name__ == "__main__":
    main()