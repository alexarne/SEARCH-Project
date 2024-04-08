from bs4 import BeautifulSoup
import asyncio
import aiohttp
import sys

GOODREADS_URL = "https://www.goodreads.com"
GOODREADS_LIST_URL = "https://www.goodreads.com/list/show/1.Best_Books_Ever?page="
NUM_LIST_PAGES = 10
ELASTIC_INSERT_URL = "https://localhost:9200/"

async def main():
    async with aiohttp.ClientSession() as session:
        updateProgress()
        tasks = [asyncio.ensure_future(indexBookList(pageNumber, session)) for pageNumber in range(1, NUM_LIST_PAGES+1)]
        await asyncio.gather(*tasks)
        print()
        global numErrors
        print(f"Non-existing books: {numErrors}")
        # await indexBook("https://www.goodreads.com/book/show/135836.Trainspotting", session)
    # indexBook("https://www.goodreads.com/book/show/2767052-the-hunger-games")
    # indexBook("https://www.goodreads.com/book/show/1885.Pride_and_Prejudice")
    # indexBook("https://www.goodreads.com/book/show/12067.Good_Omens?from_search=true&from_srp=true&qid=AYPzlLhVGU&rank=1")

async def indexBookList(pageNumber, session):
    URLs = await getBookURLs(pageNumber, session)
    addProgressGoal(len(URLs))
    tasks = [asyncio.ensure_future(indexBook(URL, session)) for URL in URLs]
    await asyncio.gather(*tasks)

async def getBookURLs(pageNumber, session):
    # print(f"Book list (page {pageNumber}): Fetching list...")
    page = await fetch(session, GOODREADS_LIST_URL + str(pageNumber))
    soup = BeautifulSoup(page, "html.parser")
    entries = soup.find("div", id="all_votes").find_all("tr")
    URLs = [GOODREADS_URL + entry.find("a", class_="bookTitle")["href"] for entry in entries]
    # print(f"Book list (page {pageNumber}): List fetched")
    return URLs

async def indexBook(URL, session):
    # print(f"Indexing book {URL}")
    page = await fetch(session, URL)
    soup = BeautifulSoup(page, "html.parser")
    # print(soup.prettify())
    result = {}

    # Isolate metadata
    mainContent = soup.find("div", class_="BookPage__mainContent")
    if mainContent is None:
        print(f"\rERROR: Book not found: {URL}")
        updateErrors()
        return

    # Get title
    result["title"] = mainContent.find("h1", class_="Text Text__title1").text

    # Get abstract
    abstract = mainContent.find("div", class_="DetailsLayoutRightParagraph__widthConstrained")
    if abstract is None:
        print(f"crash point abstract on {URL}")
    for br in abstract.find_all("br"):
        br.replace_with("\n")
    result["abstract"] = abstract.text

    # Get author (assuming just one, reason: https://www.goodreads.com/book/show/7190.The_Three_Musketeers)
    authorSection = mainContent.find("div", class_="BookPageMetadataSection__contributor")
    author = authorSection.find("span", class_="ContributorLink__name")
    result["author"] = author.text

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
    # print(URL)
    addBookToIndex(result)

def addBookToIndex(data):
    # print("Indexed book " + data["title"] + " by " + data["author"])
    updateProgress()

async def fetch(session, url):
    status = 404
    attempts = 0
    while status != 200 and attempts < 5:
        try:
            async with session.get(url) as response:
                text = await response.text()
                status = response.status
                if status == 200:
                    return text
                attempts += 1
                print(f"\rRETRY FETCH: {status} on {url}")
        except:
            print(f"\rRETRY FETCH: Fetch threw exception on {url}")
    print(f"\rFATAL: Fetch failed after multiple attempts on {url}")

numIndexedBooks = -1
numErrors = 0
totalBooks = 0
def updateProgress():
    # return
    global numIndexedBooks
    numIndexedBooks += 1
    sys.stdout.write(f"\rIndexed books: {numIndexedBooks}/{totalBooks}")
    sys.stdout.flush()
def addProgressGoal(numBooks):
    global totalBooks
    totalBooks += numBooks
    sys.stdout.write(f"\rIndexed books: {numIndexedBooks}/{totalBooks}")
    sys.stdout.flush()
def updateErrors():
    global numErrors
    numErrors += 1

loop = asyncio.get_event_loop()
loop.run_until_complete(main())