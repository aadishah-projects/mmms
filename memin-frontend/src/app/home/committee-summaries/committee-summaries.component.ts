import { HttpClient, HttpParams } from '@angular/common/http';
import { Component } from '@angular/core';
import { BACKEND_URL } from '../../../global_constants';
import { CommitteeSummary } from './committee-summary/committee-summary.model';
import { Response } from '../../response/response';
import { CommitteeSummaryComponent } from './committee-summary/committee-summary.component';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-committee-summaries',
  standalone: true,
  imports: [CommitteeSummaryComponent, RouterLink, FormsModule],
  templateUrl: './committee-summaries.component.html',
  styleUrl: './committee-summaries.component.scss',
})
export class CommitteeSummariesComponent {
  committeeSummaries!: CommitteeSummary[];
  sortBy: 'createdDate' | 'title' = 'createdDate';
  constructor(private httpClient: HttpClient, private router: Router) {}
  hasCommitteesLoaded = false;

  ngOnInit(): void {
    this.httpClient
      .get<Response<CommitteeSummary[]>>(
        BACKEND_URL + '/api/my-active-committees',
        {
          withCredentials: true,
        }
      )
      .subscribe({
        next: (response) => {
          this.committeeSummaries = response.mainBody;
          this.sortCommittees();
          this.hasCommitteesLoaded = true;
        },
        error: (response) => {
          console.log('error fetching committee');
        },
      });
  }

  onSortChange(event: Event): void {
    this.sortCommittees();
  }

  private sortCommittees(): void {
    if (!this.committeeSummaries) return;

    this.committeeSummaries.sort((a, b) => {
      if (this.sortBy === 'createdDate') {
        return (
          new Date(b.createdDate).getTime() - new Date(a.createdDate).getTime()
        );
      } else {
        return a.name.localeCompare(b.name);
      }
    });
  }


  showMenuOptions = false;
  dropdownTop = -1;
  dropdownRight = -1;
  committeeId = -1;  //set when the option display is clicked


  closeMenuOptionsIfOpen() {
    if(this.showMenuOptions) {
      this.showMenuOptions = false;
      //resetting these variables because onMenuOptionClick() uses them for comparison
      this.dropdownRight = -1;
      this.dropdownTop = -1;
    }
  }

  onMenuOptionClick(eventObj: {event: Event, committeeId: number}) {
    this.committeeId = eventObj.committeeId;
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
    this.router.navigate(['./committee-details/edit'], {
      queryParams: {
        committeeId: this.committeeId,
      },
    });
  }

  onNewMeetingClick(event: Event) {
    event?.stopPropagation();
    this.router.navigate(['./home/create-meeting'], {
      queryParams: {
        committeeId: this.committeeId,
      },
    })
  }

  onOverviewOptionClick(event: Event) {
    event.stopPropagation();
    this.router.navigate(['./committee-details/overview'], {
      queryParams: {
        committeeId: this.committeeId,
      },
    });
  }

  onSummaryOptionClick(event: Event) {
    event.stopPropagation();
    this.router.navigate(['./committee-details/summary'], {
      queryParams: {
	committeeId: this.committeeId,
      }
    })
  }

  onArchiveOptionClick(event: Event) {
    event.stopPropagation();

    const httpParams = new HttpParams().set(
      'committeeId',
      this.committeeId,
    );
    this.httpClient
      .patch<Response<Object>>(
        BACKEND_URL + '/api/toggle-committee-status',
        null,
        {
          withCredentials: true,
          params: httpParams,
        },
      )
      .subscribe({
        next: (response) => {
          console.log('TODO: handle this response');
          const currentUrl = this.router.url;

          // Navigate to a non-existent or temporary path for reloading data
          this.router
            .navigateByUrl('/non-existent', { skipLocationChange: true })
            .then(() => {

              // Navigate back to the original URL
              this.router.navigate([currentUrl]);
	      console.log("rerouting complete");
            });
        },
      });
  }
}
