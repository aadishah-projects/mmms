import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Popup } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class PopupService {
  private messageSource = new BehaviorSubject<Popup | null>(null);
  currentMessage$ = this.messageSource.asObservable();

  showPopup(message: string, type: "Error" | "Success" = "Success", displayTimeInMs = 2000) {
    this.messageSource.next({
      message: message,
      type: type,
      displayTime: displayTimeInMs,
    })
  }
}
