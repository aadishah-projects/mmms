export interface MemberSearchResult {
  memberId: number;
  firstName: string;
  lastName: string;
  firstNameNepali?: string;
  lastNameNepali?: string;
  title?: string;
  titleNepali?: string;
}

export interface MemberDetails {
  memberId: number;
  firstName: string;
  lastName: string;
  firstNameNepali: string;
  lastNameNepali: string;
  post: string;
  title: string;
  titleNepali: string;
  institution: string;
  email: string;
}

export class CommitteeCreationDto {
  name: string = '';
  description: string = '';
  status: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';
  maximumNumberOfMeetings?: number = undefined;
  members: MemberIdAndRole[] = [];
  coordinatorId: number = 0;
  minuteLanguage: string = '';
}

export class MemberIdAndRole {
  memberId!: number;
  role!: string;
}

export class CommitteeDetailsDto {
  id: number = 0;
  name: string = '';
  description: string = '';
  createdDate: string = '';
  status: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';
  maxNoOfMeetings?: number = undefined;
  meetings: MeetingSummaryDto[] = [];
  members: MemberDetailsDto[] = [];
}


export class MeetingSummaryDto {
  id: number = 0;
  title: string = '';
  heldDate: string = '';
  heldTime: number[] = []; //HH:mm:ss
  heldPlace: string = '';
  createdDate: string = '';
  agendas: string[] = [];
}

export class CommitteeOverviewDto {
  name: string = '';
  description: string = '';
  createdDate: string = '';
  memberCount: number = 0;
  meetingCount: number = 0;
  decisionCount: number = 0;
  coordinatorName: string = '';
  coordinatorId: number | null = null;
  chairmanCandidates: MemberSearchResult[] = [];
  secretaryName: string | null = null;
  secretaryId: number | null = null;
  firstMeetingDate: string = '';
  lastMeetingDate: string = '';
  meetingDates: string[] = [];
  meetings: DateAndMeetingIdsDto[] = [];
  language: string = '';
}

export class DateAndMeetingIdsDto {
  meetingDate: string = '';
  meetings: MeetingSummaryDto[] = [];
}

export class MemberOfCommitteeDto {
  id: number = 0;
  name: string = '';
  role: string = '';
}

export class MinuteDataDto {
  minuteLanguage: string = '';
  meetingNumber: string = '';
  meetingHeldDateNepali: string = '';
  meetingHeldDate: string = '';
  meetingHeldDay: string = '';
  partOfDay: string = '';
  meetingHeldTime: string = '';
  meetingHeldPlace: string = '';
  committeeName: string = '';
  committeeDescription: string = '';
  coordinatorFullName: string = '';
  chairmanFullName: string = '';
  openingParagraph: string | null = null;
  header: string | null = null;
  minuteContentHtml: string | null = null;
  decisions: DecisionDto[] = [];
  agendas: AgendaDto[] = [];
  participants: CommitteeMembershipDto[] = [];
}

export interface MinuteTemplateDto {
  committeeId: number;
  committeeName: string;
  committeeDescription: string;
  minuteLanguage: string;
  minuteTemplateHtml: string | null;
  minuteOpeningTemplate: string | null;
  minuteHeaderTemplate: string | null;
  activeTemplateId: number | null;
  savedTemplates: MinuteTemplateSummaryDto[];
}

export interface MinuteTemplateUpdateDto {
  templateId?: number | null;
  name?: string | null;
  minuteTemplateHtml: string | null;
}

export interface AiStructuredMinuteDto {
  agendas: AgendaDto[];
  decisions: DecisionDto[];
  htmlContent: string | null;
  usedCommitteeTemplate: boolean;
}

export class DecisionDto {
  decisionId: number = 0;
  decision: string = '';
}
export class AgendaDto {
  agendaId: number = 0;
  agenda: string = '';
}


export class DecisionWithMeetingId {
  meetingId: number = 0;
  decision: string = '';
}
export class AgendaWithMeetingId {
  meetingId: number = 0;
  agenda: string = '';
}


export class CommitteeMembershipDto {
  memberId: number = 0;
  fullName: string = '';
  role: string = '';
}

export interface MinuteTemplateSummaryDto {
  templateId: number;
  name: string;
  minuteTemplateHtml: string;
  minuteLanguage: string | null;
  active: boolean;
}

export class MemberDetailsDto {
  memberId: number = 0; 
  firstName: string = '';
  lastName: string = '';
  firstNameNepali: string = '';
  lastNameNepali: string = '';
  title: string = '';
  titleNepali: string = '';
  post: string = '';
  institution: string = '';
  email: string = '';
}

