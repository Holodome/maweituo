<script lang="ts">
  import { page } from '$app/stores';
  import { enhance } from '$app/forms';
  import type { PageData } from './$types';
  export let data: PageData;

  $: myId = $page.data.user?.userId;
</script>

<div class="container mx-auto max-w-3xl">
  <h1 class="mb-10 text-2xl leading-none">Chat</h1>
  <div>
    <span class="font-extralight text-sm">advertisement </span>
    <a
      class="mb-8 text-lg leading-none underline underline-offset-1"
      href="/ads/{data.adInfo.id}">{data.adInfo.title}</a
    >
  </div>
  <div>
    <span class="font-extralight text-sm">send to </span>
    <a
      class="mb-8 text-lg leading-none underline underline-offset-1"
      href="/account/{data.authorInfo.id}">{data.authorInfo.name}</a
    >
  </div>

  <div class="my-8">
    {#each data.messages as msg}
      {#if msg.sender === myId}
        <div class="chat chat-end">
          <div class="chat-bubble">
            {msg.text}
          </div>
        </div>
      {:else}
        <div class="chat chat-start">
          <div class="chat-bubble">
            {msg.text}
          </div>
        </div>
      {/if}
    {/each}
  </div>

  <form use:enhance action="?/send_message" method="POST">
    <textarea
      name="text"
      class="textarea textarea-primary resize-none textarea-sm w-full max-w-xs"
      required
    ></textarea>
    <br />
    <button type="submit" class="btn btn-primary">Send</button>
  </form>
</div>
