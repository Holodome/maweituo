<script lang="ts">
  import AdCard from '$lib/components/AdCard.svelte';
  import Pagination from '$lib/components/Pagination.svelte';
  import { page } from '$app/stores';
  import type { PageData } from './$types';

  export let data: PageData;

  const currentPage: number = parseInt(
    $page.url.searchParams.get('page') || '0',
    10
  );
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
  <Pagination totalCount={data.total} {currentPage} />
</div>
