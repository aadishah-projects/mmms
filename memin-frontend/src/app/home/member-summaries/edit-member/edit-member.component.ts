import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  MemberCreationDto,
  MemberDetailsDto,
  MemberFormData,
} from '../../../models/models';
import { BACKEND_URL } from '../../../../global_constants';
import { Response } from '../../../response/response';
import { MemberFormComponent } from '../../../forms/member-form/member-form.component';
import { PopupService } from '../../../popup/popup.service';

@Component({
  selector: 'app-edit-member',
  standalone: true,
  imports: [MemberFormComponent],
  templateUrl: './edit-member.component.html',
  styleUrl: './edit-member.component.scss',
})
export class EditMemberComponent implements OnInit {
  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private popupService: PopupService,
  ) {
    console.log('EditMemberComponent constructor called');
  }

  hasDataLoaded = false;
  httpParams!: HttpParams;

  memberFormData: MemberFormData = {
    firstName: '',
    lastName: '',
    title: '',
    post: '',
    institution: '',
  };

  ngOnInit() {
    console.log('trying to load data');
    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      this.httpParams = new HttpParams().set(
        'memberId',
        receivedParams['memberId'],
      );

      this.httpClient
        .get<Response<MemberDetailsDto>>(
          BACKEND_URL + '/api/member-details',
          {
            withCredentials: true,
            params: this.httpParams,
          },
        )
        .subscribe({
          next: (response) => {
            const mainBody = response.mainBody;
            this.memberFormData.firstName = mainBody.firstName;
            this.memberFormData.lastName = mainBody.lastName;
            this.memberFormData.title = mainBody.title;
            this.memberFormData.post = mainBody.post;
            this.memberFormData.institution = mainBody.institution;
            this.hasDataLoaded = true;
            console.log(response.mainBody);
          },
          error: (error) => {
            console.log('todo: show error', error);
          },
        });
    });
  }

  onFormSave(requestBody: MemberCreationDto) {
    console.log('submitting the form');

    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      this.httpParams = new HttpParams().set(
        'memberId',
        receivedParams['memberId'],
      );

      this.httpClient
        .patch<
          Response<Object>
        >(BACKEND_URL + '/api/member', requestBody, { withCredentials: true, params: this.httpParams })
        .subscribe({
          next: (response) => {
            console.log('TODO: handle this response');
            console.log(response);
	this.router.navigate(['/home/members-list'], {queryParamsHandling:'preserve'})
	    this.popupService.showPopup("Member Edited!", "Success", 2000);
          },
          error: (error) => {
	    this.popupService.showPopup("Member Edit Failed!", "Error", 2000);
          },
        });
    });
  }
}
