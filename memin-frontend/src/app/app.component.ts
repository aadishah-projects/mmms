import { Component, HostListener } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { HeaderComponent } from './header/header.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { AuthService } from './service/auth.service';
import { PopupComponent } from './popup/popup.component';
import { BACKEND_URL } from '../global_constants';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    HeaderComponent,
    SidebarComponent /*DemoComponent*/,
    PopupComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  isLoggedIn = false;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {
    this.authService.loggedIn$.subscribe((value) => (this.isLoggedIn = value));
  }

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    // Using Alt + N instead to avoid browser conflict
    const isAltPressed = event.altKey;
    const isNPressed = event.code === 'KeyN';
    const isMPressed = event.code == 'KeyM';
    const isCPressed = event.code == 'KeyC';

    if (isAltPressed && isNPressed) {
      event.preventDefault();
      this.router.navigate(['/home/create-meeting']);
    } else if (isAltPressed && isMPressed) {
      event.preventDefault();
      this.router.navigate(['/home/create-member']);
    } else if (isAltPressed && isCPressed) {
      event.preventDefault();
      this.router.navigate(['/home/create-committee']);
    }
  }
}
