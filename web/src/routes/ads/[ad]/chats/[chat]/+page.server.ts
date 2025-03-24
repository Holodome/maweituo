import { api, type AdId, type ChatId } from '$lib/api';
import { redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';

export const load = (async ({ locals, params }) => {
  const adId = params.ad as AdId;
  const chatId = params.chat as ChatId;
  const messages = await api.getChatHistory(adId, chatId, locals.user?.token);
  const adInfo = await api.getAd(adId);
  const authorInfo = await api.getUser(adInfo.authorId);
  const chatInfo = await api.getChat(adId, chatId, locals.user?.token);
  return {
    messages: messages.messages.items,
    adInfo,
    authorInfo,
    chatInfo
  };
}) satisfies PageServerLoad;

export const actions = {
  send_message: async ({ locals, request, params }) => {
    const adId = params.ad as AdId;
    const chatId = params.chat as ChatId;
    const data = await request.formData();
    await api.sendMessage(
      adId, chatId, { text: data.get('text') as string },
      locals.user?.token
    );
    throw redirect(307, `/ads/${params.ad}/chats/${params.chat}`);
  },
  resolved: async ({ locals, params }) => {
    const adId = params.ad as AdId;
    const body = await api.updateAd(
      adId, { resolved: true },
      locals.user?.token
    );
    body;
  },
} satisfies Actions;
