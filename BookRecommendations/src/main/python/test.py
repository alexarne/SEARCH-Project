import requests

response = requests.get('https://www.goodreads.com/user/show/38610813-paromjit')
print(response.text)