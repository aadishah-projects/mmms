import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BACKEND_URL } from '../../../../../global_constants';
import {
  MeetingCreationDto,
  MeetingDetailsForEdit,
  MeetingFormData,
} from '../../../../models/models';
import { Response } from '../../../../response/response';
import { MeetingForm } from '../../../../forms/meeting-form/meeting-form.component';
import { PopupService } from '../../../../popup/popup.service';

@Component({
  selector: 'app-edit-meeting',
  standalone: true,
  imports: [MeetingForm],
  templateUrl: './edit-meeting.component.html',
  styleUrl: './edit-meeting.component.scss',
})
export class EditMeetingComponent implements OnInit {
  meetingFormData!: MeetingFormData;
  hasMemberFormDataLoaded = false;
  meetingId!: number;

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private popupService: PopupService, 
  ) {}

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((params) => {
      const meetingId = params['meetingId'];
      this.meetingId = meetingId; //to use in the onFormSave() method
      const committeeId = params['committeeId'];
      const httpParams = new HttpParams()
        .set('meetingId', meetingId)
        .set('committeeId', committeeId);
      this.httpClient
        .get<
          Response<MeetingDetailsForEdit>
        >(BACKEND_URL + '/api/meeting-details-for-edit', { withCredentials: true, params: httpParams })
        .subscribe({
          next: (response) => {
            const meetingDetails = response.mainBody;
            console.log(meetingDetails);
            this.meetingFormData = {
              title: meetingDetails.title,
              heldDate: meetingDetails.heldDate,
              heldTime: meetingDetails.heldTime,
              heldPlace: meetingDetails.heldPlace,
              committeeName: meetingDetails.committeeName,
              decisions: meetingDetails.decisions,
              agendas: meetingDetails.agendas,
              selectedInvitees: meetingDetails.selectedInvitees,
              possibleInvitees: meetingDetails.possibleInvitees,
            };
            this.hasMemberFormDataLoaded = true;
          },
          error: (error) => {
            console.log('TODO: show error message');
          },
        });
    });
  }

  onFormSave(responseBody: MeetingCreationDto) {
    this.httpClient
      .patch<
        Response<Object>
      >(BACKEND_URL + '/api/meeting', responseBody, { withCredentials: true, params: new HttpParams().set('meetingId', this.meetingId) })
      .subscribe({
	next: (response) => {
	  console.log("TODO: handle the response correctly", response);
	  this.router.navigate(["./committee-details/overview/minute"], {queryParamsHandling: "preserve"});
	  this.popupService.showPopup("Meeting Edited!", "Success", 2000);
	},
	error: (error) => {
	  this.popupService.showPopup("Meeting Edit Failed!", "Error", 2000);
	}
      });
  }
}
