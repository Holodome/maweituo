<script>
  import { page } from '$app/stores';
  import { enhance } from '$app/forms';
  /** @type {import('./$types').PageData} */
  export let data;

  const isAuthor = () => {
    return $page.data.user?.userId === data.adInfo.authorId;
  };
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
  <div class="container mb-8">
    <h2 class="text-lg leading-none mt-8 mb-4">Gallery</h2>
    <div class="carousel carousel-center p-4 space-x-4 bg-neutral rounded-box mb-4">
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1559703248-dcaaec9fab78.jpg"
          class="rounded-box"
        />
      </div>
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1565098772267-60af42b81ef2.jpg"
          class="rounded-box"
        />
      </div>
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1572635148818-ef6fd45eb394.jpg"
          class="rounded-box"
        />
      </div>
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1494253109108-2e30c049369b.jpg"
          class="rounded-box"
        />
      </div>
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1550258987-190a2d41a8ba.jpg"
          class="rounded-box"
        />
      </div>
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1559181567-c3190ca9959b.jpg"
          class="rounded-box"
        />
      </div>
      <div class="carousel-item">
        <img
          src="https://img.daisyui.com/images/stock/photo-1601004890684-d8cbf643f5f2.jpg"
          class="rounded-box"
        />
      </div>
    </div>
    {#if isAuthor()}
      <form use:enhance method="POST" action="?/add_image" enctype="multipart/form-data">
        <input
          id="imageAdd"
          accept="image/png, image/jpeg"
          type="file"
          name="image"
          class="file-input file-input-bordered w-full max-w-xs"
        />
        <button type="submit" class="btn btn-outline btn-primary">Add image</button>
      </form>
    {/if}
  </div>

  <div class="container mb-8">
    <h2 class="text-lg leading-none mt-8 mb-4">Tags</h2>
    {#each data.adInfo.tags as tag}
      <div>
        {tag}
        {#if isAuthor()}
          <form use:enhance method="POST" action="?/delete_tag" style="display: inline-block;">
            <input type="hidden" name="tag" value={tag} />
            <button type="submit" class="btn btn-outline btn-error">Delete</button>
          </form>
        {/if}
      </div>
    {/each}
    {#if isAuthor()}
      <form use:enhance method="POST" action="?/add_tag">
        <input type="text" name="tag" class="input input-bordered w-full max-w-xs" />
        <button type="submit" class="btn btn-outline btn-primary">Add tag</button>
      </form>
    {/if}
  </div>

  {#each data.images as img}
    <div style="position: relative;">
      <img src={img.url} alt="ad" width="300" height="300" />
      {#if isAuthor()}
        <form use:enhance method="POST" action="?/delete_image">
          <input type="hidden" name="image" value={img.id} />
          <button type="submit" style="position: absolute; bottom: 10px; left: 10px">Delete</button>
        </form>
      {/if}
    </div>
  {/each}

  {#if $page.data.user && !isAuthor()}
    <form use:enhance method="POST" action="?/create_chat">
      <button type="submit">Create chat</button>
    </form>
  {:else if isAuthor() && data.chat}
    <a href="/ads/{$page.params.ad}/chats/{data.chat}">Open chat</a>
  {/if}
</div>
