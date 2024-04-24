from bs4 import BeautifulSoup
import asyncio
import aiohttp
import sys
from elasticsearch import Elasticsearch
from os import getenv
from dotenv import load_dotenv

GOODREADS_URL = "https://www.goodreads.com"
GOODREADS_BOOKLIST_URL = "https://www.goodreads.com/list/show/1.Best_Books_Ever?page="
GOODREADS_USERLIST_URL = "https://www.goodreads.com/user/best_reviewers?country=all&duration=w"
NUM_LIST_PAGES = 10
ELASTIC_INSERT_URL = "https://localhost:9200/"


COOKIES = {
    "lc-main" : "en_US",
    "sess-at-main" : "\"GaDSddEYrwQ021iOXsJF9RfTpZlv7bPIan0f8e7lLAk=\"",
    "csm-sid" : "600-4915203-3462935",
    "ubid-main" : "131-4782610-8999937",
    "session-id-time" : "2341755309l",
    "session-token" : "\"L+SZUx79BbyOUe1yogaZcJJzyGAVsJysfOYP2lA2YDMKG7PJifCNQgt3+Swt/wcD5SVDTQUmgQN/gqKpiaKdzHJn1BLAAJ/p8KOx44WqGkDHsDDmdyzO2Kp/5RIDGJQc0s5RxC2VWLuWJNaShXdW5BgNTPniGZoX6+JtJ/MGRq+lkiwXu5hymfIQVjaK0W8Yos10rfaRkzFUh0HIshd/nX+PK/Wz5TkyPsQnl9Fa/DtFeg3eHU+emBcbXOmbKi6RMZLHEaHoheKRZ5y7J+/JMoRzx51myj3Gc5dceqQyLzEGo3gEydqvu/3ov78mnBAzI80+k5vyRcJ2pMqsuEiiJ4wAZHSIaW5lZPxYXBSiXeCxVfD6r1z9Nw==\"",
    "locale" : "en",
    "likely_has_account" : "true",
    "__qca" : "P0-1063639988-1711035255766",
    "_session_id2" : "83c54e773d30b6123943ad3805b1ef7c",
    "at-main" : "Atza|IwEBIO-4L3362SddhBXZSmveNilFa_XmNt1Ai0FRTN3Z-OX3l0CvfBQeBdlf0qwqnP6XvOHQEFEixIBFmAnERp47xJPlEayNQ8bBB0vjvbSUf1czApz_aqR8-i1xOdl9ktZ8kTQlJqgaI4f8UXdWunED5SR7LtjkaM9Am061x3etTpXPDHPPmY911Jz4g1F-N-LWSHkjxerd3LgSLY_lTJikWkPYt8q_DYpQwyfhQ_qmt553gVaQJRDq6dZ_hUlsAelkj1I",
    "ccsid" : "200-7284764-5296829",
    "csm-hit" : "tb:s-WBNNTBJWXT5KZM8BC0HJ|1711035475698&t:1711035475698",
    "session-id" : "134-8973486-2506540",
    "x-main" : "\"ZXO1@7WmBVMUIGVLEP5aPT7NYe1aopvQ02@RM5tw6TNSNWNRxpyogrIrEBQa@FZj\""
}


