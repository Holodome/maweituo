import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals }) {
  let feed;
  if (locals.user) {
    feed = api.get(`feed/${locals.user?.userId}`, locals.user?.token);
  } else {
    feed = api.get(`feed`);
  }

  const ads = await feed
    .then((ids) =>
      Promise.all(ids.map((id) => api.get(`ads/${id}`, locals.user?.token)))
    );

  return {
    ads
  };
}
