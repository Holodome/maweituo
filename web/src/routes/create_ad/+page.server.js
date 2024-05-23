import { redirect, fail } from '@sveltejs/kit';
import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals }) {
  if (!locals.user) throw redirect(302, `/login`);
}

/** @type {import('./$types').Actions} */
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
};
