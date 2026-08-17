import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { ErrorComponent } from './error/error.component';
import { HomeComponent } from './home/home.component';
import { committeeRouteGuard, isAuthenticated, isNotAuthenticated, meetingRouteGuard, memberRouteGuard, hasWriteAccessGuard, deptHeadGuard } from './app.guards';
import { CommitteeSummariesComponent } from './home/committee-summaries/committee-summaries.component';
import { SearchBarComponent } from './search-bar/search-bar.component';
import { CreateCommitteeComponent } from './home/create-committee/create-committee.component';
import { CreateMemberComponent } from './home/create-member/create-member.component';
import { CommitteeOverviewComponent } from './committee-details/committee-overview/committee-overview.component';
import { CommitteeDetailsComponent } from './committee-details/committee-details.component';
import { MinuteComponent } from './committee-details/minute/minute.component';
import { CreateMeetingComponent } from './home/create-meeting/create-meeting.component';
import { EditCommitteeComponent } from './committee-details/edit-committee/edit-committee.component';
import { MemberSummariesComponent } from './home/member-summaries/member-summaries.component';
import { ViewArchivesComponent } from './home/view-archives/view-archives.component';
import { CommitteeExtendedSummaryComponent } from './committee-details/committee-extended-summary/committee-extended-summary.component';
import { EditMemberComponent } from './home/member-summaries/edit-member/edit-member.component';
import { EditMeetingComponent } from './committee-details/committee-overview/meeting-summaries/edit-meeting/edit-meeting.component';
import { DocumentationComponent } from './documentation/documentation.component';
import { ManageUsersComponent } from './home/manage-users/manage-users.component';
import { RegisterComponent } from './register/register.component';
import { MinuteTemplateComponent } from './committee-details/minute-template/minute-template.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home/my-committees',
    pathMatch: 'full',
  },
  {
    path: 'login',
    component: LoginComponent,
    canMatch: [isNotAuthenticated],
  },
  {
    path: 'register',
    component: RegisterComponent,
    canMatch: [isNotAuthenticated],
  },
  {
    path: 'documentation',
    component: DocumentationComponent,
  },

  {
    path: 'home',
    component: HomeComponent,
    canMatch: [isAuthenticated],
    children: [
      {
        path: 'my-committees',
        component: CommitteeSummariesComponent,
      },
      {
        path: 'create-committee',
        component: CreateCommitteeComponent,
        canActivate: [deptHeadGuard]
      },
      {
        path: 'create-member',
        component: CreateMemberComponent,
        canActivate: [deptHeadGuard]
      },
      {
        path: 'create-meeting',
        component: CreateMeetingComponent,
        canActivate: [hasWriteAccessGuard]
      },
      {
	// it is not a children to members-list because children's require <router-outlet> and it is an independent page to members list 
	canActivate: [memberRouteGuard, deptHeadGuard],
	path: 'members-list/edit',
	component: EditMemberComponent
      },
      {
        path: 'members-list',
        component: MemberSummariesComponent,
      },
      {
        path: 'archives',
        component: ViewArchivesComponent,
      },
      {
        path: 'manage-users',
        component: ManageUsersComponent,
        canActivate: [deptHeadGuard],
      },
      {
        path: '**',
        component: ErrorComponent,
      },
    ],
  },
  {
    path: 'committee-details/overview/meeting/edit',
    component: EditMeetingComponent,
    canMatch: [isAuthenticated],
    canActivate: [meetingRouteGuard, committeeRouteGuard, hasWriteAccessGuard]
  },

  {
    path: 'committee-details',
    component: CommitteeDetailsComponent,
    canMatch: [isAuthenticated],
    children: [
      {
        //TODO: only open this when there is a meetingId
        path: 'overview/minute',
        component: MinuteComponent,
      },
      {
        canActivate: [committeeRouteGuard],
        path: 'overview',
        component: CommitteeOverviewComponent,
      },
      {
        canActivate: [committeeRouteGuard, hasWriteAccessGuard],
        path: 'edit',
        component: EditCommitteeComponent,
      },
      {
        canActivate: [committeeRouteGuard, hasWriteAccessGuard],
        path: 'template',
        component: MinuteTemplateComponent,
      },
      {
        canActivate: [committeeRouteGuard],
        path: 'summary',
        component: CommitteeExtendedSummaryComponent,
      },
      {
        path: '**',
        component: ErrorComponent,
      },
    ],
  },

  {
    path: 'search',
    component: SearchBarComponent,
  },

  {
    path: '**',
    component: ErrorComponent,
  },
];
