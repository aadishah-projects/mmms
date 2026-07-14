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
  isSecretary: boolean = false;
  private roleSub!: Subscription;
  private secSub!: Subscription;

  constructor(private route: ActivatedRoute, private authService: AuthService) {}

  ngOnInit() {
    this.roleSub = this.authService.userRole$.subscribe(role => {
      this.userRole = role;
    });
    this.secSub = this.authService.isSecretary$.subscribe(isSec => {
      this.isSecretary = isSec;
    });
  }

  ngOnDestroy() {
    if (this.roleSub) {
      this.roleSub.unsubscribe();
    }
    if (this.secSub) {
      this.secSub.unsubscribe();
    }
  }

  get canWrite(): boolean {
    return this.isSecretary || this.userRole === 'DEPARTMENT_HEAD';
  }

  // Method to get only the committeeId query parameter and ignoring other query parameters if any
  getCommitteeQueryParams(): { [key: string]: any } {
    const currentParams = this.route.snapshot.queryParams;
    return currentParams['committeeId']
      ? { committeeId: currentParams['committeeId'] }
      : {};
  }
}
