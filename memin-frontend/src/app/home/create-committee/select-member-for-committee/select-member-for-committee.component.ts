import { Component, inject, input, OnInit } from '@angular/core';
import {
  FormGroup,
  FormControl,
  ReactiveFormsModule,
} from '@angular/forms';
import { Subscription, debounceTime } from 'rxjs';
import { MemberSearchResult } from '../../../models/models';
import { MemberSelectionService } from './select-member-for-committee.service';
import { LoadMemberService } from '../../../load-member.service';

@Component({
  selector: 'app-select-member-for-committee',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './select-member-for-committee.component.html',
  styleUrl: './select-member-for-committee.component.scss',
})
export class SelectMemberForCommitteeComponent implements OnInit {
  memberSelectionService = inject(MemberSelectionService);
  memberLoadService = inject(LoadMemberService);


  //this variable is needed to prevent the template from accessing a form control before initialization

  searchInputFieldSubscription!: Subscription;

  formData = new FormGroup({
    searchBarInput: new FormControl(''),
  });

  //inputs
  memberAndRoleFormControlMap = input.required<Map<number, FormControl<string>>>();
  isEditPage = input.required<boolean>();


  ngOnInit(): void {
    this.setupObservableForSearchBarInputChange();
    this.restoreSelectedMembersFromLocalStorage();
  }


  //when the search bar input changes filter the displayed members after a debounce time,
  setupObservableForSearchBarInputChange() {
    this.searchInputFieldSubscription =
      this.formData.controls.searchBarInput.valueChanges
        .pipe(debounceTime(500)) // wait 0.5 seconds after user stops typing
        .subscribe((value) => {
          console.log('searching');
          if (value === '') {
            this.memberSelectionService.setDisplayed(
              this.memberSelectionService.unselected(),
            );
          } else {
            this.memberSelectionService.setDisplayed(
              this.memberSelectionService.fuzzySearchUnselectedMembers(
                value as string,
              ),
            );
          }
        });
  }

  ngOnDestroy(): void {
    this.searchInputFieldSubscription.unsubscribe();
    console.log("DEBUG: select-member-for-committee component destroyed");
  }

  onRoleSelect(selectedMember: MemberSearchResult) {
    const role = this.memberAndRoleFormControlMap().get(selectedMember.memberId)!.value;
    this.memberSelectionService.addMemberToSelectedMembersWithRolesAndSync(
      selectedMember,
      role,
    );
  }

  onRoleChange(member: MemberSearchResult): void {
    if(this.memberAndRoleFormControlMap().get(member.memberId)!.value === 'remove') {
      this.memberSelectionService.addMemberToUnselectedMembers(member);
      this.memberSelectionService.addMemberToDisplayedMembers(member);
      this.memberSelectionService.removeMemberFromSelectedMembersWithRoles(member);
      this.memberAndRoleFormControlMap().get(member.memberId)!.setValue('Add');
    }

    this.memberSelectionService.updateRoleOfSelectedMember(
      member,
      this.memberAndRoleFormControlMap().get(member.memberId)!.value,
    );
  }


  restoreSelectedMembersFromLocalStorage(): void {
    //if loadDataForEditCommitteePage = true, don't restore from local storage
    if(this.isEditPage()) return;


    //restore the selected members and remove them from the unselected list
    const savedSelectedMembers = localStorage.getItem('selectedMembersWithRole');
    if (savedSelectedMembers) {
      try {

        const selectedMembersWithRoles: {member: MemberSearchResult, role: string}[] =
          JSON.parse(savedSelectedMembers);

        //for the saved selected members:
        //1. add them to the selected members list
        //2. restore the form control value for their role
        //3. remove them from the unselected members list and displayed members list

        selectedMembersWithRoles.forEach((memberWithRole) => {
            this.memberSelectionService.addMemberToSelectedMembersWithRolesAndSync(
            memberWithRole.member, memberWithRole.role);
          });
      } catch (err) {
        console.error('Error parsing saved selected members data:', err);
      }
    }
  }
}
