import { CaptchaChallenge, CaptchaSolution } from './models';
import { sha256Hex } from './sha256';

const BATCH_SIZE = 2000;

export async function solveCaptcha(challenge: CaptchaChallenge): Promise<CaptchaSolution> {
  for (let start = 0; start <= challenge.maxNumber; start += BATCH_SIZE) {
    const end = Math.min(start + BATCH_SIZE, challenge.maxNumber + 1);
    for (let n = start; n < end; n++) {
      if (sha256Hex(challenge.salt + n) === challenge.challenge) {
        return { challenge: challenge.challenge, salt: challenge.salt, number: n, signature: challenge.signature };
      }
    }
    await new Promise((resolve) => setTimeout(resolve));
  }
  throw new Error('Captcha could not be solved');
}
