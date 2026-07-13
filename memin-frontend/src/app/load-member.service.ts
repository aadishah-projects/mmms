import { Injectable, OnDestroy, OnInit } from '@angular/core';
import { MemberDetails, MemberSearchResult } from './models/models';
import { BACKEND_URL } from '../global_constants';
import { Response } from './response/response';
import { HttpClient } from '@angular/common/http';
import { map, Subscription } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class LoadMemberService implements OnDestroy {
  constructor(private httpClient: HttpClient) {}

  subscription!: Subscription;

  public loadAllMembers() {
    console.log("DEBUG: trying to load all members")
    return this.httpClient
      .get<Response<MemberDetails[]>>(BACKEND_URL + '/api/all-members', {
        withCredentials: true,
      })
      .pipe(
        map((response) => {
	  console.log(response.mainBody);
          return response.mainBody;
        }),
      );
  }

  public ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
