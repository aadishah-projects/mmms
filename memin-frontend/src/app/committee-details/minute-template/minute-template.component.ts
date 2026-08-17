import {
  Component,
  ElementRef,
  OnInit,
  ViewChild,
} from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BACKEND_URL } from '../../../global_constants';
import {
  MinuteTemplateDto,
  MinuteTemplateUpdateDto,
} from '../../models/models';
import { Response } from '../../response/response';
import { PopupService } from '../../popup/popup.service';

interface TemplateToken {
  token: string;
  label: string;
  description: string;
  block?: boolean;
}

interface TemplatePreset {
  id: string;
  name: string;
  description: string;
  html: string;
}

@Component({
  selector: 'app-minute-template',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './minute-template.component.html',
  styleUrl: './minute-template.component.scss',
})
export class MinuteTemplateComponent implements OnInit {
  @ViewChild('editor') editor?: ElementRef<HTMLDivElement>;

  readonly templateTokens: TemplateToken[] = [
    { token: '@committee', label: 'Committee', description: 'Committee name' },
    { token: '@title', label: 'Meeting title', description: 'Meeting title' },
    { token: '@purpose', label: 'Purpose', description: 'Committee description / purpose' },
    { token: '@date', label: 'Date', description: 'Meeting date' },
    { token: '@day', label: 'Day', description: 'Sunday, Monday, etc.' },
    { token: '@partOfDay', label: 'Part of day', description: 'Morning, afternoon, evening, etc.' },
    { token: '@time', label: 'Time', description: 'Meeting time' },
    { token: '@location', label: 'Location', description: 'Where the meeting was held' },
    { token: '@coordinator', label: 'Coordinator', description: 'Coordinator full name' },
    { token: '@attendance', label: 'Attendance table', description: 'Participant table with roles and signatures', block: true },
    { token: '@agendas', label: 'Agendas', description: 'Numbered agenda list', block: true },
    { token: '@decisions', label: 'Decisions', description: 'Numbered decisions list', block: true },
    { token: '@header', label: 'Saved header', description: 'Legacy committee header text' },
    { token: '@openingParagraph', label: 'Saved opening', description: 'Legacy opening paragraph text' },
  ];

  readonly templatePresets: TemplatePreset[] = [
    {
      id: 'classic',
      name: 'Classic minutes',
      description: 'A centered title followed by the opening, attendance, agendas, and decisions.',
      html: `<h1 style="text-align: center">@committee</h1>
        <p style="text-align: center"><strong>Meeting Minutes</strong></p>
        <p>The @committee was held on @day, @date, at @time at @location. Its purpose was to oversee @purpose. The meeting was coordinated by @coordinator.</p>
        <h2>Attendance</h2>
        @attendance
        <h2>Agendas</h2>
        @agendas
        <h2>Decisions</h2>
        @decisions`,
    },
    {
      id: 'formal',
      name: 'Formal report',
      description: 'A formal report layout with a meeting-information table and clear section headings.',
      html: `<div style="text-align: center">
          <h1>@committee</h1>
          <p><strong>OFFICIAL MEETING MINUTES</strong></p>
        </div>
        <table class="minute-template-table">
          <tbody>
            <tr><th>Meeting</th><td>@title</td></tr>
            <tr><th>Date and day</th><td>@day, @date</td></tr>
            <tr><th>Time and location</th><td>@time — @location</td></tr>
            <tr><th>Coordinator</th><td>@coordinator</td></tr>
          </tbody>
        </table>
        <p><strong>Purpose:</strong> @purpose</p>
        <h2>1. Attendance</h2>
        @attendance
        <h2>2. Agenda items</h2>
        @agendas
        <h2>3. Decisions and resolutions</h2>
        @decisions`,
    },
    {
      id: 'compact',
      name: 'Compact record',
      description: 'A concise one-page style for committees that need a shorter record.',
      html: `<h1>@committee — @title</h1>
        <p><strong>Held:</strong> @day, @date at @time, @location</p>
        <p><strong>Purpose:</strong> @purpose</p>
        <p><strong>Coordinator:</strong> @coordinator</p>
        <hr>
        <h2>Participants</h2>
        @attendance
        <h2>Discussion and decisions</h2>
        @decisions
        <h2>Agenda reference</h2>
        @agendas`,
    },
  ];

