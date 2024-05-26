import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';

export const load = (async ({ parent }) => {
  const { user } = await parent();
  throw redirect(307, user ? `/account/${user.userId}` : '/login');
}) satisfies PageLoad;
