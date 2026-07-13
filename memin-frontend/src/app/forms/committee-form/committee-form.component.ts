import {
  Component,
  effect,
  ElementRef,
  input,
  OnInit,
  output,
  viewChild,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MemberSearchResult,
  CommitteeCreationDto,
  MemberIdAndRole,
  CommitteeFormData,
} from '../../models/models';
import { SafeCloseDialogCustom } from '../../utils/safe-close-dialog-custom.directive';
import { debounceTime, Subscription } from 'rxjs';
import Fuse from 'fuse.js';
import {
  CdkDragDrop,
  DragDropModule,
  moveItemInArray,
} from '@angular/cdk/drag-drop';

@Component({
  selector: 'app-committee-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    SafeCloseDialogCustom,
    DragDropModule,
    FormsModule,
  ],
  templateUrl: './committee-form.component.html',
  styleUrl: './committee-form.component.scss',
})
export class CommitteeFormComponent implements OnInit {
  diag = viewChild<ElementRef<HTMLDialogElement>>('committee_form_dialog');

  //////////////////////////////////////
  // for next button mobile view
  isNextButtonInMobileViewActive() {
    if (
      this.name.valid &&
      this.description.valid &&
      this.isCoordinatorValid() &&
	this.isMinuteLanguageValid()
    ) {
      return true;
    } else {
      return false;
    }
  }

  //just checking if the selected coordinator id > 0(may need more rebust implementation)
  isCoordinatorValid() {
    return this.coordinator.value.memberId > 0;
  }

  isMinuteLanguageValid() {
    return (this.minuteLanguage.value == 'ENGLISH' || this.minuteLanguage.value=='NEPALI');
  }

  ///////////////////////////////////
  //for multipart form in mobile view, this variable is not used in desktop view
  isCommitteeDetailsPart = true;

  ///////////////////////////////////

  //for role dropdowns
  showDropdown = false;
  dropdownTop = 0;
  dropdownLeft = 0;

  //member for whom the dropdown is rendered should be stored for later use in case a drop down option is selected(as same drop down is rendered for each member)
  memberForWhomRoleDropdownIsShown: MemberSearchResult | undefined;

  showCustomDropdownForRoleSelection(
    event: FocusEvent,
    targetMember: MemberSearchResult,
  ) {
    const input = event.target as HTMLElement;
    const rect = input.getBoundingClientRect();
    this.dropdownTop = rect.bottom + 4;
    this.dropdownLeft = rect.left;
    this.memberForWhomRoleDropdownIsShown = targetMember;
    this.showDropdown = true;
  }

  ////////////////////////////////////

  // Form Drag and drop reorder

  drop(event: CdkDragDrop<MemberSearchResult[]>) {
    moveItemInArray(
      this.selectedMembersWithRoles,
      event.previousIndex,
      event.currentIndex,
    );
    console.log('drop executed');
    console.log(this.selectedMembersWithRoles);
  }

  //////////////////////////////////////////

  //SELECT MEMBER SECTION OF THE FORM

  searchInputFieldSubscription!: Subscription;

  //inputs
  isEditPage = input.required<boolean>();
  committeeFormData = input.required<CommitteeFormData>();

  //variables
  unselectedMembers: MemberSearchResult[] = [];
  displayedMembers: MemberSearchResult[] = [];
  selectedMembersWithRoles: {
    member: MemberSearchResult;
    role: string;
  }[] = [];

  //FORM CONTROLS:

  //setting aliases for this.committeeFormGroup().controls
  name!: FormControl<string>;
  description!: FormControl<string>;
  coordinator!: FormControl<MemberSearchResult>;
  status!: FormControl<'ACTIVE' | 'INACTIVE'>;
  maxNoOfMeetings!: FormControl<number>;
  minuteLanguage!: FormControl<'NEPALI' | 'ENGLISH' | null>;

  committeeFormGroup!: FormGroup<{
    name: FormControl<string>;
    description: FormControl<string>;
    coordinator: FormControl<MemberSearchResult>;
    status: FormControl<'ACTIVE' | 'INACTIVE'>;
    maxNoOfMeetings: FormControl<number>;
    minuteLanguage: FormControl<'NEPALI' | 'ENGLISH' | null>;
  }>;

  //formControls for left panel:
  selectMemberFormGroup = new FormGroup({
    searchBarInput: new FormControl(''),
  });

