import * as api from '$lib/api.js';
import { redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';
import type { Advertisement, Message, User, Chat } from "$lib/types";

export const load = (async ({ locals, params }) => {
  const messages: { messages: Message[] } = await api.get(
    `ads/${params.ad}/msg/${params.chat}`,
    locals.user?.token
  );
  const adInfo: Advertisement = await api.get(`ads/${params.ad}`, locals.user?.token);
  const authorInfo: User = await api.get(`users/${adInfo.authorId}`, locals.user?.token);
  const chatInfo: Chat = await api.get(`ads/${adInfo.id}/chat/${params.chat}`, locals.user?.token)
  return {
    messages: messages.messages,
    adInfo,
    authorInfo,
    chatInfo
  };
}) satisfies PageServerLoad;

export const actions = {
  send_message: async ({ locals, request, params }) => {
    const data = await request.formData();
    await api.post(
      `ads/${params.ad}/msg/${params.chat}`,
      {
        text: data.get('text')
      },
      locals.user?.token
    );
    throw redirect(307, `/ads/${params.ad}/chats/${params.chat}`);
  },
  resolved: async ({ locals, params, request }) => {
    const data = await request.formData();
    const body = await api.post(
      `ads/${params.ad}/resolved`,
      data.get("user_id") as string,
      locals.user?.token
    );
    body;
  },
} satisfies Actions;
