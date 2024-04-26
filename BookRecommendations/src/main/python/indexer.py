from bs4 import BeautifulSoup
import asyncio
import aiohttp
import sys
from elasticsearch import Elasticsearch
from os import getenv
from dotenv import load_dotenv
import time

GOODREADS_URL = "https://www.goodreads.com"
GOODREADS_BOOKLIST_URL = "https://www.goodreads.com/list/show/1.Best_Books_Ever?page="
GOODREADS_USERLIST_URL = "https://www.goodreads.com/user/best_reviewers?country=all&duration=w"
NUM_LIST_PAGES = 100
ELASTIC_INSERT_URL = "https://localhost:9200/"

load_dotenv()
# client = Elasticsearch(
#   ELASTIC_INSERT_URL,
#   ssl_assert_fingerprint = getenv("ES_FINGERPRINT"),
#   basic_auth=("elastic", getenv("ES_PASSWORD"))
# )
# client.indices.create(index=getenv("ES_INDEX"))

COOKIES = {
    'ubid-main': getenv("COOKIES_UBID_MAIN"),
    'at-main': getenv("COOKIES_AT_MAIN"),
}



### Book scraping ###

async def indexBooks():
    log("[STATUS] Starting book indexing...")
    INCREMENTAL = 5
    async with aiohttp.ClientSession() as session:
        for i in range(1, NUM_LIST_PAGES, INCREMENTAL):
            last = min(i+INCREMENTAL, NUM_LIST_PAGES+1)
            log(f"[STATUS] Indexing book list pages {i} to {last-1} (out of {NUM_LIST_PAGES})")
            tasks = [asyncio.ensure_future(indexBookList(pageNumber, session)) for pageNumber in range(i, last)]
            await asyncio.gather(*tasks)

async def indexBookList(pageNumber, session):
    URLs = await getBookURLs(pageNumber, session)
    addProgressGoalBooks(len(URLs))
    if (len(URLs) != 100):
        log(f"[BOOK FETCH] Page number {pageNumber} found {len(URLs)} URLs")
    if (60533475 in URLs):
        log(f"weird book {60533475} found on page {pageNumber}")
    tasks = [asyncio.ensure_future(indexBook(URL, session)) for URL in URLs]
    await asyncio.gather(*tasks)

async def getBookURLs(pageNumber, session):
    # print(f"Book list (page {pageNumber}): Fetching list...")
    page = await fetch(session, GOODREADS_BOOKLIST_URL + str(pageNumber))
    soup = BeautifulSoup(page, "html.parser")
    entries = soup.find("div", id="all_votes").find_all("tr")
    URLs = [GOODREADS_URL + entry.find("a", class_="bookTitle")["href"] for entry in entries]
    # print(f"Book list (page {pageNumber}): List fetched")
    return URLs

async def indexBook(URL, session):
    # print(f"Indexing components {URL}")
    page = await fetch(session, URL)
    soup = BeautifulSoup(page, "html.parser")
    # print(soup.prettify())
    result = {}

    # Isolate metadata
    mainContent = soup.find("div", class_="BookPage__mainContent")
    if mainContent is None:     # Book is potentially private for offline users
        # log(f"[RETRY BOOK]: Book may be private: {URL}")
        page = await fetch(session, URL, loggedin=True)
        soup = BeautifulSoup(page, "html.parser")
        mainContent = soup.find("div", class_="BookPage__mainContent")
        if mainContent is None:
            log(f"[ERROR]: Book not found: {URL}")
            updateErrorsBooks()
            return

    # Get title
    try:
        result["title"] = mainContent.find("h1", class_="Text Text__title1").text
    except:
        log(f"[FATAL] Book title not found on {URL}")
        raise Exception()

    # Get abstract
    abstract = mainContent.find("div", class_="DetailsLayoutRightParagraph__widthConstrained")
    if abstract is None:
        print(f"crash point abstract on {URL}")
    for br in abstract.find_all("br"):
        br.replace_with("\n")
    result["abstr"] = abstract.text

    # Get author (assuming just one, reason: https://www.goodreads.com/components/show/7190.The_Three_Musketeers)
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
    numRatings = int(numRatings.replace("ratings", "").replace("rating", "").strip().replace(",", ""))
    numReviews = int(numReviews.replace("reviews", "").replace("review", "").strip().replace(",", ""))
    result["numRatings"] = numRatings
    result["numReviews"] = numReviews
    # print(URL)


    result["id"] = URLtoID(URL)
    addBookToIndex(result)

def addBookToIndex(data):
    # client.index(index=getenv("ES_INDEX"),
    #          id=numIndexedBooks,      # =data["id"]
    #          document=data)
    # print("Indexed book " + data["title"] + " by " + data["author"])
    updateProgressBooks()

