<script>
	import { enhance } from '$app/forms';
	import { page } from '$app/stores';

	/** @type {import('./$types').PageData} */
	export let data;
</script>

<svelte:head>
	<title>{data.userInfo.name}</title>
</svelte:head>

<div>
    <h1>Name</h1>
    <p>{data.userInfo.name}</p>
    <h2>Email</h2>
    <p>{data.userInfo.email}</p>

    <form use:enhance method="POST" action="?/logout">
        <button type="submit">Logout</button>
    </form>
</div>