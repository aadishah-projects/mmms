import { HttpParams, HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Router, ActivatedRoute, RouterOutlet } from '@angular/router';
import { BACKEND_URL } from '../../../global_constants';
import { CommitteeOverviewDto, MeetingSummaryDto, MemberOfCommitteeDto } from '../../models/models';
import { Response } from '../../response/response';
import { CalendarComponent } from './calendar/calendar.component';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MeetingSummariesComponent } from './meeting-summaries/meeting-summaries.component';
import { CommitteeMemberSummariesComponent } from './committee-member-summaries/committee-member-summaries.component';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-committee-overview',
  standalone: true,
  imports: [RouterOutlet, CommitteeMemberSummariesComponent, CalendarComponent, DatePipe, FormsModule, MeetingSummariesComponent],
  templateUrl: './committee-overview.component.html',
  styleUrl: './committee-overview.component.scss',
})
export class CommitteeOverviewComponent {

  //used to display template
  hasCommitteeMembersLoaded = false;
  hasOverviewDataLoaded = false;
  hasMeetingDataLoaded = false;


  //variables to store the loaded data
  membersOfCommittee!: MemberOfCommitteeDto[];
  committeeOverview!: CommitteeOverviewDto;
  meetingSummaries!: MeetingSummaryDto[];

  //committee currently being viewed (used for secretary assignment and creating a meeting)
  committeeId!: number;

  //access control + secretary assignment UI state
  isDepartmentHead = false;
  isSecretary = false;
  isAssigningSecretary = false;
  selectedSecretaryId: number | null = null;

  // Department heads and committee secretaries may create meetings (mirrors hasWriteAccessGuard).
  get canCreateMeeting(): boolean {
    return this.isDepartmentHead || this.isSecretary;
  }

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
  ) {

    this.authService.userRole$.subscribe((role) => {
      this.isDepartmentHead = role === 'DEPARTMENT_HEAD';
    });

    this.authService.isSecretary$.subscribe((isSecretary) => {
      this.isSecretary = isSecretary;
    });

    //load committee members
    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      this.committeeId = Number(receivedParams['committeeId']);
      const params = new HttpParams().set(
        'committeeId',
        receivedParams['committeeId'],
      );
      this.httpClient
        .get<
          Response<MemberOfCommitteeDto[]>
        >(BACKEND_URL + '/api/all-members-of-committee', {params: params,  withCredentials: true })
        .subscribe({
          next: (response) => {
            this.membersOfCommittee = response.mainBody;
            this.hasCommitteeMembersLoaded = true;
          },
          error: (response) => {
            //TODO: handle error with popup message and redirect to error page
            console.log(response);
          },
        });
    });


    //load committee overview data
    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      this.loadCommitteeOverview(receivedParams['committeeId']);
    });

    //TODO: combine both of these requests to a single one from the backend
    //load meetings data for the committee
        this.activatedRoute.queryParams.subscribe((receivedParams) => {
      const params = new HttpParams().set('committeeId', receivedParams['committeeId']);
      this.httpClient
        .get<
          Response<MeetingSummaryDto[]>
        >(BACKEND_URL + '/api/meetings-of-committee', { params: params, withCredentials: true })
        .subscribe({
          next: (response) => {
            this.meetingSummaries = response.mainBody;
            this.hasMeetingDataLoaded = true;
          },
          error: (response) => {
            console.log(response);
            //TODO: handle error with popup message
          },
        });
    });
  }

  loadCommitteeOverview(committeeId: string | number) {
    const params = new HttpParams().set('committeeId', committeeId);
    this.httpClient
      .get<
        Response<CommitteeOverviewDto>
      >(BACKEND_URL + '/api/committee-overview', { params: params, withCredentials: true })
      .subscribe({
        next: (response) => {
          this.committeeOverview = response.mainBody;
          this.selectedSecretaryId = this.committeeOverview.secretaryId;
          this.hasOverviewDataLoaded = true;
        },
        error: (response) => {
          console.log(response);
          //TODO: handle error with popup message
        },
      });
  }

  // --- Secretary assignment (DEPARTMENT_HEAD only) ---

  startAssigningSecretary() {
    this.selectedSecretaryId = this.committeeOverview.secretaryId;
    this.isAssigningSecretary = true;
  }

  cancelAssigningSecretary() {
    this.isAssigningSecretary = false;
  }

  saveSecretary() {
    let params = new HttpParams();
    // Omitting memberId tells the backend to clear the secretary.
    if (this.selectedSecretaryId != null) {
      params = params.set('memberId', this.selectedSecretaryId);
    }
    this.httpClient
      .patch<Response<Object>>(
        `${BACKEND_URL}/api/committee/${this.committeeId}/secretary`,
        null,
        { params: params, withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.isAssigningSecretary = false;
          this.loadCommitteeOverview(this.committeeId);
        },
        error: (response) => {
          console.log(response);
          //TODO: handle error with popup message
        },
      });
  }

  // --- Create meeting scoped to this committee ---

  createMeetingForThisCommittee() {
    this.router.navigate(['/home/create-meeting'], {
      queryParams: { committeeId: this.committeeId },
    });
  }

  editMinuteTemplate() {
    this.router.navigate(['/committee-details/template'], {
      queryParams: { committeeId: this.committeeId },
    });
  }
}