async def fetch(session, url, loggedin=False):
    # start = time.time()
    status = 404
    attempts = 0
    cookies = {}
    if (loggedin):
        # log(f"Using cookies on {url}")
        cookies = COOKIES
    repeatedExceptions = False
    while status != 200 and attempts < 5:
        updateProgressRequests()
        try:
            async with session.get(url, cookies=cookies) as response:
                text = await response.text()
                status = response.status
                if status == 200:
                    # elapsed = time.time() - start
                    # log(f"Fetch {url} took {elapsed}")
                    return text
                attempts += 1
                if (status != 502 and status != 504) or attempts > 1:
                    log(f"[RETRY FETCH]: {status} on {url}")
        except:
            if (repeatedExceptions):
                log(f"[RETRY FETCH]: Threw repeated exception on {url}")
            repeatedExceptions = True
            await asyncio.sleep(5)
    log(f"[FATAL]: Fetch failed after multiple attempts on {url}")



### User scraping ###

FRIEND_DEPTH = 0
async def indexUsers():
    log("[STATUS] Starting user indexing...")
    async with aiohttp.ClientSession(connector=aiohttp.TCPConnector(limit=200)) as session:
        userIDs = await getUserIDs(session)
        addProgressGoalUsers(len(userIDs))
        # tasks = [asyncio.ensure_future(indexUser(session, userID, FRIEND_DEPTH)) for userID in userIDs]
        # await asyncio.gather(*tasks)
        for userID in userIDs:
            await indexUser(session, userID, FRIEND_DEPTH)

async def getUserIDs(session):
    # Retrieved from top users in the world for "this" week (https://www.goodreads.com/user/best_reviewers?country=all&duration=w)
    # return [53701594, 29005117, 113964939, 32879029, 22106879, 5599497, 124132123, 16958299, 151231754, 6431467, 49815208, 48328025, 3569327, 138801181, 4622890, 128034500, 149694522, 60866073, 19283284, 154684875, 142072672, 22189348, 10490224, 82156089, 42130592, 89100122, 10171516, 1720620, 29981066, 48727754, 1323413, 27304766, 11215896, 150076375, 91622714, 30181442, 66222749, 13427823, 106675807, 80549046, 156768790, 11345366, 120762651, 138277086, 148600677, 11701608, 4674014, 8114361, 10477405, 4125660, 59458347, 54835325, 25400887, 134523072, 151334777, 721595, 117399210, 3672777, 78009594, 137111152, 1151637, 39575951, 107658832, 89964678, 26560207, 155007415, 38610813, 142245488, 35794399, 110912303, 104791668, 70395042, 5032725, 2190064, 142709713, 8338960, 1232712, 151638606, 91520258, 11626803, 11284813, 1526851, 77509618, 5009669, 60964126, 129743582, 3978225, 41321285, 112332654, 103654355, 67861858, 45147300, 17119647, 2846645, 128403534, 161893172, 42926711, 110612670]
    page = await fetch(session, "https://www.goodreads.com/user/best_reviewers?country=all&duration=w", loggedin=True)
    soup = BeautifulSoup(page, "html.parser")
    entries = soup.find("table", class_="tableList").find_all("tr")
    ids = [URLtoID(entry.find_all("td")[2].find_all("a")[0]["href"]) for entry in entries]
    # print(ids)
    return ids

async def indexUser(session, userID, depth):
    tasks = [indexUserRatings(session, userID), getFriendIDs(session, userID, depth)]
    values = await asyncio.gather(*tasks)
    updateProgressUsers()
    friendIDs = values[1]
    addProgressGoalUsers(len(friendIDs))
    recursive = [indexUser(session, ID, depth-1) for ID in friendIDs]
    await asyncio.gather(*recursive)

async def indexUserRatings(session, userID):
    numPages = await getUserRatingPages(session, userID)
    # log(f"user {userID} has {numPages} pages")
    if (numPages == 0):
        log(f"{userID} is private")
        return
    tasks = [indexUserRatingsPage(session, userID, page) for page in range(1, numPages+1)]
    await asyncio.gather(*tasks)

async def getUserRatingPages(session, userID):
    page = await fetch(session, f"https://www.goodreads.com/review/list/{userID}?page=1&per_page=100&shelf=read&utf8=✓&view=reviews", loggedin=True)
    soup = BeautifulSoup(page, "html.parser")
    pagination = soup.find("div", id="reviewPagination")
    if pagination is None:  # User has either just one page or is private
        private = soup.find("div", id="privateProfile")
        if private is None: 
            # log(f"{userID} has only 1 page..?")
            return 1
        else:
            return 0
    pages = pagination.find_all("a")
    lastPage = pages[-2].text
    return int(lastPage)

