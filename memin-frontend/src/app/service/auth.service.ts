import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Subscription, tap } from 'rxjs';
import { BACKEND_URL } from '../../global_constants';
import { Response } from '../response/response';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private loggedIn = new BehaviorSubject<boolean>(false);
  loggedIn$ = this.loggedIn.asObservable();

  private loggingIn = new BehaviorSubject<boolean>(false);
  isLoggingIn$ = this.loggingIn.asObservable();

  private userRole = new BehaviorSubject<string | null>(null);
  userRole$ = this.userRole.asObservable();

  constructor(private httpClient: HttpClient, private router: Router) {
    this.checkAuthOnLoad();
  }

  checkAuthOnLoad() {
    this.httpClient
      .get<Response<Object>>(`${BACKEND_URL}/isAuthenticated`, {
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          if (response.mainBody && (response.mainBody as any).role) {
            this.userRole.next((response.mainBody as any).role);
          }
          this.loggedIn.next(true);
        },
	error: (error) => {
	  this.loggedIn.next(false);
	}
      });
  }

  subscription!: Subscription;

  login(formattedCredentials: string) {
    const formattedEncodedCredentials = btoa(formattedCredentials);

    const headers = new HttpHeaders({
      Authorization: `Basic ${formattedEncodedCredentials}`,
    });

    this.loggingIn.next(true);

    this.subscription = this.httpClient
      .get<Response<any>>(BACKEND_URL + '/api/login', {
        headers: headers,
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          console.log(response.message);
          if (response.mainBody && response.mainBody.role) {
            this.userRole.next(response.mainBody.role);
          }
          this.router.navigateByUrl('/home/my-committees');
	  this.loggedIn.next(true);
          this.loggingIn.next(false);
        },
        error: (error) => {
          if (error.error && error.error.message) {
            console.log(error.error.message);
          }
          this.router.navigateByUrl('/login');
          this.loggingIn.next(false);
        },
      });
  }

  // logout() {
  //   return this.http
  //     .post(
  //       `${BACKEND_URL}/logout`,
  //       {},
  //       {
  //         withCredentials: true,
  //       },
  //     )
  //     .pipe(tap(() => this.loggedIn.next(false)));
  // }

  registerWithToken(payload: any) {
    return this.httpClient.post<Response<any>>(BACKEND_URL + '/api/register-with-token', payload, {
      withCredentials: true,
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