export class MemberCreationDto {
  firstName: string = '';
  lastName: string = '';
  firstNameNepali: string = '';
  lastNameNepali: string = '';
  title: string = '';
  titleNepali: string = '';
  post: string = '';
  institution: string = '';
  email: string = '';
}

export class MeetingCreationDto {
  committeeId: number = 0;
  chairmanId: number | null = null;
  title: string = '';
  heldDate: string = '';
  heldTime: string = '';
  heldPlace: string = '';
  inviteeIds: number[] = [];
  decisions: DecisionDto[] = [];
  agendas: AgendaDto[] = [];
}

export class CommitteeDetailsForEditDto {
  id: number = 0;
  name: string = '';
  description: string = '';
  status: 'ACTIVE' | 'INACTIVE' = 'ACTIVE';
  maxNoOfMeetings?: number = undefined;
  minuteLanguage: 'NEPALI' | 'ENGLISH' | null = null;
  minuteOpeningTemplate: string | null = null;
  minuteHeaderTemplate: string | null = null;
  minuteTemplateHtml: string | null = null;
  coordinator: MemberDetails = {
    memberId: 0,
    firstName: '',
    lastName: '',
    firstNameNepali: '',
    lastNameNepali: '',
    post: '',
    title:'',
    titleNepali: '',
    institution: '',
    email: '',
  };
  membersWithRoles: MemberDetailsWithRole[] = [];
}

export class MemberDetailsWithRole {
  member: MemberDetails = {
    memberId: 0,
    firstName: '',
    lastName: '',
    firstNameNepali: '',
    lastNameNepali: '',
    post: '',
    title: '',
    titleNepali: '',
    institution: '',
    email: '',
  };
  role: string = '';
}

export class MinuteUpdateDto {
  committeeName!: string;
  committeeDescription!: string;
  meetingHeldDate!: string;
  meetingHeldTime!: string;
  meetingHeldPlace!: string;
  decisions!: DecisionDto[];
  agendas!: AgendaDto[];
  htmlContent?: string;
}

export class GlobalSearchResult {
  committees!:CommitteeIdAndName[];
  members!: MemberSearchResult[];
  decisions!: DecisionWithMeetingId[];
  agendas!: AgendaWithMeetingId[];
}

export class CommitteeIdAndName {
  committeeId!: number;
  committeeName!: string;
}

export class CommitteeExtendedSummary {
  committeeId!: number;
  name!: string;
  description!: string
  language!: "NEPALI" | "ENGLISH" | null;
  meetings!: MeetingExtendedSummary[];
}

export class MeetingExtendedSummary {
  meetingId!: number;
  meetingHeldDate!: string
  meetingHeldPlace!: string;
  meetingHeldTime!: string
  decisions!: string[];
  agendas!: string[]
  inviteeNames!: string[];
}


//these models are used in committee-form, member-form components
export interface CommitteeFormData {
  name: string;
  description: string;
  coordinator: MemberSearchResult;
  status: 'ACTIVE' | 'INACTIVE';
  maxNoOfMeetings: number;
  minuteLanguage: 'NEPALI' | 'ENGLISH' | null;
  selectedMembersWithRoles:{member: MemberSearchResult;
    role: string;
  }[];
  unselectedMembers: MemberSearchResult[];
}


export interface MemberFormData {
  firstName: string;
  lastName: string;
  firstNameNepali: string;
  lastNameNepali: string;
  post: string;
  title: string;
  titleNepali: string;
  institution: string;
  email: string;
}

export interface MeetingFormData {
  meetingNumber?: number;
  title: string;
  committeeName: string; //for edit page
  heldDate: string;
  heldTime: number[];
  heldPlace: string;
  decisions: DecisionDto[];
  agendas: AgendaDto [];
  possibleInvitees: MemberSearchResult[];
  selectedInvitees: MemberSearchResult[];
  chairman: MemberSearchResult | null;
}

export interface MeetingDetailsForEdit {
  meetingId: number;
  committeeId: number;
  committeeName: string;
  title: string;
  heldDate: string;
  heldTime: number[];
  heldPlace: string;
  selectedInvitees: MemberSearchResult[];
  possibleInvitees: MemberSearchResult[];
  chairman: MemberSearchResult;
  meetingNumber: number;
  decisions: DecisionDto[];
  agendas: AgendaDto[];
}

export interface Popup {
  message: string;
  type: "Error" | "Success";
  displayTime: number;
}

