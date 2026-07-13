import {
    HttpClient,
    HttpErrorResponse,
    HttpHeaders,
} from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, of, Subscription } from 'rxjs';
import { Response } from '../response/response';
import { BACKEND_URL } from '../../global_constants';
import { validateUsernameFormat } from './login.validators';
import { Router } from '@angular/router';
import { AuthService } from '../service/auth.service';


@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  isLoading = false;
  private authSub!: Subscription;


  constructor(private authService: AuthService, private httpClient: HttpClient, private router: Router) {
    
  }


  formData = new FormGroup({
    username: new FormControl('', {
      validators: [Validators.required, validateUsernameFormat],
    }),
    password: new FormControl('', {
      validators: [Validators.required],
    }),
  });

  username!: FormControl;
  password!: FormControl;

  ngOnInit(): void {
    this.username = this.formData.controls.username;
    this.password = this.formData.controls.password;
    this.authSub = this.authService.isLoggingIn$.subscribe(loading => {
      this.isLoading = loading;
    });
  }

  ngOnDestroy(): void {
    if (this.authSub) {
      this.authSub.unsubscribe();
    }
  }

  showAllErrors = false;
  onSubmit() {
    if(this.formData.invalid) {
      this.showAllErrors = true;
    }
    const authenticationDetails = `${this.username.value}:${this.password.value}`;
    this.authService.login(authenticationDetails);
  }

}