  committeeId = 0;
  committeeName = '';
  committeeDescription = '';
  minuteLanguage = '';
  editorHtml = '';
  hasDataLoaded = false;
  isSaving = false;
  errorMessage = '';
  selectedPresetId: string | null = null;
  private savedEditorHtml = '';

  showTokenSuggestions = false;
  tokenSuggestions: TemplateToken[] = [...this.templateTokens];
  tokenSuggestionIndex = 0;
  private tokenQueryLength = 0;

  showTableOptions = false;
  tableRows = 4;
  tableColumns = 4;
  tableHasHeader = true;

  private savedSelection: Range | null = null;

  constructor(
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private popupService: PopupService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.queryParamMap.subscribe((params) => {
      const committeeId = Number(params.get('committeeId'));
      if (!committeeId) {
        this.errorMessage = 'A committee was not selected.';
        return;
      }
      this.committeeId = committeeId;
      this.loadTemplate();
    });
  }

  loadTemplate(): void {
    const params = new HttpParams().set('committeeId', this.committeeId);
    this.httpClient
      .get<Response<MinuteTemplateDto>>(
        `${BACKEND_URL}/api/committee/${this.committeeId}/minute-template`,
        { params, withCredentials: true },
      )
      .subscribe({
        next: (response) => {
          const data = response.mainBody;
          this.committeeName = data.committeeName;
          this.committeeDescription = data.committeeDescription;
          this.minuteLanguage = data.minuteLanguage;
          this.editorHtml = this.getInitialTemplate(data);
          this.selectedPresetId = this.templatePresets.find(
            (preset) => preset.html.trim() === this.editorHtml.trim(),
          )?.id ?? null;
          this.savedEditorHtml = this.editorHtml;
          this.hasDataLoaded = true;
        },
        error: () => {
          this.errorMessage = 'The minute template could not be loaded.';
        },
      });
  }

  getInitialTemplate(data: MinuteTemplateDto): string {
    if (data.minuteTemplateHtml?.trim()) {
      return data.minuteTemplateHtml;
    }

    if (!data.minuteHeaderTemplate?.trim() && !data.minuteOpeningTemplate?.trim()) {
      return this.templatePresets[0].html;
    }

    const oldHeader = data.minuteHeaderTemplate?.trim()
      ? `<div class="minute-header"><strong>${this.escapeHtml(data.minuteHeaderTemplate).replace(/\n/g, '<br>')}</strong></div>`
      : `<h1 style="text-align: center">@committee</h1>`;
    const oldOpening = data.minuteOpeningTemplate?.trim()
      ? `<p>${this.escapeHtml(data.minuteOpeningTemplate).replace(/\n/g, '<br>')}</p>`
      : `<p>The @committee meeting was held on @day, @date, at @time at @location. Its purpose was to oversee @purpose. The meeting was coordinated by @coordinator.</p>`;

    return `${oldHeader}${oldOpening}
      <h2>Attendance</h2>
      @attendance
      <h2>Agendas</h2>
      @agendas
      <h2>Decisions</h2>
      @decisions`;
  }

  setEditorHtml(): void {
    const editor = this.editor?.nativeElement;
    if (editor && editor.innerHTML !== this.editorHtml) {
      editor.innerHTML = this.editorHtml;
    }
  }

  applyPreset(preset: TemplatePreset): void {
    if (!this.hasDataLoaded) {
      return;
    }

    const currentHtml = this.editorHtml.trim();
    if (currentHtml !== this.savedEditorHtml.trim() &&
        !window.confirm('Replace the current edits with this template? Your unsaved changes will be lost.')) {
      return;
    }

    this.selectedPresetId = preset.id;
    this.editorHtml = preset.html;
    this.setEditorHtml();
    this.editor?.nativeElement.focus();
  }

