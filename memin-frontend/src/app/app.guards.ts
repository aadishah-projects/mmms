import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Route, UrlSegment, Router, UrlTree, ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { BACKEND_URL } from '../global_constants';
import { Response } from './response/response';
import { catchError, map, Observable, of } from 'rxjs';
import { AuthService } from './service/auth.service';

export function isAuthenticated(
  route: Route,
  segment: UrlSegment[],
): Observable<Boolean | UrlTree> {
  const httpClient = inject(HttpClient);
  const router = inject(Router);

  return httpClient
    .get<Response<Object>>(BACKEND_URL + '/isAuthenticated', {
      withCredentials: true,
    })
    .pipe(
      map((response) => {
        if (response?.message == 'true') {
          return true;
        } else {
          return router.parseUrl('/login');
        }
      }),
      //when login fails, server returns HTTP error, and this will be executed
      catchError((error: HttpErrorResponse) => {
        return of(router.parseUrl('/login'));
      }),
    );
}


export function isNotAuthenticated(
  route: Route,
  segment: UrlSegment[],
): Observable<Boolean | UrlTree> {
  const httpClient = inject(HttpClient);
  const router = inject(Router);

  return httpClient
    .get<Response<Object>>(BACKEND_URL + '/isAuthenticated', {
      withCredentials: true,
    })
    // .subscribe({
    //   next: (response) => {
    // 	return of(router.parseUrl('/home/my-committees'));
    //   },
    //   error: (error) => {
    // 	return of(true);
    //   },
    // })
    .pipe(
      map((response) => {
	console.log(response?.message);
        if (response?.message == 'true') {
          return router.parseUrl('/home/my-committees');
        } else {
	  return true;
	}
      }),
      catchError((error: HttpErrorResponse) => {
	return of(true);
      }),
    );
}

export const committeeRouteGuard: CanActivateFn= (route:ActivatedRouteSnapshot, state: RouterStateSnapshot)=> {
  const router = inject(Router);
  const committeeId = route.queryParams['committeeId'];
  if (committeeId) {
    return true;
  }
  return router.parseUrl('/error'); //redirect
};


export const memberRouteGuard: CanActivateFn= (route:ActivatedRouteSnapshot, state: RouterStateSnapshot)=> {
  const router = inject(Router);
  const memberId = route.queryParams['memberId'];
  if (memberId) {
    console.log("opening edit page");
    return true;
  }
  return router.parseUrl('/error'); //redirect
};


export const meetingRouteGuard: CanActivateFn= (route:ActivatedRouteSnapshot, state: RouterStateSnapshot)=> {
  const router = inject(Router);
  const meetingId = route.queryParams['meetingId'];
  if (meetingId) {
    console.log("opening edit page");
    return true;
  }
  return router.parseUrl('/error'); //redirect
};


//ensures route has a meetingId and committeeId
export const minuteGuard: CanActivateFn= (route:ActivatedRouteSnapshot, state: RouterStateSnapshot)=> {
  const router = inject(Router);
  const committeeId = route.queryParams['committeeId'];
  const meetingId = route.queryParams['meetingId'];
  if (committeeId && meetingId) {
    return true;
  }
  return router.parseUrl('/error'); //redirect
};

export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    return authService.userRole$.pipe(
      map(role => {
        if (role && allowedRoles.includes(role)) {
          return true;
        }
        return router.parseUrl('/home/my-committees');
      })
    );
  };
};

export const secretaryOrDeptHeadGuard = roleGuard(['SECRETARY', 'DEPARTMENT_HEAD']);
export const deptHeadGuard = roleGuard(['DEPARTMENT_HEAD']);
