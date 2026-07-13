import { Component, Input, output } from '@angular/core';
import { CommitteeSummary } from './committee-summary.model';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Response } from '../../../response/response';
import { BACKEND_URL } from '../../../../global_constants';

@Component({
  selector: 'app-committee-summary',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './committee-summary.component.html',
  styleUrl: './committee-summary.component.scss',
})
export class CommitteeSummaryComponent {
  @Input({ required: true }) committeeSummary!: CommitteeSummary;
  toggleMenu = output<{event: Event, committeeId: number}>();
  toggleMenuEvent(eventObj: Event) {
    this.toggleMenu.emit({event: eventObj, committeeId: this.committeeSummary.id});
  }
}