HEADERS = {
    'User-Agent' : 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0' ,
    'Accept' : 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8' ,
    'Accept-Language' : 'sv-SE,sv;q=0.8,en-US;q=0.5,en;q=0.3' ,
    'Accept-Encoding' : 'gzip, deflate, br' ,
    'Connection' : 'keep-alive' ,
    'Cookie' : 'ccsid=200-7284764-5296829; __qca=P0-1063639988-1711035255766; session-id=134-8973486-2506540; session-id-time=2341755309l; lc-main=en_US; csm-hit=tb:s-WBNNTBJWXT5KZM8BC0HJ|1711035475698&t:1711035475698; ubid-main=131-4782610-8999937; session-token="L+SZUx79BbyOUe1yogaZcJJzyGAVsJysfOYP2lA2YDMKG7PJifCNQgt3+Swt/wcD5SVDTQUmgQN/gqKpiaKdzHJn1BLAAJ/p8KOx44WqGkDHsDDmdyzO2Kp/5RIDGJQc0s5RxC2VWLuWJNaShXdW5BgNTPniGZoX6+JtJ/MGRq+lkiwXu5hymfIQVjaK0W8Yos10rfaRkzFUh0HIshd/nX+PK/Wz5TkyPsQnl9Fa/DtFeg3eHU+emBcbXOmbKi6RMZLHEaHoheKRZ5y7J+/JMoRzx51myj3Gc5dceqQyLzEGo3gEydqvu/3ov78mnBAzI80+k5vyRcJ2pMqsuEiiJ4wAZHSIaW5lZPxYXBSiXeCxVfD6r1z9Nw=="; x-main="ZXO1@7WmBVMUIGVLEP5aPT7NYe1aopvQ02@RM5tw6TNSNWNRxpyogrIrEBQa@FZj"; at-main=Atza|IwEBIO-4L3362SddhBXZSmveNilFa_XmNt1Ai0FRTN3Z-OX3l0CvfBQeBdlf0qwqnP6XvOHQEFEixIBFmAnERp47xJPlEayNQ8bBB0vjvbSUf1czApz_aqR8-i1xOdl9ktZ8kTQlJqgaI4f8UXdWunED5SR7LtjkaM9Am061x3etTpXPDHPPmY911Jz4g1F-N-LWSHkjxerd3LgSLY_lTJikWkPYt8q_DYpQwyfhQ_qmt553gVaQJRDq6dZ_hUlsAelkj1I; sess-at-main="GaDSddEYrwQ021iOXsJF9RfTpZlv7bPIan0f8e7lLAk="; likely_has_account=true; csm-sid=600-4915203-3462935; locale=en; _session_id2=83c54e773d30b6123943ad3805b1ef7c' ,
    'Upgrade-Insecure-Requests' : '1' ,
    'Sec-Fetch-Dest' : 'document' ,
    'Sec-Fetch-Mode' : 'navigate' ,
    'Sec-Fetch-Site' : 'none' ,
    'Sec-Fetch-User' : '?1',
}

### Book scraping ###

load_dotenv()
client = Elasticsearch(
  ELASTIC_INSERT_URL,
  ssl_assert_fingerprint = getenv("ES_FINGERPRINT"),
  basic_auth=("elastic", getenv("ES_PASSWORD"))
)

client.options(ignore_status=[400,404]).indices.delete(index=getenv("ES_INDEX")) 
client.indices.create(index=getenv("ES_INDEX"))

async def indexBooks():
    async with aiohttp.ClientSession() as session:
        session.cookie_jar.update_cookies(COOKIES)
        print(session.cookie_jar)
        tasks = [asyncio.ensure_future(indexBookList(pageNumber, session)) for pageNumber in range(1, NUM_LIST_PAGES+1)]
        await asyncio.gather(*tasks)
        print()
        global numErrorBooks
        print(f"Non-existing books: {numErrorBooks}")

async def indexBookList(pageNumber, session):
    URLs = await getBookURLs(pageNumber, session)
    addProgressGoalBooks(len(URLs))
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
    if mainContent is None:
        print(f"\r[ERROR]: Book not found: {URL}")
        updateErrorsBooks()
        return

    # Get title
    result["title"] = mainContent.find("h1", class_="Text Text__title1").text

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
    numRatings = int(numRatings.replace("ratings", "").strip().replace(",", ""))
    numReviews = int(numReviews.replace("reviews", "").strip().replace(",", ""))
    result["numRatings"] = numRatings
    result["numReviews"] = numReviews
    # print(URL)
    addBookToIndex(result)

