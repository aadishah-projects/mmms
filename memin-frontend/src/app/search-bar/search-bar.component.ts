import { HttpClient } from '@angular/common/http';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MemberSearchResult } from '../models/models';
import { BACKEND_URL } from '../../global_constants';
import { Response } from '../response/response';
import { FormGroup, FormControl, ReactiveFormsModule } from '@angular/forms';
import { FormControlName } from '@angular/forms';
import Fuse from 'fuse.js';
import { query } from '@angular/animations';
import { debounceTime, Subscription } from 'rxjs';
import { LoadMemberService } from '../load-member.service';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.scss',
})
export class SearchBarComponent implements OnInit, OnDestroy {
  loadMemberService = inject(LoadMemberService);
  allMembers!: MemberSearchResult[];
  searchInputFieldSubscription!: Subscription;
  searchResults!: MemberSearchResult[];
  httpClient = inject(HttpClient);
  router = inject(Router);
  formData = new FormGroup({
    searchInput: new FormControl(''),
  });

  ngOnInit(): void {
    this.searchInputFieldSubscription =
      this.formData.controls.searchInput.valueChanges
        .pipe(debounceTime(500)) // wait 0.5 seconds after user stops typing
        .subscribe((value) => {
          if (value === '') {
            this.searchResults = this.allMembers;
          } else {
            this.searchResults = this.fuzzySearch(
              this.allMembers,
              value as string,
            );
          }
        });

    this.loadMemberService.loadAllMembers().subscribe({
      next: (response) => {
        this.allMembers = response;
        this.searchResults = response;
      },
      error: (error) => {
        //TODO: show in popup
        console.log(error);
      },
    });
  }

  fuzzySearch(
    users: MemberSearchResult[],
    query: string,
  ): MemberSearchResult[] {
    const fuse = new Fuse(users, {
      keys: ['firstName', 'lastName'],
      threshold: 0.3, // lower = stricter match
    });
    return fuse.search(query).map((result) => result.item);
  }

  ngOnDestroy(): void {
    this.searchInputFieldSubscription.unsubscribe();
  }
}
