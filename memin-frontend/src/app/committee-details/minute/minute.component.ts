import { Component } from '@angular/core';
import { MinuteViewComponent } from './minute-view/minute-view.component';
import { MinuteEditComponent } from './minute-edit/minute-edit.component';
import { MinuteDataService } from './minute-data.service';

@Component({
  selector: 'app-minute',
  standalone: true,
  imports: [MinuteViewComponent, MinuteEditComponent],
  templateUrl: './minute.component.html',
  styleUrl: './minute.component.scss',
  providers: [MinuteDataService, MinuteViewComponent],
})
export class MinuteComponent {
  minuteDataService: MinuteDataService;
  constructor(minuteDataService: MinuteDataService) {
    this.minuteDataService = minuteDataService;
  }
}

