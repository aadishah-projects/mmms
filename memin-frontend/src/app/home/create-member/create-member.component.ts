import { Component } from '@angular/core';
import { MemberCreationDto, MemberFormData } from '../../models/models';
import { BACKEND_URL } from '../../../global_constants';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { MemberFormComponent } from '../../forms/member-form/member-form.component';
import { PopupService } from '../../popup/popup.service';

@Component({
  selector: 'app-create-member',
  standalone: true,
  imports: [MemberFormComponent],
  templateUrl: './create-member.component.html',
  styleUrl: './create-member.component.scss',
})
export class CreateMemberComponent {
  memberFormData: MemberFormData = {
    firstName: '',
    lastName: '',
    post: '',
    title: '',
    institution: '',
  };

  constructor(
    private httpClient: HttpClient,
    private router: Router,
    private popupService: PopupService,
  ) {}

  onFormSave(requestBody: MemberCreationDto) {
    console.log('onSubmitted called');

    this.httpClient
      .post<Response>(BACKEND_URL + '/api/member', requestBody, {
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          console.log(response);
          this.router.navigate(['/home/my-committees']);
          this.popupService.showPopup('Member Created!', 'Success', 2000);
        },
        error: (error) => {
          console.log(error.error.message);
          this.popupService.showPopup('Member Creation Failed!', 'Error', 2000);
        },
      });
  }
}
