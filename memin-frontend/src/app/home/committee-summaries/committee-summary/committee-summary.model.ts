export interface CommitteeSummary {
  id: number;
  name: string;
  nepaliName?: string;
  description: string;
  maxNoOfMeetings: number | null;
  status: 'ACTIVE' | 'INACTIVE';
  createdDate: Date;
  numberOfMeetings: number;
  numberOfMembers: number;
}