  onEditorInput(): void {
    this.syncEditorHtml();
    this.updateTokenSuggestions();
  }

  onEditorKeyup(event: KeyboardEvent): void {
    this.rememberSelection();
    this.updateTokenSuggestions();
  }

  onEditorKeydown(event: KeyboardEvent): void {
    if (!this.showTokenSuggestions || this.tokenSuggestions.length === 0) {
      if (event.key === 'Tab') {
        event.preventDefault();
        document.execCommand('insertText', false, '    ');
        this.syncEditorHtml();
      }
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.tokenSuggestionIndex =
        (this.tokenSuggestionIndex + 1) % this.tokenSuggestions.length;
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.tokenSuggestionIndex =
        (this.tokenSuggestionIndex - 1 + this.tokenSuggestions.length) %
        this.tokenSuggestions.length;
    } else if (event.key === 'Enter' || event.key === 'Tab') {
      event.preventDefault();
      this.selectToken(this.tokenSuggestions[this.tokenSuggestionIndex]);
    } else if (event.key === 'Escape') {
      this.showTokenSuggestions = false;
    }
  }

  rememberSelection(): void {
    const selection = window.getSelection();
    const editor = this.editor?.nativeElement;
    if (!selection || selection.rangeCount === 0 || !editor) {
      return;
    }

    const range = selection.getRangeAt(0);
    if (editor.contains(range.commonAncestorContainer)) {
      this.savedSelection = range.cloneRange();
    }
  }

  preventToolbarFocus(event: MouseEvent): void {
    event.preventDefault();
    this.rememberSelection();
  }

  format(command: string, value?: string): void {
    this.restoreSelection();
    this.editor?.nativeElement.focus();
    document.execCommand(command, false, value);
    this.syncEditorHtml();
  }

  insertHorizontalRule(): void {
    this.insertHtml('<hr>');
  }

  insertLink(): void {
    const url = window.prompt('Enter the link URL');
    if (url?.trim()) {
      this.format('createLink', url.trim());
    }
  }

  toggleTableOptions(): void {
    this.rememberSelection();
    this.showTableOptions = !this.showTableOptions;
  }

  insertTable(): void {
    const rows = Math.min(20, Math.max(1, Math.floor(Number(this.tableRows) || 1)));
    const columns = Math.min(12, Math.max(1, Math.floor(Number(this.tableColumns) || 1)));
    let html = '<table class="minute-template-table"><tbody>';

    for (let rowIndex = 0; rowIndex < rows; rowIndex++) {
      html += '<tr>';
      for (let columnIndex = 0; columnIndex < columns; columnIndex++) {
        const cell = this.tableHasHeader && rowIndex === 0 ? 'th' : 'td';
        html += `<${cell}>${cell === 'th' ? 'Header' : '<br>'}</${cell}>`;
      }
      html += '</tr>';
    }

    this.insertHtml(`${html}</tbody></table><p><br></p>`);
    this.showTableOptions = false;
  }

  addTableRow(): void {
    const table = this.getFocusedTable();
    if (!table) {
      return;
    }
    const row = table.insertRow(-1);
    const columnCount = table.rows[0]?.cells.length ?? 1;
    for (let index = 0; index < columnCount; index++) {
      row.insertCell(-1).innerHTML = '<br>';
    }
    this.syncEditorHtml();
  }

  addTableColumn(): void {
    const table = this.getFocusedTable();
    if (!table) {
      return;
    }
    Array.from(table.rows).forEach((row) => {
      row.insertCell(-1).innerHTML = '<br>';
    });
    this.syncEditorHtml();
  }

  deleteFocusedTable(): void {
    const table = this.getFocusedTable();
    if (table) {
      table.remove();
      this.syncEditorHtml();
    }
  }

  insertToken(token: TemplateToken): void {
    this.rememberSelection();
    this.insertText(token.token);
  }

  selectToken(token: TemplateToken): void {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) {
      this.showTokenSuggestions = false;
      return;
    }

