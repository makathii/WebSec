import { CaptchaChallenge, CaptchaSolution } from './models';

const BATCH_SIZE = 500;

export async function solveCaptcha(challenge: CaptchaChallenge): Promise<CaptchaSolution> {
  const encoder = new TextEncoder();
  for (let start = 0; start <= challenge.maxNumber; start += BATCH_SIZE) {
    const end = Math.min(start + BATCH_SIZE, challenge.maxNumber + 1);
    const numbers = Array.from({ length: end - start }, (_, i) => start + i);
    const hashes = await Promise.all(
      numbers.map((n) => crypto.subtle.digest('SHA-256', encoder.encode(challenge.salt + n))),
    );
    const index = hashes.findIndex((hash) => toHex(hash) === challenge.challenge);
    if (index >= 0) {
      return {
        challenge: challenge.challenge,
        salt: challenge.salt,
        number: numbers[index],
        signature: challenge.signature,
      };
    }
  }
  throw new Error('Captcha could not be solved');
}

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer), (b) => b.toString(16).padStart(2, '0')).join('');
}
