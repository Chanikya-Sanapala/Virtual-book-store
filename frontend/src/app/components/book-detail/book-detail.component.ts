import { Component, OnInit } from '@angular/core';
// Triggering re-compilation
import { ActivatedRoute, Router } from '@angular/router';
import { BookService } from '../../services/book.service';
import { CartService } from '../../services/cart.service';
import { ReviewService } from '../../services/review.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Book, Review } from '../../models/interfaces';

@Component({
  selector: 'app-book-detail',
  templateUrl: './book-detail.component.html',
  styleUrls: ['./book-detail.component.css'],
  standalone: false
})
export class BookDetailComponent implements OnInit {
  book: Book | null = null;
  realReviews: Review[] = [];
  isLoading = true;
  errorMessage = '';
  quantity = 1;
  activeTab = 'description';

  // For review form
  newReviewRating = 5;
  newReviewComment = '';
  isSubmittingReview = false;

  // Mock data for premium feel
  specifications = [
    { label: 'Publisher', value: 'LeafyBooks Publishing' },
    { label: 'Language', value: 'English' },
    { label: 'Print length', value: '320 pages' },
    { label: 'Dimensions', value: '6.14 x 0.72 x 9.21 inches' }
  ];

  reviews = [
    { user: 'Sarah J.', rating: 5, comment: 'Absolutely loved this book! The story was gripping from start to finish.', date: '2 days ago' },
    { user: 'Michael R.', rating: 4, comment: 'Great read, though the middle section felt a bit slow. Highly recommend though!', date: '1 week ago' },
    { user: 'Emily B.', rating: 5, comment: 'A masterpiece. One of the best books I have read this year.', date: '2 weeks ago' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private cartService: CartService,
    private reviewService: ReviewService,
    private authService: AuthService,
    public notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.loadBook(id);
        this.loadReviews(id);
      }
    });
  }

  loadBook(id: string): void {
    this.isLoading = true;
    this.bookService.getBook(id).subscribe({
      next: (data) => {
        this.book = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading book:', err);
        this.errorMessage = 'Could not find the book you are looking for.';
        this.isLoading = false;
      }
    });
  }

  loadReviews(id: string): void {
    this.reviewService.getReviewsByBook(id).subscribe({
      next: (data) => {
        this.realReviews = data;
      }
    });
  }

  submitReview(): void {
    console.log('>>> submitReview: Attempting to submit review...');
    if (!this.authService.isLoggedIn()) {
      console.warn('>>> submitReview: User not logged in, redirecting to login');
      this.notificationService.show('Please log in to leave a review.', 'info');
      this.router.navigate(['/login']);
      return;
    }

    if (!this.newReviewComment.trim()) {
      console.warn('>>> submitReview: Comment is empty');
      return;
    }

    this.isSubmittingReview = true;
    const user = this.authService.currentUserValue;
    console.log('>>> submitReview: User info:', user);
    
    const review: Review = {
      bookId: this.book?.id || '',
      userId: user.id || '',
      username: user.username,
      rating: this.newReviewRating,
      comment: this.newReviewComment
    };

    console.log('>>> submitReview: Review object:', review);

    this.reviewService.addReview(review).subscribe({
      next: (savedReview) => {
        console.log('>>> submitReview: Successfully saved review:', savedReview);
        this.realReviews.unshift(savedReview);
        this.newReviewComment = '';
        this.newReviewRating = 5;
        this.isSubmittingReview = false;
        this.notificationService.show('Review submitted successfully!', 'success');
      },
      error: (err) => {
        console.error('>>> submitReview: Error submitting review:', err);
        this.isSubmittingReview = false;
        this.notificationService.show('Failed to submit review. Please try again.', 'error');
      }
    });
  }


  addToCart(): void {
    if (this.book) {
      for (let i = 0; i < this.quantity; i++) {
        this.cartService.addToCart(this.book);
      }
    }
  }

  buyNow(): void {
    this.addToCart();
    this.router.navigate(['/payment']);
  }

  updateQuantity(delta: number): void {
    const newQty = this.quantity + delta;
    if (newQty >= 1 && newQty <= (this.book?.stock || 1)) {
      this.quantity = newQty;
    }
  }

  getStarArray(rating: number): number[] {
    return Array(5).fill(0).map((x, i) => i < rating ? 1 : 0);
  }
}
