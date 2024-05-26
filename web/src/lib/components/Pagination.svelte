<script lang="ts">
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';

  export let totalCount: number;
  export let currentPage: number;
  const perPage: number = 10;

  $: isFirst = currentPage == 0;
  $: isLast = (currentPage) * perPage > totalCount;

  const prevPage = async () => {
    $page.url.searchParams.set('page', (currentPage - 1).toString());
    currentPage -= 1;
    await goto(`?${$page.url.searchParams.toString()}`);
  };
  const nextPage = async () => {
    $page.url.searchParams.set('page', (currentPage + 1).toString());
    currentPage += 1;
    await goto(`?${$page.url.searchParams.toString()}`);
  };
</script>

<div class="join flex flex-row justify-center items-center mb-8">
  {#if !isFirst}
    <button class="join-item btn" on:click={prevPage}>«</button>
  {:else}
    <button class="join-item btn btn-disabled">«</button>
  {/if}
  <button class="join-item btn">Page {currentPage}</button>
  {#if !isLast}
    <button class="join-item btn" on:click={nextPage}>»</button>
  {:else}
    <button class="join-item btn btn-disabled">»</button>
  {/if}
</div>
