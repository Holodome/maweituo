import { buildImageUrl } from "$lib/http";
import { api, type AdId, type ImageId } from '$lib/api';
import { fail, redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';

export const load = (async ({ params }) => {
  const adId = params.ad as AdId;
  const adInfo = await api.getAd(adId);
  const images = await api.getAdImages(adId).then(
    (images) => {
      return images.images.map((img) => {
        return {
          url: buildImageUrl(`ads/${params.ad}/imgs/${img}`),
          id: img
        };
      })
    });
  const adTags = await api.getAdTags(adId).then(x => x.tags);
  const authorName = await api.getUser(adInfo.authorId).then((u) => u.name);
  return {
    adInfo,
    authorName,
    images,
    adTags
  };
}) satisfies PageServerLoad;

export const actions = {
  delete_tag: async ({ locals, request, params }) => {
    const adId = params.ad as AdId;
    const data = await request.formData();
    await api.deleteAdTag(
      adId,
      {
        tag: data.get('tag') as string
      },
      locals.user?.token
    );
    throw redirect(307, `/ads/${params.ad}`);
  },
  add_tag: async ({ locals, request, params }) => {
    const data = await request.formData();
    const adId = params.ad as AdId;
    await api.addAdTag(
      adId,
      {
        tag: data.get('tag') as string
      },
      locals.user?.token
    );
    throw redirect(307, `/ads/${params.ad}`);
  },
  create_chat: async ({ locals, params }) => {
    const adId = params.ad as AdId;
    const chatId = await api.createChat(
      adId,
      locals.user?.token
    );
    throw redirect(307, `/ads/${params.ad}/chats/${chatId}`);
  },
  delete_image: async ({ locals, request, params }) => {
    const data = await request.formData();
    const image = data.get('image') as ImageId;
    const adId = params.ad as AdId;
    await api.deleteImage(adId, image, locals.user?.token);
  },
  add_image: async ({ locals, request, params }) => {
    const data = await request.formData();
    const adId = params.ad as AdId;
    const image = data.get('image') as File;
    if (!image.name || image.name === 'undefined') {
      return fail(400, {
        error: true,
        message: 'You must provide an image to upload'
      });
    }

    await api.uploadImage(adId, image, locals.user?.token);
  },
} satisfies Actions;
