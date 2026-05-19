import urllib.request
import json
import csv
import random
import time

def fetch_books():
    print("Fetching books from OpenLibrary...")
    url = "https://openlibrary.org/subjects/fiction.json?limit=800"
    
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
    
    works = data.get('works', [])
    print(f"Found {len(works)} works. Filtering for those with covers...")
    
    books = []
    for work in works:
        if 'cover_id' in work and work['cover_id']:
            title = work.get('title', 'Unknown Title')
            
            authors = work.get('authors', [])
            author_name = authors[0].get('name', 'Unknown Author') if authors else 'Unknown Author'
            
            cover_id = work['cover_id']
            image_url = f"https://covers.openlibrary.org/b/id/{cover_id}-L.jpg"
            
            # OpenLibrary subject search doesn't give ISBN directly, so we generate a mock one or just use the work ID
            # Our backend accepts any string as ISBN.
            isbn = str(random.randint(1000000000000, 9999999999999))
            
            price = round(random.uniform(9.99, 29.99), 2)
            stock = random.randint(5, 50)
            
            books.append({
                'isbn': isbn,
                'title': title,
                'author': author_name,
                'category': 'Fiction',
                'description': f"A fantastic book by {author_name}.",
                'image': image_url,
                'price': price,
                'stock': stock
            })
            
            if len(books) == 500:
                break
                
    print(f"Successfully compiled {len(books)} books with real covers!")
    
    # Also fetch some non-fiction just to mix it up if we have room
    if len(books) < 500:
        url_nf = "https://openlibrary.org/subjects/science.json?limit=300"
        req_nf = urllib.request.Request(url_nf, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_nf) as response:
            data_nf = json.loads(response.read().decode())
            
        for work in data_nf.get('works', []):
            if 'cover_id' in work and work['cover_id']:
                books.append({
                    'isbn': str(random.randint(1000000000000, 9999999999999)),
                    'title': work.get('title', 'Unknown Title'),
                    'author': work.get('authors', [{'name':'Unknown'}])[0].get('name'),
                    'category': 'Science',
                    'description': "An insightful non-fiction read.",
                    'image': f"https://covers.openlibrary.org/b/id/{work['cover_id']}-L.jpg",
                    'price': round(random.uniform(14.99, 39.99), 2),
                    'stock': random.randint(5, 30)
                })
                if len(books) == 500:
                    break

    # Write to CSV
    filename = '500_beautiful_books.csv'
    print(f"Writing to {filename}...")
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=['isbn', 'title', 'author', 'category', 'description', 'image', 'price', 'stock'])
        writer.writeheader()
        writer.writerows(books)
        
    print("Done! You can now upload this CSV.")

if __name__ == '__main__':
    fetch_books()
