import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BACKEND_URL } from '../../../global_constants';
import { MinuteDataDto } from '../../models/models';
import { Response } from '../../response/response';

@Injectable()
//data loading logic is not in the component because data needs to be shared with minute-edit component.
export class MinuteDataService {
  private minuteData = signal<MinuteDataDto>(new MinuteDataDto());

  private originalDataString: string = '';

  public hasMinuteDataLoaded = false;

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
  ) {
    this.activatedRoute.queryParams.subscribe((receivedParams) => {
      let params = new HttpParams().set(
        'meetingId',
        receivedParams['meetingId'],
      );
      this.httpClient
        .get<Response<MinuteDataDto>>(BACKEND_URL + '/api/data-for-minute', {
          params: params,
          withCredentials: true,
        })
        .subscribe({
          next: (response) => {
            this.minuteData.set(response.mainBody);
	    this.hasMinuteDataLoaded = true;

	    //originalDataString should no contain invitees because when invitee order changs, 'Save' button should not appear
            const originalData = {
              committeeName: this.minuteData().committeeName,
              committeeDescription: this.minuteData().committeeDescription,
              meetingHeldDate: this.minuteData().meetingHeldDate,
              meetingHeldTime: this.minuteData().meetingHeldTime,
              meetingHeldPlace: this.minuteData().meetingHeldPlace,
              agendas: this.minuteData().agendas,
              decisions: this.minuteData().decisions,
            };
            this.originalDataString = JSON.stringify(originalData);
            console.log(response.mainBody);
          },
          error: (response) => {
            console.log(response);
          },
        });
    });
  }

  getMinuteData(): Signal<MinuteDataDto> {
    return this.minuteData;
  }

  hasDataChanged(): boolean {
    const newData = {
      committeeName: this.minuteData().committeeName,
      committeeDescription: this.minuteData().committeeDescription,
      meetingHeldDate: this.minuteData().meetingHeldDate,
      meetingHeldTime: this.minuteData().meetingHeldTime,
      meetingHeldPlace: this.minuteData().meetingHeldPlace,
      agendas: this.minuteData().agendas,
      decisions: this.minuteData().decisions,
    };

    if (
      this.originalDataString.length != 0 &&
      JSON.stringify(newData) !== this.originalDataString
    ) {
      return true;
    }
    return false;
  }
}
