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

  constructor(private httpClient: HttpClient) {}

  htmlContent!: string | undefined;

  onWordFileDownload($event: Event) {
    $event.preventDefault();
    this.showMinuteOptions = false;
    if (this.minuteData().minuteLanguage == 'ENGLISH') {
      this.htmlContent =
        this.minuteEnglish1()?.processedMinute()?.nativeElement?.innerHTML;
    } else if ((this.minuteData().minuteLanguage = 'NEPALI')) {
      this.htmlContent =
        this.minuteNepali1()?.processedMinute()?.nativeElement?.innerHTML;
    }

    this.httpClient
      .post(BACKEND_URL + '/api/word-file-for-minute', this.htmlContent, {
        withCredentials: true,
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
