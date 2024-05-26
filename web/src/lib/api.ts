import { error } from '@sveltejs/kit';
import type { Token } from "$lib/types.ts";

const base = process.env.BACKEND ?? 'http://127.0.0.1:8080';

export function buildUrl(path: string) {
  return `${base}/${path}`;
}

async function sendInternal({ method, path, data, token }:
  { method: string, path: string, data?: Object, token?: Token }) {
  const opts: any = { method, headers: {}, mode: 'cors' };
  if (data) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(data);
  }

  if (token) {
    opts.headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(buildUrl(path), opts);
  if (res.ok || res.status === 422) return res;

  throw error(res.status);
}

export async function send({ method, path: string, data, token }:
  { method: string, path: string, data?: any, token?: Token }) {
  const res = await sendInternal({ method, path: string, data, token });
  const text = await res.text();
  return text ? JSON.parse(text) : {};
}

export function get(path: string, token?: Token) {
  return send({ method: 'GET', path, token });
}

export async function getImage(path: string, token?: Token) {
  const res = await sendInternal({ method: 'GET', path, token });
  return await res.blob();
}

export function del(path: string, data?: Object, token?: Token) {
  return send({ method: 'DELETE', path, data, token });
}

export function post(path: string, data: Object, token?: Token) {
  return send({ method: 'POST', path, data, token });
}

export async function postFile(path: string, file: any, token?: Token) {
  const opts: any = { method: 'POST', headers: {}, body: file };

  if (token) {
    opts.headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${base}/${path}`, opts);
  if (res.ok || res.status === 422) return res;

  throw error(res.status);
}

export function put(path: string, data: Object, token?: Token) {
  return send({ method: 'PUT', path, data, token });
}
