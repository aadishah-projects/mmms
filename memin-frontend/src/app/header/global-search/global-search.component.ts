import {
  Component,
  effect,
  ElementRef,
  HostListener,
  output,
  viewChild,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, Subscription } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Response } from '../../response/response';
import { GlobalSearchResult } from '../../models/models';
import { BACKEND_URL } from '../../../global_constants';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './global-search.component.html',
  styleUrl: './global-search.component.scss',
})
export class GlobalSearchComponent {
  dialogElementRef = viewChild<ElementRef<HTMLDialogElement>>(
    'global_search_dialog',
  );
  closeDialog = output();

  closeDialogNow() {
    this.closeDialog.emit();
  }

  searchBarFormControl = new FormControl<string>('');
  searchBarSubscription!: Subscription;
  globalSearchResult: GlobalSearchResult | null = null;

  constructor(private http: HttpClient) {
    effect(() => {
      this.dialogElementRef()!.nativeElement.showModal();
    });
    this.setupObservableForSearchBarValueChange();
  }

  setupObservableForSearchBarValueChange() {
    this.searchBarSubscription = this.searchBarFormControl.valueChanges
      .pipe(debounceTime(500))
      .subscribe((response) => {
        const httpParams = new HttpParams().set(
          'input',
          this.searchBarFormControl.value as string,
        );

        if (
          this.searchBarFormControl.value &&
          this.searchBarFormControl.value != ''
        ) {
          this.http
            .get<Response<GlobalSearchResult>>(
              BACKEND_URL + '/api/global-search',
              {
                withCredentials: true,
                params: httpParams,
              },
            )
            .subscribe({
              next: (response) => {
                this.globalSearchResult = response.mainBody;
                console.log(this.globalSearchResult);
              },
              error: (error) => {
                console.log('TODO: show the error properly');
              },
            });
        } else {
	  this.globalSearchResult = null;
	}
      });
  }

  @HostListener('document:keydown.escape', ['$event'])
  onKeydown(event: KeyboardEvent) {
    this.closeDialog.emit();
  }

  // Close when clicking outside the dialog content
  @HostListener('click', ['$event'])
  onDialogClick(event: MouseEvent) {
    const dlg = this.dialogElementRef()!.nativeElement;
    if (event.target === dlg && dlg.open) {
      this.closeDialog.emit();
    }
  }
}
