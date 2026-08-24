import { Component, input } from '@angular/core';
import { GlobalSearchComponent } from './global-search/global-search.component';
import { RouterLink } from '@angular/router';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [GlobalSearchComponent, RouterLink],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  showGlobalSearchDialog = false;
  isLoggedIn = input.required<boolean>();

  constructor(private authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
