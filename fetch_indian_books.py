import urllib.request
import json
import csv
import random

def fetch_books():
    print("Fetching Indian books from OpenLibrary...")
    
    # Define subjects related to India
    subjects = [
        ("india", "Fiction", "An incredible book set in India."),
        ("indian_literature", "Fiction", "A masterpiece of Indian literature."),
        ("indian_history", "Non-Fiction", "An insightful read on Indian history.")
    ]
    
    books = []
    
    for subject, category, default_desc in subjects:
        url = f"https://openlibrary.org/subjects/{subject}.json?limit=500"
        print(f"Querying: {url}")
        
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req) as response:
                data = json.loads(response.read().decode())
                
            works = data.get('works', [])
            print(f"Found {len(works)} works for '{subject}'. Filtering for those with covers...")
            
            for work in works:
                if 'cover_id' in work and work['cover_id']:
                    title = work.get('title', 'Unknown Title')
                    
                    authors = work.get('authors', [])
                    author_name = authors[0].get('name', 'Unknown Author') if authors else 'Unknown Author'
                    
                    cover_id = work['cover_id']
                    image_url = f"https://covers.openlibrary.org/b/id/{cover_id}-L.jpg"
                    
                    # Generate mock ISBN
                    isbn = str(random.randint(1000000000000, 9999999999999))
                    
                    # Prices mapped to INR equivalent roughly in USD for consistency (e.g. $5 - $25)
                    price = round(random.uniform(5.99, 25.99), 2)
                    stock = random.randint(10, 100)
                    
                    books.append({
                        'isbn': isbn,
                        'title': title,
                        'author': author_name,
                        'category': category,
                        'description': f"{title} by {author_name}. {default_desc}",
                        'image': image_url,
                        'price': price,
                        'stock': stock
                    })
                    
                    if len(books) >= 500:
                        break
                        
            if len(books) >= 500:
                break
                
        except Exception as e:
            print(f"Failed to fetch {subject}: {e}")

    # Remove duplicates based on title
    unique_books = []
    seen_titles = set()
    for book in books:
        if book['title'] not in seen_titles:
            unique_books.append(book)
            seen_titles.add(book['title'])
            
    print(f"Successfully compiled {len(unique_books)} unique Indian books with real covers!")
    
    # Write to CSV
    filename = 'indian_books_collection.csv'
    print(f"Writing to {filename}...")
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=['isbn', 'title', 'author', 'category', 'description', 'image', 'price', 'stock'])
        writer.writeheader()
        writer.writerows(unique_books)
        
    print("Done! You can now upload this CSV.")

if __name__ == '__main__':
    fetch_books()
