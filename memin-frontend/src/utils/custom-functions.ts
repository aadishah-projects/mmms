import NepaliDate from "nepali-date-converter";

export function toNepaliDigits(input: string | null) {

  if(input === null) {
    return null;
  }
  const nepaliDigits = ['०','१','२','३','४','५','६','७','८','९'];
  const result: string[] = [];

  for (let ch of input) {
      if (/\d/.test(ch)) {
          result.push(nepaliDigits[Number(ch)]);
      } else {
          result.push(ch); // keep dashes, colons, etc.
      }
  }
  return result.join('');
}


  export function getNepaliDate(dateStr: string) {
    const nepaliDate = NepaliDate.fromAD(new Date(dateStr));;
    return toNepaliDigits(nepaliDate.format('YYYY/MM/DD'));
  }

