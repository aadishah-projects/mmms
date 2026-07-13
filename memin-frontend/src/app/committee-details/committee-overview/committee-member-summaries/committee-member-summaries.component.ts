import { Component, input } from '@angular/core';
import { MemberOfCommitteeDto } from '../../../models/models';
import { CommitteeMemberSummaryComponent } from './committee-member-summary/committee-member-summary.component';

@Component({
  selector: 'app-member-summaries',
  standalone: true,
  imports: [CommitteeMemberSummaryComponent],
  templateUrl: './committee-member-summaries.component.html',
  styleUrl: './committee-member-summaries.component.scss'
})
export class CommitteeMemberSummariesComponent {
  membersOfCommittee = input.required<MemberOfCommitteeDto[]>();
}
