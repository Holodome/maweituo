import type { FeedResponseDTO } from '$lib/api';
import * as api from '$lib/http';
import { api as appApi } from '$lib/api';
import type { PageServerLoad } from './$types';

export const load = (async ({ locals, url }) => {
  let feed: FeedResponseDTO;
  let pagination = url.searchParams.get('page');
  if (pagination) {
    pagination = `?page=${pagination.toString()}`;
  } else {
    pagination = '?page=0';
  }
  if (locals.user) {
    feed = await api.get(`feed/${locals.user?.userId}` + pagination, locals.user?.token) as FeedResponseDTO;
  } else {
    feed = await api.get(`feed` + pagination) as FeedResponseDTO;
  }

  const ads = await Promise.all(
    feed.feed.items.map((id) => appApi.getAd(id))
  );

  return {
    ads,
    total: feed.feed.totalItems
  };
}) satisfies PageServerLoad;
