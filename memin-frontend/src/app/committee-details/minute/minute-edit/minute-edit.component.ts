import { AfterViewChecked, Component, ElementRef, inject, OnInit, viewChildren } from '@angular/core';
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
import { toNepaliDigits } from '../../../../utils/custom-functions';
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
      if (classSection.matches('ol, ul, .minute-list')) {
        return classSection;
      }
      const list = classSection.querySelector<HTMLElement>('ol, ul, .minute-list');
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
      if (sibling.matches('ol, ul, .minute-list')) {
        return sibling;
      }
      const list = sibling.querySelector<HTMLElement>('ol, ul, .minute-list');
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
      
      const isDivList = list.tagName.toLowerCase() === 'div';
      const isNepali = data.minuteLanguage === 'NEPALI';

      for (let i = 0; i < section.values.length; i++) {
        const value = section.values[i];
        if (isDivList) {
          const item = document.createElement('p');
          let numStr = String(i + 1);
          if (isNepali) {
            numStr = toNepaliDigits(numStr) || numStr;
          }
          item.textContent = `${numStr}. ${value}`;
          list.appendChild(item);
        } else {
          const item = document.createElement('li');
          item.textContent = value;
          list.appendChild(item);
        }
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

    // Keep the structured fields and the directly editable minute surface in sync.
    this.syncHtmlWithStructuredFields();
    

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
    // the same edited minute displayed by the minute view.
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
