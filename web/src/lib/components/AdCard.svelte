<script lang="ts">
  import type { Advertisement } from '$lib/types';

  export let ad: Advertisement;
</script>

<div class="card w-96 bg-base-100 shrink-0 max-w-sm shadow-md mx-auto">
  <div class="card-body">
    <h2 class="card-title">{ad.title}</h2>
    <div class="card-actions justify-start">
      {#each ad.tags as tag}
        <div class="badge badge-outline">{tag}</div>
      {/each}
    </div>
    <div class="card-actions justify-end">
      <a class="btn btn-primary" href="/ads/{ad.id}">View</a>
    </div>
  </div>
</div>
