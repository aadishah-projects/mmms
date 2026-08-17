import { Component, input, output } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MeetingSummaryComponent } from './meeting-summary/meeting-summary.component';
import { Router, RouterOutlet, RouterLink } from '@angular/router';
import { MeetingSummaryDto } from '../../../models/models';
import { BACKEND_URL } from '../../../../global_constants';
import { Response } from '../../../response/response';
import { PopupService } from '../../../popup/popup.service';

@Component({
  selector: 'app-meeting-summaries',
  standalone: true,
  imports: [MeetingSummaryComponent ],
  templateUrl: './meeting-summaries.component.html',
  styleUrl: './meeting-summaries.component.scss',
})
export class MeetingSummariesComponent {
  meetingSummaries = input.required<MeetingSummaryDto[]>();
  meetingDeleted = output<number>();

  constructor(
    private router: Router,
    private httpClient: HttpClient,
    private popupService: PopupService,
  ) {}

  showMenuOptions = false;

  dropdownTop = -1;
  dropdownRight = -1;
  meetingId = -1; //set when the option display is clicked
  isDeletingMeeting = false;

  onMenuOptionClick(eventObj: { event: Event; meetingId: number }) {
    this.meetingId = eventObj.meetingId;
    const input = eventObj.event.currentTarget as HTMLElement;
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
    this.router.navigate(['/committee-details/overview/meeting/edit'], {
      queryParams: {
        meetingId: this.meetingId,
      },
      queryParamsHandling: 'merge',
    });
  }

  onMinuteOptionClick(event: Event) {
    event.stopPropagation();
    this.router.navigate(['./committee-details/overview/minute'], {
      queryParams: {
	meetingId: this.meetingId,
      },
      queryParamsHandling: 'merge',
    });
  }

  onDeleteOptionClick(event: Event) {
    event.stopPropagation();

    const meeting = this.meetingSummaries().find(
      (meetingSummary) => meetingSummary.id === this.meetingId,
    );
    if (!meeting || this.isDeletingMeeting) {
      return;
    }

    const confirmed = window.confirm(
      `Delete “${meeting.title}”? This will permanently remove the meeting, its minute, agendas, and decisions.`,
    );
    if (!confirmed) {
      this.closeMenuOptionsIfOpen();
      return;
    }

    this.isDeletingMeeting = true;
    this.httpClient
      .delete<Response<unknown>>(`${BACKEND_URL}/api/meeting/${meeting.id}`, {
        withCredentials: true,
      })
      .subscribe({
        next: () => {
          this.isDeletingMeeting = false;
          this.closeMenuOptionsIfOpen();
          this.meetingDeleted.emit(meeting.id);
          this.popupService.showPopup('Meeting deleted successfully.', 'Success', 2500);
        },
        error: (error) => {
          this.isDeletingMeeting = false;
          const message = error?.error?.message || 'Meeting could not be deleted.';
          this.popupService.showPopup(message, 'Error', 3500);
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
}
