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
import { MemberSearchResult } from '../../../models/models';

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

  drop(event: CdkDragDrop<MemberSearchResult[]>) {
    moveItemInArray(
      this.minuteData().participants,
      event.previousIndex,
      event.currentIndex,
    );
    console.log('drop executed');
  }

  diag = viewChild<ElementRef<HTMLDialogElement>>('participant_order_dialog');
  showChangeParticipantOrderDialog() {
    this.showMinuteOptions = false;
    this.diag()!.nativeElement.showModal();
  }

  //data loading logic is not in the component because data needs to be shared with minute-edit component.
  minuteData = inject(MinuteDataService).getMinuteData();
  minuteNepali1 = viewChild(MinuteNepali1Component);
  minuteEnglish1 = viewChild(MinuteEnglish1Component);
  customProcessedMinute = viewChild<ElementRef<HTMLDivElement>>('customProcessedMinute');

  constructor(private httpClient: HttpClient) {}

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
