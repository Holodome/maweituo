import { api, type UserId, type UserPublicInfoDto, type AdResponseDto } from '$lib/api';
import { redirect } from '@sveltejs/kit';
import type { PageServerLoad, Actions } from './$types';

export const load = (async ({ params }) => {
  const userId = params.user as UserId;
  const userInfo: UserPublicInfoDto = await api.getUser(userId);
  const ads: AdResponseDto[] = await api.getUserAds(userId)
    .then((resp) => {
      return Promise.all(resp.ads.map((id) => api.getAd(id)))
    });
  return {
    userInfo,
    ads
  };
}) satisfies PageServerLoad;

export const actions = {
  logout: async ({ locals, cookies }) => {
    await api.logout(locals.user?.token);
    cookies.delete('jwt', { path: '/' });
    throw redirect(307, '/');
  }
} satisfies Actions;
