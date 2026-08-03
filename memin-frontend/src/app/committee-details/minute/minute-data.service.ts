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
            this.markSaved();
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

  setMinuteContentHtml(htmlContent: string | null): void {
    this.minuteData.update((data) => ({ ...data, minuteContentHtml: htmlContent }));
  }

  markSaved(): void {
    const data = this.minuteData();
    this.originalDataString = JSON.stringify({
      committeeName: data.committeeName,
      committeeDescription: data.committeeDescription,
      meetingHeldDate: data.meetingHeldDate,
      meetingHeldTime: data.meetingHeldTime,
      meetingHeldPlace: data.meetingHeldPlace,
      agendas: data.agendas,
      decisions: data.decisions,
      minuteContentHtml: data.minuteContentHtml,
    });
  }

  hasDataChanged(): boolean {
    const data = this.minuteData();
    const newData = {
      committeeName: data.committeeName,
      committeeDescription: data.committeeDescription,
      meetingHeldDate: data.meetingHeldDate,
      meetingHeldTime: data.meetingHeldTime,
      meetingHeldPlace: data.meetingHeldPlace,
      agendas: data.agendas,
      decisions: data.decisions,
      minuteContentHtml: data.minuteContentHtml,
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
