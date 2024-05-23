import * as api from '$lib/api.js';
import { fail, redirect } from '@sveltejs/kit';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals, params }) {
  const messages = await api.get(`ads/${params.ad}/msg/${params.chat}`, locals?.user.token);
  return {
    messages: messages.messages
  };
}

/** @type {import('./$types').Actions} */
export const actions = {
  send_message: async ({ locals, request, params }) => {
    const data = await request.formData();
    await api.post(
      `ads/${params.ad}/msg/${params.chat}`,
      {
        text: data.get('text')
      },
      locals?.user.token
    );
    throw redirect(307, `/ads/${params.ad}/chats/${params.chat}`);
  }
};
