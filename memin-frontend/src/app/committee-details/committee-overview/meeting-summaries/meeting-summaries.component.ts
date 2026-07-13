import { Component, input } from '@angular/core';
import { MeetingSummaryComponent } from './meeting-summary/meeting-summary.component';
import { Router, RouterOutlet, RouterLink } from '@angular/router';
import { MeetingSummaryDto } from '../../../models/models';

@Component({
  selector: 'app-meeting-summaries',
  standalone: true,
  imports: [MeetingSummaryComponent ],
  templateUrl: './meeting-summaries.component.html',
  styleUrl: './meeting-summaries.component.scss',
})
export class MeetingSummariesComponent {
  meetingSummaries = input.required<MeetingSummaryDto[]>();

  constructor(private router: Router) {}

  showMenuOptions = false;

  dropdownTop = -1;
  dropdownRight = -1;
  meetingId = -1; //set when the option display is clicked

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

  closeMenuOptionsIfOpen() {
    if (this.showMenuOptions) {
      this.showMenuOptions = false;
      //resetting these variables because onMenuOptionClick() uses them for comparison
      this.dropdownRight = -1;
      this.dropdownTop = -1;
    }
  }
}
