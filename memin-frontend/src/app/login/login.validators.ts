import { AbstractControl, ValidationErrors, Validator } from '@angular/forms';

export function validateUsernameFormat(
  control: AbstractControl,
): ValidationErrors | null {
  const specialSymbolRegex = /[!@#$%^&*(),.?":{}|<>\\\/\[\];'`~\-=+ ]/;
  const enteredUsername = control.value;
  const result = specialSymbolRegex.test(enteredUsername);
  if (result) {
    return {
      usernameImproperFormat: 'username is not in proper format',
    };
  } else {
    return null;
  }
}
