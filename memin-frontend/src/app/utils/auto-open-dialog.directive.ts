import { Directive, ElementRef, AfterViewInit } from '@angular/core';

@Directive({
  selector: 'dialog[appAutoOpen]', // Selects <dialog> elements with this attribute
  standalone: true
})
export class AutoOpenDialogDirective implements AfterViewInit {
  constructor(private el: ElementRef<HTMLDialogElement>) {}

  ngAfterViewInit() {
    // Native HTML method to show the dialog as a modal
    this.el.nativeElement.showModal();
  }
}