RATING = {
    "did not like it": 1,
    "it was ok" : 2,
    "liked it" : 3,
    "really liked it" : 4,
    "it was amazing" : 5
}
def ratingToInt(rating):
    return RATING[rating]

async def indexUserRatingsPage(session, userID, pageNumber):
    # log(f"fetyching {userID} page {pageNumber}")
    page = await fetch(session, f"https://www.goodreads.com/review/list/{userID}?page={pageNumber}&per_page=100&shelf=read&utf8=✓&view=reviews", loggedin=True)
    # start = time.time()
    soup = BeautifulSoup(page, "html.parser")
    entries = soup.find("tbody", id="booksBody").find_all("tr")
    for entry in entries:
        data = {}
        data["userID"] = userID
        data["bookID"] = URLtoID(entry.find("div", class_="js-tooltipTrigger tooltipTrigger").find("a")["href"])
        try:
            title = entry.find("td", class_="field title").find("a").text
            data["title"] = title.strip().split("\n")[0]
        except:
            log(f"unable to find title on user {userID} page {pageNumber} book {data['bookID']}")
            raise Exception()
        # print(f"from {title.encode()} to {data['title']}")
        try:
            author = entry.find("td", class_="field author").find("a").text.split(", ")
        except:
            # log(f"unable to find author on user {userID} page {pageNumber} book {data['title']}")
            return  # no author, illegitimate, skip
        if (len(author) == 1):
            data["author"] = author[0]
        elif (len(author) == 2):
            data["author"] = author[1] + " " + author[0]
        elif (len(author) == 3):
            data["author"] = author[1] + " " + author[0] + " " + author[2]
        else:
            log(f"weird author review user {userID} page {pageNumber}: {', '.join(author)}")
        rating = entry.find("td", class_="field rating").find_all("span", class_="staticStar")[0].text
        # print(rating)
        if (rating == ""):
            # log(f"dropping {userID} page {pageNumber} book {data['title']} by {data['author']}")
            continue
        data["rating"] = ratingToInt(rating)
        # print(str(userID) + " " + str(pageNumber))
        addRatingToIndex(data)
    # elapsed = time.time() - start
    # log(f"[STATUS] Processed user {userID} page {pageNumber}, took {elapsed}")

def addRatingToIndex(data):
    # client.index(index="ratings",
    #          document=data)
    # print(data)
    return

async def getFriendIDs(session, userID, depth):
    if (depth <= 0):
        return []
    print("getting friends")


def URLtoID(URL):
    return int(URL.split("/")[-1].split("-")[0].split(".")[0])


### Progress printing ###

numProgressBooks = 0
numErrorBooks = 0
numTotalBooks = 0
numProgressUsers = 0
numErrorUsers = 0
numTotalUsers = 0
numTotalRequests = 0
def updateProgressRequests():
    global numTotalRequests
    numTotalRequests += 1
    printProgress()
def updateProgressBooks():
    global numProgressBooks
    numProgressBooks += 1
    printProgress()
def addProgressGoalBooks(numBooks):
    global numTotalBooks
    numTotalBooks += numBooks
    printProgress()
def updateErrorsBooks():
    global numErrorBooks
    numErrorBooks += 1
def updateProgressUsers():
    global numProgressUsers
    numProgressUsers += 1
    printProgress()
def addProgressGoalUsers(numUsers):
    global numTotalUsers
    numTotalUsers += numUsers
    printProgress()
def updateErrorsUsers():
    global numErrorUsers
    numErrorUsers += 1
def printProgress():
    bookPrint = f"Indexed books: {numProgressBooks}/{numTotalBooks}"
    padding = " "*(30-len(bookPrint))
    userPrint = f"Indexed users: {numProgressUsers}/{numTotalUsers}"
    padding2 = " "*(30-len(userPrint))
    requestPrint = f"Total requests: {numTotalRequests}"
    sys.stdout.write(f"\r{bookPrint}{padding}{userPrint}{padding2}{requestPrint}")
    sys.stdout.flush()

def log(msg):
    print("\r" + msg + " "*(90-len(msg)))
    printProgress()

async def main():
    timeStart = time.time()
    printProgress()
    tasks = [
        indexBooks(),
        indexUsers()
    ]
    await asyncio.gather(*tasks)
    # await indexBooks()
    # await indexUsers()
    timeEnd = time.time()
    timeElapsed = timeEnd - timeStart
    print()
    global numErrorBooks
    print(f"Non-existing books: {numErrorBooks}")
    minutes = int(timeElapsed / 60)
    seconds = int(timeElapsed % 60)
    print(f"Elapsed time: {minutes}m {seconds}s")


if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    loop.run_until_complete(main())

