import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals, params }) {
    const adInfo = await api.get(`ads/${params.ad}`, locals.user?.token);
    return {
        adInfo
    };
}

/** @type {import('./$types').Actions} */
export const actions = {
    add_image: async ({ locals, request, params }) => {
        const data = await request.formData();
        if (!data.image.name ||
            data.image.name === 'undefined') {
            return fail(400, {
              error: true,
              message: 'You must provide an image to upload'
            });
        }
        
        await api.post(`ads/${ params.ad }/img`,   )
    },
};