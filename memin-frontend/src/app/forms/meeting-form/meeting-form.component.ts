import { HttpClient, HttpParams } from '@angular/common/http';
import {
  Component,
  effect,
  ElementRef,
  input,
  OnInit,
  output,
  QueryList,
  viewChild,
  ViewChild,
  viewChildren,
} from '@angular/core';
import {
  FormGroup,
  FormControl,
  ReactiveFormsModule,
  FormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import Fuse from 'fuse.js';
import { Subscription, debounceTime, take } from 'rxjs';
import { BACKEND_URL } from '../../../global_constants';
import {
  MemberSearchResult,
  MeetingCreationDto,
  MeetingFormData,
  AgendaDto,
  DecisionDto,
} from '../../models/models';
import { CommonModule } from '@angular/common';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SafeCloseDialogCustom } from '../../utils/safe-close-dialog-custom.directive';
import { Response } from '../../response/response';

@Component({
  selector: 'app-meeting-form',
  standalone: true,
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule,
    SafeCloseDialogCustom,
    MatInputModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatOptionModule,
    RouterLink,
  ],
  templateUrl: './meeting-form.component.html',
  styleUrl: './meeting-form.component.scss',
})
export class MeetingForm implements OnInit {
  //outputs
  formSaveEvent = output<MeetingCreationDto>();

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
      !this.hasNoNonEmptyDecisions()
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

    //INITIALIZING VARIABLES FOR THE LEFT PANEL
    this.possibleInvitees = this.meetingFormData().possibleInvitees;
    this.displayedPossibleInvitees = this.possibleInvitees;
    this.selectedInvitees = this.meetingFormData().selectedInvitees;
    this.hasInviteeDataLoaded = true;

    //INITIALIZING RIGHT PANELS SELECT COMMITTEE DROPDOWN
    if (this.isEditPage()) {
      this.committeeSearch.setValue(this.meetingFormData().committeeName);
      //edit page must be accessed with /committeeId in the route
      this.selectedCommitteeId = Number(
        this.activatedRoute.snapshot.queryParamMap.get('committeeId'),
      );
      this.committeeSearch.disable();
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
          console.log(response.mainBody);
          response.mainBody.forEach((committeeIdAndName) =>
            this.committeeIdsAndNames.push({
              committeeId: committeeIdAndName.committeeId,
              committeeName: committeeIdAndName.committeeName,
            }),
          );
          this.checkIfCommitteeIdAvailableInRoute();
          this.displayedCommitteeIdsAndNames = this.committeeIdsAndNames;
          console.log(this.displayedCommitteeIdsAndNames);
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
  }

  setupObservableForInviteeSearchBarInputChange() {
    this.invitteeSearchInputFieldSubscription =
      this.selectInviteeFormGroup.controls.searchBarInput.valueChanges
        .pipe(debounceTime(500)) // wait 0.5 seconds after user stops typing

        .subscribe((value) => {
          console.log('searching');
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

    this.hasInviteeDataLoaded = false;
    this.loadPossibleInvitees(committeeIdAndName.committeeId);
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
          this.possibleInvitees = response.mainBody;
          this.displayedPossibleInvitees = this.possibleInvitees;
          console.log(this.displayedPossibleInvitees);
          this.hasInviteeDataLoaded = true;
        },
        error: (response) => {
          console.log(response);
          //TODO: handle error with popup message
        },
      });
  }

  redirectToCreateCommittee() {
    this.router.navigate(['/home/create-committee']);
  }

  hasNoNonEmptyDecisions(): boolean {
    return (
      this.decisions.filter((d) => d.decision && d.decision.length > 0).length <
      1
    );
  }

  showAllFormErrors = false;
  isFormSaving = false;

  onSubmit($event: Event) {
    $event.preventDefault();
    if (
      this.meetingFormGroup.invalid ||
      this.hasNoNonEmptyDecisions() ||
      this.selectedCommitteeId == undefined
    ) {
      this.showAllFormErrors = true;
      return;
    }
    const requestBody = new MeetingCreationDto();
    requestBody.committeeId = this.selectedCommitteeId;
    requestBody.title = this.title.value;
    requestBody.heldPlace = this.heldPlace.value;
    requestBody.heldDate = this.heldDate.value;

    requestBody.heldTime = this.heldTime.value;
    requestBody.agendas = this.agendas.filter(
      (agenda) => agenda.agenda.length > 0,
    );
    requestBody.decisions = this.decisions.filter(
      (decision) => decision.decision.length > 0,
    );
    console.log(requestBody);

    requestBody.inviteeIds = this.selectedInvitees.map(
      (invitee) => invitee.memberId,
    );

    this.isFormSaving = true;
    this.formSaveEvent.emit(requestBody);

    //TODO: should be done in the create meeting or edit meeting as request might fail as well
    localStorage.removeItem(this.FORM_NAME);
  }

  count = -1; //unique negative number which is assigned as the decision or agenda id which is used for deletion

  agendaInputFields = viewChildren<ElementRef>('agendaInputFields');

  createEmptyAgenda() {
    const newAgenda = new AgendaDto();
    newAgenda.agendaId = this.count;
    this.count--;
    this.agendas.push(newAgenda);

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
    this.decisions.push(newDecision);
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
    console.log('delete agenda executed');
    this.agendas = this.agendas.filter(
      (agenda) => agenda.agendaId !== agendaId,
    );
  }

  deleteDecision(decisionId: number) {
    this.decisions = this.decisions.filter(
      (decision) => decision.decisionId !== decisionId,
    );
  }

  saveFormData = () => {
    console.log('saving form');
    if (this.isEditPage()) return;

    const formValue = this.meetingFormGroup.getRawValue();

    //also saving the agendas and decisions
    const dataToSave = {
      ...this.meetingFormGroup.getRawValue(),
      agendas: this.agendas.map((agendaDto) => agendaDto.agenda),
      decisions: this.decisions.map((decisionDto) => decisionDto.decision),
    };

    // Check if at least one field has some value
    const hasData = Object.values(dataToSave).some(
      (value) => value !== null && value !== undefined && value !== '',
    );

    console.log(hasData);
    if (!hasData) {
      return;
    }

    localStorage.setItem(this.FORM_NAME, JSON.stringify(dataToSave));
    console.log('saving form');
    console.log(formValue);
  };

  restoreFormData = () => {
    if (this.isEditPage()) return;

    //restore form normally ie restores the FormGroup
    const savedData = localStorage.getItem(this.FORM_NAME);
    if (savedData) {
      console.log('Found the saved Data');
      console.log(savedData);
      try {
        const parsedData = JSON.parse(savedData);
        this.meetingFormGroup.patchValue(parsedData); // prefill the form

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
            console.log(decision);
            this.decisions.push(decisionDto);
          });
        }
      } catch (err) {
        console.error('Failed to parse saved form data', err);
      }
    }
  };

  onSelectedInviteeRemoval(inviteeToUnselect: MemberSearchResult) {
    console.log(inviteeToUnselect);
    //remove from selected invitees
    this.selectedInvitees = this.selectedInvitees.filter(
      (invitee) => invitee.memberId != inviteeToUnselect.memberId,
    );

    //add to possible invtees
    //not added to displayedPossibleInvittes because when not being searched possibleInvitees and displayedPossibleInvitees point to the same array
    this.possibleInvitees.push(inviteeToUnselect);
  }

  ngOnDestroy() {
    console.log('DEBUG: create-committee component destroyed');
    this.committeeSearchSubscription.unsubscribe();
    this.invitteeSearchInputFieldSubscription.unsubscribe();
  }
}
