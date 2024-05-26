import { redirect, error } from '@sveltejs/kit';
import * as api from '$lib/api.js';
import type { PageServerLoad, Actions } from './$types';

export const load = (async ({ locals }) => {
  if (!locals.user) throw redirect(302, `/login`);
}) satisfies PageServerLoad;

export const actions = {
  default: async ({ request, locals }) => {
    if (!locals.user) throw error(401);

    const data = await request.formData();
    const result = await api.post(
      'ads',
      {
        title: data.get('title')
      },
      locals.user?.token
    );
    throw redirect(303, `/ads/${result}`);
  }
} satisfies Actions;
