import { error } from '@sveltejs/kit';

const base = 'http://core:8080';
// const base = 'http://localhost:8080';

export function buildUrl(path) {
	return `${base}/${path}`;
}

async function sendInternal({ method, path, data, token }) {
	const opts = { method, headers: {}, mode: 'cors' };
	if (data) {
		opts.headers['Content-Type'] = 'application/json';
		opts.body = JSON.stringify(data);
	}

	if (token) {
		opts.headers['Authorization'] = `Bearer ${token}`;
	}

	const res = await fetch(buildUrl(path), opts);
	if (res.ok || res.status === 422) 
		return res;

	throw error(res.status);
}

export async function send({ method, path, data, token }) {
	const res = await sendInternal({method, path, data, token});
	const text = await res.text();
	return text ? JSON.parse(text) : {};
}

export function get(path, token) {
	return send({ method: 'GET', path, token });
}

export async function getImage(path, token) {
	const res = await sendInternal({method: 'GET', path, data: null, token});
	return await res.blob();
}

export function del(path, data, token) {
	return send({ method: 'DELETE', path, data, token });
}

export function post(path, data, token) {
	return send({ method: 'POST', path, data, token });
}

export async function postFile(path, file, token) {
	const opts = { method: 'POST', headers: {}, body: file };

	if (token) {
		opts.headers['Authorization'] = `Bearer ${token}`;
	}

	const res = await fetch(`${base}/${path}`, opts);
	if (res.ok || res.status === 422) 
		return res;

	throw error(res.status);
}

export function put(path, data, token) {
	return send({ method: 'PUT', path, data, token });
}