import { Component } from "@angular/core";
import { BACKEND_URL } from "../../../global_constants";
import { MeetingCreationDto, MeetingFormData, MeetingSummaryDto } from "../../models/models";
import { HttpClient } from "@angular/common/http";
import { Router } from "@angular/router";
import { Response } from "../../response/response"
import { MeetingForm } from "../../forms/meeting-form/meeting-form.component";
import { PopupService } from "../../popup/popup.service";

@Component({
  selector: 'app-create-meeting',
  standalone: true,
  imports: [MeetingForm],
  templateUrl: './create-meeting.component.html',
  styleUrl: './create-meeting.component.scss',
})
export class CreateMeetingComponent  {

  constructor(private httpClient: HttpClient,private router: Router, private popupService: PopupService) {
    
  }


  meetingFormData: MeetingFormData = {
    title: '',
    committeeName:'',
    heldDate: '',
    heldTime: [],
    heldPlace: '',
    decisions: [],
    agendas: [],
    possibleInvitees: [],
    selectedInvitees: [],
  }


  onFormSave(requestBody: MeetingCreationDto) {

    console.log(requestBody);

    this.httpClient
      .post<Response<MeetingSummaryDto>>(
        BACKEND_URL + '/api/meeting',
        requestBody,
        {
          withCredentials: true,
        },
      )
      .subscribe({
        next: (response) => {
          console.log(response.message);
          this.router.navigate(['/home/my-committees']);
	  this.popupService.showPopup("Meeting Created!", "Success", 2000);
        },

        error: (error) => {
          console.log('TODO: show in popup' + error.error.message);
	  this.popupService.showPopup("Meeting Creation Failed!", "Error", 2000);
        },
      });
  }
  
} 
