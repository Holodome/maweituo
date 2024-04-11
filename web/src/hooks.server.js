/** @type {import('@sveltejs/kit').Handle} */
export function handle({ event, resolve }) {
	const jwt = event.cookies.get('jwt');
	event.locals.user = null;
	try {
		event.locals.user = jwt ? JSON.parse(atob(jwt)) : null;
	} catch (SyntaxError) {}

	return resolve(event);
}