// See https://kit.svelte.dev/docs/types#app
// for information about these interfaces
import type { Token, UserId } from '$lib/dto';

declare global {
  namespace App {
    // interface Error {}
    interface Locals {
      user?: {
        token: Token;
        userId: UserId;
      };
    }
    // interface PageData {}
    // interface PageState {}
    // interface Platform {}
  }
}

export { };
