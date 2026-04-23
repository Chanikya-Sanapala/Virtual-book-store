import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
  standalone: false
})
export class NavbarComponent {
  cartCount = 0;

  constructor(public authService: AuthService, private cartService: CartService) {
    this.cartService.getCart().subscribe(items => {
      this.cartCount = items.reduce((acc, item) => acc + item.quantity, 0);
    });
  }

  logout() {
    this.authService.logout();
  }
}
