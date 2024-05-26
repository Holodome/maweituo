import { fail, redirect } from '@sveltejs/kit';
import * as api from '$lib/api.js';
import type { PageServerLoad, Actions } from './$types';

export const load = (async ({ locals }) => {
  if (locals.user) throw redirect(307, '/');
}) satisfies PageServerLoad;

export const actions = {
  default: async ({ cookies, request }) => {
    const data = await request.formData();

    const body = await api.post(
      'login',
      {
        name: data.get('name'),
        password: data.get('password')
      },
    );

    if (body.errors) {
      return fail(401, body);
    }

    const jwt = body.access_token;
    cookies.set('jwt', jwt, { path: '/' });
    redirect(307, '/');
  }
} satisfies Actions;
