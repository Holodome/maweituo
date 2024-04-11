import * as api from '$lib/api.js';
import { fail } from '@sveltejs/kit';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals, params }) {
    const userInfo = await api.get(`users/${params.user}`, locals.user?.token);
    return {
        userInfo
    };
}