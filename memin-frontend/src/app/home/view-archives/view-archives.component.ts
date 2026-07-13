import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { BACKEND_URL } from '../../../global_constants';
import { CommitteeSummary } from '../committee-summaries/committee-summary/committee-summary.model';
import { CommitteeSummaryComponent } from '../committee-summaries/committee-summary/committee-summary.component';
import { Response } from '../../response/response'
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-view-archives',
  standalone: true,
  imports: [CommitteeSummaryComponent, RouterLink],
  templateUrl: './view-archives.component.html',
  styleUrl: './view-archives.component.scss'
})
export class ViewArchivesComponent {
  committeeSummaries!: CommitteeSummary[];
  constructor(private httpClient: HttpClient) {}
  hasCommitteesLoaded=false;

  ngOnInit(): void {
    this.httpClient
      .get<Response<CommitteeSummary[]>>(BACKEND_URL + '/api/my-inactive-committees', {
        withCredentials: true,
      })
      .subscribe({
        next: (response) => {
          this.committeeSummaries = response.mainBody;
	  this.hasCommitteesLoaded = true;
        },
        error: (response) => {
          console.log('error fetching committee');
        },
      });
  }
}
