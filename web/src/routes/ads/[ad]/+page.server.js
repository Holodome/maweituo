import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals, params }) {
    const adInfo = await api.get(`ads/${params.ad}`, locals.user?.token);
    const images = adInfo.images.map((img) => api.buildUrl(`ads/${params.ad}/img/${img}`));
    return {
        adInfo,
        images
    };
}

/** @type {import('./$types').Actions} */
export const actions = {
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