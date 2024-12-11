import { redirect } from '@sveltejs/kit';
import type { PageServerLoad, Actions } from './$types';
import { api } from '$lib/api';

export const load = (async ({ locals }) => {
  if (locals.user) throw redirect(307, '/');
}) satisfies PageServerLoad;

export const actions = {
  default: async ({ cookies, request }) => {
    const data = await request.formData();

    const response = await api.login({
      name: data.get('name') as string,
      password: data.get('password') as string
    });

    const jwt = response.jwt.access_token;
    cookies.set('jwt', jwt, { path: '/' });
    redirect(307, '/');
  }
} satisfies Actions;
