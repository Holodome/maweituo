import * as api from '$lib/api.js';
import type { PageServerLoad } from './$types';
import type { Feed } from '$lib/types';

export const load = (async ({ locals, url }) => {
  let feed: Feed;
  let pagination = url.searchParams.get('page');
  if (pagination) {
    pagination = `?page=${pagination.toString()}`;
  } else {
    pagination = '';
  }
  if (locals.user) {
    feed = await api.get(`feed/${locals.user?.userId}` + pagination, locals.user?.token);
  } else {
    feed = await api.get(`feed` + pagination);
  }

  const ads = await Promise.all(
    feed.ads.map((id) => api.get(`ads/${id}`, locals.user?.token))
  );

  return {
    ads,
    total: feed.total
  };
}) satisfies PageServerLoad;
