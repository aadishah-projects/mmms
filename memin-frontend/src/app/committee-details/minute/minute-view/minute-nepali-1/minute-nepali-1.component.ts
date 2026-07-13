import { Component, ElementRef, inject, input, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MinuteDataDto } from '../../../../models/models';
import { MinuteDataService } from '../../minute-data.service';
import { toNepaliDigits } from '../../../../../utils/custom-functions';
import NepaliDate from 'nepali-date-converter';

@Component({
  selector: 'app-minute-nepali-1',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './minute-nepali-1.component.html',
  styleUrl: './minute-nepali-1.component.scss',
  providers: [DatePipe],
})
export class MinuteNepali1Component {
  minuteData = input.required<MinuteDataDto>();
  isEditable = input<boolean>(false);
  constructor(private datePipe: DatePipe) {}
  processedMinute = viewChild<ElementRef<HTMLDivElement>>('processedMinute');

  
  // to use it in the template
  toNepaliDigits = toNepaliDigits;

  getNepaliDate(dateStr: string | number) {
    return toNepaliDigits(this.datePipe.transform(dateStr, 'yyyy/MM/dd') || '');
  }

  getDay(dateStr: string) {
    const date = new Date(dateStr);
    const daysInNepali = [
      'आइतबार',
      'सोमबार',
      'मङ्गलबार',
      'बुधबार',
      'बिहिबार',
      'शुक्रबार',
      'शनिबार',
    ];
    return daysInNepali[date.getDay()];
  }

  toNepaliDate(dateStr: string) {
    try {
      const nepaliDate = NepaliDate.fromAD(new Date(dateStr));
      return toNepaliDigits(nepaliDate.format('YYYY/MM/DD'));
    } catch {
      return 'Invalid Date';
    }
  }
}
