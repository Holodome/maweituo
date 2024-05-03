import { fail, redirect } from '@sveltejs/kit';
import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ locals }) {
	if (locals.user) throw redirect(307, '/');
}

/** @type {import('./$types').Actions} */
export const actions = {
	default: async ({ cookies, request }) => {
		const data = await request.formData();

		const body = await api.post('login', {
			name: data.get('name'),
			password: data.get('password')
		});

		if (body.errors) {
			return fail(401, body);
		}
		
		const jwt = body.access_token;
		cookies.set('jwt', jwt, { path: '/' });
		redirect(307, '/');
	}
};