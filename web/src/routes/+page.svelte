<script>
	import AdCard from './AdCard.svelte';

    /** @type {import('./$types').PageData} */
	export let data;
</script>

<svelte:head>
	<title>Maweituo</title>
</svelte:head>

<div>
    {#each data.adInfo as ad}
        <AdCard {ad} />
    {/each}
</div>