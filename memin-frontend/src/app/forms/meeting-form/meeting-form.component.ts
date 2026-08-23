import { HttpClient, HttpParams } from '@angular/common/http';
import {
  Component,
  AfterViewChecked,
  effect,
  ElementRef,
  input,
  OnInit,
  output,
  ViewChild,
  viewChild,
} from '@angular/core';
import {
  CdkDragDrop,
  DragDropModule,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import {
  FormGroup,
  FormControl,
  ReactiveFormsModule,
  FormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import Fuse from 'fuse.js';
import { Subscription, debounceTime } from 'rxjs';
import { BACKEND_URL } from '../../../global_constants';
import {
  MemberSearchResult,
  MeetingCreationDto,
  MeetingFormData,
  AgendaDto,
  DecisionDto,
  CommitteeOverviewDto,
  MinuteTemplateDto,
} from '../../models/models';
import { SafeCloseDialogCustom } from '../../utils/safe-close-dialog-custom.directive';
import { Response } from '../../response/response';
import { formatMemberDisplayName } from '../../utils/member-display';

@Component({
  selector: 'app-meeting-form',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    SafeCloseDialogCustom,
    RouterLink,
    DragDropModule,
  ],
  templateUrl: './meeting-form.component.html',
  styleUrl: './meeting-form.component.scss',
})
export class MeetingForm implements OnInit, AfterViewChecked {
  //outputs
  formSaveEvent = output<MeetingCreationDto>();
  draftWithAiEvent = output<string>();

  //inputs
  isEditPage = input.required<boolean>();
  meetingFormData = input.required<MeetingFormData>();

  ////////////////////////////////////////////
  // for next button mobile view

  isNextButtonInMobileViewActive() {
    if (
      this.title.valid &&
      this.heldPlace.valid &&
      this.heldTime.valid &&
      this.heldDate.valid &&
      this.selectedCommitteeId != undefined &&
      this.selectedChairman != null &&
      !this.hasNoNonEmptyMeetingItems()
    ) {
      return true;
    } else {
      return false;
    }
  }

  /////////////////////////////////////////////
  //Form control initalization with @Input data for left and right panel

  selectInviteeFormGroup = new FormGroup({
    searchBarInput: new FormControl(''),
  });

  //alias for this.meetingFormGroup().controls
  committeeSearch!: FormControl<string>;
  title!: FormControl<string>;
  heldDate!: FormControl<string>;
  heldTime!: FormControl<string>;
  heldPlace!: FormControl<string>;

  meetingFormGroup!: FormGroup<{
    title: FormControl<string>;
    heldDate: FormControl<string>;
    heldTime: FormControl<string>;
    heldPlace: FormControl<string>;
  }>;

  //for agenda and decision we will use two way binding as the input should be associated with DecisionDto not a string(as id is needed for edit page);
  agendas: AgendaDto[] = [];
  decisions: DecisionDto[] = [];
  aiDraftPrompt = '';

  ngOnInit(): void {
    //initializing the form groups and controls for both right and left panel
    this.committeeSearch = new FormControl(
      this.meetingFormData().committeeName,
      {
        nonNullable: true,
      },
    );

    this.title = new FormControl(this.meetingFormData().title, {
      validators: [Validators.required],
      nonNullable: true,
    });

    //correctly format the incoming date and time
    if (this.isEditPage()) {
      this.assignLoadedHeldDateAndTimeToFormControl(
        this.meetingFormData().heldDate,
        this.meetingFormData().heldTime,
      );
    } else {
      this.heldDate = new FormControl('', {
        validators: [Validators.required],
        nonNullable: true,
      });

      this.heldTime = new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      });
    }

    this.heldPlace = new FormControl(this.meetingFormData().heldPlace, {
      validators: [Validators.required],
      nonNullable: true,
    });

    this.decisions = this.meetingFormData().decisions;
    this.agendas = this.meetingFormData().agendas;

    this.meetingFormGroup = new FormGroup({
      title: this.title,
      heldDate: this.heldDate,
      heldTime: this.heldTime,
      heldPlace: this.heldPlace,
    });
    this.minuteDetailsSubscription = this.meetingFormGroup.valueChanges.subscribe(() => {
      this.updateMinuteDocument();
    });

    //INITIALIZING VARIABLES FOR THE LEFT PANEL
    this.possibleInvitees = this.meetingFormData().possibleInvitees;
    this.displayedPossibleInvitees = this.possibleInvitees;
    this.selectedInvitees = this.meetingFormData().selectedInvitees;
    this.selectedChairman = this.meetingFormData().chairman;
    this.meetingNumber = this.meetingFormData().meetingNumber || 1;
    this.hasInviteeDataLoaded = true;

    //INITIALIZING RIGHT PANELS SELECT COMMITTEE DROPDOWN
    if (this.isEditPage()) {
      this.committeeSearch.setValue(this.meetingFormData().committeeName);
      //edit page must be accessed with /committeeId in the route
      this.selectedCommitteeId = Number(
        this.activatedRoute.snapshot.queryParamMap.get('committeeId'),
      );
      this.committeeSearch.disable();
      this.loadCommitteeOverview(this.selectedCommitteeId);
    } else {
      //load data for right panel's select committee dropdown if it isn't an edit page
      this.loadActiveCommitteeNamesAndIdsForDropdown();
    }

    this.setupObservableForInviteeSearchBarInputChange();
    this.setupObservableForCommitteeSearchBarInputChange();
  }

  assignLoadedHeldDateAndTimeToFormControl(
    heldDate: string,
    heldTime: number[],
  ) {
    const heldDateObj = new Date(heldDate);
    this.heldDate = new FormControl(heldDateObj.toISOString().slice(0, 10), {
      validators: [Validators.required],
      nonNullable: true,
    });

    //formatting time in HH:MM format incase there is no padding infront
    const [h, m] = heldTime;

    // pad with leading zeros
    const hour = String(h).padStart(2, '0');
    const minute = String(m).padStart(2, '0');

    // final ISO-ish time string for LocalTime
    const timeString = `${hour}:${minute}:00`;

    this.heldTime = new FormControl(timeString, {
      validators: [Validators.required],
      nonNullable: true,
    });
  }

  loadActiveCommitteeNamesAndIdsForDropdown() {
    this.httpClient
      .get<
        Response<{ committeeId: number; committeeName: string }[]>
      >(BACKEND_URL + '/api/my-active-committee-names-and-ids', { withCredentials: true })
        .subscribe({
          next: (response) => {
          response.mainBody.forEach((committeeIdAndName) =>
            this.committeeIdsAndNames.push({
              committeeId: committeeIdAndName.committeeId,
              committeeName: committeeIdAndName.committeeName,
            }),
          );
          this.checkIfCommitteeIdAvailableInRoute();
          this.displayedCommitteeIdsAndNames = this.committeeIdsAndNames;
        },
      });
  }

  checkIfCommitteeIdAvailableInRoute() {
    //if not edit page, first check the route, if there is a committeeId, set that committeeId(when coming from /home/committee-details -> menu option)
    const committeeId =
      this.activatedRoute.snapshot.queryParamMap.get('committeeId');
    if (committeeId) {
      const selectedCommitteeIdAndName = this.committeeIdsAndNames.find(
        (committeeIdAndName) =>
          committeeIdAndName.committeeId == Number(committeeId),
      );
      if (selectedCommitteeIdAndName) {
        this.committeeSearch.setValue(selectedCommitteeIdAndName.committeeName);
        this.selectedCommitteeId = Number(committeeId);
        this.loadPossibleInvitees(selectedCommitteeIdAndName.committeeId);
        this.loadCommitteeOverview(selectedCommitteeIdAndName.committeeId);
      }
    }
  }

  //////////////////////////////////////////////
  //this variable is used for mobile view styling
  isMeetingDetailsPart = true;

  //---------------------------------LEFT PANEL-----------------------------

  //variables
  invitteeSearchInputFieldSubscription!: Subscription;
  possibleInvitees: MemberSearchResult[] = [];
  selectedInvitees: MemberSearchResult[] = [];
  displayedPossibleInvitees: MemberSearchResult[] = [];
  chairmanCandidates: MemberSearchResult[] = [];
  selectedChairman: MemberSearchResult | null = null;
  committeeCoordinatorId: number | null = null;
  meetingNumber = 1;

  constructor(
    private router: Router,
    private httpClient: HttpClient,
    private activatedRoute: ActivatedRoute,
  ) {
    //open dialog
    effect(() => {
      this.diag()!.nativeElement.showModal();
    });
  }

  onInviteeSelect(selectedInvitee: MemberSearchResult) {
    this.selectedInvitees.push(selectedInvitee);
    this.possibleInvitees = this.possibleInvitees.filter(
      (possibleInvitee) =>
        possibleInvitee.memberId !== selectedInvitee.memberId,
    );
    this.displayedPossibleInvitees = this.possibleInvitees;
    this.updateMinuteDocument();
  }

  onSelectedInviteeDrop(event: CdkDragDrop<MemberSearchResult[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    moveItemInArray(this.selectedInvitees, event.previousIndex, event.currentIndex);
    this.updateMinuteDocument();
  }

  moveSelectedInvitee(index: number, direction: -1 | 1, event?: Event): void {
    event?.stopPropagation();
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= this.selectedInvitees.length) {
      return;
    }
    moveItemInArray(this.selectedInvitees, index, targetIndex);
    this.updateMinuteDocument();
  }

  setupObservableForInviteeSearchBarInputChange() {
    this.invitteeSearchInputFieldSubscription =
      this.selectInviteeFormGroup.controls.searchBarInput.valueChanges
        .pipe(debounceTime(500)) // wait 0.5 seconds after user stops typing

        .subscribe((value) => {
          if (value === '') {
            this.displayedPossibleInvitees = this.possibleInvitees;
          } else {
            this.displayedPossibleInvitees = this.fuzzySearchPossibleInvitees(
              value as string,
            );
          }
        });
  }

  fuzzySearchPossibleInvitees(query: string): MemberSearchResult[] {
    const fuse = new Fuse(this.possibleInvitees, {
      keys: ['firstName', 'lastName'],
      threshold: 0.3, // lower = stricter match
    });
    return fuse
      .search(query)
      .map((result) => result.item)
      .sort(this.memberSortingFunction);
  }

  private memberSortingFunction = (
    member1: MemberSearchResult,
    member2: MemberSearchResult,
  ) => member1.firstName.localeCompare(member2.firstName);

  //---------------------------------RIGHT PANEL-----------------------------

  //variables
  FORM_NAME = 'create_meeting_form';
  diag = viewChild<ElementRef<HTMLDialogElement>>('new_meeting_dialogue');

  showDropdown = false;

  selectedCommitteeId!: number; //required to get the committeeId during request submission because the CommitteeSearch FormControl only stores the committeeName
  committeeSearchSubscription!: Subscription;
  committeeIdsAndNames: { committeeId: number; committeeName: string }[] = [];
  displayedCommitteeIdsAndNames: {
    committeeId: number;
    committeeName: string;
  }[] = [];

  setupObservableForCommitteeSearchBarInputChange() {
    this.committeeSearchSubscription = this.committeeSearch.valueChanges
      .pipe(debounceTime(250))
      .subscribe((value) => {
        if (value === '') {
          this.displayedCommitteeIdsAndNames = this.committeeIdsAndNames;
        } else {
          this.displayedCommitteeIdsAndNames = this.fuzzySearchCommittee(
            value as string,
          );
        }
      });
  }

  //if a committee is already selected, and again 'Select Committee' is clicked, all possible committees are displayed
  onCommitteeSearchBarFocus() {
    this.displayedCommitteeIdsAndNames = this.committeeIdsAndNames;
  }

  fuzzySearchCommittee(
    query: string,
  ): { committeeId: number; committeeName: string }[] {
    const fuse = new Fuse(Array.from(this.committeeIdsAndNames.values()), {
      keys: ['committeeName'],
      threshold: 0.3,
    });
    return fuse
      .search(query)
      .map((result) => result.item)
      .sort(this.committeeSortingFunction);
  }

  private committeeSortingFunction = (
    committee1: { committeeId: number; committeeName: string },
    committee2: { committeeId: number; committeeName: string },
  ) => committee1.committeeName.localeCompare(committee2.committeeName);

  //to use in template to make sure invitee has been displayed before displaying: No possible invitees
  hasInviteeDataLoaded = false;

  // Handle option selection
  //when isEditPage = true, the committeeSelection dropdown is disabled and data is never loaded with an api call to the backend, instead, the data is prefilled from the @Input meetingFormData
  onCommitteeSelection(committeeIdAndName: {
    committeeId: number;
    committeeName: string;
  }): void {
    this.committeeSearch.setValue(committeeIdAndName.committeeName);
    this.selectedCommitteeId = committeeIdAndName.committeeId;
    this.showDropdown = false;

    //reset displayedCommitteeIdsAndNames
    this.displayedCommitteeIdsAndNames = this.committeeIdsAndNames;

    //clear the invitees variables first
    this.possibleInvitees = [];
    this.displayedPossibleInvitees = [];
    this.selectedInvitees = [];
    this.chairmanCandidates = [];
    this.selectedChairman = null;
    this.minuteDocumentHtml = '';
    this.renderedDocumentHtml = '';

    this.hasInviteeDataLoaded = false;
    this.coordinatorName = '';
    this.committeeCoordinatorId = null;
    this.loadPossibleInvitees(committeeIdAndName.committeeId);
    this.loadCommitteeOverview(committeeIdAndName.committeeId);
  }

  loadPossibleInvitees(committeeId: number) {
    this.httpClient
      .get<Response<MemberSearchResult[]>>(
        BACKEND_URL + '/api/possible-invitees',
        {
          params: new HttpParams().set('committeeId', committeeId),
          withCredentials: true,
        },
      )
      .subscribe({
        next: (response) => {
          const selectedInviteeIds = new Set(
            this.selectedInvitees.map((invitee) => invitee.memberId),
          );
          this.possibleInvitees = response.mainBody.filter(
            (invitee) => !selectedInviteeIds.has(invitee.memberId),
          );
          this.displayedPossibleInvitees = this.possibleInvitees;
          this.hasInviteeDataLoaded = true;
        },
        error: () => {
          this.hasInviteeDataLoaded = true;
        },
      });
  }

  redirectToCreateCommittee() {
    this.router.navigate(['/home/create-committee']);
  }

  hasNoNonEmptyMeetingItems(): boolean {
    const hasAgenda = this.agendas.some(
      (agenda) => !!agenda.agenda && agenda.agenda.trim().length > 0,
    );
    const hasDecision = this.decisions.some(
      (decision) => !!decision.decision && decision.decision.trim().length > 0,
    );
    return !hasAgenda && !hasDecision;
  }

  showAllFormErrors = false;
  isFormSaving = false;

  onSubmit($event: Event, draftWithAi = false) {
    $event.preventDefault();
    if (
      this.meetingFormGroup.invalid ||
      this.hasNoNonEmptyMeetingItems() ||
      this.selectedCommitteeId == undefined ||
      this.selectedChairman == null
    ) {
      this.showAllFormErrors = true;
      return;
    }
    const requestBody = new MeetingCreationDto();
    requestBody.committeeId = this.selectedCommitteeId;
    requestBody.chairmanId = this.selectedChairman?.memberId || null;
    requestBody.title = this.title.value;
    requestBody.heldPlace = this.heldPlace.value;
    requestBody.heldDate = this.heldDate.value;

    requestBody.heldTime = this.heldTime.value;
    requestBody.agendas = this.agendas.filter(
      (agenda) => agenda.agenda && agenda.agenda.trim().length > 0,
    );
    requestBody.decisions = this.decisions.filter(
      (decision) => decision.decision && decision.decision.trim().length > 0,
    );
    requestBody.inviteeIds = this.selectedInvitees.map(
      (invitee) => invitee.memberId,
    );

    this.isFormSaving = true;
    if (draftWithAi) {
      this.draftWithAiEvent.emit(this.aiDraftPrompt.trim());
    }
    this.formSaveEvent.emit(requestBody);

    localStorage.removeItem(this.FORM_NAME);
  }

  onSubmitWithAi($event: Event) {
    this.onSubmit($event, true);
  }

  count = -1; //unique negative number which is assigned as the decision or agenda id which is used for deletion

  @ViewChild('minuteDocument') minuteDocument?: ElementRef<HTMLDivElement>;
  minuteDocumentHtml = '';
  private renderedDocumentHtml = '';
  private minuteDocumentVersion = 0;
  private minuteDetailsSubscription?: Subscription;

  createEmptyAgenda(): void {
    const tempId = -Math.floor(Math.random() * 1000000);
    const newAgenda = new AgendaDto();
    newAgenda.agendaId = tempId;
    newAgenda.agenda = '';
    this.agendas.push(newAgenda);
    this.updateMinuteDocument();
    this.focusLastMinuteItem('agenda');
  }

  createEmptyDecision(): void {
    const tempId = -Math.floor(Math.random() * 1000000);
    const newDecision = new DecisionDto();
    newDecision.decisionId = tempId;
    newDecision.decision = '';
    this.decisions.push(newDecision);
    this.updateMinuteDocument();
    this.focusLastMinuteItem('decision');
  }

  deleteAgenda(agendaId: number) {
    this.agendas = this.agendas.filter(
      (agenda) => agenda.agendaId !== agendaId,
    );
    this.updateMinuteDocument();
  }

  deleteDecision(decisionId: number) {
    this.decisions = this.decisions.filter(
      (decision) => decision.decisionId !== decisionId,
    );
    this.updateMinuteDocument();
  }

  coordinatorName = '';
  minuteTemplateHtml: string | null = null;
  minuteTemplateLanguage = '';
  isMinuteTemplateLoading = false;
  minuteTemplateLoadError = false;

  loadCommitteeOverview(committeeId: number): void {
    if (!committeeId) {
      this.coordinatorName = '';
      this.committeeCoordinatorId = null;
      this.chairmanCandidates = [];
      this.selectedChairman = null;
      this.minuteTemplateHtml = null;
      this.minuteTemplateLanguage = '';
      this.isMinuteTemplateLoading = false;
      this.minuteTemplateLoadError = false;
      this.updateMinuteDocument();
      return;
    }

    this.loadCommitteeMinuteTemplate(committeeId);

    this.httpClient
      .get<Response<CommitteeOverviewDto>>(BACKEND_URL + '/api/committee-overview', {
        params: new HttpParams().set('committeeId', committeeId),
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          if (!this.isEditPage()) {
            this.meetingNumber = (response.mainBody.meetingCount || 0) + 1;
          }
          this.minuteTemplateLanguage = response.mainBody.language || this.minuteTemplateLanguage;
          this.coordinatorName = response.mainBody.coordinatorName || '';
          this.committeeCoordinatorId = response.mainBody.coordinatorId ?? null;
          this.chairmanCandidates = response.mainBody.chairmanCandidates || [];
          const currentChairmanId = this.selectedChairman?.memberId;
          this.selectedChairman = this.chairmanCandidates.find(
            (candidate) => candidate.memberId === currentChairmanId,
          ) || this.chairmanCandidates.find(
            (candidate) => candidate.memberId === this.committeeCoordinatorId,
          ) || this.chairmanCandidates[0] || null;
          this.updateMinuteDocument();
        },
        error: () => {
          this.coordinatorName = '';
          this.committeeCoordinatorId = null;
          this.chairmanCandidates = [];
          this.selectedChairman = null;
          this.updateMinuteDocument();
        },
      });
  }

  loadCommitteeMinuteTemplate(committeeId: number): void {
    this.isMinuteTemplateLoading = true;
    this.minuteTemplateLoadError = false;
    this.minuteTemplateHtml = null;

    this.httpClient
      .get<Response<MinuteTemplateDto>>(
        `${BACKEND_URL}/api/committee/${committeeId}/minute-template`,
        { withCredentials: true },
      )
      .subscribe({
        next: (response) => {
          const template = response.mainBody;
          this.minuteTemplateHtml = template.minuteTemplateHtml?.trim() || null;
          this.minuteTemplateLanguage = template.minuteLanguage || '';
          this.isMinuteTemplateLoading = false;
          this.updateMinuteDocument();
        },
        error: () => {
          this.minuteTemplateHtml = null;
          this.minuteTemplateLanguage = '';
          this.isMinuteTemplateLoading = false;
          this.minuteTemplateLoadError = true;
          this.updateMinuteDocument();
        },
      });
  }

  get hasCommitteeMinuteTemplate(): boolean {
    return !!this.minuteTemplateHtml?.trim();
  }

  private buildMinuteDocumentHtml(): string {
    const template = this.minuteTemplateHtml?.trim() || this.getFallbackMinuteTemplate();
    const meetingDate = this.heldDate?.value || 'Date to be confirmed';
    const dateObject = meetingDate ? new Date(`${meetingDate}T00:00:00`) : null;
    const meetingDay = dateObject && !Number.isNaN(dateObject.getTime())
      ? new Intl.DateTimeFormat(undefined, { weekday: 'long' }).format(dateObject)
      : 'Day to be confirmed';
    const values: { names: string[]; value: string }[] = [
      { names: ['committeeName', 'committee', 'committe'], value: this.committeeDisplayName },
      { names: ['meetingTitle', 'title'], value: this.title?.value || 'Meeting title' },
      { names: ['meetingNumber', 'meetingNo'], value: this.formatMeetingNumber(this.meetingNumber) },
      { names: ['committeeDescription', 'purpose'], value: 'Committee purpose' },
      { names: ['date', 'data'], value: meetingDate },
      { names: ['day'], value: meetingDay },
      { names: ['partOfDay'], value: '' },
      { names: ['time'], value: this.heldTime?.value || 'Time to be confirmed' },
      { names: ['place', 'location'], value: this.heldPlace?.value || 'Venue to be confirmed' },
      { names: ['coordinator'], value: this.coordinatorDisplayName },
      { names: ['chairman', 'chairperson'], value: this.chairmanDisplayName },
      { names: ['header', 'openingParagraph'], value: '' },
      { names: ['attendance', 'participants'], value: this.templateSlotMarker('attendance') },
      { names: ['agendas'], value: this.templateSlotMarker('agendas') },
      { names: ['decisions'], value: this.templateSlotMarker('decisions') },
    ];

    let rendered = values.reduce((html, replacement) => {
      const safeValue = replacement.names.some((name) => ['attendance', 'participants', 'agendas', 'decisions'].includes(name))
        ? replacement.value
        : this.escapeTemplatePreviewValue(replacement.value);

      return replacement.names.reduce(
        (updatedHtml, name) => updatedHtml
          .replaceAll(`{{${name}}}`, safeValue)
          .replaceAll(`{${name}}`, safeValue)
          .replaceAll(`@${name}`, safeValue),
        html,
      );
    }, template);

    // A custom template may contain only the header and meeting metadata. Keep
    // that template as the document source of truth, but append any omitted
    // live sections inside the same document so editable meeting content can
    // never fall back to a second generic page.
    const missingSections: Array<{ names: string[]; heading: string; slot: 'attendance' | 'agendas' | 'decisions' }> = [
      { names: ['attendance', 'participants'], heading: this.minuteTemplateLanguage === 'NEPALI' ? 'उपस्थिति' : 'Attendance', slot: 'attendance' },
      { names: ['agendas'], heading: this.minuteTemplateLanguage === 'NEPALI' ? 'कार्यसूची' : 'Agendas', slot: 'agendas' },
      { names: ['decisions'], heading: this.minuteTemplateLanguage === 'NEPALI' ? 'निर्णयहरू' : 'Decisions and resolutions', slot: 'decisions' },
    ];
    missingSections.forEach(({ names, heading, slot }) => {
      if (!this.templateContainsToken(template, names)) {
        rendered += `<h2>${heading}</h2>${this.templateSlotMarker(slot)}`;
      }
    });
    return rendered;
  }

  private templateContainsToken(template: string, names: string[]): boolean {
    return names.some((name) => template.includes(`@${name}`)
      || template.includes(`{${name}}`)
      || template.includes(`{{${name}}}`));
  }

  private updateMinuteDocument(): void {
    if (!this.selectedCommitteeId) {
      this.minuteDocumentHtml = '';
      this.renderedDocumentHtml = '';
      return;
    }
    // The template string itself can remain unchanged when a user adds,
    // removes, or edits an item. Change the binding value so Angular rebuilds
    // the live controls from the latest arrays.
    this.minuteDocumentVersion++;
    this.minuteDocumentHtml = `<!-- minute-document-${this.minuteDocumentVersion} -->${this.buildMinuteDocumentHtml()}`;
    this.renderedDocumentHtml = '';
  }

  private templateSlotMarker(slot: 'attendance' | 'agendas' | 'decisions'): string {
    // CSS classes survive Angular's HTML sanitizer; custom data attributes do not.
    return `<span class="minute-slot-marker minute-slot-${slot}"></span>`;
  }

  private getFallbackMinuteTemplate(): string {
    const isNepali = this.minuteTemplateLanguage === 'NEPALI';
    if (isNepali) {
      return `<p style="text-align:center"><strong>@committee</strong></p><p>\u092c\u0948\u0920\u0915 \u0928\u0902. @meetingNo</p><p>\u0906\u091c \u092e\u093f\u0924\u093f @date (@day) \u092f\u0938 @committee \u0915\u094b \u092c\u0948\u0920\u0915, @chairman \u091c\u094d\u092f\u0942\u0915\u094b \u0905\u0927\u094d\u092f\u0915\u094d\u0937\u0924\u093e\u092e\u093e \u092c\u0938\u0940 \u0926\u0947\u0939\u093e\u092f \u092c\u092e\u094b\u091c\u093f\u092e \u091b\u0932\u092b\u0932 \u0924\u0925\u093e \u0928\u093f\u0930\u094d\u0923\u092f \u0917\u0930\u093f\u092f\u094b \u0964</p><h2>\u0909\u092a\u0938\u094d\u0925\u093f\u0924\u093f\u0903</h2>@attendance<h2>\u0928\u093f\u0930\u094d\u0923\u092f\u0903</h2>@agendas@decisions<p>\u0905\u0927\u094d\u092f\u0915\u094d\u0937\u0903 @chairman&nbsp;&nbsp;&nbsp;&nbsp;\u0939\u0938\u094d\u0924\u093e\u0915\u094d\u0937\u0930\u0903 ____________________</p>`;
    }
    return isNepali
      ? `<h1 style="text-align:center">@committee</h1><p><strong>बैठकको विषय:</strong> @title</p><p><strong>मिति:</strong> @date&nbsp;&nbsp;&nbsp;<strong>समय:</strong> @time&nbsp;&nbsp;&nbsp;<strong>स्थान:</strong> @location</p><h2>उपस्थिति</h2>@attendance<h2>कार्यसूची</h2>@agendas<h2>निर्णयहरू</h2>@decisions`
      : `<h1 style="text-align:center">@committee</h1><p><strong>Meeting no.:</strong> @meetingNo</p><p><strong>Meeting:</strong> @title</p><p><strong>Date:</strong> @date&nbsp;&nbsp;&nbsp;<strong>Time:</strong> @time&nbsp;&nbsp;&nbsp;<strong>Venue:</strong> @location</p><h2>Attendance</h2>@attendance<h2>Agendas</h2>@agendas<h2>Decisions and resolutions</h2>@decisions`;
  }

  ngAfterViewChecked(): void {
    const documentElement = this.minuteDocument?.nativeElement;
    if (!documentElement || !this.minuteDocumentHtml || this.renderedDocumentHtml === this.minuteDocumentHtml) {
      return;
    }

    const markers = Array.from(documentElement.querySelectorAll<HTMLElement>('.minute-slot-marker'));
    markers.forEach((marker) => {
      const slot = (['attendance', 'agendas', 'decisions'] as const)
        .find((candidate) => marker.classList.contains(`minute-slot-${candidate}`));
      if (slot === 'attendance') {
        marker.replaceWith(this.createAttendanceSlot());
      } else if (slot === 'agendas') {
        marker.replaceWith(this.createAgendaSlot());
      } else if (slot === 'decisions') {
        marker.replaceWith(this.createDecisionSlot());
      }
    });
    this.renderedDocumentHtml = this.minuteDocumentHtml;
    // The slot is built off-DOM first, so its initial width measurement can
    // be too small. Recalculate after the browser has laid out the paper so
    // existing long values use the available horizontal space before they
    // wrap vertically.
    requestAnimationFrame(() => {
      this.minuteDocument?.nativeElement
        .querySelectorAll<HTMLTextAreaElement>('.template-item-input')
        .forEach((input) => this.autoGrowTextarea(input));
    });
  }

  private createAttendanceSlot(): HTMLElement {
    const isNepali = this.minuteTemplateLanguage === 'NEPALI';
    const wrapper = document.createElement('div');
    wrapper.className = 'template-slot-content template-attendance-slot';
    const table = document.createElement('table');
    table.className = 'memberships';
    table.innerHTML = `<thead><tr><th>${isNepali ? 'क्र.सं.' : 'S.N.'}</th><th>${isNepali ? 'नाम' : 'Name'}</th><th>${isNepali ? 'पद/भूमिका' : 'Position'}</th><th>${isNepali ? 'हस्ताक्षर' : 'Signature'}</th></tr></thead>`;
    const body = document.createElement('tbody');
    const participants: Array<{ name: string; role: string }> = [];
    if (this.selectedChairman) {
      participants.push({ name: this.chairmanDisplayName, role: isNepali ? 'अध्यक्ष' : 'Chairman' });
    }
    if (this.coordinatorName && this.selectedChairman?.memberId !== this.committeeCoordinatorId) {
      participants.push({ name: this.coordinatorName, role: isNepali ? 'समन्वयक' : 'Coordinator' });
    }
    this.selectedInvitees.forEach((invitee) => participants.push({
      name: this.getInviteeName(invitee),
      role: isNepali ? 'आमन्त्रित' : 'Invitee',
    }));
    if (participants.length === 0) {
      const row = document.createElement('tr');
      row.innerHTML = `<td colspan="4" class="template-slot-empty">${isNepali ? 'बायाँपट्टिबाट सदस्य छान्नुहोस्।' : 'Select members from the left panel to add them here.'}</td>`;
      body.appendChild(row);
    } else {
      participants.forEach((participant, index) => {
        const row = document.createElement('tr');
        row.innerHTML = `<td>${index + 1}</td><td></td><td></td><td></td>`;
        row.children[1].textContent = participant.name;
        row.children[2].textContent = participant.role;
        body.appendChild(row);
      });
    }
    table.appendChild(body);
    wrapper.appendChild(table);
    return wrapper;
  }

  private createAgendaSlot(): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'template-slot-content template-list-slot';
    wrapper.appendChild(this.createSlotAction('agenda', '+ Add Agenda', () => this.createEmptyAgenda()));
    const list = document.createElement('ol');
    this.agendas.forEach((agenda, index) => {
      const row = document.createElement('li');
      const contentWrapper = document.createElement('div');
      contentWrapper.className = 'template-item-row';
      const input = document.createElement('textarea');
      input.rows = 1;
      input.className = 'template-item-input';
      input.value = agenda.agenda || '';
      input.placeholder = 'Enter an agenda item';
      input.setAttribute('aria-label', `Agenda item ${index + 1}`);
      input.addEventListener('input', () => {
        agenda.agenda = input.value;
        this.autoGrowTextarea(input);
      });
      const remove = this.createRemoveButton(`Remove agenda ${index + 1}`, () => this.deleteAgenda(agenda.agendaId));
      contentWrapper.append(input, remove);
      row.append(contentWrapper);
      list.appendChild(row);
      this.autoGrowTextarea(input);
    });
    if (this.agendas.length === 0) {
      const empty = document.createElement('li');
      empty.className = 'template-slot-empty template-slot-empty-list-item';
      empty.textContent = 'Add an agenda item using the button above.';
      list.appendChild(empty);
    }
    wrapper.appendChild(list);
    return wrapper;
  }

  private createDecisionSlot(): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'template-slot-content template-list-slot';
    wrapper.appendChild(this.createSlotAction('decision', '+ Add Decision', () => this.createEmptyDecision()));
    const list = document.createElement('ol');
    this.decisions.forEach((decision, index) => {
      const row = document.createElement('li');
      const contentWrapper = document.createElement('div');
      contentWrapper.className = 'template-item-row';
      const input = document.createElement('textarea');
      input.rows = 1;
      input.className = 'template-item-input';
      input.value = decision.decision || '';
      input.placeholder = 'Record the decision or resolution';
      input.setAttribute('aria-label', `Decision or resolution ${index + 1}`);
      input.addEventListener('input', () => {
        decision.decision = input.value;
        this.autoGrowTextarea(input);
      });
      const remove = this.createRemoveButton(`Remove decision ${index + 1}`, () => this.deleteDecision(decision.decisionId));
      contentWrapper.append(input, remove);
      row.append(contentWrapper);
      list.appendChild(row);
      this.autoGrowTextarea(input);
    });
    if (this.decisions.length === 0) {
      const empty = document.createElement('li');
      empty.className = 'template-slot-empty template-slot-empty-list-item';
      empty.textContent = 'Add a decision or resolution using the button above.';
      list.appendChild(empty);
    }
    wrapper.appendChild(list);
    return wrapper;
  }

  private createSlotAction(type: 'agenda' | 'decision', label: string, action: () => void): HTMLElement {
    const actionBar = document.createElement('div');
    actionBar.className = 'template-slot-action-bar';
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'minute-slot-add-button';
    button.textContent = label;
    button.dataset['slotAction'] = type;
    button.addEventListener('click', action);
    actionBar.appendChild(button);
    return actionBar;
  }

  private createRemoveButton(label: string, action: () => void): HTMLButtonElement {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'item-remove-button';
    button.textContent = '×';
    button.title = label;
    button.setAttribute('aria-label', label);
    button.addEventListener('click', action);
    return button;
  }

  private autoGrowTextarea(textarea: HTMLTextAreaElement): void {
    const row = textarea.closest<HTMLElement>('.template-item-row');
    const availableWidth = row
      ? Math.max(80, row.clientWidth - 42)
      : 640;
    const minimumWidth = Math.min(220, availableWidth);
    const styles = window.getComputedStyle(textarea);
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    let contentWidth = minimumWidth;

    if (context) {
      context.font = `${styles.fontStyle} ${styles.fontWeight} ${styles.fontSize} ${styles.fontFamily}`;
      const longestLine = textarea.value
        .split('\n')
        .reduce((longest, line) => Math.max(longest, context.measureText(line).width), 0);
      const horizontalPadding = parseFloat(styles.paddingLeft) + parseFloat(styles.paddingRight) + 2;
      contentWidth = Math.ceil(longestLine + horizontalPadding);
    }

    // Use horizontal space first. Once the paper width is reached, the
    // textarea wraps naturally and its height grows to fit the wrapped text.
    textarea.style.width = `${Math.min(availableWidth, Math.max(minimumWidth, contentWidth))}px`;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.max(textarea.scrollHeight, 46)}px`;
  }

  private focusLastMinuteItem(type: 'agenda' | 'decision'): void {
    setTimeout(() => {
      const selector = `[data-slot-action="${type}"]`;
      const action = this.minuteDocument?.nativeElement.querySelector<HTMLButtonElement>(selector);
      action?.closest('.template-slot-content')?.querySelector<HTMLTextAreaElement>('textarea:last-of-type')?.focus();
    });
  }

  private escapeTemplatePreviewValue(value: string | null | undefined): string {
    return (value || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  get committeeDisplayName(): string {
    return this.committeeSearch?.value?.trim() || this.meetingFormData().committeeName || 'Committee name';
  }

  get coordinatorDisplayName(): string {
    return this.coordinatorName || 'Not assigned';
  }

  get chairmanDisplayName(): string {
    return this.selectedChairman
      ? this.getInviteeName(this.selectedChairman)
      : this.coordinatorDisplayName;
  }

  onChairmanSelection(event: Event): void {
    const selectedId = Number((event.target as HTMLSelectElement).value);
    this.selectedChairman = this.chairmanCandidates.find(
      (candidate) => candidate.memberId === selectedId,
    ) || null;
    this.updateMinuteDocument();
  }

  formatMeetingDate(dateValue: string | null | undefined): string {
    if (!dateValue) {
      return 'Date to be confirmed';
    }

    const date = new Date(`${dateValue}T00:00:00`);
    return Number.isNaN(date.getTime())
      ? dateValue
      : new Intl.DateTimeFormat(undefined, {
          weekday: 'long',
          month: 'long',
          day: 'numeric',
          year: 'numeric',
        }).format(date);
  }

  formatMeetingTime(timeValue: string | null | undefined): string {
    if (!timeValue) {
      return 'Time to be confirmed';
    }

    const [hours, minutes] = timeValue.split(':').map(Number);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) {
      return timeValue;
    }

    const date = new Date(1970, 0, 1, hours, minutes);
    return new Intl.DateTimeFormat(undefined, {
      hour: 'numeric',
      minute: '2-digit',
    }).format(date);
  }

  getInviteeName(invitee: MemberSearchResult): string {
    return formatMemberDisplayName(invitee, this.minuteTemplateLanguage);
  }

  formatMeetingNumber(number: number | null | undefined): string {
    const value = String(number || 1);
    return this.minuteTemplateLanguage === 'NEPALI'
      ? value.replace(/[0-9]/g, (digit) => '०१२३४५६७८९'[Number(digit)])
      : value;
  }

  isDecisionInvalid(decision: DecisionDto): boolean {
    return this.showAllFormErrors && !decision.decision?.trim();
  }

  saveFormData = () => {
    if (this.isEditPage()) return;

    //also saving the agendas and decisions
    const dataToSave = {
      ...this.meetingFormGroup.getRawValue(),
      committeeName: this.committeeSearch.value,
      selectedCommitteeId: this.selectedCommitteeId,
      selectedInvitees: this.selectedInvitees,
      chairman: this.selectedChairman,
      agendas: this.agendas.map((agendaDto) => agendaDto.agenda),
      decisions: this.decisions.map((decisionDto) => decisionDto.decision),
    };

    // Check if at least one field has some value
    const hasData = Object.values(dataToSave).some(
      (value) => Array.isArray(value)
        ? value.length > 0
        : value !== null && value !== undefined && value !== '' && value !== 0,
    );

    if (!hasData) {
      return;
    }

    localStorage.setItem(this.FORM_NAME, JSON.stringify(dataToSave));
  };

  restoreFormData = () => {
    if (this.isEditPage()) return;

    //restore form normally ie restores the FormGroup
    const savedData = localStorage.getItem(this.FORM_NAME);
    if (savedData) {
      try {
        const parsedData = JSON.parse(savedData);
        this.meetingFormGroup.patchValue(parsedData); // prefill the form

        if (parsedData['committeeName']) {
          this.committeeSearch.setValue(parsedData['committeeName']);
        }

        if (parsedData['selectedCommitteeId']) {
          this.selectedCommitteeId = Number(parsedData['selectedCommitteeId']);
          this.loadPossibleInvitees(this.selectedCommitteeId);
          this.loadCommitteeOverview(this.selectedCommitteeId);
        }

        if (Array.isArray(parsedData['selectedInvitees'])) {
          this.selectedInvitees = parsedData['selectedInvitees'];
        }
        if (parsedData['chairman']) {
          this.selectedChairman = parsedData['chairman'];
        }

        //the above patchValue does not restore the FormArrays, so manually restoring agendas and decisions

        if (parsedData['agendas'] && parsedData['agendas'].length > 0) {
          (parsedData['agendas'] as string[]).forEach((agenda) => {
            const agendaDto = new AgendaDto();
            //agendaId is required for agenda deletion on double click
            agendaDto.agendaId = this.count--;
            agendaDto.agenda = agenda;
            this.agendas.push(agendaDto);
          });
        }

        if (parsedData['decisions'] && parsedData['decisions'].length > 0) {
          (parsedData['decisions'] as string[]).forEach((decision) => {
            const decisionDto = new DecisionDto();
            //agendaId is required for agenda deletion on double click
            decisionDto.decisionId = this.count--;
            decisionDto.decision = decision;
            this.decisions.push(decisionDto);
          });
        }
      } catch (err) {
        console.error('Failed to parse saved form data', err);
      }
    }
  };

  onSelectedInviteeRemoval(inviteeToUnselect: MemberSearchResult) {
    //remove from selected invitees
    this.selectedInvitees = this.selectedInvitees.filter(
      (invitee) => invitee.memberId != inviteeToUnselect.memberId,
    );

    //add to possible invtees
    //not added to displayedPossibleInvittes because when not being searched possibleInvitees and displayedPossibleInvitees point to the same array
    this.possibleInvitees.push(inviteeToUnselect);
    this.updateMinuteDocument();
  }

  ngOnDestroy() {
    this.committeeSearchSubscription.unsubscribe();
    this.invitteeSearchInputFieldSubscription.unsubscribe();
    this.minuteDetailsSubscription?.unsubscribe();
  }
}