  //initialize form-controls for both right and left panel
  ngOnInit(): void {
    this.setupObservableForSearchBarInputChange();

    this.name = new FormControl(this.committeeFormData().name, {
      validators: [Validators.required],
      nonNullable: true,
    });
    this.description = new FormControl(this.committeeFormData().description, {
      validators: [Validators.required],
      nonNullable: true,
    });
    this.status = new FormControl(this.committeeFormData().status, {
      nonNullable: true,
    });
    this.maxNoOfMeetings = new FormControl(
      this.committeeFormData().maxNoOfMeetings,
      { nonNullable: true },
    );
    ((this.minuteLanguage = new FormControl(
      this.committeeFormData().minuteLanguage,
    )),
      {
        validators: [Validators.required],
      });

    //set unselected members and display them as well
    this.unselectedMembers = this.committeeFormData().unselectedMembers;
    this.displayedMembers = this.unselectedMembers;

    if (this.isEditPage()) {
      //if edit page, there may be already some selected members
      this.committeeFormData().selectedMembersWithRoles.forEach(
        (memberWithRole) => {
          this.addMemberToSelectedMembersWithRolesAndSync(
            memberWithRole.member,
            memberWithRole.role,
          );
        },
      );
    }

    //at last, add coordinator at last because coordinator has to be removed from the unselectedMembers
    this.coordinator = new FormControl(this.committeeFormData().coordinator, {
      validators: [Validators.required],
      nonNullable: true,
    });

    //this needs to be set because onCoordinatorSelect() relies on this to restore when a new coordaintor is selected
    this.currentCoordinator = this.committeeFormData().coordinator;
    this.removeMemberFromDisplayedMembers(this.committeeFormData().coordinator);
    this.removeMemberFromUnselectedMembers(
      this.committeeFormData().coordinator,
    );

    //finally initialize the form group for right panel
    this.committeeFormGroup = new FormGroup({
      name: this.name,
      description: this.description,
      coordinator: this.coordinator,
      status: this.status,
      maxNoOfMeetings: this.maxNoOfMeetings,
      minuteLanguage: this.minuteLanguage,
    });

    //finally restore selected members from local storage
    if (!this.isEditPage()) {
      this.restoreSelectedMembersFromLocalStorage();
    }
  }

  //when the search bar input changes filter the displayed members after a debounce time,
  setupObservableForSearchBarInputChange() {
    this.searchInputFieldSubscription =
      this.selectMemberFormGroup.controls.searchBarInput.valueChanges
        .pipe(debounceTime(500)) // wait 0.5 seconds after user stops typing
        .subscribe((value) => {
          console.log('searching');
          if (value === '') {
            this.displayedMembers = this.unselectedMembers;
          } else {
            this.displayedMembers = this.fuzzySearchUnselectedMembers(
              value as string,
            );
          }
        });
  }

