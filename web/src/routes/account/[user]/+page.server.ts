import * as api from '$lib/api.js';
import { redirect } from '@sveltejs/kit';
import type { PageServerLoad, Actions } from './$types';
import type { Advertisement, User } from "$lib/types";

export const load = (async ({ locals, params }) => {
  const userInfo: User = await api.get(`users/${params.user}`, locals.user?.token);
  const ads: Advertisement[] = await api.get(`users/${params.user}/ads`, locals.user?.token)
    .then((ids: string[]) =>
      Promise.all(ids.map((id) => api.get(`ads/${id}`, locals.user?.token)))
    );
  return {
    userInfo,
    ads
  };
}) satisfies PageServerLoad;

export const actions = {
  logout: async ({ locals, cookies }) => {
    await api.post('logout', {}, locals.user?.token);
    cookies.delete('jwt', { path: '/' });
    throw redirect(307, '/');
  }
} satisfies Actions;
