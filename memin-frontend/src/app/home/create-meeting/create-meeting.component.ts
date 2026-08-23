import { Component } from "@angular/core";
import { BACKEND_URL } from "../../../global_constants";
import { AiStructuredMinuteDto, MeetingCreationDto, MeetingFormData, MeetingSummaryDto } from "../../models/models";
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

  private aiPromptForNewMinute: string | null = null;

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
    chairman: null,
  }


  onFormSave(requestBody: MeetingCreationDto) {

    console.log(requestBody);
    const aiPrompt = this.aiPromptForNewMinute;
    this.aiPromptForNewMinute = null;

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
          const meetingId = response.mainBody.id;
          if (aiPrompt !== null) {
            this.httpClient
              .post<Response<AiStructuredMinuteDto>>(
                `${BACKEND_URL}/api/meetings/${meetingId}/ai-minute`,
                { roughPrompt: aiPrompt },
                { withCredentials: true },
              )
              .subscribe({
                next: () => {
                  this.navigateToMinute(requestBody.committeeId, meetingId);
                  this.popupService.showPopup('Meeting created and agenda/decision entries refined with AI.', 'Success', 3000);
                },
                error: (error) => {
                  this.navigateToMinute(requestBody.committeeId, meetingId);
                  const message = error?.error?.message || 'AI refinement failed. You can retry from the minute editor.';
                  this.popupService.showPopup(message, 'Error', 4000);
                },
              });
          } else {
            this.navigateToMinute(requestBody.committeeId, meetingId);
            this.popupService.showPopup("Meeting Created!", "Success", 2000);
          }
        },

        error: (error) => {
          console.log('TODO: show in popup' + error.error.message);
	  this.popupService.showPopup("Meeting Creation Failed!", "Error", 2000);
        },
      });
  }

  onDraftWithAi(prompt: string) {
    this.aiPromptForNewMinute = prompt;
  }

  private navigateToMinute(committeeId: number, meetingId: number) {
    this.router.navigate(['/committee-details/overview/minute'], {
      queryParams: { committeeId, meetingId },
    });
  }
  
} 
