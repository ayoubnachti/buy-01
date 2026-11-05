import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CartStore } from '../../../core/services/cart.store';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  private readonly router = inject(Router);
  private authService = inject(AuthService);
  private readonly cartStore = inject(CartStore);
  readonly cartCount = this.cartStore.count;

  isLoggedIn = computed(() => this.authService.user() !== null);
  isSeller = computed(() => this.authService.user()?.role === 'SELLER');
  isClient = computed(() => this.authService.user()?.role === 'CLIENT');

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}