import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../service/auth.service';
import { validateUsernameFormat } from '../login/login.validators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnInit {
  isLoading = false;
  token: string | null = null;
  inviteEmail: string | null = null;
  inviteRole: string | null = null;
  inviteLoaded = false;
  errorMessage: string | null = null;
  showAllErrors = false;

  formData = new FormGroup({
    firstName: new FormControl('', { validators: [Validators.required] }),
    lastName: new FormControl('', { validators: [Validators.required] }),
    username: new FormControl('', { validators: [Validators.required, validateUsernameFormat] }),
    password: new FormControl('', { validators: [Validators.required, Validators.minLength(5)] }),
    confirmPassword: new FormControl('', { validators: [Validators.required] })
  });

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      if (!this.token) {
        this.errorMessage = "No registration token provided. Please use the link sent to your email.";
        return;
      }

      this.authService.getInviteDetails(this.token).subscribe({
        next: (response) => {
          this.inviteEmail = response.mainBody.email;
          this.inviteRole = response.mainBody.role;
          this.inviteLoaded = true;
        },
        error: (err) => {
          this.inviteLoaded = false;
          this.errorMessage = err.error?.message || 'This invitation is invalid, expired, or already used.';
        }
      });
    });
  }

  get f() { return this.formData.controls; }

  formatRole(role: string | null): string {
    return role ? role.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()) : '';
  }

  onSubmit(): void {
    if (this.formData.invalid) {
      this.showAllErrors = true;
      return;
    }

    if (this.formData.value.password !== this.formData.value.confirmPassword) {
      this.errorMessage = "Passwords do not match.";
      return;
    }

    if (!this.token || !this.inviteLoaded) {
      this.errorMessage = "Invalid or missing token.";
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    const payload = {
      token: this.token,
      firstName: this.formData.value.firstName,
      lastName: this.formData.value.lastName,
      username: this.formData.value.username,
      password: this.formData.value.password,
      confirmPassword: this.formData.value.confirmPassword
    };

    this.authService.registerWithToken(payload).subscribe({
      next: (res) => {
        this.isLoading = false;
        // Upon successful registration, redirect to login
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || "Registration failed. The token may be expired or invalid.";
      }
    });
  }
}
