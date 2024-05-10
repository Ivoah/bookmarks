import json

with open('bookmarks.json') as f:
    bookmarks = json.load(f)

def printBookmark(bookmark, indent=0):
    print(f'{"  "*indent}{bookmark["title"]}: {bookmark["url"]}')
    for child in bookmark["children"]:
        printBookmark(child, indent+1)

for bookmark in bookmarks:
    printBookmark(bookmark)
