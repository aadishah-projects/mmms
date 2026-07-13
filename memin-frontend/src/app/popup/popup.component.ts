import { Component, ElementRef, ViewChild } from '@angular/core';
import { PopupService } from './popup.service';
import { Popup } from '../models/models';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-popup',
  standalone: true,
  imports: [NgClass],
  templateUrl: './popup.component.html',
  styleUrl: './popup.component.scss',
})
export class PopupComponent {
@ViewChild('myPopup') popupElement!: ElementRef;
  startPopupDisappearingAnimation = false;
  popup: Popup = {
    message:"",
    type:"Success",
    displayTime: 0,
  };
  constructor(private popupService: PopupService) {
    this.popupService.currentMessage$.subscribe({
      next: (value) => {
        if (value != null) {
	  this.startPopupDisappearingAnimation = false;
          this.popup = value;
this.popupElement.nativeElement.showPopover();
          setTimeout(() => {this.popupElement.nativeElement.hidePopover()}, this.popup.displayTime + 300 /*300ms for fade out animation*/);

          setTimeout(() => {this.startPopupDisappearingAnimation = true}, this.popup.displayTime);
        }
      },
    });
  }
}
