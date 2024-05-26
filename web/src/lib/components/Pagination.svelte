<script lang="ts">
  import { redirect } from "@sveltejs/kit";
  import { page } from '$app/stores';
  import { invalidateAll, goto } from '$app/navigation';

  export let totalCount: number;
  export let currentPage: number;
  const perPage: number = 10;

  $: isFirst = currentPage <= 1;
  $: isLast = (currentPage) * perPage > totalCount;

  const prevPage = async () => {
    currentPage -= 1;
    $page.url.searchParams.set('page', currentPage.toString());
    await goto(`?${$page.url.searchParams.toString()}`);
    await invalidateAll();
  };
  const nextPage = async () => {
    currentPage += 1;
    $page.url.searchParams.set('page', currentPage.toString());
    await goto(`?${$page.url.searchParams.toString()}`);
    await invalidateAll();
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
