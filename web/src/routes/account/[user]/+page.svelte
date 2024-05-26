<script lang="ts">
  import { page } from '$app/stores';
  import { enhance } from '$app/forms';
  import AdCard from '$lib/components/AdCard.svelte';
  import type { PageData } from './$types';

  export let data: PageData;

  $: isMe = $page.data.user?.userId === data.userInfo.id;
</script>

<svelte:head>
  <title>{data.userInfo.name}</title>
</svelte:head>

<div class="container mx-auto max-w-3xl">
  <h1 class="mb-10 text-2xl leading-none">User</h1>
  <dl class="my-2 form-control">
    <dt><label for="name">Name</label></dt>
    <dd class="md-15">
      <input
        id="name"
        class="input input-bordered w-full max-w-xs mt-2 rounded-md"
        type="text"
        value={data.userInfo.name}
        readonly
      />
    </dd>
  </dl>
  <dl class="my-2 form-control">
    <dt><label for="name">Email</label></dt>
    <dd class="md-15">
      <input
        id="name"
        class="input input-bordered w-full max-w-xs mt-2 rounded-md"
        type="text"
        value={data.userInfo.email}
        readonly
      />
    </dd>
  </dl>

  <div>
    <h2 class="text-lg leading-none mt-8 mb-4">Advertisements</h2>
    {#if data.ads.length}
      <div class="grid grid-cols-2 gap-4 mb-8 border border-spacing-4 px-4 py-4">
        {#each data.ads as ad}
          <AdCard {ad} />
        {/each}
      </div>
    {:else}
      <p>User has no ads</p>
    {/if}
  </div>

  {#if isMe}
    <form use:enhance method="POST" action="?/logout" class="my-4">
      <button class="btn btn-outline btn-error" type="submit">Logout</button>
    </form>
  {/if}
</div>
