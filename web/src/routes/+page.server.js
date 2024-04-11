import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals }) {
    const adInfo = await api.get(`ads`, locals.user?.token);
    return {
        adInfo
    };
}