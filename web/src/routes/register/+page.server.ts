import { redirect } from '@sveltejs/kit';
import { api } from '$lib/api';
import type { PageServerLoad, Actions } from './$types';

export const load = (async ({ parent }) => {
  const { user } = await parent();
  if (user) throw redirect(307, '/');
}) satisfies PageServerLoad;

export const actions = {
  default: async ({ request }) => {
    const data = await request.formData();

    await api.register({
      name: data.get('name') as string,
      email: data.get('email') as string,
      password: data.get('password') as string
    });

    throw redirect(307, '/login');
  }
} satisfies Actions;
