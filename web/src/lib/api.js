import { error } from '@sveltejs/kit';

const base = 'http://127.0.0.1:8080';

async function sendInternal({ method, path, data, token }) {
	const opts = { method, headers: {} };

	if (data) {
		opts.headers['Content-Type'] = 'application/json';
		opts.body = JSON.stringify(data);
	}

	if (token) {
		opts.headers['Authorization'] = `Bearer ${token}`;
	}

	const res = await fetch(`${base}/${path}`, opts);
	if (res.ok || res.status === 422) 
		return res;

	throw error(res.status);
}

async function send({ method, path, data, token }) {
	const res = await sendInternal({method, path, data, token});
	const text = await res.text();
	return text ? JSON.parse(text) : {};
}

export function get(path, token) {
	return send({ method: 'GET', path, token });
}

export function del(path, token) {
	return send({ method: 'DELETE', path, token });
}

export function post(path, data, token) {
	return send({ method: 'POST', path, data, token });
}

export function put(path, data, token) {
	return send({ method: 'PUT', path, data, token });
}