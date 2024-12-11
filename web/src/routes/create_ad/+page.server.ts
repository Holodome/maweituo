import { redirect, error } from '@sveltejs/kit';
import { api } from '$lib/api';
import type { PageServerLoad, Actions } from './$types';

export const load = (async ({ locals }) => {
  if (!locals.user) throw redirect(302, `/login`);
}) satisfies PageServerLoad;

export const actions = {
  default: async ({ request, locals }) => {
    if (!locals.user) throw error(401);

    const data = await request.formData();
    const result = await api.createAd(
      {
        title: data.get('title') as string
      },
      locals.user?.token
    ).then(x => x.id);
    throw redirect(303, `/ads/${result}`);
  }
} satisfies Actions;
