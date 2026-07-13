import { Component, OnInit } from '@angular/core';
import { MemberDetailsDto, MemberSearchResult } from '../../models/models';
import { Response } from '../../response/response';
import { HttpClient } from '@angular/common/http';
import { BACKEND_URL } from '../../../global_constants';
import { MemberSummaryComponent } from './member-summary/member-summary.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-member-summaries',
  standalone: true,
  imports: [MemberSummaryComponent],
  templateUrl: './member-summaries.component.html',
  styleUrl: './member-summaries.component.scss',
})
export class MemberSummariesComponent implements OnInit {
  memberDetails!: MemberDetailsDto[];
  memberDetailsLoaded = false;

  constructor(
    private httpClient: HttpClient,
    private router: Router,
  ) {
  }

  showMenuOptions = false;
  dropdownTop = -1;
  dropdownRight = -1;
  memberId = -1; //set when the option display is clicked

  onMenuOptionClick(eventObj: { event: Event; memberId: number }) {
    this.memberId = eventObj.memberId;
    eventObj.event.stopPropagation();
    const input = eventObj.event.target as HTMLElement;
    const rect = input.getBoundingClientRect();
    const newDropdownTop = rect.bottom + 10;
    // so both rect.right and left.right gives the distance from left edge of the view port, but right property of css expects distance from right edge of the viewport
    const newDropdownRight = window.innerWidth - rect.right - 10;
    if (
      this.dropdownTop == newDropdownTop &&
      this.dropdownRight == newDropdownRight
    ) {
      this.showMenuOptions = false;
      this.dropdownRight = -1;
      this.dropdownTop = -1;
      return;
    }
    this.showMenuOptions = true;
    this.dropdownRight = newDropdownRight;
    this.dropdownTop = newDropdownTop;
  }

  onEditOptionClick(event: Event) {
    event.stopPropagation();
    this.router.navigate(['/home/members-list/edit'], {
      queryParams: {
        memberId: this.memberId,
      },
    });
  }

  closeMenuOptionsIfOpen() {
    if (this.showMenuOptions) {
      this.showMenuOptions = false;
      //resetting these variables because onMenuOptionClick() uses them for comparison
      this.dropdownRight = -1;
      this.dropdownTop = -1;
    }
  }

  ngOnInit() {
    this.httpClient
      .get<
        Response<MemberDetailsDto[]>
      >(BACKEND_URL + '/api/all-members', { withCredentials: true })
      .subscribe({
        next: (response) => {
          this.memberDetails = response.mainBody;
          this.memberDetailsLoaded = true;
          console.log('data has been loaded: member summaries');
        },
        error: (error) => {
          console.log('TODO: show response properly', error);
        },
      });
  }
}
