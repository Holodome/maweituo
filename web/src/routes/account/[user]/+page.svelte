<script>
	import { enhance } from '$app/forms';

	/** @type {import('./$types').PageData} */
	export let data;
</script>

<svelte:head>
	<title>{data.userInfo.name}</title>
</svelte:head>

<div>
    <h1>User</h1>
    <p>Name: {data.userInfo.name}</p>
    <p>Email: {data.userInfo.email}</p>

    <form use:enhance method="POST" action="?/logout">
        <button type="submit">Logout</button>
    </form>
</div>