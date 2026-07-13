import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MeetingSummaryDto } from '../../../../models/models';
import { RouterLink, RouterOutlet } from '@angular/router';


@Component({
  selector: 'app-meeting-summary',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './meeting-summary.component.html',
  styleUrl: './meeting-summary.component.scss'
})
export class MeetingSummaryComponent {
  meetingSummary = input.required<MeetingSummaryDto>();

  //converting to a Date object to use Date pipe in the template
  meetingHeldTime = new Date();

  ngOnInit() {
      this.meetingHeldTime.setHours(this.meetingSummary().heldTime[0]);
      this.meetingHeldTime.setMinutes(this.meetingSummary().heldTime[1]);
  }


  toggleMenu = output<{event: Event, meetingId: number}>();

  toggleMenuEvent(eventObj: Event) {
    eventObj.stopPropagation();
    this.toggleMenu.emit({event: eventObj, meetingId: this.meetingSummary().id});
  }
}
