<script>
  import AdCard from '$lib/components/AdCard.svelte';

  /** @type {import('./$types').PageData} */
  export let data;
</script>

<svelte:head>
  <title>Maweituo</title>
</svelte:head>

<div class="container mx-auto max-w-3xl">
  {#if data.ads.length}
    <div class="grid grid-cols-2 gap-4 mb-8">
      {#each data.ads as ad}
        <AdCard {ad} />
      {/each}
    </div>
  {:else}
    <p>No feed, try again later</p>
  {/if}
</div>
