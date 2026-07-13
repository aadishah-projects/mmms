import { Component, OnInit } from '@angular/core';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  ActivatedRoute,
} from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLinkActive, RouterLink],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent implements OnInit {
  userRole: string | null = null;
  private roleSub!: Subscription;

  constructor(private route: ActivatedRoute, private authService: AuthService) {}

  ngOnInit() {
    this.roleSub = this.authService.userRole$.subscribe(role => {
      this.userRole = role;
    });
  }

  ngOnDestroy() {
    if (this.roleSub) {
      this.roleSub.unsubscribe();
    }
  }

  get canWrite(): boolean {
    return this.userRole === 'SECRETARY' || this.userRole === 'DEPARTMENT_HEAD';
  }

  // Method to get only the committeeId query parameter and ignoring other query parameters if any
  getCommitteeQueryParams(): { [key: string]: any } {
    const currentParams = this.route.snapshot.queryParams;
    return currentParams['committeeId']
      ? { committeeId: currentParams['committeeId'] }
      : {};
  }
}
