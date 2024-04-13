import * as api from '$lib/api.js';
import { fail, redirect } from '@sveltejs/kit';


/** @type {import('./$types').PageServerLoad} */
export async function load({ locals, params }) {
    const adInfo = await api.get(`ads/${params.ad}`, locals.user?.token);
    const chatInfo = await api.get(`ads/${params.ad}/chat`, locals.user?.token);
    const images = adInfo.images.map((img) => {
        return {
            url: api.buildUrl(`ads/${params.ad}/img/${img}`),
            id: img
        };
    });
    return {
        adInfo,
        images,
        chat: chatInfo?.errors ? null : chatInfo
    };
}

/** @type {import('./$types').Actions} */
export const actions = {
    create_chat: async ({ locals, params }) => {
        const chatId = await api.post(`ads/${params.ad}/chat`, {}, locals.user?.token);
        throw redirect(307, `/ads/${params.ad}/chats/${chatId}`);
    },  
    delete_image: async ({ locals, request, params }) => {
        const data = await request.formData();
        const image = data.get('image');
        await api.del(`ads/${params.ad}/img/${image}`, locals.user?.token);
    },
    add_image: async ({ locals, request, params }) => {
        const data = await request.formData();
        const image = data.get('image');
        if (!image.name || image.name === 'undefined') {
            return fail(400, {
                error: true,
                message: 'You must provide an image to upload'
            });
        }

        await api.postFile(`ads/${params.ad}/img`, image,
            locals.user?.token);
    },
};