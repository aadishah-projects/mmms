import {
  Component,
  ElementRef,
  inject,
  input,
  ViewChild,
  viewChild,
} from '@angular/core';
import { MinuteEnglish1Component } from './minute-english-1/minute-english-1.component';
import { MinuteNepali1Component } from './minute-nepali-1/minute-nepali-1.component';
import { MinuteDataService } from '../minute-data.service';
import { BACKEND_URL } from '../../../../global_constants';
import { HttpClient } from '@angular/common/http';
import {
  CdkDragDrop,
  DragDropModule,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import { ActivatedRoute } from '@angular/router';
import { CommitteeMembershipDto } from '../../../models/models';
import { Response } from '../../../response/response';
import { PopupService } from '../../../popup/popup.service';

@Component({
  selector: 'app-minute-view',
  standalone: true,
  imports: [
    MinuteNepali1Component,
    MinuteEnglish1Component,
    DragDropModule,
  ],
  templateUrl: './minute-view.component.html',
  styleUrl: './minute-view.component.scss',
})
export class MinuteViewComponent {
  showMinuteOptions = false;
  isEditMode = false;
  participantOrderSaving = false;
  private participantOrderSnapshot: CommitteeMembershipDto[] = [];

  ///////////////////////////////////////////
  // for invitee order change dialog
  @ViewChild('participant_order_dialog')
  dialogElementRef!: ElementRef<HTMLDialogElement>;

  onDialogClick(event: MouseEvent) {
    const dlg = this.dialogElementRef.nativeElement;
    if (event.target === dlg && dlg.open) {
      const dialog = this.dialogElementRef?.nativeElement;
      dialog.close();
    }
  }

  ////////////////////////////////////////////
  // for menu options

  toggleEditMode() {
    this.isEditMode = !this.isEditMode;
    this.showMinuteOptions = false;
  }

  printPage() {
    this.showMinuteOptions = false;
    // Small delay to allow Angular to update the DOM before printing
    setTimeout(() => {
      window.print();
    }, 1);
  }

  ////////////////////////////////////////////////////////////
  //for participant order change dialog

  drop(event: CdkDragDrop<CommitteeMembershipDto[]>) {
    moveItemInArray(
      this.minuteData().participants,
      event.previousIndex,
      event.currentIndex,
    );
  }

  diag = viewChild<ElementRef<HTMLDialogElement>>('participant_order_dialog');
  showChangeParticipantOrderDialog() {
    this.showMinuteOptions = false;
    this.participantOrderSnapshot = this.minuteData().participants.map((participant) => ({ ...participant }));
    this.diag()!.nativeElement.showModal();
  }

  cancelParticipantOrder(): void {
    this.injectedMinuteDataService.setParticipants(this.participantOrderSnapshot);
    this.diag()?.nativeElement.close();
  }

  saveParticipantOrder(): void {
    if (this.participantOrderSaving) return;
    const meetingId = this.routeMeetingId();
    if (!meetingId) return;
    this.participantOrderSaving = true;
    this.httpClient
      .patch<Response<{ participantIds: number[]; minuteContentHtml: string | null }>>(
        `${BACKEND_URL}/api/meeting/${meetingId}/participant-order`,
        { participantIds: this.minuteData().participants.map((participant) => participant.memberId) },
        { withCredentials: true },
      )
      .subscribe({
        next: (response) => {
          this.participantOrderSaving = false;
          if (response.mainBody?.minuteContentHtml !== undefined) {
            this.injectedMinuteDataService.setMinuteContentHtml(response.mainBody.minuteContentHtml);
          }
          this.participantOrderSnapshot = this.minuteData().participants.map((participant) => ({ ...participant }));
          this.diag()?.nativeElement.close();
          this.popupService.showPopup('Attendance order saved.', 'Success', 2200);
        },
        error: (error) => {
          this.participantOrderSaving = false;
          this.popupService.showPopup(error?.error?.message || 'Attendance order could not be saved.', 'Error', 3000);
        },
      });
  }

  //data loading logic is not in the component because data needs to be shared with minute-edit component.
  private injectedMinuteDataService = inject(MinuteDataService);
  minuteData = this.injectedMinuteDataService.getMinuteData();
  minuteNepali1 = viewChild(MinuteNepali1Component);
  minuteEnglish1 = viewChild(MinuteEnglish1Component);
  customProcessedMinute = viewChild<ElementRef<HTMLDivElement>>('customProcessedMinute');

  constructor(private httpClient: HttpClient, private popupService: PopupService, private route: ActivatedRoute) {}

  private routeMeetingId(): string | null {
    return this.route.snapshot.queryParamMap.get('meetingId');
  }

  htmlContent!: string | undefined;

  onWordFileDownload($event: Event) {
    $event.preventDefault();
    this.showMinuteOptions = false;

    const minuteData = this.minuteData();
    let renderedMinute: HTMLElement | null | undefined;

    if (minuteData.minuteContentHtml) {
      renderedMinute = this.customProcessedMinute()
        ?.nativeElement.querySelector<HTMLElement>('#a4-box');
    } else if (minuteData.minuteLanguage === 'ENGLISH') {
      renderedMinute = this.minuteEnglish1()
        ?.processedMinute()
        ?.nativeElement.querySelector<HTMLElement>('#a4-box');
    } else if (minuteData.minuteLanguage === 'NEPALI') {
      renderedMinute = this.minuteNepali1()
        ?.processedMinute()
        ?.nativeElement.querySelector<HTMLElement>('#a4-box');
    }

    // Send the actual minute surface, including #a4-box. The backend uses
    // that element as the document root when converting HTML to DOCX.
    this.htmlContent = renderedMinute?.outerHTML;

    // A saved custom draft can briefly render before its view child is
    // available. Keep the download usable by falling back to the saved HTML.
    if (!this.htmlContent && minuteData.minuteContentHtml?.trim()) {
      this.htmlContent = `<div id="a4-box">${minuteData.minuteContentHtml}</div>`;
    }

    if (!this.htmlContent?.trim()) {
      console.error('Cannot download minute: rendered minute content is empty');
      return;
    }

    this.httpClient
      .post(BACKEND_URL + '/api/word-file-for-minute', this.htmlContent, {
        withCredentials: true,
        headers: { 'Content-Type': 'text/html; charset=UTF-8' },
        responseType: 'blob', // This tells Angular to parse the body as binary
      })
      .subscribe({
        next: (blob: Blob) => {
          // The response is now directly the file blob.
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'minute.docx'; // TODO: extract the file name from header or create the download name in the frontend itself with time in the title
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: (error) => {
          //TODO: handle error properly
          console.error('Download failed', error.error);
        },
      });
  }
}
