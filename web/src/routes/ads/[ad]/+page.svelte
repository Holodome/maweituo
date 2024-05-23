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

<div>
  <h1>Advertisement</h1>
  <p>Title: {data.adInfo.title}</p>
  <p><a href="/account/{data.adInfo.authorId}">Author</a></p>

  <div>
    <h3>Tags:</h3>
    {#each data.adInfo.tags as tag}
      <div>
        {tag}
        {#if isAuthor()}
          <form use:enhance method="POST" action="?/delete_tag" style="display: inline-block;">
            <input type="hidden" name="tag" value={tag} />
            <button type="submit">Delete</button>
          </form>
        {/if}
      </div>
    {/each}
    {#if isAuthor()}
      <form use:enhance method="POST" action="?/add_tag">
        <input type="text" name="tag" />
        <button type="submit">Add tag</button>
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
  {#if isAuthor()}
    <form use:enhance method="POST" action="?/add_image" enctype="multipart/form-data">
      <input id="imageAdd" accept="image/png, image/jpeg" type="file" name="image" />
      <button type="submit">Add image</button>
    </form>
  {/if}
  {#if $page.data.user && !isAuthor()}
    <form use:enhance method="POST" action="?/create_chat">
      <button type="submit">Create chat</button>
    </form>
  {:else if isAuthor() && data.chat}
    <a href="/ads/{$page.params.ad}/chats/{data.chat}">Open chat</a>
  {/if}
</div>
