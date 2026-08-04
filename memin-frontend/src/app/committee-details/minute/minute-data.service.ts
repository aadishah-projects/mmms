import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, Signal, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { distinctUntilChanged, filter, map, switchMap } from 'rxjs';
import { BACKEND_URL } from '../../../global_constants';
import { MinuteDataDto } from '../../models/models';
import { Response } from '../../response/response';

@Injectable()
//data loading logic is not in the component because data needs to be shared with minute-edit component.
export class MinuteDataService {
  private minuteData = signal<MinuteDataDto>(new MinuteDataDto());
  private fullEditorMode = signal(false);

  private originalDataString: string = '';

  public hasMinuteDataLoaded = false;

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
  ) {
    // A minute route can be reused while its query parameters change. Use
    // switchMap so a slower response for the previous meeting can never
    // overwrite the minute currently being viewed.
    this.activatedRoute.queryParams
      .pipe(
        map((params) => params['meetingId']),
        filter((meetingId): meetingId is string => !!meetingId),
        distinctUntilChanged(),
        switchMap((meetingId) => {
          this.hasMinuteDataLoaded = false;
          this.fullEditorMode.set(false);

          const params = new HttpParams().set('meetingId', meetingId);
          return this.httpClient.get<Response<MinuteDataDto>>(
            BACKEND_URL + '/api/data-for-minute',
            { params, withCredentials: true },
          );
        }),
      )
      .subscribe({
        next: (response) => {
          this.minuteData.set(response.mainBody);
          this.fullEditorMode.set(
            !!response.mainBody.minuteContentHtml?.trim(),
          );
          this.hasMinuteDataLoaded = true;

          // originalDataString should not contain invitees because changing
          // invitee order is intentionally not persisted by this form.
          this.markSaved();
        },
        error: (response) => {
          this.hasMinuteDataLoaded = false;
          console.log(response);
        },
      });
  }

  getMinuteData(): Signal<MinuteDataDto> {
    return this.minuteData;
  }

  getFullEditorMode(): Signal<boolean> {
    return this.fullEditorMode;
  }

  setFullEditorMode(enabled: boolean): void {
    this.fullEditorMode.set(enabled);
  }

  setMinuteContentHtml(htmlContent: string | null): void {
    this.minuteData.update((data) => ({ ...data, minuteContentHtml: htmlContent }));
  }

  setStructuredFields(
    agendas: MinuteDataDto['agendas'],
    decisions: MinuteDataDto['decisions'],
  ): void {
    this.minuteData.update((data) => ({ ...data, agendas, decisions }));
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
