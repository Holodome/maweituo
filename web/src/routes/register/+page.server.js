import { fail, redirect } from '@sveltejs/kit';
import * as api from '$lib/api.js';

/** @type {import('./$types').PageServerLoad} */
export async function load({ parent }) {
	const { user } = await parent();
	if (user) throw redirect(307, '/');
}

/** @type {import('./$types').Actions} */
export const actions = {
	default: async ({ request }) => {
		const data = await request.formData();

		const body = await api.post('register', {
			name: data.get('name'),
			email: data.get('email'),
			password: data.get('password')
		}, null);
		if (body.errors) {
			return fail(401, body);
		}

		throw redirect(307, '/login');
	}
};