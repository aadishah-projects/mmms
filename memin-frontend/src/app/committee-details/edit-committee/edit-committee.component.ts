import { Component, inject, OnInit } from '@angular/core';
import { CommitteeFormComponent } from '../../forms/committee-form/committee-form.component';
import { MemberSelectionService } from '../../home/create-committee/select-member-for-committee/select-member-for-committee.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Response } from '../../response/response';
import { BACKEND_URL } from '../../../global_constants';
import {
  CommitteeCreationDto,
  CommitteeDetailsForEditDto,
  CommitteeFormData,
} from '../../models/models';
import { LoadMemberService } from '../../load-member.service';
import { PopupService } from '../../popup/popup.service';

@Component({
  selector: 'app-edit-committee',
  standalone: true,
  imports: [CommitteeFormComponent],
  templateUrl: './edit-committee.component.html',
  styleUrl: './edit-committee.component.scss',
  providers: [MemberSelectionService],
})
export class EditCommitteeComponent implements OnInit {
  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private popupService: PopupService,
  ) {}
  httpParams!: HttpParams;

  hasDataLoaded = false;

  committeeFormData: CommitteeFormData = {
    name: '',
    description: '',
    coordinator: {
      memberId: 0,
      firstName: '',
      lastName: '',
    },
    status: 'ACTIVE',
    maxNoOfMeetings: 0,
    minuteLanguage: null,
    selectedMembersWithRoles: [],
    unselectedMembers: [],
  };

  ngOnInit() {
    this.loadAllData();
  }

  loadAllData() {
    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      this.httpParams = new HttpParams().set(
        'committeeId',
        receivedParams['committeeId'],
      );
      this.httpClient
        .get<Response<CommitteeDetailsForEditDto>>(
          BACKEND_URL + '/api/committee-details-for-edit-page',
          {
            withCredentials: true,
            params: this.httpParams,
          },
        )
        .subscribe({
          next: (response) => {
            const mainBody = response.mainBody;
            this.committeeFormData.name = mainBody.name;
            this.committeeFormData.description = mainBody.description;
            this.committeeFormData.coordinator = mainBody.coordinator;
            this.committeeFormData.status = mainBody.status;
            this.committeeFormData.maxNoOfMeetings =
              mainBody.maxNoOfMeetings as number;
            this.committeeFormData.minuteLanguage = mainBody.minuteLanguage;
            this.committeeFormData.selectedMembersWithRoles =
              mainBody.membersWithRoles;
            this.loadAllMembers();
            console.log('CommitteeFormData');
            console.log(this.committeeFormData);
          },
          error: (error) => {
            console.log('TODO: show error', error);
          },
        });
    });
  }

  loadMemberService = inject(LoadMemberService);
  loadAllMembers() {
    this.loadMemberService.loadAllMembers().subscribe({
      next: (loadedMembers) => {
        this.committeeFormData.unselectedMembers = loadedMembers;

        //dataLoaded should be here, not after call for this.loadAllMembers() because this is an async operation, and if template is loaded with unselected members, there will be timing issues

        this.hasDataLoaded = true;
      },
      error: (response) => {
        console.log('TODO: handle error' + response);
      },
    });
  }

  onFormSave(committeeCreationDto: CommitteeCreationDto) {
    console.log('saving form');
    this.httpClient
      .patch<
        Response<Object>
      >(BACKEND_URL + '/api/committee', committeeCreationDto, { withCredentials: true, params: this.httpParams })
      .subscribe({
        next: (response) => {
          console.log('TODO: show response properly');
          console.log(response.mainBody);
          this.router.navigate(['./committee-details/overview'], {
            queryParamsHandling: 'preserve',
          });
          this.popupService.showPopup('Committee Edited!', 'Success', 2000);
        },
        error: (error) => {
          this.popupService.showPopup('Committee Edit Failed!', 'Error', 2000);
        },
      });
  }
}
