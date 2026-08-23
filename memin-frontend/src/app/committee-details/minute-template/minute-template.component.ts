import {
  Component,
  ElementRef,
  AfterViewChecked,
  AfterViewInit,
  OnInit,
  ViewChild,
} from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BACKEND_URL } from '../../../global_constants';
import {
  MinuteTemplateDto,
  MinuteTemplateSummaryDto,
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
export class MinuteTemplateComponent implements OnInit, AfterViewInit, AfterViewChecked {
  @ViewChild('editor') editor?: ElementRef<HTMLDivElement>;
  @ViewChild('sourceEditor') sourceEditor?: ElementRef<HTMLTextAreaElement>;

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
    { token: '@chairman', label: 'Chairman', description: 'Meeting chairperson full name' },
    { token: '@attendance', label: 'Attendance table', description: 'Participant table with roles and signatures', block: true },
    { token: '@attendanceTable', label: 'Attendance table (explicit)', description: 'Participant table with roles and signatures', block: true },
    { token: '@attendanceList', label: 'Attendance list', description: 'Numbered participant list', block: true },
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

  readonly nepaliTemplatePresets: TemplatePreset[] = [
    {
      id: 'nepali-drc-standard',
      name: 'DRC standard minute',
      description: 'Standard departmental research committee minute layout with meeting-specific chairmanship.',
      html: `<p style="text-align: center"><strong>@committee</strong></p>
        <p>बैठक नं. @title</p>
        <p>आज मिति @date (@day) यस @committee को बैठक, @chairman ज्यूको अध्यक्षतामा बसी देहाय बमोजिम छलफल तथा निर्णय गरियो ।</p>
        <p><strong>उपस्थितिः</strong></p>
        @attendance
        <p>बैठक बसेको समयः @time बजे</p>
        <p><strong>निर्णयः</strong></p>
        @agendas
        @decisions
        <p>अध्यक्षः @chairman&nbsp;&nbsp;&nbsp;&nbsp;हस्ताक्षरः ____________________</p>`,
    },
    {
      id: 'nepali-legacy-structured',
      name: 'Legacy structured minute',
      description: 'Legacy structured academic minute layout retained for existing committees.',
      html: `<h1 style="text-align: center">@committee</h1>
        <p style="text-align: center"><strong>बैठकको कार्यवृत्त तथा निर्णय अभिलेख</strong></p>
        <p style="text-align: center">@title</p>
        <table class="minute-template-table">
          <tbody>
            <tr><th>बैठकको मिति</th><td>@day, @date</td></tr>
            <tr><th>समय</th><td>@time</td></tr>
            <tr><th>स्थान</th><td>@location</td></tr>
            <tr><th>समन्वयक</th><td>@coordinator</td></tr>
          </tbody>
        </table>
        <h2>१. उपस्थित सदस्य तथा आमन्त्रितहरू</h2>
        @attendance
        <h2>२. कार्यसूची तथा छलफलका विषयहरू</h2>
        @agendas
        <h2>३. निर्णयहरू</h2>
        @decisions
        <h2>४. प्रमाणिकरण</h2>
        <p>माथि उल्लिखित कार्यवृत्त बैठकमा भएको छलफल तथा निर्णयअनुसार सही अभिलेख गरिएको हो।</p>
        <table class="minute-template-table">
          <tbody>
            <tr><td>समन्वयकको हस्ताक्षर: ____________________</td><td>मिति: ____________________</td></tr>
            <tr><td>सदस्य-सचिवको हस्ताक्षर: ____________________</td><td>मिति: ____________________</td></tr>
          </tbody>
        </table>`,
    },
    {
      id: 'nepali-academic-department',
      name: 'Academic and departmental meeting',
      description: 'A structured layout for department, subject, examination, or academic council meetings.',
      html: `<div style="text-align: center">
          <h1>@committee</h1>
          <p><strong>शैक्षिक समिति बैठकको कार्यवृत्त</strong></p>
        </div>
        <p><strong>बैठकको विषय:</strong> @title</p>
        <p><strong>बैठक विवरण:</strong> @day, @date, @time, @location</p>
        <p><strong>समन्वयक:</strong> @coordinator</p>
        <h2>१. उपस्थित सदस्य तथा आमन्त्रितहरू</h2>
        @attendance
        <h2>२. अघिल्लो बैठकको कार्यवृत्त</h2>
        <p>अघिल्लो बैठकको कार्यवृत्त अध्ययन गरी आवश्यक संशोधनसहित स्वीकृत गरियो।</p>
        <h2>३. कार्यसूची तथा छलफल</h2>
        @agendas
        <h2>४. निर्णय तथा कार्यान्वयन</h2>
        @decisions
        <h2>५. विविध तथा आगामी बैठक</h2>
        <p>विविध विषयहरू: ________________________________________________</p>
        <p>आगामी बैठकको मिति, समय र स्थान: _________________________________</p>
        <p>बैठक समाप्त भएको समय: _________________________________</p>
        <p>कार्यवृत्त तयार गर्ने: ____________________&nbsp;&nbsp;&nbsp;&nbsp;प्रमाणित गर्ने: ____________________</p>`,
    },
    {
      id: 'nepali-decision-register',
      name: 'Concise decision register',
      description: 'A compact layout for recording proposals, discussions, decisions, and signatures.',
      html: `<h1>@committee - @title</h1>
        <p><strong>बैठकको मिति:</strong> @day, @date</p>
        <p><strong>समय:</strong> @time&nbsp;&nbsp;&nbsp;&nbsp;<strong>स्थान:</strong> @location</p>
        <p><strong>समन्वयक:</strong> @coordinator</p>
        <h2>१. उपस्थिति</h2>
        @attendance
        <h2>२. प्रस्ताव तथा छलफलका विषयहरू</h2>
        @agendas
        <h2>३. निर्णयहरू</h2>
        @decisions
        <p>आजको बैठकमा छलफल गर्नुपर्ने अन्य विषय बाँकी नरहेकाले बैठक समाप्त गरियो।</p>
        <p>अध्यक्षता/समन्वयक: ____________________&nbsp;&nbsp;&nbsp;&nbsp;सदस्य-सचिव: ____________________</p>`,
    },
  ];

  get allTemplatePresets(): TemplatePreset[] {
    return [...this.templatePresets, ...this.nepaliTemplatePresets]
      .filter((preset) => preset.id !== 'nepali-legacy-structured');
  }

  get availableTemplatePresets(): TemplatePreset[] {
    return this.normalizeTemplateLanguage(this.minuteLanguage) === 'NEPALI'
      ? this.nepaliTemplatePresets.filter((preset) => preset.id !== 'nepali-legacy-structured')
      : this.templatePresets;
  }

  get hasUnsavedChanges(): boolean {
    return this.editorHtml.trim() !== this.savedEditorHtml.trim();
  }

  get templateWarnings(): string[] {
    const requiredBlocks = [
      { name: 'attendance', label: 'attendance table' },
      { name: 'agendas', label: 'agenda list' },
      { name: 'decisions', label: 'decision list' },
    ];
    return requiredBlocks
      .filter(({ name }) => name === 'attendance'
        ? !this.hasAnyTemplateToken(['attendance', 'attendanceTable', 'attendanceList', 'participants'])
        : !this.hasTemplateToken(name))
      .map(({ label }) => `Add the ${label} token so meeting data appears in the saved minute.`);
  }

  committeeId = 0;
  committeeName = '';
  committeeDescription = '';
  minuteLanguage = '';
  editorHtml = '';
  templateName = 'Current template';
  savedTemplates: MinuteTemplateSummaryDto[] = [];
  selectedTemplateId: number | null = null;
  hasDataLoaded = false;
  isSaving = false;
  errorMessage = '';
  selectedPresetId: string | null = null;
  private savedEditorHtml = '';
  editorMode: 'visual' | 'source' = 'visual';

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

  ngAfterViewInit(): void {
    // The editor is created after the async template request completes. This
    // also keeps the initial HTML write separate from normal user editing.
    this.setEditorHtml();
  }

  /**
   * The editor is inside an @if block and the saved template arrives
   * asynchronously. Angular can therefore create the editor after
   * ngAfterViewInit has already run. Synchronize it once the view exists.
   * setEditorHtml only writes when the DOM differs from editorHtml, so this
   * does not overwrite user edits.
   */
  ngAfterViewChecked(): void {
    if (this.hasDataLoaded) {
      this.setEditorHtml();
    }
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
          this.savedTemplates = data.savedTemplates ?? [];
          this.selectedTemplateId = data.activeTemplateId ?? null;
          const activeTemplate = this.savedTemplates.find((template) => template.active);
          this.templateName = activeTemplate?.name ?? 'Current template';
          this.editorHtml = this.getInitialTemplate(data);
          this.selectedPresetId = this.allTemplatePresets.find(
            (preset) => preset.html.trim() === this.editorHtml.trim(),
          )?.id ?? null;
          this.savedEditorHtml = this.editorHtml;
          this.hasDataLoaded = true;
          setTimeout(() => this.setEditorHtml());
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
      return this.getDefaultTemplate(data.minuteLanguage);
    }

    const isNepaliTemplate = this.normalizeTemplateLanguage(data.minuteLanguage) === 'NEPALI';
    const oldHeader = data.minuteHeaderTemplate?.trim()
      ? `<div class="minute-header"><strong>${this.escapeHtml(data.minuteHeaderTemplate).replace(/\n/g, '<br>')}</strong></div>`
      : `<h1 style="text-align: center">@committee</h1>`;
    const oldOpening = data.minuteOpeningTemplate?.trim()
      ? `<p>${this.escapeHtml(data.minuteOpeningTemplate).replace(/\n/g, '<br>')}</p>`
      : isNepaliTemplate
        ? `<p>@committee को बैठक @day, @date मा @time बजे @location मा बस्यो। बैठकको उद्देश्य @purpose सम्बन्धी विषयमा छलफल गर्नु थियो। बैठकको समन्वय @coordinator ले गर्नुभयो।</p>`
        : `<p>The @committee meeting was held on @day, @date, at @time at @location. Its purpose was to oversee @purpose. The meeting was coordinated by @coordinator.</p>`;

    const sections = isNepaliTemplate
      ? `<h2>उपस्थित सदस्य तथा आमन्त्रितहरू</h2>
        @attendance
        <h2>कार्यसूची तथा छलफलका विषयहरू</h2>
        @agendas
        <h2>निर्णयहरू</h2>
        @decisions`
      : `<h2>Attendance</h2>
        @attendance
        <h2>Agendas</h2>
        @agendas
        <h2>Decisions</h2>
        @decisions`;

    return `${oldHeader}${oldOpening}${sections}`;
  }

  private getDefaultTemplate(language: string | null | undefined): string {
    return this.normalizeTemplateLanguage(language) === 'NEPALI'
      ? this.nepaliTemplatePresets[0].html
      : this.templatePresets[0].html;
  }

  private normalizeTemplateLanguage(language: string | null | undefined): 'ENGLISH' | 'NEPALI' {
    return language?.toUpperCase() === 'NEPALI' ? 'NEPALI' : 'ENGLISH';
  }

  setEditorHtml(): void {
    const editor = this.editor?.nativeElement;
    if (editor && editor.innerHTML !== this.editorHtml) {
      editor.innerHTML = this.editorHtml;
    }
    const sourceEditor = this.sourceEditor?.nativeElement;
    if (sourceEditor && sourceEditor.value !== this.editorHtml) {
      sourceEditor.value = this.editorHtml;
    }
  }

  setEditorMode(mode: 'visual' | 'source'): void {
    if (mode === this.editorMode) {
      return;
    }
    if (this.editorMode === 'visual') {
      this.syncEditorHtml();
    }
    this.editorMode = mode;
    setTimeout(() => {
      this.setEditorHtml();
      if (mode === 'source') {
        this.sourceEditor?.nativeElement.focus();
      } else {
        this.editor?.nativeElement.focus();
      }
    });
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
    if (this.editorMode === 'visual') {
      this.focusEditorAtEnd();
    } else {
      this.sourceEditor?.nativeElement.focus();
    }
  }

  onEditorInput(): void {
    this.syncEditorHtml();
    this.selectedPresetId = null;
    this.updateTokenSuggestions();
  }

  onSourceInput(value: string): void {
    this.editorHtml = value;
    this.selectedPresetId = null;
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
    const editor = this.editor?.nativeElement;
    if (!editor) {
      return;
    }
    // Focusing first and restoring second prevents the browser from
    // collapsing the saved selection when the toolbar receives focus.
    editor.focus();
    this.restoreSelection();
    document.execCommand(command, false, value);
    this.rememberSelection();
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
    if (this.editorMode === 'source') {
      this.insertTokenIntoSource(token.token);
      return;
    }
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
      templateId: this.selectedTemplateId,
      name: this.templateName.trim() || 'Current template',
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
          this.popupService.showPopup('Minute template saved to the template library.', 'Success', 2500);
          this.loadTemplate();
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

  saveAsNewTemplate(): void {
    const name = window.prompt('Name this minute template', `Copy of ${this.templateName}`);
    if (!name?.trim()) {
      return;
    }
    this.selectedTemplateId = null;
    this.templateName = name.trim();
    this.saveTemplate();
  }

  useSavedTemplate(template: MinuteTemplateSummaryDto): void {
    if (this.hasUnsavedChanges && !window.confirm('Replace the current edits with this saved template?')) {
      return;
    }
    this.selectedTemplateId = template.templateId;
    this.templateName = template.name;
    this.editorHtml = template.minuteTemplateHtml;
    this.selectedPresetId = null;
    this.savedEditorHtml = this.editorHtml;
    this.setEditorHtml();
  }

  deleteSavedTemplate(template: MinuteTemplateSummaryDto, event: Event): void {
    event.stopPropagation();
    if (!window.confirm(`Delete the "${template.name}" template?`)) {
      return;
    }
    this.httpClient
      .delete(`${BACKEND_URL}/api/committee/${this.committeeId}/minute-templates/${template.templateId}`, { withCredentials: true })
      .subscribe({
        next: () => {
          this.popupService.showPopup('Minute template deleted.', 'Success', 2200);
          this.loadTemplate();
        },
        error: (error) => this.popupService.showPopup(error?.error?.message || 'Minute template could not be deleted.', 'Error', 3000),
      });
  }

  cancel(): void {
    this.router.navigate(['/committee-details/overview'], {
      queryParams: { committeeId: this.committeeId },
    });
  }

  private syncEditorHtml(): void {
    if (this.editorMode === 'visual' && this.editor) {
      this.editorHtml = this.editor.nativeElement.innerHTML;
    }
  }

  restoreSavedTemplate(): void {
    if (!this.hasUnsavedChanges || window.confirm('Discard your unsaved template changes?')) {
      this.editorHtml = this.savedEditorHtml;
      this.selectedPresetId = this.allTemplatePresets.find(
        (preset) => preset.html.trim() === this.editorHtml.trim(),
      )?.id ?? null;
      this.setEditorHtml();
    }
  }

  private hasTemplateToken(name: string): boolean {
    const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return new RegExp(`(?:@${escapedName}|\\{${escapedName}\\}|\\{\\{${escapedName}\\}\\})`).test(this.editorHtml);
  }

  private hasAnyTemplateToken(names: string[]): boolean {
    return names.some((name) => this.hasTemplateToken(name));
  }

  private insertTokenIntoSource(token: string): void {
    const sourceEditor = this.sourceEditor?.nativeElement;
    if (!sourceEditor) {
      return;
    }
    const start = sourceEditor.selectionStart ?? this.editorHtml.length;
    const end = sourceEditor.selectionEnd ?? start;
    this.editorHtml = `${this.editorHtml.slice(0, start)}${token}${this.editorHtml.slice(end)}`;
    this.setEditorHtml();
    setTimeout(() => {
      const caret = start + token.length;
      sourceEditor.focus();
      sourceEditor.setSelectionRange(caret, caret);
    });
  }

  private focusEditorAtEnd(): void {
    const editor = this.editor?.nativeElement;
    if (!editor) {
      return;
    }

    editor.focus();
    const selection = window.getSelection();
    if (!selection) {
      return;
    }

    const range = document.createRange();
    range.selectNodeContents(editor);
    range.collapse(false);
    selection.removeAllRanges();
    selection.addRange(range);
    this.savedSelection = range.cloneRange();
  }

  private insertText(value: string): void {
    const editor = this.editor?.nativeElement;
    if (!editor) {
      return;
    }
    editor.focus();
    this.restoreSelection();
    document.execCommand('insertText', false, value);
    this.rememberSelection();
    this.syncEditorHtml();
  }

  private insertHtml(value: string): void {
    const editor = this.editor?.nativeElement;
    if (!editor) {
      return;
    }
    editor.focus();
    this.restoreSelection();
    document.execCommand('insertHTML', false, value);
    this.rememberSelection();
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
