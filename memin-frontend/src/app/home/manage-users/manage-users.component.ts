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

interface CommitteeOption {
  id: number;
  name: string;
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
  availableRoles = ['DEPARTMENT_HEAD', 'DEPARTMENT_MEMBER', 'COMMITTEE_MEMBER', 'SECRETARY', 'GUEST'];
  committees: CommitteeOption[] = [];
  inviteEmail = '';
  inviteRole = 'DEPARTMENT_MEMBER';
  inviteCommitteeId: number | null = null;
  isInviting = false;
  feedbackMessage: string | null = null;
  feedbackType: 'success' | 'error' = 'success';

  constructor(private httpClient: HttpClient) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadCommittees();
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

  loadCommittees(): void {
    this.httpClient
      .get<Response<CommitteeOption[]>>(BACKEND_URL + '/api/my-active-committees', {
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          this.committees = response.mainBody;
        },
        error: (error) => {
          console.error('Failed to load committees for invitation', error);
        },
      });
  }

  sendInvite(): void {
    const email = this.inviteEmail.trim().toLowerCase();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email) || this.isInviting) {
      this.feedbackMessage = 'Enter a valid email address.';
      this.feedbackType = 'error';
      return;
    }

    this.isInviting = true;
    const payload: { email: string; role: string; committeeId?: number } = {
      email,
      role: this.inviteRole,
    };
    if (this.inviteCommitteeId !== null) {
      payload.committeeId = this.inviteCommitteeId;
    }

    this.httpClient
      .post<Response<any>>(
        `${BACKEND_URL}/api/invite`,
        payload,
        { withCredentials: true }
      )
      .subscribe({
        next: () => {
          this.inviteEmail = '';
          this.inviteCommitteeId = null;
          this.feedbackMessage = `Invitation sent to ${email}. They can create their username and password from the email link.`;
          this.feedbackType = 'success';
          this.isInviting = false;
          this.clearFeedback();
        },
        error: (error) => {
          this.feedbackMessage = error.error?.message || 'Could not send invitation. Please try again.';
          this.feedbackType = 'error';
          this.isInviting = false;
          this.clearFeedback();
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
