import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BACKEND_URL } from '../../../global_constants';
import { Response } from '../../response/response';
import { FormsModule } from '@angular/forms';

interface UserDto {
  uid: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  role: string;
}

@Component({
  selector: 'app-manage-users',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './manage-users.component.html',
  styleUrl: './manage-users.component.scss',
})
export class ManageUsersComponent implements OnInit {
  users: UserDto[] = [];
  isLoaded = false;
  loadError = false;
  availableRoles = ['DEPARTMENT_HEAD', 'DEPARTMENT_MEMBER', 'COMMITTEE_MEMBER', 'GUEST'];
  feedbackMessage: string | null = null;
  feedbackType: 'success' | 'error' = 'success';

  constructor(private httpClient: HttpClient) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loadError = false;
    this.httpClient
      .get<Response<UserDto[]>>(BACKEND_URL + '/api/users', { withCredentials: true })
      .subscribe({
        next: (response) => {
          this.users = response.mainBody;
          this.isLoaded = true;
        },
        error: (error) => {
          // Don't leave the page stuck on the loader — surface a retryable error.
          this.loadError = true;
          console.error('Failed to load users', error);
        },
      });
  }

  onRoleChange(user: UserDto, newRole: string): void {
    const previousRole = user.role;
    this.httpClient
      .patch<Response<any>>(
        `${BACKEND_URL}/api/users/${user.uid}/role`,
        { role: newRole },
        { withCredentials: true }
      )
      .subscribe({
        next: () => {
          user.role = newRole;
          this.feedbackMessage = `${user.firstName} ${user.lastName}'s role updated to ${this.formatRole(newRole)}`;
          this.feedbackType = 'success';
          this.clearFeedback();
        },
        error: (error) => {
          user.role = previousRole;
          this.feedbackMessage = 'Failed to update role. Please try again.';
          this.feedbackType = 'error';
          this.clearFeedback();
          console.error('Role update failed', error);
        },
      });
  }

  formatRole(role: string): string {
    return role.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()).replace(/\B\w+/g, (c) => c.toLowerCase());
  }

  private clearFeedback(): void {
    setTimeout(() => {
      this.feedbackMessage = null;
    }, 3000);
  }
}