    const range = selection.getRangeAt(0).cloneRange();
    if (range.collapsed && range.startContainer.nodeType === Node.TEXT_NODE) {
      const start = Math.max(0, range.startOffset - this.tokenQueryLength);
      range.setStart(range.startContainer, start);
      range.deleteContents();
      range.insertNode(document.createTextNode(token.token));
      range.collapse(false);
      selection.removeAllRanges();
      selection.addRange(range);
      this.syncEditorHtml();
    } else {
      this.insertToken(token);
    }

    this.showTokenSuggestions = false;
    this.tokenQueryLength = 0;
  }

  saveTemplate(): void {
    this.syncEditorHtml();
    const update: MinuteTemplateUpdateDto = {
      minuteTemplateHtml: this.editorHtml.trim() || null,
    };
    this.isSaving = true;
    this.httpClient
      .patch<Response<unknown>>(
        `${BACKEND_URL}/api/committee/${this.committeeId}/minute-template`,
        update,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.isSaving = false;
          this.savedEditorHtml = this.editorHtml;
          this.popupService.showPopup('Minute template saved.', 'Success', 2500);
          this.router.navigate(['/committee-details/overview'], {
            queryParams: { committeeId: this.committeeId },
          });
        },
        error: (error) => {
          this.isSaving = false;
          this.popupService.showPopup(
            error?.error?.message || 'Minute template could not be saved.',
            'Error',
            3500,
          );
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/committee-details/overview'], {
      queryParams: { committeeId: this.committeeId },
    });
  }

  private syncEditorHtml(): void {
    if (this.editor) {
      this.editorHtml = this.editor.nativeElement.innerHTML;
    }
  }

  private insertText(value: string): void {
    this.restoreSelection();
    this.editor?.nativeElement.focus();
    document.execCommand('insertText', false, value);
    this.syncEditorHtml();
  }

  private insertHtml(value: string): void {
    this.restoreSelection();
    this.editor?.nativeElement.focus();
    document.execCommand('insertHTML', false, value);
    this.syncEditorHtml();
  }

  private restoreSelection(): void {
    const selection = window.getSelection();
    if (!selection || !this.savedSelection) {
      return;
    }
    selection.removeAllRanges();
    selection.addRange(this.savedSelection.cloneRange());
  }

  private getFocusedTable(): HTMLTableElement | null {
    const selection = window.getSelection();
    const node = selection?.anchorNode;
    if (!node) {
      return null;
    }
    const element = node.nodeType === Node.ELEMENT_NODE
      ? node as Element
      : node.parentElement;
    return element?.closest('table') as HTMLTableElement | null;
  }

  private updateTokenSuggestions(): void {
    const beforeCaret = this.textBeforeCaret();
    const atIndex = beforeCaret.lastIndexOf('@');
    const query = atIndex >= 0 ? beforeCaret.slice(atIndex + 1) : '';
    const precedingCharacter = atIndex > 0 ? beforeCaret[atIndex - 1] : '';

    if (
      atIndex < 0 ||
      (atIndex > 0 && !/[\s([{"']/.test(precedingCharacter)) ||
      !/^[a-zA-Z]*$/.test(query)
    ) {
      this.showTokenSuggestions = false;
      this.tokenQueryLength = 0;
      return;
    }

    this.tokenSuggestions = this.templateTokens.filter(({ token }) =>
      token.slice(1).toLowerCase().startsWith(query.toLowerCase()),
    );
    this.tokenSuggestionIndex = 0;
    this.tokenQueryLength = query.length + 1;
    this.showTokenSuggestions = this.tokenSuggestions.length > 0;
  }

  private textBeforeCaret(): string {
    const selection = window.getSelection();
    const editor = this.editor?.nativeElement;
    if (!selection || selection.rangeCount === 0 || !editor) {
      return '';
    }

    const caret = selection.getRangeAt(0);
    const range = document.createRange();
    range.selectNodeContents(editor);
    range.setEnd(caret.startContainer, caret.startOffset);
    return range.toString();
  }

  private escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, (character) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    }[character] ?? character));
  }
}
