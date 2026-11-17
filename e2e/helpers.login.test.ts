import {describe, expect, it, vi} from 'vitest';
import type {Page} from '@playwright/test';
import {login, readLoginOutcome} from './helpers';

/**
 * Covers the sign-in helper's handling of a Keycloak that threw the authentication session away.
 *
 * That state used to be invisible to the helper: it kept polling a page that would never change
 * and failed on its timeout, which read as a slow environment rather than as a rejection. The
 * cases below are the three the CI flake came down to.
 */
describe('login', () => {

  /**
   * A stand-in for the browser page, driven by what each screen should look like.
   * Every screen says which of the three markers is visible, and the page moves on to the next
   * screen when the sign-in button is clicked.
   */
  function fakePage(screens: Array<'denied' | 'profile' | 'expired' | 'pending'>, initial = 'pending') {
    const filled: Record<string, string> = {};
    const signInClicks: number[] = [];
    // What the page shows right now. Each submission of the sign-in form replaces it with the
    // next screen Keycloak is supposed to answer with.
    let current: string = initial;

    const visible = (marker: string) => ({
      first: () => visible(marker),
      isVisible: async () => current === marker,
      fill: async (value: string) => {
        filled[marker] = value;
      },
      click: async () => {
        if (marker === 'signIn') {
          current = screens[Math.min(signInClicks.length, screens.length - 1)];
          signInClicks.push(signInClicks.length);
        }
      },
    });

    const page = {
      goto: vi.fn(async () => undefined),
      url: () => 'https://mish/auth/realms/mock-realm/login-actions/authenticate',
      waitForTimeout: vi.fn(async () => undefined),
      getByRole: (role: string, options?: {name?: string | RegExp}) => {
        const name = String(options?.name ?? '');
        if (role === 'heading') return visible('denied');
        if (role === 'menuitem') return visible('profile');
        if (role === 'textbox') return visible(name.includes('Password') ? 'password' : 'username');
        if (role === 'button' && /sign in/i.test(name)) return visible('signIn');
        return visible('otherButton');
      },
      getByText: () => visible('expired'),
    };

    return {page: page as unknown as Page, signInClicks, filled};
  }

  it('signs in when Keycloak accepts the first attempt', async () => {
    const {page, signInClicks} = fakePage(['profile']);

    await login(page, 'alice', 'password');

    expect(signInClicks).toHaveLength(1);
  });

  it('submits once more when Keycloak discarded the login attempt', async () => {
    // The screen the flake left behind: the sign-in form again, carrying "login attempt timed
    // out". It never changes on its own, so waiting longer is not what gets past it.
    const {page, signInClicks} = fakePage(['expired', 'profile']);

    await login(page, 'alice', 'password');

    expect(signInClicks).toHaveLength(2);
  });

  it('gives up with Keycloak\'s own reason rather than a bare timeout', async () => {
    const {page, signInClicks} = fakePage(['expired', 'expired']);

    await expect(login(page, 'alice', 'password')).rejects.toThrow(/discarded the login attempt twice/i);

    // Twice, not forever: a Keycloak that is still restarting is not worth another round.
    expect(signInClicks).toHaveLength(2);
  });

  it('reports an access-denied screen as a finished login', async () => {
    // The role-guard specs sign in as a student on purpose and expect to be turned away.
    const {page, signInClicks} = fakePage(['denied']);

    await login(page, 'bart', 'password');

    expect(signInClicks).toHaveLength(1);
  });

  it('reads the signed-in shell as profile and the stale form as expired', async () => {
    expect(await readLoginOutcome(fakePage([], 'profile').page)).toBe('profile');
    expect(await readLoginOutcome(fakePage([], 'denied').page)).toBe('denied');
    expect(await readLoginOutcome(fakePage([], 'expired').page)).toBe('expired');
    expect(await readLoginOutcome(fakePage([], 'pending').page)).toBe('pending');
  });
});
