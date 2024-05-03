<script>
    import { page } from '$app/stores';
	import { enhance } from '$app/forms';

	/** @type {import('./$types').PageData} */
	export let data;

	const isAuthor = () => {
        return $page.data.user?.userId === data.userInfo.id;
    };
</script>

<svelte:head>
	<title>{data.userInfo.name}</title>
</svelte:head>

<div>
    <h1>User</h1>
    <p>Name: {data.userInfo.name}</p>
    <p>Email: {data.userInfo.email}</p>

    {#if isAuthor()}
        <form use:enhance method="POST" action="?/logout">
            <button type="submit">Logout</button>
        </form>
    {/if}
</div>