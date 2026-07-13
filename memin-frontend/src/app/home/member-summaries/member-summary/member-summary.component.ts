import { Component, input, output } from '@angular/core';
import { MemberDetailsDto } from '../../../models/models';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-member-summary',
  standalone: true,
  imports: [],
  templateUrl: './member-summary.component.html',
  styleUrl: './member-summary.component.scss'
})
export class MemberSummaryComponent {
  memberDetail = input.required<MemberDetailsDto>();
  showMenuOptions = false;
  constructor (private httpClient: HttpClient, private router: Router) {
    
  }

  toggleMenu = output<{event: Event, memberId: number}>();

  toggleMenuEvent(eventObj: Event) {
    this.toggleMenu.emit({event: eventObj, memberId: this.memberDetail().memberId});
  }
}
