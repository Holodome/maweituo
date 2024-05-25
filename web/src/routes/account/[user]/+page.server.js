import * as api from '$lib/api.js';
import { fail, redirect } from '@sveltejs/kit';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals, params }) {
  const userInfo = await api.get(`users/${params.user}`, locals.user?.token);
  const ads = await api.get(`users/${params.user}/ads`, locals.user?.token)
    .then((ids) =>
      Promise.all(ids.map((id) => api.get(`ads/${id}`, locals.user?.token)))
    );
  return {
    userInfo,
    ads
  };
}

/** @type {import('./$types').Actions} */
export const actions = {
  logout: async ({ locals, cookies }) => {
    await api.post('logout', {}, locals.user?.token);
    cookies.delete('jwt', { path: '/' });
    throw redirect(307, '/');
  }
};
