export interface CommitteeSummary {
  id: number;
  name: string;
  description: string;
  maxNoOfMeetings: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdDate: Date;
  numberOfMeetings: number;
  numberOfMembers: number;
}
