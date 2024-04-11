<script>
    import { page } from '$app/stores';
    import { enhance } from '$app/forms';
	/** @type {import('./$types').PageData} */
	export let data;
</script>

<svelte:head>
	<title>{data.adInfo.title}</title>
</svelte:head>

<div>
    <h1>Advertisement</h1>
    <p>Name: {data.adInfo.title}</p>

    {#if $page.data.user?.userId === data.adInfo.authorId}
        <!-- <input bind:files id="imageAdd" accept="image/png, image/jpeg" type="file" /> -->
        <!-- <button on:click={addImage}>Add photos</button> -->

        <form method="POST" action="?/add_image" use:enhance enctype="multipart/form-data">
            <input id="imageAdd" accept="image/png, image/jpeg" type="file" name="image" />
            <button type="submit">Add image</button>
        </form>
    {/if}
</div>