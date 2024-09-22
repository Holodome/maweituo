import * as api from '$lib/api.js';
import type { Advertisement, Chat, User } from '$lib/types.js';
import { fail, redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';

export const load = (async ({ locals, params }) => {
  const adInfo: Advertisement = await api.get(
    `ads/${params.ad}`,
    locals.user?.token
  );
  const isAuthor = locals.user?.userId === adInfo.authorId;
  const chatInfo = locals.user?.token
    ? await api.get(`ads/${params.ad}/myChat`, locals.user?.token)
    : null;
  const images = adInfo.images.map((img) => {
    return {
      url: api.buildImageUrl(`ads/${params.ad}/img/${img}`),
      id: img
    };
  });
  const authorName = await api
    .get(`users/${adInfo.authorId}`, locals.user?.token)
    .then((u) => u.name);
  const chatInfos: () => Promise<(Chat & { clientName: string })[]> = () =>
    Promise.all(adInfo.chats.map((id) => {
      return api.get(`ads/${adInfo.id}/chat/${id}`, locals.user?.token)
        .then(async (chatInfo: Chat) => {
          const userInfo: User = await api.get(`users/${chatInfo.client}`, locals.user?.token);
          return { ...chatInfo, clientName: userInfo.name };
        });
    }));
  return {
    adInfo,
    authorName,
    images,
    chat: chatInfo?.errors ? null : chatInfo,
    chatInfos: isAuthor ? await chatInfos() : []
  };
}) satisfies PageServerLoad;

/** @type {import('./$types').Actions} */
export const actions = {
  delete_tag: async ({ locals, request, params }) => {
    const data = await request.formData();
    const body = await api.del(
      `ads/${params.ad}/tag`,
      {
        tag: data.get('tag')
      },
      locals.user?.token
    );
    if (body.errors) {
      return fail(401, body);
    }
    throw redirect(307, `/ads/${params.ad}`);
  },
  add_tag: async ({ locals, request, params }) => {
    const data = await request.formData();
    const body = await api.post(
      `ads/${params.ad}/tag`,
      {
        tag: data.get('tag')
      },
      locals.user?.token
    );
    if (body.errors) {
      return fail(401, body);
    }
    throw redirect(307, `/ads/${params.ad}`);
  },
  create_chat: async ({ locals, params }) => {
    const chatId = await api.post(
      `ads/${params.ad}/chat`,
      {},
      locals.user?.token
    );
    if (chatId.errors) {
      return fail(401, chatId);
    }
    throw redirect(307, `/ads/${params.ad}/chats/${chatId}`);
  },
  delete_image: async ({ locals, request, params }) => {
    const data = await request.formData();
    const image = data.get('image');
    const body = await api.del(
      `ads/${params.ad}/img/${image}`,
      undefined,
      locals.user?.token
    );
    if (body.errors) {
      return fail(401, body);
    }
  },
  add_image: async ({ locals, request, params }) => {
    const data = await request.formData();
    const image = data.get('image') as File;
    if (!image.name || image.name === 'undefined') {
      return fail(400, {
        error: true,
        message: 'You must provide an image to upload'
      });
    }

    const body = (await api.postFile(
      `ads/${params.ad}/img`,
      image,
      locals.user?.token
    )) as { errors?: string[] };
    if (body.errors) {
      return fail(401, body);
    }
  },
} satisfies Actions;
