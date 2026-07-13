import { Component, input } from '@angular/core';
import { MemberOfCommitteeDto } from '../../../../models/models';

@Component({
  selector: 'app-member-summary',
  standalone: true,
  imports: [],
  templateUrl: './committee-member-summary.component.html',
  styleUrl: './committee-member-summary.component.scss'
})
export class CommitteeMemberSummaryComponent {
  memberOfCommittee = input.required<MemberOfCommitteeDto>();
}

