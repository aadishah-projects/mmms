import { HttpClient, HttpParams } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BACKEND_URL } from '../../../global_constants';
import { CommitteeExtendedSummary, MemberOfCommitteeDto } from '../../models/models';
import { Response } from '../../response/response';
import { CommitteeMemberSummariesComponent } from '../committee-overview/committee-member-summaries/committee-member-summaries.component';
import { getNepaliDate, toNepaliDigits } from '../../../utils/custom-functions';

@Component({
  selector: 'app-committee-extended-summary',
  standalone: true,
  imports: [CommitteeMemberSummariesComponent],
  templateUrl: './committee-extended-summary.component.html',
  styleUrl: './committee-extended-summary.component.scss',
})
export class CommitteeExtendedSummaryComponent {

  summaryDataLoaded = false;
  committeeExtendedSummary!: CommitteeExtendedSummary;

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
  ) {
    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      const params = new HttpParams().set(
        'committeeId',
        receivedParams['committeeId'],
      );

      this.httpClient
        .get<Response<CommitteeExtendedSummary>>(BACKEND_URL + '/api/committee-summary', {
          withCredentials: true,
          params: params,
        })
        .subscribe({
          next: (response) => {
	    this.committeeExtendedSummary = response.mainBody;
	    console.log(response.mainBody);
	    this.summaryDataLoaded = true;

	    if(this.committeeExtendedSummary.language == 'NEPALI') {
	      this.committeeExtendedSummary.meetings.forEach(meeting=> {
		meeting.meetingHeldDate = getNepaliDate(meeting.meetingHeldDate) as string;
	      })
	    }
	  },
        });
    });
  }
}