numIndexedBooks = 0
def addBookToIndex(data):
    global numIndexedBooks
    numIndexedBooks += 1
    data["id"] = numIndexedBooks
    client.index(index=getenv("ES_INDEX"),
             id=numIndexedBooks,
             document=data)
    print("Indexed book " + data["title"] + " by " + data["author"])
    updateProgressBooks()

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
                print(f"\r[RETRY FETCH]: {status} on {url}")
        except:
            print(f"\r[RETRY FETCH]: Fetch threw exception on {url}")
    print(f"\r[FATAL]: Fetch failed after multiple attempts on {url}")



### User scraping ###

FRIEND_DEPTH = 0
async def indexUsers():
    async with aiohttp.ClientSession() as session:
        numpages = await getUserRatingPages(session, 1036893)
        print(f"profile has {numpages} pages")
        numpages = await getUserRatingPages(session, 53701594)
        print(f"profile has {numpages} pages")
        numpages = await getUserRatingPages(session, 13616715)
        print(f"profile has {numpages} pages")
        numpages = await getUserRatingPages(session, 58488537)
        print(f"profile has {numpages} pages")
        userIDs = getUserIDs()
        addProgressGoalUsers(len(userIDs))
        tasks = [asyncio.ensure_future(indexUser(session, userID, FRIEND_DEPTH)) for userID in userIDs]
        await asyncio.gather(*tasks)

def getUserIDs():
    # Retrieved from top users in the world for "this" week (https://www.goodreads.com/user/best_reviewers?country=all&duration=w)
    return [53701594, 29005117, 113964939, 32879029, 22106879, 5599497, 124132123, 16958299, 151231754, 6431467, 49815208, 48328025, 3569327, 138801181, 4622890, 128034500, 149694522, 60866073, 19283284, 154684875, 142072672, 22189348, 10490224, 82156089, 42130592, 89100122, 10171516, 1720620, 29981066, 48727754, 1323413, 27304766, 11215896, 150076375, 91622714, 30181442, 66222749, 13427823, 106675807, 80549046, 156768790, 11345366, 120762651, 138277086, 148600677, 11701608, 4674014, 8114361, 10477405, 4125660, 59458347, 54835325, 25400887, 134523072, 151334777, 721595, 117399210, 3672777, 78009594, 137111152, 1151637, 39575951, 107658832, 89964678, 26560207, 155007415, 38610813, 142245488, 35794399, 110912303, 104791668, 70395042, 5032725, 2190064, 142709713, 8338960, 1232712, 151638606, 91520258, 11626803, 11284813, 1526851, 77509618, 5009669, 60964126, 129743582, 3978225, 41321285, 112332654, 103654355, 67861858, 45147300, 17119647, 2846645, 128403534, 161893172, 42926711, 110612670]

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

async def getUserRatingPages(session, userID):
    page = await fetch(session, f"https://www.goodreads.com/review/list/{userID}?page=1&per_page=100&sort=rating&utf8=%E2%9C%93&view=reviews")
    soup = BeautifulSoup(page, "html.parser")
    pagination = soup.find("div", id="reviewPagination")
    if pagination is None:
        print(f"\r{userID} has only 1 page..?" + " "*40)
        return 1
    pages = pagination.find_all("a")
    lastPage = pages[-2].text
    return int(lastPage)

async def indexUserRatingsPage(session, userID, pageNumber):
    print("indexing")

async def getFriendIDs(session, userID, depth):
    if (depth <= 0):
        return []
    print("getting friends")
    return "lmaomqdowdm"


### Progress printing ###

numProgressBooks = 0
numErrorBooks = 0
numTotalBooks = 0
numProgressUsers = 0
numErrorUsers = 0
numTotalUsers = 0
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
    sys.stdout.write(f"\r{bookPrint}{padding}{userPrint}")
    sys.stdout.flush()

async def main():
    printProgress()
    tasks = [indexBooks(), indexUsers()]
    await asyncio.gather(*tasks)

if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    loop.run_until_complete(main())

