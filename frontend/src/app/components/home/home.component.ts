import { Component, OnInit } from '@angular/core';
import { BookService } from '../../services/book.service';
import { CartService } from '../../services/cart.service';
import { Book } from '../../models/interfaces';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  standalone: false
})
export class HomeComponent implements OnInit {
  books: Book[] = [];
  searchQuery = '';
  selectedCategory = '';
  categories = ['Fiction', 'Non-Fiction', 'Science', 'Technology', 'Biography', 'Self-Help'];

  constructor(private bookService: BookService, private cartService: CartService) { }

  ngOnInit(): void {
    this.loadBooks();
  }

  loadBooks() {
    this.bookService.getBooks(this.searchQuery).subscribe(data => {
      this.books = data;
    });
  }

  filterByCategory(category: string) {
    this.selectedCategory = category;
    if (category) {
      this.bookService.getBooksByCategory(category).subscribe(data => {
        this.books = data;
      });
    } else {
      this.loadBooks();
    }
  }

  addToCart(book: Book) {
    this.cartService.addToCart(book);
  }

  onSearch() {
    this.loadBooks();
  }
}
