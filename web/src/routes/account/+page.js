import { redirect } from '@sveltejs/kit';

export async function load({ parent }) {
	const { user } = await parent();
	console.log(user.userId);
	throw redirect(307, user ? `/account/${user.userId}` : '/login');
}