import { fail, redirect } from '@sveltejs/kit';
import * as api from '$lib/api.js';
import type { PageServerLoad, Actions } from './$types';

export const load = (async ({ parent }) => {
  const { user } = await parent();
  if (user) throw redirect(307, '/');
}) satisfies PageServerLoad;

export const actions = {
  default: async ({ request }) => {
    const data = await request.formData();

    const body = await api.post(
      'register',
      {
        name: data.get('name'),
        email: data.get('email'),
        password: data.get('password')
      }
    );
    if (body.errors) {
      return fail(401, body);
    }

    throw redirect(307, '/login');
  }
} satisfies Actions;
