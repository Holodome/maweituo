/** @type {import('@sveltejs/kit').Handle} */
export function handle({ event, resolve }) {
  const jwt = event.cookies.get('jwt');
  if (jwt) {
    const jwtBody = JSON.parse(atob(jwt.split('.')[1]));
    event.locals.user = {
      userId: jwtBody.user_id,
      token: jwt
    };
  } else {
    event.locals.user = null;
  }

  return resolve(event);
}
