import { AfterViewChecked, Component, ElementRef, inject, input, OnInit, viewChildren } from '@angular/core';
import { MinuteDataService } from '../minute-data.service';
import { FormsModule } from '@angular/forms';
import {
  AgendaDto,
  AiStructuredMinuteDto,
  DecisionDto,
  MinuteUpdateDto,
} from '../../../models/models';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Response } from '../../../response/response';
import { BACKEND_URL } from '../../../../global_constants';
import { PopupService } from '../../../popup/popup.service';

@Component({
  selector: 'app-minute-edit',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './minute-edit.component.html',
  styleUrl: './minute-edit.component.scss',
})
export class MinuteEditComponent implements OnInit, AfterViewChecked {
  minuteDataService = inject(MinuteDataService);
  minuteData = this.minuteDataService.getMinuteData();
  fullEditorMode = this.minuteDataService.getFullEditorMode();
  count = -1; //unique negative number which is assigned as the decision or agenda id which is used for deletion
  httpParams = new HttpParams();
  aiPrompt = '';
  aiInProgress = false;
  saveInProgress = false;

  constructor(
    private activatedRoute: ActivatedRoute,
    private httpClient: HttpClient,
    private router: Router,
    private popupService: PopupService,
  ) {}

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((params) => {
      console.log('setting params');
      this.httpParams = this.httpParams.set(
        'committeeId',
        params['committeeId'],
      );
      this.httpParams = this.httpParams.set('meetingId', params['meetingId']);
      console.log(params['committeeId']);
    });
  }

  ngAfterViewChecked(): void {
    if (this.fullEditorMode()) {
      return;
    }
    this.agendaInputFields().forEach((input) => this.resizeTextarea(input.nativeElement));
    this.decisionInputFields().forEach((input) => this.resizeTextarea(input.nativeElement));
  }

  hasNoNonEmptyMeetingItems(): boolean {
    const hasAgenda = this.minuteData().agendas.some(
      (agenda) => !!agenda.agenda && agenda.agenda.trim().length > 0,
    );
    const hasDecision = this.minuteData().decisions.some(
      (decision) => !!decision.decision && decision.decision.trim().length > 0,
    );
    return !hasAgenda && !hasDecision;
  }

  onFullContentInput(event: Event): void {
    const element = event.target as HTMLElement;
    this.minuteDataService.setMinuteContentHtml(element.innerHTML);
  }

  startFullEditor(): void {
    const data = this.minuteData();
    // Changing modes must not replace an existing AI/custom draft.
    if (data.minuteContentHtml?.trim()) {
      this.minuteDataService.setFullEditorMode(true);
      return;
    }
    const attendance = data.participants
      .map((participant, index) => `<tr><td>${index + 1}</td><td>${this.escapeHtml(participant.fullName)}</td><td>${this.escapeHtml(participant.role)}</td><td></td></tr>`)
      .join('');
    const agendas = data.agendas.map((agenda) => `<li>${this.escapeHtml(agenda.agenda)}</li>`).join('');
    const decisions = data.decisions.map((decision) => `<li>${this.escapeHtml(decision.decision)}</li>`).join('');
    this.minuteDataService.setMinuteContentHtml(`
      <h1>${this.escapeHtml(data.committeeName)} — Meeting Minute</h1>
      <p>${this.escapeHtml(data.openingParagraph ?? `Meeting held on ${data.meetingHeldDate} at ${data.meetingHeldPlace}.`)}</p>
      <h2>Attendance</h2>
      <table class="memberships" border="1"><thead><tr><th>S.N.</th><th>Name</th><th>Position</th><th>Signature</th></tr></thead><tbody>${attendance}</tbody></table>
      <h2>Agendas</h2><ol>${agendas}</ol>
      <h2>Decisions</h2><ol>${decisions}</ol>`);
    this.minuteDataService.setFullEditorMode(true);
  }

  useStructuredEditor(): void {
    const htmlContent = this.minuteData().minuteContentHtml;
    if (htmlContent) {
      this.copyStructuredFieldsFromHtml(htmlContent);
    }
    // Keep the custom HTML as the minute content. The separate mode flag
    // allows the structured fields to be shown without revealing stale data.
    this.minuteDataService.setFullEditorMode(false);
  }

  generateAiMinute(): void {
    if (this.aiInProgress) return;
    this.aiInProgress = true;
    const meetingId = this.httpParams.get('meetingId');
    this.httpClient
      .post<Response<AiStructuredMinuteDto>>(
        `${BACKEND_URL}/api/meetings/${meetingId}/ai-minute`,
        { roughPrompt: this.aiPrompt.trim() },
        { withCredentials: true },
      )
      .subscribe({
        next: (response) => {
          const result = response.mainBody;
          this.minuteDataService.setStructuredFields(result.agendas, result.decisions);
          this.minuteDataService.setMinuteContentHtml(result.htmlContent);
          this.minuteDataService.setFullEditorMode(!!result.htmlContent?.trim());
          this.aiInProgress = false;
          this.popupService.showPopup(
            result.usedCommitteeTemplate
              ? 'Agenda and decision entries refined. The committee template was filled with them.'
              : 'Agenda and decision entries refined. Review them before saving.',
            'Success',
            3500,
          );
        },
        error: (error) => {
          this.aiInProgress = false;
          const message = error?.error?.message;
          this.popupService.showPopup(
            message || 'AI refinement failed. Check the AI configuration.',
            'Error',
            3000,
          );
        },
      });
  }

  private escapeHtml(value: string): string {
    return value.replace(/[&<>"']/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character] ?? character));
  }

  private copyStructuredFieldsFromHtml(htmlContent: string): void {
    const container = document.createElement('div');
    container.innerHTML = htmlContent;

    const agendaValues = this.extractSectionItems(container, 'agenda');
    const decisionValues = this.extractSectionItems(container, 'decision');
    if (agendaValues === null && decisionValues === null) {
      return;
    }

    const data = this.minuteData();
    const agendas = agendaValues === null
      ? data.agendas
      : agendaValues.map((agenda, index) => ({
          agendaId: data.agendas[index]?.agendaId ?? this.count--,
          agenda,
        }));
    const decisions = decisionValues === null
      ? data.decisions
      : decisionValues.map((decision, index) => ({
          decisionId: data.decisions[index]?.decisionId ?? this.count--,
          decision,
        }));

    this.minuteDataService.setStructuredFields(agendas, decisions);
  }

  private extractSectionItems(
    container: HTMLElement,
    section: 'agenda' | 'decision',
  ): string[] | null {
    const list = this.findSectionList(container, section);
    if (!list) {
      return null;
    }

    return Array.from(list.children)
      .filter((child): child is HTMLElement => child.tagName.toLowerCase() === 'li')
      .map((item) => {
        const itemCopy = item.cloneNode(true) as HTMLElement;
        itemCopy.querySelectorAll('ol, ul').forEach((nestedList) => nestedList.remove());
        return (itemCopy.textContent ?? '')
          .replace(/^\s*(?:\d+|[\u0966-\u096F]+)\s*[.)।:-]?\s*/u, '')
          .trim();
      })
      .filter((item) => item.length > 0);
  }

  private findSectionList(
    container: HTMLElement,
    section: 'agenda' | 'decision',
  ): HTMLElement | null {
    const classNames = section === 'agenda'
      ? ['agenda', 'agendas']
      : ['decision', 'decisions'];

    const classSection = Array.from(container.querySelectorAll<HTMLElement>('[class]'))
      .find((element) => Array.from(element.classList).some((className) =>
        classNames.includes(className.toLowerCase()),
      ));
    if (classSection) {
      if (classSection.matches('ol, ul')) {
        return classSection;
      }
      const list = classSection.querySelector<HTMLElement>('ol, ul');
      if (list) {
        return list;
      }
    }

    const heading = Array.from(
      container.querySelectorAll<HTMLElement>('h1, h2, h3, h4, h5, h6'),
    ).find((element) => this.isSectionHeading(element.textContent ?? '', section));
    if (!heading) {
      return null;
    }

    let sibling = heading.nextElementSibling as HTMLElement | null;
    while (sibling) {
      if (/^H[1-6]$/.test(sibling.tagName)) {
        break;
      }
      if (sibling.matches('ol, ul')) {
        return sibling;
      }
      const list = sibling.querySelector<HTMLElement>('ol, ul');
      if (list) {
        return list;
      }
      sibling = sibling.nextElementSibling as HTMLElement | null;
    }

    return null;
  }

  private isSectionHeading(
    text: string,
    section: 'agenda' | 'decision',
  ): boolean {
    const normalized = text.trim().toLocaleLowerCase();
    if (section === 'agenda') {
      return /agenda|proposal|\u092a\u094d\u0930\u0938\u094d\u0924\u093e\u0935/u.test(normalized);
    }
    return /decision|\u0928\u093f\u0930\u094d\u0923\u092f/u.test(normalized);
  }

  private syncHtmlWithStructuredFields(): void {
    const data = this.minuteData();
    if (!data.minuteContentHtml) {
      return;
    }

    const container = document.createElement('div');
    container.innerHTML = data.minuteContentHtml;
    let changed = false;
    const sections: Array<{ type: 'agenda' | 'decision'; values: string[] }> = [
      { type: 'agenda', values: data.agendas.map((agenda) => agenda.agenda) },
      { type: 'decision', values: data.decisions.map((decision) => decision.decision) },
    ];

    for (const section of sections) {
      const list = this.findSectionList(container, section.type);
      if (!list) {
        continue;
      }

      while (list.firstChild) {
        list.removeChild(list.firstChild);
      }
      for (const value of section.values) {
        const item = document.createElement('li');
        item.textContent = value;
        list.appendChild(item);
      }
      changed = true;
    }

    if (changed) {
      this.minuteDataService.setMinuteContentHtml(container.innerHTML);
    }
  }

  
  agendaInputFields = viewChildren<ElementRef>('agendaInputFields');

  createEmptyAgenda() {
    const newAgenda = new AgendaDto();
    newAgenda.agendaId = this.count;
    this.count--;
    this.minuteData().agendas.push(newAgenda);

    // Wait for DOM Update
    setTimeout(() => {
      const inputs = this.agendaInputFields();
      const lastInput = inputs[inputs.length - 1];

      if (lastInput) {
        const element = lastInput.nativeElement;

        element.focus();

        //Scroll it into the center of the view
        element.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
        });
      }
    });
  }

  
  decisionInputFields = viewChildren<ElementRef>('decisionInputFields');

  createEmptyDecision() {
    const newDecision = new DecisionDto();
    newDecision.decisionId = this.count;
    this.count--;
    this.minuteData().decisions.push(newDecision);

    // Wait for DOM Update
    setTimeout(() => {
      const inputs = this.decisionInputFields();
      const lastInput = inputs[inputs.length - 1];

      if (lastInput) {
        const element = lastInput.nativeElement;

        element.focus();

        //Scroll it into the center of the view
        element.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
        });
      }
    });
  }

  deleteAgenda(agendaId: number) {
    this.minuteData().agendas = this.minuteData().agendas.filter(
      (agenda) => agenda.agendaId !== agendaId,
    );
  }

  deleteDecision(decisionId: number) {
    this.minuteData().decisions = this.minuteData().decisions.filter(
      (decision) => decision.decisionId !== decisionId,
    );
  }

  autoGrow(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.resizeTextarea(textarea);
  }

  private resizeTextarea(textarea: HTMLTextAreaElement): void {
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.max(textarea.scrollHeight, 44)}px`;
  }

  showAllErrors = false;
  onSubmit() {
    if (
      this.minuteData().committeeName.trim().length < 1 ||
      this.minuteData().committeeDescription.trim().length < 1 ||
      this.minuteData().meetingHeldDate.trim().length <1 ||
	this.minuteData().meetingHeldTime.trim().length < 1 ||
	this.minuteData().meetingHeldPlace.trim().length < 1 ||
      (!this.minuteData().minuteContentHtml && this.hasNoNonEmptyMeetingItems())
    ) {
      this.showAllErrors = true;
      return;
    } else {
      this.showAllErrors = false;
    }

    // Structured edits must update the same HTML draft that the minute view
    // and the full editor use. This keeps both editing modes synchronized.
    if (!this.fullEditorMode()) {
      this.syncHtmlWithStructuredFields();
    }
    

    const minuteUpdateDto = new MinuteUpdateDto();
    minuteUpdateDto.committeeName = this.minuteData().committeeName;
    minuteUpdateDto.committeeDescription =
      this.minuteData().committeeDescription;
    minuteUpdateDto.meetingHeldDate = this.minuteData().meetingHeldDate;
    minuteUpdateDto.meetingHeldTime = this.minuteData().meetingHeldTime;
    minuteUpdateDto.meetingHeldPlace = this.minuteData().meetingHeldPlace;
    minuteUpdateDto.decisions = this.minuteData().decisions;
    minuteUpdateDto.agendas = this.minuteData().agendas;
    // Keep the meeting-specific draft when saving structured fields. It is
    // the same edited minute displayed by the full editor and minute view.
    minuteUpdateDto.htmlContent = this.minuteData().minuteContentHtml ?? '';

    this.saveInProgress = true;
    this.httpClient
      .patch<
        Response<Object>
      >(BACKEND_URL + '/api/minute', minuteUpdateDto, { withCredentials: true, params: this.httpParams })
      .subscribe({
        next: (response) => {
          console.log('TODO: handle this properly' + response.message);
          this.saveInProgress = false;
          this.minuteDataService.markSaved();
          this.router.navigate(['./committee-details/overview'], {
            queryParamsHandling: 'preserve',
          });

	  this.popupService.showPopup("Minute Edited!", "Success", 2000);
        },

        error: (error) => {
	  this.saveInProgress = false;
          const message = error?.error?.message || 'Minute save failed. Please try again.';
	  this.popupService.showPopup(message, "Error", 4000);
        },
      });

    console.log(minuteUpdateDto);
    console.log(this.httpParams);

    //when form is submitted, first remove the empty decision and agenda, as server won't accept those(probably, check later);

    //now include all decisions and agendas, server will not save the empty ones, remove the deleted ones, and add new ones

    //the submit to the backend
  }
}
