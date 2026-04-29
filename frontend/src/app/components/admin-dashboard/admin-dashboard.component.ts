import { Component, OnInit } from '@angular/core';
import { BookService } from '../../services/book.service';
import { Book } from '../../models/interfaces';
import { NotificationService } from '../../services/notification.service';
import { AuthService } from '../../services/auth.service';
import { CATEGORIES } from '../../constants/categories';
import { timeout, catchError, of } from 'rxjs';

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
  userId = '';
  isUploading = false;
  isLoading = false;
  errorMessage = '';
  imagePreview: string | null = null;
  selectedCategory = '';
  categories: string[] = [];
  
  isImporting = false;
  importStatus = '';

  constructor(
    private bookService: BookService, 
    private notificationService: NotificationService,
    private authService: AuthService
  ) { 
    this.userId = this.authService.currentUserValue.id;
  }

  ngOnInit(): void {
    // Load categories first
    this.loadCategories();
    
    this.loadBooks();
    
    // Listen for global refreshes to keep inventory in sync
    this.bookService.bookRefresh$.subscribe(() => {
      // Clear cache to ensure fresh data
      this.bookService.clearCache();
      this.loadBooks();
    });
  }

  loadCategories() {
    this.bookService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
      },
      error: (err) => {
        console.error('Error loading categories:', err);
        // Fallback to hardcoded categories
        this.categories = CATEGORIES;
      }
    });
  }

  loadBooks() {
    this.isLoading = true;
    this.errorMessage = '';
    
    // Check if user is admin
    const isAdmin = this.authService.currentUserValue.roles.includes('ROLE_ADMIN');
    
    let observable;
    if (this.selectedCategory) {
      // For category filtering, get books by category first
      observable = this.bookService.getBooksByCategory(this.selectedCategory);
    } else if (!isAdmin && this.userId) {
      // Non-admin users see only their books
      observable = this.bookService.getBooksBySeller(this.userId);
    } else {
      // Admin users see all books
      observable = this.bookService.getBooks();
    }

    observable.pipe(
      timeout(10000),
      catchError(err => {
        console.error('AdminDashboard: Fetch failed', err);
        this.errorMessage = 'Connection timeout. Failed to retrieve books.';
        return of([]);
      })
    ).subscribe({
      next: (data) => {
        if (!isAdmin && this.userId) {
          // Filter non-admin user's books
          this.books = data.filter(b => b.sellerId === this.userId);
        } else {
          // Admin sees all books
          this.books = data;
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading books:', err);
        this.isLoading = false;
      }
    });
  }

  filterByCategory(category: string) {
    this.selectedCategory = category;
    this.loadBooks();
  }

  onSubmit() {
    this.isLoading = true;
    if (this.isEditing) {
      this.bookService.updateBook(this.editingId, this.newBook).pipe(
        timeout(10000),
        catchError(err => {
          this.notificationService.show('Update failed (timeout).', 'error');
          return of(null);
        })
      ).subscribe((res) => {
        if (res) {
          this.loadBooks();
          this.cancelEdit();
          this.notificationService.show('Book updated successfully!');
        }
        this.isLoading = false;
      });
    } else {
      this.bookService.addBook(this.newBook).pipe(
        timeout(10000),
        catchError(err => {
          this.notificationService.show('Failed to add book (timeout).', 'error');
          return of(null);
        })
      ).subscribe({
        next: (res) => {
          if (res) {
            this.loadBooks();
            this.newBook = this.resetBook();
            this.notificationService.show('Book added successfully!');
          }
          this.isLoading = false;
        },
        error: (err) => {
          console.error(err);
          this.notificationService.show('Failed to add book. Access Denied.', 'error');
          this.isLoading = false;
        }
      });
    }
  }

  editBook(book: Book) {
    this.isEditing = true;
    this.editingId = book.id!;
    this.newBook = { ...book };
    this.imagePreview = book.imageUrl;
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.isUploading = true;
      
      const reader = new FileReader();
      reader.onload = (e: any) => this.imagePreview = e.target.result;
      reader.readAsDataURL(file);

      this.bookService.uploadImage(file).subscribe({
        next: (res) => {
          this.newBook.imageUrl = res.url;
          this.isUploading = false;
          this.notificationService.show('Image uploaded successfully!');
        },
        error: (err) => {
          console.error(err);
          this.isUploading = false;
          this.notificationService.show('Upload failed.', 'error');
        }
      });
    }
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
    this.imagePreview = null;
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

  onCsvFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.isImporting = true;
      this.importStatus = 'Reading file and fetching details...';
      
      this.bookService.importBooks(file).subscribe({
        next: (response) => {
          this.importStatus = response;
          this.notificationService.show('Import completed successfully!');
          this.isImporting = false;
          // Refresh list
          this.loadBooks();
          // Reset status after a delay
          setTimeout(() => this.importStatus = '', 5000);
        },
        error: (err) => {
          console.error(err);
          this.importStatus = 'Import failed. Check CSV format.';
          this.notificationService.show('Bulk import failed.', 'error');
          this.isImporting = false;
        }
      });
    }
  }
}