  fuzzySearchUnselectedMembers(query: string): MemberSearchResult[] {
    const fuse = new Fuse(this.unselectedMembers, {
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

  sortUnselected() {
    return this.displayedMembers.sort(this.memberSortingFunction);
  }

  ngOnDestroy(): void {
    this.searchInputFieldSubscription.unsubscribe();
    console.log('DEBUG: select-member-for-committee component destroyed');
  }

  //by sync we mean to remove the selected member from unselected member and displayed member array
  addMemberToSelectedMembersWithRolesAndSync(
    selectedMember: MemberSearchResult,
    role: string,
  ) {
    this.removeMemberFromUnselectedMembers(selectedMember);
    this.removeMemberFromDisplayedMembers(selectedMember);
    this.selectedMembersWithRoles.push({
      member: selectedMember,
      role: role,
    });
  }

  removeMemberFromUnselectedMembers(memberToRemove: MemberSearchResult) {
    this.unselectedMembers = this.unselectedMembers.filter(
      (member) => member.memberId != memberToRemove.memberId,
    );
  }

  removeMemberFromDisplayedMembers(memberToRemove: MemberSearchResult) {
    this.displayedMembers = this.displayedMembers.filter(
      (member) => member.memberId != memberToRemove.memberId,
    );
  }

  removeMemberFromSelectedMembersWithRoles(memberToRemove: MemberSearchResult) {
    this.selectedMembersWithRoles = this.selectedMembersWithRoles.filter(
      (memberWithRole) =>
        memberWithRole.member.memberId != memberToRemove.memberId,
    );
  }

  onRoleAssign(role: string) {
    /* there are two possibilities:
       1. the member is already selected, this means, it is a role change
       2. the member is not yet select, this means, it is a new selected
    */

    if (this.memberForWhomRoleDropdownIsShown == undefined || role == '') {
      return;
    }

    //check if the member is already selected
    const memberToBeUpdatedWithRole = this.selectedMembersWithRoles.find(
      (memberWithRole) =>
        memberWithRole.member.memberId ==
        this.memberForWhomRoleDropdownIsShown?.memberId,
    );

    if (memberToBeUpdatedWithRole) {
      //change the role
      memberToBeUpdatedWithRole!.role = role;
    } else {
      //add the member to the selected array with the selected/assigned role
      this.addMemberToSelectedMembersWithRolesAndSync(
        this.memberForWhomRoleDropdownIsShown,
        role,
      );
    }
    this.showDropdown = false;
    this.memberForWhomRoleDropdownIsShown = undefined;
  }

  onRemoveSelectedInvitee(member: MemberSearchResult) {
    this.unselectedMembers.push(member);
    this.displayedMembers.push(member);
    this.removeMemberFromSelectedMembersWithRoles(member);
  }

  restoreSelectedMembersFromLocalStorage(): void {
    //if loadDataForEditCommitteePage = true, don't restore from local storage
    if (this.isEditPage()) return;

    //restore the selected members and remove them from the unselected list
    const savedSelectedMembers = localStorage.getItem(
      'selectedMembersWithRole',
    );
    if (savedSelectedMembers) {
      try {
        const selectedMembersWithRoles: {
          member: MemberSearchResult;
          role: string;
        }[] = JSON.parse(savedSelectedMembers);

        //for the saved selected members:
        //1. add them to the selected members list
        //2. restore the form control value for their role
        //3. remove them from the unselected members list and displayed members list

        console.log(selectedMembersWithRoles);
        selectedMembersWithRoles.forEach((memberWithRole) => {
          this.addMemberToSelectedMembersWithRolesAndSync(
            memberWithRole.member,
            memberWithRole.role,
          );
        });
      } catch (err) {
        console.error('Error parsing saved selected members data:', err);
      }
    }
  }

  ///////////////////////////////////////////////
  //main form section:

  //outputs
  formSaveEvent = output<CommitteeCreationDto>();

  constructor() {
    effect(() => {
      this.diag()!.nativeElement.showModal();
    });
  }

  currentCoordinator!: MemberSearchResult;

  onCoordinatorSelectionOrChange() {
    const newCoordinator = this.coordinator.value;
    //when current coordinator is a valid coordinator, add them ot unselectedMembers, and displayedMembers
    if (this.currentCoordinator.memberId > 0) {
      //add previous coordinator baced to unselected and displayed members
      this.unselectedMembers.push(this.currentCoordinator);
      this.displayedMembers.push(this.currentCoordinator);
    }

    this.removeMemberFromUnselectedMembers(newCoordinator);
    this.removeMemberFromDisplayedMembers(newCoordinator);
    this.currentCoordinator = newCoordinator;
  }

  showAllFormErrors = false;
  isFormSaving = false;

  onSubmit($event: Event) {
    $event.preventDefault();
    if (
      this.committeeFormGroup?.invalid ||
      this.selectedMembersWithRoles.length < 1 ||
      !this.isCoordinatorValid()
    ) {
      this.showAllFormErrors = true;
      return;
    }

    const committeeCreationDto = new CommitteeCreationDto();
    committeeCreationDto.name = this.name.value as string;
    committeeCreationDto.description = this.description.value as string;
    committeeCreationDto.coordinatorId = this.coordinator.value.memberId;
    committeeCreationDto.status = this.status.value;
    committeeCreationDto.maximumNumberOfMeetings = this.maxNoOfMeetings
      .value as number;
    committeeCreationDto.minuteLanguage = this.minuteLanguage.value!;

    this.selectedMembersWithRoles.forEach((memberWithRole) => {
      const memberIdAndRole = new MemberIdAndRole();
      memberIdAndRole.memberId = memberWithRole.member.memberId;
      memberIdAndRole.role = memberWithRole.role;
      committeeCreationDto.members.push(memberIdAndRole);
      console.log(memberWithRole.member.firstName);
    });
    console.log('emitting event');
    console.log(committeeCreationDto);
    this.isFormSaving = true;
    this.formSaveEvent.emit(committeeCreationDto);
  }

  /* used by the template to compare two coordinators in the select coordinator dropdown

   *  without this function, angular will compare the two objects by reference which will not work as expected in the case:
   *  when the Safe Close Dialog Directive saves and tries to restore the coordinator
   *  while restoring, a new object is created with the same values but different reference
   *  hence angular will not be able to select the correct option in the dropdown and will have blank value

   */
  compareCoordinators(o1: any, o2: any): boolean {
    return o1 && o2 ? o1.memberId === o2.memberId : o1 === o2;
  }

  saveForm = () => {
    //only try to saveForm if loadDataForEditCommitteePage = false
    if (this.isEditPage()) return;

    //save the form EXCEPT for the coordinator
    (this.committeeFormGroup as any).removeControl('coordinator');
    localStorage.setItem(
      'createCommitteeForm',
      JSON.stringify(this.committeeFormGroup.getRawValue()),
    );

    //save the selected members
    localStorage.setItem(
      'selectedMembersWithRole',
      JSON.stringify(this.selectedMembersWithRoles),
    );
  };

  restoreForm = () => {
    //only try to restore form if loadDataForEditCommitteePage = false
    if (this.isEditPage()) return;

    //restore the form except for the coordinator
    const savedForm = localStorage.getItem('createCommitteeForm');
    if (savedForm) {
      try {
        const parsedData = JSON.parse(savedForm);
        this.committeeFormGroup.patchValue(parsedData);
      } catch (err) {
        console.error('Error parsing saved form data:', err);
      }
    }
  };
}
