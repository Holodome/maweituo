import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals }) {
  if (locals.user) {
    const adInfo = await api.get(`feed/${locals.user?.userId}`, locals.user?.token);
    return {
      adInfo
    };
  } else {
    const adInfo = await api.get(`feed`);
    return {
      adInfo
    };
  }
}
