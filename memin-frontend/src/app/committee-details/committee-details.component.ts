import { Component} from '@angular/core';
import {  RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-committee-details',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './committee-details.component.html',
  styleUrl: './committee-details.component.scss',
})
export class CommitteeDetailsComponent {
}
