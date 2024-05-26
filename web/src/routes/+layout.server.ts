import type { LayoutServerLoad } from './$types';

export const load = (async ({ locals }) => {
  return {
    user: locals.user && {
      userId: locals.user.userId
    }
  };
}) satisfies LayoutServerLoad;
