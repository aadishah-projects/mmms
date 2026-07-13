import { Component, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  DateAndMeetingIdsDto,
  MeetingSummaryDto,
} from '../../../models/models';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss',
})
export class CalendarComponent {
  meetingDates: string[] = [];
  meetings = input.required<DateAndMeetingIdsDto[]>();

  currentYear = new Date().getFullYear();
  selectedYear = this.currentYear;
  months = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
  years: Set<number> = new Set();

  // Popup state
  showPopup = false;
  popupMeetings: MeetingSummaryDto[] = [];
  popupDate: string = '';

  constructor(private router: Router) {}

  ngOnInit() {
    //if at least one meeting date is present
    if (
      this.meetings() != null &&
      this.meetings() != undefined &&
      this.meetings().length > 0
    ) {
      console.log(this.meetings());

      this.meetingDates = this.meetings().map(
        (dateAndMeetingIds) => dateAndMeetingIds.meetingDate,
      );

      console.log(this.meetingDates);
      this.years = new Set(
        this.meetingDates.map((date) => new Date(date).getFullYear()),
      );

      this.years.add(this.currentYear);
    } else {
      this.years.add(this.currentYear);
    }
  }

  getMonthName(month: number): string {
    return new Date(this.selectedYear, month).toLocaleString('default', {
      month: 'long',
    });
  }

  getDaysInMonth(year: number, month: number): Date[] {
    const days: Date[] = [];
    const date = new Date(year, month, 1);
    while (date.getMonth() === month) {
      days.push(new Date(date));
      date.setDate(date.getDate() + 1);
    }
    return days;
  }

  isMeeting(date: Date): boolean {
    if (
      this.meetingDates == null ||
      this.meetingDates == undefined ||
      this.meetingDates.length <= 0
    ) {
      return false;
    }
    return this.meetingDates.some(
      (d) => new Date(d).toDateString() === date.toDateString(),
    );
  }

  isToday(date: Date): boolean {
    return date.toDateString() === new Date().toDateString();
  }

  onMeetingDateClick(date: Date): void {
    if (!this.isMeeting(date)) return;

    const dateString = date.toDateString();
    const matchingDate = this.meetings().find(
      (m) => new Date(m.meetingDate).toDateString() === dateString,
    );

    if (matchingDate && matchingDate.meetings.length > 0) {
      this.popupMeetings = matchingDate.meetings;
      this.popupDate = date.toLocaleDateString('default', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
      this.showPopup = true;
    }
  }

  closePopup(): void {
    this.showPopup = false;
    this.popupMeetings = [];
    this.popupDate = '';
  }

  goToMeeting(meetingId: number): void {
    this.closePopup();
    this.router.navigate(['/committee-details/overview/minute'], {
      queryParams: { meetingId },
    });
  }

  formatTime(heldTime: number[]): string {
    if (!heldTime || heldTime.length < 2) return '';
    const hours = heldTime[0].toString().padStart(2, '0');
    const minutes = heldTime[1].toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }
}
