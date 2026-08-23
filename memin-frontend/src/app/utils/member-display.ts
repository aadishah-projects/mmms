export interface BilingualMemberName {
  firstName?: string | null;
  lastName?: string | null;
  firstNameNepali?: string | null;
  lastNameNepali?: string | null;
  title?: string | null;
  titleNepali?: string | null;
}

export function formatMemberDisplayName(
  member: BilingualMemberName,
  language: string | null | undefined = 'ENGLISH',
): string {
  const nepali = language?.toUpperCase() === 'NEPALI';
  const title = nepali
    ? firstNonBlank(member.titleNepali, member.title)
    : firstNonBlank(member.title, member.titleNepali);
  const firstName = nepali
    ? firstNonBlank(member.firstNameNepali, member.firstName)
    : firstNonBlank(member.firstName, member.firstNameNepali);
  const lastName = nepali
    ? firstNonBlank(member.lastNameNepali, member.lastName)
    : firstNonBlank(member.lastName, member.lastNameNepali);

  return [title, firstName, lastName].filter(Boolean).join(' ');
}

function firstNonBlank(preferred?: string | null, fallback?: string | null): string {
  return preferred?.trim() || fallback?.trim() || '';
}
