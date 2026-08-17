import {
  Component,
  ElementRef,
  HostListener,
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
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-minute-view',
  standalone: true,
  imports: [
    MinuteNepali1Component,
    MinuteEnglish1Component,
    DragDropModule,
    FormsModule,
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
    this.showInsertTablePanel = false;
    this.toolbarMessage = '';
  }

  ////////////////////////////////////////////
  // flexible table editing in "Edit Directly" mode
  // The minute page itself is read-only; users edit inside isolated regions
  // (intro, headings, agenda/decision lists). Tables therefore can't be
  // created or destroyed accidentally by backspace/delete at paragraph
  // boundaries - only intentionally through these toolbar controls.

  showInsertTablePanel = false;
  insertRows = 2;
  insertCols = 3;
  toolbarMessage = '';
  caretInsideUserTable = false;
  attendanceSectionDetached = false;

  private lastEditableRange: Range | null = null;
  private parkedAttendanceSection: HTMLElement | null = null;
  private attendanceParkingPlaceholder: Comment | null = null;

  private getA4Box(): HTMLElement | null {
    const root =
      this.minuteNepali1()?.processedMinute()?.nativeElement ??
      this.minuteEnglish1()?.processedMinute()?.nativeElement ??
      null;
    return root ? root.querySelector<HTMLElement>('#a4-box') : null;
  }

  @HostListener('document:selectionchange')
  onSelectionChange() {
    if (!this.isEditMode) return;
    const box = this.getA4Box();
    const selection = window.getSelection();
    if (!box || !selection || selection.rangeCount === 0) return;

    const range = selection.getRangeAt(0);
    if (!box.contains(range.commonAncestorContainer)) return;

    this.lastEditableRange = range.cloneRange();

    let insideUserTable = false;
    let node: Node | null = range.commonAncestorContainer;
    while (node && node !== box) {
      if (
        node instanceof HTMLTableElement &&
        node.classList.contains('user-table')
      ) {
        insideUserTable = true;
        break;
      }
      node = node.parentNode;
    }
    this.caretInsideUserTable = insideUserTable;
  }

  toggleInsertTablePanel() {
    this.showInsertTablePanel = !this.showInsertTablePanel;
    this.toolbarMessage = '';
  }

  // parks the whole "Attendance" section (heading + table) outside the
  // document so it also disappears from printing and Word export
  removeAttendanceSection() {
    const box = this.getA4Box();
    const section = box?.querySelector<HTMLElement>('.memberships');
    if (!box || !section) return;

    this.attendanceParkingPlaceholder = document.createComment(
      'attendance-section-parked',
    );
    box.insertBefore(this.attendanceParkingPlaceholder, section);
    this.parkedAttendanceSection = section;
    section.remove();
    this.attendanceSectionDetached = true;
    this.toolbarMessage = '';
  }

  restoreAttendanceSection() {
    if (
      !this.parkedAttendanceSection ||
      !this.attendanceParkingPlaceholder?.parentNode
    ) {
      return;
    }
    this.attendanceParkingPlaceholder.parentNode.replaceChild(
      this.parkedAttendanceSection,
      this.attendanceParkingPlaceholder,
    );
    this.parkedAttendanceSection = null;
    this.attendanceParkingPlaceholder = null;
    this.attendanceSectionDetached = false;
    this.toolbarMessage = '';
  }

  insertTableAtStoredCaret() {
    this.toolbarMessage = '';
    const box = this.getA4Box();
    const range = this.lastEditableRange;
    if (!box || !range || !box.contains(range.commonAncestorContainer)) {
      this.toolbarMessage = 'Click where you want the table first.';
      return;
    }

    //walk up to the owning section - tables go right after the introduction,
    //agendas or decisions section (the attendance section has its own control)
    let section: HTMLElement | null = null;
    let node: Node | null = range.commonAncestorContainer;
    while (node && node !== box) {
      if (
        node instanceof HTMLElement &&
        ['introduction', 'agendas', 'decisions'].some((c) =>
          node instanceof HTMLElement ? node.classList.contains(c) : false,
        )
      ) {
        section = node;
        break;
      }
      node = node.parentNode;
    }
    if (!section) {
      this.toolbarMessage =
        'Tables can be placed after the introduction, agendas or decisions sections.';
      return;
    }

    //the wrapper sits directly in the a4 page, NOT inside an editable region:
    //non-editable neighbours cannot merge into it or delete it via backspace
    const wrapper = document.createElement('div');
    wrapper.className = 'user-table-block';
    wrapper.setAttribute('contenteditable', 'false');
    wrapper.appendChild(this.buildUserTable());
    box.insertBefore(wrapper, section.nextSibling);
    this.showInsertTablePanel = false;

    //move the caret into the first cell so the user can start typing immediately
    const firstCell = wrapper.querySelector('td');
    if (firstCell) {
      firstCell.focus();
      const newRange = document.createRange();
      newRange.setStart(firstCell, 0);
      newRange.collapse(true);
      const selection = window.getSelection();
      selection?.removeAllRanges();
      selection?.addRange(newRange);
    }
  }

  removeTableAtCaret() {
    const box = this.getA4Box();
    if (!box || !this.lastEditableRange) return;

    let node: Node | null = this.lastEditableRange.commonAncestorContainer;
    while (node && node !== box) {
      if (
        node instanceof HTMLElement &&
        node.classList.contains('user-table-block')
      ) {
        node.remove();
        this.caretInsideUserTable = false;
        return;
      }
      node = node.parentNode;
    }
  }

  // the table itself is contenteditable=false and each cell is its own
  // editable island, so backspace at the table's edge can never drag
  // neighbouring paragraphs into cells or delete the table by accident
  private buildUserTable(): HTMLTableElement {
    const rowCount = Math.min(Math.max(Math.floor(this.insertRows) || 1, 1), 12);
    const colCount = Math.min(Math.max(Math.floor(this.insertCols) || 1, 1), 8);

    const table = document.createElement('table');
    table.className = 'user-table';
    table.setAttribute('border', '1');
    table.setAttribute('cellspacing', '0');
    table.setAttribute('cellpadding', '5');
    table.setAttribute('contenteditable', 'false');

    const tbody = document.createElement('tbody');
    for (let r = 0; r < rowCount; r++) {
      const tr = document.createElement('tr');
      for (let c = 0; c < colCount; c++) {
        const td = document.createElement('td');
        td.setAttribute('contenteditable', 'true');
        td.appendChild(document.createElement('br'));
        tr.appendChild(td);
      }
      tbody.appendChild(tr);
    }
    table.appendChild(tbody);
    return table;
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
