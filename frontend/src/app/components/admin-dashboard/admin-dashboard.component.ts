import { Component, OnInit } from '@angular/core';
import { BookService } from '../../services/book.service';
import { Book } from '../../models/interfaces';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css'],
  standalone: false
})
export class AdminDashboardComponent implements OnInit {
  books: Book[] = [];
  newBook: Book = this.resetBook();
  isEditing = false;
  editingId = '';

  constructor(private bookService: BookService, private notificationService: NotificationService) { }

  ngOnInit(): void {
    this.loadBooks();
  }

  loadBooks() {
    this.bookService.getBooks().subscribe(data => {
      this.books = data;
    });
  }

  onSubmit() {
    if (this.isEditing) {
      this.bookService.updateBook(this.editingId, this.newBook).subscribe(() => {
        this.loadBooks();
        this.cancelEdit();
      });
    } else {
      this.bookService.addBook(this.newBook).subscribe({
        next: () => {
          this.loadBooks();
          this.newBook = this.resetBook();
          this.notificationService.show('Book added successfully!');
        },
        error: (err) => {
          console.error(err);
          this.notificationService.show('Failed to add book. Access Denied.', 'error');
        }
      });
    }
  }

  editBook(book: Book) {
    this.isEditing = true;
    this.editingId = book.id!;
    this.newBook = { ...book };
  }

  deleteBook(id: string) {
    if (confirm('Are you sure you want to delete this book?')) {
      this.bookService.deleteBook(id).subscribe({
        next: () => {
          this.loadBooks();
          this.notificationService.show('Book deleted.');
        },
        error: () => this.notificationService.show('Delete failed.', 'error')
      });
    }
  }

  cancelEdit() {
    this.isEditing = false;
    this.editingId = '';
    this.newBook = this.resetBook();
  }

  private resetBook(): Book {
    return {
      title: '',
      author: '',
      description: '',
      category: 'Fiction',
      price: 0,
      stock: 0,
      imageUrl: ''
    };
  }
}
