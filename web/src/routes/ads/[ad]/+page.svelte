<script lang="ts">
  import { page } from '$app/stores';
  import { enhance } from '$app/forms';
  import type { PageData } from './$types.js';

  export let data: PageData;

  $: isAuthor = $page.data.user?.userId === data.adInfo.authorId;
</script>

<svelte:head>
  <title>{data.adInfo.title}</title>
</svelte:head>

<div class="container mx-auto max-w-3xl">
  <h1 class="mb-10 text-2xl leading-none">Advertisement</h1>
  <div>
    <span class="font-extralight text-sm">title </span>
    <span class="mb-8 text-lg leading-none">{data.adInfo.title}</span>
  </div>
  <div>
    <span class="font-extralight text-sm">author </span>
    <a
      class="mb-8 text-lg leading-none underline underline-offset-1"
      href="/account/{data.adInfo.authorId}">{data.authorName}</a
    >
  </div>
  {#if data.adInfo.resolved}
    <p class="my-8 text-red-500">Advertisement resolved</p>
  {/if}
  <div class="container mb-8">
    <h2 class="text-lg leading-none mt-8 mb-4">Gallery</h2>
    {#if data.images.length !== 0}
      <div
        class="carousel carousel-center p-4 space-x-4 bg-neutral rounded-box mb-4"
      >
        {#each data.images as img}
          <div class="carousel-item relative">
            <img
              src={img.url}
              alt="ad"
              class="rounded-box"
              width="300"
              height="300"
            />
            {#if isAuthor}
              <form use:enhance method="POST" action="?/delete_image">
                <input type="hidden" name="image" value={img.id} />
                <button
                  class="btn btn-circle btn-outline absolute mt-2"
                  style="transform: translate(-120%, 0);"
                  type="submit"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    class="h-6 w-6"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    ><path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M6 18L18 6M6 6l12 12"
                    /></svg
                  >
                </button>
              </form>
            {/if}
          </div>
        {/each}
      </div>
    {:else}
      <p>No images in gallery</p>
    {/if}

    {#if isAuthor}
      <form
        use:enhance
        method="POST"
        action="?/add_image"
        enctype="multipart/form-data"
      >
        <input
          id="imageAdd"
          accept="image/png, image/jpeg"
          type="file"
          name="image"
          class="file-input file-input-bordered w-full max-w-xs"
        />
        <button type="submit" class="btn btn-outline btn-primary"
          >Add image</button
        >
      </form>
    {/if}
  </div>

  <div class="container mb-8">
    <h2 class="text-lg leading-none mt-8 mb-4">Tags</h2>
    <ul class="list-disc list-inside mb-4">
      {#each data.adTags as tag}
        <li>
          {tag}
          {#if isAuthor}
            <form
              use:enhance
              method="POST"
              action="?/delete_tag"
              class="inline-block ml-4"
            >
              <input type="hidden" name="tag" value={tag} />
              <button type="submit" class="btn btn-outline btn-error btn-sm"
                >Delete</button
              >
            </form>
          {/if}
        </li>
      {/each}
    </ul>
    {#if isAuthor}
      <form use:enhance method="POST" action="?/add_tag">
        <input
          type="text"
          name="tag"
          class="input input-bordered w-full max-w-xs"
        />
        <button type="submit" class="btn btn-outline btn-primary"
          >Add tag</button
        >
      </form>
    {/if}
  </div>

  {#if $page.data.user && !isAuthor && !$page.data.chat}
    <form use:enhance method="POST" action="?/create_chat">
      <button type="submit" class="btn btn-outline btn-primary"
        >Create chat</button
      >
    </form>
  {:else if data.chats !== undefined && !isAuthor && $page.data.chat}
    <a class="btn btn-outline" href="/ads/{$page.params.ad}/chats/{data.chats.at(0)?.id}"
      >Open chat</a
    >
  {:else if isAuthor && data.chats !== undefined}
    <div>
      {#each data.chats as chat}
        <p>
          <a
            href="/ads/{$page.params.ad}/chats/{chat.id}"
            class="mb-8 text-lg leading-none">Chat with user {chat.clientName}</a
          >
        </p>
      {/each}
    </div>
  {/if}

  
</div>
