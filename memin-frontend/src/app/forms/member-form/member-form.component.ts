import {
  AfterViewInit,
  Component,
  ElementRef,
  input,
  OnInit,
  output,
  viewChild,
} from '@angular/core';
import { DragDropModule } from '@angular/cdk/drag-drop';
import {
  ReactiveFormsModule,
  FormControl,
  Validators,
  FormGroup,
} from '@angular/forms';
import { MemberCreationDto, MemberFormData } from '../../models/models';
import { SafeCloseDialogCustom } from '../../utils/safe-close-dialog-custom.directive';

@Component({
  selector: 'app-member-form',
  standalone: true,
  imports: [ReactiveFormsModule, SafeCloseDialogCustom, DragDropModule],
  templateUrl: './member-form.component.html',
  styleUrl: './member-form.component.scss',
})
export class MemberFormComponent implements AfterViewInit, OnInit {
  //outputs
  formSaveEvent = output<MemberCreationDto>();

  //inputs
  isEditPage = input.required<boolean>();
  memberFormData = input.required<MemberFormData>();

  FORM_NAME = 'create_member_form';
  diag = viewChild<ElementRef<HTMLDialogElement>>('create_member_dialog');

  //setting aliases for this.memberFormGroup().controls
  firstName!: FormControl<string>;
  lastName!: FormControl<string>;
  post!: FormControl<string>;
  title!: FormControl<string>;
  institution!: FormControl<string>;

  memberFormGroup!: FormGroup<{
    firstName: FormControl<string>;
    lastName: FormControl<string>;
    post: FormControl<string>;
    title: FormControl<string>;
    institution: FormControl<string>;
  }>;

  ngOnInit(): void {
    this.firstName = new FormControl(this.memberFormData().firstName,{ nonNullable: true, validators:  [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(50),
    ]});

    this.lastName = new FormControl(this.memberFormData().lastName,{nonNullable: true, validators:  [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(50),
    ]},);

    this.post = new FormControl(this.memberFormData().post,{nonNullable: true} );
    this.title = new FormControl(this.memberFormData().title, {nonNullable: true, validators: [
      Validators.required,
    ]});


    this.institution = new FormControl(this.memberFormData().institution, {nonNullable: true });

    
    this.memberFormGroup = new FormGroup({
      firstName: this.firstName,
      lastName: this.lastName,
      post: this.post,
      title: this.title,
      institution: this.institution,
    });
  }

  ngAfterViewInit(): void {
    // Show the dialog
    if (this.diag() && !this.diag()!.nativeElement.open) {
      this.diag()!.nativeElement.showModal();
    }
  }

  showAllErrors = false;

  onFormSave($event: Event) {
    if(this.memberFormGroup.invalid) {
      this.showAllErrors = true;
      return;
    }
    $event.preventDefault();
    const memberCreationDto = new MemberCreationDto();
    memberCreationDto.firstName = this.firstName.value!;
    memberCreationDto.lastName = this.lastName.value!;
    memberCreationDto.post = this.post.value!;
    memberCreationDto.title = this.title.value!;
    memberCreationDto.institution = this.institution.value!;
    localStorage.removeItem(this.FORM_NAME);
    this.formSaveEvent.emit(memberCreationDto);
    this.diag()!.nativeElement.close();
    console.log("saving form", memberCreationDto);
  }

  saveFormData = () => {
    if (this.isEditPage()) return;

    const formValue = this.memberFormGroup.getRawValue();

    // Check if at least one field has some value
    const hasData = Object.values(formValue).some(
      (value) => value !== null && value !== undefined && value !== '',
    );

    if (!hasData) {
      return;
    }

    localStorage.setItem(this.FORM_NAME, JSON.stringify(formValue));
  };

  restoreFormData = () => {
    if (this.isEditPage()) return;

    //restore form normally ie restores the FormGroup
    const savedData = localStorage.getItem(this.FORM_NAME);
    if (savedData) {
      console.log('Found the saved Data');
      console.log(savedData);
      try {
        const parsedData = JSON.parse(savedData);
        this.memberFormGroup.patchValue(parsedData); // prefill the form
      } catch (err) {
        console.error('Failed to parse saved form data', err);
      }
    }
  };
}
