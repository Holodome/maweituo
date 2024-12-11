<script lang="ts">
  import type { AdResponseDto } from '$lib/api';

  export let ad: AdResponseDto;
</script>

<div class="card bg-base-100 shrink-0 max-w-sm shadow-md mx-auto" style="width: 22rem;">
  <div class="card-body">
    <h2 class="card-title">{ad.title}</h2>
    <div class="card-actions justify-start">
      <!-- {#each ad.tags as tag}
        <div class="badge badge-outline">{tag}</div>
      {/each} -->
    </div>
    <div class="card-actions justify-end">
      <a class="btn btn-primary" href="/ads/{ad.id}">View</a>
    </div>
  </div>
</div>
