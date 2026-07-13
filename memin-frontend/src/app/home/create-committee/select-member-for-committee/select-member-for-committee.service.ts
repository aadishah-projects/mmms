import { HttpClient } from '@angular/common/http';
import { inject, Injectable, OnDestroy, OnInit } from '@angular/core';
import Fuse from 'fuse.js';
import { query } from '@angular/animations';
import { FormControl } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import rn from '@angular/common/locales/rn';
import { MemberSearchResult } from '../../../models/models';
import { LoadMemberService } from '../../../load-member.service';

@Injectable()
export class MemberSelectionService implements OnDestroy {
  memberLoadService = inject(LoadMemberService);
  httpClient = inject(HttpClient);

  private unselectedMembers: MemberSearchResult[] = [];
  private displayedMembers: MemberSearchResult[] = [];
  private selectedMembersWithRoles: {
    member: MemberSearchResult;
    role: string;
  }[] = [];


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

  removeMemberFromSelectedMembersWithRoles(
    memberToRemove: MemberSearchResult,
  ) {
    this.selectedMembersWithRoles = this.selectedMembersWithRoles.filter(
      (memberWithRole) =>
        memberWithRole.member.memberId != memberToRemove.memberId,
    );
  }

  addMemberToUnselectedMembers(memberToAdd: MemberSearchResult) {
    this.unselectedMembers = [...this.unselectedMembers, memberToAdd];
  }

  addMemberToDisplayedMembers(memberToAdd: MemberSearchResult) {
    this.displayedMembers = [...this.displayedMembers, memberToAdd];
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

  updateRoleOfSelectedMember(
    memberToBeUpdated: MemberSearchResult,
    newRole: string,
  ) {
    const memberToBeUpdatedWithRole = this.selectedMembersWithRoles.find(
      (memberWithRole) =>
        memberWithRole.member.memberId == memberToBeUpdated.memberId,
    );
    memberToBeUpdatedWithRole!.role = newRole;
  }

  unselected() {
    return this.unselectedMembers.sort(this.memberSortingFunction);
  }

  private memberSortingFunction = (
    member1: MemberSearchResult,
    member2: MemberSearchResult,
  ) => member1.firstName.localeCompare(member2.firstName);

  displayed() {
    return this.displayedMembers.sort(this.memberSortingFunction);
  }

  selectedWithRoles() {
    return this.selectedMembersWithRoles;
  }

  selected() {
    return this.selectedMembersWithRoles.map(
      (memberWithRole) => memberWithRole.member,
    );
  }


  setDisplayed(newDisplayedMembers: MemberSearchResult[]) {
    this.displayedMembers = newDisplayedMembers;
  }

  setUnselected(unselectedMembers: MemberSearchResult[]) {
    this.unselectedMembers = unselectedMembers;
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

  ngOnDestroy(): void {
    console.log('DEBUG: MemberSelectionService destroyed');
  }
}
